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

// --- Extracted Settings Components ---

/** Mobile's section order, with desktop-only groups placed beside their closest mobile counterpart. */
internal enum class DesktopSettingsSection {
    ACCOUNT,
    LISTEN_TOGETHER,
    SOURCES,
    AUDIO_QUALITY,
    DOWNLOADS,
    OUTPUT_PRECISION,
    AUDIO_OUTPUT,
    PLAYBACK,
    DESKTOP_PLAYBACK,
    APPEARANCE,
    LYRICS,
    LOCAL_MUSIC,
    STORAGE,
    MISCELLANEOUS,
    LANGUAGE,
    ADVANCED,
}

@Composable
internal fun DesktopSettingsScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    listState: LazyListState,
    autoplay: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    automix: Boolean,
    onAutomixChange: (Boolean) -> Unit,
    automixOnSkip: Boolean,
    onAutomixOnSkipChange: (Boolean) -> Unit,
    automixPerformance: AutomixPerformanceMode,
    onAutomixPerformanceChange: (AutomixPerformanceMode) -> Unit,
    /** Apple-style Automix phrase length in bars: 0 = adaptive, 8 or 16 = fixed. */
    automixPhraseBars: Int,
    onAutomixPhraseBarsChange: (Int) -> Unit,
    shuffle: Boolean,
    onShuffleChange: (Boolean) -> Unit,
    repeatMode: DesktopRepeatMode,
    onRepeatModeChange: (DesktopRepeatMode) -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeedChange: (Float) -> Unit,
    crossfadeSeconds: Int,
    onCrossfadeSecondsChange: (Int) -> Unit,
    animatedCanvas: Boolean,
    onAnimatedCanvasChange: (Boolean) -> Unit,
    spotifyCanvasReady: Boolean,
    onOpenSpotifyCanvasSetup: () -> Unit,
    dontRepeatSuggestions: Boolean,
    onDontRepeatSuggestionsChange: (Boolean) -> Unit,
    predictiveBuffer: Boolean = DesktopPersistence.predictiveBufferEnabled(),
    onPredictiveBufferChange: (Boolean) -> Unit = {},
    smartTrim: Boolean = DesktopPersistence.smartTrimEnabled(),
    onSmartTrimChange: (Boolean) -> Unit = {},
    preferAudioTracks: Boolean = true,
    onPreferAudioTracksChange: (Boolean) -> Unit = {},
    filterNonMusicAudio: Boolean,
    onFilterNonMusicAudioChange: (Boolean) -> Unit,
    onChooseLocalMusicFolder: (onChosen: () -> Unit) -> Unit,
    onLocalMusicFolderChanged: () -> Unit,
    syncedLyrics: Boolean,
    onSyncedLyricsChange: (Boolean) -> Unit,
    lyricsBlur: Boolean,
    onLyricsBlurChange: (Boolean) -> Unit,
    /** The sources that will actually be asked, in the order they are asked. */
    enabledLyricsSources: List<String>,
    onOpenLyricsSources: () -> Unit,
    onOpenTranslationLanguage: () -> Unit,
    onOpenEqualizer: () -> Unit,
    /** Opens the output-device picker. */
    onOpenAudioOutput: () -> Unit,
    /** Opens the listening-together party. */
    onOpenListenTogether: () -> Unit,
    showNerdStats: Boolean,
    onShowNerdStatsChange: (Boolean) -> Unit,
    fullBleedArtwork: Boolean,
    onFullBleedArtworkChange: (Boolean) -> Unit,
    legacyMeshGradient: Boolean,
    onLegacyMeshGradientChange: (Boolean) -> Unit,
    trayIconEnabled: Boolean,
    closeToTray: Boolean,
    onCloseToTrayChange: (Boolean) -> Unit,
    onTrayIconChange: (Boolean) -> Unit,
    spatialAudio: Boolean,
    onSpatialAudioChange: (Boolean) -> Unit,
    dolbyAtmos: Boolean,
    onDolbyAtmosChange: (Boolean) -> Unit,
    skipSilence: Boolean,
    onSkipSilenceChange: (Boolean) -> Unit,
    volumeNormalization: Boolean = true,
    onVolumeNormalizationChange: (Boolean) -> Unit = {},
    outputPrecision: String,
    onOutputPrecisionChange: (String) -> Unit,
    outputSummary: String,
    downloadQuality: String,
    onDownloadQualityChange: (String) -> Unit,
    audioQuality: DesktopAudioQuality,
    onAudioQualityChange: (DesktopAudioQuality) -> Unit,
    sleepTimerMinutes: Int?,
    sleepAfterTrack: Boolean,
    sleepRemainingMs: Long?,
    onSleepTimerCycle: () -> Unit,
    sourceConfigs: List<DesktopSourceConfig>,
    sourceStatus: Map<String, String>,
    onSourceEnabledChange: (DesktopSourceConfig, Boolean) -> Unit,
    onSaveSource: (DesktopSourceConfig) -> Unit,
    onRemoveSource: (DesktopSourceConfig) -> Unit,
    onMoveSource: (DesktopSourceConfig, Int) -> Unit,
    onTestSource: (DesktopSourceConfig) -> Unit,
    onOpenIntegrations: () -> Unit,
    onOpenLicenses: () -> Unit,
) {
    var editingSource by remember { mutableStateOf<DesktopSourceConfig?>(null) }
    val sourceProbeKey = sourceConfigs
        .filter { it.kind.needsServer && it.isComplete }
        .joinToString { "${it.id}@${it.baseUrl}" }
    LaunchedEffect(sourceProbeKey) {
        sourceConfigs
            .filter { it.kind.needsServer && it.isComplete }
            .forEach(onTestSource)
    }
    DesktopSettingsPageFrame {
            Column(Modifier.fillMaxSize()) {
                val settingsQuery = query
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        DesktopStrings["settings", "Settings"],
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    DesktopSearchField(
                        query = settingsQuery,
                        onQueryChange = onQueryChange,
                        onSearch = {},
                        placeholder = DesktopStrings["settings_search_hint", "Search settings"],
                        modifier = Modifier.padding(start = 24.dp).width(280.dp),
                    )
                }
                CompositionLocalProvider(LocalSettingsQuery provides settingsQuery.trim()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
                ) {
            DesktopSettingsSection.entries.forEach { section ->
            if (section == DesktopSettingsSection.PLAYBACK) item {
                    SettingsGroup(DesktopStrings["playback", "Playback"]) {
                    SettingsToggle(
                        DesktopStrings["predictive_buffer", "Buffer predictivo de la cola"],
                        DesktopStrings[
                            "predictive_buffer_subtitle",
                            "Descarga en segundo plano las siguientes 2–3 canciones para reproducción instantánea sin cortes",
                        ],
                        predictiveBuffer,
                        {
                            onPredictiveBufferChange(it)
                            DesktopPersistence.savePredictiveBufferEnabled(it)
                        },
                    )
                    SettingsToggle(
                        DesktopStrings["smart_trim", "Smart Trim y reproducción continua (Gapless)"],
                        DesktopStrings[
                            "smart_trim_subtitle",
                            "Elimina los silencios digitales de inicio y final de pista para una transición perfecta de 0 ms entre temas de álbum",
                        ],
                        smartTrim,
                        {
                            onSmartTrimChange(it)
                            DesktopPersistence.saveSmartTrimEnabled(it)
                        },
                    )
                    SettingsToggle(
                        DesktopStrings["prefer_audio_tracks", "Modo Solo Canción (Evitar videos musicales)"],
                        DesktopStrings[
                            "prefer_audio_tracks_subtitle",
                            "Prioriza siempre masters oficiales de estudio (audio limpio y carátula 1:1) sobre videoclips con intros",
                        ],
                        preferAudioTracks,
                        {
                            onPreferAudioTracksChange(it)
                            DesktopPersistence.savePreferAudioTracks(it)
                        },
                    )
                    if (!automix) {
                        if (settingsRowVisible(DesktopStrings["crossfade", "Crossfade"])) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                            Text(DesktopStrings["crossfade", "Crossfade"], style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (crossfadeSeconds == 0) "Off" else "${crossfadeSeconds}s",
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            DesktopBareSlider(
                                value = crossfadeSeconds.toFloat(),
                                onValueChange = { onCrossfadeSecondsChange(it.toInt()) },
                                valueRange = 0f..12f,
                                steps = 11,
                            )
                        }
                        }
                    }
                    SettingsToggle(
                        DesktopStrings["automix", "Automix [BETA]"],
                        if (automix) {
                            "Calculates timing and blends transitions from the tracks"
                        } else {
                            "Automatically calculates transition timing without a slider"
                        },
                        automix,
                        onAutomixChange,
                    )
                    SettingsToggle(
                        DesktopStrings["automix_on_skip", "Automix on skip"],
                        DesktopStrings[
                            "automix_on_skip_subtitle",
                            "When you skip ahead, mix into the next song over ~15s instead of cutting",
                        ],
                        automixOnSkip,
                        onAutomixOnSkipChange,
                    )
                    if (automix) {
                        val phraseTitle = DesktopStrings["automix_phrase_length", "Automix phrase length"]
                        val phraseSubtitle =
                            DesktopStrings["automix_phrase_length_subtitle", "Blend over a fixed number of bars, beat-aligned"]
                        if (settingsRowVisible(phraseTitle, phraseSubtitle)) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Text(phraseTitle, fontWeight = FontWeight.Medium)
                                Text(
                                    phraseSubtitle,
                                    color = DesktopSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0, 8, 16).forEach { bars ->
                                        FilterChip(
                                            colors = desktopChipColors(),
                                            selected = automixPhraseBars == bars,
                                            onClick = { onAutomixPhraseBarsChange(bars) },
                                            label = { Text(phraseBarsLabel(bars)) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    phraseBarsDetail(automixPhraseBars),
                                    color = DesktopSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    if (settingsRowVisible(DesktopStrings["automix_performance", "Automix performance"], DesktopStrings["automix_performance_subtitle", "Sets how much CPU background analysis may use"])) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Text(DesktopStrings["automix_performance", "Automix performance"], fontWeight = FontWeight.Medium)
                            Text(
                                DesktopStrings["automix_performance_subtitle", "Sets how much CPU background analysis may use"],
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AutomixPerformanceMode.entries.forEach { mode ->
                                    FilterChip(
                                        colors = desktopChipColors(),
                                        selected = automixPerformance == mode,
                                        onClick = { onAutomixPerformanceChange(mode) },
                                        label = { Text(mode.label()) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                automixPerformance.detail(),
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    val eqOn by DesktopEqualizerSettings.enabled.collectAsState()
                    SettingsToggle(
                        DesktopStrings["skip_silence", "Skip silence"],
                        DesktopStrings["d_shorten_long_gaps_rather_than_playing_them_out", "Shorten long gaps rather than playing them out"],
                        skipSilence,
                        onSkipSilenceChange,
                    )
                    SettingsToggle(
                        DesktopStrings["loudness_normalization", "Normalización de volumen"],
                        DesktopStrings["loudness_normalization_desc", "Iguala el volumen entre canciones (LUFS estándar -14 dB) para evitar cambios bruscos"],
                        volumeNormalization,
                        onVolumeNormalizationChange,
                    )
                    SettingsToggle(
                        DesktopStrings["spatial_audio", "Spatial audio"],
                        DesktopStrings["spatial_audio_subtitle", "Widens stereo tracks for a more immersive feel"],
                        spatialAudio,
                        onSpatialAudioChange,
                    )
                    SettingsToggle(
                        DesktopStrings["dolby_atmos", "Dolby Atmos"],
                        if (DesktopCodecs.supportsDolbyAtmos) {
                            "Prefer an add-on's immersive mix and decode E-AC-3 with FFmpeg"
                        } else {
                            DesktopStrings["dolby_atmos_unavailable", "This device can't play Atmos, so these songs play in their usual version"]
                        },
                        dolbyAtmos && DesktopCodecs.supportsDolbyAtmos,
                        { if (DesktopCodecs.supportsDolbyAtmos) onDolbyAtmosChange(it) },
                    )
                    SettingsNavigationRow(
                        title = DesktopStrings["equalizer", "Equalizer"],
                        subtitle = if (eqOn) {
                            DesktopStrings["equalizer_subtitle", "Tone, seven bands and balance"]
                        } else {
                            DesktopStrings["d_off", "Off"]
                        },
                        onClick = onOpenEqualizer,
                    )
                }
            }
            if (section == DesktopSettingsSection.DESKTOP_PLAYBACK) item {
                SettingsGroup(DesktopStrings["d_desktop_playback", "Desktop playback"]) {
                    SettingsToggle(
                        DesktopStrings["autoplay", "Autoplay"],
                        DesktopStrings["d_keep_the_music_going_with_similar_songs", "Keep the music going with similar songs"],
                        autoplay,
                        onAutoplayChange,
                    )
                    SettingsToggle(
                        DesktopStrings["shuffle", "Shuffle"],
                        DesktopStrings["d_mix_the_order_of_the_current_queue", "Mix the order of the current queue"],
                        shuffle,
                        onShuffleChange,
                    )
                    SettingsRow(
                        MaviMusicIcons.Repeat,
                        DesktopStrings["d_repeat", "Repeat"],
                        "${repeatMode.label()} · Tap to change",
                    ) { onRepeatModeChange(repeatMode.next()) }
                    SettingsRow(
                        Icons.Rounded.Tune,
                        DesktopStrings["d_playback_speed", "Playback speed"],
                        "${"%.2f".format(playbackSpeed)}×",
                    ) {
                        val next = when {
                            playbackSpeed < 0.76f -> 1.0f
                            playbackSpeed < 1.01f -> 1.25f
                            playbackSpeed < 1.26f -> 1.5f
                            playbackSpeed < 1.51f -> 2.0f
                            else -> 0.5f
                        }
                        onPlaybackSpeedChange(next)
                    }
                    SettingsRow(
                        Icons.Rounded.Bedtime,
                        "Sleep timer",
                        when {
                            sleepRemainingMs != null -> "${formatSleepTimer(sleepRemainingMs)} until playback pauses"
                            sleepAfterTrack -> "Pausing when this song ends"
                            else -> "Pause playback after a while"
                        },
                    ) { onSleepTimerCycle() }
                }
            }
            if (section == DesktopSettingsSection.LYRICS) item {
                SettingsGroup(DesktopStrings["open_lyrics", "Lyrics"]) {
                    SettingsToggle(
                        DesktopStrings["synced_lyrics", "Synced lyrics"],
                        DesktopStrings["synced_lyrics_subtitle", "Lights up the words on the player as they're sung"],
                        syncedLyrics,
                        onSyncedLyricsChange,
                    )
                    // Nothing to choose between while the feature is off, and the sources are
                    // third-party services reached on the listener's connection.
                    if (syncedLyrics) {
                        SettingsToggle(
                            DesktopStrings["d_blur_unfocused_lyrics", "Blur unfocused lyrics"],
                            DesktopStrings["d_keeps_the_spotlight_on_the_current_line", "Keeps the spotlight on the current line"],
                            lyricsBlur,
                            onLyricsBlurChange,
                        )
                        SettingsNavigationRow(
                            title = DesktopStrings["lyrics_sources", "Lyrics sources"],
                            subtitle = enabledLyricsSources
                                .joinToString(", ")
                                .ifEmpty { "None. Lyrics will not be fetched." },
                            onClick = onOpenLyricsSources,
                        )
                        val translationLanguage by DesktopTranslationSetting.language.collectAsState()
                        SettingsNavigationRow(
                            title = DesktopStrings["translation_language", "Translation language"],
                            subtitle = DesktopTranslationSetting.describe(translationLanguage),
                            onClick = onOpenTranslationLanguage,
                        )
                    }
                }
            }
            if (section == DesktopSettingsSection.LANGUAGE) item {
                SettingsGroup(DesktopStrings["language", "Language"]) {
                    var languageMenuOpen by remember { mutableStateOf(false) }
                    val chosen by DesktopStrings.language.collectAsState()
                    Box {
                        SettingsRow(
                            Icons.Rounded.Language,
                            DesktopStrings["app_language", "App language"],
                            DesktopStrings.languageLabel(),
                        ) { languageMenuOpen = true }
                        DropdownMenu(languageMenuOpen, onDismissRequest = { languageMenuOpen = false }) {
                            DesktopStrings.available.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.label) },
                                    trailingIcon = if (language.tag == chosen) {
                                        { Icon(MaviMusicIcons.Check, contentDescription = null) }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        DesktopStrings.setLanguage(language.tag)
                                        languageMenuOpen = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
            if (section == DesktopSettingsSection.STORAGE) item {
                SettingsGroup(DesktopStrings["storage", "Storage"]) {
                    var limitMb by remember { mutableStateOf(DesktopMediaCache.limitMb()) }
                    var cleared by remember { mutableStateOf<String?>(null) }
                    if (settingsRowVisible(DesktopStrings["song_cache_limit", "Song cache limit"])) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Text(DesktopStrings["song_cache_limit", "Song cache limit"], style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (limitMb > DesktopMediaCache.WARNING_MB) {
                                "Keeps up to ${formatCacheSize(limitMb)} of downloaded audio on disk. " +
                                    "This can take a noticeable share of your free space."
                            } else {
                                "Keeps downloaded audio on disk for instant seeking and replays"
                            },
                            color = DesktopSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        DesktopBareSlider(
                            value = limitMb.toFloat(),
                            onValueChange = { limitMb = it.roundToInt() },
                            onValueChangeFinished = { DesktopMediaCache.setLimitMb(limitMb) },
                            valueRange = DesktopMediaCache.MIN_LIMIT_MB.toFloat()..
                                DesktopMediaCache.MAX_LIMIT_MB.toFloat(),
                            steps = 18,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(formatCacheSize(limitMb), color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                    }
                    SettingsRow(
                        Icons.Rounded.DeleteSweep,
                        "Clear song cache",
                        cleared ?: "Frees space used by downloaded audio",
                    ) {
                        DesktopMediaCache.clear()
                        cleared = "Song cache cleared"
                    }
                    var imagesCleared by remember { mutableStateOf(false) }
                    SettingsRow(
                        Icons.Rounded.DeleteSweep,
                        "Clear image cache",
                        if (imagesCleared) "Image cache cleared" else "Frees space used by album artwork",
                    ) {
                        DesktopArtworkCache.clear()
                        imagesCleared = true
                    }
                }
            }
            if (section == DesktopSettingsSection.LOCAL_MUSIC) item {
                SettingsGroup(DesktopStrings["local_music", "Local Music"]) {
                    var folderLabel by remember { mutableStateOf(DesktopLocalMusic.folderLabel()) }
                    SettingsRow(
                        Icons.Rounded.Folder,
                        "Local music folder",
                        folderLabel,
                    ) {
                        onChooseLocalMusicFolder { folderLabel = DesktopLocalMusic.folderLabel() }
                    }
                    if (DesktopLocalMusic.folder() != null) {
                        SettingsRow(
                            MaviMusicIcons.Library,
                            DesktopStrings["use_all_audio_folders", "Use all audio folders"],
                            DesktopStrings["use_all_audio_folders_subtitle", "Remove the folder limit and scan music across the device"],
                        ) {
                            DesktopLocalMusic.setFolder(null)
                            folderLabel = DesktopLocalMusic.folderLabel()
                            onLocalMusicFolderChanged()
                        }
                    }
                    SettingsToggle(
                        DesktopStrings["filter_non_music_audio", "Filter non-music audio"],
                        DesktopStrings[
                            "filter_non_music_audio_subtitle",
                            "Hides clips under 30 seconds, WAV files, voice notes, recordings and system sounds",
                        ],
                        filterNonMusicAudio,
                        onFilterNonMusicAudioChange,
                    )
                }
            }
            if (section == DesktopSettingsSection.APPEARANCE) item {
                SettingsGroup(DesktopStrings["appearance", "Apariencia"]) {
                    val currentTheme by DesktopAppearanceSettings.themeTone.collectAsState()
                    val dynamicArtworkTheme by DesktopAppearanceSettings.dynamicArtworkTheme.collectAsState()
                    SettingsToggle(
                        "Adaptar colores a la carátula",
                        "Los tonos de la interfaz y acentos visuales se adaptan dinámicamente a la carátula en reproducción",
                        dynamicArtworkTheme,
                        DesktopAppearanceSettings::setDynamicArtworkTheme,
                    )

                    val themeTitle = "Tema y tono de fondo"
                    val themeSubtitle = if (dynamicArtworkTheme) {
                        "Desactivado temporalmente por la carátula dinámica. Pulsa un tema para fijarlo manualmente."
                    } else {
                        "Personaliza el color de fondo y la atmósfera visual de la app"
                    }
                    if (settingsRowVisible(themeTitle, themeSubtitle)) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            Text(themeTitle, fontWeight = FontWeight.Medium)
                            Text(themeSubtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                DesktopThemeTone.entries.forEach { tone ->
                                    FilterChip(
                                        colors = desktopChipColors(),
                                        selected = !dynamicArtworkTheme && currentTheme == tone,
                                        onClick = { DesktopAppearanceSettings.setThemeTone(tone) },
                                        leadingIcon = {
                                            Box(
                                                Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(tone.background)
                                                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                            )
                                        },
                                        label = { Text(tone.title) },
                                    )
                                }
                            }
                        }
                    }

                    val floatingDownload by DesktopAppearanceSettings.floatingDownloadStatus.collectAsState()
                    SettingsToggle(
                        "Estado flotante de descargas",
                        "Muestra una ventana flotante con progreso en la esquina inferior derecha al descargar canciones",
                        floatingDownload,
                        DesktopAppearanceSettings::setFloatingDownloadStatus,
                    )

                    val reduceDynamicBlur by DesktopAppearanceSettings.reduceDynamicBlur.collectAsState()
                    SettingsToggle(
                        DesktopStrings["reduce_dynamic_blur", "Reduce dynamic blur"],
                        DesktopStrings["reduce_dynamic_blur_subtitle", "Swaps frosted glass for solid fills across the app"],
                        reduceDynamicBlur,
                        DesktopAppearanceSettings::setReduceDynamicBlur,
                    )
                    // Window furniture belongs beside the visual settings it changes, rather than
                    // interrupting playback controls. These rows remain Windows-only.
                    if (DesktopPlatform.drawsOwnWindowFrame) {
                        val titleBar by DesktopTitleBarSetting.enabled.collectAsState()
                        SettingsToggle(
                            DesktopStrings["d_title_bar", "Title bar"],
                            DesktopStrings[
                                "d_a_slim_bar_above_the_toolbar",
                                "A slim bar above the toolbar with the window's own buttons. " +
                                    "Off, the window has no title bar at all.",
                            ],
                            titleBar,
                            DesktopTitleBarSetting::set,
                        )
                    }
                    if (DesktopWindowBackdrop.available) {
                        val backdropChoice by DesktopWindowBackdrop.selected.collectAsState()
                        val backdropActive by DesktopWindowBackdrop.active.collectAsState()
                        val materialTitle = DesktopStrings["d_window_material", "Window material"]
                        val materialSubtitle = DesktopStrings[
                            "d_window_material_subtitle",
                            "Sets the material for the sidebar and top bar",
                        ]
                        if (settingsRowVisible(materialTitle, materialSubtitle)) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Text(materialTitle, fontWeight = FontWeight.Medium)
                                Text(materialSubtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DesktopBackdrop.entries.forEach { option ->
                                        FilterChip(
                                            colors = desktopChipColors(),
                                            selected = backdropChoice == option,
                                            onClick = { DesktopWindowBackdrop.set(option) },
                                            label = { Text(option.label) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    when {
                                        backdropChoice != DesktopBackdrop.OFF && backdropActive == DesktopBackdrop.OFF ->
                                            if (DesktopPlatform.isMac) "Native vibrancy could not be attached"
                                            else "Needs Windows 11 version 22H2 or later"
                                        backdropChoice == DesktopBackdrop.MICA ->
                                            if (DesktopPlatform.isMac) "A subtle blur of your desktop wallpaper"
                                            else "A soft tint taken from your wallpaper"
                                        backdropChoice == DesktopBackdrop.ACRYLIC ->
                                            "A frosted blur of whatever is behind the window"
                                        else -> "Solid, as the rest of the app"
                                    },
                                    color = DesktopSecondary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        val appBackground by DesktopWindowBackdrop.appBackground.collectAsState()
                        SettingsToggle(
                            DesktopStrings["d_app_background", "App background"],
                            DesktopStrings[
                                "d_app_background_subtitle",
                                "Use the window material instead of a solid gray background",
                            ],
                            appBackground,
                            DesktopWindowBackdrop::setAppBackground,
                        )
                        val ambientBackdrop by DesktopAppearanceSettings.ambientBackdrop.collectAsState()
                        SettingsToggle(
                            DesktopStrings["d_ambient_backdrop", "Ambient artwork glow"],
                            DesktopStrings[
                                "d_ambient_backdrop_subtitle",
                                "Diffuse current song artwork colors under the window glass",
                            ],
                            ambientBackdrop,
                            DesktopAppearanceSettings::setAmbientBackdrop,
                        )
                    }
                    SettingsToggle(
                        DesktopStrings["full_screen_cover_art", "Full-screen cover art"],
                        DesktopStrings["full_screen_cover_art_subtitle", "Runs the cover to the edges of the player instead of a square sleeve"],
                        fullBleedArtwork,
                        onFullBleedArtworkChange,
                    )
                    SettingsToggle(
                        DesktopStrings["legacy_mesh_gradient", "Legacy mesh gradient"],
                        DesktopStrings["d_use_the_older_blob_backdrop_behind_the_player", "Use the older blob backdrop behind the player"],
                        legacyMeshGradient,
                        onLegacyMeshGradientChange,
                    )
                    SettingsToggle(
                        DesktopStrings["d_animated_canvas", "Animated canvas"],
                        DesktopStrings["d_show_motion_artwork_when_it_is_available", "Show motion artwork when it is available"],
                        animatedCanvas,
                        onAnimatedCanvasChange,
                    )
                    if (animatedCanvas) {
                        SettingsNavigationRow(
                            title = DesktopStrings["spotify_canvas_setup", "Spotify Canvas setup"],
                            subtitle = if (spotifyCanvasReady) {
                                "Connected"
                            } else {
                                "To use Spotify Canvas, provide your Spotify sp_dc cookie."
                            },
                            onClick = onOpenSpotifyCanvasSetup,
                        )
                    }
                }
            }
            if (section == DesktopSettingsSection.MISCELLANEOUS) item {
                SettingsGroup(DesktopStrings["miscellaneous", "Miscellaneous"]) {
                    SettingsToggle(
                        DesktopStrings["d_dont_repeat_songs_in_current_session", "Don\u2019t repeat songs in current session"],
                        DesktopStrings["d_autoplay_wont_suggest_a_song_already_played_or_suggested", "AutoPlay won\u2019t suggest a song already played or suggested this session"],
                        dontRepeatSuggestions,
                        onDontRepeatSuggestionsChange,
                    )
                    SettingsToggle(
                        DesktopStrings["d_tray_icon", "Tray icon"],
                        DesktopStrings[
                            "d_show_mavimusic_in_the_system_tray_with_playback_controls",
                            "Mostrar MaviMusic en la barra de tareas, con controles de reproducción",
                        ],
                        trayIconEnabled,
                        onTrayIconChange,
                    )
                    if (trayIconEnabled) {
                        SettingsToggle(
                            DesktopStrings["d_keep_playing_when_closed", "Keep playing when closed"],
                            DesktopStrings[
                                "d_closing_the_window_leaves_mavimusic_in_the_tray_instead_o",
                                "Cerrar la ventana minimiza MaviMusic a la barra en vez de salir",
                            ],
                            closeToTray,
                            onCloseToTrayChange,
                        )
                    }
                    val autoStartEnabled by DesktopAutoStart.enabled.collectAsState()
                    SettingsToggle(
                        DesktopStrings["d_start_with_system", "Start with system"],
                        DesktopStrings[
                            "d_start_with_system_subtitle",
                            "Abrir MaviMusic automáticamente al iniciar el equipo",
                        ],
                        autoStartEnabled,
                        { DesktopAutoStart.setEnabled(it) },
                    )
                    val hideVolumeBar by DesktopAppearanceSettings.hideVolumeBar.collectAsState()
                    SettingsToggle(
                        DesktopStrings["hide_volume_bar", "Hide volume bar"],
                        DesktopStrings["hide_volume_bar_subtitle", "Removes the volume slider from the main player"],
                        hideVolumeBar,
                        DesktopAppearanceSettings::setHideVolumeBar,
                    )
                }
            }
            if (section == DesktopSettingsSection.AUDIO_QUALITY) item {
                SettingsGroup(DesktopStrings["audio_quality", "Audio quality"]) {
                    if (settingsRowVisible(DesktopStrings["audio_quality", "Audio quality"])) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DesktopAudioQuality.entries.forEach { rung ->
                                FilterChip(
                                    colors = desktopChipColors(),
                                    selected = audioQuality == rung,
                                    onClick = { onAudioQualityChange(rung) },
                                    label = { Text(rung.label) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // A rung is not a bitrate: it decides which kinds of source are allowed to
                        // answer, so say which.
                        Text(
                            audioQuality.detail,
                            color = DesktopSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    }
                }
            }
            if (section == DesktopSettingsSection.LISTEN_TOGETHER) item {
                SettingsGroup(DesktopStrings["listen_together", "Listen together"]) {
                    SettingsNavigationRow(
                        DesktopStrings["listen_together", "Listen together"],
                        DesktopStrings["listen_together_create_subtitle", "Start a party and share the code."],
                    ) { onOpenListenTogether() }
                }
            }
            if (section == DesktopSettingsSection.AUDIO_OUTPUT) item {
                SettingsGroup(DesktopStrings["audio_output", "Audio output"]) {
                    SettingsNavigationRow(
                        DesktopStrings["pipeline_output_device", "Output device"],
                        DesktopAudioDevices.label(),
                    ) { onOpenAudioOutput() }
                }
            }
            if (section == DesktopSettingsSection.OUTPUT_PRECISION) item {
                SettingsGroup(DesktopStrings["d_output_precision", "Output precision"]) {
                    if (settingsRowVisible(DesktopStrings["d_output_precision", "Output precision"])) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("PCM_16" to "16-bit PCM", "FLOAT_32" to "32-bit float").forEach { (value, label) ->
                                FilterChip(
                                    colors = desktopChipColors(),
                                    selected = outputPrecision == value,
                                    onClick = { onOutputPrecisionChange(value) },
                                    label = { Text(label) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // What was asked for and what the device agreed to are not the same
                        // question, and a setting that silently did not take is worse than one that
                        // says so.
                        Text(
                            "Playing at $outputSummary",
                            color = DesktopSecondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    }
                }
            }
            if (section == DesktopSettingsSection.DOWNLOADS) item {
                SettingsGroup(DesktopStrings["download_channel_name", "Downloads"]) {
                    if (settingsRowVisible(DesktopStrings["download_channel_name", "Downloads"])) {
                        var downloadFolder by remember {
                            mutableStateOf(DesktopDownloadManager.getDownloadDirectory().toString())
                        }
                        val isDefaultFolder = downloadFolder == DesktopDownloadManager.defaultDownloadDirectory().toString()

                        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                            DesktopDownloadRepair()
                            Spacer(Modifier.height(12.dp))
                            var downloadSource by remember { mutableStateOf(DesktopPersistence().string(DesktopDownloadSource.KEY, DesktopDownloadSource.GOOGLE)) }
                            Text("Origen de descarga", fontWeight = FontWeight.Medium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(DesktopDownloadSource.GOOGLE to "Google / YouTube", DesktopDownloadSource.CONFIGURED to "Fuentes configuradas").forEach { (value, label) ->
                                    FilterChip(
                                        selected = downloadSource == value,
                                        onClick = { DesktopPersistence().saveString(DesktopDownloadSource.KEY, value); downloadSource = value },
                                        label = { Text(label) }, colors = desktopChipColors(),
                                    )
                                }
                            }
                            Text("Google descarga directamente del origen. Las pistas de otros proveedores conservan su fuente.", color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(12.dp))
                            Text("Calidad de audio", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                listOf("STANDARD" to "Standard", "HIGH" to "High", "LOSSLESS" to "Lossless").forEach { (value, label) ->
                                    FilterChip(
                                        colors = desktopChipColors(),
                                        selected = downloadQuality == value,
                                        onClick = { onDownloadQualityChange(value) },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Formato de salida: MP3 a 320 kbps. La calidad final depende del audio original; convertir no lo vuelve lossless.",
                                color = DesktopSecondary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        HorizontalDivider(color = DesktopDivider)

                        SettingsRow(
                            Icons.Rounded.Folder,
                            "Carpeta de descargas",
                            downloadFolder,
                        ) {
                            DesktopFolderPicker.pickFolder(downloadFolder)?.let { chosen ->
                                DesktopPersistence().saveString("download_folder", chosen)
                                downloadFolder = chosen
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(start = 56.dp, top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    DesktopFolderPicker.pickFolder(downloadFolder)?.let { chosen ->
                                        DesktopPersistence().saveString("download_folder", chosen)
                                        downloadFolder = chosen
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DesktopSurfaceRaised,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text("Cambiar carpeta...", style = MaterialTheme.typography.labelMedium)
                            }

                            if (!isDefaultFolder) {
                                Button(
                                    onClick = {
                                        DesktopPersistence().saveString("download_folder", "")
                                        downloadFolder = DesktopDownloadManager.defaultDownloadDirectory().toString()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DesktopSurfaceRaised,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text("Restablecer", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Button(
                                onClick = {
                                    runCatching {
                                        java.awt.Desktop.getDesktop().open(java.io.File(downloadFolder))
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DesktopSurfaceRaised,
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(if (DesktopPlatform.isMac) "Abrir en Finder" else "Abrir carpeta", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
            if (section == DesktopSettingsSection.SOURCES) item {
                SettingsGroup(DesktopStrings["sources_order_header", "Sources · tried in this order"]) {
                    val sourcesHeading = DesktopStrings["sources_order_header", "Sources · tried in this order"]
                    val userAddedIds = sourceConfigs.inSourceOrder()
                        .filter(DesktopSourceConfig::isUserAdded)
                        .map(DesktopSourceConfig::id)
                    sourceConfigs.inSourceOrder().forEachIndexed { index, config ->
                        if (!settingsRowVisible(sourcesHeading, config.displayName)) return@forEachIndexed
                        DesktopSourceSettingsRow(
                            position = index + 1,
                            config = config,
                            status = sourceStatus[config.id],
                            skippedByQuality = config.enabled && !audioQuality.permits(config.kind),
                            onEdit = if (config.kind.needsServer) {
                                { editingSource = config }
                            } else {
                                null
                            },
                            onToggle = if (config.kind == DesktopSourceKind.YOUTUBE) {
                                null
                            } else {
                                { onSourceEnabledChange(config, it) }
                            },
                            onMoveUp = if (userAddedIds.indexOf(config.id) > 0) {
                                { onMoveSource(config, -1) }
                            } else null,
                            onMoveDown = if (userAddedIds.indexOf(config.id) in 0 until userAddedIds.lastIndex) {
                                { onMoveSource(config, 1) }
                            } else null,
                        )
                    }
                    // One entry point, and it creates an addon.
                    SettingsRow(
                        MaviMusicIcons.Plus,
                        "Add a source",
                        DesktopSourceKind.ADDON.detail,
                    ) {
                        editingSource = DesktopSourceConfig(
                            id = UUID.randomUUID().toString(),
                            kind = DesktopSourceKind.ADDON,
                        )
                    }
                }
            }
            if (section == DesktopSettingsSection.ACCOUNT) item {
                // One row rather than the four groups this used to be. Every
                // integration option Android has lives behind it; inline, the
                // list was longer than the rest of Settings put together.
                SettingsGroup(DesktopStrings["d_account", "Account"]) {
                    DesktopSessionRecovery()
                    SettingsRow(
                        Icons.Rounded.Share,
                        DesktopStrings["account_integrations", "Account & integrations"],
                        DesktopStrings["d_discord_rich_presence_last_fm_and_listenbrainz", "Discord rich presence, Last.fm and ListenBrainz"],
                        onOpenIntegrations,
                    )
                }
            }
            if (section == DesktopSettingsSection.ADVANCED) item {
                SettingsGroup(DesktopStrings["advanced_options", "Advanced options"]) {
                    SettingsToggle(
                        DesktopStrings["show_nerd_stats", "Show stats for nerds"],
                        DesktopStrings["show_nerd_stats_subtitle", "Codec, bitrate and sample rate on the player"],
                        showNerdStats,
                        onShowNerdStatsChange,
                    )
                }
            }
            }
            item {
                DesktopUpdateSettings()
                DesktopSettingsFooter(onLicenses = onOpenLicenses)
            }
        }
        }
            }
    }

    editingSource?.let { config ->
        DesktopSourceEditorDialog(
            config = config,
            configuredSources = sourceConfigs,
            isNew = sourceConfigs.none { it.id == config.id },
            status = sourceStatus[config.id],
            onDismiss = { editingSource = null },
            onSave = {
                onSaveSource(it)
                editingSource = null
            },
            onRemove = {
                onRemoveSource(config)
                editingSource = null
            },
            onTest = onTestSource,
        )
    }
}

@Composable
private fun DesktopUpdateSettings() {
    val update by DesktopMacUpdater.state.collectAsState()
    SettingsGroup("Actualizaciones") {
        Text("MaviMusic ${DesktopUpdateChecker.currentVersion}", fontWeight = FontWeight.SemiBold)
        Text(update.message, style = MaterialTheme.typography.bodySmall, color = DesktopSecondary)
        if (update.ready) {
            SettingsToggle("Buscar actualizaciones automáticamente", "Comprobar periódicamente si hay una versión nueva.",
                update.automaticChecks, { DesktopMacUpdater.configure(checks = it) })
            SettingsToggle("Descargar e instalar al salir", "Descargar en segundo plano e instalar cuando cierres la aplicación por completo.",
                update.automaticDownloads, { DesktopMacUpdater.configure(downloads = it) })
            TextButton(onClick = DesktopMacUpdater::check) { Text("Buscar actualizaciones…") }
        } else {
            TextButton(onClick = { DesktopExternalLinks.open("https://github.com/EmersonSMM/MaviMusic/releases/latest") }) {
                Text("Ver versiones en GitHub")
            }
        }
    }
}

/** The line at the foot of the settings sheet. */
@Composable
private fun DesktopSettingsFooter(onLicenses: () -> Unit) {
    val version = remember { System.getProperty("mavimusic.version") ?: "1.8" }
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = DesktopAccent, textDecoration = TextDecoration.Underline),
    )
    Text(
        text = buildAnnotatedString {
            append("MaviMusic Desktop $version  •  ")
            withLink(LinkAnnotation.Clickable("licenses", linkStyles) { onLicenses() }) {
                append("Licenses")
            }
        },
        style = MaterialTheme.typography.labelSmall,
        color = DesktopSecondary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 8.dp),
    )
}

/** Everything this build is assembled from, and the terms each part came under. A page of Settings. */
@Composable
internal fun DesktopLicensesPage(onDismiss: () -> Unit) {
    DesktopSettingsPageFrame {
        Text(
            DesktopStrings["d_third_party_licenses", "Third-party licenses"],
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        Text(
            DesktopStrings["d_mavimusic_is_free_software_and_so_is_everything_it_is_bui", "MaviMusic es software libre, y también lo es todo aquello sobre lo que está construido."],
            color = DesktopSecondary,
            style = MaterialTheme.typography.bodySmall,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DesktopLicenses.groups.forEach { group ->
                item(key = group.title) {
                    SettingsGroup(group.title) {
                        group.entries.forEach { entry ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(entry.name, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        entry.license,
                                        color = DesktopAccent,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                                if (entry.note.isNotBlank()) {
                                    Text(
                                        entry.note,
                                        color = DesktopSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Text(
                                    entry.url,
                                    color = DesktopSecondary.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = DesktopAccent, contentColor = Color.Black),
            ) { Text(DesktopStrings["done", "Done"]) }
        }
    }
}

@Composable
private fun DesktopSourceSettingsRow(
    position: Int,
    config: DesktopSourceConfig,
    status: String?,
    skippedByQuality: Boolean = false,
    onEdit: (() -> Unit)?,
    onToggle: ((Boolean) -> Unit)?,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    val enabledAlpha = if (config.enabled) 1f else 0.45f
    Row(
        Modifier
            .fillMaxWidth()
            .desktopRowClickable(enabled = onEdit != null) { onEdit?.invoke() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$position",
            color = DesktopSecondary,
            modifier = Modifier.width(20.dp).alpha(enabledAlpha),
        )
        Icon(
            when (config.kind) {
                DesktopSourceKind.ADDON -> Icons.Rounded.Extension
                DesktopSourceKind.CUSTOM_MODULE, DesktopSourceKind.MODULE -> Icons.Rounded.Extension
                DesktopSourceKind.JIOSAAVN -> Icons.Rounded.GraphicEq
                DesktopSourceKind.YOUTUBE -> Icons.Rounded.PlayCircle
            },
            contentDescription = null,
            tint = DesktopAccent,
            modifier = Modifier.size(21.dp).alpha(enabledAlpha),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).alpha(enabledAlpha)) {
            Text(config.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                when {
                    // The ceiling in force, not the switch, is why this one is being passed over —
                    // worth saying, or the row reads as enabled and silently unused.
                    skippedByQuality -> "Skipped at this audio quality"
                    status != null -> status
                    !config.isComplete -> "Setup required"
                    config.kind.needsServer -> "Checking…"
                    else -> config.kind.detail
                } + if (!config.allowDownloads) {
                    " · " + DesktopStrings["source_downloads_off", "Streaming only"]
                } else {
                    ""
                },
                color = if (skippedByQuality) DesktopAccent else DesktopSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (onMoveUp != null) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.KeyboardArrowUp, "Move source earlier")
            }
        }
        if (onMoveDown != null) {
            IconButton(onClick = onMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.KeyboardArrowDown, "Move source later")
            }
        }
        if (onToggle == null) {
            Text(DesktopStrings["always_on", "Always on"], color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
        } else {
            Switch(checked = config.enabled, onCheckedChange = onToggle, colors = desktopSwitchColors())
        }
    }
}

@Composable
private fun DesktopSourceEditorDialog(
    config: DesktopSourceConfig,
    configuredSources: List<DesktopSourceConfig>,
    isNew: Boolean,
    status: String?,
    onDismiss: () -> Unit,
    onSave: (DesktopSourceConfig) -> Unit,
    onRemove: () -> Unit,
    onTest: (DesktopSourceConfig) -> Unit,
) {
    var label by remember(config.id) { mutableStateOf(config.label) }
    var baseUrl by remember(config.id) { mutableStateOf(config.baseUrl) }
    var checking by remember(config.id) { mutableStateOf(false) }
    var message by remember(config.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val candidate = config.copy(label = label.trim(), baseUrl = baseUrl.trim())

    /** Works out what is actually on the end of the URL before storing it. */
    fun identify(thenSave: Boolean) {
        if (candidate.baseUrl.isBlank() || checking) return
        checking = true
        message = null
        scope.launch {
            val outcome = DesktopSourceFormats.identify(candidate.baseUrl)
            checking = false
            outcome.fold(
                onSuccess = { detected ->
                    when (detected) {
                        is DesktopDetectedFormat.Addon -> {
                            val named = candidate.copy(
                                kind = DesktopSourceKind.ADDON,
                                baseUrl = detected.baseUrl,
                                label = candidate.label.ifBlank { detected.manifest.displayName },
                                allowDownloads = detected.manifest.downloadsAllowed,
                            )
                            val duplicate = configuredSources.duplicateOf(named.baseUrl, exceptId = named.id)
                            if (duplicate != null) {
                                message = "Already added as ${duplicate.displayName}"
                            } else {
                                message = "Addon · ${detected.manifest.displayName}"
                                if (thenSave) onSave(named)
                            }
                        }
                        is DesktopDetectedFormat.ModuleIndex -> {
                            val asIndex = candidate.copy(
                                kind = DesktopSourceKind.CUSTOM_MODULE,
                                baseUrl = detected.url.ifBlank { candidate.baseUrl },
                            )
                            message = "Module index · ${detected.moduleCount} modules"
                            if (thenSave) onSave(asIndex)
                        }
                        is DesktopDetectedFormat.Unsupported -> message = detected.reason
                    }
                },
                onFailure = { message = it.message ?: "Nothing answered at that address" },
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add a source" else config.displayName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = {
                        baseUrl = it
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(DesktopStrings["d_addon_url", "Addon URL"]) },
                    placeholder = { Text("https://example.com/addon") },
                    singleLine = true,
                )
                Text(
                    DesktopStrings[
                        "d_an_addon_server",
                        "Un servidor de complementos — MaviMusic lo consulta por HTTP simple y no ejecuta código suyo. " +
                            "Un índice de módulos compatible también sirve y se guarda como tal.",
                    ],
                    color = DesktopSecondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(DesktopStrings["d_name_optional", "Name (optional)"]) },
                    placeholder = { Text(DesktopStrings["d_my_music_source", "My music source"]) },
                    singleLine = true,
                )
                (message ?: status)?.let {
                    Text(it, color = DesktopSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onRemove) { Text(DesktopStrings["remove", "Remove"]) }
                TextButton(onClick = onDismiss) { Text(DesktopStrings["cancel", "Cancel"]) }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = {
                        onTest(candidate)
                        identify(thenSave = false)
                    },
                    enabled = candidate.baseUrl.isNotBlank() && !checking,
                ) {
                    Text(if (checking) "Checking…" else "Test")
                }
                TextButton(
                    onClick = { identify(thenSave = true) },
                    enabled = candidate.baseUrl.isNotBlank() && !checking,
                ) {
                    Text(DesktopStrings["save", "Save"])
                }
            }
        },
    )
}


private fun AutomixPerformanceMode.label(): String = when (this) {
    AutomixPerformanceMode.EFFICIENT -> "Efficient"
    AutomixPerformanceMode.BALANCED -> "Balanced"
    AutomixPerformanceMode.PERFORMANCE -> "Performance"
}

/** Android's description of each rung. */
private fun AutomixPerformanceMode.detail(): String = when (this) {
    AutomixPerformanceMode.EFFICIENT ->
        "1 thread · lowest heat and battery use · analysis may take longer"
    AutomixPerformanceMode.BALANCED ->
        "2 threads · recommended balance of speed, heat and battery"
    AutomixPerformanceMode.PERFORMANCE ->
        "4 threads · fastest analysis · higher heat and battery use"
}

/** Chip label for an Apple-style Automix phrase length, in bars; 0 is the adaptive default. */
private fun phraseBarsLabel(bars: Int): String = when (bars) {
    8 -> DesktopStrings["automix_phrase_8", "8 bars"]
    16 -> DesktopStrings["automix_phrase_16", "16 bars"]
    else -> DesktopStrings["automix_phrase_auto", "Auto"]
}

/** What the selected phrase length does, shown under the chips. */
private fun phraseBarsDetail(bars: Int): String = when (bars) {
    8 -> DesktopStrings[
        "automix_phrase_8_detail",
        "Mixes over exactly 8 bars, both edges on the beat grid",
    ]
    16 -> DesktopStrings[
        "automix_phrase_16_detail",
        "Mixes over exactly 16 bars, both edges on the beat grid",
    ]
    else -> DesktopStrings[
        "automix_phrase_auto_detail",
        "Chooses each transition's length from the two tracks",
    ]
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    val query = LocalSettingsQuery.current
    // A group whose own name matches shows whole; otherwise only its matching rows do, and the
    // heading goes with them when none are left.
    val wholeGroup = query.isBlank() || title.contains(query, ignoreCase = true)
    val matched = remember(query) { androidx.compose.runtime.mutableStateListOf<String>() }
    val visible = wholeGroup || matched.isNotEmpty()
    // The gap belongs to the group, not to the list: as list spacing, every filtered-out group
    // still left its 18dp behind and the surviving ones sat under a band of empty space.
    Column(if (visible) Modifier.padding(bottom = 20.dp) else Modifier) {
        if (visible) {
            Text(
                title.uppercase(),
                color = DesktopSecondary,
                style = MaterialTheme.typography.labelSmall,
                // In line with the rows' own text, now that no card sets them in.
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
        }
        Column {
            CompositionLocalProvider(
                LocalSettingsQuery provides if (wholeGroup) "" else query,
                LocalSettingsMatches provides matched,
            ) {
                content()
            }
        }
    }
}

/** What the settings field is filtering on, or blank when it is empty. */
private val LocalSettingsQuery = compositionLocalOf { "" }

/** Where a row tells its group that it survived the filter, so an empty group can hide its name. */
private val LocalSettingsMatches = compositionLocalOf<MutableList<String>?> { null }

/**
 * Whether a row with this text belongs on screen, and registering it with its group when it does.
 * Rows call this first and return early when it is false.
 */
@Composable
private fun settingsRowVisible(title: String, subtitle: String = ""): Boolean {
    val query = LocalSettingsQuery.current
    if (query.isBlank()) return true
    val hit = title.contains(query, ignoreCase = true) || subtitle.contains(query, ignoreCase = true)
    val matches = LocalSettingsMatches.current
    DisposableEffect(hit, title, matches) {
        if (hit) matches?.add(title)
        onDispose { if (hit) matches?.remove(title) }
    }
    return hit
}

/** A settings row that opens something rather than toggling it. */
@Composable
private fun SettingsNavigationRow(title: String, subtitle: String, onClick: () -> Unit) {
    if (!settingsRowVisible(title, subtitle)) return
    Row(
        Modifier
            .fillMaxWidth()
            .desktopRowClickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = DesktopSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(10.dp))
        Icon(
            MaviMusicIcons.ChevronRight,
            null,
            tint = DesktopSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** A settings row whose control is a slider, with its value read out beside the title. */

@Composable
private fun SettingsToggle(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    if (!settingsRowVisible(title, subtitle)) return
    Row(
        Modifier
            .fillMaxWidth()
            .desktopRowClickable { onCheckedChange(!checked) }
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
        Spacer(Modifier.width(18.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = desktopSwitchColors())
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    if (!settingsRowVisible(title, subtitle)) return
    Row(
        Modifier
            .fillMaxWidth()
            .desktopRowClickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = DesktopAccent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, color = DesktopSecondary, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
        }
        if (onClick != null) {
            Spacer(Modifier.width(10.dp))
            Icon(MaviMusicIcons.ChevronRight, null, tint = DesktopSecondary, modifier = Modifier.size(18.dp))
        }
    }
}
