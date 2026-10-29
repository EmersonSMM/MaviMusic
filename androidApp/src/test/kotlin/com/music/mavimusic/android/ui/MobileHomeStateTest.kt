package com.music.mavimusic.android.ui

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MobileHomeStateTest {
    @Test fun `no history means no generic recommendations or network request`() = runTest {
        var calls = 0
        val state = MobileHomeState(this, { emptyList() }, { calls++; Result.success(emptyList()) })
        state.load()
        advanceUntilIdle()
        assertEquals(0, calls)
        assertTrue(state.songs.isEmpty())
    }

    @Test fun `only two recent searches are used and changing history refreshes home`() = runTest {
        var history = listOf("Queen", "Coldplay", "other")
        val calls = mutableListOf<String>()
        val state = MobileHomeState(this, { history }, { term ->
            calls += term
            Result.success(listOf(Song(term, term, term, null)))
        })
        state.load()
        advanceUntilIdle()
        assertEquals(listOf("Queen", "Coldplay"), calls)
        state.load()
        advanceUntilIdle()
        assertEquals(2, calls.size)
        history = listOf("Metallica")
        state.load()
        advanceUntilIdle()
        assertEquals(listOf("Metallica"), state.songs.map { it.videoId })
    }
}
