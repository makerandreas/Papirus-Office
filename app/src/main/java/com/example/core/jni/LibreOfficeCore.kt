package com.example.core.jni

import android.util.Log

object LibreOfficeCore {
    private const val TAG = "LibreOfficeCore"
    var isNativeLibraryLoaded: Boolean = false

    interface DocumentCallback {
        fun onEvent(type: Int, payload: String)
    }

    fun initialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean) {
        Log.d(TAG, "LibreOfficeCore initializing (simulated fallback)")
    }

    fun registerCallback(docId: Int, callback: DocumentCallback) {
    }

    fun createDocument(name: String): Long {
        return 0L
    }
}
