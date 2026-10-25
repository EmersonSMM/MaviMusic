package com.music.mavimusic.desktop

import com.music.mavimusic.data.lyrics.LyricsSource
import com.music.mavimusic.data.model.Song
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID
import java.util.prefs.Preferences

/** Small OS-backed store for desktop state; no Android Context is required. */
class DesktopPersistence {
    internal val preferences = DesktopPreferenceMigration.open()
    private val sourceJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun likedIds(): Set<String> = readLines(KEY_LIKED_IDS).toSet()

    fun saveLikedIds(ids: Set<String>) = writeLines(KEY_LIKED_IDS, ids.toList())

    /** Thumbed-down tracks. */
    fun dislikedIds(): Set<String> = readLines(KEY_DISLIKED_IDS).toSet()

    fun saveDislikedIds(ids: Set<String>) = writeLines(KEY_DISLIKED_IDS, ids.toList())

    /** Every lyric database, in the order they are asked. */
    fun lyricsSourceOrder(): List<String> =
        // The phone's rule for a saved order meeting a newer build: hidden sources drop out, a new
        // one slots in beside its declared neighbour.
        LyricsSource.ordered(readNames(DesktopLyricsClient.KEY_LYRICS_ORDER).mapNotNull(::lyricsSourceNamed))
            .map { it.label }

    fun saveLyricsSourceOrder(names: List<String>) =
        saveString(DesktopLyricsClient.KEY_LYRICS_ORDER, names.joinToString(","))

    /** The ticked sources. */
    fun lyricsEnabledSources(): Set<String> {
        val known = DesktopLyricsClient.sources.map { it.name }
        val stored = string(DesktopLyricsClient.KEY_LYRICS_SOURCES, UNSET)
        if (stored == UNSET) return known.toSet()
        val ticked = stored.split(',').map(String::trim).filter(String::isNotBlank).toSet()
        val offered = readNames(DesktopLyricsClient.KEY_LYRICS_SOURCES_SEEN).toSet()
        return ticked + known.filterNot { it in offered }
    }

    fun saveLyricsEnabledSources(names: Set<String>) {
        saveString(DesktopLyricsClient.KEY_LYRICS_SOURCES, names.joinToString(","))
        // Whatever was on screen when this was saved has now been offered, so a source left
        // unticked here is a decision rather than an omission.
        saveString(
            DesktopLyricsClient.KEY_LYRICS_SOURCES_SEEN,
            DesktopLyricsClient.sources.joinToString(",") { it.name },
        )
    }

    private fun readNames(key: String): List<String> =
        string(key, "").split(',').map(String::trim).filter(String::isNotBlank)

    /**
     * Distinguishes "never chosen" from "chosen to be empty", which a plain blank string cannot.
     */
    private val UNSET = "\u0000unset"

    fun history(): List<Song> = readSongs(KEY_HISTORY)

    fun saveHistory(songs: List<Song>) = writeSongs(KEY_HISTORY, songs.take(MAX_HISTORY))

    fun queue(): List<Song> = readSongs(KEY_QUEUE)

    fun saveQueue(songs: List<Song>) = writeSongs(KEY_QUEUE, songs.take(MAX_QUEUE))

    /** Tracks pinned to YouTube's own upload — see [DesktopOriginalVersion]. */
    internal fun originalVersionIds(): Set<String> = readLines(KEY_ORIGINAL_VERSIONS).toSet()

    internal fun saveOriginalVersionIds(ids: Set<String>) = writeLines(KEY_ORIGINAL_VERSIONS, ids.toList())

    fun downloads(): List<Song> = readSongs(KEY_DOWNLOADS)

    fun saveDownloads(songs: List<Song>) = writeSongs(KEY_DOWNLOADS, songs)

    fun playlists(): List<DesktopPlaylist> = readLines(KEY_PLAYLISTS).mapNotNull(::decodePlaylist)

