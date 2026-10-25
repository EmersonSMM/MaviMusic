package com.music.mavimusic.desktop

import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class DesktopWindowsUpdateIntegrityTest {
    @Test fun `modified and truncated installers are rejected before launching helpers`() {
        val file = Files.createTempFile("mavimusic-installer-test", ".msi")
        try {
            val bytes = "synthetic test MSI".toByteArray()
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            Files.write(file, bytes)
            DesktopWindowsUpdater.verifyInstaller(file, bytes.size.toLong(), hash)
            Files.write(file, ByteArray(bytes.size))
            assertFailsWith<IllegalArgumentException> { DesktopWindowsUpdater.verifyInstaller(file, bytes.size.toLong(), hash) }
            Files.write(file, byteArrayOf(1))
            assertFailsWith<IllegalArgumentException> { DesktopWindowsUpdater.verifyInstaller(file, bytes.size.toLong(), hash) }
        } finally { Files.deleteIfExists(file) }
    }
    @Test fun `malformed and prerelease version strings never trigger an installation`() {
        for (tag in listOf("v1.8.52-beta1", "banana", "v999999999999.0.0", "1.9")) {
            assertFalse(DesktopWindowsUpdater.isNewerVersion(tag, "1.8.51"))
        }
    }
}
