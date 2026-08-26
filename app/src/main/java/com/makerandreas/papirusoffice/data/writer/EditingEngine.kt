package com.makerandreas.papirusoffice.data.writer

import com.makerandreas.papirusoffice.data.PapirusLogger
import com.makerandreas.papirusoffice.data.SessionManager
import com.makerandreas.papirusoffice.data.writer.commands.DeleteSelectionCommand
import com.makerandreas.papirusoffice.data.writer.commands.DocumentCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditingEngine(
    private val getSession: () -> com.makerandreas.papirusoffice.data.DocumentSession? = {
        SessionManager.getInstance().current.value
    },
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    fun execute(command: DocumentCommand) {
        val session = getSession() ?: return
        val updated = command.execute(session.document)
        session.document = updated
        session.dirty = true
        if (command is com.makerandreas.papirusoffice.data.undo.UndoAction) {
            coroutineScope.launch {
                session.undoManager.recordAction(command)
            }
        }
    }

    suspend fun executeSuspend(command: DocumentCommand) {
        val session = getSession() ?: return
        val updated = command.execute(session.document)
        session.document = updated
        session.dirty = true
        if (command is com.makerandreas.papirusoffice.data.undo.UndoAction) {
            session.undoManager.recordAction(command)
        }
    }

    fun deleteSelection(
        selection: SelectionRange,
        fullText: String,
        onApply: (newText: String, newSelection: SelectionRange) -> Unit
    ): String {
        PapirusLogger.d("UNDO", "DeleteSelectionCommand")
        val deleted = SelectionEngine.extract(fullText, selection)
        val newText = SelectionEngine.delete(fullText, selection)
        val newSelection = SelectionRange(selection.min, selection.min)

        val command = DeleteSelectionCommand(
            selection = selection,
            deletedText = deleted,
            fullOriginalText = fullText,
            onApply = onApply
        )

        val session = getSession()
        if (session != null) {
            val updatedDoc = command.execute(session.document)
            session.document = updatedDoc
            session.dirty = true
            coroutineScope.launch {
                session.undoManager.recordAction(command)
            }
        }

        onApply(newText, newSelection)
        return newText
    }
}
