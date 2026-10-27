package com.joshua_afk.usagetracker.collector

fun interface EventSource {
    /** Events with timestamps in [fromMs, toMs). */
    fun read(fromMs: Long, toMs: Long): List<RawEvent>
}
