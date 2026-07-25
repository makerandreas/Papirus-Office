package com.makerandreas.papirusoffice.data.writer

/**
 * Base sealed class for SwUndo action records.
 */
sealed class SwUndo(val description: String) {

    class SwUndoInsertText(
        val nodeIndex: Int,
        val offset: Int,
        val insertedText: String
    ) : SwUndo("Insert Text")

    class SwUndoDeleteText(
        val nodeIndex: Int,
        val offset: Int,
        val deletedText: String
    ) : SwUndo("Delete Text")

    class SwUndoFormatChange(
        val nodeIndex: Int,
        val oldAttrs: List<SwTextAttr>,
        val newAttrs: List<SwTextAttr>
    ) : SwUndo("Format Change")

    class SwUndoNodeOperation(
        val nodeIndex: Int,
        val backedUpNode: SwNode
    ) : SwUndo("Node Operation")
}

/**
 * Manages SwUndo action stack and secondary SwNodes array for content restoration
 * mirroring LibreOffice Writer UndoManager architecture.
 */
class UndoManager(
    private val maxStackSize: Int = 50
) {
    private val undoStack: ArrayDeque<SwUndo> = ArrayDeque()
    private val redoStack: ArrayDeque<SwUndo> = ArrayDeque()

    /**
     * Secondary SwNodes array for storing deleted/backed-up nodes for recovery.
     */
    val secondaryUndoNodes: SwNodes = SwNodes()

    fun recordUndo(action: SwUndo) {
        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun performUndo(primaryNodes: SwNodes): Boolean {
        if (!canUndo()) return false
        val action = undoStack.removeLast()

        when (action) {
            is SwUndo.SwUndoInsertText -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null && node.text.length >= action.offset + action.insertedText.length) {
                    node.text = node.text.removeRange(action.offset, action.offset + action.insertedText.length)
                }
            }
            is SwUndo.SwUndoDeleteText -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null) {
                    val sb = StringBuilder(node.text)
                    val safeOffset = action.offset.coerceIn(0, sb.length)
                    sb.insert(safeOffset, action.deletedText)
                    node.text = sb.toString()
                }
            }
            is SwUndo.SwUndoFormatChange -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null) {
                    node.textAttributes.clear()
                    node.textAttributes.addAll(action.oldAttrs)
                }
            }
            is SwUndo.SwUndoNodeOperation -> {
                primaryNodes.insertNode(action.nodeIndex, action.backedUpNode)
            }
        }

        redoStack.addLast(action)
        return true
    }

    fun performRedo(primaryNodes: SwNodes): Boolean {
        if (!canRedo()) return false
        val action = redoStack.removeLast()

        when (action) {
            is SwUndo.SwUndoInsertText -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null) {
                    val sb = StringBuilder(node.text)
                    val safeOffset = action.offset.coerceIn(0, sb.length)
                    sb.insert(safeOffset, action.insertedText)
                    node.text = sb.toString()
                }
            }
            is SwUndo.SwUndoDeleteText -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null && node.text.length >= action.offset + action.deletedText.length) {
                    node.text = node.text.removeRange(action.offset, action.offset + action.deletedText.length)
                }
            }
            is SwUndo.SwUndoFormatChange -> {
                val node = primaryNodes.getNode(action.nodeIndex) as? SwTextNode
                if (node != null) {
                    node.textAttributes.clear()
                    node.textAttributes.addAll(action.newAttrs)
                }
            }
            is SwUndo.SwUndoNodeOperation -> {
                primaryNodes.removeNode(action.nodeIndex)
            }
        }

        undoStack.addLast(action)
        return true
    }

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }
}
