package com.music.mavimusic.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.android.MobileTab

@Composable
fun MobileBrand() {
    MaviAnimatedLogo(fontSize = 23.sp)
}

@Composable
fun MobileSidebar(
    selected: MobileTab,
    expanded: Boolean,
    timerActive: Boolean,
    onSelect: (MobileTab) -> Unit,
    onTimer: () -> Unit,
    onConnect: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(
        Modifier.width(if (expanded) 184.dp else 80.dp).fillMaxHeight()
            .background(Color(0xB8111319))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 20.dp),
        horizontalAlignment = if (expanded) Alignment.Start else Alignment.CenterHorizontally,
    ) {
        if (expanded) {
            MobileBrand()
            Text("TU MÚSICA", color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp, letterSpacing = 1.6.sp, modifier = Modifier.padding(top = 28.dp, bottom = 12.dp))
        } else {
            Icon(Icons.Rounded.MusicNote, "MaviMusic", tint = Color(0xFFFF9A32), modifier = Modifier.padding(bottom = 24.dp))
        }
        MobileTab.entries.forEach { tab ->
            val active = selected == tab
            Surface(
                onClick = { onSelect(tab) },
                shape = RoundedCornerShape(12.dp),
                color = if (active) Color.White.copy(alpha = 0.10f) else Color.Transparent,
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            ) {
                if (expanded) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(tab.icon, null, tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(tab.label, fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(tab.icon, tab.label, tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Text(tab.label, fontSize = 10.sp, color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        SidebarUtility(Icons.Rounded.Bedtime, if (timerActive) "Temporizador activo" else "Temporizador", expanded, onTimer)
        SidebarUtility(Icons.Rounded.Computer, "Escritorio", expanded, onConnect)
        SidebarUtility(Icons.Rounded.Settings, "Ajustes", expanded, onSettings)
    }
}

@Composable
private fun SidebarUtility(icon: ImageVector, label: String, expanded: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = if (expanded) 12.dp else 0.dp),
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        if (expanded) {
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
