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

1. ~~**vault 變更自動偵測**~~ ✅ 已完成（2026-08-14）：`VaultChangeWatcher`（ContentObserver on tree URI，vault 切換自動重註冊）+ `VaultChangeCoordinator`（2s debounce + AtomicBoolean 防回饋迴圈，4 個測試）。所有同步路徑收斂至單一守衛入口 `syncOnce()`。
2. ~~**增量同步**~~ ✅ 已完成（2026-08-14）：export 端內容等價跳過（note/索引/daily 寫入前比較既有內容）；索引 frontmatter 移除易變 `updated:` 時間戳以確保不變內容可偵測。限制：import 端仍全量讀取（SAF 讀取成本低於解析/寫入，未做 mtime 快取）。
3. ~~**衝突處理**~~ ✅ 已完成（2026-08-14）：`ConflictDetector` 以 last-exported 內容 hash 為基準，兩側皆改 → 保留 vault 版並在 `tasks.md` 標 `<!-- conflict -->`（6 測試）。限制：僅偵測索引行欄位（title/status/dates/reminder/recurrence/energy/estimate/priority），note 描述-only 變更不偵測。
4. ~~**通知排程的持久性**~~ ✅ 已完成：`ReminderScheduler.schedule(Task)` + `rescheduleAll`；BootReceiver 委派重排；`ReminderResync.afterSync()` 同步後重排；新增/編輯/刪除/完成任務即時排程；`OverdueCheckWorker` 先 `syncOnce()` 再讀。
5. ~~**slug 碰撞**~~ ✅ 已完成：note 檔名 `slug-<id前8碼>.md` + 匯入依 id 去重。
6. ~~**大 vault 效能**~~ ✅ 隨 #2 緩解（不變檔案零寫入）。

## 開發計畫（2026-08-14 全部完成）

### Phase A：同步可靠性 ✅

| # | 工作 | 狀態 |
|---|---|---|
| A1 | `ContentObserver` 註冊於 tree URI，變更觸發增量匯入 | ✅ |
| A2 | 增量同步：內容不變即跳過寫入（含索引 frontmatter 時間戳修正） | ✅ |
| A3 | slug 碰撞：`slugify(title)` → `slug-id前8碼.md` + 匯入去重 | ✅ |
| A4 | 衝突標記：同 id 兩側皆改 → 保留 vault 版並標 `<!-- conflict -->` | ✅ |

### Phase B：通知強化 ✅

| # | 工作 | 狀態 |
|---|---|---|
| B1 | 匯入後重排提醒（同步後 + 任務新增/編輯/刪除/完成即時） | ✅ |
| B2 | exact alarm 降級為 inexact + 設定頁權限狀態與跳轉 | ✅ |
| B3 | 逾期通知 tap → deep link 開啟任務（含 reminder 通知修正） | ✅ |
| B4 | `OverdueCheckWorker` 先同步 vault 再讀過期任務 | ✅ |

### Phase C：Obsidian 體驗 ✅

| # | 工作 | 狀態 |
|---|---|---|
| C1 | 索引格式文件化（README「Vault Data Format」章節，en + zh-TW） | ✅ |
| C2 | README：emoji metadata 對照表、檔案結構、Obsidian 建議、Dataview 範例 | ✅ |
| C3 | 設定頁顯示 vault 路徑、檔案數、最後同步時間（同步鈕既有） | ✅ |

### Phase D：發布 ✅

| # | 工作 | 狀態 |
|---|---|---|
| D1 | fastlane 商店描述 + 隱私政策改 markdown-first | ✅ |
| D2 | 移除 `vikunja-openapi.json` 與死字串（`settings_vikunja_server`/`settings_connected`） | ✅ |
| D3 | zh-TW 補齊 75 個缺漏 key（含 plurals），兩語系 key 完全對稱 | ✅ |

### 其他 ✅
- Todoist 匯入手機端流程確認可用（Settings → GetContent 文件選擇器），匯入後自動 `syncOnce()` 寫入 vault。

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
