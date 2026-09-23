package com.example

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.HyphenationEngine
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageStyleSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Plain JUnit (no Robolectric): android.graphics.Paint is a throwing stub
// here, so LayoutEngine uses its deterministic 8-units-per-char fallback
// measure and absolute-width wrap scenarios are testable.
class HyphenationLayoutTest {

    @Test
    fun hyphenationSplitsOverflowingWordIntoLines() {
        // Dictionary pattern for a run of x's with a break after every letter
        // (the leading dot is mandatory for parse to accept the line).
        val pattern = "." + buildString { repeat(11) { append("x2") }; append("x") }
        val engine = HyphenationEngine.parse(listOf(pattern))
        val doc = OfficeDocument(
            body = DocumentBody(listOf(OfficeParagraph(text = "x".repeat(95))))
        )
        val spec = PageStyleSpec.FALLBACK
        val plain = LayoutEngine(spec).performLayout(doc)
        val hyphenated = LayoutEngine(spec, hyphenator = engine).performLayout(doc)

        val plainLines = plain.pages.first().elements.first().paragraphLayout!!.lines.size
        val hyphenLines = hyphenated.pages.first().elements.first().paragraphLayout!!.lines.size
        assertEquals("no hyphenator keeps the word on one overflowing line", 1, plainLines)
        assertTrue("hyphenation must break the word across lines, got $hyphenLines", hyphenLines > 1)
    }

    @Test
    fun defaultPathMatchesHistoricalWrapping() {
        // The off-by-default path must reproduce legacy output exactly:
        // "hello" fills line 1, the 95-char word overflows alone on line 2.
        val doc = OfficeDocument(
            body = DocumentBody(listOf(OfficeParagraph(text = "hello " + "x".repeat(95))))
        )
        val layout = LayoutEngine(PageStyleSpec.FALLBACK).performLayout(doc)
        val para = layout.pages.first().elements.first().paragraphLayout!!
        assertEquals(2, para.lines.size)
        assertEquals("hello", para.lines.first().text)
    }
}
