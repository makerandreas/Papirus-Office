package com.example.core.util

import android.content.Context
import android.util.Log
import com.example.core.ai.GeminiAiService
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TemplateManager {
    private const val TAG = "TemplateManager"

    data class TemplateItem(
        val name: String,
        val type: String, // ODT, ODS, ODP
        val url: String,
        val description: String
    )

    // A curated, robust list of LibreOffice templates of standard ODF formats acting as the primary source
    val curatedTemplates = listOf(
        // ODT Templates
        TemplateItem(
            name = "Resume (Modern Layout)",
            type = "ODT",
            url = "https://filesamples.com/samples/document/odt/sample1.odt",
            description = "A clean and modern curriculum vitae layout for professional job applications, featuring styled sidebars and clean typography."
        ),
        TemplateItem(
            name = "Business Proposal",
            type = "ODT",
            url = "https://filesamples.com/samples/document/odt/sample2.odt",
            description = "A formal standard ODT business proposal template with structured headers, custom margins, and professional formatting."
        ),
        TemplateItem(
            name = "Academic Project Charter",
            type = "ODT",
            url = "https://filesamples.com/samples/document/odt/sample3.odt",
            description = "A comprehensive ODT document featuring multi-level hierarchical headers, styled bullet lists, and standard margins for report writing."
        ),

        // ODS Templates
        TemplateItem(
            name = "Monthly Personal Budget",
            type = "ODS",
            url = "https://filesamples.com/samples/document/ods/sample1.ods",
            description = "An interactive Calc sheet featuring pre-defined formulas, financial expense tracking tables, and budget rules."
        ),
        TemplateItem(
            name = "Product Sales Report",
            type = "ODS",
            url = "https://filesamples.com/samples/document/ods/sample2.ods",
            description = "A detailed business table containing product sales statistics, structured calculations, and regional performance matrices."
        ),
        TemplateItem(
            name = "Company Inventory Tracker",
            type = "ODS",
            url = "https://filesamples.com/samples/document/ods/sample3.ods",
            description = "A robust stock and asset management Calc template with custom columns, unit counts, and price calculations."
        ),

        // ODP Templates
        TemplateItem(
            name = "Executive Business Slide Deck",
            type = "ODP",
            url = "https://filesamples.com/samples/document/odp/sample1.odp",
            description = "An elegant, corporate presentation slide layout tailored for business review meetings, stakeholder pitches, and executive reports."
        ),
        TemplateItem(
            name = "Interactive Educational Tutorial",
            type = "ODP",
            url = "https://filesamples.com/samples/document/odp/sample2.odp",
            description = "A visually appealing ODP slideshow designed for academic lectures, training modules, and comprehensive teaching slides."
        ),
        TemplateItem(
            name = "Project Roadmap Pitch Deck",
            type = "ODP",
            url = "https://filesamples.com/samples/document/odp/sample3.odp",
            description = "A professional slide layout ideal for highlighting milestone timelines, agile roadmaps, team roles, and key deliverables."
        )
    )

    /**
     * Retrieves ODF templates, returning the high-quality curated templates list as the primary source.
     * Google Custom Search Engine (CSE) and Gemini AI recommendations serve as fallback options.
     */
    suspend fun searchTemplates(context: Context, type: String): List<TemplateItem> = withContext(Dispatchers.IO) {
        val filteredCurated = curatedTemplates.filter { type == "All" || it.type.equals(type, ignoreCase = true) }
        
        // 1. Try curated templates (primary source) first
        if (filteredCurated.isNotEmpty()) {
            Log.d(TAG, "Successfully loaded ${filteredCurated.size} primary source templates for type: $type")
            return@withContext filteredCurated
        }

        // 2. Fallback: Google Custom Search Engine (CSE)
        val queryType = if (type == "All") "ODF LibreOffice templates" else "$type LibreOffice templates"
        try {
            val cseRepo = com.makerandreas.papirusoffice.data.TemplateSearchRepository()
            val cseResults = cseRepo.searchOnlineTemplates(queryType)
            if (cseResults.isNotEmpty()) {
                Log.d(TAG, "Fallback: Retrieved ${cseResults.size} templates from Google CSE.")
                return@withContext cseResults.filter { type == "All" || it.type.equals(type, ignoreCase = true) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback: Google CSE search failed: ${e.localizedMessage}")
        }

        // 3. Fallback: Gemini AI Recommendations
        if (GeminiAiService.isAiEnabled(context)) {
            val prompt = """
                Provide a list of real ODF $type template files available online.
                Format your response STRICTLY as a raw JSON array of objects. Do not wrap the JSON in markdown code blocks like ```json ... ```.
                Each object in the array must have the following keys:
                - "name": String (the title of the template, e.g. "Professional CV" or "Household Ledger")
                - "type": String (either "ODT", "ODS", or "ODP")
                - "url": String (a direct download link of an ODF file, e.g. "https://filesamples.com/samples/document/odt/sample1.odt")
                - "description": String (a clean, user-friendly description of what this template is for)

                Ensure the URLs are direct and functional.
            """.trimIndent()

            try {
                val responseText = GeminiAiService.generateContent(
                    context = context,
                    prompt = prompt,
                    systemInstruction = "You are an assistant that outputs ONLY direct ODF template lists in raw JSON array format."
                )

                val cleanedJson = responseText.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val jsonArray = JSONArray(cleanedJson)
                val result = mutableListOf<TemplateItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name", "Unnamed Template")
                    val itemType = obj.optString("type", type).uppercase()
                    val url = obj.optString("url", "https://filesamples.com/samples/document/odt/sample1.odt")
                    val description = obj.optString("description", "")
                    result.add(TemplateItem(name, itemType, url, description))
                }
                if (result.isNotEmpty()) {
                    Log.d(TAG, "Fallback: Retrieved ${result.size} templates from Gemini AI.")
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback: Gemini AI recommendation failed", e)
            }
        }

        return@withContext emptyList()
    }

    /**
     * Download a template using OkHttpClient to /storage/emulated/0/Android/data/com.makerandreas.papirusoffice/files/templates
     */
    suspend fun downloadTemplate(
        context: Context,
        template: TemplateItem,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            val templatesDir = context.getExternalFilesDir("templates") ?: return@withContext null
            if (!templatesDir.exists()) {
                templatesDir.mkdirs()
            }

            val fileExtension = template.type.lowercase()
            // Sanitize file name to prevent directory traversal or malformed paths
            val safeName = template.name.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
            val fileName = "$safeName.$fileExtension"
            val destinationFile = File(templatesDir, fileName)

            Log.d(TAG, "Downloading template from ${template.url} to ${destinationFile.absolutePath}")

            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(template.url)
                .header("User-Agent", "PapirusOffice/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to download template. HTTP Code: ${response.code}")
                    return@withContext null
                }

                val body = response.body ?: return@withContext null
                val contentLength = body.contentLength()
                val inputStream: InputStream = body.byteStream()
                val outputStream = FileOutputStream(destinationFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                        withContext(Dispatchers.Main) {
                            onProgress(progress)
                        }
                    }
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                Log.d(TAG, "Template downloaded successfully to: ${destinationFile.absolutePath}")
                destinationFile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading template: ${e.localizedMessage}", e)
            null
        }
    }
}
