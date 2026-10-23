package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.Song
import kotlin.test.*

class DesktopNavigationHistoryTest {
    private fun page(destination: DesktopDestination) = DesktopNavEntry(destination, null, null, null, null, false, null)
    @Test fun `back restores visits without creating a new visit`() {
        val history = DesktopNavigationHistory()
        val home = page(DesktopDestination.LISTEN_NOW)
        val search = page(DesktopDestination.SEARCH)
        val library = page(DesktopDestination.LIBRARY)
        history.visit(home); history.visit(search); history.visit(library)
        val previous = history.back(library)!!
        assertEquals(search, previous)
        history.visit(previous)
        assertEquals(home, history.back(previous))
        assertFalse(history.canGoBack)
    }
    @Test fun `paging a collection updates its restored data without adding another location`() {
        val history = DesktopNavigationHistory()
        val album = DesktopCollection("album", "Álbum", "Artista", null, BrowseType.ALBUM, emptyList())
        val first = page(DesktopDestination.LIBRARY).copy(collection = album)
        val loaded = first.copy(collection = album.copy(songs = listOf(Song("audio", "Canción", "Artista", null))))
        history.visit(first); history.visit(loaded)
        assertFalse(history.canGoBack)
        val search = page(DesktopDestination.SEARCH)
        history.visit(search)
        assertEquals(loaded, history.back(search))
    }
    @Test fun `history stays bounded for a long browsing session`() {
        val history = DesktopNavigationHistory()
        var current = page(DesktopDestination.LIBRARY)
        repeat(100) { i -> current = current.copy(artist = DesktopArtistTarget("artist-$i", "Artista")); history.visit(current) }
        var count = 0
        while (history.canGoBack) { current = history.back(current)!!; history.visit(current); count++ }
        assertEquals(NAV_HISTORY_LIMIT, count)
    }
}
