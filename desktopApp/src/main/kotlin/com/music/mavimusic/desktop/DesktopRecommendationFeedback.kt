package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.ShelfItem

/** Small local preference store, independent of the listening history. */
internal class DesktopRecommendationFeedback(
    private val read: (String) -> String,
    private val write: (String, String) -> Unit,
) {
    private val hidden = read("home_hidden_collections").lineSequence().filter(String::isNotBlank).toMutableSet()
    private val penalties = read("home_artist_penalties").lineSequence().mapNotNull {
        val parts = it.split('\t', limit = 2)
        parts.getOrNull(1)?.toIntOrNull()?.let { score -> parts[0] to score.coerceIn(0, 20) }
    }.toMap().toMutableMap()

    fun isHidden(item: ShelfItem): Boolean = item.browseId in hidden
    fun artistPenalty(artist: String): Int = penalties[artist.lowercase()] ?: 0

    fun dismiss(item: ShelfItem, artist: String?) {
        val id = item.browseId ?: return
        if (!hidden.add(id)) return
        while (hidden.size > 200) hidden.remove(hidden.first())
        artist?.trim()?.lowercase()?.replace('\n', ' ')?.replace('\t', ' ')?.takeIf(String::isNotBlank)?.let {
            penalties[it] = ((penalties[it] ?: 0) + 1).coerceAtMost(20)
        }
        while (penalties.size > 200) penalties.remove(penalties.keys.first())
        write("home_hidden_collections", hidden.joinToString("\n"))
        write("home_artist_penalties", penalties.entries.toList().takeLast(200).joinToString("\n") { "${it.key}\t${it.value}" })
    }
}
