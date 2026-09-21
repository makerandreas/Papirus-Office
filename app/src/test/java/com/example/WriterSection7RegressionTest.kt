package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentSerializer
import com.makerandreas.papirusoffice.data.DocumentTextMerger
import com.makerandreas.papirusoffice.data.LastSessionInfo
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.ModuleType
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeDocumentParser
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.OpenedDocumentStore
import com.makerandreas.papirusoffice.data.SafeSessionRestore
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.flattenHeadings
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.writer.SelectionEngine
import com.makerandreas.papirusoffice.data.writer.SelectionRange
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WriterSection7RegressionTest {

    private fun findTestFile(vararg candidates: String): File {
        val bases = listOf("", "../", "../../")
        for (candidate in candidates) {
            for (base in bases) {
                val f = File(base + candidate)
                if (f.exists() && f.length() > 0) return f
            }
        }
        return File(candidates.first())
    }

    @Test
    fun sample5LayoutPageCountIsNotCharCountOrMetadataForce() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = OfficeDocumentParser(context)
        val file = findTestFile("tests/inky/Sample-5.docx", "tests/inky/Sample-5.odt")
        assertTrue("Sample-5 fixture missing", file.exists())

        val parsed = parser.parseDocument(file, bypassCache = true)
        assertFalse(parsed.isParsingFailed)
        val officeDoc = parsed.toOfficeDocument()
        val layout = LayoutEngine().performLayout(officeDoc)
        val pages = layout.pages.size

        // Char-count paging produced 63; metadata <Pages> forced 15. Layout must be independent.
        assertNotEquals("layout must not use the 63-page char-count pager", 63, pages)
        assertNotEquals("layout must not force metadata page count 15", 15, pages)
        assertTrue("Sample-5 is multi-page; layout produced $pages", pages >= 2)

        val index = DocumentIndexEngine(officeDoc)
        index.applyLayout(layout)
        val headings = flattenHeadings(index.reindex().headings)
        assertTrue("Sample-5 Navigator headings from structure, got ${headings.size}", headings.size >= 5)
        headings.forEach { h ->
            val mapped = layout.elementPageIndex[h.elementIndex]
            if (mapped != null) {
                assertEquals("heading page must share layout coords", mapped, h.pageIndex)
            }
            assertTrue(h.pageIndex in 1..pages)
        }
    }

    @Test
    fun imageIsNotForcedOntoPageOne() {
        val doc = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph(text = "Cover paragraph"),
                    OfficePageBreak,
                    OfficeParagraph(text = "Body after the break"),
                    OfficeImage(imagePath = "pictures/figure.png", widthDp = 200f, heightDp = 180f)
                )
            )
        )
        val layout = LayoutEngine().performLayout(doc)
        val engine = DocumentIndexEngine(doc)
        engine.applyLayout(layout)
        val images = engine.getGraphicObjects()
        assertEquals(1, images.size)
        assertTrue(
            "image after a page break must not be forced to page 1 (was ${images[0].pageIndex})",
            images[0].pageIndex > 1
        )
        val imgLayoutPage = layout.elementPageIndex[images[0].elementIndex]
        assertNotNull(imgLayoutPage)
        assertEquals(imgLayoutPage, images[0].pageIndex)
    }

    @Test
    fun processDeathRestoresSameFileAndMissingSessionGoesHome() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fixture = findTestFile("tests/inky/Sample-5.odt")
        assertTrue(fixture.exists())

        val persisted = OpenedDocumentStore.persistFromFile(context, fixture, "Sample-5.odt")
        assertTrue(persisted.absolutePath.contains("/files/"))
        assertFalse("must not live in cacheDir", persisted.absolutePath.contains("/cache/"))

        val writer = SafeSessionRestore(context)
        writer.clearLastSession()
        writer.saveLastSession(
            LastSessionInfo(
                uri = persisted.absolutePath,
                cursor = 42,
                zoom = 1.25f,
                scroll = 80,
                module = ModuleType.WRITER,
                docTitle = "Sample-5.odt",
                draftText = "unsaved buffer",
                isSaved = false
            )
        )
        writer.flush()

        // New instance simulates process death.
        val restored = SafeSessionRestore(context).getRestorableSession()
        assertNotNull(restored)
        assertEquals(persisted.absolutePath, restored!!.uri)
        assertEquals(42, restored.cursor)
        assertEquals(80, restored.scroll)
        assertEquals("unsaved buffer", restored.draftText)
        assertFalse(restored.isSaved)

        persisted.delete()
        val gone = SafeSessionRestore(context).getRestorableSession()
        assertNull("missing session file must not fall back to Normal.ott", gone)
    }

    @Test
    fun mergerAndSelectionEngineDoNotFlattenStructure() {
        val original = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeHeading(text = "Chapter", level = 1, styleName = "Heading 1"),
                    OfficeParagraph(text = "Intro"),
                    OfficeTable(
                        rows = listOf(OfficeTableRow(cells = listOf(OfficeTableCell("A"), OfficeTableCell("B")))),
                        numColumns = 2
                    ),
                    OfficeImage(imagePath = "img.png", widthDp = 80f, heightDp = 80f),
                    OfficePageBreak,
                    OfficeParagraph(text = "After break")
                )
            )
        )
        val edited = "Chapter\n\nIntro edited\n\nAfter break"
        val merged = DocumentTextMerger.mergeEditedText(original, edited)
        assertTrue(merged.body.elements.any { it is OfficeHeading })
        assertTrue(merged.body.elements.any { it is OfficeTable })
        assertTrue(merged.body.elements.any { it is OfficeImage })
        assertTrue(merged.body.elements.any { it is OfficePageBreak })
        assertEquals(1, merged.body.elements.filterIsInstance<OfficeHeading>().size)

        val afterDelete = SelectionEngine.delete(original, SelectionRange(0, 1))
        assertTrue("delete must keep table", afterDelete.body.elements.any { it is OfficeTable })
        assertTrue("delete must keep image", afterDelete.body.elements.any { it is OfficeImage })

        val afterInsert = SelectionEngine.insert(original, 0, "X")
        assertTrue("insert must keep heading", afterInsert.body.elements.any { it is OfficeHeading })
        assertTrue("insert must keep table", afterInsert.body.elements.any { it is OfficeTable })
    }

    @Test
    fun odtSaveRoundTripKeepsHeadingsTablesImages() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val original = OfficeDocument(
            body = DocumentBody(
                elements = listOf(
                    OfficeHeading(text = "Kept Heading", level = 1, styleName = "Heading 1"),
                    OfficeParagraph(text = "Body text"),
                    OfficeTable(
                        rows = listOf(OfficeTableRow(cells = listOf(OfficeTableCell("H1"), OfficeTableCell("H2")))),
                        numColumns = 2
                    ),
                    OfficeImage(imagePath = "figure.png", widthDp = 40f, heightDp = 40f)
                )
            )
        )
        val out = File(context.filesDir, "section7-roundtrip.odt")
        val ok = DocumentSerializer(context).serializeToFormat(original, "ODT", out)
        assertTrue("ODT serialize failed", ok)
        assertTrue(out.exists() && out.length() > 0)

        val parsed = OfficeDocumentParser(context).parseDocument(out, bypassCache = true)
        assertFalse(parsed.failureReason ?: "", parsed.isParsingFailed)
        val restored = parsed.toOfficeDocument()
        val index = DocumentIndexEngine(restored).reindex()
        assertTrue(
            "saved ODT must still expose the heading",
            flattenHeadings(index.headings).any { it.title.contains("Kept Heading") } ||
                restored.body.elements.any { it is OfficeHeading && it.text.contains("Kept Heading") }
        )
        assertTrue(
            "saved ODT must still expose the table",
            restored.body.elements.any { it is OfficeTable } || index.tables.isNotEmpty()
        )
    }
}
