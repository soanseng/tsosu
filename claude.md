# Tsosu — ADHD-Friendly Task Manager for Vikunja

> tsosu.app — 台語「做事」(tsò-sū)
> Designed by a psychiatrist with ADHD. Built for minds that work differently.

## Project Overview

Tsosu is a **Todoist-style** native Android client for [Vikunja](https://vikunja.io/), designed with ADHD minds as the primary audience. Every design decision is shaped by clinical understanding of ADHD, Atomic Habits principles, and lived experience.

**10 Core Features**:
1. 🎯 Focus 3 — pick 3 tasks, that's your day
2. 🔁 Daily Habits — Atomic Habits style, mapped to Vikunja repeating tasks
3. 😌 No Shame UI — warm language, no overdue counts
4. ⏱ Time Awareness — estimates, over-schedule prevention
5. ⚡ Energy Matching — tag tasks by energy needed
6. 🎲 Pick One For Me — random selector beats decision paralysis
7. 🔔 Gentle Nudge — supportive notifications
8. 🎉 Weekly Review — celebration, not report card
9. 🧹 Stale Task Cleanup — gentle archive suggestions
10. 📅 Calendar Auto-Sync — set date → on your calendar

## Vikunja API Integration Strategy

### Vikunja Task Fields Available for Sync

```
✅ Synced to Vikunja:
   title, description, done, doneAt
   dueDate, startDate, endDate
   priority (int64)
   projectId, position
   labels (many-to-many)
   assignees
   reminders
   repeatAfter (int64, seconds), repeatMode
   percentDone
   hexColor
   isFavorite
   attachments, comments
   relatedTasks (subtask, blocking, etc.)
   uid (CalDAV UUID)

❌ NOT in Vikunja (Tsosu-specific):
   estimatedMinutes
   energyLevel
   isFocus
   calendarEventId
   DailyFocus
   HabitCompletion (local tracking)
   Routine
```

### Per-Feature Sync Strategy

| Feature | Field | Strategy | Details |
|---------|-------|----------|---------|
| Tasks (core) | title, done, dueDate, priority, labels, project | ✅ **Direct sync** | Standard Vikunja API |
| 🎯 Focus 3 | isFocus, DailyFocus | 🔶 **Local only** | Resets daily, no sync needed |
| 🔁 Habits | Habit entity | ✅ **Repeating task** | Habit → Vikunja task with `repeatAfter` |
| 🔁 Habit completion | HabitCompletion | 🔶 **Local tracking** | Vikunja tracks done/undone, we track history locally |
| 🔁 Routine | Routine entity | 🔶 **Project** | Routine → Vikunja project (e.g. "🌅 Morning Routine") |
| ⏱ Time estimate | estimatedMinutes | 📝 **Description metadata** | `<!-- tsosu:{"est":30} -->` in description |
| ⚡ Energy level | energyLevel | 🏷 **Label mapping** | Labels "⚡high" / "😐medium" / "🪫low" |
| 📅 Calendar event ID | calendarEventId | 🔶 **Local only** | Per-device calendar differs |
| 🎲 Pick One | — | — | Pure client-side logic |
| 😌 No Shame UI | — | — | Pure UI design |
| 🔔 Gentle Nudge | — | — | Local notifications |
| 🎉 Weekly Review | — | — | Computed from local DB |
| 🧹 Stale Cleanup | — | — | Query from local DB |

### Habit ↔ Vikunja Repeating Task Mapping

Habits are stored as Vikunja repeating tasks. This means they sync across devices and are visible in Vikunja web UI.

```
Tsosu Habit                    Vikunja Task
─────────────────────────────────────────────────
habit.title                  → task.title
habit.tinyVersion            → task.description (first line)
habit.frequency DAILY        → task.repeatAfter = 86400 (24h in seconds)
habit.frequency WEEKDAYS     → task.repeatAfter = 86400 + repeatMode = 0
habit.frequency CUSTOM (3/wk)→ task.repeatAfter = 86400 (track locally which days)
habit.energyLevel            → label "⚡high" / "😐medium" / "🪫low"
habit.routineId              → task.projectId (routine = project)
habit.position               → task.position
habit.color                  → task.hexColor
completing a habit           → mark task done → Vikunja auto-creates next occurrence

Routine                      → Vikunja Project
─────────────────────────────────────────────────
routine.title "🌅 Morning"  → project.title "🌅 Morning Routine"
routine.timeOfDay            → (stored in project description as metadata)
routine habits               → tasks in this project with repeatAfter > 0
```

**How completion works**:

```
1. User taps habit checkbox in Tsosu
2. → PUT /api/v1/tasks/{id} with done=true
3. → Vikunja server auto-creates next occurrence (repeatAfter logic)
4. → Tsosu records HabitCompletion locally (for streak tracking)
5. → Next sync pulls the new task occurrence
```

**Flexible streak tracking** (local):

Vikunja only knows "done or not done" for each occurrence. Tsosu tracks `HabitCompletion` records locally to calculate:
- completedLast7Days
- completedLast30Days
- completionRate

This is fine because streak stats are personal — they don't need to sync.

### Energy Level ↔ Vikunja Labels

```kotlin
// On first sync / setup, create these labels in Vikunja:
val ENERGY_LABELS = mapOf(
    EnergyLevel.HIGH   to Label(title = "⚡high", color = "#4CAF50"),
    EnergyLevel.MEDIUM to Label(title = "😐medium", color = "#FFC107"),
    EnergyLevel.LOW    to Label(title = "🪫low", color = "#90A4AE")
)

// When syncing a task:
// - Tsosu energyLevel → add corresponding label
// - When pulling from Vikunja → check labels → set energyLevel
// - Task can only have ONE energy label (replace, not accumulate)
```

### Time Estimate ↔ Description Metadata

```kotlin
// Append to description when syncing to Vikunja:
fun appendEstimate(description: String, minutes: Int?): String {
    val cleaned = description.replace(Regex("<!-- tsosu:.*?-->"), "").trimEnd()
    return if (minutes != null) {
        "$cleaned\n<!-- tsosu:{\"est\":$minutes} -->"
    } else cleaned
}

// Parse when pulling from Vikunja:
fun extractEstimate(description: String): Int? {
    return Regex("<!-- tsosu:\\{\"est\":(\\d+)\\}").find(description)
        ?.groupValues?.get(1)?.toIntOrNull()
}

// This is invisible in Vikunja web UI (HTML comment)
// Other Vikunja clients will ignore it
```

## Architecture

```
┌──────────────────────────────────────────────┐
│  Presentation (Jetpack Compose, Material 3)  │
│  Focus 3, Habits, Pick One, Gentle Nudge     │
├──────────────────────────────────────────────┤
│  Domain (Pure Kotlin — ZERO Android deps)    │
│  Repository Interfaces, Models, Use Cases    │
├──────────────────────────────────────────────┤
│  Data Layer                                  │
│  ┌─────────────┐ ┌────────────────────────┐  │
│  │ data-local/  │ │ data-vikunja/ (opt-in) │  │
│  │ Room DB      │ │ Vikunja REST API       │  │
│  │ (primary)    │ │ (sync engine)          │  │
│  └─────────────┘ └────────────────────────┘  │
│  ┌─────────────┐ ┌────────────────────────┐  │
│  │ data-import/ │ │ data-calendar/         │  │
│  │ Todoist CSV  │ │ CalDAV / Google (future)│  │
│  └─────────────┘ └────────────────────────┘  │
└──────────────────────────────────────────────┘
```

### Licensing
- Tsosu: closed-source, proprietary
- Vikunja: AGPL-3.0 — HTTP REST API only

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI**: Jetpack Compose + Material 3 + Material You
- **Architecture**: MVVM + Clean Architecture + TDD
- **Networking**: Retrofit 2 + OkHttp + Kotlin Serialization
- **Local DB**: Room (primary store)
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **CalDAV**: ical4j + OkHttp
- **Testing**: JUnit 5, Turbine, MockK, Robolectric, Compose UI tests
- **Localization**: English, 繁體中文 (zh-TW)
- **Min SDK**: 26 / **Target SDK**: 35

## Project Structure

```
tsosu/
├── app/
│   ├── src/main/java/app/tsosu/
│   │   ├── TsosuApp.kt
│   │   ├── MainActivity.kt
│   │   ├── navigation/
│   │   ├── ui/screens/
│   │   │   ├── focus/          # Focus 3
│   │   │   ├── habits/         # Daily Habits + Routines
│   │   │   ├── inbox/
│   │   │   ├── today/
│   │   │   ├── upcoming/
│   │   │   ├── project/
│   │   │   ├── taskdetail/
│   │   │   ├── habitdetail/
│   │   │   ├── quickadd/
│   │   │   ├── pickone/        # Pick One 🎲
│   │   │   ├── weeklyreview/   # Weekly Review 🎉
│   │   │   ├── search/
│   │   │   ├── settings/
│   │   │   └── import/
│   │   ├── ui/components/
│   │   ├── ui/widget/
│   │   ├── ui/theme/
│   │   ├── di/
│   │   ├── notification/       # Gentle Nudge
│   │   └── receiver/
│   ├── main/res/values/strings.xml
│   ├── main/res/values-zh-rTW/strings.xml
│   ├── test/
│   └── androidTest/
│
├── domain/                     # Pure Kotlin
│   ├── model/
│   ├── repository/
│   └── usecase/
│
├── data-local/                 # Room primary store
├── data-vikunja/               # Vikunja API + sync engine
│   ├── api/                    # Retrofit (from OpenAPI)
│   ├── dto/                    # Vikunja DTOs
│   ├── mapper/                 # ★ DTO ↔ Domain + metadata encoding
│   └── sync/                   # SyncManager + HabitSyncManager
├── data-calendar/              # CalDAV / Google
├── data-import/                # Todoist CSV/JSON
│
└── build.gradle.kts
```

## Domain Layer

### Models

```kotlin
// ── Tasks ──

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val dueDate: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val labels: List<Label> = emptyList(),
    val projectId: String? = null,
    val position: Double = 0.0,
    val subtasks: List<Task> = emptyList(),
    val repeatAfter: Duration? = null,
    val calendarEventId: String? = null,
    // ADHD fields (sync strategy noted)
    val estimatedMinutes: Int? = null,           // → description metadata
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,  // → label
    val isFocus: Boolean = false,                // → local only
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)

enum class Priority(val value: Int, val color: Long) {
    NONE(0, 0xFF808080), LOW(1, 0xFF4A90D9),
    MEDIUM(2, 0xFFF5A623), HIGH(3, 0xFFEB8909), URGENT(4, 0xFFD1453B)
}

enum class EnergyLevel(val emoji: String, val labelTitle: String) {
    LOW("🪫", "🪫low"),
    MEDIUM("😐", "😐medium"),
    HIGH("🔋", "⚡high")
}

// ── Habits (maps to Vikunja repeating tasks) ──

data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Long? = null,                  // Vikunja task ID
    val title: String,
    val tinyVersion: String? = null,             // "Sit, take 3 breaths"
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val targetDaysPerWeek: Int = 7,
    val energyLevel: EnergyLevel = EnergyLevel.LOW,
    val routineId: String? = null,               // → Vikunja projectId
    val position: Double = 0.0,
    val color: String = "#4CAF50",               // → Vikunja hexColor
    val isArchived: Boolean = false,
    val createdAt: Instant = Clock.System.now()
)

enum class HabitFrequency(val repeatAfterSeconds: Long) {
    DAILY(86400),       // 24 hours
    WEEKDAYS(86400),    // 24 hours (skip weekends locally)
    CUSTOM(86400)       // 24 hours (track target days locally)
}

// Local-only: streak tracking
data class HabitCompletion(
    val habitId: String,
    val date: LocalDate,
    val completedAt: Instant
)

data class HabitStreakInfo(
    val habitId: String,
    val habitTitle: String,
    val completedLast7Days: Int,
    val completedLast30Days: Int,
    val currentConsecutiveDays: Int,
    val completionRate: Float
)

// ── Routines (maps to Vikunja projects) ──

data class Routine(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Long? = null,                  // Vikunja project ID
    val title: String,
    val timeOfDay: RoutineTime,
    val habits: List<Habit> = emptyList()
)

enum class RoutineTime(val emoji: String) {
    MORNING("🌅"), AFTERNOON("☀️"), EVENING("🌙")
}

// ── Focus ──

data class DailyFocus(
    val date: LocalDate,
    val taskIds: List<String>,       // max 3, local only
    val completedCount: Int = 0
)

data class WeeklyReview(
    val weekStart: LocalDate,
    val tasksCompleted: Int,
    val habitsCompletedTotal: Int,
    val focusDaysCompleted: Int,
    val totalEstimatedMinutes: Int,
    val topProject: String?,
    val longestHabitStreak: HabitStreakInfo?
)

data class Project(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Long? = null,
    val title: String,
    val color: String = "#808080",
    val parentProjectId: String? = null,
    val position: Double = 0.0,
    val isFavorite: Boolean = false,
    val isRoutine: Boolean = false     // ★ marks this project as a Routine container
)

data class Label(
    val id: String = UUID.randomUUID().toString(),
    val serverId: Long? = null,
    val title: String,
    val color: String = "#4287f5"
)
```

### Repository Interfaces

```kotlin
interface TaskRepository {
    fun getInboxTasks(): Flow<List<Task>>
    fun getTodayTasks(): Flow<List<Task>>
    fun getUpcomingTasks(days: Int = 7): Flow<List<Task>>
    fun getTasksForProject(projectId: String): Flow<List<Task>>
    fun getTask(taskId: String): Flow<Task?>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getFocusTasks(date: LocalDate = today()): Flow<List<Task>>
    fun getTasksByEnergy(level: EnergyLevel): Flow<List<Task>>
    fun getStaleTaskIds(olderThanDays: Int = 14): Flow<List<String>>
    suspend fun createTask(task: Task): Result<Task>
    suspend fun updateTask(task: Task): Result<Task>
    suspend fun deleteTask(taskId: String): Result<Unit>
    suspend fun toggleDone(taskId: String): Result<Task>
    suspend fun reorder(taskId: String, newPosition: Double): Result<Unit>
    suspend fun setFocus(taskId: String, isFocus: Boolean): Result<Task>
    suspend fun clearFocus(date: LocalDate = today()): Result<Unit>
    suspend fun archiveTasks(taskIds: List<String>): Result<Int>
}

interface HabitRepository {
    fun getActiveHabits(): Flow<List<Habit>>
    fun getHabitsForRoutine(routineId: String): Flow<List<Habit>>
    fun getHabit(habitId: String): Flow<Habit?>
    fun getTodayCompletions(): Flow<List<HabitCompletion>>
    fun getStreakInfo(habitId: String): Flow<HabitStreakInfo>
    fun getAllStreakInfos(): Flow<List<HabitStreakInfo>>
    suspend fun createHabit(habit: Habit): Result<Habit>
    suspend fun updateHabit(habit: Habit): Result<Habit>
    suspend fun archiveHabit(habitId: String): Result<Unit>
    suspend fun completeHabit(habitId: String, date: LocalDate = today()): Result<HabitCompletion>
    suspend fun uncompleteHabit(habitId: String, date: LocalDate = today()): Result<Unit>
}

interface RoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    fun getRoutine(routineId: String): Flow<Routine?>
    suspend fun createRoutine(routine: Routine): Result<Routine>
    suspend fun updateRoutine(routine: Routine): Result<Routine>
    suspend fun deleteRoutine(routineId: String): Result<Unit>
}

interface FocusRepository {
    fun getDailyFocus(date: LocalDate = today()): Flow<DailyFocus?>
    suspend fun setDailyFocus(date: LocalDate, taskIds: List<String>): Result<DailyFocus>
    fun getWeeklyReview(weekStart: LocalDate): Flow<WeeklyReview>
}

interface CalendarRepository {
    fun isConfigured(): Flow<Boolean>
    suspend fun configureCaldav(serverUrl: String, email: String, password: String): Result<Unit>
    suspend fun configureGoogle(oauthToken: String): Result<Unit>
    suspend fun disconnect()
    suspend fun listCalendars(): Result<List<CalendarInfo>>
    suspend fun setDefaultCalendar(calendarId: String)
    suspend fun syncTaskToCalendar(task: Task): Result<String>
    suspend fun updateCalendarEvent(task: Task): Result<Unit>
    suspend fun removeCalendarEvent(eventId: String): Result<Unit>
}

interface SyncRepository {
    fun syncState(): Flow<SyncState>
    suspend fun configureServer(url: String, token: String): Result<ServerInfo>
    suspend fun disconnect()
    suspend fun sync(): Result<SyncResult>
    fun isRemoteConfigured(): Flow<Boolean>
}

interface ImportRepository {
    suspend fun importFromTodoist(data: ByteArray, format: ImportFormat): Result<ImportResult>
}
```

## Data-Vikunja: Sync Mappers

### Task Mapper (with metadata encoding)

```kotlin
class VikunjaTaskMapper {

    fun domainToDto(task: Task): VikunjaTaskDto {
        return VikunjaTaskDto(
            id = task.serverId,
            title = task.title,
            description = appendEstimate(task.description, task.estimatedMinutes),
            done = task.done,
            dueDate = task.dueDate?.toVikunjaFormat(),
            priority = task.priority.value.toLong(),
            // energyLevel → handled separately via label sync
            repeatAfter = task.repeatAfter?.inWholeSeconds,
            hexColor = null,
            position = task.position
        )
    }

    fun dtoToDomain(dto: VikunjaTaskDto, localTask: TaskEntity?): Task {
        val estimate = extractEstimate(dto.description ?: "")
        val energy = extractEnergyFromLabels(dto.labels)
        return Task(
            id = localTask?.id ?: UUID.randomUUID().toString(),
            serverId = dto.id,
            title = dto.title,
            description = stripTsosuMetadata(dto.description ?: ""),
            done = dto.done,
            dueDate = dto.dueDate?.fromVikunjaFormat(),
            priority = Priority.fromValue(dto.priority?.toInt() ?: 0),
            estimatedMinutes = estimate,
            energyLevel = energy ?: localTask?.energyLevel?.toDomain() ?: EnergyLevel.MEDIUM,
            isFocus = localTask?.isFocus ?: false,  // always preserve local focus state
            calendarEventId = localTask?.calendarEventId,
            // ...
        )
    }
}
```

### Habit Mapper (Habit ↔ Vikunja Repeating Task)

```kotlin
class VikunjaHabitMapper {

    fun habitToTaskDto(habit: Habit): VikunjaTaskDto {
        return VikunjaTaskDto(
            id = habit.serverId,
            title = habit.title,
            description = buildHabitDescription(habit.tinyVersion),
            done = false,
            priority = 0,
            repeatAfter = habit.frequency.repeatAfterSeconds,
            repeatMode = 0,  // default: repeat from due date
            hexColor = habit.color,
            projectId = habit.routineId?.let { /* resolve to Vikunja project ID */ },
            position = habit.position,
            dueDate = today().toVikunjaFormat()  // starts today
        )
    }

    fun taskDtoToHabit(dto: VikunjaTaskDto, localHabit: HabitEntity?): Habit {
        return Habit(
            id = localHabit?.id ?: UUID.randomUUID().toString(),
            serverId = dto.id,
            title = dto.title,
            tinyVersion = extractTinyVersion(dto.description),
            frequency = frequencyFromRepeatAfter(dto.repeatAfter),
            energyLevel = extractEnergyFromLabels(dto.labels) ?: EnergyLevel.LOW,
            routineId = dto.projectId?.let { /* resolve to local routine ID */ },
            position = dto.position ?: 0.0,
            color = dto.hexColor ?: "#4CAF50"
        )
    }

    private fun buildHabitDescription(tinyVersion: String?): String {
        return buildString {
            if (tinyVersion != null) append("✨ Tiny version: $tinyVersion\n\n")
            append("— Tsosu Habit")
        }
    }

    // Identify a Vikunja task as a Habit:
    // has repeatAfter > 0 AND is in a routine project AND description contains "— Tsosu Habit"
    fun isHabitTask(dto: VikunjaTaskDto, routineProjectIds: Set<Long>): Boolean {
        return dto.repeatAfter != null
            && dto.repeatAfter > 0
            && dto.projectId in routineProjectIds
    }
}
```

### Routine Mapper (Routine ↔ Vikunja Project)

```kotlin
class VikunjaRoutineMapper {

    fun routineToProjectDto(routine: Routine): VikunjaProjectDto {
        return VikunjaProjectDto(
            id = routine.serverId,
            title = "${routine.timeOfDay.emoji} ${routine.title}",
            description = "<!-- tsosu-routine:${routine.timeOfDay.name} -->",
        )
    }

    fun projectDtoToRoutine(dto: VikunjaProjectDto): Routine? {
        val timeOfDay = extractRoutineTime(dto.description) ?: return null
        return Routine(
            serverId = dto.id,
            title = dto.title.removePrefix("${timeOfDay.emoji} "),
            timeOfDay = timeOfDay
        )
    }

    // Identify a Vikunja project as a Routine:
    fun isRoutineProject(dto: VikunjaProjectDto): Boolean {
        return dto.description?.contains("<!-- tsosu-routine:") == true
    }
}
```

## Data-Local (Room)

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val done: Boolean = false,
    val doneAt: Long? = null,
    val dueDate: Long? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val position: Double = 0.0,
    val repeatAfterSeconds: Long? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: Int = 1,        // 0=LOW, 1=MEDIUM, 2=HIGH
    val isFocus: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,      // ★ Vikunja task ID
    val title: String,
    val tinyVersion: String? = null,
    val frequency: Int = 0,
    val targetDaysPerWeek: Int = 7,
    val energyLevel: Int = 0,
    val routineId: String? = null,   // ★ local routine ID → Vikunja project
    val position: Double = 0.0,
    val color: String = "#4CAF50",
    val isArchived: Boolean = false,
    val createdAt: Long
)

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val date: Long,
    val completedAt: Long
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,      // ★ Vikunja project ID
    val title: String,
    val timeOfDay: Int = 0
)

