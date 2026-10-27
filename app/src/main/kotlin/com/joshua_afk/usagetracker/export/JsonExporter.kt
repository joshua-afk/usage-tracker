package com.joshua_afk.usagetracker.export

import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import com.joshua_afk.usagetracker.sync.ApiJson
import com.joshua_afk.usagetracker.sync.SyncPayload
import com.joshua_afk.usagetracker.sync.toDto
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ExportSource {
    suspend fun sessions(fromMs: Long, toMs: Long): List<SessionEntity>
    suspend fun unlocks(fromMs: Long, toMs: Long): List<UnlockEntity>
}

class RoomExportSource(private val db: AppDatabase) : ExportSource {
    override suspend fun sessions(fromMs: Long, toMs: Long) = db.sessionDao().startingBetween(fromMs, toMs)
    override suspend fun unlocks(fromMs: Long, toMs: Long) = db.unlockDao().between(fromMs, toMs)
}

data class ExportResult(val file: File, val sessionCount: Int, val unlockCount: Int)

private val ExportJson = Json(from = ApiJson) { prettyPrint = true }

/** Writes the same JSON shape the sync endpoint receives, so the file can be replayed against it. */
class JsonExporter(
    private val source: ExportSource,
    private val deviceId: () -> String,
    private val dir: File,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> LocalDateTime = LocalDateTime::now,
) {
    suspend fun export(range: ExportRange): ExportResult {
        val sessions = source.sessions(range.startMs(zone), range.endMs(zone))
        val unlocks = source.unlocks(range.startMs(zone), range.endMs(zone))
        val payload = SyncPayload(deviceId(), sessions.map { it.toDto(zone) }, unlocks.map { it.toDto(zone) })
        val json = ExportJson.encodeToString(payload)
        return withContext(Dispatchers.IO) {
            dir.mkdirs()
            dir.listFiles()?.forEach { it.delete() } // only the latest export is kept in the cache
            val file = File(dir, range.fileName(now()))
            file.writeText(json)
            ExportResult(file, sessions.size, unlocks.size)
        }
    }
}
