package com.makerandreas.papirusoffice.data

import android.content.Context

/**
 * TeX-format hyphenation dictionary (assets/hyphenation/en_us.hyph).
 * Pattern lines look like ".a2ch4": a dot, then letters interleaved with
 * digits; a digit marks a break position after that many letters and its
 * value is the pattern priority. Lookup is longest match per start
 * position, highest priority wins, and breaks respect the dictionary's
 * LEFTHYPHENMIN / RIGHYPHENMIN (defaults 2/3).
 */
class HyphenationEngine private constructor(
    private val patterns: Map<String, Pattern>,
    private val leftMin: Int,
    private val rightMin: Int
) {
    private data class Pattern(val points: IntArray, val priority: Int)

    /**
     * Allowed break positions in [word]; position k means "between chars
     * k-1 and k" (1-based, so 0 and length are never returned).
     */
    fun hyphenationPoints(word: String): List<Int> {
        val w = word.lowercase()
        val n = w.length
        if (n < leftMin + rightMin) return emptyList()

        val best = IntArray(n) { 0 }
        for (i in 0 until n) {
            var matched: Pattern? = null
            var prefix = ""
            for (len in 1..minOf(MAX_PATTERN_LEN, n - i)) {
                prefix += w[i + len - 1]
                // A hit does not stop the scan: a longer pattern at the same
                // position overrides a shorter one (TeX longest-match rule).
                matched = patterns[prefix] ?: matched
            }
            val pattern = matched ?: continue
            for (p in pattern.points) {
                val pos = i + p
                if (pos in 1 until n && pattern.priority > best[pos]) {
                    best[pos] = pattern.priority
                }
            }
        }
        return (leftMin until n - rightMin + 1).filter { best[it] > 0 }
    }

    /** First break whose hyphenated head fits [maxWidth], or null. */
    fun firstFittingBreak(word: String, maxWidth: Float, measure: (String) -> Float): Int? {
        for (k in hyphenationPoints(word)) {
            if (measure(word.substring(0, k)) <= maxWidth) return k
        }
        return null
    }

    companion object {
        // Longest pattern in the bundled en_us dictionary is 26 letters.
        private const val MAX_PATTERN_LEN = 32

        /**
         * Parses dictionary lines. Tolerates the "UTF-8" declaration line;
         * LEFTHYPHENMIN / RIGHYPHENMIN override the 2/3 defaults.
         */
        fun parse(lines: List<String>): HyphenationEngine {
            var leftMin = 2
            var rightMin = 3
            val patterns = HashMap<String, Pattern>()
            for (raw in lines) {
                val line = raw.trim()
                when {
                    line.startsWith("LEFTHYPHENMIN", ignoreCase = true) ->
                        line.substringAfter(' ').trim().toIntOrNull()?.let { leftMin = it }
                    line.startsWith("RIGHTHYPHENMIN", ignoreCase = true) ->
                        line.substringAfter(' ').trim().toIntOrNull()?.let { rightMin = it }
                    line.startsWith('.') -> parsePattern(line.substring(1), patterns)
                    else -> Unit
                }
            }
            return HyphenationEngine(patterns, leftMin, rightMin)
        }

        private fun parsePattern(body: String, patterns: HashMap<String, Pattern>) {
            val letters = StringBuilder()
            val points = mutableListOf<Int>()
            var priority = 0
            for (ch in body) {
                when {
                    ch.isDigit() -> {
                        points.add(letters.length)
                        priority = maxOf(priority, ch.digitToInt())
                    }
                    ch.isLetter() -> letters.append(ch)
                }
            }
            val key = letters.toString()
            if (key.isEmpty()) return
            val existing = patterns[key]
            if (existing == null || priority >= existing.priority) {
                patterns[key] = Pattern(points.toIntArray(), priority)
            }
        }

        fun loadFile(file: java.io.File): HyphenationEngine = parse(file.readLines())

        /** Loads the bundled en_us dictionary straight from app assets. */
        fun loadDefault(context: Context): HyphenationEngine {
            return context.assets.open("hyphenation/en_us.hyph").bufferedReader().use {
                parse(it.readLines())
            }
        }
    }
}
