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
- Focus tasks stay at the top, everything else collapses
- Complete all 3 → 🎉 celebration animation
- "Everything else is bonus" — because it truly is

### 🔁 Daily Habits — "Make it small, make it easy."

Inspired by Atomic Habits: start with something so small you can't say no. Tsosu's habit tracking is designed for ADHD minds that struggle with consistency.

- **2-Minute Habits**: "Meditate" → "Sit and take 3 breaths." Small enough to always start.
- **Flexible streaks**: Missed a day? Your streak pauses, not resets. "4 out of the last 7 days" not "Day 0 again."
- **Morning & Evening routines**: Group habits into routines you can check off in order.
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

## Technical Details

- **Android native** — Kotlin, Jetpack Compose, Material 3
- **Local-first** — works 100% offline, no account needed
- **Markdown sync** — plain `.md` files, compatible with Obsidian Tasks
- **Calendar sync** — CalDAV (Fastmail, Nextcloud), Google Calendar
- **Todoist import** — bring your existing tasks
- **Privacy** — no analytics, no tracking, no data collection, no server
- **Architecture** — MVVM, Clean Architecture, TDD

## Who Made This?

Tsosu is designed by a **board-certified psychiatrist** who also lives with ADHD. Every design decision comes from both clinical expertise and personal experience with the daily challenges of executive function, time blindness, and decision fatigue.

This isn't a productivity app that happens to have some ADHD features. ADHD-friendly design IS the product.

## License

Tsosu is a proprietary application.

---

*tsosu.app — 做事，用你的方式。*
*Getting things done, your way.*
