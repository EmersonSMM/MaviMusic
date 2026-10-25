package com.music.mavimusic.desktop

import androidx.compose.ui.awt.ComposeWindow
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.time.Instant
import java.util.Locale

/** A report built from an explicit allowlist. Raw logs, preferences and environment are never exported. */
internal object DesktopDiagnostics {
    data class Incident(val at: Instant, val category: String, val kind: String)
    private val incidents = ArrayDeque<Incident>()
    private const val LIMIT = 100
    private val propertyNames = listOf("os.name", "os.version", "os.arch", "java.version", "java.vendor", "java.vm.name")

    /** Retain classifications only, not message text, track names, URLs, paths or credentials. */
    @Synchronized fun recordLog(message: String) {
        val text = message.take(4096).lowercase(Locale.ROOT)
        if (listOf("failed", "failure", "error", "exception", "unavailable", "could not", "cannot", "timed out").none { it in text }) return
        val category = when {
            "window" in text || "frame" in text || "render" in text -> "window/rendering"
            "update" in text || "sparkle" in text -> "updates"
            "ffmpeg" in text || "decoder" in text || "audio" in text -> "audio"
            "resolve" in text || "stream" in text -> "source-resolution"
            "lyrics" in text -> "lyrics"
            else -> "other"
        }
        val kind = when {
            "cancel" in text -> "cancelled"
            "timeout" in text || "timed out" in text -> "timeout"
            "unsatisfiedlink" in text -> "native-library"
            "unavailable" in text -> "unavailable"
            else -> "failure"
        }
        if (incidents.size == LIMIT) incidents.removeFirst()
        incidents.addLast(Incident(Instant.now(), category, kind))
    }

    @Synchronized fun recentIncidents(): List<Incident> = incidents.toList()

    /** Called on the UI thread; screen coordinates are logical, display modes are physical pixels. */
    fun capture(window: ComposeWindow?): String {
        val runtime = Runtime.getRuntime()
        val details = linkedMapOf(
            "report.schema" to "1",
            "generated.utc" to Instant.now().toString(),
            "app.version" to DesktopUpdateChecker.currentVersion,
            "renderer.requested" to (System.getProperty("skiko.renderApi") ?: "automatic"),
            "renderer.actual" to (runCatching { window?.renderApi?.name }.getOrNull() ?: "not-captured"),
            "cpu.logicalProcessors" to runtime.availableProcessors().toString(),
            "jvm.heap.maxBytes" to runtime.maxMemory().toString(),
            "jvm.heap.usedBytes" to (runtime.totalMemory() - runtime.freeMemory()).toString(),
        )
        propertyNames.forEach { details[it] = System.getProperty(it) ?: "unknown" }
        if (window != null) {
            details["window.logicalBounds"] = "${window.x},${window.y},${window.width},${window.height}"
            details["window.placement"] = window.placement.name
            details["window.minimized"] = window.isMinimized.toString()
            details["window.fullscreenRequested"] = DesktopWindowMode.fullScreen.value.toString()
        }
        if (!GraphicsEnvironment.isHeadless()) runCatching {
            GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.take(16).forEachIndexed { index, device ->
                val config = device.defaultConfiguration
                val bounds = config.bounds
                val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
                details["screen.$index.logicalBounds"] = "${bounds.x},${bounds.y},${bounds.width},${bounds.height}"
                details["screen.$index.logicalWorkArea"] = "${bounds.x + insets.left},${bounds.y + insets.top},${bounds.width - insets.left - insets.right},${bounds.height - insets.top - insets.bottom}"
                details["screen.$index.scale"] = "${config.defaultTransform.scaleX},${config.defaultTransform.scaleY}"
                details["screen.$index.physicalPixels"] = "${device.displayMode.width}x${device.displayMode.height}"
                details["screen.$index.refreshHz"] = device.displayMode.refreshRate.toString()
                details["screen.$index.currentWindow"] = (window?.graphicsConfiguration?.device == device).toString()
            }
        }.onFailure { details["screens.status"] = "unavailable" }
        return format(details, recentIncidents())
    }

    internal fun format(details: Map<String, String>, events: List<Incident>): String = buildString {
        appendLine("MaviMusic — diagnóstico técnico")
        appendLine("No incluye cookies, credenciales, cuentas, biblioteca, rutas personales ni mensajes originales del registro.")
        appendLine("El renderizador solicitado puede diferir del renderizador real. Las incidencias son categorías de esta sesión.")
        appendLine()
        details.forEach { (key, value) -> appendLine("$key=${value.replace(Regex("[\\r\\n\\p{Cntrl}]"), " ").take(200)}") }
        appendLine()
        appendLine("Incidencias recientes (máximo $LIMIT; solo esta sesión):")
        events.takeLast(LIMIT).forEach { appendLine("${it.at} ${it.category} ${it.kind}") }
        if (events.isEmpty()) appendLine("Sin incidencias clasificadas en esta sesión.")
    }

    fun write(report: String, destination: Path) {
        val target = destination.toAbsolutePath()
        val temporary = Files.createTempFile(target.parent, ".mavimusic-diagnostic-", ".tmp")
        try {
            Files.writeString(temporary, report)
            try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING) }
            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING) }
        } finally { Files.deleteIfExists(temporary) }
    }
}
