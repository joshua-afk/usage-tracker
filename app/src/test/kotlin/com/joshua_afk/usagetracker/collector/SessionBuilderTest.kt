package com.joshua_afk.usagetracker.collector

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionBuilderTest {
    private fun resumed(t: Long, pkg: String, cls: String = "Main") = RawEvent(EventKind.RESUMED, t, pkg, cls)
    private fun paused(t: Long, pkg: String, cls: String = "Main") = RawEvent(EventKind.PAUSED, t, pkg, cls)
    private fun screenOff(t: Long) = RawEvent(EventKind.SCREEN_OFF, t)
    private fun unlock(t: Long) = RawEvent(EventKind.UNLOCK, t)

    @Test
    fun `pairs resume and pause into one session`() {
        val r = SessionBuilder.build(listOf(resumed(1_000, "a"), paused(6_000, "a")), 0, 10_000)
        assertEquals(listOf(RawSession("a", 1_000, 6_000)), r.sessions)
        assertEquals(10_000, r.newCheckpointMs)
    }

    @Test
    fun `merges activity transitions inside one app`() {
        val events = listOf(
            resumed(1_000, "a", "A1"), paused(5_000, "a", "A1"),
            resumed(5_100, "a", "A2"), paused(9_000, "a", "A2"),
        )
        val r = SessionBuilder.build(events, 0, 20_000)
        assertEquals(listOf(RawSession("a", 1_000, 9_000)), r.sessions)
    }

    @Test
    fun `keeps sessions apart when the gap is longer than the merge gap`() {
        val events = listOf(
            resumed(1_000, "a"), paused(5_000, "a"),
            resumed(8_000, "a"), paused(12_000, "a"),
        )
        val r = SessionBuilder.build(events, 0, 20_000)
        assertEquals(listOf(RawSession("a", 1_000, 5_000), RawSession("a", 8_000, 12_000)), r.sessions)
    }

    @Test
    fun `open session is held back and checkpoint stays at its start`() {
        val events = listOf(resumed(1_000, "a"), resumed(2_000, "b"), paused(5_000, "b"))
        val r = SessionBuilder.build(events, 0, 10_000)
        assertTrue(r.sessions.isEmpty())
        assertEquals(1_000, r.newCheckpointMs)
    }

    @Test
    fun `re-reading from the held checkpoint yields each session exactly once`() {
        val firstRun = listOf(resumed(1_000, "a"), resumed(2_000, "b"), paused(5_000, "b"))
        val run1 = SessionBuilder.build(firstRun, 0, 10_000)

        val secondRun = firstRun + paused(12_000, "a")
        val run2 = SessionBuilder.build(secondRun, run1.newCheckpointMs, 20_000)

        assertEquals(
            listOf(RawSession("a", 1_000, 12_000), RawSession("b", 2_000, 5_000)),
            (run1.sessions + run2.sessions).sortedBy { it.startMs },
        )
        assertEquals(20_000, run2.newCheckpointMs)
    }

    @Test
    fun `session emitted in one run is not duplicated by its stray pause in the next`() {
        val run1 = SessionBuilder.build(listOf(resumed(1_000, "a"), paused(5_000, "a"), resumed(9_000, "b")), 0, 10_000)
        assertEquals(listOf(RawSession("a", 1_000, 5_000)), run1.sessions)
        assertEquals(9_000, run1.newCheckpointMs)

        val run2 = SessionBuilder.build(listOf(resumed(9_000, "b"), paused(15_000, "b")), run1.newCheckpointMs, 20_000)
        assertEquals(listOf(RawSession("b", 9_000, 15_000)), run2.sessions)
    }

    @Test
    fun `screen off closes open sessions`() {
        val r = SessionBuilder.build(listOf(resumed(1_000, "a"), screenOff(7_000)), 0, 10_000)
        assertEquals(listOf(RawSession("a", 1_000, 7_000)), r.sessions)
    }

    @Test
    fun `screen off ends an already paused session at its pause time`() {
        val r = SessionBuilder.build(listOf(resumed(1_000, "a"), paused(5_000, "a"), screenOff(5_500)), 0, 10_000)
        assertEquals(listOf(RawSession("a", 1_000, 5_000)), r.sessions)
    }

    @Test
    fun `pause without a matching resume is ignored`() {
        val r = SessionBuilder.build(listOf(paused(1_000, "a")), 0, 10_000)
        assertTrue(r.sessions.isEmpty())
    }

    @Test
    fun `drops sessions shorter than the minimum`() {
        val r = SessionBuilder.build(listOf(resumed(1_000, "a"), paused(1_500, "a")), 0, 10_000)
        assertTrue(r.sessions.isEmpty())
    }

    @Test
    fun `recently paused app is held in case it resumes`() {
        val events = listOf(resumed(1_000, "a"), paused(9_500, "a"))
        val early = SessionBuilder.build(events, 0, 10_000)
        assertTrue(early.sessions.isEmpty())
        assertEquals(1_000, early.newCheckpointMs)

        val late = SessionBuilder.build(events, early.newCheckpointMs, 20_000)
        assertEquals(listOf(RawSession("a", 1_000, 9_500)), late.sessions)
    }

    @Test
    fun `stale open session is dropped so the checkpoint can advance`() {
        val toMs = 1_000 + SessionBuilder.MAX_OPEN_MS + 1
        val r = SessionBuilder.build(listOf(resumed(1_000, "a")), 0, toMs)
        assertTrue(r.sessions.isEmpty())
        assertEquals(toMs, r.newCheckpointMs)
    }

    @Test
    fun `unlocks after the held checkpoint are left for the next run`() {
        val events = listOf(unlock(3_000), resumed(5_000, "a"), unlock(9_000))
        val r = SessionBuilder.build(events, 0, 10_000)
        assertEquals(listOf(3_000L), r.unlockTimesMs)
        assertEquals(5_000, r.newCheckpointMs)
    }

    @Test
    fun `events before the window start are ignored`() {
        val events = listOf(unlock(4_000), resumed(1_000, "a"), paused(2_000, "a"))
        val r = SessionBuilder.build(events, 5_000, 10_000)
        assertTrue(r.sessions.isEmpty())
        assertTrue(r.unlockTimesMs.isEmpty())
    }
}
