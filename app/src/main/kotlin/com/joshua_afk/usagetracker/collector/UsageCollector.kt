package com.joshua_afk.usagetracker.collector

import androidx.room.withTransaction
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import com.joshua_afk.usagetracker.data.prefs.SettingsStore
import java.util.UUID

data class CollectSummary(val sessions: Int, val unlocks: Int)

class UsageCollector(
    private val source: EventSource,
    private val labelFor: (String) -> String,
    private val db: AppDatabase,
    private val settings: SettingsStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun collect(): CollectSummary {
        val now = clock()
        val from = settings.checkpointMs.takeIf { it > 0 } ?: (now - INITIAL_LOOKBACK_MS)
        val result = SessionBuilder.build(source.read(from, now), from, now)

        val sessions = result.sessions.map {
            SessionEntity(
                id = stableId("session", it.packageName, it.startMs, it.endMs),
                packageName = it.packageName,
                appName = labelFor(it.packageName),
                startedAtMs = it.startMs,
                endedAtMs = it.endMs,
                durationSeconds = it.durationMs / 1000,
            )
        }
        val unlocks = result.unlockTimesMs.map { UnlockEntity(id = stableId("unlock", it), occurredAtMs = it) }

        db.withTransaction {
            db.sessionDao().insertAll(sessions)
            db.unlockDao().insertAll(unlocks)
        }
        // Advanced only after the insert succeeds. If the process dies in between, the next run
        // rebuilds the same ids and the IGNORE-on-conflict insert drops the duplicates.
        settings.checkpointMs = result.newCheckpointMs
        return CollectSummary(sessions.size, unlocks.size)
    }

    private companion object {
        /** First-run backfill. The system keeps events only for a limited time, so asking for more is harmless. */
        const val INITIAL_LOOKBACK_MS = 30L * 24 * 60 * 60 * 1000

        /** Deterministic per record, so reprocessing an event window never creates a second id. */
        fun stableId(vararg parts: Any): String =
            UUID.nameUUIDFromBytes(parts.joinToString("|").toByteArray()).toString()
    }
}
