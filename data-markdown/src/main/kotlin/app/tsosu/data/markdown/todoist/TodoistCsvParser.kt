package app.tsosu.data.markdown.todoist

import app.tsosu.domain.recurrence.RecurrenceParser
import app.tsosu.domain.recurrence.RecurrenceResult
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime

data class TodoistImportResult(
    val tasks: List<Task>,
    val warnings: List<String>,
)

class TodoistCsvParser(
    private val recurrenceParser: RecurrenceParser,
) {

    fun parse(csvContent: String): TodoistImportResult {
        if (csvContent.isBlank()) return TodoistImportResult(emptyList(), emptyList())

        val lines = parseCsvLines(csvContent.trimStart('\uFEFF'))
        if (lines.size <= 1) return TodoistImportResult(emptyList(), emptyList())

        val header = lines[0].map { it.lowercase().trim() }
        val colIndex = buildColumnIndex(header)
            ?: return TodoistImportResult(emptyList(), listOf("Invalid CSV: missing TYPE or CONTENT column"))

        val warnings = mutableListOf<String>()
        val flatTasks = mutableListOf<ParsedRow>()

        for (i in 1 until lines.size) {
            val fields = lines[i]
            val type = fields.getOrNull(colIndex.type)?.trim()?.lowercase() ?: continue
            if (type != "task") continue

            val content = fields.getOrNull(colIndex.content)?.trim() ?: continue
            if (content.isBlank()) continue

            val description = colIndex.description.takeIf { it >= 0 }
                ?.let { fields.getOrNull(it)?.trim() } ?: ""
            val priorityCsv = colIndex.priority.takeIf { it >= 0 }
                ?.let { fields.getOrNull(it)?.trim()?.toIntOrNull() } ?: 4
            val indent = colIndex.indent.takeIf { it >= 0 }
                ?.let { fields.getOrNull(it)?.trim()?.toIntOrNull() } ?: 1
            val dateStr = colIndex.date.takeIf { it >= 0 }
                ?.let { fields.getOrNull(it)?.trim() } ?: ""

            val priority = mapPriority(priorityCsv)
            val (dueDate, recurrenceRule, dateWarning) = parseDate(dateStr, content)

            var finalDescription = description
            if (dateWarning != null) {
                warnings.add(dateWarning)
                val recurrenceNote = "Todoist recurrence: $dateStr"
                finalDescription = if (description.isNotBlank()) {
                    "$description\n$recurrenceNote"
                } else {
                    recurrenceNote
                }
            }

            val now = Clock.System.now()
            val task = Task(
                title = content,
                description = finalDescription,
                status = TaskStatus.TODO,
                priority = priority,
                dueDate = dueDate,
                recurrenceRule = recurrenceRule,
                createdAt = now,
                updatedAt = now,
            )

            flatTasks.add(ParsedRow(task = task, indent = indent))
        }

        val topLevel = buildTaskTree(flatTasks)
        return TodoistImportResult(topLevel, warnings)
    }

    private fun parseDate(dateStr: String, taskTitle: String): DateParseResult {
        if (dateStr.isBlank()) return DateParseResult(null, null, null)

        // Try as a concrete date: YYYY-MM-DD or YYYY-MM-DD at HH:MM
        val dateMatch = DATE_PATTERN.find(dateStr)
        if (dateMatch != null) {
            val year = dateMatch.groupValues[1].toInt()
            val month = dateMatch.groupValues[2].toInt()
            val day = dateMatch.groupValues[3].toInt()
            val dueDate = LocalDateTime(year, month, day, 0, 0)
            return DateParseResult(dueDate, null, null)
        }

        // Try as recurrence
        val result = recurrenceParser.parse(dateStr)
        return when (result) {
            is RecurrenceResult.Success -> DateParseResult(null, result.rrule, null)
            is RecurrenceResult.Unrecognized -> DateParseResult(
                null,
                null,
                "Unrecognized recurrence for \"$taskTitle\": $dateStr",
            )
        }
    }

    private fun mapPriority(todoistPriority: Int): Priority = when (todoistPriority) {
        1 -> Priority.URGENT
        2 -> Priority.HIGH
        3 -> Priority.MEDIUM
        else -> Priority.NONE
    }

    private fun buildTaskTree(rows: List<ParsedRow>): List<Task> {
        val topLevel = mutableListOf<Task>()
        // Stack: indent level → mutable list of tasks at that level, and reference to parent
        val parentStack = mutableMapOf<Int, MutableList<Task>>()
        var positionCounter = 0.0

        for (row in rows) {
            val task = row.task.copy(position = positionCounter++)
            val indent = row.indent

            if (indent <= 1) {
                topLevel.add(task)
                parentStack.clear()
                parentStack[1] = mutableListOf(task)
            } else {
                // Find the parent at indent - 1
                val parentLevel = indent - 1
                val siblings = parentStack[parentLevel]
                if (siblings != null && siblings.isNotEmpty()) {
                    val parent = siblings.last()
                    val updatedParent = parent.copy(subtasks = parent.subtasks + task)

                    // Update the parent in its container
                    if (parentLevel <= 1) {
                        topLevel[topLevel.lastIndex] = findAndReplace(topLevel.last(), parent, updatedParent)
                    } else {
                        // Recursively update the top-level ancestor
                        updateAncestors(topLevel, parentStack, parentLevel, parent, updatedParent)
                    }

                    // Update stack references
                    siblings[siblings.lastIndex] = updatedParent
                    parentStack[indent] = mutableListOf(task)
                } else {
                    // Orphan — treat as top-level
                    topLevel.add(task)
                    parentStack.clear()
                    parentStack[1] = mutableListOf(task)
                }
            }
        }

        return topLevel
    }

    private fun updateAncestors(
        topLevel: MutableList<Task>,
        parentStack: MutableMap<Int, MutableList<Task>>,
        level: Int,
        oldTask: Task,
        newTask: Task,
    ) {
        // Walk up the stack replacing each ancestor
        var currentOld = oldTask
        var currentNew = newTask

        for (l in level downTo 2) {
            val upperSiblings = parentStack[l - 1] ?: break
            if (upperSiblings.isEmpty()) break

            val upperParent = upperSiblings.last()
            val updatedChildren = upperParent.subtasks.map {
                if (it.id == currentOld.id) currentNew else it
            }
            val updatedUpperParent = upperParent.copy(subtasks = updatedChildren)

            upperSiblings[upperSiblings.lastIndex] = updatedUpperParent
            currentOld = upperParent
            currentNew = updatedUpperParent
        }

        // Finally update top-level
        val topIdx = topLevel.indexOfLast { it.id == currentOld.id }
        if (topIdx >= 0) {
            topLevel[topIdx] = currentNew
        }
    }

    private fun findAndReplace(root: Task, old: Task, new: Task): Task {
        if (root.id == old.id) return new
        val updatedChildren = root.subtasks.map {
            if (it.id == old.id) new else findAndReplace(it, old, new)
        }
        return root.copy(subtasks = updatedChildren)
    }

    // ── CSV Parsing ──

    private fun parseCsvLines(csv: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val reader = CsvLineReader(csv)
        while (reader.hasMore()) {
            val line = reader.readLine()
            if (line != null) {
                result.add(line)
            }
        }
        return result
    }

    private data class ParsedRow(val task: Task, val indent: Int)

    private data class DateParseResult(
        val dueDate: LocalDateTime?,
        val recurrenceRule: String?,
        val warning: String?,
    )

    private data class ColumnIndex(
        val type: Int,
        val content: Int,
        val description: Int,
        val priority: Int,
        val indent: Int,
        val date: Int,
    )

    private fun buildColumnIndex(header: List<String>): ColumnIndex? {
        val type = header.indexOf("type")
        val content = header.indexOf("content")
        if (type < 0 || content < 0) return null
        return ColumnIndex(
            type = type,
            content = content,
            description = header.indexOf("description"),
            priority = header.indexOf("priority"),
            indent = header.indexOf("indent"),
            date = header.indexOf("date"),
        )
    }

    companion object {
        private val DATE_PATTERN = Regex("""(\d{4})-(\d{2})-(\d{2})""")
    }
}

