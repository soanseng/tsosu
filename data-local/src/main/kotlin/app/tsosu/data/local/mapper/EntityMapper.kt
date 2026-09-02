package app.tsosu.data.local.mapper

import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Label
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.RoutineTime
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    serverId = serverId,
    title = title,
    description = description,
    status = TaskStatus.fromOrdinal(status),
    dueDate = dueDate?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
    },
    scheduledDate = scheduledDate?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
    },
    startDate = startDate?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
    },
    reminderTime = reminderTimeMinutes?.let { LocalTime(it / 60, it % 60) },
    completedDate = completedDate?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
    },
    cancelledDate = cancelledDate?.let {
        Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
    },
    priority = Priority.fromValue(priority),
    projectId = projectId,
    position = position,
    recurrenceRule = recurrenceRule,
    calendarEventId = calendarEventId,
    estimatedMinutes = estimatedMinutes,
    isFocus = isFocus,
    timeSpentMinutes = timeSpentMinutes,
    tinyVersion = tinyVersion,
    routineTime = routineTime?.let { RoutineTime.fromOrdinal(it) },
    completions = completionsCsv.orEmpty()
        .split(",")
        .mapNotNull { it.trim().takeIf(String::isNotEmpty) }
        .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() },
    createdAt = Instant.fromEpochMilliseconds(createdAt),
    updatedAt = Instant.fromEpochMilliseconds(updatedAt),
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    serverId = serverId,
    title = title,
    description = description,
    status = status.ordinal,
    done = status.isDone,
    dueDate = dueDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    scheduledDate = scheduledDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    startDate = startDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    reminderTimeMinutes = reminderTime?.let { it.hour * 60 + it.minute },
    completedDate = completedDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    cancelledDate = cancelledDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    priority = priority.value,
    projectId = projectId,
    position = position,
    repeatAfterSeconds = null,
    recurrenceRule = recurrenceRule,
    calendarEventId = calendarEventId,
    estimatedMinutes = estimatedMinutes,
    isFocus = isFocus,
    tinyVersion = tinyVersion,
    routineTime = routineTime?.ordinal,
    completionsCsv = completions.takeIf { it.isNotEmpty() }
        ?.joinToString(",") { it.toString() },
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)


fun LabelEntity.toDomain(): Label = Label(
    id = id,
    serverId = serverId,
    title = title,
    color = color,
)

fun Label.toEntity(): LabelEntity = LabelEntity(
    id = id,
    serverId = serverId,
    title = title,
    color = color,
)

fun ProjectEntity.toDomain(): Project = Project(
    id = id,
    serverId = serverId,
    title = title,
    color = color,
    parentProjectId = parentProjectId,
    position = position,
    isFavorite = isFavorite,
    isRoutine = isRoutine,
)

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    serverId = serverId,
    title = title,
    color = color,
    parentProjectId = parentProjectId,
    position = position,
    isFavorite = isFavorite,
    isRoutine = isRoutine,
)
