package com.makerandreas.papirusoffice.data.writer

import android.util.Xml
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

        try {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "content.xml") {
                        contentXmlBytes = zip.readBytes()
                    } else if (entry.name == "meta.xml") {
                        metaXmlBytes = zip.readBytes()
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
            PapirusLogger.e("ODT", "READ_FAILED entry=content.xml", Exception("content.xml missing from zip"))
            return OfficeDocument()
        }

        PapirusLogger.d("ODT", "CONTENT_XML_READ")

        val title = if (metaXmlBytes != null) parseTitleFromMetaXml(metaXmlBytes!!) else ""
        val elements = parseContentXml(contentXmlBytes!!)

        PapirusLogger.d("ODT", "BODY_ELEMENTS=${elements.size}")
        PapirusLogger.d("ODT", "READ_SUCCESS")

        return OfficeDocument(
            metadata = DocumentMetadata(title = title),
            body = DocumentBody(elements = elements)
        )
    }

    fun parse(file: File): OfficeDocument {
        return parse(file.readBytes())
    }

    private fun parseTitleFromMetaXml(metaXmlBytes: ByteArray): String {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(metaXmlBytes), "UTF-8")

            var eventType = parser.eventType
            var inTitle = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        if (name == "dc:title" || name == "title") {
                            inTitle = true
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (inTitle && !parser.text.isNullOrBlank()) {
                            return parser.text.trim()
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        if (name == "dc:title" || name == "title") {
                            inTitle = false
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // Ignore meta parse errors
        }
        return ""
    }

    private fun parseContentXml(contentXmlBytes: ByteArray): List<OfficeElement> {
        val elements = mutableListOf<OfficeElement>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(contentXmlBytes), "UTF-8")

            var eventType = parser.eventType
            val currentText = StringBuilder()
            var headingLevel = 1
            var inParagraph = false
            var inHeading = false
            var inTable = false
            var inListItem = false
            val currentTableRows = mutableListOf<OfficeTableRow>()
            val currentRowCells = mutableListOf<OfficeTableCell>()

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tag = parser.name ?: ""
                        when {
                            tag == "text:p" || tag == "p" -> {
                                inParagraph = true
                                currentText.clear()
                            }
                            tag == "text:h" || tag == "h" -> {
                                inHeading = true
                                currentText.clear()
                                val levelAttr = parser.getAttributeValue(null, "text:outline-level")
                                    ?: parser.getAttributeValue(null, "outline-level")
                                headingLevel = levelAttr?.toIntOrNull() ?: 1
                            }
                            tag == "text:list-item" || tag == "list-item" -> {
                                inListItem = true
                            }
                            tag == "table:table" || tag == "table" -> {
                                inTable = true
                                currentTableRows.clear()
                            }
                            tag == "table:table-row" || tag == "table-row" -> {
                                currentRowCells.clear()
                            }
                            tag == "table:table-cell" || tag == "table-cell" -> {
                                currentText.clear()
                            }
                        }
                    }
                    XmlPullParser.TEXT -> {
                        currentText.append(parser.text ?: "")
                    }
                    XmlPullParser.END_TAG -> {
                        val tag = parser.name ?: ""
                        when {
                            tag == "text:p" || tag == "p" -> {
                                val text = currentText.toString()
                                if (inHeading) {
                                    // heading end tag will handle
                                } else if (inTable) {
                                    // cell end tag will handle
                                } else if (inListItem) {
                                    elements.add(OfficeListItem(text = text))
                                } else {
                                    elements.add(OfficeParagraph(text = text))
                                }
                                inParagraph = false
                            }
                            tag == "text:h" || tag == "h" -> {
                                val text = currentText.toString()
                                elements.add(OfficeHeading(text = text, level = headingLevel))
                                inHeading = false
                            }
                            tag == "text:list-item" || tag == "list-item" -> {
                                inListItem = false
                            }
                            tag == "table:table-cell" || tag == "table-cell" -> {
                                val cellText = currentText.toString()
                                currentRowCells.add(OfficeTableCell(text = cellText))
                            }
                            tag == "table:table-row" || tag == "table-row" -> {
                                if (currentRowCells.isNotEmpty()) {
                                    currentTableRows.add(OfficeTableRow(cells = ArrayList(currentRowCells)))
                                    currentRowCells.clear()
                                }
                            }
                            tag == "table:table" || tag == "table" -> {
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
}
