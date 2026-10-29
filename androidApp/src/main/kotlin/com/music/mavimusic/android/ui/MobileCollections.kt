package com.music.mavimusic.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.SongRowItem
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.BrowseItem
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.launch

@Composable
fun CollectionRow(item: BrowseItem, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onOpen).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(item.thumbnailUrl, null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionSheet(item: BrowseItem, onDismiss: () -> Unit, onAdd: (Song) -> Unit, onRadio: (Song) -> Unit) {
    var songs by remember(item.browseId) { mutableStateOf<List<Song>>(emptyList()) }
    var continuation by remember(item.browseId) { mutableStateOf<String?>(null) }
    var loading by remember(item.browseId) { mutableStateOf(true) }
    var error by remember(item.browseId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun load(more: Boolean = false) {
        scope.launch {
            loading = true
            error = false
            try {
                val response = if (more && continuation != null) YtMusicRepository.moreSongs(continuation!!)
                    else YtMusicRepository.browseSongs(item.browseId)
                response.onSuccess {
                    songs = ((if (more) songs else emptyList()) + it.songs).distinctBy { track -> track.videoId }
                    continuation = it.continuation
                }.onFailure { error = true }
            } finally { loading = false }
        }
    }
    LaunchedEffect(item.browseId) { load() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(horizontal = 20.dp)) {
            Text(item.title, style = MaterialTheme.typography.headlineSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(12.dp))
            if (songs.isNotEmpty()) Button(onClick = { AndroidPlayerEngine.playQueue(songs); onDismiss() }) { Text("Reproducir") }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp)) {
                itemsIndexed(songs, key = { _, song -> song.videoId }) { index, song ->
                    SongRowItem(song, onClick = { AndroidPlayerEngine.playQueue(songs, index); onDismiss() },
                        onAddToPlaylist = { onAdd(song) }, onStartRadio = { onRadio(song) })
                }
                item {
                    when {
                        loading -> CircularProgressIndicator(Modifier.padding(16.dp))
                        error -> TextButton(onClick = { load(songs.isNotEmpty()) }) { Text("No se pudo cargar. Reintentar") }
                        continuation != null -> TextButton(onClick = { load(more = true) }) { Text("Cargar más canciones") }
                        songs.isEmpty() -> Text("No hay canciones disponibles", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
