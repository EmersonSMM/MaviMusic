package com.music.bitchord.desktop

import com.music.bitchord.data.model.Song
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.Taskbar
import javax.swing.SwingUtilities

/**
 * Native macOS Dock icon contextual menu.
 *
 * Provides quick-action controls directly from the Dock:
 * Current song display, Play/Pause, Next track, Previous track, Like/Favorite, and Sleep timer status.
 */
internal object DesktopMacDockMenu {

    private var isInstalled = false
    private var controller: Controller? = null

    class Controller(
        val onPlay: () -> Unit,
        val onPause: () -> Unit,
        val onNext: () -> Unit,
        val onPrevious: () -> Unit,
        val onToggleLike: () -> Unit,
        val onCancelSleepTimer: () -> Unit,
    )

    fun start(controller: Controller) {
        if (!DesktopPlatform.isMac) return
        this.controller = controller
        isInstalled = true
    }

    fun stop() {
        if (!DesktopPlatform.isMac) return
        isInstalled = false
        controller = null
        runCatching {
            if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.MENU)) {
                Taskbar.getTaskbar().menu = null
            }
        }
    }

    fun update(
        song: Song?,
        isPlaying: Boolean,
        isLiked: Boolean,
        sleepTimerRemainingMs: Long?,
        sleepAfterTrack: Boolean,
    ) {
        if (!DesktopPlatform.isMac || !isInstalled) return
        val currentController = controller ?: return

        SwingUtilities.invokeLater {
            runCatching {
                if (!Taskbar.isTaskbarSupported()) return@runCatching
                val taskbar = Taskbar.getTaskbar()
                if (!taskbar.isSupported(Taskbar.Feature.MENU)) return@runCatching

                val menu = PopupMenu("MaviMusic")

                // 1. Current track header
                val trackLabel = if (song != null) {
                    val title = song.title.take(35)
                    val artist = song.artist.take(25)
                    "$title — $artist"
                } else {
                    "MaviMusic"
                }
                val headerItem = MenuItem(trackLabel).apply { isEnabled = false }
                menu.add(headerItem)
                menu.addSeparator()

                // 2. Play / Pause
                val playPauseItem = MenuItem(if (isPlaying) "Pausar" else "Reproducir").apply {
                    addActionListener {
                        if (isPlaying) currentController.onPause() else currentController.onPlay()
                    }
                }
                menu.add(playPauseItem)

                // 3. Next Track
                val nextItem = MenuItem("Siguiente canción").apply {
                    addActionListener { currentController.onNext() }
                }
                menu.add(nextItem)

                // 4. Previous Track
                val prevItem = MenuItem("Canción anterior").apply {
                    addActionListener { currentController.onPrevious() }
                }
                menu.add(prevItem)

                // 5. Like / Favorite
                if (song != null) {
                    menu.addSeparator()
                    val likeItem = MenuItem(if (isLiked) "♥ En canciones que me gustan" else "♡ Marcar con Me gusta").apply {
                        addActionListener { currentController.onToggleLike() }
                    }
                    menu.add(likeItem)
                }

                // 6. Sleep Timer status if active
                if (sleepTimerRemainingMs != null || sleepAfterTrack) {
                    menu.addSeparator()
                    val timerLabel = when {
                        sleepAfterTrack -> "Temporizador: fin de canción (Cancelar)"
                        sleepTimerRemainingMs != null -> {
                            val mins = (sleepTimerRemainingMs / 60_000L).coerceAtLeast(1)
                            "Temporizador: $mins min restantes (Cancelar)"
                        }
                        else -> "Temporizador activo"
                    }
                    val timerItem = MenuItem(timerLabel).apply {
                        addActionListener { currentController.onCancelSleepTimer() }
                    }
                    menu.add(timerItem)
                }

                taskbar.menu = menu
            }
        }
    }
}
