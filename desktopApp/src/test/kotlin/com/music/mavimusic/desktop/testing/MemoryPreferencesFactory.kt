package com.music.mavimusic.desktop.testing

import java.util.concurrent.ConcurrentHashMap
import java.util.prefs.AbstractPreferences
import java.util.prefs.Preferences
import java.util.prefs.PreferencesFactory

/** macOS ignores java.util.prefs.userRoot: use an explicit factory to isolate every test JVM. */
class MemoryPreferencesFactory : PreferencesFactory {
    private val user = MemoryNode(null, "")
    private val system = MemoryNode(null, "")
    override fun userRoot(): Preferences = user
    override fun systemRoot(): Preferences = system

    private class MemoryNode(private val owner: MemoryNode?, name: String) : AbstractPreferences(owner, name) {
        private val values = ConcurrentHashMap<String, String>()
        private val nodes = ConcurrentHashMap<String, MemoryNode>()
        override fun putSpi(key: String, value: String) { values[key] = value }
        override fun getSpi(key: String): String? = values[key]
        override fun removeSpi(key: String) { values.remove(key) }
        override fun keysSpi(): Array<String> = values.keys.toTypedArray()
        override fun childrenNamesSpi(): Array<String> = nodes.keys.toTypedArray()
        override fun childSpi(name: String): AbstractPreferences = nodes.computeIfAbsent(name) { MemoryNode(this, it) }
        override fun removeNodeSpi() { values.clear(); nodes.clear(); owner?.nodes?.remove(name()) }
        override fun syncSpi() = Unit
        override fun flushSpi() = Unit
    }
}
