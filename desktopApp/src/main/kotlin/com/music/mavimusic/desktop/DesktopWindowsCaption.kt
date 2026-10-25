package com.music.mavimusic.desktop

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.win32.StdCallLibrary
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import java.awt.EventQueue
import java.awt.Frame
import java.lang.ref.WeakReference

/** DWM colors only: OS title buttons, Snap, system menu and resize semantics remain native. */
internal object DesktopWindowsCaption {
    private interface Dwm : StdCallLibrary {
        fun DwmSetWindowAttribute(window: HWND, attribute: Int, value: Pointer, size: Int): Int
    }
    private var window = WeakReference<Frame>(null)
    private var requested = Color(0xFF202020)
    private var lastApplied: Color? = null

    fun attach(frame: Frame) {
        if (!DesktopPlatform.isWindows) return
        window = WeakReference(frame)
        lastApplied = null
        refresh()
    }
    fun apply(color: Color) {
        if (!DesktopPlatform.isWindows) return
        requested = color
        if (lastApplied != color) refresh()
    }
    internal fun colorRef(color: Color): Int = (color.red * 255).toInt() or
        ((color.green * 255).toInt() shl 8) or ((color.blue * 255).toInt() shl 16)

    /** Returns whether the OS accepts both documented set-only color attributes. */
    internal fun applyTo(frame: Frame, color: Color): Boolean {
        val hwnd = HWND(Native.getComponentPointer(frame))
        val dwm = Native.load("dwmapi", Dwm::class.java)
        fun attribute(id: Int, value: Int): Boolean = Memory(4).use {
            it.setInt(0, value); dwm.DwmSetWindowAttribute(hwnd, id, it, 4) == 0
        }
        val dark = if (color.luminance() < 0.5f) 1 else 0
        if (!attribute(20, dark)) attribute(19, dark)
        val caption = attribute(35, colorRef(color))
        val text = attribute(36, colorRef(if (dark == 1) Color.White else Color.Black))
        return caption && text
    }

    fun refresh() {
        if (!DesktopPlatform.isWindows) return
        val update = {
            val frame = window.get()
            if (frame != null && frame.isDisplayable) runCatching {
                applyTo(frame, requested)
                lastApplied = requested
            }
            Unit
        }
        if (EventQueue.isDispatchThread()) update() else EventQueue.invokeLater(update)
    }
}
