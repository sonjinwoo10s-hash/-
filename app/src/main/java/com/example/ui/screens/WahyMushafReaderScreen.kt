package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioState
import com.example.data.local.PersistentOfflineStorage
import com.example.data.model.AyahRepeatMode
import com.example.data.model.MushafPaperTheme
import com.example.data.model.QuranBookmark
import com.example.data.repository.MushafPageAyah
import com.example.data.repository.MushafPageData
import com.example.data.repository.MushafSurahBlock
import com.example.data.repository.OfflineQuranRepository
import com.example.data.repository.TafsirBook
import com.example.quran.TajweedEngine
import com.example.quran.TajweedEngine.appendUthmaniWithTajweed
import com.example.ui.components.ArabicOfflineErrorCard
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.NightReadPaper
import com.example.ui.theme.SoftGold
import android.util.LruCache
import androidx.compose.ui.text.AnnotatedString
import com.example.ui.viewmodel.QuranUiState
import com.example.ui.viewmodel.TafsirUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private data class CachedBlockAnnotated(
    val baseAnnotated: AnnotatedString,
    val ayahTextStartOffsets: IntArray,
    val ayahFullEndOffsets: IntArray
)

// Tiny max-5-page LRU cache for Tajweed-colored Mushaf blocks so Tajweed never re-runs
// on scroll/tap/80ms audio highlight ticks while keeping RAM usage tiny on 2GB-3GB devices.
private val mushafBlockAnnotatedCache = LruCache<String, CachedBlockAnnotated>(5)

