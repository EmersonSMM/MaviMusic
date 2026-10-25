package com.music.mavimusic.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.awt.ComposeWindow
import java.awt.FileDialog
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DesktopDiagnosticsSettings() {
    val parent = LocalDesktopWindowScope.current?.window as? ComposeWindow
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    Text("Guarda versión, pantallas, escalado, renderizador e incidencias técnicas de esta sesión. Puedes revisar el informe antes de compartirlo. No incluye cookies ni credenciales.",
        style = MaterialTheme.typography.bodySmall, color = DesktopSecondary)
    TextButton(enabled = !busy, onClick = {
        val dialog = FileDialog(parent, "Guardar diagnóstico de MaviMusic", FileDialog.SAVE)
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
        dialog.file = "MaviMusic-diagnostico-$stamp.txt"
        val destination = try {
            dialog.isVisible = true
            dialog.file?.let { name -> dialog.directory?.let { directory -> Path.of(directory, name) } }
        } finally { dialog.dispose() }
        if (destination != null) scope.launch {
            busy = true
            status = null
            try {
                val report = DesktopDiagnostics.capture(parent)
                withContext(Dispatchers.IO) { DesktopDiagnostics.write(report, destination) }
                status = "Informe guardado. Revísalo antes de compartirlo."
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { status = "No se pudo guardar el informe. Comprueba la carpeta de destino." }
            finally { busy = false }
        }
    }) { Text(if (busy) "Guardando…" else "Exportar diagnóstico…") }
    status?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = DesktopSecondary) }
}
