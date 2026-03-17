package com.motofader.model

import android.media.AudioManager

data class AudioChannel(
    val streamType: Int,
    val name: String,
    val shortName: String,
    val volume: Int = 0,
    val maxVolume: Int = 15,
    val isMuted: Boolean = false
) {
    val normalizedVolume: Float
        get() = if (maxVolume > 0) volume.toFloat() / maxVolume else 0f
}

val STREAM_CHANNELS = listOf(
    AudioChannel(AudioManager.STREAM_MUSIC, "Music", "MUS"),
    AudioChannel(AudioManager.STREAM_RING, "Ring", "RNG"),
    AudioChannel(AudioManager.STREAM_NOTIFICATION, "Notification", "NTF"),
    AudioChannel(AudioManager.STREAM_ALARM, "Alarm", "ALM"),
    AudioChannel(AudioManager.STREAM_SYSTEM, "System", "SYS"),
    AudioChannel(AudioManager.STREAM_VOICE_CALL, "Voice", "VOX"),
    AudioChannel(AudioManager.STREAM_ACCESSIBILITY, "Access", "ACC"),
    AudioChannel(AudioManager.STREAM_DTMF, "DTMF", "DTM"),
)
