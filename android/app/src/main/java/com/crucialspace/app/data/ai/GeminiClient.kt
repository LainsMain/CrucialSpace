package com.crucialspace.app.data.ai

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Base64
import com.crucialspace.app.data.remote.AiReminder
import com.crucialspace.app.data.remote.AiResult
import com.crucialspace.app.settings.SettingsStore
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.ZoneOffset

class GeminiClient(private val context: Context) {
    private val settings = SettingsStore(context)
    private val http = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .readTimeout(java.time.Duration.ofSeconds(120))
        .writeTimeout(java.time.Duration.ofSeconds(120))
        .callTimeout(java.time.Duration.ofSeconds(180))
        .pingInterval(java.time.Duration.ofSeconds(30))
        .build()
    private val moshi = Moshi.Builder().build()
    private val GENERATION_MODEL = "gemini-2.5-flash"

	private val MAX_UPLOAD_BYTES = 25 * 1024 * 1024 // mirror backend limit

    suspend fun analyze(
        imageUri: Uri?,
        noteText: String?,
        audioUri: Uri?,
        nowUtc: String?,
        existingCollectionsMultiline: String
    ): AiResult {
        val apiKey = settings.getGeminiApiKey()?.trim().orEmpty()
        require(apiKey.isNotEmpty()) { "Gemini API key is not set" }

        val resolver = context.contentResolver

        // Optional STT first (to match backend behavior)
        val transcript: String? = if (audioUri != null) {
            runCatching { transcribeWithGemini(resolver, audioUri, apiKey) }.getOrNull()
        } else null

        val prompt = buildPrompt(
            note = noteText.orEmpty(),
            sttText = transcript.orEmpty(),
            nowUtc = nowUtc,
            existingCollections = existingCollectionsMultiline
        )

		val parts = mutableListOf(PartReq(text = prompt))
		if (imageUri != null) {
			val img = loadInlineImageData(resolver, imageUri)
			if (img != null) parts.add(PartReq(inline_data = img))
		}

        val req = GenerateContentRequest(
            contents = listOf(ContentReq(role = "user", parts = parts))
        )
        val model = GENERATION_MODEL
        val json = moshi.adapter(GenerateContentRequest::class.java).toJson(req)
        val resp = postWithFallback(
            listOf(
                "https://generativelanguage.googleapis.com/v1/models/${model}:generateContent?key=${apiKey}",
                "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}"
            ),
            json
        )
        resp.use {
            if (!it.isSuccessful) throw RuntimeException("Gemini error ${it.code}: ${it.body?.string()}")
            val bodyStr = it.body?.string().orEmpty()
            val out = moshi.adapter(GenerateContentResponse::class.java).fromJson(bodyStr)
            val text = extractText(out).trim()
            if (text.isEmpty()) throw RuntimeException("Empty response from Gemini")
            val payload = extractJsonObject(text)
            val coerced = coerceResult(payload)

            // Build embedding similar to backend
            val joined = listOf(
                coerced.title.orEmpty(),
                coerced.summary,
                coerced.todos.joinToString(" "),
                coerced.urls.joinToString(" "),
                noteText.orEmpty(),
                transcript.orEmpty()
            ).joinToString(" ").trim()
            val embedding = if (joined.isNotEmpty()) embed(joined, apiKey) else emptyList()
            return coerced.copy(embedding = embedding, transcript = transcript)
        }
    }

    suspend fun embed(text: String): List<Double> {
        val apiKey = settings.getGeminiApiKey()?.trim().orEmpty()
        require(apiKey.isNotEmpty()) { "Gemini API key is not set" }
        return embed(text, apiKey)
    }

    private fun embed(text: String, apiKey: String): List<Double> {
        return runCatching {
            val req = EmbedContentRequest(content = ContentReq(parts = listOf(PartReq(text = text))))
            val adapter = moshi.adapter(EmbedContentRequest::class.java)
        val model = settings.getEmbeddingModel()
            val resp = postWithFallback(
                listOf(
                    "https://generativelanguage.googleapis.com/v1/models/${model}:embedContent?key=${apiKey}",
                    "https://generativelanguage.googleapis.com/v1beta/models/${model}:embedContent?key=${apiKey}"
                ),
                adapter.toJson(req)
            )
            resp.use {
                if (!it.isSuccessful) return emptyList()
                val bodyStr = it.body?.string().orEmpty()
                val out = moshi.adapter(EmbedContentResponse::class.java).fromJson(bodyStr)
                out?.embedding?.values ?: emptyList()
            }
        }.getOrDefault(emptyList())
    }

