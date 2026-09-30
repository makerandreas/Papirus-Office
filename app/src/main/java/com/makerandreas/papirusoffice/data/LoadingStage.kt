package com.makerandreas.papirusoffice.data

import androidx.annotation.StringRes
import com.example.R

/**
 * Plan 6C (F-4): the real stages of opening a document, in the order the
 * open path runs them. Each stage is announced when its work starts, never on
 * a timer, so the loading screen only says what the app is actually doing.
 *
 * Pipeline order (ordinals only ever increase during one open):
 * - [OPENING_PACKAGE]: the ZIP package is opened and the main XML part is read.
 * - [VALIDATING]: the XML is checked before anything is built from it.
 * - [EXTRACTING_MEDIA]: embedded images go into the durable media store.
 * - [READING_STYLES]: `styles.xml` (ODF) or `word/styles.xml` (OOXML) is parsed.
 * - [READING_BODY]: the document body is parsed into elements.
 * - [CACHED]: the parsed model came from the in-memory cache instead of the
 *   four stages above (media is still re-checked on this path).
 * - [LAYOUT]: the paginator lays the document out on pages. Owned by the
 *   editor screen, not the parser, because layout runs there.
 *
 * [percent] is a coarse, fixed position for each stage, not a measured
 * fraction of work.
 */
enum class LoadingStage(val percent: Int, @StringRes val messageRes: Int) {
    OPENING_PACKAGE(10, R.string.loading_stage_opening_package),
    VALIDATING(20, R.string.loading_stage_validating),
    EXTRACTING_MEDIA(35, R.string.loading_stage_extracting_media),
    READING_STYLES(50, R.string.loading_stage_reading_styles),
    READING_BODY(65, R.string.loading_stage_reading_body),
    CACHED(70, R.string.loading_stage_cached),
    LAYOUT(90, R.string.loading_stage_layout);
}
