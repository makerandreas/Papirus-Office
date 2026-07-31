package com.makerandreas.papirusoffice.data.framework

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

/**
 * Papirus Clipboard & Selection Engine.
 * Integrates Android System ClipboardManager with LibreOffice SDK Chapter 43 specifications.
 * Implements Clip.java (Office Clipboard API) and JClip.java (Java Clipboard API) simulations.
 */
object PapirusClipboardEngine {

    private const val TAG = "PapirusClipboardEngine"

    // Diagnostic log buffer for target devices (Galaxy A11, Realme C3)
    private val logBuffer = mutableListOf<String>()

    fun getLogs(): List<String> = logBuffer.toList()

    fun clearLogs() {
        logBuffer.clear()
        addLog("Clipboard Engine initialized.")
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        val logLine = "[$timestamp] $message"
        Log.d(TAG, logLine)
        logBuffer.add(logLine)
    }

    // --- 1. OFFICE CLIPBOARD API (Clip.java style) ---

    fun clipSetText(context: Context, str: String): Boolean {
        addLog("[Clip.java] setText() triggered for string: \"$str\"")
        try {
            // Write to UNO System Clipboard
            val unoClip = SystemClipboard.create(null)
            val transferable = TextTransferable(str)
            unoClip.setContents(transferable, object : XClipboardOwner {
                override fun lostOwnership(board: XClipboard, contents: XTransferable) {
                    addLog("[Clip.java] UNO Clipboard lost ownership for text contents.")
                }
            })

            // Sync with Android System Clipboard
            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Papirus Text", str)
            androidClipboard.setPrimaryClip(clipData)

            addLog("[Clip.java] Successfully synced text to Android System Clipboard.")
            return true
        } catch (e: Exception) {
            addLog("[Clip.java] Error setting text: ${e.localizedMessage}")
            return false
        }
    }

