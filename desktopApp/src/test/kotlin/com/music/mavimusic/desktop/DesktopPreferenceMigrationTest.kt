package com.music.mavimusic.desktop

import java.util.UUID
import java.util.prefs.Preferences
import kotlin.test.*

class DesktopPreferenceMigrationTest {
    @Test fun `settings retain their types and cookies are not copied in plaintext`() {
        val root = Preferences.userRoot().node("migration-test-${UUID.randomUUID()}")
        try {
            val old = root.node("com.music.imported.desktop")
            DesktopPreferenceChunks.write(old, "google_accounts_v1", "[]")
            old.putBoolean("shuffle", true)
            old.putInt("crossfade_seconds", 7)
            DesktopPreferenceChunks.write(old, "google_cookie_test", "synthetic-cookie")
            val importer = DesktopPreferenceImporter()
            val current = importer.open(root)
            assertTrue(current.getBoolean("shuffle", false))
            assertEquals(7, current.getInt("crossfade_seconds", 0))
            assertNull(DesktopPreferenceChunks.read(current, "google_cookie_test"))
            assertEquals("synthetic-cookie", importer.legacyCookie("google_cookie_test"))
            importer.clearLegacyCookie("google_cookie_test")
            assertNull(DesktopPreferenceChunks.read(old, "google_cookie_test"))
            importer.finish(root)
            assertFalse(root.nodeExists("com.music.imported.desktop"))
        } finally { root.removeNode() }
    }

    @Test fun `a longer legacy value never appends stale chunks to current state`() {
        val root = Preferences.userRoot().node("migration-test-${UUID.randomUUID()}")
        try {
            val old = root.node("com.music.imported.desktop")
            DesktopPreferenceChunks.write(old, "queue", "x".repeat(12000))
            val target = root.node("com.music.mavimusic.desktop")
            DesktopPreferenceChunks.write(target, "queue", "current-queue")
            val current = DesktopPreferenceImporter().open(root)
            assertEquals("current-queue", DesktopPreferenceChunks.read(current, "queue"))
            assertEquals("x".repeat(12000), DesktopPreferenceChunks.read(current.node("migration-archive"), "queue"))
        } finally { root.removeNode() }
    }

    @Test fun `both account lists survive migration without replacing the current account`() {
        val root = Preferences.userRoot().node("migration-test-${UUID.randomUUID()}")
        try {
            val old = root.node("com.music.imported.desktop")
            DesktopPreferenceChunks.write(old, "google_accounts_v1", "[{\"accountId\":\"old\"}]")
            val target = root.node("com.music.mavimusic.desktop")
            DesktopPreferenceChunks.write(target, "google_accounts_v1", "[{\"accountId\":\"current\"}]")
            val current = DesktopPreferenceImporter().open(root)
            assertEquals("[{\"accountId\":\"current\"},{\"accountId\":\"old\"}]", DesktopPreferenceChunks.read(current, "google_accounts_v1"))
        } finally { root.removeNode() }
    }
}
