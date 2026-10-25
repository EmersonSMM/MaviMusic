package com.music.mavimusic.desktop

import java.awt.Dimension
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWindowsWindowLayoutTest {
    @Test fun `controls fit below the caption and above a visible taskbar on a laptop`() {
        val work = Rectangle(0, 0, 1366, 728)
        val fitted = DesktopWindowsWindowLayout.fit(Rectangle(73, 30, 1220, 780), work)
        assertTrue(work.contains(fitted))
        assertEquals(728, fitted.height)
        assertEquals(Dimension(1024, 640), DesktopWindowsWindowLayout.minimum(work))
    }

    @Test fun `logical work areas at fractional DPI never get multiplied a second time`() {
        for (scale in listOf(1.0, 1.25, 1.5, 2.0)) {
            val work = Rectangle(0, 0, (1920 / scale).toInt(), (1040 / scale).toInt())
            val fitted = DesktopWindowsWindowLayout.fit(Rectangle(50, 50, 1220, 780), work)
            assertTrue(work.contains(fitted), "scale=$scale, fitted=$fitted")
            val minimum = DesktopWindowsWindowLayout.minimum(work)
            assertTrue(minimum.width <= work.width && minimum.height <= work.height)
        }
    }

    @Test fun `moving to a smaller monitor with a left taskbar retains accessible controls`() {
        val work = Rectangle(-1258, 0, 1258, 720)
        val fitted = DesktopWindowsWindowLayout.fit(Rectangle(1700, 600, 1600, 1000), work)
        assertTrue(work.contains(fitted))
        assertEquals(Rectangle(-1258, 0, 1258, 720), fitted)
    }

    @Test fun `a window that already fits is not moved or resized`() {
        val work = Rectangle(1920, 40, 2560, 1400)
        val original = Rectangle(2100, 120, 1220, 780)
        assertEquals(original, DesktopWindowsWindowLayout.fit(original, work))
    }
}
