package com.makerandreas.papirusoffice.data.framework

/**
 * Event object for documents.
 * Matches com.sun.star.document.EventObject
 */
data class DocumentEventObject(
    val source: Any,
    val eventName: String
)

/**
 * Listener for document events.
 * Matches com.sun.star.document.XEventListener
 */
interface XDocumentEventListener : XEventListener {
    fun notifyEvent(event: DocumentEventObject)
}

/**
 * Broadcaster for document events.
 * Matches com.sun.star.document.XEventBroadcaster
 */
interface XEventBroadcaster {
    fun addEventListener(listener: XDocumentEventListener)
    fun removeEventListener(listener: XDocumentEventListener)
}

/**
 * Standard document event names
 * Matches com.sun.star.document.Events
 */
object DocumentEvents {
    const val ON_NEW = "OnNew"
    const val ON_LOAD = "OnLoad"
    const val ON_SAVE_AS = "OnSaveAs"
    const val ON_SAVE_AS_DONE = "OnSaveAsDone"
    const val ON_SAVE = "OnSave"
    const val ON_SAVE_DONE = "OnSaveDone"
    const val ON_PREPARE_UNLOAD = "OnPrepareUnload"
    const val ON_UNLOAD = "OnUnload"
    const val ON_FOCUS = "OnFocus"
    const val ON_UNFOCUS = "OnUnfocus"
    const val ON_PRINT = "OnPrint"
    const val ON_MODIFY_CHANGE = "OnModifyChange"
}
