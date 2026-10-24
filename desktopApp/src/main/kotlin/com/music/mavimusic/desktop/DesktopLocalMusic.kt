package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import java.util.Locale
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/** Files stay in place; the library stores references and never edits their tags. */
object DesktopLocalMusic {
    internal val supportedExtensions = setOf("aac", "aiff", "aif", "flac", "m4a", "mp3", "ogg", "opus", "wav", "webm")

    fun scan(): List<Song> = runCatching {
        val filterNonMusic = DesktopPersistence().boolean(KEY_FILTER_NON_MUSIC_AUDIO, false)
        val explicitFolders = importedFolders().toSet()
        (roots().filter { runCatching { Files.isDirectory(it) }.getOrDefault(false) }
            .flatMap { scanDirectory(it, filterNonMusic && it !in explicitFolders) } +
            importedPaths().filter { runCatching { Files.isRegularFile(it) }.getOrDefault(false) }
                .filter(::supports).mapNotNull { runCatching { it.toSong() }.getOrNull() })
            .distinctBy(Song::localPath)
            .sortedBy { it.title.lowercase(Locale.ROOT) }
            .also { DesktopMediaCache.trim() }
    }.getOrDefault(emptyList())

    internal fun supports(path: Path): Boolean = path.extension.lowercase(Locale.ROOT) in supportedExtensions

