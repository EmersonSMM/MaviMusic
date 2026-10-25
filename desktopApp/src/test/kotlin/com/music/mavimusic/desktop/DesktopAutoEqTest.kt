package com.music.mavimusic.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class DesktopAutoEqTest {

    @Test
    fun parseStandardAutoEqFormat() {
        val raw = """
            # GraphicEQ and ParametricEQ AutoEq profile
            Preamp: -6.5 dB
            Filter 1: ON PK Fc 32 Hz Gain 4.8 dB Q 1.41
            Filter 2: ON LSC Fc 105 Hz Gain 5.2 dB Q 0.71
            Filter 3: OFF PK Fc 1000 Hz Gain 0.0 dB Q 2.00
            Filter 4: ON HSC Fc 10000 Hz Gain -3.0 dB Q 0.71
        """.trimIndent()

        val profile = AutoEqParser.parse(raw, id = "test_profile", name = "Test Headphone")
        assertNotNull(profile)
        assertEquals("test_profile", profile!!.id)
        assertEquals("Test Headphone", profile.name)
        assertEquals(-6.5f, profile.preampDb, 0.01f)
        // Filter 3 is OFF, so 3 active filters parsed
        assertEquals(3, profile.filters.size)

        assertEquals(AutoEqFilterType.PEAK, profile.filters[0].type)
        assertEquals(32f, profile.filters[0].freqHz, 0.01f)
        assertEquals(4.8f, profile.filters[0].gainDb, 0.01f)
        assertEquals(1.41f, profile.filters[0].q, 0.01f)

        assertEquals(AutoEqFilterType.LOW_SHELF, profile.filters[1].type)
        assertEquals(105f, profile.filters[1].freqHz, 0.01f)
        assertEquals(5.2f, profile.filters[1].gainDb, 0.01f)
        assertEquals(0.71f, profile.filters[1].q, 0.01f)

        assertEquals(AutoEqFilterType.HIGH_SHELF, profile.filters[2].type)
        assertEquals(10000f, profile.filters[2].freqHz, 0.01f)
        assertEquals(-3.0f, profile.filters[2].gainDb, 0.01f)
    }

    @Test
    fun autoPreampProtectionAgainstClipping() {
        val rawWithoutPreamp = """
            Filter 1: ON PK Fc 100 Hz Gain 6.0 dB Q 1.0
            Filter 2: ON PK Fc 1000 Hz Gain 2.0 dB Q 1.0
        """.trimIndent()

        val profile = AutoEqParser.parse(rawWithoutPreamp)
        assertNotNull(profile)
        // Must automatically calculate negative preamp at least equal to max positive gain (+6.0 dB -> -6.0 dB)
        assertTrue(profile!!.safePreampDb <= -6.0f)
    }

    @Test
    fun deviceDetectionSuggestsProfiles() {
        val airpods = AutoEqDatabase.suggestProfileForDevice("Emerson's AirPods Pro (2nd generation)")
        assertNotNull(airpods)
        assertEquals("apple_airpods_pro_2", airpods!!.id)

        val sony = AutoEqDatabase.suggestProfileForDevice("WH-1000XM5 Bluetooth Audio")
        assertNotNull(sony)
        assertEquals("sony_wh_1000xm5", sony!!.id)

        val sennheiser = AutoEqDatabase.suggestProfileForDevice("Sennheiser HD 600 Output")
        assertNotNull(sennheiser)
        assertEquals("sennheiser_hd_600", sennheiser!!.id)

        val unknown = AutoEqDatabase.suggestProfileForDevice("MacBook Pro Speakers")
        assertEquals(null, unknown)
    }

    @Test
    fun biquadSectionFilteringStability() {
        val biquad = AutoEqBiquadSection()
        biquad.reset(2)
        // 1kHz peak filter with +6 dB boost
        biquad.compute(AutoEqFilter(AutoEqFilterType.PEAK, 1000f, 6.0f, 2.0f), sampleRate = 48000)

        // Generate 1kHz sine wave: 48 samples per cycle
        var maxIn = 0f
        var maxOut = 0f
        for (i in 0 until 480) {
            val sample = sin(2.0 * PI * 1000.0 * i / 48000.0).toFloat() * 0.5f
            val out = biquad.process(sample, 0)
            if (abs(sample) > maxIn) maxIn = abs(sample)
            if (i > 100 && abs(out) > maxOut) maxOut = abs(out)
        }

        // Boost at 1kHz should produce higher output than input
        assertTrue("Output peak ($maxOut) should exceed input peak ($maxIn)", maxOut > maxIn)
        assertFalse("Output should not blow up or be NaN", maxOut.isNaN() || maxOut.isInfinite())
    }

    @Test
    fun abLevelMatchedComparison() {
        val processor = DesktopAutoEqProcessor()
        processor.configure(channels = 2, sampleRate = 48000)

        val profile = AutoEqProfile(
            id = "test_ab",
            name = "Test AB",
            brand = "Test",
            model = "AB",
            preampDb = -6.0f,
            filters = listOf(AutoEqFilter(AutoEqFilterType.PEAK, 1000f, 4.0f, 1.0f))
        )

        val testBlockA = FloatArray(512) { 0.5f }
        val testBlockB = FloatArray(512) { 0.5f }

        // State A: with correction (filters + preamp)
        processor.setCorrection(enabled = true, profile = profile, abBypass = false)
        processor.process(testBlockA, testBlockA.size)

        // State B: bypass (no filters, but same level-matched preamp)
        processor.reset()
        processor.setCorrection(enabled = true, profile = profile, abBypass = true)
        processor.process(testBlockB, testBlockB.size)

        // State B with DC input should be scaled exactly by linear preamp = 10^(-6/20) = ~0.501187 -> 0.5 * 0.501187 = ~0.25059
        val expectedPreampLinear = Math.pow(10.0, -6.0 / 20.0).toFloat()
        val expectedSample = 0.5f * expectedPreampLinear
        assertEquals(expectedSample, testBlockB[500], 0.01f)

        // Verify state A also has reduced overall preamp
        assertTrue("State A peak must remain well below 0.5 to prevent clipping", testBlockA[500] < 0.45f)
    }

    @Test
    fun disabledAutoEqDoesNotAlterAudio() {
        val processor = DesktopAutoEqProcessor()
        processor.configure(channels = 2, sampleRate = 48000)
        processor.setCorrection(enabled = false, profile = null, abBypass = false)

        val block = FloatArray(128) { 0.75f }
        processor.process(block, block.size)

        for (sample in block) {
            assertEquals(0.75f, sample, 0.0001f)
        }
    }
}
