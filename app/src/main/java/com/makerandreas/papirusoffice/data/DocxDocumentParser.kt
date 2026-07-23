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
                            val tagName = parser.name
                            if (tagName == "text:p" || tagName == "text:h" || tagName == "p" || tagName == "h") {
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
                            val tagName = parser.name
                            if (tagName == "text:p" || tagName == "text:h" || tagName == "p" || tagName == "h") {
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

    suspend fun saveDocument(file: File, text: String): Boolean = withContext(Dispatchers.IO) {
        val fileName = file.name.lowercase()
        val isDocx = fileName.endsWith(".docx") || fileName.endsWith(".docm") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx")
        val isOdt = fileName.endsWith(".odt") || fileName.endsWith(".ods") || fileName.endsWith(".odp")
        
        if (!isDocx && !isOdt) {
            // Write raw text for plain text fallback
            return@withContext try {
                file.writeText(text)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        
        val tempFile = File(context.cacheDir, "temp_save_" + file.name)
        val success = try {
            val targetEntry = if (isDocx) "word/document.xml" else "content.xml"
            
            // Check if file is empty or doesn't exist yet (new document case)
            if (!file.exists() || file.length() == 0L) {
                // If it's a new document, we can write a basic ZIP structure with the target xml entry
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    val newEntry = java.util.zip.ZipEntry(targetEntry)
                    zout.putNextEntry(newEntry)
                    val updatedXmlBytes = if (isDocx) {
                        generateDocxXml(text)
                    } else {
                        generateOdtXml(text)
                    }
                    zout.write(updatedXmlBytes)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(file.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundTarget = false
                        while (entry != null) {
                            val newEntry = java.util.zip.ZipEntry(entry.name)
                            zout.putNextEntry(newEntry)
                            
                            if (entry.name == targetEntry) {
                                foundTarget = true
                                val updatedXmlBytes = if (isDocx) {
                                    generateDocxXml(text)
                                } else {
                                    generateOdtXml(text)
                                }
                                zout.write(updatedXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }
                            
                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }
                        
                        if (!foundTarget) {
                            val newEntry = java.util.zip.ZipEntry(targetEntry)
                            zout.putNextEntry(newEntry)
                            val updatedXmlBytes = if (isDocx) generateDocxXml(text) else generateOdtXml(text)
                            zout.write(updatedXmlBytes)
                            zout.closeEntry()
                        }
                    }
                }
            }
            
            // Copy tempFile back to file
            tempFile.copyTo(file, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
        return@withContext success
    }

    private fun generateDocxXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">\n")
        sb.append("  <w:body>\n")
        
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            val escapedText = escapeXml(p)
            sb.append("    <w:p>\n")
            sb.append("      <w:r>\n")
            sb.append("        <w:t>$escapedText</w:t>\n")
            sb.append("      </w:r>\n")
            sb.append("    </w:p>\n")
        }
        
        sb.append("    <w:sectPr>\n")
        sb.append("      <w:pgSz w:w=\"12240\" w:h=\"15840\"/>\n")
        sb.append("      <w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\" w:header=\"720\" w:footer=\"720\" w:gutter=\"0\"/>\n")
        sb.append("    </w:sectPr>\n")
        sb.append("  </w:body>\n")
        sb.append("</w:document>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")
        
        val paragraphs = text.split("\n")
        for (p in paragraphs) {
            val escapedText = escapeXml(p)
            sb.append("      <text:p>$escapedText</text:p>\n")
        }
        
        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;")
    }
}
