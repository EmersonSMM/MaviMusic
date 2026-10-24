package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.durationMillis
import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Multi-layer track similarity & duplicate version detector (Point A.3).
 *
 * Detects when two tracks represent the same song or a redundant upload/version
 * (e.g., "Bad Bunny - Amorfoda" vs "Bad Bunny - Amorfoda (Video Oficial)",
 * "G O D D E S S" vs "OMAR COURTZ (((ousi))) - GODDESS (Video Official) l PRIMERA MUSA",
 * "Dices" vs "Dices [Explícita]") across Automix, queue progression, radio/autoplay
 * recommendations, and manual queue additions.
 */
object DesktopTrackSimilarity {

    /** Default combined similarity threshold above which two tracks are considered duplicates. */
    const val DEFAULT_THRESHOLD = 0.82

    /** Default number of recently played tracks to compare against. */
    const val DEFAULT_HISTORY_WINDOW = 10

    @Volatile
    var avoidDuplicateVersions: Boolean = DesktopPersistence.avoidDuplicateVersions()

    @Volatile
    var allowRemixesAndLive: Boolean = DesktopPersistence.allowRemixesAndLive()

    data class SimilarityResult(
        val isDuplicate: Boolean,
        val combinedScore: Double,
        val titleSimilarity: Double,
        val artistSimilarity: Double,
        val durationBonus: Double,
        val directIdMatch: Boolean,
        val normalizedTitleA: String,
        val normalizedTitleB: String,
        val versionTagsA: Set<String>,
        val versionTagsB: Set<String>,
        val reason: String,
    )

    private val VERSION_TAG_PATTERNS = listOf(
        "remix" to Regex("""\b(?:remix|rmx|rework|vip mix|bootleg|club mix|extended mix)\b""", RegexOption.IGNORE_CASE),
        "live" to Regex("""\b(?:en vivo|ao vivo|live(?:\s+at|\s+from|\s+session|\s+version|\s+performance)?|concierto|unplugged)\b""", RegexOption.IGNORE_CASE),
        "acoustic" to Regex("""\b(?:acoustic|acustico|acustica|version acustica|stripped)\b""", RegexOption.IGNORE_CASE),
        "cover" to Regex("""\b(?:cover|tributo|tribute|karaoke|instrumental)\b""", RegexOption.IGNORE_CASE),
        "speed" to Regex("""\b(?:slowed|sped up|nightcore|daycore|reverb|chopped)\b""", RegexOption.IGNORE_CASE),
    )

    private val LANGUAGE_TAG_PATTERNS = listOf(
        "es" to Regex("""\b(?:spanish|espanol|castellano)\b""", RegexOption.IGNORE_CASE),
        "en" to Regex("""\b(?:english|ingles)\b""", RegexOption.IGNORE_CASE),
        "pt" to Regex("""\b(?:portuguese|portugues)\b""", RegexOption.IGNORE_CASE),
        "fr" to Regex("""\b(?:french|frances)\b""", RegexOption.IGNORE_CASE),
        "it" to Regex("""\b(?:italian|italiano)\b""", RegexOption.IGNORE_CASE),
        "ja" to Regex("""\b(?:japanese|japones)\b""", RegexOption.IGNORE_CASE),
        "ko" to Regex("""\b(?:korean|coreano)\b""", RegexOption.IGNORE_CASE),
    )

    private fun extractLanguageTags(rawTitle: String): Set<String> {
        val folded = foldDiacritics(rawTitle)
        return LANGUAGE_TAG_PATTERNS.mapNotNullTo(mutableSetOf()) { (lang, regex) ->
            lang.takeIf { regex.containsMatchIn(folded) }
        }
    }

