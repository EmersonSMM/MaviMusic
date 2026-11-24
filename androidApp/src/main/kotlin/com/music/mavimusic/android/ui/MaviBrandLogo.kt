package com.music.mavimusic.android.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Animated MaviMusic wordmark with a smooth diagonal "curtain light-sweep" sheen
 * gliding across the letters over the signature warm amber-coral brand gradient.
 */
@Composable
fun MaviAnimatedLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp,
) {
    val transition = rememberInfiniteTransition(label = "mavi_logo_curtain")
    // Ranges from -0.6 to 2.2 so the curtain sweeps cleanly across the letters and pauses naturally.
    val sweep by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "curtain_phase",
    )

    val widthPx = fontSize.value * 16f
    val bandHalf = widthPx * 0.22f
    val centerX = sweep * widthPx

    val curtainBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0.0f to Color(0xFFFFB84D),
            0.35f to Color(0xFFFF8C32),
            0.50f to Color(0xFFFFF6DC), // Bright luminous curtain core
            0.65f to Color(0xFFFF7A2F),
            1.0f to Color(0xFFFF5E2B),
        ),
        start = Offset(centerX - bandHalf, 0f),
        end = Offset(centerX + bandHalf, fontSize.value * 1.4f),
    )

    Text(
        text = "MaviMusic",
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            brush = curtainBrush,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.6).sp,
        ),
    )
}
