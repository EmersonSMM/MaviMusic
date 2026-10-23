package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class DesktopDownloadSchedulerTest {
    private val song = Song("song", "Canción", "Artista", null)

    @Test fun `cancel and immediate retry wait for cleanup and ignore the old attempt's progress`() = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val firstStarted = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val staleProgress = CompletableDeferred<Unit>()
            val staleSent = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            val secondStarted = CompletableDeferred<DesktopDownloadItem>()
            val secondFinish = CompletableDeferred<Unit>()
            val persisted = CompletableDeferred<Song>()
            val calls = AtomicInteger()
            val queue = DesktopDownloadScheduler(scope, transfer = { item, progress ->
                if (calls.incrementAndGet() == 1) {
                    firstStarted.complete(Unit)
                    try { awaitCancellation() } finally {
                        withContext(NonCancellable) {
                            cleaning.complete(Unit); staleProgress.await()
                            progress(DesktopDownloadProgress(DesktopDownloadStage.VERIFYING))
                            staleSent.complete(Unit); releaseCleanup.await()
                        }
                    }
                } else {
                    secondStarted.complete(item); secondFinish.await()
                    Result.success(item.song)
                }
            }, completed = { persisted.complete(it) })
            try {
                queue.enqueue(song); firstStarted.await()
                queue.cancel(song.videoId); cleaning.await()
                val playlist = DesktopDownloadContext(DesktopDownloadContext.Kind.PLAYLIST, "Favoritas")
                queue.enqueue(song, playlist)
                val sequence = queue.active.value.getValue(song.videoId).sequence
                staleProgress.complete(Unit); staleSent.await()
                assertEquals(sequence, queue.active.value.getValue(song.videoId).sequence)
                assertEquals(DesktopDownloadState.Queued, queue.active.value.getValue(song.videoId).state)
                assertFalse(secondStarted.isCompleted, "The old FFmpeg/transfer cleanup still owns the temporary file")
                releaseCleanup.complete(Unit)
                assertEquals(playlist, secondStarted.await().context)
                secondFinish.complete(Unit)
                assertEquals(song, persisted.await())
                queue.active.first { it.isEmpty() }
                assertEquals(2, calls.get())
            } finally { releaseCleanup.complete(Unit); staleProgress.complete(Unit); scope.cancel() }
        }
    }

    @Test fun `retry preserves collection context and cannot restart a running download`() = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val started = CompletableDeferred<Unit>()
            val fail = CompletableDeferred<Unit>()
            val saved = CompletableDeferred<DesktopDownloadContext>()
            val calls = AtomicInteger()
            val context = DesktopDownloadContext(DesktopDownloadContext.Kind.ALBUM, "Álbum", "Artista")
            val queue = DesktopDownloadScheduler(scope, transfer = { item, _ ->
                if (calls.incrementAndGet() == 1) {
                    started.complete(Unit); fail.await(); Result.failure(IllegalStateException("falló la fuente"))
                } else { saved.complete(item.context); Result.success(item.song) }
            })
            try {
                queue.enqueue(song, context); started.await()
                val firstSequence = queue.active.value.getValue(song.videoId).sequence
                queue.retry(song.videoId)
                assertEquals(firstSequence, queue.active.value.getValue(song.videoId).sequence)
                fail.complete(Unit)
                queue.active.first { it[song.videoId]?.state is DesktopDownloadState.Failed }
                queue.retry(song.videoId)
                assertEquals(context, saved.await())
                queue.active.first { it.isEmpty() }
                assertEquals(2, calls.get())
            } finally { scope.cancel() }
        }
    }

    @Test fun `a failed library write is retryable and success does not need an event collector`() = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val writes = AtomicInteger()
            val persisted = CompletableDeferred<Song>()
            val queue = DesktopDownloadScheduler(scope, transfer = { item, _ -> Result.success(item.song) }, completed = {
                if (writes.incrementAndGet() == 1) error("No se pudo guardar el registro")
                persisted.complete(it)
            })
            try {
                queue.enqueue(song)
                val failed = queue.active.first { it[song.videoId]?.state is DesktopDownloadState.Failed }
                assertTrue((failed.getValue(song.videoId).state as DesktopDownloadState.Failed).reason.contains("guardar"))
                queue.retry(song.videoId)
                assertEquals(song, persisted.await())
                queue.active.first { it.isEmpty() }
                assertEquals(2, writes.get())
            } finally { scope.cancel() }
        }
    }

    @Test fun `four workers stay available when one track is cancelled`() = runBlocking {
        withTimeout(5_000) {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val starts = kotlinx.coroutines.channels.Channel<String>(10)
            val running = AtomicInteger()
            val maximum = AtomicInteger()
            val queue = DesktopDownloadScheduler(scope, transfer = { item, _ ->
                val count = running.incrementAndGet(); maximum.updateAndGet { maxOf(it, count) }
                try { starts.send(item.song.videoId); awaitCancellation() } finally { running.decrementAndGet() }
            })
            try {
                repeat(5) { queue.enqueue(song.copy(videoId = "track-$it")) }
                val initial = (1..4).map { starts.receive() }
                assertEquals(4, initial.distinct().size)
                queue.cancel(initial.first())
                val fifth = starts.receive()
                assertFalse(fifth in initial)
                assertEquals(4, queue.active.value.size)
                assertTrue(maximum.get() <= 4)
                queue.cancelAll()
                assertTrue(queue.active.value.isEmpty())
            } finally { scope.cancel() }
        }
    }
}
