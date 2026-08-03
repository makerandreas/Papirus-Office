package com.makerandreas.papirusoffice.data.undo

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Phase 8: Keyboard Shortcut Handler
 * Direct keyboard processing for Ctrl + Z (Undo) and Ctrl + Y / Ctrl + Shift + Z (Redo)
 */
object KeyboardShortcutHandler {

    fun handleUndoRedoKeyEvent(
        event: KeyEvent,
        onUndo: () -> Unit,
        onRedo: () -> Unit
    ): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        if (!event.isCtrlPressed) return false

        return when (event.key) {
            Key.Z -> {
                onUndo()
                true
            }
            Key.Y -> {
                onRedo()
                true
            }
            else -> false
        }
    }
}

fun Modifier.undoRedoKeyboardShortcuts(
    undoManager: UndoManager,
    coroutineScope: CoroutineScope
): Modifier = this.onPreviewKeyEvent { event ->
    KeyboardShortcutHandler.handleUndoRedoKeyEvent(
        event = event,
        onUndo = {
            coroutineScope.launch { undoManager.undo() }
        },
        onRedo = {
            coroutineScope.launch { undoManager.redo() }
        }
    )
}
