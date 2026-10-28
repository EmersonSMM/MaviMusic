package com.music.mavimusic.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.host.AndroidAppHost
import com.music.mavimusic.android.host.AndroidPlayerHost
import com.music.mavimusic.android.service.PlaybackService
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.LikeStatus
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.ui.AppUi
import com.music.mavimusic.ui.player.NowPlayingScreen
import com.music.mavimusic.ui.player.PlayerPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MobileTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Rounded.Home),
    EXPLORE("Explorar", Icons.Rounded.Explore),
    SEARCH("Buscar", Icons.Rounded.Search),
    LIBRARY("Biblioteca", Icons.Rounded.LibraryMusic)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start native background audio playback service
        val serviceIntent = Intent(this, PlaybackService::class.java)
        startService(serviceIntent)

        // Install shared UI & Player hosts
        PlayerPlatform.install(AndroidPlayerHost(applicationContext))
        AppUi.install(AndroidAppHost())

        setContent {
            val darkTheme = darkColorScheme(
                primary = Color(0xFFFF3366),
                background = Color(0xFF0F0F13),
                surface = Color(0xFF17171F),
                surfaceVariant = Color(0xFF22222E),
                onBackground = Color.White,
                onSurface = Color.White
            )

            MaterialTheme(colorScheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndroidMusicApp()
                }
            }
        }
    }
}

@Composable
fun AndroidMusicApp() {
    var selectedTab by remember { mutableStateOf(MobileTab.HOME) }
    var showFullPlayer by remember { mutableStateOf(false) }

    val currentSong by AndroidPlayerEngine.currentSong.collectAsState()
    val isPlaying by AndroidPlayerEngine.isPlaying.collectAsState()
    val isLoading by AndroidPlayerEngine.isLoading.collectAsState()
    val durationMs by AndroidPlayerEngine.durationMs.collectAsState()
    val queue by AndroidPlayerEngine.queue.collectAsState()
    val queueIndex by AndroidPlayerEngine.queueIndex.collectAsState()
    val repeatMode by AndroidPlayerEngine.repeatMode.collectAsState()
    val shuffleEnabled by AndroidPlayerEngine.shuffleEnabled.collectAsState()

    BackHandler(enabled = showFullPlayer) {
        showFullPlayer = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val windowW = maxWidth
        val windowH = maxHeight

        if (showFullPlayer && currentSong != null) {
            val song = currentSong!!
            NowPlayingScreen(
                song = song,
                isPlaying = isPlaying,
                isLoading = isLoading,
                position = AndroidPlayerEngine.position,
                durationMs = durationMs,
                audioVersionSwitching = false,
                qualityUpgraded = false,
                queue = queue,
                queueIndex = queueIndex,
                hasPrevious = queueIndex > 0,
                hasNext = queueIndex + 1 < queue.size,
                repeatMode = repeatMode,
                shuffleEnabled = shuffleEnabled,
                autoplayEnabled = true,
                signedIn = false,
                accountName = null,
                likeStatus = LikeStatus.INDIFFERENT,
                onToggleLike = {},
                onPlayPause = { AndroidPlayerEngine.togglePlayPause() },
                onNext = { AndroidPlayerEngine.skipNext() },
                onPrevious = { AndroidPlayerEngine.skipPrevious() },
                onBlockedControl = {},
                onSeek = { AndroidPlayerEngine.seekTo(it) },
                onSeekFraction = { AndroidPlayerEngine.seekFraction(it) },
                onToggleShuffle = { AndroidPlayerEngine.toggleShuffle() },
                onCycleRepeat = { AndroidPlayerEngine.cycleRepeatMode() },
                onToggleAutoplay = {},
                onJumpTo = { idx ->
                    if (idx in queue.indices) AndroidPlayerEngine.playQueue(queue, idx)
                },
                onRemoveFromQueue = {},
                onMoveInQueue = { _, _ -> },
                onClearQueue = {},
                onOpenMenu = {},
                onOpenAlbum = {},
                onOpenArtist = { _, _ -> },
                onOpenPlaybackSource = {},
                onListenTogether = {},
                lyrics = null,
                lyricsSource = null,
                lyricsProviderStates = emptyMap(),
                onSelectLyricsProvider = {},
                lyricsUnavailable = true,
                lyricsOffsetOpen = false,
                onDismissLyricsOffset = {},
                windowWidth = windowW,
                windowHeight = windowH
            )
        } else {
            Scaffold(
                bottomBar = {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        // MiniPlayer bar when playing or loaded
                        if (currentSong != null) {
                            val song = currentSong!!
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showFullPlayer = true },
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                tonalElevation = 6.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = song.thumbnailUrl,
                                        contentDescription = song.title,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        IconButton(onClick = { AndroidPlayerEngine.togglePlayPause() }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                                tint = Color.White
                                            )
                                        }
                                    }
                                    IconButton(onClick = { AndroidPlayerEngine.skipNext() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = "Siguiente",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Color.White,
                            tonalElevation = 4.dp
                        ) {
                            MobileTab.values().forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 11.sp) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .statusBarsPadding()
                ) {
                    when (selectedTab) {
                        MobileTab.HOME -> MobileHomeView()
                        MobileTab.EXPLORE -> MobileExploreView()
                        MobileTab.SEARCH -> MobileSearchView()
                        MobileTab.LIBRARY -> MobileLibraryView()
                    }
                }
            }
        }
    }
}

