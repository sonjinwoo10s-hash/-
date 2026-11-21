package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerScheduleStore
import com.example.audio.AdhanAudioPlayer
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioPlayer
import com.example.audio.AyahAudioState
import com.example.audio.FajrAlarmPlayer
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranPlaybackSpeeds
import com.example.data.local.AppPreferences
import com.example.data.local.HadithLibraryData
import com.example.data.local.MuezzinLibraryData
import com.example.data.local.OfflineIslamicData
import com.example.data.local.PersistentOfflineStorage
import com.example.quran.TajweedEngine
import com.example.service.PrayerCountdownService
import com.example.service.QuranMediaEngine
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
import com.example.data.model.OfflineHadithBookMeta
import com.example.data.model.PrayerEntry
import com.example.data.model.PrayerTimesUiState
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranViewMode
import com.example.data.model.ReciterOption
import com.example.data.model.SurahSummary
import com.example.data.model.TasbihState
import com.example.data.model.WorshipTask
import com.example.data.model.ZikrItem
import com.example.data.remote.IslamicNetworkClient
import com.example.data.repository.MuezzinRepository
import com.example.data.repository.MushafPageAyah
import com.example.data.repository.MushafPageData
import com.example.data.repository.OfflineAdhkarRepository
import com.example.data.repository.OfflineHadithRepository
import com.example.data.repository.OfflineMushafBook
import com.example.data.repository.OfflineQuranRepository
import com.example.data.repository.QuranTimingRepository
import com.example.ui.components.AppUpdateDialogStage
import com.example.ui.components.AppUpdateUiState
import com.example.update.AppUpdateManager
import com.example.data.repository.ReciterRepository
import com.example.data.repository.TafsirBook
import com.example.data.repository.TafsirRepository
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
    val targetScrollAyah: Int = 1,
    // 100% Offline 604-page Wahy Mushaf state
    val isMushafOpen: Boolean = false,
    val currentMushafPage: Int = 1,
    val lastSavedMushafPage: Int = 1,
    val mushafPages: List<MushafPageData> = emptyList(),
    val surahStartPages: Map<Int, Int> = emptyMap(),
    val juzStartPages: Map<Int, Int> = emptyMap(),
    val isLoadingOfflineMushaf: Boolean = false,
    val mushafDownloadProgress: String? = null,
    val mushafError: String? = null,
    val isMushafCachedOffline: Boolean = false
)

data class TafsirUiState(
    val selectedBook: TafsirBook = TafsirBook.MUYASSAR,
    val currentSurahNumber: Int = 1,
    val currentAyahNumber: Int = 1,
    val currentText: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val downloadedBookIds: Set<String> = emptySet(),
    val downloadingProgress: Map<String, Float> = emptyMap()
)

data class AzkarUiState(
    val subTabIndex: Int = 0, // 0 = Azkar, 1 = Tasbih
    val selectedCategory: String = "أذكار الصباح",
    val categories: Map<String, List<ZikrItem>> = emptyMap(),
    val categoryNames: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val remainingCounts: Map<String, Int> = emptyMap()
)

