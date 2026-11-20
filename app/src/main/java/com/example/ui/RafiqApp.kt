package com.example.ui

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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.OfflineIslamicData
import com.example.ui.components.StickyQuranMiniPlayer
import com.example.ui.screens.AzkarScreen
import com.example.ui.screens.FajrAlarmRingingScreen
import com.example.ui.screens.FajrAlarmSettingsModal
import com.example.ui.screens.HadithScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MuezzinLibraryModal
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
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val prayerState by viewModel.prayerState.collectAsStateWithLifecycle()
    val worshipTasks by viewModel.worshipTasks.collectAsStateWithLifecycle()
    val inspirationIdx by viewModel.inspirationIndex.collectAsStateWithLifecycle()
    val bookmark by viewModel.bookmark.collectAsStateWithLifecycle()
    val quranState by viewModel.quranState.collectAsStateWithLifecycle()
    val quranFontSize by viewModel.quranFontSize.collectAsStateWithLifecycle()
    val mushafPaperTheme by viewModel.mushafPaperTheme.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val ayahAudioState by viewModel.ayahAudioState.collectAsStateWithLifecycle()
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

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isFullScreenQuran = selectedTab == RafiqTab.QURAN &&
        quranState.activeSurah != null &&
        quranState.isFullScreenReading

    // Back press from secondary tabs returns to Home
    if (selectedTab != RafiqTab.HOME &&
        quranState.activeSurah == null &&
        !muezzinLibraryOpen &&
        !reciterLibraryOpen &&
        !fajrAlarmSettingsOpen &&
        !fajrAlarmRinging
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
                                        onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                        onOpenReciterLibrary = viewModel::openReciterLibrary,
                                        onDismiss = viewModel::dismissMiniPlayer
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
                                    onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                    onOpenReciterLibrary = viewModel::openReciterLibrary,
                                    onDismiss = viewModel::dismissMiniPlayer,
                                    modifier = Modifier.widthIn(max = 620.dp)
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
                                    selectedCity = selectedCity,
                                    prayerState = prayerState,
                                    activeMuezzin = activeDefaultMuezzin,
                                    adhanState = adhanState,
                                    fajrAlarmSettings = fajrAlarmSettings,
                                    fajrWakeupTime = fajrWakeupTime,
                                    worshipTasks = worshipTasks,
                                    bookmark = bookmark,
                                    dailyVerse = dailyVerse,
                                    dailyHadith = dailyHadith,
                                    onSelectCity = viewModel::selectCity,
                                    onRefreshPrayers = viewModel::refreshPrayerTimes,
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
                                    bookmark = bookmark,
                                    audioState = audioState,
                                    ayahAudioState = ayahAudioState,
                                    onSearchChange = viewModel::updateSurahSearch,
                                    onFilterChange = viewModel::updateRevelationFilter,
                                    onOpenSurah = { viewModel.openSurahReader(it) },
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
                                    onDownloadMp3 = viewModel::downloadCurrentSurahMp3,
                                    onTogglePlaySingleAyah = viewModel::togglePlaySingleAyah,
                                    onStopSingleAyah = viewModel::stopSingleAyahAudio,
                                    onSelectAyahRepeatMode = viewModel::setAyahRepeatMode,
                                    onToggleAyahContinueReading = viewModel::setAyahContinueReading,
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
                                    favoriteUids = favoriteHadithUids,
                                    hadithFontSizeSp = hadithFontSize,
                                    onSelectBook = viewModel::selectHadithBook,
                                    onSearchChange = viewModel::updateHadithSearch,
                                    onToggleSearchAllBooks = viewModel::toggleHadithSearchAllBooks,
                                    onSelectChapter = viewModel::selectHadithChapter,
                                    onToggleExplanation = viewModel::toggleHadithExplanation,
                                    onToggleFavorite = viewModel::toggleFavoriteHadith,
                                    onFontSizeChange = viewModel::updateHadithFontSize,
                                    onLoadMore = viewModel::loadMoreHadiths,
                                    onRetryCollection = { viewModel.fetchHadithCollection(hadithState.selectedBook) },
                                    onShowToast = viewModel::emitToast
                                )

                                RafiqTab.TOOLS -> ToolsScreen(
                                    state = toolsState,
                                    activeMuezzin = activeDefaultMuezzin,
                                    fajrAlarmSettings = fajrAlarmSettings,
                                    fajrWakeupTime = fajrWakeupTime,
                                    darkMode = darkMode,
                                    onOpenMuezzinLibrary = viewModel::openMuezzinLibrary,
                                    onOpenFajrAlarm = viewModel::openFajrAlarmSettings,
                                    onGpsLocation = viewModel::onGpsLocationDetected,
                                    onHeadingChanged = viewModel::updateDeviceHeading,
                                    onToggleCompassSensor = viewModel::setCompassSensorEnabled,
                                    onToggleGuide = viewModel::toggleGuideAccordion,
                                    onToggleDarkMode = viewModel::toggleDarkMode,
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
                    favoriteIds = favoriteMuezzinIds,
                    adhanState = adhanState,
                    onClose = viewModel::closeMuezzinLibrary,
                    onTogglePreview = viewModel::toggleAdhanPreview,
                    onSwitchFallbackStream = viewModel::switchAdhanFallbackStream,
                    onRetryAdhan = viewModel::retryAdhanPreview,
                    onToggleFavorite = viewModel::toggleFavoriteMuezzin,
                    onSetDefaultMuezzin = viewModel::setDefaultMuezzin,
                    onSetFajrMuezzin = viewModel::setFajrMuezzin,
                    onToggleDedicatedFajr = viewModel::setUseDedicatedFajrAdhan,
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

            // TOPMOST Z-LAYER: Full-Screen Inescapable Smart Fajr Alarm Ringing Overlay
            if (fajrAlarmRinging) {
                FajrAlarmRingingScreen(
                    settings = fajrAlarmSettings,
                    onChallengeSolved = viewModel::dismissFajrAlarmAfterChallenge
                )
            }
        }
    }
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
