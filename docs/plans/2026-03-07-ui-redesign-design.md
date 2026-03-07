# Tsosu UI Redesign Design Document

**Date:** 2026-03-07
**Status:** Approved
**Inspiration:** Grit (shub39/Grit), VerveDo (Super12138/VerveDo) — UI patterns only, no code copied (both GPL-3)

## Goals

Full UI redesign of Tsosu ADHD task manager: custom Material 3 theme with warm orange brand identity, dopamine-driven feedback (konfetti, haptics, animated cards), consolidated navigation, and an overview dashboard on the Focus home screen.

## 1. Theme & Color System

- **Brand color:** Warm Orange `#FF7043` (Deep Orange 400) as seed color
- **Dynamic colors:** Toggle in settings; when enabled uses wallpaper via `dynamicColorScheme` (Android 12+); when off uses brand orange static palette
- **Dark mode:** Follow system by default, manual override in settings (Light / Dark / System)
- **Typography:** Default Material 3 typography, no custom fonts
- **Shapes:** Rounded corners with animated morphing on press (spring physics)
- Define `TsosuTheme` composable replacing bare `MaterialTheme {}`

## 2. Navigation Restructure

### Bottom Nav: 3 tabs
- **Focus** (home) — `Icons.Default.TaskAlt` — Overview dashboard + today's tasks
- **Habits** — `Icons.Default.Loop` — Daily habits grouped by routine time
- **Upcoming** — `Icons.Default.DateRange` — Future tasks by date

### FAB behavior
- **Single tap** -> Quick Add Task sheet
- **Long press** -> Pick One mode (full-height bottom sheet with energy selector + random task)

### Removed from bottom nav
- **Inbox** — tasks with no due date merge into Focus "Other tasks" section
- **Pick One** — moved to FAB long press

### Screen transitions
- Tab switches: `fadeScale` with motion scheme
- Settings navigation: `materialSharedAxisX` with offset
- Bottom sheets (Quick Add, Task Detail, Pick One): `veilFade` effect
- Back navigation: reverse of enter transition

## 3. Focus Screen (Home Tab) Redesign

Single `LazyColumn` with two zones:

### Zone 1 — Overview Dashboard (top)
- **ProgressCard:** circular or linear progress showing "3/7 tasks done today", warm orange fill
- **StatsRow:** horizontal stat chips — streak count, total estimated minutes, energy distribution
- Compact: ~160dp total height, scrolls with content

### Zone 2 — Task Lists
- **"Today's Focus"** section in Card with `primaryContainer` — top 3 priority tasks
- **"Other tasks"** section — remaining tasks + inbox items (no due date), sorted by priority
- Inbox items show small "No date" chip
- Empty state: warm encouragement text

## 4. Interaction Feedback & Animations

### Haptic Feedback
| Action | Haptic Constant |
|--------|----------------|
| Task completion toggle | `CONFIRM` |
| Task creation | `CONFIRM` |
| Task deletion | `REJECT` |
| Priority/energy selection | `CLOCK_TICK` |
| FAB tap/long press | `LONG_PRESS` |
| Bottom sheet expand | `GESTURE_START` |

### Konfetti Celebration
- Triggers on task marked done (not un-done)
- 3 emitters: bottom-center, bottom-left, bottom-right
- Colors: brand orange blended with 8-9 complementary colors at 30%
- 100 particles/sec over 2 seconds, auto-hides on complete

### Card Animations
- `animateColorAsState` for selection/completion state
- Corner shape morphs on press (spring physics)
- `AnimatedVisibility` with `fadeIn + expandVertically` for conditional content
- Completed tasks: crossfade to `secondaryContainer` + strikethrough

### List Animations
- `animateItem()` on LazyColumn items for smooth reorder/removal

## 5. Component Redesigns

### TaskListItem (shared)
- Wrap in Card with animated shape (corner morph on press)
- Priority: colored left border + subtle card tint
- Custom circular checkbox with animated fill (brand color)
- Energy emoji, optional category chip
- Due date: relative format ("Today", "Tomorrow", "in 3d"), overdue in `error` color
- Haptic on every interaction

### QuickAddTaskSheet
- Keep fields: title, priority, energy, time estimate, due date
- Horizontally scrollable chip groups
- Quick-date buttons: "Today", "Tomorrow", "Next Week" before calendar
- Haptic on each selection, animated expand for optional fields

### TaskDetailSheet
- Same fields as Quick Add, pre-filled for editing
- Delete button with confirmation dialog + haptic reject
- `veilFade` transition

### PickOneSheet (new, replaces PickOneScreen)
- Full-height bottom sheet via FAB long press
- Energy selector chips at top
- Large hero card with animated entrance (scale + fade)
- "Pick another" (shuffle animation) + "Start this one" buttons

### SettingsScreen
- Add: Dynamic Colors toggle, Dark Mode picker
- Keep: Vikunja sync, Todoist import
- `materialSharedAxisX` transition

## 6. Habits & Upcoming Screen Polish

### HabitsScreen
- Keep routine grouping (Morning/Anytime/Evening)
- Animated card style matching TaskListItem
- Progress header: circular ring "5/8 done"
- Mini konfetti when all habits in a routine group completed
- Haptic on toggle, completed crossfade to `secondaryContainer`

### UpcomingScreen
- Group by date headers: "Today", "Tomorrow", "This Week", "Later"
- Sticky date headers on scroll
- Same TaskListItem card style
- Collapse empty groups, overall empty state: "Clear schedule ahead!"

### WeeklyReviewScreen
- Polish with new theme, lower priority
- Accessible from settings or Focus overflow menu (not bottom nav)

## Dependencies

- `nl.dionsegijn:konfetti-compose` — celebration animations
- No other new dependencies needed; Material 3 animation APIs are built-in

## GPL-3 License Compliance

Grit and VerveDo are both GPL-3 licensed. This design takes **conceptual inspiration only** — design patterns, UX flows, and visual ideas. All implementation code is original. No source code has been or will be copied.
