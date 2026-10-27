package com.joshua_afk.usagetracker.collector

/**
 * Turns a window of raw usage events into closed sessions and unlock times.
 *
 * Re-read safety: a session still open at the end of the window is not emitted, and the returned
 * checkpoint is held back to the start of the earliest open session. Only sessions that start
 * before the checkpoint (and unlocks before it) are returned, so the next run, which re-reads
 * from the checkpoint, never emits the same record twice and never misses one.
 */
object SessionBuilder {
    /** Activity transitions inside one app pause and resume within this gap; treat them as one session. */
    const val MERGE_GAP_MS = 2_000L
    const val MIN_SESSION_MS = 1_000L

    /** An app "open" this long without a pause event is a missing event, not real usage. */
    const val MAX_OPEN_MS = 12 * 60 * 60 * 1000L

    private class State(val startMs: Long, val active: MutableSet<String>, var pausedAtMs: Long? = null)

    fun build(events: List<RawEvent>, fromMs: Long, toMs: Long): BuildResult {
        val states = LinkedHashMap<String, State>()
        val closed = mutableListOf<RawSession>()
        val unlocks = mutableListOf<Long>()

        fun close(pkg: String, endMs: Long) {
            val state = states.remove(pkg) ?: return
            closed += RawSession(pkg, state.startMs, endMs)
        }

        fun flushExpired(nowMs: Long) {
            val expired = states.filterValues { s -> s.pausedAtMs?.let { nowMs - it > MERGE_GAP_MS } == true }
            for ((pkg, state) in expired) close(pkg, state.pausedAtMs!!)
        }

        val window = events.filter { it.timeMs in fromMs until toMs }.sortedBy { it.timeMs }
        for (event in window) {
            flushExpired(event.timeMs)
            when (event.kind) {
                EventKind.RESUMED -> {
                    val state = states[event.packageName]
                    if (state == null) {
                        states[event.packageName] = State(event.timeMs, mutableSetOf(event.className))
                    } else {
                        state.active += event.className
                        state.pausedAtMs = null
                    }
                }
                EventKind.PAUSED -> states[event.packageName]?.let { state ->
                    state.active -= event.className
                    if (state.active.isEmpty()) state.pausedAtMs = event.timeMs
                }
                EventKind.SCREEN_OFF, EventKind.SHUTDOWN ->
                    for (pkg in states.keys.toList()) close(pkg, states.getValue(pkg).pausedAtMs ?: event.timeMs)
                EventKind.UNLOCK -> unlocks += event.timeMs
            }
        }
        flushExpired(toMs)
        states.entries.removeAll { (_, s) -> s.pausedAtMs == null && toMs - s.startMs > MAX_OPEN_MS }

        val checkpoint = minOf(toMs, states.values.minOfOrNull { it.startMs } ?: toMs)
        return BuildResult(
            sessions = closed.filter { it.startMs < checkpoint && it.durationMs >= MIN_SESSION_MS },
            unlockTimesMs = unlocks.filter { it < checkpoint },
            newCheckpointMs = checkpoint,
        )
    }
}
