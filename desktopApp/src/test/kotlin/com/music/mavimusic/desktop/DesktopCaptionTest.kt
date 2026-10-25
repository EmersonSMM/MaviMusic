package com.music.mavimusic.desktop
import androidx.compose.ui.graphics.Color
import kotlin.test.*
class DesktopCaptionTest {
    @Test fun `DWM uses COLORREF byte order rather than ARGB`() {
        assertEquals(0x000000FF, DesktopWindowsCaption.colorRef(Color.Red))
        assertEquals(0x00FF0000, DesktopWindowsCaption.colorRef(Color.Blue))
        assertEquals(0x00FFFFFF, DesktopWindowsCaption.colorRef(Color.White))
    }
}
