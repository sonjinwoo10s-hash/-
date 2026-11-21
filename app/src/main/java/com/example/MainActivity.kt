package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alarm.OnboardingPrefs
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerScheduleStore
import com.example.security.AppIntegrityGuard
import com.example.security.SecureConfigVault
import com.example.service.AdhanPlaybackService
import com.example.service.PrayerCountdownService
import com.example.ui.RafiqApp
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.RafiqAlMuslimTheme
import com.example.ui.viewmodel.RafiqViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: RafiqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Release-only anti-tamper & signature integrity block
        if (AppIntegrityGuard.isTampered) {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            )
            setContent {
                RafiqAlMuslimTheme(darkTheme = true) {
                    TamperBlockedScreen()
                }
            }
            return
        }

        if (!OnboardingPrefs.isCompleted(this)) {
            startActivity(Intent(this, PermissionsOnboardingActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            RafiqApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!AppIntegrityGuard.isTampered && OnboardingPrefs.isCompleted(this)) {
            PrayerAlarmScheduler.scheduleAllPrayers(this)
            PrayerCountdownService.syncState(this)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) &&
            AdhanPlaybackService.isActivelyPlayingOrLoading() &&
            PrayerScheduleStore.isAdhanSilenceOnVolumeEnabled(this)
        ) {
            AdhanPlaybackService.sendControlAction(this, AdhanPlaybackService.ACTION_STOP_ADHAN)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

@Composable
private fun TamperBlockedScreen() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "تحذير أمان",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Text(
                    text = "تنبيه أمني",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = AppIntegrityGuard.TAMPER_BLOCK_MESSAGE_AR,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(SecureConfigVault.officialWebsiteUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "تحميل النسخة الرسمية",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}
