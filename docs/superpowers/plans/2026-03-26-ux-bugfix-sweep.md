# UX Bugfix Sweep — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix 12 UX bugs spanning form validation, error handling, konfetti timing, accessibility, and visual polish.

**Architecture:** All changes are in the Compose UI layer (`app/` module). No domain/data changes needed. Each task touches 1-2 files and is independently committable.

**Tech Stack:** Jetpack Compose, Material 3, Kotlin, Hilt

**Build command:** `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`

---

### Task 1: QuickAdd form validation feedback (#1)

Both QuickAddTaskSheet and QuickAddHabitSheet silently ignore blank titles. Users tap "Add" and nothing happens.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddHabitSheet.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add error string resources**

In `app/src/main/res/values/strings.xml`, add after the `quick_add_ok` line:
```xml
<string name="quick_add_title_required">Please enter a title</string>
```

In `app/src/main/res/values-zh-rTW/strings.xml`, add after the existing quick_add section:
```xml
<string name="quick_add_title_required">請輸入標題</string>
```

- [ ] **Step 2: Add validation state to QuickAddTaskSheet**

In `QuickAddTaskSheet.kt`, add a state variable after `var title`:
```kotlin
var titleError by remember { mutableStateOf(false) }
```

Change the `OutlinedTextField` for title to show error:
```kotlin
OutlinedTextField(
    value = title,
    onValueChange = {
        title = it
        if (it.isNotBlank()) titleError = false
    },
    label = { Text(stringResource(R.string.quick_add_task_hint)) },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    isError = titleError,
    supportingText = if (titleError) {
        { Text(stringResource(R.string.quick_add_title_required)) }
    } else null,
)
```

Change the Button onClick to set error:
```kotlin
onClick = {
    if (title.isNotBlank()) {
        haptic.confirm()
        val recurrenceRule = when (selectedRecurrence) {
            RecurrenceOption.CUSTOM -> customRecurrence.takeIf { it.isNotBlank() }
            else -> selectedRecurrence.rule
        }
        onAdd(title, selectedPriority, selectedEnergy, estimatedMinutes.takeIf { it > 0 }, dueDate, reminderTime, recurrenceRule)
        onDismiss()
    } else {
        titleError = true
    }
},
```

- [ ] **Step 3: Add validation state to QuickAddHabitSheet**

In `QuickAddHabitSheet.kt`, add a state variable after `var title`:
```kotlin
var titleError by remember { mutableStateOf(false) }
```

Change the title `OutlinedTextField`:
```kotlin
OutlinedTextField(
    value = title,
    onValueChange = {
        title = it
        if (it.isNotBlank()) titleError = false
    },
    label = { Text(stringResource(R.string.quick_add_habit_hint)) },
    modifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester),
    singleLine = true,
    isError = titleError,
    supportingText = if (titleError) {
        { Text(stringResource(R.string.quick_add_title_required)) }
    } else null,
)
```

Change the Button onClick:
```kotlin
onClick = {
    if (title.isNotBlank()) {
        onAdd(title, tinyVersion.takeIf { it.isNotBlank() }, routineTime)
    } else {
        titleError = true
    }
},
```

- [ ] **Step 4: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddHabitSheet.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "fix(ux): show validation error when QuickAdd title is blank"
```

---

### Task 2: Habit toggle error feedback (#2)

`HabitsViewModel.createHabit()` silently catches errors. Users get no feedback when habit creation fails.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsViewModel.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add error string resources**

In `values/strings.xml`, add in the Habits section:
```xml
<string name="habits_create_failed">Failed to create habit</string>
```

In `values-zh-rTW/strings.xml`:
```xml
<string name="habits_create_failed">建立習慣失敗</string>
```

- [ ] **Step 2: Add error state to HabitsViewModel**

In `HabitsViewModel.kt`, add a `SharedFlow` for error events after the `routineMutex` line:

```kotlin
private val _errorEvent = MutableSharedFlow<String>()
val errorEvent = _errorEvent.asSharedFlow()
```

Add the import:
```kotlin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
```

Update `createHabit` to emit error:
```kotlin
fun createHabit(title: String, tinyVersion: String?, routineTime: RoutineTime) {
    viewModelScope.launch {
        val routineId = findOrCreateRoutine(routineTime)
        val habit = Habit(
            title = title,
            tinyVersion = tinyVersion,
            routineId = routineId,
        )
        createHabitUseCase(habit).onFailure { e ->
            Log.e("HabitsViewModel", "Failed to create habit", e)
            _errorEvent.emit(e.message ?: "Unknown error")
        }
    }
}
```

- [ ] **Step 3: Show snackbar on error in HabitsScreen**

In `HabitsScreen.kt`, add snackbar host state and collect errors. Add these parameters and the effect right after `val haptic = rememberHaptic()`:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
val errorMsg = stringResource(R.string.habits_create_failed)

LaunchedEffect(Unit) {
    viewModel.errorEvent.collect {
        snackbarHostState.showSnackbar(errorMsg)
    }
}
```