    private fun transcribeWithGemini(resolver: ContentResolver, audioUri: Uri, apiKey: String): String? {
        val prompt = buildSttPrompt()
		// Guard audio size; if too large, skip STT (mirrors backend size enforcement)
		val audioSize = getContentLength(resolver, audioUri)
		if (audioSize != null && audioSize > MAX_UPLOAD_BYTES) return null
		val audio = loadInlineBytesData(resolver, audioUri)
        val parts = listOf(PartReq(text = prompt), PartReq(inline_data = audio))
        val req = GenerateContentRequest(contents = listOf(ContentReq(parts = parts)))
        val model = GENERATION_MODEL
        val json = moshi.adapter(GenerateContentRequest::class.java).toJson(req)
        val resp = postWithFallback(
            listOf(
                "https://generativelanguage.googleapis.com/v1/models/${model}:generateContent?key=${apiKey}",
                "https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}"
            ),
            json
        )
        resp.use {
            if (!it.isSuccessful) return null
            val bodyStr = it.body?.string().orEmpty()
            val out = moshi.adapter(GenerateContentResponse::class.java).fromJson(bodyStr)
            val t = extractText(out).trim()
            return t.ifBlank { null }
        }
    }

    fun listModels(apiKeyOverride: String? = null): List<ModelInfo> {
        val apiKey = (apiKeyOverride?.trim().orEmpty()).ifEmpty { settings.getGeminiApiKey()?.trim().orEmpty() }
        if (apiKey.isEmpty()) return emptyList()
        val urls = listOf(
            "https://generativelanguage.googleapis.com/v1/models?key=${apiKey}",
            "https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}"
        )
        for (u in urls) {
            val resp = http.newCall(Request.Builder().url(u).get().build()).execute()
            try {
                if (!resp.isSuccessful) {
                    continue
                }
                val body = resp.body?.string().orEmpty()
                val parsed = moshi.adapter(ModelListResponse::class.java).fromJson(body)
                val items = parsed?.models ?: emptyList()
                if (items.isNotEmpty()) return items
            } finally {
                try { resp.close() } catch (_: Throwable) {}
            }
        }
        return emptyList()
    }

    private fun postWithFallback(urls: List<String>, body: String): okhttp3.Response {
        var last: okhttp3.Response? = null
        for (u in urls) {
            last?.close()
            val resp = http.newCall(
                Request.Builder()
                    .url(u)
                    .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()
            ).execute()
            if (resp.isSuccessful) return resp
            // Retry next on 404/400, otherwise return immediately
            if (resp.code != 404 && resp.code < 500) return resp
            last = resp
        }
        return last ?: throw RuntimeException("No endpoints attempted")
    }

    private fun buildPrompt(note: String, sttText: String, nowUtc: String?, existingCollections: String): String {
        val parts = mutableListOf<String>()
        if (note.isNotBlank()) parts.add("User note: ${note.trim()}")
        if (sttText.isNotBlank()) parts.add("Transcribed voice note: ${sttText.trim()}")
        if (existingCollections.isNotBlank()) parts.add("Existing collections (use if relevant; create new only if needed):\n${existingCollections.trim()}")
        val contextBlock = if (parts.isEmpty()) "(no additional text context provided)" else parts.joinToString("\n")
        val nowUtcVal = nowUtc ?: java.time.OffsetDateTime.now(ZoneOffset.UTC).toString()
        val nowLocal = java.time.ZonedDateTime.now(java.time.ZoneId.systemDefault())
        val timezoneOffset = nowLocal.offset.toString()
        val nowLocalIso = nowLocal.toString()
        return (
            "You are a Memory Assistant inside a personal knowledge app. Return ONLY valid JSON.\n" +
            "Your goal is to turn the inputs (image + note + transcript) into a concise memory summary, not a photo caption.\n" +
            "Produce a JSON object with this exact schema: \n" +
            "{\"title\": string, \"summary\": string, \"todos\": string[], \"reminders\": [{\"event\": string, \"datetime\": string}], \"urls\": string[], \"collections\": string[]}.\n" +
            "Summary style: 2–5 short sentences focused on the memory's key information.\n" +
            "- Speak as a memory manager, not as a photographer.\n" +
            "- NEVER say phrases like 'the image shows', 'the photo shows', 'in the picture'.\n" +
            "- Begin with the subject directly (e.g., 'Burger sauce recipe: ...'), not 'The image shows a burger ...'.\n" +
            "- Highlight entities (people/orgs/products), numbers (prices/qty), decisions/actions, deadlines, and must-know facts.\n" +
            "- Prefer factual, bullet-like sentences separated by periods; no first-person voice.\n" +
            "- You MUST format 'summary' in Markdown: use headings (#/##/###), bullet lists (- or 1.), and **bold** where useful.\n" +
            "- Include blank lines between headings and paragraphs for readability. Do NOT use code fences.\n" +
            "Title: a very short, scannable headline (≤ 7 words), noun-led (e.g., 'Burger Sauce Recipe').\n" +
            "Extract concrete, actionable todos; write them as imperative phrases.\n" +
            "If any date/time is implied, set reminders with ISO 8601 UTC in 'datetime'. IMPORTANT: When the user mentions a time (e.g., '7pm', '3:30pm'), interpret it as LOCAL TIME in the user's timezone (${timezoneOffset}), then convert to UTC. Current local datetime: ${nowLocalIso}. Current UTC datetime: ${nowUtcVal}.\n" +
            "If the context contains any URLs, include them in 'urls' as absolute URLs.\n" +
            "Also propose up to 3 concise 'collections' (topics like 'Fashion', 'Minecraft', 'Textile').\n" +
            "Collections policy: Prefer selecting 1–3 from the provided Existing collections when they fit well.\n" +
            "Only propose a new collection if none of the existing collections are suitable, and propose at most ONE new collection.\n" +
            "New collection naming: Title Case, 1–3 words, specific (avoid generic terms), no emojis/punctuation, no duplicates.\n" +
            "Do not return more than 3 total collections, and no more than 1 new; return [] if nothing fits.\n" +
            "If nothing is found for a field, return an empty list or an empty string accordingly.\n\n" +
            "Context:\n${contextBlock}\n\n" +
            "Respond with JSON only — no code fences, no extra text."
        )
    }

