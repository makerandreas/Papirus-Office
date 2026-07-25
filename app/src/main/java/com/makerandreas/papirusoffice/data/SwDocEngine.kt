package com.makerandreas.papirusoffice.data

import androidx.compose.ui.geometry.Rect

/**
 * LibreOffice Writer Core Architecture (SwDoc) Representation in Papirus Engine.
 *
 * Implements LibreOffice Writer's fundamental subsystems:
 * 1. SwDoc & Managers (IDocumentUndoRedo, IDocumentFieldsAccess, IDocumentListsAccess, IDocumentLayoutAccess)
 * 2. SwNodes (Flat array with SwStartNode / SwEndNode sections: Empty, Footnote, Header/Footer, ChangeTracking, Body)
 * 3. Undo/Redo Engine (SwUndoManager, SwUndo actions, ListActions with StartUndo/EndUndo wrappers, Undo SwNodes)
 * 4. Text Attributes (SwpHintsArray, SwTextAttr: Formatting, Nesting, Misc, Without-End dummy char attributes)
 * 5. Fields Model (SwFieldIds, SwFieldType, SwFormatField, SwField, SwTextField, SwXTextField)
 * 6. Lists Engine (SwNumFormat, SwNumRule, SwNodeNum, SwList, DocumentListsManager, Outline numbering)
 * 7. Layout Tree (SwFrame with upper/lower/next/prev, SwFlowFrame with master/follow/precede pointers)
 */

// ============================================================================
// 1. SWNODES & SECTION STRUCTURE
// ============================================================================

enum class SwNodeSectionType {
    EMPTY,
    FOOTNOTE,
    HEADER_FOOTER,
    DELETED_CHANGE_TRACKING,
    BODY
}

sealed class SwNode(open val nodeIndex: Int) {
    data class StartNode(
        override val nodeIndex: Int,
        val sectionType: SwNodeSectionType,
        val sectionName: String = sectionType.name
    ) : SwNode(nodeIndex)

    data class EndNode(
        override val nodeIndex: Int,
        val startNodeIndex: Int
    ) : SwNode(nodeIndex)

    data class TextNode(
        override val nodeIndex: Int,
        var text: String = "",
        val hints: SwpHintsArray = SwpHintsArray(),
        var outlineLevel: Int = 0,
        var numberingStyleName: String? = null,
        var listId: String? = null,
        var listLevel: Int = 0,
        var isNumberingRestart: Boolean = false,
        var numberingRestartValue: Int = 1,
        var isCountedInNumbering: Boolean = true
    ) : SwNode(nodeIndex)

    data class TableNode(
        override val nodeIndex: Int,
        val rowsCount: Int,
        val colsCount: Int,
        val tableTitle: String = "Table"
    ) : SwNode(nodeIndex)
}

class SwNodes {
    private val nodeList = mutableListOf<SwNode>()

    // Top-level section boundaries
    val emptySectionStart: Int = 0
    var footnoteSectionStart: Int = -1
    var headerFooterSectionStart: Int = -1
    var changeTrackingSectionStart: Int = -1
    var bodySectionStart: Int = -1

    init {
        initializeDefaultTopLevelSections()
    }

    private fun initializeDefaultTopLevelSections() {
        var idx = 0
        
        // 1. Empty Section
        nodeList.add(SwNode.StartNode(idx++, SwNodeSectionType.EMPTY))
        nodeList.add(SwNode.EndNode(idx++, 0))

        // 2. Footnote Section
        footnoteSectionStart = idx
        nodeList.add(SwNode.StartNode(idx++, SwNodeSectionType.FOOTNOTE))
        nodeList.add(SwNode.EndNode(idx++, footnoteSectionStart))

        // 3. Header / Footer Section
        headerFooterSectionStart = idx
        nodeList.add(SwNode.StartNode(idx++, SwNodeSectionType.HEADER_FOOTER))
        nodeList.add(SwNode.EndNode(idx++, headerFooterSectionStart))

        // 4. Change Tracking Section
        changeTrackingSectionStart = idx
        nodeList.add(SwNode.StartNode(idx++, SwNodeSectionType.DELETED_CHANGE_TRACKING))
        nodeList.add(SwNode.EndNode(idx++, changeTrackingSectionStart))

        // 5. Body Section
        bodySectionStart = idx
        nodeList.add(SwNode.StartNode(idx++, SwNodeSectionType.BODY))
        // Default initial text node in body
        nodeList.add(SwNode.TextNode(idx++, "Papirus Writer Document Content"))
        nodeList.add(SwNode.EndNode(idx++, bodySectionStart))
    }

    fun getAllNodes(): List<SwNode> = nodeList.toList()

    fun getBodyTextNodes(): List<SwNode.TextNode> {
        return nodeList.filterIsInstance<SwNode.TextNode>()
    }

