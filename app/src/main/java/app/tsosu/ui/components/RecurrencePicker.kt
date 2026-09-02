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
import app.tsosu.ui.util.recurrenceDisplayLabel
import java.time.format.TextStyle
import java.util.Locale

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

val RecurrencePreset.labelRes: Int
    get() = when (this) {
        RecurrencePreset.NONE -> R.string.recurrence_none
        RecurrencePreset.DAILY -> R.string.recurrence_daily
        RecurrencePreset.WEEKDAYS -> R.string.recurrence_weekdays
        RecurrencePreset.WEEKLY -> R.string.recurrence_weekly
        RecurrencePreset.MONTHLY -> R.string.recurrence_monthly
        RecurrencePreset.CUSTOM -> R.string.recurrence_custom
    }

/** Editable form of a custom rule the builder UI can chip-select. */
data class CustomRecurrenceSpec(
    val freq: String = "DAILY", // DAILY | WEEKLY | MONTHLY
    val interval: Int = 1,
    /** ISO weekdays (1=Mon..7=Sun); only meaningful for WEEKLY. */
    val weekdays: Set<Int> = emptySet(),
    /** Day of month 1..31; only meaningful for MONTHLY. */
    val monthDay: Int? = null,
)

private val DAY_CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU") // index = ISO day - 1

fun isoDayName(isoDay: Int): String =
    java.time.DayOfWeek.of(isoDay).getDisplayName(TextStyle.SHORT, Locale.getDefault())

fun parseCustomSpec(rrule: String?): CustomRecurrenceSpec {
    if (rrule == null) return CustomRecurrenceSpec()
    val kv = rrule.removePrefix("RRULE:").split(";").mapNotNull { seg ->
        seg.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
    }.toMap()
    val freq = when (kv["FREQ"]) {
        "WEEKLY" -> "WEEKLY"
        "MONTHLY" -> "MONTHLY"
        else -> "DAILY"
    }
    val weekdays = kv["BYDAY"]
        ?.split(",")
        ?.mapNotNull { code -> DAY_CODES.indexOf(code).takeIf { it >= 0 }?.plus(1) }
        ?.toSet()
        ?: emptySet()
    return CustomRecurrenceSpec(
        freq = freq,
        interval = kv["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1,
        weekdays = weekdays,
        monthDay = kv["BYMONTHDAY"]?.toIntOrNull()?.takeIf { it in 1..31 },
    )
}

fun CustomRecurrenceSpec.toRrule(): String = buildString {
    append("RRULE:FREQ=").append(freq)
    if (interval > 1) append(";INTERVAL=").append(interval)
    if (freq == "WEEKLY" && weekdays.isNotEmpty()) {
        append(";BYDAY=")
        append(weekdays.sorted().joinToString(",") { DAY_CODES[it - 1] })
    }
    if (freq == "MONTHLY" && monthDay != null) append(";BYMONTHDAY=").append(monthDay)
}

/**
 * Chip-based recurrence picker: preset frequencies plus a custom mode with a
 * structured builder (interval / weekday / day-of-month chips) and a
 * natural-language field with live preview. Never reports an unrecognized
 * custom phrase as a rule — [onRruleChange] only fires with a parsed RRULE
 * (or null).
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
    fun emitBuilderSpec(spec: CustomRecurrenceSpec) {
        customText = ""
        onRruleChange(spec.toRrule())
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
            val spec = remember(rrule) { parseCustomSpec(rrule) }

            Spacer(Modifier.height(8.dp))

            // Frequency unit: 天 / 週 / 月
            Text(
                stringResource(R.string.recurrence_builder_freq),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf(
                    "DAILY" to R.string.recurrence_unit_day,
                    "WEEKLY" to R.string.recurrence_unit_week,
                    "MONTHLY" to R.string.recurrence_unit_month,
                ).forEach { (freq, label) ->
                    FilterChip(
                        selected = spec.freq == freq,
                        onClick = {
                            emitBuilderSpec(
                                when (freq) {
                                    "WEEKLY" -> spec.copy(freq = freq, monthDay = null)
                                    "MONTHLY" -> spec.copy(freq = freq, weekdays = emptySet())
                                    else -> spec.copy(freq = freq, weekdays = emptySet(), monthDay = null)
                                },
                            )
                        },
                        label = { Text(stringResource(label)) },
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Interval chips per frequency
            val intervalOptions = when (spec.freq) {
                "WEEKLY" -> listOf(1, 2, 3, 4)
                "MONTHLY" -> listOf(1, 2, 3, 6)
                else -> listOf(1, 2, 3, 7)
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                intervalOptions.forEach { n ->
                    val label = when {
                        spec.freq == "DAILY" && n == 1 -> stringResource(R.string.recurrence_daily)
                        spec.freq == "WEEKLY" && n == 1 -> stringResource(R.string.recurrence_weekly)
                        spec.freq == "MONTHLY" && n == 1 -> stringResource(R.string.recurrence_monthly)
                        spec.freq == "DAILY" -> stringResource(R.string.recurrence_every_n_days, n)
                        spec.freq == "WEEKLY" -> stringResource(R.string.recurrence_every_n_weeks, n)
                        else -> stringResource(R.string.recurrence_every_n_months, n)
                    }
                    FilterChip(
                        selected = spec.interval == n,
                        onClick = { emitBuilderSpec(spec.copy(interval = n)) },
                        label = { Text(label) },
                    )
                }
            }

            if (spec.freq == "WEEKLY") {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.recurrence_builder_weekdays),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..7).forEach { iso ->
                        val selected = iso in spec.weekdays
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val next = if (selected) spec.weekdays - iso else spec.weekdays + iso
                                emitBuilderSpec(spec.copy(weekdays = next))
                            },
                            label = { Text(isoDayName(iso)) },
                        )
                    }
                }
            }

            if (spec.freq == "MONTHLY") {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.recurrence_builder_month_day),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    (1..31).forEach { day ->
                        FilterChip(
                            selected = spec.monthDay == day,
                            onClick = { emitBuilderSpec(spec.copy(monthDay = day)) },
                            label = { Text("$day") },
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Free-text keywords as an alternative to the builder
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
                            Text("🔁 ${recurrenceDisplayLabel(parsed.rrule)}")
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
            Text(
                text = stringResource(R.string.task_detail_repeats, recurrenceDisplayLabel(current)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
