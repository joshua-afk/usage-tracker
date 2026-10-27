package com.joshua_afk.usagetracker.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Usage access needed", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Usage Tracker reads which apps you use and when, using Android's usage access. " +
                "That data stays on this device and is only sent to the server you configure. " +
                "In the next screen, find Usage Tracker in the list and turn on \"Permit usage access\".",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onOpenSettings) { Text("Open usage access settings") }
    }
}
