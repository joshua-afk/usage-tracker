package com.joshua_afk.usagetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.joshua_afk.usagetracker.ui.nav.AppNavHost
import com.joshua_afk.usagetracker.ui.theme.UsageTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UsageTrackerTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) { AppNavHost() }
            }
        }
    }
}
