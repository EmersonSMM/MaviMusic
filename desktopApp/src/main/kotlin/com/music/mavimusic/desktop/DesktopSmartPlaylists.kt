package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.random.Random

/** Fields available for Smart Playlist filtering rules. */
enum class SmartRuleField(val label: String) {
    PLAY_COUNT("Reproducciones totales"),
    PLAY_COUNT_RECENT("Reproducciones (últimos 30 días)"),
    LAST_PLAYED("Última reproducción"),
    DATE_ADDED("Fecha de adición"),
    IS_LIKED("Me gusta"),
    ARTIST("Artista"),
    ALBUM("Álbum"),
    GENRE("Género"),
    YEAR("Año"),
    DURATION("Duración (segundos)"),
    BPM("BPM"),
    RATING("Calificación"),
    ORIGIN("Origen")
}

/** Operators for rule comparisons. */
enum class SmartRuleOperator(val label: String) {
    IS("Es"),
    IS_NOT("No es"),
    CONTAINS("Contiene"),
    GREATER_THAN("Mayor que"),
    LESS_THAN("Menor que"),
    BETWEEN("Entre"),
    IN_LAST_DAYS("En los últimos N días"),
    IS_EMPTY("Vacío / No disponible")
}

/** Match logic between multiple conditions. */
enum class SmartRuleMatchMode(val label: String) {
    ALL("Cumplir TODAS las reglas (Y)"),
    ANY("Cumplir CUALQUIERA de las reglas (O)")
}

/** Fields available for sorting. */
enum class SmartSortField(val label: String) {
    DATE_ADDED("Fecha de adición"),
    LAST_PLAYED("Última reproducción"),
    PLAY_COUNT("Más reproducidas"),
    TITLE("Título"),
    ARTIST("Artista"),
    DURATION("Duración"),
    BPM("BPM"),
    RANDOM("Aleatorio")
}

/** Single condition inside a rule set. */
data class SmartRuleCondition(
    val field: SmartRuleField = SmartRuleField.PLAY_COUNT,
    val operator: SmartRuleOperator = SmartRuleOperator.GREATER_THAN,
    val value: String = "0",
    val secondaryValue: String = ""
)

/** Complete specification of a Smart Playlist's dynamic criteria. */
data class SmartPlaylistRuleSet(
    val matchMode: SmartRuleMatchMode = SmartRuleMatchMode.ALL,
    val conditions: List<SmartRuleCondition> = emptyList(),
    val sortBy: SmartSortField = SmartSortField.PLAY_COUNT,
    val sortDescending: Boolean = true,
    val limit: Int = 50,
    val autoUpdate: Boolean = true
)

data class SmartPlaylistTemplate(
    val id: String,
    val name: String,
    val description: String,
    val ruleSet: SmartPlaylistRuleSet
)

/** Built-in ready-to-use smart playlist templates. */
object SmartPlaylistTemplates {

