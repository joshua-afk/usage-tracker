package com.joshua_afk.usagetracker.collector

enum class EventKind { RESUMED, PAUSED, SCREEN_OFF, SHUTDOWN, UNLOCK }

/** Platform-independent view of a UsageEvents.Event, so session building stays unit-testable. */
data class RawEvent(
    val kind: EventKind,
    val timeMs: Long,
    val packageName: String = "",
    val className: String = "",
)

data class RawSession(val packageName: String, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

/**
 * @property newCheckpointMs events before this time are fully processed; the next run must read from here.
 */
data class BuildResult(
    val sessions: List<RawSession>,
    val unlockTimesMs: List<Long>,
    val newCheckpointMs: Long,
)
