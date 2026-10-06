package com.makerandreas.papirusoffice.data

import com.makerandreas.papirusoffice.data.util.readCappedBytes
import com.makerandreas.papirusoffice.data.util.BudgetedInputStream
import com.makerandreas.papirusoffice.data.util.ZipSafe
import com.makerandreas.papirusoffice.data.util.ZipScanBudget
import com.makerandreas.papirusoffice.data.util.nextEntryBudgeted
import com.makerandreas.papirusoffice.data.odf.AndroidOdfImportDiagnostics
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import java.util.Locale
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.content.Context
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipInputStream

data class ParsingProgress(
    val percentage: Int = 0,
    val statusMessage: String = "Loading document...",
    val isFailed: Boolean = false,
    val errorMessage: String? = null,
    /** Plan 6C: the real open stage this update announces; null for failures. */
    val stage: LoadingStage? = null
)

sealed class SchemaValidationResult {
    object Valid : SchemaValidationResult()
    data class Invalid(val reason: String, val warnings: List<String> = emptyList()) : SchemaValidationResult()
}

/**
 * One `w:style` definition as `word/styles.xml` states it, before the
 * `w:basedOn` chain is resolved. Every property is nullable so "this style
 * does not set it" stays distinguishable from "this style sets it false":
 * that distinction is what makes the owner decision of 2026-10-06 (the
 * paragraph style wins for the properties it sets, its linked character style
 * supplies the rest) implementable. Toggle properties are tri-state for the
 * same reason (ECMA-376 Part 1 §17.7.3, `[MS-OI29500]` §17.7.3 p.105).
 */
data class DocxStyleMeta(
    val styleId: String,
    val name: String,
    /** `paragraph`, `character`, `table` or `numbering` (`w:type`). */
    val styleType: String = "paragraph",
    val outlineLvl: Int? = null,
    val isHeading: Boolean = false,
    val headingLevel: Int = 1,
    val basedOn: String? = null,
    /** `w:link` target: the character style this paragraph style is linked to (§17.7.4.6). */
    val link: String? = null,
    val fontSizeSp: Float? = null,
    /** `w:szCs` complex-script half-point size; used only when the chain sets no `w:sz`. */
    val fontSizeCsSp: Float? = null,
    val isBold: Boolean? = null,
    val isItalic: Boolean? = null,
    val isUnderline: Boolean? = null,
    val fontFamily: String? = null,
    val colorHex: String? = null,
    /** `w:highlight` colour name (`ST_HighlightColor`). */
    val highlight: String? = null,
    /** `w:vanish` hidden text. */
    val isHidden: Boolean? = null,
    /** Style-level `w:numPr` (`w:numId` 0 suppresses numbering for the style and its descendants). */
    val numId: Int? = null,
    val numIlvl: Int? = null,
    val spaceBeforeUnits: Float? = null,
    val spaceAfterUnits: Float? = null,
    val lineHeightFactor: Float? = null,
    val lineHeightExactUnits: Float? = null,
    val lineHeightMinimumUnits: Float? = null,
    val indentStartUnits: Float? = null,
    val indentEndUnits: Float? = null,
    val firstLineIndentUnits: Float? = null,
    val keepWithNext: Boolean? = null,
    val pageBreakBefore: Boolean? = null,
    val alignment: String? = null,
    val keepTogether: Boolean? = null,
    val widowControl: Boolean? = null,
    val tabStops: List<ParagraphTabStop>? = null
)

data class DocxStylesParseResult(
    val stylesMetaMap: Map<String, DocxStyleMeta> = emptyMap(),
    val paragraphStyles: Map<String, ParagraphStyle> = emptyMap(),
    /** `w:type="character"` styles after their own `w:basedOn` chain is resolved. */
    val characterStyles: Map<String, CharacterStyle> = emptyMap(),
    /** Effective `w:numPr` per paragraph style, resolved through the chain; Plan 8B renders labels from it. */
    val numberingByStyle: Map<String, DocxNumberingRef> = emptyMap(),
    val defaultParagraphStyle: ParagraphStyle? = null
)

/**
 * Character properties of one resolved level of a `word/styles.xml` chain.
 * Every field stays nullable so "this level does not set it" is
 * distinguishable from "this level sets it false", which is what lets the
 * paragraph style keep its own value and take only the gaps from its linked
 * character style (owner decision 2026-10-06, audit-019 section 4.1).
 */
private data class DocxRunProps(
    val sizeSp: Float? = null,
    val sizeCsSp: Float? = null,
    val fontFamily: String? = null,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val colorHex: String? = null,
    val highlight: String? = null,
    val hidden: Boolean? = null
)

/**
 * Modern document parser for ODT and DOCX files.
 * Uses standard Java ZIP and XML libraries to extract 'content.xml' (ODT) or 'word/document.xml' (DOCX)
 * and parse paragraphs, headings, tables, list items, and images into structured OfficeParsedDocument models.
 * Logs malformed XML structures or unsupported tags into crash.log via DocumentParsingLogger.
 */
class OfficeDocumentParser(private val context: Context) {

    private data class ParsedCacheEntry(
        val lastModified: Long,
        val fileLength: Long,
        val document: OfficeParsedDocument
    )

    companion object {
        private val inMemoryParsedDocCache = ConcurrentHashMap<String, ParsedCacheEntry>()

        // SpreadsheetML / PresentationML entry patterns (hoisted: matched per sheet/slide).
        private val XLSX_SHEET_FILE_REGEX = Regex("sheet(\\d+)")
        private val PPTX_SLIDE_FILE_REGEX = Regex("slide(\\d+)\\.xml")
        // Strict full-entry match: slide layouts/masters share the ppt/slides/slide prefix.
        private val PPTX_SLIDE_ENTRY_REGEX = Regex("^ppt/slides/slide\\d+\\.xml$")

        /** ECMA-376 §17.7.4.17: a `styleId` longer than this is ignored by Word. */
        private const val MAX_STYLE_ID_LENGTH = 253

        /** Built-in styles whose child elements Word ignores (ECMA-376 §17.7.4.17). */
        private val STYLES_WITH_IGNORED_CHILDREN = setOf("NoList", "DefaultParagraphFont", "TableNormal")

        fun clearCacheForFile(path: String) {
            inMemoryParsedDocCache.remove(path)
        }

        fun getCachedDocument(path: String, lastModified: Long): OfficeParsedDocument? {
            val cached = inMemoryParsedDocCache[path] ?: return null
            if (cached.lastModified == lastModified && !cached.document.isParsingFailed) {
                return cached.document
            }
            return null
        }
    }

