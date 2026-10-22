package com.music.bitchord.desktop
import androidx.compose.foundation.lazy.LazyListState
import com.music.bitchord.ui.components.LocalShelfRowChrome
import com.music.bitchord.ui.components.ShelfRowChrome
import com.music.bitchord.ui.components.ShelfRow
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateListOf
import com.music.bitchord.ui.components.PAGE_GUTTER
import com.music.bitchord.ui.components.SourceQualityBadge
import com.music.bitchord.ui.components.sourceQualityBadge
import com.music.bitchord.ui.components.contextClick
import com.music.bitchord.ui.components.trackColumnWidth
import com.music.bitchord.ui.replay.ReplayCreditCard
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.music.bitchord.data.YtMusicRepository
import com.music.bitchord.data.model.EntityType
import com.music.bitchord.data.model.SearchHistoryEntity
import com.music.bitchord.ui.LocalPullToRefreshEnabled
import com.music.bitchord.ui.screens.ExploreScreen
import com.music.bitchord.ui.screens.HomeScreen
import com.music.bitchord.ui.screens.CompactTrackRow
import com.music.bitchord.ui.screens.LibraryGridPage
import com.music.bitchord.ui.screens.LibraryLink
import com.music.bitchord.ui.screens.LibraryScreen
import com.music.bitchord.ui.screens.MoodGenrePlaylistsScreen
import com.music.bitchord.ui.screens.SearchScreen
import com.music.bitchord.data.NerdStats
import com.music.bitchord.playback.PlaybackPosition
import com.music.bitchord.playback.QueueSource
import com.music.bitchord.playback.QueueTimeline
import com.music.bitchord.playback.QueueTimeline.asQueueEntry
import com.music.bitchord.data.listentogether.partyQueueIndexOf
import com.music.bitchord.data.listentogether.partyUpcomingAfter
import com.music.bitchord.ui.LyricsProviderState
import com.music.bitchord.data.lyrics.LyricsSource
import com.music.bitchord.ui.player.LyricsSidePanel
import com.music.bitchord.ui.player.NowPlayingScreen
import com.music.bitchord.ui.player.QueueSidePanel
import com.music.bitchord.ui.player.PlayerBack
import com.music.bitchord.ui.player.RepeatModes
import com.music.bitchord.ui.player.rememberMixPulse
import androidx.compose.runtime.SideEffect
import com.music.bitchord.data.model.QueueTier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bitchord.desktopapp.generated.resources.Res
import bitchord.desktopapp.generated.resources.app_logo
import bitchord.desktopapp.generated.resources.logo
import bitchord.desktopapp.generated.resources.logo_mark
import bitchord.desktopapp.generated.resources.sf_pro_display_bold
import bitchord.desktopapp.generated.resources.sf_pro_display_heavy
import bitchord.desktopapp.generated.resources.sf_pro_display_medium
import bitchord.desktopapp.generated.resources.sf_pro_display_regular
import bitchord.desktopapp.generated.resources.sf_pro_display_semibold
import com.music.bitchord.data.model.ArtistPage
import com.music.bitchord.data.model.BrowseItem
import com.music.bitchord.data.model.BrowseType
import com.music.bitchord.data.model.CARD_ART_PX
import com.music.bitchord.data.model.HEADER_ART_PX
import com.music.bitchord.data.model.HomeShelf
import com.music.bitchord.data.model.LibraryPage
import com.music.bitchord.data.model.LikeStatus
import com.music.bitchord.data.model.MoodGenre
import com.music.bitchord.data.model.MoodGenreSection
import com.music.bitchord.data.model.PLAYER_ART_PX
import com.music.bitchord.data.model.PlaylistPrivacy
import com.music.bitchord.data.model.ROW_ART_PX
import com.music.bitchord.data.model.SearchFilter
import com.music.bitchord.data.model.SearchResult
import com.music.bitchord.data.model.ShelfItem
import com.music.bitchord.data.model.PlaybackSourceType
import com.music.bitchord.data.model.Song
import com.music.bitchord.data.model.SubscriptionState
import com.music.bitchord.data.model.UiState
import com.music.bitchord.data.model.UserPlaylist
import com.music.bitchord.data.model.artworkAt
import com.music.bitchord.data.model.durationMillis
import com.music.bitchord.data.model.isMatchMissing
import com.music.bitchord.data.model.isMatchPending
import com.music.bitchord.data.model.isSameTrackAs
import com.music.bitchord.data.model.playingRowMatcher
import com.music.bitchord.data.model.isUnresolvedSpotify
import com.music.bitchord.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.bitchord.data.spotify.SpotifyLibrary
import com.music.bitchord.sharedui.resources.Res as SharedRes
import com.music.bitchord.sharedui.resources.spotify_logo
import com.music.bitchord.data.model.withoutRepeatsOf
import com.music.bitchord.data.settings.AutomixPerformanceMode
import com.music.bitchord.data.settings.LastPlayerScreen
import com.music.bitchord.data.settings.SmartAnalysis
import com.music.bitchord.data.settings.TrackAnalysisState
import com.music.bitchord.data.settings.TransitionWindow
import com.music.bitchord.ui.icons.BitChordIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.jetbrains.compose.resources.Font as composeFont
import org.jetbrains.compose.resources.painterResource