@Entity(tableName = "daily_focus")
data class DailyFocusEntity(
    @PrimaryKey val date: Long,
    val taskId1: String?,
    val taskId2: String?,
    val taskId3: String?
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,   // "task", "habit", "routine"
    val entityId: String,
    val operation: String,
    val payload: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null
)
```

## Data-Calendar

Task → VEVENT: uses estimatedMinutes for DTEND.
Habits are NOT synced to calendar (they're micro-actions, not time-blocked events).

## UI Design

### Bottom Navigation

```
📥     🎯      🔁      📆      🎲
Inbox  Focus   Habits  Upcoming Pick
```

### Habit Streak Display — Flexible, Not Punishing

```kotlin
// ✅ Tsosu says:
"5 out of the last 7 days"          「過去 7 天完成 5 天」
"22 times this month!"               「這個月做了 22 次！」

// ❌ NEVER show:
"Streak broken!"
"0 day streak"
```

### Notification Copy (Gentle)

```kotlin
// Habit nudge
"🌅 Your morning routine is ready. Start with just one."
"🌅 早安！晨間習慣準備好了，先做一個就好。"

// After completing all focus
"🎉 Focus 3 complete! Everything else is bonus."
"🎉 Focus 3 全部完成！剩下的都是加分。"
```

## TDD Strategy

### Sync-Specific Tests

```kotlin
class VikunjaHabitMapperTest {
    @Test fun `maps habit to repeating task with correct repeatAfter`()
    @Test fun `identifies routine projects by description metadata`()
    @Test fun `identifies habit tasks by repeatAfter + routine project`()
    @Test fun `extracts tinyVersion from task description`()
    @Test fun `completing habit marks Vikunja task done`()
}

