package app.tsosu.data.calendar

import app.tsosu.domain.recurrence.RecurrenceExpander
import kotlinx.datetime.LocalDate

data class IcsEvent(
    val uid: String,
    val summary: String,
    val start: LocalDate,
    val allDay: Boolean,
)

/**
 * Minimal RFC 5545 reader for ICS subscriptions: unfolds continuation lines,
 * unescapes TEXT values, and expands simple RRULEs (FREQ/INTERVAL/BYDAY/
 * BYMONTHDAY/UNTIL/COUNT are handled by the shared [RecurrenceExpander]).
 * Recurring occurrences are date-only projections of the DTSTART date.
 */
object IcsParser {

    fun parse(
        text: String,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): List<IcsEvent> {
        val events = mutableListOf<IcsEvent>()
        val lines = unfold(text)

        var i = 0
        while (i < lines.size) {
            if (!lines[i].startsWith("BEGIN:VEVENT")) {
                i++
                continue
            }
            val props = mutableMapOf<String, String>()
            i++
            while (i < lines.size && !lines[i].startsWith("END:VEVENT")) {
                val line = lines[i]
                val colon = line.indexOf(':')
                if (colon > 0) {
                    val name = line.substring(0, colon).substringBefore(';').uppercase()
                    // Later properties win (VALARM DESCRIPTION must not
                    // overwrite the event's — VALARM blocks end at END:VALARM
                    // and are skipped by only keeping the first DESCRIPTION).
                    if (name == "DESCRIPTION") {
                        props.putIfAbsent(name, unescape(line.substring(colon + 1)))
                    } else {
                        props[name] = unescape(line.substring(colon + 1))
                    }
                }
                i++
            }
            parseEvent(props, windowStart, windowEnd)?.let(events::addAll)
            i++
        }
        return events
    }

    private fun parseEvent(
        props: Map<String, String>,
        windowStart: LocalDate,
        windowEnd: LocalDate,
    ): List<IcsEvent>? {
        val dtStart = props["DTSTART"] ?: return null
        val date = parseDate(dtStart) ?: return null
        val uid = props["UID"] ?: "${date.toEpochDays()}-${props["SUMMARY"]}"
        val summary = props["SUMMARY"] ?: "(no title)"
        val allDay = !dtStart.contains("T")

        val rule = props["RRULE"]?.let { "RRULE:" + it.substringAfter("RRULE:") }
        if (rule == null) {
            return if (date in windowStart..windowEnd) {
                listOf(IcsEvent(uid, summary, date, allDay))
            } else {
                null
            }
        }

        // Expand the series inside the visible window.
        val occurrences = mutableListOf<IcsEvent>()
        var cursor = date
        var guard = 0
        while (cursor <= windowEnd && guard < 5_000) {
            if (cursor >= windowStart) {
                occurrences.add(IcsEvent(uid, summary, cursor, allDay))
            }
            cursor = RecurrenceExpander.nextDueDate(rule, cursor, cursor) ?: break
            guard++
        }
        return occurrences
    }

    internal fun parseDate(value: String): LocalDate? {
        val compact = value.trim()
        return runCatching {
            LocalDate(
                compact.substring(0, 4).toInt(),
                compact.substring(4, 6).toInt(),
                compact.substring(6, 8).toInt(),
            )
        }.getOrNull()
    }

    private fun unfold(text: String): List<String> = buildList {
        for (raw in text.split("\r\n", "\n")) {
            if (raw.startsWith(" ") || raw.startsWith("\t")) {
                if (isNotEmpty()) {
                    set(size - 1, last() + raw.substring(1))
                    continue
                }
            }
            add(raw)
        }
    }

    private fun unescape(value: String): String = value
        .replace("\\n", "\n")
        .replace("\\N", "\n")
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
