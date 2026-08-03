package com.makerandreas.papirusoffice.data.undo

import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val icon: String = "edit",
    val commandType: String = "GENERAL"
)

/**
 * Phase 4: HistoryManager
 * Exposes reactive undo & redo histories to Compose and external consumers.
 */
class HistoryManager {

    private val _undoHistory = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val undoHistory: StateFlow<List<HistoryEntry>> = _undoHistory.asStateFlow()

    private val _redoHistory = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val redoHistory: StateFlow<List<HistoryEntry>> = _redoHistory.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _lastActionTitle = MutableStateFlow<String?>(null)
    val lastActionTitle: StateFlow<String?> = _lastActionTitle.asStateFlow()

    fun onActionRecorded(action: UndoAction) {
        val entry = HistoryEntry(
            title = action.title,
            timestamp = action.timestamp,
            icon = action.icon,
            commandType = action.commandType
        )
        val currentUndo = _undoHistory.value.toMutableList()
        currentUndo.add(entry)
        _undoHistory.value = currentUndo
        _redoHistory.value = emptyList() // LibreOffice clears redo on new action
        updateStates()
    }

    fun onUndoPerformed(): HistoryEntry? {
        val currentUndo = _undoHistory.value.toMutableList()
        if (currentUndo.isEmpty()) return null
        val popped = currentUndo.removeAt(currentUndo.size - 1)
        _undoHistory.value = currentUndo

        val currentRedo = _redoHistory.value.toMutableList()
        currentRedo.add(popped)
        _redoHistory.value = currentRedo

        updateStates()
        return popped
    }

    fun onRedoPerformed(): HistoryEntry? {
        val currentRedo = _redoHistory.value.toMutableList()
        if (currentRedo.isEmpty()) return null
        val popped = currentRedo.removeAt(currentRedo.size - 1)
        _redoHistory.value = currentRedo

        val currentUndo = _undoHistory.value.toMutableList()
        currentUndo.add(popped)
        _undoHistory.value = currentUndo

        updateStates()
        return popped
    }

    fun clear() {
        _undoHistory.value = emptyList()
        _redoHistory.value = emptyList()
        updateStates()
    }

    private fun updateStates() {
        val undoList = _undoHistory.value
        val redoList = _redoHistory.value
        _canUndo.value = undoList.isNotEmpty()
        _canRedo.value = redoList.isNotEmpty()
        _lastActionTitle.value = undoList.lastOrNull()?.title
    }
}
