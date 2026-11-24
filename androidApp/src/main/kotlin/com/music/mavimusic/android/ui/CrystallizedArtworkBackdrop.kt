package com.music.mavimusic.android.ui

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.mavimusic.ui.theme.rememberArtworkPalette

/**
 * Full-screen crystallized frosted-glass backdrop derived from the currently playing track's
 * album artwork. Adapts smoothly as tracks change while keeping foreground content crisp and legible.
 */
@Composable
fun CrystallizedArtworkBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(artworkUrl)
    val bgColor by animateColorAsState(
        targetValue = palette.background,
        animationSpec = tween(durationMillis = 1100),
        label = "crystal_bg",
    )
    val washColor by animateColorAsState(
        targetValue = palette.wash,
        animationSpec = tween(durationMillis = 1100),
        label = "crystal_wash",
    )
    val accentColor by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = tween(durationMillis = 1100),
        label = "crystal_accent",
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0B0F))) {
        // 1. Softly blurred full-bleed album artwork layer (hardware RenderEffect blur on Android 12+)
        if (!artworkUrl.isNullOrBlank()) {
            val blurMod = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Modifier.blur(56.dp)
            } else {
                Modifier
            }
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurMod)
                    .alpha(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.32f else 0.16f),
            )
        }

        // 2. Deep chromatic wash & crystallized glass facets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Rich vertical base blend from artwork palette
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        washColor.copy(alpha = 0.42f),
                        bgColor.copy(alpha = 0.52f),
                        Color(0xFF0A0B0F).copy(alpha = 0.88f),
                    )
                )
            )

            // Upper-right radial chromatic bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.24f),
                        washColor.copy(alpha = 0.10f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.88f, h * 0.10f),
                    radius = maxOf(w, h) * 0.72f,
                ),
                center = Offset(w * 0.88f, h * 0.10f),
                radius = maxOf(w, h) * 0.72f,
            )

            // Lower-left secondary bloom
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.16f),
                        Color.Transparent,
                    ),
                    center = Offset(w * 0.12f, h * 0.82f),
                    radius = maxOf(w, h) * 0.60f,
                ),
                center = Offset(w * 0.12f, h * 0.82f),
                radius = maxOf(w, h) * 0.60f,
            )

            // Crystalline glass facet 1 — diagonal prismatic plane
            val facet1 = Path().apply {
                moveTo(0f, h * 0.14f)
                lineTo(w * 0.78f, 0f)
                lineTo(w, h * 0.30f)
                lineTo(w * 0.24f, h * 0.46f)
                close()
            }
            drawPath(
                path = facet1,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.055f),
                        accentColor.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    start = Offset(0f, h * 0.14f),
                    end = Offset(w, h * 0.36f),
                ),
            )

            // Crystalline glass facet 2 — lower counter-angled refraction shard
            val facet2 = Path().apply {
                moveTo(w * 0.15f, h * 0.42f)
                lineTo(w, h * 0.28f)
                lineTo(w, h * 0.68f)
                lineTo(0f, h * 0.76f)
                close()
            }
            drawPath(
                path = facet2,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.04f),
                        washColor.copy(alpha = 0.09f),
                    ),
                    start = Offset(0f, h * 0.55f),
                    end = Offset(w, h * 0.34f),
                ),
            )

            // Subtle crisp crystal edge highlights
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(0f, h * 0.14f),
                    end = Offset(w * 0.78f, 0f),
                ),
                start = Offset(0f, h * 0.14f),
                end = Offset(w * 0.78f, 0f),
                strokeWidth = 1.5f,
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, accentColor.copy(alpha = 0.14f), Color.Transparent),
                    start = Offset(w * 0.15f, h * 0.42f),
                    end = Offset(w, h * 0.28f),
                ),
                start = Offset(w * 0.15f, h * 0.42f),
                end = Offset(w, h * 0.28f),
                strokeWidth = 1.5f,
            )
        }

        // 3. Soft readability veil ensuring high contrast on every screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x660A0B0F),
                            Color(0x880A0B0F),
                            Color(0xBB0A0B0F),
                        )
                    )
                )
        )
    }
}
