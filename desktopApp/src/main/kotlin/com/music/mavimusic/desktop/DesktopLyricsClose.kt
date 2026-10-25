package com.music.mavimusic.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DesktopLyricsClose(onClose: () -> Unit) {
    TooltipArea(tooltip = { Surface { Text("Cerrar (Esc)", Modifier.padding(8.dp)) } }) {
        IconButton(onClick = onClose, modifier = Modifier.size(44.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Cerrar (Esc)", modifier = Modifier.size(22.dp))
        }
    }
}
