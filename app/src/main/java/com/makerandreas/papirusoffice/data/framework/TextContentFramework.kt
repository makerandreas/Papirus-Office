package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Bookmarks, References, Footnotes
// ---------------------------------------------------------

interface XBookmarksSupplier {
    val bookmarks: Any // Represents a XNameAccess
}

interface XReferenceMarksSupplier {
    val referenceMarks: Any // Represents a XNameAccess
}

interface XFootnote : XTextContent {
    var label: String
}

interface XFootnotesSupplier {
    val footnotes: Any // Represents an XIndexAccess
    val footnoteSettings: Any // Represents XPropertySet
}

interface XEndnotesSupplier {
    val endnotes: Any // Represents an XIndexAccess
    val endnoteSettings: Any // Represents XPropertySet
}

// ---------------------------------------------------------
// Indexes
// ---------------------------------------------------------

interface XDocumentIndex : XTextContent {
    val serviceName: String
    fun update()
}

interface XDocumentIndexMark : XTextContent {
    var markEntry: String
}

interface XDocumentIndexesSupplier {
    val documentIndexes: Any // Represents an XIndexAccess & XNameAccess
}

// ---------------------------------------------------------
// Content Suppliers
// ---------------------------------------------------------

interface XTextTablesSupplier {
    val textTables: Any // Represents an XNameAccess
}

interface XTextFramesSupplier {
    val textFrames: Any // Represents an XNameAccess
}

interface XTextGraphicObjectsSupplier {
    val graphicObjects: Any // Represents an XNameAccess
}

interface XTextEmbeddedObjectsSupplier {
    val embeddedObjects: Any // Represents an XNameAccess
}

interface XTextFieldsSupplier {
    val textFields: Any // Represents an XEnumerationAccess
    val textFieldMasters: Any // Represents an XNameAccess
}

// ---------------------------------------------------------
// Text Sections
// ---------------------------------------------------------

interface XTextSection : XTextContent

interface XTextSectionsSupplier {
    val textSections: Any // Represents an XNameAccess
}

// ---------------------------------------------------------
// Drawing Shapes & Frames
// ---------------------------------------------------------

interface XShape {
    var position: Point
    var size: Size
}

interface XTextFrame : XTextContent {
    val text: XText
}

interface XDrawPageSupplier {
    val drawPage: XShapes
}

interface XShapes : XIndexAccess {
    fun add(shape: XShape)
    fun remove(shape: XShape)
}

data class Point(var X: Long = 0, var Y: Long = 0)
data class Size(var Width: Long = 0, var Height: Long = 0)

// ---------------------------------------------------------
// Generic Containers (Simplified)
// ---------------------------------------------------------

interface XNameAccess {
    fun getByName(name: String): Any
    fun getElementNames(): List<String>
    fun hasByName(name: String): Boolean
}

interface XNameContainer : XNameAccess {
    fun insertByName(name: String, element: Any)
    fun removeByName(name: String)
}

interface XIndexAccess {
    val count: Int
    fun getByIndex(index: Int): Any
}

interface XEnumerationAccess {
    fun createEnumeration(): XEnumeration
}

interface XEnumeration {
    fun hasMoreElements(): Boolean
    fun nextElement(): Any
}
