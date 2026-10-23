package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.*

class DesktopAudioTranscoderTest {
    @Test fun `bundled FFmpeg converts WebM to a decodable MP3 with metadata`() = runBlocking {
        val folder = Files.createTempDirectory("mavimusic-transcode-test").toFile()
        try {
            val input = folder.resolve("input.webm")
            val output = folder.resolve("output.mp3")
            runFfmpeg(listOf("-f", "lavfi", "-i", "sine=frequency=440:duration=0.3", "-c:a", "libopus", input.path))
            val song = Song(thumbnailUrl = null, fromAutoplay = false, videoId = "test", title = "Título de prueba", artist = "Artista", albumName = "Álbum")
            val cover = folder.resolve("cover.png")
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", cover)
            assertTrue(DesktopAudioTranscoder.transcodeToMp3(input, output, song, cover))
            assertTrue(output.length() > 1000)
            assertEquals("ID3", output.inputStream().use { String(it.readNBytes(3), Charsets.US_ASCII) })
            runFfmpeg(listOf("-i", output.path, "-f", "null", "-"))
            assertTrue(input.isFile, "The converter must leave source deletion to the download manager")
            val probe = ProcessBuilder(org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.ffprobe::class.java),
                "-v", "error", "-show_entries", "stream=codec_name,bit_rate:format_tags=title,artist,album", output.path)
                .redirectErrorStream(true).start()
            val metadata = probe.inputStream.bufferedReader().readText()
            assertEquals(0, probe.waitFor())
            assertTrue("codec_name=mp3" in metadata, metadata)
            assertTrue("codec_name=mjpeg" in metadata, metadata)
            assertTrue("bit_rate=320000" in metadata, metadata)
            assertTrue("TAG:title=Título de prueba" in metadata, metadata)
            assertTrue("TAG:artist=Artista" in metadata, metadata)
            assertTrue("TAG:album=Álbum" in metadata, metadata)
            assertEquals(listOf("cover.png", "input.webm", "output.mp3"), folder.list()!!.sorted())
        } finally { folder.deleteRecursively() }
    }

    @Test fun `corrupt input fails visibly and preserves an existing destination`() = runBlocking {
        val folder = Files.createTempDirectory("mavimusic-transcode-test").toFile()
        try {
            val input = folder.resolve("bad.webm").apply { writeText("invalid media") }
            val output = folder.resolve("existing.mp3").apply { writeText("keep this") }
            assertFailsWith<IllegalStateException> {
                DesktopAudioTranscoder.transcodeToMp3(input, output, Song(thumbnailUrl = null, fromAutoplay = false, videoId = "test", title = "Test", artist = "Test"))
            }
            assertEquals("keep this", output.readText())
            assertEquals(2, folder.list()!!.size)
        } finally { folder.deleteRecursively() }
    }

    @Test fun `uncached WebP artwork is fetched and embedded in the actual export`() = runBlocking {
        val folder = Files.createTempDirectory("mavimusic-webp-test").toFile()
        val server = com.sun.net.httpserver.HttpServer.create(java.net.InetSocketAddress("127.0.0.1", 0), 0)
        try {
            val image = folder.resolve("cover.png")
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", image)
            val webp = folder.resolve("cover.webp")
            runFfmpeg(listOf("-i", image.path, "-frames:v", "1", webp.path))
            server.createContext("/cover") { exchange ->
                val bytes = webp.readBytes()
                exchange.responseHeaders.add("Content-Type", "image/webp")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
                exchange.close()
            }
            server.start()
            val input = folder.resolve("input.webm")
            runFfmpeg(listOf("-f", "lavfi", "-i", "sine=duration=0.2", "-c:a", "libopus", input.path))
            val song = Song("export-test", "Canción", "Artista", "http://127.0.0.1:${server.address.port}/cover", albumName = "Álbum", trackNumber = 3, fromAutoplay = false)
            val output = folder.resolve("output.mp3")
            assertTrue(DesktopAudioTranscoder.transcodeToMp3(input, output, song))
            val tags = probe(output)
            assertTrue("codec_name=mjpeg" in tags, tags)
            assertTrue("TAG:album=Álbum" in tags, tags)
            assertTrue("TAG:track=3" in tags, tags)
            assertTrue("TAG:title=Canción" in tags, tags)
            assertTrue(folder.listFiles()!!.none { it.name.startsWith(".mavimusic-") })
        } finally { server.stop(0); folder.deleteRecursively() }
    }

    @Test fun `source tags and existing embedded artwork survive MP3 retagging without audio encoding`() = runBlocking {
        val folder = Files.createTempDirectory("mavimusic-retag-test").toFile()
        try {
            val image = folder.resolve("cover.png")
            javax.imageio.ImageIO.write(java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", image)
            val source = folder.resolve("original.mp3")
            runFfmpeg(listOf("-f", "lavfi", "-i", "sine=duration=0.3", "-i", image.path,
                "-map", "0:a", "-map", "1:v", "-c:a", "libmp3lame", "-b:a", "192k", "-c:v", "mjpeg",
                "-disposition:v", "attached_pic", "-id3v2_version", "3", "-metadata", "album=Original Album",
                "-metadata", "date=2024", "-metadata", "genre=Rock", "-metadata", "album_artist=Original Band",
                "-metadata", "track=4", source.path))
            val output = folder.resolve("updated.mp3")
            assertTrue(DesktopAudioTranscoder.transcodeToMp3(source, output,
                Song("tag-test", "Updated Title", "Updated Artist", null, fromAutoplay = false)))
            val tags = probe(output)
            for (value in listOf("codec_name=mjpeg", "TAG:album=Original Album", "TAG:genre=Rock", "TAG:date=2024", "TAG:track=4", "TAG:album_artist=Original Band", "TAG:title=Updated Title")) {
                assertTrue(value in tags, tags)
            }
            assertEquals(audioHash(source), audioHash(output), "Retagging must preserve the compressed audio packets")
        } finally { folder.deleteRecursively() }
    }

    private fun probe(file: java.io.File): String {
        val process = ProcessBuilder(org.bytedeco.javacpp.Loader.load(org.bytedeco.ffmpeg.ffprobe::class.java),
            "-v", "error", "-show_entries", "stream=codec_name:format_tags", file.path).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), text)
        return text
    }

    private fun audioHash(file: java.io.File): String {
        val process = ProcessBuilder(DesktopAudioTranscoder.bundledBinary, "-v", "error", "-i", file.path,
            "-map", "0:a:0", "-c:a", "copy", "-f", "hash", "-hash", "sha256", "-").redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), text)
        return text.trim()
    }

    private fun runFfmpeg(args: List<String>) {
        val log = Files.createTempFile("mavimusic-ffmpeg-test", ".log").toFile()
        val process = ProcessBuilder(listOf(DesktopAudioTranscoder.bundledBinary, "-hide_banner", "-loglevel", "error", "-nostdin", "-y") + args)
            .redirectErrorStream(true).redirectOutput(log).start()
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "FFmpeg did not finish")
            assertEquals(0, process.exitValue(), log.readText())
        } finally { process.destroyForcibly(); log.delete() }
    }
}
