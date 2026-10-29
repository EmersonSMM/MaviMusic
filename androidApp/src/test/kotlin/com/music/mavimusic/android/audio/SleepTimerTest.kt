package com.music.mavimusic.android.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepTimerTest {
    @Test fun `minute timer allows track changes and pauses only at deadline`() = runTest {
        var pauses = 0
        val timer = SleepTimerController(this) { pauses++ }
        timer.start(2)
        assertFalse(timer.onTrackEnded())
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, timer.minutesLeft.value)
        assertFalse(timer.onTrackEnded())
        assertEquals(0, pauses)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(1, pauses)
        assertFalse(timer.isRunning())
    }

    @Test fun `end of song consumes the event before repeat or autoplay can resume`() = runTest {
        var pauses = 0
        val timer = SleepTimerController(this) { pauses++ }
        timer.startEndOfSong()
        assertTrue(timer.onTrackEnded())
        assertEquals(1, pauses)
        assertFalse(timer.isRunning())
        assertFalse(timer.onTrackEnded())
    }

    @Test fun `cancelling prevents a pending deadline from pausing playback`() = runTest {
        var pauses = 0
        val timer = SleepTimerController(this) { pauses++ }
        timer.start(1)
        advanceTimeBy(30_000)
        timer.cancel()
        advanceTimeBy(60_000)
        assertEquals(0, pauses)
    }

    @Test fun `switching from minute timer to end of song cancels old deadline`() = runTest {
        var pauses = 0
        val timer = SleepTimerController(this) { pauses++ }
        timer.start(1)
        timer.startEndOfSong()
        advanceTimeBy(120_000)
        assertEquals(0, pauses)
        assertTrue(timer.onTrackEnded())
        assertEquals(1, pauses)
    }
}
