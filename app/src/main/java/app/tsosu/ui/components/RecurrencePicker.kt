package app.tsosu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsosu.R
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.recurrence.RecurrenceResult

enum class RecurrencePreset(val rrule: String?) {
    NONE(null),
    DAILY("RRULE:FREQ=DAILY"),
    WEEKDAYS("RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"),
    WEEKLY("RRULE:FREQ=WEEKLY"),
    MONTHLY("RRULE:FREQ=MONTHLY"),
    CUSTOM(null);

    companion object {
        fun fromRrule(rrule: String?): RecurrencePreset = when (rrule) {
            null -> NONE
            DAILY.rrule -> DAILY
            WEEKDAYS.rrule -> WEEKDAYS
            WEEKLY.rrule -> WEEKLY
            MONTHLY.rrule -> MONTHLY
            else -> CUSTOM
        }
    }
}

private val RecurrencePreset.labelRes: Int
    get() = when (this) {
        RecurrencePreset.NONE -> R.string.recurrence_none
        RecurrencePreset.DAILY -> R.string.recurrence_daily
        RecurrencePreset.WEEKDAYS -> R.string.recurrence_weekdays
        RecurrencePreset.WEEKLY -> R.string.recurrence_weekly
        RecurrencePreset.MONTHLY -> R.string.recurrence_monthly
        RecurrencePreset.CUSTOM -> R.string.recurrence_custom
    }

/**
 * Chip-based recurrence picker: preset frequencies plus a natural-language
 * custom field with live preview. Never reports an unrecognized custom phrase
 * as a rule — [onRruleChange] only fires with a parsed RRULE (or null).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecurrencePicker(
    rrule: String?,
    onRruleChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // "Custom chosen but nothing parsed yet" leaves rrule null, which fromRrule
    // maps to NONE — so the custom selection is tracked separately.
    var customSelected by remember { mutableStateOf(false) }
    val parsedPreset = RecurrencePreset.fromRrule(rrule)
    val preset = if (customSelected) RecurrencePreset.CUSTOM else parsedPreset
    var customText by remember { mutableStateOf("") }
    val parser = remember { RecurrenceParser() }

    // An externally supplied preset rule (e.g. a title-token detection)
    // takes over and leaves custom editing mode.
    LaunchedEffect(parsedPreset) {
        if (parsedPreset != RecurrencePreset.CUSTOM && parsedPreset != RecurrencePreset.NONE) {
            customSelected = false
        }
    }

    Column(modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            RecurrencePreset.entries.forEach { option ->
                FilterChip(
                    selected = preset == option,
                    onClick = {
                        when (option) {
                            RecurrencePreset.NONE -> {
                                customSelected = false
                                customText = ""
                                onRruleChange(null)
                            }
                            RecurrencePreset.CUSTOM -> {
                                if (!customSelected) {
                                    customSelected = true
                                    customText = ""
                                    onRruleChange(null)
                                }
                            }
                            else -> {
                                customSelected = false
                                customText = ""
                                onRruleChange(option.rrule)
                            }
                        }
                    },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }

        if (preset == RecurrencePreset.CUSTOM) {
            Spacer(Modifier.height(4.dp))
            val parsed = parser.parse(customText)
            OutlinedTextField(
                value = customText,
                onValueChange = { text ->
                    customText = text
                    when (val result = parser.parse(text)) {
                        is RecurrenceResult.Success -> onRruleChange(result.rrule)
                        is RecurrenceResult.Unrecognized -> if (text.isBlank()) onRruleChange(null)
                    }
                },
                label = { Text(stringResource(R.string.quick_add_recurrence_hint)) },
                isError = customText.isNotBlank() && parsed is RecurrenceResult.Unrecognized,
                supportingText = {
                    when {
                        parsed is RecurrenceResult.Success ->
                            Text("🔁 ${RecurrenceParser.toDisplayLabel(parsed.rrule)}")
                        customText.isNotBlank() ->
                            Text(stringResource(R.string.recurrence_unrecognized))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }

        val current = rrule
        if (current != null) {
            Spacer(Modifier.height(4.dp))
            val label = when (preset) {
                RecurrencePreset.CUSTOM -> RecurrenceParser.toDisplayLabel(current)
                else -> stringResource(preset.labelRes)
            }
            Text(
                text = stringResource(R.string.task_detail_repeats, label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
