package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.Song
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo

/**
 * Controller for the macOS global floating Spotlight search window.
 * Allows searching songs, playlists, albums and executing playback commands
 * from anywhere in macOS via Cmd+K or Cmd+Shift+K.
 */
internal object DesktopSpotlightState {
    var isOpen by mutableStateOf(false)
    var screenX by mutableStateOf(100)
    var screenY by mutableStateOf(100)

    // Callbacks registered by DesktopApp / Main
    var onPlaySong: (Song) -> Unit = {}
    var onOpenCollection: (String, BrowseType) -> Unit = { _, _ -> }
    var onOpenArtist: (String, String) -> Unit = { _, _ -> }
    var onPlayLucky: () -> Unit = {}
    var onTogglePlayPause: () -> Unit = {}
    var onPlayNext: () -> Unit = {}
    var onPlayPrevious: () -> Unit = {}
    var onToggleShuffle: () -> Unit = {}
    var onOpenDownloads: () -> Unit = {}
    var onOpenSettings: () -> Unit = {}
    var onOpenLyrics: () -> Unit = {}
    var onOpenQueue: () -> Unit = {}
    var onNavigate: (DesktopDestination) -> Unit = {}

    fun toggle() {
        if (isOpen) {
            isOpen = false
        } else {
            show()
        }
    }

    fun show() {
        val p = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = runCatching { env.screenDevices }.getOrNull().orEmpty()

        val currentDevice = if (p != null) {
            screens.firstOrNull { device ->
                device.defaultConfiguration.bounds.contains(p)
            } ?: env.defaultScreenDevice
        } else {
            env.defaultScreenDevice
        }

        val bounds = currentDevice.defaultConfiguration.bounds
        val widthPx = 660
        val heightPx = 480

        screenX = bounds.x + (bounds.width - widthPx) / 2
        screenY = bounds.y + (bounds.height * 0.18).toInt()
        isOpen = true
        DesktopMacFrame.activateProcess()
    }

    fun dismiss() {
        isOpen = false
    }
}
