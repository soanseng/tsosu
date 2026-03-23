# Phase 2: Multi-file Architecture (TaskNotes + HabitNotes + Daily Notes) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate from single-file (tasks.md/habits.md) to a multi-file vault architecture. Tasks and habits with rich content get individual note files (YAML frontmatter). Index files (tasks.md, habits.md) are auto-generated with wikilinks. Daily notes are write-only output for Obsidian Tracker/Dataview. HabitNotes store completions for bidirectional sync.

**Architecture:** Three-layer vault structure:
1. `tasks/` folder — individual TaskNote `.md` files with YAML frontmatter (source of truth for rich tasks)
2. `habits/` folder — individual HabitNote `.md` files with YAML frontmatter + completion history
3. `daily/` folder — write-only daily notes with `#habit` checkboxes for Obsidian ecosystem
4. `tasks.md` / `habits.md` — auto-generated index files with inline tasks and wikilinks

Import priority: individual files > index file. Export: write individual files + regenerate index + write today's daily note.

**Tech Stack:** Kotlin 2.1, kotlinx-serialization (YAML parsing via regex — no external YAML lib), SAF DocumentFile API, JUnit 5, MockK

**Depends on:** Phase 1 complete (TaskStatus, new date fields in Task model)

---

## File Map

### New Files in data-markdown module
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializer.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteParser.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteSerializer.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteParser.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/dailynote/DailyNoteWriter.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/index/TaskIndexGenerator.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/index/HabitIndexGenerator.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/YamlFrontmatterParser.kt` — shared YAML helper

### Modified Files
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownFileAccess.kt` — add folder operations
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/SafMarkdownFileAccess.kt` — implement folder ops
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncManager.kt` — orchestrate multi-file
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncRepository.kt` — new sync flow

### Test Files
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializerTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteParserTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteSerializerTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteParserTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/dailynote/DailyNoteWriterTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/index/TaskIndexGeneratorTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/index/HabitIndexGeneratorTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/YamlFrontmatterParserTest.kt`

---

