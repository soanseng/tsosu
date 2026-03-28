package app.tsosu.data.markdown

import androidx.room.withTransaction
import app.tsosu.data.local.TsosuDatabase
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.data.markdown.recurrence.RecurrenceParser
import app.tsosu.data.markdown.todoist.TodoistCsvParser
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Task
import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult
import app.tsosu.domain.repository.ImportTarget
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class TodoistImportRepository(
    private val database: TsosuDatabase,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
) : ImportRepository {

    private val csvParser = TodoistCsvParser(RecurrenceParser())

    override suspend fun importFromTodoist(
        data: ByteArray,
        format: ImportFormat,
        target: ImportTarget,
    ): Result<ImportResult> = runCatching {
        val csvContent = data.toString(Charsets.UTF_8)
        val parseResult = csvParser.parse(csvContent)

        if (parseResult.tasks.isEmpty()) {
            return@runCatching ImportResult(
                tasksImported = 0,
                projectsImported = 0,
                labelsImported = 0,
                warnings = parseResult.warnings,
            )
        }

        var count = 0
        var projectsCreated = 0

        database.withTransaction {
            val projectId = resolveProjectId(target)
            if (target is ImportTarget.NewProject) {
                projectsCreated = 1
            }

            val tasksToInsert = assignProject(parseResult.tasks, projectId)
            for (task in tasksToInsert) {
                insertTaskWithSubtasks(task)
                count++
                count += countSubtasks(task)
            }
        }

        ImportResult(
            tasksImported = count,
            projectsImported = projectsCreated,
            labelsImported = 0,
            warnings = parseResult.warnings,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun resolveProjectId(target: ImportTarget): String? = when (target) {
        is ImportTarget.Inbox -> null
        is ImportTarget.ExistingProject -> target.projectId
        is ImportTarget.NewProject -> {
            val project = Project(
                id = Uuid.random().toString(),
                title = target.name,
            )
            projectDao.insert(project.toEntity())
            project.id
        }
    }

    private fun assignProject(tasks: List<Task>, projectId: String?): List<Task> {
        if (projectId == null) return tasks
        return tasks.map { task ->
            task.copy(
                projectId = projectId,
                subtasks = assignProject(task.subtasks, projectId),
            )
        }
    }

    private suspend fun insertTaskWithSubtasks(task: Task) {
        // Insert the task itself (without subtasks in the entity — Room doesn't store nested lists)
        taskDao.insert(task.toEntity())
        // Insert each subtask as a separate task (flattened with same projectId)
        for (subtask in task.subtasks) {
            insertTaskWithSubtasks(subtask)
        }
    }

    private fun countSubtasks(task: Task): Int {
        var count = 0
        for (subtask in task.subtasks) {
            count++
            count += countSubtasks(subtask)
        }
        return count
    }
}
