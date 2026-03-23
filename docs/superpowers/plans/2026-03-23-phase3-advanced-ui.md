# Phase 3: Advanced UI — Calendar, Kanban, Filters, ICS Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Calendar View (month + agenda), Kanban Board with drag-and-drop, advanced filter engine with custom saved lists, and ICS file export. These are the visual/interactive features that bring tsosu to TaskForge-level UX.

**Architecture:** New Compose screens added to navigation. Filter engine is a reusable `FilterSpec` data class used across Kanban, Calendar, and list views. Calendar uses a custom Compose grid. Kanban uses `LazyRow` + `LazyColumn` per column with reorderable modifier. ICS export leverages existing `VEventBuilder` in data-calendar module.

**Tech Stack:** Kotlin 2.1, Jetpack Compose (Material 3), Hilt, ical4j 4.0.7, JUnit 5

**Depends on:** Phase 1 (TaskStatus, extended dates) and Phase 2 (multi-file architecture)

---

## File Map

### Filter Engine (shared across views)
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/FilterSpec.kt`
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/SortSpec.kt`
- Create: `domain/src/main/kotlin/app/tsosu/domain/usecase/FilterTasksUseCase.kt`
- Create: `data-local/src/main/kotlin/app/tsosu/data/local/entity/SavedFilterEntity.kt`
- Create: `data-local/src/main/kotlin/app/tsosu/data/local/dao/SavedFilterDao.kt`

### Calendar View
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/CalendarScreen.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/CalendarViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/MonthGrid.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/AgendaView.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/DayCell.kt`

### Kanban Board
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanScreen.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanColumn.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanCard.kt`

### ICS Export
- Create: `domain/src/main/kotlin/app/tsosu/domain/usecase/ExportIcsUseCase.kt`
- Modify: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/VEventBuilder.kt` — add VALARM, RRULE

### Navigation + DI
- Modify: `app/src/main/java/app/tsosu/navigation/Screen.kt` — add Calendar, Kanban, CustomList screens
- Modify: `app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt` — register new routes

---

## Task 1: Filter Engine — Domain Models

**Files:**
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/FilterSpec.kt`
- Create: `domain/src/main/kotlin/app/tsosu/domain/model/SortSpec.kt`
- Test: `domain/src/test/kotlin/app/tsosu/domain/model/FilterSpecTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
package app.tsosu.domain.model

import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FilterSpecTest {

    private val tasks = listOf(
        Task(id = "1", title = "Urgent work", status = TaskStatus.TODO,
             priority = Priority.URGENT, energyLevel = EnergyLevel.HIGH,
             dueDate = LocalDateTime.parse("2026-03-25T09:00:00"),
             projectId = "proj-1",
             createdAt = Instant.parse("2026-03-20T10:00:00Z"),
             updatedAt = Instant.parse("2026-03-20T10:00:00Z")),
        Task(id = "2", title = "Low energy read", status = TaskStatus.IN_PROGRESS,
             priority = Priority.LOW, energyLevel = EnergyLevel.LOW,
             projectId = "proj-2",
             createdAt = Instant.parse("2026-03-20T10:00:00Z"),
             updatedAt = Instant.parse("2026-03-20T10:00:00Z")),
        Task(id = "3", title = "Done task", status = TaskStatus.DONE,
             priority = Priority.MEDIUM,
             createdAt = Instant.parse("2026-03-20T10:00:00Z"),
             updatedAt = Instant.parse("2026-03-20T10:00:00Z")),
    )

    @Test
    fun `filter by status`() {
        val spec = FilterSpec(statuses = setOf(TaskStatus.TODO, TaskStatus.IN_PROGRESS))
        val result = spec.apply(tasks)
        assertEquals(2, result.size)
        assertTrue(result.none { it.status == TaskStatus.DONE })
    }

    @Test
    fun `filter by priority`() {
        val spec = FilterSpec(minPriority = Priority.MEDIUM)
        val result = spec.apply(tasks)
        assertEquals(2, result.size)  // URGENT + MEDIUM (done)
    }

    @Test
    fun `filter by energy level`() {
        val spec = FilterSpec(energyLevels = setOf(EnergyLevel.HIGH))
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `filter by due date range`() {
        val spec = FilterSpec(
            dueDateFrom = LocalDate.parse("2026-03-24"),
            dueDateTo = LocalDate.parse("2026-03-26"),
        )
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `filter by project`() {
        val spec = FilterSpec(projectIds = setOf("proj-1"))
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
    }

    @Test
    fun `filter by title search`() {
        val spec = FilterSpec(titleContains = "read")
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("2", result[0].id)
    }

    @Test
    fun `combined AND filters`() {
        val spec = FilterSpec(
            statuses = setOf(TaskStatus.TODO),
            energyLevels = setOf(EnergyLevel.HIGH),
        )
        val result = spec.apply(tasks)
        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun `sort by priority descending`() {
        val spec = FilterSpec()
        val sort = SortSpec(field = SortField.PRIORITY, ascending = false)
        val result = sort.apply(spec.apply(tasks))
        assertEquals(Priority.URGENT, result[0].priority)
    }

    @Test
    fun `sort by due date ascending, nulls last`() {
        val sort = SortSpec(field = SortField.DUE_DATE, ascending = true)
        val result = sort.apply(tasks)
        assertEquals("1", result[0].id)  // has due date
        // tasks without due date come after
    }
}
```

