package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bytedeco.javacpp.Loader
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

/**
 * Transcodes downloaded audio (webm, opus, m4a, flac) to high-quality 320 kbps MP3
 * with embedded ID3 metadata tags (Title, Artist, Album) using the bundled FFmpeg binary.
 */
internal object DesktopAudioTranscoder {

    private val ffmpegBinary: String? by lazy {
        val candidates = listOf(
            "/opt/homebrew/bin/ffmpeg",
            "/usr/local/bin/ffmpeg",
            "/usr/bin/ffmpeg",
        )
        for (candidate in candidates) {
            val f = File(candidate)
            if (f.isFile && f.canExecute()) return@lazy candidate
        }
        // Try extracting and caching the bundled FFmpeg binary from javacpp-ffmpeg
        val resourcePath = "org/bytedeco/ffmpeg/" + Loader.getPlatform() + "/ffmpeg"
        val cached = runCatching { Loader.cacheResource(resourcePath) }.getOrNull()
        if (cached != null && cached.isFile) {
            cached.setExecutable(true)
            if (cached.canExecute()) return@lazy cached.absolutePath
        }

        // Search in ~/.javacpp/cache if previously extracted
        runCatching {
            val cacheDir = Loader.getCacheDir()
            cacheDir?.walkTopDown()?.firstOrNull { it.isFile && it.name == "ffmpeg" && it.canExecute() }?.absolutePath
        }.getOrNull() ?: runCatching {
            val proc = ProcessBuilder("which", "ffmpeg").start()
            val path = proc.inputStream.bufferedReader().readLine()?.trim()
            if (!path.isNullOrBlank() && File(path).canExecute()) path else null
        }.getOrNull()
    }

    /**
     * Converts [sourceFile] into an MP3 at [targetMp3File] with 320 kbps CBR and ID3 tags.
     * Returns true if transcoding succeeded, false otherwise.
     */
    suspend fun transcodeToMp3(
        sourceFile: File,
        targetMp3File: File,
        song: Song,
    ): Boolean = withContext(Dispatchers.IO) {
        val binary = ffmpegBinary ?: return@withContext false
        if (!sourceFile.isFile || sourceFile.length() == 0L) return@withContext false

        val tempMp3 = File(targetMp3File.parentFile, ".${targetMp3File.name}.transcoding.tmp")
        try {
            tempMp3.delete()
            val cmd = mutableListOf(
                binary,
                "-y",
                "-i", sourceFile.absolutePath,
                "-vn",
                "-c:a", "libmp3lame",
                "-b:a", "320k",
                "-id3v2_version", "3",
                "-metadata", "title=${song.title}",
                "-metadata", "artist=${song.artist}",
            )
            if (!song.albumName.isNullOrBlank()) {
                cmd.add("-metadata")
                cmd.add("album=${song.albumName}")
            }
            cmd.add(tempMp3.absolutePath)

            val process = ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start()

            val finished = process.waitFor(120, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                DesktopTrackLog.log("FFmpeg transcode timed out for '${song.title}'")
                return@withContext false
            }

            if (process.exitValue() == 0 && tempMp3.isFile && tempMp3.length() > 0L) {
                targetMp3File.delete()
                if (tempMp3.renameTo(targetMp3File)) {
                    DesktopTrackLog.log("Transcoded '${song.title}' to 320kbps MP3 (${targetMp3File.length()} bytes)")
                    return@withContext true
                }
            } else {
                DesktopTrackLog.log("FFmpeg transcode exited with code ${process.exitValue()}")
            }
            false
        } catch (e: Exception) {
            DesktopTrackLog.log("FFmpeg transcode error: ${e.message}")
            false
        } finally {
            tempMp3.delete()
        }
    }
}
