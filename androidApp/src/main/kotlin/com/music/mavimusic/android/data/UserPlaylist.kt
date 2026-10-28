package com.music.mavimusic.android.data

import com.music.mavimusic.data.model.Song

data class UserPlaylist(
    val id: String,
    val name: String,
    val songs: List<Song> = emptyList()
)
