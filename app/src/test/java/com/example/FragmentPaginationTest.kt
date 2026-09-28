package com.example

import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Test

class FragmentPaginationTest {
    private val page = PageStyleSpec.FALLBACK.copy(widthDp = 200f, heightDp = 180f,
        marginTopDp = 20f, marginBottomDp = 20f, marginStartDp = 20f, marginEndDp = 20f)
    private val style = ParagraphStyle("body", fontSizeSp = 12f, fontFamily = "Times New Roman", orphans = 2, widows = 2)
    private fun doc(vararg elements: OfficeElement, s: ParagraphStyle = style) =
        OfficeDocument(styles = DocumentStyles(defaultParagraphStyle = s), body = DocumentBody(elements.toList()))
    private fun engine() = LayoutEngine(page, advanceSource = TableAdvanceSource)

    @Test fun longParagraphSplitsIntoLosslessFragmentsWithinBody() {
        val text = "paragraph with variable widths MiWi ".repeat(100)
        val e = engine()
        val result = e.performLayout(doc(OfficeParagraph(text)))
        assertTrue(result.pages.size > 2)
        val fragments = result.pages.flatMap { it.elements }
        assertEquals(text, fragments.joinToString("") { text.substring(it.sourceStart, it.sourceEnd) })
        assertEquals(1, result.elementPageIndex[0])
        assertEquals((1..result.pages.size).toList(), result.elementPages[0])
        fragments.forEachIndexed { i, f ->
            assertEquals(0, f.elementIndex)
            assertEquals(i > 0, f.continuesBefore)
            assertEquals(i < fragments.lastIndex, f.continuesAfter)
            assertTrue(f.bounds.top >= page.bodyTopDp)
            assertTrue(f.bounds.bottom <= page.bodyBottomDp + 0.001f)
            assertTrue(f.paragraphLayout!!.lines.size >= 2)
        }
        assertEquals(result, e.performLayout(doc(OfficeParagraph(text)), forceRebuildAll = true))
    }

    @Test fun spacingDoesNotRepeatOnContinuations() {
        val result = engine().performLayout(doc(OfficeParagraph("word ".repeat(400)),
            s = style.copy(spaceBeforeUnits = 15f, spaceAfterUnits = 20f)))
        result.pages.forEach { assertEquals(page.bodyTopDp, it.elements.first().bounds.top, 0f) }
    }

    @Test fun keepNextMovesAHeadingWithFollowingParagraph() {
        val styles = DocumentStyles(defaultParagraphStyle = style, paragraphStyles = mapOf("heading" to style.copy(name = "heading", keepWithNext = true)))
        val doc = OfficeDocument(styles = styles, body = DocumentBody(listOf(
            OfficeParagraph("a\nb\nc\nd\ne\nf"), OfficeHeading("Heading", "heading"), OfficeParagraph("following text\nsecond line"))))
        val result = engine().performLayout(doc)
        assertEquals(result.elementPageIndex[1], result.elementPageIndex[2])
        assertEquals(PageEndReason.KEEP, result.pages.first().endReason)
    }

    @Test fun overPageKeepChainsAndOversizedLinesTerminate() {
        val result = engine().performLayout(doc(OfficeParagraph("long ".repeat(300)), OfficeParagraph("next"),
            s = style.copy(keepWithNext = true, keepTogether = true)))
        assertTrue(result.pages.size > 1)
        val enormous = engine().performLayout(doc(OfficeParagraph("X"), s = style.copy(lineHeightExactUnits = 500f)))
        assertEquals(1, enormous.pages.size)
        assertEquals(1, enormous.pages.single().elements.size)
    }

    @Test fun explicitBlankPagesArePreservedAndExplained() {
        val empty = engine().performLayout(doc())
        assertEquals(1, empty.pages.size)
        val result = engine().performLayout(doc(OfficePageBreak, OfficePageBreak, OfficeParagraph("body"), OfficePageBreak))
        assertEquals(4, result.pages.size)
        assertTrue(result.pages[0].elements.isEmpty())
        assertTrue(result.pages[1].elements.isEmpty())
        assertTrue(result.pages.last().elements.isEmpty())
        assertTrue(result.pages.take(3).all { it.endReason == PageEndReason.AUTHORED })
    }

