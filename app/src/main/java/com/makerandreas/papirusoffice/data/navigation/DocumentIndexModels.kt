package com.makerandreas.papirusoffice.data.navigation

/**
 * Visibility state for elements in Document Index and Navigator.
 */
enum class VisibilityState {
    VISIBLE,
    HIDDEN
}

/**
 * Node pointing to a Heading in OfficeDocument.
 */
data class HeadingNode(
    val id: String,
    val paragraphIndex: Int,
    val outlineLevel: Int, // 1 for Heading 1, 2 for Heading 2, etc.
    val title: String,
    val collapsed: Boolean = false,
    val pageIndex: Int = 1,
    val layoutNodeId: String = "heading_$paragraphIndex",
    val children: List<HeadingNode> = emptyList()
)

/**
 * Node pointing to a Table in OfficeDocument.
 */
data class TableNode(
    val id: String,
    val tableName: String, // e.g. "Table1"
    val rows: Int,
    val cols: Int,
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to an Image / Graphic object in OfficeDocument.
 */
data class ImageNode(
    val id: String,
    val imageName: String, // e.g. "Image1"
    val imagePath: String,
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to a Bookmark in OfficeDocument.
 */
data class BookmarkNode(
    val id: String,
    val name: String, // e.g. "Bookmark1"
    val paragraphIndex: Int,
    val elementIndex: Int,
    val pageIndex: Int = 1
)

/**
 * Node pointing to a Comment / Annotation in OfficeDocument.
 */
data class CommentNode(
    val id: String,
    val author: String,
    val content: String,
    val date: String,
    val elementIndex: Int,
    val pageIndex: Int = 1
)

/**
 * Node pointing to a Section in OfficeDocument.
 */
data class SectionNode(
    val id: String,
    val sectionName: String, // e.g. "Section1"
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val isProtected: Boolean = false,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to a Text Frame in OfficeDocument.
 */
data class FrameNode(
    val id: String,
    val frameName: String, // e.g. "Frame1"
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to a Document Field in OfficeDocument.
 */
data class FieldNode(
    val id: String,
    val fieldType: String, // e.g. "PageNumber", "Date", "Author"
    val value: String,
    val elementIndex: Int,
    val pageIndex: Int = 1
)

/**
 * Node pointing to a Footnote / Endnote in OfficeDocument.
 */
data class FootnoteNode(
    val id: String,
    val label: String,
    val text: String,
    val elementIndex: Int,
    val pageIndex: Int = 1
)

/**
 * Node pointing to a Hyperlink in OfficeDocument.
 */
data class HyperlinkNode(
    val id: String,
    val text: String,
    val url: String,
    val elementIndex: Int,
    val pageIndex: Int = 1
)

/**
 * Node pointing to a Drawing Shape in OfficeDocument.
 */
data class ShapeNode(
    val id: String,
    val shapeName: String, // e.g. "Shape1"
    val shapeType: String,
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to an OLE Object in OfficeDocument.
 */
data class OleNode(
    val id: String,
    val oleName: String, // e.g. "Object1"
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val visibility: VisibilityState = VisibilityState.VISIBLE
)

/**
 * Node pointing to a Reminder / Marker in OfficeDocument.
 */
data class ReminderNode(
    val id: String,
    val paragraphIndex: Int,
    val elementIndex: Int,
    val pageIndex: Int = 1,
    val note: String = "Reminder"
)

/**
 * Complete Indexed Document Object Tree.
 */
data class DocumentIndex(
    val headings: List<HeadingNode> = emptyList(),
    val tables: List<TableNode> = emptyList(),
    val images: List<ImageNode> = emptyList(),
    val bookmarks: List<BookmarkNode> = emptyList(),
    val comments: List<CommentNode> = emptyList(),
    val sections: List<SectionNode> = emptyList(),
    val frames: List<FrameNode> = emptyList(),
    val fields: List<FieldNode> = emptyList(),
    val footnotes: List<FootnoteNode> = emptyList(),
    val hyperlinks: List<HyperlinkNode> = emptyList(),
    val shapes: List<ShapeNode> = emptyList(),
    val oleObjects: List<OleNode> = emptyList(),
    val reminders: List<ReminderNode> = emptyList()
) {
    fun isEmpty(): Boolean =
        headings.isEmpty() && tables.isEmpty() && images.isEmpty() &&
                bookmarks.isEmpty() && comments.isEmpty() && sections.isEmpty() &&
                frames.isEmpty() && fields.isEmpty() && footnotes.isEmpty() &&
                hyperlinks.isEmpty() && shapes.isEmpty() && oleObjects.isEmpty() &&
                reminders.isEmpty()

    fun totalItemsCount(): Int =
        headings.size + tables.size + images.size + bookmarks.size +
                comments.size + sections.size + frames.size + fields.size +
                footnotes.size + hyperlinks.size + shapes.size + oleObjects.size +
                reminders.size
}

// ==========================================
// LIBREOFFICE UNO SUPPLIER INTERFACES (CONCEPT)
// ==========================================

interface XBookmarksSupplier {
    fun getBookmarks(): List<BookmarkNode>
}

interface XTextTablesSupplier {
    fun getTextTables(): List<TableNode>
}

interface XTextGraphicObjectsSupplier {
    fun getGraphicObjects(): List<ImageNode>
}

interface XTextFramesSupplier {
    fun getTextFrames(): List<FrameNode>
}

interface XTextFieldsSupplier {
    fun getTextFields(): List<FieldNode>
}

interface XTextSectionsSupplier {
    fun getTextSections(): List<SectionNode>
}

interface XFootnotesSupplier {
    fun getFootnotes(): List<FootnoteNode>
}

interface XDocumentIndexesSupplier {
    fun getDocumentIndex(): DocumentIndex
}
