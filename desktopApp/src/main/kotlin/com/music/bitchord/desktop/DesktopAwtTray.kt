package com.music.bitchord.desktop

import java.awt.EventQueue
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/** The tray icon on the platforms that have no StatusNotifierItem. */
internal object DesktopAwtTray {

    private val lock = Any()
    private var icon: TrayIcon? = null
    private var image: BufferedImage? = null
    private var tooltip: String = APP_NAME

    /** Whether this platform has a tray AWT can reach. */
    fun isSupported(): Boolean = runCatching { SystemTray.isSupported() }.getOrDefault(false)

    /** The logo, rasterised. */
    fun setIcon(rendered: BufferedImage?) {
        if (rendered == null) return
        synchronized(lock) {
            image = rendered
            icon?.image = rendered
        }
    }

    fun start(onActivate: () -> Unit) {
        if (!isSupported()) return
        EventQueue.invokeLater {
            synchronized(lock) {
                if (icon != null) return@invokeLater
                val tray = runCatching { SystemTray.getSystemTray() }.getOrNull() ?: return@invokeLater
                val art = if (DesktopPlatform.isMac) {
                    createWhiteTemplateIcon(tray.trayIconSize.width.coerceIn(18, 22))
                } else {
                    image ?: blank(tray.trayIconSize.width.coerceAtLeast(16))
                }
                val created = if (DesktopPlatform.isMac) {
                    // On macOS, avoid the retro grey text menu on left-click; left-click directly activates Now Playing
                    TrayIcon(art, tooltip).apply {
                        isImageAutoSize = true
                        addActionListener { EventQueue.invokeLater(onActivate) }
                        addMouseListener(object : java.awt.event.MouseAdapter() {
                            override fun mouseReleased(e: java.awt.event.MouseEvent) {
                                if (e.isPopupTrigger || e.button == java.awt.event.MouseEvent.BUTTON3) {
                                    // Right click shows options
                                    popupMenu = DesktopTrayMenu.awtPopup()
                                } else if (e.button == java.awt.event.MouseEvent.BUTTON1) {
                                    EventQueue.invokeLater(onActivate)
                                }
                            }
                        })
                    }
                } else {
                    TrayIcon(art, tooltip, DesktopTrayMenu.awtPopup()).apply {
                        isImageAutoSize = true
                        addActionListener { EventQueue.invokeLater(onActivate) }
                    }
                }
                runCatching { tray.add(created) }
                    .onSuccess { icon = created }
                    .onFailure { DesktopTrackLog.log("tray unavailable: ${it.message}") }
            }
        }
    }

    fun stop() {
        EventQueue.invokeLater {
            synchronized(lock) {
                val current = icon ?: return@invokeLater
                runCatching { SystemTray.getSystemTray().remove(current) }
                icon = null
            }
        }
    }

    /**
     * Re-labels the icon, and rebuilds the menu because one of its entries is Play or Pause
     * depending on this.
     */
    fun update(title: String?, isPlaying: Boolean) {
        val text = title?.let { "$APP_NAME — $it" } ?: APP_NAME
        EventQueue.invokeLater {
            synchronized(lock) {
                tooltip = text
                val current = icon ?: return@invokeLater
                current.toolTip = text
                current.popupMenu = DesktopTrayMenu.awtPopup()
            }
        }
    }

    /** PNG bytes to an image AWT can hand the tray. */
    fun decode(png: ByteArray): BufferedImage? =
        runCatching { ImageIO.read(ByteArrayInputStream(png)) }.getOrNull()

    /** A placeholder while the logo is still being rasterised. */
    private fun blank(size: Int) = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    /** Generates a crisp, aesthetic white monochrome template icon for the macOS menu bar. */
    private fun createWhiteTemplateIcon(size: Int): BufferedImage {
        val s = size.coerceIn(18, 24)
        val img = BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = java.awt.Color.WHITE
        val scale = s / 22.0f
        val bars = listOf(
            Triple(3.0f, 6.0f, 10.0f),
            Triple(7.0f, 2.5f, 17.0f),
            Triple(11.0f, 7.5f, 7.0f),
            Triple(15.0f, 4.0f, 14.0f),
        )
        for (bar in bars) {
            val x = bar.first * scale
            val y = bar.second * scale
            val w = 2.4f * scale
            val h = bar.third * scale
            g.fill(java.awt.geom.RoundRectangle2D.Float(x, y, w, h, w, w))
        }
        g.dispose()
        return img
    }

    private const val APP_NAME = "MaviMusic"
}
