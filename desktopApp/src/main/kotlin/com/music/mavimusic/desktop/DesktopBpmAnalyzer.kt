package com.music.mavimusic.desktop

import com.music.mavimusic.data.model.Song
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Source of the detected BPM. */
enum class BpmSource {
    METADATA_TAG,
    DSP_ANALYSIS,
    ESTIMATED,
}

/** Result of a tempo calculation with reliability scoring. */
data class BpmResult(
    val bpm: Float,
    val confidence: Float,
    val isReliable: Boolean,
    val source: BpmSource,
) {
    val displayString: String
        get() = if (isReliable && bpm > 0f) "${bpm.toInt()} BPM" else "BPM no fiable"
}

/** Stored record of an analyzed track. */
data class TrackBpmRecord(
    val trackId: String,
    val bpm: Float,
    val confidence: Float,
    val isReliable: Boolean,
    val source: BpmSource,
    val analyzedAt: Long = System.currentTimeMillis(),
)

/** Progress report of background library tempo scanning. */
data class BpmAnalysisProgress(
    val isRunning: Boolean = false,
    val analyzedCount: Int = 0,
    val totalCount: Int = 0,
    val reliableCount: Int = 0,
    val currentTitle: String = "",
)

/**
 * Onset detection and autocorrelation tempo detector.
 * Normalizes double/half-tempo ambiguities and computes confidence.
 */
object DesktopBpmDetector {

    const val MIN_BPM = 60
    const val MAX_BPM = 200
    const val RELIABILITY_THRESHOLD = 0.35f

    /** Normalizes BPM into the standard DJ range [70, 180] to prevent double/half tempo mistakes. */
    fun normalize(rawBpm: Float): Float {
        if (rawBpm <= 0f) return 0f
        var bpm = rawBpm
        while (bpm > 0f && bpm < 70f) {
            bpm *= 2f
        }
        while (bpm > 180f) {
            bpm /= 2f
        }
        return (Math.round(bpm * 10f) / 10f)
    }

    /**
     * Analyzes mono PCM audio samples to determine tempo and rhythmic periodicity.
     */
    fun detectFromPcm(samples: FloatArray, sampleRate: Int): BpmResult {
        if (samples.size < sampleRate * 3) {
            return BpmResult(0f, 0f, isReliable = false, BpmSource.DSP_ANALYSIS)
        }

        // Frame: 20 ms, hop: 10 ms (approx 100 Hz analysis rate)
        val hopSamples = (sampleRate * 0.01).toInt().coerceAtLeast(1)
        val windowSamples = (sampleRate * 0.02).toInt().coerceAtLeast(hopSamples)
        val frameCount = (samples.size - windowSamples) / hopSamples
        if (frameCount < 100) {
            return BpmResult(0f, 0f, isReliable = false, BpmSource.DSP_ANALYSIS)
        }

        // Step 1: RMS energy envelope
        val energy = FloatArray(frameCount)
        var sampleIdx = 0
        for (i in 0 until frameCount) {
            var sum = 0f
            for (w in 0 until windowSamples) {
                val s = samples[sampleIdx + w]
                sum += s * s
            }
            energy[i] = sqrt(sum / windowSamples)
            sampleIdx += hopSamples
        }

        // Step 2: Half-wave rectified first difference (spectral energy flux)
        val onsets = FloatArray(frameCount)
        for (i in 1 until frameCount) {
            val diff = energy[i] - energy[i - 1]
            onsets[i] = if (diff > 0f) diff else 0f
        }

        // Step 3: Autocorrelation over the tempo lag window
        val frameRate = sampleRate.toDouble() / hopSamples.toDouble()
        val minLag = (frameRate * 60.0 / MAX_BPM).toInt().coerceAtLeast(1)
        val maxLag = (frameRate * 60.0 / MIN_BPM).toInt().coerceAtMost(frameCount / 2)

        if (maxLag <= minLag) {
            return BpmResult(0f, 0f, isReliable = false, BpmSource.DSP_ANALYSIS)
        }

        var bestLag = minLag
        var maxCorr = 0.0
        var sumCorr = 0.0
        var countCorr = 0

        for (lag in minLag..maxLag) {
            var c = 0.0
            val limit = frameCount - lag
            for (n in 0 until limit) {
                c += onsets[n].toDouble() * onsets[n + lag].toDouble()
            }
            sumCorr += c
            countCorr++
            if (c > maxCorr) {
                maxCorr = c
                bestLag = lag
            }
        }

        val avgCorr = if (countCorr > 0) sumCorr / countCorr else 1.0
        val prominence = if (avgCorr > 0.0) ((maxCorr - avgCorr) / avgCorr).toFloat() else 0f
        val confidence = (prominence / 2.2f).coerceIn(0f, 1f)

        val rawBpm = (frameRate * 60.0 / bestLag).toFloat()
        val normalized = normalize(rawBpm)
        val isReliable = confidence >= RELIABILITY_THRESHOLD && normalized in 60f..200f

        return BpmResult(
            bpm = normalized,
            confidence = confidence,
            isReliable = isReliable,
            source = BpmSource.DSP_ANALYSIS,
        )
    }
}

