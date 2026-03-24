package app.tsosu.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.tsosu.ui.screens.calendar.CalendarScreen
import app.tsosu.ui.screens.focus.FocusScreen
import app.tsosu.ui.screens.focus.FocusViewModel
import app.tsosu.ui.screens.habits.HabitsScreen
import app.tsosu.ui.screens.inbox.InboxScreen
import app.tsosu.ui.screens.kanban.KanbanScreen
import app.tsosu.ui.screens.settings.SettingsScreen
import app.tsosu.ui.screens.upcoming.UpcomingScreen
import app.tsosu.ui.screens.weeklyreview.WeeklyReviewScreen

@Composable
fun TsosuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    focusViewModel: FocusViewModel? = null,
    onTaskClick: (String) -> Unit = {},
    isVaultConfigured: Boolean = true,
    onSelectFolder: () -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Focus.route,
        modifier = modifier,
    ) {
        composable(
            Screen.Focus.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) {
            if (focusViewModel != null) {
                FocusScreen(
                    viewModel = focusViewModel,
                    onTaskClick = onTaskClick,
                    isVaultConfigured = isVaultConfigured,
                    onSelectFolder = onSelectFolder,
                )
            } else {
                FocusScreen(
                    onTaskClick = onTaskClick,
                    isVaultConfigured = isVaultConfigured,
                    onSelectFolder = onSelectFolder,
                )
            }
        }
        composable(
            Screen.Inbox.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { InboxScreen(onTaskClick = onTaskClick) }
        composable(
            Screen.Habits.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { HabitsScreen() }
        composable(
            Screen.Calendar.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { CalendarScreen(onTaskClick = onTaskClick) }
        composable(
            Screen.Upcoming.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { UpcomingScreen(onTaskClick = onTaskClick) }
        composable(
            Screen.Settings.route,
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
        ) { SettingsScreen() }
        composable(
            Screen.WeeklyReview.route,
            enterTransition = { fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
        ) { WeeklyReviewScreen() }
        composable(
            Screen.Kanban.route,
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
        ) { KanbanScreen(onTaskClick = onTaskClick) }
    }
}
