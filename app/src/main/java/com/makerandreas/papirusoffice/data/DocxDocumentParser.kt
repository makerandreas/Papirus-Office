package com.makerandreas.papirusoffice.data

import android.content.Context
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

data class DocxParseResult(
    val text: String,
    val extractedImages: Map<String, File> = emptyMap(),
    val imageExtents: Map<String, Pair<Long, Long>> = emptyMap()
)

class DocxDocumentParser(private val context: Context) {

    private val imageExtractor = DocxImageExtractor(context)

    suspend fun parseDocument(file: File): DocxParseResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext DocxParseResult("")

        val fileName = file.name.lowercase()
        if (fileName.endsWith(".docx") || fileName.endsWith(".docm") || isZipFile(file)) {
            return@withContext parseDocxFile(file)
        } else if (fileName.endsWith(".odt")) {
            return@withContext parseOdtFile(file)
        } else {
            // Plain text fallback
            return@withContext try {
                DocxParseResult(file.readText())
            } catch (e: Exception) {
                DocxParseResult("Error reading file: ${e.message}")
            }
        }
    }

    private fun isZipFile(file: File): Boolean {
        return try {
            file.inputStream().use { input ->
                val b1 = input.read()
                val b2 = input.read()
                b1 == 'P'.code && b2 == 'K'.code
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun parseDocxFile(docxFile: File): DocxParseResult = withContext(Dispatchers.IO) {
        val extractedImages = imageExtractor.extractImagesFromDocx(docxFile)
        var documentXmlStream: InputStream? = null
        val extentsMap = mutableMapOf<String, Pair<Long, Long>>()

        val textBuilder = StringBuilder()

        try {
            ZipInputStream(docxFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val byteArray = zip.readBytes()
                        documentXmlStream = byteArray.inputStream()
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            documentXmlStream?.use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var eventType = parser.eventType
                var inParagraph = false
                var inText = false
                val currentParagraph = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name
                            if (tagName.equals("w:p", ignoreCase = true) || tagName.equals("p", ignoreCase = true)) {
                                inParagraph = true
                                currentParagraph.clear()
                            } else if (tagName.equals("w:t", ignoreCase = true) || tagName.equals("t", ignoreCase = true)) {
                                inText = true
                            } else if (tagName.equals("w:br", ignoreCase = true) || tagName.equals("br", ignoreCase = true)) {
                                currentParagraph.append("\n")
                            } else if (tagName.equals("wp:extent", ignoreCase = true) || tagName.equals("extent", ignoreCase = true)) {
                                val cxStr = parser.getAttributeValue(null, "cx")
                                val cyStr = parser.getAttributeValue(null, "cy")
                                val cx = cxStr?.toLongOrNull() ?: 0L
                                val cy = cyStr?.toLongOrNull() ?: 0L
                                if (cx > 0 && cy > 0) {
                                    extentsMap["default"] = Pair(cx, cy)
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inText) {
                                currentParagraph.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tagName = parser.name
                            if (tagName.equals("w:t", ignoreCase = true) || tagName.equals("t", ignoreCase = true)) {
                                inText = false
                            } else if (tagName.equals("w:p", ignoreCase = true) || tagName.equals("p", ignoreCase = true)) {
                                inParagraph = false
                                if (currentParagraph.isNotEmpty()) {
                                    textBuilder.append(currentParagraph.toString()).append("\n\n")
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (textBuilder.isEmpty()) {
                textBuilder.append("Failed to parse DOCX document: ").append(e.localizedMessage)
            }
        }

        val extractedText = textBuilder.toString().trim()
        val finalResultText = if (extractedText.isBlank()) "Empty Document" else extractedText

        return@withContext DocxParseResult(
            text = finalResultText,
            extractedImages = extractedImages,
            imageExtents = extentsMap
        )
    }

    private suspend fun parseOdtFile(odtFile: File): DocxParseResult = withContext(Dispatchers.IO) {
        val textBuilder = StringBuilder()
        try {
            var contentXmlBytes: ByteArray? = null
            ZipInputStream(odtFile.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "content.xml") {
                        contentXmlBytes = zip.readBytes()
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            contentXmlBytes?.inputStream()?.use { stream ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(stream, "UTF-8")

                var eventType = parser.eventType
                var inText = false
                val currentPara = StringBuilder()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name.contains("p", ignoreCase = true) || parser.name.contains("h", ignoreCase = true)) {
                                inText = true
                                currentPara.clear()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inText) {
                                currentPara.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name.contains("p", ignoreCase = true) || parser.name.contains("h", ignoreCase = true)) {
                                inText = false
                                if (currentPara.isNotEmpty()) {
                                    textBuilder.append(currentPara.toString()).append("\n\n")
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val resultStr = textBuilder.toString().trim()
        return@withContext DocxParseResult(if (resultStr.isBlank()) "Empty Document" else resultStr)
    }
}
