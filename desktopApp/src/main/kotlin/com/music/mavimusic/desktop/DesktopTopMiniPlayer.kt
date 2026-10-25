package com.music.mavimusic.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import mavimusic.desktopapp.generated.resources.Res
import mavimusic.desktopapp.generated.resources.app_logo
import androidx.compose.ui.draw.alpha
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
import com.music.mavimusic.data.model.CARD_ART_PX
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.ui.components.contextClick
import com.music.mavimusic.ui.icons.MaviMusicIcons
import com.music.mavimusic.ui.player.FullArtworkBlurBackdrop
import com.music.mavimusic.ui.player.rememberFullArtworkBlurImage

/**
 * Authentic Apple Music / macOS style Glass Mini Reproductor (Widget Superior).
 * Features dynamic blurred artwork backdrop, timestamps, and playback controls.
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
    onOpenSongMenu: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = rememberDesktopArtworkPalette(song?.thumbnailUrl)
    val blurImage = rememberFullArtworkBlurImage(imageUrl = song?.thumbnailUrl)
    val shape = RoundedCornerShape(14.dp)
    val subtitleColor = Color.White.copy(alpha = 0.72f)

    Surface(
        modifier = modifier
            .widthIn(min = 340.dp, max = 420.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .shadow(16.dp, shape),
        color = Color(0xFF131318),
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Blurred artwork backdrop
            if (blurImage != null) {
                FullArtworkBlurBackdrop(
                    image = blurImage,
                    modifier = Modifier.matchParentSize().alpha(0.42f),
                )
            }
            // Ambient artwork color wash & gradient scrim for legibility
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                palette.primary.copy(alpha = 0.40f),
                                palette.secondary.copy(alpha = 0.25f),
                                Color(0xFF0C0C10).copy(alpha = 0.88f),
                            )
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                                        .shadow(6.dp, RoundedCornerShape(8.dp))
                                        .contextClick { onOpenSongMenu?.invoke(song) }
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
                                    Image(
                                        painter = painterResource(Res.drawable.app_logo),
                                        contentDescription = "MaviMusic",
                                        modifier = Modifier.size(46.dp),
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
                            .contextClick { song?.let { onOpenSongMenu?.invoke(it) } }
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
                            color = subtitleColor,
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
                        color = subtitleColor,
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
                        color = subtitleColor,
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
                        Icon(volIcon, "Volumen", tint = subtitleColor, modifier = Modifier.size(19.dp))
                    }

                    Spacer(Modifier.width(10.dp))

                    // More Options button (...)
                    var moreMenuOpen by remember { mutableStateOf(false) }
                    Box {
                        DesktopMiniControlButton(onClick = { moreMenuOpen = true }, size = 28.dp) {
                            Icon(Icons.Rounded.MoreHoriz, "Opciones", tint = subtitleColor, modifier = Modifier.size(20.dp))
                        }
                        DesktopGlassMenu(
                            expanded = moreMenuOpen,
                            onDismissRequest = { moreMenuOpen = false },
                            modifier = Modifier,
                        ) {
                            if (song != null && onOpenSongMenu != null) {
                                DesktopGlassMenuItem(
                                    text = { Text("Opciones de la canción", style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = { Icon(Icons.Rounded.MoreHoriz, null, modifier = Modifier.size(17.dp)) },
                                    onClick = {
                                        moreMenuOpen = false
                                        onOpenSongMenu(song)
                                    },
                                )
                            }
                            DesktopGlassMenuItem(
                                text = { Text("Modo Azar (Shuffle)", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    moreMenuOpen = false
                                    onFeelingLucky()
                                },
                            )
                            DesktopGlassMenuItem(
                                text = { Text("Abrir MaviMusic", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Rounded.OpenInFull, null, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    moreMenuOpen = false
                                    onOpenNowPlaying()
                                },
                            )
                            DesktopGlassMenuItem(
                                text = { Text("Buscar / Paleta (⌘K)", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    moreMenuOpen = false
                                    DesktopSpotlightState.show()
                                },
                            )
                            DesktopGlassMenuItem(
                                text = { Text("Ajustes", style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(17.dp)) },
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
                            MaviMusicIcons.LyricsQuote,
                            contentDescription = DesktopStrings["lyrics", "Letras"],
                            tint = subtitleColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Queue button
                    DesktopMiniControlButton(onClick = onOpenQueue, size = 28.dp) {
                        Icon(
                            MaviMusicIcons.Queue,
                            contentDescription = DesktopStrings["queue", "Cola de reproducción"],
                            tint = subtitleColor,
                            modifier = Modifier.size(18.dp),
                        )
                    }
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
