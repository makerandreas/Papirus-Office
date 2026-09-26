package com.example

import com.makerandreas.papirusoffice.data.FontRegistry
import com.makerandreas.papirusoffice.data.GenericFamily
import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import com.makerandreas.papirusoffice.data.TextMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap E-EN-2: one resolved style measures the same way twice, in layout
 * units, with the family the registry decided. The table source is the
 * deterministic JVM path; the Paint path is exercised on device and by the
 * Robolectric dump.
 */
class TextMetricsTest {

    private val body = ParagraphStyle("Normal", fontSizeSp = 12f, fontFamily = "Times New Roman")

    @Test
    fun sizeIsInLayoutUnitsAndFamilyComesFromTheRegistry() {
        val metrics = TextMetrics.forStyle(body, TableAdvanceSource)
        assertEquals(16f, metrics.fontSizeUnits, 0.001f)
        assertEquals(FontRegistry.resolve("Times New Roman"), metrics.choice)
        assertEquals(GenericFamily.SERIF, metrics.choice.generic)
        assertEquals("table", metrics.sourceName)
    }

    @Test
    fun tableAdvancesAreDeterministicAndScaleWithSize() {
        val twelve = TextMetrics.forStyle(body, TableAdvanceSource)
        val twentyFour = TextMetrics.forStyle(body.copy(fontSizeSp = 24f), TableAdvanceSource)
        val text = "Papirus Office paginates honestly."
        assertEquals(twelve.widthOf(text), TextMetrics.forStyle(body, TableAdvanceSource).widthOf(text), 0f)
        assertEquals(twelve.widthOf(text) * 2f, twentyFour.widthOf(text), 0.001f)
        assertEquals(0f, twelve.widthOf(""), 0f)
        // Em fractions from the Times table: "iiii" is narrower than "MMMM".
        assertTrue(twelve.widthOf("MMMM") > twelve.widthOf("iiii") * 2f)
        // A space is a quarter em at 16 units.
        assertEquals(4f, twelve.widthOf(" "), 0.001f)
    }

    @Test
    fun boldAndClassChangeTheAdvance() {
        val regular = TextMetrics.forStyle(body, TableAdvanceSource).widthOf("Sample")
        val bold = TextMetrics.forStyle(body.copy(isBold = true), TableAdvanceSource).widthOf("Sample")
        val sans = TextMetrics.forStyle(body.copy(fontFamily = "Arial"), TableAdvanceSource).widthOf("Sample")
        val mono = TextMetrics.forStyle(body.copy(fontFamily = "Courier New"), TableAdvanceSource).widthOf("Sample")
        assertTrue(bold > regular)
        assertTrue(sans > regular)
        assertEquals(6 * 0.6f * 16f, mono, 0.001f)
    }

    @Test
    fun lineHeightFollowsTheStyleFactorOrExactValue() {
        val single = TextMetrics.forStyle(body, TableAdvanceSource)
        assertEquals(16f * 1.107f, single.naturalLineHeightUnits, 0.001f)
        assertEquals(single.naturalLineHeightUnits, single.lineHeightUnits, 0.001f)

        val proportional = TextMetrics.forStyle(body.copy(lineHeightFactor = 1.15f), TableAdvanceSource)
        assertEquals(single.naturalLineHeightUnits * 1.15f, proportional.lineHeightUnits, 0.001f)

        val exact = TextMetrics.forStyle(body.copy(lineHeightExactUnits = LayoutUnits.cmToUnits(0.5f)), TableAdvanceSource)
        assertEquals(LayoutUnits.cmToUnits(0.5f), exact.lineHeightUnits, 0.001f)
    }

    @Test
    fun defaultStyleCarriesNoMetricFields() {
        // E-EN-3: the pre-plan-5 shape must stay byte-identical for every consumer.
        assertTrue(!ParagraphStyle("Default", fontSizeSp = 14f).hasMetricFields)
        assertTrue(ParagraphStyle("x", spaceAfterUnits = 10f).hasMetricFields)
        assertTrue(ParagraphStyle("x", keepWithNext = true).hasMetricFields)
        assertEquals(ParagraphStyle("x"), ParagraphStyle("x", lineHeightFactor = 1f))
    }
}
