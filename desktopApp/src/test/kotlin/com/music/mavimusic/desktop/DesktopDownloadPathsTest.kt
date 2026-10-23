package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import java.nio.file.Path
import kotlin.test.*

class DesktopDownloadPathsTest {
    private val song = Song("audio", "Mentí", "Ráfaga", null, albumName = "Otra dimensión", trackNumber = 2)
    private val root = Path.of("/exports")
    @Test fun `standalone album and playlist have distinct stable destinations`() {
        val artist = DesktopDownloadPaths.target(root, song, DesktopDownloadContext())
        val album = DesktopDownloadPaths.target(root, song, DesktopDownloadContext(DesktopDownloadContext.Kind.ALBUM, "Otra dimensión", "Ráfaga"))
        val playlistContext = DesktopDownloadContext(DesktopDownloadContext.Kind.PLAYLIST, "Mis favoritas")
        val playlist = DesktopDownloadPaths.target(root, song, playlistContext)
        assertEquals(root.resolve("Artistas/Ráfaga/Otra dimensión"), artist.parent)
        assertEquals(root.resolve("Álbumes/Ráfaga/Otra dimensión"), album.parent)
        assertTrue(album.fileName.toString().startsWith("02 - "))
        assertEquals(root.resolve("Playlists/Mis favoritas"), playlist.parent)
        assertTrue(DesktopDownloadPaths.belongsTo(playlist.toString(), playlistContext))
        assertFalse(DesktopDownloadPaths.belongsTo(artist.toString(), playlistContext))
    }
    @Test fun `unsafe folder names cannot escape the output directory`() {
        val context = DesktopDownloadContext(DesktopDownloadContext.Kind.PLAYLIST, "../../con/\u0000:")
        val path = DesktopDownloadPaths.target(root, song.copy(artist = "../Artist", title = "x/y"), context).normalize()
        assertTrue(path.startsWith(root))
        assertEquals(root.nameCount + 3, path.nameCount)
        assertEquals("_CON", DesktopDownloadPaths.component("CON", "fallback"))
    }
    @Test fun `distinct recordings with identical names never overwrite each other`() {
        assertNotEquals(DesktopDownloadPaths.target(root, song, DesktopDownloadContext()),
            DesktopDownloadPaths.target(root, song.copy(videoId = "another-recording"), DesktopDownloadContext()))
    }
}
