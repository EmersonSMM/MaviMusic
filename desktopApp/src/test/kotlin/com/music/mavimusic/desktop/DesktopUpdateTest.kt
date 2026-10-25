package com.music.mavimusic.desktop

import java.nio.file.Files
import kotlin.test.*

class DesktopUpdateTest {
    @Test fun `restart checkpoint restores only the same song once`() {
        DesktopUpdateResume.save("track", 12345L)
        assertEquals(DesktopUpdateResume.Checkpoint("track", 12345L), DesktopUpdateResume.consume("track"))
        assertNull(DesktopUpdateResume.consume("track"))
        DesktopUpdateResume.save("previous", 900L)
        assertNull(DesktopUpdateResume.consume("different"))
        assertNull(DesktopUpdateResume.consume("previous"))
    }

    @Test fun `restart checkpoint rejects corrupt state and clamps negative positions`() {
        DesktopUpdateResume.save("track", -40L)
        assertEquals(0L, assertNotNull(DesktopUpdateResume.consume("track")).positionMs)
        DesktopPersistence().saveString("update_restart_checkpoint", "invalid json")
        assertNull(DesktopUpdateResume.consume("track"))
        DesktopUpdateResume.save(null, 120L)
        assertNull(DesktopUpdateResume.consume(null))
    }

    @Test fun `native updater loads only from an installed app layout`() {
        val folder = Files.createTempDirectory("mavimusic-update-path-")
        try {
            assertNull(DesktopMacUpdater.libraryPath(null))
            assertNull(DesktopMacUpdater.libraryPath(folder.resolve("java")))
            val launcher = folder.resolve("MaviMusic.app/Contents/MacOS/MaviMusic")
            assertNull(DesktopMacUpdater.libraryPath(launcher))
            val library = launcher.parent.parent.resolve("Frameworks/libmavimusic_updates.dylib")
            Files.createDirectories(library.parent)
            Files.write(library, byteArrayOf(1))
            assertEquals(library, DesktopMacUpdater.libraryPath(launcher))
        } finally { folder.toFile().deleteRecursively() }
    }

    @Test fun `windows updater detects newer semver versions correctly`() {
        assertTrue(DesktopWindowsUpdater.isNewerVersion("v1.8.50", "1.8.49"))
        assertTrue(DesktopWindowsUpdater.isNewerVersion("1.8.50", "1.8.49"))
        assertTrue(DesktopWindowsUpdater.isNewerVersion("v1.9.0", "1.8.50"))
        assertTrue(DesktopWindowsUpdater.isNewerVersion("2.0.0", "1.8.50"))
        assertFalse(DesktopWindowsUpdater.isNewerVersion("v1.8.49", "1.8.49"))
        assertFalse(DesktopWindowsUpdater.isNewerVersion("1.8.48", "1.8.49"))
        assertFalse(DesktopWindowsUpdater.isNewerVersion("v1.8.40", "1.8.49"))
        assertFalse(DesktopWindowsUpdater.isNewerVersion("v1.8.50", "development"))
    }
}
