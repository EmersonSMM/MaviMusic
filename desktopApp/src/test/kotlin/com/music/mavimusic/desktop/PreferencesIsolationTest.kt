package com.music.mavimusic.desktop

import java.util.prefs.Preferences
import kotlin.test.Test
import kotlin.test.assertTrue

class PreferencesIsolationTest {
    @Test fun `the test JVM cannot open the listener's operating system preferences`() {
        assertTrue(Preferences.userRoot().javaClass.name.contains("MemoryPreferencesFactory"),
            "Use the in-memory factory; macOS ignores java.util.prefs.userRoot")
    }
}
