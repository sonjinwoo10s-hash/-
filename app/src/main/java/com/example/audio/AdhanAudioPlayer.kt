package com.example.audio

import android.content.Context
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.MuezzinItem
import com.example.data.repository.MuezzinRepository
import com.example.service.AdhanPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * Lifecycle-safe facade over [AdhanPlaybackService] so all in-app Adhan playback
 * runs inside the Foreground Service with MediaSession controls, Audio Focus, and
 * uninterrupted background persistence.
 */
class AdhanAudioPlayer(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
) {

    val state: StateFlow<AdhanPlayerState> = AdhanPlaybackService.playbackState

    fun togglePreview(muezzin: MuezzinItem) {
        val current = state.value
        if (current.activeMuezzinId == muezzin.id &&
            (current.status == AdhanPlaybackStatus.PLAYING ||
                current.status == AdhanPlaybackStatus.FALLBACK_PLAYING ||
                current.status == AdhanPlaybackStatus.LOADING ||
                current.status == AdhanPlaybackStatus.PAUSED)
        ) {
            stop()
            return
        }
        playMuezzin(muezzin)
    }

    fun playMuezzin(muezzin: MuezzinItem) {
        // Strictly resolve ONLY this Muezzin's own audio sources:
        // 1) User-uploaded custom local file
        // 2) Offline cached MP3 in `filesDir/adhan/{id}.mp3`
        // 3) Remote direct MP3 URL + fallback URLs
        val cachedFile = MuezzinRepository.getCachedAdhanFile(context, muezzin.id)
        val candidateSources = ArrayList(
            buildList {
                if (!muezzin.localFilePath.isNullOrBlank() && File(muezzin.localFilePath).exists()) {
                    add(muezzin.localFilePath)
                } else if (cachedFile.exists() && cachedFile.length() > 0L) {
                    add(cachedFile.absolutePath)
                    if (muezzin.primaryUrl.isNotBlank()) {
                        add(muezzin.primaryUrl)
                    }
                } else {
                    if (muezzin.primaryUrl.isNotBlank()) {
                        add(muezzin.primaryUrl)
                    }
                    addAll(muezzin.fallbackUrls.filter { it.isNotBlank() })
                }
            }.distinct()
        )

        AdhanPlaybackService.startPreview(
            context = context,
            muezzinId = muezzin.id,
            muezzinName = muezzin.name,
            sources = candidateSources
        )
    }

    fun switchToNextFallback() {
        AdhanPlaybackService.sendControlAction(context, AdhanPlaybackService.ACTION_SWITCH_FALLBACK)
    }

    fun retryActiveMuezzin() {
        AdhanPlaybackService.sendControlAction(context, AdhanPlaybackService.ACTION_RETRY)
    }

    fun stop() {
        AdhanPlaybackService.sendControlAction(context, AdhanPlaybackService.ACTION_STOP_ADHAN)
    }
}
