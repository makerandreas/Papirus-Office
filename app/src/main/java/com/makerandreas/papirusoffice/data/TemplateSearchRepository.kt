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

    private val apiKey = BuildConfig.GOOGLE_CSE_API_KEY
    private val searchEngineId = BuildConfig.GOOGLE_CSE_CX

    fun buildTemplateSearchUrl(query: String): String {
        val encodedQuery = URLEncoder.encode("$query filetype:odt OR filetype:docx OR filetype:ott", "UTF-8")
        return "https://www.googleapis.com/customsearch/v1?key=$apiKey&cx=$searchEngineId&q=$encodedQuery"
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
                        type = if (query.lowercase().contains("sheet") || query.lowercase().contains("calc")) "ODS" else if (query.lowercase().contains("slide") || query.lowercase().contains("presentation")) "ODP" else "ODT",
                        url = citation.url,
                        description = "Grounded Search Result: ${citation.title}"
                    )
                )
            }
        } else if (groundedResult.textResponse.isNotEmpty()) {
            templates.add(
                TemplateManager.TemplateItem(
                    name = "$query Template (AI Generated Idea)",
                    type = "ODT",
                    url = "https://templates.office.com",
                    description = groundedResult.textResponse.take(150) + "..."
                )
            )
        }
        templates
    }

    /**
     * Search online templates using Google Custom Search Engine with fallback
     */
    suspend fun searchOnlineTemplates(query: String): List<TemplateManager.TemplateItem> = withContext(Dispatchers.IO) {
        if (apiKey.isEmpty() || apiKey == "YOUR_CSE_API_KEY" || searchEngineId.isEmpty() || searchEngineId == "YOUR_CSE_CX") {
            Log.d(TAG, "Google CSE credentials are not configured or are placeholder values. Skipping CSE query.")
            return@withContext emptyList()
        }

        val url = buildTemplateSearchUrl(query)
        Log.d(TAG, "Querying Google CSE: $url")

        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "PapirusOffice/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Google CSE request failed with HTTP code: ${response.code}")
                    return@withContext emptyList()
                }

                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val jsonObject = JSONObject(bodyString)
                val itemsArray = jsonObject.optJSONArray("items") ?: return@withContext emptyList()

                val results = mutableListOf<TemplateManager.TemplateItem>()
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    val title = item.optString("title", "Online Template")
                    val link = item.optString("link", "")
                    val snippet = item.optString("snippet", "")

                    if (link.isNotEmpty()) {
                        val lowerLink = link.lowercase()
                        val type = when {
                            lowerLink.endsWith(".odt") -> "ODT"
                            lowerLink.endsWith(".ods") -> "ODS"
                            lowerLink.endsWith(".odp") -> "ODP"
                            else -> "ODT"
                        }

                        results.add(
                            TemplateManager.TemplateItem(
                                name = title.replace(".odt", "").replace(".docx", "").trim(),
                                type = type,
                                url = link,
                                description = snippet
                            )
                        )
                    }
                }
                return@withContext results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing Google CSE search: ${e.localizedMessage}", e)
            emptyList()
        }
    }
}
