package com.music.mavimusic.desktop

import java.util.prefs.Preferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Imports the former desktop namespace by its data schema, without retaining an old product ID. */
internal object DesktopPreferenceMigration {
    private val importer = DesktopPreferenceImporter()
    fun open(): Preferences = importer.open()
    fun legacyCookie(key: String): String? = importer.legacyCookie(key)
    fun clearLegacyCookie(key: String) = importer.clearLegacyCookie(key)
    fun finish() = importer.finish()
}

internal class DesktopPreferenceImporter {
    private val CURRENT = "com.music.mavimusic.desktop"
    private val legacy = mutableListOf<Preferences>()
    private val opened = mutableMapOf<String, Preferences>()

    @Synchronized
    fun open(root: Preferences = Preferences.userRoot()): Preferences {
        opened[root.absolutePath()]?.let { return it }
        val target = root.node(CURRENT)
        if (target.getBoolean("namespace_migrated_v1", false)) return target.also { opened[root.absolutePath()] = it }
        root.childrenNames().filter { it != CURRENT && it.startsWith("com.music.") && it.endsWith(".desktop") }
            .forEach { name ->
                val candidate = root.node(name)
                val keys = candidate.keys().map { it.replace(Regex("\\.\\d+$"), "") }.toSet()
                if (keys.none { it == "google_accounts_v1" || it == "liked_ids" || it == "queue" }) return@forEach
                keys.filterNot(::isCookie).forEach { key ->
                    val old = DesktopPreferenceChunks.read(candidate, key) ?: return@forEach
                    val current = DesktopPreferenceChunks.read(target, key)
                    when {
                        current == null -> {
                            if (candidate.get("$key.0", null) != null) DesktopPreferenceChunks.write(target, key, old)
                            else target.put(key, old)
                        }
                        current != old -> {
                            // Keep conflicting state recoverable without preserving an old application ID.
                            DesktopPreferenceChunks.write(target.node("migration-archive"), key, old)
                            mergeArrays(current, old)?.let { DesktopPreferenceChunks.write(target, key, it) }
                        }
                    }
                }
                if (legacy.none { it.absolutePath() == candidate.absolutePath() }) legacy += candidate
            }
        target.flush()
        return target.also { opened[root.absolutePath()] = it }
    }

    private fun mergeArrays(current: String, old: String): String? = runCatching {
        val a = Json.parseToJsonElement(current) as? JsonArray ?: return null
        val b = Json.parseToJsonElement(old) as? JsonArray ?: return null
        JsonArray((a + b).distinctBy { item ->
            val obj = item as? JsonObject
            obj?.get("accountId") ?: obj?.get("videoId") ?: obj?.get("id") ?: item
        }).toString()
    }.getOrNull()

    private fun isCookie(key: String) = key.startsWith("google_cookie_") || key == "spotify_spdc_token"

    /** Existing sessions remain recoverable during namespace import. */
    @Synchronized
    fun legacyCookie(key: String): String? = legacy.firstNotNullOfOrNull { DesktopPreferenceChunks.read(it, key)?.takeIf(String::isNotBlank) }

    @Synchronized
    fun clearLegacyCookie(key: String) { legacy.forEach { DesktopPreferenceChunks.remove(it, key) } }

    /** Retire the previous namespace only after its local sessions have been imported. */
    @Synchronized
    fun finish(root: Preferences = Preferences.userRoot()) {
        legacy.forEach { it.removeNode() }
        legacy.clear()
        root.node(CURRENT).apply { putBoolean("namespace_migrated_v1", true); flush() }
    }
}
