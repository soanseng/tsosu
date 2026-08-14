# Tsosu 開發計畫 — Markdown-First（取代 Todoist）

> tsosu.app — 台語「做事」(tsò-sū)
> ADHD-Friendly 任務管理器：**Markdown 為唯一事實來源**，存於 Obsidian vault（SAF），可在 Obsidian / 任何文字編輯器雙向編輯，App 提供通知與 ADHD 輔助功能。

## 目標與定位

| 面向 | 決定 |
|---|---|
| 資料主體 | Markdown 檔案（vault 根目錄 `tasks.md`/`habits.md` 索引 + `tasks/`、`habits/` 個別 note 資料夾） |
| 存取 | SAF（`ACTION_OPEN_DOCUMENT_TREE` + 持久化 URI），相容 Obsidian vault |
| 取代 | Todoist（透過既有 CSV 匯入一次性遷移） |
| 同步 | App ↔ vault 雙向；外部編輯（Obsidian 桌面同步）優先保留 |
| 通知 | 到期提醒（exact alarm）+ 逾期彙總（WorkManager 週期）+ Gentle Nudge |
| 架構 | Clean Architecture（domain 純 Kotlin；data-markdown 為資料層核心） |

## 現況盤點（2026-08-14 驗證）

**已實作並驗證（119 unit tests 全綠、`assembleDebug` 成功）：**

- `domain/`：Task/Habit/Routine/Focus/WeeklyReview 模型、RecurrenceParser（自然語言週期）、14 個 use case
- `data-markdown/`：
  - `MarkdownTaskParser/Serializer` — 行內 emoji metadata 格式（`📅 2026-08-15`、`⏫` 優先級、`⏰ 09:00` 提醒、`🔁 every monday`、`⚡high` 能量、`🍅 30m` 估時、`<!-- id:... -->`）
  - `TaskNoteSerializer/Parser` — 單一任務 note（含 project 名稱、subtasks）
  - `HabitNoteSerializer/Parser`、`DailyNoteWriter`、`TaskIndexGenerator`、`HabitIndexGenerator`
  - `SafMarkdownFileAccess` — DocumentFile 讀寫、資料夾管理
  - `MarkdownSyncRepository` — Room ↔ markdown 雙向合併（外部編輯優先）
  - `TodoistImportRepository` + `TodoistCsvParser`（含 recurrence parser）
- `app/`：通知全套（`ReminderScheduler` exact alarm、`ReminderReceiver`、`BootReceiver`、`OverdueCheckWorker`、`GentleNudgeManager`、`NotificationHelper`）、設定頁 vault 資料夾選擇（`SettingsViewModel.setFolderUri`）、全部 UI 畫面（Focus 3、Habits、Inbox、Today、Kanban、Pick One、Weekly Review、Search）
- 環境：JDK 17（Temurin，`~/jdks/jdk-17.0.20+8`）、Android SDK 35（`/home/scipio/Android/Sdk`）

**已知瑕疵 / 待辦（依優先序）：**

~~1. **vault 變更自動偵測**~~ ✅ 已完成（2026-08-14）：`VaultChangeWatcher`（ContentObserver on tree URI，vault 切換自動重註冊）+ `VaultChangeCoordinator`（2s debounce + AtomicBoolean 防回饋迴圈，4 個測試）。所有同步路徑（folder picker / resume pull / 設定頁 / watcher）收斂至單一守衛入口 `syncOnce()`。
2. **增量同步** ⏳ 未做：目前每次同步全量重寫所有 note + 索引。下一步以 mtime/內容 hash 只處理變更檔案。
3. **衝突處理** ⏳ 未做：同步為「外部優先」整體覆寫；無 per-task 衝突標記。
4. ~~**通知排程的持久性**~~ ✅ 部分完成：`ReminderScheduler.schedule(Task)` + `rescheduleAll(List<TaskEntity>)`；BootReceiver 委派重排；`ReminderResync.afterSync()` 在每次同步後重排（匯入的新提醒立即生效）；新增/編輯/刪除/完成任務時即時排程或取消。剩餘：`OverdueCheckWorker` 仍讀 Room DAO（Phase B4）。
5. ~~**slug 碰撞**~~ ✅ 已完成：note 檔名改 `slug-<id前8碼>.md`，匯入端依 id 去重（相容既有舊檔名），索引 wikilink 同步更新。
6. **大 vault 效能** — 同 #2 增量同步。

