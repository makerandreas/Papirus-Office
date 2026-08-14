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
        var contentXmlBytes: ByteArray? = null
        var metaXmlBytes: ByteArray? = null
        var stylesXmlBytes: ByteArray? = null

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "content.xml" -> contentXmlBytes = zip.readBytes()
                        "meta.xml" -> metaXmlBytes = zip.readBytes()
                        "styles.xml" -> stylesXmlBytes = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            PapirusLogger.d("ODT", "PACKAGE_OPENED")
        } catch (e: Exception) {
            PapirusLogger.e("ODT", "READ_FAILED entry=package", e)
            return OfficeDocument()
        }

        if (contentXmlBytes == null) {
            PapirusLogger.d("ODT", "content.xml missing from zip (template mode), creating default document structure")
        } else {
            PapirusLogger.d("ODT", "CONTENT_XML_READ")
        }

        val metadata = if (metaXmlBytes != null) parseMetaXml(metaXmlBytes!!) else DocumentMetadata()
        val styles = if (stylesXmlBytes != null) parseStylesXml(stylesXmlBytes!!) else DocumentStyles()
        val elements = if (contentXmlBytes != null) parseContentXml(contentXmlBytes!!) else listOf(OfficeParagraph(""))

        PapirusLogger.d("ODT", "BODY_ELEMENTS=${elements.size}")
        PapirusLogger.d("ODT", "READ_SUCCESS")

        return OfficeDocument(
            metadata = metadata,
            styles = styles,
            body = DocumentBody(elements = elements)
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
                        val name = parser.name ?: ""
                        if (name == "title" || name == "creator" || name == "initial-creator") {
                            currentTarget = name
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
        val paragraphStyles = mutableMapOf<String, ParagraphStyle>()
        val characterStyles = mutableMapOf<String, CharacterStyle>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(stylesXmlBytes), "UTF-8")

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val name = parser.name ?: ""
                    if (name == "style") {
                        val styleName = getAttr(parser, "name") ?: getAttr(parser, "style-name")
                        val family = getAttr(parser, "family")
                        if (!styleName.isNullOrEmpty()) {
                            if (family == "paragraph") {
                                paragraphStyles[styleName] = ParagraphStyle(name = styleName)
                            } else if (family == "text") {
                                characterStyles[styleName] = CharacterStyle(name = styleName)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Ignore style parse errors
        }
        return DocumentStyles(paragraphStyles = paragraphStyles, characterStyles = characterStyles)
    }

    private fun parseContentXml(contentXmlBytes: ByteArray): List<OfficeElement> {
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
                        val name = parser.name ?: ""
                        when (name) {
                            "p" -> {
                                inParagraph = true
                                currentText.clear()
                                currentRuns.clear()
                                paragraphStyleName = getAttr(parser, "style-name")
                            }
                            "h" -> {
                                inHeading = true
                                currentText.clear()
                                currentRuns.clear()
                                headingStyleName = getAttr(parser, "style-name")
                                val levelAttr = getAttr(parser, "outline-level")
                                headingLevel = levelAttr?.toIntOrNull() ?: 1
                            }
                            "list-item" -> {
                                inListItem = true
                            }
                            "table" -> {
                                inTable = true
                                currentTableRows.clear()
                            }
                            "table-row" -> {
                                currentRowCells.clear()
                            }
                            "table-cell" -> {
                                currentText.clear()
                                currentRuns.clear()
                            }
                            "span" -> {
                                spanStyleName = getAttr(parser, "style-name")
                            }
                            "b" -> boldDepth++
                            "i" -> italicDepth++
                            "u" -> underlineDepth++
                        }
                    }
                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty() && (inParagraph || inHeading || inListItem)) {
                            currentText.append(text)
                            val isB = boldDepth > 0
                            val isI = italicDepth > 0
                            val isU = underlineDepth > 0
                            currentRuns.add(
                                OfficeTextRun(
                                    text = text,
                                    styleName = spanStyleName,
                                    characterStyle = spanStyleName,
                                    isBold = isB,
                                    isItalic = isI,
                                    isUnderline = isU
                                )
                            )
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        when (name) {
                            "span" -> spanStyleName = null
                            "b" -> if (boldDepth > 0) boldDepth--
                            "i" -> if (italicDepth > 0) italicDepth--
                            "u" -> if (underlineDepth > 0) underlineDepth--
                            "p" -> {
                                val text = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                if (inHeading) {
                                    // inside heading, handled by h end tag
                                } else if (inTable) {
                                    // cell end tag will handle or cell paragraph
                                } else if (inListItem) {
                                    elements.add(OfficeListItem(text = text, runs = runs))
                                } else {
                                    elements.add(OfficeParagraph(text = text, styleName = paragraphStyleName, runs = runs))
                                }
                                inParagraph = false
                            }
                            "h" -> {
                                val text = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                elements.add(OfficeHeading(text = text, level = headingLevel, styleName = headingStyleName, runs = runs))
                                inHeading = false
                            }
                            "list-item" -> {
                                inListItem = false
                            }
                            "table-cell" -> {
                                val cellText = currentText.toString()
                                val runs = ArrayList(currentRuns)
                                val cellParagraphs = if (runs.isNotEmpty()) {
                                    listOf(OfficeParagraph(text = cellText, runs = runs))
                                } else {
                                    emptyList()
                                }
                                currentRowCells.add(OfficeTableCell(text = cellText, paragraphs = cellParagraphs))
                            }
                            "table-row" -> {
                                if (currentRowCells.isNotEmpty()) {
                                    currentTableRows.add(OfficeTableRow(cells = ArrayList(currentRowCells)))
                                    currentRowCells.clear()
                                }
                            }
                            "table" -> {
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
        return elements
    }

    private fun getAttr(parser: XmlPullParser, localName: String): String? {
        for (i in 0 until parser.attributeCount) {
            val name = parser.getAttributeName(i) ?: ""
            if (name == localName || name.endsWith(":$localName")) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }
}
