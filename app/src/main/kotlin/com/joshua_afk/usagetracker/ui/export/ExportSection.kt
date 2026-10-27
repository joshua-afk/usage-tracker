package com.joshua_afk.usagetracker.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshua_afk.usagetracker.export.ExportRange
import com.joshua_afk.usagetracker.ui.components.DayPickerDialog
import java.time.LocalDate

private enum class Bound { FROM, TO }

@Composable
fun ExportSection(modifier: Modifier = Modifier, viewModel: ExportViewModel = viewModel()) {
    val context = LocalContext.current
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    var from by remember { mutableStateOf<LocalDate?>(null) }
    var to by remember { mutableStateOf<LocalDate?>(null) }
    var picking by remember { mutableStateOf<Bound?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ExportEvent.Ready -> {
                    message = "Exported ${event.sessions} sessions and ${event.unlocks} unlocks"
                    context.startActivity(event.shareIntent)
                }
                is ExportEvent.Failed -> message = "Export failed: ${event.message}"
            }
        }
    }

    val rangeValid = from != null && to != null && from!! <= to!!
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Export", style = MaterialTheme.typography.titleLarge)
        Button(onClick = { viewModel.export(ExportRange()) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
            Text("Export all data")
        }
        Text("Or a date range", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { picking = Bound.FROM }, modifier = Modifier.weight(1f)) {
                Text("From: ${from ?: "pick"}")
            }
            OutlinedButton(onClick = { picking = Bound.TO }, modifier = Modifier.weight(1f)) {
                Text("To: ${to ?: "pick"}")
            }
        }
        if (from != null && to != null && !rangeValid) {
            Text("From must not be after To", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = { viewModel.export(ExportRange(from, to)) },
            enabled = rangeValid && !busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export date range") }
        message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }

    picking?.let { bound ->
        DayPickerDialog(
            initial = (if (bound == Bound.FROM) from else to) ?: LocalDate.now(),
            onPick = {
                if (bound == Bound.FROM) from = it else to = it
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}
