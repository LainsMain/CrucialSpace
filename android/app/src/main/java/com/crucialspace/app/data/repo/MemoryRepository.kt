package com.crucialspace.app.data.repo

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import com.crucialspace.app.data.local.AppDatabase
import com.crucialspace.app.data.local.MemoryEntity
import com.crucialspace.app.work.UploadMemoryWorker
import com.crucialspace.app.work.ReminderWorker
import com.crucialspace.app.data.remote.AiReminder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val db: AppDatabase) {
	suspend fun listAll(): List<MemoryEntity> = db.memoryDao().listAll()

    fun observeAll(): Flow<List<MemoryEntity>> = db.memoryDao().listAllFlow()

	suspend fun saveAndQueue(entity: MemoryEntity, context: Context) {
		db.memoryDao().insert(entity)
		// queue a unique work to upload pending items
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		val work = OneTimeWorkRequestBuilder<UploadMemoryWorker>()
			.setConstraints(constraints)
			.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, java.time.Duration.ofSeconds(30))
			.build()
		WorkManager.getInstance(context).enqueueUniqueWork(
			"upload-pending", ExistingWorkPolicy.APPEND_OR_REPLACE, work
		)
	}

    suspend fun getById(id: String): MemoryEntity? = db.memoryDao().getById(id)

    suspend fun retry(id: String, context: Context) {
        db.memoryDao().markPending(id)
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		val work = OneTimeWorkRequestBuilder<UploadMemoryWorker>()
			.setConstraints(constraints)
			.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, java.time.Duration.ofSeconds(30))
			.build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "upload-pending", ExistingWorkPolicy.APPEND_OR_REPLACE, work
        )
    }

    suspend fun delete(id: String, context: Context) {
        // Cancel any scheduled reminders for this memory
        try {
            val entity = db.memoryDao().getById(id)
            val json = entity?.aiRemindersJson
            if (!json.isNullOrBlank()) {
                val moshi = Moshi.Builder().build()
                val type = Types.newParameterizedType(List::class.java, AiReminder::class.java)
                val adapter = moshi.adapter<List<AiReminder>>(type)
                val list = runCatching { adapter.fromJson(json) }.getOrNull() ?: emptyList()
                list.forEachIndexed { idx, _ ->
                    ReminderWorker.cancel(context, "reminder-$id-$idx")
                }
            }
        } catch (_: Throwable) {}
        db.memoryDao().deleteById(id)
    }

    suspend fun deleteMany(ids: List<String>, context: Context) {
        // Best-effort cancel for each id
        ids.forEach { id ->
            try {
                val entity = db.memoryDao().getById(id)
                val json = entity?.aiRemindersJson
                if (!json.isNullOrBlank()) {
                    val moshi = Moshi.Builder().build()
                    val type = Types.newParameterizedType(List::class.java, AiReminder::class.java)
                    val adapter = moshi.adapter<List<AiReminder>>(type)
                    val list = runCatching { adapter.fromJson(json) }.getOrNull() ?: emptyList()
                    list.forEachIndexed { idx, _ ->
                        ReminderWorker.cancel(context, "reminder-$id-$idx")
                    }
                }
            } catch (_: Throwable) {}
        }
        db.memoryDao().deleteByIds(ids)
    }

    suspend fun assignCollections(memoryId: String, names: List<String>) {
        if (names.isEmpty()) return
        val collRepo = CollectionRepository(db)
        val dao = db.memoryDao()
        val desired = names.mapNotNull { it.trim() }.filter { it.isNotBlank() }.distinct().take(3)
        if (desired.isEmpty()) return
        val existingNames = dao.listCollections().map { it.name }.toSet()
        var createdNew = 0
        for (name in desired) {
            val coll = if (existingNames.contains(name)) {
                dao.getCollectionByName(name)!!
            } else {
                if (createdNew >= 1) continue
                createdNew += 1
                collRepo.create(name)
            }
            collRepo.addMemoryToCollection(coll.id, memoryId)
        }
    }
}
