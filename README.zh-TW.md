# Tsosu 做事

> 台語「做事」(tsò-sū) — 由精神科醫師設計的 ADHD 友善任務管理工具。

[🌐 官網](https://soanseng.github.io/tsosu/) · [English](README.md)

**Tsosu** 是一款原生 Android 任務管理 app，專為不同思維模式的大腦打造。所有資料以純 markdown 檔案儲存——透過 Syncthing 同步到電腦，用 Obsidian、nvim 或任何文字編輯器管理。不需要伺服器。

## 為什麼選 Tsosu？

大部分任務管理工具都是為神經典型的大腦設計的。它們用逾期數字懲罰你、用太多選項淹沒你、在你落後時讓你充滿罪惡感。

Tsosu 不一樣。它建立在對 ADHD 的臨床理解與《原子習慣》的原則上——**做小一點、做簡單一點、慶祝進步。**

## 安裝

### 透過 Obtainium（推薦）

[Obtainium](https://github.com/ImranR98/Obtainium) 會直接追蹤本專案的 GitHub Releases——安裝後有新版本會自動通知更新。

1. 安裝 Obtainium（Google Play 或 F-Droid）
2. 開啟 Obtainium，點右下角 **＋** 新增應用程式
3. App 來源選 **GitHub**，輸入 `soanseng/tsosu`
4. 點**新增**——自動下載最新版 APK，之後新版本自動提醒

### 直接下載

不需要 Obtainium 的話，直接到 [Releases 頁面](https://github.com/soanseng/tsosu/releases) 下載最新的 `.apk` 檔案安裝。Android 會詢問「安裝未知應用程式」權限，允許即可。

## 功能

### 🎯 Focus 3 —「挑三件就好。」

ADHD 大腦面對 30 件任務時會當機。Tsosu 只請你挑今天的 3 件事。其他的收起來——還在，但不會對你大叫。

- 每天早上（或前一晚）挑選 3 件重點
- 重點任務置頂，未排期任務**可收合**，附數量徽章
- 完成 3 件 → 🎉 慶祝動畫
- 「其他都是加分」——因為真的是這樣
- **右滑**標記完成、**左滑**推遲到明天——不用找小小的按鈕
- 逾期任務有**紅色逾期標籤**提醒你
- 底部導航顯示**待辦數量**，一眼看到還有多少事

### 🔁 每日習慣 —「做小一點，做簡單一點。」

受《原子習慣》啟發：從小到不能拒絕的事開始。Tsosu 的習慣追蹤專為難以維持一致性的 ADHD 大腦設計。

- **2 分鐘習慣**：「冥想」→「坐下來深呼吸 3 次」。小到一定做得到。
- **彈性連續記錄**：漏了一天？你的紀錄暫停，不是歸零。🔥 連續天數顯示你的動力。
- **7 天進度條**：每個習慣顯示過去 7 天的視覺化進度——看趨勢，不只看數字。
- **早晨、隨時 & 晚間例行**：依時段分組習慣。在 FAB 直接新增習慣。
- **溫和的督促**：每個習慣都有每週計數（`3/5`）與連續記錄（`🔥7`）——顯示進度，不數漏掉的天數

### 😌 無羞恥 UI —「還在你的清單上。」

每個像素都經過羞恥感與焦慮觸發因素的審查。

- 逾期任務只帶一個小小的「Overdue」標籤——沒有紅色計數徽章大聲告訴你有多少件逾期
- 沒有懲罰中斷的連續記錄——連續記錄暫停，不是歸零
- 每週回顧只顯示你**完成了什麼**，不提你沒做的

### ⏱ 時間感知 —「今天排太多了。」

ADHD 常伴隨時間盲。Tsosu 讓時間看得見，但不說教。

- 預估每項任務耗時——任務卡片上有 🍅 標記
- 「幫我挑一個」與日曆都會用到你的預估
- 日曆事件以時間預估作為事件長度（預設 60 分鐘）

### ⚡ 能量匹配 —「配合你的能量。」

不是每個小時都一樣。依據任務需要的能量來標記，然後配合你當下的狀態。

- 🔋 高能量 — 深度工作、困難對話、複雜任務
- 😐 中能量 — 日常工作、回信、跑腿
- 🪫 低能量 — 整理、簡單行政、輕鬆的小事

### 🎲「幫我挑一個」—「隨便開始就好。」

選擇困難是真的。當你不知道要做什麼，按骰子。

- 選擇你目前的能量等級
- Tsosu 隨機挑一個匹配的任務
- 「就做這個 ✓」——不再分析癱瘓
- 不喜歡？「再挑一個 🔄」

### ⏰ 任務提醒 —「在你設定的時間。」

提醒跟著任務走——設定一次，到期日準時響起，完成後自動消失。

- 每任務提醒時間——在到期日 ⏰ 觸發通知
- 點通知直接開啟任務；也能從通知列直接標記完成
- 逾期彙總通知——「3 個逾期任務」，點開第一個
- 精確鬧鐘（Android 12+ 權限詢問）自動降級為不精確鬧鐘
- 重新開機與每次 vault 同步後自動重排提醒

### 🎉 每週回顧 —「看看你做了什麼！」

週末 = 慶祝，不是成績單。

- 「這週你完成了 15 件任務！」
- 「習慣：35 次中完成 28 次——80%！」
- Focus 天數：4/7 ⭐
- 最活躍的專案
- 「進步，不是完美。」
- 完全不提還剩什麼沒做

### 📅 日曆自動同步 —「設個日期，到處都看得到。」

有日期的任務自動出現在你的日曆上。不用重複輸入。

- 設定到期日 → 自動建立日曆事件
- 改日期 → 事件跟著移動
- 完成任務 → 事件移除
- 時間預估 → 日曆事件長度
- 支援 **CalDAV**（Fastmail、Nextcloud）和 **Google Calendar**

### 📥 Todoist 匯入 —「把你的清單帶過來。」

在設定頁匯入 Todoist 的 CSV 匯出檔——依 id 去重、轉成 markdown、自動寫入你的 vault。

### 📝 Markdown Vault —「你的任務就是你的檔案。」

所有資料以純 markdown 檔案存在你擁有的資料夾裡——用 Obsidian、nvim 或任何文字編輯器在任意裝置上檢視與編輯。詳見[怎麼同步](#怎麼同步)。

## 怎麼同步

Tsosu 是 **local-first**——100% 離線運作，不需要帳號。你的資料以純 markdown 檔案存在，完全屬於你。

### Markdown 檔案

在 Tsosu 裡指向手機上的任何資料夾（Obsidian vault 最理想）。它會寫入一組 markdown 檔案（單一任務/習慣 note 與每日 note 詳見下方「Vault 資料格式」）：

**`tasks.md`** — 你的任務，按專案分組：
```markdown
---
tsosu: v1
generated: true
---

## 收件匣

- [ ] 買菜 ⚡medium 🍅15m <!-- id:abc-123 -->
- [x] 打給牙醫 ✅ 2026-03-22 ⚡low <!-- id:def-456 -->

## 工作

- [ ] 準備簡報 📅 2026-03-25 ⚡high 🍅60m ⏫ <!-- id:ghi-789 -->
```

**`habits.md`** — 你的習慣，每習慣一行，依例行時段分組，附每週計數與連續記錄：
```markdown
---
tsosu: v1
generated: true
---

## Daily

- [ ] 運動 (tiny: 做 1 個伏地挺身) 🔁7x/week ⚡medium 3/5 🔥7 [[habits/exercise-h1abcdef]] <!-- id:h1 -->
```

### 跨裝置同步

用 [Syncthing](https://syncthing.net/) 或 Obsidian Sync 同步資料夾——你的任務出現在每台裝置上。

| 在哪裡 | 怎麼用 |
|--------|--------|
| **手機** | Tsosu app（豐富 UI、習慣追蹤、Focus 3） |
| **桌面** | Obsidian 搭配 [Tasks 外掛](https://publish.obsidian.md/tasks/)——勾選任務、到期日、篩選 |
| **終端** | nvim / 任何文字編輯器——就是 markdown |

靈感來自 [org-mode](https://doc.norang.ca/org-mode.html)：文字檔是萬用介面。

## Vault 資料格式

Tsosu 讀寫一組固定、有文件的 markdown 檔案。全部都是純文字——你可以在 Obsidian 或任何編輯器手動修改，下次同步時 Tsosu 會吃進來。

### 檔案結構

```
<你的 vault 資料夾>/
├── tasks.md              # 任務索引——每任務一行 checkbox，依專案分組
├── habits.md             # 習慣索引——每習慣一行，完成紀錄縮排在下方
├── tasks/
│   └── <slug>-<id8>.md   # 單一任務 note（有描述或子任務的任務）
├── habits/
│   └── <slug>-<id8>.md   # 單一習慣 note（完整完成歷史）
└── daily/
    └── YYYY-MM-DD.md     # 每日 note——當天習慣與勾選狀態
```

### 任務行格式（`tasks.md`）

每任務一行 checkbox，中繼資料用 emoji 標記，機器可讀的 id 藏在 HTML 註解裡。**不要刪改 `<!-- id:... -->` 註解**——Tsosu 靠它辨識任務。

```markdown
- [ ] Prepare presentation 📅 2026-03-25 ⏰ 09:00 ⚡high 🍅60m ⏫ <!-- id:ghi-789 -->
```

| 標記 | 意義 | 範例 |
|------|------|------|
| `[ ]` `[/]` `[!]` `[>]` `[x]` `[-]` | 狀態：待辦/進行中/暫緩/已排程/完成/取消 | `[x]` |
| `✅ YYYY-MM-DD` | 完成日期（已完成任務） | `✅ 2026-03-22` |
| `❌ YYYY-MM-DD` | 取消日期（已取消任務） | `❌ 2026-03-22` |
| `📅 YYYY-MM-DD` | 到期日 | `📅 2026-03-25` |
| `⏳ YYYY-MM-DD` | 排程日 | `⏳ 2026-03-24` |
| `🛫 YYYY-MM-DD` | 開始日 | `🛫 2026-03-23` |
| `➕ YYYY-MM-DD` | 建立日期 | `➕ 2026-03-20` |
| `⏰ HH:MM` | 提醒時間（於到期日當天） | `⏰ 09:00` |
| `🔁 <規則>` | 週期規則（自然語言） | `🔁 every monday` |
| `⚡high` `😐medium` `🪫low` | 能量等級 | `⚡high` |
| `🍅 Nm` | 預估分鐘數 | `🍅60m` |
| `⏫` `🔺` `🔼` `🔽` | 優先級：急/高/中/低 | `⏫` |
| `<!-- id:... -->` | 穩定任務 id——**勿編輯** | `<!-- id:ghi-789 -->` |
| `<!-- conflict -->` | 兩端都改過；保留 vault 版本 | |

專案用 `## 標題` 分區。第一個區塊（或無標題）以下是 Inbox。有描述或子任務的任務會額外在 `tasks/` 產生 note 檔，索引以 wikilink `[[tasks/...]]` 連結。

### 習慣行格式（`habits.md`）

```markdown
## Daily

- [ ] 運動 (tiny: 做 1 個伏地挺身) 🔁7x/week ⚡medium 3/5 🔥7 [[habits/exercise-h1abcdef]] <!-- id:h1 -->
```

| 標記 | 意義 | 範例 |
|------|------|------|
| `🔁 Nx/week` | 每週目標次數 | `🔁7x/week` |
| `3/5` | 本週完成次數 / 目標 | `3/5` |
| `🔥N` | 目前連續天數 | `🔥7` |
| `[[habits/...]]` | 連結到完整歷史 note | `[[habits/exercise-h1abcdef]]` |

索引行只帶上面的計數。完成日期存放在 `habits/` 下的習慣 note 與每日 note 裡。

### Obsidian 建議

- Tsosu 不會寫入 `.obsidian/`、templates 或 attachments——整個 vault 同步都安全。
- 索引檔是標準 checkbox 語法，[Tasks 外掛](https://publish.obsidian.md/tasks/) 篩選與 [Dataview](https://blacksmithgu.github.io/obsidian-dataview/) 查詢可直接用：

````markdown
```dataview
TASK FROM "tasks.md"
WHERE !completed AND contains(text, "📅")
```
````

- 手動新增任務：複製任一行，改標題與日期，換一個新 id（`<!-- id:my-new-id -->`）。Tsosu 下次同步會保留未知 id。
- 若任務在 App 與 vault 兩端都被修改：保留 vault 版本，並在該行加上 `<!-- conflict -->` 讓你知道需要人工確認。

## 技術細節

- **Android 原生** — Kotlin、Jetpack Compose、Material 3
- **Local-first** — 100% 離線運作，不需要帳號
- **Markdown 同步** — 純 `.md` 檔案，相容 Obsidian Tasks
- **日曆同步** — CalDAV（Fastmail、Nextcloud）、Google Calendar
- **Todoist 匯入** — 帶入你現有的任務
- **隱私** — 無分析、無追蹤、無資料收集、無伺服器
- **架構** — MVVM、Clean Architecture、TDD
- **多語系** — English、繁體中文

## 誰做的

Tsosu 由一位**精神科專科醫師**設計，他本身也有 ADHD。每個設計決策都來自臨床專業知識與個人經歷——執行功能、時間盲、決策疲勞的日常挑戰。

這不是一個碰巧有一些 ADHD 功能的生產力 app。ADHD 友善設計就是產品本身。

## 授權

Tsosu 為專有軟體。

---

*tsosu.app — 做事，用你的方式。*