    val TEMPLATES = listOf(
        SmartPlaylistTemplate(
            id = "most_played_month",
            name = "Más escuchadas del último mes",
            description = "Tus canciones con más reproducciones en los últimos 30 días",
            ruleSet = SmartPlaylistRuleSet(
                matchMode = SmartRuleMatchMode.ALL,
                conditions = listOf(
                    SmartRuleCondition(SmartRuleField.PLAY_COUNT_RECENT, SmartRuleOperator.GREATER_THAN, "0")
                ),
                sortBy = SmartSortField.PLAY_COUNT,
                sortDescending = true,
                limit = 50
            )
        ),
        SmartPlaylistTemplate(
            id = "favorites_high_bpm",
            name = "Favoritas con BPM > 120",
            description = "Tus pistas favoritas con tempo enérgico",
            ruleSet = SmartPlaylistRuleSet(
                matchMode = SmartRuleMatchMode.ALL,
                conditions = listOf(
                    SmartRuleCondition(SmartRuleField.IS_LIKED, SmartRuleOperator.IS, "true"),
                    SmartRuleCondition(SmartRuleField.BPM, SmartRuleOperator.GREATER_THAN, "120")
                ),
                sortBy = SmartSortField.BPM,
                sortDescending = true,
                limit = 50
            )
        ),
        SmartPlaylistTemplate(
            id = "recent_discoveries_unrated",
            name = "Descubrimientos recientes sin calificar",
            description = "Pistas descubiertas en los últimos 14 días que aún no has calificado",
            ruleSet = SmartPlaylistRuleSet(
                matchMode = SmartRuleMatchMode.ALL,
                conditions = listOf(
                    SmartRuleCondition(SmartRuleField.ORIGIN, SmartRuleOperator.IS, "discovered"),
                    SmartRuleCondition(SmartRuleField.IS_LIKED, SmartRuleOperator.IS, "false"),
                    SmartRuleCondition(SmartRuleField.DATE_ADDED, SmartRuleOperator.IN_LAST_DAYS, "14")
                ),
                sortBy = SmartSortField.DATE_ADDED,
                sortDescending = true,
                limit = 50
            )
        ),
        SmartPlaylistTemplate(
            id = "forgotten_gems",
            name = "Olvidadas (no escuchadas en 6 meses)",
            description = "Canciones de tu biblioteca no reproducidas en más de 180 días",
            ruleSet = SmartPlaylistRuleSet(
                matchMode = SmartRuleMatchMode.ALL,
                conditions = listOf(
                    SmartRuleCondition(SmartRuleField.ORIGIN, SmartRuleOperator.IS, "library"),
                    SmartRuleCondition(SmartRuleField.LAST_PLAYED, SmartRuleOperator.IN_LAST_DAYS, "180")
                ),
                sortBy = SmartSortField.LAST_PLAYED,
                sortDescending = false,
                limit = 50
            )
        )
    )
}

/** Lightweight serializer for storing rule sets in preferences. */
object SmartRuleSerializer {

    fun encode(ruleSet: SmartPlaylistRuleSet): String {
        val header = "v1|${ruleSet.matchMode.name}|${ruleSet.sortBy.name}|${ruleSet.sortDescending}|${ruleSet.limit}|${ruleSet.autoUpdate}"
        val conds = ruleSet.conditions.joinToString(";") { c ->
            val v1 = c.value.replace("|", "_").replace(";", "_")
            val v2 = c.secondaryValue.replace("|", "_").replace(";", "_")
            "${c.field.name}|${c.operator.name}|$v1|$v2"
        }
        return "$header#$conds"
    }

    fun decode(text: String): SmartPlaylistRuleSet? {
        if (text.isBlank()) return null
        return runCatching {
            val parts = text.split("#")
            val headerParts = parts[0].split("|")
            if (headerParts.size < 6) return null

            val matchMode = SmartRuleMatchMode.valueOf(headerParts[1])
            val sortBy = SmartSortField.valueOf(headerParts[2])
            val sortDescending = headerParts[3].toBooleanStrictOrNull() ?: true
            val limit = headerParts[4].toIntOrNull() ?: 50
            val autoUpdate = headerParts[5].toBooleanStrictOrNull() ?: true

            val conditions = if (parts.size > 1 && parts[1].isNotBlank()) {
                parts[1].split(";").filter { it.isNotBlank() }.mapNotNull { condStr ->
                    val cParts = condStr.split("|")
                    if (cParts.size < 3) null
                    else {
                        val field = SmartRuleField.valueOf(cParts[0])
                        val op = SmartRuleOperator.valueOf(cParts[1])
                        val v1 = cParts.getOrElse(2) { "" }
                        val v2 = cParts.getOrElse(3) { "" }
                        SmartRuleCondition(field, op, v1, v2)
                    }
                }
            } else {
                emptyList()
            }

            SmartPlaylistRuleSet(
                matchMode = matchMode,
                conditions = conditions,
                sortBy = sortBy,
                sortDescending = sortDescending,
                limit = limit,
                autoUpdate = autoUpdate
            )
        }.getOrNull()
    }
}

