package com.joshua_afk.usagetracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshua_afk.usagetracker.ui.components.DayPickerDialog
import com.joshua_afk.usagetracker.ui.components.DayStatsContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val date by viewModel.date.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { viewModel.shift(-1) }) { Text("Previous") }
            TextButton(onClick = { showPicker = true }) { Text("Pick a day") }
            TextButton(onClick = { viewModel.shift(1) }, enabled = date < LocalDate.now()) { Text("Next") }
        }
        DayStatsContent(
            title = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            stats = stats,
            modifier = Modifier.weight(1f),
        )
    }

    if (showPicker) {
        DayPickerDialog(
            initial = date,
            onPick = {
                viewModel.select(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
