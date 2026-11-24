package com.music.mavimusic.android.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DesktopRemoteClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `pair sends PIN and connects WebSocket receiving state and sending commands`() = runBlocking {
        val stateReceived = CountDownLatch(1)
        val commandReceived = CountDownLatch(1)
        var capturedCommand: String? = null

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"success":true,"token":"tok-123","deviceId":"dev-1"}""")
        )
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(
                        """{
                            "type":"STATE",
                            "state":{
                                "isPlaying":true,
                                "positionMs":12000,
                                "durationMs":210000,
                                "volume":0.75,
                                "shuffle":true,
                                "repeatMode":"ALL",
                                "isLiked":true,
                                "song":{"videoId":"v1","title":"Bohemian Rhapsody","artist":"Queen","durationMs":210000},
                                "outputDevices":[{"id":"out1","name":"Altavoces","type":"speaker","isSelected":true}],
                                "currentOutputId":"out1",
                                "currentOutputName":"Altavoces"
                            }
                        }""".trimIndent()
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    capturedCommand = text
                    commandReceived.countDown()
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }
            })
        )

        val client = DesktopRemoteClient()
        val baseUrl = server.url("").toString().trimEnd('/')
        val result = client.pair(baseUrl, "1234", "Pixel Test")
        assertTrue("Expected pair success: $result", result.isSuccess)
        assertEquals("tok-123", result.getOrNull()?.second)

        val deadline = System.currentTimeMillis() + 3_000L
        while (client.playbackState.value == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(25)
        }
        if (client.playbackState.value != null) stateReceived.countDown()
        assertTrue("Should receive STATE payload over WebSocket", stateReceived.await(1, TimeUnit.SECONDS))

        val st = client.playbackState.value!!
        assertTrue(st.isPlaying)
        assertEquals("Bohemian Rhapsody", st.song?.title)
        assertEquals("Queen", st.song?.artist)
        assertEquals(0.75f, st.volume, 0.01f)
        assertEquals("ALL", st.repeatMode)
        assertEquals(1, st.outputDevices.size)

        assertTrue(client.sendCommand("SEEK", mapOf("positionMs" to 45000L)))
        assertTrue("Server should receive outbound command", commandReceived.await(3, TimeUnit.SECONDS))
        val parsed = Json.parseToJsonElement(capturedCommand!!).jsonObject
        assertEquals("SEEK", parsed["action"]?.jsonPrimitive?.content)
        assertEquals(45000L, parsed["positionMs"]?.jsonPrimitive?.long)

        client.disconnect()
    }

    @Test
    fun `pair reports server error on invalid PIN`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"success":false,"message":"PIN incorrecto."}""")
        )

        val client = DesktopRemoteClient()
        val baseUrl = server.url("").toString().trimEnd('/')
        val result = client.pair(baseUrl, "0000")
        assertTrue(result.isFailure)
        assertEquals("PIN incorrecto.", result.exceptionOrNull()?.message)
        assertTrue(client.connection.value is DesktopRemoteConnection.Error)
    }
}
