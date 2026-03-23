# Phase 1: Core Domain + Obsidian Compatibility Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the domain model to full Obsidian Tasks compatibility (extended statuses, all date types, reminders), align priority emoji to standard, add Android notifications and interactive widgets.

**Architecture:** Extend `Task` domain model with `TaskStatus` enum replacing `done: Boolean`, add `scheduledDate`, `startDate`, `reminderTime`, `completedDate`, `cancelledDate`, and `recurrenceRule`. Migrate Room DB schema (v2→v3). Update serializers/parsers to use Obsidian Tasks standard emoji. Add `AlarmManager`-based reminder notifications and upgrade Glance widgets to show live data.

**Tech Stack:** Kotlin 2.1, Room 2.6.1, Hilt 2.53.1, Glance 1.1.1, AlarmManager, WorkManager 2.10, JUnit 5, MockK

---

## File Map

### Domain Model Changes
- Modify: `domain/src/main/kotlin/app/tsosu/domain/model/Task.kt` — add fields, replace `done` with `status`
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/TaskStatus.kt` — new enum
- Modify: `domain/src/main/kotlin/app/tsosu/domain/model/Priority.kt` — update emoji constants

### Data Layer Changes
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/entity/TaskEntity.kt` — add columns
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt` — add MIGRATION_2_3, bump version
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/mapper/EntityMapper.kt` — update Task↔Entity mapping
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt` — update queries for `status` column

### Markdown Serializer/Parser Changes
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt` — new emoji, new fields
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt` — parse new emoji, new fields
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt`
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt`

### Notification System
- Create: `app/src/main/java/app/tsosu/notification/ReminderScheduler.kt` — AlarmManager wrapper
- Create: `app/src/main/java/app/tsosu/notification/ReminderReceiver.kt` — BroadcastReceiver
- Create: `app/src/main/java/app/tsosu/notification/NotificationHelper.kt` — build notifications
- Create: `app/src/main/java/app/tsosu/notification/OverdueCheckWorker.kt` — daily overdue scan
- Create: `app/src/main/java/app/tsosu/notification/BootReceiver.kt` — reschedule after reboot
- Modify: `app/src/main/AndroidManifest.xml` — register receivers, permissions

### Widget Upgrade
- Modify: `app/src/main/java/app/tsosu/ui/widget/FocusWidget.kt` — live data, interactive checkboxes
- Create: `app/src/main/java/app/tsosu/ui/widget/WidgetDataProvider.kt` — Room→widget data bridge
- Create: `app/src/main/java/app/tsosu/ui/widget/ToggleTaskActionCallback.kt` — checkbox action

### DI Changes
- Modify: `app/src/main/java/app/tsosu/di/DatabaseModule.kt` — add migration
- Create: `app/src/main/java/app/tsosu/di/NotificationModule.kt` — provide ReminderScheduler

### UI Updates (propagate TaskStatus)
- Modify: `app/src/main/java/app/tsosu/ui/components/TaskListItem.kt` — show status icon
- Modify: `app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailSheet.kt` — status picker
- Modify: `app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailViewModel.kt` — handle status changes
- Modify: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt` — reminder time picker

---

## Task 1: Create TaskStatus Enum

