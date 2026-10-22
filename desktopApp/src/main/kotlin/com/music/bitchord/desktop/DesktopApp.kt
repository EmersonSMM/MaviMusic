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

/**
 * The margin every page keeps from the window's edge.
 *
 * Desktop-only pages share Library's edge inset so switching destinations does not move the page
 * content sideways.
 */
internal val DesktopPageGutter = PAGE_GUTTER

internal val DesktopBackground = Color(0xFF202020)
internal val DesktopSurface = Color(0xFF0D0D0F)
internal val DesktopSurfaceRaised = Color(0xFF1C1C1E)
internal val DesktopGlass = Color(0x661C1C1E)
internal val DesktopGlassStrong = Color(0xCC0D0D0F)

/** The floating bars in the narrow layout. */
private val DesktopBarGlass = Color(0xB324242A)

/** The backdrop the floating bars blur. */
private val LocalDesktopHaze = staticCompositionLocalOf<HazeState?> { null }

/** How much a page's scrollable should keep clear of the floating bars. */
internal val LocalDesktopBottomInset = staticCompositionLocalOf { 0.dp }

/** Android's own edge on these bars: `Color.White.copy(alpha = 0.10f)`. */
private val DesktopBarEdge = Color(0x1AFFFFFF)
internal val DesktopAccent = Color.White

/** Deleting, signing out, and anything that failed — the one place a colour survives, as on Android. */
internal val DesktopDestructive = Color(0xFFFF453A)

/** A chip the way Android draws a chosen segment: solid white with black on it, or nothing at all. */
@Composable
internal fun desktopChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = DesktopSecondary,
    selectedContainerColor = Color.White,
    selectedLabelColor = Color.Black,
)

/** Android's switch: a white track with a black knob on, a faint wash off. */
@Composable
internal fun desktopSwitchColors() = SwitchDefaults.colors(
    checkedTrackColor = Color.White,
    checkedThumbColor = Color.Black,
    checkedBorderColor = Color.Transparent,
    uncheckedTrackColor = Color.White.copy(alpha = 0.10f),
    uncheckedThumbColor = Color.White.copy(alpha = 0.35f),
    uncheckedBorderColor = Color.Transparent,
)
internal val DesktopSecondary = Color(0xFF8E8E93)
internal val DesktopDivider = Color(0xFF2C2C2E)
private const val STATS_SAMPLE_MS = 5_000L

/** How often the playhead is offered to the history tracker. */
private const val HISTORY_SAMPLE_MS = 5_000L

/** How long the search field is left alone before its typeahead is asked for. */
private const val SUGGESTION_DEBOUNCE_MS = 220L

/** How many tracks a station is built with when one is started explicitly. */
/** A search costs more than a completion, so the live rows wait for a longer pause. */
private const val TYPEAHEAD_MEDIA_DEBOUNCE_MS = 400L

/** How many playable rows the dropdown shows, matching Android's own limit. */
private const val TYPEAHEAD_MEDIA_LIMIT = 8

private const val INITIAL_RADIO_TRACKS = 24

/** An artist's top songs, capped so the release shelves are not buried. */
internal const val MAX_ARTIST_SONGS = 20
internal const val ARTIST_SONGS_PER_COLUMN = 4

/** The artist photograph's height. */
internal val ARTIST_BANNER_HEIGHT = 340.dp

/** The library shelf the account's own playlists arrive on, and the one edits are spliced into. */
private const val PLAYLISTS_SHELF = "Playlists"
private const val STATS_MAX_DELTA_MS = 15_000L
private const val SCROBBLE_THRESHOLD_MS = 180_000L

/** Used for tracking restart of the current song when previous button is pressed. */
private const val BACK_RESTARTS_AFTER_MS = 10_000L

/** How long a manual skip takes to blend into the next track when Automix on skip is on. */
private const val SKIP_MIX_ON_SKIP_SECONDS = 15

/** How long the supplier waits for the server to echo an AutoPlay queue addition. */
private const val PARTY_AUTOPLAY_ECHO_TIMEOUT_MS = 5_000L

/** The phone gives one empty station response a delayed second chance. */
private const val AUTOPLAY_EMPTY_REFRESH_DELAY_MS = 2_000L

/** The app's dark scheme, supporting customizable background tones and themes. */
internal fun desktopColorScheme(theme: DesktopThemeTone = DesktopThemeTone.DARK_CLASSIC) = darkColorScheme(
    primary = DesktopAccent,
    // Black on the accent, now that the accent is white.
    onPrimary = Color.Black,
    primaryContainer = Color.White.copy(alpha = 0.16f),
    onPrimaryContainer = Color.White,
    background = theme.background,
    onBackground = Color.White,
    surface = theme.surface,
    onSurface = Color.White,
    surfaceVariant = theme.surfaceRaised,
    onSurfaceVariant = DesktopSecondary,
    outline = Color(0xFF2C2C2E),
    outlineVariant = DesktopDivider,
    surfaceTint = DesktopAccent,
)

@Composable
internal fun desktopTypography(): Typography {
    val sfProDisplay = FontFamily(
        composeFont(Res.font.sf_pro_display_regular, FontWeight.W400),
        composeFont(Res.font.sf_pro_display_medium, FontWeight.W500),
        composeFont(Res.font.sf_pro_display_semibold, FontWeight.W600),
        composeFont(Res.font.sf_pro_display_bold, FontWeight.W700),
        composeFont(Res.font.sf_pro_display_heavy, FontWeight.W800),
    )
    val defaults = Typography()
    return defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 36.sp, letterSpacing = (-0.8).sp),
        displayMedium = defaults.displayMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 30.sp),
        displaySmall = defaults.displaySmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 26.sp),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 24.sp, letterSpacing = (-0.4).sp),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 20.sp),
        titleLarge = defaults.titleLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W700, fontSize = 21.sp, letterSpacing = (-0.3).sp),
        titleMedium = defaults.titleMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 17.sp, letterSpacing = (-0.2).sp),
        titleSmall = defaults.titleSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 15.sp),
        bodyLarge = defaults.bodyLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 16.sp),
        bodyMedium = defaults.bodyMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 15.sp),
        bodySmall = defaults.bodySmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W400, fontSize = 13.sp),
        labelLarge = defaults.labelLarge.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 14.sp),
        labelMedium = defaults.labelMedium.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W600, fontSize = 13.sp),
        labelSmall = defaults.labelSmall.copy(fontFamily = sfProDisplay, fontWeight = FontWeight.W500, fontSize = 12.sp),
    )
}

internal enum class DesktopDestination(val id: String, val label: String) {
    LISTEN_NOW("listen-now", "Inicio"),
    EXPLORE("explore", "Explorar"),
    LIBRARY("library", "Biblioteca"),
    SEARCH("search", "Buscar"),
    HISTORY("history", "Historial"),
    DOWNLOADS("downloads", "Descargas"),
    LOCAL_MUSIC("local-music", "Música local"),
    SETTINGS("settings", "Ajustes"),
}

internal enum class DesktopRepeatMode {
    OFF,
    ALL,
    ONE,
    ;

    fun next(): DesktopRepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    fun label(): String = when (this) {
        OFF -> "Off"
        ALL -> "All"
        ONE -> "One"
    }
}

