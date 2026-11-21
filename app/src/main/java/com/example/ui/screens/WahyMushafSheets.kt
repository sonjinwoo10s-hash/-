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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.ui.unit.sp
import com.example.audio.AudioOutputRoute
import com.example.audio.AudioPlayerState
import com.example.audio.AyahAudioState
import com.example.audio.QuranPlaybackSpeeds
import com.example.data.local.OfflineIslamicData
import com.example.data.model.SurahSummary
import com.example.data.repository.OfflineQuranRepository
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WahySurahIndexSheet(
    surahs: List<SurahSummary>,
    surahStartPages: Map<Int, Int>,
    currentSurahNumber: Int,
    onSelectSurahPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val normalizedQuery = remember(query) { OfflineIslamicData.normalizeArabic(query) }
    val filtered = remember(surahs, normalizedQuery, query) {
        if (normalizedQuery.isEmpty()) {
            surahs
        } else {
            surahs.filter { s ->
                OfflineIslamicData.normalizeArabic(s.name).contains(normalizedQuery) ||
                    s.englishName.contains(query, ignoreCase = true) ||
                    s.number.toString() == normalizedQuery
            }
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
                .heightIn(max = 560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("mushaf_surah_index_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "فهرس سور القرآن الكريم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${toArabicIndicDigits(surahs.size)} سورة",
                    style = MaterialTheme.typography.labelMedium,
                    color = islamic.textMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mushaf_index_search"),
                placeholder = { Text("ابحث باسم السورة أو رقمها...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "بحث", tint = islamic.textMuted)
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = islamic.border
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 18.dp)
            ) {
                items(filtered, key = { it.number }) { surah ->
                    val pageNum = surahStartPages[surah.number] ?: surah.startPage
                    val isCurrent = surah.number == currentSurahNumber
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) islamic.emeraldSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else islamic.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("index_surah_${surah.number}")
                            .clickable {
                                onSelectSurahPage(pageNum)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else islamic.goldSoft,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = toArabicIndicDigits(surah.number),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isCurrent) Color.White else islamic.goldText
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = surah.name,
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = AmiriFontFamily),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val revLabel = if (surah.revelationType == "Meccan") "مكية" else "مدنية"
                                    Text(
                                        text = "$revLabel • ${toArabicIndicDigits(surah.numberOfAyahs)} آية",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = islamic.textMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = islamic.goldSoft,
                                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = "ص ${toArabicIndicDigits(pageNum)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = islamic.goldText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WahyJuzPickerSheet(
    juzStartPages: Map<Int, Int>,
    currentJuz: Int,
    onSelectJuzPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val juzNumbers = remember { (1..30).toList() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .testTag("mushaf_juz_picker_sheet")
        ) {
            Text(
                text = "أجزاء القرآن الكريم (٣٠ جزءاً)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(juzNumbers, key = { it }) { juz ->
                    val startPage = juzStartPages[juz] ?: ((juz - 1) * 20 + 1).coerceAtMost(604)
                    val isCurrent = juz == currentJuz
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCurrent) islamic.emeraldSoft else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            1.dp,
                            if (isCurrent) MaterialTheme.colorScheme.primary else islamic.border
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("juz_item_$juz")
                            .clickable {
                                onSelectJuzPage(startPage)
                                onDismiss()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = OfflineQuranRepository.getJuzArabicName(juz),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else islamic.goldSoft,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = toArabicIndicDigits(juz),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else islamic.goldText
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "صفحة ${toArabicIndicDigits(startPage)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = islamic.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WahyOnlineAudioPanel(
    currentSurahNumber: Int,
    currentSurahName: String,
    audioState: AudioPlayerState,
    ayahAudioState: AyahAudioState,
    isNightMode: Boolean,
    onToggleSurahAudio: () -> Unit,
    onOpenReciterLibrary: () -> Unit,
    onSeekAudio: (Float) -> Unit,
    onSelectPlaybackSpeed: (Float) -> Unit,
    onToggleWordByWord: (Boolean) -> Unit,
    onSyncLatencyOffsetChange: (Int) -> Unit,
    onDownloadMp3: () -> Unit,
    onStopSingleAyah: () -> Unit
) {
    val view = LocalView.current
    val bgColor = if (isNightMode) Color(0xFF1A221E) else Color(0xFFF6EFE0)
    val borderColor = if (isNightMode) Color(0xFF2D3C35) else Color(0xFFDCC99C)
    val textColor = if (isNightMode) Color(0xFFEDE8DC) else Color(0xFF1F1C16)
    val mutedColor = if (isNightMode) Color(0xFFA7B7B0) else Color(0xFF6B5E45)
    val isCurrentSurahLoaded = audioState.loadedSurahNumber == currentSurahNumber
    var showSpeedChips by remember { mutableStateOf(false) }
    var showSyncSettings by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mushaf_online_audio_panel")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        shape = CircleShape,
                        color = EmeraldGreen,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mushaf_audio_play_button")
                            .clickable {
                                performKeyTapHaptic(view)
                                onToggleSurahAudio()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (audioState.isBuffering && isCurrentSurahLoaded) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (audioState.isPlaying && isCurrentSurahLoaded) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "تشغيل التلاوة",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تلاوة $currentSurahName (أونلاين)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable(onClick = onOpenReciterLibrary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "تغيير القارئ",
                                tint = SoftGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${audioState.selectedReciter.arabicName} • تغيير القارئ",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isNightMode) Color(0xFFE7C875) else EmeraldGreen,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Playback Speed Button (1.0x)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (showSpeedChips || audioState.playbackSpeed != 1.0f) {
                            SoftGold
                        } else {
                            EmeraldGreen.copy(alpha = if (isNightMode) 0.25f else 0.12f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showSpeedChips || audioState.playbackSpeed != 1.0f) SoftGold else borderColor
                        ),
                        modifier = Modifier
                            .testTag("mushaf_audio_speed_button")
                            .clickable { showSpeedChips = !showSpeedChips }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = QuranPlaybackSpeeds.formatLabel(audioState.playbackSpeed),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (showSpeedChips || audioState.playbackSpeed != 1.0f) {
                                    Color(0xFF1A1405)
                                } else {
                                    textColor
                                },
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDownloadMp3,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("mushaf_audio_download_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "تحميل السورة MP3",
                            tint = mutedColor
                        )
                    }
                }
            }

            // Expandable Playback Speed Selector Chips (0.5x .. 2.0x)
            AnimatedVisibility(visible = showSpeedChips) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "السرعة:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = mutedColor
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(QuranPlaybackSpeeds.options) { speed ->
                            val selected = audioState.playbackSpeed == speed
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) EmeraldGreen else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) EmeraldGreen else borderColor
                                ),
                                modifier = Modifier
                                    .testTag("mushaf_speed_chip_${QuranPlaybackSpeeds.formatLabel(speed)}")
                                    .clickable {
                                        onSelectPlaybackSpeed(speed)
                                        showSpeedChips = false
                                    }
                            ) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = QuranPlaybackSpeeds.formatLabel(speed),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else textColor,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isCurrentSurahLoaded && audioState.durationMs > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                val progress = (audioState.currentPositionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Slider(
                        value = progress,
                        onValueChange = onSeekAudio,
                        colors = SliderDefaults.colors(
                            thumbColor = SoftGold,
                            activeTrackColor = EmeraldGreen,
                            inactiveTrackColor = borderColor
                        ),
                        modifier = Modifier.height(26.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatMushafDuration(audioState.currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor
                    )
                    Text(
                        text = formatMushafDuration(audioState.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedColor
                    )
                }
            }

            // Subtle Audio Output Status Pill + Sync Settings Toggle
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(8.dp))

            val routeIcon = when (audioState.audioOutputRoute) {
                AudioOutputRoute.BLUETOOTH -> Icons.Default.BluetoothAudio
                AudioOutputRoute.WIRED_OR_USB -> Icons.Default.Headset
                AudioOutputRoute.SPEAKER -> Icons.Default.VolumeUp
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (audioState.audioOutputRoute.isBluetooth) {
                        EmeraldGreen.copy(alpha = if (isNightMode) 0.25f else 0.12f)
                    } else {
                        borderColor.copy(alpha = if (isNightMode) 0.45f else 0.35f)
                    },
                    border = BorderStroke(
                        0.8.dp,
                        if (audioState.audioOutputRoute.isBluetooth) EmeraldGreen.copy(alpha = 0.5f) else borderColor
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("audio_output_route_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = routeIcon,
                            contentDescription = null,
                            tint = if (audioState.audioOutputRoute.isBluetooth) {
                                if (isNightMode) Color(0xFF7CE2B9) else EmeraldGreen
                            } else {
                                mutedColor
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "مخرج الصوت: ${audioState.audioOutputRoute.arabicLabel}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (showSyncSettings) SoftGold else Color.Transparent,
                    border = BorderStroke(1.dp, if (showSyncSettings) SoftGold else borderColor),
                    modifier = Modifier
                        .testTag("mushaf_sync_settings_toggle")
                        .clickable {
                            performKeyTapHaptic(view)
                            showSyncSettings = !showSyncSettings
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "إعدادات التزامن",
                            tint = if (showSyncSettings) Color(0xFF1A1405) else mutedColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "التزامن",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (showSyncSettings) Color(0xFF1A1405) else textColor
                        )
                    }
                }
            }

            // Expandable Word-by-Word Toggle & Fine Latency Calibration Slider (-300ms .. +300ms)
            AnimatedVisibility(visible = showSyncSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تظليل كلمة بكلمة",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Text(
                                text = "إبراز الكلمة المتلوّة لحظياً مع تظليل الآية (اضغط على أي كلمة للانتقال إليها)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = mutedColor
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = audioState.wordByWordEnabled,
                            onCheckedChange = {
                                performKeyTapHaptic(view)
                                onToggleWordByWord(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldGreen
                            ),
                            modifier = Modifier.testTag("mushaf_word_by_word_switch")
                        )
                    }

                    HorizontalDivider(color = borderColor.copy(alpha = 0.55f))

                    val activeMs = audioState.activeLatencyOffsetMs
                    val defaultForRoute = audioState.audioOutputRoute.defaultLatencyMs
                    val signPrefix = if (activeMs > 0) "+" else ""
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معايرة دقيقة للتزامن",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "$signPrefix${activeMs} مللي ثانية",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isNightMode) Color(0xFFE7C875) else EmeraldGreen
                            )
                            if (activeMs != defaultForRoute) {
                                Text(
                                    text = "افتراضي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftGold,
                                    modifier = Modifier
                                        .clickable {
                                            performKeyTapHaptic(view)
                                            onSyncLatencyOffsetChange(defaultForRoute)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Slider(
                            value = activeMs.toFloat(),
                            onValueChange = { raw ->
                                // Snap smoothly in 10ms steps for clean calibration
                                val snapped = ((raw / 10f).roundToInt() * 10).coerceIn(-300, 300)
                                onSyncLatencyOffsetChange(snapped)
                            },
                            valueRange = -300f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = SoftGold,
                                activeTrackColor = EmeraldGreen,
                                inactiveTrackColor = borderColor
                            ),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("mushaf_latency_calibration_slider")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "-300ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = mutedColor
                        )
                        Text(
                            text = if (audioState.audioOutputRoute.isBluetooth) {
                                "يُحفظ تلقائياً للبلوتوث (الافتراضي +180ms)"
                            } else {
                                "يُحفظ تلقائياً للسماعة (الافتراضي 0ms)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = mutedColor
                        )
                        Text(
                            text = "+300ms",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = mutedColor
                        )
                    }
                }
            }

            if (ayahAudioState.isPlaying || ayahAudioState.isBuffering) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = borderColor)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تلاوة الآية ${toArabicIndicDigits(ayahAudioState.ayahNumber ?: 1)} • ${ayahAudioState.activeReciterName}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }
                    IconButton(
                        onClick = onStopSingleAyah,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "إيقاف تلاوة الآية",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatMushafDuration(ms: Int): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}
