package com.example.core.ai

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Service to handle Google Gemini AI interactions.
 * Features an opt-in architecture, Zero Data Retention prompts, user API Key storage,
 * and standard 60-second timeouts to avoid connection interruption.
 */
object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private const val PREFS_NAME = "papirus_office_ai_prefs"
    private const val KEY_API_KEY = "user_gemini_api_key"
    private const val KEY_IS_ENABLED = "ai_features_enabled"
    private const val KEY_MODEL = "selected_gemini_model"

    // Default to the highly performant and modern gemini-3.5-flash for basic text & code tasks
    const val DEFAULT_MODEL = "gemini-3.5-flash"
    
    // Model aliases
    val SUPPORTED_MODELS = listOf(
        "gemini-3.5-flash" to "Gemini 3.5 Flash (Default & Fastest)",
        "gemini-3.1-pro-preview" to "Gemini 3.1 Pro (Complex Reasoning)",
        "gemini-2.5-flash-image" to "Gemini 2.5 Image Generator"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Opt-in: Enable/Disable AI helper features globally.
     */
    fun setAiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ENABLED, enabled)
            .apply()
    }

    fun isAiEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ENABLED, false)
    }

    /**
     * Configure and save the user's private Google AI Studio API key.
     */
    fun saveUserApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getUserApiKey(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "") ?: ""
        
        // Fallback to BuildConfig if developer supplied an API key at compile-time
        return saved.ifEmpty { 
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    /**
     * Choose preferred model.
     */
    fun saveSelectedModel(context: Context, model: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL, model)
            .apply()
    }

    fun getSelectedModel(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    /**
     * Executes content generation asynchronously with the user-supplied API key,
     * ensuring that System Instructions mandate zero-data-retention / local context rules.
     */
    suspend fun generateContent(context: Context, prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        if (!isAiEnabled(context)) {
            return@withContext "AI Assistant is disabled. Please enable it and set up your API Key in Settings."
        }

        val apiKey = getUserApiKey(context)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing! Please enter a valid Gemini API Key from Google AI Studio in the Settings menu."
        }

        val model = getSelectedModel(context)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        // Enforce strong zero data retention compliance policies in the system instruction
        val complianceInstruction = """
            You are the Papirus Office AI Assistant. 
            Zero Data Retention Policy: Do not store, leak, or log any user data. Treat all input content as strictly ephemeral.
            Do not include promotional speech or flowery filler. Be precise and clean in your office document recommendations.
            ${systemInstruction ?: ""}
        """.trimIndent()

        try {
            // Build request JSON programmatically
            val requestJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                val systemInstructionObj = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", complianceInstruction)
                        })
                    })
                }
                put("systemInstruction", systemInstructionObj)
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API request failed. Code: ${response.code}, Body: $errBody")
                    return@withContext "API Request Failed (Code ${response.code}): $errBody"
                }

                val resBody = response.body?.string() ?: return@withContext "Empty response from Gemini server."
                val jsonResponse = JSONObject(resBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
                "Failed to parse generation content candidate parts."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception invoking Gemini AI service", e)
            "Error contacting Gemini API: ${e.localizedMessage ?: "Connection Timeout (60s exceeded)"}"
        }
    }
}
