package app.tsosu.data.vikunja.repository

import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.data.vikunja.importer.TodoistImporter
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ImportRepositoryImpl(
    private val taskDao: TaskDao,
    private val importer: TodoistImporter,
) : ImportRepository {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun importFromTodoist(data: ByteArray, format: ImportFormat): Result<ImportResult> {
        return try {
            val parseResult = importer.parse(data, format)
            val now = Clock.System.now().toEpochMilliseconds()

            for (imported in parseResult.tasks) {
                val entity = TaskEntity(
                    id = Uuid.random().toString(),
                    title = imported.title,
                    description = imported.description,
                    priority = imported.priority,
                    energyLevel = EnergyLevel.MEDIUM.ordinal,
                    createdAt = now,
                    updatedAt = now,
                )
                taskDao.insert(entity)
            }

            Result.success(
                ImportResult(
                    tasksImported = parseResult.tasks.size,
                    projectsImported = 0,
                    labelsImported = 0,
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