## 開發計畫（後續迭代）

### Phase A：同步可靠性（優先）

| # | 工作 | 狀態 |
|---|---|---|
| A1 | `ContentObserver` 註冊於 tree URI，變更觸發增量匯入 | ✅ 2026-08-14 |
| A2 | 增量同步：以 mtime/內容 hash 只處理變更檔案（`content-hash-cache-pattern`） | ⏳ 下一步 |
| A3 | slug 碰撞：`slugify(title)` 改 `slug-id前8碼.md`，讀取端兩者皆認 | ✅ 2026-08-14 |
| A4 | 衝突標記：同 id 兩側皆改 → 保留 markdown 版並在 `tasks.md` 以 `<!-- conflict -->` 標記 | ⏳ |

### Phase B：通知強化

| # | 工作 | 狀態 |
|---|---|---|
| B1 | 匯入後重排：`MarkdownSyncRepository.sync()` 完成後呼叫 `ReminderScheduler.rescheduleAll()` | ✅ 2026-08-14（同步後 + 任務新增/編輯/刪除/完成即時排程） |
| B2 | exact alarm 不可用時降級：`setAndAllowWhileIdle`（非 exact）並記錄；設定頁顯示權限狀態 + 跳轉 | ⏳ |
| B3 | 逾期通知 tap → deep link 至該任務（現為彙總通知，需帶 taskId list + navigation） | ⏳ |
| B4 | `OverdueCheckWorker` 改讀 markdown repository（現在讀 Room DAO，跨 vault 不一致風險） | ⏳ |

### Phase C：Obsidian 體驗

| # | 工作 | 驗收 |
|---|---|---|
| C1 | 匯入後自動產生 `📅 Tasks.md` 風格索引（資料夾 note + Dataview 相容格式）或維持現行索引格式並寫文件 | 文件說明 vault 內可讀/可改結構 |
| C2 | README 增加「vault 資料格式」章節：emoji metadata 對照表、範例檔案、Obsidian 設定建議（排除 `.obsidian/` 不寫入） | 使用者照文件可手動新增任務 |
| C3 | 設定頁：顯示目前 vault 路徑/檔案數、最後同步時間、一鍵同步按鈕 | 手動驗證 |

### Phase D：發布

| # | 工作 | 驗收 |
|---|---|---|
| D1 | `fastlane` metadata 更新（描述改 markdown-first） | fastlane 驗證通過 |
| D2 | 移除/停用未使用的 Vikunja 相關程式碼（如存在）與 `vikunja-openapi.json` 引用 | `./gradlew assembleDebug` 綠 |
| D3 | zh-TW/en 字串完整審查 | 兩語系 build 綠 |

## 建置與驗證指令

```bash
export JAVA_HOME=/home/scipio/jdks/jdk-17.0.20+8
export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug                 # 主建置
./gradlew testDebugUnitTest             # 全部 unit tests（domain + data-markdown + data-calendar）
./gradlew :data-markdown:testDebugUnitTest  # 僅 markdown 模組
```

## 風險登錄

| 風險 | 緩解 |
|---|---|
| SAF 大目錄列舉慢 | Phase A2 增量；避免 `listFiles()` 全掃 |
| Obsidian 同步（iCloud/Drive）與 App 併發寫入 | A4 衝突標記 + 寫前重讀 |
| OEM 電池限制殺 alarm/worker | 文件說明白名單；B2 降級 |
| `content://` 非真實路徑，禁 `File()` 假設 | 全數經 DocumentFile/contentResolver |
| frontmatter 未知 key | parser 對未知欄位保留原樣，round-trip 不丟資料 |
