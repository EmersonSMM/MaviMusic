package com.music.mavimusic.android.data

import org.junit.Assert.*
import org.junit.Test

class DesktopRemoteAddressTest {
    @Test fun `plain LAN addresses use the actual desktop default port`() {
        assertEquals("http://192.168.1.20:8765/", DesktopRemoteAddress.normalize("192.168.1.20"))
        assertEquals("http://mavimusic.local:8765/", DesktopRemoteAddress.normalize(" mavimusic.local "))
    }

    @Test fun `explicit protocol and port are preserved`() {
        assertEquals("http://192.168.1.20:9000/", DesktopRemoteAddress.normalize("192.168.1.20:9000"))
        assertEquals("https://music.local:8443/", DesktopRemoteAddress.normalize("https://music.local:8443/"))
    }

    @Test fun `IPv6 host is not split at its address colons`() {
        assertEquals("http://[::1]:8765/", DesktopRemoteAddress.normalize("[::1]"))
    }

    @Test fun `invalid addresses do not create a navigation intent`() {
        listOf("", "file:///etc/passwd", "javascript:alert(1)", "http://user:pass@host/", "http://host/path", "http://host/?token=x", "host:99999", "not a host")
            .forEach { address -> assertTrue(address, runCatching { DesktopRemoteAddress.normalize(address) }.isFailure) }
    }
}
