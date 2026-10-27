package com.joshua_afk.usagetracker.sync

import com.joshua_afk.usagetracker.data.db.SessionEntity
import com.joshua_afk.usagetracker.data.db.UnlockEntity
import java.time.ZoneOffset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class MappersTest {
    @Test
    fun `formats time with millisecond precision and offset`() {
        assertEquals("1970-01-01T08:00:00.000+08:00", formatTime(0, ZoneOffset.ofHours(8)))
        assertEquals("1970-01-01T00:00:01.500Z", formatTime(1_500, ZoneOffset.UTC))
    }

    @Test
    fun `payload uses the field names from the api contract`() {
        val zone = ZoneOffset.ofHours(8)
        val payload = SyncPayload(
            deviceId = "dev-1",
            sessions = listOf(
                SessionEntity("s1", "com.example", "Example", 0, 405_000, 405).toDto(zone),
            ),
            unlocks = listOf(UnlockEntity("u1", 1_000).toDto(zone)),
        )

        val root = Json.parseToJsonElement(ApiJson.encodeToString(payload)).jsonObject
        assertEquals(setOf("device_id", "sessions", "unlocks"), root.keys)
        assertEquals("dev-1", root.getValue("device_id").jsonPrimitive.content)

        val session = root.getValue("sessions").jsonArray.single().jsonObject
        assertEquals(setOf("id", "package", "app_name", "started_at", "ended_at", "duration_seconds"), session.keys)
        assertEquals("1970-01-01T08:00:00.000+08:00", session.getValue("started_at").jsonPrimitive.content)
        assertEquals("405", session.getValue("duration_seconds").jsonPrimitive.content)

        val unlock = root.getValue("unlocks").jsonArray.single().jsonObject
        assertEquals(setOf("id", "occurred_at"), unlock.keys)
    }
}
