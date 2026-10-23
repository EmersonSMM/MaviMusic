package com.music.mavimusic.desktop

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Sparkle owns download verification, release notes, installation and rollback on macOS. */
internal object DesktopMacUpdater {
    data class State(
        val ready: Boolean = false,
        val automaticChecks: Boolean = true,
        val automaticDownloads: Boolean = false,
        val message: String = "Las actualizaciones automáticas están disponibles en la app instalada para macOS.",
    )
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val started = AtomicBoolean(false)
    @Volatile var prepareForInstall: (() -> Unit)? = null

    /** Never load native code in tests, an IDE launch or another platform. */
    fun start() {
        if (!DesktopPlatform.isMac || !started.compareAndSet(false, true)) return
        runCatching {
            val library = libraryPath(System.getProperty("jpackage.app-path")?.let(Path::of))
                ?: return
            System.load(library.toString())
            val error = nativeStart()
            check(error.isBlank()) { error }
            refreshPreferences("Actualizaciones verificadas desde GitHub.")
        }.onFailure { failure ->
            mutableState.update { it.copy(ready = false, message = "No se pudo iniciar el actualizador: ${failure.message}") }
            DesktopTrackLog.log("Updater could not start: ${failure.message}")
        }
    }

    internal fun libraryPath(launcher: Path?): Path? {
        val contents = launcher?.toAbsolutePath()?.parent?.takeIf { it.fileName.toString() == "MacOS" }?.parent ?: return null
        if (contents.fileName.toString() != "Contents" || !contents.parent.fileName.toString().endsWith(".app")) return null
        return contents.resolve("Frameworks/libmavimusic_updates.dylib").takeIf(Files::isRegularFile)
    }

    fun check() {
        if (!state.value.ready) return
        mutableState.update { it.copy(message = "Buscando actualizaciones…") }
        nativeCheck()
    }

    fun configure(checks: Boolean = state.value.automaticChecks, downloads: Boolean = state.value.automaticDownloads) {
        if (!state.value.ready) return
        nativeConfigure(checks, downloads)
        mutableState.update { it.copy(automaticChecks = checks, automaticDownloads = downloads) }
    }

    private fun refreshPreferences(message: String) {
        val flags = nativePreferences()
        mutableState.value = State(true, flags and 1 != 0, flags and 2 != 0, message)
    }

    @JvmStatic
    private fun onNativeEvent(event: Int, message: String) {
        when (event) {
            1 -> mutableState.update { it.copy(message = "MaviMusic $message disponible.") }
            2 -> refreshPreferences(message.takeIf(String::isNotBlank) ?: "Comprobación finalizada.")
            3 -> {
                val prepare = prepareForInstall
                if (prepare != null) prepare() else readyToInstall()
            }
        }
    }

    fun readyToInstall() { nativeReadyToInstall() }
    private external fun nativeStart(): String
    private external fun nativeCheck()
    private external fun nativePreferences(): Int
    private external fun nativeConfigure(checks: Boolean, downloads: Boolean)
    private external fun nativeReadyToInstall()
}
