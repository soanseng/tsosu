# Tsosu Implementation Plan: Phase 0 + Phase 1

---

## Phase 0: Pre-flight Checks (1 day)

### Task 0.1: Verify Vikunja API

**Goal:** Confirm Vikunja instance is reachable and get OpenAPI spec.

### Steps:
1. Find Vikunja server URL from existing infra (Dockge)
2. `curl <vikunja-url>/api/v1/info` — verify API responds
3. `curl <vikunja-url>/api/v1/docs.json -o vikunja-openapi.json` — download OpenAPI spec
4. Verify key fields exist in the spec: `repeatAfter`, `repeatMode`, `labels`, `hexColor`, `description`

### Verification:
- API info endpoint returns valid JSON
- OpenAPI spec saved locally

---

### Task 0.2: Verify Fastmail CalDAV

**Goal:** Confirm CalDAV connectivity to Fastmail.

### Steps:
1. Test PROPFIND against `caldav.fastmail.com`
2. Confirm principal URL resolves
3. Document CalDAV base URL for later use

### Verification:
- CalDAV PROPFIND returns XML with calendar home

---

### Task 0.3: Research Vikunja Task Model

**Goal:** Study Vikunja task model fields relevant to Tsosu sync.

### Steps:
1. Examine OpenAPI spec for task endpoints
2. Document field mappings: repeatAfter, repeatMode, labels, hexColor, description
3. Verify repeating task behavior (create task with repeatAfter, mark done, confirm next occurrence)
4. Verify label CRUD endpoints
5. Verify project CRUD endpoints (for routines)

### Verification:
- Field mapping notes documented
- Repeating task lifecycle confirmed via API calls

---

## Phase 1: Core MVP (3-4 weeks)

> Tasks + Habits + ADHD (local-first, no sync yet)

## Task 1: Project Scaffolding

**Goal:** Create Android project with multi-module structure.

### Steps:
1. Create root project with `settings.gradle.kts` and `build.gradle.kts`
2. Create modules: `app`, `domain`, `data-local`
3. Configure Kotlin 2.0, Compose, Hilt, Room, JUnit 5, MockK, Turbine
4. Configure `app/src/main/AndroidManifest.xml` with `app.tsosu` package
5. Create `TsosuApp.kt` (Hilt Application class)
6. Create `MainActivity.kt` with empty Compose scaffold

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 2: Domain Models

**Goal:** Create all domain models in `domain/` module (pure Kotlin, zero Android deps).

### Steps:
1. Create `domain/src/main/kotlin/app/tsosu/domain/model/` directory
2. Implement models from claude.md:
   - `Task.kt` (with estimatedMinutes, energyLevel, isFocus)
   - `Priority.kt` enum
   - `EnergyLevel.kt` enum (with labelTitle for Vikunja sync)
   - `Habit.kt` (with serverId, tinyVersion, frequency)
   - `HabitFrequency.kt` enum (with repeatAfterSeconds)
   - `HabitCompletion.kt`
   - `HabitStreakInfo.kt`
   - `Routine.kt` (with serverId)
   - `RoutineTime.kt` enum
   - `DailyFocus.kt`
   - `WeeklyReview.kt`
   - `Project.kt` (with isRoutine flag)
   - `Label.kt`
3. Add kotlinx-datetime dependency for `LocalDate`, `LocalDateTime`, `Instant`

### Verification:
```bash
./gradlew :domain:build
```

---

## Task 3: Domain Repository Interfaces

**Goal:** Define repository interfaces in `domain/` module.

### Steps:
1. Create `domain/src/main/kotlin/app/tsosu/domain/repository/`
2. Implement interfaces from claude.md:
   - `TaskRepository.kt`
   - `HabitRepository.kt`
   - `RoutineRepository.kt`
   - `FocusRepository.kt`
   - `CalendarRepository.kt` (stub for Phase 2)
   - `SyncRepository.kt` (stub for Phase 2)
   - `ImportRepository.kt` (stub for Phase 2)

### Verification:
```bash
./gradlew :domain:build
```

