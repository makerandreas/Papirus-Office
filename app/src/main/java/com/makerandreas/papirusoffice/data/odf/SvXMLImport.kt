package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.CharacterStyle
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.LayoutUnits
import com.makerandreas.papirusoffice.data.OfficeTableColumnSpec
import com.makerandreas.papirusoffice.data.TableBorder
import com.makerandreas.papirusoffice.data.TableBorderLineStyle
import com.makerandreas.papirusoffice.data.TableCellBoxStyle
import com.makerandreas.papirusoffice.data.TableColumnWidthKind
import com.makerandreas.papirusoffice.data.TableColumnWidthSpec
import com.makerandreas.papirusoffice.data.TableDiagnostic
import com.makerandreas.papirusoffice.data.TableDiagnosticCode
import com.makerandreas.papirusoffice.data.TableInsets
import com.makerandreas.papirusoffice.data.TableRowStyle
import com.makerandreas.papirusoffice.data.TableStyleSpec
import com.makerandreas.papirusoffice.data.TableVerticalAlignment
import com.makerandreas.papirusoffice.data.NumberingCounterState
import com.makerandreas.papirusoffice.data.NumberingFormatter
import com.makerandreas.papirusoffice.data.NumberingLevelSpec
import com.makerandreas.papirusoffice.data.NumberingSpec
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeFontFace
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.ParagraphTabStop
import com.makerandreas.papirusoffice.data.TabAlignment
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.util.OdfLength
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

// Pull-parser implementations expose attribute names either with their
// namespace prefix ("style:name") or without it ("name") depending on the
// runtime, so attribute lookups must match on the local name alone. Runs on
// XMLPullParser output, not the JDK-11 DOM stack, so getLocalName is avoided.
private fun attrIndex(parser: XmlPullParser): Map<String, String> {
    val attrs = HashMap<String, String>(parser.attributeCount * 2)
    for (i in 0 until parser.attributeCount) {
        val local = parser.getAttributeName(i).substringAfterLast(':')
        val value = parser.getAttributeValue(i)
        if (!attrs.containsKey(local)) {
            attrs[local] = value
        }
    }
    return attrs
}

/**
 * Modern ODF SAX Import Filter class in Papirus Engine,
 * mirroring SvXMLImport in LibreOffice xmloff module ("xo" library).
 *
 * Maintains a stack of element contexts (SvXMLImportContext) for robust context-driven
 * document parsing and token mapping.
 */
data class OdfStyleInfo(
    val name: String,
    val family: String = "paragraph",
    val parentName: String? = null,
    val displayName: String? = null,
    val outlineLevel: Int? = null,
    val fontFamily: String? = null,
    val fontSizePt: Float? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null,
    val colorHex: String? = null,
    val alignment: String? = null,
    /** `style:master-page-name` on a paragraph (automatic) style: the page style this paragraph starts. */
    val masterPageName: String? = null,
    /** `style:list-style-name` on a paragraph style (null = inherit, "" = suppress numbering). */
    val listStyleName: String? = null,
    val spaceBeforeUnits: Float? = null,
    val spaceAfterUnits: Float? = null,
    val lineHeightFactor: Float? = null,
    val lineHeightExactUnits: Float? = null,
    val lineHeightUsesFontSize: Boolean? = null,
    val indentStartUnits: Float? = null,
    val indentEndUnits: Float? = null,
    val firstLineIndentUnits: Float? = null,
    val keepWithNext: Boolean? = null,
    val pageBreakBefore: Boolean? = null,
    val pageBreakAfter: Boolean? = null,
    val keepTogether: Boolean? = null,
    val orphans: Int? = null,
    val widows: Int? = null,
    val tabStops: List<ParagraphTabStop>? = null,
    val defaultTabIntervalUnits: Float? = null,
    val tableWidth: TableColumnWidthSpec? = null,
    val columnWidth: TableColumnWidthSpec? = null,
    val defaultCellStyleName: String? = null,
    val tableMinimumHeightUnits: Float? = null,
    val tableExactHeightUnits: Float? = null,
    val tableKeepTogether: Boolean? = null,
    val tablePadding: TableInsets? = null,
    val tableBorderTop: TableBorder? = null,
    val tableBorderEnd: TableBorder? = null,
    val tableBorderBottom: TableBorder? = null,
    val tableBorderStart: TableBorder? = null,
    val tableBackgroundColorHex: String? = null,
    val tableVerticalAlignment: TableVerticalAlignment? = null
)

/** Bold/italic/underline resolved from a character style, never from the style name. */
data class OdfSpanFormat(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false
)

private class StyleDraft(
    val name: String,
    val family: String,
    val parentName: String?,
    val displayName: String?,
    val outlineLevel: Int?,
    val masterPageName: String? = null,
    val listStyleName: String? = null,
    var fontFamily: String? = null,
    var fontSizePt: Float? = null,
    var isBold: Boolean? = null,
    var isItalic: Boolean? = null,
    var isUnderline: Boolean? = null,
    var colorHex: String? = null,
    var alignment: String? = null,
    var spaceBeforeUnits: Float? = null,
    var spaceAfterUnits: Float? = null,
    var lineHeightFactor: Float? = null,
    var lineHeightExactUnits: Float? = null,
    var lineHeightUsesFontSize: Boolean? = null,
    var indentStartUnits: Float? = null,
    var indentEndUnits: Float? = null,
    var firstLineIndentUnits: Float? = null,
    var keepWithNext: Boolean? = null,
    var pageBreakBefore: Boolean? = null,
    var pageBreakAfter: Boolean? = null,
    var keepTogether: Boolean? = null,
    var orphans: Int? = null,
    var widows: Int? = null,
    var tabStops: List<ParagraphTabStop>? = null,
    var defaultTabIntervalUnits: Float? = null,
    var tableWidth: TableColumnWidthSpec? = null,
    var columnWidth: TableColumnWidthSpec? = null,
    var defaultCellStyleName: String? = null,
    var tableMinimumHeightUnits: Float? = null,
    var tableExactHeightUnits: Float? = null,
    var tableKeepTogether: Boolean? = null,
    var tablePadding: TableInsets? = null,
    var tableBorderTop: TableBorder? = null,
    var tableBorderEnd: TableBorder? = null,
    var tableBorderBottom: TableBorder? = null,
    var tableBorderStart: TableBorder? = null,
    var tableBackgroundColorHex: String? = null,
    var tableVerticalAlignment: TableVerticalAlignment? = null
) {
    fun toInfo(): OdfStyleInfo = OdfStyleInfo(
        name = name,
        family = family,
        parentName = parentName,
        displayName = displayName,
        outlineLevel = outlineLevel,
        fontFamily = fontFamily,
        fontSizePt = fontSizePt,
        isBold = isBold,
        isItalic = isItalic,
        isUnderline = isUnderline,
        colorHex = colorHex,
        alignment = alignment,
        masterPageName = masterPageName,
        listStyleName = listStyleName,
        spaceBeforeUnits = spaceBeforeUnits,
        spaceAfterUnits = spaceAfterUnits,
        lineHeightFactor = lineHeightFactor,
        lineHeightExactUnits = lineHeightExactUnits,
        lineHeightUsesFontSize = lineHeightUsesFontSize,
        indentStartUnits = indentStartUnits,
        indentEndUnits = indentEndUnits,
        firstLineIndentUnits = firstLineIndentUnits,
        keepWithNext = keepWithNext,
        pageBreakBefore = pageBreakBefore,
        pageBreakAfter = pageBreakAfter,
        keepTogether = keepTogether, orphans = orphans, widows = widows,
        tabStops = tabStops, defaultTabIntervalUnits = defaultTabIntervalUnits,
        tableWidth = tableWidth,
        columnWidth = columnWidth,
        defaultCellStyleName = defaultCellStyleName,
        tableMinimumHeightUnits = tableMinimumHeightUnits,
        tableExactHeightUnits = tableExactHeightUnits,
        tableKeepTogether = tableKeepTogether,
        tablePadding = tablePadding,
        tableBorderTop = tableBorderTop,
        tableBorderEnd = tableBorderEnd,
        tableBorderBottom = tableBorderBottom,
        tableBorderStart = tableBorderStart,
        tableBackgroundColorHex = tableBackgroundColorHex,
        tableVerticalAlignment = tableVerticalAlignment
    )
}

