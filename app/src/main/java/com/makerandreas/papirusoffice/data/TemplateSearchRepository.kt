package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.core.ai.GeminiAiService
import com.example.core.util.TemplateManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TemplateSearchRepository {
    private val TAG = "TemplateSearchRepository"

    private val searchEngineId = BuildConfig.GOOGLE_CSE_CX

    fun buildTemplateSearchUrl(key: String, query: String): String {
        val encodedQuery = URLEncoder.encode("$query filetype:ott OR filetype:ots OR filetype:otp OR filetype:odt OR filetype:ods OR filetype:odp", "UTF-8")
        return "https://www.googleapis.com/customsearch/v1?key=$key&cx=$searchEngineId&q=$encodedQuery"
    }

    /**
     * Search templates using AI Search Grounding powered by Gemini (Google Search Tool)
     */
    suspend fun searchTemplatesWithAiGrounding(context: Context, query: String): List<TemplateManager.TemplateItem> = withContext(Dispatchers.IO) {
        val prompt = "Search for high quality document templates online related to '$query'. Provide 4 template recommendations with titles, description, and source links."
        
        val groundedResult = GeminiAiService.generateWithSearchGrounding(context, prompt)
        val templates = mutableListOf<TemplateManager.TemplateItem>()

        if (groundedResult.citations.isNotEmpty()) {
            groundedResult.citations.forEach { citation ->
                templates.add(
                    TemplateManager.TemplateItem(
                        name = citation.title,
                        type = if (query.lowercase().contains("sheet") || query.lowercase().contains("calc") || query.lowercase().contains("ots")) "OTS" else if (query.lowercase().contains("slide") || query.lowercase().contains("presentation") || query.lowercase().contains("otp")) "OTP" else "OTT",
                        url = citation.url,
                        description = "Grounded Search Result: ${citation.title}"
                    )
                )
            }
        } else if (groundedResult.textResponse.isNotEmpty()) {
            templates.add(
                TemplateManager.TemplateItem(
                    name = "$query Template (AI Generated Idea)",
                    type = "OTT",
                    url = "https://templates.office.com",
                    description = groundedResult.textResponse.take(150) + "..."
                )
            )
        }
        templates
    }

    /**
     * Search online templates using Google Custom Search Engine with API Key fallback
     */
    suspend fun searchOnlineTemplates(query: String): List<TemplateManager.TemplateItem> = withContext(Dispatchers.IO) {
        if (searchEngineId.isEmpty() || searchEngineId == "YOUR_CSE_CX") {
            Log.d(TAG, "Google CSE CX is not configured. Skipping CSE query.")
            return@withContext emptyList()
        }

        val results = ApiKeyManager.executeWithFallback("Google CSE Template Search") { key ->
            val url = buildTemplateSearchUrl(key, query)
            Log.d(TAG, "Querying Google CSE with key prefix [${key.take(6)}...]: $url")

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
                    Log.e(TAG, "Google CSE request failed HTTP ${response.code} with key prefix [${key.take(6)}...]")
                    return@use null
                }

                val bodyString = response.body?.string() ?: return@use null
                val jsonObject = JSONObject(bodyString)
                val itemsArray = jsonObject.optJSONArray("items") ?: return@use emptyList<TemplateManager.TemplateItem>()

                val list = mutableListOf<TemplateManager.TemplateItem>()
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val title = item.optString("title", "Online Template")
                    val link = item.optString("link", "")
                    val snippet = item.optString("snippet", "")

                    if (link.isNotEmpty()) {
                        val lowerLink = link.lowercase()
                        val type = when {
                            lowerLink.endsWith(".ott") -> "OTT"
                            lowerLink.endsWith(".ots") -> "OTS"
                            lowerLink.endsWith(".otp") -> "OTP"
                            lowerLink.endsWith(".odt") -> "ODT"
                            lowerLink.endsWith(".ods") -> "ODS"
                            lowerLink.endsWith(".odp") -> "ODP"
                            else -> "OTT"
                        }

                        list.add(
                            TemplateManager.TemplateItem(
                                name = title.replace(".ott", "").replace(".ots", "").replace(".otp", "").replace(".odt", "").trim(),
                                type = type,
                                url = link,
                                description = snippet
                            )
                        )
                    }
                }
                list
            }
        }

        results ?: emptyList()
    }
}
