package com.music.mavimusic.desktop

import androidx.compose.runtime.CompositionLocalProvider
import com.music.mavimusic.ui.player.PlayerPlatform
import com.music.mavimusic.data.DebugLog
import com.music.mavimusic.data.TrackLog
import com.music.mavimusic.data.innertube.InnerTubeXResolver
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.innertube.potoken.PoTokenGenerator
import com.music.mavimusic.data.lyrics.LyricsTranslation
import com.music.mavimusic.data.spotify.SpotifyLibrary
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import mavimusic.desktopapp.generated.resources.Res
import mavimusic.desktopapp.generated.resources.app_logo
import mavimusic.desktopapp.generated.resources.logo
import java.awt.Dimension
import java.awt.Rectangle
import java.awt.Toolkit
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

fun main(args: Array<String>) {
    // Enable Metal hardware-accelerated rendering on macOS for maximum animation fluidity and ProMotion support
    if (System.getProperty("os.name").contains("Mac", ignoreCase = true)) {
        System.setProperty("apple.awt.application.name", "MaviMusic")
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MaviMusic")
        System.setProperty("skiko.renderApi", "METAL")
        System.setProperty("skiko.metal.gpu.priority", "auto")
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua")
        System.setProperty("apple.laf.useScreenMenuBar", "true")
    } else if (System.getProperty("os.name").contains("Windows", ignoreCase = true)) {
        // Enable Direct3D GPU hardware acceleration on Windows for 60-120 FPS fluid rendering
        System.setProperty("skiko.renderApi", "DIRECT3D")
        System.setProperty("sun.java2d.d3d", "true")
        System.setProperty("sun.java2d.noddraw", "false")
    }

    DesktopOpenAudio.install(args)

    // Initialize application default locale (Spanish by default)
    runCatching {
        val langTag = DesktopStrings.resolvedTag()
        val loc = java.util.Locale.forLanguageTag(langTag)
        java.util.Locale.setDefault(loc)
        java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, loc)
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, loc)
    }

    // Before any WebView exists, so the sign-in window can start over with an empty jar.
    runCatching { DesktopAccounts.restoreLocalSessions() }
        .onFailure { DesktopTrackLog.log("Could not finish importing local sessions") }
    DesktopWebCookies.install()
    DesktopWindowVisibility.install()
    // The player is the phone's, from the shared UI module; this is what it reads underneath.
    PlayerPlatform.install(DesktopPlayerHost)
    com.music.mavimusic.ui.AppUi.install(DesktopAppUiHost)
    // The data layer both apps share logs through here, and keeps translated
    // lyrics beside the rest of the desktop's cache.
    DebugLog.sink = DebugLog.Sink { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    LyricsTranslation.cacheDir = DesktopMediaCache.directory.toFile()
    // The Spotify library both apps share, on the tokens the web player mints in JavaFX.
    SpotifyLibrary.auth = SpotifyLibrary.Auth {
        DesktopSpotifyToken.accessToken()?.let { SpotifyLibrary.Tokens(it, DesktopSpotifyToken.clientToken()) }
    }
    // Desktop follows its own language picker (zh-Hant / zh-Hans preserved).
    com.music.mavimusic.data.innertube.Innertube.appLanguage = { DesktopStrings.resolvedTag() }
    // YouTube playback is the phone's StreamResolver over InnerTubeX, with BotGuard PoTokens
    // minted in JavaFX's WebView where the phone uses Android's.
    TrackLog.echo = { level, tag, message, error ->
        DesktopTrackLog.log("$tag/$level: $message" + (error?.let { " (${it.message})" } ?: ""))
    }
    StreamResolver.maxKbps = {
        DesktopStreamClient.ceiling.get() ?: DesktopSourceRegistry.ceiling(null).maxKbps
    }
    InnerTubeXResolver.init(
        filesDir = DesktopMediaCache.directory.toFile(),
        store = DesktopInnerTubeXStore,
        poTokenProvider = PoTokenGenerator(
            createMinter = { DesktopPoTokenWebView.getNewPoTokenGenerator() },
            available = { DesktopPoTokenWebView.available },
        ).asTokenProvider(),
    )
    // The public "apps open right now" count; see Presence.
    DesktopPresence.install()
    desktopMain()
}

