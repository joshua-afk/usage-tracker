package com.joshua_afk.usagetracker.sync

import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

/** ISO 8601 with the offset the given zone had at that instant, e.g. 2026-10-27T09:15:02.000+08:00. */
fun formatTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMs), zone).format(timeFormat)

fun SessionEntity.toDto(zone: ZoneId = ZoneId.systemDefault()) = SessionDto(
    id = id,
    packageName = packageName,
    appName = appName,
    startedAt = formatTime(startedAtMs, zone),
    endedAt = formatTime(endedAtMs, zone),
    durationSeconds = durationSeconds,
)

fun UnlockEntity.toDto(zone: ZoneId = ZoneId.systemDefault()) =
    UnlockDto(id = id, occurredAt = formatTime(occurredAtMs, zone))
