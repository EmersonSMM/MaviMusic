package com.music.mavimusic.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.playback.PlaybackPosition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class DesktopMusicVideo(
    val original: Song,
    val song: Song,
    val url: String,
    val headers: Map<String, String>,
    val wasPlaying: Boolean,
    val startAtMs: Long,
)

/** Keep queue identity and provenance when switching representations of one recording. */
internal fun Song.withRecording(version: Song): Song = version.copy(
    queueEntryId = queueEntryId,
    queueTier = queueTier,
    playbackSource = playbackSource,
    playbackSourceType = playbackSourceType,
    playbackSourceId = playbackSourceId,
    radioName = radioName,
    isVideoOrigin = isVideoOrigin || isVideo || version.isVideoOrigin || version.isVideo,
)

internal fun isMatchingMusicVideo(original: Song, candidate: Song): Boolean {
    val excluded = Regex("""\b(?:lyrics?|letra|visuali[sz]er|official audio|audio oficial|karaoke)\b""", RegexOption.IGNORE_CASE)
    if (!candidate.isVideo || excluded.containsMatchIn(candidate.title)) return false
    if (DesktopTrackSimilarity.extractVersionTags(original.title) != DesktopTrackSimilarity.extractVersionTags(candidate.title)) return false
    return DesktopTrackSimilarity.compare(original, candidate, avoidDuplicates = true, allowRemixesLive = true).isDuplicate
}

internal suspend fun findMusicVideo(song: Song): Song? {
    val resolved = com.music.mavimusic.data.YtMusicRepository.resolveVideo(song)
    if (resolved != null && isMatchingMusicVideo(song, resolved)) return resolved
    return DesktopSearchClient.search("${song.artist} ${song.title} official music video", com.music.mavimusic.data.model.SearchFilter.VIDEOS)
        .getOrNull().orEmpty().filterIsInstance<com.music.mavimusic.data.model.SearchResult.Track>()
        .map { it.song }.firstOrNull { isMatchingMusicVideo(song, it) }
}

internal data class VideoRenderGate(val visible: Boolean, val playing: Boolean, val buffering: Boolean, val seeks: Int) {
    fun shouldRender(needsFrame: Boolean, lastSeek: Int): Boolean =
        visible && !buffering && (playing || needsFrame || seeks != lastSeek)
}

internal fun shouldSeekVideo(
    lastSeek: Int,
    currentSeeks: Int,
    atEnd: Boolean,
    decoderPositionMs: Long,
    targetMs: Long,
): Boolean = lastSeek != currentSeeks || (!atEnd && abs(decoderPositionMs - targetMs) > 1_500)

@Composable
internal fun DesktopMusicVideoStage(
    video: DesktopMusicVideo,
    position: PlaybackPosition,
    isPlaying: Boolean,
    isLoading: Boolean,
    durationMs: Long,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var frame by remember(video.url) { mutableStateOf<ImageBitmap?>(null) }
    val playing by rememberUpdatedState(isPlaying)
    val loading by rememberUpdatedState(isLoading)
    val active by DesktopWindowVisibility.renderActive.collectAsState()
    val renderActive by rememberUpdatedState(active)
    val failed by rememberUpdatedState(onFailure)
    LaunchedEffect(video.url) {
        // One owner for all native operations, including cleanup after cancellation.
        withContext(Dispatchers.IO) {
            val decoder = DesktopCanvasDecoder(maxEdge = 1280, looping = false)
            try {
                decoder.open(video.url, video.headers).getOrThrow()
                val pixels = ByteArray(decoder.width * decoder.height * 4)
                val info = ImageInfo.makeN32(decoder.width, decoder.height, ColorAlphaType.OPAQUE)
                var seek = -1
                var needsFrame = true
                var atEnd = false
                while (isActive) {
                    // No timer wakeups while paused or minimized. A seek can request one paused frame.
                    snapshotFlow { VideoRenderGate(renderActive, playing, loading || (!position.advancing && playing), position.seeks) }
                        .first { it.shouldRender(needsFrame, seek) }
                    val target = position.positionMs + if (playing && position.advancing && position.sampledAtNanos > 0)
                        ((System.nanoTime() - position.sampledAtNanos) / 1_000_000).coerceIn(0L, 600L) else 0L
                    if (shouldSeekVideo(seek, position.seeks, atEnd, decoder.positionMs, target)) {
                        check(decoder.seek(target)) { "No se pudo sincronizar el videoclip" }
                        seek = position.seeks
                        atEnd = false
                        needsFrame = true
                    }
                    if (atEnd) { delay(100); continue }
                    if (!needsFrame && decoder.positionMs > target + 30) { delay(10); continue }
                    var decoded = decoder.nextFrame(pixels)
                    // Discard the keyframe lead-in, and catch up after a delayed render.
                    while (decoded && decoder.positionMs < target - 45 && isActive) decoded = decoder.nextFrame(pixels)
                    decoder.failure?.let { throw it }
                    if (!decoded) { atEnd = true; delay(50); continue }
                    val bitmap = Image.makeRaster(info, pixels.copyOf(), decoder.width * 4).toComposeImageBitmap()
                    withContext(Dispatchers.Main) { frame = bitmap }
                    needsFrame = false
                    delay(decoder.frameIntervalMillis.coerceAtMost(33))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                withContext(Dispatchers.Main) { failed(failure.message ?: "No se pudo reproducir el videoclip") }
            } finally {
                decoder.close()
            }
        }
    }
    Column(modifier.background(Color.Black)) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val bitmap = frame ?: return@Canvas
            val scale = minOf(size.width / bitmap.width, size.height / bitmap.height)
            val width = (bitmap.width * scale).roundToInt()
            val height = (bitmap.height * scale).roundToInt()
            drawImage(bitmap, dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()), dstSize = IntSize(width, height))
        }
        if (frame == null || loading) CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color.White)
        }
        VideoTransport(position, isPlaying, durationMs, volume, onVolumeChange, onSeek, onPlayPause, onPrevious, onNext)
    }
}

@Composable
private fun VideoTransport(position: PlaybackPosition, playing: Boolean, durationMs: Long, volume: Float, onVolumeChange: (Float) -> Unit, seek: (Long) -> Unit, toggle: () -> Unit, previous: () -> Unit, next: () -> Unit) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp)) {
        Slider(
            value = dragging ?: (position.positionMs.toFloat() / durationMs.coerceAtLeast(1)).coerceIn(0f, 1f),
            onValueChange = { dragging = it },
            onValueChangeFinished = { dragging?.let { seek((it * durationMs).toLong()) }; dragging = null },
            enabled = durationMs > 0,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(((dragging ?: (position.positionMs.toFloat() / durationMs.coerceAtLeast(1))) * durationMs).toLong()), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            Text(formatTime(durationMs), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = previous) { Text(DesktopStrings["widget_previous", "Previous"]) }
            Spacer(Modifier.width(24.dp))
            Button(onClick = toggle) {
                Text(if (playing) DesktopStrings["pause", "Pause"] else DesktopStrings["play", "Play"])
            }
            Spacer(Modifier.width(24.dp))
            TextButton(onClick = next) { Text(DesktopStrings["widget_next", "Next"]) }
            Spacer(Modifier.width(24.dp))
            TextButton(onClick = { onVolumeChange(if (volume > 0f) 0f else 0.5f) }) {
                Text(DesktopStrings["d_volume", "Volume"])
            }
            Slider(value = volume, onValueChange = onVolumeChange, modifier = Modifier.width(120.dp))
        }
    }
}
