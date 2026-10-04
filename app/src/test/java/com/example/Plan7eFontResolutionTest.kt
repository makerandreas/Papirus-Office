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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 7E commit 2 exit gate: `style:font-name` resolves through the
 * declaration table before [FontRegistry] sees it, and the resolved family is
 * the one both measurement (`TextMetrics.forStyle`) and display
 * (`OfficeRuns.fontFamilyFor`) use.
 *
 * The tables below are written the way the fixtures declare them
 * (audit-017 section 4.1): an alias names a family that can then be
 * substituted, and the alias itself carries the generic value `system`, so
 * classification has to come from the declared family rather than from the
 * alias spelling.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7eFontResolutionTest {

    private val faces: Map<String, OfficeFontFace> = mapOf(
        "Times New Roman1" to OfficeFontFace("Times New Roman1", "'Times New Roman'", "system", "variable"),
        "Aptos1" to OfficeFontFace("Aptos1", "Aptos", "system", "variable"),
        "Aptos2" to OfficeFontFace("Aptos2", "Aptos", "system", "variable"),
        "Aptos Display1" to OfficeFontFace("Aptos Display1", "'Aptos Display'", "system", "variable"),
        "List" to OfficeFontFace("List", "Gentium Basic, Liberation Serif", "roman", "variable")
    )

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
    fun aliasAndDirectNameProduceTheSameFontChoice() {
        assertEquals("Times New Roman", FontFaceResolver.familyFor("Times New Roman1", faces))
        assertEquals("Aptos", FontFaceResolver.familyFor("Aptos1", faces))
        assertEquals("Aptos Display", FontFaceResolver.familyFor("Aptos Display1", faces))

        assertSameDecision(FontFaceResolver.familyFor("Times New Roman1", faces), "Times New Roman")
        assertSameDecision(FontFaceResolver.familyFor("Aptos1", faces), "Aptos")
        assertSameDecision(FontFaceResolver.familyFor("Aptos Display1", faces), "Aptos Display")

        val alias = FontRegistry.resolve(FontFaceResolver.familyFor("Times New Roman1", faces))
        assertEquals(FontSource.BUNDLED_METRIC_COMPATIBLE, alias.source)
        assertEquals("Liberation Serif", alias.family)
        assertEquals("LiberationSerif", alias.assetStem)
        assertEquals(GenericFamily.SERIF, alias.generic)
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
    fun unusableDeclarationsLeaveTheRawNameToTheRegistry() {
        assertNull(FontFaceResolver.familyFor("Unknown1", faces))
        assertNull(FontFaceResolver.familyFor(null, faces))
        assertNull(FontFaceResolver.familyFor("   ", faces))
        assertNull(FontFaceResolver.familyFor("Aptos1", emptyMap()))
        // The raw alias alone is not a family. Without the table the registry
        // guesses "Times New Roman1" as a serif by its spelling and finds
        // nothing for "Aptos1"; the table is what reaches the declared face.
        val rawAlias = FontRegistry.resolve("Times New Roman1")
        assertEquals(FontSource.SYSTEM_GENERIC, rawAlias.source)
        assertEquals(GenericFamily.SERIF, rawAlias.generic)
        assertEquals("serif", rawAlias.family)
        assertEquals(FontSource.DEFAULT, FontRegistry.resolve("Aptos1").source)
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
}
