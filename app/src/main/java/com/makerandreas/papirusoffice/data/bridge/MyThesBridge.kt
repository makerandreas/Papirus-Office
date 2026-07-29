package com.makerandreas.papirusoffice.data.bridge

import android.content.Context
import android.util.Log
import com.makerandreas.papirusoffice.data.framework.Locale
import com.makerandreas.papirusoffice.data.framework.PropertyValue
import com.makerandreas.papirusoffice.data.framework.XMeaning
import com.makerandreas.papirusoffice.data.framework.XThesaurus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.charset.Charset

/**
 * Data model for a MyThes meaning entry containing definition/part of speech and synonyms.
 */
data class MyThesMeaning(
    override val meaning: String,
    override val synonyms: Array<String>
) : XMeaning

/**
 * Pure Kotlin native implementation of MyThes engine (mythes.hxx / mythes.cxx)
 * Reads MyThes index (.idx) and data (.dat) files for fast binary search thesaurus lookups.
 * Fully integrated with LibreOffice / Papirus UNO XThesaurus interface.
 */
class MyThesBridge private constructor() : XThesaurus {

    private val TAG = "MyThesBridge"

    private var encodingName: String = "ISO-8859-1"
    private var isInitialized = false

    // Index entries: word -> byte offset in data file
    private val indexList = ArrayList<String>()
    private val offsetList = ArrayList<Long>()

    private var dataFile: File? = null

    override val locales: Array<Locale>
        get() = arrayOf(Locale("en", "US", ""))

    override fun hasLocale(locale: Locale): Boolean {
        return locale.Language.equals("en", ignoreCase = true)
    }

    /**
     * Initializes MyThes engine from asset streams or local files.
     */
    suspend fun initializeFromAssets(context: Context, idxAssetPath: String = "thesaurus/checkme.lst", datAssetPath: String = "thesaurus/data_layout.txt") = withContext(Dispatchers.IO) {
        try {
            // Check if thesaurus files exist in assets
            val assetManager = context.assets
            
            // Extract to cache dir for RandomAccessFile seeking if needed
            val idxFile = File(context.cacheDir, "thesaurus_active.idx")
            val datFile = File(context.cacheDir, "thesaurus_active.dat")

            var hasAssets = false
            try {
                assetManager.open(idxAssetPath).use { input ->
                    idxFile.outputStream().use { output -> input.copyTo(output) }
                }
                assetManager.open(datAssetPath).use { input ->
                    datFile.outputStream().use { output -> input.copyTo(output) }
                }
                hasAssets = true
            } catch (e: Exception) {
                Log.w(TAG, "Custom asset paths $idxAssetPath / $datAssetPath not directly readable, using internal fallback dictionary.")
            }

            if (hasAssets) {
                loadIndexAndData(idxFile, datFile)
            } else {
                // Populate default English thesaurus entries for seamless offline operation
                populateDefaultFallbackThesaurus()
            }
            isInitialized = true
            Log.d(TAG, "MyThesBridge initialized successfully. Loaded ${indexList.size} thesaurus index entries.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MyThesBridge: ${e.message}", e)
            populateDefaultFallbackThesaurus()
            isInitialized = true
        }
    }

    private fun loadIndexAndData(idxFile: File, datFile: File) {
        if (!idxFile.exists() || !datFile.exists()) return

        dataFile = datFile
        idxFile.bufferedReader(Charset.forName("ISO-8859-1")).use { reader ->
            // Line 1: Encoding
            val encLine = reader.readLine() ?: "ISO-8859-1"
            encodingName = parseEncoding(encLine)

            // Line 2: Number of index entries
            val countLine = reader.readLine() ?: "0"
            val count = countLine.trim().toIntOrNull() ?: 0

            var line: String? = reader.readLine()
            while (line != null) {
                val pipeIdx = line.indexOf('|')
                if (pipeIdx > 0) {
                    val word = line.substring(0, pipeIdx).lowercase().trim()
                    val offset = line.substring(pipeIdx + 1).trim().toLongOrNull() ?: 0L
                    indexList.add(word)
                    offsetList.add(offset)
                }
                line = reader.readLine()
            }
        }
    }

    private fun parseEncoding(encHeader: String): String {
        val cleaned = encHeader.trim().uppercase()
        return when {
            cleaned.contains("UTF-8") || cleaned.contains("UTF8") -> "UTF-8"
            cleaned.contains("ISO8859-1") || cleaned.contains("ISO-8859-1") -> "ISO-8859-1"
            cleaned.contains("CP-1251") || cleaned.contains("WINDOWS-1251") -> "windows-1251"
            else -> "ISO-8859-1"
        }
    }

