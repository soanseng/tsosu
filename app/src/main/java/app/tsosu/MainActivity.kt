package app.tsosu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import app.tsosu.data.markdown.MarkdownPreferences
import app.tsosu.navigation.BottomNavBar
import app.tsosu.navigation.Screen
import app.tsosu.navigation.TsosuNavHost
import app.tsosu.ui.screens.filter.FilterSheet
import app.tsosu.ui.screens.focus.FocusViewModel
import app.tsosu.ui.screens.pickone.PickOneSheet
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.screens.quickadd.QuickAddViewModel
import app.tsosu.ui.screens.taskdetail.TaskDetailSheet
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.theme.TsosuTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.tsosu.domain.repository.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var syncRepository: SyncRepository
    @Inject lateinit var markdownPreferences: MarkdownPreferences

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
                var showPickOne by remember { mutableStateOf(false) }
                var showFilter by remember { mutableStateOf(false) }
                var editingTaskId by remember { mutableStateOf<String?>(null) }
                val focusViewModel: FocusViewModel = hiltViewModel()
                val isVaultConfigured by syncRepository.isConfigured()
                    .collectAsState(initial = true)
                val scope = rememberCoroutineScope()

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
                        syncRepository.sync()
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        val focusState by focusViewModel.uiState.collectAsState()
                        TopAppBar(
                            title = { Text("Tsosu") },
                            actions = {
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
                                    navController.navigate(Screen.Kanban.route)
                                }) {
                                    Icon(Icons.Default.ViewKanban, contentDescription = "Kanban")
                                }
                                IconButton(onClick = {
                                    navController.navigate(Screen.Settings.route)
                                }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                                }
                            },
                        )
                    },
                    bottomBar = { BottomNavBar(navController) },
                    floatingActionButton = {
                        @OptIn(ExperimentalFoundationApi::class)
                        FloatingActionButton(
                            onClick = { showAddTask = true },
                            modifier = Modifier.combinedClickable(
                                onClick = { showAddTask = true },
                                onLongClick = { showPickOne = true },
                            ),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Task")
                        }
                    },
                ) { innerPadding ->
                    TsosuNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        focusViewModel = focusViewModel,
                        onTaskClick = { taskId -> editingTaskId = taskId },
                        isVaultConfigured = isVaultConfigured,
                        onSelectFolder = { folderPicker.launch(null) },
                    )
                }

                if (showAddTask) {
                    val quickAddViewModel: QuickAddViewModel = hiltViewModel()
                    ModalBottomSheet(
                        onDismissRequest = { showAddTask = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        QuickAddTaskSheet(
                            onDismiss = { showAddTask = false },
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
                    ModalBottomSheet(
                        onDismissRequest = { showFilter = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    ) {
                        FilterSheet(
                            currentFilter = currentFilter,
                            currentSort = currentSort,
                            onApply = { filter, sort ->
                                focusViewModel.applyFilter(filter, sort)
                            },
                            onClear = { focusViewModel.clearFilter() },
                            onDismiss = { showFilter = false },
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
                            val result = syncRepository.sync()
                            result.fold(
                                onSuccess = { r ->
                                    snackbarHostState.showSnackbar(
                                        "Synced ${r.exported} tasks, ${r.imported} habits"
                                    )
                                },
                                onFailure = { e ->
                                    snackbarHostState.showSnackbar(
                                        "Sync failed: ${e.message}"
                                    )
                                },
                            )
                        }
                    }
                }
            }
        })
    }
}
