package app.tsosu.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.appLockDataStore by preferencesDataStore(name = "app_lock_prefs")

/** Biometric/device-credential gate for the app (off by default). */
@Singleton
class AppLockPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        val ENABLED = booleanPreferencesKey("app_lock_enabled")
    }

    val enabled: Flow<Boolean> = context.appLockDataStore.data
        .map { it[ENABLED] ?: false }

    suspend fun setEnabled(value: Boolean) {
        context.appLockDataStore.edit { it[ENABLED] = value }
    }
}
