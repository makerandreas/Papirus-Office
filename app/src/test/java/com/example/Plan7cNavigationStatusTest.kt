package com.example

import com.makerandreas.papirusoffice.data.BodyElementRange
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentIndexEntry
import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.DocumentIndexRange
import com.makerandreas.papirusoffice.data.DocumentRanges
import com.makerandreas.papirusoffice.data.DocumentSectionRange
import com.makerandreas.papirusoffice.data.DocumentTextMerger
import com.makerandreas.papirusoffice.data.OfficeComment
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.ParserReport
import com.makerandreas.papirusoffice.data.navigation.NavigatorCategories
import com.makerandreas.papirusoffice.data.navigation.NavigatorCategoryAvailability
import com.makerandreas.papirusoffice.data.navigation.StatusObjectResolver
import com.makerandreas.papirusoffice.data.navigation.StatusRangeContext
import com.makerandreas.papirusoffice.data.toOfficeParsedDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plan 7C status resolution, range integrity and format-aware honesty
 * (audit-015 F-5, F-6, F-8, F-9, F-10).
 */
class Plan7cNavigationStatusTest {

    private val toc = DocumentIndexRange(
        id = "index_1",
        kind = DocumentIndexKind.TABLE_OF_CONTENT,
        bodyRange = BodyElementRange(0, 4),
        titleRange = BodyElementRange(0, 1),
        entries = listOf(
            DocumentIndexEntry(1, 1, "_TOC1", "ii", "Kata Pengantar"),
            DocumentIndexEntry(2, 1, "_Missing", "1", "Bab I"),
            DocumentIndexEntry(3, 2, null, "2", "1.1 Latar")
        )
    )
    private val outer = DocumentSectionRange(id = "section_1", name = "Outer", bodyRange = BodyElementRange(3, 8))
    private val inner = DocumentSectionRange(
        id = "section_2", name = "Inner", bodyRange = BodyElementRange(5, 6), parentId = "section_1", depth = 1
    )

    private fun compose(ctx: StatusRangeContext, detail: String?) = StatusObjectResolver.compose(
        ctx, detail, indexLabel = { "Table of Contents" }, join = { a, b -> "$a : $b" }
    )

    @Test
    fun indexTypeWinsInsideAnIndexOtherwiseInnermostSection() {
        val indexes = listOf(toc)
        val sections = listOf(outer, inner)
        assertEquals(DocumentIndexKind.TABLE_OF_CONTENT, StatusObjectResolver.resolve(indexes, sections, 1).indexKind)
        // Element 3 is inside both the TOC and Outer: LO checks the index first.
        val overlap = StatusObjectResolver.resolve(indexes, sections, 3)
        assertEquals(DocumentIndexKind.TABLE_OF_CONTENT, overlap.indexKind)
        assertNull(overlap.sectionName)
        assertEquals("Inner", StatusObjectResolver.resolve(indexes, sections, 5).sectionName)
        assertEquals("Outer", StatusObjectResolver.resolve(indexes, sections, 6).sectionName)
        assertEquals(StatusRangeContext(), StatusObjectResolver.resolve(indexes, sections, 9))
        assertEquals(StatusRangeContext(), StatusObjectResolver.resolve(indexes, sections, -1))
    }

    @Test
    fun composeShowsIndexTypeOnlyOrJoinsSectionWithDetail() {
        assertEquals("Table of Contents", compose(StatusRangeContext(indexKind = DocumentIndexKind.TABLE_OF_CONTENT), "Heading 1: X"))
        assertEquals("Inner : Heading 1: X", compose(StatusRangeContext(sectionName = "Inner"), "Heading 1: X"))
        assertEquals("Inner", compose(StatusRangeContext(sectionName = "Inner"), null))
        assertEquals("Table", compose(StatusRangeContext(), "Table"))
        assertNull(compose(StatusRangeContext(), null))
    }

    @Test
    fun goToEntryIsOfferedOnlyForEntriesWhoseLinkResolves() {
        val bookmarks = setOf("_TOC1")
        assertEquals("_TOC1", StatusObjectResolver.goToEntryAnchor(listOf(toc), bookmarks, 1))
        assertNull("unresolved anchor", StatusObjectResolver.goToEntryAnchor(listOf(toc), bookmarks, 2))
        assertNull("entry without link", StatusObjectResolver.goToEntryAnchor(listOf(toc), bookmarks, 3))
        assertNull("index title", StatusObjectResolver.goToEntryAnchor(listOf(toc), bookmarks, 0))
        assertNull("outside the index", StatusObjectResolver.goToEntryAnchor(listOf(toc), bookmarks, 6))
    }

