package com.music.mavimusic.desktop
import androidx.compose.foundation.lazy.LazyListState
import com.music.mavimusic.ui.components.LocalShelfRowChrome
import com.music.mavimusic.ui.components.ShelfRowChrome
import com.music.mavimusic.ui.components.ShelfRow
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
import com.music.mavimusic.ui.components.PAGE_GUTTER
import com.music.mavimusic.ui.components.SourceQualityBadge
import com.music.mavimusic.ui.components.sourceQualityBadge
import com.music.mavimusic.ui.components.contextClick
import com.music.mavimusic.ui.components.trackColumnWidth
import com.music.mavimusic.ui.replay.ReplayCreditCard
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.EntityType
import com.music.mavimusic.data.model.SearchHistoryEntity
import com.music.mavimusic.ui.LocalPullToRefreshEnabled
import com.music.mavimusic.ui.screens.ExploreScreen
import com.music.mavimusic.ui.screens.HomeScreen
import com.music.mavimusic.ui.screens.CompactTrackRow
import com.music.mavimusic.ui.screens.LibraryGridPage
import com.music.mavimusic.ui.screens.LibraryLink
import com.music.mavimusic.ui.screens.LibraryScreen
import com.music.mavimusic.ui.screens.MoodGenrePlaylistsScreen
import com.music.mavimusic.ui.screens.SearchScreen
import com.music.mavimusic.data.NerdStats
import com.music.mavimusic.playback.PlaybackPosition
import com.music.mavimusic.playback.QueueSource
import com.music.mavimusic.playback.QueueTimeline
import com.music.mavimusic.playback.QueueTimeline.asQueueEntry
import com.music.mavimusic.data.listentogether.partyQueueIndexOf
import com.music.mavimusic.data.listentogether.partyUpcomingAfter
import com.music.mavimusic.ui.LyricsProviderState
import com.music.mavimusic.data.lyrics.LyricsSource
import com.music.mavimusic.ui.player.LyricsSidePanel
import com.music.mavimusic.ui.player.NowPlayingScreen
import com.music.mavimusic.ui.player.QueueSidePanel
import com.music.mavimusic.ui.player.PlayerBack
import com.music.mavimusic.ui.player.RepeatModes
import com.music.mavimusic.ui.player.rememberMixPulse
import androidx.compose.runtime.SideEffect
import com.music.mavimusic.data.model.QueueTier
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
import mavimusic.desktopapp.generated.resources.Res
import mavimusic.desktopapp.generated.resources.app_logo
import mavimusic.desktopapp.generated.resources.logo
import mavimusic.desktopapp.generated.resources.logo_mark
import mavimusic.desktopapp.generated.resources.sf_pro_display_bold
import mavimusic.desktopapp.generated.resources.sf_pro_display_heavy
import mavimusic.desktopapp.generated.resources.sf_pro_display_medium
import mavimusic.desktopapp.generated.resources.sf_pro_display_regular
import mavimusic.desktopapp.generated.resources.sf_pro_display_semibold
import com.music.mavimusic.data.model.ArtistPage
import com.music.mavimusic.data.model.BrowseItem
import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.CARD_ART_PX
import com.music.mavimusic.data.model.HEADER_ART_PX
import com.music.mavimusic.data.model.HomeShelf
import com.music.mavimusic.data.model.LibraryPage
import com.music.mavimusic.data.model.LikeStatus
import com.music.mavimusic.data.model.MoodGenre
import com.music.mavimusic.data.model.MoodGenreSection
import com.music.mavimusic.data.model.PLAYER_ART_PX
import com.music.mavimusic.data.model.PlaylistPrivacy
import com.music.mavimusic.data.model.ROW_ART_PX
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.ShelfItem
import com.music.mavimusic.data.model.PlaybackSourceType
import com.music.mavimusic.data.model.Song
import com.music.mavimusic.data.model.SubscriptionState
import com.music.mavimusic.data.model.UiState
import com.music.mavimusic.data.model.UserPlaylist
import com.music.mavimusic.data.model.artworkAt
import com.music.mavimusic.data.model.durationMillis
import com.music.mavimusic.data.model.isMatchMissing
import com.music.mavimusic.data.model.isMatchPending
import com.music.mavimusic.data.model.isSameTrackAs
import com.music.mavimusic.data.model.playingRowMatcher
import com.music.mavimusic.data.model.isUnresolvedSpotify
import com.music.mavimusic.data.spotify.SPOTIFY_PAGE_PREFIX
import com.music.mavimusic.data.spotify.SpotifyLibrary
import com.music.mavimusic.desktopui.resources.Res as SharedRes
import com.music.mavimusic.desktopui.resources.spotify_logo
import com.music.mavimusic.data.model.withoutRepeatsOf
import com.music.mavimusic.data.settings.AutomixPerformanceMode
import com.music.mavimusic.data.settings.LastPlayerScreen
import com.music.mavimusic.data.settings.SmartAnalysis
import com.music.mavimusic.data.settings.TrackAnalysisState
import com.music.mavimusic.data.settings.TransitionWindow
import com.music.mavimusic.ui.icons.MaviMusicIcons
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

