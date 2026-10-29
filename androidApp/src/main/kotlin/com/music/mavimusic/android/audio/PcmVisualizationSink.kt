package com.music.mavimusic.android.audio

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AndroidAudioVisualization {
    @Volatile var enabled = false
    @Volatile var reducedMotion = false
    private val _frame = MutableStateFlow<AudioVisualizationFrame?>(null)
    val frame = _frame.asStateFlow()
    internal fun publish(value: AudioVisualizationFrame) { _frame.value = value }
    internal fun reset() { _frame.value = null }
}

/** Observes a read-only PCM copy. It never modifies audio or captures microphone input. */
@androidx.annotation.OptIn(UnstableApi::class)
internal class PcmVisualizationSink(
    scope: CoroutineScope,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val onFrame: (AudioVisualizationFrame) -> Unit = AndroidAudioVisualization::publish,
) : TeeAudioProcessor.AudioBufferSink {
    private data class Window(val samples: FloatArray, val sampleRate: Int)
    private val pending = Channel<Window>(Channel.CONFLATED)
    private var channels = 0
    private var sampleRate = 0
    private var encoding = 0
    private val samples = FloatArray(1024)
    private var filled = 0
    private var lastPublishedNs = 0L

    init {
        scope.launch(dispatcher) {
            val analyzer = AudioSpectrumAnalyzer()
            for (window in pending) {
                if (AndroidAudioVisualization.enabled) {
                    val frame = analyzer.analyze(window.samples, window.sampleRate)
                    if (AndroidAudioVisualization.enabled) onFrame(frame)
                }
            }
        }
    }

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        sampleRate = sampleRateHz
        channels = channelCount
        this.encoding = encoding
        filled = 0
        lastPublishedNs = 0L
        AndroidAudioVisualization.reset()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        if (!AndroidAudioVisualization.enabled || channels <= 0 || sampleRate <= 0) {
            filled = 0
            return
        }
        val bytesPerSample = when (encoding) {
            C.ENCODING_PCM_16BIT -> 2
            C.ENCODING_PCM_FLOAT -> 4
            else -> return
        }
        val input = buffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val frameBytes = bytesPerSample * channels
        while (input.remaining() >= frameBytes) {
            val next = input.position() + frameBytes
            // First channel preserves a real waveform even for opposite-phase stereo.
            samples[filled++] = if (bytesPerSample == 2) input.short / 32768f else input.float
            input.position(next)
            if (filled == samples.size) {
                val now = System.nanoTime()
                val interval = if (AndroidAudioVisualization.reducedMotion) 100_000_000L else 33_333_333L
                if (now - lastPublishedNs >= interval) {
                    pending.trySend(Window(samples.copyOf(), sampleRate))
                    lastPublishedNs = now
                }
                filled = 0
            }
        }
    }
}
