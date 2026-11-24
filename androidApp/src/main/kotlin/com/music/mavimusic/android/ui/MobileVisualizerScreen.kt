package com.music.mavimusic.android.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private enum class VisualizerStyle(val label: String) {
    AURORA("Aurora"),
    SPECTRUM("Espectro"),
    ORBIT("Órbita"),
    WAVE("Onda Neón"),
    CRYSTAL("Cristal"),
}

@Composable
fun MobileVisualizerScreen(song: Song, onClose: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { AndroidPreferences(context) }
    var style by remember {
        mutableStateOf(
            VisualizerStyle.entries.firstOrNull { it.name == prefs.getVisualizerStyle() }
                ?: VisualizerStyle.AURORA
        )
    }
    var reduceMotion by remember { mutableStateOf(prefs.getReduceVisualizerMotion()) }
    var isFullScreen by remember { mutableStateOf(false) }
    var showFullScreenHud by remember { mutableStateOf(false) }

    val playing by AndroidPlayerEngine.isPlaying.collectAsStateWithLifecycle()
    val loading by AndroidPlayerEngine.isLoading.collectAsStateWithLifecycle()
    val duration by AndroidPlayerEngine.durationMs.collectAsStateWithLifecycle()
    val frame by AndroidAudioVisualization.frame.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val active = playing && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)

    BackHandler(enabled = isFullScreen) {
        isFullScreen = false
        showFullScreenHud = false
    }

    // Enter / exit true immersive Android FullScreen (hides status bar + navigation bar)
    DisposableEffect(isFullScreen, view) {
        val window = (context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        if (controller != null) {
            if (isFullScreen) {
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Auto-hide fullscreen floating HUD after 4 seconds of inactivity
    LaunchedEffect(isFullScreen, showFullScreenHud, style) {
        if (isFullScreen && showFullScreenHud) {
            delay(4_000L)
            showFullScreenHud = false
        }
    }

    DisposableEffect(active, reduceMotion) {
        AndroidAudioVisualization.reducedMotion = reduceMotion
        AndroidAudioVisualization.enabled = active
        onDispose { AndroidAudioVisualization.enabled = false }
    }

    StatusBarIcons(dark = false)
    val palette = rememberArtworkPalette(song.thumbnailUrl)
    val accent = palette.accent
    val secondary = remember(accent, palette.wash) {
        Color(
            red = (accent.red * 0.45f + 0.32f).coerceIn(0f, 1f),
            green = (accent.green * 0.55f + 0.42f).coerceIn(0f, 1f),
            blue = (accent.blue * 0.75f + 0.50f).coerceIn(0f, 1f),
            alpha = 1f,
        )
    }
    var seek by remember(song.videoId) { mutableStateOf<Float?>(null) }
    val position = AndroidPlayerEngine.position.positionMs

    // Smooth 60fps interpolated audio state so every visualizer glides like the PC shaders
    val latestFrame by rememberUpdatedState(frame)
    val latestPlaying by rememberUpdatedState(playing)
    val latestReduce by rememberUpdatedState(reduceMotion)
    val smoothSpectrum = remember { FloatArray(32) }
    val peakHold = remember { FloatArray(32) }
    val smoothWave = remember { FloatArray(128) }
    var smoothRms by remember { mutableStateOf(0f) }
    var animTime by remember { mutableStateOf(0f) }

    LaunchedEffect(active) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                lastNanos = now
                val speed = if (latestReduce) 0.45f else 1.0f
                animTime += if (latestPlaying) dt * speed else dt * 0.12f

                val src = latestFrame
                val attack = (dt * 18f).coerceIn(0.15f, 0.65f)
                val decay = (dt * 6.5f).coerceIn(0.06f, 0.30f)

                val spec = src?.spectrum
                for (i in 0 until 32) {
                    val target = if (latestPlaying && spec != null && i < spec.size) spec[i] else 0f
                    val cur = smoothSpectrum[i]
                    val next = if (target > cur) cur + (target - cur) * attack else cur + (target - cur) * decay
                    smoothSpectrum[i] = next
                    if (next >= peakHold[i]) {
                        peakHold[i] = next
                    } else {
                        peakHold[i] = max(0f, peakHold[i] - dt * 0.45f)
                    }
                }

                val wave = src?.waveform
                for (i in 0 until 128) {
                    val target = if (latestPlaying && wave != null && i < wave.size) wave[i] else 0f
                    smoothWave[i] += (target - smoothWave[i]) * (dt * 20f).coerceIn(0.18f, 0.72f)
                }

                val targetRms = if (latestPlaying && src != null) src.rms else 0f
                smoothRms += (targetRms - smoothRms) * (dt * 10f).coerceIn(0.10f, 0.45f)
            }
        }
    }

    val t = animTime
    val bass = remember(t) {
        (smoothSpectrum[0] + smoothSpectrum[1] + smoothSpectrum[2] + smoothSpectrum[3]) / 4f
    }
    val mid = remember(t) {
        var sum = 0f
        for (i in 6..15) sum += smoothSpectrum[i]
        sum / 10f
    }
    val treble = remember(t) {
        var sum = 0f
        for (i in 20..31) sum += smoothSpectrum[i]
        sum / 12f
    }

    @Composable
    fun VisualizerStage(modifier: Modifier, fullBleed: Boolean) {
        val stageShape = if (fullBleed) RoundedCornerShape(0.dp) else RoundedCornerShape(24.dp)
        Box(
            modifier = modifier
                .clip(stageShape)
                .then(
                    if (fullBleed) Modifier
                    else Modifier.border(1.dp, Color.White.copy(alpha = 0.10f), stageShape)
                )
                .background(
                    Brush.radialGradient(
                        listOf(
                            palette.background.copy(alpha = if (fullBleed) 0.55f else 0.70f),
                            Color(if (fullBleed) 0xB807080C else 0xCC090A0F),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f
                val vis = if (playing) 1f else 0.42f
                val gain = if (reduceMotion) 0.55f else 1.0f

                when (style) {
                    VisualizerStyle.AURORA -> {
                        val haloR = min(w, h) * (0.36f + bass * 0.38f * gain)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = vis * (0.38f + bass * 0.35f)),
                                    secondary.copy(alpha = vis * 0.15f),
                                    Color.Transparent,
                                ),
                                center = Offset(cx, cy),
                                radius = haloR,
                            ),
                            radius = haloR,
                            center = Offset(cx, cy),
                        )

                        for (layer in 0..2) {
                            val phase = t * (0.8f + layer * 0.35f) + layer * 1.9f
                            val baseY = cy + (layer - 1) * h * 0.08f
                            val ribbonColor = when (layer) {
                                0 -> accent
                                1 -> secondary
                                else -> Color.White
                            }
                            val path = Path()
                            val fillPath = Path()
                            val pts = 48
                            for (i in 0..pts) {
                                val frac = i.toFloat() / pts
                                val x = frac * w
                                val specIdx = (frac * 31f).toInt().coerceIn(0, 31)
                                val energy = smoothSpectrum[specIdx] * gain
                                val waveOffset = sin(frac * PI * (2.2 + layer) + phase).toFloat() * h * (0.06f + 0.12f * energy) +
                                    cos(frac * PI * 4.5 - phase * 0.7f).toFloat() * h * 0.03f * (0.4f + mid)
                                val y = baseY - waveOffset - energy * h * 0.16f * (if (layer == 1) -0.7f else 1f)
                                if (i == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, h)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                            }
                            fillPath.lineTo(w, h)
                            fillPath.close()

                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        ribbonColor.copy(alpha = vis * (0.18f - layer * 0.03f)),
                                        Color.Transparent,
                                    ),
                                    startY = cy - h * 0.25f,
                                    endY = h,
                                ),
                            )
                            drawPath(
                                path = path,
                                color = ribbonColor.copy(alpha = vis * 0.22f),
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                            drawPath(
                                path = path,
                                color = ribbonColor.copy(alpha = vis * (0.85f - layer * 0.15f)),
                                style = Stroke(width = (3.2f - layer * 0.6f).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }

                        for (i in 0 until 12) {
                            val angle = t * (0.28f + (i % 4) * 0.11f) + i * (2f * PI.toFloat() / 12f)
                            val bandVal = smoothSpectrum[(i * 2) % 32] * gain
                            val rx = w * (0.26f + 0.16f * sin(t * 0.5f + i))
                            val ry = h * (0.22f + 0.12f * cos(t * 0.4f + i))
                            val px = cx + cos(angle) * rx
                            val py = cy + sin(angle) * ry
                            val r = (4f + bandVal * 16f).dp.toPx()
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = vis * (0.55f + bandVal * 0.4f)),
                                        accent.copy(alpha = vis * 0.25f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(px, py),
                                    radius = r,
                                ),
                                radius = r,
                                center = Offset(px, py),
                            )
                        }
                    }

                    VisualizerStyle.SPECTRUM -> {
                        val padX = 20.dp.toPx()
                        val usableW = w - padX * 2f
                        val barCount = 32
                        val slot = usableW / barCount
                        val barW = slot * 0.58f
                        val horizonY = h * 0.62f
                        val maxBarH = h * 0.48f

                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, accent.copy(alpha = vis * (0.14f + bass * 0.18f)), Color.Transparent),
                                startY = horizonY - 40.dp.toPx(),
                                endY = horizonY + 40.dp.toPx(),
                            )
                        )

                        for (i in 0 until barCount) {
                            val distFromCenter = kotlin.math.abs(i - (barCount - 1) / 2f) / ((barCount - 1) / 2f)
                            val idx = (distFromCenter * 28f).toInt().coerceIn(0, 31)
                            val level = (smoothSpectrum[idx] * gain).coerceIn(0f, 1f)
                            val peak = (peakHold[idx] * gain).coerceIn(0f, 1f)
                            val barH = max(4.dp.toPx(), level * maxBarH)
                            val x = padX + i * slot + (slot - barW) / 2f

                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = vis * 0.95f),
                                        accent.copy(alpha = vis * 0.92f),
                                        secondary.copy(alpha = vis * 0.55f),
                                    ),
                                    startY = horizonY - barH,
                                    endY = horizonY,
                                ),
                                topLeft = Offset(x, horizonY - barH),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(barW / 2f),
                            )

                            val reflH = barH * 0.42f
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        accent.copy(alpha = vis * 0.28f),
                                        Color.Transparent,
                                    ),
                                    startY = horizonY + 3.dp.toPx(),
                                    endY = horizonY + 3.dp.toPx() + reflH,
                                ),
                                topLeft = Offset(x, horizonY + 3.dp.toPx()),
                                size = Size(barW, reflH),
                                cornerRadius = CornerRadius(barW / 2f),
                            )

                            val peakY = horizonY - max(6.dp.toPx(), peak * maxBarH) - 6.dp.toPx()
                            drawCircle(
                                color = Color.White.copy(alpha = vis * 0.85f),
                                radius = barW * 0.36f,
                                center = Offset(x + barW / 2f, peakY),
                            )
                        }
                    }

                    VisualizerStyle.ORBIT -> {
                        val baseRadius = min(w, h) * 0.23f
                        val pulseRadius = baseRadius + bass * 18.dp.toPx() * gain

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = vis * (0.34f + bass * 0.28f)),
                                    secondary.copy(alpha = vis * 0.12f),
                                    Color.Transparent,
                                ),
                                center = Offset(cx, cy),
                                radius = pulseRadius * 2.1f,
                            ),
                            radius = pulseRadius * 2.1f,
                            center = Offset(cx, cy),
                        )

                        drawCircle(
                            color = secondary.copy(alpha = vis * (0.22f + treble * 0.35f)),
                            radius = pulseRadius * (1.28f + mid * 0.16f * gain),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )

                        val spokes = 64
                        for (i in 0 until spokes) {
                            val angle = (i.toFloat() / spokes) * 2f * PI.toFloat() - PI.toFloat() / 2f + t * 0.18f
                            val specIdx = if (i < 32) i else 63 - i
                            val mag = smoothSpectrum[specIdx.coerceIn(0, 31)] * gain
                            val len = (6.dp.toPx() + mag * min(w, h) * 0.19f)
                            val start = Offset(
                                cx + cos(angle) * (pulseRadius + 6.dp.toPx()),
                                cy + sin(angle) * (pulseRadius + 6.dp.toPx()),
                            )
                            val end = Offset(
                                cx + cos(angle) * (pulseRadius + 6.dp.toPx() + len),
                                cy + sin(angle) * (pulseRadius + 6.dp.toPx() + len),
                            )
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(accent.copy(alpha = vis * 0.9f), Color.White.copy(alpha = vis * 0.9f)),
                                    start = start,
                                    end = end,
                                ),
                                start = start,
                                end = end,
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                        }
                    }

                    VisualizerStyle.WAVE -> {
                        val padX = 16.dp.toPx()
                        val usableW = w - padX * 2f
                        val amp = h * (if (reduceMotion) 0.18f else 0.32f)

                        val layers = listOf(
                            Triple(secondary.copy(alpha = vis * 0.45f), 5.dp.toPx(), 1.15f),
                            Triple(accent.copy(alpha = vis * 0.32f), 14.dp.toPx(), 1.0f),
                            Triple(Color.White.copy(alpha = vis * 0.95f), 2.5.dp.toPx(), 1.0f),
                        )
                        layers.forEachIndexed { layerIdx, (color, strokePx, scale) ->
                            val path = Path()
                            val step = 2
                            var prevX = padX
                            var prevY = cy
                            for (i in 0 until 128 step step) {
                                val x = padX + (i.toFloat() / 127f) * usableW
                                val sample = smoothWave[i] * scale
                                val shimmer = sin(i * 0.15f + t * 2.5f + layerIdx).toFloat() * 0.03f * (0.3f + smoothRms)
                                val y = cy - (sample + shimmer) * amp
                                if (i == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    val midX = (prevX + x) / 2f
                                    path.quadraticTo(prevX, prevY, midX, (prevY + y) / 2f)
                                }
                                prevX = x
                                prevY = y
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                            )
                        }
                    }

                    VisualizerStyle.CRYSTAL -> {
                        val energy = smoothRms * gain
                        val orbs = listOf(
                            Triple(0.28f + 0.08f * sin(t * 0.6f), 0.36f + 0.06f * cos(t * 0.5f), accent),
                            Triple(0.72f + 0.07f * cos(t * 0.45f), 0.62f + 0.08f * sin(t * 0.55f), secondary),
                            Triple(0.50f + 0.05f * sin(t * 0.8f), 0.50f + 0.05f * cos(t * 0.7f), Color.White),
                        )
                        orbs.forEachIndexed { idx, (fx, fy, col) ->
                            val center = Offset(w * fx, h * fy)
                            val r = min(w, h) * (0.24f + energy * (0.26f + idx * 0.06f))
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        col.copy(alpha = vis * (0.38f + energy * 0.35f)),
                                        col.copy(alpha = vis * 0.08f),
                                        Color.Transparent,
                                    ),
                                    center = center,
                                    radius = r,
                                ),
                                radius = r,
                                center = center,
                            )
                        }

                        for (ring in 0..2) {
                            val sides = 6
                            val r = min(w, h) * (0.18f + ring * 0.11f + (if (ring == 0) bass else mid) * 0.10f * gain)
                            val rot = t * (0.18f + ring * 0.08f) * (if (ring % 2 == 0) 1f else -1f)
                            val poly = Path()
                            for (s in 0..sides) {
                                val a = rot + s * (2f * PI.toFloat() / sides)
                                val px = cx + cos(a) * r
                                val py = cy + sin(a) * r
                                if (s == 0) poly.moveTo(px, py) else poly.lineTo(px, py)
                            }
                            drawPath(
                                path = poly,
                                color = (if (ring == 1) accent else Color.White).copy(alpha = vis * (0.22f + treble * 0.25f)),
                                style = Stroke(width = (1.8f - ring * 0.3f).dp.toPx()),
                            )
                        }
                    }
                }
            }

            if (style == VisualizerStyle.ORBIT) {
                val baseDp = if (fullBleed) 164f else 132f
                val pulseScale = 1f + (smoothRms * (if (reduceMotion) 0.03f else 0.08f))
                Box(
                    modifier = Modifier
                        .size((baseDp * pulseScale).dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                        .background(Color(0xFF15161D)),
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            val statusText = when {
                loading -> "Cargando audio…"
                !playing -> "En pausa"
                frame == null -> "Esperando señal de audio…"
                else -> ""
            }
            if (statusText.isNotEmpty() && !fullBleed) {
                Text(
                    text = statusText,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CrystallizedArtworkBackdrop(artworkUrl = song.thumbnailUrl)

        if (isFullScreen) {
            // 100% Edge-to-Edge Immersive FullScreen Visualizer
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showFullScreenHud = !showFullScreenHud },
                    ),
            ) {
                VisualizerStage(modifier = Modifier.fillMaxSize(), fullBleed = true)

                // Minimal song badge subtly tucked in the bottom-left corner when HUD is hidden
                AnimatedVisibility(
                    visible = !showFullScreenHud,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                ) {
                    Column {
                        Text(
                            song.title,
                            color = Color.White.copy(alpha = 0.65f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artist,
                            color = Color.White.copy(alpha = 0.42f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Tap-to-reveal floating immersive HUD (auto-hides after 4s)
                AnimatedVisibility(
                    visible = showFullScreenHud,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        // Top floating mode chips + Exit FullScreen button
                        Row(
                            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                VisualizerStyle.entries.forEach { mode ->
                                    FilterChip(
                                        selected = style == mode,
                                        onClick = { style = mode; prefs.setVisualizerStyle(mode.name) },
                                        label = { Text(mode.label, fontSize = 12.sp, maxLines = 1) },
                                        colors = mobileChipColors(),
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { isFullScreen = false; showFullScreenHud = false },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                            ) {
                                Icon(Icons.Rounded.FullscreenExit, "Salir de pantalla completa", tint = Color.White)
                            }
                        }

                        // Compact floating bottom transport pill
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xAA10121A),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    song.thumbnailUrl,
                                    null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                                )
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(song.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = Color.White.copy(alpha = 0.68f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = AndroidPlayerEngine::skipPrevious, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = Color.White)
                                }
                                FilledIconButton(
                                    onClick = AndroidPlayerEngine::togglePlayPause,
                                    modifier = Modifier.size(44.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                                ) {
                                    Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pausar" else "Reproducir")
                                }
                                IconButton(onClick = AndroidPlayerEngine::skipNext, modifier = Modifier.size(44.dp)) {
                                    Icon(Icons.Rounded.SkipNext, "Siguiente", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Standard Visualizer layout with FullScreen button
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver al reproductor", tint = Color.White)
                    }
                    Text(
                        "Visualizador",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Suave",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = reduceMotion,
                        onCheckedChange = { reduceMotion = it; prefs.setReduceVisualizerMotion(it) },
                        modifier = Modifier.height(40.dp).semantics { contentDescription = "Reducir movimiento" },
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(
                        onClick = { isFullScreen = true; showFullScreenHud = true },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Rounded.Fullscreen, "Pantalla completa", tint = Color.White)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    VisualizerStyle.entries.forEach { mode ->
                        FilterChip(
                            selected = style == mode,
                            onClick = { style = mode; prefs.setVisualizerStyle(mode.name) },
                            label = { Text(mode.label, fontSize = 12.sp, maxLines = 1) },
                            colors = mobileChipColors(),
                        )
                    }
                }

                VisualizerStage(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    fullBleed = false,
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x8814161E),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp)),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                song.thumbnailUrl,
                                null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(
                                    song.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    song.artist,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            IconButton(onClick = AndroidPlayerEngine::skipPrevious, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, "Anterior", tint = Color.White)
                            }
                            FilledIconButton(
                                onClick = AndroidPlayerEngine::togglePlayPause,
                                modifier = Modifier.size(48.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                            ) {
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pausar" else "Reproducir")
                            }
                            IconButton(onClick = AndroidPlayerEngine::skipNext, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Rounded.SkipNext, "Siguiente", tint = Color.White)
                            }
                        }
                        Slider(
                            value = seek ?: if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f,
                            onValueChange = { seek = it },
                            onValueChangeFinished = { seek?.let(AndroidPlayerEngine::seekFraction); seek = null },
                            enabled = duration > 0,
                            modifier = Modifier.fillMaxWidth().height(28.dp),
                        )
                    }
                }
            }
        }
    }
}
