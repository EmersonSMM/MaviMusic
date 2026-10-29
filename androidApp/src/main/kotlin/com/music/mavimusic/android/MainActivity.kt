package com.music.mavimusic.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.audio.SleepTimer
import com.music.mavimusic.android.data.AndroidPreferences
import com.music.mavimusic.android.data.LocalAudioScanner
import com.music.mavimusic.android.data.OfflineDownloadManager
import com.music.mavimusic.android.data.UserPlaylist
import com.music.mavimusic.android.host.AndroidAppHost
import com.music.mavimusic.android.host.AndroidPlayerHost
import com.music.mavimusic.android.service.PlaybackService
import com.music.mavimusic.android.ui.AddToPlaylistModal
import com.music.mavimusic.android.ui.CreatePlaylistModal
import com.music.mavimusic.android.ui.DesktopSyncModal
import com.music.mavimusic.android.ui.LyricsOffsetModal
import com.music.mavimusic.android.ui.SettingsModal
import com.music.mavimusic.android.ui.SleepTimerModal
import com.music.mavimusic.android.ui.MobileSearchState
import com.music.mavimusic.ui.theme.StatusBarIcons
import com.music.mavimusic.android.ui.TrackActionsModal
import com.music.mavimusic.ui.screens.MOOD_CARD_ASPECT
import com.music.mavimusic.ui.screens.MoodGenreSleeve
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.LikeStatus
import com.music.mavimusic.data.model.MoodGenre
import com.music.mavimusic.data.model.MoodGenreSection
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.ui.AppUi
import com.music.mavimusic.ui.player.NowPlayingScreen
import com.music.mavimusic.ui.player.PlayerPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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

        // Initialize audio engine & persistent storage
        AndroidPlayerEngine.init(applicationContext)

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
    val context = LocalContext.current
    val prefs = remember { AndroidPreferences(context) }
    val searchScope = rememberCoroutineScope()
    val searchState = remember {
        MobileSearchState(searchScope, prefs.getSearchHistory(), prefs::saveSearchHistory)
    }
    var selectedTab by remember { mutableStateOf(MobileTab.HOME) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var showSleepTimerModal by remember { mutableStateOf(false) }
    var showDesktopSyncModal by remember { mutableStateOf(false) }
    var songForPlaylist by remember { mutableStateOf<Song?>(null) }
    var showCreatePlaylistModal by remember { mutableStateOf(false) }
    var showTrackActionsModal by remember { mutableStateOf(false) }
    var showLyricsOffsetModal by remember { mutableStateOf(false) }
    var showSettingsModal by remember { mutableStateOf(false) }
    var lyricsOffsetMs by remember { mutableStateOf(0L) }

    if (!showFullPlayer) StatusBarIcons(dark = false)

    val currentSong by AndroidPlayerEngine.currentSong.collectAsState()
    val isPlaying by AndroidPlayerEngine.isPlaying.collectAsState()
    val isLoading by AndroidPlayerEngine.isLoading.collectAsState()
    val durationMs by AndroidPlayerEngine.durationMs.collectAsState()
    val queue by AndroidPlayerEngine.queue.collectAsState()
    val queueIndex by AndroidPlayerEngine.queueIndex.collectAsState()
    val repeatMode by AndroidPlayerEngine.repeatMode.collectAsState()
    val shuffleEnabled by AndroidPlayerEngine.shuffleEnabled.collectAsState()
    val autoplayEnabled by AndroidPlayerEngine.autoplayEnabled.collectAsState()
    val isFavorite by AndroidPlayerEngine.isFavorite.collectAsState()
    val lyrics by AndroidPlayerEngine.lyrics.collectAsState()
    val lyricsLoading by AndroidPlayerEngine.lyricsLoading.collectAsState()
    val lyricsSource by AndroidPlayerEngine.lyricsSource.collectAsState()
    val sleepTimerMinutes by SleepTimer.minutesLeft.collectAsState()

    val ambientTargetColor = remember(currentSong?.title) {
        if (currentSong != null) {
            val hash = (currentSong?.title.hashCode() xor currentSong?.artist.hashCode()).toLong()
            val hues = listOf(
                Color(0xFF5A1E48), // Deep Berry Magenta
                Color(0xFF1B3B6F), // Midnight Ocean
                Color(0xFF6B1D2F), // Crimson Velvet
                Color(0xFF184E3F), // Emerald Night
                Color(0xFF4B2378), // Royal Indigo
                Color(0xFF66361C)  // Dark Amber
            )
            hues[(Math.abs(hash) % hues.size).toInt()]
        } else {
            Color(0xFF1E1428)
        }
    }
    val animatedAmbientColor by animateColorAsState(
        targetValue = ambientTargetColor,
        animationSpec = tween(durationMillis = 1000)
    )

    BackHandler(enabled = showFullPlayer) {
        showFullPlayer = false
    }

    if (showSleepTimerModal) {
        SleepTimerModal(onDismiss = { showSleepTimerModal = false })
    }
    if (showSettingsModal) {
        SettingsModal(onDismiss = { showSettingsModal = false })
    }
    if (showDesktopSyncModal) {
        DesktopSyncModal(onDismiss = { showDesktopSyncModal = false })
    }
    if (showTrackActionsModal && currentSong != null) {
        TrackActionsModal(
            song = currentSong!!,
            onStartRadio = { AndroidPlayerEngine.startRadio(currentSong!!) },
            onAddToPlaylist = { songForPlaylist = currentSong },
            onAdjustLyricsOffset = { showLyricsOffsetModal = true },
            onDismiss = { showTrackActionsModal = false }
        )
    }
    if (showLyricsOffsetModal) {
        LyricsOffsetModal(
            currentOffsetMs = lyricsOffsetMs,
            onOffsetChange = { lyricsOffsetMs = it },
            onDismiss = { showLyricsOffsetModal = false }
        )
    }
    if (songForPlaylist != null) {
        AddToPlaylistModal(
            song = songForPlaylist!!,
            playlists = prefs.getPlaylists(),
            onSelectPlaylist = { pl ->
                prefs.addSongToPlaylist(pl.id, songForPlaylist!!)
            },
            onCreateNew = {
                showCreatePlaylistModal = true
            },
            onDismiss = { songForPlaylist = null }
        )
    }
    if (showCreatePlaylistModal) {
        CreatePlaylistModal(
            onDismiss = { showCreatePlaylistModal = false },
            onCreate = { newName ->
                val pl = prefs.createPlaylist(newName)
                if (songForPlaylist != null) {
                    prefs.addSongToPlaylist(pl.id, songForPlaylist!!)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C12))
    ) {
        val windowW = maxWidth
        val windowH = maxHeight
        val isTablet = windowW >= 720.dp

        // Atmospheric Ambient Glow Layers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAmbientColor.copy(alpha = 0.35f),
                            animatedAmbientColor.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(Float.POSITIVE_INFINITY, 0f),
                        radius = 1300f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedAmbientColor.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(0f, Float.POSITIVE_INFINITY),
                        radius = 1100f
                    )
                )
        )

        if (showFullPlayer && currentSong != null) {
            val song = currentSong!!
            Box(modifier = Modifier.fillMaxSize()) {
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
                    hasPrevious = shuffleEnabled || queueIndex > 0,
                    hasNext = (shuffleEnabled && queue.size > 1) || queueIndex + 1 < queue.size ||
                        repeatMode == com.music.mavimusic.ui.player.RepeatModes.ALL || autoplayEnabled,
                    repeatMode = repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    autoplayEnabled = autoplayEnabled,
                    signedIn = false,
                    accountName = null,
                    likeStatus = if (isFavorite) LikeStatus.LIKE else LikeStatus.INDIFFERENT,
                    onToggleLike = { AndroidPlayerEngine.toggleCurrentFavorite() },
                    onPlayPause = { AndroidPlayerEngine.togglePlayPause() },
                    onNext = { AndroidPlayerEngine.skipNext() },
                    onPrevious = { AndroidPlayerEngine.skipPrevious() },
                    onBlockedControl = {},
                    onSeek = { AndroidPlayerEngine.seekTo(it) },
                    onSeekFraction = { AndroidPlayerEngine.seekFraction(it) },
                    onToggleShuffle = { AndroidPlayerEngine.toggleShuffle() },
                    onCycleRepeat = { AndroidPlayerEngine.cycleRepeatMode() },
                    onToggleAutoplay = { AndroidPlayerEngine.toggleAutoplay() },
                    onJumpTo = { idx ->
                        if (idx in queue.indices) AndroidPlayerEngine.playQueue(queue, idx)
                    },
                    onRemoveFromQueue = {},
                    onMoveInQueue = { _, _ -> },
                    onClearQueue = {},
                    onOpenMenu = { showTrackActionsModal = true },
                    onOpenAlbum = {},
                    onOpenArtist = { _, _ -> },
                    onOpenPlaybackSource = {},
                    onListenTogether = {},
                    lyrics = lyrics,
                    lyricsSource = lyricsSource,
                    lyricsProviderStates = emptyMap(),
                    onSelectLyricsProvider = {},
                    lyricsUnavailable = (!lyricsLoading && lyrics == null),
                    lyricsOffsetOpen = showLyricsOffsetModal,
                    onDismissLyricsOffset = { showLyricsOffsetModal = false },
                    windowWidth = windowW,
                    windowHeight = windowH
                )

                // Top control buttons on NowPlayingScreen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showFullPlayer = false },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimizar",
                            tint = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { AndroidPlayerEngine.startRadio(song) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Radio,
                                contentDescription = "Iniciar Radio",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { songForPlaylist = song },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = "Añadir a playlist",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showSleepTimerModal = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = "Temporizador",
                                tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color.White
                            )
                        }
                    }
                }
            }
        } else if (isTablet) {
            // Adaptive Tablet Layout: Side NavigationRail + Main Content + Bottom MiniPlayer
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                    containerColor = Color(0xFF101017),
                    contentColor = Color.White,
                    header = {
                        Box(
                            modifier = Modifier
                                .padding(top = 10.dp, bottom = 16.dp)
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF2B66),
                                            Color(0xFF9E1FBF)
                                        )
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = "MaviMusic",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                ) {
                    MobileTab.values().forEach { tab ->
                        NavigationRailItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color(0xFF8E8E9F),
                                unselectedTextColor = Color(0xFF8E8E9F),
                                indicatorColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Island Dock for Utilities
                    Surface(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        color = Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            IconButton(
                                onClick = { showSleepTimerModal = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bedtime,
                                    contentDescription = "Temporizador",
                                    tint = if (sleepTimerMinutes != null) MaterialTheme.colorScheme.primary else Color(0xFFA0A0B2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            IconButton(
                                onClick = { showDesktopSyncModal = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Computer,
                                    contentDescription = "Sincronizar PC/Mac",
                                    tint = Color(0xFFA0A0B2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.08f))
                            )

                            IconButton(
                                onClick = { showSettingsModal = true },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "Ajustes",
                                    tint = Color(0xFFA0A0B2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Vertical Divider between NavigationRail and Content
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.06f))
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .navigationBarsPadding()
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().statusBarsPadding()) {
                        when (selectedTab) {
                            MobileTab.HOME -> MobileHomeView(
                                onAddToPlaylist = { songForPlaylist = it },
                                onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                            )
                            MobileTab.EXPLORE -> MobileExploreView(
                                onAddToPlaylist = { songForPlaylist = it },
                                onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                            )
                            MobileTab.SEARCH -> MobileSearchView(
                                state = searchState,
                                onAddToPlaylist = { songForPlaylist = it },
                                onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                            )
                            MobileTab.LIBRARY -> MobileLibraryView(
                                onAddToPlaylist = { songForPlaylist = it },
                                onStartRadio = { AndroidPlayerEngine.startRadio(it) },
                                onOpenSettings = { showSettingsModal = true }
                            )
                        }
                    }

                    if (currentSong != null) {
                        MiniPlayerBar(
                            song = currentSong!!,
                            isPlaying = isPlaying,
                            isLoading = isLoading,
                            durationMs = durationMs,
                            onClick = { showFullPlayer = true },
                            onPlayPause = { AndroidPlayerEngine.togglePlayPause() },
                            onNext = { AndroidPlayerEngine.skipNext() }
                        )
                    }
                }
            }
        } else {
            // Phone Layout: Bottom Navigation Bar + MiniPlayer
            Scaffold(
                bottomBar = {
                    Column(modifier = Modifier.navigationBarsPadding()) {
                        if (currentSong != null) {
                            MiniPlayerBar(
                                song = currentSong!!,
                                isPlaying = isPlaying,
                                isLoading = isLoading,
                                durationMs = durationMs,
                                onClick = { showFullPlayer = true },
                                onPlayPause = { AndroidPlayerEngine.togglePlayPause() },
                                onNext = { AndroidPlayerEngine.skipNext() }
                            )
                        }

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
                        MobileTab.HOME -> MobileHomeView(
                            onAddToPlaylist = { songForPlaylist = it },
                            onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                        )
                        MobileTab.EXPLORE -> MobileExploreView(
                            onAddToPlaylist = { songForPlaylist = it },
                            onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                        )
                        MobileTab.SEARCH -> MobileSearchView(
                            state = searchState,
                            onAddToPlaylist = { songForPlaylist = it },
                            onStartRadio = { AndroidPlayerEngine.startRadio(it) }
                        )
                        MobileTab.LIBRARY -> MobileLibraryView(
                            onAddToPlaylist = { songForPlaylist = it },
                            onStartRadio = { AndroidPlayerEngine.startRadio(it) },
                            onOpenSettings = { showSettingsModal = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    isLoading: Boolean,
    durationMs: Long = 0L,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val positionMs = AndroidPlayerEngine.position.positionMs
    val progress = remember(positionMs, durationMs) {
        if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.7f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xE620202E),
                        Color(0xF214141E)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.18f),
                        Color.White.copy(alpha = 0.04f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF282836))
                ) {
                    AsyncImage(
                        model = song.thumbnailUrl,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artist,
                            color = Color(0xFFB0B0C4),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Siguiente",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Integrated Glowing Progress Line at the very bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    Color(0xFFFF5277)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun MobileSearchView(
    state: MobileSearchState,
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit
) {
    val query = state.query
    val searchFilter = state.filter
    val results = state.results
    val suggestions = state.suggestions
    val isSearching = state.isSearching
    val searchHistory = state.history
    val keyboardController = LocalSoftwareKeyboardController.current

    fun executeSearch(searchQuery: String = query) {
        keyboardController?.hide()
        state.submit(searchQuery)
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
            onValueChange = state::updateQuery,
            placeholder = { Text("Canciones, artistas, álbumes...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { state.updateQuery("") }) {
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
                        keyboardController?.hide()
                        state.selectFilter(filter)
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
        } else if (suggestions.isNotEmpty() && results.isEmpty()) {
            LazyColumn(
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(suggestions.size) { idx ->
                    val item = suggestions[idx]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { executeSearch(item) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = item, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        } else if (query.isEmpty() && results.isEmpty()) {
            // Recent searches history
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = "Búsquedas recientes",
                    color = Color.LightGray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(searchHistory.size) { idx ->
                        val hist = searchHistory[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { executeSearch(hist) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = hist, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    state.removeHistory(hist)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Eliminar", tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        } else if (results.isEmpty() && query.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "No se encontraron resultados", color = Color.Gray)
            }
        } else {
            LazyColumn(
                state = state.resultsScroll,
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(results, key = { _, s -> s.videoId }) { index, song ->
                    SongRowItem(
                        song = song,
                        onClick = {
                            AndroidPlayerEngine.playQueue(results, index)
                        },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onStartRadio = { onStartRadio(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileHomeView(
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit
) {
    var quickPicks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val currentPlayingSong by AndroidPlayerEngine.currentSong.collectAsState()

    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = remember(hour) {
        when (hour) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val res = YtMusicRepository.quickPicks()
            res.onSuccess { list ->
                quickPicks = list
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (quickPicks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Usa la pestaña Buscar para encontrar canciones", color = Color.Gray)
        }
    } else {
        val heroSong = quickPicks.firstOrNull()
        val horizontalRecommendations = remember(quickPicks) { quickPicks.drop(1).take(10) }
        val restOfTracks = remember(quickPicks) { quickPicks.drop(6) }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header: Greeting + Brand
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = greeting,
                            color = Color(0xFFA0A0B2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "MaviMusic",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ONLINE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Hero Banner: Panoramic featured mix card
            if (heroSong != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.8f)
                            .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF38122B),
                                        Color(0xFF151833)
                                    )
                                )
                            )
                            .clickable {
                                AndroidPlayerEngine.playQueue(quickPicks, 0)
                            }
                    ) {
                        // Background Artwork faded
                        AsyncImage(
                            model = heroSong.thumbnailUrl,
                            contentDescription = heroSong.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.35f)
                        )
                        // Gradient Overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xF012121A),
                                            Color(0x9912121A),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        // Content
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "★ DESTACADO DE HOY",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = heroSong.title,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = heroSong.artist,
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { AndroidPlayerEngine.playQueue(quickPicks, 0) },
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reproducir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Horizontal Carousel: Éxitos y Recomendados
            if (horizontalRecommendations.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Para descubrir",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Deslizar ▸",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(end = 16.dp)
                        ) {
                            itemsIndexed(horizontalRecommendations) { rIdx, song ->
                                val actualIdx = rIdx + 1
                                Column(
                                    modifier = Modifier
                                        .width(132.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            AndroidPlayerEngine.playQueue(quickPicks, actualIdx)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(132.dp)
                                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = Color.Black.copy(alpha = 0.5f))
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF222230))
                                    ) {
                                        AsyncImage(
                                            model = song.thumbnailUrl,
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Floating Play Pill in corner
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(8.dp)
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.65f))
                                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Reproducir",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = Color(0xFFA0A0B2),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Tracks List
            item {
                Text(
                    text = "Recomendaciones rápidas",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(restOfTracks, key = { _, s -> s.videoId }) { index, song ->
                val queueIdx = index + 6
                SongRowItem(
                    song = song,
                    isCurrent = currentPlayingSong?.videoId == song.videoId,
                    onClick = {
                        AndroidPlayerEngine.playQueue(quickPicks, queueIdx)
                    },
                    onAddToPlaylist = { onAddToPlaylist(song) },
                    onStartRadio = { onStartRadio(song) }
                )
            }
        }
    }
}

@Composable
fun MobileExploreView(
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit
) {
    var sections by remember { mutableStateOf<List<MoodGenreSection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedGenreSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedGenreTitle by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val res = YtMusicRepository.moodAndGenres()
            res.onSuccess { list ->
                sections = list
            }
        }
        isLoading = false
    }

    LaunchedEffect(sections) {
        if (sections.isEmpty()) return@LaunchedEffect
        if (sections.all { section -> section.items.all { it.thumbnailUrl != null } }) return@LaunchedEffect
        val limiter = Semaphore(4)
        coroutineScope {
            sections.flatMap(MoodGenreSection::items)
                .filter { it.thumbnailUrl == null }
                .forEach { item ->
                    launch(Dispatchers.IO) {
                        val artwork = limiter.withPermit {
                            YtMusicRepository.moodGenreArtwork(item.browseId, item.params).getOrNull()
                        } ?: return@launch
                        withContext(Dispatchers.Main) {
                            sections = sections.map { section ->
                                section.copy(
                                    items = section.items.map { entry ->
                                        if (entry.browseId == item.browseId && entry.params == item.params) {
                                            entry.copy(thumbnailUrl = artwork)
                                        } else {
                                            entry
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectedGenreTitle != null) {
                IconButton(onClick = { selectedGenreTitle = null; selectedGenreSongs = emptyList() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
            }
            Text(
                text = selectedGenreTitle ?: "Explorar estados de ánimo y géneros",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (selectedGenreTitle != null) {
            // Genre tracks header and play all
            Column(modifier = Modifier.fillMaxSize()) {
                if (selectedGenreSongs.isNotEmpty()) {
                    Button(
                        onClick = { AndroidPlayerEngine.playQueue(selectedGenreSongs, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reproducir todo (${selectedGenreSongs.size} canciones)", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(selectedGenreSongs, key = { _, s -> s.videoId }) { index, song ->
                        SongRowItem(
                            song = song,
                            onClick = { AndroidPlayerEngine.playQueue(selectedGenreSongs, index) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onStartRadio = { onStartRadio(song) }
                        )
                    }
                }
            }
        } else {
            // Grid of categories
            val allGenres = remember(sections) { sections.flatMap { it.items } }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allGenres.size) { gIdx ->
                    val genre = allGenres[gIdx]
                    MoodGenreSleeve(
                        item = genre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(MOOD_CARD_ASPECT)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                selectedGenreTitle = genre.title
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val res = withContext(Dispatchers.IO) {
                                            YtMusicRepository.moodGenreShelves(genre.browseId, genre.params)
                                        }
                                        res.onSuccess { shelves ->
                                            val songs = shelves.flatMap { sh ->
                                                sh.items.mapNotNull { item ->
                                                    if (item.videoId != null) {
                                                        Song(
                                                            videoId = item.videoId!!,
                                                            title = item.title,
                                                            artist = item.subtitle,
                                                            thumbnailUrl = item.thumbnailUrl
                                                        )
                                                    } else null
                                                }
                                            }
                                            selectedGenreSongs = songs
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}

@Composable
fun MobileLibraryView(
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { AndroidPreferences(context) }
    var libraryTab by remember { mutableStateOf(0) } // 0 = Descargas, 1 = Favoritos, 2 = Playlists, 3 = Locales, 4 = Cola
    val queue by AndroidPlayerEngine.queue.collectAsState()
    val queueIndex by AndroidPlayerEngine.queueIndex.collectAsState()
    val downloadedSongs by OfflineDownloadManager.downloadedSongs.collectAsState()
    var favorites by remember { mutableStateOf(prefs.getFavorites()) }
    var playlists by remember { mutableStateOf(prefs.getPlaylists()) }
    var localTracks by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedPlaylist by remember { mutableStateOf<UserPlaylist?>(null) }
    var showCreateModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        favorites = prefs.getFavorites()
        playlists = prefs.getPlaylists()
        withContext(Dispatchers.IO) {
            localTracks = LocalAudioScanner.scanLocalTracks(context)
        }
    }

    if (showCreateModal) {
        CreatePlaylistModal(
            onDismiss = { showCreateModal = false },
            onCreate = { newName ->
                prefs.createPlaylist(newName)
                playlists = prefs.getPlaylists()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tu Biblioteca", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (libraryTab == 2 && selectedPlaylist == null) {
                    IconButton(onClick = { showCreateModal = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Nueva Playlist", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = Color.LightGray)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal scrolling tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = libraryTab == 0,
                onClick = { libraryTab = 0; selectedPlaylist = null },
                label = { Text("Descargas (${downloadedSongs.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF22222E),
                    labelColor = Color.LightGray
                )
            )
            FilterChip(
                selected = libraryTab == 1,
                onClick = {
                    libraryTab = 1
                    selectedPlaylist = null
                    favorites = prefs.getFavorites()
                },
                label = { Text("Favoritos (${favorites.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF22222E),
                    labelColor = Color.LightGray
                )
            )
            FilterChip(
                selected = libraryTab == 2,
                onClick = {
                    libraryTab = 2
                    selectedPlaylist = null
                    playlists = prefs.getPlaylists()
                },
                label = { Text("Playlists (${playlists.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF22222E),
                    labelColor = Color.LightGray
                )
            )
            FilterChip(
                selected = libraryTab == 3,
                onClick = { libraryTab = 3; selectedPlaylist = null },
                label = { Text("Archivos locales (${localTracks.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF22222E),
                    labelColor = Color.LightGray
                )
            )
            FilterChip(
                selected = libraryTab == 4,
                onClick = { libraryTab = 4; selectedPlaylist = null },
                label = { Text("Cola actual (${queue.size})", fontSize = 13.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF22222E),
                    labelColor = Color.LightGray
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (libraryTab) {
            0 -> {
                // Descargas offline
                if (downloadedSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                tint = Color.DarkGray,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tienes canciones descargadas", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pulsa el icono de descarga en cualquier canción para escucharla sin conexión", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    Button(
                        onClick = { AndroidPlayerEngine.playQueue(downloadedSongs, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reproducir todo sin conexión (${downloadedSongs.size})", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(downloadedSongs, key = { _, s -> s.videoId }) { index, song ->
                            val isCurrent = song.videoId == AndroidPlayerEngine.currentSong.value?.videoId
                            SongRowItem(
                                song = song,
                                isCurrent = isCurrent,
                                onClick = { AndroidPlayerEngine.playQueue(downloadedSongs, index) },
                                onDelete = { OfflineDownloadManager.deleteDownload(song.videoId) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onStartRadio = { onStartRadio(song) }
                            )
                        }
                    }
                }
            }
            1 -> {
                // Favoritos
                if (favorites.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No tienes favoritos aún", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pulsa el corazón en el reproductor para marcar canciones que te encanten", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 32.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                } else {
                    Button(
                        onClick = { AndroidPlayerEngine.playQueue(favorites, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reproducir favoritos (${favorites.size})", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(favorites, key = { _, s -> s.videoId }) { index, song ->
                            val isCurrent = song.videoId == AndroidPlayerEngine.currentSong.value?.videoId
                            SongRowItem(
                                song = song,
                                isCurrent = isCurrent,
                                onClick = { AndroidPlayerEngine.playQueue(favorites, index) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onStartRadio = { onStartRadio(song) }
                            )
                        }
                    }
                }
            }
            2 -> {
                // Playlists
                if (selectedPlaylist != null) {
                    val pl = selectedPlaylist!!
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedPlaylist = null; playlists = prefs.getPlaylists() }) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Volver", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("${pl.songs.size} canciones", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (pl.songs.isNotEmpty()) {
                            Button(
                                onClick = { AndroidPlayerEngine.playQueue(pl.songs, 0) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reproducir playlist", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(pl.songs, key = { _, s -> s.videoId }) { index, song ->
                                    val isCurrent = song.videoId == AndroidPlayerEngine.currentSong.value?.videoId
                                    SongRowItem(
                                        song = song,
                                        isCurrent = isCurrent,
                                        onClick = { AndroidPlayerEngine.playQueue(pl.songs, index) },
                                        onDelete = {
                                            prefs.removeSongFromPlaylist(pl.id, song.videoId)
                                            val updated = prefs.getPlaylists().find { it.id == pl.id }
                                            selectedPlaylist = updated
                                            playlists = prefs.getPlaylists()
                                        },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                        onStartRadio = { onStartRadio(song) }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Esta playlist está vacía", color = Color.Gray)
                            }
                        }
                    }
                } else {
                    if (playlists.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(52.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No tienes playlists aún", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { showCreateModal = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Crear primera playlist", color = Color.White)
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(playlists) { pl ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedPlaylist = pl },
                                    color = Color(0xFF1E1E28)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                            Text("${pl.songs.size} canciones", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        IconButton(onClick = {
                                            prefs.deletePlaylist(pl.id)
                                            playlists = prefs.getPlaylists()
                                        }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Eliminar playlist", tint = Color.DarkGray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Archivos locales
                if (localTracks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(52.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No se encontraron canciones en el dispositivo", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Añade canciones a la carpeta Music o Descargas de tu tablet", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = { AndroidPlayerEngine.playQueue(localTracks, 0) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reproducir todo (${localTracks.size} archivos locales)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(localTracks, key = { _, s -> s.videoId }) { index, song ->
                            val isCurrent = song.videoId == AndroidPlayerEngine.currentSong.value?.videoId
                            SongRowItem(
                                song = song,
                                isCurrent = isCurrent,
                                onClick = { AndroidPlayerEngine.playQueue(localTracks, index) },
                                onAddToPlaylist = { onAddToPlaylist(song) }
                            )
                        }
                    }
                }
            }
            4 -> {
                // Cola actual
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
                                onClick = { AndroidPlayerEngine.playQueue(queue, index) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onStartRadio = { onStartRadio(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongRowItem(
    song: Song,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onStartRadio: (() -> Unit)? = null
) {
    val downloadedSongs by OfflineDownloadManager.downloadedSongs.collectAsState()
    val downloadingIds by OfflineDownloadManager.downloadingIds.collectAsState()
    val isDownloaded = remember(downloadedSongs, song.videoId) {
        downloadedSongs.any { it.videoId == song.videoId }
    }
    val isDownloading = remember(downloadingIds, song.videoId) {
        downloadingIds.contains(song.videoId)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .then(
                if (isCurrent) Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                else Modifier
            ),
        color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF222230))
            ) {
                AsyncImage(
                    model = song.thumbnailUrl,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isCurrent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
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
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (onStartRadio != null && !song.videoId.startsWith("local_")) {
                IconButton(onClick = onStartRadio, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Radio,
                        contentDescription = "Iniciar Radio",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onAddToPlaylist != null) {
                IconButton(onClick = onAddToPlaylist, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Añadir a playlist",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (isDownloading) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                }
            } else if (isDownloaded) {
                IconButton(
                    onClick = { OfflineDownloadManager.deleteDownload(song.videoId) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = "Descargada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else if (!song.videoId.startsWith("local_")) {
                IconButton(
                    onClick = { OfflineDownloadManager.downloadSong(song) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = "Descargar",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
