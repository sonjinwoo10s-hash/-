package com.example.data.remote

import com.example.data.model.SurahSummary
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---- AlAdhan API Models ----

@JsonClass(generateAdapter = true)
data class AlAdhanResponse(
    val code: Int,
    val data: AlAdhanData
)

@JsonClass(generateAdapter = true)
data class AlAdhanData(
    val timings: AlAdhanTimings,
    val date: AlAdhanDate
)

@JsonClass(generateAdapter = true)
data class AlAdhanTimings(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String
)

@JsonClass(generateAdapter = true)
data class AlAdhanDate(
    val hijri: HijriDateDto
)

@JsonClass(generateAdapter = true)
data class HijriDateDto(
    val day: String,
    val year: String,
    val weekday: HijriWeekdayDto,
    val month: HijriMonthDto
)

@JsonClass(generateAdapter = true)
data class HijriWeekdayDto(val ar: String)

@JsonClass(generateAdapter = true)
data class HijriMonthDto(val ar: String)

interface AlAdhanApi {
    @GET("v1/timingsByCity")
    suspend fun getTimingsByCity(
        @Query("city") city: String,
        @Query("country") country: String,
        @Query("method") method: Int
    ): AlAdhanResponse

    @GET("v1/timings/{date}")
    suspend fun getTimingsByCoordinates(
        @Path("date") date: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("method") method: Int
    ): AlAdhanResponse
}

// ---- AlQuran Cloud API Models ----

@JsonClass(generateAdapter = true)
data class SurahListResponse(
    val code: Int,
    val data: List<SurahSummary>
)

@JsonClass(generateAdapter = true)
data class SurahEditionsResponse(
    val code: Int,
    val data: List<SurahEditionDto>
)

@JsonClass(generateAdapter = true)
data class SurahEditionDto(
    val number: Int,
    val name: String,
    val numberOfAyahs: Int,
    val revelationType: String,
    val ayahs: List<AyahDto>
)

@JsonClass(generateAdapter = true)
data class AyahDto(
    val number: Int,
    val numberInSurah: Int,
    val text: String,
    val juz: Int = 1
)

interface AlQuranApi {
    @GET("v1/surah")
    suspend fun getAllSurahs(): SurahListResponse

    @GET("v1/surah/{number}/editions/quran-uthmani,ar.muyassar")
    suspend fun getSurahWithTafsir(@Path("number") surahNumber: Int): SurahEditionsResponse
}

// ---- MP3Quran API v3 Models ----

@JsonClass(generateAdapter = true)
data class Mp3QuranRecitersResponse(
    val reciters: List<Mp3QuranReciterDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Mp3QuranReciterDto(
    val id: Int,
    val name: String,
    val letter: String? = null,
    val moshaf: List<Mp3QuranMoshafDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Mp3QuranMoshafDto(
    val id: Int,
    val name: String,
    val server: String,
    @Json(name = "surah_total") val surahTotal: Int = 114,
    @Json(name = "surah_list") val surahList: String = ""
)

interface Mp3QuranApi {
    @GET("api/v3/reciters")
    suspend fun getReciters(@Query("language") language: String = "ar"): Mp3QuranRecitersResponse
}

// ---- jsDelivr Hadith API (fawazahmed0/hadith-api) ----

@JsonClass(generateAdapter = true)
data class HadithCdnResponse(
    val metadata: HadithCdnMetadata? = null,
    val hadiths: List<HadithCdnEntry> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HadithCdnMetadata(
    val name: String? = null,
    val sections: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class HadithCdnReference(
    val book: Int? = null,
    val hadith: Int? = null
)

@JsonClass(generateAdapter = true)
data class HadithCdnEntry(
    val hadithnumber: Double = 0.0,
    val arabicnumber: Double? = null,
    val text: String? = null,
    val reference: HadithCdnReference? = null
)

interface HadithCdnApi {
    @GET("gh/fawazahmed0/hadith-api@1/editions/{edition}.min.json")
    suspend fun getCollection(@Path("edition") edition: String): HadithCdnResponse
}

object IslamicNetworkClient {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val alAdhanApi: AlAdhanApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.aladhan.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlAdhanApi::class.java)
    }

    val alQuranApi: AlQuranApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AlQuranApi::class.java)
    }

    val mp3QuranApi: Mp3QuranApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.mp3quran.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Mp3QuranApi::class.java)
    }

    val hadithCdnApi: HadithCdnApi by lazy {
        val hadithClient = okHttpClient.newBuilder()
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://cdn.jsdelivr.net/")
            .client(hadithClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HadithCdnApi::class.java)
    }
}
