package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AyahAudioState
import com.example.data.model.AyahRepeatMode
import com.example.data.repository.MushafPageAyah
import com.example.data.repository.OfflineQuranRepository
import com.example.data.repository.TafsirBook
import com.example.quran.TajweedEngine
import com.example.quran.TajweedEngine.appendUthmaniWithTajweed
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.MushafCreamPaper
import com.example.ui.theme.NightReadPaper
import com.example.ui.theme.SoftGold
import com.example.ui.viewmodel.TafsirUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WahyAyahTafsirSheet(
    ayah: MushafPageAyah,
    isNightMode: Boolean,
    tajweedColorsEnabled: Boolean,
    isBookmarked: Boolean,
    ayahAudioState: AyahAudioState,
    tafsirState: TafsirUiState,
    onDismiss: () -> Unit,
    onPlayThisAyahOnly: () -> Unit,
    onPlayFromHere: () -> Unit,
    onSelectRepeatMode: (AyahRepeatMode) -> Unit,
    onSaveBookmark: () -> Unit,
    onCopyAyah: () -> Unit,
    onShareAyah: () -> Unit,
    onSelectTafsirBook: (TafsirBook) -> Unit,
    onRetryTafsir: () -> Unit,
    onDownloadTafsirBook: (TafsirBook) -> Unit,
    onCopyTafsir: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRepeatOptions by remember { mutableStateOf(false) }

    val isThisAyahPlayingSingle = ayahAudioState.surahNumber == ayah.surahNumber &&
        ayahAudioState.ayahNumber == ayah.numberInSurah &&
        ayahAudioState.isPlaying &&
        !ayahAudioState.continueReading
    val isThisAyahPlayingContinuous = ayahAudioState.surahNumber == ayah.surahNumber &&
        ayahAudioState.ayahNumber == ayah.numberInSurah &&
        ayahAudioState.isPlaying &&
        ayahAudioState.continueReading

    val selectedBook = tafsirState.selectedBook
    val isSelectedBookDownloaded = tafsirState.downloadedBookIds.contains(selectedBook.bookId)
    val downloadProgress = tafsirState.downloadingProgress[selectedBook.bookId]

    val ayahPreviewPaper = if (isNightMode) NightReadPaper else MushafCreamPaper
    val ayahPreviewTextColor = if (isNightMode) Color(0xFFEDE8DC) else Color(0xFF1F1B14)

    val formattedAyahText = remember(ayah, tajweedColorsEnabled, isNightMode) {
        buildAnnotatedString {
            TajweedEngine.run {
                appendUthmaniWithTajweed(
                    text = ayah.text,
                    tajweedEnabled = tajweedColorsEnabled,
                    isNightMode = isNightMode,
                    baseTextColor = ayahPreviewTextColor
                )
            }
            append(" \uFD3F${toArabicIndicDigits(ayah.numberInSurah)}\uFD3E")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 660.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 22.dp)
                .testTag("ayah_long_press_tafsir_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOP HEADER: Surah name + Ayah number + Page/Juz meta
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${ayah.surahName} • الآية ${toArabicIndicDigits(ayah.numberInSurah)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = AmiriFontFamily),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "صفحة ${toArabicIndicDigits(ayah.page)} • ${OfflineQuranRepository.getJuzArabicName(ayah.juz)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = islamic.textMuted
                )
            }

            // Ayah Uthmani Text Preview Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ayahPreviewPaper,
                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = formattedAyahText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 36.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }

            // 2. TWO DISTINCT SIDE-BY-SIDE PLAYBACK BUTTONS:
            //    (1) "استماع لهذه الآية فقط"  |  (2) "استماع من هذه الآية"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPlayThisAyahOnly,
                    shape = RoundedCornerShape(13.dp),
                    border = BorderStroke(
                        width = 1.4.dp,
                        color = if (isThisAyahPlayingSingle) SoftGold else EmeraldGreen
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isThisAyahPlayingSingle) islamic.goldSoft else islamic.emeraldSoft,
                        contentColor = if (isThisAyahPlayingSingle) islamic.goldText else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("sheet_play_single_ayah_only_button")
                ) {
                    Icon(
                        imageVector = if (isThisAyahPlayingSingle) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "استماع لهذه الآية فقط",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "استماع لهذه الآية فقط",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = onPlayFromHere,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isThisAyahPlayingContinuous) SoftGold else EmeraldGreen,
                        contentColor = if (isThisAyahPlayingContinuous) Color(0xFF1A1405) else Color.White
                    ),
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("sheet_play_from_here_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "استماع من هذه الآية",
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "استماع من هذه الآية",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactSheetActionBtn(
                    icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = "علامة",
                    isHighlighted = isBookmarked,
                    testTag = "sheet_bookmark_button",
                    onClick = onSaveBookmark,
                    modifier = Modifier.weight(1f)
                )
                CompactSheetActionBtn(
                    icon = Icons.Default.ContentCopy,
                    label = "نسخ",
                    isHighlighted = false,
                    testTag = "sheet_copy_button",
                    onClick = onCopyAyah,
                    modifier = Modifier.weight(1f)
                )
                CompactSheetActionBtn(
                    icon = Icons.Default.Share,
                    label = "مشاركة",
                    isHighlighted = false,
                    testTag = "sheet_share_button",
                    onClick = onShareAyah,
                    modifier = Modifier.weight(1f)
                )
                CompactSheetActionBtn(
                    icon = Icons.Default.Repeat,
                    label = "تكرار",
                    isHighlighted = showRepeatOptions || ayahAudioState.repeatMode != AyahRepeatMode.ONCE,
                    testTag = "sheet_repeat_button",
                    onClick = { showRepeatOptions = !showRepeatOptions },
                    modifier = Modifier.weight(1f)
                )
            }

            // Expandable Repeat Mode Chips when "تكرار" is tapped
            AnimatedVisibility(visible = showRepeatOptions) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, islamic.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "تكرار الآية للحفظ والمراجعة:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = islamic.textMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AyahRepeatMode.entries.forEach { mode ->
                                val selected = ayahAudioState.repeatMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectRepeatMode(mode) },
                                    label = {
                                        Text(
                                            text = mode.arabicLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldGreen,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = islamic.border)

            // 3. INTEGRATED AUTHENTIC TAFSIR LIBRARY SECTION (مباشرة أسفل الأزرار)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "مكتبة التفاسير",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Offline status badge or compact "تحميل هذا التفسير أوفلاين" button
                when {
                    isSelectedBookDownloaded -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF2E7D32).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("tafsir_offline_ready_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "متاح بدون نت ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    downloadProgress != null -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = islamic.goldSoft,
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "جاري التحميل ${(downloadProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = islamic.goldText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            )
                        }
                    }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = islamic.emeraldSoft,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .testTag("tafsir_download_offline_button")
                                .clickable { onDownloadTafsirBook(selectedBook) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "تحميل هذا التفسير أوفلاين",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تحميل هذا التفسير أوفلاين",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Smooth download progress bar when downloading the full book
            if (downloadProgress != null) {
                LinearProgressIndicator(
                    progress = { downloadProgress.coerceIn(0f, 1f) },
                    color = EmeraldGreen,
                    trackColor = islamic.emeraldSoft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )
            }

            // Horizontal Scrollable Chip Bar of the 6 Authentic Arabic Tafsir Books
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tafsir_books_chip_bar")
            ) {
                items(TafsirBook.entries, key = { it.bookId }) { book ->
                    val isSelected = book == selectedBook
                    val isDownloaded = tafsirState.downloadedBookIds.contains(book.bookId)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldGreen else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) EmeraldGreen else islamic.border
                        ),
                        modifier = Modifier
                            .testTag("tafsir_chip_${book.bookId}")
                            .clickable { onSelectTafsirBook(book) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (isDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "متاح أوفلاين",
                                    tint = if (isSelected) Color(0xFFF6DC97) else Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = book.arabicName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Selected Ayah's Tafsir Content Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                border = BorderStroke(1.dp, islamic.border),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tafsir_content_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedBook.arabicName} • ${selectedBook.shortAuthor}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!tafsirState.currentText.isNullOrBlank()) {
                            IconButton(
                                onClick = { onCopyTafsir(tafsirState.currentText) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ التفسير",
                                    tint = islamic.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    when {
                        tafsirState.isLoading -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldGreen,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جاري جلب ${selectedBook.arabicName}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = islamic.textMuted
                                )
                            }
                        }

                        !tafsirState.errorMessage.isNullOrBlank() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tafsirState.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = islamic.textMuted,
                                    textAlign = TextAlign.Center
                                )
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = EmeraldGreen,
                                    modifier = Modifier.clickable(onClick = onRetryTafsir)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "إعادة المحاولة",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        !tafsirState.currentText.isNullOrBlank() -> {
                            Text(
                                text = tafsirState.currentText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = AmiriFontFamily,
                                    fontSize = 18.sp,
                                    lineHeight = 32.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactSheetActionBtn(
    icon: ImageVector,
    label: String,
    isHighlighted: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val islamic = LocalIslamicColors.current
    val bgColor = if (isHighlighted) islamic.goldSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val borderColor = if (isHighlighted) SoftGold else islamic.border
    val contentTint = if (isHighlighted) islamic.goldText else MaterialTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .testTag(testTag)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentTint,
                maxLines = 1
            )
        }
    }
}
