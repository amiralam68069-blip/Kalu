package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import android.view.KeyEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DeviceController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val _brightnessPercent = MutableStateFlow(75)
    val brightnessPercent: StateFlow<Int> = _brightnessPercent.asStateFlow()

    private val _volumePercent = MutableStateFlow(getCurrentVolumePercent())
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    fun setFlashlight(enabled: Boolean): Boolean {
        return try {
            val camId = cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager?.cameraIdList?.firstOrNull()

            if (camId != null) {
                cameraManager?.setTorchMode(camId, enabled)
                _isFlashlightOn.value = enabled
                true
            } else {
                _isFlashlightOn.value = enabled
                false
            }
        } catch (e: Exception) {
            _isFlashlightOn.value = enabled
            false
        }
    }

    fun getCurrentVolumePercent(): Int {
        val am = audioManager ?: return 70
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val cur = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((cur.toFloat() / max.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int): Int {
        val clamped = percent.coerceIn(0, 100)
        val am = audioManager
        if (am != null) {
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val targetIndex = ((clamped / 100f) * max).toInt().coerceIn(0, max)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
        }
        _volumePercent.value = clamped
        return clamped
    }

    fun adjustVolume(increase: Boolean): Int {
        val current = getCurrentVolumePercent()
        val next = if (increase) (current + 15).coerceAtMost(100) else (current - 15).coerceAtLeast(0)
        return setVolumePercent(next)
    }

    fun setBrightnessPercent(percent: Int): Int {
        val clamped = percent.coerceIn(5, 100)
        _brightnessPercent.value = clamped
        return clamped
    }

    fun adjustBrightness(increase: Boolean): Int {
        val next = if (increase) (_brightnessPercent.value + 20).coerceAtMost(100) else (_brightnessPercent.value - 20).coerceAtLeast(10)
        _brightnessPercent.value = next
        return next
    }

    fun dispatchMediaKey(keyCode: Int): Boolean {
        val am = audioManager ?: return false
        return try {
            val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val upEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            am.dispatchMediaKeyEvent(downEvent)
            am.dispatchMediaKeyEvent(upEvent)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun buildOpenAppIntent(appNameQuery: String): Intent? {
        val lower = appNameQuery.lowercase()
        val pm = context.packageManager

        val packageAndFallback: Pair<String, String> = when {
            lower.contains("instagram") || lower.contains("insta") ->
                "com.instagram.android" to "https://www.instagram.com"
            lower.contains("whatsapp") ->
                "com.whatsapp" to "https://web.whatsapp.com"
            lower.contains("spotify") ->
                "com.spotify.music" to "https://open.spotify.com"
            lower.contains("youtube") ->
                "com.google.android.youtube" to "https://www.youtube.com"
            lower.contains("chrome") || lower.contains("browser") ->
                "com.android.chrome" to "https://www.google.com"
            lower.contains("setting") ->
                return Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            else -> return null
        }

        val launchIntent = pm.getLaunchIntentForPackage(packageAndFallback.first)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return launchIntent
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse(packageAndFallback.second)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAlarmIntent(hour: Int, minute: Int, label: String): Intent {
        return Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAccessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
