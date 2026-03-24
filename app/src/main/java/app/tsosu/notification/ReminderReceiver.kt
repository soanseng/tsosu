package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.tsosu.data.local.dao.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER = "app.tsosu.ACTION_REMINDER"
        const val ACTION_COMPLETE = "app.tsosu.ACTION_COMPLETE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var taskDao: TaskDao

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        when (intent.action) {
            ACTION_REMINDER -> handleReminder(taskId)
            ACTION_COMPLETE -> handleComplete(taskId)
        }
    }

    private fun handleReminder(taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskDao.getByIdSync(taskId) ?: return@launch
                notificationHelper.showReminder(
                    taskId = task.id,
                    title = task.title,
                    notificationId = task.id.hashCode(),
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleComplete(taskId: String) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                taskDao.setStatus(
                    taskId = taskId,
                    status = 4, // TaskStatus.DONE ordinal
                    completedDate = now,
                    cancelledDate = null,
                    updatedAt = now,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
