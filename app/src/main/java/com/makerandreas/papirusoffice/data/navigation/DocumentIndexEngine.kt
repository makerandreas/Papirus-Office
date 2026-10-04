package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.BodyElementRange
import com.makerandreas.papirusoffice.data.DocumentLayoutResult
import com.makerandreas.papirusoffice.data.DocumentRanges
import com.makerandreas.papirusoffice.data.SectionDisplay
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
        val seenBookmarkNames = LinkedHashSet<String>()

        val rawElements = flattenDocumentElements(document)
        val locale = NavigatorStringCatalog.resolveNavigatorLocale(document, preferAppLocale, appLanguageTag)

        // Page by page-break count, so ranges resolved after the body loop get
        // the same fallback page as elements visited inside it.
        val breakPages = IntArray(rawElements.size)
        run {
            var page = 1
            rawElements.forEachIndexed { i, el ->
                if (el is OfficePageBreak) page++
                breakPages[i] = page
            }
        }
        fun pageFor(elemIndex: Int): Int =
            layoutPageMap[elemIndex] ?: breakPages.getOrNull(elemIndex) ?: currentPages

        // Plan 7C: paragraphs inside an authored index are index content, never
        // headings (audit-015 F-1), and TOC entry links are grouped under their
        // index rather than listed as hyperlinks (owner decision 3).
        val sidecarsFit = document.authoredIndexes.all { it.bodyRange.endExclusive <= rawElements.size }
        val indexRanges = if (sidecarsFit) document.authoredIndexes else emptyList()
        val inIndex = BooleanArray(rawElements.size)
        val linkedEntryElements = HashSet<Int>()
        indexRanges.forEach { range ->
            for (i in range.bodyRange.startInclusive until range.bodyRange.endExclusive) inIndex[i] = true
            range.entries.forEach { if (it.targetAnchor != null) linkedEntryElements.add(it.elementIndex) }
        }
        fun storedOrAuto(stored: String?, kind: NavigatorObjectKind, index: Int): String {
            val trimmed = stored?.trim().orEmpty()
            if (trimmed.isNotEmpty()) return trimmed
            return locale.autoName(kind, index)
        }
        fun registerBookmark(rawName: String?, pIndex: Int, elIndex: Int) {
            val clean = rawName?.trim().orEmpty()
            if (clean.isEmpty() || !seenBookmarkNames.add(clean)) return
            bookmarksList.add(
                BookmarkNode(
                    id = "bookmark_$clean",
                    name = clean,
                    paragraphIndex = pIndex,
                    elementIndex = elIndex,
                    pageIndex = pageFor(elIndex)
                )
            )
            bookmarkCounter++
        }
        fun indexRuns(runs: List<com.makerandreas.papirusoffice.data.OfficeTextRun>, elIndex: Int) {
            runs.forEach { run ->
                val groupedUnderIndex = elIndex in linkedEntryElements && run.hyperlink?.startsWith("#") == true
                if (!run.hyperlink.isNullOrBlank() && !groupedUnderIndex) {
                    val linkId = "link_${hyperlinksList.size + 1}"
                    hyperlinksList.add(
                        HyperlinkNode(
                            id = linkId,
                            text = run.text.ifBlank { run.hyperlink },
                            url = run.hyperlink,
                            elementIndex = elIndex,
                            pageIndex = pageFor(elIndex)
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
                            elementIndex = elIndex,
                            pageIndex = pageFor(elIndex)
                        )
                    )
                }
            }
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
                    element.bookmarks.forEach { bm ->
                        registerBookmark(bm.name, paragraphCounter, elemIndex)
                    }
                    indexRuns(element.runs, elemIndex)
                }

                is OfficeParagraph -> {
                    paragraphCounter++
                    val pText = element.text
                    val headingLevel = if (inIndex.getOrElse(elemIndex) { false }) 0 else resolveParagraphHeadingLevel(element)

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
                        registerBookmark(element.bookmark, paragraphCounter, elemIndex)
                    }
                    element.bookmarks.forEach { bm ->
                        registerBookmark(bm.name, paragraphCounter, elemIndex)
                    }

                    indexRuns(element.runs, elemIndex)
                }

                is com.makerandreas.papirusoffice.data.OfficeListItem -> {
                    paragraphCounter++
                    element.bookmarks.forEach { bm ->
                        registerBookmark(bm.name, paragraphCounter, elemIndex)
                    }
                    indexRuns(element.runs, elemIndex)
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
                    registerBookmark(bmName, paragraphCounter, elemIndex)
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

        document.bookmarks.forEach { bm ->
            registerBookmark(bm.name, paragraphCounter.coerceAtLeast(0), 0)
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

        val authoredIndexNodes = indexRanges.mapNotNull { range ->
            val jump = DocumentRanges.firstNavigableIndex(range.bodyRange, rawElements) ?: return@mapNotNull null
            IndexNode(
                id = "authored_${range.id}",
                name = range.name,
                kind = range.kind,
                elementIndex = jump,
                pageIndex = pageFor(jump),
                isProtected = range.isProtected,
                entries = range.entries.mapIndexed { entryIndex, entry ->
                    IndexEntryNode(
                        id = "authored_${range.id}_entry_${entryIndex + 1}",
                        text = entry.text,
                        level = entry.level,
                        pageLabel = entry.displayedPageLabel,
                        targetAnchor = entry.targetAnchor,
                        elementIndex = entry.elementIndex,
                        pageIndex = pageFor(entry.elementIndex),
                        targetBookmarkId = entry.targetAnchor?.let { anchor ->
                            bookmarksList.firstOrNull { it.name == anchor }?.id
                        }
                    )
                },
                rangeStart = range.bodyRange.startInclusive,
                rangeEnd = range.bodyRange.endExclusive
            )
        }

        val sectionRanges = document.namedSectionRanges.filter { it.bodyRange.endExclusive <= rawElements.size }
        val hiddenRanges: List<BodyElementRange> = sectionRanges
            .filter { it.display == SectionDisplay.HIDDEN }
            .map { it.bodyRange }
        val rangeSectionNodes = sectionRanges.mapNotNull { range ->
            val id = "section_range_${range.id}"
            val hidden = range.display == SectionDisplay.HIDDEN ||
                objectVisibilities[id] == VisibilityState.HIDDEN
            val jump = (if (hidden) DocumentRanges.nearestVisible(range.bodyRange, hiddenRanges, rawElements.size) else null)
                ?: DocumentRanges.firstNavigableIndex(range.bodyRange, rawElements)
                ?: return@mapNotNull null
            SectionNode(
                id = id,
                sectionName = range.name,
                elementIndex = jump,
                pageIndex = pageFor(jump),
                isProtected = range.isProtected,
                visibility = if (hidden) VisibilityState.HIDDEN else VisibilityState.VISIBLE,
                parentId = range.parentId?.let { "section_range_$it" },
                depth = range.depth,
                rangeStart = range.bodyRange.startInclusive,
                rangeEnd = range.bodyRange.endExclusive,
                isConditional = range.display == SectionDisplay.CONDITIONAL
            )
        }
        val allSections = (sectionsList + rangeSectionNodes)
            .withIndex()
            .sortedWith(compareBy({ it.value.rangeStart }, { it.value.depth }, { it.index }))
            .map { it.value }

        val hierarchicalHeadings = buildHeadingTree(headingsList)

        currentIndex = DocumentIndex(
            headings = hierarchicalHeadings,
            tables = tablesList,
            images = imagesList,
            bookmarks = bookmarksList,
            comments = commentsList,
            sections = allSections,
            frames = framesList,
            fields = fieldsList,
            footnotes = footnotesList,
            hyperlinks = hyperlinksList,
            shapes = shapesList,
            oleObjects = oleList,
            reminders = remindersList,
            authoredIndexes = authoredIndexNodes,
            sourceFormat = document.parserReport.format
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

    /**
     * Body elements in order. Indices must equal body positions: layout page
     * maps, caret windows and the Plan 7C range sidecars all address the body
     * directly, so nothing may be prepended here (audit-015 F-7).
     */
    private fun flattenDocumentElements(doc: OfficeDocument): List<OfficeElement> = doc.body.elements

    override fun getBookmarks(): List<BookmarkNode> = currentIndex.bookmarks
    override fun getTextTables(): List<TableNode> = currentIndex.tables
    override fun getGraphicObjects(): List<ImageNode> = currentIndex.images
    override fun getTextFrames(): List<FrameNode> = currentIndex.frames
    override fun getTextFields(): List<FieldNode> = currentIndex.fields
    override fun getTextSections(): List<SectionNode> = currentIndex.sections
    override fun getFootnotes(): List<FootnoteNode> = currentIndex.footnotes
    override fun getDocumentIndex(): DocumentIndex = currentIndex
}
