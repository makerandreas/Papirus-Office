package com.makerandreas.papirusoffice.data

data class OutlineNode(
    val headingIndex: Int,
    val headingText: String,
    val level: Int,
    val childElementIndices: List<Int>,
    var isCollapsed: Boolean = false
)

interface OutlineEngine {
    fun buildOutline(document: OfficeDocument)
    fun toggle(headingIndex: Int)
    fun collapse(headingIndex: Int)
    fun expand(headingIndex: Int)
    fun collapseAll()
    fun expandAll()
    fun isElementHidden(elementIndex: Int): Boolean
    fun getNodes(): List<OutlineNode>
}

class OutlineEngineImpl : OutlineEngine {
    private var nodes: List<OutlineNode> = emptyList()

    override fun buildOutline(document: OfficeDocument) {
        val elements = document.body.elements
        val tempNodes = mutableListOf<OutlineNode>()

        elements.forEachIndexed { index, element ->
            val styleName = when (element) {
                is OfficeDocElement.ParagraphElement -> element.paragraph.styleName
                is OfficeParagraph -> element.styleName
                else -> null
            }
            val headingLevel = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(styleName)
            if (headingLevel > 0) {
                val level = headingLevel
                
                // Find all child elements under this heading
                val childIndices = mutableListOf<Int>()
                for (j in (index + 1) until elements.size) {
                    val nextElem = elements[j]
                    val nextStyle = when (nextElem) {
                        is OfficeDocElement.ParagraphElement -> nextElem.paragraph.styleName
                        is OfficeParagraph -> nextElem.styleName
                        else -> null
                    }
                    val nextLevel = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(nextStyle)
                    if (nextLevel > 0) {
                        if (nextLevel <= level) {
                            break
                        }
                    }
                    childIndices.add(j)
                }

                val text = when (element) {
                    is OfficeDocElement.ParagraphElement -> element.paragraph.text
                    is OfficeParagraph -> element.text
                    else -> ""
                }

                tempNodes.add(
                    OutlineNode(
                        headingIndex = index,
                        headingText = text,
                        level = level,
                        childElementIndices = childIndices
                    )
                )
            }
        }
        val oldMap = nodes.associate { it.headingIndex to it.isCollapsed }
        tempNodes.forEach { node ->
            node.isCollapsed = oldMap[node.headingIndex] ?: false
        }
        nodes = tempNodes
    }

    override fun toggle(headingIndex: Int) {
        nodes.find { it.headingIndex == headingIndex }?.let {
            it.isCollapsed = !it.isCollapsed
        }
    }

    override fun collapse(headingIndex: Int) {
        nodes.find { it.headingIndex == headingIndex }?.let {
            it.isCollapsed = true
        }
    }

    override fun expand(headingIndex: Int) {
        nodes.find { it.headingIndex == headingIndex }?.let {
            it.isCollapsed = false
        }
    }

    override fun collapseAll() {
        nodes.forEach { it.isCollapsed = true }
    }

    override fun expandAll() {
        nodes.forEach { it.isCollapsed = false }
    }

    override fun isElementHidden(elementIndex: Int): Boolean {
        return nodes.any { node ->
            node.isCollapsed && node.childElementIndices.contains(elementIndex)
        }
    }

    override fun getNodes(): List<OutlineNode> = nodes
}
