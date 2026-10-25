package com.music.mavimusic.desktop

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val digest: String = "",
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
        val downloadSize: Long = 0,
        val downloadSha256: String = "",
        val installing: Boolean = false,
        val lastError: String? = null,
        val automaticChecks: Boolean = true,
        val automaticDownloads: Boolean = false,
        val message: String = "MaviMusic está actualizado.",
    )

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val downloading = AtomicBoolean(false)
    private val installing = AtomicBoolean(false)
    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    @Volatile var prepareForInstall: (() -> Unit)? = null

    fun start() {
        if (!DesktopPlatform.isWindows || !started.compareAndSet(false, true)) return
        readInstallResult()
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
        if (state.value.checking || state.value.downloading || state.value.installing) return
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
                        val version = remoteTag.removePrefix("v")
                        val expectedName = "MaviMusic-$version-windows-x64.msi"
                        val setupAsset = release.assets.singleOrNull { it.name == expectedName }
                            ?: error("La versión no incluye el MSI de Windows")
                        require(setupAsset.size in 1..2_147_483_648L && setupAsset.digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))) { "Falta la integridad del instalador" }
                        val downloadUrl = "https://github.com/$GITHUB_REPO/releases/download/v$version/$expectedName"
                        require(setupAsset.browser_download_url == downloadUrl) { "Origen del instalador no válido" }
                        mutableState.update {
                            it.copy(
                                checking = false,
                                updateAvailable = true,
                                latestVersion = remoteTag.removePrefix("v").removePrefix("V"),
                                releaseNotes = release.body,
                                downloadUrl = downloadUrl,
                                downloadSize = setupAsset.size,
                                downloadSha256 = setupAsset.digest.substringAfter(":").lowercase(),
                                installerPath = if (it.latestVersion == version && it.downloadSha256 == setupAsset.digest.substringAfter(":").lowercase()) it.installerPath else null,
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
        if (state.value.installerPath != null) installNow() else download(autoInstall = true)
    }

    internal fun verifyInstaller(file: Path, size: Long, sha256: String) {
        require(size > 0 && Files.size(file) == size) { "El tamaño del instalador no coincide" }
        require(sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Falta el hash del instalador" }
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(65536)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        require(digest.digest().joinToString("") { "%02x".format(it) }.equals(sha256, true)) { "La integridad del instalador no coincide" }
    }

    fun download(autoInstall: Boolean = false) {
        val update = state.value
        if (update.downloadUrl.isBlank() || !downloading.compareAndSet(false, true)) return
        mutableState.update { it.copy(downloading = true, installerPath = null, lastError = null, downloadProgress = 0f, message = "Descargando actualización…") }
        scope.launch {
            var partial: Path? = null
            var connection: HttpURLConnection? = null
            try {
                val directory = Files.createTempDirectory("mavimusic-update-")
                val pending = directory.resolve("installer.partial")
                partial = pending
                val target = directory.resolve("MaviMusic-${update.latestVersion}.msi")
                connection = URI(update.downloadUrl).toURL().openConnection() as HttpURLConnection
                connection.setRequestProperty("User-Agent", "MaviMusic-Desktop")
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                require(connection.responseCode == 200) { "No se pudo descargar el instalador" }
                var count = 0L
                connection.inputStream.use { input -> Files.newOutputStream(pending).use { output ->
                    val buffer = ByteArray(65536)
                    while (true) {
                        val read = input.read(buffer); if (read < 0) break
                        count += read
                        require(count <= update.downloadSize) { "El instalador excede el tamaño esperado" }
                        output.write(buffer, 0, read)
                        mutableState.update { it.copy(downloadProgress = (count.toFloat() / update.downloadSize).coerceIn(0f, 1f)) }
                    }
                } }
                verifyInstaller(pending, update.downloadSize, update.downloadSha256)
                Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING)
                mutableState.update { it.copy(downloading = false, installerPath = target.toString(), downloadProgress = 1f, message = "Actualización verificada. Lista para instalar.") }
                if (autoInstall) installNow()
            } catch (failure: Exception) {
                partial?.let { runCatching { Files.deleteIfExists(it) } }
                mutableState.update { it.copy(downloading = false, installerPath = null, lastError = "No se pudo verificar o descargar la actualización. Puedes reintentar.", message = "Actualización no instalada.") }
                DesktopTrackLog.log("Windows updater download failed: ${failure.message}")
            } finally { connection?.disconnect(); downloading.set(false) }
        }
    }

    private fun stateDirectory(): Path = Path.of(System.getProperty("user.home"), ".local", "state", "mavimusic", "updates")

    private fun readInstallResult() {
        runCatching {
            val result = stateDirectory().resolve("result.json")
            if (!Files.exists(result) || Files.size(result) > 16384) return
            val status = json.parseToJsonElement(Files.readString(result).trim().removePrefix("\uFEFF")).jsonObject["status"]?.jsonPrimitive?.content
            if (status != "success") mutableState.update { it.copy(lastError = "La actualización anterior no se completó. Se intentó recuperar la versión previa; puedes reintentar. Consulta el registro local de actualización.") }
            Files.deleteIfExists(result)
        }
    }

    fun installNow() {
        val update = state.value
        val file = update.installerPath?.let { Path.of(it) } ?: return
        if (!installing.compareAndSet(false, true)) return
        mutableState.update { it.copy(installing = true, lastError = null, message = "Preparando instalación silenciosa y recuperación…") }
        scope.launch {
            var helper: Process? = null
            try {
                verifyInstaller(file, update.downloadSize, update.downloadSha256)
                val launcher = System.getProperty("jpackage.app-path")?.let { Path.of(it) }
                    ?: error("Se necesita la app instalada por usuario; la versión portable requiere instalación manual")
                Files.deleteIfExists(file.parent.resolve("ready"))
                val script = file.parent.resolve("update.ps1")
                javaClass.getResourceAsStream("/updates/windows-update.ps1")!!.use { Files.copy(it, script, StandardCopyOption.REPLACE_EXISTING) }
                val command = mutableListOf("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.toString(),
                    "-AppPid", ProcessHandle.current().pid().toString(), "-Installer", file.toString(), "-ExpectedHash", update.downloadSha256,
                    "-InstallRoot", launcher.parent.toString(), "-Launcher", launcher.toString(), "-StateDirectory", stateDirectory().toString())
                if (!DesktopWindowVisibility.visible.value) command += "-StartHidden"
                helper = ProcessBuilder(command).redirectErrorStream(true).redirectOutput(file.parent.resolve("helper.log").toFile()).start()
                withTimeout(90_000) {
                    while (!Files.exists(file.parent.resolve("ready"))) {
                        check(helper?.isAlive == true) { "El auxiliar no pudo preparar una actualización por usuario. Consulta su registro; la app sigue abierta." }
                        delay(100)
                    }
                }
                prepareForInstall?.invoke()
                kotlin.system.exitProcess(0)
            } catch (failure: Exception) {
                helper?.destroy()
                installing.set(false)
                mutableState.update { it.copy(installing = false, lastError = "No se pudo preparar la instalación silenciosa. La app sigue abierta. Puedes reintentar o descargar el instalador manual.", message = "Actualización no instalada.") }
                DesktopTrackLog.log("Windows updater preparation failed: ${failure.message}")
            }
        }
    }

    internal fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        fun parts(value: String): List<Int>? {
            val clean = value.trim().removePrefix("v").removePrefix("V")
            if (!Regex("[0-9]+[.][0-9]+[.][0-9]+").matches(clean)) return null
            return clean.split('.').map { it.toIntOrNull() ?: return null }
        }
        val remote = parts(remoteTag) ?: return false
        val current = parts(currentVersion) ?: return false
        for (i in 0..2) { if (remote[i] != current[i]) return remote[i] > current[i] }
        return false
    }
}
