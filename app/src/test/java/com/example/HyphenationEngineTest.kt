package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.HyphenationEngine
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HyphenationEngineTest {

    // --- Dictionary parsing and lookup rules ---

    @Test
    fun parsesPatternsAndMarksBreakPositions() {
        val engine = HyphenationEngine.parse(
            listOf("UTF-8", "LEFTHYPHENMIN 1", "RIGHTHYPHENMIN 1", ".a2ch4")
        )
        // "ach": pattern points after char 1 (a|ch) and char 3 (end, invalid)
        assertEquals(listOf(1), engine.hyphenationPoints("ach"))
    }

    @Test
    fun respectsDefaultLeftAndRightMinima() {
        val engine = HyphenationEngine.parse(listOf(".a2ch4"))
        // n=3 cannot satisfy left 2 + right 3, so no break at all
        assertTrue(engine.hyphenationPoints("ach").isEmpty())
    }

    @Test
    fun longestPatternAtPositionWins() {
        val engine = HyphenationEngine.parse(
            listOf("LEFTHYPHENMIN 1", "RIGHTHYPHENMIN 1", ".a1b", ".ab2c")
        )
        // "abc": the shorter "ab" pattern would break at 1, but the longer
        // "abc" pattern breaks at 2 and must win
        assertEquals(listOf(2), engine.hyphenationPoints("abc"))
    }

    @Test
    fun higherPriorityPatternReplacesLower() {
        val engine = HyphenationEngine.parse(
            listOf("LEFTHYPHENMIN 1", "RIGHTHYPHENMIN 1", ".a1b", ".ab2")
        )
        // Both patterns cover "ab" inside "xabc"; priority 2 points at 3
        assertEquals(listOf(3), engine.hyphenationPoints("xabc"))
    }

    @Test
    fun lookupIsCaseInsensitive() {
        val engine = HyphenationEngine.parse(listOf("LEFTHYPHENMIN 1", "RIGHTHYPHENMIN 1", ".a2ch4"))
        assertEquals(listOf(1), engine.hyphenationPoints("ACH"))
    }

    @Test
    fun firstFittingBreakPrefersLeftmostHeadThatFits() {
        val engine = HyphenationEngine.parse(listOf("LEFTHYPHENMIN 1", "RIGHTHYPHENMIN 1", ".a2ch4"))
        val word = "achx"
        assertEquals(listOf(1, 3), engine.hyphenationPoints(word))
        assertEquals(1, engine.firstFittingBreak(word, 1f) { it.length.toFloat() })
        assertNull(engine.firstFittingBreak(word, 0f) { it.length.toFloat() })
    }

    // --- LayoutEngine integration (JVM fallback metrics: 8 units per char) ---

    @Test
    fun hyphenationSplitsOverflowingWordIntoLines() {
        // Pattern of 23 x's (within the 32-letter scan window) with a break
        // point every two letters.
        val pattern = buildString { repeat(11) { append("x2") }; append("x") }
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

    // --- Real bundled dictionary + Sample-5 window with the toggle on ---

    private fun findTestFile(vararg candidates: String): File {
        val bases = listOf("", "../", "../../")
        for (candidate in candidates) {
            for (base in bases) {
                val f = File(base + candidate)
                if (f.exists() && f.length() > 0) return f
            }
        }
        return File(candidates.first())
    }

    @Test
    fun bundledDictionaryFindsBreaksInRealWords() {
        val file = findTestFile(
            "src/main/assets/hyphenation/en_us.hyph",
            "../src/main/assets/hyphenation/en_us.hyph"
        )
        assertTrue("en_us.hyph must exist (${file.absolutePath})", file.exists())
        val engine = HyphenationEngine.loadFile(file)
        assertTrue("en_us dictionary should find breaks in 'hyphenation'",
            engine.hyphenationPoints("hyphenation").isNotEmpty())
        assertTrue("en_us dictionary should find breaks in 'Internationalization'",
            engine.hyphenationPoints("Internationalization").isNotEmpty())
    }

    @Test
    fun dictionaryLoadsFromAppAssets() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = HyphenationEngine.loadDefault(context)
        assertTrue(engine.hyphenationPoints("Internationalization").isNotEmpty())
    }

    @Test
    fun sample5PageCountStaysInWindowWithHyphenationOn() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile("tests/inky/Sample-5.odt")
        assertTrue("Sample-5.odt must exist (${file.absolutePath})", file.exists() && file.length() > 0)
        val parsed = parser.parseDocument(file, bypassCache = true)
        assertTrue("Sample-5.odt parse failed: ${parsed.failureReason}", !parsed.isParsingFailed)
        val officeDoc = parsed.toOfficeDocument()
        val spec = officeDoc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
        val dictionary = findTestFile(
            "src/main/assets/hyphenation/en_us.hyph",
            "../src/main/assets/hyphenation/en_us.hyph"
        )
        val layout = LayoutEngine(spec, hyphenator = HyphenationEngine.loadFile(dictionary))
            .performLayout(officeDoc)
        assertTrue(
            "Sample-5.odt with hyphenation on laid out to ${layout.pages.size} pages; interim window 12..30",
            layout.pages.size in 12..30
        )
    }
}
