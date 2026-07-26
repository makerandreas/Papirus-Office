package com.makerandreas.papirusoffice.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import java.io.File

/**
 * PapirusConfigManager
 * Central configuration manager for Papirus Office using INI configuration files.
 * Reads priority: UserConfig.ini > DefaultConfig.ini > Hardcoded default.
 */
object PapirusConfigManager {

    private const val TAG = "PapirusConfigManager"

    private const val DEFAULT_CONFIG_FILENAME = "DefaultConfig.ini"
    private const val USER_CONFIG_FILENAME = "UserConfig.ini"

    private const val PREFS_OPTIONS_NAME = "papirus_options_prefs"
    private const val PREFS_THEME_NAME = "papirus_office_theme_prefs"
    private const val PREFS_GENERAL_NAME = "papirus_options"

    private const val KEY_RESET_SUCCESS_PENDING = "key_reset_success_pending"

    // In-memory cached representations of parsed INI configs: Section -> (Key -> Value)
    private var defaultConfigMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    private var userConfigMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    private var isInitialized = false

    /**
     * Initializes the configuration manager, ensuring DefaultConfig.ini exists
     * and parsing both DefaultConfig.ini and UserConfig.ini into memory.
     */
    @Synchronized
    fun initialize(context: Context) {
        ensureDefaultConfigExists(context)
        val userFile = getUserConfigFile(context)
        if (!userFile.exists()) {
            try {
                getDefaultConfigFile(context).copyTo(userFile, overwrite = true)
                Log.d(TAG, "Copied DefaultConfig.ini to UserConfig.ini on initial start.")
            } catch (e: Exception) {
                Log.e(TAG, "Error copying DefaultConfig.ini to UserConfig.ini", e)
            }
        }
        loadConfigs(context)
        isInitialized = true
    }

    private fun getDefaultConfigFile(context: Context): File {
        return File(context.filesDir, DEFAULT_CONFIG_FILENAME)
    }

    private fun getUserConfigFile(context: Context): File {
        return File(context.filesDir, USER_CONFIG_FILENAME)
    }

    /**
     * Creates DefaultConfig.ini if it doesn't exist yet with baseline Papirus Office settings.
     */
    fun ensureDefaultConfigExists(context: Context) {
        val file = getDefaultConfigFile(context)
        if (!file.exists()) {
            val defaultConfigContent = """
                # Papirus Office Default System Configuration
                # Created automatically as master reference (DefaultConfig.ini)

                [General]
                theme_dynamic_color=true
                theme_dark_mode=false
                user_name=Papirus User
                user_initials=PU

                [LoadAndSave]
                load_user_specific_settings=true
                load_printer_settings=true
                auto_recovery_enabled=true
                auto_recovery_interval=10
                auto_save_document_too=false
                edit_doc_properties_before_saving=false
                always_create_backup_copy=true
                place_backup_in_same_folder=false
                save_urls_relative_file_system=true
                save_urls_relative_internet=true
                odf_format_version=1.4 Extended (recommended)
                always_save_as=OpenDocument
                warn_when_not_saving_odf=true

                [Security]
                macro_protection=true

                [Inky]
                default_font_family=Liberation Serif
                default_font_size=12
                show_ruler=true

                [Cellina]
                default_sheets=1
                show_gridlines=true

                [Slidia]
                default_slide_aspect_ratio=16:9
            """.trimIndent()
            try {
                file.writeText(defaultConfigContent)
                Log.d(TAG, "DefaultConfig.ini generated successfully at ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error writing DefaultConfig.ini", e)
            }
        }
    }

    /**
     * Loads/reloads INI configuration files from storage into memory maps.
     */
    @Synchronized
    fun loadConfigs(context: Context) {
        val defaultFile = getDefaultConfigFile(context)
        if (defaultFile.exists()) {
            defaultConfigMap = parseIniFile(defaultFile)
        } else {
            ensureDefaultConfigExists(context)
            defaultConfigMap = parseIniFile(defaultFile)
        }

        val userFile = getUserConfigFile(context)
        if (userFile.exists()) {
            userConfigMap = parseIniFile(userFile)
        } else {
            userConfigMap = mutableMapOf()
        }
    }

