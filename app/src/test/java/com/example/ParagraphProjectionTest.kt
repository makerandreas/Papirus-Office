package com.example

import androidx.compose.ui.text.AnnotatedString
import com.example.modules.inky.ParagraphProjection
import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Test

class ParagraphProjectionTest {
    private val styles = DocumentStyles(defaultParagraphStyle = ParagraphStyle("body"))
    private val spec = PageStyleSpec.FALLBACK.copy(widthDp = 240f, marginStartDp = 20f, marginEndDp = 20f)

    @Test fun wrapsAndPageGapsAreNotPartOfTheEditableText() {
        val source = "Some long text with  whitespace\tand a\nnewline. ".repeat(4)
        val p = OfficeParagraph(source)
        val layout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(0, p, styles)
        val gaps = layout.lines.mapIndexed { i, _ -> if (i == 2) 180f else 0f }
        for (zoom in listOf(0.5f, 1f, 1.5f, 2f, 3f)) {
            val transformed = ParagraphProjection(layout.lines, gaps, zoom, p, styles).filter(AnnotatedString(source))
            val map = transformed.offsetMapping
            var prev = -1
            for (i in 0..source.length) {
                val offset = map.originalToTransformed(i)
                assertTrue(offset >= prev)
                assertTrue(offset in 0..transformed.text.length)
                prev = offset
            }
            prev = -1
            for (i in 0..transformed.text.length) {
                val offset = map.transformedToOriginal(i)
                assertTrue(offset >= prev)
                assertTrue(offset in 0..source.length)
                prev = offset
            }
            assertEquals(source.length, map.transformedToOriginal(transformed.text.length))
            assertTrue(transformed.text.paragraphStyles.any { it.item.lineHeight.value == 180f * zoom })
        }
        assertEquals(source, p.text)
    }

    @Test fun sourceEditAcrossPageBoundaryRemainsOneLogicalWindow() {
        val source = "abc def ghi jkl mno pqr stu vwx yz".repeat(20)
        val window = DocumentTextWindow(0, source, 0, source.length)
        val at = source.length / 2
        val changed = source.removeRange(at - 4, at + 4)
        assertEquals(changed, DocumentTextWindows.applyLocalEdit(source, window, changed))
        val p = OfficeParagraph(changed)
        val layout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(0, p, styles)
        val transformed = ParagraphProjection(layout.lines, emptyList(), 1f, p, styles).filter(AnnotatedString(changed))
        val caret = transformed.offsetMapping.originalToTransformed(at - 4)
        assertEquals(at - 4, transformed.offsetMapping.transformedToOriginal(caret))
    }

    @Test fun staleLayoutFallsBackRatherThanCorruptingImeOffsets() {
        val p = OfficeParagraph("text")
        val layout = LayoutEngine(spec, advanceSource = TableAdvanceSource).layoutParagraph(0, p, styles)
        val transformed = ParagraphProjection(layout.lines, emptyList(), 1f, p, styles).filter(AnnotatedString("new longer text"))
        assertEquals("new longer text", transformed.text.text)
        assertEquals(6, transformed.offsetMapping.originalToTransformed(6))
    }
}
