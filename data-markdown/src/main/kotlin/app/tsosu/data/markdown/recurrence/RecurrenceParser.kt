package app.tsosu.data.markdown.recurrence

sealed class RecurrenceResult {
    data class Success(val rrule: String) : RecurrenceResult()
    data class Unrecognized(val original: String) : RecurrenceResult()
}

data class TitleRecurrence(
    val title: String,
    val rrule: String?,
)

class RecurrenceParser {

    fun parse(input: String): RecurrenceResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return RecurrenceResult.Unrecognized(input)

        return tryParseEnglish(trimmed)
            ?: tryParseChinese(trimmed)
            ?: RecurrenceResult.Unrecognized(input)
    }

    fun extractFromTitle(fullTitle: String): TitleRecurrence {
        // Try to find recurrence pattern at the end of the title.
        // Check for English "every ..." or Chinese "每..." patterns.

        val everyIndex = fullTitle.lastIndexOf(" every ", ignoreCase = true)
        if (everyIndex >= 0) {
            val candidate = fullTitle.substring(everyIndex + 1).trim()
            val result = parse(candidate)
            if (result is RecurrenceResult.Success) {
                return TitleRecurrence(
                    title = fullTitle.substring(0, everyIndex).trim(),
                    rrule = result.rrule,
                )
            }
        }

        // Check if the entire string starts with "every"
        if (fullTitle.trim().startsWith("every", ignoreCase = true)) {
            val result = parse(fullTitle.trim())
            if (result is RecurrenceResult.Success) {
                return TitleRecurrence(title = "", rrule = result.rrule)
            }
        }

        // Chinese: find 每 with a space before it
        val meiIndex = fullTitle.lastIndexOf(" 每")
        if (meiIndex >= 0) {
            val candidate = fullTitle.substring(meiIndex + 1).trim()
            val result = parse(candidate)
            if (result is RecurrenceResult.Success) {
                return TitleRecurrence(
                    title = fullTitle.substring(0, meiIndex).trim(),
                    rrule = result.rrule,
                )
            }
        }

        // Check if the entire string starts with 每
        if (fullTitle.trim().startsWith("每")) {
            val result = parse(fullTitle.trim())
            if (result is RecurrenceResult.Success) {
                return TitleRecurrence(title = "", rrule = result.rrule)
            }
        }

        return TitleRecurrence(title = fullTitle, rrule = null)
    }

    // ── English ──

    private fun tryParseEnglish(input: String): RecurrenceResult.Success? {
        val normalized = input.lowercase().replace(Regex("\\s+"), " ").trim()

        // "every day"
        if (normalized == "every day") {
            return success("FREQ=DAILY")
        }

        // "every weekday"
        if (normalized == "every weekday") {
            return success("FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR")
        }

        // "every week"
        if (normalized == "every week") {
            return success("FREQ=WEEKLY")
        }

        // "every month on the Nth"
        MONTHLY_DAY_EN.matchEntire(normalized)?.let { match ->
            val day = match.groupValues[1].toInt()
            return success("FREQ=MONTHLY;BYMONTHDAY=$day")
        }

        // "every month"
        if (normalized == "every month") {
            return success("FREQ=MONTHLY")
        }

        // "every year"
        if (normalized == "every year") {
            return success("FREQ=YEARLY")
        }

        // "every N days/weeks/months/years"
        INTERVAL_EN.matchEntire(normalized)?.let { match ->
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
        DAYS_EN.matchEntire(normalized)?.let { match ->
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
            return success("FREQ=MONTHLY;BYMONTHDAY=$day")
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
        private val MONTHLY_DAY_EN = Regex("""every month on the (\d+)(?:st|nd|rd|th)""")
        private val INTERVAL_EN = Regex("""every (\d+) (days?|weeks?|months?|years?)""")
        private val DAYS_EN = Regex("""every (.+)""")

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

            return if (interval != null && interval > 1) {
                "Every $interval ${freqLabel}s"
            } else {
                "Every $freqLabel"
            }
        }
    }
}
