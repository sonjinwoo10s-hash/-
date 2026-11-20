package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.local.OfflineIslamicData
import com.example.data.model.ReciterOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Int = 0,
    val durationMs: Int = 0,
    val selectedReciter: ReciterOption = OfflineIslamicData.reciters.first(),
    val loadedSurahNumber: Int? = null,
    val loadedSurahName: String = "",
    val activeStreamUrl: String? = null,
    val usedFallbackStream: Boolean = false,
    val errorMessage: String? = null
)

class QuranAudioPlayer(private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var timeoutJob: Job? = null
    private var preparedSurahKey: String? = null
    private var sessionToken: Long = 0L

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    fun syncPersistedReciter(reciter: ReciterOption) {
        if (_state.value.selectedReciter.id == reciter.id &&
            _state.value.selectedReciter.serverUrl == reciter.serverUrl
        ) return
        _state.update { it.copy(selectedReciter = reciter) }
    }

    fun setReciter(reciter: ReciterOption) {
        val current = _state.value
        val wasActive = current.isPlaying || current.isBuffering
        val surahNum = current.loadedSurahNumber
        val surahName = current.loadedSurahName
        releasePlayerOnly()
        preparedSurahKey = null

        _state.update {
            it.copy(
                selectedReciter = reciter,
                isPlaying = false,
                isBuffering = false,
                currentPositionMs = 0,
                durationMs = 0,
                activeStreamUrl = surahNum?.let { n -> reciter.buildPrimaryAudioUrl(n) },
                usedFallbackStream = false,
                errorMessage = null
            )
        }

        if (wasActive && surahNum != null) {
            playSurah(surahNum, surahName, forceRestart = true)
        }
    }

    fun prepareSurahMetadata(surahNumber: Int, surahName: String) {
        val current = _state.value
        if (current.loadedSurahNumber == surahNumber) {
            if (current.loadedSurahName.isBlank() && surahName.isNotBlank()) {
                _state.update { it.copy(loadedSurahName = surahName) }
            }
            return
        }
        // Only update metadata silently if nothing is currently playing
        if (!current.isPlaying && !current.isBuffering) {
            _state.update {
                it.copy(
                    loadedSurahNumber = surahNumber,
                    loadedSurahName = surahName,
                    currentPositionMs = 0,
                    durationMs = 0,
                    activeStreamUrl = it.selectedReciter.buildPrimaryAudioUrl(surahNumber),
                    errorMessage = null
                )
            }
        }
    }

    fun togglePlayPause(surahNumber: Int, surahName: String) {
        val current = _state.value
        val targetKey = "${current.selectedReciter.id}|${current.selectedReciter.serverUrl}|$surahNumber"
        val mp = mediaPlayer

        if (mp != null && preparedSurahKey == targetKey && !current.isBuffering) {
            if (runCatching { mp.isPlaying }.getOrDefault(false)) {
                runCatching { mp.pause() }
                stopProgressTicker()
                _state.update { it.copy(isPlaying = false) }
            } else {
                runCatching { mp.start() }
                startProgressTicker()
                _state.update { it.copy(isPlaying = true, errorMessage = null) }
            }
            return
        }

        playSurah(surahNumber, surahName, forceRestart = true)
    }

    fun playSurah(surahNumber: Int, surahName: String, forceRestart: Boolean = false) {
        val reciter = _state.value.selectedReciter
        val targetKey = "${reciter.id}|${reciter.serverUrl}|$surahNumber"
        if (!forceRestart && preparedSurahKey == targetKey && _state.value.isPlaying) return

        val token = ++sessionToken
        releasePlayerOnly()
        preparedSurahKey = null

        val urls = reciter.buildCandidateAudioUrls(surahNumber)
        _state.update {
            it.copy(
                loadedSurahNumber = surahNumber,
                loadedSurahName = surahName,
                isBuffering = true,
                isPlaying = false,
                currentPositionMs = 0,
                durationMs = 0,
                activeStreamUrl = urls.firstOrNull(),
                usedFallbackStream = false,
                errorMessage = null
            )
        }

        tryStreamIndex(surahNumber, surahName, targetKey, urls, index = 0, token = token)
    }

    private fun tryStreamIndex(
        surahNumber: Int,
        surahName: String,
        targetKey: String,
        urls: List<String>,
        index: Int,
        token: Long
    ) {
        if (token != sessionToken) return
        if (index >= urls.size) {
            releasePlayerOnly()
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "تعذّر تشغيل التلاوة حالياً، تأكد من الاتصال بالإنترنت أو جرّب قارئاً آخر"
                )
            }
            return
        }

        val url = urls[index]
        val isFallback = index > 0
        releasePlayerOnly()

        _state.update {
            it.copy(
                isBuffering = true,
                activeStreamUrl = url,
                usedFallbackStream = isFallback,
                errorMessage = null
            )
        }

        timeoutJob = scope.launch(Dispatchers.Main) {
            delay(10_000L)
            if (token == sessionToken && _state.value.isBuffering) {
                tryStreamIndex(surahNumber, surahName, targetKey, urls, index + 1, token)
            }
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { player ->
                    if (token != sessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    timeoutJob?.cancel()
                    preparedSurahKey = targetKey
                    player.start()
                    _state.update {
                        it.copy(
                            loadedSurahNumber = surahNumber,
                            loadedSurahName = surahName,
                            isBuffering = false,
                            isPlaying = true,
                            durationMs = player.duration.coerceAtLeast(0),
                            currentPositionMs = 0,
                            activeStreamUrl = url,
                            usedFallbackStream = isFallback,
                            errorMessage = null
                        )
                    }
                    startProgressTicker()
                }
                setOnCompletionListener {
                    if (token != sessionToken) return@setOnCompletionListener
                    stopProgressTicker()
                    _state.update { it.copy(isPlaying = false, currentPositionMs = 0) }
                }
                setOnErrorListener { _, _, _ ->
                    if (token != sessionToken) return@setOnErrorListener true
                    timeoutJob?.cancel()
                    tryStreamIndex(surahNumber, surahName, targetKey, urls, index + 1, token)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            timeoutJob?.cancel()
            tryStreamIndex(surahNumber, surahName, targetKey, urls, index + 1, token)
        }
    }

    fun seekToFraction(fraction: Float) {
        val mp = mediaPlayer ?: return
        val duration = _state.value.durationMs
        if (duration > 0) {
            val targetMs = (fraction.coerceIn(0f, 1f) * duration).toInt()
            runCatching { mp.seekTo(targetMs) }
            _state.update { it.copy(currentPositionMs = targetMs) }
        }
    }

    fun dismissAndStop() {
        sessionToken++
        releasePlayerOnly()
        preparedSurahKey = null
        val reciter = _state.value.selectedReciter
        _state.value = AudioPlayerState(selectedReciter = reciter)
    }

    fun pauseIfPlaying() {
        val mp = mediaPlayer ?: return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            runCatching { mp.pause() }
            stopProgressTicker()
            _state.update { it.copy(isPlaying = false) }
        }
    }

    fun stopAndRelease(keepReciter: Boolean = true) {
        sessionToken++
        releasePlayerOnly()
        preparedSurahKey = null
        val reciter = if (keepReciter) _state.value.selectedReciter else OfflineIslamicData.reciters.first()
        _state.value = AudioPlayerState(selectedReciter = reciter)
    }

    private fun releasePlayerOnly() {
        stopProgressTicker()
        timeoutJob?.cancel()
        timeoutJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                    _state.update {
                        it.copy(
                            currentPositionMs = runCatching { mp.currentPosition }.getOrDefault(it.currentPositionMs),
                            durationMs = runCatching { mp.duration }.getOrDefault(it.durationMs).coerceAtLeast(0)
                        )
                    }
                }
                delay(400)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
