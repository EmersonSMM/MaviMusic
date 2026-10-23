package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Files
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import kotlin.test.*

class DesktopDownloadRecoveryTest {
    private fun song(title: String) = Song(videoId = "local-recovery-test", title = title, artist = "Artista", thumbnailUrl = null, fromAutoplay = false)

    @Test fun `a failed conversion retries local audio without resolving or downloading again`() = runBlocking {
        val directory = Files.createTempDirectory("mavimusic-recovery")
        val persistence = DesktopPersistence()
        val previous = persistence.string("download_folder")
        try {
            persistence.saveString("download_folder", directory.toString())
            val source = directory.resolve("Artista - Recovery.wav").toFile()
            val format = AudioFormat(44100f, 16, 1, true, false)
            AudioInputStream(ByteArrayInputStream(ByteArray(44100)), format, 22050).use {
                AudioSystem.write(it, AudioFileFormat.Type.WAVE, source)
            }
            val saved = DesktopDownloadManager.download(song("Recovery")).getOrThrow()
            assertEquals(DesktopDownloadPaths.target(directory, song("Recovery"), DesktopDownloadContext()).toString(), saved.localPath)
            assertTrue(Files.size(DesktopDownloadPaths.target(directory, song("Recovery"), DesktopDownloadContext())) > 0)
            assertFalse(source.exists())
        } finally { persistence.saveString("download_folder", previous); directory.toFile().deleteRecursively() }
    }

    @Test fun `conversion errors are failed downloads and keep the source for recovery`() = runBlocking {
        val directory = Files.createTempDirectory("mavimusic-recovery")
        val persistence = DesktopPersistence()
        val previous = persistence.string("download_folder")
        try {
            persistence.saveString("download_folder", directory.toString())
            val source = directory.resolve("Artista - Invalid.webm").toFile().apply { writeText("bad audio") }
            assertTrue(DesktopDownloadManager.download(song("Invalid")).isFailure)
            assertTrue(source.exists())
            assertFalse(Files.exists(directory.resolve("Artista - Invalid.mp3")))
        } finally { persistence.saveString("download_folder", previous); directory.toFile().deleteRecursively() }
    }
}
