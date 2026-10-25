package com.music.mavimusic.desktop

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopDiagnosticsTest {
    @Test fun `export never contains raw log messages secrets paths or unrelated JVM properties`() {
        val secret = "synthetic-secret-83140"
        System.setProperty("mavimusic.test.private", secret)
        try {
            DesktopDiagnostics.recordLog("stream failed Cookie: SID=$secret Authorization: Bearer $secret https://example.invalid/?token=$secret /Users/private/music/title.mp3")
            val report = DesktopDiagnostics.capture(null)
            assertFalse(secret in report)
            assertFalse("/Users/private" in report)
            assertFalse("example.invalid" in report)
            assertTrue("source-resolution failure" in report)
            assertTrue("os.arch=" in report)
            assertTrue("renderer.actual=not-captured" in report)
        } finally { System.clearProperty("mavimusic.test.private") }
    }

    @Test fun `incident history is bounded and keeps classifications only`() {
        repeat(150) { DesktopDiagnostics.recordLog("decoder failed synthetic-private-track-$it") }
        val events = DesktopDiagnostics.recentIncidents()
        assertEquals(100, events.size)
        assertTrue(events.all { it.category in setOf("window/rendering", "updates", "audio", "source-resolution", "lyrics", "other") })
        assertFalse("synthetic-private-track" in events.toString())
    }

    @Test fun `report replaces a chosen file without leaving partial exports`() {
        val folder = Files.createTempDirectory("mavimusic-diagnostic-test")
        try {
            val target = folder.resolve("report.txt")
            Files.writeString(target, "old report")
            DesktopDiagnostics.write("diagnóstico UTF-8", target)
            assertEquals("diagnóstico UTF-8", Files.readString(target))
            Files.list(folder).use { assertEquals(1, it.count()) }
        } finally { folder.toFile().deleteRecursively() }
    }
}
