package com.music.mavimusic.desktop

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Checks GitHub releases for Windows updates, downloads the setup installer, and runs it cleanly. */
internal object DesktopWindowsUpdater {
    private const val GITHUB_REPO = "EmersonSMM/MaviMusic"
    private const val GITHUB_LATEST_API = "https://api.github.com/repos/$GITHUB_REPO/releases/latest"
    private const val KEY_WINDOWS_CHECKS = "windows_updater_checks"
    private const val KEY_WINDOWS_DOWNLOADS = "windows_updater_downloads"
    private const val KEY_LAST_CHECK = "windows_updater_last_check"

    @Serializable
    private data class GitHubRelease(
        val tag_name: String = "",
        val html_url: String = "",
        val body: String = "",
        val assets: List<GitHubAsset> = emptyList(),
    )

    @Serializable
    private data class GitHubAsset(
        val name: String = "",
        val browser_download_url: String = "",
        val size: Long = 0L,
    )

    data class State(
        val ready: Boolean = true,
        val checking: Boolean = false,
        val updateAvailable: Boolean = false,
        val latestVersion: String = "",
        val releaseNotes: String = "",
        val downloadUrl: String = "",
        val downloading: Boolean = false,
        val downloadProgress: Float = 0f,
        val installerPath: String? = null,
        val automaticChecks: Boolean = true,
        val automaticDownloads: Boolean = false,
        val message: String = "MaviMusic está actualizado.",
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    @Volatile var prepareForInstall: (() -> Unit)? = null

    fun start() {
        if (!DesktopPlatform.isWindows || !started.compareAndSet(false, true)) return
        val persistence = DesktopPersistence()
        val checks = persistence.boolean(KEY_WINDOWS_CHECKS, true)
        val downloads = persistence.boolean(KEY_WINDOWS_DOWNLOADS, false)
        mutableState.update { it.copy(automaticChecks = checks, automaticDownloads = downloads) }

        if (checks) {
            val lastCheck = persistence.string(KEY_LAST_CHECK, "0").toLongOrNull() ?: 0L
            val now = System.currentTimeMillis()
            // Auto-check once per 4 hours on startup
            if (now - lastCheck > 4 * 3600 * 1000L) {
                check(silent = true)
            }
        }
    }

    fun configure(checks: Boolean = state.value.automaticChecks, downloads: Boolean = state.value.automaticDownloads) {
        DesktopPersistence().apply {
            saveBoolean(KEY_WINDOWS_CHECKS, checks)
            saveBoolean(KEY_WINDOWS_DOWNLOADS, downloads)
        }
        mutableState.update { it.copy(automaticChecks = checks, automaticDownloads = downloads) }
    }

    fun check(silent: Boolean = false) {
        if (state.value.checking || state.value.downloading) return
        if (!silent) {
            mutableState.update { it.copy(checking = true, message = "Buscando actualizaciones…") }
        } else {
            mutableState.update { it.copy(checking = true) }
        }
        scope.launch {
            try {
                val connection = URI(GITHUB_LATEST_API).toURL().openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "MaviMusic-Desktop")
                connection.connectTimeout = 8_000
                connection.readTimeout = 10_000

                if (connection.responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = json.decodeFromString<GitHubRelease>(responseText)
                    val remoteTag = release.tag_name.trim()
                    val currentVersion = DesktopUpdateChecker.currentVersion

                    DesktopPersistence().saveString(KEY_LAST_CHECK, System.currentTimeMillis().toString())

                    if (isNewerVersion(remoteTag, currentVersion)) {
                        val setupAsset = release.assets.firstOrNull {
                            it.name.contains("windows-x64-setup.exe", ignoreCase = true) ||
                                (it.name.endsWith(".exe", ignoreCase = true) && !it.name.contains("portable", ignoreCase = true))
                        } ?: release.assets.firstOrNull { it.name.endsWith(".exe", ignoreCase = true) }

                        val downloadUrl = setupAsset?.browser_download_url.orEmpty()
                        mutableState.update {
                            it.copy(
                                checking = false,
                                updateAvailable = true,
                                latestVersion = remoteTag.removePrefix("v").removePrefix("V"),
                                releaseNotes = release.body,
                                downloadUrl = downloadUrl,
                                message = "MaviMusic $remoteTag disponible.",
                            )
                        }
                        if (state.value.automaticDownloads && downloadUrl.isNotBlank()) {
                            download()
                        }
                    } else {
                        mutableState.update {
                            it.copy(
                                checking = false,
                                updateAvailable = false,
                                message = "MaviMusic ya está en la versión más reciente ($currentVersion).",
                            )
                        }
                    }
                } else {
                    mutableState.update {
                        it.copy(
                            checking = false,
                            message = if (silent) it.message else "No se pudo verificar: HTTP ${connection.responseCode}",
                        )
                    }
                }
            } catch (e: Exception) {
                DesktopTrackLog.log("Windows updater check failed: ${e.message}")
                mutableState.update {
                    it.copy(
                        checking = false,
                        message = if (silent) it.message else "Error al buscar actualizaciones: ${e.message}",
                    )
                }
            }
        }
    }

    fun downloadAndInstall() {
        val current = state.value
        if (current.installerPath != null && File(current.installerPath).exists()) {
            installNow()
            return
        }
        download(autoInstall = true)
    }

    fun download(autoInstall: Boolean = false) {
        val url = state.value.downloadUrl
        if (url.isBlank() || state.value.downloading) return
        mutableState.update { it.copy(downloading = true, downloadProgress = 0f, message = "Descargando actualización…") }
        scope.launch {
            try {
                val tempDir = File(System.getProperty("java.io.tmpdir"), "mavimusic-update")
                tempDir.mkdirs()
                val filename = url.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "MaviMusic-setup.exe"
                val targetFile = File(tempDir, filename)

                val connection = URI(url).toURL().openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MaviMusic-Desktop")
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.connect()

                val totalLength = connection.contentLengthLong
                var bytesWritten = 0L

                connection.inputStream.use { input ->
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(65536)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesWritten += read
                            if (totalLength > 0L) {
                                val progress = (bytesWritten.toFloat() / totalLength).coerceIn(0f, 1f)
                                mutableState.update { it.copy(downloadProgress = progress) }
                            }
                        }
                    }
                }

                mutableState.update {
                    it.copy(
                        downloading = false,
                        downloadProgress = 1f,
                        installerPath = targetFile.absolutePath,
                        message = "Actualización descargada. Lista para instalar.",
                    )
                }

                if (autoInstall) {
                    installNow()
                }
            } catch (e: Exception) {
                DesktopTrackLog.log("Windows updater download failed: ${e.message}")
                mutableState.update {
                    it.copy(
                        downloading = false,
                        message = "Error al descargar: ${e.message}",
                    )
                }
            }
        }
    }

    fun installNow() {
        val path = state.value.installerPath ?: return
        val file = File(path)
        if (!file.exists()) return
        scope.launch {
            try {
                prepareForInstall?.invoke()
                ProcessBuilder(file.absolutePath).start()
                kotlin.system.exitProcess(0)
            } catch (e: Exception) {
                DesktopTrackLog.log("Could not launch Windows installer: ${e.message}")
            }
        }
    }

    internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val cleanRemote = remoteTag.trim().removePrefix("v").removePrefix("V")
        val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
        if (cleanCurrent.equals("development", ignoreCase = true)) return false
        val remoteParts = cleanRemote.split('.', '-').mapNotNull { it.toIntOrNull() }
        val currentParts = cleanCurrent.split('.', '-').mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
