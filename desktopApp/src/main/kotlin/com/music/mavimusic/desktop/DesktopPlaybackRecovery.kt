package com.music.mavimusic.desktop

/** Bounded recovery per listening intent; successful progress re-arms a later network outage. */
internal class DesktopPlaybackRecovery {
    private var key: String? = null
    private var attempts = 0
    private var lastPosition = 0L

    fun nextDelay(intent: String, remote: Boolean, error: String, positionMs: Long): Long? {
        if (key != intent || positionMs - lastPosition >= 30_000) {
            key = intent
            attempts = 0
        }
        if (!remote || error.contains("No audio output", true) ||
            error.contains("permission", true) || error.contains("not available in", true)) return null
        if (attempts >= 2) return null
        lastPosition = positionMs
        return listOf(1_000L, 3_000L)[attempts++]
    }

    companion object {
        fun prematureEnd(remote: Boolean, positionMs: Long, durationMs: Long): Boolean =
            remote && durationMs > 0 && durationMs - positionMs > 5_000
    }
}
