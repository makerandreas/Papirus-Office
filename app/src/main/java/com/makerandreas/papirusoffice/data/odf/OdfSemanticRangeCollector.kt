package com.makerandreas.papirusoffice.data.odf

import com.makerandreas.papirusoffice.data.BodyElementRange
import com.makerandreas.papirusoffice.data.DocumentIndexEntry
import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.DocumentIndexRange
import com.makerandreas.papirusoffice.data.DocumentSectionRange
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.SectionDisplay
import java.net.URLDecoder
import java.util.Locale

/**
 * Plan 7C: collects authored index and named section ranges while their
 * paragraphs keep flowing into the normal body element list.
 *
 * Ranges are half-open over [SvXMLImport.elements] and are captured from the
 * element count at the start and end tag of each `text:section` and index
 * element (ODF 1.4 part 3, 5.4 and 8). Entry metadata is derived only from the
 * rendered `text:index-body` snapshot; nothing is regenerated from sources.
 */
internal class OdfSemanticRangeCollector(
    private val elementCount: () -> Int,
    private val elementAt: (Int) -> OfficeDocumentElement,
    /** Parent style name for a style, or null when the chain ends. */
    private val parentStyleOf: (String) -> String?
) {
    private class OpenIndex(
        val kind: DocumentIndexKind,
        val name: String?,
        val styleName: String?,
        val isProtected: Boolean,
        val start: Int
    ) {
        /** Entry style name (lowercase) to template outline level. */
        val templateLevels = LinkedHashMap<String, Int>()
        var titleStart: Int? = null
        var titleEnd: Int? = null
    }

    private class OpenSection(
        val id: String?,
        val name: String,
        val styleName: String?,
        val display: SectionDisplay,
        val condition: String?,
        val isProtected: Boolean,
        val protectionKey: String?,
        val start: Int,
        val depth: Int
    )

    private val openIndexes = ArrayDeque<OpenIndex>()
    private val openSections = ArrayDeque<OpenSection?>()
    private val indexes = mutableListOf<DocumentIndexRange>()
    private val sections = mutableListOf<DocumentSectionRange>()
    private var indexOrdinal = 0
    private var sectionOrdinal = 0

    val isInsideIndex: Boolean get() = openIndexes.isNotEmpty()

    fun reset() {
        openIndexes.clear()
        openSections.clear()
        indexes.clear()
        sections.clear()
        indexOrdinal = 0
        sectionOrdinal = 0
    }

    fun authoredIndexes(): List<DocumentIndexRange> = indexes.sortedBy { it.bodyRange.startInclusive }

    fun namedSections(): List<DocumentSectionRange> = sections.sortedWith(
        compareBy<DocumentSectionRange> { it.bodyRange.startInclusive }.thenBy { it.depth }
    )

    fun beginIndex(kind: DocumentIndexKind, attributes: Map<String, String>) {
        openIndexes.addLast(
            OpenIndex(
                kind = kind,
                name = attributes.attr("text:name"),
                styleName = attributes.attr("text:style-name"),
                isProtected = attributes.attr("text:protected").equals("true", ignoreCase = true),
                start = elementCount()
            )
        )
    }

    fun recordEntryTemplate(attributes: Map<String, String>) {
        val open = openIndexes.lastOrNull() ?: return
        val style = attributes.attr("text:style-name") ?: return
        val level = attributes.attr("text:outline-level")?.toIntOrNull()?.takeIf { it > 0 } ?: return
        open.templateLevels.putIfAbsent(style.lowercase(Locale.ROOT), level)
    }

    fun beginIndexTitle() {
        val open = openIndexes.lastOrNull() ?: return
        if (open.titleStart == null) open.titleStart = elementCount()
    }

    fun endIndexTitle() {
        val open = openIndexes.lastOrNull() ?: return
        if (open.titleStart != null && open.titleEnd == null) open.titleEnd = elementCount()
    }

    fun endIndex() {
        val open = openIndexes.removeLastOrNull() ?: return
        val end = elementCount()
        val title = open.titleStart?.let { s -> BodyElementRange(s, (open.titleEnd ?: end).coerceAtLeast(s)) }
        val entries = ArrayList<DocumentIndexEntry>()
        for (i in open.start until end) {
            if (title != null && i in title) continue
            val paragraph = elementAt(i) as? OfficeDocumentElement.Paragraph ?: continue
            entryFrom(i, paragraph, open.templateLevels)?.let { entries.add(it) }
        }
        indexOrdinal++
        indexes.add(
            DocumentIndexRange(
                id = "index_$indexOrdinal",
                kind = open.kind,
                name = open.name,
                styleName = open.styleName,
                bodyRange = BodyElementRange(open.start, end),
                entries = entries,
                isProtected = open.isProtected,
                titleRange = title
            )
        )
    }

    /**
     * Opens a section scope. A section without `text:name` (required by
     * ODF 1.4 part 3, 19.876.13) still pushes a placeholder so nesting
     * depth stays balanced, but produces no range.
     */
    fun beginSection(attributes: Map<String, String>) {
        val name = attributes.attr("text:name")?.trim()?.takeIf { it.isNotEmpty() }
        if (name == null) {
            openSections.addLast(null)
            return
        }
        val display = when (attributes.attr("text:display")?.lowercase(Locale.ROOT)) {
            "none" -> SectionDisplay.HIDDEN
            "condition" -> SectionDisplay.CONDITIONAL
            else -> SectionDisplay.VISIBLE
        }
        sectionOrdinal++
        openSections.addLast(
            OpenSection(
                id = "section_$sectionOrdinal",
                name = name,
                styleName = attributes.attr("text:style-name"),
                display = display,
                condition = attributes.attr("text:condition")?.takeIf { display == SectionDisplay.CONDITIONAL },
                isProtected = attributes.attr("text:protected").equals("true", ignoreCase = true),
                protectionKey = attributes.attr("text:protection-key"),
                start = elementCount(),
                depth = openSections.count { it != null }
            )
        )
    }

    fun endSection() {
        val open = openSections.removeLastOrNull() ?: return
        val parent = openSections.lastOrNull { it != null }
        sections.add(
            DocumentSectionRange(
                id = requireNotNull(open.id),
                name = open.name,
                bodyRange = BodyElementRange(open.start, elementCount()),
                styleName = open.styleName,
                parentId = parent?.id,
                depth = open.depth,
                display = open.display,
                condition = open.condition,
                isProtected = open.isProtected,
                protectionKey = open.protectionKey
            )
        )
    }

    private fun entryFrom(
        elementIndex: Int,
        paragraph: OfficeDocumentElement.Paragraph,
        templateLevels: Map<String, Int>
    ): DocumentIndexEntry? {
        val raw = paragraph.text
        if (raw.isBlank()) return null
        val lastTab = raw.lastIndexOf('\t')
        val label = if (lastTab >= 0) raw.substring(lastTab + 1).trim().takeIf { it.isNotEmpty() } else null
        val body = if (lastTab >= 0) raw.substring(0, lastTab) else raw
        val text = body.replace('\t', ' ').replace(WHITESPACE, " ").trim()
        if (text.isEmpty()) return null
        val anchor = paragraph.runs.firstNotNullOfOrNull { run ->
            run.hyperlink?.takeIf { it.startsWith("#") && it.length > 1 }
        }?.let { decodeAnchor(it.substring(1)) }
        return DocumentIndexEntry(
            elementIndex = elementIndex,
            level = levelFor(paragraph.styleName, templateLevels),
            targetAnchor = anchor,
            displayedPageLabel = label,
            text = text
        )
    }

    /** Walks the paragraph style chain until a style named by an entry template is found. */
    private fun levelFor(styleName: String?, templateLevels: Map<String, Int>): Int? {
        if (templateLevels.isEmpty()) return null
        val seen = HashSet<String>(8)
        var current = styleName
        var depth = 0
        while (current != null && depth < 16 && seen.add(current.lowercase(Locale.ROOT))) {
            templateLevels[current.lowercase(Locale.ROOT)]?.let { return it }
            current = parentStyleOf(current)
            depth++
        }
        return null
    }

    private fun decodeAnchor(value: String): String {
        if (!value.contains('%')) return value
        return runCatching { URLDecoder.decode(value.replace("+", "%2B"), "UTF-8") }.getOrDefault(value)
    }

    private fun Map<String, String>.attr(qualified: String): String? =
        this[qualified] ?: this[qualified.substringAfter(':')]

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
