package com.makerandreas.papirusoffice.data.navigation

import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.DocumentIndexRange
import com.makerandreas.papirusoffice.data.DocumentRanges
import com.makerandreas.papirusoffice.data.DocumentSectionRange

/**
 * Range facts about the element holding the caret, for the status bar's
 * "section or object information" field (WG 26.2 Ch.1 Table 1).
 */
data class StatusRangeContext(
    /** Kind of the innermost authored index containing the caret, if any. */
    val indexKind: DocumentIndexKind? = null,
    /** Name of the innermost named section containing the caret, if any. */
    val sectionName: String? = null
)

/**
 * Plan 7C status resolution, kept free of Android types so it is unit tested
 * (audit-015 F-10). Priority follows LibreOffice's Navigator tracking
 * (`content.cxx` checks the current index before the current section) and
 * `SwView::StateStatusLine`: inside an index only the index type is shown,
 * otherwise the innermost section name joins the existing heading, table or
 * list detail.
 */
object StatusObjectResolver {

    fun resolve(
        indexes: List<DocumentIndexRange>,
        sections: List<DocumentSectionRange>,
        elementIndex: Int
    ): StatusRangeContext {
        if (elementIndex < 0) return StatusRangeContext()
        val index = DocumentRanges.innermostIndexAt(indexes, elementIndex)
        if (index != null) return StatusRangeContext(indexKind = index.kind)
        return StatusRangeContext(sectionName = DocumentRanges.innermostSectionAt(sections, elementIndex)?.name)
    }

    /**
     * Final status text. [indexLabel] and [join] come from string resources in
     * the UI layer ("Table of Contents", "%1$s : %2$s").
     */
    fun compose(
        context: StatusRangeContext,
        detail: String?,
        indexLabel: (DocumentIndexKind) -> String,
        join: (String, String) -> String
    ): String? {
        context.indexKind?.let { return indexLabel(it) }
        val section = context.sectionName?.takeIf { it.isNotBlank() }
        return when {
            section != null && !detail.isNullOrBlank() -> join(section, detail)
            section != null -> section
            else -> detail
        }
    }

    /**
     * Anchor for the FCT "Go to entry..." action at [elementIndex]: the entry's
     * link target when the caret is on a linked index entry and the target is
     * a known bookmark, else null (the action is not offered).
     */
    fun goToEntryAnchor(
        indexes: List<DocumentIndexRange>,
        bookmarkNames: Set<String>,
        elementIndex: Int
    ): String? {
        if (elementIndex < 0) return null
        for (index in indexes) {
            if (elementIndex !in index.bodyRange) continue
            val entry = index.entries.firstOrNull { it.elementIndex == elementIndex } ?: continue
            val anchor = entry.targetAnchor ?: continue
            if (anchor in bookmarkNames) return anchor
        }
        return null
    }
}
