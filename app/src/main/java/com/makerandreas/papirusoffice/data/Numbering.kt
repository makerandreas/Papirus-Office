package com.makerandreas.papirusoffice.data

import java.util.Locale

/**
 * Shared numbering model (E-EN-4) for ODF `<text:list-style>` / `<text:outline-style>`
 * (Plan 7A) and OOXML `word/numbering.xml` `<w:abstractNum>` / `<w:num>` (Plan 8).
 */
data class NumberingLevelSpec(
    val level: Int,
    val isBullet: Boolean = false,
    /** ODF `style:num-format` ("1", "a", "A", "i", "I", or "" for unnumbered). */
    val numFormat: String = "1",
    val numPrefix: String = "",
    val numSuffix: String = "",
    val displayLevels: Int = 1,
    val startValue: Int = 1,
    val bulletChar: String = "\u2022",
    val textStyleName: String? = null,
    val fontFamily: String? = null,
    val fontSizeSp: Float? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val colorHex: String? = null,
    val indentStartUnits: Float? = null,
    val firstLineIndentUnits: Float? = null,
    val tabStopPositionUnits: Float? = null,
    val labelFollowedBy: String = "listtab"
)

data class NumberingSpec(
    val name: String,
    val displayName: String? = null,
    val isOutline: Boolean = false,
    val levels: Map<Int, NumberingLevelSpec> = emptyMap()
) {
    fun level(oneBasedLevel: Int): NumberingLevelSpec? =
        levels[oneBasedLevel.coerceIn(1, MAX_NUMBERING_LEVELS)]

    companion object {
        const val MAX_NUMBERING_LEVELS = 10
    }
}

typealias CounterState = NumberingCounterState

/**
 * Per-level counter state across levels 1..10 for a single list or outline sequence.
 * Advancing level N increments level N (or initializes it to `startValue`) and resets
 * all deeper levels `(N + 1)..10` to 0 so subsequent sub-lists restart at their start value.
 */
class NumberingCounterState {
    private val counts = IntArray(NumberingSpec.MAX_NUMBERING_LEVELS + 1)

    fun currentCount(oneBasedLevel: Int): Int {
        val lvl = oneBasedLevel.coerceIn(1, NumberingSpec.MAX_NUMBERING_LEVELS)
        return counts[lvl]
    }

    fun reset() {
        counts.fill(0)
    }

    fun resetFrom(oneBasedLevel: Int) {
        val start = oneBasedLevel.coerceIn(1, NumberingSpec.MAX_NUMBERING_LEVELS)
        for (lvl in start..NumberingSpec.MAX_NUMBERING_LEVELS) {
            counts[lvl] = 0
        }
    }

    /**
     * Advances the counter at [oneBasedLevel] according to [spec] and returns the raw
     * formatted label (without trailing separator space). Returns `""` when the level's
     * `numFormat` is empty (`""` or `"none"`).
     */
    fun advance(
        spec: NumberingSpec,
        oneBasedLevel: Int,
        startValueOverride: Int? = null
    ): String {
        val lvl = oneBasedLevel.coerceIn(1, NumberingSpec.MAX_NUMBERING_LEVELS)
        val levelSpec = spec.level(lvl)

        // Ensure ancestor levels have at least their startValue when a document jumps
        // directly to a deeper level (for example, level 2 before any level 1).
        for (ancestor in 1 until lvl) {
            if (counts[ancestor] == 0) {
                counts[ancestor] = (spec.level(ancestor)?.startValue ?: 1).coerceAtLeast(1)
            }
        }

        if (levelSpec != null && levelSpec.isBullet) {
            // Reset deeper levels below this bullet level.
            for (deeper in (lvl + 1)..NumberingSpec.MAX_NUMBERING_LEVELS) {
                counts[deeper] = 0
            }
            return NumberingFormatter.formatBullet(levelSpec)
        }

        val initial = (levelSpec?.startValue ?: 1).coerceAtLeast(0)
        counts[lvl] = when {
            startValueOverride != null -> startValueOverride.coerceAtLeast(0)
            counts[lvl] == 0 -> initial
            else -> counts[lvl] + 1
        }
        for (deeper in (lvl + 1)..NumberingSpec.MAX_NUMBERING_LEVELS) {
            counts[deeper] = 0
        }

        if (levelSpec == null) {
            return "${counts[lvl]}."
        }
        return NumberingFormatter.formatNumberedLevel(spec, lvl, counts)
    }
}

