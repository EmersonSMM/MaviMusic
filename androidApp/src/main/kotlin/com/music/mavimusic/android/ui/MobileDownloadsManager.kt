package com.music.mavimusic.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.mavimusic.android.SongRowItem
import com.music.mavimusic.android.audio.AndroidPlayerEngine
import com.music.mavimusic.android.data.OfflineDownloadManager
import com.music.mavimusic.android.data.UserPlaylist
import com.music.mavimusic.data.model.Song

enum class OfflineSegment(val label: String) {
    SONGS("Canciones"),
    ARTISTS("Artistas"),
    ALBUMS("Álbumes"),
    PLAYLISTS("Playlists"),
}

data class OfflineArtistGroup(
    val name: String,
    val artworkUrl: String?,
    val albumCount: Int,
    val songs: List<Song>,
)

data class OfflineAlbumGroup(
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val songs: List<Song>,
)

data class OfflinePlaylistGroup(
    val id: String,
    val name: String,
    val artworkUrl: String?,
    val offlineSongs: List<Song>,
    val totalSongs: Int,
)

internal fun groupOfflineArtists(songs: List<Song>): List<OfflineArtistGroup> =
    songs
        .groupBy { it.artist.trim().ifBlank { "Artista desconocido" } }
        .entries
        .sortedBy { it.key.lowercase() }
        .map { (artistName, tracks) ->
            val albumCount = tracks
                .map { it.albumName?.trim().orEmpty().ifBlank { "Sencillos" } }
                .distinctBy { it.lowercase() }
                .size
            OfflineArtistGroup(
                name = artistName,
                artworkUrl = tracks.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
                albumCount = albumCount,
                songs = tracks.sortedBy { it.title.lowercase() },
            )
        }

internal fun groupOfflineAlbums(songs: List<Song>): List<OfflineAlbumGroup> =
    songs
        .groupBy { song ->
            val album = song.albumName?.trim().orEmpty().ifBlank { "Sencillos" }
            val artist = song.artist.trim().ifBlank { "Artista desconocido" }
            album.lowercase() to artist.lowercase()
        }
        .values
        .map { tracks ->
            val first = tracks.first()
            OfflineAlbumGroup(
                title = first.albumName?.trim().orEmpty().ifBlank { "Sencillos" },
                artist = first.artist.trim().ifBlank { "Artista desconocido" },
                artworkUrl = tracks.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
                songs = tracks.sortedWith(compareBy({ it.trackNumber ?: Int.MAX_VALUE }, { it.title.lowercase() })),
            )
        }
        .sortedBy { it.title.lowercase() }

internal fun groupOfflinePlaylists(
    playlists: List<UserPlaylist>,
    downloadedSongs: List<Song>,
): List<OfflinePlaylistGroup> {
    val byId = downloadedSongs.associateBy { it.videoId }
    return playlists.map { pl ->
        val offline = pl.songs.mapNotNull { byId[it.videoId] }
        OfflinePlaylistGroup(
            id = pl.id,
            name = pl.name,
            artworkUrl = offline.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl
                ?: pl.songs.firstOrNull { !it.thumbnailUrl.isNullOrBlank() }?.thumbnailUrl,
            offlineSongs = offline,
            totalSongs = pl.songs.size,
        )
    }.sortedByDescending { it.offlineSongs.size }
}

/**
 * Classic iPod / Apple Music-inspired offline Downloads Manager with clean segmentation by
 * Songs, Artists, Albums, and Playlists + instant drill-down and offline shuffle playback.
 */
