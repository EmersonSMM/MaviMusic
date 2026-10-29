package com.music.mavimusic.android.audio

import kotlin.random.Random

/** Keeps shuffle traversal and back navigation separate from the visible queue. */
internal class AndroidQueueOrder(private val random: Random = Random.Default) {
    private var order = emptyList<Int>()
    private var cursor = 0
    private var shuffled = false

    fun reset(size: Int, currentIndex: Int, shuffle: Boolean) {
        shuffled = shuffle
        if (size == 0) {
            order = emptyList()
            cursor = 0
            return
        }
        val current = currentIndex.coerceIn(0, size - 1)
        order = if (shuffle) {
            listOf(current) + (0 until size).filter { it != current }.shuffled(random)
        } else {
            (0 until size).toList()
        }
        cursor = if (shuffle) 0 else current
    }

    fun next(repeatAll: Boolean): Int? {
        if (order.isEmpty()) return null
        if (cursor + 1 < order.size) return order[++cursor]
        if (!repeatAll) return null
        // A new shuffle cycle must not immediately repeat the track just played.
        if (shuffled && order.size > 1) {
            val current = order[cursor]
            order = order.filter { it != current }.shuffled(random) + current
        }
        cursor = 0
        return order[cursor]
    }

    fun previous(): Int? = if (cursor > 0) order[--cursor] else null

    fun append(newSize: Int) {
        val added = (order.size until newSize).toList()
        order = order + if (shuffled) added.shuffled(random) else added
    }
}
