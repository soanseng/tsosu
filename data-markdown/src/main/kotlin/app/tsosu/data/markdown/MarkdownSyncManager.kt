package app.tsosu.data.markdown

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.Task

class MarkdownSyncManager(
    private val fileAccess: MarkdownFileAccess,
    private val taskSerializer: MarkdownTaskSerializer,
    private val taskParser: MarkdownTaskParser,
    private val habitSerializer: MarkdownHabitSerializer,
    private val habitParser: MarkdownHabitParser,
) {
    suspend fun exportTasks(tasks: List<Task>, projectNames: Map<String, String>) {
        val content = taskSerializer.serialize(tasks, projectNames)
        fileAccess.writeTasksFile(content)
    }

    suspend fun importTasks(): ParsedTasks {
        val content = fileAccess.readTasksFile()
            ?: return ParsedTasks(emptyList(), emptyMap())
        return taskParser.parse(content)
    }

    suspend fun exportHabits(habits: List<Habit>, completions: List<HabitCompletion>) {
        val content = habitSerializer.serialize(habits, completions)
        fileAccess.writeHabitsFile(content)
    }

    suspend fun importHabits(): ParsedHabits {
        val content = fileAccess.readHabitsFile()
            ?: return ParsedHabits(emptyList(), emptyList())
        return habitParser.parse(content)
    }
}
