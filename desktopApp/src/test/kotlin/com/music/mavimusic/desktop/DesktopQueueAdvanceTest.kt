package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.QueueTier
import com.music.mavimusic.data.model.Song
import kotlin.test.*

class DesktopQueueAdvanceTest {
    private val audio = Song("song", "Mentí", "Ráfaga", null, durationText = "3:20")
    private val video = audio.copy(videoId = "video", title = "Rafaga - Menti (Video Oficial)", isVideo = true)
    private val other = audio.copy(videoId = "other", title = "Una cerveza")
    @Test fun `the final duplicate cannot become the next track`() {
        val queue = DesktopQueue(listOf(audio, video))
        assertNull(queue.nextDistinctIndex(audio.videoId, false))
        assertNull(queue.followingFor(audio.videoId, false))
    }
    @Test fun `Next and Automix choose the same distinct song and handoff removes skipped context`() {
        val queue = DesktopQueue(listOf(audio, video, other))
        val next = queue.nextDistinctIndex(audio.videoId, false)!!
        assertEquals(other, queue.followingFor(audio.videoId, false))
        assertEquals(queue.jumpTo(next), queue.afterHandoffTo(other.videoId))
        assertEquals(listOf(audio, other), queue.afterHandoffTo(other.videoId).songs)
    }
    @Test fun `explicit user repeats and repeat all are honored`() {
        val requested = video.copy(queueTier = QueueTier.USER_QUEUE)
        assertEquals(requested, DesktopQueue(listOf(audio, requested)).followingFor(audio.videoId, false))
        assertEquals(audio, DesktopQueue(listOf(audio, video)).followingFor(audio.videoId, true))
    }
    @Test fun `recent recordings are skipped without suppressing a different artist`() {
        val cover = audio.copy(videoId = "cover", artist = "Otro artista")
        val queue = DesktopQueue(listOf(other, video, cover))
        assertEquals(cover, queue.followingFor(other.videoId, false, listOf(audio)))
    }
}