/** Contextual metadata gathered for rule evaluation. */
data class SmartEvaluationContext(
    val likedIds: Set<String>,
    val dislikedIds: Set<String>,
    val playCountsTotal: Map<String, Int>,
    val playCountsRecent: Map<String, Int>,
    val lastPlayedTimestamps: Map<String, Long>,
    val bpmCache: Map<String, Float>,
    val librarySongIds: Set<String>,
    val currentTimestamp: Long = System.currentTimeMillis()
)

/** Evaluation engine for Smart Playlists. */
object DesktopSmartPlaylistEngine {

    /**
     * Evaluates [ruleSet] over [universe] of songs, applying condition filters,
     * similarity version deduplication, sorting, and limit.
     */
    fun evaluate(
        ruleSet: SmartPlaylistRuleSet,
        universe: List<Song>,
        context: SmartEvaluationContext
    ): List<Song> {
        if (!DesktopSmartPlaylistSettings.enabled.value) {
            return universe.take(ruleSet.limit)
        }

        val filtered = universe.filter { song ->
            matchesRuleSet(song, ruleSet, context)
        }

        // Deduplicate versions so multiple variations of the same song do not flood the list
        val deduplicated = filterVersionDuplicates(filtered)

        // Sort according to criteria
        val sorted = when (ruleSet.sortBy) {
            SmartSortField.DATE_ADDED -> deduplicated.sortedBy { it.localDateAddedSeconds ?: 0L }
            SmartSortField.LAST_PLAYED -> deduplicated.sortedBy { context.lastPlayedTimestamps[it.videoId] ?: 0L }
            SmartSortField.PLAY_COUNT -> deduplicated.sortedBy { context.playCountsTotal[it.videoId] ?: 0 }
            SmartSortField.TITLE -> deduplicated.sortedBy { it.title.lowercase(Locale.ROOT) }
            SmartSortField.ARTIST -> deduplicated.sortedBy { it.artist.lowercase(Locale.ROOT) }
            SmartSortField.DURATION -> deduplicated.sortedBy { it.durationText.durationSeconds() }
            SmartSortField.BPM -> deduplicated.sortedBy { context.bpmCache[it.videoId] ?: 0f }
            SmartSortField.RANDOM -> deduplicated.shuffled(Random(42))
        }

        val ordered = if (ruleSet.sortDescending && ruleSet.sortBy != SmartSortField.RANDOM) {
            sorted.reversed()
        } else {
            sorted
        }

        return ordered.take(ruleSet.limit)
    }

    private fun matchesRuleSet(
        song: Song,
        ruleSet: SmartPlaylistRuleSet,
        context: SmartEvaluationContext
    ): Boolean {
        if (ruleSet.conditions.isEmpty()) return true

        return when (ruleSet.matchMode) {
            SmartRuleMatchMode.ALL -> ruleSet.conditions.all { matchesCondition(song, it, context) }
            SmartRuleMatchMode.ANY -> ruleSet.conditions.any { matchesCondition(song, it, context) }
        }
    }

