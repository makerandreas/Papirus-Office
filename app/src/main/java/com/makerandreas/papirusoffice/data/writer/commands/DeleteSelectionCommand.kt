package com.makerandreas.papirusoffice.data.writer.commands

import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.PapirusLogger
import com.makerandreas.papirusoffice.data.undo.UndoAction
import com.makerandreas.papirusoffice.data.writer.SelectionEngine
import com.makerandreas.papirusoffice.data.writer.SelectionRange

interface DocumentCommand {
    fun execute(doc: OfficeDocument): OfficeDocument
    fun undo(doc: OfficeDocument): OfficeDocument
}

class DeleteSelectionCommand(
    val selection: SelectionRange,
    deletedText: String = "",
    val fullOriginalText: String = "",
    private val onApply: ((String, SelectionRange) -> Unit)? = null
) : DocumentCommand, UndoAction {

    var actualDeletedText: String = deletedText
        private set

    override val title: String get() = "Delete Selection"
    override val timestamp: Long = System.currentTimeMillis()
    override val icon: String = "backspace"
    override val commandType: String = "DELETE_SELECTION"

    override fun execute(doc: OfficeDocument): OfficeDocument {
        PapirusLogger.d("UNDO", "DeleteSelectionCommand execute")
        if (actualDeletedText.isEmpty()) {
            actualDeletedText = SelectionEngine.extract(doc, selection)
        }
        return SelectionEngine.delete(doc, selection)
    }

    override fun undo(doc: OfficeDocument): OfficeDocument {
        PapirusLogger.d("UNDO", "DeleteSelectionCommand undo doc")
        return SelectionEngine.insert(doc, selection.min, actualDeletedText)
    }

    override suspend fun undo() {
        PapirusLogger.d("UNDO", "DeleteSelectionCommand undo")
        if (fullOriginalText.isNotEmpty()) {
            onApply?.invoke(fullOriginalText, selection)
        } else {
            val restored = actualDeletedText
            onApply?.invoke(restored, selection)
        }
    }

    override suspend fun redo() {
        PapirusLogger.d("UNDO", "DeleteSelectionCommand redo")
        val postDeleteText = if (fullOriginalText.isNotEmpty()) {
            SelectionEngine.delete(fullOriginalText, selection)
        } else {
            ""
        }
        onApply?.invoke(postDeleteText, SelectionRange(selection.min, selection.min))
    }
}
