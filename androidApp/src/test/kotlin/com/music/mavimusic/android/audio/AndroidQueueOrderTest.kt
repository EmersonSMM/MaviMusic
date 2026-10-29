package com.music.mavimusic.android.audio

import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class AndroidQueueOrderTest {
    @Test fun `shuffle visits every other track once and previous retraces that order`() {
        val order = AndroidQueueOrder(Random(42))
        order.reset(8, 3, shuffle = true)
        val visited = (1..7).map { order.next(repeatAll = false)!! }
        assertEquals((0..7).filter { it != 3 }.toSet(), visited.toSet())
        assertNotEquals(listOf(4, 5, 6, 7, 0, 1, 2), visited)
        assertNull(order.next(repeatAll = false))
        assertEquals(visited[5], order.previous())
        assertEquals(visited[6], order.next(repeatAll = false))
    }

    @Test fun `repeat shuffle starts a complete new cycle without repeating last track`() {
        val order = AndroidQueueOrder(Random(8))
        order.reset(5, 0, shuffle = true)
        val firstCycle = listOf(0) + (1..4).map { order.next(false)!! }
        val secondCycle = (1..5).map { order.next(true)!! }
        assertNotEquals(firstCycle.last(), secondCycle.first())
        assertEquals((0..4).toSet(), secondCycle.toSet())
    }

    @Test fun `radio append continues after exhausted shuffle without replaying old tracks`() {
        val order = AndroidQueueOrder(Random(9))
        order.reset(3, 1, shuffle = true)
        repeat(2) { order.next(false) }
        assertNull(order.next(false))
        order.append(6)
        assertEquals(setOf(3, 4, 5), (1..3).map { order.next(false)!! }.toSet())
        assertNull(order.next(false))
    }

    @Test fun `disabling shuffle resumes sequentially from current track`() {
        val order = AndroidQueueOrder(Random(2))
        order.reset(6, 0, shuffle = true)
        val current = order.next(false)!!
        order.reset(6, current, shuffle = false)
        assertEquals(if (current < 5) current + 1 else null, order.next(false))
    }

    @Test fun `empty and singleton queues handle repeat and previous`() {
        val order = AndroidQueueOrder()
        order.reset(0, 0, true)
        assertNull(order.next(true))
        assertNull(order.previous())
        order.reset(1, 0, true)
        assertNull(order.next(false))
        assertEquals(0, order.next(true))
        assertNull(order.previous())
    }
}
