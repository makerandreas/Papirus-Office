package com.makerandreas.papirusoffice.data

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * CI compatibility harness.
 *
 * Parses the bundled office sample files (repo root: `tests/`) through the
 * real production parsers and asserts that each produces a non-failed,
 * non-empty document. Runs as JVM unit tests (Robolectric) so the same check
 * executes on every PR via `./gradlew :app:testDebugUnitTest` (GitHub
 * Actions) with no Android SDK/device required.
 */
@org.junit.runner.RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SampleFilesCompatibilityTest {

    private fun context(): Context =
        androidx.test.core.app.ApplicationProvider.getApplicationContext()


    /** Locates a repo-root tests fixture regardless of the module's working dir. */
    private fun repoRoot(): File {
        var f: File? = File(".").absoluteFile
        while (f != null) {
            if (File(f, ".git").exists()) return f
            f = f.parentFile
        }
        return File(".").absoluteFile
    }

    private fun fixture(name: String): File {
        val direct = File(repoRoot(), "tests/$name")
        if (direct.isFile) return direct
        return File("tests/$name")
    }

    private fun assertParses(parser: OfficeDocumentParser, path: String) {
        val file = fixture(path)
        assumeTrue("missing fixture $path (local checkout?)", file.isFile)
        // parseDocument is the production suspend API and performs the full
        // parse on Dispatchers.IO before returning, so no polling is needed.
        val parsed = runBlocking { parser.parseDocument(file, bypassCache = true) }
        assertNotNull("$path produced no parsed document", parsed)
        assertTrue(
            "$path failed to parse: ${parsed?.failureReason}",
            !parsed.isParsingFailed
        )
        assertTrue(
            "$path parsed but produced no elements (silent truncation?)",
            parsed.elements.isNotEmpty()
        )
        assertTrue(
            "$path produced empty plainText",
            parsed.plainText.isNotBlank()
        )
    }

    // ---------- Inky (Writer): DOCX / ODT ----------

    @Test
    fun inky_docx_sample1() = assertParses(OfficeDocumentParser(context()), "inky/Sample-1.docx")

    @Test
    fun inky_docx_sample2() = assertParses(OfficeDocumentParser(context()), "inky/Sample-2.docx")

    @Test
    fun inky_docx_sample3() = assertParses(OfficeDocumentParser(context()), "inky/Sample-3.docx")

    @Test
    fun inky_docx_sample4() = assertParses(OfficeDocumentParser(context()), "inky/Sample-4.docx")

    @Test
    fun inky_odt_sample1() = assertParses(OfficeDocumentParser(context()), "inky/Sample-1.odt")

    @Test
    fun inky_odt_sample2() = assertParses(OfficeDocumentParser(context()), "inky/Sample-2.odt")

    @Test
    fun inky_odt_sample3() = assertParses(OfficeDocumentParser(context()), "inky/Sample-3.odt")

    @Test
    fun inky_odt_sample4() = assertParses(OfficeDocumentParser(context()), "inky/Sample-4.odt")

    // ---------- Cellina (Calc): XLSX / ODS ----------

    @Test
    fun cellina_xlsx_sample1() = assertParses(OfficeDocumentParser(context()), "cellina/Sample-1.xlsx")

    @Test
    fun cellina_ods_sample1() = assertParses(OfficeDocumentParser(context()), "cellina/Sample-1.ods")

    // ---------- Slidia (Impress): PPTX / ODP ----------

    @Test
    fun slidia_pptx_sample1() = assertParses(OfficeDocumentParser(context()), "slidia/Sample-1.pptx")

    @Test
    fun slidia_odp_sample1() = assertParses(OfficeDocumentParser(context()), "slidia/Sample-1.odp")

    // ---------- Content-level checks for the streaming rewrite ----------

    @Test
    fun xlsx_sample1_has_named_tables() {
        val file = fixture("cellina/Sample-1.xlsx")
        assumeTrue("missing fixture", file.isFile)
        val parsed = runBlocking { OfficeDocumentParser(context()).parseDocument(file, bypassCache = true) }
        assertNotNull("XLSX produced no parsed document", parsed)
        assertTrue("XLSX failed to parse: ${parsed?.failureReason}", !parsed.isParsingFailed)
        val tables = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>()
        assertTrue("Expected at least one named table in XLSX, got ${tables.size}", tables.isNotEmpty())
        // The streaming parser must propagate workbook sheet names.
        assertTrue(
            "Sheet names were not propagated from workbook.xml",
            tables.any { !it.name.isNullOrBlank() }
        )
        // Plain text of a numeric sheet should contain some digit (values, not only empty cells).
        assertTrue("No cell digits found in XLSX plain text", parsed.plainText.any { it.isDigit() })
        // Page count maps to sheet count.
        assertEquals(tables.size, parsed.pageCount)
    }
}
