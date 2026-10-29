package com.music.mavimusic.android.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.durationMillis
import com.music.mavimusic.playback.PlaybackPosition
import com.music.mavimusic.ui.player.RepeatModes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android native playback engine powered by ExoPlayer.
 *
 * Bridges MaviMusic queue and stream resolution with AndroidX Media3
 * background playback, lockscreen and notification controls.
 */
object AndroidPlayerEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var preferences: com.music.mavimusic.android.data.AndroidPreferences? = null

    private var player: ExoPlayer? = null
    private var tickerJob: Job? = null
    private var resolutionJob: Job? = null
    private var lyricsJob: Job? = null
    @Volatile private var loadGeneration = 0L
    private val queueOrder = AndroidQueueOrder()
    private var playWhenAttached = false
    private var shouldPlayWhenReady = false
    private var lastCheckpointNs = 0L

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs = _durationMs.asStateFlow()

    val position = PlaybackPosition()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue = _queue.asStateFlow()

    private val _queueIndex = MutableStateFlow(0)
    val queueIndex = _queueIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatModes.OFF)
    val repeatMode = _repeatMode.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled = _shuffleEnabled.asStateFlow()

    private val _autoplayEnabled = MutableStateFlow(true)
    val autoplayEnabled = _autoplayEnabled.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite = _isFavorite.asStateFlow()

    private val _lyrics = MutableStateFlow<List<com.music.mavimusic.data.lyrics.LyricLine>?>(null)
    val lyrics = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading = _lyricsLoading.asStateFlow()

    private val _lyricsSource = MutableStateFlow<com.music.mavimusic.data.lyrics.LyricsSource?>(null)
    val lyricsSource = _lyricsSource.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            generateSequence<Throwable>(error) { it.cause }
                .filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()?.let { refused ->
                    StreamResolver.onPlaybackRefused(refused.dataSpec.uri.toString(), refused.responseCode)
                }
            _isLoading.value = false
            _error.value = "No se pudo reproducir la canción. Comprueba la conexión o el archivo local."
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                startTicker()
            } else {
                tickerJob?.cancel()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    _isLoading.value = true
                }
                Player.STATE_READY -> {
                    _isLoading.value = false
                    val dur = player?.duration ?: 0L
                    _durationMs.value = if (dur > 0) dur else 0L
                }
                Player.STATE_ENDED -> {
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }
    }

    fun init(context: Context) {
        val prefs = com.music.mavimusic.android.data.AndroidPreferences(context)
        preferences = prefs
        StreamResolver.maxKbps = { if (prefs.getDataSaver()) 128 else Int.MAX_VALUE }
        com.music.mavimusic.android.data.OfflineDownloadManager.init(context)

        // Restore last session if available
        if (_currentSong.value == null) {
            _shuffleEnabled.value = prefs.getShuffle()
            _repeatMode.value = prefs.getRepeatMode()
            _autoplayEnabled.value = prefs.getAutoplay()
            prefs.getLastSong()?.let { last ->
                _currentSong.value = last
                _isFavorite.value = prefs.isFavorite(last.videoId)
                position.report(prefs.getPlaybackPosition(last.videoId))
                _durationMs.value = last.durationText.durationMillis()
            }
            prefs.getQueue()?.let { (q, idx) ->
                _queue.value = q
                _queueIndex.value = idx
                queueOrder.reset(q.size, idx, _shuffleEnabled.value)
            }
            if (_queue.value.isEmpty()) {
                _currentSong.value?.let { _queue.value = listOf(it); queueOrder.reset(1, 0, _shuffleEnabled.value) }
            }
            _currentSong.value?.let { restored ->
                val index = _queue.value.indexOfFirst { it.videoId == restored.videoId }
                if (index >= 0) _queueIndex.value = index
                else {
                    _queue.value = _queue.value + restored
                    _queueIndex.value = _queue.value.lastIndex
                }
                queueOrder.reset(_queue.value.size, _queueIndex.value, _shuffleEnabled.value)
            }
        }
    }

    fun attachPlayer(exoPlayer: ExoPlayer) {
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        _isPlaying.value = exoPlayer.isPlaying
        if (exoPlayer.isPlaying) {
            startTicker()
        }
        if (playWhenAttached) {
            playWhenAttached = false
            play()
        }
    }

    fun detachPlayer() {
        checkpointPosition()
        resolutionJob?.cancel()
        resolutionJob = null
        lyricsJob?.cancel()
        loadGeneration++
        tickerJob?.cancel()
        player?.removeListener(playerListener)
        player = null
        _isPlaying.value = false
        _isLoading.value = false
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val currentP = player?.currentPosition ?: 0L
                position.report(currentP, System.nanoTime())
                if (System.nanoTime() - lastCheckpointNs >= 5_000_000_000L) checkpointPosition()
                delay(200)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val targetIndex = startIndex.coerceIn(0, songs.size - 1)
        _queueIndex.value = targetIndex
        queueOrder.reset(songs.size, targetIndex, _shuffleEnabled.value)
        loadAndPlaySong(songs[targetIndex])
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun togglePlayPause() {
        if (player?.isPlaying == true || (_isLoading.value && shouldPlayWhenReady)) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        shouldPlayWhenReady = true
        val p = player
        if (p == null) {
            playWhenAttached = true
            return
        }
        if (p.currentMediaItem == null || p.playbackState == Player.STATE_IDLE) {
            if (resolutionJob?.isActive == true) return
            _currentSong.value?.let { loadAndPlaySong(it, position.positionMs) }
        } else {
            if (p.playbackState == Player.STATE_ENDED) seekTo(0L)
            p.play()
        }
    }

    fun pause() {
        playWhenAttached = false
        shouldPlayWhenReady = false
        player?.pause()
        checkpointPosition()
    }

    fun seekTo(positionMs: Long) {
        position.seeks++
        position.report(positionMs)
        player?.seekTo(positionMs)
        _currentSong.value?.let { preferences?.savePlaybackPosition(it.videoId, positionMs) }
    }

    private fun checkpointPosition() {
        val song = _currentSong.value ?: return
        val p = player
        val at = if (p?.currentMediaItem?.mediaId == song.videoId) p.currentPosition else position.positionMs
        preferences?.savePlaybackPosition(song.videoId, at)
        lastCheckpointNs = System.nanoTime()
    }

    fun seekFraction(fraction: Float) {
        val total = _durationMs.value
        if (total > 0L) {
            val target = (total * fraction.coerceIn(0f, 1f)).toLong()
            seekTo(target)
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return
        val current = _queueIndex.value
        val nextIndex = queueOrder.next(_repeatMode.value == RepeatModes.ALL)
        if (nextIndex != null) {
            _queueIndex.value = nextIndex
            loadAndPlaySong(q[nextIndex])
        } else if (_autoplayEnabled.value) {
            val lastSong = q[current]
            fetchRadioAndAppend(lastSong)
        }
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return
        val currentP = player?.currentPosition ?: 0L
        if (currentP > 3000L) {
            seekTo(0L)
            return
        }
        val prevIndex = queueOrder.previous()
        if (prevIndex != null) {
            _queueIndex.value = prevIndex
            loadAndPlaySong(q[prevIndex])
        } else {
            seekTo(0L)
        }
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatModes.OFF -> RepeatModes.ALL
            RepeatModes.ALL -> RepeatModes.ONE
            else -> RepeatModes.OFF
        }
        persistPlaybackModes()
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
        queueOrder.reset(_queue.value.size, _queueIndex.value, _shuffleEnabled.value)
        persistPlaybackModes()
    }

    fun toggleAutoplay() {
        _autoplayEnabled.value = !_autoplayEnabled.value
        persistPlaybackModes()
    }

    private fun persistPlaybackModes() {
        preferences?.savePlaybackModes(_shuffleEnabled.value, _repeatMode.value, _autoplayEnabled.value)
    }

    fun toggleCurrentFavorite(): Boolean {
        val s = _currentSong.value ?: return false
        val prefs = preferences ?: return false
        val nowFav = prefs.toggleFavorite(s)
        _isFavorite.value = nowFav
        return nowFav
    }

    fun startRadio(song: Song) {
        scope.launch {
            _isLoading.value = true
            try {
                val res = withContext(Dispatchers.IO) {
                    com.music.mavimusic.data.YtMusicRepository.radio(song.videoId)
                }
                res.onSuccess { radioTracks ->
                    val newQueue = listOf(song) + radioTracks.filter { it.videoId != song.videoId }.distinctBy { it.videoId }
                    playQueue(newQueue, 0)
                }.onFailure {
                    playSong(song)
                }
            } catch (_: Exception) {
                playSong(song)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun fetchRadioAndAppend(song: Song) {
        val requestedQueue = _queue.value
        scope.launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    com.music.mavimusic.data.YtMusicRepository.radio(song.videoId)
                }
                res.onSuccess { radioTracks ->
                    if (_queue.value !== requestedQueue || _currentSong.value?.videoId != song.videoId) return@onSuccess
                    val existingIds = _queue.value.map { it.videoId }.toSet()
                    val toAdd = radioTracks.filter { it.videoId !in existingIds }.distinctBy { it.videoId }
                    if (toAdd.isNotEmpty()) {
                        val newQueue = _queue.value + toAdd
                        _queue.value = newQueue
                        queueOrder.append(newQueue.size)
                        val nextIndex = queueOrder.next(repeatAll = false)
                        if (nextIndex != null) {
                            _queueIndex.value = nextIndex
                            loadAndPlaySong(newQueue[nextIndex])
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun handleTrackEnded() {
        if (!SleepTimer.onTrackEnded()) {
            when (_repeatMode.value) {
                RepeatModes.ONE -> {
                    seekTo(0L)
                    play()
                }
                else -> skipNext()
            }
        }
    }

    private fun loadAndPlaySong(song: Song, startPositionMs: Long = 0L) {
        val generation = ++loadGeneration
        lyricsJob?.cancel()
        checkpointPosition()
        player?.pause()
        playWhenAttached = player == null
        shouldPlayWhenReady = true
        _currentSong.value = song
        _isFavorite.value = preferences?.isFavorite(song.videoId) ?: false
        _isLoading.value = true
        _error.value = null
        _durationMs.value = song.durationText.durationMillis()
        _lyrics.value = null
        _lyricsSource.value = null
        _lyricsLoading.value = true
        position.seeks++
        position.report(startPositionMs)
        preferences?.savePlaybackPosition(song.videoId, startPositionMs)

        // Asynchronously fetch synchronized lyrics with song duration
        lyricsJob = scope.launch(Dispatchers.IO) {
            try {
                val dur = song.durationText.durationMillis()
                var found = com.music.mavimusic.data.lyrics.LyricsRepository.lyrics(
                    videoId = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    durationMs = dur,
                    album = song.albumName
                )
                if (found == null && dur > 0L) {
                    found = com.music.mavimusic.data.lyrics.LyricsRepository.lyrics(
                        videoId = song.videoId,
                        title = song.title,
                        artist = song.artist,
                        durationMs = 0L,
                        album = song.albumName
                    )
                }
                if (found != null && generation == loadGeneration) {
                    _lyrics.value = found.lines
                    _lyricsSource.value = found.source
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
            } finally {
                if (generation == loadGeneration) _lyricsLoading.value = false
            }
        }

        resolutionJob?.cancel()
        resolutionJob = scope.launch {
            try {
                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                    .build()

                // Check if song has direct local path (e.g. MediaStore or offline file)
                if (!song.localPath.isNullOrBlank()) {
                    val localUri = if (song.localPath!!.startsWith("content://") || song.localPath!!.startsWith("file://")) {
                        Uri.parse(song.localPath)
                    } else {
                        Uri.fromFile(java.io.File(song.localPath!!))
                    }
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.videoId)
                        .setUri(localUri)
                        .setMediaMetadata(metadata)
                        .build()

                    val p = player ?: return@launch
                    p.setMediaItem(mediaItem, startPositionMs)
                    p.prepare()
                    p.playWhenReady = shouldPlayWhenReady

                    preferences?.saveLastSong(song)
                    preferences?.saveQueue(_queue.value, _queueIndex.value)
                    return@launch
                }

                // Check if the song was downloaded offline
                val localFile = com.music.mavimusic.android.data.OfflineDownloadManager.getLocalAudioFile(song.videoId)
                if (localFile != null && localFile.exists()) {
                    val mediaItem = MediaItem.Builder()
                        .setMediaId(song.videoId)
                        .setUri(Uri.fromFile(localFile))
                        .setMediaMetadata(metadata)
                        .build()

                    val p = player ?: return@launch
                    p.setMediaItem(mediaItem, startPositionMs)
                    p.prepare()
                    p.playWhenReady = shouldPlayWhenReady

                    preferences?.saveLastSong(song)
                    preferences?.saveQueue(_queue.value, _queueIndex.value)
                    return@launch
                }

                val streamUrl = withContext(Dispatchers.IO) {
                    StreamResolver.resolve(song.videoId)
                }

                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.videoId)
                    .setUri(Uri.parse(streamUrl))
                    .setMediaMetadata(metadata)
                    .build()

                val headers = withContext(Dispatchers.IO) {
                    StreamResolver.mediaHeadersFor(streamUrl)
                }

                val httpSourceFactory = DefaultHttpDataSource.Factory()
                    .setUserAgent(headers["User-Agent"] ?: "Mozilla/5.0")
                    .setDefaultRequestProperties(headers)

                val mediaSource = ProgressiveMediaSource.Factory(httpSourceFactory)
                    .createMediaSource(mediaItem)

                val p = player ?: return@launch
                p.setMediaSource(mediaSource, startPositionMs)
                p.prepare()
                p.playWhenReady = shouldPlayWhenReady

                preferences?.saveLastSong(song)
                preferences?.saveQueue(_queue.value, _queueIndex.value)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                _isLoading.value = false
                _error.value = "No se pudo cargar la canción. Comprueba la conexión o el archivo local."
            }
        }
    }
}