    /**
     * Binary search algorithm matching mythes.cxx binsearch
     */
    private fun binarySearchIndex(target: String): Int {
        var low = 0
        var high = indexList.size - 1
        val searchWord = target.lowercase().trim()

        while (low <= high) {
            val mid = (low + high) ushr 1
            val cmp = indexList[mid].compareTo(searchWord)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -1
    }

    /**
     * Async background query for meanings & synonyms (XThesaurus implementation)
     */
    override fun queryMeanings(
        term: String,
        locale: Locale,
        properties: Array<PropertyValue>
    ): Array<XMeaning>? {
        val results = lookupSynonymsSync(term)
        return if (results.isNotEmpty()) results.toTypedArray() else null
    }

    /**
     * Synchronous lookup implementation matching MyThes::Lookup
     */
    fun lookupSynonymsSync(term: String): List<MyThesMeaning> {
        val searchWord = term.lowercase().trim()
        val idx = binarySearchIndex(searchWord)

        if (idx >= 0 && dataFile != null && dataFile!!.exists()) {
            val offset = offsetList[idx]
            try {
                RandomAccessFile(dataFile, "r").use { raf ->
                    raf.seek(offset)
                    val headerLine = readLineFromRaf(raf, encodingName) ?: return emptyList()
                    val pipeIdx = headerLine.indexOf('|')
                    if (pipeIdx < 0) return emptyList()

                    val numMeanings = headerLine.substring(pipeIdx + 1).trim().toIntOrNull() ?: 0
                    val meaningsList = mutableListOf<MyThesMeaning>()

                    for (i in 0 until numMeanings) {
                        val meanLine = readLineFromRaf(raf, encodingName) ?: break
                        val parts = meanLine.split("|")
                        if (parts.isNotEmpty()) {
                            val pos = parts[0].trim()
                            val synonyms = parts.drop(1).filter { it.isNotBlank() }.map { it.trim() }
                            val defn = if (pos.isNotEmpty()) "$pos ${synonyms.firstOrNull() ?: ""}" else synonyms.firstOrNull() ?: ""
                            meaningsList.add(
                                MyThesMeaning(
                                    meaning = defn,
                                    synonyms = synonyms.toTypedArray()
                                )
                            )
                        }
                    }
                    return meaningsList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading thesaurus data file for '$term': ${e.message}")
            }
        }

        // Fallback to memory map if index lookup not found
        val fallback = defaultThesaurusMap[searchWord]
        if (fallback != null) {
            return fallback
        }

        return emptyList()
    }

    /**
     * Async coroutine lookup for Kotlin / Compose UI
     */
    suspend fun lookupSynonymsAsync(term: String): List<MyThesMeaning> = withContext(Dispatchers.IO) {
        lookupSynonymsSync(term)
    }

    private fun readLineFromRaf(raf: RandomAccessFile, encoding: String): String? {
        val lineBytes = mutableListOf<Byte>()
        var b = raf.read()
        if (b == -1) return null
        while (b != -1 && b != '\n'.code && b != '\r'.code) {
            lineBytes.add(b.toByte())
            b = raf.read()
        }
        if (b == '\r'.code) {
            val nextByte = raf.read()
            if (nextByte != '\n'.code && nextByte != -1) {
                raf.seek(raf.filePointer - 1)
            }
        }
        return String(lineBytes.toByteArray(), Charset.forName(encoding))
    }

    private val defaultThesaurusMap = HashMap<String, List<MyThesMeaning>>()

    private fun populateDefaultFallbackThesaurus() {
        defaultThesaurusMap["simple"] = listOf(
            MyThesMeaning("(adj)", arrayOf("easy", "plain", "uncomplicated", "elementary", "bare", "straightforward")),
            MyThesMeaning("(noun)", arrayOf("simpleton", "herb", "element"))
        )
        defaultThesaurusMap["complex"] = listOf(
            MyThesMeaning("(adj)", arrayOf("complicated", "intricate", "elaborate", "composite", "multifaceted")),
            MyThesMeaning("(noun)", arrayOf("structure", "network", "system", "syndrome"))
        )
        defaultThesaurusMap["document"] = listOf(
            MyThesMeaning("(noun)", arrayOf("record", "text", "file", "paper", "manuscript", "certificate")),
            MyThesMeaning("(verb)", arrayOf("record", "detail", "substantiate", "verify", "register"))
        )
        defaultThesaurusMap["edit"] = listOf(
            MyThesMeaning("(verb)", arrayOf("modify", "revise", "format", "correct", "amend", "alter", "redact"))
        )
        defaultThesaurusMap["format"] = listOf(
            MyThesMeaning("(noun)", arrayOf("style", "layout", "design", "structure", "appearance")),
            MyThesMeaning("(verb)", arrayOf("arrange", "design", "align", "organize"))
        )
        defaultThesaurusMap["office"] = listOf(
            MyThesMeaning("(noun)", arrayOf("bureau", "workplace", "agency", "department", "post", "function"))
        )
        defaultThesaurusMap["table"] = listOf(
            MyThesMeaning("(noun)", arrayOf("grid", "chart", "matrix", "schedule", "spreadsheet", "board")),
            MyThesMeaning("(verb)", arrayOf("submit", "propose", "postpone"))
        )
        defaultThesaurusMap["slide"] = listOf(
            MyThesMeaning("(noun)", arrayOf("presentation", "transparency", "frame", "diapositive")),
            MyThesMeaning("(verb)", arrayOf("glide", "slip", "drift", "skate"))
        )
    }

    companion object {
        @Volatile
        private var instance: MyThesBridge? = null

        fun getInstance(): MyThesBridge {
            return instance ?: synchronized(this) {
                instance ?: MyThesBridge().also { instance = it }
            }
        }
    }
}