/**
 * Persistent storage for BPM records.
 */
object DesktopBpmStore {
    private val persistence = DesktopPersistence()
    private const val KEY_RECORDS = "bpm_records_v1"
    private val records = ConcurrentHashMap<String, TrackBpmRecord>()
    private val initialized = AtomicBoolean(false)

    private fun ensureLoaded() {
        if (initialized.compareAndSet(false, true)) {
            val raw = persistence.string(KEY_RECORDS, "")
            if (raw.isNotBlank()) {
                raw.lineSequence().forEach { line ->
                    val parts = line.split("|")
                    if (parts.size >= 5) {
                        val id = parts[0]
                        val bpm = parts[1].toFloatOrNull() ?: 0f
                        val conf = parts[2].toFloatOrNull() ?: 0f
                        val reliable = parts[3].toBoolean()
                        val source = runCatching { BpmSource.valueOf(parts[4]) }.getOrDefault(BpmSource.DSP_ANALYSIS)
                        val at = parts.getOrNull(5)?.toLongOrNull() ?: 0L
                        records[id] = TrackBpmRecord(id, bpm, conf, reliable, source, at)
                    }
                }
            }
        }
    }

    fun get(trackId: String): TrackBpmRecord? {
        ensureLoaded()
        return records[trackId]
    }

    fun getReliableBpm(trackId: String): Float? {
        ensureLoaded()
        val rec = records[trackId]
        return if (rec != null && rec.isReliable && rec.bpm > 0f) rec.bpm else null
    }

    fun getAllReliable(): Map<String, Float> {
        ensureLoaded()
        return records.filterValues { it.isReliable && it.bpm > 0f }.mapValues { it.value.bpm }
    }

    fun save(record: TrackBpmRecord) {
        ensureLoaded()
        records[record.trackId] = record
        persist()
    }

    fun count(): Int {
        ensureLoaded()
        return records.size
    }

    fun reliableCount(): Int {
        ensureLoaded()
        return records.count { it.value.isReliable && it.value.bpm > 0f }
    }

    private fun persist() {
        val text = buildString {
            records.values.forEach { r ->
                append(r.trackId).append('|')
                    .append(r.bpm).append('|')
                    .append(r.confidence).append('|')
                    .append(r.isReliable).append('|')
                    .append(r.source.name).append('|')
                    .append(r.analyzedAt).append('\n')
            }
        }
        persistence.saveString(KEY_RECORDS, text)
    }
}

/**
 * Background analyzer manager with low priority, cancelability, and feature flag gating.
 */
object DesktopBpmAnalyzer {

    private val persistence = DesktopPersistence()
    private const val KEY_ENABLED = "bpm_analyzer_enabled"

    fun isEnabled(): Boolean = persistence.boolean(KEY_ENABLED, false)
    fun setEnabled(enabled: Boolean) {
        persistence.saveBoolean(KEY_ENABLED, enabled)
        if (!enabled) cancel()
    }

    private val _progress = MutableStateFlow(BpmAnalysisProgress())
    val progress: StateFlow<BpmAnalysisProgress> = _progress.asStateFlow()

