package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.makerandreas.papirusoffice.data.*
import com.makerandreas.papirusoffice.data.odf.OdfImportDiagnostics
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import com.makerandreas.papirusoffice.data.writer.OdtDocumentParser
import com.makerandreas.papirusoffice.data.writer.OdtDocumentWriter
import com.makerandreas.papirusoffice.data.writer.UnsupportedOdtStructureException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7bSemanticImportTest {
    @Test
    fun runtimeAndCompatibilityFacadeShareOneSemanticResultForEveryOdtFixture() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = OfficeDocumentParser(context)

        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = runtime.parseDocument(file, bypassCache = true)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val runtimeDocument = parsed.toOfficeDocument()
            val compatibilityDocument = OdtDocumentParser().parse(file)

            assertEquals(file.name, semanticSnapshot(runtimeDocument), semanticSnapshot(compatibilityDocument))
            assertEquals(file.name, SampleMatrix.references.getValue(sample).odtPages, runtimeDocument.metadata.pageCount)
            assertEquals(file.name, runtimeDocument.metadata.pageCount, compatibilityDocument.metadata.pageCount)
            assertEquals(file.name, runtimeDocument.metadata.language, compatibilityDocument.metadata.language)
            if (sample == 1) {
                assertEquals(file.name, "id-ID", runtimeDocument.metadata.language)
            } else {
                assertEquals(file.name, "Andreas Maker", runtimeDocument.metadata.author)
            }
            assertEquals(
                file.name,
                runtimeDocument.odtPackageData?.entries?.keys,
                compatibilityDocument.odtPackageData?.entries?.keys
            )
        }
    }

    @Test
    fun sourceFeatureInventoryIsDerivedOnceFromThePackage() {
        val expectedIndexes = setOf(2, 4, 5, 6)
        val expectedTables = setOf(1, 2, 3, 6)
        for (sample in SampleMatrix.sampleNumbers) {
            val file = SampleMatrix.findTestFile(SampleMatrix.odtName(sample))
            val parsed = OdtImportPipeline().parse(file)
            assertFalse("${file.name}: ${parsed.failureReason}", parsed.isParsingFailed)
            val features = requireNotNull(parsed.odtPackageData).sourceFeatures
            assertEquals("${file.name} indexes", sample in expectedIndexes, features.hasAuthoredIndexes)
            assertEquals("${file.name} tables", sample in expectedTables, features.hasAdvancedTables)
            assertFalse("${file.name} has no ODF text:section", features.hasNamedSections)
            assertTrue("${file.name} declares font faces", features.hasFontFaceDeclarations)
        }
    }

    @Test
    fun namedSectionProvenanceBlocksModifiedSave() {
        val bytes = odtPackage(
            mapOf(
                "mimetype" to "application/vnd.oasis.opendocument.text",
                "content.xml" to """
                    <office:document-content
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0">
                      <office:body><office:text>
                        <text:section text:name="Chapter"><text:p>Section body</text:p></text:section>
                      </office:text></office:body>
                    </office:document-content>
                """.trimIndent()
            )
        )
        val parsed = OdtImportPipeline().parse(bytes, "section.odt")
        assertFalse(parsed.isParsingFailed)
        assertTrue(requireNotNull(parsed.odtPackageData).sourceFeatures.hasNamedSections)
        // Plan 7C populates the range; save stays refused either way.
        assertEquals(listOf("Chapter"), parsed.namedSectionRanges.map { it.name })

        val document = parsed.toOfficeDocument().copy(isModified = true)
        val capability = OdtDocumentWriter().saveCapability(document)
        assertFalse(capability.isSupported)
        assertEquals(listOf("named sections"), capability.blockingFeatures)
    }

    @Test
    fun canonicalSidecarsAndTableSemanticsSurviveParsedAdapter() {
        val range = BodyElementRange(0, 1)
        val index = DocumentIndexRange(
            id = "odf-index-0",
            kind = DocumentIndexKind.TABLE_OF_CONTENT,
            name = "Contents",
            bodyRange = range,
            entries = listOf(DocumentIndexEntry(0, 1, "chapter-1", "ii"))
        )
        val section = DocumentSectionRange(
            id = "odf-section-0",
            name = "Protected chapter",
            bodyRange = range,
            depth = 1,
            display = SectionDisplay.CONDITIONAL,
            condition = "chapter-visible",
            isProtected = true
        )
        val column = OfficeTableColumnSpec(
            styleName = "ColumnA",
            width = TableColumnWidthSpec(TableColumnWidthKind.RELATIVE, 2f),
            repeatCount = 2
        )
        val box = TableCellBoxStyle(
            padding = TableInsets(1f, 2f, 3f, 4f),
            backgroundColorHex = "#fff4cc",
            verticalAlignment = TableVerticalAlignment.MIDDLE
        )
        val parsed = OfficeParsedDocument(
            elements = listOf(
                OfficeDocumentElement.Table(
                    rows = listOf(
                        TableRow(
                            cells = listOf(
                                TableCell(
                                    text = "Cell",
                                    paragraphs = listOf(OfficeDocumentElement.Paragraph("Cell")),
                                    startColumn = 1,
                                    columnSpan = 2,
                                    rowSpan = 3,
                                    repeatCount = 4,
                                    styleName = "CellA",
                                    boxStyle = box
                                )
                            ),
                            styleName = "RowA",
                            isHeader = true,
                            repeatCount = 5,
                            rowStyle = TableRowStyle(minimumHeightUnits = 12f, keepTogether = true)
                        )
                    ),
                    numColumns = 3,
                    name = "SemanticTable",
                    columns = listOf(column),
                    styleName = "TableA"
                )
            ),
            isOdt = true,
            authoredIndexes = listOf(index),
            namedSectionRanges = listOf(section),
            styles = DocumentStyles(
                fontFaces = mapOf("Times New Roman1" to OfficeFontFace("Times New Roman1", "Times New Roman"))
            )
        )

        val canonical = parsed.toOfficeDocument()
        assertEquals(listOf(index), canonical.authoredIndexes)
        assertEquals(listOf(section), canonical.namedSectionRanges)
        assertEquals(parsed.styles.fontFaces, canonical.styles.fontFaces)
        val table = canonical.body.elements.single() as OfficeTable
        assertEquals(listOf(column), table.columns)
        assertEquals("TableA", table.styleName)
        val row = table.rows.single()
        assertTrue(row.isHeader)
        assertEquals(5, row.repeatCount)
        assertEquals(12f, row.rowStyle.minimumHeightUnits)
        val cell = row.cells.single()
        assertEquals(1, cell.startColumn)
        assertEquals(2, cell.columnSpan)
        assertEquals(3, cell.rowSpan)
        assertEquals(4, cell.repeatCount)
        assertEquals(box, cell.boxStyle)

        val adaptedBack = canonical.toOfficeParsedDocument("ODT")
        assertEquals(listOf(index), adaptedBack.authoredIndexes)
        assertEquals(listOf(section), adaptedBack.namedSectionRanges)
        assertEquals(parsed.styles.fontFaces, adaptedBack.styles.fontFaces)
        val parsedTable = adaptedBack.elements.single() as OfficeDocumentElement.Table
        assertEquals(listOf(column), parsedTable.columns)
        assertEquals(row.rowStyle, parsedTable.rows.single().rowStyle)
        assertEquals(box, parsedTable.rows.single().cells.single().boxStyle)

        val capability = OdtDocumentWriter().saveCapability(canonical.copy(isModified = true))
        assertFalse(capability.isSupported)
        assertEquals(
            listOf("authored indexes", "named sections", "source table structure"),
            capability.blockingFeatures
        )
    }

    @Test
    fun malformedContentKeepsPackageFailureStateAndStylesOnlyTemplateStaysValid() {
        val diagnostics = RecordingDiagnostics()
        val malformed = odtPackage(
            mapOf(
                "mimetype" to "application/vnd.oasis.opendocument.text",
                "content.xml" to "<office:document-content xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\"><broken>"
            )
        )
        val failed = OdtImportPipeline(diagnostics).parse(malformed, "malformed-content.odt")
        assertTrue(failed.isParsingFailed)
        assertNotNull(failed.odtPackageData)
        assertTrue(diagnostics.malformed.any { it.startsWith("malformed-content.odt:") })

        val template = odtPackage(
            mapOf(
                "mimetype" to "application/vnd.oasis.opendocument.text",
                "styles.xml" to """
                    <office:document-styles
                        xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                        xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0">
                      <office:styles>
                        <style:style style:name="Body" style:family="paragraph"/>
                      </office:styles>
                    </office:document-styles>
                """.trimIndent()
            )
        )
        val blank = OdtImportPipeline().parse(template, "template.ott")
        assertFalse(blank.isParsingFailed)
        assertEquals(listOf(OfficeDocumentElement.Paragraph(text = "")), blank.elements)
        assertTrue(blank.styles.paragraphStyles.containsKey("Body"))
    }

    @Test
    fun unmodifiedSavePreservesContentButModifiedStructuralSaveRefuses() = runBlocking {
        val file = SampleMatrix.findTestFile("Sample-2.odt")
        val imported = OdtDocumentParser().parse(file)
        val originalContent = requireNotNull(imported.odtPackageData?.entries?.get("content.xml"))
        val writer = OdtDocumentWriter()

        assertTrue(writer.saveCapability(imported).isSupported)
        val unmodified = writer.write(imported)
        assertArrayEquals(originalContent, requireNotNull(zipEntry(unmodified, "content.xml")))

        val modified = imported.copy(isModified = true)
        val capability = writer.saveCapability(modified)
        assertFalse(capability.isSupported)
        assertEquals(listOf("authored indexes", "source table structure"), capability.blockingFeatures)
        val error = assertThrows(UnsupportedOdtStructureException::class.java) {
            writer.write(modified)
        }
        assertTrue(error.features.contains("authored indexes"))
        assertTrue(error.features.contains("source table structure"))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val target = File(context.cacheDir, "plan7b-refused-save.odt")
        val sentinel = "keep-existing-file".toByteArray()
        target.writeBytes(sentinel)
        try {
            val saved = DocumentSerializer(context).serializeToFormat(modified, "ODT", target)
            assertFalse(saved)
            assertArrayEquals(sentinel, target.readBytes())
        } finally {
            target.delete()
        }
    }

    @Test
    fun compatibilityFacadeContainsNoSecondXmlParser() {
        val source = locateSource(
            "app/src/main/java/com/makerandreas/papirusoffice/data/writer/OdtDocumentParser.kt"
        ).readText()
        assertTrue(source.contains("OdtImportPipeline"))
        assertFalse(source.contains("XmlPullParser"))
        assertFalse(source.contains("parseContentXml"))
        assertFalse(source.contains("parseStylesFromStream"))

        val semanticImporter = locateSource(
            "app/src/main/java/com/makerandreas/papirusoffice/data/odf/SvXMLImport.kt"
        ).readText()
        assertFalse(semanticImporter.contains("android.content.Context"))
        assertFalse(semanticImporter.contains("DocumentParsingLogger"))
    }

    private fun semanticSnapshot(document: OfficeDocument): List<String> {
        val lines = mutableListOf<String>()
        lines += "metadata:${document.metadata.title}:${document.metadata.author}:${document.metadata.pageCount}"
        lines += "bookmarks:${document.bookmarks.map { it.name }}"
        lines += "styles:${document.styles.paragraphStyles.toSortedMap()}"
        lines += "characters:${document.styles.characterStyles.toSortedMap()}"
        lines += "lists:${document.styles.listStyles.toSortedMap()}"
        document.body.elements.forEachIndexed { index, element ->
            lines += when (element) {
                is OfficeParagraph -> "p:$index:${element.styleName}:${element.text}:${runs(element.runs)}:${element.bookmarks.map { it.name }}"
                is OfficeHeading -> "h:$index:${element.level}:${element.styleName}:${element.text}:${runs(element.runs)}:${element.bookmarks.map { it.name }}"
                is OfficeListItem -> "li:$index:${element.level}:${element.bullet}:${element.styleName}:${element.text}:${runs(element.runs)}:${element.bookmarks.map { it.name }}"
                is OfficeTable -> "table:$index:${element.name}:${element.numColumns}:" + element.rows.joinToString("|") { row ->
                    row.cells.joinToString(";") { cell -> "${cell.text}[${cell.paragraphs.joinToString { it.text }}]" }
                }
                is OfficeImage -> "image:$index:${element.imagePath}:${element.widthDp}:${element.heightDp}:${element.name}"
                is OfficePageBreak -> "break:$index"
                else -> "${element::class.java.simpleName}:$index"
            }
        }
        return lines
    }

    private fun runs(runs: List<OfficeTextRun>): String = runs.joinToString("|") {
        "${it.text}/${it.styleName}/${it.hyperlink}/${it.isBold}/${it.isItalic}/${it.isUnderline}"
    }

    private fun zipEntry(bytes: ByteArray, name: String): ByteArray? {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) return zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    private fun odtPackage(entries: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            for ((name, value) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun locateSource(relative: String): File {
        for (prefix in listOf("", "../", "../../")) {
            val file = File(prefix + relative)
            if (file.exists()) return file
        }
        return File(relative)
    }

    private class RecordingDiagnostics : OdfImportDiagnostics {
        val malformed = mutableListOf<String>()
        override fun unsupportedTag(fileName: String, tagName: String, attributes: Map<String, String>) = Unit
        override fun malformedXml(fileName: String, errorMessage: String, cause: Throwable?) {
            malformed += "$fileName:$errorMessage"
        }
    }
}
