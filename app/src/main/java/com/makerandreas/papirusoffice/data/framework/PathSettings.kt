package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import java.io.File

/**
 * Manages the paths of LibreOffice/PapirusOffice.
 * Matches com.sun.star.util.PathSettings and com.sun.star.util.PathSubstitution
 */
class PathSettings(private val context: Context) {

    private val predefinedPaths = mutableMapOf<String, String>()

    init {
        // Initialize default paths based on Android Context
        val userDir = context.filesDir.absolutePath
        val cacheDir = context.cacheDir.absolutePath
        
        predefinedPaths["Addin"] = "$userDir/addin"
        predefinedPaths["AutoCorrect"] = "$userDir/autocorr"
        predefinedPaths["AutoText"] = "$userDir/autotext"
        predefinedPaths["Backup"] = "$userDir/backup"
        predefinedPaths["Basic"] = "$userDir/basic"
        predefinedPaths["Bitmap"] = "$userDir/config/symbol"
        predefinedPaths["Config"] = "$userDir/config"
        predefinedPaths["Dictionary"] = "$userDir/wordbook"
        predefinedPaths["Favorite"] = "$userDir/config/folders"
        predefinedPaths["Filter"] = "$userDir/filter"
        predefinedPaths["Font"] = "$userDir/fonts"
        predefinedPaths["Gallery"] = "$userDir/gallery"
        predefinedPaths["Graphic"] = "$userDir/gallery"
        predefinedPaths["Help"] = "$userDir/help"
        predefinedPaths["Hyphenation"] = "$userDir/hyphenation"
        predefinedPaths["Linguistic"] = "$userDir/dict"
        predefinedPaths["Module"] = userDir
        predefinedPaths["Palette"] = "$userDir/config"
        predefinedPaths["Plugin"] = "$userDir/plugin"
        predefinedPaths["Storage"] = "$userDir/store"
        predefinedPaths["Temp"] = cacheDir
        predefinedPaths["Template"] = "$userDir/template"
        predefinedPaths["UIConfig"] = "$userDir/config/ui"
        predefinedPaths["UserConfig"] = "$userDir/config"
        predefinedPaths["UserDictionary"] = "$userDir/wordbook"
        predefinedPaths["Work"] = "$userDir/work"
        
        // Ensure directories exist
        predefinedPaths.values.forEach { path ->
            val dir = File(path)
            if (!dir.exists()) {
                dir.mkdirs()
            }
        }
    }

    fun getPropertyValue(propertyName: String): String {
        return predefinedPaths[propertyName] ?: throw IllegalArgumentException("Unknown path property: $propertyName")
    }

    fun setPropertyValue(propertyName: String, value: String) {
        predefinedPaths[propertyName] = value
    }

    fun substituteVariables(text: String, substRequired: Boolean = false): String {
        var result = text
        predefinedPaths.forEach { (key, value) ->
            result = result.replace("$($key)", value, ignoreCase = true)
        }
        return result
    }

    fun getSubstituteVariableValue(variable: String): String {
        val cleanVar = variable.removePrefix("$(").removeSuffix(")")
        return predefinedPaths[cleanVar] ?: throw IllegalArgumentException("Unknown variable: $variable")
    }
}