    private var currentJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Scans and calculates BPM for a list of songs in the background with low CPU priority.
     * Skips tracks that already have an analysis unless [force] is true.
     */
    fun startAnalysis(songs: List<Song>, force: Boolean = false) {
        if (!isEnabled() && !force) return
        currentJob?.cancel()

        currentJob = scope.launch {
            val toAnalyze = if (force) songs else songs.filter { DesktopBpmStore.get(it.videoId) == null }
            val total = toAnalyze.size

            _progress.value = BpmAnalysisProgress(
                isRunning = true,
                analyzedCount = DesktopBpmStore.count(),
                totalCount = songs.size,
                reliableCount = DesktopBpmStore.reliableCount(),
            )

            for ((index, song) in toAnalyze.withIndex()) {
                if (!isActive) break

                _progress.value = _progress.value.copy(
                    currentTitle = song.title,
                    analyzedCount = DesktopBpmStore.count(),
                    reliableCount = DesktopBpmStore.reliableCount(),
                )

                analyzeSingleTrack(song)

                // Low priority yield between tracks
                delay(40)
            }

            _progress.value = BpmAnalysisProgress(
                isRunning = false,
                analyzedCount = DesktopBpmStore.count(),
                totalCount = songs.size,
                reliableCount = DesktopBpmStore.reliableCount(),
                currentTitle = "",
            )
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
        _progress.value = _progress.value.copy(isRunning = false, currentTitle = "")
    }

    /**
     * Analyzes a single song: checks metadata tags first, falls back to DSP audio onset detection.
     */
    fun analyzeSingleTrack(song: Song): BpmResult {
        // Step 1: Check existing record
        DesktopBpmStore.get(song.videoId)?.let {
            return BpmResult(it.bpm, it.confidence, it.isReliable, it.source)
        }

        // Step 2: Check ID3/container tags if it's a local track
        val localPathStr = song.localPath
        if (!localPathStr.isNullOrBlank()) {
            val path = Path.of(localPathStr)
            if (path.toFile().exists()) {
                val tags = DesktopLocalMetadata.read(path)
                val tagBpm = tags.bpm
                if (tagBpm != null && tagBpm > 0f) {
                    val normalized = DesktopBpmDetector.normalize(tagBpm)
                    val result = BpmResult(
                        bpm = normalized,
                        confidence = 0.95f,
                        isReliable = true,
                        source = BpmSource.METADATA_TAG,
                    )
                    DesktopBpmStore.save(
                        TrackBpmRecord(song.videoId, result.bpm, result.confidence, result.isReliable, result.source)
                    )
                    return result
                }

                // Step 3: Run DSP analysis on local file samples
                val pcmSamples = readPcmSlice(localPathStr, sampleRate = 22050, durationSec = 45)
                if (pcmSamples != null && pcmSamples.isNotEmpty()) {
                    val result = DesktopBpmDetector.detectFromPcm(pcmSamples, sampleRate = 22050)
                    DesktopBpmStore.save(
                        TrackBpmRecord(song.videoId, result.bpm, result.confidence, result.isReliable, result.source)
                    )
                    return result
                }
            }
        }

        // If no audio could be decoded
        val fallback = BpmResult(0f, 0f, isReliable = false, BpmSource.ESTIMATED)
        DesktopBpmStore.save(
            TrackBpmRecord(song.videoId, 0f, 0f, isReliable = false, BpmSource.ESTIMATED)
        )
        return fallback
    }

    /**
     * Reads a 45-second representative slice of audio as 22050 Hz mono float PCM.
     */
    private fun readPcmSlice(filePath: String, sampleRate: Int, durationSec: Int): FloatArray? = runCatching {
        val decoder = DesktopAudioDecoder()
        val format = DesktopPcmFormat(sampleRate = sampleRate, channels = 1, bytesPerSample = 4, isFloat = true)
        val opened = decoder.open(url = filePath, requested = format)
        if (opened.isFailure) {
            decoder.close()
            return null
        }

        try {
            val maxSamples = sampleRate * durationSec
            val samples = FloatArray(maxSamples)
            var totalRead = 0

            // Skip the first 10 seconds if possible to avoid ambient intros
            val skipCount = sampleRate * 10
            var skipped = 0
            while (skipped < skipCount) {
                val block = decoder.readSamples() ?: break
                skipped += decoder.sampleCount
            }

            while (totalRead < maxSamples) {
                val block = decoder.readSamples() ?: break
                val count = decoder.sampleCount
                if (count <= 0) continue
                val toCopy = count.coerceAtMost(maxSamples - totalRead)
                System.arraycopy(block, 0, samples, totalRead, toCopy)
                totalRead += toCopy
            }

            if (totalRead > sampleRate * 3) {
                samples.copyOf(totalRead)
            } else {
                null
            }
        } finally {
            decoder.close()
        }
    }.getOrNull()
}
