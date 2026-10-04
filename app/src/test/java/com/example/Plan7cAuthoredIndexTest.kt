package com.example

import com.makerandreas.papirusoffice.data.DocumentIndexKind
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.odf.SvXMLImport
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog
import com.makerandreas.papirusoffice.data.navigation.flattenHeadings
import com.makerandreas.papirusoffice.data.toOfficeDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Plan 7C exit gate for authored indexes (audit-015 section 6).
 *
 * TOC inventory: Sample-2 15/15 linked, Sample-4 21/0, Sample-5 14/14,
 * Sample-6 45/45; Sample-1 and Sample-3 carry no index. Entries are never
 * empty or duplicated, page labels stay authored strings (roman "ii", "iii"),
 * index titles are not headings, and TOC links are grouped under the index.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7cAuthoredIndexTest {

    private data class Expected(val entries: Int, val linked: Int)

    private val expected = mapOf(
        2 to Expected(15, 15),
        4 to Expected(21, 0),
        5 to Expected(14, 14),
        6 to Expected(45, 45)
    )

    @Test
    fun tocInventoryMatchesTheExitGate() {
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val want = expected[sample]
            if (want == null) {
                assertTrue("${file.name} has no authored index", parsed.authoredIndexes.isEmpty())
                continue
            }
            val index = parsed.authoredIndexes.single()
            assertEquals(file.name, DocumentIndexKind.TABLE_OF_CONTENT, index.kind)
            assertEquals(file.name, "Table of Contents1", index.name)
            assertTrue("${file.name} TOC is protected", index.isProtected)
            assertEquals("${file.name} entries", want.entries, index.entries.size)
            assertEquals("${file.name} linked entries", want.linked, index.entries.count { it.targetAnchor != null })
        }
    }

    @Test
    fun entriesAreNonEmptyUniqueAndKeepAuthoredPageLabels() {
        for ((sample, _) in expected) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            val index = parsed.authoredIndexes.single()
            // The fixtures author "Daftar Isi" as a heading before the TOC, not as text:index-title.
            assertNull("${file.name} has no index title", index.titleRange)

            assertEquals("${file.name} duplicate entry rows", index.entries.size, index.entries.map { it.elementIndex }.distinct().size)
            for (entry in index.entries) {
                assertTrue("${file.name} entry text is blank at ${entry.elementIndex}", entry.text.isNotBlank())
                assertFalse("${file.name} entry text keeps a tab", entry.text.contains('\t'))
                assertTrue("${file.name} entry outside index body", entry.elementIndex in index.bodyRange)
                val level = entry.level
                assertNotNull("${file.name} entry '${entry.text}' has no level", level)
                assertTrue("${file.name} entry level $level", level!! in 1..10)
                assertNotNull("${file.name} entry '${entry.text}' has no page label", entry.displayedPageLabel)
                assertTrue(parsed.elements[entry.elementIndex] is OfficeDocumentElement.Paragraph)
            }
            // Front-matter entries keep roman labels as strings, never numbers.
            assertEquals("${file.name} first label", "ii", index.entries[0].displayedPageLabel)
            assertEquals("${file.name} second label", "iii", index.entries[1].displayedPageLabel)
            assertEquals("${file.name} first entry level", 1, index.entries[0].level)
        }
    }

    @Test
    fun indexContentIsNeverPromotedToHeadings() {
        for ((sample, _) in expected) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            val index = parsed.authoredIndexes.single()
            for (i in index.bodyRange.startInclusive until index.bodyRange.endExclusive) {
                assertFalse(
                    "${file.name} element $i inside the TOC became a heading",
                    parsed.elements[i] is OfficeDocumentElement.Heading
                )
            }
            val document = parsed.toOfficeDocument()
            val headings = flattenHeadings(DocumentIndexEngine(document).getDocumentIndex().headings)
            assertTrue(
                "${file.name} Navigator lists a heading inside the TOC",
                headings.none { it.elementIndex in index.bodyRange }
            )
        }
    }

    @Test
    fun navigatorGroupsTocLinksUnderTheIndexAndResolvesTargets() {
        for ((sample, want) in expected) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val document = OdtImportPipeline().parse(file).toOfficeDocument()
            val range = document.authoredIndexes.single()
            val nav = DocumentIndexEngine(document).getDocumentIndex()

            assertEquals(file.name, "ODT", nav.sourceFormat)
            val node = nav.authoredIndexes.single()
            assertEquals(file.name, want.entries, node.entries.size)
            assertTrue("${file.name} index jump lands inside the index", node.elementIndex in range.bodyRange)
            assertFalse("${file.name} index jump lands on a page break", document.body.elements[node.elementIndex] is com.makerandreas.papirusoffice.data.OfficePageBreak)

            assertTrue(
                "${file.name} TOC links must not be listed as hyperlinks",
                nav.hyperlinks.none { it.elementIndex in range.bodyRange && it.url.startsWith("#") }
            )
            val resolved = node.entries.filter { it.targetBookmarkId != null }
            assertEquals("${file.name} entries whose link resolves to a bookmark", want.linked, resolved.size)
            for (entry in resolved) {
                val bookmark = nav.bookmarks.single { it.id == entry.targetBookmarkId }
                assertFalse(
                    "${file.name} '${entry.text}' resolves into the TOC itself",
                    bookmark.elementIndex in range.bodyRange
                )
            }
        }
    }

    @Test
    fun sixFixturesHaveNoNamedSectionsAndSample3StaysTheNegativeControl() {
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertTrue("${file.name} has no text:section", parsed.namedSectionRanges.isEmpty())
        }
        val s3 = OdtImportPipeline().parse(SampleMatrix.findTestFile(SampleMatrix.odtName(3)))
        assertTrue(s3.authoredIndexes.isEmpty())
        assertTrue(s3.namedSectionRanges.isEmpty())
        val nav = DocumentIndexEngine(s3.toOfficeDocument()).getDocumentIndex()
        assertTrue(nav.authoredIndexes.isEmpty())
        assertTrue(nav.sections.isEmpty())
    }

    @Test
    fun synthetic_alphabeticalAndBibliographyIndexesAreRecognised() {
        val parsed = SvXMLImport().parseOdfXml(
            content(
                """
                <text:alphabetical-index text:name="Alpha1">
                  <text:alphabetical-index-source>
                    <text:alphabetical-index-entry-template text:outline-level="1" text:style-name="Index_20_1"/>
                  </text:alphabetical-index-source>
                  <text:index-body>
                    <text:index-title><text:p text:style-name="Index_20_Heading">Index</text:p></text:index-title>
                    <text:p text:style-name="Index_20_1">apple<text:tab/>3</text:p>
                    <text:p text:style-name="Index_20_1"></text:p>
                  </text:index-body>
                </text:alphabetical-index>
                <text:bibliography text:name="Bib1">
                  <text:bibliography-source/>
                  <text:index-body><text:p>[1] Source</text:p></text:index-body>
                </text:bibliography>
                <text:p>After</text:p>
                """
            ),
            "indexes.odt"
        )
        assertFalse(parsed.failureReason, parsed.isParsingFailed)
        assertEquals(listOf(DocumentIndexKind.ALPHABETICAL_INDEX, DocumentIndexKind.BIBLIOGRAPHY), parsed.authoredIndexes.map { it.kind })
        val alpha = parsed.authoredIndexes[0]
        assertEquals(com.makerandreas.papirusoffice.data.BodyElementRange(0, 1), alpha.titleRange)
        assertTrue("index title stays a paragraph", parsed.elements[0] is OfficeDocumentElement.Paragraph)
        assertEquals("blank paragraph is body flow, not an entry", 1, alpha.entries.size)
        assertEquals("apple", alpha.entries[0].text)
        assertEquals("3", alpha.entries[0].displayedPageLabel)
        assertEquals(1, alpha.entries[0].level)
        assertNull(alpha.entries[0].targetAnchor)
        val bib = parsed.authoredIndexes[1]
        assertEquals("[1] Source", bib.entries.single().text)
        assertNull("no tab means no page label", bib.entries.single().displayedPageLabel)
        assertEquals("After", (parsed.elements.last() as OfficeDocumentElement.Paragraph).text)
    }

    @Test
    fun odfStyleNameEscapesAreDecodedBeforeLevelDetection() {
        assertEquals("Heading 1", NavigatorStringCatalog.decodeOdfStyleName("Heading_20_1"))
        assertEquals(1, NavigatorStringCatalog.headingLevelFromStyleName("Heading_20_1"))
        assertEquals(3, NavigatorStringCatalog.headingLevelFromStyleName("Heading_20_3"))
        assertEquals("Plain_name", NavigatorStringCatalog.decodeOdfStyleName("Plain_name"))
    }

    private fun content(body: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <office:document-content xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
            xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
            xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
            xmlns:xlink="http://www.w3.org/1999/xlink">
          <office:body><office:text>
          $body
          </office:text></office:body>
        </office:document-content>
    """.trimIndent()
}
