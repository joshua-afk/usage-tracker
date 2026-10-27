package com.joshua_afk.usagetracker.stats

data class AppUsage(val packageName: String, val appName: String, val durationMs: Long)

data class DayStats(
    val totalMs: Long,
    val unlockCount: Int,
    /** Sorted by time spent, longest first. */
    val apps: List<AppUsage>,
)
