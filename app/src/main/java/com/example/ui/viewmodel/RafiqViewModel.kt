package com.example.ui.viewmodel

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AdhanAudioPlayer
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioPlayer
import com.example.audio.AyahAudioState
import com.example.audio.FajrAlarmPlayer
import com.example.audio.QuranAudioPlayer
import com.example.data.local.AppPreferences
import com.example.data.local.HadithLibraryData
import com.example.data.local.MuezzinLibraryData
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AdhanPlayerState
import com.example.data.model.AyahItem
import com.example.data.model.AyahRepeatMode
import com.example.data.model.CalculatedWakeupTime
import com.example.data.model.CityOption
import com.example.data.model.DailyQuote
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.FajrTimeCalculator
import com.example.data.model.HadithBookCollection
import com.example.data.model.HadithChapters
import com.example.data.model.LibraryHadith
import com.example.data.model.MoshafOption
import com.example.data.model.MuezzinAudioOverride
import com.example.data.model.MuezzinItem
import com.example.data.model.MuezzinRegion
import com.example.data.model.MushafPaperTheme
import com.example.data.model.PrayerEntry
import com.example.data.model.PrayerTimesUiState
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranViewMode
import com.example.data.model.ReciterOption
import com.example.data.model.SurahSummary
import com.example.data.model.TasbihState
import com.example.data.model.WorshipTask
import com.example.data.remote.IslamicNetworkClient
import com.example.sensor.QiblaMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class RafiqTab(val route: String, val label: String) {
    HOME("home", "الرئيسية"),
    QURAN("quran", "القرآن"),
    AZKAR("azkar", "الأذكار"),
    HADITH("hadith", "الحديث"),
    TOOLS("tools", "المزيد")
}

data class QuranUiState(
    val surahs: List<SurahSummary> = emptyList(),
    val isLoadingList: Boolean = false,
    val searchQuery: String = "",
    val revelationFilter: String = "all",
    val activeSurah: SurahSummary? = null,
    val ayahs: List<AyahItem> = emptyList(),
    val isLoadingReader: Boolean = false,
    val readerError: String? = null,
    val viewMode: QuranViewMode = QuranViewMode.MUSHAF,
    val isFullScreenReading: Boolean = false,
    val showAllTafsir: Boolean = false,
    val expandedTafsirAyahs: Set<Int> = emptySet(),
    val targetScrollAyah: Int = 1
)

data class AzkarUiState(
    val subTabIndex: Int = 0, // 0 = Azkar, 1 = Tasbih
    val selectedCategory: String = OfflineIslamicData.azkarCategories.keys.first(),
    val remainingCounts: Map<String, Int> = emptyMap()
)

data class HadithUiState(
    val selectedBook: HadithBookCollection = HadithBookCollection.BUKHARI,
    val searchQuery: String = "",
    val searchAllBooks: Boolean = false,
    val selectedChapter: String = HadithChapters.ALL,
    val loadedCollections: Map<HadithBookCollection, List<LibraryHadith>> = mapOf(
        HadithBookCollection.BUKHARI to HadithLibraryData.bukhariOffline,
        HadithBookCollection.MUSLIM to HadithLibraryData.muslimOffline,
        HadithBookCollection.QUDSI to HadithLibraryData.qudsiOffline,
        HadithBookCollection.NAWAWI to HadithLibraryData.nawawiOffline()
    ),
    val fetchedFromApiBooks: Set<HadithBookCollection> = emptySet(),
    val isLoadingBook: Boolean = false,
    val bookLoadNotice: String? = null,
    val visibleCount: Int = 30,
    val expandedUids: Set<String> = emptySet()
)

data class ToolsUiState(
    val locationLabel: String = OfflineIslamicData.cities.first().arabicLabel,
    val qiblaAngleDeg: Float = QiblaMath.calculateQiblaBearing(
        OfflineIslamicData.cities.first().lat,
        OfflineIslamicData.cities.first().lng
    ),
    val distanceKm: Int = QiblaMath.calculateDistanceKm(
        OfflineIslamicData.cities.first().lat,
        OfflineIslamicData.cities.first().lng
    ),
    val headingDeg: Float = 0f,
    val isSensorEnabled: Boolean = false,
    val expandedGuideIds: Set<String> = setOf("wudu")
)

class RafiqViewModel(application: Application) : AndroidViewModel(application) {

    private enum class AudioSource { SURAH, AYAH, ADHAN, FAJR_ALARM }

    private val prefs = AppPreferences(application.applicationContext)
    private val audioPlayer = QuranAudioPlayer(viewModelScope)
    private val ayahAudioPlayer = AyahAudioPlayer(viewModelScope)
    private val adhanPlayer = AdhanAudioPlayer(viewModelScope)
    private val fajrAlarmPlayer = FajrAlarmPlayer(application.applicationContext, viewModelScope)

