package app.tsosu.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsosu.R
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.ImportTarget
import app.tsosu.domain.repository.SyncState
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.LanguageOption
import app.tsosu.ui.screens.recurrencehelp.RecurrenceHelpSheet
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showRecurrenceHelp by remember { mutableStateOf(false) }
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    val todoistFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.stageTodoistImport(uri)
    }

    val alarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { viewModel.refreshAlarmPermission() }

    // Refresh permission state when returning from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAlarmPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        Text(stringResource(R.string.settings_appearance), style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(stringResource(R.string.settings_dynamic_colors), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.settings_dynamic_colors_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )
        }

        Text(stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DarkModeOption.entries.forEach { option ->
                FilterChip(
                    selected = darkMode == option,
                    onClick = { viewModel.setDarkMode(option) },
                    label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LanguageOption.entries.forEach { option ->
                FilterChip(
                    selected = language == option,
                    onClick = { viewModel.setLanguage(option) },
                    label = {
                        Text(
                            when (option) {
                                LanguageOption.SYSTEM -> stringResource(R.string.settings_language_system)
                                LanguageOption.ENGLISH -> stringResource(R.string.settings_language_english)
                                LanguageOption.ZH_TW -> stringResource(R.string.settings_language_zh_tw)
                            },
                        )
                    },
                )
            }
        }

        OutlinedButton(
            onClick = { showRecurrenceHelp = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_recurrence_help))
        }

        HorizontalDivider()

        // Markdown Sync Section
        Text(stringResource(R.string.settings_markdown_sync), style = MaterialTheme.typography.titleMedium)

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
                    Text(stringResource(R.string.settings_syncing_to_folder), style = MaterialTheme.typography.bodyLarge)
                    state.folderUri?.let {
                        Text(
                            Uri.parse(it).lastPathSegment ?: it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.lastSync > 0L) {
                        val lastSyncText = java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault(),
                        ).format(java.util.Date(state.lastSync))
                        Text(
                            stringResource(R.string.settings_last_sync, lastSyncText),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(R.string.settings_vault_files, state.vaultFileCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.sync() },
                            enabled = state.syncState != SyncState.SYNCING,
                        ) {
                            Text(if (state.syncState == SyncState.SYNCING) stringResource(R.string.settings_syncing) else stringResource(R.string.settings_sync_now))
                        }
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text(stringResource(R.string.settings_disconnect))
                        }
                    }
                    if (!state.canScheduleExactAlarms) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.settings_exact_alarm_denied),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"),
                                )
                                alarmSettingsLauncher.launch(intent)
                            },
                        ) {
                            Text(stringResource(R.string.settings_enable_exact_alarm))
                        }
                    }
                }
            }
        } else {
            Text(
                stringResource(R.string.settings_select_folder_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_select_folder))
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

        if (state.pendingImportUri != null) {
            TodoistImportDialog(
                projects = state.projects,
                onConfirm = { target -> viewModel.confirmTodoistImport(target) },
                onDismiss = { viewModel.cancelTodoistImport() },
            )
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
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                ) {
                    Text("Google Calendar (Coming soon)")
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
                        placeholder = { Text(stringResource(R.string.settings_caldav_placeholder)) },
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

        HorizontalDivider()

        // Export Section
        Text("Export", style = MaterialTheme.typography.titleMedium)

        Text(
            "Export tasks with due dates as an ICS calendar file.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { viewModel.exportIcs() },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.padding(start = 4.dp))
            Text("Export to ICS")
        }

        // Share ICS when content is available
        LaunchedEffect(state.icsContent) {
            val ics = state.icsContent ?: return@LaunchedEffect
            val cacheDir = File(context.cacheDir, "exports")
            cacheDir.mkdirs()
            val file = File(cacheDir, "tsosu-tasks.ics")
            file.writeText(ics)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export ICS"))
            viewModel.clearIcsContent()
        }

        state.message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showRecurrenceHelp) {
        ModalBottomSheet(
            onDismissRequest = { showRecurrenceHelp = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            RecurrenceHelpSheet()
        }
    }
}

private enum class ImportDestination { INBOX, NEW_PROJECT, EXISTING_PROJECT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoistImportDialog(
    projects: List<app.tsosu.domain.model.Project>,
    onConfirm: (ImportTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    var destination by remember { mutableStateOf(ImportDestination.INBOX) }
    var newProjectName by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf(projects.firstOrNull()?.id ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.import_dialog_description),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = destination == ImportDestination.INBOX,
                        onClick = { destination = ImportDestination.INBOX },
                        label = { Text(stringResource(R.string.import_target_inbox)) },
                    )
                    FilterChip(
                        selected = destination == ImportDestination.NEW_PROJECT,
                        onClick = { destination = ImportDestination.NEW_PROJECT },
                        label = { Text(stringResource(R.string.import_target_new_project)) },
                    )
                    if (projects.isNotEmpty()) {
                        FilterChip(
                            selected = destination == ImportDestination.EXISTING_PROJECT,
                            onClick = { destination = ImportDestination.EXISTING_PROJECT },
                            label = { Text(stringResource(R.string.import_target_existing)) },
                        )
                    }
                }

                when (destination) {
                    ImportDestination.NEW_PROJECT -> {
                        OutlinedTextField(
                            value = newProjectName,
                            onValueChange = { newProjectName = it },
                            label = { Text(stringResource(R.string.import_project_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    ImportDestination.EXISTING_PROJECT -> {
                        val selectedProject = projects.find { it.id == selectedProjectId }
                        ExposedDropdownMenuBox(
                            expanded = dropdownExpanded,
                            onExpandedChange = { dropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = selectedProject?.title ?: "",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            )
                            ExposedDropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false },
                            ) {
                                projects.forEach { project ->
                                    DropdownMenuItem(
                                        text = { Text(project.title) },
                                        onClick = {
                                            selectedProjectId = project.id
                                            dropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                    ImportDestination.INBOX -> { /* No extra input needed */ }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = when (destination) {
                        ImportDestination.INBOX -> ImportTarget.Inbox
                        ImportDestination.NEW_PROJECT -> ImportTarget.NewProject(newProjectName.trim())
                        ImportDestination.EXISTING_PROJECT -> ImportTarget.ExistingProject(selectedProjectId)
                    }
                    onConfirm(target)
                },
                enabled = when (destination) {
                    ImportDestination.INBOX -> true
                    ImportDestination.NEW_PROJECT -> newProjectName.isNotBlank()
                    ImportDestination.EXISTING_PROJECT -> selectedProjectId.isNotBlank()
                },
            ) {
                Text(stringResource(R.string.import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_cancel))
            }
        },
    )
}