@Composable
fun BitChordDesktopApp() {
    val scope = rememberCoroutineScope()
    // Filled once the audio engine exists. Local playback helpers use this single hook so every
    // transport and queue gesture reaches Listen Together without duplicating protocol logic.
    val partySyncHolder = remember { arrayOfNulls<DesktopPartySync>(1) }
    val partyState by DesktopListenTogether.state.collectAsState()
    var personalQueueStash by remember { mutableStateOf<DesktopQueue?>(null) }
    var personalPositionStash by remember { mutableStateOf(0L) }
    var personalPlayingStash by remember { mutableStateOf(false) }
    val persistence = remember { DesktopPersistence() }
    var destination by remember { mutableStateOf(DesktopDestination.LISTEN_NOW) }
    var query by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter.ALL) }
    var searchRows by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    // What has been searched before, and what YouTube thinks is being typed.
    val searchHistory by DesktopSearchHistory.recent.collectAsState()
    // Whether anything has been searched for since the field was last emptied: until then the
    // page shows recent searches rather than a result set.
    var searchCommitted by remember { mutableStateOf(false) }
    var searchScrollReset by remember { mutableStateOf(0) }
    var searchFocusRequested by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    // Playable rows for the half-typed query, shown under the text completions.
    var searchTypeahead by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    // False while the query is being set *by* the app.
    var searchTyping by remember { mutableStateOf(false) }
    var searchLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var homeState by remember { mutableStateOf<UiState<List<HomeShelf>>>(UiState.Loading) }
    // The home feed is paged: `FEmusic_home` answers with a couple of shelves and a token, and
    // everything else arrives by following it.
    var homeContinuation by remember { mutableStateOf<String?>(null) }
    var homeLoadingMore by remember { mutableStateOf(false) }
    // Recently Played has its own request and its own skeleton at the head of the page, as on the
    // phone; it is only asked for with an account to have played on.
    var homeRecentsLoading by remember { mutableStateOf(false) }
    // Held out here so a page keeps its place when another is visited and it is come back to.
    val homeListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()
    val moodGenreListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val libraryGridState = rememberLazyGridState()
    /** A Library shelf's "Show all", open as a grid in place of the page. */
    var libraryShowAll by remember { mutableStateOf<HomeShelf?>(null) }
    /** What the Spotify grid is waiting on, or why it is empty; null once its playlists are in. */
    var spotifyLibraryStatus by remember { mutableStateOf<String?>(null) }
    var spotifyImportOpen by remember { mutableStateOf(false) }
    /** Where the one back button in the top bar goes: every place visited, oldest first. */
    val navHistory = remember { mutableStateListOf<DesktopNavEntry>() }
    /** Set while [goBack] puts a place back, so that move is not itself recorded as a visit. */
    val navRestoring = remember { booleanArrayOf(false) }
    /** Opens a song's menu at the pointer; set by the pages' menu host once it is in place. */
    var pageMenu by remember { mutableStateOf<(Song) -> Unit>({}) }
    val homeSeenTitles = remember { HashSet<String>() }
    var homeGeneration by remember { mutableStateOf(0) }
    var homeIdentity by remember { mutableStateOf<String?>(null) }
    var exploreState by remember { mutableStateOf<UiState<List<MoodGenreSection>>>(UiState.Loading) }
    var selectedMoodGenre by remember { mutableStateOf<MoodGenre?>(null) }
    // Bumped by a retry so the loaders below re-run without the state they are keyed on having to
    // change to something and back again.
    var exploreReloads by remember { mutableStateOf(0) }
    var moodGenreReloads by remember { mutableStateOf(0) }
    var moodGenreShelves by remember { mutableStateOf<UiState<List<HomeShelf>>>(UiState.Loading) }
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    // The live queue: what is playing, what played before it, what is next.
    var liveQueue by remember {
        mutableStateOf(
            DesktopQueue.restored(
                persistence.queue(),
                persistence.string("queue_index", "0").toIntOrNull() ?: 0,
            ),
        )
    }
    // The order the queue was in before shuffle rearranged it, so the toggle can be undone.
    var preShuffleOrder by remember { mutableStateOf<List<String>>(emptyList()) }
    // One party AutoPlay top-up at a time; see topUpPartyAutoplay.
    val partyAutoplayLock = remember { Mutex() }
    val queue = liveQueue.songs
    // What was played on this computer. The account's own history replaces it
    // while signed in — see [remoteHistory] — because that is what Android's
    // History screen is and what makes a phone and a desktop agree.
    var history by remember { mutableStateOf(persistence.history()) }
    var remoteHistory by remember { mutableStateOf<List<Song>>(emptyList()) }
    var likedIds by remember { mutableStateOf(persistence.likedIds()) }
    // In-flight tail of the Liked Music chain; replaced whenever the library is fetched again.
    var likedSyncJob by remember { mutableStateOf<Job?>(null) }
    var dislikedIds by remember { mutableStateOf(persistence.dislikedIds()) }
    val overlays = remember { DesktopOverlays() }
    var downloads by remember { mutableStateOf(persistence.downloads()) }
    var localSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val downloadQueue by DesktopDownloadQueue.active.collectAsState()
    val downloadInProgress = downloadQueue.keys
    var playlists by remember { mutableStateOf(persistence.playlists()) }
    var statsRevision by remember { mutableStateOf(0L) }
    var replayPeriod by remember { mutableStateOf(DesktopReplayPeriod.ALL_TIME) }
    val replayHolder = remember { DesktopAccounts.active()?.name.orEmpty() }
    var replaySummary by remember { mutableStateOf(DesktopListeningStats.summary()) }
    var statsCountedSongs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    // One busy flag and one message for every playlist edit: only one of them can be in flight,
    // because each is driven by a dialog that is modal.
    var playlistBusy by remember { mutableStateOf(false) }
    var playlistError by remember { mutableStateOf<String?>(null) }
    // The album and artist pages the playing track belongs to, looked up while
    // the player is open. Whatever started a track knew its title and artwork
    // but rarely its ids — a home-feed tile carries neither — so without this
    // the credit under the title is not a link at all. Android does the same
    // thing, and for the same reason; see its `links` in MainActivity.
    var trackLinks by remember { mutableStateOf<Song?>(null) }
    // Settings is a modal over whatever page is open, the way Music puts its own preferences in a
    // sheet rather than a navigation destination.
    var autoplay by remember { mutableStateOf(persistence.boolean("autoplay", true)) }
    var dontRepeatSuggestions by remember {
        mutableStateOf(persistence.boolean(KEY_DONT_REPEAT_SUGGESTIONS, false))
    }
    /** Everything AutoPlay has played or offered this run — see [KEY_DONT_REPEAT_SUGGESTIONS]. */
    val sessionSongHistory = remember { mutableListOf<Song>() }
    var filterNonMusicAudio by remember {
        mutableStateOf(persistence.boolean(DesktopLocalMusic.KEY_FILTER_NON_MUSIC_AUDIO, false))
    }
    var localMusicRevision by remember { mutableStateOf(0) }
    // The track the queued station was built around, so a top-up is asked for once per song rather
    // than once per recomposition.
    var autoplaySeed by remember { mutableStateOf<String?>(null) }
    var autoplayJob by remember { mutableStateOf<Job?>(null) }
    var automix by remember { mutableStateOf(persistence.boolean("automix", false)) }
    // A manual skip blends into the already-prepared next track over ~15s instead of cutting.
    var automixOnSkip by remember { mutableStateOf(persistence.boolean("automix_on_skip", false)) }
    // Apple-style Automix phrase length in bars: 0 = adaptive (default), 8 or 16 = fixed.
    var automixPhraseBars by remember {
        mutableStateOf(persistence.string("automix_phrase_bars", "0").toIntOrNull()?.let { if (it == 8 || it == 16) it else 0 } ?: 0)
    }
    var automixPerformance by remember {
        mutableStateOf(
            runCatching { AutomixPerformanceMode.valueOf(persistence.string("automix_performance", "BALANCED")) }
                .getOrDefault(AutomixPerformanceMode.BALANCED),
        )
    }
    var shuffle by remember { mutableStateOf(persistence.boolean("shuffle", false)) }
    var repeatMode by remember {
        mutableStateOf(
            runCatching { DesktopRepeatMode.valueOf(persistence.string("repeat_mode", "OFF")) }
                .getOrDefault(DesktopRepeatMode.OFF),
        )
    }
    var playbackSpeed by remember { mutableStateOf(persistence.string("playback_speed", "1.0").toFloatOrNull() ?: 1.0f) }
    var volume by remember { mutableStateOf(persistence.string("volume", "1.0").toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 1.0f) }
    var crossfadeSeconds by remember { mutableStateOf(persistence.string("crossfade_seconds", "0").toIntOrNull()?.coerceIn(0, 12) ?: 0) }
    var downloadQuality by remember { mutableStateOf(persistence.string("download_quality", "LOSSLESS")) }
    var trayIconEnabled by remember { mutableStateOf(persistence.boolean("tray_icon", true)) }
    var closeToTray by remember { mutableStateOf(persistence.boolean("close_to_tray", true)) }
    // The three the FFmpeg engine unlocked: none of them could exist while JavaFX owned the decode,
    // because none of them can be done without the samples themselves.
    var spatialAudio by remember { mutableStateOf(persistence.boolean("spatial_audio", false)) }
    var dolbyAtmos by remember { mutableStateOf(persistence.boolean("dolby_atmos", true)) }
    var skipSilence by remember { mutableStateOf(persistence.boolean("skip_silence", false)) }
    var volumeNormalization by remember { mutableStateOf(persistence.boolean("volume_normalization", true)) }
    var outputPrecision by remember { mutableStateOf(persistence.string("output_precision", "PCM_16")) }
    // The blob backdrop the mesh replaced, kept as an opt-out.
    var legacyMeshGradient by remember { mutableStateOf(persistence.boolean("legacy_mesh_gradient", false)) }
    var animatedCanvas by remember { mutableStateOf(persistence.boolean("animated_canvas", true)) }
    var spotifyCanvasCookie by remember { mutableStateOf(DesktopSpotifyToken.cookie()) }
    // Held here rather than by the Integrations page, so leaving the page does not close the
    // sign-in window.
    var spotifySignInJob by remember { mutableStateOf<Job?>(null) }
    var spotifySignInError by remember { mutableStateOf<String?>(null) }
    var showNerdStats by remember { mutableStateOf(persistence.boolean("show_nerd_stats", false)) }
    var syncedLyrics by remember {
        mutableStateOf(persistence.boolean(DesktopLyricsClient.KEY_SYNCED_LYRICS, true))
    }
    var lyricsBlur by remember {
        mutableStateOf(persistence.boolean(DesktopLyricsClient.KEY_LYRICS_BLUR, true))
    }
    var prioritizeSyllables by remember {
        mutableStateOf(persistence.boolean(DesktopLyricsClient.KEY_PRIORITIZE_SYLLABLES, false))
    }
    var lyricsOrder by remember { mutableStateOf(persistence.lyricsSourceOrder()) }
    var lyricsOn by remember { mutableStateOf(persistence.lyricsEnabledSources()) }
    var fullBleedArtwork by remember { mutableStateOf(persistence.boolean("full_bleed_artwork", false)) }
    var accounts by remember { mutableStateOf(DesktopAccounts.accounts()) }
    var activeAccountId by remember { mutableStateOf(DesktopAccounts.activeAccountId()) }
    var activeProfileId by remember { mutableStateOf(DesktopAccounts.activeProfileId()) }
    var signInBusy by remember { mutableStateOf<String?>(null) }
    var signInError by remember { mutableStateOf<String?>(null) }
    var webSignInJob by remember { mutableStateOf<Job?>(null) }
    val activeAccount = accounts.firstOrNull { it.accountId == activeAccountId } ?: accounts.firstOrNull()
    // The account's own library, fetched once a session is in force.
    var libraryState by remember { mutableStateOf<UiState<LibraryPage>>(UiState.Loading) }
    // Whether YouTube is actually accepting the session, which is not the same as having an account
    // saved.
    var youtubeSignedIn by remember { mutableStateOf(false) }
    // Set by an edit that the library feed has not caught up with yet.
    var libraryStale by remember { mutableStateOf(false) }
    // Bumped when the session changes underneath, so the loader below re-runs without anything it
    // is keyed on having to change to something and back.
    var sessionRevision by remember { mutableStateOf(0) }
    // Read off the library rather than fetched again: the Playlists shelf is the same feed the
    // picker wants, minus the entries no edit can be aimed at.
    val accountPlaylists = remember(libraryState) {
        (libraryState as? UiState.Success)?.data?.shelves
            ?.firstOrNull { it.title == PLAYLISTS_SHELF }
            ?.items
            ?.let(DesktopSearchClient::userPlaylists)
            .orEmpty()
    }
    // The add-to-playlist dialog intentionally keeps only playlists owned by the account. The
    // sidebar is a library browser instead, so it shows the complete paged shelf: owned, saved,
    // and YouTube's built-in playlists such as Liked Music.
    val sidebarAccountPlaylists = remember(libraryState) {
        (libraryState as? UiState.Success)?.data?.shelves
            ?.firstOrNull { it.title == PLAYLISTS_SHELF }
            ?.items
            ?.filter { it.browseId != null }
            .orEmpty()
    }

    fun reloadLibrary() {
        libraryState = UiState.Loading
        sessionRevision++
    }

    /** Edits the Playlists shelf in place, without re-fetching it. */
    fun editPlaylistShelf(edit: (List<ShelfItem>) -> List<ShelfItem>) {
        val page = (libraryState as? UiState.Success)?.data ?: return
        val index = page.shelves.indexOfFirst { it.title == PLAYLISTS_SHELF }
        val shelves = if (index >= 0) {
            page.shelves.toMutableList().apply {
                this[index] = this[index].copy(items = edit(this[index].items))
            }
        } else {
            // A fresh account has no Playlists shelf at all, and it is exactly the account most
            // likely to be making its first one.
            val items = edit(emptyList())
            if (items.isEmpty()) return
            listOf(HomeShelf(PLAYLISTS_SHELF, items)) + page.shelves
        }
        libraryState = UiState.Success(page.copy(shelves = shelves.toList()))
    }

    // Whoever was signed in last is signed in again, before anything asks YouTube for something
    // that depends on it.
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { DesktopAccounts.activate() }
        sessionRevision++
    }
    var audioQuality by remember { mutableStateOf(persistence.audioQuality()) }
    var sourceConfigs by remember { mutableStateOf(persistence.sourceConfigs()) }
    var sourceStatus by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val youtubeSourceEnabled = sourceConfigs.any {
        it.kind == DesktopSourceKind.YOUTUBE && it.enabled
    }
    val sleepTimerMinutes by DesktopSleepTimer.minutes.collectAsState()
    val sleepAfterTrack by DesktopSleepTimer.afterTrack.collectAsState()
    var sleepTimerTick by remember { mutableStateOf(0L) }
    LaunchedEffect(sleepTimerMinutes) {
        while (sleepTimerMinutes != null && currentCoroutineContext().isActive) {
            delay(1_000L)
            sleepTimerTick++
        }
    }
    val sleepRemainingMs = remember(sleepTimerMinutes, sleepTimerTick) {
        DesktopSleepTimer.remainingMs()
    }
    var openedCollection by remember { mutableStateOf<DesktopCollection?>(null) }
    var collectionLoadingMore by remember { mutableStateOf(false) }
    var collectionError by remember { mutableStateOf<String?>(null) }
    var collectionReloads by remember { mutableStateOf(0) }
    // The artist page is its own destination rather than a collection with a different header.
    var openedArtist by remember { mutableStateOf<DesktopArtistTarget?>(null) }
    var artistState by remember { mutableStateOf<UiState<ArtistPage>>(UiState.Loading) }
    var artistReloads by remember { mutableStateOf(0) }

    fun canonicalSong(song: Song): Song {
        val homeSong = (homeState as? UiState.Success)?.data.orEmpty()
            .asSequence()
            .flatMap { it.items.asSequence() }
            .filter { it.videoId == song.videoId }
            .map(ShelfItem::toSong)
            .firstOrNull()
        return if (homeSong != null && song.artist.isUnknownArtist()) {
            song.copy(
                artist = homeSong.artist,
                durationText = song.durationText ?: homeSong.durationText,
                thumbnailUrl = song.thumbnailUrl ?: homeSong.thumbnailUrl,
            )
        } else {
            song
        }
    }

    lateinit var startSong: (Song, Boolean) -> Unit
    lateinit var playbackEngine: DesktopPlaybackEngine

    fun saveQueue() {
        // A party queue is temporary. The phone keeps the listener's own queue aside and so does
        // desktop; never let a shared running order overwrite the queue restored after leaving or
        // after a process restart.
        if (DesktopListenTogether.state.value.inParty) return
        persistence.saveQueue(liveQueue.songs)
        persistence.saveString("queue_index", liveQueue.index.toString())
    }

    /** Opens whatever the queue is currently pointing at. */
    fun playCurrent(startPlaying: Boolean = true) {
        val song = liveQueue.current ?: return
        selectedSong = song
        history = (listOf(song) + history.filterNot { it.videoId == song.videoId }).take(50)
        saveQueue()
        persistence.saveHistory(history)
        playbackEngine.load(song, startPlaying)
        partySyncHolder[0]?.onLocalIntent(song.videoId)
        scope.launch { DesktopScrobbling.updateNowPlaying(song) }

        // Prefer songs over videos using YouTube Music's track matching engine
        if (song.isVideo) {
            scope.launch {
                runCatching {
                    val resolvedAudio = YtMusicRepository.resolveAudio(song)
                    if (resolvedAudio.videoId != song.videoId && !resolvedAudio.isVideo) {
                        DesktopTrackLog.log("audio prefer: resolved video '${song.title}' to official song '${resolvedAudio.title}' (${resolvedAudio.videoId})")
                        if (selectedSong?.videoId == song.videoId) {
                            selectedSong = resolvedAudio
                            liveQueue = liveQueue.copy(
                                songs = liveQueue.songs.map { if (it.videoId == song.videoId) resolvedAudio else it },
                            )
                            playbackEngine.load(resolvedAudio, playbackEngine.state.value.isPlaying)
                        }
                    }
                }
            }
        }
    }

    fun partyTrackChangeBlocked(): Boolean {
        if (!DesktopListenTogether.state.value.controlsLocked) return false
        DesktopPlayerHost.showMessage("Only the party host can change playback")
        return true
    }

    /**
     * The phone's party queue for a track picked here: that track, then what the party's members
     * queued by hand. Never the album around it, and never the last track's AutoPlay — the party
     * shares one running order, and the phone builds it exactly this way.
     */
    fun partyPlaybackQueue(tapped: Song): List<Song> {
        val party = DesktopListenTogether.state.value
        val upcoming = partyUpcomingAfter(party.queue, party.playback, party.playback.track?.videoId)
        return QueueTimeline.buildPartyPlaybackQueue(
            tapped,
            tapped.queueSource(),
            DesktopQueue.adopt(liveQueue.upcoming, upcoming.map { it.toDesktopSong() }),
        )
    }

    /** A song played on its own — from a search row, a shelf card, history. */
    fun playSong(song: Song, startPlaying: Boolean = true, source: DesktopQueueSource? = null) {
        if (partyTrackChangeBlocked()) return
        val tapped = canonicalSong(song).withSource(source)
        val songs = if (DesktopListenTogether.state.value.inParty) {
            partyPlaybackQueue(tapped)
        } else {
            // The phone's one-off queue: the song, then whatever the listener had queued by hand.
            QueueTimeline.buildOneOffQueue(liveQueue.songs, liveQueue.index, tapped, tapped.queueSource())
        }
        liveQueue = DesktopQueue(songs, index = 0)
        preShuffleOrder = emptyList()
        playCurrent(startPlaying)
    }

    /** Opens the Spotify sign-in window for an `sp_dc`, or closes the one already open. */
    fun signInToSpotify() {
        spotifySignInJob?.let {
            it.cancel()
            return
        }
        spotifySignInError = null
        spotifySignInJob = scope.launch {
            try {
                val cookie = DesktopWebSignIn.captureSpotify()
                DesktopSpotifyToken.setCookie(cookie)
                spotifyCanvasCookie = cookie
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                DesktopTrackLog.log("sign-in: Spotify failed: ${failure.message}")
                spotifySignInError = failure.message ?: "Could not complete the Spotify sign-in."
            } finally {
                spotifySignInJob = null
            }
        }
    }

    /** Takes a captured cookie all the way to a saved account. */
    suspend fun signIn(cookie: String, label: String, sourceProfile: String? = null) {
        signInError = null
        signInBusy = label
        try {
            val scope = withContext(Dispatchers.IO) { DesktopYouTubeSession.adoptSessionScope(cookie) }
            if (scope == null) {
                signInError = "$label is not signed in to YouTube Music."
                return
            }
            DesktopYouTubeAuth.adopt(scope)
            // Asked with the session in force, so they describe the account that was just captured
            // rather than nobody in particular.
            val details = DesktopSearchClient.accountMenu().getOrNull()?.let(DesktopAccountParser::account)
            val channels = DesktopSearchClient.accountsList().getOrNull()
                ?.let(DesktopAccountParser::channels)
                .orEmpty()
            // Off the UI thread: saving reaches the platform keyring over D-Bus, and a blocking
            // call to another process is not something to do on the thread that is drawing.
            withContext(Dispatchers.IO) { DesktopAccounts.save(cookie, scope, details, channels, sourceProfile) }
            accounts = withContext(Dispatchers.IO) { DesktopAccounts.accounts() }
            activeAccountId = DesktopAccounts.activeAccountId()
            activeProfileId = DesktopAccounts.activeProfileId()
            overlays.signIn = false
            overlays.accounts = false
        } finally {
            signInBusy = null
        }
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, source: DesktopQueueSource? = null) {
        // A Spotify playlist's rows still being matched to YouTube Music, or with no match, have
        // nothing to play: they stay out of the queue, and the pick moves to the next playable row.
        val resolved = songs.withIndex().filterNot { it.value.isUnresolvedSpotify }
        if (resolved.isEmpty()) return
        if (partyTrackChangeBlocked()) return
        val playable = resolved.map { canonicalSong(it.value).withSource(source) }
        // Past the end still means the last row, as the clamp here always did.
        val at = resolved.indexOfFirst { it.index >= startIndex }.takeIf { it >= 0 } ?: resolved.lastIndex
        if (DesktopListenTogether.state.value.inParty) {
            liveQueue = DesktopQueue(partyPlaybackQueue(playable[at]), index = 0)
            preShuffleOrder = emptyList()
            playCurrent()
            return
        }
        // The phone's context queue: the album up to the pick, the pick, what the listener had
        // queued by hand, then the rest of the album.
        val built = QueueTimeline.buildContextQueue(
            currentTimeline = liveQueue.songs,
            currentIndex = liveQueue.index,
            newContextSongs = playable,
            selectedIndex = at,
            contextSource = playable[at].queueSource(),
        )
        liveQueue = if (shuffle) {
            DesktopQueue.shuffledStartingAt(built.timeline, built.startIndex)
        } else {
            // Keep the complete queue so Previous can navigate to the tracks before the selected one.
            DesktopQueue.startingAt(built.timeline, built.startIndex)
        }
        preShuffleOrder = if (shuffle) built.timeline.map(DesktopQueue::orderKey) else emptyList()
        playCurrent()
    }

    /** Moves within the queue, dropping whatever a forward jump passed over. */
    fun playQueueIndex(target: Int) {
        if (target !in liveQueue.songs.indices) return
        if (partyTrackChangeBlocked()) return
        liveQueue = liveQueue.jumpTo(target)
        playCurrent()
    }

    fun cycleSleepTimer() {
        when {
            sleepAfterTrack -> DesktopSleepTimer.cancel()
            sleepTimerMinutes == null -> DesktopSleepTimer.start(DesktopSleepTimer.presets.first())
            sleepTimerMinutes == DesktopSleepTimer.presets.last() -> DesktopSleepTimer.startAfterTrack()
            else -> {
                val current = DesktopSleepTimer.presets.indexOf(sleepTimerMinutes)
                DesktopSleepTimer.start(DesktopSleepTimer.presets.getOrElse(current + 1) { DesktopSleepTimer.presets.last() })
            }
        }
    }
    startSong = ::playSong

    /** Applies a rating locally and carries it to the account. */
    fun rate(song: Song, status: LikeStatus) {
        likedIds = if (status == LikeStatus.LIKE) likedIds + song.videoId else likedIds - song.videoId
        dislikedIds = if (status == LikeStatus.DISLIKE) {
            dislikedIds + song.videoId
        } else {
            dislikedIds - song.videoId
        }
        persistence.saveLikedIds(likedIds)
        persistence.saveDislikedIds(dislikedIds)
        // Keep live library liked songs list in sync immediately
        (libraryState as? UiState.Success)?.data?.let { page ->
            val updatedLiked = if (status == LikeStatus.LIKE) {
                listOf(song) + page.likedSongs.filterNot { it.videoId == song.videoId }
            } else {
                page.likedSongs.filterNot { it.videoId == song.videoId }
            }
            libraryState = UiState.Success(page.copy(likedSongs = updatedLiked))
        }
        scope.launch {
            DesktopSearchClient.rate(song.videoId, status)
                .onSuccess { DesktopTrackLog.log("youtube: rated ${song.videoId} as $status successfully") }
                .onFailure { DesktopTrackLog.log("youtube: rating ${song.videoId} failed: ${it.message}") }
        }
    }

    fun toggleLike(song: Song) =
        rate(song, if (song.videoId in likedIds) LikeStatus.INDIFFERENT else LikeStatus.LIKE)

    fun toggleDislike(song: Song) =
        rate(song, if (song.videoId in dislikedIds) LikeStatus.INDIFFERENT else LikeStatus.DISLIKE)

    /** The origin the live queue is carrying, for anything appended to it. */
    fun currentQueueSource(): DesktopQueueSource? = liveQueue.current?.playbackSource?.let {
        DesktopQueueSource(
            it,
            liveQueue.current?.playbackSourceType ?: PlaybackSourceType.QUEUE,
            liveQueue.current?.playbackSourceId,
        )
    }

    /**
     * Queues a track by hand, into the listener's own section — "Play next" at its head, "Add to
     * queue" at its end, above the album and AutoPlay either way. In a party, held to the same
     * twenty-five upcoming songs the phone and the server allow.
     */
    fun enqueue(song: Song, next: Boolean) {
        val party = DesktopListenTogether.state.value
        if (party.controlsLocked) return
        if (party.inParty && liveQueue.upcoming.size >= DesktopPartySync.MAX_PARTY_UPCOMING_QUEUE) {
            DesktopPlayerHost.showMessage(
                "Queue is full (maximum ${DesktopPartySync.MAX_PARTY_UPCOMING_QUEUE} songs in party)",
            )
            return
        }
        val queued = canonicalSong(song).copy(radioName = liveQueue.current?.radioName)
            .withSource(currentQueueSource())
        liveQueue = liveQueue.enqueue(queued, playNext = next)
        saveQueue()
        partySyncHolder[0]?.onLocalIntent()
    }

    /** Slots a track in right after the one playing. */
    fun playNext(song: Song) = enqueue(song, next = true)

    /** Puts a track at the end of what the listener queued — not the end of the queue. */
    fun addToQueue(song: Song) = enqueue(song, next = false)

    /** Starts the station YouTube Music builds around one track. */
    fun startRadio(song: Song) {
        if (partyTrackChangeBlocked()) return
        val seed = canonicalSong(song).copy(radioName = song.title)
        scope.launch {
            // AutoPlay's section behind the seed, as the phone queues a station — which is also
            // what lets a party share it: a context tail is never published.
            val related = DesktopAutoplay.tracksFor(listOf(seed), seed, INITIAL_RADIO_TRACKS)
                .getOrNull()
                .orEmpty()
            if (related.isEmpty()) {
                DesktopTrackLog.log("radio: nothing to build a station on for '${song.title}'")
                return@launch
            }
            if (partyTrackChangeBlocked()) return@launch
            if (liveQueue.current?.videoId == seed.videoId) {
                liveQueue = DesktopQueue(listOf(liveQueue.current!!) + related, index = 0)
                saveQueue()
                partySyncHolder[0]?.onLocalIntent()
            } else {
                val station = listOf(seed.asQueueEntry(QueueTier.CONTEXT)) + related
                liveQueue = if (shuffle) DesktopQueue.shuffledStartingAt(station, 0) else DesktopQueue(station, 0)
                preShuffleOrder = if (shuffle) station.map(DesktopQueue::orderKey) else emptyList()
                playCurrent()
            }
            DesktopTrackLog.log("radio: started a station on '${song.title}' with ${related.size} tracks")
        }
    }

    /**
     * Opens an album or playlist in place of whatever page was open. The page it replaces is kept
     * in the history, not underneath: an artist page left standing under an album would be drawn
     * over it, which is how an album opened from an artist once went nowhere.
     */
    fun showCollection(collection: DesktopCollection) {
        openedArtist = null
        overlays.replay = false
        overlays.settingsPage = null
        collectionError = null
        openedCollection = collection
    }

    /**
     * Opens an album or playlist by id straight away, with a loader where the tracks go, the way an
     * artist's page opens — rather than leaving the click with nothing to show until YouTube answers.
     * [fallback] is what the card that was clicked already knew.
     */
    fun openCollection(browseId: String, fallback: BrowseItem? = null) {
        val current = openedCollection
        if (current?.browseId == browseId && !current.loading) {
            showCollection(current)
            return
        }
        showCollection(
            DesktopCollection(
                browseId = browseId,
                title = fallback?.title.orEmpty(),
                subtitle = fallback?.subtitle.orEmpty(),
                thumbnailUrl = fallback?.thumbnailUrl,
                type = fallback?.type?.takeIf { it != BrowseType.OTHER } ?: browseTypeOf(browseId),
                songs = emptyList(),
                loading = true,
            ),
        )
        collectionReloads++
    }

    // Keyed on the id, so a second page opened before the first answered cancels the first rather
    // than racing it; and a loading page restored by Back fetches again.
    LaunchedEffect(openedCollection?.browseId, openedCollection?.loading, collectionReloads) {
        val pending = openedCollection
            ?.takeIf { it.loading && !it.browseId.startsWith(SPOTIFY_PAGE_PREFIX) }
            ?: return@LaunchedEffect
        collectionError = null
        DesktopSearchClient.browse(
            browseId = pending.browseId,
            fallback = BrowseItem(
                browseId = pending.browseId,
                title = pending.title,
                subtitle = pending.subtitle,
                thumbnailUrl = pending.thumbnailUrl,
                type = pending.type,
            ),
        ).fold(
            onSuccess = { loaded ->
                if (openedCollection?.browseId == pending.browseId) openedCollection = loaded
            },
            onFailure = { collectionError = it.message ?: "Could not open ${pending.title.ifBlank { "this page" }}" },
        )
    }

    // A Spotify playlist: its tracks, then each one's YouTube Music match. Keyed like the browse
    // above, so opening another page cancels it, and Back to a page left half matched (or Retry
    // on one that failed) reads it again.
    LaunchedEffect(openedCollection?.browseId, collectionReloads) {
        val page = openedCollection?.takeIf { it.browseId.startsWith(SPOTIFY_PAGE_PREFIX) }
            ?: return@LaunchedEffect
        if (!page.loading && page.songs.none { it.isMatchPending }) return@LaunchedEffect
        collectionError = null
        fun update(change: (DesktopCollection) -> DesktopCollection) {
            openedCollection?.takeIf { it.browseId == page.browseId }?.let { openedCollection = change(it) }
        }
        runCatching { loadSpotifyPlaylist(page.browseId.removePrefix(SPOTIFY_PAGE_PREFIX), ::update) }
            .onFailure { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                DesktopTrackLog.log("spotify: playlist ${page.title} failed: ${failure.message}")
                collectionError = failure.message ?: "Could not open ${page.title}"
                // The error page stands in for the list only while there is no list.
                update { if (it.songs.isEmpty()) it.copy(loading = true) else it }
            }
    }

    fun openAlbum(browseId: String) = openCollection(browseId)

    /** Copies the track's YouTube Music link. */
    fun shareSong(song: Song) {
        DesktopExternalLinks.copy("https://music.youtube.com/watch?v=${song.videoId}")
        DesktopTrackLog.log("copied a link to '${song.title}'")
    }

    /**
     * The link YouTube Music's own overflow shares for a release, built from the browse id rather
     * than fetched — nothing about it depends on the tracks or the account. Albums and playlists
     * only: an artist's is a channel link, not a release.
     */
    fun shareCollection(collection: DesktopCollection) {
        val id = collection.browseId
        val url = when (collection.type) {
            BrowseType.PLAYLIST -> "https://music.youtube.com/playlist?list=${id.removePrefix("VL")}"
            BrowseType.ALBUM -> "https://music.youtube.com/browse/$id"
            else -> return
        }
        DesktopExternalLinks.copy(url)
        DesktopTrackLog.log("copied a link to '${collection.title}'")
    }

    fun downloadSong(song: Song) {
        // A second tap on something already queued or running calls it off.
        if (song.videoId in downloadInProgress) {
            DesktopDownloadQueue.cancel(song.videoId)
            return
        }
        if (song.videoId in downloads.map(Song::videoId) || song.localPath != null) return
        DesktopDownloadQueue.enqueue(song)
    }

    /** Deletes the file a download saved and forgets the record. */
    fun removeDownload(song: Song) {
        scope.launch {
            withContext(Dispatchers.IO) { DesktopDownloadManager.delete(song) }
            downloads = downloads.filterNot { it.videoId == song.videoId }
            persistence.saveDownloads(downloads)
        }
    }

    /** Everything on a page, in the order it is listed. */
    fun downloadAll(songs: List<Song>) {
        val have = downloads.map(Song::videoId).toSet()
        DesktopDownloadQueue.enqueueAll(
            songs.filterNot { it.videoId in have || it.localPath != null || it.isUnresolvedSpotify },
        )
    }

    /** Keeps a station queued ahead of whatever is playing. */

    /**
     * How many AutoPlay tracks the party already has waiting after [videoId], or null outside a
     * party or while its queue does not hold that track yet.
     */
    fun partyAutoplayWaiting(party: DesktopListenTogether.State, videoId: String): Int? {
        if (!party.inParty) return null
        if (partyQueueIndexOf(party.queue, party.playback, videoId) < 0) return null
        return partyUpcomingAfter(party.queue, party.playback, videoId).count { it.fromAutoplay }
    }

    /**
     * Sends [suggestions] to the party and waits for them to come back; the phone's
     * `topUpPartyAutoplay`.
     *
     * Serialised, and the send-and-wait not cancellable, because a track change cancels the load in
     * flight and starts another: the second used to read the party before the first batch had
     * echoed, find nothing waiting, and send the same station again — every suggestion twice, on
     * every device. Holding the lock until the echo means the next top-up reads a queue that
     * already has these in it, and filters against it.
     */
    suspend fun topUpPartyAutoplay(currentId: String, suggestions: List<Song>): Boolean =
        partyAutoplayLock.withLock {
            withContext(NonCancellable) {
                val party = DesktopListenTogether.state.value
                val waiting = partyUpcomingAfter(party.queue, party.playback, currentId)
                val waitingIds = waiting.mapTo(HashSet()) { it.videoId }
                val room = MAX_QUEUED_AUTOPLAY - waiting.count { it.fromAutoplay }
                val fresh = suggestions.filterNot { it.videoId in waitingIds }.take(room.coerceAtLeast(0))
                if (fresh.isEmpty()) return@withContext true
                DesktopListenTogether.queueAdd(fresh.map { it.toPartyTrack(0L) })
                val added = fresh.first().videoId
                withTimeoutOrNull(PARTY_AUTOPLAY_ECHO_TIMEOUT_MS) {
                    DesktopListenTogether.state.first { state -> state.queue.items.any { it.videoId == added } }
                } != null
            }
        }

    fun loadAutoplaySongs(playFirst: Boolean = false) {
        val current = selectedSong ?: return
        val party = DesktopListenTogether.state.value
        if (!DesktopAutoplay.enabled(party, autoplay) ||
            repeatMode == DesktopRepeatMode.ALL ||
            (party.inParty && DesktopAutoplay.supplierId(party) != party.you?.memberId)
        ) return

        if (dontRepeatSuggestions) sessionSongHistory += current

        // In a party the server's queue is the one being topped up, and this computer's copy of it
        // lags by however long reconcile is held off. Counted from the copy, a batch that had
        // already landed read as missing and the same station went out again.
        val queued = partyAutoplayWaiting(party, current.videoId)
            ?: liveQueue.songs.drop(liveQueue.index + 1).count { it.fromAutoplay }
        val needed = MAX_QUEUED_AUTOPLAY - queued
        if (needed <= 0 && !playFirst) return
        // One request per seed.
        if (!playFirst && autoplaySeed == current.videoId) return
        autoplaySeed = current.videoId

        autoplayJob?.cancel()
        autoplayJob = scope.launch {
            var remaining = needed.coerceAtLeast(1)
            var emptyRefreshesRemaining = 1
            while (isActive) {
                DesktopTrackLog.log("autoplay: building a station from '${current.title}'")
                val at = liveQueue.songs.size
                // What the queue holds — the party's too — plus everything this session has
                // already offered.
                val partyQueue = DesktopListenTogether.state.value
                    .takeIf { it.inParty }?.queue?.items.orEmpty().map { it.toDesktopSong() }
                val suggestions = DesktopAutoplay.tracksFor(
                    existing = if (dontRepeatSuggestions) {
                        liveQueue.songs + partyQueue + sessionSongHistory
                    } else {
                        liveQueue.songs + partyQueue
                    },
                    seedSong = current,
                    limit = remaining,
                ).getOrElse { failure ->
                    DesktopTrackLog.log("autoplay: could not build a station: ${failure.message}")
                    emptyList()
                }
                // The listener may have moved on while the station was being fetched; appending
                // then would attach it to the wrong seed.
                val latestParty = DesktopListenTogether.state.value
                if (selectedSong?.videoId != current.videoId ||
                    !DesktopAutoplay.enabled(latestParty, autoplay) ||
                    (latestParty.inParty && DesktopAutoplay.supplierId(latestParty) != latestParty.you?.memberId)
                ) return@launch
                if (suggestions.isEmpty()) {
                    DesktopTrackLog.log("autoplay: no station came back for '${current.title}'")
                    // Match the phone's bounded recovery: retry once after a short delay, but only
                    // while the current track genuinely has no tail to play next.
                    if (liveQueue.index < 0 || liveQueue.index != liveQueue.songs.lastIndex) return@launch
                    if (emptyRefreshesRemaining-- <= 0) return@launch
                    delay(AUTOPLAY_EMPTY_REFRESH_DELAY_MS)
                    remaining = MAX_QUEUED_AUTOPLAY
                    continue
                }
                // The station carries on from what was playing, so it keeps that queue's origin —
                // Android holds the source on every queue item, not just the ones picked by hand.
                val inherited = current.playbackSource?.let {
                    DesktopQueueSource(it, current.playbackSourceType ?: PlaybackSourceType.QUEUE, current.playbackSourceId)
                }
                val queuedSuggestions = suggestions.map { it.withSource(inherited) }
                if (latestParty.inParty) {
                    // Match the phone: the server owns the party queue. Do not mutate this one
                    // device first; queueAdd is echoed back and all members apply it atomically.
                    val landed = topUpPartyAutoplay(current.videoId, queuedSuggestions)
                    if (!landed) {
                        autoplaySeed = null
                        DesktopTrackLog.log("autoplay: party refused the queue top-up; retry is armed")
                        return@launch
                    }
                    if (playFirst) DesktopListenTogether.next()
                } else {
                    liveQueue = liveQueue.append(queuedSuggestions)
                    saveQueue()
                    // The mix continues from where it was added rather than starting a queue of
                    // its own.
                    if (playFirst) playQueueIndex(at)
                }
                if (dontRepeatSuggestions) sessionSongHistory += suggestions
                DesktopTrackLog.log(
                    "autoplay: queued ${suggestions.size} after '${current.title}'",
                )
                return@launch
            }
        }
    }

    /**
     * Turning AutoPlay on or off, and what that does to the queue.
     *
     * Android runs one path for this — `toggleAutoplayFromNotification` — whether the switch was
     * thrown in the player or anywhere else, and it does more than set a flag. Off drops the
     * suggestions it had already queued, because switching it off is the listener saying they do
     * not want them. On clears the seed first, so the station is fetched again for the track
     * already playing rather than being refused as one this seed has been loaded for.
     */
    fun setAutoplay(enabled: Boolean) {
        if (DesktopListenTogether.state.value.controlsLocked) return
        autoplay = enabled
        persistence.saveBoolean("autoplay", enabled)
        autoplayJob?.cancel()
        autoplayJob = null
        autoplaySeed = null
        if (DesktopListenTogether.state.value.inParty) {
            // As on the phone, the server owns the effective setting and queue while connected.
            // Keep the personal preference for after leaving, but do not trim or top up this one
            // device ahead of the server's state broadcast.
            DesktopListenTogether.setAutoplay(enabled)
            return
        }
        if (enabled) {
            loadAutoplaySongs()
        } else {
            val trimmed = liveQueue.withoutAutoplay()
            if (trimmed !== liveQueue) {
                liveQueue = trimmed
                saveQueue()
            }
        }
    }

    fun playNext() {
        if (DesktopListenTogether.state.value.controlsLocked) return
        // Automix on skip: when enabled and the next deck is already prepared, blend into it over a
        // short crossfade instead of cutting. The blend's own hand-off advances the queue, so a
        // successful mix returns here without touching liveQueue; anything else falls through to the
        // ordinary skip below.
        if (automixOnSkip && repeatMode != DesktopRepeatMode.ONE) {
            val currentId = selectedSong?.videoId
            val upcoming = currentId?.let {
                liveQueue.followingFor(it, repeatAll = repeatMode == DesktopRepeatMode.ALL)
            }
            if (upcoming != null && playbackEngine.beginSkipMix(upcoming.videoId, SKIP_MIX_ON_SKIP_SECONDS)) {
                return
            }
        }
        when {
            liveQueue.hasNext -> {
                var nextQueue = liveQueue.next()
                val current = selectedSong
                if (repeatMode != DesktopRepeatMode.ONE && current != null) {
                    val recentSongs = (listOf(current) + history.take(6)).distinctBy { it.videoId }
                    while (nextQueue.hasNext && nextQueue.current?.let { candidate ->
                        recentSongs.any { com.music.bitchord.playback.QueueBuilder.isSameTrackOrVariant(it, candidate) }
                    } == true) {
                        DesktopTrackLog.log("queue: skipping duplicate recording/variant '${nextQueue.current?.title}' by '${nextQueue.current?.artist}'")
                        nextQueue = nextQueue.next()
                    }
                }
                liveQueue = nextQueue
                playCurrent()
            }
            repeatMode == DesktopRepeatMode.ALL && liveQueue.songs.isNotEmpty() -> {
                liveQueue = liveQueue.copy(index = 0)
                playCurrent()
            }
            DesktopAutoplay.enabled(DesktopListenTogether.state.value, autoplay) ->
                loadAutoplaySongs(playFirst = true)
        }
    }

    fun playPrevious() {
        if (DesktopListenTogether.state.value.controlsLocked) return
        val positionMs = playbackEngine.state.value.positionMs

        // After 10 seconds, Previous restarts the current song.
        if (positionMs > BACK_RESTARTS_AFTER_MS) {
            playbackEngine.seekTo(0L)
            partySyncHolder[0]?.onLocalIntent()
            return
        }

        // Within 10 seconds, Previous goes to the previous song in queue.
        if (!liveQueue.hasPrevious) return
        liveQueue = liveQueue.previous()
        playCurrent()
    }

    /**
     * Shuffle rearranges the queue rather than switching playback to a hidden random order, so what
     * the queue shows stays what plays.
     */
    fun setShuffle(enabled: Boolean) {
        if (DesktopListenTogether.state.value.controlsLocked) return
        shuffle = enabled
        persistence.saveBoolean("shuffle", enabled)
        liveQueue = if (enabled) {
            preShuffleOrder = liveQueue.orderKeys()
            liveQueue.shuffledAhead()
        } else {
            liveQueue.inOrderOf(preShuffleOrder).also { preShuffleOrder = emptyList() }
        }
        saveQueue()
        partySyncHolder[0]?.onLocalIntent()
    }

    fun closePlaylistDialogs() {
        overlays.playlistDialog = false
        playlistTarget = null
        overlays.rename = false
        overlays.delete = false
        overlays.reorder = false
        playlistError = null
        playlistBusy = false
    }

    /** Creates a playlist — on the account when there is one, otherwise here. */
    fun createPlaylist(title: String, privacy: PlaylistPrivacy) {
        val name = title.trim()
        if (name.isBlank()) return
        val seed = playlistTarget
        // A track from another source cannot go in an account playlist, so the playlist made to
        // hold it is one kept here.
        if (!DesktopYouTubeAuth.isSignedIn || (seed != null && !DesktopSearchClient.isVideoId(seed.videoId))) {
            playlists = playlists + DesktopPlaylist(title = name, songs = listOfNotNull(seed))
            persistence.savePlaylists(playlists)
            closePlaylistDialogs()
            return
        }
        playlistBusy = true
        playlistError = null
        scope.launch {
            DesktopSearchClient.createPlaylist(name, privacy, listOfNotNull(seed?.videoId))
                .onSuccess { playlistId ->
                    closePlaylistDialogs()
                    editPlaylistShelf { items ->
                        val card = ShelfItem(
                            title = name,
                            // Only what this request itself establishes: an unseeded playlist gets
                            // a card of just its name rather than a guess at what the feed will
                            // call it.
                            subtitle = if (seed != null) "1 song" else "",
                            thumbnailUrl = seed?.thumbnailUrl,
                            videoId = null,
                            browseId = "VL$playlistId",
                        )
                        // Leads the shelf because it is the newest, which is the order the feed
                        // itself comes in.
                        listOf(card) + items.filterNot { it.browseId == card.browseId }
                    }
                    libraryStale = true
                }
                .onFailure {
                    playlistBusy = false
                    playlistError = it.message ?: "Could not create the playlist"
                }
        }
    }

    fun openPlaylist(playlist: DesktopPlaylist) {
        showCollection(
            DesktopCollection(
                browseId = playlist.id,
                title = playlist.title,
                subtitle = "Playlist • ${playlist.songs.size} songs",
                thumbnailUrl = playlist.songs.firstOrNull()?.thumbnailUrl,
                type = BrowseType.PLAYLIST,
                songs = playlist.songs,
            ),
        )
    }

    /**
     * Keeps an imported Spotify playlist where the phone does: on the account when signed in,
     * otherwise on this computer, which is also the fallback if the account refuses it. Returns a
     * note only for that fallback.
     */
    suspend fun importSpotifyPlaylist(title: String, songs: List<Song>): String? {
        val signedIn = DesktopYouTubeAuth.isSignedIn
        if (signedIn) {
            val created = DesktopSearchClient.createPlaylist(title, PlaylistPrivacy.PRIVATE, songs.map(Song::videoId))
            created.getOrNull()?.let { playlistId ->
                editPlaylistShelf { items ->
                    val card = ShelfItem(
                        title = title,
                        subtitle = "${songs.size} songs",
                        thumbnailUrl = songs.firstOrNull()?.thumbnailUrl,
                        videoId = null,
                        browseId = "VL$playlistId",
                    )
                    listOf(card) + items.filterNot { it.browseId == card.browseId }
                }
                libraryStale = true
                openCollection("VL$playlistId")
                return null
            }
            DesktopTrackLog.log("spotify import: account playlist failed: ${created.exceptionOrNull()?.message}")
        }
        val playlist = DesktopPlaylist(title = title, songs = songs)
        playlists = playlists + playlist
        persistence.savePlaylists(playlists)
        openPlaylist(playlist)
        return if (signedIn) {
            DesktopStrings.format(
                "spotify_import_local_fallback",
                title,
                fallback = "Couldn't create it on YouTube Music, so '%1\$s' was saved on this device instead.",
            )
        } else {
            null
        }
    }

    fun addToPlaylist(playlist: DesktopPlaylist) {
        val song = playlistTarget ?: return
        playlists = playlists.map { current ->
            if (current.id == playlist.id) current.copy(songs = (current.songs + song).distinctBy(Song::videoId)) else current
        }
        persistence.savePlaylists(playlists)
        playlistTarget = null
    }

    fun addToAccountPlaylist(playlist: UserPlaylist) {
        val song = playlistTarget ?: return
        playlistBusy = true
        playlistError = null
        scope.launch {
            DesktopSearchClient.addToPlaylist(playlist.playlistId, listOf(song.videoId))
                .onSuccess { closePlaylistDialogs() }
                .onFailure {
                    DesktopTrackLog.log("playlist: could not add ${song.videoId}: ${it.message}")
                    playlistBusy = false
                    playlistError = it.message ?: "Could not add to ${playlist.title}"
                }
        }
    }

    playbackEngine = remember {
        DesktopPlaybackEngine(
            onEnded = {
                selectedSong?.let(DesktopScrobbling::onPlaybackEnded)
                if (DesktopSleepTimer.afterTrack.value) {
                    DesktopSleepTimer.cancel()
                } else if (repeatMode == DesktopRepeatMode.ONE) {
                    // Replayed in place. [startSong] makes the track a queue of one, which threw
                    // the rest of the queue away the first time it repeated.
                    val song = selectedSong
                    if (song != null && liveQueue.current?.videoId == song.videoId) {
                        playCurrent()
                    } else {
                        song?.let { startSong(it, true) }
                    }
                } else {
                    playNext()
                }
            },
            onCrossfaded = { song ->
                scope.launch {
                    // The queue moves with the audio. Only the selection used to, which left the
                    // queue on the track that had just finished — so Next "advanced" onto the song
                    // already playing, and every later skip landed one behind.
                    liveQueue = liveQueue.afterHandoffTo(song.videoId)
                    saveQueue()
                    selectedSong = song
                    history = (listOf(song) + history.filterNot { it.videoId == song.videoId }).take(50)
                    persistence.saveHistory(history)
                    DesktopScrobbling.updateNowPlaying(song)
                    partySyncHolder[0]?.onLocalIntent()
                }
            },
        )
    }
    val partySync = remember(playbackEngine) {
        DesktopPartySync(
            scope = scope,
            engine = playbackEngine,
            playTrack = { shared, at ->
                // The phone's PartySync.load: the party's running order, standing on its track —
                // so the queue on screen moves with the song rather than keeping the old one as
                // "now playing" and the new one as the first thing still to come.
                val songs = DesktopQueue.adopt(liveQueue.songs, shared.map { it.toDesktopSong() })
                liveQueue = DesktopQueue(songs, at.coerceIn(songs.indices)).trimmed()
                liveQueue.current?.let { song ->
                    selectedSong = song
                    playbackEngine.load(song, playWhenReady = false)
                }
            },
            localQueue = { liveQueue.songs to liveQueue.index },
            applyPartyUpcoming = { upcoming ->
                val aligned = liveQueue.withPartyUpcoming(upcoming.map { it.toDesktopSong() })
                if (aligned !== liveQueue) liveQueue = aligned
            },
            applyPartyAutoplay = { _ ->
                // The party setting overrides the personal preference only while connected. The
                // phone deliberately does not persist it or delete queue rows on this device.
                autoplayJob?.cancel()
                autoplayJob = null
                autoplaySeed = null
            },
            onEnteredParty = {
                if (personalQueueStash == null) {
                    personalQueueStash = liveQueue
                    personalPositionStash = playbackEngine.state.value.positionMs
                    personalPlayingStash = playbackEngine.state.value.isPlaying
                }
            },
            onLeftParty = {
                personalQueueStash?.let { stashed ->
                    liveQueue = stashed
                    selectedSong = stashed.current
                    stashed.current?.let {
                        playbackEngine.load(it, personalPlayingStash, personalPositionStash)
                    }
                    saveQueue()
                }
                personalQueueStash = null
            },
        )
    }
    DisposableEffect(partySync) {
        partySyncHolder[0] = partySync
        partySync.start()
        onDispose {
            partySync.stop()
            if (partySyncHolder[0] === partySync) partySyncHolder[0] = null
        }
    }
    fun togglePlayPauseFromUser() {
        if (partySync.handleLockedPlayPause()) return
        playbackEngine.togglePlayPause()
        partySync.onLocalIntent()
    }
    fun playFromUser() {
        if (DesktopListenTogether.state.value.controlsLocked) {
            if (!playbackEngine.state.value.isPlaying) partySync.handleLockedPlayPause()
            return
        }
        playbackEngine.play()
        partySync.onLocalIntent()
    }
    fun pauseFromUser() {
        if (DesktopListenTogether.state.value.controlsLocked) {
            if (playbackEngine.state.value.isPlaying) partySync.handleLockedPlayPause()
            return
        }
        playbackEngine.pause()
        partySync.onLocalIntent()
    }
    val mprisController = remember(playbackEngine) {
        DesktopMprisController(
            onPlay = ::playFromUser,
            onPause = ::pauseFromUser,
            onPlayPause = ::togglePlayPauseFromUser,
            onNext = ::playNext,
            onPrevious = ::playPrevious,
            onShuffleChanged = { enabled ->
                shuffle = enabled
                persistence.saveBoolean("shuffle", enabled)
            },
            onLoopStatusChanged = { status ->
                repeatMode = when (status) {
                    "Track" -> DesktopRepeatMode.ONE
                    "Playlist" -> DesktopRepeatMode.ALL
                    else -> DesktopRepeatMode.OFF
                }
                persistence.saveString("repeat_mode", repeatMode.name)
            },
            onRateChanged = { value ->
                playbackSpeed = value.toFloat().coerceIn(0.25f, 3.0f)
                persistence.saveString("playback_speed", playbackSpeed.toString())
            },
            onVolumeChanged = { value ->
                volume = value.toFloat().coerceIn(0.0f, 1.0f)
                persistence.saveString("volume", volume.toString())
            },
            onSeek = { positionMs ->
                if (!DesktopListenTogether.state.value.controlsLocked) {
                    playbackEngine.seekTo(positionMs)
                    partySync.onLocalIntent()
                }
            },
        )
    }
    DisposableEffect(mprisController) {
        mprisController.start()
        onDispose { mprisController.stop() }
    }
    // The Windows half of the same idea. Linux has MPRIS above; on Windows this
    // is what the media keys and the volume flyout's card talk to.
    DisposableEffect(playbackEngine) {
        DesktopWindowsMedia.start(
            DesktopWindowsMedia.Controller(
                onPlay = ::playFromUser,
                onPause = ::pauseFromUser,
                onNext = ::playNext,
                onPrevious = ::playPrevious,
                onStop = ::pauseFromUser,
                onSeek = { positionMs ->
                    if (!DesktopListenTogether.state.value.controlsLocked) {
                        playbackEngine.seekTo(positionMs)
                        partySync.onLocalIntent()
                    }
                },
            ),
        )
        DesktopMacMedia.start(
            DesktopMacMedia.Controller(
                onPlay = ::playFromUser,
                onPause = ::pauseFromUser,
                onNext = ::playNext,
                onPrevious = ::playPrevious,
                onStop = ::pauseFromUser,
                onSeek = { positionMs ->
                    if (!DesktopListenTogether.state.value.controlsLocked) {
                        playbackEngine.seekTo(positionMs)
                        partySync.onLocalIntent()
                    }
                },
            ),
        )
        DesktopMacDockMenu.start(
            DesktopMacDockMenu.Controller(
                onPlay = ::playFromUser,
                onPause = ::pauseFromUser,
                onNext = ::playNext,
                onPrevious = ::playPrevious,
                onToggleLike = { selectedSong?.let { toggleLike(it) } },
                onCancelSleepTimer = { DesktopSleepTimer.cancel() },
            ),
        )
        onDispose {
            DesktopWindowsMedia.stop()
            DesktopMacMedia.stop()
            DesktopMacDockMenu.stop()
        }
    }

    LaunchedEffect(playbackSpeed) { playbackEngine.setPlaybackSpeed(playbackSpeed) }
    LaunchedEffect(volume) { playbackEngine.setVolume(volume) }
    LaunchedEffect(audioQuality) { playbackEngine.setAudioQuality(audioQuality.name) }
    LaunchedEffect(automixPerformance) {
        playbackEngine.setAutomixPerformance(automixPerformance)
    }
    // Every one of the seven settings renders the same curve, so they are collected together
    // rather than destructured — seven sources of one tuning is seven chances to read them in the
    // wrong order.
    val eqEnabled by DesktopEqualizerSettings.enabled.collectAsState()
    val eqMode by DesktopEqualizerSettings.mode.collectAsState()
    val eqToneX by DesktopEqualizerSettings.toneX.collectAsState()
    val eqToneY by DesktopEqualizerSettings.toneY.collectAsState()
    val eqFocused by DesktopEqualizerSettings.focused.collectAsState()
    val eqBalance by DesktopEqualizerSettings.balance.collectAsState()
    val eqBands by DesktopEqualizerSettings.bands.collectAsState()
    LaunchedEffect(eqEnabled, eqMode, eqToneX, eqToneY, eqFocused, eqBalance, eqBands) {
        playbackEngine.setEqualizer(eqEnabled, DesktopEqualizerSettings.curve(), eqBalance)
    }
    LaunchedEffect(spatialAudio, skipSilence, outputPrecision, volumeNormalization) {
        playbackEngine.setSpatialAudio(spatialAudio)
        playbackEngine.setSkipSilence(skipSilence)
        playbackEngine.setOutputPrecision(outputPrecision)
        playbackEngine.setVolumeNormalization(volumeNormalization)
    }
    // Not while listening together, exactly as on the phone: a blend starts the next track early,
    // by a length this computer decides from its own copy of the audio, so every member would begin
    // the next song at a different moment and be dragged back by a correcting seek. The transition a
    // party shares is the plain one. The settings themselves are left alone and come back after.
    val inParty = partyState.inParty
    LaunchedEffect(automix, automixOnSkip, automixPhraseBars, crossfadeSeconds, inParty, audioQuality, selectedSong?.videoId, queue, liveQueue.index, shuffle, repeatMode) {
        playbackEngine.setAutomixEnabled(automix && !inParty)
        playbackEngine.setAutomixPhraseBars(if (automix && !inParty) automixPhraseBars else 0)
        playbackEngine.setCrossfadeSeconds(if (inParty) 0 else crossfadeSeconds)
        // A party mixes only with the plain transition, so a skip there cuts like it always did.
        playbackEngine.setSkipMixEnabled(automixOnSkip && !inParty)
        // Exactly what Next would play, so a crossfade can never blend into anything else. Shuffle
        // is already the queue's order; picking "the first track that is not this one" under it
        // blended into a song from history whenever the current one was not at the top.
        // Repeat-one prepares nothing: the track ends and [onEnded] starts it again, rather than
        // blending into the next one.
        val next = selectedSong
            ?.takeIf { repeatMode != DesktopRepeatMode.ONE }
            ?.let { liveQueue.followingFor(it.videoId, repeatAll = repeatMode == DesktopRepeatMode.ALL) }
        playbackEngine.prepareNext(next)
    }
    // Topped up on every track change, the way Android does it, rather than only once the queue has
    // run dry.
    LaunchedEffect(selectedSong?.videoId, autoplay, repeatMode) {
        loadAutoplaySongs()
    }
    val partyAutoplaySupplier = DesktopAutoplay.supplierId(partyState)
    LaunchedEffect(
        partyState.code,
        partyState.playback.autoplayEnabled,
        partyAutoplaySupplier,
    ) {
        if (partyState.inParty &&
            partyState.playback.autoplayEnabled &&
            partyAutoplaySupplier == partyState.you?.memberId
        ) {
            autoplayJob?.cancel()
            autoplayJob = null
            autoplaySeed = null
            loadAutoplaySongs()
        }
    }
    LaunchedEffect(liveQueue.songs.size, liveQueue.index, partyState.queue.seq) {
        val currentId = selectedSong?.videoId
        val shouldRefresh = DesktopAutoplay.queueNeedsRefresh(
            enabled = DesktopAutoplay.enabled(partyState, autoplay),
            repeatAll = repeatMode == DesktopRepeatMode.ALL,
            currentIndex = liveQueue.index,
            itemCount = liveQueue.songs.size,
            loadInProgress = autoplayJob?.isActive == true,
        )
        // Only re-arm a seed that has already been attempted. The track-change effect above owns
        // the initial load; this effect owns later local or server queue edits that expose a tail.
        if (shouldRefresh && currentId != null && autoplaySeed == currentId) {
            autoplayJob = null
            autoplaySeed = null
            loadAutoplaySongs()
        }
    }
    LaunchedEffect(Unit) {
        while (currentCoroutineContext().isActive) {
            delay(1_000L)
            if (DesktopSleepTimer.isExpired()) {
                DesktopSleepTimer.cancel()
                playbackEngine.pause()
            }
        }
    }
    val playback by playbackEngine.state.collectAsState()

    // The shared player reads the playhead off this one object, and only where it
    // draws it — see PlaybackPosition — so a tick never recomposes the player.
    val playerPosition = remember { PlaybackPosition() }
    LaunchedEffect(playbackEngine) {
        playbackEngine.state.collect {
            // The audio thread's own timestamp, not this collector's: it runs on the UI thread and
            // gets to a reading as late as the UI is busy.
            playerPosition.report(it.positionMs, it.positionSampledAtNanos)
            playerPosition.seeks = it.seeks
            playerPosition.advancing = !it.awaitingAudio
        }
    }

    // Lyrics and motion artwork follow whatever is *playing*, not whatever page happens to be open.
    // This is where Android keeps it — `MainActivity`, keyed on the player's own track — so the
    // lookup starts the moment a track does and the panel is already populated by the time anyone
    // opens the player. Run from the player page instead, nothing was fetched until it was opened,
    // and a track half a minute in then showed a skeleton and arrived mid-song.
    var lyrics by remember { mutableStateOf<DesktopLyrics?>(null) }
    // The provider picked for this track in the player's lyrics drawer, if any.
    var lyricsOnly by remember(selectedSong?.videoId) { mutableStateOf<String?>(null) }
    // A title and artist typed into the song menu's lyrics search, overriding the
    // track's own credits for the lookup — for a cover or a mislabelled upload.
    var lyricsQuery by remember(selectedSong?.videoId) { mutableStateOf<Pair<String, String>?>(null) }
    // Bumped on every search so re-running the same query still refetches.
    var lyricsSearchTick by remember(selectedSong?.videoId) { mutableStateOf(0) }
    // What each provider's most recent race turned up, so the song menu's
    // provider drawer can say "Found" or "Not found" per source, not just name
    // the winner. It is keyed on the track and is only ever added to, never
    // cleared mid-track: a repeat search a cache answers straight away fires no
    // callbacks, so clearing on start would blank the very states it should hold.
    val lyricsProviderResults = remember(selectedSong?.videoId) {
        mutableStateOf<Map<LyricsSource, LyricsProviderState>>(emptyMap())
    }
    var lyricsLoading by remember { mutableStateOf(false) }
    var lyricsError by remember { mutableStateOf<String?>(null) }
    var canvas by remember { mutableStateOf<DesktopCanvasArtwork?>(null) }

    // The length the lyrics are matched on, taken once per track. A better rendition swapped in
    // mid-song reports its own, slightly different length — or none, for a manifest — and keyed
    // on the live duration, every such upgrade threw away the lyrics on screen and fetched them
    // all over again. Only the playing track's own report counts: right after a skip, [playback]
    // still describes the track before.
    var lyricsLengthMs by remember(selectedSong?.videoId) { mutableStateOf(0L) }
    val playingLengthMs = playback.durationMs.takeIf { playback.song?.videoId == selectedSong?.videoId } ?: 0L
    LaunchedEffect(selectedSong?.videoId, playingLengthMs > 0L) {
        if (lyricsLengthMs <= 0L && playingLengthMs > 0L) lyricsLengthMs = playingLengthMs
    }

    // Keyed on the duration too: it lands a beat after the track, and a database match needs it,
    // so looking up against a length of zero would settle on the wrong recording.
    LaunchedEffect(
        selectedSong?.videoId,
        lyricsLengthMs,
        syncedLyrics,
        prioritizeSyllables,
        lyricsOn,
        lyricsOrder,
        lyricsOnly,
        lyricsQuery,
        lyricsSearchTick,
    ) {
        val current = selectedSong
        lyrics = null
        lyricsError = null
        if (current == null) {
            lyricsLoading = false
            return@LaunchedEffect
        }
        if (!syncedLyrics) {
            lyricsError = "Synced lyrics are switched off"
            lyricsLoading = false
            return@LaunchedEffect
        }
        // A length is what a database match is made on, and it lands a beat after the track. Looking
        // up without one would settle on whichever recording shares the name — so this waits, the
        // same way Android's `loadLyrics` turns the call away until a duration exists.
        val length = lyricsLengthMs.takeIf { it > 0L } ?: current.durationMillis()
        if (length <= 0L) {
            lyricsLoading = true
            return@LaunchedEffect
        }
        lyricsLoading = true
        // The manual search asks the same providers under the typed words rather
        // than the track's own credits; a provider switch keeps that query.
        val probe = lyricsQuery?.let { (title, artist) -> current.copy(title = title, artist = artist) } ?: current
        DesktopLyricsClient.lookup(
            probe,
            length,
            only = lyricsOnly,
            // Each provider reports itself as the race runs; the callback is on
            // the IO thread, so the map is updated back on the composition's own.
            onSourceResult = { source, found ->
                scope.launch {
                    lyricsProviderResults.value += source to
                        if (found) LyricsProviderState.FOUND else LyricsProviderState.NOT_FOUND
                }
            },
        ).fold(
            onSuccess = { lyrics = it },
            onFailure = { lyricsError = it.message ?: "Lyrics unavailable" },
        )
        lyricsLoading = false
    }

    // A manual ask from the song menu's lyrics drawer, for a track whose automatic
    // lookup came up empty and whose real title or artist the listener knows
    // better than the tag. It just sets the query and nudges the effect above,
    // which owns the loading state and the result — so the drawer's provider
    // list updates through the same path as the automatic lookup.
    fun searchLyrics(title: String, artist: String) {
        lyricsQuery = title to artist
        lyricsSearchTick++
    }

    // The song menu's lyrics offset nudges the whole synced lyric against the clock. It is a
    // per-track adjustment, so a new track starts back at zero; the desktop owns the reset so the
    // phone's sheet, which carries the offset across tracks, is left alone.
    val lyricsOffsetMs by DesktopPlayerSettings.lyricsOffsetMs.collectAsState()
    LaunchedEffect(selectedSong?.videoId) {
        DesktopPlayerSettings.setLyricsOffsetMs(0)
    }

    LaunchedEffect(selectedSong?.videoId, animatedCanvas) {
        val current = selectedSong
        if (current == null || !animatedCanvas) {
            canvas = null
            return@LaunchedEffect
        }
        // Paint what is already known before waiting on anything: re-opening the player on a track
        // resolved a minute ago should not go dark on its way back to the same clip.
        canvas = DesktopCanvasClient.cached(current)
        canvas = withContext(Dispatchers.IO) { DesktopCanvasClient.lookup(current) }
    }

    LaunchedEffect(
        playback.song?.videoId,
        playback.isPlaying,
        playback.durationMs,
        playback.volume,
        playback.error,
        shuffle,
        repeatMode,
        playbackSpeed,
    ) {
        mprisController.update(
            playback = playback,
            shuffle = shuffle,
            loopStatus = when (repeatMode) {
                DesktopRepeatMode.OFF -> "None"
                DesktopRepeatMode.ALL -> "Playlist"
                DesktopRepeatMode.ONE -> "Track"
            },
            rate = playbackSpeed.toDouble(),
        )
    }
    LaunchedEffect(playback.song?.videoId, playback.isPlaying, playback.durationMs) {
        DesktopScrobbling.onPlaybackStateChanged(playback)
        DesktopWindowsMedia.publish(playback)
        DesktopMacMedia.publish(playback)
    }
    var lastNotifiedTrackId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSong, playback.isPlaying, likedIds, sleepRemainingMs, sleepAfterTrack) {
        DesktopMacDockMenu.update(
            song = selectedSong,
            isPlaying = playback.isPlaying,
            isLiked = selectedSong?.let { it.videoId in likedIds } ?: false,
            sleepTimerRemainingMs = sleepRemainingMs,
            sleepAfterTrack = sleepAfterTrack,
        )
        if (playback.isPlaying && selectedSong != null && selectedSong?.videoId != lastNotifiedTrackId) {
            lastNotifiedTrackId = selectedSong?.videoId
            val artUrl = selectedSong?.artworkAt(512) ?: selectedSong?.thumbnailUrl
            val artPath = DesktopArtworkCache.getCachedFile(artUrl)?.absolutePath
                ?: selectedSong?.thumbnailUrl?.let { DesktopArtworkCache.getCachedFile(it)?.absolutePath }
                ?: artUrl.orEmpty()
            DesktopMacMedia.postNotification(
                title = selectedSong?.title.orEmpty(),
                subtitle = selectedSong?.artist.orEmpty(),
                body = selectedSong?.albumName ?: "MaviMusic",
                imagePath = artPath,
            )
        }
    }
    // Refetched on opening the page rather than cached for the session: the
    // point of it is that something played elsewhere shows up here.
    LaunchedEffect(destination, youtubeSignedIn, sessionRevision) {
        if (destination != DesktopDestination.HISTORY || !youtubeSignedIn) return@LaunchedEffect
        DesktopSearchClient.history().onSuccess { remoteHistory = it }
    }
    LaunchedEffect(selectedSong?.videoId, overlays.nowPlaying) {
        trackLinks = null
        val current = selectedSong ?: return@LaunchedEffect
        // Only while the player is up, so playing from the mini player costs
        // nothing, and only when something is actually missing.
        if (!overlays.nowPlaying) return@LaunchedEffect
        if (current.artistId != null && current.albumId != null) return@LaunchedEffect
        trackLinks = DesktopSearchClient.trackLinks(current.videoId).getOrNull()
    }
    // Keyed on the same things, plus the rate: Discord counts the bar down on
    // its own clock, so only a change to what it was told is worth another push.
    LaunchedEffect(playback.song?.videoId, playback.isPlaying, playback.durationMs, playbackSpeed, playback.streamFormat) {
        DesktopDiscordRpc.onPlaybackStateChanged(playback, playbackSpeed)
    }
    // The account's own history on YouTube Music, which is what the home feed is built out of.
    DisposableEffect(playback.song?.videoId) {
        val previous = playbackEngine.state.value.positionMs / 1_000
        onDispose { DesktopPlaybackTracker.onTrackChanged(previous) }
    }
    LaunchedEffect(playback.song?.videoId, playback.isPlaying) {
        val songId = playback.song?.videoId ?: return@LaunchedEffect
        if (!playback.isPlaying) return@LaunchedEffect
        DesktopPlaybackTracker.onPlaying(songId)
        while (currentCoroutineContext().isActive) {
            delay(HISTORY_SAMPLE_MS)
            val snapshot = playbackEngine.state.value
            if (snapshot.song?.videoId != songId) break
            DesktopPlaybackTracker.onProgress(songId, snapshot.positionMs / 1_000)
        }
    }
    LaunchedEffect(playback.song?.videoId, playback.isPlaying) {
        val songId = playback.song?.videoId ?: return@LaunchedEffect
        if (!playback.isPlaying) return@LaunchedEffect
        var previousPosition = playback.positionMs
        var countedAsPlay = songId in statsCountedSongs
        while (currentCoroutineContext().isActive) {
            delay(STATS_SAMPLE_MS)
            val snapshot = playbackEngine.state.value
            if (!snapshot.isPlaying || snapshot.song?.videoId != songId) break
            val position = snapshot.positionMs
            val delta = (position - previousPosition).takeIf { it in 1L..STATS_MAX_DELTA_MS } ?: 0L
            if (delta > 0L) {
                val threshold = snapshot.durationMs.takeIf { it > 0L }?.let { minOf(it / 2L, SCROBBLE_THRESHOLD_MS) }
                    ?: SCROBBLE_THRESHOLD_MS
                val reachedThreshold = !countedAsPlay && position >= threshold
                DesktopListeningStats.record(snapshot.song, delta, reachedThreshold)
                if (reachedThreshold) {
                    countedAsPlay = true
                    statsCountedSongs = statsCountedSongs + songId
                }
                statsRevision++
            }
            previousPosition = position
        }
    }
    LaunchedEffect(statsRevision) {
        replaySummary = withContext(Dispatchers.IO) { DesktopListeningStats.summary(replayPeriod) }
    }
    DisposableEffect(playbackEngine) {
        onDispose { playbackEngine.release() }
    }

    /** Opens an artist's page, from a card, a search row or the player's credit. */
    fun openArtist(browseId: String, name: String = "") {
        val target = DesktopArtistTarget(browseId, name)
        if (openedArtist == target && artistState is UiState.Success) return
        openedCollection = null
        overlays.replay = false
        overlays.settingsPage = null
        openedArtist = target
        artistState = UiState.Loading
        artistReloads++
    }

    /** Finds and opens an album by title and optional artist name. */
    fun openAlbumByName(title: String, artist: String? = null) {
        scope.launch {
            val query = if (artist.isNullOrBlank()) title else "$title $artist"
            val match = DesktopSearchClient.search(query, SearchFilter.ALBUMS)
                .getOrDefault(emptyList())
                .filterIsInstance<SearchResult.Browse>()
                .map { it.item }
                .firstOrNull { it.browseId.isNotBlank() }
                ?: return@launch
            openAlbum(match.browseId)
        }
    }

    /** Replay counts artists by name, so the page has to be found from the name alone. */
    fun openArtistByName(name: String) {
        scope.launch {
            val match = DesktopSearchClient.search(name, SearchFilter.ARTISTS)
                .getOrDefault(emptyList())
                .filterIsInstance<SearchResult.Browse>()
                .map { it.item }
                .firstOrNull { it.browseId.isNotBlank() }
                ?: return@launch
            openArtist(match.browseId, match.title)
        }
    }

    /**
     * The "…" menu for one row, wherever it is drawn.
     *
     * Built once so a row on Downloads or Local Music offers what the player's own "…" offers —
     * Android opens the same `SongActionsSheet` from every one of them.
     */
    fun songActionsFor(song: Song) = DesktopSongActions(
        signedIn = youtubeSignedIn,
        disliked = song.videoId in dislikedIds,
        downloaded = downloads.any { it.videoId == song.videoId },
        downloadInProgress = song.videoId in downloadInProgress,
        sleepTimerMinutes = sleepTimerMinutes,
        sleepAfterTrack = sleepAfterTrack,
        onToggleDislike = ::toggleDislike,
        onAddToPlaylist = { playlistTarget = it },
        onDownload = ::downloadSong,
        onRemoveDownload = ::removeDownload,
        onStartRadio = ::startRadio,
        onPlayNext = ::playNext,
        onAddToQueue = ::addToQueue,
        onOpenAlbum = { target ->
            if (target.startsWith("MPRE") || target.startsWith("FEmusic_library_privately_owned_release") || target.startsWith("OLAK") || (target.length >= 24 && !target.contains(' '))) {
                openAlbum(target)
            } else {
                openAlbumByName(target, song.artist)
            }
        },
        onOpenArtist = { target ->
            if (target.startsWith("UC") || target.startsWith("FEmusic_library_privately_owned_artist") || (target.length >= 24 && !target.contains(' '))) {
                openArtist(target, song.artist)
            } else {
                openArtistByName(target)
            }
        },
        onSleepTimer = { minutes ->
            if (minutes == null) DesktopSleepTimer.cancel() else DesktopSleepTimer.start(minutes)
        },
        onSleepAfterTrack = { DesktopSleepTimer.startAfterTrack() },
        onShare = ::shareSong,
    )

    /** The "…" a song row hangs off, built the same way on every page that offers one. */
    @Composable
    fun songMenu(song: Song) = DesktopSongMenuAnchor(
        song = song,
        liked = song.videoId in likedIds,
        actions = songActionsFor(song),
        onToggleLike = { toggleLike(song) },
        onRevertToOriginal = null,
        onUpgradeQuality = null,
    )

    LaunchedEffect(openedArtist, artistReloads) {
        val target = openedArtist ?: return@LaunchedEffect
        if (artistState !is UiState.Loading) return@LaunchedEffect
        DesktopSearchClient.artistPage(target.browseId).fold(
            onSuccess = { artistState = UiState.Success(it) },
            onFailure = { artistState = UiState.Error(it.message ?: "Could not open ${target.name}") },
        )
    }

    fun toggleSubscription(subscription: SubscriptionState) {
        val target = openedArtist ?: return
        val next = !subscription.subscribed
        // Painted before the network answers and put back if it refuses.
        artistState = (artistState as? UiState.Success)?.let { current ->
            UiState.Success(current.data.copy(subscription = subscription.copy(subscribed = next)))
        } ?: artistState
        scope.launch {
            DesktopSearchClient.setSubscribed(subscription.channelId, next).onFailure { failure ->
                DesktopTrackLog.log("subscription: ${target.name} failed: ${failure.message}")
                artistState = (artistState as? UiState.Success)?.let { current ->
                    UiState.Success(current.data.copy(subscription = subscription))
                } ?: artistState
            }
        }
    }

    /** A search hit or a recent search that is a page: an artist's own, or a collection's. */
    fun openBrowseItem(item: BrowseItem) {
        if (item.type == BrowseType.ARTIST) {
            openArtist(item.browseId, item.title)
        } else {
            openCollection(item.browseId, item)
        }
    }

    fun openShelfItem(item: ShelfItem, shelfTitle: String? = null) {
        val videoId = item.videoId
        val browseId = item.browseId
        when {
            videoId != null -> {
                val source = shelfTitle?.let { DesktopQueueSource(it, PlaybackSourceType.HOME) }
                playSong(item.toSong(), source = source)
            }
            // An artist is a page of its own, not a list of tracks with a photograph on top.
            browseTypeOf(browseId.orEmpty()) == BrowseType.ARTIST ->
                openArtist(browseId!!, item.title)
            browseId != null -> openCollection(
                browseId = browseId,
                fallback = BrowseItem(
                    browseId = browseId,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    type = BrowseType.OTHER,
                ),
            )
        }
    }

    fun openMenu(song: Song) = pageMenu(song)

    /** What the phone records for a search hit: the thing picked, not the words typed to find it. */
    fun recordSongSearch(song: Song) = DesktopSearchHistory.record(
        SearchHistoryEntity(
            id = song.videoId,
            title = song.title,
            subtitle = song.artist,
            artworkUrl = song.thumbnailUrl,
            entityType = EntityType.TRACK,
        ),
    )

    fun recordBrowseSearch(item: BrowseItem) = DesktopSearchHistory.record(
        SearchHistoryEntity(
            id = item.browseId,
            title = item.title,
            subtitle = item.subtitle,
            artworkUrl = item.thumbnailUrl,
            entityType = when (item.type) {
                BrowseType.ALBUM -> EntityType.ALBUM
                BrowseType.ARTIST -> EntityType.ARTIST
                BrowseType.PLAYLIST -> EntityType.PLAYLIST
                else -> EntityType.TRACK
            },
        ),
    )

    /** Typing, from the page's field or the sidebar's: the typeahead follows, and an empty field goes back to recents. */
    fun editQuery(text: String) {
        query = text
        searchTyping = true
        if (text.isBlank()) {
            searchCommitted = false
            searchRows = emptyList()
            searchError = null
        }
    }

    fun search() {
        if (query.isBlank() || searchLoading) return
        overlays.settingsPage = null
        destination = DesktopDestination.SEARCH
        searchTyping = false
        searchSuggestions = emptyList()
        // Submitting is picking the first track the typeahead offered, as on the phone — or, with
        // none, the first the results bring back.
        val typedHit = searchTypeahead.firstNotNullOfOrNull { row ->
            when (row) {
                is SearchResult.TopTrack -> row.song
                is SearchResult.Track -> row.song
                else -> null
            }
        }
        typedHit?.let(::recordSongSearch)
        searchCommitted = true
        searchScrollReset++
        searchLoading = true
        searchError = null
        scope.launch {
            try {
                DesktopMusicSources.search(query, searchFilter).fold(
                    onSuccess = { rows ->
                        searchRows = rows
                        if (typedHit == null) {
                            rows.firstNotNullOfOrNull { row ->
                                when (row) {
                                    is SearchResult.TopTrack -> row.song
                                    is SearchResult.Track -> row.song
                                    else -> null
                                }
                            }?.let(::recordSongSearch)
                        }
                    },
                    onFailure = { searchError = it.message ?: "Search failed" },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                searchError = failure.message ?: "Search failed"
            } finally {
                // OBS and other desktop capture tools can briefly interrupt focus/coroutines.
                searchLoading = false
            }
        }
    }

    /** The typeahead, one request behind the keystrokes rather than one per. */
    LaunchedEffect(query, searchTyping) {
        if (!searchTyping || query.isBlank()) {
            searchSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(SUGGESTION_DEBOUNCE_MS)
        DesktopSearchClient.searchSuggestions(query)
            .onSuccess { if (searchTyping) searchSuggestions = it }
            .onFailure { searchSuggestions = emptyList() }
    }

    // Its own pass, on its own delay: a search costs far more than a completion, so it waits for a
    // longer pause in the typing rather than riding the same one.
    LaunchedEffect(query, searchTyping) {
        if (!searchTyping || query.isBlank()) {
            searchTypeahead = emptyList()
            return@LaunchedEffect
        }
        delay(TYPEAHEAD_MEDIA_DEBOUNCE_MS)
        DesktopSearchClient.searchTypeahead(query)
            .onSuccess { if (searchTyping) searchTypeahead = it.take(TYPEAHEAD_MEDIA_LIMIT) }
            .onFailure { searchTypeahead = emptyList() }
    }

    fun runSearch(term: String) {
        query = term
        searchTyping = false
        searchSuggestions = emptyList()
        searchTypeahead = emptyList()
        search()
    }

    fun selectDestination(next: DesktopDestination) {
        if (next == DesktopDestination.LIBRARY && libraryStale) {
            libraryStale = false
            reloadLibrary()
        }
        openedArtist = null
        openedCollection = null
        overlays.replay = false
        overlays.settingsPage = null
        selectedMoodGenre = null
        libraryShowAll = null
        if (next == DesktopDestination.SEARCH) searchFocusRequested = true
        destination = next
    }

    // Settings' search and scroll outlive a visit to one of its pages, and start over each time
    // Settings itself is opened.
    var settingsSession by remember { mutableStateOf(0) }
    var settingsQuery by remember(settingsSession) { mutableStateOf("") }
    val settingsListState = remember(settingsSession) { LazyListState() }

    /** Settings in place of the page — from the sidebar, the account switcher or the tray. */
    fun openSettings() {
        overlays.nowPlaying = false
        if (overlays.settingsPage == null) settingsSession++
        overlays.settingsPage = DesktopSettingsPage.MAIN
    }

    /** The Library's folder rows: the two folders this computer has, and Spotify once connected. */
    val libraryLinks = remember(spotifyCanvasCookie.isNotBlank()) {
        listOfNotNull(
            LibraryLink(
                item = ShelfItem(
                    title = DesktopStrings["downloads", "Downloads"],
                    subtitle = DesktopStrings["downloaded_songs", "Downloaded songs"],
                    thumbnailUrl = null,
                    videoId = null,
                    browseId = LOCAL_DOWNLOADS_ID,
                ),
                icon = Icons.Rounded.Download,
            ),
            LibraryLink(
                item = ShelfItem(
                    title = DesktopStrings["local_music", "Local Music"],
                    subtitle = DesktopStrings["d_audio_files_on_this_computer", "Audio files on this computer"],
                    thumbnailUrl = null,
                    videoId = null,
                    browseId = LOCAL_MUSIC_ID,
                ),
                icon = Icons.Rounded.Folder,
            ),
            // As on the phone, only while signed in to Spotify in Account & integrations.
            LibraryLink(
                item = ShelfItem(
                    title = SPOTIFY_SHELF,
                    subtitle = DesktopStrings["spotify_library_subtitle", "Your playlists and Liked Songs"],
                    thumbnailUrl = null,
                    videoId = null,
                    browseId = SPOTIFY_LIBRARY_ID,
                ),
                icon = Icons.Rounded.Cloud,
                logo = SharedRes.drawable.spotify_logo,
            ).takeIf { spotifyCanvasCookie.isNotBlank() },
        )
    }

    /** The Spotify account's playlists, as a Library grid; Liked Songs first, as on the phone. */
    fun openSpotifyLibrary() {
        libraryShowAll = HomeShelf(SPOTIFY_SHELF, emptyList())
        spotifyLibraryStatus = "Loading your Spotify playlists…"
        scope.launch {
            runCatching { SpotifyLibrary.playlists() }
                .onSuccess { found ->
                    spotifyLibraryStatus = null
                    if (libraryShowAll?.title == SPOTIFY_SHELF) {
                        libraryShowAll = HomeShelf(SPOTIFY_SHELF, found.map { it.toShelfItem() })
                    }
                }
                .onFailure {
                    DesktopTrackLog.log("spotify: playlists failed: ${it.message}")
                    spotifyLibraryStatus = "Could not load your Spotify playlists: ${it.message}"
                }
        }
    }

    /**
     * The Library's "On device" shelf: the playlists kept on this computer rather than on the
     * account — the desktop's counterpart of the phone's downloaded releases, and like them
     * reachable from nowhere else.
     */
    val libraryDeviceItems = remember(playlists) {
        playlists.map { playlist ->
            ShelfItem(
                title = playlist.title,
                subtitle = "Playlist • ${playlist.songs.size} songs",
                thumbnailUrl = playlist.songs.firstOrNull()?.thumbnailUrl,
                videoId = null,
                browseId = LOCAL_PLAYLIST_PREFIX + playlist.id,
            )
        }
    }

    /** A Library card: a folder is a page of this app's, a local playlist is opened from here. */
    fun openLibraryItem(item: ShelfItem) {
        val browseId = item.browseId.orEmpty()
        when {
            browseId == LOCAL_DOWNLOADS_ID -> selectDestination(DesktopDestination.DOWNLOADS)
            browseId == LOCAL_MUSIC_ID -> selectDestination(DesktopDestination.LOCAL_MUSIC)
            browseId == SPOTIFY_LIBRARY_ID -> openSpotifyLibrary()
            // Spotify's tracks fill in below, as they are read and matched.
            browseId.startsWith(SPOTIFY_PAGE_PREFIX) -> showCollection(
                DesktopCollection(
                    browseId = browseId,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    type = BrowseType.PLAYLIST,
                    songs = emptyList(),
                    loading = true,
                ),
            )
            browseId.startsWith(LOCAL_PLAYLIST_PREFIX) ->
                playlists.firstOrNull { it.id == browseId.removePrefix(LOCAL_PLAYLIST_PREFIX) }?.let(::openPlaylist)
            else -> openShelfItem(item)
        }
    }

    /**
     * Adds shelves to the page as they arrive, skipping any heading already on it and any shelf
     * that repeats one's releases — the phone's `publishHomeShelves`. Recently Played is
     * [prepend]ed and replaces any stale copy the core feed carried.
     */
    fun publishHomeShelves(shelves: List<HomeShelf>, prepend: Boolean = false) {
        val existing = (homeState as? UiState.Success)?.data.orEmpty()
        if (prepend) {
            val replacing = shelves.map { it.title.lowercase() }.toSet()
            homeSeenTitles.addAll(replacing)
            homeState = UiState.Success(shelves + existing.filterNot { it.title.lowercase() in replacing })
            return
        }
        val added = shelves.withoutRepeatsOf(existing)
            .filter { it.items.isNotEmpty() && homeSeenTitles.add(it.title.lowercase()) }
        if (added.isNotEmpty()) homeState = UiState.Success(existing + added)
    }

    /**
     * The phone's Home: the core feed, the account's own Recently Played at its head, and the
     * supplementary browse feeds, all asked for at once and laid down as each lands. Signed out
     * there is no Recently Played to ask for, which is also how the phone has it.
     */
    fun loadHome() {
        if (!youtubeSourceEnabled) {
            homeState = UiState.Error("YouTube Music is disabled")
            return
        }
        val generation = ++homeGeneration
        val signedIn = DesktopYouTubeAuth.isSignedIn
        homeIdentity = "$signedIn/${DesktopAccounts.activeAccountId()}"
        homeState = UiState.Loading
        homeContinuation = null
        homeSeenTitles.clear()
        homeLoadingMore = false
        homeRecentsLoading = signedIn
        scope.launch {
            launch {
                DesktopSearchClient.home().fold(
                    onSuccess = { feed ->
                        if (generation != homeGeneration) return@fold
                        homeContinuation = feed.continuation
                        publishHomeShelves(feed.shelves)
                    },
                    onFailure = {
                        if (generation == homeGeneration && homeState !is UiState.Success) {
                            homeState = UiState.Error(it.message ?: "Could not load Home")
                        }
                    },
                )
            }
            if (signedIn) {
                launch {
                    YtMusicRepository.homeRecentlyPlayed()
                        .onSuccess { shelf ->
                            if (generation != homeGeneration) return@onSuccess
                            homeRecentsLoading = false
                            shelf?.let { publishHomeShelves(listOf(it), prepend = true) }
                        }
                        .onFailure { if (generation == homeGeneration) homeRecentsLoading = false }
                }
            }
            DesktopSearchClient.HOME_SUPPLEMENT_BROWSE_IDS.forEach { browseId ->
                launch {
                    DesktopSearchClient.homeSupplement(browseId).onSuccess { shelves ->
                        if (generation == homeGeneration) publishHomeShelves(shelves)
                    }
                }
            }
        }
    }

    /** The next page of the core feed, as the list nears its end; a no-op once it has run dry. */
    fun loadMoreHome() {
        val token = homeContinuation ?: return
        if (homeLoadingMore || homeState !is UiState.Success) return
        val generation = homeGeneration
        homeLoadingMore = true
        scope.launch {
            DesktopSearchClient.moreHome(token).fold(
                onSuccess = { feed ->
                    if (generation != homeGeneration) return@fold
                    val before = homeSeenTitles.size
                    publishHomeShelves(feed.shelves)
                    // A page with nothing new is the feed looping back on itself, not running dry.
                    homeContinuation = feed.continuation.takeIf { homeSeenTitles.size > before }
                },
                onFailure = { homeContinuation = null },
            )
            homeLoadingMore = false
        }
    }

    /** Plays random music matching user's taste ("Tener suerte" / I'm Feeling Lucky) */
    fun playFeelingLucky() {
        val userTasteSongs = mutableListOf<Song>()
        userTasteSongs.addAll(history)
        userTasteSongs.addAll(localSongs)

        val currentHome = homeState
        if (currentHome is UiState.Success) {
            for (shelf in currentHome.data) {
                for (item in shelf.items) {
                    if (item.videoId != null) {
                        userTasteSongs.add(item.toSong())
                    }
                }
            }
        }

        val chosen = userTasteSongs.distinctBy { it.videoId }.shuffled().firstOrNull()
        if (chosen != null) {
            playSong(chosen, startPlaying = true, source = DesktopQueueSource("🎲 Tener suerte", PlaybackSourceType.HOME))
            DesktopPlayerHost.showMessage("🎲 Voy a tener suerte: ${chosen.title} — ${chosen.artist}")
        } else {
            scope.launch {
                loadHome()
                DesktopPlayerHost.showMessage("🎲 Cargando música recomendada...")
            }
        }
    }

    // The tray icon speaks StatusNotifierItem rather than going through AWT, which only implements
    // the XEmbed tray a Wayland session does not have.
    val tray = remember {
        DesktopStatusNotifierController(
            // On macOS clicking the menu bar icon toggles the Dark Teal Glass Mini Player;
            // on other platforms it brings the main window back.
            onActivate = {
                if (DesktopPlatform.isMac) {
                    DesktopMiniPlayerState.toggle()
                } else {
                    DesktopWindowVisibility.show()
                    overlays.nowPlaying = true
                }
            },
            onPlayPause = { if (selectedSong != null) togglePlayPauseFromUser() },
        )
    }
    DisposableEffect(tray, trayIconEnabled) {
        if (trayIconEnabled) tray.start()
        onDispose { tray.stop() }
    }
    // Deliberately not a key on the effect above.
    DisposableEffect(trayIconEnabled, closeToTray) {
        // The listener's choice, with the tray as its precondition: hiding with no tray to come
        // back from leaves an audible process and no window.
        DesktopWindowVisibility.keepRunningWhenClosed = trayIconEnabled && closeToTray
        onDispose { DesktopWindowVisibility.keepRunningWhenClosed = false }
    }
    LaunchedEffect(tray, trayIconEnabled) {
        if (!trayIconEnabled) return@LaunchedEffect
        // The logo travels to the tray as pixels: a themed icon name would be whatever the user's
        // theme draws for a generic music player.
        runCatching { Res.readBytes("drawable/app_logo.png") }.onSuccess(tray::setIcon)
    }
    LaunchedEffect(tray) {
        DesktopTrayMenu.bind(
            onPlayPause = { if (selectedSong != null) togglePlayPauseFromUser() },
            onNext = ::playNext,
            onPrevious = ::playPrevious,
            onOpenPlayer = {
                if (DesktopPlatform.isMac) {
                    DesktopMiniPlayerState.show()
                } else {
                    DesktopWindowVisibility.show()
                    overlays.miniPlayer = true
                }
            },
            onOpenSettings = {
                DesktopWindowVisibility.show()
                openSettings()
            },
            onQuit = {
                // Native work first: the audio thread and the media controls are inside FFmpeg,
                // WASAPI and WinRT, and exitProcess would unload those libraries under them.
                playbackEngine.shutdown()
                DesktopWindowsMedia.stop()
                DesktopMacMedia.stop()
                DesktopMacDockMenu.stop()
                exitProcess(0)
            },
        )
    }
    LaunchedEffect(selectedSong, playback.isPlaying) {
        val title = selectedSong?.let { "${it.title} — ${it.artist}" }
        tray.update(title = title, isPlaying = playback.isPlaying)
        DesktopTrayMenu.publish(title = title, playing = playback.isPlaying)
    }

    LaunchedEffect(
        selectedSong,
        playback.isPlaying,
        playback.isLoading,
        playback.positionMs,
        playback.durationMs,
        playback.volume,
        liveQueue.hasPrevious,
    ) {
        DesktopMiniPlayerState.song = selectedSong
        DesktopMiniPlayerState.isPlaying = playback.isPlaying
        DesktopMiniPlayerState.isLoading = playback.isLoading
        DesktopMiniPlayerState.positionMs = playback.positionMs
        DesktopMiniPlayerState.durationMs = playback.durationMs
        DesktopMiniPlayerState.volume = playback.volume
        DesktopMiniPlayerState.previousEnabled = liveQueue.hasPrevious || playback.positionMs > BACK_RESTARTS_AFTER_MS
    }
    LaunchedEffect(Unit) {
        DesktopMiniPlayerState.onPlayPause = { if (selectedSong != null) togglePlayPauseFromUser() }
        DesktopMiniPlayerState.onNext = ::playNext
        DesktopMiniPlayerState.onPrevious = ::playPrevious
        DesktopMiniPlayerState.onSeek = { playbackEngine.seekTo(it) }
        DesktopMiniPlayerState.onVolumeChange = {
            volume = it
            persistence.saveString("volume", it.toString())
        }
        DesktopMiniPlayerState.onOpenNowPlaying = {
            DesktopWindowVisibility.show()
            overlays.nowPlaying = true
        }
        DesktopMiniPlayerState.onOpenLyrics = {
            DesktopWindowVisibility.show()
            overlays.nowPlaying = false
            overlays.sidePanel = DesktopSidePanel.LYRICS
        }
        DesktopMiniPlayerState.onOpenQueue = {
            DesktopWindowVisibility.show()
            overlays.nowPlaying = false
            overlays.sidePanel = DesktopSidePanel.QUEUE
        }
        DesktopMiniPlayerState.onToggleExpand = {
            DesktopWindowVisibility.show()
            overlays.nowPlaying = true
        }
        DesktopMiniPlayerState.onFeelingLucky = ::playFeelingLucky
        DesktopMiniPlayerState.onOpenSongMenu = { song ->
            DesktopWindowVisibility.show()
            openMenu(song)
        }
    }

    LaunchedEffect(Unit) {
        // Unpacks the analyser and its models on first run.
        DesktopAnalysisRuntime.ensureStarted()
        DesktopHorizontalScroll.install()
        localSongs = withContext(Dispatchers.IO) { DesktopLocalMusic.scan() }
        // A download record is a claim about a folder this app does not own, so it can outlive the
        // file it names. Checked once at startup rather than trusted: a stale entry showed the
        // track as downloaded, refused to fetch it again, and then would not play.
        val onDisk = withContext(Dispatchers.IO) { DesktopDownloadManager.verified(downloads) }
        if (onDisk.size != downloads.size) {
            DesktopTrackLog.log(
                "downloads: ${downloads.size - onDisk.size} recorded file(s) are gone; forgetting them",
            )
            downloads = onDisk
            persistence.saveDownloads(onDisk)
        }
        loadHome()
    }

    // The feed is the account's: signing in, out or across to another channel is a different Home,
    // and Recently Played only exists with an account behind it.
    LaunchedEffect(youtubeSignedIn, activeAccountId) {
        val identity = "${DesktopYouTubeAuth.isSignedIn}/${DesktopAccounts.activeAccountId()}"
        if (homeIdentity != null && identity != homeIdentity) loadHome()
    }

    // The mark on a downloaded row, for the shared pages.
    LaunchedEffect(downloads) {
        DesktopAppUiHost.downloaded.value = downloads.mapTo(HashSet(), Song::videoId)
    }

    // What reached disk goes into the library's own record, which is what the Downloads page and
    // every row's tick read from.
    LaunchedEffect(Unit) {
        DesktopDownloadQueue.finished.collect { saved ->
            downloads = (listOf(saved) + downloads.filterNot { it.videoId == saved.videoId }).take(200)
            persistence.saveDownloads(downloads)
        }
    }

    // The charts are period-filtered at read time, so a different period is a different read.
    LaunchedEffect(replayPeriod) {
        replaySummary = withContext(Dispatchers.IO) { DesktopListeningStats.summary(replayPeriod) }
    }

    // Re-scanned when the filter or the folder changes, never on first composition.
    LaunchedEffect(localMusicRevision) {
        if (localMusicRevision == 0) return@LaunchedEffect
        localSongs = withContext(Dispatchers.IO) { DesktopLocalMusic.scan() }
        DesktopLocalMusicWatcher.restart()
    }

    // A track dropped into the folder appears without a relaunch.
    LaunchedEffect(Unit) {
        DesktopLocalMusicWatcher.restart()
        DesktopLocalMusicWatcher.changes.collect {
            localSongs = withContext(Dispatchers.IO) { DesktopLocalMusic.scan() }
        }
    }

    suspend fun loadExplore() {
        if (!youtubeSourceEnabled) {
            exploreState = UiState.Error("YouTube Music is disabled")
            return
        }
        exploreState = UiState.Loading
        DesktopSearchClient.moodAndGenres().fold(
            onSuccess = { sections ->
                exploreState = if (sections.isEmpty()) {
                    UiState.Error("Nothing to explore right now")
                } else {
                    UiState.Success(sections)
                }
            },
            onFailure = { exploreState = UiState.Error(it.message ?: "Could not load Explore") },
        )
    }

    LaunchedEffect(destination, exploreReloads) {
        if (destination == DesktopDestination.EXPLORE &&
            (exploreState is UiState.Loading || exploreReloads > 0)
        ) {
            loadExplore()
        }
    }

    // Category buttons carry no artwork of their own, so a few are resolved at a time from the
    // shelves they open.
    LaunchedEffect(exploreState) {
        val sections = (exploreState as? UiState.Success)?.data ?: return@LaunchedEffect
        if (sections.all { section -> section.items.all { it.thumbnailUrl != null } }) return@LaunchedEffect
        val limiter = Semaphore(4)
        coroutineScope {
            sections.flatMap(MoodGenreSection::items)
                .filter { it.thumbnailUrl == null }
                .forEach { item ->
                    launch {
                        val artwork = limiter.withPermit {
                            DesktopSearchClient.moodGenreArtwork(item.browseId, item.params).getOrNull()
                        } ?: return@launch
                        val current = (exploreState as? UiState.Success)?.data ?: return@launch
                        exploreState = UiState.Success(
                            current.map { section ->
                                section.copy(
                                    items = section.items.map { entry ->
                                        if (entry.browseId == item.browseId && entry.params == item.params) {
                                            entry.copy(thumbnailUrl = artwork)
                                        } else {
                                            entry
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
        }
    }

    /** The next page of the open collection, appended in place. */
    fun loadMoreCollectionSongs() {
        val collection = openedCollection ?: return
        val token = collection.continuation ?: return
        if (collectionLoadingMore) return
        collectionLoadingMore = true
        scope.launch {
            DesktopSearchClient.moreCollectionSongs(token, collection.songs.firstOrNull()?.artist)
                .onSuccess { (songs, next) ->
                    val current = openedCollection
                    if (current != null && current.browseId == collection.browseId) {
                        openedCollection = current.copy(
                            songs = (current.songs + songs).distinctBy(Song::videoId),
                            continuation = next,
                        )
                    }
                }
                .onFailure {
                    DesktopTrackLog.log("collection: no more rows for ${collection.browseId}: ${it.message}")
                    val current = openedCollection
                    if (current != null && current.browseId == collection.browseId) {
                        // Dropped rather than retried on scroll for ever: the trailing row is what
                        // asks, and leaving the token in place would have it ask again on every
                        // frame.
                        openedCollection = current.copy(continuation = null)
                    }
                }
            collectionLoadingMore = false
        }
    }

    fun renameOpenPlaylist(title: String) {
        val collection = openedCollection ?: return
        playlistBusy = true
        playlistError = null
        scope.launch {
            DesktopSearchClient.renamePlaylist(collection.playlistId, title)
                .onSuccess {
                    // Renamed in place as well as re-fetched: the page that ordered it is still on
                    // screen showing the old name, and the library reload behind it takes a moment.
                    openedCollection = openedCollection
                        ?.takeIf { it.browseId == collection.browseId }
                        ?.copy(title = title)
                        ?: openedCollection
                    editPlaylistShelf { items ->
                        items.map { if (it.browseId == collection.browseId) it.copy(title = title) else it }
                    }
                    closePlaylistDialogs()
                    libraryStale = true
                }
                .onFailure {
                    playlistBusy = false
                    playlistError = it.message ?: "Could not rename the playlist"
                }
        }
    }

    fun deleteOpenPlaylist() {
        val collection = openedCollection ?: return
        playlistBusy = true
        playlistError = null
        scope.launch {
            DesktopSearchClient.deletePlaylist(collection.playlistId)
                .onSuccess {
                    closePlaylistDialogs()
                    // Its page is the one open, and a deleted playlist has nothing left to show.
                    if (openedCollection?.browseId == collection.browseId) openedCollection = null
                    editPlaylistShelf { items -> items.filterNot { it.browseId == collection.browseId } }
                    libraryStale = true
                }
                .onFailure {
                    playlistBusy = false
                    playlistError = it.message ?: "Could not delete the playlist"
                }
        }
    }

    /** Takes one row back out of the playlist it is being read in. */
    fun removeFromOpenPlaylist(song: Song) {
        val collection = openedCollection ?: return
        val setVideoId = song.setVideoId ?: return
        scope.launch {
            DesktopSearchClient.removeFromPlaylist(
                collection.playlistId,
                listOf(setVideoId to song.videoId),
            ).onSuccess {
                val current = openedCollection
                if (current != null && current.browseId == collection.browseId) {
                    openedCollection = current.copy(
                        songs = current.songs.filterNot { it.setVideoId == setVideoId },
                    )
                }
            }.onFailure {
                DesktopTrackLog.log("playlist: could not remove ${song.videoId}: ${it.message}")
            }
        }
    }

    /**
     * Sends a new order for the playlist being read. [original] and [reordered] are the same whole
     * list of entries — as it was and as the user left it — since a reorder is sent as moves relative
     * to neighbours and needs every entry to move against.
     */
    fun reorderOpenPlaylist(original: List<Song>, reordered: List<Song>) {
        val collection = openedCollection ?: return
        playlistBusy = true
        playlistError = null
        scope.launch {
            DesktopSearchClient.reorderPlaylist(
                collection.playlistId,
                current = original.mapNotNull { it.setVideoId },
                target = reordered.mapNotNull { it.setVideoId },
            ).onSuccess {
                val current = openedCollection
                if (current != null && current.browseId == collection.browseId) {
                    // The whole list arrived to be reordered, so the page now shows all of it and has
                    // no further page to ask for.
                    openedCollection = current.copy(songs = reordered, continuation = null)
                }
                closePlaylistDialogs()
                libraryStale = true
            }.onFailure {
                playlistBusy = false
                playlistError = it.message ?: "Could not reorder the playlist"
            }
        }
    }

    /**
     * The account's library, re-read whenever the session behind it changes — signing in, switching
     * channel, signing out.
     */
    LaunchedEffect(activeAccountId, activeProfileId, sessionRevision) {
        youtubeSignedIn = DesktopYouTubeAuth.isSignedIn
        if (!DesktopYouTubeAuth.isSignedIn) {
            libraryState = UiState.Success(LibraryPage(emptyList(), emptyList(), emptyList()))
            return@LaunchedEffect
        }
        libraryState = UiState.Loading
        DesktopSearchClient.library().fold(
            onSuccess = { page ->
                // The continuation is a job to run, not page state — it is consumed below.
                libraryState = UiState.Success(page.copy(likedContinuation = null))
                DesktopTrackLog.log(
                    "library: ${page.likedSongs.size} liked, ${page.librarySongs.size} added, " +
                        "shelves ${page.shelves.joinToString { "${it.title}=${it.items.size}" }}",
                )
                val fromAccount = page.likedSongs.mapTo(HashSet(), Song::videoId)
                if (!likedIds.containsAll(fromAccount)) {
                    likedIds = likedIds + fromAccount
                    // Off the drawing thread: an account with a long Liked Music writes a few tens
                    // of kilobytes through the preference store, and that is a disk flush.
                    withContext(Dispatchers.IO) { persistence.saveLikedIds(likedIds) }
                }
                // Liked Music is published to the tab a page budget deep; the rest is followed here
                // for its ids alone, so a liked track past the budget still reads as liked.
                // Tied to the account it was fetched for: the liked set is one shared map, and a
                // sync still running after a switch would seed it with a stranger's likes.
                val syncingFor = activeAccountId
                page.likedContinuation?.let { token ->
                    likedSyncJob?.cancel()
                    likedSyncJob = scope.launch {
                        val extra = HashSet<String>()
                        DesktopSearchClient.syncLikedIds(token, onIds = { ids -> extra += ids })
                        if (syncingFor != DesktopAccounts.activeAccountId()) return@launch
                        if (!likedIds.containsAll(extra)) {
                            likedIds = likedIds + extra
                            withContext(Dispatchers.IO) { persistence.saveLikedIds(likedIds) }
                            DesktopTrackLog.log("library: liked sync added ${extra.size} ids past the first pages")
                        }
                    }
                }
            },
            onFailure = { libraryState = UiState.Error(it.message ?: "Could not load your library") },
        )
        // A session refused mid-flight is dropped by the request itself, so the answer to "are we
        // signed in" is only settled once the call is over.
        youtubeSignedIn = DesktopYouTubeAuth.isSignedIn
    }

    fun here() = DesktopNavEntry(
        destination = destination,
        artist = openedArtist,
        collection = openedCollection,
        mood = selectedMoodGenre,
        showAll = libraryShowAll,
        replay = overlays.replay,
        settings = overlays.settingsPage,
    )

    // Every move between places is a visit, however it was made — the sidebar, a card, a search
    // hit, the player's credits. The place left is what back returns to.
    LaunchedEffect(Unit) {
        var last: DesktopNavEntry? = null
        snapshotFlow { here() }.collect { now ->
            val before = last
            if (before != null && before.key != now.key) {
                if (navRestoring[0]) {
                    navRestoring[0] = false
                } else {
                    navHistory.add(before)
                    if (navHistory.size > NAV_HISTORY_LIMIT) navHistory.removeAt(0)
                }
            }
            // The newest copy of the same place, so an album that has paged in more tracks comes
            // back with them.
            last = now
        }
    }

    /** Back to the place before this one — the top bar's arrow, the mouse's back button, Alt+Left. */
    fun goBack() {
        val entry = navHistory.removeLastOrNull() ?: return
        if (entry.key == here().key) return
        navRestoring[0] = true
        destination = entry.destination
        overlays.replay = entry.replay
        overlays.settingsPage = entry.settings
        collectionError = null
        openedCollection = entry.collection
        libraryShowAll = entry.showAll
        if (entry.mood != selectedMoodGenre) {
            selectedMoodGenre = entry.mood
            moodGenreShelves = UiState.Loading
        }
        if (entry.artist != openedArtist) {
            openedArtist = entry.artist
            if (entry.artist != null) {
                artistState = UiState.Loading
                artistReloads++
            }
        }
    }

    fun openMoodGenre(item: MoodGenre) {
        selectedMoodGenre = item
        moodGenreShelves = UiState.Loading
    }

    LaunchedEffect(selectedMoodGenre, moodGenreReloads) {
        val item = selectedMoodGenre ?: return@LaunchedEffect
        if (moodGenreShelves !is UiState.Loading) return@LaunchedEffect
        DesktopSearchClient.moodGenreShelves(item.browseId, item.params).fold(
            onSuccess = { shelves ->
                moodGenreShelves = if (shelves.isEmpty()) {
                    UiState.Error("Nothing to explore here yet")
                } else {
                    UiState.Success(shelves)
                }
            },
            onFailure = { moodGenreShelves = UiState.Error(it.message ?: "Could not load this category") },
        )
    }

    // The menu the shared player's "…" opens.
    var playerMenuOpen by remember(selectedSong?.videoId) { mutableStateOf(false) }
    // The player's own overflow (volume, the lyrics/output/queue row, the lyric
    // footer), raised from the song menu's "More…" and drawn by the shared
    // player. Held here so the desktop song menu can open it.
    val playerOverflowOpen = remember(selectedSong?.videoId) { mutableStateOf(false) }
    // Requests the song menu sends the player (switch lyrics/queue, translate),
    // flipped here and consumed by it, and the player's state mirrored back so the
    // menu's rows say what is on screen. Not keyed to the track: the player's own
    // panel and translation state outlive a skip, so copies recreated per track
    // would contradict it until it next changed.
    val playerQueueToggle = remember { mutableStateOf(false) }
    val playerQueueShowing = remember { mutableStateOf(false) }
    val playerTranslateToggle = remember { mutableStateOf(false) }
    val playerShowingTranslation = remember { mutableStateOf(false) }
    // Where the player's "…" is, in window coordinates, so its menu hangs off it.
    val playerMenuAnchor = remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    /** The song menu's verbs for [song], as the player sheet offers them. */
    fun playerSongActions(song: Song) = DesktopSongActions(
        signedIn = youtubeSignedIn,
        disliked = song.videoId in dislikedIds,
        downloaded = downloads.any { it.videoId == song.videoId },
        downloadInProgress = song.videoId in downloadInProgress,
        sleepTimerMinutes = sleepTimerMinutes,
        sleepAfterTrack = sleepAfterTrack,
        onToggleDislike = ::toggleDislike,
        onAddToPlaylist = { playlistTarget = it },
        onDownload = ::downloadSong,
        onRemoveDownload = ::removeDownload,
        onStartRadio = ::startRadio,
        onPlayNext = ::playNext,
        onAddToQueue = ::addToQueue,
        // Both leave the player, the way opening a page from Android's sheet collapses it.
        onOpenAlbum = { id ->
            overlays.nowPlaying = false
            openAlbum(id)
        },
        onOpenArtist = { id ->
            overlays.nowPlaying = false
            openArtist(id, song.artist)
        },
        onSleepTimer = { minutes ->
            if (minutes == null) DesktopSleepTimer.cancel() else DesktopSleepTimer.start(minutes)
        },
        onSleepAfterTrack = { DesktopSleepTimer.startAfterTrack() },
        onShare = ::shareSong,
    )

    // What the shared player reads as settings, kept in step with the window's own.
    SideEffect {
        DesktopPlayerSettings.animatedCanvas.value = animatedCanvas
        DesktopPlayerSettings.fullBleedArtwork.value = fullBleedArtwork
        DesktopPlayerSettings.legacyMeshGradient.value = legacyMeshGradient
        DesktopPlayerSettings.lyricsBlur.value = lyricsBlur
        DesktopPlayerSettings.syncedLyrics.value = syncedLyrics
        DesktopPlayerSettings.showNerdStats.value = showNerdStats
        DesktopPlayerSettings.smartFadeEnabled.value = automix
        DesktopPlayerSettings.smartAnalysis.value = playback.smartAnalysis
        DesktopPlayerSettings.smartMixInProgress.value = playback.mixing
        DesktopPlayerSettings.smartTransitionWindow.value = playback.transitionWindow
        DesktopPlayerSettings.lyricsSourceOrder.value =
            DesktopLyricsClient.enabledSources(lyricsOrder, lyricsOn).mapNotNull(::lyricsSourceNamed)
        DesktopPlayerHost.volumeLevel.value = volume
        DesktopPlayerHost.onVolumeChange = {
            volume = it
            persistence.saveString("volume", it.toString())
        }
    }

    // The engine's measured stream, as the player's quality badge and stats line read it.
    LaunchedEffect(playback.streamFormat, playback.streamSourceId) {
        NerdStats.current.value = playback.streamFormat?.nerdSnapshot(playback.streamSourceId)
    }
    LaunchedEffect(playback.searchingBetter, selectedSong?.videoId) {
        NerdStats.racingLossless.value = selectedSong?.videoId
            ?.takeIf { playback.searchingBetter }
            ?.let(::setOf)
            .orEmpty()
    }
    LaunchedEffect(playback.streamFormat, playback.isPlaying) {
        DesktopPlayerHost.pipeline.value = playbackEngine.pipeline()
    }
    DesktopPlayerHost.pipelineDialog = { onDismiss ->
        DesktopAudioPipelineDialog(
            format = playback.streamFormat,
            sourceName = playback.streamSourceId?.let { id ->
                sourceConfigs.firstOrNull { it.id == id }?.displayName
            },
            pipeline = playbackEngine.pipeline(),
            onDismiss = onDismiss,
        )
    }

    // What the player and the lyrics column beside the page both read, worked out once for the two.
    val sharedLyrics = lyrics?.lines
    val sharedLyricsSource = remember(lyrics) { lyrics?.source?.let(::lyricsSourceNamed) }
    val sharedLyricsUnavailable = !lyricsLoading && sharedLyrics.isNullOrEmpty()
    val providerStates = remember(lyricsOrder, lyricsOn, sharedLyricsSource, lyricsLoading) {
        DesktopLyricsClient.enabledSources(lyricsOrder, lyricsOn)
            .mapNotNull(::lyricsSourceNamed)
            .associateWith { source ->
                when {
                    source == sharedLyricsSource -> LyricsProviderState.FOUND
                    lyricsProviderResults.value[source] != null -> lyricsProviderResults.value.getValue(source)
                    lyricsLoading -> LyricsProviderState.FETCHING
                    else -> LyricsProviderState.NOT_FETCHED
                }
            }
    }

    // Every provider the song menu's lyrics drawer lists — all of them, in the
    // saved order, not just the enabled ones — each carrying what its last race
    // turned up so the row can read "Found", "Not found" or "Searching…". The
    // winner is the one the sheet shows now.
    val menuLyricsProviders = remember(
        lyricsOrder,
        lyricsOn,
        lyricsOnly,
        lyricsLoading,
        lyrics?.source,
        lyricsProviderResults.value,
    ) {
        val currentLabel = lyrics?.source
        val asked = lyricsOnly?.let { listOf(it) } ?: DesktopLyricsClient.enabledSources(lyricsOrder, lyricsOn)
        lyricsOrder.map { label ->
            val source = lyricsSourceNamed(label)
            val state = when {
                label == currentLabel -> LyricsProviderState.FOUND
                source != null && lyricsProviderResults.value[source] != null -> lyricsProviderResults.value.getValue(source)
                lyricsLoading && label in asked -> LyricsProviderState.FETCHING
                else -> LyricsProviderState.NOT_FETCHED
            }
            DesktopLyricsProviderRow(label, state, label == currentLabel)
        }
    }

    // The queue's edits, for the player's queue and the queue column alike.
    fun removeFromQueue(at: Int) {
        if (DesktopListenTogether.state.value.controlsLocked) return
        val edited = liveQueue.removeAt(at)
        if (edited !== liveQueue) {
            liveQueue = edited
            saveQueue()
            partySyncHolder[0]?.onLocalIntent()
        }
    }

    fun moveInQueue(from: Int, to: Int) {
        if (DesktopListenTogether.state.value.controlsLocked) return
        val edited = liveQueue.move(from, to)
        if (edited !== liveQueue) {
            liveQueue = edited
            saveQueue()
            partySyncHolder[0]?.onLocalIntent()
        }
    }

    // Clears what the listener queued by hand, as the phone's Clear does; the album and AutoPlay
    // stay where they are.
    fun clearQueue() {
        if (DesktopListenTogether.state.value.controlsLocked) return
        val edited = liveQueue.withoutUserQueue()
        if (edited === liveQueue) return
        liveQueue = edited
        saveQueue()
        partySyncHolder[0]?.onLocalIntent()
    }

    fun seekPlayer(target: Long) {
        if (DesktopListenTogether.state.value.controlsLocked) return
        val duration = playback.durationMs
        playbackEngine.seekTo(
            if (duration > 0) target.coerceIn(0L, duration) else target.coerceAtLeast(0L),
        )
        partySyncHolder[0]?.onLocalIntent()
    }

    val currentTheme by DesktopAppearanceSettings.themeTone.collectAsState()
    val dynamicArtworkTheme by DesktopAppearanceSettings.dynamicArtworkTheme.collectAsState()
    val artworkPalette = rememberDesktopArtworkPalette(selectedSong?.thumbnailUrl.takeIf { dynamicArtworkTheme })

    val activeTheme = if (dynamicArtworkTheme && selectedSong != null && artworkPalette != DesktopArtworkPalette.Default) {
        val p = artworkPalette.primary
        DesktopThemeTone(
            id = "dynamic_artwork",
            title = "Carátula dinámica",
            background = Color(
                red = (p.red * 0.12f).coerceIn(0f, 1f),
                green = (p.green * 0.12f).coerceIn(0f, 1f),
                blue = (p.blue * 0.12f).coerceIn(0f, 1f),
                alpha = 1f,
            ),
            surface = Color(
                red = (p.red * 0.08f).coerceIn(0f, 1f),
                green = (p.green * 0.08f).coerceIn(0f, 1f),
                blue = (p.blue * 0.08f).coerceIn(0f, 1f),
                alpha = 1f,
            ),
            surfaceRaised = Color(
                red = (p.red * 0.20f + 0.06f).coerceIn(0f, 1f),
                green = (p.green * 0.20f + 0.06f).coerceIn(0f, 1f),
                blue = (p.blue * 0.20f + 0.06f).coerceIn(0f, 1f),
                alpha = 1f,
            ),
            glass = p.copy(alpha = 0.20f),
        )
    } else {
        currentTheme
    }

    val activeScheme = desktopColorScheme(activeTheme).let { scheme ->
        if (dynamicArtworkTheme && selectedSong != null && artworkPalette != DesktopArtworkPalette.Default) {
            scheme.copy(primary = artworkPalette.primary, surfaceTint = artworkPalette.primary)
        } else {
            scheme
        }
    }

    MaterialTheme(
        colorScheme = activeScheme,
        typography = desktopTypography(),
    ) {
        val ambientBackdrop by DesktopAppearanceSettings.ambientBackdrop.collectAsState()
        val windowActions = LocalDesktopWindowActions.current
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalNowPlaying provides selectedSong,
        ) {
            DesktopFrame(
                containerColor = activeTheme.background,
                // Only Replay dresses itself unless ambient backdrop is on; everywhere else the chrome sits on the plain surface.
                backdrop = { transparentBase ->
                    val ambientArtworkUrl = replaySummary.songs.firstOrNull()?.song?.thumbnailUrl
                        .takeIf { overlays.replay }
                        ?: selectedSong?.thumbnailUrl.takeIf { ambientBackdrop }
                    DesktopPageBackdrop(
                        artworkUrl = ambientArtworkUrl,
                        transparentBase = transparentBase,
                        containerColor = currentTheme.background,
                    )
                },
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyUp) return@onPreviewKeyEvent false
                    when {
                        event.key == Key.MediaPlayPause -> {
                            if (selectedSong != null) togglePlayPauseFromUser()
                            true
                        }
                        event.key == Key.MediaNext -> {
                            playNext()
                            true
                        }
                        event.key == Key.MediaPrevious -> {
                            playPrevious()
                            true
                        }
                        event.key == Key.F11 -> {
                            DesktopWindowMode.toggle()
                            true
                        }
                        event.key == Key.DirectionLeft && event.isAltPressed && !overlays.nowPlaying -> {
                            goBack()
                            true
                        }
                        DesktopPlatform.isMac && event.isMetaPressed && event.key == Key.Comma -> {
                            openSettings()
                            true
                        }
                        DesktopPlatform.isMac && event.isMetaPressed && event.key == Key.F -> {
                            searchFocusRequested = true
                            true
                        }
                        DesktopPlatform.isMac && event.isMetaPressed && event.key == Key.W -> {
                            windowActions?.close?.invoke()
                            true
                        }
                        DesktopPlatform.isMac && event.isMetaPressed && event.key == Key.M -> {
                            windowActions?.minimize?.invoke()
                            true
                        }
                        (event.isMetaPressed || event.isCtrlPressed) && event.key == Key.K -> {
                            overlays.commandPalette = !overlays.commandPalette
                            true
                        }
                        // One layer at a time, innermost first: the player's own side panel, then
                        // the player.
                        event.key == Key.Escape -> when {
                            overlays.commandPalette -> {
                                overlays.commandPalette = false
                                true
                            }
                            overlays.immersiveLyrics -> {
                                overlays.immersiveLyrics = false
                                true
                            }
                            // The player's own layers first — the lyrics, the queue, a
                            // drawer — in the order Android's back reaches them.
                            overlays.nowPlaying && PlayerBack.dispatch() -> true
                            overlays.nowPlaying -> {
                                overlays.nowPlaying = false
                                true
                            }
                            overlays.sidePanel != null -> {
                                overlays.sidePanel = null
                                true
                            }
                            // A page of Settings steps back the way the back button does, unless
                            // one of its prompts is up over it.
                            overlays.settingsPage != null &&
                                !overlays.lastfmLogin && !overlays.listenBrainzToken && !overlays.discordToken -> {
                                goBack()
                                true
                            }
                            else -> false
                        }
                        else -> false
                    }
                },
                topBar = { compact ->
                    DesktopTopBar(
                        compact = compact,
                        accountAvatar = activeAccount?.profiles?.firstOrNull { it.profileId == activeProfileId }?.avatar?.takeIf { it.isNotBlank() }
                            ?: activeAccount?.avatar?.takeIf { it.isNotBlank() }
                            ?: activeAccount?.profiles?.firstOrNull()?.avatar?.takeIf { it.isNotBlank() },
                        onOpenAccounts = { DesktopTrackLog.log("accounts: opening the switcher"); overlays.accounts = true },
                        canGoBack = navHistory.isNotEmpty(),
                        onBack = ::goBack,
                        query = query,
                        onQueryChange = { text ->
                            editQuery(text)
                            if (destination != DesktopDestination.SEARCH || overlays.settingsPage != null) {
                                selectDestination(DesktopDestination.SEARCH)
                            }
                        },
                        onSearch = ::search,
                        focusSearch = searchFocusRequested,
                        onSearchFocused = { searchFocusRequested = false },
                        onToggleMiniPlayer = { overlays.miniPlayer = !overlays.miniPlayer },
                        onFeelingLucky = ::playFeelingLucky,
                        onOpenCommandPalette = { overlays.commandPalette = true },
                        downloadingCount = downloadInProgress.size,
                        onOpenDownloads = { overlays.downloadManager = true },
                    )
                },
                sidebar = {
                    DesktopSidebar(
                        destination = destination,
                        settingsOpen = overlays.settingsPage != null,
                        accountPlaylists = sidebarAccountPlaylists,
                        localPlaylists = playlists,
                        openedCollectionId = openedCollection?.browseId,
                        onDestinationSelected = { dest ->
                            if (dest == DesktopDestination.SEARCH) {
                                searchFocusRequested = true
                            }
                            selectDestination(dest)
                        },
                        onOpenAccountPlaylist = { openShelfItem(it, PLAYLISTS_SHELF) },
                        onOpenLocalPlaylist = ::openPlaylist,
                        onOpenSettings = ::openSettings,
                    )
                },
                bottomBar = { compact ->
                    Column(Modifier.fillMaxWidth()) {
                        if (compact) {
                            DesktopBottomChrome(
                                compact = true,
                                destination = destination,
                                song = selectedSong,
                                isPlaying = playback.isPlaying,
                                onDestinationSelected = ::selectDestination,
                                onExpand = { overlays.nowPlaying = true },
                                onPlayPause = { if (selectedSong != null) togglePlayPauseFromUser() },
                                onNext = ::playNext,
                            )
                        } else {
                            DesktopBottomPlayerBar(
                                song = selectedSong,
                                isPlaying = playback.isPlaying,
                                isLoading = playback.isLoading,
                                previousEnabled = liveQueue.hasPrevious ||
                                    playback.positionMs > BACK_RESTARTS_AFTER_MS,
                                progress = if (playback.durationMs > 0L) {
                                    playback.positionMs.toFloat() / playback.durationMs.toFloat()
                                } else {
                                    0f
                                },
                                positionMs = playback.positionMs,
                                durationMs = playback.durationMs,
                                onSeek = { playbackEngine.seekTo(it) },
                                volume = playback.volume,
                                onVolumeChange = {
                                    volume = it
                                    persistence.saveString("volume", it.toString())
                                },
                                shuffle = shuffle,
                                onShuffleChange = ::setShuffle,
                                repeatMode = repeatMode,
                                onRepeatModeChange = {
                                    repeatMode = it
                                    persistence.saveString("repeat_mode", it.name)
                                },
                                onPlayPause = { if (selectedSong != null) togglePlayPauseFromUser() },
                                onPrevious = ::playPrevious,
                                onNext = ::playNext,
                                onOpenNowPlaying = { overlays.nowPlaying = true },
                                onOpenLyrics = { overlays.toggleSidePanel(DesktopSidePanel.LYRICS) },
                                onOpenQueue = { overlays.toggleSidePanel(DesktopSidePanel.QUEUE) },
                                onOpenAudioOutput = { overlays.audioOutput = true },
                                onOpenSleepTimer = { overlays.sleepTimer = true },
                                isSleepTimerActive = sleepTimerMinutes != null || sleepAfterTrack,
                                sidePanel = overlays.sidePanel,
                                isLiked = selectedSong?.let { it.videoId in likedIds } ?: false,
                                onToggleLike = { selectedSong?.let { toggleLike(it) } },
                                onToggleMiniPlayer = { overlays.miniPlayer = !overlays.miniPlayer },
                                onOpenSongMenu = ::openMenu,
                            )
                        }
                    }
                },
                trailing = {
                    val current = selectedSong
                    DesktopSidePanelColumn(
                        panel = overlays.sidePanel.takeIf { current != null },
                        onClose = { overlays.sidePanel = null },
                        onExpandImmersive = { overlays.immersiveLyrics = true },
                    ) { which ->
                        if (current == null) return@DesktopSidePanelColumn
                        when (which) {
                            DesktopSidePanel.LYRICS -> LyricsSidePanel(
                                song = current,
                                isPlaying = playback.isPlaying,
                                position = playerPosition,
                                lyrics = sharedLyrics,
                                lyricsSource = sharedLyricsSource,
                                lyricsProviderStates = providerStates,
                                onSelectLyricsProvider = { source -> lyricsOnly = source.label },
                                lyricsUnavailable = sharedLyricsUnavailable,
                                onSeek = ::seekPlayer,
                                modifier = Modifier.fillMaxSize(),
                            )
                            DesktopSidePanel.QUEUE -> QueueSidePanel(
                                queue = liveQueue.songs,
                                queueIndex = liveQueue.index,
                                autoplayEnabled = DesktopAutoplay.enabled(partyState, autoplay),
                                onJumpTo = ::playQueueIndex,
                                onRemove = ::removeFromQueue,
                                onMove = ::moveInQueue,
                                onClear = ::clearQueue,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                },
                overlay = {
                    DesktopPlayerSheet(
                        visible = overlays.nowPlaying && selectedSong != null,
                        onDismiss = {
                            DesktopWindowMode.exit()
                            overlays.nowPlaying = false
                        },
                    ) { windowWidth, windowHeight ->
                        val current = selectedSong ?: return@DesktopPlayerSheet
                        val playerSong = trackLinks?.takeIf { it.videoId == current.videoId }
                            ?.let { extra ->
                                current.copy(
                                    artistId = current.artistId ?: extra.artistId,
                                    // Same as on the phone: the watch-queue lookup
                                    // is what carries a channel per credited
                                    // artist, so it is what makes every name in
                                    // the credit line openable.
                                    artists = current.artists.ifEmpty { extra.artists },
                                    albumId = current.albumId ?: extra.albumId,
                                    albumName = current.albumName ?: extra.albumName,
                                )
                            }
                            ?: current
                        NowPlayingScreen(
                            song = playerSong,
                            isPlaying = playback.isPlaying,
                            isLoading = playback.isLoading,
                            position = playerPosition,
                            durationMs = playback.durationMs,
                            audioVersionSwitching = false,
                            qualityUpgraded = false,
                            queue = liveQueue.songs,
                            queueIndex = liveQueue.index,
                            hasPrevious = liveQueue.hasPrevious,
                            hasNext = liveQueue.hasNext,
                            repeatMode = when (repeatMode) {
                                DesktopRepeatMode.OFF -> RepeatModes.OFF
                                DesktopRepeatMode.ONE -> RepeatModes.ONE
                                DesktopRepeatMode.ALL -> RepeatModes.ALL
                            },
                            shuffleEnabled = shuffle,
                            autoplayEnabled = DesktopAutoplay.enabled(partyState, autoplay),
                            signedIn = youtubeSignedIn,
                            accountName = activeAccount?.profiles?.firstOrNull { it.profileId == activeProfileId }?.name?.takeIf(String::isNotBlank)
                                ?: activeAccount?.name?.takeIf(String::isNotBlank),
                            likeStatus = when (current.videoId) {
                                in likedIds -> LikeStatus.LIKE
                                in dislikedIds -> LikeStatus.DISLIKE
                                else -> LikeStatus.INDIFFERENT
                            },
                            onToggleLike = { toggleLike(current) },
                            onPlayPause = ::togglePlayPauseFromUser,
                            onNext = ::playNext,
                            onPrevious = ::playPrevious,
                            onBlockedControl = {
                                DesktopPlayerHost.showMessage("Only the party host can change playback")
                            },
                            onSeek = ::seekPlayer,
                            onSeekFraction = { fraction ->
                                val duration = playbackEngine.state.value.durationMs
                                if (duration > 0) seekPlayer((fraction * duration).toLong())
                            },
                            onToggleShuffle = { setShuffle(!shuffle) },
                            // The phone's order: off, all, one.
                            onCycleRepeat = {
                                repeatMode = when (repeatMode) {
                                    DesktopRepeatMode.OFF -> DesktopRepeatMode.ALL
                                    DesktopRepeatMode.ALL -> DesktopRepeatMode.ONE
                                    DesktopRepeatMode.ONE -> DesktopRepeatMode.OFF
                                }
                                persistence.saveString("repeat_mode", repeatMode.name)
                            },
                            onToggleAutoplay = {
                                setAutoplay(!DesktopAutoplay.enabled(DesktopListenTogether.state.value, autoplay))
                            },
                            onJumpTo = ::playQueueIndex,
                            onRemoveFromQueue = ::removeFromQueue,
                            onMoveInQueue = ::moveInQueue,
                            onClearQueue = ::clearQueue,
                            onOpenMenu = { playerMenuOpen = true },
                            onOpenAlbum = { id ->
                                overlays.nowPlaying = false
                                openAlbum(id)
                            },
                            // The lead credit carries the track's own channel
                            // id and opens straight away; anyone else on the
                            // line has no id, so the page is found from the
                            // name — and only when the answer is that name
                            // exactly, so a fragment like "2115" opens nothing
                            // rather than White 2115.
                            onOpenArtist = { id, name ->
                                overlays.nowPlaying = false
                                if (id != null) {
                                    openArtist(id, name)
                                } else {
                                    scope.launch {
                                        YtMusicRepository.findArtistPageId(name).getOrNull()
                                            ?.let { found -> openArtist(found, name) }
                                    }
                                }
                            },
                            onOpenPlaybackSource = {
                                val id = playerSong.playbackSourceId
                                overlays.nowPlaying = false
                                if (playerSong.playbackSourceType == PlaybackSourceType.BROWSE && id != null) {
                                    openAlbum(id)
                                }
                            },
                            onListenTogether = {
                                overlays.nowPlaying = false
                                overlays.listenTogether = true
                            },
                            lyrics = sharedLyrics,
                            lyricsSource = sharedLyricsSource,
                            lyricsProviderStates = providerStates,
                            onSelectLyricsProvider = { source -> lyricsOnly = source.label },
                            lyricsUnavailable = sharedLyricsUnavailable,
                            lyricsOffsetOpen = false,
                            onDismissLyricsOffset = {},
                            windowWidth = windowWidth,
                            windowHeight = windowHeight,
                            playerOverflowOpen = playerOverflowOpen,
                            playerQueueToggle = playerQueueToggle,
                            playerQueueShowing = playerQueueShowing,
                            playerMenuAnchor = playerMenuAnchor,
                            playerTranslateToggle = playerTranslateToggle,
                            playerShowingTranslation = playerShowingTranslation,
                        )
                        // The sheet's own origin in the window, so the menu below can be
                        // placed on the "…" button the player reports in window terms.
                        val sheetOrigin = remember { mutableStateOf(Offset.Zero) }
                        Spacer(
                            Modifier
                                .matchParentSize()
                                .onGloballyPositioned { sheetOrigin.value = it.positionInWindow() },
                        )
                        if (playerMenuOpen) {
                            // Hung off the "…" itself, wherever the layout put it. Read in
                            // the offset block, so the sheet moving relays this out and
                            // recomposes nothing.
                            val anchorSize = playerMenuAnchor.value?.let { anchor ->
                                with(LocalDensity.current) { DpSize(anchor.width.toDp(), anchor.height.toDp()) }
                            } ?: DpSize(34.dp, 34.dp)
                            Box(
                                Modifier
                                    .offset {
                                        val anchor = playerMenuAnchor.value
                                            ?: return@offset IntOffset(
                                                (windowWidth.roundToPx() - 34.dp.roundToPx()) / 2,
                                                (windowHeight.roundToPx() - 34.dp.roundToPx()) / 2,
                                            )
                                        IntOffset(
                                            (anchor.left - sheetOrigin.value.x).roundToInt(),
                                            (anchor.top - sheetOrigin.value.y).roundToInt(),
                                        )
                                    }
                                    .size(anchorSize),
                            ) {
                                DesktopSongMenuFor(
                                    song = playerSong,
                                    liked = current.videoId in likedIds,
                                    actions = playerSongActions(current),
                                    onToggleLike = { toggleLike(current) },
                                    onRevertToOriginal = {
                                        DesktopOriginalVersion.pin(current.videoId)
                                        playbackEngine.reloadCurrent()
                                    }.takeIf {
                                        playback.streamSourceId != null &&
                                            playback.streamSourceId != "youtube" &&
                                            DesktopMusicSources.hasYouTubeOriginal(current) &&
                                            !DesktopOriginalVersion.isPinned(current.videoId)
                                    },
                                    onUpgradeQuality = {
                                        DesktopOriginalVersion.clear(current.videoId)
                                        playbackEngine.reloadCurrent(forceSourceRefresh = true)
                                    }.takeIf { DesktopOriginalVersion.isPinned(current.videoId) },
                                    onMore = {
                                        playerMenuOpen = false
                                        playerOverflowOpen.value = true
                                    },
                                    onSwitchLyricsQueue = {
                                        playerMenuOpen = false
                                        playerQueueToggle.value = true
                                    },
                                    queueShowing = playerQueueShowing.value,
                                    onTranslate = {
                                        playerMenuOpen = false
                                        playerTranslateToggle.value = true
                                    },
                                    showingTranslation = playerShowingTranslation.value,
                                    translateEnabled = !sharedLyrics.isNullOrEmpty(),
                                    lyricsProviders = menuLyricsProviders,
                                    onSelectLyricsProvider = { label -> lyricsOnly = label },
                                    onSearchLyrics = { title, artist -> searchLyrics(title, artist) },
                                    lyricsOffsetMs = lyricsOffsetMs,
                                    onSetLyricsOffset = { DesktopPlayerSettings.setLyricsOffsetMs(it) },
                                    onDismiss = { playerMenuOpen = false },
                                )
                            }
                        }
                        val message by DesktopPlayerHost.messages.collectAsState()
                        LaunchedEffect(message) {
                            if (message != null) {
                                delay(2_500)
                                DesktopPlayerHost.messages.value = null
                            }
                        }
                        message?.let { text ->
                            Text(
                                text = text,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 36.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.Black.copy(alpha = 0.72f))
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                            )
                        }
                    }
                    AnimatedVisibility(
                        visible = overlays.immersiveLyrics && selectedSong != null,
                        enter = fadeIn(tween(250)),
                        exit = fadeOut(tween(200)),
                    ) {
                        val current = selectedSong ?: return@AnimatedVisibility
                        DesktopImmersiveLyricsScreen(
                            song = current,
                            isPlaying = playback.isPlaying,
                            isLoading = playback.isLoading,
                            position = playerPosition,
                            durationMs = playback.durationMs,
                            volume = playback.volume,
                            onVolumeChange = {
                                volume = it
                                persistence.saveString("volume", it.toString())
                            },
                            onSeek = ::seekPlayer,
                            onPlayPause = { togglePlayPauseFromUser() },
                            onPrevious = ::playPrevious,
                            onNext = ::playNext,
                            shuffle = shuffle,
                            onShuffleChange = ::setShuffle,
                            repeatMode = repeatMode,
                            onRepeatModeChange = {
                                repeatMode = it
                                persistence.saveString("repeat_mode", it.name)
                            },
                            lyrics = sharedLyrics,
                            lyricsSource = sharedLyricsSource,
                            lyricsProviderStates = providerStates,
                            onSelectLyricsProvider = { source -> lyricsOnly = source.label },
                            lyricsUnavailable = sharedLyricsUnavailable,
                            volumeNormalizationActive = volumeNormalization && (playback.volumeNormalizationActive || playback.loudnessDb != null),
                            onClose = { overlays.immersiveLyrics = false },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (overlays.accounts) {
                        DesktopAccountSelector(
                            accounts = accounts,
                            activeAccountId = activeAccountId,
                            activeProfileId = activeProfileId,
                            busy = signInBusy != null,
                            onSelect = { account, profile ->
                                DesktopAccounts.select(account.accountId, profile.profileId)
                                activeAccountId = account.accountId
                                activeProfileId = profile.profileId
                            },
                            onAddAccount = { overlays.accounts = false; overlays.signIn = true },
                            onRemoveAccount = { account ->
                                scope.launch {
                                    accounts = withContext(Dispatchers.IO) {
                                        DesktopAccounts.remove(account.accountId)
                                        DesktopAccounts.accounts()
                                    }
                                    activeAccountId = DesktopAccounts.activeAccountId()
                                    activeProfileId = DesktopAccounts.activeProfileId()
                                }
                            },
                            onOpenSettings = { overlays.accounts = false; openSettings() },
                            onDismiss = { overlays.accounts = false },
                        )
                    }


                    if (spotifyImportOpen) {
                        DesktopSpotifyImportDialog(
                            signedIn = youtubeSignedIn,
                            onImported = ::importSpotifyPlaylist,
                            onDismiss = { spotifyImportOpen = false },
                        )
                    }

                    if (overlays.signIn) {
                        DesktopSignInDialog(
                            busy = signInBusy,
                            error = signInError,
                            webSignIn = DesktopWebSignIn.available,
                            onWebSignIn = {
                                webSignInJob?.cancel()
                                webSignInJob = scope.launch {
                                    signInError = null
                                    signInBusy = WEB_SIGN_IN
                                    try {
                                        val cookie = DesktopWebSignIn.captureYouTube()
                                        signIn(cookie, WEB_SIGN_IN)
                                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                        throw cancelled
                                    } catch (failure: Exception) {
                                        DesktopTrackLog.log("sign-in: YouTube Music failed: ${failure.message}")
                                        signInError = failure.message ?: "Could not complete the sign-in."
                                    } finally {
                                        signInBusy = null
                                        webSignInJob = null
                                    }
                                }
                            },
                            onImport = { profile ->
                                scope.launch {
                                    when (val found = withContext(Dispatchers.IO) { DesktopBrowserCookies.read(profile) }) {
                                        is DesktopBrowserCookies.Result.Session ->
                            signIn(found.cookie, profile.label, profile.database.toString())
                                        DesktopBrowserCookies.Result.SignedOut ->
                                            signInError = "${profile.label} is not signed in."
                                        is DesktopBrowserCookies.Result.Unavailable ->
                                            signInError = "${profile.label}: ${found.reason}."
                                    }
                                }
                            },
                            onPaste = { pasted ->
                                scope.launch {
                                    if (!DesktopBrowserCookies.hasSigningSecret(pasted)) {
                                        signInError = "That cookie has no signing secret in it — copy the whole header."
                                    } else {
                                        signIn(pasted, "the cookie you pasted")
                                    }
                                }
                            },
                            onDismiss = {
                                webSignInJob?.cancel()
                                overlays.signIn = false
                                signInError = null
                            },
                        )
                    }
                    if (overlays.playlistDialog || playlistTarget != null) {
                        DesktopPlaylistDialog(
                            song = playlistTarget,
                            accountPlaylists = accountPlaylists,
                            localPlaylists = playlists,
                            signedIn = youtubeSignedIn,
                            canUseAccount = playlistTarget
                                ?.let { DesktopSearchClient.isVideoId(it.videoId) } ?: true,
                            busy = playlistBusy,
                            error = playlistError,
                            onPickAccount = ::addToAccountPlaylist,
                            onPickLocal = ::addToPlaylist,
                            onCreate = ::createPlaylist,
                            onDismiss = ::closePlaylistDialogs,
                        )
                    }
                    if (overlays.rename) {
                        DesktopRenamePlaylistDialog(
                            current = openedCollection?.title.orEmpty(),
                            busy = playlistBusy,
                            error = playlistError,
                            onRename = ::renameOpenPlaylist,
                            onDismiss = ::closePlaylistDialogs,
                        )
                    }
                    if (overlays.delete) {
                        DesktopDeletePlaylistDialog(
                            title = openedCollection?.title.orEmpty(),
                            busy = playlistBusy,
                            error = playlistError,
                            onDelete = ::deleteOpenPlaylist,
                            onDismiss = ::closePlaylistDialogs,
                        )
                    }
                    val reorderCollection = openedCollection?.takeIf { overlays.reorder }
                    if (reorderCollection != null) {
                        DesktopReorderPlaylistDialog(
                            browseId = reorderCollection.browseId,
                            busy = playlistBusy,
                            error = playlistError,
                            onSave = ::reorderOpenPlaylist,
                            onDismiss = ::closePlaylistDialogs,
                            songContextMenu = { song, onDismiss ->
                                DesktopSongMenuFor(
                                    song = song,
                                    liked = song.videoId in likedIds,
                                    actions = songActionsFor(song),
                                    onToggleLike = { toggleLike(song) },
                                    onRevertToOriginal = null,
                                    onUpgradeQuality = null,
                                    onDismiss = onDismiss,
                                )
                            },
                        )
                    }
                    if (overlays.downloadManager) {
                        DesktopDownloadManagerDialog(onDismiss = { overlays.downloadManager = false })
                    }
                    if (overlays.lastfmLogin) {
                        DesktopLastfmLoginDialog(onDismiss = { overlays.lastfmLogin = false })
                    }
                    if (overlays.listenBrainzToken) {
                        DesktopListenBrainzTokenDialog(onDismiss = { overlays.listenBrainzToken = false })
                    }
                    if (overlays.discordToken) {
                        DesktopDiscordTokenDialog(onDismiss = { overlays.discordToken = false })
                    }
                    if (overlays.listenTogether) {
                        DesktopListenTogetherDialog(
                            autoplayEnabled = autoplay,
                            onDismiss = { overlays.listenTogether = false },
                        )
                    }
                    if (overlays.audioOutput) {
                        DesktopAudioOutputDialog(onDismiss = { overlays.audioOutput = false })
                    }
                    if (overlays.sleepTimer) {
                        DesktopSleepTimerDialog(
                            sleepRemainingMs = sleepRemainingMs,
                            onDismiss = { overlays.sleepTimer = false },
                        )
                    }
                    if (overlays.pipeline) {
                        DesktopAudioPipelineDialog(
                            format = playback.streamFormat,
                            sourceName = playback.streamSourceId?.let { id ->
                                sourceConfigs.firstOrNull { it.id == id }?.displayName
                            },
                            pipeline = playbackEngine.pipeline(),
                            onDismiss = { overlays.pipeline = false },
                        )
                    }
                    if (overlays.commandPalette) {
                        DesktopCommandPalette(
                            onDismiss = { overlays.commandPalette = false },
                            onNavigate = { dest ->
                                destination = dest
                                openedCollection = null
                                openedArtist = null
                                overlays.replay = false
                                overlays.settingsPage = null
                            },
                            onOpenSettings = ::openSettings,
                            onOpenSleepTimer = { overlays.sleepTimer = true },
                            onOpenDownloads = { overlays.downloadManager = true },
                            onPlayLucky = ::playFeelingLucky,
                            onTogglePlayPause = ::togglePlayPauseFromUser,
                            onPlayNext = ::playNext,
                            onPlayPrevious = ::playPrevious,
                            onToggleShuffle = { setShuffle(!shuffle) },
                            onOpenLyrics = {
                                overlays.sidePanel = DesktopSidePanel.LYRICS
                            },
                            onOpenQueue = {
                                overlays.sidePanel = DesktopSidePanel.QUEUE
                            },
                            onOpenImmersiveLyrics = {
                                overlays.immersiveLyrics = true
                            },
                            onToggleNormalization = {
                                volumeNormalization = !volumeNormalization
                                persistence.saveBoolean("volume_normalization", volumeNormalization)
                                playbackEngine.setVolumeNormalization(volumeNormalization)
                            },
                            onPlaySong = { song ->
                                playSong(song, startPlaying = true, source = DesktopQueueSource("⌘K Búsqueda", PlaybackSourceType.SEARCH))
                            },
                            onOpenCollection = { browseId, type ->
                                openCollection(browseId, BrowseItem(browseId, "", "", null, type))
                            },
                            onOpenArtist = { browseId, name ->
                                openArtist(browseId, name)
                            },
                        )
                    }
                },
            ) { contentPadding ->
                // Desktop pages start at the same top edge as Library. Only preserve space for the
                // floating player/navigation bar at the bottom in compact windows.
                val sharedPagePadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())
                CompositionLocalProvider(
                    LocalPullToRefreshEnabled provides false,
                    LocalShelfRowChrome provides DesktopShelfRowChrome,
                ) {
                DesktopPointerMenuHost<Song>(
                    menu = { song, onDismiss ->
                        DesktopSongMenuFor(
                            song = song,
                            liked = song.videoId in likedIds,
                            actions = songActionsFor(song),
                            onToggleLike = { toggleLike(song) },
                            onRevertToOriginal = null,
                            onUpgradeQuality = null,
                            onDismiss = onDismiss,
                        )
                    },
                ) { openMenu ->
                pageMenu = openMenu
                CompositionLocalProvider(LocalDesktopContextMenu provides openMenu) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press && event.buttons.isBackPressed) goBack()
                                }
                            }
                        },
                ) {
                    when {
                        // Settings and its pages stand in for the page, held to the width the
                        // Settings card had. Its prompts are still cards, over the page.
                        overlays.settingsPage != null -> Box(
                            Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding()),
                        ) {
                            CompositionLocalProvider(LocalDesktopPanelIsPage provides true) {
                            when (overlays.settingsPage!!) {
                                DesktopSettingsPage.MAIN -> DesktopSettingsScreen(
                                    query = settingsQuery,
                                    onQueryChange = { settingsQuery = it },
                                    listState = settingsListState,
                                    autoplay = autoplay,
                                    onAutoplayChange = ::setAutoplay,
                                    automix = automix,
                                    onAutomixChange = {
                                        automix = it
                                        persistence.saveBoolean("automix", it)
                                    },
                                    automixOnSkip = automixOnSkip,
                                    onAutomixOnSkipChange = {
                                        automixOnSkip = it
                                        persistence.saveBoolean("automix_on_skip", it)
                                    },
                                    automixPerformance = automixPerformance,
                                    onAutomixPerformanceChange = {
                                        automixPerformance = it
                                        persistence.saveString("automix_performance", it.name)
                                    },
                                    automixPhraseBars = automixPhraseBars,
                                    onAutomixPhraseBarsChange = {
                                        automixPhraseBars = it
                                        persistence.saveString("automix_phrase_bars", it.toString())
                                    },
                                    shuffle = shuffle,
                                    onShuffleChange = ::setShuffle,
                                    repeatMode = repeatMode,
                                    onRepeatModeChange = {
                                        repeatMode = it
                                        persistence.saveString("repeat_mode", it.name)
                                    },
                                    playbackSpeed = playbackSpeed,
                                    onPlaybackSpeedChange = {
                                        playbackSpeed = it
                                        persistence.saveString("playback_speed", it.toString())
                                    },
                                    crossfadeSeconds = crossfadeSeconds,
                                    onCrossfadeSecondsChange = {
                                        crossfadeSeconds = it
                                        persistence.saveString("crossfade_seconds", it.toString())
                                    },
                                    animatedCanvas = animatedCanvas,
                                    showNerdStats = showNerdStats,
                                    fullBleedArtwork = fullBleedArtwork,
                                    onAnimatedCanvasChange = {
                                        animatedCanvas = it
                                        persistence.saveBoolean("animated_canvas", it)
                                    },
                                    spotifyCanvasReady = spotifyCanvasCookie.isNotBlank(),
                                    onOpenSpotifyCanvasSetup = { overlays.settingsPage = DesktopSettingsPage.SPOTIFY_CANVAS },
                                    dontRepeatSuggestions = dontRepeatSuggestions,
                                    onDontRepeatSuggestionsChange = {
                                        dontRepeatSuggestions = it
                                        persistence.saveBoolean(KEY_DONT_REPEAT_SUGGESTIONS, it)
                                    },
                                    onChooseLocalMusicFolder = { onChosen ->
                                        DesktopLocalMusic.chooseFolder()?.let { chosen ->
                                            DesktopLocalMusic.setFolder(chosen)
                                            onChosen()
                                            localMusicRevision++
                                        }
                                    },
                                    onLocalMusicFolderChanged = { localMusicRevision++ },
                                    filterNonMusicAudio = filterNonMusicAudio,
                                    onFilterNonMusicAudioChange = {
                                        filterNonMusicAudio = it
                                        persistence.saveBoolean(DesktopLocalMusic.KEY_FILTER_NON_MUSIC_AUDIO, it)
                                        // The scan's result changes with it, so it has to be taken again.
                                        localMusicRevision++
                                    },
                                    syncedLyrics = syncedLyrics,
                                    onSyncedLyricsChange = {
                                        syncedLyrics = it
                                        persistence.saveBoolean(DesktopLyricsClient.KEY_SYNCED_LYRICS, it)
                                    },
                                    lyricsBlur = lyricsBlur,
                                    onLyricsBlurChange = {
                                        lyricsBlur = it
                                        persistence.saveBoolean(DesktopLyricsClient.KEY_LYRICS_BLUR, it)
                                    },
                                    enabledLyricsSources = DesktopLyricsClient.enabledSources(lyricsOrder, lyricsOn),
                                    onOpenLyricsSources = { overlays.settingsPage = DesktopSettingsPage.LYRICS_SOURCES },
                                    onOpenTranslationLanguage = { overlays.settingsPage = DesktopSettingsPage.TRANSLATION_LANGUAGE },
                                    onOpenEqualizer = { overlays.settingsPage = DesktopSettingsPage.EQUALIZER },
                                    onOpenAudioOutput = { overlays.settingsPage = DesktopSettingsPage.AUDIO_OUTPUT },
                                    onOpenListenTogether = { overlays.settingsPage = DesktopSettingsPage.LISTEN_TOGETHER },
                                    onShowNerdStatsChange = {
                                        showNerdStats = it
                                        persistence.saveBoolean("show_nerd_stats", it)
                                    },
                                    onFullBleedArtworkChange = {
                                        fullBleedArtwork = it
                                        persistence.saveBoolean("full_bleed_artwork", it)
                                    },
                                    legacyMeshGradient = legacyMeshGradient,
                                    onLegacyMeshGradientChange = {
                                        legacyMeshGradient = it
                                        persistence.saveBoolean("legacy_mesh_gradient", it)
                                    },
                                    spatialAudio = spatialAudio,
                                    onSpatialAudioChange = {
                                        spatialAudio = it
                                        persistence.saveBoolean("spatial_audio", it)
                                    },
                                    dolbyAtmos = dolbyAtmos,
                                    onDolbyAtmosChange = {
                                        dolbyAtmos = it
                                        DesktopAddonSettings.dolbyAtmosEnabled = it
                                        persistence.saveBoolean("dolby_atmos", it)
                                        DesktopAddonSource.clearCompletedTrackCalls()
                                    },
                                    skipSilence = skipSilence,
                                    onSkipSilenceChange = {
                                        skipSilence = it
                                        persistence.saveBoolean("skip_silence", it)
                                    },
                                    volumeNormalization = volumeNormalization,
                                    onVolumeNormalizationChange = {
                                        volumeNormalization = it
                                        persistence.saveBoolean("volume_normalization", it)
                                        playbackEngine.setVolumeNormalization(it)
                                    },
                                    outputPrecision = outputPrecision,
                                    onOutputPrecisionChange = {
                                        outputPrecision = it
                                        persistence.saveString("output_precision", it)
                                    },
                                    outputSummary = playbackEngine.outputSummary(),
                                    trayIconEnabled = trayIconEnabled,
                                    closeToTray = closeToTray,
                                    onCloseToTrayChange = {
                                        closeToTray = it
                                        persistence.saveBoolean("close_to_tray", it)
                                    },
                                    onTrayIconChange = {
                                        trayIconEnabled = it
                                        persistence.saveBoolean("tray_icon", it)
                                    },
                                    downloadQuality = downloadQuality,
                                    onDownloadQualityChange = {
                                        downloadQuality = it
                                        persistence.saveString("download_quality", it)
                                    },
                                    audioQuality = audioQuality,
                                    onAudioQualityChange = {
                                        audioQuality = it
                                        persistence.saveAudioQuality(it)
                                    },
                                    sleepTimerMinutes = sleepTimerMinutes,
                                    sleepAfterTrack = sleepAfterTrack,
                                    sleepRemainingMs = sleepRemainingMs,
                                    onSleepTimerCycle = ::cycleSleepTimer,
                                    sourceConfigs = sourceConfigs,
                                    sourceStatus = sourceStatus,
                                    onSourceEnabledChange = { config, enabled ->
                                        val next = sourceConfigs.map {
                                            if (it.id == config.id && it.kind != DesktopSourceKind.YOUTUBE) {
                                                it.copy(enabled = enabled)
                                            } else {
                                                it
                                            }
                                        }
                                        persistence.saveSourceConfigs(next)
                                        sourceConfigs = persistence.sourceConfigs()
                                        DesktopModuleSource.reload()
                                    },
                                    onSaveSource = { saved ->
                                        // Replaced by id, so any number of addons can be configured.
                                        val without = sourceConfigs.filterNot {
                                            it.id == saved.id ||
                                                (saved.kind == DesktopSourceKind.CUSTOM_MODULE &&
                                                    it.kind == DesktopSourceKind.CUSTOM_MODULE)
                                        }
                                        persistence.saveSourceConfigs((without + saved).inSourceOrder())
                                        sourceConfigs = persistence.sourceConfigs()
                                        // Whatever was held for this entry describes a server that may no
                                        // longer be the one selected.
                                        DesktopAddonSource.forget(saved.id)
                                        DesktopModuleSource.reload()
                                    },
                                    onRemoveSource = { config ->
                                        if (config.isUserAdded) {
                                            persistence.saveSourceConfigs(sourceConfigs.filterNot { it.id == config.id })
                                            sourceConfigs = persistence.sourceConfigs()
                                            DesktopAddonSource.forget(config.id)
                                            DesktopModuleSource.reload()
                                        }
                                    },
                                    onMoveSource = { config, delta ->
                                        val next = moveUserSource(sourceConfigs, config.id, delta)
                                        persistence.saveSourceConfigs(next)
                                        sourceConfigs = persistence.sourceConfigs()
                                    },
                                    onTestSource = { candidate ->
                                        sourceStatus = sourceStatus + (candidate.id to "Checking source…")
                                        scope.launch {
                                            val health = if (candidate.kind == DesktopSourceKind.ADDON) {
                                                DesktopAddonSource.health(candidate)
                                            } else {
                                                DesktopModuleSource.health(candidate)
                                            }
                                            health.fold(
                                                onSuccess = { sourceStatus = sourceStatus + (candidate.id to it) },
                                                onFailure = {
                                                    sourceStatus = sourceStatus +
                                                        (candidate.id to (it.message ?: "Source unavailable"))
                                                },
                                            )
                                        }
                                    },
                                    onOpenIntegrations = { overlays.settingsPage = DesktopSettingsPage.INTEGRATIONS },
                                    onOpenLicenses = { overlays.settingsPage = DesktopSettingsPage.LICENSES },
                                )
                                DesktopSettingsPage.EQUALIZER -> DesktopEqualizerDialog(onDismiss = ::goBack)
                                DesktopSettingsPage.AUDIO_OUTPUT -> DesktopAudioOutputDialog(onDismiss = ::goBack)
                                DesktopSettingsPage.LISTEN_TOGETHER -> DesktopListenTogetherDialog(
                                    autoplayEnabled = autoplay,
                                    onDismiss = ::goBack,
                                )
                                DesktopSettingsPage.LYRICS_SOURCES -> DesktopLyricsSourcesDialog(
                                    order = lyricsOrder,
                                    enabled = lyricsOn,
                                    prioritizeSyllables = prioritizeSyllables,
                                    onReorder = {
                                        lyricsOrder = it
                                        persistence.saveLyricsSourceOrder(it)
                                    },
                                    onToggle = { name ->
                                        lyricsOn = if (name in lyricsOn) lyricsOn - name else lyricsOn + name
                                        persistence.saveLyricsEnabledSources(lyricsOn)
                                    },
                                    onPrioritizeSyllables = {
                                        prioritizeSyllables = it
                                        persistence.saveBoolean(DesktopLyricsClient.KEY_PRIORITIZE_SYLLABLES, it)
                                    },
                                    onReset = {
                                        lyricsOrder = DesktopLyricsClient.sources.map { it.name }
                                        lyricsOn = lyricsOrder.toSet()
                                        prioritizeSyllables = false
                                        persistence.saveLyricsSourceOrder(lyricsOrder)
                                        persistence.saveLyricsEnabledSources(lyricsOn)
                                        persistence.saveBoolean(DesktopLyricsClient.KEY_PRIORITIZE_SYLLABLES, false)
                                    },
                                    onDismiss = ::goBack,
                                )
                                DesktopSettingsPage.TRANSLATION_LANGUAGE -> DesktopTranslationLanguageDialog(onDismiss = ::goBack)
                                DesktopSettingsPage.SPOTIFY_CANVAS -> DesktopSpotifyCanvasDialog(
                                    onDismiss = ::goBack,
                                    onSaved = { spotifyCanvasCookie = it },
                                )
                                DesktopSettingsPage.INTEGRATIONS -> DesktopIntegrationsDialog(
                                    song = playback.song,
                                    spotify = DesktopSpotifyConnection(
                                        connected = spotifyCanvasCookie.isNotBlank(),
                                        canSignIn = DesktopWebSignIn.available,
                                        signingIn = spotifySignInJob != null,
                                        error = spotifySignInError,
                                        onSignIn = ::signInToSpotify,
                                        onPasteCookie = { overlays.settingsPage = DesktopSettingsPage.SPOTIFY_CANVAS },
                                        onDisconnect = {
                                            DesktopSpotifyToken.setCookie("")
                                            spotifyCanvasCookie = ""
                                        },
                                    ),
                                    onOpenLastfm = { overlays.lastfmLogin = true },
                                    onOpenListenBrainz = { overlays.listenBrainzToken = true },
                                    onOpenDiscordToken = { overlays.discordToken = true },
                                    onDismiss = ::goBack,
                                )
                                DesktopSettingsPage.LICENSES -> DesktopLicensesPage(onDismiss = ::goBack)
                            }
                            }
                        }
                        overlays.replay -> DesktopReplayPage(
                            summary = replaySummary,
                            period = replayPeriod,
                            holder = replayHolder,
                            onPeriodChange = { replayPeriod = it },
                            onPlaySong = { playSong(it) },
                            onOpenArtist = ::openArtistByName,
                            contentPadding = contentPadding,
                        )
                        openedArtist != null -> DesktopArtistPage(
                            state = artistState,
                            fallbackName = openedArtist!!.name,
                            onRetry = {
                                artistState = UiState.Loading
                                artistReloads++
                            },
                            onPlaySongs = { songs, index ->
                                playSongs(songs, index, openedArtist?.let { artistSource(it) })
                            },
                            onShuffle = { songs ->
                                playSongs(songs, 0, openedArtist?.let { artistSource(it) })
                            },
                            onOpenMenu = { song -> openMenu(song) },
                            onShelfItemClick = ::openShelfItem,
                            // A channel subscription is the account's, so a guest is never shown
                            // the button.
                            onToggleSubscription = if (youtubeSignedIn) ::toggleSubscription else null,
                            contentPadding = contentPadding,
                        )
                        openedCollection?.loading == true -> DesktopPageScaffold(contentPadding) {
                            Box(Modifier.fillMaxSize()) {
                                val error = collectionError
                                if (error != null) {
                                    DesktopErrorPage(error) { collectionReloads++ }
                                } else {
                                    val title = openedCollection?.title.orEmpty()
                                    DesktopLoadingPage(if (title.isBlank()) "Loading…" else "Loading $title…")
                                }
                            }
                        }
                        openedCollection != null -> DesktopCollectionPage(
                            collection = openedCollection!!,
                            loadingMore = collectionLoadingMore,
                            onLoadMore = ::loadMoreCollectionSongs,
                            onRename = { overlays.rename = true }.takeIf { openedCollection?.owned == true },
                            onShare = openedCollection
                                ?.takeIf { it.type == BrowseType.ALBUM || it.type == BrowseType.PLAYLIST }
                                ?.let { collection -> { shareCollection(collection) } },
                            onDelete = { overlays.delete = true }.takeIf { openedCollection?.owned == true },
                            onReorder = { overlays.reorder = true }.takeIf {
                                openedCollection?.owned == true && openedCollection?.type == BrowseType.PLAYLIST
                            },
                            onRemoveFromPlaylist = ::removeFromOpenPlaylist
                                .takeIf { openedCollection?.owned == true },
                            likedIds = likedIds,
                            onPlaySongs = { songs, index ->
                                playSongs(songs, index, openedCollection?.let { collectionSource(it) })
                            },
                            onShuffle = { songs ->
                                playSongs(songs.shuffled(), 0, openedCollection?.let { collectionSource(it) })
                            },
                            onDownloadAll = ::downloadAll,
                            animatedCanvas = animatedCanvas,
                            onToggleLike = { song ->
                                toggleLike(song)
                            },
                            onAddToPlaylist = { playlistTarget = it },
                            onDownload = ::downloadSong,
                            downloadedIds = downloads.map(Song::videoId).toSet(),
                            downloadInProgress = downloadInProgress,
                            contentPadding = contentPadding,
                            onOpenArtist = { target ->
                                if (target.startsWith("UC") || target.startsWith("FEmusic_library_privately_owned_artist") || (target.length >= 24 && !target.contains(' '))) {
                                    openArtist(target)
                                } else {
                                    openArtistByName(target)
                                }
                            },
                        )
                        destination == DesktopDestination.LISTEN_NOW -> HomeScreen(
                            state = homeState,
                            listState = homeListState,
                            onItemClick = { item, shelfTitle -> openShelfItem(item, shelfTitle) },
                            onRetry = ::loadHome,
                            refreshing = false,
                            onRefresh = ::loadHome,
                            pullState = rememberPullToRefreshState(),
                            contentPadding = sharedPagePadding,
                            title = null,
                            signedIn = youtubeSignedIn,
                            onSignIn = { overlays.accounts = true },
                            onItemLongPress = { item -> if (item.videoId != null) openMenu(item.toSong()) },
                            onLoadMore = ::loadMoreHome,
                            loadingMore = homeLoadingMore,
                            recentlyPlayedLoading = homeRecentsLoading,
                            leadHero = false,
                        )
                        destination == DesktopDestination.EXPLORE && selectedMoodGenre != null -> Column(Modifier.fillMaxSize()) {
                            MoodGenrePlaylistsScreen(
                                title = selectedMoodGenre!!.title,
                                state = moodGenreShelves,
                                listState = moodGenreListState,
                                onItemClick = { item -> openShelfItem(item, selectedMoodGenre?.title) },
                                onRetry = {
                                    moodGenreShelves = UiState.Loading
                                    moodGenreReloads++
                                },
                                contentPadding = PaddingValues(bottom = sharedPagePadding.calculateBottomPadding()),
                                modifier = Modifier.weight(1f),
                            )
                        }
                        destination == DesktopDestination.EXPLORE -> ExploreScreen(
                            state = exploreState,
                            listState = exploreListState,
                            onCategoryClick = ::openMoodGenre,
                            onRetry = { exploreReloads++ },
                            refreshing = false,
                            onRefresh = { exploreReloads++ },
                            pullState = rememberPullToRefreshState(),
                            contentPadding = sharedPagePadding,
                            showTitle = false,
                        )
                        destination == DesktopDestination.SEARCH -> SearchScreen(
                            query = query,
                            onQueryChange = ::editQuery,
                            filter = searchFilter,
                            // The playing row's indicator, as the phone passes it: the engine's
                            // live state, not the selection a track change publishes behind it.
                            currentSong = playback.song,
                            isPlaying = playback.isPlaying,
                            onFilterChange = {
                                searchFilter = it
                                if (query.isNotBlank()) search()
                            },
                            results = when {
                                searchLoading -> UiState.Loading
                                searchError != null -> UiState.Error(searchError!!)
                                searchCommitted -> UiState.Success(searchRows)
                                else -> null
                            },
                            loadingMore = false,
                            onLoadMore = {},
                            listState = searchListState,
                            scrollResetTrigger = searchScrollReset,
                            focusRequested = searchFocusRequested,
                            onFocusHandled = { searchFocusRequested = false },
                            onSongClick = { songs, index ->
                                songs.getOrNull(index)?.let { song ->
                                    recordSongSearch(song)
                                    playSong(song, source = DesktopQueueSource(DesktopStrings["search", "Search"], PlaybackSourceType.SEARCH))
                                }
                            },
                            onSongLongPress = ::openMenu,
                            onSongSwipe = { song ->
                                if (DesktopAppUiHost.swipeToPlayNext.value) playNext(song) else addToQueue(song)
                            },
                            onTopResultPlay = { song ->
                                recordSongSearch(song)
                                playSong(song, source = DesktopQueueSource(DesktopStrings["search", "Search"], PlaybackSourceType.SEARCH))
                            },
                            onTopResultPlaylist = { song ->
                                recordSongSearch(song)
                                playlistTarget = song
                            },
                            onBrowseClick = { item ->
                                recordBrowseSearch(item)
                                openBrowseItem(item)
                            },
                            history = searchHistory,
                            // The typed text leads the list, put there by the keystroke rather than
                            // taken from the response — the page drops it, as the phone's does.
                            suggestions = if (searchTyping && query.isNotBlank()) {
                                listOf(query) +
                                    searchSuggestions.filterNot { it.equals(query, ignoreCase = true) }
                            } else {
                                emptyList()
                            },
                            typeaheadResults = if (searchTyping && query.isNotBlank()) searchTypeahead else emptyList(),
                            onSubmit = ::search,
                            onSuggestionClick = ::runSearch,
                            onHistoryClick = { entity ->
                                // Going back to a recent does not record it again, as on the phone.
                                when (entity.entityType) {
                                    EntityType.TRACK -> playSong(
                                        Song(
                                            videoId = entity.id,
                                            title = entity.title,
                                            artist = entity.subtitle,
                                            thumbnailUrl = entity.artworkUrl,
                                        ),
                                        source = DesktopQueueSource(entity.title, PlaybackSourceType.SEARCH),
                                    )
                                    EntityType.ARTIST -> openArtist(entity.id, entity.title)
                                    EntityType.ALBUM, EntityType.PLAYLIST -> openBrowseItem(
                                        BrowseItem(
                                            browseId = entity.id,
                                            title = entity.title,
                                            subtitle = entity.subtitle,
                                            thumbnailUrl = entity.artworkUrl,
                                            type = if (entity.entityType == EntityType.ALBUM) BrowseType.ALBUM else BrowseType.PLAYLIST,
                                        ),
                                    )
                                }
                            },
                            onHistoryRemove = DesktopSearchHistory::remove,
                            onHistoryClear = DesktopSearchHistory::clear,
                            onTypeaheadLongPress = ::openMenu,
                            contentPadding = sharedPagePadding,
                            topPadding = 0.dp,
                            showField = false,
                        )
                        destination == DesktopDestination.LIBRARY && libraryShowAll != null -> Column(Modifier.fillMaxSize()) {
                            // The phone keeps the grid's sort beside it, in the bar above; here it sits
                            // at the end of the row the way back is on.
                            Row(
                                Modifier.fillMaxWidth().padding(top = 8.dp, end = 12.dp),
                                horizontalArrangement = Arrangement.End,
                            ) { DesktopLibrarySortMenu() }
                            spotifyLibraryStatus?.takeIf { libraryShowAll?.title == SPOTIFY_SHELF }?.let { status ->
                                Text(
                                    status,
                                    color = DesktopSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = PAGE_GUTTER, vertical = 12.dp),
                                )
                            }
                            LibraryGridPage(
                                shelf = libraryShowAll!!,
                                gridState = libraryGridState,
                                onItemClick = ::openLibraryItem,
                                onItemLongPress = { item -> if (item.videoId != null) openMenu(item.toSong()) },
                                contentPadding = PaddingValues(bottom = sharedPagePadding.calculateBottomPadding()),
                                modifier = Modifier.weight(1f),
                                onNewPlaylist = { overlays.playlistDialog = true }
                                    .takeIf { libraryShowAll?.title == PLAYLISTS_SHELF },
                            )
                        }
                        destination == DesktopDestination.LIBRARY -> LibraryScreen(
                            signedIn = youtubeSignedIn,
                            state = libraryState,
                            listState = libraryListState,
                            onShelfItemClick = ::openLibraryItem,
                            onShelfItemLongPress = { item -> if (item.videoId != null) openMenu(item.toSong()) },
                            onNewPlaylist = { overlays.playlistDialog = true },
                            onImportSpotifyPlaylist = { spotifyImportOpen = true },
                            onShowAll = { shelf -> libraryShowAll = shelf },
                            replay = {
                                // The phone's wallet of Replay cards, or nothing until there is
                                // listening to deal them from.
                                val cards = remember(replaySummary) { replaySummary.heroCards() }
                                if (!replaySummary.isEmpty && cards.isNotEmpty()) {
                                    ShelfRow(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentPadding = PaddingValues(horizontal = PAGE_GUTTER),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        items(cards, key = { it.label }) { card ->
                                            ReplayCreditCard(
                                                label = card.label,
                                                value = card.value,
                                                detail = card.detail,
                                                artworkUrl = card.artworkUrl,
                                                holder = replayHolder,
                                                memberSince = replaySummary.memberSince(),
                                                onClick = { overlays.replay = true },
                                                modifier = Modifier.width(300.dp),
                                            )
                                        }
                                    }
                                }
                            },
                            onSignIn = { overlays.accounts = true },
                            onRetry = ::reloadLibrary,
                            refreshing = false,
                            onRefresh = ::reloadLibrary,
                            pullState = rememberPullToRefreshState(),
                            contentPadding = sharedPagePadding,
                            links = libraryLinks,
                            deviceItems = libraryDeviceItems,
                            showTitle = false,
                        )
                        destination == DesktopDestination.HISTORY -> DesktopHistoryPage(
                            // The account's history when there is one, and what
                            // this computer played when there is not.
                            history = remoteHistory.ifEmpty { history },
                            onSongClick = { playSong(it, source = DesktopQueueSource(DesktopStrings["history", "History"], PlaybackSourceType.HISTORY)) },
                            onDownload = ::downloadSong,
                            onAddToPlaylist = { playlistTarget = it },
                            downloadedIds = downloads.map(Song::videoId).toSet(),
                            downloadInProgress = downloadInProgress,
                            contentPadding = contentPadding,
                            menu = { song -> songMenu(song) },
                        )
                        destination == DesktopDestination.DOWNLOADS -> Box(Modifier.fillMaxSize()) {
                            DesktopDownloadsPage(
                                downloads = downloads,
                                onSongClick = { playSong(it, source = DesktopQueueSource(DesktopStrings["downloads", "Downloads"], PlaybackSourceType.BROWSE)) },
                                contentPadding = contentPadding,
                                menu = { song -> songMenu(song) },
                            )
                            // Only while there is a queue to look at.
                            if (downloadInProgress.isNotEmpty()) {
                                DesktopActionButton(
                                    "Queue · ${downloadInProgress.size}",
                                    BitChordIcons.Download,
                                    onClick = { overlays.downloadManager = true },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 28.dp, bottom = 28.dp + contentPadding.calculateBottomPadding()),
                                )
                            }
                        }
                        destination == DesktopDestination.LOCAL_MUSIC -> DesktopLocalMusicPage(
                            songs = localSongs,
                            onSongClick = { playSong(it, source = DesktopQueueSource(DesktopStrings["local_music", "Local Music"], PlaybackSourceType.BROWSE)) },
                            contentPadding = contentPadding,
                            menu = { song -> songMenu(song) },
                        )
                    }
                }
                }
                }

            }
        }
        }
    }

}