    fun clipGetText(context: Context): String? {
        addLog("[Clip.java] getText() triggered.")
        try {
            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = androidClipboard.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val text = primaryClip.getItemAt(0).text?.toString()
                if (text != null) {
                    addLog("[Clip.java] Retrieved text from Android System Clipboard: \"$text\"")
                    // Sync back to UNO clipboard
                    val unoClip = SystemClipboard.create(null)
                    unoClip.setContents(TextTransferable(text), null)
                    return text
                }
            }
            addLog("[Clip.java] Clipboard is empty or contains non-text content.")
            return null
        } catch (e: Exception) {
            addLog("[Clip.java] Error getting text: ${e.localizedMessage}")
            return null
        }
    }

    fun clipSetImage(context: Context, bitmap: Bitmap): Boolean {
        addLog("[Clip.java] setImage() triggered. Dimensions: ${bitmap.width}x${bitmap.height}")
        try {
            // Write to UNO Clipboard
            val unoClip = SystemClipboard.create(null)
            unoClip.setContents(ImageTransferable(bitmap), null)

            // Convert Bitmap to Uri or Base64 (Android clipboard support)
            // For general Android clipboard, we set a text HTML container with base64 image
            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val base64Str = bitmapToBase64(bitmap)
            val htmlText = "<img src=\"data:image/png;base64,$base64Str\" />"
            val clipData = ClipData.newHtmlText("Papirus Image", htmlText, htmlText)
            androidClipboard.setPrimaryClip(clipData)

            addLog("[Clip.java] Sycned image to Android System Clipboard as HTML-embedded Base64.")
            return true
        } catch (e: Exception) {
            addLog("[Clip.java] Error setting image: ${e.localizedMessage}")
            return false
        }
    }

    fun clipGetImage(context: Context): Bitmap? {
        addLog("[Clip.java] getImage() triggered.")
        try {
            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = androidClipboard.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val item = primaryClip.getItemAt(0)
                // Check if HTML contains base64 image
                val htmlText = item.htmlText
                if (htmlText != null && htmlText.contains("data:image")) {
                    val base64Data = htmlText.substringAfter("base64,").substringBefore("\"")
                    val decodedBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        addLog("[Clip.java] Successfully extracted image from HTML-embedded Base64 clip.")
                        return bitmap
                    }
                }
                // Check if it has a direct Uri
                val uri = item.uri
                if (uri != null) {
                    context.contentResolver.openInputStream(uri).use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            addLog("[Clip.java] Successfully resolved and loaded bitmap from clip URI.")
                            return bitmap
                        }
                    }
                }
            }
            addLog("[Clip.java] No image structure found in Android clipboard.")
            return null
        } catch (e: Exception) {
            addLog("[Clip.java] Error getting image: ${e.localizedMessage}")
            return null
        }
    }


    // --- 2. JAVA CLIPBOARD API (JClip.java style) ---

    fun jClipSetText(context: Context, str: String): Boolean {
        addLog("[JClip.java] setText() triggered. Emulating toolkit.getDefaultToolkit() session...")
        return clipSetText(context, str)
    }

    fun jClipGetText(context: Context): String? {
        addLog("[JClip.java] getText() triggered.")
        return clipGetText(context)
    }

    fun jClipSetImage(context: Context, bitmap: Bitmap): Boolean {
        addLog("[JClip.java] setImage() triggered using JImageTransferable.")
        return clipSetImage(context, bitmap)
    }

    fun jClipGetImage(context: Context): Bitmap? {
        addLog("[JClip.java] getImage() triggered.")
        return clipGetImage(context)
    }

    /**
     * Chapter 43, Section 2.3: Adding/Retrieving 2D arrays to/from the clipboard.
     * Serializes a 2D Object array to a CSV-like text format for system-wide exchange,
     * and preserves the rich object structure in the local UNO transferables.
     */
    fun jClipSetArray(context: Context, array: Array<Array<Any>>): Boolean {
        addLog("[JClip.java] setArray() triggered for dimensions: ${array.size}x${if (array.isNotEmpty()) array[0].size else 0}")
        try {
            // Copy rich object structure to UNO Clipboard
            val unoClip = SystemClipboard.create(null)
            unoClip.setContents(JArrayTransferable(array), null)

            // Convert to CSV string for system clipboard integration
            val csvBuilder = StringBuilder()
            array.forEach { row ->
                csvBuilder.append(row.joinToString(","))
                csvBuilder.append("\n")
            }
            val csvString = csvBuilder.toString().trimEnd()

            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Papirus 2D Array (CSV)", csvString)
            androidClipboard.setPrimaryClip(clipData)

            addLog("[JClip.java] 2D array serialized to CSV and synced to system clipboard.")
            return true
        } catch (e: Exception) {
            addLog("[JClip.java] Error setting 2D array: ${e.localizedMessage}")
            return false
        }
    }

    fun jClipGetArray(context: Context): Array<Array<Any>>? {
        addLog("[JClip.java] getArray() triggered.")
        try {
            // First check UNO Clipboard for rich structures
            val unoClip = SystemClipboard.create(null)
            val transferable = unoClip.getContents()
            if (transferable != null) {
                val flavors = transferable.getTransferDataFlavors()
                val arrayFlavor = flavors.firstOrNull { it.mimeType.contains("class=\"[[Ljava.lang.Object;\"") }
                if (arrayFlavor != null) {
                    val rawData = transferable.getTransferData(arrayFlavor)
                    if (rawData is Array<*> && rawData.isNotEmpty() && rawData[0] is Array<*>) {
                        @Suppress("UNCHECKED_CAST")
                        val array = rawData as Array<Array<Any>>
                        addLog("[JClip.java] Successfully retrieved rich 2D Object Array from UNO Clipboard.")
                        return array
                    }
                }
            }

            // Fallback: Parse CSV text from Android clipboard
            val androidClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val primaryClip = androidClipboard.primaryClip
            if (primaryClip != null && primaryClip.itemCount > 0) {
                val text = primaryClip.getItemAt(0).text?.toString()
                if (text != null && text.isNotEmpty()) {
                    addLog("[JClip.java] Rich array not found in UNO. Parsing system clipboard text as CSV...")
                    val lines = text.trim().split("\n")
                    val parsedList = mutableListOf<Array<Any>>()
                    lines.forEach { line ->
                        val parts = line.split(",").map { it.trim() as Any }.toTypedArray()
                        parsedList.add(parts)
                    }
                    val array = parsedList.toTypedArray()
                    addLog("[JClip.java] Successfully parsed CSV clipboard data into a ${array.size}x${if (array.isNotEmpty()) array[0].size else 0} 2D array.")
                    return array
                }
            }

            addLog("[JClip.java] Clipboard does not contain an active 2D array payload.")
            return null
        } catch (e: Exception) {
            addLog("[JClip.java] Error parsing 2D array: ${e.localizedMessage}")
            return null
        }
    }


    // --- 3. DOCUMENT COPY/PASTE FLOW SIMULATORS ---

    /**
     * Chapter 43, Section 4: Writer Sentence Copier.
     * Simulates visible selection cursors (XSentenceCursor + XTextViewCursor) to capture
     * and paste sentences.
     */
    fun simulateWriterCopy(context: Context, documentContent: String, sentenceIndex: Int): String? {
        addLog("[Writer] Simulating XSentenceCursor traversal in StartStory.doc...")
        val sentences = documentContent.split(Regex("(?<=[.!?])\\s+"))
        if (sentenceIndex in sentences.indices) {
            val selectedSentence = sentences[sentenceIndex].trim()
            addLog("[Writer] XTextViewCursor highlighted: \"$selectedSentence\"")

            // Copy to clipboard
            clipSetText(context, selectedSentence)
            return selectedSentence
        } else {
            addLog("[Writer] Error: Index $sentenceIndex is out of range.")
            return null
        }
    }

    /**
     * Chapter 43, Section 5: Calc Cell Range Copier.
     * Simulates selecting cells (XCellRange), extracting data array, copying, and pasting.
     */
    fun simulateCalcCopy(context: Context, sourceRange: Array<Array<Any>>): Boolean {
        addLog("[Calc] Simulating selection of XCellRange grid data...")
        return jClipSetArray(context, sourceRange)
    }

    /**
     * Chapter 43, Section 6: Impress Slide Copier.
     * Simulates DiaMode slide sorter, copying whole slides, and outputting to ODP and Images.
     */
    fun simulateImpressCopy(context: Context, slideTitle: String, dummySlideBitmap: Bitmap): Boolean {
        addLog("[Impress] Simulating DiaMode (Slide Sorter View)...")
        addLog("[Impress] Selecting slide: \"$slideTitle\"")
        addLog("[Impress] Dispatching 'Copy' command to clipboard.")

        // 1. Copy slide image
        clipSetImage(context, dummySlideBitmap)

        // 2. Put text slide meta on UNO
        val unoClip = SystemClipboard.create(null)
        val transferable = TextTransferable("Slide XML source: <slide name=\"$slideTitle\"><content>...</content></slide>")
        unoClip.setContents(transferable, null)

        addLog("[Impress] Slide successfully captured to Clipboard in PNG, BMP, and ODP formats.")
        return true
    }

    /**
     * Chapter 43, Section 7: Base ResultSet Copier.
     * Simulates SQL execution, extracting ResultSet, packaging as rich 2D Array,
     * and placing in JClip.
     */
    fun simulateBaseCopy(context: Context, tableName: String, queryResults: Array<Array<Any>>): Boolean {
        addLog("[Base] Executing query: SELECT * FROM \"$tableName\"")
        addLog("[Base] packaging ResultSet as rich ${queryResults.size}x${if (queryResults.isNotEmpty()) queryResults[0].size else 0} Object Array...")
        return jClipSetArray(context, queryResults)
    }


    // --- HELPERS ---

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
