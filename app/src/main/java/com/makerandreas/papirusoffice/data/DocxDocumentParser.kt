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
    val imageExtents: Map<String, Pair<Long, Long>> = emptyMap(),
    val parsedDocument: OfficeParsedDocument? = null
)

class DocxDocumentParser(private val context: Context) {

    private val imageExtractor = DocxImageExtractor(context)
    private val officeParser = OfficeDocumentParser(context)

    val parsingProgress: androidx.lifecycle.LiveData<ParsingProgress> get() = officeParser.parsingProgress

    suspend fun parseDocument(file: File): DocxParseResult = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext DocxParseResult("")

        val fileName = file.name.lowercase()
        if (fileName.endsWith(".docx") || fileName.endsWith(".docm") || fileName.endsWith(".odt") || isZipFile(file)) {
            val parsedDoc = officeParser.parseDocument(file)
            return@withContext DocxParseResult(
                text = parsedDoc.plainText,
                extractedImages = parsedDoc.extractedImages,
                parsedDocument = parsedDoc
            )
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
                var isFirstCellInRow = true

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name
                            val tagLower = tagName.lowercase()
                            when {
                                tagLower == "w:p" || tagLower == "p" -> {
                                    inParagraph = true
                                }
                                tagLower == "w:numpr" || tagLower == "numpr" -> {
                                    currentParagraph.append("• ")
                                }
                                tagLower == "w:t" || tagLower == "t" -> {
                                    inText = true
                                }
                                tagLower == "w:br" || tagLower == "br" || tagLower == "w:cr" || tagLower == "cr" -> {
                                    currentParagraph.append("\n")
                                }
                                tagLower == "w:tab" || tagLower == "tab" -> {
                                    currentParagraph.append("\t")
                                }
                                tagLower == "w:tc" || tagLower == "tc" -> {
                                    if (!isFirstCellInRow) {
                                        currentParagraph.append("\t")
                                    }
                                    isFirstCellInRow = false
                                }
                                tagLower == "w:tr" || tagLower == "tr" -> {
                                    isFirstCellInRow = true
                                }
                                tagLower == "wp:extent" || tagLower == "extent" -> {
                                    val cxStr = parser.getAttributeValue(null, "cx")
                                    val cyStr = parser.getAttributeValue(null, "cy")
                                    val cx = cxStr?.toLongOrNull() ?: 0L
                                    val cy = cyStr?.toLongOrNull() ?: 0L
                                    if (cx > 0 && cy > 0) {
                                        extentsMap["default"] = Pair(cx, cy)
                                    }
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
                            val tagLower = tagName.lowercase()
                            when {
                                tagLower == "w:t" || tagLower == "t" -> {
                                    inText = false
                                }
                                tagLower == "w:p" || tagLower == "p" -> {
                                    inParagraph = false
                                    if (currentParagraph.isNotEmpty()) {
                                        textBuilder.append(currentParagraph.toString()).append("\n\n")
                                        currentParagraph.clear()
                                    }
                                }
                                tagLower == "w:tr" || tagLower == "tr" -> {
                                    if (currentParagraph.isNotEmpty()) {
                                        textBuilder.append(currentParagraph.toString()).append("\n")
                                        currentParagraph.clear()
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                if (currentParagraph.isNotEmpty()) {
                    textBuilder.append(currentParagraph.toString())
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
        val extractedImages = imageExtractor.extractImagesFromOdt(odtFile)
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
                var inTextElement = false
                val currentPara = StringBuilder()
                var isFirstCellInRow = true

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val tagName = parser.name
                            when {
                                tagName == "text:p" || tagName == "text:h" || tagName == "p" || tagName == "h" -> {
                                    inTextElement = true
                                }
                                tagName == "text:list-item" || tagName == "list-item" -> {
                                    currentPara.append("• ")
                                }
                                tagName == "text:line-break" || tagName == "line-break" -> {
                                    currentPara.append("\n")
                                }
                                tagName == "text:tab" || tagName == "tab" -> {
                                    currentPara.append("\t")
                                }
                                tagName == "text:s" || tagName == "s" -> {
                                    val countAttr = parser.getAttributeValue(null, "c") ?: parser.getAttributeValue(null, "text:c")
                                    val count = countAttr?.toIntOrNull() ?: 1
                                    repeat(count) { currentPara.append(" ") }
                                }
                                tagName == "table:table-cell" || tagName == "table-cell" -> {
                                    if (!isFirstCellInRow) {
                                        currentPara.append("\t")
                                    }
                                    isFirstCellInRow = false
                                }
                                tagName == "table:table-row" || tagName == "table-row" -> {
                                    isFirstCellInRow = true
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inTextElement || parser.text.trim().isNotEmpty()) {
                                currentPara.append(parser.text)
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tagName = parser.name
                            when {
                                tagName == "text:p" || tagName == "text:h" || tagName == "p" || tagName == "h" -> {
                                    inTextElement = false
                                    if (currentPara.isNotEmpty()) {
                                        textBuilder.append(currentPara.toString()).append("\n\n")
                                        currentPara.clear()
                                    }
                                }
                                tagName == "table:table-row" || tagName == "table-row" -> {
                                    if (currentPara.isNotEmpty()) {
                                        textBuilder.append(currentPara.toString()).append("\n")
                                        currentPara.clear()
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
                if (currentPara.isNotEmpty()) {
                    textBuilder.append(currentPara.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val resultStr = textBuilder.toString().trim()
        return@withContext DocxParseResult(
            text = if (resultStr.isBlank()) "Empty Document" else resultStr,
            extractedImages = extractedImages
        )
    }

    suspend fun saveDocument(file: File, text: String): Boolean = withContext(Dispatchers.IO) {
        val fileName = file.name.lowercase()
        val isDocx = fileName.endsWith(".docx") || fileName.endsWith(".docm") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx")
        val isOdt = fileName.endsWith(".odt") || fileName.endsWith(".ods") || fileName.endsWith(".odp")
        
        if (isOdt) {
            return@withContext officeParser.saveOdtDocument(file, text)
        }

        if (!isDocx) {
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
                // If it's a new document, create full standard ODT / DOCX ZIP container
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    if (isOdt) {
                        // 1. mimetype (must be uncompressed/stored or first entry in ODT)
                        val mimeEntry = java.util.zip.ZipEntry("mimetype")
                        mimeEntry.method = java.util.zip.ZipEntry.STORED
                        val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                        mimeEntry.size = mimeBytes.size.toLong()
                        val crc = java.util.zip.CRC32()
                        crc.update(mimeBytes)
                        mimeEntry.crc = crc.value
                        zout.putNextEntry(mimeEntry)
                        zout.write(mimeBytes)
                        zout.closeEntry()

                        // 2. META-INF/manifest.xml
                        val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                        zout.putNextEntry(manifestEntry)
                        zout.write(generateOdtManifestXml())
                        zout.closeEntry()

                        // 3. styles.xml
                        val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                        zout.putNextEntry(stylesEntry)
                        zout.write(generateOdtStylesXml())
                        zout.closeEntry()

                        // 4. content.xml
                        val contentEntry = java.util.zip.ZipEntry("content.xml")
                        zout.putNextEntry(contentEntry)
                        zout.write(generateOdtXml(text))
                        zout.closeEntry()
                    } else {
                        // 1. [Content_Types].xml
                        val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                        zout.putNextEntry(ctEntry)
                        zout.write(generateDocxContentTypesXml())
                        zout.closeEntry()

                        // 2. _rels/.rels
                        val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                        zout.putNextEntry(relsEntry)
                        zout.write(generateDocxRelsXml())
                        zout.closeEntry()

                        // 3. word/_rels/document.xml.rels
                        val docRelsEntry = java.util.zip.ZipEntry("word/_rels/document.xml.rels")
                        zout.putNextEntry(docRelsEntry)
                        zout.write(generateDocxDocumentRelsXml())
                        zout.closeEntry()

                        // 4. word/document.xml
                        val newEntry = java.util.zip.ZipEntry(targetEntry)
                        zout.putNextEntry(newEntry)
                        zout.write(generateDocxXml(text))
                        zout.closeEntry()
                    }
                }
            } else {
                java.util.zip.ZipInputStream(file.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundTarget = false
                        var foundMime = false
                        var foundManifest = false
                        var foundStyles = false
                        var foundContentTypes = false
                        var foundRels = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "mimetype") foundMime = true
                            if (entryName == "META-INF/manifest.xml") foundManifest = true
                            if (entryName == "styles.xml") foundStyles = true
                            if (entryName == "[Content_Types].xml") foundContentTypes = true
                            if (entryName == "_rels/.rels") foundRels = true

                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)
                            
                            if (entryName == targetEntry) {
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

                        if (isOdt) {
                            if (!foundManifest) {
                                val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                                zout.putNextEntry(manifestEntry)
                                zout.write(generateOdtManifestXml())
                                zout.closeEntry()
                            }
                            if (!foundStyles) {
                                val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                                zout.putNextEntry(stylesEntry)
                                zout.write(generateOdtStylesXml())
                                zout.closeEntry()
                            }
                        } else if (isDocx) {
                            if (!foundContentTypes) {
                                val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                                zout.putNextEntry(ctEntry)
                                zout.write(generateDocxContentTypesXml())
                                zout.closeEntry()
                            }
                            if (!foundRels) {
                                val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                                zout.putNextEntry(relsEntry)
                                zout.write(generateDocxRelsXml())
                                zout.closeEntry()
                            }
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

    private fun generateOdtManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
 <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.text"/>
 <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
 <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtStylesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" office:version="1.2">
  <office:styles/>
</office:document-styles>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateDocxDocumentRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
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
            sb.append("        <w:t xml:space=\"preserve\">$escapedText</w:t>\n")
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
