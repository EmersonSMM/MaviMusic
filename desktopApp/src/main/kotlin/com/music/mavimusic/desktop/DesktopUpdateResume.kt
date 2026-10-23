package com.music.mavimusic.desktop

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** One-use checkpoint for an update restart. Playback returns paused at the saved position. */
internal object DesktopUpdateResume {
    private const val KEY = "update_restart_checkpoint"
    @Serializable data class Checkpoint(val videoId: String, val positionMs: Long)

    fun save(videoId: String?, positionMs: Long) {
        val value = videoId?.let { Json.encodeToString(Checkpoint(it, positionMs.coerceAtLeast(0L))) }.orEmpty()
        DesktopPersistence().saveString(KEY, value)
    }

    fun consume(videoId: String?): Checkpoint? {
        val persistence = DesktopPersistence()
        val raw = persistence.string(KEY)
        persistence.saveString(KEY, "")
        return runCatching { Json.decodeFromString<Checkpoint>(raw) }.getOrNull()
            ?.takeIf { it.videoId == videoId && it.positionMs >= 0L }
    }
}
