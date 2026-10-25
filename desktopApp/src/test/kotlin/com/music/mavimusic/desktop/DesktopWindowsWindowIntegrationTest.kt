package com.music.mavimusic.desktop

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.RECT
import com.sun.jna.platform.win32.WinUser
import java.awt.EventQueue
import java.awt.Frame
import java.awt.Rectangle
import javax.swing.JFrame
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** A separate opt-in JVM opens only a test HWND; never the app or a listener's preferences. */
class DesktopWindowsWindowIntegrationTest {
    @Test fun `standard caption fullscreen stays within the monitor and restores placement`() = runBlocking {
        org.junit.Assume.assumeTrue(DesktopPlatform.isWindows &&
            java.lang.Boolean.getBoolean("mavimusic.test.windowsWindow"))
        lateinit var frame: JFrame
        EventQueue.invokeAndWait {
            frame = JFrame("MaviMusic").apply {
                setBounds(60, 60, 900, 600)
                isVisible = true
            }
        }
        try {
            assertTrue(DesktopWindowsFrame.install("MaviMusic"), "Windows JNI bridge must load")
            val hwnd = HWND(Native.getComponentPointer(frame))
            val user32 = User32.INSTANCE
            // Exercise the real service, not only its shortcut parser. This JVM uses memory preferences.
            DesktopWindowsHotkey.configure("Ctrl+Alt+Shift+Q")
            val shortcut = withTimeout(6000) {
                DesktopWindowsHotkey.state.first { it.shortcut == "Ctrl+Alt+Shift+Q" && it.message.isNotBlank() }
            }
            assertTrue(shortcut.registered, shortcut.message)
            DesktopWindowsHotkey.stop()
            val parsed = DesktopWindowsHotkey.parse("Ctrl+Alt+Shift+Q")!!
            val probeId = 0x4D57
            try {
                withTimeout(6000) {
                    while (!user32.RegisterHotKey(null, probeId, parsed.modifiers, parsed.key)) delay(50)
                }
            } finally { user32.UnregisterHotKey(null, probeId) }
            val originalStyle = user32.GetWindowLong(hwnd, WinUser.GWL_STYLE)
            assertTrue(originalStyle and WinUser.WS_CAPTION != 0, "Installing the bridge removed the native caption")
            fun nativeBounds(): Rectangle = RECT().also {
                assertTrue(user32.GetWindowRect(hwnd, it))
            }.toRectangle()
            val original = nativeBounds()
            repeat(3) {
                assertTrue(DesktopWindowsFrame.setFullscreen(true))
                EventQueue.invokeAndWait { frame.validate() }
                val monitor = WinUser.MONITORINFO()
                assertTrue(user32.GetMonitorInfo(user32.MonitorFromWindow(hwnd, WinUser.MONITOR_DEFAULTTONEAREST), monitor).booleanValue())
                assertEquals(monitor.rcMonitor.toRectangle(), nativeBounds(), "Fullscreen must not overscan or leave controls below the monitor")
                assertEquals(0, frame.insets.top + frame.insets.bottom, "Fullscreen content must not retain caption insets")
                assertTrue(DesktopWindowsFrame.setFullscreen(false))
                assertEquals(original, nativeBounds(), "Normal position/size was not restored")
                assertEquals(originalStyle, user32.GetWindowLong(hwnd, WinUser.GWL_STYLE))
            }
            EventQueue.invokeAndWait { frame.extendedState = Frame.MAXIMIZED_BOTH }
            // SendMessage is a synchronous round trip through the HWND owner thread.
            assertTrue(DesktopWindowsFrame.setFullscreen(true))
            assertTrue(DesktopWindowsFrame.setFullscreen(false))
            val placement = WinUser.WINDOWPLACEMENT()
            assertTrue(user32.GetWindowPlacement(hwnd, placement).booleanValue())
            assertEquals(WinUser.SW_SHOWMAXIMIZED, placement.showCmd, "Exiting fullscreen must restore a maximized window")
        } finally {
            EventQueue.invokeAndWait { frame.dispose() }
        }
    }
}