@Composable
private fun DesktopFrame(
    containerColor: Color,
    modifier: Modifier = Modifier,
    /**
     * The open page's own colours, painted across the whole window rather than only the content
     * area, so the title bar, top bar and sidebar take their tint from the page they are framing
     * instead of staying a flat slab beside it.
     */
    /** Receives true when its solid base should be left clear for the system material. */
    backdrop: @Composable (transparentBase: Boolean) -> Unit,
    topBar: @Composable (Boolean) -> Unit,
    sidebar: @Composable () -> Unit,
    bottomBar: @Composable (Boolean) -> Unit,
    /** The column on the far side of the page — the lyrics or the queue, when one is open. */
    trailing: @Composable () -> Unit = {},
    overlay: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // Held out here rather than inside the constraints box.
    val haze = remember { HazeState() }
    // With a system material active, the window stays clear under its chrome. The app-background
    // preference decides whether the page and side column also stay clear or retain their solid
    // gray ground.
    val material by DesktopWindowBackdrop.active.collectAsState()
    val appBackground by DesktopWindowBackdrop.appBackground.collectAsState()
    val glass = material != DesktopBackdrop.OFF
    val materialBehindApp = glass && appBackground
    val maximized by DesktopWindowMode.maximized.collectAsState()
    val roundCorners = DesktopPlatform.isMac && !maximized
    CompositionLocalProvider(LocalDesktopHaze provides haze) {
        Box(
            modifier
                .fillMaxSize()
                .then(if (roundCorners) Modifier.clip(RoundedCornerShape(10.dp)) else Modifier)
                .then(if (glass) Modifier else Modifier.background(containerColor)),
        ) {
            // Both sources of the same state: the chrome blurs the backdrop behind it, and the
            // floating bottom bar blurs the page scrolling under it.
            if (!glass) Box(Modifier.fillMaxSize().hazeSource(haze)) { backdrop(false) }
            Column(Modifier.fillMaxSize()) {
                // Above everything, and outside the box the rest of the window is drawn in, because
                // that is what a title bar is.
                DesktopTitleBar()
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    BoxWithConstraints(Modifier.fillMaxSize()) {
                        val compact = maxWidth < 820.dp
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxWidth().weight(1f)) {
                                if (!compact) sidebar()
                                Column(Modifier.weight(1f).fillMaxHeight()) {
                                    topBar(compact)
                                    Row(Modifier.fillMaxWidth().weight(1f)) {
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .then(
                                                    if (glass && !materialBehindApp) {
                                                        Modifier.background(containerColor)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        ) {
                                            Box(Modifier.fillMaxSize().hazeSource(haze)) {
                                                if (glass) backdrop(materialBehindApp)
                                                content(
                                                    PaddingValues(
                                                        start = 0.dp,
                                                        end = 0.dp,
                                                        bottom = if (compact) 128.dp else 84.dp,
                                                    ),
                                                )
                                            }
                                        }
                                        Box(
                                            Modifier
                                                .fillMaxHeight()
                                                .then(
                                                    if (glass && !materialBehindApp) {
                                                        Modifier.background(containerColor)
                                                    } else {
                                                        Modifier
                                                    },
                                                ),
                                        ) { trailing() }
                                    }
                                }
                            }
                            bottomBar(compact)
                        }
                    }
                    overlay()
                    DesktopDownloadFloatingWidget(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 24.dp, bottom = 96.dp),
                    )
                }
            }
        }
    }
}

