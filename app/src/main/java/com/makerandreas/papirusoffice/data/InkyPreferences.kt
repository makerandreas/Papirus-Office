package com.makerandreas.papirusoffice.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ZoomMode {
    LAST,
    FIT_WIDTH,
    HUNDRED,
    CUSTOM
}

data class InkyViewOptions(
    val showImages: Boolean = true,
    val showTables: Boolean = true,
    val enableOutlineFolding: Boolean = true,
    val enableSmoothScrolling: Boolean = true,
    val zoomMode: ZoomMode = ZoomMode.HUNDRED,
    val customZoomPercent: Int = 100
)

val Context.inkyDataStore: DataStore<Preferences> by preferencesDataStore(name = "inky_preferences")

class InkyPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_SHOW_IMAGES = booleanPreferencesKey("show_images")
        val KEY_SHOW_TABLES = booleanPreferencesKey("show_tables")
        val KEY_ENABLE_OUTLINE_FOLDING = booleanPreferencesKey("enable_outline_folding")
        val KEY_ENABLE_SMOOTH_SCROLLING = booleanPreferencesKey("enable_smooth_scrolling")
        val KEY_ZOOM_MODE = stringPreferencesKey("zoom_mode")
        val KEY_CUSTOM_ZOOM_PERCENT = intPreferencesKey("custom_zoom_percent")
    }

    val viewOptionsFlow: Flow<InkyViewOptions> = context.inkyDataStore.data
        .map { preferences ->
            InkyViewOptions(
                showImages = preferences[KEY_SHOW_IMAGES] ?: true,
                showTables = preferences[KEY_SHOW_TABLES] ?: true,
                enableOutlineFolding = preferences[KEY_ENABLE_OUTLINE_FOLDING] ?: true,
                enableSmoothScrolling = preferences[KEY_ENABLE_SMOOTH_SCROLLING] ?: true,
                zoomMode = try {
                    ZoomMode.valueOf(preferences[KEY_ZOOM_MODE] ?: ZoomMode.HUNDRED.name)
                } catch (e: Exception) {
                    ZoomMode.HUNDRED
                },
                customZoomPercent = preferences[KEY_CUSTOM_ZOOM_PERCENT] ?: 100
            )
        }

    suspend fun updateShowImages(show: Boolean) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_SHOW_IMAGES] = show
        }
    }

    suspend fun updateShowTables(show: Boolean) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_SHOW_TABLES] = show
        }
    }

    suspend fun updateEnableOutlineFolding(enable: Boolean) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_ENABLE_OUTLINE_FOLDING] = enable
        }
    }

    suspend fun updateEnableSmoothScrolling(enable: Boolean) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_ENABLE_SMOOTH_SCROLLING] = enable
        }
    }

    suspend fun updateZoomMode(mode: ZoomMode) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_ZOOM_MODE] = mode.name
        }
    }

    suspend fun updateCustomZoomPercent(percent: Int) {
        context.inkyDataStore.edit { preferences ->
            preferences[KEY_CUSTOM_ZOOM_PERCENT] = percent
        }
    }
}
