package com.makerandreas.papirusoffice.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

data class ParsingProgress(
    val percentage: Int = 0,
    val statusMessage: String = "Loading document...",
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

sealed class SchemaValidationResult {
    object Valid : SchemaValidationResult()
    data class Invalid(val reason: String, val warnings: List<String> = emptyList()) : SchemaValidationResult()
}

/**
 * Modern document parser for ODT and DOCX files.
 * Uses standard Java ZIP and XML libraries to extract 'content.xml' (ODT) or 'word/document.xml' (DOCX)
 * and parse paragraphs, headings, tables, list items, and images into structured OfficeParsedDocument models.
 * Logs malformed XML structures or unsupported tags into crash.log via DocumentParsingLogger.
 */
class OfficeDocumentParser(private val context: Context) {

    private val cacheRepository = com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository(context)
    private val imageExtractor = DocxImageExtractor(context)

    private val _parsingProgress = MutableLiveData<ParsingProgress>(ParsingProgress(0, "Loading document..."))
    val parsingProgress: LiveData<ParsingProgress> get() = _parsingProgress

    /**
     * Validates XML structure integrity against ODF/OOXML standard schema expectations.
     * Logs structural anomalies and warnings to crash.log via DocumentParsingLogger.
     */
    fun validateXmlSchema(
        xmlContent: String,
        isOdt: Boolean,
        isDocx: Boolean,
        fileName: String,
        isOds: Boolean = false,
        isXlsx: Boolean = false,
        isOdp: Boolean = false,
        isPptx: Boolean = false
    ): SchemaValidationResult {
        val warnings = mutableListOf<String>()
        if (xmlContent.isBlank()) {
            val errorMsg = "XML content is empty or corrupted in $fileName."
            DocumentParsingLogger.logMalformedXml(context, fileName, errorMsg)
            return SchemaValidationResult.Invalid(errorMsg)
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType
            var rootTag: String? = null
            var hasBody = false
            var tagCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name ?: ""
                    val tagNameLower = tagName.lowercase()
                    tagCount++

                    if (rootTag == null) {
                        rootTag = tagName
                        if ((isOdt || isOds || isOdp) && !tagNameLower.contains("document") && !tagNameLower.contains("content") && !tagNameLower.contains("presentation") && !tagNameLower.contains("page")) {
                            val formatName = if (isOds) "ODS" else if (isOdp) "ODP" else "ODT"
                            val msg = "$formatName Schema Deviation: Root tag <$tagName> does not match ODF standard."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "$formatName Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isDocx && !tagNameLower.endsWith("document")) {
                            val msg = "DOCX Schema Deviation: Root tag <$tagName> does not match OpenXML standard (<w:document>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "DOCX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isXlsx && !tagNameLower.contains("worksheet") && !tagNameLower.contains("workbook")) {
                            val msg = "XLSX Schema Deviation: Root tag <$tagName> does not match OpenXML standard (<worksheet>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "XLSX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isPptx && !tagNameLower.contains("sld") && !tagNameLower.contains("presentation")) {
                            val msg = "PPTX Schema Deviation: Root tag <$tagName> does not match OpenXML Presentation standard (<p:sld>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "PPTX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        }
                    }

                    if (tagNameLower.endsWith("body") || tagNameLower.endsWith("text") || tagNameLower.endsWith("spreadsheet") || tagNameLower.endsWith("sheetdata") || tagNameLower.endsWith("worksheet") || tagNameLower.contains("sld") || tagNameLower.contains("presentation") || tagNameLower.contains("page")) {
                        hasBody = true
                    }
                }
                eventType = parser.next()
            }

            if (tagCount == 0) {
                val errorMsg = "XML structure contains no valid tags in file $fileName"
                DocumentParsingLogger.logMalformedXml(context, fileName, errorMsg)
                return SchemaValidationResult.Invalid(errorMsg)
            }

            if (isOdt && !hasBody) {
                val msg = "ODT Structural Deviation: Missing <office:body> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "ODT Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isOds && !hasBody) {
                val msg = "ODS Structural Deviation: Missing <office:body> or <office:spreadsheet> element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "ODS Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isDocx && !hasBody) {
                val msg = "DOCX Structural Deviation: Missing <w:body> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "DOCX Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isXlsx && !hasBody) {
                val msg = "XLSX Structural Deviation: Missing <worksheet> or <sheetData> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "XLSX Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            return SchemaValidationResult.Valid

        } catch (e: Exception) {
            val errorMsg = "XML Schema Validation Failed: ${e.localizedMessage ?: "Syntax error"}"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = fileName,
                errorMsg = errorMsg,
                cause = e
            )
            return SchemaValidationResult.Invalid(errorMsg, warnings)
        }
    }

    /**
     * Pre-processing sanitization that removes invalid non-UTF-8 characters,
     * illegal control characters, and Unicode replacement characters (\uFFFD) from XML structure.
     * Logs any removed characters to the Crash Log system via DocumentParsingLogger.
     */
    fun sanitizeXmlContent(rawXml: String, fileName: String): String {
        if (rawXml.isEmpty()) return rawXml

        val sanitizedBuilder = java.lang.StringBuilder(rawXml.length)
        var removedCount = 0
        val removedSampleHex = mutableListOf<String>()

        var i = 0
        val length = rawXml.length
        while (i < length) {
            val codePoint = rawXml.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Valid XML 1.0 character specification:
            // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            val isValidXmlChar = (codePoint == 0x9) ||
                    (codePoint == 0xA) ||
                    (codePoint == 0xD) ||
                    (codePoint in 0x20..0xD7FF) ||
                    (codePoint in 0xE000..0xFFFD && codePoint != 0xFFFD) || // Exclude \uFFFD (bad UTF-8 byte)
                    (codePoint in 0x10000..0x10FFFF)

            if (isValidXmlChar) {
                sanitizedBuilder.appendCodePoint(codePoint)
            } else {
                removedCount++
                if (removedSampleHex.size < 5) {
                    removedSampleHex.add(String.format("U+%04X", codePoint))
                }
            }
            i += charCount
        }

        if (removedCount > 0) {
            val samples = removedSampleHex.joinToString(", ")
            DocumentParsingLogger.logError(
                context = context,
                tag = "XmlSanitization",
                exceptionType = "NonUtf8XmlSanitized",
                message = "Sanitized $removedCount invalid non-UTF-8 or illegal XML character(s) from $fileName.",
                details = "File: $fileName, Removed Count: $removedCount, Sample Hex: [$samples]"
            )
        }

        return sanitizedBuilder.toString()
    }

    /**
     * Extracts raw 'content.xml' from ODT/ODS file, 'word/document.xml' from DOCX file,
     * or 'xl/worksheets/sheet1.xml' from XLSX file using standard Java ZIP input stream.
     */
    suspend fun extractXmlContent(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        val isOdf = file.name.endsWith(".odt", ignoreCase = true) ||
                file.name.endsWith(".ods", ignoreCase = true) ||
                file.name.endsWith(".odp", ignoreCase = true)
        val isXlsx = file.name.endsWith(".xlsx", ignoreCase = true) ||
                file.name.endsWith(".xlsm", ignoreCase = true)
        val isPptx = file.name.endsWith(".pptx", ignoreCase = true) ||
                file.name.endsWith(".pptm", ignoreCase = true)

        val targetEntry = if (isOdf) "content.xml" else if (isXlsx) "xl/worksheets/sheet1.xml" else if (isPptx) "ppt/slides/slide1.xml" else "word/document.xml"

        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                var fallbackSheetContent: String? = null
                var fallbackSlideContent: String? = null
                var contentXmlFound: String? = null
                var wordDocumentFound: String? = null
                val slideContents = mutableListOf<String>()

                while (entry != null) {
                    val name = entry.name
                    if (name == targetEntry) {
                        val rawContent = zip.readBytes().toString(Charsets.UTF_8)
                        return@withContext sanitizeXmlContent(rawContent, file.name)
                    }
                    if (name == "content.xml") {
                        contentXmlFound = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (name == "word/document.xml") {
                        wordDocumentFound = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith("xl/worksheets/sheet") && fallbackSheetContent == null) {
                        fallbackSheetContent = zip.readBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith("ppt/slides/slide")) {
                        slideContents.add(zip.readBytes().toString(Charsets.UTF_8))
                    } else if (name == "ppt/presentation.xml" && fallbackSlideContent == null) {
                        fallbackSlideContent = zip.readBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                if (slideContents.isNotEmpty()) {
                    if (slideContents.size == 1) {
                        return@withContext sanitizeXmlContent(slideContents[0], file.name)
                    } else {
                        val composite = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<presentation>\n")
                        for (s in slideContents) {
                            composite.append(s).append("\n")
                        }
                        composite.append("</presentation>")
                        return@withContext sanitizeXmlContent(composite.toString(), file.name)
                    }
                }
                if (contentXmlFound != null) return@withContext sanitizeXmlContent(contentXmlFound, file.name)
                if (wordDocumentFound != null) return@withContext sanitizeXmlContent(wordDocumentFound, file.name)
                if (fallbackSheetContent != null) return@withContext sanitizeXmlContent(fallbackSheetContent, file.name)
                if (fallbackSlideContent != null) return@withContext sanitizeXmlContent(fallbackSlideContent, file.name)
            }
        } catch (e: Exception) {
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = "Failed to extract XML content from ZIP container: ${e.localizedMessage}",
                cause = e
            )
        }
        return@withContext ""
    }

    /**
     * Parses the ODT, ODS, DOCX, or XLSX file into structured OfficeParsedDocument model
     * mapping paragraphs, headings, list items, tables, and images.
     */
    suspend fun parseDocument(file: File): OfficeParsedDocument = withContext(Dispatchers.IO) {
        // Fast path: Check Room cache first
        val cached = cacheRepository.getCachedDocument(file)
        if (cached != null) {
            val statusMsg = try {
                context.getString(com.example.R.string.loading_status_cached)
            } catch (e: Exception) {
                "Loading document from local cache..."
            }
            _parsingProgress.postValue(ParsingProgress(100, statusMsg))

            val cachedElements = if (cached.plainText.isNotBlank()) {
                cached.plainText.split("\n\n").map { block ->
                    OfficeDocumentElement.Paragraph(text = block)
                }
            } else {
                emptyList()
            }
            return@withContext OfficeParsedDocument(
                elements = cachedElements,
                rawXml = "",
                plainText = cached.plainText,
                extractedImages = emptyMap(),
                isOdt = cached.format == "ODT",
                isDocx = cached.format == "DOCX",
                isOds = cached.format == "ODS",
                isXlsx = cached.format == "XLSX",
                isOdp = cached.format == "ODP",
                isPptx = cached.format == "PPTX",
                isParsingFailed = cached.isParsingFailed,
                failureReason = cached.failureReason
            )
        }

        _parsingProgress.postValue(ParsingProgress(10, context.getString(com.example.R.string.loading_status_initial)))

        val isOdt = file.name.endsWith(".odt", ignoreCase = true)
        val isOds = file.name.endsWith(".ods", ignoreCase = true)
        val isOdp = file.name.endsWith(".odp", ignoreCase = true)
        val isDocx = file.name.endsWith(".docx", ignoreCase = true) || file.name.endsWith(".doc", ignoreCase = true)
        val isXlsx = file.name.endsWith(".xlsx", ignoreCase = true) || file.name.endsWith(".xlsm", ignoreCase = true)
        val isPptx = file.name.endsWith(".pptx", ignoreCase = true) || file.name.endsWith(".pptm", ignoreCase = true)

        val xmlContent = extractXmlContent(file)
        val detectedPptx = isPptx || xmlContent.contains("<p:sld") || xmlContent.contains("<p:presentation")
        val detectedOdp = isOdp || xmlContent.contains("<office:presentation") || xmlContent.contains("<draw:page")

        _parsingProgress.postValue(ParsingProgress(25, context.getString(com.example.R.string.loading_status_validating)))

        val validation = validateXmlSchema(
            xmlContent = xmlContent,
            isOdt = isOdt,
            isDocx = isDocx,
            fileName = file.name,
            isOds = isOds,
            isXlsx = isXlsx,
            isOdp = detectedOdp,
            isPptx = detectedPptx
        )
        if (validation is SchemaValidationResult.Invalid) {
            val failProgress = ParsingProgress(
                percentage = 0,
                statusMessage = context.getString(com.example.R.string.doc_open_failed_title),
                isFailed = true,
                errorMessage = validation.reason
            )
            _parsingProgress.postValue(failProgress)
            return@withContext OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = emptyMap(),
                isOdt = isOdt,
                isDocx = isDocx,
                isOds = isOds,
                isXlsx = isXlsx,
                isOdp = detectedOdp,
                isPptx = detectedPptx,
                isParsingFailed = true,
                failureReason = validation.reason
            )
        }

        _parsingProgress.postValue(ParsingProgress(45, context.getString(com.example.R.string.loading_status_extracting)))
        val extractedImages = if (isOdt) {
            imageExtractor.extractImagesFromOdt(file)
        } else {
            imageExtractor.extractImagesFromDocx(file)
        }

        _parsingProgress.postValue(ParsingProgress(60, context.getString(com.example.R.string.loading_status_processing)))

        if (isOdt || isOds || detectedOdp) {
            val odfImport = com.makerandreas.papirusoffice.data.odf.SvXMLImport(context, extractedImages)
            val parsedDoc = odfImport.parseOdfXml(
                xmlContent = xmlContent,
                fileName = file.name,
                isOdt = isOdt,
                isOds = isOds,
                isOdp = detectedOdp
            )
            if (!parsedDoc.isParsingFailed) {
                _parsingProgress.postValue(ParsingProgress(100, context.getString(com.example.R.string.loading_status_completed)))
                cacheRepository.saveCachedDocument(file, parsedDoc)
                return@withContext parsedDoc
            }
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
            var eventCount = 0

            // Standard supported tag set for warning/unsupported tag diagnostic logging
            val supportedTags = setOf(
                "p", "h", "text:p", "text:h", "w:p", "w:h", "w:t", "t", "a:t", "a:p", "a:r", "text:span", "w:r",
                "table", "table:table", "w:tbl", "table:table-row", "w:tr", "table:table-cell", "w:tc",
                "text:list-item", "text:list", "w:numpr", "text:line-break", "w:br", "w:cr",
                "text:tab", "w:tab", "text:s", "s", "draw:frame", "draw:image", "w:drawing", "wp:inline",
                "document", "office:document-content", "office:body", "office:text", "office:spreadsheet", "office:presentation",
                "draw:page", "draw:text-box", "p:sld", "p:sp", "p:txbody", "p:presentation", "p:sldid", "p:sldidlst",
                "table:table-column", "table:table-header-rows", "table:covered-table-cell",
                "office:value", "office:value-type", "table:number-columns-repeated", "table:number-rows-repeated",
                "worksheet", "sheetdata", "row", "c", "v", "f", "t", "is", "r", "inlinestr", "dimension", "cols", "col",
                "sheetviews", "sheetview", "selection", "pagemargins", "[content_types].xml", "_rels/.rels",
                "w:document", "w:body", "w:pPr", "w:rPr", "w:b", "w:i", "w:u", "style:style", "meta-inf/manifest.xml", "styles.xml"
            )

            while (eventType != XmlPullParser.END_DOCUMENT) {
                eventCount++
                if (eventCount % 40 == 0) {
                    if (eventCount > 150) {
                        _parsingProgress.postValue(
                            ParsingProgress(85, context.getString(com.example.R.string.loading_status_still_processing))
                        )
                    } else {
                        _parsingProgress.postValue(
                            ParsingProgress(75, context.getString(com.example.R.string.loading_status_processing))
                        )
                    }
                }
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
                            nameLower == "text:p" || nameLower == "w:p" || nameLower == "p" || nameLower == "a:p" -> {
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

                            nameLower == "text:p" || nameLower == "w:p" || nameLower == "p" || nameLower == "a:p" -> {
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

            _parsingProgress.postValue(ParsingProgress(100, context.getString(com.example.R.string.loading_status_completed)))

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown XML parsing error"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = errorMsg,
                cause = e
            )
            val failProgress = ParsingProgress(
                percentage = 0,
                statusMessage = context.getString(com.example.R.string.doc_open_failed_title),
                isFailed = true,
                errorMessage = errorMsg
            )
            _parsingProgress.postValue(failProgress)
            return@withContext OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = isDocx,
                isOds = isOds,
                isXlsx = isXlsx,
                isOdp = detectedOdp,
                isPptx = detectedPptx,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }

        val plainTextResult = plainTextBuilder.toString().trim()
        val parsedDoc = OfficeParsedDocument(
            elements = elements,
            rawXml = xmlContent,
            plainText = if (plainTextResult.isBlank()) "Empty Document" else plainTextResult,
            extractedImages = extractedImages,
            isOdt = isOdt,
            isDocx = isDocx,
            isOds = isOds,
            isXlsx = isXlsx,
            isOdp = detectedOdp,
            isPptx = detectedPptx,
            isParsingFailed = false
        )
        cacheRepository.saveCachedDocument(file, parsedDoc)
        return@withContext parsedDoc
    }

    /**
     * Saves or creates a valid ODS ZIP package containing properly formatted 'content.xml',
     * 'mimetype', 'META-INF/manifest.xml', 'styles.xml', and 'meta.xml'.
     */
    suspend fun saveOdsDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdsDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveOdsDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdsDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveOdsDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_ods_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdsContentXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new ODS Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. mimetype (MUST be uncompressed STORED entry per ODF spec)
                    val mimeBytes = "application/vnd.oasis.opendocument.spreadsheet".toByteArray(Charsets.UTF_8)
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
                    zout.write(generateOdsManifestXml())
                    zout.closeEntry()

                    // 3. styles.xml
                    val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                    zout.putNextEntry(stylesEntry)
                    zout.write(generateOdtStylesXml())
                    zout.closeEntry()

                    // 4. meta.xml
                    val metaEntry = java.util.zip.ZipEntry("meta.xml")
                    zout.putNextEntry(metaEntry)
                    zout.write(generateOdsMetaXml())
                    zout.closeEntry()

                    // 5. content.xml
                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing ODS file in-place
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
                            zout.write(generateOdsManifestXml())
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
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODS Save Error",
                exceptionType = "OdsPackageSaveException",
                message = "Failed to create/update ODS ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdsContentXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:spreadsheet>\n")
        sb.append("      <table:table table:name=\"Sheet1\">\n")

        val tables = elements.filterIsInstance<OfficeDocumentElement.Table>()
        if (tables.isNotEmpty()) {
            for (tbl in tables) {
                for (row in tbl.rows) {
                    sb.append("        <table:table-row>\n")
                    for (cell in row.cells) {
                        val escCell = escapeXml(cell.text)
                        sb.append("          <table:table-cell office:value-type=\"string\">\n")
                        sb.append("            <text:p>$escCell</text:p>\n")
                        sb.append("          </table:table-cell>\n")
                    }
                    sb.append("        </table:table-row>\n")
                }
            }
        } else {
            val lines = text.split("\n")
            for (line in lines) {
                sb.append("        <table:table-row>\n")
                val cells = line.split("\t")
                for (cellText in cells) {
                    val escCell = escapeXml(cellText)
                    sb.append("          <table:table-cell office:value-type=\"string\">\n")
                    sb.append("            <text:p>$escCell</text:p>\n")
                    sb.append("          </table:table-cell>\n")
                }
                sb.append("        </table:table-row>\n")
            }
        }

        sb.append("      </table:table>\n")
        sb.append("    </office:spreadsheet>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdsManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdsMetaXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
  <office:meta>
    <dc:title>Papirus Spreadsheet</dc:title>
    <meta:generator>Papirus Office Parser</meta:generator>
  </office:meta>
</office:document-meta>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
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

    /**
     * Saves or creates a valid XLSX OpenXML ZIP package containing properly formatted
     * 'xl/worksheets/sheet1.xml', '[Content_Types].xml', '_rels/.rels',
     * 'xl/_rels/workbook.xml.rels', and 'xl/workbook.xml'.
     */
    suspend fun saveXlsxDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveXlsxDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveXlsxDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveXlsxDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveXlsxDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_xlsx_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val sheetXmlBytes = generateFormattedXlsxSheetXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new XLSX Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. [Content_Types].xml
                    val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                    zout.putNextEntry(ctEntry)
                    zout.write(generateXlsxContentTypesXml())
                    zout.closeEntry()

                    // 2. _rels/.rels
                    val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                    zout.putNextEntry(relsEntry)
                    zout.write(generateXlsxRelsXml())
                    zout.closeEntry()

                    // 3. xl/_rels/workbook.xml.rels
                    val wbRelsEntry = java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels")
                    zout.putNextEntry(wbRelsEntry)
                    zout.write(generateXlsxWorkbookRelsXml())
                    zout.closeEntry()

                    // 4. xl/workbook.xml
                    val wbEntry = java.util.zip.ZipEntry("xl/workbook.xml")
                    zout.putNextEntry(wbEntry)
                    zout.write(generateXlsxWorkbookXml())
                    zout.closeEntry()

                    // 5. xl/worksheets/sheet1.xml
                    val sheetEntry = java.util.zip.ZipEntry("xl/worksheets/sheet1.xml")
                    zout.putNextEntry(sheetEntry)
                    zout.write(sheetXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing XLSX file in-place
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundSheet = false
                        var foundContentTypes = false
                        var foundRels = false
                        var foundWorkbookRels = false
                        var foundWorkbook = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "[Content_Types].xml") foundContentTypes = true
                            if (entryName == "_rels/.rels") foundRels = true
                            if (entryName == "xl/_rels/workbook.xml.rels") foundWorkbookRels = true
                            if (entryName == "xl/workbook.xml") foundWorkbook = true

                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)

                            if (entryName == "xl/worksheets/sheet1.xml" || (entryName.startsWith("xl/worksheets/sheet") && !foundSheet)) {
                                foundSheet = true
                                zout.write(sheetXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundSheet) {
                            val sheetEntry = java.util.zip.ZipEntry("xl/worksheets/sheet1.xml")
                            zout.putNextEntry(sheetEntry)
                            zout.write(sheetXmlBytes)
                            zout.closeEntry()
                        }
                        if (!foundContentTypes) {
                            val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                            zout.putNextEntry(ctEntry)
                            zout.write(generateXlsxContentTypesXml())
                            zout.closeEntry()
                        }
                        if (!foundRels) {
                            val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                            zout.putNextEntry(relsEntry)
                            zout.write(generateXlsxRelsXml())
                            zout.closeEntry()
                        }
                        if (!foundWorkbookRels) {
                            val wbRelsEntry = java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels")
                            zout.putNextEntry(wbRelsEntry)
                            zout.write(generateXlsxWorkbookRelsXml())
                            zout.closeEntry()
                        }
                        if (!foundWorkbook) {
                            val wbEntry = java.util.zip.ZipEntry("xl/workbook.xml")
                            zout.putNextEntry(wbEntry)
                            zout.write(generateXlsxWorkbookXml())
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser XLSX Save Error",
                exceptionType = "XlsxPackageSaveException",
                message = "Failed to create/update XLSX ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedXlsxSheetXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sb.append("  <sheetData>\n")

        val tables = elements.filterIsInstance<OfficeDocumentElement.Table>()
        if (tables.isNotEmpty()) {
            var rowIndex = 1
            for (tbl in tables) {
                for (row in tbl.rows) {
                    sb.append("    <row r=\"$rowIndex\">\n")
                    var colIndex = 0
                    for (cell in row.cells) {
                        val colName = getExcelColumnName(colIndex)
                        val cellRef = "$colName$rowIndex"
                        val escCell = escapeXml(cell.text)
                        sb.append("      <c r=\"$cellRef\" t=\"inlineStr\">\n")
                        sb.append("        <is><t>$escCell</t></is>\n")
                        sb.append("      </c>\n")
                        colIndex++
                    }
                    sb.append("    </row>\n")
                    rowIndex++
                }
            }
        } else {
            val lines = text.split("\n")
            var rowIndex = 1
            for (line in lines) {
                sb.append("    <row r=\"$rowIndex\">\n")
                val cells = line.split("\t")
                var colIndex = 0
                for (cellText in cells) {
                    val colName = getExcelColumnName(colIndex)
                    val cellRef = "$colName$rowIndex"
                    val escCell = escapeXml(cellText)
                    sb.append("      <c r=\"$cellRef\" t=\"inlineStr\">\n")
                    sb.append("        <is><t>$escCell</t></is>\n")
                    sb.append("      </c>\n")
                    colIndex++
                }
                sb.append("    </row>\n")
                rowIndex++
            }
        }

        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun getExcelColumnName(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp >= 0) {
            val rem = temp % 26
            sb.insert(0, (rem + 'A'.code).toChar())
            temp = temp / 26 - 1
        }
        return sb.toString()
    }

    private fun generateXlsxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxWorkbookRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxWorkbookXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid PPTX OpenXML ZIP package containing properly formatted
     * 'ppt/slides/slide1.xml', '[Content_Types].xml', '_rels/.rels',
     * 'ppt/_rels/presentation.xml.rels', and 'ppt/presentation.xml'.
     */
    suspend fun savePptxDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext savePptxDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun savePptxDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext savePptxDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun savePptxDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_pptx_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val slideXmlBytes = generateFormattedPptxSlideXml(text)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                    zout.putNextEntry(ctEntry)
                    zout.write(generatePptxContentTypesXml())
                    zout.closeEntry()

                    val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                    zout.putNextEntry(relsEntry)
                    zout.write(generatePptxRelsXml())
                    zout.closeEntry()

                    val presRelsEntry = java.util.zip.ZipEntry("ppt/_rels/presentation.xml.rels")
                    zout.putNextEntry(presRelsEntry)
                    zout.write(generatePptxPresRelsXml())
                    zout.closeEntry()

                    val presEntry = java.util.zip.ZipEntry("ppt/presentation.xml")
                    zout.putNextEntry(presEntry)
                    zout.write(generatePptxPresentationXml())
                    zout.closeEntry()

                    val slideEntry = java.util.zip.ZipEntry("ppt/slides/slide1.xml")
                    zout.putNextEntry(slideEntry)
                    zout.write(slideXmlBytes)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundSlide = false

                        while (entry != null) {
                            val entryName = entry.name
                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)

                            if (entryName == "ppt/slides/slide1.xml" || (entryName.startsWith("ppt/slides/slide") && !foundSlide)) {
                                foundSlide = true
                                zout.write(slideXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundSlide) {
                            val slideEntry = java.util.zip.ZipEntry("ppt/slides/slide1.xml")
                            zout.putNextEntry(slideEntry)
                            zout.write(slideXmlBytes)
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser PPTX Save Error",
                exceptionType = "PptxPackageSaveException",
                message = "Failed to create/update PPTX ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedPptxSlideXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" ")
        sb.append("xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n")
        sb.append("  <p:cSld><p:spTree>\n")
        sb.append("    <p:sp><p:txBody><a:bodyPr/><a:lstStyle/>\n")

        val lines = text.split("\n")
        for (line in lines) {
            val esc = escapeXml(line)
            sb.append("      <a:p><a:r><a:t>$esc</a:t></a:r></a:p>\n")
        }

        sb.append("    </p:txBody></p:sp>\n")
        sb.append("  </p:spTree></p:cSld>\n")
        sb.append("</p:sld>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxPresRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxPresentationXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
    <p:sldId id="256" r:id="rId1"/>
  </p:sldIdLst>
</p:presentation>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid ODP ZIP package containing properly formatted 'content.xml',
     * 'mimetype', and 'META-INF/manifest.xml'.
     */
    suspend fun saveOdpDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdpDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveOdpDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdpDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveOdpDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_odp_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdpContentXml(text)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    val mimeEntry = java.util.zip.ZipEntry("mimetype")
                    zout.putNextEntry(mimeEntry)
                    zout.write("application/vnd.oasis.opendocument.presentation".toByteArray(Charsets.UTF_8))
                    zout.closeEntry()

                    val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                    zout.putNextEntry(manifestEntry)
                    zout.write(generateOdpManifestXml())
                    zout.closeEntry()

                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundContent = false

                        while (entry != null) {
                            val entryName = entry.name
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
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODP Save Error",
                exceptionType = "OdpPackageSaveException",
                message = "Failed to create/update ODP ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdpContentXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" ")
        sb.append("xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\">\n")
        sb.append("  <office:body><office:presentation>\n")
        sb.append("    <draw:page draw:name=\"page1\">\n")
        sb.append("      <draw:frame><draw:text-box>\n")

        val lines = text.split("\n")
        for (line in lines) {
            val esc = escapeXml(line)
            sb.append("        <text:p>$esc</text:p>\n")
        }

        sb.append("      </draw:text-box></draw:frame>\n")
        sb.append("    </draw:page>\n")
        sb.append("  </office:presentation></office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdpManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0">
  <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.presentation"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
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
