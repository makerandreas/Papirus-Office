package com.makerandreas.papirusoffice.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class ModuleType {
    WRITER, CALC, IMPRESS, PAGELLA
}

data class LastSessionInfo(
    val uri: String,
    val cursor: Int = 0,
    val zoom: Float = 1.0f,
    val scroll: Int = 0,
    val module: ModuleType = ModuleType.WRITER,
    val docTitle: String? = null,
    val draftText: String? = null,
    val isSaved: Boolean = true,
    val workspace: String = "Inky",
    val sourceUri: String? = null
)

/**
 * Atomic session record stored under filesDir (survives cache trim / LMK).
 * JSON + optional draft.txt are written to a temp file, fsynced, then renamed.
 * Independent of Compose rememberSaveable workspace state.
 */
class SafeSessionRestore(context: Context) {
    private val appContext = context.applicationContext
    private val sessionDir: File = File(appContext.filesDir, "session")
    private val jsonFile: File = File(sessionDir, "last_session.json")
    private val draftFile: File = File(sessionDir, "draft.txt")

    fun saveLastSession(info: LastSessionInfo) {
        PapirusLogger.i(
            "SessionRestore",
            "Saving last session details: uri=${info.uri}, cursor=${info.cursor}, module=${info.module}, isSaved=${info.isSaved}"
        )
        if (!sessionDir.exists()) sessionDir.mkdirs()
        val json = JSONObject().apply {
            put("uri", info.uri)
            put("cursor", info.cursor)
            put("zoom", info.zoom.toDouble())
            put("scroll", info.scroll)
            put("module", info.module.name)
            put("docTitle", info.docTitle ?: JSONObject.NULL)
            put("isSaved", info.isSaved)
            put("workspace", info.workspace)
            put("sourceUri", info.sourceUri ?: JSONObject.NULL)
            put("hasDraft", !info.isSaved && !info.draftText.isNullOrEmpty())
        }
        atomicWrite(jsonFile, json.toString())
        if (!info.isSaved && info.draftText != null) {
            atomicWrite(draftFile, info.draftText)
        } else if (info.isSaved && draftFile.exists()) {
            draftFile.delete()
        }
    }

    /**
     * Re-sync already-written session files. Called from Activity ON_PAUSE / ON_STOP.
     */
    fun flush() {
        syncExisting(jsonFile)
        syncExisting(draftFile)
    }

    fun getLastSession(): LastSessionInfo? {
        if (!jsonFile.exists()) {
            return migrateFromLegacyPrefs()
        }
        return try {
            val json = JSONObject(jsonFile.readText())
            val uri = json.optString("uri", "").ifBlank { return null }
            val moduleStr = json.optString("module", ModuleType.WRITER.name)
            val module = try {
                ModuleType.valueOf(moduleStr)
            } catch (_: Exception) {
                ModuleType.WRITER
            }
            val hasDraft = json.optBoolean("hasDraft", false)
            val isSaved = json.optBoolean("isSaved", true)
            val draft = if (!isSaved && hasDraft && draftFile.exists()) {
                draftFile.readText()
            } else {
                null
            }
            val sessionInfo = LastSessionInfo(
                uri = uri,
                cursor = json.optInt("cursor", 0),
                zoom = json.optDouble("zoom", 1.0).toFloat(),
                scroll = json.optInt("scroll", 0),
                module = module,
                docTitle = json.optString("docTitle").takeIf { json.has("docTitle") && !json.isNull("docTitle") },
                draftText = draft,
                isSaved = isSaved,
                workspace = json.optString("workspace", "Inky"),
                sourceUri = json.optString("sourceUri").takeIf { json.has("sourceUri") && !json.isNull("sourceUri") }
            )
            PapirusLogger.i("SessionRestore", "Retrieved last session: $sessionInfo")
            sessionInfo
        } catch (e: Exception) {
            PapirusLogger.w("SessionRestore", "Failed to read session file: ${e.message}")
            null
        }
    }

    /**
     * Returns a restorable session only when the on-disk file still exists.
     * Missing files must NOT spawn Normal.ott — callers should send the user home.
     */
    fun getRestorableSession(): LastSessionInfo? {
        val session = getLastSession() ?: return null
        val file = File(session.uri)
        if (!file.exists() || file.length() <= 0L) {
            PapirusLogger.w("SessionRestore", "Session file gone, refusing Normal.ott fallback: ${session.uri}")
            return null
        }
        return session
    }

    fun clearLastSession() {
        PapirusLogger.i("SessionRestore", "Clearing last session info")
        if (jsonFile.exists()) jsonFile.delete()
        if (draftFile.exists()) draftFile.delete()
        try {
            appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit()
        } catch (_: Exception) {
        }
    }

    private fun migrateFromLegacyPrefs(): LastSessionInfo? {
        val prefs = appContext.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val uri = prefs.getString("uri", null) ?: return null
        val moduleStr = prefs.getString("module", null) ?: return null
        val module = try {
            ModuleType.valueOf(moduleStr)
        } catch (_: Exception) {
            ModuleType.WRITER
        }
        val info = LastSessionInfo(
            uri = uri,
            cursor = prefs.getInt("cursor", 0),
            zoom = prefs.getFloat("zoom", 1.0f),
            scroll = prefs.getInt("scroll", 0),
            module = module,
            docTitle = prefs.getString("docTitle", null),
            draftText = prefs.getString("draftText", null),
            isSaved = prefs.getBoolean("isSaved", true)
        )
        saveLastSession(info)
        prefs.edit().clear().commit()
        return info
    }

    private fun atomicWrite(file: File, content: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { fos ->
            fos.write(content.toByteArray(Charsets.UTF_8))
            fos.flush()
            fos.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    private fun syncExisting(file: File) {
        if (!file.exists()) return
        try {
            FileOutputStream(file, true).use { fos ->
                fos.flush()
                fos.fd.sync()
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val LEGACY_PREFS = "papirus_session_restore"
    }
}
