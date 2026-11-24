package com.music.mavimusic.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
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
import com.music.mavimusic.android.data.AndroidYouTubeAuth
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.BrowseItem
import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.HomeFeed
import com.music.mavimusic.data.model.HomeShelf
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.ShelfItem
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MobileHomeState(
    private val scope: CoroutineScope,
    private val recentSearches: () -> List<String>,
    private val search: suspend (String) -> Result<List<Song>> = { term ->
        YtMusicRepository.search(term, SearchFilter.SONGS).map { rows ->
            rows.mapNotNull {
                when (it) {
                    is SearchResult.Track -> it.song
                    is SearchResult.TopTrack -> it.song
                    else -> null
                }
            }
        }
    },
    private val fetchHomeFeed: (suspend () -> Result<HomeFeed>)? = null,
    private val fetchRecentlyPlayed: (suspend () -> Result<HomeShelf?>)? = null,
    private val localHistory: () -> List<Song> = { emptyList() },
    private val isSignedIn: () -> Boolean = { false },
) {
    val scroll = androidx.compose.foundation.lazy.LazyListState()
    var songs by mutableStateOf<List<Song>>(emptyList())
        private set
    var recentTracks by mutableStateOf<List<Song>>(emptyList())
        private set
    var shelves by mutableStateOf<List<HomeShelf>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set

    private var lastTerms: List<String>? = null
    private var lastSignedIn: Boolean? = null
    private var request: Job? = null
    private var generation = 0

    fun load(refresh: Boolean = false) {
        val terms = recentSearches().map(String::trim).filter(String::isNotBlank).distinct().take(2)
        val signedInNow = isSignedIn()
        val hasFeedSources = fetchHomeFeed != null || fetchRecentlyPlayed != null
        val localRecent = localHistory().distinctBy { it.videoId }.take(12)
        if (localRecent.isNotEmpty() && recentTracks.isEmpty()) {
            recentTracks = localRecent
        }

        if (lastTerms == terms && lastSignedIn == signedInNow && (!refresh || loading)) return
        request?.cancel()
        val token = ++generation
        if (lastTerms != terms) songs = emptyList()
        lastTerms = terms
        lastSignedIn = signedInNow

        if (terms.isEmpty() && !hasFeedSources) {
            songs = emptyList()
            recentTracks = localRecent
            shelves = emptyList()
            failed = false
            loading = false
            return
        }

        loading = true
        request = scope.launch {
            try {
                val searchDeferred = if (terms.isNotEmpty()) {
                    async { terms.map { search(it) } }
                } else null

                val homeDeferred = fetchHomeFeed?.let { fetcher ->
                    async { runCatching { fetcher() }.getOrElse { Result.failure(it) } }
                }
                val recentDeferred = fetchRecentlyPlayed?.let { fetcher ->
                    async { runCatching { fetcher() }.getOrElse { Result.failure(it) } }
                }

                val searchResponses = searchDeferred?.await().orEmpty()
                val homeResult = homeDeferred?.await()
                val recentResult = recentDeferred?.await()

                if (token == generation) {
                    val mergedSearchSongs = searchResponses
                        .flatMap { it.getOrDefault(emptyList()).take(12) }
                        .distinctBy { it.videoId }
                    songs = mergedSearchSongs

                    val remoteRecents = recentResult?.getOrNull()?.items.orEmpty().mapNotNull { item ->
                        item.toSongOrNull()
                    }
                    recentTracks = (localHistory() + remoteRecents)
                        .distinctBy { it.videoId }
                        .take(16)

                    val rawShelves = homeResult?.getOrNull()?.shelves.orEmpty()
                        .filter { shelf ->
                            shelf.items.isNotEmpty() &&
                                !shelf.title.equals("Recents", ignoreCase = true) &&
                                !shelf.title.equals("Recientes", ignoreCase = true)
                        }
                    shelves = rawShelves

                    val searchFailed = searchResponses.isNotEmpty() && searchResponses.all { it.isFailure }
                    val homeFailed = homeResult?.isFailure == true
                    failed = songs.isEmpty() && recentTracks.isEmpty() && shelves.isEmpty() && (searchFailed || homeFailed)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (token == generation) failed = songs.isEmpty() && recentTracks.isEmpty() && shelves.isEmpty()
            } finally {
                if (token == generation) loading = false
            }
        }
    }

    fun syncLocalHistory(history: List<Song>) {
        if (history.isEmpty()) return
        recentTracks = (history + recentTracks).distinctBy { it.videoId }.take(16)
    }
}

private fun ShelfItem.toSongOrNull(): Song? {
    val vid = videoId?.takeIf { it.isNotBlank() } ?: return null
    return Song(
        videoId = vid,
        title = title,
        artist = subtitle,
        thumbnailUrl = thumbnailUrl,
    )
}

private fun ShelfItem.toBrowseItemOrNull(): BrowseItem? {
    val bid = browseId?.takeIf { it.isNotBlank() } ?: return null
    val type = when {
        bid.startsWith("MPREb_") || bid.startsWith("OLAK5uy_") -> BrowseType.ALBUM
        bid.startsWith("UC") -> BrowseType.ARTIST
        else -> BrowseType.PLAYLIST
    }
    return BrowseItem(
        browseId = bid,
        title = title,
        subtitle = subtitle,
        thumbnailUrl = thumbnailUrl,
        type = type,
    )
}

@Composable
fun MobileHomeView(
    state: MobileHomeState,
    onSearch: () -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit,
    onOpenCollection: (BrowseItem) -> Unit = {},
    onOpenSignIn: (() -> Unit)? = null,
) {
    val song by AndroidPlayerEngine.currentSong.collectAsState()
    val playing by AndroidPlayerEngine.isPlaying.collectAsState()
    val playHistory by AndroidPlayerEngine.playHistory.collectAsState()
    val authState by AndroidYouTubeAuth.state.collectAsState()

    LaunchedEffect(authState.signedIn) { state.load(refresh = true) }
    LaunchedEffect(playHistory) { state.syncLocalHistory(playHistory) }

    val hasAnyContent = state.recentTracks.isNotEmpty() || state.songs.isNotEmpty() || state.shelves.isNotEmpty()
    val radioSeeds = remember(state.recentTracks, state.songs) {
        (state.recentTracks + state.songs).distinctBy { it.videoId }.take(6)
    }

    LazyColumn(
        state = state.scroll,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "ESCUCHA A TU MANERA",
                        fontSize = 10.sp,
                        letterSpacing = 1.6.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Inicio", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(
                    onClick = { state.load(refresh = true) },
                    enabled = !state.loading,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Rounded.Refresh,
                        "Actualizar recomendaciones",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onSearch, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.Search, "Buscar música", tint = Color.White)
                }
            }
        }

        // Optional compact YouTube Music sign-in banner when browsing as guest
        if (!authState.signedIn && onOpenSignIn != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF1A2229),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Conecta tu cuenta de YouTube Music",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "Sincroniza tu historial reciente y recomendaciones reales",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        FilledTonalButton(
                            onClick = onOpenSignIn,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text("Entrar", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 1. Recientes pinned at the top (local + YouTube history)
        if (state.recentTracks.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Recientes", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(state.recentTracks, key = { _, track -> "recent_${track.videoId}" }) { index, track ->
                            Column(
                                Modifier
                                    .width(120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { AndroidPlayerEngine.playQueue(state.recentTracks, index) }
                            ) {
                                AsyncImage(
                                    track.thumbnailUrl,
                                    null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                                Text(
                                    track.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                                Text(
                                    track.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }

        // The mini player already offers resume; avoid duplicating it above recommendations.
        song?.takeIf { !hasAnyContent }?.let { current ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Continúa escuchando", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF103238)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                current.thumbnailUrl,
                                null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = .05f)),
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    current.title,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    current.artist,
                                    color = Color(0xFF9AB3B7),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            FilledIconButton(
                                onClick = { AndroidPlayerEngine.togglePlayPause() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color.White,
                                    contentColor = Color.Black,
                                ),
                            ) {
                                Icon(
                                    if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    if (playing) "Pausar" else "Continuar",
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.loading && !hasAnyContent) {
            item {
                Row(Modifier.padding(vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Cargando tu inicio musical…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else if (!hasAnyContent) {
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            if (state.failed) "No pudimos cargar las recomendaciones" else "Encuentra tu próxima canción",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Busca una canción o inicia sesión con YouTube Music para personalizar tu feed.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                        Button(onClick = onSearch) { Text("Explorar música") }
                    }
                }
            }
        }

        // 2. Quick radio starter from recent tracks / searches
        if (radioSeeds.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Iniciar radio", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(radioSeeds, key = { _, track -> "radio_${track.videoId}" }) { _, track ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1C232A),
                                modifier = Modifier
                                    .width(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onStartRadio(track) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AsyncImage(
                                        track.thumbnailUrl,
                                        null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            track.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            track.artist,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Icon(
                                        Icons.Rounded.Radio,
                                        contentDescription = "Iniciar radio",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Featured search-based recommendation card
        state.songs.firstOrNull()?.let { featured ->
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { AndroidPlayerEngine.playQueue(state.songs) }
                ) {
                    AsyncImage(
                        featured.thumbnailUrl,
                        null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.align(Alignment.CenterEnd).width(180.dp).fillMaxHeight(),
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1C1C1E), Color(0xF21C1C1E), Color.Transparent)
                                )
                            )
                    )
                    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.Center) {
                        Text(
                            "SEGÚN TUS BÚSQUEDAS",
                            color = Color(0xFFFFB66A),
                            letterSpacing = 1.sp,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            featured.title,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(.72f).padding(vertical = 5.dp),
                        )
                        Text(
                            featured.artist,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth(.72f),
                        )
                    }
                }
            }
        }

        // 4. Real YouTube Music Home shelves (personalized when signed in, curated when guest)
        itemsIndexed(state.shelves.take(6), key = { idx, shelf -> "shelf_${idx}_${shelf.title}" }) { _, shelf ->
            val shelfSongs = remember(shelf) { shelf.items.mapNotNull { it.toSongOrNull() } }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    if (shelf.subtitle.isNotBlank()) {
                        Text(
                            shelf.subtitle.uppercase(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            letterSpacing = 1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        shelf.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(
                        shelf.items.take(12),
                        key = { itemIdx, item -> "${shelf.title}_${itemIdx}_${item.videoId ?: item.browseId ?: item.title}" }
                    ) { _, item ->
                        Column(
                            Modifier
                                .width(132.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    val track = item.toSongOrNull()
                                    if (track != null) {
                                        val startIdx = shelfSongs.indexOfFirst { it.videoId == track.videoId }.coerceAtLeast(0)
                                        if (shelfSongs.isNotEmpty()) {
                                            AndroidPlayerEngine.playQueue(shelfSongs, startIdx)
                                        } else {
                                            AndroidPlayerEngine.playSong(track)
                                        }
                                    } else {
                                        item.toBrowseItemOrNull()?.let(onOpenCollection)
                                    }
                                }
                        ) {
                            AsyncImage(
                                item.thumbnailUrl,
                                null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(132.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            )
                            Text(
                                item.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Text(
                                item.subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // 5. Search-based horizontal row + list
        if (state.songs.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("De tus búsquedas", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        itemsIndexed(state.songs.drop(1).take(8), key = { _, track -> "search_${track.videoId}" }) { index, track ->
                            Column(
                                Modifier
                                    .width(116.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { AndroidPlayerEngine.playQueue(state.songs, index + 1) }
                            ) {
                                AsyncImage(
                                    track.thumbnailUrl,
                                    null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(116.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                                Text(
                                    track.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Text(
                                    track.artist,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            item { Text("Sigue escuchando", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            itemsIndexed(state.songs.drop(9), key = { _, track -> "more_${track.videoId}" }) { index, track ->
                SongRowItem(
                    track,
                    isCurrent = song?.videoId == track.videoId,
                    onClick = { AndroidPlayerEngine.playQueue(state.songs, index + 9) },
                    onAddToPlaylist = { onAddToPlaylist(track) },
                    onStartRadio = { onStartRadio(track) },
                )
            }
        }
    }
}
