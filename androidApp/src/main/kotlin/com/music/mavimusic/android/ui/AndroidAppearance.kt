package com.music.mavimusic.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Desktop's neutral palette, with Android's system font and touch sizing. */
@Composable
fun MaviAndroidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF303034),
            onPrimaryContainer = Color.White,
            background = Color(0xFF141416),
            onBackground = Color.White,
            surface = Color(0xFF0D0D0F),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1C1C1E),
            onSurfaceVariant = Color(0xFF98989F),
            outline = Color(0xFF3A3A3E),
            outlineVariant = Color(0xFF2C2C2E),
            error = Color(0xFFFF453A),
        ),
        content = content,
    )
}

@Composable
fun mobileChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
)
