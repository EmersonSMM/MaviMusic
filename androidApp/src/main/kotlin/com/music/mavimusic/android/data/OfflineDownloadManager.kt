package com.music.mavimusic.android.data

import android.content.Context
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

object OfflineDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadDir: File? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val CHUNK_SIZE = 2 * 1024 * 1024L // 2MB parallel blocks to bypass YouTube rate-limiting

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    fun init(context: Context) {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(root, "downloads")
        if (!dir.exists()) dir.mkdirs()
        downloadDir = dir
        refreshDownloadedSongs()
    }

    fun isDownloaded(videoId: String): Boolean {
        val dir = downloadDir ?: return false
        val audioFile = File(dir, "$videoId.audio")
        return audioFile.exists() && audioFile.length() > 0L
    }

    fun getLocalAudioFile(videoId: String): File? {
        val dir = downloadDir ?: return null
        val audioFile = File(dir, "$videoId.audio")
        return if (audioFile.exists() && audioFile.length() > 0L) audioFile else null
    }

    fun getDownloadsSizeBytes(): Long {
        val dir = downloadDir ?: return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearAllDownloads() {
        val dir = downloadDir ?: return
        dir.listFiles()?.forEach { it.delete() }
        refreshDownloadedSongs()
    }

    fun refreshDownloadedSongs() {
        val dir = downloadDir ?: return
        val list = mutableListOf<Song>()
        val files = dir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        for (f in files) {
            try {
                val json = JSONObject(f.readText())
                val videoId = json.getString("videoId")
                val audioFile = File(dir, "$videoId.audio")
                if (audioFile.exists() && audioFile.length() > 0L) {
                    list.add(
                        Song(
                            videoId = videoId,
                            title = json.getString("title"),
                            artist = json.getString("artist"),
                            thumbnailUrl = json.optString("thumbnailUrl").takeIf { it.isNotBlank() },
                            durationText = json.optString("durationText").takeIf { it.isNotBlank() },
                            albumName = json.optString("albumName").takeIf { it.isNotBlank() },
                            localPath = audioFile.absolutePath
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _downloadedSongs.value = list
    }

    fun downloadSong(song: Song) {
        val dir = downloadDir ?: return
        if (isDownloaded(song.videoId)) return

        _downloadingIds.value = _downloadingIds.value + song.videoId

        scope.launch {
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    StreamResolver.resolve(song.videoId)
                }
                val headers = withContext(Dispatchers.IO) {
                    StreamResolver.mediaHeadersFor(streamUrl)
                }

                val tempFile = File(dir, "${song.videoId}.tmp")
                val finalAudioFile = File(dir, "${song.videoId}.audio")
                val metaFile = File(dir, "${song.videoId}.json")

                if (tempFile.exists()) tempFile.delete()

                // 1. Probe total content length using a probe range request (0-1)
                val probeReq = Request.Builder()
                    .url(streamUrl)
                    .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                    .addHeader("Range", "bytes=0-1")
                    .build()

                var totalBytes = -1L
                okHttpClient.newCall(probeReq).execute().use { response ->
                    val cr = response.header("Content-Range")
                    if (cr != null && cr.contains("/")) {
                        totalBytes = cr.substringAfterLast("/").toLongOrNull() ?: -1L
                    }
                    if (totalBytes <= 0L) {
                        totalBytes = response.header("Content-Length")?.toLongOrNull() ?: -1L
                    }
                }

                if (totalBytes > 0L) {
                    // 2. High-speed multi-chunk download with 2MB blocks
                    val raf = RandomAccessFile(tempFile, "rw")
                    raf.setLength(totalBytes)

                    val chunks = mutableListOf<Pair<Long, Long>>()
                    var offset = 0L
                    while (offset < totalBytes) {
                        val end = minOf(offset + CHUNK_SIZE - 1, totalBytes - 1)
                        chunks.add(Pair(offset, end))
                        offset = end + 1
                    }

                    // Process up to 3 chunks in parallel
                    val chunkBatches = chunks.chunked(3)
                    for (batch in chunkBatches) {
                        val downloadedBlocks = withContext(Dispatchers.IO) {
                            batch.map { (start, end) ->
                                async {
                                    val req = Request.Builder()
                                        .url(streamUrl)
                                        .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                                        .addHeader("Range", "bytes=$start-$end")
                                        .build()

                                    okHttpClient.newCall(req).execute().use { resp ->
                                        if (resp.isSuccessful) {
                                            Pair(start, resp.body?.bytes() ?: ByteArray(0))
                                        } else {
                                            throw Exception("Chunk HTTP ${resp.code}")
                                        }
                                    }
                                }
                            }.awaitAll()
                        }

                        synchronized(raf) {
                            for ((start, bytes) in downloadedBlocks) {
                                raf.seek(start)
                                raf.write(bytes)
                            }
                        }
                    }
                    raf.close()
                } else {
                    // Fallback to streaming if server didn't specify total range
                    val fallbackReq = Request.Builder()
                        .url(streamUrl)
                        .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                        .build()

                    okHttpClient.newCall(fallbackReq).execute().use { response ->
                        if (response.isSuccessful) {
                            response.body?.byteStream()?.use { input ->
                                tempFile.outputStream().use { output ->
                                    input.copyTo(output, bufferSize = 64 * 1024)
                                }
                            }
                        }
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0L) {
                    tempFile.renameTo(finalAudioFile)

                    // Save metadata
                    val metaJson = JSONObject().apply {
                        put("videoId", song.videoId)
                        put("title", song.title)
                        put("artist", song.artist)
                        put("thumbnailUrl", song.thumbnailUrl ?: "")
                        put("durationText", song.durationText ?: "")
                        put("albumName", song.albumName ?: "")
                    }
                    metaFile.writeText(metaJson.toString())
                    refreshDownloadedSongs()
                }
            } catch (_: Exception) {
            } finally {
                _downloadingIds.value = _downloadingIds.value - song.videoId
            }
        }
    }

    fun deleteDownload(videoId: String) {
        val dir = downloadDir ?: return
        val audioFile = File(dir, "$videoId.audio")
        val metaFile = File(dir, "$videoId.json")
        if (audioFile.exists()) audioFile.delete()
        if (metaFile.exists()) metaFile.delete()
        refreshDownloadedSongs()
    }
}
