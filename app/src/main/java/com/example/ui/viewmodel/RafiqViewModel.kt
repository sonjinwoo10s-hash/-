package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.alarm.AppPermissionsStatus
import com.example.alarm.AstronomicalPrayerCalculator
import com.example.alarm.OnboardingPrefs
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerCalculationMethod
import com.example.alarm.PrayerLogStats
import com.example.alarm.PrayerLogStore
import com.example.alarm.PrayerScheduleStore
import com.example.alarm.PrayerTimeAdjustments
import com.example.alarm.QadaaItem
import com.example.audio.AdhanAudioPlayer
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioPlayer
import com.example.audio.AyahAudioState
import com.example.audio.EveryAyahMapper
import com.example.audio.FajrAlarmPlayer
import com.example.audio.QuranAudioPlayer
import com.example.audio.QuranPlaybackSpeeds
import com.example.data.local.AppPreferences
import com.example.data.local.HadithLibraryData
import com.example.data.local.MuezzinLibraryData
import com.example.data.local.OfflineIslamicData
import com.example.data.local.PersistentOfflineStorage
import com.example.quran.TajweedEngine
import com.example.service.AdhanPlaybackService
import com.example.service.PrayerCountdownService
import com.example.service.QuranMediaEngine
import com.example.data.model.AdhanPlaybackStatus
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
import com.example.data.model.PrayerAlertCategory
import com.example.data.model.PrayerAlertSound
import com.example.data.model.PrayerAlertsSettings
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
import com.example.data.repository.GlobalLocationConfig
import com.example.data.repository.GlobalLocationRepository
import com.example.data.repository.GlobalSearchResult
import com.example.data.repository.MuezzinRepository
import com.example.data.repository.MushafPageAyah
import com.example.data.repository.MushafPageData
import com.example.data.repository.OfflineAdhkarRepository
import com.example.data.repository.OfflineHadithRepository
import com.example.data.repository.OfflineMushafBook
import com.example.data.repository.OfflineQuranRepository
import com.example.data.repository.PrayerAlertRepository
import com.example.data.repository.QuranTimingRepository
import com.example.ui.components.AppUpdateDialogStage
import com.example.ui.components.AppUpdateUiState
import com.example.update.AppUpdateManager
import com.example.data.repository.ReciterRepository
import com.example.data.repository.TafsirBook
import com.example.data.repository.TafsirRepository
import com.example.sensor.QiblaMath
import com.example.widget.PrayerTimesWidgetProvider
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
    val isMushafCachedOffline: Boolean = false,
    val downloadedSurahAudioKeys: Set<String> = emptySet(),
    val downloadingSurahAudioKeys: Set<String> = emptySet()
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
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
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

    // Unified real-time Ayah ("surah:ayah") and Word-by-Word (0-based word index) highlight StateFlows
    // observed directly by the Mushaf UI.
    val activeVerseKey: StateFlow<String?> = combine(
        audioPlayer.activeVerseKey,
        ayahAudioPlayer.state
    ) { surahVerseKey, ayahSt ->
        ayahSt.activeVerseKey ?: surahVerseKey
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeWordIndex: StateFlow<Int?> = combine(
        audioPlayer.activeWordIndex,
        ayahAudioPlayer.state
    ) { surahWordIdx, ayahSt ->
        if (ayahSt.isPlaying || ayahSt.isBuffering) ayahSt.activeWordIndex else surahWordIdx
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _defaultMuezzinId = MutableStateFlow(
        PrayerScheduleStore.getDefaultMuezzinId(application.applicationContext)
    )
    val defaultMuezzinId: StateFlow<String> = _defaultMuezzinId.asStateFlow()

    private val _fajrMuezzinId = MutableStateFlow(
        PrayerScheduleStore.getFajrMuezzinId(application.applicationContext)
    )
    val fajrMuezzinId: StateFlow<String> = _fajrMuezzinId.asStateFlow()

    private val _useDedicatedFajrAdhan = MutableStateFlow(
        PrayerScheduleStore.isUseDedicatedFajr(application.applicationContext)
    )
    val useDedicatedFajrAdhan: StateFlow<Boolean> = _useDedicatedFajrAdhan.asStateFlow()

    private val _perPrayerMuezzinEnabled = MutableStateFlow(
        PrayerScheduleStore.isPerPrayerMuezzinEnabled(application.applicationContext)
    )
    val perPrayerMuezzinEnabled: StateFlow<Boolean> = _perPrayerMuezzinEnabled.asStateFlow()

    private val _perPrayerMuezzinIds = MutableStateFlow(
        PrayerScheduleStore.getPerPrayerMuezzinIds(application.applicationContext)
    )
    val perPrayerMuezzinIds: StateFlow<Map<String, String>> = _perPrayerMuezzinIds.asStateFlow()

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
                val hasValidLocal = !ov.localFilePath.isNullOrBlank()
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
        list.find { it.id == id } ?: run {
            val snap = PrayerScheduleStore.getDefaultMuezzinSnapshot(getApplication())
            if (snap.muezzinId == id && snap.muezzinName.isNotBlank()) {
                MuezzinItem(
                    id = snap.muezzinId,
                    name = snap.muezzinName,
                    subtitle = "الأذان الافتراضي المحفوظ",
                    region = MuezzinRegion.HARAMAIN.name,
                    primaryUrl = snap.remoteUrl.orEmpty(),
                    localFilePath = snap.localFilePath
                )
            } else {
                list.firstOrNull { it.hasAudioSource() }
                    ?: list.firstOrNull()
                    ?: MuezzinLibraryData.builtInMuezzins.first()
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        runCatching {
            PrayerScheduleStore.getDefaultMuezzinSnapshot(application.applicationContext).let { snap ->
                MuezzinLibraryData.builtInMuezzins.find { it.id == snap.muezzinId }
                    ?: MuezzinItem(
                        id = snap.muezzinId,
                        name = snap.muezzinName,
                        subtitle = "الأذان الافتراضي المحفوظ",
                        region = MuezzinRegion.HARAMAIN.name,
                        primaryUrl = snap.remoteUrl.orEmpty(),
                        localFilePath = snap.localFilePath
                    )
            }
        }.getOrElse { MuezzinLibraryData.builtInMuezzins.first() }
    )

    private val _muezzinLibraryOpen = MutableStateFlow(false)
    val muezzinLibraryOpen: StateFlow<Boolean> = _muezzinLibraryOpen.asStateFlow()

    // Smart Fajr Alarm UI State
    private val _fajrAlarmSettingsOpen = MutableStateFlow(false)
    val fajrAlarmSettingsOpen: StateFlow<Boolean> = _fajrAlarmSettingsOpen.asStateFlow()

    // Pre-Adhan & Post-Adhan (Iqama) Alerts State (ON by default)
    private val _prayerAlertsSettingsOpen = MutableStateFlow(false)
    val prayerAlertsSettingsOpen: StateFlow<Boolean> = _prayerAlertsSettingsOpen.asStateFlow()

    private val _prayerAlertsSettings = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.getPrayerAlertsSettings(application.applicationContext)
        }.getOrDefault(PrayerAlertsSettings())
    )
    val prayerAlertsSettings: StateFlow<PrayerAlertsSettings> = _prayerAlertsSettings.asStateFlow()

    private val _alertSoundsCatalog = MutableStateFlow<List<PrayerAlertSound>>(emptyList())
    val alertSoundsCatalog: StateFlow<List<PrayerAlertSound>> = _alertSoundsCatalog.asStateFlow()

    private val _isSyncingAlertSounds = MutableStateFlow(false)
    val isSyncingAlertSounds: StateFlow<Boolean> = _isSyncingAlertSounds.asStateFlow()

    private val _cachedAlertSoundIds = MutableStateFlow<Set<String>>(emptySet())
    val cachedAlertSoundIds: StateFlow<Set<String>> = _cachedAlertSoundIds.asStateFlow()

    private val _downloadingAlertSoundIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingAlertSoundIds: StateFlow<Set<String>> = _downloadingAlertSoundIds.asStateFlow()

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

    // Global Auto-Location, Hijri Offset, Prayer Log, Auto-Silent DND, Friday Al-Kahf
    private val _globalLocation = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.getGlobalLocation(application.applicationContext)
        }.getOrDefault(GlobalLocationConfig())
    )
    val globalLocation: StateFlow<GlobalLocationConfig> = _globalLocation.asStateFlow()

    private val _globalLocationModalOpen = MutableStateFlow(false)
    val globalLocationModalOpen: StateFlow<Boolean> = _globalLocationModalOpen.asStateFlow()

    private val _showCalculationMethodPickerInModal = MutableStateFlow(false)
    val showCalculationMethodPickerInModal: StateFlow<Boolean> =
        _showCalculationMethodPickerInModal.asStateFlow()

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<GlobalSearchResult>>(emptyList())
    val globalSearchResults: StateFlow<List<GlobalSearchResult>> = _globalSearchResults.asStateFlow()

    private val _isSearchingLocations = MutableStateFlow(false)
    val isSearchingLocations: StateFlow<Boolean> = _isSearchingLocations.asStateFlow()

    private val _hijriDayOffset = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.getHijriDayOffset(application.applicationContext)
        }.getOrDefault(0)
    )
    val hijriDayOffset: StateFlow<Int> = _hijriDayOffset.asStateFlow()

    private val _prayerLogStats = MutableStateFlow(PrayerLogStats())
    val prayerLogStats: StateFlow<PrayerLogStats> = _prayerLogStats.asStateFlow()

    private val _prayerTrackerOpen = MutableStateFlow(false)
    val prayerTrackerOpen: StateFlow<Boolean> = _prayerTrackerOpen.asStateFlow()

    private val _pendingQadaaItems = MutableStateFlow<List<QadaaItem>>(emptyList())
    val pendingQadaaItems: StateFlow<List<QadaaItem>> = _pendingQadaaItems.asStateFlow()

    private val _prayerLogRevision = MutableStateFlow(0L)
    val prayerLogRevision: StateFlow<Long> = _prayerLogRevision.asStateFlow()

    private val _autoSilentEnabled = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.isAutoSilentEnabled(application.applicationContext)
        }.getOrDefault(false)
    )
    val autoSilentEnabled: StateFlow<Boolean> = _autoSilentEnabled.asStateFlow()

    private val _autoSilentAtIqama = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.isAutoSilentAtIqama(application.applicationContext)
        }.getOrDefault(false)
    )
    val autoSilentAtIqama: StateFlow<Boolean> = _autoSilentAtIqama.asStateFlow()

    private val _autoSilentDurationMinutes = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.getAutoSilentDurationMinutes(application.applicationContext)
        }.getOrDefault(15)
    )
    val autoSilentDurationMinutes: StateFlow<Int> = _autoSilentDurationMinutes.asStateFlow()

    private val _fridayKahfReminderEnabled = MutableStateFlow(
        runCatching {
            PrayerScheduleStore.isFridayKahfReminderEnabled(application.applicationContext)
        }.getOrDefault(true)
    )
    val fridayKahfReminderEnabled: StateFlow<Boolean> = _fridayKahfReminderEnabled.asStateFlow()

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

    private val _prayerTimeAdjustments = MutableStateFlow(
        PrayerScheduleStore.getTimeAdjustments(application.applicationContext)
    )
    val prayerTimeAdjustments: StateFlow<PrayerTimeAdjustments> = _prayerTimeAdjustments.asStateFlow()

    private val _permissionsStatus = MutableStateFlow(
        runCatching { PermissionsHelper.checkAll(application.applicationContext) }
            .getOrDefault(AppPermissionsStatus())
    )
    val permissionsStatus: StateFlow<AppPermissionsStatus> = _permissionsStatus.asStateFlow()

    private val _showReliabilityPrompt = MutableStateFlow(false)
    val showReliabilityPrompt: StateFlow<Boolean> = _showReliabilityPrompt.asStateFlow()

    val lastHadithResume: StateFlow<Pair<HadithBookCollection, Int>?> = prefs.lastHadithResumeFlow
        .map { pair ->
            if (pair == null) null
            else HadithBookCollection.entries.find { it.id == pair.first }?.let { it to pair.second }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var rawCurrentTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var rawSelectedDateTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var rawNextDayTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var currentTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var selectedDateTimings24: Map<String, String> = OfflineIslamicData.fallbackTimings24
    private var selectedPrayerDateKey: String = AstronomicalPrayerCalculator.todayDateKey()
    private var lastObservedPrayerDateKey: String = AstronomicalPrayerCalculator.todayDateKey()
    private var countdownJob: Job? = null
    private var prayerDateFetchJob: Job? = null
    private var prayerTrackerRefreshJob: Job? = null
    private var globalSearchJob: Job? = null
    private var appUpdateDownloadJob: Job? = null
    private var surahListRequested = false
    private var offlineMushafBook: OfflineMushafBook? = null
    private var offlineMushafJob: Job? = null
    private var azkarLoadJob: Job? = null
    private var tafsirFetchJob: Job? = null
    private val activeSurahAudioDownloads = mutableSetOf<String>()
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
        runCatching {
            QuranMediaEngine.ensureInitialized(
                context = application.applicationContext,
                resolveSurahName = { num -> resolveSurahName(num) }
            )
        }
        // Defer non-critical GitHub Releases update check until after Home Screen renders smoothly
        viewModelScope.launch {
            delay(2_500L)
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                withContext(Dispatchers.IO) {
                    AppUpdateManager.cleanupLeftoverUpdateApk(appCtx)
                }
                checkForAppUpdate(manual = false)
            }
        }
        // Defer non-critical alerts.json cache scan & remote refresh after Home Screen is drawn
        viewModelScope.launch {
            delay(1_600L)
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                val cached = PrayerAlertRepository.loadCachedCatalog(appCtx)
                val cachedIds = withContext(Dispatchers.IO) {
                    PrayerAlertRepository.getAllCachedAlertIds(appCtx, cached)
                }
                _cachedAlertSoundIds.value = cachedIds
                if (cached.isNotEmpty()) {
                    _alertSoundsCatalog.value = cached
                    applyRemoteDefaultAlertSoundsIfNeeded(appCtx, cached)
                }
                syncAlertSoundsCatalog(manual = false)
            }
        }
        viewModelScope.launch {
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                val locConfig = withContext(Dispatchers.IO) {
                    if (PrayerScheduleStore.hasSavedGlobalLocation(appCtx)) {
                        PrayerScheduleStore.getGlobalLocation(appCtx)
                    } else {
                        // Migrate legacy city choice cleanly to GlobalLocationConfig
                        val savedCityId = runCatching { prefs.selectedCityIdFlow.first() }.getOrDefault("cairo")
                        val city = OfflineIslamicData.cities.find { it.id == savedCityId }
                            ?: OfflineIslamicData.cities.first()
                        _selectedCity.value = city
                        val cc = inferCountryCodeForLegacyCity(city.country)
                        val migrated = GlobalLocationConfig(
                            lat = city.lat,
                            lng = city.lng,
                            displayLabel = city.arabicLabel,
                            countryCode = cc,
                            countryName = city.country,
                            cityName = city.city,
                            calculationMethodId = city.method,
                            isMethodManualOverride = false
                        )
                        PrayerScheduleStore.saveGlobalLocation(appCtx, migrated)
                        migrated
                    }
                }
                _globalLocation.value = locConfig
                updateQiblaLocation(locConfig.lat, locConfig.lng, locConfig.displayLabel)
                loadPrayerTimesForLocation(locConfig, selectedPrayerDateKey, manual = false)
            }
        }
        viewModelScope.launch {
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                val todayStr = AstronomicalPrayerCalculator.todayDateKey()
                val doneIds = withContext(Dispatchers.IO) {
                    val ids = prefs.getTrackerDoneIds(todayStr).toMutableSet()
                    // Sync today's PrayerLogStore performed prayers into daily worship tasks
                    val loggedToday = PrayerLogStore.getPerformedPrayers(appCtx, todayStr)
                    loggedToday.forEach { pKey ->
                        trackerTaskIdByPrayerKey[pKey]?.let { ids.add(it) }
                    }
                    // Also seed PrayerLogStore if user previously checked worship tasks today
                    val prayerKeysFromTasks = ids.mapNotNull { trackerPrayerKeyByTaskId[it] }.toSet()
                    if (prayerKeysFromTasks.isNotEmpty() && prayerKeysFromTasks != loggedToday) {
                        PrayerLogStore.setPerformedPrayers(appCtx, todayStr, loggedToday + prayerKeysFromTasks)
                    }
                    ids
                }
                _worshipTasks.value = OfflineIslamicData.worshipTasks.map {
                    it.copy(completed = doneIds.contains(it.id))
                }
                refreshFuturePrayerLocks(System.currentTimeMillis())
                refreshPrayerTrackerState()
            }
        }
        viewModelScope.launch {
            runCatching {
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
        }
        // Defer backup restore check, legacy cleanup, cached Muezzin load & widget refresh off Main thread
        viewModelScope.launch {
            delay(800L)
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                withContext(Dispatchers.IO) {
                    runCatching { PrayerLogStore.ensureRestoredFromBackupFile(appCtx) }
                    runCatching { OfflineHadithRepository.cleanupLegacyRemovedBooks(appCtx) }
                    runCatching {
                        if (MuezzinRepository.hasCatalogCache(appCtx)) {
                            val cachedMuezzins = MuezzinRepository.loadCachedCatalog(appCtx)
                            if (cachedMuezzins.isNotEmpty()) {
                                _repoMuezzins.value = cachedMuezzins
                                muezzinCatalogLoaded = true
                            }
                        }
                    }
                }
                refreshPrayerTrackerState()
                PrayerTimesWidgetProvider.scheduleDebouncedWidgetUpdate(appCtx, delayMs = 600L)
                // One-time startup reliability check: prompt once if battery optimization or exact alarms missing
                val shouldPromptReliability = withContext(Dispatchers.IO) {
                    val status = PermissionsHelper.checkAll(appCtx)
                    _permissionsStatus.value = status
                    OnboardingPrefs.isCompleted(appCtx) &&
                        !OnboardingPrefs.hasShownReliabilityPrompt(appCtx) &&
                        !status.adhanReliabilityReady
                }
                if (shouldPromptReliability) {
                    _showReliabilityPrompt.value = true
                }
            }
        }
        // Keep persistent countdown notification service synced with user preference
        viewModelScope.launch {
            runCatching {
                prefs.countdownNotificationEnabledFlow.collect { enabled ->
                    val appCtx = getApplication<Application>().applicationContext
                    withContext(Dispatchers.IO) {
                        runCatching { PrayerScheduleStore.setCountdownNotificationEnabled(appCtx, enabled) }
                        runCatching { PrayerCountdownService.syncState(appCtx) }
                    }
                }
            }
        }
        // Keep Adhan quick-silence toggles synced in PrayerScheduleStore for lock-screen service access
        viewModelScope.launch {
            runCatching {
                prefs.adhanSilenceOnVolumeFlow.collect { enabled ->
                    runCatching {
                        PrayerScheduleStore.setAdhanSilenceOnVolumeEnabled(
                            getApplication<Application>().applicationContext,
                            enabled
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                prefs.adhanSilenceOnFlipFlow.collect { enabled ->
                    runCatching {
                        PrayerScheduleStore.setAdhanSilenceOnFlipEnabled(
                            getApplication<Application>().applicationContext,
                            enabled
                        )
                    }
                }
            }
        }
        // Deferred lightweight startup check on Dispatchers.IO: read last Mushaf page pref & disk download checkmarks.
        viewModelScope.launch {
            delay(900L)
            runCatching {
                val appCtx = getApplication<Application>().applicationContext
                val lastPage = runCatching { prefs.lastMushafPageFlow.first() }.getOrDefault(1)
                val (cachedLocally, downloadedAudioKeys, downloadedTafsirs) = withContext(Dispatchers.IO) {
                    Triple(
                        runCatching { OfflineQuranRepository.isCachedLocally(appCtx) }.getOrDefault(false),
                        runCatching { PersistentOfflineStorage.getDownloadedSurahKeys(appCtx) }.getOrDefault(emptySet()),
                        runCatching { TafsirRepository.getDownloadedBookIds(appCtx) }.getOrDefault(emptySet())
                    )
                }
                _quranState.update {
                    it.copy(
                        currentMushafPage = lastPage,
                        lastSavedMushafPage = lastPage,
                        isMushafCachedOffline = cachedLocally,
                        downloadedSurahAudioKeys = downloadedAudioKeys
                    )
                }
                _tafsirState.update {
                    it.copy(downloadedBookIds = downloadedTafsirs)
                }
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
    // MODULE A: HOME — GLOBAL LOCATION, DATE NAVIGATION, PRAYER TIMES & LOG
    // =========================================================================

    private val trackerPrayerKeyByTaskId = mapOf(
        "fajr" to "Fajr",
        "dhuhr" to "Dhuhr",
        "asr" to "Asr",
        "maghrib" to "Maghrib",
        "isha" to "Isha"
    )

    private val trackerTaskIdByPrayerKey = mapOf(
        "Fajr" to "fajr",
        "Dhuhr" to "dhuhr",
        "Asr" to "asr",
        "Maghrib" to "maghrib",
        "Isha" to "isha"
    )

    private fun inferCountryCodeForLegacyCity(country: String): String {
        return when (country.trim().lowercase(Locale.US)) {
            "egypt" -> "EG"
            "saudi arabia" -> "SA"
            "united arab emirates", "uae" -> "AE"
            "kuwait" -> "KW"
            "qatar" -> "QA"
            "jordan" -> "JO"
            "morocco" -> "MA"
            "turkey" -> "TR"
            "united kingdom", "uk" -> "GB"
            "united states", "usa" -> "US"
            else -> "EG"
        }
    }

    fun selectCity(city: CityOption) {
        _selectedCity.value = city
        viewModelScope.launch { prefs.setSelectedCityId(city.id) }
        val appCtx = getApplication<Application>().applicationContext
        val cc = inferCountryCodeForLegacyCity(city.country)
        val config = GlobalLocationConfig(
            lat = city.lat,
            lng = city.lng,
            displayLabel = city.arabicLabel,
            countryCode = cc,
            countryName = city.country,
            cityName = city.city,
            calculationMethodId = city.method,
            isMethodManualOverride = false
        )
        applyNewGlobalLocation(config, showToast = true)
    }

    fun openGlobalLocationModal(includeMethodOverride: Boolean = false) {
        _showCalculationMethodPickerInModal.value = includeMethodOverride
        _globalSearchResults.value = emptyList()
        _globalLocationModalOpen.value = true
    }

    fun closeGlobalLocationModal() {
        globalSearchJob?.cancel()
        _globalLocationModalOpen.value = false
    }

    fun searchGlobalLocations(query: String) {
        globalSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _globalSearchResults.value = emptyList()
            _isSearchingLocations.value = false
            return
        }
        globalSearchJob = viewModelScope.launch {
            _isSearchingLocations.value = true
            val appCtx = getApplication<Application>().applicationContext
            val results = GlobalLocationRepository.searchLocationsGlobally(appCtx, trimmed)
            _globalSearchResults.value = results
            _isSearchingLocations.value = false
        }
    }

    fun selectGlobalSearchResult(result: GlobalSearchResult) {
        val current = _globalLocation.value
        val method = if (current.isMethodManualOverride) {
            current.resolvedMethod()
        } else {
            PrayerCalculationMethod.forCountryCode(result.countryCode)
        }
        val nextConfig = GlobalLocationConfig(
            lat = result.lat,
            lng = result.lng,
            displayLabel = result.displayLabel,
            countryCode = result.countryCode,
            countryName = result.countryName,
            cityName = result.cityName,
            calculationMethodId = method.alAdhanMethodId,
            isMethodManualOverride = current.isMethodManualOverride
        )
        _globalLocationModalOpen.value = false
        applyNewGlobalLocation(nextConfig, showToast = true)
    }

    fun onGlobalGpsDetected(location: Location) {
        if (_isDetectingLocation.value) return
        viewModelScope.launch {
            _isDetectingLocation.value = true
            val appCtx = getApplication<Application>().applicationContext
            val preserveManual = _globalLocation.value
                .takeIf { it.isMethodManualOverride }
                ?.calculationMethodId
            val resolved = GlobalLocationRepository.reverseGeocode(
                context = appCtx,
                lat = location.latitude,
                lng = location.longitude,
                preserveManualMethodId = preserveManual
            )
            _isDetectingLocation.value = false
            _globalLocationModalOpen.value = false
            applyNewGlobalLocation(resolved, showToast = true)
        }
    }

    fun setManualCalculationMethod(method: PrayerCalculationMethod?) {
        val current = _globalLocation.value
        val nextConfig = if (method == null) {
            val auto = PrayerCalculationMethod.forCountryCode(current.countryCode)
            current.copy(
                calculationMethodId = auto.alAdhanMethodId,
                isMethodManualOverride = false
            )
        } else {
            current.copy(
                calculationMethodId = method.alAdhanMethodId,
                isMethodManualOverride = true
            )
        }
        applyNewGlobalLocation(nextConfig, showToast = false)
        val label = if (method == null) {
            "تم ضبط طريقة الحساب تلقائياً (${nextConfig.resolvedMethod().shortArabicName})"
        } else {
            "تم تغيير طريقة الحساب إلى: ${method.shortArabicName}"
        }
        emitToast(label)
    }

    private fun applyNewGlobalLocation(config: GlobalLocationConfig, showToast: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        _globalLocation.value = config
        PrayerScheduleStore.saveGlobalLocation(appCtx, config)
        updateQiblaLocation(config.lat, config.lng, config.displayLabel)
        loadPrayerTimesForLocation(config, selectedPrayerDateKey, manual = false)
        if (showToast) {
            emitToast("تم تحديث الموقع: ${config.displayLabel} (${config.resolvedMethod().shortArabicName})")
        }
    }

    fun setHijriDayOffset(offsetDays: Int) {
        val clamped = offsetDays.coerceIn(-2, 2)
        val appCtx = getApplication<Application>().applicationContext
        _hijriDayOffset.value = clamped
        PrayerScheduleStore.setHijriDayOffset(appCtx, clamped)

        val cal = AstronomicalPrayerCalculator.parseDateKey(selectedPrayerDateKey)
        val adjustedHijri = PrayerScheduleStore.computeAdjustedHijriInfo(cal.timeInMillis, clamped)
        _prayerState.update {
            it.copy(
                hijriDate = adjustedHijri.formattedDate,
                blessedDayBadge = adjustedHijri.blessedDayBadge
            )
        }
        if (selectedPrayerDateKey == AstronomicalPrayerCalculator.todayDateKey()) {
            PrayerScheduleStore.saveDisplayMetadata(
                appCtx,
                adjustedHijri.formattedDate,
                _globalLocation.value.displayLabel
            )
        }
        PrayerTimesWidgetProvider.scheduleDebouncedWidgetUpdate(appCtx)
        val label = when {
            clamped > 0 -> "+$clamped يوم"
            clamped < 0 -> "$clamped يوم"
            else -> "تلقائي (0)"
        }
        emitToast("تم تعديل التاريخ الهجري: $label")
    }

    fun refreshPermissionsStatus() {
        viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            val status = withContext(Dispatchers.IO) {
                runCatching { PermissionsHelper.checkAll(appCtx) }.getOrDefault(AppPermissionsStatus())
            }
            _permissionsStatus.value = status
            if (status.adhanReliabilityReady) {
                if (_showReliabilityPrompt.value) {
                    _showReliabilityPrompt.value = false
                    withContext(Dispatchers.IO) {
                        runCatching { OnboardingPrefs.setReliabilityPromptShown(appCtx, true) }
                    }
                }
                withContext(Dispatchers.IO) {
                    runCatching { PrayerAlarmScheduler.scheduleAllPrayers(appCtx) }
                }
            }
        }
    }

    fun dismissReliabilityPrompt() {
        _showReliabilityPrompt.value = false
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { OnboardingPrefs.setReliabilityPromptShown(appCtx, true) }
        }
    }

    fun setGlobalPrayerTimeAdjustment(minutes: Int) {
        val clamped = minutes.coerceIn(
            PrayerScheduleStore.MIN_TIME_ADJUSTMENT_MINUTES,
            PrayerScheduleStore.MAX_TIME_ADJUSTMENT_MINUTES
        )
        val updated = _prayerTimeAdjustments.value.copy(globalMinutes = clamped)
        val label = formatMinuteDeltaArabic(clamped)
        applyAndPersistTimeAdjustments(updated, "تم ضبط التعديل العام للمواقيت ($label)")
    }

    fun setPerPrayerTimeAdjustment(prayerKey: String, minutes: Int) {
        val clamped = minutes.coerceIn(
            PrayerScheduleStore.MIN_TIME_ADJUSTMENT_MINUTES,
            PrayerScheduleStore.MAX_TIME_ADJUSTMENT_MINUTES
        )
        val current = _prayerTimeAdjustments.value
        val nextMap = current.perPrayerMinutes.toMutableMap()
        if (clamped == 0) {
            nextMap.remove(prayerKey)
        } else {
            nextMap[prayerKey] = clamped
        }
        val updated = current.copy(perPrayerMinutes = nextMap)
        val prayerAr = prayerMeta.firstOrNull { it.first == prayerKey }?.second ?: prayerKey
        applyAndPersistTimeAdjustments(updated, "تم تعديل وقت $prayerAr (${formatMinuteDeltaArabic(clamped)})")
    }

    fun resetPrayerTimeAdjustments() {
        applyAndPersistTimeAdjustments(
            PrayerTimeAdjustments(),
            "تمت إعادة جميع تعديلات الدقائق للوضع الافتراضي (0)"
        )
    }

    private fun formatMinuteDeltaArabic(minutes: Int): String = when {
        minutes > 0 -> "+$minutes د"
        minutes < 0 -> "$minutes د"
        else -> "0 د"
    }

    private fun applyAndPersistTimeAdjustments(updated: PrayerTimeAdjustments, toastMsg: String) {
        val appCtx = getApplication<Application>().applicationContext
        _prayerTimeAdjustments.value = updated
        reapplyManualTimeAdjustmentsToState()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { PrayerScheduleStore.saveTimeAdjustments(appCtx, updated) }
            runCatching { PrayerAlarmScheduler.scheduleAllPrayers(appCtx) }
            runCatching { PrayerCountdownService.syncState(appCtx) }
        }
        PrayerTimesWidgetProvider.scheduleDebouncedWidgetUpdate(appCtx)
        emitToast(toastMsg)
    }

    private fun reapplyManualTimeAdjustmentsToState() {
        val adj = _prayerTimeAdjustments.value
        currentTimings24 = PrayerScheduleStore.applyTimeAdjustments(rawCurrentTimings24, adj)
        selectedDateTimings24 = PrayerScheduleStore.applyTimeAdjustments(rawSelectedDateTimings24, adj)
        val adjustedNextDay = PrayerScheduleStore.applyTimeAdjustments(rawNextDayTimings24, adj)
        val targetCal = AstronomicalPrayerCalculator.parseDateKey(selectedPrayerDateKey)
        val nightTimes = PrayerScheduleStore.computeIslamicNightTimes(
            todayMaghrib24 = selectedDateTimings24["Maghrib"] ?: "17:02",
            tomorrowFajr24 = adjustedNextDay["Fajr"] ?: selectedDateTimings24["Fajr"] ?: "04:55",
            nowMillis = targetCal.timeInMillis
        )
        _prayerState.update {
            it.copy(
                islamicMidnightFormatted = nightTimes.islamicMidnightFormatted,
                lastThirdNightFormatted = nightTimes.lastThirdNightFormatted
            )
        }
        updatePrayerCountdownOnce()
    }

    fun setAutoSilentEnabled(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        if (enabled && !PrayerScheduleStore.isDndPermissionGranted(appCtx)) {
            emitToast("يرجى منح إذن عدم الإزعاج لتفعيل الوضع الصامت وقت الصلاة")
            return
        }
        _autoSilentEnabled.value = enabled
        PrayerScheduleStore.setAutoSilentEnabled(appCtx, enabled)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        emitToast(
            if (enabled) "تم تفعيل الوضع الصامت التلقائي وقت الصلاة"
            else "تم إيقاف الوضع الصامت وقت الصلاة"
        )
    }

    fun setAutoSilentAtIqama(atIqama: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        _autoSilentAtIqama.value = atIqama
        PrayerScheduleStore.setAutoSilentAtIqama(appCtx, atIqama)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        emitToast(
            if (atIqama) "سيتم تفعيل الصامت تلقائياً عند الإقامة"
            else "سيتم تفعيل الصامت تلقائياً عند الأذان"
        )
    }

    fun setAutoSilentDurationMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 60)
        val appCtx = getApplication<Application>().applicationContext
        _autoSilentDurationMinutes.value = clamped
        PrayerScheduleStore.setAutoSilentDurationMinutes(appCtx, clamped)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        emitToast("مدة الوضع الصامت: $clamped دقيقة")
    }

    fun setFridayKahfReminderEnabled(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        _fridayKahfReminderEnabled.value = enabled
        PrayerScheduleStore.setFridayKahfReminderEnabled(appCtx, enabled)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        emitToast(
            if (enabled) "تم تفعيل تذكير سورة الكهف كل جمعة الساعة 9:00 صباحاً"
            else "تم إيقاف تذكير سورة الكهف يوم الجمعة"
        )
    }

    fun refreshPrayerTimes() {
        loadPrayerTimesForLocation(_globalLocation.value, selectedPrayerDateKey, manual = true)
    }

    fun navigatePrayerDateByDays(deltaDays: Int) {
        val cal = AstronomicalPrayerCalculator.parseDateKey(selectedPrayerDateKey)
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        val nextKey = AstronomicalPrayerCalculator.dateKey(cal)
        selectedPrayerDateKey = nextKey
        loadPrayerTimesForLocation(_globalLocation.value, nextKey, manual = false)
    }

    fun selectPrayerDate(year: Int, monthZeroBased: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, 31))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextKey = AstronomicalPrayerCalculator.dateKey(cal)
        selectedPrayerDateKey = nextKey
        loadPrayerTimesForLocation(_globalLocation.value, nextKey, manual = false)
    }

    fun returnToTodayPrayerDate() {
        val today = AstronomicalPrayerCalculator.todayDateKey()
        if (selectedPrayerDateKey == today) return
        selectedPrayerDateKey = today
        loadPrayerTimesForLocation(_globalLocation.value, today, manual = false)
    }

    private fun refreshPrayerTrackerState() {
        _prayerLogRevision.update { it + 1L }
        prayerTrackerRefreshJob?.cancel()
        prayerTrackerRefreshJob = viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            val (stats, qadaa) = withContext(Dispatchers.IO) {
                val computedStats = runCatching { PrayerLogStore.computeStats(appCtx) }
                    .getOrDefault(_prayerLogStats.value)
                val computedQadaa = runCatching { PrayerLogStore.getPendingQadaaItems(appCtx) }
                    .getOrDefault(_pendingQadaaItems.value)
                computedStats to computedQadaa
            }
            _prayerLogStats.value = stats
            _pendingQadaaItems.value = qadaa
        }
    }

    fun openPrayerTracker() {
        refreshPrayerTrackerState()
        _prayerTrackerOpen.value = true
    }

    fun closePrayerTracker() {
        _prayerTrackerOpen.value = false
    }

    fun togglePrayerPerformed(prayerKey: String) {
        togglePrayerPerformedForDate(selectedPrayerDateKey, prayerKey)
    }

    fun togglePrayerPerformedForDate(dateKey: String, prayerKey: String) {
        val today = AstronomicalPrayerCalculator.todayDateKey()
        if (dateKey > today) return
        val appCtx = getApplication<Application>().applicationContext
        val updatedSet = PrayerLogStore.togglePrayer(appCtx, dateKey, prayerKey)
        refreshPrayerTrackerState()

        // Keep Today's Worship Tracker synchronized when marking today's prayers
        if (dateKey == today) {
            val taskId = trackerTaskIdByPrayerKey[prayerKey]
            if (taskId != null) {
                val isDone = updatedSet.contains(prayerKey)
                val nextTasks = _worshipTasks.value.map { task ->
                    if (task.id == taskId) task.copy(completed = isDone) else task
                }
                _worshipTasks.value = nextTasks
                val doneIds = nextTasks.filter { it.completed }.map { it.id }.toSet()
                viewModelScope.launch { prefs.saveTrackerDoneIds(today, doneIds) }
            }
        }
        if (dateKey == selectedPrayerDateKey) {
            updatePrayerCountdownOnce()
        }
    }

    fun markQadaaDone(dateKey: String, prayerKey: String) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerLogStore.markQadaaCompleted(appCtx, dateKey, prayerKey)
        refreshPrayerTrackerState()
        if (dateKey == selectedPrayerDateKey) {
            updatePrayerCountdownOnce()
        }
        emitToast("تقبل الله منك — تم تسجيل قضاء الصلاة ✓")
    }

    fun toggleSunnahHabitForDate(dateKey: String, habitKey: String) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerLogStore.toggleSunnahHabit(appCtx, dateKey, habitKey)
        _prayerLogRevision.update { it + 1L }
    }

    fun exportPrayerBackupJson(): String {
        return PrayerLogStore.exportBackupJson(getApplication<Application>().applicationContext)
    }

    fun importPrayerBackupJson(rawJson: String) {
        val appCtx = getApplication<Application>().applicationContext
        val result = PrayerLogStore.importBackupJson(appCtx, rawJson)
        if (result.success) {
            refreshPrayerTrackerState()
            // Sync today's Home card & worship tracker
            val today = AstronomicalPrayerCalculator.todayDateKey()
            val todayLogged = PrayerLogStore.getPerformedPrayers(appCtx, today)
            val nextTasks = _worshipTasks.value.map { task ->
                val pKey = trackerPrayerKeyByTaskId[task.id]
                if (pKey != null) task.copy(completed = todayLogged.contains(pKey)) else task
            }
            _worshipTasks.value = nextTasks
            updatePrayerCountdownOnce()
            emitToast("تم استيراد السجل بنجاح (${result.importedDaysCount} يوم محفوظ) ✓")
        } else {
            emitToast(result.errorMessage ?: "تعذّر استيراد النسخة الاحتياطية")
        }
    }

    private fun loadPrayerTimesForLocation(
        location: GlobalLocationConfig,
        dateKey: String,
        manual: Boolean
    ) {
        prayerDateFetchJob?.cancel()
        prayerDateFetchJob = viewModelScope.launch {
            val appCtx = getApplication<Application>().applicationContext
            val todayStr = AstronomicalPrayerCalculator.todayDateKey()
            val isToday = dateKey == todayStr
            val isFuture = dateKey > todayStr
            val isPast = dateKey < todayStr

            val targetCal = AstronomicalPrayerCalculator.parseDateKey(dateKey)
            val nextDayCal = (targetCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
            val gregStr = AstronomicalPrayerCalculator.formatGregorianArabic(targetCal)
            val method = location.resolvedMethod()
            val hijriOffset = _hijriDayOffset.value
            val hijriInfo = PrayerScheduleStore.computeAdjustedHijriInfo(targetCal.timeInMillis, hijriOffset)

            // 1. Instant 100% offline astronomical calculation (works worldwide with zero latency!)
            val offlineTargetTimings = AstronomicalPrayerCalculator.calculateTimings24(
                latitude = location.lat,
                longitude = location.lng,
                dateCalendar = targetCal,
                method = method
            )
            val offlineNextDayTimings = AstronomicalPrayerCalculator.calculateTimings24(
                latitude = location.lat,
                longitude = location.lng,
                dateCalendar = nextDayCal,
                method = method
            )
            val offlineTodayTimings = if (isToday) {
                offlineTargetTimings
            } else {
                AstronomicalPrayerCalculator.calculateTimings24(
                    latitude = location.lat,
                    longitude = location.lng,
                    dateCalendar = Calendar.getInstance(),
                    method = method
                )
            }

            val adjustments = _prayerTimeAdjustments.value
            rawSelectedDateTimings24 = offlineTargetTimings
            rawNextDayTimings24 = offlineNextDayTimings
            if (isToday || rawCurrentTimings24 === OfflineIslamicData.fallbackTimings24) {
                rawCurrentTimings24 = offlineTodayTimings
            }
            selectedDateTimings24 = PrayerScheduleStore.applyTimeAdjustments(rawSelectedDateTimings24, adjustments)
            currentTimings24 = PrayerScheduleStore.applyTimeAdjustments(rawCurrentTimings24, adjustments)
            val adjustedNextDayTimings = PrayerScheduleStore.applyTimeAdjustments(offlineNextDayTimings, adjustments)

            val performedForDate = if (isFuture) {
                emptySet()
            } else {
                PrayerLogStore.getPerformedPrayers(appCtx, dateKey)
            }

            val offlineNightTimes = PrayerScheduleStore.computeIslamicNightTimes(
                todayMaghrib24 = selectedDateTimings24["Maghrib"] ?: "17:02",
                tomorrowFajr24 = adjustedNextDayTimings["Fajr"] ?: "04:55",
                nowMillis = targetCal.timeInMillis
            )

            _prayerState.update {
                it.copy(
                    isLoading = manual,
                    isOfflineFallback = false,
                    selectedDateKey = dateKey,
                    isSelectedDateToday = isToday,
                    isSelectedDateFuture = isFuture,
                    isSelectedDatePast = isPast,
                    hijriDate = hijriInfo.formattedDate,
                    gregorianDate = gregStr,
                    blessedDayBadge = hijriInfo.blessedDayBadge,
                    performedCount = performedForDate.size,
                    statusNote = "${location.displayLabel} • ${method.shortArabicName}",
                    islamicMidnightFormatted = offlineNightTimes.islamicMidnightFormatted,
                    lastThirdNightFormatted = offlineNightTimes.lastThirdNightFormatted
                )
            }
            updatePrayerCountdownOnce()

            // Save today's raw baseline schedules asynchronously (getTimings applies manual adjustments cleanly)
            if (isToday) {
                lastObservedPrayerDateKey = todayStr
                val rawSnapshot = rawCurrentTimings24
                withContext(Dispatchers.IO) {
                    runCatching { PrayerScheduleStore.saveTimings(appCtx, rawSnapshot) }
                    runCatching { PrayerScheduleStore.saveDisplayMetadata(appCtx, hijriInfo.formattedDate, location.displayLabel) }
                    runCatching { PrayerAlarmScheduler.scheduleAllPrayers(appCtx) }
                    runCatching { PrayerCountdownService.syncState(appCtx) }
                }
                PrayerTimesWidgetProvider.scheduleDebouncedWidgetUpdate(appCtx)
            }
            refreshFuturePrayerLocks(System.currentTimeMillis())
            startPrayerCountdownLoop()

            // 2. Quietly refine via AlAdhan coordinate API when online (deferred briefly on startup so UI paints smoothly)
            if (!manual) delay(500L)
            try {
                val dateParam = AstronomicalPrayerCalculator.formatAlAdhanDateParam(targetCal)
                val response = withContext(Dispatchers.IO) {
                    IslamicNetworkClient.alAdhanApi.getTimingsByCoordinates(
                        date = dateParam,
                        latitude = location.lat,
                        longitude = location.lng,
                        method = method.alAdhanMethodId
                    )
                }
                val t = response.data.timings
                val apiTimings = mapOf(
                    "Fajr" to cleanTime(t.Fajr),
                    "Dhuhr" to cleanTime(t.Dhuhr),
                    "Asr" to cleanTime(t.Asr),
                    "Maghrib" to cleanTime(t.Maghrib),
                    "Isha" to cleanTime(t.Isha)
                )
                val latestAdj = _prayerTimeAdjustments.value
                if (selectedPrayerDateKey == dateKey) {
                    rawSelectedDateTimings24 = apiTimings
                    selectedDateTimings24 = PrayerScheduleStore.applyTimeAdjustments(apiTimings, latestAdj)
                }
                if (isToday) {
                    rawCurrentTimings24 = apiTimings
                    currentTimings24 = PrayerScheduleStore.applyTimeAdjustments(apiTimings, latestAdj)
                }

                val h = response.data.date.hijri
                val finalHijriText = if (hijriOffset != 0) {
                    hijriInfo.formattedDate
                } else {
                    "${h.weekday.ar}، ${h.day} ${h.month.ar} ${h.year} هـ"
                }

                val adjustedApiTimings = PrayerScheduleStore.applyTimeAdjustments(apiTimings, latestAdj)
                val adjustedTomorrow = PrayerScheduleStore.applyTimeAdjustments(offlineNextDayTimings, latestAdj)
                val nightTimes = PrayerScheduleStore.computeIslamicNightTimes(
                    todayMaghrib24 = adjustedApiTimings["Maghrib"] ?: "17:02",
                    tomorrowFajr24 = adjustedTomorrow["Fajr"] ?: adjustedApiTimings["Fajr"] ?: "04:55",
                    nowMillis = targetCal.timeInMillis
                )

                if (selectedPrayerDateKey == dateKey) {
                    _prayerState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineFallback = false,
                            hijriDate = finalHijriText,
                            gregorianDate = gregStr,
                            blessedDayBadge = hijriInfo.blessedDayBadge,
                            statusNote = "${location.displayLabel} • ${method.shortArabicName}",
                            islamicMidnightFormatted = nightTimes.islamicMidnightFormatted,
                            lastThirdNightFormatted = nightTimes.lastThirdNightFormatted
                        )
                    }
                    updatePrayerCountdownOnce()
                }

                if (isToday) {
                    val rawSnapshot = rawCurrentTimings24
                    withContext(Dispatchers.IO) {
                        runCatching { PrayerScheduleStore.saveTimings(appCtx, rawSnapshot) }
                        runCatching { PrayerScheduleStore.saveDisplayMetadata(appCtx, finalHijriText, location.displayLabel) }
                        runCatching { PrayerAlarmScheduler.scheduleAllPrayers(appCtx) }
                        runCatching { PrayerCountdownService.syncState(appCtx) }
                    }
                    PrayerTimesWidgetProvider.scheduleDebouncedWidgetUpdate(appCtx)
                }
                if (manual) emitToast("تم تحديث مواقيت الصلاة")
            } catch (_: Exception) {
                if (selectedPrayerDateKey == dateKey) {
                    _prayerState.update {
                        it.copy(
                            isLoading = false,
                            isOfflineFallback = true,
                            statusNote = "${location.displayLabel} • حساب فلكي أوفلاين (${method.shortArabicName})"
                        )
                    }
                }
                if (manual) emitToast("تم حساب المواقيت فلكياً بدون إنترنت")
            }
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
        val currentDateKey = AstronomicalPrayerCalculator.todayDateKey()
        if (currentDateKey != lastObservedPrayerDateKey) {
            val wasOnOldToday = selectedPrayerDateKey == lastObservedPrayerDateKey
            lastObservedPrayerDateKey = currentDateKey
            if (wasOnOldToday) {
                selectedPrayerDateKey = currentDateKey
            }
            loadPrayerTimesForLocation(_globalLocation.value, selectedPrayerDateKey, manual = false)
            return
        }

        // Next prayer & live countdown ALWAYS reflect TODAY's timings
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
            val adjustedNextDay = PrayerScheduleStore.applyTimeAdjustments(
                rawNextDayTimings24,
                _prayerTimeAdjustments.value
            )
            val tomorrowFajrStr = adjustedNextDay["Fajr"] ?: currentTimings24["Fajr"] ?: "05:00"
            val parts = tomorrowFajrStr.split(":")
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

        val appCtx = getApplication<Application>().applicationContext
        val isToday = selectedPrayerDateKey == currentDateKey
        val isFuture = selectedPrayerDateKey > currentDateKey
        val isPast = selectedPrayerDateKey < currentDateKey
        val displayTimings = if (isToday) currentTimings24 else selectedDateTimings24
        val performedForSelected = if (isFuture) {
            emptySet()
        } else {
            PrayerLogStore.getPerformedPrayers(appCtx, selectedPrayerDateKey)
        }

        val entries = prayerMeta.map { (key, arName) ->
            val raw = displayTimings[key] ?: "--:--"
            PrayerEntry(
                key = key,
                arabicName = arName,
                time24 = raw,
                formatted12 = format12HourArabic(raw),
                isNext = isToday && key == nextKey,
                isPerformed = performedForSelected.contains(key)
            )
        }

        val activeIqama = PrayerScheduleStore.computeActiveIqamaCountdown(
            settings = _prayerAlertsSettings.value,
            timings24 = currentTimings24,
            nowMillis = now.timeInMillis
        )

        _prayerState.update { state ->
            val updatedNight = if (isToday) {
                val adjNext = PrayerScheduleStore.applyTimeAdjustments(
                    rawNextDayTimings24,
                    _prayerTimeAdjustments.value
                )
                PrayerScheduleStore.computeIslamicNightTimes(
                    todayMaghrib24 = currentTimings24["Maghrib"] ?: "17:02",
                    tomorrowFajr24 = adjNext["Fajr"] ?: currentTimings24["Fajr"] ?: "04:55",
                    nowMillis = now.timeInMillis
                )
            } else null

            state.copy(
                prayers = entries,
                nextPrayerName = nextArabic,
                countdownText = countdown,
                selectedDateKey = selectedPrayerDateKey,
                isSelectedDateToday = isToday,
                isSelectedDateFuture = isFuture,
                isSelectedDatePast = isPast,
                performedCount = performedForSelected.size,
                islamicMidnightFormatted = updatedNight?.islamicMidnightFormatted ?: state.islamicMidnightFormatted,
                lastThirdNightFormatted = updatedNight?.lastThirdNightFormatted ?: state.lastThirdNightFormatted,
                activeIqamaCountdown = activeIqama
            )
        }

        // Automatically lock/unlock today's 5 prayers in the daily tracker as real time progresses
        refreshFuturePrayerLocks(now.timeInMillis)

        // Check Smart Fajr Alarm auto-trigger when enabled
        val alarmCfg = fajrAlarmSettings.value
        if (alarmCfg.enabled && !_fajrAlarmRinging.value) {
            if (lastAutoTriggeredAlarmDate != currentDateKey) {
                val fajr24 = currentTimings24["Fajr"] ?: "04:55"
                val wakeup24 = FajrTimeCalculator.calculateWakeupTime(fajr24, alarmCfg.offsetMinutesBeforeFajr).time24
                val nowHHmm = String.format(
                    Locale.US,
                    "%02d:%02d",
                    now.get(Calendar.HOUR_OF_DAY),
                    now.get(Calendar.MINUTE)
                )
                if (nowHHmm == wakeup24) {
                    lastAutoTriggeredAlarmDate = currentDateKey
                    triggerFajrAlarmNow(isTest = false)
                }
            }
        }
    }

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
        val today = AstronomicalPrayerCalculator.todayDateKey()
        viewModelScope.launch {
            prefs.saveTrackerDoneIds(today, doneIds)
        }
        // Keep PrayerLogStore synchronized for today's 5 prayers
        val prayerKey = trackerPrayerKeyByTaskId[taskId]
        if (prayerKey != null) {
            val appCtx = getApplication<Application>().applicationContext
            val currentLogged = PrayerLogStore.getPerformedPrayers(appCtx, today).toMutableSet()
            val nowDone = updated.find { it.id == taskId }?.completed == true
            if (nowDone) currentLogged.add(prayerKey) else currentLogged.remove(prayerKey)
            PrayerLogStore.setPerformedPrayers(appCtx, today, currentLogged)
            _prayerLogStats.value = PrayerLogStore.computeStats(appCtx)
            updatePrayerCountdownOnce()
        }
        if (doneIds.size == updated.size && updated.find { it.id == taskId }?.completed == true) {
            emitToast("ما شاء الله! أتممت وردك اليومي بالكامل")
        }
    }

    fun resetTodayTracker() {
        val nowMillis = System.currentTimeMillis()
        val today = AstronomicalPrayerCalculator.todayDateKey()
        val appCtx = getApplication<Application>().applicationContext
        _worshipTasks.value = OfflineIslamicData.worshipTasks.map {
            it.copy(
                completed = false,
                isLockedFuture = isPrayerStillInFuture(it.id, nowMillis)
            )
        }
        PrayerLogStore.setPerformedPrayers(appCtx, today, emptySet())
        _prayerLogStats.value = PrayerLogStore.computeStats(appCtx)
        viewModelScope.launch { prefs.saveTrackerDoneIds(today, emptySet()) }
        updatePrayerCountdownOnce()
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
     * - When Quran.com v4/QDC official millisecond timestamps are available: plays ONLY this Ayah
     *   within the seamless Surah MP3 (`playSingleAyahOnly = true`, `stopAtMs = ayah.endMs`) and
     *   pauses immediately when finished (or repeats if repeat mode is enabled).
     * - When timestamps are NOT available (e.g., mp3quran.net reciter or API fallback): NEVER plays
     *   the full-Surah MP3; instead plays the direct per-Ayah audio URL (EveryAyah / AlQuran.cloud),
     *   highlights that exact Ayah while playing, and stops automatically on completion.
     */
    fun playSinglePageAyahOnly(pageAyah: MushafPageAyah) {
        if (audioPlayer.hasOfficialTimestampsSupportForCurrentReciter()) {
            stopOtherAudio(except = AudioSource.SURAH)
            audioPlayer.playSurahFromAyah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                startAyahNumber = pageAyah.numberInSurah,
                singleAyahOnly = true,
                repeatMode = ayahAudioState.value.repeatMode,
                onSingleAyahFallbackRequired = {
                    playSingleAyahDirectPerVerse(pageAyah, continueReading = false)
                }
            )
        } else {
            playSingleAyahDirectPerVerse(pageAyah, continueReading = false)
        }
    }

    /**
     * Mode 2: "استماع من هذه الآية" (Continue From Here)
     * Starts continuous recitation from the selected Ayah (`playSingleAyahOnly = false`, `stopAtMs = null`)
     * and automatically advances through following Ayahs, Mushaf pages, and the next Surah.
     */
    fun playContinueFromPageAyah(pageAyah: MushafPageAyah) {
        val advanceSurah: (Int) -> Unit = { finishedSurah ->
            if (finishedSurah < 114) {
                val nextNum = finishedSurah + 1
                audioPlayer.playSurah(nextNum, resolveSurahName(nextNum), forceRestart = true)
            }
        }
        if (audioPlayer.hasOfficialTimestampsSupportForCurrentReciter()) {
            stopOtherAudio(except = AudioSource.SURAH)
            audioPlayer.playSurahFromAyah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                startAyahNumber = pageAyah.numberInSurah,
                singleAyahOnly = false,
                repeatMode = AyahRepeatMode.ONCE,
                onSurahCompleted = advanceSurah,
                onSingleAyahFallbackRequired = null
            )
        } else if (EveryAyahMapper.resolveFolderForReciter(audioState.value.selectedReciter) != null) {
            playSingleAyahDirectPerVerse(pageAyah, continueReading = true)
        } else {
            stopOtherAudio(except = AudioSource.SURAH)
            audioPlayer.playSurah(
                surahNumber = pageAyah.surahNumber,
                surahName = pageAyah.surahName,
                forceRestart = true,
                onSurahCompleted = advanceSurah
            )
            emitToast("جاري تشغيل ${pageAyah.surahName} كاملة بصوت ${audioState.value.selectedReciter.arabicName}")
        }
    }

    private fun playSingleAyahDirectPerVerse(
        pageAyah: MushafPageAyah,
        continueReading: Boolean
    ) {
        stopOtherAudio(except = AudioSource.AYAH)
        val reciter = audioState.value.selectedReciter
        val ayahItem = AyahItem(
            numberInSurah = pageAyah.numberInSurah,
            text = pageAyah.text,
            tafsir = "",
            globalNumber = pageAyah.globalNumber,
            juz = pageAyah.juz
        )
        ayahAudioPlayer.playAyah(
            surahNumber = pageAyah.surahNumber,
            ayah = ayahItem,
            reciter = reciter,
            continueReading = continueReading,
            onAdvanceToNextAyah = { nextAyahNum ->
                val nextPageAyah = findPageAyah(pageAyah.surahNumber, nextAyahNum)
                if (nextPageAyah != null) {
                    playSingleAyahDirectPerVerse(nextPageAyah, continueReading = true)
                }
            }
        )
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
        if (audioState.value.isSingleAyahMode) {
            audioPlayer.pauseIfPlaying()
        }
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
            // Dynamically verify real local disk files on every Tafsir open so all downloaded
            // books accurately display their green checkmark without stale/overwritten state.
            val downloaded = withContext(Dispatchers.IO) {
                TafsirRepository.getDownloadedBookIds(appCtx)
            }
            _tafsirState.update { it.copy(downloadedBookIds = downloaded) }
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
                val verifiedDiskIds = withContext(Dispatchers.IO) {
                    TafsirRepository.getDownloadedBookIds(appCtx)
                }
                _tafsirState.update { state ->
                    state.copy(
                        downloadedBookIds = verifiedDiskIds + book.bookId,
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
        val remoteUrl = state.activeStreamUrl?.takeIf {
            it.startsWith("http://") || it.startsWith("https://")
        } ?: reciter.buildPrimaryAudioUrl(surahNum)
        val downloadKey = PersistentOfflineStorage.surahAudioKey(reciter.id, surahNum)
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
                val diskKeys = withContext(Dispatchers.IO) {
                    PersistentOfflineStorage.getDownloadedSurahKeys(appCtx)
                }
                _quranState.update { it.copy(downloadedSurahAudioKeys = diskKeys) }
                emitToast("$surahName محفوظة بالفعل أوفلاين على مساحة الجهاز ✓")
                return@launch
            }

            activeSurahAudioDownloads.add(downloadKey)
            _quranState.update { it.copy(downloadingSurahAudioKeys = it.downloadingSurahAudioKeys + downloadKey) }
            emitToast("جاري حفظ $surahName أوفلاين (${reciter.arabicName})...")
            try {
                val result = PersistentOfflineStorage.downloadSurahAudioOffline(
                    context = appCtx,
                    reciterId = reciter.id,
                    surahNumber = surahNum,
                    streamUrl = remoteUrl
                )
                result.onSuccess {
                    val diskKeys = withContext(Dispatchers.IO) {
                        PersistentOfflineStorage.getDownloadedSurahKeys(appCtx)
                    }
                    _quranState.update { it.copy(downloadedSurahAudioKeys = diskKeys) }
                    emitToast("تم حفظ $surahName أوفلاين على مساحة الجهاز ✓")
                }.onFailure {
                    emitToast("تعذّر تحميل $surahName، تأكد من الاتصال بالإنترنت")
                }
            } finally {
                activeSurahAudioDownloads.remove(downloadKey)
                _quranState.update { it.copy(downloadingSurahAudioKeys = it.downloadingSurahAudioKeys - downloadKey) }
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
        onGlobalGpsDetected(location)
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
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.saveDefaultMuezzin(appCtx, muezzin)
        _defaultMuezzinId.value = muezzin.id
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        viewModelScope.launch {
            prefs.setDefaultMuezzinId(muezzin.id)
        }
        if (muezzin.hasAudioSource()) {
            emitToast("تم تعيين ${muezzin.name} كأذان افتراضي")
        } else {
            emitToast("تم تعيين ${muezzin.name} — اضغط «أضف ملف الصوت» لإرفاق ملف MP3 أو رابط")
        }
    }

    fun setFajrMuezzin(muezzin: MuezzinItem) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.saveFajrMuezzin(appCtx, muezzin)
        PrayerScheduleStore.setUseDedicatedFajr(appCtx, true)
        _fajrMuezzinId.value = muezzin.id
        _useDedicatedFajrAdhan.value = true
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        viewModelScope.launch {
            prefs.setFajrMuezzinId(muezzin.id)
            prefs.setUseDedicatedFajrAdhan(true)
        }
        if (muezzin.hasAudioSource()) {
            emitToast("تم تعيين ${muezzin.name} لأذان الفجر")
        } else {
            emitToast("تم تعيين ${muezzin.name} للفجر — اضغط «أضف ملف الصوت» لإرفاق ملف MP3 أو رابط")
        }
    }

    fun setUseDedicatedFajrAdhan(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.setUseDedicatedFajr(appCtx, enabled)
        _useDedicatedFajrAdhan.value = enabled
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        viewModelScope.launch {
            prefs.setUseDedicatedFajrAdhan(enabled)
        }
    }

    fun setPerPrayerMuezzinEnabled(enabled: Boolean) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.setPerPrayerMuezzinEnabled(appCtx, enabled)
        _perPrayerMuezzinEnabled.value = enabled
        _perPrayerMuezzinIds.value = PrayerScheduleStore.getPerPrayerMuezzinIds(appCtx)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        emitToast(
            if (enabled) "تم تفعيل اختيار مؤذن مخصص لكل صلاة"
            else "تم العودة لمؤذن موحّد لجميع الصلوات"
        )
    }

    fun setMuezzinForPrayer(prayerKey: String, muezzin: MuezzinItem) {
        val appCtx = getApplication<Application>().applicationContext
        PrayerScheduleStore.saveMuezzinForPrayer(appCtx, prayerKey, muezzin)
        _perPrayerMuezzinIds.value = PrayerScheduleStore.getPerPrayerMuezzinIds(appCtx)
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
        val pArabic = PrayerScheduleStore.prayerOrder.find { it.first == prayerKey }?.second ?: prayerKey
        emitToast("تم تعيين ${muezzin.name} لصلاة $pArabic")
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
                setFajrMuezzin(newItem)
            } else {
                setDefaultMuezzin(newItem)
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
            PrayerScheduleStore.refreshSavedAudioMetadataIfSelected(
                appCtx,
                muezzin.copy(primaryUrl = cleanUrl, localFilePath = null)
            )
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
            PrayerScheduleStore.refreshSavedAudioMetadataIfSelected(
                getApplication(),
                muezzin.copy(primaryUrl = "", localFilePath = savedPath)
            )
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
            val original = (_repoMuezzins.value + MuezzinLibraryData.builtInMuezzins).find { it.id == muezzin.id }
            if (original != null) {
                PrayerScheduleStore.refreshSavedAudioMetadataIfSelected(getApplication(), original)
            }
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
                setFajrMuezzin(newItem)
            } else {
                setDefaultMuezzin(newItem)
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
            val fallbackBuiltIn = MuezzinLibraryData.builtInMuezzins.first()
            if (defaultMuezzinId.value == muezzin.id) {
                setDefaultMuezzin(fallbackBuiltIn)
            }
            if (fajrMuezzinId.value == muezzin.id) {
                val fallbackFajr = MuezzinLibraryData.builtInMuezzins.find { it.id == MuezzinLibraryData.defaultFajrMuezzinId }
                    ?: fallbackBuiltIn
                setFajrMuezzin(fallbackFajr)
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
    // MODULE G2: PRE-ADHAN REMINDER & POST-ADHAN IQAMA ALERTS (تنبيهات قبل وبعد الأذان)
    // =========================================================================

    fun openPrayerAlertsSettings() {
        _prayerAlertsSettingsOpen.value = true
        if (_alertSoundsCatalog.value.isEmpty() && !_isSyncingAlertSounds.value) {
            syncAlertSoundsCatalog(manual = false)
        }
    }

    fun closePrayerAlertsSettings() {
        AdhanPlaybackService.sendControlAction(
            getApplication<Application>().applicationContext,
            AdhanPlaybackService.ACTION_STOP_ADHAN
        )
        _prayerAlertsSettingsOpen.value = false
    }

    fun setPreAdhanEnabled(enabled: Boolean) {
        updatePrayerAlertsSettings { it.copy(preAdhanEnabled = enabled) }
        emitToast(if (enabled) "تم تفعيل تنبيه قبل الأذان" else "تم إيقاف تنبيه قبل الأذان")
    }

    fun setPreAdhanMinutes(minutes: Int) {
        if (minutes !in PrayerAlertsSettings.PRE_ADHAN_MINUTE_OPTIONS) return
        updatePrayerAlertsSettings { it.copy(preAdhanMinutes = minutes) }
        emitToast("سيتم التذكير قبل الأذان بـ $minutes دقيقة")
    }

    fun setIqamaEnabled(enabled: Boolean) {
        updatePrayerAlertsSettings { it.copy(iqamaEnabled = enabled) }
        updatePrayerCountdownOnce()
        emitToast(if (enabled) "تم تفعيل تنبيه الإقامة بعد الأذان" else "تم إيقاف تنبيه الإقامة")
    }

    fun setIqamaMinutesForPrayer(prayerKey: String, minutes: Int) {
        val clamped = minutes.coerceIn(
            PrayerAlertsSettings.MIN_IQAMA_MINUTES,
            PrayerAlertsSettings.MAX_IQAMA_MINUTES
        )
        updatePrayerAlertsSettings { current ->
            val updatedMap = current.iqamaMinutesByPrayer.toMutableMap().apply {
                put(prayerKey, clamped)
            }
            current.copy(iqamaMinutesByPrayer = updatedMap)
        }
        updatePrayerCountdownOnce()
    }

    fun resetIqamaMinutesToDefaults() {
        updatePrayerAlertsSettings { it.copy(iqamaMinutesByPrayer = PrayerAlertsSettings.DEFAULT_IQAMA_MINUTES) }
        updatePrayerCountdownOnce()
        emitToast("تم استعادة أوقات الإقامة الافتراضية")
    }

    fun selectPreAdhanSound(sound: PrayerAlertSound) {
        updatePrayerAlertsSettings(markPreAdhanSoundUserCustomized = true) {
            it.copy(
                preAdhanSoundId = sound.id,
                preAdhanSoundName = sound.name,
                preAdhanSoundUrl = sound.url,
                preAdhanIsPerPrayer = sound.isPerPrayer,
                preAdhanPerPrayerUrls = sound.perPrayerUrls
            )
        }
        emitToast("تم اختيار صوت قبل الأذان: ${sound.name}")
        if (!sound.isBuiltInFallback && (sound.url.isNotBlank() || sound.perPrayerUrls.isNotEmpty())) {
            downloadAlertSoundOffline(sound, silent = true)
        }
    }

    fun selectIqamaSound(sound: PrayerAlertSound) {
        updatePrayerAlertsSettings(markIqamaSoundUserCustomized = true) {
            it.copy(
                iqamaSoundId = sound.id,
                iqamaSoundName = sound.name,
                iqamaSoundUrl = sound.url
            )
        }
        emitToast("تم اختيار صوت الإقامة: ${sound.name}")
        if (!sound.isBuiltInFallback && sound.url.isNotBlank()) {
            downloadAlertSoundOffline(sound, silent = true)
        }
    }

    fun syncAlertSoundsCatalog(manual: Boolean) {
        if (_isSyncingAlertSounds.value) return
        viewModelScope.launch {
            _isSyncingAlertSounds.value = true
            val appCtx = getApplication<Application>().applicationContext
            val result = PrayerAlertRepository.syncCatalogFromRemote(appCtx)
            _isSyncingAlertSounds.value = false
            result.onSuccess { list ->
                _alertSoundsCatalog.value = list
                _cachedAlertSoundIds.value = withContext(Dispatchers.IO) {
                    PrayerAlertRepository.getAllCachedAlertIds(appCtx, list)
                }
                applyRemoteDefaultAlertSoundsIfNeeded(appCtx, list)
                if (manual) emitToast("تم تحديث قائمة أصوات التنبيهات (${list.size})")
            }.onFailure {
                if (manual) emitToast("تعذّر تحديث أصوات التنبيهات، تأكد من الاتصال بالإنترنت")
            }
        }
    }

    fun downloadAlertSoundOffline(sound: PrayerAlertSound, silent: Boolean = false) {
        if (sound.isBuiltInFallback) return
        if (!sound.isPerPrayer && sound.url.isBlank()) return
        if (sound.isPerPrayer && sound.perPrayerUrls.isEmpty() && sound.url.isBlank()) return
        if (_downloadingAlertSoundIds.value.contains(sound.id)) return
        val appCtx = getApplication<Application>().applicationContext
        viewModelScope.launch {
            val alreadyCached = withContext(Dispatchers.IO) {
                PrayerAlertRepository.isSoundCached(appCtx, sound)
            }
            if (alreadyCached) {
                _cachedAlertSoundIds.value = withContext(Dispatchers.IO) {
                    PrayerAlertRepository.getAllCachedAlertIds(appCtx, _alertSoundsCatalog.value)
                }
                return@launch
            }
            _downloadingAlertSoundIds.update { it + sound.id }
            val result = PrayerAlertRepository.downloadAlertSound(appCtx, sound)
            _downloadingAlertSoundIds.update { it - sound.id }
            result.onSuccess {
                _cachedAlertSoundIds.value = withContext(Dispatchers.IO) {
                    PrayerAlertRepository.getAllCachedAlertIds(appCtx, _alertSoundsCatalog.value)
                }
                if (!silent) {
                    val msg = if (sound.isPerPrayer) {
                        "تم حفظ حزمة «${sound.name}» للصلوات الخمس أوفلاين"
                    } else {
                        "تم حفظ «${sound.name}» للتشغيل دون إنترنت"
                    }
                    emitToast(msg)
                }
            }.onFailure {
                if (!silent) emitToast("تعذّر حفظ الصوت حالياً")
            }
        }
    }

    fun toggleAlertSoundPreview(sound: PrayerAlertSound) {
        val appCtx = getApplication<Application>().applicationContext
        val state = adhanState.value
        val isSameActive = state.activeMuezzinId == sound.id && (
            state.status == AdhanPlaybackStatus.PLAYING ||
                state.status == AdhanPlaybackStatus.FALLBACK_PLAYING ||
                state.status == AdhanPlaybackStatus.LOADING
            )
        if (isSameActive) {
            AdhanPlaybackService.sendControlAction(appCtx, AdhanPlaybackService.ACTION_STOP_ADHAN)
            return
        }

        viewModelScope.launch {
            val sources = withContext(Dispatchers.IO) {
                val resolved = ArrayList<String>()
                if (!sound.isBuiltInFallback) {
                    if (sound.isPerPrayer) {
                        val dhuhrCached = PrayerAlertRepository.getCachedPerPrayerAlertFile(appCtx, sound.id, "dhuhr")
                        if (sound.url.isNotBlank()) {
                            resolved.add(sound.url)
                        }
                        if (dhuhrCached.exists() && dhuhrCached.length() > 0L) {
                            resolved.add(dhuhrCached.absolutePath)
                        } else {
                            PrayerAlertsSettings.PRAYER_URL_KEYS.firstNotNullOfOrNull { pKey ->
                                PrayerAlertRepository.getCachedPerPrayerAlertFile(appCtx, sound.id, pKey)
                                    .takeIf { it.exists() && it.length() > 0L }
                            }?.let { resolved.add(it.absolutePath) }
                        }
                    } else {
                        val cached = PrayerAlertRepository.getCachedAlertFile(appCtx, sound.id)
                        if (cached.exists() && cached.length() > 0L) {
                            resolved.add(cached.absolutePath)
                        }
                        if (sound.url.isNotBlank()) {
                            resolved.add(sound.url)
                        }
                    }
                }
                ArrayList(resolved.distinct())
            }
            AdhanPlaybackService.startAlertPreview(
                context = appCtx,
                soundId = sound.id,
                soundName = sound.name,
                sources = sources
            )
        }
    }

    private fun updatePrayerAlertsSettings(
        markPreAdhanSoundUserCustomized: Boolean = false,
        markIqamaSoundUserCustomized: Boolean = false,
        transform: (PrayerAlertsSettings) -> PrayerAlertsSettings
    ) {
        val appCtx = getApplication<Application>().applicationContext
        val updated = transform(_prayerAlertsSettings.value)
        _prayerAlertsSettings.value = updated
        PrayerScheduleStore.savePrayerAlertsSettings(
            context = appCtx,
            settings = updated,
            markPreAdhanSoundUserCustomized = markPreAdhanSoundUserCustomized,
            markIqamaSoundUserCustomized = markIqamaSoundUserCustomized
        )
        PrayerAlarmScheduler.scheduleAllPrayers(appCtx)
    }

    private fun applyRemoteDefaultAlertSoundsIfNeeded(
        appCtx: Context,
        catalog: List<PrayerAlertSound>
    ) {
        if (catalog.isEmpty()) return
        var updated = _prayerAlertsSettings.value
        var changed = false

        // Always make "pre_voice_hamed" the default Pre-Adhan sound unless the user explicitly chose another
        val preferredPre = catalog.firstOrNull {
            it.id.equals(PrayerAlertsSettings.PREFERRED_DEFAULT_PRE_ADHAN_SOUND_ID, ignoreCase = true)
        }
        if (!PrayerScheduleStore.isPreAdhanSoundUserCustomized(appCtx)) {
            val defaultPre = preferredPre
                ?: catalog.firstOrNull { it.category == PrayerAlertCategory.PRE_ADHAN }
                ?: catalog.firstOrNull { it.category == PrayerAlertCategory.BOTH }
                ?: catalog.firstOrNull()
            if (defaultPre != null && (
                updated.preAdhanSoundId != defaultPre.id ||
                    updated.preAdhanIsPerPrayer != defaultPre.isPerPrayer ||
                    updated.preAdhanPerPrayerUrls != defaultPre.perPrayerUrls
                )
            ) {
                updated = updated.copy(
                    preAdhanSoundId = defaultPre.id,
                    preAdhanSoundName = defaultPre.name,
                    preAdhanSoundUrl = defaultPre.url,
                    preAdhanIsPerPrayer = defaultPre.isPerPrayer,
                    preAdhanPerPrayerUrls = defaultPre.perPrayerUrls
                )
                changed = true
            }
            if (defaultPre != null) {
                downloadAlertSoundOffline(defaultPre, silent = true)
            }
        } else {
            // Keep selected per_prayer sound URLs fresh and ensure all 5 MP3s are cached
            catalog.firstOrNull { it.id == updated.preAdhanSoundId }?.let { activePre ->
                if (activePre.isPerPrayer && updated.preAdhanPerPrayerUrls != activePre.perPrayerUrls) {
                    updated = updated.copy(
                        preAdhanSoundUrl = activePre.url,
                        preAdhanIsPerPrayer = true,
                        preAdhanPerPrayerUrls = activePre.perPrayerUrls
                    )
                    changed = true
                }
                downloadAlertSoundOffline(activePre, silent = true)
            }
        }

        if (!PrayerScheduleStore.isIqamaSoundUserCustomized(appCtx) &&
            updated.iqamaSoundId == PrayerAlertsSettings.DEFAULT_IQAMA_SOUND_ID
        ) {
            val defaultIqama = catalog.firstOrNull { it.category == PrayerAlertCategory.IQAMA }
                ?: catalog.firstOrNull { it.category == PrayerAlertCategory.BOTH }
                ?: catalog.firstOrNull()
            if (defaultIqama != null) {
                updated = updated.copy(
                    iqamaSoundId = defaultIqama.id,
                    iqamaSoundName = defaultIqama.name,
                    iqamaSoundUrl = defaultIqama.url
                )
                changed = true
                downloadAlertSoundOffline(defaultIqama, silent = true)
            }
        }

        if (changed) {
            _prayerAlertsSettings.value = updated
            PrayerScheduleStore.savePrayerAlertsSettings(appCtx, updated)
        }
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
                    if (!manual && AppUpdateManager.shouldSuppressPopupForDismissal(appCtx, release.versionName)) {
                        _appUpdateState.update { it.copy(isChecking = false) }
                        return@onSuccess
                    }
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
                        emitToast("أنت على أحدث إصدار (${BuildConfig.VERSION_NAME})")
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
        AppUpdateManager.clearDismissal(getApplication<Application>().applicationContext)
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
        val current = _appUpdateState.value
        if (current.stage == AppUpdateDialogStage.DOWNLOADING) return
        if (current.stage == AppUpdateDialogStage.UPDATE_AVAILABLE) {
            current.availableRelease?.versionName?.let { dismissedVersion ->
                AppUpdateManager.recordDismissal(
                    getApplication<Application>().applicationContext,
                    dismissedVersion
                )
            }
        }
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
