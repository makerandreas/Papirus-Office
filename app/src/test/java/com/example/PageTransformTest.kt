package com.example

import com.example.modules.inky.PageTransform
import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.PageStyleSpec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Roadmap E-7 acceptance: at 100 % the on-screen text column equals the
 * layout's content width for any viewport, and every length on the sheet is
 * the same `units * pageScale`.
 */
class PageTransformTest {

    private val a4 = PageStyleSpec(
        name = "a4",
        widthDp = LayoutUnits.cmToUnits(21f),
        heightDp = LayoutUnits.cmToUnits(29.7f),
        marginTopDp = 96f, marginBottomDp = 96f, marginStartDp = 96f, marginEndDp = 96f
    )

    @Test
    fun hundredPercentFillsTheViewportOnEveryScreen() {
        for (viewport in listOf(320f, 360f, 411f, 600f, 840f, 1280f)) {
            val sheet = PageTransform.sheetFor(a4, viewport, 1f)
            assertEquals("viewport $viewport", viewport, sheet.widthDp, 0.001f)
            // Column on screen = (page width - margins) * pageScale = content width * pageScale.
            val columnDp = sheet.widthDp - sheet.toDp(a4.marginStartDp) - sheet.toDp(a4.marginEndDp)
            assertEquals("viewport $viewport", a4.contentWidthDp * sheet.pageScale, columnDp, 0.001f)
            assertEquals("aspect ratio", a4.heightDp / a4.widthDp, sheet.heightDp / sheet.widthDp, 0.0001f)
        }
    }

    @Test
    fun zoomScalesTheSheetTextAndTapsTogether() {
        val base = PageTransform.sheetFor(a4, 360f, 1f)
        val doubled = PageTransform.sheetFor(a4, 360f, 2f)
        assertEquals(base.pageScale * 2f, doubled.pageScale, 0.00001f)
        assertEquals(base.fontDp(12f) * 2f, doubled.fontDp(12f), 0.0001f)
        // 12 pt is 16 units; at scale 1 it is 16 dp on screen.
        assertEquals(16f, PageTransform.sheetFor(a4, 0f, 1f).fontDp(12f), 0.0001f)
        // Taps invert the same factor.
        assertEquals(250f, doubled.toUnits(doubled.toDp(250f)), 0.001f)
    }

    @Test
    fun fallbackBoxKeepsLetterRatioAndNoViewportMeansOneUnitPerDp() {
        val sheet = PageTransform.sheetFor(0f, 0f, 0f, 1f)
        assertEquals(PageStyleSpec.FALLBACK.widthDp, sheet.widthDp, 0.001f)
        assertEquals(PageStyleSpec.FALLBACK.heightDp, sheet.heightDp, 0.001f)
        assertEquals(1f, sheet.pageScale, 0.00001f)
        val fitted = PageTransform.sheetFor(PageStyleSpec.FALLBACK, 360f, 1f)
        assertEquals(360f, fitted.widthDp, 0.001f)
        assertEquals(360f * 11f / 8.5f, fitted.heightDp, 0.01f)
    }

    @Test
    fun textScaleNeutralisesTheSystemFontScale() {
        val sheet = PageTransform.sheetFor(a4, 360f, 1f)
        val atOne = PageTransform.textScale(sheet, 1f)
        val atLarge = PageTransform.textScale(sheet, 1.3f)
        // sp = pt * textScale; on screen sp * fontScale dp; both settings land on fontDp.
        assertEquals(sheet.fontDp(12f), 12f * atOne * 1f, 0.0001f)
        assertEquals(sheet.fontDp(12f), 12f * atLarge * 1.3f, 0.0001f)
        assertEquals(atOne, PageTransform.textScale(sheet, 0f), 0.00001f)
    }
}
