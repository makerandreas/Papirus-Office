package com.example

import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.toOfficeDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * Plan 7E commit 1 exit gate: `office:font-face-decls` from both XML parts
 * reaches [com.makerandreas.papirusoffice.data.DocumentStyles.fontFaces]
 * (ODF 1.4 Part 3 3.14, 19.502.3, 19.532).
 *
 * Fixture face counts were re-derived from the raw packages on 2026-10-04
 * (audit-017 section 4.1): 9, 10, 12, 12, 10, 13 for Samples 1 to 6. The
 * declaration sets of `content.xml` and `styles.xml` are identical in all
 * six files, so the synthetic package is what proves both parts are read and
 * that the first declaration of an alias wins.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7eFontFaceImportTest {

    private val expectedFaceCounts = mapOf(1 to 9, 2 to 10, 3 to 12, 4 to 12, 5 to 10, 6 to 13)

    @Test
    fun everyOdtFixtureImportsItsDeclaredFaces() {
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val faces = parsed.styles.fontFaces
            assertEquals("${file.name} face count", expectedFaceCounts.getValue(sample), faces.size)

            // The document default family of Sample-6 is the alias Aptos1, so
            // the table has to carry it before resolution can use it.
            if (sample == 6) {
                assertEquals("'Aptos Display'", faces.getValue("Aptos Display1").family)
                assertEquals("Aptos", faces.getValue("Aptos2").family)
                assertEquals("x-symbol", faces.getValue("Wingdings").charset)
            }
            if (sample == 4) {
                assertEquals("Arial", faces.getValue("Arial1").family)
                assertEquals("'Basic Sans'", faces.getValue("Basic Sans1").family)
                assertEquals("system", faces.getValue("Basic Sans1").genericFamily)
            }
            if (sample == 1) {
                assertEquals("'Times New Roman'", faces.getValue("Times New Roman1").family)
                assertEquals("system", faces.getValue("Times New Roman1").genericFamily)
                assertEquals("roman", faces.getValue("Times New Roman").genericFamily)
                assertNull(faces.getValue("Times New Roman").charset)
            }
        }
    }

    @Test
    fun bothXmlPartsAreReadAndTheFirstDeclarationOfAnAliasWins() {
        val stylesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0">
              <office:font-face-decls>
                <style:font-face style:name="Shared1" svg:font-family="'Styles Family'"
                    style:font-family-generic="roman" style:font-pitch="variable"/>
                <style:font-face style:name="StylesOnly1" svg:font-family="Styles Only"/>
              </office:font-face-decls>
            </office:document-styles>
        """.trimIndent()
        val contentXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
              <office:font-face-decls>
                <style:font-face style:name="Shared1" svg:font-family="'Content Family'"/>
                <style:font-face style:name="ContentOnly1" svg:font-family="Content Only"/>
                <style:font-face style:name="BlankFamily1" svg:font-family=""/>
              </office:font-face-decls>
              <office:body><office:text><text:p>Body</text:p></office:text></office:body>
            </office:document-content>
        """.trimIndent()

        val parsed = OdtImportPipeline().parse(
            odtPackage(mapOf("content.xml" to contentXml, "styles.xml" to stylesXml)),
            "declarations.odt"
        )
        assertFalse(parsed.failureReason, parsed.isParsingFailed)
        val faces = parsed.styles.fontFaces
        assertEquals("styles.xml is parsed first", "'Styles Family'", faces.getValue("Shared1").family)
        assertEquals("roman", faces.getValue("Shared1").genericFamily)
        assertEquals("content.xml contributes its own declarations", "Content Only", faces.getValue("ContentOnly1").family)
        assertEquals("styles.xml contributes its own declarations", "Styles Only", faces.getValue("StylesOnly1").family)
        assertFalse("a blank svg:font-family is not a declaration", faces.containsKey("BlankFamily1"))
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

    @Test
    fun importedFacesAreCarriedIntoTheOfficeDocument() {
        val file = SampleMatrix.findTestFile("Sample-6.odt")
        val parsed = OdtImportPipeline().parse(file)
        val document = parsed.toOfficeDocument()
        assertEquals(parsed.styles.fontFaces, document.styles.fontFaces)
        assertEquals(13, document.styles.fontFaces.size)
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
