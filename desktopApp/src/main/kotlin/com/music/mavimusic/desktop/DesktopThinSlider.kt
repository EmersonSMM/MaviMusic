package com.music.mavimusic.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Apple Music's scrubber, ported from Android's `ThinSlider`. */
@Composable
internal fun DesktopThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    /** Optional formatter to display target timestamp in a floating tooltip while dragging. */
    tooltipText: ((Float) -> String)? = null,
    /** Sends a sheen travelling along the bar for as long as it is true. */
    mixing: Boolean = false,
    /**
     * Span of the track, as fractions of its duration, that the next Automix transition is planned
     * to occupy.
     */
    transitionWindow: ClosedFloatingPointRange<Float>? = null,
    idleHeight: Dp = 7.dp,
    activeHeight: Dp = 12.dp,
    activeColor: Color = Color.White.copy(alpha = 0.92f),
    inactiveColor: Color = Color.White.copy(alpha = 0.26f),
    /** Halfway between the two track colours: visible against unplayed, invisible under played. */
    markerColor: Color = Color.White.copy(alpha = 0.5f),
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var barWidthPx by remember { mutableIntStateOf(0) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val currentTooltipText by rememberUpdatedState(tooltipText)

    val height by animateDpAsState(
        targetValue = if (dragging) activeHeight else idleHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sliderHeight",
    )

    Box(
        modifier = modifier
            // Generous invisible hit target — the visible bar is only ~7dp.
            .height(activeHeight + 22.dp)
            .onSizeChanged { barWidthPx = it.width }
            // One gesture loop for both clicks and drags.
            // Uses rememberUpdatedState so song duration changes never read a stale closure.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    dragging = true
                    val initialFraction = (down.position.x / widthPx).coerceIn(0f, 1f)
                    dragFraction = initialFraction
                    currentOnValueChange(initialFraction)

                    while (true) {
                        val event = awaitPointerEvent()
                        val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!pointer.pressed) {
                            pointer.consume()
                            break
                        }
                        if (pointer.positionChanged()) {
                            val liveWidthPx = size.width.toFloat().coerceAtLeast(1f)
                            val newFraction = (pointer.position.x / liveWidthPx).coerceIn(0f, 1f)
                            dragFraction = newFraction
                            currentOnValueChange(newFraction)
                            pointer.consume()
                        }
                    }

                    dragging = false
                    currentOnValueChangeFinished?.invoke()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (dragging && currentTooltipText != null) {
            val label = currentTooltipText?.invoke(dragFraction).orEmpty()
            if (label.isNotEmpty()) {
                val xOffsetPx = if (barWidthPx > 0) {
                    ((dragFraction - 0.5f) * barWidthPx).roundToInt()
                } else {
                    0
                }
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(xOffsetPx, -30),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xF2161822),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        shadowElevation = 8.dp,
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = inactiveColor, cornerRadius = radius)
            // Between the two track colours, and drawn *under* the played fill.
            transitionWindow?.let { window ->
                val from = size.width * window.start.coerceIn(0f, 1f)
                val to = size.width * window.endInclusive.coerceIn(0f, 1f)
                if (to > from) {
                    drawRoundRect(
                        color = markerColor,
                        topLeft = Offset(from, 0f),
                        // Never thinner than the bar is tall, so a short transition still reads as
                        // a dot rather than a hairline.
                        size = Size((to - from).coerceAtLeast(size.height), size.height),
                        cornerRadius = radius,
                    )
                }
            }
            val displayFraction = if (dragging) dragFraction else value.coerceIn(0f, 1f)
            val filled = size.width * displayFraction
            if (filled > 0f && !mixing) {
                drawRoundRect(
                    color = activeColor,
                    size = Size(filled.coerceAtLeast(size.height), size.height),
                    cornerRadius = radius,
                )
            }
        }
        // Composed only while mixing rather than drawn conditionally inside the Canvas above.
        AnimatedVisibility(
            visible = mixing,
            enter = fadeIn(tween(durationMillis = 420)),
            exit = fadeOut(tween(durationMillis = 520)),
        ) {
            DesktopMixSheen(height = height)
        }
    }
}

/**
 * A single soft highlight travelling the length of the bar, over and over, while two tracks are
 * being mixed.
 */
@Composable
private fun DesktopMixSheen(height: Dp) {
    val transition = rememberInfiniteTransition(label = "mixSheen")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mixSheenPhase",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val band = size.width * SHEEN_BAND_FRACTION
        // Travels from fully off the left edge to fully off the right, so the highlight enters and
        // leaves rather than materialising mid-bar.
        val centre = -band + (size.width + band * 2f) * phase
        drawRoundRect(
            brush = Brush.linearGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    0.5f to Color.White.copy(alpha = 0.95f),
                    1f to Color.Transparent,
                ),
                start = Offset(centre - band / 2f, 0f),
                end = Offset(centre + band / 2f, 0f),
            ),
            cornerRadius = CornerRadius(size.height / 2f),
        )
    }
}

/** Width of the travelling highlight, as a fraction of the whole bar. */
private const val SHEEN_BAND_FRACTION = 0.7f

/**
 * Pure helper computing the exact seek target in milliseconds from a pointer X coordinate [xPx]
 * and the real measured bar width [barWidthPx] for a track of length [durationMs].
 * No segment snapping or coarse rounding is applied.
 */
internal fun computeSeekTargetMs(xPx: Float, barWidthPx: Float, durationMs: Long): Long {
    if (durationMs <= 0L || barWidthPx <= 0f) return 0L
    val fraction = (xPx / barWidthPx).coerceIn(0f, 1f)
    return (fraction.toDouble() * durationMs.toDouble()).roundToLong().coerceIn(0L, durationMs)
}
