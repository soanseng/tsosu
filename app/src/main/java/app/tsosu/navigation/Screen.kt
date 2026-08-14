package app.tsosu.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Inbox : Screen("inbox", "Inbox", Icons.Default.Inbox)
    data object Focus : Screen("focus", "Focus", Icons.Default.TaskAlt)
    data object Habits : Screen("habits", "Habits", Icons.Default.Loop)
    data object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    data object Upcoming : Screen("upcoming", "Upcoming", Icons.Default.DateRange)
    data object PickOne : Screen("pickone", "Pick One", Icons.Default.Casino)
    data object HabitDetail : Screen("habit/{habitId}", "Habit Detail", Icons.Default.Loop)
    data object Search : Screen("search", "Search", Icons.Default.Inbox)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object WeeklyReview : Screen("weekly_review", "Weekly Review", Icons.Default.TaskAlt)
    data object Kanban : Screen("kanban", "Kanban", Icons.Default.ViewKanban)

    companion object {
        val bottomNavItems: List<Screen> by lazy {
            listOf(Focus, Habits, Calendar, Upcoming)
        }

        val viewModes: List<Screen> by lazy {
            listOf(Focus, Inbox, Kanban, Calendar, Upcoming)
        }
    }
}
