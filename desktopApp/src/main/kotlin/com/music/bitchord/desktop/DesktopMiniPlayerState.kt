package com.music.bitchord.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.bitchord.data.model.Song
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Point

/**
 * Global state controller for the macOS top status bar Mini Player widget.
 */
internal object DesktopMiniPlayerState {
    var isOpen by mutableStateOf(false)
    var screenX by mutableStateOf(100)
    var screenY by mutableStateOf(30)
    var lastLostFocusTime: Long = 0L
    private var lastToggleTime: Long = 0L

    // Playback state exposed for the mini player
    var song by mutableStateOf<Song?>(null)
    var isPlaying by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var positionMs by mutableStateOf(0L)
    var durationMs by mutableStateOf(0L)
    var volume by mutableStateOf(1f)
    var previousEnabled by mutableStateOf(false)

    // Callbacks
    var onPlayPause: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onPrevious: () -> Unit = {}
    var onSeek: (Long) -> Unit = {}
    var onVolumeChange: (Float) -> Unit = {}
    var onOpenNowPlaying: () -> Unit = {}
    var onOpenLyrics: () -> Unit = {}
    var onOpenQueue: () -> Unit = {}
    var onToggleExpand: () -> Unit = {}
    var onFeelingLucky: () -> Unit = {}
    var onOpenSongMenu: (Song) -> Unit = {}

    fun toggle(clickPoint: Point? = null) {
        val now = System.currentTimeMillis()
        if (now - lastToggleTime < 350) return
        lastToggleTime = now

        if (isOpen || (now - lastLostFocusTime < 350)) {
            isOpen = false
        } else {
            show(clickPoint)
        }
    }

    fun show(clickPoint: Point? = null) {
        val p = clickPoint ?: runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
        val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val screens = runCatching { env.screenDevices }.getOrNull().orEmpty()

        // Find the monitor that contains the mouse click position (multi-monitor support)
        val currentDevice = if (p != null) {
            screens.firstOrNull { device ->
                device.defaultConfiguration.bounds.contains(p)
            } ?: env.defaultScreenDevice
        } else {
            env.defaultScreenDevice
        }

        val bounds = currentDevice.defaultConfiguration.bounds
        val widgetWidthPx = 384

        val minX = bounds.x + 12
        val maxX = bounds.x + bounds.width - widgetWidthPx - 12
        val x = if (p != null) {
            (p.x - widgetWidthPx / 2).coerceIn(minX, maxX)
        } else {
            maxX
        }
        val y = bounds.y + 28

        screenX = x
        screenY = y
        isOpen = true
    }
}