    private fun matchesCondition(
        song: Song,
        condition: SmartRuleCondition,
        context: SmartEvaluationContext
    ): Boolean {
        val target = condition.value.trim()

        return when (condition.field) {
            SmartRuleField.PLAY_COUNT -> {
                val count = context.playCountsTotal[song.videoId] ?: 0
                compareNumeric(count.toFloat(), condition.operator, target, condition.secondaryValue)
            }
            SmartRuleField.PLAY_COUNT_RECENT -> {
                val count = context.playCountsRecent[song.videoId] ?: 0
                compareNumeric(count.toFloat(), condition.operator, target, condition.secondaryValue)
            }
            SmartRuleField.LAST_PLAYED -> {
                val timestamp = context.lastPlayedTimestamps[song.videoId]
                if (timestamp == null || timestamp == 0L) {
                    condition.operator == SmartRuleOperator.IS_EMPTY
                } else {
                    val daysAgo = (context.currentTimestamp - timestamp) / (24L * 3600 * 1000)
                    when (condition.operator) {
                        SmartRuleOperator.IN_LAST_DAYS -> daysAgo <= (target.toLongOrNull() ?: 30L)
                        SmartRuleOperator.GREATER_THAN -> daysAgo < (target.toLongOrNull() ?: 0L)
                        SmartRuleOperator.LESS_THAN -> daysAgo > (target.toLongOrNull() ?: 0L)
                        SmartRuleOperator.IS_EMPTY -> false
                        else -> compareNumeric(daysAgo.toFloat(), condition.operator, target, condition.secondaryValue)
                    }
                }
            }
            SmartRuleField.DATE_ADDED -> {
                val addedSec = song.localDateAddedSeconds
                if (addedSec == null || addedSec == 0L) {
                    condition.operator == SmartRuleOperator.IS_EMPTY
                } else {
                    val daysAgo = (context.currentTimestamp - addedSec * 1000L) / (24L * 3600 * 1000)
                    when (condition.operator) {
                        SmartRuleOperator.IN_LAST_DAYS -> daysAgo <= (target.toLongOrNull() ?: 30L)
                        SmartRuleOperator.IS_EMPTY -> false
                        else -> compareNumeric(daysAgo.toFloat(), condition.operator, target, condition.secondaryValue)
                    }
                }
            }
            SmartRuleField.IS_LIKED -> {
                val isLiked = song.videoId in context.likedIds
                val expectLiked = target.equals("true", ignoreCase = true) || target.equals("si", ignoreCase = true) || target == "1"
                when (condition.operator) {
                    SmartRuleOperator.IS -> isLiked == expectLiked
                    SmartRuleOperator.IS_NOT -> isLiked != expectLiked
                    else -> isLiked == expectLiked
                }
            }
            SmartRuleField.ARTIST -> compareString(song.artist, condition.operator, target)
            SmartRuleField.ALBUM -> compareString(song.albumName.orEmpty(), condition.operator, target)
            SmartRuleField.GENRE -> compareString("", condition.operator, target) // Genre from tag/metadata
            SmartRuleField.YEAR -> {
                val yearMatch = Regex("""\b(19\d\d|20\d\d)\b""").find(song.albumName.orEmpty() + " " + song.title)
                val year = yearMatch?.groupValues?.get(1)?.toIntOrNull()
                if (year == null) {
                    condition.operator == SmartRuleOperator.IS_EMPTY
                } else {
                    compareNumeric(year.toFloat(), condition.operator, target, condition.secondaryValue)
                }
            }
            SmartRuleField.DURATION -> {
                val durationSec = song.durationText.durationSeconds()
                if (durationSec == null) {
                    condition.operator == SmartRuleOperator.IS_EMPTY
                } else {
                    compareNumeric(durationSec.toFloat(), condition.operator, target, condition.secondaryValue)
                }
            }
            SmartRuleField.BPM -> {
                val bpm = context.bpmCache[song.videoId]
                if (bpm == null || bpm <= 0f) {
                    condition.operator == SmartRuleOperator.IS_EMPTY
                } else {
                    compareNumeric(bpm, condition.operator, target, condition.secondaryValue)
                }
            }
            SmartRuleField.RATING -> {
                val rating = when {
                    song.videoId in context.likedIds -> 1
                    song.videoId in context.dislikedIds -> -1
                    else -> 0
                }
                compareNumeric(rating.toFloat(), condition.operator, target, condition.secondaryValue)
            }
            SmartRuleField.ORIGIN -> {
                val isLibrary = song.videoId in context.librarySongIds || song.localPath != null
                val expectLibrary = target.equals("library", ignoreCase = true) || target.equals("biblioteca", ignoreCase = true)
                when (condition.operator) {
                    SmartRuleOperator.IS -> if (expectLibrary) isLibrary else !isLibrary
                    SmartRuleOperator.IS_NOT -> if (expectLibrary) !isLibrary else isLibrary
                    else -> true
                }
            }
        }
    }

