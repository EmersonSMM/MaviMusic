package com.music.bitchord.desktop

import com.music.bitchord.data.model.artworkAt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * macOS Media Remote Command Center and Now Playing Info Center.
 *
 * Connects macOS media keys, AirPods gestures (double-tap skip, single tap play/pause,
 * auto-pause when taking an AirPod out of the ear, resume on reinserting), and the macOS
 * Control Center / Menu Bar "Now Playing" interface to MaviMusic playback.
 */
internal object DesktopMacMedia {

    private const val LIBRARY = "bitchord_mac_media"
    private const val BUTTON_PLAY = 0
    private const val BUTTON_PAUSE = 1
    private const val BUTTON_NEXT = 2
    private const val BUTTON_PREVIOUS = 3
    private const val BUTTON_STOP = 4
    private const val BUTTON_SPOTLIGHT = 5

    private val started = AtomicBoolean(false)
    private val shutdownHookAdded = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastArtFetchKey: String? = null

    @Volatile
    private var controller: Controller? = null

    /** What a media key or AirPods gesture does, filled in by the player. */
    internal class Controller(
        val onPlay: () -> Unit,
        val onPause: () -> Unit,
        val onNext: () -> Unit,
        val onPrevious: () -> Unit,
        val onStop: () -> Unit,
        /** Moves playback to a position the scrubber was dragged to, in milliseconds. */
        val onSeek: (Long) -> Unit,
        val onSpotlight: () -> Unit = {},
    )

    private val available: Boolean by lazy {
        if (!DesktopPlatform.isMac) return@lazy false
        runCatching {
            DesktopAnalysisRuntime.loadNative(LIBRARY)
        }.recoverCatching {
            System.loadLibrary(LIBRARY)
        }.onFailure {
            DesktopTrackLog.log("macOS media controls unavailable: ${it.message}")
        }.isSuccess
    }

    fun start(controller: Controller) {
        this.controller = controller
        if (!available || !started.compareAndSet(false, true)) return
        if (!nativeStart()) {
            started.set(false)
            DesktopTrackLog.log("macOS media controls could not be created")
            return
        }
        if (shutdownHookAdded.compareAndSet(false, true)) {
            Runtime.getRuntime().addShutdownHook(Thread(::stop, "bitchord-mac-media-stop"))
        }
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) return
        controller = null
        lastArtFetchKey = null
        runCatching { nativeStop() }
    }

    /** Publishes the current track. Called on every change macOS would show. */
    fun publish(state: DesktopPlaybackState) {
        if (!started.get()) return
        val song = state.song
        val preferredArtUrl = song?.artworkAt(512) ?: song?.thumbnailUrl
        val cachedFile = DesktopArtworkCache.getCachedFile(preferredArtUrl)
            ?: song?.thumbnailUrl?.let { DesktopArtworkCache.getCachedFile(it) }

        val artToSend = cachedFile?.absolutePath
            ?: preferredArtUrl
            ?: song?.thumbnailUrl.orEmpty()

        runCatching {
            nativeUpdate(
                song?.title.orEmpty(),
                song?.artist.orEmpty(),
                song?.albumName.orEmpty(),
                artToSend,
                state.isPlaying,
                state.positionMs,
                state.durationMs,
            )
        }

        if (cachedFile == null && !preferredArtUrl.isNullOrBlank()) {
            val trackKey = "${song?.videoId}|$preferredArtUrl"
            if (lastArtFetchKey != trackKey) {
                lastArtFetchKey = trackKey
                scope.launch {
                    val file = DesktopArtworkCache.getOrFetchFile(preferredArtUrl)
                    if (file != null && started.get() && controller != null && lastArtFetchKey == trackKey) {
                        runCatching {
                            nativeUpdate(
                                song?.title.orEmpty(),
                                song?.artist.orEmpty(),
                                song?.albumName.orEmpty(),
                                file.absolutePath,
                                state.isPlaying,
                                state.positionMs,
                                state.durationMs,
                            )
                        }
                    }
                }
            }
        }
    }

    /** Called from the native side when a media key or an AirPods gesture is used. */
    @JvmStatic
    fun onButton(button: Int) {
        val target = controller ?: return
        when (button) {
            BUTTON_PLAY -> target.onPlay()
            BUTTON_PAUSE -> target.onPause()
            BUTTON_NEXT -> target.onNext()
            BUTTON_PREVIOUS -> target.onPrevious()
            BUTTON_STOP -> target.onStop()
            BUTTON_SPOTLIGHT -> target.onSpotlight()
        }
    }

    /** Called from the native side when the Control Center / lock screen scrubber is dragged. */
    @JvmStatic
    fun onSeek(positionMs: Long) {
        controller?.onSeek(positionMs)
    }

    @JvmStatic
    private external fun nativeStart(): Boolean

    @JvmStatic
    private external fun nativeStop()

    /** Displays a rich native macOS banner notification with album artwork. */
    fun postNotification(
        title: String,
        subtitle: String,
        body: String,
        imagePath: String = "",
    ) {
        if (!started.get() && !available) return
        runCatching {
            nativePostNotification(title, subtitle, body, imagePath)
        }
    }

    @JvmStatic
    private external fun nativeUpdate(
        title: String,
        artist: String,
        album: String,
        artworkPath: String,
        playing: Boolean,
        positionMs: Long,
        durationMs: Long,
    )

    @JvmStatic
    private external fun nativePostNotification(
        title: String,
        subtitle: String,
        body: String,
        imagePath: String,
    )
}
