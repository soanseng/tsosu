# Tsosu Development Plan

> tsosu.app — 台語「做事」(tsò-sū)
> Designed by a psychiatrist with ADHD.

## Vikunja Integration Summary

```
Feature           Vikunja Sync Strategy
──────────────────────────────────────────────
Tasks (core)      ✅ Direct API sync
Habits            ✅ Repeating tasks (repeatAfter)
Routines          ✅ Projects (with metadata marker)
Energy Level      ✅ Labels ("⚡high"/"😐medium"/"🪫low")
Time Estimate     ✅ Description metadata (<!-- tsosu:... -->)
Focus 3           🔶 Local only (resets daily)
Streak tracking   🔶 Local only (HabitCompletion records)
Calendar Event ID 🔶 Local only (per-device)
Daily Focus       🔶 Local only
Nudge settings    🔶 Local only
```

---

## Phase 0：前置準備（1 天）

```bash
# Vikunja API
curl https://your-vikunja.com/api/v1/info
curl https://your-vikunja.com/api/v1/docs.json -o vikunja-openapi.json

# Fastmail CalDAV
curl -u email:app-password -X PROPFIND \
  https://caldav.fastmail.com/dav/principals/user/email@fastmail.com/

# 研究 Vikunja Task model fields
# 重點：repeatAfter, repeatMode, labels, hexColor, description
```

---

## Phase 1：核心 MVP — Tasks + Habits + ADHD（3-4 週）

### Sprint 1.1：Domain Layer（3 天）

```
Claude Code（先讀 claude.md，特別是 Vikunja 欄位映射段落）：

domain/ 純 Kotlin：

Task models:
- Task（含 estimatedMinutes, energyLevel, isFocus）
- EnergyLevel（含 labelTitle 欄位 for Vikunja label sync）
- Priority, Project, Label

★ Habit models — 設計時考慮 Vikunja repeating task 映射:
- Habit（含 serverId: Long? → Vikunja task ID）
- HabitFrequency（含 repeatAfterSeconds → maps to Vikunja repeatAfter）
- HabitCompletion（local streak tracking）
- HabitStreakInfo（completedLast7Days, completionRate）

★ Routine models — 設計時考慮 Vikunja project 映射:
- Routine（含 serverId: Long? → Vikunja project ID）
- RoutineTime（MORNING/AFTERNOON/EVENING）

Focus: DailyFocus, WeeklyReview

Interfaces: TaskRepository, HabitRepository, RoutineRepository,
  FocusRepository, CalendarRepository, SyncRepository, ImportRepository

Use cases: CreateTask, ToggleTaskDone, GetTodayOverview,
  PickOneTask, SetDailyFocus, CreateHabit, CompleteHabit,
  GetTodayHabits, GetRoutine, GetStaleTaskIds, GetWeeklyReview

★ TDD: 每個 use case 先寫 test
```

關鍵 Habit test:

```kotlin
class CompleteHabitUseCaseTest {
    @Test
    fun `records local HabitCompletion for streak tracking`() = runTest {
        val result = useCase("habit-1")
        assertTrue(result.isSuccess)
        coVerify { habitRepo.completeHabit("habit-1", today()) }
    }
}

class CreateHabitUseCaseTest {
    @Test
    fun `sets repeatAfter based on frequency`() = runTest {
        val habit = Habit(title = "Meditate", frequency = HabitFrequency.DAILY)
        val result = useCase(habit)
        assertEquals(86400L, result.getOrThrow().frequency.repeatAfterSeconds)
    }

    @Test
    fun `stores tinyVersion in habit`() = runTest {
        val habit = Habit(title = "Meditate", tinyVersion = "Sit, take 3 breaths")
        val result = useCase(habit)
        assertEquals("Sit, take 3 breaths", result.getOrThrow().tinyVersion)
    }
}
```

### Sprint 1.2：Room DB（2 天）

```
Entities: TaskEntity, HabitEntity (with serverId), HabitCompletionEntity,
  RoutineEntity (with serverId), DailyFocusEntity, ProjectEntity,
  LabelEntity, SyncQueueEntity

★ HabitEntity.serverId → Vikunja task ID (filled after first sync)
★ RoutineEntity.serverId → Vikunja project ID

★ TDD: DAO tests (Robolectric)
```

### Sprint 1.3：Focus 3 + Today View UI（3 天）

