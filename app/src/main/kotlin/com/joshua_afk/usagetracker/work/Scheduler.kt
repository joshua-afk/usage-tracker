package com.joshua_afk.usagetracker.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object Scheduler {
    private const val COLLECT_PERIODIC = "collect-periodic"
    private const val COLLECT_NOW = "collect-now"

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
}
