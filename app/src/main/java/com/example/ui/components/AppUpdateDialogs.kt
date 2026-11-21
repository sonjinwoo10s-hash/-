package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.LocalIslamicColors
import com.example.update.AppReleaseInfo
import com.example.update.AppUpdateManager
import java.io.File

enum class AppUpdateDialogStage {
    NONE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    INSTALL_PERMISSION_REQUIRED,
    READY_TO_INSTALL,
    ERROR
}

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val stage: AppUpdateDialogStage = AppUpdateDialogStage.NONE,
    val availableRelease: AppReleaseInfo? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = -1L,
    val progressPercent: Int = 0,
    val downloadedApkFile: File? = null,
    val errorMessage: String? = null
)

@Composable
fun AppUpdateOverlayHost(
    state: AppUpdateUiState,
    onStartDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
    onInstallReadyApk: (File) -> Unit,
    onPermissionRequired: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val islamic = LocalIslamicColors.current

    // Launcher for Android 8+ "Allow from this source" Settings screen
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val apkFile = state.downloadedApkFile
        if (apkFile != null && apkFile.exists() && AppUpdateManager.canInstallPackages(context)) {
            onInstallReadyApk(apkFile)
        } else if (!AppUpdateManager.canInstallPackages(context)) {
            onShowToast("يرجى تفعيل خيار السماح بتثبيت التطبيقات لإكمال التحديث")
        }
    }

    // Automatically trigger installer or ask for install permission the moment download finishes
    LaunchedEffect(state.stage, state.downloadedApkFile) {
        if (state.stage == AppUpdateDialogStage.READY_TO_INSTALL) {
            val apkFile = state.downloadedApkFile
            if (apkFile != null && apkFile.exists()) {
                if (AppUpdateManager.canInstallPackages(context)) {
                    onInstallReadyApk(apkFile)
                } else {
                    onPermissionRequired()
                }
            }
        }
    }

    when (state.stage) {
        AppUpdateDialogStage.NONE,
        AppUpdateDialogStage.READY_TO_INSTALL -> Unit

        AppUpdateDialogStage.UPDATE_AVAILABLE -> {
            val release = state.availableRelease ?: return
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(islamic.emeraldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "تحديث جديد متاح",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "تحديث جديد متاح (الإصدار ${release.versionName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ما الجديد في هذا الإصدار:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = islamic.cardAlt,
                            border = BorderStroke(1.dp, islamic.border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp, max = 220.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = release.releaseNotes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        if (release.assetSizeBytes > 0L) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "حجم التحديث: ${AppUpdateManager.formatMegabytes(release.assetSizeBytes)} ميجابايت",
                                style = MaterialTheme.typography.labelSmall,
                                color = islamic.textMuted
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onStartDownload,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("update_now_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تحديث الآن",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("update_later_button")
                    ) {
                        Text(
                            text = "لاحقاً",
                            fontWeight = FontWeight.Bold,
                            color = islamic.textMuted
                        )
                    }
                }
            )
        }

        AppUpdateDialogStage.DOWNLOADING -> {
            val hasTotal = state.totalBytes > 0L
            val progressTitle = if (hasTotal) {
                "جاري تحميل التحديث... ${state.progressPercent}%"
            } else {
                "جاري تحميل التحديث..."
            }
            val sizeLabel = if (hasTotal) {
                "${AppUpdateManager.formatMegabytes(state.downloadedBytes)} / ${AppUpdateManager.formatMegabytes(state.totalBytes)} ميجابايت"
            } else {
                "${AppUpdateManager.formatMegabytes(state.downloadedBytes)} ميجابايت"
            }

            AlertDialog(
                onDismissRequest = { /* Keep open during download; use Cancel button */ },
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(islamic.emeraldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "جاري تحميل التحديث",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = progressTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_download_progress_title")
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasTotal) {
                            LinearProgressIndicator(
                                progress = { (state.progressPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = islamic.emeraldSoft
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = islamic.emeraldSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sizeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = islamic.textMuted
                            )
                            if (hasTotal) {
                                Text(
                                    text = "${state.progressPercent}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "سيبدأ التثبيت تلقائياً فور اكتمال التحميل",
                            style = MaterialTheme.typography.labelSmall,
                            color = islamic.textMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = onCancelDownload,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("cancel_update_download_button")
                    ) {
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        AppUpdateDialogStage.INSTALL_PERMISSION_REQUIRED -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(islamic.goldSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "إذن تثبيت التحديث",
                            tint = islamic.goldText,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "السماح بتثبيت التحديث",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "تم تحميل التحديث بنجاح. لتثبيت الإصدار الجديد مباشرة من داخل «رفيق المسلم»، يرجى تفعيل خيار «السماح من هذا المصدر» لمرة واحدة فقط، وسيبدأ التثبيت تلقائياً فور عودتك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            runCatching {
                                unknownSourcesLauncher.launch(
                                    AppUpdateManager.buildUnknownSourcesSettingsIntent(context)
                                )
                            }.onFailure {
                                onShowToast("تعذّر فتح إعدادات التثبيت على هذا الجهاز")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("grant_install_permission_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("السماح والمتابعة", fontWeight = FontWeight.ExtraBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("لاحقاً", fontWeight = FontWeight.Bold, color = islamic.textMuted)
                    }
                }
            )
        }

        AppUpdateDialogStage.ERROR -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = "خطأ في التحديث",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "تعذّر إكمال التحديث",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = state.errorMessage ?: "حدث خطأ أثناء تحميل ملف التحديث، يرجى المحاولة مرة أخرى.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onStartDownload,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("retry_update_download_button")
                    ) {
                        Text("إعادة المحاولة", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق", color = islamic.textMuted)
                    }
                }
            )
        }
    }
}