    internal fun importedPaths(): List<Path> = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(
            DesktopPersistence().string("local_imported_paths", "[]"),
        ).mapNotNull { runCatching { Path.of(it) }.getOrNull() }
    }.getOrDefault(emptyList())

    private fun importedFolders(): List<Path> = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(
            DesktopPersistence().string("local_imported_folders", "[]"),
        ).mapNotNull { runCatching { Path.of(it) }.getOrNull() }
    }.getOrDefault(emptyList())

    internal fun watchRoots(): List<Path> = (roots() + importedPaths().mapNotNull { it.parent }).distinct()

    /** Import is a reference, not a copy. Explicit selections bypass the small-clip filter. */
    @Synchronized
    fun importFiles(paths: List<Path>): List<Song> {
        val songs = paths.flatMap { path ->
            if (Files.isDirectory(path)) scanDirectory(path, false).sortedWith(
                compareBy<Song> { it.albumName.orEmpty() }.thenBy { it.trackNumber ?: Int.MAX_VALUE }.thenBy { it.title },
            )
            else if (Files.isRegularFile(path) && supports(path)) listOf(path.toSong())
            else emptyList()
        }.distinctBy(Song::localPath)
        val folders = (importedFolders() + paths.filter(Files::isDirectory).map { it.toRealPath() }).distinct()
        DesktopPersistence().saveString("local_imported_folders",
            kotlinx.serialization.json.Json.encodeToString(folders.map(Path::toString)))
        // Folder imports need one reference, not thousands of per-track preference entries.
        val explicitFiles = paths.filter { Files.isRegularFile(it) && supports(it) }
            .mapNotNull { runCatching { it.toRealPath().toString() }.getOrNull() }
        val imported = (importedPaths().map(Path::toString) + explicitFiles).distinct()
        DesktopPersistence().saveString("local_imported_paths",
            kotlinx.serialization.json.Json.encodeToString(imported))
        DesktopMediaCache.trim()
        return songs
    }

    /** The selected/default library, plus folders explicitly imported by the listener. */
    internal fun roots(): List<Path> {
        val configured = folder()
        if (configured != null) {
            return (listOf(configured) + importedFolders()).distinct()
        }
        val imported = importedFolders()
        if (imported.isNotEmpty()) {
            return imported.distinct()
        }
        // If the user hasn't configured a local music folder or imported files,
        // we do NOT scan unprompted. Scanning ~/Downloads or Apple Music on startup
        // triggers macOS system permission prompts (TCC) on every app restart/update.
        return emptyList()
    }

    /** The folder the listener picked, or null for all of them. */
    fun folder(): Path? = DesktopPersistence().string(KEY_LOCAL_MUSIC_FOLDER)
        .takeIf(String::isNotBlank)
        ?.let { runCatching { Paths.get(it) }.getOrNull() }
        ?.takeIf(Files::isDirectory)

    fun setFolder(path: Path?) {
        DesktopPersistence().saveString(KEY_LOCAL_MUSIC_FOLDER, path?.toAbsolutePath()?.toString().orEmpty())
    }

    /** What the Local Music settings row says underneath its title. */
    fun folderLabel(): String = folder()?.fileName?.toString() ?: "Sin carpeta configurada (Haz clic para seleccionar)"

    internal const val KEY_LOCAL_MUSIC_FOLDER = "local_music_folder"

    /**
     * Asks for a folder with the platform's own chooser.
     *
     * Swing's, not a drawn one: this is a file-system question, and the desktop already has an
     * answer for it that knows about bookmarks, hidden files and network mounts.
     */
    fun chooseFolder(): Path? = runCatching {
        val chooser = javax.swing.JFileChooser(folder()?.toFile() ?: java.io.File(System.getProperty("user.home")))
        chooser.fileSelectionMode = javax.swing.JFileChooser.DIRECTORIES_ONLY
        chooser.dialogTitle = "Local music folder"
        if (chooser.showOpenDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return null
        chooser.selectedFile?.toPath()
    }.getOrNull()

    private fun scanDirectory(root: Path, filterNonMusic: Boolean): List<Song> = buildList {
        runCatching {
            Files.walkFileTree(root, emptySet(), 32, object : java.nio.file.SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult {
                    val name = dir.fileName?.toString()?.lowercase(Locale.ROOT) ?: ""
                    // Skip Apple Music / iTunes library packages and system directories that trigger TCC permission prompts
                    if (name.startsWith(".") ||
                        name.endsWith(".musiclibrary") ||
                        name.endsWith(".itdb") ||
                        name.endsWith(".photoslibrary") ||
                        name == "music library" ||
                        (name == "music" && dir.parent?.fileName?.toString()?.lowercase(Locale.ROOT) == "music")
                    ) {
                        return java.nio.file.FileVisitResult.SKIP_SUBTREE
                    }
                    return java.nio.file.FileVisitResult.CONTINUE
                }

                override fun visitFile(path: Path, attrs: BasicFileAttributes): java.nio.file.FileVisitResult {
                    if (Files.isRegularFile(path) && supports(path) && (!filterNonMusic || isMusic(path))) {
                        runCatching { path.toSong() }.getOrNull()?.let { add(it) }
                    }
                    return java.nio.file.FileVisitResult.CONTINUE
                }
                override fun visitFileFailed(path: Path, error: java.io.IOException) = java.nio.file.FileVisitResult.CONTINUE
            })
        }
    }

    /** Optional scan filter; explicit file selections always bypass it. */
    internal fun isMusic(path: Path): Boolean {
        if (path.extension.lowercase(Locale.ROOT) == "wav") return false
        if (runCatching { Files.size(path) }.getOrDefault(Long.MAX_VALUE) < MIN_MUSIC_BYTES) return false
        val haystack = path.toString().lowercase(Locale.ROOT)
        return NON_MUSIC_MARKERS.none { it in haystack }
    }

    /** Folders and names that hold something other than music. */
    private val NON_MUSIC_MARKERS = listOf(
        "/ringtones/", "/notifications/", "/alarms/", "/ui/", "/recordings/", "/voice recorder/",
        "voice note", "voicenote", "recording", "ringtone", "notification", "alarm",
    )

    /**
     * The size below which a file cannot hold thirty seconds of music — thirty seconds of even a
     * 64 kbps stream is a quarter of a megabyte.
     */
    private const val MIN_MUSIC_BYTES = 240L * 1024

    internal const val KEY_FILTER_NON_MUSIC_AUDIO = "filter_non_music_audio"

    internal fun Path.toSong(): Song {
        val absolutePath = runCatching { toRealPath() }.getOrElse { toAbsolutePath().normalize() }
        val attributes = runCatching {
            Files.readAttributes(absolutePath, BasicFileAttributes::class.java)
        }.getOrNull()
        val modified = attributes?.lastModifiedTime()?.toInstant()
        val added = attributes?.creationTime()?.toInstant() ?: modified ?: Instant.now()
        val identity = absolutePath.toString()
        val parsed = parseFileName(nameWithoutExtension)
        val metadata = DesktopLocalMetadata.read(absolutePath)
        val codec = extension.lowercase(Locale.ROOT).let { value ->
            when (value) {
                "flac" -> "FLAC"
                "wav" -> "WAV"
                "aac", "m4a" -> "AAC"
                "ogg" -> "OGG"
                "webm" -> "OPUS"
                else -> value.uppercase(Locale.ROOT)
            }
        }
        return Song(
            videoId = "local:$identity",
            title = metadata.title ?: parsed.title,
            artist = metadata.artist ?: parsed.artist,
            thumbnailUrl = metadata.artworkUri,
            albumName = metadata.album,
            durationText = metadata.durationSeconds?.let { "%d:%02d".format(Locale.ROOT, it / 60, it % 60) },
            trackNumber = metadata.trackNumber,
            localUri = absolutePath.toUri().toString(),
            localPath = absolutePath.toString(),
            localDateAddedSeconds = added.epochSecond,
            localDateModifiedSeconds = modified?.epochSecond,
            sourceQuality = codec,
        )
    }

    private fun parseFileName(name: String): ParsedName {
        val parts = name.split(" - ", limit = 2)
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            ParsedName(title = parts[1].trim(), artist = parts[0].trim())
        } else {
            ParsedName(title = name, artist = "On This Computer")
        }
    }

    private data class ParsedName(val title: String, val artist: String)
}

enum class DesktopLibrarySort {
    TITLE_ASC,
    TITLE_DESC,
    DATE_ADDED,
    DATE_MODIFIED,
}

enum class DesktopLibraryView {
    LIST,
    GRID,
}

fun List<Song>.sortedForDesktopLibrary(sort: DesktopLibrarySort): List<Song> = when (sort) {
    DesktopLibrarySort.TITLE_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER, Song::title))
    DesktopLibrarySort.TITLE_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER, Song::title))
    DesktopLibrarySort.DATE_ADDED -> sortedWith(
        compareByDescending<Song> { it.localDateAddedSeconds ?: Long.MIN_VALUE }
            .thenBy(String.CASE_INSENSITIVE_ORDER, Song::title),
    )
    DesktopLibrarySort.DATE_MODIFIED -> sortedWith(
        compareByDescending<Song> { it.localDateModifiedSeconds ?: Long.MIN_VALUE }
            .thenBy(String.CASE_INSENSITIVE_ORDER, Song::title),
    )
}
