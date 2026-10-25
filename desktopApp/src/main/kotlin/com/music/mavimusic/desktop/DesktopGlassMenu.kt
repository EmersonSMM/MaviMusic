package com.music.mavimusic.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

internal val DesktopMenuShape = RoundedCornerShape(20.dp)
internal val DesktopMenuRowShape = RoundedCornerShape(12.dp)

/** One glass treatment for anchored, dropdown and context menus; respects reduced-blur preference. */
@Composable
internal fun Modifier.desktopMenuGlass(): Modifier {
    val base = shadow(14.dp, DesktopMenuShape).clip(DesktopMenuShape)
    val fill = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f)
        base.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)) else base.desktopChromeGlass()
    return fill.border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)), DesktopMenuShape)
}

@Composable
internal fun DesktopGlassMenu(expanded: Boolean, onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(expanded, onDismissRequest, modifier = modifier.desktopMenuGlass(),
        shape = DesktopMenuShape, containerColor = Color.Transparent, tonalElevation = 0.dp,
        shadowElevation = 0.dp, content = content)
}

@Composable
internal fun DesktopGlassMenuItem(text: @Composable () -> Unit, onClick: () -> Unit,
    modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    interactionSource: MutableInteractionSource? = null) {
    val resolved = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f && colors.textColor == Color.White)
        MenuDefaults.itemColors() else colors
    DropdownMenuItem(text, onClick, modifier.padding(horizontal = 4.dp).clip(DesktopMenuRowShape),
        leadingIcon, trailingIcon, enabled, resolved, contentPadding, interactionSource)
}
