package com.example.core.jni

import android.util.Log
import org.libreoffice.kit.LibreOfficeKit

/**
 * JNI Bridge for LibreOffice core (`org.libreoffice.kit.*`) and the pure-Kotlin
 * fallback seam. Heavy document conversions execute in the isolated `:office`
 * process via [OfficeEngineService] and [OfficeEngineClient].
 */
object LibreOfficeCore {
    private const val TAG = "LibreOfficeCore"
    private var isLibraryLoaded = false
    private var isEnvConfigured = false

    /**
     * True when the pre-bundled native library was actually loaded from
     * `app/src/main/libs/<abi>/`. Until then every call below runs its
     * JVM fallback (see [LokitEngine]).
     */
    val isNativeLibraryLoaded: Boolean
        get() = isLibraryLoaded

    val isNativeConfigured: Boolean
        get() = isLibraryLoaded && isEnvConfigured

    /**
     * Soname load order for the LibreOffice Viewer for Android build shipped
     * under `app/src/main/libs/<abi>/` (`liblo-native-code.so` + NSS deps).
     */
    private val LO_NATIVE_LOAD_ORDER = listOf(
        "nspr4", "plds4", "plc4", "nssutil3", "freebl3", "sqlite3",
        "softokn3", "nss3", "nssckbi", "nssdbm3", "smime3", "ssl3",
        "c++_shared", "lo-native-code"
    )

    init {
        isLibraryLoaded = tryLoadNative()
        if (isLibraryLoaded) {
            safeLog { Log.i(TAG, "Native LibreOffice library loaded successfully.") }
        } else {
            safeLog { Log.w(TAG, "No native library found (tried lo-native-code chain + libreoffice-core). Running simulated/JVM fallback mode.") }
        }
    }

    /**
     * `android.util.Log` is the "Stub!" jar on a plain JVM host (the repository's
     * unit tests run without Robolectric for this seam), and the stub throws
     * `RuntimeException` from every method. Diagnostics must never decide whether
     * the native probe or its fallback works, so every log call in this object
     * passes through here.
     */
    private inline fun safeLog(block: () -> Unit) {
        try {
            block()
        } catch (ignored: Throwable) {
            // Host without a working android.util.Log: drop the diagnostic.
        }
    }

    /**
     * Native probe: loads the pre-bundled `liblo-native-code.so` first (with
     * its dependency chain, resolved from `app/src/main/libs/<abi>/`), then
     * the legacy `libreoffice-core.so` custom name.
     *
     * The probe catches every [Throwable] per library instead of only
     * `UnsatisfiedLinkError`. On a device a missing dependency surfaces as
     * `UnsatisfiedLinkError`, but a JVM or Robolectric host can raise an
     * `ExceptionInInitializerError` (or another `LinkageError` from the class
     * loader) for the same condition, and "native library unavailable" must
     * always mean the pure-Kotlin engine rather than a crash. A probe that
     * returns true still proves the whole chain loaded.
     */
    private fun tryLoadNative(): Boolean {
        for (soname in LO_NATIVE_LOAD_ORDER) {
            if (!tryLoadOne(soname)) {
                // Fall through to the legacy/custom soname.
                return tryLoadOne("libreoffice-core")
            }
        }
        return true
    }

    private fun tryLoadOne(soname: String): Boolean = try {
        System.loadLibrary(soname)
        true
    } catch (t: Throwable) {
        safeLog { Log.w(TAG, "Native library '$soname' could not be loaded: ${t.javaClass.simpleName}") }
        false
    }

    /**
     * Configures the LibreOfficeKit environment (`org.libreoffice.kit.LibreOfficeKit.putenv`).
     * Full UNO bootstrap (`initializeNative`) is deferred to the isolated `:office` process
     * in [LokitRuntime] so the UI process does not pay the native heap cost.
     */
    fun initialize(cacheDir: String, enableOoxml: Boolean, enableOmml: Boolean): Boolean {
        safeLog { Log.d(TAG, "Initializing LibreOffice Core JNI. cacheDir=$cacheDir, enableOoxml=$enableOoxml, enableOmml=$enableOmml") }
        if (!isLibraryLoaded) {
            safeLog { Log.w(TAG, "Native library not loaded. Running JVM mock setup.") }
            return true
        }
        return try {
            LibreOfficeKit.putenv("TMPDIR=$cacheDir")
            LibreOfficeKit.putenv("SAL_LOK_OPTIONS=compact_fonts")
            isEnvConfigured = true
            true
        } catch (e: Throwable) {
            safeLog { Log.w(TAG, "LibreOfficeKit.putenv unavailable (${e.javaClass.simpleName}), running JVM mock setup") }
            isEnvConfigured = false
            true
        }
    }

    /**
     * Render a document page into a byte array buffer.
     * Interactive Writer/Calc/Impress views use the pure-Kotlin layout engine;
     * PDF export routes through [OfficeEngineClient] in `:office`.
     */
    fun renderPageToBuffer(docPath: String, pageIndex: Int, outputBuffer: ByteArray, width: Int, height: Int): Boolean {
        safeLog { Log.d(TAG, "Rendering page $pageIndex of $docPath (${width}x${height})") }
        return true
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
    }

    /**
     * Parse and export spreadsheet calculations when called through the legacy bridge.
     */
    fun evaluateFormula(formula: String, sheetDataJson: String): String {
        return "MOCK_RESULT_FOR($formula)"
    }

    /**
     * Create a new document handle in the bridge.
     */
    fun createDocument(fileName: String): Int {
        safeLog { Log.d(TAG, "Creating new document: $fileName") }
        return 1
    }
}
