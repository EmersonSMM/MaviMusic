package com.music.mavimusic.desktop.remote

import com.music.mavimusic.desktop.DesktopTrackLog
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.utils.io.readFully
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.InetAddress
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Embedded Ktor HTTP + WebSocket server for MaviMusic Remote.
 */
internal object DesktopRemoteServer {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var server: EmbeddedServer<*, *>? = null
    private var serverScope: CoroutineScope? = null

    private val activeSessions = Collections.newSetFromMap(ConcurrentHashMap<WebSocketSession, Boolean>())
    private val sessionDevices = ConcurrentHashMap<WebSocketSession, String>()

    @Serializable
    private data class PairRequest(
        val pin: String? = null,
        val otp: String? = null,
        val deviceName: String? = null,
        val autoTrust: Boolean = false,
    )

    @Serializable
    private data class PairResponse(
        val success: Boolean,
        val token: String? = null,
        val deviceId: String? = null,
        val message: String? = null,
    )

    fun isRunning(): Boolean = server != null

    @Synchronized
    fun start(preferredPort: Int = DesktopRemoteDiscovery.port.value) {
        if (server != null) return

        serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        var actualPort = preferredPort
        var engine: EmbeddedServer<*, *>? = null
        var attempts = 0
        val maxAttempts = 5

        while (attempts < maxAttempts) {
            try {
                engine = embeddedServer(CIO, port = actualPort, host = "0.0.0.0") {
                    install(CORS) {
                        anyHost()
                        allowHeader(HttpHeaders.Authorization)
                        allowHeader(HttpHeaders.ContentType)
                        allowMethod(HttpMethod.Options)
                        allowMethod(HttpMethod.Get)
                        allowMethod(HttpMethod.Post)
                    }

                    install(WebSockets)

                    // Security Interceptor: enforce private LAN IPs only
                    intercept(ApplicationCallPipeline.Plugins) {
                        val host = call.request.local.remoteHost
                        val inet = runCatching { InetAddress.getByName(host) }.getOrNull()
                        if (inet == null || !DesktopRemoteSecurity.isPrivateNetwork(inet)) {
                            DesktopTrackLog.log("Remote security: rejected non-LAN connection from $host")
                            call.respond(HttpStatusCode.Forbidden, "Acceso restringido a la red local privada.")
                            finish()
                        } else {
                            proceed()
                        }
                    }

                    routing {
                        // Static web app resources
                        get("/") { serveStaticResource(call, "index.html", ContentType.Text.Html) }
                        get("/index.html") { serveStaticResource(call, "index.html", ContentType.Text.Html) }
                        get("/style.css") { serveStaticResource(call, "style.css", ContentType.Text.CSS) }
                        get("/app.js") { serveStaticResource(call, "app.js", ContentType.Application.JavaScript) }
                        get("/manifest.json") { serveStaticResource(call, "manifest.json", ContentType.Application.Json) }
                        get("/icon-192.png") { serveStaticResource(call, "icon-192.png", ContentType.Image.PNG) }
                        get("/icon-512.png") { serveStaticResource(call, "icon-512.png", ContentType.Image.PNG) }
                        get("/icon.png") { serveStaticResource(call, "icon-192.png", ContentType.Image.PNG) }
                        get("/apple-touch-icon.png") { serveStaticResource(call, "icon-192.png", ContentType.Image.PNG) }
                        get("/apple-touch-icon-precomposed.png") { serveStaticResource(call, "icon-192.png", ContentType.Image.PNG) }
                        get("/favicon.ico") { serveStaticResource(call, "icon-192.png", ContentType.Image.PNG) }

                        // Pairing endpoint (public to LAN)
                        post("/api/pair") {
                            try {
                                val raw = runCatching { call.receiveText() }.getOrDefault("")
                                val body = runCatching { json.decodeFromString<PairRequest>(raw) }.getOrNull()
                                val pinAttempt = body?.pin ?: call.request.queryParameters["pin"]
                                val otpAttempt = body?.otp ?: call.request.queryParameters["otp"]
                                val devName = body?.deviceName ?: call.request.queryParameters["deviceName"] ?: "Navegador Web"
                                val autoTrust = body?.autoTrust ?: (call.request.queryParameters["autoTrust"]?.toBooleanStrictOrNull() ?: false)
                                val result = DesktopRemoteSecurity.pair(
                                    pinAttempt = pinAttempt,
                                    otpAttempt = otpAttempt,
                                    deviceName = devName,
                                    autoTrust = autoTrust,
                                )
                                when (result) {
                                    is DesktopRemoteSecurity.PairResult.Success -> {
                                        val res = PairResponse(
                                            success = true,
                                            token = result.token,
                                            deviceId = result.deviceId,
                                        )
                                        call.respondText(json.encodeToString(res), ContentType.Application.Json)
                                    }
                                    is DesktopRemoteSecurity.PairResult.Error -> {
                                        val res = PairResponse(success = false, message = result.message)
                                        call.respondText(
                                            json.encodeToString(res),
                                            ContentType.Application.Json,
                                            HttpStatusCode.Unauthorized
                                        )
                                    }
                                }
                            } catch (t: Throwable) {
                                DesktopTrackLog.log("PAIR HANDLER ERROR: ${t.javaClass.name}: ${t.message}")
                                call.respondText("Error: ${t.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                            }
                        }

                        // State endpoint (requires token)
                        get("/api/state") {
                            val token = extractToken(call)
                            if (DesktopRemoteSecurity.validateToken(token) == null) {
                                call.respond(HttpStatusCode.Unauthorized, "No autorizado")
                                return@get
                            }
                            val state = DesktopRemoteBridge.getCurrentState()
                            call.respondText(json.encodeToString(state), ContentType.Application.Json)
                        }

                        // Lyrics endpoint
                        get("/api/lyrics") {
                            val token = extractToken(call)
                            if (DesktopRemoteSecurity.validateToken(token) == null) {
                                call.respond(HttpStatusCode.Unauthorized, "No autorizado")
                                return@get
                            }
                            val lines = DesktopRemoteBridge.getCurrentLyrics()
                            val res = DesktopRemoteBridge.WsMessage(type = "LYRICS", lines = lines)
                            call.respondText(json.encodeToString(res), ContentType.Application.Json)
                        }

                        // Queue endpoint
                        get("/api/queue") {
                            val token = extractToken(call)
                            if (DesktopRemoteSecurity.validateToken(token) == null) {
                                call.respond(HttpStatusCode.Unauthorized, "No autorizado")
                                return@get
                            }
                            val items = DesktopRemoteBridge.getCurrentQueue()
                            val res = DesktopRemoteBridge.WsMessage(type = "QUEUE", items = items)
                            call.respondText(json.encodeToString(res), ContentType.Application.Json)
                        }

                        // WebSocket real-time connection
                        webSocket("/ws") {
                            val queryToken = call.request.queryParameters["token"]
                            val headerToken = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ")?.trim()
                            val token = queryToken ?: headerToken

                            val session = DesktopRemoteSecurity.validateToken(token)
                            if (session == null) {
                                close(CloseReason(4001.toShort(), "Token no válido o revocado"))
                                return@webSocket
                            }

                            activeSessions.add(this)
                            sessionDevices[this] = session.id
                            DesktopRemoteBridge.setConnectedClients(activeSessions.size)
                            DesktopTrackLog.log("Remote server: client connected from ${call.request.local.remoteHost} (${session.name})")

                            try {
                                // Send initial state snapshots
                                val initState = DesktopRemoteBridge.getCurrentState()
                                send(Frame.Text(json.encodeToString(DesktopRemoteBridge.WsMessage(type = "STATE", state = initState))))
                                val initLyrics = DesktopRemoteBridge.getCurrentLyrics()
                                if (initLyrics.isNotEmpty()) {
                                    send(Frame.Text(json.encodeToString(DesktopRemoteBridge.WsMessage(type = "LYRICS", lines = initLyrics))))
                                }
                                val initQueue = DesktopRemoteBridge.getCurrentQueue()
                                if (initQueue.isNotEmpty()) {
                                    send(Frame.Text(json.encodeToString(DesktopRemoteBridge.WsMessage(type = "QUEUE", items = initQueue))))
                                }

                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        handleIncomingCommand(text)
                                    }
                                }
                            } catch (e: Exception) {
                                // Normal disconnect or socket error
                            } finally {
                                activeSessions.remove(this)
                                sessionDevices.remove(this)
                                DesktopRemoteBridge.setConnectedClients(activeSessions.size)
                                DesktopTrackLog.log("Remote server: client disconnected (${session.name})")
                            }
                        }
                    }
                }.start(wait = false)
                break
            } catch (e: Exception) {
                attempts++
                DesktopTrackLog.log("Remote server: port $actualPort unavailable (${e.message}), trying ${actualPort + 1}")
                actualPort++
            }
        }

        if (engine != null) {
            server = engine
            DesktopRemoteDiscovery.setActivePort(actualPort)
            DesktopTrackLog.log("Remote server: started successfully on port $actualPort")
        } else {
            DesktopTrackLog.log("Remote server: failed to bind to any port after $maxAttempts attempts")
        }
    }