    fun savePlaylists(playlists: List<DesktopPlaylist>) = writeLines(KEY_PLAYLISTS, playlists.map(DesktopPlaylist::toPreferenceLine))

    fun boolean(key: String, default: Boolean): Boolean = preferences.getBoolean(key, default)

    fun saveBoolean(key: String, value: Boolean) = preferences.putBoolean(key, value)

    fun int(key: String, default: Int): Int = preferences.getInt(key, default)

    fun saveInt(key: String, value: Int) = preferences.putInt(key, value)

    fun float(key: String, default: Float): Float = preferences.getFloat(key, default)

    fun saveFloat(key: String, value: Float) = preferences.putFloat(key, value)

    fun sourceEnabled(sourceId: String): Boolean = boolean("source_${sourceId}_enabled", true)

    fun saveSourceEnabled(sourceId: String, enabled: Boolean) =
        saveBoolean("source_${sourceId}_enabled", enabled)

    /** The source list is the desktop equivalent of Android's encrypted SourceRegistry state. */
    internal fun sourceConfigs(): List<DesktopSourceConfig> {
        val protected = DesktopPreferenceChunks.read(preferences, KEY_SOURCE_CONFIGS_PROTECTED)
        val legacy = DesktopPreferenceChunks.read(preferences, KEY_SOURCE_CONFIGS)
        val raw = if (protected != null) {
            runCatching { Base64.getDecoder().decode(protected) }.getOrNull()
                ?.let(DesktopWindowsCrypto::unprotect)
                ?.toString(StandardCharsets.UTF_8)
        } else {
            legacy
        }
        val stored = raw?.let {
            runCatching { sourceJson.decodeFromString<List<DesktopSourceConfig>>(it) }.getOrNull()
        }
        // Something is stored and could not be read.
        if ((raw != null || protected != null) && stored == null) {
            DesktopTrackLog.log("stored sources could not be read; leaving them untouched")
            return listOf(
                DesktopSourceConfig("jiosaavn", DesktopSourceKind.JIOSAAVN, enabled = sourceEnabled("jiosaavn")),
                DesktopSourceConfig("youtube", DesktopSourceKind.YOUTUBE),
            )
        }
        // A build-seeded module index is no longer carried, and the one this app used to ship has
        // been withdrawn — it answers HTTP 404 now.
        val initial = (stored ?: migrateLegacySources())
            .filterNot { it.kind == DesktopSourceKind.MODULE }
        val withBuiltIns = initial.toMutableList().apply {
            if (none { it.kind == DesktopSourceKind.JIOSAAVN }) {
                add(
                    DesktopSourceConfig(
                        "jiosaavn",
                        DesktopSourceKind.JIOSAAVN,
                        enabled = sourceEnabled("jiosaavn"),
                    ),
                )
            }
            if (none { it.kind == DesktopSourceKind.YOUTUBE }) {
                add(DesktopSourceConfig("youtube", DesktopSourceKind.YOUTUBE))
            }
        }

        // YouTube remains the non-removable catalogue fallback, matching the Android source
        // registry.
        val normalized = withBuiltIns.map {
            if (it.kind == DesktopSourceKind.YOUTUBE && !it.enabled) it.copy(enabled = true) else it
        }.inSourceOrder()
        if (stored == null || normalized != stored || (DesktopPlatform.isWindows && protected == null)) {
            saveSourceConfigs(normalized)
            if (DesktopPlatform.isWindows && stored == null) {
                DesktopPreferenceChunks.remove(preferences, KEY_MODULE_INDEX_URL)
            }
        }
        return normalized
    }

