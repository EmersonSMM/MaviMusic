package com.music.mavimusic.android.data

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** Validates every range before a temporary file may be promoted to an offline song. */
internal class VerifiedAudioDownloader(
    private val client: OkHttpClient,
    private val chunkSize: Long = 2 * 1024 * 1024L,
) {
    private data class Range(val start: Long, val end: Long, val total: Long)

    suspend fun download(url: String, headers: Map<String, String>, destination: File, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        require(chunkSize in 1..Int.MAX_VALUE.toLong())
        destination.outputStream().use { } // Truncate incomplete data from an earlier attempt.
        fun request(start: Long? = null, end: Long? = null): Request = Request.Builder().url(url).apply {
            headers.forEach { (key, value) -> header(key, value) }
            header("Accept-Encoding", "identity")
            if (start != null) header("Range", "bytes=$start-$end") else removeHeader("Range")
        }.build()

        val context = currentCoroutineContext()
        val total = fetch(request(0, 1)) { response ->
            verifyContentType(response)
            when (response.code) {
                200 -> {
                    // A server ignoring Range already returned the entire resource.
                    val expected = response.body.contentLength()
                    var received = 0L
                    response.body.byteStream().use { input ->
                        destination.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                context.ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                received += count
                                progress(if (expected > 0) (received.toFloat() / expected).coerceAtMost(1f) else -1f)
                            }
                        }
                    }
                    if (received == 0L || (expected >= 0 && received != expected)) throw IOException("Descarga incompleta")
                    null
                }
                206 -> {
                    val range = parseRange(response)
                    if (range.start != 0L || range.end != minOf(1, range.total - 1)) throw IOException("Rango inicial incorrecto")
                    readExactly(response, range.end + 1)
                    range.total
                }
                else -> throw IOException("HTTP ${response.code}")
            }
        }
        if (total != null) {
            var received = 0L
            RandomAccessFile(destination, "rw").use { output ->
                var offset = 0L
                while (offset < total) {
                    context.ensureActive()
                    val batch = buildList {
                        repeat(3) {
                            if (offset < total) {
                                val end = minOf(offset + chunkSize - 1, total - 1)
                                add(offset to end)
                                offset = end + 1
                            }
                        }
                    }
                    val blocks = coroutineScope {
                        batch.map { (start, end) -> async {
                            fetch(request(start, end)) { response ->
                                verifyContentType(response)
                                if (response.code != 206) throw IOException("El servidor no respetó el rango solicitado")
                                if (parseRange(response) != Range(start, end, total)) throw IOException("Rango de descarga incorrecto")
                                start to readExactly(response, end - start + 1)
                            }
                        } }.awaitAll()
                    }
                    for ((start, bytes) in blocks) {
                        context.ensureActive()
                        output.seek(start)
                        output.write(bytes)
                        received += bytes.size
                        progress(received.toFloat() / total)
                    }
                }
            }
            if (destination.length() != total || received != total) throw IOException("Descarga incompleta")
        }
        context.ensureActive()
        progress(1f)
    }

    private fun parseRange(response: Response): Range {
        val match = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(response.header("Content-Range") ?: "")
            ?: throw IOException("Content-Range ausente o inválido")
        val (start, end, total) = match.destructured
        val range = Range(start.toLongOrNull() ?: -1, end.toLongOrNull() ?: -1, total.toLongOrNull() ?: -1)
        if (range.start < 0 || range.end < range.start || range.end >= range.total) throw IOException("Content-Range inválido")
        return range
    }

    private fun readExactly(response: Response, count: Long): ByteArray {
        if (count <= 0 || count > Int.MAX_VALUE) throw IOException("Bloque inválido")
        val bytes = ByteArray(count.toInt())
        response.body.byteStream().use { input ->
            var offset = 0
            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read < 0) throw IOException("Bloque incompleto")
                offset += read
            }
            if (input.read() != -1) throw IOException("El bloque supera el rango solicitado")
        }
        return bytes
    }

    private fun verifyContentType(response: Response) {
        val type = response.header("Content-Type").orEmpty().lowercase()
        if (type.startsWith("text/") || type.contains("json") || type.contains("xml")) throw IOException("La respuesta no contiene audio")
    }

    /** Cancelling the coroutine also interrupts a blocked HTTP read. */
    private suspend fun <T> fetch(request: Request, read: (Response) -> T): T = coroutineScope {
        currentCoroutineContext().ensureActive()
        val call = client.newCall(request)
        val cancellation = launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { call.cancel() }
        }
        try {
            call.execute().use(read)
        } catch (error: IOException) {
            // Socket cancellation must remain coroutine cancellation, not a failed download.
            currentCoroutineContext().ensureActive()
            throw error
        } finally { cancellation.cancel() }
    }
}
