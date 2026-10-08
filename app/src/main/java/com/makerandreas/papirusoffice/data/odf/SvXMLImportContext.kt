package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeTableColumnSpec
import com.makerandreas.papirusoffice.data.TableCell
import com.makerandreas.papirusoffice.data.TableCellOccupancy
import com.makerandreas.papirusoffice.data.TableDiagnostic
import com.makerandreas.papirusoffice.data.TableDiagnosticCode
import com.makerandreas.papirusoffice.data.TableRow
import com.makerandreas.papirusoffice.data.TextRun

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
        return when (token) {
            OdfXmlToken.XML_DOCUMENT, OdfXmlToken.XML_DOCUMENT_CONTENT, OdfXmlToken.XML_OFFICE -> OdfDocumentContentContext(importFilter, token)
            OdfXmlToken.XML_BODY -> OdfBodyContext(importFilter, token)
            OdfXmlToken.XML_TEXT, OdfXmlToken.XML_SPREADSHEET, OdfXmlToken.XML_PRESENTATION -> OdfTextBodyContext(importFilter, token)
            OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
            OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
            OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token, attributes)
            OdfXmlToken.XML_TABLE_OF_CONTENT_SOURCE -> OdfIgnoreSubtreeContext(importFilter, token)
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                importFilter.recordBookmark(attributes["text:name"] ?: attributes["name"])
                SvXMLImportContext(importFilter, token)
            }
            OdfXmlToken.XML_AUTOMATIC_STYLES, OdfXmlToken.XML_STYLES -> OdfStylesContainerContext(importFilter, token)
            else -> SvXMLImportContext(importFilter, token)
        }
    }
}

/**
 * Context that ignores all descendant elements and character data (for example,
 * `<text:table-of-content-source>` template definitions inside a TOC).
 */
class OdfIgnoreSubtreeContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext = this
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
        if (token == OdfXmlToken.XML_SECTION || indexKindFor(token) != null) {
            return requireNotNull(textFlowChildContext(importFilter, token, attributes))
        }
        return when (token) {
            OdfXmlToken.XML_P -> {
                OdfParagraphContext(importFilter, token, attributes)
            }
            OdfXmlToken.XML_H -> {
                OdfHeadingContext(importFilter, token, attributes)
            }
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
            OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token, attributes)
            OdfXmlToken.XML_PAGE -> OdfSlidePageContext(importFilter, token, attributes) // Slide page for ODP
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            OdfXmlToken.XML_TEXT_BOX, OdfXmlToken.XML_CUSTOM_SHAPE, OdfXmlToken.XML_G -> {
                OdfDrawingContainerContext(importFilter, token)
            }
            OdfXmlToken.XML_SOFT_PAGE_BREAK -> {
                super.createChildContext(token, attributes)
            }
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for <draw:page> (ODP Slide page).
 */
class OdfSlidePageContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
) : SvXMLImportContext(importFilter, token) {

    init {
        if (importFilter.elements.isNotEmpty()) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
        val pageName = attributes["draw:name"] ?: attributes["name"]
        if (!pageName.isNullOrBlank()) {
            importFilter.addElement(OfficeDocumentElement.Heading(text = pageName, level = 1, styleName = "SlideTitle"))
        }
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
            OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            OdfXmlToken.XML_TEXT_BOX, OdfXmlToken.XML_CUSTOM_SHAPE, OdfXmlToken.XML_G -> {
                OdfDrawingContainerContext(importFilter, token)
            }
            OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }
}

/**
 * Context for nested drawing containers (<draw:g>, <draw:custom-shape>, <draw:text-box>).
 */
class OdfDrawingContainerContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
            OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            OdfXmlToken.XML_TEXT_BOX, OdfXmlToken.XML_CUSTOM_SHAPE, OdfXmlToken.XML_G -> {
                OdfDrawingContainerContext(importFilter, token)
            }
            OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token, attributes)
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
    private val bookmarks = LinkedHashSet<String>()
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]

    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        if (importFilter.hasPageBreakBefore(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }

    override fun onCharacters(text: String) {
        textBuilder.append(text)
        runs.add(TextRun(text = text))
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onSpanParsed = { spanText, run ->
                    textBuilder.append(spanText)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_A -> OdfHyperlinkContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onRunEmitted = { run ->
                    textBuilder.append(run.text)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_BOOKMARK_END -> {
                super.createChildContext(token, attributes)
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
            OdfXmlToken.XML_SOFT_PAGE_BREAK -> {
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val fullText = textBuilder.toString()
        // Index titles and entries use outline-capable styles (Contents_20_Heading);
        // they stay paragraphs so the Navigator does not list them as headings.
        val headingLvl = if (importFilter.isInsideIndex) null else importFilter.resolveHeadingLevel(styleName)
        val element = if (headingLvl != null) {
            val paraListStyle = importFilter.resolveParagraphListStyleName(styleName)
            val label = if (!paraListStyle.isNullOrEmpty() && fullText.isNotBlank()) {
                importFilter.formatHeadingLabel(styleName = styleName, outlineLevel = headingLvl)
            } else {
                null
            }
            val (finalText, finalRuns) = prependHeadingLabelIfNeeded(fullText, runs, label)
            OfficeDocumentElement.Heading(
                text = finalText,
                level = headingLvl,
                styleName = styleName,
                runs = finalRuns,
                bookmarks = bookmarks.toList()
            )
        } else {
            OfficeDocumentElement.Paragraph(
                text = fullText,
                styleName = styleName,
                runs = runs.toList(),
                bookmarks = bookmarks.toList()
            )
        }
        importFilter.addElement(element)
        if (importFilter.hasPageBreakAfter(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }
}

private fun prependHeadingLabelIfNeeded(
    rawText: String,
    rawRuns: List<TextRun>,
    label: OdfFormattedListLabel?
): Pair<String, List<TextRun>> {
    val prefix = label?.bullet?.takeIf { it.isNotEmpty() } ?: return rawText to rawRuns.toList()
    if (rawText.isBlank()) return rawText to rawRuns.toList()
    val trimmedPrefix = prefix.trimEnd()
    if (trimmedPrefix.isNotEmpty() && rawText.trimStart().startsWith(trimmedPrefix)) {
        return rawText to rawRuns.toList()
    }
    val prefixRun = TextRun(
        text = prefix,
        isBold = label.isBold == true,
        isItalic = label.isItalic == true,
        styleName = label.textStyleName
    )
    val baseRuns = if (rawRuns.isEmpty() && rawText.isNotEmpty()) {
        listOf(TextRun(text = rawText))
    } else {
        rawRuns
    }
    return (prefix + rawText) to (listOf(prefixRun) + baseRuns)
}

/**
 * Context for <text:h> headings.
 */
class OdfHeadingContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val enclosingListStyleName: String? = null,
    private val enclosingListLevel: Int? = null,
    private val listStartValueOverride: Int? = null,
    private val continueListNumbering: Boolean = false,
    initialBookmarks: Collection<String> = emptyList()
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()
    private val runs = mutableListOf<TextRun>()
    private val bookmarks = LinkedHashSet<String>(initialBookmarks)
    private val level: Int = attributes["text:outline-level"]?.toIntOrNull()
        ?: attributes["outline-level"]?.toIntOrNull() ?: 1
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]
    private val startValueOverride: Int? = (attributes["text:start-value"] ?: attributes["start-value"])?.toIntOrNull()
        ?: listStartValueOverride
    private val isListHeader: Boolean =
        (attributes["text:is-list-header"] ?: attributes["is-list-header"]) == "true"

    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        if (importFilter.hasPageBreakBefore(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }

    override fun onCharacters(text: String) {
        textBuilder.append(text)
        runs.add(TextRun(text = text))
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onSpanParsed = { spanText, run ->
                    textBuilder.append(spanText)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_A -> OdfHyperlinkContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onRunEmitted = { run ->
                    textBuilder.append(run.text)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_BOOKMARK_END -> {
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                textBuilder.append(spaces)
                runs.add(TextRun(text = spaces))
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val headingText = textBuilder.toString()
        val label = if (!isListHeader && headingText.isNotBlank()) {
            importFilter.formatHeadingLabel(
                styleName = styleName,
                outlineLevel = level,
                enclosingListStyleName = enclosingListStyleName,
                enclosingListLevel = enclosingListLevel,
                startValueOverride = startValueOverride,
                continueNumbering = continueListNumbering
            )
        } else {
            null
        }
        val (finalText, finalRuns) = prependHeadingLabelIfNeeded(headingText, runs, label)
        val heading = OfficeDocumentElement.Heading(
            text = finalText,
            level = level,
            styleName = styleName,
            runs = finalRuns,
            bookmarks = bookmarks.toList()
        )
        importFilter.addElement(heading)
        if (importFilter.hasPageBreakAfter(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }
}

/**
 * Context for <text:a> hyperlinks (ODF 1.4 Part 3 section 6.1.8).
 * Preserves `xlink:href` across direct character runs, nested `<text:span>` runs,
 * `<text:s/>`, and `<text:tab/>` (for example in `<text:table-of-content>` entries).
 */
class OdfHyperlinkContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val onRunEmitted: (TextRun) -> Unit,
    private val onBookmarkFound: ((String) -> Unit)? = null
) : SvXMLImportContext(importFilter, token) {

    private val href: String? = (attributes["xlink:href"] ?: attributes["href"])?.takeIf { it.isNotEmpty() }
    private val styleName: String? = (attributes["text:style-name"] ?: attributes["style-name"])?.takeIf { it.isNotBlank() }

    private fun emitDirectRun(text: String) {
        if (text.isEmpty()) return
        val format = importFilter.resolveSpanFormatting(styleName)
        onRunEmitted(
            TextRun(
                text = text,
                isBold = format.isBold,
                isItalic = format.isItalic,
                // A hyperlink run is underlined by the app's own convention;
                // the span's declaration still wins when it makes one.
                isUnderline = if (href != null) true else format.isUnderline,
                styleName = styleName,
                hyperlink = href
            )
        )
    }

    override fun onCharacters(text: String) {
        emitDirectRun(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                inheritedHyperlink = href,
                fallbackStyleName = styleName,
                onSpanParsed = { _, run -> onRunEmitted(run) },
                onBookmarkFound = onBookmarkFound
            )
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                emitDirectRun(spaces)
            }
            OdfXmlToken.XML_TAB -> {
                emitDirectRun("\t")
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_LINE_BREAK -> {
                emitDirectRun("\n")
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    onBookmarkFound?.invoke(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            else -> super.createChildContext(token, attributes)
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
    private val inheritedHyperlink: String? = null,
    private val fallbackStyleName: String? = null,
    private val onBookmarkFound: ((String) -> Unit)? = null,
    private val onSpanParsed: (String, TextRun) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val spanTextBuilder = StringBuilder()
    private val styleName = (attributes["text:style-name"] ?: attributes["style-name"])
        ?.takeIf { it.isNotBlank() }
        ?: fallbackStyleName
        ?: ""

    private fun flushAccumulatedSegment() {
        if (spanTextBuilder.isEmpty()) return
        val text = spanTextBuilder.toString()
        spanTextBuilder.setLength(0)
        val format = importFilter.resolveSpanFormatting(styleName)
        val run = TextRun(
            text = text,
            isBold = format.isBold,
            isItalic = format.isItalic,
            isUnderline = if (inheritedHyperlink != null) true else format.isUnderline,
            styleName = styleName.takeIf { it.isNotBlank() },
            hyperlink = inheritedHyperlink
        )
        onSpanParsed(text, run)
    }

    override fun onCharacters(text: String) {
        spanTextBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                spanTextBuilder.append(spaces)
            }
            OdfXmlToken.XML_TAB -> {
                spanTextBuilder.append("\t")
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_LINE_BREAK -> {
                spanTextBuilder.append("\n")
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_A -> {
                flushAccumulatedSegment()
                OdfHyperlinkContext(
                    importFilter = importFilter,
                    token = token,
                    attributes = attributes,
                    onRunEmitted = { run -> onSpanParsed(run.text, run) },
                    onBookmarkFound = onBookmarkFound
                )
            }
            OdfXmlToken.XML_SPAN -> {
                flushAccumulatedSegment()
                OdfSpanContext(
                    importFilter = importFilter,
                    token = token,
                    attributes = attributes,
                    inheritedHyperlink = inheritedHyperlink,
                    fallbackStyleName = styleName.takeIf { it.isNotBlank() },
                    onBookmarkFound = onBookmarkFound,
                    onSpanParsed = onSpanParsed
                )
            }
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    onBookmarkFound?.invoke(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        flushAccumulatedSegment()
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
    val listLevel: Int,
    attributes: Map<String, String> = emptyMap(),
    parentListStyleName: String? = null,
    parentContinueNumbering: Boolean = false
) : SvXMLImportContext(importFilter, token) {

    private val continueNumbering: Boolean =
        parentContinueNumbering ||
            (attributes["text:continue-numbering"] ?: attributes["continue-numbering"]) == "true" ||
            !(attributes["text:continue-list"] ?: attributes["continue-list"]).isNullOrBlank()

    private val listStyleName: String? = importFilter.onListStarted(
        styleName = (attributes["text:style-name"] ?: attributes["style-name"])?.takeIf { it.isNotBlank() },
        listLevel = listLevel,
        continueNumbering = continueNumbering
    ) ?: parentListStyleName

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_LIST_ITEM -> {
                OdfListItemContext(
                    importFilter = importFilter,
                    token = token,
                    listLevel = listLevel,
                    attributes = attributes,
                    listStyleName = listStyleName,
                    continueNumbering = continueNumbering,
                    isListHeader = false
                )
            }
            OdfXmlToken.XML_LIST_HEADER -> {
                OdfListItemContext(
                    importFilter = importFilter,
                    token = token,
                    listLevel = listLevel,
                    attributes = attributes,
                    listStyleName = listStyleName,
                    continueNumbering = continueNumbering,
                    isListHeader = true
                )
            }
            OdfXmlToken.XML_LIST -> OdfListContext(
                importFilter = importFilter,
                token = token,
                listLevel = listLevel + 1,
                attributes = attributes,
                parentListStyleName = listStyleName,
                parentContinueNumbering = continueNumbering
            )
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
    private val listLevel: Int,
    attributes: Map<String, String> = emptyMap(),
    private val listStyleName: String? = null,
    private val continueNumbering: Boolean = false,
    private val isListHeader: Boolean = false
) : SvXMLImportContext(importFilter, token) {

    private val directTextBuilder = StringBuilder()
    private val pendingBookmarks = LinkedHashSet<String>()
    private val startValueOverride: Int? =
        (attributes["text:start-value"] ?: attributes["start-value"])?.toIntOrNull()
    private var hasChildBlock = false
    private var hasEmittedFirstBlock = false

    override fun onCharacters(text: String) {
        directTextBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> {
                hasChildBlock = true
                val isFirst = !hasEmittedFirstBlock
                hasEmittedFirstBlock = true
                val inheritedBookmarks = pendingBookmarks.toList()
                pendingBookmarks.clear()
                OdfListItemParagraphContext(
                    importFilter = importFilter,
                    token = token,
                    attributes = attributes,
                    listLevel = listLevel,
                    listStyleName = listStyleName,
                    startValueOverride = if (isFirst) startValueOverride else null,
                    continueNumbering = continueNumbering,
                    isHeaderOrContinuation = isListHeader || !isFirst,
                    initialBookmarks = inheritedBookmarks
                )
            }
            OdfXmlToken.XML_H -> {
                hasChildBlock = true
                val isFirst = !hasEmittedFirstBlock
                hasEmittedFirstBlock = true
                val inheritedBookmarks = pendingBookmarks.toList()
                pendingBookmarks.clear()
                OdfHeadingContext(
                    importFilter = importFilter,
                    token = token,
                    attributes = attributes,
                    enclosingListStyleName = listStyleName,
                    enclosingListLevel = listLevel,
                    listStartValueOverride = if (isFirst) startValueOverride else null,
                    continueListNumbering = continueNumbering,
                    initialBookmarks = inheritedBookmarks
                )
            }
            OdfXmlToken.XML_LIST -> {
                hasChildBlock = true
                OdfListContext(
                    importFilter = importFilter,
                    token = token,
                    listLevel = listLevel + 1,
                    attributes = attributes,
                    parentListStyleName = listStyleName,
                    parentContinueNumbering = continueNumbering
                )
            }
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    pendingBookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val text = directTextBuilder.toString()
        if (!hasChildBlock && text.isNotBlank()) {
            val label = if (isListHeader) {
                OdfFormattedListLabel(bullet = "", isOrdered = false)
            } else {
                importFilter.formatListItemLabel(
                    listStyleName = listStyleName,
                    paragraphStyleName = null,
                    listLevel = listLevel,
                    startValueOverride = startValueOverride,
                    continueNumbering = continueNumbering
                )
            }
            val listItem = OfficeDocumentElement.ListItem(
                text = text,
                level = listLevel,
                bullet = label.bullet,
                isOrdered = label.isOrdered,
                runs = listOf(TextRun(text = text)),
                labelFontSizeSp = label.labelFontSizeSp,
                labelFontFamily = label.labelFontFamily,
                bookmarks = pendingBookmarks.toList()
            )
            importFilter.addElement(listItem)
        }
    }
}

class OdfListItemParagraphContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String> = emptyMap(),
    private val listLevel: Int = 1,
    private val listStyleName: String? = null,
    private val startValueOverride: Int? = null,
    private val continueNumbering: Boolean = false,
    private val isHeaderOrContinuation: Boolean = false,
    initialBookmarks: Collection<String> = emptyList()
) : SvXMLImportContext(importFilter, token) {

    private val paragraphBuilder = StringBuilder()
    private val runs = mutableListOf<TextRun>()
    private val bookmarks = LinkedHashSet<String>(initialBookmarks)
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]

    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        if (importFilter.hasPageBreakBefore(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }

    override fun onCharacters(text: String) {
        paragraphBuilder.append(text)
        runs.add(TextRun(text = text))
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onSpanParsed = { spanText, run ->
                    paragraphBuilder.append(spanText)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_A -> OdfHyperlinkContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onRunEmitted = { run ->
                    paragraphBuilder.append(run.text)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_BOOKMARK_END -> {
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                paragraphBuilder.append(spaces)
                runs.add(TextRun(text = spaces))
            }
            OdfXmlToken.XML_TAB -> {
                paragraphBuilder.append("\t")
                runs.add(TextRun(text = "\t"))
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_LINE_BREAK -> {
                paragraphBuilder.append("\n")
                runs.add(TextRun(text = "\n"))
                super.createChildContext(token, attributes)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val text = paragraphBuilder.toString()
        val headingLvl = importFilter.resolveHeadingLevel(styleName)
        if (headingLvl != null && text.isNotBlank()) {
            val label = if (isHeaderOrContinuation) {
                null
            } else {
                importFilter.formatHeadingLabel(
                    styleName = styleName,
                    outlineLevel = headingLvl,
                    enclosingListStyleName = listStyleName,
                    enclosingListLevel = listLevel,
                    startValueOverride = startValueOverride,
                    continueNumbering = continueNumbering
                )
            }
            val (finalText, finalRuns) = prependHeadingLabelIfNeeded(text, runs, label)
            importFilter.addElement(
                OfficeDocumentElement.Heading(
                    text = finalText,
                    level = headingLvl,
                    styleName = styleName,
                    runs = finalRuns,
                    bookmarks = bookmarks.toList()
                )
            )
        } else if (text.isNotBlank() || bookmarks.isNotEmpty()) {
            val label = if (isHeaderOrContinuation || text.isBlank()) {
                OdfFormattedListLabel(bullet = "", isOrdered = false)
            } else {
                importFilter.formatListItemLabel(
                    listStyleName = listStyleName,
                    paragraphStyleName = styleName,
                    listLevel = listLevel,
                    startValueOverride = startValueOverride,
                    continueNumbering = continueNumbering
                )
            }
            importFilter.addElement(
                OfficeDocumentElement.ListItem(
                    text = text,
                    level = listLevel,
                    bullet = label.bullet,
                    isOrdered = label.isOrdered,
                    styleName = styleName,
                    runs = runs.toList(),
                    labelFontSizeSp = label.labelFontSizeSp,
                    labelFontFamily = label.labelFontFamily,
                    bookmarks = bookmarks.toList()
                )
            )
        }
        if (importFilter.hasPageBreakAfter(styleName)) {
            importFilter.addElement(OfficeDocumentElement.PageBreak)
        }
    }
}

/** Parse a positive ODF repeat count without silently dropping declarations. */
private fun parseTableRepeat(raw: String?, onInvalid: (String) -> Unit): Int {
    if (raw.isNullOrBlank()) return 1
    val parsed = raw.trim().toLongOrNull()
    return when {
        parsed == null || parsed <= 0L -> {
            onInvalid("Invalid table repeat count '$raw'; using 1")
            1
        }
        parsed > Int.MAX_VALUE.toLong() -> {
            onInvalid("Table repeat count '$raw' exceeds the model range; using Int.MAX_VALUE")
            Int.MAX_VALUE
        }
        else -> parsed.toInt()
    }
}

/**
 * Context for <table:table>.
 *
 * Rows, cells and columns stay compact here. The repeat counts are source
 * declarations; TableGridResolver expands them later without duplicating the
 * source text or losing provenance.
 */
class OdfTableContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String> = emptyMap()
) : SvXMLImportContext(importFilter, token) {

    val tableName: String = attributes["table:name"] ?: attributes["name"] ?: ""
    val styleName: String? = (attributes["table:style-name"] ?: attributes["style-name"])
        ?.takeIf { it.isNotBlank() }
    val rows = mutableListOf<TableRow>()
    val columns = mutableListOf<OfficeTableColumnSpec>()
    val diagnostics = mutableListOf<TableDiagnostic>()

    fun addDiagnostic(
        code: TableDiagnosticCode,
        message: String,
        sourceRowOrdinal: Int? = null,
        sourceCellOrdinal: Int? = null
    ) {
        diagnostics += TableDiagnostic(code, message, sourceRowOrdinal, sourceCellOrdinal)
    }

    fun addColumn(column: OfficeTableColumnSpec) {
        columns += column.copy(sourceColumnOrdinal = columns.size)
    }

    fun addRow(row: TableRow) {
        rows += row.copy(sourceRowOrdinal = rows.size)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TABLE_COLUMN -> OdfTableColumnContext(importFilter, token, attributes, this)
            OdfXmlToken.XML_TABLE_ROW -> OdfTableRowContext(importFilter, token, attributes, this, isHeader = false)
            OdfXmlToken.XML_TABLE_HEADER_ROWS -> OdfTableHeaderRowsContext(importFilter, token, this)
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val declaredColumns = columns.sumOf { it.repeatCount.toLong() }
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val rowColumns = rows.maxOfOrNull { row ->
            row.cells.sumOf { it.repeatCount.toLong() }
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } ?: 0
        val tableElement = OfficeDocumentElement.Table(
            rows = rows.toList(),
            numColumns = maxOf(declaredColumns, rowColumns),
            name = tableName.ifBlank { null },
            columns = columns.toList(),
            styleName = styleName,
            tableWidth = importFilter.resolveTableWidth(styleName),
            diagnostics = diagnostics.toList()
        )
        // A declared empty table is still a table. This also keeps source
        // diagnostics visible to the later geometry stage.
        if (rows.isNotEmpty() || columns.isNotEmpty() || styleName != null) {
            importFilter.addElement(tableElement)
        }
    }
}

/** Context for one compact <table:table-column> declaration. */
class OdfTableColumnContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val attributes: Map<String, String>,
    private val parentTableContext: OdfTableContext
) : SvXMLImportContext(importFilter, token) {
    override fun onEndElement(token: OdfXmlToken) {
        val styleName = (attributes["table:style-name"] ?: attributes["style-name"])
            ?.takeIf { it.isNotBlank() }
        val repeatCount = parseTableRepeat(
            attributes["table:number-columns-repeated"] ?: attributes["number-columns-repeated"]
        ) { message ->
            parentTableContext.addDiagnostic(TableDiagnosticCode.INVALID_REPEAT, message)
        }
        val directWidth = com.makerandreas.papirusoffice.data.util.OdfLength.toLayoutUnits(
            attributes["style:column-width"] ?: attributes["column-width"], fallback = -1f
        ).takeIf { it >= 0f && it.isFinite() }
        val width = if (directWidth != null) {
            com.makerandreas.papirusoffice.data.TableColumnWidthSpec(
                com.makerandreas.papirusoffice.data.TableColumnWidthKind.ABSOLUTE,
                directWidth
            )
        } else {
            importFilter.resolveTableColumnWidth(styleName)
        }
        val defaultCellStyleName = attributes["table:default-cell-style-name"]
            ?: attributes["default-cell-style-name"]
            ?: importFilter.resolveTableDefaultCellStyle(styleName)
        parentTableContext.addColumn(
            OfficeTableColumnSpec(
                styleName = styleName,
                width = width,
                repeatCount = repeatCount,
                defaultCellStyleName = defaultCellStyleName
            )
        )
    }
}

/** ODF header rows are a wrapper, not a row. */
class OdfTableHeaderRowsContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val parentTableContext: OdfTableContext
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TABLE_ROW -> OdfTableRowContext(
                importFilter, token, attributes, parentTableContext, isHeader = true
            )
            else -> super.createChildContext(token, attributes)
        }
    }
}

/** Context for <table:table-row>. */
class OdfTableRowContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val attributes: Map<String, String> = emptyMap(),
    private val parentTableContext: OdfTableContext,
    private val isHeader: Boolean
) : SvXMLImportContext(importFilter, token) {

    val cells = mutableListOf<TableCell>()
    private val styleName: String? = (attributes["table:style-name"] ?: attributes["style-name"])
        ?.takeIf { it.isNotBlank() }

    fun addCell(cell: TableCell) {
        cells += cell.copy(sourceCellOrdinal = cells.size)
    }

    fun addDiagnostic(code: TableDiagnosticCode, message: String) {
        parentTableContext.addDiagnostic(code, message)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_TABLE_CELL, OdfXmlToken.XML_COVERED_TABLE_CELL -> {
                OdfTableCellContext(importFilter, token, attributes, this)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        val repeatCount = parseTableRepeat(
            attributes["table:number-rows-repeated"] ?: attributes["number-rows-repeated"]
        ) { message ->
            parentTableContext.addDiagnostic(TableDiagnosticCode.INVALID_REPEAT, message)
        }
        parentTableContext.addRow(
            TableRow(
                cells = cells.toList(),
                styleName = styleName,
                isHeader = isHeader,
                repeatCount = repeatCount,
                rowStyle = importFilter.resolveTableRowStyle(styleName)
            )
        )
    }
}

/** Context for <table:table-cell> and <table:covered-table-cell>. */
class OdfTableCellContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val attributes: Map<String, String>,
    private val parentRowContext: OdfTableRowContext
) : SvXMLImportContext(importFilter, token) {

    private val textBuilder = StringBuilder()
    private val cellParagraphs = mutableListOf<OfficeDocumentElement.Paragraph>()
    private val covered = token == OdfXmlToken.XML_COVERED_TABLE_CELL

    override fun onCharacters(text: String) {
        textBuilder.append(text)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_P -> OdfTableCellParagraphContext(importFilter, token, attributes) { cellPara ->
                if (textBuilder.isNotEmpty()) textBuilder.append(" ")
                textBuilder.append(cellPara.text)
                cellParagraphs.add(cellPara)
            }
            OdfXmlToken.XML_SECTION -> {
                importFilter.reportUnsupported("text:section", attributes)
                OdfTableCellSectionContext(importFilter, token, this)
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        var rawText = textBuilder.toString().trim()
        if (rawText.isEmpty()) {
            val officeVal = attributes["office:value"] ?: attributes["office:date-value"] ?: attributes["office:boolean-value"]
            if (!officeVal.isNullOrBlank()) rawText = officeVal
        }
        if (covered && (rawText.isNotEmpty() || cellParagraphs.isNotEmpty())) {
            parentRowContext.addDiagnostic(
                TableDiagnosticCode.UNSUPPORTED_DECLARATION,
                "Covered table cell content was ignored to preserve the anchor text"
            )
            rawText = ""
            cellParagraphs.clear()
        }
        val repeatCount = parseTableRepeat(
            attributes["table:number-columns-repeated"] ?: attributes["number-columns-repeated"]
        ) { message ->
            parentRowContext.addDiagnostic(TableDiagnosticCode.INVALID_REPEAT, message)
        }
        val columnSpan = parseTableSpan(
            attributes["table:number-columns-spanned"] ?: attributes["number-columns-spanned"],
            "column",
            parentRowContext
        )
        val rowSpan = parseTableSpan(
            attributes["table:number-rows-spanned"] ?: attributes["number-rows-spanned"],
            "row",
            parentRowContext
        )
        val styleName = (attributes["table:style-name"] ?: attributes["style-name"])
            ?.takeIf { it.isNotBlank() }
        val cell = TableCell(
            text = rawText,
            paragraphs = if (cellParagraphs.isNotEmpty()) {
                cellParagraphs.toList()
            } else if (rawText.isNotEmpty() && !covered) {
                listOf(OfficeDocumentElement.Paragraph(text = rawText))
            } else {
                emptyList()
            },
            columnSpan = columnSpan,
            rowSpan = rowSpan,
            occupancy = if (covered) TableCellOccupancy.COVERED else TableCellOccupancy.ORIGIN,
            repeatCount = repeatCount,
            styleName = styleName,
            boxStyle = importFilter.resolveTableCellBoxStyle(styleName)
        )
        parentRowContext.addCell(cell)
    }

    private fun parseTableSpan(raw: String?, axis: String, row: OdfTableRowContext): Int {
        if (raw.isNullOrBlank()) return 1
        val parsed = raw.trim().toLongOrNull()
        return when {
            parsed == null || parsed <= 0L -> {
                row.addDiagnostic(
                    TableDiagnosticCode.INVALID_SPAN,
                    "Invalid $axis span '$raw'; using 1"
                )
                1
            }
            parsed > Int.MAX_VALUE.toLong() -> {
                row.addDiagnostic(
                    TableDiagnosticCode.INVALID_SPAN,
                    "$axis span '$raw' exceeds the model range; using Int.MAX_VALUE"
                )
                Int.MAX_VALUE
            }
            else -> parsed.toInt()
        }
    }
}

class OdfTableCellParagraphContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String> = emptyMap(),
    private val onParagraphExtracted: (OfficeDocumentElement.Paragraph) -> Unit
) : SvXMLImportContext(importFilter, token) {

    private val pBuilder = StringBuilder()
    private val runs = mutableListOf<TextRun>()
    private val bookmarks = LinkedHashSet<String>()
    private val styleName: String? = attributes["text:style-name"] ?: attributes["style-name"]

    override fun onCharacters(text: String) {
        pBuilder.append(text)
        runs.add(TextRun(text = text))
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_SPAN -> OdfSpanContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onSpanParsed = { spanText, run ->
                    pBuilder.append(spanText)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_A -> OdfHyperlinkContext(
                importFilter = importFilter,
                token = token,
                attributes = attributes,
                onRunEmitted = { run ->
                    pBuilder.append(run.text)
                    runs.add(run)
                },
                onBookmarkFound = { name ->
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
            )
            OdfXmlToken.XML_BOOKMARK, OdfXmlToken.XML_BOOKMARK_START -> {
                val name = (attributes["text:name"] ?: attributes["name"])?.trim()
                if (!name.isNullOrEmpty()) {
                    bookmarks.add(name)
                    importFilter.recordBookmark(name)
                }
                super.createChildContext(token, attributes)
            }
            OdfXmlToken.XML_S -> OdfSpaceContext(importFilter, token, attributes) { spaces ->
                pBuilder.append(spaces)
                runs.add(TextRun(text = spaces))
            }
            else -> super.createChildContext(token, attributes)
        }
    }

    override fun onEndElement(token: OdfXmlToken) {
        onParagraphExtracted(
            OfficeDocumentElement.Paragraph(
                text = pBuilder.toString(),
                styleName = styleName,
                runs = runs.toList(),
                bookmarks = bookmarks.toList()
            )
        )
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
    private val frameName: String? = attributes["draw:name"] ?: attributes["name"]

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        return when (token) {
            OdfXmlToken.XML_IMAGE -> OdfImageContext(importFilter, token, attributes, widthDp, heightDp, frameName)
            OdfXmlToken.XML_TEXT_BOX, OdfXmlToken.XML_CUSTOM_SHAPE, OdfXmlToken.XML_G -> {
                OdfDrawingContainerContext(importFilter, token)
            }
            OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
            OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
            OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
            else -> super.createChildContext(token, attributes)
        }
    }

    // Frames and the page box share one length scale (layout units at
    // 96/inch) so declared image sizes compare against page width correctly.
    private fun parseDimensionToDp(dimStr: String?): Float {
        if (dimStr.isNullOrBlank()) return 100f
        return com.makerandreas.papirusoffice.data.util.OdfLength.toLayoutUnits(dimStr, fallback = 100f)
    }
}

class OdfImageContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>,
    private val widthDp: Float,
    private val heightDp: Float,
    private val objectName: String? = null
) : SvXMLImportContext(importFilter, token) {

    private val href: String? = attributes["xlink:href"] ?: attributes["href"]

    override fun onEndElement(token: OdfXmlToken) {
        if (!href.isNullOrBlank()) {
            val imgFile = importFilter.extractedImages[href] ?: importFilter.extractedImages[href.substringAfterLast("/")]
            val imageElement = OfficeDocumentElement.ImageElement(
                imagePath = href,
                imageFile = imgFile,
                widthDp = widthDp,
                heightDp = heightDp,
                name = objectName
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
