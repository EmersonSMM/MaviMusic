package com.music.mavimusic.desktop

import java.awt.Desktop
import java.awt.EventQueue
import java.awt.Frame
import java.awt.Window
import java.awt.desktop.AppHiddenEvent
import java.awt.desktop.AppHiddenListener
import java.awt.desktop.AppReopenedListener
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the window is on screen, and what closing it means. */
internal object DesktopWindowVisibility {

    private val _visible = MutableStateFlow(true)

    fun startHidden() {
        _visible.value = false
        refreshRendering()
    }

    /** What the window's own `visible` is driven from. */
    val visible: StateFlow<Boolean> = _visible
    private val _renderActive = MutableStateFlow(true)
    val renderActive: StateFlow<Boolean> = _renderActive

    /** Whether a close should hide rather than quit. */
    @Volatile
    var keepRunningWhenClosed: Boolean = false

    private var windowRef: WeakReference<Window>? = null
    private var isListenerInstalled = false
    @Volatile
    private var appHidden: Boolean = false

    internal fun shouldRenderWindow(
        visible: Boolean,
        showing: Boolean,
        iconified: Boolean,
        appHidden: Boolean = false,
    ): Boolean = visible && showing && !iconified && !appHidden

    private fun refreshRendering(window: Window? = windowRef?.get()) {
        val showing = window?.isShowing ?: true
        val iconified = window is Frame && (window.extendedState and Frame.ICONIFIED) != 0
        _renderActive.value = shouldRenderWindow(_visible.value, showing, iconified, appHidden)
    }

    /** Attaches the active Compose window so show() can bring it to the foreground. */
    fun attachWindow(window: Window) {
        windowRef = WeakReference(window)
        window.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentShown(event: java.awt.event.ComponentEvent) = refreshRendering(window)
            override fun componentHidden(event: java.awt.event.ComponentEvent) = refreshRendering(window)
        })
        if (window is Frame) window.addWindowStateListener { refreshRendering(window) }
        refreshRendering(window)
    }

    /**
     * Installs system-level reopen and hide hooks (e.g. macOS Dock click or Cmd+H).
     */
    fun install() {
        if (isListenerInstalled) return
        isListenerInstalled = true

        if (DesktopPlatform.isMac && Desktop.isDesktopSupported()) {
            runCatching {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.APP_EVENT_REOPENED)) {
                    desktop.addAppEventListener(AppReopenedListener {
                        EventQueue.invokeLater {
                            DesktopTrackLog.log("AppReopenedEvent received from macOS Dock")
                            show()
                        }
                    })
                    DesktopTrackLog.log("DesktopWindowVisibility: AppReopenedListener registered")
                }
                if (desktop.isSupported(Desktop.Action.APP_EVENT_HIDDEN)) {
                    desktop.addAppEventListener(object : AppHiddenListener {
                        override fun appHidden(event: AppHiddenEvent?) {
                            appHidden = true
                            refreshRendering()
                        }

                        override fun appUnhidden(event: AppHiddenEvent?) {
                            appHidden = false
                            refreshRendering()
                        }
                    })
                }
            }.onFailure {
                DesktopTrackLog.log("DesktopWindowVisibility: failed to register macOS app listeners: ${it.message}")
            }
        }
    }

    /** Brings the window back — from the Dock, tray menu, or the icon itself. */
    fun show() {
        appHidden = false
        _visible.value = true
        refreshRendering()
        EventQueue.invokeLater {
            val win = windowRef?.get()
            if (win != null) {
                if (win is Frame && (win.extendedState and Frame.ICONIFIED) != 0) {
                    win.extendedState = win.extendedState and Frame.ICONIFIED.inv()
                }
                win.isVisible = true
                win.toFront()
                win.requestFocus()
                refreshRendering(win)
            }
            if (DesktopPlatform.isMac) {
                runCatching {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_REQUEST_FOREGROUND)) {
                        Desktop.getDesktop().requestForeground(true)
                    }
                }
                DesktopMacFrame.activateApp()
            }
        }
    }

    /**
     * Handles the window's close request.
     * @return true when the caller should end the application; false when the
     */
    fun onCloseRequest(): Boolean {
        if (!keepRunningWhenClosed) return true
        // A window put away full-screen comes back full-screen with nothing on it, which reads as
        // the app having broken rather than as it having been hidden.
        DesktopWindowMode.exit()
        _visible.value = false
        refreshRendering()
        DesktopTrackLog.log("window closed to the tray; playback continues")
        return false
    }
}
