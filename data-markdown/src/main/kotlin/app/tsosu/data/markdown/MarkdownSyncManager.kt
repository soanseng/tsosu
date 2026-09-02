package app.tsosu.data.markdown

import app.tsosu.data.markdown.dailynote.DailyNoteWriter
import app.tsosu.data.markdown.index.TaskIndexGenerator
import app.tsosu.data.markdown.tasknote.TaskNoteParser
import app.tsosu.data.markdown.tasknote.TaskNoteSerializer
import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDate

class MarkdownSyncManager(
    private val fileAccess: MarkdownFileAccess,
    private val taskSerializer: MarkdownTaskSerializer,
    private val taskParser: MarkdownTaskParser,
    private val taskNoteSerializer: TaskNoteSerializer = TaskNoteSerializer(),
    private val taskNoteParser: TaskNoteParser = TaskNoteParser(),
    private val dailyNoteWriter: DailyNoteWriter = DailyNoteWriter(),
    private val taskIndexGenerator: TaskIndexGenerator = TaskIndexGenerator(),
) {
    suspend fun exportTasks(
        tasks: List<Task>,
        projectNames: Map<String, String>,
        conflictIds: Set<String> = emptySet(),
    ) {
        fileAccess.ensureFolder("tasks")
        val noteFilenames = mutableMapOf<String, String>()

        for (task in tasks) {
            if (shouldCreateTaskNote(task)) {
                val slug = taskNoteSerializer.slugify(task.title)
                val filename = "$slug-${task.id.take(8)}.md"
                val content = taskNoteSerializer.serialize(task, projectNames[task.projectId])
                writeNoteIfChanged("tasks", filename, content)
                noteFilenames[task.id] = filename.removeSuffix(".md")
            }
        }

        // Regenerate index; skip write when unchanged (incremental sync)
        val indexContent = taskIndexGenerator.generate(tasks, projectNames, noteFilenames, conflictIds)
        if (fileAccess.readTasksFile() != indexContent) {
            fileAccess.writeTasksFile(indexContent)
        }
    }

    suspend fun importTasks(): ParsedTasks {
        val allTasks = mutableListOf<Task>()
        val projectSections = mutableMapOf<String, String>()
        val noteTaskIds = mutableSetOf<String>()

        // 1. Read individual TaskNote files (source of truth)
        val noteFiles = fileAccess.listFolder("tasks")
        for (filename in noteFiles) {
            if (!filename.endsWith(".md")) continue
            val content = fileAccess.readFileInFolder("tasks", filename) ?: continue
            try {
                val parsed = taskNoteParser.parse(content)
                // Legacy pre-id-suffix files and new files can both exist for the same id
                if (parsed.task.id in noteTaskIds) continue
                allTasks.add(parsed.task)
                noteTaskIds.add(parsed.task.id)
                parsed.projectName?.let { projectSections[parsed.task.id] = it }
            } catch (_: Exception) {
                // skip malformed files
            }
        }

        // 2. Read index file for inline-only tasks
        val indexContent = fileAccess.readTasksFile()
        if (indexContent != null) {
            val indexParsed = taskParser.parse(indexContent)
            for (task in indexParsed.tasks) {
                if (task.id !in noteTaskIds) {
                    allTasks.add(task)
                }
            }
            for ((id, section) in indexParsed.projectSections) {
                if (id !in projectSections) {
                    projectSections[id] = section
                }
            }
        }

        return ParsedTasks(allTasks, projectSections)
    }

    /**
     * Daily note habit checklist is the recurring-task series: completed =
     * today's occurrence recorded in the task's completions.
     */
    suspend fun exportDailyNote(
        date: LocalDate,
        recurringTasks: List<Task>,
        completedTaskIds: Set<String>,
    ) {
        fileAccess.ensureFolder("daily")
        val content = dailyNoteWriter.write(date, recurringTasks, completedTaskIds)
        writeNoteIfChanged("daily", dailyNoteWriter.filename(date), content)
    }

    /**
     * Writes [content] only when the existing file differs — avoids rewriting
     * unchanged notes on every sync (reduces SAF traffic and Obsidian sync churn).
     */
    private suspend fun writeNoteIfChanged(folder: String, filename: String, content: String) {
        val existing = fileAccess.readFileInFolder(folder, filename)
        if (existing != content) {
            fileAccess.writeFileInFolder(folder, filename, content)
        }
    }

    private fun shouldCreateTaskNote(task: Task): Boolean {
        return task.description.isNotBlank() || task.subtasks.isNotEmpty()
    }
}
