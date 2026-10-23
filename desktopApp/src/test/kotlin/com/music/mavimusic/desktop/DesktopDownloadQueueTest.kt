package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The download queue's bookkeeping.
 *
 * The transfer itself is not exercised — that reaches the network and writes to the listener's own
 * Music folder — so these pin the part that decides what is queued, in what order, and what a
 * cancel does to it.
 */
class DesktopDownloadQueueTest {
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
    private val queue = DesktopDownloadScheduler(scope, transfer = { _, _ -> kotlinx.coroutines.awaitCancellation() })

    private fun song(id: String) = Song(videoId = id, title = "Track $id", artist = "Artist", thumbnailUrl = "")

    @AfterTest
    fun tearDown() {
        queue.cancelAll()
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    @Test
    fun `a track is queued once however many times it is asked for`() {
        queue.enqueue(song("a"))
        queue.enqueue(song("a"))
        assertEquals(1, queue.active.value.count { it.key == "a" })
    }

    @Test
    fun `the queue keeps the order it was asked in`() {
        listOf("a", "b", "c").forEach { queue.enqueue(song(it)) }
        val order = queue.active.value.values
            .sortedBy(DesktopDownloadItem::sequence)
            .map { it.song.videoId }
        assertEquals(listOf("a", "b", "c"), order.take(3))
    }

    @Test
    fun `a cancelled track leaves the manager rather than sitting in it`() {
        queue.enqueue(song("gone"))
        assertTrue("gone" in queue.active.value)
        queue.cancel("gone")
        assertFalse("gone" in queue.active.value)
    }

    @Test
    fun `cancel all empties the queue`() {
        listOf("a", "b", "c").forEach { queue.enqueue(song(it)) }
        queue.cancelAll()
        assertTrue(queue.active.value.isEmpty())
    }

    @Test
    fun `a blank id is not something that can be downloaded`() {
        queue.enqueue(song(""))
        assertFalse("" in queue.active.value)
    }

    @Test
    fun `the summary line says what the queue is doing`() {
        assertEquals("Nothing downloading", summary(total = 0, failed = 0, busy = false))
        assertEquals("1 track", summary(total = 1, failed = 0, busy = true))
        assertEquals("4 tracks", summary(total = 4, failed = 0, busy = true))
        assertEquals("1 download failed", summary(total = 1, failed = 1, busy = false))
        assertEquals("3 downloads failed", summary(total = 3, failed = 3, busy = false))
        // Still going, so the count is what matters rather than the failures behind it.
        assertEquals("5 tracks", summary(total = 5, failed = 1, busy = true))
    }

    @Test
    fun `four transfers run at once, matching Android`() {
        assertEquals(4, DesktopDownloadQueue.WORKERS)
    }
}
