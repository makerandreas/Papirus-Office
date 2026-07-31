package com.makerandreas.papirusoffice.data.framework

import android.util.Log
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Papirus ODF Engine.
 * Implements simulations and integrations for LibreOffice SDK Guide Chapter 51: Simple ODF.
 * Supports document properties retrieval, unzipping listing representation, and ODF Simple API simulations.
 */
object PapirusOdfEngine {

    private const val TAG = "PapirusOdfEngine"
    private val logBuffer = mutableListOf<String>()

    fun getLogs(): List<String> = logBuffer.toList()

    fun clearLogs() {
        logBuffer.clear()
        addLog("ODF Engine initialized.")
    }

    private fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(Date())
        val logLine = "[$timestamp] $message"
        Log.d(TAG, logLine)
        logBuffer.add(logLine)
    }

    // --- SECTION 1: DOCUMENT PROPERTIES (XDocumentProperties) ---

    data class OdfProperties(
        val title: String,
        val author: String,
        val subject: String,
        val description: String,
        val generator: String,
        val modificationDate: String,
        val secretValue: String
    )

    private var currentProperties = OdfProperties(
        title = "PowerPoint Presentation",
        author = "Developer",
        subject = "Algorithm Lecture Notes",
        description = "A comprehensive slide deck about graph algorithms.",
        generator = "LibreOffice/5.1.0.3\$Windows_x86 LibreOffice_project/5e3e00a007d9b3b6efb6797a8b8e57b51ab1f737",
        modificationDate = "2026-07-30T16:53:58",
        secretValue = "Made in Indonesia (Papirus Office)"
    )

    fun getDocProperties(): OdfProperties {
        addLog("[XDocumentProperties] Querying document properties supplier.")
        addLog("[XDocumentProperties]   Title: \"${currentProperties.title}\"")
        addLog("[XDocumentProperties]   Author: \"${currentProperties.author}\"")
        addLog("[XPropertyContainer] Retrieving custom user defined properties.")
        addLog("[XPropertyContainer]   Secret == \"${currentProperties.secretValue}\"")
        return currentProperties
    }

    fun updateDocProperties(updated: OdfProperties) {
        currentProperties = updated
        addLog("[XDocumentProperties] Updated metadata properties successfully.")
    }

    // --- SECTION 2: UNZIPPING ARCHIVE LISTING ---

    data class ZipEntryInfo(
        val name: String,
        val rawSize: Long,
        val compressedSize: Long,
        val date: String
    )

    fun getOdfZipContents(filename: String): List<ZipEntryInfo> {
        addLog("[ZipFileAccess] Querying zipped contents of: \"$filename\" via XNameAccess.")
        val timestamp = SimpleDateFormat("MMM d, yyyy h:mm:ss a", java.util.Locale.getDefault()).format(Date())
        return listOf(
            ZipEntryInfo("mimetype", 47, 47, timestamp),
            ZipEntryInfo("content.xml", 86313, 8040, timestamp),
            ZipEntryInfo("styles.xml", 83608, 6501, timestamp),
            ZipEntryInfo("meta.xml", 1141, 491, timestamp),
            ZipEntryInfo("settings.xml", 6037, 913, timestamp),
            ZipEntryInfo("META-INF/manifest.xml", 2248, 452, timestamp),
            ZipEntryInfo("Thumbnails/thumbnail.png", 18238, 18238, timestamp),
            ZipEntryInfo("Pictures/logo.png", 4364, 4364, timestamp)
        )
    }

    fun simulateUnzipFile(zipFilename: String, entryName: String): String {
        addLog("[ZipFileAccess] Fetching input stream for entry: \"$entryName\" via pattern mapping.")
        addLog("[SimpleFileAccess] Directing XInputStream stream to local storage.")
        val copyFnm = entryName.replace(".xml", "Copy.xml").replace(".png", "Copy.png")
        addLog("[SimpleFileAccess] Successfully extracted and saved file as: \"$copyFnm\"")
        return copyFnm
    }

    // --- SECTION 3: SIMPLE ODF API SIMULATIONS ---

    fun simulateMakeTextDoc(title: String, hasImage: Boolean, listItems: List<String>): String {
        addLog("[SimpleAPI] Instantiating blank TextDocument.")
        if (hasImage) {
            addLog("[SimpleAPI] Appending new image: URI(\"odf-logo.png\")")
        }
        addLog("[SimpleAPI] Adding paragraph: \"$title\"")
        if (listItems.isNotEmpty()) {
            addLog("[SimpleAPI] Creating list container.")
            listItems.forEach { item ->
                addLog("[SimpleAPI]   Adding item: \"$item\"")
            }
        }
        addLog("[SimpleAPI] Creating table component: 2 rows x 2 columns.")
        addLog("[SimpleAPI] Setting cell [0,0] string: \"Hello World!\"")
        addLog("[SimpleAPI] Saving document to disk: \"MakeTextDoc.odt\"")
        return "MakeTextDoc.odt"
    }

    fun simulateMakeSheet(startValue: Double, rowMultiplier: Double): String {
        addLog("[SimpleAPI] Instantiating blank SpreadsheetDocument.")
        addLog("[SimpleAPI] Loading first Table sheet index: 0")
        addLog("[SimpleAPI] Setting cell [0,0] label: \"Hello\"")
        for (row in 0 until 5) {
            val cellVal = startValue + row * rowMultiplier
            addLog("[SimpleAPI]   Setting double cell [1,$row] value: $cellVal")
        }
        addLog("[SimpleAPI] Saving spreadsheet to disk: \"makeSheet.ods\"")
        return "makeSheet.ods"
    }

    fun simulateMakeSlides(mainTitle: String, bulletPoints: List<String>): String {
        addLog("[SimpleAPI] Instantiating blank PresentationDocument.")
        addLog("[SimpleAPI] Generating slide 0 layout: TITLE_ONLY")
        addLog("[SimpleAPI] Setting title box content: \"$mainTitle\"")
        
        addLog("[SimpleAPI] Generating slide 1 layout: TITLE_OUTLINE")
        addLog("[SimpleAPI] Setting title box content: \"Overview\"")
        addLog("[SimpleAPI] Creating bullet list in outline textbox.")
        bulletPoints.forEach { point ->
            addLog("[SimpleAPI]   Adding bullet item: \"$point\"")
        }
        
        addLog("[SimpleAPI] Loading figure image: URI(\"skinner.png\")")
        addLog("[SimpleAPI] Positioning layout frame coordinates: x=8.0, y=4.0")
        addLog("[SimpleAPI] Saving presentation slide deck to disk: \"makeSlides.odp\"")
        return "makeSlides.odp"
    }

    fun simulateSlideRearrange(): String {
        addLog("[SimpleAPI] Loading presentation document: \"algs.odp\"")
        addLog("[SimpleAPI] Counting slides inside presentation deck.")
        addLog("[SimpleAPI] Moving first slide (index 0) to position index: 3")
        addLog("[SimpleAPI] Saving modified deck: \"algsMoved.odp\"")
        return "algsMoved.odp"
    }

    fun simulateCombineTexts(): String {
        addLog("[SimpleAPI] Loading source document: \"doc1.odt\"")
        addLog("[SimpleAPI] Loading append target document: \"doc2.odt\"")
        addLog("[SimpleAPI] Inserting page break into doc1.")
        addLog("[SimpleAPI] Fetching final anchor paragraph position.")
        addLog("[SimpleAPI] Appending doc2 contents after anchor paragraph with styles copy.")
        addLog("[SimpleAPI] Saving combined output: \"combined.odt\"")
        return "combined.odt"
    }

    fun simulateCombineSheets(): String {
        addLog("[SimpleAPI] Loading source spreadsheet: \"ss1.ods\"")
        addLog("[SimpleAPI] Loading append spreadsheet: \"ss2.ods\"")
        addLog("[SimpleAPI] Iterating sheets in ss2.ods...")
        addLog("[SimpleAPI]   Appending sheet \"Sheet1\" to ss1.ods...")
        addLog("[SimpleAPI]   Appending sheet \"Sheet2\" to ss1.ods...")
        addLog("[SimpleAPI] Saving combined spreadsheet: \"combined.ods\"")
        return "combined.ods"
    }

    fun simulateCombineDecks(): String {
        addLog("[SimpleAPI] Loading deck1 presentation: \"deck1.odp\"")
        addLog("[SimpleAPI] Loading deck2 presentation: \"deck2.odp\"")
        addLog("[SimpleAPI] Concatenating slides via PresentationDocument.appendPresentation().")
        addLog("[SimpleAPI] Saving combined slides: \"combined.odp\"")
        return "combined.odp"
    }
}