- [ ] **Step 2: Run test to fail**

- [ ] **Step 3: Implement**

```kotlin
package app.tsosu.domain.model

import kotlinx.datetime.LocalDate

data class FilterSpec(
    val statuses: Set<TaskStatus>? = null,
    val minPriority: Priority? = null,
    val energyLevels: Set<EnergyLevel>? = null,
    val projectIds: Set<String>? = null,
    val dueDateFrom: LocalDate? = null,
    val dueDateTo: LocalDate? = null,
    val titleContains: String? = null,
    val hasDescription: Boolean? = null,
) {
    fun apply(tasks: List<Task>): List<Task> = tasks.filter { task ->
        (statuses == null || task.status in statuses) &&
        (minPriority == null || task.priority.value >= minPriority.value) &&
        (energyLevels == null || task.energyLevel in energyLevels) &&
        (projectIds == null || task.projectId in projectIds) &&
        (dueDateFrom == null || (task.dueDate != null && task.dueDate.date >= dueDateFrom)) &&
        (dueDateTo == null || (task.dueDate != null && task.dueDate.date <= dueDateTo)) &&
        (titleContains == null || task.title.contains(titleContains, ignoreCase = true)) &&
        (hasDescription == null || (task.description.isNotBlank() == hasDescription))
    }
}

enum class SortField { PRIORITY, DUE_DATE, CREATED, TITLE, ENERGY, STATUS }

data class SortSpec(
    val field: SortField = SortField.DUE_DATE,
    val ascending: Boolean = true,
) {
    fun apply(tasks: List<Task>): List<Task> {
        val comparator: Comparator<Task> = when (field) {
            SortField.PRIORITY -> compareBy { it.priority.value }
            SortField.DUE_DATE -> compareBy(nullsLast()) { it.dueDate }
            SortField.CREATED -> compareBy { it.createdAt }
            SortField.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            SortField.ENERGY -> compareBy { it.energyLevel.ordinal }
            SortField.STATUS -> compareBy { it.status.ordinal }
        }
        return if (ascending) tasks.sortedWith(comparator) else tasks.sortedWith(comparator.reversed())
    }
}
```

- [ ] **Step 4: Test, commit**

```bash
git commit -m "feat(domain): add FilterSpec and SortSpec for advanced task organization"
```

---

## Task 2: Saved Filters (Custom Lists) — Room Persistence

**Files:**
- Create: `data-local/src/main/kotlin/app/tsosu/data/local/entity/SavedFilterEntity.kt`
- Create: `data-local/src/main/kotlin/app/tsosu/data/local/dao/SavedFilterDao.kt`
- Modify: `data-local/src/main/kotlin/app/tsosu/data/local/TsosuDatabase.kt` — add entity + dao + migration

- [ ] **Step 1: Create entity**

```kotlin
@Entity(tableName = "saved_filters")
data class SavedFilterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val filterJson: String,   // serialized FilterSpec
    val sortJson: String,     // serialized SortSpec
    val position: Double = 0.0,
    val createdAt: Long,
)
```

- [ ] **Step 2: Create DAO**

