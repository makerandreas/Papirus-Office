package com.makerandreas.papirusoffice.data.util

import java.util.Locale

/**
 * Converts absolute ODF lengths (ODF 1.4 Part 1, "length" datatype) into the
 * unit space the LayoutEngine paginates in: CSS px at 96 units per inch, so
 * US Letter 8.5x11in is 816x1056. Shared by ODF page-layout parsing and ODF
 * frame sizing so both read the same length the same way.
 */
object OdfLength {
    private const val UNITS_PER_INCH = 96f
    private val LEADING_NUMBER = Regex("^([-+]?[0-9]*\\.?[0-9]+)")

    fun toLayoutUnits(raw: String?, fallback: Float = 0f): Float {
        val text = raw?.lowercase(Locale.ROOT)?.trim().orEmpty()
        val number = LEADING_NUMBER.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: return fallback
        return when {
            text.endsWith("in") -> number * UNITS_PER_INCH
            text.endsWith("cm") -> number * UNITS_PER_INCH / 2.54f
            text.endsWith("mm") -> number * UNITS_PER_INCH / 25.4f
            text.endsWith("pt") -> number * UNITS_PER_INCH / 72f
            text.endsWith("pc") -> number * UNITS_PER_INCH / 6f
            else -> number
        }
    }

    /** OOXML lengths are twips at 1440 per inch (ECMA-376 §17.18.65). */
    fun twipsToLayoutUnits(twips: Int): Float = twips * UNITS_PER_INCH / 1440f
}
