package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import com.example.data.model.FajrAlarmSound
import com.example.data.model.MuezzinItem
import com.example.data.repository.MuezzinRepository
import com.example.util.VibrationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class FajrAlarmPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var toneLoopJob: Job? = null
    private var vibrationJob: Job? = null
    private var sessionToken: Long = 0L

    fun startRinging(sound: FajrAlarmSound, fajrMuezzin: MuezzinItem) {
        stopRinging()
        val token = ++sessionToken

        startAlarmVibrationPulse(token)

        when (sound) {
            FajrAlarmSound.FAJR_ADHAN -> {
                val cachedMp3 = MuezzinRepository.getCachedAdhanFile(context, fajrMuezzin.id)
                val sources = buildList {
                    val localPath = fajrMuezzin.localFilePath
                    if (!localPath.isNullOrBlank() && File(localPath).exists()) {
                        add(localPath)
                    } else if (cachedMp3.exists() && cachedMp3.length() > 0L) {
                        add(cachedMp3.absolutePath)
                        if (fajrMuezzin.primaryUrl.isNotBlank()) add(fajrMuezzin.primaryUrl)
                    } else {
                        if (fajrMuezzin.primaryUrl.isNotBlank()) add(fajrMuezzin.primaryUrl)
                        addAll(fajrMuezzin.fallbackUrls)
                    }
                }.distinct()
                playLoopingUrlOrFallback(sources, index = 0, token = token)
            }
            FajrAlarmSound.LOUD_ALARM -> {
                playSystemAlarmRingtoneOrToneFallback(token)
            }
        }
    }

    private fun playLoopingUrlOrFallback(sources: List<String>, index: Int, token: Long) {
        if (token != sessionToken) return
        if (index >= sources.size) {
            playSystemAlarmRingtoneOrToneFallback(token)
            return
        }

        releaseMediaPlayerOnly()
        val src = sources[index]
        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(src)
                isLooping = true
                setOnPreparedListener { player ->
                    if (token != sessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    player.start()
                }
                setOnErrorListener { _, _, _ ->
                    if (token != sessionToken) return@setOnErrorListener true
                    playLoopingUrlOrFallback(sources, index + 1, token)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            playLoopingUrlOrFallback(sources, index + 1, token)
        }
    }

    private fun playSystemAlarmRingtoneOrToneFallback(token: Long) {
        if (token != sessionToken) return
        releaseMediaPlayerOnly()

        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (uri != null) {
            val started = runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    prepare()
                    start()
                }
                mediaPlayer = mp
                true
            }.getOrDefault(false)
            if (started) return
        }

        // Guaranteed audible repeating alarm fallback even if device has no default alarm URI
        toneLoopJob?.cancel()
        toneLoopJob = scope.launch(Dispatchers.Default) {
            val toneGen = runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 95) }.getOrNull()
            try {
                while (isActive && token == sessionToken) {
                    runCatching { toneGen?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600) }
                    delay(1100)
                }
            } finally {
                runCatching { toneGen?.release() }
            }
        }
    }

    private fun startAlarmVibrationPulse(token: Long) {
        vibrationJob?.cancel()
        if (!VibrationHelper.isVibrationSupported(context)) return
        vibrationJob = scope.launch(Dispatchers.Default) {
            while (isActive && token == sessionToken) {
                VibrationHelper.vibrateRoundComplete(context)
                delay(2000)
            }
        }
    }

    fun stopRinging() {
        sessionToken++
        toneLoopJob?.cancel()
        toneLoopJob = null
        vibrationJob?.cancel()
        vibrationJob = null
        releaseMediaPlayerOnly()
    }

    private fun releaseMediaPlayerOnly() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}
