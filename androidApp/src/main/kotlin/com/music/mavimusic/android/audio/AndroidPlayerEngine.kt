package com.music.mavimusic.android.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.durationMillis
import com.music.mavimusic.playback.PlaybackPosition
import com.music.mavimusic.ui.player.RepeatModes
import kotlinx.coroutines.CoroutineScope
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

    private var player: ExoPlayer? = null
    private var tickerJob: Job? = null
    private var resolutionJob: Job? = null

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

    private val _lyrics = MutableStateFlow<List<com.music.mavimusic.data.lyrics.LyricLine>?>(null)
    val lyrics = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading = _lyricsLoading.asStateFlow()

    private val _lyricsSource = MutableStateFlow<com.music.mavimusic.data.lyrics.LyricsSource?>(null)
    val lyricsSource = _lyricsSource.asStateFlow()

    private val playerListener = object : Player.Listener {
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

    fun attachPlayer(exoPlayer: ExoPlayer) {
        player = exoPlayer
        exoPlayer.addListener(playerListener)
        _isPlaying.value = exoPlayer.isPlaying
        if (exoPlayer.isPlaying) {
            startTicker()
        }
    }

    fun detachPlayer() {
        tickerJob?.cancel()
        player?.removeListener(playerListener)
        player = null
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val currentP = player?.currentPosition ?: 0L
                position.report(currentP, System.nanoTime())
                delay(200)
            }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        _queue.value = songs
        val targetIndex = startIndex.coerceIn(0, songs.size - 1)
        _queueIndex.value = targetIndex
        loadAndPlaySong(songs[targetIndex])
    }

    fun playSong(song: Song) {
        playQueue(listOf(song), 0)
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun seekTo(positionMs: Long) {
        position.seeks++
        position.report(positionMs)
        player?.seekTo(positionMs)
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
        if (current + 1 < q.size) {
            val nextIndex = current + 1
            _queueIndex.value = nextIndex
            loadAndPlaySong(q[nextIndex])
        } else if (_repeatMode.value == RepeatModes.ALL) {
            _queueIndex.value = 0
            loadAndPlaySong(q[0])
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
        val current = _queueIndex.value
        if (current - 1 >= 0) {
            val prevIndex = current - 1
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
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    private fun handleTrackEnded() {
        when (_repeatMode.value) {
            RepeatModes.ONE -> {
                seekTo(0L)
                play()
            }
            else -> skipNext()
        }
    }

    private fun loadAndPlaySong(song: Song) {
        _currentSong.value = song
        _isLoading.value = true
        _lyrics.value = null
        _lyricsSource.value = null
        _lyricsLoading.value = true
        position.seeks++
        position.report(0L)

        // Asynchronously fetch synchronized lyrics with song duration
        scope.launch(Dispatchers.IO) {
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
                if (found != null && _currentSong.value?.videoId == song.videoId) {
                    _lyrics.value = found.lines
                    _lyricsSource.value = found.source
                }
            } catch (_: Exception) {
            } finally {
                _lyricsLoading.value = false
            }
        }

        resolutionJob?.cancel()
        resolutionJob = scope.launch {
            try {
                val streamUrl = withContext(Dispatchers.IO) {
                    StreamResolver.resolve(song.videoId)
                }

                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.albumName)
                    .setArtworkUri(song.thumbnailUrl?.let { Uri.parse(it) })
                    .build()

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
                p.setMediaSource(mediaSource)
                p.prepare()
                p.play()
            } catch (e: Exception) {
                _isLoading.value = false
            }
        }
    }
}
