package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.*
import kotlin.test.*

class DesktopDownloadPipelineTest {
    private fun wav(): ByteArray {
        val output = ByteArrayOutputStream()
        AudioInputStream(ByteArrayInputStream(ByteArray(88200)), AudioFormat(44100f, 16, 1, true, false), 44100).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, output)
        }
        return output.toByteArray()
    }

    @Test fun `video to catalogue song to verified playlist MP3 and artwork retry uses existing audio`() = runBlocking {
        val root = Files.createTempDirectory("mavimusic-pipeline")
        val previous = DesktopPersistence().string("download_folder")
        val previousDownloads = DesktopPersistence().downloads()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        val audioRequests = AtomicInteger()
        val bytes = wav()
        val cover = ByteArrayOutputStream().also { javax.imageio.ImageIO.write(java.awt.image.BufferedImage(16, 16, 1), "png", it) }.toByteArray()
        var artworkAvailable = false
        server.createContext("/audio") { ex ->
            audioRequests.incrementAndGet()
            ex.sendResponseHeaders(200, bytes.size.toLong()); ex.responseBody.use { it.write(bytes) }; ex.close()
        }
        server.createContext("/cover") { ex ->
            val payload = if (artworkAvailable) cover else "unavailable".toByteArray()
            ex.sendResponseHeaders(if (artworkAvailable) 200 else 503, payload.size.toLong())
            ex.responseBody.use { it.write(payload) }; ex.close()
        }
        server.start()
        try {
            DesktopPersistence().saveString("download_folder", root.toString())
            val video = Song("music-video", "Rafaga - Menti (Video Oficial)", "Ráfaga", null, isVideo = true)
            val catalogue = video.copy(videoId = "catalogue-audio", title = "Mentí", isVideo = false,
                albumName = "Álbum real", trackNumber = 3, thumbnailUrl = "http://127.0.0.1:${server.address.port}/cover")
            val context = DesktopDownloadContext(DesktopDownloadContext.Kind.PLAYLIST, "Mis favoritas")
            var streamSong: Song? = null
            val services = DesktopDownloadServices(
                song = { DesktopDownloadSong.resolve(it) { catalogue } }, metadata = { it },
                stream = { song, _ -> streamSong = song; Result.success(DesktopStream("http://127.0.0.1:${server.address.port}/audio", DesktopStreamFormat(codec = "wav"))) })
            val stages = mutableListOf<DesktopDownloadStage>()
            val failed = DesktopDownloadManager.downloadUsing(video, "HIGH", context, services, { stages += it.stage })
            assertTrue(failed.isFailure, "Missing known artwork must not publish an incomplete success")
            assertTrue(failed.exceptionOrNull()!!.message!!.contains("Verificando"))
            assertEquals(catalogue.videoId, streamSong?.videoId, "Download the song, not the video upload")
            assertEquals(1, audioRequests.get())
            assertFalse(Files.exists(DesktopDownloadPaths.target(root, catalogue.copy(videoId = video.videoId), context)))
            assertTrue(Files.exists(DesktopDownloadPaths.staging(root, video.videoId, context).resolve("audio.wav")))
            artworkAvailable = true
            stages.clear()
            val saved = DesktopDownloadManager.downloadUsing(video, "HIGH", context, services, { stages += it.stage }).getOrThrow()
            assertEquals(1, audioRequests.get(), "Retry should only repair conversion/artwork")
            assertEquals(video.videoId, saved.videoId)
            assertEquals(root.resolve("Playlists/Mis favoritas"), java.nio.file.Path.of(saved.localPath!!).parent)
            val info = DesktopExportProbe.inspect(java.io.File(saved.localPath!!))
            assertTrue(info.hasAudio && info.hasCover)
            assertEquals("Mentí", info.tags["title"])
            assertEquals("Ráfaga", info.tags["artist"])
            assertEquals("Álbum real", info.tags["album"], "Playlist names must never become album tags")
            assertEquals("3", info.tags["track"])
            assertTrue(kotlin.math.abs(info.duration!! - 1.0) < .1)
            assertFalse(DesktopDownloadStage.TRANSFER in stages)
            assertTrue(DesktopDownloadStage.ARTWORK in stages && DesktopDownloadStage.CONVERTING in stages && DesktopDownloadStage.VERIFYING in stages)
            assertFalse(Files.exists(DesktopDownloadPaths.staging(root, video.videoId, context)))
            assertTrue(DesktopDownloadHistory.recent.value.first().info.hasCover)
            DesktopPersistence().saveDownloads(listOf(saved))
            val artistExport = DesktopDownloadManager.downloadUsing(video, "HIGH", DesktopDownloadContext(), services).getOrThrow()
            assertEquals(1, audioRequests.get(), "A second destination should reuse the saved recording")
            assertTrue(Files.exists(java.nio.file.Path.of(saved.localPath!!)), "Keep the original playlist export")
            assertNotEquals(saved.localPath, artistExport.localPath)
            assertTrue(DesktopExportProbe.inspect(java.io.File(artistExport.localPath!!)).hasCover)
        } finally {
            DesktopPersistence().saveString("download_folder", previous); DesktopPersistence().saveDownloads(previousDownloads); server.stop(0); root.toFile().deleteRecursively()
        }
    }

    @Test fun `cancellation propagates without publishing a download or reporting a normal failure`() = runBlocking {
        val root = Files.createTempDirectory("mavimusic-cancel-pipeline")
        val previous = DesktopPersistence().string("download_folder")
        try {
            DesktopPersistence().saveString("download_folder", root.toString())
            val started = CompletableDeferred<Unit>()
            val song = Song("cancel", "Canción", "Artista", null)
            val services = DesktopDownloadServices(song = { it }, metadata = { it }, stream = { _, _ ->
                started.complete(Unit); awaitCancellation()
            })
            val job = async { DesktopDownloadManager.downloadUsing(song, "HIGH", DesktopDownloadContext(), services) }
            started.await(); job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertFalse(Files.exists(DesktopDownloadPaths.target(root, song, DesktopDownloadContext())))
        } finally { DesktopPersistence().saveString("download_folder", previous); root.toFile().deleteRecursively() }
    }

    @Test fun `catalogue songs avoid the video search and unmatched videos retain their requested audio`() = runBlocking {
        val song = Song("song", "Mentí", "Ráfaga", null)
        assertEquals(song, DesktopDownloadSong.resolve(song) { error("A song needs no extra search") })
        val video = song.copy(videoId = "video", isVideo = true)
        assertEquals(video.copy(isVideoOrigin = true), DesktopDownloadSong.resolve(video) { it })
    }
}