```
- 🎯 Focus 3 card at top
- 🔁 Habits summary: "3/5 done"
- Other tasks collapsed
- ⏱ Time total
- Bottom nav: 📥 🎯 🔁 📆 🎲

★ strings.xml: en + zh-TW
```

### Sprint 1.4：Habits Tab + Routines（3 天）

```
- Routines grouped: 🌅 Morning | ☀️ Anytime | 🌙 Evening
- Tap checkbox → complete habit
- Tiny version shown as grey subtitle
- Flexible streak: "5/7 this week"
- Habit detail: dot calendar + "X out of Y" stats

★ Habit creation: "What's the 2-minute version?" prompt
★ Zero "streak broken" language
```

### Sprint 1.5：Quick Add + Energy/Time（2 天）

```
Quick Add: Title + optional Date/Priority/Energy/Time/Label/Project
New Habit quick add: Title + Tiny version + Routine
```

### Sprint 1.6：Pick One 🎲（1 天）

### Sprint 1.7：Inbox + Projects + Search + Widget（2 天）

---

## Phase 2：Vikunja Sync + Calendar + Nudge（3 週）

### Sprint 2.1：Vikunja Task Sync（3 天）

```
Claude Code：

data-vikunja/ sync engine：

1. VikunjaTaskMapper — ★ 核心 mapper，包含：
   a. estimatedMinutes → description metadata 編碼/解碼
   b. energyLevel → label 映射（建立 "⚡high"/"😐medium"/"🪫low"）
   c. isFocus → 不 sync（保留 local 值）
   d. calendarEventId → 不 sync

2. SyncManager:
   - Push: local changes → Vikunja API
   - Pull: Vikunja → local Room
   - 衝突: server wins, preserve local-only fields

3. Energy Label 初始化:
   - 首次 sync 時檢查 Vikunja 是否已有 energy labels
   - 沒有 → POST /api/v1/labels 建立三個
   - 已有 → 記住 label IDs

★ TDD (MockWebServer):
- 上傳 task → description 包含 <!-- tsosu:{"est":30} -->
- 下載 task → 正確解析 estimatedMinutes
- 上傳 task → 自動附加 energy label
- 下載 task → energy label 映射回 EnergyLevel
- Sync 後 isFocus 保持不變（local-only）
```

### Sprint 2.2：Vikunja Habit Sync（3 天）

```
★ 這是新的 sync 邏輯：Habit ↔ Vikunja Repeating Task

HabitSyncManager:

1. Create habit:
   → POST /api/v1/projects/{routineProjectId}/tasks
   → body: repeatAfter=86400, title, description (含 tinyVersion + "— Tsosu Habit")
   → 回填 habit.serverId

2. Complete habit:
   → PUT /api/v1/tasks/{serverId} with done=true
   → Vikunja 自動建下一個 occurrence（repeatAfter 邏輯）
   → 本地記錄 HabitCompletion

3. Create routine:
   → POST /api/v1/projects
   → title: "🌅 Morning Routine"
   → description: <!-- tsosu-routine:MORNING -->
   → 回填 routine.serverId

4. Pull sync:
   → 掃描所有 projects，識別 routine projects（by metadata）
   → 掃描 routine projects 下的 tasks，識別 habits（repeatAfter > 0 + marker）
   → 新的 habit occurrence → 更新本地

5. 識別 Habit vs 普通 Task:
   → Vikunja task 是 Habit 的條件：
     a. repeatAfter > 0
     b. 在 routine project 裡
     c. description 包含 "— Tsosu Habit"
   → 三個條件都滿足才當 Habit 處理

★ TDD:
- 建立 habit → Vikunja 收到 repeatAfter=86400 的 task
- 完成 habit → Vikunja task done=true
- 下一個 occurrence 出現 → 本地更新
- 建立 routine → Vikunja 收到帶 metadata 的 project
- Pull sync 正確識別 habits vs 普通 repeating tasks
```

### Sprint 2.3：CalDAV Calendar Sync（2 天）

```
Task → VEVENT auto-sync
estimatedMinutes → event duration
Habits 不 sync 到 calendar

★ TDD: VEVENT format, duration from estimate
```

### Sprint 2.4：Gentle Nudge + Weekly Review + Stale Cleanup（2 天）

```
Task + Habit nudges (全部 en + zh-TW)
Weekly Review 含 habit stats
Stale cleanup (14+ days)

★ UI copy review: zero shame
```

### Sprint 2.5：Todoist Import（1 天）

```
CSV/JSON import + server-side migration
```

