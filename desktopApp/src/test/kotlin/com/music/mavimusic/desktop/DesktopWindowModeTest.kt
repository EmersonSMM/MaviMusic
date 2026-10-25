package com.music.mavimusic.desktop

import androidx.compose.ui.window.WindowPlacement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWindowModeTest {
    @Test fun `fullscreen returns to the previous maximized placement`() {
        try {
            DesktopWindowMode.adopt(WindowPlacement.Maximized)
            DesktopWindowMode.toggle()
            assertTrue(DesktopWindowMode.fullScreen.value)
            DesktopWindowMode.exit()
            assertFalse(DesktopWindowMode.fullScreen.value)
            assertEquals(WindowPlacement.Maximized, DesktopWindowMode.placement.value)
        } finally { DesktopWindowMode.exit(); DesktopWindowMode.adopt(WindowPlacement.Floating) }
    }

    @Test fun `fullscreen returns to floating and repeated exit is harmless`() {
        DesktopWindowMode.adopt(WindowPlacement.Floating)
        DesktopWindowMode.toggle()
        DesktopWindowMode.exit()
        DesktopWindowMode.exit()
        assertEquals(WindowPlacement.Floating, DesktopWindowMode.placement.value)
        assertFalse(DesktopWindowMode.fullScreen.value)
    }
}
