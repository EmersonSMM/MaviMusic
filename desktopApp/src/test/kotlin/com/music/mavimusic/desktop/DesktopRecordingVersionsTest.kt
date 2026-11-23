package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.*
import kotlin.test.*

class DesktopRecordingVersionsTest {
    private val audio = Song("audio", "Mentí", "Ráfaga", null, durationText = "3:20", playbackSourceType = PlaybackSourceType.BROWSE, queueEntryId = "original-entry")
    private val video = audio.copy(videoId = "video", title = "Rafaga - Menti (Video Oficial)", isVideo = true, durationText = "4:10")
    private val other = audio.copy(videoId = "other", title = "Otra canción")

    @Test fun orderedCollectionsDoNotRepeatAudioAsVideoButRetainRemixes() {
        val remix = video.copy(videoId = "remix", title = "Mentí (Remix)")
        val queue = DesktopQueue(listOf(audio, video, remix, other))
        assertEquals(remix, queue.followingFor(audio.videoId, false))
        assertEquals(listOf(audio, remix, other), queue.withoutDuplicates().songs)
    }

    @Test fun manualRequestForVideoRemainsIntentional() {
        val requested = video.copy(queueTier = QueueTier.USER_QUEUE)
        val queue = DesktopQueue(listOf(audio, requested, other)).withoutDuplicates()
        assertEquals(requested, queue.followingFor(audio.videoId, false))
    }

    @Test fun twoRepresentationsResolvedToSameIdDoNotRepeatInCollection() {
        val converted = video.withRecording(audio)
        val queue = DesktopQueue(listOf(audio, converted, other))
        assertEquals(other, queue.followingFor(audio.videoId, false))
        assertEquals(listOf(audio, other), queue.withoutDuplicates().songs)
    }

    @Test fun representationSwitchPreservesQueueIdentityAndProvenance() {
        val entry = audio.copy(queueTier = QueueTier.USER_QUEUE, playbackSource = "Mi playlist", playbackSourceId = "playlist-id")
        val updated = entry.withRecording(video.copy(queueEntryId = "different-entry", playbackSource = "Otra fuente"))
        assertEquals(entry.queueEntryId, updated.queueEntryId)
        assertEquals(entry.queueTier, updated.queueTier)
        assertEquals(entry.playbackSource, updated.playbackSource)
        assertEquals(entry.playbackSourceId, updated.playbackSourceId)
        assertEquals(video.videoId, updated.videoId)
        assertTrue(updated.isVideoOrigin)
        val backToAudio = updated.withRecording(audio)
        assertEquals(entry.queueEntryId, backToAudio.queueEntryId)
        assertTrue(backToAudio.isVideoOrigin)
    }

    @Test fun musicVideoMatchingRejectsLyricsOtherArtistsAndDifferentVersions() {
        assertTrue(isMatchingMusicVideo(audio, video))
        assertFalse(isMatchingMusicVideo(audio, video.copy(title = "Mentí (Lyric Video)")))
        assertFalse(isMatchingMusicVideo(audio, video.copy(title = "Mentí (Remix)")))
        assertFalse(isMatchingMusicVideo(audio, video.copy(artist = "Otro artista", title = "Mentí (Video Oficial)")))
    }

    @Test fun collectionShuffleCanRestoreOriginalOrderAroundChosenSong() {
        val songs = listOf(audio, other, audio.copy(videoId = "third", title = "Tercera"), audio.copy(videoId = "fourth", title = "Cuarta"))
        // Real queue entries have unique keys; assign them explicitly for this scenario.
        val entries = songs.mapIndexed { i, song -> song.copy(queueEntryId = "entry-$i") }
        val result = DesktopQueue.shuffledStartingAt(entries, 2).inOrderOf(entries.map(DesktopQueue::orderKey))
        assertEquals(listOf(entries[2], entries[0], entries[1], entries[3]), result.songs)
    }
}
