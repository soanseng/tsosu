package app.tsosu.domain.recurrence

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
sealed class RecurrenceResult {
    data class Success(val rrule: String) : RecurrenceResult()
    data class Unrecognized(val original: String) : RecurrenceResult()
}

data class TitleRecurrence(
    val title: String,
    val rrule: String?,
    /** First occurrence date from a "starting <date>" modifier; null = none. */
    val startDate: LocalDate? = null,
    /** Reminder preset from a time-of-day keyword ("every morning" etc.). */
    val suggestedReminder: kotlinx.datetime.LocalTime? = null,
)

/** Modifier dates stripped from a recurrence phrase before the core parse. */
private data class RecurrenceModifiers(
    val untilDate: kotlinx.datetime.LocalDate? = null,
    val startDate: kotlinx.datetime.LocalDate? = null,
    /** Reminder preset from a time-of-day keyword ("every morning" etc.). */
    val suggestedReminder: kotlinx.datetime.LocalTime? = null,
)

class RecurrenceParser {

    fun parse(input: String): RecurrenceResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return RecurrenceResult.Unrecognized(input)

        val (core, modifiers) = stripModifiers(trimmed)
        val base = tryParseEnglish(core)
            ?: tryParseChinese(core)
            ?: return RecurrenceResult.Unrecognized(input)
        return RecurrenceResult.Success(applyUntil(base.rrule, modifiers.untilDate))
    }

    /** Full parse with modifiers; used by extractFromTitle for prefills. */
    private fun parseWithModifiers(input: String): Pair<String, RecurrenceModifiers>? {
        val (core, modifiers) = stripModifiers(input)
        val base = tryParseEnglish(core) ?: tryParseChinese(core) ?: return null
        return applyUntil(base.rrule, modifiers.untilDate) to modifiers
    }

    fun extractFromTitle(fullTitle: String): TitleRecurrence {
        // Try to find recurrence pattern at the end of the title.
        // Check for English "every ..." or Chinese "每..." patterns.

        val everyIndex = fullTitle.lastIndexOf(" every ", ignoreCase = true)
        if (everyIndex >= 0) {
            val candidate = fullTitle.substring(everyIndex + 1).trim()
            parseWithModifiers(candidate)?.let { (rrule, modifiers) ->
                return TitleRecurrence(
                    title = fullTitle.substring(0, everyIndex).trim(),
                    rrule = rrule,
                    startDate = modifiers.startDate,
                    suggestedReminder = modifiers.suggestedReminder,
                )
            }
        }

        // Todoist-style shorthand "… ev day / ev mon" at the end of the title
        val evMatch = Regex(" ev[! ]").findAll(fullTitle).lastOrNull()
        if (evMatch != null) {
            val candidate = fullTitle.substring(evMatch.range.first + 1).trim()
            parseWithModifiers(candidate)?.let { (rrule, modifiers) ->
                return TitleRecurrence(
                    title = fullTitle.substring(0, evMatch.range.first).trim(),
                    rrule = rrule,
                    startDate = modifiers.startDate,
                    suggestedReminder = modifiers.suggestedReminder,
                )
            }
        }

        // Check if the entire string starts with "every"
        if (fullTitle.trim().startsWith("every", ignoreCase = true)) {
            parseWithModifiers(fullTitle.trim())?.let { (rrule, modifiers) ->
                return TitleRecurrence(
                    title = "",
                    rrule = rrule,
                    startDate = modifiers.startDate,
                    suggestedReminder = modifiers.suggestedReminder,
                )
            }
        }

        // Chinese: find 每 with a space before it
        val meiIndex = fullTitle.lastIndexOf(" 每")
        if (meiIndex >= 0) {
            val candidate = fullTitle.substring(meiIndex + 1).trim()
            parseWithModifiers(candidate)?.let { (rrule, modifiers) ->
                return TitleRecurrence(
                    title = fullTitle.substring(0, meiIndex).trim(),
                    rrule = rrule,
                    startDate = modifiers.startDate,
                    suggestedReminder = modifiers.suggestedReminder,
                )
            }
        }

        // Check if the entire string starts with 每
        if (fullTitle.trim().startsWith("每")) {
            parseWithModifiers(fullTitle.trim())?.let { (rrule, modifiers) ->
                return TitleRecurrence(
                    title = "",
                    rrule = rrule,
                    startDate = modifiers.startDate,
                    suggestedReminder = modifiers.suggestedReminder,
                )
            }
        }

        return TitleRecurrence(title = fullTitle, rrule = null)
    }

    // ── Start/end modifiers (Batch H) ──

    /**
     * Strips trailing "until/starting <date>" modifiers (EN) or
     * "到/直到 <date>" / "从 <date> 开始" (ZH). Returns the core phrase
     * plus whatever dates were found. Modifier order is free.
     */
    private fun stripModifiers(input: String): Pair<String, RecurrenceModifiers> {
        var core = input.trim()
        var untilDate: kotlinx.datetime.LocalDate? = null
        var startDate: kotlinx.datetime.LocalDate? = null
        var suggestedReminder: kotlinx.datetime.LocalTime? = null

        // Repeat-stripping: all modifiers, any order, possibly repeated words.
        while (true) {
            val lower = core.lowercase()

            // Time-of-day keyword ("every morning" → FREQ=DAILY + 08:00).
            // Only meaningful on a daily core; strip and remember the preset.
            val todMatch = TIME_OF_DAY.find(lower)
            if (todMatch != null) {
                val preset = TIME_OF_DAY_PRESETS[todMatch.value]
                if (preset != null && suggestedReminder == null) suggestedReminder = preset
                core = (core.substring(0, todMatch.range.first) + core.substring(todMatch.range.last + 1))
                    .trim(' ', ',')
                // "every " left dangling (e.g. "every morning" → "every")
                core = core.replace(Regex("""\bevery\s*$"""), "").trim()
                continue
            }


            val untilMatch = UNTIL_EN.find(lower) ?: UNTIL_ZH.find(core)
            if (untilMatch != null) {
                val date = parseFlexibleDate(untilMatch.groupValues[1])
                    ?: return core to RecurrenceModifiers(untilDate, startDate, suggestedReminder)
                if (untilDate == null) untilDate = date
                core = (core.substring(0, untilMatch.range.first) + core.substring(untilMatch.range.last + 1))
                    .trim(' ', ',')
                continue
            }

            val startMatch = STARTING_EN.find(lower) ?: STARTING_ZH.find(core)
            if (startMatch != null) {
                val date = parseFlexibleDate(startMatch.groupValues[1])
                    ?: return core to RecurrenceModifiers(untilDate, startDate, suggestedReminder)
                if (startDate == null) startDate = date
                core = (core.substring(0, startMatch.range.first) + core.substring(startMatch.range.last + 1))
                    .trim(' ', ',')
                continue
            }
            break
        }

        return core to RecurrenceModifiers(untilDate, startDate, suggestedReminder)
    }

    /**
     * Flexible date: 8/31, 2026/8/31, 2026-08-31, aug 31, august 31 2027,
     * and Chinese 8月31 / 2026年8月31日. No year → current year.
     */
    internal fun parseFlexibleDate(raw: String): kotlinx.datetime.LocalDate? {
        val text = raw.trim()

        ZH_DATE.matchEntire(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: currentYear()
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return safeDate(year, month, day)
        }

        ISO_DATE.matchEntire(text)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return safeDate(year, month, day)
        }

        val lower = text.lowercase()
        MONTH_NAME_DATE.matchEntire(lower)?.let { match ->
            val month = MONTHS_EN.indexOf(match.groupValues[1].take(3)) + 1
            if (month == 0) return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: currentYear()
            return safeDate(year, month, day)
        }

        NUMERIC_DATE.matchEntire(text)?.let { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: currentYear()
            return safeDate(year, month, day)
        }

        return null
    }
    private fun applyUntil(rrule: String, until: kotlinx.datetime.LocalDate?): String {
        if (until == null) return rrule
        val rule = rrule.removePrefix("RRULE:")
        return "RRULE:$rule;UNTIL=${until.year.toString().padStart(4, '0')}" +
            "${until.monthNumber.toString().padStart(2, '0')}" +
            "${until.dayOfMonth.toString().padStart(2, '0')}T235959Z"
    }

    private fun currentYear(): Int =
        kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).year

    private fun safeDate(year: Int, month: Int, day: Int): kotlinx.datetime.LocalDate? = try {
        kotlinx.datetime.LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        null
    }


    // ── English ──

    private fun tryParseEnglish(input: String): RecurrenceResult.Success? {
        val normalized = input.lowercase().replace(Regex("\\s+"), " ").trim()
            // Todoist-style shorthand: "ev day", "ev mon", "ev! daily" → treat as "every"
            .replace(Regex("^ev[! ]\\s*"), "every ")
            // "every! 30 days" = restart-from-completion; same schedule shape,
            // flagged so callers can apply completion-based scheduling.
            .replace(Regex("^every!\\s*"), "every ")

        // "every daily/weekly/..." folds to the standalone word
        val folded = when (normalized) {
            "every daily" -> "daily"
            "every weekly" -> "weekly"
            "every monthly" -> "monthly"
            "every yearly" -> "yearly"
            "every quarterly" -> "quarterly"
            else -> normalized
        }

        // Standalone words: daily / weekly / monthly / yearly / quarterly
        when (folded) {
            "daily" -> return success("FREQ=DAILY")
            "weekly" -> return success("FREQ=WEEKLY")
            "monthly" -> return success("FREQ=MONTHLY")
            "yearly" -> return success("FREQ=YEARLY")
            "quarterly" -> return success("FREQ=MONTHLY;INTERVAL=3")
        }


        // "every other day/week/month/year" (Todoist-style)
        OTHER_EN.matchEntire(folded)?.let { match ->
            val unit = match.groupValues[1]
            val freq = when {
                unit.startsWith("day") -> "DAILY"
                unit.startsWith("week") -> "WEEKLY"
                unit.startsWith("month") -> "MONTHLY"
                unit.startsWith("year") -> "YEARLY"
                else -> return null
            }
            return success("FREQ=$freq;INTERVAL=2")
        }

        // "every day"
        if (folded == "every day") {
            return success("FREQ=DAILY")
        }

        // "every weekday"
        if (folded == "every weekday") {
            return success("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
        }

        // "every week"
        if (folded == "every week") {
            return success("FREQ=WEEKLY")
        }

        // "every month on the Nth"
        MONTHLY_DAY_EN.matchEntire(folded)?.let { match ->
            val day = match.groupValues[1].toInt()
            if (day in 1..31) return success("FREQ=MONTHLY;BYMONTHDAY=$day")
        }

        // "every month"
        if (folded == "every month") {
            return success("FREQ=MONTHLY")
        }

        // "every year"
        if (folded == "every year") {
            return success("FREQ=YEARLY")
        }

        // "every N days/weeks/months/years"
        INTERVAL_EN.matchEntire(folded)?.let { match ->
            val n = match.groupValues[1].toInt()
            val unit = match.groupValues[2]
            val freq = when {
                unit.startsWith("day") -> "DAILY"
                unit.startsWith("week") -> "WEEKLY"
                unit.startsWith("month") -> "MONTHLY"
                unit.startsWith("year") -> "YEARLY"
                else -> return null
            }
            return success("FREQ=$freq;INTERVAL=$n")
        }

        // "every Monday", "every Mon, Wed, Fri", "every Tuesday and Thursday"
        DAYS_EN.matchEntire(folded)?.let { match ->
            val daysStr = match.groupValues[1]
            val days = parseDayNamesEnglish(daysStr) ?: return null
            if (days.isNotEmpty()) {
                return success("FREQ=WEEKLY;BYDAY=${days.joinToString(",")}")
            }
        }

        return null
    }

    private fun parseDayNamesEnglish(input: String): List<String>? {
        // Split on commas, "and", spaces — then map to RRULE day codes
        val tokens = input
            .replace(",", " ")
            .replace(" and ", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val days = tokens.mapNotNull { EN_DAY_MAP[it.lowercase()] }
        return if (days.size == tokens.size && days.isNotEmpty()) days else null
    }

    // ── Chinese ──

    private fun tryParseChinese(input: String): RecurrenceResult.Success? {
        // 每個工作日
        if (input == "每個工作日" || input == "每个工作日") {
            return success("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
        }

        // 每天
        if (input == "每天" || input == "每日") {
            return success("FREQ=DAILY")
        }

        // 每月N號
        MONTHLY_DAY_ZH.matchEntire(input)?.let { match ->
            val day = match.groupValues[1].toInt()
            if (day in 1..31) return success("FREQ=MONTHLY;BYMONTHDAY=$day")
        }

        // 每月
        if (input == "每月") {
            return success("FREQ=MONTHLY")
        }

        // 每年
        if (input == "每年") {
            return success("FREQ=YEARLY")
        }

        // 每N天/週/周/月/年
        INTERVAL_ZH.matchEntire(input)?.let { match ->
            val numStr = match.groupValues[1]
            val unit = match.groupValues[2]
            val n = ZH_NUM_MAP[numStr] ?: numStr.toIntOrNull() ?: return null
            val freq = when (unit) {
                "天", "日" -> "DAILY"
                "週", "周" -> "WEEKLY"
                "月" -> "MONTHLY"
                "年" -> "YEARLY"
                else -> return null
            }
            return success("FREQ=$freq;INTERVAL=$n")
        }

        // 每週N / 每周N  (one or more Chinese day characters)
        WEEKLY_DAYS_ZH.matchEntire(input)?.let { match ->
            val daysStr = match.groupValues[1]
            val days = parseChineseDays(daysStr) ?: return null
            if (days.isNotEmpty()) {
                return success("FREQ=WEEKLY;BYDAY=${days.joinToString(",")}")
            }
        }

        // 每週 / 每周 (bare, no day specified)
        if (input == "每週" || input == "每周") {
            return success("FREQ=WEEKLY")
        }

        return null
    }

    private fun parseChineseDays(input: String): List<String>? {
        val days = mutableListOf<String>()
        for (char in input) {
            val day = ZH_DAY_MAP[char] ?: return null
            days.add(day)
        }
        return if (days.isNotEmpty()) days else null
    }

    private fun success(rule: String): RecurrenceResult.Success =
        RecurrenceResult.Success("RRULE:$rule")

    companion object {
        // Time-of-day keywords (Batch I): EN "morning/afternoon/evening/night",
        // ZH 早上/上午/下午/晚上. Preset reminder times follow Todoist defaults.
        private val TIME_OF_DAY = Regex("""\b(?:morning|afternoon|evening|night|早上|上午|下午|晚上|傍晚)\b""")
        private val TIME_OF_DAY_PRESETS = mapOf(
            "morning" to kotlinx.datetime.LocalTime(8, 0),
            "早上" to kotlinx.datetime.LocalTime(8, 0),
            "上午" to kotlinx.datetime.LocalTime(8, 0),
            "afternoon" to kotlinx.datetime.LocalTime(13, 0),
            "下午" to kotlinx.datetime.LocalTime(13, 0),
            "evening" to kotlinx.datetime.LocalTime(18, 0),
            "傍晚" to kotlinx.datetime.LocalTime(18, 0),
            "night" to kotlinx.datetime.LocalTime(21, 0),
            "晚上" to kotlinx.datetime.LocalTime(21, 0),
        )

        private val MONTHLY_DAY_EN = Regex("""every month on the (\d+)(?:st|nd|rd|th)""")
        private val OTHER_EN = Regex("""every other (days?|weeks?|months?|years?)""")
        private val DAYS_EN = Regex("""every (.+)""")
        private val INTERVAL_EN = Regex("""every (\d+) (days?|weeks?|months?|years?)""")

        // Start/end modifiers (Batch H). Date alternation: ISO, M/d[/yyyy], month-name day [year], ZH 月日.
        private const val DATE_ALT = """(\d{4}-\d{1,2}-\d{1,2}|\d{1,2}/\d{1,2}(?:/\d{2,4})?|[a-z]{3,9}\.?\s+\d{1,2}(?:\s+\d{4})?|(?:\d{4}年)?\d{1,2}月\d{1,2}[日号]?)"""
        private val UNTIL_EN = Regex("""\b(?:until|through|till)\s+$DATE_ALT""")
        private val STARTING_EN = Regex("""\b(?:starting|from)\s+$DATE_ALT""")
        private val UNTIL_ZH = Regex("""(?:直到|到|至)\s*((?:\d{4}年)?\d{1,2}月\d{1,2}[日号]?)""")
        private val STARTING_ZH = Regex("""(?:从|自)\s*((?:\d{4}年)?\d{1,2}月\d{1,2}[日号]?)(?:\s*开始)?""")

        private val ISO_DATE = Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""")
        private val NUMERIC_DATE = Regex("""(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?""")
        private val MONTH_NAME_DATE = Regex("""([a-z]{3,9})\.?\s+(\d{1,2})(?:\s+(\d{4}))?""")
        private val ZH_DATE = Regex("""(?:(\d{4})年)?(\d{1,2})月(\d{1,2})[日号]?""")

        private val MONTHS_EN = listOf(
            "jan", "feb", "mar", "apr", "may", "jun",
            "jul", "aug", "sep", "oct", "nov", "dec",
        )

        private val MONTHS_EN_DISPLAY = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        )


        private val MONTHLY_DAY_ZH = Regex("""每月(\d+)[號号]""")
        private val INTERVAL_ZH = Regex("""每([一二三四五六七八九十兩两\d]+)([天日週周月年])""")
        private val WEEKLY_DAYS_ZH = Regex("""每[週周]([一二三四五六日]+)""")

        private val EN_DAY_MAP = mapOf(
            "monday" to "MO", "mon" to "MO",
            "tuesday" to "TU", "tue" to "TU", "tues" to "TU",
            "wednesday" to "WE", "wed" to "WE",
            "thursday" to "TH", "thu" to "TH", "thur" to "TH", "thurs" to "TH",
            "friday" to "FR", "fri" to "FR",
            "saturday" to "SA", "sat" to "SA",
            "sunday" to "SU", "sun" to "SU",
        )

        private val ZH_DAY_MAP = mapOf(
            '一' to "MO",
            '二' to "TU",
            '三' to "WE",
            '四' to "TH",
            '五' to "FR",
            '六' to "SA",
            '日' to "SU",
        )

        private val ZH_NUM_MAP = mapOf(
            "一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5,
            "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10,
            "兩" to 2, "两" to 2,
        )

        private val RRULE_DAY_DISPLAY = mapOf(
            "MO" to "Mon", "TU" to "Tue", "WE" to "Wed", "TH" to "Thu",
            "FR" to "Fri", "SA" to "Sat", "SU" to "Sun",
        )

        fun toDisplayLabel(rrule: String): String {
            val rule = rrule.removePrefix("RRULE:")
            val parts = rule.split(";").mapNotNull { segment ->
                val split = segment.split("=", limit = 2)
                if (split.size == 2) split[0] to split[1] else null
            }.toMap()
            val freq = parts["FREQ"] ?: return rrule
            val interval = parts["INTERVAL"]?.toIntOrNull()
            val byDay = parts["BYDAY"]
            val byMonthDay = parts["BYMONTHDAY"]?.toIntOrNull()

            // Weekdays shorthand
            if (freq == "WEEKLY" && byDay == "MO,TU,WE,TH,FR" && interval == null) {
                return "Every weekday"
            }

            // Weekly with specific days
            if (freq == "WEEKLY" && byDay != null && interval == null) {
                val dayLabels = byDay.split(",").map { RRULE_DAY_DISPLAY[it] ?: it }
                return "Every ${dayLabels.joinToString(", ")}"
            }

            // Monthly with day
            if (freq == "MONTHLY" && byMonthDay != null) {
                return "Every month on day $byMonthDay"
            }

            val freqLabel = when (freq) {
                "DAILY" -> "day"
                "WEEKLY" -> "week"
                "MONTHLY" -> "month"
                "YEARLY" -> "year"
                else -> return rrule
            }

            val baseLabel = if (interval != null && interval > 1) {
                "Every $interval ${freqLabel}s"
            } else {
                "Every $freqLabel"
            }

            // "until <date>" suffix (Batch H)
            val until = parts["UNTIL"]
            val untilLabel = until?.take(8)?.let { digits ->
                val year = digits.substring(0, 4).toIntOrNull()
                val month = digits.substring(4, 6).toIntOrNull()
                val day = digits.substring(6, 8).toIntOrNull()
                if (year != null && month != null && day != null && month in 1..12) {
                    " until ${MONTHS_EN_DISPLAY[month - 1]} $day"
                } else {
                    ""
                }
            } ?: ""

            return baseLabel + untilLabel
        }
    }
}
