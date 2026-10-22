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
    var openedExternally by mutableStateOf(false)
    var isDragging by mutableStateOf(false)

    @Volatile
    var isSearchFieldFocused: Boolean = false

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
    var onOpenImmersive: () -> Unit = {}
    var onNavigate: (DesktopDestination) -> Unit = {}

    fun toggle(external: Boolean = false) {
        if (isOpen) {
            dismiss()
        } else {
            show(external)
        }
    }

    fun show(external: Boolean = false) {
        openedExternally = external
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

        screenX = bounds.x + (bounds.width - widthPx) / 2
        screenY = bounds.y + (bounds.height * 0.18).toInt()
        isOpen = true
    }

    /** Moves the Spotlight window to the other monitor when working with multiple displays. */
    fun moveToOtherScreen() {
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = runCatching { env.screenDevices }.getOrNull().orEmpty()
        if (screens.size <= 1) return

        val centerPoint = java.awt.Point(screenX + 330, screenY + 100)
        val currentIdx = screens.indexOfFirst { it.defaultConfiguration.bounds.contains(centerPoint) }
            .takeIf { it >= 0 } ?: 0
        val nextDevice = screens[(currentIdx + 1) % screens.size]
        val bounds = nextDevice.defaultConfiguration.bounds
        val widthPx = 660

        screenX = bounds.x + (bounds.width - widthPx) / 2
        screenY = bounds.y + (bounds.height * 0.18).toInt()
    }

    fun dismiss(returnFocusToExternalApp: Boolean = true) {
        val wasExternal = openedExternally
        isOpen = false
        openedExternally = false
        isDragging = false
        if (wasExternal && returnFocusToExternalApp) {
            DesktopMacMedia.onSpotlightDismissed(true)
        }
    }
}

