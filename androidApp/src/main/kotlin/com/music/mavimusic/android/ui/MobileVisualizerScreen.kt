package com.music.mavimusic.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.mavimusic.android.audio.AndroidAudioVisualization
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.data.AndroidPreferences
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.ui.theme.StatusBarIcons
import com.music.mavimusic.ui.theme.rememberArtworkPalette
import kotlin.math.max

private enum class VisualizerStyle(val label: String) { SPECTRUM("Espectro"), WAVE("Onda"), AMBIENT("Ambiente") }

@Composable
fun MobileVisualizerScreen(song: Song, onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AndroidPreferences(context) }
    var style by remember { mutableStateOf(VisualizerStyle.entries.firstOrNull { it.name == prefs.getVisualizerStyle() } ?: VisualizerStyle.SPECTRUM) }
    var reduceMotion by remember { mutableStateOf(prefs.getReduceVisualizerMotion()) }
    val playing by AndroidPlayerEngine.isPlaying.collectAsStateWithLifecycle()
    val loading by AndroidPlayerEngine.isLoading.collectAsStateWithLifecycle()
    val duration by AndroidPlayerEngine.durationMs.collectAsStateWithLifecycle()
    val frame by AndroidAudioVisualization.frame.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val active = playing && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    DisposableEffect(active, reduceMotion) {
        AndroidAudioVisualization.reducedMotion = reduceMotion
        AndroidAudioVisualization.enabled = active
        onDispose { AndroidAudioVisualization.enabled = false }
    }
    StatusBarIcons(dark = false)
    val palette = rememberArtworkPalette(song.thumbnailUrl)
    val accent = palette.accent
    var seek by remember(song.videoId) { mutableStateOf<Float?>(null) }
    val position = AndroidPlayerEngine.position.positionMs

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0D0D0F))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.ArrowBack, "Volver al reproductor", tint = Color.White) }
            Text("Visualizador", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, modifier = Modifier.weight(1f))
            Text("Reducir movimiento", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Switch(checked = reduceMotion, onCheckedChange = { reduceMotion = it; prefs.setReduceVisualizerMotion(it) },
                modifier = Modifier.height(40.dp).semantics { contentDescription = "Reducir movimiento" })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            VisualizerStyle.entries.forEach { mode ->
                FilterChip(selected = style == mode,
                    onClick = { style = mode; prefs.setVisualizerStyle(mode.name) },
                    label = { Text(mode.label) }, colors = mobileChipColors(), modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp))
            .background(Brush.radialGradient(listOf(palette.background, Color(0xFF101012)))), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().padding(24.dp)) {
                val visible = if (playing) 1f else .35f
                when (style) {
                    VisualizerStyle.SPECTRUM -> {
                        val levels = frame?.spectrum ?: List(32) { 0f }
                        val slot = size.width / levels.size
                        val brush = Brush.verticalGradient(listOf(accent.copy(alpha = visible), accent.copy(alpha = visible * .25f)))
                        levels.forEachIndexed { index, level ->
                            val height = max(2.dp.toPx(), level * size.height * if (reduceMotion) .45f else .85f)
                            drawRoundRect(brush, topLeft = Offset(index * slot + slot * .15f, (size.height - height) / 2),
                                size = Size(slot * .7f, height), cornerRadius = CornerRadius(slot * .3f))
                        }
                    }
                    VisualizerStyle.WAVE -> {
                        val wave = frame?.waveform ?: List(128) { 0f }
                        val path = Path()
                        wave.forEachIndexed { index, value ->
                            val x = index * size.width / (wave.size - 1)
                            val y = size.height / 2 - value * size.height * if (reduceMotion) .2f else .42f
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, accent.copy(alpha = visible * .12f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path, accent.copy(alpha = visible), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    VisualizerStyle.AMBIENT -> {
                        val energy = (frame?.rms ?: 0f) * if (reduceMotion) .25f else 1f
                        val base = size.minDimension * (.22f + energy * .5f)
                        listOf(.3f, .5f, .7f).forEachIndexed { index, x ->
                            val center = Offset(size.width * x, size.height * if (index % 2 == 0) .45f else .6f)
                            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = visible * (.35f + energy)), Color.Transparent), center, base * 1.5f),
                                radius = base * 1.5f, center = center)
                        }
                    }
                }
            }
            Text(if (loading) "Cargando audio…" else if (!playing) "En pausa" else if (frame == null) "Esperando audio…" else "",
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(song.thumbnailUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = AndroidPlayerEngine::skipPrevious) { Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = Color.White) }
            Spacer(Modifier.width(12.dp))
            FilledIconButton(onClick = AndroidPlayerEngine::togglePlayPause,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pausar" else "Reproducir")
            }
            Spacer(Modifier.width(12.dp))
            IconButton(onClick = AndroidPlayerEngine::skipNext) { Icon(Icons.Rounded.SkipNext, "Siguiente", tint = Color.White) }
        }
        Slider(value = seek ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
            onValueChange = { seek = it }, onValueChangeFinished = { seek?.let(AndroidPlayerEngine::seekFraction); seek = null }, enabled = duration > 0,
            modifier = Modifier.fillMaxWidth().height(24.dp))
    }
}
