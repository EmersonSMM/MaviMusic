package com.music.mavimusic.android.data

import android.content.Context
import android.webkit.CookieManager
import com.music.mavimusic.data.YtMusicRepository
import com.music.mavimusic.data.innertube.Innertube
import com.music.mavimusic.data.innertube.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Manages optional YouTube Music account cookies for Android, sharing the exact
 * [Innertube.cookie] + [StreamResolver.onSessionChanged] pipeline used on Desktop.
 */
object AndroidYouTubeAuth {

    data class SessionState(
        val signedIn: Boolean = false,
        val accountName: String? = null,
        val checking: Boolean = false,
    )

    private val SIGNING_COOKIES = listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    val isSignedIn: Boolean get() = _state.value.signedIn

    fun hasAuthCookies(rawCookie: String?): Boolean {
        if (rawCookie.isNullOrBlank()) return false
        val map = parseCookies(rawCookie)
        return SIGNING_COOKIES.any { !map[it].isNullOrBlank() }
    }

    internal fun parseCookies(rawCookie: String): Map<String, String> =
        rawCookie.split(';').mapNotNull { entry ->
            val name = entry.substringBefore('=').trim()
            val value = entry.substringAfter('=', "").trim()
            if (name.isEmpty() || value.isEmpty()) null else name to value
        }.toMap()

    fun init(context: Context) {
        val prefs = AndroidPreferences(context.applicationContext)
        val savedCookie = prefs.getYouTubeCookie()
        val savedName = prefs.getYouTubeAccountName()
        if (hasAuthCookies(savedCookie)) {
            Innertube.cookie = savedCookie
            Innertube.selectChannel(null, null, "0")
            _state.value = SessionState(
                signedIn = true,
                accountName = savedName ?: "Cuenta de YouTube Music",
                checking = false,
            )
        } else {
            Innertube.cookie = null
            _state.value = SessionState(signedIn = false, accountName = null, checking = false)
        }
    }

    suspend fun adoptCookie(context: Context, rawCookie: String): Result<String> {
        val trimmed = rawCookie.trim()
        if (!hasAuthCookies(trimmed)) {
            return Result.failure(IllegalArgumentException("No se encontró una sesión activa de YouTube Music."))
        }
        _state.value = _state.value.copy(checking = true)
        return withContext(Dispatchers.IO) {
            try {
                Innertube.cookie = trimmed
                Innertube.selectChannel(null, null, "0")
                StreamResolver.onSessionChanged()

                val account = YtMusicRepository.account().getOrNull()
                val displayName = account?.name?.takeIf { it.isNotBlank() }
                    ?: account?.email?.takeIf { it.isNotBlank() }
                    ?: "Cuenta de YouTube Music"

                AndroidPreferences(context.applicationContext).saveYouTubeSession(trimmed, displayName)
                _state.value = SessionState(
                    signedIn = true,
                    accountName = displayName,
                    checking = false,
                )
                Result.success(displayName)
            } catch (e: Exception) {
                Innertube.cookie = null
                StreamResolver.onSessionChanged()
                _state.value = SessionState(signedIn = false, accountName = null, checking = false)
                Result.failure(e)
            }
        }
    }

    fun signOut(context: Context) {
        Innertube.cookie = null
        Innertube.selectChannel(null, null, null)
        StreamResolver.onSessionChanged()
        AndroidPreferences(context.applicationContext).clearYouTubeSession()
        runCatching {
            val manager = CookieManager.getInstance()
            manager.removeAllCookies(null)
            manager.flush()
        }
        _state.value = SessionState(signedIn = false, accountName = null, checking = false)
    }
}