// --- Extracted Navigation Chrome Components ---

@Composable
internal fun DesktopTopBar(
    compact: Boolean,
    accountAvatar: String?,
    onOpenAccounts: () -> Unit,
    canGoBack: Boolean,
    onBack: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    focusSearch: Boolean,
    onSearchFocused: () -> Unit,
    onToggleMiniPlayer: () -> Unit = {},
    onFeelingLucky: () -> Unit = {},
    onOpenCommandPalette: () -> Unit = {},
    downloadingCount: Int = 0,
    onOpenDownloads: () -> Unit = {},
) {
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            runCatching { searchFocusRequester.requestFocus() }
            onSearchFocused()
        }
    }
    DesktopTitleBarDragArea(Modifier.fillMaxWidth().height(52.dp)) {
        Box(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxSize()
                    .desktopWindowGlass(DesktopChromeEdge.BOTTOM)
                    .padding(
                        start = 18.dp,
                        end = 14.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MaviMusicFireWordmark()
                    Spacer(Modifier.width(12.dp))
                    DesktopToolbarButton(onClick = onBack, enabled = canGoBack, size = 32.dp) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            DesktopStrings["back", "Atrás"],
                            tint = if (canGoBack) Color.White else DesktopSecondary.copy(alpha = 0.35f),
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                // Centered search bar like YouTube Music with "Azar" button
                Box(
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.widthIn(max = 660.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(Modifier.weight(1f, fill = false).widthIn(max = 480.dp).fillMaxWidth()) {
                            DesktopSearchField(
                                query = query,
                                onQueryChange = onQueryChange,
                                onSearch = onSearch,
                                focusRequester = searchFocusRequester,
                                placeholder = DesktopStrings["search", "Buscar canciones, álbumes, artistas..."],
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = onFeelingLucky,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "🎲",
                                    fontSize = 13.sp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = DesktopStrings["feeling_lucky", "Azar"],
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            onClick = onOpenCommandPalette,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.height(36.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (DesktopPlatform.isMac) "⌘K" else "Ctrl+K",
                                    color = DesktopAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (downloadingCount > 0) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                onClick = onOpenDownloads,
                                shape = RoundedCornerShape(20.dp),
                                color = DesktopAccent.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, DesktopAccent.copy(alpha = 0.35f)),
                                modifier = Modifier.height(36.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = DesktopAccent,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "$downloadingCount",
                                        color = DesktopAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    DesktopAccountButton(avatar = accountAvatar, onClick = onOpenAccounts)
                }
            }

            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                thickness = 1.dp,
                color = desktopChromeDivider(),
            )
        }
    }
}

@Composable
internal fun DesktopToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: Dp = 36.dp,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .desktopHoverWash()
            .clickable(
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun DesktopSidebar(
    destination: DesktopDestination,
    settingsOpen: Boolean,
    accountPlaylists: List<ShelfItem>,
    localPlaylists: List<DesktopPlaylist>,
    openedCollectionId: String?,
    onDestinationSelected: (DesktopDestination) -> Unit,
    onOpenAccountPlaylist: (ShelfItem) -> Unit,
    onOpenLocalPlaylist: (DesktopPlaylist) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val titleBarEnabled by DesktopTitleBarSetting.enabled.collectAsState()
    val inlineCaption = (DesktopPlatform.drawsOwnWindowFrame || DesktopPlatform.isMac) && !titleBarEnabled
    // Rows the listener has tucked away; keyed the same way the sidebar lists them.
    val hiddenSidebar by DesktopSidebarVisibility.hidden.collectAsState()
    Box(
        Modifier
            .width(230.dp)
            .fillMaxHeight()
            .desktopWindowGlass(DesktopChromeEdge.END, fade = 0.07f),
    ) {
        Column(Modifier.fillMaxSize()) {
            // The sidebar runs to the top of the window, so its head is the window's caption: the
            // three buttons, and room to take hold of the window by.
            if (inlineCaption) {
                DesktopTitleBarDragArea(Modifier.fillMaxWidth().height(SIDEBAR_CAPTION_HEIGHT)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(start = if (DesktopPlatform.isMac) 16.dp else 10.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        DesktopWindowButtons()
                    }
                }
            }
        Column(
            Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = if (inlineCaption) 8.dp else 16.dp, bottom = 16.dp),
        ) {
            DesktopSidebarItem(MaviMusicIcons.Home, DesktopStrings["home", "Inicio"], destination == DesktopDestination.LISTEN_NOW) {
                onDestinationSelected(DesktopDestination.LISTEN_NOW)
            }
            DesktopSidebarItem(MaviMusicIcons.Explore, DesktopStrings["explore", "Explorar"], destination == DesktopDestination.EXPLORE) {
                onDestinationSelected(DesktopDestination.EXPLORE)
            }
            DesktopSidebarItem(MaviMusicIcons.Library, DesktopStrings["library", "Biblioteca"], destination == DesktopDestination.LIBRARY) {
                onDestinationSelected(DesktopDestination.LIBRARY)
            }
            DesktopSidebarItem(MaviMusicIcons.Search, DesktopStrings["search", "Buscar"], destination == DesktopDestination.SEARCH) {
                onDestinationSelected(DesktopDestination.SEARCH)
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = desktopChromeDivider())
            Spacer(Modifier.height(12.dp))
            if (HISTORY_KEY !in hiddenSidebar) {
                DesktopSidebarItem(
                    MaviMusicIcons.Clock,
                    DesktopStrings["history", "Historial"],
                    destination == DesktopDestination.HISTORY,
                    onClick = { onDestinationSelected(DesktopDestination.HISTORY) },
                    onHide = { DesktopSidebarVisibility.hide(HISTORY_KEY, DesktopStrings["history", "Historial"]) },
                )
            }
            val queued by DesktopDownloadQueue.active.collectAsState()
            if (DOWNLOADS_KEY !in hiddenSidebar) {
                DesktopSidebarItem(
                    MaviMusicIcons.Download,
                    if (queued.isEmpty()) DesktopStrings["downloads", "Descargas"] else "${DesktopStrings["downloads", "Descargas"]} · ${queued.size}",
                    destination == DesktopDestination.DOWNLOADS,
                    onClick = { onDestinationSelected(DesktopDestination.DOWNLOADS) },
                    onHide = { DesktopSidebarVisibility.hide(DOWNLOADS_KEY, DesktopStrings["downloads", "Descargas"]) },
                )
            }
            if (LOCAL_MUSIC_KEY !in hiddenSidebar) {
                DesktopSidebarItem(
                    MaviMusicIcons.Library,
                    DesktopStrings["local_music", "Música local"],
                    destination == DesktopDestination.LOCAL_MUSIC,
                    onClick = { onDestinationSelected(DesktopDestination.LOCAL_MUSIC) },
                    onHide = { DesktopSidebarVisibility.hide(LOCAL_MUSIC_KEY, DesktopStrings["local_music", "Música local"]) },
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = desktopChromeDivider())
            Spacer(Modifier.height(12.dp))
            val visibleAccount = accountPlaylists.filterNot { "account:${it.browseId}" in hiddenSidebar }
            val visibleLocal = localPlaylists.filterNot { "local:${it.id}" in hiddenSidebar }
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(visibleAccount, key = { "account:${it.browseId}" }) { playlist ->
                    DesktopSidebarItem(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        playlist.title,
                        openedCollectionId == playlist.browseId,
                        onClick = { onOpenAccountPlaylist(playlist) },
                        onHide = {
                            DesktopSidebarVisibility.hide("account:${playlist.browseId}", playlist.title)
                        },
                    )
                }
                items(visibleLocal, key = { "local:${it.id}" }) { playlist ->
                    DesktopSidebarItem(
                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                        playlist.title,
                        openedCollectionId == playlist.id,
                        onClick = { onOpenLocalPlaylist(playlist) },
                        onHide = { DesktopSidebarVisibility.hide("local:${playlist.id}", playlist.title) },
                    )
                }
                if (visibleAccount.isEmpty() && visibleLocal.isEmpty()) {
                    item {
                        Text(
                            DesktopStrings["d_your_playlists_empty", "Tus listas de reproducción aparecerán aquí"],
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                            color = DesktopSecondary.copy(alpha = 0.68f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            HorizontalDivider(color = desktopChromeDivider())
            Spacer(Modifier.height(8.dp))
            if (hiddenSidebar.isNotEmpty()) {
                DesktopSidebarRestoreRow(hiddenSidebar)
            }
            DesktopSidebarItem(Icons.Rounded.Settings, DesktopStrings["settings", "Ajustes"], settingsOpen) {
                onOpenSettings()
            }
        }
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(1.dp)
                .fillMaxHeight()
                .background(desktopChromeDivider()),
        )
    }
}

@Composable
internal fun DesktopSidebarItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    /** When set, a right-click offers to hide the row — see [DesktopSidebarVisibility]. */
    onHide: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .desktopHoverFill()
                .clip(RoundedCornerShape(7.dp))
                .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent)
                .clickable(onClick = onClick)
                .then(
                    if (onHide == null) Modifier
                    else Modifier.pointerInput(onHide) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                    menuOpen = true
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    },
                )
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, label, tint = if (selected) DesktopAccent else DesktopSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                label,
                color = if (selected) Color.White else DesktopSecondary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onHide != null) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(DesktopStrings["hide_from_sidebar", "Hide from sidebar"]) },
                    onClick = {
                        menuOpen = false
                        onHide()
                    },
                )
            }
        }
    }
}

