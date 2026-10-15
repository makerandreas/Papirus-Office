package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OdtPackageData
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeElement
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeListItem
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.OfficeTextRun
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Plan 9 (save round-trip integrity) pre-change gate.
 *
 * This file lands FIRST and asserts only what today's writer already preserves.
 * It is the ratchet Plan 3A used: every later Plan 9 commit adds one assertion
 * group that fails before the change and passes after it, so the plan is
 * measured rather than claimed.
 *
 * What is measured here, and what each test is for:
 *
 *  - [odtUnmodifiedRoundTripPreservesTextAndElementCount] is the "preserved"
 *    path. With `isModified == false` and a package present, `OdtDocumentWriter`
 *    copies the original `content.xml` byte-for-byte, so this is the baseline
 *    for "the package we rebuild is still a package the parser accepts".
 *  - [modifiedOdtSaveIsRefusedForEveryFixture] records the finding that no
 *    fixture can reach `buildContentXml` at all, which is why §2.1 of the plan
 *    has to be proven with synthetic documents rather than samples. See
 *    [freshDocumentRoundTripProducesAReadablePackage].
 *  - [freshDocumentRoundTripProducesAReadablePackage] is the path a new
 *    document takes: no package data, so `content.xml` is generated from
 *    scratch. `templates/styles/Default.ott` carries no `content.xml`
 *    (audit-025 §5.1 finding J), so this is the only round trip that exercises
 *    creating that part instead of updating it.
 *  - [docxSaveStillRefusesWhenTheDocumentCarriesImages] pins today's DOCX
 *    refusal so Plan 9b's removal of it is a measured change, not a claim.
 *  - [roundTripOutputStaysOutOfTheFixtureDirectory] is the guard for
 *    cross-plan invariant 6.
 *
 * Robolectric is not optional: the parsers drive `org.xmlpull.v1`, whose only
 * implementation ships in the Android runtime.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan9RoundTripTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    /** `style:name="..."` on a `<style:style>` declaration. */
    private val DECLARED_STYLE_NAME = Regex("style:name=\"([^\"]+)\"")

    /** Any of the three attributes that point at a style by name. */
    private val STYLE_REFERENCE = Regex("(?:text:style-name|table:style-name|draw:style-name)=\"([^\"]+)\"")

    private fun fixture(name: String): File {
        val direct = SampleMatrix.findTestFile(name)
        assertTrue(
            "Missing fixture $name (looked at ${direct.absolutePath}); run tests from the repo root",
            direct.isFile
        )
        return direct
    }

    /**
     * Every file this test writes lands here. Never `tests/inky`: re-saving a
     * fixture is a deliberate act that updates `fixture-identities.properties`
     * in the same commit, and `FixtureIdentityTest` is the guard.
     */
    private fun scratch(name: String): File {
        val dir = File(context.cacheDir, "plan9-roundtrip").apply { mkdirs() }
        assertTrue("scratch dir must live under cacheDir", dir.absolutePath.contains("cache"))
        return File(dir, name)
    }

    private fun sha256(file: File): String =
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }

    private fun zipEntries(bytes: ByteArray): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val buffer = ByteArrayOutputStream()
                zip.copyTo(buffer)
                out[entry.name] = buffer.toString(Charsets.UTF_8)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out
    }

    // ------------------------------------------------------------------
    // ODT: the preserved path
    // ------------------------------------------------------------------

    @Test
    fun odtUnmodifiedRoundTripPreservesTextAndElementCount() = runBlocking {
        val parser = OfficeDocumentParser(context)
        for (sample in SampleMatrix.sampleNumbers) {
            val name = SampleMatrix.odtName(sample)
            val source = fixture(name)
            val original = parser.parseDocument(source, bypassCache = true)
            assertFalse("$name failed to parse: ${original.failureReason}", original.isParsingFailed)

            val document = original.toOfficeDocument()
            val bytes = OdtDocumentWriter().write(document)
            val target = scratch(name)
            try {
                target.writeBytes(bytes)
                val reopened = parser.parseDocument(target, bypassCache = true)
                assertFalse(
                    "$name did not re-open after a save: ${reopened.failureReason}",
                    reopened.isParsingFailed
                )
                assertEquals(
                    "$name element count changed across an unmodified save",
                    original.elements.size,
                    reopened.elements.size
                )
                assertEquals(
                    "$name plain text changed across an unmodified save",
                    original.plainText,
                    reopened.plainText
                )
            } finally {
                target.delete()
            }
        }
    }

    // ------------------------------------------------------------------
    // ODT: the generated path is unreachable through any fixture
    // ------------------------------------------------------------------

    /**
     * Measured at the time of writing, against `tests/inky`:
     *
     *  - `detectSourceFeatures` (`OdtImportPipeline.kt:231`) sets
     *    `hasAdvancedTables` when it sees ANY `<table:table>` element, and
     *    `hasAuthoredIndexes` when it sees ANY `table-of-content` element.
     *  - Samples 1, 2, 3 and 6 contain tables; samples 2, 4, 5 and 6 contain a
     *    table of contents.
     *  - `saveCapability` (`OdtDocumentWriter.kt:142`) refuses a modified save
     *    for either.
     *
     * So all six land on the refusal list and none can reach
     * `buildContentXml`, which is the code Plan 9 §2.1 rewrites. The plan
     * therefore has to prove §2.1 with synthetic documents, not samples. This
     * test is the pin: when Plan 9 narrows the refusal, this test is the thing
     * that says so.
     */
    @Test
    fun modifiedOdtSaveIsRefusedForEveryFixture() = runBlocking {
        val parser = OfficeDocumentParser(context)
        val writer = OdtDocumentWriter()
        val report = StringBuilder()
        for (sample in SampleMatrix.sampleNumbers) {
            val name = SampleMatrix.odtName(sample)
            val parsed = parser.parseDocument(fixture(name), bypassCache = true)
            assertFalse("$name failed to parse: ${parsed.failureReason}", parsed.isParsingFailed)
            val modified = parsed.toOfficeDocument().copy(isModified = true)
            val capability = writer.saveCapability(modified)
            report.append("$name -> ${capability.blockingFeatures}\n")
            assertFalse(
                "$name was expected to be refused for a modified save today; " +
                    "if Plan 9 has opened this path, replace this assertion with a round trip",
                capability.isSupported
            )
        }
        println("=== PLAN 9 BASELINE: modified-ODT refusal per fixture ===\n$report")
    }

    // ------------------------------------------------------------------
    // ODT: the fresh-document path, which is the one with no content.xml
    // ------------------------------------------------------------------

    @Test
    fun freshDocumentRoundTripProducesAReadablePackage() = runBlocking {
        val body: List<OfficeElement> = listOf(
            OfficeParagraph("Plan nine opening paragraph."),
            OfficeHeading("Plan nine heading", level = 2),
            OfficeListItem("First item", bullet = "1. "),
            OfficeListItem("Second item", bullet = "2. "),
            OfficeParagraph("Closing paragraph."),
            OfficeTable(
                rows = listOf(
                    OfficeTableRow(
                        cells = listOf(
                            OfficeTableCell("Top left"),
                            OfficeTableCell("Top right")
                        )
                    ),
                    OfficeTableRow(
                        cells = listOf(
                            OfficeTableCell("Bottom left"),
                            OfficeTableCell("Bottom right")
                        )
                    )
                ),
                numColumns = 2
            )
        )
        val document = OfficeDocument(
            metadata = DocumentMetadata(title = "Plan nine fresh document"),
            body = DocumentBody(elements = body)
        )

        // No odtPackageData, so write() takes the fresh-generation branch and
        // buildContentXml() has to create content.xml rather than copy it.
        val bytes = OdtDocumentWriter().write(document)

        val entries = zipEntries(bytes)
        for (required in listOf(
            "mimetype",
            "content.xml",
            "styles.xml",
            "meta.xml",
            "settings.xml",
            "META-INF/manifest.xml"
        )) {
            assertTrue("generated package is missing $required", entries.containsKey(required))
        }

        // mimetype must be the first entry and STORED, per ODF Part 2.
        ZipInputStream(bytes.inputStream()).use { zip ->
            val first = zip.nextEntry
            assertEquals("mimetype must be the first zip entry", "mimetype", first?.name)
            assertEquals("mimetype must be STORED", ZipEntry.STORED, first!!.method)
        }
        assertEquals(
            "mimetype must declare an ODF text document",
            "application/vnd.oasis.opendocument.text",
            entries["mimetype"]
        )

        val target = scratch("fresh-document.odt")
        try {
            target.writeBytes(bytes)
            val reopened = OfficeDocumentParser(context).parseDocument(target, bypassCache = true)
            assertFalse(
                "a freshly generated ODT must re-open: ${reopened.failureReason}",
                reopened.isParsingFailed
            )
            val text = reopened.plainText
            for (expected in listOf(
                "Plan nine opening paragraph.",
                "Plan nine heading",
                "First item",
                "Second item",
                "Closing paragraph.",
                "Top left",
                "Top right",
                "Bottom left",
                "Bottom right"
            )) {
                assertTrue(
                    "re-opened document lost \"$expected\"\n--- plain text ---\n$text",
                    text.contains(expected)
                )
            }
        } finally {
            target.delete()
        }
    }

    /**
     * The smallest document the app can produce, which is the shape a new
     * document has before anyone types: no table, no list, no package data.
     * `office:version` is still `1.2` today; §2.1 item 6 moves it to `1.4`,
     * and this test is the before-picture.
     */
    @Test
    fun smallestFreshDocumentStillRoundTrips() = runBlocking {
        val document = OfficeDocument(
            metadata = DocumentMetadata(title = "Plan nine smallest document"),
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph("Only paragraph.")
                )
            )
        )
        val bytes = OdtDocumentWriter().write(document)
        val target = scratch("smallest.odt")
        try {
            target.writeBytes(bytes)
            val reopened = OfficeDocumentParser(context).parseDocument(target, bypassCache = true)
            assertFalse(
                "the smallest generated ODT must re-open: ${reopened.failureReason}",
                reopened.isParsingFailed
            )
            assertTrue(
                "lost the only paragraph: ${reopened.plainText}",
                reopened.plainText.contains("Only paragraph.")
            )
        } finally {
            target.delete()
        }
    }

    // ------------------------------------------------------------------
    // Commit 2: ODT styles. Decision D4, the document owns its names.
    // ------------------------------------------------------------------

    /**
     * A source `content.xml` in the shape the six fixtures actually have: an
     * automatic `P1` that is really the document's title, and a `Table1` with
     * the document's own width. Both names collide with the writer's hardcoded
     * eleven, which is the normal case rather than an edge case — all six
     * `tests/inky` fixtures declare `P1`, and four declare `Table1`.
     */
    private fun sourceContentXmlWithOwnStyles(): String = """
<?xml version="1.0" encoding="UTF-8"?>
<office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">
  <office:automatic-styles>
    <style:style style:name="P1" style:family="paragraph" style:parent-style-name="Title"/>
    <style:style style:name="Table1" style:family="table"><style:table-properties style:width="15.905cm" table:align="left"/></style:style>
  </office:automatic-styles>
  <office:body><office:text><text:p text:style-name="P1">The title</text:p></office:text></office:body>
</office:document-content>
    """.trimIndent()

    /**
     * Forces the generating path with a package present: `isModified` skips the
     * byte-exact copy, and the default `OdtSourceFeatures` keeps
     * `saveCapability` supported, which no real fixture can do.
     */
    private fun documentWithOwnStyles(body: List<OfficeElement>): OfficeDocument = OfficeDocument(
        body = DocumentBody(elements = body),
        odtPackageData = OdtPackageData(
            entries = mapOf("content.xml" to sourceContentXmlWithOwnStyles().toByteArray(Charsets.UTF_8)),
            originalContentXml = sourceContentXmlWithOwnStyles()
        ),
        isModified = true
    )

    @Test
    fun documentStyleNamesSurviveARegeneratedContentXml() {
        val document = documentWithOwnStyles(
            listOf(
                OfficeParagraph("The title", styleName = "P1"),
                OfficeParagraph("Body text with no style of its own")
            )
        )
        val content = zipEntries(OdtDocumentWriter().write(document))["content.xml"]
            ?: error("the package must contain a content.xml")

        // The document's own definition is copied through verbatim.
        assertTrue(
            "the document's P1 must keep its own definition:\n$content",
            content.contains("style:name=\"P1\" style:family=\"paragraph\" style:parent-style-name=\"Title\"")
        )
        // The writer stepped aside rather than taking the name.
        assertTrue(
            "the writer must move to P1_papirus_1 rather than reuse P1:\n$content",
            content.contains("style:name=\"P1_papirus_1\"")
        )
        // And each paragraph keeps pointing at the style it arrived with.
        assertTrue(
            "the titled paragraph must still reference the document's P1:\n$content",
            content.contains("<text:p text:style-name=\"P1\">The title</text:p>")
        )
        assertTrue(
            "an unstyled paragraph must reference the writer's moved name, not P1:\n$content",
            content.contains("<text:p text:style-name=\"P1_papirus_1\">Body text with no style of its own</text:p>")
        )
    }

    @Test
    fun everyStyleReferenceInTheGeneratedContentResolvesInThePackage() {
        val document = documentWithOwnStyles(
            listOf(
                OfficeParagraph("The title", styleName = "P1"),
                OfficeParagraph("Plain body"),
                OfficeListItem("First item"),
                OfficeTable(
                    rows = listOf(OfficeTableRow(cells = listOf(OfficeTableCell("a"), OfficeTableCell("b")))),
                    numColumns = 2
                )
            )
        )
        val entries = zipEntries(OdtDocumentWriter().write(document))
        val content = entries["content.xml"] ?: error("the package must contain a content.xml")
        val declared = DECLARED_STYLE_NAME.findAll(content).map { it.groupValues[1] }.toSet()
        val referenced = STYLE_REFERENCE.findAll(content).map { it.groupValues[1] }.toSet()
        val dangling = referenced - declared
        assertTrue(
            "these references have no style in the package: $dangling\n--- content ---\n$content",
            dangling.isEmpty()
        )
        // The table style moved aside too, because the source declares Table1.
        assertTrue(
            "the table must use the moved name, not the document's Table1:\n$content",
            content.contains("style:name=\"Table1_papirus_1\"")
        )
    }

    @Test
    fun aColouredRunKeepsItsColourInsteadOfBeingDropped() {
        val document = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph(
                        "red text",
                        runs = listOf(OfficeTextRun("red text", colorHex = "#ff0000"))
                    )
                )
            )
        )
        val content = zipEntries(OdtDocumentWriter().write(document))["content.xml"]
            ?: error("the package must contain a content.xml")
        assertTrue(
            "a run colour was dropped on save:\n$content",
            content.contains("fo:color=\"#ff0000\"")
        )
        assertTrue(
            "the coloured run must reference a style the package declares:\n$content",
            content.contains("<text:span text:style-name=\"T_papirus\">red text</text:span>")
        )
    }

    @Test
    fun generatedRootsDeclareOfficeVersion14() {
        val document = OfficeDocument(
            body = DocumentBody(elements = listOf(OfficeParagraph("Version check.")))
        )
        val entries = zipEntries(OdtDocumentWriter().write(document))
        for (part in listOf("content.xml", "styles.xml", "meta.xml", "settings.xml")) {
            val xml = entries[part] ?: error("the package must contain $part")
            assertTrue(
                "$part must declare office:version=\"1.4\" (was 1.2):\n$xml",
                xml.contains("office:version=\"1.4\"")
            )
        }
        // The manifest version is a different attribute and must not move.
        val manifest = entries["META-INF/manifest.xml"] ?: error("the package must contain a manifest")
        assertTrue(
            "manifest:version is the manifest format and stays 1.2:\n$manifest",
            manifest.contains("manifest:version=\"1.2\"")
        )
    }

    // ------------------------------------------------------------------
    // DOCX: today's refusal, pinned so Plan 9b can remove it as a measurement
    // ------------------------------------------------------------------

    @Test
    fun docxSaveStillRefusesWhenTheDocumentCarriesImages() = runBlocking {
        val parser = OfficeDocumentParser(context)
        val parsed = parser.parseDocument(fixture(SampleMatrix.docxName(6)), bypassCache = true)
        assertFalse("Sample-6.docx failed to parse: ${parsed.failureReason}", parsed.isParsingFailed)
        val document = parsed.toOfficeDocument()
        // containsEmbeddedImages() is internal to the data module, so the
        // refusal is observed through the serializer's false return instead.
        val target = scratch("Sample-6.docx")
        try {
            val saved = com.makerandreas.papirusoffice.data.DocumentSerializer(context)
                .serializeToFormat(document, "DOCX", target)
            assertFalse(
                "the DOCX writer still refuses images today; " +
                    "when Plan 9b lands the image path, flip this to assertTrue and add the round trip",
                saved
            )
        } finally {
            target.delete()
        }
    }

    // ------------------------------------------------------------------
    // Invariant 6: a test must never write into the fixture directory
    // ------------------------------------------------------------------

    @Test
    fun roundTripOutputStaysOutOfTheFixtureDirectory() = runBlocking {
        val parser = OfficeDocumentParser(context)
        val name = SampleMatrix.odtName(1)
        val source = fixture(name)
        val before = sha256(source)
        val beforeSize = source.length()

        val document = parser.parseDocument(source, bypassCache = true).toOfficeDocument()
        val target = scratch(name)
        try {
            assertTrue(
                "round-trip output must live under cacheDir, was ${target.absolutePath}",
                target.absolutePath.startsWith(context.cacheDir.absolutePath)
            )
            target.writeBytes(OdtDocumentWriter().write(document))
            assertTrue("the round-trip target must exist", target.isFile)
        } finally {
            target.delete()
        }

        assertEquals("a fixture was rewritten: $name", beforeSize, source.length())
        assertEquals("a fixture was rewritten: $name", before, sha256(source))
    }
}
