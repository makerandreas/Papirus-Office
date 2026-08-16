package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

class OdtDocumentParser {

    fun parse(bytes: ByteArray): OfficeDocument {
        PapirusLogger.d("ODT", "READ_START")
        val packageEntries = mutableMapOf<String, ByteArray>()

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val entryBytes = zip.readBytes()
                    packageEntries[entry.name] = entryBytes
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            PapirusLogger.d("ODT", "PACKAGE_OPENED entries=${packageEntries.size}")
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "READ_FAILED entry=package", e)
            return OfficeDocument()
        }

        val contentXmlBytes = packageEntries["content.xml"]
        val metaXmlBytes = packageEntries["meta.xml"]
        val stylesXmlBytes = packageEntries["styles.xml"]
        val manifestXmlBytes = packageEntries["META-INF/manifest.xml"]
        val settingsXmlBytes = packageEntries["settings.xml"]

        if (contentXmlBytes == null) {
            PapirusLogger.d("ODT", "content.xml missing from zip (template mode), creating default document structure")
        } else {
            PapirusLogger.d("ODT", "CONTENT_XML_READ")
        }

        val metadata = if (metaXmlBytes != null) parseMetaXml(metaXmlBytes) else DocumentMetadata()

        // Styles from styles.xml (document styles)
        val docStyles = if (stylesXmlBytes != null) parseStylesXml(stylesXmlBytes) else DocumentStyles()

        // Parse automatic styles and elements from content.xml
        val (automaticStyles, elements) = if (contentXmlBytes != null) {
            parseContentXml(contentXmlBytes, docStyles)
        } else {
            Pair(DocumentStyles(), listOf(OfficeParagraph("")))
        }

        // Merge styles: document styles + automatic styles
        val mergedParagraphStyles = docStyles.paragraphStyles.toMutableMap().apply {
            putAll(automaticStyles.paragraphStyles)
        }
        val mergedCharacterStyles = docStyles.characterStyles.toMutableMap().apply {
            putAll(automaticStyles.characterStyles)
        }
        val mergedStyles = DocumentStyles(
            paragraphStyles = mergedParagraphStyles,
            characterStyles = mergedCharacterStyles
        )

        val packageData = OdtPackageData(
            entries = packageEntries,
            originalContentXml = contentXmlBytes?.toString(Charsets.UTF_8),
            originalStylesXml = stylesXmlBytes?.toString(Charsets.UTF_8),
            originalManifestXml = manifestXmlBytes?.toString(Charsets.UTF_8),
            originalMetaXml = metaXmlBytes?.toString(Charsets.UTF_8),
            originalSettingsXml = settingsXmlBytes?.toString(Charsets.UTF_8)
        )

        PapirusLogger.d("ODT", "BODY_ELEMENTS=${elements.size}")
        PapirusLogger.d("ODT", "READ_SUCCESS")

        return OfficeDocument(
            metadata = metadata,
            styles = mergedStyles,
            body = DocumentBody(elements = elements),
            odtPackageData = packageData
        )
    }

    fun parse(file: File): OfficeDocument {
        return parse(file.readBytes())
    }

    private fun parseMetaXml(metaXmlBytes: ByteArray): DocumentMetadata {
        var title = ""
        var author = ""
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(metaXmlBytes), "UTF-8")

            var eventType = parser.eventType
            var currentTarget = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = getLocalName(parser)
                        if (localName == "title" || localName == "creator" || localName == "initial-creator") {
                            currentTarget = localName
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim() ?: ""
                        if (text.isNotEmpty()) {
                            when (currentTarget) {
                                "title" -> title = text
                                "creator", "initial-creator" -> author = text
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        currentTarget = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Ignore meta parse errors
        }
        return DocumentMetadata(title = title, author = author, creator = author)
    }

    private fun parseStylesXml(stylesXmlBytes: ByteArray): DocumentStyles {
        return parseStylesFromStream(stylesXmlBytes)
    }

    private fun parseStylesFromStream(xmlBytes: ByteArray): DocumentStyles {
        val paragraphStyles = mutableMapOf<String, ParagraphStyle>()
        val characterStyles = mutableMapOf<String, CharacterStyle>()

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var currentStyleName: String? = null
            var currentFamily: String? = null
            var currentParentStyle: String? = null

            var currentIsBold = false
            var currentIsItalic = false
            var currentIsUnderline = false
            var currentFontSizeSp = 12f
            var currentColorHex: String? = null
            var currentFontFamily: String? = null
            var currentAlignment = "Left"

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = getLocalName(parser)
                        when (localName) {
                            "style", "default-style" -> {
                                currentStyleName = getAttr(parser, "name") ?: getAttr(parser, "style-name") ?: if (localName == "default-style") "Default" else null
                                currentFamily = getAttr(parser, "family")
                                currentParentStyle = getAttr(parser, "parent-style-name")

                                currentIsBold = false
                                currentIsItalic = false
                                currentIsUnderline = false
                                currentFontSizeSp = 12f
                                currentColorHex = null
                                currentFontFamily = null
                                currentAlignment = "Left"
                            }
                            "text-properties" -> {
                                val fontWeight = getAttr(parser, "font-weight") ?: getAttr(parser, "font-weight-asian") ?: getAttr(parser, "font-weight-complex")
                                if (fontWeight.equals("bold", ignoreCase = true) || fontWeight.equals("700", ignoreCase = true) || fontWeight.equals("800", ignoreCase = true) || fontWeight.equals("900", ignoreCase = true)) {
                                    currentIsBold = true
                                }
                                val fontStyle = getAttr(parser, "font-style") ?: getAttr(parser, "font-style-asian") ?: getAttr(parser, "font-style-complex")
                                if (fontStyle.equals("italic", ignoreCase = true) || fontStyle.equals("oblique", ignoreCase = true)) {
                                    currentIsItalic = true
                                }
                                val underline = getAttr(parser, "text-underline-style") ?: getAttr(parser, "text-underline-type")
                                if (!underline.isNullOrEmpty() && !underline.equals("none", ignoreCase = true)) {
                                    currentIsUnderline = true
                                }
                                val sizeAttr = getAttr(parser, "font-size") ?: getAttr(parser, "font-size-asian")
                                if (!sizeAttr.isNullOrEmpty()) {
                                    val numeric = sizeAttr.filter { it.isDigit() || it == '.' }.toFloatOrNull()
                                    if (numeric != null && numeric > 0f) {
                                        currentFontSizeSp = numeric
                                    }
                                }
                                currentColorHex = getAttr(parser, "color")
                                currentFontFamily = getAttr(parser, "font-name") ?: getAttr(parser, "font-family")
                            }
                            "paragraph-properties" -> {
                                val align = getAttr(parser, "text-align")
                                if (!align.isNullOrEmpty()) {
                                    currentAlignment = when (align.lowercase()) {
                                        "center" -> "Center"
                                        "right", "end" -> "Right"
                                        "justify" -> "Justify"
                                        else -> "Left"
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val localName = getLocalName(parser)
                        if (localName == "style" || localName == "default-style") {
                            val name = currentStyleName
                            if (!name.isNullOrEmpty()) {
                                if (currentFamily == "paragraph" || currentFamily == null) {
                                    paragraphStyles[name] = ParagraphStyle(
                                        name = name,
                                        fontSizeSp = currentFontSizeSp,
                                        isBold = currentIsBold,
                                        isItalic = currentIsItalic,
                                        isUnderline = currentIsUnderline,
                                        colorHex = currentColorHex,
                                        alignment = currentAlignment,
                                        fontFamily = currentFontFamily,
                                        parentStyleName = currentParentStyle
                                    )
                                }
                                if (currentFamily == "text" || currentFamily == null) {
                                    characterStyles[name] = CharacterStyle(
                                        name = name,
                                        fontSizeSp = currentFontSizeSp,
                                        isBold = currentIsBold,
                                        isItalic = currentIsItalic,
                                        isUnderline = currentIsUnderline,
                                        colorHex = currentColorHex,
                                        fontFamily = currentFontFamily,
                                        parentStyleName = currentParentStyle
                                    )
                                }
                            }
                            currentStyleName = null
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "parseStylesFromStream error: ${e.message}", e)
        }

        return DocumentStyles(paragraphStyles = paragraphStyles, characterStyles = characterStyles)
    }

    private fun parseContentXml(
        contentXmlBytes: ByteArray,
        existingStyles: DocumentStyles
    ): Pair<DocumentStyles, List<OfficeElement>> {
        val automaticStyles = parseStylesFromStream(contentXmlBytes)

        val mergedParagraphStyles = existingStyles.paragraphStyles.toMutableMap().apply {
            putAll(automaticStyles.paragraphStyles)
        }
        val mergedCharacterStyles = existingStyles.characterStyles.toMutableMap().apply {
            putAll(automaticStyles.characterStyles)
        }

        val elements = mutableListOf<OfficeElement>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(contentXmlBytes), "UTF-8")

            var eventType = parser.eventType
            val currentText = StringBuilder()
            val currentRuns = mutableListOf<OfficeTextRun>()

            var headingLevel = 1
            var headingStyleName: String? = null
            var paragraphStyleName: String? = null

            var inBody = false
            var inParagraph = false
            var inHeading = false
            var inTable = false
            var inListItem = false

            var boldDepth = 0
            var italicDepth = 0
            var underlineDepth = 0
            var spanStyleName: String? = null

            val currentTableRows = mutableListOf<OfficeTableRow>()
            val currentRowCells = mutableListOf<OfficeTableCell>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = getLocalName(parser)
                        when (localName) {
                            "body" -> inBody = true
                            "p" -> if (inBody) {
                                inParagraph = true
                                currentText.clear()
                                currentRuns.clear()
                                paragraphStyleName = getAttr(parser, "style-name")
                            }
                            "h" -> if (inBody) {
                                inHeading = true
                                currentText.clear()
                                currentRuns.clear()
                                headingStyleName = getAttr(parser, "style-name")
                                val levelAttr = getAttr(parser, "outline-level")
                                headingLevel = levelAttr?.toIntOrNull() ?: 1
                            }
                            "list-item" -> if (inBody) {
                                inListItem = true
                            }
                            "table" -> if (inBody) {
                                inTable = true
                                currentTableRows.clear()
                            }
                            "table-row" -> if (inBody) {
                                currentRowCells.clear()
                            }
                            "table-cell" -> if (inBody) {
                                currentText.clear()
                                currentRuns.clear()
                            }
                            "span" -> if (inBody) {
                                spanStyleName = getAttr(parser, "style-name")
                            }
                            "s" -> if (inBody && (inParagraph || inHeading || inListItem)) {
                                val countAttr = getAttr(parser, "c")
                                val count = countAttr?.toIntOrNull() ?: 1
                                repeat(count) { currentText.append(" ") }
                            }
                            "tab" -> if (inBody && (inParagraph || inHeading || inListItem)) {
                                currentText.append("\t")
                            }
                            "line-break" -> if (inBody && (inParagraph || inHeading || inListItem)) {
                                currentText.append("\n")
                            }
                            "b" -> boldDepth++
                            "i" -> italicDepth++
                            "u" -> underlineDepth++
                            "image" -> if (inBody) {
                                val href = getAttr(parser, "href")
                                if (!href.isNullOrEmpty()) {
                                    elements.add(OfficeImage(imagePath = href))
                                }
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (inBody && text.isNotEmpty() && (inParagraph || inHeading || inListItem)) {
                            currentText.append(text)

                            // Resolve formatting from style name and inline tags
                            val matchingCharStyle = spanStyleName?.let { mergedCharacterStyles[it] }
                            val matchingParaStyle = if (inHeading) headingStyleName?.let { mergedParagraphStyles[it] } else paragraphStyleName?.let { mergedParagraphStyles[it] }

                            val isB = boldDepth > 0 ||
                                    matchingCharStyle?.isBold == true ||
                                    matchingParaStyle?.isBold == true ||
                                    spanStyleName?.contains("Bold", ignoreCase = true) == true ||
                                    inHeading

                            val isI = italicDepth > 0 ||
                                    matchingCharStyle?.isItalic == true ||
                                    matchingParaStyle?.isItalic == true ||
                                    spanStyleName?.contains("Italic", ignoreCase = true) == true

                            val isU = underlineDepth > 0 ||
                                    matchingCharStyle?.isUnderline == true ||
                                    matchingParaStyle?.isUnderline == true ||
                                    spanStyleName?.contains("Underline", ignoreCase = true) == true

                            currentRuns.add(
                                OfficeTextRun(
                                    text = text,
                                    styleName = spanStyleName ?: (if (inHeading) headingStyleName else paragraphStyleName),
                                    characterStyle = spanStyleName,
                                    isBold = isB,
                                    isItalic = isI,
                                    isUnderline = isU
                                )
                            )
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val localName = getLocalName(parser)
                        when (localName) {
                            "body" -> inBody = false
                            "span" -> spanStyleName = null
                            "b" -> if (boldDepth > 0) boldDepth--
                            "i" -> if (italicDepth > 0) italicDepth--
                            "u" -> if (underlineDepth > 0) underlineDepth--
                            "p" -> if (inBody) {
                                val text = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                if (inHeading) {
                                    // inside heading, handled by h end tag
                                } else if (inTable) {
                                    // inside table cell, handled by table-cell
                                } else if (inListItem) {
                                    elements.add(OfficeListItem(text = text, runs = runs))
                                } else {
                                    elements.add(OfficeParagraph(text = text, styleName = paragraphStyleName, runs = runs))
                                }
                                inParagraph = false
                            }
                            "h" -> if (inBody) {
                                val text = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                elements.add(OfficeHeading(text = text, level = headingLevel, styleName = headingStyleName, runs = runs))
                                inHeading = false
                            }
                            "list-item" -> if (inBody) {
                                inListItem = false
                            }
                            "table-cell" -> if (inBody) {
                                val cellText = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                val cellParagraphs = if (runs.isNotEmpty()) {
                                    listOf(OfficeParagraph(text = cellText, runs = runs))
                                } else {
                                    emptyList()
                                }
                                currentRowCells.add(OfficeTableCell(text = cellText, paragraphs = cellParagraphs))
                            }
                            "table-row" -> if (inBody) {
                                if (currentRowCells.isNotEmpty()) {
                                    currentTableRows.add(OfficeTableRow(cells = ArrayList(currentRowCells)))
                                    currentRowCells.clear()
                                }
                            }
                            "table" -> if (inBody) {
                                if (currentTableRows.isNotEmpty()) {
                                    val maxCols = currentTableRows.maxOfOrNull { it.cells.size } ?: 0
                                    elements.add(OfficeTable(rows = ArrayList(currentTableRows), numColumns = maxCols))
                                    currentTableRows.clear()
                                }
                                inTable = false
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "parseContentXml error: ${e.message}", e)
        }

        return Pair(automaticStyles, elements)
    }

    private fun getLocalName(parser: XmlPullParser): String {
        val name = parser.name ?: return ""
        return if (name.contains(":")) name.substringAfter(":") else name
    }

    private fun getAttr(parser: XmlPullParser, localName: String): String? {
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i) ?: ""
            val attrLocal = if (attrName.contains(":")) attrName.substringAfter(":") else attrName
            if (attrLocal.equals(localName, ignoreCase = true)) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }
}