    internal fun saveSourceConfigs(configs: List<DesktopSourceConfig>) {
        val normalized = configs.map { config ->
            if (config.kind == DesktopSourceKind.ADDON) {
                config.copy(baseUrl = DesktopAddonClient.normalizeBase(config.baseUrl))
            } else {
                config
            }
        }
        val raw = sourceJson.encodeToString(normalized)
        if (DesktopPlatform.isWindows) {
            val sealed = DesktopWindowsCrypto.protect(raw.toByteArray(StandardCharsets.UTF_8))
                ?: error("Windows could not protect the configured source credentials")
            DesktopPreferenceChunks.write(
                preferences,
                KEY_SOURCE_CONFIGS_PROTECTED,
                Base64.getEncoder().encodeToString(sealed),
            )
            DesktopPreferenceChunks.remove(preferences, KEY_SOURCE_CONFIGS)
        } else {
            DesktopPreferenceChunks.write(preferences, KEY_SOURCE_CONFIGS, raw)
        }
    }

    /** The stream ceiling in force. */
    internal fun audioQuality(): DesktopAudioQuality =
        DesktopAudioQuality.stored(
            preferences.get(KEY_AUDIO_QUALITY, null),
            hasFourRungs = preferences.getBoolean(KEY_AUDIO_QUALITY_RUNGS, false),
        )

    internal fun saveAudioQuality(value: DesktopAudioQuality) {
        preferences.put(KEY_AUDIO_QUALITY, value.name)
        preferences.putBoolean(KEY_AUDIO_QUALITY_RUNGS, true)
        runCatching { preferences.flush() }
    }

    fun string(key: String, default: String = ""): String =
        DesktopPreferenceChunks.read(preferences, key) ?: default

    fun saveString(key: String, value: String) =
        DesktopPreferenceChunks.write(preferences, key, value)

    /** A configured module index, when one is still set up. */
    fun moduleIndexUrl(): String =
        sourceConfigs().firstOrNull { it.kind == DesktopSourceKind.CUSTOM_MODULE }?.baseUrl.orEmpty()

    /** The single module URL older desktop builds stored, as a source entry. */
    private fun migrateLegacySources(): List<DesktopSourceConfig> {
        val legacyUrl = string(KEY_MODULE_INDEX_URL).trim()
        if (legacyUrl.isBlank()) return emptyList()
        return listOf(
            DesktopSourceConfig(
                id = "custom-module",
                kind = DesktopSourceKind.CUSTOM_MODULE,
                baseUrl = legacyUrl,
                enabled = sourceEnabled("module"),
            ),
        )
    }

    private fun readSongs(key: String): List<Song> = readLines(key).mapNotNull(::decodeSong)

    private fun writeSongs(key: String, songs: List<Song>) =
        writeLines(key, songs.map(::encodeSong))

    private fun readLines(key: String): List<String> =
        DesktopPreferenceChunks.read(preferences, key).orEmpty()
            .split('\n')
            .filter(String::isNotBlank)

    private fun writeLines(key: String, lines: List<String>) =
        DesktopPreferenceChunks.write(preferences, key, lines.joinToString("\n"))

    private fun encodeSong(song: Song): String = listOf(
        song.videoId,
        song.title,
        song.artist,
        song.thumbnailUrl.orEmpty(),
        song.durationText.orEmpty(),
        song.artistId.orEmpty(),
        song.albumId.orEmpty(),
        song.albumName.orEmpty(),
        song.localUri.orEmpty(),
        song.localPath.orEmpty(),
        song.isVideo.toString(),
        song.isVideoOrigin.toString(),
        song.setVideoId.orEmpty(),
        song.fromAutoplay.toString(),
        song.localDateAddedSeconds?.toString().orEmpty(),
        song.localDateModifiedSeconds?.toString().orEmpty(),
        song.sourceQuality.orEmpty(),
        song.isExplicit?.toString().orEmpty(),
    ).joinToString(DELIMITER, transform = ::encode)