/**
 * What [DesktopFrame] paints behind the window.
 *
 * [artworkUrl] is the page's lead artwork when it has one, and the mesh built from it is laid under
 * ink so a page of text stays readable over it.
 */
@Composable
private fun DesktopPageBackdrop(artworkUrl: String?, transparentBase: Boolean, containerColor: Color = DesktopBackground) {
    Box(
        Modifier
            .fillMaxSize()
            .then(if (transparentBase) Modifier else Modifier.background(containerColor)),
    ) {
        if (artworkUrl == null) return@Box
        DesktopMesh(artworkUrl)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.72f),
                        Color.Black.copy(alpha = 0.88f),
                    ),
                ),
            ),
        )
    }
}

/** What the mesh is averaged from — coarse by design. */
/** Whether AutoPlay may offer a song this session has already played or suggested. */
internal const val KEY_DONT_REPEAT_SUGGESTIONS = "dont_repeat_suggestions"

internal const val KEY_LIBRARY_SORT = "library_sort"

private const val MESH_SOURCE_PX = 120



/** Enough to scroll through, short of turning the shelf into the history page. */
/** The Library's "On device" cards — the phone's ids for the two folders, so their cards draw alike. */
internal const val LOCAL_DOWNLOADS_ID = "local:downloads"
internal const val LOCAL_MUSIC_ID = "local:all"
internal const val LOCAL_PLAYLIST_PREFIX = "desktop-playlist:"


