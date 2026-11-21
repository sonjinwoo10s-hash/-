package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HadithBookCollection
import com.example.data.model.LibraryHadith
import com.example.data.repository.OfflineHadithRepository
import com.example.ui.components.ArabicEmptyStateCard
import com.example.ui.components.ArabicSkeletonList
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.CairoFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.NightReadPaper
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.HadithUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun HadithBookReaderView(
    book: HadithBookCollection,
    state: HadithUiState,
    darkMode: Boolean,
    favoriteUids: Set<String>,
    hadithFontSizeSp: Float,
    initialScrollIndex: Int = 0,
    onScrollIndexChanged: (Int) -> Unit = {},
    onBack: () -> Unit,
    onDownloadBook: (HadithBookCollection) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleFontFamily: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleExplanation: (String) -> Unit,
    onToggleFavorite: (LibraryHadith) -> Unit,
    onLoadMore: () -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler { onBack() }

    val islamic = LocalIslamicColors.current
    val context = LocalContext.current
    val meta = state.bookStatuses[book.id]
    val isDownloaded = meta?.isDownloaded == true
    val downloadProgress = state.downloadingProgress[book.id]
    val allHadiths = state.loadedCollections[book].orEmpty()
    var localSearchQuery by remember(book.id) { mutableStateOf(state.searchQuery) }

    // 300ms debounce so searching huge books like Sahih Bukhari never stutters typing
    LaunchedEffect(localSearchQuery) {
        delay(300L)
        if (localSearchQuery != state.searchQuery) {
            onSearchChange(localSearchQuery)
        }
    }

    val normalizedQuery = remember(state.searchQuery) {
        OfflineHadithRepository.normalizeArabicForSearch(state.searchQuery)
    }

    val filteredHadiths = remember(allHadiths, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            allHadiths
        } else {
            val numberMatch = normalizedQuery.toIntOrNull()
            allHadiths.filter { hadith ->
                if (numberMatch != null && hadith.number == numberMatch) {
                    true
                } else {
                    val searchTarget = hadith.normalizedSearchText.ifEmpty {
                        OfflineHadithRepository.normalizeArabicForSearch(
                            "${hadith.number} ${hadith.title} ${hadith.narrator} ${hadith.text}"
                        )
                    }
                    searchTarget.contains(normalizedQuery)
                }
            }
        }
    }

    val pagedHadiths = remember(filteredHadiths, state.visibleCount) {
        filteredHadiths.take(state.visibleCount)
    }
    val hasMore = filteredHadiths.size > pagedHadiths.size
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialScrollIndex.coerceAtLeast(0))
    var didRestoreScroll by remember(book.id) { mutableStateOf(initialScrollIndex <= 0) }

    // Smart Resume: restore saved Hadith reading position cleanly after book loads
    LaunchedEffect(allHadiths.isNotEmpty(), pagedHadiths.size, initialScrollIndex, state.searchQuery) {
        if (!didRestoreScroll && initialScrollIndex > 0 && state.searchQuery.isEmpty() && allHadiths.isNotEmpty()) {
            if (pagedHadiths.size <= initialScrollIndex && hasMore) {
                onLoadMore()
            } else {
                val safeTarget = initialScrollIndex.coerceIn(0, (pagedHadiths.size - 1).coerceAtLeast(0))
                listState.scrollToItem(safeTarget)
                didRestoreScroll = true
            }
        }
    }

    // Persist reading position smoothly without blocking scrolling
    LaunchedEffect(listState, book.id, state.searchQuery) {
        if (state.searchQuery.isNotEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { idx ->
                if (didRestoreScroll) {
                    delay(450L)
                    onScrollIndexChanged(idx)
                }
            }
    }

    // Automatically load next 25 Hadiths when scrolling near the end
    LaunchedEffect(listState, hasMore, pagedHadiths.size) {
        if (!hasMore) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastIdx ->
                if (lastIdx >= pagedHadiths.size - 5) {
                    onLoadMore()
                }
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .testTag("hadith_book_reader_${book.id}")
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Top Mushaf-style Reader Bar
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (darkMode) NightReadPaper else MushafCreamPaper,
            border = BorderStroke(1.dp, SoftGold.copy(alpha = if (darkMode) 0.4f else 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
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
                            onClick = onBack,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("hadith_reader_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "العودة لقائمة الكتب",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.arabicTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val displayCount = allHadiths.size.takeIf { it > 0 }
                                    ?: meta?.actualCount
                                    ?: book.expectedCount
                                Text(
                                    text = "${toArabicIndicDigits(displayCount)} حديث",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = islamic.textMuted
                                )
                                if (isDownloaded) {
                                    Text(
                                        text = "• متاح بدون نت ✓",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    // Typography controls: Amiri / Cairo toggle + A- / A+
                    HadithTypographyToolbar(
                        useCairoFont = state.useCairoFont,
                        fontSizeSp = hadithFontSizeSp,
                        onToggleFontFamily = onToggleFontFamily,
                        onFontSizeChange = onFontSizeChange
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when {
            !isDownloaded && allHadiths.isEmpty() -> {
                // Prompt user to download book for offline reading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (darkMode) NightReadPaper else MushafCreamPaper
                        ),
                        border = BorderStroke(1.2.dp, SoftGold.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp)
                            .testTag("hadith_download_prompt_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = islamic.emeraldSoft,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                modifier = Modifier.size(58.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = book.arabicTitle,
                                style = MaterialTheme.typography.headlineSmall.copy(fontFamily = AmiriFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${book.author} • ${toArabicIndicDigits(book.expectedCount)} حديث",
                                style = MaterialTheme.typography.labelMedium,
                                color = islamic.goldText,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "قم بتحميل هذا الكتاب مرة واحدة لحفظه على جهازك والقراءة والبحث فيه بسرعة فائقة بدون إنترنت.",
                                style = MaterialTheme.typography.bodySmall,
                                color = islamic.textMuted,
                                textAlign = TextAlign.Center
                            )

                            if (!state.bookError.isNullOrBlank()) {
                                Text(
                                    text = state.bookError,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (downloadProgress != null) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress.coerceIn(0f, 1f) },
                                        color = EmeraldGreen,
                                        trackColor = islamic.emeraldSoft,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                    Text(
                                        text = "جاري التحميل والحفظ أوفلاين... ${(downloadProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onDownloadBook(book) },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("prompt_download_book_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تحميل أوفلاين الآن",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.isLoadingBook && allHadiths.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "جاري فتح ${book.arabicTitle} أوفلاين...",
                        style = MaterialTheme.typography.labelMedium,
                        color = islamic.textMuted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ArabicSkeletonList(itemCount = 4)
                }
            }

            else -> {
                // Fast Offline Search Bar (keyword or Hadith number, 300ms debounced)
                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = { localSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hadith_offline_search_input"),
                    placeholder = {
                        Text(
                            text = "ابحث بكلمة أو رقم الحديث (بدون تشكيل)...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث في الكتاب",
                            tint = islamic.textMuted
                        )
                    },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                localSearchQuery = ""
                                onSearchChange("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = islamic.border
                    )
                )

                if (normalizedQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "نتائج البحث الفوري أوفلاين: ${toArabicIndicDigits(filteredHadiths.size)} حديث",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredHadiths.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ArabicEmptyStateCard(
                            title = "لم يتم العثور على أحاديث مطابقة لبحثك",
                            subtitle = "جرّب البحث بكلمة أخرى أو برقم الحديث بدون تشكيل.",
                            icon = Icons.Default.Search
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(pagedHadiths, key = { it.uid }) { hadith ->
                            val isFav = favoriteUids.contains(hadith.uid)
                            MushafHadithCard(
                                hadith = hadith,
                                darkMode = darkMode,
                                useCairoFont = state.useCairoFont,
                                fontSizeSp = hadithFontSizeSp,
                                showBookNameBadge = false,
                                isFavorite = isFav,
                                isExpanded = state.expandedUids.contains(hadith.uid),
                                onToggleExplain = { onToggleExplanation(hadith.uid) },
                                onToggleFavorite = { onToggleFavorite(hadith) },
                                onCopy = {
                                    copyTextToClipboard(context, formatHadithShareText(hadith))
                                    onShowToast("تم نسخ الحديث بنجاح")
                                },
                                onShare = {
                                    sharePlainText(context, formatHadithShareText(hadith))
                                }
                            )
                        }

                        if (hasMore) {
                            item(key = "load_more_offline_hadiths") {
                                OutlinedButton(
                                    onClick = onLoadMore,
                                    border = BorderStroke(1.dp, SoftGold),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("load_more_hadiths_button")
                                ) {
                                    Text(
                                        text = "عرض المزيد (${toArabicIndicDigits(pagedHadiths.size)} / ${toArabicIndicDigits(filteredHadiths.size)})",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
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

@Composable
fun HadithTypographyToolbar(
    useCairoFont: Boolean,
    fontSizeSp: Float,
    onToggleFontFamily: () -> Unit,
    onFontSizeChange: (Float) -> Unit
) {
    val islamic = LocalIslamicColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = islamic.goldSoft,
            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.55f)),
            modifier = Modifier
                .testTag("hadith_font_family_toggle")
                .clickable(onClick = onToggleFontFamily)
        ) {
            Text(
                text = if (useCairoFont) "Cairo" else "Amiri",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = islamic.goldText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, islamic.border),
            modifier = Modifier
                .testTag("hadith_font_dec")
                .clickable { onFontSizeChange((fontSizeSp - 2f).coerceAtLeast(16f)) }
        ) {
            Text(
                text = "A-",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, islamic.border),
            modifier = Modifier
                .testTag("hadith_font_inc")
                .clickable { onFontSizeChange((fontSizeSp + 2f).coerceAtMost(30f)) }
        ) {
            Text(
                text = "A+",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
fun MushafHadithCard(
    hadith: LibraryHadith,
    darkMode: Boolean,
    useCairoFont: Boolean,
    fontSizeSp: Float,
    showBookNameBadge: Boolean,
    isFavorite: Boolean,
    isExpanded: Boolean,
    onToggleExplain: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val view = LocalView.current
    val paperBg = if (darkMode) NightReadPaper else MushafCreamPaper
    val inkColor = if (darkMode) Color(0xFFEDE8DC) else Color(0xFF1F1B14)
    val fontFamily = if (useCairoFont) CairoFontFamily else AmiriFontFamily
    val lineHeightSp = if (useCairoFont) fontSizeSp * 1.75f else fontSizeSp * 1.92f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_card_${hadith.uid}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = paperBg),
        border = BorderStroke(
            width = if (isFavorite) 1.5.dp else 1.dp,
            color = if (isFavorite) SoftGold else SoftGold.copy(alpha = if (darkMode) 0.35f else 0.55f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Header: Hadith Number Medallion + Book / Section Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldGreen.copy(alpha = if (darkMode) 0.28f else 0.12f),
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "حديث رقم ${toArabicIndicDigits(hadith.number)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (darkMode) Color(0xFF7CE2B9) else EmeraldGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (showBookNameBadge) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = islamic.goldSoft,
                        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = hadith.book.arabicTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = islamic.goldText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                } else if (hadith.title.isNotBlank()) {
                    Text(
                        text = hadith.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = islamic.goldText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            if (hadith.narrator.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hadith.narrator,
                    style = MaterialTheme.typography.labelMedium,
                    color = islamic.goldText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Hadith Arabic Text (Amiri or Cairo, adjustable font size)
            Text(
                text = hadith.text,
                fontFamily = fontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = lineHeightSp.sp,
                color = inkColor,
                textAlign = TextAlign.Justify,
                modifier = Modifier.fillMaxWidth()
            )

            // Optional collapsible explanation when available
            if (hadith.explanation.isNotBlank()) {
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, islamic.border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "الشرح والفوائد:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = hadith.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = islamic.textMuted,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = SoftGold.copy(alpha = 0.3f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row: Copy, Share, Bookmark/Favorite (+ optional Explanation toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniActionPill(
                        icon = Icons.Default.ContentCopy,
                        label = "نسخ",
                        onClick = onCopy
                    )
                    MiniActionPill(
                        icon = Icons.Default.Share,
                        label = "مشاركة",
                        onClick = onShare
                    )
                    if (hadith.explanation.isNotBlank()) {
                        MiniActionPill(
                            icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            label = if (isExpanded) "إخفاء" else "الشرح",
                            highlighted = isExpanded,
                            onClick = onToggleExplain
                        )
                    }
                }

                MiniActionPill(
                    icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (isFavorite) "في المفضلة" else "حفظ",
                    highlighted = isFavorite,
                    onClick = {
                        performKeyTapHaptic(view)
                        onToggleFavorite()
                    }
                )
            }
        }
    }
}

fun formatHadithShareText(hadith: LibraryHadith): String {
    return buildString {
        if (hadith.narrator.isNotBlank()) appendLine("${hadith.narrator}:")
        appendLine(hadith.text)
        appendLine()
        append("[${hadith.book.arabicTitle} - حديث رقم ${hadith.number}]")
    }
}
