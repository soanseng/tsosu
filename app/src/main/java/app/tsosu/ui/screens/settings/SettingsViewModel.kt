package app.tsosu.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isConnected: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val calendarProvider: CalendarProvider = CalendarProvider.NONE,
    val caldavUrl: String = "",
    val caldavEmail: String = "",
    val caldavPassword: String = "",
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val calendarRepository: CalendarRepository,
    private val importRepository: ImportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            syncRepository.isRemoteConfigured().collect { configured ->
                _uiState.value = _uiState.value.copy(isConnected = configured)
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
    }

    fun updateServerUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
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

    fun connect() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = syncRepository.login(state.serverUrl, state.username, state.password)
            result.fold(
                onSuccess = { info ->
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        message = "Connected to ${info.version}",
                    )
                    sync()
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Error: ${e.message}",
                    )
                },
            )
        }
    }

    fun sync() {
        viewModelScope.launch {
            val result = syncRepository.sync()
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = "Synced: ${r.pulled} pulled, ${r.pushed} pushed",
                    )
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
                isConnected = false,
                serverUrl = "",
                username = "",
                password = "",
                message = null,
            )
        }
    }

    fun importTodoist(data: ByteArray) {
        viewModelScope.launch {
            val result = importRepository.importFromTodoist(data, ImportFormat.TODOIST_CSV)
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = "Imported ${r.tasksImported} tasks from Todoist",
                    )
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
}