/** The artist page that is open, and the name to bill it under until it loads. */
/** The page an artist's tracks were started from. */
private fun artistSource(target: DesktopArtistTarget) =
    DesktopQueueSource(target.name, PlaybackSourceType.BROWSE, target.browseId)

/** The album or playlist a queue was started from. */
private fun collectionSource(collection: DesktopCollection) =
    DesktopQueueSource(collection.title, PlaybackSourceType.BROWSE, collection.browseId)

/** Where a queue was started from, for the player's "Playing from" caption. */
internal data class DesktopQueueSource(
    val title: String,
    val type: PlaybackSourceType,
    val id: String? = null,
)

/**
 * Where this row says it was played from, as the shared queue rules take it — the phone's
 * `play()` makes the same one from the first row of what it was handed.
 */
private fun Song.queueSource(): QueueSource = QueueSource(
    title = playbackSource ?: albumName ?: "Queue",
    type = playbackSourceType ?: PlaybackSourceType.QUEUE,
    id = playbackSourceId,
)

/** Stamps [source] onto a row, leaving one that already names its origin alone. */
private fun Song.withSource(source: DesktopQueueSource?): Song = when {
    source == null || playbackSource != null -> this
    else -> copy(
        playbackSource = source.title,
        playbackSourceType = source.type,
        playbackSourceId = source.id,
    )
}