@Composable
fun MobileDownloadsManager(
    downloadedSongs: List<Song>,
    playlists: List<UserPlaylist>,
    currentSongId: String?,
    onAddToPlaylist: (Song) -> Unit,
    onStartRadio: (Song) -> Unit,
) {
    val activeDownloads by OfflineDownloadManager.downloadingIds.collectAsState()
    val downloadProgress by OfflineDownloadManager.progress.collectAsState()
    val totalMb = remember(downloadedSongs, activeDownloads) {
        OfflineDownloadManager.getDownloadsSizeBytes() / (1024 * 1024)
    }

    var segment by remember { mutableStateOf(OfflineSegment.SONGS) }
    var filterQuery by remember { mutableStateOf("") }
    var showSearchField by remember { mutableStateOf(false) }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var selectedAlbumKey by remember { mutableStateOf<Pair<String, String>?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }

    val filteredSongs = remember(downloadedSongs, filterQuery) {
        val q = filterQuery.trim()
        if (q.isEmpty()) downloadedSongs
        else downloadedSongs.filter {
            it.title.contains(q, ignoreCase = true) ||
                it.artist.contains(q, ignoreCase = true) ||
                (it.albumName?.contains(q, ignoreCase = true) == true)
        }
    }

    val artistGroups = remember(filteredSongs) { groupOfflineArtists(filteredSongs) }
    val albumGroups = remember(filteredSongs) { groupOfflineAlbums(filteredSongs) }
    val playlistGroups = remember(playlists, downloadedSongs, filterQuery) {
        val all = groupOfflinePlaylists(playlists, downloadedSongs)
        val q = filterQuery.trim()
        if (q.isEmpty()) all else all.filter { it.name.contains(q, ignoreCase = true) }
    }

    fun playShuffled(tracks: List<Song>) {
        if (tracks.isEmpty()) return
        AndroidPlayerEngine.playQueue(tracks.shuffled(), 0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Active downloads progress banner (when WorkManager is downloading tracks)
        if (activeDownloads.isNotEmpty()) {
            val avg = activeDownloads.map { downloadProgress[it] ?: 0f }.filter { it >= 0f }.average().let {
                if (it.isNaN()) -1f else it.toFloat()
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xAA172A30),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Descargando ${activeDownloads.size} ${if (activeDownloads.size == 1) "pista" else "pistas"}…",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                        if (avg >= 0f) {
                            Text("${(avg * 100).toInt()} %", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (avg >= 0f) {
                        LinearProgressIndicator(progress = { avg.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        if (downloadedSongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Tu biblioteca sin conexión está vacía", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Descarga canciones para organizarlas aquí por artista, álbum y playlist como un reproductor clásico.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            return
        }

        // Drill-down: Artist detail view
        val activeArtist = selectedArtist?.let { name -> artistGroups.firstOrNull { it.name == name } }
        if (activeArtist != null) {
            val artistAlbums = remember(activeArtist.songs) { groupOfflineAlbums(activeArtist.songs) }
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { selectedArtist = null }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a artistas", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(activeArtist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${activeArtist.albumCount} ${if (activeArtist.albumCount == 1) "álbum" else "álbumes"} · ${activeArtist.songs.size} canciones", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                PlayShuffleHeader(
                    onPlayAll = { AndroidPlayerEngine.playQueue(activeArtist.songs, 0) },
                    onShuffle = { playShuffled(activeArtist.songs) },
                )
                LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (artistAlbums.size > 1) {
                        item {
                            Text("Álbumes descargados", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(artistAlbums, key = { "${it.title}_${it.artist}" }) { alb ->
                                    Column(
                                        Modifier
                                            .width(116.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedArtist = null
                                                segment = OfflineSegment.ALBUMS
                                                selectedAlbumKey = alb.title to alb.artist
                                            }
                                    ) {
                                        AsyncImage(
                                            alb.artworkUrl,
                                            null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.size(116.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF1F222C)),
                                        )
                                        Text(alb.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                        Text("${alb.songs.size} pistas", color = Color.Gray, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                    itemsIndexed(activeArtist.songs, key = { _, s -> s.videoId }) { index, song ->
                        SongRowItem(
                            song = song,
                            isCurrent = song.videoId == currentSongId,
                            onClick = { AndroidPlayerEngine.playQueue(activeArtist.songs, index) },
                            onDelete = { OfflineDownloadManager.deleteDownload(song.videoId) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onStartRadio = { onStartRadio(song) },
                        )
                    }
                }
            }
            return
        }

        // Drill-down: Album detail view
        val activeAlbum = selectedAlbumKey?.let { (title, artist) ->
            albumGroups.firstOrNull { it.title == title && it.artist == artist }
        }
        if (activeAlbum != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { selectedAlbumKey = null }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a álbumes", tint = Color.White)
                    }
                    Text("Álbum", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AsyncImage(
                        activeAlbum.artworkUrl,
                        null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1F222C)),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(activeAlbum.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(activeAlbum.artist, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(2.dp))
                        Text("${activeAlbum.songs.size} ${if (activeAlbum.songs.size == 1) "canción" else "canciones"} sin conexión", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
                PlayShuffleHeader(
                    onPlayAll = { AndroidPlayerEngine.playQueue(activeAlbum.songs, 0) },
                    onShuffle = { playShuffled(activeAlbum.songs) },
                )
                LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(activeAlbum.songs, key = { _, s -> s.videoId }) { index, song ->
                        SongRowItem(
                            song = song,
                            isCurrent = song.videoId == currentSongId,
                            onClick = { AndroidPlayerEngine.playQueue(activeAlbum.songs, index) },
                            onDelete = { OfflineDownloadManager.deleteDownload(song.videoId) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onStartRadio = { onStartRadio(song) },
                        )
                    }
                }
            }
            return
        }

        // Drill-down: Offline Playlist detail view
        val activePlaylist = selectedPlaylistId?.let { id -> playlistGroups.firstOrNull { it.id == id } }
        if (activePlaylist != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = { selectedPlaylistId = null }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver a playlists", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(activePlaylist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${activePlaylist.offlineSongs.size} de ${activePlaylist.totalSongs} descargadas", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (activePlaylist.offlineSongs.isNotEmpty()) {
                    PlayShuffleHeader(
                        onPlayAll = { AndroidPlayerEngine.playQueue(activePlaylist.offlineSongs, 0) },
                        onShuffle = { playShuffled(activePlaylist.offlineSongs) },
                    )
                    LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        itemsIndexed(activePlaylist.offlineSongs, key = { _, s -> s.videoId }) { index, song ->
                            SongRowItem(
                                song = song,
                                isCurrent = song.videoId == currentSongId,
                                onClick = { AndroidPlayerEngine.playQueue(activePlaylist.offlineSongs, index) },
                                onDelete = { OfflineDownloadManager.deleteDownload(song.videoId) },
                                onAddToPlaylist = { onAddToPlaylist(song) },
                                onStartRadio = { onStartRadio(song) },
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Esta playlist todavía no tiene canciones descargadas", color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                    }
                }
            }
            return
        }

        // Compact single-row iPod / Apple Music Segmented Toolbar + collapsible filter icon
        // so the song/artist/album browser gets maximum vertical breathing room even in landscape.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x88161820),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    OfflineSegment.entries.forEach { tab ->
                        val selected = segment == tab
                        val count = when (tab) {
                            OfflineSegment.SONGS -> filteredSongs.size
                            OfflineSegment.ARTISTS -> artistGroups.size
                            OfflineSegment.ALBUMS -> albumGroups.size
                            OfflineSegment.PLAYLISTS -> playlistGroups.size
                        }
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (selected) Color.White else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { segment = tab },
                        ) {
                            Text(
                                text = "${tab.label} ($count)",
                                color = if (selected) Color.Black else Color.White.copy(alpha = 0.78f),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (showSearchField || filterQuery.isNotEmpty()) Color.White else Color(0x88161820),
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable {
                        if (showSearchField) {
                            filterQuery = ""
                            showSearchField = false
                        } else {
                            showSearchField = true
                        }
                    },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (showSearchField) Icons.Rounded.Clear else Icons.Rounded.Search,
                        contentDescription = if (showSearchField) "Cerrar filtro" else "Filtrar descargas",
                        tint = if (showSearchField || filterQuery.isNotEmpty()) Color.Black else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (showSearchField) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = { Text("Filtrar por artista, álbum o canción…", color = Color.Gray, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Rounded.Clear, "Limpiar filtro", tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x881A1C24),
                    unfocusedContainerColor = Color(0x66161820),
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                ),
            )
        }

        when (segment) {
            OfflineSegment.SONGS -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${downloadedSongs.size} canciones · $totalMb MB sin conexión",
                                color = Color.White.copy(alpha = 0.62f),
                                fontSize = 11.sp,
                            )
                        }
                        PlayShuffleHeader(
                            onPlayAll = { AndroidPlayerEngine.playQueue(filteredSongs, 0) },
                            onShuffle = { playShuffled(filteredSongs) },
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    itemsIndexed(filteredSongs, key = { _, s -> s.videoId }) { index, song ->
                        SongRowItem(
                            song = song,
                            isCurrent = song.videoId == currentSongId,
                            onClick = { AndroidPlayerEngine.playQueue(filteredSongs, index) },
                            onDelete = { OfflineDownloadManager.deleteDownload(song.videoId) },
                            onAddToPlaylist = { onAddToPlaylist(song) },
                            onStartRadio = { onStartRadio(song) },
                        )
                    }
                }
            }

            OfflineSegment.ARTISTS -> {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(artistGroups, key = { it.name }) { group ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0x661A1D26),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                .clickable { selectedArtist = group.name },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF232634)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!group.artworkUrl.isNullOrBlank()) {
                                        AsyncImage(group.artworkUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Icon(Icons.Rounded.Person, null, tint = Color.White.copy(alpha = 0.6f))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${group.albumCount} ${if (group.albumCount == 1) "álbum" else "álbumes"} · ${group.songs.size} ${if (group.songs.size == 1) "canción" else "canciones"}",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.45f))
                            }
                        }
                    }
                }
            }

            OfflineSegment.ALBUMS -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 144.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(albumGroups, key = { "${it.title}_${it.artist}" }) { alb ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x55181B24))
                                .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(14.dp))
                                .clickable { selectedAlbumKey = alb.title to alb.artist }
                                .padding(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF232634)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (!alb.artworkUrl.isNullOrBlank()) {
                                    AsyncImage(alb.artworkUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Rounded.Album, null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(alb.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(alb.artist, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${alb.songs.size} ${if (alb.songs.size == 1) "pista" else "pistas"}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        }
                    }
                }
            }

            OfflineSegment.PLAYLISTS -> {
                if (playlistGroups.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, null, tint = Color.White.copy(alpha = 0.35f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Crea una playlist y descarga sus canciones para escucharla sin conexión.", color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(playlistGroups, key = { it.id }) { pl ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0x661A1D26),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                    .clickable { selectedPlaylistId = pl.id },
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF232634)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!pl.artworkUrl.isNullOrBlank()) {
                                            AsyncImage(pl.artworkUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(pl.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(
                                            "${pl.offlineSongs.size} de ${pl.totalSongs} disponibles sin conexión",
                                            color = if (pl.offlineSongs.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                                            fontSize = 12.sp,
                                        )
                                    }
                                    Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.45f))
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
private fun PlayShuffleHeader(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onPlayAll,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Reproducir", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        OutlinedButton(
            onClick = onShuffle,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Aleatorio", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}
