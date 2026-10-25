package com.music.mavimusic.desktop

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopKaraokeTest {

    @Test
    fun testSettingsAndActiveState() {
        val initialFeature = DesktopKaraokeSettings.isFeatureEnabled()
        val initialActive = DesktopKaraokeSettings.isActive.value
        val initialAtt = DesktopKaraokeSettings.attenuation.value

        try {
            DesktopKaraokeSettings.setFeatureEnabled(true)
            assertTrue(DesktopKaraokeSettings.isFeatureEnabled())

            DesktopKaraokeSettings.setActive(true)
            assertTrue(DesktopKaraokeSettings.isActive.value)

            // Disabling feature should automatically turn off active karaoke
            DesktopKaraokeSettings.setFeatureEnabled(false)
            assertFalse(DesktopKaraokeSettings.isFeatureEnabled())
            assertFalse(DesktopKaraokeSettings.isActive.value)

            // Cannot activate when feature is disabled
            DesktopKaraokeSettings.setActive(true)
            assertFalse(DesktopKaraokeSettings.isActive.value)

            // Attenuation clamping
            DesktopKaraokeSettings.setAttenuation(-0.5f)
            assertEquals(0f, DesktopKaraokeSettings.attenuation.value, 1e-4f)

            DesktopKaraokeSettings.setAttenuation(1.5f)
            assertEquals(1f, DesktopKaraokeSettings.attenuation.value, 1e-4f)

            DesktopKaraokeSettings.setAttenuation(0.85f)
            assertEquals(0.85f, DesktopKaraokeSettings.attenuation.value, 1e-4f)
        } finally {
            DesktopKaraokeSettings.setFeatureEnabled(initialFeature)
            DesktopKaraokeSettings.setActive(initialActive)
            DesktopKaraokeSettings.setAttenuation(initialAtt)
        }
    }

    @Test
    fun testExactPassthroughWhenInactive() {
        val processor = DesktopKaraokeProcessor()
        val initialFeature = DesktopKaraokeSettings.isFeatureEnabled()
        val initialActive = DesktopKaraokeSettings.isActive.value

        try {
            DesktopKaraokeSettings.setFeatureEnabled(true)
            DesktopKaraokeSettings.setActive(false)

            val samples = FloatArray(100) { (it - 50) * 0.015f }
            val original = samples.clone()

            processor.process(samples, samples.size, 44100, 2)

            for (i in samples.indices) {
                assertEquals(original[i], samples[i], 1e-6f, "Samples must remain byte-identical when inactive")
            }
        } finally {
            DesktopKaraokeSettings.setFeatureEnabled(initialFeature)
            DesktopKaraokeSettings.setActive(initialActive)
        }
    }

    @Test
    fun testCenterVocalAttenuatedWhileBassAndSidesPreserved() {
        val processor = DesktopKaraokeProcessor()
        val initialFeature = DesktopKaraokeSettings.isFeatureEnabled()
        val initialActive = DesktopKaraokeSettings.isActive.value
        val initialAtt = DesktopKaraokeSettings.attenuation.value

        try {
            DesktopKaraokeSettings.setFeatureEnabled(true)
            DesktopKaraokeSettings.setActive(true)
            DesktopKaraokeSettings.setAttenuation(0.95f)

            val sampleRate = 44100
            val numFrames = 2048
            val count = numFrames * 2

            // Case A: Centered vocal tone at 1000 Hz (identical on L and R)
            val vocalSamples = FloatArray(count)
            for (f in 0 until numFrames) {
                val t = f.toDouble() / sampleRate
                val s = sin(2.0 * PI * 1000.0 * t).toFloat() * 0.7f
                vocalSamples[f * 2] = s
                vocalSamples[f * 2 + 1] = s
            }
            val originalVocalRms = calculateRms(vocalSamples)

            processor.process(vocalSamples, count, sampleRate, 2)
            val processedVocalRms = calculateRms(vocalSamples)

            assertTrue(
                processedVocalRms < originalVocalRms * 0.40f,
                "Center vocal at 1 kHz must be attenuated by at least 60% (got ${processedVocalRms / originalVocalRms})"
            )

            // Case B: Sub-bass tone at 60 Hz (identical on L and R) - should NOT be cancelled
            processor.reset()
            val bassSamples = FloatArray(count)
            for (f in 0 until numFrames) {
                val t = f.toDouble() / sampleRate
                val s = sin(2.0 * PI * 60.0 * t).toFloat() * 0.6f
                bassSamples[f * 2] = s
                bassSamples[f * 2 + 1] = s
            }
            val originalBassRms = calculateRms(bassSamples)

            processor.process(bassSamples, count, sampleRate, 2)
            val processedBassRms = calculateRms(bassSamples)

            assertTrue(
                processedBassRms > originalBassRms * 0.75f,
                "Sub-bass at 60 Hz must be preserved above 75% power (got ${processedBassRms / originalBassRms})"
            )

            // Case C: Pure side signal (L = -R) at 1000 Hz - center is 0, so side signal must remain intact
            processor.reset()
            val sideSamples = FloatArray(count)
            for (f in 0 until numFrames) {
                val t = f.toDouble() / sampleRate
                val s = sin(2.0 * PI * 1000.0 * t).toFloat() * 0.5f
                sideSamples[f * 2] = s
                sideSamples[f * 2 + 1] = -s
            }
            val originalSideRms = calculateRms(sideSamples)

            processor.process(sideSamples, count, sampleRate, 2)
            val processedSideRms = calculateRms(sideSamples)

            assertTrue(
                abs(processedSideRms - originalSideRms) < originalSideRms * 0.25f,
                "Stereo side signal should not be cancelled"
            )
        } finally {
            processor.reset()
            DesktopKaraokeSettings.setFeatureEnabled(initialFeature)
            DesktopKaraokeSettings.setActive(initialActive)
            DesktopKaraokeSettings.setAttenuation(initialAtt)
        }
    }

    private fun calculateRms(samples: FloatArray): Float {
        var sum = 0.0
        for (s in samples) {
            sum += s * s
        }
        return kotlin.math.sqrt(sum / samples.size).toFloat()
    }
}