**Files:**
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/TaskStatus.kt`
- Test: `domain/src/test/kotlin/app/tsosu/domain/model/TaskStatusTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskStatusTest {

    @Test
    fun `all statuses have unique checkbox markers`() {
        val markers = TaskStatus.entries.map { it.checkboxMarker }
        assertEquals(markers.size, markers.toSet().size, "All markers must be unique")
    }

    @Test
    fun `fromCheckboxChar parses all known markers`() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromCheckboxChar(' '))
        assertEquals(TaskStatus.IN_PROGRESS, TaskStatus.fromCheckboxChar('/'))
        assertEquals(TaskStatus.ON_HOLD, TaskStatus.fromCheckboxChar('!'))
        assertEquals(TaskStatus.PLANNED, TaskStatus.fromCheckboxChar('>'))
        assertEquals(TaskStatus.DONE, TaskStatus.fromCheckboxChar('x'))
        assertEquals(TaskStatus.DONE, TaskStatus.fromCheckboxChar('X'))
        assertEquals(TaskStatus.CANCELLED, TaskStatus.fromCheckboxChar('-'))
    }

    @Test
    fun `unknown char defaults to TODO`() {
        assertEquals(TaskStatus.TODO, TaskStatus.fromCheckboxChar('?'))
    }

    @Test
    fun `isDone returns true only for DONE`() {
        assertTrue(TaskStatus.DONE.isDone)
        for (status in TaskStatus.entries.filter { it != TaskStatus.DONE }) {
            assertTrue(!status.isDone, "$status should not be done")
        }
    }

    @Test
    fun `isTerminal returns true for DONE and CANCELLED`() {
        assertTrue(TaskStatus.DONE.isTerminal)
        assertTrue(TaskStatus.CANCELLED.isTerminal)
        for (status in TaskStatus.entries.filter { it != TaskStatus.DONE && it != TaskStatus.CANCELLED }) {
            assertTrue(!status.isTerminal, "$status should not be terminal")
        }
    }

    @Test
    fun `ordinal values are sequential for Room storage`() {
        assertEquals(0, TaskStatus.TODO.ordinal)
        assertEquals(1, TaskStatus.IN_PROGRESS.ordinal)
        assertEquals(2, TaskStatus.ON_HOLD.ordinal)
        assertEquals(3, TaskStatus.PLANNED.ordinal)
        assertEquals(4, TaskStatus.DONE.ordinal)
        assertEquals(5, TaskStatus.CANCELLED.ordinal)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test --tests "app.tsosu.domain.model.TaskStatusTest" --no-daemon`
Expected: FAIL — class not found

- [ ] **Step 3: Write implementation**

```kotlin
package app.tsosu.domain.model

enum class TaskStatus(val checkboxMarker: Char) {
    TODO(' '),
    IN_PROGRESS('/'),
    ON_HOLD('!'),
    PLANNED('>'),
    DONE('x'),
    CANCELLED('-');

    val isDone: Boolean get() = this == DONE
    val isTerminal: Boolean get() = this == DONE || this == CANCELLED

    companion object {
        fun fromCheckboxChar(char: Char): TaskStatus = when (char) {
            ' ' -> TODO
            '/' -> IN_PROGRESS
            '!' -> ON_HOLD
            '>' -> PLANNED
            'x', 'X' -> DONE
            '-' -> CANCELLED
            else -> TODO
        }

        fun fromOrdinal(ordinal: Int): TaskStatus =
            entries.getOrElse(ordinal) { TODO }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test --tests "app.tsosu.domain.model.TaskStatusTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add domain/src/main/kotlin/app/tsosu/domain/model/TaskStatus.kt domain/src/test/kotlin/app/tsosu/domain/model/TaskStatusTest.kt
git commit -m "feat(domain): add TaskStatus enum with extended Obsidian-compatible statuses"
```

---

## Task 2: Expand Task Domain Model

**Files:**
- Modify: `domain/src/main/kotlin/app/tsosu/domain/model/Task.kt`
- Test: `domain/src/test/kotlin/app/tsosu/domain/model/TaskTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskTest {

    @Test
    fun `default task has TODO status and null optional fields`() {
        val task = Task(title = "Test task")
        assertEquals(TaskStatus.TODO, task.status)
        assertNull(task.scheduledDate)
        assertNull(task.startDate)
        assertNull(task.reminderTime)
        assertNull(task.completedDate)
        assertNull(task.cancelledDate)
        assertNull(task.recurrenceRule)
    }

    @Test
    fun `done property delegates to status`() {
        val doneTask = Task(title = "Done", status = TaskStatus.DONE)
        assertTrue(doneTask.done)

        val todoTask = Task(title = "Todo", status = TaskStatus.TODO)
        assertFalse(todoTask.done)

        val inProgressTask = Task(title = "WIP", status = TaskStatus.IN_PROGRESS)
        assertFalse(inProgressTask.done)
    }

    @Test
    fun `task with all new fields`() {
        val task = Task(
            title = "Full task",
            status = TaskStatus.IN_PROGRESS,
            scheduledDate = LocalDateTime.parse("2026-03-24T09:00:00"),
            startDate = LocalDateTime.parse("2026-03-20T08:00:00"),
            reminderTime = LocalTime(14, 30),
            completedDate = null,
            cancelledDate = null,
            recurrenceRule = "every week",
        )
        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        assertEquals(14, task.reminderTime?.hour)
        assertEquals(30, task.reminderTime?.minute)
        assertEquals("every week", task.recurrenceRule)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test --tests "app.tsosu.domain.model.TaskTest" --no-daemon`
Expected: FAIL — compilation error (fields don't exist)

- [ ] **Step 3: Modify Task.kt**

Replace the current `Task.kt` with:

```kotlin
package app.tsosu.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Task(
    val id: String = generateId(),
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val dueDate: LocalDateTime? = null,
    val scheduledDate: LocalDateTime? = null,
    val startDate: LocalDateTime? = null,
    val reminderTime: LocalTime? = null,
    val completedDate: LocalDateTime? = null,
    val cancelledDate: LocalDateTime? = null,
    val priority: Priority = Priority.NONE,
    val labels: List<Label> = emptyList(),
    val projectId: String? = null,
    val position: Double = 0.0,
    val subtasks: List<Task> = emptyList(),
    val recurrenceRule: String? = null,
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val isFocus: Boolean = false,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
) {
    /** Backwards-compatible convenience property */
    val done: Boolean get() = status.isDone
}

@OptIn(ExperimentalUuidApi::class)
private fun generateId(): String = Uuid.random().toString()
```

Note: `done: Boolean` field is removed and replaced with computed property. `repeatAfter: Duration?` is removed in favor of `recurrenceRule: String?` (human-readable like "every week", "every 2 days").

- [ ] **Step 4: Fix all compilation errors in dependent modules**

The following files reference `task.done` as a constructor param or `task.copy(done = ...)`:

Search for all usages and update:
- `done = true` → `status = TaskStatus.DONE`
- `done = false` → `status = TaskStatus.TODO`
- `task.copy(done = ...)` → `task.copy(status = ...)`
- `repeatAfter` → `recurrenceRule` (convert Duration to string where needed)

Key files to update:
- `data-local/src/main/kotlin/app/tsosu/data/local/mapper/EntityMapper.kt`
- `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt` (queries referencing `done`)
- `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt`
- `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt`
- All ViewModels that call `task.copy(done = ...)`
- All test files that construct `Task(done = ...)`

- [ ] **Step 5: Run tests to verify everything passes**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:test --no-daemon`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(domain): expand Task model with TaskStatus, dates, reminder, recurrence"
```

---

## Task 3: Update Priority Emoji to Obsidian Tasks Standard

**Files:**
- Modify: `domain/src/main/kotlin/app/tsosu/domain/model/Priority.kt` — add emoji constants
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt`
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt`
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt`
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt`

- [ ] **Step 1: Write failing tests for new emoji**

Add to `MarkdownTaskSerializerTest.kt`:

```kotlin
@Test
fun `priority uses Obsidian Tasks standard emoji`() {
    for ((priority, expectedMarker) in listOf(
        Priority.LOW to "🔽",
        Priority.MEDIUM to "🔼",
        Priority.HIGH to "🔺",
        Priority.URGENT to "⏫",
    )) {
        val result = serializer.serialize(listOf(task(priority = priority)))
        val taskLine = result.lines().first { it.startsWith("- [") }
        assertTrue(
            taskLine.contains(expectedMarker),
            "Priority $priority should produce marker '$expectedMarker', got: $taskLine"
        )
    }
}
```

Add to `MarkdownTaskParserTest.kt`:

```kotlin
@Test
fun `Obsidian Tasks standard priority emoji parsed correctly`() {
    val markdown = """
        ---
        tsosu: v1
        updated: 2026-03-23T12:00:00
        ---

        ## Inbox
        - [ ] Highest ⏫ <!-- id:p-1 -->
        - [ ] High 🔺 <!-- id:p-2 -->
        - [ ] Medium 🔼 <!-- id:p-3 -->
        - [ ] Low 🔽 <!-- id:p-4 -->
        - [ ] Lowest ⏬ <!-- id:p-5 -->
    """.trimIndent()

    val result = parser.parse(markdown)

    assertEquals(5, result.tasks.size)
    assertEquals(Priority.URGENT, result.tasks[0].priority)
    assertEquals(Priority.HIGH, result.tasks[1].priority)
    assertEquals(Priority.MEDIUM, result.tasks[2].priority)
    assertEquals(Priority.LOW, result.tasks[3].priority)
    assertEquals(Priority.NONE, result.tasks[4].priority)  // LOWEST maps to NONE
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --no-daemon`
Expected: FAIL

- [ ] **Step 3: Update Priority enum with emoji**

```kotlin
package app.tsosu.domain.model

enum class Priority(val value: Int, val color: Long, val emoji: String) {
    NONE(0, 0xFF808080, ""),
    LOW(1, 0xFF4A90D9, "🔽"),
    MEDIUM(2, 0xFFF5A623, "🔼"),
    HIGH(3, 0xFFEB8909, "🔺"),
    URGENT(4, 0xFFD1453B, "⏫");

    companion object {
        fun fromValue(value: Int): Priority =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}
```

- [ ] **Step 4: Update MarkdownTaskSerializer priority section**

Replace the priority `when` block in `formatTask()`:

```kotlin
// Priority (NONE is omitted)
if (task.priority != Priority.NONE) {
    append(" ${task.priority.emoji}")
}
```

- [ ] **Step 5: Update MarkdownTaskParser priority regexes**

Replace the old priority regex fields:

```kotlin
private val priorityHighestRegex = Regex("""⏫""")
private val priorityHighRegex = Regex("""🔺""")
private val priorityMediumRegex = Regex("""🔼""")
private val priorityLowRegex = Regex("""🔽""")
private val priorityLowestRegex = Regex("""⏬""")
```

Update the priority extraction:

```kotlin
val priority = when {
    priorityHighestRegex.containsMatchIn(rawContent) -> Priority.URGENT
    priorityHighRegex.containsMatchIn(rawContent) -> Priority.HIGH
    priorityMediumRegex.containsMatchIn(rawContent) -> Priority.MEDIUM
    priorityLowRegex.containsMatchIn(rawContent) -> Priority.LOW
    priorityLowestRegex.containsMatchIn(rawContent) -> Priority.NONE
    else -> Priority.NONE
}
```

Update the title cleaning to strip the new emoji.

- [ ] **Step 6: Update old tests to use new emoji**

Remove the old `each priority level maps to correct marker` test and replace with the new one. Update the `task with all metadata` test to use `⏫` instead of `‼️urgent`.

- [ ] **Step 7: Run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --no-daemon`
Expected: ALL PASS

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "feat(markdown): align priority emoji to Obsidian Tasks standard (⏫🔺🔼🔽)"
```

---

## Task 4: Update Serializer/Parser for Extended Status + New Date Fields

**Files:**
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt`
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt`
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt`
- Modify: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt`

- [ ] **Step 1: Write failing tests for extended status serialization**

```kotlin
@Test
fun `extended task statuses serialize to correct checkbox markers`() {
    val statuses = mapOf(
        TaskStatus.TODO to "- [ ]",
        TaskStatus.IN_PROGRESS to "- [/]",
        TaskStatus.ON_HOLD to "- [!]",
        TaskStatus.PLANNED to "- [>]",
        TaskStatus.DONE to "- [x]",
        TaskStatus.CANCELLED to "- [-]",
    )
    for ((status, expected) in statuses) {
        val result = serializer.serialize(listOf(task(status = status)))
        val taskLine = result.lines().first { it.contains("Buy groceries") }
        assertTrue(
            taskLine.startsWith(expected),
            "Status $status should produce '$expected', got: $taskLine"
        )
    }
}

@Test
fun `scheduled date serialized with hourglass emoji`() {
    val result = serializer.serialize(listOf(
        task(scheduledDate = LocalDateTime.parse("2026-03-24T09:00:00"))
    ))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("⏳ 2026-03-24"), "Should contain scheduled date")
}

@Test
fun `start date serialized with airplane emoji`() {
    val result = serializer.serialize(listOf(
        task(startDate = LocalDateTime.parse("2026-03-20T08:00:00"))
    ))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("🛫 2026-03-20"), "Should contain start date")
}

@Test
fun `created date serialized with plus emoji`() {
    val createdAt = Instant.parse("2026-03-18T10:00:00Z")
    val result = serializer.serialize(listOf(task(createdAt = createdAt)))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("➕ 2026-03-18"), "Should contain created date")
}

@Test
fun `reminder time serialized with alarm emoji`() {
    val result = serializer.serialize(listOf(
        task(reminderTime = LocalTime(14, 30))
    ))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("⏰ 14:30"), "Should contain reminder time")
}

@Test
fun `recurrence rule serialized with repeat emoji`() {
    val result = serializer.serialize(listOf(
        task(recurrenceRule = "every week")
    ))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("🔁 every week"), "Should contain recurrence")
}

@Test
fun `cancelled task has cancelled date with X emoji`() {
    val result = serializer.serialize(listOf(
        task(
            status = TaskStatus.CANCELLED,
            cancelledDate = LocalDateTime.parse("2026-03-22T14:30:00"),
        )
    ))
    val taskLine = result.lines().first { it.startsWith("- [") }
    assertTrue(taskLine.contains("- [-]"), "Cancelled checkbox")
    assertTrue(taskLine.contains("❌ 2026-03-22"), "Cancelled date")
}
```

- [ ] **Step 2: Write failing tests for extended status parsing**

```kotlin
@Test
fun `extended statuses parsed from checkbox markers`() {
    val markdown = """
        ---
        tsosu: v1
        updated: 2026-03-23T12:00:00
        ---

        ## Inbox
        - [ ] Todo task <!-- id:s-1 -->
        - [/] In progress task <!-- id:s-2 -->
        - [!] On hold task <!-- id:s-3 -->
        - [>] Planned task <!-- id:s-4 -->
        - [x] Done task ✅ 2026-03-22 <!-- id:s-5 -->
        - [-] Cancelled task ❌ 2026-03-22 <!-- id:s-6 -->
    """.trimIndent()

    val result = parser.parse(markdown)

    assertEquals(6, result.tasks.size)
    assertEquals(TaskStatus.TODO, result.tasks[0].status)
    assertEquals(TaskStatus.IN_PROGRESS, result.tasks[1].status)
    assertEquals(TaskStatus.ON_HOLD, result.tasks[2].status)
    assertEquals(TaskStatus.PLANNED, result.tasks[3].status)
    assertEquals(TaskStatus.DONE, result.tasks[4].status)
    assertEquals(TaskStatus.CANCELLED, result.tasks[5].status)
}

@Test
fun `scheduled and start dates parsed`() {
    val markdown = """
        ---
        tsosu: v1
        ---

        ## Inbox
        - [ ] Task ⏳ 2026-03-24 🛫 2026-03-20 📅 2026-03-28 <!-- id:d-1 -->
    """.trimIndent()

    val result = parser.parse(markdown)
    val task = result.tasks[0]
    assertEquals(24, task.scheduledDate?.dayOfMonth)
    assertEquals(20, task.startDate?.dayOfMonth)
    assertEquals(28, task.dueDate?.dayOfMonth)
}

@Test
fun `reminder time parsed`() {
    val markdown = """
        ---
        tsosu: v1
        ---

        ## Inbox
        - [ ] Task ⏰ 14:30 <!-- id:r-1 -->
    """.trimIndent()

    val result = parser.parse(markdown)
    assertEquals(14, result.tasks[0].reminderTime?.hour)
    assertEquals(30, result.tasks[0].reminderTime?.minute)
}

@Test
fun `recurrence rule parsed`() {
    val markdown = """
        ---
        tsosu: v1
        ---

        ## Inbox
        - [ ] Task 🔁 every week <!-- id:rec-1 -->
    """.trimIndent()

    val result = parser.parse(markdown)
    assertEquals("every week", result.tasks[0].recurrenceRule)
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --no-daemon`
Expected: FAIL

- [ ] **Step 4: Update MarkdownTaskSerializer**

Update `formatTask()` to:
1. Use `task.status.checkboxMarker` for checkbox: `"- [${task.status.checkboxMarker}] "`
2. Add completed date: `if (task.status == TaskStatus.DONE) append(" ✅ ${task.completedDate?.date ?: ...}")`
3. Add cancelled date: `if (task.status == TaskStatus.CANCELLED && task.cancelledDate != null) append(" ❌ ${task.cancelledDate.date}")`
4. Add scheduled date: `if (task.scheduledDate != null) append(" ⏳ ${task.scheduledDate.date}")`
5. Add start date: `if (task.startDate != null) append(" 🛫 ${task.startDate.date}")`
6. Add created date: `append(" ➕ ${task.createdAt...date}")`
7. Add reminder: `if (task.reminderTime != null) append(" ⏰ ${format(task.reminderTime)}")`
8. Add recurrence: `if (task.recurrenceRule != null) append(" 🔁 ${task.recurrenceRule}")`

- [ ] **Step 5: Update MarkdownTaskParser**

1. Update taskLineRegex to match extended markers: `Regex("""^- \[([ xX/!>\-])] (.+)$""")`
2. Use `TaskStatus.fromCheckboxChar(match.groupValues[1][0])` instead of `isDone` boolean
3. Add regex patterns for new date fields and extract them
4. Clean title by stripping all new emoji markers

- [ ] **Step 6: Run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --no-daemon`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(markdown): serialize/parse extended statuses, scheduled/start/created dates, reminders, recurrence"
```

---

## Task 5: Update TaskEntity + Room Migration (v2→v3)

**Files:**
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/entity/TaskEntity.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/mapper/EntityMapper.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt`

- [ ] **Step 1: Update TaskEntity**

```kotlin
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val serverId: Long? = null,
    val title: String,
    val description: String = "",
    val status: Int = 0,  // was: done: Boolean
    val doneAt: Long? = null,
    val dueDate: Long? = null,
    val scheduledDate: Long? = null,
    val startDate: Long? = null,
    val reminderTimeMinutes: Int? = null,  // minutes since midnight
    val completedDate: Long? = null,
    val cancelledDate: Long? = null,
    val priority: Int = 0,
    val projectId: String? = null,
    val position: Double = 0.0,
    val recurrenceRule: String? = null,  // was: repeatAfterSeconds
    val calendarEventId: String? = null,
    val estimatedMinutes: Int? = null,
    val energyLevel: Int = 1,
    val isFocus: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val syncStatus: Int = 0,
)
```

- [ ] **Step 2: Add MIGRATION_2_3**

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add status column (0 = TODO, 4 = DONE)
        db.execSQL("ALTER TABLE tasks ADD COLUMN status INTEGER NOT NULL DEFAULT 0")
        // Migrate done=1 to status=4 (DONE)
        db.execSQL("UPDATE tasks SET status = 4 WHERE done = 1")

        // Add new date columns
        db.execSQL("ALTER TABLE tasks ADD COLUMN scheduledDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN startDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN reminderTimeMinutes INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN completedDate INTEGER")
        db.execSQL("ALTER TABLE tasks ADD COLUMN cancelledDate INTEGER")

        // Rename repeatAfterSeconds -> recurrenceRule
        db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT")

        // Migrate completedDate from doneAt
        db.execSQL("UPDATE tasks SET completedDate = doneAt WHERE done = 1")
    }
}
```

- [ ] **Step 3: Update TsosuDatabase**

Bump version to 3, add migration, remove old `done` column reference (it stays in SQLite but is ignored).

- [ ] **Step 4: Update EntityMapper**

```kotlin
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
    reminderTime = reminderTimeMinutes?.let {
        LocalTime(it / 60, it % 60)
    },
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
    dueDate = dueDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    scheduledDate = scheduledDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    startDate = startDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    reminderTimeMinutes = reminderTime?.let { it.hour * 60 + it.minute },
    completedDate = completedDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    cancelledDate = cancelledDate?.toInstant(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
    priority = priority.value,
    projectId = projectId,
    position = position,
    recurrenceRule = recurrenceRule,
    calendarEventId = calendarEventId,
    estimatedMinutes = estimatedMinutes,
    energyLevel = energyLevel.ordinal,
    isFocus = isFocus,
    createdAt = createdAt.toEpochMilliseconds(),
    updatedAt = updatedAt.toEpochMilliseconds(),
)
```

- [ ] **Step 5: Update TaskDao queries**

Replace `done = 0` with `status < 4` (not DONE or CANCELLED) in queries:
- `getInboxTasks`: `WHERE projectId IS NULL AND dueDate IS NULL AND status < 4`
- `getTodayTasks`: `WHERE ... AND status < 4`
- `getUpcomingTasks`: `WHERE ... AND status < 4`
- `getByEnergyLevel`: `WHERE energyLevel = :level AND status < 4`
- `getStaleTaskIds`: `WHERE status < 4 AND updatedAt < :threshold`

Replace `setDone()`:
```kotlin
@Query("UPDATE tasks SET status = :status, completedDate = :completedDate, updatedAt = :updatedAt WHERE id = :taskId")
suspend fun setStatus(taskId: String, status: Int, completedDate: Long?, updatedAt: Long)
```

- [ ] **Step 6: Build and run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug test --no-daemon`
Expected: BUILD SUCCESS, ALL TESTS PASS

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "feat(data-local): add Room migration v2→v3 for TaskStatus and new date fields"
```

---

## Task 6: Update UI to Use TaskStatus

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/components/TaskListItem.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailSheet.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/taskdetail/TaskDetailViewModel.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/inbox/InboxViewModel.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/upcoming/UpcomingViewModel.kt`

- [ ] **Step 1: Update TaskListItem to show status icons**

Replace the simple checkbox with a status-aware icon:
- `TODO` → empty checkbox
- `IN_PROGRESS` → half-filled / slash icon
- `ON_HOLD` → exclamation icon
- `PLANNED` → arrow-right icon
- `DONE` → checked checkbox with strikethrough title
- `CANCELLED` → X icon with strikethrough title

Update `onToggleDone` callback to `onStatusChange: (TaskStatus) -> Unit`.

- [ ] **Step 2: Update TaskDetailSheet with status picker**

Add a segmented button row or dropdown for selecting TaskStatus. Add date pickers for scheduledDate and startDate. Add time picker for reminderTime.

- [ ] **Step 3: Update ViewModels**

Replace all `toggleDone()` calls with `setStatus(taskId, newStatus)`:
- `InboxViewModel`
- `UpcomingViewModel`
- `TaskDetailViewModel`

- [ ] **Step 4: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(ui): update task UI for extended statuses, date pickers, reminder time"
```

---

## Task 7: Notification System

**Files:**
- Create: `app/src/main/java/app/tsosu/notification/NotificationHelper.kt`
- Create: `app/src/main/java/app/tsosu/notification/ReminderScheduler.kt`
- Create: `app/src/main/java/app/tsosu/notification/ReminderReceiver.kt`
- Create: `app/src/main/java/app/tsosu/notification/OverdueCheckWorker.kt`
- Create: `app/src/main/java/app/tsosu/notification/BootReceiver.kt`
- Create: `app/src/main/java/app/tsosu/di/NotificationModule.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create NotificationHelper**

```kotlin
package app.tsosu.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import app.tsosu.MainActivity
import app.tsosu.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_REMINDERS = "task_reminders"
        const val CHANNEL_OVERDUE = "task_overdue"
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_OVERDUE, "Overdue Tasks", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun showReminder(taskId: String, title: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("taskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("taskId", taskId)
        }
        val completePending = PendingIntent.getBroadcast(
            context, notificationId + 10000, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText("Task reminder")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(0, "Complete", completePending)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }

    fun showOverdueSummary(count: Int, titles: List<String>) {
        val text = if (titles.size <= 3) titles.joinToString(", ")
                   else "${titles.take(3).joinToString(", ")} +${titles.size - 3} more"

        val notification = NotificationCompat.Builder(context, CHANNEL_OVERDUE)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$count overdue tasks")
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(99999, notification)
    }
}
```

- [ ] **Step 2: Create ReminderScheduler**

```kotlin
package app.tsosu.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(taskId: String, triggerAtMillis: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_REMINDER"
            putExtra("taskId", taskId)
        }
        val pending = PendingIntent.getBroadcast(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pending
        )
    }

    fun cancel(taskId: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, taskId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }
}
```

- [ ] **Step 3: Create ReminderReceiver**

```kotlin
package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("taskId") ?: return

        when (intent.action) {
            "ACTION_REMINDER" -> {
                val title = intent.getStringExtra("taskTitle") ?: "Task reminder"
                notificationHelper.showReminder(taskId, title, taskId.hashCode())
            }
            "ACTION_COMPLETE" -> {
                // TODO: mark task as DONE via repository (needs coroutine scope)
            }
        }
    }
}
```

- [ ] **Step 4: Create OverdueCheckWorker**

```kotlin
package app.tsosu.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.tsosu.data.local.dao.TaskDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class OverdueCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val overdueTasks = taskDao.getOverdueTasks(now).first()
        if (overdueTasks.isNotEmpty()) {
            notificationHelper.showOverdueSummary(
                overdueTasks.size,
                overdueTasks.map { it.title }
            )
        }
        return Result.success()
    }
}
```

- [ ] **Step 5: Create BootReceiver**

```kotlin
package app.tsosu.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var reminderScheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule all active reminders from Room DB
            // TODO: query tasks with reminderTime and reschedule
        }
    }
}
```

- [ ] **Step 6: Add getOverdueTasks query to TaskDao**

```kotlin
@Query("SELECT * FROM tasks WHERE dueDate < :now AND status < 4 ORDER BY dueDate")
fun getOverdueTasks(now: Long): Flow<List<TaskEntity>>
```

- [ ] **Step 7: Update AndroidManifest.xml**

Add permissions and receivers:
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<receiver android:name=".notification.ReminderReceiver"
    android:exported="false" />
<receiver android:name=".notification.BootReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

- [ ] **Step 8: Create NotificationModule**

```kotlin
package app.tsosu.di

import app.tsosu.notification.NotificationHelper
import app.tsosu.notification.ReminderScheduler
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule
// NotificationHelper and ReminderScheduler use @Inject constructor, auto-provided
```

- [ ] **Step 9: Register OverdueCheckWorker in MainActivity or TsosuApp**

```kotlin
val overdueWork = PeriodicWorkRequestBuilder<OverdueCheckWorker>(1, TimeUnit.DAYS)
    .setInitialDelay(calculateDelayToMorning(), TimeUnit.MILLISECONDS)
    .build()
WorkManager.getInstance(this).enqueueUniquePeriodicWork(
    "overdue_check", ExistingPeriodicWorkPolicy.KEEP, overdueWork
)
```

- [ ] **Step 10: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESS

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat(notification): add AlarmManager reminders, overdue check worker, boot reschedule"
```

---

## Task 8: Interactive Glance Widget

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/widget/FocusWidget.kt`
- Create: `app/src/main/java/app/tsosu/ui/widget/WidgetDataProvider.kt`
- Create: `app/src/main/java/app/tsosu/ui/widget/ToggleTaskAction.kt`

- [ ] **Step 1: Create WidgetDataProvider**

```kotlin
package app.tsosu.ui.widget

import android.content.Context
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.domain.model.Task
import kotlinx.coroutines.flow.first

class WidgetDataProvider(private val context: Context) {

    suspend fun getFocusTasks(): List<Task> {
        // Get database instance via Hilt EntryPoint
        val dao = WidgetEntryPoint.get(context).taskDao()
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000)
        val endOfDay = startOfDay + 86_400_000
        return dao.getFocusTasks(startOfDay, endOfDay).first().map { it.toDomain() }
    }

    suspend fun getTodayTasks(): List<Task> {
        val dao = WidgetEntryPoint.get(context).taskDao()
        val now = System.currentTimeMillis()
        val startOfDay = now - (now % 86_400_000)
        val endOfDay = startOfDay + 86_400_000
        return dao.getTodayTasks(startOfDay, endOfDay).first().map { it.toDomain() }
    }
}
```

- [ ] **Step 2: Create ToggleTaskAction**

```kotlin
package app.tsosu.ui.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import app.tsosu.domain.model.TaskStatus

class ToggleTaskAction : ActionCallback {
    companion object {
        val TaskIdKey = ActionParameters.Key<String>("taskId")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val taskId = parameters[TaskIdKey] ?: return
        val dao = WidgetEntryPoint.get(context).taskDao()
        val now = System.currentTimeMillis()
        dao.setStatus(taskId, TaskStatus.DONE.ordinal, now, now)
        FocusWidget().update(context, glanceId)
    }
}
```

- [ ] **Step 3: Upgrade FocusWidget to show live tasks**

Replace the static placeholder with dynamic content:

```kotlin
class FocusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val provider = WidgetDataProvider(context)
        val tasks = provider.getFocusTasks().take(3)

        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(12.dp)
                    .background(GlanceTheme.colors.surface),
            ) {
                Text("Focus 3", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp))
                Spacer(GlanceModifier.size(8.dp))
                if (tasks.isEmpty()) {
                    Text("No focus tasks set", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant))
                } else {
                    tasks.forEach { task ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CheckBox(
                                checked = task.done,
                                onCheckedChange = actionRunCallback<ToggleTaskAction>(
                                    parameters = actionParametersOf(ToggleTaskAction.TaskIdKey to task.id)
                                ),
                            )
                            Spacer(GlanceModifier.size(8.dp))
                            Text(task.title, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create Hilt EntryPoint for widget**

```kotlin
package app.tsosu.ui.widget

import android.content.Context
import app.tsosu.data.local.dao.TaskDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskDao(): TaskDao

    companion object {
        fun get(context: Context): WidgetEntryPoint =
            EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
    }
}
```

- [ ] **Step 5: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug --no-daemon`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(widget): upgrade Focus widget with live data and interactive checkboxes"
```

---

## Task 9: Final Build Verification + Full Test Suite

- [ ] **Step 1: Run full build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew clean assembleDebug --no-daemon`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all unit tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test --no-daemon`
Expected: ALL PASS

- [ ] **Step 3: Commit any fixes**

```bash
git add -A
git commit -m "fix: resolve Phase 1 integration issues"
```

- [ ] **Step 4: Tag Phase 1 complete**

```bash
git tag phase1-complete
```
