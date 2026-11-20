package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.OfflineIslamicData
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.CalculatedWakeupTime
import com.example.data.model.CityOption
import com.example.data.model.DailyQuote
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.MuezzinItem
import com.example.data.model.PrayerTimesUiState
import com.example.data.model.QuranBookmark
import com.example.data.model.WorshipTask
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold

@Composable
fun HomeScreen(
    selectedCity: CityOption,
    prayerState: PrayerTimesUiState,
    activeMuezzin: MuezzinItem,
    adhanState: AdhanPlayerState,
    fajrAlarmSettings: FajrAlarmSettings,
    fajrWakeupTime: CalculatedWakeupTime,
    worshipTasks: List<WorshipTask>,
    bookmark: QuranBookmark?,
    dailyVerse: DailyQuote,
    dailyHadith: DailyQuote,
    onSelectCity: (CityOption) -> Unit,
    onRefreshPrayers: () -> Unit,
    onToggleAdhanPreview: (MuezzinItem) -> Unit,
    onOpenMuezzinLibrary: () -> Unit,
    onOpenFajrAlarm: () -> Unit,
    onToggleTask: (String) -> Unit,
    onContinueReading: () -> Unit,
    onRotateInspiration: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Prayer Times Hero Card
        item(key = "prayer_hero") {
            PrayerHeroCard(
                selectedCity = selectedCity,
                prayerState = prayerState,
                activeMuezzin = activeMuezzin,
                adhanState = adhanState,
                fajrAlarmSettings = fajrAlarmSettings,
                fajrWakeupTime = fajrWakeupTime,
                onSelectCity = onSelectCity,
                onRefresh = onRefreshPrayers,
                onToggleAdhanPreview = { onToggleAdhanPreview(activeMuezzin) },
                onOpenMuezzinLibrary = onOpenMuezzinLibrary,
                onOpenFajrAlarm = onOpenFajrAlarm
            )
        }

        // 2. Quick Continue Reading Banner
        if (bookmark != null) {
            item(key = "continue_reading") {
                ContinueReadingBanner(
                    bookmark = bookmark,
                    onClick = onContinueReading
                )
            }
        }

        // 3. Daily Worship Tracker
        item(key = "worship_tracker") {
            DailyWorshipTrackerCard(
                tasks = worshipTasks,
                onToggleTask = onToggleTask
            )
        }

        // 4. Daily Inspiration
        item(key = "daily_inspiration") {
            DailyInspirationCard(
                verse = dailyVerse,
                hadith = dailyHadith,
                onRotate = onRotateInspiration,
                onCopy = { text, label ->
                    copyTextToClipboard(context, text)
                    onShowToast("تم نسخ $label")
                },
                onShare = { text ->
                    sharePlainText(context, text)
                }
            )
        }
    }
}

