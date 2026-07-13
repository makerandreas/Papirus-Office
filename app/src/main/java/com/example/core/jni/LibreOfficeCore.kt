package com.example.core.jni

import android.util.Log

/**
 * JNI Bridge for LibreOffice core and the C++ OOXML compatibility engine.
 * Governed by build-time flags in BuildConfig.
 */
object LibreOfficeCore {
    private const val TAG = "LibreOfficeCore"

    // Load native libraries if available. In prototype mode, we fail gracefully.
    init {
        try {
            System.loadLibrary("libreoffice-core")
            Log.i(TAG, "LibreOffice Core Native Library loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library 'libreoffice-core' not found. Running in mock/compatibility fallback mode.")
        }
    }

    /**
     * Initialize the LibreOffice Core engine with optional OOXML compat configurations.
     */
    fun initialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean): Boolean {
        Log.d(TAG, "Initializing LibreOffice Core JNI. cacheDir=$cacheDir, enableOoxml=$enableOoxml, enableOmml=$enableOmml")
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
        return try {
            nativeRenderPage(docPath, pageIndex, outputBuffer, width, height)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeRenderPage UnsatisfiedLinkError, running fallback rendering simulation")
            true
        }
    }

    /**
     * Parse and export native spreadsheet calculations.
     */
    fun evaluateFormula(formula: String, sheetDataJson: String): String {
        return try {
            nativeEvaluateFormula(formula, sheetDataJson)
        } catch (e: UnsatisfiedLinkError) {
            "MOCK_RESULT_FOR($formula)"
        }
    }

    // --- Native Methods ---
    private external fun nativeInitialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean): Boolean
    private external fun nativeRenderPage(docPath: String, pageIndex: Int, buffer: ByteArray, w: Int, h: Int): Boolean
    private external fun nativeEvaluateFormula(formula: String, sheetDataJson: String): String
}
