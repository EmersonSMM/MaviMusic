package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.HomeShelf
import com.music.mavimusic.data.model.LibraryPage
import com.music.mavimusic.data.model.ShelfItem

/** Apply playlist changes immediately, retaining every unrelated shelf and continuation. */
internal object DesktopLibraryEdits {
    fun playlists(page: LibraryPage, shelfTitle: String, edit: (List<ShelfItem>) -> List<ShelfItem>): LibraryPage {
        val index = page.shelves.indexOfFirst { it.title == shelfTitle }
        val shelves = if (index >= 0) page.shelves.toMutableList().apply {
            this[index] = this[index].copy(items = edit(this[index].items))
        } else {
            val items = edit(emptyList())
            if (items.isEmpty()) return page
            listOf(HomeShelf(shelfTitle, items)) + page.shelves
        }
        return page.copy(shelves = shelves.toList())
    }
}