    @Test fun hitTestingContinuationReturnsOriginalParagraphOffset() {
        val e = engine()
        val result = e.performLayout(doc(OfficeParagraph("text ".repeat(300))))
        val fragment = result.pages[1].elements.first()
        val line = fragment.paragraphLayout!!.lines.first()
        val hit = e.hitTest(fragment.bounds.left + line.left, page.heightDp + fragment.bounds.top + 1f, result.pages)!!
        assertEquals(fragment.sourceStart, hit.characterOffset)
        assertEquals(fragment.firstLineIndex, hit.lineIndex)
    }

    @Test fun hundredPageStressReusesParagraphMeasurements() {
        val e = engine()
        val doc = doc(*Array<OfficeElement>(160) { OfficeParagraph("word ".repeat(100)) })
        val started = System.nanoTime()
        val first = e.performLayout(doc)
        assertTrue(first.pages.size >= 100)
        val measurements = e.measuredParagraphCount
        val second = e.performLayout(doc)
        assertEquals(first, second)
        assertEquals(measurements, e.measuredParagraphCount)
        val rebuilt = e.performLayout(doc, forceRebuildAll = true)
        assertEquals(first, rebuilt)
        println("fragment-stress backend=table pages=${first.pages.size} paragraphs=160 measurements=$measurements elapsedMs=${(System.nanoTime() - started) / 1000000}")
    }
    @Test fun inlineBoundariesSplitLayoutNotDocumentAndPreserveTrailingBlank() {
        val p = OfficeParagraph("AB", pageBreakOffsets = listOf(1, 2))
        val result = engine().performLayout(doc(p))
        assertEquals(3, result.pages.size)
        assertEquals("A", result.pages[0].elements.single().paragraphLayout!!.lines.single().text)
        assertEquals("B", result.pages[1].elements.single().paragraphLayout!!.lines.single().text)
        assertTrue(result.pages.last().elements.isEmpty())
        assertEquals(listOf(1, 2), result.elementPages[0])
    }

    @Test fun continuousSectionsDoNotBreakAndParitySectionsInsertOnlyRequiredBlank() {
        val base = doc(OfficeParagraph("A"), OfficeParagraph("B"))
        val continuous = engine().performLayout(base.copy(sectionStarts = listOf(SectionStart(1, SectionStartKind.CONTINUOUS))))
        assertEquals(1, continuous.pages.size)
        val odd = engine().performLayout(base.copy(sectionStarts = listOf(SectionStart(1, SectionStartKind.ODD_PAGE))))
        assertEquals(3, odd.pages.size)
        assertTrue(odd.pages[1].elements.isEmpty())
        val even = engine().performLayout(base.copy(sectionStarts = listOf(SectionStart(1, SectionStartKind.EVEN_PAGE))))
        assertEquals(2, even.pages.size)
    }

    @Test fun editingOneLogicalParagraphRetainsHardNewlinesAndRemapsInlineBoundaries() {
        val p = OfficeParagraph("A\n\nB", pageBreakOffsets = listOf(3))
        val original = doc(p)
        assertEquals(original, DocumentTextMerger.mergeEditedText(original, p.text))
        val changed = DocumentTextMerger.mergeEditedText(original, "XA\n\nB")
        val updated = changed.body.elements.single() as OfficeParagraph
        assertEquals("XA\n\nB", updated.text)
        assertEquals(listOf(4), updated.pageBreakOffsets)
        assertEquals(1, DocumentTextWindows.compute(changed.body.elements, updated.text).size)
    }

    @Test fun masterPageStartsANewPageButNotAnEmptyPageAtDocumentStart() {
        val styles = DocumentStyles(defaultParagraphStyle = style,
            paragraphStyles = mapOf("chapter" to style.copy(name = "chapter", masterPageName = "Other")))
        val document = OfficeDocument(styles = styles, body = DocumentBody(listOf(
            OfficeParagraph("A", "chapter"), OfficeParagraph("B", "chapter"))))
        val result = engine().performLayout(document)
        assertEquals(2, result.pages.size)
        assertTrue(result.pages.all { it.elements.size == 1 })
        assertEquals(PageEndReason.AUTHORED, result.pages.first().endReason)
    }

}
