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
                Text("Crear", color = Color.White)
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
fun DesktopSyncModal(
    onDismiss: () -> Unit
) {
    var hostIp by remember { mutableStateOf("192.168.1.") }
    var pin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sincronizar con PC/Mac", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Activa el servidor remoto en MaviMusic Desktop (Ajustes -> Control Remoto) e introduce los datos de conexión:",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = hostIp,
                    onValueChange = { hostIp = it },
                    label = { Text("IP de tu ordenador", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN de enlace (4 dígitos)", color = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Text(statusMessage!!, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    statusMessage = "Conectando a $hostIp:8080..."
                    // Connection handshake
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Conectar", color = Color.White)
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
