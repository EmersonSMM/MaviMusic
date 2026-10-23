package com.music.mavimusic.desktop

import java.awt.Desktop
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.nio.file.Path
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

internal data class DesktopAudioOpenRequest(val paths: List<Path>, val playAfterImport: Boolean = true)

/** Buffers Finder events even while Compose and the audio engine are still starting. */
internal object DesktopOpenAudio {
    private val pending = Channel<DesktopAudioOpenRequest>(Channel.UNLIMITED)
    val requests = pending.receiveAsFlow()

    fun submit(paths: List<Path>, playAfterImport: Boolean = true) {
        if (paths.isEmpty()) return
        pending.trySend(DesktopAudioOpenRequest(paths, playAfterImport))
    }

    fun install(arguments: Array<String>) {
        if (Desktop.isDesktopSupported()) runCatching {
            val desktop = Desktop.getDesktop()
            if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
                desktop.setOpenFileHandler { event -> submit(event.files.map { it.toPath() }) }
            }
        }.onFailure { DesktopTrackLog.log("Could not register audio file handler: ${it.message}") }
        submit(arguments.mapNotNull { value ->
            runCatching {
                if (value.startsWith("file:")) Path.of(java.net.URI(value)) else Path.of(value)
            }.getOrNull()?.takeIf { java.nio.file.Files.exists(it) }
        })
    }

    fun chooseFiles() = EventQueue.invokeLater {
        val dialog = FileDialog(null as Frame?, "Abrir archivos de audio", FileDialog.LOAD)
        try {
            dialog.isMultipleMode = true
            dialog.setFilenameFilter { _, name -> DesktopLocalMusic.supports(Path.of(name)) }
            dialog.isVisible = true
            submit(dialog.files.map { it.toPath() })
        } finally { dialog.dispose() }
    }

    fun chooseFolder() = EventQueue.invokeLater {
        DesktopLocalMusic.chooseFolder()?.let { submit(listOf(it), playAfterImport = false) }
    }

    /** Native file drops use the same import/queue path as Finder and the file chooser. */
    fun attachDropTarget(window: java.awt.Window): DropTarget = DropTarget(window, object : DropTargetAdapter() {
        override fun drop(event: DropTargetDropEvent) {
            if (!event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.rejectDrop()
                return
            }
            try {
                event.acceptDrop(DnDConstants.ACTION_COPY)
                val files = (event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                    .orEmpty().filterIsInstance<java.io.File>()
                submit(files.map { it.toPath() })
                event.dropComplete(true)
            } catch (_: Exception) { event.dropComplete(false) }
        }
    })
}
