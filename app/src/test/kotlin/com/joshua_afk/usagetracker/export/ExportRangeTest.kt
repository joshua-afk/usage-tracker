package com.joshua_afk.usagetracker.export

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportRangeTest {
    private val utc = ZoneOffset.UTC
    private val day = 24 * 60 * 60 * 1000L

    @Test
    fun `no bounds means all data`() {
        val range = ExportRange()
        assertTrue(range.isAll)
        assertEquals(0L, range.startMs(utc))
        assertEquals(Long.MAX_VALUE, range.endMs(utc))
    }

    @Test
    fun `the to day is included in full`() {
        val range = ExportRange(LocalDate.of(1970, 1, 2), LocalDate.of(1970, 1, 3))
        assertEquals(day, range.startMs(utc))
        assertEquals(3 * day, range.endMs(utc))
    }

    @Test
    fun `bounds follow the given time zone`() {
        val range = ExportRange(LocalDate.of(1970, 1, 2), LocalDate.of(1970, 1, 2))
        assertEquals(day - 8 * 3_600_000L, range.startMs(ZoneOffset.ofHours(8)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `from after to is rejected`() {
        ExportRange(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1))
    }

    @Test
    fun `file names`() {
        val now = LocalDateTime.of(2026, 10, 27, 9, 5)
        assertEquals("usage-tracker-all-20261027-0905.json", ExportRange().fileName(now))
        assertEquals(
            "usage-tracker-2026-10-01_to_2026-10-27.json",
            ExportRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 27)).fileName(now),
        )
        assertEquals("usage-tracker-beginning_to_2026-10-27.json", ExportRange(to = LocalDate.of(2026, 10, 27)).fileName(now))
    }
}
