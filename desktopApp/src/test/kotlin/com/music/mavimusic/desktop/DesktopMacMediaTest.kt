package com.music.mavimusic.desktop

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopMacMediaTest {

    private val pressed = mutableListOf<String>()

    private fun install() {
        DesktopMacMedia.start(
            DesktopMacMedia.Controller(
                onPlay = { pressed += "play" },
                onPause = { pressed += "pause" },
                onNext = { pressed += "next" },
                onPrevious = { pressed += "previous" },
                onStop = { pressed += "stop" },
                onSeek = { pressed += "seek $it" },
            ),
        )
    }

    @AfterTest
    fun tearDown() = DesktopMacMedia.stop()

    @Test
    fun everyButtonReachesItsOwnControl() {
        install()
        (0..4).forEach(DesktopMacMedia::onButton)
        assertEquals(listOf("play", "pause", "next", "previous", "stop"), pressed)
    }

    @Test
    fun anUnknownButtonIsIgnoredRatherThanGuessed() {
        install()
        DesktopMacMedia.onButton(99)
        DesktopMacMedia.onButton(-1)
        assertEquals(emptyList(), pressed)
    }

    @Test
    fun aScrubberDragReachesSeekWithItsPosition() {
        install()
        DesktopMacMedia.onSeek(42_000L)
        assertEquals(listOf("seek 42000"), pressed)
    }
}