    private fun buildSttPrompt(): String =
        "Transcribe the audio to plain text. " +
            "Language hint: en-US. " +
            "Return only the transcription text without timestamps or extra formatting."

	private fun loadInlineBytesData(resolver: ContentResolver, uri: Uri): InlineDataReq {
		val inferredType = resolver.getType(uri) ?: guessContentType(uri.lastPathSegment ?: "file.bin")
		val bytes: ByteArray = when (uri.scheme) {
			"file" -> java.io.File(uri.path!!).readBytes()
			else -> resolver.openInputStream(uri)!!.use { it.readBytes() }
		}
		if (bytes.size > MAX_UPLOAD_BYTES) throw RuntimeException("Attachment too large; max 25MB")
		val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
		return InlineDataReq(mime_type = inferredType, data = b64)
	}

	private fun loadInlineImageData(resolver: ContentResolver, uri: Uri): InlineDataReq? {
		val inferredType = (resolver.getType(uri) ?: guessContentType(uri.lastPathSegment ?: "file.bin")).lowercase()
		return try {
			val processed = compressImageIfNeeded(resolver, uri)
			val b64 = Base64.encodeToString(processed, Base64.NO_WRAP)
			InlineDataReq(mime_type = if (inferredType.startsWith("image/")) inferredType else "image/jpeg", data = b64)
		} catch (t: Throwable) {
			android.util.Log.w("GeminiClient", "Failed to load/compress image, skipping", t)
			null
		}
	}

	private fun getContentLength(resolver: ContentResolver, uri: Uri): Long? {
		return try {
			resolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
		} catch (_: Throwable) { null }
	}