Wrap the `LazyColumn` in a `Scaffold` with the snackbar host:
```kotlin
Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    containerColor = Color.Transparent,
) { padding ->
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        // ... existing content
    )
}
```

Add imports:
```kotlin
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
```

- [ ] **Step 4: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/habits/HabitsViewModel.kt app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "fix(ux): surface habit creation errors via snackbar"
```

---

### Task 3: Konfetti fires before toggle completes (#3)

In `HabitsScreen.kt`, konfetti shows immediately before `onToggleHabit` returns. If toggle fails, the user sees celebration for a failed action.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsViewModel.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`

- [ ] **Step 1: Make onToggleHabit return success/failure**

In `HabitsViewModel.kt`, add a `SharedFlow` for celebration events:

```kotlin
private val _celebrateEvent = MutableSharedFlow<Unit>()
val celebrateEvent = _celebrateEvent.asSharedFlow()
```

Update `onToggleHabit`:
```kotlin
fun onToggleHabit(habitId: String) {
    viewModelScope.launch {
        val today = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val wasCompleted = uiState.value.habits
            .find { it.habit.id == habitId }?.isCompletedToday ?: false
        completeHabit(habitId, today)
        if (!wasCompleted) {
            _celebrateEvent.emit(Unit)
        }
    }
}
```

- [ ] **Step 2: Collect celebrate events in HabitsScreen**

In `HabitsScreen.kt`, add a `LaunchedEffect` to collect celebration events (add after the error `LaunchedEffect`):

```kotlin
LaunchedEffect(Unit) {
    viewModel.celebrateEvent.collect {
        showKonfetti.value = true
    }
}
```

Remove the inline konfetti logic from `onToggle` lambdas. Change both `onToggle` callbacks (routined and unroutined habits) from:
```kotlin
onToggle = {
    haptic.confirm()
    if (!habitWithStatus.isCompletedToday) {
        showKonfetti.value = true
    }
    viewModel.onToggleHabit(habitWithStatus.habit.id)
},
```
to:
```kotlin
onToggle = {
    haptic.confirm()
    viewModel.onToggleHabit(habitWithStatus.habit.id)
},
```

- [ ] **Step 3: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/habits/HabitsViewModel.kt app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt
git commit -m "fix(ux): fire konfetti after toggle succeeds, not before"
```

---

### Task 4: HabitRow dual click target (#4)

The Card Row's `clickable` and the nested `Checkbox` both handle clicks, creating confusion.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`

- [ ] **Step 1: Remove Checkbox onCheckedChange**

In `HabitsScreen.kt`, in the `HabitRow` composable, change the `Checkbox`:
```kotlin
Checkbox(
    checked = habitWithStatus.isCompletedToday,
    onCheckedChange = null,
)
```

This makes the Checkbox display-only while the Row clickable handles the toggle.

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt
git commit -m "fix(ux): remove duplicate click target on HabitRow checkbox"
```

---

### Task 5: Konfetti only fires once in FocusScreen (#5)

`showKonfetti` is set to `true` on first task complete, then the `KonfettiOverlay` callback sets it to `false` when animation ends. But subsequent completions set it to `true` again — the issue is that `KonfettiView` is keyed on the same `parties` list, so Compose doesn't recompose a new `KonfettiView`.

The fix: use a counter instead of a boolean so each trigger produces a new key.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/components/KonfettiOverlay.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`

- [ ] **Step 1: Refactor KonfettiOverlay to use counter-based trigger**

