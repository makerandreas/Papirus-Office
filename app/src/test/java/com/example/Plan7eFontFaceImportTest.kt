package com.example

import com.makerandreas.papirusoffice.data.OfficeFontFace
import com.makerandreas.papirusoffice.data.odf.FontFaceResolver
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.toOfficeDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Plan 7E commit 1 exit gate: `office:font-face-decls` is read from both
 * package parts into `DocumentStyles.fontFaces`, keyed by `style:name`
 * (ODF 1.4 Part 3 3.14, 19.502.3, 19.532).
 *
 * The six ODT fixtures are the corpus. Their declaration counts and the
 * alias-to-family pairs were re-derived from the raw ZIPs on 2026-10-04 and
 * are asserted here, so a fixture regeneration cannot drop a declaration
 * without a red test. The synthetic packages cover what the corpus cannot:
 * both parts read, first declaration wins, malformed declarations skipped,
 * and a package whose only part is `styles.xml`.
 *
 * The declaration tables and expectations were aligned with the recovered
 * original diff of the first 7E build on 2026-10-04; see `audit-017`
 * section 13 for the comparison.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7eFontFaceImportTest {

    private val expectedFaceCounts = mapOf(1 to 9, 2 to 10, 3 to 12, 4 to 12, 5 to 10, 6 to 13)

    /** The generated aliases of the brief plus `Basic Sans1`, which Sample-4 also declares. */
    private val expectedAliases = mapOf(
        "Aptos1" to "Aptos",
        "Aptos2" to "Aptos",
        "Aptos Display1" to "Aptos Display",
        "Times New Roman1" to "Times New Roman",
        "Arial1" to "Arial",
        "Noto Sans Devanagari1" to "Noto Sans Devanagari",
        "Basic Sans1" to "Basic Sans"
    )

    private val aliasSamples = mapOf(
        "Aptos1" to setOf(1, 2, 3, 4, 5, 6),
        "Aptos2" to setOf(6),
        "Aptos Display1" to setOf(1, 2, 3, 5, 6),
        "Times New Roman1" to setOf(1, 2, 4, 5, 6),
        "Arial1" to setOf(4),
        "Noto Sans Devanagari1" to setOf(2, 3, 4, 5, 6),
        "Basic Sans1" to setOf(4)
    )

    @Test
    fun everyOdtFixtureDeclaresItsFontFacesWithTheFamilyEachAliasNames() {
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val faces = parsed.styles.fontFaces
            assertEquals("${file.name} declaration count", expectedFaceCounts.getValue(sample), faces.size)
            for ((alias, family) in expectedAliases) {
                val declared = faces[alias]
                if (sample in aliasSamples.getValue(alias)) {
                    assertNotNull("${file.name} must declare $alias", declared)
                    assertEquals("${file.name} $alias", family, declared!!.family)
                    assertEquals("${file.name} $alias name", alias, declared.name)
                } else {
                    assertNull("${file.name} must not declare $alias", declared)
                }
            }
            // A declaration is never its own family by accident: a name that is a
            // real family carries that family, which is what the resolver leans on.
            for (face in faces.values) {
                assertTrue("${file.name} ${face.name} family blank", face.family.isNotBlank())
                assertTrue("${file.name} ${face.name} name blank", face.name.isNotBlank())
            }
        }
    }

    @Test
    fun declarationsCarryGenericFamilyPitchAndCharsetFromTheSource() {
        val sampleSix = OdtImportPipeline().parse(SampleMatrix.findTestFile("Sample-6.odt"))
        assertFalse(sampleSix.isParsingFailed)
        val aptos = requireNotNull(sampleSix.styles.fontFaces["Aptos"])
        assertEquals("Aptos", aptos.family)
        assertEquals("swiss", aptos.genericFamily)
        assertEquals("variable", aptos.pitch)
        assertEquals("x-symbol", aptos.charset)

        val sampleOne = OdtImportPipeline().parse(SampleMatrix.findTestFile("Sample-1.odt"))
        val display = requireNotNull(sampleOne.styles.fontFaces["Aptos Display"])
        assertEquals("Aptos Display", display.family)
        assertEquals("swiss", display.genericFamily)
        assertEquals("variable", display.pitch)
        assertNull("Sample-1 declares no charset", display.charset)
    }

    @Test
    fun importedFacesAreCarriedIntoTheOfficeDocument() {
        val file = SampleMatrix.findTestFile("Sample-6.odt")
        val parsed = OdtImportPipeline().parse(file)
        val document = parsed.toOfficeDocument()
        assertEquals(parsed.styles.fontFaces, document.styles.fontFaces)
        assertEquals(13, document.styles.fontFaces.size)
    }

    @Test
    fun declarationsComeFromStylesXmlWhenContentXmlHasNone() {
        // Only styles.xml carries the declarations: the pipeline parses that part
        // first, and the resolver must still see the aliases of the styles it reads.
        val bytes = odtPackage(
            mapOf(
                "mimetype" to "application/vnd.oasis.opendocument.text",
                "styles.xml" to """
                    <office:document-styles
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                        xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                        xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
                      <office:font-face-decls>
                        <style:font-face style:name="Body Face1" svg:font-family="&apos;Body Face&apos;" style:font-family-generic="roman" style:font-pitch="variable"/>
                        <style:font-face style:name="Symbol1" svg:font-family="Symbol" style:font-family-generic="system" style:font-pitch="variable"/>
                      </office:font-face-decls>
                      <office:styles>
                        <style:style style:name="Body" style:family="paragraph">
                          <style:text-properties style:font-name="Body Face1" fo:font-size="11pt"/>
                        </style:style>
                      </office:styles>
                    </office:document-styles>
                """.trimIndent()
            )
        )
        val parsed = OdtImportPipeline().parse(bytes, "styles-only-faces.odt")
        assertFalse(parsed.isParsingFailed)
        assertEquals(setOf("Body Face1", "Symbol1"), parsed.styles.fontFaces.keys)
        assertEquals("Body Face", parsed.styles.fontFaces.getValue("Body Face1").family)
        assertEquals("roman", parsed.styles.fontFaces.getValue("Body Face1").genericFamily)
    }

    @Test
    fun repeatedDeclarationsKeepTheFirstAndMalformedOnesAreSkipped() {
        // styles.xml is read before content.xml, so its declaration wins; a blank
        // family or a missing style:name is dropped rather than stored as a face.
        val bytes = odtPackage(
            mapOf(
                "mimetype" to "application/vnd.oasis.opendocument.text",
                "styles.xml" to """
                    <office:document-styles
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                        xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0">
                      <office:font-face-decls>
                        <style:font-face style:name="Alias1" svg:font-family="First"/>
                        <style:font-face style:name="Blank1" svg:font-family=""/>
                        <style:font-face svg:font-family="NoName"/>
                      </office:font-face-decls>
                    </office:document-styles>
                """.trimIndent(),
                "content.xml" to """
                    <office:document-content
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                        xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                      <office:font-face-decls>
                        <style:font-face style:name="Alias1" svg:font-family="Second"/>
                        <style:font-face style:name="Content1" svg:font-family="Content Family"/>
                      </office:font-face-decls>
                      <office:body><office:text><text:p>Body</text:p></office:text></office:body>
                    </office:document-content>
                """.trimIndent()
            )
        )
        val parsed = OdtImportPipeline().parse(bytes, "duplicate-faces.odt")
        assertFalse(parsed.isParsingFailed)
        assertEquals("First", parsed.styles.fontFaces.getValue("Alias1").family)
        assertEquals("Content Family", parsed.styles.fontFaces.getValue("Content1").family)
        assertEquals(setOf("Alias1", "Content1"), parsed.styles.fontFaces.keys)
    }

    @Test
    fun aQuotedFamilyListStoresItsFirstFamily() {
        assertEquals("Times New Roman", FontFaceResolver.firstFamily("'Times New Roman', serif"))
        assertEquals("Aptos Display", FontFaceResolver.firstFamily("'Aptos Display'"))
        assertEquals("Symbol", FontFaceResolver.firstFamily("Symbol"))
        assertNull(FontFaceResolver.firstFamily("   "))
        assertNull(FontFaceResolver.firstFamily(null))

        val faces = mapOf("Devanagari2" to OfficeFontFace("Devanagari2", "Noto Sans Devanagari"))
        assertEquals("Noto Sans Devanagari", FontFaceResolver.familyFor("Devanagari2", faces))
    }

    @Test
    fun contentOnlyPackageStillImportsItsDeclarations() {
        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:font-face-decls>
                <style:font-face style:name="ContentOnly1" svg:font-family="Content Only"/>
              </office:font-face-decls>
              <office:body><office:text><text:p>Body</text:p></office:text></office:body>
            </office:document-content>
        """.trimIndent()

        val parsed = OdtImportPipeline().parse(
            odtPackage(mapOf("content.xml" to contentXml)),
            "content-only.odt"
        )
        assertFalse(parsed.failureReason, parsed.isParsingFailed)
        assertTrue(parsed.styles.fontFaces.containsKey("ContentOnly1"))
    }

    private fun odtPackage(entries: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, value) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
