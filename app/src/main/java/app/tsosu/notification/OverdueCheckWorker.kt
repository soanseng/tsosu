package app.tsosu.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tsosu.data.local.dao.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "overdue_check"
    }

    override suspend fun doWork(): Result {
        return try {
            val now = System.currentTimeMillis()
            val overdueTasks = taskDao.getOverdueTasks(now).first()

            if (overdueTasks.isNotEmpty()) {
                notificationHelper.showOverdueSummary(
                    count = overdueTasks.size,
                    titles = overdueTasks.map { it.title },
                )
            }

            Result.success()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Result.retry()
        }
    }
}