Replace `KonfettiOverlay.kt` entirely:
```kotlin
package app.tsosu.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.compose.OnParticleSystemUpdateListener
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.PartySystem
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

@Composable
fun KonfettiOverlay(trigger: MutableIntState) {
    val count = trigger.intValue
    if (count == 0) return

    var lastSeen by remember { mutableIntStateOf(0) }
    if (count == lastSeen) return
    lastSeen = count

    val primary = MaterialTheme.colorScheme.primary.toArgb()
    val colors = listOf(
        primary,
        Color(0xFFFFA726).toArgb(),
        Color(0xFFFFD54F).toArgb(),
        Color(0xFFFF8A65).toArgb(),
        Color(0xFF81C784).toArgb(),
        Color(0xFF4FC3F7).toArgb(),
        Color(0xFFBA68C8).toArgb(),
        Color(0xFFFF7043).toArgb(),
        Color(0xFFE0E0E0).toArgb(),
    )

    val parties = listOf(
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.5, 1.0),
            spread = 360,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(0.0, 1.0),
            angle = 45,
            spread = 90,
        ),
        Party(
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
            colors = colors,
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            position = Position.Relative(1.0, 1.0),
            angle = 135,
            spread = 90,
        ),
    )

    KonfettiView(
        parties = parties,
        updateListener = object : OnParticleSystemUpdateListener {
            override fun onParticleSystemEnded(system: PartySystem, activeSystems: Int) {
                // no-op: counter-based, no need to reset
            }
        },
    )
}
```

- [ ] **Step 2: Update FocusScreen to use counter**

In `FocusScreen.kt`, change:
```kotlin
val showKonfetti = remember { mutableStateOf(false) }
```
to:
```kotlin
val konfettiTrigger = remember { mutableIntStateOf(0) }
```

Change `KonfettiOverlay(showKonfetti)` to `KonfettiOverlay(konfettiTrigger)`.

Change both `showKonfetti.value = true` occurrences to `konfettiTrigger.intValue++`.

Add import: `import androidx.compose.runtime.mutableIntStateOf`

- [ ] **Step 3: Update HabitsScreen to use counter**

In `HabitsScreen.kt`, change:
```kotlin
val showKonfetti = remember { mutableStateOf(false) }
```
to:
```kotlin
val konfettiTrigger = remember { mutableIntStateOf(0) }
```

Change `KonfettiOverlay(showKonfetti)` to `KonfettiOverlay(konfettiTrigger)`.

In the `LaunchedEffect` for celebrate events, change `showKonfetti.value = true` to `konfettiTrigger.intValue++`.

Add import: `import androidx.compose.runtime.mutableIntStateOf`

