package com.music.mavimusic.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** No native call is made until the explanatory dialog is explicitly confirmed. */
@Composable
internal fun DesktopSessionRecovery() {
    if (!DesktopPlatform.isMac) return
    var confirming by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.padding(vertical = 8.dp)) {
        Text("La app no accede al llavero al iniciarse. Puedes iniciar sesión normalmente o recuperar las sesiones de la versión anterior.",
            color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { confirming = true }, enabled = !busy) {
            Text(if (busy) "Recuperando…" else "Recuperar sesiones anteriores…")
        }
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = DesktopSecondary) }
    }
    if (confirming) AlertDialog(
        onDismissRequest = { confirming = false },
        title = { Text("Recuperar tus sesiones de MaviMusic") },
        text = { Text("Solo se leerán las sesiones de Google y Spotify que esta app guardó anteriormente bajo MaviMusic. No se solicitan otras contraseñas del llavero. macOS puede pedirte permiso. Las sesiones recuperadas se guardarán localmente como antes; no habrá acceso automático al llavero. También puedes cancelar e iniciar sesión de nuevo.") },
        confirmButton = {
            TextButton(onClick = {
                confirming = false
                busy = true
                scope.launch {
                    message = runCatching { DesktopAccounts.recoverSessionsFromKeychain() }.fold(
                        onSuccess = { if (it > 0) "$it sesión(es) recuperada(s). La sesión ya está disponible en la app." else "No hay sesiones pendientes de recuperar. Puedes iniciar sesión normalmente." },
                        onFailure = { "No se pudo completar la recuperación. Puedes volver a intentarlo o iniciar sesión normalmente." },
                    )
                    busy = false
                }
            }) { Text("Recuperar mis sesiones") }
        },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } },
    )
}
