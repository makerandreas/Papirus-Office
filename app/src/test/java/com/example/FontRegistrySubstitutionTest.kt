package com.example

import androidx.compose.ui.text.font.FontFamily
import com.makerandreas.papirusoffice.data.FontRegistry
import com.makerandreas.papirusoffice.data.FontSource
import com.makerandreas.papirusoffice.data.GenericFamily
import com.makerandreas.papirusoffice.data.OfficeRuns
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import com.makerandreas.papirusoffice.data.TextMetrics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap E-EN-5: the audit-007 §8 corpus (every family name the six sample
 * pairs declare) resolves to one decision, and measuring and display read
 * that same decision.
 */
class FontRegistrySubstitutionTest {

    /** audit-007 §8, in the order of that table. */
    private val corpus = listOf(
        "Times New Roman", "Aptos", "Aptos Display", "Calibri", "Cambria Math", "Courier New", "Arial",
        "Basic Sans", "Segoe UI Variable", "Microsoft YaHei UI", "MS Mincho"
    )

    @After
    fun clearUserFonts() {
        FontRegistry.clearUserFamilies()
    }

    @Test
    fun metricCompatibleSubstitutesComeFromTheBundledSet() {
        val expectations = mapOf(
            "Times New Roman" to "Liberation Serif",
            "Arial" to "Liberation Sans",
            "Courier New" to "Liberation Mono",
            "Calibri" to "Carlito",
            "Cambria Math" to "Caladea"
        )
        for ((requested, family) in expectations) {
            val choice = FontRegistry.resolve(requested)
            assertEquals(requested, family, choice.family)
            assertEquals(requested, FontSource.BUNDLED_METRIC_COMPATIBLE, choice.source)
            assertTrue(requested, choice.metricCompatible)
            assertNotNull("$requested must name its asset stem", choice.assetStem)
        }
    }

    @Test
    fun aptosStandsInWithMartelSansAndIsRecordedAsNotMetricCompatible() {
        for (requested in listOf("Aptos", "Aptos Display")) {
            val choice = FontRegistry.resolve(requested)
            assertEquals("Martel Sans", choice.family)
            assertEquals(FontSource.BUNDLED_STAND_IN, choice.source)
            assertFalse("Martel Sans does not share Aptos advances", choice.metricCompatible)
            assertEquals("MartelSans", choice.assetStem)
            assertEquals(GenericFamily.SANS_SERIF, choice.generic)
        }
    }

    @Test
    fun namesWithoutAnyBundledFaceFallToAGenericClass() {
        assertEquals(FontSource.SYSTEM_GENERIC, FontRegistry.resolve("Basic Sans").source)
        assertEquals(GenericFamily.SANS_SERIF, FontRegistry.resolve("Segoe UI Variable").generic)
        assertEquals(GenericFamily.SANS_SERIF, FontRegistry.resolve("Microsoft YaHei UI").generic)
        assertEquals(GenericFamily.SERIF, FontRegistry.resolve("MS Mincho").generic)
        val unknown = FontRegistry.resolve("Some Unknown Font")
        assertEquals(FontSource.DEFAULT, unknown.source)
        assertEquals(FontFamily.Default, unknown.composeFamily)
        assertEquals(FontSource.DEFAULT, FontRegistry.resolve(null).source)
        assertEquals(FontSource.DEFAULT, FontRegistry.resolve("  ").source)
    }

    @Test
    fun exactBundledNamesAndQuotedOdfNamesResolveTheSame() {
        val exact = FontRegistry.resolve("Liberation Serif")
        assertEquals(FontSource.BUNDLED_EXACT, exact.source)
        assertEquals("'Times New Roman'", "Liberation Serif", FontRegistry.resolve("'Times New Roman'").family)
        assertEquals("times new roman", FontRegistry.resolve(" TIMES NEW ROMAN ").family, FontRegistry.resolve("Times New Roman").family)
    }

    @Test
    fun userFontsSitBetweenBundledAndSystem() {
        FontRegistry.registerUserFamilies(listOf("Noto Sans Display", "Aptos"))
        val user = FontRegistry.resolve("Noto Sans Display")
        assertEquals(FontSource.USER, user.source)
        assertEquals(GenericFamily.SANS_SERIF, user.generic)
        // A bundled decision still wins over a user font of the same name.
        assertEquals(FontSource.BUNDLED_STAND_IN, FontRegistry.resolve("Aptos").source)
        FontRegistry.clearUserFamilies()
        assertEquals(FontSource.SYSTEM_GENERIC, FontRegistry.resolve("Noto Sans Display").source)
    }

    @Test
    fun measurementAndDisplayReadOneDecisionForTheWholeCorpus() {
        for (name in corpus) {
            val choice = FontRegistry.resolve(name)
            val metrics = TextMetrics.forStyle(ParagraphStyle("body", fontFamily = name), TableAdvanceSource)
            assertEquals(name, choice, metrics.choice)
            assertEquals(name, choice.composeFamily, OfficeRuns.fontFamilyFor(name))
        }
    }

    @Test
    fun legacyFamilySwitchStillHolds() {
        assertEquals(FontFamily.Serif, OfficeRuns.fontFamilyFor("Times New Roman"))
        assertEquals(FontFamily.SansSerif, OfficeRuns.fontFamilyFor("arial"))
        assertEquals(FontFamily.Monospace, OfficeRuns.fontFamilyFor("Courier New"))
        assertEquals(FontFamily.Serif, OfficeRuns.fontFamilyFor("Caladea"))
        assertEquals(FontFamily.SansSerif, OfficeRuns.fontFamilyFor("Carlito"))
        assertEquals(FontFamily.Default, OfficeRuns.fontFamilyFor(null))
    }
}
