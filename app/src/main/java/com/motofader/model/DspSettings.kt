package com.motofader.model

data class EqBandSettings(
    val label: String,
    val cutoffFrequency: Float,
    val gain: Float = 0f, // dB, -12 to +12
)

data class CompressorBandSettings(
    val label: String,
    val cutoffFrequency: Float,
    val enabled: Boolean = true,
    val threshold: Float = -20f,   // dB, -60 to 0
    val ratio: Float = 4f,         // x:1, 1 to 20
    val attackTime: Float = 3f,    // ms, 0.1 to 100
    val releaseTime: Float = 100f, // ms, 10 to 1000
    val kneeWidth: Float = 6f,    // dB, 0 to 30
    val preGain: Float = 0f,      // dB, -12 to +12
    val postGain: Float = 0f,     // dB, -12 to +12
)

data class LimiterSettings(
    val enabled: Boolean = true,
    val threshold: Float = -1f,   // dB, -60 to 0
    val ratio: Float = 10f,       // x:1, 1 to 50
    val attackTime: Float = 1f,   // ms, 0.1 to 50
    val releaseTime: Float = 50f, // ms, 10 to 500
    val postGain: Float = 0f,    // dB, -12 to +12
)

data class ExtrasSettings(
    val bassBoostEnabled: Boolean = false,
    val bassBoostStrength: Int = 500,    // 0-1000
    val virtualizerEnabled: Boolean = false,
    val virtualizerStrength: Int = 500,  // 0-1000
    val loudnessEnabled: Boolean = false,
    val loudnessGain: Int = 0,           // millibels, 0-3000
)

data class FullBandCompressorSettings(
    val enabled: Boolean = false,
    val threshold: Float = -20f,   // dB
    val ratio: Float = 4f,         // x:1
    val attackTime: Float = 3f,    // ms
    val releaseTime: Float = 100f, // ms
    val kneeWidth: Float = 6f,    // dB
    val makeupGain: Float = 0f,   // dB
)

val MBC_BAND_RANGES = listOf(
    "20\u2013250 Hz",
    "250\u20131k Hz",
    "1k\u20134k Hz",
    "4k\u201320k Hz",
)

val EQ_BAND_RANGES = listOf(
    "0\u201360 Hz",
    "60\u2013250 Hz",
    "250\u20131k Hz",
    "1k\u20134k Hz",
    "4k\u201320k Hz",
)

val DEFAULT_EQ_BANDS = listOf(
    EqBandSettings("SUB", 60f),
    EqBandSettings("LOW", 250f),
    EqBandSettings("MID", 1000f),
    EqBandSettings("PRES", 4000f),
    EqBandSettings("AIR", 20000f),
)

val DEFAULT_MBC_BANDS = listOf(
    CompressorBandSettings("LOW", 250f),
    CompressorBandSettings("LO-MID", 1000f),
    CompressorBandSettings("HI-MID", 4000f),
    CompressorBandSettings("HIGH", 20000f),
)
