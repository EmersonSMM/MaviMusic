package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.Song
import kotlin.test.*

class DesktopDownloadMetadataTest {
    private val song = Song("track", "Title", "Artist", null, fromAutoplay = false)

    @Test fun `album downloads carry the release title cover and track order`() {
        val collection = DesktopCollection("release", "Album", "Artist", "https://example.com/cover.webp", BrowseType.ALBUM, listOf(song))
        val tagged = DesktopDownloadMetadata.collectionSongs(collection).single()
        assertEquals("Album", tagged.albumName)
        assertEquals(collection.thumbnailUrl, tagged.thumbnailUrl)
        assertEquals(1, tagged.trackNumber)
    }

    @Test fun `a playlist title never becomes a track album name`() {
        val collection = DesktopCollection("playlist", "My Playlist", "", null, BrowseType.PLAYLIST, listOf(song))
        assertNull(DesktopDownloadMetadata.collectionSongs(collection).single().albumName)
    }

    @Test fun `catalogue enrichment only merges the same recording`() {
        assertNull(DesktopDownloadMetadata.merge(song, song.copy(videoId = "other", albumName = "Wrong")).albumName)
        assertEquals("Album", DesktopDownloadMetadata.merge(song, song.copy(albumName = "Album")).albumName)
        assertEquals("Keep", DesktopDownloadMetadata.merge(song.copy(albumName = "Keep"), song.copy(albumName = "Replace")).albumName)
    }

    @Test fun `ordinary macOS session operations never enter Keychain`() {
        if (!DesktopPlatform.isMac) return
        val attributes = mapOf("application" to "mavimusic", "account" to "no-native-access")
        assertNull(DesktopSecretStore.lookup(attributes))
        assertFalse(DesktopSecretStore.store("test", attributes, "synthetic".toByteArray()))
        assertFalse(DesktopSecretStore.remove(attributes))
        assertFalse(DesktopSecretStore.isAvailable())
    }
}
