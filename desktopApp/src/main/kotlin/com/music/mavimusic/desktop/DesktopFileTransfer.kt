package com.music.mavimusic.desktop

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Resumable bounded reads for Google's media CDN, with at most three blocks in flight per track. */
internal class DesktopFileTransfer(private val client: OkHttpClient = DesktopDownloadHttp.client) {
    suspend fun save(stream: DesktopStream, target: Path, userAgent: String, progress: (Long, Long?) -> Unit) = withContext(Dispatchers.IO) {
        val offset = if (Files.exists(target)) Files.size(target) else 0L
        if (stream.windowedReads) ranged(stream, target, offset, userAgent, progress)
        else sequential(stream, target, offset, userAgent, progress)
    }

    private data class Block(val bytes: ByteArray, val total: Long)

    private suspend fun fetch(stream: DesktopStream, start: Long, end: Long, userAgent: String): Block {
        repeat(3) { attempt ->
            try {
                return suspendCancellableCoroutine { continuation ->
                    val call = client.newCall(DesktopDownloadHttp.request(stream.url, userAgent, stream.headers, start, end))
                    continuation.invokeOnCancellation { call.cancel() }
                    call.enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            if (continuation.isActive) continuation.resumeWithException(e)
                        }
                        override fun onResponse(call: Call, response: Response) {
                            val outcome = runCatching {
                                response.use {
                                    if (it.code == 416) {
                                        val total = it.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                                        check(total == start) { "The partial download no longer matches the remote audio" }
                                        return@runCatching Block(ByteArray(0), start)
                                    }
                                    check(it.code == 206) { "Audio server refused the requested block (HTTP ${it.code})" }
                                    val match = RANGE.matchEntire(it.header("Content-Range").orEmpty())
                                        ?: error("Audio server did not identify its byte range")
                                    val (from, to, size) = match.destructured
                                    val total = size.toLong()
                                    check(from.toLong() == start && to.toLong() == minOf(end, total - 1)) { "Audio server returned a different byte range" }
                                    val expected = to.toLong() - start + 1
                                    check(expected in 1..BLOCK_BYTES.toLong()) { "Invalid audio block length" }
                                    val bytes = it.body.byteStream().use { input -> input.readNBytes(expected.toInt() + 1) }
                                    check(bytes.size.toLong() == expected) { "Incomplete audio block; retry the download" }
                                    Block(bytes, total)
                                }
                            }
                            if (continuation.isActive) outcome.fold(continuation::resume, continuation::resumeWithException)
                        }
                    })
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (attempt == 2) throw failure
                delay(200L * (attempt + 1))
            }
        }
        error("Unreachable")
    }

    private suspend fun ranged(stream: DesktopStream, target: Path, offset: Long, userAgent: String, progress: (Long, Long?) -> Unit) {
        val first = fetch(stream, offset, offset + BLOCK_BYTES - 1, userAgent)
        var written = offset
        Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND).buffered(BLOCK_BYTES).use { output ->
            output.write(first.bytes)
            written += first.bytes.size
            progress(written, first.total)
            while (written < first.total) {
                currentCoroutineContext().ensureActive()
                val start = written
                val blocks = coroutineScope {
                    (0 until PARALLEL_BLOCKS).mapNotNull { index ->
                        val from = start + index.toLong() * BLOCK_BYTES
                        if (from >= first.total) null else async {
                            fetch(stream, from, minOf(from + BLOCK_BYTES - 1, first.total - 1), userAgent)
                        }
                    }.awaitAll()
                }
                for (block in blocks) {
                    check(block.total == first.total) { "The remote audio changed during download" }
                    output.write(block.bytes)
                    written += block.bytes.size
                    progress(written, first.total)
                }
            }
        }
        check(written == first.total && Files.size(target) == first.total) { "Incomplete audio download" }
    }

    private suspend fun sequential(stream: DesktopStream, target: Path, existing: Long, userAgent: String, progress: (Long, Long?) -> Unit) {
        val request = DesktopDownloadHttp.request(stream.url, userAgent, stream.headers, existing.takeIf { it > 0 })
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Download failed (HTTP ${response.code})" }
            val append = existing > 0 && response.code == 206
            val offset = if (append) existing else 0L
            val range = response.header("Content-Range")?.let { RANGE.matchEntire(it) }
            if (response.code == 206) check(range != null && range.groupValues[1].toLong() == offset) { "Invalid resume response" }
            val total = range?.groupValues?.get(3)?.toLong()
                ?: response.body.contentLength().takeIf { it >= 0 }?.plus(offset)
            Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                if (append) StandardOpenOption.APPEND else StandardOpenOption.TRUNCATE_EXISTING).buffered(BLOCK_BYTES).use { output ->
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(128 * 1024)
                    var written = offset
                    var reported = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        written += count
                        val now = System.nanoTime()
                        if (now - reported > 100_000_000L) { progress(written, total); reported = now }
                    }
                    check(total == null || written == total) { "Incomplete audio download ($written / $total bytes)" }
                    progress(written, total)
                }
            }
        }
    }

    companion object {
        internal const val BLOCK_BYTES = 512 * 1024
        private const val PARALLEL_BLOCKS = 3
        private val RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+)")
    }
}
