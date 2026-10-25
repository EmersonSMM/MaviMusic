package com.music.mavimusic.desktop

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun updateActionForeground(background: Color): Color = if (background.luminance() > 0.45f) Color.Black else Color.White

@Composable
internal fun DesktopUpdateAction(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent,
            contentColor = updateActionForeground(DesktopAccent),
            disabledContainerColor = Color(0xFF424247), disabledContentColor = Color(0xFFE8E8ED))) {
        Text(label, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold)
    }
}
