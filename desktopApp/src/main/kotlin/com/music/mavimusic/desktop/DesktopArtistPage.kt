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

// --- Extracted Artist Page ---

/**
 * An artist: their picture, the numbers under it, their top songs, and the carousels of what they
 * have released.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DesktopArtistPage(
    state: UiState<ArtistPage>,
    fallbackName: String,
    onRetry: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onShuffle: (List<Song>) -> Unit,
    /** The ⋮ on a top song, or a right-click on it. */
    onOpenMenu: (Song) -> Unit,
    onShelfItemClick: (ShelfItem, String?) -> Unit,
    /** Null for a guest: a channel subscription is the account's. */
    onToggleSubscription: ((SubscriptionState) -> Unit)?,
    contentPadding: PaddingValues,
) {
    DesktopPageScaffold(contentPadding) {
        when (state) {
            UiState.Loading -> Box(Modifier.fillMaxSize()) {
                DesktopLoadingPage("Loading $fallbackName…")
            }
            is UiState.Error -> Box(Modifier.fillMaxSize()) {
                DesktopErrorPage(state.message, onRetry)
            }
            is UiState.Success -> {
                val artist = state.data
                val name = artist.name?.takeIf(String::isNotBlank) ?: fallbackName
                val palette = rememberDesktopArtworkPalette(artist.thumbnailUrl)
                // Android caps the top-songs block and pages it sideways so the release shelves are
                // not buried under a hundred rows.
                val top = artist.songs.take(MAX_ARTIST_SONGS)
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        contentPadding = pagePadding(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        item(key = "artist-header") {
                            DesktopArtistBanner(artist.thumbnailUrl, name, palette)
                        }
                        if (artist.subscriberCountText != null || artist.monthlyListenerCount != null) {
                            item(key = "artist-stats") {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = DesktopPageGutter, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // YouTube's own text already reads "1.2M subscribers" in full,
                                    // so only the number is kept and the label re-said in the app's
                                    // words.
                                    artist.subscriberCountText?.let {
                                        DesktopStatChip(Icons.Rounded.Person, "${it.substringBefore(' ')} subscribers")
                                    }
                                    artist.monthlyListenerCount?.let {
                                        DesktopStatChip(Icons.Rounded.GraphicEq, "${it.substringBefore(' ')} monthly listeners")
                                    }
                                }
                            }
                        }
                        item(key = "artist-actions") {
                            // The phone's artist controls: subscribing first, where saving sits on a
                            // release, then the labelled Play pill and Shuffle.
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = DesktopPageGutter, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val subscription = artist.subscription
                                if (subscription != null && onToggleSubscription != null) {
                                    DesktopCircleButton(
                                        if (subscription.subscribed) MaviMusicIcons.Check else MaviMusicIcons.Plus,
                                        if (subscription.subscribed) {
                                            DesktopStrings["unsubscribe", "Unsubscribe"]
                                        } else {
                                            DesktopStrings["subscribe", "Subscribe"]
                                        },
                                        onClick = { onToggleSubscription(subscription) },
                                    )
                                }
                                DesktopPlayPill(
                                    label = DesktopStrings["play", "Play"],
                                    onClick = { if (top.isNotEmpty()) onPlaySongs(top, 0) },
                                )
                                DesktopCircleButton(MaviMusicIcons.Shuffle, DesktopStrings["shuffle", "Shuffle"], onClick = { onShuffle(top) })
                            }
                        }
                        artist.description?.takeIf(String::isNotBlank)?.let { blurb ->
                            item(key = "artist-about") {
                                Column(Modifier.padding(top = 16.dp)) {
                                    SectionTitle(DesktopStrings["d_about_artist", "About artist"])
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        blurb,
                                        color = DesktopSecondary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = DesktopPageGutter),
                                    )
                                }
                            }
                        }
                        if (top.isNotEmpty()) {
                            // Paged sideways four to a column, the way Home lists Recents.
                            item(key = "artist-top-songs") {
                                Column(Modifier.padding(top = 20.dp, bottom = 6.dp)) {
                                    SectionTitle(DesktopStrings["top_songs", "Top songs"])
                                    Spacer(Modifier.height(8.dp))
                                    BoxWithConstraints {
                                        val columnWidth = trackColumnWidth(maxWidth)
                                        ShelfRow(
                                            contentPadding = PaddingValues(horizontal = DesktopPageGutter),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            items(top.withIndex().chunked(ARTIST_SONGS_PER_COLUMN)) { column ->
                                                Column(Modifier.width(columnWidth)) {
                                                    column.forEach { (index, song) ->
                                                        CompactTrackRow(
                                                            title = song.title,
                                                            subtitle = song.artist,
                                                            thumbnailUrl = song.thumbnailUrl,
                                                            onClick = { onPlaySongs(top, index) },
                                                            onLongPress = { onOpenMenu(song) },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        artist.sections.forEach { shelf ->
                            item(key = "artist-shelf-${shelf.title}") {
                                Box(Modifier.padding(top = 18.dp)) {
                                    DesktopShelf(shelf, onItemClick = onShelfItemClick)
                                }
                            }
                        }
                        if (top.isEmpty() && artist.sections.isEmpty()) {
                            item {
                                DesktopEmptyPage(
                                    Icons.Rounded.Person,
                                    "Nothing to show for $name",
                                    "YouTube Music has no songs or releases on this page.",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The photograph, and the name across the foot of it. */
@Composable
private fun DesktopArtistBanner(url: String?, name: String, palette: DesktopArtworkPalette) {
    Box(Modifier.fillMaxWidth().height(ARTIST_BANNER_HEIGHT)) {
        DesktopArtwork(url, Modifier.matchParentSize(), px = HEADER_ART_PX)
        // Two washes rather than one.
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to palette.primary.copy(alpha = 0.18f),
                    1f to DesktopSurface,
                ),
            ),
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.35f),
                    0.4f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.45f),
                ),
            ),
        )
        Text(
            name,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = DesktopPageGutter, end = DesktopPageGutter, bottom = 18.dp),
        )
    }
}

@Composable
private fun DesktopStatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(DesktopGlass.copy(alpha = 0.55f))
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = DesktopSecondary, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}