class VikunjaTaskMapperTest {
    @Test fun `appends estimatedMinutes to description as metadata`()
    @Test fun `extracts estimatedMinutes from description metadata`()
    @Test fun `maps energy label to EnergyLevel enum`()
    @Test fun `strips tsosu metadata from description for display`()
    @Test fun `preserves local isFocus when pulling from server`()
}

class HabitSyncManagerTest {
    @Test fun `creates Vikunja repeating task when habit created`()
    @Test fun `creates Vikunja project when routine created`()
    @Test fun `completing habit pushes done=true to Vikunja`()
    @Test fun `new occurrence from Vikunja creates next habit entry`()
}
```

## Coding Conventions

- domain/ has ZERO Android deps
- ViewModels only see domain interfaces
- Tests first
- Local-first
- **★ UI copy: zero shame, flexible streaks**
- **★ Sync mapper tests: metadata encoding/decoding with real Vikunja JSON**
- **★ Habit identity: repeatAfter > 0 + routine project + "— Tsosu Habit" marker**
- Strings: en + zh-TW

## Reference

- **Todoist**: UI/UX patterns
- **Vikunja Flutter app**: API reference
- **Vikunja Task Model** (pkg/models/tasks.go): field mapping source of truth
- **tasks.org**: CalDAV reference
- **Finch**: gentle nudge UX
- **Atomic Habits**: habit design
- **Streaks (iOS)**: flexible habit tracking
