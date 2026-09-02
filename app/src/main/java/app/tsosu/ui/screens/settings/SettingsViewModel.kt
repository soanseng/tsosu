package app.tsosu.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.markdown.MarkdownFileAccess
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.R
import app.tsosu.VaultChangeWatcher
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.model.Project
import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult
import app.tsosu.domain.repository.ImportTarget
import app.tsosu.domain.repository.ProjectRepository
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncState
import app.tsosu.domain.usecase.ExportIcsUseCase
import app.tsosu.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.LanguageOption
import app.tsosu.ui.theme.LocaleHelper
import app.tsosu.data.local.BackupRepository
import app.tsosu.ui.theme.AppLockPreferences
import app.tsosu.notification.DigestPreferences
import app.tsosu.notification.ReminderResync
import app.tsosu.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val folderUri: String? = null,
    val isConfigured: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val canScheduleExactAlarms: Boolean = true,
    val lastSync: Long = 0L,
    val vaultFileCount: Int = 0,
    val calendarProvider: CalendarProvider = CalendarProvider.NONE,
    val caldavUrl: String = "",
    val caldavEmail: String = "",
    val caldavPassword: String = "",
    val webdavUrl: String = "",
    val webdavUsername: String = "",
    val webdavPassword: String = "",
    val message: String? = null,
    val icsContent: String? = null,
    val pendingImportUri: Uri? = null,
    val projects: List<Project> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val syncRepository: SyncRepository,
    private val vaultChangeWatcher: VaultChangeWatcher,
    private val markdownPreferences: MarkdownPreferences,
    private val markdownFileAccess: MarkdownFileAccess,
    private val calendarRepository: CalendarRepository,
    private val importRepository: ImportRepository,
    private val projectRepository: ProjectRepository,
    private val themePreferences: ThemePreferences,
    private val digestPreferences: DigestPreferences,
    private val appLockPreferences: AppLockPreferences,
    private val backupRepository: BackupRepository,
    private val reminderResync: ReminderResync,
    private val exportIcsUseCase: ExportIcsUseCase,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private fun msg(res: Int, vararg args: Any): String = appContext.getString(res, *args)

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkMode: StateFlow<DarkModeOption> = themePreferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkModeOption.SYSTEM)

    val digestEnabled: StateFlow<Boolean> = digestPreferences.enabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _language = MutableStateFlow(LocaleHelper.current())
    val language: StateFlow<LanguageOption> = _language
    init {
        viewModelScope.launch {
            markdownPreferences.folderUri().collect { uri ->
                _uiState.value = _uiState.value.copy(folderUri = uri?.toString())
            }
        }
        refreshVaultStats()
        viewModelScope.launch {
            syncRepository.isConfigured().collect { configured ->
                _uiState.value = _uiState.value.copy(isConfigured = configured)
            }
        }
        viewModelScope.launch {
            syncRepository.syncState().collect { state ->
                _uiState.value = _uiState.value.copy(syncState = state)
            }
        }
        viewModelScope.launch {
            calendarRepository.activeProvider().collect { provider ->
                _uiState.value = _uiState.value.copy(calendarProvider = provider)
            }
        }
        viewModelScope.launch {
            projectRepository.getAllProjects().collect { projects ->
                _uiState.value = _uiState.value.copy(projects = projects)
            }
        }
    }

    fun refreshAlarmPermission() {
        _uiState.value = _uiState.value.copy(
            canScheduleExactAlarms = reminderScheduler.canScheduleExactAlarms(),
        )
    }

    fun refreshVaultStats() {
        viewModelScope.launch {
            val lastSync = markdownPreferences.getLastSync()
            val taskFiles = markdownFileAccess.listFolder("tasks").size
            val dailyFiles = markdownFileAccess.listFolder("daily").size
            _uiState.value = _uiState.value.copy(
                lastSync = lastSync,
                vaultFileCount = taskFiles + dailyFiles,
            )
        }
    }

    fun selectFolder(uri: Uri) {
        viewModelScope.launch {
            markdownPreferences.setFolderUri(uri)
            sync()
        }
    }

    fun sync() {
        viewModelScope.launch {
            val result = vaultChangeWatcher.syncOnce()
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_synced, r.exported, r.imported),
                    )
                    refreshVaultStats()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_sync_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                isConfigured = false,
                folderUri = null,
                message = null,
            )
        }
    }

    fun updateCaldavUrl(url: String) {
        _uiState.value = _uiState.value.copy(caldavUrl = url)
    }

    fun updateCaldavEmail(email: String) {
        _uiState.value = _uiState.value.copy(caldavEmail = email)
    }

    fun updateCaldavPassword(password: String) {
        _uiState.value = _uiState.value.copy(caldavPassword = password)
    }

    fun stageTodoistImport(uri: Uri) {
        _uiState.value = _uiState.value.copy(pendingImportUri = uri)
    }

    fun cancelTodoistImport() {
        _uiState.value = _uiState.value.copy(pendingImportUri = null)
    }

    companion object {
        private const val MAX_IMPORT_SIZE = 10 * 1024 * 1024 // 10 MB
    }

    fun confirmTodoistImport(target: ImportTarget) {
        val uri = _uiState.value.pendingImportUri ?: return
        _uiState.value = _uiState.value.copy(pendingImportUri = null)
        viewModelScope.launch {
            val result: Result<ImportResult> = withContext(Dispatchers.IO) {
                try {
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext Result.failure(IllegalStateException("Could not read file"))
                    if (bytes.size > MAX_IMPORT_SIZE) {
                        return@withContext Result.failure(
                            IllegalArgumentException("File too large (${bytes.size / 1024}KB). Max 10MB."),
                        )
                    }
                    importRepository.importFromTodoist(bytes, ImportFormat.TODOIST_CSV, target)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            result.fold(
                onSuccess = { r ->
                    val warningText = if (r.warnings.isNotEmpty()) {
                        "\n${r.warnings.joinToString("\n")}"
                    } else ""
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_imported_todoist, r.tasksImported, warningText),
                    )
                    // Push imported tasks into the markdown vault
                    vaultChangeWatcher.syncOnce()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_import_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun importTickTick(uri: Uri) {
        viewModelScope.launch {
            val result: Result<ImportResult> = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = appContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not read file")
                    if (bytes.size > MAX_IMPORT_SIZE) error("File too large (${bytes.size / 1024}KB). Max 10MB.")
                    importRepository.importFromTickTick(bytes).getOrThrow()
                }
            }
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_imported_ticktick, r.tasksImported),
                    )
                    vaultChangeWatcher.syncOnce()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(message = msg(R.string.msg_import_error, e.message ?: ""))
                },
            )
        }
    }

    fun connectGoogle(accessToken: String, refreshToken: String?, email: String) {
        viewModelScope.launch {
            val result = calendarRepository.configureGoogle(accessToken, refreshToken, email)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        calendarProvider = CalendarProvider.GOOGLE,
                        message = msg(R.string.msg_google_connected),
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_google_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun connectCaldav() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = calendarRepository.configureCaldav(
                state.caldavUrl, state.caldavEmail, state.caldavPassword,
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        calendarProvider = CalendarProvider.CALDAV,
                        message = msg(R.string.msg_caldav_connected),
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_caldav_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun updateWebdavUrl(value: String) {
        _uiState.value = _uiState.value.copy(webdavUrl = value)
    }

    fun updateWebdavUsername(value: String) {
        _uiState.value = _uiState.value.copy(webdavUsername = value)
    }

    fun updateWebdavPassword(value: String) {
        _uiState.value = _uiState.value.copy(webdavPassword = value)
    }

    fun connectWebdav() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = calendarRepository.configureWebdav(
                state.webdavUrl, state.webdavUsername, state.webdavPassword,
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        calendarProvider = CalendarProvider.WEBDAV,
                        message = msg(R.string.msg_webdav_connected),
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_webdav_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun disconnectCalendar() {
        viewModelScope.launch {
            calendarRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                calendarProvider = CalendarProvider.NONE,
                caldavUrl = "",
                caldavEmail = "",
                caldavPassword = "",
                webdavUrl = "",
                webdavUsername = "",
                webdavPassword = "",
                message = msg(R.string.msg_calendar_disconnected),
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDynamicColor(enabled) }
    }

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { themePreferences.setDarkMode(option) }
    }

    fun setDigestEnabled(enabled: Boolean) {
        viewModelScope.launch { digestPreferences.setEnabled(enabled) }
    }

    val appLockEnabled: StateFlow<Boolean> = appLockPreferences.enabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch { appLockPreferences.setEnabled(enabled) }
    }

    fun setLanguage(option: LanguageOption) {
        LocaleHelper.apply(option)
        _language.value = option
    }

    fun exportBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            runCatching {
                val json = backupRepository.exportJson()
                val out = context.contentResolver.openOutputStream(uri, "wt")
                    ?: error("Could not open $uri for writing")
                // Close the Writer (not just the stream) so the buffer
                // flushes — closing only the stream silently drops it.
                out.bufferedWriter().use { it.write(json) }
            }.onSuccess {
                _uiState.value = _uiState.value.copy(message = msg(R.string.msg_backup_saved))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(message = msg(R.string.msg_backup_failed, e.message ?: ""))
            }
        }
    }

    fun restoreBackup(uri: Uri, context: Context) {
        viewModelScope.launch {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: error("Could not read backup file")
                backupRepository.restore(backupRepository.decode(text))
            }.onSuccess {
                reminderResync.afterSync()
                _uiState.value = _uiState.value.copy(message = msg(R.string.msg_backup_restored))
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(message = msg(R.string.msg_restore_failed, e.message ?: ""))
            }
        }
    }

    fun exportIcs() {
        viewModelScope.launch {
            val result = exportIcsUseCase()
            result.fold(
                onSuccess = { ics ->
                    if (ics.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            message = msg(R.string.msg_ics_empty),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(icsContent = ics)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = msg(R.string.msg_ics_error, e.message ?: ""),
                    )
                },
            )
        }
    }

    fun clearIcsContent() {
        _uiState.value = _uiState.value.copy(icsContent = null)
    }
}
