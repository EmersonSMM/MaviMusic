package com.music.mavimusic.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.ui.icons.MaviMusicIcons

/**
 * Where playback is sent. Android routes through `AudioManager`; here the choice is a mixer.
 *
 * Enumerated when the dialog opens rather than held: devices come and go with what is plugged in.
 */
@Composable
internal fun DesktopAudioOutputDialog(onDismiss: () -> Unit) {
    val selected by DesktopAudioDevices.selected.collectAsState()
    val changes by DesktopAudioDevices.changes.collectAsState()
    val devices = remember(changes) { DesktopAudioDevices.available() }
    val airplayEnabled by DesktopAirPlaySettings.featureEnabled.collectAsState()
    DesktopDialogPanel(onDismiss = onDismiss, maxWidth = 440) {
        Column(Modifier.padding(horizontal = panelInset(22.dp), vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                DesktopStrings["audio_output", "Audio output"],
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                DesktopStrings["d_audio_output_subtitle", "Changes take effect immediately."],
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(10.dp))
            OutputRow(
                name = DesktopStrings["d_system_default", "System default"],
                description = "",
                chosen = selected == DesktopAudioDevices.SYSTEM_DEFAULT,
            ) {
                DesktopAudioDevices.select(DesktopAudioDevices.SYSTEM_DEFAULT)
                onDismiss()
            }
            devices.forEach { device ->
                OutputRow(cleanDeviceName(device.name), device.description, chosen = selected == device.id) {
                    DesktopAudioDevices.select(device.id)
                    onDismiss()
                }
            }

            val airplayEnabled by DesktopAirPlaySettings.featureEnabled.collectAsState()
            if (airplayEnabled && DesktopPlatform.isMac) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                val airplayDevices by DesktopAirPlaySettings.discoveredDevices.collectAsState()
                if (airplayDevices.isNotEmpty()) {
                    Text(
                        "Receptores AirPlay 2",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    )
                    airplayDevices.forEach { ap ->
                        OutputRow(ap.name, "${ap.type} • Red local", chosen = selected == ap.id) {
                            DesktopAudioDevices.select(ap.id)
                            onDismiss()
                        }
                    }
                }
                OutputRow(
                    name = "Enrutar en macOS (AirPlay / Salida del sistema)...",
                    description = "Abre el selector nativo de macOS para agrupar HomePods y Apple TVs",
                    chosen = false,
                ) {
                    DesktopAirPlayManager.openMacSoundSettings()
                    onDismiss()
                }
            }
        }
    }
}

/**
 * Sleek, compact audio output popover anchored right above the bottom player bar button,
 * offering a tidy scrollable list with redundant labels stripped.
 */
@Composable
internal fun DesktopAudioOutputPopover(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected by DesktopAudioDevices.selected.collectAsState()
    val changes by DesktopAudioDevices.changes.collectAsState()
    val devices = remember(changes) { DesktopAudioDevices.available() }
    val airplayEnabled by DesktopAirPlaySettings.featureEnabled.collectAsState()

    Surface(
        modifier = modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .shadow(16.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF16161A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Headphones,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        DesktopStrings["audio_output", "Salida de audio"],
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = Color.White,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = DesktopSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    CompactOutputRow(
                        name = DesktopStrings["d_system_default", "Por defecto del sistema"],
                        chosen = selected == DesktopAudioDevices.SYSTEM_DEFAULT,
                        onClick = {
                            DesktopAudioDevices.select(DesktopAudioDevices.SYSTEM_DEFAULT)
                            onDismiss()
                        },
                    )
                }
                items(devices) { device ->
                    CompactOutputRow(
                        name = cleanDeviceName(device.name),
                        chosen = selected == device.id,
                        onClick = {
                            DesktopAudioDevices.select(device.id)
                            onDismiss()
                        },
                    )
                }

                if (airplayEnabled && DesktopPlatform.isMac) {
                    item {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        val airplayDevices by DesktopAirPlaySettings.discoveredDevices.collectAsState()
                        if (airplayDevices.isNotEmpty()) {
                            Text(
                                "Receptores AirPlay 2",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    val airplayDevices = DesktopAirPlaySettings.discoveredDevices.value
                    items(airplayDevices) { ap ->
                        CompactOutputRow(
                            name = "${ap.name} (${ap.type})",
                            chosen = selected == ap.id,
                            onClick = {
                                DesktopAudioDevices.select(ap.id)
                                onDismiss()
                            },
                        )
                    }
                    item {
                        CompactOutputRow(
                            name = "Enrutar en macOS...",
                            chosen = false,
                            onClick = {
                                DesktopAirPlayManager.openMacSoundSettings()
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactOutputRow(
    name: String,
    chosen: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (chosen) Color.White.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (chosen) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.88f),
            fontWeight = if (chosen) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 12.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (chosen) {
            Spacer(Modifier.width(8.dp))
            Icon(
                MaviMusicIcons.Check,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

private fun cleanDeviceName(raw: String): String {
    return raw
        .removePrefix("Direct Audio Device: ")
        .removePrefix("Default Audio Device: ")
        .trim()
}

@Composable
private fun OutputRow(name: String, description: String, chosen: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .desktopRowClickable(onClick = onClick)
            .padding(horizontal = panelInset(12.dp), vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (description.isNotBlank()) {
                Text(
                    description,
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (chosen) {
            Spacer(Modifier.width(10.dp))
            Icon(MaviMusicIcons.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}