private class NumberingLevelDraft(
    val level: Int,
    val isBullet: Boolean,
    val numFormat: String,
    val numPrefix: String,
    val numSuffix: String,
    val displayLevels: Int,
    val startValue: Int,
    val bulletChar: String,
    val textStyleName: String?
) {
    var fontFamily: String? = null
    var fontSizeSp: Float? = null
    var isBold: Boolean? = null
    var isItalic: Boolean? = null
    var colorHex: String? = null
    var indentStartUnits: Float? = null
    var firstLineIndentUnits: Float? = null
    var tabStopPositionUnits: Float? = null
    var labelFollowedBy: String = "listtab"

    fun toSpec(): NumberingLevelSpec = NumberingLevelSpec(
        level = level,
        isBullet = isBullet,
        numFormat = numFormat,
        numPrefix = numPrefix,
        numSuffix = numSuffix,
        displayLevels = displayLevels,
        startValue = startValue,
        bulletChar = bulletChar,
        textStyleName = textStyleName,
        fontFamily = fontFamily,
        fontSizeSp = fontSizeSp,
        isBold = isBold,
        isItalic = isItalic,
        colorHex = colorHex,
        indentStartUnits = indentStartUnits,
        firstLineIndentUnits = firstLineIndentUnits,
        tabStopPositionUnits = tabStopPositionUnits,
        labelFollowedBy = labelFollowedBy
    )
}

private fun applyLevelTextProperties(
    draft: NumberingLevelDraft,
    attrs: Map<String, String>,
    fontFaces: Map<String, OfficeFontFace> = emptyMap()
) {
    val fontWeight = attrs["font-weight"] ?: attrs["font-weight-asian"] ?: attrs["font-weight-complex"]
    parseFontWeightBold(fontWeight)?.let { draft.isBold = it }

    val fontStyle = attrs["font-style"] ?: attrs["font-style-asian"] ?: attrs["font-style-complex"]
    if (!fontStyle.isNullOrBlank()) {
        val lowered = fontStyle.lowercase(Locale.ROOT)
        draft.isItalic = lowered == "italic" || lowered == "oblique"
    }

    val sizeAttr = attrs["font-size"] ?: attrs["font-size-asian"]
    parseOdfFontSizePt(sizeAttr)?.let { draft.fontSizeSp = it }

    attrs["color"]?.takeIf { it.isNotBlank() }?.let { draft.colorHex = it }

    val family = attrs["font-name"] ?: attrs["font-family"]
    if (!family.isNullOrBlank()) {
        draft.fontFamily = resolveFontFamily(family, fontFaces)
    }
}

/**
 * The single resolution point for an ODF family value: `style:font-name`
 * (ODF 1.4 Part 3 20.277) is looked up in the document's declaration table,
 * and any other spelling falls through to the raw, quote-trimmed name so the
 * registry still classifies it.
 *
 * Resolution happens while the style is read, which is sound because both
 * `office:document-content` and `office:document-styles` place
 * `office:font-face-decls` before their style elements; that order was
 * checked in all twelve fixture parts on 2026-10-04 (audit-017 section 4.1).
 * `styles.xml` is also parsed before `content.xml`
 * (`SvXMLImport.parseOdfXml`), so an alias declared in either part is in the
 * table before any style that uses it is read.
 */
private fun resolveFontFamily(raw: String, fontFaces: Map<String, OfficeFontFace>): String =
    FontFaceResolver.familyFor(raw, fontFaces) ?: raw.trim().trim('\'', '"')

/** Resolved label and font properties for a list item or numbered heading. */
data class OdfFormattedListLabel(
    val bullet: String,
    val isOrdered: Boolean,
    val labelFontSizeSp: Float? = null,
    val labelFontFamily: String? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val colorHex: String? = null,
    val textStyleName: String? = null
)

/** ODF `fo:font-size` like `12pt` stays in points; [LayoutUnits.parsePoints] owns the arithmetic. */
internal fun parseOdfFontSizePt(raw: String?): Float? = LayoutUnits.parsePoints(raw)

private fun parseAlignment(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return when (raw.trim().lowercase(Locale.ROOT)) {
        "center" -> "Center"
        "right", "end" -> "Right"
        "justify" -> "Justify"
        "left", "start" -> "Left"
        else -> null
    }
}

private fun parseFontWeightBold(raw: String?): Boolean? {
    if (raw.isNullOrBlank()) return null
    val text = raw.trim().lowercase(Locale.ROOT)
    if (text == "bold" || text == "bolder") return true
    if (text == "normal" || text == "lighter") return false
    val numeric = text.toIntOrNull() ?: return null
    return numeric >= 700
}

private fun parsePositiveFloat(raw: String?): Float? = raw?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }

private fun parseRelativeColumnWidth(raw: String?): TableColumnWidthSpec? {
    val text = raw?.trim()?.removeSuffix("*") ?: return null
    return parsePositiveFloat(text)?.let { TableColumnWidthSpec(TableColumnWidthKind.RELATIVE, it) }
}

private fun parseRelativeTableWidth(raw: String?): TableColumnWidthSpec? {
    val text = raw?.trim()?.removeSuffix("%") ?: return null
    return parsePositiveFloat(text)?.let { TableColumnWidthSpec(TableColumnWidthKind.RELATIVE, it) }
}

private fun parseAbsoluteWidth(raw: String?): TableColumnWidthSpec? {
    if (raw.isNullOrBlank()) return null
    val units = OdfLength.toLayoutUnits(raw, fallback = -1f)
    return units.takeIf { it >= 0f && it.isFinite() }
        ?.let { TableColumnWidthSpec(TableColumnWidthKind.ABSOLUTE, it) }
}

private fun parseColumnWidth(absolute: String?, relative: String?): TableColumnWidthSpec? =
    parseAbsoluteWidth(absolute) ?: parseRelativeColumnWidth(relative)

private fun parseTableWidth(absolute: String?, relative: String?): TableColumnWidthSpec? =
    parseRelativeTableWidth(relative) ?: parseAbsoluteWidth(absolute)

private fun parseBorder(raw: String?): TableBorder? {
    if (raw.isNullOrBlank()) return null
    val tokens = raw.trim().split(Regex("\\s+"))
    if (tokens.any { it.equals("none", ignoreCase = true) }) return TableBorder()
    val width = tokens.firstNotNullOfOrNull { token ->
        val value = OdfLength.toLayoutUnits(token, fallback = -1f)
        value.takeIf { it >= 0f && it.isFinite() }
    } ?: 0f
    val style = tokens.firstNotNullOfOrNull { token ->
        when (token.lowercase(Locale.ROOT)) {
            "solid" -> TableBorderLineStyle.SOLID
            "dotted" -> TableBorderLineStyle.DOTTED
            "dashed" -> TableBorderLineStyle.DASHED
            "double" -> TableBorderLineStyle.DOUBLE
            "none" -> TableBorderLineStyle.NONE
            else -> null
        }
    } ?: if (width > 0f) TableBorderLineStyle.SOLID else TableBorderLineStyle.OTHER
    val color = tokens.firstOrNull { it.startsWith("#") || it.equals("transparent", ignoreCase = true) }
    return TableBorder(widthUnits = width, style = style, colorHex = color, sourceStyle = raw)
}

private fun parseVerticalAlignment(raw: String?): TableVerticalAlignment? = when (raw?.trim()?.lowercase(Locale.ROOT)) {
    "top" -> TableVerticalAlignment.TOP
    "middle", "center" -> TableVerticalAlignment.MIDDLE
    "bottom" -> TableVerticalAlignment.BOTTOM
    "automatic", "auto" -> TableVerticalAlignment.AUTOMATIC
    else -> null
}

private fun parseTablePadding(attrs: Map<String, String>, previous: TableInsets?): TableInsets? {
    val shorthand = attrs["padding"]?.let { OdfLength.toLayoutUnits(it, fallback = -1f) }
        ?.takeIf { it >= 0f && it.isFinite() }
    val base = previous ?: TableInsets()
    val top = attrs["padding-top"]?.let { OdfLength.toLayoutUnits(it, fallback = -1f) }
        ?.takeIf { it >= 0f && it.isFinite() } ?: shorthand ?: base.topUnits
    val end = attrs["padding-right"]?.let { OdfLength.toLayoutUnits(it, fallback = -1f) }
        ?.takeIf { it >= 0f && it.isFinite() } ?: shorthand ?: base.endUnits
    val bottom = attrs["padding-bottom"]?.let { OdfLength.toLayoutUnits(it, fallback = -1f) }
        ?.takeIf { it >= 0f && it.isFinite() } ?: shorthand ?: base.bottomUnits
    val start = attrs["padding-left"]?.let { OdfLength.toLayoutUnits(it, fallback = -1f) }
        ?.takeIf { it >= 0f && it.isFinite() } ?: shorthand ?: base.startUnits
    return if (shorthand != null || attrs.keys.any { it.startsWith("padding-") }) {
        TableInsets(topUnits = top, endUnits = end, bottomUnits = bottom, startUnits = start)
    } else {
        previous
    }
}

