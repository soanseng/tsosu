package app.tsosu.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.markdown.MarkdownFileAccess
import app.tsosu.data.markdown.MarkdownPreferences
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
    private val exportIcsUseCase: ExportIcsUseCase,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkMode: StateFlow<DarkModeOption> = themePreferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkModeOption.SYSTEM)

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
            val habitFiles = markdownFileAccess.listFolder("habits").size
            val dailyFiles = markdownFileAccess.listFolder("daily").size
            _uiState.value = _uiState.value.copy(
                lastSync = lastSync,
                vaultFileCount = taskFiles + habitFiles + dailyFiles,
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
                        message = "Synced: ${r.exported} exported, ${r.imported} imported",
                    )
                    refreshVaultStats()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Sync error: ${e.message}",
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
                        message = "Imported ${r.tasksImported} tasks from Todoist$warningText",
                    )
                    // Push imported tasks into the markdown vault
                    vaultChangeWatcher.syncOnce()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Import error: ${e.message}",
                    )
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
                        message = "Google Calendar connected",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Google Calendar error: ${e.message}",
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
                        message = "CalDAV connected",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "CalDAV error: ${e.message}",
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
                message = "Calendar disconnected",
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDynamicColor(enabled) }
    }

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { themePreferences.setDarkMode(option) }
    }

    fun exportIcs() {
        viewModelScope.launch {
            val result = exportIcsUseCase()
            result.fold(
                onSuccess = { ics ->
                    if (ics.isBlank()) {
                        _uiState.value = _uiState.value.copy(
                            message = "No tasks with due dates to export",
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(icsContent = ics)
                    }
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "ICS export error: ${e.message}",
                    )
                },
            )
        }
    }

    fun clearIcsContent() {
        _uiState.value = _uiState.value.copy(icsContent = null)
    }
}