data class HadithUiState(
    val activeSubTab: Int = 0, // 0 = المكتبة والكتب, 1 = المفضلة
    val openBook: HadithBookCollection? = null,
    val bookStatuses: Map<String, OfflineHadithBookMeta> = emptyMap(),
    val downloadingProgress: Map<String, Float> = emptyMap(),
    val loadedCollections: Map<HadithBookCollection, List<LibraryHadith>> = emptyMap(),
    val isLoadingBook: Boolean = false,
    val bookError: String? = null,
    val searchQuery: String = "",
    val favoritesSearchQuery: String = "",
    val useCairoFont: Boolean = false, // false = Amiri (default), true = Cairo
    val visibleCount: Int = 25,
    val expandedUids: Set<String> = emptySet(),
    val savedFavoriteHadiths: List<LibraryHadith> = emptyList()
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
    // Process-scoped background audio players linked to QuranAudioPlaybackService
    private val audioPlayer: QuranAudioPlayer get() = QuranMediaEngine.surahPlayer
    private val ayahAudioPlayer: AyahAudioPlayer get() = QuranMediaEngine.ayahPlayer
    private val adhanPlayer = AdhanAudioPlayer(application.applicationContext, viewModelScope)
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

    val tajweedColorsEnabled: StateFlow<Boolean> = prefs.tajweedColorsEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
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

    val countdownNotificationEnabled: StateFlow<Boolean> = prefs.countdownNotificationEnabledFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val adhanSilenceOnVolume: StateFlow<Boolean> = prefs.adhanSilenceOnVolumeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val adhanSilenceOnFlip: StateFlow<Boolean> = prefs.adhanSilenceOnFlipFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
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

    private val _repoMuezzins = MutableStateFlow<List<MuezzinItem>>(emptyList())

    private val _isSyncingMuezzinRepo = MutableStateFlow(false)
    val isSyncingMuezzinRepo: StateFlow<Boolean> = _isSyncingMuezzinRepo.asStateFlow()

    private val _muezzinRepoStatus = MutableStateFlow<String?>(null)
    val muezzinRepoStatus: StateFlow<String?> = _muezzinRepoStatus.asStateFlow()

    private val _cachedAdhanIds = MutableStateFlow<Set<String>>(emptySet())
    val cachedAdhanIds: StateFlow<Set<String>> = _cachedAdhanIds.asStateFlow()

    private val _downloadingMuezzinIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingMuezzinIds: StateFlow<Set<String>> = _downloadingMuezzinIds.asStateFlow()

    val allMuezzins: StateFlow<List<MuezzinItem>> = combine(
        customMuezzins,
        muezzinAudioOverrides,
        _repoMuezzins
    ) { custom, overrides, repoList ->
        val baseCatalog = if (repoList.isNotEmpty()) repoList else MuezzinLibraryData.builtInMuezzins
        val mergedBase = baseCatalog.map { item ->
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
        mergedCustom + mergedBase
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), MuezzinLibraryData.builtInMuezzins
    )

    val activeDefaultMuezzin: StateFlow<MuezzinItem> = combine(
        allMuezzins,
        defaultMuezzinId
    ) { list, id ->
        list.find { it.id == id }
            ?: list.firstOrNull { it.hasAudioSource() }
            ?: list.firstOrNull()
            ?: MuezzinLibraryData.builtInMuezzins.first()
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
            ?: list.firstOrNull { it.isFajr && it.hasAudioSource() }
            ?: list.firstOrNull { it.isFajr }
            ?: list.firstOrNull { it.hasAudioSource() }
            ?: list.firstOrNull()
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

    private val _tafsirState = MutableStateFlow(TafsirUiState())
    val tafsirState: StateFlow<TafsirUiState> = _tafsirState.asStateFlow()

    private val _azkarState = MutableStateFlow(AzkarUiState())
    val azkarState: StateFlow<AzkarUiState> = _azkarState.asStateFlow()

    private val _hadithState = MutableStateFlow(HadithUiState())
    val hadithState: StateFlow<HadithUiState> = _hadithState.asStateFlow()

    private val _toolsState = MutableStateFlow(ToolsUiState())
    val toolsState: StateFlow<ToolsUiState> = _toolsState.asStateFlow()

    private val _appUpdateState = MutableStateFlow(AppUpdateUiState())
    val appUpdateState: StateFlow<AppUpdateUiState> = _appUpdateState.asStateFlow()

    private val _aboutCopyrightOpen = MutableStateFlow(false)
    val aboutCopyrightOpen: StateFlow<Boolean> = _aboutCopyrightOpen.asStateFlow()

    val lastHadithResume: StateFlow<Pair<HadithBookCollection, Int>?> = prefs.lastHadithResumeFlow
        .map { pair ->
            if (pair == null) null
            else HadithBookCollection.entries.find { it.id == pair.first }?.let { it to pair.second }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var currentTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var countdownJob: Job? = null
    private var appUpdateDownloadJob: Job? = null
    private var surahListRequested = false
    private var offlineMushafBook: OfflineMushafBook? = null
    private var offlineMushafJob: Job? = null
    private var azkarLoadJob: Job? = null
    private var tafsirFetchJob: Job? = null
    private val activeSurahAudioDownloads = mutableSetOf<String>()
    private var tafsirStatusesLoaded = false
    private var hadithFavoritesLoaded = false
    private var muezzinCatalogLoaded = false
    private var pendingMushafSurah: Int? = null
    private var pendingMushafAyah: Int = 1

    private val prayerMeta = listOf(
        "Fajr" to "الفجر",
        "Dhuhr" to "الظهر",
        "Asr" to "العصر",
        "Maghrib" to "المغرب",
        "Isha" to "العشاء"
    )

    init {
        QuranMediaEngine.ensureInitialized(
            context = application.applicationContext,
            resolveSurahName = { num -> resolveSurahName(num) }
        )
        // Clean up any leftover update APK & perform silent check (max once per 12h) off main thread
        viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            AppUpdateManager.cleanupLeftoverUpdateApk(appCtx)
            if (AppUpdateManager.shouldRunSilentCheck(appCtx)) {
                checkForAppUpdate(manual = false)
            }
        }
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
            refreshFuturePrayerLocks(System.currentTimeMillis())
        }
        viewModelScope.launch {
            val persistedReciter = prefs.selectedQuranReciterFlow.first()
            audioPlayer.syncPersistedReciter(persistedReciter)
            val persistedSpeed = prefs.quranPlaybackSpeedFlow.first()
            audioPlayer.syncPersistedSpeed(persistedSpeed)
            ayahAudioPlayer.setPlaybackSpeed(persistedSpeed)
            val wbwEnabled = prefs.wordByWordHighlightEnabledFlow.first()
            val btOffset = prefs.syncOffsetBluetoothMsFlow.first()
            val spkOffset = prefs.syncOffsetSpeakerMsFlow.first()
            audioPlayer.syncLatencyPreferences(
                wordByWordEnabled = wbwEnabled,
                bluetoothOffsetMs = btOffset,
                speakerOffsetMs = spkOffset
            )
        }
        // Keep PrayerScheduleStore synchronized with selected Muezzin / Fajr Adhan for background playback
        viewModelScope.launch {
            combine(
                activeDefaultMuezzin,
                activeFajrMuezzin,
                useDedicatedFajrAdhan
            ) { defM, fajrM, dedicated ->
                Triple(defM, fajrM, dedicated)
            }.collect { (defM, fajrM, dedicated) ->
                PrayerScheduleStore.saveMuezzinSelection(
                    context = getApplication(),
                    defaultMuezzin = defM,
                    fajrMuezzin = fajrM,
                    useDedicatedFajr = dedicated
                )
            }
        }
        // Keep persistent countdown notification service synced with user preference
        viewModelScope.launch {
            prefs.countdownNotificationEnabledFlow.collect { enabled ->
                val appCtx = getApplication<Application>().applicationContext
                PrayerScheduleStore.setCountdownNotificationEnabled(appCtx, enabled)
                PrayerCountdownService.syncState(appCtx)
            }
        }
        // Keep Adhan quick-silence toggles synced in PrayerScheduleStore for lock-screen service access
        viewModelScope.launch {
            prefs.adhanSilenceOnVolumeFlow.collect { enabled ->
                PrayerScheduleStore.setAdhanSilenceOnVolumeEnabled(
                    getApplication<Application>().applicationContext,
                    enabled
                )
            }
        }
        viewModelScope.launch {
            prefs.adhanSilenceOnFlipFlow.collect { enabled ->
                PrayerScheduleStore.setAdhanSilenceOnFlipEnabled(
                    getApplication<Application>().applicationContext,
                    enabled
                )
            }
        }
        // Lightweight startup check ONLY: read last Mushaf page pref & file existence flag.
        // Heavy Quran, Tafsir, Hadith, and Muezzin JSON files are NEVER parsed on app startup;
        // they load strictly on-demand on Dispatchers.IO when the user opens each section.
        viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            val lastPage = prefs.lastMushafPageFlow.first()
            val cachedLocally = withContext(Dispatchers.IO) {
                OfflineQuranRepository.isCachedLocally(appCtx)
            }
            _quranState.update {
                it.copy(
                    currentMushafPage = lastPage,
                    lastSavedMushafPage = lastPage,
                    isMushafCachedOffline = cachedLocally
                )
            }
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
        val previous = _selectedTab.value
        _selectedTab.value = tab

        // Auto-release heavy in-memory arrays when navigating away so GC frees RAM on 2GB-3GB devices
        if (previous == RafiqTab.HADITH && tab != RafiqTab.HADITH) {
            releaseHadithMemory()
        }
        if (previous == RafiqTab.AZKAR && tab != RafiqTab.AZKAR) {
            releaseAzkarMemory()
        }
        if (previous == RafiqTab.QURAN && tab != RafiqTab.QURAN && !_quranState.value.isMushafOpen) {
            releaseMushafMemory()
        }

        if (tab == RafiqTab.QURAN) {
            if (!surahListRequested) loadSurahList()
            if (offlineMushafBook == null) ensureOfflineMushafLoaded(forceRetry = false)
        } else if (tab == RafiqTab.AZKAR) {
            onAzkarTabOpened()
        } else if (tab == RafiqTab.HADITH) {
            onHadithLibraryOpened()
        }
    }

    private fun onAzkarTabOpened() {
        if (_azkarState.value.categories.isNotEmpty() || azkarLoadJob?.isActive == true) return
        val appCtx = getApplication<Application>().applicationContext
        _azkarState.update { it.copy(isLoading = true) }
        azkarLoadJob = viewModelScope.launch {
            val loadedCategories = OfflineAdhkarRepository.loadCategories(appCtx)
            val savedAzkar = prefs.getAzkarRemaining(todayKey())
            val defaults = loadedCategories.values.flatten()
                .associate { it.id to (savedAzkar[it.id] ?: it.requiredCount) }
            val names = loadedCategories.keys.toList()
            _azkarState.update { state ->
                val activeCat = if (loadedCategories.containsKey(state.selectedCategory)) {
                    state.selectedCategory
                } else {
                    names.firstOrNull() ?: state.selectedCategory
                }
                state.copy(
                    categories = loadedCategories,
                    categoryNames = names,
                    selectedCategory = activeCat,
                    remainingCounts = defaults,
                    isLoading = false
                )
            }
        }
    }

    private fun releaseAzkarMemory() {
        azkarLoadJob?.cancel()
        azkarLoadJob = null
        OfflineAdhkarRepository.clearMemoryCache()
        _azkarState.update {
            it.copy(
                categories = emptyMap(),
                categoryNames = emptyList(),
                isLoading = false
            )
        }
    }

    private fun releaseHadithMemory() {
        OfflineHadithRepository.clearMemoryCache()
        _hadithState.update {
            it.copy(
                openBook = null,
                loadedCollections = emptyMap(),
                searchQuery = "",
                visibleCount = 25,
                expandedUids = emptySet()
            )
        }
    }

    private fun releaseMushafMemory() {
        TafsirRepository.clearMemoryCache()
        TajweedEngine.clearCaches()
        if (!audioState.value.isPlaying && !audioState.value.isBuffering) {
            QuranTimingRepository.clearMemoryCache()
        }
        // Release 604-page text array from RAM unless Ayah-by-Ayah recitation needs page lookup
        if (!ayahAudioState.value.isPlaying && !ayahAudioState.value.isBuffering &&
            !audioState.value.isPlaying && !audioState.value.isBuffering
        ) {
            offlineMushafBook = null
            _quranState.update { it.copy(mushafPages = emptyList()) }
        }
    }

    fun openAboutCopyright() {
        _aboutCopyrightOpen.value = true
    }

    fun closeAboutCopyright() {
        _aboutCopyrightOpen.value = false
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            prefs.setDarkMode(!darkMode.value)
        }
    }

    fun setCountdownNotificationEnabled(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        if (enabled && !PermissionsHelper.isNotificationGranted(appCtx)) {
            emitToast("يرجى تفعيل إذن الإشعارات أولاً لعرض عدّاد الصلاة القادمة")
            return
        }
        PrayerScheduleStore.setCountdownNotificationEnabled(appCtx, enabled)
        viewModelScope.launch {
            prefs.setCountdownNotificationEnabled(enabled)
        }
        PrayerCountdownService.syncState(appCtx)
        emitToast(
            if (enabled) "تم تفعيل إشعار الوقت المتبقي للصلاة القادمة"
            else "تم إيقاف إشعار الوقت المتبقي للصلاة القادمة"
        )
    }

    fun setAdhanSilenceOnVolume(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.setAdhanSilenceOnVolumeEnabled(appCtx, enabled)
        viewModelScope.launch {
            prefs.setAdhanSilenceOnVolume(enabled)
        }
        emitToast(
            if (enabled) "تم تفعيل إيقاف الأذان بأزرار الصوت"
            else "تم إيقاف ميزة كتم الأذان بأزرار الصوت"
        )
    }

    fun setAdhanSilenceOnFlip(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.setAdhanSilenceOnFlipEnabled(appCtx, enabled)
        viewModelScope.launch {
            prefs.setAdhanSilenceOnFlip(enabled)
        }
        emitToast(
            if (enabled) "تم تفعيل إيقاف الأذان بقلب الهاتف"
            else "تم إيقاف ميزة كتم الأذان بقلب الهاتف"
        )
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
            val appCtx = getApplication<Application>().applicationContext
            PrayerScheduleStore.saveTimings(appCtx, currentTimings24)
            PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
            PrayerCountdownService.syncState(appCtx)
            refreshFuturePrayerLocks(System.currentTimeMillis())
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

        // Automatically lock/unlock today's 5 prayers in the daily tracker as real time progresses
        refreshFuturePrayerLocks(now.timeInMillis)

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

    private val trackerPrayerKeyByTaskId = mapOf(
        "fajr" to "Fajr",
        "dhuhr" to "Dhuhr",
        "asr" to "Asr",
        "maghrib" to "Maghrib",
        "isha" to "Isha"
    )

    private fun isPrayerStillInFuture(taskId: String, nowMillis: Long): Boolean {
        val prayerKey = trackerPrayerKeyByTaskId[taskId] ?: return false
        val time24 = currentTimings24[prayerKey] ?: return false
        val prayerMillis = PrayerScheduleStore.parseTodayMillis(time24, nowMillis) ?: return false
        return nowMillis < prayerMillis
    }

    private fun refreshFuturePrayerLocks(nowMillis: Long) {
        val currentList = _worshipTasks.value
        var changed = false
        val updated = currentList.map { task ->
            val locked = isPrayerStillInFuture(task.id, nowMillis)
            if (task.isLockedFuture != locked) {
                changed = true
                task.copy(isLockedFuture = locked)
            } else {
                task
            }
        }
        if (changed) {
            _worshipTasks.value = updated
        }
    }

    fun toggleWorshipTask(taskId: String) {
        val nowMillis = System.currentTimeMillis()
        val target = _worshipTasks.value.find { it.id == taskId } ?: return
        if (target.isLockedFuture || isPrayerStillInFuture(taskId, nowMillis)) {
            emitToast("لم يحن وقت ${target.label} بعد")
            return
        }
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
        val nowMillis = System.currentTimeMillis()
        _worshipTasks.value = OfflineIslamicData.worshipTasks.map {
            it.copy(
                completed = false,
                isLockedFuture = isPrayerStillInFuture(it.id, nowMillis)
            )
        }
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
        val resolvedPage = offlineMushafBook?.findPageForAyah(bm.surahNumber, bm.ayahNumber)
            ?: if (bm.pageNumber > 1 || bm.surahNumber == 1) bm.pageNumber
            else _quranState.value.surahStartPages[bm.surahNumber] ?: 1
        pendingMushafSurah = bm.surahNumber
        pendingMushafAyah = bm.ayahNumber
        openMushafAtPage(resolvedPage)
    }

    // =========================================================================
    // MODULE B: QURAN — 100% OFFLINE 604-PAGE WAHY MUSHAF + ONLINE AUDIO
    // =========================================================================

    fun ensureOfflineMushafLoaded(forceRetry: Boolean = false) {
        if (offlineMushafBook != null && !forceRetry) return
        if (offlineMushafJob?.isActive == true) return

        offlineMushafJob = viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            _quranState.update {
                it.copy(
                    isLoadingOfflineMushaf = true,
                    mushafError = null,
                    mushafDownloadProgress = if (OfflineQuranRepository.isCachedLocally(appCtx)) {
                        "جاري فتح المصحف الشريف أوفلاين..."
                    } else {
                        "جاري تحميل المصحف الكامل (٦٠٤ صفحة) لمرة واحدة..."
                    }
                )
            }

            val result = OfflineQuranRepository.loadOrDownloadMushaf(appCtx) { status ->
                _quranState.update { it.copy(mushafDownloadProgress = status) }
            }

            result.onSuccess { book ->
                offlineMushafBook = book
                prefs.cacheSurahs(book.surahs)

                val targetPage = pendingMushafSurah?.let { sNum ->
                    book.findPageForAyah(sNum, pendingMushafAyah)
                } ?: _quranState.value.currentMushafPage.coerceIn(1, book.pages.size)
                pendingMushafSurah = null
                pendingMushafAyah = 1

                val pageData = book.pages.getOrNull(targetPage - 1)
                val primarySurah = pageData?.let { pd ->
                    book.surahs.find { it.number == pd.primarySurahNumber }
                }

                _quranState.update { state ->
                    state.copy(
                        surahs = book.surahs,
                        isLoadingList = false,
                        mushafPages = book.pages,
                        surahStartPages = book.surahStartPages,
                        juzStartPages = book.juzStartPages,
                        currentMushafPage = targetPage,
                        activeSurah = if (state.isMushafOpen) primarySurah ?: state.activeSurah else state.activeSurah,
                        isLoadingOfflineMushaf = false,
                        mushafDownloadProgress = null,
                        mushafError = null,
                        isMushafCachedOffline = true
                    )
                }
                if (_quranState.value.isMushafOpen && primarySurah != null && !audioState.value.isPlaying) {
                    audioPlayer.prepareSurahMetadata(primarySurah.number, primarySurah.name)
                }
            }.onFailure {
                _quranState.update {
                    it.copy(
                        isLoadingOfflineMushaf = false,
                        mushafDownloadProgress = null,
                        mushafError = "تعذّر تحميل نص المصحف الكامل لأول مرة. يرجى الاتصال بالإنترنت ثم الضغط على إعادة المحاولة ليتم حفظه أوفلاين للأبد."
                    )
                }
            }
        }
    }

    fun loadSurahList() {
        surahListRequested = true
        viewModelScope.launch {
            offlineMushafBook?.let { book ->
                _quranState.update { it.copy(surahs = book.surahs, isLoadingList = false) }
                return@launch
            }
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
        val targetPage = offlineMushafBook?.findPageForAyah(surah.number, scrollToAyah)
            ?: _quranState.value.surahStartPages[surah.number]
            ?: surah.startPage.coerceIn(1, 604)

        if (offlineMushafBook == null) {
            pendingMushafSurah = surah.number
            pendingMushafAyah = scrollToAyah
            ensureOfflineMushafLoaded(forceRetry = false)
        }

        _quranState.update {
            it.copy(
                activeSurah = surah,
                isMushafOpen = true,
                currentMushafPage = targetPage,
                lastSavedMushafPage = targetPage,
                targetScrollAyah = scrollToAyah
            )
        }
        viewModelScope.launch { prefs.saveLastMushafPage(targetPage) }
    }

    fun openMushafAtPage(pageNumber: Int) {
        val safePage = pageNumber.coerceIn(1, 604)
        val pageData = _quranState.value.mushafPages.getOrNull(safePage - 1)
        val primarySurah = pageData?.let { pd ->
            _quranState.value.surahs.find { it.number == pd.primarySurahNumber }
                ?: SurahSummary(
                    number = pd.primarySurahNumber,
                    name = pd.primarySurahName,
                    numberOfAyahs = 0,
                    revelationType = "Meccan",
                    startPage = safePage
                )
        }
        if (primarySurah != null && !audioState.value.isPlaying) {
            audioPlayer.prepareSurahMetadata(primarySurah.number, primarySurah.name)
        }
        if (offlineMushafBook == null) {
            ensureOfflineMushafLoaded(forceRetry = false)
        }
        _quranState.update {
            it.copy(
                isMushafOpen = true,
                currentMushafPage = safePage,
                lastSavedMushafPage = safePage,
                activeSurah = primarySurah ?: it.activeSurah
            )
        }
        viewModelScope.launch { prefs.saveLastMushafPage(safePage) }
    }

    fun openMushafAtLastPage() {
        openMushafAtPage(_quranState.value.lastSavedMushafPage)
    }

    fun onMushafPageChanged(pageNumber: Int) {
        val safePage = pageNumber.coerceIn(1, 604)
        val currentState = _quranState.value
        val pageData = currentState.mushafPages.getOrNull(safePage - 1)
        val primarySurah = pageData?.let { pd ->
            currentState.surahs.find { it.number == pd.primarySurahNumber }
                ?: SurahSummary(
                    number = pd.primarySurahNumber,
                    name = pd.primarySurahName,
                    numberOfAyahs = 0,
                    revelationType = "Meccan",
                    startPage = safePage
                )
        }
        if (primarySurah != null && !audioState.value.isPlaying) {
            audioPlayer.prepareSurahMetadata(primarySurah.number, primarySurah.name)
        }
        _quranState.update {
            it.copy(
                currentMushafPage = safePage,
                lastSavedMushafPage = safePage,
                activeSurah = primarySurah ?: it.activeSurah
            )
        }
        viewModelScope.launch { prefs.saveLastMushafPage(safePage) }
    }

    fun closeSurahReader() {
        // Keep Surah background audio playing if active; release Tafsir & Tajweed RAM immediately
        TafsirRepository.clearMemoryCache()
        TajweedEngine.clearCaches()
        _quranState.update {
            it.copy(
                isMushafOpen = false,
                activeSurah = null,
                ayahs = emptyList(),
                readerError = null,
                isFullScreenReading = false
            )
        }
    }

    fun updateQuranFontSize(sizeSp: Float) {
        viewModelScope.launch { prefs.setQuranFontSize(sizeSp.coerceIn(18f, 36f)) }
    }

    fun selectQuranViewMode(mode: QuranViewMode) {
        _quranState.update { it.copy(viewMode = mode) }
    }

    fun setMushafPaperTheme(theme: MushafPaperTheme) {
        viewModelScope.launch { prefs.setMushafPaperTheme(theme) }
    }

    fun toggleMushafNightMode() {
        val nextTheme = if (mushafPaperTheme.value == MushafPaperTheme.NIGHT) {
            MushafPaperTheme.CREAM
        } else {
            MushafPaperTheme.NIGHT
        }
        setMushafPaperTheme(nextTheme)
    }

    fun toggleTajweedColors() {
        val next = !tajweedColorsEnabled.value
        viewModelScope.launch { prefs.setTajweedColorsEnabled(next) }
        emitToast(if (next) "تم تفعيل تلوين أحكام التجويد" else "تم إيقاف تلوين التجويد")
    }

    fun setQuranPlaybackSpeed(speed: Float) {
        val safeSpeed = speed.coerceIn(0.5f, 2.0f)
        audioPlayer.setPlaybackSpeed(safeSpeed)
        ayahAudioPlayer.setPlaybackSpeed(safeSpeed)
        viewModelScope.launch { prefs.setQuranPlaybackSpeed(safeSpeed) }
        emitToast("سرعة التلاوة: ${QuranPlaybackSpeeds.formatLabel(safeSpeed)}")
    }

    fun toggleFullScreenReading() {
        _quranState.update { it.copy(isFullScreenReading = !it.isFullScreenReading) }
    }

    fun setWordByWordHighlightEnabled(enabled: Boolean) {
        audioPlayer.setWordByWordEnabled(enabled)
        viewModelScope.launch { prefs.setWordByWordHighlightEnabled(enabled) }
        emitToast(if (enabled) "تم تفعيل التظليل كلمة بكلمة" else "تم التبديل إلى تظليل الآية كاملة")
    }

    fun setSyncLatencyOffsetMs(offsetMs: Int) {
        val (isBluetooth, clamped) = audioPlayer.setLatencyOffsetForActiveRoute(offsetMs)
        viewModelScope.launch {
            if (isBluetooth) {
                prefs.setSyncOffsetBluetoothMs(clamped)
            } else {
                prefs.setSyncOffsetSpeakerMs(clamped)
            }
        }
    }

    fun seekToWordOrAyahWhilePlaying(
        surahNumber: Int,
        ayahNumber: Int,
        wordIndexZeroBased: Int?
    ): Boolean {
        return audioPlayer.seekToAyahOrWordIfLoaded(
            surahNumber = surahNumber,
            ayahNumber = ayahNumber,
            wordIndexZeroBased = wordIndexZeroBased
        )
    }

    /**
     * Mode 1: "استماع لهذه الآية فقط" (Play This Ayah Only)
     * Uses official Quran.com v4 millisecond timestamps on the single seamless Surah MP3
     * whenever the reciter supports them, stopping cleanly at `timestamp_to` (or repeating).
     * Smart fallback: for mp3quran.net reciters without official timestamps, plays full Surah cleanly
     * with highlighting disabled so there is never inaccurate highlighting.
     */
    fun playSinglePageAyahOnly(pageAyah: MushafPageAyah) {
        stopOtherAudio(except = AudioSource.SURAH)
        if (audioPlayer.hasOfficialTimestampsSupportForCurrentReciter()) {
            audioPlayer.playSurahFromAyah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                startAyahNumber = pageAyah.numberInSurah,
                singleAyahOnly = true,
                repeatMode = ayahAudioState.value.repeatMode
            )
        } else {
            audioPlayer.playSurah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                forceRestart = true
            )
            emitToast("جاري تشغيل ${pageAyah.surahName} كاملة بصوت ${audioState.value.selectedReciter.arabicName}")
        }
    }

    /**
     * Mode 2: "استماع من هذه الآية" (Continue From Here)
     * Starts continuous seamless recitation from the exact millisecond `timestamp_from` of the selected Ayah
     * and automatically advances through following Ayahs, Mushaf pages, and the next Surah.
     */
    fun playContinueFromPageAyah(pageAyah: MushafPageAyah) {
        stopOtherAudio(except = AudioSource.SURAH)
        val advanceSurah: (Int) -> Unit = { finishedSurah ->
            if (finishedSurah < 114) {
                val nextNum = finishedSurah + 1
                audioPlayer.playSurah(nextNum, resolveSurahName(nextNum), forceRestart = true)
            }
        }
        if (audioPlayer.hasOfficialTimestampsSupportForCurrentReciter()) {
            audioPlayer.playSurahFromAyah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                startAyahNumber = pageAyah.numberInSurah,
                singleAyahOnly = false,
                repeatMode = AyahRepeatMode.ONCE,
                onSurahCompleted = advanceSurah
            )
        } else {
            audioPlayer.playSurah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                forceRestart = true,
                onSurahCompleted = advanceSurah
            )
            emitToast("جاري تشغيل ${pageAyah.surahName} كاملة بصوت ${audioState.value.selectedReciter.arabicName}")
        }
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

    private fun findPageAyah(surahNumber: Int, ayahNumber: Int): MushafPageAyah? {
        val pages = _quranState.value.mushafPages
        for (page in pages) {
            for (block in page.blocks) {
                if (block.surahNumber == surahNumber) {
                    val found = block.ayahs.find { it.numberInSurah == ayahNumber }
                    if (found != null) return found
                }
            }
        }
        return null
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

    fun loadAyahTafsir(
        surahNumber: Int,
        ayahNumber: Int,
        book: TafsirBook = _tafsirState.value.selectedBook
    ) {
        tafsirFetchJob?.cancel()
        val appCtx = getApplication<Application>().applicationContext
        _tafsirState.update {
            it.copy(
                selectedBook = book,
                currentSurahNumber = surahNumber,
                currentAyahNumber = ayahNumber,
                currentText = null,
                isLoading = true,
                errorMessage = null
            )
        }
        tafsirFetchJob = viewModelScope.launch {
            if (!tafsirStatusesLoaded) {
                tafsirStatusesLoaded = true
                val downloaded = withContext(Dispatchers.IO) {
                    TafsirRepository.getDownloadedBookIds(appCtx)
                }
                _tafsirState.update { it.copy(downloadedBookIds = downloaded) }
            }
            val result = TafsirRepository.getAyahTafsir(
                context = appCtx,
                book = book,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber
            )
            result.onSuccess { text ->
                _tafsirState.update { state ->
                    if (state.selectedBook == book &&
                        state.currentSurahNumber == surahNumber &&
                        state.currentAyahNumber == ayahNumber
                    ) {
                        state.copy(
                            currentText = text,
                            isLoading = false,
                            errorMessage = null
                        )
                    } else {
                        state
                    }
                }
            }.onFailure { err ->
                _tafsirState.update { state ->
                    if (state.selectedBook == book &&
                        state.currentSurahNumber == surahNumber &&
                        state.currentAyahNumber == ayahNumber
                    ) {
                        state.copy(
                            currentText = null,
                            isLoading = false,
                            errorMessage = err.message
                                ?: "تعذّر تحميل التفسير حالياً. تأكد من الاتصال بالإنترنت أو حمّل التفسير للعمل بدون نت."
                        )
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun selectTafsirBook(book: TafsirBook) {
        val current = _tafsirState.value
        loadAyahTafsir(
            surahNumber = current.currentSurahNumber,
            ayahNumber = current.currentAyahNumber,
            book = book
        )
    }

    fun retryCurrentAyahTafsir() {
        val current = _tafsirState.value
        loadAyahTafsir(
            surahNumber = current.currentSurahNumber,
            ayahNumber = current.currentAyahNumber,
            book = current.selectedBook
        )
    }

    fun downloadTafsirBookOffline(book: TafsirBook) {
        if (_tafsirState.value.downloadingProgress.containsKey(book.bookId)) return
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _tafsirState.update { state ->
                state.copy(
                    downloadingProgress = state.downloadingProgress + (book.bookId to 0.02f)
                )
            }
            val result = TafsirRepository.downloadFullBook(appCtx, book) { progress ->
                _tafsirState.update { state ->
                    state.copy(
                        downloadingProgress = state.downloadingProgress + (book.bookId to progress)
                    )
                }
            }
            result.onSuccess {
                _tafsirState.update { state ->
                    state.copy(
                        downloadedBookIds = state.downloadedBookIds + book.bookId,
                        downloadingProgress = state.downloadingProgress - book.bookId
                    )
                }
                emitToast("تم تحميل ${book.arabicName} للعمل بدون إنترنت ✓")
                val latest = _tafsirState.value
                if (latest.selectedBook == book && latest.currentText.isNullOrBlank()) {
                    loadAyahTafsir(latest.currentSurahNumber, latest.currentAyahNumber, book)
                }
            }.onFailure {
                _tafsirState.update { state ->
                    state.copy(
                        downloadingProgress = state.downloadingProgress - book.bookId
                    )
                }
                emitToast("تعذّر تحميل ${book.arabicName} أوفلاين. يرجى التحقق من الاتصال.")
            }
        }
    }

    fun saveQuranBookmark(ayahNumber: Int = 1) {
        val surah = _quranState.value.activeSurah ?: return
        val pageNum = _quranState.value.currentMushafPage
        saveMushafPageBookmark(pageNum, surah.number, surah.name, ayahNumber)
    }

    fun saveMushafPageBookmark(
        pageNumber: Int,
        surahNumber: Int,
        surahName: String,
        ayahNumber: Int
    ) {
        val safePage = pageNumber.coerceIn(1, 604)
        viewModelScope.launch {
            prefs.saveBookmark(
                QuranBookmark(
                    surahNumber = surahNumber,
                    surahName = surahName,
                    ayahNumber = ayahNumber,
                    pageNumber = safePage
                )
            )
            emitToast("تم حفظ العلامة: $surahName • صفحة $safePage")
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
        // Release full reciters list from RAM when modal closes (reloads fast from reciters_cache.json on demand)
        _apiReciters.value = emptyList()
        recitersRequested = false
    }

    fun loadApiReciters(forceRefresh: Boolean = false) {
        recitersRequested = true
        viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            _isRecitersLoading.value = true
            _recitersError.value = null

            // 1. Instant offline load from filesDir/reciters_cache.json when not forcing remote refresh
            if (!forceRefresh && ReciterRepository.hasLocalCache(appCtx)) {
                val cachedRepo = ReciterRepository.loadCachedReciters(appCtx)
                if (cachedRepo.isNotEmpty()) {
                    _apiReciters.value = cachedRepo
                    _isRecitersLoading.value = false
                    return@launch
                }
            }

            // 2. Sync from remote GitHub JSON repository (DEFAULT_RECITERS_REPO_URL) & save to reciters_cache.json
            val repoResult = ReciterRepository.syncRecitersFromRepo(appCtx)
            if (repoResult.isSuccess) {
                val repoList = repoResult.getOrNull().orEmpty()
                if (repoList.isNotEmpty()) {
                    _apiReciters.value = repoList
                    prefs.cacheReciters(repoList)
                    _isRecitersLoading.value = false
                    if (forceRefresh) {
                        emitToast("تم تحديث قائمة القراء (${repoList.size} قارئ)")
                    }
                    return@launch
                }
            }

            // 3. Fallback to local reciters_cache.json or cached MP3Quran list if offline
            val fallbackFileCache = ReciterRepository.loadCachedReciters(appCtx)
            if (fallbackFileCache.isNotEmpty()) {
                _apiReciters.value = fallbackFileCache
                _isRecitersLoading.value = false
                if (forceRefresh) {
                    emitToast("تعذّر الاتصال، تم تحميل قائمة القراء المحفوظة محلياً")
                }
                return@launch
            }

            val legacyCache = prefs.getCachedReciters()
            if (!legacyCache.isNullOrEmpty()) {
                _apiReciters.value = legacyCache
                _isRecitersLoading.value = false
                if (forceRefresh) emitToast("تعذّر الاتصال، تم استخدام القائمة المحفوظة")
                return@launch
            }

            // 4. Secondary remote fallback to MP3Quran API if GitHub repo was unreachable on first launch
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
                _recitersError.value = "تعذّر تحديث قائمة القراء، يتم عرض قائمة أشهر القراء"
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
        val reciter = state.selectedReciter
        val url = state.activeStreamUrl ?: reciter.buildPrimaryAudioUrl(surahNum)
        val downloadKey = "${reciter.id}_$surahNum"
        val appCtx = getApplication<Application>().applicationContext

        if (activeSurahAudioDownloads.contains(downloadKey)) {
            emitToast("جاري تحميل $surahName حالياً...")
            return
        }

        viewModelScope.launch {
            val alreadySaved = withContext(Dispatchers.IO) {
                PersistentOfflineStorage.isSurahAudioDownloaded(appCtx, reciter.id, surahNum)
            }
            if (alreadySaved) {
                emitToast("$surahName محفوظة بالفعل أوفلاين على مساحة الجهاز ✓")
                return@launch
            }

            activeSurahAudioDownloads.add(downloadKey)
            emitToast("جاري حفظ $surahName أوفلاين (${reciter.arabicName})...")
            try {
                val result = PersistentOfflineStorage.downloadSurahAudioOffline(
                    context = appCtx,
                    reciterId = reciter.id,
                    surahNumber = surahNum,
                    streamUrl = url
                )
                result.onSuccess {
                    emitToast("تم حفظ $surahName أوفلاين على مساحة الجهاز ✓")
                }.onFailure {
                    emitToast("تعذّر تحميل $surahName، تأكد من الاتصال بالإنترنت")
                }
            } finally {
                activeSurahAudioDownloads.remove(downloadKey)
            }
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
        val items = _azkarState.value.categories[category]
            ?: OfflineIslamicData.azkarCategories[category].orEmpty()
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
    // MODULE D: OFFLINE HADITH LIBRARY (8 AUTHENTIC BOOKS + DOWNLOAD MANAGER)
    // =========================================================================

    private var hadithNawawiAutoTried = false

    fun onHadithLibraryOpened() {
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            val statuses = withContext(Dispatchers.IO) {
                OfflineHadithRepository.getAllBookStatuses(appCtx)
            }
            if (!hadithFavoritesLoaded) {
                hadithFavoritesLoaded = true
                val savedFavorites = OfflineHadithRepository.loadSavedFavorites(appCtx)
                _hadithState.update {
                    it.copy(bookStatuses = statuses, savedFavoriteHadiths = savedFavorites)
                }
            } else {
                _hadithState.update { it.copy(bookStatuses = statuses) }
            }

            // Auto-download/cache الأربعون النووية (ara-nawawi.min.json) on first open so it's ready offline immediately
            val nawawiDownloaded = statuses[HadithBookCollection.NAWAWI.id]?.isDownloaded == true
            if (!nawawiDownloaded && !hadithNawawiAutoTried) {
                hadithNawawiAutoTried = true
                downloadHadithBookOffline(HadithBookCollection.NAWAWI, isAutoFirstOpen = true)
            }
        }
    }

    fun selectHadithSubTab(subTab: Int) {
        _hadithState.update {
            it.copy(
                activeSubTab = subTab.coerceIn(0, 1),
                openBook = null
            )
        }
    }

    fun openHadithBook(book: HadithBookCollection) {
        val appCtx = getApplication<Application>().applicationContext
        val savedResume = lastHadithResume.value
        val resumeIndex = if (savedResume?.first == book) savedResume.second else 0
        val initialVisible = maxOf(25, resumeIndex + 15)
        _hadithState.update {
            it.copy(
                openBook = book,
                searchQuery = "",
                visibleCount = initialVisible,
                bookError = null
            )
        }
        viewModelScope.launch {
            prefs.saveLastHadithReadingPosition(book.id, resumeIndex)
        }

        val alreadyInMemory = _hadithState.value.loadedCollections[book]
        if (!alreadyInMemory.isNullOrEmpty()) return

        viewModelScope.launch {
            val isDownloaded = withContext(Dispatchers.IO) {
                OfflineHadithRepository.isBookDownloaded(appCtx, book)
            }
            if (isDownloaded) {
                _hadithState.update { it.copy(isLoadingBook = true, bookError = null) }
                val res = OfflineHadithRepository.loadBookFromLocalStorage(appCtx, book)
                res.onSuccess { list ->
                    val refreshedStatuses = withContext(Dispatchers.IO) {
                        OfflineHadithRepository.getAllBookStatuses(appCtx)
                    }
                    _hadithState.update { state ->
                        // Keep ONLY the currently open Hadith book in RAM to protect 2GB-3GB devices
                        state.copy(
                            loadedCollections = mapOf(book to list),
                            bookStatuses = refreshedStatuses,
                            isLoadingBook = false,
                            bookError = null
                        )
                    }
                }.onFailure { err ->
                    _hadithState.update {
                        it.copy(
                            isLoadingBook = false,
                            bookError = err.message ?: "تعذّر فتح الملف المحلي للكتاب"
                        )
                    }
                }
            } else if (book.autoDownloadOnFirstOpen && !_hadithState.value.downloadingProgress.containsKey(book.id)) {
                downloadHadithBookOffline(book, isAutoFirstOpen = true)
            }
        }
    }

    fun closeHadithBook() {
        // Immediately release the loaded Hadith book array from memory so GC frees RAM
        OfflineHadithRepository.clearMemoryCache()
        _hadithState.update {
            it.copy(
                openBook = null,
                loadedCollections = emptyMap(),
                searchQuery = "",
                visibleCount = 25,
                expandedUids = emptySet(),
                bookError = null
            )
        }
    }

    fun saveHadithScrollIndex(book: HadithBookCollection, firstVisibleIndex: Int) {
        if (firstVisibleIndex < 0) return
        viewModelScope.launch {
            prefs.saveLastHadithReadingPosition(book.id, firstVisibleIndex)
        }
    }

    fun downloadHadithBookOffline(
        book: HadithBookCollection,
        isAutoFirstOpen: Boolean = false
    ) {
        if (_hadithState.value.downloadingProgress.containsKey(book.id)) return
        val appCtx = getApplication<Application>().applicationContext

        viewModelScope.launch {
            _hadithState.update { state ->
                state.copy(
                    downloadingProgress = state.downloadingProgress + (book.id to 0.02f),
                    bookError = null
                )
            }
            val result = OfflineHadithRepository.downloadBookOffline(appCtx, book) { progress ->
                _hadithState.update { state ->
                    state.copy(
                        downloadingProgress = state.downloadingProgress + (book.id to progress)
                    )
                }
            }
            result.onSuccess { parsedList ->
                val refreshedStatuses = withContext(Dispatchers.IO) {
                    OfflineHadithRepository.getAllBookStatuses(appCtx)
                }
                _hadithState.update { state ->
                    state.copy(
                        loadedCollections = if (state.openBook == book) mapOf(book to parsedList) else state.loadedCollections,
                        bookStatuses = refreshedStatuses,
                        downloadingProgress = state.downloadingProgress - book.id,
                        isLoadingBook = false,
                        bookError = null
                    )
                }
                emitToast("تم تحميل ${book.arabicTitle} بنجاح (${parsedList.size} حديث) • متاح بدون نت ✓")
            }.onFailure { err ->
                _hadithState.update { state ->
                    state.copy(
                        downloadingProgress = state.downloadingProgress - book.id,
                        isLoadingBook = false,
                        bookError = if (state.openBook == book) {
                            err.message ?: "تعذّر التحميل، تأكد من الاتصال بالإنترنت ثم حاول مجدداً"
                        } else {
                            state.bookError
                        }
                    )
                }
                if (!isAutoFirstOpen) {
                    emitToast("تعذّر تحميل ${book.arabicTitle}. يرجى التحقق من الاتصال بالإنترنت.")
                }
            }
        }
    }

    fun deleteHadithBookOffline(book: HadithBookCollection) {
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            OfflineHadithRepository.deleteBookOffline(appCtx, book)
            val refreshedStatuses = withContext(Dispatchers.IO) {
                OfflineHadithRepository.getAllBookStatuses(appCtx)
            }
            _hadithState.update { state ->
                state.copy(
                    loadedCollections = state.loadedCollections - book,
                    bookStatuses = refreshedStatuses,
                    openBook = if (state.openBook == book) null else state.openBook
                )
            }
            emitToast("تم حذف ${book.arabicTitle} لتوفير مساحة التخزين")
        }
    }

    fun updateHadithSearch(query: String) {
        _hadithState.update { it.copy(searchQuery = query, visibleCount = 25) }
    }

    fun updateHadithFavoritesSearch(query: String) {
        _hadithState.update { it.copy(favoritesSearchQuery = query) }
    }

    fun toggleHadithFontFamily() {
        _hadithState.update { it.copy(useCairoFont = !it.useCairoFont) }
    }

    fun loadMoreHadiths() {
        _hadithState.update { it.copy(visibleCount = it.visibleCount + 25) }
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

    fun toggleFavoriteHadith(hadith: LibraryHadith) {
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            val currentUids = favoriteHadithUids.value
            val added = !currentUids.contains(hadith.uid)
            val nextUids = if (added) currentUids + hadith.uid else currentUids - hadith.uid
            prefs.saveFavoriteHadithUids(nextUids)

            val currentList = _hadithState.value.savedFavoriteHadiths
            val nextList = if (added) {
                listOf(hadith) + currentList.filterNot { it.uid == hadith.uid }
            } else {
                currentList.filterNot { it.uid == hadith.uid }
            }
            _hadithState.update { it.copy(savedFavoriteHadiths = nextList) }
            OfflineHadithRepository.saveFavorites(appCtx, nextList)
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
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            _cachedAdhanIds.value = withContext(Dispatchers.IO) {
                MuezzinRepository.getAllCachedAdhanIds(appCtx)
            }
            if (!muezzinCatalogLoaded) {
                muezzinCatalogLoaded = true
                if (withContext(Dispatchers.IO) { MuezzinRepository.hasCatalogCache(appCtx) }) {
                    val cachedList = MuezzinRepository.loadCachedCatalog(appCtx)
                    if (cachedList.isNotEmpty()) {
                        _repoMuezzins.value = cachedList
                        _muezzinRepoStatus.value = "محفوظ محلياً (${cachedList.size} مؤذن)"
                    } else {
                        syncMuezzinRepo(isAutoFirstLaunch = true)
                    }
                } else {
                    syncMuezzinRepo(isAutoFirstLaunch = true)
                }
            }
        }
    }

    fun closeMuezzinLibrary() {
        _muezzinLibraryOpen.value = false
    }

    fun syncMuezzinRepo(isAutoFirstLaunch: Boolean = false) {
        if (_isSyncingMuezzinRepo.value) return
        viewModelScope.launch {
            _isSyncingMuezzinRepo.value = true
            val appCtx = getApplication<Application>().applicationContext
            val result = MuezzinRepository.syncCatalogFromRemote(appCtx)
            result.onSuccess { syncedList ->
                _repoMuezzins.value = syncedList
                _cachedAdhanIds.value = withContext(Dispatchers.IO) {
                    MuezzinRepository.getAllCachedAdhanIds(appCtx)
                }
                _muezzinRepoStatus.value = "تم التحديث (${syncedList.size} مؤذن)"
                emitToast("تم تحديث قائمة المؤذنين: تم تحميل ${syncedList.size} مؤذناً")
            }.onFailure {
                // Cleanly fall back to offline cache without crashing
                val cached = MuezzinRepository.loadCachedCatalog(appCtx)
                if (cached.isNotEmpty()) {
                    _repoMuezzins.value = cached
                    _muezzinRepoStatus.value = "وضع عدم الاتصال — نسخة محفوظة (${cached.size} مؤذن)"
                } else {
                    _muezzinRepoStatus.value = "تعذّر الاتصال — يتم عرض القائمة المدمجة"
                }
                if (!isAutoFirstLaunch) {
                    emitToast(
                        if (cached.isNotEmpty()) {
                            "تعذّر التحديث — تم تحميل ${cached.size} مؤذناً من النسخة المحفوظة"
                        } else {
                            "تعذّر التحديث، تأكد من الاتصال بالإنترنت"
                        }
                    )
                }
            }
            _isSyncingMuezzinRepo.value = false
        }
    }

    fun downloadMuezzinForOffline(muezzin: MuezzinItem) {
        val url = muezzin.primaryUrl.trim()
        if (url.isBlank()) {
            emitToast("لا يوجد رابط MP3 مباشر لهذا المؤذن للتحميل")
            return
        }
        if (_downloadingMuezzinIds.value.contains(muezzin.id)) return

        viewModelScope.launch {
            _downloadingMuezzinIds.update { it + muezzin.id }
            val appCtx = getApplication<Application>().applicationContext
            val result = MuezzinRepository.downloadAdhanMp3(appCtx, muezzin.id, url)
            _downloadingMuezzinIds.update { it - muezzin.id }

            result.onSuccess {
                _cachedAdhanIds.value = withContext(Dispatchers.IO) {
                    MuezzinRepository.getAllCachedAdhanIds(appCtx)
                }
                emitToast("تم تحميل أذان ${muezzin.name} للتشغيل بدون إنترنت ✓")
            }.onFailure {
                emitToast("تعذّر تحميل صوت ${muezzin.name}، تأكد من الاتصال بالإنترنت")
            }
        }
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
            val appCtx = getApplication<Application>().applicationContext
            withContext(Dispatchers.IO) {
                MuezzinRepository.deleteCachedAdhan(appCtx, muezzin.id)
                _cachedAdhanIds.value = MuezzinRepository.getAllCachedAdhanIds(appCtx)
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
                    val dir = PersistentOfflineStorage.adhanCustomDir(appCtx)
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
                    val dir = PersistentOfflineStorage.adhanCustomDir(appCtx)
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

    // =========================================================================
    // MODULE H: IN-APP DIRECT AUTO-UPDATE (التحديث التلقائي المباشر داخل التطبيق)
    // =========================================================================

    fun checkForAppUpdate(manual: Boolean) {
        val current = _appUpdateState.value
        if (current.isChecking || current.stage == AppUpdateDialogStage.DOWNLOADING) return

        viewModelScope.launch {
            _appUpdateState.update { it.copy(isChecking = true) }
            val appCtx = getApplication<Application>().applicationContext
            val result = AppUpdateManager.checkForNewerRelease(
                context = appCtx,
                currentVersionName = BuildConfig.VERSION_NAME
            )
            result.onSuccess { release ->
                if (release != null) {
                    _appUpdateState.update {
                        it.copy(
                            isChecking = false,
                            availableRelease = release,
                            stage = AppUpdateDialogStage.UPDATE_AVAILABLE,
                            errorMessage = null
                        )
                    }
                } else {
                    _appUpdateState.update { it.copy(isChecking = false) }
                    if (manual) {
                        emitToast("التطبيق محدّث لآخر إصدار (${BuildConfig.VERSION_NAME})")
                    }
                }
            }.onFailure {
                _appUpdateState.update { it.copy(isChecking = false) }
                if (manual) {
                    emitToast("تعذّر التحقق من التحديثات، تأكد من الاتصال بالإنترنت")
                }
            }
        }
    }

    fun startAppUpdateDownload() {
        val release = _appUpdateState.value.availableRelease ?: return
        appUpdateDownloadJob?.cancel()
        appUpdateDownloadJob = viewModelScope.launch {
            _appUpdateState.update {
                it.copy(
                    stage = AppUpdateDialogStage.DOWNLOADING,
                    downloadedBytes = 0L,
                    totalBytes = release.assetSizeBytes,
                    progressPercent = 0,
                    errorMessage = null
                )
            }
            val appCtx = getApplication<Application>().applicationContext
            val result = AppUpdateManager.downloadUpdateApk(
                context = appCtx,
                downloadUrl = release.downloadUrl,
                expectedSizeBytes = release.assetSizeBytes,
                onProgress = { progress ->
                    _appUpdateState.update { state ->
                        if (state.stage == AppUpdateDialogStage.DOWNLOADING) {
                            state.copy(
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = progress.totalBytes,
                                progressPercent = progress.progressPercent
                            )
                        } else {
                            state
                        }
                    }
                }
            )

            result.onSuccess { apkFile ->
                _appUpdateState.update {
                    it.copy(
                        stage = AppUpdateDialogStage.READY_TO_INSTALL,
                        downloadedApkFile = apkFile,
                        progressPercent = 100
                    )
                }
            }.onFailure { err ->
                _appUpdateState.update {
                    it.copy(
                        stage = AppUpdateDialogStage.ERROR,
                        errorMessage = err.message ?: "تعذّر تحميل ملف التحديث، تأكد من الاتصال بالإنترنت"
                    )
                }
            }
        }
    }

    fun cancelAppUpdateDownload() {
        appUpdateDownloadJob?.cancel()
        appUpdateDownloadJob = null
        _appUpdateState.update {
            it.copy(
                stage = AppUpdateDialogStage.NONE,
                downloadedBytes = 0L,
                totalBytes = -1L,
                progressPercent = 0
            )
        }
        emitToast("تم إلغاء تحميل التحديث")
    }

    fun markInstallPermissionRequired() {
        _appUpdateState.update { it.copy(stage = AppUpdateDialogStage.INSTALL_PERMISSION_REQUIRED) }
    }

    fun installDownloadedApk(apkFile: File) {
        val appCtx = getApplication<Application>().applicationContext
        val launched = AppUpdateManager.launchApkInstaller(appCtx, apkFile)
        launched.onSuccess {
            _appUpdateState.update { it.copy(stage = AppUpdateDialogStage.NONE) }
        }.onFailure {
            _appUpdateState.update {
                it.copy(
                    stage = AppUpdateDialogStage.ERROR,
                    errorMessage = "تعذّر فتح مثبّت التطبيقات، يرجى المحاولة مرة أخرى"
                )
            }
        }
    }

    fun dismissAppUpdateDialog() {
        if (_appUpdateState.value.stage == AppUpdateDialogStage.DOWNLOADING) return
        _appUpdateState.update { it.copy(stage = AppUpdateDialogStage.NONE, errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        appUpdateDownloadJob?.cancel()
        // Do NOT kill active background Quran or Adhan foreground services on ViewModel clear
        // so playback survives Activity recreation / backgrounding; free heavy RAM caches only.
        fajrAlarmPlayer.stopRinging()
        TafsirRepository.clearMemoryCache()
        TajweedEngine.clearCaches()
        QuranTimingRepository.clearMemoryCache()
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
