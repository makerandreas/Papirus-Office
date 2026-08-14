package com.example

import com.makerandreas.papirusoffice.data.*
import com.makerandreas.papirusoffice.data.util.OfficeDocumentComparator
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentEnginesUnitTest {

    private fun createInitialDocument(): OfficeDocument {
        val paragraph = OfficeParagraph(text = "Hello World", styleName = "Normal")
        return OfficeDocument(
            body = DocumentBody(elements = listOf(paragraph)),
            metadata = DocumentMetadata(author = "Andreas", title = "My Book")
        )
    }

    @Test
    fun testInsertText() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 6) // cursor after 'Hello '
        
        engine.insertText("Beautiful ")
        
        val updatedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello Beautiful World", updatedPara.text)
        assertEquals(16, engine.cursor.offset)
    }

    @Test
    fun testDeleteBackward() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 5) // cursor after 'Hello'
        
        engine.deleteBackward()
        
        val updatedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hell World", updatedPara.text)
        assertEquals(4, engine.cursor.offset)
    }

    @Test
    fun testUndoRedoInsert() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 11) // end of "Hello World"
        
        engine.insertText("!")
        
        var updatedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World!", updatedPara.text)
        
        // Undo
        engine.undo()
        updatedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World", updatedPara.text)
        
        // Redo
        engine.redo()
        updatedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World!", updatedPara.text)
    }

    @Test
    fun testSplitAndMergeParagraphs() = kotlinx.coroutines.runBlocking {
        val doc = createInitialDocument()
        val engine = EditingEngine(document = doc)
        engine.cursor = DocumentCursor(0, 0, 0, 6) // Hello [cursor] World
        
        engine.splitParagraph()
        
        assertEquals(2, engine.document.body.elements.size)
        val para1 = engine.document.body.elements[0].extractParagraph()!!
        val para2 = engine.document.body.elements[1].extractParagraph()!!
        assertEquals("Hello ", para1.text)
        assertEquals("World", para2.text)
        assertEquals(1, engine.cursor.paragraphIndex)
        assertEquals(0, engine.cursor.offset)
        
        // Now merge them back
        engine.mergeParagraphs(0, 1)
        assertEquals(1, engine.document.body.elements.size)
        val mergedPara = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World", mergedPara.text)
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
        val replacedPara = replacedDoc.body.elements[0].extractParagraph()!!
        assertEquals("Hello Everyone", replacedPara.text)
        
        // Test Spell Check (simulated)
        val spellIssues = DocumentServices.runSpellCheck(doc)
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
        
        var para = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World!!", para.text)
        
        // A single undo should rollback both inserts because they are part of the same transaction
        engine.undo()
        para = engine.document.body.elements[0].extractParagraph()!!
        assertEquals("Hello World", para.text)
    }

    @Test
    fun testOutlineFoldingAndReminders() {
        val heading = OfficeParagraph(text = "Intro", styleName = "Heading 1")
        val p1 = OfficeParagraph(text = "Nested text under heading", styleName = "Normal")
        val doc = OfficeDocument(
            body = DocumentBody(elements = listOf(heading, p1))
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
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        assertTrue(bytes.isNotEmpty())

        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip empty doc failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripSingleCharacter() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Char Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "A")))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip single character failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripHello() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Hello Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Hello!")))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
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
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip multiple paragraphs failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripHeadingAndParagraph() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Heading Doc"),
            body = DocumentBody(elements = listOf(
                OfficeHeading(text = "Main Chapter", level = 1, styleName = "Heading_1"),
                OfficeParagraph(text = "Introductory text in main chapter.", styleName = "Standard"),
                OfficeHeading(text = "Sub Section", level = 2, styleName = "Heading_2"),
                OfficeParagraph(text = "Nested text in sub section.", styleName = "Standard")
            ))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
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
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip table failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripBoldText() {
        val run1 = OfficeTextRun(text = "Hello ")
        val run2 = OfficeTextRun(text = "World", isBold = true)
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Bold Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Hello World", runs = listOf(run1, run2))))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip bold text failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripItalicText() {
        val run1 = OfficeTextRun(text = "Papirus ")
        val run2 = OfficeTextRun(text = "Office", isItalic = true)
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Italic Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Papirus Office", runs = listOf(run1, run2))))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip italic text failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripUnderlineText() {
        val run = OfficeTextRun(text = "Underlined Text", isUnderline = true)
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Underline Doc"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Underlined Text", runs = listOf(run))))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip underline text failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripParagraphStyle() {
        val p = OfficeParagraph(text = "Custom Styled Paragraph", styleName = "Body_20_Text")
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Style Doc"),
            body = DocumentBody(elements = listOf(p))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip paragraph style failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripAuthor() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Author Doc", author = "Andreas Maker"),
            body = DocumentBody(elements = listOf(OfficeParagraph(text = "Document with metadata author.")))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip author metadata failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testOdtRoundTripMixedRuns() {
        val run1 = OfficeTextRun(text = "Plain ")
        val run2 = OfficeTextRun(text = "Bold ", isBold = true)
        val run3 = OfficeTextRun(text = "Italic ", isItalic = true)
        val run4 = OfficeTextRun(text = "Underline", isUnderline = true)
        val doc = OfficeDocument(
            metadata = DocumentMetadata(title = "Mixed Runs Doc"),
            body = DocumentBody(elements = listOf(
                OfficeParagraph(text = "Plain Bold Italic Underline", runs = listOf(run1, run2, run3, run4))
            ))
        )
        val writer = OdtDocumentWriter()
        val parser = OdtDocumentParser()

        val bytes = writer.write(doc)
        val restored = parser.parse(bytes)
        val result = OfficeDocumentComparator.compare(doc, restored)
        assertTrue("RoundTrip mixed runs failed: ${result.differences}", result.isSuccess)
    }

    @Test
    fun testDefaultOttDiagnostic() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val assetPath = "templates/styles/Default.ott"
        val inputStream = context.assets.open(assetPath)
        assertNotNull("Asset $assetPath should not be null", inputStream)

        val bytes = inputStream.use { it.readBytes() }
        assertTrue("Asset $assetPath should not be empty", bytes.isNotEmpty())

        var isZip = false
        var hasMimetype = false
        var hasManifest = false
        var hasContentXml = false
        var hasStylesXml = false
        var hasSettingsXml = false
        var hasMetaXml = false

        try {
            java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(bytes)).use { zip ->
                isZip = true
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "mimetype" -> hasMimetype = true
                        "META-INF/manifest.xml" -> hasManifest = true
                        "content.xml" -> hasContentXml = true
                        "styles.xml" -> hasStylesXml = true
                        "settings.xml" -> hasSettingsXml = true
                        "meta.xml" -> hasMetaXml = true
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            isZip = false
        }

        println("=== DIAGNOSTIC TEST: Default.ott ===")
        println("ZIP: ${if (isZip) "PASS" else "FAIL"}")
        println("mimetype: ${if (hasMimetype) "PASS" else "FAIL"}")
        println("manifest.xml: ${if (hasManifest) "PASS" else "FAIL"}")
        println("content.xml: ${if (hasContentXml) "PASS" else "FAIL (expected for empty template styles)"}")
        println("styles.xml: ${if (hasStylesXml) "PASS" else "FAIL"}")
        println("settings.xml: ${if (hasSettingsXml) "PASS" else "FAIL (optional)"}")
        println("meta.xml: ${if (hasMetaXml) "PASS" else "FAIL"}")
        println("====================================")

        assertTrue("Should be a valid ZIP file", isZip)
        assertTrue("Should have mimetype entry", hasMimetype)
        assertTrue("Should have manifest.xml entry", hasManifest)
        assertTrue("Should have styles.xml entry", hasStylesXml)
        assertTrue("Should have meta.xml entry", hasMetaXml)
    }

    @Test
    fun testDefaultOttToNewDocumentAndSaveOdtRoundTrip() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val assetPath = "templates/styles/Default.ott"
        val bytes = context.assets.open(assetPath).use { it.readBytes() }
        
        // 1. Load OTT template into OfficeDocument
        val parser = OdtDocumentParser()
        val templateDoc = parser.parse(bytes)
        assertNotNull("Template OfficeDocument should not be null", templateDoc)

        // 2. Modify OfficeDocument (Insert "Hello!")
        val newElements = templateDoc.body.elements.toMutableList()
        newElements.add(OfficeParagraph(text = "Hello!"))
        newElements.add(OfficeHeading(text = "Template Section", level = 1))
        val modifiedDoc = templateDoc.copy(body = DocumentBody(elements = newElements))

        // 3. Save as ODT bytes via OdtDocumentWriter
        val writer = OdtDocumentWriter()
        val odtBytes = writer.write(modifiedDoc)
        assertTrue("Saved ODT bytes should not be empty", odtBytes.isNotEmpty())

        // 4. Parse saved ODT bytes
        val restoredDoc = parser.parse(odtBytes)

        // 5. Compare structure
        val result = OfficeDocumentComparator.compare(modifiedDoc, restoredDoc)
        assertTrue("Default.ott -> Edit -> ODT Roundtrip failed: ${result.differences}", result.isSuccess)
    }
}
