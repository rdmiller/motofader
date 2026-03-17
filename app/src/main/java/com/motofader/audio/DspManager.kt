package com.motofader.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.os.Build
import com.motofader.model.CompressorBandSettings
import com.motofader.model.DEFAULT_EQ_BANDS
import com.motofader.model.DEFAULT_MBC_BANDS
import com.motofader.model.EqBandSettings
import com.motofader.model.ExtrasSettings
import com.motofader.model.FullBandCompressorSettings
import com.motofader.model.LimiterSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DspManager {

    // DynamicsProcessing stored as Any? to avoid class-load verification on API < 28
    private var dynamicsProcessing: Any? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _bypassed = MutableStateFlow(true)
    val bypassed: StateFlow<Boolean> = _bypassed

    private val _eqEnabled = MutableStateFlow(true)
    val eqEnabled: StateFlow<Boolean> = _eqEnabled

    private val _eqBands = MutableStateFlow(DEFAULT_EQ_BANDS)
    val eqBands: StateFlow<List<EqBandSettings>> = _eqBands

    private val _fullBand = MutableStateFlow(FullBandCompressorSettings())
    val fullBand: StateFlow<FullBandCompressorSettings> = _fullBand

    private val _mbcEnabled = MutableStateFlow(true)
    val mbcEnabled: StateFlow<Boolean> = _mbcEnabled

    private val _mbcBands = MutableStateFlow(DEFAULT_MBC_BANDS)
    val mbcBands: StateFlow<List<CompressorBandSettings>> = _mbcBands

    private val _limiter = MutableStateFlow(LimiterSettings())
    val limiter: StateFlow<LimiterSettings> = _limiter

    private val _extras = MutableStateFlow(ExtrasSettings())
    val extras: StateFlow<ExtrasSettings> = _extras

    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available

    fun start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            startDynamicsProcessing()
        }

        try {
            loudnessEnhancer = LoudnessEnhancer(0).also { it.enabled = false }
            bassBoost = BassBoost(0, 0).also { it.enabled = false }
            virtualizer = Virtualizer(0, 0).also { it.enabled = false }
        } catch (e: Exception) {
            // Non-critical extras
        }
    }

    private fun startDynamicsProcessing() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        try {
            val config = android.media.audiofx.DynamicsProcessing.Config.Builder(
                android.media.audiofx.DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                2,     // stereo
                true,  // pre-EQ
                EQ_BAND_COUNT,
                true,  // MBC
                MBC_BAND_COUNT,
                false, // post-EQ
                0,
                true   // limiter
            ).build()

            val dp = android.media.audiofx.DynamicsProcessing(0, 0, config)
            applyEq(dp)
            applyMbc(dp)
            applyLimiter(dp)
            dp.enabled = false // start bypassed
            dynamicsProcessing = dp
            _available.value = true
        } catch (e: Exception) {
            _available.value = false
        }
    }

    fun stop() {
        getDp()?.release()
        loudnessEnhancer?.release()
        bassBoost?.release()
        virtualizer?.release()
        dynamicsProcessing = null
        loudnessEnhancer = null
        bassBoost = null
        virtualizer = null
        _available.value = false
    }

    private fun getDp(): android.media.audiofx.DynamicsProcessing? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return dynamicsProcessing as? android.media.audiofx.DynamicsProcessing
    }

    fun setBypassed(bypassed: Boolean) {
        _bypassed.value = bypassed
        getDp()?.enabled = !bypassed
    }

    fun loadState(
        bypassed: Boolean,
        eqEnabled: Boolean,
        eqBands: List<EqBandSettings>,
        fullBand: FullBandCompressorSettings,
        mbcEnabled: Boolean,
        mbcBands: List<CompressorBandSettings>,
        limiter: LimiterSettings,
        extras: ExtrasSettings,
    ) {
        _bypassed.value = bypassed
        _eqEnabled.value = eqEnabled
        _eqBands.value = eqBands
        _fullBand.value = fullBand
        _mbcEnabled.value = mbcEnabled
        _mbcBands.value = mbcBands
        _limiter.value = limiter
        _extras.value = extras

        getDp()?.let { dp ->
            applyEq(dp)
            applyMbc(dp)
            applyLimiter(dp)
            dp.enabled = !bypassed
        }
        updateExtras(extras)
    }

    // --- EQ ---

    fun setEqEnabled(enabled: Boolean) {
        _eqEnabled.value = enabled
        getDp()?.let { applyEq(it) }
    }

    fun updateEqBand(index: Int, gain: Float) {
        val bands = _eqBands.value.toMutableList()
        bands[index] = bands[index].copy(gain = gain)
        _eqBands.value = bands
        getDp()?.let { dp ->
            val band = bands[index]
            dp.setPreEqBandAllChannelsTo(
                index,
                android.media.audiofx.DynamicsProcessing.EqBand(_eqEnabled.value, band.cutoffFrequency, band.gain)
            )
        }
    }

    // --- Full-band compressor ---

    fun updateFullBand(settings: FullBandCompressorSettings) {
        _fullBand.value = settings
        getDp()?.let { dp ->
            if (settings.enabled) {
                // Apply identical settings to all MBC bands
                val crossovers = listOf(250f, 1000f, 4000f, 20000f)
                for (i in 0 until MBC_BAND_COUNT) {
                    dp.setMbcBandAllChannelsTo(i, android.media.audiofx.DynamicsProcessing.MbcBand(
                        true,
                        crossovers[i],
                        settings.attackTime,
                        settings.releaseTime,
                        settings.ratio,
                        settings.threshold,
                        settings.kneeWidth,
                        NOISE_GATE_THRESHOLD,
                        EXPANDER_RATIO,
                        0f,
                        settings.makeupGain
                    ))
                }
            } else {
                // Restore individual MBC band settings
                applyMbc(dp)
            }
        }
    }

    // --- Multiband compressor ---

    fun setMbcEnabled(enabled: Boolean) {
        _mbcEnabled.value = enabled
        getDp()?.let { applyMbc(it) }
    }

    fun updateMbcBand(index: Int, settings: CompressorBandSettings) {
        val bands = _mbcBands.value.toMutableList()
        bands[index] = settings
        _mbcBands.value = bands
        // Only apply if full-band is not overriding
        if (!_fullBand.value.enabled) {
            getDp()?.let { dp ->
                dp.setMbcBandAllChannelsTo(index, buildMbcBand(settings))
            }
        }
    }

    // --- Limiter ---

    fun updateLimiter(settings: LimiterSettings) {
        _limiter.value = settings
        getDp()?.let { applyLimiter(it) }
    }

    // --- Extras ---

    fun updateExtras(settings: ExtrasSettings) {
        _extras.value = settings

        bassBoost?.let {
            it.setStrength(settings.bassBoostStrength.toShort())
            it.enabled = settings.bassBoostEnabled
        }

        virtualizer?.let {
            it.setStrength(settings.virtualizerStrength.toShort())
            it.enabled = settings.virtualizerEnabled
        }

        loudnessEnhancer?.let {
            it.setTargetGain(settings.loudnessGain)
            it.enabled = settings.loudnessEnabled
        }
    }

    // --- Internal apply methods ---

    private fun applyEq(dp: android.media.audiofx.DynamicsProcessing) {
        val enabled = _eqEnabled.value
        _eqBands.value.forEachIndexed { i, band ->
            dp.setPreEqBandAllChannelsTo(
                i,
                android.media.audiofx.DynamicsProcessing.EqBand(enabled, band.cutoffFrequency, band.gain)
            )
        }
    }

    private fun applyMbc(dp: android.media.audiofx.DynamicsProcessing) {
        val enabled = _mbcEnabled.value
        _mbcBands.value.forEachIndexed { i, band ->
            dp.setMbcBandAllChannelsTo(i, buildMbcBand(band.copy(enabled = enabled)))
        }
    }

    private fun applyLimiter(dp: android.media.audiofx.DynamicsProcessing) {
        val s = _limiter.value
        dp.setLimiterAllChannelsTo(
            android.media.audiofx.DynamicsProcessing.Limiter(
                true,        // inUse
                s.enabled,   // enabled
                0,           // linkGroup
                s.attackTime,
                s.releaseTime,
                s.ratio,
                s.threshold,
                s.postGain
            )
        )
    }

    private fun buildMbcBand(s: CompressorBandSettings): android.media.audiofx.DynamicsProcessing.MbcBand {
        return android.media.audiofx.DynamicsProcessing.MbcBand(
            s.enabled,
            s.cutoffFrequency,
            s.attackTime,
            s.releaseTime,
            s.ratio,
            s.threshold,
            s.kneeWidth,
            NOISE_GATE_THRESHOLD,
            EXPANDER_RATIO,
            s.preGain,
            s.postGain
        )
    }

    companion object {
        const val EQ_BAND_COUNT = 5
        const val MBC_BAND_COUNT = 4
        private const val NOISE_GATE_THRESHOLD = -90f
        private const val EXPANDER_RATIO = 1f
    }
}
