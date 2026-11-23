package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DesktopHomeRecommendationsTest {
    @Test fun noHistoryDoesNotRequestGenericRecommendations() = runBlocking {
        val recommendations = DesktopHomeRecommendations(search = { _, _ -> error("No request expected") })
        assertTrue(recommendations.load(emptyList()).isEmpty())
    }

    @Test fun suggestionsMixAlbumsAndPlaylistsAndReuseSessionCache() = runBlocking {
        val queries = mutableListOf<Pair<String, SearchFilter>>()
        var clock = 0L
        val recommendations = DesktopHomeRecommendations(search = { artist, filter ->
            queries += artist to filter
            val type = if (filter == SearchFilter.ALBUMS) BrowseType.ALBUM else BrowseType.PLAYLIST
            Result.success(listOf(SearchResult.Browse(BrowseItem("$artist-$filter", "$artist-$filter", artist, null, type))))
        }, now = { clock })
        val artists = listOf("A", "B", "C", "D")
        val shelves = recommendations.load(artists)
        assertEquals(6, queries.size)
        assertEquals(listOf("A-PLAYLISTS", "A-ALBUMS", "B-PLAYLISTS", "B-ALBUMS", "C-PLAYLISTS", "C-ALBUMS"), shelves.first().items.map { it.browseId })
        assertEquals(3, shelves.last().items.size)
        assertEquals(shelves, recommendations.load(artists))
        assertEquals(6, queries.size)
        clock = 31 * 60_000L
        recommendations.load(artists)
        assertEquals(12, queries.size)
    }

    @Test fun recentAndFavoriteArtistsContributeToTasteRanking() {
        val recent = List(5) { Song("$it", "Song", "Recent", null) }
        val favorites = List(10) { Song("f$it", "Song", "Favorite", null) }
        val artists = DesktopHomeRecommendations.rankedArtists(recent, listOf("Frequent", "Unknown Artist"), favorites)
        assertEquals(listOf("Favorite", "Recent", "Frequent"), artists)
    }

    @Test fun hiddenItemsAreExcludedBeforeSlicingRecommendations() = runBlocking {
        val prefs = mutableMapOf<String, String>()
        val feedback = DesktopRecommendationFeedback({ prefs[it].orEmpty() }, { k, v -> prefs[k] = v })
        val recommendations = DesktopHomeRecommendations(search = { artist, filter ->
            Result.success((0..5).map { i ->
                SearchResult.Browse(BrowseItem("$filter-$i", "$artist $i", artist, null, if (filter == SearchFilter.ALBUMS) BrowseType.ALBUM else BrowseType.PLAYLIST))
            })
        }, feedback = feedback)
        val initialAlbums = recommendations.load(listOf("Artist")).last().items
        assertEquals(4, initialAlbums.size)
        feedback.dismiss(initialAlbums.first(), "Artist")
        recommendations.invalidate()
        val refreshedAlbums = recommendations.load(listOf("Artist")).last().items
        assertEquals(4, refreshedAlbums.size)
        assertFalse(refreshedAlbums.any { it.browseId == initialAlbums.first().browseId })
    }

    @Test fun recentsShelfStaysAtTopWithSuggestedShelvesImmediatelyBelow() {
        val recents = HomeShelf("Recents", listOf(ShelfItem("Track", "Artist", null, "vid1", null)))
        val suggested = HomeShelf("Música sugerida", listOf(ShelfItem("Mix", "Artist", null, null, "VL1")))
        val albumsForYou = HomeShelf("Álbumes para ti", listOf(ShelfItem("Album", "Artist", null, null, "MPRE1")))
        val radio = HomeShelf("Iniciar radio", listOf(ShelfItem("Radio de Artist", "Artist", null, "vid1", "RADIO_ARTIST_vid1")))
        val quickPicks = HomeShelf("Quick picks", listOf(ShelfItem("Pick", "Artist", null, "vid2", null)))

        // Personalized arrives after Recents & Radio -> Recents stays first, Suggested below Recents.
        var feed = mergePersonalizedHomeShelves(emptyList(), listOf(radio))
        feed = mergeHomeShelves(feed, listOf(recents), prepend = true)
        feed = mergeHomeShelves(feed, listOf(quickPicks), prepend = false)
        feed = mergePersonalizedHomeShelves(feed, listOf(suggested, albumsForYou))
        assertEquals(listOf("Recents", "Música sugerida", "Álbumes para ti", "Iniciar radio", "Quick picks"), feed.map { it.title })

        // Recents arriving last still lands at index 0 ahead of Suggested.
        val lateRecents = mergeHomeShelves(
            listOf(suggested, albumsForYou, radio, quickPicks),
            listOf(recents),
            prepend = true,
        )
        assertEquals(listOf("Recents", "Música sugerida", "Álbumes para ti", "Iniciar radio", "Quick picks"), lateRecents.map { it.title })

        // Refreshing suggestions preserves Recents at index 0.
        val refreshed = mergePersonalizedHomeShelves(feed, listOf(suggested, albumsForYou))
        assertEquals("Recents", refreshed.first().title)
        assertEquals("Música sugerida", refreshed[1].title)

        // YouTube's "Escuchar de nuevo" (Listen again) stays in the lower feed below Suggested,
        // while a prepended "Recents" replaces stale "Recently played" aliases.
        val listenAgain = HomeShelf("Escuchar de nuevo", listOf(ShelfItem("Old", "Artist", null, "vid3", null)))
        val withListenAgain = mergeHomeShelves(
            listOf(suggested, albumsForYou, listenAgain),
            listOf(HomeShelf("Recently played", listOf(ShelfItem("Stale", "Artist", null, "vid4", null)))),
            prepend = true,
        )
        val replacedRecents = mergeHomeShelves(withListenAgain, listOf(recents), prepend = true)
        assertEquals(listOf("Recents", "Música sugerida", "Álbumes para ti", "Escuchar de nuevo"), replacedRecents.map { it.title })
    }
}
