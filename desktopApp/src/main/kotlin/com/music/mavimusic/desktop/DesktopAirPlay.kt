package com.music.mavimusic.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.sound.sampled.AudioSystem

data class DesktopAirPlayDevice(
    val id: String,
    val name: String,
    val type: String = "AirPlay 2",
)

object DesktopAirPlaySettings {
    private const val PREF_KEY_ENABLED = "airplay_support_enabled"
    private const val PREF_KEY_LATENCY = "airplay_latency_ms"

    private val persistence = DesktopPersistence()

    private val _featureEnabled = MutableStateFlow(persistence.boolean(PREF_KEY_ENABLED, false))
    val featureEnabled: StateFlow<Boolean> = _featureEnabled.asStateFlow()

    private val _latencyMs = MutableStateFlow(
        persistence.string(PREF_KEY_LATENCY, "2000").toIntOrNull()?.coerceIn(0, 3000) ?: 2000
    )
    val latencyMs: StateFlow<Int> = _latencyMs.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DesktopAirPlayDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DesktopAirPlayDevice>> = _discoveredDevices.asStateFlow()

    fun isFeatureEnabled(): Boolean = _featureEnabled.value

    fun setFeatureEnabled(value: Boolean) {
        _featureEnabled.value = value
        persistence.saveBoolean(PREF_KEY_ENABLED, value)
        if (value) {
            DesktopAirPlayManager.startDiscovery()
        } else {
            DesktopAirPlayManager.stopDiscovery()
        }
    }

    fun setLatencyMs(ms: Int) {
        val clamped = ms.coerceIn(0, 3000)
        _latencyMs.value = clamped
        persistence.saveString(PREF_KEY_LATENCY, clamped.toString())
    }

    /**
     * Returns true if AirPlay feature is active and current audio output is routed to an AirPlay receiver.
     */
    fun isAirPlayActive(): Boolean {
        if (!_featureEnabled.value) return false
        val currentLabel = DesktopAudioDevices.label().lowercase()
        val currentSelected = DesktopAudioDevices.selected.value.lowercase()
        return currentLabel.contains("airplay") ||
            currentLabel.contains("homepod") ||
            currentLabel.contains("apple tv") ||
            currentSelected.contains("airplay") ||
            currentSelected.contains("homepod") ||
            currentSelected.contains("apple tv")
    }

    internal fun updateDiscovered(devices: List<DesktopAirPlayDevice>) {
        _discoveredDevices.value = devices
    }
}

object DesktopAirPlayManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var isDiscovering = false

    fun startDiscovery() {
        if (isDiscovering || !DesktopPlatform.isMac) return
        isDiscovering = true
        scope.launch {
            while (isDiscovering && DesktopAirPlaySettings.isFeatureEnabled()) {
                refreshAirPlayDevices()
                delay(4_000L)
            }
        }
    }

    fun stopDiscovery() {
        isDiscovering = false
    }

    /**
     * Refreshes the list of AirPlay receivers available on macOS.
     * Identifies devices via CoreAudio HAL mixers containing AirPlay, HomePod, or Apple TV signatures.
     */
    fun refreshAirPlayDevices() {
        runCatching {
            val mixers = AudioSystem.getMixerInfo()
            val detected = mutableListOf<DesktopAirPlayDevice>()
            for (info in mixers) {
                val name = info.name.trim()
                val lower = name.lowercase()
                val isAirPlay = lower.contains("airplay") ||
                    lower.contains("homepod") ||
                    lower.contains("apple tv") ||
                    lower.contains("airport")
                if (isAirPlay) {
                    val kind = when {
                        lower.contains("homepod") -> "HomePod"
                        lower.contains("apple tv") -> "Apple TV"
                        else -> "AirPlay Receptor"
                    }
                    detected.add(DesktopAirPlayDevice(id = name, name = name, type = kind))
                }
            }
            DesktopAirPlaySettings.updateDiscovered(detected.distinctBy { it.id })
        }
    }

    /**
     * Opens macOS System Settings -> Sound to allow the user to easily route or manage
     * system AirPlay receivers with Apple native FairPlay encryption and multi-room grouping.
     */
    fun openMacSoundSettings() {
        if (!DesktopPlatform.isMac) return
        runCatching {
            ProcessBuilder("open", "x-apple.systempreferences:com.apple.Sound-Settings.extension")
                .start()
        }.onFailure {
            runCatching {
                ProcessBuilder("open", "/System/Library/PreferencePanes/Sound.prefPane").start()
            }
        }
    }
}
