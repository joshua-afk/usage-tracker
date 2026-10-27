package com.joshua_afk.usagetracker.stats

import com.joshua_afk.usagetracker.data.db.SessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayStatsCalculatorTest {
    private val dayStart = 100_000L
    private val dayEnd = 200_000L

    private fun session(pkg: String, start: Long, end: Long, name: String = pkg) =
        SessionEntity("$pkg-$start", pkg, name, start, end, (end - start) / 1000)

    @Test
    fun `sums sessions per app and sorts by time`() {
        val stats = DayStatsCalculator.compute(
            listOf(
                session("a", 110_000, 120_000),
                session("b", 120_000, 150_000),
                session("a", 150_000, 160_000),
            ),
            unlockCount = 3, dayStartMs = dayStart, dayEndMs = dayEnd,
        )
        assertEquals(listOf("b" to 30_000L, "a" to 20_000L), stats.apps.map { it.packageName to it.durationMs })
        assertEquals(50_000L, stats.totalMs)
        assertEquals(3, stats.unlockCount)
    }

    @Test
    fun `clips sessions that cross the day boundaries`() {
        val stats = DayStatsCalculator.compute(
            listOf(session("a", 90_000, 110_000), session("b", 190_000, 230_000)),
            unlockCount = 0, dayStartMs = dayStart, dayEndMs = dayEnd,
        )
        assertEquals(mapOf("a" to 10_000L, "b" to 10_000L), stats.apps.associate { it.packageName to it.durationMs })
    }

    @Test
    fun `ignores sessions outside the day`() {
        val stats = DayStatsCalculator.compute(
            listOf(session("a", 10_000, 90_000), session("b", 200_000, 250_000)),
            unlockCount = 0, dayStartMs = dayStart, dayEndMs = dayEnd,
        )
        assertTrue(stats.apps.isEmpty())
        assertEquals(0L, stats.totalMs)
    }

    @Test
    fun `ties are ordered by app name`() {
        val stats = DayStatsCalculator.compute(
            listOf(session("z", 110_000, 120_000, "Zed"), session("y", 130_000, 140_000, "alpha")),
            unlockCount = 0, dayStartMs = dayStart, dayEndMs = dayEnd,
        )
        assertEquals(listOf("alpha", "Zed"), stats.apps.map { it.appName })
    }

    @Test
    fun `formats durations`() {
        assertEquals("30s", formatDuration(30_000))
        assertEquals("45m", formatDuration(45 * 60_000L))
        assertEquals("2h 5m", formatDuration((2 * 3600 + 5 * 60) * 1000L))
        assertEquals("0s", formatDuration(0))
    }
}
