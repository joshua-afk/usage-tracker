package com.joshua_afk.usagetracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joshua_afk.usagetracker.data.db.AppDatabase
import com.joshua_afk.usagetracker.data.prefs.SettingsStore
import com.joshua_afk.usagetracker.sync.RoomUnsyncedStore
import com.joshua_afk.usagetracker.sync.SyncClient
import com.joshua_afk.usagetracker.sync.SyncOutcome
import com.joshua_afk.usagetracker.sync.SyncRunner

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        if (!settings.isSyncConfigured) {
            settings.lastSyncStatus = "Not configured"
            return Result.success()
        }

        val client = SyncClient()
        val runner = SyncRunner(RoomUnsyncedStore(AppDatabase.get(applicationContext))) {
            client.post(settings.serverUrl, settings.token, it)
        }
        val outcome = runner.run(settings.deviceId)
        settings.lastSyncAtMs = System.currentTimeMillis()
        return when (outcome) {
            is SyncOutcome.Done -> {
                settings.lastSyncStatus = "Synced ${outcome.records} records"
                Result.success()
            }
            is SyncOutcome.Failed -> {
                settings.lastSyncStatus = "Failed: ${outcome.reason}"
                Result.retry()
            }
        }
    }
}
