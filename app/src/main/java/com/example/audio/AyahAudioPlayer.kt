package com.example.audio

import android.media.AudioAttributes
import android.media.MediaPlayer
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AyahItem
import com.example.data.model.AyahRepeatMode
import com.example.data.model.ReciterOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AyahAudioState(
    val surahNumber: Int? = null,
    val ayahNumber: Int? = null,
    val globalAyahNumber: Int? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val repeatMode: AyahRepeatMode = AyahRepeatMode.ONCE,
    val currentRepeatIndex: Int = 1,
    val continueReading: Boolean = false,
    val activeReciterName: String = "",
    val fallbackNote: String? = null,
    val errorMessage: String? = null
)

object EveryAyahMapper {

    fun resolveFolderForReciter(reciter: ReciterOption): String? {
        // Match by featured ID first
        when (reciter.id) {
            "featured_yasser_dosari" -> return "Yasser_Ad-Dussary_128kbps"
            "featured_alafasy" -> return "Alafasy_128kbps"
            "featured_maher" -> return "MaherAlMuaiqly128kbps"
            "featured_basit_murattal" -> return "Abdul_Basit_Murattal_192kbps"
            "featured_basit_mujawwad" -> return "Abdul_Basit_Murattal_192kbps"
            "featured_minshawi" -> return "Minshawy_Murattal_128kbps"
            "featured_husary" -> return "Husary_128kbps"
            "featured_qatami" -> return "Nasser_Alqatami_128kbps"
            "featured_sudais" -> return "Abdurrahmaan_As-Sudais_192kbps"
            "featured_ghamdi" -> return "Ghamadi_40kbps"
        }

        // Also match by normalized Arabic name (supports reciters chosen from MP3Quran API search)
        val norm = OfflineIslamicData.normalizeArabic(reciter.arabicName)
        return when {
            norm.contains("ياسر الدوسري") -> "Yasser_Ad-Dussary_128kbps"
            norm.contains("العفاسي") -> "Alafasy_128kbps"
            norm.contains("ماهر المعيقلي") -> "MaherAlMuaiqly128kbps"
            norm.contains("عبد الباسط") || norm.contains("عبدالباسط") -> "Abdul_Basit_Murattal_192kbps"
            norm.contains("المنشاوي") -> "Minshawy_Murattal_128kbps"
            norm.contains("الحصري") -> "Husary_128kbps"
            norm.contains("القطامي") -> "Nasser_Alqatami_128kbps"
            norm.contains("السديس") -> "Abdurrahmaan_As-Sudais_192kbps"
            norm.contains("الغامدي") -> "Ghamadi_40kbps"
            else -> null
        }
    }

    fun buildEveryAyahUrl(folder: String, surahNumber: Int, ayahNumber: Int): String {
        val sss = surahNumber.toString().padStart(3, '0')
        val aaa = ayahNumber.toString().padStart(3, '0')
        return "https://everyayah.com/data/$folder/$sss$aaa.mp3"
    }

    fun buildAlafasyGlobalFallbackUrl(globalAyahNumber: Int, surahNumber: Int, ayahNumber: Int): String {
        return if (globalAyahNumber > 0) {
            "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$globalAyahNumber.mp3"
        } else {
            buildEveryAyahUrl("Alafasy_128kbps", surahNumber, ayahNumber)
        }
    }
}

class AyahAudioPlayer(private val scope: CoroutineScope) {

    private var mediaPlayer: MediaPlayer? = null
    private var timeoutJob: Job? = null
    private var sessionToken: Long = 0L
    private var preparedKey: String? = null
    private var onAdvanceCallback: ((Int) -> Unit)? = null

    private val _state = MutableStateFlow(AyahAudioState())
    val state: StateFlow<AyahAudioState> = _state.asStateFlow()

    fun setRepeatMode(mode: AyahRepeatMode) {
        _state.update { it.copy(repeatMode = mode, currentRepeatIndex = 1) }
    }

    fun setContinueReading(enabled: Boolean) {
        _state.update { it.copy(continueReading = enabled) }
    }

    fun togglePlayAyah(
        surahNumber: Int,
        ayah: AyahItem,
        reciter: ReciterOption,
        onAdvanceToNextAyah: (nextAyahNumber: Int) -> Unit
    ) {
        onAdvanceCallback = onAdvanceToNextAyah
        val key = "${reciter.id}|$surahNumber|${ayah.numberInSurah}"
        val current = _state.value
        val mp = mediaPlayer

        if (mp != null && preparedKey == key && !current.isBuffering) {
            if (runCatching { mp.isPlaying }.getOrDefault(false)) {
                runCatching { mp.pause() }
                _state.update { it.copy(isPlaying = false) }
            } else {
                runCatching { mp.start() }
                _state.update { it.copy(isPlaying = true, errorMessage = null) }
            }
            return
        }

        playAyah(surahNumber, ayah, reciter, onAdvanceToNextAyah)
    }

