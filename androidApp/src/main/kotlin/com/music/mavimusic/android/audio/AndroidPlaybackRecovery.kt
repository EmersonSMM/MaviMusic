package com.music.mavimusic.android.audio

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Bounded automatic network recovery per listening intent.
 * Progress of at least 30 seconds re-arms the retry budget for a later outage.
 */
internal class AndroidPlaybackRecovery {
    private var intentKey: String? = null
    private var attempts = 0
    private var lastPositionMs = 0L

    fun nextDelayMs(intent: String, isRemote: Boolean, positionMs: Long): Long? {
        if (!isRemote) return null
        if (intentKey != intent || positionMs - lastPositionMs >= 30_000L) {
            intentKey = intent
            attempts = 0
        }
        if (attempts >= BACKOFF_MS.size) return null
        lastPositionMs = positionMs
        return BACKOFF_MS[attempts++]
    }

    fun reset() {
        intentKey = null
        attempts = 0
        lastPositionMs = 0L
    }

    private companion object {
        val BACKOFF_MS = longArrayOf(1_000L, 3_000L)
    }
}

/**
 * Computes safe gain parameters targeting -14 LUFS:
 * - Louder tracks attenuate cleanly via ExoPlayer linear volume (<= 1.0f).
 * - Quieter tracks apply bounded boost via Android's LoudnessEnhancer (millibels).
 */
internal object AndroidLoudnessNormalizer {
    const val TARGET_LUFS = -14.0
    private const val MAX_BOOST_DB = 9.0
    private const val MAX_ATTENUATION_DB = -12.0

    data class Gain(
        val playerVolume: Float,
        val enhancerTargetGainMb: Int,
    )

    fun compute(loudnessDb: Double?, enabled: Boolean): Gain {
        if (!enabled || loudnessDb == null || !loudnessDb.isFinite()) {
            return Gain(playerVolume = 1f, enhancerTargetGainMb = 0)
        }
        val deltaDb = (TARGET_LUFS - loudnessDb).coerceIn(MAX_ATTENUATION_DB, MAX_BOOST_DB)
        return if (deltaDb >= 0.0) {
            Gain(playerVolume = 1f, enhancerTargetGainMb = (deltaDb * 100.0).roundToInt())
        } else {
            val linear = 10.0.pow(deltaDb / 20.0).toFloat().coerceIn(0.15f, 1f)
            Gain(playerVolume = linear, enhancerTargetGainMb = 0)
        }
    }
}
