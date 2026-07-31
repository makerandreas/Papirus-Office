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
    val module: ModuleType
)

class SafeSessionRestore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("papirus_session_restore", Context.MODE_PRIVATE)

    fun saveLastSession(info: LastSessionInfo) {
        PapirusLogger.i("SessionRestore", "Saving last session details: uri=${info.uri}, cursor=${info.cursor}, module=${info.module}")
        prefs.edit().apply {
            putString("uri", info.uri)
            putInt("cursor", info.cursor)
            putFloat("zoom", info.zoom)
            putInt("scroll", info.scroll)
            putString("module", info.module.name)
            apply()
        }
    }

    fun getLastSession(): LastSessionInfo? {
        val uri = prefs.getString("uri", null) ?: return null
        val cursor = prefs.getInt("cursor", 0)
        val zoom = prefs.getFloat("zoom", 1.0f)
        val scroll = prefs.getInt("scroll", 0)
        val moduleStr = prefs.getString("module", null) ?: return null

        val module = try {
            ModuleType.valueOf(moduleStr)
        } catch (e: Exception) {
            ModuleType.WRITER
        }

        val sessionInfo = LastSessionInfo(uri, cursor, zoom, scroll, module)
        PapirusLogger.i("SessionRestore", "Retrieved last session: $sessionInfo")
        return sessionInfo
    }

    fun clearLastSession() {
        PapirusLogger.i("SessionRestore", "Clearing last session info")
        prefs.edit().clear().apply()
    }
}