    private fun extractDocxRelationships(file: File): Map<String, String> {
        val relsMap = mutableMapOf<String, String>()
        if (!file.exists() || !file.name.endsWith(".docx", ignoreCase = true)) return relsMap
        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/_rels/document.xml.rels") {
                        val xml = zip.readCappedBytes().toString(Charsets.UTF_8)
                        val factory = XmlPullParserFactory.newInstance()
                        factory.isNamespaceAware = false
                        val parser = factory.newPullParser()
                        parser.setInput(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)), "UTF-8")
                        var ev = parser.eventType
                        while (ev != XmlPullParser.END_DOCUMENT) {
                            if (ev == XmlPullParser.START_TAG && (parser.name?.equals("relationship", ignoreCase = true) == true || parser.name?.equals("Relationship", ignoreCase = true) == true)) {
                                val id = getXmlAttr(parser, "id")
                                val target = getXmlAttr(parser, "target")
                                if (!id.isNullOrBlank() && !target.isNullOrBlank()) {
                                    relsMap[id] = target
                                }
                            }
                            ev = parser.next()
                        }
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
        return relsMap
    }

    private fun extractDocxPageCount(file: File): Int? {
        if (!file.exists() || !file.name.endsWith(".docx", ignoreCase = true)) return null
        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "docProps/app.xml") {
                        val xml = zip.readCappedBytes().toString(Charsets.UTF_8)
                        val match = Regex("<(?:[a-zA-Z0-9]+:)?Pages>(\\d+)</(?:[a-zA-Z0-9]+:)?Pages>", RegexOption.IGNORE_CASE).find(xml)
                        val p = match?.groupValues?.get(1)?.toIntOrNull()
                        if (p != null && p > 0) return p
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
        return null
    }

    /**
     * Page box for the last <w:sectPr> in WordprocessingML. OOXML measures in
     * twips at 1440 per inch; converted to the LayoutEngine unit space of
     * 96/inch. Word applies 1in margins when <w:pgMar> is absent. Returns
     * null when no page size is declared, keeping the Letter fallback.
     */
    private fun extractDocxPageStyleSpec(documentXml: String): PageStyleSpec? {
        val lastSection = Regex("<w:sectPr\\b[^>]*>.*?</w:sectPr>", RegexOption.DOT_MATCHES_ALL)
            .findAll(documentXml).lastOrNull()?.value ?: return null
        val pgSz = Regex("<w:pgSz\\b[^>]*/?>").find(lastSection)?.value ?: return null
        val widthTwips = Regex("w:w=\"(\\d+)\"").find(pgSz)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val heightTwips = Regex("w:h=\"(\\d+)\"").find(pgSz)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val landscape = Regex("w:orient=\"landscape\"", RegexOption.IGNORE_CASE).containsMatchIn(pgSz)
        val widthUnits = com.makerandreas.papirusoffice.data.util.OdfLength.twipsToLayoutUnits(widthTwips)
        val heightUnits = com.makerandreas.papirusoffice.data.util.OdfLength.twipsToLayoutUnits(heightTwips)
        val swap = landscape && widthUnits < heightUnits

        val pgMar = Regex("<w:pgMar\\b[^>]*/?>").find(lastSection)?.value
        fun margin(name: String): Float {
            val twips = pgMar
                ?.let { Regex("w:$name=\"(-?\\d+)\"").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                ?: 1440
            return com.makerandreas.papirusoffice.data.util.OdfLength.twipsToLayoutUnits(twips)
        }

        return PageStyleSpec(
            name = "docx-sectPr",
            widthDp = if (swap) heightUnits else widthUnits,
            heightDp = if (swap) widthUnits else heightUnits,
            marginTopDp = margin("top"),
            marginBottomDp = margin("bottom"),
            marginStartDp = margin("left"),
            marginEndDp = margin("right"),
            landscape = landscape
        )
    }

    private fun extractOdtPageCount(file: File): Int? {
        if (!file.exists() || !file.name.endsWith(".odt", ignoreCase = true)) return null
        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "meta.xml") {
                        val xml = zip.readCappedBytes().toString(Charsets.UTF_8)
                        val match = Regex("""(?:meta:)?page-count="(\d+)"""", RegexOption.IGNORE_CASE).find(xml)
                            ?: Regex("""<meta:page-count>(\d+)</meta:page-count>""", RegexOption.IGNORE_CASE).find(xml)
                        val p = match?.groupValues?.get(1)?.toIntOrNull()
                        if (p != null && p > 0) return p
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
        return null
    }

    private fun getXmlAttr(parser: XmlPullParser, localOrQName: String): String? {
        val target = localOrQName.lowercase(Locale.ROOT).substringAfter(':')
        for (i in 0 until parser.attributeCount) {
            val attrName = parser.getAttributeName(i).lowercase(Locale.ROOT).substringAfter(':')
            if (attrName == target) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private fun parseDocxImageExtent(parser: XmlPullParser): Pair<Float, Float>? {
        val cx = getXmlAttr(parser, "cx")?.toLongOrNull() ?: return null
        val cy = getXmlAttr(parser, "cy")?.toLongOrNull() ?: return null
        if (cx <= 0L || cy <= 0L) return null
        val width = LayoutUnits.emuToUnits(cx)
        val height = LayoutUnits.emuToUnits(cy)
        if (!width.isFinite() || !height.isFinite() || width <= 0f || height <= 0f) return null
        return width to height
    }

    private fun docxOnOff(value: String?): Boolean =
        value?.lowercase(Locale.ROOT) !in setOf("0", "false", "off")

    /**
     * ECMA-376 Part 1 §17.7.3 toggle property: present without `w:val`, or with
     * `1`/`true`/`on`, means true; `0`/`false`/`off` means false; an absent
     * element means inherit, which is why the caller keeps nulls around.
     *
     * Inside one `w:basedOn` chain the nearest definition wins, so a derived
     * style's explicit `w:b w:val="0"` cancels an inherited bold. The
     * cross-tier XOR of the twelve toggle properties (`[MS-OI29500]` §2.1.230,
     * p.105) is deliberately not modelled where the paragraph style meets its
     * linked character style: the owner decision of 2026-10-06 fixes that
     * merge as own-properties-first, with the twin filling only the gaps
     * (audit-019 section 4.2 records the deviation).
     */
    private fun docxToggle(value: String?): Boolean = docxOnOff(value)

    private fun parseDocxTab(parser: XmlPullParser): ParagraphTabStop? {
        val pos = getXmlAttr(parser, "pos")?.toIntOrNull() ?: return null
        val alignment = when (getXmlAttr(parser, "val")?.lowercase(Locale.ROOT)) {
            "clear" -> TabAlignment.CLEAR
            "right", "end" -> TabAlignment.RIGHT
            "center" -> TabAlignment.CENTER
            "decimal" -> TabAlignment.DECIMAL
            "bar" -> return null // Border tabs are not positional text stops.
            else -> TabAlignment.LEFT
        }
        return ParagraphTabStop(LayoutUnits.twipsToUnits(pos), alignment)
    }

    private fun mergeTabs(base: List<ParagraphTabStop>, over: List<ParagraphTabStop>?): List<ParagraphTabStop> =
        if (over == null) base else (base + over).associateBy { it.positionUnits }.values.sortedBy { it.positionUnits }

    private fun docxTabInterval(file: File): Float {
        return try {
            java.util.zip.ZipFile(file).use { zip ->
                val entry = zip.getEntry("word/settings.xml") ?: return 48f
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                zip.getInputStream(entry).use { input ->
                    parser.setInput(input, "UTF-8")
                    while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name.substringAfter(':').equals("defaultTabStop", true)) {
                            return getXmlAttr(parser, "val")?.toIntOrNull()?.takeIf { it > 0 }?.let { LayoutUnits.twipsToUnits(it) } ?: 48f
                        }
                        parser.next()
                    }
                }
            }
            48f
        } catch (_: Exception) { 48f }
    }

    private fun extractDocxStyles(file: File): DocxStylesParseResult {
        if (!file.exists() || !file.name.endsWith(".docx", ignoreCase = true)) return DocxStylesParseResult()
        val tabInterval = docxTabInterval(file)
        try {
            var stylesXmlContent: String? = null
            java.util.zip.ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name == "word/styles.xml") {
                        stylesXmlContent = zip.readCappedBytes().toString(Charsets.UTF_8)
                        break
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (stylesXmlContent != null) {
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = false
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(stylesXmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

                var eventType = parser.eventType
                var inDocDefaults = false
                var inDocDefaultsRPr = false
                var inDocDefaultsPPr = false
                var docDefaultSz: Float? = null
                var docDefaultSzCs: Float? = null
                var docDefaultBold: Boolean? = null
                var docDefaultItalic: Boolean? = null
                var docDefaultUnderline: Boolean? = null
                var docDefaultColor: String? = null
                var docDefaultVanish: Boolean? = null
                var docDefaultFont: String? = null
                var docDefaultBefore: Float? = null
                var docDefaultAfter: Float? = null
                var docDefaultLineFactor: Float? = null
                var docDefaultLineExact: Float? = null
                var docDefaultLineMinimum: Float? = null
                var docDefaultIndentStart: Float? = null
                var docDefaultIndentEnd: Float? = null
                var docDefaultFirstLine: Float? = null
                var docDefaultKeepNext: Boolean? = null
                var docDefaultPageBreakBefore: Boolean? = null
                var docDefaultKeepTogether: Boolean? = null
                var docDefaultWidowControl: Boolean? = null
                var docDefaultTabs: List<ParagraphTabStop>? = null

                var inStyle = false
                var currentStyleIgnored = false
                var currentStyleType: String? = null
                var currentStyleId: String? = null
                var currentStyleName: String? = null
                var currentBasedOn: String? = null
                var currentLink: String? = null
                var currentOutlineLvl: Int? = null
                var inRPr = false
                var inPPr = false
                var currentSz: Float? = null
                var currentSzCs: Float? = null
                var currentColor: String? = null
                var currentHighlight: String? = null
                var currentVanish: Boolean? = null
                var currentNumId: Int? = null
                var currentNumIlvl: Int? = null
                var currentBold: Boolean? = null
                var currentItalic: Boolean? = null
                var currentUnderline: Boolean? = null
                var currentFont: String? = null
                var currentBefore: Float? = null
                var currentAfter: Float? = null
                var currentLineFactor: Float? = null
                var currentLineExact: Float? = null
                var currentLineMinimum: Float? = null
                var currentIndentStart: Float? = null
                var currentIndentEnd: Float? = null
                var currentFirstLine: Float? = null
                var currentKeepNext: Boolean? = null
                var currentPageBreakBefore: Boolean? = null
                var currentKeepTogether: Boolean? = null
                var currentWidowControl: Boolean? = null
                var currentTabs: List<ParagraphTabStop>? = null
                var currentJc: String? = null

                val rawStylesMap = mutableMapOf<String, DocxStyleMeta>()

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val rawTag = parser.name ?: ""
                    val tagName = rawTag.lowercase(Locale.ROOT).substringAfter(':')
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when {
                                tagName == "docdefaults" -> {
                                    inDocDefaults = true
                                }
                                inDocDefaults && (tagName == "rprdefault" || tagName == "rpr") -> {
                                    inDocDefaultsRPr = true
                                }
                                inDocDefaults && (tagName == "pprdefault" || tagName == "ppr") -> {
                                    inDocDefaultsPPr = true
                                }
                                inDocDefaultsRPr && tagName == "sz" -> {
                                    val valStr = getXmlAttr(parser, "val")
                                    valStr?.toIntOrNull()?.let { docDefaultSz = LayoutUnits.halfPointsToPt(it) }
                                }
                                inDocDefaultsRPr && tagName == "szcs" -> {
                                    getXmlAttr(parser, "val")?.toIntOrNull()?.let { docDefaultSzCs = LayoutUnits.halfPointsToPt(it) }
                                }
                                inDocDefaultsRPr && tagName == "b" -> { docDefaultBold = docxToggle(getXmlAttr(parser, "val")) }
                                inDocDefaultsRPr && tagName == "i" -> { docDefaultItalic = docxToggle(getXmlAttr(parser, "val")) }
                                inDocDefaultsRPr && tagName == "u" -> {
                                    val uVal = getXmlAttr(parser, "val")?.lowercase(Locale.ROOT)
                                    docDefaultUnderline = uVal == null || uVal !in setOf("none", "0", "false", "off")
                                }
                                inDocDefaultsRPr && tagName == "color" -> { docDefaultColor = getXmlAttr(parser, "val") }
                                inDocDefaultsRPr && tagName == "vanish" -> { docDefaultVanish = docxToggle(getXmlAttr(parser, "val")) }
                                inDocDefaultsRPr && tagName == "rfonts" -> {
                                    val fontStr = getXmlAttr(parser, "ascii") ?: getXmlAttr(parser, "hansi")
                                    if (!fontStr.isNullOrBlank()) docDefaultFont = fontStr.trim('\'', '"')
                                }
                                inDocDefaultsPPr && tagName == "spacing" -> {
                                    val bStr = getXmlAttr(parser, "before")
                                    bStr?.toIntOrNull()?.let { docDefaultBefore = LayoutUnits.twipsToUnits(it) }
                                    val aStr = getXmlAttr(parser, "after")
                                    aStr?.toIntOrNull()?.let { docDefaultAfter = LayoutUnits.twipsToUnits(it) }
                                    val lStr = getXmlAttr(parser, "line")
                                    val lrStr = getXmlAttr(parser, "linerule") ?: "auto"
                                    if (lStr != null) {
                                        val lv = lStr.toIntOrNull()
                                        if (lv != null) {
                                            docDefaultLineFactor = null
                                            docDefaultLineExact = null
                                            docDefaultLineMinimum = null
                                            when (lrStr.lowercase(Locale.ROOT)) {
                                                "exact" -> docDefaultLineExact = LayoutUnits.twipsToUnits(lv)
                                                "atleast" -> docDefaultLineMinimum = LayoutUnits.twipsToUnits(lv)
                                                else -> docDefaultLineFactor = LayoutUnits.lineTwentiethsToFactor(lv)
                                            }
                                        }
                                    }
                                }
                                inDocDefaultsPPr && tagName == "keeplines" -> { docDefaultKeepTogether = docxOnOff(getXmlAttr(parser, "val")) }
                                inDocDefaultsPPr && tagName == "widowcontrol" -> { docDefaultWidowControl = docxOnOff(getXmlAttr(parser, "val")) }
                                inDocDefaultsPPr && tagName == "tab" -> {
                                    parseDocxTab(parser)?.let { docDefaultTabs = docDefaultTabs.orEmpty() + it }
                                }
                                inDocDefaultsPPr && tagName == "keepnext" -> {
                                    docDefaultKeepNext = docxOnOff(getXmlAttr(parser, "val"))
                                }
                                inDocDefaultsPPr && tagName == "pagebreakbefore" -> {
                                    docDefaultPageBreakBefore = docxOnOff(getXmlAttr(parser, "val"))
                                }
                                inDocDefaultsPPr && tagName == "ind" -> {
                                    val lStr = getXmlAttr(parser, "left") ?: getXmlAttr(parser, "start")
                                    lStr?.toIntOrNull()?.let { docDefaultIndentStart = LayoutUnits.twipsToUnits(it) }
                                    val rStr = getXmlAttr(parser, "right") ?: getXmlAttr(parser, "end")
                                    rStr?.toIntOrNull()?.let { docDefaultIndentEnd = LayoutUnits.twipsToUnits(it) }
                                    val flStr = getXmlAttr(parser, "firstline")
                                    flStr?.toIntOrNull()?.let { docDefaultFirstLine = LayoutUnits.twipsToUnits(it) }
                                    val hStr = getXmlAttr(parser, "hanging")
                                    hStr?.toIntOrNull()?.let { docDefaultFirstLine = -LayoutUnits.twipsToUnits(it) }
                                }
                                tagName == "style" -> {
                                    val styleIdAttr = getXmlAttr(parser, "styleid")
                                    // ECMA-376 §17.7.4.17: a styleId longer than 253 characters is ignored.
                                    currentStyleIgnored = !styleIdAttr.isNullOrBlank() && styleIdAttr.length > MAX_STYLE_ID_LENGTH
                                    inStyle = !currentStyleIgnored
                                    currentStyleType = getXmlAttr(parser, "type") ?: "paragraph"
                                    currentStyleId = styleIdAttr
                                    currentStyleName = null
                                    currentBasedOn = null
                                    currentLink = null
                                    currentOutlineLvl = null
                                    inRPr = false
                                    inPPr = false
                                    currentSz = null
                                    currentSzCs = null
                                    currentColor = null
                                    currentHighlight = null
                                    currentVanish = null
                                    currentNumId = null
                                    currentNumIlvl = null
                                    currentBold = null
                                    currentItalic = null
                                    currentUnderline = null
                                    currentFont = null
                                    currentBefore = null
                                    currentAfter = null
                                    currentLineFactor = null
                                    currentLineExact = null
                                    currentLineMinimum = null
                                    currentIndentStart = null
                                    currentIndentEnd = null
                                    currentFirstLine = null
                                    currentKeepNext = null
                                    currentPageBreakBefore = null
                                    currentKeepTogether = null
                                    currentWidowControl = null
                                    currentTabs = null
                                    currentJc = null
                                }
                                inStyle && tagName == "name" -> {
                                    currentStyleName = getXmlAttr(parser, "val")
                                }
                                inStyle && tagName == "basedon" -> {
                                    currentBasedOn = getXmlAttr(parser, "val")
                                }
                                inStyle && tagName == "link" -> {
                                    currentLink = getXmlAttr(parser, "val")
                                }
                                inStyle && tagName == "outlinelvl" -> {
                                    currentOutlineLvl = getXmlAttr(parser, "val")?.toIntOrNull()
                                }
                                inStyle && tagName == "rpr" -> {
                                    inRPr = true
                                }
                                inStyle && inRPr && tagName == "sz" -> {
                                    val valStr = getXmlAttr(parser, "val")
                                    valStr?.toIntOrNull()?.let { currentSz = LayoutUnits.halfPointsToPt(it) }
                                }
                                inStyle && inRPr && tagName == "szcs" -> {
                                    val valStr = getXmlAttr(parser, "val")
                                    valStr?.toIntOrNull()?.let { currentSzCs = LayoutUnits.halfPointsToPt(it) }
                                }
                                inStyle && inRPr && tagName == "b" -> { currentBold = docxToggle(getXmlAttr(parser, "val")) }
                                inStyle && inRPr && tagName == "i" -> { currentItalic = docxToggle(getXmlAttr(parser, "val")) }
                                inStyle && inRPr && tagName == "u" -> {
                                    val uVal = getXmlAttr(parser, "val")?.lowercase(Locale.ROOT)
                                    currentUnderline = uVal == null || uVal !in setOf("none", "0", "false", "off")
                                }
                                inStyle && inRPr && tagName == "color" -> { currentColor = getXmlAttr(parser, "val") }
                                inStyle && inRPr && tagName == "highlight" -> { currentHighlight = getXmlAttr(parser, "val") }
                                inStyle && inRPr && tagName == "vanish" -> { currentVanish = docxToggle(getXmlAttr(parser, "val")) }
                                inStyle && inRPr && tagName == "rfonts" -> {
                                    val fontStr = getXmlAttr(parser, "ascii") ?: getXmlAttr(parser, "hansi")
                                    if (!fontStr.isNullOrBlank()) currentFont = fontStr.trim('\'', '"')
                                }
                                inStyle && tagName == "ppr" -> {
                                    inPPr = true
                                }
                                inStyle && inPPr && tagName == "spacing" -> {
                                    val bStr = getXmlAttr(parser, "before")
                                    bStr?.toIntOrNull()?.let { currentBefore = LayoutUnits.twipsToUnits(it) }
                                    val aStr = getXmlAttr(parser, "after")
                                    aStr?.toIntOrNull()?.let { currentAfter = LayoutUnits.twipsToUnits(it) }
                                    val lStr = getXmlAttr(parser, "line")
                                    val lrStr = getXmlAttr(parser, "linerule") ?: "auto"
                                    if (lStr != null) {
                                        val lv = lStr.toIntOrNull()
                                        if (lv != null) {
                                            currentLineFactor = null
                                            currentLineExact = null
                                            currentLineMinimum = null
                                            when (lrStr.lowercase(Locale.ROOT)) {
                                                "exact" -> currentLineExact = LayoutUnits.twipsToUnits(lv)
                                                "atleast" -> currentLineMinimum = LayoutUnits.twipsToUnits(lv)
                                                else -> currentLineFactor = LayoutUnits.lineTwentiethsToFactor(lv)
                                            }
                                        }
                                    }
                                }
                                inStyle && inPPr && tagName == "ind" -> {
                                    val lStr = getXmlAttr(parser, "left") ?: getXmlAttr(parser, "start")
                                    lStr?.toIntOrNull()?.let { currentIndentStart = LayoutUnits.twipsToUnits(it) }
                                    val rStr = getXmlAttr(parser, "right") ?: getXmlAttr(parser, "end")
                                    rStr?.toIntOrNull()?.let { currentIndentEnd = LayoutUnits.twipsToUnits(it) }
                                    val flStr = getXmlAttr(parser, "firstline")
                                    flStr?.toIntOrNull()?.let { currentFirstLine = LayoutUnits.twipsToUnits(it) }
                                    val hStr = getXmlAttr(parser, "hanging")
                                    hStr?.toIntOrNull()?.let { currentFirstLine = -LayoutUnits.twipsToUnits(it) }
                                }
                                inStyle && inPPr && tagName == "jc" -> {
                                    currentJc = getXmlAttr(parser, "val")
                                }
                                inStyle && inPPr && tagName == "keeplines" -> { currentKeepTogether = docxOnOff(getXmlAttr(parser, "val")) }
                                inStyle && inPPr && tagName == "widowcontrol" -> { currentWidowControl = docxOnOff(getXmlAttr(parser, "val")) }
                                inStyle && inPPr && tagName == "tab" -> {
                                    parseDocxTab(parser)?.let { currentTabs = currentTabs.orEmpty() + it }
                                }
                                // Style-level numbering state: `w:numPr` inside `w:pPr` (ECMA-376 §17.9.18).
                                inStyle && inPPr && tagName == "numid" -> {
                                    currentNumId = getXmlAttr(parser, "val")?.toIntOrNull()
                                }
                                inStyle && inPPr && tagName == "ilvl" -> {
                                    currentNumIlvl = getXmlAttr(parser, "val")?.toIntOrNull()
                                }
                                inStyle && inPPr && tagName == "keepnext" -> {
                                    currentKeepNext = docxOnOff(getXmlAttr(parser, "val"))
                                }
                                inStyle && inPPr && tagName == "pagebreakbefore" -> {
                                    currentPageBreakBefore = docxOnOff(getXmlAttr(parser, "val"))
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when {
                                tagName == "docdefaults" -> {
                                    inDocDefaults = false
                                    inDocDefaultsRPr = false
                                    inDocDefaultsPPr = false
                                }
                                inDocDefaults && (tagName == "rprdefault" || tagName == "rpr") -> {
                                    inDocDefaultsRPr = false
                                }
                                inDocDefaults && (tagName == "pprdefault" || tagName == "ppr") -> {
                                    inDocDefaultsPPr = false
                                }
                                inStyle && tagName == "rpr" -> {
                                    inRPr = false
                                }
                                inStyle && tagName == "ppr" -> {
                                    inPPr = false
                                }
                                tagName == "style" -> {
                                    val styleId = currentStyleId
                                    if (!currentStyleIgnored && styleId != null) {
                                        // `NoList`, `DefaultParagraphFont` and `TableNormal` are
                                        // built-ins whose children Word ignores (ECMA-376 §17.7.4.17),
                                        // so their definitions contribute nothing but their identity.
                                        val childrenIgnored = styleId in STYLES_WITH_IGNORED_CHILDREN
                                        val styleType = currentStyleType ?: "paragraph"
                                        val sName = if (childrenIgnored) styleId else currentStyleName ?: styleId
                                        val catalogLevel = if (childrenIgnored) 0 else
                                            com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(sName)
                                        val level = when {
                                            childrenIgnored -> 1
                                            currentOutlineLvl != null -> currentOutlineLvl!! + 1
                                            catalogLevel > 0 -> catalogLevel
                                            else -> 1
                                        }
                                        val isHeading = !childrenIgnored && (catalogLevel > 0 || currentOutlineLvl != null)

                                        rawStylesMap[styleId] = DocxStyleMeta(
                                            styleId = styleId,
                                            name = sName,
                                            styleType = styleType,
                                            outlineLvl = if (childrenIgnored) null else currentOutlineLvl,
                                            isHeading = isHeading,
                                            headingLevel = level.coerceIn(1, 6),
                                            basedOn = if (childrenIgnored) null else currentBasedOn,
                                            link = if (childrenIgnored) null else currentLink,
                                            fontSizeSp = if (childrenIgnored) null else currentSz,
                                            fontSizeCsSp = if (childrenIgnored) null else currentSzCs,
                                            isBold = if (childrenIgnored) null else currentBold,
                                            isItalic = if (childrenIgnored) null else currentItalic,
                                            isUnderline = if (childrenIgnored) null else currentUnderline,
                                            fontFamily = if (childrenIgnored) null else currentFont,
                                            colorHex = if (childrenIgnored) null else currentColor,
                                            highlight = if (childrenIgnored) null else currentHighlight,
                                            isHidden = if (childrenIgnored) null else currentVanish,
                                            numId = if (childrenIgnored) null else currentNumId,
                                            numIlvl = if (childrenIgnored) null else currentNumIlvl,
                                            spaceBeforeUnits = if (childrenIgnored) null else currentBefore,
                                            spaceAfterUnits = if (childrenIgnored) null else currentAfter,
                                            lineHeightFactor = if (childrenIgnored) null else currentLineFactor,
                                            lineHeightExactUnits = if (childrenIgnored) null else currentLineExact,
                                            lineHeightMinimumUnits = if (childrenIgnored) null else currentLineMinimum,
                                            indentStartUnits = if (childrenIgnored) null else currentIndentStart,
                                            indentEndUnits = if (childrenIgnored) null else currentIndentEnd,
                                            firstLineIndentUnits = if (childrenIgnored) null else currentFirstLine,
                                            keepWithNext = if (childrenIgnored) null else currentKeepNext,
                                            pageBreakBefore = if (childrenIgnored) null else currentPageBreakBefore,
                                            alignment = if (childrenIgnored) null else currentJc?.replaceFirstChar { it.uppercase() },
                                            keepTogether = if (childrenIgnored) null else currentKeepTogether,
                                            widowControl = if (childrenIgnored) null else currentWidowControl,
                                            tabStops = if (childrenIgnored) null else currentTabs
                                        )
                                    }
                                    inStyle = false
                                    currentStyleIgnored = false
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                // `w:basedOn` chain, leaf first. Resolution is name-aware because
                // some writers point `w:basedOn` at the style *name* ("Normal")
                // rather than the id.
                fun chainOf(meta: DocxStyleMeta): List<DocxStyleMeta> {
                    val chain = mutableListOf<DocxStyleMeta>()
                    var curr: String? = meta.styleId
                    val seen = mutableSetOf<String>()
                    while (curr != null && seen.add(curr.lowercase(Locale.ROOT))) {
                        val st = rawStylesMap[curr]
                            ?: rawStylesMap.values.firstOrNull { it.styleId.equals(curr, ignoreCase = true) || it.name.equals(curr, ignoreCase = true) }
                            ?: break
                        chain.add(st)
                        curr = st.basedOn
                    }
                    return chain
                }

                // Nearest level that sets a property wins; `w:docDefaults` is the
                // base and anything nobody sets stays null for the caller.
                fun runPropsOf(chain: List<DocxStyleMeta>): DocxRunProps {
                    var props = DocxRunProps(
                        sizeSp = docDefaultSz,
                        sizeCsSp = docDefaultSzCs,
                        fontFamily = docDefaultFont,
                        bold = docDefaultBold,
                        italic = docDefaultItalic,
                        underline = docDefaultUnderline,
                        colorHex = docDefaultColor,
                        hidden = docDefaultVanish
                    )
                    for (i in chain.lastIndex downTo 0) {
                        val st = chain[i]
                        props = DocxRunProps(
                            sizeSp = st.fontSizeSp ?: props.sizeSp,
                            sizeCsSp = st.fontSizeCsSp ?: props.sizeCsSp,
                            fontFamily = st.fontFamily ?: props.fontFamily,
                            bold = st.isBold ?: props.bold,
                            italic = st.isItalic ?: props.italic,
                            underline = st.isUnderline ?: props.underline,
                            colorHex = st.colorHex ?: props.colorHex,
                            highlight = st.highlight ?: props.highlight,
                            hidden = st.isHidden ?: props.hidden
                        )
                    }
                    return props
                }

                // ECMA-376 §17.7.4.6 with the Word notes that bind it
                // ([MS-OI29500] §2.1.235(a)-(b), p.106): when two styles link to
                // the same target, Word ignores all but the last link, and a
                // singly linked pairing is read as bi-directional. Both notes are
                // implemented here: a later claimant wins its target, and a
                // character style whose own `w:link` points back at a paragraph
                // style pairs that paragraph style even without a forward link.
                val charTwinByParagraphStyle = run {
                    val claimantByTarget = mutableMapOf<String, String>()
                    for (st in rawStylesMap.values) {
                        if (st.styleType != "paragraph") continue
                        val target = st.link ?: continue
                        if (target.isBlank()) continue
                        claimantByTarget[target.lowercase(Locale.ROOT)] = st.styleId
                    }
                    val pairs = mutableMapOf<String, String>()
                    for ((target, claimant) in claimantByTarget) {
                        pairs[claimant.lowercase(Locale.ROOT)] = target
                    }
                    for (st in rawStylesMap.values) {
                        if (st.styleType != "character") continue
                        val back = st.link ?: continue
                        if (back.isBlank()) continue
                        val paragraph = rawStylesMap[back]
                            ?: rawStylesMap.values.firstOrNull { it.styleId.equals(back, ignoreCase = true) }
                            ?: continue
                        if (paragraph.styleType != "paragraph") continue
                        pairs.putIfAbsent(paragraph.styleId.lowercase(Locale.ROOT), st.styleId)
                    }
                    pairs
                }

                fun linkedCharacterStyle(meta: DocxStyleMeta): DocxStyleMeta? {
                    if (meta.styleType != "paragraph") return null
                    val explicit = charTwinByParagraphStyle[meta.styleId.lowercase(Locale.ROOT)]
                        ?.let { target ->
                            rawStylesMap[target]
                                ?: rawStylesMap.values.firstOrNull { it.styleId.equals(target, ignoreCase = true) }
                        }
                    if (explicit != null && explicit.styleType == "character") return explicit
                    // No `w:link` (or a dangling one): Word's linked-style naming
                    // convention is "<paragraph style name> Char", which is how
                    // Samples 1, 2, 5 and 6 pair a heading with its character twin.
                    val wanted = meta.name + " Char"
                    return rawStylesMap.values.firstOrNull {
                        it.styleType == "character" && it.name.equals(wanted, ignoreCase = true)
                    } ?: rawStylesMap.values.firstOrNull {
                        it.styleType == "character" && it.name.equals(meta.name + "Char", ignoreCase = true)
                    }
                }

                /** Only the properties this very style element declares. */
                fun ownRunProps(meta: DocxStyleMeta): DocxRunProps = DocxRunProps(
                    sizeSp = meta.fontSizeSp,
                    sizeCsSp = meta.fontSizeCsSp,
                    fontFamily = meta.fontFamily,
                    bold = meta.isBold,
                    italic = meta.isItalic,
                    underline = meta.isUnderline,
                    colorHex = meta.colorHex,
                    highlight = meta.highlight,
                    hidden = meta.isHidden
                )

                /**
                 * Precedence for a paragraph style's run properties, in the
                 * order the owner decision of 2026-10-06 fixes it: the style's
                 * **own** `w:rPr` first (Sample-2 and Sample-5 declare their own
                 * 14 pt level-1 size and win), then its linked character style,
                 * which is where M365 writes the built-in heading formatting,
                 * then the `w:basedOn` chain with `w:docDefaults` behind it.
                 * The chain is last because a parent style must not outrank the
                 * style's own character identity.
                 */
                fun mergeRunProps(chain: DocxRunProps, twin: DocxRunProps?, own: DocxRunProps): DocxRunProps =
                    DocxRunProps(
                        sizeSp = own.sizeSp ?: twin?.sizeSp ?: chain.sizeSp,
                        sizeCsSp = own.sizeCsSp ?: twin?.sizeCsSp ?: chain.sizeCsSp,
                        fontFamily = own.fontFamily ?: twin?.fontFamily ?: chain.fontFamily,
                        bold = own.bold ?: twin?.bold ?: chain.bold,
                        italic = own.italic ?: twin?.italic ?: chain.italic,
                        underline = own.underline ?: twin?.underline ?: chain.underline,
                        colorHex = own.colorHex ?: twin?.colorHex ?: chain.colorHex,
                        highlight = own.highlight ?: twin?.highlight ?: chain.highlight,
                        hidden = own.hidden ?: twin?.hidden ?: chain.hidden
                    )

                fun cascadeDocxStyle(meta: DocxStyleMeta): ParagraphStyle {
                    val chain = chainOf(meta)
                    val twin = linkedCharacterStyle(meta)
                    val props = mergeRunProps(
                        chain = runPropsOf(chain),
                        twin = twin?.let { runPropsOf(chainOf(it)) },
                        own = ownRunProps(meta)
                    )

                    var before = docDefaultBefore ?: 0f
                    var after = docDefaultAfter ?: 0f
                    var lineFactor = docDefaultLineFactor ?: 1f
                    var lineExact = docDefaultLineExact
                    var lineMinimum = docDefaultLineMinimum
                    var indStart = docDefaultIndentStart ?: 0f
                    var indEnd = docDefaultIndentEnd ?: 0f
                    var firstLine = docDefaultFirstLine ?: 0f
                    var keepNext = docDefaultKeepNext ?: false
                    var pageBreakBefore = docDefaultPageBreakBefore ?: false
                    var alignment = "Left"
                    var keepTogether = docDefaultKeepTogether ?: false
                    var widowControl = docDefaultWidowControl ?: true
                    var tabs = docDefaultTabs.orEmpty()

                    for (i in chain.lastIndex downTo 0) {
                        val s = chain[i]
                        s.spaceBeforeUnits?.let { before = it }
                        s.spaceAfterUnits?.let { after = it }
                        // A newly declared mode replaces the entire inherited line-spacing
                        // choice. In particular, auto must clear an inherited exact/minimum.
                        if (s.lineHeightFactor != null || s.lineHeightExactUnits != null || s.lineHeightMinimumUnits != null) {
                            lineFactor = s.lineHeightFactor ?: 1f
                            lineExact = s.lineHeightExactUnits
                            lineMinimum = s.lineHeightMinimumUnits
                        }
                        s.indentStartUnits?.let { indStart = it }
                        s.indentEndUnits?.let { indEnd = it }
                        s.firstLineIndentUnits?.let { firstLine = it }
                        s.keepWithNext?.let { keepNext = it }
                        s.pageBreakBefore?.let { pageBreakBefore = it }
                        s.alignment?.let { alignment = it }
                        s.keepTogether?.let { keepTogether = it }
                        s.widowControl?.let { widowControl = it }
                        tabs = mergeTabs(tabs, s.tabStops)
                    }

                    return ParagraphStyle(
                        name = meta.name,
                        // `w:szCs` is the complex-script size and only stands in
                        // when no `w:sz` exists anywhere in the resolved chain.
                        fontSizeSp = props.sizeSp ?: props.sizeCsSp ?: 12f,
                        isBold = props.bold ?: false,
                        isItalic = props.italic ?: false,
                        isUnderline = props.underline ?: false,
                        colorHex = props.colorHex,
                        highlight = props.highlight,
                        isHidden = props.hidden ?: false,
                        alignment = alignment,
                        fontFamily = props.fontFamily,
                        parentStyleName = meta.basedOn,
                        spaceBeforeUnits = before,
                        spaceAfterUnits = after,
                        lineHeightFactor = lineFactor,
                        lineHeightExactUnits = lineExact,
                        lineHeightMinimumUnits = lineMinimum,
                        indentStartUnits = indStart,
                        indentEndUnits = indEnd,
                        firstLineIndentUnits = firstLine,
                        keepWithNext = keepNext,
                        pageBreakBefore = pageBreakBefore,
                        collapseSpacing = true, keepTogether = keepTogether,
                        orphans = if (widowControl) 2 else 1, widows = if (widowControl) 2 else 1,
                        tabStops = tabs, defaultTabIntervalUnits = tabInterval
                    )
                }

                fun cascadeCharacterStyle(meta: DocxStyleMeta): CharacterStyle {
                    // A character style resolves from its own element and its own
                    // `w:basedOn` chain; its `w:link` points back at the paragraph
                    // style it is paired with and is not a formatting source here.
                    val props = mergeRunProps(
                        chain = runPropsOf(chainOf(meta)),
                        twin = null,
                        own = ownRunProps(meta)
                    )
                    return CharacterStyle(
                        name = meta.name,
                        fontSizeSp = props.sizeSp ?: props.sizeCsSp,
                        isBold = props.bold == true,
                        isItalic = props.italic == true,
                        isUnderline = props.underline == true,
                        colorHex = props.colorHex,
                        highlight = props.highlight,
                        isHidden = props.hidden == true,
                        fontFamily = props.fontFamily,
                        parentStyleName = meta.basedOn
                    )
                }

                // Effective numbering of a style chain: the nearest level that sets
                // `w:numPr` wins, and `numId 0` is a suppression rather than a
                // definition ([MS-OI29500] §17.9.18, p.125). Descendants inherit it,
                // which is what makes Sample-4 `Judul4` inherit `Judul3`'s suppression.
                fun resolveNumbering(meta: DocxStyleMeta): DocxNumberingRef? {
                    for (st in chainOf(meta)) {
                        val numId = st.numId ?: continue
                        val ilvl = st.numIlvl ?: 0
                        return if (numId == 0) {
                            DocxNumberingRef(numId = 0, ilvl = ilvl, suppressed = true, fromStyle = true)
                        } else {
                            DocxNumberingRef(numId = numId, ilvl = ilvl, fromStyle = true)
                        }
                    }
                    return null
                }

                val stylesMetaMap = mutableMapOf<String, DocxStyleMeta>()
                val resolvedParagraphStyles = mutableMapOf<String, ParagraphStyle>()
                val resolvedCharacterStyles = mutableMapOf<String, CharacterStyle>()
                val numberingByStyle = mutableMapOf<String, DocxNumberingRef>()

                for ((id, meta) in rawStylesMap) {
                    val keys = listOf(id, id.lowercase(Locale.ROOT), meta.name, meta.name.lowercase(Locale.ROOT))
                    when (meta.styleType) {
                        "character" -> {
                            val cascaded = cascadeCharacterStyle(meta)
                            for (key in keys) resolvedCharacterStyles[key] = cascaded
                        }
                        "paragraph" -> {
                            for (key in keys) stylesMetaMap[key] = meta
                            val cascaded = cascadeDocxStyle(meta)
                            for (key in keys) resolvedParagraphStyles[key] = cascaded
                            resolveNumbering(meta)?.let { ref ->
                                for (key in keys) numberingByStyle[key] = ref
                            }
                        }
                        // `table` and `numbering` styles are recorded in the meta map
                        // only; Plans 8B and 9 own their consumers.
                        else -> for (key in keys) stylesMetaMap[key] = meta
                    }
                }

                val normalMeta = rawStylesMap.values.firstOrNull { it.styleId.equals("Normal", ignoreCase = true) || it.name.equals("Normal", ignoreCase = true) }
                val defaultParaStyle = normalMeta?.let { cascadeDocxStyle(it) }
                    ?: ParagraphStyle(
                        name = "Normal",
                        fontSizeSp = docDefaultSz ?: docDefaultSzCs ?: 12f,
                        fontFamily = docDefaultFont,
                        colorHex = docDefaultColor,
                        isHidden = docDefaultVanish ?: false,
                        spaceBeforeUnits = docDefaultBefore ?: 0f,
                        spaceAfterUnits = docDefaultAfter ?: 0f,
                        lineHeightFactor = docDefaultLineFactor ?: 1f,
                        lineHeightExactUnits = docDefaultLineExact,
                        lineHeightMinimumUnits = docDefaultLineMinimum,
                        keepWithNext = docDefaultKeepNext ?: false,
                        pageBreakBefore = docDefaultPageBreakBefore ?: false,
                        indentStartUnits = docDefaultIndentStart ?: 0f,
                        indentEndUnits = docDefaultIndentEnd ?: 0f,
                        firstLineIndentUnits = docDefaultFirstLine ?: 0f,
                        collapseSpacing = true, keepTogether = docDefaultKeepTogether ?: false,
                        orphans = if (docDefaultWidowControl != false) 2 else 1,
                        widows = if (docDefaultWidowControl != false) 2 else 1,
                        tabStops = docDefaultTabs.orEmpty(), defaultTabIntervalUnits = tabInterval
                    )

                return DocxStylesParseResult(
                    stylesMetaMap = stylesMetaMap,
                    paragraphStyles = resolvedParagraphStyles,
                    characterStyles = resolvedCharacterStyles,
                    numberingByStyle = numberingByStyle,
                    defaultParagraphStyle = defaultParaStyle
                )
            }
        } catch (e: Exception) {
            // Graceful fallback
        }
        return DocxStylesParseResult(defaultParagraphStyle = ParagraphStyle("Normal",
            collapseSpacing = true, defaultTabIntervalUnits = tabInterval))
    }

    private val cacheRepository = com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository(context)
    private val imageExtractor = DocxImageExtractor(context)

    private val _parsingProgress = MutableLiveData<ParsingProgress>(ParsingProgress(0, "Loading document..."))
    val parsingProgress: LiveData<ParsingProgress> get() = _parsingProgress

    /**
     * Synchronous stage hook, called on the parsing thread at the moment each
     * stage starts. LiveData coalesces rapid posts, so tests and diagnostics
     * that need the full sequence read it here instead.
     */
    @Volatile
    var stageListener: ((LoadingStage) -> Unit)? = null

    private fun announce(stage: LoadingStage) {
        stageListener?.invoke(stage)
        _parsingProgress.postValue(
            ParsingProgress(stage.percent, context.getString(stage.messageRes), stage = stage)
        )
    }

    /**
     * Validates XML structure integrity against ODF/OOXML standard schema expectations.
     * Logs structural anomalies and warnings to crash.log via DocumentParsingLogger.
     */
    fun validateXmlSchema(
        xmlContent: String,
        isOdt: Boolean,
        isDocx: Boolean,
        fileName: String,
        isOds: Boolean = false,
        isXlsx: Boolean = false,
        isOdp: Boolean = false,
        isPptx: Boolean = false
    ): SchemaValidationResult {
        val warnings = mutableListOf<String>()
        if (xmlContent.isBlank()) {
            val errorMsg = "XML content is empty or corrupted in $fileName."
            DocumentParsingLogger.logMalformedXml(context, fileName, errorMsg)
            return SchemaValidationResult.Invalid(errorMsg)
        }

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType
            var rootTag: String? = null
            var hasBody = false
            var tagCount = 0

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name ?: ""
                    val tagNameLower = tagName.lowercase(Locale.ROOT)
                    tagCount++

                    if (rootTag == null) {
                        rootTag = tagName
                        if ((isOdt || isOds || isOdp) && !tagNameLower.contains("document") && !tagNameLower.contains("content") && !tagNameLower.contains("presentation") && !tagNameLower.contains("page")) {
                            val formatName = if (isOds) "ODS" else if (isOdp) "ODP" else "ODT"
                            val msg = "$formatName Schema Deviation: Root tag <$tagName> does not match ODF standard."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "$formatName Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isDocx && !tagNameLower.endsWith("document")) {
                            val msg = "DOCX Schema Deviation: Root tag <$tagName> does not match OpenXML standard (<w:document>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "DOCX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isXlsx && !tagNameLower.contains("worksheet") && !tagNameLower.contains("workbook")) {
                            val msg = "XLSX Schema Deviation: Root tag <$tagName> does not match OpenXML standard (<worksheet>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "XLSX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        } else if (isPptx && !tagNameLower.contains("sld") && !tagNameLower.contains("presentation")) {
                            val msg = "PPTX Schema Deviation: Root tag <$tagName> does not match OpenXML Presentation standard (<p:sld>)."
                            warnings.add(msg)
                            DocumentParsingLogger.logError(
                                context = context,
                                tag = "PPTX Schema Violation",
                                exceptionType = "XmlSchemaViolationWarning",
                                message = msg,
                                details = "File: $fileName, RootTag: <$tagName>"
                            )
                        }
                    }

                    if (tagNameLower.endsWith("body") || tagNameLower.endsWith("text") || tagNameLower.endsWith("spreadsheet") || tagNameLower.endsWith("sheetdata") || tagNameLower.endsWith("worksheet") || tagNameLower.contains("sld") || tagNameLower.contains("presentation") || tagNameLower.contains("page")) {
                        hasBody = true
                    }
                }
                eventType = parser.next()
            }

            if (tagCount == 0) {
                val errorMsg = "XML structure contains no valid tags in file $fileName"
                DocumentParsingLogger.logMalformedXml(context, fileName, errorMsg)
                return SchemaValidationResult.Invalid(errorMsg)
            }

            if (isOdt && !hasBody) {
                val msg = "ODT Structural Deviation: Missing <office:body> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "ODT Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isOds && !hasBody) {
                val msg = "ODS Structural Deviation: Missing <office:body> or <office:spreadsheet> element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "ODS Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isDocx && !hasBody) {
                val msg = "DOCX Structural Deviation: Missing <w:body> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "DOCX Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            if (isXlsx && !hasBody) {
                val msg = "XLSX Structural Deviation: Missing <worksheet> or <sheetData> container element in $fileName."
                warnings.add(msg)
                DocumentParsingLogger.logError(
                    context = context,
                    tag = "XLSX Structural Anomaly",
                    exceptionType = "XmlStructureAnomalyWarning",
                    message = msg,
                    details = "File: $fileName"
                )
            }

            return SchemaValidationResult.Valid

        } catch (e: Exception) {
            val errorMsg = "XML Schema Validation Failed: ${e.localizedMessage ?: "Syntax error"}"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = fileName,
                errorMsg = errorMsg,
                cause = e
            )
            return SchemaValidationResult.Invalid(errorMsg, warnings)
        }
    }

    /**
     * Pre-processing sanitization that removes invalid non-UTF-8 characters,
     * illegal control characters, and Unicode replacement characters (\uFFFD) from XML structure.
     * Logs any removed characters to the Crash Log system via DocumentParsingLogger.
     */
    fun sanitizeXmlContent(rawXml: String, fileName: String): String {
        if (rawXml.isEmpty()) return rawXml

        val sanitizedBuilder = java.lang.StringBuilder(rawXml.length)
        var removedCount = 0
        val removedSampleHex = mutableListOf<String>()

        var i = 0
        val length = rawXml.length
        while (i < length) {
            val codePoint = rawXml.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            // Valid XML 1.0 character specification:
            // #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] | [#x10000-#x10FFFF]
            val isValidXmlChar = (codePoint == 0x9) ||
                    (codePoint == 0xA) ||
                    (codePoint == 0xD) ||
                    (codePoint in 0x20..0xD7FF) ||
                    (codePoint in 0xE000..0xFFFD && codePoint != 0xFFFD) || // Exclude \uFFFD (bad UTF-8 byte)
                    (codePoint in 0x10000..0x10FFFF)

            if (isValidXmlChar) {
                sanitizedBuilder.appendCodePoint(codePoint)
            } else {
                removedCount++
                if (removedSampleHex.size < 5) {
                    removedSampleHex.add(String.format("U+%04X", codePoint))
                }
            }
            i += charCount
        }

        if (removedCount > 0) {
            val samples = removedSampleHex.joinToString(", ")
            DocumentParsingLogger.logError(
                context = context,
                tag = "XmlSanitization",
                exceptionType = "NonUtf8XmlSanitized",
                message = "Sanitized $removedCount invalid non-UTF-8 or illegal XML character(s) from $fileName.",
                details = "File: $fileName, Removed Count: $removedCount, Sample Hex: [$samples]"
            )
        }

        return sanitizedBuilder.toString()
    }

    /**
     * Extracts raw 'content.xml' from ODT/ODS file, 'word/document.xml' from DOCX file,
     * or 'xl/worksheets/sheet1.xml' from XLSX file using standard Java ZIP input stream.
     */
    suspend fun extractXmlContent(file: File): String = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext ""
        val isOdf = file.name.endsWith(".odt", ignoreCase = true) ||
                file.name.endsWith(".ods", ignoreCase = true) ||
                file.name.endsWith(".odp", ignoreCase = true) ||
                file.name.endsWith(".ott", ignoreCase = true) ||
                file.name.endsWith(".ots", ignoreCase = true) ||
                file.name.endsWith(".otp", ignoreCase = true)
        val isXlsx = file.name.endsWith(".xlsx", ignoreCase = true) ||
                file.name.endsWith(".xlsm", ignoreCase = true)
        val isPptx = file.name.endsWith(".pptx", ignoreCase = true) ||
                file.name.endsWith(".pptm", ignoreCase = true)

        val targetEntry = if (isOdf) "content.xml" else if (isXlsx) "xl/worksheets/sheet1.xml" else if (isPptx) "ppt/slides/slide1.xml" else "word/document.xml"

        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                var fallbackSheetContent: String? = null
                var fallbackSlideContent: String? = null
                var contentXmlFound: String? = null
                var wordDocumentFound: String? = null
                val slideContents = mutableListOf<String>()

                while (entry != null) {
                    val name = entry.name
                    if (name == targetEntry) {
                        val rawContent = zip.readCappedBytes().toString(Charsets.UTF_8)
                        return@withContext sanitizeXmlContent(rawContent, file.name)
                    }
                    if (name == "content.xml") {
                        contentXmlFound = zip.readCappedBytes().toString(Charsets.UTF_8)
                    } else if (name == "word/document.xml") {
                        wordDocumentFound = zip.readCappedBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith("xl/worksheets/sheet") && fallbackSheetContent == null) {
                        fallbackSheetContent = zip.readCappedBytes().toString(Charsets.UTF_8)
                    } else if (name.startsWith("ppt/slides/slide")) {
                        slideContents.add(zip.readCappedBytes().toString(Charsets.UTF_8))
                    } else if (name == "ppt/presentation.xml" && fallbackSlideContent == null) {
                        fallbackSlideContent = zip.readCappedBytes().toString(Charsets.UTF_8)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }

                if (slideContents.isNotEmpty()) {
                    if (slideContents.size == 1) {
                        return@withContext sanitizeXmlContent(slideContents[0], file.name)
                    } else {
                        val composite = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<presentation>\n")
                        for (s in slideContents) {
                            composite.append(s).append("\n")
                        }
                        composite.append("</presentation>")
                        return@withContext sanitizeXmlContent(composite.toString(), file.name)
                    }
                }
                if (contentXmlFound != null) return@withContext sanitizeXmlContent(contentXmlFound, file.name)
                if (wordDocumentFound != null) return@withContext sanitizeXmlContent(wordDocumentFound, file.name)
                if (fallbackSheetContent != null) return@withContext sanitizeXmlContent(fallbackSheetContent, file.name)
                if (fallbackSlideContent != null) return@withContext sanitizeXmlContent(fallbackSlideContent, file.name)
            }
        } catch (e: Exception) {
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = "Failed to extract XML content from ZIP container: ${e.localizedMessage}",
                cause = e
            )
        }

        if (isOdf) {
            val nameLower = file.name.lowercase(Locale.ROOT)
            val fallbackXml = when {
                nameLower.endsWith(".odt") || nameLower.endsWith(".ott") -> """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:draw:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" xmlns:xlink="http://www.w3.org/1999/xlink" office:version="1.2">
                      <office:automatic-styles>
                        <style:style style:name="P1" style:family="paragraph" style:parent-style-name="Standard"/>
                      </office:automatic-styles>
                      <office:body>
                        <office:text>
                          <text:p text:style-name="P1"/>
                        </office:text>
                      </office:body>
                    </office:document-content>
                """.trimIndent()
                nameLower.endsWith(".ods") || nameLower.endsWith(".ots") -> """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0" xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0" office:version="1.2">
                      <office:body>
                        <office:spreadsheet>
                          <table:table table:name="Sheet1">
                            <table:table-row>
                              <table:table-cell office:value-type="string">
                                <text:p/>
                              </table:table-cell>
                            </table:table-row>
                          </table:table>
                        </office:spreadsheet>
                      </office:body>
                    </office:document-content>
                """.trimIndent()
                else -> """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0" xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:draw:1.0" xmlns:presentation="urn:oasis:names:tc:opendocument:xmlns:presentation:1.0" xmlns:xlink="http://www.w3.org/1999/xlink" office:version="1.2">
                      <office:body>
                        <office:presentation>
                          <draw:page draw:name="Slide 1"/>
                        </office:presentation>
                      </office:body>
                    </office:document-content>
                """.trimIndent()
            }
            return@withContext fallbackXml
        }

        return@withContext ""
    }

    private suspend fun refreshCachedMedia(file: File, document: OfficeParsedDocument): OfficeParsedDocument {
        if (document.elements.none { it is OfficeDocumentElement.ImageElement }) return document

        val media = if (document.isOdt) {
            imageExtractor.extractImagesFromOdt(file)
        } else {
            imageExtractor.extractImagesFromDocx(file)
        }
        val elements = document.elements.map { element ->
            if (element !is OfficeDocumentElement.ImageElement) return@map element
            val path = element.imagePath
            val resolved = media[path]
                ?: media[path.lowercase(Locale.ROOT)]
                ?: media[path.substringAfterLast('/')]
                ?: media[path.substringAfterLast('/').lowercase(Locale.ROOT)]
            element.copy(imageFile = resolved ?: element.imageFile?.takeIf { it.isFile })
        }
        return if (media == document.extractedImages && elements == document.elements) document
        else document.copy(elements = elements, extractedImages = media)
    }

    /**
     * Parses the ODT, ODS, DOCX, or XLSX file into structured OfficeParsedDocument model
     * mapping paragraphs, headings, list items, tables, and images.
     */
    suspend fun parseDocument(file: File, bypassCache: Boolean = false): OfficeParsedDocument = withContext(Dispatchers.IO) {
        // Fast path: Check in-memory rich parsed document cache first unless bypassed
        if (!bypassCache) {
            val memCached = inMemoryParsedDocCache[file.absolutePath]
            if (memCached != null && memCached.lastModified == file.lastModified() &&
                memCached.fileLength == file.length() && !memCached.document.isParsingFailed) {
                announce(LoadingStage.CACHED)
                val cachedDocument = refreshCachedMedia(file, memCached.document)
                if (cachedDocument !== memCached.document) {
                    inMemoryParsedDocCache[file.absolutePath] = memCached.copy(document = cachedDocument)
                }
                return@withContext cachedDocument
            }
        }

        announce(LoadingStage.OPENING_PACKAGE)

        val isOdt = file.name.endsWith(".odt", ignoreCase = true) || file.name.endsWith(".ott", ignoreCase = true)
        val isOds = file.name.endsWith(".ods", ignoreCase = true) || file.name.endsWith(".ots", ignoreCase = true)
        val isOdp = file.name.endsWith(".odp", ignoreCase = true) || file.name.endsWith(".otp", ignoreCase = true)
        val isDocx = file.name.endsWith(".docx", ignoreCase = true) || file.name.endsWith(".doc", ignoreCase = true)
        val isXlsx = file.name.endsWith(".xlsx", ignoreCase = true) || file.name.endsWith(".xlsm", ignoreCase = true)
        val isPptx = file.name.endsWith(".pptx", ignoreCase = true) || file.name.endsWith(".pptm", ignoreCase = true)

        val xmlContent = extractXmlContent(file)
        val detectedPptx = isPptx || xmlContent.contains("<p:sld") || xmlContent.contains("<p:presentation")
        val detectedOdp = isOdp || xmlContent.contains("<office:presentation") || xmlContent.contains("<draw:page")

        announce(LoadingStage.VALIDATING)

        val validation = validateXmlSchema(
            xmlContent = xmlContent,
            isOdt = isOdt,
            isDocx = isDocx,
            fileName = file.name,
            isOds = isOds,
            isXlsx = isXlsx,
            isOdp = detectedOdp,
            isPptx = detectedPptx
        )
        if (validation is SchemaValidationResult.Invalid) {
            val failProgress = ParsingProgress(
                percentage = 0,
                statusMessage = context.getString(com.example.R.string.doc_open_failed_title),
                isFailed = true,
                errorMessage = validation.reason
            )
            _parsingProgress.postValue(failProgress)
            return@withContext OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = emptyMap(),
                isOdt = isOdt,
                isDocx = isDocx,
                isOds = isOds,
                isXlsx = isXlsx,
                isOdp = detectedOdp,
                isPptx = detectedPptx,
                isParsingFailed = true,
                failureReason = validation.reason
            )
        }

        announce(LoadingStage.EXTRACTING_MEDIA)
        val extractedImages = if (isOdt) {
            imageExtractor.extractImagesFromOdt(file)
        } else {
            imageExtractor.extractImagesFromDocx(file)
        }

        announce(LoadingStage.READING_STYLES)

        if (isOdt) {
            val parsedDoc = OdtImportPipeline(AndroidOdfImportDiagnostics(context)).parse(
                file = file,
                extractedImages = extractedImages,
                onStylesParsed = { announce(LoadingStage.READING_BODY) }
            )
            if (!parsedDoc.isParsingFailed) {
                inMemoryParsedDocCache[file.absolutePath] = ParsedCacheEntry(file.lastModified(), file.length(), parsedDoc)
                cacheRepository.saveCachedDocument(file, parsedDoc)
            }
            return@withContext parsedDoc
        } else if (isOds || detectedOdp) {
            val odfImport = com.makerandreas.papirusoffice.data.odf.SvXMLImport(
                extractedImages,
                AndroidOdfImportDiagnostics(context)
            )
            val parsedDoc = odfImport.parseOdfXml(
                xmlContent = xmlContent,
                fileName = file.name,
                stylesXmlContent = null,
                isOdt = false,
                isOds = isOds,
                isOdp = detectedOdp,
                onStylesParsed = { announce(LoadingStage.READING_BODY) }
            )
            if (!parsedDoc.isParsingFailed) {
                inMemoryParsedDocCache[file.absolutePath] = ParsedCacheEntry(file.lastModified(), file.length(), parsedDoc)
                cacheRepository.saveCachedDocument(file, parsedDoc)
                return@withContext parsedDoc
            }
        }

        if (isXlsx) {
            val parsedDoc = parseXlsxDocument(file, extractedImages)
            inMemoryParsedDocCache[file.absolutePath] = ParsedCacheEntry(file.lastModified(), file.length(), parsedDoc)
            cacheRepository.saveCachedDocument(file, parsedDoc)
            return@withContext parsedDoc
        }

        if (detectedPptx) {
            val parsedDoc = parsePptxDocument(file, extractedImages)
            inMemoryParsedDocCache[file.absolutePath] = ParsedCacheEntry(file.lastModified(), file.length(), parsedDoc)
            cacheRepository.saveCachedDocument(file, parsedDoc)
            return@withContext parsedDoc
        }

        val elements = mutableListOf<OfficeDocumentElement>()
        val plainTextBuilder = StringBuilder()
        val sectionStarts = mutableListOf<SectionStart>()
        var currentSectionStart = 0
        var sectionKind = SectionStartKind.NEXT_PAGE
        var inSectionProperties = false
        var inDocxText = false
        val inlinePageBreaks = mutableListOf<Int>()
        val docxStylesResult = if (isDocx) extractDocxStyles(file) else DocxStylesParseResult()
        val docxStylesMap = docxStylesResult.stylesMetaMap
        val docxNumberingMap = docxStylesResult.numberingByStyle
        val docxParagraphStyles = docxStylesResult.paragraphStyles.toMutableMap()
        val docxDefaultParagraphStyle = docxStylesResult.defaultParagraphStyle
        val docxRelsMap = if (isDocx) extractDocxRelationships(file) else emptyMap()
        val generatedStyles = mutableMapOf<String, ParagraphStyle>()
        announce(LoadingStage.READING_BODY)

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(ByteArrayInputStream(xmlContent.toByteArray(Charsets.UTF_8)), "UTF-8")

            var eventType = parser.eventType
            var inParagraph = false
            var inHeading = false
            var headingLevel = 1
            var inTable = false
            var currentTableName: String? = null
            var lastGraphicName: String? = null
            var pendingDocxImageExtent: Pair<Float, Float>? = null
            var lastDocxImageElementIndex: Int? = null
            val currentRows = mutableListOf<TableRow>()
            val currentCells = mutableListOf<TableCell>()
            val currentCellParagraphs = mutableListOf<OfficeDocumentElement.Paragraph>()

            val currentText = StringBuilder()
            val currentRuns = mutableListOf<TextRun>()
            val currentRunText = StringBuilder()
            var isBold = false
            var isItalic = false
            var isUnderline = false

            var inPPr = false
            var currentPStyle: String? = null
            var hasDirectPPr = false
            var directBefore: Float? = null
            var directAfter: Float? = null
            var directLineFactor: Float? = null
            var directLineExact: Float? = null
            var directLineMinimum: Float? = null
            var directIndentStart: Float? = null
            var directIndentEnd: Float? = null
            var directFirstLine: Float? = null
            var directKeepNext: Boolean? = null
            var directPageBreakBefore: Boolean? = null
            var directKeepTogether: Boolean? = null
            var directWidowControl: Boolean? = null
            var directTabs: List<ParagraphTabStop>? = null
            var directJc: String? = null
            var directNumId: Int? = null
            var directNumIlvl: Int? = null
            var inNumPr = false
            var paraHasSectPr = false

            // Standard supported tag set for warning/unsupported tag diagnostic logging
            val supportedTags = setOf(
                "p", "h", "text:p", "text:h", "w:p", "w:h", "w:t", "t", "a:t", "a:p", "a:r", "text:span", "w:r",
                "table", "table:table", "w:tbl", "table:table-row", "w:tr", "table:table-cell", "w:tc",
                "text:list-item", "text:list", "w:numpr", "text:line-break", "w:br", "w:cr",
                "text:tab", "w:tab", "text:s", "s", "draw:frame", "draw:image", "w:drawing", "wp:inline",
                "document", "office:document-content", "office:body", "office:text", "office:spreadsheet", "office:presentation",
                "draw:page", "draw:text-box", "p:sld", "p:sp", "p:txbody", "p:presentation", "p:sldid", "p:sldidlst",
                "table:table-column", "table:table-header-rows", "table:covered-table-cell",
                "office:value", "office:value-type", "table:number-columns-repeated", "table:number-rows-repeated",
                "worksheet", "sheetdata", "row", "c", "v", "f", "t", "is", "r", "inlinestr", "dimension", "cols", "col",
                "sheetviews", "sheetview", "selection", "pagemargins", "[content_types].xml", "_rels/.rels",
                "w:document", "w:body", "w:pPr", "w:rPr", "w:b", "w:i", "w:u", "style:style", "meta-inf/manifest.xml", "styles.xml",
                "w:lastrenderedpagebreak", "w:sectpr", "text:soft-page-break"
            )

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name ?: ""
                        val nameLower = name.lowercase(Locale.ROOT)
                        val tagLocal = nameLower.substringAfter(':')

                        // Check for unknown or custom XML tags to log diagnostic warnings
                        if (name.isNotEmpty() && !supportedTags.contains(name) && !supportedTags.contains(nameLower)) {
                            if (name.contains(":") && !name.startsWith("office:") && !name.startsWith("manifest:")) {
                                val attrMap = mutableMapOf<String, String>()
                                for (i in 0 until parser.attributeCount) {
                                    attrMap[parser.getAttributeName(i)] = parser.getAttributeValue(i)
                                }
                                DocumentParsingLogger.logUnsupportedTag(
                                    context = context,
                                    fileName = file.name,
                                    tagName = name,
                                    attributes = attrMap
                                )
                            }
                        }

                        when {
                            // DOCX drawing sizes are EMUs. wp:extent is the
                            // authoritative inline/anchor box; a:ext is a
                            // fallback for legacy drawing markup when present.
                            isDocx && (tagLocal == "extent" || tagLocal == "ext") -> {
                                val extent = parseDocxImageExtent(parser)
                                if (extent != null) {
                                    val recentIndex = lastDocxImageElementIndex
                                    val recentImage = recentIndex?.let { elements.getOrNull(it) as? OfficeDocumentElement.ImageElement }
                                    when {
                                        tagLocal == "extent" -> pendingDocxImageExtent = extent
                                        recentImage != null && recentImage.widthDp <= 0f && recentImage.heightDp <= 0f -> {
                                            elements[recentIndex!!] = recentImage.copy(widthDp = extent.first, heightDp = extent.second)
                                        }
                                        recentImage == null && pendingDocxImageExtent == null -> pendingDocxImageExtent = extent
                                    }
                                }
                            }

                            tagLocal == "drawing" || tagLocal == "pict" || tagLocal == "pic" || tagLocal == "shape" -> {
                                lastDocxImageElementIndex = null
                            }

                            // Headings
                            tagLocal == "h" -> {
                                inHeading = true
                                val outlineLevel = getXmlAttr(parser, "outline-level")
                                headingLevel = outlineLevel?.toIntOrNull() ?: 1
                                currentText.clear()
                            }

                            // Paragraphs
                            tagLocal == "p" -> {
                                if (isDocx) {
                                    pendingDocxImageExtent = null
                                    lastDocxImageElementIndex = null
                                }
                                inParagraph = true
                                inHeading = false
                                headingLevel = 1
                                currentText.clear()
                                currentRuns.clear()
                                inPPr = false
                                currentPStyle = null
                                hasDirectPPr = false
                                directBefore = null
                                directAfter = null
                                directLineFactor = null
                                directLineExact = null
                                directLineMinimum = null
                                directIndentStart = null
                                directIndentEnd = null
                                directFirstLine = null
                                directKeepNext = null
                                directPageBreakBefore = null
                                directKeepTogether = null
                                directWidowControl = null
                                directTabs = null
                                directJc = null
                                directNumId = null
                                directNumIlvl = null
                                paraHasSectPr = false
                                inlinePageBreaks.clear()
                            }

                            tagLocal == "ppr" -> {
                                inPPr = true
                            }

                            // DOCX / Word Style & Outline Level detection. Heading
                            // classification comes from the style table (`w:outlineLvl`,
                            // style name) rather than from guessing at the id shape: the
                            // checked-in fixtures use localised ids like `Judul1`, and an
                            // id-shape regex matched none of them (audit-019 section 3.2).
                            tagLocal == "pstyle" -> {
                                val styleVal = getXmlAttr(parser, "val") ?: ""
                                currentPStyle = styleVal
                                val meta = docxStylesMap[styleVal.lowercase(Locale.ROOT)]
                                if (meta != null && meta.isHeading) {
                                    inHeading = true
                                    headingLevel = meta.headingLevel
                                } else {
                                    val level = com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog.headingLevelFromStyleName(styleVal)
                                    if (level > 0) {
                                        inHeading = true
                                        headingLevel = level
                                    }
                                }
                            }
                            tagLocal == "outlinelvl" -> {
                                val lvlVal = getXmlAttr(parser, "val")
                                val parsedLvl = lvlVal?.toIntOrNull()
                                if (parsedLvl != null) {
                                    inHeading = true
                                    headingLevel = parsedLvl + 1
                                }
                            }

                            inPPr && tagLocal == "spacing" -> {
                                val bStr = getXmlAttr(parser, "before")
                                bStr?.toIntOrNull()?.let { directBefore = LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                                val aStr = getXmlAttr(parser, "after")
                                aStr?.toIntOrNull()?.let { directAfter = LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                                val lStr = getXmlAttr(parser, "line")
                                val lrStr = getXmlAttr(parser, "linerule") ?: "auto"
                                if (lStr != null) {
                                    val lv = lStr.toIntOrNull()
                                    if (lv != null) {
                                        directLineFactor = null
                                        directLineExact = null
                                        directLineMinimum = null
                                        when (lrStr.lowercase(Locale.ROOT)) {
                                            "exact" -> directLineExact = LayoutUnits.twipsToUnits(lv)
                                            "atleast" -> directLineMinimum = LayoutUnits.twipsToUnits(lv)
                                            else -> directLineFactor = LayoutUnits.lineTwentiethsToFactor(lv)
                                        }
                                        hasDirectPPr = true
                                    }
                                }
                            }

                            inPPr && tagLocal == "ind" -> {
                                val lStr = getXmlAttr(parser, "left") ?: getXmlAttr(parser, "start")
                                lStr?.toIntOrNull()?.let { directIndentStart = LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                                val rStr = getXmlAttr(parser, "right") ?: getXmlAttr(parser, "end")
                                rStr?.toIntOrNull()?.let { directIndentEnd = LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                                val flStr = getXmlAttr(parser, "firstline")
                                flStr?.toIntOrNull()?.let { directFirstLine = LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                                val hStr = getXmlAttr(parser, "hanging")
                                hStr?.toIntOrNull()?.let { directFirstLine = -LayoutUnits.twipsToUnits(it); hasDirectPPr = true }
                            }

                            inPPr && tagLocal == "jc" -> {
                                val jcVal = getXmlAttr(parser, "val")
                                if (jcVal != null) {
                                    directJc = jcVal
                                    hasDirectPPr = true
                                }
                            }

                            // Direct numbering state: `w:numPr` children (`w:ilvl`,
                            // `w:numId`). The state is read where the loop already sees
                            // the element, so the existing placeholder bullet text stays
                            // exactly as it is until Plan 8B renders real labels.
                            // Numbering is not a metric override, so it does not
                            // trigger the synthetic "inline-p-*" style the way direct
                            // spacing or indents do: the paragraph keeps its own style
                            // name and carries the reference in `numbering` instead.
                            inNumPr && tagLocal == "numid" -> {
                                directNumId = getXmlAttr(parser, "val")?.toIntOrNull()
                            }
                            inNumPr && tagLocal == "ilvl" -> {
                                directNumIlvl = getXmlAttr(parser, "val")?.toIntOrNull()
                            }

                            inPPr && tagLocal == "keeplines" -> { directKeepTogether = docxOnOff(getXmlAttr(parser, "val")); hasDirectPPr = true }
                            inPPr && tagLocal == "widowcontrol" -> { directWidowControl = docxOnOff(getXmlAttr(parser, "val")); hasDirectPPr = true }
                            inPPr && tagLocal == "tab" -> {
                                parseDocxTab(parser)?.let { directTabs = directTabs.orEmpty() + it; hasDirectPPr = true }
                            }
                            inPPr && tagLocal == "keepnext" -> {
                                directKeepNext = docxOnOff(getXmlAttr(parser, "val"))
                                hasDirectPPr = true
                            }

                            inPPr && tagLocal == "pagebreakbefore" -> {
                                directPageBreakBefore = docxOnOff(getXmlAttr(parser, "val"))
                                hasDirectPPr = true
                            }

                            isDocx && tagLocal == "t" -> { inDocxText = true }
                            tagLocal == "sectpr" -> {
                                paraHasSectPr = inPPr
                                inSectionProperties = true
                                sectionKind = SectionStartKind.NEXT_PAGE
                            }
                            inSectionProperties && tagLocal == "type" -> {
                                sectionKind = when (getXmlAttr(parser, "val")?.lowercase(Locale.ROOT)) {
                                    "continuous" -> SectionStartKind.CONTINUOUS
                                    "nextcolumn" -> SectionStartKind.NEXT_COLUMN
                                    "oddpage" -> SectionStartKind.ODD_PAGE
                                    "evenpage" -> SectionStartKind.EVEN_PAGE
                                    else -> SectionStartKind.NEXT_PAGE
                                }
                            }

                            // Page breaks
                            tagLocal == "lastrenderedpagebreak" || tagLocal == "soft-page-break" -> {
                                // Ignored: soft page breaks are layout hints, not authored breaks
                            }

                            // Text formatting
                            tagLocal == "b" || tagLocal == "text-properties" -> {
                                isBold = true
                            }
                            tagLocal == "i" -> {
                                isItalic = true
                            }
                            tagLocal == "u" -> {
                                isUnderline = true
                            }

                            // Tables
                            tagLocal == "table" || tagLocal == "tbl" -> {
                                inTable = true
                                currentRows.clear()
                                currentTableName = getXmlAttr(parser, "name")
                            }
                            tagLocal == "frame" -> {
                                lastGraphicName = getXmlAttr(parser, "name")
                            }
                            tagLocal == "docpr" -> {
                                lastGraphicName = getXmlAttr(parser, "name")
                            }
                            tagLocal == "tr" || tagLocal == "table-row" -> {
                                currentCells.clear()
                            }
                            tagLocal == "tc" || tagLocal == "table-cell" -> {
                                currentCellParagraphs.clear()
                                currentText.clear()
                            }

                            // Lists. The bullet text is the placeholder that Plan 8B
                            // replaces with the numbering definition's real label.
                            tagLocal == "list-item" || tagLocal == "numpr" -> {
                                if (tagLocal == "numpr") inNumPr = true
                                currentText.append("• ")
                            }

                            // Spaces & Tabs
                            tagLocal == "s" -> {
                                val countAttr = getXmlAttr(parser, "c")
                                val count = countAttr?.toIntOrNull() ?: 1
                                repeat(count) { currentText.append(" ") }
                            }
                            tagLocal == "tab" && !inPPr -> {
                                currentText.append("\t")
                            }
                            tagLocal == "line-break" || tagLocal == "br" || tagLocal == "cr" -> {
                                val brType = getXmlAttr(parser, "type")
                                if (brType?.equals("page", ignoreCase = true) == true) {
                                    inlinePageBreaks += currentText.length
                                } else {
                                    currentText.append("\n")
                                }
                            }

                            // Images: P0-3: lower-cased fallback so Pictures/ vs pictures/ still resolves
                            tagLocal == "image" -> {
                                val href = getXmlAttr(parser, "href")
                                if (!href.isNullOrBlank()) {
                                    val imgName = href.substringAfterLast("/")
                                    val hrefLower = href.lowercase(Locale.ROOT)
                                    val nameLower2 = imgName.lowercase(Locale.ROOT)
                                    val imgFile = extractedImages[imgName] ?: extractedImages[href]
                                        ?: extractedImages[nameLower2] ?: extractedImages[hrefLower]
                                        ?: extractedImages.values.firstOrNull { it.name.equals(imgName, ignoreCase = true) }
                                    elements.add(
                                        OfficeDocumentElement.ImageElement(
                                            imagePath = href,
                                            imageFile = imgFile,
                                            name = lastGraphicName
                                        )
                                    )
                                }
                            }
                            tagLocal == "blip" || tagLocal == "imagedata" -> {
                                val embedId = getXmlAttr(parser, "embed") ?: getXmlAttr(parser, "id") ?: getXmlAttr(parser, "href")
                                if (!embedId.isNullOrBlank()) {
                                    val target = docxRelsMap[embedId] ?: ""
                                    val imgName = target.substringAfterLast("/").ifBlank { embedId }
                                    val targetLower = target.lowercase(Locale.ROOT)
                                    val nameLower2 = imgName.lowercase(Locale.ROOT)
                                    val embedLower = embedId.lowercase(Locale.ROOT)
                                    val imgFile = extractedImages[imgName] ?: extractedImages[target] ?: extractedImages[embedId]
                                        ?: extractedImages[nameLower2] ?: extractedImages[targetLower] ?: extractedImages[embedLower]
                                        ?: extractedImages.values.firstOrNull { it.name.equals(imgName, ignoreCase = true) }
                                    val extent = pendingDocxImageExtent
                                    elements.add(
                                        OfficeDocumentElement.ImageElement(
                                            imagePath = target.ifBlank { embedId },
                                            imageFile = imgFile,
                                            widthDp = extent?.first ?: 0f,
                                            heightDp = extent?.second ?: 0f,
                                            name = lastGraphicName
                                        )
                                    )
                                    lastDocxImageElementIndex = elements.lastIndex
                                    pendingDocxImageExtent = null
                                    plainTextBuilder.append("\n[Image: ${imgName.ifBlank { embedId }}]\n\n")
                                }
                            }
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val txt = parser.text ?: ""
                        if (txt.isNotEmpty() && (!isDocx || inDocxText)) {
                            currentText.append(txt)
                            currentRunText.append(txt)
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val name = parser.name ?: ""
                        val nameLower = name.lowercase(Locale.ROOT)
                        val tagLocal = nameLower.substringAfter(':')

                        when {
                            tagLocal == "h" -> {
                                inHeading = false
                                val headingText = currentText.toString().trim()
                                if (headingText.isNotEmpty()) {
                                    elements.add(
                                        OfficeDocumentElement.Heading(
                                            text = headingText,
                                            level = headingLevel
                                        )
                                    )
                                    plainTextBuilder.append(headingText).append("\n\n")
                                }
                                currentText.clear()
                            }

                            isDocx && tagLocal == "t" -> { inDocxText = false }
                            tagLocal == "sectpr" -> {
                                sectionStarts += SectionStart(currentSectionStart, sectionKind)
                                inSectionProperties = false
                            }
                            tagLocal == "numpr" -> {
                                inNumPr = false
                            }

                            tagLocal == "ppr" -> {
                                inPPr = false
                                inNumPr = false
                            }

                            tagLocal == "p" -> {
                                inParagraph = false
                                inPPr = false
                                inNumPr = false
                                // Effective numbering: a direct `w:numPr` wins, `numId 0`
                                // suppresses, and otherwise the paragraph inherits the
                                // reference its style chain resolves to (Plan 8A carries
                                // this state; Plan 8B renders the label from it).
                                val styleNumbering = currentPStyle?.let { style ->
                                    docxNumberingMap[style] ?: docxNumberingMap[style.lowercase(Locale.ROOT)]
                                }
                                val paragraphNumbering: DocxNumberingRef? = when {
                                    // A direct `w:ilvl` alone re-levels the numbering the
                                    // paragraph inherits; `w:numId` stays from the style
                                    // chain (ECMA-376 Part 1 section 17.9.23).
                                    directNumId == null -> styleNumbering?.let { ref ->
                                        if (directNumIlvl != null) ref.copy(ilvl = directNumIlvl) else ref
                                    }
                                    directNumId == 0 -> DocxNumberingRef(numId = 0, ilvl = directNumIlvl ?: 0, suppressed = true)
                                    else -> DocxNumberingRef(numId = directNumId ?: 0, ilvl = directNumIlvl ?: 0)
                                }
                                val paraText = currentText.toString()
                                val resolvedStyleName: String? = when {
                                    hasDirectPPr -> {
                                        val baseStyle = currentPStyle?.let { docxParagraphStyles[it] ?: docxParagraphStyles[it.lowercase(Locale.ROOT)] }
                                            ?: docxDefaultParagraphStyle
                                            ?: ParagraphStyle("Normal")
                                        val syntheticName = "inline-p-${generatedStyles.size + 1}"
                                        val hasDirectLine = directLineFactor != null || directLineExact != null || directLineMinimum != null
                                        val syntheticStyle = baseStyle.copy(
                                            name = syntheticName,
                                            parentStyleName = currentPStyle ?: baseStyle.name,
                                            spaceBeforeUnits = directBefore ?: baseStyle.spaceBeforeUnits,
                                            spaceAfterUnits = directAfter ?: baseStyle.spaceAfterUnits,
                                            lineHeightFactor = if (hasDirectLine) directLineFactor ?: 1f else baseStyle.lineHeightFactor,
                                            lineHeightExactUnits = if (hasDirectLine) directLineExact else baseStyle.lineHeightExactUnits,
                                            lineHeightMinimumUnits = if (hasDirectLine) directLineMinimum else baseStyle.lineHeightMinimumUnits,
                                            indentStartUnits = directIndentStart ?: baseStyle.indentStartUnits,
                                            indentEndUnits = directIndentEnd ?: baseStyle.indentEndUnits,
                                            firstLineIndentUnits = directFirstLine ?: baseStyle.firstLineIndentUnits,
                                            keepWithNext = directKeepNext ?: baseStyle.keepWithNext,
                                            pageBreakBefore = directPageBreakBefore ?: baseStyle.pageBreakBefore,
                                            keepTogether = directKeepTogether ?: baseStyle.keepTogether,
                                            orphans = directWidowControl?.let { if (it) 2 else 1 } ?: baseStyle.orphans,
                                            widows = directWidowControl?.let { if (it) 2 else 1 } ?: baseStyle.widows,
                                            tabStops = mergeTabs(baseStyle.tabStops, directTabs),
                                            collapseSpacing = true,
                                            alignment = directJc?.replaceFirstChar { it.uppercase() } ?: baseStyle.alignment
                                        )
                                        generatedStyles[syntheticName] = syntheticStyle
                                        docxParagraphStyles[syntheticName] = syntheticStyle
                                        syntheticName
                                    }
                                    else -> currentPStyle
                                }

                                if (inHeading && !inTable) {
                                    val headingStyle = resolvedStyleName ?: "Heading $headingLevel"
                                    elements.add(
                                        OfficeDocumentElement.Heading(
                                            text = paraText,
                                            level = headingLevel,
                                            styleName = headingStyle,
                                            pageBreakOffsets = inlinePageBreaks.toList(),
                                            numbering = paragraphNumbering
                                        )
                                    )
                                    if (paraText.isNotEmpty()) {
                                        plainTextBuilder.append(paraText).append("\n\n")
                                    }
                                } else {
                                    val paragraphObj = OfficeDocumentElement.Paragraph(
                                        text = paraText,
                                        styleName = resolvedStyleName,
                                        pageBreakOffsets = inlinePageBreaks.toList(),
                                        runs = if (currentRuns.isNotEmpty()) currentRuns.toList() else listOf(
                                            TextRun(paraText, isBold, isItalic, isUnderline)
                                        ),
                                        numbering = paragraphNumbering
                                    )
                                    if (inTable) {
                                        currentCellParagraphs.add(paragraphObj)
                                    } else {
                                        elements.add(paragraphObj)
                                        plainTextBuilder.append(paraText).append("\n\n")
                                    }
                                }

                                if (paraHasSectPr) currentSectionStart = elements.size

                                inHeading = false
                                currentText.clear()
                                currentRuns.clear()
                                isBold = false
                                isItalic = false
                                isUnderline = false
                            }

                            tagLocal == "tc" || tagLocal == "table-cell" -> {
                                val cellText = if (currentCellParagraphs.isNotEmpty()) {
                                    currentCellParagraphs.joinToString("\n") { it.text }
                                } else {
                                    currentText.toString().trim()
                                }
                                currentCells.add(
                                    TableCell(
                                        text = cellText,
                                        paragraphs = currentCellParagraphs.toList()
                                    )
                                )
                                currentCellParagraphs.clear()
                                currentText.clear()
                            }

                            tagLocal == "tr" || tagLocal == "table-row" -> {
                                if (currentCells.isNotEmpty()) {
                                    currentRows.add(TableRow(cells = currentCells.toList()))
                                    currentCells.clear()
                                }
                            }

                            tagLocal == "table" || tagLocal == "tbl" -> {
                                inTable = false
                                if (currentRows.isNotEmpty()) {
                                    val maxCols = currentRows.maxOfOrNull { it.cells.size } ?: 0
                                    val tableObj = OfficeDocumentElement.Table(
                                        rows = currentRows.toList(),
                                        numColumns = maxCols,
                                        name = currentTableName
                                    )
                                    currentTableName = null
                                    elements.add(tableObj)

                                    currentRows.forEach { row ->
                                        plainTextBuilder.append(row.cells.joinToString("\t") { it.text }).append("\n")
                                    }
                                    plainTextBuilder.append("\n")
                                    currentRows.clear()
                                }
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown XML parsing error"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = file.name,
                errorMsg = errorMsg,
                cause = e
            )
            val failProgress = ParsingProgress(
                percentage = 0,
                statusMessage = context.getString(com.example.R.string.doc_open_failed_title),
                isFailed = true,
                errorMessage = errorMsg
            )
            _parsingProgress.postValue(failProgress)
            return@withContext OfficeParsedDocument(
                elements = emptyList(),
                rawXml = xmlContent,
                plainText = "",
                extractedImages = extractedImages,
                isOdt = isOdt,
                isDocx = isDocx,
                isOds = isOds,
                isXlsx = isXlsx,
                isOdp = detectedOdp,
                isPptx = detectedPptx,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }

        val detectedDocPageCount = (if (isDocx) extractDocxPageCount(file) else if (isOdt) extractOdtPageCount(file) else null) ?: 0
        val plainTextResult = plainTextBuilder.toString().trim()
        val allParagraphStyles = LinkedHashMap<String, ParagraphStyle>()
        allParagraphStyles.putAll(docxParagraphStyles)
        allParagraphStyles.putAll(generatedStyles)
        val docxDocumentStyles = if (isDocx) {
            DocumentStyles(
                paragraphStyles = allParagraphStyles,
                characterStyles = docxStylesResult.characterStyles,
                defaultPageStyle = extractDocxPageStyleSpec(xmlContent),
                defaultParagraphStyle = docxDefaultParagraphStyle
            )
        } else DocumentStyles()
        if (currentSectionStart > 0 && currentSectionStart < elements.size && sectionStarts.none { it.elementIndex == currentSectionStart }) {
            sectionStarts += SectionStart(currentSectionStart, SectionStartKind.NEXT_PAGE)
        }
        val parsedDoc = OfficeParsedDocument(
            sectionStarts = sectionStarts.toList(),
            elements = elements,
            rawXml = xmlContent,
            plainText = if (plainTextResult.isBlank()) "" else plainTextResult,
            extractedImages = extractedImages,
            isOdt = isOdt,
            isDocx = isDocx,
            isOds = isOds,
            isXlsx = isXlsx,
            isOdp = detectedOdp,
            isPptx = detectedPptx,
            isParsingFailed = false,
            pageCount = detectedDocPageCount,
            styles = docxDocumentStyles
        )
        inMemoryParsedDocCache[file.absolutePath] = ParsedCacheEntry(file.lastModified(), file.length(), parsedDoc)
        cacheRepository.saveCachedDocument(file, parsedDoc)
        return@withContext parsedDoc
    }

    /**
     * Saves or creates a valid ODS ZIP package containing properly formatted 'content.xml',
     * 'mimetype', 'META-INF/manifest.xml', 'styles.xml', and 'meta.xml'.
     */
    suspend fun saveOdsDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdsDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveOdsDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdsDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveOdsDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_ods_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdsContentXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new ODS Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. mimetype (MUST be uncompressed STORED entry per ODF spec)
                    val mimeBytes = "application/vnd.oasis.opendocument.spreadsheet".toByteArray(Charsets.UTF_8)
                    val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                        method = java.util.zip.ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        compressedSize = mimeBytes.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                        extra = ByteArray(0)
                    }
                    zout.putNextEntry(mimeEntry)
                    zout.write(mimeBytes)
                    zout.closeEntry()

                    // 2. META-INF/manifest.xml
                    val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                    zout.putNextEntry(manifestEntry)
                    zout.write(generateOdsManifestXml())
                    zout.closeEntry()

                    // 3. styles.xml
                    val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                    zout.putNextEntry(stylesEntry)
                    zout.write(generateOdtStylesXml())
                    zout.closeEntry()

                    // 4. meta.xml
                    val metaEntry = java.util.zip.ZipEntry("meta.xml")
                    zout.putNextEntry(metaEntry)
                    zout.write(generateOdsMetaXml())
                    zout.closeEntry()

                    // 5. content.xml
                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing ODS file in-place
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundContent = false
                        var foundManifest = false
                        var foundStyles = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "META-INF/manifest.xml") foundManifest = true
                            if (entryName == "styles.xml") foundStyles = true

                            if (entryName == "mimetype") {
                                val mimeBytes = "application/vnd.oasis.opendocument.spreadsheet".toByteArray(Charsets.UTF_8)
                                val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                                    method = java.util.zip.ZipEntry.STORED
                                    size = mimeBytes.size.toLong()
                                    compressedSize = mimeBytes.size.toLong()
                                    crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                                    extra = ByteArray(0)
                                }
                                zout.putNextEntry(mimeEntry)
                                zout.write(mimeBytes)
                            } else if (entryName == "content.xml") {
                                foundContent = true
                                val contentEntry = java.util.zip.ZipEntry("content.xml")
                                zout.putNextEntry(contentEntry)
                                zout.write(contentXmlBytes)
                            } else {
                                val newEntry = java.util.zip.ZipEntry(entryName)
                                zout.putNextEntry(newEntry)
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundContent) {
                            val contentEntry = java.util.zip.ZipEntry("content.xml")
                            zout.putNextEntry(contentEntry)
                            zout.write(contentXmlBytes)
                            zout.closeEntry()
                        }
                        if (!foundManifest) {
                            val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                            zout.putNextEntry(manifestEntry)
                            zout.write(generateOdsManifestXml())
                            zout.closeEntry()
                        }
                        if (!foundStyles) {
                            val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                            zout.putNextEntry(stylesEntry)
                            zout.write(generateOdtStylesXml())
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODS Save Error",
                exceptionType = "OdsPackageSaveException",
                message = "Failed to create/update ODS ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdsContentXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:spreadsheet>\n")
        sb.append("      <table:table table:name=\"Sheet1\">\n")

        val tables = elements.filterIsInstance<OfficeDocumentElement.Table>()
        if (tables.isNotEmpty()) {
            for (tbl in tables) {
                for (row in tbl.rows) {
                    sb.append("        <table:table-row>\n")
                    for (cell in row.cells) {
                        val escCell = escapeXml(cell.text)
                        sb.append("          <table:table-cell office:value-type=\"string\">\n")
                        sb.append("            <text:p>$escCell</text:p>\n")
                        sb.append("          </table:table-cell>\n")
                    }
                    sb.append("        </table:table-row>\n")
                }
            }
        } else {
            val lines = text.split("\n")
            for (line in lines) {
                sb.append("        <table:table-row>\n")
                val cells = line.split("\t")
                for (cellText in cells) {
                    val escCell = escapeXml(cellText)
                    sb.append("          <table:table-cell office:value-type=\"string\">\n")
                    sb.append("            <text:p>$escCell</text:p>\n")
                    sb.append("          </table:table-cell>\n")
                }
                sb.append("        </table:table-row>\n")
            }
        }

        sb.append("      </table:table>\n")
        sb.append("    </office:spreadsheet>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdsManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.spreadsheet"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdsMetaXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
  <office:meta>
    <dc:title>Papirus Spreadsheet</dc:title>
    <meta:generator>Papirus Office Parser</meta:generator>
  </office:meta>
</office:document-meta>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid ODT ZIP package containing properly formatted 'content.xml',
     * 'mimetype', 'META-INF/manifest.xml', 'styles.xml', and 'meta.xml'.
     */
    suspend fun saveOdtDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val officeDoc = document.toOfficeDocument()
            val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
            val odtBytes = writer.write(officeDoc)
            outputFile.writeBytes(odtBytes)
            true
        } catch (e: Exception) {
            saveOdtDocumentInternal(outputFile, document.plainText, document.elements)
        }
    }

    suspend fun saveOdtDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val packageEntries = if (outputFile.exists() && outputFile.length() > 0L) {
                val map = mutableMapOf<String, ByteArray>()
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        map[entry.name] = zip.readCappedBytes()
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
                map
            } else emptyMap()

            val packageData = if (packageEntries.isNotEmpty()) OdtPackageData(entries = packageEntries) else null
            val officeDoc = OfficeDocument(
                body = DocumentBody(elements = listOf(OfficeParagraph(text = text))),
                odtPackageData = packageData
            )
            val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
            val odtBytes = writer.write(officeDoc)
            outputFile.writeBytes(odtBytes)
            true
        } catch (e: Exception) {
            saveOdtDocumentInternal(outputFile, text, emptyList())
        }
    }

    private suspend fun saveOdtDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_odt_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdtContentXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new ODT Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. mimetype (MUST be uncompressed STORED entry per ODF spec)
                    val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                    val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                        method = java.util.zip.ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        compressedSize = mimeBytes.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                        extra = ByteArray(0)
                    }
                    zout.putNextEntry(mimeEntry)
                    zout.write(mimeBytes)
                    zout.closeEntry()

                    // 2. META-INF/manifest.xml
                    val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                    zout.putNextEntry(manifestEntry)
                    zout.write(generateOdtManifestXml())
                    zout.closeEntry()

                    // 3. styles.xml
                    val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                    zout.putNextEntry(stylesEntry)
                    zout.write(generateOdtStylesXml())
                    zout.closeEntry()

                    // 4. meta.xml
                    val metaEntry = java.util.zip.ZipEntry("meta.xml")
                    zout.putNextEntry(metaEntry)
                    zout.write(generateOdtMetaXml())
                    zout.closeEntry()

                    // 5. content.xml
                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing ODT file in-place
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundContent = false
                        var foundManifest = false
                        var foundStyles = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "META-INF/manifest.xml") foundManifest = true
                            if (entryName == "styles.xml") foundStyles = true

                            if (entryName == "mimetype") {
                                val mimeBytes = "application/vnd.oasis.opendocument.text".toByteArray(Charsets.UTF_8)
                                val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                                    method = java.util.zip.ZipEntry.STORED
                                    size = mimeBytes.size.toLong()
                                    compressedSize = mimeBytes.size.toLong()
                                    crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                                    extra = ByteArray(0)
                                }
                                zout.putNextEntry(mimeEntry)
                                zout.write(mimeBytes)
                            } else if (entryName == "content.xml") {
                                foundContent = true
                                val contentEntry = java.util.zip.ZipEntry("content.xml")
                                zout.putNextEntry(contentEntry)
                                zout.write(contentXmlBytes)
                            } else {
                                val newEntry = java.util.zip.ZipEntry(entryName)
                                zout.putNextEntry(newEntry)
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundContent) {
                            val contentEntry = java.util.zip.ZipEntry("content.xml")
                            zout.putNextEntry(contentEntry)
                            zout.write(contentXmlBytes)
                            zout.closeEntry()
                        }
                        if (!foundManifest) {
                            val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                            zout.putNextEntry(manifestEntry)
                            zout.write(generateOdtManifestXml())
                            zout.closeEntry()
                        }
                        if (!foundStyles) {
                            val stylesEntry = java.util.zip.ZipEntry("styles.xml")
                            zout.putNextEntry(stylesEntry)
                            zout.write(generateOdtStylesXml())
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODT Save Error",
                exceptionType = "OdtPackageSaveException",
                message = "Failed to create/update ODT ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdtContentXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content ")
        sb.append("xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:table=\"urn:oasis:names:tc:opendocument:xmlns:table:1.0\" ")
        sb.append("xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:draw:1.0\" ")
        sb.append("xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\" ")
        sb.append("xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
        sb.append("office:version=\"1.2\">\n")
        sb.append("  <office:body>\n")
        sb.append("    <office:text>\n")

        if (elements.isNotEmpty()) {
            for (element in elements) {
                when (element) {
                    is OfficeDocumentElement.Heading -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:h text:outline-level=\"${element.level}\">$esc</text:h>\n")
                    }
                    is OfficeDocumentElement.Paragraph -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:p>$esc</text:p>\n")
                    }
                    is OfficeDocumentElement.ListItem -> {
                        val esc = escapeXml(element.text)
                        sb.append("      <text:list><text:list-item><text:p>$esc</text:p></text:list-item></text:list>\n")
                    }
                    is OfficeDocumentElement.Table -> {
                        sb.append("      <table:table table:name=\"Table1\">\n")
                        for (row in element.rows) {
                            sb.append("        <table:table-row>\n")
                            for (cell in row.cells) {
                                val escCell = escapeXml(cell.text)
                                sb.append("          <table:table-cell office:value-type=\"string\">\n")
                                sb.append("            <text:p>$escCell</text:p>\n")
                                sb.append("          </table:table-cell>\n")
                            }
                            sb.append("        </table:table-row>\n")
                        }
                        sb.append("      </table:table>\n")
                    }
                    is OfficeDocumentElement.ImageElement -> {
                        sb.append("      <draw:frame draw:name=\"Image1\">\n")
                        sb.append("        <draw:image xlink:href=\"${escapeXml(element.imagePath)}\" xlink:type=\"simple\" xlink:show=\"embed\" xlink:actuate=\"onLoad\"/>\n")
                        sb.append("      </draw:frame>\n")
                    }
                    is OfficeDocumentElement.PageBreak -> {
                        sb.append("      <text:p text:style-name=\"PageBreak\"/>\n")
                    }
                }
            }
        } else {
            val lines = text.split("\n")
            for (line in lines) {
                val esc = escapeXml(line)
                sb.append("      <text:p>$esc</text:p>\n")
            }
        }

        sb.append("    </office:text>\n")
        sb.append("  </office:body>\n")
        sb.append("</office:document-content>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0" manifest:version="1.2">
  <manifest:file-entry manifest:full-path="/" manifest:version="1.2" manifest:media-type="application/vnd.oasis.opendocument.text"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="styles.xml" manifest:media-type="text/xml"/>
  <manifest:file-entry manifest:full-path="meta.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtStylesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-styles xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0" office:version="1.2">
  <office:styles/>
</office:document-styles>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateOdtMetaXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<office:document-meta xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0" xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0" xmlns:dc="http://purl.org/dc/elements/1.1/" office:version="1.2">
  <office:meta>
    <dc:title>Papirus Document</dc:title>
    <meta:generator>Papirus Office Parser</meta:generator>
  </office:meta>
</office:document-meta>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid XLSX OpenXML ZIP package containing properly formatted
     * 'xl/worksheets/sheet1.xml', '[Content_Types].xml', '_rels/.rels',
     * 'xl/_rels/workbook.xml.rels', and 'xl/workbook.xml'.
     */
    suspend fun saveXlsxDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveXlsxDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveXlsxDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveXlsxDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveXlsxDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_xlsx_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val sheetXmlBytes = generateFormattedXlsxSheetXml(text, elements)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                // Create brand new XLSX Zip Package
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    // 1. [Content_Types].xml
                    val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                    zout.putNextEntry(ctEntry)
                    zout.write(generateXlsxContentTypesXml())
                    zout.closeEntry()

                    // 2. _rels/.rels
                    val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                    zout.putNextEntry(relsEntry)
                    zout.write(generateXlsxRelsXml())
                    zout.closeEntry()

                    // 3. xl/_rels/workbook.xml.rels
                    val wbRelsEntry = java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels")
                    zout.putNextEntry(wbRelsEntry)
                    zout.write(generateXlsxWorkbookRelsXml())
                    zout.closeEntry()

                    // 4. xl/workbook.xml
                    val wbEntry = java.util.zip.ZipEntry("xl/workbook.xml")
                    zout.putNextEntry(wbEntry)
                    zout.write(generateXlsxWorkbookXml())
                    zout.closeEntry()

                    // 5. xl/worksheets/sheet1.xml
                    val sheetEntry = java.util.zip.ZipEntry("xl/worksheets/sheet1.xml")
                    zout.putNextEntry(sheetEntry)
                    zout.write(sheetXmlBytes)
                    zout.closeEntry()
                }
            } else {
                // Update existing XLSX file in-place
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundSheet = false
                        var foundContentTypes = false
                        var foundRels = false
                        var foundWorkbookRels = false
                        var foundWorkbook = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "[Content_Types].xml") foundContentTypes = true
                            if (entryName == "_rels/.rels") foundRels = true
                            if (entryName == "xl/_rels/workbook.xml.rels") foundWorkbookRels = true
                            if (entryName == "xl/workbook.xml") foundWorkbook = true

                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)

                            if (entryName == "xl/worksheets/sheet1.xml" || (entryName.startsWith("xl/worksheets/sheet") && !foundSheet)) {
                                foundSheet = true
                                zout.write(sheetXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundSheet) {
                            val sheetEntry = java.util.zip.ZipEntry("xl/worksheets/sheet1.xml")
                            zout.putNextEntry(sheetEntry)
                            zout.write(sheetXmlBytes)
                            zout.closeEntry()
                        }
                        if (!foundContentTypes) {
                            val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                            zout.putNextEntry(ctEntry)
                            zout.write(generateXlsxContentTypesXml())
                            zout.closeEntry()
                        }
                        if (!foundRels) {
                            val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                            zout.putNextEntry(relsEntry)
                            zout.write(generateXlsxRelsXml())
                            zout.closeEntry()
                        }
                        if (!foundWorkbookRels) {
                            val wbRelsEntry = java.util.zip.ZipEntry("xl/_rels/workbook.xml.rels")
                            zout.putNextEntry(wbRelsEntry)
                            zout.write(generateXlsxWorkbookRelsXml())
                            zout.closeEntry()
                        }
                        if (!foundWorkbook) {
                            val wbEntry = java.util.zip.ZipEntry("xl/workbook.xml")
                            zout.putNextEntry(wbEntry)
                            zout.write(generateXlsxWorkbookXml())
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser XLSX Save Error",
                exceptionType = "XlsxPackageSaveException",
                message = "Failed to create/update XLSX ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedXlsxSheetXml(text: String, elements: List<OfficeDocumentElement>): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">\n")
        sb.append("  <sheetData>\n")

        val tables = elements.filterIsInstance<OfficeDocumentElement.Table>()
        if (tables.isNotEmpty()) {
            var rowIndex = 1
            for (tbl in tables) {
                for (row in tbl.rows) {
                    sb.append("    <row r=\"$rowIndex\">\n")
                    var colIndex = 0
                    for (cell in row.cells) {
                        val colName = getExcelColumnName(colIndex)
                        val cellRef = "$colName$rowIndex"
                        val escCell = escapeXml(cell.text)
                        sb.append("      <c r=\"$cellRef\" t=\"inlineStr\">\n")
                        sb.append("        <is><t>$escCell</t></is>\n")
                        sb.append("      </c>\n")
                        colIndex++
                    }
                    sb.append("    </row>\n")
                    rowIndex++
                }
            }
        } else {
            val lines = text.split("\n")
            var rowIndex = 1
            for (line in lines) {
                sb.append("    <row r=\"$rowIndex\">\n")
                val cells = line.split("\t")
                var colIndex = 0
                for (cellText in cells) {
                    val colName = getExcelColumnName(colIndex)
                    val cellRef = "$colName$rowIndex"
                    val escCell = escapeXml(cellText)
                    sb.append("      <c r=\"$cellRef\" t=\"inlineStr\">\n")
                    sb.append("        <is><t>$escCell</t></is>\n")
                    sb.append("      </c>\n")
                    colIndex++
                }
                sb.append("    </row>\n")
                rowIndex++
            }
        }

        sb.append("  </sheetData>\n")
        sb.append("</worksheet>")

        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun getExcelColumnName(colIndex: Int): String {
        var temp = colIndex
        val sb = StringBuilder()
        while (temp >= 0) {
            val rem = temp % 26
            sb.insert(0, (rem + 'A'.code).toChar())
            temp = temp / 26 - 1
        }
        return sb.toString()
    }

    private fun generateXlsxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxWorkbookRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateXlsxWorkbookXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Sheet1" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid PPTX OpenXML ZIP package containing properly formatted
     * 'ppt/slides/slide1.xml', '[Content_Types].xml', '_rels/.rels',
     * 'ppt/_rels/presentation.xml.rels', and 'ppt/presentation.xml'.
     */
    suspend fun savePptxDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext savePptxDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun savePptxDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext savePptxDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun savePptxDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_pptx_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val slideXmlBytes = generateFormattedPptxSlideXml(text)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    val ctEntry = java.util.zip.ZipEntry("[Content_Types].xml")
                    zout.putNextEntry(ctEntry)
                    zout.write(generatePptxContentTypesXml())
                    zout.closeEntry()

                    val relsEntry = java.util.zip.ZipEntry("_rels/.rels")
                    zout.putNextEntry(relsEntry)
                    zout.write(generatePptxRelsXml())
                    zout.closeEntry()

                    val presRelsEntry = java.util.zip.ZipEntry("ppt/_rels/presentation.xml.rels")
                    zout.putNextEntry(presRelsEntry)
                    zout.write(generatePptxPresRelsXml())
                    zout.closeEntry()

                    val presEntry = java.util.zip.ZipEntry("ppt/presentation.xml")
                    zout.putNextEntry(presEntry)
                    zout.write(generatePptxPresentationXml())
                    zout.closeEntry()

                    val slideEntry = java.util.zip.ZipEntry("ppt/slides/slide1.xml")
                    zout.putNextEntry(slideEntry)
                    zout.write(slideXmlBytes)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundSlide = false

                        while (entry != null) {
                            val entryName = entry.name
                            val newEntry = java.util.zip.ZipEntry(entryName)
                            zout.putNextEntry(newEntry)

                            if (entryName == "ppt/slides/slide1.xml" || (entryName.startsWith("ppt/slides/slide") && !foundSlide)) {
                                foundSlide = true
                                zout.write(slideXmlBytes)
                            } else {
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundSlide) {
                            val slideEntry = java.util.zip.ZipEntry("ppt/slides/slide1.xml")
                            zout.putNextEntry(slideEntry)
                            zout.write(slideXmlBytes)
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser PPTX Save Error",
                exceptionType = "PptxPackageSaveException",
                message = "Failed to create/update PPTX ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedPptxSlideXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<p:sld xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\" ")
        sb.append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\" ")
        sb.append("xmlns:p=\"http://schemas.openxmlformats.org/presentationml/2006/main\">\n")
        sb.append("  <p:cSld><p:spTree>\n")
        sb.append("    <p:sp><p:txBody><a:bodyPr/><a:lstStyle/>\n")

        val lines = text.split("\n")
        for (line in lines) {
            val esc = escapeXml(line)
            sb.append("      <a:p><a:r><a:t>$esc</a:t></a:r></a:p>\n")
        }

        sb.append("    </p:txBody></p:sp>\n")
        sb.append("  </p:spTree></p:cSld>\n")
        sb.append("</p:sld>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxContentTypesXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slides/slide1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slide+xml"/>
</Types>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxPresRelsXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide1.xml"/>
</Relationships>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generatePptxPresentationXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldIdLst>
    <p:sldId id="256" r:id="rId1"/>
  </p:sldIdLst>
</p:presentation>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    /**
     * Saves or creates a valid ODP ZIP package containing properly formatted 'content.xml',
     * 'mimetype', and 'META-INF/manifest.xml'.
     */
    suspend fun saveOdpDocument(outputFile: File, document: OfficeParsedDocument): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdpDocumentInternal(outputFile, document.plainText, document.elements)
    }

    suspend fun saveOdpDocument(outputFile: File, text: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext saveOdpDocumentInternal(outputFile, text, emptyList())
    }

    private suspend fun saveOdpDocumentInternal(
        outputFile: File,
        text: String,
        elements: List<OfficeDocumentElement>
    ): Boolean = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "temp_odp_save_${System.currentTimeMillis()}_${outputFile.name}")
        val success = try {
            val contentXmlBytes = generateFormattedOdpContentXml(text)

            if (!outputFile.exists() || outputFile.length() == 0L) {
                java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                    val mimeBytes = "application/vnd.oasis.opendocument.presentation".toByteArray(Charsets.UTF_8)
                    val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                        method = java.util.zip.ZipEntry.STORED
                        size = mimeBytes.size.toLong()
                        crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                    }
                    zout.putNextEntry(mimeEntry)
                    zout.write(mimeBytes)
                    zout.closeEntry()

                    val manifestEntry = java.util.zip.ZipEntry("META-INF/manifest.xml")
                    zout.putNextEntry(manifestEntry)
                    zout.write(generateOdpManifestXml())
                    zout.closeEntry()

                    val contentEntry = java.util.zip.ZipEntry("content.xml")
                    zout.putNextEntry(contentEntry)
                    zout.write(contentXmlBytes)
                    zout.closeEntry()
                }
            } else {
                java.util.zip.ZipInputStream(outputFile.inputStream()).use { zin ->
                    java.util.zip.ZipOutputStream(tempFile.outputStream()).use { zout ->
                        var entry = zin.nextEntry
                        var foundContent = false

                        while (entry != null) {
                            val entryName = entry.name
                            if (entryName == "mimetype") {
                                val mimeBytes = "application/vnd.oasis.opendocument.presentation".toByteArray(Charsets.UTF_8)
                                val mimeEntry = java.util.zip.ZipEntry("mimetype").apply {
                                    method = java.util.zip.ZipEntry.STORED
                                    size = mimeBytes.size.toLong()
                                    crc = java.util.zip.CRC32().apply { update(mimeBytes) }.value
                                }
                                zout.putNextEntry(mimeEntry)
                                zout.write(mimeBytes)
                            } else if (entryName == "content.xml") {
                                foundContent = true
                                val contentEntry = java.util.zip.ZipEntry("content.xml")
                                zout.putNextEntry(contentEntry)
                                zout.write(contentXmlBytes)
                            } else {
                                val newEntry = java.util.zip.ZipEntry(entryName)
                                zout.putNextEntry(newEntry)
                                zin.copyTo(zout)
                            }

                            zout.closeEntry()
                            zin.closeEntry()
                            entry = zin.nextEntry
                        }

                        if (!foundContent) {
                            val contentEntry = java.util.zip.ZipEntry("content.xml")
                            zout.putNextEntry(contentEntry)
                            zout.write(contentXmlBytes)
                            zout.closeEntry()
                        }
                    }
                }
            }

            tempFile.copyTo(outputFile, overwrite = true)
            cacheRepository.saveCachedDocument(outputFile, text)
            true
        } catch (e: Exception) {
            DocumentParsingLogger.logError(
                context = context,
                tag = "DocParser ODP Save Error",
                exceptionType = "OdpPackageSaveException",
                message = "Failed to create/update ODP ZIP structure: ${e.localizedMessage}",
                details = android.util.Log.getStackTraceString(e)
            )
            false
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        return@withContext success
    }

    private fun generateFormattedOdpContentXml(text: String): ByteArray {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\" ")
        sb.append("xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\" ")
        sb.append("xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\" ")
        sb.append("xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\">\n")
        sb.append("  <office:body><office:presentation>\n")
        sb.append("    <draw:page draw:name=\"page1\">\n")
        sb.append("      <draw:frame><draw:text-box>\n")

        val lines = text.split("\n")
        for (line in lines) {
            val esc = escapeXml(line)
            sb.append("        <text:p>$esc</text:p>\n")
        }

        sb.append("      </draw:text-box></draw:frame>\n")
        sb.append("    </draw:page>\n")
        sb.append("  </office:presentation></office:body>\n")
        sb.append("</office:document-content>")
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun generateOdpManifestXml(): ByteArray {
        val xml = """<?xml version="1.0" encoding="UTF-8"?>
<manifest:manifest xmlns:manifest="urn:oasis:names:tc:opendocument:xmlns:manifest:1.0">
  <manifest:file-entry manifest:full-path="/" manifest:media-type="application/vnd.oasis.opendocument.presentation"/>
  <manifest:file-entry manifest:full-path="content.xml" manifest:media-type="text/xml"/>
</manifest:manifest>""".trimIndent()
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun parseXlsxDocument(
        file: File,
        extractedImages: Map<String, File>
    ): OfficeParsedDocument {
        val sharedStrings = mutableListOf<String>()
        val sheetList = mutableListOf<Pair<String, String>>() // (sheetName, rId or path)
        val relsMap = mutableMapOf<String, String>() // rId -> target
        // Phase 6: only small metadata entries are buffered; worksheets are
        // streamed one at a time in pass 2, so peak memory no longer scales
        // with the workbook's total raw XML size.
        val metaEntries = mutableMapOf<String, ByteArray>()
        val worksheetPaths = mutableListOf<String>()
        val budget = ZipScanBudget()

        try {
            // Pass 1: metadata + worksheet inventory.
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntryBudgeted(budget)
                while (entry != null) {
                    val name = entry.name
                    if (name == "xl/sharedStrings.xml" ||
                        name == "xl/workbook.xml" ||
                        name == "xl/_rels/workbook.xml.rels"
                    ) {
                        metaEntries[name] = zip.readCappedBytes(ZipSafe.MAX_ZIP_ENTRY_BYTES, budget)
                    } else if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                        worksheetPaths.add(name)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntryBudgeted(budget)
                }
            }

            // 1. Parse sharedStrings.xml
            metaEntries["xl/sharedStrings.xml"]?.let { bytes ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
                var event = parser.eventType
                var inSi = false
                val currentString = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                            if (tag == "si") {
                                inSi = true
                                currentString.clear()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inSi) {
                                currentString.append(parser.text ?: "")
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                            if (tag == "si") {
                                inSi = false
                                sharedStrings.add(currentString.toString())
                                currentString.clear()
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            // 2. Parse xl/_rels/workbook.xml.rels
            metaEntries["xl/_rels/workbook.xml.rels"]?.let { bytes ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                        if (tag == "relationship") {
                            val id = parser.getAttributeValue(null, "Id") ?: ""
                            var target = parser.getAttributeValue(null, "Target") ?: ""
                            if (target.startsWith("/")) {
                                target = target.removePrefix("/")
                            } else if (!target.startsWith("xl/")) {
                                target = "xl/$target"
                            }
                            if (id.isNotEmpty() && target.isNotEmpty()) {
                                relsMap[id] = target
                            }
                        }
                    }
                    event = parser.next()
                }
            }

            // 3. Parse xl/workbook.xml
            metaEntries["xl/workbook.xml"]?.let { bytes ->
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val parser = factory.newPullParser()
                parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG) {
                        val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                        if (tag == "sheet") {
                            val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                            var rId = parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                            if (rId.isNullOrEmpty()) {
                                rId = parser.getAttributeValue(null, "r:id")
                            }
                            if (rId.isNullOrEmpty()) {
                                rId = parser.getAttributeValue(null, "id")
                            }
                            sheetList.add(Pair(name, rId ?: ""))
                        }
                    }
                    event = parser.next()
                }
            }

            // Fallback if workbook.xml didn't find sheets: scan worksheets/sheet*.xml
            if (sheetList.isEmpty()) {
                val sheetNames = worksheetPaths.filter { it.startsWith("xl/worksheets/sheet") }
                    .sortedBy { key ->
                        XLSX_SHEET_FILE_REGEX.find(key)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    }
                sheetNames.forEachIndexed { index, path ->
                    sheetList.add(Pair("Sheet${index + 1}", path))
                }
            }

            val elements = mutableListOf<OfficeDocumentElement>()
            val plainTextBuilder = StringBuilder()

            // Pass 2: stream each worksheet (fresh budget per scan; consumed
            // bytes are charged as the pull parser reads the entry stream).
            for ((sheetName, rIdOrPath) in sheetList) {
                val targetPath = relsMap[rIdOrPath] ?: if (rIdOrPath.startsWith("xl/")) rIdOrPath else "xl/worksheets/sheet${sheetList.indexOfFirst { it.first == sheetName } + 1}.xml"
                val resolvedPath = worksheetPaths.firstOrNull { it == targetPath }
                    ?: worksheetPaths.firstOrNull { it.endsWith(targetPath.substringAfterLast("/")) }
                    ?: continue
                val rows = streamXlsxWorksheet(file, resolvedPath, sharedStrings)

                if (rows.isNotEmpty()) {
                    val maxCols = rows.maxOfOrNull { it.cells.size } ?: 0
                    val tableElement = OfficeDocumentElement.Table(
                        rows = rows.toList(),
                        numColumns = maxCols,
                        name = sheetName
                    )
                    elements.add(tableElement)

                    plainTextBuilder.append("=== Sheet: ").append(sheetName).append(" ===\n")
                    rows.forEach { row ->
                        plainTextBuilder.append(row.cells.joinToString("\t") { it.text }).append("\n")
                    }
                    plainTextBuilder.append("\n")
                }
            }

            return OfficeParsedDocument(
                elements = elements,
                rawXml = "",
                plainText = plainTextBuilder.toString().trim(),
                extractedImages = extractedImages,
                isXlsx = true,
                isParsingFailed = false,
                pageCount = sheetList.size
            )
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to parse XLSX document"
            return OfficeParsedDocument(
                elements = emptyList(),
                rawXml = "",
                plainText = "",
                extractedImages = extractedImages,
                isXlsx = true,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }
    }

    /**
     * Phase 6: parses one worksheet by streaming its ZIP entry straight into
     * the pull parser; no intermediate ByteArray of the sheet XML is kept,
     * and the materialized row model is capped by [ZipSafe.MAX_XLSX_ROWS].
     */
    private fun streamXlsxWorksheet(
        file: File,
        worksheetPath: String,
        sharedStrings: List<String>
    ): List<TableRow> {
        val rows = mutableListOf<TableRow>()
        val budget = ZipScanBudget()
        ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntryBudgeted(budget)
            while (entry != null) {
                if (entry.name == worksheetPath) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(BudgetedInputStream(zip, budget), "UTF-8")
                    parseXlsxWorksheetStream(parser, sharedStrings, rows)
                    zip.closeEntry()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntryBudgeted(budget)
            }
        }
        return rows
    }

    private fun parseXlsxWorksheetStream(
        parser: XmlPullParser,
        sharedStrings: List<String>,
        rows: MutableList<TableRow>
    ) {
        var event = parser.eventType

        val currentRowCells = mutableListOf<TableCell>()
        var currentCellRef = ""
        var currentCellType = ""
        val currentValText = StringBuilder()
        var inV = false
        var inIs = false

        while (event != XmlPullParser.END_DOCUMENT) {
            if (rows.size >= ZipSafe.MAX_XLSX_ROWS) break
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                    when (tag) {
                        "row" -> {
                            currentRowCells.clear()
                        }
                        "c" -> {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                            currentValText.clear()
                        }
                        "v", "t" -> {
                            inV = true
                        }
                        "is" -> {
                            inIs = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inV || inIs) {
                        currentValText.append(parser.text ?: "")
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                    when (tag) {
                        "v", "t" -> {
                            inV = false
                        }
                        "is" -> {
                            inIs = false
                        }
                        "c" -> {
                            val rawVal = currentValText.toString().trim()
                            val cellText = when (currentCellType) {
                                "s" -> {
                                    val idx = rawVal.toIntOrNull() ?: -1
                                    if (idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                                }
                                "b" -> if (rawVal == "1") "TRUE" else "FALSE"
                                else -> rawVal
                            }
                            val colIdx = colIndexFromCellRef(currentCellRef)
                            if (colIdx >= 0) {
                                while (currentRowCells.size < colIdx) {
                                    currentRowCells.add(TableCell(text = ""))
                                }
                                currentRowCells.add(TableCell(text = cellText))
                            } else {
                                currentRowCells.add(TableCell(text = cellText))
                            }
                            currentValText.clear()
                        }
                        "row" -> {
                            if (currentRowCells.isNotEmpty() && currentRowCells.any { it.text.isNotBlank() }) {
                                rows.add(TableRow(cells = currentRowCells.toList()))
                            }
                            currentRowCells.clear()
                        }
                    }
                }
            }
            event = parser.next()
        }
    }

    /**
     * A1-style column letters to 0-based index. Capped at the OOXML column
     * limit (XFD) so absurd cell refs cannot drive giant sparse-row fills.
     */
    private fun colIndexFromCellRef(ref: String): Int {
        val colLetters = ref.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
        if (colLetters.isEmpty()) return -1
        var col = 0
        for (ch in colLetters) {
            col = col * 26 + (ch - 'A' + 1)
        }
        return (col - 1).coerceIn(-1, ZipSafe.MAX_XLSX_COLUMN_INDEX)
    }

    private fun parsePptxDocument(
        file: File,
        extractedImages: Map<String, File>
    ): OfficeParsedDocument {
        val elements = mutableListOf<OfficeDocumentElement>()
        val plainTextBuilder = StringBuilder()
        // Phase 6: slides stream one at a time; only the entry-name inventory
        // is kept in memory.
        val slideNames = mutableListOf<String>()
        val budget = ZipScanBudget()

        try {
            // Pass 1: strict slide inventory; slide layouts/masters share the
            // ppt/slides/slide prefix but must not count as slides.
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntryBudgeted(budget)
                while (entry != null) {
                    if (PPTX_SLIDE_ENTRY_REGEX.matches(entry.name)) {
                        slideNames.add(entry.name)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntryBudgeted(budget)
                }
            }

            val sortedSlideNames = slideNames.sortedBy { name ->
                PPTX_SLIDE_FILE_REGEX.find(name)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            }

            // Pass 2: stream each slide.
            for ((slideIndex, slideName) in sortedSlideNames.withIndex()) {
                val slideParagraphs = streamPptxSlideParagraphs(file, slideName)

                if (elements.isNotEmpty()) {
                    elements.add(OfficeDocumentElement.PageBreak)
                }

                val titleIndex = slideParagraphs.indexOfFirst { it.isTitle }.takeIf { it >= 0 }
                    ?: slideParagraphs.indices.firstOrNull()
                val titleText = titleIndex?.let { slideParagraphs[it].text } ?: "Slide ${slideIndex + 1}"
                elements.add(OfficeDocumentElement.Heading(text = titleText, level = 1, styleName = "SlideTitle"))

                plainTextBuilder.append("[Slide ").append(slideIndex + 1).append(": ").append(titleText).append("]\n")

                slideParagraphs.forEachIndexed { index, para ->
                    if (index == titleIndex) return@forEachIndexed
                    elements.add(OfficeDocumentElement.Paragraph(text = para.text))
                    plainTextBuilder.append(para.text).append("\n")
                }
                plainTextBuilder.append("\n")
            }

            return OfficeParsedDocument(
                elements = elements,
                rawXml = "",
                plainText = plainTextBuilder.toString().trim(),
                extractedImages = extractedImages,
                isPptx = true,
                isParsingFailed = false,
                pageCount = sortedSlideNames.size
            )
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to parse PPTX document"
            return OfficeParsedDocument(
                elements = emptyList(),
                rawXml = "",
                plainText = "",
                extractedImages = extractedImages,
                isPptx = true,
                isParsingFailed = true,
                failureReason = errorMsg
            )
        }
    }

    private data class SlideParagraph(val text: String, val isTitle: Boolean)

    /**
     * Phase 6: streams one slide's XML straight into the pull parser and
     * flags paragraphs sitting in title placeholders (used for the heading;
     * first paragraph stays the fallback when no title placeholder exists).
     */
    private fun streamPptxSlideParagraphs(file: File, slideName: String): List<SlideParagraph> {
        val slideParagraphs = mutableListOf<SlideParagraph>()
        val budget = ZipScanBudget()
        ZipInputStream(file.inputStream()).use { zip ->
            var entry = zip.nextEntryBudgeted(budget)
            while (entry != null) {
                if (entry.name == slideName) {
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    val parser = factory.newPullParser()
                    parser.setInput(BudgetedInputStream(zip, budget), "UTF-8")
                    var event = parser.eventType

                    val currentRunText = StringBuilder()
                    var inPara = false
                    var inTitle = false

                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> {
                                val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                                if (tag == "p") {
                                    inPara = true
                                    currentRunText.clear()
                                } else if (tag == "ph") {
                                    val type = parser.getAttributeValue(null, "type") ?: ""
                                    if (type == "title" || type == "ctrTitle") {
                                        inTitle = true
                                    }
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inPara) {
                                    currentRunText.append(parser.text ?: "")
                                }
                            }
                            XmlPullParser.END_TAG -> {
                                val tag = (parser.name ?: "").lowercase(Locale.ROOT).substringAfterLast(":")
                                if (tag == "p") {
                                    inPara = false
                                    val pText = currentRunText.toString().trim()
                                    if (pText.isNotEmpty()) {
                                        slideParagraphs.add(SlideParagraph(pText, inTitle))
                                    }
                                    currentRunText.clear()
                                } else if (tag == "sp") {
                                    inTitle = false
                                }
                            }
                        }
                        event = parser.next()
                    }
                    zip.closeEntry()
                    break
                }
                zip.closeEntry()
                entry = zip.nextEntryBudgeted(budget)
            }
        }
        return slideParagraphs
    }
}
