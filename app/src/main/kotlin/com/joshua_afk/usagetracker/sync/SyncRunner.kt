package com.joshua_afk.usagetracker.sync

import androidx.room.withTransaction
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Batch(val sessions: List<SessionEntity>, val unlocks: List<UnlockEntity>) {
    val size: Int get() = sessions.size + unlocks.size
    val isEmpty: Boolean get() = size == 0
}

interface UnsyncedStore {
    /** Up to [limit] unsynced records in total, oldest first. */
    suspend fun nextBatch(limit: Int): Batch
    suspend fun markSynced(batch: Batch)
}

class RoomUnsyncedStore(private val db: AppDatabase) : UnsyncedStore {
    override suspend fun nextBatch(limit: Int): Batch {
        val sessions = db.sessionDao().unsynced(limit)
        val remaining = limit - sessions.size
        val unlocks = if (remaining > 0) db.unlockDao().unsynced(remaining) else emptyList()
        return Batch(sessions, unlocks)
    }

    override suspend fun markSynced(batch: Batch) {
        db.withTransaction {
            db.sessionDao().markSynced(batch.sessions.map { it.id })
            db.unlockDao().markSynced(batch.unlocks.map { it.id })
        }
    }
}

sealed interface SyncOutcome {
    data class Done(val records: Int) : SyncOutcome
    data class Failed(val reason: String, val syncedBeforeFailure: Int) : SyncOutcome
}

class SyncRunner(
    private val store: UnsyncedStore,
    private val send: (SyncPayload) -> PostResult,
) {
    /** Sends batches until nothing is left. Records are marked synced only after a 2xx. */
    suspend fun run(deviceId: String): SyncOutcome {
        var total = 0
        while (true) {
            val batch = store.nextBatch(MAX_BATCH)
            if (batch.isEmpty) return SyncOutcome.Done(total)
            val payload = SyncPayload(deviceId, batch.sessions.map { it.toDto() }, batch.unlocks.map { it.toDto() })
            when (val result = withContext(Dispatchers.IO) { send(payload) }) {
                PostResult.Success -> {
                    store.markSynced(batch)
                    total += batch.size
                }
                is PostResult.HttpError -> return SyncOutcome.Failed("HTTP ${result.code}", total)
                is PostResult.NetworkError -> return SyncOutcome.Failed(result.message, total)
            }
        }
    }

    companion object {
        const val MAX_BATCH = 500
    }
}
