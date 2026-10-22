package com.music.bitchord.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The app's own copy, in whichever language is in force.
 *
 * The catalogues are Android's own, copied in at build time from its `res` values folders,
 * so a translation written once is read by both targets and desktop never drifts into its own
 * wording for a string Android already has.
 *
 * A key with no translation in the chosen language falls back to English rather than showing the
 * key: a half-translated locale should read as mostly-English, not as a debug build.
 */
object DesktopStrings {

    /** The languages there are catalogues for, by IETF tag. */
    val available: List<DesktopLanguage> = listOf(
        DesktopLanguage("", "System default"),
        DesktopLanguage("en", "English"),
        DesktopLanguage("de", "Deutsch"),
        DesktopLanguage("es", "Español"),
        DesktopLanguage("fr", "Français"),
        DesktopLanguage("he", "עברית"),
        DesktopLanguage("hi", "हिन्दी"),
        DesktopLanguage("id", "Bahasa Indonesia"),
        DesktopLanguage("it", "Italiano"),
        DesktopLanguage("ja", "日本語"),
        DesktopLanguage("pl", "Polski"),
        DesktopLanguage("pt", "Português"),
        DesktopLanguage("ru", "Русский"),
        DesktopLanguage("tr", "Türkçe"),
        DesktopLanguage("zh-Hans", "简体中文"),
        DesktopLanguage("zh-Hant", "繁體中文"),
    )

    private val english: Map<String, String> by lazy { catalogue("en") }

    @Volatile
    private var current: Map<String, String> = emptyMap()

    private val _language = MutableStateFlow(DesktopPersistence().string(KEY_LANGUAGE))

    /** The stored choice: a tag, or blank for the system's own language. */
    val language: StateFlow<String> = _language

    private const val KEY_MAVIMUSIC_INIT = "mavimusic_lang_initialized_v5"

    val SPANISH_FALLBACKS: Map<String, String> = mapOf(
        "home" to "Inicio",
        "explore" to "Explorar",
        "library" to "Biblioteca",
        "search" to "Buscar",
        "history" to "Historial",
        "downloads" to "Descargas",
        "downloaded_songs" to "Canciones descargadas",
        "local_music" to "Música local",
        "d_audio_files_on_this_computer" to "Archivos de audio en este equipo",
        "spotify_library_subtitle" to "Tus listas de reproducción y canciones favoritas",
        "settings" to "Ajustes",
        "play" to "Reproducir",
        "pause" to "Pausar",
        "playback" to "Reproducción",
        "playback_play" to "Reproducir",
        "playback_pause" to "Pausar",
        "shuffle" to "Aleatorio",
        "repeat" to "Repetir",
        "queue" to "Cola de reproducción",
        "lyrics" to "Letras",
        "show_all" to "Ver todo",
        "hide_from_sidebar" to "Ocultar de la barra lateral",
        "storage" to "Almacenamiento",
        "equalizer" to "Ecualizador",
        "done" to "Listo",
        "cancel" to "Cancelar",
        "delete" to "Eliminar",
        "rename" to "Renombrar",
        "remove" to "Quitar",
        "retry" to "Reintentar",
        "save" to "Guardar",
        "share" to "Compartir",
        "subscribe" to "Suscribirse",
        "unsubscribe" to "Cancelar suscripción",
        "top_songs" to "Canciones destacadas",
        "top_albums" to "Álbumes destacados",
        "top_artists" to "Artistas destacados",
        "d_about_artist" to "Acerca del artista",
        "new_playlist" to "Nueva lista",
        "playlist_name" to "Nombre de la lista",
        "app_language" to "Idioma de la aplicación",
        "language" to "Idioma",
        "audio_output" to "Salida de audio",
        "audio_output_playing" to "Reproduciendo aquí",
        "listen_together" to "Escuchar juntos",
        "song_cache_limit" to "Límite de caché de canciones",
        "filter_languages" to "Filtrar idiomas",
        "translation_language" to "Idioma de traducción",
        "d_follow_the_app_language" to "Seguir el idioma de la aplicación",
        "nothing_downloading" to "No hay descargas en curso",
        "cancel_all" to "Cancelar todo",
        "clear" to "Limpiar",
        "queued" to "En cola",
        "like" to "Me gusta",
        "unlike" to "Ya no me gusta",
        "feeling_lucky" to "Tener suerte",
        "sleep_timer" to "Temporizador de apagado",
        "auto_quick_picks" to "Selección rápida",
        "auto_recents" to "Recientes",
        "shelf_quick_picks" to "Selección rápida",
        "shelf_listen_again" to "Volver a escuchar",
        "shelf_mixed_for_you" to "Mezclas para ti",
        "shelf_recents" to "Recientes",
        "shelf_forgotten_favorites" to "Favoritos olvidados",
        "shelf_trending" to "Tendencias",
        "shelf_for_you" to "Para ti",
        "shelf_from_your_library" to "De tu biblioteca",
        "shelf_charts" to "Listas de éxitos",
        "shelf_new_releases" to "Novedades",
        "shelf_top_music_videos" to "Vídeos musicales destacados",
        "widget_previous" to "Anterior",
        "widget_next" to "Siguiente",
        "widget_open_bitchord" to "Abrir MaviMusic",
        "d_quit_bitchord" to "Salir de MaviMusic",
        "d_choose_something_to_play" to "Elige algo para reproducir",
        "command_palette" to "Paleta de comandos",
        "search_placeholder_palette" to "Buscar canciones, artistas, páginas o comandos... (⌘K)",
        "download_all_mp3" to "Descargar todo a MP3",
        "all_downloaded" to "Descargado en MP3",
        "command_lucky" to "Tener suerte (Música aleatoria según tus gustos)",
        "command_sleep" to "Temporizador de apagado (Sleep Timer)",
        "command_downloads" to "Administrador de descargas",
        "command_play_pause" to "Reproducir / Pausar",
        "command_next" to "Siguiente pista",
        "command_previous" to "Pista anterior",
        "command_shuffle" to "Alternar modo aleatorio",
        "command_lyrics" to "Ver letras sincronizadas",
        "command_queue" to "Ver cola de reproducción",
    )

