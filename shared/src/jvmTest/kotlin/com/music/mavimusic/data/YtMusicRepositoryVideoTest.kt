package com.music.mavimusic.data

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertNull

class YtMusicRepositoryVideoTest {

    @Test
    fun resolveVideo_whenNoVideoMatch_doesNotThrowNpeAndCachesSafely() = runBlocking {
        val nonExistentSong = Song(
            videoId = "test_audio_only_song_999",
            title = "A Nonexistent Song Title For Video Test XYZ 12345",
            artist = "Unknown Obscure Artist 98765",
            thumbnailUrl = null,
            isVideo = false,
        )

        // Verifies calling resolveVideo on a song with no video match returns null safely
        val result = YtMusicRepository.resolveVideo(nonExistentSong)
        assertNull(result)

        // Verifies cached video version returns null safely without NPE
        assertNull(YtMusicRepository.cachedVideoVersion(nonExistentSong.videoId))

        // Subsequent lookup should hit negative cache and return null immediately
        val secondResult = YtMusicRepository.resolveVideo(nonExistentSong)
        assertNull(secondResult)
    }

    @Test
    fun resolveVideo_whenAlreadyVideo_returnsNullImmediately() = runBlocking {
        val videoSong = Song(
            videoId = "already_video_123",
            title = "Music Video",
            artist = "Artist",
            thumbnailUrl = null,
            isVideo = true,
        )

        assertNull(YtMusicRepository.resolveVideo(videoSong))
    }
}
