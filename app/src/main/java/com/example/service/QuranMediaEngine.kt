package com.example.service

import android.content.Context
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioPlayer
import com.example.audio.AyahAudioState
import com.example.audio.QuranAudioPlayer
import com.example.data.local.OfflineIslamicData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class QuranPlaybackNotificationState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val title: String = "",
    val subtitle: String = "",
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isAyahMode: Boolean = false
)

/**
 * Process-scoped lightweight Quran audio engine so active recitation survives Activity
 * minimization, screen lock, or low-RAM Activity recreation, while synchronizing directly
 * with [QuranAudioPlaybackService] and strictly releasing MediaPlayer instances on stop.
 */
object QuranMediaEngine {

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized: Boolean = false

    val surahPlayer: QuranAudioPlayer by lazy { QuranAudioPlayer(engineScope) }
    val ayahPlayer: AyahAudioPlayer by lazy { AyahAudioPlayer(engineScope) }

    val surahState: StateFlow<AudioPlayerState> get() = surahPlayer.state
    val ayahState: StateFlow<AyahAudioState> get() = ayahPlayer.state

    @Volatile
    private var latestNotificationState: QuranPlaybackNotificationState = QuranPlaybackNotificationState()

    @Volatile
    private var surahNameResolver: ((Int) -> String)? = null

    private var audioOutputMonitor: com.example.audio.AudioOutputLatencyMonitor? = null

