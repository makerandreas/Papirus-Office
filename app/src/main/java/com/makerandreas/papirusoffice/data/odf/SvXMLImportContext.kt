package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.TableCell
import com.makerandreas.papirusoffice.data.TableRow
import com.makerandreas.papirusoffice.data.TextRun
import java.io.File

/**
 * Base class for element contexts maintained in a stack during ODF XML import,
 * matching SvXMLImportContext in LibreOffice xmloff.
 */
open class SvXMLImportContext(
    val importFilter: SvXMLImport,
    val token: OdfXmlToken
) {
    open fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {}
    open fun onCharacters(text: String) {}
    open fun onEndElement(token: OdfXmlToken) {}

    open fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return SvXMLImportContext(importFilter, token)
    }
}

/**
 * Context for the root document structure (<office:document> or <office:document-content>).
 */
class OdfDocumentContentContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_BODY -> OdfBodyContext(importFilter, token)
            OdfXmlToken.XML_AUTOMATIC_STYLES, OdfXmlToken.XML_STYLES -> OdfStylesContainerContext(importFilter, token)
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for <office:body>.
 */
class OdfBodyContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TEXT, OdfXmlToken.XML_SPREADSHEET, OdfXmlToken.XML_PRESENTATION -> {
                OdfTextBodyContext(importFilter, token)
            }
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for <office:text>, <office:spreadsheet>, or <office:presentation>.
 */
class OdfTextBodyContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
            OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1)
            OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token)
            OdfXmlToken.XML_PAGE -> OdfTextBodyContext(importFilter, token) // Slide page for ODP
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for <text:p> paragraphs.
 */
class OdfParagraphContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()
    private val runs = mutableListOf<TextRun>()
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]

    override fun onCharacters(text: String) {
        textBuilder.append(text)
        runs.add(TextRun(text = text))
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(importFilter, token, attributes) { spanText, run ->
                textBuilder.append(spanText)
                runs.add(run)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                textBuilder.append(spaces)
                runs.add(TextRun(text = spaces))
            }
            OdfXmlToken.XML_TAB -> {
                textBuilder.append("\t")
                runs.add(TextRun(text = "\t"))
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_LINE_BREAK -> {
                textBuilder.append("\n")
                runs.add(TextRun(text = "\n"))
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val fullText = textBuilder.toString()
        if (fullText.isNotEmpty() || runs.isNotEmpty()) {
            val paragraph = OfficeDocumentElement.Paragraph(
                text = fullText,
                styleName = styleName,
                runs = runs.toList()
            )
            importFilter.addElement(paragraph)
        }
    }
}

/**
 * Context for <text:h> headings.
 */
class OdfHeadingContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()
    private val level: Int = attributes["text:outline-level"]?.toIntOrNull()
        ?: attributes["outline-level"]?.toIntOrNull() ?: 1
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]

    override fun onCharacters(text: String) {
        textBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(importFilter, token, attributes) { spanText, _ ->
                textBuilder.append(spanText)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                textBuilder.append(spaces)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val headingText = textBuilder.toString()
        if (headingText.isNotEmpty()) {
            val heading = OfficeDocumentElement.Heading(
                text = headingText,
                level = level,
                styleName = styleName
            )
            importFilter.addElement(heading)
        }
    }
}

/**
 * Context for <text:span> inline formatting.
 */
class OdfSpanContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val onSpanParsed: (String, TextRun) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val spanTextBuilder = StringBuilder()
    private val styleName = attributes["text:style-name"] ?: attributes["style-name"] ?: ""

    override fun onCharacters(text: String) {
        spanTextBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                spanTextBuilder.append(spaces)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val text = spanTextBuilder.toString()
        val isBold = styleName.contains("bold", ignoreCase = true) || styleName.contains("b", ignoreCase = true)
        val isItalic = styleName.contains("italic", ignoreCase = true) || styleName.contains("i", ignoreCase = true)
        val isUnderline = styleName.contains("underline", ignoreCase = true) || styleName.contains("u", ignoreCase = true)
        
        val run = TextRun(
            text = text,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline
        )
        onSpanParsed(text, run)
    }
}

/**
 * Context for <text:s> whitespace runs.
 */
class OdfSpaceContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val onSpaceParsed: (String) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val count: Int = attributes["text:c"]?.toIntOrNull()
        ?: attributes["c"]?.toIntOrNull() ?: 1

    override fun onEndElement(token: OdfXmlToken) {
        val spaces = " ".repeat(count.coerceAtLeast(1))
        onSpaceParsed(spaces)
    }
}

/**
 * Context for <text:list>.
 */
class OdfListContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    val listLevel: Int
) : SvXMLImportContext(importFilter, token) {

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_LIST_ITEM, OdfXmlToken.XML_LIST_HEADER -> {
                OdfListItemContext(importFilter, token, listLevel)
            }
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, listLevel + 1)
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for <text:list-item>.
 */
class OdfListItemContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val listLevel: Int
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()

    override fun onCharacters(text: String) {
        textBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfListItemParagraphContext(importFilter, token) { itemText ->
                textBuilder.append(itemText)
            }
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, listLevel + 1)
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val text = textBuilder.toString()
        if (text.isNotBlank()) {
            val bulletSymbol = if (listLevel > 1) "◦ " else "• "
            val listItem = OfficeDocumentElement.ListItem(
                text = text,
                level = listLevel,
                bullet = bulletSymbol
            )
            importFilter.addElement(listItem)
        }
    }
}

class OdfListItemParagraphContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val onTextExtracted: (String) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val paragraphBuilder = StringBuilder()

    override fun onCharacters(text: String) {
        paragraphBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(importFilter, token, attributes) { spanText, _ ->
                paragraphBuilder.append(spanText)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                paragraphBuilder.append(spaces)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        onTextExtracted(paragraphBuilder.toString())
    }
}

/**
 * Context for <table:table>.
 */
class OdfTableContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {

    val rows = mutableListOf<TableRow>()

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TABLE_ROW, OdfXmlToken.XML_TABLE_HEADER_ROWS -> {
                OdfTableRowContext(importFilter, token, this)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        if (rows.isNotEmpty()) {
            val maxCols = rows.maxOfOrNull { it.cells.size } ?: 0
            val tableElement = OfficeDocumentElement.Table(
                rows = rows.toList(),
                numColumns = maxCols
            )
            importFilter.addElement(tableElement)
        }
    }
}

/**
 * Context for <table:table-row>.
 */
class OdfTableRowContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val parentTableContext: OdfTableContext
) : SvXMLImportContext(importFilter, token) {

    val cells = mutableListOf<TableCell>()

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TABLE_CELL, OdfXmlToken.XML_COVERED_TABLE_CELL -> {
                OdfTableCellContext(importFilter, token, this)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        if (cells.isNotEmpty()) {
            parentTableContext.rows.add(TableRow(cells = cells.toList()))
        }
    }
}

/**
 * Context for <table:table-cell>.
 */
class OdfTableCellContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val parentRowContext: OdfTableRowContext
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()
    private val cellParagraphs = mutableListOf<OfficeDocumentElement.Paragraph>()

    override fun onCharacters(text: String) {
        textBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfTableCellParagraphContext(importFilter, token) { pText ->
                if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                textBuilder.append(pText)
                cellParagraphs.add(OfficeDocumentElement.Paragraph(text = pText))
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val cell = TableCell(
            text = textBuilder.toString(),
            paragraphs = cellParagraphs.toList()
        )
        parentRowContext.cells.add(cell)
    }
}

class OdfTableCellParagraphContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val onTextExtracted: (String) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val pBuilder = StringBuilder()

    override fun onCharacters(text: String) {
        pBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(importFilter, token, attributes) { spanText, _ ->
                pBuilder.append(spanText)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                pBuilder.append(spaces)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        onTextExtracted(pBuilder.toString())
    }
}

/**
 * Context for <draw:frame> and <draw:image>.
 */
class OdfFrameContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
) : SvXMLImportContext(importFilter, token) {

    private val widthDp = parseDimensionToDp(attributes["svg:width"] ?: attributes["width"])
    private val heightDp = parseDimensionToDp(attributes["svg:height"] ?: attributes["height"])

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_IMAGE -> OdfImageContext(importFilter, token, attributes, widthDp, heightDp)
            else -> super.createChildContext(token, attributes)
        }
    }

    private fun parseDimensionToDp(dimStr: String?): Float {
        if (dimStr.isNullOrBlank()) return 100f
        val clean = dimStr.lowercase().trim()
        val num = clean.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 100f
        return when {
            clean.endsWith("in") -> num * 160f
            clean.endsWith("cm") -> num * 37.795f
            clean.endsWith("mm") -> num * 3.7795f
            clean.endsWith("pt") -> num * 1.333f
            else -> num
        }
    }
}

class OdfImageContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val widthDp: Float,
    private val heightDp: Float
) : SvXMLImportContext(importFilter, token) {

    private val href: String? = attributes["xlink:href"] ?: attributes["href"]

    override fun onEndElement(token: OdfXmlToken) {
        if (!href.isNullOrBlank()) {
            val imgFile = importFilter.extractedImages[href] ?: importFilter.extractedImages[href.substringAfterLast("/")]
            val imageElement = OfficeDocumentElement.ImageElement(
                imagePath = href,
                imageFile = imgFile,
                widthDp = widthDp,
                heightDp = heightDp
            )
            importFilter.addElement(imageElement)
        }
    }
}

/**
 * Context for parsing styles container.
 */
class OdfStylesContainerContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_STYLE, OdfXmlToken.XML_DEFAULT_STYLE -> OdfStyleContext(importFilter, token, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }
}

class OdfStyleContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
) : SvXMLImportContext(importFilter, token) {
    val styleName: String = attributes["style:name"] ?: attributes["name"] ?: ""
    val family: String = attributes["style:family"] ?: attributes["family"] ?: ""
}
