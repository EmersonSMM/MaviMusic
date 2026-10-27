package com.music.mavimusic.desktop.remote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.desktop.DesktopAccent
import com.music.mavimusic.desktop.DesktopSecondary
import com.music.mavimusic.desktop.DesktopStrings
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DesktopRemoteSettingsUi() {
    val enabled by DesktopRemoteDiscovery.enabled.collectAsState()
    val allowInBackground by DesktopRemoteDiscovery.allowInBackground.collectAsState()
    val activePort by DesktopRemoteDiscovery.activePort.collectAsState()
    val devices by DesktopRemoteSecurity.devices.collectAsState()
    val connectedCount by DesktopRemoteBridge.connectedClientsCount.collectAsState()

    var copiedToast by remember { mutableStateOf(false) }
    var pinRefreshKey by remember { mutableStateOf(0) }
    val currentPin = remember(pinRefreshKey) { DesktopRemoteSecurity.getPin() }
    val pairingUrl = remember(pinRefreshKey, activePort) { DesktopRemoteDiscovery.pairingUrl() }
    val qrBitmap = remember(pairingUrl) { DesktopRemoteDiscovery.generateQrBitmap(pairingUrl, 220) }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // Main Toggle Row
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    val next = !enabled
                    DesktopRemoteDiscovery.setEnabled(next)
                    if (next) DesktopRemoteServer.start() else DesktopRemoteServer.stop()
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    DesktopStrings["remote_server_title", "Servidor de Control Remoto Web"],
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    DesktopStrings[
                        "remote_server_desc",
                        "Controla la reproducción desde el navegador de tu iPhone o móvil en la misma red Wi-Fi sin instalar ninguna aplicación.",
                    ],
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { next ->
                    DesktopRemoteDiscovery.setEnabled(next)
                    if (next) DesktopRemoteServer.start() else DesktopRemoteServer.stop()
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = DesktopAccent,
                    uncheckedThumbColor = Color(0xFFAAAAAA),
                    uncheckedTrackColor = Color(0x33FFFFFF),
                ),
            )
        }

        if (enabled) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            // URL & Status Card
            val url = DesktopRemoteDiscovery.remoteUrl()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33000000))
                    .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (DesktopRemoteServer.isRunning()) Color(0xFF22C55E) else Color(0xFFEF4444))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (DesktopRemoteServer.isRunning()) "Servidor Activo (Puerto $activePort)" else "Servidor Detenido",
                            color = if (DesktopRemoteServer.isRunning()) Color(0xFF4ADE80) else Color(0xFFF87171),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (connectedCount > 0) {
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "• $connectedCount dispositivo(s) conectado(s)",
                                color = DesktopAccent,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        url,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                    )
                }

                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = {
                        runCatching {
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(url), null)
                            copiedToast = true
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text(if (copiedToast) "¡Copiado!" else "Copiar URL", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Pairing: QR & PIN side by side
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x251C1C26))
                    .border(0.5.dp, Color(0x2AFFFFFF), RoundedCornerShape(14.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (qrBitmap != null) {
                    Box(
                        Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = qrBitmap,
                            contentDescription = "Código QR de vinculación",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        "Vincular iPhone o Navegador",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Apunta con la cámara de tu iPhone para conectarte sin escribir nada, o abre la URL e introduce este PIN:",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x40A855F7))
                                .border(1.dp, DesktopAccent, RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                currentPin.chunked(1).joinToString("  "),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                letterSpacing = 2.sp,
                            )
                        }

                        Spacer(Modifier.width(12.dp))
                        TextButton(
                            onClick = {
                                DesktopRemoteSecurity.regeneratePin()
                                pinRefreshKey++
                            }
                        ) {
                            Text("Regenerar", color = DesktopAccent, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Background execution option
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { DesktopRemoteDiscovery.setAllowInBackground(!allowInBackground) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Permitir control en segundo plano",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "Mantiene el servidor activo si la app está minimizada en la barra de tareas o menú.",
                        color = DesktopSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = allowInBackground,
                    onCheckedChange = { DesktopRemoteDiscovery.setAllowInBackground(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DesktopAccent,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // Authorized Devices Section
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Dispositivos Autorizados (${devices.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (devices.isNotEmpty()) {
                    TextButton(onClick = { DesktopRemoteSecurity.revokeAllDevices() }) {
                        Text("Revocar todos", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }

            if (devices.isEmpty()) {
                Text(
                    "No hay dispositivos vinculados actualmente.",
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            } else {
                val dateFormat = remember { SimpleDateFormat("d MMM HH:mm", Locale.getDefault()) }
                Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    devices.forEach { dev ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x18FFFFFF))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(dev.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Última conexión: ${dateFormat.format(Date(dev.lastSeenAtMs))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DesktopSecondary,
                                )
                            }
                            TextButton(onClick = { DesktopRemoteSecurity.revokeDevice(dev.id) }) {
                                Text("Revocar", color = Color(0xFFF87171), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