    fun ensureInitialized(context: Context, resolveSurahName: (Int) -> String) {
        val appCtx = context.applicationContext
        appContext = appCtx
        surahNameResolver = resolveSurahName
        surahPlayer.attachContext(appCtx)
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            initialized = true
            val monitor = com.example.audio.AudioOutputLatencyMonitor(appCtx)
            audioOutputMonitor = monitor
            monitor.start()
            engineScope.launch {
                monitor.currentRoute.collect { route ->
                    surahPlayer.updateAudioOutputRoute(route)
                }
            }
            observeStatesForForegroundService()
        }
    }

    private fun observeStatesForForegroundService() {
        engineScope.launch {
            combine(surahPlayer.state, ayahPlayer.state) { sState, aState ->
                buildNotificationSnapshot(sState, aState)
            }
                .distinctUntilChanged { old, new ->
                    // Avoid spamming NotificationManager every 80ms progress tick;
                    // only update the notification when transport state, title, subtitle, or buffering changes.
                    old.isActive == new.isActive &&
                        old.isPlaying == new.isPlaying &&
                        old.isBuffering == new.isBuffering &&
                        old.title == new.title &&
                        old.subtitle == new.subtitle &&
                        old.isAyahMode == new.isAyahMode
                }
                .collect { snapshot ->
                    latestNotificationState = snapshot
                    val ctx = appContext ?: return@collect
                    QuranAudioPlaybackService.syncForegroundState(ctx, snapshot)
                }
        }
    }

    private fun buildNotificationSnapshot(
        sState: AudioPlayerState,
        aState: AyahAudioState
    ): QuranPlaybackNotificationState {
        // Priority 1: Active Ayah-by-Ayah recitation (single or continuous)
        val ayahActive = aState.surahNumber != null &&
            aState.ayahNumber != null &&
            (aState.isPlaying || aState.isBuffering)
        if (ayahActive) {
            val sName = resolveSurahName(aState.surahNumber!!)
            return QuranPlaybackNotificationState(
                isActive = true,
                isPlaying = aState.isPlaying,
                isBuffering = aState.isBuffering,
                title = "$sName • الآية ${aState.ayahNumber}",
                subtitle = aState.activeReciterName.ifBlank { sState.selectedReciter.arabicName },
                positionMs = 0,
                durationMs = 0,
                playbackSpeed = sState.playbackSpeed,
                isAyahMode = true
            )
        }

        // Priority 2: Surah playback (playing, buffering, or paused while loaded in mini player)
        val surahLoaded = sState.loadedSurahNumber != null &&
            (sState.isPlaying || sState.isBuffering || sState.currentPositionMs > 0)
        if (surahLoaded) {
            val sName = sState.loadedSurahName.ifBlank { resolveSurahName(sState.loadedSurahNumber!!) }
            return QuranPlaybackNotificationState(
                isActive = true,
                isPlaying = sState.isPlaying,
                isBuffering = sState.isBuffering,
                title = sName,
                subtitle = "${sState.selectedReciter.arabicName} • ${sState.selectedReciter.riwayahName}",
                positionMs = sState.currentPositionMs,
                durationMs = sState.durationMs,
                playbackSpeed = sState.playbackSpeed,
                isAyahMode = false
            )
        }

        return QuranPlaybackNotificationState(isActive = false)
    }

    fun currentNotificationState(): QuranPlaybackNotificationState = latestNotificationState

    fun isCurrentlyPlaying(): Boolean {
        return surahPlayer.state.value.isPlaying || ayahPlayer.state.value.isPlaying
    }

    fun togglePlayPauseFromNotification() {
        val aState = ayahPlayer.state.value
        if (aState.surahNumber != null && (aState.isPlaying || aState.isBuffering)) {
            ayahPlayer.pauseIfPlaying()
            return
        }
        val sState = surahPlayer.state.value
        val sNum = sState.loadedSurahNumber ?: return
        val sName = sState.loadedSurahName.ifBlank { resolveSurahName(sNum) }
        surahPlayer.togglePlayPause(sNum, sName)
    }

    fun pauseFromSystem() {
        surahPlayer.pauseIfPlaying()
        ayahPlayer.pauseIfPlaying()
    }

    fun resumeFromSystem() {
        val aState = ayahPlayer.state.value
        if (aState.surahNumber != null && !aState.isPlaying && !aState.isBuffering && latestNotificationState.isAyahMode) {
            ayahPlayer.resumeIfPaused()
            return
        }
        val sState = surahPlayer.state.value
        val sNum = sState.loadedSurahNumber ?: return
        val sName = sState.loadedSurahName.ifBlank { resolveSurahName(sNum) }
        if (!sState.isPlaying && !sState.isBuffering) {
            surahPlayer.togglePlayPause(sNum, sName)
        }
    }

    fun skipNextFromNotification() {
        val sNum = surahPlayer.state.value.loadedSurahNumber
            ?: ayahPlayer.state.value.surahNumber
            ?: return
        val nextNum = (sNum + 1).coerceAtMost(114)
        if (nextNum == sNum) return
        ayahPlayer.stop()
        surahPlayer.playSurah(nextNum, resolveSurahName(nextNum), forceRestart = true)
    }

    fun skipPreviousFromNotification() {
        val sNum = surahPlayer.state.value.loadedSurahNumber
            ?: ayahPlayer.state.value.surahNumber
            ?: return
        val prevNum = (sNum - 1).coerceAtLeast(1)
        if (prevNum == sNum) return
        ayahPlayer.stop()
        surahPlayer.playSurah(prevNum, resolveSurahName(prevNum), forceRestart = true)
    }

    fun seekToMsFromSystem(positionMs: Int) {
        val dur = surahPlayer.state.value.durationMs
        if (dur > 0) {
            surahPlayer.seekToFraction((positionMs.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
        }
    }

    fun setDucking(duck: Boolean) {
        val vol = if (duck) 0.25f else 1.0f
        surahPlayer.setVolume(vol)
        ayahPlayer.setVolume(vol)
    }

    fun stopAllFromNotification() {
        surahPlayer.dismissAndStop()
        ayahPlayer.stop()
    }

    private fun resolveSurahName(surahNumber: Int): String {
        return surahNameResolver?.invoke(surahNumber)
            ?: OfflineIslamicData.fallbackSurahs.find { it.number == surahNumber }?.name
            ?: "سورة رقم $surahNumber"
    }
}
