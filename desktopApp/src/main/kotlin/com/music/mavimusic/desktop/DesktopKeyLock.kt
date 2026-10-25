package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Key Lock (Master Tempo) and Time-Stretching engine.
 * Changes playback tempo without changing the musical key/pitch.
 */
object DesktopKeyLock {

    private val persistence = DesktopPersistence()
    private const val KEY_ENABLED = "key_lock_enabled"
    private const val KEY_SAFE_RANGE = "key_lock_safe_range_pct"

    const val DEFAULT_SAFE_RANGE_PCT = 8
    const val MAX_SAFE_RANGE_PCT = 15

    private val _enabled = MutableStateFlow(persistence.boolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled

    private val _safeRangePct = MutableStateFlow(
        persistence.int(KEY_SAFE_RANGE, DEFAULT_SAFE_RANGE_PCT).coerceIn(4, MAX_SAFE_RANGE_PCT)
    )
    val safeRangePct: StateFlow<Int> = _safeRangePct

    fun isEnabled(): Boolean = _enabled.value

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persistence.saveBoolean(KEY_ENABLED, value)
    }

    fun setSafeRangePct(value: Int) {
        val clamped = value.coerceIn(4, MAX_SAFE_RANGE_PCT)
        _safeRangePct.value = clamped
        persistence.saveInt(KEY_SAFE_RANGE, clamped)
    }

    val safeRangeFraction: Float
        get() = _safeRangePct.value / 100f

    /** Returns true when [rate] deviates from 1.0 by more than the safe range threshold. */
    fun isBeyondSafeRange(rate: Float): Boolean = abs(rate - 1f) > safeRangeFraction + 0.0001f

    /**
     * Determines whether Automix tempo alignment stretching should be applied.
     * Conditions:
     * 1. Key Lock is enabled in Settings.
     * 2. Both tracks have reliable BPM estimates.
     * 3. The tempo ratio is within the configured safe range (default +/- 8%).
     */
    fun shouldBeatmatchStretch(
        outgoingBpm: Double,
        incomingBpm: Double,
        outgoingReliable: Boolean,
        incomingReliable: Boolean,
    ): Boolean {
        if (!isEnabled()) return false
        if (!outgoingReliable || !incomingReliable) return false
        if (outgoingBpm <= 0.0 || incomingBpm <= 0.0) return false
        val ratio = incomingBpm / outgoingBpm
        return abs(ratio - 1.0) <= safeRangeFraction
    }

    /**
     * Clamps a tempo rate according to Key Lock rules.
     * When Key Lock is off: returns legacy range [1.0, 1.1] exactly as before.
     * When Key Lock is on: allows bidirectional stretch within [1 - safeRange, 1 + safeRange].
     */
    fun clampDeckRate(rate: Double): Double {
        if (!isEnabled()) {
            return rate.coerceIn(1.0, 1.1)
        }
        val maxDeviation = safeRangeFraction.toDouble()
        return rate.coerceIn(1.0 - maxDeviation, 1.0 + maxDeviation)
    }
}