private data class DesktopArtistTarget(val browseId: String, val name: String)

/** One place the window has been: which page, and whatever was open on it. */
private data class DesktopNavEntry(
    val destination: DesktopDestination,
    val artist: DesktopArtistTarget?,
    val collection: DesktopCollection?,
    val mood: MoodGenre?,
    val showAll: HomeShelf?,
    val replay: Boolean,
    val settings: DesktopSettingsPage?,
) {
    /** What makes two entries the same place: an album that paged in more tracks has not moved. */
    val key: List<Any?>
        get() = listOf(destination, artist?.browseId, collection?.browseId, mood?.browseId, mood?.params, showAll?.title, replay, settings)
}

/** Deep enough for any way back anyone takes; the oldest fall off. */
private const val NAV_HISTORY_LIMIT = 50

/** The strip at the head of the sidebar that holds the window's buttons and can be dragged by. */
internal val SIDEBAR_CAPTION_HEIGHT = 34.dp




/** The track playing, for rows that want to say so. */
internal val LocalNowPlaying = compositionLocalOf<Song?> { null }

/**
 * Opens the app's song menu where the pointer is, for the desktop's own rows.
 *
 * The pages the phone shares already hang right-click off this via their long-press call; a
 * desktop-only row has no such hook, so it reads this and calls it from a [contextClick]. The
 * pages that hold it provide it from the pointer host that plants the menu under the cursor.
 */
