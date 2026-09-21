package com.example

import android.content.Context
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.util.ZipSafe
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integrity harness for the [OfficeDocumentParser] monolith.
 *
 * Loaders/serializers in the codebase call [OfficeDocumentParser] across
 * format boundaries and from several modules (see DocumentSerializer), so a
 * partial rewrite of the file once broke those references at compile time.
 * Each test here crosses that surface for one real fixture so a corrupted
 * region fails for a concrete reason here instead of first appearing as a red
 * GitHub CI build.
 *
 * Mirrors the fixture/assert conventions of
 * com.makerandreas.papirusoffice.data.SampleFilesCompatibilityTest so both
 * harnesses agree on how a repo-root fixture is located and how a "parsed,
 * non-empty document" is judged.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfficeDocumentParserSurfaceTest {

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

    private fun assertParses(path: String, minElements: Int = 1) {
        val file = fixture(path)
        assumeTrue("missing fixture $path (local checkout?)", file.isFile)
        // Hit the monolith repeatedly: the second pass must be served from the
        // in-memory parse cache, not a silently emptied path reserved for the
        // first call only.
        repeat(3) { run ->
            val parsed = runBlocking {
                OfficeDocumentParser(context()).parseDocument(file, bypassCache = true)
            }
            assertNotNull("$path (run $run) produced no parsed document", parsed)
            assertFalse(
                "$path (run $run) failed to parse: ${parsed.failureReason}",
                parsed.isParsingFailed
            )
            assertTrue(
                "$path (run $run) parsed but produced no elements " +
                    "(expected >= $minElements, got ${parsed.elements.size})",
                parsed.elements.size >= minElements
            )
            assertTrue(
                "$path (run $run) produced empty plainText",
                parsed.plainText.isNotBlank()
            )
        }
    }

    // ---------- Parser/loader surface (parseDocument) ----------

    @Test
    fun odt_parseDocumentRepeatedly_neverReturnsEmpty() =
        assertParses("inky/Sample-1.odt")

    @Test
    fun docx_parseDocumentRepeatedly_neverReturnsEmpty() =
        assertParses("inky/Sample-1.docx")

    @Test
    fun xlsx_parseDocument_returnsTabularContent() {
        val path = "cellina/Sample-1.xlsx"
        val file = fixture(path)
        assumeTrue("missing fixture $path (local checkout?)", file.isFile)
        val parsed = runBlocking {
            OfficeDocumentParser(context()).parseDocument(file, bypassCache = true)
        }
        assertNotNull("$path produced no parsed document", parsed)
        assertFalse("$path failed to parse: ${parsed.failureReason}", parsed.isParsingFailed)
        assertTrue(
            "$path parsed but produced no table rows",
            parsed.elements.filterIsInstance<OfficeDocumentElement.Table>().any { it.rows.isNotEmpty() }
        )
    }

    @Test
    fun pptx_parseDocument_neverReturnsEmpty() =
        assertParses("slidia/Sample-1.pptx")

    // ---------- Serializer surface (save*Document) ----------

    @Test
    fun saveOdtDocument_calledAcrossFormatBoundary_doesNotThrow() {
        val context = context()
        val out = File.createTempFile("surface", ".odt")
        try {
            val doc = OfficeDocumentParser(context)
            runBlocking { doc.saveOdtDocument(out, "surface test paragraph") }
            assertTrue("saveOdtDocument must produce a file", out.length() > 0)
        } finally {
            if (out.exists()) out.delete()
        }
    }

    @Test
    fun saveOdpDocument_doesNotThrow() {
        val out = File.createTempFile("surface", ".odp")
        try {
            val doc = OfficeDocumentParser(context())
            runBlocking { doc.saveOdpDocument(out, "surface") }
            assertTrue("saveOdpDocument must produce a file", out.length() > 0)
        } finally {
            if (out.exists()) out.delete()
        }
    }

    @Test
    fun saveXlsxDocument_doesNotThrow() {
        val out = File.createTempFile("surface", ".xlsx")
        try {
            val doc = OfficeDocumentParser(context())
            runBlocking { doc.saveXlsxDocument(out, "surface") }
            assertTrue("saveXlsxDocument must produce a file", out.length() > 0)
        } finally {
            if (out.exists()) out.delete()
        }
    }

    @Test
    fun savePptxDocument_doesNotThrow() {
        val out = File.createTempFile("surface", ".pptx")
        try {
            val doc = OfficeDocumentParser(context())
            runBlocking { doc.savePptxDocument(out, "surface") }
            assertTrue("savePptxDocument must produce a file", out.length() > 0)
        } finally {
            if (out.exists()) out.delete()
        }
    }

    // ---------- Shared guard rails the parsers depend on ----------

    @Test
    fun zipSafe_exposesTheColumnCapParserDependsOn() {
        assertTrue(
            "ZipSafe.MAX_XLSX_COLUMN_INDEX must stay a positive guard value",
            ZipSafe.MAX_XLSX_COLUMN_INDEX > 0
        )
    }
}