    fun appendBodyTextNode(text: String): SwNode.TextNode {
        val endBodyIdx = nodeList.indexOfLast { it is SwNode.EndNode && it.startNodeIndex == bodySectionStart }
        val insertIndex = if (endBodyIdx != -1) endBodyIdx else nodeList.size
        
        val newTextNode = SwNode.TextNode(insertIndex, text)
        nodeList.add(insertIndex, newTextNode)
        reindexNodes()
        return newTextNode
    }

    private fun reindexNodes() {
        // Re-index all nodes sequentially
        val oldList = ArrayList(nodeList)
        nodeList.clear()
        oldList.forEachIndexed { i, node ->
            val updatedNode = when (node) {
                is SwNode.StartNode -> node.copy(nodeIndex = i)
                is SwNode.EndNode -> node.copy(nodeIndex = i)
                is SwNode.TextNode -> node.copy(nodeIndex = i)
                is SwNode.TableNode -> node.copy(nodeIndex = i)
            }
            nodeList.add(updatedNode)
        }
    }
}

// ============================================================================
// 2. TEXT ATTRIBUTES & SWPHINTSARRAY
// ============================================================================

enum class SwTextAttrCategory {
    FORMATTING, // Char styles, auto styles (BuildPortions/MergePortions)
    NESTING,    // Hyperlinks, CJK Ruby, Meta/Metafield
    MISC,       // Reference marks, ToX marks
    WITHOUT_END // Fields, Footnotes, Flys (AS_CHAR dummy character placeholder)
}

enum class SwAttrResId {
    RES_TXTATR_CHARFMT,
    RES_TXTATR_AUTOFMT,
    RES_TXTATR_INETFMT,
    RES_TXTATR_CJK_RUBY,
    RES_TXTATR_METAFIELD,
    RES_TXTATR_REFMARK,
    RES_TXTATR_FIELD,
    RES_TXTATR_FOOTNOTE,
    RES_TXTATR_FLY_AS_CHAR
}

sealed class SwTextAttr(
    open val startIdx: Int,
    open val endIdx: Int,
    open val resId: SwAttrResId,
    open val category: SwTextAttrCategory
) {
    data class CharFormat(
        override val startIdx: Int,
        override val endIdx: Int,
        val styleName: String,
        val isBold: Boolean = false,
        val isItalic: Boolean = false,
        val isUnderline: Boolean = false
    ) : SwTextAttr(startIdx, endIdx, SwAttrResId.RES_TXTATR_CHARFMT, SwTextAttrCategory.FORMATTING)

    data class Hyperlink(
        override val startIdx: Int,
        override val endIdx: Int,
        val url: String,
        val targetFrame: String = "_blank"
    ) : SwTextAttr(startIdx, endIdx, SwAttrResId.RES_TXTATR_INETFMT, SwTextAttrCategory.NESTING)

    data class CjkRuby(
        override val startIdx: Int,
        override val endIdx: Int,
        val rubyText: String
    ) : SwTextAttr(startIdx, endIdx, SwAttrResId.RES_TXTATR_CJK_RUBY, SwTextAttrCategory.NESTING)

    data class FieldAttr(
        val positionIdx: Int,
        val formatField: SwFormatField
    ) : SwTextAttr(positionIdx, positionIdx + 1, SwAttrResId.RES_TXTATR_FIELD, SwTextAttrCategory.WITHOUT_END)
}

class SwpHintsArray {
    private val hints = mutableListOf<SwTextAttr>()

    fun addAttr(attr: SwTextAttr) {
        hints.add(attr)
    }

    fun getAttrs(): List<SwTextAttr> = hints.toList()

    /**
     * LibreOffice Writer BuildPortions & MergePortions
     * Converts overlapping attribute ranges into clean, non-overlapping formatting portions.
     */
    fun buildPortions(textLength: Int): List<Pair<IntRange, List<SwTextAttr>>> {
        if (hints.isEmpty() || textLength == 0) return listOf(0 until textLength to emptyList())

        val boundaries = mutableSetOf(0, textLength)
        hints.forEach {
            boundaries.add(it.startIdx.coerceIn(0, textLength))
            boundaries.add(it.endIdx.coerceIn(0, textLength))
        }

        val sortedBoundaries = boundaries.sorted()
        val result = mutableListOf<Pair<IntRange, List<SwTextAttr>>>()

        for (i in 0 until sortedBoundaries.size - 1) {
            val start = sortedBoundaries[i]
            val end = sortedBoundaries[i + 1]
            if (start < end) {
                val activeAttrs = hints.filter { attr ->
                    attr.startIdx <= start && attr.endIdx >= end
                }
                result.add((start until end) to activeAttrs)
            }
        }
        return result
    }
}

