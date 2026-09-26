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
 * disagree on size.
 */
object OfficeRuns {

    /** Paragraph style the engine paginates with; display mirrors it. */
    fun baseStyle(paragraph: OfficeParagraph, styles: DocumentStyles): ParagraphStyle =
        StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)

    /**
     * Styled view of [paragraph] at display [scale] (zoom). Run spans only
     * apply while they exactly cover the text; after an edit the text
     * changes before runs are resliced, so a mismatched run list degrades
     * to the plain styled paragraph.
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
                androidx.compose.ui.text.ParagraphStyle(
                    textAlign = composeTextAlign(paragraph.alignment ?: base.alignment)
                ),
                0,
                text.length
            )
        }
    }

    /** Paragraph style a run displays with, layered over [base]. */
    fun mergeRun(run: OfficeTextRun, base: ParagraphStyle, styles: DocumentStyles): ParagraphStyle {
        // A character style is authoritative only on a map hit; absent
        // entries inherit the paragraph base. Boolean defaults are "absent",
        // so only positive char-style flags add to the run/paragraph flags.
        val charHit = styles.characterStyles[run.characterStyle ?: run.styleName]
        // copy() keeps the paragraph-level metric fields (spacing, indents,
        // keep flags) with the run; a run only re-decides character facts.
        return base.copy(
            fontSizeSp = charHit?.fontSizeSp ?: base.fontSizeSp,
            isBold = charHit?.isBold == true || run.isBold || base.isBold,
            isItalic = charHit?.isItalic == true || run.isItalic || base.isItalic,
            isUnderline = charHit?.isUnderline == true || run.isUnderline || base.isUnderline,
            colorHex = charHit?.colorHex ?: base.colorHex,
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

    /**
     * Display family for a document font name, decided by [FontRegistry] so
     * the paginator's [TextMetrics] and the renderer resolve one name to one
     * face. Unknown names fall back to the platform default.
     */
    fun fontFamilyFor(name: String?): FontFamily = FontRegistry.composeFamilyFor(name)

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
