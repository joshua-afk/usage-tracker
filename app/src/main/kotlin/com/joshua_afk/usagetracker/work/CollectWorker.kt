package com.joshua_afk.usagetracker.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joshua_afk.usagetracker.collector.createUsageCollector
import com.joshua_afk.usagetracker.permission.UsageAccess
import kotlin.coroutines.cancellation.CancellationException

class CollectWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Nothing to read until the user grants usage access; the next periodic run tries again.
        if (!UsageAccess.isGranted(applicationContext)) return Result.success()
        return try {
            val summary = createUsageCollector(applicationContext).collect()
            Log.i(TAG, "Collected ${summary.sessions} sessions, ${summary.unlocks} unlocks")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Collection failed, will retry", e)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "CollectWorker"
    }
}
