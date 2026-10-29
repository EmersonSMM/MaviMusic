package com.music.mavimusic.android.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.SearchResult
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owned by the app shell, so hiding Search does not discard its state or requests. */
class MobileSearchState(
    private val scope: CoroutineScope,
    initialHistory: List<String>,
    private val saveHistory: (List<String>) -> Unit,
    private val search: suspend (String, SearchFilter) -> Result<List<Song>> = { query, filter ->
        YtMusicRepository.search(query, filter).map { items ->
            items.mapNotNull {
                when (it) {
                    is SearchResult.Track -> it.song
                    is SearchResult.TopTrack -> it.song
                    else -> null
                }
            }
        }
    },
    private val suggest: suspend (String) -> Result<List<String>> = YtMusicRepository::searchSuggestions,
) {
    var query by mutableStateOf("")
        private set
    var filter by mutableStateOf(SearchFilter.SONGS)
        private set
    var results by mutableStateOf<List<Song>>(emptyList())
        private set
    var suggestions by mutableStateOf<List<String>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val history = mutableStateListOf<String>().apply { addAll(initialHistory) }
    val resultsScroll = LazyListState()
    private var request: Job? = null
    private var generation = 0

    fun updateQuery(value: String) {
        request?.cancel()
        val token = ++generation
        query = value
        results = emptyList()
        suggestions = emptyList()
        error = null
        isSearching = false
        if (value.trim().length < 2) return
        request = scope.launch {
            delay(250)
            try {
                val response = suggest(value.trim())
                if (token == generation) suggestions = response.getOrDefault(emptyList())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Suggestions are optional; submitting a search remains available.
            }
        }
    }

    fun selectFilter(value: SearchFilter) {
        filter = value
        if (query.isNotBlank()) submit()
    }

    fun submit(value: String = query) {
        val term = value.trim()
        if (term.isEmpty()) return
        request?.cancel()
        val token = ++generation
        query = term
        val requestedFilter = filter
        suggestions = emptyList()
        error = null
        isSearching = true
        history.remove(term)
        history.add(0, term)
        while (history.size > 12) history.removeLast()
        saveHistory(history.toList())
        request = scope.launch {
            try {
                val response = search(term, requestedFilter)
                if (token == generation) {
                    results = response.getOrDefault(emptyList())
                    if (response.isFailure) error = "No se pudo buscar. Comprueba la conexión e inténtalo de nuevo."
                    resultsScroll.requestScrollToItem(0)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (token == generation) {
                    results = emptyList()
                    error = "No se pudo buscar. Comprueba la conexión e inténtalo de nuevo."
                }
            } finally {
                if (token == generation) isSearching = false
            }
        }
    }

    fun removeHistory(term: String) {
        history.remove(term)
        saveHistory(history.toList())
    }
}
