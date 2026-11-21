package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.example.data.model.OfflineHadithBookMeta
import com.example.data.repository.OfflineHadithRepository
import com.example.ui.components.ArabicEmptyStateCard
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.NightReadPaper
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.HadithUiState
import kotlinx.coroutines.delay

@Composable
fun HadithScreen(
    state: HadithUiState,
    darkMode: Boolean,
    favoriteUids: Set<String>,
    hadithFontSizeSp: Float,
    lastHadithResume: Pair<HadithBookCollection, Int>?,
    onSelectSubTab: (Int) -> Unit,
    onOpenBook: (HadithBookCollection) -> Unit,
    onCloseBook: () -> Unit,
    onSaveHadithScrollIndex: (HadithBookCollection, Int) -> Unit,
    onDownloadBook: (HadithBookCollection) -> Unit,
    onDeleteBook: (HadithBookCollection) -> Unit,
    onSearchChange: (String) -> Unit,
    onFavoritesSearchChange: (String) -> Unit,
    onToggleFontFamily: () -> Unit,
    onToggleExplanation: (String) -> Unit,
    onToggleFavorite: (LibraryHadith) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLoadMore: () -> Unit,
    onShowToast: (String) -> Unit
) {
    // If a specific Hadith book is open, show the dedicated Mushaf-aesthetic Book Reader
    val openedBook = state.openBook
    if (openedBook != null) {
        val resumeIndex = if (lastHadithResume?.first == openedBook) lastHadithResume.second else 0
        HadithBookReaderView(
            book = openedBook,
            state = state,
            darkMode = darkMode,
            favoriteUids = favoriteUids,
            hadithFontSizeSp = hadithFontSizeSp,
            initialScrollIndex = resumeIndex,
            onScrollIndexChanged = { onSaveHadithScrollIndex(openedBook, it) },
            onBack = onCloseBook,
            onDownloadBook = onDownloadBook,
            onSearchChange = onSearchChange,
            onToggleFontFamily = onToggleFontFamily,
            onFontSizeChange = onFontSizeChange,
            onToggleExplanation = onToggleExplanation,
            onToggleFavorite = onToggleFavorite,
            onLoadMore = onLoadMore,
            onShowToast = onShowToast
        )
        return
    }

    val islamic = LocalIslamicColors.current
    val view = LocalView.current
    var bookPendingDelete by remember { mutableStateOf<HadithBookCollection?>(null) }

    val downloadedCount = remember(state.bookStatuses) {
        state.bookStatuses.values.count { it.isDownloaded }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("hadith_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Mushaf-Style Offline Hadith Library Header Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
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
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftGold.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.55f)),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                    contentDescription = "الأحاديث النبوية أوفلاين",
                                    tint = Color(0xFFF6DC97),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الأحاديث النبوية أوفلاين",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${toArabicIndicDigits(downloadedCount)} من ${toArabicIndicDigits(HadithBookCollection.entries.size)} كتب محفوظة للعمل بدون نت",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF6DC97)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dedicated 2-Tab Switcher: كتب الحديث (8) | المفضلة (count)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HadithSubTabButton(
                title = "كتب الحديث (${toArabicIndicDigits(HadithBookCollection.entries.size)})",
                icon = Icons.Default.MenuBook,
                selected = state.activeSubTab == 0,
                testTag = "hadith_tab_books",
                onClick = { onSelectSubTab(0) },
                modifier = Modifier.weight(1f)
            )
            HadithSubTabButton(
                title = "المفضلة (${toArabicIndicDigits(state.savedFavoriteHadiths.size)})",
                icon = Icons.Default.Bookmark,
                selected = state.activeSubTab == 1,
                testTag = "hadith_tab_favorites",
                onClick = { onSelectSubTab(1) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.activeSubTab == 0) {
            // TAB 0: 8 Authentic Hadith Books with Offline Download Manager
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hadith_books_list")
            ) {
                if (lastHadithResume != null) {
                    val (resumeBook, resumeIdx) = lastHadithResume
                    item(key = "hadith_smart_resume") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = islamic.goldSoft,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("hadith_smart_resume_card")
                                .clickable {
                                    performKeyTapHaptic(view)
                                    onOpenBook(resumeBook)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = islamic.goldText,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "متابعة القراءة — ${resumeBook.arabicTitle}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "توقفت عند الحديث رقم ~${toArabicIndicDigits(resumeIdx + 1)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldGreen
                                ) {
                                    Text(
                                        text = "متابعة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                items(HadithBookCollection.entries, key = { it.id }) { book ->
                    val meta = state.bookStatuses[book.id]
                    val progress = state.downloadingProgress[book.id]
                    HadithBookOfflineCard(
                        book = book,
                        meta = meta,
                        downloadProgress = progress,
                        darkMode = darkMode,
                        onOpen = { onOpenBook(book) },
                        onDownload = { onDownloadBook(book) },
                        onRequestDelete = { bookPendingDelete = book }
                    )
                }
            }
        } else {
            // TAB 1: Dedicated Favorites Tab
            HadithFavoritesTabContent(
                favorites = state.savedFavoriteHadiths,
                searchQuery = state.favoritesSearchQuery,
                darkMode = darkMode,
                useCairoFont = state.useCairoFont,
                fontSizeSp = hadithFontSizeSp,
                expandedUids = state.expandedUids,
                onSearchChange = onFavoritesSearchChange,
                onToggleFontFamily = onToggleFontFamily,
                onFontSizeChange = onFontSizeChange,
                onToggleExplanation = onToggleExplanation,
                onToggleFavorite = onToggleFavorite,
                onShowToast = onShowToast
            )
        }
    }

    // Delete Confirmation Dialog to free storage safely
    bookPendingDelete?.let { book ->
        val sizeStr = state.bookStatuses[book.id]?.fileSizeBytes?.let {
            OfflineHadithRepository.formatFileSizeArabic(it)
        }.orEmpty()
        AlertDialog(
            onDismissRequest = { bookPendingDelete = null },
            title = {
                Text(
                    text = "حذف ${book.arabicTitle}؟",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = if (sizeStr.isNotEmpty()) {
                        "سيتم حذف النسخة المحفوظة محلياً ($sizeStr) لتوفير المساحة. يمكنك إعادة تحميلها في أي وقت."
                    } else {
                        "سيتم حذف النسخة المحفوظة محلياً لتوفير المساحة. يمكنك إعادة تحميلها في أي وقت."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBook(book)
                        bookPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف لتوفير المساحة")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingDelete = null }) {
                    Text("إلغاء", color = islamic.textMuted)
                }
            }
        )
    }
}

@Composable
private fun HadithSubTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val islamic = LocalIslamicColors.current
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (selected) EmeraldGreen else Color.Transparent,
        modifier = modifier
            .height(44.dp)
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFFF6DC97) else islamic.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HadithBookOfflineCard(
    book: HadithBookCollection,
    meta: OfflineHadithBookMeta?,
    downloadProgress: Float?,
    darkMode: Boolean,
    onOpen: () -> Unit,
    onDownload: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val isDownloaded = meta?.isDownloaded == true
    val totalCount = meta?.actualCount ?: book.expectedCount
    val fileSizeText = remember(meta?.fileSizeBytes) {
        OfflineHadithRepository.formatFileSizeArabic(meta?.fileSizeBytes ?: 0L)
    }
    val cardPaper = if (darkMode) NightReadPaper else MushafCreamPaper
    val borderColor = if (isDownloaded) {
        Color(0xFF2E7D32).copy(alpha = if (darkMode) 0.55f else 0.45f)
    } else {
        SoftGold.copy(alpha = if (darkMode) 0.35f else 0.55f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_book_card_${book.id}")
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardPaper),
        border = BorderStroke(1.1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    // Decorative Book Spine Emblem
                    Surface(
                        shape = RoundedCornerShape(13.dp),
                        color = if (isDownloaded) {
                            EmeraldGreen.copy(alpha = if (darkMode) 0.30f else 0.14f)
                        } else {
                            islamic.goldSoft
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isDownloaded) EmeraldGreen.copy(alpha = 0.45f) else SoftGold.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = book.arabicTitle,
                                tint = if (isDownloaded) {
                                    if (darkMode) Color(0xFF7CE2B9) else EmeraldGreen
                                } else {
                                    islamic.goldText
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.arabicTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = AmiriFontFamily,
                                fontSize = 20.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = islamic.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = islamic.goldSoft
                            ) {
                                Text(
                                    text = "${toArabicIndicDigits(totalCount)} حديث",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = islamic.goldText,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            if (isDownloaded && fileSizeText.isNotEmpty()) {
                                Text(
                                    text = fileSizeText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = islamic.textMuted
                                )
                            }
                        }
                    }
                }
            }

            // Real-time progress bar while downloading
            if (downloadProgress != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { downloadProgress.coerceIn(0f, 1f) },
                        color = EmeraldGreen,
                        trackColor = islamic.emeraldSoft,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = "جاري التحميل أوفلاين... ${(downloadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // Status Badge & Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isDownloaded) {
                        // Green "متاح بدون نت ✓" badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E7D32).copy(alpha = if (darkMode) 0.24f else 0.13f),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.48f)),
                            modifier = Modifier.testTag("hadith_book_offline_badge_${book.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "متاح بدون نت ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (darkMode) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldGreen,
                                modifier = Modifier
                                    .testTag("hadith_book_open_btn_${book.id}")
                                    .clickable(onClick = onOpen)
                            ) {
                                Text(
                                    text = "قراءة الكتاب",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            IconButton(
                                onClick = onRequestDelete,
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("hadith_book_delete_btn_${book.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "حذف الكتاب لتوفير المساحة",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "غير محمّل بعد",
                            style = MaterialTheme.typography.labelSmall,
                            color = islamic.textMuted
                        )

                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            shape = RoundedCornerShape(11.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("hadith_book_download_btn_${book.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "تحميل أوفلاين",
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تحميل أوفلاين",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HadithFavoritesTabContent(
    favorites: List<LibraryHadith>,
    searchQuery: String,
    darkMode: Boolean,
    useCairoFont: Boolean,
    fontSizeSp: Float,
    expandedUids: Set<String>,
    onSearchChange: (String) -> Unit,
    onToggleFontFamily: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onToggleExplanation: (String) -> Unit,
    onToggleFavorite: (LibraryHadith) -> Unit,
    onShowToast: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val context = LocalContext.current
    var localSearch by remember { mutableStateOf(searchQuery) }
    LaunchedEffect(localSearch) {
        delay(300L)
        if (localSearch != searchQuery) {
            onSearchChange(localSearch)
        }
    }

    val normalizedQuery = remember(searchQuery) {
        OfflineHadithRepository.normalizeArabicForSearch(searchQuery)
    }
    val filteredFavorites = remember(favorites, normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            favorites
        } else {
            val num = normalizedQuery.toIntOrNull()
            favorites.filter { h ->
                (num != null && h.number == num) ||
                    OfflineHadithRepository.normalizeArabicForSearch(
                        "${h.number} ${h.book.arabicTitle} ${h.title} ${h.narrator} ${h.text}"
                    ).contains(normalizedQuery)
            }
        }
    }

    if (favorites.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .testTag("hadith_favorites_empty"),
            contentAlignment = Alignment.Center
        ) {
            ArabicEmptyStateCard(
                title = "لا توجد أحاديث محفوظة في المفضلة بعد",
                subtitle = "اضغط على زر «حفظ» أسفل أي حديث داخل كتب السنة لإضافته هنا والرجوع إليه في أي وقت بدون إنترنت.",
                icon = Icons.Default.BookmarkBorder
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hadith_favorites_list")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "الأحاديث المحفوظة (${toArabicIndicDigits(filteredFavorites.size)})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            HadithTypographyToolbar(
                useCairoFont = useCairoFont,
                fontSizeSp = fontSizeSp,
                onToggleFontFamily = onToggleFontFamily,
                onFontSizeChange = onFontSizeChange
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = localSearch,
            onValueChange = { localSearch = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hadith_favorites_search_input"),
            placeholder = { Text("ابحث في الأحاديث المفضلة...", style = MaterialTheme.typography.bodySmall) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث في المفضلة", tint = islamic.textMuted)
            },
            trailingIcon = {
                if (localSearch.isNotEmpty()) {
                    IconButton(onClick = {
                        localSearch = ""
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

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredFavorites.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ArabicEmptyStateCard(
                    title = "لا توجد نتائج مطابقة في المفضلة",
                    subtitle = "جرّب البحث بكلمة أخرى أو امسح مربع البحث لعرض جميع المحفوظات.",
                    icon = Icons.Default.Search
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredFavorites, key = { it.uid }) { hadith ->
                    MushafHadithCard(
                        hadith = hadith,
                        darkMode = darkMode,
                        useCairoFont = useCairoFont,
                        fontSizeSp = fontSizeSp,
                        showBookNameBadge = true,
                        isFavorite = true,
                        isExpanded = expandedUids.contains(hadith.uid),
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
            }
        }
    }
}
