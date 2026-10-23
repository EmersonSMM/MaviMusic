package com.music.mavimusic.desktop

import com.music.mavimusic.ui.components.PAGE_GUTTER
import androidx.compose.foundation.background
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mavimusic.desktopapp.generated.resources.Res
import mavimusic.desktopapp.generated.resources.sf_pro_display_bold
import mavimusic.desktopapp.generated.resources.sf_pro_display_heavy
import mavimusic.desktopapp.generated.resources.sf_pro_display_medium
import mavimusic.desktopapp.generated.resources.sf_pro_display_regular
import mavimusic.desktopapp.generated.resources.sf_pro_display_semibold
import dev.chrisbanes.haze.HazeState
import org.jetbrains.compose.resources.Font as composeFont

/**
 * The margin every page keeps from the window's edge.
 *
 * Desktop-only pages share Library's edge inset so switching destinations does not move the page
 * content sideways.
 */
internal val DesktopPageGutter = PAGE_GUTTER

internal val DesktopBackground = Color(0xFF202020)
internal val DesktopSurface = Color(0xFF0D0D0F)
internal val DesktopSurfaceRaised = Color(0xFF1C1C1E)
internal val DesktopGlass = Color(0x661C1C1E)
internal val DesktopGlassStrong = Color(0xCC0D0D0F)

/** The floating bars in the narrow layout. */
internal val DesktopBarGlass = Color(0xB324242A)

/** The backdrop the floating bars blur. */
internal val LocalDesktopHaze = staticCompositionLocalOf<HazeState?> { null }

/** How much a page's scrollable should keep clear of the floating bars. */
internal val LocalDesktopBottomInset = staticCompositionLocalOf { 0.dp }

/** Android's own edge on these bars: `Color.White.copy(alpha = 0.10f)`. */
internal val DesktopBarEdge = Color(0x1AFFFFFF)
internal val DesktopAccent = Color.White

/** Deleting, signing out, and anything that failed — the one place a colour survives, as on Android. */
internal val DesktopDestructive = Color(0xFFFF453A)

/** A chip the way Android draws a chosen segment: solid white with black on it, or nothing at all. */
@Composable
internal fun desktopChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = DesktopSecondary,
    selectedContainerColor = Color.White,
    selectedLabelColor = Color.Black,
)

/** Android's switch: a white track with a black knob on, a faint wash off. */
@Composable
internal fun desktopSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = Color.White,
    checkedThumbColor = Color.Black,
    checkedBorderColor = Color.Transparent,
    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
    uncheckedThumbColor = Color.White.copy(alpha = 0.35f),
    uncheckedBorderColor = Color.Transparent,
)
internal val DesktopSecondary = Color(0xFF8E8E93)
internal val DesktopDivider = Color(0xFF2C2C2E)
/** The app's dark scheme, supporting customizable background tones and themes. */
internal fun desktopColorScheme(theme: DesktopThemeTone = DesktopThemeTone.DARK_CLASSIC) = darkColorScheme(
    primary = DesktopAccent,
    // Black on the accent, now that the accent is white.
    onPrimary = Color.Black,
    primaryContainer = Color.White.copy(alpha = 0.16f),
    onPrimaryContainer = Color.White,
    background = theme.background,
    onBackground = Color.White,
    surface = theme.surface,
    onSurface = Color.White,
    surfaceVariant = theme.surfaceRaised,
    onSurfaceVariant = DesktopSecondary,
    outline = Color(0xFF2C2C2E),
    outlineVariant = DesktopDivider,
    surfaceTint = DesktopAccent,
)

@Composable
internal fun desktopTypography(): Typography {
    val sfProDisplay = FontFamily(
        composeFont(Res.font.sf_pro_display_regular, FontWeight.W400),
        composeFont(Res.font.sf_pro_display_medium, FontWeight.W500),
        composeFont(Res.font.sf_pro_display_semibold, FontWeight.W600),
        composeFont(Res.font.sf_pro_display_bold, FontWeight.W700),
        composeFont(Res.font.sf_pro_display_heavy, FontWeight.W800),
    )
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 36.sp, letterSpacing = (-0.8).sp),
        displayMedium = defaults.displayMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 30.sp),
        displaySmall = defaults.displaySmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 26.sp),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.4).sp),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 20.sp),
        titleLarge = defaults.titleLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 21.sp, letterSpacing = (-0.3).sp),
        titleMedium = defaults.titleMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 17.sp, letterSpacing = (-0.2).sp),
        titleSmall = defaults.titleSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 15.sp),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 16.sp),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 15.sp),
        bodySmall = defaults.bodySmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 13.sp),
        labelLarge = defaults.labelLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 14.sp),
        labelMedium = defaults.labelMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 13.sp),
        labelSmall = defaults.labelSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W500, fontSize = 12.sp),
    )
}

