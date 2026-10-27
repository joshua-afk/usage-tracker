package com.joshua_afk.usagetracker.export

import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import com.joshua_afk.usagetracker.sync.ApiJson
import com.joshua_afk.usagetracker.sync.SyncPayload
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JsonExporterTest {
    @get:Rule val tmp = TemporaryFolder()

    private val day = 24 * 60 * 60 * 1000L
    private val sessions = listOf(
        SessionEntity("s1", "pkg.a", "A", 1 * day + 1_000, 1 * day + 61_000, 60),
        SessionEntity("s2", "pkg.b", "B", 5 * day, 5 * day + 5_000, 5),
    )
    private val unlocks = listOf(UnlockEntity("u1", 1 * day + 500), UnlockEntity("u2", 5 * day + 500))

    private val source = object : ExportSource {
        override suspend fun sessions(fromMs: Long, toMs: Long) = sessions.filter { it.startedAtMs in fromMs until toMs }
        override suspend fun unlocks(fromMs: Long, toMs: Long) = unlocks.filter { it.occurredAtMs in fromMs until toMs }
    }

    private fun exporter(dir: File) = JsonExporter(
        source, deviceId = { "dev-1" }, dir = dir, zone = ZoneOffset.UTC,
        now = { LocalDateTime.of(2026, 10, 27, 9, 5) },
    )

    private fun read(file: File) = ApiJson.decodeFromString<SyncPayload>(file.readText())

    @Test
    fun `exports everything in the sync payload shape`() = runTest {
        val result = exporter(tmp.newFolder()).export(ExportRange())

        assertEquals(2, result.sessionCount)
        assertEquals(2, result.unlockCount)
        val payload = read(result.file)
        assertEquals("dev-1", payload.deviceId)
        assertEquals(listOf("s1", "s2"), payload.sessions.map { it.id })
        assertEquals("1970-01-02T00:00:01.000Z", payload.sessions.first().startedAt)
        assertEquals(listOf("u1", "u2"), payload.unlocks.map { it.id })
    }

    @Test
    fun `date range keeps only records inside it, including the whole last day`() = runTest {
        val range = ExportRange(LocalDate.of(1970, 1, 2), LocalDate.of(1970, 1, 2))
        val result = exporter(tmp.newFolder()).export(range)

        val payload = read(result.file)
        assertEquals(listOf("s1"), payload.sessions.map { it.id })
        assertEquals(listOf("u1"), payload.unlocks.map { it.id })
        assertEquals("usage-tracker-1970-01-02_to_1970-01-02.json", result.file.name)
    }

    @Test
    fun `only the latest export stays in the directory`() = runTest {
        val dir = tmp.newFolder()
        val first = exporter(dir).export(ExportRange(LocalDate.of(1970, 1, 2), LocalDate.of(1970, 1, 2)))
        val second = exporter(dir).export(ExportRange())

        assertFalse(first.file.exists())
        assertEquals(listOf(second.file.name), dir.list()!!.toList())
    }
}
