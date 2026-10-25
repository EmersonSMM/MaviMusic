package com.music.mavimusic.desktop

import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Local offline generator for shareable "Wrapped" summary cards.
 * Generates high-resolution PNG image without network dependencies or tracking.
 */
object DesktopWrappedExporter {

    fun exportPng(
        summary: DesktopReplaySummary,
        period: DesktopReplayPeriod,
        destinationFile: File
    ): Boolean = runCatching {
        val width = 800
        val height = 1000
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g: Graphics2D = image.createGraphics()

        try {
            // Enable high quality antialiasing
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)

            // Deep gradient background
            val gradient = GradientPaint(
                0f, 0f, Color(16, 18, 24),
                width.toFloat(), height.toFloat(), Color(32, 24, 44)
            )
            g.paint = gradient
            g.fillRect(0, 0, width, height)

            // Card border / specular rim
            g.color = Color(255, 255, 255, 25)
            g.drawRoundRect(24, 24, width - 48, height - 48, 28, 28)

            // Header - App Brand
            g.color = Color(255, 60, 60) // Accent red/pink
            g.font = Font("SansSerif", Font.BOLD, 18)
            g.drawString("MAVIMUSIC • WRAPPED", 50, 75)

            // Subtitle - Period
            g.color = Color(255, 255, 255, 180)
            g.font = Font("SansSerif", Font.PLAIN, 15)
            val periodLabel = summary.label.ifBlank { period.chip }
            g.drawString("Resumen musical local • $periodLabel", 50, 102)

            // Glass stat card 1: Total Listening Time
            drawStatBox(
                g, x = 50, y = 130, w = width - 100, h = 110,
                title = "TIEMPO TOTAL ESCUCHADO",
                main = "${summary.minutes} minutos (${summary.hours} h)",
                subtitle = summary.comparisonPercent?.let {
                    if (it >= 0) "+$it% respecto al período anterior" else "$it% respecto al período anterior"
                } ?: "Historial 100% privado guardado en tu Mac"
            )

            // Glass stat card 2: Top Artist
            val topArtist = summary.artists.firstOrNull()?.title ?: "Sin reproducciones aún"
            val topArtistPlays = summary.artists.firstOrNull()?.plays ?: 0
            val topArtistMs = summary.artists.firstOrNull()?.ms ?: 0L
            val topArtistMins = topArtistMs / 60_000L
            drawStatBox(
                g, x = 50, y = 260, w = width - 100, h = 120,
                title = "TU ARTISTA #1",
                main = topArtist,
                subtitle = "$topArtistPlays reproducciones • $topArtistMins minutos"
            )

            // Glass stat card 3: Top Song
            val topSong = summary.songs.firstOrNull()?.song?.title ?: "Sin reproducciones aún"
            val topSongArtist = summary.songs.firstOrNull()?.song?.artist ?: ""
            val topSongPlays = summary.songs.firstOrNull()?.plays ?: 0
            drawStatBox(
                g, x = 50, y = 400, w = width - 100, h = 120,
                title = "TU CANCIÓN #1",
                main = topSong,
                subtitle = "$topSongArtist • $topSongPlays reproducciones"
            )

            // Two-column stat boxes: Streak & Diversity
            val colW = (width - 120) / 2
            drawStatBox(
                g, x = 50, y = 540, w = colW, h = 110,
                title = "RACHA ACTIVA",
                main = "${summary.streakDays} días",
                subtitle = "Días seguidos con música"
            )

            drawStatBox(
                g, x = 50 + colW + 20, y = 540, w = colW, h = 110,
                title = "DIVERSIDAD MUSICAL",
                main = "${summary.distinctArtists} artistas",
                subtitle = "${summary.distinctSongs} pistas distintas"
            )

            // Top 3 Chart summary
            g.color = Color(255, 255, 255, 240)
            g.font = Font("SansSerif", Font.BOLD, 18)
            g.drawString("Top Artistas Destacados", 50, 695)

            val top3 = summary.artists.take(3)
            var yOffset = 730
            top3.forEachIndexed { idx, artist ->
                g.color = Color(255, 60, 60)
                g.font = Font("SansSerif", Font.BOLD, 16)
                g.drawString("#${idx + 1}", 55, yOffset)

                g.color = Color.WHITE
                g.font = Font("SansSerif", Font.PLAIN, 16)
                val cleanTitle = if (artist.title.length > 32) artist.title.take(30) + "…" else artist.title
                g.drawString(cleanTitle, 95, yOffset)

                g.color = Color(255, 255, 255, 140)
                g.font = Font("SansSerif", Font.PLAIN, 14)
                g.drawString("${artist.plays} repr. • ${artist.ms / 60_000L} min", width - 260, yOffset)
                yOffset += 40
            }

            // Footer
            g.color = Color(255, 255, 255, 90)
            g.font = Font("SansSerif", Font.PLAIN, 13)
            g.drawString("Datos 100% locales • Sin telemetría ni publicidad externa • MaviMusic macOS", 50, 935)

        } finally {
            g.dispose()
        }

        ImageIO.write(image, "PNG", destinationFile)
    }.getOrDefault(false)

    private fun drawStatBox(
        g: Graphics2D,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        title: String,
        main: String,
        subtitle: String
    ) {
        // Frosted card background
        g.color = Color(255, 255, 255, 14)
        g.fillRoundRect(x, y, w, h, 16, 16)
        g.color = Color(255, 255, 255, 28)
        g.drawRoundRect(x, y, w, h, 16, 16)

        // Title
        g.color = Color(255, 255, 255, 150)
        g.font = Font("SansSerif", Font.BOLD, 11)
        g.drawString(title, x + 18, y + 26)

        // Main stat
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, 22)
        val cleanMain = if (main.length > 28) main.take(26) + "…" else main
        g.drawString(cleanMain, x + 18, y + 62)

        // Subtitle
        g.color = Color(255, 255, 255, 160)
        g.font = Font("SansSerif", Font.PLAIN, 13)
        g.drawString(subtitle, x + 18, y + 90)
    }

    fun promptAndExport(
        summary: DesktopReplaySummary,
        period: DesktopReplayPeriod,
        parentFrame: java.awt.Frame? = null,
    ): File? = runCatching {
        val defaultName = "MaviMusic-Wrapped-${period.name.lowercase()}.png"
        val dialog = java.awt.FileDialog(parentFrame, "Guardar tarjeta Wrapped", java.awt.FileDialog.SAVE).apply {
            file = defaultName
            isVisible = true
        }
        val dir = dialog.directory ?: return@runCatching null
        val filename = dialog.file ?: return@runCatching null
        val target = File(dir, if (filename.endsWith(".png", ignoreCase = true)) filename else "$filename.png")
        if (exportPng(summary, period, target)) target else null
    }.getOrNull()

    fun promptAndExportData(
        content: String,
        defaultFileName: String,
        dialogTitle: String,
        parentFrame: java.awt.Frame? = null,
    ): File? = runCatching {
        val dialog = java.awt.FileDialog(parentFrame, dialogTitle, java.awt.FileDialog.SAVE).apply {
            file = defaultFileName
            isVisible = true
        }
        val dir = dialog.directory ?: return@runCatching null
        val filename = dialog.file ?: return@runCatching null
        val target = File(dir, filename)
        target.writeText(content, java.nio.charset.StandardCharsets.UTF_8)
        target
    }.getOrNull()
}