    private fun decodeSong(value: String): Song? {
        val fields = value.split(DELIMITER).map(::decode)
        if (fields.size < 10 || fields[0].isBlank()) return null
        val isVideo = fields.getOrNull(10)?.toBooleanStrictOrNull() ?: false
        return Song(
            videoId = fields[0],
            title = fields[1],
            artist = fields[2],
            thumbnailUrl = fields[3].ifBlank { null },
            durationText = fields[4].ifBlank { null },
            artistId = fields[5].ifBlank { null },
            albumId = fields[6].ifBlank { null },
            albumName = fields[7].ifBlank { null },
            localUri = fields[8].ifBlank { null },
            localPath = fields[9].ifBlank { null },
            isVideo = isVideo,
            isVideoOrigin = fields.getOrNull(11)?.toBooleanStrictOrNull() ?: isVideo,
            setVideoId = fields.getOrNull(12)?.ifBlank { null },
            fromAutoplay = fields.getOrNull(13)?.toBooleanStrictOrNull() ?: false,
            localDateAddedSeconds = fields.getOrNull(14)?.toLongOrNull(),
            localDateModifiedSeconds = fields.getOrNull(15)?.toLongOrNull(),
            sourceQuality = fields.getOrNull(16)?.ifBlank { null },
            isExplicit = fields.getOrNull(17)?.toBooleanStrictOrNull(),
        )
    }

    private fun decodePlaylist(value: String): DesktopPlaylist? {
        val fields = value.split(PLAYLIST_DELIMITER)
        if (fields.size < 3) return null
        val id = decode(fields[0]).ifBlank { return null }
        val title = decode(fields[1]).ifBlank { return null }
        val songs = fields[2].split(SONG_DELIMITER).filter(String::isNotBlank).mapNotNull(::decodeSong)
        return DesktopPlaylist(id, title, songs)
    }

    private fun encode(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decode(value: String): String =
        runCatching { String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8) }.getOrDefault("")

    fun preferAudioTracks(): Boolean = boolean(KEY_PREFER_AUDIO_TRACKS, true)

    fun savePreferAudioTracks(enabled: Boolean) = saveBoolean(KEY_PREFER_AUDIO_TRACKS, enabled)

    fun predictiveBufferEnabled(): Boolean = boolean(KEY_PREDICTIVE_BUFFER_ENABLED, true)

    fun savePredictiveBufferEnabled(enabled: Boolean) = saveBoolean(KEY_PREDICTIVE_BUFFER_ENABLED, enabled)

    fun smartTrimEnabled(): Boolean = boolean(KEY_SMART_TRIM_ENABLED, false)

    fun saveSmartTrimEnabled(enabled: Boolean) = saveBoolean(KEY_SMART_TRIM_ENABLED, enabled)

    fun globalHotkeysEnabled(): Boolean = boolean(KEY_GLOBAL_HOTKEYS_ENABLED, false)

    fun saveGlobalHotkeysEnabled(enabled: Boolean) = saveBoolean(KEY_GLOBAL_HOTKEYS_ENABLED, enabled)

    fun globalHotkeysModifier(): String = string(KEY_GLOBAL_HOTKEYS_MODIFIER, "CMD_SHIFT")

    fun saveGlobalHotkeysModifier(modifier: String) = saveString(KEY_GLOBAL_HOTKEYS_MODIFIER, modifier)

    fun menubarMiniPlayerEnabled(): Boolean = boolean(KEY_MENUBAR_MINI_PLAYER, true)

    fun saveMenubarMiniPlayerEnabled(enabled: Boolean) = saveBoolean(KEY_MENUBAR_MINI_PLAYER, enabled)

    fun allowRemixesAndLive(): Boolean = boolean(KEY_ALLOW_REMIXES_LIVE, false)

    fun saveAllowRemixesAndLive(enabled: Boolean) = saveBoolean(KEY_ALLOW_REMIXES_LIVE, enabled)

