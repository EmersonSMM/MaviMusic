package com.music.mavimusic.desktop

import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopKeyLockTest {

    @Test
    fun `clamping returns exact legacy range when key lock is off`() {
        val initial = DesktopKeyLock.isEnabled()
        try {
            DesktopKeyLock.setEnabled(false)
            assertEquals(1.0, DesktopKeyLock.clampDeckRate(0.92), 1e-4)
            assertEquals(1.0, DesktopKeyLock.clampDeckRate(0.98), 1e-4)
            assertEquals(1.04, DesktopKeyLock.clampDeckRate(1.04), 1e-4)
            assertEquals(1.10, DesktopKeyLock.clampDeckRate(1.10), 1e-4)
            assertEquals(1.10, DesktopKeyLock.clampDeckRate(1.18), 1e-4)
        } finally {
            DesktopKeyLock.setEnabled(initial)
        }
    }

    @Test
    fun `clamping allows bidirectional safe range when key lock is on`() {
        val initialEnabled = DesktopKeyLock.isEnabled()
        val initialRange = DesktopKeyLock.safeRangePct.value
        try {
            DesktopKeyLock.setEnabled(true)
            DesktopKeyLock.setSafeRangePct(8)

            // Safe range: [0.92, 1.08]
            assertEquals(0.92, DesktopKeyLock.clampDeckRate(0.85), 1e-4)
            assertEquals(0.95, DesktopKeyLock.clampDeckRate(0.95), 1e-4)
            assertEquals(1.00, DesktopKeyLock.clampDeckRate(1.00), 1e-4)
            assertEquals(1.06, DesktopKeyLock.clampDeckRate(1.06), 1e-4)
            assertEquals(1.08, DesktopKeyLock.clampDeckRate(1.15), 1e-4)

            // Configurable range up to 12%
            DesktopKeyLock.setSafeRangePct(12)
            assertEquals(0.88, DesktopKeyLock.clampDeckRate(0.80), 1e-4)
            assertEquals(1.12, DesktopKeyLock.clampDeckRate(1.20), 1e-4)
        } finally {
            DesktopKeyLock.setEnabled(initialEnabled)
            DesktopKeyLock.setSafeRangePct(initialRange)
        }
    }

    @Test
    fun `isBeyondSafeRange detects rates exceeding safe threshold`() {
        val initialRange = DesktopKeyLock.safeRangePct.value
        try {
            DesktopKeyLock.setSafeRangePct(8)
            assertFalse(DesktopKeyLock.isBeyondSafeRange(1.00f))
            assertFalse(DesktopKeyLock.isBeyondSafeRange(1.05f))
            assertFalse(DesktopKeyLock.isBeyondSafeRange(0.95f))
            assertFalse(DesktopKeyLock.isBeyondSafeRange(1.08f))

            assertTrue(DesktopKeyLock.isBeyondSafeRange(1.10f))
            assertTrue(DesktopKeyLock.isBeyondSafeRange(0.90f))
            assertTrue(DesktopKeyLock.isBeyondSafeRange(1.25f))
        } finally {
            DesktopKeyLock.setSafeRangePct(initialRange)
        }
    }

    @Test
    fun `shouldBeatmatchStretch requires reliable BPMs and safe range`() {
        val initial = DesktopKeyLock.isEnabled()
        try {
            DesktopKeyLock.setEnabled(true)
            DesktopKeyLock.setSafeRangePct(8)

            // Both reliable, within +/-8% (124 vs 120 = 1.033x) -> ALLOWED
            assertTrue(
                DesktopKeyLock.shouldBeatmatchStretch(
                    outgoingBpm = 120.0,
                    incomingBpm = 124.0,
                    outgoingReliable = true,
                    incomingReliable = true,
                )
            )

            // Both reliable, but outside safe range (135 vs 120 = 1.125x > 1.08) -> REJECTED
            assertFalse(
                DesktopKeyLock.shouldBeatmatchStretch(
                    outgoingBpm = 120.0,
                    incomingBpm = 135.0,
                    outgoingReliable = true,
                    incomingReliable = true,
                )
            )

            // One track unreliable -> REJECTED (no guessing)
            assertFalse(
                DesktopKeyLock.shouldBeatmatchStretch(
                    outgoingBpm = 120.0,
                    incomingBpm = 124.0,
                    outgoingReliable = false,
                    incomingReliable = true,
                )
            )

            // When Key Lock is disabled -> REJECTED
            DesktopKeyLock.setEnabled(false)
            assertFalse(
                DesktopKeyLock.shouldBeatmatchStretch(
                    outgoingBpm = 120.0,
                    incomingBpm = 124.0,
                    outgoingReliable = true,
                    incomingReliable = true,
                )
            )
        } finally {
            DesktopKeyLock.setEnabled(initial)
        }
    }

    @Test
    fun `wsola pitch preserving time stretcher preserves pitch frequency without clipping`() {
        val sampleRate = 44100
        val channels = 2
        val stretcher = DesktopAudioSpeed(channels, sampleRate)
        stretcher.speed = 1.06f // 6% faster tempo with key lock

        // Generate 440 Hz sine tone in 32-bit float
        val hz = 440.0
        val inputFrames = (sampleRate * 1.5).toInt()
        val inputSamples = FloatArray(inputFrames * channels)
        for (i in 0 until inputFrames) {
            val sample = sin(2.0 * Math.PI * hz * i / sampleRate).toFloat() * 0.9f
            inputSamples[i * channels] = sample
            inputSamples[i * channels + 1] = sample
        }

        val output = stretcher.process(inputSamples, inputSamples.size)
        val outputCount = stretcher.outputCount
        assertTrue(outputCount > 0, "Stretcher must produce output frames")

        // Assert 32-bit float precision and no clipping beyond [-1.0, 1.0]
        var maxPeak = 0f
        for (i in 0 until outputCount) {
            val s = abs(output[i])
            if (s > maxPeak) maxPeak = s
            assertTrue(output[i] in -1.05f..1.05f, "Sample must not clip: ${output[i]}")
        }

        // Measure pitch preservation by counting zero crossings in a clean window
        var zeroCrossings = 0
        val startFrame = 5000
        val endFrame = (startFrame + sampleRate).coerceAtMost(outputCount / channels)
        for (i in startFrame until endFrame - 1) {
            val s1 = output[i * channels]
            val s2 = output[(i + 1) * channels]
            if ((s1 <= 0f && s2 > 0f) || (s1 >= 0f && s2 < 0f)) {
                zeroCrossings++
            }
        }
        val estimatedHz = zeroCrossings / 2.0
        // Expected ~440 Hz regardless of 1.06x time stretch!
        assertEquals(440.0, estimatedHz, 15.0, "Pitch frequency must remain ~440 Hz (pitch locked)")
    }
}
