package com.example

import com.makerandreas.papirusoffice.data.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentEnginesUnitTest {

    private fun createInitialDocument(): OfficeDocument {
        val paragraph = OfficeParagraph(text = "Hello World", styleName = "Normal")
        val elements = listOf(OfficeDocElement.ParagraphElement(paragraph))
        return OfficeDocument(
            body = DocumentBody(elements = elements),
            metadata = DocumentMetadata(author = "Andreas", title = "My Book")
        )
    }

    @Test
    fun testInsertText() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 6) // cursor after 'Hello '
        
        engine.insertText("Beautiful ")
        
        val updatedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello Beautiful World", updatedPara.paragraph.text)
        assertEquals(16, engine.cursor.offset)
    }

    @Test
    fun testDeleteBackward() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 5) // cursor after 'Hello'
        
        engine.deleteBackward()
        
        val updatedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hell World", updatedPara.paragraph.text)
        assertEquals(4, engine.cursor.offset)
    }

    @Test
    fun testUndoRedoInsert() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 11) // end of "Hello World"
        
        engine.insertText("!")
        
        var updatedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World!", updatedPara.paragraph.text)
        
        // Undo
        engine.undo()
        updatedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World", updatedPara.paragraph.text)
        
        // Redo
        engine.redo()
        updatedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World!", updatedPara.paragraph.text)
    }

    @Test
    fun testSplitAndMergeParagraphs() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 6) // Hello [cursor] World
        
        engine.splitParagraph()
        
        assertEquals(2, engine.document.body.elements.size)
        val para1 = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        val para2 = engine.document.body.elements[1] as OfficeDocElement.ParagraphElement
        assertEquals("Hello ", para1.paragraph.text)
        assertEquals("World", para2.paragraph.text)
        assertEquals(1, engine.cursor.paragraphIndex)
        assertEquals(0, engine.cursor.offset)
        
        // Now merge them back
        engine.mergeParagraphs(0, 1)
        assertEquals(1, engine.document.body.elements.size)
        val mergedPara = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World", mergedPara.paragraph.text)
        assertEquals(6, engine.cursor.offset)
    }

    @Test
    fun testCaretNavigation() {
        val doc = createInitialDocument()
        var cursor = DocumentCursor(0, 0, 0, 6) // 'Hello '
        
        cursor = CaretNavigation.moveLeft(doc, cursor)
        assertEquals(5, cursor.offset)
        
        cursor = CaretNavigation.moveRight(doc, cursor)
        assertEquals(6, cursor.offset)
        
        cursor = CaretNavigation.moveWordRight(doc, cursor)
        assertEquals(11, cursor.offset) // end of string
        
        cursor = CaretNavigation.moveWordLeft(doc, cursor)
        assertEquals(6, cursor.offset)
        
        cursor = CaretNavigation.home(cursor)
        assertEquals(0, cursor.offset)
        
        cursor = CaretNavigation.end(doc, cursor)
        assertEquals(11, cursor.offset)
    }

    @Test
    fun testTableEngine() {
        val cell1 = OfficeTableCell("A1")
        val cell2 = OfficeTableCell("B1")
        val initialTable = OfficeTable(rows = listOf(OfficeTableRow(listOf(cell1, cell2))), numColumns = 2)
        
        // Add row
        var table = TableEngine.addRow(initialTable)
        assertEquals(2, table.rows.size)
        
        // Add column
        table = TableEngine.addColumn(table)
        assertEquals(3, table.rows[0].cells.size)
        
        // Set cell text
        table = TableEngine.setCellText(table, 1, 2, "C2")
        assertEquals("C2", table.rows[1].cells[2].text)
    }

    @Test
    fun testFieldEngine() {
        val doc = createInitialDocument()
        val authorField = FieldEngine.evaluateField("author", doc)
        val titleField = FieldEngine.evaluateField("title", doc)
        
        assertEquals("Andreas", authorField)
        assertEquals("My Book", titleField)
    }

    @Test
    fun testDocumentServices() {
        val doc = createInitialDocument()
        
        // Test Search
        val searchResults = DocumentServices.search(doc, "World")
        assertEquals(1, searchResults.size)
        assertEquals(6, searchResults[0].charOffset)
        
        // Test Replace All
        val replacedDoc = DocumentServices.replaceAll(doc, "World", "Everyone")
        val replacedPara = replacedDoc.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello Everyone", replacedPara.paragraph.text)
        
        // Test Spell Check (simulated)
        val spellIssues = DocumentServices.runSpellCheck(doc)
        // 'Hello' and 'World' are in our dictionary, so no issues expected
        assertEquals(0, spellIssues.size)
        
        // Test AutoCorrect
        val autocorrected = DocumentServices.autoCorrect("teh document of paprus")
        assertEquals("the document of papirus", autocorrected)
    }

    @Test
    fun testDocumentTransactions() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 11)
        
        // Start a transaction containing multiple operations
        engine.beginTransaction("Bulk Insert")
        engine.insertText("!")
        engine.insertText("!")
        engine.commitTransaction()
        
        var para = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World!!", para.paragraph.text)
        
        // A single undo should rollback both inserts because they are part of the same transaction
        engine.undo()
        para = engine.document.body.elements[0] as OfficeDocElement.ParagraphElement
        assertEquals("Hello World", para.paragraph.text)
    }

    @Test
    fun testOutlineFoldingAndReminders() {
        val heading = OfficeParagraph(text = "Intro", styleName = "Heading 1")
        val p1 = OfficeParagraph(text = "Nested text under heading", styleName = "Normal")
        val doc = OfficeDocument(
            body = DocumentBody(elements = listOf(
                OfficeDocElement.ParagraphElement(heading),
                OfficeDocElement.ParagraphElement(p1)
            ))
        )
        val outlineEngine = OutlineEngineImpl()
        outlineEngine.buildOutline(doc)
        
        // Element index 1 is the child paragraph of heading at index 0
        assertFalse(outlineEngine.isElementHidden(1))
        
        outlineEngine.toggle(0)
        assertTrue(outlineEngine.isElementHidden(1))
        
        outlineEngine.toggle(0)
        assertFalse(outlineEngine.isElementHidden(1))
        
        val reminderManager = ReminderManager()
        assertTrue(reminderManager.getReminders().isEmpty())
        
        reminderManager.setReminder(1, 10, "Review intro")
        val reminders = reminderManager.getReminders()
        assertEquals(1, reminders.size)
        assertEquals("Review intro", reminders[0].note)
        
        // Navigation previous/next
        val next = reminderManager.nextReminder(1, 5)
        assertNotNull(next)
        assertEquals(10, next?.offset)
        
        val prev = reminderManager.previousReminder(1, 15)
        assertNotNull(prev)
        assertEquals(10, prev?.offset)
    }

    @Test
    fun testOdtRoundTripEmptyDocument() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Empty Doc"),
            body = DocumentBody(elements = emptyList())
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        assertTrue(bytes.isNotEmpty())

        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip empty doc failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripSingleCharacter() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Char Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "A")))
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip single character failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripHello() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Hello Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Hello!")))
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip Hello failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripMultipleParagraphs() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Multi Para"),
            body = DocumentBody(elements = listOf(
                OfficeParagraph(text = "Paragraph 1"),
                OfficeParagraph(text = "Paragraph 2"),
                OfficeParagraph(text = "Paragraph 3")
            ))
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip multiple paragraphs failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripHeadingAndParagraph() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Heading Doc"),
            body = DocumentBody(elements = listOf(
                OfficeHeading(text = "Main Chapter", level = 1),
                OfficeParagraph(text = "Introductory text in main chapter."),
                OfficeHeading(text = "Sub Section", level = 2),
                OfficeParagraph(text = "Nested text in sub section.")
            ))
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip heading & paragraph failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripTable() {
        val table = OfficeTable(
            rows = listOf(
                OfficeTableRow(cells = listOf(OfficeTableCell("Header 1"), OfficeTableCell("Header 2"))),
                OfficeTableRow(cells = listOf(OfficeTableCell("Value 1"), OfficeTableCell("Value 2")))
            ),
            numColumns = 2
        )
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Table Doc"),
            body = DocumentBody(elements = listOf(table))
        )
        val writer = com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter()
        val parser = com.makerandreas.papirusoffice.data.writer.OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip table failed: ${result.differences}", result.isSuccess)
    }
}
