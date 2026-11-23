package com.example.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AppPermissionsStatus
import com.example.alarm.PermissionsHelper
import com.example.data.local.OfflineIslamicData
import com.example.data.local.PersistentOfflineStorage
import com.example.ui.components.AboutCopyrightModal
import com.example.ui.components.AppUpdateOverlayHost
import com.example.ui.components.GlobalLocationModal
import com.example.ui.components.StickyQuranMiniPlayer
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.FajrAlarmRingingScreen
import com.example.ui.screens.FajrAlarmSettingsModal
import com.example.ui.screens.HadithScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MuezzinLibraryModal
import com.example.ui.screens.PrayerTrackerScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.ReciterLibraryModal
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.RafiqAlMuslimTheme
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.RafiqTab
import com.example.ui.viewmodel.RafiqViewModel
import java.util.Calendar

@Composable
fun RafiqApp(viewModel: RafiqViewModel) {
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val globalLocation by viewModel.globalLocation.collectAsStateWithLifecycle()
    val globalLocationModalOpen by viewModel.globalLocationModalOpen.collectAsStateWithLifecycle()
    val showCalculationMethodPickerInModal by viewModel.showCalculationMethodPickerInModal.collectAsStateWithLifecycle()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val globalSearchResults by viewModel.globalSearchResults.collectAsStateWithLifecycle()
    val isSearchingLocations by viewModel.isSearchingLocations.collectAsStateWithLifecycle()
    val hijriDayOffset by viewModel.hijriDayOffset.collectAsStateWithLifecycle()
    val prayerLogStats by viewModel.prayerLogStats.collectAsStateWithLifecycle()
    val prayerTrackerOpen by viewModel.prayerTrackerOpen.collectAsStateWithLifecycle()
    val pendingQadaaItems by viewModel.pendingQadaaItems.collectAsStateWithLifecycle()
    val prayerLogRevision by viewModel.prayerLogRevision.collectAsStateWithLifecycle()
    val autoSilentEnabled by viewModel.autoSilentEnabled.collectAsStateWithLifecycle()
    val autoSilentAtIqama by viewModel.autoSilentAtIqama.collectAsStateWithLifecycle()
    val autoSilentDurationMinutes by viewModel.autoSilentDurationMinutes.collectAsStateWithLifecycle()
    val fridayKahfReminderEnabled by viewModel.fridayKahfReminderEnabled.collectAsStateWithLifecycle()
    val prayerState by viewModel.prayerState.collectAsStateWithLifecycle()
    val worshipTasks by viewModel.worshipTasks.collectAsStateWithLifecycle()
    val inspirationIdx by viewModel.inspirationIndex.collectAsStateWithLifecycle()
    val bookmark by viewModel.bookmark.collectAsStateWithLifecycle()
    val quranState by viewModel.quranState.collectAsStateWithLifecycle()
    val quranFontSize by viewModel.quranFontSize.collectAsStateWithLifecycle()
    val mushafPaperTheme by viewModel.mushafPaperTheme.collectAsStateWithLifecycle()
    val tajweedColorsEnabled by viewModel.tajweedColorsEnabled.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val ayahAudioState by viewModel.ayahAudioState.collectAsStateWithLifecycle()
    val activeVerseKey by viewModel.activeVerseKey.collectAsStateWithLifecycle()
    val activeWordIndex by viewModel.activeWordIndex.collectAsStateWithLifecycle()
    val tafsirState by viewModel.tafsirState.collectAsStateWithLifecycle()
    val azkarState by viewModel.azkarState.collectAsStateWithLifecycle()
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val hadithState by viewModel.hadithState.collectAsStateWithLifecycle()
    val favoriteHadithUids by viewModel.favoriteHadithUids.collectAsStateWithLifecycle()
    val hadithFontSize by viewModel.hadithFontSize.collectAsStateWithLifecycle()
    val toolsState by viewModel.toolsState.collectAsStateWithLifecycle()
    val allMuezzins by viewModel.allMuezzins.collectAsStateWithLifecycle()
    val activeDefaultMuezzin by viewModel.activeDefaultMuezzin.collectAsStateWithLifecycle()
    val defaultMuezzinId by viewModel.defaultMuezzinId.collectAsStateWithLifecycle()
    val fajrMuezzinId by viewModel.fajrMuezzinId.collectAsStateWithLifecycle()
    val useDedicatedFajrAdhan by viewModel.useDedicatedFajrAdhan.collectAsStateWithLifecycle()
    val perPrayerMuezzinEnabled by viewModel.perPrayerMuezzinEnabled.collectAsStateWithLifecycle()
    val perPrayerMuezzinIds by viewModel.perPrayerMuezzinIds.collectAsStateWithLifecycle()
    val favoriteMuezzinIds by viewModel.favoriteMuezzinIds.collectAsStateWithLifecycle()
    val adhanState by viewModel.adhanState.collectAsStateWithLifecycle()
    val muezzinLibraryOpen by viewModel.muezzinLibraryOpen.collectAsStateWithLifecycle()
    val reciterLibraryOpen by viewModel.reciterLibraryOpen.collectAsStateWithLifecycle()
    val apiReciters by viewModel.apiReciters.collectAsStateWithLifecycle()
    val isRecitersLoading by viewModel.isRecitersLoading.collectAsStateWithLifecycle()
    val recitersError by viewModel.recitersError.collectAsStateWithLifecycle()
    val fajrAlarmSettings by viewModel.fajrAlarmSettings.collectAsStateWithLifecycle()
    val fajrWakeupTime by viewModel.fajrWakeupTime.collectAsStateWithLifecycle()
    val fajrAlarmSettingsOpen by viewModel.fajrAlarmSettingsOpen.collectAsStateWithLifecycle()
    val fajrAlarmRinging by viewModel.fajrAlarmRinging.collectAsStateWithLifecycle()
    val prayerAlertsSettingsOpen by viewModel.prayerAlertsSettingsOpen.collectAsStateWithLifecycle()
    val prayerAlertsSettings by viewModel.prayerAlertsSettings.collectAsStateWithLifecycle()
    val alertSoundsCatalog by viewModel.alertSoundsCatalog.collectAsStateWithLifecycle()
    val isSyncingAlertSounds by viewModel.isSyncingAlertSounds.collectAsStateWithLifecycle()
    val cachedAlertSoundIds by viewModel.cachedAlertSoundIds.collectAsStateWithLifecycle()
    val downloadingAlertSoundIds by viewModel.downloadingAlertSoundIds.collectAsStateWithLifecycle()
    val countdownNotificationEnabled by viewModel.countdownNotificationEnabled.collectAsStateWithLifecycle()
    val adhanSilenceOnVolume by viewModel.adhanSilenceOnVolume.collectAsStateWithLifecycle()
    val adhanSilenceOnFlip by viewModel.adhanSilenceOnFlip.collectAsStateWithLifecycle()
    val isSyncingMuezzinRepo by viewModel.isSyncingMuezzinRepo.collectAsStateWithLifecycle()
    val muezzinRepoStatus by viewModel.muezzinRepoStatus.collectAsStateWithLifecycle()
    val cachedAdhanIds by viewModel.cachedAdhanIds.collectAsStateWithLifecycle()
    val downloadingMuezzinIds by viewModel.downloadingMuezzinIds.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val aboutCopyrightOpen by viewModel.aboutCopyrightOpen.collectAsStateWithLifecycle()
    val lastHadithResume by viewModel.lastHadithResume.collectAsStateWithLifecycle()
    val prayerTimeAdjustments by viewModel.prayerTimeAdjustments.collectAsStateWithLifecycle()
    val permissionsStatus by viewModel.permissionsStatus.collectAsStateWithLifecycle()
    val showReliabilityPrompt by viewModel.showReliabilityPrompt.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isFullScreenQuran = selectedTab == RafiqTab.QURAN && quranState.isMushafOpen

    // Back press from secondary tabs returns to Home
    if (selectedTab != RafiqTab.HOME &&
        !quranState.isMushafOpen &&
        hadithState.openBook == null &&
        !muezzinLibraryOpen &&
        !reciterLibraryOpen &&
        !fajrAlarmSettingsOpen &&
        !prayerAlertsSettingsOpen &&
        !globalLocationModalOpen &&
        !prayerTrackerOpen &&
        !fajrAlarmRinging &&
        !aboutCopyrightOpen
    ) {
        BackHandler { viewModel.selectTab(RafiqTab.HOME) }
    }

    RafiqAlMuslimTheme(darkTheme = darkMode) {
        val islamic = LocalIslamicColors.current

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useNavigationRail = maxWidth >= 680.dp

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    if (!isFullScreenQuran) {
                        RafiqTopBar(
                            darkMode = darkMode,
                            onToggleDarkMode = viewModel::toggleDarkMode
                        )
                    }
                },
                bottomBar = {
                    if (!isFullScreenQuran) {
                        val showStickyMiniPlayer = audioState.loadedSurahNumber != null
                        val miniPlayerAudioKey = audioState.loadedSurahNumber?.let {
                            PersistentOfflineStorage.surahAudioKey(audioState.selectedReciter.id, it)
                        }
                        val isMiniPlayerDownloaded = miniPlayerAudioKey != null &&
                            quranState.downloadedSurahAudioKeys.contains(miniPlayerAudioKey)
                        val isMiniPlayerDownloading = miniPlayerAudioKey != null &&
                            quranState.downloadingSurahAudioKeys.contains(miniPlayerAudioKey)
                        if (!useNavigationRail) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                if (showStickyMiniPlayer) {
                                    StickyQuranMiniPlayer(
                                        audioState = audioState,
                                        onTogglePlay = viewModel::toggleMiniPlayerPlayPause,
                                        onPreviousSurah = viewModel::playPreviousSurah,
                                        onNextSurah = viewModel::playNextSurah,
                                        onSeek = viewModel::seekAudio,
                                        onSelectPlaybackSpeed = viewModel::setQuranPlaybackSpeed,
                                        onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                        onOpenReciterLibrary = viewModel::openReciterLibrary,
                                        onDismiss = viewModel::dismissMiniPlayer,
                                        isCurrentSurahDownloaded = isMiniPlayerDownloaded,
                                        isCurrentSurahDownloading = isMiniPlayerDownloading
                                    )
                                }
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 4.dp,
                                    modifier = Modifier.testTag("bottom_nav_bar")
                                ) {
                                    RafiqTab.entries.forEach { tab ->
                                        val selected = tab == selectedTab
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = { viewModel.selectTab(tab) },
                                            icon = {
                                                Icon(
                                                    imageVector = tabIcon(tab, selected),
                                                    contentDescription = tab.label
                                                )
                                            },
                                            label = {
                                                Text(
                                                    text = tab.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = islamic.emeraldSoft,
                                                unselectedIconColor = islamic.textMuted,
                                                unselectedTextColor = islamic.textMuted
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                                        )
                                    }
                                }
                            }
                        } else if (showStickyMiniPlayer) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                StickyQuranMiniPlayer(
                                    audioState = audioState,
                                    onTogglePlay = viewModel::toggleMiniPlayerPlayPause,
                                    onPreviousSurah = viewModel::playPreviousSurah,
                                    onNextSurah = viewModel::playNextSurah,
                                    onSeek = viewModel::seekAudio,
                                    onSelectPlaybackSpeed = viewModel::setQuranPlaybackSpeed,
                                    onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                    onOpenReciterLibrary = viewModel::openReciterLibrary,
                                    onDismiss = viewModel::dismissMiniPlayer,
                                    modifier = Modifier.widthIn(max = 620.dp),
                                    isCurrentSurahDownloaded = isMiniPlayerDownloaded,
                                    isCurrentSurahDownloading = isMiniPlayerDownloading
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail && !isFullScreenQuran) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            RafiqTab.entries.forEach { tab ->
                                val selected = tab == selectedTab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = islamic.emeraldSoft
                                    )
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 640.dp)
                                .fillMaxWidth()
                        ) {
                            val dailyVerse = remember(inspirationIdx) { viewModel.currentDailyVerse() }
                            val dailyHadith = remember(inspirationIdx) { viewModel.currentDailyHadith() }

                            when (selectedTab) {
                                RafiqTab.HOME -> HomeScreen(
                                    globalLocation = globalLocation,
                                    prayerState = prayerState,
                                    pendingQadaaCount = pendingQadaaItems.size,
                                    activeMuezzin = activeDefaultMuezzin,
                                    adhanState = adhanState,
                                    fajrAlarmSettings = fajrAlarmSettings,
                                    fajrWakeupTime = fajrWakeupTime,
                                    worshipTasks = worshipTasks,
                                    bookmark = bookmark,
                                    dailyVerse = dailyVerse,
                                    dailyHadith = dailyHadith,
                                    onOpenGlobalLocation = { viewModel.openGlobalLocationModal(includeMethodOverride = false) },
                                    onOpenPrayerTracker = viewModel::openPrayerTracker,
                                    onRefreshPrayers = viewModel::refreshPrayerTimes,
                                    onPrevPrayerDate = { viewModel.navigatePrayerDateByDays(-1) },
                                    onNextPrayerDate = { viewModel.navigatePrayerDateByDays(1) },
                                    onSelectPrayerDate = viewModel::selectPrayerDate,
                                    onReturnToTodayDate = viewModel::returnToTodayPrayerDate,
                                    onTogglePrayerPerformed = viewModel::togglePrayerPerformed,
                                    onToggleAdhanPreview = viewModel::toggleAdhanPreview,
                                    onOpenMuezzinLibrary = viewModel::openMuezzinLibrary,
                                    onOpenFajrAlarm = viewModel::openFajrAlarmSettings,
                                    onToggleTask = viewModel::toggleWorshipTask,
                                    onContinueReading = viewModel::openBookmarkedSurah,
                                    onRotateInspiration = viewModel::rotateInspiration,
                                    onShowToast = viewModel::emitToast
                                )

                                RafiqTab.QURAN -> QuranScreen(
                                    state = quranState,
                                    fontSizeSp = quranFontSize,
                                    paperTheme = mushafPaperTheme,
                                    tajweedColorsEnabled = tajweedColorsEnabled,
                                    bookmark = bookmark,
                                    audioState = audioState,
                                    ayahAudioState = ayahAudioState,
                                    activeVerseKey = activeVerseKey,
                                    activeWordIndex = activeWordIndex,
                                    tafsirState = tafsirState,
                                    onSearchChange = viewModel::updateSurahSearch,
                                    onFilterChange = viewModel::updateRevelationFilter,
                                    onOpenSurah = { viewModel.openSurahReader(it) },
                                    onOpenMushafLastPage = viewModel::openMushafAtLastPage,
                                    onOpenMushafPage = viewModel::openMushafAtPage,
                                    onMushafPageChanged = viewModel::onMushafPageChanged,
                                    onRetryOfflineMushaf = { viewModel.ensureOfflineMushafLoaded(forceRetry = true) },
                                    onToggleMushafNightMode = viewModel::toggleMushafNightMode,
                                    onToggleTajweedColors = viewModel::toggleTajweedColors,
                                    onSaveMushafPageBookmark = viewModel::saveMushafPageBookmark,
                                    onPlaySingleAyahOnly = viewModel::playSinglePageAyahOnly,
                                    onPlayFromPageAyah = viewModel::playContinueFromPageAyah,
                                    onPlaySurahFromList = viewModel::playSurahFromList,
                                    onOpenReciterLibrary = viewModel::openReciterLibrary,
                                    onRetryReader = {
                                        quranState.activeSurah?.let { viewModel.openSurahReader(it) }
                                    },
                                    onCloseReader = viewModel::closeSurahReader,
                                    onFontSizeChange = viewModel::updateQuranFontSize,
                                    onSelectViewMode = viewModel::selectQuranViewMode,
                                    onSelectPaperTheme = viewModel::setMushafPaperTheme,
                                    onToggleFullScreenReading = viewModel::toggleFullScreenReading,
                                    onToggleAllTafsir = viewModel::toggleAllTafsir,
                                    onToggleAyahTafsir = viewModel::toggleSingleAyahTafsir,
                                    onSaveBookmark = viewModel::saveQuranBookmark,
                                    onToggleAudio = viewModel::toggleSurahAudio,
                                    onSelectReciter = viewModel::selectReciter,
                                    onSeekAudio = viewModel::seekAudio,
                                    onSelectPlaybackSpeed = viewModel::setQuranPlaybackSpeed,
                                    onToggleWordByWord = viewModel::setWordByWordHighlightEnabled,
                                    onSyncLatencyOffsetChange = viewModel::setSyncLatencyOffsetMs,
                                    onTapSeekWordOrAyah = viewModel::seekToWordOrAyahWhilePlaying,
                                    onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                    onTogglePlaySingleAyah = viewModel::togglePlaySingleAyah,
                                    onStopSingleAyah = viewModel::stopSingleAyahAudio,
                                    onSelectAyahRepeatMode = viewModel::setAyahRepeatMode,
                                    onToggleAyahContinueReading = viewModel::setAyahContinueReading,
                                    onOpenAyahTafsir = { sNum, aNum -> viewModel.loadAyahTafsir(sNum, aNum) },
                                    onSelectTafsirBook = viewModel::selectTafsirBook,
                                    onRetryTafsir = viewModel::retryCurrentAyahTafsir,
                                    onDownloadTafsirBook = viewModel::downloadTafsirBookOffline,
                                    onShowToast = viewModel::emitToast
                                )

                                RafiqTab.AZKAR -> AzkarScreen(
                                    state = azkarState,
                                    tasbihState = tasbihState,
                                    onSelectSubTab = viewModel::selectAzkarSubTab,
                                    onSelectCategory = viewModel::selectAzkarCategory,
                                    onDecrementZikr = viewModel::decrementZikr,
                                    onResetCategory = viewModel::resetCurrentAzkarCategory,
                                    onIncrementTasbih = viewModel::incrementTasbih,
                                    onSetTasbihPhrase = viewModel::setTasbihPhrase,
                                    onSetTasbihTarget = viewModel::setTasbihTarget,
                                    onResetTasbihCurrent = viewModel::resetTasbihCurrent,
                                    onResetTasbihLifetime = viewModel::resetTasbihLifetime
                                )

                                RafiqTab.HADITH -> HadithScreen(
                                    state = hadithState,
                                    darkMode = darkMode,
                                    favoriteUids = favoriteHadithUids,
                                    hadithFontSizeSp = hadithFontSize,
                                    lastHadithResume = lastHadithResume,
                                    onSelectSubTab = viewModel::selectHadithSubTab,
                                    onOpenBook = viewModel::openHadithBook,
                                    onCloseBook = viewModel::closeHadithBook,
                                    onSaveHadithScrollIndex = viewModel::saveHadithScrollIndex,
                                    onDownloadBook = { viewModel.downloadHadithBookOffline(it) },
                                    onDeleteBook = viewModel::deleteHadithBookOffline,
                                    onSearchChange = viewModel::updateHadithSearch,
                                    onFavoritesSearchChange = viewModel::updateHadithFavoritesSearch,
                                    onToggleFontFamily = viewModel::toggleHadithFontFamily,
                                    onToggleExplanation = viewModel::toggleHadithExplanation,
                                    onToggleFavorite = viewModel::toggleFavoriteHadith,
                                    onFontSizeChange = viewModel::updateHadithFontSize,
                                    onLoadMore = viewModel::loadMoreHadiths,
                                    onShowToast = viewModel::emitToast
                                )

                                RafiqTab.TOOLS -> ToolsScreen(
                                    state = toolsState,
                                    globalLocation = globalLocation,
                                    prayerLogStats = prayerLogStats,
                                    hijriDayOffset = hijriDayOffset,
                                    prayerTimeAdjustments = prayerTimeAdjustments,
                                    permissionsStatus = permissionsStatus,
                                    activeMuezzin = activeDefaultMuezzin,
                                    fajrAlarmSettings = fajrAlarmSettings,
                                    fajrWakeupTime = fajrWakeupTime,
                                    darkMode = darkMode,
                                    adhanSilenceOnVolume = adhanSilenceOnVolume,
                                    adhanSilenceOnFlip = adhanSilenceOnFlip,
                                    isCheckingAppUpdate = appUpdateState.isChecking,
                                    onOpenGlobalLocationSettings = { viewModel.openGlobalLocationModal(includeMethodOverride = true) },
                                    onSelectHijriDayOffset = viewModel::setHijriDayOffset,
                                    onSetGlobalPrayerTimeAdjustment = viewModel::setGlobalPrayerTimeAdjustment,
                                    onSetPerPrayerTimeAdjustment = viewModel::setPerPrayerTimeAdjustment,
                                    onResetPrayerTimeAdjustments = viewModel::resetPrayerTimeAdjustments,
                                    onOpenMuezzinLibrary = viewModel::openMuezzinLibrary,
                                    onOpenFajrAlarm = viewModel::openFajrAlarmSettings,
                                    onOpenPrayerAlertsSettings = viewModel::openPrayerAlertsSettings,
                                    onOpenPrayerTracker = viewModel::openPrayerTracker,
                                    onExportPrayerBackupJson = viewModel::exportPrayerBackupJson,
                                    onImportPrayerBackupJson = viewModel::importPrayerBackupJson,
                                    onGpsLocation = viewModel::onGpsLocationDetected,
                                    onHeadingChanged = viewModel::updateDeviceHeading,
                                    onToggleCompassSensor = viewModel::setCompassSensorEnabled,
                                    onToggleGuide = viewModel::toggleGuideAccordion,
                                    onToggleDarkMode = viewModel::toggleDarkMode,
                                    onToggleAdhanSilenceOnVolume = viewModel::setAdhanSilenceOnVolume,
                                    onToggleAdhanSilenceOnFlip = viewModel::setAdhanSilenceOnFlip,
                                    onCheckForAppUpdate = { viewModel.checkForAppUpdate(manual = true) },
                                    onOpenAboutCopyright = viewModel::openAboutCopyright,
                                    onResetTracker = viewModel::resetTodayTracker,
                                    onShowToast = viewModel::emitToast
                                )
                            }
                        }
                    }
                }
            }

            if (muezzinLibraryOpen) {
                MuezzinLibraryModal(
                    allMuezzins = allMuezzins,
                    defaultMuezzinId = defaultMuezzinId,
                    fajrMuezzinId = fajrMuezzinId,
                    useDedicatedFajrAdhan = useDedicatedFajrAdhan,
                    perPrayerMuezzinEnabled = perPrayerMuezzinEnabled,
                    perPrayerMuezzinIds = perPrayerMuezzinIds,
                    favoriteIds = favoriteMuezzinIds,
                    adhanState = adhanState,
                    isSyncingRepo = isSyncingMuezzinRepo,
                    repoStatusMessage = muezzinRepoStatus,
                    cachedAdhanIds = cachedAdhanIds,
                    downloadingMuezzinIds = downloadingMuezzinIds,
                    onClose = viewModel::closeMuezzinLibrary,
                    onSyncRepo = { viewModel.syncMuezzinRepo(isAutoFirstLaunch = false) },
                    onDownloadOffline = viewModel::downloadMuezzinForOffline,
                    onTogglePreview = viewModel::toggleAdhanPreview,
                    onSwitchFallbackStream = viewModel::switchAdhanFallbackStream,
                    onRetryAdhan = viewModel::retryAdhanPreview,
                    onToggleFavorite = viewModel::toggleFavoriteMuezzin,
                    onSetDefaultMuezzin = viewModel::setDefaultMuezzin,
                    onSetFajrMuezzin = viewModel::setFajrMuezzin,
                    onToggleDedicatedFajr = viewModel::setUseDedicatedFajrAdhan,
                    onTogglePerPrayerMuezzin = viewModel::setPerPrayerMuezzinEnabled,
                    onSetMuezzinForPrayer = viewModel::setMuezzinForPrayer,
                    onAddCustomFromUrl = viewModel::addCustomMuezzinFromUrl,
                    onAddCustomFromFile = viewModel::addCustomMuezzinFromFile,
                    onOverrideAudioWithUrl = viewModel::overrideMuezzinAudioWithUrl,
                    onOverrideAudioWithFile = viewModel::overrideMuezzinAudioWithFile,
                    onResetAudioToDefault = viewModel::resetMuezzinAudioToDefault,
                    onDeleteCustomMuezzin = viewModel::deleteCustomMuezzin
                )
            }

            if (reciterLibraryOpen) {
                ReciterLibraryModal(
                    featuredReciters = OfflineIslamicData.reciters,
                    apiReciters = apiReciters,
                    selectedReciter = audioState.selectedReciter,
                    isLoading = isRecitersLoading,
                    errorMessage = recitersError,
                    onClose = viewModel::closeReciterLibrary,
                    onRetryFetch = { viewModel.loadApiReciters(forceRefresh = true) },
                    onSelectReciter = viewModel::selectReciter
                )
            }

            if (fajrAlarmSettingsOpen) {
                FajrAlarmSettingsModal(
                    settings = fajrAlarmSettings,
                    wakeupTime = fajrWakeupTime,
                    onClose = viewModel::closeFajrAlarmSettings,
                    onUpdateSettings = viewModel::updateFajrAlarmSettings,
                    onTestAlarmNow = { viewModel.triggerFajrAlarmNow(isTest = true) },
                    onShowToast = viewModel::emitToast
                )
            }

            if (prayerAlertsSettingsOpen) {
                com.example.ui.screens.PrayerAlertsSettingsModal(
                    settings = prayerAlertsSettings,
                    alertSoundsCatalog = alertSoundsCatalog,
                    isSyncingSounds = isSyncingAlertSounds,
                    cachedAlertIds = cachedAlertSoundIds,
                    downloadingAlertIds = downloadingAlertSoundIds,
                    adhanState = adhanState,
                    autoSilentEnabled = autoSilentEnabled,
                    autoSilentAtIqama = autoSilentAtIqama,
                    autoSilentDurationMinutes = autoSilentDurationMinutes,
                    countdownNotificationEnabled = countdownNotificationEnabled,
                    fridayKahfReminderEnabled = fridayKahfReminderEnabled,
                    onClose = viewModel::closePrayerAlertsSettings,
                    onTogglePreAdhan = viewModel::setPreAdhanEnabled,
                    onSelectPreAdhanMinutes = viewModel::setPreAdhanMinutes,
                    onSelectPreAdhanSound = viewModel::selectPreAdhanSound,
                    onToggleIqama = viewModel::setIqamaEnabled,
                    onChangeIqamaMinutes = viewModel::setIqamaMinutesForPrayer,
                    onResetIqamaDefaults = viewModel::resetIqamaMinutesToDefaults,
                    onSelectIqamaSound = viewModel::selectIqamaSound,
                    onSyncSounds = { viewModel.syncAlertSoundsCatalog(manual = true) },
                    onDownloadSoundOffline = { sound -> viewModel.downloadAlertSoundOffline(sound, silent = false) },
                    onTogglePreviewSound = viewModel::toggleAlertSoundPreview,
                    onToggleAutoSilent = viewModel::setAutoSilentEnabled,
                    onToggleAutoSilentAtIqama = viewModel::setAutoSilentAtIqama,
                    onSelectAutoSilentDuration = viewModel::setAutoSilentDurationMinutes,
                    onToggleCountdownNotification = viewModel::setCountdownNotificationEnabled,
                    onToggleFridayKahfReminder = viewModel::setFridayKahfReminderEnabled,
                    onShowToast = viewModel::emitToast
                )
            }

            if (globalLocationModalOpen) {
                GlobalLocationModal(
                    currentLocation = globalLocation,
                    isDetectingLocation = isDetectingLocation,
                    searchResults = globalSearchResults,
                    isSearching = isSearchingLocations,
                    showCalculationMethodSection = showCalculationMethodPickerInModal,
                    onGpsDetected = viewModel::onGlobalGpsDetected,
                    onSearchQueryChanged = viewModel::searchGlobalLocations,
                    onSelectSearchResult = viewModel::selectGlobalSearchResult,
                    onSelectCalculationMethod = viewModel::setManualCalculationMethod,
                    onShowToast = viewModel::emitToast,
                    onDismiss = viewModel::closeGlobalLocationModal
                )
            }

            if (prayerTrackerOpen) {
                PrayerTrackerScreen(
                    stats = prayerLogStats,
                    pendingQadaaItems = pendingQadaaItems,
                    logRevision = prayerLogRevision,
                    onTogglePrayerForDate = viewModel::togglePrayerPerformedForDate,
                    onMarkQadaaDone = viewModel::markQadaaDone,
                    onToggleSunnahHabit = viewModel::toggleSunnahHabitForDate,
                    onClose = viewModel::closePrayerTracker
                )
            }

            if (aboutCopyrightOpen) {
                AboutCopyrightModal(
                    onClose = viewModel::closeAboutCopyright,
                    onShowToast = viewModel::emitToast
                )
            }

            // TOPMOST Z-LAYER: Full-Screen Inescapable Smart Fajr Alarm Ringing Overlay
            if (fajrAlarmRinging) {
                FajrAlarmRingingScreen(
                    settings = fajrAlarmSettings,
                    onChallengeSolved = viewModel::dismissFajrAlarmAfterChallenge
                )
            }

            // In-App Direct Auto-Update Dialogs (Update Available, Live Progress, Install Permission)
            AppUpdateOverlayHost(
                state = appUpdateState,
                onStartDownload = viewModel::startAppUpdateDownload,
                onCancelDownload = viewModel::cancelAppUpdateDownload,
                onDismiss = viewModel::dismissAppUpdateDialog,
                onInstallReadyApk = viewModel::installDownloadedApk,
                onPermissionRequired = viewModel::markInstallPermissionRequired,
                onShowToast = viewModel::emitToast
            )

            // One-time startup Adhan Reliability Prompt if Battery Optimization or Exact Alarms need granting
            if (showReliabilityPrompt && !permissionsStatus.adhanReliabilityReady && !fajrAlarmRinging) {
                AdhanReliabilityStartupDialog(
                    status = permissionsStatus,
                    onDisableBatteryOptimization = {
                        PermissionsHelper.openIgnoreBatteryOptimizationSettings(context)
                    },
                    onGrantExactAlarms = {
                        PermissionsHelper.openExactAlarmSettings(context)
                    },
                    onDismiss = viewModel::dismissReliabilityPrompt
                )
            }
        }
    }
}

