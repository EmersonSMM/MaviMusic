package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopTrackSimilarityTest {

    @Test
    fun amorfodaAndAmorfodaVideoOficialAreDetectedAsSameSong() {
        val studio = Song(
            videoId = "id_studio_1",
            title = "Bad Bunny - Amorfoda",
            artist = "Bad Bunny",
            thumbnailUrl = null,
            durationText = "2:33",
        )
        val officialVideo = Song(
            videoId = "id_video_2",
            title = "Bad Bunny - Amorfoda (Video Oficial)",
            artist = "Bad Bunny",
            thumbnailUrl = null,
            durationText = "2:37",
        )

        val result = DesktopTrackSimilarity.compare(studio, officialVideo)
        assertTrue(result.isDuplicate, "Expected Amorfoda vs Amorfoda (Video Oficial) to match: $result")
        assertEquals("amorfoda", result.normalizedTitleA)
        assertEquals("amorfoda", result.normalizedTitleB)
        assertTrue(result.combinedScore >= DesktopTrackSimilarity.DEFAULT_THRESHOLD)
    }

    @Test
    fun spacedLettersAndChannelNoiseAreNormalizedAndDetected() {
        val catalogue = Song(
            videoId = "goddess_audio",
            title = "G O D D E S S",
            artist = "Omar Courtz",
            thumbnailUrl = null,
            durationText = "3:18",
        )
        val videoUpload = Song(
            videoId = "goddess_video",
            title = "OMAR COURTZ (((ousi))) - GODDESS\uD83E\uDDDE\u200D♀\uFE0F(Video Official) l PRIMERA MUSA",
            artist = "Omar Courtz",
            thumbnailUrl = null,
            durationText = "3:22",
        )

        val result = DesktopTrackSimilarity.compare(catalogue, videoUpload)
        assertTrue(result.isDuplicate, "Expected G O D D E S S vs GODDESS Video Official to match: $result")
        assertEquals("goddess", result.normalizedTitleA)
        assertEquals("goddess", result.normalizedTitleB)
    }

    @Test
    fun explicitAndLyricsAndVisualizerVariantsAreDetectedAsDuplicates() {
        val clean = Song("dices_1", "Dices", "De La Rose", null, durationText = "3:14")
        val explicit = Song("dices_2", "Dices [Explícita]", "De La Rose", null, durationText = "3:14")
        val lyrics = Song("dices_3", "De La Rose - Dices (Lyrics / Letra) [HD]", "De La Rose", null, durationText = "3:16")
        val visualizer = Song("dices_4", "Dices (Official Visualizer) (4K)", "De La Rose", null, durationText = "3:15")

        assertTrue(DesktopTrackSimilarity.compare(clean, explicit).isDuplicate)
        assertTrue(DesktopTrackSimilarity.compare(clean, lyrics).isDuplicate)
        assertTrue(DesktopTrackSimilarity.compare(clean, visualizer).isDuplicate)
    }

    @Test
    fun distinctSongsAndNumericPartsDoNotMatch() {
        val amorfoda = Song("bb_1", "Amorfoda", "Bad Bunny", null, durationText = "2:33")
        val titi = Song("bb_2", "Tití Me Preguntó", "Bad Bunny", null, durationText = "4:03")
        val parte1 = Song("p_1", "Historia Parte 1", "Artista X", null, durationText = "3:10")
        val parte2 = Song("p_2", "Historia Parte 2", "Artista X", null, durationText = "3:12")
        val otherArtistSameTitle = Song("other_1", "Amorfoda", "Totally Different Orchestra", null, durationText = "6:45")

        assertFalse(DesktopTrackSimilarity.compare(amorfoda, titi).isDuplicate)
        assertFalse(DesktopTrackSimilarity.compare(parte1, parte2).isDuplicate)
        assertFalse(DesktopTrackSimilarity.compare(amorfoda, otherArtistSameTitle).isDuplicate)
    }

    @Test
    fun remixesAndLiveVersionsRespectAllowRemixesAndLiveSetting() {
        val original = Song("orig_1", "Amorfoda", "Bad Bunny", null, durationText = "2:33")
        val remix = Song("rmx_1", "Amorfoda (Remix)", "Bad Bunny", null, durationText = "3:45")
        val live = Song("live_1", "Amorfoda (En Vivo)", "Bad Bunny", null, durationText = "2:50")

        // When allowRemixesLive = true, Remix and En Vivo are treated as genuinely distinct versions
        assertFalse(
            DesktopTrackSimilarity.compare(original, remix, avoidDuplicates = true, allowRemixesLive = true).isDuplicate
        )
        assertFalse(
            DesktopTrackSimilarity.compare(original, live, avoidDuplicates = true, allowRemixesLive = true).isDuplicate
        )

        // When allowRemixesLive = false, even Remix and En Vivo are filtered out as variants of the same song
        assertTrue(
            DesktopTrackSimilarity.compare(original, remix, avoidDuplicates = true, allowRemixesLive = false).isDuplicate
        )
        assertTrue(
            DesktopTrackSimilarity.compare(original, live, avoidDuplicates = true, allowRemixesLive = false).isDuplicate
        )
    }

    @Test
    fun bbyWowAndBbyWowLiveFromNewYorkAreDetectedAsSameTrackVariantAndOmittedFromQueue() {
        val studio = Song("bby_1", "BbY WOW", "KAROL G, Judeline, rusowsky", null, durationText = "3:30")
        val live = Song("bby_2", "BbY WOW (Live From New York)", "KAROL G, Judeline y rusowsky", null, durationText = "3:48", queueTier = com.music.mavimusic.data.model.QueueTier.AUTOPLAY)
        val other = Song("volvi", "Volví", "Aventura y Bad Bunny", null, durationText = "3:51", queueTier = com.music.mavimusic.data.model.QueueTier.AUTOPLAY)

        assertTrue(com.music.mavimusic.playback.QueueBuilder.isSameTrackOrVariant(studio, live))
        assertTrue(DesktopTrackSimilarity.compare(studio, live, avoidDuplicates = true, allowRemixesLive = false).isDuplicate)

        // When extending the queue with Autoplay suggestions, the live duplicate is filtered out
        val queue = DesktopQueue.of(studio).append(listOf(live, other))
        assertEquals(listOf(other), queue.upcoming)
    }

    @Test
    fun queueSkipsDuplicateCandidatesAndSelectsNextValidTrackAcrossLast10History() {
        val current = Song("id_1", "Bad Bunny - Amorfoda", "Bad Bunny", null, durationText = "2:33")
        val dupVideo = Song("id_2", "Bad Bunny - Amorfoda (Video Oficial)", "Bad Bunny", null, durationText = "2:37")
        val dupLyrics = Song("id_3", "Amorfoda (Letra / Lyrics)", "Bad Bunny", null, durationText = "2:34")
        val recentPlayed8TracksAgo = Song("id_hist_8", "Monaco", "Bad Bunny", null, durationText = "4:27")
        val dupOfRecentHistory = Song("id_4", "Bad Bunny - MONACO (Official Video)", "Bad Bunny", null, durationText = "4:29")
        val validNext = Song("id_5", "Callaíta", "Bad Bunny", null, durationText = "4:10")

        val recentHistory = (1..9).map { idx ->
            if (idx == 8) recentPlayed8TracksAgo
            else Song("hist_$idx", "Cancion Historial $idx", "Artista $idx", null, durationText = "3:00")
        }

        val queue = DesktopQueue(
            songs = listOf(current, dupVideo, dupLyrics, dupOfRecentHistory, validNext),
            index = 0,
        )

        val nextIdx = queue.nextDistinctIndex(current.videoId, repeatAll = false, recent = recentHistory)
        assertNotNull(nextIdx)
        assertEquals(4, nextIdx, "Expected queue to skip all 3 duplicate candidates and pick 'Callaíta' at index 4")
        assertEquals("id_5", queue.songs[nextIdx].videoId)
    }

    @Test
    fun maduraWithFeaturedArtistAcrossDifferentMainArtistsIsDuplicate() {
        val cosculluela = Song("m1", "Madura (con Bad Bunny)", "Cosculluela", null, durationText = "4:10")
        val alexisFido = Song("m2", "Madura (con Bad Bunny)", "Alexis and Fido", null, durationText = "4:04")

        assertTrue(
            DesktopTrackSimilarity.compare(cosculluela, alexisFido).isDuplicate,
            "Expected Madura (Cosculluela) vs Madura (Alexis and Fido) to be detected as duplicates via featured Bad Bunny",
        )
        assertTrue(
            com.music.mavimusic.playback.QueueBuilder.isSameTrackOrVariant(cosculluela, alexisFido),
            "Expected QueueBuilder to detect them as variants",
        )

        // Verifies queue deduplication when starting playback from Recents / Playlists
        val other = Song("callaita", "Callaíta", "Bad Bunny", null, durationText = "4:10")
        val queue = DesktopQueue.startingAt(listOf(cosculluela, alexisFido, other), startIndex = 0).withoutDuplicates()
        assertEquals(listOf("m1", "callaita"), queue.songs.map { it.videoId })
    }
}
