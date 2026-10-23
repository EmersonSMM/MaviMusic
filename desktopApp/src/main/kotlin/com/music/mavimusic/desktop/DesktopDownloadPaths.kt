package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.Song
import java.nio.file.Path

/** Capture the page when the user asks, so navigation cannot change a queued destination. */
data class DesktopDownloadContext(val kind: Kind = Kind.ARTIST, val title: String = "", val artist: String = "") {
    enum class Kind { ARTIST, ALBUM, PLAYLIST }
    companion object {
        internal fun from(collection: DesktopCollection?): DesktopDownloadContext = when (collection?.type) {
            BrowseType.ALBUM -> DesktopDownloadContext(Kind.ALBUM, collection.title, collection.songs.firstOrNull()?.artist.orEmpty())
            BrowseType.PLAYLIST -> DesktopDownloadContext(Kind.PLAYLIST, collection.title)
            else -> DesktopDownloadContext()
        }
    }
}

internal object DesktopDownloadPaths {
    fun staging(root: Path, id: String, context: DesktopDownloadContext): Path {
        val key = java.security.MessageDigest.getInstance("SHA-256")
            .digest("$id\u0000${context.kind}\u0000${context.title}\u0000${context.artist}".toByteArray())
            .joinToString("") { "%02x".format(it) }
        return root.resolve(".mavimusic-staging").resolve(key)
    }

    fun target(root: Path, song: Song, context: DesktopDownloadContext): Path {
        val artist = component(context.artist.ifBlank { song.artist }, "Artista desconocido")
        val directory = when (context.kind) {
            DesktopDownloadContext.Kind.ARTIST -> root.resolve("Artistas").resolve(artist)
                .resolve(component(song.albumName.orEmpty(), "Canciones"))
            DesktopDownloadContext.Kind.ALBUM -> root.resolve("Álbumes").resolve(artist)
                .resolve(component(context.title, "Álbum desconocido"))
            DesktopDownloadContext.Kind.PLAYLIST -> root.resolve("Playlists").resolve(component(context.title, "Playlist"))
        }
        // A stable identity suffix prevents two recordings with the same title overwriting one another.
        val identity = java.security.MessageDigest.getInstance("SHA-256").digest(song.videoId.toByteArray())
            .take(6).joinToString("") { "%02x".format(it) }
        val order = if (context.kind == DesktopDownloadContext.Kind.ALBUM) song.trackNumber?.let { "%02d - ".format(it) }.orEmpty() else ""
        return directory.resolve("$order${component("${song.artist} - ${song.title}", "Canción")} [$identity].mp3")
    }

    fun belongsTo(path: String, context: DesktopDownloadContext): Boolean {
        val parent = runCatching { Path.of(path).parent }.getOrNull() ?: return false
        return when (context.kind) {
            DesktopDownloadContext.Kind.PLAYLIST -> parent.fileName.toString() == component(context.title, "Playlist") && parent.parent?.fileName?.toString() == "Playlists"
            DesktopDownloadContext.Kind.ALBUM -> parent.fileName.toString() == component(context.title, "Álbum desconocido") && parent.parent?.parent?.fileName?.toString() == "Álbumes"
            DesktopDownloadContext.Kind.ARTIST -> parent.parent?.parent?.fileName?.toString() == "Artistas"
        }
    }

    private fun boundedUtf8(raw: String): String = buildString {
        var bytes = 0
        for (codePoint in raw.codePoints().toArray()) {
            val text = String(Character.toChars(codePoint))
            val size = text.toByteArray(Charsets.UTF_8).size
            if (bytes + size > 180) break
            append(text); bytes += size
        }
    }

    fun component(raw: String, fallback: String): String = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFC)
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_").trim().trim('.').let(::boundedUtf8)
        .ifBlank { fallback }.let { if (Regex("(?i)^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\\..*)?$").matches(it)) "_$it" else it }
}
