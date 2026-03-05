package app.tsosu.data.vikunja.importer

import app.tsosu.domain.repository.ImportFormat

data class ImportedTask(
    val title: String,
    val description: String = "",
    val priority: Int = 0,
    val dueDate: String? = null,
    val projectName: String? = null,
)

data class ParseResult(
    val tasks: List<ImportedTask>,
    val projectNames: Set<String> = emptySet(),
)

class TodoistImporter {

    fun parse(data: ByteArray, format: ImportFormat): ParseResult {
        return when (format) {
            ImportFormat.TODOIST_CSV -> parseCsv(data.decodeToString())
            ImportFormat.TODOIST_JSON -> ParseResult(emptyList())
        }
    }

    private fun parseCsv(csv: String): ParseResult {
        val lines = csv.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return ParseResult(emptyList())

        val headers = lines[0].split(",").map { it.trim() }
        val typeIdx = headers.indexOf("TYPE")
        val contentIdx = headers.indexOf("CONTENT")
        val descIdx = headers.indexOf("DESCRIPTION")
        val priorityIdx = headers.indexOf("PRIORITY")
        val dateIdx = headers.indexOf("DATE")

        val tasks = mutableListOf<ImportedTask>()

        for (line in lines.drop(1)) {
            val cols = parseCsvLine(line)
            if (cols.getOrNull(typeIdx) != "task") continue

            tasks.add(
                ImportedTask(
                    title = cols.getOrElse(contentIdx) { "" },
                    description = cols.getOrElse(descIdx) { "" },
                    priority = mapTodoistPriority(cols.getOrNull(priorityIdx)?.toIntOrNull() ?: 4),
                    dueDate = cols.getOrNull(dateIdx)?.takeIf { it.isNotBlank() },
                )
            )
        }

        return ParseResult(tasks)
    }

    private fun parseCsvLine(line: String): List<String> {
        return line.split(",").map { it.trim() }
    }

    // Todoist: 1=highest (p1), 4=lowest (p4)
    // Tsosu: 0=NONE, 1=LOW, 2=MEDIUM, 3=HIGH, 4=URGENT
    private fun mapTodoistPriority(todoistPriority: Int): Int {
        return when (todoistPriority) {
            1 -> 4 // p1 -> URGENT
            2 -> 3 // p2 -> HIGH
            3 -> 2 // p3 -> MEDIUM
            else -> 0 // p4 -> NONE
        }
    }
}
