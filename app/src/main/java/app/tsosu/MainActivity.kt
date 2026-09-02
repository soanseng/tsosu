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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import app.tsosu.ui.screens.habits.HabitsViewModel
import app.tsosu.ui.screens.inbox.InboxViewModel
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.screens.quickadd.QuickAddViewModel
import app.tsosu.ui.screens.search.SearchSheet
import app.tsosu.ui.screens.taskdetail.TaskDetailSheet
import app.tsosu.ui.screens.today.TodayViewModel
import app.tsosu.ui.widget.QuickAddTileService
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.theme.TsosuTheme
import app.tsosu.util.StorageUris
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.tsosu.domain.repository.GamificationRepository
import app.tsosu.domain.repository.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var appLockPreferences: app.tsosu.ui.theme.AppLockPreferences
    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var markdownPreferences: MarkdownPreferences
    @Inject lateinit var vaultChangeWatcher: VaultChangeWatcher
    @Inject lateinit var gamificationRepository: GamificationRepository

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

            // Biometric gate: when enabled, keep the UI locked until the user
            // authenticates (biometric or device credential).
            // `null` = DataStore not loaded yet: no gate flash for unlocked
            // users, and no way to remember `unlocked = true` before the
            // persisted value arrives.
            val appLockEnabled by appLockPreferences.enabled
                .map { it as Boolean? }
                .collectAsState(initial = null)
            var unlocked by remember { mutableStateOf(false) }
            LaunchedEffect(appLockEnabled) { if (appLockEnabled == false) unlocked = true }

            if (appLockEnabled == true && !unlocked) {
                LockScreen(onUnlocked = { unlocked = true })
                return@setContent
            }

            TsosuTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                val navController = rememberNavController()
                var sharedCaptureText by remember {
                    mutableStateOf(SharedCaptureText.fromIntent(intent))
                }
                var showAddTask by remember {
                    mutableStateOf(
                        sharedCaptureText != null ||
                            intent.getBooleanExtra(QuickAddTileService.EXTRA_OPEN_QUICK_ADD, false),
                    )
                }
                var quickAddInitialDate by remember { mutableStateOf<kotlinx.datetime.LocalDateTime?>(null) }
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
                var showSearch by remember { mutableStateOf(false) }
                var editingTaskId by remember {
                    mutableStateOf(intent.getStringExtra("taskId") ?: null)
                }
                val inboxViewModel: InboxViewModel = hiltViewModel()
                val todayViewModel: TodayViewModel = hiltViewModel()
                val habitsViewModel: HabitsViewModel = hiltViewModel()
                val energy by gamificationRepository.energy().collectAsState(initial = 0)
                val isVaultConfigured by syncRepository.isConfigured()
                    .collectAsState(initial = true)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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
                        TopAppBar(
                            title = {
                                Text(
                                    stringResource(
                                        when (currentRoute) {
                                            Screen.Inbox.route -> R.string.nav_inbox
                                            Screen.Today.route -> R.string.nav_today
                                            Screen.Habits.route -> R.string.nav_habits
                                            Screen.Upcoming.route -> R.string.nav_upcoming
                                            Screen.Calendar.route -> R.string.nav_calendar
                                            else -> R.string.app_name
                                        },
                                    ),
                                )
                            },
                            actions = {
                                Text(
                                    "⚡$energy",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                var showViewMenu by remember { mutableStateOf(false) }
                                IconButton(onClick = { showViewMenu = true }) {
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
                                            text = { Text(stringResource(R.string.nav_calendar)) },
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
                                IconButton(onClick = { showSearch = true }) {
                                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                                }
                                IconButton(onClick = {
                                    navController.navigate(Screen.Settings.route)
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_settings))
                                }
                            },
                        )
                    },
                    bottomBar = {
                        val inboxTasks by inboxViewModel.tasks.collectAsState()
                        val todayCount by todayViewModel.pendingCount.collectAsState()
                        val habitsState by habitsViewModel.uiState.collectAsState()
                        BottomNavBar(
                            navController = navController,
                            inboxPendingCount = inboxTasks.size,
                            todayPendingCount = todayCount,
                            habitsPendingCount = habitsState.totalCount - habitsState.completedCount,
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                quickAddInitialDate = null
                                showAddTask = true
                            },
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.quick_add_task_title),
                            )
                        }
                    },
                ) { innerPadding ->
                    TsosuNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        todayViewModel = todayViewModel,
                        onTaskClick = { taskId -> editingTaskId = taskId },
                        onHabitClick = { },
                        onQuickAddDate = { javaDate ->
                            quickAddInitialDate = kotlinx.datetime.LocalDateTime(
                                kotlinx.datetime.LocalDate(javaDate.year, javaDate.monthValue, javaDate.dayOfMonth),
                                kotlinx.datetime.LocalTime(0, 0),
                            )
                            showAddTask = true
                        },
                        isVaultConfigured = isVaultConfigured,
                        onSelectFolder = { folderPicker.launch(StorageUris.browseStartUri()) },
                    )
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
                                sharedCaptureText = null
                            },
                            initialDueDate = quickAddInitialDate,
                            initialTitle = sharedCaptureText,
                            onAdd = { title, priority, energy, minutes, dueDate, reminderTime, recurrenceRule, projectName ->
                                quickAddViewModel.createTask(title, priority, energy, minutes, dueDate, reminderTime, recurrenceRule, projectName)
                                showAddTask = false
                                sharedCaptureText = null
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

                if (showSearch) {
                    ModalBottomSheet(
                        onDismissRequest = { showSearch = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        SearchSheet(
                            onTaskClick = { task ->
                                showSearch = false
                                editingTaskId = task.id
                            },
                            onToggleDone = { taskId -> inboxViewModel.toggleDone(taskId) },
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


@androidx.compose.runtime.Composable
private fun LockScreen(onUnlocked: () -> Unit) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
            androidx.compose.foundation.layout.Spacer(Modifier.padding(16.dp))
            androidx.compose.material3.Button(onClick = {
                val fragmentActivity = activity ?: return@Button
                val executor = androidx.core.content.ContextCompat.getMainExecutor(fragmentActivity)
                val prompt = androidx.biometric.BiometricPrompt(
                    fragmentActivity,
                    executor,
                    object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                            onUnlocked()
                        }
                    },
                )
                val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                    .setTitle(fragmentActivity.getString(app.tsosu.R.string.applock_title))
                    .setSubtitle(fragmentActivity.getString(app.tsosu.R.string.applock_subtitle))
                    .setAllowedAuthenticators(
                        androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    )
                    .build()
                prompt.authenticate(info)
            }) {
                Text(stringResource(app.tsosu.R.string.applock_unlock))
            }
        }
    }
}
