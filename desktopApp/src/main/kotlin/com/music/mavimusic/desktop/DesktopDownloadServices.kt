package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song

/** Network boundaries; the file transfer, converter and verifier remain the production pipeline in integration tests. */
internal class DesktopDownloadServices(
    val song: suspend (Song) -> Song = { DesktopDownloadSong.resolve(it) },
    val metadata: suspend (Song) -> Song = DesktopDownloadMetadata::enrich,
    val stream: suspend (Song, String) -> Result<DesktopStream> = DesktopDownloadSource::resolve,
)
