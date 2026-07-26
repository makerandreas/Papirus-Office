package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Styles & Families
// ---------------------------------------------------------

interface XStyle {
    var name: String
    val isUserDefined: Boolean
    val isInUse: Boolean
    var parentStyle: String
}

interface XStyleFamiliesSupplier {
    val styleFamilies: XNameAccess
}

interface XStyleLoader {
    fun loadStylesFromURL(url: String, options: MediaDescriptor)
    fun getStyleLoaderOptions(): MediaDescriptor
}

// ---------------------------------------------------------
// AutoText
// ---------------------------------------------------------

interface XAutoTextEntry {
    fun applyTo(cursor: XTextRange)
}

interface XAutoTextGroup {
    fun getTitles(): List<String>
    fun getElementNames(): List<String>
    fun getByName(name: String): Any // returns XAutoTextEntry
    fun insertNewByName(name: String, title: String, textRange: XTextRange): XAutoTextEntry
    fun removeByName(name: String)
}

interface XAutoTextContainer : XNameAccess {
    fun insertNewByName(name: String): XAutoTextGroup
    fun removeByName(name: String)
}

// ---------------------------------------------------------
// Line & Chapter Numbering
// ---------------------------------------------------------

interface XLineNumberingProperties {
    val lineNumberingProperties: Any // Represents a XPropertySet
}

interface XChapterNumberingSupplier {
    val chapterNumberingRules: Any // Represents an XIndexReplace
}

interface XIndexReplace : XIndexAccess {
    fun replaceByIndex(index: Int, element: Any)
}

object NumberingType {
    const val CHARS_UPPER_LETTER: Short = 0
    const val CHARS_LOWER_LETTER: Short = 1
    const val ROMAN_UPPER: Short = 2
    const val ROMAN_LOWER: Short = 3
    const val ARABIC: Short = 4
    const val NUMBER_NONE: Short = 5
}

// ---------------------------------------------------------
// Formatting & Typography Properties
// ---------------------------------------------------------

data class LineSpacing(
    var Mode: Short = 0, // LineSpacingMode
    var Height: Short = 0
)

object LineSpacingMode {
    const val PROPORTIONAL: Short = 0
    const val MINIMUM: Short = 1
    const val LEADING: Short = 2
    const val FIX: Short = 3
}

object FontWeight {
    const val DONTKNOW: Float = 0f
    const val THIN: Float = 50f
    const val ULTRALIGHT: Float = 60f
    const val LIGHT: Float = 75f
    const val SEMILIGHT: Float = 90f
    const val NORMAL: Float = 100f
    const val SEMIBOLD: Float = 110f
    const val BOLD: Float = 150f
    const val ULTRABOLD: Float = 175f
    const val BLACK: Float = 200f
}

object FontSlant {
    const val NONE: Short = 0
    const val OBLIQUE: Short = 1
    const val ITALIC: Short = 2
    const val DONTKNOW: Short = 3
    const val REVERSE_OBLIQUE: Short = 4
    const val REVERSE_ITALIC: Short = 5
}

object ParagraphAdjust {
    const val LEFT: Short = 0
    const val RIGHT: Short = 1
    const val BLOCK: Short = 2
    const val CENTER: Short = 3
    const val STRETCH: Short = 4
}

object BreakType {
    const val NONE: Short = 0
    const val COLUMN_BEFORE: Short = 1
    const val COLUMN_AFTER: Short = 2
    const val COLUMN_BOTH: Short = 3
    const val PAGE_BEFORE: Short = 4
    const val PAGE_AFTER: Short = 5
    const val PAGE_BOTH: Short = 6
}

// ---------------------------------------------------------
// Text Columns
// ---------------------------------------------------------

interface XTextColumns {
    var columnCount: Short
    var columns: List<TextColumn>
    val referenceValue: Int
}

data class TextColumn(
    var Width: Int = 0,
    var LeftMargin: Int = 0,
    var RightMargin: Int = 0
)

// ---------------------------------------------------------
// Document Info & Settings
// ---------------------------------------------------------

interface XDocumentPropertiesSupplier {
    val documentProperties: Any // Represents XPropertySet or specific DocumentProperties
}

interface XLinkTargetSupplier {
    val links: XNameAccess
}