```kotlin
@Dao
interface SavedFilterDao {
    @Query("SELECT * FROM saved_filters ORDER BY position")
    fun getAll(): Flow<List<SavedFilterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(filter: SavedFilterEntity)

    @Query("DELETE FROM saved_filters WHERE id = :id")
    suspend fun delete(id: String)
}
```

- [ ] **Step 3: Add migration (v3→v4)**

```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS saved_filters (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                filterJson TEXT NOT NULL,
                sortJson TEXT NOT NULL,
                position REAL NOT NULL DEFAULT 0.0,
                createdAt INTEGER NOT NULL
            )
        """)
    }
}
```

- [ ] **Step 4: Build, commit**

---

## Task 3: Calendar Screen — Month Grid + Agenda

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/CalendarScreen.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/CalendarViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/MonthGrid.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/AgendaView.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/calendar/DayCell.kt`

- [ ] **Step 1: Create CalendarViewModel**

```kotlin
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate

    val tasksForMonth: StateFlow<Map<LocalDate, List<Task>>> = _currentMonth
        .flatMapLatest { month ->
            val start = month.atDay(1)
            val end = month.atEndOfMonth()
            taskRepository.getTasksBetween(start, end)
        }
        .map { tasks -> tasks.groupBy { it.dueDate?.date } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val selectedDayTasks: StateFlow<List<Task>> = combine(
        selectedDate, tasksForMonth
    ) { date, taskMap ->
        date?.let { taskMap[it] } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun previousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
    fun selectDate(date: LocalDate) { _selectedDate.value = date }
}
```

- [ ] **Step 2: Create MonthGrid composable**

```kotlin
@Composable
fun MonthGrid(
    yearMonth: YearMonth,
    taskCounts: Map<LocalDate, Int>,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
) {
    // 7-column grid: Sun Mon Tue Wed Thu Fri Sat
    // Header row with day names
    // Grid of DayCell composables
    // Highlight today, selected date
    // Show dot indicators for days with tasks
}
```

- [ ] **Step 3: Create AgendaView composable**

```kotlin
@Composable
fun AgendaView(
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onStatusChange: (String, TaskStatus) -> Unit,
) {
    LazyColumn {
        items(tasks) { task ->
            TaskListItem(
                task = task,
                onClick = { onTaskClick(task.id) },
                onStatusChange = { onStatusChange(task.id, it) },
            )
        }
    }
}
```

- [ ] **Step 4: Create CalendarScreen**

```kotlin
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit,
) {
    val month by viewModel.currentMonth.collectAsState()
    val taskMap by viewModel.tasksForMonth.collectAsState()
    val selected by viewModel.selectedDate.collectAsState()
    val dayTasks by viewModel.selectedDayTasks.collectAsState()

    Column {
        // Month navigation: < March 2026 >
        MonthNavigationBar(month, viewModel::previousMonth, viewModel::nextMonth)

        // Month grid
        MonthGrid(
            yearMonth = month,
            taskCounts = taskMap.mapValues { it.value.size },
            selectedDate = selected,
            onDayClick = viewModel::selectDate,
            onDayLongClick = { /* quick create */ },
        )

        Divider()

        // Agenda for selected day
        if (selected != null) {
            AgendaView(dayTasks, onTaskClick, viewModel::setStatus)
        }
    }
}
```

- [ ] **Step 5: Add to navigation**

Add `Calendar` to `Screen` sealed class and `TsosuNavHost`. Add to bottom nav bar.

- [ ] **Step 6: Build and verify**

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(ui): add Calendar screen with month grid and agenda view"
```

---

