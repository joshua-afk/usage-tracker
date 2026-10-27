package com.joshua_afk.usagetracker.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.joshua_afk.usagetracker.permission.UsageAccess
import com.joshua_afk.usagetracker.ui.history.HistoryScreen
import com.joshua_afk.usagetracker.ui.onboarding.PermissionScreen
import com.joshua_afk.usagetracker.ui.settings.SettingsScreen
import com.joshua_afk.usagetracker.ui.today.TodayScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val TODAY = "today"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

private data class Tab(val route: String, val label: String)

private val tabs = listOf(Tab(Routes.TODAY, "Today"), Tab(Routes.HISTORY, "History"), Tab(Routes.SETTINGS, "Settings"))

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    // The user grants access in system settings, so re-check whenever we return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = UsageAccess.isGranted(context) }

    val navController = rememberNavController()
    val startDestination = remember { if (granted) Routes.TODAY else Routes.ONBOARDING }
    LaunchedEffect(granted) {
        val route = navController.currentDestination?.route
        if (granted && route == Routes.ONBOARDING) {
            navController.navigate(Routes.TODAY) { popUpTo(0) }
        } else if (!granted && route != Routes.ONBOARDING) {
            navController.navigate(Routes.ONBOARDING) { popUpTo(0) }
        }
    }

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.switchTab(tab.route) },
                            icon = {},
                            label = { Text(tab.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = startDestination, modifier = Modifier.padding(padding)) {
            composable(Routes.ONBOARDING) {
                PermissionScreen(onOpenSettings = { context.startActivity(UsageAccess.settingsIntent()) })
            }
            composable(Routes.TODAY) { TodayScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
        }
    }
}

private fun NavController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
