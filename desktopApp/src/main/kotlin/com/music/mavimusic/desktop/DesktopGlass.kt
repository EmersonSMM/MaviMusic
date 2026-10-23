package com.music.mavimusic.desktop

import androidx.compose.runtime.getValue

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** The frosted pane the floating bars are made of. */
@Composable
internal fun Modifier.desktopBarGlass(shape: Shape): Modifier =
    desktopFrosted(DesktopBarGlass, tintAlpha = 0.55f).border(0.5.dp, DesktopBarEdge, shape)

/** The edge of a chrome pane that meets the page, and so has to dissolve into it. */
internal enum class DesktopChromeEdge { NONE, BOTTOM, END, TOP }

/**
 * The same pane for the window's own chrome — square-edged, and a shade denser, because the sidebar
 * and top bar carry far more text than a floating bar does.
 *
 * [edge] names the side the page is on. The pane's tint is dissolved across the last [fade] of it so
 * the two meet in a gradient rather than on a line.
 */
@Composable
internal fun Modifier.desktopChromeGlass(
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0.2f,
): Modifier = desktopFrosted(
    opaque = DesktopSurface.copy(alpha = 0.88f),
    tintAlpha = 0.46f,
    edge = edge,
    fade = fade,
)

/**
 * The window's own chrome — title bar, top bar, sidebar. Over Windows 11's Mica or Acrylic
 * ([DesktopWindowBackdrop]) it is a light dark tint and nothing else, so DWM's material shows
 * through; the in-app blur would paint the page's backdrop over it. Otherwise the in-app glass.
 *
 * No dissolve over the material: it would fade the tint out into bare material right where the
 * opaque page begins, a lighter stripe along the seam rather than a softer one.
 */
@Composable
internal fun Modifier.desktopWindowGlass(
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0.2f,
): Modifier {
    val backdrop by DesktopWindowBackdrop.active.collectAsState()
    return if (backdrop == DesktopBackdrop.OFF) {
        desktopChromeGlass(edge, fade)
    } else {
        val base = background(Color.Black.copy(alpha = WINDOW_GLASS_TINT))
        if (DesktopPlatform.isMac && (edge == DesktopChromeEdge.BOTTOM || edge == DesktopChromeEdge.TOP)) {
            base.drawBehind {
                // Subtle liquid glass specular highlight along the top edge
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
        } else {
            base
        }
    }
}

/** Enough to keep white text readable over a bright wallpaper, little enough to let it through. */
private const val WINDOW_GLASS_TINT = 0.28f

/**
 * The separators in the window's chrome. Acrylic lets the wallpaper through bright, and the solid
 * dark divider cut across it as a black line; a faint white one reads as an edge in the glass
 * instead. Mica is dark enough for the usual one.
 */
@Composable
internal fun desktopChromeDivider(): Color {
    val backdrop by DesktopWindowBackdrop.active.collectAsState()
    return when {
        backdrop == DesktopBackdrop.ACRYLIC -> Color.White.copy(alpha = 0.14f)
        DesktopPlatform.isMac && backdrop != DesktopBackdrop.OFF -> Color.White.copy(alpha = 0.10f)
        else -> DesktopDivider
    }
}

@Composable
private fun Modifier.desktopFrosted(
    opaque: Color,
    tintAlpha: Float,
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0f,
): Modifier {
    val haze = LocalDesktopHaze.current
    val reduceDynamicBlur by DesktopAppearanceSettings.reduceDynamicBlur.collectAsState()
    if (haze == null || reduceDynamicBlur) {
        return this.background(edge.dissolve(opaque, fade) ?: SolidColor(opaque))
    }
    val dissolve = edge.dissolve(Color.Black, fade)
    return this.hazeEffect(state = haze) {
        // Spelled out rather than taken from a preset.
        blurEnabled = true
        backgroundColor = DesktopSurface
        blurRadius = 30.dp
        // Dark enough that white text on the pane always wins, translucent enough that the
        // colour of what is behind it still comes through.
        tints = listOf(HazeTint(DesktopSurface.copy(alpha = tintAlpha)))
        noiseFactor = 0.04f
        // Assigned either way: the scope belongs to the node and outlives the draw, so a pane that
        // stops dissolving — the leading stretch of the top bar, when the window grows back out of
        // compact — would otherwise keep the mask it was given in the other layout.
        mask = dissolve
    }
}

/** The gradient that takes [colour] to nothing across the pane's last [fade]. */
private fun DesktopChromeEdge.dissolve(colour: Color, fade: Float): Brush? {
    if (this == DesktopChromeEdge.NONE || fade <= 0f) return null
    return when (this) {
        DesktopChromeEdge.BOTTOM -> {
            val stops = arrayOf(
                0f to colour,
                (1f - fade).coerceIn(0f, 1f) to colour,
                1f to Color.Transparent,
            )
            Brush.verticalGradient(*stops)
        }
        DesktopChromeEdge.TOP -> {
            val stops = arrayOf(
                0f to Color.Transparent,
                fade.coerceIn(0f, 1f) to colour,
                1f to colour,
            )
            Brush.verticalGradient(*stops)
        }
        DesktopChromeEdge.END -> {
            val stops = arrayOf(
                0f to colour,
                (1f - fade).coerceIn(0f, 1f) to colour,
                1f to Color.Transparent,
            )
            Brush.horizontalGradient(*stops)
        }
        DesktopChromeEdge.NONE -> null
    }
}


/** Android's wording for the Automix line, so both players read alike. */
