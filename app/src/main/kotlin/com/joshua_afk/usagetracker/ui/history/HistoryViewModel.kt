package com.joshua_afk.usagetracker.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.repo.UsageRepository
import com.joshua_afk.usagetracker.stats.DayStats
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = UsageRepository(AppDatabase.get(app))
    private val _date = MutableStateFlow(LocalDate.now())
    private val _stats = MutableStateFlow<DayStats?>(null)
    private var loadJob: Job? = null

    val date: StateFlow<LocalDate> = _date.asStateFlow()
    val stats: StateFlow<DayStats?> = _stats.asStateFlow()

    init {
        load()
    }

    /** Future days have no data, so the selection is capped at today. */
    fun select(day: LocalDate) {
        _date.value = minOf(day, LocalDate.now())
        load()
    }

    fun shift(days: Long) = select(_date.value.plusDays(days))

    fun load() {
        loadJob?.cancel() // a slower earlier load must not overwrite a newer selection
        loadJob = viewModelScope.launch { _stats.value = repository.statsFor(_date.value) }
    }
}
