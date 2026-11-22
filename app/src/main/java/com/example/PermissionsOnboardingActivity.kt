package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alarm.AppPermissionsStatus
import com.example.alarm.OnboardingPrefs
import com.example.alarm.PermissionsHelper
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.local.AppPreferences
import com.example.service.PrayerCountdownService
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.RafiqAlMuslimTheme
import com.example.ui.theme.SoftGold
import kotlinx.coroutines.flow.MutableStateFlow

class PermissionsOnboardingActivity : ComponentActivity() {

    companion object {
        const val EXTRA_FROM_SETTINGS = "extra_from_settings"
    }

    private val permissionsState = MutableStateFlow(AppPermissionsStatus())
    private var openedFromSettings: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openedFromSettings = intent?.getBooleanExtra(EXTRA_FROM_SETTINGS, false) == true

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        refreshPermissions()

        setContent {
            val status by permissionsState.collectAsStateWithLifecycle()

            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                refreshPermissions()
                if (!granted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val canAskAgain = shouldShowRequestPermissionRationale(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    if (!canAskAgain) {
                        openAppNotificationSettings()
                    }
                }
            }

            val locationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                refreshPermissions()
                if (!PermissionsHelper.isLocationGranted(this)) {
                    val canAskFine = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
                    val canAskCoarse = shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (!canAskFine && !canAskCoarse) {
                        openAppDetailsSettings()
                    }
                }
            }

            val appPrefs = remember { AppPreferences(applicationContext) }
            val darkMode by appPrefs.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)

            RafiqAlMuslimTheme(darkTheme = darkMode) {
                PermissionsOnboardingScreen(
                    status = status,
                    openedFromSettings = openedFromSettings,
                    onRequestNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            refreshPermissions()
                        }
                    },
                    onRequestExactAlarms = { openExactAlarmSettings() },
                    onRequestIgnoreBattery = { openIgnoreBatteryOptimizationSettings() },
                    onRequestLocation = {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    onContinue = { finishOnboardingAndProceed() },
                    onSkip = { finishOnboardingAndProceed() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        val current = runCatching { PermissionsHelper.checkAll(this) }.getOrDefault(AppPermissionsStatus())
        permissionsState.value = current
        if (current.notificationsGranted) {
            runCatching { PrayerAlarmScheduler.scheduleAllPrayers(this) }
            runCatching { PrayerCountdownService.syncState(this) }
        }
    }

    private fun openAppNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
        }
        runCatching { startActivity(intent) }.onFailure { openAppDetailsSettings() }
    }

    private fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:$packageName")
            }
            runCatching { startActivity(intent) }.onFailure { openAppDetailsSettings() }
        } else {
            refreshPermissions()
        }
    }

    @SuppressLint("BatteryLife")
    private fun openIgnoreBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            val openedDirect = runCatching {
                startActivity(directIntent)
                true
            }.getOrDefault(false)

            if (!openedDirect) {
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                runCatching { startActivity(fallbackIntent) }.onFailure { openAppDetailsSettings() }
            }
        } else {
            refreshPermissions()
        }
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
    }

    private fun finishOnboardingAndProceed() {
        runCatching { OnboardingPrefs.setCompleted(this, true) }
        runCatching { PrayerAlarmScheduler.scheduleAllPrayers(this) }
        runCatching { PrayerCountdownService.syncState(this) }
        if (!openedFromSettings) {
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            runCatching { startActivity(mainIntent) }
        }
        finish()
    }
}

