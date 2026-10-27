package com.joshua_afk.usagetracker.stats

import com.joshua_afk.usagetracker.data.db.SessionEntity

object DayStatsCalculator {
    /** Sessions crossing midnight count only the part inside [dayStartMs, dayEndMs). */
    fun compute(sessions: List<SessionEntity>, unlockCount: Int, dayStartMs: Long, dayEndMs: Long): DayStats {
        val clipped = sessions.mapNotNull { session ->
            val ms = minOf(session.endedAtMs, dayEndMs) - maxOf(session.startedAtMs, dayStartMs)
            if (ms > 0) session to ms else null
        }
        val apps = clipped
            .groupBy { (session, _) -> session.packageName }
            .map { (pkg, entries) ->
                AppUsage(
                    packageName = pkg,
                    appName = entries.last().first.appName,
                    durationMs = entries.sumOf { it.second },
                )
            }
            .sortedWith(compareByDescending<AppUsage> { it.durationMs }.thenBy { it.appName.lowercase() })
        return DayStats(totalMs = apps.sumOf { it.durationMs }, unlockCount = unlockCount, apps = apps)
    }
}
