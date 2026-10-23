package com.music.mavimusic.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.bytedeco.javacpp.Loader
import kotlin.test.*

class DesktopLocalMusicTest {
    @Test fun `real MP3 tags cover and audio are preserved on import including unicode paths`() {
        val directory = Files.createTempDirectory("mavimusic-local-test-")
        var artwork: Path? = null
        try {
            val file = directory.resolve("Nombre engañoso 100% á.mp3")
            val cover = directory.resolve("cover.png")
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(24, 24, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", cover.toFile())
            ffmpeg("-f", "lavfi", "-i", "sine=duration=1.2", "-i", cover.toString(),
                "-map", "0:a", "-map", "1:v", "-c:a", "libmp3lame", "-c:v", "mjpeg", "-disposition:v", "attached_pic",
                "-id3v2_version", "3", "-metadata", "title=Mi canción", "-metadata", "artist=Intérprete",
                "-metadata", "album=Álbum local", "-metadata", "track=3/12", file.toString())
            val original = Files.readAllBytes(file)
            val song = with(DesktopLocalMusic) { file.toSong() }
            assertEquals("Mi canción", song.title)
            assertEquals("Intérprete", song.artist)
            assertEquals("Álbum local", song.albumName)
            assertEquals(3, song.trackNumber)
            assertEquals("0:01", song.durationText)
            assertEquals(file.toRealPath().toString(), song.localPath)
            assertEquals(file.toRealPath().toUri().toString(), song.localUri)
            artwork = Path.of(java.net.URI(assertNotNull(song.thumbnailUrl)))
            assertNotNull(javax.imageio.ImageIO.read(artwork.toFile()))
            assertContentEquals(original, Files.readAllBytes(file), "Reading metadata must never modify the MP3")
            assertEquals(file.toRealPath(), DesktopDownloadManager.savedFile(song))
            val decoder = DesktopAudioDecoder()
            try {
                decoder.open(assertNotNull(song.localPath), requested = DesktopPcmFormat(44_100, 2, 2)).getOrThrow()
                assertNotNull(decoder.read())
            } finally { decoder.close() }
            Files.delete(artwork)
            artwork = Path.of(java.net.URI(assertNotNull(with(DesktopLocalMusic) { file.toSong() }.thumbnailUrl)))
            assertTrue(Files.isRegularFile(artwork), "Cleared artwork is reconstructed from the original tags")
        } finally {
            artwork?.let { Files.deleteIfExists(it) }
            directory.toFile().deleteRecursively()
        }
    }

    @Test fun `folder import persists deduplicates paths and keeps album track order`() {
        isolatedLibrary { directory ->
            val album = Files.createDirectories(directory.resolve("album"))
            val second = album.resolve("a.mp3")
            val first = album.resolve("z.mp3")
            for ((file, number) in listOf(second to 2, first to 1)) {
                ffmpeg("-f", "lavfi", "-i", "sine=duration=0.1", "-c:a", "libmp3lame",
                    "-metadata", "album=Album", "-metadata", "track=$number", file.toString())
            }
            Files.writeString(album.resolve("ignore.txt"), "not audio")
            val imported = DesktopLocalMusic.importFiles(listOf(album, first))
            assertEquals(listOf(1, 2), imported.map { it.trackNumber })
            DesktopLocalMusic.importFiles(listOf(first.toUri().let(Path::of)))
            assertEquals(1, DesktopLocalMusic.importedPaths().size)
            assertEquals(2, DesktopLocalMusic.scan().size)
            assertTrue(album.toRealPath() in DesktopLocalMusic.roots())
            val nested = Files.createDirectories(album.resolve("new/disc"))
            ffmpeg("-f", "lavfi", "-i", "sine=duration=0.1", nested.resolve("new.WAV").toString())
            assertEquals(3, DesktopLocalMusic.scan().size, "New tracks in imported folders are included")
            Files.delete(second)
            assertEquals(2, DesktopLocalMusic.scan().size, "Missing files do not leave playable rows")
        }
    }

    @Test fun `untagged files fall back to filenames and corrupt tags do not break scans`() {
        isolatedLibrary { directory ->
            val file = directory.resolve("Artista - Canción.WAV")
            ffmpeg("-f", "lavfi", "-i", "sine=duration=0.1", file.toString())
            val song = DesktopLocalMusic.importFiles(listOf(file)).single()
            assertEquals("Artista", song.artist)
            assertEquals("Canción", song.title)
            assertNull(song.albumName)
            assertNull(song.thumbnailUrl)
            assertTrue(DesktopLocalMusic.supports(Path.of("test.MP3")))
            assertTrue(DesktopLocalMusic.supports(Path.of("test.aiff")))
            assertTrue(DesktopLocalMusic.supports(Path.of("test.opus")))
            assertFalse(DesktopLocalMusic.supports(Path.of("test.mp4")))
            val broken = directory.resolve("broken.mp3")
            Files.writeString(broken, "invalid media")
            assertEquals("broken", with(DesktopLocalMusic) { broken.toSong() }.title)
        }
    }

    @Test fun `Opus stream tags and FLAC tags are read without depending on MP3 headers`() {
        val directory = Files.createTempDirectory("mavimusic-local-codecs-")
        try {
            for ((extension, codec) in listOf("opus" to "libopus", "flac" to "flac")) {
                val file = directory.resolve("audio.$extension")
                ffmpeg("-f", "lavfi", "-i", "sine=duration=0.2", "-c:a", codec,
                    "-metadata", "title=Tagged $extension", "-metadata", "artist=Artist", "-metadata", "album=Release",
                    "-metadata", "track=7", file.toString())
                val song = with(DesktopLocalMusic) { file.toSong() }
                assertEquals("Tagged $extension", song.title)
                assertEquals("Artist", song.artist)
                assertEquals("Release", song.albumName)
                assertEquals(7, song.trackNumber)
            }
        } finally { directory.toFile().deleteRecursively() }
    }

    @Test fun `local file identities never fall through to online music sources`() = runBlocking {
        val song = com.music.mavimusic.data.model.Song("local:/missing.mp3", "Missing", "Local", null, localPath = "/missing.mp3")
        assertNull(DesktopDownloadManager.savedFile(song))
        assertTrue(DesktopMusicSources.resolve(song, null).isFailure)
        assertTrue(DesktopMusicSources.resolveLive(song, null).isFailure)
    }

    @Test fun `file requests remain queued until the UI starts collecting`() = runBlocking {
        val paths = listOf(Path.of("/first.mp3"), Path.of("/second.mp3"))
        DesktopOpenAudio.submit(paths)
        assertEquals(DesktopAudioOpenRequest(paths), withTimeout(1000) { DesktopOpenAudio.requests.first() })
        DesktopOpenAudio.submit(paths, playAfterImport = false)
        assertFalse(withTimeout(1000) { DesktopOpenAudio.requests.first() }.playAfterImport,
            "Adding folders must not interrupt playback")
    }

    private fun isolatedLibrary(block: (Path) -> Unit) {
        val directory = Files.createTempDirectory("mavimusic-local-library-")
        val persistence = DesktopPersistence()
        val keys = listOf("local_imported_paths", "local_imported_folders", DesktopLocalMusic.KEY_LOCAL_MUSIC_FOLDER, DesktopLocalMusic.KEY_FILTER_NON_MUSIC_AUDIO)
        val previous = keys.associateWith { persistence.string(it) }
        try {
            persistence.saveString("local_imported_paths", "[]")
            persistence.saveString("local_imported_folders", "[]")
            persistence.saveBoolean(DesktopLocalMusic.KEY_FILTER_NON_MUSIC_AUDIO, false)
            DesktopLocalMusic.setFolder(directory)
            block(directory)
        } finally {
            previous.forEach { (key, value) -> persistence.saveString(key, value) }
            directory.toFile().deleteRecursively()
        }
    }

    private fun ffmpeg(vararg args: String) {
        val log = Files.createTempFile("mavimusic-local-fixture-", ".log")
        val process = ProcessBuilder(listOf(Loader.load(org.bytedeco.ffmpeg.ffmpeg::class.java), "-y", "-v", "error") + args)
            .redirectErrorStream(true).redirectOutput(log.toFile()).start()
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Fixture generation timed out")
            assertEquals(0, process.exitValue(), Files.readString(log))
        } finally { if (process.isAlive) process.destroyForcibly(); Files.deleteIfExists(log) }
    }
}
