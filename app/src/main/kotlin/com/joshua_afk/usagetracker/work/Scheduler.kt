package com.joshua_afk.usagetracker.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val COLLECT_PERIODIC = "collect-periodic"
    private const val COLLECT_NOW = "collect-now"
    private const val SYNC_PERIODIC = "sync-periodic"
    private const val SYNC_NOW = "sync-now"
    private const val BACKOFF_SECONDS = 30L

    fun scheduleCollection(context: Context) {
        val request = PeriodicWorkRequestBuilder<CollectWorker>(1, TimeUnit.HOURS).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(COLLECT_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** One-off collection, e.g. when the Today screen opens so the numbers are current. */
    fun collectNow(context: Context) {
        WorkManager.getInstance(context)
            .enqueueUniqueWork(COLLECT_NOW, ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<CollectWorker>().build())
    }

    /** Hourly sync, only on unmetered networks (home Wi-Fi). Failures retry with exponential backoff. */
    fun scheduleSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.UNMETERED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(SYNC_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Manual "Sync now": runs on any connected network, since the user asked for it. */
    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(SYNC_NOW, ExistingWorkPolicy.REPLACE, request)
    }
}
