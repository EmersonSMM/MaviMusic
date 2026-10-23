package com.my.kizzy.rpc

import com.my.kizzy.gateway.entities.Identify.Companion.toIdentifyPayload
import kotlin.test.*

class DiscordClientIdentityTest {
    @Test fun `macOS uses the same desktop identity for identify and HTTP`() {
        val profile = DiscordClientIdentity.forSystem("Mac OS X")
        assertEquals("Mac OS X", profile.os)
        assertTrue("Macintosh" in profile.userAgent)
        val payload = "synthetic-token".toIdentifyPayload(os = profile.os)
        assertEquals(profile.os, payload.properties.os)
        assertEquals("Discord Client", payload.properties.browser)
        assertEquals("desktop", payload.properties.device)
    }
    @Test fun `Windows fingerprint stays on the Windows desktop platform`() {
        val profile = DiscordClientIdentity.forSystem("Windows 11")
        assertEquals("Windows", profile.os)
        assertTrue("Windows NT" in profile.userAgent)
    }
    @Test fun `Linux fingerprint stays on the Linux desktop platform`() {
        val profile = DiscordClientIdentity.forSystem("Linux")
        assertEquals("Linux", profile.os)
        assertTrue("X11; Linux" in profile.userAgent)
    }
    @Test fun `the current default payload has desktop properties`() {
        val payload = "synthetic-token".toIdentifyPayload()
        assertEquals(DiscordClientIdentity.current.os, payload.properties.os)
        assertFalse("Android" in DiscordClientIdentity.current.userAgent)
        assertEquals("desktop", payload.properties.device)
    }
}
