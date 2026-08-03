package com.makerandreas.papirusoffice.data.undo

import com.makerandreas.papirusoffice.data.PapirusLogger

/**
 * Phase 2, 3, 5, 6, 9 & 10: Complete UndoManager implementation
 * Manages Undo/Redo stacks, Undo Contexts (Transactions), Multi-Undo/Redo,
 * HistoryManager reactive state, and PapirusLogger foundation logging.
 */
class UndoManager(
    val historyManager: HistoryManager = HistoryManager(),
    private val maxStackSize: Int = 100,
    private val sessionIdProvider: () -> String = { "inky-default" }
) {
    private val undoStack: ArrayDeque<UndoAction> = ArrayDeque()
    private val redoStack: ArrayDeque<UndoAction> = ArrayDeque()

    // Phase 3: Transaction buffer for Undo Context
    private var activeTransactionTitle: String? = null
    private var activeTransactionIcon: String = "edit"
    private var activeTransactionType: String = "TRANSACTION"
    private val transactionBuffer = mutableListOf<UndoAction>()

    suspend fun recordAction(action: UndoAction) {
        if (activeTransactionTitle != null) {
            transactionBuffer.add(action)
            return
        }

        if (undoStack.size >= maxStackSize) {
            undoStack.removeFirst()
        }
        undoStack.addLast(action)
        redoStack.clear()

        historyManager.onActionRecorded(action)

        PapirusLogger.i(
            "UndoEngine",
            "[RecordAction] Title = \"${action.title}\", Session = ${sessionIdProvider()}, Timestamp = ${action.timestamp}"
        )
    }

    // Phase 3: Undo Context APIs
    fun beginTransaction(
        title: String,
        icon: String = "edit",
        commandType: String = "TYPING"
    ) {
        activeTransactionTitle = title
        activeTransactionIcon = icon
        activeTransactionType = commandType
        transactionBuffer.clear()
        PapirusLogger.d("UndoEngine", "[BeginTransaction] Title = \"$title\", Session = ${sessionIdProvider()}")
    }

    suspend fun commitTransaction() {
        val title = activeTransactionTitle ?: return
        val actionsToCommit = transactionBuffer.toList()
        activeTransactionTitle = null
        transactionBuffer.clear()

        if (actionsToCommit.isEmpty()) return

        val finalAction: UndoAction = if (actionsToCommit.size == 1) {
            actionsToCommit[0]
        } else {
            CompoundUndoAction(
                title = title,
                icon = activeTransactionIcon,
                commandType = activeTransactionType,
                actions = actionsToCommit
            )
        }

        recordAction(finalAction)
        PapirusLogger.i("UndoEngine", "[CommitTransaction] Title = \"$title\", ActionsCount = ${actionsToCommit.size}, Session = ${sessionIdProvider()}")
    }

    suspend fun rollbackTransaction() {
        val actionsToRollback = transactionBuffer.toList()
        activeTransactionTitle = null
        transactionBuffer.clear()

        for (i in actionsToRollback.indices.reversed()) {
            actionsToRollback[i].undo()
        }
        PapirusLogger.w("UndoEngine", "[RollbackTransaction] ActionsCount = ${actionsToRollback.size}, Session = ${sessionIdProvider()}")
    }

    suspend fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val action = undoStack.removeLast()
        action.undo()
        redoStack.addLast(action)

        val entry = historyManager.onUndoPerformed()
        PapirusLogger.i(
            "UndoEngine",
            "[Undo] Title = \"${entry?.title ?: action.title}\", Session = ${sessionIdProvider()}, Timestamp = ${System.currentTimeMillis()}"
        )
        return true
    }

    suspend fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val action = redoStack.removeLast()
        action.redo()
        undoStack.addLast(action)

        val entry = historyManager.onRedoPerformed()
        PapirusLogger.i(
            "UndoEngine",
            "[Redo] Title = \"${entry?.title ?: action.title}\", Session = ${sessionIdProvider()}, Timestamp = ${System.currentTimeMillis()}"
        )
        return true
    }

    // Phase 5: Multi Undo
    suspend fun undoMultiple(count: Int): Int {
        var performed = 0
        repeat(count) {
            if (undo()) {
                performed++
            }
        }
        PapirusLogger.i("UndoEngine", "[MultiUndo] Requested = $count, Executed = $performed, Session = ${sessionIdProvider()}")
        return performed
    }

    suspend fun undoTo(entry: HistoryEntry): Int {
        val historyList = historyManager.undoHistory.value
        val index = historyList.indexOfFirst { it.id == entry.id }
        if (index == -1) return 0
        val stepsToUndo = historyList.size - index
        return undoMultiple(stepsToUndo)
    }

    // Phase 6: Multi Redo
    suspend fun redoMultiple(count: Int): Int {
        var performed = 0
        repeat(count) {
            if (redo()) {
                performed++
            }
        }
        PapirusLogger.i("UndoEngine", "[MultiRedo] Requested = $count, Executed = $performed, Session = ${sessionIdProvider()}")
        return performed
    }

    suspend fun redoTo(entry: HistoryEntry): Int {
        val historyList = historyManager.redoHistory.value
        val index = historyList.indexOfFirst { it.id == entry.id }
        if (index == -1) return 0
        val stepsToRedo = historyList.size - index
        return redoMultiple(stepsToRedo)
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun getUndoStackSize(): Int = undoStack.size
    fun getRedoStackSize(): Int = redoStack.size

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        transactionBuffer.clear()
        activeTransactionTitle = null
        historyManager.clear()
        PapirusLogger.i("UndoEngine", "[Clear] Stacks cleared for Session = ${sessionIdProvider()}")
    }
}
