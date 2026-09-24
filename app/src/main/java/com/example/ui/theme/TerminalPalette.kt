package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The diagnostic consoles (ODF package log, e-mail trace, clipboard trace) are
 * deliberately terminal-styled: fixed colours on their own near-black panel,
 * identical in light and dark theme, the way a log viewer reads on a desktop.
 * They are therefore *not* theme tokens — and they are written down here once
 * instead of as loose `Color.Gray` / `Color.LightGray` / `Color(0xFF181818)`
 * values in three files, so the "chrome greys" rule can hold at zero without
 * repainting a console that was never broken (plan 3B item 3.10).
 *
 * Contrast against [PANEL], at the 11-12 sp monospace these are used at
 * (R-25 floor 4.5:1): [QUIET] ≈ 5.0:1, [LINE] ≈ 11.9:1. The hues are the ones
 * the consoles already used.
 */
object TerminalPalette {
    /** Console background, unchanged from the value the ODF sheet used. */
    val PANEL = Color(0xFF181818)

    /** Log lines and the console's control icons. */
    val LINE = Color(0xFFD3D3D3)

    /** The "nothing logged yet" line, dimmer than real output. */
    val QUIET = Color(0xFF888888)
}