    private val BRACKETED_NOISE = Regex(
        """[\(\[\{]\s*(?:""" +
            """video\s*oficial|official\s*(?:music\s*)?video|video\s*official|music\s*video|video\s*musical|videoclip|clip\s*oficial""" +
            """|audio\s*oficial|official\s*audio|audio|lyric\s*video|lyrical\s*video|lyrics?|letra(?:\s*oficial)?""" +
            """|visuali[sz]er(?:\s*oficial)?|hd|4k|hq|uhd|1080p|720p|full\s*(?:song|video|audio)""" +
            """|explicit[ao]?|clean(?:\s*version)?|version\s*explicita|remaster(?:ed|izado)?(?:\s*\d{4})?""" +
            """|album\s*version|single\s*version|original\s*version|radio\s*edit|from\s+[^)\]\}]*""" +
            """|(?:con|junto\s+a|feat\.?|ft\.?|featuring|with|prod\.?\s*by|prod\.?)\s+[^)\]\}]*""" +
            """)[^)\]\}]*[\)\]\}]""",
        RegexOption.IGNORE_CASE,
    )

    private val UNBRACKETED_NOISE = Regex(
        """\b(?:""" +
            """official\s+(?:music\s+)?video|video\s+oficial|video\s+official|official\s+audio|audio\s+oficial""" +
            """|video\s+musical|videoclip|clip\s+oficial|lyric\s+video|lyrical\s+video|official\s+lyric\s+video""" +
            """|official\s+visuali[sz]er|visuali[sz]er\s+oficial|visuali[sz]er|full\s+video|full\s+song|4k\s+video|hd\s+video""" +
            """|version\s+oficial|explicit\s+version|explicita|explicit""" +
            """)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val FEAT_TAIL = Regex(
        """\s+(?:feat\.?|ft\.?|featuring|con|junto\s+a|prod\.?\s*by)\s+.*$""",
        RegexOption.IGNORE_CASE,
    )

    private val PIPE_OR_CHANNEL_SUFFIX = Regex(
        """(?:\s*[|¦]\s*.*$|\s+//\s+.*$|\s+l\s+[A-Z0-9\s]{3,}$|\s*-\s*topic\b.*$)""",
        RegexOption.IGNORE_CASE,
    )

    private val CREDIT_CONJUNCTION = Regex("""\s+(?:y|and|&|x|\+|vs\.?|feat\.?|ft\.?|featuring|with)\s+""", RegexOption.IGNORE_CASE)
    private val CHANNEL_NOISE = Regex("""(?:\bvevo\b|\boficial\b|\bofficial\b|\btopic\b|\bmusic\b)""", RegexOption.IGNORE_CASE)
    private val PUNCTUATION = Regex("""[^\p{L}\p{N}]+""")
    private val MULTI_SPACE = Regex("""\s+""")

    /**
     * Extracts version tags (remix, live, acoustic, cover, speed) from a raw title.
     */
    fun extractVersionTags(rawTitle: String): Set<String> {
        val folded = foldDiacritics(rawTitle)
        val tags = mutableSetOf<String>()
        for ((tag, regex) in VERSION_TAG_PATTERNS) {
            if (regex.containsMatchIn(folded)) {
                tags += tag
            }
        }
        return tags
    }

    /**
     * Layer 1: Normalizes a song title by stripping diacritics, emojis, noise tags,
     * repeated artist prefixes/suffixes, and collapsing spaced single letters ("G O D D E S S" -> "goddess").
     */
    fun normalizeTitle(
        rawTitle: String,
        rawArtist: String = "",
        extraArtists: List<String> = emptyList(),
        stripVersionMarkers: Boolean = false,
    ): String {
        if (rawTitle.isBlank()) return ""

        // Strip trailing pipe / channel suffixes before folding case (so " l PRIMERA MUSA" is caught)
        var working = rawTitle
            .replace(PIPE_OR_CHANNEL_SUFFIX, " ")
            .substringBefore(" | ")
            .substringBefore(" // ")

        working = foldDiacritics(working)

        // Strip bracketed noise like (Video Oficial), (Official Video), [Explícita], (Lyrics), etc.
        working = working.replace(BRACKETED_NOISE, " ")
        working = working.replace(UNBRACKETED_NOISE, " ")
        working = working.replace(FEAT_TAIL, " ")

        if (stripVersionMarkers) {
            for ((_, regex) in VERSION_TAG_PATTERNS) {
                working = working.replace(regex, " ")
            }
            // Strip any remaining parentheticals when extracting core title
            working = working.replace(Regex("""\([^)]*\)|\[[^\]]*\]|\{[^}]*\}"""), " ")
        } else {
            // Strip nested empty or nickname parentheses like (((ousi))) while keeping valid version words
            working = working.replace(Regex("""\(+([^()]*)\)+""")) { match ->
                val inner = match.groupValues[1].trim()
                val hasVersion = VERSION_TAG_PATTERNS.any { it.second.containsMatchIn(inner) }
                if (hasVersion) " $inner " else " $inner "
            }
        }

        // Remove artist credits repeated in the title prefix or suffix (e.g. "Bad Bunny - Amorfoda" or "OMAR COURTZ (((ousi))) - GODDESS")
        val knownArtists = normalizeArtistTokens(rawArtist, extraArtists)
        working = stripArtistPrefixOrSuffix(working, knownArtists)

        // Strip remaining noise words and punctuation
        working = working
            .replace(UNBRACKETED_NOISE, " ")
            .replace(FEAT_TAIL, " ")
            .replace(PUNCTUATION, " ")
            .replace(MULTI_SPACE, " ")
            .trim()

        // Collapse spaced single letters: "g o d d e s s" -> "goddess"
        working = collapseSpacedLetters(working)

        // Second pass: if known artist name still appears verbatim at start of title with remaining words
        if (knownArtists.isNotEmpty()) {
            for (artist in knownArtists.sortedByDescending { it.length }) {
                if (artist.length >= 3 && working.startsWith("$artist ") && working.length > artist.length + 2) {
                    working = working.removePrefix("$artist ").trim()
                }
            }
        }

        return working.replace(MULTI_SPACE, " ").trim()
    }

    /**
     * Normalizes artist credits into a set of canonical lowercase tokens.
     */
    fun normalizeArtistTokens(rawArtist: String, extraArtists: List<String> = emptyList()): Set<String> {
        val combined = buildList {
            if (rawArtist.isNotBlank()) add(rawArtist)
            addAll(extraArtists.filter { it.isNotBlank() })
        }
        val out = mutableSetOf<String>()
        for (entry in combined) {
            val folded = foldDiacritics(entry)
                .replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), " ")
                .replace(CHANNEL_NOISE, " ")
                .replace(CREDIT_CONJUNCTION, ",")
            for (part in folded.split(",", "&", "·", "•", ";", "/", "|")) {
                val cleaned = collapseSpacedLetters(
                    part.replace(PUNCTUATION, " ").replace(MULTI_SPACE, " ").trim()
                )
                if (cleaned.isNotEmpty()) {
                    out += cleaned
                }
            }
        }
        return out
    }

    private fun stripArtistPrefixOrSuffix(raw: String, knownArtists: Set<String>): String {
        val separators = Regex("""\s+[-–—:]\s+|\s*[-–—:]\s+""").findAll(raw).toList()
        if (separators.isEmpty()) return raw

        fun matchesArtist(segment: String): Boolean {
            if (knownArtists.isEmpty()) return false
            val cleaned = collapseSpacedLetters(
                segment.replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), " ")
                    .replace(PUNCTUATION, " ")
                    .replace(MULTI_SPACE, " ")
                    .trim()
            )
            if (cleaned.isEmpty()) return false
            return knownArtists.any { artist ->
                cleaned == artist ||
                    cleaned.contains(artist) ||
                    artist.contains(cleaned) ||
                    jaroWinkler(cleaned, artist) >= 0.88
            }
        }

        val prefixSep = separators.lastOrNull { matchesArtist(raw.substring(0, it.range.first)) }
        if (prefixSep != null) {
            return raw.substring(prefixSep.range.last + 1)
        }
        val suffixSep = separators.firstOrNull { matchesArtist(raw.substring(it.range.last + 1)) }
        if (suffixSep != null) {
            return raw.substring(0, suffixSep.range.first)
        }
        return raw
    }

    /**
     * Collapses sequences of 3+ single-character tokens separated by spaces into one word.
     * Example: "g o d d e s s" -> "goddess", "r e m i x" -> "remix".
     */
    internal fun collapseSpacedLetters(input: String): String {
        val tokens = input.split(" ").filter { it.isNotEmpty() }
        if (tokens.size < 3) return input
        val out = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            if (tokens[i].length == 1 && tokens[i][0].isLetter()) {
                var j = i
                while (j < tokens.size && tokens[j].length == 1 && tokens[j][0].isLetter()) {
                    j++
                }
                if (j - i >= 3) {
                    out += tokens.subList(i, j).joinToString("")
                    i = j
                    continue
                }
            }
            out += tokens[i]
            i++
        }
        return out.joinToString(" ")
    }

    private fun foldDiacritics(raw: String): String =
        Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")

    /**
     * Layer 2a: Jaro-Winkler similarity in [0.0, 1.0].
     */
    fun jaroWinkler(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        val maxDist = (max(s1.length, s2.length) / 2) - 1
        val s1Matches = BooleanArray(s1.length)
        val s2Matches = BooleanArray(s2.length)
        var matches = 0

        for (i in s1.indices) {
            val start = max(0, i - maxDist)
            val end = min(i + maxDist + 1, s2.length)
            for (j in start until end) {
                if (s2Matches[j] || s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }
        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in s1.indices) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        val jaro = ((m / s1.length) + (m / s2.length) + ((m - transpositions / 2.0) / m)) / 3.0
        var prefix = 0
        val maxPrefix = min(4, min(s1.length, s2.length))
        while (prefix < maxPrefix && s1[prefix] == s2[prefix]) {
            prefix++
        }
        return (jaro + prefix * 0.1 * (1.0 - jaro)).coerceIn(0.0, 1.0)
    }

    /**
     * Layer 2b: Normalized Levenshtein similarity in [0.0, 1.0].
     */
    fun normalizedLevenshtein(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0
        val maxLen = max(s1.length, s2.length)
        val prev = IntArray(s2.length + 1) { it }
        val curr = IntArray(s2.length + 1)
        for (i in 1..s1.length) {
            curr[0] = i
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                curr[j] = min(min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost)
            }
            System.arraycopy(curr, 0, prev, 0, s2.length + 1)
        }
        val dist = prev[s2.length]
        return (1.0 - dist.toDouble() / maxLen.toDouble()).coerceIn(0.0, 1.0)
    }

    private val FEATURED_ARTIST_REGEX = Regex(
        """\b(?:con|feat\.?|ft\.?|featuring|with|junto\s+a)\s+([^()\[\]{}|/]+)""",
        RegexOption.IGNORE_CASE,
    )

    fun extractFeaturedArtistsFromTitle(rawTitle: String): Set<String> {
        val matches = FEATURED_ARTIST_REGEX.findAll(rawTitle).toList()
        if (matches.isEmpty()) return emptySet()
        val out = mutableSetOf<String>()
        for (m in matches) {
            val names = m.groupValues[1]
                .replace(Regex("""\([^)]*\)|\[[^\]]*\]"""), " ")
                .replace(PUNCTUATION, " ")
                .replace(MULTI_SPACE, " ")
                .trim()
            for (part in names.split(",", "&", "·", "•", ";", " y ", " and ")) {
                val cleaned = foldDiacritics(part).trim()
                if (cleaned.length >= 2) out += cleaned
            }
        }
        return out
    }

    /**
     * Computes artist similarity score in [0.0, 1.0].
     */
    fun artistSimilarity(a: Song, b: Song): Double {
        val featA = extractFeaturedArtistsFromTitle(a.title)
        val featB = extractFeaturedArtistsFromTitle(b.title)
        val artistsA = normalizeArtistTokens(a.artist, a.artists.map { it.name }) + featA
        val artistsB = normalizeArtistTokens(b.artist, b.artists.map { it.name }) + featB
        if (artistsA.isEmpty() || artistsB.isEmpty()) return 0.5
        if (artistsA.any { it in artistsB }) return 1.0

        // Also check if an artist from A is mentioned in B's raw title or vice versa
        val foldedTitleA = collapseSpacedLetters(foldDiacritics(a.title).replace(PUNCTUATION, " ").replace(MULTI_SPACE, " ").trim())
        val foldedTitleB = collapseSpacedLetters(foldDiacritics(b.title).replace(PUNCTUATION, " ").replace(MULTI_SPACE, " ").trim())
        if (artistsA.any { it.length >= 3 && foldedTitleB.contains(it) } ||
            artistsB.any { it.length >= 3 && foldedTitleA.contains(it) }
        ) {
            return 0.95
        }

        var best = 0.0
        for (tokenA in artistsA) {
            for (tokenB in artistsB) {
                if (tokenA.contains(tokenB) || tokenB.contains(tokenA)) {
                    best = max(best, 0.90)
                } else {
                    val jw = jaroWinkler(tokenA, tokenB)
                    if (jw >= 0.75) {
                        best = max(best, jw)
                    }
                }
            }
        }
        return best
    }

    /**
     * Evaluates multi-layer similarity between two songs [a] and [b].
     */
    fun compare(
        a: Song,
        b: Song,
        threshold: Double = DEFAULT_THRESHOLD,
        avoidDuplicates: Boolean = avoidDuplicateVersions,
        allowRemixesLive: Boolean = allowRemixesAndLive,
    ): SimilarityResult {
        if (!avoidDuplicates) {
            val idMatch = a.videoId.isNotBlank() && a.videoId == b.videoId
            return SimilarityResult(
                isDuplicate = idMatch,
                combinedScore = if (idMatch) 1.0 else 0.0,
                titleSimilarity = if (idMatch) 1.0 else 0.0,
                artistSimilarity = if (idMatch) 1.0 else 0.0,
                durationBonus = 0.0,
                directIdMatch = idMatch,
                normalizedTitleA = a.title,
                normalizedTitleB = b.title,
                versionTagsA = emptySet(),
                versionTagsB = emptySet(),
                reason = if (idMatch) "direct_video_id" else "filter_disabled",
            )
        }

        // Layer 4: Direct metadata / ID match
        if (a.videoId.isNotBlank() && a.videoId == b.videoId) {
            return SimilarityResult(
                isDuplicate = true,
                combinedScore = 1.0,
                titleSimilarity = 1.0,
                artistSimilarity = 1.0,
                durationBonus = 0.1,
                directIdMatch = true,
                normalizedTitleA = normalizeTitle(a.title, a.artist, a.artists.map { it.name }),
                normalizedTitleB = normalizeTitle(b.title, b.artist, b.artists.map { it.name }),
                versionTagsA = extractVersionTags(a.title),
                versionTagsB = extractVersionTags(b.title),
                reason = "direct_video_id",
            )
        }

        val tagsA = extractVersionTags(a.title)
        val tagsB = extractVersionTags(b.title)

        val langA = extractLanguageTags(a.title)
        val langB = extractLanguageTags(b.title)
        if (langA.isNotEmpty() && langB.isNotEmpty() && langA != langB) {
            val normA = normalizeTitle(a.title, a.artist, a.artists.map { it.name }, stripVersionMarkers = false)
            val normB = normalizeTitle(b.title, b.artist, b.artists.map { it.name }, stripVersionMarkers = false)
            return SimilarityResult(
                isDuplicate = false,
                combinedScore = 0.40,
                titleSimilarity = normalizedLevenshtein(normA, normB),
                artistSimilarity = artistSimilarity(a, b),
                durationBonus = 0.0,
                directIdMatch = false,
                normalizedTitleA = normA,
                normalizedTitleB = normB,
                versionTagsA = tagsA,
                versionTagsB = tagsB,
                reason = "distinct_language_edition",
            )
        }

        // Distinguish remixes, live, cover, acoustic when allowRemixesLive is enabled
        val distinctVersionTags = (tagsA xor tagsB)
        if (allowRemixesLive && distinctVersionTags.any { it in setOf("remix", "live", "cover", "acoustic") }) {
            val normA = normalizeTitle(a.title, a.artist, a.artists.map { it.name }, stripVersionMarkers = false)
            val normB = normalizeTitle(b.title, b.artist, b.artists.map { it.name }, stripVersionMarkers = false)
            return SimilarityResult(
                isDuplicate = false,
                combinedScore = 0.45,
                titleSimilarity = normalizedLevenshtein(normA, normB),
                artistSimilarity = artistSimilarity(a, b),
                durationBonus = 0.0,
                directIdMatch = false,
                normalizedTitleA = normA,
                normalizedTitleB = normB,
                versionTagsA = tagsA,
                versionTagsB = tagsB,
                reason = "allowed_distinct_version:${distinctVersionTags.joinToString(",")}",
            )
        }

        // Strip version markers if allowRemixesLive is false (so even remixes/live are caught as duplicates)
        // or strip speed markers ("slowed"/"sped up") which are redundant versions of the same song.
        val stripAllVersions = !allowRemixesLive || (distinctVersionTags.isNotEmpty() && distinctVersionTags.all { it == "speed" })
        val normA = normalizeTitle(
            rawTitle = a.title,
            rawArtist = a.artist,
            extraArtists = a.artists.map { it.name },
            stripVersionMarkers = stripAllVersions,
        )
        val normB = normalizeTitle(
            rawTitle = b.title,
            rawArtist = b.artist,
            extraArtists = b.artists.map { it.name },
            stripVersionMarkers = stripAllVersions,
        )

        if (normA.isEmpty() || normB.isEmpty()) {
            return SimilarityResult(
                isDuplicate = false,
                combinedScore = 0.0,
                titleSimilarity = 0.0,
                artistSimilarity = 0.0,
                durationBonus = 0.0,
                directIdMatch = false,
                normalizedTitleA = normA,
                normalizedTitleB = normB,
                versionTagsA = tagsA,
                versionTagsB = tagsB,
                reason = "empty_normalized_title",
            )
        }

        // Guard against "Parte 1" vs "Parte 2" or "Vol. 1" vs "Vol. 2"
        val numsA = Regex("\\d+").findAll(normA).map { it.value }.toList()
        val numsB = Regex("\\d+").findAll(normB).map { it.value }.toList()
        if (numsA.isNotEmpty() && numsB.isNotEmpty() && numsA != numsB) {
            return SimilarityResult(
                isDuplicate = false,
                combinedScore = 0.25,
                titleSimilarity = 0.25,
                artistSimilarity = artistSimilarity(a, b),
                durationBonus = 0.0,
                directIdMatch = false,
                normalizedTitleA = normA,
                normalizedTitleB = normB,
                versionTagsA = tagsA,
                versionTagsB = tagsB,
                reason = "numeric_part_mismatch",
            )
        }

        val jw = jaroWinkler(normA, normB)
        val lev = normalizedLevenshtein(normA, normB)
        val titleSim = if (normA == normB) 1.0 else (jw * 0.55 + lev * 0.45)
        val artistSim = artistSimilarity(a, b)

        // Layer 3: Duration proximity reinforcement (difference <= 10s gives +0.10 bonus)
        val durASec = a.durationMillis() / 1000L
        val durBSec = b.durationMillis() / 1000L
        val durDiff = if (durASec > 0L && durBSec > 0L) abs(durASec - durBSec) else -1L
        val durationBonus = when {
            durDiff in 0L..10L -> 0.10
            durDiff in 11L..25L -> 0.04
            durDiff > 120L && !isLikelyVideo(a) && !isLikelyVideo(b) -> -0.15
            else -> 0.0
        }

        // Combined weighted score
        val baseScore = if (normA == normB && artistSim >= 0.75) {
            0.92
        } else {
            (titleSim * 0.72) + (artistSim * 0.28)
        }
        val combined = (baseScore + durationBonus).coerceIn(0.0, 1.0)

        // Require meaningful title similarity AND artist compatibility so unrelated songs don't match.
        // For identical normalized titles (length >= 4):
        // If they share at least one artist credit (e.g. collaboration/featured artist overlap) or have high artist similarity,
        // and durations are close or unknown, they are treated as variants of the same recording.
        val exactTitleMatch = normA == normB && normA.length >= 4
        val featA = extractFeaturedArtistsFromTitle(a.title)
        val featB = extractFeaturedArtistsFromTitle(b.title)
        val artistsA = normalizeArtistTokens(a.artist, a.artists.map { it.name }) + featA
        val artistsB = normalizeArtistTokens(b.artist, b.artists.map { it.name }) + featB
        val sharesAnyArtist = artistsA.any { it in artistsB } ||
            artistsA.any { tokA -> artistsB.any { tokB -> tokA.contains(tokB) || tokB.contains(tokA) } }
        val isDup = (combined >= threshold && titleSim >= 0.80 && artistSim >= 0.65) ||
            (exactTitleMatch && sharesAnyArtist && (durDiff in 0L..90L || durDiff == -1L)) ||
            (exactTitleMatch && artistSim >= 0.80)

        return SimilarityResult(
            isDuplicate = isDup,
            combinedScore = combined,
            titleSimilarity = titleSim,
            artistSimilarity = artistSim,
            durationBonus = durationBonus,
            directIdMatch = false,
            normalizedTitleA = normA,
            normalizedTitleB = normB,
            versionTagsA = tagsA,
            versionTagsB = tagsB,
            reason = if (isDup) "similarity_exceeded(${"%.2f".format(Locale.US, combined)})" else "below_threshold(${"%.2f".format(Locale.US, combined)})",
        )
    }

    /**
     * Checks whether [candidate] is a duplicate of any song in [recentSongs] (comparing up to the last [maxWindow] tracks).
     */
    fun isDuplicateOfAny(
        candidate: Song,
        recentSongs: Collection<Song>,
        maxWindow: Int = DEFAULT_HISTORY_WINDOW,
        threshold: Double = DEFAULT_THRESHOLD,
    ): Boolean {
        val window = if (recentSongs.size > maxWindow) recentSongs.toList().takeLast(maxWindow) else recentSongs
        return window.any { existing ->
            compare(existing, candidate, threshold = threshold).isDuplicate
        }
    }

    /**
     * Finds the first matching song in [recentSongs] that [candidate] duplicates, along with the [SimilarityResult].
     */
    fun findDuplicateMatch(
        candidate: Song,
        recentSongs: Collection<Song>,
        maxWindow: Int = DEFAULT_HISTORY_WINDOW,
        threshold: Double = DEFAULT_THRESHOLD,
    ): Pair<Song, SimilarityResult>? {
        val window = if (recentSongs.size > maxWindow) recentSongs.toList().takeLast(maxWindow) else recentSongs
        for (existing in window) {
            val res = compare(existing, candidate, threshold = threshold)
            if (res.isDuplicate) return existing to res
        }
        return null
    }

    private fun isLikelyVideo(song: Song): Boolean =
        song.isVideo || song.isVideoOrigin ||
            Regex("""\b(?:video|oficial|official|clip|visuali[sz]er)\b""", RegexOption.IGNORE_CASE).containsMatchIn(song.title)

    private infix fun <T> Set<T>.xor(other: Set<T>): Set<T> =
        (this - other) + (other - this)
}