---

## Task 4: Domain Use Cases + TDD

**Goal:** Implement use cases with tests first (TDD).

### Steps:
1. Create `domain/src/main/kotlin/app/tsosu/domain/usecase/`
2. Create `domain/src/test/kotlin/app/tsosu/domain/usecase/`
3. For each use case, write test FIRST, then implement:

   **Task use cases:**
   - `CreateTaskUseCase` + test
   - `ToggleTaskDoneUseCase` + test
   - `GetTodayOverviewUseCase` + test (returns tasks + time total + focus count)
   - `PickOneTaskUseCase` + test (random by energy level)
   - `SetDailyFocusUseCase` + test (max 3 tasks)
   - `GetStaleTaskIdsUseCase` + test (14+ days untouched)
   - `GetWeeklyReviewUseCase` + test

   **Habit use cases:**
   - `CreateHabitUseCase` + test (sets repeatAfterSeconds, validates tinyVersion prompt)
   - `CompleteHabitUseCase` + test (records HabitCompletion)
   - `GetTodayHabitsUseCase` + test (active habits with today's completion status)

   **Routine use cases:**
   - `GetRoutineUseCase` + test (routine with its habits)

### Verification:
```bash
./gradlew :domain:test
```

---

## Task 5: Room Database (data-local)

**Goal:** Implement Room entities, DAOs, and database.

### Steps:
1. Create `data-local/src/main/kotlin/app/tsosu/data/local/`
2. Implement Room entities from claude.md:
   - `TaskEntity.kt`
   - `HabitEntity.kt`
   - `HabitCompletionEntity.kt`
   - `RoutineEntity.kt`
   - `DailyFocusEntity.kt`
   - `ProjectEntity.kt`
   - `LabelEntity.kt`
   - `SyncQueueEntity.kt`
3. Implement DAOs:
   - `TaskDao.kt`
   - `HabitDao.kt`
   - `RoutineDao.kt`
   - `FocusDao.kt`
   - `ProjectDao.kt`
   - `LabelDao.kt`
4. Create `TsosuDatabase.kt` (Room database class)
5. Create entity-domain mappers (`EntityMapper.kt`)

### Verification:
```bash
./gradlew :data-local:build
```

---

## Task 6: Room DAO Tests (TDD)

**Goal:** Write DAO tests using Robolectric.

### Steps:
1. Create `data-local/src/test/kotlin/app/tsosu/data/local/dao/`
2. Write tests for each DAO:
   - `TaskDaoTest.kt` — CRUD, query by project, query by energy, stale tasks, focus tasks
   - `HabitDaoTest.kt` — CRUD, completions, streak queries
   - `RoutineDaoTest.kt` — CRUD, habits-in-routine
   - `FocusDaoTest.kt` — daily focus CRUD

### Verification:
```bash
./gradlew :data-local:test
```

---

## Task 7: Repository Implementations

**Goal:** Implement repositories connecting Room to domain interfaces.

### Steps:
1. Create `data-local/src/main/kotlin/app/tsosu/data/local/repository/`
2. Implement:
   - `TaskRepositoryImpl.kt`
   - `HabitRepositoryImpl.kt`
   - `RoutineRepositoryImpl.kt`
   - `FocusRepositoryImpl.kt`
3. Create repository tests:
   - `TaskRepositoryImplTest.kt`
   - `HabitRepositoryImplTest.kt`
   - `RoutineRepositoryImplTest.kt`
   - `FocusRepositoryImplTest.kt`

### Verification:
```bash
./gradlew :data-local:test
```

---

## Task 8: Hilt DI Setup

**Goal:** Wire up dependency injection.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/di/`
2. Create modules:
   - `DatabaseModule.kt` — provides TsosuDatabase and DAOs
   - `RepositoryModule.kt` — binds repository impls to interfaces
   - `UseCaseModule.kt` — provides use cases

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 9: Navigation + Bottom Nav

**Goal:** Set up Compose navigation with bottom nav bar.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/navigation/`
   - `TsosuNavHost.kt`
   - `Screen.kt` (sealed class for routes)
   - `BottomNavBar.kt`
2. Bottom nav items: Inbox, Focus, Habits, Upcoming, Pick One
3. Create placeholder screens for each tab

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 10: Focus 3 + Today View UI

**Goal:** Build the main Focus 3 screen.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/ui/screens/focus/`
   - `FocusScreen.kt`
   - `FocusViewModel.kt`
2. UI elements:
   - Focus 3 card at top (pick 3 tasks)
   - Habits summary: "3/5 done"
   - Other tasks collapsed below
   - Time total display
3. ViewModel connects to use cases via Hilt

### Verification:
```bash
./gradlew assembleDebug
./gradlew :app:testDebugUnitTest
```

---

## Task 11: Habits Tab + Routines UI

**Goal:** Build habits and routines screens.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/ui/screens/habits/`
   - `HabitsScreen.kt`
   - `HabitsViewModel.kt`
2. UI elements:
   - Routines grouped: Morning | Anytime | Evening
   - Tap checkbox to complete habit
   - Tiny version shown as grey subtitle
   - Flexible streak: "5/7 this week"
3. Create `app/src/main/kotlin/app/tsosu/ui/screens/habitdetail/`
   - `HabitDetailScreen.kt` — dot calendar + stats
4. Habit creation with "What's the 2-minute version?" prompt
5. Zero "streak broken" language anywhere

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 12: Quick Add + Energy/Time

**Goal:** Build quick add dialogs for tasks and habits.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/ui/screens/quickadd/`
   - `QuickAddTaskSheet.kt` — Title + Date/Priority/Energy/Time/Label/Project
   - `QuickAddHabitSheet.kt` — Title + Tiny version + Routine
2. Energy level picker component
3. Time estimate picker component

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 13: Pick One + Inbox + Search

**Goal:** Build remaining screens.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/ui/screens/pickone/`
   - `PickOneScreen.kt` — energy selector + random task + "Pick another" button
   - `PickOneViewModel.kt`
2. Create `app/src/main/kotlin/app/tsosu/ui/screens/inbox/`
   - `InboxScreen.kt` — tasks without project/date
   - `InboxViewModel.kt`
3. Create `app/src/main/kotlin/app/tsosu/ui/screens/search/`
   - `SearchScreen.kt`
4. Create `app/src/main/kotlin/app/tsosu/ui/screens/upcoming/`
   - `UpcomingScreen.kt` — next 7 days

### Verification:
```bash
./gradlew assembleDebug
```

---

## Task 14: Strings (en + zh-TW)

**Goal:** Add all UI strings in English and Traditional Chinese.

### Steps:
1. Create `app/src/main/res/values/strings.xml`
2. Create `app/src/main/res/values-zh-rTW/strings.xml`
3. All user-facing text must use string resources
4. Review for zero-shame language in both languages

### Verification:
```bash
./gradlew assembleDebug
# Manually check: no hardcoded strings in Compose files
```

---

## Task 15: Widget

**Goal:** Create a home screen widget for Focus 3.

### Steps:
1. Create `app/src/main/kotlin/app/tsosu/ui/widget/`
   - `FocusWidget.kt` — Glance widget showing Focus 3 tasks
   - `FocusWidgetReceiver.kt`
2. Widget XML metadata

### Verification:
```bash
./gradlew assembleDebug
```

---

## Execution Order

Batches for executing-plans skill:

| Batch | Tasks | Description |
|-------|-------|-------------|
| 0 | 0.1, 0.2, 0.3 | Phase 0: Vikunja API + CalDAV + Task model research |
| 1 | 1, 2, 3 | Project setup + Domain models + Interfaces |
| 2 | 4 | Use cases with TDD |
| 3 | 5, 6 | Room DB + DAO tests |
| 4 | 7, 8 | Repository impls + DI |
| 5 | 9, 10, 11 | Navigation + Focus UI + Habits UI |
| 6 | 12, 13, 14, 15 | Quick Add + remaining screens + strings + widget |
