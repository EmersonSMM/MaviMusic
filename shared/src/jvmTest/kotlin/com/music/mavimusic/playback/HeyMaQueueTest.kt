package com.music.mavimusic.playback

import com.music.mavimusic.data.model.Song
import kotlin.test.*

class HeyMaQueueTest {
    private val audio = Song("audio", "Hey Ma (Spanish Version) (con Camila Cabello)", "Pitbull y J Balvin", null, durationText = "3:14")
    private val video = Song("video", "Hey Ma (Spanish Version) (feat. Camila Cabello) [Official Video]", "J Balvin y Pitbull", null, durationText = "3:44", isVideo = true)
    @Test fun `reordered Spanish artist credits and translated featuring identify the same recording`() {
        assertTrue(QueueBuilder.isSameRecording(audio, video))
        assertTrue(QueueBuilder.isSameTrackOrVariant(audio, video))
    }
    @Test fun `autoplay never offers Hey Ma again under the music video ID`() {
        val other = Song("other", "SUPER ESTRELLA", "Aitana", null)
        assertEquals(listOf(other), QueueBuilder.extend(listOf(audio), listOf(video.copy(isVideo = false), other), 10))
    }
    @Test fun `video intros do not turn the same song into a different recording`() {
        assertTrue(QueueBuilder.isSameRecording(audio, video.copy(durationText = "4:10")))
    }
    @Test fun `English and Spanish releases and remixes remain different recordings`() {
        assertFalse(QueueBuilder.isSameRecording(audio, video.copy(title = "Hey Ma (English Version) (feat. Camila Cabello)")))
        assertFalse(QueueBuilder.isSameRecording(audio, video.copy(title = "Hey Ma (Spanish Version) (Remix) (feat. Camila Cabello)")))
    }
}