private fun applyTableProperties(draft: StyleDraft, family: String, attrs: Map<String, String>) {
    when (family.lowercase(Locale.ROOT)) {
        "table" -> {
            parseTableWidth(attrs["width"], attrs["rel-width"])?.let { draft.tableWidth = it }
        }
        "table-column" -> {
            parseColumnWidth(attrs["column-width"], attrs["rel-column-width"])?.let { draft.columnWidth = it }
        }
        "table-row" -> {
            (attrs["min-height"] ?: attrs["min-row-height"])?.let { raw ->
                parseAbsoluteWidth(raw)?.value?.let { draft.tableMinimumHeightUnits = it }
            }
            (attrs["row-height"] ?: attrs["height"])?.let { raw ->
                parseAbsoluteWidth(raw)?.value?.let { draft.tableExactHeightUnits = it }
            }
            when (attrs["keep-together"]?.lowercase(Locale.ROOT)) {
                "always", "true" -> draft.tableKeepTogether = true
                "auto", "false" -> draft.tableKeepTogether = false
            }
        }
        "table-cell" -> {
            draft.tablePadding = parseTablePadding(attrs, draft.tablePadding)
            attrs["border"]?.let { parseBorder(it)?.let { border ->
                draft.tableBorderTop = border
                draft.tableBorderEnd = border
                draft.tableBorderBottom = border
                draft.tableBorderStart = border
            } }
            attrs["border-top"]?.let { parseBorder(it)?.let { draft.tableBorderTop = it } }
            attrs["border-right"]?.let { parseBorder(it)?.let { draft.tableBorderEnd = it } }
            attrs["border-bottom"]?.let { parseBorder(it)?.let { draft.tableBorderBottom = it } }
            attrs["border-left"]?.let { parseBorder(it)?.let { draft.tableBorderStart = it } }
            draft.tableBackgroundColorHex = attrs["background-color"] ?: draft.tableBackgroundColorHex
            parseVerticalAlignment(attrs["vertical-align"])?.let { draft.tableVerticalAlignment = it }
        }
    }
}

private fun applyTextProperties(
    draft: StyleDraft,
    attrs: Map<String, String>,
    fontFaces: Map<String, OfficeFontFace> = emptyMap()
) {
    val fontWeight = attrs["font-weight"] ?: attrs["font-weight-asian"] ?: attrs["font-weight-complex"]
    parseFontWeightBold(fontWeight)?.let { draft.isBold = it }

    val fontStyle = attrs["font-style"] ?: attrs["font-style-asian"] ?: attrs["font-style-complex"]
    if (!fontStyle.isNullOrBlank()) {
        val lowered = fontStyle.lowercase(Locale.ROOT)
        draft.isItalic = lowered == "italic" || lowered == "oblique"
    }

    val underline = attrs["text-underline-style"] ?: attrs["text-underline-type"]
    if (!underline.isNullOrBlank()) {
        draft.isUnderline = !underline.equals("none", ignoreCase = true)
    }

    val sizeAttr = attrs["font-size"] ?: attrs["font-size-asian"]
    parseOdfFontSizePt(sizeAttr)?.let { draft.fontSizePt = it }

    attrs["color"]?.takeIf { it.isNotBlank() }?.let { draft.colorHex = it }

    val family = attrs["font-name"] ?: attrs["font-family"]
    if (!family.isNullOrBlank()) {
        draft.fontFamily = resolveFontFamily(family, fontFaces)
    }
}

private fun applyParagraphProperties(draft: StyleDraft, attrs: Map<String, String>) {
    parseAlignment(attrs["text-align"])?.let { draft.alignment = it }
    attrs["margin-top"]?.let { draft.spaceBeforeUnits = LayoutUnits.parseLength(it) }
    attrs["margin-bottom"]?.let { draft.spaceAfterUnits = LayoutUnits.parseLength(it) }
    attrs["margin-left"]?.let { draft.indentStartUnits = LayoutUnits.parseLength(it) }
    attrs["margin-right"]?.let { draft.indentEndUnits = LayoutUnits.parseLength(it) }
    attrs["text-indent"]?.let { draft.firstLineIndentUnits = LayoutUnits.parseLength(it) }
    attrs["line-height"]?.let { raw ->
        val factor = LayoutUnits.parseLineHeightFactor(raw)
        val exact = LayoutUnits.parseLength(raw)
        when {
            raw.trim().equals("normal", ignoreCase = true) -> {
                draft.lineHeightFactor = 1f
                draft.lineHeightExactUnits = null
                draft.lineHeightUsesFontSize = false
            }
            factor != null -> {
                draft.lineHeightFactor = factor
                draft.lineHeightExactUnits = null
                draft.lineHeightUsesFontSize = true
            }
            exact > 0f -> {
                draft.lineHeightFactor = null
                draft.lineHeightExactUnits = exact
                draft.lineHeightUsesFontSize = false
            }
        }
    }
    attrs["orphans"]?.toIntOrNull()?.let { draft.orphans = it.coerceAtLeast(1) }
    attrs["widows"]?.toIntOrNull()?.let { draft.widows = it.coerceAtLeast(1) }
    attrs["tab-stop-distance"]?.let { LayoutUnits.parseLength(it).takeIf { n -> n > 0f }?.let { n -> draft.defaultTabIntervalUnits = n } }
    when (attrs["keep-together"]) {
        "always" -> draft.keepTogether = true
        "auto" -> draft.keepTogether = false
    }
    // Absence inherits; explicit auto/false resets an inherited declaration.
    when (attrs["keep-with-next"]) {
        "always", "true" -> draft.keepWithNext = true
        "auto", "false" -> draft.keepWithNext = false
    }
    when (attrs["break-before"]) {
        "page" -> draft.pageBreakBefore = true
        "auto" -> draft.pageBreakBefore = false
    }
    when (attrs["break-after"]) {
        "page" -> draft.pageBreakAfter = true
        "auto" -> draft.pageBreakAfter = false
    }
}

private fun overlayStyle(base: OdfStyleInfo, over: OdfStyleInfo): OdfStyleInfo = OdfStyleInfo(
    name = over.name.ifBlank { base.name },
    family = over.family.ifBlank { base.family },
    parentName = over.parentName ?: base.parentName,
    displayName = over.displayName ?: base.displayName,
    outlineLevel = over.outlineLevel ?: base.outlineLevel,
    fontFamily = over.fontFamily ?: base.fontFamily,
    fontSizePt = over.fontSizePt ?: base.fontSizePt,
    isBold = over.isBold ?: base.isBold,
    isItalic = over.isItalic ?: base.isItalic,
    isUnderline = over.isUnderline ?: base.isUnderline,
    colorHex = over.colorHex ?: base.colorHex,
    alignment = over.alignment ?: base.alignment,
    masterPageName = over.masterPageName ?: base.masterPageName,
    listStyleName = over.listStyleName ?: base.listStyleName,
    spaceBeforeUnits = over.spaceBeforeUnits ?: base.spaceBeforeUnits,
    spaceAfterUnits = over.spaceAfterUnits ?: base.spaceAfterUnits,
    lineHeightFactor = if (over.lineHeightExactUnits != null) null else over.lineHeightFactor ?: base.lineHeightFactor,
    lineHeightExactUnits = if (over.lineHeightFactor != null) null else over.lineHeightExactUnits ?: base.lineHeightExactUnits,
    lineHeightUsesFontSize = over.lineHeightUsesFontSize ?: base.lineHeightUsesFontSize,
    indentStartUnits = over.indentStartUnits ?: base.indentStartUnits,
    indentEndUnits = over.indentEndUnits ?: base.indentEndUnits,
    firstLineIndentUnits = over.firstLineIndentUnits ?: base.firstLineIndentUnits,
    keepWithNext = over.keepWithNext ?: base.keepWithNext,
    pageBreakBefore = over.pageBreakBefore ?: base.pageBreakBefore,
    pageBreakAfter = over.pageBreakAfter ?: base.pageBreakAfter,
    keepTogether = over.keepTogether ?: base.keepTogether,
    orphans = over.orphans ?: base.orphans,
    widows = over.widows ?: base.widows,
    tabStops = over.tabStops ?: base.tabStops,
    defaultTabIntervalUnits = over.defaultTabIntervalUnits ?: base.defaultTabIntervalUnits,
    tableWidth = over.tableWidth ?: base.tableWidth,
    columnWidth = over.columnWidth ?: base.columnWidth,
    defaultCellStyleName = over.defaultCellStyleName ?: base.defaultCellStyleName,
    tableMinimumHeightUnits = over.tableMinimumHeightUnits ?: base.tableMinimumHeightUnits,
    tableExactHeightUnits = over.tableExactHeightUnits ?: base.tableExactHeightUnits,
    tableKeepTogether = over.tableKeepTogether ?: base.tableKeepTogether,
    tablePadding = over.tablePadding ?: base.tablePadding,
    tableBorderTop = over.tableBorderTop ?: base.tableBorderTop,
    tableBorderEnd = over.tableBorderEnd ?: base.tableBorderEnd,
    tableBorderBottom = over.tableBorderBottom ?: base.tableBorderBottom,
    tableBorderStart = over.tableBorderStart ?: base.tableBorderStart,
    tableBackgroundColorHex = over.tableBackgroundColorHex ?: base.tableBackgroundColorHex,
    tableVerticalAlignment = over.tableVerticalAlignment ?: base.tableVerticalAlignment
)

