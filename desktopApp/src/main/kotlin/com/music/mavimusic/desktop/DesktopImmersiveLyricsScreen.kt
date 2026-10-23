package com.music.mavimusic.desktop

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
    WINAMP_WAVES("Ondas Winamp"),
    WINAMP_SPECTRUM("Espectro Winamp"),
    WINAMP_MILKDROP("Plasma MilkDrop"),
    WINAMP_TUNNEL("Túnel Retro 3D"),
    APPLE_LYRICS("Letras Mesh"),
}

/**
 * Full-window / fullscreen immersive visualizer & lyrics experience.
 * Includes classic Winamp Oscilloscope waves, LED Spectrum Analyzer with falling peak-hold dots,
 * MilkDrop psychedelic plasma waves, 3D Retro Warp Tunnel, and Apple Music synchronized kinetic lyrics.
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
    queue: List<Song> = emptyList(),
    queueIndex: Int = 0,
    onPlayQueueIndex: (Int) -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkPalette = rememberArtworkColors(song.thumbnailUrl, null)
    var visualMode by remember { mutableStateOf(ImmersiveVisualMode.WINAMP_WAVES) }
    var showLyricsInWinamp by remember { mutableStateOf(false) }
    var showQueuePanel by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

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

                // Discreet, sober dark-gray technical audio spec readout
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.height(26.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "320 kbps • 48 kHz • STEREO",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 10.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                        )
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
                                isQueueVisible = showQueuePanel,
                                onToggleQueue = { showQueuePanel = !showQueuePanel },
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(0.58f)
                                .fillMaxHeight(),
                        ) {
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

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(Color(0xFF141418))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            ) {
                ImmersiveVisualMode.entries.forEach { mode ->
                    val selected = visualMode == mode
                    DropdownMenuItem(
                        text = {
                            Text(
                                mode.label,
                                color = if (selected) Color(0xFF00E5FF) else Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.5.sp,
                            )
                        },
                        onClick = {
                            visualMode = mode
                            menuExpanded = false
                        },
                        trailingIcon = if (selected) {
                            {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 4.dp),
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            if (showLyricsInWinamp) "Letras sincronizadas" else "Solo visualizador",
                            color = Color.White,
                            fontSize = 12.5.sp,
                        )
                    },
                    onClick = {
                        showLyricsInWinamp = !showLyricsInWinamp
                        menuExpanded = false
                    },
                    trailingIcon = {
                        Switch(
                            checked = showLyricsInWinamp,
                            onCheckedChange = { showLyricsInWinamp = it },
                            modifier = Modifier.scale(0.8f),
                        )
                    },
                )
            }
        }

        // Floating Upcoming Songs (Queue) drawer panel on the right
        if (showQueuePanel) {
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 20.dp, top = 70.dp, bottom = 20.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE6121218),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shadowElevation = 24.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                ) {
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
                            Text(
                                text = "Canciones siguientes",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        IconButton(
                            onClick = { showQueuePanel = false },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(Modifier.height(8.dp))

                    val upcoming = remember(queue, queueIndex) {
                        if (queue.isEmpty()) emptyList()
                        else queue.drop((queueIndex + 1).coerceAtMost(queue.size))
                    }

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
                            itemsIndexed(upcoming) { offset, songItem ->
                                val realIndex = queueIndex + 1 + offset
                                Surface(
                                    onClick = { onPlayQueueIndex(realIndex) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.04f),
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
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = songItem.artist,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        val dur = songItem.durationText
                                        if (!dur.isNullOrBlank()) {
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = dur,
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 11.sp,
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

/**
 * Real-time 60fps Winamp-inspired audio visualizer backdrop.
 * Renders multi-harmonic oscilloscope neon waves, classic segmented LED spectrum bars
 * with gravity-falling peak-hold caps, and MilkDrop psychedelic plasma waves.
 */
