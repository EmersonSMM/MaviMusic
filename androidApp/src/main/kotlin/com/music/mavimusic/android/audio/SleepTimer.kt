package com.music.mavimusic.android.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class SleepTimerController(
    private val scope: CoroutineScope,
    private val pausePlayback: () -> Unit,
) {
    private var timerJob: Job? = null

    private val _minutesLeft = MutableStateFlow<Int?>(null)
    val minutesLeft = _minutesLeft.asStateFlow()

    private val _stopAtEndOfSong = MutableStateFlow(false)
    val stopAtEndOfSong = _stopAtEndOfSong.asStateFlow()

    fun start(minutes: Int) {
        require(minutes > 0)
        cancel()
        _stopAtEndOfSong.value = false
        _minutesLeft.value = minutes

        timerJob = scope.launch {
            var remaining = minutes
            while (isActive && remaining > 0) {
                delay(60_000L)
                remaining--
                _minutesLeft.value = remaining
            }
            if (isActive) {
                pausePlayback()
                cancel()
            }
        }
    }

    fun startEndOfSong() {
        cancel()
        _stopAtEndOfSong.value = true
        _minutesLeft.value = 1
    }

    /** True consumes the end event, so neither repeat nor autoplay can resume it. */
    fun onTrackEnded(): Boolean {
        if (_stopAtEndOfSong.value) {
            pausePlayback()
            cancel()
            return true
        }
        return false
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _minutesLeft.value = null
        _stopAtEndOfSong.value = false
    }

    fun isRunning(): Boolean = _minutesLeft.value != null || _stopAtEndOfSong.value
}

object SleepTimer {
    private val controller = SleepTimerController(
        CoroutineScope(SupervisorJob() + Dispatchers.Main),
        pausePlayback = { AndroidPlayerEngine.pause() },
    )
    val minutesLeft = controller.minutesLeft
    val stopAtEndOfSong = controller.stopAtEndOfSong
    fun start(minutes: Int) = controller.start(minutes)
    fun startEndOfSong() = controller.startEndOfSong()
    fun onTrackEnded(): Boolean = controller.onTrackEnded()
    fun cancel() = controller.cancel()
    fun isRunning(): Boolean = controller.isRunning()
}
