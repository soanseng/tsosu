package app.tsosu.data.markdown.ticktick

import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime

data class TickTickImportResult(
    val tasks: List<Task>,
    val warnings: List<String>,
)

/**
 * Reader for TickTick's CSV export
 * (Folder Name, List Name, Task Name, Task Content, Is All Day, Start Date,
 * Due Date, Reminder, Priority, Status, Created Time, Completed Time,
 * Order, Task ID, Parent ID, Project ID, Tags).
 *
 * Dates accept `yyyy-MM-dd` / `yyyy/MM/dd` with an optional `HH:mm`.
 * Priority maps High/Medium/Low/None; Status "Completed"/"2" is done.
 * Tags ride along in the description until label plumbing exists.
 */
class TickTickCsvParser {

    fun parse(csvContent: String): TickTickImportResult {
        val lines = parseCsvLines(csvContent.trimStart('\uFEFF').trim())
        if (lines.size <= 1) return TickTickImportResult(emptyList(), emptyList())

        val header = lines[0].map { it.trim().lowercase() }
        val col = { name: String -> header.indexOfFirst { it == name }.takeIf { it >= 0 } }

        val cTitle = col("task name") ?: return TickTickImportResult(
            emptyList(),
            listOf("Invalid CSV: missing Task Name column"),
        )
        val cContent = col("task content")
        val cDue = col("due date")
        val cPriority = col("priority")
        val cStatus = col("status")
        val cTags = col("tags")
        val cOrder = col("order")

        val warnings = mutableListOf<String>()
        val tasks = mutableListOf<Task>()
        val now = Clock.System.now()

        for (i in 1 until lines.size) {
            val f = lines[i]
            val title = f.getOrNull(cTitle)?.trim().orEmpty()
            if (title.isEmpty()) continue

            val due = cDue?.let { f.getOrNull(it) }?.let { parseDate(it) }
            val priority = cPriority?.let { f.getOrNull(it) }?.let { mapPriority(it) } ?: Priority.NONE
            val status = cStatus?.let { f.getOrNull(it) }?.let { mapStatus(it) } ?: TaskStatus.TODO
            val tags = cTags?.let { f.getOrNull(it) }?.split(",")?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                .orEmpty()
            val description = buildString {
                cContent?.let { idx -> f.getOrNull(idx)?.trim() }?.takeIf { it.isNotEmpty() }?.let {
                    append(it)
                    if (tags.isNotEmpty()) append("\n")
                }
                if (tags.isNotEmpty()) append(tags.joinToString(" ") { "#$it" })
            }
            val position = cOrder?.let { f.getOrNull(it) }?.trim()?.toDoubleOrNull() ?: tasks.size.toDouble()

            val task = Task(
                title = title,
                description = description,
                status = status,
                priority = priority,
                dueDate = due,
                position = position,
                createdAt = now,
                updatedAt = now,
            )
            tasks.add(task)
        }
        return TickTickImportResult(tasks, warnings)
    }

    private fun mapPriority(raw: String): Priority = when (raw.trim().lowercase()) {
        "high" -> Priority.HIGH
        "medium" -> Priority.MEDIUM
        "low" -> Priority.LOW
        else -> Priority.NONE
    }

    private fun mapStatus(raw: String): TaskStatus = when (raw.trim().lowercase()) {
        "completed", "2" -> TaskStatus.DONE
        "abandoned", "-1" -> TaskStatus.CANCELLED
        else -> TaskStatus.TODO
    }

    internal fun parseDate(raw: String): LocalDateTime? {
        val text = raw.trim().removeSuffix("Z")
        val match = DATE_PATTERN.find(text) ?: return null
        val (y, mo, d) = match.destructured
        val time = TIME_PATTERN.find(text.substringAfter(match.value))
        return runCatching {
            LocalDateTime(
                y.toInt(),
                mo.toInt(),
                d.toInt(),
                time?.groupValues?.get(1)?.toInt() ?: 0,
                time?.groupValues?.get(2)?.toInt() ?: 0,
            )
        }.getOrNull()
    }

    private companion object {
        val DATE_PATTERN = Regex("""(\d{4})[-/](\d{1,2})[-/](\d{1,2})""")
        val TIME_PATTERN = Regex("""(\d{1,2}):(\d{2})""")
    }
}

/** Shared RFC 4180 reader (mirrors the Todoist parser's rules). */
private fun parseCsvLines(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var row = mutableListOf<String>()
    var field = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                field.append('"'); i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                row.add(field.toString()); field = StringBuilder()
            }
            (c == '\n' || c == '\r') && !inQuotes -> {
                if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                row.add(field.toString())
                if (row.any { it.isNotBlank() }) rows.add(row)
                row = mutableListOf(); field = StringBuilder()
            }
            else -> field.append(c)
        }
        i++
    }
    row.add(field.toString())
    if (row.any { it.isNotBlank() }) rows.add(row)
    return rows
}