    @Synchronized
    fun stop() {
        if (server == null) return
        DesktopTrackLog.log("Remote server: stopping...")

        // Close all active web sockets
        val sessions = activeSessions.toList()
        activeSessions.clear()
        DesktopRemoteBridge.setConnectedClients(0)

        serverScope?.launch {
            sessions.forEach {
                runCatching { it.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Servidor apagado")) }
            }
        }

        runCatching {
            server?.stop(gracePeriodMillis = 300, timeoutMillis = 1000)
        }
        server = null
        serverScope?.cancel()
        serverScope = null
        DesktopTrackLog.log("Remote server: stopped. Sockets closed.")
    }

    fun disconnectDevice(deviceId: String) {
        val targets = sessionDevices.filterValues { it == deviceId }.keys
        if (targets.isEmpty()) return
        serverScope?.launch {
            for (ws in targets) {
                runCatching {
                    ws.close(CloseReason(4003.toShort(), "Dispositivo revocado por el usuario"))
                }
            }
        }
    }

    fun broadcast(message: String) {
        val sessions = activeSessions.toList()
        if (sessions.isEmpty()) return

        serverScope?.launch {
            val frame = Frame.Text(message)
            for (ws in sessions) {
                try {
                    ws.send(frame)
                } catch (e: ClosedSendChannelException) {
                    activeSessions.remove(ws)
                    DesktopRemoteBridge.setConnectedClients(activeSessions.size)
                } catch (e: Exception) {
                    // Ignore transient network errors
                }
            }
        }
    }

    private suspend fun serveStaticResource(call: ApplicationCall, name: String, contentType: ContentType) {
        val resourcePath = "remote/$name"
        val bytes = javaClass.classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes() }
        if (bytes != null) {
            call.respondBytes(bytes, contentType)
        } else {
            call.respond(HttpStatusCode.NotFound, "Recurso no encontrado: $name")
        }
    }

    private fun extractToken(call: ApplicationCall): String? {
        val auth = call.request.header(HttpHeaders.Authorization)
        if (!auth.isNullOrBlank() && auth.startsWith("Bearer ", ignoreCase = true)) {
            return auth.substring(7).trim()
        }
        return call.request.queryParameters["token"]?.trim()
    }

    private fun handleIncomingCommand(rawJson: String) {
        val root = runCatching { json.parseToJsonElement(rawJson) as? JsonObject }.getOrNull() ?: return
        val action = root["action"]?.jsonPrimitive?.content ?: return

        val params = mutableMapOf<String, Any?>()
        for ((key, elem) in root) {
            if (key == "action") continue
            val prim = elem.jsonPrimitive
            when {
                prim.booleanOrNull != null -> params[key] = prim.booleanOrNull
                prim.longOrNull != null -> params[key] = prim.longOrNull
                prim.doubleOrNull != null -> params[key] = prim.doubleOrNull
                prim.intOrNull != null -> params[key] = prim.intOrNull
                else -> params[key] = prim.content
            }
        }

        DesktopRemoteBridge.dispatchCommand(action, params)
    }
}
