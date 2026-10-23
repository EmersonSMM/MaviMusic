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

// --- Extracted Generic Page Components ---

@Composable
internal fun DesktopShelf(
    shelf: HomeShelf,
    onItemClick: (ShelfItem, String?) -> Unit,
    gutter: Dp = DesktopPageGutter,
) {
    Column {
        SectionTitle(shelf.title, shelf.subtitle, gutter)
        DesktopScrollableRow(gutter = gutter) {
            items(shelf.items, key = { it.videoId ?: it.browseId ?: it.title }) { item ->
                DesktopShelfCard(item) { onItemClick(it, shelf.title) }
            }
        }
    }
}

/**
 * A sideways row with the affordances a desktop needs to move it.
 *
 * A carousel longer than the window has no way to be scrolled with a mouse — the wheel belongs to
 * the page underneath — so this adds the three that work: dragging it, the pointer's horizontal
 * ticks while it is hovered, and a paging arrow at whichever end still has more.
 *
 * Shared rather than repeated: every shelf-shaped row in the app wants the same three.
 */
@Composable
internal fun DesktopScrollableRow(
    gutter: Dp = DesktopPageGutter,
    spacing: Dp = 14.dp,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val rowState = rememberLazyListState()
    DesktopShelfRowChrome.Wrap(rowState) {
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
    }
}

/**
 * What every sideways shelf is wrapped in on the desktop — the shared pages' rows too, through
 * [LocalShelfRowChrome]. A mouse cannot swipe a row, so it gets the arrows it pages with, a drag
 * to pull it by, and the horizontal wheel while the pointer is over it.
 */
internal object DesktopShelfRowChrome : ShelfRowChrome {
    @Composable
    override fun Wrap(state: LazyListState, row: @Composable () -> Unit) {
        val scope = rememberCoroutineScope()
        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()

        /** One screenful, less a sliver so the card at the edge stays in view. */
        fun page(forward: Boolean) {
            val viewport = state.layoutInfo.let { it.viewportEndOffset - it.viewportStartOffset }
            val distance = (viewport - SHELF_PAGE_OVERLAP_PX).coerceAtLeast(SHELF_PAGE_OVERLAP_PX)
            scope.launch { state.animateScrollBy(if (forward) distance.toFloat() else -distance.toFloat()) }
        }

        // Whichever row the pointer is in takes the horizontal ticks; the page underneath keeps the
        // wheel, as it does everywhere else.
        LaunchedEffect(hovered) {
            if (!hovered) return@LaunchedEffect
            DesktopHorizontalScroll.ticks.collect { tick -> state.scrollBy(tick * SHELF_WHEEL_STEP) }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .hoverable(interaction)
                // A row can be taken hold of and pulled with mouse or trackpad drag
                .draggable(
                    state = rememberDraggableState { delta -> scope.launch { state.scrollBy(-delta) } },
                    orientation = Orientation.Horizontal,
                ),
        ) {
            row()
            DesktopShelfArrow(
                alignment = Alignment.CenterStart,
                icon = Icons.Rounded.ChevronLeft,
                description = DesktopStrings["d_scroll_left", "Desplazar a la izquierda"],
                visible = state.canScrollBackward,
                onClick = { page(forward = false) },
            )
            DesktopShelfArrow(
                alignment = Alignment.CenterEnd,
                icon = MaviMusicIcons.ChevronRight,
                description = DesktopStrings["d_scroll_right", "Desplazar a la derecha"],
                visible = state.canScrollForward,
                onClick = { page(forward = true) },
            )
        }
    }
}

/** How much of the outgoing card stays visible after a paged scroll. */
internal const val SHELF_PAGE_OVERLAP_PX = 120

/** How far one notch of a horizontal scroll moves a shelf. */
internal const val SHELF_WHEEL_STEP = 90f