### Sprint 2.6：UI 打磨（2 天）

```
- Sync status indicators (server + calendar)
- Completion animations (task + habit + Focus 3)
- Empty states
- Full en + zh-TW copy review
```

---

## Phase 3：Google Calendar + 上架（1-2 週）

### 3.1：Google Calendar Provider
### 3.2：Google Play 上架

```
Hero message:
"Designed by a psychiatrist with ADHD.
 Built for minds that work differently."

「由精神科醫師設計，為不一樣的腦袋而生。」
```

---

## Phase 4：迭代

- Habit templates（"ADHD Morning Starter Pack"）
- Focus timer
- Kanban view
- Wear OS
- Two-way calendar sync
- More languages

---

## Claude Code 協作流程

```
── Domain ──
1.  「建立 Tsosu (app.tsosu)，先讀 claude.md」
2.  「建立 domain/ task + ADHD models」
3.  「建立 domain/ habit models（注意 serverId, repeatAfterSeconds 欄位）」
4.  「寫 task use case tests，然後實作」
5.  「寫 habit use case tests，然後實作」

── Data Local ──
6.  「寫 Room DAO tests，然後實作」
7.  「寫 Repository tests，然後實作」

── UI ──
8.  「Focus view + Habits tab + Pick One」
9.  「Quick Add + Habit detail (dot calendar, flexible streak)」
10. 「strings.xml en + zh-TW（零羞恥 review）」

── Vikunja Sync ──
11. 「★ 寫 VikunjaTaskMapper tests（metadata encoding）」
12. 「★ 寫 VikunjaHabitMapper tests（repeating task mapping）」
13. 「★ 寫 VikunjaRoutineMapper tests（project metadata）」
14. 「寫 SyncManager tests（MockWebServer），然後實作」
15. 「寫 HabitSyncManager tests，然後實作」
16. 「實作 energy label 初始化邏輯」

── Calendar ──
17. 「CalDavProvider tests → 實作」

── Nudge + Import ──
18. 「GentleNudgeManager（task + habit nudges）」
19. 「TodoistImporter tests → 實作」
```

---

## 時程

| Phase | 內容 | 時間 |
|-------|------|------|
| 0 | 準備 | 1 天 |
| 1 | MVP: Tasks + Habits + ADHD (local) | 3-4 週 |
| 2 | Vikunja Sync + Calendar + Nudge | 3 週 |
| 3 | Google Calendar + 上架 | 1-2 週 |

**Phase 1-2：7 週完成核心（含 Vikunja 同步）。**

---

## 10 Features × Sync Status Checklist

| # | Feature | Domain | Local | Vikunja Sync | UI | Test | Copy |
|---|---------|--------|-------|-------------|-----|------|------|
| 1 | 🎯 Focus 3 | ☐ | ☐ | 🔶 local | ☐ | ☐ | ☐ |
| 2 | 🔁 Habits | ☐ | ☐ | ☐ repeating task | ☐ | ☐ | ☐ |
| 3 | 😌 No Shame | — | — | — | ☐ | ☐ | ☐ |
| 4 | ⏱ Time | ☐ | ☐ | ☐ desc metadata | ☐ | ☐ | — |
| 5 | ⚡ Energy | ☐ | ☐ | ☐ labels | ☐ | ☐ | — |
| 6 | 🎲 Pick One | ☐ | — | — | ☐ | ☐ | — |
| 7 | 🔔 Nudge | ☐ | ☐ | 🔶 local | ☐ | ☐ | ☐ |
| 8 | 🎉 Review | ☐ | ☐ | — | ☐ | ☐ | ☐ |
| 9 | 🧹 Cleanup | ☐ | ☐ | — | ☐ | ☐ | ☐ |
| 10 | 📅 Calendar | ☐ | ☐ | — | ☐ | ☐ | — |

---

## 風險

| 風險 | 對策 |
|------|------|
| Vikunja repeatAfter 邏輯跟 Habit flexible streak 不同 | 完成追蹤 local，Vikunja 只管 next occurrence |
| Description metadata 被使用者不小心刪掉 | Parse 時 graceful fallback，重新 append |
| Energy labels 在 Vikunja 被刪掉 | Sync 時偵測並重新建立 |
| Routine project 被其他 client 修改 | 靠 metadata marker 識別，沒 marker = 不是 routine |
| Habit 和普通 repeating task 分不清 | 三重條件: repeatAfter + routine project + description marker |
