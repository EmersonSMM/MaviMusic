package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Explicit state machine for Automix transitions (Point A.2).
 *
 * Lifecycle: INACTIVE -> ARMED -> PRELOADING -> TRANSITIONING -> COMPLETED
 */
enum class AutomixState {
    INACTIVE,
    ARMED,
    PRELOADING,
    TRANSITIONING,
    COMPLETED,
}

/**
 * State of an active DJ Automix crossfade / beatmatch transition between two tracks,
 * including explicit state machine status and diagnostic telemetry.
 */
data class DjMixState(
    val isBlending: Boolean = false,
    val incomingTitle: String = "",
    val incomingArtist: String = "",
    val incomingBpm: Int = 120,
    val outgoingBpm: Int = 120,
    val progress: Float = 0f,
    val automixState: AutomixState = AutomixState.INACTIVE,
    val currentTitle: String = "",
    val nextTitle: String = "",
    val transitionPointSec: Double? = null,
    val fadeDurationSec: Double? = null,
    val lastReason: String? = null,
)

internal object DesktopDjState {
    private const val MAX_DIAGNOSTIC_ENTRIES = 120
    private val _state = MutableStateFlow(DjMixState())
    val state: StateFlow<DjMixState> = _state.asStateFlow()

    private val _diagnostics = ArrayDeque<String>(MAX_DIAGNOSTIC_ENTRIES)

    @Synchronized
    fun recordDiagnostic(
        automixState: AutomixState,
        currentTitle: String,
        nextTitle: String,
        transitionPointSec: Double? = _state.value.transitionPointSec,
        fadeDurationSec: Double? = _state.value.fadeDurationSec,
        reason: String? = null,
    ) {
        val tpStr = transitionPointSec?.let { "%.2fs".format(Locale.US, it) } ?: "n/a"
        val fadeStr = fadeDurationSec?.let { "%.2fs".format(Locale.US, it) } ?: "n/a"
        val reasonSuffix = if (!reason.isNullOrBlank()) " | reason=$reason" else ""
        val line = "[Automix] state=$automixState | current='$currentTitle' | next='$nextTitle' | transitionAt=$tpStr | fade=$fadeStr$reasonSuffix"
        if (_diagnostics.lastOrNull() != line) {
            if (_diagnostics.size >= MAX_DIAGNOSTIC_ENTRIES) {
                _diagnostics.removeFirst()
            }
            _diagnostics.addLast(line)
            DesktopTrackLog.log(line)
        }
        _state.value = _state.value.copy(
            automixState = automixState,
            currentTitle = currentTitle.ifBlank { _state.value.currentTitle },
            nextTitle = nextTitle,
            transitionPointSec = transitionPointSec,
            fadeDurationSec = fadeDurationSec,
            lastReason = reason ?: _state.value.lastReason,
        )
    }

    @Synchronized
    fun diagnosticsSnapshot(): List<String> = _diagnostics.toList()

    fun update(
        isBlending: Boolean,
        incomingTitle: String,
        incomingArtist: String,
        incomingBpm: Int,
        outgoingBpm: Int,
        progress: Float = 0f,
    ) {
        val prev = _state.value
        _state.value = prev.copy(
            isBlending = isBlending,
            incomingTitle = incomingTitle,
            incomingArtist = incomingArtist,
            incomingBpm = if (incomingBpm > 0) incomingBpm else 120,
            outgoingBpm = if (outgoingBpm > 0) outgoingBpm else 120,
            progress = progress,
            automixState = if (isBlending) AutomixState.TRANSITIONING else prev.automixState,
            nextTitle = incomingTitle.ifBlank { prev.nextTitle },
        )
    }

    fun updateProgress(progress: Float) {
        if (_state.value.isBlending) {
            _state.value = _state.value.copy(
                progress = progress.coerceIn(0f, 1f),
                automixState = AutomixState.TRANSITIONING,
            )
        }
    }

    fun markCompleted(newCurrentTitle: String) {
        val prev = _state.value
        recordDiagnostic(
            automixState = AutomixState.COMPLETED,
            currentTitle = newCurrentTitle,
            nextTitle = "",
            transitionPointSec = null,
            fadeDurationSec = null,
            reason = "handoff_complete",
        )
        _state.value = prev.copy(
            isBlending = false,
            incomingTitle = "",
            incomingArtist = "",
            progress = 0f,
            automixState = AutomixState.COMPLETED,
            currentTitle = newCurrentTitle,
            nextTitle = "",
            transitionPointSec = null,
            fadeDurationSec = null,
        )
    }

    fun reset(reason: String? = null) {
        val prev = _state.value
        if (prev.isBlending || prev.automixState != AutomixState.INACTIVE) {
            if (reason != null) {
                recordDiagnostic(
                    automixState = AutomixState.INACTIVE,
                    currentTitle = prev.currentTitle,
                    nextTitle = prev.nextTitle,
                    transitionPointSec = null,
                    fadeDurationSec = null,
                    reason = reason,
                )
            }
            _state.value = DjMixState(
                automixState = AutomixState.INACTIVE,
                currentTitle = prev.currentTitle,
                lastReason = reason,
            )
        }
    }
}
