package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException

/** Download source selection is independent of playback's lossless source race. */
internal object DesktopDownloadSource {
    const val KEY = "download_source"
    const val GOOGLE = "GOOGLE"
    const val CONFIGURED = "CONFIGURED"

    suspend fun resolve(song: Song, quality: String): Result<DesktopStream> {
        // Keep provider-owned rows on their provider. For ordinary YouTube rows, the CDN is
        // already Google's; there is no separate download server to redirect the bytes through.
        val direct = DesktopPersistence().string(KEY, GOOGLE) == GOOGLE &&
            DesktopSourceRegistry.hasYouTubeOriginal(song) && DesktopSourceRegistry.isEnabled("youtube")
        if (direct) {
            try {
                val result = DesktopStreamClient.resolve(song, DesktopMusicSources.ceiling(quality).maxKbps)
                if (result.isSuccess) return result
            } catch (cancelled: CancellationException) { throw cancelled }
            DesktopTrackLog.log("Google download resolution unavailable; trying the configured sources")
        }
        return DesktopMusicSources.resolve(song, quality, forDownload = true)
    }
}