    private fun compareNumeric(
        actual: Float,
        op: SmartRuleOperator,
        targetStr: String,
        secondaryStr: String
    ): Boolean {
        val target = targetStr.toFloatOrNull() ?: 0f
        return when (op) {
            SmartRuleOperator.IS -> actual == target
            SmartRuleOperator.IS_NOT -> actual != target
            SmartRuleOperator.GREATER_THAN -> actual > target
            SmartRuleOperator.LESS_THAN -> actual < target
            SmartRuleOperator.BETWEEN -> {
                val second = secondaryStr.toFloatOrNull() ?: target
                val low = minOf(target, second)
                val high = maxOf(target, second)
                actual in low..high
            }
            SmartRuleOperator.IS_EMPTY -> false
            else -> actual == target
        }
    }

    private fun compareString(actual: String, op: SmartRuleOperator, target: String): Boolean {
        if (op == SmartRuleOperator.IS_EMPTY) return actual.isBlank()
        val a = actual.trim().lowercase(Locale.ROOT)
        val t = target.trim().lowercase(Locale.ROOT)
        return when (op) {
            SmartRuleOperator.IS -> a == t
            SmartRuleOperator.IS_NOT -> a != t
            SmartRuleOperator.CONTAINS -> a.contains(t)
            else -> a.contains(t)
        }
    }

    /**
     * Filters out variant duplicates (live, acoustic, remasters of same title/artist)
     * using the similarity detector.
     */
    fun filterVersionDuplicates(songs: List<Song>): List<Song> {
        val result = mutableListOf<Song>()
        for (candidate in songs) {
            val duplicate = result.any { existing ->
                candidate.videoId == existing.videoId ||
                DesktopTrackSimilarity.compare(candidate, existing).isDuplicate
            }
            if (!duplicate) {
                result.add(candidate)
            }
        }
        return result
    }

    /**
     * Builds evaluation context from local storage.
     */
    fun buildContext(persistence: DesktopPersistence = DesktopPersistence()): SmartEvaluationContext {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 24 * 3600 * 1000
        val history = persistence.history()

        val playCountsTotal = history.groupingBy { it.videoId }.eachCount()
        // Approximation: history is ordered from most recent to oldest
        val playCountsRecent = history.take(200).groupingBy { it.videoId }.eachCount()

        val lastPlayed = mutableMapOf<String, Long>()
        history.forEachIndexed { index, song ->
            if (song.videoId !in lastPlayed) {
                // Approximate timestamp decreasing with history index
                lastPlayed[song.videoId] = now - index * 3600_000L
            }
        }

        val downloads = persistence.downloads().map { it.videoId }.toSet()
        val playlistsSongs = persistence.playlists().flatMap { it.songs }.map { it.videoId }.toSet()
        val libraryIds = downloads + playlistsSongs

        return SmartEvaluationContext(
            likedIds = persistence.likedIds(),
            dislikedIds = persistence.dislikedIds(),
            playCountsTotal = playCountsTotal,
            playCountsRecent = playCountsRecent,
            lastPlayedTimestamps = lastPlayed,
            bpmCache = emptyMap(),
            librarySongIds = libraryIds,
            currentTimestamp = now
        )
    }
}

/** Settings and persistence for Smart Playlists. */
object DesktopSmartPlaylistSettings {

    private val persistence = DesktopPersistence()
    private const val KEY_ENABLED = "smart_playlists_enabled"

    private val _enabled = MutableStateFlow(persistence.boolean(KEY_ENABLED, true))
    val enabled: StateFlow<Boolean> = _enabled

    fun setEnabled(value: Boolean) {
        _enabled.value = value
        persistence.saveBoolean(KEY_ENABLED, value)
    }
}