## Task 1: YAML Frontmatter Parser Utility

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/YamlFrontmatterParser.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/YamlFrontmatterParserTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.data.markdown

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class YamlFrontmatterParserTest {

    private val parser = YamlFrontmatterParser()

    @Test
    fun `parse simple key-value pairs`() {
        val yaml = """
            ---
            id: abc-123
            status: todo
            priority: high
            due: 2026-03-25
            ---

            # Title
            Body content here.
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("abc-123", result.frontmatter["id"])
        assertEquals("todo", result.frontmatter["status"])
        assertEquals("high", result.frontmatter["priority"])
        assertEquals("2026-03-25", result.frontmatter["due"])
        assertEquals("# Title\nBody content here.", result.body.trim())
    }

    @Test
    fun `parse list values`() {
        val yaml = """
            ---
            tags: [errands, shopping]
            ---
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("[errands, shopping]", result.frontmatter["tags"])
    }

    @Test
    fun `no frontmatter returns empty map and full body`() {
        val content = "# Just a title\nSome body."
        val result = parser.parse(content)
        assertEquals(emptyMap<String, String>(), result.frontmatter)
        assertEquals(content, result.body.trim())
    }

    @Test
    fun `empty frontmatter`() {
        val content = "---\n---\nBody"
        val result = parser.parse(content)
        assertEquals(emptyMap<String, String>(), result.frontmatter)
        assertEquals("Body", result.body.trim())
    }

    @Test
    fun `quoted string values preserve quotes`() {
        val yaml = """
            ---
            reminder: "14:30"
            tiny: "Take 3 deep breaths"
            ---
        """.trimIndent()

        val result = parser.parse(yaml)
        assertEquals("14:30", result.frontmatter["reminder"])
        assertEquals("Take 3 deep breaths", result.frontmatter["tiny"])
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --tests "*.YamlFrontmatterParserTest" --no-daemon`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package app.tsosu.data.markdown

data class ParsedDocument(
    val frontmatter: Map<String, String>,
    val body: String,
)

class YamlFrontmatterParser {

    fun parse(content: String): ParsedDocument {
        val lines = content.lines()
        if (lines.isEmpty() || lines[0].trim() != "---") {
            return ParsedDocument(emptyMap(), content)
        }

        val closingIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
        if (closingIdx == -1) {
            return ParsedDocument(emptyMap(), content)
        }

        val yamlLines = lines.subList(1, closingIdx + 1)
        val body = lines.drop(closingIdx + 2).joinToString("\n")
        val frontmatter = mutableMapOf<String, String>()

        for (line in yamlLines) {
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val rawValue = line.substring(colonIdx + 1).trim()
                val value = rawValue.removeSurrounding("\"")
                frontmatter[key] = value
            }
        }

        return ParsedDocument(frontmatter, body)
    }

    fun serialize(frontmatter: Map<String, String>, body: String): String = buildString {
        appendLine("---")
        for ((key, value) in frontmatter) {
            if (value.startsWith("[") || value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) || value == "true" || value == "false") {
                appendLine("$key: $value")
            } else if (value.contains(":") || value.contains(" ")) {
                appendLine("$key: \"$value\"")
            } else {
                appendLine("$key: $value")
            }
        }
        appendLine("---")
        appendLine()
        append(body)
    }
}
```

- [ ] **Step 4: Run tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --tests "*.YamlFrontmatterParserTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/main/kotlin/app/tsosu/data/markdown/YamlFrontmatterParser.kt data-markdown/src/test/kotlin/app/tsosu/data/markdown/YamlFrontmatterParserTest.kt
git commit -m "feat(markdown): add YAML frontmatter parser utility"
```

---

## Task 2: TaskNote Serializer

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializer.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializerTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.data.markdown.tasknote

import app.tsosu.domain.model.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TaskNoteSerializerTest {

    private val serializer = TaskNoteSerializer()

    private val baseTask = Task(
        id = "abc-123",
        title = "Buy Groceries",
        description = "Weekly shopping list",
        status = TaskStatus.TODO,
        priority = Priority.HIGH,
        dueDate = LocalDateTime.parse("2026-03-25T09:00:00"),
        scheduledDate = LocalDateTime.parse("2026-03-24T09:00:00"),
        startDate = LocalDateTime.parse("2026-03-20T08:00:00"),
        reminderTime = LocalTime(14, 30),
        energyLevel = EnergyLevel.MEDIUM,
        estimatedMinutes = 30,
        recurrenceRule = "every week",
        createdAt = Instant.parse("2026-03-18T10:00:00Z"),
        updatedAt = Instant.parse("2026-03-22T14:30:00Z"),
    )

    @Test
    fun `serializes frontmatter with all fields`() {
        val result = serializer.serialize(baseTask, projectName = "Personal")

        assertTrue(result.contains("id: abc-123"))
        assertTrue(result.contains("status: todo"))
        assertTrue(result.contains("priority: high"))
        assertTrue(result.contains("due: 2026-03-25"))
        assertTrue(result.contains("scheduled: 2026-03-24"))
        assertTrue(result.contains("start: 2026-03-20"))
        assertTrue(result.contains("reminder: \"14:30\""))
        assertTrue(result.contains("energy: medium"))
        assertTrue(result.contains("estimate: 30m"))
        assertTrue(result.contains("recurrence: \"every week\""))
        assertTrue(result.contains("project: Personal"))
        assertTrue(result.contains("created: 2026-03-18"))
    }

    @Test
    fun `serializes title as h1 heading`() {
        val result = serializer.serialize(baseTask)
        assertTrue(result.contains("# Buy Groceries"))
    }

    @Test
    fun `serializes description as body`() {
        val result = serializer.serialize(baseTask)
        assertTrue(result.contains("Weekly shopping list"))
    }

    @Test
    fun `omits null fields`() {
        val minimal = Task(
            id = "min-1",
            title = "Simple task",
            createdAt = Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = Instant.parse("2026-03-20T10:00:00Z"),
        )
        val result = serializer.serialize(minimal)

        assertTrue(result.contains("id: min-1"))
        assertTrue(result.contains("status: todo"))
        assertTrue(!result.contains("due:"))
        assertTrue(!result.contains("scheduled:"))
        assertTrue(!result.contains("reminder:"))
        assertTrue(!result.contains("recurrence:"))
    }

    @Test
    fun `done task includes completed date`() {
        val done = baseTask.copy(
            status = TaskStatus.DONE,
            completedDate = LocalDateTime.parse("2026-03-22T14:30:00"),
        )
        val result = serializer.serialize(done)
        assertTrue(result.contains("status: done"))
        assertTrue(result.contains("completed: 2026-03-22"))
    }

    @Test
    fun `generates slug filename from title`() {
        val slug = serializer.slugify("Buy Groceries for the Week!")
        assertTrue(slug == "buy-groceries-for-the-week")
    }

    @Test
    fun `slug handles unicode and special chars`() {
        val slug = serializer.slugify("冥想 & Exercise (30m)")
        assertTrue(slug == "冥想-exercise-30m")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --tests "*.TaskNoteSerializerTest" --no-daemon`
Expected: FAIL

- [ ] **Step 3: Write implementation**

```kotlin
package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import app.tsosu.domain.model.TaskStatus
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TaskNoteSerializer {

    private val yamlHelper = YamlFrontmatterParser()

    fun serialize(task: Task, projectName: String? = null): String {
        val frontmatter = buildMap {
            put("id", task.id)
            put("status", task.status.name.lowercase())
            if (task.priority != Priority.NONE) {
                put("priority", task.priority.name.lowercase())
            }
            task.dueDate?.let { put("due", it.date.toString()) }
            task.scheduledDate?.let { put("scheduled", it.date.toString()) }
            task.startDate?.let { put("start", it.date.toString()) }
            task.reminderTime?.let { put("reminder", "${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}") }
            put("energy", task.energyLevel.name.lowercase())
            task.estimatedMinutes?.let { put("estimate", "${it}m") }
            task.recurrenceRule?.let { put("recurrence", it) }
            projectName?.let { put("project", it) }
            task.completedDate?.let { put("completed", it.date.toString()) }
            task.cancelledDate?.let { put("cancelled", it.date.toString()) }
            put("created", task.createdAt.toLocalDateTime(TimeZone.UTC).date.toString())
        }

        val body = buildString {
            appendLine("# ${task.title}")
            if (task.description.isNotBlank()) {
                appendLine()
                append(task.description)
            }
        }

        return yamlHelper.serialize(frontmatter, body)
    }

    fun slugify(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^\\w\\s\\u4e00-\\u9fff\\u3040-\\u309f\\u30a0-\\u30ff-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
    }
}
```

- [ ] **Step 4: Run tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test --tests "*.TaskNoteSerializerTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/main/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializer.kt data-markdown/src/test/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteSerializerTest.kt
git commit -m "feat(markdown): add TaskNote YAML frontmatter serializer"
```

---

## Task 3: TaskNote Parser

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteParser.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/tasknote/TaskNoteParserTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.data.markdown.tasknote

import app.tsosu.domain.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TaskNoteParserTest {

    private val parser = TaskNoteParser()

    @Test
    fun `parse full TaskNote`() {
        val content = """
            ---
            id: abc-123
            status: todo
            priority: high
            due: 2026-03-25
            scheduled: 2026-03-24
            start: 2026-03-20
            reminder: "14:30"
            energy: medium
            estimate: 30m
            recurrence: "every week"
            project: Personal
            created: 2026-03-18
            ---

            # Buy Groceries

            Weekly shopping list.
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals("abc-123", result.task.id)
        assertEquals("Buy Groceries", result.task.title)
        assertEquals("Weekly shopping list.", result.task.description.trim())
        assertEquals(TaskStatus.TODO, result.task.status)
        assertEquals(Priority.HIGH, result.task.priority)
        assertEquals(25, result.task.dueDate?.dayOfMonth)
        assertEquals(24, result.task.scheduledDate?.dayOfMonth)
        assertEquals(20, result.task.startDate?.dayOfMonth)
        assertEquals(14, result.task.reminderTime?.hour)
        assertEquals(30, result.task.reminderTime?.minute)
        assertEquals(EnergyLevel.MEDIUM, result.task.energyLevel)
        assertEquals(30, result.task.estimatedMinutes)
        assertEquals("every week", result.task.recurrenceRule)
        assertEquals("Personal", result.projectName)
    }

    @Test
    fun `parse minimal TaskNote`() {
        val content = """
            ---
            id: min-1
            status: todo
            created: 2026-03-20
            ---

            # Simple task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals("min-1", result.task.id)
        assertEquals("Simple task", result.task.title)
        assertEquals(TaskStatus.TODO, result.task.status)
        assertNull(result.task.dueDate)
        assertNull(result.task.scheduledDate)
        assertNull(result.task.reminderTime)
        assertNull(result.projectName)
    }

    @Test
    fun `parse done task`() {
        val content = """
            ---
            id: done-1
            status: done
            completed: 2026-03-22
            created: 2026-03-18
            ---

            # Finished task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals(TaskStatus.DONE, result.task.status)
        assertEquals(22, result.task.completedDate?.dayOfMonth)
    }

    @Test
    fun `parse cancelled task`() {
        val content = """
            ---
            id: cancel-1
            status: cancelled
            cancelled: 2026-03-22
            created: 2026-03-18
            ---

            # Cancelled task
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals(TaskStatus.CANCELLED, result.task.status)
        assertEquals(22, result.task.cancelledDate?.dayOfMonth)
    }

    @Test
    fun `title extracted from h1 heading, rest is description`() {
        val content = """
            ---
            id: body-1
            status: todo
            created: 2026-03-20
            ---

            # Main Title

            First paragraph.

            ## Subtasks
            - [ ] Sub 1
            - [ ] Sub 2
        """.trimIndent()

        val result = parser.parse(content)
        assertEquals("Main Title", result.task.title)
        assertTrue(result.task.description.contains("First paragraph."))
        assertTrue(result.task.description.contains("## Subtasks"))
    }

    @Test
    fun `round-trip serialize then parse preserves fields`() {
        val serializer = TaskNoteSerializer()
        val original = Task(
            id = "rt-1",
            title = "Round Trip",
            description = "Test body",
            status = TaskStatus.IN_PROGRESS,
            priority = Priority.MEDIUM,
            dueDate = kotlinx.datetime.LocalDateTime.parse("2026-04-01T00:00:00"),
            energyLevel = EnergyLevel.HIGH,
            estimatedMinutes = 60,
            createdAt = kotlinx.datetime.Instant.parse("2026-03-20T10:00:00Z"),
            updatedAt = kotlinx.datetime.Instant.parse("2026-03-22T10:00:00Z"),
        )

        val markdown = serializer.serialize(original, projectName = "Work")
        val parsed = parser.parse(markdown)

        assertEquals(original.id, parsed.task.id)
        assertEquals(original.title, parsed.task.title)
        assertEquals(original.status, parsed.task.status)
        assertEquals(original.priority, parsed.task.priority)
        assertEquals(original.dueDate?.date, parsed.task.dueDate?.date)
        assertEquals(original.energyLevel, parsed.task.energyLevel)
        assertEquals(original.estimatedMinutes, parsed.task.estimatedMinutes)
        assertEquals("Work", parsed.projectName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

- [ ] **Step 3: Write implementation**

```kotlin
package app.tsosu.data.markdown.tasknote

import app.tsosu.data.markdown.YamlFrontmatterParser
import app.tsosu.domain.model.*
import kotlinx.datetime.*

data class ParsedTaskNote(
    val task: Task,
    val projectName: String?,
)

class TaskNoteParser {

    private val yamlHelper = YamlFrontmatterParser()

    fun parse(content: String): ParsedTaskNote {
        val doc = yamlHelper.parse(content)
        val fm = doc.frontmatter

        val id = fm["id"] ?: error("TaskNote missing id")
        val status = fm["status"]?.let { parseStatus(it) } ?: TaskStatus.TODO
        val priority = fm["priority"]?.let { parsePriority(it) } ?: Priority.NONE
        val energy = fm["energy"]?.let { parseEnergy(it) } ?: EnergyLevel.MEDIUM

        // Extract title from first H1 heading in body
        val bodyLines = doc.body.trim().lines()
        val h1Idx = bodyLines.indexOfFirst { it.startsWith("# ") }
        val title = if (h1Idx >= 0) bodyLines[h1Idx].removePrefix("# ").trim() else "Untitled"
        val description = if (h1Idx >= 0) {
            bodyLines.drop(h1Idx + 1).joinToString("\n").trim()
        } else {
            doc.body.trim()
        }

        val task = Task(
            id = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            dueDate = fm["due"]?.let { LocalDateTime(LocalDate.parse(it), LocalTime(0, 0)) },
            scheduledDate = fm["scheduled"]?.let { LocalDateTime(LocalDate.parse(it), LocalTime(0, 0)) },
            startDate = fm["start"]?.let { LocalDateTime(LocalDate.parse(it), LocalTime(0, 0)) },
            reminderTime = fm["reminder"]?.let { parseTime(it) },
            completedDate = fm["completed"]?.let { LocalDateTime(LocalDate.parse(it), LocalTime(0, 0)) },
            cancelledDate = fm["cancelled"]?.let { LocalDateTime(LocalDate.parse(it), LocalTime(0, 0)) },
            energyLevel = energy,
            estimatedMinutes = fm["estimate"]?.removeSuffix("m")?.toIntOrNull(),
            recurrenceRule = fm["recurrence"],
            createdAt = fm["created"]?.let {
                LocalDate.parse(it).atStartOfDayIn(TimeZone.UTC)
            } ?: Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

        return ParsedTaskNote(task, fm["project"])
    }

    private fun parseStatus(s: String): TaskStatus = when (s.lowercase()) {
        "todo" -> TaskStatus.TODO
        "in_progress", "in-progress" -> TaskStatus.IN_PROGRESS
        "on_hold", "on-hold" -> TaskStatus.ON_HOLD
        "planned" -> TaskStatus.PLANNED
        "done" -> TaskStatus.DONE
        "cancelled" -> TaskStatus.CANCELLED
        else -> TaskStatus.TODO
    }

    private fun parsePriority(s: String): Priority = when (s.lowercase()) {
        "urgent", "highest" -> Priority.URGENT
        "high" -> Priority.HIGH
        "medium" -> Priority.MEDIUM
        "low" -> Priority.LOW
        else -> Priority.NONE
    }

    private fun parseEnergy(s: String): EnergyLevel = when (s.lowercase()) {
        "high" -> EnergyLevel.HIGH
        "medium" -> EnergyLevel.MEDIUM
        "low" -> EnergyLevel.LOW
        else -> EnergyLevel.MEDIUM
    }

    private fun parseTime(s: String): LocalTime {
        val parts = s.split(":")
        return LocalTime(parts[0].toInt(), parts[1].toInt())
    }
}
```

- [ ] **Step 4: Run tests, then commit**

---

## Task 4: HabitNote Serializer

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteSerializer.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/habitnote/HabitNoteSerializerTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.data.markdown.habitnote

import app.tsosu.domain.model.*
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HabitNoteSerializerTest {

    private val serializer = HabitNoteSerializer()

    private val habit = Habit(
        id = "h1",
        title = "Meditation",
        tinyVersion = "Take 3 deep breaths",
        frequency = HabitFrequency.DAILY,
        targetDaysPerWeek = 7,
        energyLevel = EnergyLevel.MEDIUM,
        color = "#4CAF50",
        createdAt = Instant.parse("2026-01-15T10:00:00Z"),
    )

    private val completions = listOf(
        HabitCompletion("h1", LocalDate.parse("2026-03-23"), Instant.parse("2026-03-23T08:00:00Z")),
        HabitCompletion("h1", LocalDate.parse("2026-03-22"), Instant.parse("2026-03-22T08:00:00Z")),
        HabitCompletion("h1", LocalDate.parse("2026-03-21"), Instant.parse("2026-03-21T08:00:00Z")),
    )

    @Test
    fun `serializes frontmatter with all fields`() {
        val result = serializer.serialize(habit, completions)

        assertTrue(result.contains("id: h1"))
        assertTrue(result.contains("frequency: daily"))
        assertTrue(result.contains("energy: medium"))
        assertTrue(result.contains("tiny: \"Take 3 deep breaths\""))
        assertTrue(result.contains("color: \"#4CAF50\""))
        assertTrue(result.contains("created: 2026-01-15"))
    }

    @Test
    fun `serializes title as h1 heading`() {
        val result = serializer.serialize(habit, completions)
        assertTrue(result.contains("# Meditation"))
    }

    @Test
    fun `serializes completions newest first`() {
        val result = serializer.serialize(habit, completions)
        assertTrue(result.contains("## Completions"))
        val lines = result.lines()
        val compLines = lines.filter { it.startsWith("- ✅") }
        assertTrue(compLines[0].contains("2026-03-23"))
        assertTrue(compLines[1].contains("2026-03-22"))
        assertTrue(compLines[2].contains("2026-03-21"))
    }

    @Test
    fun `custom frequency includes target`() {
        val custom = habit.copy(frequency = HabitFrequency.CUSTOM, targetDaysPerWeek = 3)
        val result = serializer.serialize(custom, emptyList())
        assertTrue(result.contains("frequency: custom"))
        assertTrue(result.contains("target_days: 3"))
    }

    @Test
    fun `omits tiny when null`() {
        val noTiny = habit.copy(tinyVersion = null)
        val result = serializer.serialize(noTiny, emptyList())
        assertTrue(!result.contains("tiny:"))
    }
}
```

- [ ] **Step 2: Run, fail, implement, pass, commit**

Implementation follows same pattern as TaskNoteSerializer — YAML frontmatter with id, frequency, target_days, energy, tiny, color, tags, archived, created. Body has `# Title`, optional notes section, `## Completions` with `- ✅ YYYY-MM-DD` entries newest first.

---

## Task 5: HabitNote Parser

Similar to TaskNoteParser. Parses YAML frontmatter for habit definition, extracts title from H1, parses completion lines. Returns `ParsedHabitNote(habit, completions)`.

---

## Task 6: Daily Note Writer (Write-Only)

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/dailynote/DailyNoteWriter.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/dailynote/DailyNoteWriterTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.data.markdown.dailynote

import app.tsosu.domain.model.*
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DailyNoteWriterTest {

    private val writer = DailyNoteWriter()

    @Test
    fun `generates daily note with habit checkboxes`() {
        val habits = listOf(
            Habit(id = "h1", title = "Meditation", energyLevel = EnergyLevel.MEDIUM,
                  createdAt = Clock.System.now()),
            Habit(id = "h2", title = "Exercise", energyLevel = EnergyLevel.HIGH,
                  createdAt = Clock.System.now()),
        )
        val completedIds = setOf("h1")
        val date = LocalDate.parse("2026-03-23")

        val result = writer.write(date, habits, completedIds)

        assertTrue(result.contains("date: 2026-03-23"))
        assertTrue(result.contains("## Habits"))
        assertTrue(result.contains("- [x] Meditation #habit <!-- id:h1 -->"))
        assertTrue(result.contains("- [ ] Exercise #habit <!-- id:h2 -->"))
    }

    @Test
    fun `filename is date-based`() {
        val filename = writer.filename(LocalDate.parse("2026-03-23"))
        assertTrue(filename == "2026-03-23.md")
    }

    @Test
    fun `empty habits produces minimal daily note`() {
        val result = writer.write(LocalDate.parse("2026-03-23"), emptyList(), emptySet())
        assertTrue(result.contains("date: 2026-03-23"))
        assertTrue(result.contains("## Habits"))
        // No checkbox lines
    }
}
```

- [ ] **Step 2: Implement**

```kotlin
package app.tsosu.data.markdown.dailynote

import app.tsosu.domain.model.Habit
import kotlinx.datetime.LocalDate

class DailyNoteWriter {

    fun write(date: LocalDate, habits: List<Habit>, completedHabitIds: Set<String>): String = buildString {
        appendLine("---")
        appendLine("date: $date")
        appendLine("---")
        appendLine()
        appendLine("## Habits")
        for (habit in habits.sortedBy { it.position }) {
            val checked = if (habit.id in completedHabitIds) "x" else " "
            appendLine("- [$checked] ${habit.title} #habit <!-- id:${habit.id} -->")
        }
    }

    fun filename(date: LocalDate): String = "$date.md"
}
```

- [ ] **Step 3: Test, commit**

---

## Task 7: Task Index Generator (tasks.md with wikilinks)

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/index/TaskIndexGenerator.kt`
- Test: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/index/TaskIndexGeneratorTest.kt`

- [ ] **Step 1: Write failing test**

Key behaviors to test:
- Tasks with `hasNote=true` get `[[tasks/slug]]` wikilink appended
- Tasks with `hasNote=false` are inline-only (no wikilink)
- Grouped by project, Inbox first, alphabetical sections
- Uses Obsidian Tasks standard emoji (from Phase 1)
- Extended statuses use correct checkbox markers

- [ ] **Step 2: Implement**

The `TaskIndexGenerator` wraps the existing `MarkdownTaskSerializer` logic but adds:
1. Wikilink after ID comment for tasks that have a note file
2. Accepts a `noteFilenames: Map<String, String>` (taskId → slug)

- [ ] **Step 3: Test, commit**

---

## Task 8: Habit Index Generator (habits.md with streaks + wikilinks)

Similar to Task Index Generator. Shows habits grouped by frequency with streak info and `[[habits/slug]]` wikilinks.

---

## Task 9: Expand MarkdownFileAccess for Folder Operations

**Files:**
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownFileAccess.kt`
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/SafMarkdownFileAccess.kt`

- [ ] **Step 1: Add folder operations to interface**

```kotlin
interface MarkdownFileAccess {
    // Existing
    suspend fun readTasksFile(): String?
    suspend fun writeTasksFile(content: String)
    suspend fun readHabitsFile(): String?
    suspend fun writeHabitsFile(content: String)

    // New: folder operations
    suspend fun listFolder(folderName: String): List<String>  // returns filenames
    suspend fun readFileInFolder(folderName: String, filename: String): String?
    suspend fun writeFileInFolder(folderName: String, filename: String, content: String)
    suspend fun ensureFolder(folderName: String)
}
```

- [ ] **Step 2: Implement in SafMarkdownFileAccess**

Use `DocumentFile.findFile(folderName)` or `createDirectory(folderName)` for subfolders. Iterate with `listFiles()` for the specific subfolder (not the whole vault, just tasks/ or habits/).

- [ ] **Step 3: Commit**

---

## Task 10: Refactor MarkdownSyncManager for Multi-File

**Files:**
- Modify: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncManager.kt`

- [ ] **Step 1: Update export flow**

```kotlin
suspend fun exportTasks(tasks: List<Task>, projectNames: Map<String, String>) {
    fileAccess.ensureFolder("tasks")

    val noteFilenames = mutableMapOf<String, String>()

    for (task in tasks) {
        if (shouldCreateTaskNote(task)) {
            val slug = taskNoteSerializer.slugify(task.title)
            val content = taskNoteSerializer.serialize(task, projectNames[task.projectId])
            fileAccess.writeFileInFolder("tasks", "$slug.md", content)
            noteFilenames[task.id] = slug
        }
    }

    // Regenerate index
    val indexContent = taskIndexGenerator.generate(tasks, projectNames, noteFilenames)
    fileAccess.writeTasksFile(indexContent)
}

suspend fun exportHabits(habits: List<Habit>, completions: List<HabitCompletion>) {
    fileAccess.ensureFolder("habits")

    val completionsByHabit = completions.groupBy { it.habitId }
    for (habit in habits) {
        val slug = habitNoteSerializer.slugify(habit.title)
        val content = habitNoteSerializer.serialize(habit, completionsByHabit[habit.id] ?: emptyList())
        fileAccess.writeFileInFolder("habits", "$slug.md", content)
    }

    // Regenerate index
    val indexContent = habitIndexGenerator.generate(habits, completions)
    fileAccess.writeHabitsFile(indexContent)
}

suspend fun exportDailyNote(date: LocalDate, habits: List<Habit>, completedIds: Set<String>) {
    fileAccess.ensureFolder("daily")
    val content = dailyNoteWriter.write(date, habits, completedIds)
    fileAccess.writeFileInFolder("daily", dailyNoteWriter.filename(date), content)
}

private fun shouldCreateTaskNote(task: Task): Boolean {
    return task.description.isNotBlank() || task.subtasks.isNotEmpty()
}
```

- [ ] **Step 2: Update import flow**

```kotlin
suspend fun importTasks(): ParsedTasks {
    val allTasks = mutableListOf<Task>()
    val projectSections = mutableMapOf<String, String>()
    val noteTaskIds = mutableSetOf<String>()

    // 1. Read individual TaskNote files (source of truth)
    val noteFiles = fileAccess.listFolder("tasks")
    for (filename in noteFiles) {
        if (!filename.endsWith(".md")) continue
        val content = fileAccess.readFileInFolder("tasks", filename) ?: continue
        val parsed = taskNoteParser.parse(content)
        allTasks.add(parsed.task)
        noteTaskIds.add(parsed.task.id)
        parsed.projectName?.let { projectSections[parsed.task.id] = it }
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

suspend fun importHabits(): ParsedHabits {
    val allHabits = mutableListOf<Habit>()
    val allCompletions = mutableListOf<HabitCompletion>()

    // Read individual HabitNote files (source of truth)
    val noteFiles = fileAccess.listFolder("habits")
    for (filename in noteFiles) {
        if (!filename.endsWith(".md")) continue
        val content = fileAccess.readFileInFolder("habits", filename) ?: continue
        val parsed = habitNoteParser.parse(content)
        allHabits.add(parsed.habit)
        allCompletions.addAll(parsed.completions)
    }

    return ParsedHabits(allHabits, allCompletions)
}
```

- [ ] **Step 3: Update MarkdownSyncRepository**

Add daily note export to the sync flow:

```kotlin
// After exporting habits, export today's daily note
val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
val todayCompletions = completions.filter { it.date == today }.map { it.habitId }.toSet()
syncManager.exportDailyNote(today, habits, todayCompletions)
```

- [ ] **Step 4: Build and run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug test --no-daemon`
Expected: BUILD SUCCESS, ALL PASS

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(markdown): multi-file sync with TaskNotes, HabitNotes, daily notes, and index generation"
```

---

## Task 11: Final Integration Test + Build Verification

- [ ] **Step 1: Run full build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew clean assembleDebug --no-daemon`

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test --no-daemon`

- [ ] **Step 3: Tag Phase 2 complete**

```bash
git tag phase2-complete
```
