package app.tsosu.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.stringResource
import app.tsosu.R

@Composable
fun BottomNavBar(
    navController: NavController,
    inboxPendingCount: Int = 0,
    todayPendingCount: Int = 0,
    habitsPendingCount: Int = 0,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        Screen.bottomNavItems.forEach { screen ->
            val title = when (screen) {
                Screen.Inbox -> stringResource(R.string.nav_inbox)
                Screen.Today -> stringResource(R.string.nav_today)
                Screen.Habits -> stringResource(R.string.nav_habits)
                Screen.Upcoming -> stringResource(R.string.nav_upcoming)
                else -> screen.title
            }
            val badgeCount = when (screen) {
                Screen.Inbox -> inboxPendingCount
                Screen.Today -> todayPendingCount
                Screen.Habits -> habitsPendingCount
                else -> 0
            }

            NavigationBarItem(
                icon = {
                    if (badgeCount > 0) {
                        BadgedBox(badge = {
                            Badge { Text(if (badgeCount > 99) "99+" else "$badgeCount") }
                        }) {
                            Icon(screen.icon, contentDescription = title)
                        }
                    } else {
                        Icon(screen.icon, contentDescription = title)
                    }
                },
                label = { Text(title) },
                selected = currentRoute == screen.route,
                onClick = {
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
}
