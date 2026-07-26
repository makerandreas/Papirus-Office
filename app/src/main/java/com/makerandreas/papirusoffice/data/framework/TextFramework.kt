package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Text Range & Cursors
// ---------------------------------------------------------

interface XTextRange {
    val text: XText
    val start: XTextRange
    val end: XTextRange
    var string: String
}

interface XSimpleText : XTextRange {
    fun createTextCursor(): XTextCursor
    fun createTextCursorByRange(textPosition: XTextRange): XTextCursor
    fun insertString(range: XTextRange, string: String, absorb: Boolean)
    fun insertControlCharacter(range: XTextRange, controlCharacter: Short, absorb: Boolean)
}

interface XRelativeTextContentInsert {
    fun insertTextContentBefore(newContent: XTextContent, precedentContent: XTextContent)
    fun insertTextContentAfter(newContent: XTextContent, precedentContent: XTextContent)
}

interface XTextRangeCompare {
    fun compareRegionStarts(range1: XTextRange, range2: XTextRange): Short
    fun compareRegionEnds(range1: XTextRange, range2: XTextRange): Short
}

interface XDocumentInsertable {
    fun insertDocumentFromURL(url: String, options: Array<PropertyValue>)
}

interface XText : XSimpleText {
    fun insertTextContent(range: XTextRange, content: XTextContent, absorb: Boolean)
    fun removeTextContent(content: XTextContent)
}

interface XTextCursor : XTextRange {
    fun collapseToStart()
    fun collapseToEnd()
    fun isCollapsed(): Boolean
    fun goLeft(count: Short, expand: Boolean): Boolean
    fun goRight(count: Short, expand: Boolean): Boolean
    fun gotoStart(expand: Boolean)
    fun gotoEnd(expand: Boolean)
    fun gotoRange(range: XTextRange, expand: Boolean)
}

interface XWordCursor : XTextCursor {
    fun gotoNextWord(expand: Boolean): Boolean
    fun gotoPreviousWord(expand: Boolean): Boolean
    fun gotoEndOfWord(expand: Boolean): Boolean
    fun gotoStartOfWord(expand: Boolean): Boolean
    fun isStartOfWord(): Boolean
    fun isEndOfWord(): Boolean
}

interface XSentenceCursor : XTextCursor {
    fun gotoNextSentence(expand: Boolean): Boolean
    fun gotoPreviousSentence(expand: Boolean): Boolean
    fun gotoStartOfSentence(expand: Boolean): Boolean
    fun gotoEndOfSentence(expand: Boolean): Boolean
    fun isStartOfSentence(): Boolean
    fun isEndOfSentence(): Boolean
}

interface XParagraphCursor : XTextCursor {
    fun gotoStartOfParagraph(expand: Boolean): Boolean
    fun gotoEndOfParagraph(expand: Boolean): Boolean
    fun gotoNextParagraph(expand: Boolean): Boolean
    fun gotoPreviousParagraph(expand: Boolean): Boolean
    fun isStartOfParagraph(): Boolean
    fun isEndOfParagraph(): Boolean
}

object ControlCharacter {
    const val PARAGRAPH_BREAK: Short = 0
    const val LINE_BREAK: Short = 1
    const val HARD_HYPHEN: Short = 2
    const val SOFT_HYPHEN: Short = 3
    const val HARD_SPACE: Short = 4
    const val APPEND_PARAGRAPH: Short = 5
}

// ---------------------------------------------------------
// Text Content & Document Model
// ---------------------------------------------------------

interface XTextContent : XComponent {
    val anchor: XTextRange
}

interface XTextDocument : XModel {
    val text: XText
    fun reformat()
}

// ---------------------------------------------------------
// View and Controllers
// ---------------------------------------------------------

interface XTextViewCursorSupplier {
    val viewCursor: XTextViewCursor
}

interface XTextViewCursor : XTextCursor

interface XPageCursor {
    fun jumpToFirstPage(): Boolean
    fun jumpToLastPage(): Boolean
    fun jumpToPage(pageNo: Int): Boolean
    val page: Int
    fun jumpToNextPage(): Boolean
    fun jumpToPreviousPage(): Boolean
    fun jumpToEndOfPage(): Boolean
    fun jumpToStartOfPage(): Boolean
}

interface XLineCursor {
    fun goDown(lines: Int, expand: Boolean): Boolean
    fun goUp(lines: Int, expand: Boolean): Boolean
    fun isAtStartOfLine(): Boolean
    fun isAtEndOfLine(): Boolean
    fun gotoEndOfLine(expand: Boolean)
    fun gotoStartOfLine(expand: Boolean)
}

interface XScreenCursor {
    fun screenDown(): Boolean
    fun screenUp(): Boolean
}

interface XViewCursor {
    fun goLeft(characters: Long, expand: Boolean): Boolean
    fun goRight(characters: Long, expand: Boolean): Boolean
    fun goDown(characters: Long, expand: Boolean): Boolean
    fun goUp(characters: Long, expand: Boolean): Boolean
}

// ---------------------------------------------------------
// Replace and Search
// ---------------------------------------------------------

interface XSearchDescriptor {
    var searchString: String
    var searchBackwards: Boolean
    var searchCaseSensitive: Boolean
    var searchRegularExpression: Boolean
    var searchWords: Boolean
}

interface XReplaceDescriptor : XSearchDescriptor {
    var replaceString: String
}

interface XSearchable {
    fun createSearchDescriptor(): XSearchDescriptor
    fun findAll(descriptor: XSearchDescriptor): Any // Represents XIndexAccess
    fun findFirst(descriptor: XSearchDescriptor): Any?
    fun findNext(startAt: Any, descriptor: XSearchDescriptor): Any?
}

interface XReplaceable : XSearchable {
    fun createReplaceDescriptor(): XReplaceDescriptor
    fun replaceAll(descriptor: XSearchDescriptor): Long
}

// ---------------------------------------------------------
// Table and Text Fields
// ---------------------------------------------------------

interface XCell {
    var value: Double
    var formula: String
}

interface XCellRange {
    fun getCellByPosition(column: Int, row: Int): XCell
    fun getCellRangeByPosition(left: Int, top: Int, right: Int, bottom: Int): XCellRange
    fun getCellRangeByName(range: String): XCellRange
}

interface XTextTable : XTextContent {
    fun initialize(rows: Int, columns: Int)
    fun getCellNames(): List<String>
    fun getCellByName(cellName: String): XCell
    fun getRows(): XTableRows
    fun getColumns(): XTableColumns
    fun createCursorByCellName(cellName: String): XTextTableCursor
}

interface XTableRows {
    fun insertByIndex(index: Int, count: Int)
    fun removeByIndex(index: Int, count: Int)
}

interface XTableColumns {
    fun insertByIndex(index: Int, count: Int)
    fun removeByIndex(index: Int, count: Int)
}

interface XTextTableCursor {
    fun getRangeName(): String
    fun goLeft(count: Short, expand: Boolean): Boolean
    fun goRight(count: Short, expand: Boolean): Boolean
    fun goUp(count: Short, expand: Boolean): Boolean
    fun goDown(count: Short, expand: Boolean): Boolean
    fun gotoStart(expand: Boolean)
    fun gotoEnd(expand: Boolean)
    fun gotoCellByName(cellName: String, expand: Boolean): Boolean
    fun mergeRange(): Boolean
    fun splitRange(count: Short, horizontal: Boolean): Boolean
}

interface XTextField : XTextContent {
    val presentation: String
}

interface XDependentTextField : XTextField {
    fun attachTextFieldMaster(master: Any)
    fun getTextFieldMaster(): Any
}
