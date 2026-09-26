package com.makerandreas.papirusoffice.data

import java.util.Locale

/**
 * The one unit space the paginator works in: CSS px at 96 per inch, so US
 * Letter is 816 x 1056 and A4 is 793.7 x 1122.5. [PageStyleSpec],
 * [LayoutEngine] and the page-stack renderer already speak this space; every
 * length that enters the layout model from a file format converts here and
 * nowhere else (plan 5A, roadmap E-EN-1).
 *
 * Sources per format:
 *  - ODF lengths (ODF 1.4 Part 3 §18.3.18 "length"): `2.54cm`, `1in`, `12pt`, `10px`.
 *  - OOXML twips (ECMA-376 §17.18.65 ST_TwipsMeasure): 1440 per inch.
 *  - OOXML EMU (ECMA-376 §20.1.10.16 ST_Coordinate): 914 400 per inch.
 *  - Font sizes are points in both formats (`fo:font-size`, `w:sz` half-points).
 *
 * Naming: the model still calls its fields `*Dp` and `fontSizeSp` for
 * historical reasons; those numbers are layout units, not Android dp/sp. The
 * renderer maps units to screen dp through one page scale (roadmap E-7).
 */
object LayoutUnits {
    const val UNITS_PER_INCH = 96f
    const val POINTS_PER_INCH = 72f
    const val TWIPS_PER_INCH = 1440f
    const val EMU_PER_INCH = 914400f
    const val CM_PER_INCH = 2.54f
    const val MM_PER_INCH = 25.4f
    const val PICAS_PER_INCH = 6f

    /** 914400 / 96 = 9525 EMU per layout unit; also what `wp:extent` divides by. */
    const val EMU_PER_UNIT = EMU_PER_INCH / UNITS_PER_INCH

    /** 96 / 72: a 12 pt glyph box is 16 layout units tall. */
    const val UNITS_PER_POINT = UNITS_PER_INCH / POINTS_PER_INCH

    private val LEADING_NUMBER = Regex("^([-+]?[0-9]*\\.?[0-9]+)")

    fun ptToUnits(points: Float): Float = points * UNITS_PER_POINT

    fun unitsToPt(units: Float): Float = units / UNITS_PER_POINT

    fun inToUnits(inches: Float): Float = inches * UNITS_PER_INCH

    fun cmToUnits(cm: Float): Float = cm * UNITS_PER_INCH / CM_PER_INCH

    fun mmToUnits(mm: Float): Float = mm * UNITS_PER_INCH / MM_PER_INCH

    fun twipsToUnits(twips: Int): Float = twips * UNITS_PER_INCH / TWIPS_PER_INCH

    fun twipsToUnits(twips: Float): Float = twips * UNITS_PER_INCH / TWIPS_PER_INCH

    /** OOXML `w:sz`/`w:szCs` are half-points. */
    fun halfPointsToPt(halfPoints: Int): Float = halfPoints / 2f

    fun emuToUnits(emu: Long): Float = emu / EMU_PER_UNIT

    fun unitsToEmu(units: Float): Long = (units * EMU_PER_UNIT).toLong()

    /**
     * Parses an absolute ODF/CSS length. A bare number is taken as layout
     * units (the `px` case), so `"96"` and `"96px"` both mean one inch.
     * Percentages and malformed input return [fallback]; relative lengths are
     * not lengths in this space.
     */
    fun parseLength(raw: String?, fallback: Float = 0f): Float {
        val text = raw?.lowercase(Locale.ROOT)?.trim().orEmpty()
        if (text.isEmpty() || text.endsWith("%")) return fallback
        val number = LEADING_NUMBER.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: return fallback
        return when {
            text.endsWith("in") -> inToUnits(number)
            text.endsWith("cm") -> cmToUnits(number)
            text.endsWith("mm") -> mmToUnits(number)
            text.endsWith("pt") -> ptToUnits(number)
            text.endsWith("pc") -> number * UNITS_PER_INCH / PICAS_PER_INCH
            text.endsWith("px") -> number
            else -> number
        }
    }

    /**
     * Parses a font size that stays in points (`fo:font-size="12pt"`,
     * `"0.5cm"`). Percentages return null because they are relative to the
     * parent style, which the caller resolves. Non-positive values are
     * rejected the same way the ODF reader always did.
     */
    fun parsePoints(raw: String?): Float? {
        if (raw.isNullOrBlank()) return null
        val text = raw.trim().lowercase(Locale.ROOT)
        if (text.endsWith("%")) return null
        val number = LEADING_NUMBER.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        if (number <= 0f) return null
        return when {
            text.endsWith("in") -> number * POINTS_PER_INCH
            text.endsWith("cm") -> number * POINTS_PER_INCH / CM_PER_INCH
            text.endsWith("mm") -> number * POINTS_PER_INCH / MM_PER_INCH
            text.endsWith("px") -> unitsToPt(number)
            else -> number
        }
    }

    /**
     * ODF `fo:line-height` / OOXML `w:line` with `lineRule="auto"` expressed as
     * a factor of the single line height: `"115%"` and `276` both mean 1.15.
     * Returns null for absolute values (those are heights, not factors).
     */
    fun parseLineHeightFactor(raw: String?): Float? {
        val text = raw?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (!text.endsWith("%")) return null
        val number = LEADING_NUMBER.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        if (number <= 0f) return null
        return number / 100f
    }

    /** OOXML `w:line` in 240ths of a line when `w:lineRule` is `auto`. */
    fun lineTwentiethsToFactor(line: Int): Float = line / 240f
}
