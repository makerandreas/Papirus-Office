package com.makerandreas.papirusoffice.data.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object OpenXmlUnits {
    
    /**
     * Konversi ukuran EMU Microsoft OpenXML ke Dp Android
     * 1 Pixel = 9.525 EMUs
     */
    fun emuToDp(emuValue: Long): Dp {
        if (emuValue <= 0) return Dp.Unspecified
        val pixels = emuValue / 9525f
        return pixels.dp
    }
}
