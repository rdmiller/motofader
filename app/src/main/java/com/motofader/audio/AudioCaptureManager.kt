package com.motofader.audio

import android.media.audiofx.Visualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.ArrayDeque
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

enum class SpectrumSpeed(val label: String, val smoothingFactor: Float) {
    INST("INST", 1.0f),   // No smoothing — raw frame
    FAST("FAST", 0.6f),   // Light smoothing
    SLOW("SLOW", 0.15f),  // Heavy smoothing
}

class AudioCaptureManager {

    private var visualizer: Visualizer? = null

    private val _rmsLevel = MutableStateFlow(-60f)
    val rmsLevel: StateFlow<Float> = _rmsLevel

    private val _peakLevel = MutableStateFlow(-60f)
    val peakLevel: StateFlow<Float> = _peakLevel

    private val _spectrumBands = MutableStateFlow(FloatArray(BAND_COUNT) { -60f })
    val spectrumBands: StateFlow<FloatArray> = _spectrumBands

    private val _spectrumPeaks = MutableStateFlow(FloatArray(BAND_COUNT) { -60f })
    val spectrumPeaks: StateFlow<FloatArray> = _spectrumPeaks

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive

    private val _speed = MutableStateFlow(SpectrumSpeed.FAST)
    val speed: StateFlow<SpectrumSpeed> = _speed

    fun setSpeed(speed: SpectrumSpeed) { _speed.value = speed }

    // Pro audio statistics
    private val _peakHoldLevel = MutableStateFlow(-60f)
    val peakHoldLevel: StateFlow<Float> = _peakHoldLevel

    private val _leq = MutableStateFlow(-60f)
    val leq: StateFlow<Float> = _leq

    private val _crestFactor = MutableStateFlow(0f)
    val crestFactor: StateFlow<Float> = _crestFactor

    private val _momentaryLevel = MutableStateFlow(-60f)
    val momentaryLevel: StateFlow<Float> = _momentaryLevel

    private val _shortTermLevel = MutableStateFlow(-60f)
    val shortTermLevel: StateFlow<Float> = _shortTermLevel

    private val _measurementElapsed = MutableStateFlow(0L)
    val measurementElapsed: StateFlow<Long> = _measurementElapsed

    // Lock for shared mutable state accessed from Visualizer binder thread + main thread
    private val lock = Any()

    // Ring buffer for windowed calculations (guarded by lock)
    private data class FrameEnergy(val timestamp: Long, val sumSquares: Double, val sampleCount: Int)
    private val frameBuffer = ArrayDeque<FrameEnergy>(MAX_BUFFER_FRAMES)

    // Leq accumulators (guarded by lock)
    @Volatile private var leqTotalEnergy = 0.0
    @Volatile private var leqTotalSamples = 0L
    @Volatile private var leqStartTime = 0L

    fun resetStats() {
        _peakHoldLevel.value = -60f
        synchronized(lock) {
            leqTotalEnergy = 0.0
            leqTotalSamples = 0L
            leqStartTime = System.currentTimeMillis()
            frameBuffer.clear()
        }
        _leq.value = -60f
        _measurementElapsed.value = 0L
    }

    // FFT state (guarded by lock)
    private var peakHoldTimestamps = LongArray(BAND_COUNT) { 0L }
    private var peakDecayValues = FloatArray(BAND_COUNT) { -60f }
    private var smoothedBands = FloatArray(BAND_COUNT) { -60f }

