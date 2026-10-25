package com.music.mavimusic.desktop
import kotlin.test.*
class DesktopHotkeyTest {
    @Test fun `global shortcut requires a modifier and supports conflict alternatives`() {
        assertNotNull(DesktopWindowsHotkey.parse("Ctrl+Alt+K"))
        assertNotNull(DesktopWindowsHotkey.parse("Ctrl+K"))
        assertNotNull(DesktopWindowsHotkey.parse("Ctrl+Shift+K"))
        for (invalid in listOf("K", "Shift+K", "Ctrl+Ctrl+K", "Ctrl+Alt+")) assertNull(DesktopWindowsHotkey.parse(invalid))
    }
}
