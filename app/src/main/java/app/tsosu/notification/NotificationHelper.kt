package app.tsosu.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tsosu.MainActivity
import app.tsosu.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        const val CHANNEL_REMINDERS = "task_reminders"
        const val CHANNEL_OVERDUE = "task_overdue"
        const val CHANNEL_HABITS = "habit_reminders"
        private const val OVERDUE_SUMMARY_ID = 2001
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.notif_channel_reminders_desc)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OVERDUE,
                context.getString(R.string.notif_channel_overdue),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_overdue_desc)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_HABITS,
                context.getString(R.string.notif_channel_habits),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.notif_channel_habits_desc)
            }
        )
    }

    fun showReminder(taskId: String, title: String, notificationId: Int) {
        if (!hasPermission()) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("taskId", taskId)
        }
        val tapPending = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_COMPLETE
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val completePending = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SNOOZE
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            notificationId + 30_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(R.string.notif_action_complete),
                completePending,
            )
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(R.string.notif_action_snooze),
                snoozePending,
            )
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    fun showHabitReminder(habitId: String, title: String, text: String?) {
        if (!hasPermission()) return

        val notificationId = habitId.hashCode()
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("habitId", habitId)
        }
        val tapPending = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_HABIT_COMPLETE
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
        }
        val donePending = PendingIntent.getBroadcast(
            context,
            notificationId + 20_000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .addAction(
                R.mipmap.ic_launcher,
                context.getString(R.string.notif_habit_done),
                donePending,
            )
        if (!text.isNullOrBlank()) {
            builder.setContentText(text)
        }

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun showOverdueSummary(count: Int, titles: List<String>, taskIds: List<String>) {
        if (!hasPermission()) return
        if (count == 0) return

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            taskIds.firstOrNull()?.let { putExtra("taskId", it) }
        }
        val tapPending = PendingIntent.getActivity(
            context,
            OVERDUE_SUMMARY_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val summary = titles.take(5).joinToString("\n")
        val contentText = context.resources.getQuantityString(
            R.plurals.notif_overdue_count,
            count,
            count,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_OVERDUE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(contentText)
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(tapPending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(OVERDUE_SUMMARY_ID, notification)
    }

    fun cancelOverdueSummary() {
        NotificationManagerCompat.from(context).cancel(OVERDUE_SUMMARY_ID)
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