@Composable
fun WahyMushafReaderScreen(
    state: QuranUiState,
    fontSizeSp: Float,
    paperTheme: MushafPaperTheme,
    tajweedColorsEnabled: Boolean,
    bookmark: QuranBookmark?,
    audioState: AudioPlayerState,
    ayahAudioState: AyahAudioState,
    activeVerseKey: String?,
    activeWordIndex: Int?,
    tafsirState: TafsirUiState,
    onCloseMushaf: () -> Unit,
    onRetryOfflineLoad: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleTajweedColors: () -> Unit,
    onSavePageBookmark: (pageNumber: Int, surahNumber: Int, surahName: String, ayahNumber: Int) -> Unit,
    onToggleSurahAudio: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onToggleWordByWord: (Boolean) -> Unit,
    onSyncLatencyOffsetChange: (Int) -> Unit,
    onTapSeekWordOrAyah: (surahNumber: Int, ayahNumber: Int, wordIndexZeroBased: Int?) -> Boolean,
    onDownloadMp3: () -> Unit,
    onPlaySingleAyahOnly: (MushafPageAyah) -> Unit,
    onPlayFromPageAyah: (MushafPageAyah) -> Unit,
    onStopSingleAyah: () -> Unit,
    onSelectAyahRepeatMode: (AyahRepeatMode) -> Unit,
    onToggleAyahContinueReading: (Boolean) -> Unit,
    onOpenAyahTafsir: (surahNumber: Int, ayahNumber: Int) -> Unit,
    onSelectTafsirBook: (TafsirBook) -> Unit,
    onRetryTafsir: () -> Unit,
    onDownloadTafsirBook: (TafsirBook) -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val isNightMode = paperTheme == MushafPaperTheme.NIGHT
    val paperBg = if (isNightMode) NightReadPaper else MushafCreamPaper
    val textColor = if (isNightMode) Color(0xFFEDE8DC) else Color(0xFF1F1B14)
    val subtleColor = if (isNightMode) Color(0xFFA7B5AD) else Color(0xFF6B5D43)
    val goldAccent = if (isNightMode) Color(0xFFE7C875) else Color(0xFF9A7422)
    val outerFrameColor = if (isNightMode) Color(0xFF3E4E45) else Color(0xFFCFA958)
    val innerFrameColor = if (isNightMode) Color(0xFF23322B) else Color(0xFF0D5C46).copy(alpha = 0.42f)

    var controlsVisible by remember { mutableStateOf(true) }
    var showFontPanel by remember { mutableStateOf(false) }
    var showAudioPanel by remember { mutableStateOf(false) }
    var showSurahIndexSheet by remember { mutableStateOf(false) }
    var showJuzSheet by remember { mutableStateOf(false) }
    var showTajweedGuideSheet by remember { mutableStateOf(false) }
    var longPressedAyah by remember { mutableStateOf<MushafPageAyah?>(null) }

    // Dynamic bar height measurements so the top/bottom chrome NEVER clips or overlaps Mushaf content
    val statusBarTopDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottomDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var measuredTopBarPx by remember { mutableIntStateOf(0) }
    var measuredBottomBarPx by remember { mutableIntStateOf(0) }

    val topBarTotalDp = if (measuredTopBarPx > 0) {
        with(density) { measuredTopBarPx.toDp() }
    } else {
        statusBarTopDp + 106.dp
    }
    val bottomBarTotalDp = if (measuredBottomBarPx > 0) {
        with(density) { measuredBottomBarPx.toDp() }
    } else {
        navBarBottomDp + 72.dp
    }

    val pageTopInset by animateDpAsState(
        targetValue = if (controlsVisible) topBarTotalDp + 4.dp else statusBarTopDp + 6.dp,
        label = "mushaf_page_top_inset"
    )
    val pageBottomInset by animateDpAsState(
        targetValue = if (controlsVisible) bottomBarTotalDp + 4.dp else navBarBottomDp + 6.dp,
        label = "mushaf_page_bottom_inset"
    )

    BackHandler {
        when {
            longPressedAyah != null -> longPressedAyah = null
            showTajweedGuideSheet -> showTajweedGuideSheet = false
            showSurahIndexSheet -> showSurahIndexSheet = false
            showJuzSheet -> showJuzSheet = false
            showFontPanel -> showFontPanel = false
            showAudioPanel -> showAudioPanel = false
            else -> onCloseMushaf()
        }
    }

    // Loading / first-launch download / offline error state
    if (state.isLoadingOfflineMushaf || state.mushafPages.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(paperBg)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (state.isLoadingOfflineMushaf) {
                    CircularProgressIndicator(color = if (isNightMode) goldAccent else EmeraldGreen)
                    Text(
                        text = state.mushafDownloadProgress ?: "جاري تجهيز المصحف الشريف (٦٠٤ صفحة)...",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يتم التحميل مرة واحدة فقط ليصبح النص متاحاً ١٠٠٪ بدون إنترنت دائماً",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtleColor,
                        textAlign = TextAlign.Center
                    )
                } else {
                    ArabicOfflineErrorCard(
                        message = "لا يوجد اتصال بالإنترنت — إعادة المحاولة",
                        subtitle = state.mushafError ?: "يلزم الاتصال بالإنترنت لمرة واحدة فقط لتحميل نص المصحف الشريف وحفظه أوفلاين.",
                        retryLabel = "تحميل المصحف أوفلاين",
                        onRetry = onRetryOfflineLoad
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, outerFrameColor),
                        modifier = Modifier.clickable(onClick = onCloseMushaf)
                    ) {
                        Text(
                            text = "رجوع",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        return
    }

    val pages = state.mushafPages
    val initialIndex = remember(pages.size) {
        (state.currentMushafPage - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
    }
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { pages.size }
    )

    // Unified synchronized Ayah & Word-by-Word highlight state directly observed from activeVerseKey / activeWordIndex.
    val parsedActiveVerse = remember(activeVerseKey) {
        activeVerseKey?.split(":")?.let { parts ->
            val s = parts.getOrNull(0)?.toIntOrNull()
            val a = parts.getOrNull(1)?.toIntOrNull()
            if (s != null && a != null) s to a else null
        }
    }
    val syncedSurah: Int? = parsedActiveVerse?.first ?: when {
        ayahAudioState.isPlaying || ayahAudioState.isBuffering -> ayahAudioState.surahNumber
        audioState.hasOfficialTimings && audioState.syncedAyahNumber != null -> audioState.loadedSurahNumber
        else -> null
    }
    val syncedAyah: Int? = parsedActiveVerse?.second ?: when {
        ayahAudioState.isPlaying || ayahAudioState.isBuffering -> ayahAudioState.ayahNumber
        audioState.hasOfficialTimings -> audioState.syncedAyahNumber
        else -> null
    }
    val syncedWordIndex: Int? = activeWordIndex ?: when {
        ayahAudioState.isPlaying -> ayahAudioState.activeWordIndex
        audioState.hasOfficialTimings && audioState.wordByWordEnabled -> audioState.syncedWordIndex
        else -> null
    }
    val canTapToSeekActiveSurah = audioState.hasOfficialTimings && audioState.isPlaying && audioState.loadedSurahNumber != null

    // Sync programmatic page jumps (from Index / Juz / Bookmark / Surah list)
    LaunchedEffect(state.currentMushafPage, pages.size) {
        val targetIndex = (state.currentMushafPage - 1).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
        if (pagerState.currentPage != targetIndex) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }

    // AUTO-PAGE TURN: Automatically swipe HorizontalPager when reciter advances to an Ayah on another page
    LaunchedEffect(syncedSurah, syncedAyah, pages.size) {
        val s = syncedSurah ?: return@LaunchedEffect
        val a = syncedAyah ?: return@LaunchedEffect
        val targetPageIdx = pages.indexOfFirst { page ->
            page.blocks.any { b -> b.surahNumber == s && b.ayahs.any { it.numberInSurah == a } }
        }
        if (targetPageIdx >= 0 && targetPageIdx != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPageIdx)
        }
    }

    // Report user horizontal RTL page swipes back to ViewModel
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { index ->
                val pageNum = pages.getOrNull(index)?.pageNumber ?: (index + 1)
                onPageChanged(pageNum)
            }
    }

    val activePage = pages.getOrNull(pagerState.currentPage) ?: pages.first()
    val isCurrentPageBookmarked = bookmark?.pageNumber == activePage.pageNumber

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paperBg)
            .testTag("wahy_mushaf_reader_screen")
    ) {
        // Authentic RTL 604-page Horizontal Pager (Swipe right-to-left to advance pages 1 -> 604)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("mushaf_horizontal_pager")
            ) { pageIndex ->
                val pageData = pages[pageIndex]
                WahyMushafSinglePage(
                    pageData = pageData,
                    fontSizeSp = fontSizeSp,
                    isNightMode = isNightMode,
                    tajweedColorsEnabled = tajweedColorsEnabled,
                    paperBg = paperBg,
                    textColor = textColor,
                    subtleColor = subtleColor,
                    goldAccent = goldAccent,
                    outerFrameColor = outerFrameColor,
                    innerFrameColor = innerFrameColor,
                    bookmarkedSurah = bookmark?.surahNumber,
                    bookmarkedAyah = bookmark?.ayahNumber,
                    playingSurah = syncedSurah,
                    playingAyah = syncedAyah,
                    playingWordIndex = syncedWordIndex,
                    canTapToSeek = canTapToSeekActiveSurah,
                    selectedAyah = longPressedAyah,
                    topContentInset = pageTopInset,
                    bottomContentInset = pageBottomInset,
                    onTapToggleControls = {
                        controlsVisible = !controlsVisible
                        if (!controlsVisible) {
                            showFontPanel = false
                            showAudioPanel = false
                        }
                    },
                    onTapWordOrAyah = { tappedAyah, wordIdx ->
                        val sought = onTapSeekWordOrAyah(
                            tappedAyah.surahNumber,
                            tappedAyah.numberInSurah,
                            wordIdx
                        )
                        if (sought) {
                            performKeyTapHaptic(view)
                        }
                        sought
                    },
                    onLongPressAyah = { pressedAyah ->
                        performKeyTapHaptic(view)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        longPressedAyah = pressedAyah
                        onOpenAyahTafsir(pressedAyah.surahNumber, pressedAyah.numberInSurah)
                    }
                )
            }
        }

        // TOP OVERLAY TOOLBAR (Tap-to-Toggle + Tajweed Toggle & Guide)
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val chromeBg = if (isNightMode) Color(0xEE141B18) else Color(0xF2F7EFE0)
            val chromeBorder = if (isNightMode) Color(0xFF2A3932) else Color(0xFFDCC89B)
            Surface(
                color = chromeBg,
                shadowElevation = 0.dp,
                border = BorderStroke(0.5.dp, chromeBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (size.height > 0) measuredTopBarPx = size.height
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCloseMushaf,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mushaf_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "العودة لقائمة السور",
                                tint = textColor
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = activePage.primarySurahName,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "صفحة ${toArabicIndicDigits(activePage.pageNumber)} من ${toArabicIndicDigits(pages.size)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = subtleColor
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = goldAccent
                                )
                                Text(
                                    text = OfflineQuranRepository.getJuzArabicName(activePage.juzNumber),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = goldAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                if (state.isMushafCachedOffline) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = EmeraldGreen.copy(alpha = if (isNightMode) 0.28f else 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = if (isNightMode) Color(0xFF69D2A8) else EmeraldGreen,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "أوفلاين",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isNightMode) Color(0xFF69D2A8) else EmeraldGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                performKeyTapHaptic(view)
                                val firstAyah = activePage.blocks.firstOrNull()?.ayahs?.firstOrNull()
                                onSavePageBookmark(
                                    activePage.pageNumber,
                                    activePage.primarySurahNumber,
                                    activePage.primarySurahName,
                                    firstAyah?.numberInSurah ?: 1
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mushaf_top_bookmark_button")
                        ) {
                            Icon(
                                imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "حفظ علامة لهذه الصفحة",
                                tint = if (isCurrentPageBookmarked) SoftGold else textColor
                            )
                        }
                    }

                    // Compact Tajweed Toolbar Strip: "تلوين التجويد" Toggle + "دليل التجويد" Guide Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (tajweedColorsEnabled) {
                                EmeraldGreen
                            } else {
                                if (isNightMode) Color(0xFF1F2A25) else Color(0xFFEDE4CE)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (tajweedColorsEnabled) EmeraldGreen else chromeBorder
                            ),
                            modifier = Modifier
                                .testTag("mushaf_tajweed_toggle_button")
                                .clickable(onClick = onToggleTajweedColors)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "تلوين التجويد",
                                    tint = if (tajweedColorsEnabled) Color(0xFFF6DC97) else goldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (tajweedColorsEnabled) "تلوين التجويد: مفعّل" else "تلوين التجويد",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (tajweedColorsEnabled) Color.White else textColor
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNightMode) Color(0xFF1F2A25) else Color(0xFFEDE4CE),
                            border = BorderStroke(1.dp, chromeBorder),
                            modifier = Modifier
                                .testTag("mushaf_tajweed_guide_button")
                                .clickable { showTajweedGuideSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "دليل التجويد",
                                    tint = goldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "دليل التجويد",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM OVERLAY CONTROLS DOCK
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                val chromeBg = if (isNightMode) Color(0xF0141B18) else Color(0xF5F7EFE0)
                val chromeBorder = if (isNightMode) Color(0xFF2A3932) else Color(0xFFDCC89B)
                Surface(
                    color = chromeBg,
                    shadowElevation = 0.dp,
                    border = BorderStroke(0.5.dp, chromeBorder),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            if (size.height > 0) measuredBottomBarPx = size.height
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Expandable Font Size Controls
                        AnimatedVisibility(visible = showFontPanel) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isNightMode) Color(0xFF1A221E) else Color(0xFFFDF8EC),
                                border = BorderStroke(1.dp, chromeBorder)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EmeraldGreen.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("mushaf_font_decrease")
                                                .clickable { onFontSizeChange((fontSizeSp - 2f).coerceAtLeast(18f)) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("A-", fontWeight = FontWeight.ExtraBold, color = textColor)
                                            }
                                        }

                                        Slider(
                                            value = fontSizeSp,
                                            onValueChange = onFontSizeChange,
                                            valueRange = 18f..36f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(
                                                thumbColor = SoftGold,
                                                activeTrackColor = EmeraldGreen
                                            )
                                        )

                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = EmeraldGreen.copy(alpha = 0.14f),
                                            modifier = Modifier
                                                .size(40.dp)
                                                .testTag("mushaf_font_increase")
                                                .clickable { onFontSizeChange((fontSizeSp + 2f).coerceAtMost(36f)) }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("A+", fontWeight = FontWeight.ExtraBold, color = textColor)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "اضغط مطوّلاً على أي آية للتفسير والاستماع • ضغطة واحدة لإخفاء أو إظهار القوائم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = subtleColor,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Expandable Online Audio Player Panel (with Playback Speed, Word-by-Word & Latency Calibration)
                        AnimatedVisibility(visible = showAudioPanel) {
                            val currentAudioKey = PersistentOfflineStorage.surahAudioKey(
                                audioState.selectedReciter.id,
                                activePage.primarySurahNumber
                            )
                            WahyOnlineAudioPanel(
                                currentSurahNumber = activePage.primarySurahNumber,
                                currentSurahName = activePage.primarySurahName,
                                audioState = audioState,
                                ayahAudioState = ayahAudioState,
                                isNightMode = isNightMode,
                                onToggleSurahAudio = onToggleSurahAudio,
                                onOpenReciterLibrary = onOpenReciterLibrary,
                                onSeekAudio = onSeekAudio,
                                onSelectPlaybackSpeed = onSelectPlaybackSpeed,
                                onToggleWordByWord = onToggleWordByWord,
                                onSyncLatencyOffsetChange = onSyncLatencyOffsetChange,
                                onDownloadMp3 = onDownloadMp3,
                                onStopSingleAyah = onStopSingleAyah,
                                isCurrentSurahDownloaded = state.downloadedSurahAudioKeys.contains(currentAudioKey),
                                isCurrentSurahDownloading = state.downloadingSurahAudioKeys.contains(currentAudioKey)
                            )
                        }

                        // Main 6-Button Wahy Control Dock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MushafDockAction(
                                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                                label = "الفهرس",
                                isActive = showSurahIndexSheet,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_index",
                                onClick = { showSurahIndexSheet = true }
                            )
                            MushafDockAction(
                                icon = Icons.Default.GridView,
                                label = "الأجزاء",
                                isActive = showJuzSheet,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_juz",
                                onClick = { showJuzSheet = true }
                            )
                            MushafDockAction(
                                icon = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                label = "العلامة",
                                isActive = isCurrentPageBookmarked,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_bookmark",
                                onClick = {
                                    performKeyTapHaptic(view)
                                    if (bookmark != null && !isCurrentPageBookmarked) {
                                        scope.launch {
                                            val idx = (bookmark.pageNumber - 1).coerceIn(0, pages.lastIndex)
                                            pagerState.animateScrollToPage(idx)
                                        }
                                        onShowToast("الانتقال إلى العلامة: ${bookmark.surahName} - صفحة ${toArabicIndicDigits(bookmark.pageNumber)}")
                                    } else {
                                        val firstAyah = activePage.blocks.firstOrNull()?.ayahs?.firstOrNull()
                                        onSavePageBookmark(
                                            activePage.pageNumber,
                                            activePage.primarySurahNumber,
                                            activePage.primarySurahName,
                                            firstAyah?.numberInSurah ?: 1
                                        )
                                    }
                                }
                            )
                            MushafDockAction(
                                icon = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                label = if (isNightMode) "نهاري" else "ليلي",
                                isActive = isNightMode,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_night",
                                onClick = onToggleNightMode
                            )
                            MushafDockAction(
                                icon = Icons.Default.FormatSize,
                                label = "الخط",
                                isActive = showFontPanel,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_font",
                                onClick = {
                                    showFontPanel = !showFontPanel
                                    if (showFontPanel) showAudioPanel = false
                                }
                            )
                            val isAnyAudioActive = audioState.isPlaying || ayahAudioState.isPlaying
                            MushafDockAction(
                                icon = Icons.Default.Headphones,
                                label = "التلاوة",
                                isActive = showAudioPanel || isAnyAudioActive,
                                isNightMode = isNightMode,
                                testTag = "mushaf_dock_audio",
                                onClick = {
                                    showAudioPanel = !showAudioPanel
                                    if (showAudioPanel) showFontPanel = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Interactive Tajweed Pronunciation Guide Sheet
    if (showTajweedGuideSheet) {
        WahyTajweedGuideSheet(
            tajweedColorsEnabled = tajweedColorsEnabled,
            isNightMode = isNightMode,
            onToggleTajweedColors = onToggleTajweedColors,
            onDismiss = { showTajweedGuideSheet = false }
        )
    }

    // Surah Index Bottom Sheet
    if (showSurahIndexSheet) {
        WahySurahIndexSheet(
            surahs = state.surahs,
            surahStartPages = state.surahStartPages,
            currentSurahNumber = activePage.primarySurahNumber,
            onSelectSurahPage = { targetPage ->
                scope.launch {
                    val idx = (targetPage - 1).coerceIn(0, pages.lastIndex)
                    pagerState.scrollToPage(idx)
                }
            },
            onDismiss = { showSurahIndexSheet = false }
        )
    }

    // 30-Juz Picker Bottom Sheet
    if (showJuzSheet) {
        WahyJuzPickerSheet(
            juzStartPages = state.juzStartPages,
            currentJuz = activePage.juzNumber,
            onSelectJuzPage = { targetPage ->
                scope.launch {
                    val idx = (targetPage - 1).coerceIn(0, pages.lastIndex)
                    pagerState.scrollToPage(idx)
                }
            },
            onDismiss = { showJuzSheet = false }
        )
    }

    // All-in-One Ayah Long-Press Sheet (Actions + Authentic Arabic Tafsir Library)
    longPressedAyah?.let { pageAyah ->
        WahyAyahTafsirSheet(
            ayah = pageAyah,
            isNightMode = isNightMode,
            tajweedColorsEnabled = tajweedColorsEnabled,
            isBookmarked = bookmark?.surahNumber == pageAyah.surahNumber &&
                bookmark.ayahNumber == pageAyah.numberInSurah,
            audioState = audioState,
            ayahAudioState = ayahAudioState,
            tafsirState = tafsirState,
            onDismiss = { longPressedAyah = null },
            onPlayThisAyahOnly = {
                onPlaySingleAyahOnly(pageAyah)
                showAudioPanel = true
                controlsVisible = true
                longPressedAyah = null
            },
            onPlayFromHere = {
                onPlayFromPageAyah(pageAyah)
                showAudioPanel = true
                controlsVisible = true
                longPressedAyah = null
            },
            onSelectRepeatMode = onSelectAyahRepeatMode,
            onSaveBookmark = {
                onSavePageBookmark(
                    pageAyah.page,
                    pageAyah.surahNumber,
                    pageAyah.surahName,
                    pageAyah.numberInSurah
                )
            },
            onCopyAyah = {
                copyTextToClipboard(
                    context,
                    "${pageAyah.text} ﴿${toArabicIndicDigits(pageAyah.numberInSurah)}﴾ — ${pageAyah.surahName}"
                )
                onShowToast("تم نسخ الآية")
            },
            onShareAyah = {
                sharePlainText(
                    context,
                    "${pageAyah.text} ﴿${toArabicIndicDigits(pageAyah.numberInSurah)}﴾\n[${pageAyah.surahName} - صفحة ${pageAyah.page}]"
                )
            },
            onSelectTafsirBook = onSelectTafsirBook,
            onRetryTafsir = onRetryTafsir,
            onDownloadTafsirBook = onDownloadTafsirBook,
            onCopyTafsir = { tafsirText ->
                copyTextToClipboard(
                    context,
                    "${tafsirState.selectedBook.arabicName} — ${pageAyah.surahName} (الآية ${toArabicIndicDigits(pageAyah.numberInSurah)}):\n$tafsirText"
                )
                onShowToast("تم نسخ التفسير")
            }
        )
    }
}

@Composable
private fun MushafDockAction(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    isNightMode: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val activeColor = if (isNightMode) Color(0xFFE7C875) else EmeraldGreen
    val inactiveColor = if (isNightMode) Color(0xFFC5D0C9) else Color(0xFF4A402E)
    val tint = if (isActive) activeColor else inactiveColor

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 54.dp, height = 52.dp)
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun WahyMushafSinglePage(
    pageData: MushafPageData,
    fontSizeSp: Float,
    isNightMode: Boolean,
    tajweedColorsEnabled: Boolean,
    paperBg: Color,
    textColor: Color,
    subtleColor: Color,
    goldAccent: Color,
    outerFrameColor: Color,
    innerFrameColor: Color,
    bookmarkedSurah: Int?,
    bookmarkedAyah: Int?,
    playingSurah: Int?,
    playingAyah: Int?,
    playingWordIndex: Int?,
    canTapToSeek: Boolean,
    selectedAyah: MushafPageAyah?,
    topContentInset: Dp,
    bottomContentInset: Dp,
    onTapToggleControls: () -> Unit,
    onTapWordOrAyah: (MushafPageAyah, Int?) -> Boolean,
    onLongPressAyah: (MushafPageAyah) -> Unit
) {
    val scrollState = rememberScrollState()
    // Dual-layer highlight: subtle emerald background on active Ayah + crisp golden highlight on active Word
    val activeAyahTint = if (isNightMode) Color(0x3D4CAF50) else Color(0x2A2E7D32)
    val activeWordTint = if (isNightMode) Color(0x8CE7B84A) else Color(0x99F2C94C)
    val bookmarkHighlight = if (isNightMode) Color(0xFF28342C) else Color(0xFFF3E2B0)
    val isOpeningSpread = pageData.pageNumber == 1 || pageData.pageNumber == 2

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(paperBg)
            .padding(
                start = 10.dp,
                end = 10.dp,
                top = topContentInset,
                bottom = bottomContentInset
            )
            .pointerInput(pageData.pageNumber) {
                detectTapGestures(onTap = { onTapToggleControls() })
            }
    ) {
        // Subtle traditional decorative double border frame
        Canvas(modifier = Modifier.matchParentSize()) {
            val outerInset = 2.dp.toPx()
            val innerInset = 7.dp.toPx()
            drawRoundRect(
                color = outerFrameColor,
                topLeft = Offset(outerInset, outerInset),
                size = Size(size.width - outerInset * 2, size.height - outerInset * 2),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 1.6.dp.toPx())
            )
            drawRoundRect(
                color = innerFrameColor,
                topLeft = Offset(innerInset, innerInset),
                size = Size(size.width - innerInset * 2, size.height - innerInset * 2),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 0.9.dp.toPx())
            )
            val r = 3.2.dp.toPx()
            val cOff = 12.dp.toPx()
            val corners = listOf(
                Offset(cOff, cOff),
                Offset(size.width - cOff, cOff),
                Offset(cOff, size.height - cOff),
                Offset(size.width - cOff, size.height - cOff)
            )
            corners.forEach { center ->
                drawCircle(color = outerFrameColor, radius = r, center = center)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. TOP PAGE HEADER: Surah name on the right (Start), Juz name on the left (End)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pageData.primarySurahName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = AmiriFontFamily,
                        fontSize = 15.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = subtleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = OfflineQuranRepository.getJuzArabicName(pageData.juzNumber),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = AmiriFontFamily,
                        fontSize = 15.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = goldAccent,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                color = outerFrameColor.copy(alpha = 0.45f),
                thickness = 0.8.dp
            )

            // 2. MUSHAF BODY: Continuous justified Uthmani Arabic text with optional Tajweed + Synced Word Highlighting
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(vertical = 10.dp),
                verticalArrangement = if (isOpeningSpread) Arrangement.Center else Arrangement.Top
            ) {
                pageData.blocks.forEachIndexed { blockIdx, block ->
                    if (blockIdx > 0) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (block.startsOnThisPage) {
                        WahySurahTitleBanner(
                            block = block,
                            isNightMode = isNightMode,
                            textColor = textColor,
                            goldAccent = goldAccent,
                            frameColor = outerFrameColor
                        )
                        if (block.surahNumber != 1 && block.surahNumber != 9) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = AmiriQuranFontFamily,
                                    fontSize = (fontSizeSp * 0.94f).coerceIn(19f, 30f).sp,
                                    lineHeight = (fontSizeSp * 1.75f).sp
                                ),
                                color = textColor,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    WahyContinuousAyahParagraph(
                        block = block,
                        fontSizeSp = fontSizeSp,
                        isNightMode = isNightMode,
                        tajweedColorsEnabled = tajweedColorsEnabled,
                        textColor = textColor,
                        goldAccent = goldAccent,
                        activeAyahTint = activeAyahTint,
                        activeWordTint = activeWordTint,
                        bookmarkHighlight = bookmarkHighlight,
                        bookmarkedSurah = bookmarkedSurah,
                        bookmarkedAyah = bookmarkedAyah,
                        playingSurah = playingSurah,
                        playingAyah = playingAyah,
                        playingWordIndex = playingWordIndex,
                        canTapToSeek = canTapToSeek && playingSurah == block.surahNumber,
                        selectedAyah = selectedAyah,
                        centerAlign = isOpeningSpread,
                        onTapToggleControls = onTapToggleControls,
                        onTapWordOrAyah = onTapWordOrAyah,
                        onLongPressAyah = onLongPressAyah
                    )
                }
            }

            // 3. BOTTOM PAGE FOOTER: Centered Mushaf page number (١ to ٦٠٤)
            HorizontalDivider(
                color = outerFrameColor.copy(alpha = 0.35f),
                thickness = 0.7.dp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(outerFrameColor.copy(alpha = 0.55f))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = outerFrameColor.copy(alpha = if (isNightMode) 0.16f else 0.14f),
                    border = BorderStroke(0.8.dp, outerFrameColor.copy(alpha = 0.65f))
                ) {
                    Text(
                        text = toArabicIndicDigits(pageData.pageNumber),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = AmiriFontFamily,
                            fontSize = 15.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(1.dp)
                        .background(outerFrameColor.copy(alpha = 0.55f))
                )
            }
        }
    }
}

