package com.music.mavimusic.desktop

/** The running application's version; Sparkle compares the installed bundle's CFBundleVersion. */
internal object DesktopUpdateChecker {
    val currentVersion: String = System.getProperty("mavimusic.version") ?: "development"
}
