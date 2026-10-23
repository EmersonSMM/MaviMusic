package com.music.mavimusic.desktop

import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** A catalogue lookup is an optional improvement, never a reason to lose playable requested audio. */
internal object DesktopDownloadSong {
    private val videoLabel = Regex("(?i)\\b(video oficial|official (music )?video|video musical|videoclip)\\b")
    suspend fun resolve(song: Song, search: suspend (Song) -> Song = { YtMusicRepository.resolveAudio(it) }): Song {
        if (!DesktopSourceRegistry.hasYouTubeOriginal(song) || (!song.isVideo && !videoLabel.containsMatchIn(song.title))) return song
        val requested = song.copy(isVideo = true, isVideoOrigin = true)
        val candidate = try {
            withTimeoutOrNull(15_000) { search(requested) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            DesktopTrackLog.log("download: catalogue search unavailable; retaining the requested video's audio")
            null
        }
        // The repository validates artist/title/version. Keep fallback visibly marked as video audio,
        // including uploads whose provider omitted the video flag but labelled the title explicitly.
        return candidate?.takeUnless { it.isVideo } ?: requested
    }
}
