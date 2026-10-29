package com.music.mavimusic.android.data

import android.content.Context
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object OfflineDownloadManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadDir: File? = null

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val lock = Any()
    private val jobs = mutableMapOf<String, Job>()
    private val downloadSlots = Semaphore(2)
    private var clearing = false
    private val deleting = mutableSetOf<String>()
    private val downloader = VerifiedAudioDownloader(okHttpClient)
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val progress = _progress.asStateFlow()
    private val _errors = MutableStateFlow<Map<String, String>>(emptyMap())
    val errors = _errors.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<Song>>(emptyList())
    val downloadedSongs = _downloadedSongs.asStateFlow()

    private val _downloadingIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingIds = _downloadingIds.asStateFlow()

    fun init(context: Context) {
        val root = context.getExternalFilesDir(null) ?: context.filesDir
        val dir = File(root, "downloads")
        if (!dir.exists()) dir.mkdirs()
        synchronized(lock) {
            if (downloadDir == dir) return
            downloadDir = dir
            dir.listFiles { file -> file.name.endsWith(".tmp") }?.forEach { it.delete() }
        }
        refreshDownloadedSongs()
    }

    fun isDownloaded(videoId: String): Boolean = getLocalAudioFile(videoId) != null

    fun getLocalAudioFile(videoId: String): File? {
        val dir = downloadDir ?: return null
        val audio = File(dir, "$videoId.audio")
        val metadata = File(dir, "$videoId.json")
        if (!audio.isFile || audio.length() == 0L || !metadata.isFile) return null
        return try {
            val expected = JSONObject(metadata.readText()).optLong("sizeBytes", -1L)
            audio.takeIf { expected < 0 || it.length() == expected }
        } catch (_: Exception) { null }
    }

    fun getDownloadsSizeBytes(): Long {
        val dir = downloadDir ?: return 0L
        return dir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun clearAllDownloads() {
        val active = synchronized(lock) {
            if (clearing) return
            clearing = true
            jobs.values.toList().also { list -> list.forEach { it.cancel() } }
        }
        scope.launch {
            try {
                active.joinAll()
                downloadDir?.listFiles()?.forEach { it.delete() }
                _errors.value = emptyMap()
                refreshDownloadedSongs()
            } finally { synchronized(lock) { clearing = false } }
        }
    }

    fun refreshDownloadedSongs() {
        val dir = downloadDir ?: return
        val list = mutableListOf<Song>()
        val files = dir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        for (f in files) {
            try {
                val json = JSONObject(f.readText())
                val videoId = json.getString("videoId")
                val audioFile = getLocalAudioFile(videoId)
                if (audioFile != null) {
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
        if (!song.localPath.isNullOrBlank()) return
        synchronized(lock) {
            if (clearing || song.videoId in deleting || jobs.containsKey(song.videoId) || isDownloaded(song.videoId)) return
            _errors.update { it - song.videoId }
            _progress.update { it + (song.videoId to 0f) }
            val job = scope.launch(start = CoroutineStart.LAZY) {
                val temp = File(dir, "${song.videoId}.audio.tmp")
                val metadataTemp = File(dir, "${song.videoId}.json.tmp")
                val audio = File(dir, "${song.videoId}.audio")
                var promoted = false
                try {
                    downloadSlots.withPermit {
                        val url = StreamResolver.resolve(song.videoId)
                        val headers = StreamResolver.mediaHeadersFor(url)
                        downloader.download(url, headers, temp) { fraction ->
                            _progress.update { it + (song.videoId to fraction) }
                        }
                        coroutineContext.ensureActive()
                        val metadata = JSONObject().apply {
                            put("videoId", song.videoId)
                            put("title", song.title)
                            put("artist", song.artist)
                            put("thumbnailUrl", song.thumbnailUrl ?: "")
                            put("durationText", song.durationText ?: "")
                            put("albumName", song.albumName ?: "")
                            put("sizeBytes", temp.length())
                        }
                        metadataTemp.writeText(metadata.toString())
                        synchronized(lock) {
                            coroutineContext.ensureActive()
                            check(temp.renameTo(audio)) { "No se pudo guardar el audio" }
                            promoted = true
                            check(metadataTemp.renameTo(File(dir, "${song.videoId}.json"))) { "No se pudieron guardar los metadatos" }
                        }
                        refreshDownloadedSongs()
                    }
                } catch (cancelled: CancellationException) {
                    if (promoted) audio.delete()
                    throw cancelled
                } catch (_: Exception) {
                    if (promoted) audio.delete()
                    _errors.update { it + (song.videoId to "No se pudo completar la descarga. Toca descargar para reintentar.") }
                } finally {
                    temp.delete()
                    metadataTemp.delete()
                    synchronized(lock) {
                        jobs.remove(song.videoId)
                        _downloadingIds.value = jobs.keys.toSet()
                        _progress.update { it - song.videoId }
                    }
                }
            }
            jobs[song.videoId] = job
            _downloadingIds.value = jobs.keys.toSet()
            job.start()
        }
    }

    fun cancelDownload(videoId: String) {
        synchronized(lock) { jobs[videoId]?.cancel() }
    }

    fun deleteDownload(videoId: String) {
        val dir = downloadDir ?: return
        val active = synchronized(lock) {
            if (!deleting.add(videoId)) return
            jobs[videoId]?.also { it.cancel() }
        }
        scope.launch {
            try {
                active?.join()
                File(dir, "$videoId.audio").delete()
                File(dir, "$videoId.json").delete()
                _errors.update { it - videoId }
                refreshDownloadedSongs()
            } finally { synchronized(lock) { deleting.remove(videoId) } }
        }
    }
}