object NumberingFormatter {

    fun formatBullet(levelSpec: NumberingLevelSpec): String {
        val glyph = levelSpec.bulletChar.ifEmpty { "\u2022" }
        // Collabora writes style:num-suffix="·" alongside text:bullet-char="·" on
        // <text:list-level-style-bullet>; avoid doubling the bullet glyph.
        val suffix = if (levelSpec.numSuffix == glyph) "" else levelSpec.numSuffix
        return "${levelSpec.numPrefix}$glyph$suffix"
    }

    fun formatNumberedLevel(
        spec: NumberingSpec,
        oneBasedLevel: Int,
        counts: IntArray
    ): String {
        val lvl = oneBasedLevel.coerceIn(1, NumberingSpec.MAX_NUMBERING_LEVELS)
        val levelSpec = spec.level(lvl) ?: return ""
        val fmt = levelSpec.numFormat.trim()
        if (fmt.isEmpty() || fmt.equals("none", ignoreCase = true)) {
            return ""
        }
        val displayCount = levelSpec.displayLevels.coerceIn(1, lvl)
        val firstLevel = (lvl - displayCount + 1).coerceAtLeast(1)
        val segments = ArrayList<String>(displayCount)
        for (k in firstLevel..lvl) {
            val kSpec = spec.level(k)
            val kValue = counts[k].coerceAtLeast(kSpec?.startValue ?: 1)
            // In ODF multi-level display (e.g. level 1 = "I" for "BAB I", level 2 = "1"
            // with display-levels="2" for "1.1"), sub-levels with arabic "1" format all
            // displayed ancestor levels in arabic unless the current level uses its own format.
            val effectiveFormat = if (k < lvl && fmt == "1") {
                "1"
            } else {
                kSpec?.numFormat?.takeIf { it.isNotBlank() } ?: fmt
            }
            val formatted = formatOrdinal(kValue, effectiveFormat)
            if (formatted.isNotEmpty()) {
                segments.add(formatted)
            }
        }
        if (segments.isEmpty()) return ""
        val core = segments.joinToString(".")
        return "${levelSpec.numPrefix}$core${levelSpec.numSuffix}"
    }

    fun formatOrdinal(value: Int, numFormat: String): String {
        val fmt = numFormat.trim()
        if (fmt.isEmpty() || fmt.equals("none", ignoreCase = true)) return ""
        val safe = value.coerceAtLeast(0)
        return when (fmt) {
            "1" -> safe.toString()
            "a" -> toAlphabetic(safe, lower = true)
            "A" -> toAlphabetic(safe, lower = false)
            "i" -> toRoman(safe).lowercase(Locale.ROOT)
            "I" -> toRoman(safe)
            else -> safe.toString()
        }
    }

    private fun toAlphabetic(value: Int, lower: Boolean): String {
        if (value <= 0) return value.toString()
        val sb = StringBuilder()
        var n = value
        while (n > 0) {
            val rem = (n - 1) % 26
            val base = if (lower) 'a'.code else 'A'.code
            sb.append((base + rem).toChar())
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }

    private val ROMAN_VALUES = intArrayOf(
        1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1
    )
    private val ROMAN_SYMBOLS = arrayOf(
        "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"
    )

    private fun toRoman(value: Int): String {
        if (value <= 0 || value > 3999) return value.toString()
        val sb = StringBuilder()
        var remaining = value
        for (i in ROMAN_VALUES.indices) {
            while (remaining >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_SYMBOLS[i])
                remaining -= ROMAN_VALUES[i]
            }
        }
        return sb.toString()
    }
}
