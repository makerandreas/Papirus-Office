package com.example.core.jni

import android.util.Log

/**
 * JNI Bridge for LibreOffice core and the C++ OOXML compatibility engine.
 * Governed by build-time flags in BuildConfig.
 */
object LibreOfficeCore {
    private const val TAG = "LibreOfficeCore"
    private var isLibraryLoaded = false

    /**
     * True when a real native library was found (Phase 2 drop-in present).
     * Until then every call below runs its JVM fallback — see [LokitEngine].
     */
    val isNativeLibraryLoaded: Boolean
        get() = isLibraryLoaded

    /** Soname load order for a stock LibreOffice-Android build (single .so + NSS deps). */
    private val LO_NATIVE_LOAD_ORDER = listOf(
        "nspr4", "plds4", "plc4", "nssutil3", "freebl3", "sqlite3",
        "softokn3", "nss3", "nssckbi", "nssdbm3", "smime3", "ssl3",
        "c++_shared", "lo-native-code"
    )

    // Load native libraries if available. In prototype mode, we fail gracefully.
    init {
        isLibraryLoaded = tryLoadNative()
        if (isLibraryLoaded) {
            Log.i(TAG, "Native LibreOffice library loaded successfully.")
        } else {
            Log.w(TAG, "No native library found (tried lo-native-code chain + libreoffice-core). Running simulated/JVM fallback mode.")
        }
    }

    /**
     * Phase 2 drop-in probe: stock `liblo-native-code.so` first (with its
     * dependency chain), then the legacy `liblibreoffice-core.so` custom name.
     * Pure probe — any UnsatisfiedLinkError means "simulated mode".
     */
    private fun tryLoadNative(): Boolean {
        try {
            LO_NATIVE_LOAD_ORDER.forEach { System.loadLibrary(it) }
            return true
        } catch (ignored: UnsatisfiedLinkError) {
            // Fall through to the legacy/custom soname.
        }
        return try {
            System.loadLibrary("libreoffice-core")
            true
        } catch (ignored: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Initialize the LibreOffice Core engine with optional OOXML compat configurations.
     */
    fun initialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean): Boolean {
        Log.d(TAG, "Initializing LibreOffice Core JNI. cacheDir=$cacheDir, enableOoxml=$enableOoxml, enableOmml=$enableOmml")
        if (!isLibraryLoaded) {
            Log.w(TAG, "Native library not loaded. Running JVM mock setup.")
            return true
        }
        return try {
            nativeInitialize(cacheDir, enableOoxml, enableOmml)
        } catch (e: UnsatisfiedLinkError) {
            // Fallback mock logic for testing/prototyping without dynamic native binary compilation
            Log.w(TAG, "nativeInitialize UnsatisfiedLinkError, running JVM mock setup")
            true
        }
    }

    /**
     * Render a document page directly into a bitmap or byte array buffer.
     * Used for rendering ODF, DOCX, XLSX, PPTX, and PDF pages in Compose.
     */
    fun renderPageToBuffer(docPath: String, pageIndex: Int, outputBuffer: ByteArray, width: Int, height: Int): Boolean {
        Log.d(TAG, "Rendering page $pageIndex of $docPath to native buffer (${width}x${height})")
        if (!isLibraryLoaded) {
            Log.w(TAG, "Native library not loaded. Running fallback rendering simulation.")
            return true
        }
        return try {
            nativeRenderPage(docPath, pageIndex, outputBuffer, width, height)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeRenderPage UnsatisfiedLinkError, running fallback rendering simulation")
            true
        }
    }

    /**
     * Interface for LibreOffice core events.
     */
    interface DocumentCallback {
        fun onEvent(type: Int, payload: String)
    }

    private var currentCallback: DocumentCallback? = null

    /**
     * Register a callback for LibreOffice events (e.g. invalidate tiles).
     */
    fun registerCallback(docId: Int, callback: DocumentCallback) {
        currentCallback = callback
        if (!isLibraryLoaded) {
            Log.w(TAG, "Native library not loaded. Mocking callback registration.")
            return
        }
        try {
            nativeRegisterCallback(docId, callback)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeRegisterCallback UnsatisfiedLinkError.")
        }
    }

    /**
     * Parse and export native spreadsheet calculations.
     */
    fun evaluateFormula(formula: String, sheetDataJson: String): String {
        if (!isLibraryLoaded) {
            return "MOCK_RESULT_FOR($formula)"
        }
        return try {
            nativeEvaluateFormula(formula, sheetDataJson)
        } catch (e: UnsatisfiedLinkError) {
            "MOCK_RESULT_FOR($formula)"
        }
    }

    /**
     * Create a new document in LibreOffice.
     * @return A document ID, or a status indicating success.
     */
    fun createDocument(fileName: String): Int {
        Log.d(TAG, "Creating new document: $fileName")
        if (!isLibraryLoaded) {
            Log.w(TAG, "Native library not loaded. Mocking createDocument.")
            return 1 // Mock success
        }
        return try {
            nativeCreateDocument(fileName)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeCreateDocument UnsatisfiedLinkError.")
            1 // Mock success
        }
    }

    // --- Native Methods ---
    private external fun nativeInitialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean): Boolean
    private external fun nativeRenderPage(docPath: String, pageIndex: Int, buffer: ByteArray, w: Int, h: Int): Boolean
    private external fun nativeEvaluateFormula(formula: String, sheetDataJson: String): String
    private external fun nativeRegisterCallback(docId: Int, callback: DocumentCallback)
    private external fun nativeCreateDocument(fileName: String): Int
}
