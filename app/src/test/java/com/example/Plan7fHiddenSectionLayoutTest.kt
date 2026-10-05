package com.example

import com.makerandreas.papirusoffice.data.BodyElementRange
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentSectionRange
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.DocumentTextProjection
import com.makerandreas.papirusoffice.data.DocumentTextWindows
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeElement
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.SectionDisplay
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Plan 7F gate 2: layout filtering over Plan 7C's original body-index ranges. */
class Plan7fHiddenSectionLayoutTest {
    private val styles = DocumentStyles(defaultParagraphStyle = ParagraphStyle("Body", fontSizeSp = 12f))
    private val engine = LayoutEngine(
        PageStyleSpec.FALLBACK.copy(widthDp = 320f, marginStartDp = 20f, marginEndDp = 20f),
        advanceSource = TableAdvanceSource
    )

    @Test
    fun hiddenRangesOmitPageBreaksAndParagraphsWithoutReindexingVisibleContent() {
        val elements = listOf<OfficeElement>(
            OfficeParagraph("Before"),       // original index 0
            OfficePageBreak,                  // hidden index 1
            OfficeParagraph("Secret"),       // hidden index 2
            OfficePageBreak,                  // hidden index 3
            OfficeParagraph("After")         // original index 4
        )
        val hidden = DocumentSectionRange(
            id = "hidden",
            name = "Hidden",
            bodyRange = BodyElementRange(1, 4),
            display = SectionDisplay.HIDDEN
        )
        val result = engine.performLayout(OfficeDocument(
            styles = styles,
            body = DocumentBody(elements),
            namedSectionRanges = listOf(hidden)
        ))

        assertEquals("hidden authored breaks do not create pages", 1, result.pages.size)
        val placed = result.pages.single().elements
        assertEquals(listOf(0, 4), placed.map { it.elementIndex })
        assertEquals(listOf("Before", "After"), placed.map { (it.element as OfficeParagraph).text })
        assertEquals(4, placed.last().paragraphLayout!!.paragraphIndex)
        assertEquals(setOf(0, 4), result.elementPageIndex.keys)
        assertFalse(placed.any { (it.element as? OfficeParagraph)?.text == "Secret" })
    }

    @Test
    fun nestedHiddenRangesAreExcludedButVisibleAndConditionalRangesRemain() {
        val elements = listOf<OfficeElement>(
            OfficeParagraph("Intro"),
            OfficeParagraph("Outer first"),
            OfficeParagraph("Hidden nested"),
            OfficeParagraph("Outer last"),
            OfficeParagraph("Conditional first"),
            OfficeParagraph("Conditional second"),
            OfficeParagraph("Visible tail")
        )
        val outer = DocumentSectionRange(
            id = "outer", name = "Outer", bodyRange = BodyElementRange(1, 4)
        )
        val nestedHidden = DocumentSectionRange(
            id = "nested-hidden",
            name = "Nested hidden",
            bodyRange = BodyElementRange(2, 3),
            parentId = outer.id,
            depth = 1,
            display = SectionDisplay.HIDDEN
        )
        val conditional = DocumentSectionRange(
            id = "conditional",
            name = "Conditional",
            bodyRange = BodyElementRange(4, 6),
            display = SectionDisplay.CONDITIONAL,
            condition = "ooow:show == 1"
        )
        val visibleTail = DocumentSectionRange(
            id = "visible-tail", name = "Visible tail", bodyRange = BodyElementRange(6, 7)
        )
        val result = engine.performLayout(OfficeDocument(
            styles = styles,
            body = DocumentBody(elements),
            namedSectionRanges = listOf(outer, nestedHidden, conditional, visibleTail)
        ))
        val placed = result.pages.flatMap { it.elements }

        assertEquals(listOf(0, 1, 3, 4, 5, 6), placed.map { it.elementIndex })
        assertEquals(
            listOf("Intro", "Outer first", "Outer last", "Conditional first", "Conditional second", "Visible tail"),
            placed.map { (it.element as OfficeParagraph).text }
        )
        assertTrue("conditional metadata stays authored", conditional.display == SectionDisplay.CONDITIONAL)
        assertEquals("ooow:show == 1", conditional.condition)
        assertEquals("visible parent range remains intact", BodyElementRange(1, 4), outer.bodyRange)
    }

    @Test
    fun hidingLayoutDoesNotDeleteTextOrShiftEditorWindows() {
        val elements = listOf<OfficeElement>(
            OfficeParagraph("Intro"),
            OfficeParagraph("Secret text"),
            OfficeParagraph("Visible later")
        )
        val hidden = DocumentSectionRange(
            id = "hidden", name = "Hidden", bodyRange = BodyElementRange(1, 2), display = SectionDisplay.HIDDEN
        )
        val document = OfficeDocument(
            styles = styles,
            body = DocumentBody(elements),
            namedSectionRanges = listOf(hidden)
        )
        val projectedText = DocumentTextProjection.editorText(document)
        val windows = DocumentTextWindows.compute(elements, projectedText)
        val laterWindow = windows.getValue(2)
        val expectedStart = "Intro".length + DocumentTextProjection.BLOCK_SEPARATOR.length +
            "Secret text".length + DocumentTextProjection.BLOCK_SEPARATOR.length

        assertEquals("Intro\n\nSecret text\n\nVisible later", projectedText)
        assertEquals(setOf(0, 1, 2), windows.keys)
        assertEquals(expectedStart, laterWindow.start)
        assertEquals("Visible later", laterWindow.text)
        assertEquals(2, DocumentTextWindows.elementForOffset(windows, laterWindow.start)!!.elementIndex)

        val result = engine.performLayout(document)
        val placed = result.pages.flatMap { it.elements }
        assertEquals(listOf(0, 2), placed.map { it.elementIndex })
        assertEquals("source model text remains available for editing/serialization", projectedText, DocumentTextProjection.editorText(document))
    }
}
