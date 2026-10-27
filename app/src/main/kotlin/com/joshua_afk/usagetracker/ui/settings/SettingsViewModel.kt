package com.joshua_afk.usagetracker.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.joshua_afk.usagetracker.collector.createUsageCollector
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.prefs.SettingsStore
import com.joshua_afk.usagetracker.sync.isValidServerUrl
import com.joshua_afk.usagetracker.work.Scheduler
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val serverUrl: String,
    val token: String,
    val lastSyncAtMs: Long,
    val lastSyncStatus: String,
    val unsyncedCount: Int = 0,
    val syncing: Boolean = false,
) {
    val urlValid: Boolean get() = isValidServerUrl(serverUrl)
    val canSync: Boolean get() = urlValid && token.isNotBlank() && !syncing
}

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settings = SettingsStore(app)
    private val db = AppDatabase.get(app)
    private val _state = MutableStateFlow(
        SettingsUiState(settings.serverUrl, settings.token, settings.lastSyncAtMs, settings.lastSyncStatus),
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            Scheduler.syncNowWork(app).collect { infos ->
                val running = infos.any { it.state == WorkInfo.State.RUNNING }
                _state.update { it.copy(syncing = running) }
                // An attempt just ended (success or a failed try waiting to retry): show its result.
                if (!running) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val unsynced = db.sessionDao().unsyncedCount() + db.unlockDao().unsyncedCount()
            _state.update {
                it.copy(
                    lastSyncAtMs = settings.lastSyncAtMs,
                    lastSyncStatus = settings.lastSyncStatus,
                    unsyncedCount = unsynced,
                )
            }
        }
    }

    fun setServerUrl(value: String) {
        settings.serverUrl = value
        _state.update { it.copy(serverUrl = value) }
    }

    fun setToken(value: String) {
        settings.token = value
        _state.update { it.copy(token = value) }
    }

    /** Collects fresh events first so the upload includes what was just used. */
    fun syncNow() {
        viewModelScope.launch {
            try {
                createUsageCollector(getApplication()).collect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Collection before sync failed", e)
            }
            Scheduler.syncNow(getApplication())
        }
    }

    private companion object {
        const val TAG = "SettingsViewModel"
    }
}