private val TABLE_STYLE_FAMILIES = setOf("table", "table-column", "table-row", "table-cell")

private fun tableStyleKey(family: String, name: String): String =
    "${family.lowercase(Locale.ROOT)}:${name.lowercase(Locale.ROOT)}"

class SvXMLImport(
    val extractedImages: Map<String, File> = emptyMap(),
    private val diagnostics: OdfImportDiagnostics = SilentOdfImportDiagnostics
) {
    private val contextStack = ArrayDeque<SvXMLImportContext>()
    private val parsedElements = mutableListOf<OfficeDocumentElement>()
    private val styleMap = mutableMapOf<String, OdfStyleInfo>()
    private val tableStyleMap = LinkedHashMap<String, OdfStyleInfo>()
    private val pageLayouts = LinkedHashMap<String, PageStyleSpec>()
    private val masterPages = LinkedHashMap<String, String>()
    /**
     * `office:font-face-decls` declarations from both XML parts, keyed by
     * `style:name` (ODF 1.4 Part 3 3.14, 19.502.3). The first declaration of
     * an alias wins, which is what the fixtures need: every `.odt` declares
     * the same set in `styles.xml` and `content.xml`, and `styles.xml` is
     * parsed first. Plan 7E resolves `style:font-name` through this table.
     */
    private val fontFaces = LinkedHashMap<String, OfficeFontFace>()
    private var pageSpecFromDefaultStyle: PageStyleSpec? = null
    private var standardPageLayoutName: String? = null
    private var firstMasterPageLayoutName: String? = null
    private var defaultParagraphStyle: OdfStyleInfo? = null
    private val listStyles = LinkedHashMap<String, NumberingSpec>()
    private var outlineStyle: NumberingSpec? = null
    private val outlineCounter = NumberingCounterState()
    private val listCounters = HashMap<String, NumberingCounterState>()
    private var anonymousListCounter = NumberingCounterState()
    private var lastListStyleName: String? = null
    private val documentBookmarks = LinkedHashSet<String>()
    private var currentFileName: String = ""
    private val semanticRanges = OdfSemanticRangeCollector(
        elementCount = { parsedElements.size },
        elementAt = { parsedElements[it] },
        parentStyleOf = { lookupStyle(it)?.parentName }
    )

    val elements: List<OfficeDocumentElement> get() = parsedElements

    /** True while body flow is inside an authored index; suppresses heading promotion (audit-015 F-1). */
    val isInsideIndex: Boolean get() = semanticRanges.isInsideIndex

    internal val ranges: OdfSemanticRangeCollector get() = semanticRanges

    /** Reports an element the importer recognises but cannot represent at its position. */
    fun reportUnsupported(tagName: String, attributes: Map<String, String>) {
        diagnostics.unsupportedTag(currentFileName, tagName, attributes)
    }

    fun addElement(element: OfficeDocumentElement) {
        parsedElements.add(element)
    }

    fun recordBookmark(name: String?) {
        val clean = name?.trim() ?: return
        if (clean.isNotEmpty()) {
            documentBookmarks.add(clean)
        }
    }

    fun parseOdfStyles(xml: String?) {
        if (xml.isNullOrBlank()) return
        var pendingPageLayoutName: String? = null
        var capturingDefaultPageStyle = false
        var pendingDraft: StyleDraft? = null
        var pendingIsDefault = false
        // style:header-style / style:footer-style inside the current page layout.
        var pendingHeaderFooter: String? = null
        var pendingHeaderHeight = 0f
        var pendingFooterHeight = 0f
        // text:list-style and text:outline-style level definitions.
        var pendingListStyleName: String? = null
        var pendingListDisplayName: String? = null
        var pendingListIsOutline = false
        val pendingListLevels = LinkedHashMap<Int, NumberingLevelSpec>()
        var pendingLevelDraft: NumberingLevelDraft? = null
        // office:font-face-decls is a sibling of office:styles; only the
        // declared faces inside it are read.
        var insideFontFaceDecls = false
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), "UTF-8")
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val rawTagName = parser.name ?: ""
                    val localName = rawTagName.substringAfterLast(':')
                    // Attribute maps are built only for the handful of style-bearing
                    // tags; content.xml would otherwise pay this cost per element.
                    val attrs = when (localName) {
                        "style", "page-layout", "default-style", "page-layout-properties",
                        "master-page", "text-properties", "paragraph-properties", "tab-stop", "font-face",
                        "table-properties", "table-column-properties", "table-row-properties", "table-cell-properties",
                        "header-footer-properties", "list-style", "outline-style",
                        "list-level-style-number", "list-level-style-bullet", "list-level-style-image",
                        "outline-level-style", "list-level-properties", "list-level-label-alignment" -> attrIndex(parser)
                        else -> emptyMap()
                    }
                    when (localName) {
                        // ODF 1.4 Part 3 3.14 / 19.502.3: the declaration table
                        // that style:font-name refers into (20.277). Declared
                        // faces are stored once; a blank name or blank family is
                        // unusable because OfficeFontFace rejects both.
                        "font-face-decls" -> insideFontFaceDecls = true
                        "font-face" -> if (insideFontFaceDecls) {
                            val faceName = attrs["name"]?.takeIf { it.isNotBlank() }
                            // svg:font-family may be a list; the declaration's
                            // family is its first entry (19.532).
                            val faceFamily = FontFaceResolver.firstFamily(attrs["font-family"])
                            if (faceName != null && faceFamily != null) {
                                fontFaces.putIfAbsent(
                                    faceName,
                                    OfficeFontFace(
                                        name = faceName,
                                        family = faceFamily,
                                        genericFamily = attrs["font-family-generic"],
                                        pitch = attrs["font-pitch"],
                                        charset = attrs["font-charset"]
                                    )
                                )
                            }
                        }
                        "style" -> {
                            pendingDraft?.let { commitStyleDraft(it, isDefault = pendingIsDefault) }
                            val name = attrs["name"]
                            pendingIsDefault = false
                            pendingDraft = if (!name.isNullOrBlank()) {
                                StyleDraft(
                                    name = name,
                                    family = attrs["family"] ?: "paragraph",
                                    parentName = attrs["parent-style-name"],
                                    displayName = attrs["display-name"],
                                    outlineLevel = attrs["default-outline-level"]?.toIntOrNull(),
                                    masterPageName = attrs["master-page-name"]?.takeIf { it.isNotBlank() },
                                    listStyleName = attrs["list-style-name"]
                                )
                            } else {
                                null
                            }
                        }
                        "list-style", "outline-style" -> {
                            pendingLevelDraft?.let { ld -> pendingListLevels[ld.level] = ld.toSpec() }
                            pendingLevelDraft = null
                            pendingListIsOutline = localName == "outline-style"
                            pendingListStyleName = attrs["name"]?.takeIf { it.isNotBlank() }
                                ?: if (pendingListIsOutline) "Outline" else null
                            pendingListDisplayName = attrs["display-name"]
                            pendingListLevels.clear()
                        }
                        "list-level-style-number", "list-level-style-bullet", "outline-level-style" -> {
                            pendingLevelDraft?.let { ld -> pendingListLevels[ld.level] = ld.toSpec() }
                            val lvl = attrs["level"]?.toIntOrNull() ?: 1
                            val isBullet = localName == "list-level-style-bullet"
                            val defaultFmt = if (localName == "outline-level-style") "" else "1"
                            val numFmt = attrs["num-format"] ?: defaultFmt
                            pendingLevelDraft = NumberingLevelDraft(
                                level = lvl,
                                isBullet = isBullet,
                                numFormat = numFmt,
                                numPrefix = attrs["num-prefix"] ?: "",
                                numSuffix = attrs["num-suffix"] ?: "",
                                displayLevels = attrs["display-levels"]?.toIntOrNull() ?: 1,
                                startValue = attrs["start-value"]?.toIntOrNull() ?: 1,
                                bulletChar = attrs["bullet-char"] ?: "\u2022",
                                textStyleName = attrs["style-name"]?.takeIf { it.isNotBlank() }
                            )
                        }
                        "list-level-properties" -> {
                            pendingLevelDraft?.let { ld ->
                                val margin = attrs["margin-left"] ?: attrs["space-before"]
                                margin?.let { ld.indentStartUnits = LayoutUnits.parseLength(it) }
                                attrs["text-indent"]?.let { ld.firstLineIndentUnits = LayoutUnits.parseLength(it) }
                            }
                        }
                        "list-level-label-alignment" -> {
                            pendingLevelDraft?.let { ld ->
                                attrs["margin-left"]?.let { ld.indentStartUnits = LayoutUnits.parseLength(it) }
                                attrs["text-indent"]?.let { ld.firstLineIndentUnits = LayoutUnits.parseLength(it) }
                                attrs["list-tab-stop-position"]?.let { ld.tabStopPositionUnits = LayoutUnits.parseLength(it) }
                                attrs["label-followed-by"]?.let { ld.labelFollowedBy = it }
                            }
                        }
                        "page-layout" -> {
                            pendingPageLayoutName = attrs["name"]?.takeIf { it.isNotBlank() }
                            pendingHeaderHeight = 0f
                            pendingFooterHeight = 0f
                        }
                        "header-style" -> pendingHeaderFooter = "header"
                        "footer-style" -> pendingHeaderFooter = "footer"
                        // Height the header/footer occupies between margin and body:
                        // fixed svg:height (ODF 1.4 Part 3 §20.407.2) wins over the
                        // content minimum fo:min-height (§20.212).
                        "header-footer-properties" -> {
                            val height = attrs["height"]?.let { OdfLength.toLayoutUnits(it) }
                                ?: attrs["min-height"]?.let { OdfLength.toLayoutUnits(it) }
                                ?: 0f
                            when (pendingHeaderFooter) {
                                "header" -> pendingHeaderHeight = height.coerceAtLeast(0f)
                                "footer" -> pendingFooterHeight = height.coerceAtLeast(0f)
                            }
                        }
                        "default-style" -> {
                            pendingDraft?.let { commitStyleDraft(it, isDefault = pendingIsDefault) }
                            val family = attrs["family"] ?: ""
                            capturingDefaultPageStyle = family.equals("page", ignoreCase = true)
                            pendingIsDefault = family.equals("paragraph", ignoreCase = true) ||
                                family.equals("text", ignoreCase = true)
                            pendingDraft = if (pendingIsDefault) {
                                StyleDraft(
                                    name = "",
                                    family = family,
                                    parentName = null,
                                    displayName = null,
                                    outlineLevel = null
                                )
                            } else {
                                null
                            }
                        }
                        "text-properties" -> {
                            if (pendingLevelDraft != null) {
                                applyLevelTextProperties(pendingLevelDraft!!, attrs, fontFaces)
                            } else {
                                pendingDraft?.let { applyTextProperties(it, attrs, fontFaces) }
                            }
                        }
                        "paragraph-properties" -> pendingDraft?.let { applyParagraphProperties(it, attrs) }
                        "table-properties" -> pendingDraft?.let { applyTableProperties(it, "table", attrs) }
                        "table-column-properties" -> pendingDraft?.let { applyTableProperties(it, "table-column", attrs) }
                        "table-row-properties" -> pendingDraft?.let { applyTableProperties(it, "table-row", attrs) }
                        "table-cell-properties" -> pendingDraft?.let { applyTableProperties(it, "table-cell", attrs) }
                        "tab-stops" -> pendingDraft?.let { it.tabStops = emptyList() }
                        "tab-stop" -> pendingDraft?.let { draft ->
                            val position = attrs["position"]?.let { LayoutUnits.parseLength(it) }
                            val align = when (attrs["type"]) {
                                "right" -> TabAlignment.RIGHT
                                "center" -> TabAlignment.CENTER
                                "char" -> TabAlignment.DECIMAL
                                else -> TabAlignment.LEFT
                            }
                            if (position != null && position >= 0f) draft.tabStops = draft.tabStops.orEmpty() + ParagraphTabStop(position, align)
                        }
                        "page-layout-properties" -> {
                            val spec = buildPageLayoutSpec(attrs)
                            val pendingName = pendingPageLayoutName
                            when {
                                spec != null && pendingName != null -> pageLayouts[pendingName] = spec.copy(name = pendingName)
                                spec != null && capturingDefaultPageStyle -> pageSpecFromDefaultStyle = spec
                            }
                        }
                        "master-page" -> {
                            val layoutName = attrs["page-layout-name"]?.takeIf { it.isNotBlank() }
                            if (layoutName != null) {
                                attrs["name"]?.takeIf { it.isNotBlank() }?.let { masterPages.putIfAbsent(it, layoutName) }
                                if (firstMasterPageLayoutName == null) firstMasterPageLayoutName = layoutName
                                if (attrs["name"].equals("Standard", ignoreCase = true)) {
                                    standardPageLayoutName = layoutName
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    when ((parser.name ?: "").substringAfterLast(':')) {
                        "font-face-decls" -> insideFontFaceDecls = false
                        "header-style", "footer-style" -> pendingHeaderFooter = null
                        "list-level-style-number", "list-level-style-bullet", "outline-level-style" -> {
                            pendingLevelDraft?.let { ld -> pendingListLevels[ld.level] = ld.toSpec() }
                            pendingLevelDraft = null
                        }
                        "list-style", "outline-style" -> {
                            pendingLevelDraft?.let { ld -> pendingListLevels[ld.level] = ld.toSpec() }
                            pendingLevelDraft = null
                            val name = pendingListStyleName
                            if (!name.isNullOrBlank() && pendingListLevels.isNotEmpty()) {
                                val spec = NumberingSpec(
                                    name = name,
                                    displayName = pendingListDisplayName,
                                    isOutline = pendingListIsOutline,
                                    levels = pendingListLevels.toMap()
                                )
                                if (pendingListIsOutline) {
                                    outlineStyle = spec
                                } else {
                                    listStyles[name] = spec
                                }
                            }
                            pendingListStyleName = null
                            pendingListDisplayName = null
                            pendingListIsOutline = false
                            pendingListLevels.clear()
                        }
                        "page-layout" -> {
                            val layoutName = pendingPageLayoutName
                            if (layoutName != null && (pendingHeaderHeight > 0f || pendingFooterHeight > 0f)) {
                                pageLayouts[layoutName]?.let { spec ->
                                    pageLayouts[layoutName] = spec.copy(
                                        headerHeightDp = pendingHeaderHeight,
                                        footerHeightDp = pendingFooterHeight
                                    )
                                }
                            }
                            pendingPageLayoutName = null
                            pendingHeaderHeight = 0f
                            pendingFooterHeight = 0f
                        }
                        "style" -> {
                            pendingDraft?.let { commitStyleDraft(it, isDefault = false) }
                            pendingDraft = null
                            pendingIsDefault = false
                        }
                        "default-style" -> {
                            pendingDraft?.let { commitStyleDraft(it, isDefault = pendingIsDefault) }
                            pendingDraft = null
                            pendingIsDefault = false
                            capturingDefaultPageStyle = false
                        }
                    }
                }
                eventType = parser.next()
            }
            pendingDraft?.let { commitStyleDraft(it, isDefault = pendingIsDefault) }
        } catch (e: Exception) {
            // graceful fallback
        }
    }

    private fun commitStyleDraft(draft: StyleDraft, isDefault: Boolean) {
        val info = draft.toInfo()
        if (isDefault) {
            if (info.family.equals("paragraph", ignoreCase = true)) {
                defaultParagraphStyle = info
            }
            return
        }
        if (info.name.isBlank()) return
        styleMap[info.name] = info
        styleMap[info.name.lowercase(Locale.ROOT)] = info
        if (info.family.lowercase(Locale.ROOT) in TABLE_STYLE_FAMILIES) {
            tableStyleMap[tableStyleKey(info.family, info.name)] = info
            tableStyleMap[tableStyleKey(info.family, info.name.lowercase(Locale.ROOT))] = info
        }
    }

    private fun lookupStyle(name: String): OdfStyleInfo? {
        return styleMap[name] ?: styleMap[name.lowercase(Locale.ROOT)]
    }

    private fun lookupTableStyle(family: String, name: String): OdfStyleInfo? {
        return tableStyleMap[tableStyleKey(family, name)]
            ?: tableStyleMap[tableStyleKey(family, name.lowercase(Locale.ROOT))]
    }

    /**
     * Overlay default-style (paragraph family only) then the parent chain,
     * child last. Character styles skip default-style so a missing size stays
     * null and [OfficeRuns.mergeRun] can inherit the paragraph size (F-3).
     */
    private fun cascadeStyle(name: String, family: String): OdfStyleInfo? {
        val chain = ArrayList<OdfStyleInfo>(4)
        val seen = HashSet<String>(8)
        var curr: String? = name
        var depth = 0
        while (curr != null && depth < 16 && seen.add(curr.lowercase(Locale.ROOT))) {
            val info = lookupStyle(curr) ?: break
            chain.add(info)
            curr = info.parentName
            depth++
        }
        if (chain.isEmpty()) return null
        val useDefault = family.equals("paragraph", ignoreCase = true)
        var acc = if (useDefault) defaultParagraphStyle ?: OdfStyleInfo(name = "", family = family)
        else OdfStyleInfo(name = "", family = family)
        for (i in chain.lastIndex downTo 0) {
            acc = overlayStyle(acc, chain[i])
        }
        return acc.copy(name = chain.first().name, family = chain.first().family, parentName = chain.first().parentName)
    }

    private fun cascadeTableStyle(name: String, family: String): OdfStyleInfo? {
        val chain = ArrayList<OdfStyleInfo>(4)
        val seen = HashSet<String>(8)
        var curr: String? = name
        var depth = 0
        while (curr != null && depth < 16 && seen.add(curr.lowercase(Locale.ROOT))) {
            val info = lookupTableStyle(family, curr) ?: break
            chain.add(info)
            curr = info.parentName
            depth++
        }
        if (chain.isEmpty()) return null
        var acc = OdfStyleInfo(name = "", family = family)
        for (i in chain.lastIndex downTo 0) {
            acc = overlayStyle(acc, chain[i])
        }
        return acc.copy(name = chain.first().name, family = family, parentName = chain.first().parentName)
    }

    private fun OdfStyleInfo.toTableStyleSpec(): TableStyleSpec = TableStyleSpec(
        name = name,
        family = family,
        parentStyleName = parentName,
        tableWidth = tableWidth,
        columnWidth = columnWidth,
        defaultCellStyleName = defaultCellStyleName,
        minimumHeightUnits = tableMinimumHeightUnits,
        exactHeightUnits = tableExactHeightUnits,
        keepTogether = tableKeepTogether,
        padding = tablePadding,
        borderTop = tableBorderTop,
        borderEnd = tableBorderEnd,
        borderBottom = tableBorderBottom,
        borderStart = tableBorderStart,
        backgroundColorHex = tableBackgroundColorHex,
        verticalAlignment = tableVerticalAlignment
    )

    fun resolveTableStyle(styleName: String?, family: String): TableStyleSpec? {
        if (styleName.isNullOrBlank()) return null
        return cascadeTableStyle(styleName, family)?.toTableStyleSpec()
    }

    fun resolveTableWidth(styleName: String?): TableColumnWidthSpec {
        return resolveTableStyle(styleName, "table")?.tableWidth ?: TableColumnWidthSpec()
    }

    fun resolveTableColumnWidth(styleName: String?): TableColumnWidthSpec {
        return resolveTableStyle(styleName, "table-column")?.columnWidth ?: TableColumnWidthSpec()
    }

    fun resolveTableRowStyle(styleName: String?): TableRowStyle {
        val style = resolveTableStyle(styleName, "table-row")
        return TableRowStyle(
            minimumHeightUnits = style?.minimumHeightUnits,
            exactHeightUnits = style?.exactHeightUnits,
            keepTogether = style?.keepTogether == true
        )
    }

    fun resolveTableCellBoxStyle(styleName: String?): TableCellBoxStyle {
        val style = resolveTableStyle(styleName, "table-cell")
        return TableCellBoxStyle(
            padding = style?.padding ?: TableInsets(),
            borderTop = style?.borderTop ?: TableBorder(),
            borderEnd = style?.borderEnd ?: TableBorder(),
            borderBottom = style?.borderBottom ?: TableBorder(),
            borderStart = style?.borderStart ?: TableBorder(),
            backgroundColorHex = style?.backgroundColorHex,
            verticalAlignment = style?.verticalAlignment ?: TableVerticalAlignment.AUTOMATIC
        )
    }

    fun resolveTableDefaultCellStyle(styleName: String?): String? =
        resolveTableStyle(styleName, "table-column")?.defaultCellStyleName

    fun resolveAllTableStyles(): List<TableStyleSpec> = tableStyleMap.values
        .distinctBy { tableStyleKey(it.family, it.name) }
        .map { it.toTableStyleSpec() }

    fun resolveSpanFormatting(styleName: String?): OdfSpanFormat {
        if (styleName.isNullOrBlank()) return OdfSpanFormat()
        val cascaded = cascadeStyle(styleName, "text") ?: return OdfSpanFormat()
        return OdfSpanFormat(
            isBold = cascaded.isBold == true,
            isItalic = cascaded.isItalic == true,
            isUnderline = cascaded.isUnderline == true
        )
    }

    fun lookupListStyle(name: String?): NumberingSpec? {
        if (name.isNullOrBlank()) return null
        return listStyles[name]
            ?: listStyles.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }

    fun resolveParagraphListStyleName(styleName: String?): String? {
        if (styleName.isNullOrBlank()) return null
        return cascadeStyle(styleName, "paragraph")?.listStyleName
    }

    fun onListStarted(styleName: String?, listLevel: Int, continueNumbering: Boolean): String? {
        val explicitName = styleName?.takeIf { it.isNotBlank() }
        if (listLevel <= 1) {
            val resolvedName = explicitName ?: if (continueNumbering) lastListStyleName else null
            if (resolvedName != null) {
                if (!continueNumbering) {
                    listCounters[resolvedName] = NumberingCounterState()
                }
                lastListStyleName = resolvedName
            } else if (!continueNumbering) {
                anonymousListCounter = NumberingCounterState()
            }
            return resolvedName
        }
        return explicitName
    }

    fun formatListItemLabel(
        listStyleName: String?,
        paragraphStyleName: String?,
        listLevel: Int,
        startValueOverride: Int? = null,
        continueNumbering: Boolean = false
    ): OdfFormattedListLabel {
        val paraListStyle = resolveParagraphListStyleName(paragraphStyleName)
        if (listStyleName.isNullOrBlank() && paraListStyle != null && paraListStyle.isEmpty()) {
            return OdfFormattedListLabel(bullet = "", isOrdered = false)
        }
        val effectiveName = listStyleName?.takeIf { it.isNotBlank() }
            ?: paraListStyle?.takeIf { it.isNotBlank() }
            ?: if (continueNumbering) lastListStyleName else null
        if (effectiveName != null && listStyleName.isNullOrBlank()) {
            lastListStyleName = effectiveName
        }
        val spec = lookupListStyle(effectiveName)
        if (spec != null) {
            val counter = listCounters.getOrPut(spec.name) { NumberingCounterState() }
            val raw = counter.advance(spec, listLevel, startValueOverride)
            val levelSpec = spec.level(listLevel)
            val charInfo = levelSpec?.textStyleName?.let { cascadeStyle(it, "text") }
            val bullet = when {
                raw.isEmpty() -> ""
                raw.endsWith(" ") || raw.endsWith("\t") -> raw
                else -> "$raw "
            }
            return OdfFormattedListLabel(
                bullet = bullet,
                isOrdered = levelSpec?.isBullet == false && raw.isNotEmpty(),
                labelFontSizeSp = levelSpec?.fontSizeSp ?: charInfo?.fontSizePt,
                labelFontFamily = levelSpec?.fontFamily ?: charInfo?.fontFamily,
                isBold = levelSpec?.isBold ?: charInfo?.isBold,
                isItalic = levelSpec?.isItalic ?: charInfo?.isItalic,
                colorHex = levelSpec?.colorHex ?: charInfo?.colorHex,
                textStyleName = levelSpec?.textStyleName
            )
        }
        val fallbackBulletSpec = NumberingLevelSpec(
            level = listLevel,
            isBullet = true,
            bulletChar = if (listLevel > 1) "\u25e6" else "\u2022"
        )
        val raw = NumberingFormatter.formatBullet(fallbackBulletSpec)
        return OdfFormattedListLabel(bullet = "$raw ", isOrdered = false)
    }

    fun formatHeadingLabel(
        styleName: String?,
        outlineLevel: Int,
        enclosingListStyleName: String? = null,
        enclosingListLevel: Int? = null,
        startValueOverride: Int? = null,
        continueNumbering: Boolean = false
    ): OdfFormattedListLabel? {
        if (enclosingListLevel != null) {
            val label = formatListItemLabel(
                listStyleName = enclosingListStyleName,
                paragraphStyleName = styleName,
                listLevel = enclosingListLevel,
                startValueOverride = startValueOverride,
                continueNumbering = continueNumbering
            )
            return label.takeIf { it.bullet.isNotEmpty() }
        }
        val paraListStyle = resolveParagraphListStyleName(styleName)
        if (paraListStyle != null) {
            if (paraListStyle.isEmpty()) return null
            val spec = lookupListStyle(paraListStyle) ?: return null
            val counter = listCounters.getOrPut(spec.name) { NumberingCounterState() }
            val raw = counter.advance(spec, outlineLevel, startValueOverride)
            if (raw.isEmpty()) return null
            val levelSpec = spec.level(outlineLevel)
            val charInfo = levelSpec?.textStyleName?.let { cascadeStyle(it, "text") }
            val prefix = if (raw.endsWith(" ") || raw.endsWith("\t")) raw else "$raw "
            return OdfFormattedListLabel(
                bullet = prefix,
                isOrdered = true,
                labelFontSizeSp = levelSpec?.fontSizeSp ?: charInfo?.fontSizePt,
                labelFontFamily = levelSpec?.fontFamily ?: charInfo?.fontFamily,
                isBold = levelSpec?.isBold ?: charInfo?.isBold,
                isItalic = levelSpec?.isItalic ?: charInfo?.isItalic,
                colorHex = levelSpec?.colorHex ?: charInfo?.colorHex,
                textStyleName = levelSpec?.textStyleName
            )
        }
        val spec = outlineStyle ?: return null
        val raw = outlineCounter.advance(spec, outlineLevel, startValueOverride)
        if (raw.isEmpty()) return null
        val levelSpec = spec.level(outlineLevel)
        val charInfo = levelSpec?.textStyleName?.let { cascadeStyle(it, "text") }
        val prefix = if (raw.endsWith(" ") || raw.endsWith("\t")) raw else "$raw "
        return OdfFormattedListLabel(
            bullet = prefix,
            isOrdered = true,
            labelFontSizeSp = levelSpec?.fontSizeSp ?: charInfo?.fontSizePt,
            labelFontFamily = levelSpec?.fontFamily ?: charInfo?.fontFamily,
            isBold = levelSpec?.isBold ?: charInfo?.isBold,
            isItalic = levelSpec?.isItalic ?: charInfo?.isItalic,
            colorHex = levelSpec?.colorHex ?: charInfo?.colorHex,
            textStyleName = levelSpec?.textStyleName
        )
    }

    // ODF page lengths are absolute (Part 1 §2.4). When print-orientation
    // declares landscape but the box is still portrait, only the box is
    // swapped; LibreOffice writes pre-swapped dimensions, so margins stay as
    // authored and this is the rare corrective path.
    private fun buildPageLayoutSpec(attrs: Map<String, String>): PageStyleSpec? {
        val width = OdfLength.toLayoutUnits(attrs["page-width"])
        val height = OdfLength.toLayoutUnits(attrs["page-height"])
        if (width <= 0f || height <= 0f) return null
        val landscape = attrs["print-orientation"].equals("landscape", ignoreCase = true)
        val swap = landscape && width < height
        return PageStyleSpec(
            widthDp = if (swap) height else width,
            heightDp = if (swap) width else height,
            marginTopDp = OdfLength.toLayoutUnits(attrs["margin-top"]),
            marginBottomDp = OdfLength.toLayoutUnits(attrs["margin-bottom"]),
            marginStartDp = OdfLength.toLayoutUnits(attrs["margin-left"]),
            marginEndDp = OdfLength.toLayoutUnits(attrs["margin-right"]),
            landscape = landscape
        )
    }

    fun resolveHeadingLevel(styleName: String?): Int? {
        if (styleName.isNullOrBlank()) return null
        var curr: String? = styleName
        var depth = 0
        while (curr != null && depth < 10) {
            val info = lookupStyle(curr)
            if (info != null) {
                if (info.outlineLevel != null && info.outlineLevel > 0) {
                    return info.outlineLevel.coerceIn(1, 6)
                }
                val disp = info.displayName
                if (!disp.isNullOrBlank()) {
                    val lvl = parseHeadingLevelFromText(disp)
                    if (lvl != null) return lvl
                }
                val lvlFromName = parseHeadingLevelFromText(info.name)
                if (lvlFromName != null) return lvlFromName
                curr = info.parentName
            } else {
                val lvlFromName = parseHeadingLevelFromText(curr)
                if (lvlFromName != null) return lvlFromName
                break
            }
            depth++
        }
        return parseHeadingLevelFromText(styleName)
    }

    private fun parseHeadingLevelFromText(text: String): Int? {
        val level = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(text)
        return level.takeIf { it > 0 }
    }

    fun toDocumentStyles(): DocumentStyles {
        val paragraphs = LinkedHashMap<String, ParagraphStyle>()
        val characters = LinkedHashMap<String, CharacterStyle>()
        for (info in styleMap.values) {
            val family = info.family
            if (family.equals("text", ignoreCase = true)) {
                val cascaded = cascadeStyle(info.name, "text") ?: info
                characters.putIfAbsent(info.name, cascaded.toCharacterStyle())
            } else if (family.isBlank() || family.equals("paragraph", ignoreCase = true)) {
                val cascaded = cascadeStyle(info.name, "paragraph") ?: info
                paragraphs.putIfAbsent(info.name, cascaded.toParagraphStyle())
            }
        }
        val defaultPage = (standardPageLayoutName ?: firstMasterPageLayoutName)
            ?.let { pageLayouts[it] }
            ?: pageLayouts.values.firstOrNull()
            ?: pageSpecFromDefaultStyle
        val defaultPara = defaultParagraphStyle?.toParagraphStyle()
        val tableStyles = resolveAllTableStyles()
        return DocumentStyles(
            paragraphStyles = paragraphs,
            characterStyles = characters,
            pageStyles = pageLayouts.toMap(),
            defaultPageStyle = defaultPage,
            masterPages = masterPages.toMap(),
            firstMasterPageName = firstBodyMasterPageName(),
            defaultParagraphStyle = defaultPara,
            listStyles = listStyles.toMap(),
            outlineStyle = outlineStyle,
            fontFaces = fontFaces.toMap(),
            tableStyles = tableStyles.filter { it.family.equals("table", ignoreCase = true) }
                .associateBy { it.name },
            tableColumnStyles = tableStyles.filter { it.family.equals("table-column", ignoreCase = true) }
                .associateBy { it.name },
            tableRowStyles = tableStyles.filter { it.family.equals("table-row", ignoreCase = true) }
                .associateBy { it.name },
            tableCellStyles = tableStyles.filter { it.family.equals("table-cell", ignoreCase = true) }
                .associateBy { it.name }
        )
    }

    fun hasPageBreakBefore(styleName: String?): Boolean {
        val cascaded = styleName?.let { cascadeStyle(it, "paragraph") } ?: defaultParagraphStyle
        return cascaded?.pageBreakBefore == true
    }

    fun hasPageBreakAfter(styleName: String?): Boolean {
        val cascaded = styleName?.let { cascadeStyle(it, "paragraph") } ?: defaultParagraphStyle
        return cascaded?.pageBreakAfter == true
    }

    /**
     * Master page the body starts on: the first paragraph's or heading's
     * style chain is walked for `style:master-page-name` (ODF 1.4 Part 3
     * §19.505, set on the automatic style of the paragraph that begins a
     * page). Null when the body names none, which ODF resolves to "Standard".
     */
    private fun firstBodyMasterPageName(): String? {
        val firstStyle = parsedElements.firstNotNullOfOrNull { element ->
            when (element) {
                is OfficeDocumentElement.Paragraph -> element.styleName ?: ""
                is OfficeDocumentElement.Heading -> element.styleName ?: ""
                else -> null
            }
        } ?: return null
        var curr: String? = firstStyle.takeIf { it.isNotBlank() }
        val seen = HashSet<String>(8)
        var depth = 0
        while (curr != null && depth < 16 && seen.add(curr.lowercase(Locale.ROOT))) {
            val info = lookupStyle(curr) ?: return null
            info.masterPageName?.takeIf { it.isNotBlank() }?.let { return it }
            curr = info.parentName
            depth++
        }
        return null
    }

    /**
     * Executes context-driven ODF XML import for content.xml input stream.
     */
    fun parseOdfXml(
        xmlContent: String,
        fileName: String,
        stylesXmlContent: String? = null,
        isOdt: Boolean = true,
        isOds: Boolean = false,
        isOdp: Boolean = false,
        onStylesParsed: (() -> Unit)? = null
    ): OfficeParsedDocument {
        parsedElements.clear()
        contextStack.clear()
        styleMap.clear()
        tableStyleMap.clear()
        pageLayouts.clear()
        masterPages.clear()
        pageSpecFromDefaultStyle = null
        standardPageLayoutName = null
        firstMasterPageLayoutName = null
        defaultParagraphStyle = null
        listStyles.clear()
        outlineStyle = null
        fontFaces.clear()
        outlineCounter.reset()
        listCounters.clear()
        anonymousListCounter.reset()
        lastListStyleName = null
        documentBookmarks.clear()
        semanticRanges.reset()
        currentFileName = fileName

        // Preload style hierarchies from styles.xml and content.xml automatic-styles
        // without clearing defaults between the two so office:styles survive.
        if (!stylesXmlContent.isNullOrBlank()) {
            parseOdfStyles(stylesXmlContent)
        }
        parseOdfStyles(xmlContent)
        onStylesParsed?.invoke()

        // Push initial Root document context
        val rootContext = OdfDocumentContentContext(this, OdfXmlToken.XML_DOCUMENT_CONTENT)
        contextStack.push(rootContext)

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val rawTagName = parser.name ?: ""
                        val token = OdfXmlToken.fromTag(rawTagName)
                        
                        val attributes = mutableMapOf<String, String>()
                        for (i in 0 until parser.attributeCount) {
                            val attrName = parser.getAttributeName(i)
                            val attrPrefix = parser.getAttributePrefix(i)
                            val fullName = if (!attrPrefix.isNullOrEmpty()) "$attrPrefix:$attrName" else attrName
                            attributes[fullName] = parser.getAttributeValue(i)
                            // Also map unprefixed name for convenience
                            attributes[attrName] = parser.getAttributeValue(i)
                        }

                        val currentTop = contextStack.peek() ?: rootContext
                        val childContext = currentTop.createChildContext(token, attributes)
                        
                        // Push child context onto stack and notify start element
                        contextStack.push(childContext)
                        childContext.onStartElement(token, attributes)

                        if (token == OdfXmlToken.XML_UNKNOWN && rawTagName.isNotBlank()) {
                            diagnostics.unsupportedTag(fileName, rawTagName, attributes)
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            contextStack.peek()?.onCharacters(text)
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val rawTagName = parser.name ?: ""
                        val token = OdfXmlToken.fromTag(rawTagName)
                        
                        if (!contextStack.isEmpty()) {
                            val topContext = contextStack.pop()
                            topContext.onEndElement(token)
                        }
                    }
                }
                eventType = parser.next()
            }
            if (contextStack.size != 1) {
                throw IllegalStateException("Unexpected end of ODF XML with ${contextStack.size - 1} unclosed elements")
            }

            // Build full plain text from parsed elements
            val plainTextBuilder = StringBuilder()
            parsedElements.forEach { element ->
                when (element) {
                    is OfficeDocumentElement.Paragraph -> plainTextBuilder.append(element.text).append("\n\n")
                    is OfficeDocumentElement.Heading -> plainTextBuilder.append(element.text).append("\n\n")
                    is OfficeDocumentElement.ListItem -> plainTextBuilder.append(element.bullet).append(element.text).append("\n")
                    is OfficeDocumentElement.Table -> {
                        if (!element.name.isNullOrBlank()) {
                            plainTextBuilder.append("=== Sheet: ").append(element.name).append(" ===\n")
                        }
                        element.rows.forEach { row ->
                            plainTextBuilder.append(row.cells.joinToString("\t") { it.text }).append("\n")
                        }
                        plainTextBuilder.append("\n")
                    }
                    else -> {}
                }
            }

            val odpSlideCount = if (isOdp) {
                val breaks = parsedElements.count { it is OfficeDocumentElement.PageBreak }
                if (parsedElements.isNotEmpty()) breaks + 1 else 0
            } else 0

            return OfficeParsedDocument(
                elements = parsedElements.toList(),
                rawXml = xmlContent,
                plainText = plainTextBuilder.toString().trim(),
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = false,
                isOds = isOds,
                isXlsx = false,
                isOdp = isOdp,
                isPptx = false,
                isParsingFailed = false,
                failureReason = null,
                pageCount = odpSlideCount,
                styles = toDocumentStyles(),
                bookmarks = documentBookmarks.toList(),
                authoredIndexes = if (isOdt) semanticRanges.authoredIndexes() else emptyList(),
                namedSectionRanges = if (isOdt) semanticRanges.namedSections() else emptyList()
            )

        } catch (e: Exception) {
            val errorMsg = "ODF SAX Import Error: ${e.localizedMessage ?: "Failed parsing XML"}"
            diagnostics.malformedXml(fileName, errorMsg, e)
            return OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = false,
                isOds = isOds,
                isXlsx = false,
                isOdp = isOdp,
                isPptx = false,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }
    }
}

