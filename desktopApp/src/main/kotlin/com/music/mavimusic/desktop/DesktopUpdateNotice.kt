package com.music.mavimusic.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

private object UpdateNotice { var open by mutableStateOf(false) }

@Composable
internal fun DesktopUpdatePill() {
    val windows by DesktopWindowsUpdater.state.collectAsState()
    val mac by DesktopMacUpdater.state.collectAsState()
    val version = if (DesktopPlatform.isWindows) windows.latestVersion.takeIf { windows.updateAvailable } else mac.availableVersion
    if (version.isNullOrBlank()) return
    Spacer(Modifier.width(8.dp))
    Surface(onClick = { if (DesktopPlatform.isMac) DesktopMacUpdater.check() else UpdateNotice.open = true },
        shape = RoundedCornerShape(16.dp), color = Color(0xFF503B1C), contentColor = Color(0xFFFFD88C)) {
        Text("● ACTUALIZACIÓN DISPONIBLE", fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
    }
}

@Composable
internal fun DesktopUpdateNoticeDialog() {
    if (!UpdateNotice.open) return
    Dialog(onDismissRequest = { UpdateNotice.open = false }) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.widthIn(max = 560.dp).heightIn(max = 680.dp)) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState())) {
                DesktopUpdateSettings()
                TextButton(onClick = { UpdateNotice.open = false }) { Text("Cerrar") }
            }
        }
    }
}
