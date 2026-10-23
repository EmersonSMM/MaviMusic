package com.music.mavimusic.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DesktopDownloadRepair() {
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column {
        Text("Actualiza carátulas y etiquetas de las descargas existentes. Los MP3 se etiquetan sin volver a comprimir el audio.",
            color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
        TextButton(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    val songs = withContext(Dispatchers.IO) { DesktopDownloadLibrary.songs.value.filter(DesktopDownloadManager::isSaved) }
                    var repaired = 0
                    var failed = 0
                    for ((index, song) in songs.withIndex()) {
                        status = "Revisando ${index + 1} de ${songs.size}…"
                        val result = withContext(Dispatchers.IO) { DesktopDownloadManager.repairMetadata(song) }
                        result.onSuccess { saved ->
                            repaired++
                            withContext(Dispatchers.IO) { DesktopDownloadLibrary.repair(song, saved) }
                        }.onFailure { failed++ }
                    }
                    status = if (songs.isEmpty()) "No hay descargas registradas en esta carpeta."
                        else "$repaired reparadas; $failed pendientes. Las descargas que fallan conservan su archivo original."
                } finally { busy = false }
            }
        }) { Text(if (busy) "Reparando…" else "Reparar carátulas y etiquetas") }
        status?.let { Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall) }
    }
}
