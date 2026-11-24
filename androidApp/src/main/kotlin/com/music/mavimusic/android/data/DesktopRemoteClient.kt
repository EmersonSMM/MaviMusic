package com.music.mavimusic.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Serializable
data class DesktopRemoteTrack(
    val videoId: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val durationMs: Long = 0L,
    val artworkUrl: String = "",
)

@Serializable
data class DesktopRemoteAudioOutput(
    val id: String = "",
    val name: String = "",
    val type: String = "speaker",
    val isSelected: Boolean = false,
)

@Serializable
data class DesktopRemotePlaybackState(
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1.0f,
    val shuffle: Boolean = false,
    val repeatMode: String = "OFF",
    val isLiked: Boolean = false,
    val song: DesktopRemoteTrack? = null,
    val outputDevices: List<DesktopRemoteAudioOutput> = emptyList(),
    val currentOutputId: String = "",
    val currentOutputName: String = "",
)

sealed interface DesktopRemoteConnection {
    data object Disconnected : DesktopRemoteConnection
    data object Connecting : DesktopRemoteConnection
    data class Connected(val baseUrl: String) : DesktopRemoteConnection
    data class Error(val message: String) : DesktopRemoteConnection
}

/**
 * Native HTTP + WebSocket client for MaviMusic Desktop Remote (`DesktopRemoteServer`).
 */
class DesktopRemoteClient(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
) {
    @Serializable
    private data class PairRequest(
        val pin: String,
        val deviceName: String,
        val autoTrust: Boolean = false,
    )

    @Serializable
    private data class PairResponse(
        val success: Boolean = false,
        val token: String? = null,
        val deviceId: String? = null,
        val message: String? = null,
    )

    @Serializable
    private data class WsEnvelope(
        val type: String = "",
        val state: DesktopRemotePlaybackState? = null,
        val message: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val lock = Any()
    private var socket: WebSocket? = null

    private val _connection = MutableStateFlow<DesktopRemoteConnection>(DesktopRemoteConnection.Disconnected)
    val connection: StateFlow<DesktopRemoteConnection> = _connection.asStateFlow()

    private val _playbackState = MutableStateFlow<DesktopRemotePlaybackState?>(null)
    val playbackState: StateFlow<DesktopRemotePlaybackState?> = _playbackState.asStateFlow()

    suspend fun pair(
        rawAddress: String,
        pin: String,
        deviceName: String = "MaviMusic Android",
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val normalized = runCatching { DesktopRemoteAddress.normalize(rawAddress) }.getOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("Revisa la dirección del equipo y su puerto."))
        val baseUrl = normalized.trimEnd('/')
        val cleanPin = pin.trim()
        if (cleanPin.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Introduce el PIN de 4 dígitos que muestra tu equipo."))
        }
        _connection.value = DesktopRemoteConnection.Connecting
        try {
            val payload = json.encodeToString(PairRequest(pin = cleanPin, deviceName = deviceName, autoTrust = false))
            val request = Request.Builder()
                .url("$baseUrl/api/pair")
                .post(payload.toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val bodyText = response.body.string()
                val parsed = runCatching { json.decodeFromString<PairResponse>(bodyText) }.getOrNull()
                val token = parsed?.token?.takeIf { it.isNotBlank() }
                if (response.isSuccessful && parsed?.success == true && token != null) {
                    connect(normalized, token)
                    Result.success(normalized to token)
                } else {
                    val message = parsed?.message?.takeIf { it.isNotBlank() }
                        ?: "PIN incorrecto o equipo no disponible (HTTP ${response.code})."
                    _connection.value = DesktopRemoteConnection.Error(message)
                    Result.failure(IllegalStateException(message))
                }
            }
        } catch (e: Exception) {
            val message = "No se pudo conectar con $baseUrl: ${e.message ?: "error de red"}"
            _connection.value = DesktopRemoteConnection.Error(message)
            Result.failure(IllegalStateException(message, e))
        }
    }

    fun connect(rawAddress: String, token: String) {
        val baseUrl = runCatching { DesktopRemoteAddress.normalize(rawAddress) }.getOrNull() ?: run {
            _connection.value = DesktopRemoteConnection.Error("Dirección del equipo no válida.")
            return
        }
        val wsUrl = toWebSocketUrl(baseUrl, token)
        disconnect()
        _connection.value = DesktopRemoteConnection.Connecting

        val request = Request.Builder()
            .url(wsUrl)
            .header("Authorization", "Bearer $token")
            .build()

        val ws = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connection.value = DesktopRemoteConnection.Connected(baseUrl)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                parseMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                synchronized(lock) {
                    if (socket === webSocket) socket = null
                }
                if (_connection.value !is DesktopRemoteConnection.Error) {
                    _connection.value = DesktopRemoteConnection.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                synchronized(lock) {
                    if (socket === webSocket) socket = null
                }
                val code = response?.code
                val msg = if (code == 401 || code == 403) {
                    "Sesión expirada. Vuelve a introducir el PIN de tu equipo."
                } else {
                    "Conexión interrumpida (${t.message ?: "sin respuesta"})."
                }
                _connection.value = DesktopRemoteConnection.Error(msg)
            }
        })
        synchronized(lock) {
            socket = ws
        }
    }

    fun sendCommand(action: String, params: Map<String, Any?> = emptyMap()): Boolean {
        val payload = buildJsonObject {
            put("action", action)
            params.forEach { (k, v) ->
                when (v) {
                    is String -> put(k, v)
                    is Boolean -> put(k, v)
                    is Int -> put(k, v)
                    is Long -> put(k, v)
                    is Float -> put(k, v)
                    is Double -> put(k, v)
                }
            }
        }.toString()
        val active = synchronized(lock) { socket } ?: return false
        return active.send(payload)
    }

    fun disconnect() {
        val active = synchronized(lock) {
            socket.also { socket = null }
        }
        active?.close(1000, "Client closed")
        _connection.value = DesktopRemoteConnection.Disconnected
        _playbackState.value = null
    }

    internal fun parseMessage(text: String) {
        val envelope = runCatching { json.decodeFromString<WsEnvelope>(text) }.getOrNull() ?: return
        when (envelope.type.uppercase()) {
            "STATE" -> {
                val st = envelope.state ?: return
                val cleanedSong = st.song?.takeIf { it.title.isNotBlank() || it.videoId.isNotBlank() }
                _playbackState.value = st.copy(
                    volume = st.volume.coerceIn(0f, 1f),
                    song = cleanedSong,
                )
            }
            "ERROR" -> {
                val msg = envelope.message?.takeIf { it.isNotBlank() } ?: "Error del servidor remoto."
                _connection.value = DesktopRemoteConnection.Error(msg)
            }
        }
    }

    companion object {
        fun toWebSocketUrl(baseHttpUrl: String, token: String): String {
            val wsBase = when {
                baseHttpUrl.startsWith("https://", ignoreCase = true) -> "wss://" + baseHttpUrl.removePrefix("https://")
                baseHttpUrl.startsWith("http://", ignoreCase = true) -> "ws://" + baseHttpUrl.removePrefix("http://")
                else -> "ws://$baseHttpUrl"
            }.trimEnd('/')
            val encodedToken = URLEncoder.encode(token, "UTF-8")
            return "$wsBase/ws?token=$encodedToken"
        }
    }
}
