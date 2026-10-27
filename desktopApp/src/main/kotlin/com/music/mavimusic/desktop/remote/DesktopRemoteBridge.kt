package com.music.mavimusic.desktop.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bridge between DesktopApp UI / playback engine and the embedded Remote Server.
 */
internal object DesktopRemoteBridge {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class RemoteTrack(
        val videoId: String = "",
        val title: String = "",
        val artist: String = "",
        val album: String = "",
        val durationMs: Long = 0L,
        val artworkUrl: String = "",
    )

    @Serializable
    data class RemoteAudioOutput(
        val id: String = "",
        val name: String = "",
        val type: String = "speaker", // default, speaker, headphones, airplay, homepod, apple_tv
        val isSelected: Boolean = false,
    )

    @Serializable
    data class RemotePlaybackState(
        val isPlaying: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val timestamp: Long = System.currentTimeMillis(),
        val volume: Float = 1.0f,
        val shuffle: Boolean = false,
        val repeatMode: String = "OFF", // OFF, ALL, ONE
        val isLiked: Boolean = false,
        val audioQuality: String = "HIGH",
        val song: RemoteTrack? = null,
        val outputDevices: List<RemoteAudioOutput> = emptyList(),
        val currentOutputId: String = "",
        val currentOutputName: String = "",
    )

    @Serializable
    data class RemoteQueueItem(
        val index: Int = 0,
        val videoId: String = "",
        val title: String = "",
        val artist: String = "",
        val durationMs: Long = 0L,
        val artworkUrl: String = "",
        val isCurrent: Boolean = false,
    )

    @Serializable
    data class RemoteLyricLine(
        val timeMs: Long = 0L,
        val text: String = "",
    )

    @Serializable
    data class WsMessage(
        val type: String, // "STATE", "LYRICS", "QUEUE", "ERROR"
        val state: RemotePlaybackState? = null,
        val lines: List<RemoteLyricLine>? = null,
        val items: List<RemoteQueueItem>? = null,
        val message: String? = null,
    )

    // Current snapshots
    private var currentState = RemotePlaybackState()
    private var currentLyrics = emptyList<RemoteLyricLine>()
    private var currentQueue = emptyList<RemoteQueueItem>()

    // State flow for connected devices indicator in UI
    private val _connectedClientsCount = MutableStateFlow(0)
    val connectedClientsCount = _connectedClientsCount.asStateFlow()

    // Command dispatchers invoked on UI thread / coroutine
    var onPlayPause: (() -> Unit)? = null
    var onPlay: (() -> Unit)? = null
    var onPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onSetVolume: ((Float) -> Unit)? = null
    var onToggleShuffle: (() -> Unit)? = null
    var onCycleRepeat: (() -> Unit)? = null
    var onToggleLike: (() -> Unit)? = null
    var onPlayQueueIndex: ((Int) -> Unit)? = null
    var onRemoveFromQueue: ((Int) -> Unit)? = null
    var onMoveQueueItem: ((Int, Int) -> Unit)? = null
    var onSetOutputDevice: ((String) -> Unit)? = null

    fun setConnectedClients(count: Int) {
        _connectedClientsCount.value = count
    }

    fun getCurrentState(): RemotePlaybackState = synchronized(this) { currentState }

    fun getCurrentLyrics(): List<RemoteLyricLine> = synchronized(this) { currentLyrics }

    fun getCurrentQueue(): List<RemoteQueueItem> = synchronized(this) { currentQueue }

    fun updatePlaybackState(state: RemotePlaybackState) {
        synchronized(this) {
            currentState = state
        }
        val payload = json.encodeToString(WsMessage(type = "STATE", state = state))
        DesktopRemoteServer.broadcast(payload)
    }

    fun updateLyrics(lines: List<RemoteLyricLine>) {
        synchronized(this) {
            currentLyrics = lines
        }
        val payload = json.encodeToString(WsMessage(type = "LYRICS", lines = lines))
        DesktopRemoteServer.broadcast(payload)
    }

    fun updateQueue(items: List<RemoteQueueItem>) {
        synchronized(this) {
            currentQueue = items
        }
        val payload = json.encodeToString(WsMessage(type = "QUEUE", items = items))
        DesktopRemoteServer.broadcast(payload)
    }

    fun dispatchCommand(action: String, params: Map<String, Any?>) {
        when (action.uppercase()) {
            "PLAY_PAUSE" -> onPlayPause?.invoke()
            "PLAY" -> onPlay?.invoke()
            "PAUSE" -> onPause?.invoke()
            "NEXT" -> onNext?.invoke()
            "PREVIOUS" -> onPrevious?.invoke()
            "SEEK" -> {
                val pos = (params["positionMs"] as? Number)?.toLong() ?: return
                onSeekTo?.invoke(pos)
            }
            "SET_VOLUME" -> {
                val vol = (params["volume"] as? Number)?.toFloat() ?: return
                onSetVolume?.invoke(vol.coerceIn(0f, 1f))
            }
            "TOGGLE_SHUFFLE" -> onToggleShuffle?.invoke()
            "CYCLE_REPEAT" -> onCycleRepeat?.invoke()
            "TOGGLE_LIKE" -> onToggleLike?.invoke()
            "PLAY_QUEUE_INDEX" -> {
                val idx = (params["index"] as? Number)?.toInt() ?: return
                onPlayQueueIndex?.invoke(idx)
            }
            "REMOVE_QUEUE_INDEX" -> {
                val idx = (params["index"] as? Number)?.toInt() ?: return
                onRemoveFromQueue?.invoke(idx)
            }
            "MOVE_QUEUE_ITEM" -> {
                val from = (params["fromIndex"] as? Number)?.toInt() ?: return
                val to = (params["toIndex"] as? Number)?.toInt() ?: return
                onMoveQueueItem?.invoke(from, to)
            }
            "SET_OUTPUT_DEVICE" -> {
                val devId = params["deviceId"]?.toString() ?: ""
                onSetOutputDevice?.invoke(devId)
            }
        }
    }
}
