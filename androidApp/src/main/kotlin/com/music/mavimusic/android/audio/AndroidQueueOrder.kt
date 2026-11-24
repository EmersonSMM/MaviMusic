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

    fun hasNext(repeatAll: Boolean): Boolean =
        order.isNotEmpty() && (cursor + 1 < order.size || repeatAll)

    fun hasPrevious(): Boolean = cursor > 0

    fun append(newSize: Int) {
        val added = (order.size until newSize).toList()
        order = order + if (shuffled) added.shuffled(random) else added
    }

    fun insertAfterCurrent(insertedIndex: Int) {
        if (order.isEmpty()) {
            order = listOf(insertedIndex)
            cursor = 0
            return
        }
        val shifted = order.map { if (it >= insertedIndex) it + 1 else it }.toMutableList()
        val insertAt = (cursor + 1).coerceIn(0, shifted.size)
        shifted.add(insertAt, insertedIndex)
        order = shifted
    }

    fun removeAt(removedIndex: Int) {
        if (order.isEmpty()) return
        val removedPos = order.indexOf(removedIndex)
        val updated = order.filter { it != removedIndex }.map { if (it > removedIndex) it - 1 else it }
        order = updated
        if (updated.isEmpty()) {
            cursor = 0
            return
        }
        if (removedPos in 0 until cursor) cursor--
        cursor = cursor.coerceIn(0, updated.lastIndex)
    }

    fun syncAfterMove(from: Int, to: Int) {
        if (order.isEmpty() || from == to) return
        order = order.map { idx ->
            when {
                idx == from -> to
                from < to && idx in (from + 1)..to -> idx - 1
                to < from && idx in to until from -> idx + 1
                else -> idx
            }
        }
    }
}