private fun desktopMain() = application {
    // Closing puts the window away rather than ending the process, while there is a tray icon to
    // bring it back from — see [DesktopWindowVisibility].
    val visible by DesktopWindowVisibility.visible.collectAsState()
    val state = rememberWindowState(width = 1_220.dp, height = 780.dp)
    LaunchedEffect(visible) {
        if (visible && state.isMinimized) {
            state.isMinimized = false
        }
    }
    // Hoisted so the player can fill the screen and the caption buttons can maximize — see
    // [DesktopWindowMode]. Fullscreen on Windows is a native borderless window that covers the
    // taskbar, so it needs no placement change here; only ordinary maximize does.
    val placement by DesktopWindowMode.placement.collectAsState()
    LaunchedEffect(placement) { state.placement = placement }
    // ...and back, for the times the window is moved between placements by something that is not
    // us; see [DesktopWindowMode.adopt].
    LaunchedEffect(state) {
        snapshotFlow { state.placement }.collect(DesktopWindowMode::adopt)
    }
    val maximized by DesktopWindowMode.maximized.collectAsState()
    LaunchedEffect(maximized) {
        if (DesktopPlatform.isMac) {
            DesktopMacFrame.updateCornerRadius(maximized)
        }
    }
    Window(
        onCloseRequest = { if (DesktopWindowVisibility.onCloseRequest()) exitApplication() },
        visible = visible,
        title = if (DesktopPlatform.isMac) "" else "MaviMusic",
        icon = painterResource(Res.drawable.app_logo),
        state = state,
        // No system title bar on Windows, where the application draws its own instead — see
        // [DesktopWindowChrome].
        undecorated = DesktopPlatform.drawsOwnWindowFrame,
        // For Mica and Acrylic behind the sidebar and top bar: only a transparent window lets
        // DWM's material show through the chrome. Fixed at creation, so it is on wherever the
        // material is possible and the chosen one can be switched live — see [DesktopWindowBackdrop].
        transparent = DesktopWindowBackdrop.available,
        resizable = true,
    ) {
        val composeWindow = window
        val openingSize = remember { state.size }
        DisposableEffect(composeWindow) {
            val detach = if (DesktopPlatform.isWindows) DesktopWindowsWindowLayout.attach(
                composeWindow, Dimension(openingSize.width.value.roundToInt(), openingSize.height.value.roundToInt()),
            ) else ({})
            onDispose { detach() }
        }
        DisposableEffect(composeWindow) {
            val previous = composeWindow.dropTarget
            val target = DesktopOpenAudio.attachDropTarget(composeWindow)
            onDispose { target.isActive = false; composeWindow.dropTarget = previous }
        }
        LaunchedEffect(composeWindow) {
            DesktopWindowVisibility.attachWindow(composeWindow)
            // AWT's default is white, and it is what shows for the frame or two a moved or resized
            // window takes to repaint: a white band along its edges.
            // Not over a transparent window, whose clear background is what the material shows
            // through; DWM moves that window, so there is no band to hide.
            if (!DesktopWindowBackdrop.available) {
                composeWindow.background = java.awt.Color.BLACK
                composeWindow.contentPane.background = java.awt.Color.BLACK
            } else {
                composeWindow.background = java.awt.Color(0, 0, 0, 0)
                composeWindow.contentPane.background = java.awt.Color(0, 0, 0, 0)
                (composeWindow.contentPane as? javax.swing.JComponent)?.isOpaque = false
                composeWindow.rootPane.isOpaque = false
            }
            if (!DesktopPlatform.isWindows) {
                val transform = composeWindow.graphicsConfiguration.defaultTransform
                val scaleX = if (DesktopPlatform.isMac) 1.0 else transform.scaleX
                val scaleY = if (DesktopPlatform.isMac) 1.0 else transform.scaleY
                val minW = (MIN_WINDOW_WIDTH_DP * scaleX).roundToInt()
                val minH = (MIN_WINDOW_HEIGHT_DP * scaleY).roundToInt()
                composeWindow.minimumSize = Dimension(minW, minH)
                composeWindow.size = Dimension(
                    (openingSize.width.value * scaleX).roundToInt().coerceAtLeast(minW),
                    (openingSize.height.value * scaleY).roundToInt().coerceAtLeast(minH),
                )
                composeWindow.addComponentListener(object : java.awt.event.ComponentAdapter() {
                    override fun componentResized(e: java.awt.event.ComponentEvent?) {
                        val w = composeWindow.width
                        val h = composeWindow.height
                        if (w < minW || h < minH) {
                            composeWindow.setSize(w.coerceAtLeast(minW), h.coerceAtLeast(minH))
                        }
                    }
                })
            }
            if (DesktopPlatform.isWindows && DesktopWindowsFrame.install("MaviMusic")) {
                DesktopWindowBackdrop.apply()
            } else if (DesktopPlatform.isMac && DesktopMacFrame.install(composeWindow)) {
                DesktopWindowBackdrop.apply()
                DesktopMacMedia.enforceMainMinSize(MIN_WINDOW_WIDTH_DP.roundToInt(), MIN_WINDOW_HEIGHT_DP.roundToInt())
            }
            DesktopCursorFix.install(composeWindow)
        }
        val actions = remember {
            DesktopWindowActions(
                minimize = {
                    if (!DesktopWindowsFrame.minimize()) state.isMinimized = true
                },
                toggleMaximize = {
                    if (!DesktopWindowsFrame.toggleMaximize()) {
                        // An undecorated AWT window otherwise maximizes to the monitor bounds on
                        // Windows when the native frame bridge is unavailable. Give AWT the
                        // monitor's usable work area before Compose switches the placement.
                        if (DesktopPlatform.isWindows && !DesktopWindowMode.maximized.value) {
                            val screen = composeWindow.graphicsConfiguration.bounds
                            val insets = Toolkit.getDefaultToolkit().getScreenInsets(
                                composeWindow.graphicsConfiguration,
                            )
                            composeWindow.maximizedBounds = Rectangle(
                                screen.x + insets.left,
                                screen.y + insets.top,
                                screen.width - insets.left - insets.right,
                                screen.height - insets.top - insets.bottom,
                            )
                        }
                        DesktopWindowMode.toggleMaximized()
                    }
                },
                // The same door the system's close button went through, so the tray keeps the
                // process alive exactly as it did before.
                close = { if (DesktopWindowVisibility.onCloseRequest()) exitApplication() },
            )
        }
        CompositionLocalProvider(
            LocalDesktopWindowActions provides actions,
            LocalDesktopWindowScope provides this,
        ) {
            MaviMusicDesktopApp()
        }
    }

    if (DesktopMiniPlayerState.isOpen) {
        val miniState = rememberWindowState(
            position = WindowPosition(DesktopMiniPlayerState.screenX.dp, DesktopMiniPlayerState.screenY.dp),
            size = DpSize(384.dp, 172.dp),
        )
        LaunchedEffect(DesktopMiniPlayerState.screenX, DesktopMiniPlayerState.screenY) {
            miniState.position = WindowPosition(DesktopMiniPlayerState.screenX.dp, DesktopMiniPlayerState.screenY.dp)
        }
        Window(
            onCloseRequest = { DesktopMiniPlayerState.isOpen = false },
            state = miniState,
            title = "MaviMusic",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = true,
        ) {
            val miniWindow = window
            DisposableEffect(miniWindow) {
                miniWindow.background = java.awt.Color(0, 0, 0, 0)
                miniWindow.contentPane.background = java.awt.Color(0, 0, 0, 0)
                (miniWindow.contentPane as? javax.swing.JComponent)?.isOpaque = false
                miniWindow.rootPane.isOpaque = false

                miniWindow.setLocation(DesktopMiniPlayerState.screenX, DesktopMiniPlayerState.screenY)
                miniWindow.toFront()
                miniWindow.requestFocus()

                val focusListener = object : java.awt.event.WindowFocusListener {
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {}
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                        DesktopMiniPlayerState.lastLostFocusTime = System.currentTimeMillis()
                        DesktopMiniPlayerState.isOpen = false
                    }
                }
                miniWindow.addWindowFocusListener(focusListener)
                onDispose {
                    miniWindow.removeWindowFocusListener(focusListener)
                }
            }

            DesktopTopMiniPlayer(
                compact = false,
                song = DesktopMiniPlayerState.song,
                isPlaying = DesktopMiniPlayerState.isPlaying,
                isLoading = DesktopMiniPlayerState.isLoading,
                previousEnabled = DesktopMiniPlayerState.previousEnabled,
                progress = if (DesktopMiniPlayerState.durationMs > 0L) {
                    DesktopMiniPlayerState.positionMs.toFloat() / DesktopMiniPlayerState.durationMs.toFloat()
                } else {
                    0f
                },
                positionMs = DesktopMiniPlayerState.positionMs,
                durationMs = DesktopMiniPlayerState.durationMs,
                volume = DesktopMiniPlayerState.volume,
                onVolumeChange = DesktopMiniPlayerState.onVolumeChange,
                onSeek = DesktopMiniPlayerState.onSeek,
                onPlayPause = DesktopMiniPlayerState.onPlayPause,
                onPrevious = DesktopMiniPlayerState.onPrevious,
                onNext = DesktopMiniPlayerState.onNext,
                onFeelingLucky = DesktopMiniPlayerState.onFeelingLucky,
                onOpenNowPlaying = {
                    DesktopMiniPlayerState.isOpen = false
                    DesktopWindowVisibility.show()
                    DesktopMiniPlayerState.onOpenNowPlaying()
                },
                onOpenLyrics = {
                    DesktopMiniPlayerState.isOpen = false
                    DesktopWindowVisibility.show()
                    DesktopMiniPlayerState.onOpenLyrics()
                },
                onOpenQueue = {
                    DesktopMiniPlayerState.isOpen = false
                    DesktopWindowVisibility.show()
                    DesktopMiniPlayerState.onOpenQueue()
                },
                onMinimize = {
                    DesktopMiniPlayerState.isOpen = false
                },
                onToggleExpand = {
                    DesktopMiniPlayerState.isOpen = false
                    DesktopWindowVisibility.show()
                    DesktopMiniPlayerState.onToggleExpand()
                },
                onClose = {
                    DesktopMiniPlayerState.isOpen = false
                },
                onOpenSongMenu = { song ->
                    DesktopMiniPlayerState.isOpen = false
                    DesktopWindowVisibility.show()
                    DesktopMiniPlayerState.onOpenSongMenu(song)
                },
            )
        }
    }

    if (DesktopSpotlightState.isOpen) {
        val spotlightWindowState = rememberWindowState(
            position = WindowPosition(DesktopSpotlightState.screenX.dp, DesktopSpotlightState.screenY.dp),
            size = DpSize(660.dp, 500.dp),
        )
        Window(
            onCloseRequest = { DesktopSpotlightState.dismiss() },
            state = spotlightWindowState,
            title = "MaviMusic Spotlight",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = true,
            onKeyEvent = { event ->
                if (event.key == Key.Escape) {
                    DesktopSpotlightState.dismiss()
                    true
                } else false
            },
        ) {
            val spotWindow = window
            LaunchedEffect(DesktopSpotlightState.screenX, DesktopSpotlightState.screenY) {
                spotlightWindowState.position = WindowPosition(DesktopSpotlightState.screenX.dp, DesktopSpotlightState.screenY.dp)
                spotWindow.setLocation(DesktopSpotlightState.screenX, DesktopSpotlightState.screenY)
            }
            DisposableEffect(spotWindow) {
                spotWindow.background = java.awt.Color(0, 0, 0, 0)
                spotWindow.contentPane.background = java.awt.Color(0, 0, 0, 0)
                (spotWindow.contentPane as? javax.swing.JComponent)?.isOpaque = false
                spotWindow.rootPane.isOpaque = false

                spotWindow.isAutoRequestFocus = true
                spotWindow.isFocusable = true
                spotWindow.focusableWindowState = true

                spotWindow.setLocation(DesktopSpotlightState.screenX, DesktopSpotlightState.screenY)
                java.awt.EventQueue.invokeLater {
                    spotWindow.toFront()
                    spotWindow.requestFocus()
                    spotWindow.requestFocusInWindow()
                    DesktopMacMedia.configureSpotlight(DesktopSpotlightState.openedExternally)
                }

                val focusListener = object : java.awt.event.WindowFocusListener {
                    private var hasGainedFocus = false
                    override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                        hasGainedFocus = true
                    }
                    override fun windowLostFocus(e: java.awt.event.WindowEvent?) {
                        if (hasGainedFocus && !DesktopSpotlightState.isDragging) {
                            DesktopSpotlightState.dismiss(returnFocusToExternalApp = false)
                        }
                    }
                }
                spotWindow.addWindowFocusListener(focusListener)
                onDispose {
                    spotWindow.removeWindowFocusListener(focusListener)
                }
            }

            DesktopCommandPalette(
                isStandaloneWindow = true,
                onDismiss = { DesktopSpotlightState.dismiss() },
                onNavigate = { dest ->
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onNavigate(dest)
                },
                onOpenSettings = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenSettings()
                },
                onOpenSleepTimer = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss(returnFocusToExternalApp = false)
                },
                onOpenDownloads = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenDownloads()
                },
                onPlayLucky = {
                    DesktopSpotlightState.onPlayLucky()
                    DesktopSpotlightState.dismiss()
                },
                onTogglePlayPause = {
                    DesktopSpotlightState.onTogglePlayPause()
                    DesktopSpotlightState.dismiss()
                },
                onPlayNext = {
                    DesktopSpotlightState.onPlayNext()
                    DesktopSpotlightState.dismiss()
                },
                onPlayPrevious = {
                    DesktopSpotlightState.onPlayPrevious()
                    DesktopSpotlightState.dismiss()
                },
                onToggleShuffle = {
                    DesktopSpotlightState.onToggleShuffle()
                    DesktopSpotlightState.dismiss()
                },
                onOpenLyrics = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenLyrics()
                },
                onOpenQueue = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenQueue()
                },
                onOpenImmersiveLyrics = {
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenImmersive()
                },
                onToggleNormalization = {},
                onPlaySong = { song ->
                    DesktopSpotlightState.onPlaySong(song)
                    DesktopSpotlightState.dismiss()
                },
                onOpenCollection = { id, type ->
                    DesktopSpotlightState.onOpenCollection(id, type)
                    DesktopSpotlightState.dismiss()
                },
                onOpenArtist = { id, name ->
                    DesktopSpotlightState.openedExternally = false
                    DesktopWindowVisibility.show()
                    DesktopSpotlightState.dismiss()
                    DesktopSpotlightState.onOpenArtist(id, name)
                },
            )
        }
    }
}

private const val MIN_WINDOW_WIDTH_DP = 1024.0
private const val MIN_WINDOW_HEIGHT_DP = 768.0
