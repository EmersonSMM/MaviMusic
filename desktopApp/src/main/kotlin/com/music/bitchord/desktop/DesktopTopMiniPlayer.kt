package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * Authentic Apple Music / macOS style Dark Teal Glass Mini Reproductor (Widget Superior).
 * Faithfully matches the exact layout, dimensions, teal glass finish, timestamps,
 * and controls from macOS Apple Music Mini Player.
 */
@Composable
internal fun DesktopTopMiniPlayer(
    compact: Boolean = false,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    volume: Float = 1f,
    onVolumeChange: (Float) -> Unit = {},
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFeelingLucky: () -> Unit = {},
    onOpenNowPlaying: () -> Unit = {},
    onOpenLyrics: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
    onMinimize: () -> Unit = {},
    onToggleExpand: () -> Unit = onOpenNowPlaying,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tealTop = Color(0xFF09414B)
    val tealBottom = Color(0xFF052B32)
    val backgroundBrush = Brush.verticalGradient(listOf(tealTop, tealBottom))
    val shape = RoundedCornerShape(14.dp)
    val mutedTeal = Color(0xFF8BA5A8)

    Surface(
        modifier = modifier
            .widthIn(min = 340.dp, max = 420.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .shadow(12.dp, shape),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .background(backgroundBrush)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                // Top section: Album Art on left + Perfectly Centered Song/Artist Info + Window Controls at top-right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                ) {
                    // Artwork on the left
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        if (song != null) {
                            DesktopArtwork(
                                song.thumbnailUrl,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shadow(4.dp, RoundedCornerShape(8.dp))
                                    .clickable(onClick = onOpenNowPlaying),
                                px = CARD_ART_PX,
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    BitChordIcons.MusicNote,
                                    contentDescription = null,
                                    tint = mutedTeal,
                                    modifier = Modifier.size(28.dp),
                                )
                            }
                        }
                    }

                    // Window control buttons at top right (⤢, ✕)
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DesktopMiniControlButton(onClick = onToggleExpand, size = 20.dp) {
                            Icon(
                                Icons.Rounded.OpenInFull,
                                contentDescription = "Ampliar",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(11.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        DesktopMiniControlButton(onClick = onClose, size = 20.dp) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White.copy(alpha = 0.65f),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }

                    // Title and Subtitle: horizontally and vertically centered relative to the widget
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 74.dp, end = 74.dp)
                            .align(Alignment.Center)
                            .clickable(onClick = onOpenNowPlaying),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = song?.title ?: "MaviMusic",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(3.dp))
                        val subtitle = if (song != null) {
                            val album = song.albumName?.takeIf { it.isNotBlank() }
                            if (album != null) "${song.artist} — $album" else song.artist
                        } else {
                            DesktopStrings["d_choose_something_to_play", "Elige algo para reproducir"]
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = mutedTeal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Middle section: Scrubber Timeline (Elapsed time, Scrubber slider, Remaining time with minus)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatTimestamp(positionMs),
                        fontSize = 11.sp,
                        color = mutedTeal,
                        modifier = Modifier.width(34.dp),
                    )
                    DesktopThinSlider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            if (durationMs > 0L) {
                                onSeek((fraction * durationMs).toLong())
                            }
                        },
                        idleHeight = 3.5.dp,
                        activeHeight = 6.dp,
                        activeColor = Color.White,
                        inactiveColor = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                    )
                    val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                    Text(
                        text = "-${formatTimestamp(remainingMs)}",
                        fontSize = 11.sp,
                        color = mutedTeal,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(38.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Bottom section: Controls (Volume, More, Prev, Play/Pause, Next, Lyrics, Queue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Volume button
                    DesktopMiniControlButton(
                        onClick = { onVolumeChange(if (volume > 0.05f) 0f else 0.8f) },
                        size = 28.dp,
                    ) {
                        val volIcon = when {
                            volume <= 0.01f -> Icons.Rounded.VolumeMute
                            volume < 0.5f -> Icons.Rounded.VolumeDown
                            else -> Icons.Rounded.VolumeUp
                        }
                        Icon(volIcon, "Volumen", tint = mutedTeal, modifier = Modifier.size(19.dp))
                    }

                    Spacer(Modifier.width(10.dp))

                    // More Options button (...)
                    var moreMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        DesktopMiniControlButton(onClick = { moreMenuOpen = true }, size = 28.dp) {
                            Icon(Icons.Rounded.MoreHoriz, "Opciones", tint = mutedTeal, modifier = Modifier.size(20.dp))
                        }
                        DropdownMenu(
                            expanded = moreMenuOpen,
                            onDismissRequest = { moreMenuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("🎲 Tener suerte") },
                                onClick = {
                                    moreMenuOpen = false
                                    onFeelingLucky()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("🪟 Abrir MaviMusic") },
                                onClick = {
                                    moreMenuOpen = false
                                    onOpenNowPlaying()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("⚙ Ajustes") },
                                onClick = {
                                    moreMenuOpen = false
                                    onToggleExpand()
                                },
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Previous Track
                    DesktopMiniControlButton(onClick = onPrevious, enabled = previousEnabled, size = 28.dp) {
                        Icon(
                            Icons.Rounded.FastRewind,
                            contentDescription = DesktopStrings["widget_previous", "Anterior"],
                            tint = if (previousEnabled) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Play / Pause
                    DesktopMiniControlButton(onClick = onPlayPause, size = 32.dp) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    // Next Track
                    DesktopMiniControlButton(onClick = onNext, size = 28.dp) {
                        Icon(
                            Icons.Rounded.FastForward,
                            contentDescription = DesktopStrings["widget_next", "Siguiente"],
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Lyrics button
                    DesktopMiniControlButton(onClick = onOpenLyrics, size = 28.dp) {
                        Icon(
                            BitChordIcons.LyricsQuote,
                            contentDescription = DesktopStrings["lyrics", "Letras"],
                            tint = mutedTeal,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Queue button
                    DesktopMiniControlButton(onClick = onOpenQueue, size = 28.dp) {
                        Icon(
                            BitChordIcons.Queue,
                            contentDescription = DesktopStrings["queue", "Cola de reproducción"],
                            tint = mutedTeal,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopMiniControlButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 26.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .desktopHoverWash()
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

private fun formatTimestamp(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