    fun start(): Boolean {
        return try {
            val viz = Visualizer(0)
            val sizeRange = Visualizer.getCaptureSizeRange()
            viz.captureSize = sizeRange[1] // max (typically 1024)
            viz.scalingMode = Visualizer.SCALING_MODE_AS_PLAYED
            viz.setDataCaptureListener(
                object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer,
                        waveform: ByteArray,
                        samplingRate: Int
                    ) {
                        processWaveform(waveform)
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer,
                        fft: ByteArray,
                        samplingRate: Int
                    ) {
                        processFFT(fft, samplingRate)
                    }
                },
                Visualizer.getMaxCaptureRate(),
                true,
                true
            )
            viz.enabled = true
            visualizer = viz
            _isActive.value = true
            true
        } catch (e: Exception) {
            _isActive.value = false
            false
        }
    }

    fun stop() {
        visualizer?.let {
            it.enabled = false
            it.release()
        }
        visualizer = null
        _isActive.value = false
    }

    private fun processWaveform(waveform: ByteArray) {
        val now = System.currentTimeMillis()

        // Remove DC offset by subtracting the mean
        var dcSum = 0.0
        for (b in waveform) {
            dcSum += (b.toInt() and 0xFF).toDouble()
        }
        val dcOffset = dcSum / waveform.size

        var sumSquares = 0.0
        var peak = 0.0
        for (b in waveform) {
            val sample = ((b.toInt() and 0xFF) - dcOffset) / 128.0
            sumSquares += sample * sample
            peak = max(peak, abs(sample))
        }
        val rms = sqrt(sumSquares / waveform.size)

        // Noise gate: below ~-50dB is silence (0.003 linear ≈ -50.5dB)
        val rmsDb = if (rms > 0.003) (20.0 * log10(rms)).toFloat().coerceIn(-60f, 0f) else -60f
        val peakDb = if (peak > 0.003) (20.0 * log10(peak)).toFloat().coerceIn(-60f, 0f) else -60f

        // VU ballistics: instant attack, smooth release
        _rmsLevel.value = if (rmsDb > _rmsLevel.value) {
            rmsDb
        } else {
            max(rmsDb, _rmsLevel.value - VU_RELEASE_RATE)
        }
        _peakLevel.value = if (peakDb > _peakLevel.value) {
            peakDb
        } else {
            max(peakDb, _peakLevel.value - VU_RELEASE_RATE)
        }

        // --- Pro audio statistics ---

        // Peak hold (absolute max since reset)
        if (peakDb > _peakHoldLevel.value) {
            _peakHoldLevel.value = peakDb
        }

        // Crest factor (instantaneous peak / RMS)
        _crestFactor.value = if (rmsDb > -59f) (peakDb - rmsDb) else 0f

        synchronized(lock) {
            if (leqStartTime == 0L) leqStartTime = now

            // Leq (equivalent continuous level — running energy average)
            leqTotalEnergy += sumSquares
            leqTotalSamples += waveform.size
            if (leqTotalSamples > 0 && leqTotalEnergy > 0.0) {
                val avgEnergy = leqTotalEnergy / leqTotalSamples
                _leq.value = if (avgEnergy > 1e-6) (10.0 * log10(avgEnergy)).toFloat().coerceIn(-60f, 0f) else -60f
            }
            _measurementElapsed.value = now - leqStartTime

            // Frame buffer for windowed calculations
            frameBuffer.addLast(FrameEnergy(now, sumSquares, waveform.size))
            while (frameBuffer.size > MAX_BUFFER_FRAMES) {
                frameBuffer.removeFirst()
            }

            // Momentary (400ms window)
            _momentaryLevel.value = computeWindowedLevel(now, MOMENTARY_WINDOW_MS)

            // Short-term (3s window)
            _shortTermLevel.value = computeWindowedLevel(now, SHORT_TERM_WINDOW_MS)
        }
    }

    // Must be called under lock
    private fun computeWindowedLevel(now: Long, windowMs: Long): Float {
        val cutoff = now - windowMs
        var energy = 0.0
        var samples = 0L
        val iter = frameBuffer.descendingIterator()
        while (iter.hasNext()) {
            val frame = iter.next()
            if (frame.timestamp < cutoff) break
            energy += frame.sumSquares
            samples += frame.sampleCount
        }
        if (samples == 0L) return -60f
        val avgEnergy = energy / samples
        return if (avgEnergy > 1e-6) (10.0 * log10(avgEnergy)).toFloat().coerceIn(-60f, 0f) else -60f
    }

    private fun processFFT(fft: ByteArray, samplingRateMilliHz: Int) {
        val captureSize = fft.size
        val sampleRate = samplingRateMilliHz / 1000.0
        val binCount = captureSize / 2
        val binWidth = sampleRate / captureSize

        val magnitudes = FloatArray(binCount)
        // DC
        magnitudes[0] = abs(fft[0].toFloat())
        // Nyquist
        magnitudes[binCount - 1] = abs(fft[1].toFloat())
        // Remaining bins
        for (k in 1 until binCount - 1) {
            val real = fft[2 * k].toFloat()
            val imag = fft[2 * k + 1].toFloat()
            magnitudes[k] = sqrt(real * real + imag * imag)
        }

        val now = System.currentTimeMillis()

        synchronized(lock) {
            val bands = FloatArray(BAND_COUNT)
            val peaks = peakDecayValues.copyOf()

            for (i in 0 until BAND_COUNT) {
                val fc = BAND_FREQUENCIES[i]
                val lowerEdge = fc * TWO_POW_NEG_SIXTH
                val upperEdge = fc * TWO_POW_POS_SIXTH

                val lowerBin = (lowerEdge / binWidth).toInt().coerceIn(0, binCount - 1)
                val upperBin = (upperEdge / binWidth).toInt().coerceIn(0, binCount - 1)

                var sumMag = 0f
                var count = 0
                for (bin in lowerBin..upperBin) {
                    sumMag += magnitudes[bin] * magnitudes[bin]
                    count++
                }

                val avgMag = if (count > 0) sqrt(sumMag / count) else 0f
                // Noise gate: 8-bit FFT quantization floor is ~magnitude 1-2
                val db = if (avgMag > 1.5f) (20.0 * log10(avgMag.toDouble() / 128.0)).toFloat() else -60f
                bands[i] = db.coerceIn(-60f, 0f)

                // Peak hold with decay
                if (bands[i] > peaks[i]) {
                    peaks[i] = bands[i]
                    peakHoldTimestamps[i] = now
                } else if (now - peakHoldTimestamps[i] > PEAK_HOLD_MS) {
                    peaks[i] = (peaks[i] - PEAK_DECAY_RATE).coerceAtLeast(bands[i])
                }
            }

            // Exponential smoothing based on selected speed
            val alpha = _speed.value.smoothingFactor
            for (i in 0 until BAND_COUNT) {
                smoothedBands[i] = smoothedBands[i] + alpha * (bands[i] - smoothedBands[i])
            }

            peakDecayValues = peaks
            _spectrumBands.value = smoothedBands.copyOf()
            _spectrumPeaks.value = peaks.copyOf()
        }
    }

    companion object {
        const val BAND_COUNT = 31

        // ISO 1/3 octave center frequencies (Hz)
        val BAND_FREQUENCIES = floatArrayOf(
            20f, 25f, 31.5f, 40f, 50f, 63f, 80f, 100f, 125f, 160f,
            200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f,
            2000f, 2500f, 3150f, 4000f, 5000f, 6300f, 8000f, 10000f, 12500f, 16000f,
            20000f
        )

        // Labels for x-axis (sparse)
        val BAND_LABELS = arrayOf(
            "20", "", "", "", "50", "", "", "100", "", "",
            "200", "", "", "", "500", "", "", "1k", "", "",
            "2k", "", "", "", "5k", "", "", "10k", "", "",
            "20k"
        )

        private val TWO_POW_NEG_SIXTH = 2.0.pow(-1.0 / 6.0).toFloat()
        private val TWO_POW_POS_SIXTH = 2.0.pow(1.0 / 6.0).toFloat()
        private const val PEAK_HOLD_MS = 1500L
        private const val PEAK_DECAY_RATE = 0.8f
        private const val VU_RELEASE_RATE = 1.5f // dB per frame (~20fps), smooth fall-off
        private const val MOMENTARY_WINDOW_MS = 400L
        private const val SHORT_TERM_WINDOW_MS = 3000L
        private const val MAX_BUFFER_FRAMES = 128 // ~6.4s at 20fps, covers short-term window
    }
}
