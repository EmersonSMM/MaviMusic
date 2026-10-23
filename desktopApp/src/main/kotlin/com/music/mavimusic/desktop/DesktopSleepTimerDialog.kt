package com.music.mavimusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun DesktopSleepTimerDialog(
    sleepRemainingMs: Long?,
    onDismiss: () -> Unit,
) {
    val activeMinutes by DesktopSleepTimer.minutes.collectAsState()
    val afterTrack by DesktopSleepTimer.afterTrack.collectAsState()
    val isActive = activeMinutes != null || afterTrack

    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 380) {
        Column(Modifier.fillMaxWidth().padding(22.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = DesktopAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Temporizador de apagado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        if (isActive) {
                            val statusText = when {
                                afterTrack -> "Se pausará al finalizar la canción actual"
                                sleepRemainingMs != null -> {
                                    val m = sleepRemainingMs / 60000L
                                    val s = (sleepRemainingMs % 60000L) / 1000L
                                    "Se pausará en %02d:%02d".format(m, s)
                                }
                                else -> "Activo"
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = DesktopSecondary,
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = DesktopSecondary, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = DesktopDivider)
            Spacer(Modifier.height(10.dp))

            // Presets: 15, 30, 45, 60 minutos
            DesktopSleepTimer.presets.forEach { mins ->
                val selected = activeMinutes == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                        .clickable {
                            DesktopSleepTimer.start(mins)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$mins minutos",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.White,
                    )
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = DesktopAccent, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Option: Al finalizar canción
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (afterTrack) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable {
                        DesktopSleepTimer.startAfterTrack()
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Al terminar esta canción",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (afterTrack) FontWeight.SemiBold else FontWeight.Normal,
                    color = Color.White,
                )
                if (afterTrack) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = DesktopAccent, modifier = Modifier.size(18.dp))
                }
            }

            if (isActive) {
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        DesktopSleepTimer.cancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesktopDestructive.copy(alpha = 0.15f),
                        contentColor = DesktopDestructive,
                    ),
                ) {
                    Text("Desactivar temporizador", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }
        }
    }
}
