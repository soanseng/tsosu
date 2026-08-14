package app.tsosu

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.data.markdown.VaultChangeCoordinator
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.notification.ReminderResync
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Watches the selected Obsidian vault tree for external changes and triggers a
 * guarded, debounced sync. Also the single funnel for manual sync triggers so
 * the re-entrancy guard covers all sync paths (prevents observer feedback loops).
 *
 * External edits (Obsidian desktop, file sync) → ContentObserver → [VaultChangeCoordinator.onChange]
 * App-originated syncs (folder picker, resume pull, settings, worker) → [syncOnce]
 */
@Singleton
class VaultChangeWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: MarkdownPreferences,
    private val syncRepository: SyncRepository,
    private val reminderResync: ReminderResync,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val coordinator = VaultChangeCoordinator(
        scope = scope,
        syncAction = {
            val result = syncRepository.sync()
            reminderResync.afterSync()
            result
        },
    )

    private var observer: ContentObserver? = null

    /** Called from Application.onCreate. Re-registers whenever the vault URI changes. */
    fun start() {
        scope.launch {
            preferences.folderUri().collect { uri -> registerFor(uri) }
        }
    }

    /** Immediate guarded sync; result is observable for UI feedback. */
    suspend fun syncOnce(): Result<SyncResult> = coordinator.syncOnce()

    private fun registerFor(uri: Uri?) {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
        if (uri != null) {
            val newObserver = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) = coordinator.onChange()
            }
            observer = newObserver
            context.contentResolver.registerContentObserver(uri, true, newObserver)
        }
    }
}
