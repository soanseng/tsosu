package app.tsosu.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Inbox : Screen("inbox", "Inbox", Icons.Default.Inbox)
    data object Today : Screen("today", "Today", Icons.Default.Today)
    data object Habits : Screen("habits", "Habits", Icons.Default.Loop)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Categories : Screen("categories", "Categories", Icons.Default.Folder)
    data object Upcoming : Screen("upcoming", "Upcoming", Icons.Default.DateRange)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    companion object {
        val bottomNavItems: List<Screen> by lazy {
            listOf(Inbox, Today, Habits, Upcoming)
        }

        val viewModes: List<Screen> by lazy {
            listOf(Calendar, Categories)
        }
    }
}
