package com.joshua_afk.usagetracker.sync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Wire format shared by the sync POST body and the exported file. See docs/api-contract.md. */
@Serializable
data class SyncPayload(
    @SerialName("device_id") val deviceId: String,
    val sessions: List<SessionDto>,
    val unlocks: List<UnlockDto>,
)

@Serializable
data class SessionDto(
    val id: String,
    @SerialName("package") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("ended_at") val endedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Long,
)

@Serializable
data class UnlockDto(
    val id: String,
    @SerialName("occurred_at") val occurredAt: String,
)

val ApiJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}
