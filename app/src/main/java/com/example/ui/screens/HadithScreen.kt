package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineIslamicData
import com.example.data.model.HadithBookCollection
import com.example.data.model.HadithChapters
import com.example.data.model.LibraryHadith
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.HadithUiState

@Composable
fun HadithScreen(
    state: HadithUiState,
    favoriteUids: Set<String>,
    hadithFontSizeSp: Float,
    onSelectBook: (HadithBookCollection) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSearchAllBooks: (Boolean) -> Unit,
    onSelectChapter: (String) -> Unit,
    onToggleExplanation: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLoadMore: () -> Unit,
    onRetryCollection: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val context = LocalContext.current

    val normalizedQuery = remember(state.searchQuery) {
        OfflineIslamicData.normalizeArabic(state.searchQuery)
    }

    val sourceList: List<LibraryHadith> = remember(
        state.selectedBook,
        state.searchAllBooks,
        state.loadedCollections
    ) {
        if (state.searchAllBooks && normalizedQuery.isNotEmpty()) {
            HadithBookCollection.entries.flatMap { book ->
                state.loadedCollections[book].orEmpty()
            }
        } else {
            state.loadedCollections[state.selectedBook].orEmpty()
        }
    }

    val filteredHadiths = remember(
        sourceList,
        normalizedQuery,
        state.selectedChapter,
        favoriteUids
    ) {
        sourceList.filter { hadith ->
            val matchChapter = when (state.selectedChapter) {
                HadithChapters.ALL -> true
                HadithChapters.FAVORITES -> favoriteUids.contains(hadith.uid)
                else -> hadith.chapter == state.selectedChapter
            }
            if (!matchChapter) return@filter false

            if (normalizedQuery.isEmpty()) return@filter true
            val matchNum = hadith.number.toString() == normalizedQuery
            matchNum ||
                OfflineIslamicData.normalizeArabic(hadith.title).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(hadith.text).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(hadith.narrator).contains(normalizedQuery) ||
                OfflineIslamicData.normalizeArabic(hadith.source).contains(normalizedQuery)
        }
    }

    val pagedHadiths = remember(filteredHadiths, state.visibleCount) {
        filteredHadiths.take(state.visibleCount)
    }
    val hasMore = filteredHadiths.size > pagedHadiths.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("hadith_screen")
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header + Font Size Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeading(title = "مكتبة الحديث الشريف")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, islamic.border),
                    modifier = Modifier
                        .testTag("hadith_font_dec")
                        .clickable { onFontSizeChange((hadithFontSizeSp - 2f).coerceAtLeast(16f)) }
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
                        .clickable { onFontSizeChange((hadithFontSizeSp + 2f).coerceAtMost(30f)) }
                ) {
                    Text(
                        text = "A+",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = islamic.goldSoft
                ) {
                    Text(
                        text = "${filteredHadiths.size}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = islamic.goldText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4 Main Selectable Book Collections
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HadithBookCollection.entries, key = { it.id }) { book ->
                val selected = state.selectedBook == book
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.2.dp,
                        if (selected) MaterialTheme.colorScheme.primary else islamic.border
                    ),
                    modifier = Modifier
                        .testTag("hadith_book_${book.id}")
                        .clickable { onSelectBook(book) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.arabicTitle,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search bar + All-books toggle
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hadith_search_input"),
            placeholder = { Text("ابحث بالكلمة أو الراوي أو رقم الحديث...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "بحث في الأحاديث", tint = islamic.textMuted)
            },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث")
                    }
                }
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

        if (state.searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !state.searchAllBooks,
                    onClick = { onToggleSearchAllBooks(false) },
                    label = { Text("في ${state.selectedBook.shortTitle} فقط", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = state.searchAllBooks,
                    onClick = { onToggleSearchAllBooks(true) },
                    label = { Text("بحث في جميع الكتب الأربعة", style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = islamic.gold,
                        selectedLabelColor = Color(0xFF1A1405)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Categorized Chapters (الأبواب)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(HadithChapters.allChapterTabs) { chapter ->
                val isFavTab = chapter == HadithChapters.FAVORITES
                FilterChip(
                    selected = state.selectedChapter == chapter,
                    onClick = { onSelectChapter(chapter) },
                    label = {
                        Text(
                            text = if (isFavTab) "$chapter (${favoriteUids.size})" else chapter,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (isFavTab) SoftGold else MaterialTheme.colorScheme.primary,
                        selectedLabelColor = if (isFavTab) Color(0xFF1A1405) else Color.White
                    )
                )
            }
        }

        // Loading / Offline Status Banner
        if (state.isLoadingBook) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = "جاري تحميل أحاديث ${state.selectedBook.arabicTitle} الكاملة...",
                    style = MaterialTheme.typography.labelSmall,
                    color = islamic.textMuted
                )
            }
        } else if (state.bookLoadNotice != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = state.bookLoadNotice,
                    style = MaterialTheme.typography.labelSmall,
                    color = islamic.goldText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = onRetryCollection,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة التحميل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (filteredHadiths.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.selectedChapter == HadithChapters.FAVORITES) {
                        "لا توجد أحاديث محفوظة في المفضلة بعد"
                    } else {
                        "لم يتم العثور على أحاديث مطابقة للبحث"
                    },
                    color = islamic.textMuted
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(pagedHadiths, key = { it.uid }) { hadith ->
                    val isFav = favoriteUids.contains(hadith.uid)
                    LibraryHadithCard(
                        hadith = hadith,
                        fontSizeSp = hadithFontSizeSp,
                        isFavorite = isFav,
                        isExpanded = state.expandedUids.contains(hadith.uid),
                        onToggleExplain = { onToggleExplanation(hadith.uid) },
                        onToggleFavorite = { onToggleFavorite(hadith.uid) },
                        onCopy = {
                            val txt = buildString {
                                if (hadith.title.isNotBlank()) appendLine(hadith.title)
                                if (hadith.narrator.isNotBlank()) appendLine("${hadith.narrator}:")
                                appendLine(hadith.text)
                                appendLine("[${hadith.source}]")
                                append("— من تطبيق رفيق المسلم")
                            }
                            copyTextToClipboard(context, txt)
                            onShowToast("تم نسخ الحديث بنجاح")
                        },
                        onShare = {
                            val txt = buildString {
                                if (hadith.title.isNotBlank()) appendLine(hadith.title)
                                if (hadith.narrator.isNotBlank()) appendLine("${hadith.narrator}:")
                                appendLine(hadith.text)
                                appendLine("[${hadith.source}]")
                                append("— من تطبيق رفيق المسلم")
                            }
                            sharePlainText(context, txt)
                        }
                    )
                }

                if (hasMore) {
                    item(key = "load_more_hadiths") {
                        OutlinedButton(
                            onClick = onLoadMore,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("load_more_hadiths_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "عرض المزيد من الأحاديث (${pagedHadiths.size} / ${filteredHadiths.size})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHadithCard(
    hadith: LibraryHadith,
    fontSizeSp: Float,
    isFavorite: Boolean,
    isExpanded: Boolean,
    onToggleExplain: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hadith_card_${hadith.uid}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isFavorite) 1.5.dp else 1.dp,
            color = if (isFavorite) islamic.gold else islamic.border
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hadith.title.isNotBlank()) {
                        "حديث #${hadith.number} — ${hadith.title}"
                    } else {
                        "${hadith.book.arabicTitle} — حديث رقم ${hadith.number}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = islamic.emeraldSoft
                ) {
                    Text(
                        text = hadith.chapter,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            if (hadith.narrator.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = hadith.narrator,
                    style = MaterialTheme.typography.labelMedium,
                    color = islamic.goldText,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = hadith.text,
                fontFamily = AmiriFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.85f).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (hadith.explanation.isNotBlank()) {
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(islamic.cardAlt)
                                .padding(12.dp)
                        ) {
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
                                color = islamic.textMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = islamic.goldSoft,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = hadith.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.goldText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (hadith.explanation.isNotBlank()) {
                        MiniActionPill(
                            icon = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            label = if (isExpanded) "إخفاء" else "الشرح",
                            highlighted = isExpanded,
                            onClick = onToggleExplain
                        )
                    }
                    MiniActionPill(
                        icon = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        label = if (isFavorite) "محفوظ" else "حفظ",
                        highlighted = isFavorite,
                        onClick = onToggleFavorite
                    )
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
                }
            }
        }
    }
}
