package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Global settings and reactive state for Karaoke / Vocal Suppression mode.
 */
object DesktopKaraokeSettings {
    private const val PREF_FEATURE_ENABLED = "karaoke_mode_enabled"
    private const val PREF_ATTENUATION = "karaoke_vocal_attenuation"

    private val persistence = DesktopPersistence()

    // General feature flag in Settings (enabled by default as available, but dormant until user activates it)
    private val _featureEnabled = MutableStateFlow(persistence.boolean(PREF_FEATURE_ENABLED, true))
    val featureEnabled: StateFlow<Boolean> = _featureEnabled.asStateFlow()

    // Active toggle in player / immersive mode
    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    // Vocal attenuation factor: 0.0 (original vocal) to 1.0 (maximum vocal suppression)
    private val _attenuation = MutableStateFlow(
        persistence.string(PREF_ATTENUATION, "0.80").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.80f
    )
    val attenuation: StateFlow<Float> = _attenuation.asStateFlow()

    // Alert flag when CPU usage exceeds 25% of audio budget
    private val _cpuAlert = MutableStateFlow(false)
    val cpuAlert: StateFlow<Boolean> = _cpuAlert.asStateFlow()

    fun isFeatureEnabled(): Boolean = _featureEnabled.value

    fun setFeatureEnabled(value: Boolean) {
        _featureEnabled.value = value
        persistence.saveBoolean(PREF_FEATURE_ENABLED, value)
        if (!value) {
            setActive(false)
        }
    }

    fun setActive(value: Boolean) {
        _isActive.value = value && _featureEnabled.value
        if (!_isActive.value) {
            _cpuAlert.value = false
        }
    }

    fun setAttenuation(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _attenuation.value = clamped
        persistence.saveString(PREF_ATTENUATION, "%.2f".format(clamped))
    }

    internal fun setCpuAlert(alert: Boolean) {
        if (_cpuAlert.value != alert) {
            _cpuAlert.value = alert
        }
    }
}

/**
 * Zero-latency 32-bit float real-time vocal suppression engine.
 *
 * Employs center-channel extraction ($M = (L+R)/2$) with high-pass bass retention (corner ~180 Hz)
 * and high-frequency presence preservation (corner ~7500 Hz). The center vocal formant band
 * is extracted and phase-inverted against both stereo decks proportionally to [attenuation],
 * preserving the stereo soundstage, basslines, kick drum, and spatial effects.
 */
class DesktopKaraokeProcessor {

    // 2nd-order Biquad Bandpass filter state for vocal formant isolation (180 Hz to 7500 Hz)
    private var hpX1 = 0f
    private var hpX2 = 0f
    private var hpY1 = 0f
    private var hpY2 = 0f

    private var lpX1 = 0f
    private var lpX2 = 0f
    private var lpY1 = 0f
    private var lpY2 = 0f

    private var lastSampleRate = 44100
    private var hpB0 = 1f
    private var hpB1 = 0f
    private var hpB2 = 0f
    private var hpA1 = 0f
    private var hpA2 = 0f

    private var lpB0 = 1f
    private var lpB1 = 0f
    private var lpB2 = 0f
    private var lpA1 = 0f
    private var lpA2 = 0f

    init {
        configureFilters(44100)
    }

