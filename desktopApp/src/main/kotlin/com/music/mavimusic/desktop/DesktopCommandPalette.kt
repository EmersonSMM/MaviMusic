package com.music.mavimusic.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.music.mavimusic.data.model.BrowseItem
import com.music.mavimusic.data.model.BrowseType
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.Song
import java.awt.MouseInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One item actionable in the Command Palette. */
internal sealed interface PaletteItem {
    val title: String
    val subtitle: String?
    val icon: ImageVector?
    val imageUrl: String?
    val action: () -> Unit

    data class Command(
        override val title: String,
        override val subtitle: String? = null,
        override val icon: ImageVector,
        override val action: () -> Unit,
    ) : PaletteItem {
        override val imageUrl: String? = null
    }

    data class TrackItem(
        val song: Song,
        override val action: () -> Unit,
    ) : PaletteItem {
        override val title: String = song.title
        override val subtitle: String = song.artist
        override val icon: ImageVector = Icons.Rounded.MusicNote
        override val imageUrl: String? = song.thumbnailUrl
    }

    data class CollectionItem(
        val item: BrowseItem,
        override val action: () -> Unit,
    ) : PaletteItem {
        override val title: String = item.title
        override val subtitle: String = item.subtitle
        override val icon: ImageVector = when (item.type) {
            BrowseType.ALBUM -> Icons.Rounded.LibraryMusic
            BrowseType.ARTIST -> Icons.Rounded.Mic
            else -> Icons.Rounded.FormatListBulleted
        }
        override val imageUrl: String? = item.thumbnailUrl
    }
}

/**
 * Modern floating Command Palette (Spotlight / Raycast style) triggered via Cmd+K.
 * Allows instant search of songs, albums, artists, navigation and quick playback actions.
 */
