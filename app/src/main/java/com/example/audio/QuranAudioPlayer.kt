package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.SystemClock
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AyahRepeatMode
import com.example.data.model.ReciterOption
import com.example.data.repository.QuranTimingRepository
import com.example.data.repository.SurahTimingMap
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
    val playbackSpeed: Float = 1.0f,
    val selectedReciter: ReciterOption = OfflineIslamicData.reciters.first(),
    val loadedSurahNumber: Int? = null,
    val loadedSurahName: String = "",
    val activeStreamUrl: String? = null,
    val isPlayingOffline: Boolean = false,
    val usedFallbackStream: Boolean = false,
    val errorMessage: String? = null,
    val activeVerseKey: String? = null,
    val syncedAyahNumber: Int? = null,
    val syncedWordIndex: Int? = null,
    val isSingleAyahMode: Boolean = false,
    val hasOfficialTimings: Boolean = false,
    val hasWordTimings: Boolean = false,
    val wordByWordEnabled: Boolean = true,
    val audioOutputRoute: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val bluetoothOffsetMs: Int = AudioOutputRoute.BLUETOOTH.defaultLatencyMs,
    val speakerOffsetMs: Int = AudioOutputRoute.SPEAKER.defaultLatencyMs
) {
    val activeLatencyOffsetMs: Int
        get() = if (audioOutputRoute.isBluetooth) bluetoothOffsetMs else speakerOffsetMs
}

object QuranPlaybackSpeeds {
    val options: List<Float> = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun formatLabel(speed: Float): String {
        return when (speed) {
            0.5f -> "0.5x"
            0.75f -> "0.75x"
            1.0f -> "1.0x"
            1.25f -> "1.25x"
            1.5f -> "1.5x"
            2.0f -> "2.0x"
            else -> "${speed}x"
        }
    }
}

/**
 * Seamless single-MP3 Surah audio player powered by official Quran.com v4 millisecond
 * timestamps & word-level segments, with automatic Bluetooth/Speaker latency compensation,
 * 50ms lightweight highlight sync loop (running ONLY while playing), instant tap-to-seek,
 * and strict smart fallback (disabling highlighting for mp3quran.net reciters without timestamps).
 */
class QuranAudioPlayer(private val scope: CoroutineScope) {

    @Volatile
    private var appContext: Context? = null

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private var timeoutJob: Job? = null
    private var prepareFlowJob: Job? = null
    private var activeTimingMap: SurahTimingMap? = null
    private var preparedSurahKey: String? = null
    private var sessionToken: Long = 0L

    // Single-Ayah bounded playback state (for "استماع لهذه الآية فقط" using seamless Surah timestamps)
    private var playSingleAyahOnly: Boolean = false
    private var stopAtMs: Int? = null
    private var singleAyahStartMs: Int? = null
    private var singleAyahRepeatMode: AyahRepeatMode = AyahRepeatMode.ONCE
    private var singleAyahRemainingRepeats: Int = 0
    private var singleAyahSeekGraceUntilMs: Long = 0L
    private var singleAyahFallbackCallback: (() -> Unit)? = null
    private var onSurahCompleteCallback: ((Int) -> Unit)? = null

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private val _activeVerseKey = MutableStateFlow<String?>(null)
    val activeVerseKey: StateFlow<String?> = _activeVerseKey.asStateFlow()

    private val _activeWordIndex = MutableStateFlow<Int?>(null)
    val activeWordIndex: StateFlow<Int?> = _activeWordIndex.asStateFlow()

    private fun updateHighlightFlows(surahNumber: Int?, ayahNumber: Int?, wordIndex: Int?) {
        val nextKey = if (surahNumber != null && ayahNumber != null) "$surahNumber:$ayahNumber" else null
        val nextWord = if (nextKey != null) wordIndex else null
        if (_activeVerseKey.value != nextKey) {
            _activeVerseKey.value = nextKey
        }
        if (_activeWordIndex.value != nextWord) {
            _activeWordIndex.value = nextWord
        }
    }