## Task 4: Kanban Board

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanScreen.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanColumn.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/kanban/KanbanCard.kt`

- [ ] **Step 1: Create KanbanViewModel**

```kotlin
@HiltViewModel
class KanbanViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
) : ViewModel() {

    enum class GroupBy { STATUS, PRIORITY, PROJECT, ENERGY }

    private val _groupBy = MutableStateFlow(GroupBy.STATUS)
    val groupBy: StateFlow<GroupBy> = _groupBy

    val columns: StateFlow<List<KanbanColumnData>> = combine(
        _groupBy, taskRepository.getAllTasks()
    ) { group, tasks ->
        val activeTasks = tasks.filter { !it.status.isTerminal }
        when (group) {
            GroupBy.STATUS -> TaskStatus.entries
                .filter { !it.isTerminal }
                .map { status ->
                    KanbanColumnData(
                        title = status.name.replace("_", " "),
                        tasks = activeTasks.filter { it.status == status },
                    )
                }
            GroupBy.PRIORITY -> Priority.entries.reversed().map { priority ->
                KanbanColumnData(
                    title = priority.name,
                    tasks = activeTasks.filter { it.priority == priority },
                )
            }
            GroupBy.PROJECT -> { /* group by projectId */ }
            GroupBy.ENERGY -> EnergyLevel.entries.map { energy ->
                KanbanColumnData(
                    title = energy.name,
                    tasks = activeTasks.filter { it.energyLevel == energy },
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setGroupBy(group: GroupBy) { _groupBy.value = group }

    fun moveTask(taskId: String, toColumn: String) {
        // Update task status/priority/project based on current groupBy
        viewModelScope.launch { /* ... */ }
    }
}

data class KanbanColumnData(
    val title: String,
    val tasks: List<Task>,
    val isCollapsed: Boolean = false,
)
```

- [ ] **Step 2: Create KanbanColumn composable**

```kotlin
@Composable
fun KanbanColumn(
    data: KanbanColumnData,
    onToggleCollapse: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.width(280.dp).fillMaxHeight()) {
        Column {
            // Header: title + task count + collapse toggle
            Row(
                modifier = Modifier.clickable { onToggleCollapse() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(data.title, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("${data.tasks.size}", style = MaterialTheme.typography.labelSmall)
            }

            if (!data.isCollapsed) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(data.tasks, key = { it.id }) { task ->
                        KanbanCard(task, onClick = { onTaskClick(task.id) })
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create KanbanCard composable**

```kotlin
@Composable
fun KanbanCard(task: Task, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(task.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (task.priority != Priority.NONE) {
                    Text(task.priority.emoji, fontSize = 12.sp)
                }
                task.dueDate?.let {
                    Text("📅 ${it.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                task.estimatedMinutes?.let {
                    Text("🍅 ${it}m", fontSize = 11.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create KanbanScreen**

```kotlin
@Composable
fun KanbanScreen(
    viewModel: KanbanViewModel = hiltViewModel(),
    onTaskClick: (String) -> Unit,
) {
    val groupBy by viewModel.groupBy.collectAsState()
    val columns by viewModel.columns.collectAsState()

    Column {
        // Group-by selector
        ScrollableTabRow(selectedTabIndex = groupBy.ordinal) {
            KanbanViewModel.GroupBy.entries.forEach { option ->
                Tab(
                    selected = groupBy == option,
                    onClick = { viewModel.setGroupBy(option) },
                    text = { Text(option.name) },
                )
            }
        }

        // Kanban board: horizontal scroll of columns
        LazyRow(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(columns) { column ->
                KanbanColumn(
                    data = column,
                    onToggleCollapse = { /* toggle in VM */ },
                    onTaskClick = onTaskClick,
                )
            }
        }
    }
}
```

- [ ] **Step 5: Add to navigation**

- [ ] **Step 6: Build and verify**

- [ ] **Step 7: Commit**

```bash
git commit -m "feat(ui): add Kanban board with group-by status/priority/project/energy"
```

---

## Task 5: ICS Export

**Files:**
- Modify: `data-calendar/src/main/kotlin/app/tsosu/data/calendar/VEventBuilder.kt`
- Create: `domain/src/main/kotlin/app/tsosu/domain/usecase/ExportIcsUseCase.kt`
- Test: `data-calendar/src/test/kotlin/app/tsosu/data/calendar/VEventBuilderTest.kt` (update)

- [ ] **Step 1: Enhance VEventBuilder with VALARM**

```kotlin
fun buildVEvent(
    uid: String,
    title: String,
    description: String,
    dueDate: String,
    estimatedMinutes: Int?,
    reminderMinutesBefore: Int? = null,
): String {
    // ... existing code ...
    // Add VALARM if reminder set:
    if (reminderMinutesBefore != null) {
        appendLine("BEGIN:VALARM")
        appendLine("TRIGGER:-PT${reminderMinutesBefore}M")
        appendLine("ACTION:DISPLAY")
        appendLine("DESCRIPTION:$title")
        appendLine("END:VALARM")
    }
    // ...
}
```

- [ ] **Step 2: Create ExportIcsUseCase**

```kotlin
class ExportIcsUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val vEventBuilder: VEventBuilder,
) {
    suspend fun exportAll(): String {
        val tasks = taskRepository.getAllTasksSync()
            .filter { it.dueDate != null }

        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//Tsosu//NONSGML v1//EN")
            for (task in tasks) {
                appendLine(vEventBuilder.buildVEvent(
                    uid = task.id,
                    title = task.title,
                    description = task.description,
                    dueDate = task.dueDate.toString(),
                    estimatedMinutes = task.estimatedMinutes,
                    reminderMinutesBefore = task.reminderTime?.let { 15 },
                ))
            }
            appendLine("END:VCALENDAR")
        }
    }
}
```

- [ ] **Step 3: Add export button to Calendar screen**

Add share intent: write ICS string to temp file, share via `Intent.ACTION_SEND`.

- [ ] **Step 4: Test, commit**

```bash
git commit -m "feat(calendar): add ICS export with VALARM reminders"
```

---

## Task 6: Advanced Filters UI

**Files:**
- Create: `app/src/main/java/app/tsosu/ui/screens/filter/FilterSheet.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/filter/FilterViewModel.kt`
- Create: `app/src/main/java/app/tsosu/ui/screens/filter/CustomListScreen.kt`

- [ ] **Step 1: Create FilterSheet (bottom sheet for filter configuration)**

```kotlin
@Composable
fun FilterSheet(
    currentFilter: FilterSpec,
    onApply: (FilterSpec) -> Unit,
    onSave: (name: String, FilterSpec) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Filters", style = MaterialTheme.typography.titleLarge)

        // Status filter chips
        Text("Status", style = MaterialTheme.typography.labelMedium)
        FlowRow {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = currentFilter.statuses?.contains(status) ?: false,
                    onClick = { /* toggle */ },
                    label = { Text(status.name) },
                )
            }
        }

        // Priority filter
        Text("Min Priority", style = MaterialTheme.typography.labelMedium)
        // Slider or chips

        // Energy level filter
        Text("Energy", style = MaterialTheme.typography.labelMedium)
        // Chips for LOW, MEDIUM, HIGH

        // Due date range
        Text("Due Date", style = MaterialTheme.typography.labelMedium)
        // DateRangePicker

        // Title search
        OutlinedTextField(value = ..., label = { Text("Search title") })

        // Action buttons
        Row {
            TextButton(onClick = onDismiss) { Text("Cancel") }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { /* save dialog */ }) { Text("Save as List") }
            Button(onClick = { onApply(currentFilter) }) { Text("Apply") }
        }
    }
}
```

- [ ] **Step 2: Create CustomListScreen**

Shows tasks filtered by a saved FilterSpec. Reuses TaskListItem component.

- [ ] **Step 3: Build and verify**

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(ui): add advanced filter sheet and custom saved lists"
```

---

## Task 7: Navigation Update + Bottom Nav Reorganization

**Files:**
- Modify: `app/src/main/java/app/tsosu/navigation/Screen.kt`
- Modify: `app/src/main/java/app/tsosu/navigation/TsosuNavHost.kt`
- Modify: `app/src/main/java/app/tsosu/navigation/BottomNavBar.kt`

- [ ] **Step 1: Add new screens to sealed class**

```kotlin
data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
data object Kanban : Screen("kanban", "Board", Icons.Default.ViewKanban)
data object CustomList : Screen("custom_list/{filterId}", "List", Icons.Default.FilterList)
```

- [ ] **Step 2: Update bottom nav**

New bottom nav: `Focus | Habits | Calendar | Upcoming`

Kanban and CustomList accessible from top bar menu or long-press navigation.

- [ ] **Step 3: Register routes in TsosuNavHost**

- [ ] **Step 4: Build and verify**

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(nav): add Calendar, Kanban, and CustomList to navigation"
```

---

## Task 8: Final Integration + Build Verification

- [ ] **Step 1: Run full build**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew clean assembleDebug --no-daemon`

- [ ] **Step 2: Run all tests**

Run: `ANDROID_HOME=/home/scipio/Android/Sdk ./gradlew test --no-daemon`

- [ ] **Step 3: Commit any fixes**

- [ ] **Step 4: Tag Phase 3 complete**

```bash
git tag phase3-complete
```