- [ ] **Step 4: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/components/KonfettiOverlay.kt app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt
git commit -m "fix(ux): allow konfetti to fire on every task/habit completion"
```

---

### Task 6: Badge overflow at 99+ (#7)

Badge count renders raw number, which breaks layout at high counts.

**Files:**
- Modify: `app/src/main/java/app/tsosu/navigation/BottomNavBar.kt`

- [ ] **Step 1: Cap badge display at 99+**

In `BottomNavBar.kt`, change the badge text from:
```kotlin
BadgedBox(badge = { Badge { Text("$badgeCount") } }) {
```
to:
```kotlin
BadgedBox(badge = {
    Badge { Text(if (badgeCount > 99) "99+" else "$badgeCount") }
}) {
```

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/navigation/BottomNavBar.kt
git commit -m "fix(ux): cap bottom nav badge at 99+"
```

---

### Task 7: Streak shows only after day 2 (#8)

Streak emoji only shows when `currentConsecutiveDays > 1`. Day 1 gets no feedback.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt`

- [ ] **Step 1: Show streak from day 1**

In `HabitsScreen.kt`, in `HabitRow`, change:
```kotlin
if (info.currentConsecutiveDays > 1) {
```
to:
```kotlin
if (info.currentConsecutiveDays > 0) {
```

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/habits/HabitsScreen.kt
git commit -m "fix(ux): show streak counter from day 1"
```

---

### Task 8: Accessibility — missing contentDescriptions (#10)

Multiple icons across the app have `contentDescription = null`.

**Files:**
- Modify: `app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt`
- Modify: `app/src/main/java/app/tsosu/ui/components/TaskListItem.kt`
- Modify: `app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Add accessibility string resources**

In `values/strings.xml`, add:
```xml
<!-- Accessibility -->
<string name="a11y_filter_active">Filter active</string>
<string name="a11y_expand_section">Expand section</string>
<string name="a11y_collapse_section">Collapse section</string>
<string name="a11y_setup_vault">Set up vault</string>
<string name="a11y_pick_date">Pick date</string>
<string name="a11y_pick_time">Pick time</string>
```

In `values-zh-rTW/strings.xml`, add:
```xml
<!-- Accessibility -->
<string name="a11y_filter_active">篩選已啟用</string>
<string name="a11y_expand_section">展開區段</string>
<string name="a11y_collapse_section">收合區段</string>
<string name="a11y_setup_vault">設定保管庫</string>
<string name="a11y_pick_date">選擇日期</string>
<string name="a11y_pick_time">選擇時間</string>
```

- [ ] **Step 2: Fix FocusScreen contentDescriptions**

In `FocusScreen.kt`:

Line 82 — vault setup icon:
```kotlin
contentDescription = stringResource(R.string.a11y_setup_vault),
```

Line 126 — filter active icon:
```kotlin
contentDescription = stringResource(R.string.a11y_filter_active),
```

Line 191 — expand/collapse arrow:
```kotlin
contentDescription = if (noDateExpanded)
    stringResource(R.string.a11y_collapse_section)
else
    stringResource(R.string.a11y_expand_section),
```

- [ ] **Step 3: Fix TaskListItem swipe contentDescriptions**

In `TaskListItem.kt`, line 148, change:
```kotlin
Icon(icon, contentDescription = null, tint = Color.White)
```
to:
```kotlin
Icon(
    icon,
    contentDescription = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> stringResource(R.string.swipe_done)
        else -> stringResource(R.string.swipe_postpone)
    },
    tint = Color.White,
)
```

- [ ] **Step 4: Fix QuickAddTaskSheet icon contentDescriptions**

In `QuickAddTaskSheet.kt`:

Line 190 — calendar icon:
```kotlin
Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.a11y_pick_date))
```

Line 213 — time icon:
```kotlin
Icon(Icons.Default.AccessTime, contentDescription = stringResource(R.string.a11y_pick_time))
```

- [ ] **Step 5: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/app/tsosu/ui/screens/focus/FocusScreen.kt app/src/main/java/app/tsosu/ui/components/TaskListItem.kt app/src/main/java/app/tsosu/ui/screens/quickadd/QuickAddTaskSheet.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "fix(a11y): add missing contentDescriptions across UI"
```

---

### Task 9: "No date" section label unclear (#11)

The collapsible section title "No date" is confusing — it contains both unscheduled tasks and inbox tasks.

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rTW/strings.xml`

- [ ] **Step 1: Rename string resource**

In `values/strings.xml`, change:
```xml
<string name="focus_other_tasks">No date</string>
```
to:
```xml
<string name="focus_other_tasks">Other tasks</string>
```

In `values-zh-rTW/strings.xml`, change:
```xml
<string name="focus_other_tasks">未排期</string>
```
to:
```xml
<string name="focus_other_tasks">其他任務</string>
```

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-zh-rTW/strings.xml
git commit -m "fix(ux): rename 'No date' section to 'Other tasks'"
```

---

### Task 10: No sync progress indicator (#12)

Sync runs silently on resume — no indication to the user that something is happening.

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`

- [ ] **Step 1: Show syncing snackbar before sync completes**

In `MainActivity.kt`, in `setupResumePull()`, add a "Syncing..." message before the sync call. Change the sync block from:

```kotlin
if (isConfigured) {
    val result = syncRepository.sync()
    result.fold(
```

to:

```kotlin
if (isConfigured) {
    snackbarHostState.showSnackbar("Syncing…")
    val result = syncRepository.sync()
    result.fold(
```

Note: `showSnackbar` is a suspend function — it will display and auto-dismiss when the next snackbar (success/failure) shows. Since we're already in a coroutine scope, this works naturally.

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/MainActivity.kt
git commit -m "fix(ux): show syncing indicator during markdown sync"
```

---

### Task 11: Clean up unused imports from FAB fix

The earlier FAB fix left behind the unused `ExperimentalFoundationApi` and `combinedClickable` imports in `MainActivity.kt`.

**Files:**
- Modify: `app/src/main/java/app/tsosu/MainActivity.kt`

- [ ] **Step 1: Remove unused imports**

In `MainActivity.kt`, remove these two lines:
```kotlin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
```

- [ ] **Step 2: Build and verify**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/app/tsosu/MainActivity.kt
git commit -m "chore: remove unused FAB imports in MainActivity"
```

---

### Task 12: Final verification

- [ ] **Step 1: Full build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test`
Expected: All tests pass
