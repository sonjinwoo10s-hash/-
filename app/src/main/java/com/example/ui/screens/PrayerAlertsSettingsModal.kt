package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerScheduleStore
import com.example.data.model.AdhanPlaybackStatus
import com.example.data.model.AdhanPlayerState
import com.example.data.model.PrayerAlertCategory
import com.example.data.model.PrayerAlertSound
import com.example.data.model.PrayerAlertsSettings
import com.example.data.repository.PrayerAlertRepository
import com.example.ui.components.performKeyTapHaptic
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.NightReadGold
import com.example.ui.theme.SoftGold

@Composable
fun PrayerAlertsSettingsModal(
    settings: PrayerAlertsSettings,
    alertSoundsCatalog: List<PrayerAlertSound>,
    isSyncingSounds: Boolean,
    cachedAlertIds: Set<String>,
    downloadingAlertIds: Set<String>,
    adhanState: AdhanPlayerState,
    autoSilentEnabled: Boolean,
    autoSilentAtIqama: Boolean,
    autoSilentDurationMinutes: Int,
    countdownNotificationEnabled: Boolean,
    fridayKahfReminderEnabled: Boolean,
    onClose: () -> Unit,
    onTogglePreAdhan: (Boolean) -> Unit,
    onSelectPreAdhanMinutes: (Int) -> Unit,
    onSelectPreAdhanSound: (PrayerAlertSound) -> Unit,
    onToggleIqama: (Boolean) -> Unit,
    onChangeIqamaMinutes: (String, Int) -> Unit,
    onResetIqamaDefaults: () -> Unit,
    onSelectIqamaSound: (PrayerAlertSound) -> Unit,
    onToggleAutoSilent: (Boolean) -> Unit,
    onToggleAutoSilentAtIqama: (Boolean) -> Unit,
    onSelectAutoSilentDuration: (Int) -> Unit,
    onToggleCountdownNotification: (Boolean) -> Unit,
    onToggleFridayKahfReminder: (Boolean) -> Unit,
    onSyncSounds: () -> Unit,
    onDownloadSoundOffline: (PrayerAlertSound) -> Unit,
    onTogglePreviewSound: (PrayerAlertSound) -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler { onClose() }

    val context = LocalContext.current
    val view = LocalView.current
    val islamic = LocalIslamicColors.current
    var pickerTarget by remember { mutableStateOf<PrayerAlertCategory?>(null) }
    var durationMenuExpanded by remember { mutableStateOf(false) }
    var iqamaMinutesExpanded by remember { mutableStateOf(false) }
    var autoSilentDurationExpanded by remember { mutableStateOf(false) }
    var autoSilentTriggerExpanded by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onToggleCountdownNotification(true)
        } else {
            onShowToast("يرجى السماح بإذن الإشعارات لعرض إشعار الصلاة القادمة الدائم")
        }
    }

    val iqamaSummary = remember(settings.iqamaMinutesByPrayer) {
        PrayerScheduleStore.prayerOrder.joinToString(" • ") { (key, arName) ->
            "$arName ${settings.iqamaMinutesFor(key)}د"
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("prayer_alerts_settings_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Minimal Emerald Top Bar with Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            performKeyTapHaptic(view)
                            onClose()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("prayer_alerts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنبيهات قبل وبعد الأذان",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "إعدادات هادئة ومرتّبة للتنبيه قبل الصلاة والإقامة",
                            style = MaterialTheme.typography.labelSmall,
                            color = NightReadGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CARD 1: Pre-Adhan (قبل الأذان) — 1 Toggle + 1 Compact Duration Row + 1 Compact Sound Picker Row
                item(key = "pre_adhan_section") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pre_adhan_settings_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // 1) Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(islamic.emeraldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "قبل الأذان",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = settings.preAdhanEnabled,
                                    onCheckedChange = { enabled ->
                                        performKeyTapHaptic(view)
                                        onTogglePreAdhan(enabled)
                                    },
                                    modifier = Modifier.testTag("pre_adhan_enabled_switch")
                                )
                            }

                            AnimatedVisibility(visible = settings.preAdhanEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = islamic.border
                                    )

                                    // 2) Single Compact Duration Row (with clean dropdown menu)
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        CompactSettingRow(
                                            icon = Icons.Default.Timer,
                                            title = "مدة التنبيه قبل الأذان",
                                            valueText = formatArabicMinutes(settings.preAdhanMinutes),
                                            trailingIcon = Icons.Default.KeyboardArrowDown,
                                            testTagName = "pre_adhan_duration_row",
                                            onClick = {
                                                performKeyTapHaptic(view)
                                                durationMenuExpanded = true
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = durationMenuExpanded,
                                            onDismissRequest = { durationMenuExpanded = false }
                                        ) {
                                            PrayerAlertsSettings.PRE_ADHAN_MINUTE_OPTIONS.forEach { minutes ->
                                                val selected = settings.preAdhanMinutes == minutes
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = formatArabicMinutes(minutes),
                                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                                            color = if (selected) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.onSurface
                                                            }
                                                        )
                                                    },
                                                    trailingIcon = if (selected) {
                                                        {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    } else {
                                                        null
                                                    },
                                                    onClick = {
                                                        durationMenuExpanded = false
                                                        performKeyTapHaptic(view)
                                                        onSelectPreAdhanMinutes(minutes)
                                                    },
                                                    modifier = Modifier.testTag("pre_adhan_min_$minutes")
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = islamic.border
                                    )

                                    // 3) Single Compact Sound Picker Row
                                    CompactSettingRow(
                                        icon = Icons.Default.VolumeUp,
                                        title = "صوت التنبيه قبل الأذان",
                                        valueText = settings.preAdhanSoundName,
                                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                                        testTagName = "pick_pre_adhan_sound_button",
                                        onClick = {
                                            performKeyTapHaptic(view)
                                            pickerTarget = PrayerAlertCategory.PRE_ADHAN
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // CARD 2: Iqama (الإقامة بعد الأذان) — 1 Toggle + 1 Sound Picker Row + 1 Collapsible Minutes Row
                item(key = "iqama_section") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("iqama_settings_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // 1) Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(islamic.goldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = islamic.goldText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "الإقامة (بعد الأذان)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Switch(
                                    checked = settings.iqamaEnabled,
                                    onCheckedChange = { enabled ->
                                        performKeyTapHaptic(view)
                                        onToggleIqama(enabled)
                                    },
                                    modifier = Modifier.testTag("iqama_enabled_switch")
                                )
                            }

                            AnimatedVisibility(visible = settings.iqamaEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = islamic.border
                                    )

                                    // 2) Single Compact Sound Picker Row
                                    CompactSettingRow(
                                        icon = Icons.Default.VolumeUp,
                                        title = "صوت الإقامة",
                                        valueText = settings.iqamaSoundName,
                                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                                        testTagName = "pick_iqama_sound_button",
                                        onClick = {
                                            performKeyTapHaptic(view)
                                            pickerTarget = PrayerAlertCategory.IQAMA
                                        }
                                    )

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = islamic.border
                                    )

                                    // 3) Collapsible Clean Row for Per-Prayer Iqama Minutes (collapsed by default)
                                    CompactSettingRow(
                                        icon = Icons.Default.Timer,
                                        title = "أوقات الإقامة لكل صلاة",
                                        valueText = iqamaSummary,
                                        trailingIcon = if (iqamaMinutesExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        testTagName = "iqama_minutes_expand_row",
                                        onClick = {
                                            performKeyTapHaptic(view)
                                            iqamaMinutesExpanded = !iqamaMinutesExpanded
                                        }
                                    )

                                    AnimatedVisibility(visible = iqamaMinutesExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = islamic.cardAlt,
                                                border = BorderStroke(1.dp, islamic.border),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    PrayerScheduleStore.prayerOrder.forEachIndexed { idx, (key, arabicName) ->
                                                        val minutes = settings.iqamaMinutesFor(key)
                                                        if (idx > 0) {
                                                            HorizontalDivider(
                                                                color = islamic.border,
                                                                modifier = Modifier.padding(horizontal = 12.dp)
                                                            )
                                                        }
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "صلاة $arabicName",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        performKeyTapHaptic(view)
                                                                        onChangeIqamaMinutes(
                                                                            key,
                                                                            (minutes - 1).coerceAtLeast(
                                                                                PrayerAlertsSettings.MIN_IQAMA_MINUTES
                                                                            )
                                                                        )
                                                                    },
                                                                    enabled = minutes > PrayerAlertsSettings.MIN_IQAMA_MINUTES,
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("iqama_dec_$key")
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .clip(CircleShape)
                                                                            .background(MaterialTheme.colorScheme.surface)
                                                                            .border(1.dp, islamic.border, CircleShape),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Remove,
                                                                            contentDescription = "إنقاص دقائق إقامة $arabicName",
                                                                            tint = MaterialTheme.colorScheme.onSurface,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }

                                                                Text(
                                                                    text = "$minutes د",
                                                                    style = MaterialTheme.typography.labelLarge,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                                )

                                                                IconButton(
                                                                    onClick = {
                                                                        performKeyTapHaptic(view)
                                                                        onChangeIqamaMinutes(
                                                                            key,
                                                                            (minutes + 1).coerceAtMost(
                                                                                PrayerAlertsSettings.MAX_IQAMA_MINUTES
                                                                            )
                                                                        )
                                                                    },
                                                                    enabled = minutes < PrayerAlertsSettings.MAX_IQAMA_MINUTES,
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .testTag("iqama_inc_$key")
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(32.dp)
                                                                            .clip(CircleShape)
                                                                            .background(MaterialTheme.colorScheme.surface)
                                                                            .border(1.dp, islamic.border, CircleShape),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Add,
                                                                            contentDescription = "زيادة دقائق إقامة $arabicName",
                                                                            tint = MaterialTheme.colorScheme.primary,
                                                                            modifier = Modifier.size(16.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        performKeyTapHaptic(view)
                                                        onResetIqamaDefaults()
                                                    },
                                                    modifier = Modifier.testTag("iqama_reset_defaults_button")
                                                ) {
                                                    Text(
                                                        text = "استعادة الافتراضي",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold
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

                // CARD 3: Smart Prayer Extras (Auto-Silent DND, Persistent Countdown, Friday Al-Kahf)
                item(key = "extra_prayer_alerts_section") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("extra_prayer_alerts_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, islamic.border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            // 1) Auto-Silent Mode During Prayer (OFF by default; requests DND ONLY when toggled ON)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(islamic.emeraldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DoNotDisturbOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "الوضع الصامت وقت الصلاة",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "تفعيل الصامت تلقائياً ثم إعادة الصوت بعد الصلاة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = autoSilentEnabled,
                                    onCheckedChange = { wantEnabled ->
                                        performKeyTapHaptic(view)
                                        if (wantEnabled && !PrayerScheduleStore.isDndPermissionGranted(context)) {
                                            onShowToast("يرجى تفعيل إذن «عدم الإزعاج» لرفيق المسلم ثم العودة لتشغيل الوضع الصامت")
                                            runCatching {
                                                val dndIntent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(dndIntent)
                                            }
                                        } else {
                                            onToggleAutoSilent(wantEnabled)
                                        }
                                    },
                                    modifier = Modifier.testTag("auto_silent_enabled_switch")
                                )
                            }

                            AnimatedVisibility(visible = autoSilentEnabled) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = islamic.border
                                    )
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        CompactSettingRow(
                                            icon = Icons.Default.Schedule,
                                            title = "وقت بدء الصامت",
                                            valueText = if (autoSilentAtIqama) "عند الإقامة" else "عند الأذان",
                                            trailingIcon = Icons.Default.KeyboardArrowDown,
                                            testTagName = "auto_silent_trigger_row",
                                            onClick = {
                                                performKeyTapHaptic(view)
                                                autoSilentTriggerExpanded = true
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = autoSilentTriggerExpanded,
                                            onDismissRequest = { autoSilentTriggerExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("عند الإقامة", fontWeight = if (autoSilentAtIqama) FontWeight.ExtraBold else FontWeight.Normal) },
                                                onClick = {
                                                    autoSilentTriggerExpanded = false
                                                    onToggleAutoSilentAtIqama(true)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("عند الأذان", fontWeight = if (!autoSilentAtIqama) FontWeight.ExtraBold else FontWeight.Normal) },
                                                onClick = {
                                                    autoSilentTriggerExpanded = false
                                                    onToggleAutoSilentAtIqama(false)
                                                }
                                            )
                                        }
                                    }

                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = islamic.border
                                    )
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        CompactSettingRow(
                                            icon = Icons.Default.Timer,
                                            title = "مدة الوضع الصامت",
                                            valueText = formatArabicMinutes(autoSilentDurationMinutes),
                                            trailingIcon = Icons.Default.KeyboardArrowDown,
                                            testTagName = "auto_silent_duration_row",
                                            onClick = {
                                                performKeyTapHaptic(view)
                                                autoSilentDurationExpanded = true
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = autoSilentDurationExpanded,
                                            onDismissRequest = { autoSilentDurationExpanded = false }
                                        ) {
                                            listOf(10, 15, 20, 30).forEach { mins ->
                                                val sel = autoSilentDurationMinutes == mins
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = formatArabicMinutes(mins),
                                                            fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.Normal,
                                                            color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    },
                                                    onClick = {
                                                        autoSilentDurationExpanded = false
                                                        onSelectAutoSilentDuration(mins)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = islamic.border
                            )

                            // 2) Persistent Next-Prayer Countdown Notification (OFF by default)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(islamic.goldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsNone,
                                            contentDescription = null,
                                            tint = islamic.goldText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "إشعار الصلاة القادمة الدائم",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "عدّاد صامت ومستمر في شريط الإشعارات",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = countdownNotificationEnabled,
                                    onCheckedChange = { wantEnabled ->
                                        performKeyTapHaptic(view)
                                        if (wantEnabled &&
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            !PermissionsHelper.isNotificationGranted(context)
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            onToggleCountdownNotification(wantEnabled)
                                        }
                                    },
                                    modifier = Modifier.testTag("settings_countdown_notification_switch")
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = islamic.border
                            )

                            // 3) Friday Surah Al-Kahf Reminder (ON by default)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                            .background(islamic.emeraldSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "تذكير سورة الكهف يوم الجمعة",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "تذكير أسبوعي كل جمعة الساعة 9:00 صباحاً",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = islamic.textMuted
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Switch(
                                    checked = fridayKahfReminderEnabled,
                                    onCheckedChange = { enabled ->
                                        performKeyTapHaptic(view)
                                        onToggleFridayKahfReminder(enabled)
                                    },
                                    modifier = Modifier.testTag("friday_kahf_reminder_switch")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Voice Picker Dialog backed by alerts.json
    val activePicker = pickerTarget
    if (activePicker != null) {
        val isIqamaPicker = activePicker == PrayerAlertCategory.IQAMA
        val voices = remember(alertSoundsCatalog, activePicker) {
            PrayerAlertRepository.soundsForCategory(alertSoundsCatalog, activePicker)
        }
        AlertVoicePickerDialog(
            title = if (isIqamaPicker) "صوت الإقامة" else "صوت التنبيه قبل الأذان",
            voices = voices,
            selectedSoundId = if (isIqamaPicker) settings.iqamaSoundId else settings.preAdhanSoundId,
            isSyncing = isSyncingSounds,
            cachedAlertIds = cachedAlertIds,
            downloadingAlertIds = downloadingAlertIds,
            adhanState = adhanState,
            onRefreshCatalog = onSyncSounds,
            onTogglePreview = onTogglePreviewSound,
            onDownloadOffline = onDownloadSoundOffline,
            onSelectVoice = { sound ->
                if (isIqamaPicker) onSelectIqamaSound(sound) else onSelectPreAdhanSound(sound)
                pickerTarget = null
            },
            onDismiss = { pickerTarget = null }
        )
    }
}

@Composable
private fun CompactSettingRow(
    icon: ImageVector,
    title: String,
    valueText: String,
    trailingIcon: ImageVector,
    testTagName: String,
    onClick: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTagName),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelSmall,
                    color = islamic.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = trailingIcon,
            contentDescription = title,
            tint = islamic.textMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun formatArabicMinutes(minutes: Int): String = when (minutes) {
    in 3..10 -> "$minutes دقائق"
    else -> "$minutes دقيقة"
}

@Composable
private fun AlertVoicePickerDialog(
    title: String,
    voices: List<PrayerAlertSound>,
    selectedSoundId: String,
    isSyncing: Boolean,
    cachedAlertIds: Set<String>,
    downloadingAlertIds: Set<String>,
    adhanState: AdhanPlayerState,
    onRefreshCatalog: () -> Unit,
    onTogglePreview: (PrayerAlertSound) -> Unit,
    onDownloadOffline: (PrayerAlertSound) -> Unit,
    onSelectVoice: (PrayerAlertSound) -> Unit,
    onDismiss: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val view = LocalView.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, islamic.gold.copy(alpha = 0.55f)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .widthIn(max = 420.dp)
                    .heightIn(max = 560.dp)
                    .testTag("alert_voice_picker_dialog")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = NightReadGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "اختر الصوت واضغط للمعاينة أو الحفظ أوفلاين",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NightReadGold
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    performKeyTapHaptic(view)
                                    onRefreshCatalog()
                                },
                                enabled = !isSyncing,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("refresh_alerts_catalog_button")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = NightReadGold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "تحديث قائمة الأصوات",
                                        tint = NightReadGold
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_voice_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Voices list
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(voices, key = { it.id }) { sound ->
                            val isSelected = sound.id == selectedSoundId
                            val isThisActive = adhanState.activeMuezzinId == sound.id
                            val isLoading = isThisActive && adhanState.status == AdhanPlaybackStatus.LOADING
                            val isPlaying = isThisActive && (
                                adhanState.status == AdhanPlaybackStatus.PLAYING ||
                                    adhanState.status == AdhanPlaybackStatus.FALLBACK_PLAYING
                                )
                            val isCached = sound.isBuiltInFallback ||
                                cachedAlertIds.contains(PrayerAlertRepository.sanitizeId(sound.id))
                            val isDownloading = downloadingAlertIds.contains(sound.id)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) islamic.emeraldSoft else islamic.cardAlt,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else islamic.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("alert_voice_item_${sound.id}")
                                    .clickable {
                                        performKeyTapHaptic(view)
                                        onSelectVoice(sound)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play/Stop preview button (uses preview_url for per_prayer packs)
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlaying) SoftGold else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                1.dp,
                                                if (isPlaying) SoftGold else islamic.border,
                                                CircleShape
                                            )
                                            .testTag("preview_alert_voice_${sound.id}")
                                            .clickable {
                                                performKeyTapHaptic(view)
                                                onTogglePreview(sound)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when {
                                            isLoading -> CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            isPlaying -> Icon(
                                                imageVector = Icons.Default.Stop,
                                                contentDescription = "إيقاف المعاينة",
                                                tint = MaterialTheme.colorScheme.onSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            else -> Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "معاينة الصوت",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sound.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (sound.subtitle.isNotBlank()) {
                                            Text(
                                                text = sound.subtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = islamic.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = when {
                                                sound.isBuiltInFallback -> "مضمّن • يعمل أوفلاين"
                                                isCached && sound.isPerPrayer -> "محفوظ أوفلاين (الصلوات الخمس)"
                                                isCached -> "محفوظ أوفلاين"
                                                sound.isPerPrayer -> "حزمة 5 صلوات (تُحفظ تلقائياً عند الاختيار)"
                                                else -> "صوت سحابي (يُحفظ تلقائياً عند الاختيار)"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCached) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                islamic.goldText
                                            }
                                        )
                                    }

                                    if (!sound.isBuiltInFallback && !isCached) {
                                        IconButton(
                                            onClick = {
                                                performKeyTapHaptic(view)
                                                onDownloadOffline(sound)
                                            },
                                            enabled = !isDownloading,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            if (isDownloading) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.CloudDownload,
                                                    contentDescription = "حفظ أوفلاين",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "الصوت المحدد",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(16.dp)
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
}
