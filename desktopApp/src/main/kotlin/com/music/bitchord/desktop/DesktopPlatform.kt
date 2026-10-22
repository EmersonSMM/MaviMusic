package com.music.bitchord.desktop

/** Which desktop this is running on. */
internal object DesktopPlatform {

    private val name: String = System.getProperty("os.name").orEmpty()

    val isWindows: Boolean = name.startsWith("Windows", ignoreCase = true)

    val isLinux: Boolean = name.contains("linux", ignoreCase = true)

    val isMac: Boolean = name.contains("mac", ignoreCase = true)

    /**
     * Whether the application draws the window's own frame instead of the system drawing it.
     * On macOS, we delegate window borders and resizing to native AppKit (Cocoa) with
     * apple.awt.transparentTitleBar and apple.awt.fullWindowContent so that macOS WindowServer
     * handles 120 FPS hardware-accelerated resizing and native vector cursors.
     */
    val drawsOwnWindowFrame: Boolean = isWindows
}
