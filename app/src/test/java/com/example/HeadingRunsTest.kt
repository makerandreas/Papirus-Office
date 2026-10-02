package com.example

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeListItem
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HeadingRunsTest {

    @Test
    fun sample4AndSample5HeadingRunsPreserveInlineItalicSpansAndSynchronizeWithText() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)

        val sample4 = parser.parseDocument(SampleMatrix.findTestFile("Sample-4.odt"), bypassCache = true).toOfficeDocument()
        val sample4Headings = sample4.body.elements.filterIsInstance<OfficeHeading>()
        assertTrue(sample4Headings.isNotEmpty())
        sample4Headings.forEach { h ->
            assertTrue("Heading runs must not be empty on '${h.text}'", h.runs.isNotEmpty())
            assertEquals("Heading runs must concatenate to heading text", h.text, h.runs.joinToString("") { it.text })
        }
        val mwsHeading = sample4Headings.first { it.text.contains("Mean World Syndrome") }
        assertTrue(
            "Mean World Syndrome heading must preserve italic span run: ${mwsHeading.runs}",
            mwsHeading.runs.any { it.text.contains("Mean World Syndrome") && it.isItalic }
        )

        val sample5 = parser.parseDocument(SampleMatrix.findTestFile("Sample-5.odt"), bypassCache = true).toOfficeDocument()
        val sample5Headings = sample5.body.elements.filterIsInstance<OfficeHeading>()
        sample5Headings.forEach { h ->
            if (h.text.isNotEmpty()) {
                assertTrue("Sample-5 heading runs must not be empty on '${h.text}'", h.runs.isNotEmpty())
                assertEquals(h.text, h.runs.joinToString("") { it.text })
            }
        }
        val italicHeading = sample5Headings.first { it.text.contains("Supergrup") }
        assertTrue(
            "Sample-5 Supergrup heading must preserve italic span run: ${italicHeading.runs}",
            italicHeading.runs.any { it.isItalic }
        )
    }

    @Test
    fun sample6ListItemsPreserveInlineBoldLeadInRunsAndRenderViaOfficeRuns() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val doc = parser.parseDocument(SampleMatrix.findTestFile("Sample-6.odt"), bypassCache = true).toOfficeDocument()

        val listItems = doc.body.elements.filterIsInstance<OfficeListItem>()
        assertTrue("Sample-6.odt must contain OfficeListItem elements", listItems.isNotEmpty())

        var boldRunCount = 0
        for (item in listItems) {
            assertEquals(
                "ListItem runs must concatenate to item.text",
                item.text,
                item.runs.joinToString("") { it.text }
            )
            if (item.runs.any { it.isBold }) {
                boldRunCount++
                val asPara = OfficeParagraph(text = item.text, styleName = item.styleName, runs = item.runs)
                val annotated = OfficeRuns.toAnnotatedString(asPara, doc.styles, 1f, Color.Black)
                assertEquals(item.text, annotated.text)
                assertTrue("AnnotatedString for ListItem with bold run must have SpanStyles", annotated.spanStyles.isNotEmpty())
            }
        }
        // 12 of Sample-6.odt's 88 list items carry a bold (<style:text-properties fo:font-weight="bold"/>)
        // span; the floor names the property, not an exact count.
        assertTrue("Sample-6.odt list items must preserve bold lead-in runs (found $boldRunCount)", boldRunCount >= 10)
    }
}
