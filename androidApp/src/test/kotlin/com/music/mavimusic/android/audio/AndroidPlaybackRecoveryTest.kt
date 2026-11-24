package com.music.mavimusic.android.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidPlaybackRecoveryTest {

    @Test
    fun `bounded network recovery retries twice with backoff and stops on third failure`() {
        val recovery = AndroidPlaybackRecovery()

        assertEquals(1_000L, recovery.nextDelayMs(intent = "vid1", isRemote = true, positionMs = 4_000L))
        assertEquals(3_000L, recovery.nextDelayMs(intent = "vid1", isRemote = true, positionMs = 4_000L))
        assertNull(recovery.nextDelayMs(intent = "vid1", isRemote = true, positionMs = 4_000L))
    }

    @Test
    fun `stable playback over 30s re-arms recovery budget`() {
        val recovery = AndroidPlaybackRecovery()
        assertEquals(1_000L, recovery.nextDelayMs("vid1", isRemote = true, positionMs = 10_000L))
        assertEquals(3_000L, recovery.nextDelayMs("vid1", isRemote = true, positionMs = 10_000L))
        assertNull(recovery.nextDelayMs("vid1", isRemote = true, positionMs = 10_000L))

        // After advancing >= 30s, the retry budget re-arms
        assertEquals(1_000L, recovery.nextDelayMs("vid1", isRemote = true, positionMs = 41_000L))
    }

    @Test
    fun `local tracks skip network recovery`() {
        val recovery = AndroidPlaybackRecovery()
        assertNull(recovery.nextDelayMs("local_1", isRemote = false, positionMs = 1_000L))
    }

    @Test
    fun `loudness normalizer attenuates loud tracks and boosts quiet tracks safely`() {
        val disabled = AndroidLoudnessNormalizer.compute(loudnessDb = -8.0, enabled = false)
        assertEquals(1.0f, disabled.playerVolume, 0.001f)
        assertEquals(0, disabled.enhancerTargetGainMb)

        // Loud track (-8 LUFS vs -14 LUFS target = -6 dB -> ~0.501 linear)
        val loud = AndroidLoudnessNormalizer.compute(loudnessDb = -8.0, enabled = true)
        assertTrue(loud.playerVolume in 0.48f..0.52f)
        assertEquals(0, loud.enhancerTargetGainMb)

        // Quiet track (-20 LUFS vs -14 LUFS target = +6 dB -> +600 mB)
        val quiet = AndroidLoudnessNormalizer.compute(loudnessDb = -20.0, enabled = true)
        assertEquals(1.0f, quiet.playerVolume, 0.001f)
        assertEquals(600, quiet.enhancerTargetGainMb)

        // Very quiet track (-35 LUFS) clamped to +9 dB (+900 mB)
        val veryQuiet = AndroidLoudnessNormalizer.compute(loudnessDb = -35.0, enabled = true)
        assertEquals(1.0f, veryQuiet.playerVolume, 0.001f)
        assertEquals(900, veryQuiet.enhancerTargetGainMb)
    }
}
