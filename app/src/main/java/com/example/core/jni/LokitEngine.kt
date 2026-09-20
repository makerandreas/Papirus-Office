// SPDX-License-Identifier: MPL-2.0
package com.example.core.jni

/**
 * Integration seam for a real LibreOfficeKit native build (Phase 2).
 *
 * Drop-in contract: place a LibreOffice-Android build's
 * `liblo-native-code.so` (plus its dependency chain) under
 * `app/src/main/jniLibs/<abi>/` — see `docs/LOKIT_INTEGRATION.md` and
 * `app/src/main/jniLibs/README.md`. [LibreOfficeCore] probes for it at
 * startup; when absent (the current state) the app runs its pure-Kotlin
 * Papirus engine and every consumer below reports simulated mode instead
 * of fabricating native telemetry.
 */
object LokitEngine {
    enum class EngineMode { NATIVE, SIMULATED }

    /** True only when a real native library was actually loaded. */
    val isNativeAvailable: Boolean
        get() = LibreOfficeCore.isNativeLibraryLoaded

    val mode: EngineMode
        get() = if (isNativeAvailable) EngineMode.NATIVE else EngineMode.SIMULATED

    /** User-facing engine status (About screen, diagnostics log seed). */
    val statusLabel: String
        get() = if (isNativeAvailable) {
            "Native LOKit library loaded"
        } else {
            "Simulated (pure Kotlin engine — no native library)"
        }

    /**
     * Tags engine event log lines so simulated telemetry is never mistaken
     * for native LOKit dispatch. Pass-through when native is loaded.
     */
    fun tagLog(message: String): String =
        if (isNativeAvailable) message else "[simulated] $message"

    /** Sonames the [LibreOfficeCore] probe accepts, for docs/About diagnostics. */
    val acceptedSonames: List<String> =
        listOf("liblo-native-code.so", "liblibreoffice-core.so")
}
