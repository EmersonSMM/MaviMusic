package com.music.mavimusic.android.data

import android.content.Context
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object OfflineDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadDir: File? = null

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
        val dir = downloadDir ?: return false.let { null }
        val audioFile = File(dir, "$videoId.audio")
        return if (audioFile.exists() && audioFile.length() > 0L) audioFile else null
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

                val url = URL(streamUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                conn.connect()

                if (conn.responseCode in 200..299) {
                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
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
