package com.motofader.audio

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.motofader.model.AudioChannel
import com.motofader.model.STREAM_CHANNELS
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class StreamVolumeManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _channels = MutableStateFlow<List<AudioChannel>>(emptyList())
    val channels: StateFlow<List<AudioChannel>> = _channels

    private val previousVolumes = mutableMapOf<Int, Int>()

    private var isMonitoring = false

    private val volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: android.net.Uri?) {
            // Filter to volume-related setting changes only
            val key = uri?.lastPathSegment ?: ""
            if (key.startsWith("volume_") || key == "mode_ringer") {
                refreshVolumes()
            }
        }
    }

    fun start() {
        if (isMonitoring) return
        isMonitoring = true
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI, true, volumeObserver
        )
        refreshVolumes()
    }

    fun stop() {
        if (!isMonitoring) return
        isMonitoring = false
        context.contentResolver.unregisterContentObserver(volumeObserver)
    }

    fun refreshVolumes() {
        _channels.value = STREAM_CHANNELS.map { template ->
            val maxVol = audioManager.getStreamMaxVolume(template.streamType)
            val curVol = audioManager.getStreamVolume(template.streamType)
            val muted = audioManager.isStreamMute(template.streamType)
            template.copy(
                volume = curVol,
                maxVolume = maxVol,
                isMuted = muted
            )
        }
    }

    fun setVolume(streamType: Int, volume: Int) {
        audioManager.setStreamVolume(streamType, volume, 0)
        refreshVolumes()
    }

    fun toggleMute(streamType: Int) {
        val isMuted = audioManager.isStreamMute(streamType)
        if (isMuted) {
            val prev = previousVolumes[streamType]
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
            // Post volume restore to let unmute complete first
            if (prev != null && prev > 0) {
                Handler(Looper.getMainLooper()).post {
                    audioManager.setStreamVolume(streamType, prev, 0)
                    refreshVolumes()
                }
                return
            }
        } else {
            previousVolumes[streamType] = audioManager.getStreamVolume(streamType)
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, 0)
        }
        refreshVolumes()
    }
}
