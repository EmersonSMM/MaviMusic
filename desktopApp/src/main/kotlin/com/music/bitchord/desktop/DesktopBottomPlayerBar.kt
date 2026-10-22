package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Headphones
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.music.bitchord.ui.components.contextClick
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * Modern Liquid Glass Bottom Player Bar docked at the foot of the window (pie de ventana).
 * Preserves 100% of BitChord's playback capabilities with fluid controls, responsive layout,
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
    sidePanel: DesktopSidePanel?,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onToggleMiniPlayer: () -> Unit = {},
    onOpenSongMenu: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
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

                        DesktopToolbarButton(
                            onClick = onToggleLike,
                            size = 32.dp,
                        ) {
                            Icon(
                                if (isLiked) BitChordIcons.HeartFilled else BitChordIcons.Heart,
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
                                BitChordIcons.MusicNote,
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
                            BitChordIcons.Shuffle,
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
                                BitChordIcons.Repeat,
                                DesktopStrings["repeat", "Repetir"],
                                tint = tint,
                                modifier = Modifier.size(17.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Timeline Scrubber Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTimestamp(positionMs),
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        modifier = Modifier.width(36.dp),
                    )

                    DesktopThinSlider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            if (durationMs > 0) onSeek((fraction * durationMs).toLong())
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DesktopToolbarButton(onClick = onOpenAudioOutput, size = 32.dp) {
                        Icon(
                            Icons.Rounded.Headphones,
                            DesktopStrings["audio_output", "Salida de audio"],
                            tint = DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DesktopToolbarButton(onClick = onOpenLyrics, size = 32.dp) {
                        Icon(
                            BitChordIcons.LyricsQuote,
                            DesktopStrings["lyrics", "Letras"],
                            tint = if (sidePanel == DesktopSidePanel.LYRICS) Color.White else DesktopSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    DesktopToolbarButton(onClick = onOpenQueue, size = 32.dp) {
                        Icon(
                            BitChordIcons.Queue,
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

private fun formatTimestamp(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
