package app.tsosu.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.tsosu.ui.screens.focus.FocusScreen
import app.tsosu.ui.screens.habits.HabitsScreen
import app.tsosu.ui.screens.settings.SettingsScreen
import app.tsosu.ui.screens.upcoming.UpcomingScreen
import app.tsosu.ui.screens.weeklyreview.WeeklyReviewScreen

@Composable
fun TsosuNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    onTaskClick: (String) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Focus.route,
        modifier = modifier,
    ) {
        composable(Screen.Focus.route) { FocusScreen(onTaskClick = onTaskClick) }
        composable(Screen.Habits.route) { HabitsScreen() }
        composable(Screen.Upcoming.route) { UpcomingScreen(onTaskClick = onTaskClick) }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.WeeklyReview.route) { WeeklyReviewScreen() }
    }
}
