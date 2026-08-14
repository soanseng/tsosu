package app.tsosu.data.markdown

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.markdownDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "markdown_prefs")

class MarkdownPreferences(private val context: Context) {

    private val folderUriKey = stringPreferencesKey("folder_uri")
    private val lastSyncKey = longPreferencesKey("last_sync")
    private val taskHashesKey = stringPreferencesKey("task_hashes")

    fun folderUri(): Flow<Uri?> = context.markdownDataStore.data.map { prefs ->
        prefs[folderUriKey]?.let { Uri.parse(it) }
    }

    fun isConfigured(): Flow<Boolean> = context.markdownDataStore.data.map { prefs ->
        prefs[folderUriKey] != null
    }

    suspend fun setFolderUri(uri: Uri) {
        context.markdownDataStore.edit { prefs ->
            prefs[folderUriKey] = uri.toString()
        }
    }

    suspend fun getFolderUri(): Uri? =
        context.markdownDataStore.data.first()[folderUriKey]?.let { Uri.parse(it) }

    suspend fun getLastSync(): Long =
        context.markdownDataStore.data.first()[lastSyncKey] ?: 0L

    suspend fun setLastSync(timestamp: Long) {
        context.markdownDataStore.edit { prefs ->
            prefs[lastSyncKey] = timestamp
        }
    }

    /**
     * Last-exported canonical hash per task id — baseline for conflict detection.
     */
    suspend fun getTaskHashes(): Map<String, String> =
        context.markdownDataStore.data.first()[taskHashesKey]?.let(::decodeHashes) ?: emptyMap()

    suspend fun setTaskHashes(hashes: Map<String, String>) {
        context.markdownDataStore.edit { prefs ->
            if (hashes.isEmpty()) {
                prefs.remove(taskHashesKey)
            } else {
                prefs[taskHashesKey] = encodeHashes(hashes)
            }
        }
    }

    private fun encodeHashes(hashes: Map<String, String>): String =
        hashes.entries.joinToString("\n") { "${it.key}=${it.value}" }

    private fun decodeHashes(raw: String): Map<String, String> =
        raw.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx > 0) line.substring(0, idx) to line.substring(idx + 1) else null
            }
            .toMap()

    suspend fun clear() {
        context.markdownDataStore.edit { it.clear() }
    }
}
