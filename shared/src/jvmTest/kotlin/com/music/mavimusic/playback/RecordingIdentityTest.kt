package com.music.mavimusic.playback

import com.music.mavimusic.data.model.Song
import kotlin.test.*

class RecordingIdentityTest {
    private fun track(id: String, title: String, artist: String = "Ráfaga", duration: String = "3:20") =
        Song(id, title, artist, null, durationText = duration)

    @Test fun `video packaging and accent differences identify the same song`() {
        assertTrue(QueueBuilder.isSameRecording(track("a", "Mentí"), track("b", "RAFAGA - Menti (Video Oficial)", "Rafaga - Topic")))
        assertTrue(QueueBuilder.isSameRecording(track("a", "Mentí"), track("v", "Menti - Letra", "RafagaVEVO")))
        assertTrue(QueueBuilder.isSameRecording(track("a", "Una canción"), track("b", "Una cancion | Official Music Video")))
    }
    @Test fun `small spelling differences need artist and duration agreement`() {
        assertTrue(QueueBuilder.isSameRecording(track("a", "Dildaara"), track("b", "Dildara")))
        assertFalse(QueueBuilder.isSameRecording(track("a", "Dildaara"), track("b", "Dildara", "Otro artista")))
        assertFalse(QueueBuilder.isSameRecording(track("a", "Dildaara"), track("b", "Dildara", duration = "7:20")))
    }
    @Test fun `short prefixes and numbered songs stay distinct`() {
        assertFalse(QueueBuilder.isSameRecording(track("a", "Love Me", "Love"), track("b", "Me", "Love")))
        assertFalse(QueueBuilder.isSameTrackOrVariant(track("a", "Love"), track("b", "Love Me")))
        assertFalse(QueueBuilder.isSameRecording(track("a", "Mi canción 1"), track("b", "Mi canción 2")))
        assertFalse(QueueBuilder.isSameRecording(track("a", "Mentí"), track("b", "Mentí", "Otro artista")))
    }
    @Test fun `live and remix recordings remain distinct in suggestions`() {
        assertFalse(QueueBuilder.isSameRecording(track("a", "Mentí"), track("b", "Mentí (Live)")))
        assertFalse(QueueBuilder.isSameRecording(track("a", "Mentí"), track("b", "Mentí (Remix)")))
        assertTrue(QueueBuilder.isSameTrackOrVariant(track("a", "Mentí"), track("b", "Mentí (Remix)")))
    }
}
