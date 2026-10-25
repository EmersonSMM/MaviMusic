package com.music.mavimusic.desktop

import androidx.compose.runtime.mutableStateListOf
import com.music.mavimusic.data.model.HomeShelf
import com.music.mavimusic.data.model.MoodGenre

internal enum class DesktopDestination(val id: String, val label: String) {
    LISTEN_NOW("listen-now", "Inicio"),
    EXPLORE("explore", "Explorar"),
    LIBRARY("library", "Biblioteca"),
    SEARCH("search", "Buscar"),
    HISTORY("history", "Historial"),
    DOWNLOADS("downloads", "Descargas"),
    LOCAL_MUSIC("local-music", "Música local"),
    STATS("stats", "Estadísticas"),
    SETTINGS("settings", "Ajustes"),
}

internal data class DesktopArtistTarget(val browseId: String, val name: String)

/** One place the window has been: which page, and whatever was open on it. */
internal data class DesktopNavEntry(
    val destination: DesktopDestination,
    val artist: DesktopArtistTarget?,
    val collection: DesktopCollection?,
    val mood: MoodGenre?,
    val showAll: HomeShelf?,
    val replay: Boolean,
    val settings: DesktopSettingsPage?,
) {
    /** What makes two entries the same place: an album that paged in more tracks has not moved. */
    val key: List<Any?>
        get() = listOf(destination, artist?.browseId, collection?.browseId, mood?.browseId, mood?.params, showAll?.title, replay, settings)
}

/** Deep enough for any way back anyone takes; the oldest fall off. */
internal const val NAV_HISTORY_LIMIT = 50

/** Bounded history shared by sidebar, cards, search, keyboard and mouse Back. */
internal class DesktopNavigationHistory {
    private val entries = mutableStateListOf<DesktopNavEntry>()
    private var last: DesktopNavEntry? = null
    private var restoring = false
    val canGoBack: Boolean get() = entries.isNotEmpty()
    fun visit(now: DesktopNavEntry) {
        val before = last
        if (before != null && before.key != now.key) {
            if (restoring) restoring = false else {
                entries.add(before)
                if (entries.size > NAV_HISTORY_LIMIT) entries.removeAt(0)
            }
        }
        // Keep freshly paged collection data even when the location did not change.
        last = now
    }
    fun back(current: DesktopNavEntry): DesktopNavEntry? {
        val entry = entries.removeLastOrNull() ?: return null
        if (entry.key == current.key) return null
        restoring = true
        return entry
    }
}
