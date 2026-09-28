package com.example

import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Test

class ParagraphMetricsTest {
    private val spec = PageStyleSpec.FALLBACK.copy(widthDp = 200f, marginStartDp = 20f, marginEndDp = 20f)
    private val body = ParagraphStyle("body", fontSizeSp = 12f, fontFamily = "Times New Roman")
    private fun styles(style: ParagraphStyle = body) = DocumentStyles(defaultParagraphStyle = style)
    private fun engine() = LayoutEngine(spec, advanceSource = TableAdvanceSource)

    @Test fun measuredUnitsAndVariableWidthGlyphsReplaceCharacterCounting() {
        val e = engine()
        val wide = e.layoutParagraph(0, OfficeParagraph("MMMM"), styles())
        val narrow = e.layoutParagraph(1, OfficeParagraph("iiii"), styles())
        assertTrue(wide.lines.single().width > narrow.lines.single().width * 2)
        assertEquals(TextMetrics.forStyle(body, TableAdvanceSource).lineHeightUnits, wide.height, 0.001f)
        assertTrue(e.measurementProbe().startsWith("table at "))
    }

    @Test fun whitespaceNewlinesAndUnicodeHaveLosslessSourceRanges() {
        val text = "  a   b\t🙂e\u0301\n\n" + "longword".repeat(20) + "\n"
        val p = engine().layoutParagraph(0, OfficeParagraph(text), styles())
        assertEquals(text, p.lines.joinToString("") { text.substring(it.startOffset, it.endOffset) })
        assertEquals(text.length, p.lines.last().endOffset)
        assertEquals("", p.lines.last().text)
        assertTrue(p.lines.size > 3)
        p.lines.zipWithNext().forEach { (a,b) -> assertEquals(a.endOffset, b.startOffset) }
        val emoji = text.indexOf("🙂")
        val accent = text.indexOf("e\u0301")
        val carets = p.lines.flatMap { it.caretOffsets }
        assertFalse(carets.contains(emoji + 1))
        assertFalse(carets.contains(accent + 1))
    }

    @Test fun indentsAffectAvailableWidthAndLineOrigin() {
        val p = engine().layoutParagraph(0, OfficeParagraph("word ".repeat(30)),
            styles(body.copy(indentStartUnits = 20f, indentEndUnits = 10f, firstLineIndentUnits = 15f)))
        assertEquals(35f, p.lines.first().left, 0f)
        assertTrue(p.lines.drop(1).all { it.left == 20f })
        assertTrue(p.lines.all { it.left + it.width <= spec.contentWidthDp - 10f + 0.01f })
    }

    @Test fun tabsAdvanceFromCurrentPositionAndClearSkipsDefaultStop() {
        val p = engine().layoutParagraph(0, OfficeParagraph("a\tb\tc"), styles(body.copy(
            tabStops = listOf(ParagraphTabStop(48f, TabAlignment.CLEAR), ParagraphTabStop(110f)))))
        assertEquals(110f, p.lines.first().caretAdvances[2], 0.001f)
        val defaults = engine().layoutParagraph(0, OfficeParagraph("a\tb"), styles(body.copy(
            tabStops = listOf(ParagraphTabStop(48f, TabAlignment.CLEAR)))))
        assertEquals(96f, defaults.lines.single().caretAdvances[2], 0.001f)
    }

    @Test fun rightAndDecimalTabsAccountForFollowingText() {
        for (alignment in listOf(TabAlignment.RIGHT, TabAlignment.CENTER, TabAlignment.DECIMAL)) {
            val line = engine().layoutParagraph(0, OfficeParagraph("\t12.3"), styles(body.copy(
                tabStops = listOf(ParagraphTabStop(100f, alignment))))).lines.single()
            val metrics = TextMetrics.forStyle(body, TableAdvanceSource)
            val offset = when (alignment) {
                TabAlignment.RIGHT -> metrics.widthOf("12.3")
                TabAlignment.CENTER -> metrics.widthOf("12.3") / 2
                else -> metrics.widthOf("12")
            }
            assertEquals(100f - offset, line.caretAdvances[1], 0.01f)
        }
    }

    @Test fun mixedRunsSetMaximumLineMetricsAndSlicedRunRanges() {
        val p = OfficeParagraph("small BIG", runs = listOf(OfficeTextRun("small "), OfficeTextRun("BIG", characterStyle = "large")))
        val styles = styles().copy(characterStyles = mapOf("large" to CharacterStyle("large", fontSizeSp = 24f)))
        val line = engine().layoutParagraph(0, p, styles).lines.single()
        assertEquals(32f * 1.107f, line.height, 0.001f)
        assertEquals(p.text, line.runs.joinToString("") { it.text })
        assertTrue(line.width > TextMetrics.forStyle(body, TableAdvanceSource).widthOf(p.text))
    }

    @Test fun cacheInvalidatesTextStyleRunAndWidthAndMatchesForcedRebuild() {
        val e = engine()
        val p = OfficeParagraph("some text ".repeat(15))
        val a = e.layoutParagraph(0, p, styles())
        assertSame(a, e.layoutParagraph(0, p, styles()))
        assertEquals(1, e.measuredParagraphCount)
        val changed = p.copy(text = p.text + " changed")
        val b = e.layoutParagraph(0, changed, styles())
        assertEquals(b, e.layoutParagraph(0, changed, styles(), forceRebuild = true))
        val enlarged = styles(body.copy(fontSizeSp = 24f))
        assertTrue(e.layoutParagraph(0, p, enlarged).height > a.height)
        val run = p.copy(runs = listOf(OfficeTextRun(p.text, characterStyle = "large")))
        val withRun = styles().copy(characterStyles = mapOf("large" to CharacterStyle("large", fontSizeSp = 30f)))
        assertTrue(e.layoutParagraph(0, run, withRun).height > a.height)
        val wider = LayoutEngine(spec.copy(widthDp = 400f), advanceSource = TableAdvanceSource)
        assertTrue(wider.layoutParagraph(0, p, styles()).height < a.height)
    }

    @Test fun emptyParagraphHasOneMeasuredLine() {
        val p = engine().layoutParagraph(0, OfficeParagraph(""), styles())
        assertEquals(1, p.lines.size)
        assertEquals(0, p.lines.single().endOffset)
        assertEquals(TextMetrics.forStyle(body, TableAdvanceSource).lineHeightUnits, p.height, 0f)
    }

    @Test fun hitTestSupportsDirectParagraphsAndMeasuredCaretAdvances() {
        val e = engine()
        val p = OfficeParagraph("Wiii")
        val result = e.performLayout(OfficeDocument(styles = styles(), body = DocumentBody(listOf(p))))
        val placed = result.pages.single().elements.single()
        val line = placed.paragraphLayout!!.lines.single()
        val hit = e.hitTest(placed.bounds.left + line.caretAdvances[1], placed.bounds.top + 1f, result.pages)!!
        assertEquals(1, hit.characterOffset)
        assertEquals(0, hit.elementIndex)
    }

    @Test fun paragraphSpacingCollapsesForDocxAndSumsForOdf() {
        for (collapse in listOf(true, false)) {
            val s = styles(body.copy(spaceBeforeUnits = 4f, spaceAfterUnits = 12f, collapseSpacing = collapse))
            val result = engine().performLayout(OfficeDocument(styles = s, body = DocumentBody(listOf(OfficeParagraph("a"), OfficeParagraph("b")))))
            val placed = result.pages.single().elements
            assertEquals(if (collapse) 12f else 16f, placed[1].bounds.top - placed[0].bounds.bottom, 0.001f)
        }
    }
}
