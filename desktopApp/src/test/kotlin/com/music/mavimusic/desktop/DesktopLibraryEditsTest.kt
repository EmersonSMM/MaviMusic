package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.*
import kotlin.test.*

class DesktopLibraryEditsTest {
    private val playlist = ShelfItem("Mi playlist", "", null, null, "VLplaylist")
    @Test fun `a first playlist appears immediately without losing likes library or continuation`() {
        val song = Song("audio", "Canción", "Artista", null)
        val page = LibraryPage(listOf(song), listOf(song), listOf(HomeShelf("Albums", emptyList())), "next")
        val result = DesktopLibraryEdits.playlists(page, "Playlists") { it + playlist }
        assertEquals(listOf("Playlists", "Albums"), result.shelves.map { it.title })
        assertEquals(listOf(playlist), result.shelves.first().items)
        assertEquals(page.likedSongs, result.likedSongs)
        assertEquals(page.librarySongs, result.librarySongs)
        assertEquals("next", result.likedContinuation)
    }
    @Test fun `renaming or removing a playlist preserves its shelf metadata and neighboring shelves`() {
        val shelf = HomeShelf("Playlists", listOf(playlist), subtitle = "Tu colección", moreBrowseId = "browse-more")
        val page = LibraryPage(emptyList(), emptyList(), listOf(HomeShelf("Albums", emptyList()), shelf))
        val renamed = DesktopLibraryEdits.playlists(page, "Playlists") { it.map { row -> row.copy(title = "Nuevo nombre") } }
        assertEquals(shelf.copy(items = listOf(playlist.copy(title = "Nuevo nombre"))), renamed.shelves[1])
        val removed = DesktopLibraryEdits.playlists(renamed, "Playlists") { emptyList() }
        assertEquals(shelf.copy(items = emptyList()), removed.shelves[1])
        assertEquals(page.shelves[0], removed.shelves[0])
    }
}
