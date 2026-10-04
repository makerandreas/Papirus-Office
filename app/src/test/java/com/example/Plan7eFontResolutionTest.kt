package com.example

import androidx.compose.ui.text.font.FontFamily
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.FontRegistry
import com.makerandreas.papirusoffice.data.FontSource
import com.makerandreas.papirusoffice.data.GenericFamily
import com.makerandreas.papirusoffice.data.OfficeFontFace
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import com.makerandreas.papirusoffice.data.TextMetrics
import com.makerandreas.papirusoffice.data.odf.FontFaceResolver
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
 * Plan 7E commit 2 exit gate: `style:font-name` resolves through the
 * declaration table before [FontRegistry] sees it, and the resolved family is
 * the one both measurement (`TextMetrics.forStyle`) and display
 * (`OfficeRuns.fontFamilyFor`) use.
 *
 * The union of the fixture declarations is written the way the fixtures
 * declare them: an alias names a family that can then be substituted, and the
 * alias itself carries the generic value `system`, so classification has to
 * come from the declared family rather than from the alias spelling.
 *
 * The corpus tests and the synthetic package test were aligned with the
 * recovered original diff of the first 7E build on 2026-10-04; see
 * `audit-017` section 13 for the comparison.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7eFontResolutionTest {

    private val faces: Map<String, OfficeFontFace> = mapOf(
        "Times New Roman1" to OfficeFontFace("Times New Roman1", "Times New Roman", "system", "variable"),
        "Aptos1" to OfficeFontFace("Aptos1", "Aptos", "system", "variable"),
        "Aptos2" to OfficeFontFace("Aptos2", "Aptos", "system", "variable"),
        "Aptos Display1" to OfficeFontFace("Aptos Display1", "Aptos Display", "system", "variable"),
        "List" to OfficeFontFace("List", "Gentium Basic, Liberation Serif", "roman", "variable")
    )

    /** Every declaration name the six fixtures carry, paired with the family it answers. */
    private val fixtureFaces: Map<String, OfficeFontFace> = mapOf(
        "Aptos" to "Aptos",
        "Aptos1" to "Aptos",
        "Aptos2" to "Aptos",
        "Aptos Display" to "Aptos Display",
        "Aptos Display1" to "Aptos Display",
        "Arial" to "Arial",
        "Arial1" to "Arial",
        "Basic Sans" to "Basic Sans",
        "Basic Sans1" to "Basic Sans",
        "Noto Sans Devanagari" to "Noto Sans Devanagari",
        "Noto Sans Devanagari1" to "Noto Sans Devanagari",
        "Times New Roman" to "Times New Roman",
        "Times New Roman1" to "Times New Roman"
    ).mapValues { (name, family) -> OfficeFontFace(name, family) }

    /** One decision means the same replacement on every field that drives a face. */
    private fun assertSameDecision(alias: String?, direct: String?) {
        val a = FontRegistry.resolve(alias)
        val b = FontRegistry.resolve(direct)
        assertEquals("family", b.family, a.family)
        assertEquals("generic class", b.generic, a.generic)
        assertEquals("source", b.source, a.source)
        assertEquals("metric compatibility", b.metricCompatible, a.metricCompatible)
        assertEquals("asset stem", b.assetStem, a.assetStem)
    }

    @Test
    fun aliasAndDirectNamesProduceTheSameFontChoice() {
        // Any name the corpus declares, alias or real family, answers its
        // family, and both forms reach one FontChoice.
        for ((name, face) in fixtureFaces) {
            assertEquals("$name is not an alias here", face.family, FontFaceResolver.familyFor(name, fixtureFaces))
            assertEquals(
                "$name and ${face.family} must reach one decision",
                FontRegistry.resolve(face.family),
                FontRegistry.resolve(FontFaceResolver.familyFor(name, fixtureFaces))
            )
        }

        assertSameDecision(FontFaceResolver.familyFor("Times New Roman1", faces), "Times New Roman")
        assertSameDecision(FontFaceResolver.familyFor("Aptos1", faces), "Aptos")
        assertSameDecision(FontFaceResolver.familyFor("Aptos Display1", faces), "Aptos Display")

        val alias = FontRegistry.resolve(FontFaceResolver.familyFor("Times New Roman1", faces))
        assertEquals(FontSource.BUNDLED_METRIC_COMPATIBLE, alias.source)
        assertEquals("Liberation Serif", alias.family)
        assertEquals("LiberationSerif", alias.assetStem)
        assertEquals(GenericFamily.SERIF, alias.generic)
        assertTrue("Liberation Serif shares Times New Roman advances", alias.metricCompatible)
        // The registry sees the resolved string; the alias itself never
        // reaches it, which is exactly what the resolution is for.
        assertEquals("Times New Roman", alias.requested)
        assertEquals("Times New Roman1", FontRegistry.resolve("Times New Roman1").requested)
    }

    @Test
    fun aptosAliasesReachTheStandInAndStayNonMetricCompatible() {
        for (alias in listOf("Aptos1", "Aptos2", "Aptos Display1")) {
            val choice = FontRegistry.resolve(FontFaceResolver.familyFor(alias, faces))
            assertEquals(alias, FontSource.BUNDLED_STAND_IN, choice.source)
            assertEquals(alias, "Martel Sans", choice.family)
            assertEquals(alias, "MartelSans", choice.assetStem)
            assertEquals(alias, GenericFamily.SANS_SERIF, choice.generic)
            assertFalse("$alias must not claim metric compatibility", choice.metricCompatible)
        }
        // The alias spelling alone is not a class hint. This is the decision
        // the resolution replaces, and the reason page metrics can move.
        assertEquals(FontSource.DEFAULT, FontRegistry.resolve("Aptos1").source)
        assertEquals(GenericFamily.DEFAULT, FontRegistry.resolve("Aptos1").generic)
    }

    @Test
    fun everyFixtureDeclarationResolvesAndUndeclaredNamesFallBackToTheReference() {
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val faces = parsed.styles.fontFaces
            for (face in faces.values) {
                assertEquals("${file.name} ${face.name}", face.family, FontFaceResolver.familyFor(face.name, faces))
            }
            // An undeclared reference is handed to the registry unchanged instead
            // of becoming null, so a substitution or a generic class can still answer.
            assertEquals("Mystery Face1", FontFaceResolver.familyFor("Mystery Face1", faces))
            assertEquals("Symbol", FontFaceResolver.familyFor("Symbol", faces))
            assertNull(FontFaceResolver.familyFor("   ", faces))
        }
    }

    @Test
    fun aDeclarationThatRepeatsItsOwnNameIsNotASecondLookup() {
        val selfNamed = mapOf("Aptos" to OfficeFontFace("Aptos", "Aptos"))
        assertEquals("Aptos", FontFaceResolver.familyFor("Aptos", selfNamed))
        assertEquals(FontSource.BUNDLED_STAND_IN, FontRegistry.resolve(FontFaceResolver.familyFor("Aptos", selfNamed)).source)
    }

    @Test
    fun familyListsAndQuotesAreHandled() {
        assertEquals("Gentium Basic", FontFaceResolver.familyFor("List", faces))
        assertEquals("Liberation Serif", FontFaceResolver.firstFamily(" , 'Liberation Serif', monospace "))
        assertEquals("Times New Roman", FontFaceResolver.firstFamily("\"Times New Roman\""))
        assertNull(FontFaceResolver.firstFamily(" , "))
        assertNull(FontFaceResolver.firstFamily(null))
    }

    @Test
    fun theAliasAndTheFamilyListFormAgreeInAnImportedStyle() {
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
                        <style:font-face style:name="Times New Roman1" svg:font-family="&apos;Times New Roman&apos;" style:font-family-generic="roman" style:font-pitch="variable"/>
                      </office:font-face-decls>
                    </office:document-styles>
                """.trimIndent(),
                "content.xml" to """
                    <office:document-content
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                        xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                      <office:automatic-styles>
                        <style:style style:name="AliasBody" style:family="paragraph">
                          <style:text-properties style:font-name="Times New Roman1" fo:font-size="12pt"/>
                        </style:style>
                        <style:style style:name="ListBody" style:family="paragraph">
                          <style:text-properties fo:font-family="&apos;Times New Roman&apos;, serif" fo:font-size="12pt"/>
                        </style:style>
                      </office:automatic-styles>
                      <office:body><office:text>
                        <text:p text:style-name="AliasBody">Alias body</text:p>
                        <text:p text:style-name="ListBody">List body</text:p>
                      </office:text></office:body>
                    </office:document-content>
                """.trimIndent()
            )
        )
        val parsed = OdtImportPipeline().parse(bytes, "alias-and-list.odt")
        assertFalse(parsed.isParsingFailed)
        val aliasStyle = requireNotNull(parsed.styles.paragraphStyles["AliasBody"]) { "AliasBody must import" }
        val listStyle = requireNotNull(parsed.styles.paragraphStyles["ListBody"]) { "ListBody must import" }
        assertEquals("Times New Roman", aliasStyle.fontFamily)
        assertEquals("Times New Roman", listStyle.fontFamily)

        // One decision for measurement and display, from either spelling.
        val measured = TextMetrics.forStyle(aliasStyle, TableAdvanceSource)
        val displayed = OfficeRuns.fontFamilyFor(listStyle.fontFamily)
        assertEquals(FontRegistry.resolve("Times New Roman"), measured.choice)
        assertEquals(measured.choice.composeFamily, displayed)
    }

    @Test
    fun sampleSixDefaultStyleResolvesAptosOneToAptos() {
        val file = SampleMatrix.findTestFile("Sample-6.odt")
        val parsed = OdtImportPipeline().parse(file)
        assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
        val styles = parsed.styles

        assertEquals("Aptos", FontFaceResolver.familyFor("Aptos1", styles.fontFaces))
        // The paragraph default-style declares style:font-name="Aptos1"; the
        // dump printed that alias before 7E (audit-017 section 1, finding 3).
        assertEquals("Aptos", styles.defaultParagraphStyle?.fontFamily)
        assertNotEquals("Aptos1", styles.defaultParagraphStyle?.fontFamily)
        // A direct name in the same file is untouched by the table.
        assertEquals("Times New Roman", styles.paragraphStyles["Standard"]?.fontFamily)

        val choice = TextMetrics.forStyle(styles.defaultParagraphStyle!!, TableAdvanceSource).choice
        assertEquals(FontRegistry.resolve("Aptos"), choice)
        assertEquals("Martel Sans", choice.family)
        assertFalse(choice.metricCompatible)
    }

    @Test
    fun directNamesInTheOtherFixturesKeepTheirDecisions() {
        for (sample in listOf(1, 2, 3, 4, 5)) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val family = parsed.styles.defaultParagraphStyle?.fontFamily
            assertEquals("${file.name} default family", "Aptos", family)
            assertEquals("${file.name} decision", FontSource.BUNDLED_STAND_IN, FontRegistry.resolve(family).source)
        }
    }

    @Test
    fun resolvedFamilyIsTheSingleInputToMeasurementAndDisplay() {
        val style = ParagraphStyle(name = "Standard", fontFamily = FontFaceResolver.familyFor("Aptos1", faces))
        assertEquals("Aptos", style.fontFamily)

        val metrics = TextMetrics.forStyle(style, TableAdvanceSource)
        assertEquals("Martel Sans", metrics.choice.family)
        assertEquals(GenericFamily.SANS_SERIF, metrics.choice.generic)
        assertEquals(FontFamily.SansSerif, metrics.choice.composeFamily)
        assertEquals(FontFamily.SansSerif, OfficeRuns.fontFamilyFor(style.fontFamily))

        // The styles object keeps the table so later consumers can reach the
        // same declaration, and the entry point does not depend on it.
        val styles = DocumentStyles(fontFaces = faces, defaultParagraphStyle = style)
        assertEquals(style.fontFamily, styles.defaultParagraphStyle?.fontFamily)
    }

    @Test
    fun paragraphStylesWithoutADeclaredFontKeepTheGenericSubstitution() {
        // A name with no declaration and no bundled face is still classified, not dropped.
        val style = ParagraphStyle("plain", fontFamily = "Basic Sans")
        val choice = TextMetrics.forStyle(style, TableAdvanceSource).choice
        assertEquals(FontSource.SYSTEM_GENERIC, choice.source)
        assertEquals(GenericFamily.SANS_SERIF, choice.generic)
    }

    @Test
    fun composeFamilyMappingIsUnchangedForEveryResolvedChoice() {
        val expectations = mapOf(
            "Liberation Serif" to FontFamily.Serif,
            "Martel Sans" to FontFamily.SansSerif,
            "Liberation Mono" to FontFamily.Monospace
        )
        for ((family, expected) in expectations) {
            assertEquals(family, expected, FontRegistry.composeFamilyFor(family))
        }
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
