package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.DocumentIndexKind

/**
 * Plan 7C import contexts for `text:section` and the authored index family
 * (ODF 1.4 part 3, 5.4 and 8.2 to 8.8). Paragraphs, headings, lists, tables
 * and frames inside them keep flowing into the body element list; these
 * contexts only record where each range starts and ends.
 */

private val INDEX_KIND_BY_TOKEN: Map<OdfXmlToken, DocumentIndexKind> = mapOf(
    OdfXmlToken.XML_TABLE_OF_CONTENT to DocumentIndexKind.TABLE_OF_CONTENT,
    OdfXmlToken.XML_ALPHABETICAL_INDEX to DocumentIndexKind.ALPHABETICAL_INDEX,
    OdfXmlToken.XML_ILLUSTRATION_INDEX to DocumentIndexKind.ILLUSTRATION_INDEX,
    OdfXmlToken.XML_TABLE_INDEX to DocumentIndexKind.TABLE_INDEX,
    OdfXmlToken.XML_OBJECT_INDEX to DocumentIndexKind.OBJECT_INDEX,
    OdfXmlToken.XML_USER_INDEX to DocumentIndexKind.USER_INDEX,
    OdfXmlToken.XML_BIBLIOGRAPHY to DocumentIndexKind.BIBLIOGRAPHY
)

private val INDEX_SOURCE_TOKENS: Set<OdfXmlToken> = setOf(
    OdfXmlToken.XML_TABLE_OF_CONTENT_SOURCE,
    OdfXmlToken.XML_ALPHABETICAL_INDEX_SOURCE,
    OdfXmlToken.XML_ILLUSTRATION_INDEX_SOURCE,
    OdfXmlToken.XML_TABLE_INDEX_SOURCE,
    OdfXmlToken.XML_OBJECT_INDEX_SOURCE,
    OdfXmlToken.XML_USER_INDEX_SOURCE,
    OdfXmlToken.XML_BIBLIOGRAPHY_SOURCE
)

private val INDEX_ENTRY_TEMPLATE_TOKENS: Set<OdfXmlToken> = setOf(
    OdfXmlToken.XML_TABLE_OF_CONTENT_ENTRY_TEMPLATE,
    OdfXmlToken.XML_ALPHABETICAL_INDEX_ENTRY_TEMPLATE,
    OdfXmlToken.XML_ILLUSTRATION_INDEX_ENTRY_TEMPLATE,
    OdfXmlToken.XML_TABLE_INDEX_ENTRY_TEMPLATE,
    OdfXmlToken.XML_OBJECT_INDEX_ENTRY_TEMPLATE,
    OdfXmlToken.XML_USER_INDEX_ENTRY_TEMPLATE,
    OdfXmlToken.XML_BIBLIOGRAPHY_ENTRY_TEMPLATE
)

/** Index element kind for [token], or null when the token is not an index element. */
internal fun indexKindFor(token: OdfXmlToken): DocumentIndexKind? = INDEX_KIND_BY_TOKEN[token]

/**
 * Child dispatch shared by `office:text`, `text:section`, `text:index-body`
 * and `text:index-title`: the text body content model (ODF 1.4 part 3, 3.4).
 * Returns null for tokens the caller should hand to its own fallback.
 */
internal fun textFlowChildContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    attributes: Map<String, String>
): SvXMLImportContext? {
    indexKindFor(token)?.let { return OdfIndexContext(importFilter, token, it) }
    return when (token) {
        OdfXmlToken.XML_SECTION -> OdfSectionContext(importFilter, token)
        OdfXmlToken.XML_P -> OdfParagraphContext(importFilter, token, attributes)
        OdfXmlToken.XML_H -> OdfHeadingContext(importFilter, token, attributes)
        OdfXmlToken.XML_LIST -> OdfListContext(importFilter, token, 1, attributes)
        OdfXmlToken.XML_TABLE -> OdfTableContext(importFilter, token, attributes)
        OdfXmlToken.XML_FRAME -> OdfFrameContext(importFilter, token, attributes)
        OdfXmlToken.XML_TEXT_BOX, OdfXmlToken.XML_CUSTOM_SHAPE, OdfXmlToken.XML_G ->
            OdfDrawingContainerContext(importFilter, token)
        else -> null
    }
}

/** `<text:section>`: a named range over body flow. */
class OdfSectionContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        importFilter.ranges.beginSection(attributes)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext =
        textFlowChildContext(importFilter, token, attributes) ?: super.createChildContext(token, attributes)

    override fun onEndElement(token: OdfXmlToken) {
        importFilter.ranges.endSection()
    }
}

/** Any of the seven authored index elements. */
class OdfIndexContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val kind: DocumentIndexKind
) : SvXMLImportContext(importFilter, token) {
    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        importFilter.ranges.beginIndex(kind, attributes)
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext =
        when {
            token in INDEX_SOURCE_TOKENS -> OdfIndexSourceContext(importFilter, token)
            token == OdfXmlToken.XML_INDEX_BODY -> OdfIndexBodyContext(importFilter, token)
            else -> OdfIgnoreSubtreeContext(importFilter, token)
        }

    override fun onEndElement(token: OdfXmlToken) {
        importFilter.ranges.endIndex()
    }
}

/**
 * Index source: only entry templates are read, for the outline level that
 * maps an entry paragraph style to its level. The source text itself is a
 * generation recipe and never reaches body flow.
 */
class OdfIndexSourceContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext {
        if (token in INDEX_ENTRY_TEMPLATE_TOKENS) {
            importFilter.ranges.recordEntryTemplate(attributes)
        }
        return OdfIgnoreSubtreeContext(importFilter, token)
    }
}

/** `<text:index-body>`: the rendered snapshot, which flows as normal body content. */
class OdfIndexBodyContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext =
        when (token) {
            OdfXmlToken.XML_INDEX_TITLE -> OdfIndexTitleContext(importFilter, token)
            else -> textFlowChildContext(importFilter, token, attributes) ?: super.createChildContext(token, attributes)
        }
}

/** `<text:index-title>`: rendered heading of an index, excluded from entries. */
class OdfIndexTitleContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken
) : SvXMLImportContext(importFilter, token) {
    override fun onStartElement(token: OdfXmlToken, attributes: Map<String, String>) {
        importFilter.ranges.beginIndexTitle()
    }

    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext =
        textFlowChildContext(importFilter, token, attributes) ?: super.createChildContext(token, attributes)

    override fun onEndElement(token: OdfXmlToken) {
        importFilter.ranges.endIndexTitle()
    }
}

/**
 * `<text:section>` inside a table cell. Ranges are body-level only
 * (audit-015 F-13), so the section is reported and its paragraphs are handed
 * back to the owning cell instead of leaking into body flow.
 */
class OdfTableCellSectionContext(
    importFilter: SvXMLImport,
    token: OdfXmlToken,
    private val cell: SvXMLImportContext
) : SvXMLImportContext(importFilter, token) {
    override fun createChildContext(token: OdfXmlToken, attributes: Map<String, String>): SvXMLImportContext =
        cell.createChildContext(token, attributes)
}
