package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlin.test.*

class DesktopDownloadCatalogTest {
    private fun song(id: String, path: String = "/music/$id.mp3") = Song(id, id, "Artista", null, localPath = path)
    @Test fun `more than 200 downloads remain recorded and survive reloading`() {
        var disk = emptyList<Song>()
        val catalog = DesktopDownloadCatalog({ disk }, { disk = it })
        repeat(260) { catalog.add(song("track-$it")) }
        assertEquals(260, catalog.songs.value.size)
        assertEquals(catalog.songs.value, DesktopDownloadCatalog({ disk }, { disk = it }).songs.value)
        catalog.add(song("track-5", "/new/track-5.mp3"))
        assertEquals(260, catalog.songs.value.size)
        assertEquals("/new/track-5.mp3", catalog.songs.value.first().localPath)
    }
    @Test fun `a slow disk verification cannot erase a new download or repair`() {
        val old = song("old")
        val updated = old.copy(albumName = "Álbum recuperado")
        val catalog = DesktopDownloadCatalog({ listOf(old) }, {})
        val checked = catalog.songs.value
        catalog.repair(old, updated)
        catalog.add(song("new"))
        catalog.verified(checked, emptyList())
        assertEquals(listOf(song("new"), updated), catalog.songs.value)
    }
    @Test fun `finishing a repair cannot resurrect a removed download`() {
        val original = song("old")
        val catalog = DesktopDownloadCatalog({ listOf(original) }, {})
        catalog.remove(original)
        catalog.repair(original, original.copy(albumName = "Álbum"))
        assertTrue(catalog.songs.value.isEmpty())
    }
    @Test fun `deleting an older copy cannot remove a newer export of the same song`() {
        val original = song("old")
        val replacement = original.copy(localPath = "/playlist/old.mp3")
        val catalog = DesktopDownloadCatalog({ listOf(original) }, {})
        catalog.add(replacement)
        catalog.remove(original)
        assertEquals(listOf(replacement), catalog.songs.value)
    }
    @Test fun `failed persistence never publishes a successful catalog update`() {
        val catalog = DesktopDownloadCatalog({ emptyList() }, { error("disk unavailable") })
        assertFailsWith<IllegalStateException> { catalog.add(song("new")) }
        assertTrue(catalog.songs.value.isEmpty())
    }
}