    private fun configureFilters(sampleRate: Int) {
        val sr = sampleRate.coerceAtLeast(8000).toDouble()
        lastSampleRate = sampleRate

        // High-pass filter at 180 Hz (Q = 0.707 Butterworth) to protect sub-bass and kick drums
        val fHp = 180.0
        val omegaHp = 2.0 * PI * fHp / sr
        val alphaHp = sin(omegaHp) / (2.0 * 0.7071)
        val cosOmegaHp = cos(omegaHp)

        val a0Hp = 1.0 + alphaHp
        hpB0 = ((1.0 + cosOmegaHp) / 2.0 / a0Hp).toFloat()
        hpB1 = (-(1.0 + cosOmegaHp) / a0Hp).toFloat()
        hpB2 = hpB0
        hpA1 = (-2.0 * cosOmegaHp / a0Hp).toFloat()
        hpA2 = ((1.0 - alphaHp) / a0Hp).toFloat()

        // Low-pass filter at 7500 Hz (Q = 0.707 Butterworth) to protect cymbals and air
        val fLp = 7500.0.coerceAtMost(sr * 0.45)
        val omegaLp = 2.0 * PI * fLp / sr
        val alphaLp = sin(omegaLp) / (2.0 * 0.7071)
        val cosOmegaLp = cos(omegaLp)

        val a0Lp = 1.0 + alphaLp
        lpB0 = ((1.0 - cosOmegaLp) / 2.0 / a0Lp).toFloat()
        lpB1 = ((1.0 - cosOmegaLp) / a0Lp).toFloat()
        lpB2 = lpB0
        lpA1 = (-2.0 * cosOmegaLp / a0Lp).toFloat()
        lpA2 = ((1.0 - alphaLp) / a0Lp).toFloat()
    }

    /**
     * Processes interleaved float PCM audio in-place.
     * When Karaoke mode is inactive or attenuation is near 0, returns immediately with 0% CPU.
     */
    fun process(samples: FloatArray, count: Int, sampleRate: Int, channels: Int) {
        if (!DesktopKaraokeSettings.isActive.value || channels != 2 || count <= 0) {
            return
        }

        val attenuation = DesktopKaraokeSettings.attenuation.value
        if (attenuation <= 0.01f) {
            return
        }

        if (sampleRate != lastSampleRate) {
            configureFilters(sampleRate)
        }

        val startNanos = System.nanoTime()

        // Compensation boost for subtracted center vocal power
        val gainBoost = 1.0f + 0.16f * attenuation

        var i = 0
        while (i < count - 1) {
            val left = samples[i]
            val right = samples[i + 1]

            // Center mono signal
            val center = 0.5f * (left + right)

            // High-pass filter stage (> 180 Hz)
            val hpY = hpB0 * center + hpB1 * hpX1 + hpB2 * hpX2 - hpA1 * hpY1 - hpA2 * hpY2
            hpX2 = hpX1
            hpX1 = center
            hpY2 = hpY1
            hpY1 = hpY

            // Low-pass filter stage (< 7500 Hz)
            val vocalBand = lpB0 * hpY + lpB1 * lpX1 + lpB2 * lpX2 - lpA1 * lpY1 - lpA2 * lpY2
            lpX2 = lpX1
            lpX1 = hpY
            lpY2 = lpY1
            lpY1 = vocalBand

            // Subtract center vocal formant scaled by attenuation factor
            val vocalSub = attenuation * vocalBand
            samples[i] = (left - vocalSub) * gainBoost
            samples[i + 1] = (right - vocalSub) * gainBoost

            i += 2
        }

        // CPU budget monitoring: calculate fraction of audio duration spent processing
        val elapsedNanos = System.nanoTime() - startNanos
        val frameCount = count / channels
        val blockDurationNanos = (frameCount.toDouble() / sampleRate * 1_000_000_000.0).toLong()
        if (blockDurationNanos > 0) {
            val dutyCycle = elapsedNanos.toDouble() / blockDurationNanos.toDouble()
            // Alert if duty cycle exceeds 25%
            DesktopKaraokeSettings.setCpuAlert(dutyCycle > 0.25)
        }
    }

    /**
     * Clears filter internal state and history when resetting audio deck.
     */
    fun reset() {
        hpX1 = 0f
        hpX2 = 0f
        hpY1 = 0f
        hpY2 = 0f
        lpX1 = 0f
        lpX2 = 0f
        lpY1 = 0f
        lpY2 = 0f
        DesktopKaraokeSettings.setCpuAlert(false)
    }
}