private fun OdfStyleInfo.toParagraphStyle(): ParagraphStyle = ParagraphStyle(
    name = name,
    fontSizeSp = fontSizePt ?: 12f,
    isBold = isBold == true,
    isItalic = isItalic == true,
    isUnderline = isUnderline == true,
    colorHex = colorHex,
    alignment = alignment ?: "Left",
    fontFamily = fontFamily,
    parentStyleName = parentName,
    spaceBeforeUnits = spaceBeforeUnits ?: 0f,
    spaceAfterUnits = spaceAfterUnits ?: 0f,
    lineHeightFactor = lineHeightFactor ?: 1f,
    lineHeightExactUnits = lineHeightExactUnits,
    lineHeightUsesFontSize = lineHeightUsesFontSize == true,
    indentStartUnits = indentStartUnits ?: 0f,
    indentEndUnits = indentEndUnits ?: 0f,
    firstLineIndentUnits = firstLineIndentUnits ?: 0f,
    keepWithNext = keepWithNext == true,
    pageBreakBefore = pageBreakBefore == true,
    pageBreakAfter = pageBreakAfter == true,
    keepTogether = keepTogether == true,
    orphans = orphans ?: 2,
    widows = widows ?: 2,
    tabStops = tabStops.orEmpty(),
    defaultTabIntervalUnits = defaultTabIntervalUnits ?: 48f,
    masterPageName = masterPageName,
    listStyleName = listStyleName
)

private fun OdfStyleInfo.toCharacterStyle(): CharacterStyle = CharacterStyle(
    name = name,
    fontSizeSp = fontSizePt,
    isBold = isBold == true,
    isItalic = isItalic == true,
    isUnderline = isUnderline == true,
    colorHex = colorHex,
    fontFamily = fontFamily,
    parentStyleName = parentName
)
