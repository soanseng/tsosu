# Tsosu 做事

> 台語「做事」(tsò-sū) — 由精神科醫師設計的 ADHD 友善任務管理工具。

[🌐 官網](https://soanseng.github.io/tsosu/) · [English](README.md)

**Tsosu** 是一款原生 Android 任務管理 app，專為不同思維模式的大腦打造。所有資料以 [Obsidian Tasks](https://publish.obsidian.md/tasks/Introduction) 格式的純 markdown 檔案儲存——手機上記錄，回到電腦用 Obsidian、nvim 或任何文字編輯器打開同一個 vault。不需要伺服器。

<p>
<img src="docs/screenshots/inbox.png" width="240" alt="收件匣" />
<img src="docs/screenshots/today.png" width="240" alt="今天" />
<img src="docs/screenshots/habits.png" width="240" alt="習慣" />
</p>
<p>
<img src="docs/screenshots/recurrence-builder.png" width="240" alt="自訂重複 builder" />
<img src="docs/screenshots/detail-history.png" width="240" alt="完成紀錄" />
<img src="docs/screenshots/gamification-help.png" width="240" alt="能量與護盾說明" />
</p>

## 為什麼選 Tsosu？

大部分任務管理工具都是為神經典型的大腦設計的。它們用逾期數字懲罰你、用太多選項淹沒你、在你落後時讓你充滿罪惡感。

Tsosu 不一樣。它建立在對 ADHD 的臨床理解與《原子習慣》的原則上——**做小一點、做簡單一點、慶祝進步。** 四個分頁、零層疊選單，遊戲化機制獎勵你的出現，而不是羞辱你的缺席。

## 1.3 新功能

- **🗂 更安靜的首頁**——收件匣是起點；app 就是四個分頁（收件匣／今天／習慣／即將到來）加收進頂欄選單的日曆。番茄鐘、看板、每週回顧、儲存檢視已移除——刻意的。
- **🤝 Obsidian Tasks 相容**——`tasks.md` 每行改用該插件的原生 emoji 格式寫入與解析（`🆔` id、`🔁 every week on Tuesday`、`⛔` 依賴、完整日期組）。在 Obsidian 勾一個循環任務，下次同步 Tsosu 會把完成行摺進連續紀錄；反向亦然。
- **☑️ 勾選式重複 builder**——「自訂」重複改成晶片：每 N 天／週／月、星期幾多選、每月幾號。自然語言（`every 2 days`、`每週一二三`）依然可用。
- **📜 完成紀錄**——打開任務就能看到完成了幾次、分別在哪一天（超過 5 筆自動收合）。
- **❓ 遊戲化說明**——頂欄的 ⚡ 可以點、習慣頁有 ⓘ、設定也有一條；三個入口講清楚能量、護盾、連續紀錄怎麼運作。
- **⛔ 依賴與 ⏳/🛫 日期**——Obsidian 的 dependsOn、排程與開始日期都能讀取、儲存、顯示。

## 安裝

### 透過 Obtainium（推薦）

[Obtainium](https://github.com/ImranR98/Obtainium) 會直接追蹤本專案的 GitHub Releases——安裝後有新版本會自動通知更新。

1. 安裝 Obtainium（Google Play 或 F-Droid）
2. 開啟 Obtainium，點右下角 **＋** 新增應用程式
3. App 來源選 **GitHub**，輸入 `soanseng/tsosu`
4. 點**新增**——自動下載最新版 APK

### 直接下載

到 [Releases 頁面](https://github.com/soanseng/tsosu/releases) 下載最新的 `.apk` 安裝。Android 會詢問「安裝未知應用程式」權限，允許即可。

## App 內容

### 📥 收件匣 → 📅 今天 → 🔁 習慣 → 🗓 即將到來

四個分頁，一個心智模型：

- **收件匣**——剛捕捉、還沒排期的所有任務。清空它，不用怕它。
- **今天**——「逾期」與「今天」兩個溫和的區塊。今天到期的任務永遠不會被標「逾期」。
- **習慣**——所有循環任務，依 🌅 早晨／☀️ 隨時／🌙 晚間分組，帶 🔥 連續天數。**Tsosu 的習慣就是一條有重複規則的任務**——單一資料模型，沒有另外一套習慣資料庫。
- **即將到來**——之後的排程。日曆收在頂欄 ⌄ 選單。

### ⚡ 能量、❄ 護盾、🔥 連續——有動力，沒有罪惡感

- 每完成一個任務 **+2⚡**。一個 **❄ 護盾** ⚡30（最多囤 2 個）。
- 漏了一天？3 天內再次完成，自動消耗一個 ❄ 補上缺口——🔥 不中斷。
- 點頂欄的 ⚡（或習慣頁的 ⓘ、設定裡的說明）就能在 app 內看到這段說明。
- 護盾補上的日子也算進連續。沒有紅色警告，沒有羞辱計數。

### ✍️ 速記語法——「怎麼說就怎麼打。」

- **重複**：`every day`、`weekly`、`every mon, wed, fri`、`每 2 天`、`每週一二三`——或在「自訂」選擇器裡用晶片組出來
- **時段**：`every morning` / `every afternoon` / `every evening`——重複加預設提醒（08:00 / 13:00 / 18:00 / 21:00）
- **範圍**：`starting 8/20 until 8/31`；優先級 `p1`–`p4`；`@專案` 歸檔；`due:今天` / `due:8/31`
- 同樣的關鍵字在 vault 也通：在 Obsidian 寫 `Buy milk every other week`，下次同步就把規則帶進 app

### ⏰ 提醒與摘要

每任務提醒時間、通知上稍後 10 分鐘、早晚溫和摘要、重新開機與時區變更後提醒不會丟。

### 🧰 值得留下的設定

JSON 備份與還原、ICS 行事曆訂閱（唯讀疊加）與匯出、Todoist 與 TickTick CSV 匯入、生物辨識 App 鎖、English／繁體中文、Material You 動態配色。

## Markdown Vault——Obsidian Tasks 格式

把 Tsosu 指向任何資料夾（Obsidian vault 最理想）。它會寫：

```
<你的 vault>/
├── tasks.md              # 每個任務一行核取框，按專案分節
├── tasks/
│   └── <slug>-<id8>.md   # 有描述的任務另有單獨筆記
└── daily/
    └── YYYY-MM-DD.md     # 每日筆記——今天的習慣清單
```

任務行採用 [Obsidian Tasks](https://publish.obsidian.md/tasks/Introduction) 預設 emoji 格式，欄位順序與該插件一致：

```markdown
- [ ] Prepare presentation 🆔 ghi-789 ⏫ 🔁 every week ➕ 2026-09-01 🛫 2026-09-02 ⏳ 2026-09-03 📅 2026-09-04
- [x] Call dentist 🆔 def-456 ➕ 2026-03-20 📅 2026-03-22 ✅ 2026-03-22
```

| 標記 | 意義 |
|------|------|
| `- [ ]` `[/]` `[!]` `[>]` `[x]` `[-]` | 待辦／進行中／暫停／計畫／完成／取消 |
| `🆔 id` | 任務穩定 id（也相容舊版 `<!-- id:... -->`） |
| `⛔ id1,id2` | 等待其他任務 |
| `🔺 ⏫ 🔼 🔽 ⏬` | 優先級 |
| `🔁 every …` | 重複規則，rrule.js 英文——`every day`、`every 2 weeks on Monday, Friday`、`every month on the 15th` |
| `➕ 🛫 ⏳ 📅 ❌ ✅` | 建立／開始／排程／到期／取消／完成日期 |
| `[[tasks/slug]]` | 連到任務筆記（有描述的任務） |
| `<!-- conflict -->` | 兩側都改過；以 vault 版本為準 |

Tsosu 專屬欄位（⏰ 提醒、精力、🍅 估時、迷你版習慣、完整完成歷史）不寫在行上——存在 SQLite 與任務筆記的 YAML，讓每一行對 Obsidian 都是原生格式。

### 兩邊都能勾循環任務

Obsidian 的模型會把一次完成拆成 `[x] … ✅ 日期` 加一條新的 `[ ]` 行；Tsosu 的模型是單行帶完成歷史。兩邊都處理了：

- **在 Obsidian 勾**：留下完成行和新行即可——下次同步 Tsosu 依 `🆔` 摺疊：`[ ]` 行成為下一次到期，每個 `✅` 日期成為歷史裡的一次完成。
- **在 Tsosu 勾**：索引會在現行行上方保留每個系列最近 10 次完成的 `[x]` 歷史行，Tsosu 重寫檔案不會抹掉你在 Obsidian 端的歷史。
- 完整歷史（不限 10 筆）在任務筆記 YAML 與 app 內。

### 跨裝置

| 裝置 | 用法 |
|------|------|
| **手機** | Tsosu——捕捉、習慣、連續紀錄、提醒 |
| **電腦** | Obsidian + [Tasks 插件](https://publish.obsidian.md/tasks/)——查詢、編輯 |

````markdown
```tasks
not done
path does not include daily
group by due
```
````

資料夾用 [Syncthing](https://syncthing.net/) 或 Obsidian Sync 同步。Vault 的修改會自動偵測——沒有手动同步按鈕。Tsosu 絕不寫入 `.obsidian/`。

## 技術細節

- **Android 原生**——Kotlin、Jetpack Compose、Material 3
- **本地優先**——100% 離線可用，無帳號、無分析、無追蹤
- **Markdown 同步**——Obsidian Tasks 相容純 `.md`
- **行事曆**——裝置日曆（Google、透過同步用戶端的 CalDAV）、ICS 訂閱
- **在地化**——English、繁體中文
- **架構**——MVVM、Clean Architecture、TDD

## 誰做的？

Tsosu 由一位**精神科專科醫師**設計，他本身也與 ADHD 共處。每個設計決策都來自臨床專業，加上對執行功能、時間盲視與決策疲勞的親身體會。

這不是一款「碰巧有些 ADHD 功能」的生產力 app。ADHD 友善設計本身就是產品。

## 授權

Tsosu 為專有應用程式。

---

*tsosu.app — 做事，用你的方式。*
