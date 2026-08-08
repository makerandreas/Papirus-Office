package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import com.example.ui.home.RecentFilesTracker
import com.makerandreas.papirusoffice.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

sealed class CloseResult {
    object Success : CloseResult()
    data class HasUnsavedChanges(val session: DocumentSession) : CloseResult()
}

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

            // Invalidate cache first so reload reads fresh data from disk!
            val cacheRepo = com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository(context)
            cacheRepo.invalidateCache(fileObj)

            // Read and parse document bypassing cache
            val parser = DocxDocumentParser(context)
            val parseResult = parser.parseDocument(fileObj, bypassCache = true)

            val officeDoc = parseResult.parsedDocument?.toOfficeDocument() ?: session.document
            session.document = officeDoc
            session.dirty = false

            // Replace current active session
            SessionManager.getInstance().setCurrentSession(session)

            // Register recent files projection
            registerRecentDocument(context, session)

            // Emit DocumentReloaded event
            DocumentEventBus.emit(
                PapirusDocumentEvent.DocumentReloaded(
                    sessionId = session.id,
                    title = session.file?.displayName ?: fileObj.name,
                    filePath = session.file?.uri ?: fileObj.absolutePath
                )
            )

            return parseResult
        } finally {
            DocumentLockManager.unlock(session.id)
        }
    }

    suspend fun closeSession(
        session: DocumentSession,
        force: Boolean = false
    ): CloseResult {
        if (session.dirty && !force) {
            return CloseResult.HasUnsavedChanges(session)
        }

        // Clear Undo manager
        session.undoManager.clear()

        // Emit DocumentClosed event
        DocumentEventBus.emit(
            PapirusDocumentEvent.DocumentClosed(
                sessionId = session.id,
                title = session.file?.displayName ?: session.file?.file?.name ?: "Unknown"
            )
        )

        // Reset current active session if it matches closed session
        if (SessionManager.getInstance().current.value?.id == session.id) {
            SessionManager.getInstance().setCurrentSession(null)
        }

        return CloseResult.Success
    }

    fun close(session: DocumentSession) {
        // Clear Undo manager
        session.undoManager.clear()

        // Emit DocumentClosed event
        DocumentEventBus.emit(
            PapirusDocumentEvent.DocumentClosed(
                sessionId = session.id,
                title = session.file?.displayName ?: session.file?.file?.name ?: "Unknown"
            )
        )

        // Reset current active session
        SessionManager.getInstance().setCurrentSession(null)
    }

    fun registerRecentDocument(context: Context, session: DocumentSession) {
        val path = session.file?.uri ?: session.file?.file?.absolutePath ?: return
        val name = session.file?.displayName ?: session.file?.file?.name ?: "Document.odt"
        val fileType = if (name.endsWith(".docx", ignoreCase = true)) "DOCX" else "Inky"
        RecentFilesTracker.addFile(context, path, fileType)
    }
}
