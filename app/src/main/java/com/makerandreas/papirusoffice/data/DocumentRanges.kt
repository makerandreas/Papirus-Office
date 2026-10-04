package com.makerandreas.papirusoffice.data

/**
 * Plan 7C range integrity (audit-015 F-5, F-6, F-8).
 *
 * Index and section sidecars address body elements by position, so every
 * transformation that changes the element list must either remap them or
 * drop the ones it can no longer vouch for. Save stays refused for documents
 * that carry them (OdtDocumentWriter), so dropping only affects navigation.
 */
object DocumentRanges {

    /**
     * Remaps a half-open range through [oldToNew], where each old index maps to
     * its new index or -1 when the element was dropped. Returns null when the
     * old range is no longer representable.
     */
    fun remap(range: BodyElementRange, oldToNew: IntArray, newSize: Int): BodyElementRange? {
        if (range.endExclusive > oldToNew.size) return null
        val kept = (range.startInclusive until range.endExclusive).map { oldToNew[it] }.filter { it >= 0 }
        if (kept.isNotEmpty()) return BodyElementRange(kept.first(), kept.last() + 1)
        // Empty or fully dropped: anchor at the first surviving element at or after the old start.
        var anchor = newSize
        for (i in range.startInclusive until oldToNew.size) {
            if (oldToNew[i] >= 0) {
                anchor = oldToNew[i]
                break
            }
        }
        return BodyElementRange(anchor, anchor)
    }

    fun remapIndexes(indexes: List<DocumentIndexRange>, oldToNew: IntArray, newSize: Int): List<DocumentIndexRange> =
        indexes.mapNotNull { index ->
            val body = remap(index.bodyRange, oldToNew, newSize) ?: return@mapNotNull null
            val title = index.titleRange?.let { remap(it, oldToNew, newSize) }
                ?.takeIf { it.startInclusive >= body.startInclusive && it.endExclusive <= body.endExclusive }
            val entries = index.entries.mapNotNull { entry ->
                oldToNew.getOrNull(entry.elementIndex)?.takeIf { it >= 0 }?.let { entry.copy(elementIndex = it) }
            }.filter { it.elementIndex in body }
            index.copy(bodyRange = body, titleRange = title, entries = entries)
        }

    fun remapSections(sections: List<DocumentSectionRange>, oldToNew: IntArray, newSize: Int): List<DocumentSectionRange> {
        val remapped = sections.mapNotNull { section ->
            remap(section.bodyRange, oldToNew, newSize)?.let { section.copy(bodyRange = it) }
        }
        return dropOrphanParents(remapped)
    }

    /** Drops sidecars that point outside a body of [size] elements. */
    fun validIndexes(indexes: List<DocumentIndexRange>, size: Int): List<DocumentIndexRange> =
        indexes.filter { it.bodyRange.endExclusive <= size }

    fun validSections(sections: List<DocumentSectionRange>, size: Int): List<DocumentSectionRange> =
        dropOrphanParents(sections.filter { it.bodyRange.endExclusive <= size })

    private fun dropOrphanParents(sections: List<DocumentSectionRange>): List<DocumentSectionRange> {
        val ids = sections.mapTo(HashSet()) { it.id }
        return sections.map { if (it.parentId != null && it.parentId !in ids) it.copy(parentId = null) else it }
    }

    /**
     * First element a jump into [range] should land on: skips leading page
     * breaks (a range may begin with one, F-8). An empty range resolves to
     * the nearest valid element. Null only for an empty body.
     */
    fun firstNavigableIndex(range: BodyElementRange, elements: List<OfficeElement>): Int? {
        if (elements.isEmpty()) return null
        for (i in range.startInclusive until minOf(range.endExclusive, elements.size)) {
            if (elements[i] !is OfficePageBreak) return i
        }
        return nearestValidIndex(range.startInclusive, elements.size)
    }

    /**
     * Nearest position outside [range] for content that must not be shown
     * (hidden sections): the element right after the range, else the one
     * right before it, else null when the range covers the whole body.
     */
    fun nearestOutside(range: BodyElementRange, size: Int): Int? = when {
        range.endExclusive < size -> range.endExclusive
        range.startInclusive > 0 -> range.startInclusive - 1
        else -> null
    }

    fun nearestValidIndex(index: Int, size: Int): Int? = if (size <= 0) null else index.coerceIn(0, size - 1)

    /** Smallest index range containing [elementIndex]; nested indexes resolve to the inner one. */
    fun innermostIndexAt(indexes: List<DocumentIndexRange>, elementIndex: Int): DocumentIndexRange? =
        indexes.filter { elementIndex in it.bodyRange }
            .minByOrNull { it.bodyRange.endExclusive - it.bodyRange.startInclusive }

    /** Deepest section containing [elementIndex]. */
    fun innermostSectionAt(sections: List<DocumentSectionRange>, elementIndex: Int): DocumentSectionRange? =
        sections.filter { elementIndex in it.bodyRange }
            .maxWithOrNull(
                compareBy<DocumentSectionRange> { it.depth }
                    .thenByDescending { it.bodyRange.endExclusive - it.bodyRange.startInclusive }
            )
}

/** Returns a copy whose index and section sidecars all fit inside the current body. */
fun OfficeDocument.withValidatedRanges(): OfficeDocument {
    val size = body.elements.size
    val indexes = DocumentRanges.validIndexes(authoredIndexes, size)
    val sections = DocumentRanges.validSections(namedSectionRanges, size)
    if (indexes.size == authoredIndexes.size && sections == namedSectionRanges) return this
    return copy(authoredIndexes = indexes, namedSectionRanges = sections)
}
