package com.makerandreas.papirusoffice.data

import android.content.Context
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

/**
 * Modern document parser for ODT and DOCX files.
 * Uses standard Java ZIP and XML libraries to extract 'content.xml' (ODT) or 'word/document.xml' (DOCX)
 * and parse paragraphs, headings, tables, list items, and images into structured OfficeParsedDocument models.
 * Logs malformed XML structures or unsupported tags into crash.log via DocumentParsingLogger.
 */
class OfficeDocumentParser(private val context: Context) {

    private val imageExtractor = DocxImageExtractor(context)

    /**
     * Extracts raw 'content.xml' from ODT file or 'word/document.xml' from DOCX file
     * using standard Java ZIP input stream.
     */
    suspend fun extractXmlContent(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        val isOdt = file.name.endsWith(".odt", ignoreCase = true)
        val targetEntry = if (isOdt) "content.xml" else "word/document.xml"

        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == targetEntry) {
                        return@withContext zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = "Failed to extract $targetEntry from ZIP container: ${e.localizedMessage}",
                cause = e
            )
        }
        return@withContext ""
    }

    /**
     * Parses the ODT or DOCX file into structured OfficeParsedDocument model
     * mapping paragraphs, headings, list items, tables, and images.
     */
    suspend fun parseDocument(file: File): OfficeParsedDocument = withContext(Dispatchers.IO) {
        val isOdt = file.name.endsWith(".odt", ignoreCase = true)
        val isDocx = file.name.endsWith(".docx", ignoreCase = true) || file.name.endsWith(".doc", ignoreCase = true)

        val extractedImages = if (isOdt) {
            imageExtractor.extractImagesFromOdt(file)
        } else {
            imageExtractor.extractImagesFromDocx(file)
        }

        val xmlContent = extractXmlContent(file)
        if (xmlContent.isBlank()) {
            return@withContext OfficeParsedDocument(
                elements = emptyList(),
                rawXml = "",
                plainText = "",
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = isDocx
            )
        }

        val elements = mutableListOf<OfficeDocumentElement>()
        val plainTextBuilder = StringBuilder()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var inHeading = false
            var headingLevel = 1
            var inTable = false
            val currentRows = mutableListOf<TableRow>()
            val currentCells = mutableListOf<TableCell>()
            val currentCellParagraphs = mutableListOf<OfficeDocumentElement.Paragraph>()

            val currentText = StringBuilder()
            val currentRuns = mutableListOf<TextRun>()
            val currentRunText = StringBuilder()
            var isBold = false
            var isItalic = false
            var isUnderline = false

            // Standard supported tag set for warning/unsupported tag diagnostic logging
            val supportedTags = setOf(
                "p", "h", "text:p", "text:h", "w:p", "w:h", "w:t", "t", "text:span", "w:r",
                "table", "table:table", "w:tbl", "table:table-row", "w:tr", "table:table-cell", "w:tc",
                "text:list-item", "text:list", "w:numpr", "text:line-break", "w:br", "w:cr",
                "text:tab", "w:tab", "text:s", "s", "draw:frame", "draw:image", "w:drawing", "wp:inline",
                "document", "office:document-content", "office:body", "office:text", "w:document", "w:body",
                "w:pPr", "w:rPr", "w:b", "w:i", "w:u", "style:style", "meta-inf/manifest.xml", "styles.xml"
            )

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        val nameLower = name.lowercase()

                        // Check for unknown or custom XML tags to log diagnostic warnings
                        if (name.isNotEmpty() && !supportedTags.contains(name) && !supportedTags.contains(nameLower)) {
                            if (name.contains(":") && !name.startsWith("office:") && !name.startsWith("manifest:")) {
                                val attrMap = mutableMapOf<String, String>()
                                for (i in 0 until parser.attributeCount) {
                                    attrMap[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                                }
                                DocumentParsingLogger.logUnsupportedTag(
                                    context = context,
                                    fileName = file.name,
                                    tagName = name,
                                    attributes = attrMap
                                )
                            }
                        }

                        when {
                            // Headings
                            nameLower == "text:h" || nameLower == "h" -> {
                                inHeading = true
                                val outlineLevel = parser.getAttributeValue(null, "outline-level")
                                    ?: parser.getAttributeValue(null, "text:outline-level")
                                headingLevel = outlineLevel?.toIntOrNull() ?: 1
                                currentText.clear()
                            }

                            // Paragraphs
                            nameLower == "text:p" || nameLower == "w:p" || nameLower == "p" -> {
                                inParagraph = true
                                currentText.clear()
                                currentRuns.clear()
                            }

                            // Text formatting
                            nameLower == "w:b" || nameLower == "b" || nameLower == "style:text-properties" -> {
                                isBold = true
                            }
                            nameLower == "w:i" || nameLower == "i" -> {
                                isItalic = true
                            }
                            nameLower == "w:u" || nameLower == "u" -> {
                                isUnderline = true
                            }

                            // Tables
                            nameLower == "table:table" || nameLower == "w:tbl" || nameLower == "table" -> {
                                inTable = true
                                currentRows.clear()
                            }
                            nameLower == "table:table-row" || nameLower == "w:tr" || nameLower == "tr" -> {
                                currentCells.clear()
                            }
                            nameLower == "table:table-cell" || nameLower == "w:tc" || nameLower == "tc" -> {
                                currentCellParagraphs.clear()
                                currentText.clear()
                            }

                            // Lists
                            nameLower == "text:list-item" || nameLower == "w:numpr" -> {
                                currentText.append("• ")
                            }

                            // Spaces & Tabs
                            nameLower == "text:s" || nameLower == "s" -> {
                                val countAttr = parser.getAttributeValue(null, "c")
                                    ?: parser.getAttributeValue(null, "text:c")
                                val count = countAttr?.toIntOrNull() ?: 1
                                repeat(count) { currentText.append(" ") }
                            }
                            nameLower == "text:tab" || nameLower == "w:tab" || nameLower == "tab" -> {
                                currentText.append("\t")
                            }
                            nameLower == "text:line-break" || nameLower == "w:br" || nameLower == "w:cr" -> {
                                currentText.append("\n")
                            }

                            // Images
                            nameLower == "draw:image" -> {
                                val href = parser.getAttributeValue(null, "href")
                                    ?: parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                                if (!href.isNullOrBlank()) {
                                    val imgName = href.substringAfterLast("/")
                                    val imgFile = extractedImages[imgName] ?: extractedImages[href]
                                    elements.add(
                                        OfficeDocumentElement.ImageElement(
                                            imagePath = href,
                                            imageFile = imgFile
                                        )
                                    )
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val txt = parser.text ?: ""
                        if (txt.isNotEmpty()) {
                            currentText.append(txt)
                            currentRunText.append(txt)
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        val nameLower = name.lowercase()

                        when {
                            nameLower == "text:h" || nameLower == "h" -> {
                                inHeading = false
                                val headingText = currentText.toString().trim()
                                if (headingText.isNotEmpty()) {
                                    elements.add(
                                        OfficeDocumentElement.Heading(
                                            text = headingText,
                                            level = headingLevel
                                        )
                                    )
                                    plainTextBuilder.append(headingText).append("\n\n")
                                }
                                currentText.clear()
                            }

                            nameLower == "text:p" || nameLower == "w:p" || nameLower == "p" -> {
                                inParagraph = false
                                val paraText = currentText.toString().trim()
                                if (paraText.isNotEmpty()) {
                                    val paragraphObj = OfficeDocumentElement.Paragraph(
                                        text = paraText,
                                        runs = if (currentRuns.isNotEmpty()) currentRuns.toList() else listOf(
                                            TextRun(paraText, isBold, isItalic, isUnderline)
                                        )
                                    )
                                    if (inTable) {
                                        currentCellParagraphs.add(paragraphObj)
                                    } else {
                                        elements.add(paragraphObj)
                                        plainTextBuilder.append(paraText).append("\n\n")
                                    }
                                }
                                currentText.clear()
                                currentRuns.clear()
                                isBold = false
                                isItalic = false
                                isUnderline = false
                            }

                            nameLower == "table:table-cell" || nameLower == "w:tc" || nameLower == "tc" -> {
                                val cellText = if (currentCellParagraphs.isNotEmpty()) {
                                    currentCellParagraphs.joinToString("\n") { it.text }
                                } else {
                                    currentText.toString().trim()
                                }
                                currentCells.add(
                                    TableCell(
                                        text = cellText,
                                        paragraphs = currentCellParagraphs.toList()
                                    )
                                )
                                currentCellParagraphs.clear()
                                currentText.clear()
                            }

                            nameLower == "table:table-row" || nameLower == "w:tr" || nameLower == "tr" -> {
                                if (currentCells.isNotEmpty()) {
                                    currentRows.add(TableRow(cells = currentCells.toList()))
                                    currentCells.clear()
                                }
                            }

                            nameLower == "table:table" || nameLower == "w:tbl" || nameLower == "table" -> {
                                inTable = false
                                if (currentRows.isNotEmpty()) {
                                    val maxCols = currentRows.maxOfOrNull { it.cells.size } ?: 0
                                    val tableObj = OfficeDocumentElement.Table(
                                        rows = currentRows.toList(),
                                        numColumns = maxCols
                                    )
                                    elements.add(tableObj)

                                    currentRows.forEach { row ->
                                        plainTextBuilder.append(row.cells.joinToString("\t") { it.text }).append("\n")
                                    }
                                    plainTextBuilder.append("\n")
                                    currentRows.clear()
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

        } catch (e: Exception) {
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = e.message ?: "Unknown XML parsing error",
                cause = e
            )
        }

        val plainTextResult = plainTextBuilder.toString().trim()
        return@withContext OfficeParsedDocument(
            elements = elements,
            rawXml = xmlContent,
            plainText = if (plainTextResult.isBlank()) "Empty Document" else plainTextResult,
            extractedImages = extractedImages,
            isOdt = isOdt,
            isDocx = isDocx
        )
    }

    /**
     * Saves or creates a valid ODT ZIP package containing properly formatted 'content.xml',
     * 'mimetype', 'META-INF/manifest.xml', 'styles.xml', and 'meta.xml'.
     */
    suspend fun saveOdtDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdtDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveOdtDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdtDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveOdtDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_odt_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdtContentXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new ODT Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. mimetype (MUST be uncompressed STORED entry per ODF spec)
                    val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                    val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                        method = java.util.zip.ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                    }
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

                    // 4. meta.xml
                    val metaEntry = java.util.zip.ZipEntry("meta.xml")
                    zout.putNextEntry(metaEntry)
                    zout.write(generateOdtMetaXml())
                    zout.closeEntry()

                    // 5. content.xml
                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing ODT file in-place
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundContent = false
                        var foundManifest = false
                        var foundStyles = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "META-INF/manifest.xml") foundManifest = true
                            if (entryName == "styles.xml") foundStyles = true

                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)

                            if (entryName == "content.xml") {
                                foundContent = true
                                zout.write(contentXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundContent) {
                            val contentEntry = java.util.zip.ZipEntry("content.xml")
                            zout.putNextEntry(contentEntry)
                            zout.write(contentXmlBytes)
                            zout.closeEntry()
                        }
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
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODT Save Error",
                exceptionType = "OdtPackageSaveException",
                message = "Failed to create/update ODT ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdtContentXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:draw:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")

        if (elements.isNotEmpty()) {
            for (element in elements) {
                when (element) {
                    is OfficeDocumentElement.Heading -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:h text:outline-level=\"${element.level}\">$esc</text:h>\n")
                    }
                    is OfficeDocumentElement.Paragraph -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:p>$esc</text:p>\n")
                    }
                    is OfficeDocumentElement.ListItem -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:list><text:list-item><text:p>$esc</text:p></text:list-item></text:list>\n")
                    }
                    is OfficeDocumentElement.Table -> {
                        sb.append("      <table:table table:name=\"Table1\">\n")
                        for (row in element.rows) {
                            sb.append("        <table:table-row>\n")
                            for (cell in row.cells) {
                                val escCell = escapeXml(cell.text)
                                sb.append("          <table:table-cell office:value-type=\"string\">\n")
                                sb.append("            <text:p>$escCell</text:p>\n")
                                sb.append("          </table:table-cell>\n")
                            }
                            sb.append("        </table:table-row>\n")
                        }
                        sb.append("      </table:table>\n")
                    }
                    is OfficeDocumentElement.ImageElement -> {
                        sb.append("      <draw:frame draw:name=\"Image1\">\n")
                        sb.append("        <draw:image xlink:href=\"${escapeXml(element.imagePath)}\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>\n")
                        sb.append("      </draw:frame>\n")
                    }
                }
            }
        } else {
            val lines = text.split("\n")
            for (line in lines) {
                val esc = escapeXml(line)
                sb.append("      <text:p>$esc</text:p>\n")
            }
        }

        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.text"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
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

    private fun generateOdtMetaXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
  <office:meta>
    <dc:title>Papirus Document</dc:title>
    <meta:generator>Papirus Office Parser</meta:generator>
  </office:meta>
</office:document-meta>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
