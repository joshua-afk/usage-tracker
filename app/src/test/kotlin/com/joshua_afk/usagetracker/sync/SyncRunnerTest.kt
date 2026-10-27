package com.joshua_afk.usagetracker.sync

import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRunnerTest {
    private class FakeStore(sessions: Int, unlocks: Int) : UnsyncedStore {
        val sessions = (1..sessions).map { SessionEntity("s$it", "pkg", "App", it * 1000L, it * 1000L + 5000, 5) }.toMutableList()
        val unlocks = (1..unlocks).map { UnlockEntity("u$it", it * 1000L) }.toMutableList()

        override suspend fun nextBatch(limit: Int): Batch {
            val s = sessions.take(limit)
            val u = unlocks.take(limit - s.size)
            return Batch(s, u)
        }

        override suspend fun markSynced(batch: Batch) {
            sessions.removeAll(batch.sessions.toSet())
            unlocks.removeAll(batch.unlocks.toSet())
        }
    }

    @Test
    fun `sends batches of at most 500 until the store is empty`() = runTest {
        val store = FakeStore(sessions = 1_200, unlocks = 300)
        val sizes = mutableListOf<Int>()
        val outcome = SyncRunner(store) { payload ->
            sizes += payload.sessions.size + payload.unlocks.size
            PostResult.Success
        }.run("dev")

        assertEquals(SyncOutcome.Done(1_500), outcome)
        assertEquals(listOf(500, 500, 500), sizes)
        assertTrue(store.sessions.isEmpty() && store.unlocks.isEmpty())
    }

    @Test
    fun `failure keeps unsent records and stops`() = runTest {
        val store = FakeStore(sessions = 700, unlocks = 0)
        var calls = 0
        val outcome = SyncRunner(store) {
            if (++calls == 1) PostResult.Success else PostResult.HttpError(503)
        }.run("dev")

        assertEquals(SyncOutcome.Failed("HTTP 503", syncedBeforeFailure = 500), outcome)
        assertEquals(200, store.sessions.size)
        assertEquals(2, calls)
    }

    @Test
    fun `network error marks nothing as synced`() = runTest {
        val store = FakeStore(sessions = 3, unlocks = 2)
        val outcome = SyncRunner(store) { PostResult.NetworkError("timeout") }.run("dev")

        assertEquals(SyncOutcome.Failed("timeout", syncedBeforeFailure = 0), outcome)
        assertEquals(3, store.sessions.size)
        assertEquals(2, store.unlocks.size)
    }

    @Test
    fun `a later run after a failure sends everything that was left`() = runTest {
        val store = FakeStore(sessions = 4, unlocks = 1)
        SyncRunner(store) { PostResult.NetworkError("down") }.run("dev")

        val sentIds = mutableListOf<String>()
        val outcome = SyncRunner(store) { payload ->
            sentIds += payload.sessions.map { it.id } + payload.unlocks.map { it.id }
            PostResult.Success
        }.run("dev")

        assertEquals(SyncOutcome.Done(5), outcome)
        assertEquals(listOf("s1", "s2", "s3", "s4", "u1"), sentIds)
    }

    @Test
    fun `empty store sends nothing`() = runTest {
        var called = false
        val outcome = SyncRunner(FakeStore(0, 0)) { called = true; PostResult.Success }.run("dev")
        assertEquals(SyncOutcome.Done(0), outcome)
        assertTrue(!called)
    }
}
