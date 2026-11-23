package com.music.mavimusic.desktop

import com.music.mavimusic.data.innertube.StreamResolver
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.*

/** Explicit network smoke test, without a window, speakers, account or real preferences. */
class DesktopMusicVideoNetworkTest {
    @org.junit.Test(timeout = 90_000)
    fun pictureAndAudioFromPublicMusicVideoCanBeDecoded(): Unit = runBlocking {
        org.junit.Assume.assumeTrue(System.getenv("MAVIMUSIC_TEST_VIDEO_NETWORK") == "1")
        val streams = withTimeout(40_000L) { StreamResolver.resolveVideoStreams("dQw4w9WgXcQ") }
        val picture = DesktopCanvasDecoder(maxEdge = 320, looping = false)
        val audio = DesktopAudioDecoder()
        try {
            picture.open(streams.videoUrl, StreamResolver.mediaHeadersFor(streams.videoUrl)).getOrThrow()
            assertTrue(picture.nextFrame(ByteArray(picture.width * picture.height * 4)))
            assertTrue(picture.seek(10_000))
            assertTrue(picture.nextFrame(ByteArray(picture.width * picture.height * 4)))
            audio.open(streams.audioUrl, StreamResolver.mediaHeadersFor(streams.audioUrl), DesktopPcmFormat(44_100, 2, 2)).getOrThrow()
            assertNotNull(audio.read())
        } finally { picture.close(); audio.close() }
    }
}
