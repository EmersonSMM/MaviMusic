package com.music.mavimusic.desktop

import com.sun.jna.Native
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.platform.win32.WinDef.LPARAM
import java.awt.EventQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One blocking GetMessage loop; no keyboard hook, polling or main-window activation. */
internal object DesktopWindowsHotkey {
    data class Shortcut(val modifiers: Int, val key: Int)
    data class State(val shortcut: String = "Ctrl+Alt+K", val registered: Boolean = false, val message: String = "")
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val changes = Mutex()
    @Volatile private var threadId = 0
    private var worker: Thread? = null
    private var started = false
    private const val KEY = "windows_spotlight_shortcut"
    private const val ID = 0x4D56

    internal fun parse(text: String): Shortcut? {
        val parts = text.uppercase().split('+').map(String::trim)
        if (parts.size < 2 || parts.distinct().size != parts.size || parts.last().length != 1 || parts.last()[0] !in 'A'..'Z') return null
        var flags = 0
        for (part in parts.dropLast(1)) flags = flags or when (part) {
            "CTRL" -> 2; "ALT" -> 1; "SHIFT" -> 4; "WIN" -> 8; else -> return null
        }
        if (flags and (1 or 2 or 8) == 0) return null
        return Shortcut(flags or 0x4000, parts.last()[0].code)
    }

    @Synchronized fun start() {
        if (!DesktopPlatform.isWindows || started) return
        started = true
        Runtime.getRuntime().addShutdownHook(Thread({ stop() }, "MaviMusic shortcut cleanup"))
        configure(DesktopPersistence().string(KEY, "Ctrl+Alt+K"))
    }

    fun configure(text: String) {
        val shortcut = parse(text)
        if (shortcut == null) { mutableState.value = state.value.copy(message = "Usa Ctrl, Alt o Win con una letra, por ejemplo Ctrl+Alt+K."); return }
        if (!DesktopPlatform.isWindows) return
        scope.launch { changes.withLock {
            stop()
            worker?.join(1500)
            if (worker?.isAlive == true) { mutableState.value = State(text, false, "No se pudo liberar el atajo anterior. Reintenta."); return@withLock }
            DesktopPersistence().saveString(KEY, text)
            worker = Thread({
                val user32 = User32.INSTANCE
                val message = WinUser.MSG()
                user32.PeekMessage(message, null, 0, 0, 0)
                threadId = Kernel32.INSTANCE.GetCurrentThreadId()
                var registered = false
                try {
                    registered = user32.RegisterHotKey(null, ID, shortcut.modifiers, shortcut.key)
                    mutableState.value = State(text, registered, if (registered) "Atajo global activo." else "No se pudo registrar ($text), puede estar ocupado. Elige otra combinación.")
                    if (!registered) return@Thread
                    while (true) {
                        val result = user32.GetMessage(message, null, 0, 0)
                        if (result <= 0) break
                        if (message.message == WinUser.WM_HOTKEY && message.wParam.toInt() == ID) {
                            EventQueue.invokeLater { DesktopSpotlightState.toggle(external = true) }
                        }
                    }
                } catch (_: Throwable) { mutableState.value = State(text, false, "El registro global no está disponible. Ctrl+K dentro de la app sigue funcionando.") }
                finally {
                    if (registered) user32.UnregisterHotKey(null, ID)
                    threadId = 0
                }
            }, "MaviMusic global Spotlight").apply { isDaemon = true; start() }
        } }
    }

    fun stop() {
        val id = threadId
        if (id != 0) User32.INSTANCE.PostThreadMessage(id, WinUser.WM_QUIT, WPARAM(0), LPARAM(0))
    }
}
