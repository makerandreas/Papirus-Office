package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.DocumentLayoutResult
import com.makerandreas.papirusoffice.data.OfficeBookmark
import com.makerandreas.papirusoffice.data.OfficeComment
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
    var objectVisibilities: Map<String, VisibilityState> = emptyMap(),
    var layoutPageMap: Map<Int, Int> = emptyMap(),
    /** P2-2: When true (default), Navigator prefixes follow the app locale — e.g. English app shows \"Table1\" even for an Indonesian `Judul1` document. Recognition via [NavigatorStringCatalog.headingLevelFromStyleName] stays agnostic. */
    var preferAppLocale: Boolean = true,
    /** Override tag for tests / DataStore-driven setting; null → Locale.getDefault().language */
    var appLanguageTag: String? = null
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

    fun applyLayout(layout: DocumentLayoutResult) {
        layoutPageMap = layout.elementPageIndex
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

        val rawElements = flattenDocumentElements(document)
        val locale = NavigatorStringCatalog.resolveNavigatorLocale(document, preferAppLocale, appLanguageTag)

        fun pageFor(elemIndex: Int): Int = layoutPageMap[elemIndex] ?: currentPages
        fun storedOrAuto(stored: String?, kind: NavigatorObjectKind, index: Int): String {
            val trimmed = stored?.trim().orEmpty()
            if (trimmed.isNotEmpty()) return trimmed
            return locale.autoName(kind, index)
        }

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
                            title = element.text.ifBlank { locale.untitledHeading(element.level) },
                            collapsed = isCollapsed,
                            pageIndex = pageFor(elemIndex),
                            elementIndex = elemIndex,
                            layoutNodeId = "layout_p_$paragraphCounter"
                        )
                    )
                }

                is OfficeParagraph -> {
                    paragraphCounter++
                    val pText = element.text
                    val headingLevel = resolveParagraphHeadingLevel(element)

                    if (headingLevel > 0) {
                        val id = "heading_$paragraphCounter"
                        val isCollapsed = headingFoldStates[id] ?: false
                        headingsList.add(
                            HeadingNode(
                                id = id,
                                paragraphIndex = paragraphCounter,
                                outlineLevel = headingLevel,
                                title = pText.ifBlank { locale.untitledHeading(headingLevel) },
                                collapsed = isCollapsed,
                                pageIndex = pageFor(elemIndex),
                                elementIndex = elemIndex,
                                layoutNodeId = "layout_p_$paragraphCounter"
                            )
                        )
                    }

                    if (!element.bookmark.isNullOrBlank()) {
                        val bmName = element.bookmark
                        val bmId = "bookmark_${element.bookmark}"
                        bookmarksList.add(
                            BookmarkNode(
                                id = bmId,
                                name = bmName,
                                paragraphIndex = paragraphCounter,
                                elementIndex = elemIndex,
                                pageIndex = pageFor(elemIndex)
                            )
                        )
                    }

                    element.runs.forEach { run ->
                        if (!run.hyperlink.isNullOrBlank()) {
                            val linkId = "link_${hyperlinksList.size + 1}"
                            hyperlinksList.add(
                                HyperlinkNode(
                                    id = linkId,
                                    text = run.text.ifBlank { run.hyperlink },
                                    url = run.hyperlink,
                                    elementIndex = elemIndex,
                                    pageIndex = pageFor(elemIndex)
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
                                    pageIndex = pageFor(elemIndex)
                                )
                            )
                        }
                    }
                }

                is OfficeTable -> {
                    val id = "table_$tableCounter"
                    val autoName = storedOrAuto(element.name, NavigatorObjectKind.TABLE, tableCounter)
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    tablesList.add(
                        TableNode(
                            id = id,
                            tableName = autoName,
                            rows = element.rows.size,
                            cols = element.numColumns.coerceAtLeast(if (element.rows.isNotEmpty()) element.rows[0].cells.size else 1),
                            elementIndex = elemIndex,
                            pageIndex = pageFor(elemIndex),
                            visibility = vis
                        )
                    )
                    tableCounter++
                }

                is OfficeImage -> {
                    val id = "image_$imageCounter"
                    val kind = NavigatorStringCatalog.kindOfStoredName(element.name) ?: NavigatorObjectKind.IMAGE
                    val autoName = storedOrAuto(element.name, kind, imageCounter)
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    imagesList.add(
                        ImageNode(
                            id = id,
                            imageName = autoName,
                            imagePath = element.imagePath,
                            elementIndex = elemIndex,
                            pageIndex = pageFor(elemIndex),
                            visibility = vis
                        )
                    )
                    imageCounter++
                }

                is OfficeBookmark -> {
                    val bmName = storedOrAuto(element.name, NavigatorObjectKind.BOOKMARK, bookmarkCounter)
                    val id = "bookmark_$bmName"
                    bookmarksList.add(
                        BookmarkNode(
                            id = id,
                            name = bmName,
                            paragraphIndex = paragraphCounter,
                            elementIndex = elemIndex,
                            pageIndex = pageFor(elemIndex)
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
                            pageIndex = pageFor(elemIndex)
                        )
                    )
                }

                is OfficeSection -> {
                    val id = "section_$sectionCounter"
                    val name = storedOrAuto(element.name, NavigatorObjectKind.SECTION, sectionCounter)
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    sectionsList.add(
                        SectionNode(
                            id = id,
                            sectionName = name,
                            elementIndex = elemIndex,
                            pageIndex = pageFor(elemIndex),
                            isProtected = false,
                            visibility = vis
                        )
                    )
                    sectionCounter++
                }

                is OfficeShape -> {
                    val id = "shape_$shapeCounter"
                    val name = storedOrAuto(element.name, NavigatorObjectKind.SHAPE, shapeCounter)
                    val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
                    shapesList.add(
                        ShapeNode(
                            id = id,
                            shapeName = name,
                            shapeType = element.type,
                            elementIndex = elemIndex,
                            pageIndex = pageFor(elemIndex),
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
                            pageIndex = pageFor(elemIndex)
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
                            pageIndex = pageFor(elemIndex)
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
                            pageIndex = pageFor(elemIndex)
                        )
                    )
                }

                else -> {
                    // Other elements
                }
            }
        }

        document.resources.objects.forEachIndexed { idx, objName ->
            val id = "ole_${idx + 1}"
            val vis = objectVisibilities[id] ?: VisibilityState.VISIBLE
            val oleKind = NavigatorStringCatalog.kindOfStoredName(objName) ?: NavigatorObjectKind.OBJECT
            oleList.add(
                OleNode(
                    id = id,
                    oleName = storedOrAuto(objName, oleKind, idx + 1),
                    elementIndex = 0,
                    pageIndex = 1,
                    visibility = vis
                )
            )
        }

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
     * Resolves outline level from [OfficeParagraph.outlineLevel] or the parent
     * style chain (ODF `P*` → `JudulN`, DOCX `w:basedOn` / `w:outlineLvl`).
     */
    private fun resolveParagraphHeadingLevel(element: OfficeParagraph): Int {
        if (element.outlineLevel > 0) return element.outlineLevel.coerceIn(1, 6)
        var curr: String? = element.styleName
        var depth = 0
        while (!curr.isNullOrBlank() && depth < 10) {
            val fromName = headingLevelFromStyleName(curr)
            if (fromName > 0) return fromName
            val style = document.styles.paragraphStyles[curr]
                ?: document.styles.paragraphStyles[curr.lowercase()]
            if (style != null) {
                val fromStyleName = headingLevelFromStyleName(style.name)
                if (fromStyleName > 0) return fromStyleName
                val parent = style.parentStyleName
                if (!parent.isNullOrBlank()) {
                    val fromParent = headingLevelFromStyleName(parent)
                    if (fromParent > 0) return fromParent
                    curr = parent
                    depth++
                    continue
                }
            }
            break
        }
        return 0
    }

    private fun headingLevelFromStyleName(sName: String): Int {
        return NavigatorStringCatalog.headingLevelFromStyleName(sName)
    }

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
        doc.sections.forEach { section ->
            result.add(OfficeSection(name = section.name))
            section.elements.forEach { elem ->
                result.add(elem)
            }
        }
        doc.body.elements.forEach { elem ->
            result.add(elem)
        }
        return result
    }

    override fun getBookmarks(): List<BookmarkNode> = currentIndex.bookmarks
    override fun getTextTables(): List<TableNode> = currentIndex.tables
    override fun getGraphicObjects(): List<ImageNode> = currentIndex.images
    override fun getTextFrames(): List<FrameNode> = currentIndex.frames
    override fun getTextFields(): List<FieldNode> = currentIndex.fields
    override fun getTextSections(): List<SectionNode> = currentIndex.sections
    override fun getFootnotes(): List<FootnoteNode> = currentIndex.footnotes
    override fun getDocumentIndex(): DocumentIndex = currentIndex
}
