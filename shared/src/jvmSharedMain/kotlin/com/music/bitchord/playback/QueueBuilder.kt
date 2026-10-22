package com.music.bitchord.playback

import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.durationMillis
import java.util.Locale

/**
 * Builds the station that plays on after a one-off song.
 *
 * Shared: Android and the desktop assemble the same station from the same
 * watch queue, so the de-duplication and the per-artist caps are one
 * implementation rather than two that drift.
 *
 * Two problems this exists to solve:
 *
 *  - YouTube's watch queues routinely carry the same recording twice — the
 *    official audio and the music video are separate videoIds with near
 *    identical titles — so de-duping on id alone lets a track play, then play
 *    again as its video. Songs are matched on a normalised title plus lead
 *    artist instead.
 *
 *  - A radio mix leans hard on the seed's own artist. Capping how many tracks
 *    any one artist contributes keeps a station from turning into a single
 *    artist's discography.
 */
object QueueBuilder {

    /** No more than this many tracks by one artist in a single batch. */
    const val PER_ARTIST_LIMIT = 2

    /** The artist the station was seeded on gets more room, but not the run of it. */
    const val SEED_ARTIST_LIMIT = 4

    /**
     * The subset of [candidates] worth appending after [existing]: nothing
     * already queued, nothing repeated, at most [limit] tracks.
     */
    fun extend(existing: List<Song>, candidates: List<Song>, limit: Int): List<Song> {
        val taken = existing.toMutableList()
        val perArtist = mutableMapOf<String, Int>()
        val seedArtists = existing.lastOrNull()?.artist?.let(::artistSet).orEmpty()
        val out = mutableListOf<Song>()

        // A mix pairs all but every track with its own music-video upload —
        // the same recording under a different id, titled far enough apart
        // ("Dildaara (Stand By Me)" against "Lyrical Video: Dildara Song")
        // that no title match will catch it. The catalogue cut is the one a
        // music app wants; videos only stand in when there is nothing else.
        val songsOnly = candidates.filterNot { it.isVideo }
        val ordered = songsOnly.ifEmpty { candidates }

        for (candidate in ordered) {
            if (out.size >= limit) break
            if (taken.any { isSameRecording(it, candidate) }) continue

            val artists = artistSet(candidate.artist)
            val key = artists.minOrNull()
            if (key != null) {
                val cap = if (artists.any { it in seedArtists }) {
                    SEED_ARTIST_LIMIT
                } else {
                    PER_ARTIST_LIMIT
                }
                val played = perArtist[key] ?: 0
                if (played >= cap) continue
                perArtist[key] = played + 1
            }

            taken += candidate
            out += candidate
        }
        return out
    }

    /**
     * Whether two entries are the same recording. Ids differ between the audio
     * and video cuts of a track, which is exactly the case id equality misses.
     */
    fun isSameRecording(a: Song, b: Song): Boolean {
        if (a.videoId == b.videoId) return true
        val titleA = normalisedTitle(a.title)
        val titleB = normalisedTitle(b.title)
        if (titleA.isEmpty() || titleB.isEmpty()) return false

        val left = artistSet(a.artist)
        val right = artistSet(b.artist)
        val artistMatches = left.isEmpty() || right.isEmpty() || left.any { it in right }

        // Exact normalized title match with matching artist
        if (titleA == titleB && artistMatches) {
            val durA = a.durationMillis() / 1000L
            val durB = b.durationMillis() / 1000L
            if (durA > 0L && durB > 0L && kotlin.math.abs(durA - durB) > 40L) {
                // Large duration difference (e.g. extended mix or different version)
                return false
            }
            return true
        }

        // Substring / prefix match (e.g. one title includes extra subtitle or feature) with matching artist and duration
        if (artistMatches && (titleA.startsWith(titleB) || titleB.startsWith(titleA))) {
            val durA = a.durationMillis() / 1000L
            val durB = b.durationMillis() / 1000L
            if (durA > 0L && durB > 0L && kotlin.math.abs(durA - durB) <= 15L) {
                return true
            }
        }
        return false
    }

    /**
     * Whether two songs are variants of the same base track (e.g. original vs remix, acoustic, ft artist).
     * Used to prevent consecutive playback of the same song in queue and autoplay.
     */
    fun isSameTrackOrVariant(a: Song, b: Song): Boolean {
        if (isSameRecording(a, b)) return true
        val coreA = coreTrackTitle(a.title)
        val coreB = coreTrackTitle(b.title)
        if (coreA.isNotEmpty() && coreB.isNotEmpty()) {
            val left = artistSet(a.artist)
            val right = artistSet(b.artist)
            val artistMatches = left.isEmpty() || right.isEmpty() || left.any { it in right }
            if (artistMatches && (coreA == coreB || coreA.startsWith(coreB) || coreB.startsWith(coreA))) {
                return true
            }
        }
        return false
    }

    /**
     * `Kesariya (From "Brahmastra") | Official Video` and `Kesariya` are one
     * recording as far as a queue is concerned. Remix and cover markers are
     * deliberately left in for this function — those really are different tracks.
     */
    fun normalisedTitle(raw: String): String = raw.lowercase(Locale.ROOT)
        .substringBefore(" | ")
        .substringBefore(" // ")
        .replace(NOISE, " ")
        .replace(PUNCTUATION, " ")
        .replace(SPACES, " ")
        .trim()

    /**
     * Aggressively extracts the core track title by removing all parentheticals,
     * featuring clauses, remixes, versions, and subtitles for consecutive track duplicate prevention.
     */
    fun coreTrackTitle(raw: String): String = raw.lowercase(Locale.ROOT)
        .substringBefore(" | ")
        .substringBefore(" // ")
        .substringBefore(" - ")
        .replace(Regex("""\([^)]*\)|\[[^]]*]"""), " ")
        .replace(Regex("""\b(?:feat\.?|ft\.?|featuring|with|remix|version|deluxe|radio edit|edit|acoustic|en vivo|live)\b.*$""", RegexOption.IGNORE_CASE), " ")
        .replace(NOISE, " ")
        .replace(PUNCTUATION, " ")
        .replace(SPACES, " ")
        .trim()

    /** The cast behind a credit, split out so billing order stops mattering. */
    fun artistSet(raw: String): Set<String> = raw.lowercase(Locale.ROOT)
        .replace(TOPIC, " ")
        .split(",", "&", "·", "•", ";", " feat.", " feat", " ft.", " ft ", " x ", " with ")
        .map { it.replace(PUNCTUATION, " ").replace(SPACES, " ").trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    private val NOISE = Regex(
        """\((?:official|video oficial|audio oficial|letra|lyric|lyrics|lyrical|audio|video|visuali[sz]er|full song|hd|4k|clip oficial|en vivo|live|feat\.?|ft\.?|featuring|with)[^)]*\)""" +
            """|\(from[^)]*\)""" +
            """|\[(?:official|video oficial|audio oficial|letra|lyric|lyrics|audio|video|visuali[sz]er|hd|4k|clip oficial|feat\.?|ft\.?|featuring|with)[^]]*]""" +
            """|\b(?:official (?:video|audio|music video)|video oficial|audio oficial|video musical|lyrical video|full video|4k video|clip oficial)\b""" +
            """|\s+(?:feat\.?|ft\.?|featuring|with)\s+.*$""",
        RegexOption.IGNORE_CASE,
    )
    private val TOPIC = Regex("""\s*-\s*topic\b""", RegexOption.IGNORE_CASE)
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")
    private val SPACES = Regex("""\s+""")
}
