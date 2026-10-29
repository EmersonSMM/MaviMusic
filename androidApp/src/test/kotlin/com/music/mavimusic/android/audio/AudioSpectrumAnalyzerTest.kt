package com.music.mavimusic.android.audio

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class AudioSpectrumAnalyzerTest {
    private fun tone(hz: Int, amplitude: Float = 0.5f) = FloatArray(1024) {
        (sin(2 * PI * hz * it / 48000) * amplitude).toFloat()
    }

    @Test fun `silence produces no spectrum energy or waveform`() {
        val frame = AudioSpectrumAnalyzer().analyze(FloatArray(1024), 48000)
        assertTrue(frame.spectrum.all { it == 0f })
        assertTrue(frame.waveform.all { it == 0f })
        assertEquals(0f, frame.rms, 0f)
    }

    @Test fun `sine RMS has the expected physical amplitude`() {
        val frame = AudioSpectrumAnalyzer().analyze(tone(1500), 48000)
        assertEquals(0.5f / sqrt(2f), frame.rms, 0.001f)
        assertEquals(128, frame.waveform.size)
        assertEquals(32, frame.spectrum.size)
    }

    @Test fun `high tones excite higher spectrum bands than bass tones`() {
        val analyzer = AudioSpectrumAnalyzer()
        val bass = analyzer.analyze(tone(187), 48000).spectrum
        val treble = analyzer.analyze(tone(6000), 48000).spectrum
        assertTrue(treble.indices.maxBy { treble[it] } > bass.indices.maxBy { bass[it] } + 10)
    }

    @Test fun `quieter input produces lower energy and spectral level`() {
        val analyzer = AudioSpectrumAnalyzer()
        val quiet = analyzer.analyze(tone(1500, 0.05f), 48000)
        val loud = analyzer.analyze(tone(1500, 0.8f), 48000)
        assertTrue(loud.rms > quiet.rms * 10)
        assertTrue(loud.spectrum.max() > quiet.spectrum.max())
        assertTrue(loud.spectrum.all { it in 0f..1f })
    }

    @Test fun `DC offset is excluded from the spectrum`() {
        val frame = AudioSpectrumAnalyzer().analyze(FloatArray(1024) { .4f }, 48000)
        assertTrue(frame.spectrum.all { it == 0f })
    }
}
