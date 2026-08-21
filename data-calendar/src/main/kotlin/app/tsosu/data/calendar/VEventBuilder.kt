package app.tsosu.data.calendar

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * RFC 5545-compliant VEVENT serialization: mandatory DTSTAMP, UTF-8-safe
 * 75-octet line folding, and full TEXT escaping (backslash, comma, semicolon,
 * CRLF/CR/LF).
 */
class VEventBuilder {

    private val icalDateFormat = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val utcStampFormat =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC)

    fun buildVEvent(
        uid: String,
        title: String,
        description: String,
        dueDate: String,
        estimatedMinutes: Int?,
        reminderMinutesBefore: Int? = null,
        dtStampEpochMillis: Long = System.currentTimeMillis(),
    ): String {
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Tsosu//NONSGML v1//EN")
            append(buildVEventBody(uid, title, description, dueDate, estimatedMinutes, reminderMinutesBefore, dtStampEpochMillis))
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
        dtStampEpochMillis: Long = System.currentTimeMillis(),
    ): String {
        val dt = LocalDateTime.parse(dueDate)
        val dtStart = dt.format(icalDateFormat)
        val duration = estimatedMinutes ?: 60

        return buildString {
            appendLine("BEGIN:VEVENT")
            appendFolded("UID:$uid")
            appendFolded("DTSTAMP:${utcStampFormat.format(Instant.ofEpochMilli(dtStampEpochMillis))}")
            appendFolded("DTSTART:$dtStart")
            appendFolded("DURATION:PT${duration}M")
            appendFolded("SUMMARY:${escapeIcal(title)}")
            if (description.isNotEmpty()) {
                appendFolded("DESCRIPTION:${escapeIcal(description)}")
            }
            if (reminderMinutesBefore != null) {
                appendLine("BEGIN:VALARM")
                appendFolded("TRIGGER:-PT${reminderMinutesBefore}M")
                appendLine("ACTION:DISPLAY")
                appendFolded("DESCRIPTION:${escapeIcal(title)}")
                appendLine("END:VALARM")
            }
            appendLine("END:VEVENT")
        }
    }

    private fun StringBuilder.appendFolded(line: String) {
        foldIcsLine(line).forEach { appendLine(it) }
    }

    /**
     * RFC 5545 §3.1: content lines are limited to 75 octets; continuation
     * lines start with a single space. Never splits a multi-byte UTF-8
     * character (CJK titles hit the limit quickly).
     */
    internal fun foldIcsLine(line: String): List<String> {
        val bytes = line.toByteArray(Charsets.UTF_8)
        if (bytes.size <= 75) return listOf(line)

        val out = mutableListOf<String>()
        var offset = 0
        while (offset < bytes.size) {
            val limit = if (out.isEmpty()) 75 else 74
            var end = minOf(offset + limit, bytes.size)
            // Back off to the last lead byte so a continuation never starts
            // mid-character.
            while (end > offset && end < bytes.size && (bytes[end].toInt() and 0xC0) == 0x80) {
                end--
            }
            val chunk = String(bytes, offset, end - offset, Charsets.UTF_8)
            out.add(if (out.isEmpty()) chunk else " $chunk")
            offset = end
        }
        return out
    }

    private fun escapeIcal(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\r\n", "\\n")
            .replace("\r", "\\n")
            .replace("\n", "\\n")
    }
}
