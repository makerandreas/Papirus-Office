package com.makerandreas.papirusoffice.data.odf

import android.content.Context
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
    val outlineLevel: Int? = null
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

    val elements: List<OfficeDocumentElement> get() = parsedElements

    fun addElement(element: OfficeDocumentElement) {
        parsedElements.add(element)
    }

    fun parseOdfStyles(xml: String?) {
        if (xml.isNullOrBlank()) return
        var pendingPageLayoutName: String? = null
        var capturingDefaultPageStyle = false
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
                        "style", "page-layout", "default-style", "page-layout-properties", "master-page" -> attrIndex(parser)
                        else -> emptyMap()
                    }
                    when (localName) {
                        "style" -> {
                            val name = attrs["name"]
                            val family = attrs["family"] ?: "paragraph"
                            val parent = attrs["parent-style-name"]
                            val disp = attrs["display-name"]
                            val outline = attrs["default-outline-level"]

                            if (!name.isNullOrBlank()) {
                                val info = OdfStyleInfo(
                                    name = name,
                                    family = family,
                                    parentName = parent,
                                    displayName = disp,
                                    outlineLevel = outline?.toIntOrNull()
                                )
                                styleMap[name] = info
                                styleMap[name.lowercase(Locale.ROOT)] = info
                            }
                        }
                        "page-layout" -> pendingPageLayoutName = attrs["name"]?.takeIf { it.isNotBlank() }
                        "default-style" -> capturingDefaultPageStyle = attrs["family"].equals("page", ignoreCase = true)
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
                        "default-style" -> capturingDefaultPageStyle = false
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            // graceful fallback
        }
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
            val info = styleMap[curr] ?: styleMap[curr.lowercase(java.util.Locale.ROOT)]
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
        for (info in styleMap.values) {
            if (info.family.isNotBlank() && !info.family.equals("paragraph", ignoreCase = true)) continue
            paragraphs.putIfAbsent(
                info.name,
                ParagraphStyle(name = info.name, parentStyleName = info.parentName)
            )
        }
        val defaultPage = (standardPageLayoutName ?: firstMasterPageLayoutName)
            ?.let { pageLayouts[it] }
            ?: pageLayouts.values.firstOrNull()
            ?: pageSpecFromDefaultStyle
        return DocumentStyles(
            paragraphStyles = paragraphs,
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

        // Preload style hierarchies from styles.xml and content.xml automatic-styles
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
