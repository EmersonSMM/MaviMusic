package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.Loader
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.TimeUnit

/** Converts locally downloaded audio with the FFmpeg executable and libraries shipped in the app. */
internal object DesktopAudioTranscoder {
    internal val bundledBinary: String by lazy {
        // Loader extracts the executable AND its dependent dylibs into the same directory.
        // Extracting only the executable left fresh installations unable to start it.
        Loader.load(org.bytedeco.ffmpeg.ffmpeg::class.java)
            ?: error("The bundled FFmpeg executable is missing")
    }

    private val ffmpegBinary: String by lazy {
        runCatching { bundledBinary }.getOrElse {
            listOf("/opt/homebrew/bin/ffmpeg", "/usr/local/bin/ffmpeg", "/usr/bin/ffmpeg")
                .firstOrNull { path -> File(path).let { it.isFile && it.canExecute() } }
                ?: error("FFmpeg is not available. Reinstall MaviMusic to restore the bundled converter")
        }
    }

    suspend fun transcodeToMp3(
        sourceFile: File, targetMp3File: File, song: Song,
        artwork: File? = null,
        onStage: (DesktopDownloadStage) -> Unit = {},
    ): Boolean =
        withContext(Dispatchers.IO) {
            require(sourceFile.isFile && sourceFile.length() > 0L) { "The downloaded audio is empty" }
            val directory = targetMp3File.absoluteFile.parentFile.toPath()
            Files.createDirectories(directory)
            val temporary = Files.createTempFile(directory, ".mavimusic-convert-", ".mp3")
            val diagnostic = Files.createTempFile(directory, ".mavimusic-ffmpeg-", ".log")
            val coverDirectory = Files.createTempDirectory(directory, ".mavimusic-artwork-")
            var process: Process? = null
            try {
                val command = mutableListOf(
                    ffmpegBinary, "-hide_banner", "-loglevel", "error", "-nostdin", "-y",
                    "-i", sourceFile.absolutePath,
                )
                onStage(DesktopDownloadStage.ARTWORK)
                val cover = artwork?.takeIf { it.isFile && it.length() > 0 }
                    ?: DesktopExportArtwork.resolve(song, coverDirectory)
                if (cover != null) command += listOf("-i", cover.absolutePath)
                command += listOf("-map", "0:a:0", "-map_metadata", "0", "-id3v2_version", "3")
                // Updating tags on an existing MP3 must not encode its audio a second time.
                command += if (sourceFile.extension.equals("mp3", ignoreCase = true)) listOf("-c:a", "copy")
                    else listOf("-c:a", "libmp3lame", "-b:a", "320k")
                if (song.title.isNotBlank()) command += listOf("-metadata", "title=${song.title}")
                if (song.artist.isNotBlank()) command += listOf("-metadata", "artist=${song.artist}")
                song.trackNumber?.let { command += listOf("-metadata", "track=$it") }
                command += listOf("-map", if (cover == null) "0:v:0?" else "1:v:0", "-c:v", "mjpeg",
                    "-disposition:v", "attached_pic", "-metadata:s:v", "title=Album cover",
                    "-metadata:s:v", "comment=Cover (front)")
                song.albumName?.takeIf(String::isNotBlank)?.let { command += listOf("-metadata", "album=$it") }
                // Explicit muxer: the output format must not depend on a temporary suffix.
                command += listOf("-f", "mp3", temporary.toString())
                onStage(DesktopDownloadStage.CONVERTING)
                process = ProcessBuilder(command).redirectErrorStream(true)
                    .redirectOutput(diagnostic.toFile()).start()
                val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(10)
                while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
                    currentCoroutineContext().ensureActive()
                    check(System.nanoTime() < deadline) { "MP3 conversion timed out" }
                    delay(10)
                }
                currentCoroutineContext().ensureActive()
                val error = Files.newBufferedReader(diagnostic).use { it.readText().takeLast(2048) }
                check(process.exitValue() == 0 && Files.size(temporary) > 0) {
                    "MP3 conversion failed (${process.exitValue()}): $error"
                }
                onStage(DesktopDownloadStage.VERIFYING)
                val exported = DesktopExportProbe.inspect(temporary.toFile())
                check(exported.hasAudio) { "El MP3 no contiene audio" }
                check(song.thumbnailUrl.isNullOrBlank() || exported.hasCover) {
                    "No se pudo incrustar la carátula. El audio original se conserva; vuelve a intentar."
                }
                val expectedTags = buildMap {
                    song.title.takeIf(String::isNotBlank)?.let { put("title", it) }
                    song.artist.takeIf(String::isNotBlank)?.let { put("artist", it) }
                    song.albumName?.takeIf(String::isNotBlank)?.let { put("album", it) }
                }
                check(expectedTags.all { (key, value) -> exported.tags[key] == value }) {
                    "La verificación de los metadatos del MP3 falló; se conserva el original"
                }
                val sourceDuration = DesktopExportProbe.inspect(sourceFile).duration
                if (sourceDuration != null && exported.duration != null) {
                    check(kotlin.math.abs(sourceDuration - exported.duration) <= maxOf(0.25, sourceDuration * 0.01)) {
                        "El MP3 quedó incompleto; se conserva el audio original"
                    }
                }
                try {
                    Files.move(temporary, targetMp3File.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(temporary, targetMp3File.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                true
            } finally {
                process?.takeIf { it.isAlive }?.destroyForcibly()?.waitFor(5, TimeUnit.SECONDS)
                Files.deleteIfExists(temporary)
                Files.deleteIfExists(diagnostic)
                coverDirectory.toFile().deleteRecursively()
            }
        }
}
