package com.makerandreas.papirusoffice.data.util

import com.makerandreas.papirusoffice.data.LayoutUnits

/**
 * ODF length entry point kept for its call sites (page layout, frames,
 * tests). The arithmetic lives in [LayoutUnits], the one converter for every
 * format, so both readers keep reading the same length the same way.
 */
object OdfLength {
    fun toLayoutUnits(raw: String?, fallback: Float = 0f): Float =
        LayoutUnits.parseLength(raw, fallback)

    /** OOXML lengths are twips at 1440 per inch (ECMA-376 §17.18.65). */
    fun twipsToLayoutUnits(twips: Int): Float = LayoutUnits.twipsToUnits(twips)
}
