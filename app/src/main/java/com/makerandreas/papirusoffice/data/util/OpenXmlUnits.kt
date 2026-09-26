package com.makerandreas.papirusoffice.data.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.makerandreas.papirusoffice.data.LayoutUnits

object OpenXmlUnits {

    /**
     * OOXML EMU (914 400 per inch) to layout units at 96 per inch, one unit
     * per 9525 EMU, through [LayoutUnits]. The result is typed as [Dp] because
     * the image composables consume it directly; it is the same 96/inch space
     * the page box uses.
     */
    fun emuToDp(emuValue: Long): Dp {
        if (emuValue <= 0) return Dp.Unspecified
        return LayoutUnits.emuToUnits(emuValue).dp
    }
}
