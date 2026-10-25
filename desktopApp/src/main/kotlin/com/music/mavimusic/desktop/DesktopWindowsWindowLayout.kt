package com.music.mavimusic.desktop

import java.awt.Dimension
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.beans.PropertyChangeListener
import javax.swing.Timer

/** AWT window bounds and screen insets are already in logical coordinates on HiDPI Windows. */
internal object DesktopWindowsWindowLayout {
    fun minimum(work: Rectangle): Dimension = Dimension(
        minOf(1024, work.width.coerceAtLeast(1)),
        minOf(640, work.height.coerceAtLeast(1)),
    )

    fun fit(bounds: Rectangle, work: Rectangle): Rectangle {
        val minimum = minimum(work)
        val width = bounds.width.coerceIn(minimum.width, work.width.coerceAtLeast(1))
        val height = bounds.height.coerceIn(minimum.height, work.height.coerceAtLeast(1))
        return Rectangle(
            bounds.x.coerceIn(work.x, work.x + work.width.coerceAtLeast(1) - width),
            bounds.y.coerceIn(work.y, work.y + work.height.coerceAtLeast(1) - height),
            width, height,
        )
    }

    /** Re-evaluate the current monitor after moves/DPI changes, without fighting a live resize. */
    fun attach(window: Frame, opening: Dimension): () -> Unit {
        fun refresh(resize: Boolean) {
            if (DesktopWindowMode.fullScreen.value) return
            val configuration = window.graphicsConfiguration ?: return
            val insets = Toolkit.getDefaultToolkit().getScreenInsets(configuration)
            val screen = configuration.bounds
            val work = Rectangle(screen.x + insets.left, screen.y + insets.top,
                (screen.width - insets.left - insets.right).coerceAtLeast(1),
                (screen.height - insets.top - insets.bottom).coerceAtLeast(1))
            val minimum = minimum(work)
            if (window.minimumSize != minimum) window.minimumSize = minimum
            if (window.maximizedBounds != work) window.maximizedBounds = work
            if (resize && window.extendedState == Frame.NORMAL) {
                val fitted = fit(window.bounds, work)
                if (fitted != window.bounds) window.bounds = fitted
            }
        }
        window.size = opening
        refresh(resize = true)
        val timer = Timer(180) { refresh(resize = true) }.apply { isRepeats = false }
        val listener = object : ComponentAdapter() {
            override fun componentMoved(event: ComponentEvent) { timer.restart() }
            override fun componentResized(event: ComponentEvent) { timer.restart() }
        }
        val screenListener = PropertyChangeListener { timer.restart() }
        window.addComponentListener(listener)
        window.addPropertyChangeListener("graphicsConfiguration", screenListener)
        return {
            timer.stop()
            window.removeComponentListener(listener)
            window.removePropertyChangeListener("graphicsConfiguration", screenListener)
        }
    }
}