@Composable
internal fun BoxScope.DesktopShelfArrow(
    alignment: Alignment,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(alignment).padding(horizontal = 8.dp),
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isHovered) Color(0xFF222226).copy(alpha = 0.95f) else Color(0xFF141418).copy(alpha = 0.80f))
                .border(1.dp, Color.White.copy(alpha = if (isHovered) 0.40f else 0.18f), CircleShape)
                .hoverable(interactionSource)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, description, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun DesktopShelfCard(item: ShelfItem, onClick: (ShelfItem) -> Unit) {
    val width = 176.dp
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(width)
            // The whole card answers the pointer, artwork and captions together, so the two do not
            // move independently of each other.
            .desktopHoverLift(shape)
            .clickable { onClick(item) },
    ) {
        Box(
            Modifier
                .size(width)
                .clip(shape)
                .background(DesktopGlass)
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
        ) {
            DesktopArtwork(item.thumbnailUrl, Modifier.fillMaxSize(), px = CARD_ART_PX)
        }
        Spacer(Modifier.height(8.dp))
        Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        Text(item.subtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun DesktopSongRow(
    song: Song,
    /** From the list's [rememberPlayingRowMatcher]; null falls back to the name rule. */
    isCurrent: Boolean? = null,
    liked: Boolean,
    onClick: (Song) -> Unit,
    onToggleLike: ((Song) -> Unit)?,
    onDownload: ((Song) -> Unit)? = null,
    onAddToPlaylist: ((Song) -> Unit)? = null,
    downloaded: Boolean = false,
    downloadInProgress: Boolean = false,
    number: Int? = null,
    /** The row's own "…", when the page has a menu to give it. */
    menu: (@Composable (Song) -> Unit)? = null,
) {
    val openContextMenu = LocalDesktopContextMenu.current
    Row(
        Modifier
            .fillMaxWidth()
            .contextClick { openContextMenu(song) }
            .clip(RoundedCornerShape(10.dp))
            .background(DesktopGlass.copy(alpha = 0.38f))
            .desktopHoverFill(corner = 10.dp)
            .clickable { onClick(song) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(
                "$number",
                Modifier.width(28.dp),
                color = DesktopSecondary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(10.dp))
        }
        DesktopArtwork(
            song.thumbnailUrl,
            Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
            px = CARD_ART_PX,
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isCurrent ?: song.isNowPlaying()) Color.White else Color.White.copy(alpha = 0.85f),
            )
            Text(listOfNotNull(song.artist.takeIf(String::isNotBlank), song.durationText).joinToString(" · "), color = DesktopSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        song.sourceQualityBadge()?.let { quality ->
            SourceQualityBadge(
                quality = quality,
                color = DesktopSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        if (onToggleLike != null) {
            IconButton(onClick = { onToggleLike(song) }) { Icon(if (liked) MaviMusicIcons.HeartFilled else MaviMusicIcons.Heart, "Favorite", tint = if (liked) DesktopAccent else DesktopSecondary) }
        }
        if (onDownload != null) {
            if (downloadInProgress) {
                IconButton(onClick = { onDownload(song) }) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DesktopAccent,
                        strokeWidth = 2.dp,
                    )
                }
            } else {
                IconButton(onClick = { if (!downloaded) onDownload(song) }) {
                    Icon(
                        imageVector = if (downloaded) MaviMusicIcons.Download else MaviMusicIcons.Download,
                        contentDescription = if (downloaded) "Downloaded" else "Download",
                        tint = if (downloaded) DesktopAccent else DesktopSecondary,
                    )
                }
            }
        }
        if (onAddToPlaylist != null) {
            IconButton(onClick = { onAddToPlaylist(song) }) {
                Icon(MaviMusicIcons.Plus, DesktopStrings["add_to_playlist", "Add to playlist"], tint = DesktopSecondary)
            }
        }
        // The same menu every other surface opens, so a row on a list page offers what a row on
        // the player does; the row itself plays.
        menu?.invoke(song)
    }
}

/** A page's box, inset from the shell's chrome. */
@Composable
internal fun DesktopPageScaffold(contentPadding: PaddingValues, content: @Composable () -> Unit) {
    val direction = LocalLayoutDirection.current
    Box(
        Modifier.fillMaxSize().padding(
            start = contentPadding.calculateStartPadding(direction),
            end = contentPadding.calculateEndPadding(direction),
            top = contentPadding.calculateTopPadding(),
        ),
    ) {
        CompositionLocalProvider(
            LocalDesktopBottomInset provides contentPadding.calculateBottomPadding(),
        ) {
            content()
        }
    }
}

/** A scrollable's own padding, plus room to clear whatever floats over the page. */
@Composable
internal fun pagePadding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): PaddingValues = PaddingValues(
    start = start,
    top = top,
    end = end,
    bottom = bottom + LocalDesktopBottomInset.current,
)

@Composable
internal fun SectionTitle(
    title: String,
    subtitle: String = "",
    gutter: Dp = DesktopPageGutter,
    /** An action belonging to this heading, drawn at the far end of it. */
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = gutter), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (subtitle.isNotBlank()) Text(subtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun DesktopLoadingPage(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = DesktopAccent)
            Spacer(Modifier.height(14.dp))
            Text(label, color = DesktopSecondary)
        }
    }
}

@Composable
internal fun DesktopErrorPage(message: String, onRetry: () -> Unit) {
    DesktopEmptyPage(Icons.Rounded.Tune, "Something went wrong", message, onRetry, "Try again")
}

@Composable
internal fun DesktopEmptyPage(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, message: String, onAction: (() -> Unit)? = null, actionLabel: String? = null) {
    Column(Modifier.fillMaxWidth().padding(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = DesktopAccent, modifier = Modifier.size(42.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = DesktopSecondary, textAlign = TextAlign.Center)
        if (onAction != null && actionLabel != null) TextButton(onClick = onAction) { Text(actionLabel) }
    }
}

/** A titled block of settings rows. */
/** Material's slider with the step ticks and end-stop dot taken off, as Android draws them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopBareSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = SliderDefaults.colors(
        thumbColor = Color.White,
        activeTrackColor = Color.White,
        inactiveTrackColor = Color.White.copy(alpha = 0.24f),
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        colors = colors,
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                colors = colors,
                drawStopIndicator = null,
                drawTick = { _, _ -> },
            )
        },
        modifier = modifier,
    )
}


@Composable
internal fun DesktopActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(modifier.clip(CircleShape).background(DesktopAccent).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.Black, fontWeight = FontWeight.SemiBold)
    }
}
