package app.tsosu.data.vikunja.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vikunjaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "vikunja_credentials"
)

class VikunjaCredentialStore(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_TOKEN = stringPreferencesKey("token")
        private val KEY_USERNAME = stringPreferencesKey("username")
    }

    val serverUrl: Flow<String?> = context.vikunjaDataStore.data.map { it[KEY_SERVER_URL] }
    val token: Flow<String?> = context.vikunjaDataStore.data.map { it[KEY_TOKEN] }

    fun isConfigured(): Flow<Boolean> = context.vikunjaDataStore.data.map {
        it[KEY_SERVER_URL] != null && it[KEY_TOKEN] != null
    }

    suspend fun save(serverUrl: String, token: String, username: String) {
        context.vikunjaDataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_TOKEN] = token
            prefs[KEY_USERNAME] = username
        }
    }

    suspend fun getToken(): String? {
        return context.vikunjaDataStore.data.first()[KEY_TOKEN]
    }

    suspend fun getServerUrl(): String? {
        return context.vikunjaDataStore.data.first()[KEY_SERVER_URL]
    }

    suspend fun clear() {
        context.vikunjaDataStore.edit { it.clear() }
    }
}
