package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import com.makerandreas.papirusoffice.data.*
import java.io.File

object DocumentLifecycleManager {

    suspend fun reload(
        context: Context,
        session: DocumentSession
    ): DocxParseResult? {
        val fileObj = session.file?.file ?: return null
        if (!fileObj.exists()) return null

        // Lock document during reload
        DocumentLockManager.lock(session.id, DocumentLockMode.IN_USE)

        try {
            // Clear Undo and Redo manager
            session.undoManager.clear()

            // Read and parse document
            val parser = DocxDocumentParser(context)
            val parseResult = parser.parseDocument(fileObj)

            val officeDoc = parseResult.parsedDocument?.toOfficeDocument() ?: session.document
            session.document = officeDoc
            session.dirty = false

            // Replace current active session
            SessionManager.getInstance().setCurrentSession(session)

            // Emit DocumentReloaded event
            DocumentEventBus.emit(
                PapirusDocumentEvent.DocumentReloaded(
                    sessionId = session.id,
                    title = fileObj.name,
                    filePath = fileObj.absolutePath
                )
            )

            return parseResult
        } finally {
            DocumentLockManager.unlock(session.id)
        }
    }

    fun close(session: DocumentSession) {
        // Clear Undo manager
        session.undoManager.clear()

        // Emit DocumentClosed event
        DocumentEventBus.emit(
            PapirusDocumentEvent.DocumentClosed(
                sessionId = session.id,
                title = session.file?.file?.name ?: "Unknown"
            )
        )

        // Reset current active session
        SessionManager.getInstance().setCurrentSession(null)
    }
}
