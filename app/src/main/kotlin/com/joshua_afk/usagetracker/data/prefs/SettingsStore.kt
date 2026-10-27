package com.joshua_afk.usagetracker.data.prefs

import android.content.Context
import androidx.core.content.edit
import java.util.UUID

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("usage_tracker_settings", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString(KEY_URL, "") ?: ""
        set(value) = prefs.edit { putString(KEY_URL, value.trim()) }

    var token: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit { putString(KEY_TOKEN, value.trim()) }

    /** Stable per-install id, generated on first access. */
    val deviceId: String
        @Synchronized get() = prefs.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString()
            .also { id -> prefs.edit { putString(KEY_DEVICE_ID, id) } }

    /** Timestamp (ms) up to which usage events have been fully processed; 0 = never collected. */
    var checkpointMs: Long
        get() = prefs.getLong(KEY_CHECKPOINT, 0L)
        set(value) = prefs.edit { putLong(KEY_CHECKPOINT, value) }

    var lastSyncAtMs: Long
        get() = prefs.getLong(KEY_LAST_SYNC_AT, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC_AT, value) }

    var lastSyncStatus: String
        get() = prefs.getString(KEY_LAST_SYNC_STATUS, "") ?: ""
        set(value) = prefs.edit { putString(KEY_LAST_SYNC_STATUS, value) }

    val isSyncConfigured: Boolean get() = serverUrl.isNotBlank() && token.isNotBlank()

    private companion object {
        const val KEY_URL = "server_url"
        const val KEY_TOKEN = "token"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_CHECKPOINT = "checkpoint_ms"
        const val KEY_LAST_SYNC_AT = "last_sync_at_ms"
        const val KEY_LAST_SYNC_STATUS = "last_sync_status"
    }
}
