# Tsosu 做事

[繁體中文](README.zh-TW.md) · [🌐 Website](https://soanseng.github.io/tsosu/)

> 台語「做事」(tsò-sū) — A task manager designed by a psychiatrist with ADHD.

**Tsosu** is a native Android task manager built for minds that work differently. It stores everything as plain markdown files — sync to your desktop with Obsidian, nvim, or any text editor via Syncthing. No server needed.

## Why Tsosu?

Most task managers are designed for neurotypical brains. They punish you with overdue counts, overwhelm you with options, and make you feel guilty when you fall behind.

Tsosu is different. It's built on clinical understanding of ADHD and the principles of Atomic Habits — **make it small, make it easy, celebrate progress.**

## What's New in 1.1.0

- **⚡ Energy & streak freezes (Duolingo-style)** — completing tasks and habits earns energy; spend ⚡30 on a streak freeze that automatically bridges a missed day.
- **✍️ Quick-add syntax** — type `every mon`, `daily`, `every morning until 8/31`, or `p1`–`p4` right in the task title; the same words work in Obsidian.
- **🗂 Board view & saved views** — kanban columns per project with attached habits, list/board toggle, filter presets you can save as named views.
- **🔁 Habit editor** — per-habit reminders, specific weekdays (Mon/Wed/Fri), habit↔project links, task→habit conversion.
- **🌐 Language picker** — English / 繁體中文 in-app, plus a recurrence-keyword reference under Settings.

## Install

### Via Obtainium (recommended)

[Obtainium](https://github.com/ImranR98/Obtainium) tracks this project's GitHub Releases directly — install once, and new versions are auto-detected for update.

1. Install Obtainium (Google Play or F-Droid)
2. Open Obtainium, tap the **＋** button to add an app
3. Set the app source to **GitHub**, enter `soanseng/tsosu`
4. Tap **Add** — the latest APK downloads automatically, and future releases get update notifications

### Direct download

Prefer not to use Obtainium? Grab the latest `.apk` from the [Releases page](https://github.com/soanseng/tsosu/releases). Android will ask for the "install unknown apps" permission — allow it and install.

## Features

### 🎯 Focus 3 — "Just pick three."

ADHD brains freeze when facing a list of 30 tasks. Tsosu asks you to pick just 3 tasks for today. The rest folds away — still there, but not screaming at you.

- Pick your top 3 each morning (or the night before)
- Focus tasks stay at the top, everything else collapses into a **collapsible "No date" section** with a count badge
- Complete all 3 → 🎉 celebration animation
- "Everything else is bonus" — because it truly is
- **Swipe right** to mark done, **swipe left** to postpone to tomorrow — no need to hunt for tiny buttons
- **Overdue badge** on tasks that need attention
- Bottom nav shows your **pending count** so you know at a glance what's left

### 🔁 Daily Habits — "Make it small, make it easy."

Inspired by Atomic Habits: start with something so small you can't say no. Tsosu's habit tracking is designed for ADHD minds that struggle with consistency.

- **2-Minute Habits**: "Meditate" → "Sit and take 3 breaths." Small enough to always start.
- **Flexible streaks**: Missed a day? Your streak pauses, not resets. 🔥 streak counter shows your momentum.
- **7-day progress bar**: Each habit shows a visual bar of your last 7 days — see the trend, not just the number.
- **Morning, Anytime & Evening routines**: Group habits by time of day. Create new habits right from the FAB.
- **Gentle accountability**: a weekly counter (`3/5`) and streak (`🔥7`) on every habit — progress shown, missed days not counted
- **Habit editor**: set frequency, energy, reminder, linked project, and specific weekdays (`Mon/Wed/Fri` style) — the habit only appears on those days
- **Real streaks**: 🔥 counts genuinely consecutive days, and each habit can carry its own reminder time
- Convert any task into a habit straight from the task menu

### 😌 No Shame UI — "Still on your list."

Every pixel is reviewed for shame and anxiety triggers.

- Overdue tasks carry a small "Overdue" tag — no red counter badges screaming how many are late
- No streak penalties that punish breaks — streaks pause, not reset
- Weekly review shows what you **completed**, not what you didn't

### ⚡ Energy & Streak Freezes — "Bank your momentum."

Duolingo-style motivation, minus the guilt.

- Completing tasks and habits earns **⚡ energy**
- Spend ⚡30 on a **streak freeze** — miss a day and the freeze bridges the gap automatically, so your streak survives
- Frozen days count toward the streak; no red screens, just insurance

### ✍️ Quick-Add Syntax — "Type it like you say it."

Todoist-style keywords parsed straight from the task title — in the app or in your vault. The full reference lives in Settings → *Recurrence & quick-syntax help*.

- **Recurrence**: `every day`, `weekly`, `every mon, wed, fri`, `every other week`, `每週一二三` — stored as a `🔁` rule
- **Time of day**: `every morning` / `every afternoon` / `every evening` — daily recurrence plus a preset reminder (08:00 / 13:00 / 18:00 / 21:00)
- **Start & end dates**: `starting 8/20 until 8/31` bounds the schedule; order doesn't matter
- **Priority shorthand**: `p1`–`p4` anywhere in the title (Urgent → Low); the token is stripped from the saved title and pre-selects the priority chip
- Works identically in Obsidian — write `Buy milk every other week` in the vault and the next sync carries the rule

### 🗂 Projects, Board & Saved Views

- **Board view**: kanban columns per project — real project names, with each project's attached habits shown in the column
- **List/Board toggle** on the same screen
- **Filter presets & saved views**: filter by project, due date, status, priority, or energy — save the combination as a named view and jump back anytime

### ⏱ Time Awareness — "That's a lot for one day."

ADHD often comes with time blindness. Tsosu makes time visible without being preachy.

- Estimate how long each task takes — a 🍅 chip on every task card
- "Pick One For Me" and the calendar use your estimates
- Calendar events use your time estimate as the event duration (default 60 min)

### ⚡ Energy Matching — "Work with your energy."

Not all hours are equal. Tag tasks by the energy they need, then match them to how you feel right now.

- 🔋 High energy — for deep work, hard conversations, complex tasks
- 😐 Medium — for routine work, emails, errands
- 🪫 Low energy — for tidying, simple admin, easy wins

### 🎲 "Pick One For Me" — "Just start somewhere."

Decision paralysis is real. When you can't choose what to do, tap the dice.

- Select your current energy level
- Tsosu picks a random matching task
- "Just do this one ✓" — no more analysis paralysis
- Don't like it? "Pick another 🔄"

### ⏰ Task Reminders — "At the time you set."

Reminders that live with the task — set once, fire on the due date, gone when it's done.

- Per-task reminder time — a notification fires at ⏰ on the due date
- Tap the notification to open the task; mark it done straight from the notification
- Overdue summary notification — "3 overdue tasks", tap to open the first one
- Exact alarms (Android 12+ permission prompt) with automatic fallback to inexact alarms
- Alarms resync after reboot and after every vault sync

### 🎉 Weekly Review — "Look what you did!"

End of week = celebration, not report card.

- "You completed 15 tasks this week!"
- "Habits: 28 out of 35 — 80%!"
- Focus days completed: 4/7 ⭐
- Most active project
- "Progress, not perfection."
- Zero mention of what's left undone

### 📅 Calendar Auto-Sync — "Set a date, see it everywhere."

Tasks with dates automatically appear on your calendar. No double entry.

- Set a due date → event auto-created on your calendar
- Change the date → event moves
- Complete the task → event removed
- Time estimate → calendar event duration
- **Quick-add from the calendar** pre-fills the tapped day — tap any date, type the task, it's scheduled
- Works with **CalDAV** (Fastmail, Nextcloud) and **Google Calendar**

### 📥 Todoist Import — "Bring your list with you."

Import your Todoist CSV export from Settings — tasks are deduplicated by id, written as markdown, and pushed into your vault automatically.

### 📝 Markdown Vault — "Your tasks are your files."

Everything lives as plain markdown files in a folder you own — view and edit them in Obsidian, nvim, or any text editor, on any device. Vault changes are auto-detected, so edits flow back into the app without a manual sync. See [How It Syncs](#how-it-syncs).

## How It Syncs

Tsosu is **local-first** — it works 100% offline with no account. Your data lives as plain markdown files that you own.

### Markdown Files

Point Tsosu at any folder on your phone (an Obsidian vault is ideal). It writes a small set of markdown files (per-task/per-habit notes and daily notes are described in Vault Data Format below):

**`tasks.md`** — your tasks, grouped by project:
```markdown
---
tsosu: v1
generated: true
---

## Inbox

- [ ] Buy groceries ⚡medium 🍅15m <!-- id:abc-123 -->
- [x] Call dentist ✅ 2026-03-22 ⚡low <!-- id:def-456 -->

## Work

- [ ] Prepare presentation 📅 2026-03-25 ⚡high 🍅60m ⏫ <!-- id:ghi-789 -->
```

**`habits.md`** — your habits, one line each, grouped by routine (morning / anytime / evening / other), with a weekly counter and streak:
```markdown
---
tsosu: v1
generated: true
---

## 🌅 Morning

- [ ] Exercise (tiny: do 1 pushup) 🔁7x/week ⚡medium 3/5 🔥7 [[habits/exercise-h1abcdef]] <!-- id:h1 -->
```

### Cross-Device Sync

Sync the folder with [Syncthing](https://syncthing.net/) or Obsidian Sync — your tasks appear on every device.

| Where | How |
|-------|-----|
| **Phone** | Tsosu app (rich UI, habit tracking, Focus 3) |
| **Desktop** | Obsidian with [Tasks plugin](https://publish.obsidian.md/tasks/) — checkbox tasks, due dates, filters |
| **Terminal** | nvim / any text editor — it's just markdown |

Inspired by [org-mode](https://doc.norang.ca/org-mode.html): text files as the universal interface.

## Vault Data Format

Tsosu reads and writes a small, documented set of markdown files. Everything is plain text — you can edit any file by hand or with Obsidian, and Tsosu picks up the change on the next sync.

### File layout

```
<your vault folder>/
├── tasks.md              # task index — one checkbox line per task, grouped by project
├── habits.md             # habit index — one line per habit, completion history indented
├── tasks/
│   └── <slug>-<id8>.md   # per-task notes (only for tasks with description/subtasks)
├── habits/
│   └── <slug>-<id8>.md   # per-habit notes with full completion history
└── daily/
    └── YYYY-MM-DD.md     # daily note — today's habits with checkboxes
```

### Task line format (`tasks.md`)

Each task is a single checkbox line. Metadata is appended as emoji markers, and the machine-readable ID is a hidden HTML comment. **Do not remove or edit the `<!-- id:... -->` comment** — it is how Tsosu matches a line to a task across edits.

```markdown
- [ ] Prepare presentation 📅 2026-03-25 ⏰ 09:00 ⚡high 🍅60m ⏫ <!-- id:ghi-789 -->
```

| Marker | Meaning | Example |
|--------|---------|---------|
| `[ ]` `[/]` `[!]` `[>]` `[x]` `[-]` | status: todo / in-progress / on-hold / planned / done / cancelled | `[x]` |
| `✅ YYYY-MM-DD` | completion date (on done tasks) | `✅ 2026-03-22` |
| `❌ YYYY-MM-DD` | cancellation date (on cancelled tasks) | `❌ 2026-03-22` |
| `📅 YYYY-MM-DD` | due date | `📅 2026-03-25` |
| `⏳ YYYY-MM-DD` | scheduled date | `⏳ 2026-03-24` |
| `🛫 YYYY-MM-DD` | start date | `🛫 2026-03-23` |
| `➕ YYYY-MM-DD` | created date | `➕ 2026-03-20` |
| `⏰ HH:MM` | reminder time (on the due date) | `⏰ 09:00` |
| `🔁 <rule>` | recurrence rule (natural language) | `🔁 every monday` |
| `⚡high` `😐medium` `🪫low` | energy level | `⚡high` |
| `🍅 Nm` | time estimate in minutes | `🍅60m` |
| `⏫` `🔺` `🔼` `🔽` | priority: urgent / high / medium / low | `⏫` |
| `<!-- id:... -->` | stable task id — **do not edit** | `<!-- id:ghi-789 -->` |
| `<!-- conflict -->` | task was edited on both sides; vault version was kept | |

Projects are `## Heading` sections. Tasks under the first section (or before any heading) belong to Inbox. Tasks with a description or subtasks additionally get a note file under `tasks/`; the index links to it with a wikilink `[[tasks/...]]`.

### Habit line format (`habits.md`)

```markdown
## 🌅 Morning

- [ ] Exercise (tiny: do 1 pushup) ⏰ 07:30 🔁7x/week ⚡medium 3/5 🔥7 [[habits/exercise-h1abcdef]] <!-- id:h1 -->
```

| Marker | Meaning | Example |
|--------|---------|---------|
| `🔁 Nx/week` | weekly target frequency | `🔁7x/week` |
| `⏰ HH:MM` | daily reminder time | `⏰ 07:30` |
| `3/5` | completions this week / target | `3/5` |
| `🔥N` | current streak in days | `🔥7` |
| `[[habits/...]]` | link to the full-history note | `[[habits/exercise-h1abcdef]]` |

Sections are routines: `## 🌅 Morning`, `## ☀️ Anytime`, `## 🌙 Evening`, then `## Other`. The index line carries only the counters above; completion dates and settings live in the per-habit note under `habits/`, whose frontmatter also stores the routine (`routine: morning`), the linked project (`project: "Work"`), the reminder (`reminder: "07:30"`), and specific weekdays (`weekdays: [1,3,5]`, ISO Mon=1..Sun=7 — the habit only appears on those days) — edit either side, Tsosu reconciles them on the next sync.

### Obsidian tips

- Tsosu never writes into `.obsidian/`, templates, or attachments — safe to sync the whole vault.
- The index files use standard checkbox syntax, so the [Tasks plugin](https://publish.obsidian.md/tasks/) filters and [Dataview](https://blacksmithgu.github.io/obsidian-dataview/) queries work directly:

````markdown
```dataview
TASK FROM "tasks.md"
WHERE !completed AND contains(text, "📅")
```
````

- To add a task by hand: copy any existing line, change the title and dates, and give it a fresh id (`<!-- id:my-new-id -->`). Tsosu keeps unknown ids intact on the next sync.
- If a task was edited in both the app and the vault since the last sync, the vault version wins and the line carries `<!-- conflict -->` so you can review and resolve it manually.

## Technical Details

- **Android native** — Kotlin, Jetpack Compose, Material 3
- **Local-first** — works 100% offline, no account needed
- **Markdown sync** — plain `.md` files, compatible with Obsidian Tasks
- **Calendar sync** — CalDAV (Fastmail, Nextcloud), Google Calendar
- **Todoist import** — bring your existing tasks
- **Privacy** — no analytics, no tracking, no data collection, no server
- **Architecture** — MVVM, Clean Architecture, TDD
- **Localization** — English, 繁體中文

## Who Made This?

Tsosu is designed by a **board-certified psychiatrist** who also lives with ADHD. Every design decision comes from both clinical expertise and personal experience with the daily challenges of executive function, time blindness, and decision fatigue.

This isn't a productivity app that happens to have some ADHD features. ADHD-friendly design IS the product.

## License

Tsosu is a proprietary application.

---

*tsosu.app — 做事，用你的方式。*
*Getting things done, your way.*
