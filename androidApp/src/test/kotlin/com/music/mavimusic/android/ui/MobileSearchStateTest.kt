package com.music.mavimusic.android.ui

import com.music.mavimusic.data.model.SearchFilter
import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MobileSearchStateTest {
    @Test fun `playlist search retains collection results instead of filtering them as songs`() = runTest {
        val playlist = com.music.mavimusic.data.model.BrowseItem("VLtest", "Rock", "Playlist", null,
            com.music.mavimusic.data.model.BrowseType.PLAYLIST)
        val state = MobileSearchState(this, emptyList(), {}, searchCollections = { _, filter ->
            assertEquals(SearchFilter.PLAYLISTS, filter)
            Result.success(listOf(playlist))
        })
        state.selectFilter(SearchFilter.PLAYLISTS)
        state.submit("rock")
        advanceUntilIdle()
        assertEquals(listOf(playlist), state.collections)
        assertTrue(state.results.isEmpty())
    }
    private fun song(id: String) = Song(id, id, "Test artist", null)

    @Test fun `late result from an old search cannot overwrite a newer query`() = runTest {
        val state = MobileSearchState(this, emptyList(), {}, search = { term, _ ->
            withContext(NonCancellable) { delay(if (term == "old") 500 else 10) }
            Result.success(listOf(song(term)))
        })
        state.submit("old")
        runCurrent()
        state.submit("new")
        advanceUntilIdle()
        assertEquals("new", state.query)
        assertEquals(listOf(song("new")), state.results)
        assertFalse(state.isSearching)
    }

    @Test fun `changing filter invalidates the in flight search`() = runTest {
        val state = MobileSearchState(this, emptyList(), {}, search = { _, filter ->
            withContext(NonCancellable) { delay(if (filter == SearchFilter.SONGS) 500 else 10) }
            Result.success(listOf(song(filter.name)))
        })
        state.submit("music")
        runCurrent()
        state.selectFilter(SearchFilter.VIDEOS)
        advanceUntilIdle()
        assertEquals(listOf(song("VIDEOS")), state.results)
    }

    @Test fun `clearing query discards late results and loading state`() = runTest {
        val state = MobileSearchState(this, emptyList(), {}, search = { _, _ ->
            withContext(NonCancellable) { delay(500) }
            Result.success(listOf(song("old")))
        })
        state.submit("music")
        runCurrent()
        state.updateQuery("")
        advanceUntilIdle()
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isSearching)
    }

    @Test fun `network failure has a recoverable error instead of empty search message`() = runTest {
        var fail = true
        val state = MobileSearchState(this, emptyList(), {}, search = { _, _ ->
            if (fail) Result.failure(IllegalStateException("offline"))
            else Result.success(listOf(song("found")))
        })
        state.submit("music")
        advanceUntilIdle()
        assertNotNull(state.error)
        assertFalse(state.isSearching)
        fail = false
        state.submit()
        advanceUntilIdle()
        assertNull(state.error)
        assertEquals(listOf(song("found")), state.results)
    }

    @Test fun `typing debounces suggestions and only displays the latest term`() = runTest {
        val calls = mutableListOf<String>()
        val state = MobileSearchState(this, emptyList(), {}, suggest = {
            calls += it
            Result.success(listOf(it))
        })
        state.updateQuery("co")
        state.updateQuery("coldplay")
        advanceUntilIdle()
        assertEquals(listOf("coldplay"), calls)
        assertEquals(listOf("coldplay"), state.suggestions)
    }
}
