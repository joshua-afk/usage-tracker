package com.joshua_afk.usagetracker.ui.today

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshua_afk.usagetracker.collector.createUsageCollector
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.repo.UsageRepository
import com.joshua_afk.usagetracker.stats.DayStats
import java.time.LocalDate
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = UsageRepository(AppDatabase.get(app))
    private val _stats = MutableStateFlow<DayStats?>(null)
    val stats: StateFlow<DayStats?> = _stats.asStateFlow()

    /** Shows what is stored right away, then collects new events and shows the updated numbers. */
    fun refresh() {
        viewModelScope.launch {
            _stats.value = repository.statsFor(LocalDate.now())
            try {
                createUsageCollector(getApplication()).collect()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Collection failed", e)
            }
            _stats.value = repository.statsFor(LocalDate.now())
        }
    }

    private companion object {
        const val TAG = "TodayViewModel"
    }
}
