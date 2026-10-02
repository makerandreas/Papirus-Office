package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.util.readCappedBytes
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
                    val entryBytes = zip.readCappedBytes()
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
        val (automaticStyles, elements, bookmarks) = if (contentXmlBytes != null) {
            parseContentXml(contentXmlBytes, docStyles)
        } else {
            Triple(DocumentStyles(), listOf(OfficeParagraph("")), emptyList())
        }

        // Merge styles: document styles + automatic styles
        val mergedParagraphStyles = docStyles.paragraphStyles.toMutableMap().apply {
            putAll(automaticStyles.paragraphStyles)
        }
        val mergedCharacterStyles = docStyles.characterStyles.toMutableMap().apply {
            putAll(automaticStyles.characterStyles)
        }
        val mergedListStyles = docStyles.listStyles.toMutableMap().apply {
            putAll(automaticStyles.listStyles)
        }
        val mergedStyles = DocumentStyles(
            paragraphStyles = mergedParagraphStyles,
            characterStyles = mergedCharacterStyles,
            listStyles = mergedListStyles,
            outlineStyle = automaticStyles.outlineStyle ?: docStyles.outlineStyle
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
            odtPackageData = packageData,
            bookmarks = bookmarks
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
        val listStyles = LinkedHashMap<String, NumberingSpec>()
        var outlineStyle: NumberingSpec? = null

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

            var eventType = parser.eventType
            var currentStyleName: String? = null
            var currentFamily: String? = null
            var currentParentStyle: String? = null
            var currentListStyleAttr: String? = null

            var currentIsBold = false
            var currentIsItalic = false
            var currentIsUnderline = false
            var currentFontSizeSp = 12f
            var currentColorHex: String? = null
            var currentFontFamily: String? = null
            var currentAlignment = "Left"

            var pendingListStyleName: String? = null
            var pendingListDisplayName: String? = null
            var pendingListIsOutline = false
            val pendingListLevels = LinkedHashMap<Int, NumberingLevelSpec>()
            var pendingLevelSpec: NumberingLevelSpec? = null

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = getLocalName(parser)
                        when (localName) {
                            "style", "default-style" -> {
                                currentStyleName = getAttr(parser, "name") ?: getAttr(parser, "style-name") ?: if (localName == "default-style") "Default" else null
                                currentFamily = getAttr(parser, "family")
                                currentParentStyle = getAttr(parser, "parent-style-name")
                                currentListStyleAttr = getAttr(parser, "list-style-name")

                                currentIsBold = false
                                currentIsItalic = false
                                currentIsUnderline = false
                                currentFontSizeSp = 12f
                                currentColorHex = null
                                currentFontFamily = null
                                currentAlignment = "Left"
                            }
                            "list-style", "outline-style" -> {
                                pendingLevelSpec?.let { pendingListLevels[it.level] = it }
                                pendingLevelSpec = null
                                pendingListIsOutline = localName == "outline-style"
                                pendingListStyleName = getAttr(parser, "name")?.takeIf { it.isNotBlank() }
                                    ?: if (pendingListIsOutline) "Outline" else null
                                pendingListDisplayName = getAttr(parser, "display-name")
                                pendingListLevels.clear()
                            }
                            "list-level-style-number", "list-level-style-bullet", "outline-level-style" -> {
                                pendingLevelSpec?.let { pendingListLevels[it.level] = it }
                                val lvl = getAttr(parser, "level")?.toIntOrNull() ?: 1
                                val isBullet = localName == "list-level-style-bullet"
                                val defaultFmt = if (localName == "outline-level-style") "" else "1"
                                val numFmt = getAttr(parser, "num-format") ?: defaultFmt
                                pendingLevelSpec = NumberingLevelSpec(
                                    level = lvl,
                                    isBullet = isBullet,
                                    numFormat = numFmt,
                                    numPrefix = getAttr(parser, "num-prefix") ?: "",
                                    numSuffix = getAttr(parser, "num-suffix") ?: "",
                                    displayLevels = getAttr(parser, "display-levels")?.toIntOrNull() ?: 1,
                                    startValue = getAttr(parser, "start-value")?.toIntOrNull() ?: 1,
                                    bulletChar = getAttr(parser, "bullet-char") ?: "\u2022",
                                    textStyleName = getAttr(parser, "style-name")?.takeIf { it.isNotBlank() }
                                )
                            }
                            "text-properties" -> {
                                val fontWeight = getAttr(parser, "font-weight") ?: getAttr(parser, "font-weight-asian") ?: getAttr(parser, "font-weight-complex")
                                val bold = fontWeight.equals("bold", ignoreCase = true) || fontWeight.equals("700", ignoreCase = true) || fontWeight.equals("800", ignoreCase = true) || fontWeight.equals("900", ignoreCase = true)
                                val fontStyle = getAttr(parser, "font-style") ?: getAttr(parser, "font-style-asian") ?: getAttr(parser, "font-style-complex")
                                val italic = fontStyle.equals("italic", ignoreCase = true) || fontStyle.equals("oblique", ignoreCase = true)
                                val underline = getAttr(parser, "text-underline-style") ?: getAttr(parser, "text-underline-type")
                                val isUnder = !underline.isNullOrEmpty() && !underline.equals("none", ignoreCase = true)
                                val sizeAttr = getAttr(parser, "font-size") ?: getAttr(parser, "font-size-asian")
                                val parsedSize = sizeAttr?.filter { it.isDigit() || it == '.' }?.toFloatOrNull()?.takeIf { it > 0f }
                                val color = getAttr(parser, "color")
                                val family = (getAttr(parser, "font-name") ?: getAttr(parser, "font-family"))?.trim()?.trim('\'', '"')

                                if (pendingLevelSpec != null) {
                                    pendingLevelSpec = pendingLevelSpec!!.copy(
                                        fontFamily = family ?: pendingLevelSpec!!.fontFamily,
                                        fontSizeSp = parsedSize ?: pendingLevelSpec!!.fontSizeSp,
                                        isBold = if (fontWeight != null) bold else pendingLevelSpec!!.isBold,
                                        isItalic = if (fontStyle != null) italic else pendingLevelSpec!!.isItalic,
                                        colorHex = color ?: pendingLevelSpec!!.colorHex
                                    )
                                } else {
                                    if (bold) currentIsBold = true
                                    if (italic) currentIsItalic = true
                                    if (isUnder) currentIsUnderline = true
                                    if (parsedSize != null) currentFontSizeSp = parsedSize
                                    currentColorHex = color
                                    currentFontFamily = family
                                }
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
                        when (localName) {
                            "list-level-style-number", "list-level-style-bullet", "outline-level-style" -> {
                                pendingLevelSpec?.let { pendingListLevels[it.level] = it }
                                pendingLevelSpec = null
                            }
                            "list-style", "outline-style" -> {
                                pendingLevelSpec?.let { pendingListLevels[it.level] = it }
                                pendingLevelSpec = null
                                val name = pendingListStyleName
                                if (!name.isNullOrBlank() && pendingListLevels.isNotEmpty()) {
                                    val spec = NumberingSpec(
                                        name = name,
                                        displayName = pendingListDisplayName,
                                        isOutline = pendingListIsOutline,
                                        levels = pendingListLevels.toMap()
                                    )
                                    if (pendingListIsOutline) {
                                        outlineStyle = spec
                                    } else {
                                        listStyles[name] = spec
                                    }
                                }
                                pendingListStyleName = null
                                pendingListDisplayName = null
                                pendingListIsOutline = false
                                pendingListLevels.clear()
                            }
                            "style", "default-style" -> {
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
                                            parentStyleName = currentParentStyle,
                                            listStyleName = currentListStyleAttr
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
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "parseStylesFromStream error: ${e.message}", e)
        }

        return DocumentStyles(
            paragraphStyles = paragraphStyles,
            characterStyles = characterStyles,
            listStyles = listStyles,
            outlineStyle = outlineStyle
        )
    }

    private fun parseContentXml(
        contentXmlBytes: ByteArray,
        existingStyles: DocumentStyles
    ): Triple<DocumentStyles, List<OfficeElement>, List<OfficeBookmark>> {
        val automaticStyles = parseStylesFromStream(contentXmlBytes)

        val mergedParagraphStyles = existingStyles.paragraphStyles.toMutableMap().apply {
            putAll(automaticStyles.paragraphStyles)
        }
        val mergedCharacterStyles = existingStyles.characterStyles.toMutableMap().apply {
            putAll(automaticStyles.characterStyles)
        }
        val mergedListStyles = existingStyles.listStyles.toMutableMap().apply {
            putAll(automaticStyles.listStyles)
        }
        val effectiveOutlineStyle = automaticStyles.outlineStyle ?: existingStyles.outlineStyle

        fun resolveParaListStyleName(styleName: String?): String? {
            var curr = styleName
            var depth = 0
            while (curr != null && depth < 16) {
                val ps = mergedParagraphStyles[curr] ?: break
                if (ps.listStyleName != null) return ps.listStyleName
                curr = ps.parentStyleName
                depth++
            }
            return null
        }

        val outlineCounter = NumberingCounterState()
        val listCounters = HashMap<String, NumberingCounterState>()
        val listStyleStack = ArrayDeque<String>()
        var lastListStyleName: String? = null
        var listDepth = 0

        val elements = mutableListOf<OfficeElement>()
        val allBookmarks = LinkedHashSet<String>()
        val currentElementBookmarks = LinkedHashSet<String>()
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
            var inTocSourceDepth = 0
            var inParagraph = false
            var inHeading = false
            var inTable = false
            var inListItem = false
            var listItemStartValue: Int? = null

            var boldDepth = 0
            var italicDepth = 0
            var underlineDepth = 0
            var spanStyleName: String? = null
            var currentHyperlink: String? = null

            val currentTableRows = mutableListOf<OfficeTableRow>()
            val currentRowCells = mutableListOf<OfficeTableCell>()

            fun appendSynthesizedText(segment: String) {
                if (segment.isEmpty()) return
                currentText.append(segment)
                currentRuns.add(
                    OfficeTextRun(
                        text = segment,
                        styleName = spanStyleName ?: (if (inHeading) headingStyleName else paragraphStyleName),
                        characterStyle = spanStyleName,
                        hyperlink = currentHyperlink,
                        isBold = boldDepth > 0 || inHeading,
                        isItalic = italicDepth > 0,
                        isUnderline = underlineDepth > 0 || currentHyperlink != null
                    )
                )
            }

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val localName = getLocalName(parser)
                        if (localName == "table-of-content-source") {
                            inTocSourceDepth++
                        } else if (inTocSourceDepth > 0) {
                            // Ignore template contents inside <text:table-of-content-source>
                        } else {
                            when (localName) {
                                "body" -> inBody = true
                                "p" -> if (inBody) {
                                    inParagraph = true
                                    currentText.clear()
                                    currentRuns.clear()
                                    if (!inListItem) currentElementBookmarks.clear()
                                    paragraphStyleName = getAttr(parser, "style-name")
                                }
                                "h" -> if (inBody) {
                                    inHeading = true
                                    currentText.clear()
                                    currentRuns.clear()
                                    if (!inListItem) currentElementBookmarks.clear()
                                    headingStyleName = getAttr(parser, "style-name")
                                    val levelAttr = getAttr(parser, "outline-level")
                                    headingLevel = levelAttr?.toIntOrNull() ?: 1
                                }
                                "list" -> if (inBody) {
                                    listDepth++
                                    val styleAttr = getAttr(parser, "style-name")?.takeIf { it.isNotBlank() }
                                    val cont = getAttr(parser, "continue-numbering") == "true" ||
                                        !getAttr(parser, "continue-list").isNullOrBlank()
                                    val resolved = styleAttr
                                        ?: listStyleStack.peek()
                                        ?: if (cont) lastListStyleName else null
                                    if (listDepth == 1 && resolved != null) {
                                        if (!cont) listCounters[resolved] = NumberingCounterState()
                                        lastListStyleName = resolved
                                    }
                                    listStyleStack.push(resolved ?: "")
                                }
                                "list-item", "list-header" -> if (inBody) {
                                    inListItem = true
                                    currentElementBookmarks.clear()
                                    listItemStartValue = getAttr(parser, "start-value")?.toIntOrNull()
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
                                "a" -> if (inBody) {
                                    currentHyperlink = getAttr(parser, "href")?.takeIf { it.isNotEmpty() }
                                }
                                "bookmark", "bookmark-start" -> if (inBody) {
                                    val bmName = getAttr(parser, "name")?.trim()
                                    if (!bmName.isNullOrEmpty()) {
                                        allBookmarks.add(bmName)
                                        currentElementBookmarks.add(bmName)
                                    }
                                }
                                "s" -> if (inBody && (inParagraph || inHeading || inListItem)) {
                                    val countAttr = getAttr(parser, "c")
                                    val count = (countAttr?.toIntOrNull() ?: 1).coerceAtLeast(1)
                                    appendSynthesizedText(" ".repeat(count))
                                }
                                "tab" -> if (inBody && (inParagraph || inHeading || inListItem)) {
                                    appendSynthesizedText("\t")
                                }
                                "line-break" -> if (inBody && (inParagraph || inHeading)) {
                                    appendSynthesizedText("\n")
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
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (inBody && inTocSourceDepth == 0 && text.isNotEmpty() && (inParagraph || inHeading || inListItem)) {
                            currentText.append(text)

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
                                    currentHyperlink != null ||
                                    matchingCharStyle?.isUnderline == true ||
                                    matchingParaStyle?.isUnderline == true ||
                                    spanStyleName?.contains("Underline", ignoreCase = true) == true

                            currentRuns.add(
                                OfficeTextRun(
                                    text = text,
                                    styleName = spanStyleName ?: (if (inHeading) headingStyleName else paragraphStyleName),
                                    characterStyle = spanStyleName,
                                    hyperlink = currentHyperlink,
                                    isBold = isB,
                                    isItalic = isI,
                                    isUnderline = isU
                                )
                            )
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val localName = getLocalName(parser)
                        if (localName == "table-of-content-source") {
                            if (inTocSourceDepth > 0) inTocSourceDepth--
                        } else if (inTocSourceDepth == 0) {
                            when (localName) {
                                "body" -> inBody = false
                                "span" -> spanStyleName = null
                                "a" -> currentHyperlink = null
                                "b" -> if (boldDepth > 0) boldDepth--
                                "i" -> if (italicDepth > 0) italicDepth--
                                "u" -> if (underlineDepth > 0) underlineDepth--
                                "p" -> if (inBody) {
                                    val text = currentText.toString()
                                    val runs = ArrayList(currentRuns)
                                    val bms = currentElementBookmarks.map { OfficeBookmark(it) }
                                    if (inHeading) {
                                        // inside heading, handled by h end tag
                                    } else if (inTable) {
                                        // inside table cell, handled by table-cell
                                    } else if (inListItem) {
                                        val activeListStyle = listStyleStack.peek()?.takeIf { it.isNotBlank() }
                                            ?: resolveParaListStyleName(paragraphStyleName)?.takeIf { it.isNotBlank() }
                                        val spec = activeListStyle?.let { mergedListStyles[it] }
                                        val lvl = listDepth.coerceAtLeast(1)
                                        val levelSpec = spec?.level(lvl)
                                        val bullet = if (spec != null && text.isNotBlank()) {
                                            val counter = listCounters.getOrPut(spec.name) { NumberingCounterState() }
                                            val raw = counter.advance(spec, lvl, listItemStartValue)
                                            listItemStartValue = null
                                            if (raw.isEmpty()) "" else if (raw.endsWith(" ")) raw else "$raw "
                                        } else {
                                            if (lvl > 1) "\u25e6 " else "\u2022 "
                                        }
                                        elements.add(
                                            OfficeListItem(
                                                text = text,
                                                bullet = bullet,
                                                level = lvl,
                                                isOrdered = levelSpec?.isBullet == false && bullet.isNotBlank(),
                                                styleName = paragraphStyleName,
                                                labelFontSizeSp = levelSpec?.fontSizeSp,
                                                labelFontFamily = levelSpec?.fontFamily,
                                                runs = runs,
                                                bookmarks = bms
                                            )
                                        )
                                        currentElementBookmarks.clear()
                                    } else {
                                        elements.add(
                                            OfficeParagraph(
                                                text = text,
                                                styleName = paragraphStyleName,
                                                runs = runs,
                                                bookmark = currentElementBookmarks.firstOrNull(),
                                                bookmarks = bms
                                            )
                                        )
                                        currentElementBookmarks.clear()
                                    }
                                    inParagraph = false
                                }
                                "h" -> if (inBody) {
                                    var text = currentText.toString()
                                    val runs = ArrayList(currentRuns)
                                    val bms = currentElementBookmarks.map { OfficeBookmark(it) }
                                    val paraListStyle = resolveParaListStyleName(headingStyleName)
                                    val rawPrefix = when {
                                        text.isBlank() -> ""
                                        inListItem -> {
                                            val activeListStyle = listStyleStack.peek()?.takeIf { it.isNotBlank() }
                                                ?: paraListStyle?.takeIf { it.isNotBlank() }
                                            val spec = activeListStyle?.let { mergedListStyles[it] }
                                            if (spec != null) {
                                                val counter = listCounters.getOrPut(spec.name) { NumberingCounterState() }
                                                val r = counter.advance(spec, listDepth.coerceAtLeast(1), listItemStartValue)
                                                listItemStartValue = null
                                                r
                                            } else ""
                                        }
                                        paraListStyle != null -> {
                                            if (paraListStyle.isEmpty()) ""
                                            else {
                                                val spec = mergedListStyles[paraListStyle]
                                                if (spec != null) {
                                                    val counter = listCounters.getOrPut(spec.name) { NumberingCounterState() }
                                                    counter.advance(spec, headingLevel)
                                                } else ""
                                            }
                                        }
                                        effectiveOutlineStyle != null -> {
                                            outlineCounter.advance(effectiveOutlineStyle, headingLevel)
                                        }
                                        else -> ""
                                    }
                                    if (rawPrefix.isNotEmpty()) {
                                        val prefix = if (rawPrefix.endsWith(" ")) rawPrefix else "$rawPrefix "
                                        if (!text.trimStart().startsWith(prefix.trimEnd())) {
                                            text = prefix + text
                                            runs.add(
                                                0,
                                                OfficeTextRun(
                                                    text = prefix,
                                                    styleName = headingStyleName,
                                                    isBold = true
                                                )
                                            )
                                        }
                                    }
                                    elements.add(
                                        OfficeHeading(
                                            text = text,
                                            level = headingLevel,
                                            styleName = headingStyleName,
                                            runs = runs,
                                            bookmarks = bms
                                        )
                                    )
                                    currentElementBookmarks.clear()
                                    inHeading = false
                                }
                                "list-item", "list-header" -> if (inBody) {
                                    inListItem = false
                                    listItemStartValue = null
                                }
                                "list" -> if (inBody) {
                                    if (listStyleStack.isNotEmpty()) listStyleStack.pop()
                                    if (listDepth > 0) listDepth--
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
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "parseContentXml error: ${e.message}", e)
        }

        return Triple(automaticStyles, elements, allBookmarks.map { OfficeBookmark(it) })
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