    private fun seekPrecise(mp: MediaPlayer, targetMs: Int) {
        val safeTarget = targetMs.coerceAtLeast(0)
        if (playSingleAyahOnly) {
            // Brief settle window so stale pre-seek currentPosition never triggers early pause
            singleAyahSeekGraceUntilMs = SystemClock.elapsedRealtime() + 260L
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mp.seekTo(safeTarget.toLong(), MediaPlayer.SEEK_CLOSEST)
            } else {
                mp.seekTo(safeTarget)
            }
        }
    }

    fun attachContext(context: Context) {
        appContext = context.applicationContext
    }

    fun updateAudioOutputRoute(route: AudioOutputRoute) {
        val current = _state.value
        if (current.audioOutputRoute == route) return
        _state.update { it.copy(audioOutputRoute = route) }
        // Immediately re-evaluate active highlight with the new route's latency offset
        val timing = activeTimingMap
        val mp = mediaPlayer
        if (timing != null && mp != null && current.isPlaying) {
            val posMs = runCatching { mp.currentPosition }.getOrDefault(current.currentPositionMs)
            applyResolvedHighlightForPosition(posMs)
        }
    }

    fun syncLatencyPreferences(
        wordByWordEnabled: Boolean,
        bluetoothOffsetMs: Int,
        speakerOffsetMs: Int
    ) {
        if (!wordByWordEnabled) {
            _activeWordIndex.value = null
        }
        _state.update {
            it.copy(
                wordByWordEnabled = wordByWordEnabled,
                bluetoothOffsetMs = bluetoothOffsetMs.coerceIn(-300, 300),
                speakerOffsetMs = speakerOffsetMs.coerceIn(-300, 300),
                syncedWordIndex = if (wordByWordEnabled) it.syncedWordIndex else null
            )
        }
    }

    fun setWordByWordEnabled(enabled: Boolean) {
        if (!enabled) {
            _activeWordIndex.value = null
        }
        _state.update {
            it.copy(
                wordByWordEnabled = enabled,
                syncedWordIndex = if (enabled) it.syncedWordIndex else null
            )
        }
    }

    fun setLatencyOffsetForActiveRoute(offsetMs: Int): Pair<Boolean, Int> {
        val clamped = offsetMs.coerceIn(-300, 300)
        val isBt = _state.value.audioOutputRoute.isBluetooth
        _state.update {
            if (isBt) it.copy(bluetoothOffsetMs = clamped)
            else it.copy(speakerOffsetMs = clamped)
        }
        val mp = mediaPlayer
        if (activeTimingMap != null && mp != null && _state.value.isPlaying) {
            val posMs = runCatching { mp.currentPosition }.getOrDefault(_state.value.currentPositionMs)
            applyResolvedHighlightForPosition(posMs)
        }
        return isBt to clamped
    }

    fun syncPersistedReciter(reciter: ReciterOption) {
        if (_state.value.selectedReciter.id == reciter.id &&
            _state.value.selectedReciter.serverUrl == reciter.serverUrl
        ) return
        _state.update { it.copy(selectedReciter = reciter) }
    }

    fun syncPersistedSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackSpeed = safeSpeed) }
    }

    fun setPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        _state.update { it.copy(playbackSpeed = safeSpeed) }
        val mp = mediaPlayer ?: return
        if (runCatching { mp.isPlaying }.getOrDefault(false)) {
            applySpeedToPlayer(mp, safeSpeed)
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        runCatching { mediaPlayer?.setVolume(clamped, clamped) }
    }

    private fun applySpeedToPlayer(player: MediaPlayer, speed: Float) {
        runCatching {
            val params = player.playbackParams
            params.speed = speed.coerceIn(0.5f, 2.0f)
            params.pitch = 1.0f
            player.playbackParams = params
        }
    }

    fun setReciter(reciter: ReciterOption) {
        val current = _state.value
        val wasActive = current.isPlaying || current.isBuffering
        val surahNum = current.loadedSurahNumber
        val surahName = current.loadedSurahName
        val resumeAyah = current.syncedAyahNumber
        prepareFlowJob?.cancel()
        releasePlayerOnly()
        preparedSurahKey = null
        activeTimingMap = null
        clearSingleAyahBounds()
        updateHighlightFlows(null, null, null)

        _state.update {
            it.copy(
                selectedReciter = reciter,
                isPlaying = false,
                isBuffering = false,
                currentPositionMs = 0,
                durationMs = 0,
                activeStreamUrl = surahNum?.let { n -> reciter.buildPrimaryAudioUrl(n) },
                usedFallbackStream = false,
                errorMessage = null,
                activeVerseKey = null,
                syncedAyahNumber = null,
                syncedWordIndex = null,
                isSingleAyahMode = false,
                hasOfficialTimings = false,
                hasWordTimings = false
            )
        }

        if (wasActive && surahNum != null) {
            if (resumeAyah != null && QuranTimingRepository.resolveQuranComReciterId(reciter) != null) {
                playSurahFromAyah(surahNum, surahName, resumeAyah)
            } else {
                playSurah(surahNum, surahName, forceRestart = true)
            }
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
        if (!current.isPlaying && !current.isBuffering) {
            updateHighlightFlows(null, null, null)
            _state.update {
                it.copy(
                    loadedSurahNumber = surahNumber,
                    loadedSurahName = surahName,
                    currentPositionMs = 0,
                    durationMs = 0,
                    activeStreamUrl = it.selectedReciter.buildPrimaryAudioUrl(surahNumber),
                    errorMessage = null,
                    activeVerseKey = null,
                    syncedAyahNumber = null,
                    syncedWordIndex = null,
                    isSingleAyahMode = false,
                    hasOfficialTimings = false,
                    hasWordTimings = false
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
                // Explicitly switch to continuous full-Surah playback when tapping Surah Play
                clearSingleAyahBounds()
                runCatching {
                    mp.start()
                    applySpeedToPlayer(mp, _state.value.playbackSpeed)
                }
                startProgressTicker()
                _state.update {
                    it.copy(
                        isPlaying = true,
                        isSingleAyahMode = false,
                        errorMessage = null
                    )
                }
            }
            return
        }

        playSurah(surahNumber, surahName, forceRestart = true)
    }

    fun playSurah(
        surahNumber: Int,
        surahName: String,
        forceRestart: Boolean = false,
        onSurahCompleted: ((Int) -> Unit)? = null
    ) {
        clearSingleAyahBounds()
        onSurahCompleteCallback = onSurahCompleted
        val reciter = _state.value.selectedReciter
        val targetKey = "${reciter.id}|${reciter.serverUrl}|$surahNumber"
        if (!forceRestart && preparedSurahKey == targetKey && _state.value.isPlaying) return

        launchSurahPlayback(
            surahNumber = surahNumber,
            surahName = surahName,
            startAyahNumber = null,
            startWordIndex = null,
            singleAyahOnly = false,
            repeatMode = AyahRepeatMode.ONCE,
            onSingleAyahFallbackRequired = null
        )
    }

    /**
     * Plays a Surah starting cleanly at the exact millisecond timestamp (`timestamp_from`) of
     * [startAyahNumber] (or [startWordIndex] within that Ayah when provided).
     *
     * Returns `true` if the current reciter supports official Quran.com v4 timestamps and
     * playback/seek was handled directly by this seamless player; returns `false` if the reciter
     * has no official timestamps (caller can decide whether to play full Surah with highlighting off).
     */
    fun hasOfficialTimestampsSupportForCurrentReciter(): Boolean {
        return QuranTimingRepository.resolveQuranComReciterId(_state.value.selectedReciter) != null
    }

    fun playSurahFromAyah(
        surahNumber: Int,
        surahName: String,
        startAyahNumber: Int,
        startWordIndex: Int? = null,
        singleAyahOnly: Boolean = false,
        repeatMode: AyahRepeatMode = AyahRepeatMode.ONCE,
        onSurahCompleted: ((Int) -> Unit)? = null,
        onSingleAyahFallbackRequired: (() -> Unit)? = null
    ) {
        onSurahCompleteCallback = if (singleAyahOnly) null else onSurahCompleted

        val current = _state.value
        val reciter = current.selectedReciter
        val targetKey = "${reciter.id}|${reciter.serverUrl}|$surahNumber"
        val mp = mediaPlayer
        val timing = activeTimingMap

        // Fast path: Surah is already prepared with official timestamps -> instant zero-gap seek!
        if (mp != null &&
            preparedSurahKey == targetKey &&
            timing != null &&
            timing.surahNumber == surahNumber &&
            !current.isBuffering
        ) {
            val ayahEntry = timing.findAyahEntry(startAyahNumber)
            if (ayahEntry != null) {
                val targetMs = if (startWordIndex != null) {
                    ayahEntry.findWordStartMs(startWordIndex) ?: ayahEntry.startMs
                } else {
                    ayahEntry.startMs
                }
                configureSingleAyahBounds(
                    singleAyahOnly = singleAyahOnly,
                    startMs = ayahEntry.startMs,
                    endMs = ayahEntry.endMs,
                    repeatMode = repeatMode,
                    fallbackCallback = onSingleAyahFallbackRequired
                )
                seekPrecise(mp, targetMs)
                runCatching {
                    if (!mp.isPlaying) {
                        mp.start()
                        applySpeedToPlayer(mp, _state.value.playbackSpeed)
                    }
                }
                val resolvedWord = if (current.wordByWordEnabled && timing.hasWordSegments) {
                    startWordIndex ?: ayahEntry.resolveWordAt(targetMs)
                } else null
                updateHighlightFlows(surahNumber, ayahEntry.ayahNumber, resolvedWord)
                _state.update {
                    it.copy(
                        isPlaying = true,
                        isSingleAyahMode = singleAyahOnly,
                        currentPositionMs = targetMs,
                        activeVerseKey = "$surahNumber:${ayahEntry.ayahNumber}",
                        syncedAyahNumber = ayahEntry.ayahNumber,
                        syncedWordIndex = resolvedWord,
                        errorMessage = null
                    )
                }
                startProgressTicker()
                return
            } else if (singleAyahOnly) {
                // Never play full Surah when single-Ayah timing entry is missing
                onSingleAyahFallbackRequired?.invoke()
                return
            }
        }

        launchSurahPlayback(
            surahNumber = surahNumber,
            surahName = surahName,
            startAyahNumber = startAyahNumber,
            startWordIndex = startWordIndex,
            singleAyahOnly = singleAyahOnly,
            repeatMode = repeatMode,
            onSingleAyahFallbackRequired = onSingleAyahFallbackRequired
        )
    }

    /**
     * Instant tap-to-seek to an exact word or Ayah while the same Surah is actively loaded with official timestamps.
     */
    fun seekToAyahOrWordIfLoaded(
        surahNumber: Int,
        ayahNumber: Int,
        wordIndexZeroBased: Int? = null
    ): Boolean {
        val current = _state.value
        val timing = activeTimingMap ?: return false
        val mp = mediaPlayer ?: return false
        if (current.loadedSurahNumber != surahNumber || timing.surahNumber != surahNumber || current.isBuffering) {
            return false
        }
        val entry = timing.findAyahEntry(ayahNumber) ?: return false
        val targetMs = if (wordIndexZeroBased != null) {
            entry.findWordStartMs(wordIndexZeroBased) ?: entry.startMs
        } else {
            entry.startMs
        }
        clearSingleAyahBounds()
        seekPrecise(mp, targetMs)
        runCatching {
            if (!mp.isPlaying) {
                mp.start()
                applySpeedToPlayer(mp, current.playbackSpeed)
            }
        }
        val resolvedWord = if (current.wordByWordEnabled && timing.hasWordSegments) {
            wordIndexZeroBased ?: entry.resolveWordAt(targetMs)
        } else null
        updateHighlightFlows(surahNumber, entry.ayahNumber, resolvedWord)
        _state.update {
            it.copy(
                isPlaying = true,
                isSingleAyahMode = false,
                currentPositionMs = targetMs,
                activeVerseKey = "$surahNumber:${entry.ayahNumber}",
                syncedAyahNumber = entry.ayahNumber,
                syncedWordIndex = resolvedWord
            )
        }
        startProgressTicker()
        return true
    }

    private fun launchSurahPlayback(
        surahNumber: Int,
        surahName: String,
        startAyahNumber: Int?,
        startWordIndex: Int?,
        singleAyahOnly: Boolean,
        repeatMode: AyahRepeatMode,
        onSingleAyahFallbackRequired: (() -> Unit)?
    ) {
        val reciter = _state.value.selectedReciter
        val targetKey = "${reciter.id}|${reciter.serverUrl}|$surahNumber"
        val token = ++sessionToken
        prepareFlowJob?.cancel()
        releasePlayerOnly()
        preparedSurahKey = null
        activeTimingMap = null
        clearSingleAyahBounds()
        updateHighlightFlows(null, null, null)

        _state.update {
            it.copy(
                loadedSurahNumber = surahNumber,
                loadedSurahName = surahName,
                isBuffering = true,
                isPlaying = false,
                isSingleAyahMode = singleAyahOnly,
                currentPositionMs = 0,
                durationMs = 0,
                activeStreamUrl = null,
                usedFallbackStream = false,
                errorMessage = null,
                activeVerseKey = null,
                syncedAyahNumber = null,
                syncedWordIndex = null,
                hasOfficialTimings = false,
                hasWordTimings = false
            )
        }

        prepareFlowJob = scope.launch {
            // 0. STRICT OFFLINE-FIRST PRIORITY: Always check local downloaded MP3 BEFORE any network call!
            val localOfflineAudio = appContext?.let { ctx ->
                com.example.data.local.PersistentOfflineStorage
                    .getOfflineSurahAudioFile(ctx, reciter.id, surahNumber)
                    .takeIf { it.exists() && it.isFile && it.length() > 1_024L }
                    ?.absolutePath
            }
            if (token != sessionToken) return@launch

            // If local MP3 exists for full-Surah playback, NEVER block on network timings:
            // read ONLY local disk cached timings (zero latency) and play local file immediately!
            val officialTimings = if (localOfflineAudio != null && !singleAyahOnly && startAyahNumber == null) {
                QuranTimingRepository.getCachedSurahTimingsLocalOnly(appContext, reciter, surahNumber)
            } else {
                QuranTimingRepository.getCachedSurahTimingsLocalOnly(appContext, reciter, surahNumber)
                    ?: QuranTimingRepository.fetchSurahTimings(appContext, reciter, surahNumber)
            }
            if (token != sessionToken) return@launch

            if (officialTimings != null) {
                val startEntry = startAyahNumber?.let { officialTimings.findAyahEntry(it) }
                if (singleAyahOnly && startEntry == null) {
                    abortSingleAyahToPerVerseFallback(onSingleAyahFallbackRequired)
                    return@launch
                }

                activeTimingMap = officialTimings
                val initialSeekMs = if (startEntry != null) {
                    if (startWordIndex != null) {
                        startEntry.findWordStartMs(startWordIndex) ?: startEntry.startMs
                    } else {
                        startEntry.startMs
                    }
                } else 0

                if (startEntry != null) {
                    configureSingleAyahBounds(
                        singleAyahOnly = singleAyahOnly,
                        startMs = startEntry.startMs,
                        endMs = startEntry.endMs,
                        repeatMode = repeatMode,
                        fallbackCallback = onSingleAyahFallbackRequired
                    )
                }

                val initialAyah = startEntry?.ayahNumber
                val initialWord = if (_state.value.wordByWordEnabled && officialTimings.hasWordSegments && startEntry != null) {
                    startWordIndex ?: startEntry.resolveWordAt(initialSeekMs)
                } else null
                updateHighlightFlows(surahNumber, initialAyah, initialWord)

                _state.update {
                    it.copy(
                        hasOfficialTimings = true,
                        hasWordTimings = officialTimings.hasWordSegments,
                        isSingleAyahMode = singleAyahOnly,
                        activeVerseKey = initialAyah?.let { a -> "$surahNumber:$a" },
                        syncedAyahNumber = initialAyah,
                        syncedWordIndex = initialWord
                    )
                }

                // Strict offline-first: local file is ALWAYS index 0 when present!
                val fallbackUrls = if (singleAyahOnly) emptyList() else reciter.buildCandidateAudioUrls(surahNumber)
                val allUrls = buildList {
                    if (localOfflineAudio != null) add(localOfflineAudio)
                    add(officialTimings.quranComAudioUrl)
                    fallbackUrls.forEach { if (it != officialTimings.quranComAudioUrl) add(it) }
                }
                tryStreamIndex(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    targetKey = targetKey,
                    urls = allUrls,
                    index = 0,
                    token = token,
                    isOfficialFirstUrl = true,
                    initialSeekMs = initialSeekMs
                )
            } else {
                if (singleAyahOnly) {
                    abortSingleAyahToPerVerseFallback(onSingleAyahFallbackRequired)
                    return@launch
                }

                // Play local offline MP3 FIRST with zero internet requirement; only fall back to streaming if not downloaded
                activeTimingMap = null
                updateHighlightFlows(null, null, null)
                val urls = buildList {
                    if (localOfflineAudio != null) add(localOfflineAudio)
                    addAll(reciter.buildCandidateAudioUrls(surahNumber))
                }
                tryStreamIndex(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    targetKey = targetKey,
                    urls = urls,
                    index = 0,
                    token = token,
                    isOfficialFirstUrl = false,
                    initialSeekMs = 0
                )
            }
        }
    }

    private fun abortSingleAyahToPerVerseFallback(callback: (() -> Unit)?) {
        releasePlayerOnly()
        activeTimingMap = null
        preparedSurahKey = null
        clearSingleAyahBounds()
        updateHighlightFlows(null, null, null)
        _state.update {
            it.copy(
                isBuffering = false,
                isPlaying = false,
                isSingleAyahMode = false,
                hasOfficialTimings = false,
                hasWordTimings = false,
                activeVerseKey = null,
                syncedAyahNumber = null,
                syncedWordIndex = null,
                errorMessage = null
            )
        }
        callback?.invoke()
    }

    private fun tryStreamIndex(
        surahNumber: Int,
        surahName: String,
        targetKey: String,
        urls: List<String>,
        index: Int,
        token: Long,
        isOfficialFirstUrl: Boolean,
        initialSeekMs: Int
    ) {
        if (token != sessionToken) return
        if (index >= urls.size) {
            val fallbackCb = if (playSingleAyahOnly) singleAyahFallbackCallback else null
            if (fallbackCb != null) {
                abortSingleAyahToPerVerseFallback(fallbackCb)
                return
            }
            releasePlayerOnly()
            activeTimingMap = null
            clearSingleAyahBounds()
            updateHighlightFlows(null, null, null)
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    isSingleAyahMode = false,
                    hasOfficialTimings = false,
                    hasWordTimings = false,
                    activeVerseKey = null,
                    syncedAyahNumber = null,
                    syncedWordIndex = null,
                    errorMessage = "تعذّر تشغيل التلاوة حالياً، تأكد من الاتصال بالإنترنت أو جرّب قارئاً آخر"
                )
            }
            return
        }

        val url = urls[index]
        val isLocalFile = url.startsWith("/")
        val isFallback = index > 0 && !isLocalFile
        val usingOfficialAudio = isOfficialFirstUrl && (
            index == 0 || url == activeTimingMap?.quranComAudioUrl
        )

        if (!usingOfficialAudio) {
            if (playSingleAyahOnly) {
                val cb = singleAyahFallbackCallback
                abortSingleAyahToPerVerseFallback(cb)
                return
            }
            activeTimingMap = null
            clearSingleAyahBounds()
            updateHighlightFlows(null, null, null)
        }

        releasePlayerOnly()

        _state.update {
            it.copy(
                isBuffering = !isLocalFile,
                activeStreamUrl = url,
                isPlayingOffline = isLocalFile,
                usedFallbackStream = isFallback,
                hasOfficialTimings = usingOfficialAudio && activeTimingMap != null,
                hasWordTimings = usingOfficialAudio && (activeTimingMap?.hasWordSegments == true),
                activeVerseKey = if (usingOfficialAudio) it.activeVerseKey else null,
                syncedAyahNumber = if (usingOfficialAudio) it.syncedAyahNumber else null,
                syncedWordIndex = if (usingOfficialAudio) it.syncedWordIndex else null,
                errorMessage = null
            )
        }

        if (!isLocalFile) {
            timeoutJob = scope.launch(Dispatchers.Main) {
                delay(10_000L)
                if (token == sessionToken && _state.value.isBuffering) {
                    tryStreamIndex(
                        surahNumber = surahNumber,
                        surahName = surahName,
                        targetKey = targetKey,
                        urls = urls,
                        index = index + 1,
                        token = token,
                        isOfficialFirstUrl = isOfficialFirstUrl,
                        initialSeekMs = initialSeekMs
                    )
                }
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
                if (isLocalFile) {
                    java.io.FileInputStream(url).use { fis ->
                        setDataSource(fis.fd)
                    }
                } else {
                    setDataSource(url)
                }
                setOnPreparedListener { player ->
                    if (token != sessionToken) {
                        runCatching { player.release() }
                        return@setOnPreparedListener
                    }
                    timeoutJob?.cancel()
                    preparedSurahKey = targetKey
                    val dur = player.duration.coerceAtLeast(0)
                    val safeSeek = if (usingOfficialAudio && initialSeekMs > 0 && (dur == 0 || initialSeekMs < dur)) {
                        initialSeekMs
                    } else 0
                    if (safeSeek > 0) {
                        seekPrecise(player, safeSeek)
                    }
                    player.start()
                    applySpeedToPlayer(player, _state.value.playbackSpeed)

                    val timing = activeTimingMap
                    val resolved = if (usingOfficialAudio && timing != null) {
                        val eff = (safeSeek - _state.value.activeLatencyOffsetMs).coerceAtLeast(0)
                        timing.resolveAtPosition(eff)
                    } else null

                    val nextAyah = resolved?.first ?: if (usingOfficialAudio) _state.value.syncedAyahNumber else null
                    val nextWord = if (usingOfficialAudio && _state.value.wordByWordEnabled && timing?.hasWordSegments == true) {
                        resolved?.second ?: _state.value.syncedWordIndex
                    } else null
                    updateHighlightFlows(if (usingOfficialAudio) surahNumber else null, nextAyah, nextWord)

                    _state.update {
                        it.copy(
                            loadedSurahNumber = surahNumber,
                            loadedSurahName = surahName,
                            isBuffering = false,
                            isPlaying = true,
                            isPlayingOffline = isLocalFile,
                            isSingleAyahMode = playSingleAyahOnly,
                            durationMs = dur,
                            currentPositionMs = safeSeek,
                            activeStreamUrl = url,
                            usedFallbackStream = isFallback,
                            hasOfficialTimings = usingOfficialAudio && timing != null,
                            hasWordTimings = usingOfficialAudio && (timing?.hasWordSegments == true),
                            activeVerseKey = if (usingOfficialAudio && nextAyah != null) "$surahNumber:$nextAyah" else null,
                            syncedAyahNumber = nextAyah,
                            syncedWordIndex = nextWord,
                            errorMessage = null
                        )
                    }
                    startProgressTicker()
                }
                setOnCompletionListener {
                    if (token != sessionToken) return@setOnCompletionListener
                    stopProgressTicker()
                    if (playSingleAyahOnly && stopAtMs != null && handleSingleAyahBoundaryReached(this)) {
                        return@setOnCompletionListener
                    }
                    clearSingleAyahBounds()
                    updateHighlightFlows(null, null, null)
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isSingleAyahMode = false,
                            currentPositionMs = 0,
                            activeVerseKey = null,
                            syncedAyahNumber = null,
                            syncedWordIndex = null
                        )
                    }
                    onSurahCompleteCallback?.invoke(surahNumber)
                }
                setOnErrorListener { _, _, _ ->
                    if (token != sessionToken) return@setOnErrorListener true
                    timeoutJob?.cancel()
                    tryStreamIndex(
                        surahNumber = surahNumber,
                        surahName = surahName,
                        targetKey = targetKey,
                        urls = urls,
                        index = index + 1,
                        token = token,
                        isOfficialFirstUrl = isOfficialFirstUrl,
                        initialSeekMs = initialSeekMs
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            timeoutJob?.cancel()
            tryStreamIndex(
                surahNumber = surahNumber,
                surahName = surahName,
                targetKey = targetKey,
                urls = urls,
                index = index + 1,
                token = token,
                isOfficialFirstUrl = isOfficialFirstUrl,
                initialSeekMs = initialSeekMs
            )
        }
    }

    fun seekToFraction(fraction: Float) {
        val mp = mediaPlayer ?: return
        val duration = _state.value.durationMs
        if (duration > 0) {
            clearSingleAyahBounds()
            val targetMs = (fraction.coerceIn(0f, 1f) * duration).toInt()
            seekPrecise(mp, targetMs)
            val timing = activeTimingMap
            val current = _state.value
            val resolved = if (timing != null && current.hasOfficialTimings) {
                val effectiveMs = (targetMs - current.activeLatencyOffsetMs).coerceAtLeast(0)
                timing.resolveAtPosition(effectiveMs)
            } else null

            val nextAyah = resolved?.first
            val nextWord = if (current.wordByWordEnabled && timing?.hasWordSegments == true) {
                resolved?.second
            } else null
            updateHighlightFlows(current.loadedSurahNumber, nextAyah, nextWord)

            _state.update {
                it.copy(
                    currentPositionMs = targetMs,
                    isSingleAyahMode = false,
                    activeVerseKey = if (it.loadedSurahNumber != null && nextAyah != null) {
                        "${it.loadedSurahNumber}:$nextAyah"
                    } else null,
                    syncedAyahNumber = nextAyah,
                    syncedWordIndex = nextWord
                )
            }
        }
    }

    fun dismissAndStop() {
        sessionToken++
        prepareFlowJob?.cancel()
        prepareFlowJob = null
        activeTimingMap = null
        clearSingleAyahBounds()
        releasePlayerOnly()
        preparedSurahKey = null
        updateHighlightFlows(null, null, null)
        val cur = _state.value
        _state.value = AudioPlayerState(
            selectedReciter = cur.selectedReciter,
            playbackSpeed = cur.playbackSpeed,
            wordByWordEnabled = cur.wordByWordEnabled,
            audioOutputRoute = cur.audioOutputRoute,
            bluetoothOffsetMs = cur.bluetoothOffsetMs,
            speakerOffsetMs = cur.speakerOffsetMs
        )
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
        prepareFlowJob?.cancel()
        prepareFlowJob = null
        activeTimingMap = null
        clearSingleAyahBounds()
        releasePlayerOnly()
        preparedSurahKey = null
        updateHighlightFlows(null, null, null)
        val cur = _state.value
        _state.value = AudioPlayerState(
            selectedReciter = if (keepReciter) cur.selectedReciter else OfflineIslamicData.reciters.first(),
            playbackSpeed = cur.playbackSpeed,
            wordByWordEnabled = cur.wordByWordEnabled,
            audioOutputRoute = cur.audioOutputRoute,
            bluetoothOffsetMs = cur.bluetoothOffsetMs,
            speakerOffsetMs = cur.speakerOffsetMs
        )
    }

    private fun configureSingleAyahBounds(
        singleAyahOnly: Boolean,
        startMs: Int,
        endMs: Int,
        repeatMode: AyahRepeatMode,
        fallbackCallback: (() -> Unit)? = null
    ) {
        if (!singleAyahOnly) {
            clearSingleAyahBounds()
            return
        }
        playSingleAyahOnly = true
        singleAyahStartMs = startMs
        stopAtMs = endMs
        singleAyahRepeatMode = repeatMode
        singleAyahFallbackCallback = fallbackCallback
        singleAyahRemainingRepeats = when (repeatMode) {
            AyahRepeatMode.ONCE -> 0
            AyahRepeatMode.THREE -> 2
            AyahRepeatMode.FIVE -> 4
            AyahRepeatMode.CONTINUOUS -> Int.MAX_VALUE
        }
    }

    private fun clearSingleAyahBounds() {
        playSingleAyahOnly = false
        singleAyahStartMs = null
        stopAtMs = null
        singleAyahRepeatMode = AyahRepeatMode.ONCE
        singleAyahRemainingRepeats = 0
        singleAyahSeekGraceUntilMs = 0L
        singleAyahFallbackCallback = null
    }

    private fun handleSingleAyahBoundaryReached(mp: MediaPlayer): Boolean {
        val startMs = singleAyahStartMs ?: return false
        if (singleAyahRepeatMode == AyahRepeatMode.CONTINUOUS) {
            seekPrecise(mp, startMs)
            runCatching { if (!mp.isPlaying) mp.start() }
            return true
        }
        if (singleAyahRemainingRepeats > 0) {
            singleAyahRemainingRepeats--
            seekPrecise(mp, startMs)
            runCatching { if (!mp.isPlaying) mp.start() }
            return true
        }
        // Finished single Ayah cleanly -> immediately pause & clear highlight
        clearSingleAyahBounds()
        runCatching { if (mp.isPlaying) mp.pause() }
        stopProgressTicker()
        updateHighlightFlows(null, null, null)
        _state.update {
            it.copy(
                isPlaying = false,
                isSingleAyahMode = false,
                activeVerseKey = null,
                syncedAyahNumber = null,
                syncedWordIndex = null
            )
        }
        return false
    }

    private fun applyResolvedHighlightForPosition(posMs: Int) {
        val cur = _state.value
        val timing = activeTimingMap
        if (timing == null || !cur.hasOfficialTimings) return
        val effectiveMs = (posMs - cur.activeLatencyOffsetMs).coerceAtLeast(0)
        val resolved = timing.resolveAtPosition(effectiveMs)
        val nextAyah = resolved?.first
        val nextWord = if (cur.wordByWordEnabled && timing.hasWordSegments) resolved?.second else null
        if (nextAyah != cur.syncedAyahNumber || nextWord != cur.syncedWordIndex) {
            updateHighlightFlows(cur.loadedSurahNumber, nextAyah, nextWord)
            _state.update {
                it.copy(
                    currentPositionMs = posMs,
                    activeVerseKey = if (it.loadedSurahNumber != null && nextAyah != null) {
                        "${it.loadedSurahNumber}:$nextAyah"
                    } else null,
                    syncedAyahNumber = nextAyah,
                    syncedWordIndex = nextWord
                )
            }
        }
    }

    private fun releasePlayerOnly() {
        stopProgressTicker()
        timeoutJob?.cancel()
        timeoutJob = null
        val oldPlayer = mediaPlayer
        mediaPlayer = null
        if (oldPlayer != null) {
            runCatching {
                oldPlayer.setOnPreparedListener(null)
                oldPlayer.setOnCompletionListener(null)
                oldPlayer.setOnErrorListener(null)
                if (oldPlayer.isPlaying) oldPlayer.stop()
                oldPlayer.reset()
                oldPlayer.release()
            }
        }
    }

    /**
     * Lightweight 50ms sync loop running ONLY while audio is actively playing (paused immediately
     * on pause/buffering/stop to conserve battery & RAM).
     *
     * Emits UI state updates ONLY when the active Ayah or Word actually changes, or every ~250ms
     * for the seek bar — guaranteeing zero unnecessary full-page recompositions on 2GB RAM devices.
     */
    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch(Dispatchers.Main) {
            var tickCount = 0
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && runCatching { mp.isPlaying }.getOrDefault(false)) {
                    val posMs = runCatching { mp.currentPosition }.getOrDefault(_state.value.currentPositionMs)
                    val cur = _state.value
                    val effectiveMs = (posMs - cur.activeLatencyOffsetMs).coerceAtLeast(0)

                    // Check exact single-Ayah end boundary when playing a single Ayah only
                    val stopBoundary = stopAtMs
                    if (playSingleAyahOnly &&
                        stopBoundary != null &&
                        SystemClock.elapsedRealtime() >= singleAyahSeekGraceUntilMs &&
                        effectiveMs >= stopBoundary
                    ) {
                        val looped = handleSingleAyahBoundaryReached(mp)
                        if (!looped) break
                        delay(50L)
                        continue
                    }

                    val timing = activeTimingMap
                    val resolved = if (timing != null && cur.hasOfficialTimings) {
                        timing.resolveAtPosition(effectiveMs)
                    } else null

                    val nextAyah = if (timing != null && cur.hasOfficialTimings) resolved?.first else null
                    val nextWord = if (timing != null && cur.hasOfficialTimings && cur.wordByWordEnabled && timing.hasWordSegments) {
                        resolved?.second
                    } else null

                    val highlightChanged = nextAyah != cur.syncedAyahNumber || nextWord != cur.syncedWordIndex
                    val shouldUpdateSeekBar = (tickCount % 5 == 0) // every 250ms for smooth seekbar without spamming Compose

                    if (highlightChanged) {
                        updateHighlightFlows(cur.loadedSurahNumber, nextAyah, nextWord)
                    }

                    if (highlightChanged || shouldUpdateSeekBar) {
                        val durMs = if (shouldUpdateSeekBar) {
                            runCatching { mp.duration }.getOrDefault(cur.durationMs).coerceAtLeast(0)
                        } else {
                            cur.durationMs
                        }
                        val nextKey = if (cur.loadedSurahNumber != null && nextAyah != null) {
                            "${cur.loadedSurahNumber}:$nextAyah"
                        } else null
                        _state.update {
                            it.copy(
                                currentPositionMs = posMs,
                                durationMs = durMs,
                                activeVerseKey = nextKey,
                                syncedAyahNumber = nextAyah,
                                syncedWordIndex = nextWord
                            )
                        }
                    }
                    tickCount++
                }
                delay(50L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