@Composable
private fun PrayerHeroCard(
    selectedCity: CityOption,
    prayerState: PrayerTimesUiState,
    activeMuezzin: MuezzinItem,
    adhanState: AdhanPlayerState,
    fajrAlarmSettings: FajrAlarmSettings,
    fajrWakeupTime: CalculatedWakeupTime,
    onSelectCity: (CityOption) -> Unit,
    onRefresh: () -> Unit,
    onToggleAdhanPreview: () -> Unit,
    onOpenMuezzinLibrary: () -> Unit,
    onOpenFajrAlarm: () -> Unit
) {
    var cityDropdownExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_hero_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Generated subtle architectural emerald-and-gold background art
            Image(
                painter = painterResource(id = R.drawable.img_prayer_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            // Smooth emerald gradient scrim ensuring high contrast legibility
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DeepGreen.copy(alpha = 0.84f),
                                EmeraldGreen.copy(alpha = 0.78f),
                                DeepGreen.copy(alpha = 0.90f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Dates + City selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prayerState.hijriDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF6DC97)
                        )
                        if (prayerState.gregorianDate.isNotEmpty()) {
                            Text(
                                text = prayerState.gregorianDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                            modifier = Modifier
                                .testTag("city_selector_button")
                                .clickable { cityDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "اختر المدينة",
                                    tint = Color(0xFFF6DC97),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedCity.arabicLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = cityDropdownExpanded,
                            onDismissRequest = { cityDropdownExpanded = false }
                        ) {
                            OfflineIslamicData.cities.forEach { city ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = city.arabicLabel,
                                            fontWeight = if (city.id == selectedCity.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (city.id == selectedCity.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        cityDropdownExpanded = false
                                        onSelectCity(city)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Next prayer & live countdown
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.24f),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "الصلاة القادمة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = prayerState.nextPrayerName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الوقت المتبقي",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = prayerState.countdownText,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF6DC97),
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5 Daily Prayers Grid (Fajr card includes quick Smart Fajr Alarm indicator)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prayerState.prayers.forEach { entry ->
                        val isFajr = entry.key == "Fajr"
                        val bgColor = if (entry.isNext) SoftGold.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.12f)
                        val borderColor = if (entry.isNext) Color(0xFFE7C875) else Color.White.copy(alpha = 0.12f)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .then(
                                    if (isFajr) Modifier
                                        .testTag("home_fajr_prayer_cell")
                                        .clickable(onClick = onOpenFajrAlarm)
                                    else Modifier
                                )
                                .padding(vertical = 9.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.arabicName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (isFajr) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "منبه الفجر الذكي",
                                        tint = if (fajrAlarmSettings.enabled) Color(0xFFF6DC97) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = entry.formatted12,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isNext) Color(0xFFF6DC97) else Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dedicated Smart Fajr Alarm Shortcut Bar right below Fajr / Prayer Grid
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (fajrAlarmSettings.enabled) SoftGold.copy(alpha = 0.22f) else Color.Black.copy(alpha = 0.2f),
                    border = BorderStroke(
                        1.dp,
                        if (fajrAlarmSettings.enabled) SoftGold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_smart_fajr_alarm_button")
                        .clickable(onClick = onOpenFajrAlarm)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "منبه الفجر الذكي",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "منبه صلاة الفجر الذكي",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (fajrAlarmSettings.enabled) {
                                        "مفعّل • الاستيقاظ ${fajrWakeupTime.formatted12Arabic} (${fajrAlarmSettings.resolvedChallenge().arabicTitle})"
                                    } else {
                                        "اضبط منبهاً ذكياً قبل الفجر بـ ${fajrAlarmSettings.offsetMinutesBeforeFajr} دقيقة"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF6DC97),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (fajrAlarmSettings.enabled) Color(0xFF16A34A) else Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = if (fajrAlarmSettings.enabled) "مفعّل" else "ضبط",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Currently Selected Muezzin Strip inside Prayer Times Card
                val isThisActive = adhanState.activeMuezzinId == activeMuezzin.id
                val isAdhanLoading = isThisActive && adhanState.status == AdhanPlaybackStatus.LOADING
                val isAdhanPlaying = isThisActive && (
                    adhanState.status == AdhanPlaybackStatus.PLAYING ||
                        adhanState.status == AdhanPlaybackStatus.FALLBACK_PLAYING
                    )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.22f),
                    border = BorderStroke(
                        1.dp,
                        if (isAdhanPlaying) SoftGold.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_muezzin_strip")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(if (isAdhanPlaying) SoftGold else Color.White.copy(alpha = 0.16f))
                                        .testTag("home_adhan_play_button")
                                        .clickable(onClick = onToggleAdhanPreview),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isAdhanLoading -> CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFF6DC97)
                                        )
                                        isAdhanPlaying -> Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = "إيقاف الأذان",
                                            tint = Color(0xFF1A1405),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل صوت المؤذن الافتراضي",
                                            tint = Color(0xFFF6DC97),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClick = onOpenMuezzinLibrary)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = Color(0xFFF6DC97),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isThisActive && adhanState.status != AdhanPlaybackStatus.IDLE) {
                                                "صوت الأذان • ${adhanState.status.arabicLabel}"
                                            } else {
                                                "المؤذن الافتراضي • ${activeMuezzin.regionEnum().label}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isThisActive && adhanState.status == AdhanPlaybackStatus.ERROR) {
                                                Color(0xFFFECACA)
                                            } else {
                                                Color(0xFFF6DC97)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = activeMuezzin.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftGold,
                                modifier = Modifier
                                    .testTag("open_muezzin_library_home_button")
                                    .clickable(onClick = onOpenMuezzinLibrary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "فتح مكتبة المؤذنين العالمية",
                                        tint = Color(0xFF1A1405),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "المؤذنين",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1405)
                                    )
                                }
                            }
                        }

                        if (isAdhanPlaying) {
                            LinearProgressIndicator(
                                progress = { adhanState.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = SoftGold,
                                trackColor = Color.White.copy(alpha = 0.14f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Status & refresh button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prayerState.statusNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TextButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_prayers_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث مواقيت الصلاة",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تحديث",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFF6DC97)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueReadingBanner(
    bookmark: QuranBookmark,
    onClick: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("continue_reading_card")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = islamic.goldSoft),
        border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .background(islamic.gold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = "علامة القرآن",
                        tint = islamic.goldText
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "متابعة القراءة من حيث توقفت",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${bookmark.surahName} — الآية ${bookmark.ayahNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = islamic.textMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = islamic.gold
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "متابعة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1405)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF1A1405),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyWorshipTrackerCard(
    tasks: List<WorshipTask>,
    onToggleTask: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val doneCount = tasks.count { it.completed }
    val rawProgress = if (tasks.isEmpty()) 0f else doneCount.toFloat() / tasks.size.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "worship_progress")
    val pct = (rawProgress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_worship_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(title = "وردي اليومي")
                Surface(
                    shape = RoundedCornerShape(50),
                    color = islamic.emeraldSoft
                ) {
                    Text(
                        text = "$pct% مكتمل",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = islamic.emeraldSoft
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2-column checklist grid
            tasks.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { task ->
                        val bgColor by animateColorAsState(
                            targetValue = if (task.completed) islamic.emeraldSoft else islamic.cardAlt,
                            label = "task_bg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (task.completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else islamic.border,
                            label = "task_border"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .testTag("worship_task_${task.id}")
                                .clickable { onToggleTask(task.id) }
                                .padding(horizontal = 10.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (task.completed) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .border(
                                        2.dp,
                                        if (task.completed) MaterialTheme.colorScheme.primary else islamic.textMuted.copy(alpha = 0.6f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.completed) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "مكتمل",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyInspirationCard(
    verse: DailyQuote,
    hadith: DailyQuote,
    onRotate: () -> Unit,
    onCopy: (String, String) -> Unit,
    onShare: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_inspiration_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(title = "إلهام اليوم")
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = islamic.emeraldSoft,
                    modifier = Modifier
                        .testTag("rotate_inspiration_button")
                        .clickable(onClick = onRotate)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تبديل إلهام اليوم",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تبديل",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InspirationQuoteBox(
                label = "آية اليوم",
                quote = verse,
                fontSizeSp = 21,
                onCopy = {
                    onCopy("${verse.text}\n(${verse.reference})\n— من تطبيق رفيق المسلم", "الآية")
                },
                onShare = {
                    onShare("${verse.text}\n(${verse.reference})\n— من تطبيق رفيق المسلم")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            InspirationQuoteBox(
                label = "حديث اليوم",
                quote = hadith,
                fontSizeSp = 19,
                onCopy = {
                    onCopy("${hadith.text}\n(${hadith.reference})\n— من تطبيق رفيق المسلم", "الحديث")
                },
                onShare = {
                    onShare("${hadith.text}\n(${hadith.reference})\n— من تطبيق رفيق المسلم")
                }
            )
        }
    }
}

@Composable
private fun InspirationQuoteBox(
    label: String,
    quote: DailyQuote,
    fontSizeSp: Int,
    onCopy: () -> Unit,
    onShare: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(islamic.cardAlt)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(120.dp)
                .background(islamic.gold)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = islamic.goldText,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = quote.text,
                fontFamily = AmiriFontFamily,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.75f).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = quote.reference,
                style = MaterialTheme.typography.bodySmall,
                color = islamic.textMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier) {
    val islamic = LocalIslamicColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(islamic.gold)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun MiniActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val islamic = LocalIslamicColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) islamic.goldSoft else Color.Transparent,
        border = BorderStroke(1.dp, if (highlighted) islamic.gold else islamic.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (highlighted) islamic.goldText else islamic.textMuted,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlighted) islamic.goldText else islamic.textMuted
            )
        }
    }
}

fun copyTextToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("رفيق المسلم", text))
}

fun sharePlainText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة عبر"))
}
