package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.BuildConfig
import com.example.alarm.PrayerCalculationMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class GlobalLocationConfig(
    val lat: Double = 30.0444,
    val lng: Double = 31.2357,
    val displayLabel: String = "مصر - القاهرة",
    val countryCode: String = "EG",
    val countryName: String = "مصر",
    val cityName: String = "القاهرة",
    val calculationMethodId: Int = PrayerCalculationMethod.EGYPT.alAdhanMethodId,
    val isMethodManualOverride: Boolean = false
) {
    fun resolvedMethod(): PrayerCalculationMethod =
        PrayerCalculationMethod.fromId(calculationMethodId)
}

data class GlobalSearchResult(
    val lat: Double,
    val lng: Double,
    val displayLabel: String,
    val countryCode: String,
    val countryName: String,
    val cityName: String
)

/**
 * Worldwide location resolution & global city search repository:
 * - Reverse geocodes ANY GPS coordinates using Android Geocoder (Arabic locale) with OpenStreetMap Nominatim fallback.
 * - Searches ANY city, town, village, or country worldwide via Nominatim Search API with Geocoder fallback.
 * - Auto-selects the official Islamic calculation method from the resolved ISO country code.
 */
object GlobalLocationRepository {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val userAgent: String
        get() = "RafiqAlMuslim/${BuildConfig.VERSION_NAME} (Android Islamic Companion)"

    suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        lng: Double,
        preserveManualMethodId: Int? = null
    ): GlobalLocationConfig = withContext(Dispatchers.IO) {
        // 1. Try device Android Geocoder in Arabic locale first
        val fromGeocoder = runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            val geocoder = Geocoder(context.applicationContext, Locale("ar"))
            val addresses = getAddressesFromLatLng(geocoder, lat, lng)
            val addr = addresses.firstOrNull() ?: return@runCatching null
            buildConfigFromAddress(addr, lat, lng, preserveManualMethodId)
        }.getOrNull()

        if (fromGeocoder != null && fromGeocoder.displayLabel.isNotBlank()) {
            return@withContext fromGeocoder
        }

        // 2. Fallback to OpenStreetMap Nominatim reverse geocoding in Arabic
        val fromNominatim = runCatching {
            fetchNominatimReverse(lat, lng, preserveManualMethodId)
        }.getOrNull()

        if (fromNominatim != null) {
            return@withContext fromNominatim
        }

        // 3. Offline coordinate-only fallback (still calculates exact astronomical times offline!)
        val coordLabel = String.format(Locale.US, "%.3f, %.3f", lat, lng)
        val methodId = preserveManualMethodId ?: PrayerCalculationMethod.MWL.alAdhanMethodId
        GlobalLocationConfig(
            lat = lat,
            lng = lng,
            displayLabel = "موقع GPS ($coordLabel)",
            countryCode = "",
            countryName = "",
            cityName = coordLabel,
            calculationMethodId = methodId,
            isMethodManualOverride = preserveManualMethodId != null
        )
    }

    suspend fun searchLocationsGlobally(
        context: Context,
        rawQuery: String
    ): List<GlobalSearchResult> = withContext(Dispatchers.IO) {
        val query = rawQuery.trim()
        if (query.length < 2) return@withContext emptyList()

        // 1. Primary: OpenStreetMap Nominatim global search (Arabic display names + ISO country code)
        val nominatimResults = runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&accept-language=ar&addressdetails=1&limit=8"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use emptyList()
                val body = res.body?.string().orEmpty()
                if (body.isBlank()) return@use emptyList()
                val arr = JSONArray(body)
                val list = mutableListOf<GlobalSearchResult>()
                val seenLabels = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull() ?: continue
                    val lng = item.optString("lon").toDoubleOrNull() ?: continue
                    val addr = item.optJSONObject("address")
                    val cc = addr?.optString("country_code", "").orEmpty().uppercase(Locale.US)
                    val country = addr?.optString("country", "").orEmpty().trim()
                    val city = extractBestCityFromNominatim(addr, item.optString("name", ""))
                    val display = formatDisplayLabel(country, city, item.optString("display_name", query))
                    if (seenLabels.add(display)) {
                        list.add(
                            GlobalSearchResult(
                                lat = lat,
                                lng = lng,
                                displayLabel = display,
                                countryCode = cc,
                                countryName = country,
                                cityName = city
                            )
                        )
                    }
                }
                list
            }
        }.getOrDefault(emptyList())

        if (nominatimResults.isNotEmpty()) {
            return@withContext nominatimResults
        }

        // 2. Fallback: Android Geocoder.getFromLocationName
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching emptyList()
            val geocoder = Geocoder(context.applicationContext, Locale("ar"))
            val addresses = getAddressesFromName(geocoder, query)
            addresses.mapNotNull { addr ->
                val cc = addr.countryCode.orEmpty().uppercase(Locale.US)
                val country = addr.countryName.orEmpty().trim()
                val city = listOfNotNull(
                    addr.locality?.takeIf { it.isNotBlank() },
                    addr.subAdminArea?.takeIf { it.isNotBlank() },
                    addr.adminArea?.takeIf { it.isNotBlank() },
                    addr.featureName?.takeIf { it.isNotBlank() }
                ).firstOrNull().orEmpty()
                val display = formatDisplayLabel(country, city, query)
                GlobalSearchResult(
                    lat = addr.latitude,
                    lng = addr.longitude,
                    displayLabel = display,
                    countryCode = cc,
                    countryName = country,
                    cityName = city.ifBlank { query }
                )
            }.distinctBy { it.displayLabel }
        }.getOrDefault(emptyList())
    }

    private fun fetchNominatimReverse(
        lat: Double,
        lng: Double,
        preserveManualMethodId: Int?
    ): GlobalLocationConfig? {
        val url = String.format(
            Locale.US,
            "https://nominatim.openstreetmap.org/reverse?format=json&lat=%.6f&lon=%.6f&accept-language=ar&addressdetails=1",
            lat,
            lng
        )
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .build()

        return runCatching {
            httpClient.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                val body = res.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                val root = JSONObject(body)
                val addr = root.optJSONObject("address")
                val cc = addr?.optString("country_code", "").orEmpty().uppercase(Locale.US)
                val country = addr?.optString("country", "").orEmpty().trim()
                val city = extractBestCityFromNominatim(addr, root.optString("name", ""))
                val display = formatDisplayLabel(country, city, root.optString("display_name", ""))
                val autoMethod = PrayerCalculationMethod.forCountryCode(cc)
                val methodId = preserveManualMethodId ?: autoMethod.alAdhanMethodId
                GlobalLocationConfig(
                    lat = lat,
                    lng = lng,
                    displayLabel = display,
                    countryCode = cc,
                    countryName = country,
                    cityName = city,
                    calculationMethodId = methodId,
                    isMethodManualOverride = preserveManualMethodId != null
                )
            }
        }.getOrNull()
    }

    private fun buildConfigFromAddress(
        addr: Address,
        lat: Double,
        lng: Double,
        preserveManualMethodId: Int?
    ): GlobalLocationConfig? {
        val cc = addr.countryCode.orEmpty().uppercase(Locale.US)
        val country = addr.countryName.orEmpty().trim()
        val city = listOfNotNull(
            addr.locality?.takeIf { it.isNotBlank() },
            addr.subAdminArea?.takeIf { it.isNotBlank() },
            addr.adminArea?.takeIf { it.isNotBlank() }
        ).firstOrNull().orEmpty().trim()
        if (country.isBlank() && city.isBlank()) return null
        val display = formatDisplayLabel(country, city, "")
        val autoMethod = PrayerCalculationMethod.forCountryCode(cc)
        val methodId = preserveManualMethodId ?: autoMethod.alAdhanMethodId
        return GlobalLocationConfig(
            lat = lat,
            lng = lng,
            displayLabel = display,
            countryCode = cc,
            countryName = country,
            cityName = city.ifBlank { country },
            calculationMethodId = methodId,
            isMethodManualOverride = preserveManualMethodId != null
        )
    }

    private fun extractBestCityFromNominatim(addr: JSONObject?, fallbackName: String): String {
        if (addr == null) return fallbackName.trim()
        val keys = listOf(
            "city",
            "town",
            "village",
            "municipality",
            "county",
            "state_district",
            "state",
            "region"
        )
        for (k in keys) {
            val v = addr.optString(k, "").trim()
            if (v.isNotBlank()) return v
        }
        return fallbackName.trim()
    }

    fun formatDisplayLabel(country: String, city: String, fallback: String): String {
        val c = country.trim()
        val t = city.trim()
        return when {
            c.isNotBlank() && t.isNotBlank() && !t.equals(c, ignoreCase = true) -> "$c - $t"
            t.isNotBlank() -> t
            c.isNotBlank() -> c
            else -> fallback.split(",").take(2).joinToString(" - ").trim()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getAddressesFromLatLng(
        geocoder: Geocoder,
        lat: Double,
        lng: Double
    ): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                try {
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } else {
            runCatching { geocoder.getFromLocation(lat, lng, 1).orEmpty() }.getOrDefault(emptyList())
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun getAddressesFromName(
        geocoder: Geocoder,
        query: String
    ): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                try {
                    geocoder.getFromLocationName(query, 6, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (cont.isActive) cont.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (cont.isActive) cont.resume(emptyList())
                        }
                    })
                } catch (_: Exception) {
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } else {
            runCatching { geocoder.getFromLocationName(query, 6).orEmpty() }.getOrDefault(emptyList())
        }
    }
}
