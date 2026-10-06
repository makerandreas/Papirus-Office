package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.framework.*
import java.io.File

/**
 * Modern data model representing parsed document structure from ODT or DOCX format.
 */
sealed class OfficeDocumentElement {
    data class Paragraph(
        val text: String,
        val styleName: String? = null,
        val runs: List<TextRun> = emptyList(),
        val pageBreakOffsets: List<Int> = emptyList(),
        val bookmarks: List<String> = emptyList(),
        /** OOXML `w:numPr` state, resolved through the style chain; null when unnumbered. */
        val numbering: DocxNumberingRef? = null
    ) : OfficeDocumentElement()

    data class Heading(
        val text: String,
        val level: Int = 1,
        val styleName: String? = null,
        val runs: List<TextRun> = emptyList(),
        val pageBreakOffsets: List<Int> = emptyList(),
        val bookmarks: List<String> = emptyList(),
        /** OOXML `w:numPr` state, resolved through the style chain; null when unnumbered. */
        val numbering: DocxNumberingRef? = null
    ) : OfficeDocumentElement()

    data class ListItem(
        val text: String,
        val level: Int = 1,
        val bullet: String = "• ",
        val isOrdered: Boolean = false,
        val styleName: String? = null,
        val runs: List<TextRun> = emptyList(),
        val labelFontSizeSp: Float? = null,
        val labelFontFamily: String? = null,
        val bookmarks: List<String> = emptyList()
    ) : OfficeDocumentElement()

    data class Table(
        val rows: List<TableRow>,
        val numColumns: Int = 0,
        val name: String? = null,
        val columns: List<OfficeTableColumnSpec> = emptyList(),
        val styleName: String? = null,
        val tableWidth: TableColumnWidthSpec = TableColumnWidthSpec(),
        val diagnostics: List<TableDiagnostic> = emptyList()
    ) : OfficeDocumentElement() {
        init {
            require(numColumns >= 0) { "Table column count must be non-negative" }
        }
    }

    data class ImageElement(
        val imagePath: String,
        val imageFile: File? = null,
        val widthDp: Float = 0f,
        val heightDp: Float = 0f,
        val name: String? = null
    ) : OfficeDocumentElement()

    data object PageBreak : OfficeDocumentElement()
}

data class TableRow(
    val cells: List<TableCell>,
    val styleName: String? = null,
    val isHeader: Boolean = false,
    val repeatCount: Int = 1,
    val rowStyle: TableRowStyle = TableRowStyle(),
    /** Ordinal in the compact ODF source row list; -1 means synthetic. */
    val sourceRowOrdinal: Int = -1
) {
    init {
        require(repeatCount > 0) { "Table row repeat count must be positive" }
        require(sourceRowOrdinal >= -1) { "Table source row ordinal must be non-negative or -1" }
    }
}

data class TableCell(
    val text: String,
    val paragraphs: List<OfficeDocumentElement.Paragraph> = emptyList(),
    val startColumn: Int = 0,
    val columnSpan: Int = 1,
    val rowSpan: Int = 1,
    val occupancy: TableCellOccupancy = TableCellOccupancy.ORIGIN,
    val repeatCount: Int = 1,
    val styleName: String? = null,
    val boxStyle: TableCellBoxStyle = TableCellBoxStyle(),
    /** Ordinal in the compact source row; -1 means synthetic. */
    val sourceCellOrdinal: Int = -1
) {
    init {
        require(startColumn >= 0) { "Table cell start column must be non-negative" }
        require(columnSpan > 0 && rowSpan > 0) { "Table cell spans must be positive" }
        require(repeatCount > 0) { "Table cell repeat count must be positive" }
        require(sourceCellOrdinal >= -1) { "Table source cell ordinal must be non-negative or -1" }
    }
}

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val styleName: String? = null,
    val hyperlink: String? = null
)

/**
 * Effective OOXML numbering state of one paragraph, resolved from `w:numPr`
 * (`w:ilvl` + `w:numId`, ECMA-376 Part 1 §17.9.6 and §17.9.18).
 *
 * [numId] `0` is not a definition reference: the document uses it to suppress
 * the numbering a style would otherwise give the paragraph (`[MS-OI29500]`
 * §17.9.18 note, p.125), so [suppressed] records that fact instead of leaving
 * a caller to interpret the zero. [fromStyle] is true when no `w:pPr/w:numPr`
 * is present and the reference comes from the paragraph style chain, which is
 * the shape Word writes for headings and list styles.
 *
 * Plan 8A resolves and carries this state; Plan 8B reads `word/numbering.xml`
 * and renders the label from it.
 */
