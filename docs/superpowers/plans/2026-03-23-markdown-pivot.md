# Markdown-First Pivot Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Vikunja server sync with plain markdown files as the source of truth, enabling seamless interop with Obsidian/nvim on desktop via file sync (Syncthing/Obsidian Sync).

**Architecture:** Room DB stays as the fast in-app cache. A new `data-markdown` module reads/writes `.md` files in a user-selected folder using Android's Storage Access Framework (SAF). On every Room mutation, the app writes back to markdown. On app resume, the app re-reads markdown to pick up external edits. The `data-vikunja` module and all Vikunja sync infrastructure are removed.

**Tech Stack:** Kotlin, Room, Android SAF (DocumentFile API), kotlinx-datetime, Hilt, JUnit 5, MockK

---

## File Structure

### New files (data-markdown module)

| File | Responsibility |
|------|---------------|
| `data-markdown/build.gradle.kts` | Module build config (android.library, Hilt, kotlinx-datetime) |
| `data-markdown/src/main/AndroidManifest.xml` | Empty manifest for library module |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt` | Serialize `List<Task>` to Obsidian-compatible markdown string |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt` | Parse markdown string back to `List<Task>` (with metadata) |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializer.kt` | Serialize habits + completions to markdown string |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitParser.kt` | Parse habits + completions from markdown string |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncManager.kt` | Orchestrate Room <-> markdown file reads/writes via SAF |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncRepository.kt` | Implement `SyncRepository` interface for markdown sync |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownPreferences.kt` | DataStore for folder URI, last-sync timestamp |
| `data-markdown/src/main/kotlin/app/tsosu/data/markdown/di/MarkdownModule.kt` | Hilt DI module for markdown components |
| `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt` | Unit tests for task serialization |
| `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt` | Unit tests for task parsing |
| `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializerTest.kt` | Unit tests for habit serialization |
| `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitParserTest.kt` | Unit tests for habit parsing |
| `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownSyncManagerTest.kt` | Unit tests for sync orchestration |

### Modified files

| File | Change |
|------|--------|
| `settings.gradle.kts` | Add `:data-markdown`, remove `:data-vikunja` |
| `app/build.gradle.kts` | Replace `data-vikunja` dep with `data-markdown`, remove WorkManager deps |
| `app/src/main/java/app/tsosu/di/RepositoryModule.kt` | Remove SyncDispatcher wiring from TaskRepository |
| `app/src/main/java/app/tsosu/MainActivity.kt` | Remove SyncWorker/resume-pull, add markdown sync-on-resume |
| `app/src/main/java/app/tsosu/TsosuApp.kt` | Remove WorkManager Configuration.Provider |
| `app/src/main/AndroidManifest.xml` | Remove WorkManagerInitializer disabling, remove INTERNET permission |
| `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt` | Replace Vikunja section with folder picker |
| `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt` | Replace Vikunja connect/sync with folder selection and markdown sync |
| `domain/src/main/kotlin/app/tsosu/domain/repository/SyncRepository.kt` | Simplify interface (remove server auth, remove `ServerInfo`, rename `isRemoteConfigured` -> `isConfigured`) |
| `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt` | Add `getAllTasks()` query |
| `data-local/src/main/kotlin/app/tsosu/data/local/dao/HabitDao.kt` | Add `getAllCompletionsForHabit(habitId)` overload without date range |
| `data-local/src/main/kotlin/app/tsosu/data/local/entity/SyncQueueEntity.kt` | DELETE (no longer needed) |
| `data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt` | DELETE |
| `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt` | Remove SyncQueueEntity from schema, bump version |

**NOTE on intermediate builds:** Tasks 8-11 form an atomic batch — the `app` module will NOT compile until all four tasks are complete. This is because Task 8 changes the `SyncRepository` interface, Tasks 9-10 update the DI wiring and callers, and Task 11 updates the UI. Run the build verification at the end of Task 11, not between tasks.

**NOTE on dead columns:** `TaskEntity.serverId` and `TaskEntity.syncStatus` become unused after this pivot. Removing them requires a Room schema migration. Tracked as tech debt — leave them for now.

### Deleted files/directories

| Path | Reason |
|------|--------|
| `data-vikunja/` (entire module) | Vikunja sync engine no longer needed |

---

## Markdown Format Spec

Two files: `tasks.md` and `habits.md`, designed for Obsidian Tasks plugin compatibility.

### tasks.md

```markdown
---
tsosu: v1
updated: 2026-03-23T16:00:00+08:00
---

## Inbox

- [ ] Buy groceries #errands ⚡high 🍅30m
- [ ] Fix bike tire #home ⚡medium 🍅15m
- [x] Call dentist ✅ 2026-03-22 #errands ⚡low

## Work

- [ ] Prepare presentation 📅 2026-03-25 ⚡high 🍅60m ‼️urgent
- [ ] Review PR #dev ⚡medium 🍅20m

## Personal

