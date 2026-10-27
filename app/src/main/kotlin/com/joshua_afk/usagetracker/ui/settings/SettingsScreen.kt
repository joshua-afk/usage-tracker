package com.joshua_afk.usagetracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showToken by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = viewModel::setServerUrl,
            label = { Text("Server URL") },
            placeholder = { Text("http://192.168.1.20:3000/api/usage") },
            singleLine = true,
            isError = state.serverUrl.isNotBlank() && !state.urlValid,
            supportingText = {
                if (state.serverUrl.isNotBlank() && !state.urlValid) Text("Enter a full http:// or https:// URL")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.token,
                onValueChange = viewModel::setToken,
                label = { Text("Token") },
                singleLine = true,
                visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showToken = !showToken }) { Text(if (showToken) "Hide" else "Show") }
        }

        Button(onClick = viewModel::syncNow, enabled = state.canSync, modifier = Modifier.fillMaxWidth()) {
            Text(if (state.syncing) "Syncing..." else "Sync now")
        }

        Text("Last sync: ${formatLastSync(state.lastSyncAtMs)}", style = MaterialTheme.typography.bodyLarge)
        if (state.lastSyncStatus.isNotBlank()) {
            Text("Status: ${state.lastSyncStatus}", style = MaterialTheme.typography.bodyLarge)
        }
        Text("Waiting to sync: ${state.unsyncedCount} records", style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatLastSync(epochMs: Long): String =
    if (epochMs <= 0) {
        "never"
    } else {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
    }