internal val LocalDesktopContextMenu = compositionLocalOf<(Song) -> Unit> { {} }
/** Whether this row is the track being played, for the now-playing highlight. */
@Composable
internal fun Song.isNowPlaying(): Boolean = isSameTrackAs(LocalNowPlaying.current)

/** Which of [rows] is the track being played: the one that was played, not every namesake. */
@Composable
internal fun rememberPlayingRowMatcher(rows: List<Song>): (Song) -> Boolean {
    val current = LocalNowPlaying.current
    return remember(rows, current) { playingRowMatcher(rows, current) }
}

/** The frosted pane the floating bars are made of. */
@Composable
internal fun Modifier.desktopBarGlass(shape: Shape): Modifier =
    desktopFrosted(DesktopBarGlass, tintAlpha = 0.55f).border(0.5.dp, DesktopBarEdge, shape)

/** The edge of a chrome pane that meets the page, and so has to dissolve into it. */
internal enum class DesktopChromeEdge { NONE, BOTTOM, END, TOP }

/**
 * The same pane for the window's own chrome — square-edged, and a shade denser, because the sidebar
 * and top bar carry far more text than a floating bar does.
 *
 * [edge] names the side the page is on. The pane's tint is dissolved across the last [fade] of it so
 * the two meet in a gradient rather than on a line.
 */
