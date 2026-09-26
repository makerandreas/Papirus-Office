package com.example.modules.inky

import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.PageStyleSpec

/**
 * One page as drawn: its size in layout units and the single factor that
 * maps those units to screen dp (roadmap E-7). Card box, margins, block
 * gaps, text size and hit-testing all go through [pageScale], so the text
 * column on screen is the column the paginator wrapped against, at every
 * zoom and on every viewport.
 */
data class PageSheet(
    val pageWidthUnits: Float,
    val pageHeightUnits: Float,
    /** Screen dp per layout unit. */
    val pageScale: Float
) {
    val widthDp: Float get() = pageWidthUnits * pageScale
    val heightDp: Float get() = pageHeightUnits * pageScale

    fun toDp(units: Float): Float = units * pageScale

    /** Screen dp of a font size given in points: 12 pt is 16 units, so 16 dp at scale 1. */
    fun fontDp(points: Float): Float = LayoutUnits.ptToUnits(points) * pageScale

    /** Screen dp back to layout units, for taps. */
    fun toUnits(dp: Float): Float = if (pageScale > 0f) dp / pageScale else dp
}

object PageTransform {

    /**
     * The sheet for a page of [pageWidthUnits] x [pageHeightUnits] shown in a
     * viewport [viewportWidthDp] wide at [zoomScale]. 100 % means the page
     * width fills the viewport; a viewport of 0 (not measured yet) draws one
     * unit per dp. A page without a usable size takes the Letter fallback box
     * so the fallback keeps its own 8.5 x 11 ratio rather than a card guess.
     */
    fun sheetFor(pageWidthUnits: Float, pageHeightUnits: Float, viewportWidthDp: Float, zoomScale: Float): PageSheet {
        val width = if (pageWidthUnits > 0f) pageWidthUnits else PageStyleSpec.FALLBACK.widthDp
        val height = if (pageHeightUnits > 0f) pageHeightUnits else PageStyleSpec.FALLBACK.heightDp
        val zoom = if (zoomScale > 0f) zoomScale else 1f
        val fit = if (viewportWidthDp > 0f) viewportWidthDp / width else 1f
        return PageSheet(width, height, fit * zoom)
    }

    fun sheetFor(spec: PageStyleSpec, viewportWidthDp: Float, zoomScale: Float): PageSheet =
        sheetFor(spec.widthDp, spec.heightDp, viewportWidthDp, zoomScale)

    /**
     * Factor a point size is multiplied by before it is given to Compose as
     * `sp`, so that the glyphs land at [PageSheet.fontDp] dp regardless of the
     * user's font-scale setting: page text scales with the sheet, not with
     * the system text size, or the column would no longer be the column.
     */
    fun textScale(sheet: PageSheet, fontScale: Float): Float {
        val safeFontScale = if (fontScale > 0f) fontScale else 1f
        return LayoutUnits.UNITS_PER_POINT * sheet.pageScale / safeFontScale
    }
}
