package app.tsosu

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.tsosu.notification.OverdueCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class TsosuApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var vaultChangeWatcher: VaultChangeWatcher

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        vaultChangeWatcher.start()
        scheduleOverdueCheck()
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
}
