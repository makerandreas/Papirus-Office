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

object ReferenceFieldSource {
    const val REFERENCE_MARK: Short = 0
    const val SEQUENCE_FIELD: Short = 1
    const val BOOKMARK: Short = 2
    const val FOOTNOTE: Short = 3
    const val ENDNOTE: Short = 4
}

object ReferenceFieldPart {
    const val PAGE: Short = 0
    const val CHAPTER: Short = 1
    const val TEXT: Short = 2
    const val UP_DOWN: Short = 3
    const val PAGE_DESC: Short = 4
    const val CATEGORY_AND_NUMBER: Short = 5
    const val ONLY_NUMBER: Short = 6
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

interface XEmbeddedObjectSupplier2 {
    val embeddedObject: XComponent
}

interface XTextFieldsSupplier {
    val textFields: Any // Represents an XEnumerationAccess
    val textFieldMasters: Any // Represents an XNameAccess
}

// ---------------------------------------------------------
// Embedded Object CLSIDs
// ---------------------------------------------------------

object OfficeDocumentCLSIDs {
    const val WRITER = "8BC6B165-B1B2-4EDD-aa47-dae2ee689dd6"
    const val CALC = "47BBB4CB-CE4C-4E80-a591-42d9ae74950f"
    const val DRAW = "4BAB8970-8A3B-45B3-991c-cbeeac6bd5e3"
    const val IMPRESS = "9176E48A-637A-4D1F-803b-99d9bfac1047"
    const val MATH = "078B7ABA-54FC-457F-8551-6147e776a997"
    const val CHART = "12DCAE26-281F-416F-a234-c3086127382e"
}

// ---------------------------------------------------------
// Formatting Constants
// ---------------------------------------------------------

object HoriOrientation {
    const val NONE: Short = 0
    const val RIGHT: Short = 1
    const val CENTER: Short = 2
    const val LEFT: Short = 3
    const val INSIDE: Short = 4
    const val OUTSIDE: Short = 5
    const val FULL: Short = 6
    const val LEFT_AND_WIDTH: Short = 7
}

object VertOrientation {
    const val NONE: Short = 0
    const val TOP: Short = 1
    const val CENTER: Short = 2
    const val BOTTOM: Short = 3
    const val CHAR_TOP: Short = 4
    const val CHAR_CENTER: Short = 5
    const val CHAR_BOTTOM: Short = 6
    const val LINE_TOP: Short = 7
    const val LINE_CENTER: Short = 8
    const val LINE_BOTTOM: Short = 9
}

object PageNumberType {
    const val PREV: Short = 0
    const val CURRENT: Short = 1
    const val NEXT: Short = 2
}

data class BorderLine(
    var Color: Int = 0,
    var InnerLineWidth: Short = 0,
    var OuterLineWidth: Short = 0,
    var LineDistance: Short = 0
)

// ---------------------------------------------------------
// Text Sections
// ---------------------------------------------------------

interface XTextSection : XTextContent

interface XTextSectionsSupplier {
    val textSections: Any // Represents an XNameAccess
}

interface XTextShapesSupplier {
    val shapes: XIndexAccess
}

object TextContentAnchorType {
    const val AT_PARAGRAPH: Short = 0
    const val AS_CHARACTER: Short = 1
    const val AT_PAGE: Short = 2
    const val AT_FRAME: Short = 3
    const val AT_CHARACTER: Short = 4
}

// ---------------------------------------------------------
// Drawing Shapes & Frames
// ---------------------------------------------------------

interface XShapeDescriptor {
    val shapeType: String
}

interface XShape : XShapeDescriptor {
    var position: Point
    var size: Size
}

interface XTextFrame : XTextContent {
    val text: XText
}

interface XDrawPageSupplier {
    val drawPage: Any // Represents XDrawPage
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
