package com.motofader.audio

import android.content.Context
import android.content.SharedPreferences
import com.motofader.model.CompressorBandSettings
import com.motofader.model.DEFAULT_EQ_BANDS
import com.motofader.model.DEFAULT_MBC_BANDS
import com.motofader.model.EqBandSettings
import com.motofader.model.ExtrasSettings
import com.motofader.model.FullBandCompressorSettings
import com.motofader.model.LimiterSettings

class DspPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("dsp_settings", Context.MODE_PRIVATE)

    fun hasSavedState(): Boolean = prefs.contains("bypassed")

    fun saveBypassed(v: Boolean) = prefs.edit().putBoolean("bypassed", v).apply()
    fun loadBypassed(): Boolean = prefs.getBoolean("bypassed", true)

    fun saveEqEnabled(v: Boolean) = prefs.edit().putBoolean("eq_enabled", v).apply()
    fun loadEqEnabled(): Boolean = prefs.getBoolean("eq_enabled", true)

    fun saveEqBands(bands: List<EqBandSettings>) {
        prefs.edit().apply {
            bands.forEachIndexed { i, b ->
                putFloat("eq_${i}_gain", b.gain)
            }
            apply()
        }
    }

    fun loadEqBands(): List<EqBandSettings> {
        return DEFAULT_EQ_BANDS.mapIndexed { i, default ->
            default.copy(gain = prefs.getFloat("eq_${i}_gain", default.gain))
        }
    }

    fun saveFullBand(s: FullBandCompressorSettings) {
        prefs.edit()
            .putBoolean("fb_enabled", s.enabled)
            .putFloat("fb_threshold", s.threshold)
            .putFloat("fb_ratio", s.ratio)
            .putFloat("fb_attack", s.attackTime)
            .putFloat("fb_release", s.releaseTime)
            .putFloat("fb_knee", s.kneeWidth)
            .putFloat("fb_makeup", s.makeupGain)
            .apply()
    }

    fun loadFullBand(): FullBandCompressorSettings {
        return FullBandCompressorSettings(
            enabled = prefs.getBoolean("fb_enabled", false),
            threshold = prefs.getFloat("fb_threshold", -20f),
            ratio = prefs.getFloat("fb_ratio", 4f),
            attackTime = prefs.getFloat("fb_attack", 3f),
            releaseTime = prefs.getFloat("fb_release", 100f),
            kneeWidth = prefs.getFloat("fb_knee", 6f),
            makeupGain = prefs.getFloat("fb_makeup", 0f),
        )
    }

    fun saveMbcEnabled(v: Boolean) = prefs.edit().putBoolean("mbc_enabled", v).apply()
    fun loadMbcEnabled(): Boolean = prefs.getBoolean("mbc_enabled", true)

    fun saveMbcBands(bands: List<CompressorBandSettings>) {
        prefs.edit().apply {
            bands.forEachIndexed { i, b ->
                putFloat("mbc_${i}_threshold", b.threshold)
                putFloat("mbc_${i}_ratio", b.ratio)
                putFloat("mbc_${i}_attack", b.attackTime)
                putFloat("mbc_${i}_release", b.releaseTime)
                putFloat("mbc_${i}_knee", b.kneeWidth)
                putFloat("mbc_${i}_pre", b.preGain)
                putFloat("mbc_${i}_post", b.postGain)
            }
            apply()
        }
    }

    fun loadMbcBands(): List<CompressorBandSettings> {
        return DEFAULT_MBC_BANDS.mapIndexed { i, default ->
            default.copy(
                threshold = prefs.getFloat("mbc_${i}_threshold", default.threshold),
                ratio = prefs.getFloat("mbc_${i}_ratio", default.ratio),
                attackTime = prefs.getFloat("mbc_${i}_attack", default.attackTime),
                releaseTime = prefs.getFloat("mbc_${i}_release", default.releaseTime),
                kneeWidth = prefs.getFloat("mbc_${i}_knee", default.kneeWidth),
                preGain = prefs.getFloat("mbc_${i}_pre", default.preGain),
                postGain = prefs.getFloat("mbc_${i}_post", default.postGain),
            )
        }
    }

    fun saveLimiter(s: LimiterSettings) {
        prefs.edit()
            .putBoolean("lim_enabled", s.enabled)
            .putFloat("lim_threshold", s.threshold)
            .putFloat("lim_ratio", s.ratio)
            .putFloat("lim_attack", s.attackTime)
            .putFloat("lim_release", s.releaseTime)
            .putFloat("lim_gain", s.postGain)
            .apply()
    }

    fun loadLimiter(): LimiterSettings {
        return LimiterSettings(
            enabled = prefs.getBoolean("lim_enabled", true),
            threshold = prefs.getFloat("lim_threshold", -1f),
            ratio = prefs.getFloat("lim_ratio", 10f),
            attackTime = prefs.getFloat("lim_attack", 1f),
            releaseTime = prefs.getFloat("lim_release", 50f),
            postGain = prefs.getFloat("lim_gain", 0f),
        )
    }

    fun saveExtras(s: ExtrasSettings) {
        prefs.edit()
            .putBoolean("ext_bass_enabled", s.bassBoostEnabled)
            .putInt("ext_bass_strength", s.bassBoostStrength)
            .putBoolean("ext_virt_enabled", s.virtualizerEnabled)
            .putInt("ext_virt_strength", s.virtualizerStrength)
            .putBoolean("ext_loud_enabled", s.loudnessEnabled)
            .putInt("ext_loud_gain", s.loudnessGain)
            .apply()
    }

    fun loadExtras(): ExtrasSettings {
        return ExtrasSettings(
            bassBoostEnabled = prefs.getBoolean("ext_bass_enabled", false),
            bassBoostStrength = prefs.getInt("ext_bass_strength", 500),
            virtualizerEnabled = prefs.getBoolean("ext_virt_enabled", false),
            virtualizerStrength = prefs.getInt("ext_virt_strength", 500),
            loudnessEnabled = prefs.getBoolean("ext_loud_enabled", false),
            loudnessGain = prefs.getInt("ext_loud_gain", 0),
        )
    }
}
