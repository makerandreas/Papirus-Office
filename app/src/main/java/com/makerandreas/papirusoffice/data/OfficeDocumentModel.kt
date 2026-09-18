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
        val runs: List<TextRun> = emptyList()
    ) : OfficeDocumentElement()

    data class Heading(
        val text: String,
        val level: Int = 1,
        val styleName: String? = null
    ) : OfficeDocumentElement()

    data class ListItem(
        val text: String,
        val level: Int = 1,
        val bullet: String = "• "
    ) : OfficeDocumentElement()

    data class Table(
        val rows: List<TableRow>,
        val numColumns: Int = 0
    ) : OfficeDocumentElement()

    data class ImageElement(
        val imagePath: String,
        val imageFile: File? = null,
        val widthDp: Float = 0f,
        val heightDp: Float = 0f
    ) : OfficeDocumentElement()

    data object PageBreak : OfficeDocumentElement()
}

data class TableRow(
    val cells: List<TableCell>
)

data class TableCell(
    val text: String,
    val paragraphs: List<OfficeDocumentElement.Paragraph> = emptyList()
)

data class TextRun(
    val text: String,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

data class OfficeParsedDocument(
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
    val pageCount: Int = 0
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
