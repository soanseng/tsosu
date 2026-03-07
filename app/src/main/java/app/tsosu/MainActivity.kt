package app.tsosu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import app.tsosu.navigation.BottomNavBar
import app.tsosu.navigation.Screen
import app.tsosu.navigation.TsosuNavHost
import app.tsosu.ui.screens.pickone.PickOneSheet
import app.tsosu.ui.screens.quickadd.QuickAddTaskSheet
import app.tsosu.ui.screens.quickadd.QuickAddViewModel
import app.tsosu.ui.screens.taskdetail.TaskDetailSheet
import app.tsosu.ui.theme.DarkModeOption
import app.tsosu.ui.theme.ThemePreferences
import app.tsosu.ui.theme.TsosuTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import app.tsosu.data.vikunja.sync.SyncWorker
import app.tsosu.domain.repository.SyncRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var syncRepository: SyncRepository

    private var lastSyncTime = 0L

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleSyncWorker()
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
                var editingTaskId by remember { mutableStateOf<String?>(null) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text("Tsosu") },
                            actions = {
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
                        onTaskClick = { taskId -> editingTaskId = taskId },
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
                            onAdd = { title, priority, energy, minutes, dueDate ->
                                quickAddViewModel.createTask(title, priority, energy, minutes, dueDate)
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
            }
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    private fun setupResumePull() {
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = System.currentTimeMillis()
                if (now - lastSyncTime > 30_000) {
                    lastSyncTime = now
                    CoroutineScope(Dispatchers.IO).launch {
                        val isConfigured = syncRepository.isRemoteConfigured().first()
                        if (isConfigured) {
                            syncRepository.sync()
                        }
                    }
                }
            }
        })
    }
}
