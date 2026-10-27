package com.joshua_afk.usagetracker.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.joshua_afk.usagetracker.permission.UsageAccess
import com.joshua_afk.usagetracker.ui.onboarding.PermissionScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
}

@Composable
fun AppNavHost() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    // The user grants access in system settings, so re-check whenever we return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { granted = UsageAccess.isGranted(context) }

    val navController = rememberNavController()
    val target = if (granted) Routes.HOME else Routes.ONBOARDING
    LaunchedEffect(target) {
        if (navController.currentDestination?.route != target) {
            navController.navigate(target) { popUpTo(0) }
        }
    }

    NavHost(navController, startDestination = target) {
        composable(Routes.ONBOARDING) {
            PermissionScreen(onOpenSettings = { context.startActivity(UsageAccess.settingsIntent()) })
        }
        composable(Routes.HOME) {
            // Placeholder until the Today/History/Settings screens land.
            Text("Usage access granted")
        }
    }
}
