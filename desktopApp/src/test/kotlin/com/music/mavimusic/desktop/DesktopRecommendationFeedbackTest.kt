package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopRecommendationFeedbackTest {
    private val item = ShelfItem("Playlist", "Artist", null, null, "VLplaylist")
    @Test fun dismissalPersistsAndDownRanksArtistWithoutChangingHistory() {
        val prefs = mutableMapOf<String, String>()
        fun store() = DesktopRecommendationFeedback({ prefs[it].orEmpty() }, { key, value -> prefs[key] = value })
        val feedback = store()
        feedback.dismiss(item, "Artist")
        feedback.dismiss(item, "Artist")
        assertTrue(store().isHidden(item))
        assertEquals(1, store().artistPenalty("ARTIST"))
        val songs = listOf(Song("a", "Song", "Artist", null), Song("b", "Song", "Other", null))
        assertEquals("Other", DesktopHomeRecommendations.rankedArtists(songs, emptyList(), emptyList(), feedback::artistPenalty).first())
        assertEquals(2, songs.size)
    }
    @Test fun hiddenCollectionsStayHiddenOnCachedResultsAndRefreshRotatesCandidates(): Unit = runBlocking {
        val prefs = mutableMapOf<String, String>()
        val feedback = DesktopRecommendationFeedback({ prefs[it].orEmpty() }, { key, value -> prefs[key] = value })
        val recommendations = DesktopHomeRecommendations(search = { artist, filter ->
            Result.success((0..5).map { i -> SearchResult.Browse(BrowseItem("$filter-$i", "$artist $i", artist, null, if (filter == SearchFilter.ALBUMS) BrowseType.ALBUM else BrowseType.PLAYLIST)) })
        }, feedback = feedback)
        val first = recommendations.load(listOf("Artist")).first().items
        assertEquals("Artist", recommendations.artistFor(first.first()))
        feedback.dismiss(first.first(), "Artist")
        assertFalse(recommendations.load(listOf("Artist")).first().items.any { it.browseId == first.first().browseId })
        recommendations.invalidate()
        val fresh = recommendations.load(listOf("Artist")).first().items
        assertNotEquals(first.map { it.browseId }, fresh.map { it.browseId })
        assertFalse(fresh.any { feedback.isHidden(it) })
    }
}
