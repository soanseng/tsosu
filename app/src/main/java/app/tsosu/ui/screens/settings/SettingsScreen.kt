package app.tsosu.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.SyncState

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        // Vikunja Server Section
        Text(
            stringResource(R.string.settings_vikunja_server),
            style = MaterialTheme.typography.titleMedium,
        )

        if (state.isConnected) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_connected),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.sync() },
                            enabled = state.syncState != SyncState.SYNCING,
                        ) {
                            Text(
                                if (state.syncState == SyncState.SYNCING)
                                    stringResource(R.string.settings_syncing)
                                else
                                    stringResource(R.string.settings_sync_now)
                            )
                        }
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text(stringResource(R.string.settings_disconnect))
                        }
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = { viewModel.updateServerUrl(it) },
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text("http://localhost:3456") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.username,
                onValueChange = { viewModel.updateUsername(it) },
                label = { Text(stringResource(R.string.settings_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = { viewModel.updatePassword(it) },
                label = { Text(stringResource(R.string.settings_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(
                onClick = { viewModel.connect() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_connect))
            }
        }

        HorizontalDivider()

        // Calendar Section
        Text(
            stringResource(R.string.settings_calendar),
            style = MaterialTheme.typography.titleMedium,
        )

        when (state.calendarProvider) {
            CalendarProvider.GOOGLE -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_google_connected),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.disconnectCalendar() }) {
                            Text(stringResource(R.string.settings_calendar_disconnect))
                        }
                    }
                }
            }
            CalendarProvider.CALDAV -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.settings_caldav_connected),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.disconnectCalendar() }) {
                            Text(stringResource(R.string.settings_calendar_disconnect))
                        }
                    }
                }
            }
            CalendarProvider.NONE -> {
                var showCaldav by remember { mutableStateOf(false) }

                Text(
                    stringResource(R.string.settings_calendar_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = {
                        // Google Sign-In requires Activity context — trigger from ViewModel
                        // For now shows as placeholder; actual flow needs CredentialManager in Activity
                        viewModel.connectGoogle("", null, "")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_google_connect))
                }

                OutlinedButton(
                    onClick = { showCaldav = !showCaldav },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_caldav_title))
                }

                if (showCaldav) {
                    OutlinedTextField(
                        value = state.caldavUrl,
                        onValueChange = { viewModel.updateCaldavUrl(it) },
                        label = { Text(stringResource(R.string.settings_server_url)) },
                        placeholder = { Text("https://caldav.fastmail.com/dav/calendars/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.caldavEmail,
                        onValueChange = { viewModel.updateCaldavEmail(it) },
                        label = { Text(stringResource(R.string.settings_caldav_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.caldavPassword,
                        onValueChange = { viewModel.updateCaldavPassword(it) },
                        label = { Text(stringResource(R.string.settings_caldav_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Button(
                        onClick = { viewModel.connectCaldav() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_caldav_connect))
                    }
                }
            }
        }

        state.message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
