package com.music.bitchord.desktop

import java.awt.Cursor
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.Window
import java.awt.image.BufferedImage
import java.beans.PropertyChangeListener

/**
 * Eliminates the archaic 1995 oversized pixelated Java resize arrow cursors on macOS
 * and replaces them with sleek, anti-aliased modern double-ended cursors.
 */
internal object DesktopCursorFix {

    private var updating = false
    private val modernCursors = mutableMapOf<Int, Cursor>()

    fun install(window: Window) {
        if (!DesktopPlatform.isMac) return

        val listener = PropertyChangeListener { evt ->
            if (updating) return@PropertyChangeListener
            val cursor = evt.newValue as? Cursor ?: return@PropertyChangeListener
            val type = cursor.type
            if (type in RESIZE_TYPES) {
                val modern = getOrCreateModernCursor(type)
                if (modern != null && cursor != modern) {
                    updating = true
                    try {
                        window.cursor = modern
                    } finally {
                        updating = false
                    }
                }
            }
        }

        window.addPropertyChangeListener("cursor", listener)
    }

    private val RESIZE_TYPES = setOf(
        Cursor.SE_RESIZE_CURSOR,
        Cursor.NW_RESIZE_CURSOR,
        Cursor.NE_RESIZE_CURSOR,
        Cursor.SW_RESIZE_CURSOR,
        Cursor.E_RESIZE_CURSOR,
        Cursor.W_RESIZE_CURSOR,
        Cursor.N_RESIZE_CURSOR,
        Cursor.S_RESIZE_CURSOR,
    )

    private fun getOrCreateModernCursor(type: Int): Cursor? {
        return modernCursors.getOrPut(type) {
            createSleekCursor(type) ?: return null
        }
    }

    private fun createSleekCursor(type: Int): Cursor? {
        val size = 24
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        val cx = size / 2f
        val cy = size / 2f

        when (type) {
            Cursor.SE_RESIZE_CURSOR, Cursor.NW_RESIZE_CURSOR -> {
                // Diagonal NW-SE double-headed arrow
                drawDiagonalArrow(g, cx, cy, isNwSe = true)
            }
            Cursor.NE_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR -> {
                // Diagonal NE-SW double-headed arrow
                drawDiagonalArrow(g, cx, cy, isNwSe = false)
            }
            Cursor.E_RESIZE_CURSOR, Cursor.W_RESIZE_CURSOR -> {
                // Horizontal double-headed arrow
                drawHorizontalArrow(g, cx, cy)
            }
            Cursor.N_RESIZE_CURSOR, Cursor.S_RESIZE_CURSOR -> {
                // Vertical double-headed arrow
                drawVerticalArrow(g, cx, cy)
            }
            else -> return null
        }

        g.dispose()
        return Toolkit.getDefaultToolkit().createCustomCursor(img, Point(size / 2, size / 2), "MaviResize_$type")
    }

    private fun drawDiagonalArrow(g: java.awt.Graphics2D, cx: Float, cy: Float, isNwSe: Boolean) {
        val oldTransform = g.transform
        g.translate(cx.toDouble(), cy.toDouble())
        if (!isNwSe) {
            g.rotate(Math.toRadians(90.0))
        }

        // Draw shadow/outline first
        g.color = java.awt.Color(0, 0, 0, 180)
        g.stroke = java.awt.BasicStroke(3.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(-7, -7, 7, 7)
        // Arrow heads outline
        g.drawLine(-7, -7, -2, -7)
        g.drawLine(-7, -7, -7, -2)
        g.drawLine(7, 7, 2, 7)
        g.drawLine(7, 7, 7, 2)

        // Draw crisp white center
        g.color = java.awt.Color.WHITE
        g.stroke = java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(-7, -7, 7, 7)
        g.drawLine(-7, -7, -2, -7)
        g.drawLine(-7, -7, -7, -2)
        g.drawLine(7, 7, 2, 7)
        g.drawLine(7, 7, 7, 2)

        g.transform = oldTransform
    }

    private fun drawHorizontalArrow(g: java.awt.Graphics2D, cx: Float, cy: Float) {
        val oldTransform = g.transform
        g.translate(cx.toDouble(), cy.toDouble())

        g.color = java.awt.Color(0, 0, 0, 180)
        g.stroke = java.awt.BasicStroke(3.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(-8, 0, 8, 0)
        g.drawLine(-8, 0, -4, -4)
        g.drawLine(-8, 0, -4, 4)
        g.drawLine(8, 0, 4, -4)
        g.drawLine(8, 0, 4, 4)

        g.color = java.awt.Color.WHITE
        g.stroke = java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(-8, 0, 8, 0)
        g.drawLine(-8, 0, -4, -4)
        g.drawLine(-8, 0, -4, 4)
        g.drawLine(8, 0, 4, -4)
        g.drawLine(8, 0, 4, 4)

        g.transform = oldTransform
    }

    private fun drawVerticalArrow(g: java.awt.Graphics2D, cx: Float, cy: Float) {
        val oldTransform = g.transform
        g.translate(cx.toDouble(), cy.toDouble())

        g.color = java.awt.Color(0, 0, 0, 180)
        g.stroke = java.awt.BasicStroke(3.2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(0, -8, 0, 8)
        g.drawLine(0, -8, -4, -4)
        g.drawLine(0, -8, 4, -4)
        g.drawLine(0, 8, -4, 4)
        g.drawLine(0, 8, 4, 4)

        g.color = java.awt.Color.WHITE
        g.stroke = java.awt.BasicStroke(1.8f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND)
        g.drawLine(0, -8, 0, 8)
        g.drawLine(0, -8, -4, -4)
        g.drawLine(0, -8, 4, -4)
        g.drawLine(0, 8, -4, 4)
        g.drawLine(0, 8, 4, 4)

        g.transform = oldTransform
    }
}
