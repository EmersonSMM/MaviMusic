package com.music.mavimusic.android.audio

import kotlin.math.*

data class AudioVisualizationFrame(
    val spectrum: List<Float>,
    val waveform: List<Float>,
    val rms: Float,
)

/** Hann-windowed FFT, logarithmic frequency bands and unmodified waveform samples. */
internal class AudioSpectrumAnalyzer(private val size: Int = 1024, private val bands: Int = 32) {
    init {
        require(size >= 128 && size and (size - 1) == 0)
        require(bands > 0)
    }

    private val window = FloatArray(size) { (0.5 - 0.5 * cos(2.0 * PI * it / (size - 1))).toFloat() }
    private val windowSum = window.sum()
    private val real = FloatArray(size)
    private val imaginary = FloatArray(size)
    private val cosTable = FloatArray(size / 2) { cos(-2.0 * PI * it / size).toFloat() }
    private val sinTable = FloatArray(size / 2) { sin(-2.0 * PI * it / size).toFloat() }

    fun analyze(samples: FloatArray, sampleRate: Int): AudioVisualizationFrame {
        require(samples.size == size)
        require(sampleRate > 0)
        val mean = samples.average().toFloat()
        var squareSum = 0.0
        for (i in 0 until size) {
            real[i] = (samples[i] - mean) * window[i]
            imaginary[i] = 0f
            squareSum += samples[i].toDouble() * samples[i]
        }
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) {
                val value = real[i]; real[i] = real[j]; real[j] = value
            }
        }
        var length = 2
        while (length <= size) {
            val half = length / 2
            val step = size / length
            for (start in 0 until size step length) {
                for (offset in 0 until half) {
                    val left = start + offset
                    val right = left + half
                    val twiddle = offset * step
                    val re = real[right] * cosTable[twiddle] - imaginary[right] * sinTable[twiddle]
                    val im = real[right] * sinTable[twiddle] + imaginary[right] * cosTable[twiddle]
                    real[right] = real[left] - re
                    imaginary[right] = imaginary[left] - im
                    real[left] += re
                    imaginary[left] += im
                }
            }
            length *= 2
        }
        val upper = min(20_000.0, sampleRate / 2.0)
        val lower = min(30.0, upper / 2)
        val spectrum = List(bands) { band ->
            val fromHz = lower * (upper / lower).pow(band.toDouble() / bands)
            val toHz = lower * (upper / lower).pow((band + 1.0) / bands)
            val from = (fromHz * size / sampleRate).toInt().coerceIn(1, size / 2 - 1)
            val to = ceil(toHz * size / sampleRate).toInt().coerceIn(from + 1, size / 2)
            var amplitude = 0f
            for (bin in from until to) {
                amplitude = max(amplitude, hypot(real[bin], imaginary[bin]) * 2 / windowSum)
            }
            if (amplitude < 0.00025f) 0f else ((20 * log10(amplitude) + 72) / 72).coerceIn(0f, 1f)
        }
        return AudioVisualizationFrame(
            spectrum,
            List(128) { samples[it * size / 128].coerceIn(-1f, 1f) },
            sqrt(squareSum / size).toFloat().coerceIn(0f, 1f),
        )
    }
}