/**
 * Simple RFC 4180 CSV reader that handles quoted fields with commas and escaped quotes.
 */
private class CsvLineReader(private val text: String) {
    private var pos = 0

    fun hasMore(): Boolean = pos < text.length

    fun readLine(): List<String>? {
        if (pos >= text.length) return null

        val fields = mutableListOf<String>()
        while (pos < text.length) {
            val (field, terminated) = readField()
            fields.add(field)
            if (terminated) break
        }

        // Skip empty trailing lines
        return if (fields.size == 1 && fields[0].isBlank() && pos >= text.length) null else fields
    }

    /**
     * Returns (fieldValue, lineTerminated).
     */
    private fun readField(): Pair<String, Boolean> {
        if (pos >= text.length) return "" to true

        return if (text[pos] == '"') {
            readQuotedField()
        } else {
            readUnquotedField()
        }
    }

    private fun readQuotedField(): Pair<String, Boolean> {
        pos++ // skip opening quote
        val sb = StringBuilder()
        while (pos < text.length) {
            val ch = text[pos]
            if (ch == '"') {
                pos++
                if (pos < text.length && text[pos] == '"') {
                    // Escaped quote
                    sb.append('"')
                    pos++
                } else {
                    // End of quoted field — skip comma or line ending
                    return finishField(sb.toString())
                }
            } else {
                sb.append(ch)
                pos++
            }
        }
        return sb.toString() to true
    }

    private fun readUnquotedField(): Pair<String, Boolean> {
        val start = pos
        while (pos < text.length && text[pos] != ',' && text[pos] != '\n' && text[pos] != '\r') {
            pos++
        }
        val value = text.substring(start, pos)
        return finishField(value)
    }

    private fun finishField(value: String): Pair<String, Boolean> {
        if (pos >= text.length) return value to true
        return when (text[pos]) {
            ',' -> {
                pos++
                value to false
            }
            '\r' -> {
                pos++
                if (pos < text.length && text[pos] == '\n') pos++
                value to true
            }
            '\n' -> {
                pos++
                value to true
            }
            else -> value to false
        }
    }
}
