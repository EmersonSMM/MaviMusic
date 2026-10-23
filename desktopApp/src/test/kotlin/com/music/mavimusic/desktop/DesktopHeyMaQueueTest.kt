package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.QueueTier
import com.music.mavimusic.data.model.Song
import kotlin.test.*

class DesktopHeyMaQueueTest {
    private val audio = Song("audio", "Hey Ma (Spanish Version) (con Camila Cabello)", "Pitbull y J Balvin", null, durationText = "3:14")
    private val video = Song("video", "Hey Ma (Spanish Version) (feat. Camila Cabello) [Official Video]", "J Balvin y Pitbull", null, durationText = "3:44", isVideo = true, queueTier = QueueTier.AUTOPLAY)
    private val other = Song("other", "SUPER ESTRELLA", "Aitana", null, queueTier = QueueTier.AUTOPLAY)

    @Test fun `the screenshot duplicate disappears from a restored visible queue`() {
        val queue = DesktopQueue.restored(listOf(audio, video, other), 0)
        assertEquals(listOf(audio, other), queue.songs)
        assertEquals(listOf(other), queue.upcoming)
        assertEquals(other, queue.followingFor(audio.videoId, false))
        assertEquals(other.videoId, queue.jumpTo(queue.nextDistinctIndex(audio.videoId, false)!!).current?.videoId)
    }
    @Test fun `appending suggestions cannot insert the same recording again`() {
        val queue = DesktopQueue.of(audio).append(listOf(video, other))
        assertEquals(listOf(other), queue.upcoming)
    }
    @Test fun `cleanup preserves current history and explicit queue requests`() {
        val history = Song("history", "Otra canción", "Artista", null)
        val requested = video.copy(queueTier = QueueTier.USER_QUEUE)
        val queue = DesktopQueue(listOf(history, audio, video, requested, other), 1).withoutAutoplayDuplicates()
        assertEquals(1, queue.index)
        assertEquals(audio, queue.current)
        assertEquals(listOf(history, audio, requested, other), queue.songs)
    }
    @Test fun `different language editions are not removed as video duplicates`() {
        val english = video.copy(title = "Hey Ma (English Version) (feat. Camila Cabello)")
        assertEquals(listOf(english, other), DesktopQueue.of(audio).append(listOf(english, other)).upcoming)
    }
}
