package app.tsosu.data.vikunja.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncDispatcher: SyncDispatcher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            syncDispatcher.drainQueue()
            Result.success()
        } catch (e: Exception) {
            Log.w("SyncWorker", "Queue drain failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "tsosu_sync_worker"
    }
}
