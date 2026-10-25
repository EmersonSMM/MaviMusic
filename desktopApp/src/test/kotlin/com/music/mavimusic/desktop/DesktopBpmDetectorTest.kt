package com.music.mavimusic.desktop

import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopBpmDetectorTest {

    @Test
    fun `tempo normalization handles double and half tempo errors`() {
        // Range should be normalized into [70, 180]
        assertEquals(120f, DesktopBpmDetector.normalize(60f))
        assertEquals(100f, DesktopBpmDetector.normalize(50f))
        assertEquals(140f, DesktopBpmDetector.normalize(140f))
        assertEquals(95f, DesktopBpmDetector.normalize(190f))
        assertEquals(110f, DesktopBpmDetector.normalize(220f))
        assertEquals(128f, DesktopBpmDetector.normalize(128f))
    }

    @Test
    fun `detects 120 bpm rhythm with high confidence`() {
        val sampleRate = 22050
        val durationSeconds = 10
        val totalSamples = sampleRate * durationSeconds
        val samples = FloatArray(totalSamples)

        // 120 BPM = 2 beats per second = beat every 0.5 seconds
        val beatIntervalSamples = (sampleRate * 0.5).toInt()
        var beatSample = 0
        while (beatSample < totalSamples) {
            // Synthesize a sharp transient burst / kick
            for (i in 0 until (sampleRate * 0.05).toInt()) {
                if (beatSample + i < totalSamples) {
                    val decay = 1.0f - (i.toFloat() / (sampleRate * 0.05).toFloat())
                    val tone = sin(2.0 * Math.PI * 60.0 * (i.toDouble() / sampleRate)).toFloat()
                    samples[beatSample + i] += tone * decay
                }
            }
            beatSample += beatIntervalSamples
        }

        val result = DesktopBpmDetector.detectFromPcm(samples, sampleRate)
        assertTrue(result.isReliable, "Calculated 120 BPM rhythm should be reliable")
        assertTrue(result.confidence >= DesktopBpmDetector.RELIABILITY_THRESHOLD)
        assertEquals(120f, result.bpm, 1.5f)
    }

    @Test
    fun `detects 80 bpm rhythm with high confidence`() {
        val sampleRate = 22050
        val durationSeconds = 12
        val totalSamples = sampleRate * durationSeconds
        val samples = FloatArray(totalSamples)

        // 80 BPM = 60 / 80 = 0.75 seconds per beat
        val beatIntervalSamples = (sampleRate * 0.75).toInt()
        var beatSample = 0
        while (beatSample < totalSamples) {
            for (i in 0 until (sampleRate * 0.05).toInt()) {
                if (beatSample + i < totalSamples) {
                    val decay = 1.0f - (i.toFloat() / (sampleRate * 0.05).toFloat())
                    val tone = sin(2.0 * Math.PI * 80.0 * (i.toDouble() / sampleRate)).toFloat()
                    samples[beatSample + i] += tone * decay
                }
            }
            beatSample += beatIntervalSamples
        }

        val result = DesktopBpmDetector.detectFromPcm(samples, sampleRate)
        assertTrue(result.isReliable, "Calculated 80 BPM rhythm should be reliable")
        assertEquals(80f, result.bpm, 1.5f)
    }

    @Test
    fun `random white noise without rhythm is flagged as unreliable`() {
        val sampleRate = 22050
        val totalSamples = sampleRate * 5
        val random = Random(42)
        val noiseSamples = FloatArray(totalSamples) { random.nextFloat() * 2f - 1f }

        val result = DesktopBpmDetector.detectFromPcm(noiseSamples, sampleRate)
        assertFalse(result.isReliable, "Pure white noise must be flagged as unreliable (BPM no fiable)")
        assertEquals("BPM no fiable", result.displayString)
    }

    @Test
    fun `bpm store differentiates reliable vs unreliable records`() {
        val reliableRec = TrackBpmRecord("track_a", 128f, 0.85f, isReliable = true, BpmSource.DSP_ANALYSIS)
        val unreliableRec = TrackBpmRecord("track_b", 92f, 0.15f, isReliable = false, BpmSource.DSP_ANALYSIS)

        DesktopBpmStore.save(reliableRec)
        DesktopBpmStore.save(unreliableRec)

        assertEquals(128f, DesktopBpmStore.getReliableBpm("track_a"))
        assertNull(DesktopBpmStore.getReliableBpm("track_b"))

        val allReliable = DesktopBpmStore.getAllReliable()
        assertTrue(allReliable.containsKey("track_a"))
        assertFalse(allReliable.containsKey("track_b"))
    }

    @Test
    fun `feature flag defaults to false and updates cleanly`() {
        val initial = DesktopBpmAnalyzer.isEnabled()
        try {
            DesktopBpmAnalyzer.setEnabled(false)
            assertFalse(DesktopBpmAnalyzer.isEnabled())
            DesktopBpmAnalyzer.setEnabled(true)
            assertTrue(DesktopBpmAnalyzer.isEnabled())
        } finally {
            DesktopBpmAnalyzer.setEnabled(initial)
        }
    }
}
