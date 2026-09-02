package app.tsosu.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.tsosu.R
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.ui.components.RecurrencePreset
import app.tsosu.ui.components.labelRes
import java.time.format.TextStyle
import java.util.Locale

/** Localized energy label with emoji prefix, e.g. "🫫 低". */
@Composable
fun EnergyLevel.localizedLabel(): String = "$emoji " + stringResource(
    when (this) {
        EnergyLevel.LOW -> R.string.energy_low
        EnergyLevel.MEDIUM -> R.string.energy_medium
        EnergyLevel.HIGH -> R.string.energy_high
    },
)

/** Localized priority name, e.g. "緊急". */
@Composable
fun Priority.localizedName(): String = stringResource(
    when (this) {
        Priority.NONE -> R.string.priority_none
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
        Priority.URGENT -> R.string.priority_urgent
    },
)

private val DAY_CODES = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU") // index = ISO day - 1

private fun dayCodeName(code: String): String? {
    val iso = DAY_CODES.indexOf(code) + 1
    if (iso == 0) return null
    return java.time.DayOfWeek.of(iso).getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

/**
 * Fully localized recurrence label: preset rules use string resources; custom
 * rules are composed from their RRULE parts (interval / weekdays /
 * day-of-month / until) instead of the parser's English-only text.
 */
@Composable
fun recurrenceDisplayLabel(rrule: String): String {
    val preset = RecurrencePreset.fromRrule(rrule)
    if (preset != RecurrencePreset.CUSTOM) return stringResource(preset.labelRes)

    val kv = rrule.removePrefix("RRULE:").split(";").mapNotNull { seg ->
        seg.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] }
    }.toMap()
    val interval = kv["INTERVAL"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val byDay = kv["BYDAY"]
    val byMonthDay = kv["BYMONTHDAY"]?.toIntOrNull()

    val base = when (kv["FREQ"]) {
        "DAILY" -> stringResource(R.string.recurrence_every_n_days, interval)
        "WEEKLY" -> {
            val days = byDay
                ?.split(",")
                ?.mapNotNull { dayCodeName(it) }
                .orEmpty()
            val freqLabel = if (interval > 1) {
                stringResource(R.string.recurrence_every_n_weeks, interval)
            } else {
                stringResource(R.string.recurrence_weekly)
            }
            freqLabel + when {
                days.isNotEmpty() ->
                    " " + stringResource(R.string.recurrence_weekly_on, days.joinToString("、"))
                else -> ""
            }
        }
        "MONTHLY" -> when {
            byMonthDay != null && interval > 1 ->
                stringResource(R.string.recurrence_every_n_months_on_day, interval, byMonthDay)
            byMonthDay != null ->
                stringResource(R.string.recurrence_monthly_on_day, byMonthDay)
            else -> stringResource(R.string.recurrence_every_n_months, interval)
        }
        "YEARLY" -> stringResource(R.string.recurrence_every_year)
        // Unknown structure: keep the raw rule visible rather than English prose.
        else -> rrule
    }

    val until = kv["UNTIL"]?.take(8)?.let { digits ->
        val month = digits.substring(4, 6).toIntOrNull()
        val day = digits.substring(6, 8).toIntOrNull()
        if (month != null && day != null && month in 1..12) {
            base + stringResource(R.string.recurrence_until_short, "$month/$day")
        } else {
            null
        }
    }
    return until ?: base
}
