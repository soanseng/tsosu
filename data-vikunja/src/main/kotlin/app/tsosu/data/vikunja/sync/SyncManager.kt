package app.tsosu.data.vikunja.sync

import app.tsosu.data.local.dao.LabelDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.data.vikunja.api.VikunjaApi
import app.tsosu.data.vikunja.dto.VikunjaLabelTaskDto
import app.tsosu.data.vikunja.mapper.VikunjaRoutineMapper
import app.tsosu.data.vikunja.mapper.VikunjaTaskMapper
import app.tsosu.domain.model.EnergyLevel
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SyncManager(
    private val api: VikunjaApi,
    private val taskDao: TaskDao,
    private val projectDao: ProjectDao,
    private val labelDao: LabelDao,
    private val energyLabelManager: EnergyLabelManager,
    private val taskMapper: VikunjaTaskMapper,
) {
    private val routineMapper = VikunjaRoutineMapper()

    suspend fun pullTasks(): Int {
        var page = 1
        var totalPulled = 0

        while (true) {
            val tasks = api.getAllTasks(page = page)
            if (tasks.isEmpty()) break

            for (dto in tasks) {
                upsertTaskFromDto(dto)
                totalPulled++
            }
            page++
        }

        return totalPulled
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun upsertTaskFromDto(dto: app.tsosu.data.vikunja.dto.VikunjaTaskDto) {
        val existing = taskDao.getByServerId(dto.id)
        val fields = taskMapper.dtoToDomainFields(dto)
        val now = Clock.System.now().toEpochMilliseconds()

        val entity = TaskEntity(
            id = existing?.id ?: Uuid.random().toString(),
            serverId = dto.id,
            title = dto.title,
            description = fields.cleanDescription,
            done = dto.done,
            dueDate = taskMapper.parseDueDate(dto.dueDate),
            priority = fields.priority.value,
            projectId = existing?.projectId,
            position = dto.position,
            repeatAfterSeconds = if (dto.repeatAfter > 0) dto.repeatAfter else null,
            calendarEventId = existing?.calendarEventId,
            estimatedMinutes = fields.estimatedMinutes,
            energyLevel = (fields.energyLevel ?: EnergyLevel.MEDIUM).ordinal,
            isFocus = existing?.isFocus ?: false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            syncStatus = 0,
        )

        taskDao.upsert(entity)
    }

    suspend fun pullProjects(): Int {
        var page = 1
        var totalPulled = 0

        while (true) {
            val projects = api.getProjects(page = page)
            if (projects.isEmpty()) break

            for (dto in projects) {
                upsertProjectFromDto(dto)
                totalPulled++
            }
            page++
        }

        return totalPulled
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun upsertProjectFromDto(dto: app.tsosu.data.vikunja.dto.VikunjaProjectDto) {
        val existing = projectDao.getByServerId(dto.id)
        val isRoutine = routineMapper.isRoutineProject(dto)

        val entity = ProjectEntity(
            id = existing?.id ?: Uuid.random().toString(),
            serverId = dto.id,
            title = dto.title,
            color = if (dto.hexColor.isNotEmpty()) "#${dto.hexColor}" else "#808080",
            parentProjectId = null,
            position = dto.position,
            isFavorite = dto.isFavorite,
            isRoutine = isRoutine,
        )

        projectDao.upsert(entity)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun pullLabels() {
        val labels = api.getLabels(page = 1, perPage = 200)
        for (dto in labels) {
            val existing = labelDao.getByServerId(dto.id)
            val entity = LabelEntity(
                id = existing?.id ?: Uuid.random().toString(),
                serverId = dto.id,
                title = dto.title,
                color = if (dto.hexColor.isNotEmpty()) "#${dto.hexColor}" else "#4287f5",
            )
            labelDao.upsert(entity)
        }
    }

    suspend fun pushTask(entity: TaskEntity) {
        val projectServerId = entity.projectId?.let { projectDao.getByIdSync(it)?.serverId } ?: 1L
        val energyLevel = EnergyLevel.fromOrdinal(entity.energyLevel)

        val dto = taskMapper.domainToDto(
            title = entity.title,
            description = entity.description,
            done = entity.done,
            dueDate = taskMapper.formatDueDate(entity.dueDate),
            priority = entity.priority,
            projectId = projectServerId,
            position = entity.position,
            estimatedMinutes = entity.estimatedMinutes,
            repeatAfterSeconds = entity.repeatAfterSeconds,
            hexColor = "",
        )

        val serverId = entity.serverId
        if (serverId != null) {
            api.updateTask(serverId, dto)
            val labelId = energyLabelManager.getLabelId(energyLevel)
            if (labelId != null) {
                api.attachLabel(serverId, VikunjaLabelTaskDto(labelId))
            }
        } else {
            val created = api.createTask(projectServerId, dto)
            taskDao.updateServerId(entity.id, created.id)
            val labelId = energyLabelManager.getLabelId(energyLevel)
            if (labelId != null) {
                api.attachLabel(created.id, VikunjaLabelTaskDto(labelId))
            }
        }
    }

    suspend fun deleteTask(serverId: Long) {
        api.deleteTask(serverId)
    }
}
