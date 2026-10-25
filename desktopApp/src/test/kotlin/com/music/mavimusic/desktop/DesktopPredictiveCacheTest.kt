package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopPredictiveCacheTest {

    private val persistence = DesktopPersistence()
    private val originalEnabled = persistence.predictiveBufferEnabled()

    @BeforeTest
    fun setUp() {
        DesktopPredictiveCache.clear()
        persistence.savePredictiveBufferEnabled(true)
    }

    @AfterTest
    fun tearDown() {
        persistence.savePredictiveBufferEnabled(originalEnabled)
        DesktopPredictiveCache.clear()
    }

    @Test
    fun isEnabledMatchesPersistence() {
        persistence.savePredictiveBufferEnabled(true)
        assertTrue(DesktopPredictiveCache.isEnabled())

        persistence.savePredictiveBufferEnabled(false)
        assertFalse(DesktopPredictiveCache.isEnabled())
    }

    @Test
    fun cachedFileReturnsNullWhenDisabledEvenIfFileExists() {
        val song = Song(
            videoId = "test_vid_123",
            title = "Test Song",
            artist = "Test Artist",
            thumbnailUrl = null,
        )
        val file = DesktopPredictiveCache.directory.resolve("test_vid_123.audio")
        // Write 70 KB mock file (> 64 KB minimum threshold)
        val bytes = ByteArray(70 * 1024) { 0x55.toByte() }
        Files.write(file, bytes)

        persistence.savePredictiveBufferEnabled(true)
        assertNotNull(DesktopPredictiveCache.cachedFile(song))

        persistence.savePredictiveBufferEnabled(false)
        assertNull(DesktopPredictiveCache.cachedFile(song))
    }

    @Test
    fun localFilesAndBlanksAreNeverCached() {
        persistence.savePredictiveBufferEnabled(true)
        val localSong = Song(
            videoId = "local:my_track.mp3",
            title = "Local Track",
            artist = "Local Artist",
            thumbnailUrl = null,
        )
        assertNull(DesktopPredictiveCache.cachedFile(localSong))

        val emptySong = Song(
            videoId = "",
            title = "Empty",
            artist = "Nobody",
            thumbnailUrl = null,
        )
        assertNull(DesktopPredictiveCache.cachedFile(emptySong))
    }

    @Test
    fun cachedStreamProvidesValidPathUrl() {
        val song = Song(
            videoId = "test_stream_abc",
            title = "Stream Test",
            artist = "Artist",
            thumbnailUrl = null,
        )
        val file = DesktopPredictiveCache.directory.resolve("test_stream_abc.audio")
        val bytes = ByteArray(65 * 1024) { 0x11.toByte() }
        Files.write(file, bytes)

        val stream = DesktopPredictiveCache.cachedStream(song)
        assertNotNull(stream)
        assertEquals(file.toAbsolutePath().toString(), stream.url)
    }

    @Test
    fun lruTrimmingRemovesOldestFiles() {
        val f1 = DesktopPredictiveCache.directory.resolve("track_1.audio")
        val f2 = DesktopPredictiveCache.directory.resolve("track_2.audio")
        Files.write(f1, ByteArray(100 * 1024))
        Files.write(f2, ByteArray(100 * 1024))

        // Set f1 older than f2
        Files.setLastModifiedTime(f1, java.nio.file.attribute.FileTime.fromMillis(1_000L))
        Files.setLastModifiedTime(f2, java.nio.file.attribute.FileTime.fromMillis(2_000L))

        // Custom trim with maxAllowedBytes = 150 KB -> should evict f1 (100KB) so total is under 150KB
        DesktopPredictiveCache.trim(maxAllowedBytes = 150 * 1024L)

        assertFalse(Files.exists(f1), "Oldest file f1 should be trimmed")
        assertTrue(Files.exists(f2), "Newer file f2 should remain")
    }
}