    @Test
    fun jumpTargetsSkipPageBreaksAndHiddenContent() {
        val elements = listOf(OfficePageBreak, OfficeParagraph("a"), OfficeParagraph("b"), OfficeParagraph("c"))
        assertEquals(1, DocumentRanges.firstNavigableIndex(BodyElementRange(0, 3), elements))
        assertEquals("empty range resolves to nearest valid element", 2, DocumentRanges.firstNavigableIndex(BodyElementRange(2, 2), elements))
        assertEquals(3, DocumentRanges.firstNavigableIndex(BodyElementRange(9, 9), elements))
        assertNull(DocumentRanges.firstNavigableIndex(BodyElementRange(0, 0), emptyList()))

        val hidden = listOf(BodyElementRange(1, 2), BodyElementRange(2, 3))
        assertEquals("steps over an adjacent hidden range", 3, DocumentRanges.nearestVisible(BodyElementRange(1, 2), hidden, 4))
        assertEquals("falls back before the range at the body end", 0, DocumentRanges.nearestVisible(BodyElementRange(1, 4), hidden, 4))
        assertNull(DocumentRanges.nearestVisible(BodyElementRange(0, 4), hidden, 4))
    }

    @Test
    fun serializerRemapsRangesThroughDroppedElements() {
        val document = OfficeDocument(
            body = DocumentBody(
                listOf(
                    OfficeParagraph("Intro"),
                    OfficeComment(author = "A", text = "dropped by the parsed adapter", date = ""),
                    OfficeParagraph("Body")
                )
            ),
            namedSectionRanges = listOf(DocumentSectionRange(id = "s", name = "S", bodyRange = BodyElementRange(2, 3))),
            authoredIndexes = listOf(
                DocumentIndexRange(
                    id = "i",
                    kind = DocumentIndexKind.TABLE_OF_CONTENT,
                    bodyRange = BodyElementRange(0, 3),
                    entries = listOf(DocumentIndexEntry(2, 1, null, "1", "Body"))
                )
            )
        )
        val parsed = document.toOfficeParsedDocument("ODT")
        assertEquals(2, parsed.elements.size)
        assertEquals(BodyElementRange(1, 2), parsed.namedSectionRanges.single().bodyRange)
        val index = parsed.authoredIndexes.single()
        assertEquals(BodyElementRange(0, 2), index.bodyRange)
        assertEquals(1, index.entries.single().elementIndex)
    }

    @Test
    fun mergerDropsRangesThatNoLongerFitTheBody() {
        val document = OfficeDocument(
            body = DocumentBody(emptyList()),
            namedSectionRanges = listOf(DocumentSectionRange(id = "s", name = "S", bodyRange = BodyElementRange(0, 3)))
        )
        val merged = DocumentTextMerger.mergeEditedText(document, "only one block")
        assertEquals(1, merged.body.elements.size)
        assertTrue(merged.namedSectionRanges.isEmpty())
    }

    @Test
    fun mergerKeepsRangesWhenTheEditStaysInsideOneElement() {
        val document = OfficeDocument(
            body = DocumentBody(listOf(OfficeParagraph("Alpha"), OfficeParagraph("Beta"))),
            namedSectionRanges = listOf(DocumentSectionRange(id = "s", name = "S", bodyRange = BodyElementRange(1, 2)))
        )
        val merged = DocumentTextMerger.mergeEditedText(document, "Alpha\n\nBetter")
        assertEquals(BodyElementRange(1, 2), merged.namedSectionRanges.single().bodyRange)
    }

    @Test
    fun sectionAndIndexEmptyStatesAreReadableForOdtOnly() {
        for (key in listOf("sections", "indexes")) {
            val category = NavigatorCategories.of(key)
            assertEquals(key, NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS, category.availabilityFor("ODT"))
            assertEquals(key, NavigatorCategoryAvailability.NOT_READABLE_YET, category.availabilityFor("DOCX"))
            assertEquals(key, NavigatorCategoryAvailability.NOT_READABLE_YET, category.availabilityFor(ParserReport().format))
        }
        assertEquals(
            "categories without a format restriction ignore the format",
            NavigatorCategoryAvailability.PARSED_DOCUMENT_CLASS,
            NavigatorCategories.of("tables").availabilityFor("DOCX")
        )
    }
}
