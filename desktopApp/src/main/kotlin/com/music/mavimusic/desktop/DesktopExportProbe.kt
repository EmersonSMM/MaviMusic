package com.music.mavimusic.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.bytedeco.javacpp.Loader
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit

internal data class DesktopExportInfo(val tags: Map<String, String>, val hasAudio: Boolean, val hasCover: Boolean, val duration: Double?)

/** Validate the file that will be published, rather than just trusting FFmpeg's exit code. */
internal object DesktopExportProbe {
    suspend fun inspect(file: File): DesktopExportInfo = withContext(Dispatchers.IO) {
        val log = Files.createTempFile("mavimusic-export-check-", ".json")
        var process: Process? = null
        try {
            process = ProcessBuilder(Loader.load(org.bytedeco.ffmpeg.ffprobe::class.java), "-v", "error",
                "-show_entries", "format=duration:format_tags=title,artist,album,track:stream=codec_type,codec_name:stream_disposition=attached_pic",
                "-of", "json", file.absolutePath).redirectErrorStream(true).redirectOutput(log.toFile()).start()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
            while (!process.waitFor(100, TimeUnit.MILLISECONDS)) {
                currentCoroutineContext().ensureActive()
                check(System.nanoTime() < deadline) { "La comprobación del MP3 tardó demasiado" }
                delay(10)
            }
            check(process.exitValue() == 0) { "El archivo de salida no es audio válido; se conserva el original" }
            val json = Json.parseToJsonElement(Files.readString(log)).jsonObject
            val streams = json["streams"]?.jsonArray.orEmpty().map { it.jsonObject }
            val format = json["format"]?.jsonObject
            DesktopExportInfo(
                tags = format?.get("tags")?.jsonObject.orEmpty().mapValues { it.value.jsonPrimitive.content },
                hasAudio = streams.any { it["codec_type"]?.jsonPrimitive?.content == "audio" },
                hasCover = streams.any { it["disposition"]?.jsonObject?.get("attached_pic")?.jsonPrimitive?.intOrNull == 1 },
                duration = format?.get("duration")?.jsonPrimitive?.content?.toDoubleOrNull(),
            )
        } finally {
            process?.takeIf { it.isAlive }?.destroyForcibly()?.waitFor(5, TimeUnit.SECONDS)
            Files.deleteIfExists(log)
        }
    }
}
