package com.makerandreas.papirusoffice.data

import android.content.Context
import android.content.SharedPreferences

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
    val isSaved: Boolean = true
)

class SafeSessionRestore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("papirus_session_restore", Context.MODE_PRIVATE)

    fun saveLastSession(info: LastSessionInfo) {
        PapirusLogger.i("SessionRestore", "Saving last session details: uri=${info.uri}, cursor=${info.cursor}, module=${info.module}, isSaved=${info.isSaved}")
        prefs.edit().apply {
            putString("uri", info.uri)
            putInt("cursor", info.cursor)
            putFloat("zoom", info.zoom)
            putInt("scroll", info.scroll)
            putString("module", info.module.name)
            putString("docTitle", info.docTitle)
            putString("draftText", info.draftText)
            putBoolean("isSaved", info.isSaved)
            apply()
        }
    }

    fun getLastSession(): LastSessionInfo? {
        val uri = prefs.getString("uri", null) ?: return null
        val cursor = prefs.getInt("cursor", 0)
        val zoom = prefs.getFloat("zoom", 1.0f)
        val scroll = prefs.getInt("scroll", 0)
        val moduleStr = prefs.getString("module", null) ?: return null
        val docTitle = prefs.getString("docTitle", null)
        val draftText = prefs.getString("draftText", null)
        val isSaved = prefs.getBoolean("isSaved", true)

        val module = try {
            ModuleType.valueOf(moduleStr)
        } catch (e: Exception) {
            ModuleType.WRITER
        }

        val sessionInfo = LastSessionInfo(
            uri = uri,
            cursor = cursor,
            zoom = zoom,
            scroll = scroll,
            module = module,
            docTitle = docTitle,
            draftText = draftText,
            isSaved = isSaved
        )
        PapirusLogger.i("SessionRestore", "Retrieved last session: $sessionInfo")
        return sessionInfo
    }

    fun clearLastSession() {
        PapirusLogger.i("SessionRestore", "Clearing last session info")
        prefs.edit().clear().apply()
    }
}