    /**
     * Parses an INI configuration file into section -> (key -> value) map.
     */
    private fun parseIniFile(file: File): MutableMap<String, MutableMap<String, String>> {
        val resultMap = mutableMapOf<String, MutableMap<String, String>>()
        var currentSection = "General"

        try {
            file.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) {
                    return@forEachLine
                }
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    currentSection = trimmed.substring(1, trimmed.length - 1).trim()
                    if (!resultMap.containsKey(currentSection)) {
                        resultMap[currentSection] = mutableMapOf()
                    }
                } else if (trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    val key = parts[0].trim()
                    val value = parts[1].trim()
                    if (!resultMap.containsKey(currentSection)) {
                        resultMap[currentSection] = mutableMapOf()
                    }
                    resultMap[currentSection]!![key] = value
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse INI file: ${file.name}", e)
        }
        return resultMap
    }

    /**
     * Writes section -> (key -> value) map to an INI file format.
     */
    private fun writeIniFile(file: File, map: Map<String, Map<String, String>>) {
        val sb = java.lang.StringBuilder()
        sb.append("# Papirus Office Modified User Configuration\n")
        sb.append("# Auto-generated upon user setting change (UserConfig.ini)\n\n")

        for ((section, keys) in map) {
            if (keys.isEmpty()) continue
            sb.append("[$section]\n")
            for ((key, value) in keys) {
                sb.append("$key=$value\n")
            }
            sb.append("\n")
        }

        try {
            file.writeText(sb.toString())
            Log.d(TAG, "Saved config to ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write INI file: ${file.name}", e)
        }
    }

    /**
     * Priority read: UserConfig.ini > DefaultConfig.ini > Fallback default.
     */
    fun getString(context: Context, section: String, key: String, defaultValue: String): String {
        if (!isInitialized) initialize(context)

        // Priority 1: UserConfig.ini
        val userVal = userConfigMap[section]?.get(key)
        if (userVal != null) return userVal

        // Priority 2: DefaultConfig.ini
        val defaultVal = defaultConfigMap[section]?.get(key)
        if (defaultVal != null) return defaultVal

        return defaultValue
    }

    fun getBoolean(context: Context, section: String, key: String, defaultValue: Boolean): Boolean {
        val strVal = getString(context, section, key, defaultValue.toString())
        return strVal.toBooleanStrictOrNull() ?: defaultValue
    }

    fun getInt(context: Context, section: String, key: String, defaultValue: Int): Int {
        val strVal = getString(context, section, key, defaultValue.toString())
        return strVal.toIntOrNull() ?: defaultValue
    }

    /**
     * Saves a setting change to UserConfig.ini automatically whenever a setting is modified.
     * Also synchronizes to legacy SharedPreferences for seamless runtime consistency.
     */
    @Synchronized
    fun saveValue(context: Context, section: String, key: String, value: Any) {
        if (!isInitialized) initialize(context)

        val valueStr = value.toString()
        if (!userConfigMap.containsKey(section)) {
            userConfigMap[section] = mutableMapOf()
        }
        userConfigMap[section]!![key] = valueStr

        // Persist to UserConfig.ini file
        writeIniFile(getUserConfigFile(context), userConfigMap)

        // Also sync to SharedPreferences
        syncToSharedPreferences(context, section, key, value)
    }

    private fun syncToSharedPreferences(context: Context, section: String, key: String, value: Any) {
        val optionsPrefs = context.getSharedPreferences(PREFS_OPTIONS_NAME, Context.MODE_PRIVATE)
        val themePrefs = context.getSharedPreferences(PREFS_THEME_NAME, Context.MODE_PRIVATE)
        val generalPrefs = context.getSharedPreferences(PREFS_GENERAL_NAME, Context.MODE_PRIVATE)

        when (key) {
            "theme_dynamic_color" -> {
                if (value is Boolean) themePrefs.edit().putBoolean("dynamic_color_enabled", value).apply()
            }
            "auto_recovery_enabled" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("auto_recovery_enabled", value).apply()
            }
            "auto_recovery_interval" -> {
                if (value is Int) optionsPrefs.edit().putInt("auto_recovery_interval", value).apply()
                else value.toString().toIntOrNull()?.let { optionsPrefs.edit().putInt("auto_recovery_interval", it).apply() }
            }
            "auto_save_document_too" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("auto_save_document_too", value).apply()
            }
            "always_create_backup_copy" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("always_create_backup_copy", value).apply()
            }
            "place_backup_in_same_folder" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("place_backup_in_same_folder", value).apply()
            }
            "load_user_specific_settings" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("load_user_specific_settings", value).apply()
            }
            "load_printer_settings" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("load_printer_settings", value).apply()
            }
            "edit_doc_properties_before_saving" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("edit_doc_properties_before_saving", value).apply()
            }
            "save_urls_relative_file_system" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("save_urls_relative_file_system", value).apply()
            }
            "save_urls_relative_internet" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("save_urls_relative_internet", value).apply()
            }
            "odf_format_version" -> {
                optionsPrefs.edit().putString("odf_format_version", valueStr(value)).apply()
            }
            "always_save_as" -> {
                optionsPrefs.edit().putString("always_save_as", valueStr(value)).apply()
            }
            "warn_when_not_saving_odf" -> {
                if (value is Boolean) optionsPrefs.edit().putBoolean("warn_when_not_saving_odf", value).apply()
            }
            "macro_protection" -> {
                if (value is Boolean) generalPrefs.edit().putBoolean("macro_protection", value).apply()
            }
        }
    }

    private fun valueStr(value: Any): String = value.toString()

    /**
     * Executes the Reset operation:
     * 1. Removes UserConfig.ini.
     * 2. Directs config reader back to DefaultConfig.ini.
     * 3. Performs "hidden preparation" to recreate UserConfig.ini cleanly on subsequent edits.
     * 4. Resets SharedPreferences to defaults.
     * 5. Sets flag to show post-reset success popup upon app restart.
     * 6. If restartNow is true, triggers app restart immediately.
     */
    @Synchronized
    fun performReset(context: Context, restartNow: Boolean) {
        // 1. Delete UserConfig.ini
        val userFile = getUserConfigFile(context)
        if (userFile.exists()) {
            val deleted = userFile.delete()
            Log.d(TAG, "UserConfig.ini deleted: $deleted")
        }

        // 2. Clear userConfigMap memory cache
        userConfigMap.clear()

        // 3. Ensure DefaultConfig.ini exists & reload memory cache
        ensureDefaultConfigExists(context)
        defaultConfigMap = parseIniFile(getDefaultConfigFile(context))

        // 4. Reset SharedPreferences back to defaults
        val optionsPrefs = context.getSharedPreferences(PREFS_OPTIONS_NAME, Context.MODE_PRIVATE)
        val themePrefs = context.getSharedPreferences(PREFS_THEME_NAME, Context.MODE_PRIVATE)
        val generalPrefs = context.getSharedPreferences(PREFS_GENERAL_NAME, Context.MODE_PRIVATE)

        optionsPrefs.edit().clear().apply()
        themePrefs.edit().clear().apply()
        generalPrefs.edit().clear().apply()

        // 5. Mark pending reset success popup flag
        optionsPrefs.edit().putBoolean(KEY_RESET_SUCCESS_PENDING, true).apply()

        Log.d(TAG, "Reset process completed. UserConfig.ini removed, DefaultConfig.ini active.")

        // 6. If restartNow, perform process restart
        if (restartNow) {
            restartApp(context)
        }
    }

    /**
     * Restarts the application process safely.
     */
    fun restartApp(context: Context) {
        try {
            val packageManager = context.packageManager
            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                if (context is Activity) {
                    context.finish()
                }
                Runtime.getRuntime().exit(0)
            } else {
                if (context is Activity) {
                    context.recreate()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting app", e)
            if (context is Activity) {
                context.recreate()
            }
        }
    }

    /**
     * Checks if a post-reset success notification is pending, and if so triggers the popup callback.
     */
    fun checkAndShowResetSuccessPopup(context: Context, onShowPopup: (String) -> Unit) {
        val optionsPrefs = context.getSharedPreferences(PREFS_OPTIONS_NAME, Context.MODE_PRIVATE)
        val isPending = optionsPrefs.getBoolean(KEY_RESET_SUCCESS_PENDING, false)
        if (isPending) {
            // Clear flag
            optionsPrefs.edit().putBoolean(KEY_RESET_SUCCESS_PENDING, false).apply()
            val message = "Pengaturan aplikasi sukses direset."
            onShowPopup(message)
        }
    }
}
