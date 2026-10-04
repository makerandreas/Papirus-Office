package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.DocumentLayoutResult
import com.makerandreas.papirusoffice.data.OfficeDocument
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dropdown / Category selector for Navigation mode in Papirus Writer.
 */
enum class NavigateBy {
    ALL,
    PAGE,
    HEADING,
    TABLE,
    IMAGE,
    COMMENT,
    BOOKMARK,
    FRAME,
    SECTION,
    FIELD,
    FOOTNOTE,
    INDEX,
    OLE,
    DRAWING,
    SHAPE,
    REMINDER,
    SELECTION,
    RECENT
}

/**
 * Navigation Target Signal emitted when jumping to a document target.
 * Read by CaretEngine, SelectionEngine, PaginationEngine, and Viewport (LazyColumn).
 */
data class NavTargetSignal(
    val targetType: NavigateBy,
    val targetId: String,
    val targetPageIndex: Int,
    val targetElementIndex: Int,
    val targetParagraphIndex: Int = 0,
    val titleOrLabel: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Typed Navigator notices. The engine has no Android context, so the sheet
 * maps each value to an en_US string resource (audit-015 F-11) instead of the
 * engine carrying English literals.
 */
enum class NavigatorNotice {
    /** The target is hidden and the jump was refused. */
    HIDDEN_ITEM,
    /** The target section is hidden; the jump landed on the nearest visible position. */
    HIDDEN_SECTION_NEAREST_VISIBLE,
    /** Previous/next has nothing to step through in the current Navigate By mode. */
    NO_ITEMS_IN_MODE,
    /** The current category has no items to step through. */
    NO_ITEMS_IN_CATEGORY
}

/**
 * Reactive State representing the current Navigator under-the-hood engine state.
 */
data class NavigatorState(
    val index: DocumentIndex = DocumentIndex(),
    val navigateBy: NavigateBy = NavigateBy.ALL,
    val activeHeadingId: String? = null,
    val activeItemId: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val notice: NavigatorNotice? = null,
    val navTargetSignal: NavTargetSignal? = null,
    val headingFoldStates: Map<String, Boolean> = emptyMap(),
    val objectVisibilities: Map<String, VisibilityState> = emptyMap()
)

/**
 * Core Navigation Engine managing document navigation, jumping, heading folding,
 * hidden object checks, and next/previous iteration.
 */
class NavigationEngine(
    initialDocument: OfficeDocument = OfficeDocument(),
    preferAppLocale: Boolean = true,
    appLanguageTag: String? = null
) {
    private val foldStatesMap = mutableMapOf<String, Boolean>()
    private val objectVisibilityMap = mutableMapOf<String, VisibilityState>()

    private val indexEngine = DocumentIndexEngine(
        document = initialDocument,
        headingFoldStates = foldStatesMap,
        objectVisibilities = objectVisibilityMap,
        preferAppLocale = preferAppLocale,
        appLanguageTag = appLanguageTag
    )

    private val _state = MutableStateFlow(
        NavigatorState(
            index = indexEngine.getDocumentIndex(),
            headingFoldStates = foldStatesMap,
            objectVisibilities = objectVisibilityMap
        )
    )
    val state: StateFlow<NavigatorState> = _state.asStateFlow()

    init {
        val initialIdx = indexEngine.getDocumentIndex()
        val pagesCount = calculateTotalPages(initialIdx)
        _state.value = _state.value.copy(
            index = initialIdx,
            totalPages = pagesCount
        )
    }

    /**
     * Updates the active document model and re-indexes all elements.
     */
    fun updateDocument(doc: OfficeDocument, layout: DocumentLayoutResult? = null) {
        indexEngine.document = doc
        if (layout != null) {
            indexEngine.layoutPageMap = layout.elementPageIndex
        }
        val updatedIndex = indexEngine.reindex()
        val pagesCount = if (layout != null && layout.pages.isNotEmpty()) {
            layout.pages.size
        } else {
            calculateTotalPages(updatedIndex)
        }

        _state.value = _state.value.copy(
            index = updatedIndex,
            totalPages = pagesCount,
            currentPage = _state.value.currentPage.coerceIn(1, pagesCount.coerceAtLeast(1))
        )
    }

    /**
     * Sets current NavigateBy mode (e.g. Heading, Table, Bookmark, Image, Page).
     */
    fun setNavigateBy(mode: NavigateBy) {
        _state.value = _state.value.copy(navigateBy = mode)
    }

    // ==========================================
    // JUMP METHODS
    // ==========================================

    fun goToHeading(id: String) {
        val flatHeadings = flattenHeadings(_state.value.index.headings)
        val heading = flatHeadings.find { it.id == id } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.HEADING,
                targetId = heading.id,
                targetPageIndex = heading.pageIndex,
                targetElementIndex = heading.elementIndex,
                targetParagraphIndex = heading.paragraphIndex,
                titleOrLabel = heading.title
            ),
            activeHeadingId = heading.id,
            activeItemId = heading.id
        )
    }

    fun goToBookmark(idOrName: String) {
        val bm = _state.value.index.bookmarks.find { it.id == idOrName || it.name == idOrName } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.BOOKMARK,
                targetId = bm.id,
                targetPageIndex = bm.pageIndex,
                targetElementIndex = bm.elementIndex,
                targetParagraphIndex = bm.paragraphIndex,
                titleOrLabel = bm.name
            ),
            activeItemId = bm.id
        )
    }

    fun goToTable(id: String) {
        val table = _state.value.index.tables.find { it.id == id } ?: return

        // Phase 7: Check Hidden state
        if (table.visibility == VisibilityState.HIDDEN) {
            showNotice(NavigatorNotice.HIDDEN_ITEM)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.TABLE,
                targetId = table.id,
                targetPageIndex = table.pageIndex,
                targetElementIndex = table.elementIndex,
                titleOrLabel = table.tableName
            ),
            activeItemId = table.id
        )
    }

    fun goToImage(id: String) {
        val img = _state.value.index.images.find { it.id == id } ?: return

        // Phase 7: Check Hidden state
        if (img.visibility == VisibilityState.HIDDEN) {
            showNotice(NavigatorNotice.HIDDEN_ITEM)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.IMAGE,
                targetId = img.id,
                targetPageIndex = img.pageIndex,
                targetElementIndex = img.elementIndex,
                titleOrLabel = img.imageName
            ),
            activeItemId = img.id
        )
    }

    fun goToComment(id: String) {
        val comment = _state.value.index.comments.find { it.id == id } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.COMMENT,
                targetId = comment.id,
                targetPageIndex = comment.pageIndex,
                targetElementIndex = comment.elementIndex,
                titleOrLabel = "Comment by ${comment.author}"
            ),
            activeItemId = comment.id
        )
    }

    /**
     * Jumps to a section. Range sections (Plan 7C) carry their resolved jump
     * target in [SectionNode.elementIndex]: the first navigable element, or for
     * a hidden section the nearest visible position outside it (owner decision
     * 2, closest to LO Writer). Legacy marker sections keep refusing hidden jumps.
     */
    fun goToSection(id: String) {
        val sec = _state.value.index.sections.find { it.id == id } ?: return

        if (sec.visibility == VisibilityState.HIDDEN) {
            if (!sec.id.startsWith(RANGE_SECTION_PREFIX)) {
                showNotice(NavigatorNotice.HIDDEN_ITEM)
                return
            }
            emitNavSignal(
                signal = NavTargetSignal(
                    targetType = NavigateBy.SECTION,
                    targetId = sec.id,
                    targetPageIndex = sec.pageIndex,
                    targetElementIndex = sec.elementIndex,
                    titleOrLabel = sec.sectionName
                ),
                activeItemId = sec.id
            )
            showNotice(NavigatorNotice.HIDDEN_SECTION_NEAREST_VISIBLE)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.SECTION,
                targetId = sec.id,
                targetPageIndex = sec.pageIndex,
                targetElementIndex = sec.elementIndex,
                titleOrLabel = sec.sectionName
            ),
            activeItemId = sec.id
        )
    }

    /** Plan 7C: jumps to the start of an authored index (leading page breaks skipped). */
    fun goToIndex(id: String) {
        val idx = _state.value.index.authoredIndexes.find { it.id == id } ?: return
        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.INDEX,
                targetId = idx.id,
                targetPageIndex = idx.pageIndex,
                targetElementIndex = idx.elementIndex,
                titleOrLabel = idx.name.orEmpty()
            ),
            activeItemId = idx.id
        )
    }

    /** Plan 7C: jumps to an index entry paragraph itself (the row in the index). */
    fun goToIndexEntry(id: String) {
        val entry = _state.value.index.authoredIndexes.asSequence()
            .flatMap { it.entries.asSequence() }
            .firstOrNull { it.id == id } ?: return
        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.INDEX,
                targetId = entry.id,
                targetPageIndex = entry.pageIndex,
                targetElementIndex = entry.elementIndex,
                titleOrLabel = entry.text
            ),
            activeItemId = entry.id
        )
    }

    /**
     * Plan 7C "Go to entry...": follows an index entry's link to its target
     * bookmark. Returns false when the anchor does not resolve, so callers
     * only offer the action when it can succeed.
     */
    fun goToIndexEntryTarget(anchor: String): Boolean {
        val bookmark = _state.value.index.bookmarks.find { it.name == anchor } ?: return false
        goToBookmark(bookmark.id)
        return true
    }

    fun goToFrame(id: String) {
        val frame = _state.value.index.frames.find { it.id == id } ?: return

        if (frame.visibility == VisibilityState.HIDDEN) {
            showNotice(NavigatorNotice.HIDDEN_ITEM)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.FRAME,
                targetId = frame.id,
                targetPageIndex = frame.pageIndex,
                targetElementIndex = frame.elementIndex,
                titleOrLabel = frame.frameName
            ),
            activeItemId = frame.id
        )
    }

    fun goToField(id: String) {
        val field = _state.value.index.fields.find { it.id == id } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.FIELD,
                targetId = field.id,
                targetPageIndex = field.pageIndex,
                targetElementIndex = field.elementIndex,
                titleOrLabel = "${field.fieldType}: ${field.value}"
            ),
            activeItemId = field.id
        )
    }

    fun goToFootnote(id: String) {
        val fn = _state.value.index.footnotes.find { it.id == id } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.FOOTNOTE,
                targetId = fn.id,
                targetPageIndex = fn.pageIndex,
                targetElementIndex = fn.elementIndex,
                titleOrLabel = "Footnote ${fn.label}"
            ),
            activeItemId = fn.id
        )
    }

    fun goToShape(id: String) {
        val shape = _state.value.index.shapes.find { it.id == id } ?: return

        if (shape.visibility == VisibilityState.HIDDEN) {
            showNotice(NavigatorNotice.HIDDEN_ITEM)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.SHAPE,
                targetId = shape.id,
                targetPageIndex = shape.pageIndex,
                targetElementIndex = shape.elementIndex,
                titleOrLabel = shape.shapeName
            ),
            activeItemId = shape.id
        )
    }

    fun goToOle(id: String) {
        val ole = _state.value.index.oleObjects.find { it.id == id } ?: return

        if (ole.visibility == VisibilityState.HIDDEN) {
            showNotice(NavigatorNotice.HIDDEN_ITEM)
            return
        }

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.OLE,
                targetId = ole.id,
                targetPageIndex = ole.pageIndex,
                targetElementIndex = ole.elementIndex,
                titleOrLabel = ole.oleName
            ),
            activeItemId = ole.id
        )
    }

    fun goToReminder(id: String) {
        val rem = _state.value.index.reminders.find { it.id == id } ?: return

        emitNavSignal(
            signal = NavTargetSignal(
                targetType = NavigateBy.REMINDER,
                targetId = rem.id,
                targetPageIndex = rem.pageIndex,
                targetElementIndex = rem.elementIndex,
                targetParagraphIndex = rem.paragraphIndex,
                titleOrLabel = rem.note
            ),
            activeItemId = rem.id
        )
    }

    fun goToPage(pageNumber: Int) {
        val total = _state.value.totalPages.coerceAtLeast(1)
        val targetPage = pageNumber.coerceIn(1, total)

        _state.value = _state.value.copy(
            currentPage = targetPage,
            navTargetSignal = NavTargetSignal(
                targetType = NavigateBy.PAGE,
                targetId = "page_$targetPage",
                targetPageIndex = targetPage,
                targetElementIndex = (targetPage - 1) * 10,
                titleOrLabel = "Page $targetPage"
            )
        )
    }

    // ==========================================
    // PHASE 4: PREVIOUS / NEXT ITERATOR
    // ==========================================

    fun next(by: NavigateBy = _state.value.navigateBy) {
        when (by) {
            NavigateBy.ALL, NavigateBy.PAGE -> goToPage(_state.value.currentPage + 1)
            NavigateBy.HEADING -> iterateList(flattenHeadings(_state.value.index.headings).map { it.id }, isNext = true) { goToHeading(it) }
            NavigateBy.TABLE -> iterateList(_state.value.index.tables.map { it.id }, isNext = true) { goToTable(it) }
            NavigateBy.IMAGE -> iterateList(_state.value.index.images.map { it.id }, isNext = true) { goToImage(it) }
            NavigateBy.BOOKMARK -> iterateList(_state.value.index.bookmarks.map { it.id }, isNext = true) { goToBookmark(it) }
            NavigateBy.COMMENT -> iterateList(_state.value.index.comments.map { it.id }, isNext = true) { goToComment(it) }
            NavigateBy.SECTION -> iterateList(_state.value.index.sections.map { it.id }, isNext = true) { goToSection(it) }
            NavigateBy.FRAME -> iterateList(_state.value.index.frames.map { it.id }, isNext = true) { goToFrame(it) }
            NavigateBy.FIELD -> iterateList(_state.value.index.fields.map { it.id }, isNext = true) { goToField(it) }
            NavigateBy.FOOTNOTE -> iterateList(_state.value.index.footnotes.map { it.id }, isNext = true) { goToFootnote(it) }
            NavigateBy.SHAPE -> iterateList(_state.value.index.shapes.map { it.id }, isNext = true) { goToShape(it) }
            NavigateBy.OLE -> iterateList(_state.value.index.oleObjects.map { it.id }, isNext = true) { goToOle(it) }
            NavigateBy.REMINDER -> iterateList(_state.value.index.reminders.map { it.id }, isNext = true) { goToReminder(it) }
            NavigateBy.INDEX -> iterateList(_state.value.index.authoredIndexes.map { it.id }, isNext = true) { goToIndex(it) }
            else -> showNotice(NavigatorNotice.NO_ITEMS_IN_MODE)
        }
    }

    fun previous(by: NavigateBy = _state.value.navigateBy) {
        when (by) {
            NavigateBy.ALL, NavigateBy.PAGE -> goToPage(_state.value.currentPage - 1)
            NavigateBy.HEADING -> iterateList(flattenHeadings(_state.value.index.headings).map { it.id }, isNext = false) { goToHeading(it) }
            NavigateBy.TABLE -> iterateList(_state.value.index.tables.map { it.id }, isNext = false) { goToTable(it) }
            NavigateBy.IMAGE -> iterateList(_state.value.index.images.map { it.id }, isNext = false) { goToImage(it) }
            NavigateBy.BOOKMARK -> iterateList(_state.value.index.bookmarks.map { it.id }, isNext = false) { goToBookmark(it) }
            NavigateBy.COMMENT -> iterateList(_state.value.index.comments.map { it.id }, isNext = false) { goToComment(it) }
            NavigateBy.SECTION -> iterateList(_state.value.index.sections.map { it.id }, isNext = false) { goToSection(it) }
            NavigateBy.FRAME -> iterateList(_state.value.index.frames.map { it.id }, isNext = false) { goToFrame(it) }
            NavigateBy.FIELD -> iterateList(_state.value.index.fields.map { it.id }, isNext = false) { goToField(it) }
            NavigateBy.FOOTNOTE -> iterateList(_state.value.index.footnotes.map { it.id }, isNext = false) { goToFootnote(it) }
            NavigateBy.SHAPE -> iterateList(_state.value.index.shapes.map { it.id }, isNext = false) { goToShape(it) }
            NavigateBy.OLE -> iterateList(_state.value.index.oleObjects.map { it.id }, isNext = false) { goToOle(it) }
            NavigateBy.REMINDER -> iterateList(_state.value.index.reminders.map { it.id }, isNext = false) { goToReminder(it) }
            NavigateBy.INDEX -> iterateList(_state.value.index.authoredIndexes.map { it.id }, isNext = false) { goToIndex(it) }
            else -> showNotice(NavigatorNotice.NO_ITEMS_IN_MODE)
        }
    }

    private fun iterateList(ids: List<String>, isNext: Boolean, action: (String) -> Unit) {
        if (ids.isEmpty()) {
            showNotice(NavigatorNotice.NO_ITEMS_IN_CATEGORY)
            return
        }
        val currentId = _state.value.activeItemId
        val currentIdx = ids.indexOf(currentId)

        val nextIdx = if (currentIdx == -1) {
            if (isNext) 0 else ids.lastIndex
        } else {
            if (isNext) {
                (currentIdx + 1) % ids.size
            } else {
                if (currentIdx - 1 < 0) ids.lastIndex else currentIdx - 1
            }
        }
        action(ids[nextIdx])
    }

    // ==========================================
    // PHASE 6: HEADING FOLDING
    // ==========================================

    fun toggleHeadingFolding(headingId: String) {
        val current = foldStatesMap[headingId] ?: false
        val newFoldState = !current
        foldStatesMap[headingId] = newFoldState

        indexEngine.headingFoldStates = foldStatesMap
        val reindexed = indexEngine.reindex()

        _state.value = _state.value.copy(
            index = reindexed,
            headingFoldStates = foldStatesMap.toMap()
        )
    }

    // ==========================================
    // PHASE 7: HIDDEN OBJECTS VISIBILITY
    // ==========================================

    fun setObjectVisibility(id: String, visibility: VisibilityState) {
        objectVisibilityMap[id] = visibility
        indexEngine.objectVisibilities = objectVisibilityMap
        val reindexed = indexEngine.reindex()

        _state.value = _state.value.copy(
            index = reindexed,
            objectVisibilities = objectVisibilityMap.toMap()
        )
    }

    fun showNotice(notice: NavigatorNotice) {
        _state.value = _state.value.copy(notice = notice)
    }

    fun clearNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    fun clearNavSignal() {
        _state.value = _state.value.copy(navTargetSignal = null)
    }

    /** P2-2: Update Navigator language policy without recreating the engine. */
    fun setNavigatorLocalePolicy(preferAppLocale: Boolean, appLanguageTag: String? = null) {
        if (indexEngine.preferAppLocale != preferAppLocale || indexEngine.appLanguageTag != appLanguageTag) {
            indexEngine.preferAppLocale = preferAppLocale
            indexEngine.appLanguageTag = appLanguageTag
            // Reindex with new locale so auto-names switch immediately
            val updatedIndex = indexEngine.reindex()
            val pagesCount = _state.value.totalPages
            _state.value = _state.value.copy(index = updatedIndex, totalPages = pagesCount)
        }
    }

    private fun emitNavSignal(
        signal: NavTargetSignal,
        activeHeadingId: String? = _state.value.activeHeadingId,
        activeItemId: String? = _state.value.activeItemId
    ) {
        _state.value = _state.value.copy(
            currentPage = signal.targetPageIndex,
            activeHeadingId = activeHeadingId,
            activeItemId = activeItemId,
            navTargetSignal = signal
        )
    }

    private fun flattenHeadings(list: List<HeadingNode>): List<HeadingNode> {
        val result = mutableListOf<HeadingNode>()
        fun recurse(node: HeadingNode) {
            result.add(node)
            node.children.forEach { recurse(it) }
        }
        list.forEach { recurse(it) }
        return result
    }

    private companion object {
        /** Id prefix the index engine gives Plan 7C range sections. */
        const val RANGE_SECTION_PREFIX = "section_range_"
    }

    private fun calculateTotalPages(index: DocumentIndex): Int {
        val maxHeadingPage = flattenHeadings(index.headings).maxOfOrNull { it.pageIndex } ?: 1
        val maxTablePage = index.tables.maxOfOrNull { it.pageIndex } ?: 1
        val maxImgPage = index.images.maxOfOrNull { it.pageIndex } ?: 1
        val maxBookmarkPage = index.bookmarks.maxOfOrNull { it.pageIndex } ?: 1
        val maxCommentPage = index.comments.maxOfOrNull { it.pageIndex } ?: 1
        val maxSectionPage = index.sections.maxOfOrNull { it.pageIndex } ?: 1
        val maxFramePage = index.frames.maxOfOrNull { it.pageIndex } ?: 1
        val maxFieldPage = index.fields.maxOfOrNull { it.pageIndex } ?: 1
        val maxFootnotePage = index.footnotes.maxOfOrNull { it.pageIndex } ?: 1
        val maxShapePage = index.shapes.maxOfOrNull { it.pageIndex } ?: 1
        val maxLinkPage = index.hyperlinks.maxOfOrNull { it.pageIndex } ?: 1
        val maxOlePage = index.oleObjects.maxOfOrNull { it.pageIndex } ?: 1
        val maxReminderPage = index.reminders.maxOfOrNull { it.pageIndex } ?: 1

        return maxOf(
            maxHeadingPage, maxTablePage, maxImgPage, maxBookmarkPage,
            maxCommentPage, maxSectionPage, maxFramePage, maxFieldPage,
            maxFootnotePage, maxShapePage, maxLinkPage, maxOlePage, maxReminderPage, 1
        )
    }
}
