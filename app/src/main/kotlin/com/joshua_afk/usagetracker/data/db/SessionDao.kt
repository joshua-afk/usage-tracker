package com.joshua_afk.usagetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sessions: List<SessionEntity>)

    @Query("SELECT * FROM sessions WHERE synced = 0 ORDER BY startedAtMs LIMIT :limit")
    suspend fun unsynced(limit: Int): List<SessionEntity>

    @Query("UPDATE sessions SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM sessions WHERE synced = 0")
    suspend fun unsyncedCount(): Int

    /** Sessions overlapping [fromMs, toMs); callers clip durations to the range. */
    @Query("SELECT * FROM sessions WHERE startedAtMs < :toMs AND endedAtMs > :fromMs ORDER BY startedAtMs")
    suspend fun overlapping(fromMs: Long, toMs: Long): List<SessionEntity>

    @Query("SELECT * FROM sessions ORDER BY startedAtMs")
    suspend fun all(): List<SessionEntity>
}
