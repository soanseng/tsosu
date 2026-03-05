package app.tsosu.data.calendar

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.caldavDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "caldav_credentials"
)

class CalDavCredentialStore(private val context: Context) {

    companion object {
        private val KEY_CALENDAR_URL = stringPreferencesKey("caldav_calendar_url")
        private val KEY_EMAIL = stringPreferencesKey("caldav_email")
        private val KEY_PASSWORD = stringPreferencesKey("caldav_password")
    }

    fun isConfigured(): Flow<Boolean> = context.caldavDataStore.data.map {
        it[KEY_CALENDAR_URL] != null && it[KEY_EMAIL] != null && it[KEY_PASSWORD] != null
    }

    suspend fun save(calendarUrl: String, email: String, password: String) {
        context.caldavDataStore.edit { prefs ->
            prefs[KEY_CALENDAR_URL] = calendarUrl
            prefs[KEY_EMAIL] = email
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun getCredentials(): CalDavCredentials? {
        val prefs = context.caldavDataStore.data.first()
        val url = prefs[KEY_CALENDAR_URL] ?: return null
        val email = prefs[KEY_EMAIL] ?: return null
        val password = prefs[KEY_PASSWORD] ?: return null
        return CalDavCredentials(url, email, password)
    }

    suspend fun clear() {
        context.caldavDataStore.edit { it.clear() }
    }
}

data class CalDavCredentials(
    val calendarUrl: String,
    val email: String,
    val password: String,
)
