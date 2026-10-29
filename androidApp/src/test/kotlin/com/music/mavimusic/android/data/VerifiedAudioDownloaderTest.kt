package com.music.mavimusic.android.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VerifiedAudioDownloaderTest {
    @get:Rule val temporary = TemporaryFolder()
    private lateinit var server: MockWebServer
    private val client = OkHttpClient.Builder().readTimeout(20, TimeUnit.SECONDS).build()
    @Before fun start() { server = MockWebServer(); server.start() }
    @After fun stop() { server.shutdown() }

    private fun range(body: String, start: Int, end: Int) = MockResponse().setResponseCode(206)
        .setHeader("Content-Range", "bytes $start-$end/8").setHeader("Content-Type", "audio/webm").setBody(body)

    private fun dispatch(firstBlock: () -> MockResponse = { range("abcd", 0, 3) }) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when (request.getHeader("Range")) {
                "bytes=0-1" -> range("ab", 0, 1)
                "bytes=0-3" -> firstBlock()
                "bytes=4-7" -> range("efgh", 4, 7)
                else -> MockResponse().setResponseCode(400)
            }
        }
    }

    @Test fun `verified parallel ranges assemble exact bytes and finish progress`() = runBlocking {
        dispatch()
        val file = temporary.newFile()
        val progress = mutableListOf<Float>()
        VerifiedAudioDownloader(client, 4).download(server.url("/audio").toString(), emptyMap(), file, progress::add)
        assertEquals("abcdefgh", file.readText())
        assertEquals(1f, progress.last(), 0f)
    }

    @Test fun `a full 200 response is streamed once when server ignores probe range`() = runBlocking {
        server.enqueue(MockResponse().setBody("complete audio").setHeader("Content-Type", "audio/mp4"))
        val file = temporary.newFile()
        VerifiedAudioDownloader(client, 4).download(server.url("/audio").toString(), emptyMap(), file) { }
        assertEquals("complete audio", file.readText())
        assertEquals(1, server.requestCount)
    }

    @Test fun `a chunk ignoring its requested range is rejected`() = runBlocking {
        dispatch { MockResponse().setBody("abcdefgh") }
        val result = runCatching { VerifiedAudioDownloader(client, 4).download(server.url("/audio").toString(), emptyMap(), temporary.newFile()) { } }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test fun `a chunk with incorrect range coordinates is rejected`() = runBlocking {
        dispatch { range("abcd", 1, 4) }
        val result = runCatching { VerifiedAudioDownloader(client, 4).download(server.url("/audio").toString(), emptyMap(), temporary.newFile()) { } }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test fun `short chunk bodies cannot be accepted as a complete file`() = runBlocking {
        dispatch { range("ab", 0, 3) }
        val result = runCatching { VerifiedAudioDownloader(client, 4).download(server.url("/audio").toString(), emptyMap(), temporary.newFile()) { } }
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test fun `an HTML error returned as 200 is not saved as audio`() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Error</html>"))
        val file = temporary.newFile()
        val result = runCatching { VerifiedAudioDownloader(client).download(server.url("/audio").toString(), emptyMap(), file) { } }
        assertTrue(result.exceptionOrNull() is IOException)
        assertEquals(0L, file.length())
    }

    @Test fun `cancellation interrupts an HTTP call without waiting for read timeout`() = runBlocking {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val start = System.nanoTime()
        val result = runCatching {
            withTimeout(300) { VerifiedAudioDownloader(client).download(server.url("/audio").toString(), emptyMap(), temporary.newFile()) { } }
        }
        assertTrue(result.exceptionOrNull() is TimeoutCancellationException)
        assertTrue("Cancellation waited for HTTP timeout", TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 5)
    }
}