    init {
        val persistence = DesktopPersistence()
        val stored = persistence.string(KEY_LANGUAGE)
        // Default to Spanish for MaviMusic
        if (stored.isBlank() || stored == "system" || stored == "en" || !persistence.boolean(KEY_MAVIMUSIC_INIT, false)) {
            persistence.saveString(KEY_LANGUAGE, "es")
            persistence.saveBoolean(KEY_MAVIMUSIC_INIT, true)
            _language.value = "es"
        }
        val tag = resolvedTag()
        current = catalogue(tag)
        val loc = Locale.forLanguageTag(tag)
        runCatching {
            Locale.setDefault(loc)
            Locale.setDefault(Locale.Category.DISPLAY, loc)
            Locale.setDefault(Locale.Category.FORMAT, loc)
        }
    }

    /** Which catalogue is actually in use, after the system default is resolved. */
    fun resolvedTag(): String {
        val chosen = _language.value
        if (chosen.isNotBlank() && chosen != "system") {
            if (chosen == "zh") return "zh-Hans"
            if (available.any { it.tag.equals(chosen, ignoreCase = true) }) return chosen
            return com.music.bitchord.data.LocaleTags.normalizeAppTag(chosen)
        }
        return "es"
    }

    /**
     * One language, both layers.
     *
     * `<tag>.xml` is Android's own catalogue, copied in at build time. `desktop-<tag>.xml` holds
     * the copy only this target has — its window chrome, the source editor, output precision — and
     * is laid over the top, so a desktop key never collides with an Android one.
     *
     * zh-Hant/zh-Hans fall back to legacy "zh" files so old installs keep
     * working while the script-tagged catalogues roll out.
     */
    private fun catalogue(tag: String): Map<String, String> = when (tag) {
        "zh-Hant" ->
            load("zh-Hant").ifEmpty { load("zh") } +
                load("desktop-zh-Hant").ifEmpty { load("desktop-zh") }
        "zh-Hans" ->
            load("zh-Hans").ifEmpty { load("zh") } +
                load("desktop-zh-Hans").ifEmpty { load("desktop-zh") }
        else -> load(tag) + load("desktop-$tag")
    }

    fun setLanguage(tag: String) {
        DesktopPersistence().saveString(KEY_LANGUAGE, tag)
        _language.value = tag
        val resolved = resolvedTag()
        current = catalogue(resolved)
        val loc = Locale.forLanguageTag(resolved)
        runCatching {
            Locale.setDefault(loc)
            Locale.setDefault(Locale.Category.DISPLAY, loc)
            Locale.setDefault(Locale.Category.FORMAT, loc)
        }
    }

    /** The label shown for the stored choice. */
    fun languageLabel(): String =
        available.firstOrNull { it.tag == _language.value }?.label ?: "Español"

    /**
     * The string for [key], or [fallback] when no catalogue has one.
     *
     * When Spanish is active, prioritizes Spanish catalogue and Spanish fallbacks.
     */
    operator fun get(key: String, fallback: String = key): String {
        val resolved = resolvedTag()
        if (resolved.startsWith("es")) {
            return current[key] ?: SPANISH_FALLBACKS[key] ?: fallback.takeIf { it != key && !it.startsWith("English") } ?: english[key] ?: key
        }
        return current[key] ?: english[key] ?: fallback
    }

    /** As [get], with Android's `%1$s`-style positional arguments filled in. */
    fun format(key: String, vararg arguments: Any?, fallback: String = key): String =
        runCatching { String.format(get(key, fallback), *arguments) }.getOrDefault(get(key, fallback))

    private fun load(tag: String): Map<String, String> = runCatching {
        val stream = DesktopStrings::class.java.getResourceAsStream("/strings/$tag.xml")
            ?: return@runCatching emptyMap()
        val document = stream.use {
            DocumentBuilderFactory.newInstance()
                .apply { isNamespaceAware = false }
                .newDocumentBuilder()
                .parse(it)
        }
        val nodes = document.getElementsByTagName("string")
        buildMap {
            for (index in 0 until nodes.length) {
                val node = nodes.item(index)
                val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                put(name, unescape(node.textContent.orEmpty()))
            }
        }
    }.getOrDefault(emptyMap())

    /**
     * A catalogue value as the UI should read it.
     *
     * Android escapes an apostrophe for its own resource parser, and a `\uXXXX` can survive a
     * round trip out of source into a catalogue — either of which would otherwise be drawn
     * literally, which is how "Don't" once reached the screen as "Don\u2019t".
     */
    internal fun unescape(raw: String): String {
        val decoded = UNICODE_ESCAPE.replace(raw) { it.groupValues[1].toInt(16).toChar().toString() }
        return decoded.replace("\\'", "'").replace("\\\"", "\"").replace("\\n", "\n")
    }

    private val UNICODE_ESCAPE = Regex("""\\u([0-9a-fA-F]{4})""")

    internal const val KEY_LANGUAGE = "app_language"
}

/** One entry in the language picker. */
data class DesktopLanguage(val tag: String, val label: String)
