package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.MuezzinItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AdhanAudioPlayer(private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var timeoutJob: Job? = null
    private var progressJob: Job? = null
    private var playSessionToken: Long = 0L

    private var currentMuezzin: MuezzinItem? = null
    private var currentSources: List<String> = emptyList()
    private var currentSourceIndex: Int = 0

    private val _state = MutableStateFlow(AdhanPlayerState())
    val state: StateFlow<AdhanPlayerState> = _state.asStateFlow()

    fun togglePreview(muezzin: MuezzinItem) {
        val current = _state.value
        if (current.activeMuezzinId == muezzin.id &&
            (current.status == AdhanPlaybackStatus.PLAYING ||
                current.status == AdhanPlaybackStatus.FALLBACK_PLAYING ||
                current.status == AdhanPlaybackStatus.LOADING)
        ) {
            stop()
            return
        }
        playMuezzin(muezzin)
    }

    fun playMuezzin(muezzin: MuezzinItem) {
        releasePlayerOnly()
        val token = ++playSessionToken

        // Strictly play ONLY this Muezzin's own audio source (never fall back to a different Muezzin's voice!)
        val candidateSources = buildList {
            if (!muezzin.localFilePath.isNullOrBlank() && File(muezzin.localFilePath).exists()) {
                add(muezzin.localFilePath)
            } else {
                if (muezzin.primaryUrl.isNotBlank()) {
                    add(muezzin.primaryUrl)
                }
                addAll(muezzin.fallbackUrls.filter { it.isNotBlank() })
            }
        }.distinct()

        currentMuezzin = muezzin
        currentSources = candidateSources
        currentSourceIndex = 0

        if (candidateSources.isEmpty()) {
            _state.value = AdhanPlayerState(
                activeMuezzinId = muezzin.id,
                activeMuezzinName = muezzin.name,
                status = AdhanPlaybackStatus.ERROR,
                errorDetail = "لا يوجد ملف صوتي صالح — استخدم «تعديل الرابط / رفع صوت»"
            )
            return
        }

        trySourceAtIndex(muezzin, candidateSources, index = 0, token = token)
    }

    fun switchToNextFallback() {
        val muezzin = currentMuezzin ?: return
        if (currentSources.size <= 1) {
            playMuezzin(muezzin)
            return
        }
        val nextIndex = (currentSourceIndex + 1) % currentSources.size
        val token = ++playSessionToken
        trySourceAtIndex(muezzin, currentSources, index = nextIndex, token = token)
    }

    fun retryActiveMuezzin() {
        val muezzin = currentMuezzin ?: return
        playMuezzin(muezzin)
    }

    private fun trySourceAtIndex(
        muezzin: MuezzinItem,
        sources: List<String>,
        index: Int,
        token: Long
    ) {
        if (token != playSessionToken) return

        if (index >= sources.size) {
            releasePlayerOnly()
            _state.value = AdhanPlayerState(
                activeMuezzinId = muezzin.id,
                activeMuezzinName = muezzin.name,
                status = AdhanPlaybackStatus.ERROR,
                errorDetail = "تعذر تشغيل الصوت — يمكنك المحاولة مجدداً أو استبداله عبر «تعديل الرابط / رفع صوت»",
                activeStreamIndex = 0,
                totalStreams = sources.size
            )
            return
        }

        currentSourceIndex = index
        val source = sources[index]
        val isFallback = index > 0
        releasePlayerOnly()

        _state.value = AdhanPlayerState(
            activeMuezzinId = muezzin.id,
            activeMuezzinName = muezzin.name,
            status = AdhanPlaybackStatus.LOADING,
            errorDetail = null,
            activeStreamIndex = index,
            totalStreams = sources.size,
            progressFraction = 0f
        )

        // Guard against hung remote streams (8.5s timeout -> next same-muezzin source or error)
        timeoutJob = scope.launch {
            delay(8500L)
            if (token == playSessionToken && _state.value.status == AdhanPlaybackStatus.LOADING) {
                trySourceAtIndex(muezzin, sources, index + 1, token)
            }
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener { player ->
                    if (token != playSessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    timeoutJob?.cancel()
                    player.start()
                    _state.value = AdhanPlayerState(
                        activeMuezzinId = muezzin.id,
                        activeMuezzinName = muezzin.name,
                        status = if (isFallback) AdhanPlaybackStatus.FALLBACK_PLAYING else AdhanPlaybackStatus.PLAYING,
                        errorDetail = null,
                        activeStreamIndex = index,
                        totalStreams = sources.size,
                        progressFraction = 0f
                    )
                    startProgressTicker(token)
                }
                setOnCompletionListener {
                    if (token != playSessionToken) return@setOnCompletionListener
                    timeoutJob?.cancel()
                    progressJob?.cancel()
                    releasePlayerOnly()
                    _state.value = AdhanPlayerState()
                }
                setOnErrorListener { _, _, _ ->
                    if (token != playSessionToken) return@setOnErrorListener true
                    timeoutJob?.cancel()
                    progressJob?.cancel()
                    trySourceAtIndex(muezzin, sources, index + 1, token)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            timeoutJob?.cancel()
            progressJob?.cancel()
            trySourceAtIndex(muezzin, sources, index + 1, token)
        }
    }

    private fun startProgressTicker(token: Long) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && token == playSessionToken) {
                val mp = mediaPlayer ?: break
                val fraction = runCatching {
                    val dur = mp.duration
                    val pos = mp.currentPosition
                    if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                }.getOrDefault(0f)
                _state.update { it.copy(progressFraction = fraction) }
                delay(350L)
            }
        }
    }

    fun stop() {
        playSessionToken++
        timeoutJob?.cancel()
        progressJob?.cancel()
        releasePlayerOnly()
        _state.value = AdhanPlayerState()
    }

    private fun releasePlayerOnly() {
        timeoutJob?.cancel()
        timeoutJob = null
        progressJob?.cancel()
        progressJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}
