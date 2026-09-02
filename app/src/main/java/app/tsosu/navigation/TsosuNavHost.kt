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
import app.tsosu.ui.screens.habits.HabitsScreen
import app.tsosu.ui.screens.inbox.InboxScreen
import app.tsosu.ui.screens.settings.SettingsScreen
import app.tsosu.ui.screens.today.TodayScreen
import app.tsosu.ui.screens.today.TodayViewModel
import app.tsosu.ui.screens.upcoming.UpcomingScreen

@Composable
fun TsosuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    todayViewModel: TodayViewModel? = null,
    onTaskClick: (String) -> Unit = {},
    onHabitClick: (String) -> Unit = {},
    onQuickAddDate: (java.time.LocalDate) -> Unit = {},
    isVaultConfigured: Boolean = true,
    onSelectFolder: () -> Unit = {},
) {
    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Inbox.route,
        modifier = modifier,
    ) {
        composable(
            Screen.Inbox.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) {
            InboxScreen(
                onTaskClick = onTaskClick,
                isVaultConfigured = isVaultConfigured,
                onSelectFolder = onSelectFolder,
            )
        }
        composable(
            Screen.Today.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) {
            if (todayViewModel != null) {
                TodayScreen(viewModel = todayViewModel, onTaskClick = onTaskClick)
            } else {
                TodayScreen(onTaskClick = onTaskClick)
            }
        }
        composable(
            Screen.Habits.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { HabitsScreen(onHabitClick = onHabitClick, onTaskClick = onTaskClick) }
        composable(
            Screen.Upcoming.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) { UpcomingScreen(onTaskClick = onTaskClick) }
        composable(
            Screen.Calendar.route,
            enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
            exitTransition = { fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.92f) },
        ) {
            CalendarScreen(
                onTaskClick = onTaskClick,
                onQuickAddDate = onQuickAddDate,
                onGoToday = { navigateTo(Screen.Today.route) },
            )
        }
        composable(
            Screen.Settings.route,
            enterTransition = { slideInHorizontally(tween(300)) { it } },
            exitTransition = { slideOutHorizontally(tween(300)) { it } },
        ) { SettingsScreen() }
    }
}
