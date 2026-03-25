# Bottom Nav Redesign: Remove Inbox, Wire Habit Creation

**Date:** 2026-03-25
**Status:** Approved

## Problem

1. **Inbox tab is redundant** — Focus already shows inbox tasks in "Other Tasks" section. Inbox only shows no-date tasks and is empty most of the time.
2. **Habit creation is disconnected** — `QuickAddHabitSheet` exists but is not wired to any UI. The Habits empty state says "Tap +" but FAB only creates tasks.
3. **Focus/Inbox overlap** — Focus already includes inbox tasks, making the Inbox tab pointless.

## Design

### Bottom Nav: 5 tabs → 4 tabs

Remove Inbox. Keep Focus as start destination.

| Tab | Icon | Content |
|-----|------|---------|
| Focus | TaskAlt | Today's tasks + no-date tasks (absorbs Inbox) |
| Habits | Loop | Habit checklist grouped by routine time |
| Calendar | CalendarMonth | Month view + tasks for selected date |
| Upcoming | DateRange | Future tasks grouped by time period |

### Focus absorbs Inbox

- Focus already has `state.inboxTasks` rendered in "Other Tasks" section
- Rename section header: "Other Tasks" → "No Date" (string resource `focus_other_tasks`)
- No ViewModel/data logic changes needed — FocusViewModel already queries inbox tasks

### FAB behavior based on current tab

MainActivity monitors `navController.currentBackStackEntry` route:

| Current Tab | FAB tap | FAB long-press |
|-------------|---------|----------------|
| Focus, Calendar, Upcoming | QuickAddTaskSheet | PickOneSheet |
| Habits | QuickAddHabitSheet | — (no action) |

### QuickAddHabitSheet additions

Add a **Routine time picker** (Morning / Anytime / Evening) using `FilterChip` row:

```
Fields:
- Title (required) — existing
- Tiny version (optional) — existing
- Routine time (Morning / Anytime / Evening) — NEW, default: Anytime
```

The selected routine time maps to a `RoutineTime` enum value. The sheet's `onAdd` callback becomes `(title, tinyVersion, routineTime) -> Unit`.

### Habit creation data flow

```
QuickAddHabitSheet
  → MainActivity.onAdd callback
    → HabitsViewModel.createHabit(title, tinyVersion, routineTime)
      → HabitRepository.createHabit(title, tinyVersion, routineTime)
        → HabitDao.insert(HabitEntity)
```

Need to implement:
- `HabitsViewModel.createHabit()` method
- `HabitRepository.createHabit()` (interface + impl)
- Map `RoutineTime` to a routine ID (find or create matching routine)

## Files to modify

| File | Change |
|------|--------|
| `navigation/Screen.kt` | Remove Inbox from `bottomNavItems` |
| `navigation/BottomNavBar.kt` | No change (reads from `bottomNavItems`) |
| `navigation/TsosuNavHost.kt` | Keep Inbox route (accessible but not in nav) |
| `MainActivity.kt` | FAB behavior switch based on current route; add habit sheet state |
| `ui/screens/quickadd/QuickAddHabitSheet.kt` | Add RoutineTime picker |
| `ui/screens/habits/HabitsViewModel.kt` | Add `createHabit()` |
| `domain/repository/HabitRepository.kt` | Add `createHabit()` to interface |
| `data-local/.../HabitRepositoryImpl.kt` | Implement `createHabit()` |
| `res/values/strings.xml` | Update `focus_other_tasks` label |

## Out of scope

- Merging Calendar/Upcoming (kept separate per user preference)
- Moving Settings/WeeklyReview to bottom nav
- Habit editing or deletion UI
- InboxScreen deletion (kept in codebase, just removed from nav)