	private fun compressImageIfNeeded(resolver: ContentResolver, uri: Uri): ByteArray {
		// Decode bounds
		val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
		resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts) }
		val maxDim = 1600
		var sample = 1
		while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
			sample *= 2
		}
		val opts2 = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
		val bitmap = resolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it, null, opts2) }
			?: throw RuntimeException("Failed to decode image")
		val baos = java.io.ByteArrayOutputStream()
		var quality = 85
		bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
		var bytes = baos.toByteArray()
		while (bytes.size > MAX_UPLOAD_BYTES && quality > 50) {
			baos.reset()
			quality -= 5
			bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, baos)
			bytes = baos.toByteArray()
		}
		bitmap.recycle()
		if (bytes.size > MAX_UPLOAD_BYTES) throw RuntimeException("Image too large after compression")
		return bytes
	}

    private fun guessContentType(filename: String): String {
        return when {
            filename.endsWith(".png", true) -> "image/png"
            filename.endsWith(".jpg", true) || filename.endsWith(".jpeg", true) -> "image/jpeg"
            filename.endsWith(".webp", true) -> "image/webp"
            filename.endsWith(".m4a", true) -> "audio/mp4"
            filename.endsWith(".mp4", true) -> "audio/mp4"
            filename.endsWith(".aac", true) -> "audio/aac"
            filename.endsWith(".wav", true) -> "audio/wav"
            filename.endsWith(".3gp", true) || filename.endsWith(".3gpp", true) -> "audio/3gpp"
            filename.endsWith(".amr", true) -> "audio/amr"
            else -> "application/octet-stream"
        }
    }

    private fun extractText(resp: GenerateContentResponse?): String {
        if (resp == null) return ""
        val cands = resp.candidates ?: emptyList()
        for (c in cands) {
            val parts = c.content?.parts ?: emptyList()
            val sb = StringBuilder()
            for (p in parts) {
                val t = p.text?.trim()
                if (!t.isNullOrEmpty()) sb.append(t)
            }
            if (sb.isNotEmpty()) return sb.toString()
        }
        return ""
    }

    @JsonClass(generateAdapter = true)
    data class GenerateContentRequest(val contents: List<ContentReq>)

    @JsonClass(generateAdapter = true)
    data class ContentReq(val role: String = "user", val parts: List<PartReq>)

    @JsonClass(generateAdapter = true)
    data class PartReq(val text: String? = null, val inline_data: InlineDataReq? = null)

    @JsonClass(generateAdapter = true)
    data class InlineDataReq(val mime_type: String, val data: String)

    @JsonClass(generateAdapter = true)
    data class GenerateContentResponse(val candidates: List<Candidate>?)

    @JsonClass(generateAdapter = true)
    data class Candidate(val content: ContentResp?)

    @JsonClass(generateAdapter = true)
    data class ContentResp(val parts: List<TextPart>?)

    @JsonClass(generateAdapter = true)
    data class TextPart(val text: String?)

    @JsonClass(generateAdapter = true)
    data class EmbedContentRequest(val content: ContentReq)

    @JsonClass(generateAdapter = true)
    data class EmbedContentResponse(val embedding: EmbeddingVals?)

    @JsonClass(generateAdapter = true)
    data class EmbeddingVals(val values: List<Double>?)

    @JsonClass(generateAdapter = true)
    data class ModelListResponse(val models: List<ModelInfo>?)

    @JsonClass(generateAdapter = true)
    data class ModelInfo(
        val name: String?,
        val display_name: String?,
        val supported_generation_methods: List<String>? = null,
    )

    private fun extractJsonObject(text: String): Map<String, Any?> {
        var s = text.trim()
        if (s.startsWith("```")) {
            val lines = s.split("\n")
            s = lines.drop(1).joinToString("\n")
            if (s.trim().endsWith("```")) {
                s = s.substring(0, s.lastIndexOf("```"))
            }
        }
        return try {
            val adapter = moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
            adapter.fromJson(s) ?: emptyMap()
        } catch (e: Exception) {
            val start = s.indexOf('{')
            val end = s.lastIndexOf('}')
            if (start >= 0 && end > start) {
                val inner = s.substring(start, end + 1)
                val adapter = moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))
                adapter.fromJson(inner) ?: emptyMap()
            } else emptyMap()
        }
    }

    private fun coerceResult(payload: Map<String, Any?>): AiResult {
        val title = (payload["title"] as? String)?.trim().takeUnless { it.isNullOrEmpty() }
        val summary = ((payload["summary"] as? String)?.trim()).orEmpty()

        val todosRaw = payload["todos"]
        val todos: List<String> = when (todosRaw) {
            is List<*> -> todosRaw.mapNotNull { (it as? String)?.trim() }.filter { it.isNotEmpty() }
            is String -> listOf(todosRaw.trim()).filter { it.isNotEmpty() }
            else -> emptyList()
        }

        val remindersRaw = payload["reminders"]
        val reminders: List<AiReminder> = if (remindersRaw is List<*>) {
            remindersRaw.mapNotNull { r ->
                if (r is Map<*, *>) {
                    val event = (r["event"] as? String)?.trim().orEmpty()
                    val dt = (r["datetime"] as? String)?.trim().orEmpty()
                    if (event.isNotEmpty()) AiReminder(event, dt) else null
                } else null
            }
        } else emptyList()

        val urlsRaw = payload["urls"]
        val urls: List<String> = when (urlsRaw) {
            is List<*> -> urlsRaw.mapNotNull { (it as? String)?.trim() }.filter { it.isNotEmpty() }
            is String -> listOf(urlsRaw.trim()).filter { it.isNotEmpty() }
            else -> emptyList()
        }

        val colRaw = payload["collections"]
        val collections: List<String> = when (colRaw) {
            is List<*> -> colRaw.mapNotNull { (it as? String)?.trim() }.filter { it.isNotEmpty() }
            is String -> listOf(colRaw.trim()).filter { it.isNotEmpty() }
            else -> emptyList()
        }

        return AiResult(
            title = title,
            summary = summary,
            todos = todos,
            reminders = reminders,
            urls = urls,
            collections = collections,
            embedding = null,
            transcript = null,
        )
    }
}


