package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.navigation.NavigationEngine
import com.makerandreas.papirusoffice.data.undo.UndoManager
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DocumentSession(
    val id: UUID = UUID.randomUUID(),
    val engine: DocumentEngine,
    var document: OfficeDocument,
    var file: OfficeFile?,
    dirty: Boolean = false,
    var protected: Boolean = false,
    var readOnly: Boolean = false,
    val undoManager: UndoManager = UndoManager(sessionIdProvider = { id.toString() }),
    val parserReport: ParserReport = ParserReport(),
    val navigationEngine: NavigationEngine = NavigationEngine(document)
) {
    /**
     * Notifies [SessionManager] on every transition so UI collecting
     * [SessionManager.dirtyRevision] recomposes. Direct assignment is the only
     * mutation path (see EditingEngine / DocumentLifecycleManager), which is
     * why the notification lives in the setter instead of each call site.
     */
    var dirty: Boolean = dirty
        set(value) {
            if (field != value) {
                field = value
                SessionManager.getInstance().notifyDirtyChanged()
            }
        }
}

class SessionManager private constructor() {
    private val _current = MutableStateFlow<DocumentSession?>(null)
    val current = _current.asStateFlow()

    /**
     * Bumped on every dirty-flag change. Reassigning the same session instance
     * to [_current] would NOT emit (StateFlow conflates equal references), so
     * UI must also collect this to observe dirty transitions.
     */
    private val _dirtyRevision = MutableStateFlow(0L)
    val dirtyRevision = _dirtyRevision.asStateFlow()

    fun setCurrentSession(session: DocumentSession?) {
        _current.value = session
    }

    /** Bumps [dirtyRevision]; called by the [DocumentSession.dirty] setter. */
    fun notifyDirtyChanged() {
        _dirtyRevision.value = _dirtyRevision.value + 1
    }

    fun markCurrentSessionDirty(isDirty: Boolean) {
        // The dirty setter notifies dirtyRevision; no explicit bump needed.
        _current.value?.dirty = isDirty
    }

    companion object {
        @Volatile
        private var instance: SessionManager? = null

        fun getInstance(): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager().also { instance = it }
            }
        }
    }
}
