package com.music.mavimusic.android.ui

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import com.music.mavimusic.android.data.DesktopRemoteAddress
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.audio.SleepTimer
import com.music.mavimusic.android.data.AndroidPreferences
import com.music.mavimusic.android.data.UserPlaylist
import com.music.mavimusic.data.model.Song

@Composable
fun SleepTimerModal(
    onDismiss: () -> Unit
) {
    val minutesLeft by SleepTimer.minutesLeft.collectAsState()
    val endOfSong by SleepTimer.stopAtEndOfSong.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Temporizador de apagado", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (minutesLeft != null) {
                    Text(
                        text = if (endOfSong) "Se apagará al terminar la canción actual" else "Se apagará en aprox. $minutesLeft min",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = { SleepTimer.cancel(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Desactivar temporizador", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                listOf(15, 30, 45, 60).forEach { mins ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                SleepTimer.start(mins)
                                onDismiss()
                            },
                        color = Color(0xFF22222E)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Timer, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("$mins minutos", color = Color.White, fontSize = 14.sp)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            SleepTimer.startEndOfSong()
                            onDismiss()
                        },
                    color = Color(0xFF22222E)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Al finalizar esta canción", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun CreatePlaylistModal(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Playlist", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Nombre de la lista...", color = Color.Gray) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Crear", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AddToPlaylistModal(
    song: Song,
    playlists: List<UserPlaylist>,
    onSelectPlaylist: (UserPlaylist) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Añadir a playlist", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onCreateNew() },
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear nueva playlist", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (playlists.isEmpty()) {
                    Text("No tienes listas creadas aún", color = Color.Gray, fontSize = 13.sp)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(playlists) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectPlaylist(pl)
                                        onDismiss()
                                    },
                                color = Color(0xFF22222E)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = Color.LightGray)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(pl.name, color = Color.White, fontWeight = FontWeight.Medium)
                                        Text("${pl.songs.size} canciones", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun DesktopRemoteModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AndroidPreferences(context) }
    var hostIp by remember { mutableStateOf(preferences.getDesktopRemoteAddress()) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Control remoto de PC/Mac", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Activa Control remoto en los ajustes de MaviMusic de tu PC o Mac. Escribe la dirección que muestra el equipo; se abrirá en el navegador, donde podrás introducir su PIN.",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it; statusMessage = null },
                    label = { Text("Dirección del equipo", color = Color.Gray) },
                    placeholder = { Text("192.168.1.20:8765") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Text(statusMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val url = runCatching { DesktopRemoteAddress.normalize(hostIp) }.getOrNull()
                    if (url == null) {
                        statusMessage = "Revisa la dirección del equipo y su puerto."
                    } else {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            preferences.setDesktopRemoteAddress(url)
                            onDismiss()
                        } catch (_: ActivityNotFoundException) {
                            statusMessage = "Necesitas un navegador para abrir el control remoto."
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Abrir control remoto", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun SettingsModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AndroidPreferences(context) }
    var dataSaver by remember { mutableStateOf(preferences.getDataSaver()) }
    val downloads by com.music.mavimusic.android.data.OfflineDownloadManager.downloadedSongs.collectAsState()
    val activeDownloads by com.music.mavimusic.android.data.OfflineDownloadManager.downloadingIds.collectAsState()
    val downloadsSize = remember(downloads, activeDownloads) { com.music.mavimusic.android.data.OfflineDownloadManager.getDownloadsSizeBytes() }
    val downloadsMb = remember(downloadsSize) { downloadsSize / (1024 * 1024) }
    val installedVersion = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ajustes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("Calidad de reproducción", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(false to "Automática", true to "Ahorro de datos").forEach { (saveData, label) ->
                            androidx.compose.material3.FilterChip(
                                selected = dataSaver == saveData,
                                onClick = {
                                    dataSaver = saveData
                                    preferences.setDataSaver(saveData)
                                    com.music.mavimusic.data.innertube.StreamResolver.invalidateResolvedStreams()
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = Color(0xFF22222E),
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }

                item {
                    Text("La calidad depende de la fuente. Ahorro de datos prioriza audio de hasta 128 kbps cuando está disponible. Se aplica a las próximas reproducciones.",
                        color = Color.Gray, fontSize = 12.sp)
                }

                item {
                    Text("Almacenamiento y Caché", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Descargas offline", color = Color.White, fontSize = 14.sp)
                                Text("$downloadsMb MB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    com.music.mavimusic.android.data.OfflineDownloadManager.clearAllDownloads()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Limpiar todas las descargas", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                item {
                    Text("Actualizaciones", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Versión instalada", color = Color.White, fontSize = 14.sp)
                                Text(installedVersion, color = Color.Gray, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Canal de pruebas · actualización manual mediante APK", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Text("Acerca de", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("MaviMusic Android KMP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Reproductor nativo multiplataforma desarrollado con Kotlin Multiplatform y Compose Multiplatform.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun TrackActionsModal(
    song: Song,
    onStartRadio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAdjustLyricsOffset: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloaded = remember(song.videoId) {
        com.music.mavimusic.android.data.OfflineDownloadManager.isDownloaded(song.videoId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
                Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onAdjustLyricsOffset()
                        onDismiss()
                    },
                    color = Color(0xFF22222E)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ajustar sincronización de letra", color = Color.White, fontSize = 14.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onStartRadio()
                        onDismiss()
                    },
                    color = Color(0xFF22222E)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Radio, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Iniciar Radio a partir de esta canción", color = Color.White, fontSize = 14.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onAddToPlaylist()
                        onDismiss()
                    },
                    color = Color(0xFF22222E)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Añadir a playlist", color = Color.White, fontSize = 14.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        if (isDownloaded) {
                            com.music.mavimusic.android.data.OfflineDownloadManager.deleteDownload(song.videoId)
                        } else {
                            com.music.mavimusic.android.data.OfflineDownloadManager.downloadSong(song)
                        }
                        onDismiss()
                    },
                    color = Color(0xFF22222E)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDownloaded) Icons.Rounded.Delete else Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (isDownloaded) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(if (isDownloaded) "Eliminar descarga offline" else "Descargar pista offline", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun LyricsOffsetModal(
    currentOffsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var offset by remember { mutableStateOf(currentOffsetMs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Desfase de la letra", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${if (offset > 0) "+" else ""}${offset} ms (${offset / 1000f}s)",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { offset -= 500; onOffsetChange(offset) }) {
                        Text("-0.5s", color = Color.White)
                    }
                    Button(onClick = { offset = 0; onOffsetChange(0) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22222E))) {
                        Text("Reset", color = MaterialTheme.colorScheme.onPrimary)
                    }
                    OutlinedButton(onClick = { offset += 500; onOffsetChange(offset) }) {
                        Text("+0.5s", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Listo", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp)
    )
}
