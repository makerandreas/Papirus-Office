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
 * Service to handle Google Gemini AI interactions in Papirus Engine.
 * Features:
 * - Search Grounding using Google Search tool for live templates, stock images, and online info.
 * - Multi-model support: gemini-3.5-flash (default/general), gemini-3.1-pro-preview (complex reasoning),
 *   gemini-3.1-flash-lite-preview (fast proofreading/formatting).
 * - Office Productivity Helpers (Summarize, Grammar Polish, Formula Generation, Presentation Outlines).
 */
object GeminiAiService {
    private const val TAG = "GeminiAiService"
    private const val PREFS_NAME = "papirus_office_ai_prefs"
    private const val KEY_API_KEY = "user_gemini_api_key"
    private const val KEY_IS_ENABLED = "ai_features_enabled"
    private const val KEY_MODEL = "selected_gemini_model"

    // Supported modern models according to skill guidelines
    const val MODEL_FLASH = "gemini-3.5-flash"
    const val MODEL_PRO = "gemini-3.1-pro-preview"
    const val MODEL_LITE = "gemini-3.1-flash-lite-preview"

    val SUPPORTED_MODELS = listOf(
        MODEL_FLASH to "Gemini 3.5 Flash (General & Search Grounding)",
        MODEL_PRO to "Gemini 3.1 Pro (Complex Office Reasoning)",
        MODEL_LITE to "Gemini 3.1 Flash-Lite (Fast Proofreading & Formulas)"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun setAiEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_ENABLED, enabled)
            .apply()
    }

    fun isAiEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_ENABLED, true) // Enable by default for seamless assistant experience
    }

    fun saveUserApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getUserApiKey(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_API_KEY, "") ?: ""
        
        return saved.ifEmpty { 
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun saveSelectedModel(context: Context, model: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL, model)
            .apply()
    }

    fun getSelectedModel(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODEL, MODEL_FLASH) ?: MODEL_FLASH
    }

    /**
     * Data class holding a Grounded Search result with web citations
     */
    data class GroundedSearchResult(
        val textResponse: String,
        val searchQueries: List<String>,
        val citations: List<WebCitation>
    )

    data class WebCitation(
        val title: String,
        val url: String
    )

    /**
     * Executes generation with Search Grounding (Google Search tool enabled)
     */
    suspend fun generateWithSearchGrounding(
        context: Context,
        prompt: String,
        customModel: String? = null
    ): GroundedSearchResult = withContext(Dispatchers.IO) {
        val apiKey = getUserApiKey(context)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext GroundedSearchResult(
                textResponse = "Gemini API Key is missing. Please enter your API Key in Settings.",
                searchQueries = emptyList(),
                citations = emptyList()
            )
        }

        val model = customModel ?: MODEL_FLASH
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                // Enable Search Grounding tool
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "Error ${response.code}"
                    return@withContext GroundedSearchResult("API Error: $err", emptyList(), emptyList())
                }

                val resBody = response.body?.string() ?: ""
                val jsonResponse = JSONObject(resBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val contentObj = firstCandidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    val generatedText = parts?.optJSONObject(0)?.optString("text", "") ?: "No response."

                    val groundingMetadata = firstCandidate.optJSONObject("groundingMetadata")
                    val searchQueries = mutableListOf<String>()
                    val citations = mutableListOf<WebCitation>()

                    groundingMetadata?.optJSONArray("webSearchQueries")?.let { queriesArr ->
                        for (i in 0 until queriesArr.length()) {
                            searchQueries.add(queriesArr.getString(i))
                        }
                    }

                    groundingMetadata?.optJSONArray("groundingChunks")?.let { chunksArr ->
                        for (i in 0 until chunksArr.length()) {
                            val web = chunksArr.getJSONObject(i).optJSONObject("web")
                            if (web != null) {
                                val title = web.optString("title", "Source")
                                val uri = web.optString("uri", "")
                                if (uri.isNotEmpty()) {
                                    citations.add(WebCitation(title, uri))
                                }
                            }
                        }
                    }

                    return@withContext GroundedSearchResult(generatedText, searchQueries, citations)
                }
                return@withContext GroundedSearchResult("No candidate response generated.", emptyList(), emptyList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search Grounding Exception", e)
            return@withContext GroundedSearchResult("Network error: ${e.localizedMessage}", emptyList(), emptyList())
        }
    }

    /**
     * Executes standard content generation
     */
    suspend fun generateContent(
        context: Context,
        prompt: String,
        systemInstruction: String? = null,
        targetModel: String? = null
    ): String = withContext(Dispatchers.IO) {
        if (!isAiEnabled(context)) {
            return@withContext "AI Assistant is disabled."
        }

        val apiKey = getUserApiKey(context)
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is missing! Please set your Gemini API Key in Settings."
        }

        val model = targetModel ?: getSelectedModel(context)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val complianceInstruction = """
            You are the Papirus Office Gemini Copilot.
            Provide clean, precise, professional office document output.
            ${systemInstruction ?: ""}
        """.trimIndent()

        try {
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", complianceInstruction) })
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "Gemini API failed (${response.code}): $errBody")
                    return@withContext "API Request Failed (Code ${response.code})"
                }

                val resBody = response.body?.string() ?: return@withContext "Empty response."
                val jsonResponse = JSONObject(resBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val parts = firstCandidate.optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
                "No candidate response found."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API Exception", e)
            "Error: ${e.localizedMessage}"
        }
    }

    // --- High-Level Office Productivity Helpers ---

    suspend fun summarizeDocument(context: Context, text: String, docType: String = "Document"): String {
        val prompt = "Summarize the following $docType concisely into bullet points with key takeaways:\n\n$text"
        return generateContent(context, prompt, systemInstruction = "You are an expert document summarizer.", targetModel = MODEL_FLASH)
    }

    suspend fun proofreadAndPolish(context: Context, text: String, tone: String = "Professional"): String {
        val prompt = "Proofread, fix grammar, and rewrite the following text in a $tone tone:\n\n$text"
        return generateContent(context, prompt, systemInstruction = "You are a professional editor. Return only the improved text.", targetModel = MODEL_LITE)
    }

    suspend fun generateSpreadsheetFormula(context: Context, description: String): String {
        val prompt = "Generate an ODS/Excel spreadsheet formula for the following request: '$description'. Provide the formula (e.g., =SUM(A1:A10)) and a 1-sentence explanation."
        return generateContent(context, prompt, systemInstruction = "You are a spreadsheet formula expert.", targetModel = MODEL_LITE)
    }

    suspend fun generateSlideDeckOutline(context: Context, topic: String, numSlides: Int = 5): String {
        val prompt = "Create a $numSlides-slide presentation outline for the topic: '$topic'. Include Slide Title, Key Bullet Points, and Speaker Notes for each slide."
        return generateContent(context, prompt, systemInstruction = "You are a presentation design expert.", targetModel = MODEL_PRO)
    }
}
