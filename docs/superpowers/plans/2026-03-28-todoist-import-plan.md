# Todoist Import — Implementation Plan

**Spec:** `docs/superpowers/specs/2026-03-28-todoist-import-design.md`
**Date:** 2026-03-28

## Phase 1: RecurrenceParser (TDD, pure Kotlin)

**Module:** `data-markdown`
**Files:**
- NEW: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/recurrence/RecurrenceParser.kt`
- NEW: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/recurrence/RecurrenceParserTest.kt`

**Steps:**
1. Write tests first covering all EN + zh-TW patterns from spec
2. Implement RecurrenceParser to pass all tests
3. Verify: `./gradlew :data-markdown:test`

## Phase 2: TodoistCsvParser (TDD, pure Kotlin)

**Module:** `data-markdown`
**Files:**
- NEW: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/todoist/TodoistCsvParser.kt`
- NEW: `data-markdown/src/test/kotlin/app/tsosu/data/markdown/todoist/TodoistCsvParserTest.kt`

**Steps:**
1. Write tests covering: basic parsing, priority mapping, subtask tree, recurrence dates, quoted fields, empty fields, section rows
2. Implement TodoistCsvParser with RecurrenceParser dependency
3. Verify: `./gradlew :data-markdown:test`

## Phase 3: Replace NoOpImportRepository

**Module:** `data-markdown`
**Files:**
- MODIFY: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/NoOpImportRepository.kt` → rename to `TodoistImportRepository.kt`
- MODIFY: `data-markdown/src/main/kotlin/app/tsosu/data/markdown/di/MarkdownModule.kt` (wire real impl)

**Steps:**
1. Implement TodoistImportRepository using TodoistCsvParser + TaskDao + ProjectDao
2. Update MarkdownModule DI to provide real implementation
3. Verify build compiles

## Phase 4: Settings UI — Project Selection Dialog

**Module:** `app`
**Files:**
- MODIFY: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsScreen.kt` (add project dialog)
- MODIFY: `app/src/main/java/app/tsosu/ui/screens/settings/SettingsViewModel.kt` (update import logic)

**Steps:**
1. Add project selection dialog (Inbox / New project / Existing project)
2. Update ViewModel to pass project target to import
3. Add multi-file support to SAF picker
4. Add result snackbar with count

## Phase 5: Quick Add — Natural Language Recurrence

**Module:** `app`
**Files:**
- MODIFY: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt`

**Steps:**
1. Add RecurrenceParser integration to detect trailing recurrence syntax
2. Show parsed recurrence as chip below input
3. Allow user to dismiss chip to remove recurrence

## Phase 6: String Resources

**Files:**
- MODIFY: `app/src/main/res/values/strings.xml`
- MODIFY: `app/src/main/res/values-zh-rTW/strings.xml`

**Steps:**
1. Add all new strings for import dialog, recurrence chips, error messages

## Phase 7: Build & Final Verification

1. `./gradlew assembleDebug` — full build
2. `./gradlew test` — all tests pass
3. Code review
4. Commit
