package app.tsosu

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.tsosu.notification.DailyDigestWorker
import app.tsosu.notification.OverdueCheckWorker
import app.tsosu.notification.ReminderResync
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class TsosuApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var reminderResync: ReminderResync

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleOverdueCheck()
        scheduleDailyDigest()
        // Re-arm alarms on every process start: Android clears them on APK
        // replace, so boot-time is not enough after an app update.
        appScope.launch { reminderResync.afterSync() }
    }

    private fun scheduleOverdueCheck() {
        val request = PeriodicWorkRequestBuilder<OverdueCheckWorker>(
            6, TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OverdueCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun scheduleDailyDigest() {
        // Both slots are enqueued unconditionally; the worker no-ops when the
        // digest is disabled in settings.
        val tz = java.util.TimeZone.getDefault()
        fun nextAt(hour: Int): Long {
            val cal = java.util.Calendar.getInstance(tz)
            cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis - System.currentTimeMillis()
        }
        for (morning in listOf(true, false)) {
            val request = PeriodicWorkRequestBuilder<DailyDigestWorker>(
                24, TimeUnit.HOURS,
            )
                .setInitialDelay(nextAt(if (morning) 8 else 20), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(DailyDigestWorker.KEY_MORNING to morning))
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                if (morning) DailyDigestWorker.WORK_NAME_MORNING else DailyDigestWorker.WORK_NAME_EVENING,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
