package com.music.mavimusic.data.presence

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresenceTest {
    private lateinit var server: MockWebServer
    private val id = "3f2b8c1e-9a4d-4e6f-8b7a-1c2d3e4f5a6b"

    @BeforeTest
    fun start() {
        server = MockWebServer()
        repeat(10) { server.enqueue(MockResponse().setBody("""{"intervalSec":300}""")) }
        server.start()
    }

    @AfterTest
    fun stop() = server.shutdown()

    private fun nextPing(timeoutSec: Long = 5): JsonObject? {
        val request = server.takeRequest(timeoutSec, TimeUnit.SECONDS) ?: return null
        assertEquals("POST", request.method)
        assertEquals("/api/presence", request.path)
        assertNull(request.getHeader("Origin"), "the server refuses pings that carry an Origin")
        return Json.parseToJsonElement(request.body.readUtf8()).jsonObject
    }

    private fun JsonObject.isOpen() = getValue("open").jsonPrimitive.boolean

    @Test
    fun opensThenClosesAfterTheDebounce() {
        val presence = Presence(PresencePlatform.Pc, id, server.url("/api/presence").toString())
        presence.setOpen(true)
        assertNull(nextPing(timeoutSec = 1), "disabled telemetry should not send open request")
        presence.setOpen(false)
        assertNull(nextPing(timeoutSec = 1), "disabled telemetry should not send close request")
    }

    @Test
    fun aQuickReopenIsNotAClose() {
        val presence = Presence(PresencePlatform.Pc, id, server.url("/api/presence").toString())
        presence.setOpen(true)
        assertNull(nextPing(timeoutSec = 1))
        presence.setOpen(false)
        presence.setOpen(true)
        assertNull(nextPing(timeoutSec = 1))
    }

    @Test
    fun neverOpenedSendsNothing() {
        val presence = Presence(PresencePlatform.Pc, id, server.url("/api/presence").toString())
        presence.setOpen(false)
        presence.closeBlocking()
        assertNull(nextPing(timeoutSec = 1))
    }

    @Test
    fun closeBlockingSendsOnTheCallingThread() {
        val presence = Presence(PresencePlatform.Pc, id, server.url("/api/presence").toString())
        presence.setOpen(true)
        presence.closeBlocking()
        assertNull(nextPing(timeoutSec = 1), "disabled telemetry sends nothing on close")
    }
}
