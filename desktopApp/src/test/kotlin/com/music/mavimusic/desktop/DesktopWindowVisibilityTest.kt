package com.music.mavimusic.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopWindowVisibilityTest {
    @Test fun renderingSuspendsWhenHiddenOrMinimizedRegardlessOfPlatform() {
        assertTrue(DesktopWindowVisibility.shouldRenderWindow(visible = true, showing = true, iconified = false))
        assertFalse(DesktopWindowVisibility.shouldRenderWindow(visible = false, showing = true, iconified = false))
        assertFalse(DesktopWindowVisibility.shouldRenderWindow(visible = true, showing = false, iconified = false))
        assertFalse(DesktopWindowVisibility.shouldRenderWindow(visible = true, showing = true, iconified = true))
        assertFalse(DesktopWindowVisibility.shouldRenderWindow(visible = true, showing = true, iconified = false, appHidden = true))
    }
}
