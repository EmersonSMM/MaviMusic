package com.music.bitchord.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.Song
import com.music.bitchord.ui.icons.BitChordIcons

/**
 * Modern, interactive Mini Reproductor integrated into the Top Bar.
 * Inspired by macOS Apple Music Dynamic Island / Now Playing widget with:
 * - Cover art with subtle rounded corners
 * - Direct scrubbing timeline with mm:ss time indicators
 * - Interactive playback controls (Previous, Play/Pause, Next) right in the widget
 * - Click anywhere on the title / art opens the full Now Playing sheet
 */
@Composable
internal fun DesktopTopMiniPlayer(
    compact: Boolean,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    previousEnabled: Boolean,
    progress: Float,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (song == null) {
        Surface(
            modifier = modifier
                .widthIn(min = 240.dp, max = 460.dp)
                .height(48.dp)
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp)),
            color = Color(0xFF232328).copy(alpha = 0.85f),
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        BitChordIcons.MusicNote,
                        contentDescription = null,
                        tint = DesktopSecondary.copy(alpha = 0.60f),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DesktopStrings["playback_channel_name", "BitChord Desktop"],
                        color = DesktopSecondary.copy(alpha = 0.70f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        return
    }

    if (compact) {
        // Compact mini reproductor for narrow window states: retains artwork, title, and play/pause
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onOpenNowPlaying),
            color = Color(0xFF222227).copy(alpha = 0.90f),
            tonalElevation = 0.dp,
        ) {
            Box(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopArtwork(
                        song.thumbnailUrl,
                        Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                        px = CARD_ART_PX,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            song.artist,
                            color = DesktopSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    DesktopToolbarButton(onClick = onPlayPause, size = 32.dp) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                // Thin progress line at the bottom
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(DesktopAccent),
                )
            }
        }
        return
    }

    // Full-featured rich Mini Reproductor in Top Bar
    Surface(
        modifier = modifier
            .widthIn(min = 340.dp, max = 580.dp)
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp)),
        color = Color(0xFF222227).copy(alpha = 0.92f),
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Artwork (clickable to open Now Playing)
            DesktopArtwork(
                song.thumbnailUrl,
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenNowPlaying),
                px = CARD_ART_PX,
            )

            Spacer(Modifier.width(10.dp))

            // Center: Title + Interactive scrubber timeline
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Title and Artist header
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenNowPlaying),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        song.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        song.artist,
                        color = DesktopSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                    )
                }

                // Scrubber row with timestamps
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatTimestamp(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = DesktopSecondary,
                        modifier = Modifier.width(30.dp),
                    )
                    DesktopThinSlider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            if (durationMs > 0L) {
                                onSeek((fraction * durationMs).toLong())
                            }
                        },
                        idleHeight = 3.dp,
                        activeHeight = 7.dp,
                        activeColor = DesktopAccent,
                        inactiveColor = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                    Text(
                        formatTimestamp(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = DesktopSecondary,
                        modifier = Modifier.width(30.dp),
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Direct Mini Player Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DesktopToolbarButton(onClick = onPrevious, enabled = previousEnabled, size = 28.dp) {
                    Icon(
                        Icons.Rounded.FastRewind,
                        contentDescription = "Previous",
                        tint = if (previousEnabled) Color.White else DesktopSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                DesktopToolbarButton(onClick = onPlayPause, size = 32.dp) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                DesktopToolbarButton(onClick = onNext, size = 28.dp) {
                    Icon(
                        Icons.Rounded.FastForward,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
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
