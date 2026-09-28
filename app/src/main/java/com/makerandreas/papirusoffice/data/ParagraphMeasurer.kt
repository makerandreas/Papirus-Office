package com.makerandreas.papirusoffice.data

import java.text.BreakIterator
import java.util.Locale
import kotlin.math.floor

/** Lossless line/source mapping in layout units. No whitespace is reconstructed from words. */
internal class ParagraphMeasurer(
    private val source: AdvanceSource,
    private val hyphenator: HyphenationEngine?
) {
    private data class Glyph(val start: Int, val end: Int, val text: String, val metrics: TextMetrics)

    fun measure(index: Int, paragraph: OfficeParagraph, style: ParagraphStyle,
                styles: DocumentStyles, width: Float): ParagraphLayout {
        val text = paragraph.text
        val base = TextMetrics.forStyle(style, source)
        val metricCache = mutableMapOf(style to base)
        val runStyles = mutableListOf<Pair<Int, ParagraphStyle>>()
        if (paragraph.runs.joinToString("") { it.text } == text) {
            var end = 0
            paragraph.runs.forEach { run ->
                end += run.text.length
                runStyles += end to OfficeRuns.mergeRun(run, style, styles)
            }
        }
        val glyphs = mutableListOf<Glyph>()
        val chars = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
        var start = chars.first()
        var end = chars.next()
        var runIndex = 0
        while (end != BreakIterator.DONE) {
            while (runIndex < runStyles.size && start >= runStyles[runIndex].first) runIndex++
            val runStyle = runStyles.getOrNull(runIndex)?.second ?: style
            glyphs += Glyph(start, end, text.substring(start, end), metricCache.getOrPut(runStyle) { TextMetrics.forStyle(runStyle, source) })
            start = end
            end = chars.next()
        }
        val legalBreaks = mutableSetOf<Int>()
        val breaks = BreakIterator.getLineInstance(Locale.ROOT).apply { setText(text) }
        var boundary = breaks.first()
        while (boundary != BreakIterator.DONE) {
            legalBreaks += boundary
            boundary = breaks.next()
        }
        // Dictionary positions are source offsets. A visual hyphen never consumes a character.
        val dictionaryBreaks = mutableSetOf<Int>()
        if (hyphenator != null) {
            Regex("[\\p{L}]+").findAll(text).forEach { word ->
                hyphenator.hyphenationPoints(word.value).forEach { dictionaryBreaks += word.range.first + it }
            }
        }
        fun hard(g: Glyph) = g.text == "\n" || g.text == "\r\n" || g.text == "\r"
        fun widthOf(g: Glyph): Float = if (hard(g) || g.text == "\u00ad") 0f else g.metrics.widthOf(g.text)
        fun tabAdvance(glyphIndex: Int, x: Float): Float {
            val interval = style.defaultTabIntervalUnits.takeIf { it.isFinite() && it > 0f } ?: 48f
            val sorted = style.tabStops.sortedBy { it.positionUnits }
            val stop = sorted.firstOrNull { it.positionUnits > x + 0.001f && it.alignment != TabAlignment.CLEAR }
            var target = stop?.positionUnits ?: ((floor(x / interval) + 1) * interval)
            while (stop == null && sorted.any { it.alignment == TabAlignment.CLEAR && kotlin.math.abs(it.positionUnits - target) < 0.01f }) target += interval
            var following = 0f
            var decimal = 0f
            var foundDecimal = false
            var i = glyphIndex + 1
            while (i < glyphs.size && !hard(glyphs[i]) && glyphs[i].text != "\t") {
                if (!foundDecimal && glyphs[i].text == ".") { decimal = following; foundDecimal = true }
                following += widthOf(glyphs[i++])
            }
            val offset = when (stop?.alignment) {
                TabAlignment.RIGHT -> following
                TabAlignment.CENTER -> following / 2f
                TabAlignment.DECIMAL -> if (foundDecimal) decimal else following
                else -> 0f
            }
            return (target - x - offset).coerceAtLeast(0f)
        }
        val lines = mutableListOf<LineLayout>()
        var from = 0
        // One line for an empty paragraph and one empty line after a trailing hard newline.
        do {
            val origin = style.indentStartUnits + if (lines.isEmpty()) style.firstLineIndentUnits else 0f
            val capacity = (width - style.indentEndUnits - origin).coerceAtLeast(1f)
            var to = from
            var x = 0f
            var lastBreak = -1
            var selectedHyphen = false
            while (to < glyphs.size) {
                val g = glyphs[to]
                val advance = if (g.text == "\t") tabAdvance(to, origin + x) else widthOf(g)
                if (x + advance > capacity && to > from) {
                    if (lastBreak > from) {
                        to = lastBreak
                        selectedHyphen = glyphs[to - 1].text == "\u00ad" || glyphs[to - 1].end in dictionaryBreaks
                    }
                    break
                }
                x += advance
                to++
                if (hard(g)) break
                val hyphen = g.text == "\u00ad" || g.end in dictionaryBreaks
                if (g.end in legalBreaks || hyphen) {
                    if (!hyphen || x + g.metrics.widthOf("-") <= capacity) lastBreak = to
                }
            }
            // Oversized grapheme: consume it once rather than looping forever.
            if (to == from && from < glyphs.size) to++
            val sourceStart = glyphs.getOrNull(from)?.start ?: text.length
            val sourceEnd = glyphs.getOrNull(to - 1)?.end ?: sourceStart
            val offsets = mutableListOf(sourceStart)
            val positions = mutableListOf(0f)
            var measuredWidth = 0f
            var natural = base.naturalLineHeightUnits
            var fontSize = base.fontSizeUnits
            val display = StringBuilder()
            for (gIndex in from until to) {
                val g = glyphs[gIndex]
                measuredWidth += if (g.text == "\t") tabAdvance(gIndex, origin + measuredWidth) else widthOf(g)
                natural = maxOf(natural, g.metrics.naturalLineHeightUnits)
                fontSize = maxOf(fontSize, g.metrics.fontSizeUnits)
                if (!hard(g) && g.text != "\u00ad") display.append(g.text)
                offsets += g.end
                positions += measuredWidth
            }
            if (selectedHyphen) { display.append('-'); measuredWidth += base.widthOf("-") }
            val align = paragraph.alignment ?: style.alignment
            val alignmentOffset = when (align.lowercase(Locale.ROOT)) {
                "right", "end" -> (capacity - measuredWidth).coerceAtLeast(0f)
                "center" -> (capacity - measuredWidth).coerceAtLeast(0f) / 2f
                else -> 0f
            }
            val height = base.lineHeightFor(natural, fontSize).coerceAtLeast(1f)
            lines += LineLayout(display.toString(), sliceRuns(paragraph, sourceStart, sourceEnd), measuredWidth,
                height, (height - natural) / 2f + fontSize, sourceStart, sourceEnd,
                origin + alignmentOffset, offsets, positions, selectedHyphen)
            from = to
        } while (from < glyphs.size)
        if (glyphs.lastOrNull()?.let(::hard) == true) {
            lines += LineLayout("", height = base.lineHeightUnits, baseline = base.fontSizeUnits,
                startOffset = text.length, endOffset = text.length, left = style.indentStartUnits,
                caretOffsets = listOf(text.length), caretAdvances = listOf(0f))
        }
        val height = lines.sumOf { it.height.toDouble() }.toFloat()
        return ParagraphLayout(index, lines, width, height, lines.first().baseline, OfficeRect(0f, 0f, width, height))
    }

    companion object {
        fun sliceRuns(paragraph: OfficeParagraph, start: Int, end: Int): List<OfficeTextRun> {
            if (paragraph.runs.joinToString("") { it.text } != paragraph.text) return emptyList()
            var offset = 0
            return paragraph.runs.mapNotNull { run ->
                val from = maxOf(start, offset)
                val to = minOf(end, offset + run.text.length)
                val result = if (from < to) run.copy(text = run.text.substring(from - offset, to - offset)) else null
                offset += run.text.length
                result
            }
        }
    }
}