// --- Extracted Library Subpages ---

@Composable
internal fun DesktopHistoryPage(
    history: List<Song>,
    onSongClick: (Song) -> Unit,
    onDownload: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    downloadedIds: Set<String>,
    downloadInProgress: Set<String>,
    contentPadding: PaddingValues,
    menu: (@Composable (Song) -> Unit)? = null,
) {
    DesktopPageScaffold(contentPadding) {
        val isCurrentRow = rememberPlayingRowMatcher(history)
        LazyColumn(
            contentPadding = pagePadding(start = DesktopPageGutter, end = DesktopPageGutter, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (history.isEmpty()) item { DesktopEmptyPage(BitChordIcons.Clock, "Nothing played yet", "Songs you play will show up here.") }
            else items(history, key = Song::videoId) {
                DesktopSongRow(
                    song = it,
                    isCurrent = isCurrentRow(it),
                    liked = false,
                    onClick = onSongClick,
                    onToggleLike = null,
                    onDownload = onDownload,
                    onAddToPlaylist = onAddToPlaylist,
                    downloaded = it.videoId in downloadedIds,
                    downloadInProgress = it.videoId in downloadInProgress,
                    menu = menu,
                )
            }
        }
    }
}

@Composable
internal fun DesktopDownloadsPage(
    downloads: List<Song>,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    menu: (@Composable (Song) -> Unit)? = null,
) {
    DesktopLocalMusicPage(
        songs = downloads,
        onSongClick = onSongClick,
        contentPadding = contentPadding,
        menu = menu,
        title = DesktopStrings["downloads", "Downloads"],
        subtitle = DesktopStrings["d_available_offline", "Available offline"],
        emptyIcon = BitChordIcons.Download,
        emptyTitle = DesktopStrings["d_no_downloads", "No downloads"],
        emptyDescription = DesktopStrings["d_downloaded_songs_will_appear_here", "Downloaded songs will appear here."],
        persistenceKey = "downloaded",
    )
}

@Composable
internal fun DesktopLocalMusicPage(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    contentPadding: PaddingValues,
    title: String = "Local Music",
    subtitle: String = "Audio files found in Music and Downloads",
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector = BitChordIcons.MusicNote,
    emptyTitle: String = "No local music",
    emptyDescription: String = "Put audio files in your Music folder and reopen this page.",
    persistenceKey: String = "local",
    menu: (@Composable (Song) -> Unit)? = null,
) {
    val persistence = remember { DesktopPersistence() }
    var selectedTab by remember(persistenceKey) { mutableStateOf(0) }
    var searchQuery by remember(persistenceKey) { mutableStateOf("") }
    var view by remember(persistenceKey) {
        mutableStateOf(
            if (persistence.string("${persistenceKey}_view", "LIST") == "GRID") {
                DesktopLibraryView.GRID
            } else {
                DesktopLibraryView.LIST
            },
        )
    }
    var sort by remember(persistenceKey) {
        mutableStateOf(
            runCatching {
                DesktopLibrarySort.valueOf(persistence.string("${persistenceKey}_sort", "TITLE_ASC"))
            }.getOrDefault(DesktopLibrarySort.TITLE_ASC),
        )
    }
    var drillDown by remember(persistenceKey) { mutableStateOf<Pair<String, List<Song>>?>(null) }
    val filteredSongs = remember(songs, sort, searchQuery) {
        songs.sortedForDesktopLibrary(sort).filter { song ->
            searchQuery.isBlank() || song.title.contains(searchQuery, true) || song.artist.contains(searchQuery, true) ||
                song.albumName.orEmpty().contains(searchQuery, true)
        }
    }
    val artists = remember(filteredSongs) {
        filteredSongs.groupBy { it.artist.ifBlank { "On This Computer" } }
            .entries.sortedBy { it.key.lowercase() }
    }
    val albums = remember(filteredSongs) {
        filteredSongs.groupBy { it.albumName?.takeIf(String::isNotBlank) ?: "Unknown Album" }
            .entries.sortedBy { it.key.lowercase() }
    }
    DesktopPageScaffold(contentPadding) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = DesktopPageGutter),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DesktopSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    modifier = Modifier.weight(1f),
                    placeholder = DesktopStrings["d_search_your_music", "Search your music"],
                )
                Spacer(Modifier.width(10.dp))
                TextButton(onClick = {
                    sort = when (sort) {
                        DesktopLibrarySort.TITLE_ASC -> DesktopLibrarySort.TITLE_DESC
                        DesktopLibrarySort.TITLE_DESC -> DesktopLibrarySort.DATE_ADDED
                        DesktopLibrarySort.DATE_ADDED -> DesktopLibrarySort.DATE_MODIFIED
                        DesktopLibrarySort.DATE_MODIFIED -> DesktopLibrarySort.TITLE_ASC
                    }
                    persistence.saveString("${persistenceKey}_sort", sort.name)
                }) {
                    Text(sort.label())
                }
                IconButton(onClick = {
                    view = if (view == DesktopLibraryView.LIST) DesktopLibraryView.GRID else DesktopLibraryView.LIST
                    persistence.saveString("${persistenceKey}_view", view.name)
                }) {
                    Icon(if (view == DesktopLibraryView.LIST) BitChordIcons.GridView else BitChordIcons.ListView, "Change view")
                }
            }
            Spacer(Modifier.height(10.dp))
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent) {
                listOf("Songs", "Artists", "Albums").forEachIndexed { index, label ->
                    Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
                }
            }
            Spacer(Modifier.height(8.dp))
            if (songs.isEmpty()) {
                DesktopEmptyPage(emptyIcon, emptyTitle, emptyDescription)
            } else if (drillDown != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { drillDown = null }) { Icon(Icons.Rounded.ArrowBack, DesktopStrings["back", "Back"]) }
                    Text(drillDown!!.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(6.dp))
                DesktopLibrarySongContent(drillDown!!.second, view, onSongClick, menu)
            } else when (selectedTab) {
                0 -> DesktopLibrarySongContent(filteredSongs, view, onSongClick, menu)
                1 -> DesktopLibraryGroupingContent(artists, view, onGroupClick = { drillDown = it })
                else -> DesktopLibraryGroupingContent(albums, view, onGroupClick = { drillDown = it })
            }
        }
    }
}

