package com.music.mavimusic.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.host.AndroidAppHost
import com.music.mavimusic.android.host.AndroidPlayerHost
import com.music.mavimusic.android.service.PlaybackService
import com.music.mavimusic.data.model.LikeStatus
import com.music.mavimusic.ui.AppUi
import com.music.mavimusic.ui.player.NowPlayingScreen
import com.music.mavimusic.ui.player.PlayerPlatform

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
                        // MiniPlayer bar when a track is loaded
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
                        MobileTab.HOME -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("MaviMusic: Pantalla de Inicio", color = Color.White)
                            }
                        }
                        MobileTab.EXPLORE -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("MaviMusic: Explorar Tendencias", color = Color.White)
                            }
                        }
                        MobileTab.SEARCH -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("MaviMusic: Búsqueda de Música", color = Color.White)
                            }
                        }
                        MobileTab.LIBRARY -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("MaviMusic: Mi Biblioteca", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
