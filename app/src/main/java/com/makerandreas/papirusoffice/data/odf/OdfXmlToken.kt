package com.makerandreas.papirusoffice.data.odf

/**
 * Modern Kotlin enumeration and mapping system for ODF XML tokens,
 * mirroring LibreOffice xmloff (include/xmloff/xmltoken.hxx and xmloff/source/core/xmltoken.cxx).
 */
enum class OdfXmlToken(val tokenName: String) {
    XML_TOKEN_START(""),
    XML_TOKEN_INVALID(""),
    
    // Namespaces and document structure
    XML_OFFICE("office"),
    XML_DOCUMENT("office:document"),
    XML_DOCUMENT_CONTENT("office:document-content"),
    XML_BODY("office:body"),
    XML_TEXT("office:text"),
    XML_SPREADSHEET("office:spreadsheet"),
    XML_PRESENTATION("office:presentation"),
    XML_AUTOMATIC_STYLES("office:automatic-styles"),
    XML_MASTER_STYLES("office:master-styles"),
    XML_STYLES("office:styles"),
    XML_FONT_FACE_DECLS("office:font-face-decls"),
    XML_SCRIPTS("office:scripts"),
    
    // Text elements
    XML_P("text:p"),
    XML_H("text:h"),
    XML_SPAN("text:span"),
    XML_A("text:a"),
    XML_LINE_BREAK("text:line-break"),
    XML_TAB("text:tab"),
    XML_S("text:s"),
    XML_C("text:c"),
    XML_LIST("text:list"),
    XML_LIST_ITEM("text:list-item"),
    XML_LIST_HEADER("text:list-header"),
    XML_SECTION("text:section"),
    XML_BOOKMARK("text:bookmark"),
    XML_BOOKMARK_START("text:bookmark-start"),
    XML_BOOKMARK_END("text:bookmark-end"),
    XML_NOTE("text:note"),
    XML_NOTE_BODY("text:note-body"),
    XML_NOTE_CITATION("text:note-citation"),
    
    // Table elements
    XML_TABLE("table:table"),
    XML_TABLE_COLUMN("table:table-column"),
    XML_TABLE_ROW("table:table-row"),
    XML_TABLE_CELL("table:table-cell"),
    XML_COVERED_TABLE_CELL("table:covered-table-cell"),
    XML_TABLE_HEADER_ROWS("table:table-header-rows"),
    
    // Graphic & Drawing elements
    XML_DRAW("draw"),
    XML_PAGE("draw:page"),
    XML_FRAME("draw:frame"),
    XML_IMAGE("draw:image"),
    XML_TEXT_BOX("draw:text-box"),
    XML_OBJECT("draw:object"),
    XML_OBJECT_OLE("draw:object-ole"),
    
    // Style elements
    XML_STYLE("style:style"),
    XML_DEFAULT_STYLE("style:default-style"),
    XML_TEXT_PROPERTIES("style:text-properties"),
    XML_PARAGRAPH_PROPERTIES("style:paragraph-properties"),
    XML_TABLE_PROPERTIES("style:table-properties"),
    XML_TABLE_COLUMN_PROPERTIES("style:table-column-properties"),
    XML_TABLE_ROW_PROPERTIES("style:table-row-properties"),
    XML_TABLE_CELL_PROPERTIES("style:table-cell-properties"),
    XML_GRAPHIC_PROPERTIES("style:graphic-properties"),
    XML_PAGE_LAYOUT("style:page-layout"),
    XML_PAGE_LAYOUT_PROPERTIES("style:page-layout-properties"),
    XML_HEADER_FOOTER_PROPERTIES("style:header-footer-properties"),
    
    // Key attributes & properties
    XML_NAME("style:name"),
    XML_CLASS("style:class"),
    XML_FAMILY("style:family"),
    XML_PARENT_STYLE_NAME("style:parent-style-name"),
    XML_OUTLINE_LEVEL("text:outline-level"),
    XML_STYLE_NAME("text:style-name"),
    XML_BOLD("font-weight"),
    XML_ITALIC("font-style"),
    XML_UNDERLINE("text-underline-style"),
    XML_FONT_NAME("font-name"),
    XML_FONT_FAMILY("font-family"),
    XML_FONT_SIZE("font-size"),
    XML_COLOR("color"),
    XML_BACKGROUND_COLOR("background-color"),
    XML_TEXT_ALIGN("text-align"),
    XML_LINE_HEIGHT("line-height"),
    XML_MARGIN_TOP("margin-top"),
    XML_MARGIN_BOTTOM("margin-bottom"),
    XML_MARGIN_LEFT("margin-left"),
    XML_MARGIN_RIGHT("margin-right"),
    XML_WIDTH("svg:width"),
    XML_HEIGHT("svg:height"),
    XML_HREF("xlink:href"),
    XML_TYPE("xlink:type"),
    
    XML_UNKNOWN("_unknown_");

    companion object {
        private val tokenMap: Map<String, OdfXmlToken> by lazy {
            val map = mutableMapOf<String, OdfXmlToken>()
            values().forEach { token ->
                if (token.tokenName.isNotEmpty()) {
                    map[token.tokenName.lowercase()] = token
                    // Also strip prefix for namespace-agnostic lookup (e.g., "p" -> XML_P)
                    if (token.tokenName.contains(":")) {
                        val simpleName = token.tokenName.substringAfter(":")
                        map.putIfAbsent(simpleName.lowercase(), token)
                    }
                }
            }
            map
        }

        fun fromTag(rawTag: String?): OdfXmlToken {
            if (rawTag.isNullOrBlank()) return XML_UNKNOWN
            val cleanTag = rawTag.trim().lowercase()
            return tokenMap[cleanTag] ?: tokenMap[cleanTag.substringAfter(":")] ?: XML_UNKNOWN
        }
    }
}

/**
 * Returns the string representation for eToken matching GetXMLToken() in LibreOffice xmloff.
 */
fun GetXMLToken(eToken: OdfXmlToken): String = eToken.tokenName

/**
 * Compares string view to token matching IsXMLToken() in LibreOffice xmloff.
 */
fun IsXMLToken(rString: String?, eToken: OdfXmlToken): Boolean {
    if (rString == null) return false
    val token = OdfXmlToken.fromTag(rString)
    return token == eToken
}
