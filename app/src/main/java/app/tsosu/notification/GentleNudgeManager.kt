package app.tsosu.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.tsosu.R

class GentleNudgeManager(private val context: Context) {

    companion object {
        const val CHANNEL_MORNING = "tsosu_morning"
        const val CHANNEL_FOCUS = "tsosu_focus"
        const val NOTIFICATION_MORNING = 1001
        const val NOTIFICATION_FOCUS_COMPLETE = 1002
    }

    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MORNING,
                context.getString(R.string.notif_channel_morning),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FOCUS,
                context.getString(R.string.notif_channel_focus),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
        )
    }

    fun showMorningRoutine() {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_MORNING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(context.getString(R.string.notif_morning_routine))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_MORNING, notification)
    }

    fun showFocusComplete() {
        if (!hasPermission()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FOCUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentText(context.getString(R.string.notif_focus_complete))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_FOCUS_COMPLETE, notification)
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
