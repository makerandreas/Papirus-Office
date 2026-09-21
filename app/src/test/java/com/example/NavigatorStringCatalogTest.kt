package com.example

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentMetadata
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeHeading
import com.makerandreas.papirusoffice.data.OfficeImage
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeResources
import com.makerandreas.papirusoffice.data.OfficeShape
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.navigation.DocumentIndexEngine
import com.makerandreas.papirusoffice.data.navigation.NavigatorObjectKind
import com.makerandreas.papirusoffice.data.navigation.NavigatorStringCatalog
import com.makerandreas.papirusoffice.data.navigation.flattenHeadings
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorStringCatalogTest {

    @Test
    fun headingTokensMatchEnglishAndIndonesianAndFrench() {
        assertEquals(1, NavigatorStringCatalog.headingLevelFromStyleName("Heading 1"))
        assertEquals(2, NavigatorStringCatalog.headingLevelFromStyleName("Heading_2"))
        assertEquals(1, NavigatorStringCatalog.headingLevelFromStyleName("Judul1"))
        assertEquals(3, NavigatorStringCatalog.headingLevelFromStyleName("Judul 3"))
        assertEquals(2, NavigatorStringCatalog.headingLevelFromStyleName("Bab 2"))
        assertEquals(1, NavigatorStringCatalog.headingLevelFromStyleName("Titre 1"))
        assertEquals(0, NavigatorStringCatalog.headingLevelFromStyleName("Body Text"))
        assertEquals(0, NavigatorStringCatalog.headingLevelFromStyleName("P1"))
    }

    @Test
    fun untitledHeadingAndAutoNamesFollowLocale() {
        assertEquals("Heading 1", NavigatorStringCatalog.ENGLISH.untitledHeading(1))
        assertEquals("Judul 1", NavigatorStringCatalog.INDONESIAN.untitledHeading(1))
        assertEquals("Table1", NavigatorStringCatalog.ENGLISH.autoName(NavigatorObjectKind.TABLE, 1))
        assertEquals("Image1", NavigatorStringCatalog.ENGLISH.autoName(NavigatorObjectKind.IMAGE, 1))
        assertEquals("Chart1", NavigatorStringCatalog.ENGLISH.autoName(NavigatorObjectKind.CHART, 1))
        assertEquals("Object1", NavigatorStringCatalog.ENGLISH.autoName(NavigatorObjectKind.OBJECT, 1))
        assertEquals("Tabel1", NavigatorStringCatalog.INDONESIAN.autoName(NavigatorObjectKind.TABLE, 1))
        assertEquals("Gambar1", NavigatorStringCatalog.INDONESIAN.autoName(NavigatorObjectKind.IMAGE, 1))
        assertEquals("Grafik1", NavigatorStringCatalog.INDONESIAN.autoName(NavigatorObjectKind.CHART, 1))
        assertEquals("Objek1", NavigatorStringCatalog.INDONESIAN.autoName(NavigatorObjectKind.OBJECT, 1))
    }

    @Test
    fun englishDocumentGetsEnglishFallbackNames() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(language = "en-US"),
            body = DocumentBody(
                elements = listOf(
                    OfficeHeading(text = "", level = 1, styleName = "Heading 1"),
                    unnamedTable(),
                    OfficeImage(imagePath = "pic.png"),
                    OfficeShape(type = "Rectangle")
                )
            ),
            resources = OfficeResources(objects = listOf(""))
        )
        val index = DocumentIndexEngine(doc).reindex()
        assertEquals("Heading 1", flattenHeadings(index.headings)[0].title)
        assertEquals("Table1", index.tables[0].tableName)
        assertEquals("Image1", index.images[0].imageName)
        assertEquals("Shape1", index.shapes[0].shapeName)
        assertEquals("Object1", index.oleObjects[0].oleName)
    }

    @Test
    fun indonesianJudulStylesIndexEvenWhenMetadataIsEnglish() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(language = "en-US"),
            styles = DocumentStyles(
                paragraphStyles = mapOf(
                    "P1" to ParagraphStyle(name = "P1", parentStyleName = "Judul1"),
                    "Judul1" to ParagraphStyle(name = "Judul1")
                )
            ),
            body = DocumentBody(
                elements = listOf(
                    OfficeParagraph(text = "Pendahuluan", styleName = "P1"),
                    unnamedTable(),
                    OfficeImage(imagePath = "pic.png"),
                    OfficeShape(type = "Rectangle")
                )
            ),
            resources = OfficeResources(objects = listOf(""))
        )
        val index = DocumentIndexEngine(doc).reindex()
        val headings = flattenHeadings(index.headings)
        assertEquals(1, headings.size)
        assertEquals("Pendahuluan", headings[0].title)
        assertEquals(1, headings[0].outlineLevel)
        assertEquals("Tabel1", index.tables[0].tableName)
        assertEquals("Gambar1", index.images[0].imageName)
        assertEquals("Bentuk1", index.shapes[0].shapeName)
        assertEquals("Objek1", index.oleObjects[0].oleName)
    }

    @Test
    fun embeddedNamesWinOverAutoLocale() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(language = "id-ID"),
            styles = DocumentStyles(
                paragraphStyles = mapOf("Judul1" to ParagraphStyle(name = "Judul1"))
            ),
            body = DocumentBody(
                elements = listOf(
                    OfficeHeading(text = "Bab Satu", level = 1, styleName = "Judul1"),
                    OfficeTable(
                        rows = listOf(OfficeTableRow(cells = listOf(OfficeTableCell("A")))),
                        numColumns = 1,
                        name = "Table1"
                    ),
                    OfficeImage(imagePath = "pic.png", name = "Picture1"),
                    OfficeShape(type = "Rectangle", name = "Shape1")
                )
            ),
            resources = OfficeResources(objects = listOf("ChartObject1"))
        )
        val index = DocumentIndexEngine(doc).reindex()
        assertEquals("Bab Satu", flattenHeadings(index.headings)[0].title)
        assertEquals("Table1", index.tables[0].tableName)
        assertEquals("Picture1", index.images[0].imageName)
        assertEquals("Shape1", index.shapes[0].shapeName)
        assertEquals("ChartObject1", index.oleObjects[0].oleName)
    }

    @Test
    fun idLanguageMetadataAloneSelectsIndonesianAutoNames() {
        val doc = OfficeDocument(
            metadata = DocumentMetadata(language = "id-ID"),
            body = DocumentBody(
                elements = listOf(
                    OfficeHeading(text = "", level = 2),
                    unnamedTable(),
                    OfficeImage(imagePath = "x.png")
                )
            )
        )
        assertEquals("id", NavigatorStringCatalog.detect(doc).languageTag)
        val index = DocumentIndexEngine(doc).reindex()
        assertEquals("Judul 2", flattenHeadings(index.headings)[0].title)
        assertEquals("Tabel1", index.tables[0].tableName)
        assertEquals("Gambar1", index.images[0].imageName)
    }

    @Test
    fun toOfficeDocumentCopiesEmbeddedTableAndImageNames() {
        val parsed = com.makerandreas.papirusoffice.data.OfficeParsedDocument(
            elements = listOf(
                com.makerandreas.papirusoffice.data.OfficeDocumentElement.Table(
                    rows = listOf(
                        com.makerandreas.papirusoffice.data.TableRow(
                            cells = listOf(com.makerandreas.papirusoffice.data.TableCell("A"))
                        )
                    ),
                    numColumns = 1,
                    name = "Tabel1"
                ),
                com.makerandreas.papirusoffice.data.OfficeDocumentElement.ImageElement(
                    imagePath = "Pictures/1.png",
                    name = "Gambar1"
                )
            )
        )
        val office = parsed.toOfficeDocument()
        val table = office.body.elements.filterIsInstance<OfficeTable>().single()
        val image = office.body.elements.filterIsInstance<OfficeImage>().single()
        assertEquals("Tabel1", table.name)
        assertEquals("Gambar1", image.name)
        val index = DocumentIndexEngine(office).reindex()
        assertEquals("Tabel1", index.tables[0].tableName)
        assertEquals("Gambar1", index.images[0].imageName)
    }

    @Test
    fun kindOfStoredNameRecognizesBothLocales() {
        assertEquals(NavigatorObjectKind.TABLE, NavigatorStringCatalog.kindOfStoredName("Table2"))
        assertEquals(NavigatorObjectKind.TABLE, NavigatorStringCatalog.kindOfStoredName("Tabel2"))
        assertEquals(NavigatorObjectKind.IMAGE, NavigatorStringCatalog.kindOfStoredName("Image3"))
        assertEquals(NavigatorObjectKind.IMAGE, NavigatorStringCatalog.kindOfStoredName("Gambar3"))
        assertEquals(NavigatorObjectKind.IMAGE, NavigatorStringCatalog.kindOfStoredName("Picture1"))
        assertEquals(NavigatorObjectKind.CHART, NavigatorStringCatalog.kindOfStoredName("Chart1"))
        assertEquals(NavigatorObjectKind.CHART, NavigatorStringCatalog.kindOfStoredName("Grafik1"))
        assertEquals(NavigatorObjectKind.OBJECT, NavigatorStringCatalog.kindOfStoredName("Object1"))
        assertEquals(NavigatorObjectKind.OBJECT, NavigatorStringCatalog.kindOfStoredName("Objek1"))
    }

    private fun unnamedTable(): OfficeTable {
        return OfficeTable(
            rows = listOf(OfficeTableRow(cells = listOf(OfficeTableCell("A")))),
            numColumns = 1
        )
    }
}