@Composable
private fun WinampVisualizerBackdrop(
    mode: ImmersiveVisualMode,
    isPlaying: Boolean,
    songSeed: Int,
    modifier: Modifier = Modifier,
) {
    var timeSec by remember { mutableFloatStateOf(0f) }
    val numBands = 56
    val barValues = remember { FloatArray(numBands) { 0.15f } }
    val peakValues = remember { FloatArray(numBands) { 0.20f } }
    val peakVelocities = remember { FloatArray(numBands) { 0f } }

    LaunchedEffect(isPlaying, mode, songSeed) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { nanos ->
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

        // Deep retro-cyber grid glow backdrop
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF141834),
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
                    Color(0xFF00F0FF), // Electric Cyan
                    Color(0xFF00FF66), // Classic Winamp Green
                    Color(0xFFFF007F), // Neon Magenta
                    Color(0xFFFFD600), // Solar Gold
                )
                val steps = 160
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
                        style = Stroke(width = 10f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    // Core oscilloscope beam
                    drawPath(
                        path = path,
                        color = color.copy(alpha = 0.92f),
                        style = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
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
                )
            }

            ImmersiveVisualMode.WINAMP_SPECTRUM -> {
                // Top Mini Oscilloscope Ribbon
                val steps = 140
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
                    color = Color(0xFF00FF66).copy(alpha = 0.30f),
                    style = Stroke(width = 8f, cap = StrokeCap.Round),
                )
                drawPath(
                    path = wavePath,
                    color = Color(0xFF80FFB4),
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
                        0 -> Color(0xFF00F0FF)
                        1 -> Color(0xFFFF007F)
                        2 -> Color(0xFF00FF66)
                        else -> Color(0xFFFFAB00)
                    }
                    val ringPath = Path()
                    val pts = 96
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
                    Color(0xFF00E5FF),
                    Color(0xFFFF1744),
                    Color(0xFF76FF03),
                    Color(0xFFE040FB),
                )
                val steps = 150
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
                )
            }

            ImmersiveVisualMode.APPLE_LYRICS -> Unit
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

        // Classic Winamp Segmented LED Bar (Green -> Yellow -> Orange -> Red)
        for (s in 0 until activeSegs) {
            val segFrac = s.toFloat() / segments.toFloat()
            val segColor = when {
                segFrac < 0.50f -> Color(0xFF00FF66) // Classic Winamp Green
                segFrac < 0.75f -> Color(0xFFC6FF00) // Lime-Yellow
                segFrac < 0.88f -> Color(0xFFFFAB00) // Warm Amber-Orange
                else -> Color(0xFFFF1744) // Peak Red
            }
            val segY = bottomY - (s + 1) * (segH + segGap)
            drawRoundRect(
                color = segColor.copy(alpha = 0.88f),
                topLeft = Offset(x, segY),
                size = Size(barW, segH),
                cornerRadius = CornerRadius(2f, 2f),
            )
        }

        // Classic Winamp Falling Peak-Hold Cap
        val peakSeg = (peakValues[i].coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)
        val peakY = bottomY - (peakSeg + 1) * (segH + segGap)
        drawRoundRect(
            color = Color(0xFFE0F7FA),
            topLeft = Offset(x, peakY),
            size = Size(barW, segH.coerceAtMost(5f)),
            cornerRadius = CornerRadius(2f, 2f),
        )

        // Subtle Floor Reflection
        if (reflectionHeight > 0f && activeSegs > 0) {
            val reflH = (level * reflectionHeight).coerceAtLeast(2f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00FF66).copy(alpha = 0.22f),
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
 * Features 16 concentric depth rings accelerating toward the camera, 16 radial perspective ribs,
 * audio beat wobble, neon color cycling, and a pulsing bass singularity at the vanishing point.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWinampTunnel(
    barValues: FloatArray,
    peakValues: FloatArray,
    timeSec: Float,
    numBands: Int,
    isPlaying: Boolean,
) {
    val w = size.width
    val h = size.height
    val cx = w * 0.5f
    val cy = h * 0.48f

    val bass = (barValues[0] + barValues[1] + barValues[2] + barValues[3]) * 0.25f
    val maxR = (w.coerceAtLeast(h) * 0.75f).coerceAtLeast(200f)

    val numRings = 16
    val numSpokes = 16
    val speed = if (isPlaying) 0.52f + bass * 0.35f else 0.12f
    val rot = timeSec * (if (isPlaying) 0.32f else 0.08f)

    val tunnelColors = listOf(
        Color(0xFF00F0FF), // Electric Cyan
        Color(0xFFFF007F), // Neon Magenta
        Color(0xFF7C4DFF), // Deep Violet
        Color(0xFF00FF66), // Classic Winamp Lime
        Color(0xFFFFAB00), // Amber Gold
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
            style = Stroke(width = (4f + radiusFrac * 6f), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path = ringPath,
            color = ringColor.copy(alpha = alpha),
            style = Stroke(width = (1.5f + radiusFrac * 2.2f), cap = StrokeCap.Round, join = StrokeJoin.Round),
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
                Color(0xFF00F0FF).copy(alpha = 0.65f),
                Color(0xFFFF007F).copy(alpha = 0.25f),
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
    isQueueVisible: Boolean = false,
    onToggleQueue: () -> Unit = {},
) {
    var draggingScrubber by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    val liveProgress = if (durationMs > 0) {
        (position.positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
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
                .size(290.dp)
                .shadow(24.dp, artShape, spotColor = Color.Black.copy(alpha = 0.5f))
                .clip(artShape)
                .graphicsLayer { alpha = 0.78f },
        ) {
            DesktopArtwork(
                url = song.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                px = 1024,
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = song.title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

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
            Spacer(Modifier.height(4.dp))
            Text(
                text = album,
                color = Color.White.copy(alpha = 0.50f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(20.dp))

        DesktopThinSlider(
            value = currentDisplayFraction,
            onValueChange = {
                draggingScrubber = true
                scrubFraction = it
            },
            onValueChangeFinished = {
                if (durationMs > 0) {
                    onSeek((scrubFraction * durationMs).toLong())
                }
                draggingScrubber = false
            },
            modifier = Modifier.fillMaxWidth().height(14.dp),
            activeColor = Color.White,
            inactiveColor = Color.White.copy(alpha = 0.22f),
        )

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val displayMs = if (draggingScrubber && durationMs > 0) {
                (scrubFraction * durationMs).toLong()
            } else {
                position.positionMs
            }
            Text(
                text = formatDuration(displayMs),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.5.sp,
            )
            Text(
                text = formatDuration(durationMs),
                color = Color.White.copy(alpha = 0.60f),
                fontSize = 11.5.sp,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onShuffleChange(!shuffle) }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = DesktopStrings["shuffle", "Aleatorio"],
                    tint = if (shuffle) DesktopAccent else Color.White.copy(alpha = 0.45f),
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
                shadowElevation = 8.dp,
                modifier = Modifier.size(60.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) DesktopStrings["pause", "Pausar"] else DesktopStrings["play", "Reproducir"],
                        tint = Color.Black,
                        modifier = Modifier.size(34.dp),
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
                    tint = if (repeatMode != DesktopRepeatMode.OFF) DesktopAccent else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (volume <= 0.01f) Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                contentDescription = DesktopStrings["volume", "Volumen"],
                tint = Color.White.copy(alpha = 0.65f),
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
                    contentDescription = DesktopStrings["queue", "Canciones siguientes"],
                    tint = if (isQueueVisible) DesktopAccent else Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
