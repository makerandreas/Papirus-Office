package com.makerandreas.papirusoffice.data.undo

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.OfficeDocElement
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.extractParagraph
import com.makerandreas.papirusoffice.data.replaceParagraph

/**
 * Phase 1: UndoAction Interface
 * Represents an atomic or compound undoable/redoable document action.
 * Mirroring XUndoAction in LibreOffice Writer.
 */
interface UndoAction {
    val title: String
    val timestamp: Long
    val icon: String
    val commandType: String

    suspend fun undo()
    suspend fun redo()
}

/**
 * Concrete implementations of UndoAction
 */

// 1. InsertTextAction
class InsertTextAction(
    val paragraphIndex: Int,
    val offset: Int,
    val insertedText: String,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Insert \"$insertedText\"",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "text_fields",
    override val commandType: String = "INSERT_TEXT"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val startIndex = offset
            val endIndex = offset + insertedText.length
            val newText = if (startIndex in 0..text.length && endIndex <= text.length) {
                text.removeRange(startIndex, endIndex)
            } else {
                text.removeSuffix(insertedText)
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = newText))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val newText = if (offset in 0..text.length) {
                text.substring(0, offset) + insertedText + text.substring(offset)
            } else {
                text + insertedText
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = newText))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 2. DeleteTextAction
class DeleteTextAction(
    val paragraphIndex: Int,
    val offset: Int,
    val deletedText: String,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Delete \"$deletedText\"",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "backspace",
    override val commandType: String = "DELETE_TEXT"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val newText = if (offset in 0..text.length) {
                text.substring(0, offset) + deletedText + text.substring(offset)
            } else {
                text + deletedText
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = newText))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val startIndex = offset
            val endIndex = offset + deletedText.length
            val newText = if (startIndex in 0..text.length && endIndex <= text.length) {
                text.removeRange(startIndex, endIndex)
            } else {
                text
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = newText))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 3. ReplaceTextAction
class ReplaceTextAction(
    val paragraphIndex: Int,
    val offset: Int,
    val oldText: String,
    val newText: String,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Replace \"$oldText\" with \"$newText\"",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "find_replace",
    override val commandType: String = "REPLACE_TEXT"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val replaced = text.replaceRange(offset, (offset + newText.length).coerceAtMost(text.length), oldText)
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = replaced))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val text = oldPara.text
            val replaced = text.replaceRange(offset, (offset + oldText.length).coerceAtMost(text.length), newText)
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = replaced))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 4. ParagraphStyleAction
class ParagraphStyleAction(
    val paragraphIndex: Int,
    val oldStyleName: String?,
    val newStyleName: String?,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Change Paragraph Style to ${newStyleName ?: "Normal"}",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "style",
    override val commandType: String = "PARAGRAPH_STYLE"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(styleName = oldStyleName))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(styleName = newStyleName))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 5. CharacterStyleAction
class CharacterStyleAction(
    val paragraphIndex: Int,
    val runIndex: Int,
    val oldStyleName: String?,
    val newStyleName: String?,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Change Character Style to ${newStyleName ?: "Default"}",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "format_size",
    override val commandType: String = "CHARACTER_STYLE"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val updatedRuns = oldPara.runs.mapIndexed { idx, run ->
                if (idx == runIndex) run.copy(styleName = oldStyleName) else run
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(runs = updatedRuns))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            val updatedRuns = oldPara.runs.mapIndexed { idx, run ->
                if (idx == runIndex) run.copy(styleName = newStyleName) else run
            }
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(runs = updatedRuns))
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 6. InsertImageAction
class InsertImageAction(
    val insertIndex: Int,
    val image: OfficeImage,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Insert Image",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "image",
    override val commandType: String = "INSERT_IMAGE"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = insertIndex.coerceIn(0, (elements.size - 1).coerceAtLeast(0))
        if (safeIdx in elements.indices) {
            elements.removeAt(safeIdx)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = insertIndex.coerceIn(0, elements.size)
        elements.add(safeIdx, OfficeDocElement.ImageElement(image))
        updateDocument(doc.copy(body = DocumentBody(elements)))
    }
}

// 7. InsertTableAction
class InsertTableAction(
    val insertIndex: Int,
    val table: OfficeTable,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Insert Table (${table.rows.size}x${table.numColumns})",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "grid_on",
    override val commandType: String = "INSERT_TABLE"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = insertIndex.coerceIn(0, (elements.size - 1).coerceAtLeast(0))
        if (safeIdx in elements.indices) {
            elements.removeAt(safeIdx)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = insertIndex.coerceIn(0, elements.size)
        elements.add(safeIdx, OfficeDocElement.TableElement(table))
        updateDocument(doc.copy(body = DocumentBody(elements)))
    }
}

