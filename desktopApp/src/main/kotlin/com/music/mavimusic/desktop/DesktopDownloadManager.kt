package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Downloads resolved audio into the user's own MaviMusic folder. */
object DesktopDownloadManager {
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /** Keeps an interrupted transfer in place and resumes it when the user tries again. */
    suspend fun download(
        song: Song,
        quality: String = DesktopPersistence().string("download_quality", "LOSSLESS"),
        context: DesktopDownloadContext = DesktopDownloadContext(),
        onState: (DesktopDownloadProgress) -> Unit = {},
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
    ): Result<Song> = downloadUsing(song, quality, context, DesktopDownloadServices(), onState, onProgress)

    internal suspend fun downloadUsing(
        song: Song, quality: String, context: DesktopDownloadContext,
        services: DesktopDownloadServices,
        onState: (DesktopDownloadProgress) -> Unit = {},
        onProgress: (Long, Long?) -> Unit = { _, _ -> },
    ): Result<Song> {
        val measurement = DesktopDownloadMeasurement(onState)
        var failureStage = DesktopDownloadStage.SONG
        fun stage(value: DesktopDownloadStage) { failureStage = value; measurement.stage(value) }
        val job = currentCoroutineContext()[Job]
        if (job != null) activeJobs[song.videoId] = job
        return try {
            require(song.localPath == null) { "This track is already local" }
            stage(DesktopDownloadStage.SONG)
            val audioSong = services.song(song)
            stage(DesktopDownloadStage.METADATA)
            val canonical = services.metadata(audioSong)
            // Keep the requested row's ID for queue/library bookkeeping; metadata belongs to the audio release.
            val exportSong = canonical.copy(videoId = song.videoId,
                albumName = if (context.kind == DesktopDownloadContext.Kind.ALBUM) song.albumName ?: canonical.albumName else canonical.albumName,
                trackNumber = song.trackNumber ?: canonical.trackNumber,
                thumbnailUrl = if (context.kind == DesktopDownloadContext.Kind.ALBUM) song.thumbnailUrl ?: canonical.thumbnailUrl else canonical.thumbnailUrl)
            val root = getDownloadDirectory()
            val targetMp3 = DesktopDownloadPaths.target(root, exportSong, context)
            val directory = targetMp3.parent
            Files.createDirectories(directory)
            val staging = DesktopDownloadPaths.staging(root, song.videoId, context)
            Files.createDirectories(staging)
            val safeName = targetMp3.fileName.toString().removeSuffix(".mp3")
            suspend fun finish(source: Path, keepSource: Boolean = false): Song {
                val saved = finishAudio(exportSong, source, targetMp3, enrich = false, keepSource = keepSource) { stage(it) }
                val info = DesktopExportProbe.inspect(targetMp3.toFile())
                DesktopDownloadHistory.record(DesktopDownloadReceipt(exportSong.title, targetMp3.toString(), info,
                    measurement.finish(), if (audioSong.isVideo) "No se encontró versión canción; se guardó el audio del video." else null))
                runCatching { Files.deleteIfExists(staging) }
                return saved
            }
            if (Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) {
                return Result.success(finish(targetMp3))
            }
            // Exporting the same saved recording into another collection reuses its audio.
            // The existing file belongs to the listener and must remain in its original folder.
            val existingFile = DesktopPersistence().downloads().firstOrNull { it.videoId == song.videoId }
                ?.let(::savedFile) ?: savedFile(exportSong)
            if (existingFile != null) return Result.success(finish(existingFile, keepSource = true))

            // A previous conversion failure must retry the local audio, not fetch it again.
            val recoverable = listOf("webm", "ogg", "opus", "m4a", "flac", "wav")
                .flatMap { listOf(staging.resolve("audio.$it"), directory.resolve("$safeName.$it"), root.resolve("${safeName(song)}.$it")) }
                .firstOrNull { Files.isRegularFile(it) && Files.size(it) > 0L }
            if (recoverable != null) {
                return Result.success(finish(recoverable))
            }

            stage(DesktopDownloadStage.RESOLVING)
            val stream = services.stream(canonical, quality).getOrThrow()
            val manifest = DesktopManifestDownload.isManifest(stream)
            val extension = stream.format.fileExtension().let { if (manifest && it != "flac") "m4a" else it }
            val target = staging.resolve("audio.$extension")
            val temporary = staging.resolve("audio.$extension.part")
            stage(DesktopDownloadStage.TRANSFER)
            val progress: (Long, Long?) -> Unit = { done, total -> measurement.bytes(done, total); onProgress(done, total) }
            if (manifest) {
                Files.deleteIfExists(temporary)
                Files.newOutputStream(temporary).buffered().use { output ->
                    DesktopManifestDownload.save(
                        stream, extension, output,
                        tempDir = java.io.File(System.getProperty("java.io.tmpdir")),
                        userAgent = DOWNLOAD_USER_AGENT,
                    ) { done, total -> progress(done, total) }
                }
            } else {
                DesktopFileTransfer().save(stream, temporary, DOWNLOAD_USER_AGENT, progress)
            }
            currentCoroutineContext().ensureActive()
            check(Files.size(temporary) > 0L) { "Download failed: nothing was sent" }
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }
            Result.success(finish(target))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            measurement.finish()
            Result.failure(IllegalStateException("${failureStage.label}: ${failure.message}", failure))
        } finally {
            if (job != null) activeJobs.remove(song.videoId, job)
        }
    }

    private suspend fun finishAudio(song: Song, source: Path, targetMp3: Path, enrich: Boolean = true, keepSource: Boolean = false, onStage: (DesktopDownloadStage) -> Unit = {}): Song {
        val taggedSong = if (enrich) DesktopDownloadMetadata.enrich(song) else song
        run {
            check(DesktopAudioTranscoder.transcodeToMp3(source.toFile(), targetMp3.toFile(), taggedSong, onStage = onStage)) {
                "The audio could not be converted to MP3"
            }
            check(Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) { "The MP3 output is missing" }
            if (source != targetMp3 && !keepSource) Files.deleteIfExists(source)
        }
        return savedSong(taggedSong, targetMp3)
    }

    suspend fun repairMetadata(song: Song): Result<Song> {
        return try {
            val file = savedFile(song) ?: error("La descarga ya no existe en esta carpeta")
            val target = if (file.fileName.toString().endsWith(".mp3", ignoreCase = true)) file
                else file.resolveSibling(file.fileName.toString().substringBeforeLast('.') + ".mp3")
            Result.success(finishAudio(song, file, target))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { Result.failure(failure) }
    }

    private fun savedSong(song: Song, path: Path): Song =
        song.copy(localUri = path.toUri().toString(), localPath = path.toString())

    fun cancel(videoId: String) {
        activeJobs[videoId]?.cancel(CancellationException("Download cancelled"))
    }

    /**
     * The file saved for [song], or null when the record has outlived it.
     *
     * A download record is a claim about a folder this app does not own: the listener is expected
     * to manage it with a file manager, so an entry can name a file that has since been moved or
     * deleted. Android checks before it answers for the same reason — without it the player is
     * handed a path that is simply not there and the track refuses to play, saying only "could not
     * open stream".
     */
    fun defaultDownloadDirectory(): Path =
        Path.of(System.getProperty("user.home"), "Music", "MaviMusic")

    fun getDownloadDirectory(): Path {
        val configured = DesktopPersistence().string("download_folder", "").trim()
        val path = if (configured.isNotBlank()) {
            runCatching { Path.of(configured) }.getOrNull() ?: defaultDownloadDirectory()
        } else {
            defaultDownloadDirectory()
        }
        runCatching { Files.createDirectories(path) }
        return path
    }

    fun savedFile(song: Song): Path? = runCatching {
        val path = song.localPath?.let { runCatching { Path.of(it) }.getOrNull() }
            ?: song.localUri?.takeIf { it.startsWith("file:") }
                ?.let { runCatching { Path.of(URI(it)) }.getOrNull() }
        if (path != null && runCatching { Files.isRegularFile(path) && Files.size(path) > 0 }.getOrDefault(false)) {
            return path
        }
        if (song.videoId.startsWith("local:")) return null
        val structured = DesktopDownloadPaths.target(getDownloadDirectory(), song, DesktopDownloadContext())
        if (runCatching { Files.isRegularFile(structured) && Files.size(structured) > 0 }.getOrDefault(false)) return structured
        val safeName = safeName(song)

        val checkDirs = listOfNotNull(
            getDownloadDirectory(),
            defaultDownloadDirectory(),
        ).distinct()
        for (dir in checkDirs) {
            val candidateMp3 = dir.resolve("$safeName.mp3")
            if (runCatching { Files.isRegularFile(candidateMp3) && Files.size(candidateMp3) > 0 }.getOrDefault(false)) {
                return candidateMp3
            }
        }
        null
    }.getOrNull()

    /** Whether [song] is really on this computer, rather than merely recorded as being. */
    fun isSaved(song: Song): Boolean = savedFile(song) != null

    /**
     * The records that still name a file, with the rest dropped.
     *
     * A claim that has just been shown to be false is not worth keeping to be shown false again on
     * the next play.
     */
    fun verified(downloads: List<Song>): List<Song> = downloads.filter(::isSaved)

    /** Deletes the file saved for [song]. Returns whether there was one to delete. */
    fun delete(song: Song): Boolean {
        val file = savedFile(song) ?: return false
        return runCatching { Files.deleteIfExists(file) }
            .onFailure { DesktopTrackLog.log("could not delete the download of '${song.title}': ${it.message}") }
            .getOrDefault(false)
    }

    private fun DesktopStreamFormat.fileExtension(): String {
        val codec = codec.orEmpty().substringAfterLast('/').substringBefore(';').lowercase()
        return when (codec) {
            "flac" -> "flac"
            "wav", "wave" -> "wav"
            "alac" -> "m4a"
            "aac" -> "m4a"
            "mp3" -> "mp3"
            "ogg", "opus" -> "ogg"
            "webm" -> "webm"
            else -> "m4a"
        }
    }

    private fun safeName(song: Song): String = "${song.artist} - ${song.title}"
        .replace(ILLEGAL_FILENAME, "_").trim().take(180)
        .ifBlank { song.videoId.replace(ILLEGAL_FILENAME, "_").take(180).ifBlank { "track" } }

    private const val DOWNLOAD_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    private val ILLEGAL_FILENAME = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
}