    companion object {
        const val KEY_LIKED_IDS = "liked_ids"
        const val KEY_DISLIKED_IDS = "disliked_ids"
        const val KEY_HISTORY = "history"
        const val KEY_QUEUE = "queue"
        const val KEY_DOWNLOADS = "downloads"
        const val KEY_PLAYLISTS = "playlists"
        const val KEY_MODULE_INDEX_URL = "module_index_url"
        private const val KEY_ORIGINAL_VERSIONS = "original_versions"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_AUDIO_QUALITY_RUNGS = "audio_quality_rungs"
        const val KEY_SOURCE_CONFIGS = "source_configs"
        const val KEY_SOURCE_CONFIGS_PROTECTED = "source_configs_dpapi_v1"
        const val KEY_AVOID_DUPLICATE_VERSIONS = "avoid_duplicate_versions"
        const val KEY_ALLOW_REMIXES_LIVE = "allow_remixes_live"
        const val KEY_IMMERSIVE_VISUAL_MODE = "immersive_visual_mode"
        const val KEY_IMMERSIVE_COLOR_PALETTE = "immersive_color_palette"
        const val KEY_IMMERSIVE_CLASSIC_MODE = "immersive_classic_mode"
        const val KEY_IMMERSIVE_SHOW_LYRICS = "immersive_show_lyrics"
        const val KEY_PREFER_AUDIO_TRACKS = "prefer_audio_tracks"
        const val KEY_PREDICTIVE_BUFFER_ENABLED = "predictive_buffer_enabled"
        const val KEY_SMART_TRIM_ENABLED = "smart_trim_enabled"
        const val KEY_GLOBAL_HOTKEYS_ENABLED = "global_hotkeys_enabled"
        const val KEY_GLOBAL_HOTKEYS_MODIFIER = "global_hotkeys_modifier"
        const val KEY_MENUBAR_MINI_PLAYER = "menubar_mini_player_enabled"
        const val KEY_DJ_MIX_CURVE = "dj_mix_curve"
        const val MAX_HISTORY = 100
        const val MAX_QUEUE = 200
        const val DELIMITER = "|"
        const val PLAYLIST_DELIMITER = "#"
        const val SONG_DELIMITER = ";"

        fun avoidDuplicateVersions(): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_AVOID_DUPLICATE_VERSIONS, true) }.getOrDefault(true)

        fun saveAvoidDuplicateVersions(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_AVOID_DUPLICATE_VERSIONS, enabled)
                prefs.flush()
            }
        }

        fun allowRemixesAndLive(): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_ALLOW_REMIXES_LIVE, false) }.getOrDefault(false)

        fun saveAllowRemixesAndLive(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_ALLOW_REMIXES_LIVE, enabled)
                prefs.flush()
            }
        }

        fun immersiveVisualMode(default: String = "WINAMP_WAVES"): String =
            runCatching { DesktopPreferenceMigration.open().get(KEY_IMMERSIVE_VISUAL_MODE, default) }.getOrDefault(default)

        fun saveImmersiveVisualMode(modeName: String) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.put(KEY_IMMERSIVE_VISUAL_MODE, modeName)
                prefs.flush()
            }
        }

        fun immersiveColorPalette(default: String = "COVER_ART"): String =
            runCatching { DesktopPreferenceMigration.open().get(KEY_IMMERSIVE_COLOR_PALETTE, default) }.getOrDefault(default)

        fun saveImmersiveColorPalette(paletteName: String) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.put(KEY_IMMERSIVE_COLOR_PALETTE, paletteName)
                prefs.flush()
            }
        }

        fun immersiveClassicMode(default: Boolean = false): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_IMMERSIVE_CLASSIC_MODE, default) }.getOrDefault(default)

        fun saveImmersiveClassicMode(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_IMMERSIVE_CLASSIC_MODE, enabled)
                prefs.flush()
            }
        }

        fun immersiveShowLyrics(default: Boolean = true): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_IMMERSIVE_SHOW_LYRICS, default) }.getOrDefault(default)

        fun saveImmersiveShowLyrics(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_IMMERSIVE_SHOW_LYRICS, enabled)
                prefs.flush()
            }
        }

        fun preferAudioTracks(default: Boolean = true): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_PREFER_AUDIO_TRACKS, default) }.getOrDefault(default)

        fun savePreferAudioTracks(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_PREFER_AUDIO_TRACKS, enabled)
                prefs.flush()
            }
        }

        fun predictiveBufferEnabled(default: Boolean = true): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_PREDICTIVE_BUFFER_ENABLED, default) }.getOrDefault(default)

        fun savePredictiveBufferEnabled(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_PREDICTIVE_BUFFER_ENABLED, enabled)
                prefs.flush()
            }
        }

        fun smartTrimEnabled(default: Boolean = false): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_SMART_TRIM_ENABLED, default) }.getOrDefault(default)

        fun saveSmartTrimEnabled(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_SMART_TRIM_ENABLED, enabled)
                prefs.flush()
            }
        }

        fun globalHotkeysEnabled(default: Boolean = false): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_GLOBAL_HOTKEYS_ENABLED, default) }.getOrDefault(default)

        fun saveGlobalHotkeysEnabled(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_GLOBAL_HOTKEYS_ENABLED, enabled)
                prefs.flush()
            }
        }

        fun globalHotkeysModifier(default: String = "CMD_SHIFT"): String =
            runCatching { DesktopPreferenceMigration.open().get(KEY_GLOBAL_HOTKEYS_MODIFIER, default) }.getOrDefault(default)

        fun saveGlobalHotkeysModifier(modifier: String) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.put(KEY_GLOBAL_HOTKEYS_MODIFIER, modifier)
                prefs.flush()
            }
        }

        fun menubarMiniPlayerEnabled(default: Boolean = true): Boolean =
            runCatching { DesktopPreferenceMigration.open().getBoolean(KEY_MENUBAR_MINI_PLAYER, default) }.getOrDefault(default)

        fun saveMenubarMiniPlayerEnabled(enabled: Boolean) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.putBoolean(KEY_MENUBAR_MINI_PLAYER, enabled)
                prefs.flush()
            }
        }

        fun djMixCurve(default: String = "EQUAL_POWER"): String =
            runCatching { DesktopPreferenceMigration.open().get(KEY_DJ_MIX_CURVE, default) }.getOrDefault(default)

        fun saveDjMixCurve(curve: String) {
            runCatching {
                val prefs = DesktopPreferenceMigration.open()
                prefs.put(KEY_DJ_MIX_CURVE, curve)
                prefs.flush()
            }
        }
    }
}

