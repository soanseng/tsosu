package app.tsosu.data.calendar

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class VEventBuilder {

    private val icalDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    fun buildVEvent(
        uid: String,
        title: String,
        description: String,
        dueDate: String,
        estimatedMinutes: Int?,
        reminderMinutesBefore: Int? = null,
    ): String {
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Tsosu//NONSGML v1//EN")
            append(buildVEventBody(uid, title, description, dueDate, estimatedMinutes, reminderMinutesBefore))
            appendLine("END:VCALENDAR")
        }
    }

    fun buildVEventBody(
        uid: String,
        title: String,
        description: String,
        dueDate: String,
        estimatedMinutes: Int?,
        reminderMinutesBefore: Int? = null,
    ): String {
        val dt = LocalDateTime.parse(dueDate)
        val dtStart = dt.format(icalDateFormat)
        val duration = estimatedMinutes ?: 60

        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:$uid")
            appendLine("DTSTART:$dtStart")
            appendLine("DURATION:PT${duration}M")
            appendLine("SUMMARY:${escapeIcal(title)}")
            if (description.isNotEmpty()) {
                appendLine("DESCRIPTION:${escapeIcal(description)}")
            }
            if (reminderMinutesBefore != null) {
                appendLine("BEGIN:VALARM")
                appendLine("TRIGGER:-PT${reminderMinutesBefore}M")
                appendLine("ACTION:DISPLAY")
                appendLine("DESCRIPTION:${escapeIcal(title)}")
                appendLine("END:VALARM")
            }
            appendLine("END:VEVENT")
        }
    }

    private fun escapeIcal(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}
