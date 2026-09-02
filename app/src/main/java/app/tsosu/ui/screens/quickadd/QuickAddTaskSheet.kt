package app.tsosu.ui.screens.quickadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import app.tsosu.domain.recurrence.QuickAddGrammar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.tsosu.ui.screens.recurrencehelp.RecurrenceHelpSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.recurrence.RecurrenceResult
import app.tsosu.domain.recurrence.TitlePriority
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.RoutineTime
import app.tsosu.ui.components.RecurrencePicker
import app.tsosu.ui.util.rememberHaptic
import app.tsosu.ui.util.localizedName
import kotlinx.datetime.Clock
import kotlinx.datetime.todayIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickAddTaskSheet(
    onDismiss: () -> Unit,
    onAdd: (title: String, priority: Priority, dueDate: LocalDateTime?, reminderTime: LocalTime?, recurrenceRule: String?, projectName: String?, routineTime: RoutineTime?, tinyVersion: String?) -> Unit,
    initialDueDate: LocalDateTime? = null,
    initialTitle: String? = null,
    initialRecurrenceRule: String? = null,
)
{
    val haptic = rememberHaptic()
    val recurrenceParser = remember { RecurrenceParser() }
    var title by remember { mutableStateOf(initialTitle ?: "") }
    var titleError by remember { mutableStateOf(false) }
    var selectedPriority by remember { mutableStateOf(Priority.NONE) }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(initialDueDate) }
    // True once the user picked a date manually (or a calendar screen passed
    // one in); a "starting <date>" prefill from the title never overwrites it.
    var datePickedManually by remember { mutableStateOf(initialDueDate != null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<LocalTime?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    // True once the user set/cleared the reminder manually; keyword prefill
    // ("every morning" → 08:00) never overwrites a manual choice.
    var reminderPickedManually by remember { mutableStateOf(false) }
    var pickerRrule by remember { mutableStateOf(initialRecurrenceRule) }
    var detectedRrule by remember { mutableStateOf<String?>(null) }
    var detectedPriority by remember { mutableStateOf<Priority?>(null) }
    var detectedProjectName by remember { mutableStateOf<String?>(null) }
    var routineTime by remember { mutableStateOf(RoutineTime.AFTERNOON) }
    var tinyVersion by remember { mutableStateOf("") }
    var cleanTitle by remember { mutableStateOf("") }
    var showRecurrenceHelp by remember { mutableStateOf(false) }

    fun applyTitleInput(newValue: String) {
        // Detect p1-p4 priority token first, then the recurrence pattern,
        // then @project / due: grammar tokens.
        var working = newValue
        val prio = TitlePriority.extract(working)
        if (prio.priority != null) {
            detectedPriority = prio.priority
            working = prio.title
        } else {
            detectedPriority = null
        }
        // Detect trailing recurrence pattern
        val extraction = recurrenceParser.extractFromTitle(working)
        val baseTitle = if (extraction.rrule != null) {
            detectedRrule = extraction.rrule
            // "starting <date>" prefills the first due date unless the
            // user already picked one manually.
            extraction.startDate?.let { start ->
                if (!datePickedManually) {
                    dueDate = LocalDateTime(start, LocalTime(0, 0))
                }
            }
            // Time-of-day keyword ("every morning") prefills the
            // reminder unless the user already chose one.
            extraction.suggestedReminder?.let { preset ->
                if (!reminderPickedManually) {
                    reminderTime = preset
                }
            }
            extraction.title
        } else {
            detectedRrule = null
            working
        }
        // @project and due: tokens (explicit due: overrides a "starting"
        // prefill — the token is the more deliberate intent).
        val grammar = QuickAddGrammar.extract(
            baseTitle,
            Clock.System.todayIn(TimeZone.currentSystemDefault()),
        ) { phrase -> recurrenceParser.parseFlexibleDate(phrase) }
        detectedProjectName = grammar.projectName
        cleanTitle = grammar.title
        grammar.dueDate?.let { d ->
            if (!datePickedManually) {
                dueDate = LocalDateTime(d, LocalTime(0, 0))
            }
        }
    }

    // Shared/prefilled titles get the same detection pass as typed input.
    LaunchedEffect(initialTitle) {
        if (!initialTitle.isNullOrBlank()) {
            applyTitleInput(initialTitle)
        }
    }
    // Voice capture: speech recognition result lands in the title field and
    // runs the same detection pass as typed input.
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (!text.isNullOrEmpty()) {
            title = text
            titleError = false
            applyTitleInput(text)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
    ) {
        Text(stringResource(R.string.quick_add_task_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { newValue ->
                title = newValue
                if (newValue.isNotBlank()) titleError = false
                applyTitleInput(newValue)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = titleError,
            trailingIcon = {
                IconButton(onClick = {
                    voiceLauncher.launch(
                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                            )
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-TW")
                        },
                    )
                }) {
                    Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.quick_add_voice))
                }
            },
            supportingText = if (titleError) {
                { Text(stringResource(R.string.quick_add_title_required)) }
            } else null,
        )

        // Show detected recurrence chip
        val currentDetectedRrule = detectedRrule
        if (currentDetectedRrule != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = true,
                    onClick = {
                        detectedRrule = null
                    },
                    label = {
                        Text("\uD83D\uDD01 ${RecurrenceParser.toDisplayLabel(currentDetectedRrule)}")
                    },
                )
                IconButton(onClick = {
                    detectedRrule = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_recurrence))
                }
            }
        }

        val currentDetectedPriority = detectedPriority
        if (currentDetectedPriority != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = true,
                    onClick = { detectedPriority = null },
                    label = {
                        Text(
                            "${currentDetectedPriority.emoji} ${currentDetectedPriority.localizedName()}",
                            color = Color(currentDetectedPriority.color),
                        )
                    },
                )
                IconButton(onClick = { detectedPriority = null }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_priority))
                }
            }
        }

        // Show detected @project chip (filed on save; dismiss to keep the
        // token as plain title text).
        val currentDetectedProject = detectedProjectName
        if (currentDetectedProject != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = true,
                    onClick = { detectedProjectName = null },
                    label = { Text("\uD83D\uDCC1 @$currentDetectedProject") },
                )
                IconButton(onClick = { detectedProjectName = null }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_project))
                }
            }
        }


        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_priority), style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { p ->
                FilterChip(
                    selected = selectedPriority == p,
                    onClick = {
                        haptic.tick()
                        selectedPriority = p
                    },
                    label = {
                        Text(
                            text = p.localizedName(),
                            color = if (selectedPriority == p) Color(p.color) else Color.Unspecified,
                        )
                    },
                )
            }
        }


        Spacer(Modifier.height(12.dp))

        Text(stringResource(R.string.quick_add_due_date), style = MaterialTheme.typography.labelLarge)

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val todayDate = today.date
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = dueDate?.date == todayDate,
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    dueDate = LocalDateTime(todayDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_today)) },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(1, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    val tomorrowDate = todayDate.plus(1, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(tomorrowDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_tomorrow)) },
            )
            FilterChip(
                selected = dueDate?.date == todayDate.plus(7, DateTimeUnit.DAY),
                onClick = {
                    haptic.tick()
                    datePickedManually = true
                    val nextWeekDate = todayDate.plus(7, DateTimeUnit.DAY)
                    dueDate = LocalDateTime(nextWeekDate, LocalTime(0, 0))
                },
                label = { Text(stringResource(R.string.quick_add_next_week)) },
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.a11y_pick_date))
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    dueDate?.let { "${it.monthNumber}/${it.dayOfMonth}/${it.year}" }
                        ?: stringResource(R.string.quick_add_no_date),
                )
            }
            if (dueDate != null) {
                IconButton(onClick = {
                    datePickedManually = true
                    dueDate = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_date))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Reminder time
        Text(stringResource(R.string.quick_add_reminder), style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { showTimePicker = true }) {
                Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.a11y_pick_time))
                Spacer(Modifier.padding(start = 4.dp))
                Text(
                    reminderTime?.let { "%02d:%02d".format(it.hour, it.minute) }
                        ?: stringResource(R.string.quick_add_add_reminder),
                )
            }
            if (reminderTime != null) {
                IconButton(onClick = {
                    reminderPickedManually = true
                    reminderTime = null
                }) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_clear_reminder))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Recurrence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.quick_add_recurrence), style = MaterialTheme.typography.labelLarge)
            TextButton(onClick = { showRecurrenceHelp = true }) {
                Text(stringResource(R.string.recurrence_help_open))
            }
        }
        RecurrencePicker(
            rrule = detectedRrule ?: pickerRrule,
            onRruleChange = { pickerRrule = it },
        )
        val effectiveRrule = detectedRrule ?: pickerRrule
        if (effectiveRrule != null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.quick_add_routine), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RoutineTime.entries.forEach { time ->
                    FilterChip(
                        selected = routineTime == time,
                        onClick = {
                            haptic.tick()
                            routineTime = time
                        },
                        label = {
                            Text(
                                when (time) {
                                    RoutineTime.MORNING -> "${time.emoji} ${stringResource(R.string.habits_morning)}"
                                    RoutineTime.AFTERNOON -> "${time.emoji} ${stringResource(R.string.habits_anytime)}"
                                    RoutineTime.EVENING -> "${time.emoji} ${stringResource(R.string.habits_evening)}"
                                },
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tinyVersion,
                onValueChange = { tinyVersion = it },
                label = { Text(stringResource(R.string.quick_add_tiny_version)) },
                supportingText = { Text(stringResource(R.string.quick_add_tiny_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                // Grammar-stripped title when any token is still applied;
                // dismissing its chip falls back to the raw title (tokens
                // kept as plain text), matching the rrule/priority chips.
                // `cleanTitle != title` also covers a parsed `due:` token.
                val finalTitle = when {
                    detectedRrule != null || detectedPriority != null || detectedProjectName != null -> cleanTitle
                    cleanTitle != title -> cleanTitle
                    else -> title
                }
                if (finalTitle.isNotBlank()) {
                    haptic.confirm()
                    // Priority: detected from title > preset chips
                    val priority = detectedPriority ?: selectedPriority
                    // Recurrence: detected from title > picker
                    val recurrenceRule = detectedRrule ?: pickerRrule
                    onAdd(
                        finalTitle,
                        priority,
                        dueDate,
                        reminderTime,
                        recurrenceRule,
                        detectedProjectName,
                        if (recurrenceRule != null) routineTime else null,
                        tinyVersion.takeIf { it.isNotBlank() && recurrenceRule != null },
                    )
                    onDismiss()
                } else {
                    titleError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_add_submit_task))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate?.let {
                it.date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        dueDate = Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.quick_add_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.quick_add_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time picker dialog for reminder
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime?.hour ?: 9,
            initialMinute = reminderTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.task_detail_reminder_time)) },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderPickedManually = true
                    reminderTime = LocalTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) {
                    Text(stringResource(R.string.quick_add_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.quick_add_cancel))
                }
            },
        )
    }
}
