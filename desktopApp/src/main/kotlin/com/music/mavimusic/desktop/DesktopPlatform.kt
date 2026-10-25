package com.music.mavimusic.desktop

/** Which desktop this is running on. */
internal object DesktopPlatform {

    private val name: String = System.getProperty("os.name").orEmpty()

    val isWindows: Boolean = name.startsWith("Windows", ignoreCase = true)

    val isLinux: Boolean = name.contains("linux", ignoreCase = true)

    val isMac: Boolean = name.contains("mac", ignoreCase = true)

    /**
     * Whether the application draws a custom faux frame instead of the system drawing it.
     * We delegate window borders, resizing, High-DPI scaling and caption buttons to the native
     * OS (AppKit on macOS, DWM on Windows, and X11/Wayland on Linux) for maximum performance,
     * native buttons, and proper hardware acceleration without clipping.
     */
    val drawsOwnWindowFrame: Boolean = false
}
