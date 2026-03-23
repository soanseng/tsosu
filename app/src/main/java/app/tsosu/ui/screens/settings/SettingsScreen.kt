package app.tsosu.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.SyncState
import app.tsosu.ui.theme.DarkModeOption

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val todoistFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes != null) {
            viewModel.importTodoist(bytes)
        }
    }

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

        // Appearance Section
        Text("Appearance", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Dynamic Colors", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Adapts to your wallpaper",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )
        }

        Text("Dark Mode", style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkModeOption.entries.forEach { option ->
                FilterChip(
                    selected = darkMode == option,
                    onClick = { viewModel.setDarkMode(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        HorizontalDivider()

        // Markdown Sync Section
        Text("Markdown Sync", style = MaterialTheme.typography.titleMedium)

        val folderPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.selectFolder(uri)
        }

        if (state.isConfigured) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Syncing to folder", style = MaterialTheme.typography.bodyLarge)
                    state.folderUri?.let {
                        Text(
                            Uri.parse(it).lastPathSegment ?: it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.sync() },
                            enabled = state.syncState != SyncState.SYNCING,
                        ) {
                            Text(if (state.syncState == SyncState.SYNCING) "Syncing..." else "Sync Now")
                        }
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        } else {
            Text(
                "Select a folder to sync tasks and habits as markdown files. " +
                    "Use a Syncthing or Obsidian Sync folder for cross-device sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select Folder")
            }
        }

        HorizontalDivider()

        // Todoist Import Section
        Text(
            stringResource(R.string.settings_import),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            stringResource(R.string.settings_import_todoist_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { todoistFilePicker.launch("text/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_import_todoist))
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