    private fun stopOtherAudio(except: AudioSource) {
        if (except != AudioSource.SURAH) audioPlayer.pauseIfPlaying()
        if (except != AudioSource.AYAH) ayahAudioPlayer.stop()
        if (except != AudioSource.ADHAN) adhanPlayer.stop()
        if (except != AudioSource.FAJR_ALARM && !_fajrAlarmRinging.value) {
            fajrAlarmPlayer.stopRinging()
        }
    }

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    val darkMode: StateFlow<Boolean> = prefs.darkModeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    val quranFontSize: StateFlow<Float> = prefs.quranFontSizeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 25f
    )

    val mushafPaperTheme: StateFlow<MushafPaperTheme> = prefs.mushafPaperThemeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MushafPaperTheme.CREAM
    )

    val fajrAlarmSettings: StateFlow<FajrAlarmSettings> = prefs.fajrAlarmSettingsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), FajrAlarmSettings()
    )

    val favoriteHadithUids: StateFlow<Set<String>> = prefs.favoriteHadithUidsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet()
    )

    val hadithFontSize: StateFlow<Float> = prefs.hadithFontSizeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 20f
    )

    val bookmark: StateFlow<QuranBookmark?> = prefs.bookmarkFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val tasbihState: StateFlow<TasbihState> = prefs.tasbihFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), TasbihState()
    )

    val audioState: StateFlow<AudioPlayerState> = audioPlayer.state
    val ayahAudioState: StateFlow<AyahAudioState> = ayahAudioPlayer.state
    val adhanState: StateFlow<AdhanPlayerState> = adhanPlayer.state

    val defaultMuezzinId: StateFlow<String> = prefs.defaultMuezzinIdFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.defaultMuezzinId
    )

    val fajrMuezzinId: StateFlow<String> = prefs.fajrMuezzinIdFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.defaultFajrMuezzinId
    )

    val useDedicatedFajrAdhan: StateFlow<Boolean> = prefs.useDedicatedFajrAdhanFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val favoriteMuezzinIds: StateFlow<Set<String>> = prefs.favoriteMuezzinIdsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), setOf(MuezzinLibraryData.defaultMuezzinId)
    )

    private val customMuezzins: StateFlow<List<MuezzinItem>> = prefs.customMuezzinsFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val muezzinAudioOverrides: StateFlow<Map<String, MuezzinAudioOverride>> =
        prefs.muezzinAudioOverridesFlow.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap()
        )

    val allMuezzins: StateFlow<List<MuezzinItem>> = combine(
        customMuezzins,
        muezzinAudioOverrides
    ) { custom, overrides ->
        val mergedBuiltIns = MuezzinLibraryData.builtInMuezzins.map { item ->
            val ov = overrides[item.id]
            if (ov != null) {
                val hasValidLocal = !ov.localFilePath.isNullOrBlank() && File(ov.localFilePath).exists()
                val customLabel = when {
                    hasValidLocal -> ov.localFileName ?: "ملف محلي مخصص"
                    !ov.customUrl.isNullOrBlank() -> "رابط MP3 مخصص"
                    else -> null
                }
                if (hasValidLocal || !ov.customUrl.isNullOrBlank()) {
                    item.copy(
                        primaryUrl = ov.customUrl.orEmpty(),
                        fallbackUrls = emptyList(),
                        localFilePath = if (hasValidLocal) ov.localFilePath else null,
                        hasCustomAudio = true,
                        customAudioLabel = customLabel
                    )
                } else {
                    item
                }
            } else {
                item
            }
        }
        val mergedCustom = custom.map { it.copy(hasCustomAudio = true) }
        mergedCustom + mergedBuiltIns
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.builtInMuezzins
    )

    val activeDefaultMuezzin: StateFlow<MuezzinItem> = combine(
        allMuezzins,
        defaultMuezzinId
    ) { list, id ->
        list.find { it.id == id } ?: MuezzinLibraryData.builtInMuezzins.first()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.builtInMuezzins.first()
    )

    private val _muezzinLibraryOpen = MutableStateFlow(false)
    val muezzinLibraryOpen: StateFlow<Boolean> = _muezzinLibraryOpen.asStateFlow()

    // Smart Fajr Alarm UI State
    private val _fajrAlarmSettingsOpen = MutableStateFlow(false)
    val fajrAlarmSettingsOpen: StateFlow<Boolean> = _fajrAlarmSettingsOpen.asStateFlow()

    private val _fajrAlarmRinging = MutableStateFlow(false)
    val fajrAlarmRinging: StateFlow<Boolean> = _fajrAlarmRinging.asStateFlow()

    private var lastAutoTriggeredAlarmDate: String? = null

    val activeFajrMuezzin: StateFlow<MuezzinItem> = combine(
        allMuezzins,
        fajrMuezzinId
    ) { list, id ->
        list.find { it.id == id }
            ?: list.find { it.isFajr }
            ?: MuezzinLibraryData.builtInMuezzins.first()
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.builtInMuezzins.first()
    )

    // MP3Quran Reciter Library State
    private val _reciterLibraryOpen = MutableStateFlow(false)
    val reciterLibraryOpen: StateFlow<Boolean> = _reciterLibraryOpen.asStateFlow()

    private val _apiReciters = MutableStateFlow<List<ReciterOption>>(emptyList())
    val apiReciters: StateFlow<List<ReciterOption>> = _apiReciters.asStateFlow()

    private val _isRecitersLoading = MutableStateFlow(false)
    val isRecitersLoading: StateFlow<Boolean> = _isRecitersLoading.asStateFlow()

    private val _recitersError = MutableStateFlow<String?>(null)
    val recitersError: StateFlow<String?> = _recitersError.asStateFlow()

    private var recitersRequested = false

    private val _selectedTab = MutableStateFlow(RafiqTab.HOME)
    val selectedTab: StateFlow<RafiqTab> = _selectedTab.asStateFlow()

    private val _selectedCity = MutableStateFlow(OfflineIslamicData.cities.first())
    val selectedCity: StateFlow<CityOption> = _selectedCity.asStateFlow()

    private val _prayerState = MutableStateFlow(PrayerTimesUiState())
    val prayerState: StateFlow<PrayerTimesUiState> = _prayerState.asStateFlow()

    private val _worshipTasks = MutableStateFlow(OfflineIslamicData.worshipTasks)
    val worshipTasks: StateFlow<List<WorshipTask>> = _worshipTasks.asStateFlow()

    private val _inspirationIndex = MutableStateFlow(
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
    )
    val inspirationIndex: StateFlow<Int> = _inspirationIndex.asStateFlow()

    private val _quranState = MutableStateFlow(QuranUiState())
    val quranState: StateFlow<QuranUiState> = _quranState.asStateFlow()

    private val _azkarState = MutableStateFlow(AzkarUiState())
    val azkarState: StateFlow<AzkarUiState> = _azkarState.asStateFlow()

    private val _hadithState = MutableStateFlow(HadithUiState())
    val hadithState: StateFlow<HadithUiState> = _hadithState.asStateFlow()

    private val _toolsState = MutableStateFlow(ToolsUiState())
    val toolsState: StateFlow<ToolsUiState> = _toolsState.asStateFlow()

    private var currentTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var countdownJob: Job? = null
    private var surahListRequested = false

    private val prayerMeta = listOf(
        "Fajr" to "الفجر",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء"
    )

    init {
        viewModelScope.launch {
            val savedCityId = prefs.selectedCityIdFlow.first()
            val city = OfflineIslamicData.cities.find { it.id == savedCityId }
                ?: OfflineIslamicData.cities.first()
            _selectedCity.value = city
            updateQiblaLocation(city.lat, city.lng, city.arabicLabel)
            fetchPrayerTimes(city, manual = false)
        }
        viewModelScope.launch {
            val doneIds = prefs.getTrackerDoneIds(todayKey())
            _worshipTasks.value = OfflineIslamicData.worshipTasks.map {
                it.copy(completed = doneIds.contains(it.id))
            }
        }
        viewModelScope.launch {
            val savedAzkar = prefs.getAzkarRemaining(todayKey())
            val defaults = OfflineIslamicData.azkarCategories.values.flatten()
                .associate { it.id to (savedAzkar[it.id] ?: it.requiredCount) }
            _azkarState.update { it.copy(remainingCounts = defaults) }
        }
        viewModelScope.launch {
            val persistedReciter = prefs.selectedQuranReciterFlow.first()
            audioPlayer.syncPersistedReciter(persistedReciter)
        }
    }

    val fajrWakeupTime: StateFlow<CalculatedWakeupTime> = combine(
        _prayerState,
        fajrAlarmSettings
    ) { pState, alarm ->
        val fajr24 = pState.prayers.find { it.key == "Fajr" }?.time24
            ?: currentTimings24["Fajr"]
            ?: "04:55"
        FajrTimeCalculator.calculateWakeupTime(fajr24, alarm.offsetMinutesBeforeFajr)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        FajrTimeCalculator.calculateWakeupTime("04:55", 15)
    )

    fun selectTab(tab: RafiqTab) {
        _selectedTab.value = tab
        if (tab == RafiqTab.QURAN) {
            if (!surahListRequested) loadSurahList()
            if (!recitersRequested) loadApiReciters()
        } else if (tab == RafiqTab.HADITH) {
            val activeBook = _hadithState.value.selectedBook
            if (!_hadithState.value.fetchedFromApiBooks.contains(activeBook)) {
                fetchHadithCollection(activeBook)
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            prefs.setDarkMode(!darkMode.value)
        }
    }

    fun emitToast(message: String) {
        _toastEvents.tryEmit(message)
    }

    // =========================================================================
    // MODULE A: HOME — PRAYER TIMES, TRACKER, DAILY INSPIRATION
    // =========================================================================

    fun selectCity(city: CityOption) {
        _selectedCity.value = city
        viewModelScope.launch { prefs.setSelectedCityId(city.id) }
        updateQiblaLocation(city.lat, city.lng, city.arabicLabel)
        fetchPrayerTimes(city, manual = true)
    }

    fun refreshPrayerTimes() {
        fetchPrayerTimes(_selectedCity.value, manual = true)
    }

    private fun fetchPrayerTimes(city: CityOption, manual: Boolean) {
        viewModelScope.launch {
            val gregStr = formatGregorianArabic()
            _prayerState.update {
                it.copy(
                    isLoading = true,
                    gregorianDate = gregStr,
                    statusNote = "جاري تحديث مواقيت ${city.arabicLabel}..."
                )
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.alAdhanApi.getTimingsByCity(
                        city = city.city,
                        country = city.country,
                        method = city.method
                    )
                }
                val t = response.data.timings
                currentTimings24 = mapOf(
                    "Fajr" to cleanTime(t.Fajr),
                    "Dhuhr" to cleanTime(t.Dhuhr),
                    "Asr" to cleanTime(t.Asr),
                    "Maghrib" to cleanTime(t.Maghrib),
                    "Isha" to cleanTime(t.Isha)
                )
                val h = response.data.date.hijri
                val hijriText = "${h.weekday.ar}، ${h.day} ${h.month.ar} ${h.year} هـ"
                _prayerState.update {
                    it.copy(
                        isLoading = false,
                        isOfflineFallback = false,
                        hijriDate = hijriText,
                        gregorianDate = gregStr,
                        statusNote = "مواقيت ${city.arabicLabel} (محدّثة الآن)"
                    )
                }
                if (manual) emitToast("تم تحديث مواقيت الصلاة")
            } catch (_: Exception) {
                currentTimings24 = OfflineIslamicData.fallbackTimings24
                _prayerState.update {
                    it.copy(
                        isLoading = false,
                        isOfflineFallback = true,
                        hijriDate = "التقويم الهجري (وضع عدم الاتصال)",
                        gregorianDate = gregStr,
                        statusNote = "وضع عدم الاتصال (مواقيت تقريبية)"
                    )
                }
                if (manual) emitToast("تعذّر الاتصال، تم استخدام المواقيت التقريبية")
            }
            startPrayerCountdownLoop()
        }
    }

    private fun startPrayerCountdownLoop() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (isActive) {
                updatePrayerCountdownOnce()
                delay(1000)
            }
        }
    }

    private fun updatePrayerCountdownOnce() {
        val now = Calendar.getInstance()
        var nextKey: String? = null
        var nextArabic = "الفجر"
        var targetCal: Calendar? = null

        for ((key, arName) in prayerMeta) {
            val timeStr = currentTimings24[key] ?: continue
            val parts = timeStr.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: continue
            val m = parts.getOrNull(1)?.toIntOrNull() ?: continue
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.after(now)) {
                nextKey = key
                nextArabic = arName
                targetCal = candidate
                break
            }
        }

        if (nextKey == null) {
            nextKey = "Fajr"
            nextArabic = "الفجر"
            val parts = (currentTimings24["Fajr"] ?: "05:00").split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 5
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            targetCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val diffSec = (((targetCal?.timeInMillis ?: now.timeInMillis) - now.timeInMillis) / 1000L)
            .coerceAtLeast(0L)
        val hrs = diffSec / 3600
        val mins = (diffSec % 3600) / 60
        val secs = diffSec % 60
        val countdown = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)

        val entries = prayerMeta.map { (key, arName) ->
            val raw = currentTimings24[key] ?: "--:--"
            PrayerEntry(
                key = key,
                arabicName = arName,
                time24 = raw,
                formatted12 = format12HourArabic(raw),
                isNext = key == nextKey
            )
        }

        _prayerState.update {
            it.copy(
                prayers = entries,
                nextPrayerName = nextArabic,
                countdownText = countdown
            )
        }

        // Check Smart Fajr Alarm auto-trigger when enabled
        val alarmCfg = fajrAlarmSettings.value
        if (alarmCfg.enabled && !_fajrAlarmRinging.value) {
            val today = todayKey()
            if (lastAutoTriggeredAlarmDate != today) {
                val fajr24 = currentTimings24["Fajr"] ?: "04:55"
                val wakeup24 = FajrTimeCalculator.calculateWakeupTime(fajr24, alarmCfg.offsetMinutesBeforeFajr).time24
                val nowHHmm = String.format(
                    Locale.US,
                    "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE)
                )
                if (nowHHmm == wakeup24) {
                    lastAutoTriggeredAlarmDate = today
                    triggerFajrAlarmNow(isTest = false)
                }
            }
        }
    }

    fun toggleWorshipTask(taskId: String) {
        val updated = _worshipTasks.value.map {
            if (it.id == taskId) it.copy(completed = !it.completed) else it
        }
        _worshipTasks.value = updated
        val doneIds = updated.filter { it.completed }.map { it.id }.toSet()
        viewModelScope.launch {
            prefs.saveTrackerDoneIds(todayKey(), doneIds)
        }
        if (doneIds.size == updated.size && updated.find { it.id == taskId }?.completed == true) {
            emitToast("ما شاء الله! أتممت وردك اليومي بالكامل")
        }
    }

    fun resetTodayTracker() {
        _worshipTasks.value = OfflineIslamicData.worshipTasks.map { it.copy(completed = false) }
        viewModelScope.launch { prefs.saveTrackerDoneIds(todayKey(), emptySet()) }
        emitToast("تم تصفير ورد اليوم")
    }

    fun rotateInspiration() {
        _inspirationIndex.update { it + 1 }
    }

    fun currentDailyVerse(): DailyQuote {
        val list = OfflineIslamicData.dailyVerses
        return list[Math.floorMod(_inspirationIndex.value, list.size)]
    }

    fun currentDailyHadith(): DailyQuote {
        val list = OfflineIslamicData.dailyHadiths
        return list[Math.floorMod(_inspirationIndex.value, list.size)]
    }

    fun openBookmarkedSurah() {
        val bm = bookmark.value ?: return
        _selectedTab.value = RafiqTab.QURAN
        if (!surahListRequested) loadSurahList()
        val matched = _quranState.value.surahs.find { it.number == bm.surahNumber }
            ?: SurahSummary(
                number = bm.surahNumber,
                name = bm.surahName,
                numberOfAyahs = 0,
                revelationType = "Meccan"
            )
        openSurahReader(matched, scrollToAyah = bm.ayahNumber)
    }

    // =========================================================================
    // MODULE B: QURAN — 114 SURAHS, READER, TAFSIR, BOOKMARK, AUDIO
    // =========================================================================

    fun loadSurahList() {
        surahListRequested = true
        viewModelScope.launch {
            _quranState.update { it.copy(isLoadingList = true) }
            val cached = prefs.getCachedSurahs()
            if (!cached.isNullOrEmpty()) {
                _quranState.update { it.copy(surahs = cached, isLoadingList = false) }
                return@launch
            }
            try {
                val res = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.alQuranApi.getAllSurahs()
                }
                prefs.cacheSurahs(res.data)
                _quranState.update { it.copy(surahs = res.data, isLoadingList = false) }
            } catch (_: Exception) {
                _quranState.update {
                    it.copy(surahs = OfflineIslamicData.fallbackSurahs, isLoadingList = false)
                }
                emitToast("تعذّر الاتصال، تم عرض قائمة سور مختصرة دون إنترنت")
            }
        }
    }

    fun updateSurahSearch(query: String) {
        _quranState.update { it.copy(searchQuery = query) }
    }

    fun updateRevelationFilter(filter: String) {
        _quranState.update { it.copy(revelationFilter = filter) }
    }

    fun openSurahReader(surah: SurahSummary, scrollToAyah: Int = 1) {
        audioPlayer.prepareSurahMetadata(surah.number, surah.name)
        _quranState.update {
            it.copy(
                activeSurah = surah,
                ayahs = emptyList(),
                isLoadingReader = true,
                readerError = null,
                expandedTafsirAyahs = emptySet(),
                targetScrollAyah = scrollToAyah
            )
        }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.alQuranApi.getSurahWithTafsir(surah.number)
                }
                val uthmani = response.data.getOrNull(0)
                val tafsir = response.data.getOrNull(1)
                if (uthmani == null) throw IllegalStateException("Missing edition")

                val updatedSurah = surah.copy(
                    name = uthmani.name,
                    numberOfAyahs = uthmani.numberOfAyahs,
                    revelationType = uthmani.revelationType
                )
                val bismillahPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
                val ayahs = uthmani.ayahs.mapIndexed { idx, a ->
                    var cleanText = a.text
                    if (surah.number != 1 && surah.number != 9 && a.numberInSurah == 1) {
                        cleanText = cleanText.removePrefix(bismillahPrefix).trimStart()
                    }
                    AyahItem(
                        numberInSurah = a.numberInSurah,
                        text = cleanText,
                        tafsir = tafsir?.ayahs?.getOrNull(idx)?.text.orEmpty(),
                        globalNumber = a.number,
                        juz = a.juz.coerceAtLeast(1)
                    )
                }
                _quranState.update {
                    it.copy(
                        activeSurah = updatedSurah,
                        ayahs = ayahs,
                        isLoadingReader = false,
                        readerError = null
                    )
                }
            } catch (_: Exception) {
                val offline = OfflineIslamicData.offlineSurahAyahs[surah.number]
                if (offline != null) {
                    _quranState.update {
                        it.copy(
                            activeSurah = surah.copy(numberOfAyahs = offline.size),
                            ayahs = offline,
                            isLoadingReader = false,
                            readerError = null
                        )
                    }
                    emitToast("تم فتح السورة من الوضع غير المتصل")
                } else {
                    _quranState.update {
                        it.copy(
                            isLoadingReader = false,
                            readerError = "تعذّر تحميل السورة. يرجى التأكد من اتصالك بالإنترنت ثم إعادة المحاولة."
                        )
                    }
                }
            }
        }
    }

    fun closeSurahReader() {
        ayahAudioPlayer.stop()
        _quranState.update {
            it.copy(
                activeSurah = null,
                ayahs = emptyList(),
                readerError = null,
                isFullScreenReading = false
            )
        }
    }

    fun updateQuranFontSize(sizeSp: Float) {
        viewModelScope.launch { prefs.setQuranFontSize(sizeSp.coerceIn(18f, 40f)) }
    }

    fun selectQuranViewMode(mode: QuranViewMode) {
        _quranState.update { it.copy(viewMode = mode) }
    }

    fun setMushafPaperTheme(theme: MushafPaperTheme) {
        viewModelScope.launch { prefs.setMushafPaperTheme(theme) }
    }

    fun toggleFullScreenReading() {
        _quranState.update { it.copy(isFullScreenReading = !it.isFullScreenReading) }
    }

    fun togglePlaySingleAyah(ayah: AyahItem) {
        val surah = _quranState.value.activeSurah ?: return
        stopOtherAudio(except = AudioSource.AYAH)
        val reciter = audioState.value.selectedReciter
        ayahAudioPlayer.togglePlayAyah(
            surahNumber = surah.number,
            ayah = ayah,
            reciter = reciter,
            onAdvanceToNextAyah = { nextAyahNum ->
                val nextAyah = _quranState.value.ayahs.find { it.numberInSurah == nextAyahNum }
                if (nextAyah != null) {
                    togglePlaySingleAyah(nextAyah)
                }
            }
        )
    }

    fun stopSingleAyahAudio() {
        ayahAudioPlayer.stop()
    }

    fun setAyahRepeatMode(mode: AyahRepeatMode) {
        ayahAudioPlayer.setRepeatMode(mode)
    }

    fun setAyahContinueReading(enabled: Boolean) {
        ayahAudioPlayer.setContinueReading(enabled)
    }

    fun toggleAllTafsir() {
        _quranState.update { it.copy(showAllTafsir = !it.showAllTafsir) }
    }

    fun toggleSingleAyahTafsir(ayahNumber: Int) {
        _quranState.update { state ->
            val updated = if (state.expandedTafsirAyahs.contains(ayahNumber)) {
                state.expandedTafsirAyahs - ayahNumber
            } else {
                state.expandedTafsirAyahs + ayahNumber
            }
            state.copy(expandedTafsirAyahs = updated)
        }
    }

    fun saveQuranBookmark(ayahNumber: Int = 1) {
        val surah = _quranState.value.activeSurah ?: return
        viewModelScope.launch {
            prefs.saveBookmark(
                QuranBookmark(
                    surahNumber = surah.number,
                    surahName = surah.name,
                    ayahNumber = ayahNumber
                )
            )
            emitToast("تم حفظ العلامة: ${surah.name} - الآية $ayahNumber")
        }
    }

    fun openReciterLibrary() {
        _reciterLibraryOpen.value = true
        if (!recitersRequested || _apiReciters.value.isEmpty()) {
            loadApiReciters()
        }
    }

    fun closeReciterLibrary() {
        _reciterLibraryOpen.value = false
    }

    fun loadApiReciters(forceRefresh: Boolean = false) {
        recitersRequested = true
        viewModelScope.launch {
            _isRecitersLoading.value = true
            _recitersError.value = null

            if (!forceRefresh) {
                val cached = prefs.getCachedReciters()
                if (!cached.isNullOrEmpty()) {
                    _apiReciters.value = cached
                    _isRecitersLoading.value = false
                    return@launch
                }
            }

            try {
                val response = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.mp3QuranApi.getReciters("ar")
                }
                val mapped = response.reciters.mapNotNull { dto ->
                    val validMoshafs = dto.moshaf
                        .filter { it.server.isNotBlank() }
                        .map { m ->
                            MoshafOption(
                                id = m.id,
                                name = m.name,
                                server = m.server,
                                surahTotal = m.surahTotal,
                                surahList = m.surahList
                            )
                        }
                    val firstMoshaf = validMoshafs.firstOrNull() ?: return@mapNotNull null
                    ReciterOption(
                        id = "mp3q_${dto.id}",
                        arabicName = dto.name,
                        riwayahName = firstMoshaf.name,
                        serverUrl = firstMoshaf.server,
                        moshafList = validMoshafs,
                        isFeatured = false
                    )
                }
                if (mapped.isNotEmpty()) {
                    prefs.cacheReciters(mapped)
                    _apiReciters.value = mapped
                }
                _isRecitersLoading.value = false
            } catch (_: Exception) {
                _isRecitersLoading.value = false
                _recitersError.value = "تعذّر تحميل قائمة MP3Quran، يتم عرض قائمة أشهر القراء"
                if (forceRefresh) emitToast("تعذّر تحديث قائمة القراء، تأكد من الاتصال بالإنترنت")
            }
        }
    }

    fun playSurahFromList(surah: SurahSummary) {
        stopOtherAudio(except = AudioSource.SURAH)
        audioPlayer.togglePlayPause(surah.number, surah.name)
    }

    fun toggleSurahAudio() {
        val activeReaderSurah = _quranState.value.activeSurah
        val surahNum = activeReaderSurah?.number ?: audioState.value.loadedSurahNumber ?: return
        val surahName = activeReaderSurah?.name
            ?: audioState.value.loadedSurahName.ifBlank { resolveSurahName(surahNum) }
        stopOtherAudio(except = AudioSource.SURAH)
        audioPlayer.togglePlayPause(surahNum, surahName)
    }

    fun toggleMiniPlayerPlayPause() {
        val surahNum = audioState.value.loadedSurahNumber ?: return
        val surahName = audioState.value.loadedSurahName.ifBlank { resolveSurahName(surahNum) }
        stopOtherAudio(except = AudioSource.SURAH)
        audioPlayer.togglePlayPause(surahNum, surahName)
    }

    fun playNextSurah() {
        val currentNum = audioState.value.loadedSurahNumber ?: return
        val nextNum = (currentNum + 1).coerceAtMost(114)
        if (nextNum == currentNum) return
        val nextName = resolveSurahName(nextNum)
        stopOtherAudio(except = AudioSource.SURAH)
        audioPlayer.playSurah(nextNum, nextName, forceRestart = true)
    }

    fun playPreviousSurah() {
        val currentNum = audioState.value.loadedSurahNumber ?: return
        val prevNum = (currentNum - 1).coerceAtLeast(1)
        if (prevNum == currentNum) return
        val prevName = resolveSurahName(prevNum)
        stopOtherAudio(except = AudioSource.SURAH)
        audioPlayer.playSurah(prevNum, prevName, forceRestart = true)
    }

    fun dismissMiniPlayer() {
        audioPlayer.dismissAndStop()
    }

    fun selectReciter(reciter: ReciterOption) {
        if (audioState.value.isPlaying) {
            stopOtherAudio(except = AudioSource.SURAH)
        }
        audioPlayer.setReciter(reciter)
        viewModelScope.launch { prefs.saveSelectedQuranReciter(reciter) }
        emitToast("تم اختيار القارئ: ${reciter.arabicName}")
    }

    fun seekAudio(fraction: Float) {
        audioPlayer.seekToFraction(fraction)
    }

    fun downloadCurrentSurahMp3() {
        val state = audioState.value
        val surahNum = state.loadedSurahNumber ?: _quranState.value.activeSurah?.number ?: return
        val surahName = state.loadedSurahName.ifBlank { resolveSurahName(surahNum) }
        val url = state.activeStreamUrl ?: state.selectedReciter.buildPrimaryAudioUrl(surahNum)
        val padded = surahNum.toString().padStart(3, '0')
        val fileName = "Quran_${padded}_${state.selectedReciter.id}.mp3"

        runCatching {
            val appCtx = getApplication<Application>().applicationContext
            val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("$surahName — ${state.selectedReciter.arabicName}")
                .setDescription("جاري تحميل التلاوة بصيغة MP3")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            } else {
                request.setDestinationInExternalFilesDir(appCtx, Environment.DIRECTORY_MUSIC, fileName)
            }
            dm.enqueue(request)
            emitToast("بدأ تحميل $surahName (${state.selectedReciter.arabicName})")
        }.onFailure {
            emitToast("تعذّر بدء التحميل، تأكد من الاتصال بالإنترنت")
        }
    }

    private fun resolveSurahName(surahNumber: Int): String {
        return _quranState.value.surahs.find { it.number == surahNumber }?.name
            ?: OfflineIslamicData.fallbackSurahs.find { it.number == surahNumber }?.name
            ?: "سورة رقم $surahNumber"
    }

    // =========================================================================
    // MODULE C: AZKAR & ELECTRONIC TASBIH
    // =========================================================================

    fun selectAzkarSubTab(index: Int) {
        _azkarState.update { it.copy(subTabIndex = index) }
    }

    fun selectAzkarCategory(category: String) {
        _azkarState.update { it.copy(selectedCategory = category) }
    }

    fun decrementZikr(zikrId: String) {
        val currentMap = _azkarState.value.remainingCounts
        val rem = currentMap[zikrId] ?: return
        if (rem <= 0) return
        val nextRem = rem - 1
        val updated = currentMap.toMutableMap().apply { put(zikrId, nextRem) }
        _azkarState.update { it.copy(remainingCounts = updated) }
        viewModelScope.launch { prefs.saveAzkarRemaining(todayKey(), updated) }
        if (nextRem == 0) {
            emitToast("تم إتمام الذكر بنجاح")
        }
    }

    fun resetCurrentAzkarCategory() {
        val category = _azkarState.value.selectedCategory
        val items = OfflineIslamicData.azkarCategories[category].orEmpty()
        val updated = _azkarState.value.remainingCounts.toMutableMap()
        items.forEach { updated[it.id] = it.requiredCount }
        _azkarState.update { it.copy(remainingCounts = updated) }
        viewModelScope.launch { prefs.saveAzkarRemaining(todayKey(), updated) }
        emitToast("تم تصفير $category")
    }

    fun incrementTasbih() {
        val current = tasbihState.value
        val nextCurrent = current.current + 1
        val nextLifetime = current.lifetime + 1
        val completedRound = current.target > 0 && nextCurrent >= current.target
        val nextState = if (completedRound) {
            emitToast("أتممت دورة (${current.target}) من ${current.phrase}")
            current.copy(
                current = 0,
                rounds = current.rounds + 1,
                lifetime = nextLifetime
            )
        } else {
            current.copy(
                current = nextCurrent,
                lifetime = nextLifetime
            )
        }
        viewModelScope.launch { prefs.saveTasbih(nextState) }
    }

    fun setTasbihPhrase(phrase: String) {
        val trimmed = phrase.trim()
        if (trimmed.isEmpty()) {
            emitToast("اكتب الذكر أولاً")
            return
        }
        val next = tasbihState.value.copy(phrase = trimmed, current = 0)
        viewModelScope.launch { prefs.saveTasbih(next) }
    }

    fun setTasbihTarget(target: Int) {
        val next = tasbihState.value.copy(target = target, current = 0)
        viewModelScope.launch { prefs.saveTasbih(next) }
    }

    fun resetTasbihCurrent() {
        val next = tasbihState.value.copy(current = 0, rounds = 0)
        viewModelScope.launch { prefs.saveTasbih(next) }
        emitToast("تم تصفير العداد الحالي")
    }

    fun resetTasbihLifetime() {
        val next = tasbihState.value.copy(lifetime = 0)
        viewModelScope.launch { prefs.saveTasbih(next) }
        emitToast("تم تصفير الإجمالي الكلي")
    }

    // =========================================================================
    // MODULE D: UPGRADED HADITH LIBRARY (4 COLLECTIONS + JSDELIVR API)
    // =========================================================================

    fun selectHadithBook(book: HadithBookCollection) {
        _hadithState.update {
            it.copy(
                selectedBook = book,
                visibleCount = 30,
                bookLoadNotice = null
            )
        }
        if (!_hadithState.value.fetchedFromApiBooks.contains(book)) {
            fetchHadithCollection(book)
        }
    }

    fun fetchHadithCollection(book: HadithBookCollection) {
        viewModelScope.launch {
            _hadithState.update { it.copy(isLoadingBook = true, bookLoadNotice = null) }
            try {
                val response = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.hadithCdnApi.getCollection(book.apiEdition)
                }
                val sections = response.metadata?.sections.orEmpty()
                val offlineLookup = HadithLibraryData.offlineForBook(book).associateBy { it.number }
                val mapped = response.hadiths
                    .asSequence()
                    .filter { !it.text.isNullOrBlank() }
                    .mapIndexed { idx, entry ->
                        val num = entry.hadithnumber.toInt().takeIf { it > 0 } ?: (idx + 1)
                        val rawText = entry.text.orEmpty().trim()
                        val sectionKey = entry.reference?.book?.toString().orEmpty()
                        val sectionName = sections[sectionKey].orEmpty()
                        val offlineMatch = offlineLookup[num]
                        LibraryHadith(
                            uid = "${book.id}_$num",
                            book = book,
                            number = num,
                            chapter = offlineMatch?.chapter ?: HadithChapters.classifyChapter(rawText, sectionName),
                            title = offlineMatch?.title ?: sectionName,
                            narrator = offlineMatch?.narrator.orEmpty(),
                            text = rawText,
                            explanation = offlineMatch?.explanation.orEmpty(),
                            source = "${book.arabicTitle} • حديث رقم $num"
                        )
                    }
                    .distinctBy { it.uid }
                    .toList()

                if (mapped.isNotEmpty()) {
                    _hadithState.update { state ->
                        val updatedMap = state.loadedCollections.toMutableMap().apply {
                            put(book, mapped)
                        }
                        state.copy(
                            loadedCollections = updatedMap,
                            fetchedFromApiBooks = state.fetchedFromApiBooks + book,
                            isLoadingBook = false,
                            bookLoadNotice = null
                        )
                    }
                } else {
                    _hadithState.update {
                        it.copy(
                            isLoadingBook = false,
                            bookLoadNotice = "تم عرض المختارات المحفوظة محلياً"
                        )
                    }
                }
            } catch (_: Exception) {
                _hadithState.update {
                    it.copy(
                        isLoadingBook = false,
                        bookLoadNotice = "وضع عدم الاتصال: يتم عرض المختارات الموثقة لـ ${book.arabicTitle}"
                    )
                }
            }
        }
    }

    fun updateHadithSearch(query: String) {
        _hadithState.update { it.copy(searchQuery = query, visibleCount = 30) }
    }

    fun toggleHadithSearchAllBooks(searchAll: Boolean) {
        _hadithState.update { it.copy(searchAllBooks = searchAll, visibleCount = 30) }
    }

    fun selectHadithChapter(chapter: String) {
        _hadithState.update { it.copy(selectedChapter = chapter, visibleCount = 30) }
    }

    fun loadMoreHadiths() {
        _hadithState.update { it.copy(visibleCount = it.visibleCount + 30) }
    }

    fun toggleHadithExplanation(hadithUid: String) {
        _hadithState.update { state ->
            val next = if (state.expandedUids.contains(hadithUid)) {
                state.expandedUids - hadithUid
            } else {
                state.expandedUids + hadithUid
            }
            state.copy(expandedUids = next)
        }
    }

    fun toggleFavoriteHadith(hadithUid: String) {
        viewModelScope.launch {
            val current = favoriteHadithUids.value
            val added = !current.contains(hadithUid)
            val next = if (added) current + hadithUid else current - hadithUid
            prefs.saveFavoriteHadithUids(next)
            emitToast(if (added) "تم حفظ الحديث في المفضلة" else "تمت إزالة الحديث من المفضلة")
        }
    }

    fun updateHadithFontSize(sizeSp: Float) {
        viewModelScope.launch { prefs.setHadithFontSize(sizeSp.coerceIn(16f, 30f)) }
    }

    // =========================================================================
    // MODULE E: TOOLS — QIBLA & GUIDES
    // =========================================================================

    fun updateQiblaLocation(lat: Double, lng: Double, label: String) {
        val bearing = QiblaMath.calculateQiblaBearing(lat, lng)
        val distKm = QiblaMath.calculateDistanceKm(lat, lng)
        _toolsState.update {
            it.copy(
                locationLabel = label,
                qiblaAngleDeg = bearing,
                distanceKm = distKm
            )
        }
    }

    fun onGpsLocationDetected(location: Location) {
        val lat = location.latitude
        val lng = location.longitude
        val label = String.format(Locale.US, "موقعك الحالي (%.2f, %.2f)", lat, lng)
        updateQiblaLocation(lat, lng, label)
        emitToast("تم تحديث اتجاه القبلة حسب موقعك")
    }

    fun updateDeviceHeading(headingDeg: Float) {
        _toolsState.update { it.copy(headingDeg = headingDeg) }
    }

    fun setCompassSensorEnabled(enabled: Boolean) {
        _toolsState.update { it.copy(isSensorEnabled = enabled) }
    }

    fun toggleGuideAccordion(guideId: String) {
        _toolsState.update { state ->
            val next = if (state.expandedGuideIds.contains(guideId)) {
                state.expandedGuideIds - guideId
            } else {
                state.expandedGuideIds + guideId
            }
            state.copy(expandedGuideIds = next)
        }
    }

    // =========================================================================
    // MODULE F: GLOBAL MUEZZIN LIBRARY (مكتبة المؤذنين العالمية)
    // =========================================================================

    fun openMuezzinLibrary() {
        _muezzinLibraryOpen.value = true
    }

    fun closeMuezzinLibrary() {
        _muezzinLibraryOpen.value = false
    }

    fun toggleAdhanPreview(muezzin: MuezzinItem) {
        stopOtherAudio(except = AudioSource.ADHAN)
        adhanPlayer.togglePreview(muezzin)
    }

    fun switchAdhanFallbackStream() {
        stopOtherAudio(except = AudioSource.ADHAN)
        adhanPlayer.switchToNextFallback()
    }

    fun retryAdhanPreview() {
        stopOtherAudio(except = AudioSource.ADHAN)
        adhanPlayer.retryActiveMuezzin()
    }

    fun toggleFavoriteMuezzin(muezzinId: String) {
        viewModelScope.launch {
            val current = favoriteMuezzinIds.value
            val next = if (current.contains(muezzinId)) current - muezzinId else current + muezzinId
            prefs.saveFavoriteMuezzinIds(next)
        }
    }

    fun setDefaultMuezzin(muezzin: MuezzinItem) {
        viewModelScope.launch {
            prefs.setDefaultMuezzinId(muezzin.id)
            if (muezzin.hasAudioSource()) {
                emitToast("تم تعيين ${muezzin.name} كأذان افتراضي")
            } else {
                emitToast("تم تعيين ${muezzin.name} — اضغط «أضف ملف الصوت» لإرفاق ملف MP3 أو رابط")
            }
        }
    }

    fun setFajrMuezzin(muezzin: MuezzinItem) {
        viewModelScope.launch {
            prefs.setFajrMuezzinId(muezzin.id)
            prefs.setUseDedicatedFajrAdhan(true)
            if (muezzin.hasAudioSource()) {
                emitToast("تم تعيين ${muezzin.name} لأذان الفجر")
            } else {
                emitToast("تم تعيين ${muezzin.name} للفجر — اضغط «أضف ملف الصوت» لإرفاق ملف MP3 أو رابط")
            }
        }
    }

    fun setUseDedicatedFajrAdhan(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setUseDedicatedFajrAdhan(enabled)
        }
    }

    fun addCustomMuezzinFromUrl(name: String, url: String, isFajr: Boolean) {
        viewModelScope.launch {
            val newItem = MuezzinItem(
                id = "custom_url_${System.currentTimeMillis()}",
                name = name,
                subtitle = if (isFajr) "أذان الفجر • رابط صوتي مخصص" else "رابط صوتي مخصص (MP3)",
                region = if (isFajr) MuezzinRegion.FAJR.name else MuezzinRegion.CUSTOM.name,
                primaryUrl = url,
                fallbackUrls = emptyList(),
                isFajr = isFajr,
                isCustom = true,
                hasCustomAudio = true,
                customAudioLabel = "رابط MP3 مخصص"
            )
            val updated = listOf(newItem) + customMuezzins.value
            prefs.saveCustomMuezzins(updated)
            if (isFajr) {
                prefs.setFajrMuezzinId(newItem.id)
            } else {
                prefs.setDefaultMuezzinId(newItem.id)
            }
            emitToast("تمت إضافة $name إلى مكتبة المؤذنين")
        }
    }

    fun overrideMuezzinAudioWithUrl(muezzin: MuezzinItem, newUrl: String) {
        val cleanUrl = newUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            emitToast("يرجى إدخال رابط MP3 صالح يبدأ بـ https://")
            return
        }
        viewModelScope.launch {
            if (adhanState.value.activeMuezzinId == muezzin.id) {
                adhanPlayer.stop()
            }
            if (muezzin.isCustom) {
                // Clean up old local file if replacing a file-based custom muezzin
                muezzin.localFilePath?.let { path ->
                    withContext(Dispatchers.IO) { runCatching { File(path).delete() } }
                }
                val updated = customMuezzins.value.map { item ->
                    if (item.id == muezzin.id) {
                        item.copy(
                            primaryUrl = cleanUrl,
                            fallbackUrls = emptyList(),
                            localFilePath = null,
                            hasCustomAudio = true,
                            customAudioLabel = "رابط MP3 مخصص"
                        )
                    } else item
                }
                prefs.saveCustomMuezzins(updated)
            } else {
                val existing = muezzinAudioOverrides.value[muezzin.id]
                existing?.localFilePath?.let { oldPath ->
                    withContext(Dispatchers.IO) { runCatching { File(oldPath).delete() } }
                }
                val nextMap = muezzinAudioOverrides.value.toMutableMap()
                nextMap[muezzin.id] = MuezzinAudioOverride(
                    muezzinId = muezzin.id,
                    customUrl = cleanUrl,
                    localFilePath = null,
                    localFileName = null
                )
                prefs.saveMuezzinAudioOverrides(nextMap)
            }
            emitToast("تم تحديث رابط الصوت لـ ${muezzin.name}")
        }
    }

    fun overrideMuezzinAudioWithFile(muezzin: MuezzinItem, uri: Uri, displayName: String) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val appCtx = getApplication<Application>().applicationContext
                    val dir = File(appCtx.filesDir, "custom_adhans").apply { mkdirs() }
                    val outFile = File(dir, "override_${muezzin.id}_${System.currentTimeMillis()}.mp3")
                    appCtx.contentResolver.openInputStream(uri)?.use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching null
                    if (outFile.length() > 0L) outFile.absolutePath else null
                }.getOrNull()
            }

            if (savedPath == null) {
                emitToast("تعذّر حفظ الملف الصوتي، جرّب ملفاً آخر")
                return@launch
            }

            if (adhanState.value.activeMuezzinId == muezzin.id) {
                adhanPlayer.stop()
            }

            val label = displayName.ifBlank { "ملف MP3 محلي" }
            if (muezzin.isCustom) {
                muezzin.localFilePath?.let { oldPath ->
                    if (oldPath != savedPath) {
                        withContext(Dispatchers.IO) { runCatching { File(oldPath).delete() } }
                    }
                }
                val updated = customMuezzins.value.map { item ->
                    if (item.id == muezzin.id) {
                        item.copy(
                            primaryUrl = "",
                            fallbackUrls = emptyList(),
                            localFilePath = savedPath,
                            hasCustomAudio = true,
                            customAudioLabel = label
                        )
                    } else item
                }
                prefs.saveCustomMuezzins(updated)
            } else {
                val existing = muezzinAudioOverrides.value[muezzin.id]
                existing?.localFilePath?.let { oldPath ->
                    if (oldPath != savedPath) {
                        withContext(Dispatchers.IO) { runCatching { File(oldPath).delete() } }
                    }
                }
                val nextMap = muezzinAudioOverrides.value.toMutableMap()
                nextMap[muezzin.id] = MuezzinAudioOverride(
                    muezzinId = muezzin.id,
                    customUrl = null,
                    localFilePath = savedPath,
                    localFileName = label
                )
                prefs.saveMuezzinAudioOverrides(nextMap)
            }
            emitToast("تم حفظ الصوت المخصص محلياً لـ ${muezzin.name}")
        }
    }

    fun resetMuezzinAudioToDefault(muezzin: MuezzinItem) {
        if (muezzin.isCustom) return
        viewModelScope.launch {
            if (adhanState.value.activeMuezzinId == muezzin.id) {
                adhanPlayer.stop()
            }
            val existing = muezzinAudioOverrides.value[muezzin.id]
            existing?.localFilePath?.let { oldPath ->
                withContext(Dispatchers.IO) { runCatching { File(oldPath).delete() } }
            }
            val nextMap = muezzinAudioOverrides.value.toMutableMap()
            nextMap.remove(muezzin.id)
            prefs.saveMuezzinAudioOverrides(nextMap)
            emitToast("تمت استعادة الصوت الافتراضي لـ ${muezzin.name}")
        }
    }

    fun addCustomMuezzinFromFile(name: String, uri: Uri, isFajr: Boolean) {
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) {
                runCatching {
                    val appCtx = getApplication<Application>().applicationContext
                    val dir = File(appCtx.filesDir, "custom_adhans").apply { mkdirs() }
                    val outFile = File(dir, "adhan_${System.currentTimeMillis()}.mp3")
                    appCtx.contentResolver.openInputStream(uri)?.use { input ->
                        outFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    } ?: return@runCatching null
                    if (outFile.length() > 0L) outFile.absolutePath else null
                }.getOrNull()
            }

            if (savedPath == null) {
                emitToast("تعذّر حفظ الملف الصوتي، جرّب ملفاً آخر")
                return@launch
            }

            val newItem = MuezzinItem(
                id = "custom_file_${System.currentTimeMillis()}",
                name = name,
                subtitle = if (isFajr) "أذان الفجر • ملف محلي محفوظ" else "ملف MP3 محلي محفوظ على الجهاز",
                region = if (isFajr) MuezzinRegion.FAJR.name else MuezzinRegion.CUSTOM.name,
                primaryUrl = "",
                localFilePath = savedPath,
                isFajr = isFajr,
                isCustom = true
            )
            val updated = listOf(newItem) + customMuezzins.value
            prefs.saveCustomMuezzins(updated)
            if (isFajr) {
                prefs.setFajrMuezzinId(newItem.id)
            } else {
                prefs.setDefaultMuezzinId(newItem.id)
            }
            emitToast("تم حفظ ملف $name محلياً بنجاح")
        }
    }

    fun deleteCustomMuezzin(muezzin: MuezzinItem) {
        viewModelScope.launch {
            if (adhanState.value.activeMuezzinId == muezzin.id) {
                adhanPlayer.stop()
            }
            muezzin.localFilePath?.let { path ->
                withContext(Dispatchers.IO) { runCatching { File(path).delete() } }
            }
            val updated = customMuezzins.value.filterNot { it.id == muezzin.id }
            prefs.saveCustomMuezzins(updated)
            if (defaultMuezzinId.value == muezzin.id) {
                prefs.setDefaultMuezzinId(MuezzinLibraryData.defaultMuezzinId)
            }
            if (fajrMuezzinId.value == muezzin.id) {
                prefs.setFajrMuezzinId(MuezzinLibraryData.defaultFajrMuezzinId)
            }
            emitToast("تم حذف ${muezzin.name}")
        }
    }

    // =========================================================================
    // MODULE G: SMART FAJR ALARM (منبه صلاة الفجر الذكي)
    // =========================================================================

    fun openFajrAlarmSettings() {
        _fajrAlarmSettingsOpen.value = true
    }

    fun closeFajrAlarmSettings() {
        _fajrAlarmSettingsOpen.value = false
    }

    fun updateFajrAlarmSettings(updated: FajrAlarmSettings) {
        viewModelScope.launch {
            prefs.saveFajrAlarmSettings(updated)
        }
    }

    fun triggerFajrAlarmNow(isTest: Boolean = true) {
        stopOtherAudio(except = AudioSource.FAJR_ALARM)
        val cfg = fajrAlarmSettings.value
        _fajrAlarmRinging.value = true
        fajrAlarmPlayer.startRinging(cfg.resolvedSound(), activeFajrMuezzin.value)
        if (isTest) {
            emitToast("بدأت تجربة منبه الفجر الذكي — أتمّ التحدي لإيقاف الصوت")
        }
    }

    fun dismissFajrAlarmAfterChallenge() {
        fajrAlarmPlayer.stopRinging()
        _fajrAlarmRinging.value = false
        emitToast("بارك الله فيك! تم إيقاف منبه الفجر — تقبل الله صلاتك")
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        audioPlayer.stopAndRelease(keepReciter = false)
        ayahAudioPlayer.stop()
        adhanPlayer.stop()
        fajrAlarmPlayer.stopRinging()
    }

    // ---- Helpers ----

    private fun cleanTime(raw: String): String = raw.substringBefore(" ").trim()

    private fun format12HourArabic(time24: String): String {
        val parts = time24.split(":")
        val h24 = parts.getOrNull(0)?.toIntOrNull() ?: return "--:--"
        val min = parts.getOrNull(1) ?: "00"
        val suffix = if (h24 >= 12) "م" else "ص"
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return "$h12:$min $suffix"
    }

    private fun formatGregorianArabic(): String {
        return try {
            SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar", "EG")).format(Date())
        } catch (_: Exception) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
