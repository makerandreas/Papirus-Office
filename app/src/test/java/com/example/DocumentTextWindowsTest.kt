package com.example

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentTextMerger
import com.makerandreas.papirusoffice.data.DocumentTextWindow
import com.makerandreas.papirusoffice.data.DocumentTextWindows
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeListItem
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentTextWindowsTest {

    private fun docWithStructure() = OfficeDocument(
        body = DocumentBody(
            elements = listOf(
                OfficeHeading(text = "Title", level = 1),
                OfficeParagraph(text = "First"),
                OfficeImage(imagePath = "img.png"),
                OfficeTable(rows = listOf(OfficeTableRow(listOf(OfficeTableCell("A")))), numColumns = 1),
                OfficeParagraph(text = "Second"),
                OfficeListItem(text = "Bullet point"),
                OfficePageBreak,
                OfficeParagraph(text = "")
            )
        )
    )

    private val globalText = "Title\n\nFirst\n\nSecond\n\nBullet point\n\n"

    @Test
    fun windowsSkipStructuralElementsAndKeepOffsetOrder() {
        val windows = DocumentTextWindows.compute(docWithStructure().body.elements, globalText)

        assertEquals(setOf(0, 1, 4, 5, 7), windows.keys)
        assertEquals(DocumentTextWindow(0, "Title", 0, 5), windows[0])
        assertEquals(DocumentTextWindow(1, "First", 7, 12), windows[1])
        assertEquals(DocumentTextWindow(4, "Second", 14, 20), windows[4])
        assertEquals(DocumentTextWindow(5, "Bullet point", 22, 34), windows[5])
        // Trailing blank paragraph owns the final empty block after the last separator.
        assertEquals(DocumentTextWindow(7, "", 36, 36), windows[7])
    }

    @Test
    fun emptyTextYieldsSingleEmptyWindow() {
        val doc = OfficeDocument(body = DocumentBody(listOf(OfficeParagraph(text = "leftover"))))
        val windows = DocumentTextWindows.compute(doc.body.elements, "")
        assertEquals(listOf(DocumentTextWindow(0, "", 0, 0)), windows.values.toList())
    }

    @Test
    fun localEditMapsIntoGlobalText() {
        val windows = DocumentTextWindows.compute(docWithStructure().body.elements, globalText)
        val window = windows.getValue(4)
        val edited = DocumentTextWindows.applyLocalEdit(globalText, window, "Second edited")
        assertEquals("Title\n\nFirst\n\nSecond edited\n\nBullet point\n\n", edited)
    }

    @Test
    fun caretBoundaryBelongsToEarlierWindow() {
        val windows = DocumentTextWindows.compute(docWithStructure().body.elements, globalText)
        assertEquals(1, DocumentTextWindows.elementForOffset(windows, 12)?.elementIndex)
        assertEquals(4, DocumentTextWindows.elementForOffset(windows, 15)?.elementIndex)
        assertEquals(7, DocumentTextWindows.elementForOffset(windows, globalText.length)?.elementIndex)
        assertNull(DocumentTextWindows.compute(emptyList(), "").values.firstOrNull())
    }

    @Test
    fun windowsStayAlignedWithMergerAfterRoundTrip() {
        val merged = DocumentTextMerger.mergeEditedText(docWithStructure(), globalText)
        val windows = DocumentTextWindows.compute(merged.body.elements, globalText)
        // Every textual element merged from the edited text must own exactly its block.
        windows.values.forEach { window ->
            assertEquals(window.text, globalText.substring(window.start, window.end))
        }
        val rejoined = windows.values.joinToString("\n\n") { it.text }
        assertEquals(globalText, rejoined)
    }

    @Test
    fun localSelectionMapsIntoGlobalRangeWithClamping() {
        val windows = DocumentTextWindows.compute(docWithStructure().body.elements, globalText)
        val window = windows.getValue(4) // "Second" at 14..20

        assertEquals(
            androidx.compose.ui.text.TextRange(15, 18),
            DocumentTextWindows.toGlobalSelection(window, androidx.compose.ui.text.TextRange(1, 4))
        )
        assertEquals(
            androidx.compose.ui.text.TextRange(14, 20),
            DocumentTextWindows.toGlobalSelection(window, androidx.compose.ui.text.TextRange(0, 100))
        )
        assertEquals(
            androidx.compose.ui.text.TextRange(16, 19),
            DocumentTextWindows.toGlobalSelection(window, androidx.compose.ui.text.TextRange(5, 2))
        )
    }
}

class DocumentTextMergerEditTest {

    private fun structuredDoc() = OfficeDocument(
        body = DocumentBody(
            elements = listOf(
                OfficeHeading(text = "Chapter", level = 1),
                OfficeParagraph(text = "Intro"),
                OfficeImage(imagePath = "img.png", widthDp = 80f, heightDp = 80f),
                OfficeParagraph(text = "After break")
            )
        )
    )

    @Test
    fun deletingAllTextClearsTextualElementsInsteadOfResurrectingThem() {
        val merged = DocumentTextMerger.mergeEditedText(structuredDoc(), "")
        val texts = merged.body.elements.mapNotNull {
            when (it) {
                is OfficeHeading -> it.text
                is OfficeParagraph -> it.text
                else -> null
            }
        }
        assertTrue("select-all delete must not resurrect stale text: $texts", texts.all { it.isEmpty() })
        assertTrue("image must survive a delete-all merge", merged.body.elements.any { it is OfficeImage })
    }

    @Test
    fun deletingTrailingParagraphsClearsInsteadOfKeepingStaleText() {
        val merged = DocumentTextMerger.mergeEditedText(structuredDoc(), "Chapter\n\nIntro")
        val trailing = merged.body.elements.filterIsInstance<OfficeParagraph>().last()
        assertEquals("", trailing.text)
    }

    @Test
    fun trailingBlankBlocksBecomeParagraphsSoEnterAtDocumentEndHasATarget() {
        val merged = DocumentTextMerger.mergeEditedText(structuredDoc(), "Chapter\n\nIntro\n\nAfter break\n\n")
        val last = merged.body.elements.last()
        assertTrue(last is OfficeParagraph)
        assertEquals("", (last as OfficeParagraph).text)
    }

    @Test
    fun extraBlocksAppendInOrderAfterStructure() {
        val merged = DocumentTextMerger.mergeEditedText(structuredDoc(), "H\n\nI\n\nJ\n\nK")
        val paragraphs = merged.body.elements.filterIsInstance<OfficeParagraph>().map { it.text }
        assertTrue(paragraphs.containsAll(listOf("J", "K")))
        assertEquals("H", merged.body.elements.filterIsInstance<OfficeHeading>().single().text)
    }
}
