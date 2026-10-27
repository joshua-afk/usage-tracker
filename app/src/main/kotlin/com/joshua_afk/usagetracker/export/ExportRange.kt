package com.joshua_afk.usagetracker.export

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Inclusive calendar days; a null bound is open-ended, so ExportRange() means all data. */
data class ExportRange(val from: LocalDate? = null, val to: LocalDate? = null) {
    init {
        require(from == null || to == null || from <= to) { "from must not be after to" }
    }

    val isAll: Boolean get() = from == null && to == null

    fun startMs(zone: ZoneId): Long = from?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: 0L

    /** Exclusive: the midnight after the last day, so the whole "to" day is included. */
    fun endMs(zone: ZoneId): Long =
        to?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli() ?: Long.MAX_VALUE

    fun fileName(now: LocalDateTime): String =
        if (isAll) {
            "usage-tracker-all-${now.format(STAMP)}.json"
        } else {
            "usage-tracker-${from ?: "beginning"}_to_${to ?: "latest"}.json"
        }

    private companion object {
        val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}
