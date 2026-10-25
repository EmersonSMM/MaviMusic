package com.music.mavimusic.desktop

import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

/** One native preference read when the wordmark is composed, no timer or background observer. */
internal object DesktopMotionPreference {
    private interface UserPreferences : StdCallLibrary {
        fun SystemParametersInfoW(action: Int, parameter: Int, value: IntByReference, flags: Int): Boolean
    }
    fun reduceMotion(): Boolean = runCatching {
        when {
            DesktopPlatform.isWindows -> {
                val enabled = IntByReference(1)
                Native.load("user32", UserPreferences::class.java).SystemParametersInfoW(0x1042, 0, enabled, 0) && enabled.value == 0
            }
            DesktopPlatform.isMac -> {
                val objc = NativeLibrary.getInstance("objc")
                val cls = objc.getFunction("objc_getClass").invokePointer(arrayOf("NSWorkspace"))
                val selector = objc.getFunction("sel_registerName")
                val send = objc.getFunction("objc_msgSend")
                val workspace = send.invokePointer(arrayOf(cls, selector.invokePointer(arrayOf("sharedWorkspace"))))
                send.invokeInt(arrayOf(workspace, selector.invokePointer(arrayOf("accessibilityDisplayShouldReduceMotion")))) != 0
            }
            else -> false
        }
    }.getOrDefault(true)
}
