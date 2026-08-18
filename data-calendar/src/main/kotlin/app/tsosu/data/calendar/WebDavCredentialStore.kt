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

private val Context.webdavDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "webdav_credentials"
)

class WebDavCredentialStore(private val context: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("webdav_base_url")
        private val KEY_USERNAME = stringPreferencesKey("webdav_username")
        private val KEY_PASSWORD = stringPreferencesKey("webdav_password")
    }

    fun isConfigured(): Flow<Boolean> = context.webdavDataStore.data.map {
        it[KEY_BASE_URL] != null && it[KEY_USERNAME] != null && it[KEY_PASSWORD] != null
    }

    suspend fun save(baseUrl: String, username: String, password: String) {
        context.webdavDataStore.edit { prefs ->
            prefs[KEY_BASE_URL] = baseUrl
            prefs[KEY_USERNAME] = username
            prefs[KEY_PASSWORD] = password
        }
    }

    suspend fun getCredentials(): WebDavCredentials? {
        val prefs = context.webdavDataStore.data.first()
        val url = prefs[KEY_BASE_URL] ?: return null
        val username = prefs[KEY_USERNAME] ?: return null
        val password = prefs[KEY_PASSWORD] ?: return null
        return WebDavCredentials(url, username, password)
    }

    suspend fun clear() {
        context.webdavDataStore.edit { it.clear() }
    }
}

data class WebDavCredentials(
    val baseUrl: String,
    val username: String,
    val password: String,
)
