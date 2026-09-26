package com.example

import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.util.OdfLength
import com.makerandreas.papirusoffice.data.util.OpenXmlUnits
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** One converter for every format (roadmap E-EN-1): 96 layout units per inch. */
class LayoutUnitsTest {

    @Test
    fun oneInchIsNinetySixUnitsFromEveryFormat() {
        assertEquals(96f, LayoutUnits.inToUnits(1f), 0.001f)
        assertEquals(96f, LayoutUnits.cmToUnits(2.54f), 0.01f)
        assertEquals(96f, LayoutUnits.mmToUnits(25.4f), 0.01f)
        assertEquals(96f, LayoutUnits.ptToUnits(72f), 0.001f)
        assertEquals(96f, LayoutUnits.twipsToUnits(1440), 0.001f)
        assertEquals(96f, LayoutUnits.twipsToUnits(1440f), 0.001f)
        assertEquals(96f, LayoutUnits.emuToUnits(914400L), 0.001f)
        assertEquals(9525f, LayoutUnits.EMU_PER_UNIT, 0.001f)
    }

    @Test
    fun a4AndLetterBoxesRoundTrip() {
        // A4 as w:pgSz 11907 x 16840 twips and as 21 x 29.7 cm land on the same box.
        assertEquals(LayoutUnits.cmToUnits(21f), LayoutUnits.twipsToUnits(11907), 0.6f)
        assertEquals(LayoutUnits.cmToUnits(29.7f), LayoutUnits.twipsToUnits(16840), 0.6f)
        assertEquals(816f, LayoutUnits.inToUnits(8.5f), 0.001f)
        assertEquals(1056f, LayoutUnits.inToUnits(11f), 0.001f)
        assertEquals(12f, LayoutUnits.unitsToPt(LayoutUnits.ptToUnits(12f)), 0.0001f)
        assertEquals(914400L, LayoutUnits.unitsToEmu(96f))
    }

    @Test
    fun fontSizesStayInPointsAndTwelvePointIsSixteenUnits() {
        assertEquals(16f, LayoutUnits.ptToUnits(12f), 0.001f)
        assertEquals(12f, LayoutUnits.halfPointsToPt(24), 0.001f)
        assertEquals(11f, LayoutUnits.halfPointsToPt(22), 0.001f)
        assertEquals(12f, LayoutUnits.parsePoints("12pt")!!, 0.001f)
        assertEquals(72f, LayoutUnits.parsePoints("1in")!!, 0.001f)
        assertEquals(28.35f, LayoutUnits.parsePoints("1cm")!!, 0.01f)
        assertEquals(12f, LayoutUnits.parsePoints("16px")!!, 0.001f)
        assertNull(LayoutUnits.parsePoints("115%"))
        assertNull(LayoutUnits.parsePoints("0pt"))
        assertNull(LayoutUnits.parsePoints(null))
        assertNull(LayoutUnits.parsePoints("large"))
    }

    @Test
    fun lengthsParseLikeTheOdfReaderAlwaysDid() {
        assertEquals(96f, LayoutUnits.parseLength("2.54cm"), 0.1f)
        assertEquals(96f, LayoutUnits.parseLength("1in"), 0.01f)
        assertEquals(96f, LayoutUnits.parseLength("72pt"), 0.1f)
        assertEquals(96f, LayoutUnits.parseLength("6pc"), 0.1f)
        assertEquals(10f, LayoutUnits.parseLength("10px"), 0.01f)
        assertEquals(96f, LayoutUnits.parseLength("96"), 0.01f)
        assertEquals(5f, LayoutUnits.parseLength(null, fallback = 5f), 0.01f)
        assertEquals(5f, LayoutUnits.parseLength("abc", fallback = 5f), 0.01f)
        assertEquals(5f, LayoutUnits.parseLength("100%", fallback = 5f), 0.01f)
        assertEquals(37.8f, LayoutUnits.parseLength("1cm"), 0.05f)
        assertEquals(0f, LayoutUnits.parseLength("0.000cm"), 0.0001f)
    }

    @Test
    fun lineHeightFactorsReadBothFormats() {
        assertEquals(1.15f, LayoutUnits.parseLineHeightFactor("115%")!!, 0.0001f)
        assertEquals(1.16f, LayoutUnits.parseLineHeightFactor("116%")!!, 0.0001f)
        assertEquals(1f, LayoutUnits.parseLineHeightFactor("100%")!!, 0.0001f)
        assertNull(LayoutUnits.parseLineHeightFactor("0.5cm"))
        assertNull(LayoutUnits.parseLineHeightFactor(null))
        assertEquals(1.15f, LayoutUnits.lineTwentiethsToFactor(276), 0.0001f)
        assertEquals(1.5f, LayoutUnits.lineTwentiethsToFactor(360), 0.0001f)
        assertEquals(1f, LayoutUnits.lineTwentiethsToFactor(240), 0.0001f)
    }

    @Test
    fun legacyEntryPointsDelegateToTheOneConverter() {
        assertEquals(LayoutUnits.parseLength("2.54cm"), OdfLength.toLayoutUnits("2.54cm"), 0.0001f)
        assertEquals(LayoutUnits.twipsToUnits(11907), OdfLength.twipsToLayoutUnits(11907), 0.0001f)
        assertEquals(200.dp, OpenXmlUnits.emuToDp(1905000L))
    }
}
