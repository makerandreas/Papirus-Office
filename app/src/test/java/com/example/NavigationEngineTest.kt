package com.example

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentEngine
import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.DocumentSession
import com.makerandreas.papirusoffice.data.EditingEngine
import com.makerandreas.papirusoffice.data.OfficeBookmark
import com.makerandreas.papirusoffice.data.OfficeComment
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeField
import com.makerandreas.papirusoffice.data.OfficeFootnoteElement
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeHyperlink
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficePageBreak
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeResources
import com.makerandreas.papirusoffice.data.OfficeSection
import com.makerandreas.papirusoffice.data.OfficeShape
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.NavigateBy
import com.makerandreas.papirusoffice.data.navigation.NavigationEngine
import com.makerandreas.papirusoffice.data.navigation.VisibilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationEngineTest {

    private fun createSampleDocument(): OfficeDocument {
        val elements = listOf(
            OfficeHeading(text = "Chapter 1: Introducing Writer", level = 1),
            OfficeParagraph(text = "This is paragraph 1 of chapter 1.", styleName = "Normal"),
            OfficeTable(
                rows = listOf(
                    OfficeTableRow(cells = listOf(OfficeTableCell("Header 1"), OfficeTableCell("Header 2"))),
                    OfficeTableRow(cells = listOf(OfficeTableCell("Data 1"), OfficeTableCell("Data 2")))
                ),
                numColumns = 2
            ),
            OfficeImage(imagePath = "/path/to/sample.png", widthDp = 100f, heightDp = 100f),
            OfficeBookmark(name = "ImportantSection"),
            OfficeHeading(text = "1.1 Using the Navigator", level = 2),
            OfficeComment(author = "Andreas", text = "Check this section carefully.", date = "2026-08-02"),
            OfficePageBreak,
            OfficeHeading(text = "Chapter 2: Advanced Features", level = 1),
            OfficeSection(name = "CustomSection"),
            OfficeShape(type = "Rectangle"),
            OfficeField(type = "PageNumber", value = "2"),
            OfficeFootnoteElement(text = "Footnote explanation.", noteId = "1"),
            OfficeHyperlink(text = "Visit LibreOffice", targetUri = "https://www.libreoffice.org")
        )

        return OfficeDocument(
            body = DocumentBody(elements = elements),
            metadata = DocumentMetadata(author = "Andreas", title = "Papirus User Guide"),
            resources = OfficeResources(objects = listOf("ChartObject1"))
        )
    }

    @Test
    fun testPhase1DocumentIndexEngine() {
        val doc = createSampleDocument()
        val indexEngine = DocumentIndexEngine(doc)
        val index = indexEngine.reindex()

        // 2 root headings (Chapter 1 and Chapter 2)
        assertEquals(2, indexEngine.getDocumentIndex().headings.size)
        // Chapter 1 has 1 child heading (1.1 Using the Navigator)
        assertEquals(1, indexEngine.getDocumentIndex().headings[0].children.size)
        assertEquals(1, indexEngine.getTextTables().size)
        assertEquals("Table1", indexEngine.getTextTables()[0].tableName)
        assertEquals(1, indexEngine.getGraphicObjects().size)
        assertEquals("Image1", indexEngine.getGraphicObjects()[0].imageName)
        assertEquals(1, indexEngine.getBookmarks().size)
        assertEquals("ImportantSection", indexEngine.getBookmarks()[0].name)
        assertEquals(1, index.comments.size)
        assertEquals("Andreas", index.comments[0].author)
        assertEquals(1, indexEngine.getTextSections().size)
        assertEquals("CustomSection", indexEngine.getTextSections()[0].sectionName)
        assertEquals(1, index.shapes.size)
        assertEquals("Shape1", index.shapes[0].shapeName)
        assertEquals(1, indexEngine.getTextFields().size)
        assertEquals(1, indexEngine.getFootnotes().size)
        assertEquals(1, index.hyperlinks.size)
        assertEquals(1, index.oleObjects.size)
        assertEquals("ChartObject1", index.oleObjects[0].oleName)
    }

    @Test
    fun testPhase2And3NavigationEngineJumps() {
        val doc = createSampleDocument()
        val navEngine = NavigationEngine(doc)

        val state = navEngine.state.value
        assertEquals(2, state.index.headings.size)

        // Jump to Heading
        val h1Id = state.index.headings[0].id
        navEngine.goToHeading(h1Id)
        val signal1 = navEngine.state.value.navTargetSignal
        assertNotNull(signal1)
        assertEquals(NavigateBy.HEADING, signal1?.targetType)
        assertEquals("Chapter 1: Introducing Writer", signal1?.titleOrLabel)

        // Jump to Table
        navEngine.goToTable("table_1")
        val signal2 = navEngine.state.value.navTargetSignal
        assertNotNull(signal2)
        assertEquals(NavigateBy.TABLE, signal2?.targetType)
        assertEquals("Table1", signal2?.titleOrLabel)

        // Jump to Bookmark
        navEngine.goToBookmark("ImportantSection")
        val signal3 = navEngine.state.value.navTargetSignal
        assertNotNull(signal3)
        assertEquals(NavigateBy.BOOKMARK, signal3?.targetType)
        assertEquals("ImportantSection", signal3?.titleOrLabel)

        // Jump to Page
        navEngine.goToPage(2)
        assertEquals(2, navEngine.state.value.currentPage)
    }

    @Test
    fun testPhase4PreviousAndNextIteration() {
        val doc = createSampleDocument()
        val navEngine = NavigationEngine(doc)

        navEngine.setNavigateBy(NavigateBy.HEADING)

        // Initial position
        val headings = navEngine.state.value.index.headings
        val h1 = headings[0].id
        val child = headings[0].children[0].id

        navEngine.goToHeading(h1)
        assertEquals(h1, navEngine.state.value.activeItemId)

        // Navigate Next (goes to child heading 1.1 in document order)
        navEngine.next()
        assertEquals(child, navEngine.state.value.activeItemId)

        // Navigate Previous
        navEngine.previous()
        assertEquals(h1, navEngine.state.value.activeItemId)
    }

    @Test
    fun testPhase6HeadingFolding() {
        val doc = createSampleDocument()
        val navEngine = NavigationEngine(doc)

        val h1Id = navEngine.state.value.index.headings[0].id
        assertEquals(false, navEngine.state.value.headingFoldStates[h1Id] ?: false)

        // Toggle fold
        navEngine.toggleHeadingFolding(h1Id)
        assertEquals(true, navEngine.state.value.headingFoldStates[h1Id])
        assertEquals(true, navEngine.state.value.index.headings[0].collapsed)
    }

    @Test
    fun testPhase7HiddenObjectHandling() {
        val doc = createSampleDocument()
        val navEngine = NavigationEngine(doc)

        val tableId = "table_1"
        // Set table to HIDDEN
        navEngine.setObjectVisibility(tableId, VisibilityState.HIDDEN)

        // Attempt jump to hidden table
        navEngine.goToTable(tableId)

        // Expect warning toast signal
        assertEquals("This item is hidden", navEngine.state.value.notificationMessage)
    }

    @Test
    fun testPhase8DocumentSessionIntegration() {
        val doc = createSampleDocument()
        val session = DocumentSession(engine = DocumentEngine(), document = doc, file = null)

        assertNotNull(session.navigationEngine)
        val index = session.navigationEngine.state.value.index
        assertEquals(2, index.headings.size)
        assertEquals(1, index.tables.size)
        assertEquals("Table1", index.tables[0].tableName)
    }
}