// ============================================================================
// 3. FIELDS SUBSYSTEM
// ============================================================================

enum class SwFieldIds {
    PAGE_NUMBER,
    PAGE_COUNT,
    AUTHOR,
    DATE_TIME,
    DOCUMENT_TITLE,
    VARIABLE_SET,
    VARIABLE_GET,
    CUSTOM_FIELD
}

abstract class SwFieldType(val fieldId: SwFieldIds, val name: String) {
    class PageNumber : SwFieldType(SwFieldIds.PAGE_NUMBER, "Page Number")
    class Author : SwFieldType(SwFieldIds.AUTHOR, "Author")
    class DateTime : SwFieldType(SwFieldIds.DATE_TIME, "Date/Time")
    class DocTitle : SwFieldType(SwFieldIds.DOCUMENT_TITLE, "Document Title")
    class Variable(varName: String) : SwFieldType(SwFieldIds.VARIABLE_SET, "Var: $varName")
}

class SwField(val fieldType: SwFieldType, var cachedExpansion: String = "") {
    fun expand(context: FieldExpansionContext): String {
        cachedExpansion = when (fieldType.fieldId) {
            SwFieldIds.PAGE_NUMBER -> context.pageNumber.toString()
            SwFieldIds.PAGE_COUNT -> context.pageCount.toString()
            SwFieldIds.AUTHOR -> context.author
            SwFieldIds.DATE_TIME -> context.formattedDate
            SwFieldIds.DOCUMENT_TITLE -> context.documentTitle
            else -> cachedExpansion
        }
        return cachedExpansion
    }
}

data class FieldExpansionContext(
    val pageNumber: Int = 1,
    val pageCount: Int = 1,
    val author: String = "Papirus User",
    val formattedDate: String = "2026-07-25",
    val documentTitle: String = "Untitled Document"
)

class SwFormatField(val field: SwField)

class DocumentFieldsManager {
    private val fieldTypes = mutableMapOf<SwFieldIds, SwFieldType>()

    init {
        initFieldTypes()
    }

    private fun initFieldTypes() {
        fieldTypes[SwFieldIds.PAGE_NUMBER] = SwFieldType.PageNumber()
        fieldTypes[SwFieldIds.AUTHOR] = SwFieldType.Author()
        fieldTypes[SwFieldIds.DATE_TIME] = SwFieldType.DateTime()
        fieldTypes[SwFieldIds.DOCUMENT_TITLE] = SwFieldType.DocTitle()
    }

    fun getFieldType(id: SwFieldIds): SwFieldType? = fieldTypes[id]

    fun createField(id: SwFieldIds, defaultVal: String = ""): SwFormatField {
        val type = fieldTypes[id] ?: SwFieldType.PageNumber()
        return SwFormatField(SwField(type, defaultVal))
    }
}

// ============================================================================
// 4. LISTS & NUMBERING ENGINE
// ============================================================================

data class SwNumFormat(
    val level: Int,
    val numberType: String = "1, 2, 3",
    val prefix: String = "",
    val suffix: String = "."
) {
    fun formatNumber(valInt: Int): String = "$prefix$valInt$suffix"
}

data class SwNumRule(
    val ruleName: String,
    val levels: Map<Int, SwNumFormat> = mapOf(
        0 to SwNumFormat(0, "1, 2, 3", "", "."),
        1 to SwNumFormat(1, "a, b, c", "(", ")"),
        2 to SwNumFormat(2, "i, ii, iii", "", ".")
    )
)

data class SwNodeNum(
    val textNodeIndex: Int,
    val listId: String,
    val level: Int,
    val formattedNumberString: String
)

class SwList(val listId: String, val numRule: SwNumRule) {
    private val nodeNums = mutableListOf<SwNodeNum>()

    fun updateNodeNumbering(textNodes: List<SwNode.TextNode>): List<SwNodeNum> {
        nodeNums.clear()
        var counter = 1
        textNodes.forEach { node ->
            if (node.listId == listId) {
                val numFormat = numRule.levels[node.listLevel] ?: SwNumFormat(node.listLevel)
                val numStr = if (node.isCountedInNumbering) {
                    if (node.isNumberingRestart) counter = node.numberingRestartValue
                    numFormat.formatNumber(counter++)
                } else ""
                nodeNums.add(SwNodeNum(node.nodeIndex, listId, node.listLevel, numStr))
            }
        }
        return nodeNums.toList()
    }
}

class DocumentListsManager {
    private val listsMap = mutableMapOf<String, SwList>()

    fun getOrCreateList(listId: String, numRule: SwNumRule = SwNumRule("DefaultRule")): SwList {
        return listsMap.getOrPut(listId) { SwList(listId, numRule) }
    }
}

// ============================================================================
// 5. UNDO / REDO ENGINE
// ============================================================================

