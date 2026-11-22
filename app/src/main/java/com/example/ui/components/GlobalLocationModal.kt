package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.core.content.ContextCompat
import com.example.alarm.PrayerCalculationMethod
import com.example.data.repository.GlobalLocationConfig
import com.example.data.repository.GlobalSearchResult
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.LocalIslamicColors
import com.example.ui.theme.NightReadGold
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay

@Composable
fun GlobalLocationModal(
    currentLocation: GlobalLocationConfig,
    isDetectingLocation: Boolean,
    searchResults: List<GlobalSearchResult>,
    isSearching: Boolean,
    showCalculationMethodSection: Boolean,
    onGpsDetected: (Location) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSelectSearchResult: (GlobalSearchResult) -> Unit,
    onSelectCalculationMethod: (PrayerCalculationMethod?) -> Unit,
    onShowToast: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val islamic = LocalIslamicColors.current

    var query by remember { mutableStateOf("") }
    var methodMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        delay(380L)
        onSearchQueryChanged(query)
    }

    @SuppressLint("MissingPermission")
    fun requestDeviceLocation() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val cts = CancellationTokenSource()
        onShowToast("جاري تحديد موقعك العالمي بدقة...")
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    onGpsDetected(loc)
                } else {
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            onGpsDetected(last)
                        } else {
                            val lmFallback = fetchLocationManagerFallback(context)
                            if (lmFallback != null) {
                                onGpsDetected(lmFallback)
                            } else {
                                onShowToast("تعذّر الحصول على إحداثيات GPS، تأكد من تفعيل خدمة الموقع أو ابحث باسم مدينتك")
                            }
                        }
                    }.addOnFailureListener {
                        val lmFallback = fetchLocationManagerFallback(context)
                        if (lmFallback != null) {
                            onGpsDetected(lmFallback)
                        } else {
                            onShowToast("تعذّر الحصول على الموقع حالياً، يمكنك البحث باسم مدينتك")
                        }
                    }
                }
            }
            .addOnFailureListener {
                val lmFallback = fetchLocationManagerFallback(context)
                if (lmFallback != null) {
                    onGpsDetected(lmFallback)
                } else {
                    onShowToast("تعذّر الوصول لخدمة الموقع، يمكنك البحث باسم مدينتك")
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            requestDeviceLocation()
        } else {
            onShowToast("يمكنك البحث باسم أي مدينة في العالم أو منح صلاحية الموقع للتحديد التلقائي")
        }
    }

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
                    .widthIn(max = 440.dp)
                    .heightIn(max = 620.dp)
                    .testTag("global_location_modal")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(DeepGreen, EmeraldGreen)))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = NightReadGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "تحديد الموقع العالمي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = currentLocation.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NightReadGold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("close_global_location_modal")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.White
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // One-Tap Auto-Detect GPS Button
                        Button(
                            onClick = {
                                performKeyTapHaptic(view)
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                val coarseGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted || coarseGranted) {
                                    requestDeviceLocation()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isDetectingLocation,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 13.dp, horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auto_detect_gps_location_button")
                        ) {
                            if (isDetectingLocation) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "جاري تحديد الموقع وطريقة الحساب...",
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تحديد الموقع تلقائياً (GPS)",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // Global City Search Input (any city/town worldwide)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("global_city_search_input"),
                            placeholder = {
                                Text("ابحث عن أي مدينة أو قرية في العالم...")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "بحث",
                                    tint = islamic.textMuted
                                )
                            },
                            trailingIcon = {
                                when {
                                    isSearching -> CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    query.isNotEmpty() -> IconButton(
                                        onClick = { query = "" },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "مسح البحث",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = islamic.border
                            )
                        )

                        // Search Results List
                        if (query.trim().length >= 2) {
                            if (searchResults.isEmpty() && !isSearching) {
                                Text(
                                    text = "لم يتم العثور على نتائج مطابقة. جرّب كتابة اسم المدينة بالعربية أو الإنجليزية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = islamic.textMuted
                                )
                            } else if (searchResults.isNotEmpty()) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 220.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(searchResults, key = { "${it.lat}_${it.lng}_${it.displayLabel}" }) { item ->
                                        val autoMethod = PrayerCalculationMethod.forCountryCode(item.countryCode)
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = islamic.cardAlt,
                                            border = BorderStroke(1.dp, islamic.border),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    performKeyTapHaptic(view)
                                                    onSelectSearchResult(item)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.displayLabel,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "طريقة الحساب التلقائية: ${autoMethod.shortArabicName}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = islamic.textMuted,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Calculation Method Override Section (always shown when opened from Settings, compact info otherwise)
                        HorizontalDivider(color = islamic.border)

                        val resolvedMethod = currentLocation.resolvedMethod()
                        if (showCalculationMethodSection) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "طريقة حساب مواقيت الصلاة",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = islamic.cardAlt,
                                        border = BorderStroke(1.dp, islamic.border),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { methodMenuExpanded = true }
                                            .testTag("calculation_method_dropdown_button")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = resolvedMethod.arabicName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (currentLocation.isMethodManualOverride) {
                                                        "اختيار يدوي"
                                                    } else {
                                                        "تلقائي حسب الدولة (${currentLocation.countryCode.ifBlank { "عالمي" }})"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "اختيار طريقة الحساب",
                                                tint = islamic.textMuted
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = methodMenuExpanded,
                                        onDismissRequest = { methodMenuExpanded = false },
                                        modifier = Modifier.heightIn(max = 320.dp)
                                    ) {
                                        val autoCountryMethod = PrayerCalculationMethod.forCountryCode(currentLocation.countryCode)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "تلقائي حسب الدولة (${autoCountryMethod.shortArabicName})",
                                                    fontWeight = if (!currentLocation.isMethodManualOverride) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = if (!currentLocation.isMethodManualOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            },
                                            trailingIcon = if (!currentLocation.isMethodManualOverride) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            } else null,
                                            onClick = {
                                                methodMenuExpanded = false
                                                onSelectCalculationMethod(null)
                                            }
                                        )
                                        HorizontalDivider(color = islamic.border)
                                        PrayerCalculationMethod.entries.forEach { method ->
                                            val isSelected = currentLocation.isMethodManualOverride &&
                                                currentLocation.calculationMethodId == method.alAdhanMethodId
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = method.arabicName,
                                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                } else null,
                                                onClick = {
                                                    methodMenuExpanded = false
                                                    onSelectCalculationMethod(method)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "طريقة الحساب النشطة: ${resolvedMethod.arabicName} • يعمل لاحقاً بدون إنترنت",
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

@SuppressLint("MissingPermission")
private fun fetchLocationManagerFallback(context: Context): Location? {
    return runCatching {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        providers.mapNotNull { provider ->
            runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }
    }.getOrNull()
}
