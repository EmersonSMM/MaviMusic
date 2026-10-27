package com.music.mavimusic.desktop

import com.music.mavimusic.desktop.remote.DesktopRemoteBridge
import com.music.mavimusic.desktop.remote.DesktopRemoteDiscovery
import com.music.mavimusic.desktop.remote.DesktopRemoteSecurity
import com.music.mavimusic.desktop.remote.DesktopRemoteServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.net.InetAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopRemoteTest {

    @BeforeTest
    fun setup() {
        DesktopRemoteSecurity.revokeAllDevices()
        DesktopRemoteServer.stop()
    }

    @AfterTest
    fun tearDown() {
        DesktopRemoteServer.stop()
    }

    @Test
    fun `private LAN IPs are accepted and public IPs are strictly rejected`() {
        // LAN & Loopback addresses
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("127.0.0.1")))
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("192.168.1.50")))
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("10.0.0.1")))
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("172.16.0.1")))
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("172.31.255.255")))
        assertTrue(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("169.254.1.1")))

        // Public IPs (must be rejected!)
        assertFalse(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("8.8.8.8")))
        assertFalse(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("1.1.1.1")))
        assertFalse(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("93.184.216.34")))
        assertFalse(DesktopRemoteSecurity.isPrivateNetwork(InetAddress.getByName("172.32.0.1")))
    }

    @Test
    fun `pairing with valid PIN succeeds and returns token`() {
        val pin = DesktopRemoteSecurity.getPin()
        val result = DesktopRemoteSecurity.pair(pinAttempt = pin, otpAttempt = null, deviceName = "iPhone de Prueba")

        assertTrue(result is DesktopRemoteSecurity.PairResult.Success)
        assertNotNull(result.token)
        assertEquals("iPhone de Prueba", result.deviceName)

        // Session is validated
        val session = DesktopRemoteSecurity.validateToken(result.token)
        assertNotNull(session)
        assertEquals("iPhone de Prueba", session.name)
    }

    @Test
    fun `pairing with invalid PIN fails and locks out after multiple attempts`() {
        for (i in 1..4) {
            val res = DesktopRemoteSecurity.pair(pinAttempt = "0000_invalid", otpAttempt = null, deviceName = "Hacker")
            assertTrue(res is DesktopRemoteSecurity.PairResult.Error)
        }

        // 5th failed attempt triggers lockout
        val lockoutRes = DesktopRemoteSecurity.pair(pinAttempt = "0000_invalid", otpAttempt = null, deviceName = "Hacker")
        assertTrue(lockoutRes is DesktopRemoteSecurity.PairResult.Error)

        // Even correct PIN is now locked out
        val pin = DesktopRemoteSecurity.getPin()
        val duringLockout = DesktopRemoteSecurity.pair(pinAttempt = pin, otpAttempt = null, deviceName = "Legit")
        assertTrue(duringLockout is DesktopRemoteSecurity.PairResult.Error)
        assertTrue(duringLockout.message.contains("Bloqueado"))
    }

    @Test
    fun `OTP token allows one-time pairing and is immediately invalidated`() {
        val otp = DesktopRemoteSecurity.getOtp()
        assertNotNull(otp)

        val pair1 = DesktopRemoteSecurity.pair(pinAttempt = null, otpAttempt = otp, deviceName = "QR Scanner 1")
        assertTrue(pair1 is DesktopRemoteSecurity.PairResult.Success)

        // Second attempt with same OTP must fail
        val pair2 = DesktopRemoteSecurity.pair(pinAttempt = null, otpAttempt = otp, deviceName = "QR Scanner Replay")
        assertTrue(pair2 is DesktopRemoteSecurity.PairResult.Error)
    }

    @Test
    fun `revoking device terminates validity of session token`() {
        val pin = DesktopRemoteSecurity.getPin()
        val pair = DesktopRemoteSecurity.pair(pinAttempt = pin, otpAttempt = null, deviceName = "iPhone") as DesktopRemoteSecurity.PairResult.Success

        // Token is valid initially
        assertNotNull(DesktopRemoteSecurity.validateToken(pair.token))

        // Revoke device
        DesktopRemoteSecurity.revokeDevice(pair.deviceId)

        // Token is now completely invalid
        assertNull(DesktopRemoteSecurity.validateToken(pair.token))
    }

    @Test
    fun `native QR code bitmap generator produces valid Compose image`() {
        val pairingUrl = DesktopRemoteDiscovery.pairingUrl()
        val qrBitmap = DesktopRemoteDiscovery.generateQrBitmap(pairingUrl, 180)

        assertNotNull(qrBitmap)
        assertEquals(180, qrBitmap.width)
        assertEquals(180, qrBitmap.height)
    }

    @Test
    fun `bridge dispatches all remote commands correctly`() {
        var playPauseCalled = false
        var nextCalled = false
        var prevCalled = false
        var seekPos: Long? = null
        var setVol: Float? = null
        var shuffleCalled = false
        var repeatCalled = false
        var likeCalled = false
        var queueIdx: Int? = null

        DesktopRemoteBridge.onPlayPause = { playPauseCalled = true }
        DesktopRemoteBridge.onNext = { nextCalled = true }
        DesktopRemoteBridge.onPrevious = { prevCalled = true }
        DesktopRemoteBridge.onSeekTo = { seekPos = it }
        DesktopRemoteBridge.onSetVolume = { setVol = it }
        DesktopRemoteBridge.onToggleShuffle = { shuffleCalled = true }
        DesktopRemoteBridge.onCycleRepeat = { repeatCalled = true }
        DesktopRemoteBridge.onToggleLike = { likeCalled = true }
        DesktopRemoteBridge.onPlayQueueIndex = { queueIdx = it }

        DesktopRemoteBridge.dispatchCommand("PLAY_PAUSE", emptyMap())
        assertTrue(playPauseCalled)

        DesktopRemoteBridge.dispatchCommand("NEXT", emptyMap())
        assertTrue(nextCalled)

        DesktopRemoteBridge.dispatchCommand("PREVIOUS", emptyMap())
        assertTrue(prevCalled)

        DesktopRemoteBridge.dispatchCommand("SEEK", mapOf("positionMs" to 45000L))
        assertEquals(45000L, seekPos)

        DesktopRemoteBridge.dispatchCommand("SET_VOLUME", mapOf("volume" to 0.75f))
        assertEquals(0.75f, setVol)

        DesktopRemoteBridge.dispatchCommand("TOGGLE_SHUFFLE", emptyMap())
        assertTrue(shuffleCalled)

        DesktopRemoteBridge.dispatchCommand("CYCLE_REPEAT", emptyMap())
        assertTrue(repeatCalled)

        DesktopRemoteBridge.dispatchCommand("TOGGLE_LIKE", emptyMap())
        assertTrue(likeCalled)

        DesktopRemoteBridge.dispatchCommand("PLAY_QUEUE_INDEX", mapOf("index" to 3))
        assertEquals(3, queueIdx)
    }

    @Test
    fun `embedded server serves static web app and pairing API over HTTP`() = runBlocking {
        val testPort = 8792
        DesktopRemoteServer.start(testPort)
        delay(300L) // Wait for server to bind

        assertTrue(DesktopRemoteServer.isRunning())

        val client = HttpClient(CIO)
        try {
            val baseUrl = "http://127.0.0.1:${DesktopRemoteDiscovery.activePort.value}"

            // 1. Static web app HTML
            val resIndex = client.get("$baseUrl/")
            assertEquals(HttpStatusCode.OK, resIndex.status)
            val html = resIndex.bodyAsText()
            assertTrue(html.contains("MaviMusic Remote"))
            assertTrue(html.contains("id=\"view-player\""))

            // 2. Unauthenticated state request should return 401
            val resStateUnauth = client.get("$baseUrl/api/state")
            assertEquals(HttpStatusCode.Unauthorized, resStateUnauth.status)

            // 3. Pair via API
            val pin = DesktopRemoteSecurity.getPin()
            val javaClient = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_1_1)
                .build()
            val pairReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("$baseUrl/api/pair?pin=$pin&deviceName=Test+Phone"))
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                .build()
            val pairRes = javaClient.send(pairReq, java.net.http.HttpResponse.BodyHandlers.ofString())
            val pairBody = pairRes.body()
            assertEquals(200, pairRes.statusCode())
            assertTrue(pairBody.contains("\"success\":true"))

            // Extract token from response
            val tokenMatch = Regex(""""token":"([^"]+)"""").find(pairBody)
            assertNotNull(tokenMatch)
            val token = tokenMatch.groupValues[1]

            // 4. Authenticated state request should succeed
            val stateReq = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("$baseUrl/api/state"))
                .header("Authorization", "Bearer $token")
                .GET()
                .build()
            val stateRes = javaClient.send(stateReq, java.net.http.HttpResponse.BodyHandlers.ofString())
            assertEquals(200, stateRes.statusCode())
        } finally {
            client.close()
            DesktopRemoteServer.stop()
        }

        assertFalse(DesktopRemoteServer.isRunning())
    }
}
