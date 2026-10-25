package com.music.mavimusic.desktop

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

@Composable
internal fun DesktopShortcutSettings() {
    if (!DesktopPlatform.isWindows) {
        Text("Spotlight global en macOS: ⌘K o ⌘⇧K, mediante Carbon. La app debe seguir abierta.")
        return
    }
    val status by DesktopWindowsHotkey.state.collectAsState()
    var shortcut by remember(status.shortcut) { mutableStateOf(status.shortcut) }
    OutlinedTextField(value = shortcut, onValueChange = { shortcut = it }, singleLine = true,
        label = { Text("Atajo global de Spotlight") }, supportingText = { Text(status.message) })
    TextButton(onClick = { DesktopWindowsHotkey.configure(shortcut) }) { Text("Aplicar atajo") }
}
