package com.music.mavimusic.desktop

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import com.music.mavimusic.ui.icons.MaviMusicIcons
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent
import java.util.Locale
import javax.swing.text.JTextComponent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.data.lyrics.LyricLine
import com.music.mavimusic.data.lyrics.LyricsSource
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.playback.PlaybackPosition
import com.music.mavimusic.ui.LyricsProviderState
import com.music.mavimusic.ui.player.LyricsSidePanel
import com.music.mavimusic.ui.player.MeshGradientBackground
import com.music.mavimusic.ui.player.rememberArtworkColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal enum class ImmersiveVisualMode(val label: String) {
    WINAMP_WAVES("Ondas Classic"),
    WINAMP_SPECTRUM("Espectro Classic"),
    WINAMP_MILKDROP("Plasma MilkDrop"),
    WINAMP_TUNNEL("Túnel Retro 3D"),
    WINAMP_LASER("Láser Osciloscopio"),
    WINAMP_WARP("Hiperespacio 3D"),
    WINAMP_SPHERE("Esfera Retro 3D"),
    WINAMP_FIRE_SPARKS("Lluvia de Fuego"),
    WINAMP_STARBURST("Vórtice Contorno"),
    GPU_NEON_TUNNEL("Túnel Neón GPU"),
    GPU_WATER_SURFACE("Superficie Líquida GPU"),
    GPU_COSMIC_PARTICLES("Partículas Cósmicas GPU"),
    APPLE_LYRICS("Letras Mesh");

    val isGpuShader: Boolean
        get() = this == GPU_NEON_TUNNEL || this == GPU_WATER_SURFACE || this == GPU_COSMIC_PARTICLES
}

internal enum class ImmersiveColorPalette(
    val label: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
) {
    DYNAMIC_ARTWORK(
        label = "Color de carátula",
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFFFF007F),
        tertiary = Color(0xFF7C4DFF),
        accent = Color(0xFFFFD600),
    ),
    NEON_CYBERPUNK(
        label = "Neón Cyberpunk",
        primary = Color(0xFF00F0FF),
        secondary = Color(0xFFFF007F),
        tertiary = Color(0xFF7C4DFF),
        accent = Color(0xFFFFD600),
    ),
    FIRE_AMBER(
        label = "Fuego y Ámbar",
        primary = Color(0xFFFF4500),
        secondary = Color(0xFFFFAB00),
        tertiary = Color(0xFFFF1744),
        accent = Color(0xFFFFEA00),
    ),
    CLASSIC_WINAMP(
        label = "Verde Classic",
        primary = Color(0xFF00FF66),
        secondary = Color(0xFF76FF03),
        tertiary = Color(0xFF00E5FF),
        accent = Color(0xFFFFD600),
    ),
    COSMIC_ICE(
        label = "Hielo Cósmico",
        primary = Color(0xFF80D8FF),
        secondary = Color(0xFF00B0FF),
        tertiary = Color(0xFFB388FF),
        accent = Color(0xFFFFFFFF),
    ),
    MATRIX_CRT(
        label = "Matrix Fósforo",
        primary = Color(0xFF00FF66),
        secondary = Color(0xFF00C853),
        tertiary = Color(0xFF69F0AE),
        accent = Color(0xFFB9F6CA),
    ),
    SYNTHWAVE_SUNSET(
        label = "Synthwave 80s",
        primary = Color(0xFFFF2A85),
        secondary = Color(0xFF9D00FF),
        tertiary = Color(0xFFFF7A00),
        accent = Color(0xFF00F5FF),
    ),
}

@Immutable
internal data class VisualizerPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
) {
    companion object {
        val Default = VisualizerPalette(
            primary = Color(0xFF00FF66),
            secondary = Color(0xFF76FF03),
            tertiary = Color(0xFF00E5FF),
            accent = Color(0xFFFFD600),
        )
    }
}

internal enum class VisualizerSubmenu {
    NONE,
    MODES,
    PALETTES,
    SETTINGS,
}

/**
 * Full-window / fullscreen immersive visualizer & lyrics experience.
 * Includes classic Winamp Oscilloscope waves, LED Spectrum Analyzer with falling peak-hold dots,
 * MilkDrop psychedelic plasma waves, 3D Retro Warp Tunnel, Retro 3D Sphere, Fire Sparks Rain,
 * Starburst Vortex, and Apple Music synchronized kinetic lyrics.
 */
