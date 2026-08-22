package app.tsosu.notification

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tsosu.R
import app.tsosu.domain.usecase.DigestFormatter
import dagger.assisted.Assisted
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.assisted.Assisted
import dagger.hilt.android.qualifiers.ApplicationContextInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.digestDataStore by preferencesDataStore(name = "digest_prefs")

class DigestPreferences @javax.inject.Inject constructor(@ApplicationContext private val context: Context) {

    private companion object {
        val ENABLED = booleanPreferencesKey("digest_enabled")
    }

    val enabled: Flow<Boolean> = context.digestDataStore.data
        .map { it[ENABLED] ?: false }

    suspend fun setEnabled(value: Boolean) {
        context.digestDataStore.edit { it[ENABLED] = value }
    }
}

/**
 * Periodic daily digest: morning briefing (top of today) and evening
 * celebration (what got done). No-Shame tone — counts, never overdue lists.
 */
@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val digestData: DigestData,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = DigestPreferences(applicationContext)
        if (!prefs.enabled.first()) return Result.success()

        val morning = inputData.getBoolean(KEY_MORNING, true)
        val content = digestData.buildDigestContent()
        val text = applicationContext.getString(
            if (morning) R.string.digest_morning_body else R.string.digest_evening_body,
            content.topPendingTitles.take(3).joinToString(" • "),
            content.completedTodayCount,
            content.habitsCompleted,
            content.habitsTotal,
        )
        notificationHelper.showDigest(morning, text)
        return Result.success()
    }

    companion object {
        const val WORK_NAME_MORNING = "daily_digest_morning"
        const val WORK_NAME_EVENING = "daily_digest_evening"
        const val KEY_MORNING = "morning"
    }
}
