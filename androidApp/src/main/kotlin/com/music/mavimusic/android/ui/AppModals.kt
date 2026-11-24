package com.music.mavimusic.android.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.audio.SleepTimer
import com.music.mavimusic.android.data.AndroidPreferences
import com.music.mavimusic.android.data.AndroidYouTubeAuth
import com.music.mavimusic.android.data.DesktopRemoteAddress
import com.music.mavimusic.android.data.DesktopRemoteClient
import com.music.mavimusic.android.data.DesktopRemoteConnection
import com.music.mavimusic.android.data.OfflineDownloadManager
import com.music.mavimusic.android.data.UserPlaylist
import com.music.mavimusic.data.innertube.StreamResolver
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.launch

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
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (minutesLeft != null) {
                    Text(
                        text = if (endOfSong) "Se apagará al terminar la canción actual" else "Se apagará en aprox. $minutesLeft min",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    OutlinedButton(
                        onClick = { SleepTimer.cancel(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                        color = Color(0xFF22222E),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
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
                    color = Color(0xFF22222E),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
        shape = RoundedCornerShape(16.dp),
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
                shape = RoundedCornerShape(12.dp),
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
        shape = RoundedCornerShape(16.dp),
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
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
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
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(playlists) { pl ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelectPlaylist(pl)
                                        onDismiss()
                                    },
                                color = Color(0xFF22222E),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
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
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun DesktopRemoteModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val preferences = remember { AndroidPreferences(context) }
    val scope = rememberCoroutineScope()
    val client = remember { DesktopRemoteClient() }
    val connection by client.connection.collectAsState()
    val remoteState by client.playbackState.collectAsState()

    var hostIp by remember { mutableStateOf(preferences.getDesktopRemoteAddress()) }
    var pin by remember { mutableStateOf("") }
    var savedToken by remember { mutableStateOf(preferences.getDesktopRemoteToken()) }
    var localError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { client.disconnect() }
    }

    val isConnected = connection is DesktopRemoteConnection.Connected
    val isConnecting = connection is DesktopRemoteConnection.Connecting
    val connectionError = (connection as? DesktopRemoteConnection.Error)?.message ?: localError

    AlertDialog(
        onDismissRequest = {
            client.disconnect()
            onDismiss()
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Control remoto de PC/Mac", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (isConnected) {
                    val state = remoteState
                    val track = state?.song
                    Surface(
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF15151E)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!track?.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = track?.artworkUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(52.dp),
                                        )
                                    } else {
                                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = track?.title?.ifBlank { "Sin reproducción activa" } ?: "Conectado al equipo",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = track?.artist?.ifBlank { "MaviMusic Desktop" } ?: "Esperando estado…",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { client.sendCommand("TOGGLE_LIKE") }) {
                                    Icon(
                                        imageVector = if (state?.isLiked == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Me gusta",
                                        tint = if (state?.isLiked == true) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    )
                                }
                            }

                            if (state != null && state.durationMs > 0L) {
                                val fraction = (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                                Slider(
                                    value = fraction,
                                    onValueChange = { f ->
                                        client.sendCommand("SEEK", mapOf("positionMs" to (f * state.durationMs).toLong()))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = { client.sendCommand("TOGGLE_SHUFFLE") }) {
                                    Icon(
                                        Icons.Rounded.Shuffle,
                                        contentDescription = "Aleatorio",
                                        tint = if (state?.shuffle == true) MaterialTheme.colorScheme.primary else Color.Gray,
                                    )
                                }
                                IconButton(onClick = { client.sendCommand("PREVIOUS") }) {
                                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                                }
                                FilledIconButton(
                                    onClick = { client.sendCommand("PLAY_PAUSE") },
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                    ),
                                ) {
                                    Icon(
                                        if (state?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (state?.isPlaying == true) "Pausar" else "Reproducir",
                                    )
                                }
                                IconButton(onClick = { client.sendCommand("NEXT") }) {
                                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                                }
                                IconButton(onClick = { client.sendCommand("CYCLE_REPEAT") }) {
                                    val repeatIcon = if (state?.repeatMode == "ONE") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat
                                    Icon(
                                        repeatIcon,
                                        contentDescription = "Repetir",
                                        tint = if (state?.repeatMode != null && state.repeatMode != "OFF") MaterialTheme.colorScheme.primary else Color.Gray,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = state?.volume ?: 1f,
                                    onValueChange = { v ->
                                        client.sendCommand("SET_VOLUME", mapOf("volume" to v.coerceIn(0f, 1f)))
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            if (!state?.outputDevices.isNullOrEmpty()) {
                                Text("Salida de audio", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state!!.outputDevices.forEach { dev ->
                                        FilterChip(
                                            selected = dev.isSelected || dev.id == state.currentOutputId,
                                            onClick = {
                                                client.sendCommand("SET_OUTPUT_DEVICE", mapOf("deviceId" to dev.id))
                                            },
                                            label = { Text(dev.name, fontSize = 11.sp, maxLines = 1) },
                                            colors = mobileChipColors(),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { client.disconnect() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("Desconectar equipo", color = Color.White, fontSize = 12.sp)
                    }
                } else {
                    Text(
                        "Activa Control remoto en los ajustes de MaviMusic de tu PC o Mac e introduce la dirección y el PIN de 4 dígitos para controlarlo directamente desde aquí.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                    )

                    OutlinedTextField(
                        value = hostIp,
                        onValueChange = { hostIp = it; localError = null },
                        label = { Text("Dirección del equipo", color = Color.Gray) },
                        placeholder = { Text("192.168.1.20:8765") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it.filter { ch -> ch.isDigit() }.take(6); localError = null },
                        label = { Text("PIN de vinculación", color = Color.Gray) },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (isConnecting) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Conectando con el equipo…", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }

                    if (connectionError != null) {
                        Text(connectionError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    if (!savedToken.isNullOrBlank() && hostIp.isNotBlank() && !isConnecting) {
                        OutlinedButton(
                            onClick = {
                                localError = null
                                client.connect(hostIp, savedToken!!)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Reconectar con sesión guardada", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    TextButton(
                        onClick = {
                            val url = runCatching { DesktopRemoteAddress.normalize(hostIp) }.getOrNull()
                            if (url == null) {
                                localError = "Revisa la dirección del equipo y su puerto."
                            } else {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    preferences.setDesktopRemoteAddress(url)
                                } catch (_: ActivityNotFoundException) {
                                    localError = "Necesitas un navegador para abrir el control remoto."
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Abrir en el navegador", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (isConnected) {
                TextButton(onClick = { client.disconnect(); onDismiss() }) {
                    Text("Cerrar", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(
                    enabled = !isConnecting,
                    onClick = {
                        localError = null
                        val normalized = runCatching { DesktopRemoteAddress.normalize(hostIp) }.getOrNull()
                        if (normalized == null) {
                            localError = "Revisa la dirección del equipo y su puerto."
                            return@Button
                        }
                        preferences.setDesktopRemoteAddress(normalized)
                        if (pin.isBlank() && !savedToken.isNullOrBlank()) {
                            client.connect(normalized, savedToken!!)
                            return@Button
                        }
                        val deviceLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "MaviMusic Android" }
                        scope.launch {
                            client.pair(normalized, pin, deviceLabel).onSuccess { (url, token) ->
                                preferences.setDesktopRemoteAddress(url)
                                preferences.setDesktopRemoteToken(token)
                                savedToken = token
                                pin = ""
                            }.onFailure { err ->
                                localError = err.message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text("Conectar", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        },
        dismissButton = {
            if (!isConnected) {
                TextButton(onClick = { client.disconnect(); onDismiss() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun SettingsModal(
    onOpenSignIn: () -> Unit = {},
    onOpenDesktopRemote: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { AndroidPreferences(context) }
    var dataSaver by remember { mutableStateOf(preferences.getDataSaver()) }
    val volumeNormalization by AndroidPlayerEngine.volumeNormalization.collectAsState()
    val authState by AndroidYouTubeAuth.state.collectAsState()
    val downloads by OfflineDownloadManager.downloadedSongs.collectAsState()
    val activeDownloads by OfflineDownloadManager.downloadingIds.collectAsState()
    val downloadsSize = remember(downloads, activeDownloads) { OfflineDownloadManager.getDownloadsSizeBytes() }
    val downloadsMb = remember(downloadsSize) { downloadsSize / (1024 * 1024) }
    val installedVersion = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "—"
    }

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
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Cuenta de YouTube Music
                item {
                    Text("Cuenta de YouTube Music", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    tint = if (authState.signedIn) MaterialTheme.colorScheme.primary else Color.Gray,
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = if (authState.signedIn) authState.accountName ?: "Sesión activa" else "Modo invitado",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = if (authState.signedIn) "Inicio personalizado y recomendaciones activas"
                                        else "Inicia sesión para cargar tu historial y feed de YouTube Music",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                    )
                                }
                            }
                            if (authState.signedIn) {
                                OutlinedButton(
                                    onClick = { AndroidYouTubeAuth.signOut(context) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Cerrar sesión", color = Color.White, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        onDismiss()
                                        onOpenSignIn()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                ) {
                                    Text("Iniciar sesión en YouTube Music", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Calidad de reproducción
                item {
                    Text("Calidad y sonido", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(false to "Automática", true to "Ahorro de datos").forEach { (saveData, label) ->
                            FilterChip(
                                selected = dataSaver == saveData,
                                onClick = {
                                    dataSaver = saveData
                                    preferences.setDataSaver(saveData)
                                    StreamResolver.invalidateResolvedStreams()
                                },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = Color(0xFF22222E),
                                    labelColor = Color.LightGray,
                                ),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Normalización de volumen", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("Equilibra el nivel de audio alrededor de -14 LUFS", color = Color.Gray, fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = volumeNormalization,
                                onCheckedChange = { AndroidPlayerEngine.setVolumeNormalization(it) },
                            )
                        }
                    }
                }

                item {
                    Text(
                        "La calidad depende de la fuente. Ahorro de datos prioriza audio de hasta 128 kbps cuando está disponible. Se aplica a las próximas reproducciones.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                    )
                }

                // Herramientas rápidas (accesibles también desde teléfonos sin barra lateral)
                item {
                    Text("Herramientas", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onOpenDesktopRemote()
                                },
                            color = Color(0xFF22222E),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Computer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Control remoto de PC/Mac", color = Color.White, fontSize = 14.sp)
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onDismiss()
                                    onOpenSleepTimer()
                                },
                            color = Color(0xFF22222E),
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("Temporizador de apagado", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // Almacenamiento y Caché
                item {
                    Text("Almacenamiento y Caché", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Descargas offline", color = Color.White, fontSize = 14.sp)
                                Text("$downloadsMb MB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { OfflineDownloadManager.clearAllDownloads() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Limpiar todas las descargas", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Actualizaciones
                item {
                    Text("Actualizaciones", color = Color.LightGray, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF22222E),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
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
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
fun TrackActionsModal(
    song: Song,
    onPlayNext: () -> Unit = { AndroidPlayerEngine.playNext(song) },
    onAddToQueue: () -> Unit = { AndroidPlayerEngine.addToQueue(song) },
    onStartRadio: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAdjustLyricsOffset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isDownloaded = remember(song.videoId) {
        OfflineDownloadManager.isDownloaded(song.videoId)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Color.Gray, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onPlayNext()
                        onDismiss()
                    },
                    color = Color(0xFF22222E),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reproducir siguiente", color = Color.White, fontSize = 14.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onAddToQueue()
                        onDismiss()
                    },
                    color = Color(0xFF22222E),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Añadir a la cola", color = Color.White, fontSize = 14.sp)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable {
                        onAdjustLyricsOffset()
                        onDismiss()
                    },
                    color = Color(0xFF22222E),
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
                    color = Color(0xFF22222E),
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
                    color = Color(0xFF22222E),
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
                            OfflineDownloadManager.deleteDownload(song.videoId)
                        } else {
                            OfflineDownloadManager.downloadSong(song)
                        }
                        onDismiss()
                    },
                    color = Color(0xFF22222E),
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isDownloaded) Icons.Rounded.Delete else Icons.Rounded.Download,
                            contentDescription = null,
                            tint = if (isDownloaded) Color.Gray else MaterialTheme.colorScheme.primary,
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
        shape = RoundedCornerShape(16.dp),
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
                    fontSize = 24.sp,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { offset -= 500; onOffsetChange(offset) }) {
                        Text("-0.5s", color = Color.White)
                    }
                    Button(
                        onClick = { offset = 0; onOffsetChange(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22222E)),
                    ) {
                        Text("Reset", color = Color.White)
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Listo", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        containerColor = Color(0xFF17171F),
        shape = RoundedCornerShape(16.dp),
    )
}
