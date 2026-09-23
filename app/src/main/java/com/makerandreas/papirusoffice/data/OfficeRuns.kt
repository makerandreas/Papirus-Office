package com.makerandreas.papirusoffice.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Maps paragraph runs and resolved styles onto the styled text both render
 * modes consume. Font sizes come from the same [StyleResolver] the
 * [LayoutEngine] paginates with, so displayed text and page breaks never
 * disagree on size (the card-transform half of audit-003 D1 is a follow-up).
 */
object OfficeRuns {

    /** Paragraph style the engine paginates with; display mirrors it. */
    fun baseStyle(paragraph: OfficeParagraph, styles: DocumentStyles): ParagraphStyle =
        StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)

    /**
     * Styled view of [paragraph] at display [scale] (zoom). Run spans only
     * apply while they exactly cover the text; after an edit the text
     * changes before runs are resliced (owned by the merger update in PR C),
     * so a mismatched run list degrades to the plain styled paragraph.
     */
    fun toAnnotatedString(
        paragraph: OfficeParagraph,
        styles: DocumentStyles,
        scale: Float,
        defaultColor: Color
    ): androidx.compose.ui.text.AnnotatedString {
        val base = baseStyle(paragraph, styles)
        val runs = paragraph.runs
        val useRuns = runs.isNotEmpty() && runs.joinToString("") { it.text } == paragraph.text
        val text = paragraph.text

        return buildAnnotatedString {
            append(text)
            addStyle(spanFor(base, scale, defaultColor), 0, text.length)
            if (useRuns) {
                var offset = 0
                for (run in runs) {
                    if (run.text.isEmpty()) continue
                    val resolved = mergeRun(run, base, styles)
                    if (resolved != base) {
                        addStyle(spanFor(resolved, scale, defaultColor), offset, offset + run.text.length)
                    }
                    offset += run.text.length
                }
            }
            addStyle(
                androidx.compose.ui.text.AnnotatedString.ParagraphStyle(
                    textAlign = composeTextAlign(paragraph.alignment ?: base.alignment)
                ),
                0,
                text.length
            )
        }
    }

    /** Paragraph style a run displays with, layered over [base]. */
    fun mergeRun(run: OfficeTextRun, base: ParagraphStyle, styles: DocumentStyles): ParagraphStyle {
        // A character style is authoritative only on a map hit; ODF import
        // fills these in PR C, so absent entries inherit the paragraph base.
        // Boolean defaults are "absent", so only positive char-style flags
        // add to the run/paragraph flags.
        val charHit = styles.characterStyles[run.characterStyle ?: run.styleName]
        return ParagraphStyle(
            name = base.name,
            fontSizeSp = base.fontSizeSp,
            isBold = charHit?.isBold == true || run.isBold || base.isBold,
            isItalic = charHit?.isItalic == true || run.isItalic || base.isItalic,
            isUnderline = charHit?.isUnderline == true || run.isUnderline || base.isUnderline,
            colorHex = charHit?.colorHex ?: base.colorHex,
            alignment = base.alignment,
            fontFamily = charHit?.fontFamily ?: base.fontFamily
        )
    }

    fun spanFor(style: ParagraphStyle, scale: Float, defaultColor: Color): SpanStyle {
        val color = style.colorHex?.let(::parseColorHex) ?: defaultColor
        return SpanStyle(
            fontSize = (style.fontSizeSp * scale).sp,
            fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (style.isUnderline) TextDecoration.Underline else TextDecoration.None,
            color = color,
            fontFamily = fontFamilyFor(style.fontFamily)
        )
    }

    /** Name to Compose family map; unknown names fall back to the system default. */
    fun fontFamilyFor(name: String?): FontFamily = when (name?.lowercase(Locale.ROOT)) {
        "serif", "times new roman", "liberation serif", "caladea" -> FontFamily.Serif
        "sans-serif", "roboto", "arial", "helvetica", "liberation sans", "carlito" -> FontFamily.SansSerif
        "monospace", "courier", "courier new", "liberation mono" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    fun composeTextAlign(alignment: String?): TextAlign = when (alignment?.lowercase(Locale.ROOT)) {
        "center" -> TextAlign.Center
        "right" -> TextAlign.Right
        "justify" -> TextAlign.Justify
        else -> TextAlign.Left
    }

    private fun parseColorHex(hex: String): Color {
        val cleaned = hex.removePrefix("#")
        return try {
            Color(if (cleaned.length == 6) 0xFF000000 + cleaned.toLong(16) else cleaned.toLong(16))
        } catch (e: NumberFormatException) {
            Color.Unspecified
        }
    }
}