sealed class SwUndo {
    data class TextInsert(
        val nodeIndex: Int,
        val charPosition: Int,
        val insertedText: String
    ) : SwUndo()

    data class TextDelete(
        val nodeIndex: Int,
        val charPosition: Int,
        val deletedText: String
    ) : SwUndo()

    data class FormatChange(
        val nodeIndex: Int,
        val attrName: String,
        val oldValue: Any?,
        val newValue: Any?
    ) : SwUndo()

    data class ListAction(
        val actionName: String,
        val subActions: List<SwUndo>
    ) : SwUndo()
}

interface IDocumentUndoRedo {
    fun startUndo(actionName: String)
    fun endUndo()
    fun addUndoAction(action: SwUndo)
    fun undo(): Boolean
    fun redo(): Boolean
    fun getUndoStackSize(): Int
    fun getRedoStackSize(): Int
}

class SwUndoManager : IDocumentUndoRedo {
    private val undoStack = mutableListOf<SwUndo>()
    private val redoStack = mutableListOf<SwUndo>()
    
    private var isRecordingListAction = false
    private var currentListActionName = ""
    private val pendingListSubActions = mutableListOf<SwUndo>()

    // Separate SwNodes holding content required for Undo/Redo (Writer Undo Nodes)
    val undoNodes: SwNodes = SwNodes()

    override fun startUndo(actionName: String) {
        isRecordingListAction = true
        currentListActionName = actionName
        pendingListSubActions.clear()
    }

    override fun endUndo() {
        if (isRecordingListAction) {
            if (pendingListSubActions.isNotEmpty()) {
                undoStack.add(SwUndo.ListAction(currentListActionName, ArrayList(pendingListSubActions)))
            }
            isRecordingListAction = false
            pendingListSubActions.clear()
        }
    }

    override fun addUndoAction(action: SwUndo) {
        if (isRecordingListAction) {
            pendingListSubActions.add(action)
        } else {
            undoStack.add(action)
        }
        redoStack.clear()
    }

    override fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val action = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(action)
        return true
    }

    override fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val action = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(action)
        return true
    }

    override fun getUndoStackSize(): Int = undoStack.size
    override fun getRedoStackSize(): Int = redoStack.size
}

// ============================================================================
// 6. LAYOUT TREE (SWFRAME & SWFLOWFRAME)
// ============================================================================

abstract class SwFrame(val frameId: String) {
    var upper: SwFrame? = null
    var lower: SwFrame? = null
    var next: SwFrame? = null
    var prev: SwFrame? = null

    var bounds: Rect = Rect(0f, 0f, 0f, 0f)
}

class SwPageFrame(pageId: String) : SwFrame("Page_$pageId")
class SwBodyFrame(bodyId: String) : SwFrame("Body_$bodyId")
class SwTxtFrm(frmId: String, val textNodeIndex: Int) : SwFrame("TxtFrm_$frmId")

/**
 * SwFlowFrame handles flowing content across page boundaries in Writer.
 */
class SwFlowFrame(val masterFrame: SwFrame) {
    var follow: SwFlowFrame? = null
    var precede: SwFlowFrame? = null

    val isMaster: Boolean get() = precede == null
    val isFollow: Boolean get() = precede != null
}

// ============================================================================
// 7. CENTRAL DOCUMENT CLASS (SWDOC)
// ============================================================================

class SwDoc(val documentTitle: String = "Papirus Writer Document") {
    
    // Core Node Array
    val nodes: SwNodes = SwNodes()

    // Subsystem Managers
    val undoManager: SwUndoManager = SwUndoManager()
    val fieldsManager: DocumentFieldsManager = DocumentFieldsManager()
    val listsManager: DocumentListsManager = DocumentListsManager()

    // Layout Root
    var layoutRoot: SwPageFrame = SwPageFrame("1")

    fun getIDocumentUndoRedo(): IDocumentUndoRedo = undoManager
    fun getIDocumentFieldsAccess(): DocumentFieldsManager = fieldsManager
    fun getIDocumentListsAccess(): DocumentListsManager = listsManager

    /**
     * Converts SwDoc document model into Papirus OfficeParsedDocument format
     */
    fun toOfficeParsedDocument(): OfficeParsedDocument {
        val bodyTextNodes = nodes.getBodyTextNodes()
        val elements = bodyTextNodes.map { node ->
            OfficeDocumentElement.Paragraph(
                text = node.text,
                styleName = if (node.outlineLevel > 0) "Heading ${node.outlineLevel}" else "Normal"
            )
        }
        val fullText = bodyTextNodes.joinToString("\n") { it.text }
        
        return OfficeParsedDocument(
            elements = elements,
            plainText = fullText,
            isOdt = true
        )
    }
}
