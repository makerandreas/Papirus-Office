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
