package com.joshua_afk.usagetracker.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sessions",
    indices = [Index("synced"), Index("startedAtMs")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    val packageName: String,
    val appName: String,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationSeconds: Long,
    val synced: Boolean = false,
)
