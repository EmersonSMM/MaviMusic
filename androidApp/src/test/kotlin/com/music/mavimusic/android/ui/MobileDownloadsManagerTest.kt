package com.music.mavimusic.android.ui

import com.music.mavimusic.android.data.UserPlaylist
import com.music.mavimusic.data.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class MobileDownloadsManagerTest {

    @Test
    fun `groups downloaded tracks cleanly by artist, album with Sencillos fallback, and playlists`() {
        val s1 = Song("v1", "Viva La Vida", "Coldplay", "art1", albumName = "Viva La Vida", trackNumber = 7)
        val s2 = Song("v2", "Lovers in Japan", "Coldplay", "art2", albumName = "Viva La Vida", trackNumber = 5)
        val s3 = Song("v3", "Yellow", "Coldplay", "art3", albumName = null)
        val s4 = Song("v4", "Bandida", "Zion y Lennox", "art4", albumName = "Motivando a la Yal")
        val all = listOf(s1, s2, s3, s4)

        val artists = groupOfflineArtists(all)
        assertEquals(listOf("Coldplay", "Zion y Lennox"), artists.map { it.name })
        assertEquals(2, artists[0].albumCount) // "Viva La Vida" + "Sencillos"
        assertEquals(3, artists[0].songs.size)

        val albums = groupOfflineAlbums(all)
        assertEquals(listOf("Motivando a la Yal", "Sencillos", "Viva La Vida"), albums.map { it.title })
        val viva = albums.first { it.title == "Viva La Vida" }
        // Sorted by trackNumber: 5 before 7
        assertEquals(listOf("v2", "v1"), viva.songs.map { it.videoId })

        val playlists = listOf(
            UserPlaylist("p1", "Viaje", listOf(s1, Song("not_dl", "Missing", "Other", null))),
            UserPlaylist("p2", "Clásicos", listOf(s2, s4)),
        )
        val groupedPlaylists = groupOfflinePlaylists(playlists, all)
        assertEquals(listOf("p2", "p1"), groupedPlaylists.map { it.id })
        assertEquals(2, groupedPlaylists[0].offlineSongs.size)
        assertEquals(1, groupedPlaylists[1].offlineSongs.size)
        assertEquals(2, groupedPlaylists[1].totalSongs)
    }
}
