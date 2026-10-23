package com.music.mavimusic.desktop

import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

internal object DesktopFolderPicker {

    /**
     * Opens a native directory picker dialog and returns the selected folder path,
     * or null if cancelled.
     */
    fun pickFolder(initialPath: String? = null): String? {
        var selectedPath: String? = null
        val runnable = java.lang.Runnable {
            if (DesktopPlatform.isMac) {
                val previousProperty = System.getProperty("apple.awt.fileDialogForDirectories")
                try {
                    System.setProperty("apple.awt.fileDialogForDirectories", "true")
                    val dialog = FileDialog(null as Frame?, "Seleccionar carpeta de descargas", FileDialog.LOAD)
                    if (!initialPath.isNullOrBlank()) {
                        dialog.directory = initialPath
                    }
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    if (dir != null) {
                        val candidate = if (file != null) File(dir, file) else File(dir)
                        selectedPath = if (candidate.exists()) candidate.absolutePath else File(dir).absolutePath
                    }
                } finally {
                    if (previousProperty != null) {
                        System.setProperty("apple.awt.fileDialogForDirectories", previousProperty)
                    } else {
                        System.clearProperty("apple.awt.fileDialogForDirectories")
                    }
                }
            } else {
                val chooser = JFileChooser().apply {
                    dialogTitle = "Seleccionar carpeta de descargas"
                    fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                    if (!initialPath.isNullOrBlank()) {
                        currentDirectory = File(initialPath)
                    }
                }
                val result = chooser.showOpenDialog(null)
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedPath = chooser.selectedFile?.absolutePath
                }
            }
        }

        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run()
        } else {
            SwingUtilities.invokeAndWait(runnable)
        }

        return selectedPath
    }
}
