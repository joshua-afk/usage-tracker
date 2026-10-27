package com.joshua_afk.usagetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.joshua_afk.usagetracker.stats.AppUsage
import com.joshua_afk.usagetracker.stats.DayStats
import com.joshua_afk.usagetracker.stats.formatDuration

/** Total screen time, unlock count and per-app list; shared by the Today and History screens. */
@Composable
fun DayStatsContent(title: String, stats: DayStats?, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(title, style = MaterialTheme.typography.headlineMedium) }
        if (stats == null) {
            item { CircularProgressIndicator() }
            return@LazyColumn
        }
        item { SummaryRow(stats) }
        if (stats.apps.isEmpty()) {
            item { Text("No app usage recorded", style = MaterialTheme.typography.bodyLarge) }
        } else {
            items(stats.apps, key = { it.packageName }) { AppRow(it, stats.totalMs) }
        }
    }
}

@Composable
private fun SummaryRow(stats: DayStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Screen time", formatDuration(stats.totalMs), Modifier.weight(1f))
        StatCard("Unlocks", stats.unlockCount.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun AppRow(app: AppUsage, totalMs: Long) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(app.appName, style = MaterialTheme.typography.bodyLarge)
            Text(formatDuration(app.durationMs), style = MaterialTheme.typography.bodyLarge)
        }
        LinearProgressIndicator(
            progress = { if (totalMs > 0) app.durationMs.toFloat() / totalMs else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
