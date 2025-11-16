package com.crucialspace.app.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Data
import com.crucialspace.app.data.local.db
import com.crucialspace.app.data.remote.AiReminder
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Duration
import java.time.OffsetDateTime

class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Reminder"
        val text = inputData.getString(KEY_TEXT) ?: ""
        val memoryId = inputData.getString(KEY_MEMORY_ID)
        val reminderIndex = inputData.getInt(KEY_REMINDER_INDEX, -1)

        ensureChannel(applicationContext)

        // Deep link intent to detail/{id}
        val detailUri = if (memoryId != null) Uri.parse("app://detail/$memoryId") else null
        val intent = Intent(Intent.ACTION_VIEW, detailUri)
        intent.setPackage(applicationContext.packageName)
        val pi = PendingIntent.getActivity(
            applicationContext,
            (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(com.crucialspace.app.R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        with(NotificationManagerCompat.from(applicationContext)) {
            notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notif)
        }

        // Remove the fired reminder from DB so it disappears from Upcoming/Detail
        if (memoryId != null && reminderIndex >= 0) {
            try {
                val dao = db(applicationContext).memoryDao()
                val entity = dao.getById(memoryId)
                if (entity != null && !entity.aiRemindersJson.isNullOrBlank()) {
                    val moshi = Moshi.Builder().build()
                    val type = Types.newParameterizedType(List::class.java, AiReminder::class.java)
                    val adapter = moshi.adapter<List<AiReminder>>(type)
                    val list = adapter.fromJson(entity.aiRemindersJson) ?: emptyList()
                    if (reminderIndex < list.size) {
                        val new = list.toMutableList().also { it.removeAt(reminderIndex) }
                        dao.updateReminders(memoryId, adapter.toJson(new))
                    }
                }
            } catch (_: Throwable) {}
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "cs_reminders"
        private const val KEY_TITLE = "title"
        private const val KEY_TEXT = "text"
        private const val KEY_MEMORY_ID = "memory_id"
        private const val KEY_REMINDER_INDEX = "reminder_index"

        fun schedule(context: Context, uniqueName: String, whenIso: String, title: String, text: String, memoryId: String, reminderIndex: Int) {
            val now = OffsetDateTime.now()
            val target = runCatching { OffsetDateTime.parse(whenIso) }.getOrNull() ?: return
            val delayMs = Duration.between(now, target).toMillis().coerceAtLeast(0)
            val data = Data.Builder()
                .putString(KEY_TITLE, title)
                .putString(KEY_TEXT, text)
                .putString(KEY_MEMORY_ID, memoryId)
                .putInt(KEY_REMINDER_INDEX, reminderIndex)
                .build()
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInputData(data)
                .setInitialDelay(java.time.Duration.ofMillis(delayMs))
                .addTag(uniqueName)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(uniqueName, androidx.work.ExistingWorkPolicy.REPLACE, req)
        }

        fun cancel(context: Context, uniqueName: String) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName)
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val m = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val ch = NotificationChannel(CHANNEL_ID, "CrucialSpace Reminders", NotificationManager.IMPORTANCE_HIGH)
                m.createNotificationChannel(ch)
            }
        }
    }
}


