package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

/**
 * Predictive background buffer for upcoming queue tracks (Phase 1, Item 1).
 *
 * Pre-buffers audio for the next 2-3 tracks in the queue into a bounded LRU disk cache
 * using background I/O. When the user skips ahead or the song changes, playback starts
 * immediately from disk in 0 ms without network roundtrips or stream resolution stalls.
 */
internal object DesktopPredictiveCache {

    const val LOOKAHEAD_COUNT = 3
    const val MAX_CACHE_BYTES = 150 * 1024 * 1024L // 150 MB bounded cache
    private const val MIN_AUDIO_BYTES = 64 * 1024L // 64 KB minimum valid stream

    val directory: Path by lazy {
        val state = System.getenv("XDG_CACHE_HOME")?.takeIf(String::isNotBlank)
            ?: "${System.getProperty("user.home")}/.cache"
        Path.of(state, "mavimusic", "predictive_buffer").also {
            runCatching { Files.createDirectories(it) }
        }
    }

    private val activeFetches = ConcurrentHashMap.newKeySet<String>()
    private val bufferLock = Mutex()

    fun isEnabled(): Boolean = DesktopPersistence.predictiveBufferEnabled()

    /**
     * Checks if [song] has been pre-buffered locally.
     * Touches file access time to update LRU rank on hit.
     */
    fun cachedFile(song: Song): Path? {
        if (!isEnabled()) return null
        if (song.videoId.isBlank() || song.videoId.startsWith("local:")) return null
        val target = directory.resolve(sanitizeVideoId(song.videoId) + ".audio")
        return if (runCatching {
                Files.isRegularFile(target) && Files.size(target) >= MIN_AUDIO_BYTES
            }.getOrDefault(false)
        ) {
            runCatching {
                val now = java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis())
                Files.setLastModifiedTime(target, now)
            }
            target
        } else {
            null
        }
    }

    /**
     * Returns a [DesktopStream] pointing to the local pre-buffered file if present.
     */
    fun cachedStream(song: Song): DesktopStream? = cachedFile(song)?.let {
        DesktopStream(it.toAbsolutePath().toString())
    }

    /**
     * Buffers the next 2-3 upcoming tracks in the background.
     * Controlled by a mutex to ensure at most one track is buffered at a time,
     * preventing network saturation and CPU load spikes.
     */
    suspend fun bufferUpcoming(upcoming: List<Song>, quality: String? = null) {
        if (!isEnabled() || upcoming.isEmpty()) return
        val candidates = upcoming
            .asSequence()
            .filter { it.videoId.isNotBlank() && !it.videoId.startsWith("local:") && it.localPath == null }
            .filter { DesktopDownloadManager.savedFile(it) == null }
            .filter { cachedFile(it) == null }
            .distinctBy { it.videoId }
            .take(LOOKAHEAD_COUNT)
            .toList()

        if (candidates.isEmpty()) return

        withContext(Dispatchers.IO) {
            for (song in candidates) {
                if (!isEnabled()) break
                if (cachedFile(song) != null) continue
                if (!activeFetches.add(song.videoId)) continue

                try {
                    bufferLock.withLock {
                        if (!isEnabled() || cachedFile(song) != null) return@withLock
                        bufferSingleTrack(song, quality)
                    }
                } finally {
                    activeFetches.remove(song.videoId)
                }
            }
        }
    }

    private suspend fun bufferSingleTrack(song: Song, quality: String?) {
        val streamResult = DesktopMusicSources.resolve(song, quality, forDownload = false)
        val stream = streamResult.getOrNull() ?: return
        if (stream.url.isBlank() || stream.url.startsWith("file:")) return

        val cleanId = sanitizeVideoId(song.videoId)
        val tmpFile = directory.resolve("$cleanId.tmp")
        val finalFile = directory.resolve("$cleanId.audio")

        runCatching {
            val userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"
            val request = DesktopDownloadHttp.request(stream.url, userAgent, stream.headers)
            DesktopDownloadHttp.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use
                val body = response.body
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile.toFile()).use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
            }

            if (Files.exists(tmpFile) && Files.size(tmpFile) >= MIN_AUDIO_BYTES) {
                Files.move(tmpFile, finalFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                DesktopTrackLog.log("[PredictiveBuffer] Successfully buffered '${song.title}' (${song.videoId})")
                trim()
            } else {
                Files.deleteIfExists(tmpFile)
            }
        }.onFailure {
            runCatching { Files.deleteIfExists(tmpFile) }
        }
    }

    /**
     * Bounded LRU trim: removes oldest buffered files when cache exceeds [maxAllowedBytes].
     */
    fun trim(maxAllowedBytes: Long = MAX_CACHE_BYTES) {
        runCatching {
            val files = Files.list(directory).use { it.filter(Files::isRegularFile).toList() }
            var totalSize = files.sumOf { runCatching { Files.size(it) }.getOrDefault(0L) }
            if (totalSize <= maxAllowedBytes) return

            files.sortedBy { path ->
                runCatching {
                    Files.readAttributes(path, BasicFileAttributes::class.java).lastModifiedTime().toMillis()
                }.getOrDefault(0L)
            }.forEach { path ->
                if (totalSize <= maxAllowedBytes) return
                val size = runCatching { Files.size(path) }.getOrDefault(0L)
                if (runCatching { Files.deleteIfExists(path) }.getOrDefault(false)) {
                    totalSize -= size
                }
            }
        }
    }

    /**
     * Clears all predictive buffer files from disk.
     */
    fun clear(): Long = runCatching {
        var freed = 0L
        Files.list(directory).use { stream ->
            stream.filter(Files::isRegularFile).forEach { path ->
                val size = runCatching { Files.size(path) }.getOrDefault(0L)
                if (runCatching { Files.deleteIfExists(path) }.getOrDefault(false)) {
                    freed += size
                }
            }
        }
        DesktopTrackLog.log("[PredictiveBuffer] Cleared cache ($freed bytes freed)")
        freed
    }.getOrDefault(0L)

    private fun sanitizeVideoId(id: String): String =
        id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
}
