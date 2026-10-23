package com.music.mavimusic.desktop

import kotlin.test.*

class DesktopCookieVaultTest {
    @Test fun `a legacy session is removed only after a verified secure write`() {
        var secure: ByteArray? = null
        var legacy: String? = "synthetic-cookie"
        val vault = DesktopCookieVault({ secure }, { secure = it; true }, { legacy }, { legacy = null }, true)
        assertEquals("synthetic-cookie", vault.read())
        assertNull(legacy)
        assertEquals("synthetic-cookie", secure!!.toString(Charsets.UTF_8))
    }

    @Test fun `a locked keychain never exposes the plaintext fallback`() {
        var legacy: String? = "synthetic-cookie"
        val vault = DesktopCookieVault({ null }, { false }, { legacy }, { legacy = null }, true)
        assertFailsWith<IllegalStateException> { vault.read() }
        assertFailsWith<IllegalStateException> { vault.write("new-cookie") }
        assertEquals("synthetic-cookie", legacy)
    }

    @Test fun `an unverified write keeps the recoverable copy`() {
        var legacy: String? = "synthetic-cookie"
        val vault = DesktopCookieVault({ null }, { true }, { legacy }, { legacy = null }, true)
        assertFailsWith<IllegalStateException> { vault.read() }
        assertEquals("synthetic-cookie", legacy)
    }

    @Test fun `a secure session takes precedence and removes a stale plaintext copy`() {
        var legacy: String? = "stale"
        val vault = DesktopCookieVault({ "protected".toByteArray() }, { true }, { legacy }, { legacy = null }, true)
        assertEquals("protected", vault.read())
        assertNull(legacy)
    }

    @Test fun `platforms without a native store retain their existing fallback`() {
        val vault = DesktopCookieVault({ null }, { false }, { "legacy" }, {}, false)
        assertEquals("legacy", vault.read())
        assertFalse(vault.write("new"))
    }

    @Test fun `native macOS keychain round trip and deletion`() {
        org.junit.Assume.assumeTrue("Native Keychain tests require explicit opt-in", DesktopPlatform.isMac && java.lang.Boolean.getBoolean("mavimusic.test.keychain"))
        val account = "integration-test-${java.util.UUID.randomUUID()}"
        try {
            assertNull(DesktopMacKeychain.read(account))
            DesktopMacKeychain.write(account, "synthetic-one".toByteArray())
            assertEquals("synthetic-one", DesktopMacKeychain.read(account)!!.toString(Charsets.UTF_8))
            DesktopMacKeychain.write(account, "synthetic-two".toByteArray())
            assertEquals("synthetic-two", DesktopMacKeychain.read(account)!!.toString(Charsets.UTF_8))
            DesktopMacKeychain.delete(account)
            assertNull(DesktopMacKeychain.read(account))
        } finally { DesktopMacKeychain.delete(account) }
    }
}
