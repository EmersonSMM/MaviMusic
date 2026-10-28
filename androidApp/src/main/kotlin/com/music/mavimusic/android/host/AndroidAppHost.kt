package com.music.mavimusic.android.host

import androidx.compose.runtime.Composable
import com.music.mavimusic.data.settings.LibrarySort
import com.music.mavimusic.data.settings.LibraryViewType
import com.music.mavimusic.ui.AppUiHost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidAppHost : AppUiHost {
    override val swipeToPlayNext = MutableStateFlow(true)

    override val homeRecentsViewType = MutableStateFlow(LibraryViewType.GRID)

    override fun setHomeRecentsViewType(value: LibraryViewType) {
        homeRecentsViewType.value = value
    }

    override val pinnedPlaylists = MutableStateFlow<List<String>>(emptyList())

    override val librarySort = MutableStateFlow(LibrarySort.DEFAULT)

    @Composable
    override fun downloadedIds(): Set<String> = emptySet()
}
