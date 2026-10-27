package com.joshua_afk.usagetracker.sync

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncClientTest {
    private lateinit var server: MockWebServer
    private val client = SyncClient()
    private val payload = SyncPayload("dev-1", emptyList(), listOf(UnlockDto("u1", "1970-01-01T00:00:01.000Z")))

    @Before fun setUp() { server = MockWebServer().apply { start() } }
    @After fun tearDown() { runCatching { server.shutdown() } }

    private fun url() = server.url("/api/usage").toString()

    @Test
    fun `posts json with bearer token and treats 2xx as success`() {
        server.enqueue(MockResponse().setResponseCode(204))

        assertEquals(PostResult.Success, client.post(url(), "secret", payload))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/usage", request.path)
        assertEquals("Bearer secret", request.getHeader("Authorization"))
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertTrue(request.body.readUtf8().contains("\"device_id\":\"dev-1\""))
    }

    @Test
    fun `non-2xx responses are http errors`() {
        server.enqueue(MockResponse().setResponseCode(401))
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(PostResult.HttpError(401), client.post(url(), "bad", payload))
        assertEquals(PostResult.HttpError(500), client.post(url(), "tok", payload))
    }

    @Test
    fun `unreachable server is a network error`() {
        val dead = url()
        server.shutdown()
        assertTrue(client.post(dead, "tok", payload) is PostResult.NetworkError)
    }

    @Test
    fun `malformed url is a network error and does not throw`() {
        assertEquals(PostResult.NetworkError("Invalid server URL"), client.post("not a url", "tok", payload))
    }
}