@Composable
private fun AdhanReliabilityStartupDialog(
    status: AppPermissionsStatus,
    onDisableBatteryOptimization: () -> Unit,
    onGrantExactAlarms: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "ضمان رنين الأذان في وقته بالثانية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "حتى يعمل الأذان بدقة في الليل أو بعد إغلاق التطبيق من التطبيقات الحديثة، يُنصح بتفعيل الإذن التالي:",
                    style = MaterialTheme.typography.bodySmall
                )
                if (!status.batteryOptimizationIgnored) {
                    Button(
                        onClick = onDisableBatteryOptimization,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("startup_dialog_battery_opt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إلغاء تحسين البطارية", fontWeight = FontWeight.Bold)
                    }
                }
                if (!status.exactAlarmsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    OutlinedButton(
                        onClick = onGrantExactAlarms,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("startup_dialog_exact_alarm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تفعيل المنبهات الدقيقة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("startup_dialog_reliability_later_button")
            ) {
                Text("لاحقاً", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun RafiqTopBar(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "صباح الخير، بارك الله في يومك"
            hour < 17 -> "طاب يومك بذكر الله وطاعته"
            else -> "مساء الخير، لا تنسَ أذكار المساء"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SoftGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "شعار رفيق المسلم",
                            tint = Color(0xFFF4D88B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "رفيق المسلم",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_dark_mode_toggle")
                ) {
                    Icon(
                        imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "تبديل الوضع الليلي",
                        tint = if (darkMode) Color(0xFFF4D88B) else Color.White
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: RafiqTab, selected: Boolean): ImageVector = when (tab) {
    RafiqTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    RafiqTab.QURAN -> if (selected) Icons.AutoMirrored.Filled.MenuBook else Icons.AutoMirrored.Outlined.MenuBook
    RafiqTab.AZKAR -> if (selected) Icons.Filled.Schedule else Icons.Outlined.Schedule
    RafiqTab.HADITH -> if (selected) Icons.AutoMirrored.Filled.Article else Icons.AutoMirrored.Outlined.Article
    RafiqTab.TOOLS -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
}
