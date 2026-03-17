package com.motofader.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.motofader.audio.AudioCaptureManager
import com.motofader.audio.DspManager
import com.motofader.audio.DspPreferences
import com.motofader.audio.SpectrumSpeed
import com.motofader.audio.StreamVolumeManager
import com.motofader.model.AudioChannel
import com.motofader.model.CompressorBandSettings
import com.motofader.model.EqBandSettings
import com.motofader.model.ExtrasSettings
import com.motofader.model.FullBandCompressorSettings
import com.motofader.model.LimiterSettings
import kotlinx.coroutines.flow.StateFlow

class MixerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioCaptureManager = AudioCaptureManager()
    private val streamVolumeManager = StreamVolumeManager(application)
    private val dspManager = DspManager()
    private val dspPreferences = DspPreferences(application)

    val channels: StateFlow<List<AudioChannel>> = streamVolumeManager.channels

    val rmsLevel: StateFlow<Float> = audioCaptureManager.rmsLevel
    val peakLevel: StateFlow<Float> = audioCaptureManager.peakLevel
    val spectrumBands: StateFlow<FloatArray> = audioCaptureManager.spectrumBands
    val spectrumPeaks: StateFlow<FloatArray> = audioCaptureManager.spectrumPeaks
    val isVisualizerActive: StateFlow<Boolean> = audioCaptureManager.isActive
    val spectrumSpeed: StateFlow<SpectrumSpeed> = audioCaptureManager.speed

    // Pro audio statistics
    val peakHoldLevel: StateFlow<Float> = audioCaptureManager.peakHoldLevel
    val leq: StateFlow<Float> = audioCaptureManager.leq
    val crestFactor: StateFlow<Float> = audioCaptureManager.crestFactor
    val momentaryLevel: StateFlow<Float> = audioCaptureManager.momentaryLevel
    val shortTermLevel: StateFlow<Float> = audioCaptureManager.shortTermLevel
    val measurementElapsed: StateFlow<Long> = audioCaptureManager.measurementElapsed

    // DSP
    val dspAvailable: StateFlow<Boolean> = dspManager.available
    val dspBypassed: StateFlow<Boolean> = dspManager.bypassed
    val eqEnabled: StateFlow<Boolean> = dspManager.eqEnabled
    val eqBands: StateFlow<List<EqBandSettings>> = dspManager.eqBands
    val fullBandSettings: StateFlow<FullBandCompressorSettings> = dspManager.fullBand
    val mbcEnabled: StateFlow<Boolean> = dspManager.mbcEnabled
    val mbcBands: StateFlow<List<CompressorBandSettings>> = dspManager.mbcBands
    val limiterSettings: StateFlow<LimiterSettings> = dspManager.limiter
    val extrasSettings: StateFlow<ExtrasSettings> = dspManager.extras

    fun setSpectrumSpeed(speed: SpectrumSpeed) = audioCaptureManager.setSpeed(speed)
    fun resetStats() = audioCaptureManager.resetStats()

    fun startCapture(): Boolean = audioCaptureManager.start()
    fun stopCapture() = audioCaptureManager.stop()

    fun startVolumeMonitor() = streamVolumeManager.start()
    fun stopVolumeMonitor() = streamVolumeManager.stop()

    fun startDsp() {
        dspManager.start()
        // Restore saved settings if available
        if (dspPreferences.hasSavedState()) {
            dspManager.loadState(
                bypassed = dspPreferences.loadBypassed(),
                eqEnabled = dspPreferences.loadEqEnabled(),
                eqBands = dspPreferences.loadEqBands(),
                fullBand = dspPreferences.loadFullBand(),
                mbcEnabled = dspPreferences.loadMbcEnabled(),
                mbcBands = dspPreferences.loadMbcBands(),
                limiter = dspPreferences.loadLimiter(),
                extras = dspPreferences.loadExtras(),
            )
        }
    }

    fun stopDsp() = dspManager.stop()

    fun setVolume(streamType: Int, volume: Int) = streamVolumeManager.setVolume(streamType, volume)
    fun toggleMute(streamType: Int) = streamVolumeManager.toggleMute(streamType)
    fun refreshVolumes() = streamVolumeManager.refreshVolumes()

    // DSP controls (with persistence)
    fun setDspBypassed(bypassed: Boolean) {
        dspManager.setBypassed(bypassed)
        dspPreferences.saveBypassed(bypassed)
    }

    fun setEqEnabled(enabled: Boolean) {
        dspManager.setEqEnabled(enabled)
        dspPreferences.saveEqEnabled(enabled)
    }

    fun updateEqBand(index: Int, gain: Float) {
        dspManager.updateEqBand(index, gain)
        dspPreferences.saveEqBands(dspManager.eqBands.value)
    }

    fun updateFullBand(settings: FullBandCompressorSettings) {
        dspManager.updateFullBand(settings)
        dspPreferences.saveFullBand(settings)
    }

    fun setMbcEnabled(enabled: Boolean) {
        dspManager.setMbcEnabled(enabled)
        dspPreferences.saveMbcEnabled(enabled)
    }

    fun updateMbcBand(index: Int, settings: CompressorBandSettings) {
        dspManager.updateMbcBand(index, settings)
        dspPreferences.saveMbcBands(dspManager.mbcBands.value)
    }

    fun updateLimiter(settings: LimiterSettings) {
        dspManager.updateLimiter(settings)
        dspPreferences.saveLimiter(settings)
    }

    fun updateExtras(settings: ExtrasSettings) {
        dspManager.updateExtras(settings)
        dspPreferences.saveExtras(settings)
    }

    override fun onCleared() {
        super.onCleared()
        audioCaptureManager.stop()
        streamVolumeManager.stop()
        dspManager.stop()
    }
}
