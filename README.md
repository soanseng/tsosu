# Tsosu 做事

> 台語「做事」(tsò-sū) — A task manager designed by a psychiatrist with ADHD.

**Tsosu** is a native Android task manager built for minds that work differently. It connects to [Vikunja](https://vikunja.io/) for self-hosted sync, auto-syncs tasks to your calendar, and treats you like a human — not a productivity machine.

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
- **Syncs via Vikunja**: Habits are stored as repeating tasks, routines as projects — visible in Vikunja web UI and synced across devices.

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
- Syncs to Vikunja as labels — visible in web UI too

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
- Habit reminder: "🌅 Your morning routine is ready. Start with just one."
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

The feature most todo apps get wrong. In Tsosu, tasks with dates automatically appear on your calendar. No double entry. No third-party integration needed.

- Set a due date → event auto-created on your calendar
- Change the date → event moves
- Complete the task → event removed
- Time estimate → calendar event duration
- Works with **CalDAV** (Fastmail, Nextcloud) now, **Google Calendar** coming soon

## How It Syncs

Tsosu is **local-first** — it works 100% offline with no account. When you're ready, connect your self-hosted Vikunja server for cross-device sync.

| Tsosu Feature | Vikunja Mapping |
|---------------|-----------------|
| Tasks | Direct API sync (title, done, dueDate, priority, labels...) |
| Habits | Repeating tasks (`repeatAfter`) |
| Routines | Projects (with metadata marker) |
| Energy Level | Labels (`⚡high` / `😐medium` / `🪫low`) |
| Time Estimate | Description metadata (`<!-- tsosu:{"est":30} -->`) |
| Focus 3 | Local only (resets daily) |
| Streak tracking | Local only (completion history) |
| Calendar events | Local only (per-device) |

## Technical Details

- **Android native** — Kotlin, Jetpack Compose, Material 3
- **Local-first** — works 100% offline, no account needed
- **Vikunja sync** — optional, connect your self-hosted Vikunja server
- **Calendar sync** — CalDAV (Fastmail, Nextcloud), Google Calendar (planned)
- **Todoist import** — bring your existing tasks via CSV/JSON
- **Privacy** — no analytics, no tracking, no data collection
- **Languages** — English, 繁體中文
- **Architecture** — MVVM, Clean Architecture, TDD

## Who Made This?

Tsosu is designed by a **board-certified psychiatrist** who also lives with ADHD. Every design decision comes from both clinical expertise and personal experience with the daily challenges of executive function, time blindness, and decision fatigue.

This isn't a productivity app that happens to have some ADHD features. ADHD-friendly design IS the product.

## License

Tsosu is a proprietary application. It communicates with Vikunja (AGPL-3.0) via HTTP REST API only — no Vikunja source code is embedded.

---

*tsosu.app — 做事，用你的方式。*
*Getting things done, your way.*
