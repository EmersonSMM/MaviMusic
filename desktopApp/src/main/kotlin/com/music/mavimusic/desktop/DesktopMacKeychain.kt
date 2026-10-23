package com.music.mavimusic.desktop

/** Direct Security.framework access. No shell process ever receives a session. */
internal object DesktopMacKeychain {
    private val loaded by lazy {
        check(DesktopPlatform.isMac)
        DesktopAnalysisRuntime.loadNative("mavimusic_mac_media")
        true
    }

    fun read(account: String): ByteArray? { check(loaded); return nativeRead(account) }
    fun write(account: String, value: ByteArray) { check(loaded); nativeWrite(account, value) }
    fun delete(account: String) { check(loaded); nativeDelete(account) }

    @JvmStatic private external fun nativeRead(account: String): ByteArray?
    @JvmStatic private external fun nativeWrite(account: String, value: ByteArray)
    @JvmStatic private external fun nativeDelete(account: String)
}