    fun playAyah(
        surahNumber: Int,
        ayah: AyahItem,
        reciter: ReciterOption,
        onAdvanceToNextAyah: (nextAyahNumber: Int) -> Unit
    ) {
        onAdvanceCallback = onAdvanceToNextAyah
        val token = ++sessionToken
        releasePlayerOnly()
        preparedKey = null

        val folder = EveryAyahMapper.resolveFolderForReciter(reciter)
        val fallbackUrl = EveryAyahMapper.buildAlafasyGlobalFallbackUrl(
            globalAyahNumber = ayah.globalNumber,
            surahNumber = surahNumber,
            ayahNumber = ayah.numberInSurah
        )
        val candidateUrls = buildList {
            if (folder != null) {
                add(EveryAyahMapper.buildEveryAyahUrl(folder, surahNumber, ayah.numberInSurah))
            }
            add(fallbackUrl)
            add(EveryAyahMapper.buildEveryAyahUrl("Alafasy_128kbps", surahNumber, ayah.numberInSurah))
        }.distinct()

        val initialFallbackNote = if (folder == null) {
            "القارئ المختار متاح لتلاوة السور الكاملة؛ يتم تشغيل تلاوة الآية بصوت الشيخ مشاري العفاسي"
        } else {
            null
        }

        val targetKey = "${reciter.id}|$surahNumber|${ayah.numberInSurah}"

        _state.update {
            it.copy(
                surahNumber = surahNumber,
                ayahNumber = ayah.numberInSurah,
                globalAyahNumber = ayah.globalNumber,
                isPlaying = false,
                isBuffering = true,
                currentRepeatIndex = 1,
                activeReciterName = if (folder != null) reciter.arabicName else "مشاري راشد العفاسي (بديل)",
                fallbackNote = initialFallbackNote,
                errorMessage = null
            )
        }

        tryPlayCandidate(
            surahNumber = surahNumber,
            ayahNumber = ayah.numberInSurah,
            targetKey = targetKey,
            urls = candidateUrls,
            index = 0,
            hadDirectFolder = folder != null,
            token = token
        )
    }

    private fun tryPlayCandidate(
        surahNumber: Int,
        ayahNumber: Int,
        targetKey: String,
        urls: List<String>,
        index: Int,
        hadDirectFolder: Boolean,
        token: Long
    ) {
        if (token != sessionToken) return
        if (index >= urls.size) {
            releasePlayerOnly()
            _state.update {
                it.copy(
                    isBuffering = false,
                    isPlaying = false,
                    errorMessage = "تعذّر تحميل صوت الآية، يرجى التأكد من الاتصال بالإنترنت"
                )
            }
            return
        }

        releasePlayerOnly()
        val url = urls[index]
        val fellBackFromPrimary = hadDirectFolder && index > 0

        timeoutJob = scope.launch(Dispatchers.Main) {
            delay(8_500L)
            if (token == sessionToken && _state.value.isBuffering) {
                tryPlayCandidate(surahNumber, ayahNumber, targetKey, urls, index + 1, hadDirectFolder, token)
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
                    preparedKey = targetKey
                    player.start()
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            isPlaying = true,
                            fallbackNote = if (fellBackFromPrimary) {
                                "تم استخدام بث بديل للآية بصوت الشيخ مشاري العفاسي"
                            } else {
                                it.fallbackNote
                            },
                            errorMessage = null
                        )
                    }
                }
                setOnCompletionListener { player ->
                    if (token != sessionToken) return@setOnCompletionListener
                    val current = _state.value
                    val shouldRepeat = when (current.repeatMode) {
                        AyahRepeatMode.CONTINUOUS -> true
                        else -> current.currentRepeatIndex < current.repeatMode.repeatCount
                    }
                    if (shouldRepeat) {
                        _state.update { it.copy(currentRepeatIndex = it.currentRepeatIndex + 1, isPlaying = true) }
                        runCatching {
                            player.seekTo(0)
                            player.start()
                        }
                    } else if (current.continueReading) {
                        _state.update { it.copy(isPlaying = false) }
                        onAdvanceCallback?.invoke(ayahNumber + 1)
                    } else {
                        _state.update { it.copy(isPlaying = false, currentRepeatIndex = 1) }
                    }
                }
                setOnErrorListener { _, _, _ ->
                    if (token != sessionToken) return@setOnErrorListener true
                    timeoutJob?.cancel()
                    tryPlayCandidate(surahNumber, ayahNumber, targetKey, urls, index + 1, hadDirectFolder, token)
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            timeoutJob?.cancel()
            tryPlayCandidate(surahNumber, ayahNumber, targetKey, urls, index + 1, hadDirectFolder, token)
        }
    }

    fun stop() {
        sessionToken++
        releasePlayerOnly()
        preparedKey = null
        val repeat = _state.value.repeatMode
        val cont = _state.value.continueReading
        _state.value = AyahAudioState(repeatMode = repeat, continueReading = cont)
    }

    private fun releasePlayerOnly() {
        timeoutJob?.cancel()
        timeoutJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        }
        mediaPlayer = null
    }
}
