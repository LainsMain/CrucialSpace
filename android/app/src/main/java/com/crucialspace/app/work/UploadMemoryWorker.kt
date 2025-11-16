package com.crucialspace.app.work

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.remote.ApiService
import com.crucialspace.app.data.remote.AiReminder
import com.crucialspace.app.data.remote.textPart
import com.crucialspace.app.data.ai.GeminiClient
import com.crucialspace.app.settings.SettingsStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

class UploadMemoryWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
		override suspend fun doWork(): Result {
		val appDb = db(applicationContext)
		val dao = appDb.memoryDao()
		val pending = dao.listByStatus("PENDING")
		if (pending.isEmpty()) return Result.success()

			val settings = SettingsStore(applicationContext)
			val useLocal = settings.isLocalAiEnabled() && !settings.getGeminiApiKey().isNullOrBlank()
			val gemini = GeminiClient(applicationContext)
		val contentResolver = applicationContext.contentResolver
		val moshi = Moshi.Builder().build()
        val todosType = Types.newParameterizedType(List::class.java, String::class.java)
        val remindersType = Types.newParameterizedType(List::class.java, AiReminder::class.java)
        val todosAdapter = moshi.adapter<List<String>>(todosType)
        val remindersAdapter = moshi.adapter<List<AiReminder>>(remindersType)

			var transientFailure = false
		for (m in pending) {
				var imagePart: MultipartBody.Part? = null
				var audioPart: MultipartBody.Part? = null
				var notePart: okhttp3.RequestBody? = null
				try {
					if (useLocal) {
						val imgUri = m.imageUri?.let { Uri.parse(it) }
						val audUri = m.audioUri?.let { Uri.parse(it) }
						val nowIso = java.time.OffsetDateTime.now(ZoneOffset.UTC).toString()
						val existing = db(applicationContext).memoryDao().listCollections().map { it.name.trim() }.filter { it.isNotEmpty() }.joinToString(separator = "\n")
						val result = gemini.analyze(imgUri, m.noteText, audUri, nowIso, existing)
						val normalizedReminders = normalizeReminders(result.reminders)
						val todosJson = todosAdapter.toJson(result.todos)
						val urlsJson = todosAdapter.toJson(result.urls)
						val remindersJson = remindersAdapter.toJson(normalizedReminders)
						val safeTitle = result.title ?: result.summary
						val embeddingJson = (result.embedding ?: emptyList<Double>()).joinToString(prefix = "[", postfix = "]") { it.toString() }
					dao.markSuccess(m.id, safeTitle, result.summary, todosJson, urlsJson, remindersJson, embeddingJson, result.transcript, "SYNCED")
						try {
							val repo = com.crucialspace.app.data.repo.MemoryRepository(appDb)
							repo.assignCollections(m.id, result.collections)
						} catch (_: Throwable) {}
						normalizedReminders.forEachIndexed { idx, r ->
							if (!r.datetime.isNullOrBlank()) {
								ReminderWorker.schedule(
									applicationContext,
									uniqueName = "reminder-${m.id}-$idx",
									whenIso = r.datetime,
									title = r.event,
									text = safeTitle,
									memoryId = m.id,
									reminderIndex = idx
								)
							}
						}
					} else {
						imagePart = m.imageUri?.let { uriString ->
							uriToPart(contentResolver, Uri.parse(uriString), "image")
						}
						audioPart = m.audioUri?.let { uriString ->
							uriToPart(contentResolver, Uri.parse(uriString), "audio")
						}
						notePart = textPart(m.noteText)
						val nowIso = java.time.OffsetDateTime.now(ZoneOffset.UTC).toString()
						val nowPart = nowIso.toRequestBody("text/plain".toMediaTypeOrNull())
						val existing = db(applicationContext).memoryDao().listCollections().map { it.name.trim() }.filter { it.isNotEmpty() }.joinToString(separator = "\n")
						val existingPart = if (existing.isNotBlank()) existing.toRequestBody("text/plain".toMediaTypeOrNull()) else null
						val api = ApiService.create(applicationContext)
						val result = api.process(imagePart, notePart, audioPart, nowPart, existingPart)
						val normalizedReminders = normalizeReminders(result.reminders)
						val todosJson = todosAdapter.toJson(result.todos)
						val urlsJson = todosAdapter.toJson(result.urls)
						val remindersJson = remindersAdapter.toJson(normalizedReminders)
						val safeTitle = result.title ?: result.summary
						val embeddingJson = (result.embedding ?: emptyList<Double>()).joinToString(prefix = "[", postfix = "]") { it.toString() }
					dao.markSuccess(m.id, safeTitle, result.summary, todosJson, urlsJson, remindersJson, embeddingJson, result.transcript, "SYNCED")
						try {
							val repo = com.crucialspace.app.data.repo.MemoryRepository(appDb)
							repo.assignCollections(m.id, result.collections)
						} catch (_: Throwable) {}
						normalizedReminders.forEachIndexed { idx, r ->
							if (!r.datetime.isNullOrBlank()) {
								ReminderWorker.schedule(
									applicationContext,
									uniqueName = "reminder-${m.id}-$idx",
									whenIso = r.datetime,
									title = r.event,
									text = safeTitle,
									memoryId = m.id,
									reminderIndex = idx
								)
							}
						}
					}
				} catch (t: Throwable) {
                // Try to fetch raw response for debugging
                try {
						val ip = imagePart
						if (ip != null && !useLocal) {
                        val nowIso2 = java.time.OffsetDateTime.now(ZoneOffset.UTC).toString()
                        val nowPart2 = nowIso2.toRequestBody("text/plain".toMediaTypeOrNull())
                        val existing2 = db(applicationContext).memoryDao().listCollections().map { it.name.trim() }.filter { it.isNotEmpty() }.joinToString(separator = "\n")
                        val existingPart2 = if (existing2.isNotBlank()) existing2.toRequestBody("text/plain".toMediaTypeOrNull()) else null
						val api = ApiService.create(applicationContext)
						val raw = api.processRaw(ip, notePart, audioPart, nowPart2, existingPart2)
                        val body = raw.body()?.string()
                        android.util.Log.e("UploadMemoryWorker", "process failed; raw=${body}")
                    }
                } catch (_: Throwable) {}
                android.util.Log.e("UploadMemoryWorker", "process failed", t)
				if (t is java.net.SocketTimeoutException || t is java.io.IOException) {
					// retry later
					dao.markPending(m.id)
					transientFailure = true
				} else {
					dao.markError(m.id, t.message ?: t.javaClass.simpleName)
				}
			}
		}
		return if (transientFailure) Result.retry() else Result.success()
	}
}

