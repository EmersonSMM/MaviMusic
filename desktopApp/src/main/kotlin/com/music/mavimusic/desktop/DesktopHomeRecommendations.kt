package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.*

/** Session-local cache: no account/library data is written by recommendations. */
internal class DesktopHomeRecommendations(
    private val search: suspend (String, SearchFilter) -> Result<List<SearchResult>> = DesktopSearchClient::search,
    private val now: () -> Long = System::currentTimeMillis,
    private val feedback: DesktopRecommendationFeedback? = null,
) {
    private var cacheKey = emptyList<String>()
    private var cachedAt = 0L
    private var cached = emptyList<HomeShelf>()
    private val owners = mutableMapOf<String, String>()
    private var refreshIndex = 0

    fun artistFor(item: ShelfItem): String? = owners[item.browseId]
    fun invalidate() { cached = emptyList(); refreshIndex++ }
    private fun visible(shelves: List<HomeShelf>) = shelves.map { it.copy(items = it.items.filterNot { item -> feedback?.isHidden(item) == true }) }
    private fun rotate(items: List<ShelfItem>): List<ShelfItem> {
        if (items.isEmpty()) return items
        val offset = (refreshIndex * 2) % items.size
        return items.drop(offset) + items.take(offset)
    }

    suspend fun load(artists: List<String>): List<HomeShelf> {
        val picked = artists.take(3)
        if (picked.isEmpty()) return emptyList()
        if (picked == cacheKey && cached.isNotEmpty() && now() - cachedAt < 30 * 60_000L) return visible(cached)
        val albums = mutableListOf<ShelfItem>()
        val mixed = mutableListOf<ShelfItem>()
        val seen = mutableSetOf<String>()
        for (artist in picked) {
            val albumResults = rotate(collections(artist, SearchFilter.ALBUMS)).take(4)
            val playlists = rotate(collections(artist, SearchFilter.PLAYLISTS)).take(4)
            albums += albumResults
            for (index in 0 until minOf(2, maxOf(albumResults.size, playlists.size))) {
                for (item in listOfNotNull(playlists.getOrNull(index), albumResults.getOrNull(index))) {
                    if (seen.add(item.browseId.orEmpty())) mixed += item
                }
            }
        }
        val shelves = buildList {
            if (mixed.isNotEmpty()) add(HomeShelf(
                DesktopStrings["suggested_music", "Música sugerida"], mixed.take(12),
                subtitle = DesktopStrings["suggested_music_subtitle", "Playlists y álbumes según tus gustos"],
            ))
            if (albums.isNotEmpty()) add(HomeShelf(
                DesktopStrings["albums_for_you", "Álbumes para ti"], albums.distinctBy { it.browseId }.take(12),
                subtitle = DesktopStrings["albums_for_you_subtitle", "Basados en tus artistas más escuchados"],
            ))
        }
        cacheKey = picked
        cachedAt = now()
        cached = shelves
        return visible(shelves)
    }

    private suspend fun collections(artist: String, filter: SearchFilter): List<ShelfItem> =
        search(artist, filter).also { result ->
            (result.exceptionOrNull() as? kotlinx.coroutines.CancellationException)?.let { throw it }
        }.getOrNull().orEmpty().filterIsInstance<SearchResult.Browse>()
            .filter { it.item.browseId.isNotBlank() && it.item.type in setOf(BrowseType.ALBUM, BrowseType.PLAYLIST) }
            .map { result -> result.item.let {
                owners[it.browseId] = artist
                ShelfItem(it.title, it.subtitle.ifBlank { artist }, it.thumbnailUrl, null, it.browseId)
            } }
            .filterNot { item -> feedback?.isHidden(item) == true }

    companion object {
        fun rankedArtists(recent: List<Song>, frequent: List<String>, favorites: List<Song>, artistPenalty: (String) -> Int = { 0 }): List<String> {
            val scores = linkedMapOf<String, Pair<String, Int>>()
            fun add(raw: String, points: Int) {
                val artist = DesktopListeningStats.primaryArtist(raw).trim()
                if (artist.isBlank() || artist.lowercase() in setOf("unknown", "unknown artist", "unknown artist(s)")) return
                val key = artist.lowercase()
                scores[key] = artist to ((scores[key]?.second ?: 0) + points)
            }
            recent.take(30).forEachIndexed { index, song -> add(song.artist, if (index < 10) 3 else 1) }
            frequent.take(10).forEachIndexed { index, artist -> add(artist, 12 - index) }
            favorites.forEach { add(it.artist, 2) }
            return scores.values.sortedByDescending { it.second - artistPenalty(it.first) * 8 }.map { it.first }
        }
    }
}

internal fun isRecentsHomeShelf(shelf: HomeShelf): Boolean {
    val title = shelf.title.trim()
    return title.equals("Recents", ignoreCase = true) ||
        title.equals("Recently played", ignoreCase = true) ||
        title.equals("Recientes", ignoreCase = true)
}

internal fun pinRecentsFirst(shelves: List<HomeShelf>): List<HomeShelf> {
    if (shelves.size <= 1) return shelves
    val (recents, others) = shelves.partition(::isRecentsHomeShelf)
    if (recents.isEmpty()) return shelves
    val orderedRecents = if (recents.size <= 1) recents else {
        val (primary, aliases) = recents.partition { it.title.trim().equals("Recents", ignoreCase = true) }
        primary + aliases
    }
    return orderedRecents + others
}

internal fun mergeHomeShelves(
    existing: List<HomeShelf>,
    incoming: List<HomeShelf>,
    prepend: Boolean,
): List<HomeShelf> {
    if (!prepend) return pinRecentsFirst(existing + incoming)
    val replacing = incoming.map { it.title.lowercase() }.toSet()
    val replacesRecents = incoming.any(::isRecentsHomeShelf)
    return pinRecentsFirst(
        incoming + existing.filterNot {
            it.title.lowercase() in replacing || (replacesRecents && isRecentsHomeShelf(it))
        },
    )
}

internal fun mergePersonalizedHomeShelves(
    existing: List<HomeShelf>,
    incoming: List<HomeShelf>,
): List<HomeShelf> {
    val replacing = incoming.map { it.title.lowercase() }.toSet()
    val base = pinRecentsFirst(existing.filterNot { it.title.lowercase() in replacing })
    val recentsCount = base.takeWhile(::isRecentsHomeShelf).size
    return base.take(recentsCount) + incoming + base.drop(recentsCount)
}
