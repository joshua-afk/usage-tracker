package com.joshua_afk.usagetracker.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshua_afk.usagetracker.ui.components.DayStatsContent

@Composable
fun TodayScreen(viewModel: TodayViewModel = viewModel()) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    DayStatsContent(title = "Today", stats = stats)
}
