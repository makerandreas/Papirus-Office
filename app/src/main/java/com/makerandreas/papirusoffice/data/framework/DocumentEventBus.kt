package com.makerandreas.papirusoffice.data.framework

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID

sealed class PapirusDocumentEvent {
    data class DocumentOpened(val sessionId: UUID, val title: String, val filePath: String?) : PapirusDocumentEvent()
    data class DocumentReloaded(val sessionId: UUID, val title: String, val filePath: String?) : PapirusDocumentEvent()
    data class DocumentSaved(val sessionId: UUID, val title: String, val filePath: String?) : PapirusDocumentEvent()
    data class DocumentClosed(val sessionId: UUID, val title: String) : PapirusDocumentEvent()
    data class DocumentModified(val sessionId: UUID, val isDirty: Boolean) : PapirusDocumentEvent()
}

object DocumentEventBus {
    private val _events = MutableSharedFlow<PapirusDocumentEvent>(extraBufferCapacity = 128)
    val events = _events.asSharedFlow()

    fun emit(event: PapirusDocumentEvent) {
        _events.tryEmit(event)
    }
}
