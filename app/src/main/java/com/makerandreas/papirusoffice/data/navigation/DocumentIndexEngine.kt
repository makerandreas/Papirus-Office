package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.OfficeBookmark
import com.makerandreas.papirusoffice.data.OfficeComment
import com.makerandreas.papirusoffice.data.OfficeDocElement
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeElement
import com.makerandreas.papirusoffice.data.OfficeField
import com.makerandreas.papirusoffice.data.OfficeFootnoteElement
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeHyperlink
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeSection
import com.makerandreas.papirusoffice.data.OfficeShape
import com.makerandreas.papirusoffice.data.OfficeTable

/**
 * Core engine responsible for scanning and indexing an [OfficeDocument].
 * Implements LibreOffice UNO-inspired Supplier interfaces.
 */
class DocumentIndexEngine(
    var document: OfficeDocument = OfficeDocument(),
    var headingFoldStates: Map<String, Boolean> = emptyMap(),
    var objectVisibilities: Map<String, VisibilityState> = emptyMap()
) : XBookmarksSupplier,
    XTextTablesSupplier,
    XTextGraphicObjectsSupplier,
    XTextFramesSupplier,
    XTextFieldsSupplier,
    XTextSectionsSupplier,
    XFootnotesSupplier,
    XDocumentIndexesSupplier {

    private var currentIndex: DocumentIndex = DocumentIndex()

    init {
        reindex()
    }

    /**
     * Fully rescans [document] and builds a updated [DocumentIndex].
     */
    fun reindex(): DocumentIndex {
        val headingsList = mutableListOf<HeadingNode>()
        val tablesList = mutableListOf<TableNode>()
        val imagesList = mutableListOf<ImageNode>()
        val bookmarksList = mutableListOf<BookmarkNode>()
        val commentsList = mutableListOf<CommentNode>()
        val sectionsList = mutableListOf<SectionNode>()
        val framesList = mutableListOf<FrameNode>()
        val fieldsList = mutableListOf<FieldNode>()
        val footnotesList = mutableListOf<FootnoteNode>()
        val hyperlinksList = mutableListOf<HyperlinkNode>()
        val shapesList = mutableListOf<ShapeNode>()
        val oleList = mutableListOf<OleNode>()
        val remindersList = mutableListOf<ReminderNode>()

        var currentPages = 1
        var paragraphCounter = 0
        var tableCounter = 1
        var imageCounter = 1
        var frameCounter = 1
        var sectionCounter = 1
        var bookmarkCounter = 1
        var shapeCounter = 1
        var oleCounter = 1

        val rawElements = flattenDocumentElements(document)

        rawElements.forEachIndexed { elemIndex, element ->
            when (element) {
                is OfficePageBreak -> {
                    currentPages++
                }

                is OfficeHeading -> {
                    paragraphCounter++
                    val id = "heading_$paragraphCounter"
                    val isCollapsed = headingFoldStates[id] ?: false
                    headingsList.add(
                        HeadingNode(
                            id = id,
                            paragraphIndex = paragraphCounter,
                            outlineLevel = element.level,
                            title = element.text.ifBlank { "Heading ${element.level}" },
                            collapsed = isCollapsed,
                            pageIndex = currentPages,
                            layoutNodeId = "layout_p_$paragraphCounter"
                        )
                    )
                }

                is OfficeParagraph -> {
                    paragraphCounter++
                    val pText = element.text
                    val isHeadingStyle = element.styleName?.contains("Heading", ignoreCase = true) == true
                    val headingLevel = if (element.outlineLevel > 0) {
                        element.outlineLevel
                    } else if (isHeadingStyle) {
                        when {
                            element.styleName.contains("Heading 1", ignoreCase = true) -> 1
                            element.styleName.contains("Heading 2", ignoreCase = true) -> 2
                            element.styleName.contains("Heading 3", ignoreCase = true) -> 3
                            element.styleName.contains("Heading 4", ignoreCase = true) -> 4
                            else -> 1
                        }
                    } else 0

                    if (headingLevel > 0) {
                        val id = "heading_$paragraphCounter"
                        val isCollapsed = headingFoldStates[id] ?: false
                        headingsList.add(
                            HeadingNode(
                                id = id,
                                paragraphIndex = paragraphCounter,
                                outlineLevel = headingLevel,
                                title = pText.ifBlank { "Heading $headingLevel" },
                                collapsed = isCollapsed,
                                pageIndex = currentPages,
                                layoutNodeId = "layout_p_$paragraphCounter"
                            )
                        )
                    }

                    // Check for inline bookmark in paragraph
                    if (!element.bookmark.isNullOrBlank()) {
                        val bmName = element.bookmark
                        val bmId = "bookmark_${element.bookmark}"
                        bookmarksList.add(
                            BookmarkNode(
                                id = bmId,
                                name = bmName,
                                paragraphIndex = paragraphCounter,
                                elementIndex = elemIndex,
                                pageIndex = currentPages
                            )
                        )
                    }

                    // Check for runs with hyperlinks, fields, etc.
                    element.runs.forEach { run ->
                        if (!run.hyperlink.isNullOrBlank()) {
                            val linkId = "link_${hyperlinksList.size + 1}"
                            hyperlinksList.add(
                                HyperlinkNode(
                                    id = linkId,
                                    text = run.text.ifBlank { run.hyperlink },
                                    url = run.hyperlink,
                                    elementIndex = elemIndex,
                                    pageIndex = currentPages
                                )
                            )
                        }
                        if (!run.field.isNullOrBlank()) {
                            val fieldId = "field_${fieldsList.size + 1}"
                            fieldsList.add(
                                FieldNode(
                                    id = fieldId,
                                    fieldType = "TextRunField",
                                    value = run.field,
                                    elementIndex = elemIndex,
                                    pageIndex = currentPages
                                )
                            )
                        }
                    }
                }

                is OfficeTable -> {
                    val id = "table_$tableCounter"
                    val autoName = "Table$tableCounter"
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    tablesList.add(
                        TableNode(
                            id = id,
                            tableName = autoName,
                            rows = element.rows.size,
                            cols = element.numColumns.coerceAtLeast(if (element.rows.isNotEmpty()) element.rows[0].cells.size else 1),
                            elementIndex = elemIndex,
                            pageIndex = currentPages,
                            visibility = vis
                        )
                    )
                    tableCounter++
                }

                is OfficeImage -> {
                    val id = "image_$imageCounter"
                    val autoName = "Image$imageCounter"
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    imagesList.add(
                        ImageNode(
                            id = id,
                            imageName = autoName,
                            imagePath = element.imagePath,
                            elementIndex = elemIndex,
                            pageIndex = currentPages,
                            visibility = vis
                        )
                    )
                    imageCounter++
                }

                is OfficeBookmark -> {
                    val id = "bookmark_${element.name.ifBlank { "Bookmark$bookmarkCounter" }}"
                    val bmName = element.name.ifBlank { "Bookmark$bookmarkCounter" }
                    bookmarksList.add(
                        BookmarkNode(
                            id = id,
                            name = bmName,
                            paragraphIndex = paragraphCounter,
                            elementIndex = elemIndex,
                            pageIndex = currentPages
                        )
                    )
                    bookmarkCounter++
                }

                is OfficeComment -> {
                    val id = "comment_${commentsList.size + 1}"
                    commentsList.add(
                        CommentNode(
                            id = id,
                            author = element.author.ifBlank { "Author" },
                            content = element.text,
                            date = element.date,
                            elementIndex = elemIndex,
                            pageIndex = currentPages
                        )
                    )
                }

                is OfficeSection -> {
                    val id = "section_$sectionCounter"
                    val name = element.name.ifBlank { "Section$sectionCounter" }
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    sectionsList.add(
                        SectionNode(
                            id = id,
                            sectionName = name,
                            elementIndex = elemIndex,
                            pageIndex = currentPages,
                            isProtected = false,
                            visibility = vis
                        )
                    )
                    sectionCounter++
                }

                is OfficeShape -> {
                    val id = "shape_$shapeCounter"
                    val name = "Shape$shapeCounter"
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    shapesList.add(
                        ShapeNode(
                            id = id,
                            shapeName = name,
                            shapeType = element.type,
                            elementIndex = elemIndex,
                            pageIndex = currentPages,
                            visibility = vis
                        )
                    )
                    shapeCounter++
                }

                is OfficeField -> {
                    val id = "field_${fieldsList.size + 1}"
                    fieldsList.add(
                        FieldNode(
                            id = id,
                            fieldType = element.type,
                            value = element.value,
                            elementIndex = elemIndex,
                            pageIndex = currentPages
                        )
                    )
                }

                is OfficeFootnoteElement -> {
                    val id = "footnote_${footnotesList.size + 1}"
                    footnotesList.add(
                        FootnoteNode(
                            id = id,
                            label = element.noteId.ifBlank { "${footnotesList.size + 1}" },
                            text = element.text,
                            elementIndex = elemIndex,
                            pageIndex = currentPages
                        )
                    )
                }

                is OfficeHyperlink -> {
                    val id = "link_${hyperlinksList.size + 1}"
                    hyperlinksList.add(
                        HyperlinkNode(
                            id = id,
                            text = element.text,
                            url = element.targetUri,
                            elementIndex = elemIndex,
                            pageIndex = currentPages
                        )
                    )
                }

                else -> {
                    // Other elements
                }
            }
        }

        // Process embedded resources (OLE Objects, Charts, Media, Extra Images)
        document.resources.objects.forEachIndexed { idx, objName ->
            val id = "ole_${idx + 1}"
            val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
            oleList.add(
                OleNode(
                    id = id,
                    oleName = if (objName.isNotBlank()) objName else "Object${idx + 1}",
                    elementIndex = 0,
                    pageIndex = 1,
                    visibility = vis
                )
            )
        }

        // Build hierarchical Heading tree structure with children
        val hierarchicalHeadings = buildHeadingTree(headingsList)

        currentIndex = DocumentIndex(
            headings = hierarchicalHeadings,
            tables = tablesList,
            images = imagesList,
            bookmarks = bookmarksList,
            comments = commentsList,
            sections = sectionsList,
            frames = framesList,
            fields = fieldsList,
            footnotes = footnotesList,
            hyperlinks = hyperlinksList,
            shapes = shapesList,
            oleObjects = oleList,
            reminders = remindersList
        )

        return currentIndex
    }

    /**
     * Builds hierarchical parent-child heading tree structure based on outline levels.
     */
    private fun buildHeadingTree(flatHeadings: List<HeadingNode>): List<HeadingNode> {
        if (flatHeadings.isEmpty()) return emptyList()

        val rootBuilders = mutableListOf<HeadingNodeBuilder>()
        val stack = mutableListOf<HeadingNodeBuilder>()

        for (node in flatHeadings) {
            val builder = HeadingNodeBuilder(node)

            while (stack.isNotEmpty() && stack.last().node.outlineLevel >= node.outlineLevel) {
                stack.removeAt(stack.size - 1)
            }

            if (stack.isEmpty()) {
                rootBuilders.add(builder)
            } else {
                stack.last().childrenBuilders.add(builder)
            }

            stack.add(builder)
        }

        return rootBuilders.map { it.toNode() }
    }

    private class HeadingNodeBuilder(val node: HeadingNode) {
        val childrenBuilders = mutableListOf<HeadingNodeBuilder>()

        fun toNode(): HeadingNode {
            return node.copy(
                children = childrenBuilders.map { it.toNode() }
            )
        }
    }

    private fun flattenDocumentElements(doc: OfficeDocument): List<OfficeElement> {
        val result = mutableListOf<OfficeElement>()

        // 1. Extract elements from sections
        doc.sections.forEach { section ->
            result.add(OfficeSection(name = section.name))
            section.elements.forEach { elem ->
                result.add(unwrapDocElement(elem))
            }
        }

        // 2. Extract elements from main body
        doc.body.elements.forEach { elem ->
            result.add(unwrapDocElement(elem))
        }

        return result
    }

    private fun unwrapDocElement(element: OfficeElement): OfficeElement {
        return if (element is OfficeDocElement) {
            when (element) {
                is OfficeDocElement.ParagraphElement -> element.paragraph
                is OfficeDocElement.TableElement -> element.table
                is OfficeDocElement.ImageElement -> element.image
                is OfficeDocElement.ShapeElement -> element.shape
                is OfficeDocElement.BookmarkElement -> element.bookmark
                is OfficeDocElement.HyperlinkElement -> element.hyperlink
                is OfficeDocElement.FieldElement -> element.field
            }
        } else {
            element
        }
    }

    // UNO SUPPLIERS IMPLEMENTATIONS
    override fun getBookmarks(): List<BookmarkNode> = currentIndex.bookmarks
    override fun getTextTables(): List<TableNode> = currentIndex.tables
    override fun getGraphicObjects(): List<ImageNode> = currentIndex.images
    override fun getTextFrames(): List<FrameNode> = currentIndex.frames
    override fun getTextFields(): List<FieldNode> = currentIndex.fields
    override fun getTextSections(): List<SectionNode> = currentIndex.sections
    override fun getFootnotes(): List<FootnoteNode> = currentIndex.footnotes
    override fun getDocumentIndex(): DocumentIndex = currentIndex
}