private fun DesktopLibrarySort.label(): String = when (this) {
    DesktopLibrarySort.TITLE_ASC -> "A–Z"
    DesktopLibrarySort.TITLE_DESC -> "Z–A"
    DesktopLibrarySort.DATE_ADDED -> "Added"
    DesktopLibrarySort.DATE_MODIFIED -> "Modified"
}

@Composable
private fun DesktopLibrarySongContent(
    songs: List<Song>,
    view: DesktopLibraryView,
    onSongClick: (Song) -> Unit,
    menu: (@Composable (Song) -> Unit)? = null,
) {
    if (songs.isEmpty()) {
        DesktopEmptyPage(BitChordIcons.Search, "No matching music", "Try a different search.")
    } else if (view == DesktopLibraryView.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = pagePadding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(songs, key = Song::videoId) { song ->
                Column(Modifier.fillMaxWidth().clickable { onSongClick(song) }) {
                    DesktopArtwork(song.thumbnailUrl, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)))
                    Spacer(Modifier.height(7.dp))
                    Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text(song.artist, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    } else {
        val isCurrentRow = rememberPlayingRowMatcher(songs)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = pagePadding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(songs, key = Song::videoId) { song ->
                DesktopSongRow(
                    song,
                    isCurrent = isCurrentRow(song),
                    liked = false,
                    onClick = onSongClick,
                    onToggleLike = null,
                    downloaded = song.localPath != null,
                    menu = menu,
                )
            }
        }
    }
}

@Composable
private fun DesktopLibraryGroupingContent(
    groups: List<Map.Entry<String, List<Song>>>,
    view: DesktopLibraryView,
    onGroupClick: (Pair<String, List<Song>>) -> Unit,
) {
    if (groups.isEmpty()) {
        DesktopEmptyPage(BitChordIcons.Search, "Nothing here yet", "Music will be grouped as it is added.")
    } else if (view == DesktopLibraryView.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = pagePadding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            gridItems(groups, key = { it.key }) { group ->
                Column(Modifier.fillMaxWidth().clickable { onGroupClick(group.key to group.value) }) {
                    DesktopArtwork(group.value.firstOrNull()?.thumbnailUrl, Modifier.fillMaxWidth().height(150.dp).clip(CircleShape))
                    Spacer(Modifier.height(7.dp))
                    Text(group.key, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                    Text("${group.value.size} songs", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = pagePadding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(groups, key = { it.key }) { group ->
                Row(
                    Modifier.fillMaxWidth().clickable { onGroupClick(group.key to group.value) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopArtwork(group.value.firstOrNull()?.thumbnailUrl, Modifier.size(54.dp).clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(group.key, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                        Text("${group.value.size} songs", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(BitChordIcons.Play, DesktopStrings["open", "Open"], tint = DesktopSecondary)
                }
            }
        }
    }
}

