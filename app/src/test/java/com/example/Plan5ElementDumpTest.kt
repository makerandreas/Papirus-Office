package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.LayoutDump
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.toOfficeDocument
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

/**
 * Plan 5A E-0: the per-page element dump over all six sample pairs.
 *
 * The output is evidence, not a target: it prints the body rectangle the
 * paginator used, the defaults it measured with, the page count against the
 * reference count and window from [SampleMatrix], the pages holding fewer
 * than three elements, and the mechanism that produced them (a break
 * element in the model, or metrics that overflow). Only structural
 * invariants of the layout are asserted here; the page windows stay with
 * `Sample5UnifiedPaginationTest` (12..30) until PR 16b.
 *
 * Runs under the same Robolectric configuration as the pagination guard so
 * the numbers are the numbers CI already produces.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan5ElementDumpTest {

    private data class Run(
        val fileName: String,
        val report: LayoutDump.Report,
        val window: SampleMatrix.Window?
    )

    private fun layoutSample(fileName: String): Run = cache.getOrPut(fileName) { layoutSampleUncached(fileName) }

    private fun layoutSampleUncached(fileName: String): Run = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = SampleMatrix.findTestFile(fileName)
        assertTrue("$fileName must exist (${file.absolutePath})", file.exists() && file.length() > 0)
        val parsed = OfficeDocumentParser(context).parseDocument(file, bypassCache = true)
        assertFalse("$fileName parse failed: ${parsed.failureReason}", parsed.isParsingFailed)
        val document = parsed.toOfficeDocument()
        val spec = document.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
        val engine = LayoutEngine(spec)
        val result = engine.performLayout(document)
        val report = LayoutDump.build(
            label = fileName,
            document = document,
            pageSpec = spec,
            result = result,
            referencePageCount = SampleMatrix.referenceFor(fileName),
            measurementProbe = engine.measurementProbe()
        )
        Run(fileName, report, SampleMatrix.windowFor(fileName))
    }

    @Test
    fun dumpAllSixPairsAndCheckLayoutInvariants() {
        val problems = ArrayList<String>()
        val runs = ArrayList<Run>()
        for (fileName in SampleMatrix.fileNames) {
            val run = layoutSample(fileName)
            runs += run
            println(run.report.toText())
            println("window (not asserted until PR 16b): ${run.window ?: "none"}")
            println()
            checkInvariants(run, problems)
        }

        println("== plan 5A dump summary")
        println(String.format(Locale.ROOT, "%-14s %5s %4s %7s %5s %5s %6s %6s %s", "file", "pages", "ref", "window", "thin", "empty", "breaks", "blank", "mechanism"))
        for (run in runs) {
            val r = run.report
            println(
                String.format(
                    Locale.ROOT, "%-14s %5d %4s %7s %5d %5d %6d %6d %s",
                    run.fileName, r.pageCount, r.referencePageCount?.toString() ?: "n/a", run.window?.toString() ?: "-",
                    r.thinPages, r.emptyPages, r.breakElements, r.blankParagraphs, r.mechanism
                )
            )
        }

        if (problems.isNotEmpty()) fail("layout invariants violated:\n" + problems.joinToString("\n"))
    }

    @Test
    fun dumpNamesTheMechanismForEveryFile() {
        for (fileName in SampleMatrix.fileNames) {
            val run = layoutSample(fileName)
            assertTrue("$fileName: mechanism must be named", run.report.mechanism.isNotBlank())
            assertTrue("$fileName: at least one page", run.report.pageCount >= 1)
        }
    }

    @Test
    fun reportBreakElementsAgainstTheParseLevelInventory() {
        // audit-007 §5, parse level: text:soft-page-break plus fo:break-before="page"
        // per ODT (8+0, 10+1, 0, 2+2, 2+2, 0+2). What the model holds today is
        // printed next to it; the difference is what PR 16a ("fake breaks out",
        // "real breaks in") has to move. Reported, not asserted: this test is
        // evidence about the reader, not a target for it.
        val parseLevel = mapOf(
            "Sample-1.odt" to 8, "Sample-2.odt" to 11, "Sample-3.odt" to 0,
            "Sample-4.odt" to 4, "Sample-5.odt" to 4, "Sample-6.odt" to 2
        )
        for ((fileName, count) in parseLevel) {
            val run = layoutSample(fileName)
            println("$fileName: ${run.report.breakElements} break elements in the model; parse-level inventory (soft + break-before) $count")
        }
        // DOCX: the reader turns lastRenderedPageBreak, w:br type=page and section
        // starts into breaks by different rules (audit-007 §9); counts printed only.
        for (sample in SampleMatrix.sampleNumbers) {
            val run = layoutSample(SampleMatrix.docxName(sample))
            println("${run.fileName}: ${run.report.breakElements} break elements in the model")
        }
    }

    private companion object {
        /** One parse + layout per fixture per sandbox; the tests only read the result. */
        val cache = HashMap<String, Run>()
    }

    private fun checkInvariants(run: Run, problems: MutableList<String>) {
        val name = run.fileName
        val report = run.report
        val spec = report.pageSpec
        if (report.pageCount < 1) problems += "$name: no pages"
        // Every non-break element is placed exactly once.
        val expectedPlaced = report.elementTotal - report.breakElements
        if (report.placedTotal != expectedPlaced) {
            problems += "$name: placed ${report.placedTotal} elements, expected $expectedPlaced (elements minus break elements)"
        }
        var previousLast = -1
        for (row in report.pages) {
            val first = row.firstElementIndex
            val last = row.lastElementIndex
            if (first != null && last != null) {
                if (first > last) problems += "$name page ${row.pageNumber}: first index $first after last $last"
                if (first <= previousLast) problems += "$name page ${row.pageNumber}: first index $first does not follow previous page's last $previousLast"
                previousLast = last
            }
            if (row.reservedHeight < 0f || row.leftover < 0f) problems += "$name page ${row.pageNumber}: negative reserved/leftover"
        }
        if (spec.contentWidthDp <= 0f) problems += "$name: content width ${spec.contentWidthDp}"
    }

    @Test
    fun placedElementsStayInsideTheTextColumn() {
        val problems = ArrayList<String>()
        for (fileName in SampleMatrix.fileNames) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val parsed = runBlocking { OfficeDocumentParser(context).parseDocument(SampleMatrix.findTestFile(fileName), bypassCache = true) }
            val document = parsed.toOfficeDocument()
            val spec = document.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK
            val result = LayoutEngine(spec).performLayout(document)
            val eps = 0.01f
            for (page in result.pages) {
                for (placed in page.elements) {
                    val b = placed.bounds
                    if (b.left < spec.marginStartDp - eps || b.right > spec.widthDp - spec.marginEndDp + eps || b.top < spec.marginTopDp - eps) {
                        problems += "$fileName page ${page.pageNumber} element ${placed.elementIndex}: bounds $b outside column [${spec.marginStartDp}, ${spec.widthDp - spec.marginEndDp}] from top ${spec.marginTopDp}"
                    }
                    if (placed.element is OfficePageBreak) problems += "$fileName page ${page.pageNumber}: a break element was placed as content"
                }
            }
            assertEquals("$fileName: totalHeightDp is pages times page height", result.pages.size * spec.heightDp, result.totalHeightDp, 0.5f)
        }
        if (problems.isNotEmpty()) fail(problems.joinToString("\n"))
    }
}