@Composable
fun MobileSearchView() {
    var query by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.SONGS) }
    var results by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun executeSearch() {
        if (query.isBlank()) return
        keyboardController?.hide()
        scope.launch {
            isSearching = true
            try {
                val res = withContext(Dispatchers.IO) {
                    YtMusicRepository.search(query.trim(), searchFilter)
                }
                res.onSuccess { items ->
                    results = items.mapNotNull { item ->
                        when (item) {
                            is SearchResult.Track -> item.song
                            is SearchResult.TopTrack -> item.song
                            else -> null
                        }
                    }
                }
            } finally {
                isSearching = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Buscar música",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Canciones, artistas, álbumes...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; results = emptyList() }) {
                        Icon(Icons.Rounded.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { executeSearch() }),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF1E1E28),
                unfocusedContainerColor = Color(0xFF17171F),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color(0xFF2A2A38),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                SearchFilter.SONGS to "Canciones",
                SearchFilter.VIDEOS to "Vídeos",
                SearchFilter.ALBUMS to "Álbumes"
            ).forEach { pair ->
                val filter = pair.first
                val label = pair.second
                FilterChip(
                    selected = searchFilter == filter,
                    onClick = {
                        searchFilter = filter
                        if (query.isNotBlank()) executeSearch()
                    },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF22222E),
                        labelColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (results.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se encontraron resultados", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(results, key = { _, s -> s.videoId }) { index, song ->
                    SongRowItem(
                        song = song,
                        onClick = {
                            AndroidPlayerEngine.playQueue(results, index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileHomeView() {
    var quickPicks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val res = YtMusicRepository.quickPicks()
            res.onSuccess { list ->
                quickPicks = list
            }
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "MaviMusic",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Recomendaciones rápidas para ti",
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (quickPicks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Usa la pestaña Buscar para encontrar canciones", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(quickPicks, key = { _, s -> s.videoId }) { index, song ->
                    SongRowItem(
                        song = song,
                        onClick = {
                            AndroidPlayerEngine.playQueue(quickPicks, index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileExploreView() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Explorar tendencias", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Listas de éxitos y estados de ánimo", color = Color.Gray)
        }
    }
}

@Composable
fun MobileLibraryView() {
    val queue by AndroidPlayerEngine.queue.collectAsState()
    val queueIndex by AndroidPlayerEngine.queueIndex.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Text("Cola actual", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay canciones en la cola", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(queue) { index, song ->
                    val isCurrent = index == queueIndex
                    SongRowItem(
                        song = song,
                        isCurrent = isCurrent,
                        onClick = {
                            AndroidPlayerEngine.playQueue(queue, index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!song.durationText.isNullOrBlank()) {
                Text(
                    text = song.durationText!!,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}