data class DocxNumberingRef(
    val numId: Int,
    val ilvl: Int = 0,
    val suppressed: Boolean = false,
    val fromStyle: Boolean = false
)

data class OfficeParsedDocument(
    val sectionStarts: List<SectionStart> = emptyList(),
    val elements: List<OfficeDocumentElement> = emptyList(),
    val rawXml: String = "",
    val plainText: String = "",
    val extractedImages: Map<String, File> = emptyMap(),
    val isOdt: Boolean = false,
    val isDocx: Boolean = false,
    val isOds: Boolean = false,
    val isXlsx: Boolean = false,
    val isOdp: Boolean = false,
    val isPptx: Boolean = false,
    val isParsingFailed: Boolean = false,
    val failureReason: String? = null,
    val odtPackageData: OdtPackageData? = null,
    val pageCount: Int = 0,
    val styles: DocumentStyles = DocumentStyles(),
    val bookmarks: List<String> = emptyList(),
    val metadata: DocumentMetadata = DocumentMetadata(),
    val authoredIndexes: List<DocumentIndexRange> = emptyList(),
    val namedSectionRanges: List<DocumentSectionRange> = emptyList()
) : BaseOfficeModel(url = "", args = emptyList()), XTextDocument, XDocumentPropertiesSupplier, XReplaceable {
    
    override val text: XText
        get() = DocumentTextImpl(this)
        
    override val documentProperties: Any
        get() = mapOf(
            "CharacterCount" to plainText.length,
            "ParagraphCount" to elements.filterIsInstance<OfficeDocumentElement.Paragraph>().size,
            "WordCount" to plainText.split(Regex("\\s+")).count { it.isNotBlank() }
        )

    override fun reformat() {
        // Implementation for reformatting layout
    }

    // Search/replace is not implemented for the parsed model yet. These
    // deliberately return safe no-op results instead of throwing, so any
    // current or future caller (e.g. Find & Replace UI) degrades gracefully.
    override fun createReplaceDescriptor(): XReplaceDescriptor = SearchDescriptorStub(replace = "")
    override fun replaceAll(descriptor: XSearchDescriptor): Long = 0L
    override fun createSearchDescriptor(): XSearchDescriptor = SearchDescriptorStub(replace = null)
    override fun findAll(descriptor: XSearchDescriptor): Any = emptyList<Any>()
    override fun findFirst(descriptor: XSearchDescriptor): Any? = null
    override fun findNext(startAt: Any, descriptor: XSearchDescriptor): Any? = null
}

class DocumentTextImpl(private val document: OfficeParsedDocument) : XText {
    override val text: XText get() = this
    override val start: XTextRange get() = this // simplified
    override val end: XTextRange get() = this // simplified
    override var string: String
        get() = document.plainText
        set(value) {}

    override fun createTextCursor(): XTextCursor = TextCursorStub(text)
    override fun createTextCursorByRange(textPosition: XTextRange): XTextCursor = TextCursorStub(text)
    override fun insertString(range: XTextRange, string: String, absorb: Boolean) {}
    override fun insertControlCharacter(range: XTextRange, controlCharacter: Short, absorb: Boolean) {}
    override fun insertTextContent(range: XTextRange, content: XTextContent, absorb: Boolean) {}
    override fun removeTextContent(content: XTextContent) {}
}

/**
 * Mutable search/replace descriptor stub. Carries the query options so
 * callers can construct descriptors; the parsed-model search itself is
 * unimplemented and always yields no matches (see [OfficeParsedDocument]).
 */
private class SearchDescriptorStub(replace: String?) : XReplaceDescriptor {
    override var searchString: String = ""
    override var searchBackwards: Boolean = false
    override var searchCaseSensitive: Boolean = false
    override var searchRegularExpression: Boolean = false
    override var searchWords: Boolean = false
    override var replaceString: String = replace ?: ""
}

/**
 * Collapsed no-op text cursor over [owner]. All movement collapses to the
 * start; never throws so cursor-driven callers degrade gracefully.
 */
private class TextCursorStub(private val owner: XText) : XTextCursor {
    override val text: XText get() = owner
    override val start: XTextRange get() = this
    override val end: XTextRange get() = this
    override var string: String
        get() = ""
        set(_) { }

    override fun collapseToStart() { }
    override fun collapseToEnd() { }
    override fun isCollapsed(): Boolean = true
    override fun goLeft(count: Short, expand: Boolean): Boolean = false
    override fun goRight(count: Short, expand: Boolean): Boolean = false
    override fun gotoStart(expand: Boolean) { }
    override fun gotoEnd(expand: Boolean) { }
    override fun gotoRange(range: XTextRange, expand: Boolean) { }
}
