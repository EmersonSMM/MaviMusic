package com.music.mavimusic.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.SongRowItem
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MobileHomeState(
    private val scope: CoroutineScope,
    private val recentSearches: () -> List<String>,
    private val search: suspend (String) -> Result<List<Song>> = { term ->
        YtMusicRepository.search(term, SearchFilter.SONGS).map { rows -> rows.mapNotNull {
            when (it) { is SearchResult.Track -> it.song; is SearchResult.TopTrack -> it.song; else -> null }
        } }
    },
) {
    val scroll = androidx.compose.foundation.lazy.LazyListState()
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    private var lastTerms: List<String>? = null
    private var request: Job? = null
    private var generation = 0

    fun load(refresh: Boolean = false) {
        val terms = recentSearches().map(String::trim).filter(String::isNotBlank).distinct().take(2)
        if (lastTerms == terms && (!refresh || loading)) return
        request?.cancel()
        val token = ++generation
        if (lastTerms != terms) songs = emptyList()
        lastTerms = terms
        if (terms.isEmpty()) { songs = emptyList(); failed = false; loading = false; return }
        loading = true
        request = scope.launch {
            try {
                val responses = terms.map { search(it) }
                if (token == generation) {
                    songs = responses.flatMap { it.getOrDefault(emptyList()).take(12) }.distinctBy { it.videoId }
                    failed = responses.all { it.isFailure }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (token == generation) failed = true }
            finally { if (token == generation) loading = false }
        }
    }
}

@Composable
fun MobileHomeView(
    state: MobileHomeState,
    onSearch: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit,
) {
    val song by AndroidPlayerEngine.currentSong.collectAsState()
    val playing by AndroidPlayerEngine.isPlaying.collectAsState()
    LaunchedEffect(Unit) { state.load() }
    LazyColumn(
        state = state.scroll,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ESCUCHA A TU MANERA", fontSize = 10.sp, letterSpacing = 1.6.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Inicio", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = { state.load(refresh = true) }, enabled = !state.loading) {
                    Icon(Icons.Rounded.Refresh, "Actualizar recomendaciones", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, "Buscar música", tint = Color.White) }
            }
        }
        // The mini player already offers resume; avoid duplicating it above recommendations.
        song?.takeIf { state.songs.isEmpty() }?.let { current ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Continúa escuchando", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF103238)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(current.thumbnailUrl, null, contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .05f)))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(current.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                Text(current.artist, color = Color(0xFF9AB3B7), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            FilledIconButton(onClick = { AndroidPlayerEngine.togglePlayPause() },
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pausar" else "Continuar")
                            }
                        }
                    }
                }
            }
        }
        if (state.loading && state.songs.isEmpty()) {
            item {
                Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Buscando música para ti…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (state.songs.isEmpty()) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (state.failed) "No pudimos cargar las recomendaciones" else "Encuentra tu próxima canción",
                            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("Busca una canción o un artista para empezar a escuchar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onSearch) { Text("Explorar música") }
                    }
                }
            }
        }
        state.songs.firstOrNull()?.let { featured ->
            item {
                Box(Modifier.fillMaxWidth().height(144.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { AndroidPlayerEngine.playQueue(state.songs) }) {
                    AsyncImage(featured.thumbnailUrl, null, contentScale = ContentScale.Crop,
                        modifier = Modifier.align(Alignment.CenterEnd).width(200.dp).fillMaxHeight())
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF1C1C1E), Color(0xF21C1C1E), Color.Transparent))))
                    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                        Text("SEGÚN TUS BÚSQUEDAS", color = Color(0xFFFFB66A), letterSpacing = 1.sp, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(featured.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(.72f).padding(vertical = 5.dp))
                        Text(featured.artist, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(.72f))
                    }
                }
            }
        }
        if (state.songs.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("De tus búsquedas", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        itemsIndexed(state.songs.drop(1).take(8), key = { _, track -> track.videoId }) { index, track ->
                            Column(Modifier.width(116.dp).clip(RoundedCornerShape(10.dp)).clickable { AndroidPlayerEngine.playQueue(state.songs, index + 1) }) {
                                AsyncImage(track.thumbnailUrl, null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(116.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                Text(track.title, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                                Text(track.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item { Text("Sigue escuchando", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            itemsIndexed(state.songs.drop(9), key = { _, track -> track.videoId }) { index, track ->
                SongRowItem(track, isCurrent = song?.videoId == track.videoId,
                    onClick = { AndroidPlayerEngine.playQueue(state.songs, index + 9) },
                    onAddToPlaylist = { onAddToPlaylist(track) }, onStartRadio = { onStartRadio(track) })
            }
        }
    }
}
