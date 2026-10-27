package com.joshua_afk.usagetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UnlockDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(unlocks: List<UnlockEntity>)

    @Query("SELECT * FROM unlocks WHERE synced = 0 ORDER BY occurredAtMs LIMIT :limit")
    suspend fun unsynced(limit: Int): List<UnlockEntity>

    @Query("UPDATE unlocks SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<String>)

    @Query("SELECT COUNT(*) FROM unlocks WHERE synced = 0")
    suspend fun unsyncedCount(): Int

    @Query("SELECT COUNT(*) FROM unlocks WHERE occurredAtMs >= :fromMs AND occurredAtMs < :toMs")
    suspend fun countBetween(fromMs: Long, toMs: Long): Int

    @Query("SELECT * FROM unlocks WHERE occurredAtMs >= :fromMs AND occurredAtMs < :toMs ORDER BY occurredAtMs")
    suspend fun between(fromMs: Long, toMs: Long): List<UnlockEntity>

    @Query("SELECT * FROM unlocks ORDER BY occurredAtMs")
    suspend fun all(): List<UnlockEntity>
}
