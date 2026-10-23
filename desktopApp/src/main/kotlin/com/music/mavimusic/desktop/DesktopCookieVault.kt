package com.music.mavimusic.desktop

/** Migrates a plaintext session only after a verified write to the native store. */
internal class DesktopCookieVault(
    private val readSecure: () -> ByteArray?,
    private val writeSecure: (ByteArray) -> Boolean,
    private val readLegacy: () -> String?,
    private val clearLegacy: () -> Unit,
    private val requireSecure: Boolean,
) {
    fun read(): String? {
        readSecure()?.toString(Charsets.UTF_8)?.takeIf(String::isNotBlank)?.let {
            clearLegacy()
            return it
        }
        val legacy = readLegacy()?.takeIf(String::isNotBlank) ?: return null
        if (writeSecure(legacy.toByteArray(Charsets.UTF_8))) {
            check(readSecure()?.toString(Charsets.UTF_8) == legacy) { "Could not verify the protected session" }
            clearLegacy()
            return legacy
        }
        check(!requireSecure) { "Unlock Keychain to protect and restore this session" }
        return legacy
    }

    fun write(cookie: String): Boolean {
        if (!writeSecure(cookie.toByteArray(Charsets.UTF_8))) {
            check(!requireSecure) { "Could not save the session in Keychain. Unlock Keychain and try again" }
            return false
        }
        check(readSecure()?.toString(Charsets.UTF_8) == cookie) { "Could not verify the protected session" }
        clearLegacy()
        return true
    }
}
