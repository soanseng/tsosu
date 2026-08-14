# Tsosu 做事

[繁體中文](README.zh-TW.md)

> 台語「做事」(tsò-sū) — A task manager designed by a psychiatrist with ADHD.

**Tsosu** is a native Android task manager built for minds that work differently. It stores everything as plain markdown files — sync to your desktop with Obsidian, nvim, or any text editor via Syncthing. No server needed.

## Why Tsosu?

Most task managers are designed for neurotypical brains. They punish you with overdue counts, overwhelm you with options, and make you feel guilty when you fall behind.

Tsosu is different. It's built on clinical understanding of ADHD and the principles of Atomic Habits — **make it small, make it easy, celebrate progress.**

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
- **Gentle accountability**: "You've done your morning routine 5 of the last 7 days — that's great!" not "You missed 2 days."

### 😌 No Shame UI — "Still on your list."

Every pixel is reviewed for shame and anxiety triggers.

- Overdue tasks say "Still on your list — reschedule?" not "⚠️ OVERDUE"
- No red badge counts showing "47 overdue tasks"
- No streak penalties that punish breaks
- Tasks older than 2 weeks? "These have been here a while. Archive or reschedule?"
- Weekly review shows what you **completed**, not what you didn't

### ⏱ Time Awareness — "That's a lot for one day."

ADHD often comes with time blindness. Tsosu makes time visible without being preachy.

- Estimate how long each task takes (15min / 30min / 1hr / 2hr)
- Today view shows total: "⏱ Today: ~2.5 hrs estimated"
- Over 6 hours? Gentle nudge: "That's a lot — maybe pick your top 3?"
- Calendar events use your time estimates for accurate scheduling

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

### 🔔 Gentle Nudge — "Hey, not hey!"

Notifications that feel like a supportive friend, not a disappointed boss.

- Morning: "Pick your Focus 3 for today 🎯"
- Afternoon: "1/3 done — want to tackle another? 💪"
- All done: "🎉 Focus 3 complete! You did it!"
- Welcome back (after days away): "No pressure — pick just one thing?"
- All nudges are configurable — turn on/off, set times

### 🎉 Weekly Review — "Look what you did!"

End of week = celebration, not report card.

- "You completed 15 tasks this week!"
- "Habits: 28 out of 35 — 80%!"
- Focus days completed: 4/7 ⭐
- Most active project
- "Progress, not perfection."
- Zero mention of what's left undone

### 🧹 Stale Task Cleanup — "It's OK to let go."

Tasks sitting untouched for 2+ weeks get a gentle prompt:

- "These have been waiting a while. Archive or reschedule?"
- One-tap archive all
- No guilt, no judgment — sometimes priorities change

### 📅 Calendar Auto-Sync — "Set a date, see it everywhere."

Tasks with dates automatically appear on your calendar. No double entry.

- Set a due date → event auto-created on your calendar
- Change the date → event moves
- Complete the task → event removed
- Time estimate → calendar event duration
- Works with **CalDAV** (Fastmail, Nextcloud) and **Google Calendar**

## How It Syncs

Tsosu is **local-first** — it works 100% offline with no account. Your data lives as plain markdown files that you own.

### Markdown Files

Point Tsosu at any folder on your phone. It writes two files:

**`tasks.md`** — your tasks, grouped by project:
```markdown
## Inbox

- [ ] Buy groceries ⚡medium 🍅15m <!-- id:abc-123 -->
- [x] Call dentist ✅ 2026-03-22 ⚡low <!-- id:def-456 -->

## Work

- [ ] Prepare presentation 📅 2026-03-25 ⚡high 🍅60m ‼️urgent <!-- id:ghi-789 -->
```

**`habits.md`** — your habits with completion history:
```markdown
## Daily

- [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium <!-- id:h1 -->
  - ✅ 2026-03-23
  - ✅ 2026-03-22
  - ✅ 2026-03-21
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
| `⏫` `🔺` `🔽` `🔽` | priority: urgent / high / medium / low | `⏫` |
| `<!-- id:... -->` | stable task id — **do not edit** | `<!-- id:ghi-789 -->` |
| `<!-- conflict -->` | task was edited on both sides; vault version was kept | |

Projects are `## Heading` sections. Tasks under the first section (or before any heading) belong to Inbox. Tasks with a description or subtasks additionally get a note file under `tasks/`; the index links to it with a wikilink `[[tasks/...]]`.

### Habit line format (`habits.md`)

```markdown
## Daily

- [ ] Exercise (tiny: do 1 pushup) 🔁daily ⚡medium <!-- id:h1 -->
  - ✅ 2026-03-23
```

Completion history is indented `- ✅ YYYY-MM-DD` lines under the habit. Full history lives in `habits/<slug>-<id8>.md`.

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
