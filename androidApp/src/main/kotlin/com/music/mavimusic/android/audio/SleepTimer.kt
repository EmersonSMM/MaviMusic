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

object SleepTimer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null

    private val _minutesLeft = MutableStateFlow<Int?>(null)
    val minutesLeft = _minutesLeft.asStateFlow()

    private val _stopAtEndOfSong = MutableStateFlow(false)
    val stopAtEndOfSong = _stopAtEndOfSong.asStateFlow()

    fun start(minutes: Int) {
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
                AndroidPlayerEngine.pause()
                cancel()
            }
        }
    }

    fun startEndOfSong() {
        cancel()
        _stopAtEndOfSong.value = true
        _minutesLeft.value = 1
    }

    fun onTrackEnded() {
        if (_stopAtEndOfSong.value) {
            AndroidPlayerEngine.pause()
            cancel()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _minutesLeft.value = null
        _stopAtEndOfSong.value = false
    }

    fun isRunning(): Boolean = _minutesLeft.value != null || _stopAtEndOfSong.value
}
