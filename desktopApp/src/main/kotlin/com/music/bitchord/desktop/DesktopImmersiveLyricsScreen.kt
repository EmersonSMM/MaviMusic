package com.music.bitchord.desktop

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.lyrics.LyricLine
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.data.model.Song
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.ui.player.LyricsSidePanel
import com.music.bitchord.ui.player.MeshGradientBackground
import com.music.bitchord.ui.player.rememberArtworkColors

/**
 * Apple Music-inspired full-window / fullscreen immersive lyrics experience.
 * Features an organic living animated artwork mesh backdrop, high-resolution album artwork,
 * full playback transport controls, and kinetic synchronized lyrics with tap-to-seek.
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkPalette = rememberArtworkColors(song.thumbnailUrl, null)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // Dynamic living Apple Music mesh gradient backdrop
        MeshGradientBackground(
            palette = artworkPalette,
            trackKey = song.videoId,
            modifier = Modifier.fillMaxSize(),
        )

        // Cinematic vignette / scrim overlay for contrast & readability
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

        Column(Modifier.fillMaxSize()) {
            // Minimal Glass Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Exit Pill Button
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.height(34.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.FullscreenExit,
                            contentDescription = DesktopStrings["exit_immersive_lyrics", "Salir de pantalla completa"],
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

                Spacer(Modifier.width(16.dp))

                // Track and Artist indicator
                Text(
                    text = "${song.title} • ${song.artist}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Volume Normalization Badge (if active)
                if (volumeNormalizationActive) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.height(26.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = DesktopStrings["normalized_lufs_badge", "✓ -14 LUFS"],
                                color = DesktopAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }

                // Close Button
                DesktopToolbarButton(onClick = onClose, size = 32.dp) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = DesktopStrings["close", "Cerrar"],
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Body Area (Responsive layout: 2 Columns on wide screen, single column if compact)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 24.dp, end = 24.dp, bottom = 20.dp),
            ) {
                val isWide = maxWidth >= 860.dp

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(36.dp),
                    ) {
                        // Left Column: Large Artwork, Metadata & Transport Controls
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
                            )
                        }

                        // Right Column: Kinetic Synchronized Lyrics
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
                    // Compact mode: Lyrics take center stage
                    Box(modifier = Modifier.fillMaxSize()) {
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
        }
    }
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
        // High-resolution Artwork with ambient glow
        val artShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .size(310.dp)
                .shadow(28.dp, artShape, spotColor = Color.Black.copy(alpha = 0.7f))
                .clip(artShape),
        ) {
            DesktopArtwork(
                url = song.thumbnailUrl,
                modifier = Modifier.fillMaxSize(),
                px = 1024,
            )
        }

        Spacer(Modifier.height(28.dp))

        // Song Title
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

        // Artist Name
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

        Spacer(Modifier.height(24.dp))

        // Scrubber Slider
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

        // Timestamp Row
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

        Spacer(Modifier.height(18.dp))

        // Transport Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Shuffle
            IconButton(onClick = { onShuffleChange(!shuffle) }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = DesktopStrings["shuffle", "Aleatorio"],
                    tint = if (shuffle) DesktopAccent else Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.size(20.dp),
                )
            }

            // Previous
            DesktopToolbarButton(onClick = onPrevious, size = 42.dp) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    contentDescription = DesktopStrings["previous", "Anterior"],
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            // Play / Pause Circle
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

            // Next
            DesktopToolbarButton(onClick = onNext, size = 42.dp) {
                Icon(
                    Icons.Rounded.SkipNext,
                    contentDescription = DesktopStrings["next", "Siguiente"],
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }

            // Repeat Mode
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

        Spacer(Modifier.height(18.dp))

        // Volume Bar
        Row(
            modifier = Modifier
                .widthIn(max = 280.dp)
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
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1_000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