@Composable
private fun WahySurahTitleBanner(
    block: MushafSurahBlock,
    isNightMode: Boolean,
    textColor: Color,
    goldAccent: Color,
    frameColor: Color
) {
    val bannerBg = if (isNightMode) Color(0xFF1A2520) else Color(0xFFF3EAD4)
    val revArabic = if (block.revelationType == "Meccan") "مكية" else "مدنية"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bannerBg,
        border = BorderStroke(1.2.dp, frameColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = revArabic,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = goldAccent
            )

            Text(
                text = "۞  ${block.surahName}  ۞",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = AmiriFontFamily,
                    fontSize = 20.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${toArabicIndicDigits(block.totalSurahAyahs)} آية",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = goldAccent
            )
        }
    }
}

@Composable
private fun WahyContinuousAyahParagraph(
    block: MushafSurahBlock,
    fontSizeSp: Float,
    isNightMode: Boolean,
    tajweedColorsEnabled: Boolean,
    textColor: Color,
    goldAccent: Color,
    activeAyahTint: Color,
    activeWordTint: Color,
    bookmarkHighlight: Color,
    bookmarkedSurah: Int?,
    bookmarkedAyah: Int?,
    playingSurah: Int?,
    playingAyah: Int?,
    playingWordIndex: Int?,
    canTapToSeek: Boolean,
    selectedAyah: MushafPageAyah?,
    centerAlign: Boolean,
    onTapToggleControls: () -> Unit,
    onTapWordOrAyah: (MushafPageAyah, Int?) -> Boolean,
    onLongPressAyah: (MushafPageAyah) -> Unit
) {
    val isBlockPlaying = playingSurah == block.surahNumber
    val effectiveWordIdx = if (isBlockPlaying) playingWordIndex else null

    // Stage 1: Build & cache Tajweed-colored base AnnotatedString in a tiny 5-page LRU cache.
    // Runs ONLY ONCE per block/page — NEVER re-runs on tap, scroll, or 80ms audio highlight ticks!
    val baseBlockData = remember(
        block,
        isNightMode,
        tajweedColorsEnabled,
        textColor,
        goldAccent,
        fontSizeSp
    ) {
        val firstAyahNum = block.ayahs.firstOrNull()?.numberInSurah ?: 0
        val cacheKey = "${block.surahNumber}|$firstAyahNum|${block.ayahs.size}|$isNightMode|$tajweedColorsEnabled|${fontSizeSp.toInt()}"
        mushafBlockAnnotatedCache.get(cacheKey) ?: run {
            val starts = IntArray(block.ayahs.size)
            val ends = IntArray(block.ayahs.size)
            val baseStr = buildAnnotatedString {
                block.ayahs.forEachIndexed { idx, ayah ->
                    starts[idx] = length
                    pushStringAnnotation(tag = "AYAH_IDX", annotation = idx.toString())
                    TajweedEngine.run {
                        appendUthmaniWithTajweed(
                            text = ayah.text,
                            tajweedEnabled = tajweedColorsEnabled,
                            isNightMode = isNightMode,
                            baseTextColor = textColor,
                            backgroundColor = Color.Unspecified,
                            activeWordIndex = null,
                            activeWordBackground = Color.Unspecified
                        )
                    }
                    withStyle(
                        SpanStyle(
                            color = goldAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSizeSp * 0.80f).sp
                        )
                    ) {
                        append(" \uFD3F${toArabicIndicDigits(ayah.numberInSurah)}\uFD3E ")
                    }
                    pop()
                    ends[idx] = length
                }
            }
            CachedBlockAnnotated(baseStr, starts, ends).also {
                mushafBlockAnnotatedCache.put(cacheKey, it)
            }
        }
    }

    // Stage 2: Lightweight background-only overlay for bookmarked / selected / playing Ayah & active word.
    // Performs ZERO Tajweed regex/cluster parsing and returns baseAnnotated directly when idle!
    val annotatedText = remember(
        baseBlockData,
        block,
        activeAyahTint,
        activeWordTint,
        bookmarkHighlight,
        bookmarkedSurah,
        bookmarkedAyah,
        playingSurah,
        playingAyah,
        effectiveWordIdx,
        selectedAyah
    ) {
        val hasAnyHighlight = isBlockPlaying ||
            (selectedAyah?.surahNumber == block.surahNumber) ||
            (bookmarkedSurah == block.surahNumber)

        if (!hasAnyHighlight) {
            baseBlockData.baseAnnotated
        } else {
            val builder = AnnotatedString.Builder(baseBlockData.baseAnnotated)
            block.ayahs.forEachIndexed { idx, ayah ->
                val isPlaying = playingSurah == ayah.surahNumber && playingAyah == ayah.numberInSurah
                val isSelected = selectedAyah?.surahNumber == ayah.surahNumber &&
                    selectedAyah.numberInSurah == ayah.numberInSurah
                val isBookmarked = bookmarkedSurah == ayah.surahNumber && bookmarkedAyah == ayah.numberInSurah

                val ayahBg = when {
                    isPlaying -> activeAyahTint
                    isSelected -> activeAyahTint.copy(alpha = 0.55f)
                    isBookmarked -> bookmarkHighlight.copy(alpha = 0.65f)
                    else -> Color.Transparent
                }
                if (ayahBg != Color.Transparent) {
                    val start = baseBlockData.ayahTextStartOffsets[idx]
                    val end = baseBlockData.ayahFullEndOffsets[idx]
                    builder.addStyle(SpanStyle(background = ayahBg), start, end)

                    if (isPlaying && effectiveWordIdx != null) {
                        val wordRange = TajweedEngine.findWordCharRange(ayah.text, effectiveWordIdx)
                        if (wordRange != null) {
                            val wStart = (start + wordRange.first).coerceIn(start, end)
                            val wEnd = (start + wordRange.last + 1).coerceIn(wStart, end)
                            if (wEnd > wStart) {
                                builder.addStyle(SpanStyle(background = activeWordTint), wStart, wEnd)
                            }
                        }
                    }
                }
            }
            builder.toAnnotatedString()
        }
    }

    var textLayoutResult by remember(block) { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = annotatedText,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = AmiriQuranFontFamily,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 2.02f).sp,
            textAlign = if (centerAlign) TextAlign.Center else TextAlign.Justify
        ),
        onTextLayout = { textLayoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(block, annotatedText, canTapToSeek, baseBlockData) {
                detectTapGestures(
                    // While playing with official timestamps: tapping a word/Ayah seeks directly to its startMs.
                    // When not playing: single tap cleanly toggles full-screen bars (never opens Ayah menu).
                    onTap = { offset ->
                        if (canTapToSeek) {
                            val layout = textLayoutResult
                            if (layout != null && annotatedText.isNotEmpty()) {
                                val rawOffset = layout.getOffsetForPosition(offset)
                                val safeOffset = rawOffset.coerceIn(0, (annotatedText.length - 1).coerceAtLeast(0))
                                val match = annotatedText
                                    .getStringAnnotations(tag = "AYAH_IDX", start = safeOffset, end = safeOffset)
                                    .firstOrNull()
                                val idx = match?.item?.toIntOrNull()
                                val ayah = idx?.let { block.ayahs.getOrNull(it) }
                                if (ayah != null && idx != null) {
                                    val ayahStart = baseBlockData.ayahTextStartOffsets.getOrNull(idx) ?: 0
                                    val localOffset = (safeOffset - ayahStart).coerceIn(0, (ayah.text.length - 1).coerceAtLeast(0))
                                    val wordIdx = TajweedEngine.findWordIndexAtCharOffset(ayah.text, localOffset)
                                    if (onTapWordOrAyah(ayah, wordIdx)) {
                                        return@detectTapGestures
                                    }
                                }
                            }
                        }
                        onTapToggleControls()
                    },
                    // Long press selects & highlights the Ayah and opens the All-in-One Ayah + Tafsir Sheet
                    onLongPress = { offset ->
                        val layout = textLayoutResult ?: return@detectTapGestures
                        val rawOffset = layout.getOffsetForPosition(offset)
                        val safeOffset = rawOffset.coerceIn(0, (annotatedText.length - 1).coerceAtLeast(0))
                        val match = annotatedText
                            .getStringAnnotations(tag = "AYAH_IDX", start = safeOffset, end = safeOffset)
                            .firstOrNull()
                        val idx = match?.item?.toIntOrNull()
                        val ayah = idx?.let { block.ayahs.getOrNull(it) } ?: block.ayahs.firstOrNull()
                        if (ayah != null) {
                            onLongPressAyah(ayah)
                        }
                    }
                )
            }
    )
}
