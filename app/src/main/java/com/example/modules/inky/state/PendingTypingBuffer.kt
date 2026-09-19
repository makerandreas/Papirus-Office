// SPDX-License-Identifier: MPL-2.0
package com.example.modules.inky.state

import androidx.compose.ui.text.TextRange
import com.makerandreas.papirusoffice.data.writer.EditingEngine
import com.makerandreas.papirusoffice.data.writer.SelectionRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns Inky's typing-debounce -> baseline-commit protocol.
 *
 * The editor keeps two versions of the truth: the live text shown in the
 * field and the last baseline committed to [com.makerandreas.papirusoffice.data.undo.UndoManager].
 * Keystrokes arrive continuously but are only recorded after a quiet window
 * ([debounceMs]); any destructive action (select-all Delete, cut, undo/redo
 * navigation) MUST synchronously commit ("flush") the pending typing first,
 * otherwise the destructive action records against a stale baseline and a
 * later Undo resurrects the wrong text (the dropped-second-line race).
 *
 * Centralizing the sequence here — instead of scattering
 * cancel-then-flush-then-act across click handlers — makes the invariant
 * impossible to skip: [deleteSelection] always flushes before deleting.
 * The debounce window and all state access are injected so the protocol is
 * deterministically unit-testable (see `DocumentEnginesUnitTest`).
 */
class PendingTypingBuffer(
    private val scope: CoroutineScope,
    private val debounceMs: Long = 800,
    private val getLiveText: () -> String,
    private val getBaseline: () -> String,
    private val setBaseline: (String) -> Unit,
    private val getSelection: () -> TextRange,
    private val recordCommittedTyping: suspend (oldValue: String, newValue: String, selection: TextRange) -> Unit
) {
    private var pendingJob: Job? = null

    /**
     * Continuous typing: restart the debounce window. When the user pauses
     * for [debounceMs], whatever is live at fire time gets committed.
     */
    fun onTextChanged() {
        pendingJob?.cancel()
        pendingJob = scope.launch {
            delay(debounceMs)
            commit(getLiveText())
        }
    }

    /** Drop a scheduled auto-commit without committing (mode switches etc.). */
    fun cancelPending() {
        pendingJob?.cancel()
        pendingJob = null
    }

    /**
     * Synchronously commit pending typing up to [liveText].
     * @return true if a commit was recorded, false if already in sync.
     */
    suspend fun flush(liveText: String = getLiveText()): Boolean {
        cancelPending()
        return commit(liveText)
    }

    /**
     * Owned flush-then-delete sequence for select-all Delete / cut paths.
     * Removing the flush from this function breaks
     * `testTwoLinesTypingSelectionDeletionAndUndo` by design.
     */
    suspend fun deleteSelection(
        selection: SelectionRange,
        fullText: String,
        engine: EditingEngine,
        onApply: (newText: String, newSelection: SelectionRange) -> Unit
    ): String {
        flush(fullText)
        return engine.deleteSelection(selection, fullText, onApply)
    }

    private suspend fun commit(liveText: String): Boolean {
        val oldValue = getBaseline()
        if (liveText == oldValue) return false
        recordCommittedTyping(oldValue, liveText, getSelection())
        setBaseline(liveText)
        return true
    }
}
