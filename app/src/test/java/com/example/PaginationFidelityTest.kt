package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fixed audit-008 windows. No calibration constants or soft-break hints are used to meet them. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaginationFidelityTest {
    @Test fun allTwelveFixturesMeetRecordedWindowsWithLosslessTextFragments() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val failures = mutableListOf<String>()
        for (name in SampleMatrix.fileNames) {
            val parsed = OfficeDocumentParser(context).parseDocument(SampleMatrix.findTestFile(name), bypassCache = true)
            assertFalse("$name: ${parsed.failureReason}", parsed.isParsingFailed)
            val doc = parsed.toOfficeDocument()
            val engine = LayoutEngine(doc.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK, advanceSource = TableAdvanceSource)
            val result = engine.performLayout(doc)
            val window = SampleMatrix.windowFor(name)!!
            println("fidelity $name backend=table pages=${result.pages.size} reference=${SampleMatrix.referenceFor(name)} window=$window")
            if (result.pages.size !in window) failures += "$name: ${result.pages.size} pages, expected $window (reference ${SampleMatrix.referenceFor(name)})"
            doc.body.elements.forEachIndexed { index, element ->
                val source = DocumentTextMerger.textOf(element) ?: return@forEachIndexed
                val fragments = result.pages.flatMap { it.elements }.filter { it.elementIndex == index }
                assertTrue("$name element $index missing", fragments.isNotEmpty())
                assertEquals("$name element $index source coverage", source,
                    fragments.joinToString("") { source.substring(it.sourceStart, it.sourceEnd) })
                fragments.zipWithNext().forEach { (a,b) -> assertEquals(a.sourceEnd, b.sourceStart) }
            }
        }
        assertTrue("Fidelity windows failed; inspect source/styles/media/fonts rather than widen them:\n${failures.joinToString("\n")}", failures.isEmpty())
    }
}
