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
    val helplinesWhileMoving: Boolean = true,
    val showDrawingControls: Boolean = true,
    val showComments: Boolean = true,
    val showResolvedComments: Boolean = true,
    val showHiddenText: Boolean = false,
    val showHiddenParagraphs: Boolean = false,
    val trackedDeletionsInMargin: Boolean = false,
    val tooltipsOnTrackedChanges: Boolean = true,
    val enableOutlineFolding: Boolean = true,
    val includeSubLevelsFold: Boolean = true,
    val enableSmoothScrolling: Boolean = true,
    val zoomMode: ZoomMode = ZoomMode.HUNDRED,
    val customZoomPercent: Int = 100
)

val Context.inkyDataStore: DataStore<Preferences> by preferencesDataStore(name = "inky_preferences")

class InkyPreferencesRepository(private val context: Context) {
    companion object {
        val KEY_SHOW_IMAGES = booleanPreferencesKey("show_images")
        val KEY_SHOW_TABLES = booleanPreferencesKey("show_tables")
        val KEY_HELPLINES_WHILE_MOVING = booleanPreferencesKey("helplines_while_moving")
        val KEY_SHOW_DRAWING_CONTROLS = booleanPreferencesKey("show_drawing_controls")
        val KEY_SHOW_COMMENTS = booleanPreferencesKey("show_comments")
        val KEY_SHOW_RESOLVED_COMMENTS = booleanPreferencesKey("show_resolved_comments")
        val KEY_SHOW_HIDDEN_TEXT = booleanPreferencesKey("show_hidden_text")
        val KEY_SHOW_HIDDEN_PARAGRAPHS = booleanPreferencesKey("show_hidden_paragraphs")
        val KEY_TRACKED_DELETIONS_MARGIN = booleanPreferencesKey("tracked_deletions_margin")
        val KEY_TOOLTIPS_TRACKED_CHANGES = booleanPreferencesKey("tooltips_tracked_changes")
        val KEY_ENABLE_OUTLINE_FOLDING = booleanPreferencesKey("enable_outline_folding")
        val KEY_INCLUDE_SUB_LEVELS_FOLD = booleanPreferencesKey("include_sub_levels_fold")
        val KEY_ENABLE_SMOOTH_SCROLLING = booleanPreferencesKey("enable_smooth_scrolling")
        val KEY_ZOOM_MODE = stringPreferencesKey("zoom_mode")
        val KEY_CUSTOM_ZOOM_PERCENT = intPreferencesKey("custom_zoom_percent")
    }

    val viewOptionsFlow: Flow<InkyViewOptions> = context.inkyDataStore.data
        .map { preferences ->
            InkyViewOptions(
                showImages = preferences[KEY_SHOW_IMAGES] ?: true,
                showTables = preferences[KEY_SHOW_TABLES] ?: true,
                helplinesWhileMoving = preferences[KEY_HELPLINES_WHILE_MOVING] ?: true,
                showDrawingControls = preferences[KEY_SHOW_DRAWING_CONTROLS] ?: true,
                showComments = preferences[KEY_SHOW_COMMENTS] ?: true,
                showResolvedComments = preferences[KEY_SHOW_RESOLVED_COMMENTS] ?: true,
                showHiddenText = preferences[KEY_SHOW_HIDDEN_TEXT] ?: false,
                showHiddenParagraphs = preferences[KEY_SHOW_HIDDEN_PARAGRAPHS] ?: false,
                trackedDeletionsInMargin = preferences[KEY_TRACKED_DELETIONS_MARGIN] ?: false,
                tooltipsOnTrackedChanges = preferences[KEY_TOOLTIPS_TRACKED_CHANGES] ?: true,
                enableOutlineFolding = preferences[KEY_ENABLE_OUTLINE_FOLDING] ?: true,
                includeSubLevelsFold = preferences[KEY_INCLUDE_SUB_LEVELS_FOLD] ?: true,
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
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_IMAGES] = show }
    }

    suspend fun updateShowTables(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_TABLES] = show }
    }

    suspend fun updateHelplinesWhileMoving(enable: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_HELPLINES_WHILE_MOVING] = enable }
    }

    suspend fun updateShowDrawingControls(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_DRAWING_CONTROLS] = show }
    }

    suspend fun updateShowComments(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_COMMENTS] = show }
    }

    suspend fun updateShowResolvedComments(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_RESOLVED_COMMENTS] = show }
    }

    suspend fun updateShowHiddenText(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_HIDDEN_TEXT] = show }
    }

    suspend fun updateShowHiddenParagraphs(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_SHOW_HIDDEN_PARAGRAPHS] = show }
    }

    suspend fun updateTrackedDeletionsMargin(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_TRACKED_DELETIONS_MARGIN] = show }
    }

    suspend fun updateTooltipsTrackedChanges(show: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_TOOLTIPS_TRACKED_CHANGES] = show }
    }

    suspend fun updateEnableOutlineFolding(enable: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_ENABLE_OUTLINE_FOLDING] = enable }
    }

    suspend fun updateIncludeSubLevelsFold(enable: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_INCLUDE_SUB_LEVELS_FOLD] = enable }
    }

    suspend fun updateEnableSmoothScrolling(enable: Boolean) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_ENABLE_SMOOTH_SCROLLING] = enable }
    }

    suspend fun updateZoomMode(mode: ZoomMode) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_ZOOM_MODE] = mode.name }
    }

    suspend fun updateCustomZoomPercent(percent: Int) {
        context.inkyDataStore.edit { preferences -> preferences[KEY_CUSTOM_ZOOM_PERCENT] = percent }
    }
}