/** The secondary destinations that can be tucked away, by the key they hide under. */
private const val HISTORY_KEY = "dest:HISTORY"
private const val DOWNLOADS_KEY = "dest:DOWNLOADS"
private const val LOCAL_MUSIC_KEY = "dest:LOCAL_MUSIC"

/**
 * The "N hidden" row that brings tucked-away rows back: a menu of everything
 * hidden, each restoring on its own, plus a "Show all". Only drawn while
 * something is hidden, so an ordinary sidebar never shows it.
 */
@Composable
internal fun DesktopSidebarRestoreRow(hidden: Map<String, String>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .desktopHoverFill()
                .clip(RoundedCornerShape(7.dp))
                .clickable { open = true }
                .padding(horizontal = 9.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.RemoveCircleOutline,
                null,
                tint = DesktopSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                DesktopStrings.format("n_hidden", hidden.size, fallback = "%1\$d hidden"),
                color = DesktopSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            hidden.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        DesktopSidebarVisibility.show(key)
                        if (DesktopSidebarVisibility.hidden.value.isEmpty()) open = false
                    },
                )
            }
            if (hidden.size > 1) {
                DropdownMenuItem(
                    text = { Text(DesktopStrings["show_all", "Show all"]) },
                    onClick = {
                        DesktopSidebarVisibility.showAll()
                        open = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun DesktopBottomChrome(
    compact: Boolean,
    destination: DesktopDestination,
    song: Song?,
    isPlaying: Boolean,
    onDestinationSelected: (DesktopDestination) -> Unit,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (compact && song != null) {
            DesktopMiniPlayer(
                song = song,
                isPlaying = isPlaying,
                onExpand = onExpand,
                onPlayPause = onPlayPause,
                onNext = onNext,
            )
        }
        DesktopFloatingNavigation(destination, onDestinationSelected)
    }
}

@Composable
internal fun DesktopFloatingNavigation(
    destination: DesktopDestination,
    onDestinationSelected: (DesktopDestination) -> Unit,
) {
    val tabs = listOf(
        DesktopDestination.LISTEN_NOW to (MaviMusicIcons.Home to DesktopStrings["home", "Inicio"]),
        DesktopDestination.EXPLORE to (MaviMusicIcons.Explore to DesktopStrings["explore", "Explorar"]),
        DesktopDestination.LIBRARY to (MaviMusicIcons.Library to DesktopStrings["library", "Biblioteca"]),
        DesktopDestination.SEARCH to (MaviMusicIcons.Search to DesktopStrings["search", "Buscar"]),
    )
    val selected = tabs.firstOrNull { it.first == destination }?.first ?: DesktopDestination.LIBRARY
    val shape = RoundedCornerShape(percent = 50)
    Surface(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .clip(shape)
            .desktopBarGlass(shape),
        shape = shape,
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEach { (item, iconAndLabel) ->
                val isSelected = item == selected
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (isSelected) DesktopAccent.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onDestinationSelected(item) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(iconAndLabel.first, iconAndLabel.second, tint = if (isSelected) DesktopAccent else DesktopSecondary, modifier = Modifier.size(20.dp))
                    Text(iconAndLabel.second, color = if (isSelected) Color.White else DesktopSecondary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
internal fun DesktopMiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val tealTop = Color(0xFF09414B)
    val tealBottom = Color(0xFF052B32)
    val backgroundBrush = Brush.verticalGradient(listOf(tealTop, tealBottom))
    val shape = RoundedCornerShape(14.dp)
    val mutedTeal = Color(0xFF8BA5A8)
    Surface(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .clickable(onClick = onExpand),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Row(
            Modifier
                .background(backgroundBrush)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopArtwork(song.thumbnailUrl, Modifier.size(42.dp).clip(RoundedCornerShape(8.dp)), px = ROW_ART_PX)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(song.artist, color = mutedTeal, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isPlaying) "Pausar" else "Reproducir", tint = Color.White)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Rounded.FastForward, DesktopStrings["widget_next", "Siguiente"], tint = Color.White)
            }
        }
    }
}

@Composable
internal fun DesktopSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChanged: (Boolean) -> Unit = {},
    placeholder: String = DesktopStrings["search", "Buscar"],
) {
    val searchShape = RoundedCornerShape(7.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(searchShape)
            .background(Color.White.copy(alpha = 0.07f))
            .border(1.dp, Color.White.copy(alpha = 0.10f), searchShape)
            .clickable { focusRequester.requestFocus() }
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    DesktopSpotlightState.isSearchFieldFocused = it.isFocused
                    onFocusChanged(it.isFocused)
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                        onSearch()
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
            cursorBrush = SolidColor(DesktopAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        MaviMusicIcons.Search,
                        contentDescription = null,
                        tint = DesktopSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isBlank()) {
                            Text(placeholder, color = DesktopSecondary, maxLines = 1)
                        }
                        innerTextField()
                    }
                }
            },
        )
    }
}

