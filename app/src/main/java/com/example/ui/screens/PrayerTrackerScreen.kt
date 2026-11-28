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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.example.alarm.AstronomicalPrayerCalculator
import com.example.alarm.DayCompletionStatus
import com.example.alarm.MonthCalendarData
import com.example.alarm.MonthDayCell
import com.example.alarm.PrayerLogStats
import com.example.alarm.PrayerLogStore
import com.example.alarm.QadaaItem
import com.example.alarm.YearlyOverviewData
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.SoftGold
import java.util.Calendar

private enum class TrackerTab(val label: String) {
    MONTHLY("السجل الشهري"),
    YEARLY("الملخص السنوي"),
    QADAA("صلوات القضاء")
}

private val calendarWeekdayHeaders = listOf("سبت", "أحد", "إثن", "ثلا", "أرب", "خمي", "جمع")

@Composable
fun PrayerTrackerScreen(
    stats: PrayerLogStats,
    pendingQadaaItems: List<QadaaItem>,
    logRevision: Long,
    lockedTodayPrayerKeys: Set<String> = emptySet(),
    onTogglePrayerForDate: (dateKey: String, prayerKey: String) -> Unit,
    onMarkQadaaDone: (dateKey: String, prayerKey: String) -> Unit,
    onToggleSunnahHabit: (dateKey: String, habitKey: String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val view = LocalView.current
    val islamic = LocalIslamicColors.current

    val todayCal = remember { Calendar.getInstance() }
    val todayKey = remember { AstronomicalPrayerCalculator.todayDateKey() }

    var activeTab by remember { mutableStateOf(TrackerTab.MONTHLY) }
    var viewingYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var viewingMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var sunnahExpanded by remember { mutableStateOf(false) }
    var yearMenuExpanded by remember { mutableStateOf(false) }

    val initialYear = remember { todayCal.get(Calendar.YEAR) }
    val availableYears by produceState(
        initialValue = listOf(initialYear, initialYear - 1),
        key1 = logRevision
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { PrayerLogStore.getAvailableYears(context) }
                .getOrDefault(listOf(initialYear, initialYear - 1))
        }
    }

    val monthCalendar by produceState(
        initialValue = MonthCalendarData(
            year = viewingYear,
            monthZeroBased = viewingMonth,
            monthLabelArabic = "${PrayerLogStore.arabicMonthNames[viewingMonth.coerceIn(0, 11)]} $viewingYear",
            firstDayOffsetFromSat = 0,
            days = emptyList(),
            completedPrayers = 0,
            totalTrackablePrayers = 0,
            completionPercent = 0,
            perfectDaysCount = 0
        ),
        key1 = viewingYear,
        key2 = viewingMonth,
        key3 = logRevision
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching { PrayerLogStore.getMonthCalendar(context, viewingYear, viewingMonth) }
                .getOrDefault(value)
        }
    }

    val yearlyOverview by produceState(
        initialValue = YearlyOverviewData(
            year = viewingYear,
            months = emptyList(),
            annualCompleted = 0,
            annualTrackable = 0,
            annualPercent = 0,
            perfectDaysCount = 0
        ),
        key1 = viewingYear,
        key2 = logRevision,
        key3 = activeTab
    ) {
        if (activeTab == TrackerTab.YEARLY) {
            value = withContext(Dispatchers.IO) {
                runCatching { PrayerLogStore.getYearlyOverview(context, viewingYear) }
                    .getOrDefault(value)
            }
        }
    }

    val selectedDayCell = remember(monthCalendar, selectedDateKey, logRevision) {
        monthCalendar.days.find { it.dateKey == selectedDateKey }
            ?: if (viewingYear == todayCal.get(Calendar.YEAR) && viewingMonth == todayCal.get(Calendar.MONTH)) {
                monthCalendar.days.find { it.isToday } ?: monthCalendar.days.firstOrNull()
            } else {
                monthCalendar.days.firstOrNull()
            }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer_tracker_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Emerald Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .testTag("close_prayer_tracker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق سجل الصلوات",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "سجل الصلوات والعبادات",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "محفوظ تلقائياً • سجل غير محدود بالشهور والسنوات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF6DC97)
                                )
                            }
                        }

                        // Streak Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGold.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "سلسلة الالتزام",
                                    tint = SoftGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${stats.currentStreakDays} يوم",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Compact 3-Metric Summary Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeaderStatPill(
                            label = "اليوم",
                            value = "${stats.todayCompleted}/5",
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStatPill(
                            label = "الأسبوع",
                            value = "${stats.weeklyPercent}%",
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStatPill(
                            label = "القضاء",
                            value = if (pendingQadaaItems.isEmpty()) "0 ✓" else "${pendingQadaaItems.size}",
                            highlightWarn = pendingQadaaItems.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { activeTab = TrackerTab.QADAA }
                        )
                    }
                }
            }

            // Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TrackerTab.entries.forEach { tab ->
                    val selected = activeTab == tab
                    val badgeCount = if (tab == TrackerTab.QADAA) pendingQadaaItems.size else 0
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else islamic.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                performKeyTapHaptic(view)
                                activeTab = tab
                            }
                            .testTag("tracker_tab_${tab.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (badgeCount > 0) "${tab.label} ($badgeCount)" else tab.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subtle Next-Day Reminder Banner if yesterday had missed Fard prayers
                if (stats.yesterdayMissedCount > 0 && activeTab != TrackerTab.QADAA) {
                    item(key = "yesterday_qadaa_banner") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeTab = TrackerTab.QADAA }
                                .testTag("tracker_yesterday_qadaa_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = islamic.goldSoft),
                            border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.7f))
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
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        tint = islamic.goldText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "تذكير لطيف: فاتتك ${stats.yesterdayMissedCount} صلاة من أمس — اضغط لمتابعة القضاء",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = islamic.goldText
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = islamic.goldText
                                )
                            }
                        }
                    }
                }

                when (activeTab) {
                    TrackerTab.MONTHLY -> {
                        item(key = "month_navigator_card") {
                            MonthlyCalendarCard(
                                calendarData = monthCalendar,
                                selectedDateKey = selectedDayCell?.dateKey ?: selectedDateKey,
                                availableYears = availableYears,
                                yearMenuExpanded = yearMenuExpanded,
                                onToggleYearMenu = { yearMenuExpanded = it },
                                onSelectYear = { y ->
                                    viewingYear = y
                                    yearMenuExpanded = false
                                },
                                onPrevMonth = {
                                    if (viewingMonth == 0) {
                                        viewingMonth = 11
                                        viewingYear -= 1
                                    } else {
                                        viewingMonth -= 1
                                    }
                                },
                                onNextMonth = {
                                    if (viewingMonth == 11) {
                                        viewingMonth = 0
                                        viewingYear += 1
                                    } else {
                                        viewingMonth += 1
                                    }
                                },
                                onSelectDay = { cell ->
                                    if (cell.status != DayCompletionStatus.FUTURE) {
                                        performKeyTapHaptic(view)
                                        selectedDateKey = cell.dateKey
                                    }
                                }
                            )
                        }

                        if (selectedDayCell != null) {
                            item(key = "selected_day_fard") {
                                SelectedDayPrayerChecklistCard(
                                    cell = selectedDayCell,
                                    lockedPrayerKeys = if (selectedDayCell.isToday) lockedTodayPrayerKeys else emptySet(),
                                    onTogglePrayer = { pKey ->
                                        performKeyTapHaptic(view)
                                        onTogglePrayerForDate(selectedDayCell.dateKey, pKey)
                                    }
                                )
                            }

                            item(key = "optional_sunnah_habits") {
                                CollapsibleSunnahHabitsCard(
                                    cell = selectedDayCell,
                                    expanded = sunnahExpanded,
                                    onToggleExpanded = { sunnahExpanded = !sunnahExpanded },
                                    onToggleHabit = { habitKey ->
                                        performKeyTapHaptic(view)
                                        onToggleSunnahHabit(selectedDayCell.dateKey, habitKey)
                                    }
                                )
                            }
                        }

                        item(key = "auto_backup_note") {
                            ZeroLoginBackupNoticeCard()
                        }
                    }

                    TrackerTab.YEARLY -> {
                        item(key = "yearly_overview") {
                            YearlyOverviewCard(
                                overview = yearlyOverview,
                                onPrevYear = { viewingYear -= 1 },
                                onNextYear = { viewingYear += 1 },
                                onOpenMonth = { monthIdx ->
                                    viewingMonth = monthIdx
                                    activeTab = TrackerTab.MONTHLY
                                }
                            )
                        }
                    }

                    TrackerTab.QADAA -> {
                        if (pendingQadaaItems.isEmpty()) {
                            item(key = "qadaa_empty") {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, islamic.border)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "لا توجد صلوات فائتة للقضاء",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "ما شاء الله! جميع صلوات الفرض المسجّلة مكتملة. عند نسيان أي فرض من الأيام السابقة سيظهر هنا تلقائياً لتذكيرك بقضائه.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = islamic.textMuted,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            item(key = "qadaa_header_info") {
                                Text(
                                    text = "صلوات فائتة (للقضاء) — ${pendingQadaaItems.size} صلاة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            items(
                                items = pendingQadaaItems,
                                key = { "${it.dateKey}_${it.prayerKey}" }
                            ) { item ->
                                QadaaRowCard(
                                    item = item,
                                    onMarkDone = {
                                        performKeyTapHaptic(view)
                                        onMarkQadaaDone(item.dateKey, item.prayerKey)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightWarn: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlightWarn) Color(0xFF92400E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.14f),
        border = BorderStroke(
            1.dp,
            if (highlightWarn) SoftGold.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.22f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlightWarn) SoftGold else Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun MonthlyCalendarCard(
    calendarData: MonthCalendarData,
    selectedDateKey: String,
    availableYears: List<Int>,
    yearMenuExpanded: Boolean,
    onToggleYearMenu: (Boolean) -> Unit,
    onSelectYear: (Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDay: (MonthDayCell) -> Unit
) {
    val islamic = LocalIslamicColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Month & Year Navigator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("tracker_prev_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "الشهر السابق",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onToggleYearMenu(true) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = calendarData.monthLabelArabic,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "اختيار السنة",
                            tint = islamic.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = yearMenuExpanded,
                        onDismissRequest = { onToggleYearMenu(false) }
                    ) {
                        availableYears.forEach { yr ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "$yr",
                                        fontWeight = if (yr == calendarData.year) FontWeight.ExtraBold else FontWeight.Normal,
                                        color = if (yr == calendarData.year) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = { onSelectYear(yr) }
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("tracker_next_month_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "الشهر التالي",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Monthly Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إنجاز الشهر: ${calendarData.completionPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${calendarData.perfectDaysCount} يوم مكتمل (5/5)",
                    style = MaterialTheme.typography.labelSmall,
                    color = islamic.textMuted
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (calendarData.completionPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = islamic.emeraldSoft
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                calendarWeekdayHeaders.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = islamic.textMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 7-Column Calendar Grid
            val leadingBlanks = calendarData.firstDayOffsetFromSat
            val totalSlots = leadingBlanks + calendarData.days.size
            val rows = (totalSlots + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        for (c in 0 until 7) {
                            val idx = r * 7 + c - leadingBlanks
                            val cell = calendarData.days.getOrNull(idx)
                            if (cell == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val isSelected = cell.dateKey == selectedDateKey
                                val (bgColor, textColor) = when (cell.status) {
                                    DayCompletionStatus.FULL -> Color(0xFF15803D) to Color.White
                                    DayCompletionStatus.PARTIAL -> {
                                        if (cell.completedCount > 0) Color(0xFFD97706).copy(alpha = 0.22f) to Color(0xFFB45309)
                                        else islamic.cardAlt to MaterialTheme.colorScheme.onSurface
                                    }
                                    DayCompletionStatus.MISSED -> Color(0xFFDC2626).copy(alpha = 0.16f) to Color(0xFFB91C1C)
                                    DayCompletionStatus.FUTURE -> Color.Transparent to islamic.textMuted.copy(alpha = 0.45f)
                                    DayCompletionStatus.BEFORE_TRACKING -> islamic.cardAlt.copy(alpha = 0.45f) to islamic.textMuted
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = bgColor,
                                    border = when {
                                        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        cell.isToday -> BorderStroke(1.2.dp, islamic.gold)
                                        else -> null
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = cell.status != DayCompletionStatus.FUTURE) {
                                            onSelectDay(cell)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "${cell.dayOfMonth}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected || cell.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = textColor
                                        )
                                        if (cell.status != DayCompletionStatus.FUTURE && cell.completedCount > 0) {
                                            Text(
                                                text = "${cell.completedCount}/5",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (cell.status == DayCompletionStatus.FULL) {
                                                    Color.White.copy(alpha = 0.9f)
                                                } else {
                                                    textColor
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = Color(0xFF15803D), label = "5/5 مكتمل")
                LegendDot(color = Color(0xFFD97706), label = "جزئي")
                LegendDot(color = Color(0xFFDC2626), label = "فائتة/قضاء")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val islamic = LocalIslamicColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = islamic.textMuted
        )
    }
}

@Composable
private fun SelectedDayPrayerChecklistCard(
    cell: MonthDayCell,
    lockedPrayerKeys: Set<String> = emptySet(),
    onTogglePrayer: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    val cal = remember(cell.dateKey) { AstronomicalPrayerCalculator.parseDateKey(cell.dateKey) }
    val gregLabel = remember(cell.dateKey) { AstronomicalPrayerCalculator.formatGregorianArabic(cal) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (cell.isToday) "صلوات اليوم" else "صلوات يوم $gregLabel",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "اضغط على أي صلاة لتحديث سجلها مباشرة",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (cell.completedCount >= 5) Color(0xFFDCFCE7) else islamic.emeraldSoft
                ) {
                    Text(
                        text = "${cell.completedCount}/5",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (cell.completedCount >= 5) Color(0xFF15803D) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PrayerLogStore.prayerOrder.forEach { (pKey, pArabic) ->
                    val isLocked = lockedPrayerKeys.contains(pKey)
                    val done = !isLocked && (cell.performedKeys.contains(pKey) || cell.madeUpKeys.contains(pKey))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (done) Color(0xFF15803D) else islamic.cardAlt,
                        border = BorderStroke(
                            1.dp,
                            if (done) Color(0xFF15803D) else islamic.border
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .alpha(if (isLocked) 0.55f else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTogglePrayer(pKey) }
                            .testTag("tracker_day_prayer_${cell.dateKey}_$pKey")
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.Check,
                                contentDescription = if (isLocked) "لم يدخل وقت $pArabic بعد" else pArabic,
                                tint = if (done) Color.White else islamic.textMuted.copy(alpha = 0.55f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = pArabic,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (done) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSunnahHabitsCard(
    cell: MonthDayCell,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleHabit: (String) -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, islamic.border)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExpanded),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سنن وعبادات يومية (اختياري)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${cell.sunnahDoneKeys.size}/${PrayerLogStore.sunnahHabits.size} مكتمل • لا تُضاف لقائمة القضاء",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "توسيع السنن اليومية",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrayerLogStore.sunnahHabits.forEach { habit ->
                        val checked = cell.sunnahDoneKeys.contains(habit.key)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (checked) islamic.emeraldSoft else islamic.cardAlt,
                            border = BorderStroke(
                                1.dp,
                                if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else islamic.border
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleHabit(habit.key) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = habit.arabicLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (checked) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
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

@Composable
private fun YearlyOverviewCard(
    overview: YearlyOverviewData,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onOpenMonth: (Int) -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                IconButton(onClick = onPrevYear) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "السنة السابقة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ملخص عام ${overview.year}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نسبة الالتزام السنوي: ${overview.annualPercent}% • ${overview.perfectDaysCount} يوم مكتمل",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onNextYear) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "السنة التالية",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                overview.months.forEach { m ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = islamic.cardAlt,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenMonth(m.monthZeroBased) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = m.monthNameArabic,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (m.trackablePrayers > 0) {
                                        "${m.percent}% (${m.completedPrayers}/${m.trackablePrayers})"
                                    } else {
                                        "—"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (m.percent >= 80) Color(0xFF15803D) else islamic.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (m.percent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = when {
                                    m.percent >= 85 -> Color(0xFF15803D)
                                    m.percent >= 50 -> Color(0xFFD97706)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = islamic.border.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QadaaRowCard(
    item: QadaaItem,
    onMarkDone: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (item.isYesterday) islamic.gold.copy(alpha = 0.7f) else islamic.border
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFB91C1C),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "${item.prayerArabicName} - ${item.friendlyDateArabic}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "صلاة فرض فائتة بانتظار القضاء",
                        style = MaterialTheme.typography.labelSmall,
                        color = islamic.textMuted
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onMarkDone,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF15803D),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("qadaa_done_${item.dateKey}_${item.prayerKey}")
            ) {
                Text(
                    text = "تم القضاء ✓",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ZeroLoginBackupNoticeCard() {
    val islamic = LocalIslamicColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = islamic.emeraldSoft.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "النسخ الاحتياطي التلقائي لنظام أندرويد مفعّل بدون تسجيل دخول، ويمكنك التصدير أو الاستيراد يدوياً من «المزيد».",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
