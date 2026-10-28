package com.music.mavimusic.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.music.mavimusic.ui.components.contextClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.music.mavimusic.data.model.CARD_ART_PX
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.ui.icons.MaviMusicIcons

/**
 * Modern Liquid Glass Bottom Player Bar docked at the foot of the window (pie de ventana).
 * Preserves 100% of MaviMusic's playback capabilities with fluid controls, responsive layout,
 * and authentic macOS translucent blur.
 */
@Composable
internal fun DesktopBottomPlayerBar(
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    shuffle: Boolean,
    onShuffleChange: (Boolean) -> Unit,
    repeatMode: DesktopRepeatMode,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenAudioOutput: () -> Unit,
    onOpenSleepTimer: () -> Unit = {},
    isSleepTimerActive: Boolean = false,
    onOpenImmersive: () -> Unit = {},
    automix: Boolean = false,
    onAutomixChange: (Boolean) -> Unit = {},
    djMixState: DjMixState = DjMixState(),
    sidePanel: DesktopSidePanel?,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onToggleVideoMode: () -> Unit = {},
    isSwitchingVideoMode: Boolean = false,
    onToggleMiniPlayer: () -> Unit = {},
    onOpenSongMenu: ((Song) -> Unit)? = null,
    playbackSpeed: Float = 1.0f,
    onPlaybackSpeedChange: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var showOutputPopover by remember { mutableStateOf(false) }
    var draggingScrubber by remember(song?.videoId) { mutableStateOf(false) }
    var scrubFraction by remember(song?.videoId) { mutableFloatStateOf(0f) }
    var pendingSeekMs by remember(song?.videoId) { mutableStateOf<Long?>(null) }
    var pendingSeekAt by remember(song?.videoId) { mutableStateOf(0L) }
    var showDjTooltip by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = DesktopPlatform.isWindows && maxWidth < 1800.dp
        Box(
            modifier = Modifier.fillMaxWidth()
                .height(if (compact) 110.dp else 78.dp)
                .desktopWindowGlass(DesktopChromeEdge.TOP),
        ) {
        // Top subtle divider line for Liquid Glass definition
        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            thickness = 1.dp,
            color = Color.White.copy(alpha = 0.08f),
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: Song Info & Like Button (max 320.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (song != null) {
                    Row(
                        modifier = Modifier.contextClick { onOpenSongMenu?.invoke(song) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        DesktopArtwork(
                            song.thumbnailUrl,
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenNowPlaying),
                            px = CARD_ART_PX,
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .widthIn(max = 240.dp)
                                .clickable(onClick = onOpenNowPlaying),
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = song.artist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        DesktopSongVideoTogglePill(
                            isVideo = song.isVideo,
                            isSwitching = isSwitchingVideoMode,
                            onToggle = onToggleVideoMode,
                        )

                        DesktopToolbarButton(
                            onClick = onToggleLike,
                            size = 32.dp,
                        ) {
                            Icon(
                                if (isLiked) MaviMusicIcons.HeartFilled else MaviMusicIcons.Heart,
                                contentDescription = if (isLiked) DesktopStrings["unlike", "Ya no me gusta"] else DesktopStrings["like", "Me gusta"],
                                tint = if (isLiked) Color(0xFFFF375F) else DesktopSecondary,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                MaviMusicIcons.MusicNote,
                                contentDescription = null,
                                tint = DesktopSecondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            "MaviMusic",
                            color = DesktopSecondary.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            // Center: Playback Transport Controls + Timeline Scrubber (max 620.dp)
            Column(
                modifier = Modifier
                    .weight(2f)
                    .widthIn(max = 620.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Controls Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    DesktopToolbarButton(
                        onClick = { onShuffleChange(!shuffle) },
                        size = 32.dp,
                    ) {
                        Icon(
                            MaviMusicIcons.Shuffle,
                            DesktopStrings["shuffle", "Aleatorio"],
                            tint = if (shuffle) DesktopAccent else DesktopSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }

                    DesktopToolbarButton(
                        onClick = onPrevious,
                        enabled = previousEnabled,
                        size = 32.dp,
                    ) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            DesktopStrings["widget_previous", "Anterior"],
                            tint = if (previousEnabled) Color.White else DesktopSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Play / Pause Circle Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (song != null) Color.White else Color.White.copy(alpha = 0.15f))
                            .clickable(enabled = song != null, onClick = onPlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) DesktopStrings["playback_pause", "Pausar"] else DesktopStrings["playback_play", "Reproducir"],
                                tint = if (song != null) Color.Black else Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    DesktopToolbarButton(
                        onClick = onNext,
                        enabled = song != null,
                        size = 32.dp,
                    ) {
                        Icon(
                            Icons.Rounded.FastForward,
                            DesktopStrings["widget_next", "Siguiente"],
                            tint = if (song != null) Color.White else DesktopSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    DesktopToolbarButton(
                        onClick = { onRepeatModeChange(repeatMode.next()) },
                        size = 32.dp,
                    ) {
                        val tint = if (repeatMode != DesktopRepeatMode.OFF) DesktopAccent else DesktopSecondary
                        if (repeatMode == DesktopRepeatMode.ONE) {
                            Text(
                                "1",
                                color = tint,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.semantics { contentDescription = "Repetir una" },
                            )
                        } else {
                            Icon(
                                MaviMusicIcons.Repeat,
                                DesktopStrings["repeat", "Repetir"],
                                tint = tint,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }

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
                        DesktopToolbarButton(
                            onClick = { onAutomixChange(!automix) },
                            size = 32.dp,
                        ) {
                            Icon(
                                Icons.Rounded.AutoAwesome,
                                DesktopStrings["automix", "Modo DJ Automix"],
                                tint = if (automix) Color(0xFF00E5FF) else DesktopSecondary,
                                modifier = Modifier.size(17.dp),
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

                // Fixed-height subtle container for Automix status (zero layout shift)
                Box(
                    modifier = Modifier.fillMaxWidth().height(14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (djMixState.isBlending) {
                        val infiniteTransition = rememberInfiniteTransition(label = "djPulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.45f,
                            targetValue = 0.85f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(750, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse,
                            ),
                            label = "pulseAlpha",
                        )
                        Text(
                            text = "MEZCLANDO CON ${djMixState.incomingTitle.uppercase()} A ${djMixState.incomingBpm} BPM",
                            color = Color.White.copy(alpha = 0.50f * pulseAlpha),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            fontSize = 9.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Timeline Scrubber Row
                val settledSeekMs = pendingSeekMs?.takeIf { target ->
                    val elapsed = System.currentTimeMillis() - pendingSeekAt
                    elapsed < 650L && kotlin.math.abs(positionMs - target) > 1_200L
                }
                val displayPositionMs = when {
                    draggingScrubber && durationMs > 0 -> (scrubFraction.toDouble() * durationMs.toDouble()).toLong()
                    settledSeekMs != null -> settledSeekMs
                    else -> positionMs
                }
                val displayProgress = when {
                    draggingScrubber -> scrubFraction
                    settledSeekMs != null && durationMs > 0 -> (settledSeekMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    else -> progress.coerceIn(0f, 1f)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTimestamp(displayPositionMs),
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        modifier = Modifier.width(36.dp),
                    )

                    DesktopThinSlider(
                        value = displayProgress,
                        onValueChange = { fraction ->
                            draggingScrubber = true
                            scrubFraction = fraction.coerceIn(0f, 1f)
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
                                formatTimestamp((fraction.toDouble() * durationMs.toDouble()).toLong().coerceIn(0L, durationMs))
                            } else {
                                "0:00"
                            }
                        },
                        idleHeight = 4.dp,
                        activeHeight = 7.dp,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )

                    Text(
                        text = formatTimestamp(durationMs),
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        modifier = Modifier.width(36.dp),
                    )
                }
            }

            // Right: Volume & Utilities (max 320.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                DesktopPlayerUtilities(compact) {
                    Box {
                        DesktopToolbarButton(
                            onClick = { showOutputPopover = !showOutputPopover },
                            size = 32.dp,
                        ) {
                            Icon(
                                Icons.Rounded.Headphones,
                                DesktopStrings["audio_output", "Salida de audio"],
                                tint = if (showOutputPopover) Color(0xFF00E5FF) else DesktopSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }

                        if (showOutputPopover) {
                            Popup(
                                alignment = Alignment.BottomEnd,
                                offset = IntOffset(0, -48),
                                onDismissRequest = { showOutputPopover = false },
                            ) {
                                DesktopAudioOutputPopover(onDismiss = { showOutputPopover = false })
                            }
                        }
                    }

                    DesktopToolbarButton(onClick = onOpenLyrics, size = 32.dp) {
                        Icon(
                            MaviMusicIcons.LyricsQuote,
                            DesktopStrings["lyrics", "Letras"],
                            tint = if (sidePanel == DesktopSidePanel.LYRICS) Color.White else DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DesktopToolbarButton(onClick = onOpenQueue, size = 32.dp) {
                        Icon(
                            MaviMusicIcons.Queue,
                            DesktopStrings["queue", "Cola de reproducción"],
                            tint = if (sidePanel == DesktopSidePanel.QUEUE) Color.White else DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DesktopToolbarButton(onClick = onOpenSleepTimer, size = 32.dp) {
                        Icon(
                            Icons.Rounded.Bedtime,
                            DesktopStrings["sleep_timer", "Temporizador de apagado"],
                            tint = if (isSleepTimerActive) Color.White else DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    if (onPlaybackSpeedChange != null) {
                        val keyLockOn by DesktopKeyLock.enabled.collectAsState()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = if (playbackSpeed != 1f) 0.16f else 0.08f))
                                .clickable {
                                    val next = when {
                                        playbackSpeed < 0.94f -> 1.0f
                                        playbackSpeed < 1.04f -> 1.08f
                                        playbackSpeed < 1.15f -> 1.25f
                                        playbackSpeed < 1.35f -> 0.92f
                                        else -> 1.0f
                                    }
                                    onPlaybackSpeedChange(next)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "${"%.2f".format(playbackSpeed).trimEnd('0').trimEnd('.')}×",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (playbackSpeed != 1f) DesktopAccent else DesktopSecondary,
                                )
                                if (keyLockOn) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = "Bloqueo de tono activo",
                                        tint = DesktopAccent,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                        }
                    }

                    val karaokeFeatureOn by DesktopKaraokeSettings.featureEnabled.collectAsState()
                    if (karaokeFeatureOn) {
                        val isKaraokeActive by DesktopKaraokeSettings.isActive.collectAsState()
                        DesktopToolbarButton(
                            onClick = { DesktopKaraokeSettings.setActive(!isKaraokeActive) },
                            size = 32.dp,
                        ) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = if (isKaraokeActive) "Modo Karaoke (Activo)" else "Modo Karaoke (K)",
                                tint = if (isKaraokeActive) Color(0xFF00E5FF) else DesktopSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    DesktopToolbarButton(onClick = onOpenImmersive, size = 32.dp) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            DesktopStrings["immersive_lyrics", "Modo Inmersivo Classic / Ondas"],
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(19.dp),
                        )
                    }

                    // Volume Control
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, end = 2.dp),
                    ) {
                        val volumeIcon = when {
                            volume <= 0.01f -> Icons.Rounded.VolumeMute
                            volume < 0.5f -> Icons.Rounded.VolumeDown
                            else -> Icons.Rounded.VolumeUp
                        }
                        Icon(
                            volumeIcon,
                            contentDescription = DesktopStrings["d_volume", "Volumen"],
                            tint = DesktopSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    onVolumeChange(if (volume > 0.01f) 0f else 0.8f)
                                },
                        )
                        Spacer(Modifier.width(6.dp))
                        DesktopThinSlider(
                            value = volume,
                            onValueChange = onVolumeChange,
                            idleHeight = 4.dp,
                            activeHeight = 7.dp,
                            modifier = Modifier.width(76.dp),
                        )
                    }


                    DesktopToolbarButton(onClick = onOpenNowPlaying, size = 32.dp) {
                        Icon(
                            Icons.Rounded.OpenInFull,
                            contentDescription = DesktopStrings["expand", "Pantalla completa"],
                            tint = DesktopSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopPlayerUtilities(compact: Boolean, content: @Composable () -> Unit) {
    if (compact) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    } else {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
    }
}

private fun formatTimestamp(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun DesktopSongVideoTogglePill(
    isVideo: Boolean,
    isSwitching: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { if (!isSwitching) onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = if (isSwitching) 0.04f else 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = if (isSwitching) 0.06f else 0.12f)),
        modifier = modifier
            .height(24.dp)
            .then(if (isSwitching) Modifier.alpha(0.6f) else Modifier),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (!isVideo) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Canción",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (!isVideo) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isVideo) Color.White else DesktopSecondary,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isVideo) Color(0xFF0A84FF).copy(alpha = 0.35f) else Color.Transparent)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Normal,
                    color = if (isVideo) Color.White else DesktopSecondary,
                )
            }
        }
    }
}
