package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioOutputRoute(
    val arabicLabel: String,
    val isBluetooth: Boolean,
    val defaultLatencyMs: Int
) {
    BLUETOOTH(
        arabicLabel = "سماعة بلوتوث (تزامن تلقائي)",
        isBluetooth = true,
        defaultLatencyMs = 180
    ),
    WIRED_OR_USB(
        arabicLabel = "سماعة سلكية",
        isBluetooth = false,
        defaultLatencyMs = 0
    ),
    SPEAKER(
        arabicLabel = "سماعة الجهاز",
        isBluetooth = false,
        defaultLatencyMs = 0
    )
}

/**
 * Automatically detects the active Android audio output route (Bluetooth A2DP/LE/SCO vs.
 * Wired/USB-C vs. Phone Speaker) in real time using [AudioManager.getDevices] and
 * [AudioDeviceCallback] without requiring extra Bluetooth permissions or interrupting playback.
 */
class AudioOutputLatencyMonitor(private val context: Context) {

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _currentRoute = MutableStateFlow(detectCurrentOutputRoute())
    val currentRoute: StateFlow<AudioOutputRoute> = _currentRoute.asStateFlow()

    @Volatile
    private var started = false

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            refreshRoute()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            refreshRoute()
        }
    }

    private val routeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refreshRoute()
        }
    }

    fun start() {
        if (started) return
        started = true
        refreshRoute()
        runCatching {
            audioManager?.registerAudioDeviceCallback(deviceCallback, mainHandler)
        }
        runCatching {
            val filter = IntentFilter().apply {
                addAction(AudioManager.ACTION_HEADSET_PLUG)
                addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.applicationContext.registerReceiver(
                    routeBroadcastReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.applicationContext.registerReceiver(routeBroadcastReceiver, filter)
            }
        }
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { audioManager?.unregisterAudioDeviceCallback(deviceCallback) }
        runCatching { context.applicationContext.unregisterReceiver(routeBroadcastReceiver) }
    }

    fun refreshRoute(): AudioOutputRoute {
        val detected = detectCurrentOutputRoute()
        if (_currentRoute.value != detected) {
            _currentRoute.value = detected
        }
        return detected
    }

    private fun detectCurrentOutputRoute(): AudioOutputRoute {
        val am = audioManager ?: return AudioOutputRoute.SPEAKER
        val outputs = runCatching {
            am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull().orEmpty()

        var hasBluetooth = false
        var hasWired = false

        for (device in outputs) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_HEARING_AID -> {
                    hasBluetooth = true
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_AUX_LINE -> {
                    hasWired = true
                }
                else -> {
                    // API 31+ LE Audio Bluetooth output types
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                            device.type == AudioDeviceInfo.TYPE_BLE_BROADCAST
                        ) {
                            hasBluetooth = true
                        }
                    }
                }
            }
        }

        // Priority: Wired headphones take precedence if plugged in while BT is idle, else Bluetooth, else Speaker
        return when {
            hasBluetooth && !hasWired -> AudioOutputRoute.BLUETOOTH
            hasWired -> AudioOutputRoute.WIRED_OR_USB
            hasBluetooth -> AudioOutputRoute.BLUETOOTH
            else -> AudioOutputRoute.SPEAKER
        }
    }
}
