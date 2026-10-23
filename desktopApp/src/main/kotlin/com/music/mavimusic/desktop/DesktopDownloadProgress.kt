package com.music.mavimusic.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Only measured transfer bytes contribute to speed and ETA. Other stages are indeterminate. */
enum class DesktopDownloadStage(val label: String) {
    SONG("Buscando versión canción"), METADATA("Consultando metadatos"), RESOLVING("Resolviendo fuente"),
    TRANSFER("Descargando audio"), ARTWORK("Obteniendo carátula"), CONVERTING("Preparando MP3"), VERIFYING("Verificando MP3")
}

data class DesktopDownloadProgress(val stage: DesktopDownloadStage, val fraction: Float = 0f,
    val bytesPerSecond: Long? = null, val remainingSeconds: Long? = null) {
    fun label(): String = buildString {
        append(stage.label)
        if (fraction > 0f) append(" · ${(fraction * 100).toInt()}%")
        bytesPerSecond?.let { append(" · %.1f MB/s".format(it / 1_000_000.0)) }
        remainingSeconds?.let { append(" · ${it / 60}:%02d restantes".format(it % 60)) }
    }
}

internal data class DesktopDownloadReceipt(val title: String, val path: String, val info: DesktopExportInfo,
    val milliseconds: Map<DesktopDownloadStage, Long>, val note: String?, val completedAt: Long = System.nanoTime()) {
    val metadataSummary: String get() = listOf("title" to "título", "artist" to "artista", "album" to "álbum", "track" to "pista")
        .joinToString(" · ") { (key, label) -> "$label ${if (info.tags[key].isNullOrBlank()) "pendiente" else "✓"}" } +
        " · carátula ${if (info.hasCover) "✓" else "pendiente"}"
}

internal object DesktopDownloadHistory {
    private val _recent = MutableStateFlow<List<DesktopDownloadReceipt>>(emptyList())
    val recent: StateFlow<List<DesktopDownloadReceipt>> = _recent
    @Synchronized fun record(receipt: DesktopDownloadReceipt) { _recent.value = (listOf(receipt) + _recent.value).take(30) }
}

internal class DesktopDownloadMeasurement(private val notify: (DesktopDownloadProgress) -> Unit) {
    private var stage: DesktopDownloadStage? = null
    private var since = System.nanoTime()
    private var transferStart = since
    private var firstBytes: Long? = null
    val milliseconds = linkedMapOf<DesktopDownloadStage, Long>()
    @Synchronized fun stage(value: DesktopDownloadStage) {
        val now = System.nanoTime()
        stage?.let { milliseconds[it] = (milliseconds[it] ?: 0) + (now - since) / 1_000_000 }
        stage = value; since = now
        if (value == DesktopDownloadStage.TRANSFER) transferStart = now
        notify(DesktopDownloadProgress(value))
    }
    @Synchronized fun bytes(done: Long, total: Long?) {
        if (firstBytes == null) firstBytes = done
        val elapsed = (System.nanoTime() - transferStart) / 1e9
        val speed = if (elapsed >= .5) ((done - (firstBytes ?: 0)) / elapsed).toLong().takeIf { it > 0 } else null
        notify(DesktopDownloadProgress(DesktopDownloadStage.TRANSFER,
            if (total != null && total > 0) (done.toDouble() / total).toFloat().coerceIn(0f, 1f) else 0f,
            speed, if (speed != null && total != null) ((total - done).coerceAtLeast(0) / speed) else null))
    }
    @Synchronized fun finish(): Map<DesktopDownloadStage, Long> {
        val now = System.nanoTime()
        stage?.let { milliseconds[it] = (milliseconds[it] ?: 0) + (now - since) / 1_000_000 }
        stage = null
        DesktopTrackLog.log("download timings (ms): " + milliseconds.entries.joinToString { "${it.key}=${it.value}" })
        return milliseconds.toMap()
    }
}
