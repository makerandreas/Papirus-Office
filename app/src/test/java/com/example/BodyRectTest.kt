package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 5d body rectangle computation from margins and header/footer heights.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BodyRectTest {

    private fun parseOfficeDoc(name: String) = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = SampleMatrix.findTestFile(name)
        assertTrue("$name must exist", file.exists() && file.length() > 0)
        val parsed = parser.parseDocument(file, bypassCache = true)
        assertFalse("$name parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        parsed
    }

    @Test
    fun odtBodyTopIsOneInchAndFooterDeductedFromBottom() {
        for (name in listOf("Sample-1.odt", "Sample-2.odt", "Sample-4.odt", "Sample-5.odt", "Sample-6.odt")) {
            val parsed = parseOfficeDoc(name)
            val spec = parsed.styles.defaultPageStyle
            assertNotNull("$name must have defaultPageStyle", spec)
            // 2.54 cm = 96 layout units top margin; 0 header -> bodyTop = 96
            assertEquals("$name bodyTopDp", 96f, spec!!.bodyTopDp, 0.5f)
            // 1 cm margin (37.8 units) + 1.54 cm footer (58.2 units) = 96 units total bottom inset
            assertEquals("$name bodyBottomDp", spec.heightDp - 96f, spec.bodyBottomDp, 1.0f)
        }
    }

    @Test
    fun docxBodyRectangleMatchesDeclaredPageMargins() {
        for (name in listOf("Sample-1.docx", "Sample-2.docx", "Sample-4.docx", "Sample-5.docx", "Sample-6.docx")) {
            val parsed = parseOfficeDoc(name)
            val spec = parsed.styles.defaultPageStyle
            assertNotNull("$name must have defaultPageStyle", spec)
            // Word: header/footer are within margins, so bodyTop = marginTop, bodyBottom = contentBottom
            assertEquals("$name bodyTopDp", spec!!.marginTopDp, spec.bodyTopDp, 0.1f)
            assertEquals("$name bodyBottomDp", spec.contentBottomDp, spec.bodyBottomDp, 0.1f)
            // A4 is 793.8 x 1122.7 units; 1 in (96 units) margins all sides -> content width ~ 601.8 units (6.27 in)
            assertEquals("$name contentWidthDp", 601.8f, spec.contentWidthDp, 2.0f)
        }
    }
}
