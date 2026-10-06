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

    /**
     * Paragraph style a run displays with, layered over [base]. Direct run
     * formatting is tri-state and wins where it states a value, so a run can
     * subtract as well as add: `w:b w:val="0"` on a run inside a bold paragraph
     * is not bold (ECMA-376 Part 1 §17.7.3). Where the run states nothing, the
     * character style named by `w:rStyle` decides, and where that is absent or
     * silent, the paragraph style is the base. `copy()` keeps the
     * paragraph-level metric fields (spacing, indents, keep flags) with the run;
     * a run only re-decides character facts.
     */
    fun mergeRun(run: OfficeTextRun, base: ParagraphStyle, styles: DocumentStyles): ParagraphStyle {
        val charHit = styles.characterStyles[run.characterStyle ?: run.styleName]
        return base.copy(
            fontSizeSp = run.fontSizeSp ?: charHit?.fontSizeSp ?: base.fontSizeSp,
            isBold = run.isBold ?: (charHit?.isBold == true || base.isBold),
            isItalic = run.isItalic ?: (charHit?.isItalic == true || base.isItalic),
            isUnderline = run.isUnderline ?: (charHit?.isUnderline == true || base.isUnderline),
            colorHex = run.colorHex ?: charHit?.colorHex ?: base.colorHex,
            highlight = run.highlight ?: charHit?.highlight ?: base.highlight,
            isHidden = run.isHidden ?: (charHit?.isHidden == true || base.isHidden),
            fontFamily = run.fontFamily ?: charHit?.fontFamily ?: base.fontFamily
        )
    }

    fun spanFor(style: ParagraphStyle, scale: Float, defaultColor: Color): SpanStyle {
        // Hidden text stays in the string so caret and selection offsets keep
        // matching the model; Word paints nothing for it, so display does not
        // either. The reserved line box is the interim shape until Plan 8B
        // settles hidden-run text projection.
        val color = if (style.isHidden) Color.Transparent else style.colorHex?.let(::parseColorHex) ?: defaultColor
        return SpanStyle(
            fontSize = (style.fontSizeSp * scale).sp,
            fontWeight = if (style.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (style.isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (style.isUnderline) TextDecoration.Underline else TextDecoration.None,
            color = color,
            background = highlightColor(style.highlight) ?: Color.Unspecified,
            fontFamily = fontFamilyFor(style.fontFamily)
        )
    }

    /**
     * Background for an OOXML `w:highlight` colour name (ECMA-376 Part 1
     * §17.18.40 `ST_HighlightColor`). Names are the document's own values, so
     * every consumer of a style sees one mapping; unknown names fall through
     * to no background rather than a guessed colour.
     */
    fun highlightColor(name: String?): Color? = when (name?.trim()?.lowercase(Locale.ROOT)) {
        "black" -> Color(0xFF000000)
        "blue" -> Color(0xFF0000FF)
        "cyan" -> Color(0xFF00FFFF)
        "darkblue" -> Color(0xFF000080)
        "darkcyan" -> Color(0xFF008080)
        "darkgray", "darkgrey" -> Color(0xFF808080)
        "darkgreen" -> Color(0xFF008000)
        "darkmagenta" -> Color(0xFF800080)
        "darkred" -> Color(0xFF800000)
        "darkyellow" -> Color(0xFF808000)
        "green" -> Color(0xFF00FF00)
        "lightgray", "lightgrey" -> Color(0xFFC0C0C0)
        "magenta" -> Color(0xFFFF00FF)
        "red" -> Color(0xFFFF0000)
        "white" -> Color(0xFFFFFFFF)
        "yellow" -> Color(0xFFFFFF00)
        "none" -> null
        else -> null
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
