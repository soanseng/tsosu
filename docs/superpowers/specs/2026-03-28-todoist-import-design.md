# Todoist CSV Import & Natural Language Recurrence Parser

**Date:** 2026-03-28
**Status:** Approved

## Summary

Add a Todoist CSV import feature to tsosu, allowing users to migrate their tasks from Todoist. Additionally, build a natural language recurrence parser that powers both the import and in-app Quick Add input.

## User Flow

1. User exports a project from Todoist web: Project → ⋮ → Export as template → downloads `.csv`
2. Opens tsosu → Settings → "Import from Todoist"
3. SAF file picker opens (supports selecting multiple `.csv` files)
4. For each file, a dialog appears:
   - Shows "Import Work.csv (12 tasks)"
   - Options: "Add to Inbox" / "Create new project" / dropdown to select existing project
5. Import completes → summary snackbar: "Imported 35 tasks"

## Todoist CSV Format

```csv
TYPE,CONTENT,DESCRIPTION,PRIORITY,INDENT,AUTHOR,RESPONSIBLE,DATE,DATE_LANG,TIMEZONE
task,Buy groceries,Don't forget milk,4,1,,,2025-03-28,en,America/New_York
task,Bread,,4,2,,,,,
task,Eggs,,4,2,,,,,
task,Weekly review,,2,1,,,every Friday,en,
```

## Field Mapping

| Todoist CSV | tsosu Task Field | Notes |
|-------------|-----------------|-------|
| CONTENT | title | Direct map |
| DESCRIPTION | description | Direct map, empty string if absent |
| PRIORITY 1 | URGENT | Todoist inverts: 1 = most urgent |
| PRIORITY 2 | HIGH | |
| PRIORITY 3 | MEDIUM | |
| PRIORITY 4 | NONE | Default / no priority |
| INDENT 1 | Top-level task | |
| INDENT 2+ | subtask | Parent = nearest preceding indent-1 task |
| DATE (concrete) | dueDate | Parse as LocalDateTime |
| DATE (recurrence) | recurrenceRule + dueDate | Parse via RecurrenceParser; dueDate = today if no concrete date embedded |
| DATE (empty) | null | No due date |
| AUTHOR | ignored | Not relevant for personal use |
| RESPONSIBLE | ignored | Not relevant for personal use |
| TYPE = "section" | Section separator | Used to group tasks but not mapped to a field |
| TIMEZONE | ignored | tsosu uses device timezone |

## Priority Mapping

| Todoist CSV Value | Todoist UI Label | tsosu Priority |
|-------------------|-----------------|----------------|
| 1 | Priority 1 (urgent) | URGENT |
| 2 | Priority 2 | HIGH |
| 3 | Priority 3 | MEDIUM |
| 4 | No priority | NONE |

## Subtask Handling

Todoist CSV uses an INDENT column:
- `1` = top-level task
- `2` = child of the nearest preceding indent-1 task
- `3` = child of the nearest preceding indent-2 task (nested subtask)

tsosu supports `subtasks: List<Task>` on the domain model. The parser builds the tree by maintaining a stack of parent tasks indexed by indent level.

## RecurrenceParser — Natural Language → RRULE

A pure-Kotlin regex-based parser in `data-markdown`. No third-party NLP dependencies.

### Supported English Patterns

| Input | Output RRULE |
|-------|-------------|
| `every day` | `FREQ=DAILY` |
| `every weekday` | `FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR` |
| `every Monday` | `FREQ=WEEKLY;BYDAY=MO` |
| `every Mon, Wed, Fri` | `FREQ=WEEKLY;BYDAY=MO,WE,FR` |
| `every 2 weeks` | `FREQ=WEEKLY;INTERVAL=2` |
| `every 3 days` | `FREQ=DAILY;INTERVAL=3` |
| `every month` | `FREQ=MONTHLY` |
| `every year` | `FREQ=YEARLY` |
| `every month on the 15th` | `FREQ=MONTHLY;BYMONTHDAY=15` |

### Supported Chinese Patterns

| Input | Output RRULE |
|-------|-------------|
| `每天` | `FREQ=DAILY` |
| `每週一` | `FREQ=WEEKLY;BYDAY=MO` |
| `每週一三五` | `FREQ=WEEKLY;BYDAY=MO,WE,FR` |
| `每兩週` | `FREQ=WEEKLY;INTERVAL=2` |
| `每月` | `FREQ=MONTHLY` |
| `每月15號` | `FREQ=MONTHLY;BYMONTHDAY=15` |
| `每年` | `FREQ=YEARLY` |
| `每個工作日` | `FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR` |

### API

```kotlin
class RecurrenceParser {
    fun parse(input: String): RecurrenceResult
}

sealed class RecurrenceResult {
    data class Success(val rrule: String) : RecurrenceResult()
    data class Unrecognized(val original: String) : RecurrenceResult()
}
```

- On import: `Unrecognized` → original text appended to task description
- In Quick Add: `Unrecognized` → no recurrence set, user can configure manually

### Quick Add Integration

When user types in Quick Add, the parser detects trailing recurrence syntax:
- Input: `買菜 every Monday` or `買菜 每週一`
- Splits into: title = `買菜`, recurrence = `FREQ=WEEKLY;BYDAY=MO`
- Shows a chip below the input: `🔁 每週一` — user can tap to remove

Detection triggers on known prefixes: `every`, `每`.

## Architecture

```
app/
  settings/          ImportSettingsItem (UI entry point)
  import/            TodoistImportViewModel, TodoistImportScreen (SAF picker, project dialog)
  quickadd/          Updated QuickAddTaskSheet (recurrence chip)

domain/
  usecase/           ImportTasksUseCase (receives parsed tasks, delegates to TaskRepository)

data-markdown/
  todoist/           TodoistCsvParser (CSV text → List<Task>)
  recurrence/        RecurrenceParser (natural language → RRULE)
```

### TodoistCsvParser

```kotlin
class TodoistCsvParser(
    private val recurrenceParser: RecurrenceParser
) {
    fun parse(csvContent: String): TodoistImportResult
}

data class TodoistImportResult(
    val tasks: List<Task>,
    val warnings: List<String>  // e.g., "Unrecognized recurrence: every other Tuesday"
)
```

- Handles CSV quoting (fields with commas/newlines in quotes)
- Skips header row
- Skips rows where TYPE != "task"
- Builds subtask tree from INDENT column
- Generates UUIDs for all tasks

### ImportTasksUseCase

```kotlin
class ImportTasksUseCase(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository
) {
    suspend fun execute(
        tasks: List<Task>,
        targetProject: ImportTarget
    ): ImportResult
}

sealed class ImportTarget {
    object Inbox : ImportTarget()
    data class ExistingProject(val projectId: String) : ImportTarget()
    data class NewProject(val name: String) : ImportTarget()
}

data class ImportResult(
    val importedCount: Int,
    val warnings: List<String>
)
```

## Localization

All user-facing strings use string resources for EN and zh-TW:
- "Import from Todoist"
- "Add to Inbox"
- "Create new project"
- "Imported %d tasks"
- "Import failed: %s"
- Recurrence chip labels

## Error Handling

- Invalid CSV format → show error dialog with message
- Empty file → show "No tasks found" message
- Partial parse failure → import successful tasks, report warnings in result
- File read failure (SAF permission) → show error snackbar

## Out of Scope

- Todoist API/token-based import
- Comments/notes import
- Completed tasks import (CSV only contains active tasks)
- Attachment import
- Label import (CSV does not include labels)
- Multi-level project hierarchy
