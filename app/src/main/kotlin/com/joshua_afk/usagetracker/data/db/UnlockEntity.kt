package com.joshua_afk.usagetracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unlocks",
    indices = [Index("synced"), Index("occurredAtMs")],
)
data class UnlockEntity(
    @PrimaryKey val id: String,
    val occurredAtMs: Long,
    val synced: Boolean = false,
)
