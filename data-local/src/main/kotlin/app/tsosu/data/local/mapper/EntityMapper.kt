package app.tsosu.data.local.mapper

import app.tsosu.data.local.entity.DailyFocusEntity
import app.tsosu.data.local.entity.HabitCompletionEntity
import app.tsosu.data.local.entity.HabitEntity
import app.tsosu.data.local.entity.LabelEntity
import app.tsosu.data.local.entity.ProjectEntity
import app.tsosu.data.local.entity.RoutineEntity
import app.tsosu.data.local.entity.TaskEntity
import app.tsosu.domain.model.DailyFocus
import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import app.tsosu.domain.model.Label
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Project
import app.tsosu.domain.model.Routine
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
    energyLevel = EnergyLevel.fromOrdinal(energyLevel),
    isFocus = isFocus,
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
    energyLevel = energyLevel.ordinal,
    isFocus = isFocus,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)

fun HabitEntity.toDomain(): Habit = Habit(
    id = id,
    serverId = serverId,
    title = title,
    tinyVersion = tinyVersion,
    frequency = HabitFrequency.fromOrdinal(frequency),
    targetDaysPerWeek = targetDaysPerWeek,
    energyLevel = EnergyLevel.fromOrdinal(energyLevel),
    routineId = routineId,
    position = position,
    color = color,
    isArchived = isArchived,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

fun Habit.toEntity(): HabitEntity = HabitEntity(
    id = id,
    serverId = serverId,
    title = title,
    tinyVersion = tinyVersion,
    frequency = frequency.ordinal,
    targetDaysPerWeek = targetDaysPerWeek,
    energyLevel = energyLevel.ordinal,
    routineId = routineId,
    position = position,
    color = color,
    isArchived = isArchived,
    createdAt = createdAt.toEpochMilliseconds(),
)

fun HabitCompletionEntity.toDomain(): HabitCompletion = HabitCompletion(
    habitId = habitId,
    date = Instant.fromEpochMilliseconds(date)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    completedAt = Instant.fromEpochMilliseconds(completedAt),
)

fun HabitCompletion.toEntity(): HabitCompletionEntity = HabitCompletionEntity(
    habitId = habitId,
    date = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    completedAt = completedAt.toEpochMilliseconds(),
)

fun RoutineEntity.toDomain(habits: List<Habit> = emptyList()): Routine = Routine(
    id = id,
    serverId = serverId,
    title = title,
    timeOfDay = RoutineTime.fromOrdinal(timeOfDay),
    habits = habits,
)

fun Routine.toEntity(): RoutineEntity = RoutineEntity(
    id = id,
    serverId = serverId,
    title = title,
    timeOfDay = timeOfDay.ordinal,
)

fun DailyFocusEntity.toDomain(): DailyFocus = DailyFocus(
    date = Instant.fromEpochMilliseconds(date)
        .toLocalDateTime(TimeZone.currentSystemDefault()).date,
    taskIds = listOfNotNull(taskId1, taskId2, taskId3),
)

fun DailyFocus.toEntity(): DailyFocusEntity = DailyFocusEntity(
    date = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
    taskId1 = taskIds.getOrNull(0),
    taskId2 = taskIds.getOrNull(1),
    taskId3 = taskIds.getOrNull(2),
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
