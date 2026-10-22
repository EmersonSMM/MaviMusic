package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/** Downloads resolved audio into the user's own BitChord folder. */
object DesktopDownloadManager {
    private val activeJobs = ConcurrentHashMap<String, Job>()

    /** Keeps an interrupted transfer in place and resumes it when the user tries again. */
    suspend fun download(
        song: Song,
        quality: String = DesktopPersistence().string("download_quality", "LOSSLESS"),
        onProgress: (downloaded: Long, total: Long?) -> Unit = { _, _ -> },
    ): Result<Song> {
        val job = currentCoroutineContext()[Job]
        if (job != null) activeJobs[song.videoId] = job
        return try {
            require(song.localPath == null) { "This track is already local" }
            val stream = DesktopMusicSources.resolve(song, quality, forDownload = true).getOrThrow()
            val directory = Path.of(System.getProperty("user.home"), "Music", "MaviMusic")
            Files.createDirectories(directory)
            val safeName = buildString {
                append(song.artist)
                append(" - ")
                append(song.title)
            }.replace(ILLEGAL_FILENAME, "_").trim().take(180).ifBlank { song.videoId }

            val targetMp3 = directory.resolve("$safeName.mp3")
            if (Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) {
                return Result.success(song.copy(localUri = targetMp3.toUri().toString(), localPath = targetMp3.toString()))
            }

            // A manifest is an index of the audio, not the audio: fetched as a file it saved a few
            // KB of XML as `.flac`. Its segments are fMP4, so it can only become FLAC or M4A.
            val manifest = DesktopManifestDownload.isManifest(stream)
            val extension = stream.format.fileExtension().let { if (manifest && it != "flac") "m4a" else it }
            val target = directory.resolve("$safeName.$extension")
            val temporary = directory.resolve(".$safeName.$extension.part")
            if (Files.isRegularFile(target) && Files.size(target) > 0L) {
                // If it exists as webm, transcode it to MP3 in background if needed
                if (extension != "mp3") {
                    val transcoded = DesktopAudioTranscoder.transcodeToMp3(target.toFile(), targetMp3.toFile(), song)
                    if (transcoded && Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) {
                        Files.deleteIfExists(target)
                        return Result.success(song.copy(localUri = targetMp3.toUri().toString(), localPath = targetMp3.toString()))
                    }
                }
                return Result.success(song.copy(localUri = target.toUri().toString(), localPath = target.toString()))
            }

            if (manifest) {
                // Segments are fetched and joined whole, so there is no partial file to resume.
                Files.deleteIfExists(temporary)
                Files.newOutputStream(temporary).buffered().use { output ->
                    DesktopManifestDownload.save(
                        stream,
                        extension,
                        output,
                        tempDir = java.io.File(System.getProperty("java.io.tmpdir")),
                        userAgent = DOWNLOAD_USER_AGENT,
                    ) { done, total -> onProgress(done, total) }
                }
                check(Files.size(temporary) > 0L) { "Download failed: nothing was sent" }
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
                val finalTarget = if (extension != "mp3") {
                    val transcoded = DesktopAudioTranscoder.transcodeToMp3(target.toFile(), targetMp3.toFile(), song)
                    if (transcoded && Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) {
                        Files.deleteIfExists(target)
                        targetMp3
                    } else target
                } else target
                return Result.success(song.copy(localUri = finalTarget.toUri().toString(), localPath = finalTarget.toString()))
            }

            val existing = if (Files.isRegularFile(temporary)) Files.size(temporary) else 0L
            val request = DesktopDownloadHttp.request(
                stream.url,
                DOWNLOAD_USER_AGENT,
                stream.headers,
                rangeFrom = existing.takeIf { it > 0L },
            )
            DesktopDownloadHttp.client.newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Download failed (HTTP ${response.code})" }
                val body = response.body
                val append = existing > 0L && response.code == HTTP_PARTIAL
                val offset = if (append) existing else 0L
                val total = response.header("Content-Range")
                    ?.substringAfterLast('/')
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?: body.contentLength().takeIf { it > 0L }?.let { it + offset }
                if (!append && existing > 0L) Files.deleteIfExists(temporary)
                Files.newOutputStream(
                    temporary,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    if (append) StandardOpenOption.APPEND else StandardOpenOption.TRUNCATE_EXISTING,
                ).buffered(BUFFER_SIZE).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var downloaded = offset
                        var lastReported = 0L
                        var lastReportedBytes = downloaded
                        onProgress(downloaded, total)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            if (now - lastReported >= 100L || (downloaded - lastReportedBytes) >= 128 * 1024) {
                                lastReported = now
                                lastReportedBytes = downloaded
                                onProgress(downloaded, total)
                            }
                        }
                        onProgress(downloaded, total)
                    }
                }
            }
            check(Files.size(temporary) > 0L) { "Download failed: nothing was sent" }
            runCatching {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }.getOrElse {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING)
            }

            // Convert webm / m4a to 320kbps MP3 with full metadata
            val finalTarget = if (extension != "mp3") {
                val transcoded = DesktopAudioTranscoder.transcodeToMp3(target.toFile(), targetMp3.toFile(), song)
                if (transcoded && Files.isRegularFile(targetMp3) && Files.size(targetMp3) > 0L) {
                    Files.deleteIfExists(target)
                    targetMp3
                } else {
                    target
                }
            } else {
                target
            }

            Result.success(song.copy(localUri = finalTarget.toUri().toString(), localPath = finalTarget.toString()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            Result.failure(failure)
        } finally {
            if (job != null) activeJobs.remove(song.videoId, job)
        }
    }

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
    fun savedFile(song: Song): Path? {
        val path = song.localPath?.let { runCatching { Path.of(it) }.getOrNull() }
            ?: song.localUri?.takeIf { it.startsWith("file:") }
                ?.let { runCatching { Path.of(URI(it)) }.getOrNull() }
        if (path != null && Files.isRegularFile(path) && runCatching { Files.size(path) > 0 }.getOrDefault(false)) {
            return path
        }
        val directory = Path.of(System.getProperty("user.home"), "Music", "MaviMusic")
        val safeName = buildString {
            append(song.artist)
            append(" - ")
            append(song.title)
        }.replace(ILLEGAL_FILENAME, "_").trim().take(180).ifBlank { song.videoId }
        val candidateMp3 = directory.resolve("$safeName.mp3")
        if (Files.isRegularFile(candidateMp3) && runCatching { Files.size(candidateMp3) > 0 }.getOrDefault(false)) {
            return candidateMp3
        }
        val legacyCandidate = Path.of(System.getProperty("user.home"), "Music", "BitChord").resolve("$safeName.mp3")
        if (Files.isRegularFile(legacyCandidate) && runCatching { Files.size(legacyCandidate) > 0 }.getOrDefault(false)) {
            return legacyCandidate
        }
        return null
    }

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

    private const val BUFFER_SIZE = 128 * 1024
    private const val HTTP_PARTIAL = 206
    private const val DOWNLOAD_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
    private val ILLEGAL_FILENAME = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")
}
