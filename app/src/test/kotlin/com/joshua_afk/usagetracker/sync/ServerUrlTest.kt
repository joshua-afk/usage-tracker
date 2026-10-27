package com.joshua_afk.usagetracker.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerUrlTest {
    @Test
    fun `accepts http and https urls including LAN addresses`() {
        assertTrue(isValidServerUrl("http://192.168.1.20:3000/api/usage"))
        assertTrue(isValidServerUrl("https://example.com/sync"))
        assertTrue(isValidServerUrl("  http://nas.local/api  "))
    }

    @Test
    fun `rejects blank, schemeless and non-http urls`() {
        assertFalse(isValidServerUrl(""))
        assertFalse(isValidServerUrl("192.168.1.20:3000"))
        assertFalse(isValidServerUrl("ftp://example.com"))
        assertFalse(isValidServerUrl("not a url"))
    }
}
