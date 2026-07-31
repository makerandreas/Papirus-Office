package com.makerandreas.papirusoffice.core.fonts

import android.content.Context
import android.util.Log
import com.makerandreas.papirusoffice.data.ApiKeyManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class GoogleFontsRepository(private val context: Context) {
    private val TAG = "GoogleFontsRepository"
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(GoogleFontsResponse::class.java)
    
    private var cachedFonts: List<GoogleFontMetadata> = emptyList()

    /**
     * Fetches fonts dynamically from Google Fonts REST API with primary key (GOOGLE_CSE_API_KEY)
     * and fallback key (GOOGLE_FONTS_REST_API).
     */
    suspend fun fetchFontsFromNetwork(): List<GoogleFontMetadata>? = withContext(Dispatchers.IO) {
        ApiKeyManager.executeWithFallback("Google Fonts REST API Fetch") { apiKey ->
            val url = "https://www.googleapis.com/webfonts/v1/webfonts?key=$apiKey&sort=popularity"
            Log.d(TAG, "Fetching Google Fonts catalog with key prefix [${apiKey.take(6)}...]")

            val client = OkHttpClient.Builder()
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PapirusOffice/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Google Fonts REST API failed HTTP ${response.code} with key prefix [${apiKey.take(6)}...]")
                    return@use null
                }

                val bodyString = response.body?.string() ?: return@use null
                val fontResponse = adapter.fromJson(bodyString)
                val items = fontResponse?.items?.sortedBy { it.family }
                if (items.isNullOrEmpty()) {
                    Log.w(TAG, "Google Fonts REST API returned empty list for key prefix [${apiKey.take(6)}...]")
                    return@use null
                }
                items
            }
        }
    }

    suspend fun getFontsList(): List<GoogleFontMetadata> = withContext(Dispatchers.IO) {
        if (cachedFonts.isNotEmpty()) return@withContext cachedFonts

        // 1. Try Google Fonts REST API first
        val networkFonts = fetchFontsFromNetwork()
        if (!networkFonts.isNullOrEmpty()) {
            Log.d(TAG, "Loaded ${networkFonts.size} fonts dynamically from Google Fonts REST API.")
            cachedFonts = networkFonts
            return@withContext cachedFonts
        }

        // 2. Fallback: Load from assets/fonts.json
        Log.d(TAG, "Network REST API unavailable. Loading font catalog from assets/fonts.json fallback...")
        try {
            val inputStream = context.assets.open("fonts.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val response = adapter.fromJson(jsonString)
            cachedFonts = response?.items?.sortedBy { it.family } ?: emptyList()
            cachedFonts
        } catch (e: Exception) {
            Log.e(TAG, "Error reading assets/fonts.json: ${e.localizedMessage}", e)
            emptyList()
        }
    }

    suspend fun getDownloadableFonts(family: String): List<DownloadableFont> {
        val fonts = getFontsList()
        val font = fonts.find { it.family.equals(family, ignoreCase = true) } ?: return emptyList()
        
        val downloadables = mutableListOf<DownloadableFont>()
        font.files?.forEach { (variant, url) ->
            downloadables.add(DownloadableFont(font.family, variant, url, font.category))
        }
        return downloadables
    }
}

