package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioState
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AyahItem
import com.example.data.model.AyahRepeatMode
import com.example.data.model.MushafPaperTheme
import com.example.data.model.QuranBookmark
import com.example.data.model.QuranViewMode
import com.example.data.model.ReciterOption
import com.example.data.model.SurahSummary
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.QuranUiState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun QuranScreen(
    state: QuranUiState,
    fontSizeSp: Float,
    paperTheme: MushafPaperTheme,
    bookmark: QuranBookmark?,
    audioState: AudioPlayerState,
    ayahAudioState: AyahAudioState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenSurah: (SurahSummary) -> Unit,
    onPlaySurahFromList: (SurahSummary) -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onRetryReader: () -> Unit,
    onCloseReader: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectViewMode: (QuranViewMode) -> Unit,
    onSelectPaperTheme: (MushafPaperTheme) -> Unit,
    onToggleFullScreenReading: () -> Unit,
    onToggleAllTafsir: () -> Unit,
    onToggleAyahTafsir: (Int) -> Unit,
    onSaveBookmark: (Int) -> Unit,
    onToggleAudio: () -> Unit,
    onSelectReciter: (ReciterOption) -> Unit,
    onSeekAudio: (Float) -> Unit,
    onDownloadMp3: () -> Unit,
    onTogglePlaySingleAyah: (AyahItem) -> Unit,
    onStopSingleAyah: () -> Unit,
    onSelectAyahRepeatMode: (AyahRepeatMode) -> Unit,
    onToggleAyahContinueReading: (Boolean) -> Unit,
    onShowToast: (String) -> Unit
) {
    if (state.activeSurah != null) {
        BackHandler {
            if (state.isFullScreenReading) {
                onToggleFullScreenReading()
            } else {
                onCloseReader()
            }
        }
        SurahReaderView(
            state = state,
            fontSizeSp = fontSizeSp,
            paperTheme = paperTheme,
            bookmark = bookmark,
            audioState = audioState,
            ayahAudioState = ayahAudioState,
            onClose = onCloseReader,
            onRetry = onRetryReader,
            onFontSizeChange = onFontSizeChange,
            onSelectViewMode = onSelectViewMode,
            onSelectPaperTheme = onSelectPaperTheme,
            onToggleFullScreenReading = onToggleFullScreenReading,
            onToggleAllTafsir = onToggleAllTafsir,
            onToggleAyahTafsir = onToggleAyahTafsir,
            onSaveBookmark = onSaveBookmark,
            onToggleAudio = onToggleAudio,
            onOpenReciterLibrary = onOpenReciterLibrary,
            onSeekAudio = onSeekAudio,
            onDownloadMp3 = onDownloadMp3,
            onTogglePlaySingleAyah = onTogglePlaySingleAyah,
            onStopSingleAyah = onStopSingleAyah,
            onSelectAyahRepeatMode = onSelectAyahRepeatMode,
            onToggleAyahContinueReading = onToggleAyahContinueReading,
            onShowToast = onShowToast
        )
    } else {
        SurahListView(
            state = state,
            audioState = audioState,
            onSearchChange = onSearchChange,
            onFilterChange = onFilterChange,
            onOpenSurah = onOpenSurah,
            onPlaySurahFromList = onPlaySurahFromList,
            onSelectReciter = onSelectReciter,
            onOpenReciterLibrary = onOpenReciterLibrary
        )
    }
}

