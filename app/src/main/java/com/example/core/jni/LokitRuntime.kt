// SPDX-License-Identifier: MPL-2.0
package com.example.core.jni

import android.content.Context
import android.system.Os
import android.util.Log
import com.makerandreas.papirusoffice.data.PapirusAssetEngine
import org.libreoffice.kit.Document
import org.libreoffice.kit.LibreOfficeKit
import org.libreoffice.kit.Office
import java.io.File

sealed class LokitConversionResult {
    data class Success(val pdfFile: File) : LokitConversionResult()
    data object Unavailable : LokitConversionResult()
    data object PasswordRequired : LokitConversionResult()
    data class Failed(val reason: String) : LokitConversionResult()
}

/**
 * In-process LibreOfficeKit bootstrap and document conversion runner.
 * Intended to execute inside the isolated `:office` process via [OfficeEngineService].
 */
object LokitRuntime {
    private const val TAG = "LokitRuntime"
    private val lock = Any()
    private var office: Office? = null

    fun buildFontsConfXml(fontDirs: List<File>, cacheDir: File): String {
        val dirsXml = fontDirs.joinToString("\n") { dir ->
            "    <dir>${escapeXml(dir.absolutePath)}</dir>"
        }
        val aliases = listOf(
            "Calibri" to "Carlito",
            "Cambria" to "Caladea",
            "Arial" to "Liberation Sans",
            "Helvetica" to "Liberation Sans",
            "Times New Roman" to "Liberation Serif",
            "Courier New" to "Liberation Mono",
            "Aptos" to "Martel Sans",
            "Aptos Display" to "Martel Sans"
        )
        val aliasesXml = aliases.joinToString("\n") { (from, to) ->
            """
            |    <alias binding="same">
            |        <family>${escapeXml(from)}</family>
            |        <accept><family>${escapeXml(to)}</family></accept>
            |    </alias>
            """.trimMargin()
        }
        return """<?xml version="1.0"?>
<!DOCTYPE fontconfig SYSTEM "fonts.dtd">
<fontconfig>
$dirsXml
    <dir>/system/fonts</dir>
    <cachedir>${escapeXml(cacheDir.absolutePath)}</cachedir>
$aliasesXml
    <alias>
        <family>serif</family>
        <prefer><family>Liberation Serif</family></prefer>
    </alias>
    <alias>
        <family>sans-serif</family>
        <prefer><family>Liberation Sans</family><family>Carlito</family></prefer>
    </alias>
    <alias>
        <family>monospace</family>
        <prefer><family>Liberation Mono</family></prefer>
    </alias>
</fontconfig>
"""
    }

    private fun escapeXml(value: String): String =
        value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    fun prepareFontConfig(filesDir: File, cacheDir: File): File {
        val etcFontsDir = File(filesDir, "etc/fonts").apply { mkdirs() }
        val fontCacheDir = File(cacheDir, "fontconfig").apply { mkdirs() }
        val bundledFontsDir = File(filesDir, "papirus_assets/fonts").apply { mkdirs() }
        val fontsConf = File(etcFontsDir, "fonts.conf")
        fontsConf.writeText(buildFontsConfXml(listOf(bundledFontsDir), fontCacheDir))
        return fontsConf
    }

    private fun ensureInitialized(context: Context): Office? = synchronized(lock) {
        office?.let { return it }
        if (!LibreOfficeCore.isNativeLibraryLoaded) {
            return null
        }

        return try {
            PapirusAssetEngine.initialize(context)
            val filesDir = context.filesDir
            val cacheDir = context.cacheDir
            val fontsConf = prepareFontConfig(filesDir, cacheDir)

            val unpackProgramDir = File(filesDir, "papirus_assets/unpack/program")
            val targetProgramDir = File(filesDir, "program")
            if (unpackProgramDir.isDirectory) {
                targetProgramDir.mkdirs()
                unpackProgramDir.listFiles()?.forEach { src ->
                    val dst = File(targetProgramDir, src.name)
                    if (src.isFile && (!dst.exists() || dst.length() == 0L)) {
                        src.copyTo(dst, overwrite = true)
                    }
                }
            }

            val envVars = mapOf(
                "FONTCONFIG_FILE" to fontsConf.absolutePath,
                "FONTCONFIG_PATH" to fontsConf.parentFile!!.absolutePath,
                "TMPDIR" to cacheDir.absolutePath,
                "HOME" to filesDir.absolutePath,
                "XDG_CACHE_HOME" to cacheDir.absolutePath,
                "SAL_LOK_OPTIONS" to "compact_fonts"
            )
            for ((k, v) in envVars) {
                runCatching { Os.setenv(k, v, true) }
                runCatching { LibreOfficeKit.putenv("$k=$v") }
            }

            runCatching { LibreOfficeKit.redirectStdio(true) }

            val apkPath = context.applicationInfo.sourceDir
            val ok = LibreOfficeKit.initializeNative(
                filesDir.absolutePath,
                cacheDir.absolutePath,
                apkPath,
                context.assets
            )
            if (!ok) {
                Log.w(TAG, "LibreOfficeKit.initializeNative returned false")
                return null
            }

            val handle = LibreOfficeKit.getLibreOfficeKitHandle()
            if (handle == null) {
                Log.w(TAG, "LibreOfficeKit.getLibreOfficeKitHandle returned null")
                return null
            }

            Office(handle).also { instance ->
                runCatching { instance.bindMessageCallback() }
                office = instance
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize LOKit runtime", t)
            null
        }
    }

    fun convertToPdf(
        context: Context,
        inputFile: File,
        outputFile: File,
        password: String? = null
    ): LokitConversionResult = synchronized(lock) {
        val lo = ensureInitialized(context) ?: return LokitConversionResult.Unavailable
        outputFile.parentFile?.mkdirs()
        outputFile.delete()

        val inputUrl = inputFile.toURI().toASCIIString()
        val outputUrl = outputFile.toURI().toASCIIString()

        return try {
            val features = if (password != null) Office.LOK_FEATURE_DOCUMENT_PASSWORD else 0L
            runCatching { lo.setOptionalFeatures(features) }
            if (password != null) {
                runCatching { lo.setDocumentPassword(inputUrl, password) }
            }
            val doc: Document = lo.documentLoad(inputUrl)
                ?: return LokitConversionResult.Failed(lo.getError() ?: "documentLoad returned null")
            val rc = try {
                doc.saveAs(outputUrl, "pdf", "")
            } finally {
                runCatching { doc.destroy() }
            }
            if (rc != 1 || !outputFile.exists() || outputFile.length() == 0L) {
                outputFile.delete()
                LokitConversionResult.Failed(lo.getError() ?: "saveAs returned $rc")
            } else {
                LokitConversionResult.Success(outputFile)
            }
        } catch (t: Throwable) {
            outputFile.delete()
            LokitConversionResult.Failed("${t.javaClass.simpleName}: ${t.message}")
        } finally {
            runCatching { lo.setOptionalFeatures(0L) }
        }
    }
}
