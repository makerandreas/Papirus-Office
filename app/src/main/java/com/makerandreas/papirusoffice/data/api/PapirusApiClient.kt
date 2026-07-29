package com.makerandreas.papirusoffice.data.api

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit Client manager for Papirus Office external API communications.
 * Handles template fetching and cloud metadata synchronization.
 */
object PapirusApiClient {

    private const val TAG = "PapirusApiClient"
    private const val BASE_URL = "https://papirus-office-cloud.appspot.com/api/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor { message ->
            Log.d(TAG, "OkHttp: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val apiService: PapirusCloudApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PapirusCloudApiService::class.java)
    }

    /**
     * Helper to fetch document templates with built-in fallback mock data if offline or endpoint unreachable.
     */
    suspend fun fetchTemplatesWithFallback(module: String? = null): List<DocumentTemplate> {
        return try {
            val response = apiService.getTemplates(module)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.templates
            } else {
                getFallbackTemplates(module)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network call failed (${e.message}), utilizing fallback templates.")
            getFallbackTemplates(module)
        }
    }

    /**
     * Default templates provided offline
     */
    private fun getFallbackTemplates(module: String?): List<DocumentTemplate> {
        val all = listOf(
            DocumentTemplate(
                id = "tpl_inky_report",
                title = "Standard Business Report",
                category = "Business",
                moduleType = "INKY",
                description = "Professional OOXML document template with headers and table styles.",
                tags = listOf("DOCX", "Report", "Business")
            ),
            DocumentTemplate(
                id = "tpl_cellina_budget",
                title = "Annual Financial Spreadsheet",
                category = "Finance",
                moduleType = "CELLINA",
                description = "Spreadsheet template with chart formulas and multi-sheet structure.",
                tags = listOf("XLSX", "Finance", "Budget")
            ),
            DocumentTemplate(
                id = "tpl_slidia_pitch",
                title = "Executive Presentation Deck",
                category = "Presentation",
                moduleType = "SLIDIA",
                description = "Slide presentation template with animated layout transitions.",
                tags = listOf("PPTX", "Slides", "Executive")
            ),
            DocumentTemplate(
                id = "tpl_pagella_memo",
                title = "Official Desktop Publishing Note",
                category = "Publishing",
                moduleType = "PAGELLA",
                description = "Pagella vector DTP layout template for newsletter and formal letters.",
                tags = listOf("ODT", "DTP", "Newsletter")
            )
        )
        return if (module == null) all else all.filter { it.moduleType.equals(module, ignoreCase = true) }
    }
}
