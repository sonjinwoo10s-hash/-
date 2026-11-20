package com.example.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibrationHelper {

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun isVibrationSupported(context: Context): Boolean {
        val vibrator = getVibrator(context) ?: return false
        return runCatching { vibrator.hasVibrator() }.getOrDefault(false)
    }

    fun vibrateShortTick(context: Context, durationMs: Long = 20L) {
        val vibrator = getVibrator(context) ?: return
        if (!runCatching { vibrator.hasVibrator() }.getOrDefault(false)) return

        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        }
    }

    fun vibrateRoundComplete(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (!runCatching { vibrator.hasVibrator() }.getOrDefault(false)) return

        val pattern = longArrayOf(0L, 30L, 50L, 35L)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }
}
