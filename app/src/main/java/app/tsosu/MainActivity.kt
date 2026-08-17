package app.tsosu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tsosu.R
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.navigation.BottomNavBar
import app.tsosu.navigation.Screen
import app.tsosu.navigation.TsosuNavHost
import app.tsosu.ui.screens.filter.FilterSheet
import app.tsosu.ui.screens.focus.FocusViewModel
import app.tsosu.ui.screens.habitdetail.HabitDetailSheet
import app.tsosu.ui.screens.habits.HabitsViewModel
import app.tsosu.ui.screens.pickone.PickOneSheet
import app.tsosu.ui.screens.quickadd.QuickAddHabitSheet
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.screens.quickadd.QuickAddViewModel
import app.tsosu.ui.screens.taskdetail.TaskDetailSheet
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.theme.TsosuTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.repository.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var markdownPreferences: MarkdownPreferences
    @Inject lateinit var vaultChangeWatcher: VaultChangeWatcher
    @Inject lateinit var gamificationRepository: GamificationRepository
    @Inject lateinit var savedViewPreferences: app.tsosu.ui.screens.filter.SavedViewPreferences
    @Inject lateinit var projectRepository: app.tsosu.domain.repository.ProjectRepository

    private var lastSyncTime = 0L
    private val snackbarHostState = SnackbarHostState()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setupResumePull()
        setContent {
            val dynamicColor by themePreferences.dynamicColor.collectAsState(initial = false)
            val darkModeOption by themePreferences.darkMode.collectAsState(initial = DarkModeOption.SYSTEM)
            val darkTheme = when (darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.LIGHT -> false
                DarkModeOption.DARK -> true
            }

            TsosuTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                var showAddTask by remember { mutableStateOf(false) }
                var quickAddInitialDate by remember { mutableStateOf<kotlinx.datetime.LocalDateTime?>(null) }
                var showAddHabit by remember { mutableStateOf(false) }
                var showPickOne by remember { mutableStateOf(false) }
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { }
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                var showFilter by remember { mutableStateOf(false) }
                var editingTaskId by remember {
                    mutableStateOf(intent.getStringExtra("taskId") ?: null)
                }
                var editingHabitId by remember {
                    mutableStateOf(intent.getStringExtra("habitId") ?: null)
                }
                val focusViewModel: FocusViewModel = hiltViewModel()
                val habitsViewModel: HabitsViewModel = hiltViewModel()
                val energy by gamificationRepository.energy().collectAsState(initial = 0)
                val isVaultConfigured by syncRepository.isConfigured()
                    .collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val isOnHabitsTab = currentRoute == Screen.Habits.route

                val folderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    uri ?: return@rememberLauncherForActivityResult
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    scope.launch {
                        markdownPreferences.setFolderUri(uri)
                        vaultChangeWatcher.syncOnce()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        val focusState by focusViewModel.uiState.collectAsState()
                        var showViewMenu by remember { mutableStateOf(false) }
                        val currentView = Screen.viewModes.firstOrNull { it.route == currentRoute }
                            ?: Screen.Focus
                        TopAppBar(
                            title = {
                                TextButton(onClick = { showViewMenu = true }) {
                                    Icon(currentView.icon, contentDescription = null)
                                    Text(
                                        when (currentView) {
                                            Screen.Focus -> stringResource(R.string.view_focus)
                                            Screen.Inbox -> stringResource(R.string.view_inbox)
                                            Screen.Kanban -> stringResource(R.string.view_kanban)
                                            Screen.Calendar -> stringResource(R.string.view_calendar)
                                            Screen.Upcoming -> stringResource(R.string.view_upcoming)
                                            else -> stringResource(R.string.app_name)
                                        },
                                    )
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = stringResource(R.string.view_switcher),
                                    )
                                }
                                DropdownMenu(
                                    expanded = showViewMenu,
                                    onDismissRequest = { showViewMenu = false },
                                ) {
                                    Screen.viewModes.forEach { screen ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    when (screen) {
                                                        Screen.Focus -> stringResource(R.string.view_focus)
                                                        Screen.Inbox -> stringResource(R.string.view_inbox)
                                                        Screen.Kanban -> stringResource(R.string.view_kanban)
                                                        Screen.Calendar -> stringResource(R.string.view_calendar)
                                                        Screen.Upcoming -> stringResource(R.string.view_upcoming)
                                                        else -> screen.title
                                                    },
                                                )
                                            },
                                            leadingIcon = { Icon(screen.icon, contentDescription = null) },
                                            onClick = {
                                                showViewMenu = false
                                                if (currentRoute != screen.route) {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            },
                            actions = {
                                Text(
                                    "⚡$energy",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                IconButton(onClick = { showFilter = true }) {
                                    Icon(
                                        imageVector = if (focusState.isFiltered) {
                                            Icons.Default.FilterAltOff
                                        } else {
                                            Icons.Default.FilterAlt
                                        },
                                        contentDescription = "Filter",
                                    )
                                }
                                IconButton(onClick = {
                                    navController.navigate(Screen.Settings.route)
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                        )
                    },
                    bottomBar = {
                        val focusState by focusViewModel.uiState.collectAsState()
                        val habitsState by habitsViewModel.uiState.collectAsState()
                        BottomNavBar(
                            navController = navController,
                            focusPendingCount = focusState.totalCount - focusState.completedCount,
                            habitsPendingCount = habitsState.totalCount - habitsState.completedCount,
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                if (isOnHabitsTab) showAddHabit = true
                                else {
                                    quickAddInitialDate = null
                                    showAddTask = true
                                }
                            },
                            modifier = Modifier.pointerInput(isOnHabitsTab) {
                                detectTapGestures(
                                    onLongPress = {
                                        if (!isOnHabitsTab) showPickOne = true
                                    },
                                )
                            },
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = if (isOnHabitsTab) {
                                    stringResource(R.string.quick_add_habit_title)
                                } else {
                                    stringResource(R.string.quick_add_task_title)
                                },
                            )
                        }
                    },
                ) { innerPadding ->
                    TsosuNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        focusViewModel = focusViewModel,
                        onTaskClick = { taskId -> editingTaskId = taskId },
                        onHabitClick = { habitId -> editingHabitId = habitId },
                        onQuickAddDate = { javaDate ->
                            quickAddInitialDate = kotlinx.datetime.LocalDateTime(
                                kotlinx.datetime.LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth),
                                kotlinx.datetime.LocalTime(0, 0),
                            )
                            showAddTask = true
                        },
                        isVaultConfigured = isVaultConfigured,
                        onSelectFolder = { folderPicker.launch(null) },
                    )
                }

                if (showAddHabit) {
                    ModalBottomSheet(
                        onDismissRequest = { showAddHabit = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        QuickAddHabitSheet(
                            onDismiss = { showAddHabit = false },
                            onAdd = { title, tinyVersion, routineTime, reminderTime ->
                                habitsViewModel.createHabit(title, tinyVersion, routineTime, reminderTime)
                                showAddHabit = false
                            },
                        )
                    }
                }

                if (showAddTask) {
                    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
                    ModalBottomSheet(
                        onDismissRequest = { showAddTask = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        QuickAddTaskSheet(
                            onDismiss = {
                                showAddTask = false
                                quickAddInitialDate = null
                            },
                            initialDueDate = quickAddInitialDate,
                            onAdd = { title, priority, energy, minutes, dueDate, reminderTime, recurrenceRule ->
                                quickAddViewModel.createTask(title, priority, energy, minutes, dueDate, reminderTime, recurrenceRule)
                                showAddTask = false
                            },
                        )
                    }
                }

                editingTaskId?.let { taskId ->
                    ModalBottomSheet(
                        onDismissRequest = { editingTaskId = null },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        TaskDetailSheet(
                            taskId = taskId,
                            onDismiss = { editingTaskId = null },
                        )
                    }
                }

                editingHabitId?.let { habitId ->
                    ModalBottomSheet(
                        onDismissRequest = { editingHabitId = null },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        HabitDetailSheet(
                            habitId = habitId,
                            onDismiss = { editingHabitId = null },
                        )
                    }
                }

                if (showPickOne) {
                    ModalBottomSheet(
                        onDismissRequest = { showPickOne = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        PickOneSheet(onDismiss = { showPickOne = false })
                    }
                }

                if (showFilter) {
                    val currentFilter by focusViewModel.filterSpec.collectAsState()
                    val currentSort by focusViewModel.sortSpec.collectAsState()
                    val savedViews by savedViewPreferences.views.collectAsState(initial = emptyList())
                    val projects by projectRepository.getAllProjects()
                        .collectAsState(initial = emptyList())
                    val scope = rememberCoroutineScope()
                    ModalBottomSheet(
                        onDismissRequest = { showFilter = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        FilterSheet(
                            currentFilter = currentFilter,
                            currentSort = currentSort,
                            projects = projects,
                            savedViews = savedViews,
                            onApply = { filter, sort ->
                                focusViewModel.applyFilter(filter, sort)
                            },
                            onClear = { focusViewModel.clearFilter() },
                            onDismiss = { showFilter = false },
                            onSaveView = { name, filter, sort ->
                                scope.launch { savedViewPreferences.save(name, filter, sort) }
                            },
                            onDeleteView = { name ->
                                scope.launch { savedViewPreferences.delete(name) }
                            },
                        )
                    }
                }
            }
        }
    }

    private fun setupResumePull() {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = System.currentTimeMillis()
                if (now - lastSyncTime > 30_000) {
                    lastSyncTime = now
                    lifecycleScope.launch(Dispatchers.IO) {
                        val isConfigured = syncRepository.isConfigured().first()
                        if (isConfigured) {
                            vaultChangeWatcher.pullOnce()
                        }
                    }
                }
            }
        })
    }
}
