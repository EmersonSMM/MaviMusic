package com.music.mavimusic.desktop

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class DesktopFileTransferTest {
    @Test fun `bounded parallel reads reconstruct every byte and resume an interrupted file`() = runBlocking {
        val bytes = ByteArray(DesktopFileTransfer.BLOCK_BYTES * 5 + 791) { (it * 31).toByte() }
        val active = AtomicInteger()
        val maximum = AtomicInteger()
        val executor = Executors.newCachedThreadPool()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = executor
        server.createContext("/audio") { exchange ->
            val inFlight = active.incrementAndGet()
            maximum.updateAndGet { maxOf(it, inFlight) }
            try {
                val range = exchange.requestHeaders.getFirst("Range").removePrefix("bytes=").split('-')
                val start = range[0].toInt()
                val end = minOf(range[1].toInt(), bytes.lastIndex)
                exchange.responseHeaders.add("Content-Range", "bytes $start-$end/${bytes.size}")
                exchange.sendResponseHeaders(206, (end - start + 1).toLong())
                Thread.sleep(30)
                exchange.responseBody.use { it.write(bytes, start, end - start + 1) }
            } finally { active.decrementAndGet(); exchange.close() }
        }
        server.start()
        val file = Files.createTempFile("mavimusic-range-test", ".part")
        try {
            val offset = 12345
            Files.write(file, bytes.copyOfRange(0, offset))
            val progress = mutableListOf<Long>()
            DesktopFileTransfer().save(DesktopStream("http://127.0.0.1:${server.address.port}/audio", windowedReads = true), file, "test") { written, total ->
                assertEquals(bytes.size.toLong(), total)
                progress += written
            }
            assertContentEquals(bytes, Files.readAllBytes(file))
            assertTrue(maximum.get() > 1, "The remaining ranges should overlap in flight")
            assertEquals(bytes.size.toLong(), progress.last())
            assertTrue(progress.zipWithNext().all { (a, b) -> b >= a })
        } finally { Files.deleteIfExists(file); server.stop(0); executor.shutdownNow() }
    }

    @Test fun `a server returning the wrong range is rejected without modifying the partial file`() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/bad") { exchange ->
            exchange.responseHeaders.add("Content-Range", "bytes 0-2/3")
            exchange.sendResponseHeaders(206, 3)
            exchange.responseBody.use { it.write(byteArrayOf(1, 2, 3)) }
            exchange.close()
        }
        server.start()
        val file = Files.createTempFile("mavimusic-range-test", ".part")
        Files.write(file, byteArrayOf(9))
        try {
            assertFailsWith<IllegalStateException> {
                DesktopFileTransfer().save(DesktopStream("http://127.0.0.1:${server.address.port}/bad", windowedReads = true), file, "test") { _, _ -> }
            }
            assertContentEquals(byteArrayOf(9), Files.readAllBytes(file))
        } finally { Files.deleteIfExists(file); server.stop(0) }
    }

    @Test fun `a non range server restarts instead of appending a second full copy`() = runBlocking {
        val bytes = "complete-file".toByteArray()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file") { exchange ->
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.start()
        val file = Files.createTempFile("mavimusic-range-test", ".part")
        Files.write(file, "old-partial".toByteArray())
        try {
            DesktopFileTransfer().save(DesktopStream("http://127.0.0.1:${server.address.port}/file"), file, "test") { _, _ -> }
            assertContentEquals(bytes, Files.readAllBytes(file))
        } finally { Files.deleteIfExists(file); server.stop(0) }
    }
}
