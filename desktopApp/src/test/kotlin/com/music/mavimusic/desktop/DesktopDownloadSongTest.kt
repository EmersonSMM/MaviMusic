package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.*
import java.io.IOException
import kotlin.test.*

class DesktopDownloadSongTest {
    private val upload = Song("upload", "Artista - Canción (Video Oficial)", "Artista", null)
    @Test fun `a failed optional catalogue search keeps the requested audio and reports its video origin`() = runBlocking {
        val result = DesktopDownloadSong.resolve(upload) { throw IOException("offline") }
        assertEquals(upload.videoId, result.videoId)
        assertEquals(upload.title, result.title)
        assertTrue(result.isVideo && result.isVideoOrigin)
    }
    @Test fun `cancelling the lookup still cancels the download instead of silently falling back`() = runBlocking<Unit> {
        assertFailsWith<CancellationException> {
            DesktopDownloadSong.resolve(upload) { throw CancellationException("cancelled") }
        }
    }
    @Test fun `a successful song lookup uses the catalogue metadata`() = runBlocking {
        val catalogue = upload.copy(videoId = "song", title = "Canción", albumName = "Álbum")
        assertEquals(catalogue, DesktopDownloadSong.resolve(upload) { catalogue })
    }
}
