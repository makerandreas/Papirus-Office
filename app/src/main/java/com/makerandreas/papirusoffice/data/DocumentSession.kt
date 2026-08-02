package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.navigation.NavigationEngine
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DocumentSession(
    val id: UUID = UUID.randomUUID(),
    val engine: DocumentEngine,
    var document: OfficeDocument,
    var file: OfficeFile?,
    var dirty: Boolean = false,
    var protected: Boolean = false,
    var readOnly: Boolean = false,
    val undoManager: UndoManager = UndoManager(),
    val parserReport: ParserReport = ParserReport(),
    val navigationEngine: NavigationEngine = NavigationEngine(document)
)

class SessionManager private constructor() {
    private val _current = MutableStateFlow<DocumentSession?>(null)
    val current = _current.asStateFlow()

    fun setCurrentSession(session: DocumentSession?) {
        _current.value = session
    }

    fun markCurrentSessionDirty(isDirty: Boolean) {
        _current.value?.let {
            it.dirty = isDirty
            // Trigger state change
            _current.value = it
        }
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
