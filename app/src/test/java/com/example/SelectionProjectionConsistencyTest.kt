package com.example

import com.makerandreas.papirusoffice.data.DocxParseResult
import com.makerandreas.papirusoffice.data.DocumentTextMerger
import com.makerandreas.papirusoffice.data.DocumentTextProjection
import com.makerandreas.papirusoffice.data.DocumentTextWindow
import com.makerandreas.papirusoffice.data.DocumentTextWindows
import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.OfficeParsedDocument
import com.makerandreas.papirusoffice.data.TableCell
import com.makerandreas.papirusoffice.data.TableRow
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.toPlainText
import com.makerandreas.papirusoffice.data.writer.SelectionEngine
import com.makerandreas.papirusoffice.data.writer.SelectionRange
import com.makerandreas.papirusoffice.data.writer.commands.DeleteSelectionCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectionProjectionConsistencyTest {

    private val parsedDocument = OfficeParsedDocument(
        elements = listOf(
            OfficeDocumentElement.Heading(text = "Chapter", level = 1),
            OfficeDocumentElement.ListItem(text = "Item", bullet = "•"),
            OfficeDocumentElement.Table(
                rows = listOf(
                    TableRow(cells = listOf(TableCell("A"), TableCell("B")))
                ),
                numColumns = 2
            ),
            OfficeDocumentElement.Paragraph(text = "After")
        ),
        plainText = "Chapter\n\n• Item\n\nA\tB\n\nAfter"
    )

    @Test
    fun initializationSelectionMergeAndWindowsShareTheEditorProjection() {
        val editorText = "Chapter\n\nItem\n\nAfter"
        val document = parsedDocument.toOfficeDocument()
        val parseResult = DocxParseResult(
            text = parsedDocument.plainText,
            parsedDocument = parsedDocument
        )

        assertEquals(editorText, DocumentTextProjection.editorText(parseResult))
        assertEquals(editorText, DocumentTextProjection.editorText(parsedDocument))
        assertEquals(editorText, DocumentTextProjection.editorText(document))
        assertEquals(editorText, DocumentTextProjection.editorText(document.body.elements))
        assertEquals(editorText, DocumentTextMerger.mergeEditedText(document, editorText).let {
            DocumentTextProjection.editorText(it)
        })

        // The broad plain-text view intentionally includes generated labels and table cells;
        // its offsets must not be mixed with the editor's selection coordinates.
        assertEquals("Chapter\n\n• Item\n\nA\tB\n\nAfter", document.toPlainText())
        assertFalse(editorText == document.toPlainText())

        val windows = DocumentTextWindows.compute(document.body.elements, editorText)
        assertEquals(
            DocumentTextWindow(3, "After", editorText.indexOf("After"), editorText.length),
            windows[3]
        )

        val selection = SelectionRange(editorText.indexOf("After"), editorText.indexOf("After") + 1)
        assertEquals("A", SelectionEngine.extract(document, selection))
        val directCut = SelectionEngine.delete(document, selection)
        assertEquals("Chapter\n\nItem\n\nfter", DocumentTextProjection.editorText(directCut))
        assertEquals(editorText, DocumentTextProjection.editorText(SelectionEngine.insert(directCut, selection.min, "A")))

        // Inky supplies its current buffer to the command so model deletion uses those same offsets.
        val command = DeleteSelectionCommand(selection, fullOriginalText = editorText)
        val cutDocument = command.execute(document)
        assertEquals("A", command.actualDeletedText)
        assertEquals("Chapter\n\nItem\n\nfter", DocumentTextProjection.editorText(cutDocument))
        assertTrue(cutDocument.body.elements.any { it is com.makerandreas.papirusoffice.data.OfficeTable })

        val restoredDocument = command.undo(cutDocument)
        assertEquals(editorText, DocumentTextProjection.editorText(restoredDocument))
    }

    @Test
    fun plainTextImportsKeepTheirTextWhenNoStructuredParseIsAvailable() {
        val rawText = "one line\nand another"
        assertEquals(rawText, DocumentTextProjection.editorText(DocxParseResult(rawText)))
    }
}
