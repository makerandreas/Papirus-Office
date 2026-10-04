package com.example

import com.makerandreas.papirusoffice.data.OfficeDocumentElement
import com.makerandreas.papirusoffice.data.TableCellOccupancy
import com.makerandreas.papirusoffice.data.TableColumnWidthKind
import com.makerandreas.papirusoffice.data.TableVerticalAlignment
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Plan7dTableImportTest {
    @Test
    fun existingOdtCorpusKeepsTheExactTableMatrix() {
        data class Expected(val rows: Int, val columns: Int, val hasHeader: Boolean)
        val expected = mapOf(
            1 to listOf(Expected(5, 3, true), Expected(6, 3, false), Expected(8, 3, false)),
            2 to listOf(Expected(6, 3, true), Expected(13, 3, true)),
            3 to listOf(Expected(8, 5, true)),
            4 to emptyList(),
            5 to emptyList(),
            6 to listOf(Expected(6, 5, true))
        )

        for (sample in SampleMatrix.sampleNumbers) {
            val parsed = OdtImportPipeline().parse(SampleMatrix.findTestFile(SampleMatrix.odtName(sample)))
            assertFalse("Sample-$sample should parse: ${parsed.failureReason}", parsed.isParsingFailed)
            val tables = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>()
            assertEquals("Sample-$sample table count", expected.getValue(sample).size, tables.size)
            expected.getValue(sample).zip(tables).forEachIndexed { index, (shape, table) ->
                assertEquals("Sample-$sample table ${index + 1} rows", shape.rows, table.rows.size)
                assertEquals("Sample-$sample table ${index + 1} columns", shape.columns, table.numColumns)
                assertEquals(
                    "Sample-$sample table ${index + 1} header",
                    shape.hasHeader,
                    table.rows.any { it.isHeader }
                )
            }
        }
    }

    @Test
    fun compactImportPreservesWrappersRepeatsStylesAndCellText() {
        val parsed = OdtImportPipeline().parse(syntheticOdt(), "plan-7d-table.odt")
        assertFalse(parsed.failureReason ?: "synthetic table parse failed", parsed.isParsingFailed)
        assertEquals(3, parsed.elements.size)
        assertEquals(listOf("Before", "After"), parsed.elements.filterIsInstance<OfficeDocumentElement.Paragraph>().map { it.text })

        val table = parsed.elements.filterIsInstance<OfficeDocumentElement.Table>().single()
        assertEquals("SyntheticTable", table.name)
        assertEquals(4, table.numColumns)
        assertEquals(4, table.columns.single().repeatCount)
        assertEquals(TableColumnWidthKind.ABSOLUTE, table.columns.single().width.kind)
        assertEquals(2, table.rows.size)
        assertTrue(table.rows.first().isHeader)
        assertEquals(1, table.rows.first().repeatCount)
        assertEquals(3, table.rows[1].repeatCount)
        assertEquals(2, table.rows[1].cells.single().repeatCount)
        assertEquals(TableCellOccupancy.ORIGIN, table.rows[1].cells.single().occupancy)
        assertEquals(0, table.rows[1].cells.single().sourceCellOrdinal)
        assertEquals(1, table.rows[1].sourceRowOrdinal)

        val bodyCell = table.rows[1].cells.single()
        assertEquals("Body styled linked", bodyCell.text)
        assertEquals(1, bodyCell.paragraphs.size)
        assertEquals("cell-bookmark", bodyCell.paragraphs.single().bookmarks.single())
        assertTrue(bodyCell.paragraphs.single().runs.any { it.hyperlink == "https://example.test" })
        assertEquals(TableVerticalAlignment.MIDDLE, bodyCell.boxStyle.verticalAlignment)
        assertTrue(bodyCell.boxStyle.padding.startUnits > 0f)
        assertNotNull(parsed.styles.tableStyles["TableA"])
        assertNotNull(parsed.styles.tableColumnStyles["ColumnA"])
        assertNotNull(parsed.styles.tableRowStyles["RowA"])
        assertNotNull(parsed.styles.tableCellStyles["CellAuto"])
        assertTrue(parsed.bookmarks.contains("cell-bookmark"))
    }

    private fun syntheticOdt(): ByteArray {
        val styles = """
            <office:document-styles
                xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0">
              <office:styles>
                <style:style style:name="TableA" style:family="table">
                  <style:table-properties style:rel-width="100%"/>
                </style:style>
                <style:style style:name="ColumnA" style:family="table-column">
                  <style:table-column-properties style:column-width="2cm"/>
                </style:style>
                <style:style style:name="RowA" style:family="table-row">
                  <style:table-row-properties fo:min-height="0.4cm" fo:keep-together="always"/>
                </style:style>
                <style:style style:name="CellA" style:family="table-cell">
                  <style:table-cell-properties fo:padding="0.1cm" fo:border="0.5pt solid #000000"/>
                </style:style>
              </office:styles>
            </office:document-styles>
        """.trimIndent()
        val content = """
            <office:document-content
                xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
                xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
                xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
                xmlns:xlink="http://www.w3.org/1999/xlink">
              <office:automatic-styles>
                <style:style style:name="CellAuto" style:family="table-cell">
                  <style:table-cell-properties fo:padding-left="0.2cm" fo:padding-right="0.2cm"
                      fo:border="0.5pt solid #000000" style:vertical-align="middle"/>
                </style:style>
              </office:automatic-styles>
              <office:body><office:text>
                <text:p>Before</text:p>
                <table:table table:name="SyntheticTable" table:style-name="TableA">
                  <table:table-column table:style-name="ColumnA" table:number-columns-repeated="4"/>
                  <table:table-header-rows>
                    <table:table-row table:style-name="RowA">
                      <table:table-cell table:style-name="CellA"><text:p>Header</text:p></table:table-cell>
                      <table:table-cell table:style-name="CellA"><text:p>Two</text:p></table:table-cell>
                      <table:table-cell table:style-name="CellA"><text:p>Three</text:p></table:table-cell>
                      <table:table-cell table:style-name="CellA"><text:p>Four</text:p></table:table-cell>
                    </table:table-row>
                  </table:table-header-rows>
                  <table:table-row table:style-name="RowA" table:number-rows-repeated="3">
                    <table:table-cell table:style-name="CellAuto" table:number-columns-repeated="2">
                      <text:p>Body <text:bookmark-start text:name="cell-bookmark"/>styled <text:a xlink:href="https://example.test">linked</text:a></text:p>
                    </table:table-cell>
                    <table:table-cell><text:p>Three</text:p></table:table-cell>
                    <table:table-cell><text:p>Four</text:p></table:table-cell>
                  </table:table-row>
                </table:table>
                <text:p>After</text:p>
              </office:text></office:body>
            </office:document-content>
        """.trimIndent()
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            fun entry(name: String, value: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(value.toByteArray())
                zip.closeEntry()
            }
            entry("mimetype", "application/vnd.oasis.opendocument.text")
            entry("styles.xml", styles)
            entry("content.xml", content)
        }
        return output.toByteArray()
    }
}