@Composable
internal fun Modifier.desktopChromeGlass(
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0.2f,
): Modifier = desktopFrosted(
    opaque = DesktopSurface.copy(alpha = 0.88f),
    tintAlpha = 0.46f,
    edge = edge,
    fade = fade,
)

/**
 * The window's own chrome — title bar, top bar, sidebar. Over Windows 11's Mica or Acrylic
 * ([DesktopWindowBackdrop]) it is a light dark tint and nothing else, so DWM's material shows
 * through; the in-app blur would paint the page's backdrop over it. Otherwise the in-app glass.
 *
 * No dissolve over the material: it would fade the tint out into bare material right where the
 * opaque page begins, a lighter stripe along the seam rather than a softer one.
 */
@Composable
internal fun Modifier.desktopWindowGlass(
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0.2f,
): Modifier {
    val backdrop by DesktopWindowBackdrop.active.collectAsState()
    return if (backdrop == DesktopBackdrop.OFF) {
        desktopChromeGlass(edge, fade)
    } else {
        val base = background(Color.Black.copy(alpha = WINDOW_GLASS_TINT))
        if (DesktopPlatform.isMac && (edge == DesktopChromeEdge.BOTTOM || edge == DesktopChromeEdge.TOP)) {
            base.drawBehind {
                // Subtle liquid glass specular highlight along the top edge
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1f,
                )
            }
        } else {
            base
        }
    }
}

/** Enough to keep white text readable over a bright wallpaper, little enough to let it through. */
private const val WINDOW_GLASS_TINT = 0.28f

/**
 * The separators in the window's chrome. Acrylic lets the wallpaper through bright, and the solid
 * dark divider cut across it as a black line; a faint white one reads as an edge in the glass
 * instead. Mica is dark enough for the usual one.
 */
@Composable
internal fun desktopChromeDivider(): Color {
    val backdrop by DesktopWindowBackdrop.active.collectAsState()
    return when {
        backdrop == DesktopBackdrop.ACRYLIC -> Color.White.copy(alpha = 0.14f)
        DesktopPlatform.isMac && backdrop != DesktopBackdrop.OFF -> Color.White.copy(alpha = 0.10f)
        else -> DesktopDivider
    }
}

@Composable
private fun Modifier.desktopFrosted(
    opaque: Color,
    tintAlpha: Float,
    edge: DesktopChromeEdge = DesktopChromeEdge.NONE,
    fade: Float = 0f,
): Modifier {
    val haze = LocalDesktopHaze.current
    val reduceDynamicBlur by DesktopAppearanceSettings.reduceDynamicBlur.collectAsState()
    if (haze == null || reduceDynamicBlur) {
        return this.background(edge.dissolve(opaque, fade) ?: SolidColor(opaque))
    }
    val dissolve = edge.dissolve(Color.Black, fade)
    return this.hazeEffect(state = haze) {
        // Spelled out rather than taken from a preset.
        blurEnabled = true
        backgroundColor = DesktopSurface
        blurRadius = 30.dp
        // Dark enough that white text on the pane always wins, translucent enough that the
        // colour of what is behind it still comes through.
        tints = listOf(HazeTint(DesktopSurface.copy(alpha = tintAlpha)))
        noiseFactor = 0.04f
        // Assigned either way: the scope belongs to the node and outlives the draw, so a pane that
        // stops dissolving — the leading stretch of the top bar, when the window grows back out of
        // compact — would otherwise keep the mask it was given in the other layout.
        mask = dissolve
    }
}

/** The gradient that takes [colour] to nothing across the pane's last [fade]. */
private fun DesktopChromeEdge.dissolve(colour: Color, fade: Float): Brush? {
    if (this == DesktopChromeEdge.NONE || fade <= 0f) return null
    return when (this) {
        DesktopChromeEdge.BOTTOM -> {
            val stops = arrayOf(
                0f to colour,
                (1f - fade).coerceIn(0f, 1f) to colour,
                1f to Color.Transparent,
            )
            Brush.verticalGradient(*stops)
        }
        DesktopChromeEdge.TOP -> {
            val stops = arrayOf(
                0f to Color.Transparent,
                fade.coerceIn(0f, 1f) to colour,
                1f to colour,
            )
            Brush.verticalGradient(*stops)
        }
        DesktopChromeEdge.END -> {
            val stops = arrayOf(
                0f to colour,
                (1f - fade).coerceIn(0f, 1f) to colour,
                1f to Color.Transparent,
            )
            Brush.horizontalGradient(*stops)
        }
        DesktopChromeEdge.NONE -> null
    }
}


/** Android's wording for the Automix line, so both players read alike. */
private fun TrackAnalysisState.label(): String = when (this) {
    TrackAnalysisState.ANALYSED -> "analysed"
    TrackAnalysisState.REFINING -> "analysed, refining…"
    TrackAnalysisState.ANALYSING -> "analysing…"
    TrackAnalysisState.WAITING -> "waiting"
    TrackAnalysisState.FAILED -> "failed"
}


private fun SearchResult.key(): String = when (this) {
    is SearchResult.TopTrack -> "track:${song.videoId}"
    is SearchResult.Track -> "track:${song.videoId}"
    is SearchResult.Browse -> "browse:${item.browseId}"
}

private fun ShelfItem.toSong(): Song {
    val parts = subtitle.split(" • ", " · ", " | ").filter(String::isNotBlank)
    // Android's InnertubeParser uses the first non-type/non-tally segment.
    val artist = parts.firstOrNull {
        !it.matches(SHELF_DURATION) &&
            it.lowercase() !in SHELF_TYPE_WORDS &&
            !it.matches(SHELF_TALLY)
    } ?: "Unknown Artist"
    return Song(
        videoId = videoId.orEmpty(),
        title = title,
        artist = artist,
        thumbnailUrl = thumbnailUrl,
        durationText = parts.firstOrNull { it.matches(SHELF_DURATION) },
    )
}

private fun String.isUnknownArtist(): Boolean =
    trim().equals("unknown", ignoreCase = true) ||
        trim().equals("unknown artist", ignoreCase = true) ||
        trim().equals("unknown artist(s)", ignoreCase = true) ||
        isBlank()

private val SHELF_DURATION = Regex("""\d{1,2}:\d{2}(?::\d{2})?""")
private val SHELF_TALLY = Regex("[0-9,.]+\\s*(?:views?|likes?|songs?)", RegexOption.IGNORE_CASE)
private val SHELF_TYPE_WORDS = setOf("song", "video", "album", "ep", "single", "playlist", "mix")

internal fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

internal fun formatCollectionDuration(totalSeconds: Long): String {
    val minutes = totalSeconds / 60L
    return if (minutes < 60L) {
        "$minutes min"
    } else {
        "${minutes / 60L} hr ${minutes % 60L} min"
    }
}

internal fun formatSleepTimer(ms: Long): String = formatTime(ms)
