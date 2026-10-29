package com.music.mavimusic.android.audio

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
class PcmVisualizationSinkTest {
    @Test fun `analysis preserves the input buffer and does not cancel opposite phase stereo`() = runTest {
        val frames = mutableListOf<AudioVisualizationFrame>()
        val sink = PcmVisualizationSink(backgroundScope, StandardTestDispatcher(testScheduler), frames::add)
        sink.flush(48000, 2, C.ENCODING_PCM_16BIT)
        val buffer = ByteBuffer.allocate(4096).order(ByteOrder.LITTLE_ENDIAN)
        repeat(1024) {
            val sample = (sin(2 * PI * 1500 * it / 48000) * 16000).toInt().toShort()
            buffer.putShort(sample)
            buffer.putShort((-sample).toShort())
        }
        buffer.flip()
        val original = buffer.array().copyOf()
        AndroidAudioVisualization.enabled = true
        try {
            sink.handleBuffer(buffer)
            runCurrent()
            assertEquals(0, buffer.position())
            assertEquals(4096, buffer.limit())
            assertArrayEquals(original, buffer.array())
            assertEquals(1, frames.size)
            assertTrue(frames.single().rms > .3f)
        } finally {
            AndroidAudioVisualization.enabled = false
        }
    }

    @Test fun `hidden visualizer does not analyze incoming audio`() = runTest {
        val frames = mutableListOf<AudioVisualizationFrame>()
        val sink = PcmVisualizationSink(backgroundScope, StandardTestDispatcher(testScheduler), frames::add)
        sink.flush(48000, 1, C.ENCODING_PCM_16BIT)
        AndroidAudioVisualization.enabled = false
        val buffer = ByteBuffer.allocate(4096)
        sink.handleBuffer(buffer)
        runCurrent()
        assertEquals(0, buffer.position())
        assertTrue(frames.isEmpty())
    }
}
