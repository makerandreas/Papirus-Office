package com.makerandreas.papirusoffice.data.writer

/**
 * Top-level sections in a Writer document hierarchy as per LibreOffice SwDoc structure.
 */
enum class SwNodeSectionType {
    EMPTY,
    FOOTNOTE,
    FRAME,
    CHANGE_TRACKING,
    BODY
}

/**
 * Abstract base class for all nodes in the document's SwNodes array.
 */
abstract class SwNode(
    var index: Int,
    val sectionType: SwNodeSectionType
)

/**
 * Start node representing the start of a nested section or group.
 */
class SwStartNode(
    index: Int,
    sectionType: SwNodeSectionType,
    val sectionName: String = ""
) : SwNode(index, sectionType) {
    var endNode: SwEndNode? = null
}

/**
 * End node corresponding to a matching SwStartNode.
 */
class SwEndNode(
    index: Int,
    sectionType: SwNodeSectionType,
    val startNode: SwStartNode
) : SwNode(index, sectionType) {
    init {
        startNode.endNode = this
    }
}

/**
 * Abstract content node containing text, table, or graphics data.
 */
open class SwContentNode(
    index: Int,
    sectionType: SwNodeSectionType
) : SwNode(index, sectionType)

/**
 * Text paragraph node holding characters and text formatting attributes.
 */
class SwTextNode(
    index: Int,
    sectionType: SwNodeSectionType,
    var text: String = "",
    val textAttributes: MutableList<SwTextAttr> = mutableListOf()
) : SwContentNode(index, sectionType) {

    fun addAttribute(attr: SwTextAttr) {
        textAttributes.add(attr)
    }

    fun removeAttribute(attr: SwTextAttr) {
        textAttributes.remove(attr)
    }
}

/**
 * Central container representing the flat/nested array of SwNode pointers in LibreOffice Writer.
 */
class SwNodes {
    private val nodesList: MutableList<SwNode> = mutableListOf()

    init {
        // Initialize default 5 top-level document sections
        SwNodeSectionType.values().forEach { sectionType ->
            val start = SwStartNode(nodesList.size, sectionType, sectionType.name)
            nodesList.add(start)
            val end = SwEndNode(nodesList.size, sectionType, start)
            nodesList.add(end)
        }
    }

    val size: Int
        get() = nodesList.size

    fun getNode(index: Int): SwNode? {
        return if (index in 0 until nodesList.size) nodesList[index] else null
    }

    fun appendTextNodeToBody(text: String): SwTextNode {
        val bodyEnd = getSectionEndNode(SwNodeSectionType.BODY)
        val insertIndex = bodyEnd?.index ?: nodesList.size
        
        val textNode = SwTextNode(insertIndex, SwNodeSectionType.BODY, text)
        nodesList.add(insertIndex, textNode)
        reindexNodes()
        return textNode
    }

    fun insertNode(index: Int, node: SwNode) {
        val validIndex = index.coerceIn(0, nodesList.size)
        node.index = validIndex
        nodesList.add(validIndex, node)
        reindexNodes()
    }

    fun removeNode(index: Int): SwNode? {
        if (index !in 0 until nodesList.size) return null
        val removed = nodesList.removeAt(index)
        reindexNodes()
        return removed
    }

    fun getSectionStartNode(type: SwNodeSectionType): SwStartNode? {
        return nodesList.filterIsInstance<SwStartNode>().firstOrNull { it.sectionType == type }
    }

    fun getSectionEndNode(type: SwNodeSectionType): SwEndNode? {
        return nodesList.filterIsInstance<SwEndNode>().firstOrNull { it.sectionType == type }
    }

    fun getNodesInSection(type: SwNodeSectionType): List<SwNode> {
        val start = getSectionStartNode(type) ?: return emptyList()
        val end = getSectionEndNode(type) ?: return emptyList()
        return nodesList.subList(start.index + 1, end.index)
    }

    fun getAllTextNodes(): List<SwTextNode> {
        return nodesList.filterIsInstance<SwTextNode>()
    }

    private fun reindexNodes() {
        nodesList.forEachIndexed { i, node ->
            node.index = i
        }
    }

    fun copyNodes(): SwNodes {
        val copy = SwNodes()
        copy.nodesList.clear()
        nodesList.forEach { node ->
            when (node) {
                is SwTextNode -> {
                    copy.nodesList.add(SwTextNode(node.index, node.sectionType, node.text, node.textAttributes.toMutableList()))
                }
                is SwStartNode -> {
                    copy.nodesList.add(SwStartNode(node.index, node.sectionType, node.sectionName))
                }
                is SwEndNode -> {
                    val matchingStart = copy.nodesList.filterIsInstance<SwStartNode>().firstOrNull { it.sectionType == node.sectionType }
                        ?: SwStartNode(node.index, node.sectionType, node.sectionType.name)
                    copy.nodesList.add(SwEndNode(node.index, node.sectionType, matchingStart))
                }
                else -> {
                    copy.nodesList.add(SwContentNode(node.index, node.sectionType))
                }
            }
        }
        return copy
    }
}
