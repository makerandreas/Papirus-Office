package com.makerandreas.papirusoffice.data

import android.util.Log
import com.example.BuildConfig

/**
 * ApiKeyManager manages API Key selection and automatic fallback strategy for
 * Google CSE (Template Search) and Google Fonts REST API.
 *
 * Strategy:
 * 1. Primary API Key: GOOGLE_CSE_API_KEY (restricted to Gemini API)
 * 2. Fallback API Key: GOOGLE_FONTS_REST_API (restricted to Discovery Engine & Telemetry APIs)
 */
object ApiKeyManager {
    private const val TAG = "ApiKeyManager"

    fun getPrimaryApiKey(): String {
        val key = BuildConfig.GOOGLE_CSE_API_KEY.trim()
        if (isValidKey(key)) {
            return key
        }
        return getFallbackApiKey()
    }

    fun getFallbackApiKey(): String {
        val key = BuildConfig.GOOGLE_FONTS_REST_API.trim()
        if (isValidKey(key)) {
            return key
        }
        return ""
    }

    private fun isValidKey(key: String): Boolean {
        return key.isNotEmpty() &&
                key != "YOUR_CSE_API_KEY" &&
                key != "YOUR_FONTS_REST_API" &&
                key != "MY_GEMINI_API_KEY" &&
                !key.startsWith("YOUR_")
    }

    /**
     * Executes an API request with automatic failover.
     * First attempts using GOOGLE_CSE_API_KEY (Primary).
     * If execution returns null or throws an exception, falls back to GOOGLE_FONTS_REST_API.
     */
    suspend fun <T> executeWithFallback(
        actionName: String,
        block: suspend (apiKey: String) -> T?
    ): T? {
        val primaryKey = BuildConfig.GOOGLE_CSE_API_KEY.trim()
        val fallbackKey = BuildConfig.GOOGLE_FONTS_REST_API.trim()

        if (isValidKey(primaryKey)) {
            try {
                Log.d(TAG, "[$actionName] Attempting API call using primary GOOGLE_CSE_API_KEY...")
                val result = block(primaryKey)
                if (result != null) {
                    Log.d(TAG, "[$actionName] Primary API key execution succeeded.")
                    return result
                }
                Log.w(TAG, "[$actionName] Primary GOOGLE_CSE_API_KEY failed/returned null. Switching to fallback key...")
            } catch (e: Exception) {
                Log.w(TAG, "[$actionName] Primary GOOGLE_CSE_API_KEY error: ${e.localizedMessage}. Switching to fallback key...", e)
            }
        } else {
            Log.d(TAG, "[$actionName] Primary GOOGLE_CSE_API_KEY is empty or placeholder. Proceeding to fallback key.")
        }

        if (isValidKey(fallbackKey) && fallbackKey != primaryKey) {
            try {
                Log.d(TAG, "[$actionName] Attempting API call using fallback GOOGLE_FONTS_REST_API...")
                val result = block(fallbackKey)
                if (result != null) {
                    Log.d(TAG, "[$actionName] Fallback API key execution succeeded.")
                    return result
                }
                Log.w(TAG, "[$actionName] Fallback GOOGLE_FONTS_REST_API also failed/returned null.")
            } catch (e: Exception) {
                Log.e(TAG, "[$actionName] Fallback GOOGLE_FONTS_REST_API error: ${e.localizedMessage}", e)
            }
        } else {
            Log.w(TAG, "[$actionName] Fallback GOOGLE_FONTS_REST_API key is empty or identical to primary key.")
        }

        return null
    }
}
