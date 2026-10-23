package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.withTimeoutOrNull

internal object DesktopDownloadMetadata {
    suspend fun enrich(song: Song): Song {
        if ((!song.albumName.isNullOrBlank() && !song.thumbnailUrl.isNullOrBlank()) || !DesktopSourceRegistry.hasYouTubeOriginal(song)) return song
        val details = withTimeoutOrNull(8_000) { DesktopSearchClient.trackLinks(song.videoId).getOrNull() }
        return merge(song, details)
    }

    internal fun merge(song: Song, details: Song?): Song {
        if (details == null || details.videoId != song.videoId) return song
        return song.copy(
            title = song.title.ifBlank { details.title },
            artist = song.artist.ifBlank { details.artist },
            albumName = song.albumName?.takeIf(String::isNotBlank) ?: details.albumName,
            albumId = song.albumId ?: details.albumId,
            thumbnailUrl = song.thumbnailUrl ?: details.thumbnailUrl,
            trackNumber = song.trackNumber ?: details.trackNumber,
        )
    }

    fun collectionSongs(collection: DesktopCollection): List<Song> =
        if (collection.type != BrowseType.ALBUM) collection.songs else collection.songs.mapIndexed { index, song ->
            song.copy(albumName = song.albumName?.takeIf(String::isNotBlank) ?: collection.title,
                albumId = song.albumId ?: collection.browseId,
                thumbnailUrl = collection.thumbnailUrl ?: song.thumbnailUrl,
                trackNumber = song.trackNumber ?: (index + 1))
        }
}
