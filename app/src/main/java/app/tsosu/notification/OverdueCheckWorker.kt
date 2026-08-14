package app.tsosu.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tsosu.VaultChangeWatcher
import app.tsosu.data.local.dao.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val vaultChangeWatcher: VaultChangeWatcher,
    private val taskDao: TaskDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "overdue_check"
    }

    override suspend fun doWork(): Result {
        return try {
            // Refresh from the markdown vault first so overdue state is current.
            // Failure here (e.g. concurrent sync) still lets us read Room state.
            vaultChangeWatcher.syncOnce()

            val now = System.currentTimeMillis()
            val overdueTasks = taskDao.getOverdueTasks(now).first()

            if (overdueTasks.isNotEmpty()) {
                notificationHelper.showOverdueSummary(
                    count = overdueTasks.size,
                    titles = overdueTasks.map { it.title },
                    taskIds = overdueTasks.map { it.id },
                )
            }

            Result.success()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Result.retry()
        }
    }
}
