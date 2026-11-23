package com.music.mavimusic.desktop

import java.nio.file.Files
import kotlin.test.*

class DesktopMusicVideoDecoderTest {
    @Test fun fullVideoSeeksUsesTimestampsAndStopsAtEnd() {
        val file = Files.createTempFile("mavi-synthetic-video", ".mp4")
        try {
            javaClass.getResourceAsStream("/video/sync-test.mp4")!!.use { input -> Files.newOutputStream(file).use { input.copyTo(it) } }
            val decoder = DesktopCanvasDecoder(maxEdge = 96, looping = false)
            try {
                decoder.open(file.toString()).getOrThrow()
                assertEquals(96, decoder.width)
                val pixels = ByteArray(decoder.width * decoder.height * 4)
                assertTrue(decoder.nextFrame(pixels))
                assertTrue(decoder.seek(1_500))
                var frames = 0
                while (decoder.nextFrame(pixels)) {
                    frames++
                    check(frames < 40) { "A full music video must not loop like Canvas artwork" }
                }
                assertTrue(frames > 0)
                assertNull(decoder.failure)
                assertTrue(decoder.positionMs >= 2_500)
                assertTrue(decoder.seek(0))
                assertTrue(decoder.nextFrame(pixels))
                assertTrue(decoder.positionMs < 500)
            } finally { decoder.close(); decoder.close() }
        } finally { Files.deleteIfExists(file) }
    }
}