data class DesktopPlaylist(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val songs: List<Song> = emptyList(),
)

private fun DesktopPlaylist.toPreferenceLine(): String = listOf(
    Base64.getUrlEncoder().withoutPadding().encodeToString(id.toByteArray(StandardCharsets.UTF_8)),
    Base64.getUrlEncoder().withoutPadding().encodeToString(title.toByteArray(StandardCharsets.UTF_8)),
    songs.joinToString(";") { song ->
        listOf(
            song.videoId,
            song.title,
            song.artist,
            song.thumbnailUrl.orEmpty(),
            song.durationText.orEmpty(),
            song.artistId.orEmpty(),
            song.albumId.orEmpty(),
            song.albumName.orEmpty(),
            song.localUri.orEmpty(),
            song.localPath.orEmpty(),
            song.isVideo.toString(),
            song.isVideoOrigin.toString(),
            song.setVideoId.orEmpty(),
            song.fromAutoplay.toString(),
            song.localDateAddedSeconds?.toString().orEmpty(),
            song.localDateModifiedSeconds?.toString().orEmpty(),
            song.sourceQuality.orEmpty(),
            song.isExplicit?.toString().orEmpty(),
        ).joinToString("|", transform = { field ->
            Base64.getUrlEncoder().withoutPadding().encodeToString(field.toByteArray(StandardCharsets.UTF_8))
        })
    },
).joinToString("#")
