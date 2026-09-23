package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.makerandreas.papirusoffice.data.CharacterStyle
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.OfficeTextRun
import com.makerandreas.papirusoffice.data.ParagraphStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficeRunsTest {

    private val black = Color.Black

    @Test
    fun baseSpanCarriesResolvedBodySize() {
        val annotated = OfficeRuns.toAnnotatedString(OfficeParagraph(text = "Hello world"), DocumentStyles(), 1f, black)

        assertEquals("Hello world", annotated.text)
        val baseSpan = annotated.spanStyles.first()
        assertEquals(14.sp, baseSpan.item.fontSize)
        assertEquals(TextAlign.Left, annotated.paragraphStyles.single().item.textAlign)
    }

    @Test
    fun baseSpanHonorsScale() {
        val annotated = OfficeRuns.toAnnotatedString(OfficeParagraph(text = "x"), DocumentStyles(), 0.5f, black)
        assertEquals(7.sp, annotated.spanStyles.single().item.fontSize)
    }

    @Test
    fun headingResolvesThroughStyleResolverCascade() {
        val annotated = OfficeRuns.toAnnotatedString(
            OfficeParagraph(text = "Title", styleName = "Heading 2"),
            DocumentStyles(),
            1f,
            black
        )
        val span = annotated.spanStyles.single()
        assertEquals(20.sp, span.item.fontSize)
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }

    @Test
    fun boldRunSpansOnlyItsRange() {
        val paragraph = OfficeParagraph(
            text = "plain bold rest",
            runs = listOf(
                OfficeTextRun(text = "plain "),
                OfficeTextRun(text = "bold", isBold = true),
                OfficeTextRun(text = " rest")
            )
        )
        val annotated = OfficeRuns.toAnnotatedString(paragraph, DocumentStyles(), 1f, black)

        val boldSpan = annotated.spanStyles.first { it.item.fontWeight == FontWeight.Bold }
        assertEquals(6, boldSpan.start)
        assertEquals(10, boldSpan.end)
    }

    @Test
    fun italicUnderlineRunCarriesDecoration() {
        val paragraph = OfficeParagraph(
            text = "em",
            runs = listOf(OfficeTextRun(text = "em", isItalic = true, isUnderline = true))
        )
        val annotated = OfficeRuns.toAnnotatedString(paragraph, DocumentStyles(), 1f, black)

        val runSpan = annotated.spanStyles.first { it.item.fontStyle == FontStyle.Italic }
        assertEquals(TextDecoration.Underline, runSpan.item.textDecoration)
        assertEquals(0, runSpan.start)
        assertEquals(2, runSpan.end)
    }

    @Test
    fun characterStyleHitOverridesRunFlags() {
        val styles = DocumentStyles(
            characterStyles = mapOf("Emph" to CharacterStyle("Emph", isBold = true, colorHex = "#FF0000"))
        )
        val paragraph = OfficeParagraph(
            text = "marked",
            runs = listOf(OfficeTextRun(text = "marked", characterStyle = "Emph"))
        )
        val annotated = OfficeRuns.toAnnotatedString(paragraph, styles, 1f, black)

        val runSpan = annotated.spanStyles.last()
        assertEquals(FontWeight.Bold, runSpan.item.fontWeight)
        assertEquals(Color(0xFFFF0000), runSpan.item.color)
    }

    @Test
    fun paragraphAlignmentOverridesStyleAlignment() {
        val styles = DocumentStyles(
            paragraphStyles = mapOf("Centered" to ParagraphStyle("Centered", fontSizeSp = 16f, alignment = "Center"))
        )
        val paragraph = OfficeParagraph(text = "x", styleName = "Centered", alignment = "Right")
        val annotated = OfficeRuns.toAnnotatedString(paragraph, styles, 1f, black)

        assertEquals(16.sp, annotated.spanStyles.single().item.fontSize)
        assertEquals(TextAlign.Right, annotated.paragraphStyles.single().item.textAlign)
    }

    @Test
    fun mismatchedRunsFallBackToPlainStyledText() {
        // Edited text diverges from stale runs; rendering must not apply them.
        val paragraph = OfficeParagraph(
            text = "edited text",
            runs = listOf(OfficeTextRun(text = "old", isBold = true))
        )
        val annotated = OfficeRuns.toAnnotatedString(paragraph, DocumentStyles(), 1f, black)

        assertEquals("edited text", annotated.text)
        assertTrue("no run span may survive a text mismatch", annotated.spanStyles.none { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun fontFamilyMappingCoversKnownFamilies() {
        assertEquals(FontFamily.Serif, OfficeRuns.fontFamilyFor("Liberation Serif"))
        assertEquals(FontFamily.Serif, OfficeRuns.fontFamilyFor("Times New Roman"))
        assertEquals(FontFamily.SansSerif, OfficeRuns.fontFamilyFor("arial"))
        assertEquals(FontFamily.Monospace, OfficeRuns.fontFamilyFor("Courier New"))
        assertEquals(FontFamily.Default, OfficeRuns.fontFamilyFor(null))
        assertEquals(FontFamily.Default, OfficeRuns.fontFamilyFor("Some Unknown Font"))
    }
}
