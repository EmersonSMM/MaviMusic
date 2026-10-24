package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * State of an active DJ Automix crossfade / beatmatch transition between two tracks.
 */
data class DjMixState(
    val isBlending: Boolean = false,
    val incomingTitle: String = "",
    val incomingArtist: String = "",
    val incomingBpm: Int = 120,
    val outgoingBpm: Int = 120,
    val progress: Float = 0f,
)

internal object DesktopDjState {
    private val _state = MutableStateFlow(DjMixState())
    val state: StateFlow<DjMixState> = _state.asStateFlow()

    fun update(
        isBlending: Boolean,
        incomingTitle: String,
        incomingArtist: String,
        incomingBpm: Int,
        outgoingBpm: Int,
        progress: Float = 0f,
    ) {
        _state.value = DjMixState(
            isBlending = isBlending,
            incomingTitle = incomingTitle,
            incomingArtist = incomingArtist,
            incomingBpm = if (incomingBpm > 0) incomingBpm else 120,
            outgoingBpm = if (outgoingBpm > 0) outgoingBpm else 120,
            progress = progress,
        )
    }

    fun updateProgress(progress: Float) {
        if (_state.value.isBlending) {
            _state.value = _state.value.copy(progress = progress.coerceIn(0f, 1f))
        }
    }

    fun reset() {
        if (_state.value.isBlending) {
            _state.value = DjMixState()
        }
    }
}
