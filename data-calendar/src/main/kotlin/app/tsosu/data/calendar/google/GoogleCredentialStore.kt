package app.tsosu.data.calendar.google

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.googleCalendarDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "google_calendar_credentials"
)

class GoogleCredentialStore(private val context: Context) {

    companion object {
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("google_access_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("google_refresh_token")
        private val KEY_ACCOUNT_EMAIL = stringPreferencesKey("google_account_email")
        private val KEY_CALENDAR_ID = stringPreferencesKey("google_calendar_id")
    }

    fun isConfigured(): Flow<Boolean> = context.googleCalendarDataStore.data.map {
        it[KEY_ACCESS_TOKEN] != null && it[KEY_ACCOUNT_EMAIL] != null
    }

    suspend fun save(
        accessToken: String,
        refreshToken: String?,
        accountEmail: String,
        calendarId: String = "primary",
    ) {
        context.googleCalendarDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[KEY_REFRESH_TOKEN] = it }
            prefs[KEY_ACCOUNT_EMAIL] = accountEmail
            prefs[KEY_CALENDAR_ID] = calendarId
        }
    }

    suspend fun getCredentials(): GoogleCalendarCredentials? {
        val prefs = context.googleCalendarDataStore.data.first()
        val accessToken = prefs[KEY_ACCESS_TOKEN] ?: return null
        val accountEmail = prefs[KEY_ACCOUNT_EMAIL] ?: return null
        return GoogleCalendarCredentials(
            accessToken = accessToken,
            refreshToken = prefs[KEY_REFRESH_TOKEN],
            accountEmail = accountEmail,
            calendarId = prefs[KEY_CALENDAR_ID] ?: "primary",
        )
    }

    suspend fun updateAccessToken(accessToken: String) {
        context.googleCalendarDataStore.edit { prefs ->
            prefs[KEY_ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun clear() {
        context.googleCalendarDataStore.edit { it.clear() }
    }
}

data class GoogleCalendarCredentials(
    val accessToken: String,
    val refreshToken: String?,
    val accountEmail: String,
    val calendarId: String = "primary",
)
