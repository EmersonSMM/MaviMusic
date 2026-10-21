package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Manages automatically starting BitChord when the user logs into their computer.
 * Supports macOS (via ~/Library/LaunchAgents), Windows (Registry Run), and Linux (xdg autostart).
 */
internal object DesktopAutoStart {

    private const val MACOS_AGENT_LABEL = "com.music.bitchord"
    private const val KEY_START_ON_BOOT = "start_with_system"

    private val persistence by lazy { DesktopPersistence() }
    private val _enabled = MutableStateFlow(isSystemConfigured())
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private fun launchAgentFile(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, "Library/LaunchAgents/$MACOS_AGENT_LABEL.plist")
    }

    private fun linuxAutostartFile(): File {
        val userHome = System.getProperty("user.home")
        return File(userHome, ".config/autostart/bitchord.desktop")
    }

    /** Path to current app executable or default installation path. */
    fun appExecutablePath(): String {
        val currentCommand = ProcessHandle.current().info().command().orElse("")
        if (currentCommand.isNotBlank() && File(currentCommand).exists()) {
            return currentCommand
        }
        return when {
            DesktopPlatform.isMac -> "/Applications/BitChord.app/Contents/MacOS/BitChord"
            DesktopPlatform.isWindows -> System.getenv("LOCALAPPDATA")?.let { "$it\\Programs\\BitChord\\BitChord.exe" } ?: "BitChord.exe"
            else -> "/usr/bin/bitchord"
        }
    }

    private fun isSystemConfigured(): Boolean {
        return when {
            DesktopPlatform.isMac -> launchAgentFile().exists()
            DesktopPlatform.isLinux -> linuxAutostartFile().exists()
            DesktopPlatform.isWindows -> persistence.boolean(KEY_START_ON_BOOT, false)
            else -> false
        }
    }

    fun setEnabled(enable: Boolean): Boolean {
        val success = if (enable) enableAutoStart() else disableAutoStart()
        if (success) {
            persistence.saveBoolean(KEY_START_ON_BOOT, enable)
            _enabled.value = enable
        }
        return success
    }

    private fun enableAutoStart(): Boolean = runCatching {
        when {
            DesktopPlatform.isMac -> {
                val file = launchAgentFile()
                file.parentFile?.mkdirs()
                val exePath = appExecutablePath()
                val plistContent = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                    <plist version="1.0">
                    <dict>
                        <key>Label</key>
                        <string>$MACOS_AGENT_LABEL</string>
                        <key>ProgramArguments</key>
                        <array>
                            <string>$exePath</string>
                        </array>
                        <key>RunAtLoad</key>
                        <true/>
                        <key>ProcessType</key>
                        <string>Interactive</string>
                    </dict>
                    </plist>
                """.trimIndent()
                file.writeText(plistContent)
                true
            }
            DesktopPlatform.isLinux -> {
                val file = linuxAutostartFile()
                file.parentFile?.mkdirs()
                val exePath = appExecutablePath()
                val desktopEntry = """
                    [Desktop Entry]
                    Type=Application
                    Name=BitChord
                    Comment=Aesthetic Music Player
                    Exec="$exePath"
                    Terminal=false
                    Categories=Audio;Music;Player;
                """.trimIndent()
                file.writeText(desktopEntry)
                true
            }
            DesktopPlatform.isWindows -> {
                val exePath = appExecutablePath()
                val proc = ProcessBuilder("reg", "add", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "BitChord", "/t", "REG_SZ", "/d", "\"$exePath\"", "/f").start()
                proc.waitFor() == 0
            }
            else -> false
        }
    }.onFailure {
        DesktopTrackLog.log("Could not enable autostart: ${it.message}")
    }.getOrDefault(false)

    private fun disableAutoStart(): Boolean = runCatching {
        when {
            DesktopPlatform.isMac -> {
                val file = launchAgentFile()
                if (file.exists()) file.delete() else true
            }
            DesktopPlatform.isLinux -> {
                val file = linuxAutostartFile()
                if (file.exists()) file.delete() else true
            }
            DesktopPlatform.isWindows -> {
                val proc = ProcessBuilder("reg", "delete", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "/v", "BitChord", "/f").start()
                proc.waitFor() == 0
            }
            else -> false
        }
    }.onFailure {
        DesktopTrackLog.log("Could not disable autostart: ${it.message}")
    }.getOrDefault(false)
}