/**
 * Brand wordmark "MaviMusic" with slight glass transparency and a continuous
 * horizontal fire shimmer/ember glow, keeping crisp legibility at all times.
 */
@Composable
internal fun MaviMusicFireWordmark() {
    val transition = rememberInfiniteTransition(label = "mavi_fire_shimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fire_phase",
    )

    val shift = phase * 260f
    val fireBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFD54F).copy(alpha = 0.93f),
            Color(0xFFFF7043).copy(alpha = 0.90f),
            Color(0xFFFF3D00).copy(alpha = 0.92f),
            Color(0xFFFFF8E1).copy(alpha = 0.97f),
            Color(0xFFFFAB00).copy(alpha = 0.93f),
            Color(0xFFFF5722).copy(alpha = 0.91f),
            Color(0xFFFFD54F).copy(alpha = 0.93f),
        ),
        start = Offset(shift - 180f, 0f),
        end = Offset(shift + 80f, 4f),
    )

    val auraBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0x00FF3D00),
            Color(0x2EFF5722),
            Color(0x38FFAB00),
            Color(0x2EFF3D00),
            Color(0x00FF3D00),
        ),
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(auraBrush)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MaviMusic",
            style = TextStyle(
                brush = fireBrush,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                shadow = Shadow(
                    color = Color(0xCCFF3D00),
                    offset = Offset(0f, 0f),
                    blurRadius = 11f,
                ),
            ),
            maxLines = 1,
        )
    }
}


