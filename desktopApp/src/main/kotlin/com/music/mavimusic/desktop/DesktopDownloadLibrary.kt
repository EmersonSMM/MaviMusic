package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** All download writes share one owner, including background completion and metadata repair. */
internal object DesktopDownloadLibrary {
    private val catalog = DesktopDownloadCatalog(DesktopPersistence()::downloads) { songs ->
        val persistence = DesktopPersistence()
        persistence.saveDownloads(songs)
        // General preference writes tolerate flush errors; a completed download must report them.
        persistence.preferences.flush()
    }
    val songs get() = catalog.songs
    fun add(song: Song) = catalog.add(song)
    fun remove(song: Song) = catalog.remove(song)
    fun repair(original: Song, repaired: Song) = catalog.repair(original, repaired)
    fun verified(checked: List<Song>, valid: List<Song>) = catalog.verified(checked, valid)
}

internal class DesktopDownloadCatalog(read: () -> List<Song>, private val write: (List<Song>) -> Unit) {
    private val state = MutableStateFlow(read())
    val songs: StateFlow<List<Song>> = state
    private fun publish(next: List<Song>) {
        write(next)
        state.value = next
    }
    @Synchronized fun add(song: Song) = publish(listOf(song) + state.value.filterNot { it.videoId == song.videoId })
    @Synchronized fun remove(song: Song) = publish(state.value.filterNot { it == song })
    @Synchronized fun repair(original: Song, repaired: Song) = publish(state.value.map { if (it == original) repaired else it })
    @Synchronized fun verified(checked: List<Song>, valid: List<Song>) {
        // A file check may finish after a new download or repair. Only remove the exact obsolete records.
        val gone = checked.toSet() - valid.toSet()
        publish(state.value.filterNot { it in gone })
    }
}
