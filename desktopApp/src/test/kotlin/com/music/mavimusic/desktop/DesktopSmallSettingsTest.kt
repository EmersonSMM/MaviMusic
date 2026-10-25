package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.HomeShelf
import com.music.mavimusic.data.model.ShelfItem
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The small Settings toggles ported from Android, each judged on what it actually changes. */
class DesktopSmallSettingsTest {

    // ── Filter non-music audio ────────────────────────────────────────────

    private val scratch = Files.createTempDirectory("mavimusic-filter")

    @AfterTest
    fun cleanUp() {
        scratch.toFile().deleteRecursively()
    }

    private fun file(name: String, bytes: Int): java.nio.file.Path {
        val path = scratch.resolve(name)
        Files.createDirectories(path.parent)
        Files.write(path, ByteArray(bytes))
        return path
    }

    @Test
    fun `an ordinary track survives the filter`() {
        assertTrue(DesktopLocalMusic.isMusic(file("Artist - Song.mp3", 4_000_000)))
    }

    @Test
    fun `a WAV is dropped however big it is`() {
        assertFalse(DesktopLocalMusic.isMusic(file("Artist - Song.wav", 40_000_000)))
    }

    @Test
    fun `a file too small to hold thirty seconds is dropped`() {
        assertFalse(DesktopLocalMusic.isMusic(file("blip.mp3", 4_000)))
    }

    @Test
    fun `ringtones, alarms, notifications and recordings are dropped`() {
        listOf(
            "Ringtones/Marimba.mp3",
            "Notifications/ping.mp3",
            "Alarms/wake.mp3",
            "Recordings/meeting.mp3",
            "my voice note 3.mp3",
        ).forEach { name ->
            assertFalse(DesktopLocalMusic.isMusic(file(name, 4_000_000)), name)
        }
    }

    // ── Phase 1 Feature Flags Persistence Tests ─────────────────────────────

    @Test
    fun `fase 1 settings retain their configured values`() {
        val persistence = DesktopPersistence()

        // 1. Predictive buffer
        persistence.savePredictiveBufferEnabled(false)
        assertFalse(persistence.predictiveBufferEnabled())
        persistence.savePredictiveBufferEnabled(true)
        assertTrue(persistence.predictiveBufferEnabled())

        // 2. Smart Trim
        persistence.saveSmartTrimEnabled(true)
        assertTrue(persistence.smartTrimEnabled())
        persistence.saveSmartTrimEnabled(false)
        assertFalse(persistence.smartTrimEnabled())

        // 3. Global Hotkeys
        persistence.saveGlobalHotkeysEnabled(true)
        assertTrue(persistence.globalHotkeysEnabled())
        persistence.saveGlobalHotkeysModifier("CTRL_OPT")
        assertEquals("CTRL_OPT", persistence.globalHotkeysModifier())
        persistence.saveGlobalHotkeysEnabled(false)
        assertFalse(persistence.globalHotkeysEnabled())

        // 4. Menubar Mini Player
        persistence.saveMenubarMiniPlayerEnabled(false)
        assertFalse(persistence.menubarMiniPlayerEnabled())
        persistence.saveMenubarMiniPlayerEnabled(true)
        assertTrue(persistence.menubarMiniPlayerEnabled())
    }

}