@Composable
internal fun DesktopCommandPalette(
    isStandaloneWindow: Boolean = false,
    onDismiss: () -> Unit,
    onNavigate: (DesktopDestination) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenDownloads: () -> Unit,
    onPlayLucky: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenImmersiveLyrics: () -> Unit = {},
    onToggleNormalization: () -> Unit = {},
    onPlaySong: (Song) -> Unit,
    onOpenCollection: (String, BrowseType) -> Unit,
    onOpenArtist: (String, String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repeat(8) { attempt ->
            delay(35L * (attempt + 1))
            runCatching { focusRequester.requestFocus() }
        }
    }

    // Static / quick commands
    val quickCommands = remember {
        listOf(
            PaletteItem.Command(
                title = DesktopStrings["command_lucky", "Azar (Música recomendada aleatoria)"],
                subtitle = "Reproduce un mix sorpresa basado en tus gustos",
                icon = Icons.Rounded.Casino,
                action = { onPlayLucky(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_sleep", "Temporizador de apagado (Sleep Timer)"],
                subtitle = "Pausar música tras 15, 30, 45, 60 min o al terminar pista",
                icon = Icons.Rounded.Bedtime,
                action = { onOpenSleepTimer(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_downloads", "Administrador de descargas"],
                subtitle = "Ver descargas activas a MP3 y progreso en tiempo real",
                icon = Icons.Rounded.Download,
                action = { onOpenDownloads(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_immersive_lyrics", "Modo inmersivo Winamp / Letras (Ondas y Espectro)"],
                subtitle = "Visualizador de ondas y espectro estilo Winamp + letras sincronizadas",
                icon = Icons.Rounded.OpenInFull,
                action = { onOpenImmersiveLyrics(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_volume_normalization", "Normalización de volumen (LUFS -14 dB)"],
                subtitle = "Nivelación automática de volumen entre pistas para evitar saltos",
                icon = Icons.Rounded.VolumeUp,
                action = { onToggleNormalization(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_play_pause", "Reproducir / Pausar"],
                subtitle = "Control de reproducción actual",
                icon = Icons.Rounded.PlayArrow,
                action = { onTogglePlayPause(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_next", "Siguiente pista"],
                subtitle = "Saltar a la siguiente canción en la cola",
                icon = Icons.Rounded.SkipNext,
                action = { onPlayNext(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_previous", "Pista anterior"],
                subtitle = "Volver a la canción anterior",
                icon = Icons.Rounded.SkipPrevious,
                action = { onPlayPrevious(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_shuffle", "Alternar modo aleatorio"],
                subtitle = "Activar o desactivar reproducción aleatoria",
                icon = Icons.Rounded.Shuffle,
                action = { onToggleShuffle(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_lyrics", "Ver letras sincronizadas"],
                subtitle = "Abrir panel lateral de letras",
                icon = Icons.Rounded.Mic,
                action = { onOpenLyrics(); onDismiss() },
            ),
            PaletteItem.Command(
                title = DesktopStrings["command_queue", "Ver cola de reproducción"],
                subtitle = "Abrir panel lateral de la cola",
                icon = Icons.Rounded.QueueMusic,
                action = { onOpenQueue(); onDismiss() },
            ),
            PaletteItem.Command(
                title = "Ir a Inicio",
                subtitle = "Página principal con mezclas y selección rápida",
                icon = Icons.Rounded.Home,
                action = { onNavigate(DesktopDestination.LISTEN_NOW); onDismiss() },
            ),
            PaletteItem.Command(
                title = "Ir a Explorar",
                subtitle = "Novedades, listas de éxitos, estados de ánimo y géneros",
                icon = Icons.Rounded.Explore,
                action = { onNavigate(DesktopDestination.EXPLORE); onDismiss() },
            ),
            PaletteItem.Command(
                title = "Ir a Biblioteca",
                subtitle = "Tus listas de reproducción, canciones guardadas y artistas",
                icon = Icons.Rounded.LibraryMusic,
                action = { onNavigate(DesktopDestination.LIBRARY); onDismiss() },
            ),
            PaletteItem.Command(
                title = "Ir a Historial",
                subtitle = "Canciones reproducidas recientemente",
                icon = Icons.Rounded.History,
                action = { onNavigate(DesktopDestination.HISTORY); onDismiss() },
            ),
            PaletteItem.Command(
                title = "Ir a Ajustes",
                subtitle = "Configuración de audio, interfaz, descargas y cuentas",
                icon = Icons.Rounded.Settings,
                action = { onOpenSettings(); onDismiss() },
            ),
        )
    }

    // Live search debounced
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(220)
        runCatching {
            DesktopSearchClient.search(trimmed, SearchFilter.ALL).getOrNull()
        }.onSuccess { results ->
            searchResults = results.orEmpty().take(15)
        }
        isSearching = false
    }

    // Filtered items based on query
    val displayItems = remember(query, searchResults) {
        val trimmed = query.trim().lowercase()
        if (trimmed.isBlank()) {
            quickCommands
        } else {
            val matchingCommands = quickCommands.filter {
                it.title.lowercase().contains(trimmed) ||
                    (it.subtitle?.lowercase()?.contains(trimmed) == true)
            }
            val remoteTracks = searchResults.mapNotNull { item ->
                when (item) {
                    is SearchResult.TopTrack -> PaletteItem.TrackItem(item.song) {
                        onPlaySong(item.song)
                        onDismiss()
                    }
                    is SearchResult.Track -> PaletteItem.TrackItem(item.song) {
                        onPlaySong(item.song)
                        onDismiss()
                    }
                    is SearchResult.Browse -> PaletteItem.CollectionItem(item.item) {
                        if (item.item.type == BrowseType.ARTIST) {
                            onOpenArtist(item.item.browseId, item.item.title)
                        } else {
                            onOpenCollection(item.item.browseId, item.item.type)
                        }
                        onDismiss()
                    }
                }
            }
            matchingCommands + remoteTracks
        }
    }

    var selectedIndex by remember { mutableStateOf(0) }
    LaunchedEffect(displayItems.size) {
        selectedIndex = 0
    }

    val cardContent: @Composable () -> Unit = {
        Surface(
            modifier = (if (isStandaloneWindow) Modifier.fillMaxSize() else Modifier.widthIn(min = 400.dp, max = 660.dp).fillMaxWidth())
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { runCatching { focusRequester.requestFocus() } },
                )
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.Escape -> {
                                onDismiss()
                                true
                            }
                            Key.DirectionDown -> {
                                if (displayItems.isNotEmpty()) {
                                    selectedIndex = (selectedIndex + 1).coerceAtMost(displayItems.size - 1)
                                    scope.launch { listState.animateScrollToItem(selectedIndex) }
                                }
                                true
                            }
                            Key.DirectionUp -> {
                                if (displayItems.isNotEmpty()) {
                                    selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                    scope.launch { listState.animateScrollToItem(selectedIndex) }
                                }
                                true
                            }
                            Key.Enter -> {
                                displayItems.getOrNull(selectedIndex)?.action?.invoke()
                                true
                            }
                            else -> false
                        }
                    } else {
                        false
                    }
                },
            color = Color(0xFF1B1B22),
            tonalElevation = 16.dp,
            shadowElevation = 24.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                if (isStandaloneWindow) {
                    var dragStartMouseX by remember { mutableStateOf(0) }
                    var dragStartMouseY by remember { mutableStateOf(0) }
                    var dragStartWinX by remember { mutableStateOf(0) }
                    var dragStartWinY by remember { mutableStateOf(0) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF14141A))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        DesktopSpotlightState.isDragging = true
                                        val p = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
                                        if (p != null) {
                                            dragStartMouseX = p.x
                                            dragStartMouseY = p.y
                                            dragStartWinX = DesktopSpotlightState.screenX
                                            dragStartWinY = DesktopSpotlightState.screenY
                                        }
                                    },
                                    onDragEnd = { DesktopSpotlightState.isDragging = false },
                                    onDragCancel = { DesktopSpotlightState.isDragging = false },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val p = runCatching { MouseInfo.getPointerInfo()?.location }.getOrNull()
                                        if (p != null) {
                                            DesktopSpotlightState.screenX = dragStartWinX + (p.x - dragStartMouseX)
                                            DesktopSpotlightState.screenY = dragStartWinY + (p.y - dragStartMouseY)
                                        }
                                    },
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "✦ MaviMusic Spotlight",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.22f)),
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { DesktopSpotlightState.moveToOtherScreen() }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "⇄ Cambiar pantalla",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0x18FFFFFF), thickness = 1.dp)
                }

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { runCatching { focusRequester.requestFocus() } },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = DesktopAccent,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionDown -> {
                                            if (displayItems.isNotEmpty()) {
                                                selectedIndex = (selectedIndex + 1).coerceAtMost(displayItems.size - 1)
                                                scope.launch { listState.animateScrollToItem(selectedIndex) }
                                            }
                                            true
                                        }
                                        Key.DirectionUp -> {
                                            if (displayItems.isNotEmpty()) {
                                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                                                scope.launch { listState.animateScrollToItem(selectedIndex) }
                                            }
                                            true
                                        }
                                        Key.Enter -> {
                                            displayItems.getOrNull(selectedIndex)?.action?.invoke()
                                            true
                                        }
                                        Key.Escape -> {
                                            onDismiss()
                                            true
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontSize = 16.sp,
                        ),
                        cursorBrush = SolidColor(DesktopAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                displayItems.getOrNull(selectedIndex)?.action?.invoke()
                            },
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (query.isBlank()) {
                                    Text(
                                        text = DesktopStrings["search_placeholder_palette", "Buscar canciones, artistas, páginas o comandos... (⌘K)"],
                                        color = Color.White.copy(alpha = 0.45f),
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                        maxLines = 1,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )

                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = DesktopAccent,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { query = "" },
                            modifier = Modifier.size(26.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Limpiar",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                    }

                    // ESC hint badge
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2B2B36), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = "ESC",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp,
                        )
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 1.dp)

                // Results / Items List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = if (isStandaloneWindow) 400.dp else 440.dp)
                        .padding(vertical = 6.dp),
                ) {
                    if (displayItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "No se encontraron resultados para '$query'",
                                    color = Color.White.copy(alpha = 0.5f),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    } else {
                        itemsIndexed(displayItems) { index, item ->
                            val isSelected = index == selectedIndex
                            PaletteRow(
                                item = item,
                                isSelected = isSelected,
                                onClick = item.action,
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0x1FFFFFFF), thickness = 1.dp)

                // Footer with keyboard tips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF16161C))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Navegar con ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                        )
                        KeyBadge("↑")
                        Spacer(Modifier.width(3.dp))
                        KeyBadge("↓")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Seleccionar ",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                        )
                        KeyBadge("↵")
                    }
                    Text(
                        text = "MaviMusic Spotlight",
                        style = MaterialTheme.typography.labelSmall,
                        color = DesktopAccent.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }

    if (isStandaloneWindow) {
        cardContent()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .padding(top = 70.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            cardContent()
        }
    }
}

@Composable
private fun KeyBadge(label: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFF282832), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun PaletteRow(
    item: PaletteItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) Color(0xFF2E2E3A) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!item.imageUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(
                        if (item is PaletteItem.CollectionItem && item.item.type == BrowseType.ARTIST) {
                            CircleShape
                        } else {
                            RoundedCornerShape(6.dp)
                        }
                    ),
            ) {
                DesktopArtwork(
                    url = item.imageUrl,
                    modifier = Modifier.fillMaxSize(),
                    px = 120,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) DesktopAccent.copy(alpha = 0.2f) else Color(0xFF252530)),
                contentAlignment = Alignment.Center,
            ) {
                item.icon?.let { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) DesktopAccent else Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            item.subtitle?.let { sub ->
                if (sub.isNotBlank()) {
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .background(DesktopAccent.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "↵ Abrir",
                    style = MaterialTheme.typography.labelSmall,
                    color = DesktopAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