private fun uriToPart(resolver: ContentResolver, uri: Uri, name: String): MultipartBody.Part {
    val filename = (uri.lastPathSegment ?: "$name.bin")
    val inferredType = resolver.getType(uri) ?: guessContentType(filename)
    val bytes: ByteArray = when (uri.scheme) {
        "file" -> File(uri.path!!).readBytes()
        else -> resolver.openInputStream(uri)!!.use { it.readBytes() }
    }
    val body = bytes.toRequestBody(inferredType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(name, filename, body)
}

private fun guessContentType(filename: String): String {
    return when {
        filename.endsWith(".png", ignoreCase = true) -> "image/png"
        filename.endsWith(".jpg", ignoreCase = true) || filename.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
        filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
        filename.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
        filename.endsWith(".mp4", ignoreCase = true) -> "audio/mp4"
        filename.endsWith(".aac", ignoreCase = true) -> "audio/aac"
        filename.endsWith(".wav", ignoreCase = true) -> "audio/wav"
        filename.endsWith(".3gp", ignoreCase = true) || filename.endsWith(".3gpp", ignoreCase = true) -> "audio/3gpp"
        filename.endsWith(".amr", ignoreCase = true) -> "audio/amr"
        else -> "application/octet-stream"
    }
}

private fun normalizeReminders(reminders: List<AiReminder>): List<AiReminder> {
    if (reminders.isEmpty()) return reminders
    val userZone: ZoneId = ZoneId.systemDefault()
    val defaultLocal = ZonedDateTime.now(userZone)
        .plusDays(1)
        .withHour(10)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
    val defaultUtc = defaultLocal.withZoneSameInstant(ZoneId.of("UTC"))
    val isoUtc = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(defaultUtc)
    return reminders.map { r ->
        if (r.datetime.isBlank()) r.copy(datetime = isoUtc) else r
    }
}
