package com.makerandreas.papirusoffice.data.odf

import android.content.Context
import com.makerandreas.papirusoffice.data.CharacterStyle
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.util.DocumentParsingLogger
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
    val alignment: String? = null
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
    var fontFamily: String? = null,
    var fontSizePt: Float? = null,
    var isBold: Boolean? = null,
    var isItalic: Boolean? = null,
    var isUnderline: Boolean? = null,
    var colorHex: String? = null,
    var alignment: String? = null
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
        alignment = alignment
    )
}

private val FONT_SIZE_NUMBER = Regex("^([-+]?[0-9]*\\.?[0-9]+)")

/** ODF `fo:font-size` like `12pt` stays in points; not converted through [OdfLength]. */
internal fun parseOdfFontSizePt(raw: String?): Float? {
    if (raw.isNullOrBlank()) return null
    val text = raw.trim().lowercase(Locale.ROOT)
    val number = FONT_SIZE_NUMBER.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
    if (number <= 0f) return null
    return when {
        text.endsWith("%") -> null
        text.endsWith("in") -> number * 72f
        text.endsWith("cm") -> number * 72f / 2.54f
        text.endsWith("mm") -> number * 72f / 25.4f
        else -> number
    }
}

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

private fun applyTextProperties(draft: StyleDraft, attrs: Map<String, String>) {
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
        draft.fontFamily = family.trim().trim('\'', '"')
    }
}

private fun applyParagraphProperties(draft: StyleDraft, attrs: Map<String, String>) {
    parseAlignment(attrs["text-align"])?.let { draft.alignment = it }
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
    alignment = over.alignment ?: base.alignment
)

class SvXMLImport(
    private val context: Context,
    val extractedImages: Map<String, File> = emptyMap()
) {

    private val contextStack = ArrayDeque<SvXMLImportContext>()
    private val parsedElements = mutableListOf<OfficeDocumentElement>()
    private val styleMap = mutableMapOf<String, OdfStyleInfo>()
    private val pageLayouts = LinkedHashMap<String, PageStyleSpec>()
    private var pageSpecFromDefaultStyle: PageStyleSpec? = null
    private var standardPageLayoutName: String? = null
    private var firstMasterPageLayoutName: String? = null
    private var defaultParagraphStyle: OdfStyleInfo? = null

    val elements: List<OfficeDocumentElement> get() = parsedElements

    fun addElement(element: OfficeDocumentElement) {
        parsedElements.add(element)
    }

    fun parseOdfStyles(xml: String?) {
        if (xml.isNullOrBlank()) return
        var pendingPageLayoutName: String? = null
        var capturingDefaultPageStyle = false
        var pendingDraft: StyleDraft? = null
        var pendingIsDefault = false
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
                        "master-page", "text-properties", "paragraph-properties" -> attrIndex(parser)
                        else -> emptyMap()
                    }
                    when (localName) {
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
                                    outlineLevel = attrs["default-outline-level"]?.toIntOrNull()
                                )
                            } else {
                                null
                            }
                        }
                        "page-layout" -> pendingPageLayoutName = attrs["name"]?.takeIf { it.isNotBlank() }
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
                        "text-properties" -> pendingDraft?.let { applyTextProperties(it, attrs) }
                        "paragraph-properties" -> pendingDraft?.let { applyParagraphProperties(it, attrs) }
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
                                if (firstMasterPageLayoutName == null) firstMasterPageLayoutName = layoutName
                                if (attrs["name"].equals("Standard", ignoreCase = true)) {
                                    standardPageLayoutName = layoutName
                                }
                            }
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    when ((parser.name ?: "").substringAfterLast(':')) {
                        "page-layout" -> pendingPageLayoutName = null
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
    }

    private fun lookupStyle(name: String): OdfStyleInfo? {
        return styleMap[name] ?: styleMap[name.lowercase(Locale.ROOT)]
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

    fun resolveSpanFormatting(styleName: String?): OdfSpanFormat {
        if (styleName.isNullOrBlank()) return OdfSpanFormat()
        val cascaded = cascadeStyle(styleName, "text") ?: return OdfSpanFormat()
        return OdfSpanFormat(
            isBold = cascaded.isBold == true,
            isItalic = cascaded.isItalic == true,
            isUnderline = cascaded.isUnderline == true
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
        return DocumentStyles(
            paragraphStyles = paragraphs,
            characterStyles = characters,
            pageStyles = pageLayouts.toMap(),
            defaultPageStyle = defaultPage
        )
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
        isOdp: Boolean = false
    ): OfficeParsedDocument {
        parsedElements.clear()
        contextStack.clear()
        styleMap.clear()
        pageLayouts.clear()
        pageSpecFromDefaultStyle = null
        standardPageLayoutName = null
        firstMasterPageLayoutName = null
        defaultParagraphStyle = null

        // Preload style hierarchies from styles.xml and content.xml automatic-styles
        // without clearing defaults between the two so office:styles survive.
        if (!stylesXmlContent.isNullOrBlank()) {
            parseOdfStyles(stylesXmlContent)
        }
        parseOdfStyles(xmlContent)

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
                            DocumentParsingLogger.logUnsupportedTag(
                                context = context,
                                fileName = fileName,
                                tagName = rawTagName,
                                attributes = attributes
                            )
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
                styles = toDocumentStyles()
            )

        } catch (e: Exception) {
            val errorMsg = "ODF SAX Import Error: ${e.localizedMessage ?: "Failed parsing XML"}"
            DocumentParsingLogger.logMalformedXml(
                context = context,
                fileName = fileName,
                errorMsg = errorMsg,
                cause = e
            )
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
    parentStyleName = parentName
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
