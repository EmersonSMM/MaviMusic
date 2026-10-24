package com.music.mavimusic.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioSystem

/**
 * macOS audio output device watcher.
 *
 * Monitors CoreAudio outputs (Bluetooth speakers, USB audio DACs, AirPods, Mac mini built-in speakers)
 * and notifies [DesktopAudioDevices.systemChanged] whenever devices connect, disconnect,
 * or change routing state.
 */
internal object DesktopMacAudioWatcher {

    private const val POLL_MS = 2_500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var started = false

    fun ensureStarted() {
        if (started || !DesktopPlatform.isMac) return
        started = true
        scope.launch {
            var last = currentDeviceSnapshot()
            while (true) {
                delay(POLL_MS)
                val now = currentDeviceSnapshot()
                if (now != last) {
                    last = now
                    DesktopAudioDevices.systemChanged()
                    DesktopTrackLog.log("[macOS Audio] Output devices changed: $now")
                }
            }
        }
    }

    private fun currentDeviceSnapshot(): String = runCatching {
        AudioSystem.getMixerInfo()
            .map { it.name }
            .sorted()
            .joinToString("|")
    }.getOrDefault("")
}
