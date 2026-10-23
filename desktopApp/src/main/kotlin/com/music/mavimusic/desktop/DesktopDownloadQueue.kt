package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

/** Where a track is between "not on this computer" and "on it". */
sealed interface DesktopDownloadState {

    /** Accepted, waiting for the one in front of it. */
    data object Queued : DesktopDownloadState

    /** [fraction] is 0f until the length is known, which is the first thing asked for. */
    data class Running(val fraction: Float, val progress: DesktopDownloadProgress = DesktopDownloadProgress(DesktopDownloadStage.TRANSFER, fraction)) : DesktopDownloadState

    data class Failed(val reason: String) : DesktopDownloadState
}

/** One row of the downloads manager. */
data class DesktopDownloadItem(
    val song: Song,
    val state: DesktopDownloadState,
    /** The order it was asked for in, which is the order the queue reaches it in. */
    val sequence: Long,
    val context: DesktopDownloadContext = DesktopDownloadContext(),
)

/** Production facade; each attempt is owned by its immutable sequence, not just its track ID. */
object DesktopDownloadQueue {
    internal const val WORKERS = 4
    private val scheduler = DesktopDownloadScheduler(
        CoroutineScope(SupervisorJob() + Dispatchers.IO),
        transfer = { item, progress -> DesktopDownloadManager.download(item.song, context = item.context, onState = progress) },
        completed = DesktopDownloadLibrary::add,
    )
    val active get() = scheduler.active
    val finished get() = scheduler.finished
    fun enqueue(song: Song, context: DesktopDownloadContext = DesktopDownloadContext()) = scheduler.enqueue(song, context)
    fun enqueueAll(songs: List<Song>, context: DesktopDownloadContext = DesktopDownloadContext()) = songs.forEach { enqueue(it, context) }
    fun cancel(videoId: String) = scheduler.cancel(videoId)
    fun cancelAll() = scheduler.cancelAll()
    fun clearFinished() = scheduler.clearFinished()
    fun retry(videoId: String) = scheduler.retry(videoId)
}

internal class DesktopDownloadScheduler(
    private val scope: CoroutineScope,
    private val transfer: suspend (DesktopDownloadItem, (DesktopDownloadProgress) -> Unit) -> Result<Song>,
    private val completed: (Song) -> Unit = {},
) {
    private data class Running(val item: DesktopDownloadItem, var job: Job? = null)
    private val lock = Any()
    private val sequence = AtomicLong()
    private val pending = LinkedHashMap<String, DesktopDownloadItem>()
    private val running = LinkedHashMap<String, Running>()
    private var workers = 0
    private val _active = MutableStateFlow<Map<String, DesktopDownloadItem>>(emptyMap())
    val active: StateFlow<Map<String, DesktopDownloadItem>> = _active
    private val _finished = MutableSharedFlow<Song>(extraBufferCapacity = 64)
    val finished: SharedFlow<Song> = _finished

    fun enqueue(song: Song, context: DesktopDownloadContext = DesktopDownloadContext()) {
        if (song.videoId.isBlank()) return
        synchronized(lock) {
            val id = song.videoId
            if (id in pending || _active.value[id]?.state?.let { it !is DesktopDownloadState.Failed } == true) return
            val item = DesktopDownloadItem(song, DesktopDownloadState.Queued, sequence.incrementAndGet(), context)
            pending[id] = item
            _active.value = _active.value + (id to item)
        }
        ensureWorkers()
    }

    fun cancel(id: String) {
        val job = synchronized(lock) {
            pending.remove(id)
            _active.value = _active.value - id
            // Retain ownership until cleanup finishes: a retry must not write to the same
            // temporary file while the cancelled transfer or FFmpeg process still has it open.
            running[id]?.job
        }
        job?.cancel()
    }
    fun cancelAll() = active.value.keys.toList().forEach(::cancel)
    fun clearFinished() = synchronized(lock) {
        _active.value = _active.value.filterValues { it.state !is DesktopDownloadState.Failed }
    }
    fun retry(id: String) {
        val item = synchronized(lock) { _active.value[id]?.takeIf { it.state is DesktopDownloadState.Failed } } ?: return
        enqueue(item.song, item.context)
    }
    private fun owns(item: DesktopDownloadItem) = _active.value[item.song.videoId]?.sequence == item.sequence
    private fun progress(item: DesktopDownloadItem, value: DesktopDownloadProgress) = synchronized(lock) {
        if (owns(item)) _active.value = _active.value + (item.song.videoId to item.copy(
            state = DesktopDownloadState.Running(value.fraction, value)))
    }
    private fun ensureWorkers() {
        val count = synchronized(lock) {
            val available = pending.keys.count { it !in running }
            val wanted = minOf(DesktopDownloadQueue.WORKERS, running.size + available)
            (wanted - workers).coerceAtLeast(0).also { workers += it }
        }
        repeat(count) { scope.launch { work() } }
    }
    private fun takeNext(): DesktopDownloadItem? = synchronized(lock) {
        val entry = pending.entries.firstOrNull { it.key !in running } ?: return null
        pending.remove(entry.key)
        running[entry.key] = Running(entry.value)
        entry.value
    }
    private suspend fun work() {
        try {
            while (true) {
                val item = takeNext() ?: return
                val id = item.song.videoId
                val result = kotlinx.coroutines.supervisorScope {
                    val task = async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
                        transfer(item) { progress(item, it) }
                    }
                    synchronized(lock) {
                        running[id]?.takeIf { it.item.sequence == item.sequence }?.job = task
                        if (!owns(item)) task.cancel()
                    }
                    progress(item, DesktopDownloadProgress(DesktopDownloadStage.SONG))
                    try { task.await() }
                    catch (cancelled: kotlinx.coroutines.CancellationException) {
                        kotlinx.coroutines.currentCoroutineContext().ensureActive()
                        Result.failure(cancelled)
                    } catch (failure: Exception) { Result.failure(failure) }
                }
                try {
                    if (result.isSuccess) {
                        val saved = result.getOrThrow()
                        val wanted = synchronized(lock) { owns(item) }
                        // Disk flushes run on the worker, outside the queue lock, so UI actions stay responsive.
                        // This file has already passed validation; a late cancel does not delete a finished file.
                        if (wanted) completed(saved)
                        val accepted = synchronized(lock) {
                            if (!wanted || !owns(item)) false else {
                                _active.value = _active.value - id
                                true
                            }
                        }
                        if (accepted) _finished.emit(saved)
                    } else failed(item, result.exceptionOrNull()!!)
                } catch (failure: Exception) {
                    if (failure is kotlinx.coroutines.CancellationException) throw failure
                    failed(item, failure)
                } finally {
                    synchronized(lock) { if (running[id]?.item?.sequence == item.sequence) running.remove(id) }
                }
            }
        } finally {
            synchronized(lock) { workers-- }
            if (scope.coroutineContext[Job]?.isActive != false) ensureWorkers()
        }
    }
    private fun failed(item: DesktopDownloadItem, cause: Throwable) = synchronized(lock) {
        if (cause is kotlinx.coroutines.CancellationException || !owns(item)) return@synchronized
        val reason = cause.message?.takeIf(String::isNotBlank) ?: "La descarga no terminó"
        _active.value = _active.value + (item.song.videoId to item.copy(state = DesktopDownloadState.Failed(reason)))
        DesktopTrackLog.log("download failed for '${item.song.title}' — $reason")
    }
}
