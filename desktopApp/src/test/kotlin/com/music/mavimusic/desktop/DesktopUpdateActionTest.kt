package com.music.mavimusic.desktop
import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
class DesktopUpdateActionTest {
    @Test fun `white accent and dark backgrounds retain contrasting button text`() {
        assertEquals(Color.Black, updateActionForeground(Color.White))
        assertEquals(Color.White, updateActionForeground(Color.Black))
        assertEquals(Color.White, updateActionForeground(Color(0xFF242428)))
    }
}
