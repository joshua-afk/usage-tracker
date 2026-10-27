package com.joshua_afk.usagetracker.data.repo

import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.stats.DayStats
import com.joshua_afk.usagetracker.stats.DayStatsCalculator
import java.time.LocalDate
import java.time.ZoneId

class UsageRepository(private val db: AppDatabase) {
    suspend fun statsFor(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): DayStats {
        // Next midnight, not +24h, so days with a DST change still line up.
        val startMs = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return DayStatsCalculator.compute(
            sessions = db.sessionDao().overlapping(startMs, endMs),
            unlockCount = db.unlockDao().countBetween(startMs, endMs),
            dayStartMs = startMs,
            dayEndMs = endMs,
        )
    }
}