@Composable
internal fun DesktopImmersiveLyricsScreen(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    shuffle: Boolean,
    onShuffleChange: (Boolean) -> Unit,
    repeatMode: DesktopRepeatMode,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    lyrics: List<LyricLine>?,
    lyricsSource: LyricsSource?,
    lyricsProviderStates: Map<LyricsSource, LyricsProviderState>,
    onSelectLyricsProvider: (LyricsSource) -> Unit,
    lyricsUnavailable: Boolean,
    volumeNormalizationActive: Boolean,
    streamFormat: DesktopStreamFormat? = null,
    queue: List<Song> = emptyList(),
    queueIndex: Int = 0,
    onPlayQueueIndex: (Int) -> Unit = {},
    onRemoveQueueIndex: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    automix: Boolean = false,
    onAutomixChange: (Boolean) -> Unit = {},
    djMixState: DjMixState = DjMixState(),
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkPalette = rememberArtworkColors(song.thumbnailUrl, null)
    var visualMode by remember {
        val saved = DesktopPersistence.immersiveVisualMode(ImmersiveVisualMode.WINAMP_WAVES.name)
        val initial = ImmersiveVisualMode.entries.firstOrNull { it.name == saved } ?: ImmersiveVisualMode.WINAMP_WAVES
        val effective = if (initial.isGpuShader && !DesktopGpuSettings.isEnabled()) {
            ImmersiveVisualMode.WINAMP_WAVES
        } else {
            initial
        }
        mutableStateOf(effective)
    }
    var colorPalette by remember {
        val saved = DesktopPersistence.immersiveColorPalette(ImmersiveColorPalette.DYNAMIC_ARTWORK.name)
        val initial = ImmersiveColorPalette.entries.firstOrNull { it.name == saved } ?: ImmersiveColorPalette.DYNAMIC_ARTWORK
        mutableStateOf(initial)
    }
    var classicEffectsMode by remember {
        mutableStateOf(DesktopPersistence.immersiveClassicMode(false))
    }
    var showLyricsInWinamp by remember {
        mutableStateOf(DesktopPersistence.immersiveShowLyrics(false))
    }
    var avoidDuplicateVersions by remember {
        mutableStateOf(DesktopTrackSimilarity.avoidDuplicateVersions)
    }
    var allowRemixesAndLive by remember {
        mutableStateOf(DesktopTrackSimilarity.allowRemixesAndLive)
    }
    var showQueuePanel by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var activeSubmenu by remember { mutableStateOf(VisualizerSubmenu.NONE) }

    // Keyboard shortcut 'Q' to toggle the dedicated floating glass queue panel in Immersive Mode
    DisposableEffect(Unit) {
        val dispatcher = KeyEventDispatcher { event ->
            if (event.id == KeyEvent.KEY_PRESSED &&
                event.keyCode == KeyEvent.VK_Q &&
                !event.isMetaDown &&
                !event.isControlDown &&
                !event.isAltDown
            ) {
                val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                if (focusOwner !is JTextComponent) {
                    showQueuePanel = !showQueuePanel
                    return@KeyEventDispatcher true
                }
            }
            if (event.id == KeyEvent.KEY_PRESSED &&
                event.keyCode == KeyEvent.VK_K &&
                !event.isMetaDown &&
                !event.isControlDown &&
                !event.isAltDown &&
                DesktopKaraokeSettings.isFeatureEnabled()
            ) {
                val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
                if (focusOwner !is JTextComponent) {
                    DesktopKaraokeSettings.setActive(!DesktopKaraokeSettings.isActive.value)
                    return@KeyEventDispatcher true
                }
            }
            false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
        onDispose {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher)
        }
    }

    val activePalette = remember(colorPalette, artworkPalette) {
        if (colorPalette == ImmersiveColorPalette.DYNAMIC_ARTWORK && artworkPalette.colors.size >= 4) {
            VisualizerPalette(
                primary = artworkPalette.colors[0],
                secondary = artworkPalette.colors[1],
                tertiary = artworkPalette.colors[2],
                accent = artworkPalette.colors[3],
            )
        } else {
            VisualizerPalette(
                primary = colorPalette.primary,
                secondary = colorPalette.secondary,
                tertiary = colorPalette.tertiary,
                accent = colorPalette.accent,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF06060C))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}, // Consumes all clicks so they never skip tracks or leak underneath
            ),
    ) {
        if (visualMode == ImmersiveVisualMode.APPLE_LYRICS) {
            MeshGradientBackground(
                palette = artworkPalette,
                trackKey = song.videoId,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.40f),
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.85f),
                            ),
                        ),
                    ),
            )
        } else {
            WinampVisualizerBackdrop(
                mode = visualMode,
                palette = activePalette,
                classicMode = classicEffectsMode,
                isPlaying = isPlaying,
                songSeed = song.videoId.hashCode(),
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            if (showLyricsInWinamp) {
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.55f),
                                )
                            } else {
                                listOf(
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.30f),
                                )
                            },
                        ),
                    ),
            )
        }

        Column(Modifier.fillMaxSize()) {
            val topStartPadding = if (DesktopPlatform.isMac) 84.dp else 24.dp
            // Header with Exit on left and sober technical spec badge on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(start = topStartPadding, end = 24.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.height(34.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.FullscreenExit,
                            contentDescription = DesktopStrings["exit_immersive_lyrics", "Salir"],
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = DesktopStrings["exit_immersive_lyrics", "Salir (Esc)"],
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Dynamic real audio quality indicator (Point B.7) + Floating Queue toggle button (Q)
                val qualityBadgeText = remember(streamFormat, song.videoId, song.sourceQuality) {
                    formatDynamicStreamQuality(streamFormat, song)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x8812141E),
                        border = BorderStroke(
                            1.dp,
                            if (streamFormat?.isLossless == true) Color(0xFF00E5FF).copy(alpha = 0.38f)
                            else Color.White.copy(alpha = 0.16f),
                        ),
                        shadowElevation = 8.dp,
                        modifier = Modifier.height(28.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (streamFormat?.isLossless == true) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E5FF)),
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = qualityBadgeText,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.3.sp,
                            )
                        }
                    }

                    Surface(
                        onClick = { showQueuePanel = !showQueuePanel },
                        shape = RoundedCornerShape(10.dp),
                        color = if (showQueuePanel) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color(0x8812141E),
                        border = BorderStroke(
                            1.dp,
                            if (showQueuePanel) Color(0xFF00E5FF).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                        ),
                        shadowElevation = 8.dp,
                        modifier = Modifier.height(28.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Cola de reproducción (Q)",
                                tint = if (showQueuePanel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(15.dp),
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "Cola (Q)",
                                color = if (showQueuePanel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    val isKaraokeFeatureOn by DesktopKaraokeSettings.featureEnabled.collectAsState()
                    if (isKaraokeFeatureOn) {
                        val isKaraokeActive by DesktopKaraokeSettings.isActive.collectAsState()
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = { DesktopKaraokeSettings.setActive(!isKaraokeActive) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isKaraokeActive) Color(0xFF00E5FF).copy(alpha = 0.22f) else Color(0x8812141E),
                            border = BorderStroke(
                                1.dp,
                                if (isKaraokeActive) Color(0xFF00E5FF).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.16f),
                            ),
                            shadowElevation = 8.dp,
                            modifier = Modifier.height(28.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = "Modo Karaoke",
                                    tint = if (isKaraokeActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = if (isKaraokeActive) "Karaoke ON" else "Karaoke",
                                    color = if (isKaraokeActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            // Body Area
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) {
                val isWide = maxWidth >= 860.dp
                val displayLyrics = visualMode == ImmersiveVisualMode.APPLE_LYRICS || showLyricsInWinamp

                if (isWide && displayLyrics) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .weight(0.42f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImmersiveHeroPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                position = position,
                                durationMs = durationMs,
                                volume = volume,
                                onVolumeChange = onVolumeChange,
                                onSeek = onSeek,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                shuffle = shuffle,
                                onShuffleChange = onShuffleChange,
                                repeatMode = repeatMode,
                                onRepeatModeChange = onRepeatModeChange,
                                automix = automix,
                                onAutomixChange = onAutomixChange,
                                djMixState = djMixState,
                                isQueueVisible = showQueuePanel,
                                onToggleQueue = { showQueuePanel = !showQueuePanel },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight(),
                        ) {
                            Crossfade(
                                targetState = song.videoId,
                                animationSpec = tween(600),
                                label = "lyricsCrossfade",
                            ) { _ ->
                                LyricsSidePanel(
                                    song = song,
                                    isPlaying = isPlaying,
                                    position = position,
                                    lyrics = lyrics,
                                    lyricsSource = lyricsSource,
                                    lyricsProviderStates = lyricsProviderStates,
                                    onSelectLyricsProvider = onSelectLyricsProvider,
                                    lyricsUnavailable = lyricsUnavailable,
                                    onSeek = onSeek,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                } else {
                    // Full stage Winamp Visualizer with centered hero player controls
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(modifier = Modifier.widthIn(max = 460.dp)) {
                            ImmersiveHeroPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                position = position,
                                durationMs = durationMs,
                                volume = volume,
                                onVolumeChange = onVolumeChange,
                                onSeek = onSeek,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                shuffle = shuffle,
                                onShuffleChange = onShuffleChange,
                                repeatMode = repeatMode,
                                onRepeatModeChange = onRepeatModeChange,
                                automix = automix,
                                onAutomixChange = onAutomixChange,
                                djMixState = djMixState,
                                isQueueVisible = showQueuePanel,
                                onToggleQueue = { showQueuePanel = !showQueuePanel },
                            )
                        }
                    }
                }
            }
        }

        // Bottom-left visualizer effects selector (sober, clean, no emojis)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp),
        ) {
            Surface(
                onClick = { menuExpanded = true },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xCC14141A),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                modifier = Modifier.height(34.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = visualMode.label,
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (menuExpanded) {
                Popup(
                    alignment = Alignment.BottomStart,
                    offset = IntOffset(0, -44),
                    onDismissRequest = {
                        menuExpanded = false
                        activeSubmenu = VisualizerSubmenu.NONE
                    },
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        // Main Root Menu (Windows XP style)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xF214141A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                            shadowElevation = 14.dp,
                            modifier = Modifier.width(220.dp),
                        ) {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.Tune,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Ajustes Visuales",
                                        color = Color.White.copy(alpha = 0.60f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )

                                // Item 1: Efectos visuales ▸
                                val isModesOpen = activeSubmenu == VisualizerSubmenu.MODES
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeSubmenu = if (isModesOpen) VisualizerSubmenu.NONE else VisualizerSubmenu.MODES
                                        }
                                        .background(if (isModesOpen) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            tint = if (isModesOpen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Efectos visuales",
                                            color = if (isModesOpen) Color(0xFF00E5FF) else Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isModesOpen) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                    Icon(
                                        MaviMusicIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isModesOpen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }

                                // Item 2: Paleta de color ▸
                                val isPalOpen = activeSubmenu == VisualizerSubmenu.PALETTES
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeSubmenu = if (isPalOpen) VisualizerSubmenu.NONE else VisualizerSubmenu.PALETTES
                                        }
                                        .background(if (isPalOpen) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(activePalette.primary),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = "Paleta de color",
                                            color = if (isPalOpen) Color(0xFF00E5FF) else Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isPalOpen) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                    Icon(
                                        MaviMusicIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isPalOpen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }

                                // Item 3: Opciones y DJ ▸
                                val isSettingsOpen = activeSubmenu == VisualizerSubmenu.SETTINGS
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeSubmenu = if (isSettingsOpen) VisualizerSubmenu.NONE else VisualizerSubmenu.SETTINGS
                                        }
                                        .background(if (isSettingsOpen) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = if (isSettingsOpen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Ajustes retro y DJ",
                                            color = if (isSettingsOpen) Color(0xFF00E5FF) else Color.White,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isSettingsOpen) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                    Icon(
                                        MaviMusicIcons.ChevronRight,
                                        contentDescription = null,
                                        tint = if (isSettingsOpen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.45f),
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }

                        // Cascading Submenu Flyout to the Right
                        if (activeSubmenu != VisualizerSubmenu.NONE) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xF714141A),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                                shadowElevation = 16.dp,
                                modifier = Modifier.width(235.dp),
                            ) {
                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    when (activeSubmenu) {
                                        VisualizerSubmenu.MODES -> {
                                            val modesToShow = if (DesktopGpuSettings.isEnabled()) {
                                                ImmersiveVisualMode.entries
                                            } else {
                                                ImmersiveVisualMode.entries.filter { !it.isGpuShader }
                                            }
                                            modesToShow.forEach { mode ->
                                                val selected = visualMode == mode
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            visualMode = mode
                                                            DesktopPersistence.saveImmersiveVisualMode(mode.name)
                                                            menuExpanded = false
                                                            activeSubmenu = VisualizerSubmenu.NONE
                                                        }
                                                        .background(if (selected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                        .padding(horizontal = 14.dp, vertical = 7.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text(
                                                        text = mode.label,
                                                        color = if (selected) Color(0xFF00E5FF) else Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                    )
                                                    if (selected) {
                                                        Icon(
                                                            Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00E5FF),
                                                            modifier = Modifier.size(15.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        VisualizerSubmenu.PALETTES -> {
                                            ImmersiveColorPalette.entries.forEach { pal ->
                                                val palSelected = colorPalette == pal
                                                val dotColor = if (pal == ImmersiveColorPalette.DYNAMIC_ARTWORK && artworkPalette.colors.isNotEmpty()) {
                                                    artworkPalette.colors[0]
                                                } else {
                                                    pal.primary
                                                }
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            colorPalette = pal
                                                            DesktopPersistence.saveImmersiveColorPalette(pal.name)
                                                            menuExpanded = false
                                                            activeSubmenu = VisualizerSubmenu.NONE
                                                        }
                                                        .background(if (palSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                                        .padding(horizontal = 14.dp, vertical = 7.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(10.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor),
                                                        )
                                                        Spacer(Modifier.width(10.dp))
                                                        Text(
                                                            text = pal.label,
                                                            color = if (palSelected) Color(0xFF00E5FF) else Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (palSelected) FontWeight.Bold else FontWeight.Normal,
                                                        )
                                                    }
                                                    if (palSelected) {
                                                        Icon(
                                                            Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = Color(0xFF00E5FF),
                                                            modifier = Modifier.size(15.dp),
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        VisualizerSubmenu.SETTINGS -> {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val next = !classicEffectsMode
                                                        classicEffectsMode = next
                                                        DesktopPersistence.saveImmersiveClassicMode(next)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "Efectos clásicos",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                )
                                                Switch(
                                                    checked = classicEffectsMode,
                                                    onCheckedChange = {
                                                        classicEffectsMode = it
                                                        DesktopPersistence.saveImmersiveClassicMode(it)
                                                    },
                                                    modifier = Modifier.scale(0.75f),
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onAutomixChange(!automix) }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "Modo DJ Automix",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                )
                                                Switch(
                                                    checked = automix,
                                                    onCheckedChange = onAutomixChange,
                                                    modifier = Modifier.scale(0.75f),
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val next = !showLyricsInWinamp
                                                        showLyricsInWinamp = next
                                                        DesktopPersistence.saveImmersiveShowLyrics(next)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "Letras sincronizadas",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                )
                                                Switch(
                                                    checked = showLyricsInWinamp,
                                                    onCheckedChange = {
                                                        showLyricsInWinamp = it
                                                        DesktopPersistence.saveImmersiveShowLyrics(it)
                                                    },
                                                    modifier = Modifier.scale(0.75f),
                                                )
                                            }

                                            val isKaraokeFeatureOn by DesktopKaraokeSettings.featureEnabled.collectAsState()
                                            if (isKaraokeFeatureOn) {
                                                val isKaraokeActive by DesktopKaraokeSettings.isActive.collectAsState()
                                                val vocalAtt by DesktopKaraokeSettings.attenuation.collectAsState()
                                                val cpuAlert by DesktopKaraokeSettings.cpuAlert.collectAsState()

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            DesktopKaraokeSettings.setActive(!isKaraokeActive)
                                                        }
                                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            Icons.Rounded.Mic,
                                                            contentDescription = null,
                                                            tint = if (isKaraokeActive) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(15.dp),
                                                        )
                                                        Spacer(Modifier.width(8.dp))
                                                        Text(
                                                            text = "Modo Karaoke",
                                                            color = if (isKaraokeActive) Color(0xFF00E5FF) else Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isKaraokeActive) FontWeight.Bold else FontWeight.Normal,
                                                        )
                                                    }
                                                    Switch(
                                                        checked = isKaraokeActive,
                                                        onCheckedChange = { DesktopKaraokeSettings.setActive(it) },
                                                        modifier = Modifier.scale(0.75f),
                                                    )
                                                }

                                                if (isKaraokeActive) {
                                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically,
                                                        ) {
                                                            Text(
                                                                text = "Atenuación vocal",
                                                                color = Color.White.copy(alpha = 0.7f),
                                                                fontSize = 10.5.sp,
                                                            )
                                                            Text(
                                                                text = "${(vocalAtt * 100).toInt()}%",
                                                                color = Color(0xFF00E5FF),
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                        }
                                                        DesktopBareSlider(
                                                            value = vocalAtt,
                                                            onValueChange = { DesktopKaraokeSettings.setAttenuation(it) },
                                                            valueRange = 0f..1f,
                                                            modifier = Modifier.fillMaxWidth().height(18.dp),
                                                        )
                                                        if (cpuAlert) {
                                                            Text(
                                                                text = "Aviso: CPU > 25%. Reduce atenuación o apaga karaoke.",
                                                                color = Color(255, 170, 70),
                                                                fontSize = 9.5.sp,
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            HorizontalDivider(
                                                color = Color.White.copy(alpha = 0.08f),
                                                modifier = Modifier.padding(vertical = 4.dp),
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val next = !avoidDuplicateVersions
                                                        avoidDuplicateVersions = next
                                                        DesktopTrackSimilarity.avoidDuplicateVersions = next
                                                        DesktopPersistence.saveAvoidDuplicateVersions(next)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "Evitar versiones repetidas",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                )
                                                Switch(
                                                    checked = avoidDuplicateVersions,
                                                    onCheckedChange = {
                                                        avoidDuplicateVersions = it
                                                        DesktopTrackSimilarity.avoidDuplicateVersions = it
                                                        DesktopPersistence.saveAvoidDuplicateVersions(it)
                                                    },
                                                    modifier = Modifier.scale(0.75f),
                                                )
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        val next = !allowRemixesAndLive
                                                        allowRemixesAndLive = next
                                                        DesktopTrackSimilarity.allowRemixesAndLive = next
                                                        DesktopPersistence.saveAllowRemixesAndLive(next)
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                            ) {
                                                Text(
                                                    text = "Permitir remixes/en vivo",
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                )
                                                Switch(
                                                    checked = allowRemixesAndLive,
                                                    onCheckedChange = {
                                                        allowRemixesAndLive = it
                                                        DesktopTrackSimilarity.allowRemixesAndLive = it
                                                        DesktopPersistence.saveAllowRemixesAndLive(it)
                                                    },
                                                    modifier = Modifier.scale(0.75f),
                                                )
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dedicated Floating Glass Queue Panel on the right (Point B.6)
        if (showQueuePanel) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp, top = 70.dp, bottom = 20.dp)
                    .widthIn(min = 340.dp, max = 400.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xD9111420),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.10f),
                        ),
                    ),
                ),
                shadowElevation = 28.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.06f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.20f),
                                ),
                            ),
                        ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                    ) {
                        val upcoming = remember(queue, queueIndex) {
                            if (queue.isEmpty()) emptyList()
                            else queue.drop((queueIndex + 1).coerceAtMost(queue.size))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.QueueMusic,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Cola de reproducción",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "${upcoming.size} en espera • Atajo: Q",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showQueuePanel = false },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Cerrar (Q)",
                                    tint = Color.White.copy(alpha = 0.75f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Currently playing card inside the floating glass queue
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.30f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                ) {
                                    DesktopArtwork(
                                        url = song.thumbnailUrl,
                                        modifier = Modifier.fillMaxSize(),
                                        px = 80,
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SONANDO AHORA",
                                        color = Color(0xFF00E5FF),
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                        Spacer(Modifier.height(8.dp))

                        if (upcoming.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No hay más canciones en la cola",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                itemsIndexed(
                                    items = upcoming,
                                    key = { offset, item -> "${item.queueEntryId ?: item.videoId}_$offset" },
                                ) { offset, songItem ->
                                    val realIndex = queueIndex + 1 + offset
                                    Surface(
                                        onClick = { onPlayQueueIndex(realIndex) },
                                        shape = RoundedCornerShape(11.dp),
                                        color = Color.White.copy(alpha = 0.05f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 10.dp, end = 4.dp, top = 7.dp, bottom = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(6.dp)),
                                            ) {
                                                DesktopArtwork(
                                                    url = songItem.thumbnailUrl,
                                                    modifier = Modifier.fillMaxSize(),
                                                    px = 80,
                                                )
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = songItem.title,
                                                    color = Color.White,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = songItem.artist,
                                                    color = Color.White.copy(alpha = 0.60f),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            val dur = songItem.durationText
                                            if (!dur.isNullOrBlank()) {
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = dur,
                                                    color = Color.White.copy(alpha = 0.45f),
                                                    fontSize = 10.5.sp,
                                                )
                                            }
                                            // Reorder Up / Down + Remove buttons
                                            IconButton(
                                                onClick = { if (offset > 0) onMoveQueueItem(realIndex, realIndex - 1) },
                                                enabled = offset > 0,
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.KeyboardArrowUp,
                                                    contentDescription = "Subir en la cola",
                                                    tint = if (offset > 0) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.20f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (offset < upcoming.lastIndex) onMoveQueueItem(realIndex, realIndex + 1)
                                                },
                                                enabled = offset < upcoming.lastIndex,
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.KeyboardArrowDown,
                                                    contentDescription = "Bajar en la cola",
                                                    tint = if (offset < upcoming.lastIndex) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.20f),
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveQueueIndex(realIndex) },
                                                modifier = Modifier.size(24.dp),
                                            ) {
                                                Icon(
                                                    Icons.Rounded.Close,
                                                    contentDescription = "Quitar de la cola",
                                                    tint = Color.White.copy(alpha = 0.60f),
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Real-time 60fps Winamp-inspired audio visualizer backdrop.
 * Renders multi-harmonic oscilloscope neon waves, classic segmented LED spectrum bars
 * with gravity-falling peak-hold caps, MilkDrop psychedelic plasma waves, 3D Retro Warp Tunnel,
 * Analog Laser Lissajous, Relativistic HyperSpace Warp, 3D Retro Wireframe Sphere, Fire Sparks Rain,
 * and Starburst Contour Vortex.
 */
@Composable
private fun WinampVisualizerBackdrop(
    mode: ImmersiveVisualMode,
    palette: VisualizerPalette,
    classicMode: Boolean,
    isPlaying: Boolean,
    songSeed: Int,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    val targetFps by DesktopGpuSettings.targetFps.collectAsState()
    val isGpuEnabled by DesktopGpuSettings.enabled.collectAsState()
    val renderActive by DesktopWindowVisibility.renderActive.collectAsState()
    val animate = !DesktopPlatform.isWindows || (isPlaying && renderActive)

    DisposableEffect(Unit) {
        onDispose {
            DesktopGpuVisualizerEngine.release()
        }
    }

    val numBands = 56
    val barValues = remember { FloatArray(numBands) { 0.15f } }
    val peakValues = remember { FloatArray(numBands) { 0.20f } }
    val peakVelocities = remember { FloatArray(numBands) { 0f } }

    LaunchedEffect(isPlaying, mode, songSeed, targetFps, animate) {
        if (!animate) return@LaunchedEffect
        var lastNanos = 0L
        val minIntervalNanos = 1_000_000_000L / targetFps.coerceIn(30, 120)
        while (true) {
            withFrameNanos { nanos ->
                if (lastNanos != 0L && (nanos - lastNanos) < minIntervalNanos) {
                    return@withFrameNanos
                }
                val dt = if (lastNanos == 0L) 0.016f else ((nanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = nanos
                timeSec += if (isPlaying) dt else dt * 0.18f

                val seedOffset = (songSeed and 0xFFFF) * 0.013f
                val beatPulse = if (isPlaying) {
                    val b1 = sin(timeSec * 7.2f + seedOffset).coerceAtLeast(0f)
                    val b2 = sin(timeSec * 3.6f + seedOffset * 0.5f).coerceAtLeast(0f)
                    0.35f + 0.65f * (b1 * b1 * 0.65f + b2 * 0.35f)
                } else {
                    0.12f
                }

                for (i in 0 until numBands) {
                    val norm = i.toFloat() / numBands.toFloat()
                    // Shape like a real musical spectrum (strong sub-bass/kick, lively mids, crisp highs)
                    val freqWeight = when {
                        norm < 0.18f -> 1.05f - norm * 1.2f
                        norm < 0.65f -> 0.78f - (norm - 0.18f) * 0.35f
                        else -> 0.58f - (norm - 0.65f) * 0.6f
                    }
                    val osc1 = sin(timeSec * (5.5f + i * 0.37f) + i * 0.61f + seedOffset)
                    val osc2 = cos(timeSec * (8.3f + (numBands - i) * 0.23f) - i * 0.43f)
                    val osc3 = sin(timeSec * 13.7f + i * 1.15f)
                    val raw = ((osc1 * 0.45f + osc2 * 0.35f + osc3 * 0.20f) * 0.5f + 0.5f)
                    val target = if (isPlaying) {
                        (raw * freqWeight * beatPulse * 1.35f).coerceIn(0.06f, 0.98f)
                    } else {
                        (0.05f + 0.03f * sin(timeSec * 2f + i * 0.3f)).coerceIn(0.03f, 0.12f)
                    }

                    // Fast attack, smooth decay like Winamp's analyzer
                    barValues[i] = if (target > barValues[i]) {
                        barValues[i] + (target - barValues[i]) * 0.55f
                    } else {
                        (barValues[i] - dt * 1.45f).coerceAtLeast(target)
                    }

                    // Classic Winamp Peak-Hold dot with gravity
                    if (barValues[i] >= peakValues[i]) {
                        peakValues[i] = barValues[i]
                        peakVelocities[i] = 0f
                    } else {
                        peakVelocities[i] += dt * 0.68f
                        peakValues[i] = (peakValues[i] - peakVelocities[i] * dt).coerceAtLeast(barValues[i])
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val t = timeSec

        // Deep retro-cyber grid glow backdrop tailored to active palette
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.10f),
                    Color(0xFF090A14),
                    Color(0xFF040408),
                ),
                center = Offset(w * 0.5f, h * 0.5f),
                radius = w.coerceAtLeast(h) * 0.75f,
            ),
        )

        when (mode) {
            ImmersiveVisualMode.WINAMP_WAVES -> {
                // 1. Multi-harmonic glowing Winamp Oscilloscope Waves across the upper/middle stage
                val waveColors = listOf(
                    palette.primary,
                    palette.secondary,
                    palette.tertiary,
                    palette.accent,
                )
                val steps = if (classicMode) 90 else 160
                val dx = w / steps.toFloat()
                val centerY = h * 0.42f
                val ampBase = if (isPlaying) h * 0.16f else h * 0.03f

                waveColors.forEachIndexed { wIdx, color ->
                    val path = Path()
                    val phaseShift = wIdx * 1.15f
                    val speed = 3.2f + wIdx * 0.75f
                    for (s in 0..steps) {
                        val x = s * dx
                        val nx = s.toFloat() / steps.toFloat()
                        val bandIdx = (nx * (numBands - 1)).toInt().coerceIn(0, numBands - 1)
                        val energy = barValues[bandIdx]
                        val env = sin(nx * PI.toFloat())
                        val y = centerY +
                            env * ampBase * (0.4f + energy * 0.9f) * (
                                sin(nx * 10f + t * speed + phaseShift) * 0.6f +
                                    cos(nx * 23f - t * (speed * 1.3f) + wIdx) * 0.4f
                                )
                        if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    // Outer neon bloom
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.24f),
                        style = Stroke(width = if (classicMode) 6f else 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    // Core oscilloscope beam
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.92f),
                        style = Stroke(width = if (classicMode) 1.8f else 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // 2. Classic Winamp Segmented LED Spectrum Analyzer with Peak-Hold Dots along bottom
                drawWinampSpectrumBars(
                    barValues = barValues,
                    peakValues = peakValues,
                    numBands = numBands,
                    topY = h * 0.56f,
                    bottomY = h * 0.92f,
                    reflectionHeight = h * 0.06f,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_SPECTRUM -> {
                // Top Mini Oscilloscope Ribbon
                val steps = if (classicMode) 80 else 140
                val dx = w / steps.toFloat()
                val wavePath = Path()
                val waveY = h * 0.20f
                for (s in 0..steps) {
                    val x = s * dx
                    val nx = s.toFloat() / steps.toFloat()
                    val bIdx = (nx * (numBands - 1)).toInt().coerceIn(0, numBands - 1)
                    val dy = sin(nx * 18f + t * 5f) * barValues[bIdx] * (h * 0.08f)
                    if (s == 0) wavePath.moveTo(x, waveY + dy) else wavePath.lineTo(x, waveY + dy)
                }
                drawPath(
                    path = wavePath,
                    color = palette.primary.copy(alpha = 0.30f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wavePath,
                    color = palette.accent,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )

                // Full-Stage Giant Classic Winamp LED Spectrum Analyzer
                drawWinampSpectrumBars(
                    barValues = barValues,
                    peakValues = peakValues,
                    numBands = numBands,
                    topY = h * 0.26f,
                    bottomY = h * 0.88f,
                    reflectionHeight = h * 0.09f,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_MILKDROP -> {
                // MilkDrop / Geiss Psychedelic Plasma & Bass Shockwaves
                val cx = w * 0.5f
                val cy = h * 0.5f
                val bassEnergy = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
                val maxR = w.coerceAtLeast(h) * 0.55f

                // Concentric bass-reactive neon plasma rings
                for (rIdx in 0 until 7) {
                    val frac = ((t * 0.22f + rIdx / 7f) % 1f)
                    val radius = (0.08f + frac * 0.92f) * maxR * (0.85f + bassEnergy * 0.30f)
                    val hueColor = when (rIdx % 4) {
                        0 -> palette.primary
                        1 -> palette.secondary
                        2 -> palette.tertiary
                        else -> palette.accent
                    }
                    val ringPath = Path()
                    val pts = if (classicMode) 48 else 96
                    for (p in 0..pts) {
                        val angle = (p.toFloat() / pts.toFloat()) * (2f * PI.toFloat())
                        val bIdx = ((p.toFloat() / pts.toFloat()) * (numBands - 1)).toInt()
                        val wobble = 1f + 0.18f * barValues[bIdx] * sin(angle * 6f + t * 3f + rIdx)
                        val px = cx + cos(angle) * radius * wobble
                        val py = cy + sin(angle) * radius * wobble
                        if (p == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
                    }
                    drawPath(
                        path = ringPath,
                        color = hueColor.copy(alpha = (1f - frac) * 0.55f),
                        style = Stroke(width = 3.5f + (1f - frac) * 4f, cap = StrokeCap.Round),
                    )
                }

                // Flowing Lissajous / Aurora Plasma Ribbons
                val ribbonColors = listOf(
                    palette.primary,
                    palette.secondary,
                    palette.tertiary,
                    palette.accent,
                )
                val steps = if (classicMode) 80 else 150
                val dx = w / steps.toFloat()
                ribbonColors.forEachIndexed { idx, col ->
                    val p = Path()
                    for (s in 0..steps) {
                        val x = s * dx
                        val nx = s.toFloat() / steps.toFloat()
                        val y = cy +
                            sin(nx * 8f + t * (2.2f + idx * 0.5f) + idx) * (h * 0.22f) * (0.5f + bassEnergy * 0.6f) +
                            cos(nx * 15f - t * 3.1f + idx * 2f) * (h * 0.10f)
                        if (s == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    drawPath(p, col.copy(alpha = 0.22f), style = Stroke(width = 14f, cap = StrokeCap.Round))
                    drawPath(p, col.copy(alpha = 0.85f), style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                }
            }

            ImmersiveVisualMode.WINAMP_TUNNEL -> {
                drawWinampTunnel(
                    barValues = barValues,
                    peakValues = peakValues,
                    timeSec = t,
                    numBands = numBands,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_LASER -> {
                drawWinampLaser(
                    barValues = barValues,
                    timeSec = t,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_WARP -> {
                drawWinampWarp(
                    barValues = barValues,
                    timeSec = t,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_SPHERE -> {
                drawWinampSphere(
                    barValues = barValues,
                    peakValues = peakValues,
                    timeSec = t,
                    numBands = numBands,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_FIRE_SPARKS -> {
                drawWinampFireSparks(
                    barValues = barValues,
                    peakValues = peakValues,
                    timeSec = t,
                    numBands = numBands,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.WINAMP_STARBURST -> {
                drawWinampStarburst(
                    barValues = barValues,
                    peakValues = peakValues,
                    timeSec = t,
                    numBands = numBands,
                    isPlaying = isPlaying,
                    palette = palette,
                    classicMode = classicMode,
                )
            }

            ImmersiveVisualMode.GPU_NEON_TUNNEL -> {
                var drawn = false
                val bass = (0..4).map { barValues[it] }.average().toFloat()
                val mids = (5..24).map { barValues[it] }.average().toFloat()
                val treble = (25 until numBands).map { barValues[it] }.average().toFloat()
                if (isGpuEnabled) {
                    drawIntoCanvas { composeCanvas ->
                        drawn = DesktopGpuVisualizerEngine.drawGpuVisualizer(
                            skiaCanvas = composeCanvas.nativeCanvas,
                            style = GpuVisualizerStyle.NEON_TUNNEL,
                            width = w,
                            height = h,
                            timeSec = t,
                            bass = bass,
                            mids = mids,
                            treble = treble,
                            primary = palette.primary,
                            secondary = palette.secondary,
                            accent = palette.accent,
                        )
                    }
                }
                if (!drawn) {
                    drawWinampTunnel(
                        barValues = barValues,
                        peakValues = peakValues,
                        timeSec = t,
                        numBands = numBands,
                        isPlaying = isPlaying,
                        palette = palette,
                        classicMode = classicMode,
                    )
                }
            }

            ImmersiveVisualMode.GPU_WATER_SURFACE -> {
                var drawn = false
                val bass = (0..4).map { barValues[it] }.average().toFloat()
                val mids = (5..24).map { barValues[it] }.average().toFloat()
                val treble = (25 until numBands).map { barValues[it] }.average().toFloat()
                if (isGpuEnabled) {
                    drawIntoCanvas { composeCanvas ->
                        drawn = DesktopGpuVisualizerEngine.drawGpuVisualizer(
                            skiaCanvas = composeCanvas.nativeCanvas,
                            style = GpuVisualizerStyle.WATER_SURFACE,
                            width = w,
                            height = h,
                            timeSec = t,
                            bass = bass,
                            mids = mids,
                            treble = treble,
                            primary = palette.primary,
                            secondary = palette.secondary,
                            accent = palette.accent,
                        )
                    }
                }
                if (!drawn) {
                    drawWinampLaser(
                        barValues = barValues,
                        timeSec = t,
                        isPlaying = isPlaying,
                        palette = palette,
                        classicMode = classicMode,
                    )
                }
            }

            ImmersiveVisualMode.GPU_COSMIC_PARTICLES -> {
                var drawn = false
                val bass = (0..4).map { barValues[it] }.average().toFloat()
                val mids = (5..24).map { barValues[it] }.average().toFloat()
                val treble = (25 until numBands).map { barValues[it] }.average().toFloat()
                if (isGpuEnabled) {
                    drawIntoCanvas { composeCanvas ->
                        drawn = DesktopGpuVisualizerEngine.drawGpuVisualizer(
                            skiaCanvas = composeCanvas.nativeCanvas,
                            style = GpuVisualizerStyle.COSMIC_PARTICLES,
                            width = w,
                            height = h,
                            timeSec = t,
                            bass = bass,
                            mids = mids,
                            treble = treble,
                            primary = palette.primary,
                            secondary = palette.secondary,
                            accent = palette.accent,
                        )
                    }
                }
                if (!drawn) {
                    drawWinampWarp(
                        barValues = barValues,
                        timeSec = t,
                        isPlaying = isPlaying,
                        palette = palette,
                        classicMode = classicMode,
                    )
                }
            }

            ImmersiveVisualMode.APPLE_LYRICS -> Unit
        }

        // Classic Nostalgia Scanlines overlay across the visualizer canvas
        if (classicMode) {
            var sy = 0f
            while (sy < h) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(0f, sy),
                    end = Offset(w, sy),
                    strokeWidth = 1.2f,
                )
                sy += 3.5f
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampSpectrumBars(
    barValues: FloatArray,
    peakValues: FloatArray,
    numBands: Int,
    topY: Float,
    bottomY: Float,
    reflectionHeight: Float,
    palette: VisualizerPalette = VisualizerPalette.Default,
    classicMode: Boolean = false,
) {
    val w = size.width
    val horizontalPad = w * 0.04f
    val usableW = w - horizontalPad * 2f
    val slotW = usableW / numBands.toFloat()
    val barW = (slotW * 0.72f).coerceAtLeast(4f)
    val maxBarH = (bottomY - topY).coerceAtLeast(60f)
    val segments = 24
    val segGap = 2.5f
    val segH = ((maxBarH / segments.toFloat()) - segGap).coerceAtLeast(3f)

    for (i in 0 until numBands) {
        val x = horizontalPad + i * slotW + (slotW - barW) * 0.5f
        val level = barValues[i].coerceIn(0f, 1f)
        val activeSegs = (level * segments).toInt()

        // Segmented LED Bar
        for (s in 0 until activeSegs) {
            val segFrac = s.toFloat() / segments.toFloat()
            val segColor = if (classicMode) {
                when {
                    segFrac < 0.50f -> Color(0xFF00FF66) // Classic Winamp Green
                    segFrac < 0.75f -> Color(0xFFC6FF00) // Lime-Yellow
                    segFrac < 0.88f -> Color(0xFFFFAB00) // Warm Amber-Orange
                    else -> Color(0xFFFF1744) // Peak Red
                }
            } else {
                when {
                    segFrac < 0.38f -> palette.primary
                    segFrac < 0.72f -> palette.secondary
                    segFrac < 0.88f -> palette.tertiary
                    else -> palette.accent
                }
            }
            val segY = bottomY - (s + 1) * (segH + segGap)
            if (classicMode) {
                drawRect(
                    color = segColor.copy(alpha = 0.92f),
                    topLeft = Offset(x, segY),
                    size = Size(barW, segH),
                )
            } else {
                drawRoundRect(
                    color = segColor.copy(alpha = 0.88f),
                    topLeft = Offset(x, segY),
                    size = Size(barW, segH),
                    cornerRadius = CornerRadius(2f, 2f),
                )
            }
        }

        // Classic Winamp Falling Peak-Hold Cap
        val peakSeg = (peakValues[i].coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)
        val peakY = bottomY - (peakSeg + 1) * (segH + segGap)
        val capColor = if (classicMode) palette.accent else Color(0xFFE0F7FA)
        if (classicMode) {
            drawRect(
                color = capColor,
                topLeft = Offset(x, peakY),
                size = Size(barW, segH.coerceAtMost(4f)),
            )
        } else {
            drawRoundRect(
                color = capColor,
                topLeft = Offset(x, peakY),
                size = Size(barW, segH.coerceAtMost(5f)),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }

        // Subtle Floor Reflection
        if (reflectionHeight > 0f && activeSegs > 0) {
            val reflH = (level * reflectionHeight).coerceAtLeast(2f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        palette.primary.copy(alpha = 0.22f),
                        Color.Transparent,
                    ),
                    startY = bottomY + 4f,
                    endY = bottomY + 4f + reflH,
                ),
                topLeft = Offset(x, bottomY + 4f),
                size = Size(barW, reflH),
            )
        }
    }
}

/**
 * 3D Retro Warp Tunnel visualizer inspired by vintage Winamp AVS and MilkDrop.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampTunnel(
    barValues: FloatArray,
    peakValues: FloatArray,
    timeSec: Float,
    numBands: Int,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val maxR = (w.coerceAtLeast(h) * 0.75f).coerceAtLeast(200f)

    val numRings = 16
    val numSpokes = if (classicMode) 12 else 16
    val speed = if (isPlaying) 0.52f + bass * 0.35f else 0.12f
    val rot = timeSec * (if (isPlaying) 0.32f else 0.08f)

    val tunnelColors = listOf(
        palette.primary,
        palette.secondary,
        palette.tertiary,
        palette.accent,
        Color(0xFFFFAB00),
    )

    val vertexGrid = Array(numRings) { arrayOfNulls<Offset>(numSpokes) }

    for (r in 0 until numRings) {
        val baseFrac = r.toFloat() / numRings.toFloat()
        val z = ((baseFrac - (timeSec * speed * 0.25f)) % 1f + 1f) % 1f
        val radiusFrac = z * z
        val radius = 18f + radiusFrac * maxR

        val alpha = when {
            z < 0.12f -> (z / 0.12f).coerceIn(0f, 1f) * 0.45f
            z > 0.85f -> ((1f - z) / 0.15f).coerceIn(0f, 1f) * 0.85f
            else -> 0.85f
        }

        val colorIdx = (r + (timeSec * 0.6f).toInt()) % tunnelColors.size
        val ringColor = tunnelColors[colorIdx]

        val ringPath = Path()
        for (s in 0 until numSpokes) {
            val angle = (s.toFloat() / numSpokes.toFloat()) * (2f * PI.toFloat()) + rot
            val bandIdx = ((s.toFloat() / numSpokes.toFloat()) * (numBands - 1)).toInt().coerceIn(0, numBands - 1)
            val audioLevel = barValues[bandIdx]

            val wobble = 1f + (if (isPlaying) 0.22f * audioLevel * sin(angle * 4f + timeSec * 6f) else 0.04f * sin(angle * 2f))
            val px = cx + cos(angle) * radius * wobble
            val py = cy + sin(angle) * radius * wobble

            val pt = Offset(px, py)
            vertexGrid[r][s] = pt
            if (s == 0) ringPath.moveTo(px, py) else ringPath.lineTo(px, py)
        }
        ringPath.close()

        drawPath(
            path = ringPath,
            color = ringColor.copy(alpha = alpha * 0.28f),
            style = Stroke(width = if (classicMode) 3f else (4f + radiusFrac * 6f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = ringPath,
            color = ringColor.copy(alpha = alpha),
            style = Stroke(width = if (classicMode) 1.4f else (1.5f + radiusFrac * 2.2f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    for (s in 0 until numSpokes) {
        val spokePath = Path()
        var started = false
        for (r in 0 until numRings) {
            val pt = vertexGrid[r][s] ?: continue
            if (!started) {
                spokePath.moveTo(pt.x, pt.y)
                started = true
            } else {
                spokePath.lineTo(pt.x, pt.y)
            }
        }
        val spokeColor = tunnelColors[s % tunnelColors.size]
        drawPath(
            path = spokePath,
            color = spokeColor.copy(alpha = 0.22f + bass * 0.25f),
            style = Stroke(width = 1.4f, cap = StrokeCap.Round),
        )
    }

    val coreRadius = (16f + bass * 38f).coerceAtLeast(10f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                palette.primary.copy(alpha = 0.65f),
                palette.secondary.copy(alpha = 0.25f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = coreRadius * 2.5f,
        ),
        radius = coreRadius * 2.5f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color.White,
        radius = (coreRadius * 0.35f).coerceAtLeast(3f),
        center = Offset(cx, cy),
    )
}

/**
 * Analog Vector Laser Oscilloscope inspired by classic vector arcade CRT monitors and audio XY analyzers.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampLaser(
    barValues: FloatArray,
    timeSec: Float,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val mid = (barValues[4] + barValues[5] + barValues[6] + barValues[7]) * 0.25f
    val treble = (barValues[8] + barValues[9] + barValues[10] + barValues[11]) * 0.25f

    // 1. CRT Phosphor Reticle Grid
    val gridColor = palette.primary.copy(alpha = 0.16f)
    val gridStep = 48f
    var gx = cx % gridStep
    while (gx < w) {
        drawLine(
            color = gridColor,
            start = Offset(gx, 0f),
            end = Offset(gx, h),
            strokeWidth = 1f,
        )
        gx += gridStep
    }
    var gy = cy % gridStep
    while (gy < h) {
        drawLine(
            color = gridColor,
            start = Offset(0f, gy),
            end = Offset(w, gy),
            strokeWidth = 1f,
        )
        gy += gridStep
    }

    // Concentric calibration rings
    val maxRadius = minOf(cx, cy) * 0.82f
    listOf(0.33f, 0.66f, 1.0f).forEach { frac ->
        drawCircle(
            color = gridColor.copy(alpha = 0.50f),
            radius = maxRadius * frac,
            center = Offset(cx, cy),
            style = Stroke(width = 1f),
        )
    }

    // 2. Phosphor Laser Lissajous Beam
    val numPoints = if (classicMode) 140 else 280
    val pathOuter = Path()
    val pathInner = Path()

    val freqA = if (isPlaying) 2.0f + (mid * 3.0f).toInt().toFloat() else 2.0f
    val freqB = if (isPlaying) 3.0f + (treble * 2.0f).toInt().toFloat() else 3.0f
    val speed = if (isPlaying) 1.2f + bass * 1.5f else 0.4f
    val phase = timeSec * speed

    val baseRx = maxRadius * (0.55f + bass * 0.35f)
    val baseRy = maxRadius * (0.50f + mid * 0.30f)

    for (i in 0..numPoints) {
        val u = (i.toFloat() / numPoints.toFloat()) * (2f * PI.toFloat())
        val waveMod = if (isPlaying) sin(u * 12f + timeSec * 8f) * (treble * 24f) else 0f

        val lx = cx + cos(freqA * u + phase) * (baseRx + waveMod) +
            sin(u * 4f - timeSec * 2f) * (bass * 30f)
        val ly = cy + sin(freqB * u + phase * 0.7f) * (baseRy + waveMod) +
            cos(u * 3f + timeSec) * (mid * 25f)

        if (i == 0) pathOuter.moveTo(lx, ly) else pathOuter.lineTo(lx, ly)
    }

    for (i in 0..numPoints) {
        val u = (i.toFloat() / numPoints.toFloat()) * (2f * PI.toFloat())
        val waveMod = if (isPlaying) cos(u * 8f - timeSec * 6f) * (mid * 18f) else 0f

        val lx = cx + cos((freqB - 1f) * u - phase * 0.8f) * (baseRx * 0.68f + waveMod)
        val ly = cy + sin((freqA + 1f) * u + phase * 0.5f) * (baseRy * 0.68f + waveMod)

        if (i == 0) pathInner.moveTo(lx, ly) else pathInner.lineTo(lx, ly)
    }

    val primaryBeam = palette.primary
    val secondaryBeam = palette.secondary

    // Outer Beam Glow
    drawPath(pathOuter, primaryBeam.copy(alpha = 0.16f + bass * 0.12f), style = Stroke(width = if (classicMode) 8f else 16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(pathOuter, primaryBeam.copy(alpha = 0.55f), style = Stroke(width = if (classicMode) 3.5f else 5.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(pathOuter, Color.White.copy(alpha = 0.95f), style = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Inner Beam
    drawPath(pathInner, secondaryBeam.copy(alpha = 0.14f + mid * 0.10f), style = Stroke(width = if (classicMode) 6f else 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(pathInner, secondaryBeam.copy(alpha = 0.60f), style = Stroke(width = if (classicMode) 2.5f else 4.0f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(pathInner, Color.White.copy(alpha = 0.90f), style = Stroke(width = 1.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))

    // Center CRT Glow Point
    val centerGlow = (12f + bass * 28f).coerceAtLeast(8f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                primaryBeam.copy(alpha = 0.6f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = centerGlow * 2f,
        ),
        radius = centerGlow * 2f,
        center = Offset(cx, cy),
    )
}

/**
 * HyperSpace Starfield 3D Warp visualizer.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampWarp(
    barValues: FloatArray,
    timeSec: Float,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val treble = (barValues[8] + barValues[9] + barValues[10] + barValues[11]) * 0.25f

    val numStars = if (classicMode) 120 else 190
    val warpSpeed = if (isPlaying) 0.65f + bass * 1.10f else 0.18f
    val maxDist = maxOf(w, h) * 0.75f

    val streakColors = listOf(
        palette.primary,
        palette.secondary,
        palette.tertiary,
        palette.accent,
        Color.White,
    )

    for (i in 0 until numStars) {
        val seedAngle = ((i * 137.508f) % 360f) * (PI.toFloat() / 180f)
        val seedDist = (((i * 47) % 100) / 100f).coerceIn(0.08f, 1.0f)
        val seedOffset = ((i * 31) % 100) / 100f

        val z = ((seedOffset - (timeSec * warpSpeed * 0.35f)) % 1f + 1f) % 1f
        if (z < 0.02f) continue

        val dist = (1f - z) * (1f - z) * maxDist * seedDist
        val prevDist = ((1f - (z + 0.045f * warpSpeed)).coerceAtLeast(0f)).let { it * it * maxDist * seedDist }

        val sx = cx + cos(seedAngle) * dist
        val sy = cy + sin(seedAngle) * dist
        val prevX = cx + cos(seedAngle) * prevDist
        val prevY = cy + sin(seedAngle) * prevDist

        if (sx < -40f || sx > w + 40f || sy < -40f || sy > h + 40f) continue

        val starColor = streakColors[i % streakColors.size]
        val alpha = when {
            z > 0.85f -> ((1f - z) / 0.15f).coerceIn(0f, 1f) * 0.65f
            z < 0.15f -> (z / 0.15f).coerceIn(0f, 1f) * 0.95f
            else -> 0.95f
        }
        val streakWidth = (1.2f + (1f - z) * 3.5f + bass * 2.0f).coerceIn(1.2f, 7.0f)

        // Draw Motion Streak Line
        drawLine(
            color = starColor.copy(alpha = alpha),
            start = Offset(prevX, prevY),
            end = Offset(sx, sy),
            strokeWidth = if (classicMode) 1.5f else streakWidth,
            cap = StrokeCap.Round,
        )

        // Star head dot / retro cross
        if (classicMode) {
            drawRetroStarCross(Offset(sx, sy), armLength = 3f, color = Color.White.copy(alpha = alpha))
        } else {
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = (streakWidth * 0.65f).coerceAtLeast(1.5f),
                center = Offset(sx, sy),
            )
        }
    }

    // Bass Shockwave Ring
    if (bass > 0.15f) {
        val shockFrac = ((timeSec * 2.2f) % 1f)
        val shockRadius = shockFrac * maxDist * (0.6f + bass * 0.4f)
        val shockAlpha = (1f - shockFrac) * (0.45f + bass * 0.40f)
        drawCircle(
            color = palette.primary.copy(alpha = shockAlpha),
            radius = shockRadius,
            center = Offset(cx, cy),
            style = Stroke(width = (4f * (1f - shockFrac) + 1f).coerceAtLeast(1.5f)),
        )
    }

    // Hyperspace Singularity Core
    val coreR = (14f + bass * 35f + treble * 12f).coerceAtLeast(8f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                palette.primary.copy(alpha = 0.80f),
                palette.secondary.copy(alpha = 0.35f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = coreR * 3f,
        ),
        radius = coreR * 3f,
        center = Offset(cx, cy),
    )
    drawCircle(
        color = Color.White,
        radius = (coreR * 0.4f).coerceAtLeast(3f),
        center = Offset(cx, cy),
    )
}

/**
 * 3D Retro Wireframe Sphere visualizer (Image 3 inspired).
 * Features rotating latitude and longitude rings modulated by FFT frequency bands,
 * an orbiting satellite orb with glow, and a retro star cross constellation field.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampSphere(
    barValues: FloatArray,
    peakValues: FloatArray,
    timeSec: Float,
    numBands: Int,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val mid = (barValues[5] + barValues[6] + barValues[7] + barValues[8]) * 0.25f
    val treble = (barValues[10] + barValues[11] + barValues[12] + barValues[13]) * 0.25f

    // 1. Retro Star Cross Constellation Field in deep space background
    val numStars = 64
    for (i in 0 until numStars) {
        val sAngle = ((i * 137.5f) % 360f) * (PI.toFloat() / 180f)
        val sDist = (((i * 73) % 100) / 100f) * (maxOf(w, h) * 0.60f)
        val sx = cx + cos(sAngle) * sDist
        val sy = cy + sin(sAngle) * sDist
        val starAlpha = (0.25f + 0.50f * sin(timeSec * 2f + i)).coerceIn(0.15f, 0.85f)
        val starCol = if (i % 3 == 0) palette.accent else Color.White

        if (classicMode) {
            drawRetroStarCross(Offset(sx, sy), armLength = 3.5f, color = starCol.copy(alpha = starAlpha))
        } else {
            drawCircle(color = starCol.copy(alpha = starAlpha), radius = 1.6f, center = Offset(sx, sy))
        }
    }

    // 2. 3D Rotating Sphere Geometry
    val baseRadius = (minOf(w, h) * 0.28f) * (0.88f + bass * 0.24f)
    val rotY = timeSec * (if (isPlaying) 0.50f else 0.15f)
    val rotX = 0.42f + 0.12f * sin(timeSec * 0.28f)
    val cosRx = cos(rotX)
    val sinRx = sin(rotX)

    fun project3D(x: Float, y: Float, z: Float): Pair<Offset, Float> {
        val cosRy = cos(rotY)
        val sinRy = sin(rotY)
        val x1 = x * cosRy + z * sinRy
        val y1 = y
        val z1 = -x * sinRy + z * cosRy

        val x2 = x1
        val y2 = y1 * cosRx - z1 * sinRx
        val z2 = y1 * sinRx + z1 * cosRx

        val fov = 600f
        val scale = fov / (fov + z2)
        return Pair(Offset(cx + x2 * scale, cy + y2 * scale), z2)
    }

    // Latitude Rings (horizontal slices)
    val numLat = 9
    for (lat in 1 until numLat) {
        val phi = ((lat.toFloat() / numLat.toFloat()) - 0.5f) * PI.toFloat()
        val latR = baseRadius * cos(phi)
        val latY = baseRadius * sin(phi)
        val bandIdx = (lat * (numBands / numLat)).coerceIn(0, numBands - 1)
        val audioMod = 1f + (if (isPlaying) barValues[bandIdx] * 0.20f else 0f)

        val pts = if (classicMode) 24 else 48
        val path = Path()
        for (p in 0..pts) {
            val theta = (p.toFloat() / pts.toFloat()) * (2f * PI.toFloat())
            val px = latR * audioMod * cos(theta)
            val pz = latR * audioMod * sin(theta)
            val (screenPt, _) = project3D(px, latY, pz)
            if (p == 0) path.moveTo(screenPt.x, screenPt.y) else path.lineTo(screenPt.x, screenPt.y)
        }
        val latColor = if (lat == numLat / 2) palette.accent else palette.primary
        drawPath(
            path = path,
            color = latColor.copy(alpha = 0.70f),
            style = Stroke(width = if (lat == numLat / 2) 2.2f else 1.4f, cap = StrokeCap.Round),
        )
    }

    // Longitude Rings (meridians passing through poles)
    val numLong = 10
    for (lng in 0 until numLong) {
        val baseTheta = (lng.toFloat() / numLong.toFloat()) * PI.toFloat()
        val pts = if (classicMode) 28 else 56
        val path = Path()
        val bandIdx = (lng * (numBands / numLong)).coerceIn(0, numBands - 1)
        val audioMod = 1f + (if (isPlaying) peakValues[bandIdx] * 0.16f else 0f)

        for (p in 0..pts) {
            val phi = (p.toFloat() / pts.toFloat()) * (2f * PI.toFloat())
            val px = baseRadius * audioMod * cos(phi) * cos(baseTheta)
            val py = baseRadius * audioMod * sin(phi)
            val pz = baseRadius * audioMod * cos(phi) * sin(baseTheta)
            val (screenPt, _) = project3D(px, py, pz)
            if (p == 0) path.moveTo(screenPt.x, screenPt.y) else path.lineTo(screenPt.x, screenPt.y)
        }
        drawPath(
            path = path,
            color = palette.secondary.copy(alpha = 0.55f),
            style = Stroke(width = 1.3f, cap = StrokeCap.Round),
        )
    }

    // Orbiting Satellite Ring tilted around sphere
    val orbitRadius = baseRadius * (1.42f + mid * 0.15f)
    val orbitTilt = 0.55f
    val cosOt = cos(orbitTilt)
    val sinOt = sin(orbitTilt)
    val orbitPts = if (classicMode) 32 else 64
    val orbitPath = Path()
    for (p in 0..orbitPts) {
        val oAngle = (p.toFloat() / orbitPts.toFloat()) * (2f * PI.toFloat())
        val ox = orbitRadius * cos(oAngle)
        val oy = orbitRadius * sin(oAngle) * sinOt
        val oz = orbitRadius * sin(oAngle) * cosOt
        val (screenPt, _) = project3D(ox, oy, oz)
        if (p == 0) orbitPath.moveTo(screenPt.x, screenPt.y) else orbitPath.lineTo(screenPt.x, screenPt.y)
    }
    drawPath(
        path = orbitPath,
        color = palette.accent.copy(alpha = 0.40f),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round),
    )

    // Satellite Orb traveling on orbit
    val satAngle = timeSec * (if (isPlaying) 1.4f else 0.4f)
    val satX = orbitRadius * cos(satAngle)
    val satY = orbitRadius * sin(satAngle) * sinOt
    val satZ = orbitRadius * sin(satAngle) * cosOt
    val (satScreen, _) = project3D(satX, satY, satZ)

    val satGlow = (6f + treble * 14f).coerceAtLeast(4f)
    drawCircle(
        color = palette.accent.copy(alpha = 0.45f),
        radius = satGlow * 2.2f,
        center = satScreen,
    )
    drawCircle(
        color = Color.White,
        radius = (3f + treble * 4f).coerceAtLeast(2.5f),
        center = satScreen,
    )
}

/**
 * Falling Rain of Fire Sparks & Embers with reflective mirror floor (Image 4 inspired).
 * Features hundreds of descending glowing sparks, beat explosions, floor bounce ripples,
 * and vertical dark water reflections.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampFireSparks(
    barValues: FloatArray,
    peakValues: FloatArray,
    timeSec: Float,
    numBands: Int,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val floorY = h * 0.78f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val mid = (barValues[6] + barValues[7] + barValues[8]) / 3f
    val treble = (barValues[12] + barValues[13] + barValues[14]) / 3f

    // 1. Dark Mirror Water Floor Backdrop
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xCC060814),
                Color(0xEE0B0D18),
                Color(0xFF04040A),
            ),
            startY = floorY,
            endY = h,
        ),
        topLeft = Offset(0f, floorY),
        size = Size(w, h - floorY),
    )
    drawLine(
        color = palette.primary.copy(alpha = 0.35f),
        start = Offset(0f, floorY),
        end = Offset(w, floorY),
        strokeWidth = 1.5f,
    )

    // 2. Falling Sparks Simulation
    val numSparks = if (classicMode) 90 else 140
    val fallSpeed = if (isPlaying) 0.55f + bass * 0.85f else 0.18f

    for (i in 0 until numSparks) {
        val seedX = (((i * 89) % 1000) / 1000f) * w
        val seedOffset = ((i * 43) % 100) / 100f
        val sparkSpeed = 0.4f + (((i * 17) % 100) / 100f) * 0.8f

        // Progress from 0 (top of screen) to 1 (floor plane)
        val progress = ((seedOffset + timeSec * fallSpeed * sparkSpeed) % 1.0f)
        val sway = sin(timeSec * 3f + i * 1.7f) * (8f + mid * 18f)
        val sparkX = (seedX + sway).coerceIn(4f, w - 4f)
        val sparkY = progress * floorY

        val sparkColor = when (i % 4) {
            0 -> palette.primary
            1 -> palette.secondary
            2 -> palette.accent
            else -> Color(0xFFFFD54F)
        }

        val tailLen = (6f + progress * 24f + bass * 28f).coerceIn(6f, 50f)
        val alpha = when {
            progress < 0.10f -> (progress / 0.10f) * 0.85f
            progress > 0.85f -> ((1f - progress) / 0.15f) * 0.95f
            else -> 0.95f
        }

        // Draw Spark Trail
        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, sparkColor.copy(alpha = alpha)),
                startY = (sparkY - tailLen).coerceAtLeast(0f),
                endY = sparkY,
            ),
            start = Offset(sparkX, (sparkY - tailLen).coerceAtLeast(0f)),
            end = Offset(sparkX, sparkY),
            strokeWidth = if (classicMode) 1.5f else 2.5f,
            cap = StrokeCap.Round,
        )

        // Spark Head Dot / Cross
        if (classicMode) {
            drawRetroStarCross(Offset(sparkX, sparkY), armLength = 2.5f, color = Color.White.copy(alpha = alpha))
        } else {
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = 1.8f,
                center = Offset(sparkX, sparkY),
            )
        }

        // Floor Water Mirror Reflection
        val reflY = floorY + (floorY - sparkY) * 0.45f
        if (reflY < h) {
            val reflAlpha = alpha * 0.28f
            drawLine(
                color = sparkColor.copy(alpha = reflAlpha),
                start = Offset(sparkX, reflY),
                end = Offset(sparkX, reflY + tailLen * 0.35f),
                strokeWidth = if (classicMode) 1.2f else 2.0f,
            )
        }

        // Floor Impact Ripples (when spark hits floor)
        if (progress > 0.92f) {
            val rippleFrac = (progress - 0.92f) / 0.08f
            val rippleR = rippleFrac * 28f
            val rippleAlpha = (1f - rippleFrac) * 0.45f
            drawOval(
                color = sparkColor.copy(alpha = rippleAlpha),
                topLeft = Offset(sparkX - rippleR, floorY + 2f - rippleR * 0.25f),
                size = Size(rippleR * 2f, rippleR * 0.5f),
                style = Stroke(width = 1.2f),
            )
        }
    }

    // Treble Twinkle Sparks floating above floor
    if (isPlaying && treble > 0.2f) {
        val numTwinkles = 16
        for (t in 0 until numTwinkles) {
            val tx = (((t * 157) % 1000) / 1000f) * w
            val ty = floorY - 30f - (((t * 97) % 100) / 100f) * 120f
            val tAlpha = (sin(timeSec * 8f + t * 2f).coerceAtLeast(0f)) * treble * 0.8f
            if (classicMode) {
                drawRetroStarCross(Offset(tx, ty), armLength = 4f, color = palette.accent.copy(alpha = tAlpha))
            } else {
                drawCircle(color = palette.accent.copy(alpha = tAlpha), radius = 2.4f, center = Offset(tx, ty))
            }
        }
    }
}

/**
 * Inward Contour Starburst / Vortex Tunnel visualizer (Image 2 inspired).
 * Features concentric starburst contour rings deformed by FFT frequencies,
 * creating an inward vortex tunnel with sharp ridges and vintage bloom.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampStarburst(
    barValues: FloatArray,
    peakValues: FloatArray,
    timeSec: Float,
    numBands: Int,
    isPlaying: Boolean,
    palette: VisualizerPalette,
    classicMode: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val maxRadius = maxOf(w, h) * 0.72f

    val numRings = 16
    val ptsPerRing = if (classicMode) 32 else 54
    val speed = if (isPlaying) 0.45f + bass * 0.50f else 0.12f
    val rot = timeSec * (if (isPlaying) 0.22f else 0.06f)

    for (r in 0 until numRings) {
        val baseFrac = r.toFloat() / numRings.toFloat()
        // Rings advance inward toward center
        val z = ((baseFrac - timeSec * speed * 0.25f) % 1f + 1f) % 1f
        val radiusFrac = z * z
        val radius = 12f + radiusFrac * maxRadius

        val alpha = when {
            z < 0.08f -> (z / 0.08f) * 0.45f
            z > 0.88f -> ((1f - z) / 0.12f) * 0.85f
            else -> 0.88f
        }

        val ringColor = when (r % 4) {
            0 -> palette.primary
            1 -> palette.secondary
            2 -> palette.tertiary
            else -> palette.accent
        }

        val path = Path()
        for (p in 0..ptsPerRing) {
            val angle = (p.toFloat() / ptsPerRing.toFloat()) * (2f * PI.toFloat()) + rot + (r * 0.08f)
            val bandIdx = ((p.toFloat() / ptsPerRing.toFloat()) * (numBands - 1)).toInt().coerceIn(0, numBands - 1)
            val audioLevel = barValues[bandIdx]

            // Starburst undulation: 8 or 12 points contour deformation
            val starHarmonic = cos(angle * 8f) * (0.16f + audioLevel * 0.25f)
            val waveWobble = sin(angle * 16f - timeSec * 4f) * (0.06f * audioLevel)
            val totalMod = (1f + starHarmonic + waveWobble).coerceAtLeast(0.3f)

            val px = cx + cos(angle) * radius * totalMod
            val py = cy + sin(angle) * radius * totalMod

            if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        path.close()

        // Outer glow
        drawPath(
            path = path,
            color = ringColor.copy(alpha = alpha * 0.25f),
            style = Stroke(width = if (classicMode) 3f else (6f + radiusFrac * 4f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        // Core contour ridge
        drawPath(
            path = path,
            color = ringColor.copy(alpha = alpha),
            style = Stroke(width = if (classicMode) 1.4f else (2.0f + radiusFrac * 1.5f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }

    // Singularity Core at vanishing point
    val coreR = (12f + bass * 30f).coerceAtLeast(6f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White,
                palette.primary.copy(alpha = 0.70f),
                palette.secondary.copy(alpha = 0.25f),
                Color.Transparent,
            ),
            center = Offset(cx, cy),
            radius = coreR * 3f,
        ),
        radius = coreR * 3f,
        center = Offset(cx, cy),
    )
}

/**
 * Draws a sharp vintage 5-pixel star crossmark (+) for retro demoscene visuals.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRetroStarCross(
    center: Offset,
    armLength: Float,
    color: Color,
    strokeWidth: Float = 1.4f,
) {
    drawLine(
        color = color,
        start = Offset(center.x - armLength, center.y),
        end = Offset(center.x + armLength, center.y),
        strokeWidth = strokeWidth,
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - armLength),
        end = Offset(center.x, center.y + armLength),
        strokeWidth = strokeWidth,
    )
}

/** Left hero column with album art, song info, scrubber, and transport playback controls. */
@Composable
private fun ImmersiveHeroPlayer(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    position: PlaybackPosition,
    durationMs: Long,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    shuffle: Boolean,
    onShuffleChange: (Boolean) -> Unit,
    repeatMode: DesktopRepeatMode,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    automix: Boolean = false,
    onAutomixChange: (Boolean) -> Unit = {},
    djMixState: DjMixState = DjMixState(),
    isQueueVisible: Boolean = false,
    onToggleQueue: () -> Unit = {},
) {
    var draggingScrubber by remember(song.videoId) { mutableStateOf(false) }
    var scrubFraction by remember(song.videoId) { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember(song.videoId) { mutableStateOf<Long?>(null) }
    var pendingSeekAt by remember(song.videoId) { mutableStateOf(0L) }

    val settledSeekMs = pendingSeekMs?.takeIf { target ->
        val elapsed = System.currentTimeMillis() - pendingSeekAt
        elapsed < 650L && kotlin.math.abs(position.positionMs - target) > 1_200L
    }
    val liveProgress = if (durationMs > 0) {
        val ms = settledSeekMs ?: position.positionMs
        (ms.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val currentDisplayFraction = if (draggingScrubber) scrubFraction else liveProgress

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val artShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .size(265.dp)
                .shadow(24.dp, artShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(artShape)
                .graphicsLayer { alpha = 0.84f },
        ) {
            DesktopArtwork(
                url = song.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                px = 1024,
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = song.artist,
            color = Color.White.copy(alpha = 0.80f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        val album = song.albumName
        if (!album.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = album,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        // Fixed-height subtle container for Automix status (zero layout shift, cover never moves)
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (djMixState.isBlending) {
                val infiniteTransition = rememberInfiniteTransition(label = "djPulseImmersive")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(750, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "pulseAlphaImmersive",
                )
                Text(
                    text = "MEZCLANDO CON ${djMixState.incomingTitle.uppercase()} A ${djMixState.incomingBpm} BPM",
                    color = Color.White.copy(alpha = 0.50f * pulseAlpha),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // macOS Frosted Glass Playback Controls Card (Point B.5)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0x9E121521),
            border = BorderStroke(
                1.dp,
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.28f),
                        Color.White.copy(alpha = 0.09f),
                    ),
                ),
            ),
            shadowElevation = 22.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f),
                            ),
                        ),
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DesktopThinSlider(
                        value = currentDisplayFraction,
                        onValueChange = {
                            draggingScrubber = true
                            scrubFraction = it.coerceIn(0f, 1f)
                        },
                        onValueChangeFinished = {
                            if (durationMs > 0) {
                                val targetMs = (scrubFraction.toDouble() * durationMs.toDouble()).toLong().coerceIn(0L, durationMs)
                                pendingSeekMs = targetMs
                                pendingSeekAt = System.currentTimeMillis()
                                onSeek(targetMs)
                            }
                            draggingScrubber = false
                        },
                        tooltipText = { fraction ->
                            if (durationMs > 0) {
                                formatDuration((fraction.toDouble() * durationMs.toDouble()).toLong().coerceIn(0L, durationMs))
                            } else {
                                "0:00"
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(14.dp),
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.26f),
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        val displayMs = when {
                            draggingScrubber && durationMs > 0 -> (scrubFraction.toDouble() * durationMs.toDouble()).toLong()
                            settledSeekMs != null -> settledSeekMs
                            else -> position.positionMs
                        }
                        Text(
                            text = formatDuration(displayMs),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                        )
                        Text(
                            text = formatDuration(durationMs),
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 11.5.sp,
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { onShuffleChange(!shuffle) }) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = DesktopStrings["shuffle", "Aleatorio"],
                                tint = if (shuffle) DesktopAccent else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        DesktopToolbarButton(onClick = onPrevious, size = 42.dp) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = DesktopStrings["previous", "Anterior"],
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        Surface(
                            onClick = onPlayPause,
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 10.dp,
                            modifier = Modifier.size(56.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) DesktopStrings["pause", "Pausar"] else DesktopStrings["play", "Reproducir"],
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }

                        DesktopToolbarButton(onClick = onNext, size = 42.dp) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = DesktopStrings["next", "Siguiente"],
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextMode = when (repeatMode) {
                                    DesktopRepeatMode.OFF -> DesktopRepeatMode.ALL
                                    DesktopRepeatMode.ALL -> DesktopRepeatMode.ONE
                                    DesktopRepeatMode.ONE -> DesktopRepeatMode.OFF
                                }
                                onRepeatModeChange(nextMode)
                            },
                        ) {
                            Icon(
                                when (repeatMode) {
                                    DesktopRepeatMode.ONE -> Icons.Rounded.RepeatOne
                                    else -> Icons.Rounded.Repeat
                                },
                                contentDescription = DesktopStrings["repeat", "Repetir"],
                                tint = if (repeatMode != DesktopRepeatMode.OFF) DesktopAccent else Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        var showDjTooltip by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        when (event.type) {
                                            PointerEventType.Enter -> showDjTooltip = true
                                            PointerEventType.Exit -> showDjTooltip = false
                                        }
                                    }
                                }
                            },
                        ) {
                            IconButton(onClick = { onAutomixChange(!automix) }) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = DesktopStrings["automix", "Modo DJ Automix"],
                                    tint = if (automix) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.65f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            if (showDjTooltip) {
                                Popup(
                                    alignment = Alignment.TopCenter,
                                    offset = IntOffset(0, -32),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xF21C1C24),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                        shadowElevation = 6.dp,
                                    ) {
                                        Text(
                                            text = "Modo DJ Automix: Mezclas y transiciones automáticas",
                                            color = Color.White.copy(alpha = 0.92f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (volume <= 0.01f) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                            contentDescription = DesktopStrings["volume", "Volumen"],
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        DesktopBareSlider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        IconButton(
                            onClick = onToggleQueue,
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = DesktopStrings["queue", "Cola de reproducción (Q)"],
                                tint = if (isQueueVisible) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats the real measured audio quality of the active stream for the Immersive Mode header badge.
 * Never falls back to a fake static string; returns "N/D" if stream metadata is unavailable.
 */
internal fun formatDynamicStreamQuality(
    format: DesktopStreamFormat?,
    song: Song? = null,
): String {
    if (format != null) {
        val rawCodec = format.codec
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.removePrefix("AUDIO/")
            ?.substringBefore(".")
            ?.takeIf { it.isNotBlank() }

        val codecLabel = when (rawCodec) {
            "MP4A", "AAC", "M4A" -> "AAC"
            "VORBIS" -> "VORBIS"
            "OPUS" -> "OPUS"
            "FLAC" -> "FLAC"
            "ALAC" -> "ALAC"
            "PCM", "WAV", "S16LE", "F32LE" -> "PCM"
            "MP3", "MPEG" -> "MP3"
            else -> rawCodec
        }

        val depthOrRate = when {
            format.bitDepth != null && format.bitDepth > 0 -> "${format.bitDepth}-bit"
            format.kbps != null && format.kbps > 0 -> "${format.kbps} kbps"
            format.isLossless -> "Lossless"
            else -> null
        }

        val firstToken = listOfNotNull(codecLabel, depthOrRate).joinToString(" ").ifBlank { null }

        val sampleRateToken = format.sampleRateHz?.takeIf { it > 0 }?.let { hz ->
            if (hz % 1000 == 0) {
                "${hz / 1000} kHz"
            } else {
                "${"%.1f".format(Locale.US, hz / 1000.0)} kHz"
            }
        }

        val channelToken = when {
            format.isDolbyAtmos -> "DOLBY ATMOS"
            format.channels == 2 -> "STEREO"
            format.channels == 1 -> "MONO"
            (format.channels ?: 0) > 2 -> "${format.channels} CH"
            else -> null
        }

        val parts = listOfNotNull(firstToken, sampleRateToken, channelToken)
        if (parts.isNotEmpty()) {
            return parts.joinToString(" • ")
        }
    }

    val songQuality = song?.sourceQuality?.trim()?.takeIf { it.isNotBlank() }
    return songQuality ?: "N/D"
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