@Composable
private fun SurahListView(
    state: QuranUiState,
    audioState: AudioPlayerState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onOpenSurah: (SurahSummary) -> Unit,
    onPlaySurahFromList: (SurahSummary) -> Unit,
    onSelectReciter: (ReciterOption) -> Unit,
    onOpenReciterLibrary: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val normalizedQuery = remember(state.searchQuery) {
        OfflineIslamicData.normalizeArabic(state.searchQuery)
    }
    val filteredSurahs = remember(state.surahs, normalizedQuery, state.revelationFilter) {
        state.surahs.filter { surah ->
            val matchRev = state.revelationFilter == "all" || surah.revelationType == state.revelationFilter
            val matchQuery = normalizedQuery.isEmpty() ||
                OfflineIslamicData.normalizeArabic(surah.name).contains(normalizedQuery) ||
                surah.englishName.contains(state.searchQuery, ignoreCase = true) ||
                surah.number.toString() == normalizedQuery
            matchRev && matchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("quran_list_view")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Featured Popular Reciters Quick-Select Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = islamic.gold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "قائمة أشهر القراء",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = islamic.goldSoft,
                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .testTag("open_all_reciters_button")
                    .clickable(onClick = onOpenReciterLibrary)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "جميع القراء",
                        tint = islamic.goldText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "جميع القراء",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = islamic.goldText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(OfflineIslamicData.reciters, key = { it.id }) { reciter ->
                val isSelected = audioState.selectedReciter.serverUrl == reciter.serverUrl ||
                    audioState.selectedReciter.id == reciter.id
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else islamic.border
                    ),
                    modifier = Modifier
                        .testTag("quick_reciter_${reciter.id}")
                        .clickable { onSelectReciter(reciter) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = reciter.arabicName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("surah_search_input"),
            placeholder = { Text("ابحث باسم السورة أو رقمها (مثال: الكهف، يس)...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث", tint = islamic.textMuted)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = islamic.border
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        val filterOptions = listOf(
            "all" to "جميع السور (${state.surahs.size.takeIf { it > 0 } ?: 114})",
            "Meccan" to "مكية",
            "Medinan" to "مدنية"
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filterOptions) { (key, label) ->
                FilterChip(
                    selected = state.revelationFilter == key,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            state.isLoadingList -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جاري تحميل فهرس السور...", color = islamic.textMuted)
                    }
                }
            }
            filteredSurahs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد سورة مطابقة لبحثك", color = islamic.textMuted)
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 18.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        val isPlayingThis = audioState.loadedSurahNumber == surah.number && audioState.isPlaying
                        val isBufferingThis = audioState.loadedSurahNumber == surah.number && audioState.isBuffering
                        SurahListItem(
                            surah = surah,
                            isPlaying = isPlayingThis,
                            isBuffering = isBufferingThis,
                            onClick = { onOpenSurah(surah) },
                            onPlayClick = { onPlaySurahFromList(surah) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahListItem(
    surah: SurahSummary,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("surah_item_${surah.number}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) islamic.emeraldSoft else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isPlaying) MaterialTheme.colorScheme.primary else islamic.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(islamic.emeraldSoft)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = surah.name,
                        fontFamily = AmiriFontFamily,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (surah.revelationType == "Meccan") "مكية" else "مدنية"} • ${surah.numberOfAyahs} آية",
                        style = MaterialTheme.typography.bodySmall,
                        color = islamic.textMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) islamic.gold else islamic.emeraldSoft)
                        .testTag("surah_quick_play_${surah.number}")
                ) {
                    when {
                        isBuffering -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        isPlaying -> Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "إيقاف مؤقت",
                            tint = Color(0xFF1A1405),
                            modifier = Modifier.size(20.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "استماع للسورة",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "قراءة السورة",
                    tint = islamic.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SurahReaderView(
    state: QuranUiState,
    fontSizeSp: Float,
    paperTheme: MushafPaperTheme,
    bookmark: QuranBookmark?,
    audioState: AudioPlayerState,
    ayahAudioState: AyahAudioState,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onSelectViewMode: (QuranViewMode) -> Unit,
    onSelectPaperTheme: (MushafPaperTheme) -> Unit,
    onToggleFullScreenReading: () -> Unit,
    onToggleAllTafsir: () -> Unit,
    onToggleAyahTafsir: (Int) -> Unit,
    onSaveBookmark: (Int) -> Unit,
    onToggleAudio: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onDownloadMp3: () -> Unit,
    onTogglePlaySingleAyah: (AyahItem) -> Unit,
    onStopSingleAyah: () -> Unit,
    onSelectAyahRepeatMode: (AyahRepeatMode) -> Unit,
    onToggleAyahContinueReading: (Boolean) -> Unit,
    onShowToast: (String) -> Unit
) {
    val surah = state.activeSurah ?: return
    val context = LocalContext.current
    val islamic = LocalIslamicColors.current
    val listState = rememberLazyListState()
    val mushafScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val palette = remember(paperTheme) { resolveMushafPalette(paperTheme) }
    var selectedAyahForSheet by remember(surah.number) { mutableStateOf<AyahItem?>(null) }
    var showAudioBarInMushaf by remember { mutableStateOf(false) }

    val activePlayingAyahNumber = if (ayahAudioState.surahNumber == surah.number &&
        (ayahAudioState.isPlaying || ayahAudioState.isBuffering)
    ) {
        ayahAudioState.ayahNumber
    } else null

    val currentJuz = remember(state.ayahs, activePlayingAyahNumber, selectedAyahForSheet) {
        val refAyahNum = activePlayingAyahNumber ?: selectedAyahForSheet?.numberInSurah
        val matched = refAyahNum?.let { n -> state.ayahs.find { it.numberInSurah == n } }
        matched?.juz ?: state.ayahs.firstOrNull()?.juz ?: 1
    }

    val bookmarkedAyahInThisSurah = if (bookmark?.surahNumber == surah.number) bookmark.ayahNumber else null

    // Auto-scroll in List mode when opened from bookmark or when playing Ayah advances
    LaunchedEffect(state.viewMode, state.ayahs, state.targetScrollAyah, activePlayingAyahNumber) {
        if (state.viewMode == QuranViewMode.LIST && state.ayahs.isNotEmpty()) {
            val targetNum = activePlayingAyahNumber ?: state.targetScrollAyah.takeIf { it > 1 }
            if (targetNum != null) {
                val idx = state.ayahs.indexOfFirst { it.numberInSurah == targetNum }
                if (idx >= 0) {
                    listState.animateScrollToItem(idx + 2)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paperBg)
            .then(if (state.isFullScreenReading) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier)
            .testTag("surah_reader_view")
    ) {
        // Slim Mushaf-Style Top Reading Bar (Surah Name + Juz + A-/A+ + Paper Theme + Full-Screen)
        Surface(
            color = palette.bannerBg,
            border = BorderStroke(1.dp, palette.dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("close_reader_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "العودة لقائمة السور",
                                tint = palette.textColor
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = surah.name,
                                fontFamily = AmiriQuranFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "الجزء ${toArabicIndicDigits(currentJuz)} • ${if (surah.revelationType == "Meccan") "مكية" else "مدنية"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.medallionColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Quick controls: A- / A+ / Full-Screen
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.paperBg,
                            border = BorderStroke(1.dp, palette.dividerColor),
                            modifier = Modifier
                                .testTag("mushaf_font_decrease")
                                .clickable { onFontSizeChange((fontSizeSp - 2f).coerceAtLeast(18f)) }
                        ) {
                            Text(
                                text = "A-",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.paperBg,
                            border = BorderStroke(1.dp, palette.dividerColor),
                            modifier = Modifier
                                .testTag("mushaf_font_increase")
                                .clickable { onFontSizeChange((fontSizeSp + 2f).coerceAtMost(40f)) }
                        ) {
                            Text(
                                text = "A+",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleFullScreenReading,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("mushaf_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (state.isFullScreenReading) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = if (state.isFullScreenReading) "إنهاء ملء الشاشة" else "وضع القراءة الكاملة",
                                tint = palette.medallionColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // View Mode Toggle + 3 Paper Themes Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Mode Toggle: Mushaf (default) vs Verse-by-Verse List
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.paperBg)
                            .border(1.dp, palette.dividerColor, RoundedCornerShape(10.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        QuranViewMode.entries.forEach { mode ->
                            val selected = state.viewMode == mode
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) EmeraldGreen else Color.Transparent)
                                    .testTag("quran_view_mode_${mode.name}")
                                    .clickable { onSelectViewMode(mode) }
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (mode == QuranViewMode.MUSHAF) Icons.Default.AutoStories else Icons.Default.ViewAgenda,
                                    contentDescription = null,
                                    tint = if (selected) Color.White else palette.subtleText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (mode == QuranViewMode.MUSHAF) "المصحف" else "الآيات والتفسير",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else palette.subtleText
                                )
                            }
                        }
                    }

                    // 3 Paper Theme Swatches (Cream / Pure White / Eye-Comfort Night)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MushafPaperTheme.entries.forEach { theme ->
                            val sel = paperTheme == theme
                            val swatchBg = when (theme) {
                                MushafPaperTheme.CREAM -> Color(0xFFFAF6E8)
                                MushafPaperTheme.PURE_WHITE -> Color(0xFFFFFFFF)
                                MushafPaperTheme.NIGHT -> Color(0xFF141A17)
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatchBg)
                                    .border(
                                        width = if (sel) 2.5.dp else 1.dp,
                                        color = if (sel) SoftGold else palette.dividerColor,
                                        shape = CircleShape
                                    )
                                    .testTag("mushaf_paper_${theme.name}")
                                    .clickable { onSelectPaperTheme(theme) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = theme.arabicLabel,
                                        tint = if (theme == MushafPaperTheme.NIGHT) SoftGold else EmeraldGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Ayah-by-Ayah Recitation Banner (when playing a specific verse)
        if (activePlayingAyahNumber != null) {
            Surface(
                color = DeepGreen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (ayahAudioState.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SoftGold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = SoftGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تلاوة الآية ${toArabicIndicDigits(activePlayingAyahNumber)} • ${ayahAudioState.activeReciterName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SoftGold,
                        modifier = Modifier
                            .testTag("stop_ayah_audio_banner_btn")
                            .clickable(onClick = onStopSingleAyah)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "إيقاف تلاوة الآية",
                                tint = Color(0xFF1A1405),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "إيقاف",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1A1405)
                            )
                        }
                    }
                }
            }
        }

        // Main Reader Area
        when {
            state.isLoadingReader -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = EmeraldGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("جاري تحميل الآيات والتفسير الميسر...", color = palette.subtleText)
                    }
                }
            }
            state.readerError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = state.readerError,
                                textAlign = TextAlign.Center,
                                color = islamic.textMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = onRetry) {
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
            }
            state.viewMode == QuranViewMode.MUSHAF -> {
                // AUTHENTIC DISTRACTION-FREE MUSHAF VIEW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(mushafScrollState)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    // Optional compact full-Surah audio bar toggle
                    if (!state.isFullScreenReading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اضغط على أي آية للتفسير والاستماع والحفظ",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.subtleText
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = palette.bannerBg,
                                border = BorderStroke(1.dp, palette.outerBorder.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .testTag("toggle_surah_audio_in_mushaf")
                                    .clickable { showAudioBarInMushaf = !showAudioBarInMushaf }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        tint = palette.medallionColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showAudioBarInMushaf) "إخفاء المشغل" else "مشغل السورة",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textColor
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = showAudioBarInMushaf) {
                            Box(modifier = Modifier.padding(bottom = 10.dp)) {
                                AudioRecitationBar(
                                    audioState = audioState,
                                    onTogglePlay = onToggleAudio,
                                    onOpenReciterLibrary = onOpenReciterLibrary,
                                    onSeek = onSeekAudio,
                                    onDownloadMp3 = onDownloadMp3
                                )
                            }
                        }
                    }

                    MushafOrnamentalFrame(palette = palette) {
                        MushafSurahTitleBanner(
                            surah = surah,
                            palette = palette
                        )

                        MushafContinuousTextBlock(
                            surahNumber = surah.number,
                            ayahs = state.ayahs,
                            fontSizeSp = fontSizeSp,
                            palette = palette,
                            bookmarkedAyahNumber = bookmarkedAyahInThisSurah,
                            selectedAyahNumber = selectedAyahForSheet?.numberInSurah,
                            playingAyahNumber = activePlayingAyahNumber,
                            onAyahTapped = { tapped -> selectedAyahForSheet = tapped },
                            onTargetAyahVerticalOffset = { yOffset ->
                                coroutineScope.launch {
                                    val targetY = (yOffset - 120).coerceAtLeast(0)
                                    mushafScrollState.animateScrollTo(targetY)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "صدق الله العظيم",
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 19.sp,
                            color = palette.medallionColor,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                }
            }
            else -> {
                // VERSE-BY-VERSE LIST & TAFSIR VIEW
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "list_audio_and_tafsir") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AudioRecitationBar(
                                audioState = audioState,
                                onTogglePlay = onToggleAudio,
                                onOpenReciterLibrary = onOpenReciterLibrary,
                                onSeek = onSeekAudio,
                                onDownloadMp3 = onDownloadMp3
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MiniActionPill(
                                    icon = Icons.AutoMirrored.Filled.MenuBook,
                                    label = if (state.showAllTafsir) "إخفاء كل التفسير" else "إظهار كل التفسير",
                                    highlighted = state.showAllTafsir,
                                    onClick = onToggleAllTafsir
                                )
                                Text(
                                    text = "${surah.numberOfAyahs} آية",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.subtleText
                                )
                            }
                        }
                    }

                    item(key = "list_surah_banner") {
                        MushafSurahTitleBanner(surah = surah, palette = palette)
                    }

                    items(state.ayahs, key = { it.numberInSurah }) { ayah ->
                        val isBookmarked = bookmarkedAyahInThisSurah == ayah.numberInSurah
                        val isPlayingThis = activePlayingAyahNumber == ayah.numberInSurah
                        val showTafsir = state.showAllTafsir || state.expandedTafsirAyahs.contains(ayah.numberInSurah)

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPlayingThis) palette.activeHighlightBg else palette.paperBg,
                            border = BorderStroke(
                                width = if (isBookmarked || isPlayingThis) 1.5.dp else 1.dp,
                                color = if (isBookmarked || isPlayingThis) palette.outerBorder else palette.dividerColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAyahForSheet = ayah }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${ayah.text}  ﴿${toArabicIndicDigits(ayah.numberInSurah)}﴾",
                                    fontFamily = AmiriQuranFontFamily,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp * 2.15f).sp,
                                    color = palette.textColor,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MiniActionPill(
                                        icon = if (isPlayingThis && ayahAudioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        label = if (isPlayingThis && ayahAudioState.isPlaying) "إيقاف" else "استماع",
                                        highlighted = isPlayingThis,
                                        onClick = { onTogglePlaySingleAyah(ayah) }
                                    )
                                    MiniActionPill(
                                        icon = Icons.AutoMirrored.Filled.MenuBook,
                                        label = "التفسير",
                                        highlighted = showTafsir,
                                        onClick = { onToggleAyahTafsir(ayah.numberInSurah) }
                                    )
                                    MiniActionPill(
                                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        label = if (isBookmarked) "محفوظة" else "علامة",
                                        highlighted = isBookmarked,
                                        onClick = { onSaveBookmark(ayah.numberInSurah) }
                                    )
                                    MiniActionPill(
                                        icon = Icons.Default.ContentCopy,
                                        label = "نسخ",
                                        onClick = {
                                            copyTextToClipboard(context, "${ayah.text} (${surah.name} : ${ayah.numberInSurah})")
                                            onShowToast("تم نسخ الآية")
                                        }
                                    )
                                }

                                AnimatedVisibility(visible = showTafsir && ayah.tafsir.isNotBlank()) {
                                    Column {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = palette.dividerColor)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(palette.tafsirBg)
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                text = "التفسير الميسر:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.medallionColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = ayah.tafsir,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = palette.tafsirText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sleek Bottom Sheet when user taps any verse
    val sheetAyah = selectedAyahForSheet
    if (sheetAyah != null) {
        AyahActionBottomSheet(
            surah = surah,
            ayah = sheetAyah,
            isBookmarked = bookmarkedAyahInThisSurah == sheetAyah.numberInSurah,
            ayahAudioState = ayahAudioState,
            onDismiss = { selectedAyahForSheet = null },
            onTogglePlayThisAyah = { onTogglePlaySingleAyah(sheetAyah) },
            onSelectRepeatMode = onSelectAyahRepeatMode,
            onToggleContinueReading = onToggleAyahContinueReading,
            onPlayFullSurah = {
                selectedAyahForSheet = null
                onToggleAudio()
            },
            onSaveBookmark = { onSaveBookmark(sheetAyah.numberInSurah) },
            onCopy = {
                copyTextToClipboard(context, "${sheetAyah.text} ﴿${toArabicIndicDigits(sheetAyah.numberInSurah)}﴾ — ${surah.name}")
                onShowToast("تم نسخ الآية بنجاح")
            },
            onShare = {
                sharePlainText(context, "${sheetAyah.text} ﴿${toArabicIndicDigits(sheetAyah.numberInSurah)}﴾\n(${surah.name})\n— من تطبيق رفيق المسلم")
            }
        )
    }
}

@Composable
private fun AudioRecitationBar(
    audioState: AudioPlayerState,
    onTogglePlay: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onSeek: (Float) -> Unit,
    onDownloadMp3: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quran_audio_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFF4D88B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = audioState.selectedReciter.arabicName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = audioState.selectedReciter.riwayahName,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF4D88B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .testTag("reader_switch_reciter_button")
                        .clickable(onClick = onOpenReciterLibrary)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "تغيير القارئ",
                            tint = Color(0xFFF4D88B),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "تغيير القارئ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SoftGold)
                        .testTag("audio_play_button")
                ) {
                    if (audioState.isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color(0xFF1A1405)
                        )
                    } else {
                        Icon(
                            imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (audioState.isPlaying) "إيقاف مؤقت" else "تشغيل التلاوة",
                            tint = Color(0xFF1A1405)
                        )
                    }
                }

                val progressFraction = if (audioState.durationMs > 0) {
                    (audioState.currentPositionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = progressFraction,
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFF4D88B),
                            activeTrackColor = Color(0xFFF4D88B),
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        )
                    )
                }

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "${formatDurationMs(audioState.currentPositionMs)} / ${formatDurationMs(audioState.durationMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                IconButton(
                    onClick = onDownloadMp3,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .testTag("reader_download_mp3_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "تحميل السورة MP3",
                        tint = Color(0xFFF4D88B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (audioState.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = audioState.errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFD599)
                )
            }
        }
    }
}

private fun formatDurationMs(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
