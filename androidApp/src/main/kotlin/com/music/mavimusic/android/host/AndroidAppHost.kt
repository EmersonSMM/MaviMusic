package com.music.mavimusic.android.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.music.mavimusic.android.data.OfflineDownloadManager
import com.music.mavimusic.data.settings.LibrarySort
import com.music.mavimusic.data.settings.LibraryViewType
import com.music.mavimusic.ui.AppUiHost
import kotlinx.coroutines.flow.MutableStateFlow

class AndroidAppHost : AppUiHost {
    override val swipeToPlayNext = MutableStateFlow(true)

    override val homeRecentsViewType = MutableStateFlow(LibraryViewType.GRID)

    override fun setHomeRecentsViewType(value: LibraryViewType) {
        homeRecentsViewType.value = value
    }

    override val pinnedPlaylists = MutableStateFlow<List<String>>(emptyList())

    override val librarySort = MutableStateFlow(LibrarySort.DEFAULT)

    @Composable
    override fun downloadedIds(): Set<String> {
        val downloaded by OfflineDownloadManager.downloadedSongs.collectAsState()
        return remember(downloaded) { downloaded.mapTo(HashSet()) { it.videoId } }
    }
}
