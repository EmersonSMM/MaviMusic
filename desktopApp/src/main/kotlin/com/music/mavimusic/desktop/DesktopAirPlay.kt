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

    private val _featureEnabled = MutableStateFlow(persistence.boolean(PREF_KEY_ENABLED, true))
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
     * Note: Evaluated on the audio thread; must NEVER invoke blocking CoreAudio or AudioSystem enumerations.
     */
    fun isAirPlayActive(): Boolean {
        if (!_featureEnabled.value) return false
        val currentSelected = DesktopAudioDevices.selected.value.lowercase()
        return currentSelected.contains("airplay") ||
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
     * Identifies devices via CoreAudio HAL mixers as well as mDNS / Bonjour network advertisement.
     */
    fun refreshAirPlayDevices() {
        runCatching {
            val detected = mutableListOf<DesktopAirPlayDevice>()

            // 1. CoreAudio HAL mixers
            val mixers = AudioSystem.getMixerInfo()
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
                        lower.contains("soundbar") || lower.contains("barra") -> "Barra de sonido"
                        lower.contains("tv") || lower.contains("samsung") -> "Smart TV"
                        else -> "AirPlay"
                    }
                    detected.add(DesktopAirPlayDevice(id = name, name = name, type = kind))
                }
            }

            // 2. Discover network AirPlay receivers via Bonjour on macOS
            if (DesktopPlatform.isMac) {
                runCatching {
                    val proc = ProcessBuilder("dns-sd", "-B", "_airplay._tcp", "local.")
                        .redirectErrorStream(true)
                        .start()
                    val reader = proc.inputStream.bufferedReader()
                    val localHostName = runCatching { java.net.InetAddress.getLocalHost().hostName }.getOrDefault("")
                    val deadline = System.currentTimeMillis() + 1000L
                    while (System.currentTimeMillis() < deadline) {
                        if (reader.ready()) {
                            val line = reader.readLine() ?: break
                            if (line.contains(" Add ") || line.contains("\tAdd\t")) {
                                val parts = line.trim().split(Regex("\\s+"))
                                if (parts.size >= 7) {
                                    val instanceName = parts.subList(6, parts.size).joinToString(" ").trim()
                                    val lower = instanceName.lowercase()
                                    if (!instanceName.contains("Mac mini", ignoreCase = true) &&
                                        !instanceName.contains(localHostName, ignoreCase = true)) {
                                        val kind = when {
                                            lower.contains("homepod") -> "HomePod"
                                            lower.contains("apple tv") -> "Apple TV"
                                            lower.contains("soundbar") || lower.contains("barra") -> "Barra de sonido"
                                            lower.contains("tv") || lower.contains("samsung") || lower.contains("recámara") -> "Smart TV"
                                            else -> "AirPlay"
                                        }
                                        detected.add(DesktopAirPlayDevice(id = instanceName, name = instanceName, type = kind))
                                    }
                                }
                            }
                        } else {
                            Thread.sleep(40L)
                        }
                    }
                    proc.destroyForcibly()
                }
            }

            DesktopAirPlaySettings.updateDiscovered(detected.distinctBy { it.name })
        }
    }

    /**
     * Routes macOS system audio output to the specified AirPlay or network audio receiver
     * via AppleScript System Settings Sound panel.
     */
    fun routeSystemToAirPlay(deviceName: String) {
        if (!DesktopPlatform.isMac || deviceName.isBlank()) return
        scope.launch(Dispatchers.IO) {
            val sanitized = deviceName.replace("\"", "\\\"").trim()
            val script = """
                tell application "System Settings"
                    reveal pane id "com.apple.Sound-Settings.extension"
                end tell
                delay 0.5
                tell application "System Events"
                    tell process "System Settings"
                        try
                            set win to window 1
                            set targetRows to {}
                            try
                                set targetRows to (every row of outline 1 of scroll area 1 of group 2 of scroll area 1 of group 1 of group 3 of splitter group 1 of group 1 of win)
                            on error
                                try
                                    set targetRows to (every row of outline 1 of scroll area 1 of win)
                                end try
                            end try
                            repeat with r in targetRows
                                try
                                    set tName to value of static text 1 of group 1 of UI element 1 of r
                                    if tName contains "$sanitized" then
                                        select r
                                        exit repeat
                                    end if
                                end try
                            end repeat
                        end try
                    end tell
                end tell
                delay 0.2
                tell application "System Settings" to quit
            """.trimIndent()

            runCatching {
                val pb = ProcessBuilder("osascript", "-e", script)
                pb.redirectErrorStream(true)
                val p = pb.start()
                val output = p.inputStream.bufferedReader().readText()
                p.waitFor()
                DesktopTrackLog.log("AirPlay route to '$sanitized' finished: ${output.trim()}")
            }.onFailure {
                DesktopTrackLog.log("AirPlay route error for '$sanitized': ${it.message}")
            }
        }
    }

    /**
     * Routes macOS system audio output back to default Mac speakers.
     */
    fun routeSystemToDefault() {
        if (!DesktopPlatform.isMac) return
        scope.launch(Dispatchers.IO) {
            val script = """
                tell application "System Settings"
                    reveal pane id "com.apple.Sound-Settings.extension"
                end tell
                delay 0.5
                tell application "System Events"
                    tell process "System Settings"
                        try
                            set win to window 1
                            set targetRows to {}
                            try
                                set targetRows to (every row of outline 1 of scroll area 1 of group 2 of scroll area 1 of group 1 of group 3 of splitter group 1 of group 1 of win)
                            on error
                                try
                                    set targetRows to (every row of outline 1 of scroll area 1 of win)
                                end try
                            end try
                            repeat with r in targetRows
                                try
                                    set tName to value of static text 1 of group 1 of UI element 1 of r
                                    if tName contains "Altavoces" or tName contains "Speakers" or tName contains "Internal" then
                                        select r
                                        exit repeat
                                    end if
                                end try
                            end repeat
                        end try
                    end tell
                end tell
                delay 0.2
                tell application "System Settings" to quit
            """.trimIndent()

            runCatching {
                val pb = ProcessBuilder("osascript", "-e", script)
                pb.redirectErrorStream(true)
                val p = pb.start()
                p.waitFor()
            }
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
