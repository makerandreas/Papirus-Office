package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.navigation.NavigationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class OfficeFile(val file: File) {
    val name: String get() = file.name
    val path: String get() = file.absolutePath
}

class DocumentSession(
    val engine: DocumentEngine = DocumentEngine(),
    var document: OfficeDocument,
    val file: OfficeFile? = null
) {
    val undoManager = UndoManager()
    val navigationEngine = NavigationEngine()
    var dirty: Boolean = false

    fun markDirty() {
        dirty = true
        SessionManager.getInstance().notifyDirtyChanged()
    }

    fun markClean() {
        dirty = false
        SessionManager.getInstance().notifyDirtyChanged()
    }
}

class SessionManager private constructor() {
    private val _current = MutableStateFlow<DocumentSession?>(null)
    val current: StateFlow<DocumentSession?> = _current.asStateFlow()

    private val _dirtyRevision = MutableStateFlow(0)
    val dirtyRevision: StateFlow<Int> = _dirtyRevision.asStateFlow()

    fun setCurrentSession(session: DocumentSession?) {
        _current.value = session
        _dirtyRevision.value++
    }

    fun notifyDirtyChanged() {
        _dirtyRevision.value++
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