- [ ] Read chapter 5 📅 2026-03-28 ⚡low 🍅30m
```

**Format per line:**
```
- [x| ] <title> [✅ YYYY-MM-DD] [📅 YYYY-MM-DD] [#project] [⚡high|medium|low] [🍅Nm] [‼️urgent|❗high|❕medium|🔽low]
```

- `- [ ]` / `- [x]` — Obsidian checkbox (done state)
- `✅ YYYY-MM-DD` — completion date (Obsidian Tasks format)
- `📅 YYYY-MM-DD` — due date (Obsidian Tasks format)
- `#project` — project as tag (maps to h2 section heading)
- `⚡high|medium|low` — energy level
- `🍅Nm` — estimated minutes (pomodoro-style)
- `‼️urgent|❗high|❕medium|🔽low` — priority markers

**Section headings** (`## Name`) map to projects. `## Inbox` is the default (no project).

### habits.md

```markdown
---
tsosu: v1
updated: 2026-03-23T16:00:00+08:00
---

## Daily

- [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium
  - ✅ 2026-03-23
  - ✅ 2026-03-22
  - ✅ 2026-03-21
- [ ] Read 30min (tiny: read 1 page) 🔁daily ⚡low
  - ✅ 2026-03-23
  - ✅ 2026-03-20

## Weekdays

- [ ] Morning standup 🔁weekdays ⚡low

## Custom

- [ ] Grocery run 🔁3x/week ⚡medium
  - ✅ 2026-03-22
  - ✅ 2026-03-19
```

**Format per habit line:**
```
- [ ] <title> [(tiny: <tiny_version>)] 🔁<frequency> [⚡energy]
  - ✅ YYYY-MM-DD    (one per completion, indented, newest first)
```

- `🔁daily|weekdays|Nx/week` — frequency
- Completions as indented `✅` lines under the habit
- Section headings group by frequency
- Habits are never `[x]` — they reset daily; the `✅` sub-items track completions

---

## Tasks

### Task 1: Create data-markdown module skeleton

**Files:**
- Create: `data-markdown/build.gradle.kts`
- Create: `data-markdown/src/main/AndroidManifest.xml`
- Modify: `settings.gradle.kts`

- [ ] **Step 1: Create build.gradle.kts**

```kotlin
// data-markdown/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "app.tsosu.data.markdown"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data-local"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.datastore.preferences)
    implementation(libs.documentfile)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)

    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Create empty AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 3: Add documentfile to libs.versions.toml**

Add under `[libraries]`:
```toml
documentfile = { group = "androidx.documentfile", name = "documentfile", version = "1.0.1" }
```

- [ ] **Step 4: Update settings.gradle.kts**

Replace `include(":data-vikunja")` with `include(":data-markdown")`.

```kotlin
include(":app")
include(":domain")
include(":data-local")
include(":data-markdown")
include(":data-calendar")
```

- [ ] **Step 5: Verify module compiles**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add data-markdown/build.gradle.kts data-markdown/src/main/AndroidManifest.xml settings.gradle.kts gradle/libs.versions.toml
git commit -m "feat(markdown): add data-markdown module skeleton"
```

---

### Task 2: MarkdownTaskSerializer — write tasks to markdown

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializerTest.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class MarkdownTaskSerializerTest {

    private val serializer = MarkdownTaskSerializer()

    @Test
    fun `serialize empty list produces frontmatter only`() {
        val result = serializer.serialize(emptyList())
        assertContains(result, "tsosu: v1")
        assertContains(result, "updated:")
        assertContains(result, "## Inbox")
    }

    @Test
    fun `serialize single inbox task`() {
        val task = Task(
            id = "abc-123",
            title = "Buy groceries",
            projectId = null,
        )
        val result = serializer.serialize(listOf(task))
        assertContains(result, "- [ ] Buy groceries")
        assertContains(result, "<!-- id:abc-123 -->")
    }

    @Test
    fun `serialize done task with completion date`() {
        val task = Task(
            id = "done-1",
            title = "Call dentist",
            done = true,
            updatedAt = kotlinx.datetime.Instant.parse("2026-03-22T10:00:00Z"),
        )
        val result = serializer.serialize(listOf(task))
        assertContains(result, "- [x] Call dentist ✅ 2026-03-22")
    }

    @Test
    fun `serialize task with due date`() {
        val task = Task(
            id = "due-1",
            title = "Prepare presentation",
            dueDate = LocalDateTime(2026, 3, 25, 0, 0),
        )
        val result = serializer.serialize(listOf(task))
        assertContains(result, "📅 2026-03-25")
    }

    @Test
    fun `serialize task with all metadata`() {
        val task = Task(
            id = "full-1",
            title = "Review PR",
            priority = Priority.URGENT,
            energyLevel = EnergyLevel.HIGH,
            estimatedMinutes = 30,
            dueDate = LocalDateTime(2026, 3, 25, 0, 0),
        )
        val result = serializer.serialize(listOf(task))
        assertContains(result, "⚡high")
        assertContains(result, "🍅30m")
        assertContains(result, "‼️urgent")
        assertContains(result, "📅 2026-03-25")
    }

    @Test
    fun `tasks grouped by project as sections`() {
        val tasks = listOf(
            Task(id = "1", title = "Inbox task", projectId = null),
            Task(id = "2", title = "Work task", projectId = "proj-work"),
        )
        // Note: serializer needs project name mapping; projectId -> name
        val result = serializer.serialize(tasks, projectNames = mapOf("proj-work" to "Work"))
        assertContains(result, "## Inbox")
        assertContains(result, "## Work")
    }

    @Test
    fun `serialize preserves task order within sections`() {
        val tasks = listOf(
            Task(id = "1", title = "First", position = 1.0),
            Task(id = "2", title = "Second", position = 2.0),
        )
        val result = serializer.serialize(tasks)
        val firstIdx = result.indexOf("First")
        val secondIdx = result.indexOf("Second")
        assert(firstIdx < secondIdx) { "First should appear before Second" }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: FAIL — MarkdownTaskSerializer class not found

- [ ] **Step 3: Write MarkdownTaskSerializer**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskSerializer.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MarkdownTaskSerializer {

    fun serialize(
        tasks: List<Task>,
        projectNames: Map<String, String> = emptyMap(),
    ): String = buildString {
        appendFrontmatter()
        appendLine()

        val grouped = tasks
            .sortedBy { it.position }
            .groupBy { it.projectId }

        // Inbox first (null projectId)
        appendSection("Inbox", grouped[null].orEmpty())

        // Then named projects
        grouped.filterKeys { it != null }.forEach { (projectId, projectTasks) ->
            val name = projectNames[projectId] ?: projectId ?: "Unknown"
            appendSection(name, projectTasks)
        }
    }

    private fun StringBuilder.appendFrontmatter() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("updated: $now")
        appendLine("---")
    }

    private fun StringBuilder.appendSection(name: String, tasks: List<Task>) {
        appendLine("## $name")
        appendLine()
        for (task in tasks) {
            appendLine(formatTask(task))
        }
        appendLine()
    }

    internal fun formatTask(task: Task): String = buildString {
        // Checkbox
        append(if (task.done) "- [x] " else "- [ ] ")

        // Title
        append(task.title)

        // Completion date
        if (task.done) {
            val date = task.updatedAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            append(" ✅ $date")
        }

        // Due date
        task.dueDate?.let { append(" 📅 ${it.date}") }

        // Energy level
        if (task.energyLevel != EnergyLevel.MEDIUM) {
            append(" ⚡${task.energyLevel.name.lowercase()}")
        } else {
            append(" ⚡medium")
        }

        // Estimated minutes
        task.estimatedMinutes?.let { append(" 🍅${it}m") }

        // Priority
        when (task.priority) {
            Priority.URGENT -> append(" ‼️urgent")
            Priority.HIGH -> append(" ❗high")
            Priority.MEDIUM -> append(" ❕medium")
            Priority.LOW -> append(" 🔽low")
            Priority.NONE -> {} // omit
        }

        // Hidden ID for round-tripping
        append(" <!-- id:${task.id} -->")
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/
git commit -m "feat(markdown): add MarkdownTaskSerializer with Obsidian-compatible format"
```

---

### Task 3: MarkdownTaskParser — read tasks from markdown

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownTaskParserTest.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class MarkdownTaskParserTest {

    private val parser = MarkdownTaskParser()

    @Test
    fun `parse empty file returns empty list`() {
        val result = parser.parse("")
        assertTrue(result.tasks.isEmpty())
    }

    @Test
    fun `parse frontmatter-only returns empty list`() {
        val md = """
            ---
            tsosu: v1
            updated: 2026-03-23T16:00:00
            ---

            ## Inbox

        """.trimIndent()
        val result = parser.parse(md)
        assertTrue(result.tasks.isEmpty())
    }

    @Test
    fun `parse single undone task`() {
        val md = """
            ---
            tsosu: v1
            updated: 2026-03-23T16:00:00
            ---

            ## Inbox

            - [ ] Buy groceries <!-- id:abc-123 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(1, result.tasks.size)
        val task = result.tasks[0]
        assertEquals("Buy groceries", task.title)
        assertEquals("abc-123", task.id)
        assertEquals(false, task.done)
        assertNull(task.projectId)
    }

    @Test
    fun `parse done task with completion date`() {
        val md = """
            ## Inbox

            - [x] Call dentist ✅ 2026-03-22 <!-- id:done-1 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(1, result.tasks.size)
        assertTrue(result.tasks[0].done)
    }

    @Test
    fun `parse task with due date`() {
        val md = """
            ## Inbox

            - [ ] Prepare presentation 📅 2026-03-25 <!-- id:due-1 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(2026, result.tasks[0].dueDate?.year)
        assertEquals(3, result.tasks[0].dueDate?.monthNumber)
        assertEquals(25, result.tasks[0].dueDate?.dayOfMonth)
    }

    @Test
    fun `parse task with all metadata`() {
        val md = """
            ## Work

            - [ ] Review PR ⚡high 🍅30m ‼️urgent 📅 2026-03-25 <!-- id:full-1 -->
        """.trimIndent()
        val result = parser.parse(md)
        val task = result.tasks[0]
        assertEquals("Review PR", task.title)
        assertEquals(EnergyLevel.HIGH, task.energyLevel)
        assertEquals(30, task.estimatedMinutes)
        assertEquals(Priority.URGENT, task.priority)
    }

    @Test
    fun `parse section heading as project`() {
        val md = """
            ## Work

            - [ ] Work task <!-- id:w1 -->

            ## Personal

            - [ ] Personal task <!-- id:p1 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals("Work", result.projectSections["w1"])
        assertEquals("Personal", result.projectSections["p1"])
    }

    @Test
    fun `parse task without id generates new id`() {
        val md = """
            ## Inbox

            - [ ] New task from Obsidian
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(1, result.tasks.size)
        assertTrue(result.tasks[0].id.isNotBlank())
        assertEquals("New task from Obsidian", result.tasks[0].title)
    }

    @Test
    fun `round-trip serializer then parser preserves data`() {
        val serializer = MarkdownTaskSerializer()
        val original = listOf(
            app.tsosu.domain.model.Task(
                id = "rt-1",
                title = "Round trip task",
                priority = Priority.HIGH,
                energyLevel = EnergyLevel.LOW,
                estimatedMinutes = 45,
                dueDate = kotlinx.datetime.LocalDateTime(2026, 4, 1, 0, 0),
            )
        )
        val md = serializer.serialize(original)
        val parsed = parser.parse(md)
        assertEquals(1, parsed.tasks.size)
        val task = parsed.tasks[0]
        assertEquals("rt-1", task.id)
        assertEquals("Round trip task", task.title)
        assertEquals(Priority.HIGH, task.priority)
        assertEquals(EnergyLevel.LOW, task.energyLevel)
        assertEquals(45, task.estimatedMinutes)
        assertEquals(2026, task.dueDate?.year)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: FAIL — MarkdownTaskParser class not found

- [ ] **Step 3: Write MarkdownTaskParser**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownTaskParser.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Priority
import app.tsosu.domain.model.Task
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ParsedTasks(
    val tasks: List<Task>,
    val projectSections: Map<String, String>, // taskId -> sectionName
)

class MarkdownTaskParser {

    private val taskLineRegex = Regex("""^- \[([ x])] (.+)$""")
    private val idRegex = Regex("""<!--\s*id:(\S+)\s*-->""")
    private val dueDateRegex = Regex("""📅\s*(\d{4}-\d{2}-\d{2})""")
    private val completionDateRegex = Regex("""✅\s*(\d{4}-\d{2}-\d{2})""")
    private val energyRegex = Regex("""⚡(high|medium|low)""")
    private val estimateRegex = Regex("""🍅(\d+)m""")
    private val priorityRegex = Regex("""(‼️urgent|❗high|❕medium|🔽low)""")
    private val sectionRegex = Regex("""^## (.+)$""")

    fun parse(markdown: String): ParsedTasks {
        val tasks = mutableListOf<Task>()
        val projectSections = mutableMapOf<String, String>()
        var currentSection = "Inbox"

        for (line in markdown.lines()) {
            val sectionMatch = sectionRegex.find(line.trim())
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1].trim()
                continue
            }

            val taskMatch = taskLineRegex.find(line.trim()) ?: continue
            val done = taskMatch.groupValues[1] == "x"
            val rest = taskMatch.groupValues[2]

            val id = idRegex.find(rest)?.groupValues?.get(1) ?: generateId()
            val dueDate = dueDateRegex.find(rest)?.groupValues?.get(1)?.let {
                LocalDate.parse(it).atTime(0, 0)
            }
            val energy = energyRegex.find(rest)?.groupValues?.get(1)?.let {
                when (it) {
                    "high" -> EnergyLevel.HIGH
                    "low" -> EnergyLevel.LOW
                    else -> EnergyLevel.MEDIUM
                }
            } ?: EnergyLevel.MEDIUM
            val estimate = estimateRegex.find(rest)?.groupValues?.get(1)?.toIntOrNull()
            val priority = priorityRegex.find(rest)?.groupValues?.get(1)?.let {
                when {
                    it.contains("urgent") -> Priority.URGENT
                    it.contains("high") -> Priority.HIGH
                    it.contains("medium") -> Priority.MEDIUM
                    it.contains("low") -> Priority.LOW
                    else -> Priority.NONE
                }
            } ?: Priority.NONE

            // Extract clean title by removing metadata markers and id comment
            val title = rest
                .replace(idRegex, "")
                .replace(dueDateRegex, "")
                .replace(completionDateRegex, "")
                .replace(energyRegex, "")
                .replace(estimateRegex, "")
                .replace(priorityRegex, "")
                .replace("⚡", "")
                .trim()

            val task = Task(
                id = id,
                title = title,
                done = done,
                dueDate = dueDate,
                priority = priority,
                energyLevel = energy,
                estimatedMinutes = estimate,
                position = tasks.size.toDouble(),
            )
            tasks.add(task)

            if (currentSection != "Inbox") {
                projectSections[id] = currentSection
            }
        }

        return ParsedTasks(tasks = tasks, projectSections = projectSections)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateId(): String = Uuid.random().toString()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/
git commit -m "feat(markdown): add MarkdownTaskParser with round-trip support"
```

---

### Task 4: MarkdownHabitSerializer + Parser — habits with completion tracking

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializer.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitParser.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializerTest.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitParserTest.kt`

- [ ] **Step 1: Write habit serializer tests**

```kotlin
// data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializerTest.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class MarkdownHabitSerializerTest {

    private val serializer = MarkdownHabitSerializer()

    @Test
    fun `serialize empty habits produces frontmatter only`() {
        val result = serializer.serialize(emptyList(), emptyList())
        assertContains(result, "tsosu: v1")
    }

    @Test
    fun `serialize habit with completions`() {
        val habit = Habit(
            id = "h1",
            title = "Exercise",
            tinyVersion = "do 1 pushup",
            frequency = HabitFrequency.DAILY,
            energyLevel = EnergyLevel.MEDIUM,
        )
        val completions = listOf(
            HabitCompletion("h1", LocalDate(2026, 3, 23), Instant.parse("2026-03-23T10:00:00Z")),
            HabitCompletion("h1", LocalDate(2026, 3, 22), Instant.parse("2026-03-22T10:00:00Z")),
        )
        val result = serializer.serialize(listOf(habit), completions)
        assertContains(result, "- [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium")
        assertContains(result, "  - ✅ 2026-03-23")
        assertContains(result, "  - ✅ 2026-03-22")
    }

    @Test
    fun `serialize groups by frequency`() {
        val habits = listOf(
            Habit(id = "h1", title = "Daily habit", frequency = HabitFrequency.DAILY),
            Habit(id = "h2", title = "Weekday habit", frequency = HabitFrequency.WEEKDAYS),
        )
        val result = serializer.serialize(habits, emptyList())
        assertContains(result, "## Daily")
        assertContains(result, "## Weekdays")
    }
}
```

- [ ] **Step 2: Write habit parser tests**

```kotlin
// data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownHabitParserTest.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.HabitFrequency
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarkdownHabitParserTest {

    private val parser = MarkdownHabitParser()

    @Test
    fun `parse empty returns empty`() {
        val result = parser.parse("")
        assertTrue(result.habits.isEmpty())
        assertTrue(result.completions.isEmpty())
    }

    @Test
    fun `parse habit with completions`() {
        val md = """
            ## Daily

            - [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium <!-- id:h1 -->
              - ✅ 2026-03-23
              - ✅ 2026-03-22
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(1, result.habits.size)
        assertEquals("Exercise", result.habits[0].title)
        assertEquals("do 1 pushup", result.habits[0].tinyVersion)
        assertEquals(HabitFrequency.DAILY, result.habits[0].frequency)
        assertEquals(EnergyLevel.MEDIUM, result.habits[0].energyLevel)
        assertEquals(2, result.completions.size)
        assertEquals("h1", result.completions[0].habitId)
    }

    @Test
    fun `parse weekdays frequency`() {
        val md = """
            ## Weekdays

            - [ ] Standup 🔁weekdays ⚡low <!-- id:h2 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(HabitFrequency.WEEKDAYS, result.habits[0].frequency)
    }

    @Test
    fun `parse custom frequency`() {
        val md = """
            ## Custom

            - [ ] Grocery run 🔁3x/week ⚡medium <!-- id:h3 -->
        """.trimIndent()
        val result = parser.parse(md)
        assertEquals(HabitFrequency.CUSTOM, result.habits[0].frequency)
        assertEquals(3, result.habits[0].targetDaysPerWeek)
    }

    @Test
    fun `round-trip habits preserve data`() {
        val serializer = MarkdownHabitSerializer()
        val habit = app.tsosu.domain.model.Habit(
            id = "rt-h1",
            title = "Meditate",
            tinyVersion = "breathe 3 times",
            frequency = HabitFrequency.DAILY,
            energyLevel = EnergyLevel.LOW,
        )
        val completions = listOf(
            app.tsosu.domain.model.HabitCompletion(
                "rt-h1",
                kotlinx.datetime.LocalDate(2026, 3, 23),
                kotlinx.datetime.Instant.parse("2026-03-23T10:00:00Z"),
            )
        )
        val md = serializer.serialize(listOf(habit), completions)
        val parsed = parser.parse(md)
        assertEquals("rt-h1", parsed.habits[0].id)
        assertEquals("Meditate", parsed.habits[0].title)
        assertEquals("breathe 3 times", parsed.habits[0].tinyVersion)
        assertEquals(1, parsed.completions.size)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: FAIL

- [ ] **Step 4: Write MarkdownHabitSerializer**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitSerializer.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class MarkdownHabitSerializer {

    fun serialize(habits: List<Habit>, completions: List<HabitCompletion>): String = buildString {
        appendFrontmatter()
        appendLine()

        val completionsByHabit = completions.groupBy { it.habitId }
        val grouped = habits
            .sortedBy { it.position }
            .groupBy { it.frequency }

        val sectionOrder = listOf(HabitFrequency.DAILY, HabitFrequency.WEEKDAYS, HabitFrequency.CUSTOM)
        for (freq in sectionOrder) {
            val sectionHabits = grouped[freq] ?: continue
            val sectionName = when (freq) {
                HabitFrequency.DAILY -> "Daily"
                HabitFrequency.WEEKDAYS -> "Weekdays"
                HabitFrequency.CUSTOM -> "Custom"
            }
            appendLine("## $sectionName")
            appendLine()
            for (habit in sectionHabits) {
                appendLine(formatHabit(habit))
                val hCompletions = completionsByHabit[habit.id].orEmpty()
                    .sortedByDescending { it.date }
                for (c in hCompletions) {
                    appendLine("  - ✅ ${c.date}")
                }
            }
            appendLine()
        }
    }

    private fun StringBuilder.appendFrontmatter() {
        val now = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        appendLine("---")
        appendLine("tsosu: v1")
        appendLine("updated: $now")
        appendLine("---")
    }

    internal fun formatHabit(habit: Habit): String = buildString {
        append("- [ ] ")
        append(habit.title)

        habit.tinyVersion?.let { append(" (tiny: $it)") }

        when (habit.frequency) {
            HabitFrequency.DAILY -> append(" 🔁daily")
            HabitFrequency.WEEKDAYS -> append(" 🔁weekdays")
            HabitFrequency.CUSTOM -> append(" 🔁${habit.targetDaysPerWeek}x/week")
        }

        append(" ⚡${habit.energyLevel.name.lowercase()}")
        append(" <!-- id:${habit.id} -->")
    }
}
```

- [ ] **Step 5: Write MarkdownHabitParser**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownHabitParser.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.EnergyLevel
import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.HabitFrequency
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class ParsedHabits(
    val habits: List<Habit>,
    val completions: List<HabitCompletion>,
)

class MarkdownHabitParser {

    private val habitLineRegex = Regex("""^- \[[ ]] (.+)$""")
    private val idRegex = Regex("""<!--\s*id:(\S+)\s*-->""")
    private val tinyRegex = Regex("""\(tiny:\s*(.+?)\)""")
    private val frequencyRegex = Regex("""🔁(daily|weekdays|(\d+)x/week)""")
    private val energyRegex = Regex("""⚡(high|medium|low)""")
    private val completionLineRegex = Regex("""^\s+- ✅\s*(\d{4}-\d{2}-\d{2})$""")

    fun parse(markdown: String): ParsedHabits {
        val habits = mutableListOf<Habit>()
        val completions = mutableListOf<HabitCompletion>()
        var currentHabitId: String? = null

        for (line in markdown.lines()) {
            // Check for completion line first (must be under a habit)
            val completionMatch = completionLineRegex.find(line)
            if (completionMatch != null && currentHabitId != null) {
                val date = LocalDate.parse(completionMatch.groupValues[1])
                completions.add(
                    HabitCompletion(
                        habitId = currentHabitId,
                        date = date,
                        completedAt = date.atStartOfDayIn(TimeZone.UTC),
                    )
                )
                continue
            }

            val habitMatch = habitLineRegex.find(line.trim()) ?: run {
                if (!line.trim().startsWith("- ")) currentHabitId = null
                continue
            }

            val rest = habitMatch.groupValues[1]
            val id = idRegex.find(rest)?.groupValues?.get(1) ?: generateId()
            val tinyVersion = tinyRegex.find(rest)?.groupValues?.get(1)
            val energy = energyRegex.find(rest)?.groupValues?.get(1)?.let {
                when (it) {
                    "high" -> EnergyLevel.HIGH
                    "low" -> EnergyLevel.LOW
                    else -> EnergyLevel.MEDIUM
                }
            } ?: EnergyLevel.MEDIUM

            val freqMatch = frequencyRegex.find(rest)
            val frequency: HabitFrequency
            val targetDays: Int
            when {
                freqMatch == null -> { frequency = HabitFrequency.DAILY; targetDays = 7 }
                freqMatch.groupValues[1] == "daily" -> { frequency = HabitFrequency.DAILY; targetDays = 7 }
                freqMatch.groupValues[1] == "weekdays" -> { frequency = HabitFrequency.WEEKDAYS; targetDays = 5 }
                else -> {
                    frequency = HabitFrequency.CUSTOM
                    targetDays = freqMatch.groupValues[2].toIntOrNull() ?: 3
                }
            }

            // Clean title
            val title = rest
                .replace(idRegex, "")
                .replace(tinyRegex, "")
                .replace(frequencyRegex, "")
                .replace(energyRegex, "")
                .replace("⚡", "")
                .replace("🔁", "")
                .trim()

            val habit = Habit(
                id = id,
                title = title,
                tinyVersion = tinyVersion,
                frequency = frequency,
                targetDaysPerWeek = targetDays,
                energyLevel = energy,
                position = habits.size.toDouble(),
            )
            habits.add(habit)
            currentHabitId = id
        }

        return ParsedHabits(habits = habits, completions = completions)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateId(): String = Uuid.random().toString()
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: ALL PASS

- [ ] **Step 7: Commit**

```bash
git add data-markdown/src/
git commit -m "feat(markdown): add habit serializer/parser with completion tracking"
```

---

### Task 5: MarkdownPreferences — folder URI and sync state

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownPreferences.kt`

- [ ] **Step 1: Write MarkdownPreferences**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownPreferences.kt
package app.tsosu.data.markdown

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.markdownDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "markdown_prefs")

class MarkdownPreferences(private val context: Context) {

    private val folderUriKey = stringPreferencesKey("folder_uri")
    private val lastSyncKey = longPreferencesKey("last_sync")

    fun folderUri(): Flow<Uri?> = context.markdownDataStore.data.map { prefs ->
        prefs[folderUriKey]?.let { Uri.parse(it) }
    }

    fun isConfigured(): Flow<Boolean> = context.markdownDataStore.data.map { prefs ->
        prefs[folderUriKey] != null
    }

    suspend fun setFolderUri(uri: Uri) {
        context.markdownDataStore.edit { prefs ->
            prefs[folderUriKey] = uri.toString()
        }
    }

    suspend fun getFolderUri(): Uri? =
        context.markdownDataStore.data.first()[folderUriKey]?.let { Uri.parse(it) }

    suspend fun getLastSync(): Long =
        context.markdownDataStore.data.first()[lastSyncKey] ?: 0L

    suspend fun setLastSync(timestamp: Long) {
        context.markdownDataStore.edit { prefs ->
            prefs[lastSyncKey] = timestamp
        }
    }

    suspend fun clear() {
        context.markdownDataStore.edit { it.clear() }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownPreferences.kt
git commit -m "feat(markdown): add MarkdownPreferences for folder URI storage"
```

---

### Task 6: MarkdownSyncManager — orchestrate Room <-> markdown files

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncManager.kt`
- Create: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownSyncManagerTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// data-markdown/src/test/kotlin/app/tsosu/data/markdown/MarkdownSyncManagerTest.kt
package app.tsosu.data.markdown

import app.tsosu.domain.model.Habit
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.model.Task
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertContains

class MarkdownSyncManagerTest {

    private val fileAccess = mockk<MarkdownFileAccess>(relaxed = true)
    private val serializer = MarkdownTaskSerializer()
    private val parser = MarkdownTaskParser()
    private val habitSerializer = MarkdownHabitSerializer()
    private val habitParser = MarkdownHabitParser()

    private val manager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = serializer,
        taskParser = parser,
        habitSerializer = habitSerializer,
        habitParser = habitParser,
    )

    @Test
    fun `exportTasks writes serialized markdown to file`() = runTest {
        val tasks = listOf(Task(id = "t1", title = "Test task"))
        val written = slot<String>()
        coEvery { fileAccess.writeTasksFile(capture(written)) } returns Unit

        manager.exportTasks(tasks, emptyMap())

        assertContains(written.captured, "Test task")
        assertContains(written.captured, "<!-- id:t1 -->")
    }

    @Test
    fun `importTasks reads and parses markdown file`() = runTest {
        val md = """
            ## Inbox

            - [ ] Imported task <!-- id:imp-1 -->
        """.trimIndent()
        coEvery { fileAccess.readTasksFile() } returns md

        val result = manager.importTasks()

        assertEquals(1, result.tasks.size)
        assertEquals("Imported task", result.tasks[0].title)
    }

    @Test
    fun `importTasks returns empty when file does not exist`() = runTest {
        coEvery { fileAccess.readTasksFile() } returns null

        val result = manager.importTasks()

        assertEquals(0, result.tasks.size)
    }

    @Test
    fun `exportHabits writes serialized habits to file`() = runTest {
        val habits = listOf(Habit(id = "h1", title = "Exercise"))
        val written = slot<String>()
        coEvery { fileAccess.writeHabitsFile(capture(written)) } returns Unit

        manager.exportHabits(habits, emptyList())

        assertContains(written.captured, "Exercise")
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: FAIL

- [ ] **Step 3: Write MarkdownFileAccess interface and MarkdownSyncManager**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownFileAccess.kt
package app.tsosu.data.markdown

/**
 * Abstraction over file I/O for testability.
 * Production impl uses Android SAF (DocumentFile).
 */
interface MarkdownFileAccess {
    suspend fun readTasksFile(): String?
    suspend fun writeTasksFile(content: String)
    suspend fun readHabitsFile(): String?
    suspend fun writeHabitsFile(content: String)
}
```

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncManager.kt
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
        val content = fileAccess.readTasksFile() ?: return ParsedTasks(emptyList(), emptyMap())
        return taskParser.parse(content)
    }

    suspend fun exportHabits(habits: List<Habit>, completions: List<HabitCompletion>) {
        val content = habitSerializer.serialize(habits, completions)
        fileAccess.writeHabitsFile(content)
    }

    suspend fun importHabits(): ParsedHabits {
        val content = fileAccess.readHabitsFile() ?: return ParsedHabits(emptyList(), emptyList())
        return habitParser.parse(content)
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :data-markdown:test`
Expected: ALL PASS

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/
git commit -m "feat(markdown): add MarkdownSyncManager with MarkdownFileAccess abstraction"
```

---

### Task 7: SAF-based MarkdownFileAccess implementation

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/SafMarkdownFileAccess.kt`

- [ ] **Step 1: Write SafMarkdownFileAccess**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/SafMarkdownFileAccess.kt
package app.tsosu.data.markdown

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class SafMarkdownFileAccess(
    private val context: Context,
    private val folderUriProvider: suspend () -> Uri?,
) : MarkdownFileAccess {

    override suspend fun readTasksFile(): String? = readFile(TASKS_FILENAME)

    override suspend fun writeTasksFile(content: String) = writeFile(TASKS_FILENAME, content)

    override suspend fun readHabitsFile(): String? = readFile(HABITS_FILENAME)

    override suspend fun writeHabitsFile(content: String) = writeFile(HABITS_FILENAME, content)

    private suspend fun readFile(filename: String): String? {
        val folderUri = folderUriProvider() ?: return null
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return null
        val file = folder.findFile(filename) ?: return null
        return context.contentResolver.openInputStream(file.uri)?.use { stream ->
            stream.bufferedReader().readText()
        }
    }

    private suspend fun writeFile(filename: String, content: String) {
        val folderUri = folderUriProvider() ?: return
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return
        val file = folder.findFile(filename)
            ?: folder.createFile("text/markdown", filename)
            ?: return
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { stream ->
            stream.bufferedWriter().use { it.write(content) }
        }
    }

    companion object {
        const val TASKS_FILENAME = "tasks.md"
        const val HABITS_FILENAME = "habits.md"
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add data-markdown/src/main/kotlin/app/tsosu/data/markdown/SafMarkdownFileAccess.kt
git commit -m "feat(markdown): add SAF-based file access for markdown read/write"
```

---

### Task 8: MarkdownSyncRepository — implement SyncRepository interface

**Files:**
- Modify: `domain/src/main/kotlin/app/tsosu/domain/repository/SyncRepository.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/dao/HabitDao.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncRepository.kt`

**NOTE:** After this task, the `app` module will NOT compile until Tasks 9-11 are also complete (interface changed, callers not yet updated). The domain and data modules will compile independently.

- [ ] **Step 1: Simplify SyncRepository interface**

Replace the Vikunja-specific interface with a simpler one. This removes `ServerInfo`, `configureServer()`, `login()`, and renames `isRemoteConfigured()` to `isConfigured()`. `SyncResult` fields change from `pushed`/`pulled` to `exported`/`imported`.

```kotlin
// domain/src/main/kotlin/app/tsosu/domain/repository/SyncRepository.kt
package app.tsosu.domain.repository

import kotlinx.coroutines.flow.Flow

data class SyncResult(val exported: Int, val imported: Int)

enum class SyncState { IDLE, SYNCING, ERROR }

interface SyncRepository {
    fun syncState(): Flow<SyncState>
    fun isConfigured(): Flow<Boolean>
    suspend fun sync(): Result<SyncResult>
    suspend fun disconnect()
}
```

- [ ] **Step 2: Add `getAllTasks()` query to TaskDao**

The existing TaskDao has no "get all tasks" query. Add one:

```kotlin
// In data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt, add:
@Query("SELECT * FROM tasks ORDER BY position")
fun getAllTasks(): Flow<List<TaskEntity>>
```

- [ ] **Step 3: Add `getAllCompletionsForHabit()` to HabitDao**

The existing `getCompletionsForHabit()` requires `startDate`/`endDate` params. Add a no-date-range overload for full export:

```kotlin
// In data-local/src/main/kotlin/app/tsosu/data/local/dao/HabitDao.kt, add:
@Query("SELECT * FROM habit_completions WHERE habitId = :habitId ORDER BY date DESC")
fun getAllCompletionsForHabit(habitId: String): Flow<List<HabitCompletionEntity>>
```

- [ ] **Step 4: Write MarkdownSyncRepository**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncRepository.kt
package app.tsosu.data.markdown

import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.mapper.toDomain
import app.tsosu.data.local.mapper.toEntity
import app.tsosu.domain.model.HabitCompletion
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncResult
import app.tsosu.domain.repository.SyncState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first

class MarkdownSyncRepository(
    private val preferences: MarkdownPreferences,
    private val syncManager: MarkdownSyncManager,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val projectDao: ProjectDao,
) : SyncRepository {

    private val _syncState = MutableStateFlow(SyncState.IDLE)

    override fun syncState(): Flow<SyncState> = _syncState

    override fun isConfigured(): Flow<Boolean> = preferences.isConfigured()

    override suspend fun sync(): Result<SyncResult> = runCatching {
        _syncState.value = SyncState.SYNCING

        // 1. Export current Room state to markdown
        val tasks = taskDao.getAllTasks().first().map { it.toDomain() }
        val projects = projectDao.getAll().first()
        val projectNames = projects.associate { it.id to it.title }
        syncManager.exportTasks(tasks, projectNames)

        val habits = habitDao.getActiveHabits().first().map { it.toDomain() }
        val completions = mutableListOf<HabitCompletion>()
        for (habit in habits) {
            val hc = habitDao.getAllCompletionsForHabit(habit.id).first()
            completions.addAll(hc.map { it.toDomain() })
        }
        syncManager.exportHabits(habits, completions)

        // 2. Import from markdown (picks up external edits)
        val importedTasks = syncManager.importTasks()
        val importedHabits = syncManager.importHabits()

        // 3. Merge: for now, upsert imported tasks (external edits win for conflicts)
        for (task in importedTasks.tasks) {
            taskDao.upsert(task.toEntity())
        }

        preferences.setLastSync(System.currentTimeMillis())
        _syncState.value = SyncState.IDLE

        SyncResult(
            exported = tasks.size + habits.size,
            imported = importedTasks.tasks.size + importedHabits.habits.size,
        )
    }.onFailure {
        _syncState.value = SyncState.ERROR
    }

    override suspend fun disconnect() {
        preferences.clear()
        _syncState.value = SyncState.IDLE
    }
}
```

- [ ] **Step 5: Verify domain and data-local modules compile**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew :domain:build :data-local:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add domain/src/main/kotlin/app/tsosu/domain/repository/SyncRepository.kt data-local/src/main/kotlin/app/tsosu/data/local/dao/TaskDao.kt data-local/src/main/kotlin/app/tsosu/data/local/dao/HabitDao.kt data-markdown/src/main/kotlin/app/tsosu/data/markdown/MarkdownSyncRepository.kt
git commit -m "feat(markdown): simplify SyncRepository, add DAO queries, add MarkdownSyncRepository"
```

---

### Task 9: DI module and wiring

**Files:**
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/di/MarkdownModule.kt`
- Create: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/NoOpImportRepository.kt`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/app/tsosu/di/RepositoryModule.kt`

- [ ] **Step 1: Write NoOpImportRepository**

The `ImportRepository` interface and `TodoistImporter` lived in `data-vikunja`. Since `data-vikunja` is being removed, provide a no-op implementation. Todoist import can be re-added later if needed.

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/NoOpImportRepository.kt
package app.tsosu.data.markdown

import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.ImportResult

class NoOpImportRepository : ImportRepository {
    override suspend fun importFromTodoist(data: ByteArray, format: ImportFormat): Result<ImportResult> =
        Result.success(ImportResult(tasksImported = 0, projectsImported = 0, labelsImported = 0))
}
```

- [ ] **Step 2: Write MarkdownModule**

```kotlin
// data-markdown/src/main/kotlin/app/tsosu/data/markdown/di/MarkdownModule.kt
package app.tsosu.data.markdown.di

import android.content.Context
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.ProjectDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.markdown.MarkdownFileAccess
import app.tsosu.data.markdown.MarkdownHabitParser
import app.tsosu.data.markdown.MarkdownHabitSerializer
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.data.markdown.MarkdownSyncManager
import app.tsosu.data.markdown.MarkdownSyncRepository
import app.tsosu.data.markdown.MarkdownTaskParser
import app.tsosu.data.markdown.MarkdownTaskSerializer
import app.tsosu.data.markdown.NoOpImportRepository
import app.tsosu.data.markdown.SafMarkdownFileAccess
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.SyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MarkdownModule {

    @Provides
    @Singleton
    fun provideMarkdownPreferences(@ApplicationContext context: Context): MarkdownPreferences =
        MarkdownPreferences(context)

    @Provides
    @Singleton
    fun provideMarkdownFileAccess(
        @ApplicationContext context: Context,
        preferences: MarkdownPreferences,
    ): MarkdownFileAccess = SafMarkdownFileAccess(context) { preferences.getFolderUri() }

    @Provides
    @Singleton
    fun provideMarkdownSyncManager(
        fileAccess: MarkdownFileAccess,
    ): MarkdownSyncManager = MarkdownSyncManager(
        fileAccess = fileAccess,
        taskSerializer = MarkdownTaskSerializer(),
        taskParser = MarkdownTaskParser(),
        habitSerializer = MarkdownHabitSerializer(),
        habitParser = MarkdownHabitParser(),
    )

    @Provides
    @Singleton
    fun provideSyncRepository(
        preferences: MarkdownPreferences,
        syncManager: MarkdownSyncManager,
        taskDao: TaskDao,
        habitDao: HabitDao,
        projectDao: ProjectDao,
    ): SyncRepository = MarkdownSyncRepository(
        preferences = preferences,
        syncManager = syncManager,
        taskDao = taskDao,
        habitDao = habitDao,
        projectDao = projectDao,
    )

    @Provides
    @Singleton
    fun provideImportRepository(): ImportRepository = NoOpImportRepository()
}
```

- [ ] **Step 2: Update app/build.gradle.kts**

Replace `implementation(project(":data-vikunja"))` with `implementation(project(":data-markdown"))`.
Remove `implementation(libs.hilt.work)`, `implementation(libs.work.runtime.ktx)`.

- [ ] **Step 3: Simplify RepositoryModule**

Remove SyncDispatcher wiring from TaskRepository:

```kotlin
// app/src/main/java/app/tsosu/di/RepositoryModule.kt
package app.tsosu.di

import app.tsosu.data.local.dao.FocusDao
import app.tsosu.data.local.dao.HabitDao
import app.tsosu.data.local.dao.RoutineDao
import app.tsosu.data.local.dao.TaskDao
import app.tsosu.data.local.repository.FocusRepositoryImpl
import app.tsosu.data.local.repository.HabitRepositoryImpl
import app.tsosu.data.local.repository.RoutineRepositoryImpl
import app.tsosu.data.local.repository.TaskRepositoryImpl
import app.tsosu.domain.repository.FocusRepository
import app.tsosu.domain.repository.HabitRepository
import app.tsosu.domain.repository.RoutineRepository
import app.tsosu.domain.repository.TaskRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(taskDao: TaskDao): TaskRepository =
        TaskRepositoryImpl(taskDao)

    @Provides
    @Singleton
    fun provideHabitRepository(habitDao: HabitDao): HabitRepository =
        HabitRepositoryImpl(habitDao)

    @Provides
    @Singleton
    fun provideRoutineRepository(routineDao: RoutineDao, habitDao: HabitDao): RoutineRepository =
        RoutineRepositoryImpl(routineDao, habitDao)

    @Provides
    @Singleton
    fun provideFocusRepository(
        focusDao: FocusDao,
        taskDao: TaskDao,
        habitDao: HabitDao,
    ): FocusRepository = FocusRepositoryImpl(focusDao, taskDao, habitDao)
}
```

- [ ] **Step 4: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (may fail — defer fixing to task 10)

- [ ] **Step 5: Commit**

```bash
git add data-markdown/src/main/kotlin/app/tsosu/data/markdown/di/ data-markdown/src/main/kotlin/app/tsosu/data/markdown/NoOpImportRepository.kt app/build.gradle.kts app/src/main/java/app/tsosu/di/RepositoryModule.kt
git commit -m "feat(markdown): wire DI module, replace data-vikunja with data-markdown"
```

---

### Task 10: Clean up app layer — MainActivity, TsosuApp, Manifest

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`
- Modify: `app/src/main/java/app/tsosu/TsosuApp.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Simplify MainActivity**

Remove SyncWorker, resume-pull, WorkManager imports. Replace with simple markdown sync on resume:

```kotlin
// app/src/main/java/app/tsosu/MainActivity.kt
package app.tsosu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import app.tsosu.navigation.BottomNavBar
import app.tsosu.navigation.Screen
import app.tsosu.navigation.TsosuNavHost
import app.tsosu.ui.screens.pickone.PickOneSheet
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.screens.quickadd.QuickAddViewModel
import app.tsosu.ui.screens.taskdetail.TaskDetailSheet
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.theme.TsosuTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.tsosu.domain.repository.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var syncRepository: SyncRepository

    private var lastSyncTime = 0L

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupResumePull()
        setContent {
            val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
            val darkModeOption by themePreferences.darkMode.collectAsState(initial = DarkModeOption.SYSTEM)
            val darkTheme = when (darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
            }

            TsosuTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                var showAddTask by remember { mutableStateOf(false) }
                var showPickOne by remember { mutableStateOf(false) }
                var editingTaskId by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Tsosu") },
                            actions = {
                                IconButton(onClick = {
                                    navController.navigate(Screen.Settings.route)
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                        )
                    },
                    bottomBar = { BottomNavBar(navController) },
                    floatingActionButton = {
                        @OptIn(ExperimentalFoundationApi::class)
                        FloatingActionButton(
                            onClick = { showAddTask = true },
                            modifier = Modifier.combinedClickable(
                                onClick = { showAddTask = true },
                                onLongClick = { showPickOne = true },
                            ),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    },
                ) { innerPadding ->
                    TsosuNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        onTaskClick = { taskId -> editingTaskId = taskId },
                    )
                }

                if (showAddTask) {
                    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
                    ModalBottomSheet(
                        onDismissRequest = { showAddTask = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        QuickAddTaskSheet(
                            onDismiss = { showAddTask = false },
                            onAdd = { title, priority, energy, minutes, dueDate ->
                                quickAddViewModel.createTask(title, priority, energy, minutes, dueDate)
                                showAddTask = false
                            },
                        )
                    }
                }

                editingTaskId?.let { taskId ->
                    ModalBottomSheet(
                        onDismissRequest = { editingTaskId = null },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        TaskDetailSheet(
                            taskId = taskId,
                            onDismiss = { editingTaskId = null },
                        )
                    }
                }

                if (showPickOne) {
                    ModalBottomSheet(
                        onDismissRequest = { showPickOne = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        PickOneSheet(onDismiss = { showPickOne = false })
                    }
                }
            }
        }
    }

    private fun setupResumePull() {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = System.currentTimeMillis()
                if (now - lastSyncTime > 30_000) {
                    lastSyncTime = now
                    CoroutineScope(Dispatchers.IO).launch {
                        val configured = syncRepository.isConfigured().first()
                        if (configured) {
                            syncRepository.sync()
                        }
                    }
                }
            }
        })
    }
}
```

- [ ] **Step 2: Simplify TsosuApp**

Remove WorkManager/HiltWorkerFactory:

```kotlin
// app/src/main/java/app/tsosu/TsosuApp.kt
package app.tsosu

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TsosuApp : Application()
```

- [ ] **Step 3: Clean up AndroidManifest.xml**

Remove WorkManagerInitializer provider and INTERNET permission:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".TsosuApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.Tsosu">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Tsosu">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <receiver
            android:name=".ui.widget.FocusWidgetReceiver"
            android:exported="true">
            <intent-filter>
                <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
            </intent-filter>
            <meta-data
                android:name="android.appwidget.provider"
                android:resource="@xml/focus_widget_info" />
        </receiver>
    </application>

</manifest>
```

- [ ] **Step 4: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/MainActivity.kt app/src/main/java/app/tsosu/TsosuApp.kt app/src/main/AndroidManifest.xml
git commit -m "refactor: remove WorkManager/Vikunja sync, simplify to markdown sync-on-resume"
```

---

### Task 11: Update Settings screen — folder picker instead of Vikunja auth

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt`

- [ ] **Step 1: Update SettingsViewModel**

Replace Vikunja auth with markdown folder selection:

```kotlin
// app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt
package app.tsosu.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.domain.repository.CalendarProvider
import app.tsosu.domain.repository.CalendarRepository
import app.tsosu.domain.repository.ImportFormat
import app.tsosu.domain.repository.ImportRepository
import app.tsosu.domain.repository.SyncRepository
import app.tsosu.domain.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val folderUri: String? = null,
    val isConfigured: Boolean = false,
    val syncState: SyncState = SyncState.IDLE,
    val calendarProvider: CalendarProvider = CalendarProvider.NONE,
    val caldavUrl: String = "",
    val caldavEmail: String = "",
    val caldavPassword: String = "",
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val calendarRepository: CalendarRepository,
    private val importRepository: ImportRepository,
    private val markdownPreferences: MarkdownPreferences,
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    val dynamicColor: StateFlow<Boolean> = themePreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkMode: StateFlow<DarkModeOption> = themePreferences.darkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DarkModeOption.SYSTEM)

    init {
        viewModelScope.launch {
            syncRepository.isConfigured().collect { configured ->
                _uiState.value = _uiState.value.copy(isConfigured = configured)
            }
        }
        viewModelScope.launch {
            syncRepository.syncState().collect { state ->
                _uiState.value = _uiState.value.copy(syncState = state)
            }
        }
        viewModelScope.launch {
            markdownPreferences.folderUri().collect { uri ->
                _uiState.value = _uiState.value.copy(folderUri = uri?.toString())
            }
        }
        viewModelScope.launch {
            calendarRepository.activeProvider().collect { provider ->
                _uiState.value = _uiState.value.copy(calendarProvider = provider)
            }
        }
    }

    fun selectFolder(uri: Uri) {
        viewModelScope.launch {
            markdownPreferences.setFolderUri(uri)
            _uiState.value = _uiState.value.copy(
                isConfigured = true,
                message = "Markdown folder selected",
            )
            sync()
        }
    }

    fun sync() {
        viewModelScope.launch {
            val result = syncRepository.sync()
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = "Synced: ${r.exported} exported, ${r.imported} imported",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Sync error: ${e.message}",
                    )
                },
            )
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                isConfigured = false,
                folderUri = null,
                message = null,
            )
        }
    }

    fun importTodoist(data: ByteArray) {
        viewModelScope.launch {
            val result = importRepository.importFromTodoist(data, ImportFormat.TODOIST_CSV)
            result.fold(
                onSuccess = { r ->
                    _uiState.value = _uiState.value.copy(
                        message = "Imported ${r.tasksImported} tasks from Todoist",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Import error: ${e.message}",
                    )
                },
            )
        }
    }

    fun updateCaldavUrl(url: String) {
        _uiState.value = _uiState.value.copy(caldavUrl = url)
    }

    fun updateCaldavEmail(email: String) {
        _uiState.value = _uiState.value.copy(caldavEmail = email)
    }

    fun updateCaldavPassword(password: String) {
        _uiState.value = _uiState.value.copy(caldavPassword = password)
    }

    fun connectGoogle(accessToken: String, refreshToken: String?, email: String) {
        viewModelScope.launch {
            val result = calendarRepository.configureGoogle(accessToken, refreshToken, email)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        calendarProvider = CalendarProvider.GOOGLE,
                        message = "Google Calendar connected",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "Google Calendar error: ${e.message}",
                    )
                },
            )
        }
    }

    fun connectCaldav() {
        viewModelScope.launch {
            val state = _uiState.value
            val result = calendarRepository.configureCaldav(
                state.caldavUrl, state.caldavEmail, state.caldavPassword,
            )
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        calendarProvider = CalendarProvider.CALDAV,
                        message = "CalDAV connected",
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        message = "CalDAV error: ${e.message}",
                    )
                },
            )
        }
    }

    fun disconnectCalendar() {
        viewModelScope.launch {
            calendarRepository.disconnect()
            _uiState.value = _uiState.value.copy(
                calendarProvider = CalendarProvider.NONE,
                caldavUrl = "",
                caldavEmail = "",
                caldavPassword = "",
                message = "Calendar disconnected",
            )
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setDynamicColor(enabled) }
    }

    fun setDarkMode(option: DarkModeOption) {
        viewModelScope.launch { themePreferences.setDarkMode(option) }
    }
}
```

- [ ] **Step 2: Update SettingsScreen**

Replace Vikunja server section with folder picker:

```kotlin
// In SettingsScreen.kt, replace the "Vikunja Server Section" with:

        // Markdown Sync Section
        Text("Markdown Sync", style = MaterialTheme.typography.titleMedium)

        val folderPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree()
        ) { uri: Uri? ->
            uri ?: return@rememberLauncherForActivityResult
            // Take persistable permission so we can access the folder across restarts
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            viewModel.selectFolder(uri)
        }

        if (state.isConfigured) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Syncing to folder",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    state.folderUri?.let {
                        Text(
                            Uri.parse(it).lastPathSegment ?: it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.sync() },
                            enabled = state.syncState != SyncState.SYNCING,
                        ) {
                            Text(
                                if (state.syncState == SyncState.SYNCING) "Syncing..."
                                else "Sync Now"
                            )
                        }
                        OutlinedButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect")
                        }
                    }
                }
            }
        } else {
            Text(
                "Select a folder to sync tasks and habits as markdown files. " +
                    "Use a Syncthing or Obsidian Sync folder for cross-device sync.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select Folder")
            }
        }
```

Also update imports — remove `R.string.settings_vikunja_server` etc. references, add `ActivityResultContracts.OpenDocumentTree`.

- [ ] **Step 3: Add missing string resources or remove unused ones**

Remove Vikunja-specific string references from `res/values/strings.xml` if they cause build errors. Keep `settings_title`.

- [ ] **Step 4: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/settings/
git commit -m "feat(settings): replace Vikunja auth with markdown folder picker"
```

---

### Task 12: Remove data-vikunja module and clean up data-local

**Files:**
- Delete: `data-vikunja/` (entire directory)
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt` (remove SyncQueueEntity)
- Delete: `data-local/src/main/kotlin/app/tsosu/data/local/entity/SyncQueueEntity.kt`
- Delete: `data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt`
- Modify: `app/src/main/java/app/tsosu/di/DatabaseModule.kt` (remove SyncQueueDao provider)

- [ ] **Step 1: Delete data-vikunja module**

```bash
rm -rf data-vikunja/
```

- [ ] **Step 2: Remove SyncQueueEntity and SyncQueueDao**

```bash
rm data-local/src/main/kotlin/app/tsosu/data/local/entity/SyncQueueEntity.kt
rm data-local/src/main/kotlin/app/tsosu/data/local/dao/SyncQueueDao.kt
```

- [ ] **Step 3: Update TsosuDatabase**

Remove `SyncQueueEntity` from the `@Database` entities array and `SyncQueueDao` abstract function. Bump version to 2.

- [ ] **Step 4: Update DatabaseModule**

Remove `provideSyncQueueDao()` function.

- [ ] **Step 5: Add Room migration (v1 -> v2) to drop sync_queue table**

`NoOpImportRepository` was already created in Task 9.

```kotlin
// In TsosuDatabase.kt or a new Migrations.kt file:
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS sync_queue")
    }
}
```

Wire migration in DatabaseModule (note: database name is `"tsosu.db"` — must match existing):

```kotlin
Room.databaseBuilder(context, TsosuDatabase::class.java, "tsosu.db")
    .addMigrations(MIGRATION_1_2)
    .build()
```

- [ ] **Step 7: Verify build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test`
Expected: ALL PASS (some data-vikunja tests gone, that's expected)

- [ ] **Step 9: Commit**

```bash
git add -A
git commit -m "refactor: remove data-vikunja module, drop sync_queue, clean up DI"
```

---

### Task 13: Run full build + tests, fix any remaining issues

- [ ] **Step 1: Full clean build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all unit tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test`
Expected: ALL PASS

- [ ] **Step 3: Fix any compilation errors**

Iterate until green. Common issues:
- Missing imports from deleted Vikunja types
- String resource references to Vikunja-specific strings
- `TaskRepositoryImpl` still referencing `onTaskChanged` callback (should be no-op now)
- Any `data-vikunja` test references in other modules

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: fix remaining compilation issues after markdown pivot"
```

---

## Summary of net changes

| Metric | Before | After |
|--------|--------|-------|
| Modules | 5 (domain, data-local, data-vikunja, data-calendar, app) | 5 (domain, data-local, **data-markdown**, data-calendar, app) |
| Sync complexity | Bidirectional REST API + queue + retry + WorkManager | Read/write 2 markdown files |
| External deps | Retrofit, OkHttp, kotlinx-serialization, WorkManager | DocumentFile (1 dep) |
| Server requirement | Vikunja instance running | None (file sync via Syncthing) |
| Desktop interop | None | Obsidian Tasks / nvim native |
| Files (data layer) | ~25 files in data-vikunja | ~12 files in data-markdown |