@Composable
private fun PermissionsOnboardingScreen(
    status: AppPermissionsStatus,
    openedFromSettings: Boolean,
    onRequestNotifications: () -> Unit,
    onRequestExactAlarms: () -> Unit,
    onRequestIgnoreBattery: () -> Unit,
    onRequestLocation: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val grantedCount = listOf(
        status.notificationsGranted,
        status.exactAlarmsGranted,
        status.batteryOptimizationIgnored,
        status.locationGranted
    ).count { it }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("permissions_onboarding_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Emerald Gradient Hero Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(DeepGreen, EmeraldGreen)))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(SoftGold.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "إعداد الأذونات",
                            tint = Color(0xFFF6DC97),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "إعداد الأذونات والتنبيهات",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "فعّل هذه الأذونات ليصدح الأذان في وقته بدقة ويعمل عدّاد الصلاة القادمة دون انقطاع",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, SoftGold.copy(alpha = 0.55f))
                    ) {
                        Text(
                            text = if (grantedCount == 4) {
                                "تم تفعيل جميع الأذونات بنجاح (4/4) ✓"
                            } else {
                                "تم تفعيل $grantedCount من 4 أذونات أساسية"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF6DC97),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4 Permission Cards
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "perm_notifications") {
                    PermissionSetupCard(
                        icon = Icons.Default.NotificationsActive,
                        title = "الإشعارات",
                        description = "لعرض إشعار الأذان عند دخول وقت الصلاة وإشعار الوقت المتبقي للصلاة القادمة.",
                        isGranted = status.notificationsGranted,
                        buttonTestTag = "perm_enable_notifications",
                        onEnableClick = onRequestNotifications
                    )
                }

                item(key = "perm_exact_alarms") {
                    PermissionSetupCard(
                        icon = Icons.Default.Alarm,
                        title = "المنبهات الدقيقة",
                        description = "لضبط مواقيت الصلوات الخمس بالثانية وتشغيل الأذان في وقته تماماً حتى عند قفل الشاشة.",
                        isGranted = status.exactAlarmsGranted,
                        buttonTestTag = "perm_enable_exact_alarms",
                        onEnableClick = onRequestExactAlarms
                    )
                }

                item(key = "perm_battery") {
                    PermissionSetupCard(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "تجاهل تحسين البطارية",
                        description = "لمنع نظام أندرويد من إيقاف الأذان أو تأخير التنبيهات عندما يكون التطبيق مغلقاً في الخلفية.",
                        isGranted = status.batteryOptimizationIgnored,
                        buttonTestTag = "perm_enable_battery",
                        onEnableClick = onRequestIgnoreBattery
                    )
                }

                item(key = "perm_location") {
                    PermissionSetupCard(
                        icon = Icons.Default.LocationOn,
                        title = "الموقع الجغرافي",
                        description = "لتحديد مواقيت الصلاة بدقة حسب مدينتك وحساب زاوية واتجاه القبلة الصحيح.",
                        isGranted = status.locationGranted,
                        buttonTestTag = "perm_enable_location",
                        onEnableClick = onRequestLocation
                    )
                }
            }

            // Bottom Action Footer
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, islamic.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onContinue,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("permissions_continue_button")
                    ) {
                        Text(
                            text = if (openedFromSettings) "حفظ والعودة للتطبيق" else "متابعة للتطبيق",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (!openedFromSettings) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("permissions_skip_button")
                        ) {
                            Text(
                                text = "تخطي الآن",
                                style = MaterialTheme.typography.labelLarge,
                                color = islamic.textMuted,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionSetupCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    buttonTestTag: String,
    onEnableClick: () -> Unit
) {
    val islamic = LocalIslamicColors.current
    val grantedBg = Color(0xFFDCFCE7)
    val grantedText = Color(0xFF15803D)
    val requiredBg = Color(0xFFFFEDD5)
    val requiredText = Color(0xFFC2410C)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isGranted) 1.5.dp else 1.dp,
            color = if (isGranted) grantedText.copy(alpha = 0.45f) else islamic.border
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isGranted) islamic.emeraldSoft else islamic.goldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isGranted) MaterialTheme.colorScheme.primary else islamic.goldText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isGranted) grantedBg else requiredBg
                        ) {
                            Text(
                                text = if (isGranted) "مفعّل ✓" else "مطلوب",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isGranted) grantedText else requiredText,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (isGranted) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "مفعّل",
                        tint = grantedText,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Button(
                        onClick = onEnableClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag(buttonTestTag)
                    ) {
                        Text(
                            text = "تفعيل",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = islamic.textMuted
            )
        }
    }
}
