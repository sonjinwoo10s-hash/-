package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
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
import com.example.alarm.AstronomicalPrayerCalculator
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.CalculatedWakeupTime
import com.example.data.model.DailyQuote
import com.example.data.model.FajrAlarmSettings
import com.example.data.model.MuezzinItem
import com.example.data.model.PrayerTimesUiState
import com.example.data.model.QuranBookmark
import com.example.data.model.WorshipTask
import com.example.data.repository.GlobalLocationConfig
import com.example.ui.components.RamadanCrescentCard
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import java.util.Calendar

@Composable
fun HomeScreen(
    globalLocation: GlobalLocationConfig,
    hijriDayOffset: Int,
    maghribAdjustmentMinutes: Int,
    prayerState: PrayerTimesUiState,
    pendingQadaaCount: Int,
    activeMuezzin: MuezzinItem,
    adhanState: AdhanPlayerState,
    fajrAlarmSettings: FajrAlarmSettings,
    fajrWakeupTime: CalculatedWakeupTime,
    worshipTasks: List<WorshipTask>,
    bookmark: QuranBookmark?,
    dailyVerse: DailyQuote,
    dailyHadith: DailyQuote,
    onOpenGlobalLocation: () -> Unit,
    onOpenPrayerTracker: () -> Unit,
    onPrevPrayerDate: () -> Unit,
    onNextPrayerDate: () -> Unit,
    onSelectPrayerDate: (Int, Int, Int) -> Unit,
    onReturnToTodayDate: () -> Unit,
    onTogglePrayerPerformed: (String) -> Unit,
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
                globalLocation = globalLocation,
                prayerState = prayerState,
                pendingQadaaCount = pendingQadaaCount,
                activeMuezzin = activeMuezzin,
                adhanState = adhanState,
                fajrAlarmSettings = fajrAlarmSettings,
                fajrWakeupTime = fajrWakeupTime,
                onOpenGlobalLocation = onOpenGlobalLocation,
                onOpenPrayerTracker = onOpenPrayerTracker,
                onPrevPrayerDate = onPrevPrayerDate,
                onNextPrayerDate = onNextPrayerDate,
                onSelectPrayerDate = onSelectPrayerDate,
                onReturnToTodayDate = onReturnToTodayDate,
                onTogglePrayerPerformed = onTogglePrayerPerformed,
                onRefresh = onRefreshPrayers,
                onToggleAdhanPreview = { onToggleAdhanPreview(activeMuezzin) },
                onOpenMuezzinLibrary = onOpenMuezzinLibrary,
                onOpenFajrAlarm = onOpenFajrAlarm
            )
        }

        // 2. Ramadan Countdown & Live Crescent Sighting Card (isolated from 1-second prayer ticks)
        item(key = "ramadan_crescent_card") {
            RamadanCrescentCard(
                globalLocation = globalLocation,
                hijriDayOffset = hijriDayOffset,
                maghribAdjustmentMinutes = maghribAdjustmentMinutes,
                onShowToast = onShowToast
            )
        }

        // 3. Quick Continue Reading Banner
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
    globalLocation: GlobalLocationConfig,
    prayerState: PrayerTimesUiState,
    pendingQadaaCount: Int,
    activeMuezzin: MuezzinItem,
    adhanState: AdhanPlayerState,
    fajrAlarmSettings: FajrAlarmSettings,
    fajrWakeupTime: CalculatedWakeupTime,
    onOpenGlobalLocation: () -> Unit,
    onOpenPrayerTracker: () -> Unit,
    onPrevPrayerDate: () -> Unit,
    onNextPrayerDate: () -> Unit,
    onSelectPrayerDate: (Int, Int, Int) -> Unit,
    onReturnToTodayDate: () -> Unit,
    onTogglePrayerPerformed: (String) -> Unit,
    onRefresh: () -> Unit,
    onToggleAdhanPreview: () -> Unit,
    onOpenMuezzinLibrary: () -> Unit,
    onOpenFajrAlarm: () -> Unit
) {
    val context = LocalContext.current

    fun showDatePicker() {
        val cal = AstronomicalPrayerCalculator.parseDateKey(prayerState.selectedDateKey)
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onSelectPrayerDate(year, month, dayOfMonth)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

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
                // Top Header Row: Global Location Pill (opens GPS + Global Search modal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("city_selector_button")
                            .clickable(onClick = onOpenGlobalLocation)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "تحديد الموقع العالمي",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = globalLocation.displayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
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

                    // Sleek compact "سجلّي" pill + optional Blessed Days badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!prayerState.blessedDayBadge.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SoftGold.copy(alpha = 0.24f),
                                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.7f)),
                                modifier = Modifier.testTag("home_blessed_day_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = prayerState.blessedDayBadge,
                                        tint = Color(0xFFF6DC97),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = prayerState.blessedDayBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF6DC97),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(
                                1.dp,
                                if (pendingQadaaCount > 0) SoftGold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onOpenPrayerTracker)
                                .testTag("home_open_prayer_tracker_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "سجلّي",
                                    tint = Color(0xFFF6DC97),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (pendingQadaaCount > 0) "سجلّي ($pendingQadaaCount)" else "سجلّي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                // Subtle live Iqama countdown badge — visible ONLY between Adhan and Iqama
                AnimatedVisibility(visible = prayerState.activeIqamaCountdown != null) {
                    val iqama = prayerState.activeIqamaCountdown
                    if (iqama != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SoftGold.copy(alpha = 0.20f),
                                border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("home_live_iqama_badge")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "العد التنازلي للإقامة",
                                            tint = Color(0xFFF6DC97),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "متبقي على إقامة صلاة ${iqama.prayerArabicName}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(
                                            text = iqama.remainingText,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFF6DC97),
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Compact Date Navigation Bar right at the top of the prayer list
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.20f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("home_compact_date_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onPrevPrayerDate,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("prayer_date_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "اليوم السابق",
                                    tint = Color(0xFFF6DC97)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showDatePicker() }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("prayer_date_picker_trigger"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "اختيار التاريخ",
                                        tint = Color(0xFFF6DC97),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = prayerState.hijriDate,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF6DC97),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (prayerState.gregorianDate.isNotEmpty()) {
                                    Text(
                                        text = prayerState.gregorianDate,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = onNextPrayerDate,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("prayer_date_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = "اليوم التالي",
                                    tint = Color(0xFFF6DC97)
                                )
                            }
                        }

                        // Contextual pills ONLY when not on today ("العودة لليوم" + past day "X/5 صلوات" summary)
                        if (!prayerState.isSelectedDateToday) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = SoftGold,
                                    modifier = Modifier
                                        .testTag("return_to_today_pill")
                                        .clickable(onClick = onReturnToTodayDate)
                                ) {
                                    Text(
                                        text = "العودة لليوم",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1A1405),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }

                                if (prayerState.isSelectedDatePast) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (prayerState.performedCount >= 5) {
                                            Color(0xFF16A34A).copy(alpha = 0.85f)
                                        } else {
                                            Color.White.copy(alpha = 0.16f)
                                        },
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                        modifier = Modifier.testTag("past_date_prayer_summary_badge")
                                    ) {
                                        Text(
                                            text = "${prayerState.performedCount}/5 صلوات",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5 Daily Prayers Grid with real-time time-gated Prayer Log checkboxes (Past/Entered unlocked; Upcoming/Future locked)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    prayerState.prayers.forEach { entry ->
                        val isFajr = entry.key == "Fajr"
                        val isLocked = entry.isLockedFuture || prayerState.isSelectedDateFuture
                        val isChecked = !isLocked && entry.isPerformed
                        val isPastMissed = prayerState.isSelectedDatePast && !entry.isPerformed
                        val bgColor = when {
                            entry.isNext -> SoftGold.copy(alpha = 0.28f)
                            isChecked -> Color(0xFF16A34A).copy(alpha = 0.24f)
                            isPastMissed -> Color.Black.copy(alpha = 0.22f)
                            isLocked -> Color.White.copy(alpha = 0.08f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                        val borderColor = when {
                            entry.isNext -> Color(0xFFE7C875)
                            isChecked -> Color(0xFF4ADE80).copy(alpha = 0.7f)
                            isLocked -> Color.White.copy(alpha = 0.10f)
                            else -> Color.White.copy(alpha = 0.12f)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .testTag("home_prayer_cell_${entry.key}")
                                .clickable { onTogglePrayerPerformed(entry.key) }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.arabicName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        isPastMissed -> Color.White.copy(alpha = 0.65f)
                                        isLocked && !entry.isNext -> Color.White.copy(alpha = 0.72f)
                                        else -> Color.White.copy(alpha = 0.92f)
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (isFajr) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.Alarm,
                                        contentDescription = "منبه الفجر الذكي",
                                        tint = if (fajrAlarmSettings.enabled) Color(0xFFF6DC97) else Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable(onClick = onOpenFajrAlarm)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = entry.formatted12,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        entry.isNext -> Color(0xFFF6DC97)
                                        isPastMissed -> Color.White.copy(alpha = 0.68f)
                                        isLocked -> Color.White.copy(alpha = 0.75f)
                                        else -> Color.White
                                    },
                                    maxLines = 1
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .alpha(if (isLocked) 0.55f else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isChecked -> Color(0xFF16A34A)
                                            isLocked -> Color.Black.copy(alpha = 0.20f)
                                            prayerState.isSelectedDatePast -> Color.White.copy(alpha = 0.08f)
                                            else -> Color.White.copy(alpha = 0.14f)
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            isChecked -> Color(0xFF86EFAC)
                                            isLocked -> Color.White.copy(alpha = 0.24f)
                                            prayerState.isSelectedDatePast -> Color.White.copy(alpha = 0.28f)
                                            else -> Color.White.copy(alpha = 0.42f)
                                        },
                                        shape = CircleShape
                                    )
                                    .testTag(
                                        if (isLocked) "prayer_log_locked_${entry.key}" else "prayer_log_check_${entry.key}"
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isChecked -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "تم أداء ${entry.arabicName}",
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                    isLocked -> {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "لم يدخل وقت ${entry.arabicName} بعد",
                                            tint = Color.White.copy(alpha = 0.78f),
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                    prayerState.isSelectedDatePast -> {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.35f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Automatic Islamic Midnight & Last Third of the Night Card (Offline Calculation)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("islamic_night_times_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Islamic Midnight (منتصف الليل الشرعي)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = "منتصف الليل الشرعي",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "منتصف الليل الشرعي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = prayerState.islamicMidnightFormatted,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF6DC97),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(28.dp)
                                .background(SoftGold.copy(alpha = 0.35f))
                        )

                        // Last Third of the Night (الثلث الأخير من الليل)
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "الثلث الأخير من الليل",
                                tint = Color(0xFFF6DC97),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "الثلث الأخير من الليل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = prayerState.lastThirdNightFormatted,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFF6DC97),
                                        maxLines = 1
                                    )
                                }
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
                        text = "${bookmark.surahName} • صفحة ${toArabicIndicDigits(bookmark.pageNumber)} (الآية ${toArabicIndicDigits(bookmark.ayahNumber)})",
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
                        val isLocked = task.isLockedFuture
                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isLocked -> islamic.cardAlt
                                task.completed -> islamic.emeraldSoft
                                else -> islamic.cardAlt
                            },
                            label = "task_bg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isLocked -> islamic.border
                                task.completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                else -> islamic.border
                            },
                            label = "task_border"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .alpha(if (isLocked) 0.58f else 1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                .testTag("worship_task_${task.id}")
                                .clickable(enabled = !isLocked) { onToggleTask(task.id) }
                                .padding(horizontal = 10.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isLocked -> islamic.border.copy(alpha = 0.45f)
                                            task.completed -> MaterialTheme.colorScheme.primary
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isLocked) 1.5.dp else 2.dp,
                                        color = when {
                                            isLocked -> islamic.textMuted.copy(alpha = 0.5f)
                                            task.completed -> MaterialTheme.colorScheme.primary
                                            else -> islamic.textMuted.copy(alpha = 0.6f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isLocked -> Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "لم يحن وقتها بعد",
                                        tint = islamic.textMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    task.completed -> Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "مكتمل",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        isLocked -> islamic.textMuted
                                        task.completed -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    textDecoration = if (!isLocked && task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isLocked) {
                                    Text(
                                        text = "لم يحن وقتها بعد",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = islamic.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
