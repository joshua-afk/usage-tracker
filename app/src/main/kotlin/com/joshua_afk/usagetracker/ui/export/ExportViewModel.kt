package com.joshua_afk.usagetracker.ui.export

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.prefs.SettingsStore
import com.joshua_afk.usagetracker.export.ExportRange
import com.joshua_afk.usagetracker.export.JsonExporter
import com.joshua_afk.usagetracker.export.RoomExportSource
import com.joshua_afk.usagetracker.export.shareIntent
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface ExportEvent {
    data class Ready(val shareIntent: Intent, val sessions: Int, val unlocks: Int) : ExportEvent
    data class Failed(val message: String) : ExportEvent
}

class ExportViewModel(app: Application) : AndroidViewModel(app) {
    private val exporter = JsonExporter(
        source = RoomExportSource(AppDatabase.get(app)),
        deviceId = { SettingsStore(app).deviceId },
        dir = File(app.cacheDir, "exports"),
    )
    private val _busy = MutableStateFlow(false)
    private val _events = Channel<ExportEvent>(Channel.BUFFERED)

    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    val events: Flow<ExportEvent> = _events.receiveAsFlow()

    fun export(range: ExportRange) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try {
                val result = exporter.export(range)
                _events.send(
                    ExportEvent.Ready(shareIntent(getApplication(), result.file), result.sessionCount, result.unlockCount),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(ExportEvent.Failed(e.message ?: e.javaClass.simpleName))
            } finally {
                _busy.value = false
            }
        }
    }
}