// 8. DeleteTableAction
class DeleteTableAction(
    val tableIndex: Int,
    val table: OfficeTable,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Delete Table",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "grid_off",
    override val commandType: String = "DELETE_TABLE"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = tableIndex.coerceIn(0, elements.size)
        elements.add(safeIdx, OfficeDocElement.TableElement(table))
        updateDocument(doc.copy(body = DocumentBody(elements)))
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val safeIdx = tableIndex.coerceIn(0, (elements.size - 1).coerceAtLeast(0))
        if (safeIdx in elements.indices) {
            elements.removeAt(safeIdx)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 9. MoveObjectAction
class MoveObjectAction(
    val objectId: String,
    val oldX: Float,
    val oldY: Float,
    val newX: Float,
    val newY: Float,
    private val onMove: (id: String, x: Float, y: Float) -> Unit,
    override val title: String = "Move Object",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "open_with",
    override val commandType: String = "MOVE_OBJECT"
) : UndoAction {
    override suspend fun undo() {
        onMove(objectId, oldX, oldY)
    }

    override suspend fun redo() {
        onMove(objectId, newX, newY)
    }
}

// 10. SplitParagraphAction
class SplitParagraphAction(
    val paragraphIndex: Int,
    val leftText: String,
    val rightText: String,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Split Paragraph",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "call_split",
    override val commandType: String = "SPLIT_PARAGRAPH"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        if (paragraphIndex in elements.indices && paragraphIndex + 1 in elements.indices) {
            val leftElement = elements[paragraphIndex]
            val rightElement = elements[paragraphIndex + 1]
            val leftPara = leftElement.extractParagraph()
            val rightPara = rightElement.extractParagraph()
            if (leftPara != null && rightPara != null) {
                val joinedText = leftPara.text + rightPara.text
                elements[paragraphIndex] = leftElement.replaceParagraph(leftPara.copy(text = joinedText))
                elements.removeAt(paragraphIndex + 1)
                updateDocument(doc.copy(body = DocumentBody(elements)))
            }
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val element = elements.getOrNull(paragraphIndex)
        val oldPara = element?.extractParagraph()
        if (oldPara != null) {
            elements[paragraphIndex] = element.replaceParagraph(oldPara.copy(text = leftText))
            val newRight = if (element is OfficeDocElement.ParagraphElement) {
                OfficeDocElement.ParagraphElement(OfficeParagraph(text = rightText, styleName = oldPara.styleName))
            } else {
                OfficeParagraph(text = rightText, styleName = oldPara.styleName)
            }
            elements.add(paragraphIndex + 1, newRight)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 11. MergeParagraphAction
class MergeParagraphAction(
    val targetIdx: Int,
    val sourceIdx: Int,
    val targetText: String,
    val sourceText: String,
    private val getDocument: () -> OfficeDocument,
    private val updateDocument: (OfficeDocument) -> Unit,
    override val title: String = "Merge Paragraphs",
    override val timestamp: Long = System.currentTimeMillis(),
    override val icon: String = "merge_type",
    override val commandType: String = "MERGE_PARAGRAPH"
) : UndoAction {
    override suspend fun undo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val targetElem = elements.getOrNull(targetIdx)
        val targetPara = targetElem?.extractParagraph()
        if (targetPara != null) {
            elements[targetIdx] = targetElem.replaceParagraph(targetPara.copy(text = targetText))
            val newSource = if (targetElem is OfficeDocElement.ParagraphElement) {
                OfficeDocElement.ParagraphElement(OfficeParagraph(text = sourceText))
            } else {
                OfficeParagraph(text = sourceText)
            }
            elements.add(sourceIdx, newSource)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }

    override suspend fun redo() {
        val doc = getDocument()
        val elements = doc.body.elements.toMutableList()
        val targetElem = elements.getOrNull(targetIdx)
        val targetPara = targetElem?.extractParagraph()
        if (targetPara != null) {
            elements[targetIdx] = targetElem.replaceParagraph(targetPara.copy(text = targetText + sourceText))
            elements.removeAt(sourceIdx)
            updateDocument(doc.copy(body = DocumentBody(elements)))
        }
    }
}

// 12. CompoundUndoAction (Transaction wrapper for Undo Context)
class CompoundUndoAction(
    override val title: String,
    override val icon: String = "layers",
    override val commandType: String = "TRANSACTION",
    val actions: List<UndoAction>,
    override val timestamp: Long = System.currentTimeMillis()
) : UndoAction {
    override suspend fun undo() {
        for (i in actions.indices.reversed()) {
            actions[i].undo()
        }
    }

    override suspend fun redo() {
        for (action in actions) {
            action.redo()
        }
    }
}

