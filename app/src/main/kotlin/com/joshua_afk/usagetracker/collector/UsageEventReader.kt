package com.joshua_afk.usagetracker.collector

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class UsageEventReader(context: Context) : EventSource {
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun read(fromMs: Long, toMs: Long): List<RawEvent> {
        val events = usageStats.queryEvents(fromMs, toMs)
        val event = UsageEvents.Event()
        val out = mutableListOf<RawEvent>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                ACTIVITY_RESUMED -> EventKind.RESUMED
                ACTIVITY_PAUSED -> EventKind.PAUSED
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> EventKind.SCREEN_OFF
                UsageEvents.Event.DEVICE_SHUTDOWN -> EventKind.SHUTDOWN
                UsageEvents.Event.KEYGUARD_HIDDEN -> EventKind.UNLOCK
                else -> continue
            }
            out += RawEvent(kind, event.timeStamp, event.packageName.orEmpty(), event.className.orEmpty())
        }
        return out
    }

    private companion object {
        // ACTIVITY_RESUMED/PAUSED are API 29 constants; minSdk is 28, where the same values are
        // named MOVE_TO_FOREGROUND/MOVE_TO_BACKGROUND (deprecated names, identical numbers).
        const val ACTIVITY_RESUMED = 1
        const val ACTIVITY_PAUSED = 2
    }
}
