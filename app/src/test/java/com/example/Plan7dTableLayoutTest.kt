package com.example

import com.makerandreas.papirusoffice.data.DocumentBody
import com.makerandreas.papirusoffice.data.DocumentStyles
import com.makerandreas.papirusoffice.data.OfficeDocument
import com.makerandreas.papirusoffice.data.OfficeParagraph
import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableColumnSpec
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.PageStyleSpec
import com.makerandreas.papirusoffice.data.ParagraphStyle
import com.makerandreas.papirusoffice.data.TableBorder
import com.makerandreas.papirusoffice.data.TableBorderLineStyle
import com.makerandreas.papirusoffice.data.TableColumnWidthKind
import com.makerandreas.papirusoffice.data.TableLayoutEngine
import com.makerandreas.papirusoffice.data.TableVerticalAlignment
import com.makerandreas.papirusoffice.data.LayoutEngine
import com.makerandreas.papirusoffice.data.TableAdvanceSource
import com.makerandreas.papirusoffice.data.toOfficeDocument
import com.makerandreas.papirusoffice.data.odf.OdtImportPipeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Plan7dTableLayoutTest {
    private val styles = DocumentStyles(
        defaultParagraphStyle = ParagraphStyle("Default", fontSizeSp = 12f),
        paragraphStyles = mapOf("Body" to ParagraphStyle("Body", fontSizeSp = 12f))
    )

    @Test
    fun measureUsesDeclaredColumnsCellParagraphsRowMinimumsAndBorderOwnership() {
        val border = TableBorder(1f, TableBorderLineStyle.SOLID, "#112233")
        val table = OfficeTable(
            numColumns = 2,
            columns = listOf(
                OfficeTableColumnSpec(width = com.makerandreas.papirusoffice.data.TableColumnWidthSpec(TableColumnWidthKind.ABSOLUTE, 90f)),
                OfficeTableColumnSpec(width = com.makerandreas.papirusoffice.data.TableColumnWidthSpec(TableColumnWidthKind.ABSOLUTE, 190f))
            ),
            rows = listOf(
                OfficeTableRow(
                    sourceRowOrdinal = 0,
                    rowStyle = com.makerandreas.papirusoffice.data.TableRowStyle(minimumHeightUnits = 28f),
                    cells = listOf(
                        OfficeTableCell(
                            text = "wide origin",
                            paragraphs = listOf(OfficeParagraph("wide origin", "Body")),
                            columnSpan = 2,
                            rowSpan = 2,
                            boxStyle = com.makerandreas.papirusoffice.data.TableCellBoxStyle(
                                borderTop = border,
                                borderEnd = border,
                                borderBottom = border,
                                borderStart = border,
                                verticalAlignment = TableVerticalAlignment.MIDDLE
                            )
                        )
                    )
                ),
                OfficeTableRow(
                    sourceRowOrdinal = 1,
                    cells = listOf(
                        OfficeTableCell("covered", occupancy = com.makerandreas.papirusoffice.data.TableCellOccupancy.COVERED),
                        OfficeTableCell("covered", occupancy = com.makerandreas.papirusoffice.data.TableCellOccupancy.COVERED)
                    )
                )
            )
        )

        val plan = TableLayoutEngine.measure(table, styles, 280f, TableAdvanceSource, null)
        assertEquals(2, plan.columnWidths.size)
        assertEquals(90f, plan.columnWidths[0], 0.01f)
        assertEquals(190f, plan.columnWidths[1], 0.01f)
        assertEquals(2, plan.rows.size)
        assertTrue("minimum row height participates in measurement", plan.rows.first().height >= 28f)
        assertTrue("a spanning cell contributes to the row group", plan.rowGroups.any { it.rowCount == 2 })

        val fragment = plan.fragment(
            elementIndex = 7,
            left = 10f,
            top = 20f,
            rowRefs = listOf(
                com.makerandreas.papirusoffice.data.TableFragmentRowRef(0),
                com.makerandreas.papirusoffice.data.TableFragmentRowRef(1)
            )
        )
        assertEquals(1, fragment.cells.size)
        assertEquals(280f, fragment.cells.single().bounds.right - fragment.cells.single().bounds.left, 0.01f)
        assertTrue("explicit border is emitted once per owned edge", fragment.borderEdges.isNotEmpty())
        assertEquals(TableVerticalAlignment.MIDDLE, fragment.cells.single().style.verticalAlignment)
        assertNotNull(fragment.cells.single().paragraphs.singleOrNull())
    }

    @Test
    fun paginationRepeatsOnlySemanticHeaderAndKeepsBodyCoverageUnique() {
        val rows = buildList {
            add(OfficeTableRow(isHeader = true, sourceRowOrdinal = 0, cells = listOf(OfficeTableCell("Header"))))
            repeat(8) { row ->
                add(
                    OfficeTableRow(
                        sourceRowOrdinal = row + 1,
                        cells = listOf(OfficeTableCell("Body row $row"))
                    )
                )
            }
        }
        val document = OfficeDocument(
            styles = styles,
            body = DocumentBody(listOf(OfficeTable(rows = rows, numColumns = 1)))
        )
        val spec = PageStyleSpec(
            widthDp = 240f,
            heightDp = 92f,
            marginTopDp = 5f,
            marginBottomDp = 5f,
            marginStartDp = 10f,
            marginEndDp = 10f
        )
        val result = LayoutEngine(spec, advanceSource = TableAdvanceSource).performLayout(document)
        assertTrue("table rows should flow onto more than one page", result.pages.size > 1)
        val fragments = result.pages.flatMap { it.elements }.mapNotNull { it.tableFragment }
        assertTrue(fragments.size > 1)
        assertTrue(fragments.drop(1).all { it.repeatedHeaderRows.contains(0) })
        val bodyRows = fragments.flatMap { it.bodyLogicalRows.toList() }
        assertEquals(bodyRows.size, bodyRows.toSet().size)
        assertEquals((1..8).toSet(), bodyRows.toSet())
        assertTrue(fragments.drop(1).all { fragment -> fragment.rows.none { it.logicalRow == 0 && it.isBodyCoverage } })
    }

    @Test
    fun everyOdtSourceTableReachesSharedGeometry() {
        for (sample in listOf(1, 2, 3, 6)) {
            val parsed = OdtImportPipeline().parse(SampleMatrix.findTestFile(SampleMatrix.odtName(sample)))
            assertTrue("Sample-$sample parse failed: ${parsed.failureReason}", !parsed.isParsingFailed)
            val document = parsed.toOfficeDocument()
            val tableIndices = document.body.elements.mapIndexedNotNull { index, element ->
                if (element is OfficeTable) index else null
            }
            assertTrue("Sample-$sample should contain imported tables", tableIndices.isNotEmpty())
            val layout = LayoutEngine(
                parsed.styles.defaultPageStyle ?: PageStyleSpec.FALLBACK,
                advanceSource = TableAdvanceSource
            ).performLayout(document)
            tableIndices.forEach { index ->
                val fragments = layout.pages.flatMap { it.elements }
                    .filter { it.elementIndex == index }
                    .mapNotNull { it.tableFragment }
                assertTrue("Sample-$sample table $index has no geometry", fragments.isNotEmpty())
                assertTrue("Sample-$sample table $index has no origin cells", fragments.any { it.cells.isNotEmpty() })
            }
        }
    }

    @Test
    fun hitTestReturnsLogicalCellIdentityForMergedAndOrdinaryCells() {
        val table = OfficeTable(
            numColumns = 2,
            rows = listOf(
                OfficeTableRow(
                    sourceRowOrdinal = 4,
                    cells = listOf(OfficeTableCell("merged", columnSpan = 2, sourceCellOrdinal = 3))
                ),
                OfficeTableRow(
                    sourceRowOrdinal = 5,
                    cells = listOf(
                        OfficeTableCell("left", sourceCellOrdinal = 0),
                        OfficeTableCell("right", sourceCellOrdinal = 1)
                    )
                )
            )
        )
        val document = OfficeDocument(styles = styles, body = DocumentBody(listOf(table)))
        val pageSpec = PageStyleSpec(
            widthDp = 300f,
            heightDp = 300f,
            marginTopDp = 5f,
            marginBottomDp = 5f,
            marginStartDp = 10f,
            marginEndDp = 10f
        )
        val layoutEngine = LayoutEngine(pageSpec, advanceSource = TableAdvanceSource)
        val result = layoutEngine.performLayout(document)
        val fragment = result.pages.single().elements.single().tableFragment!!
        val merged = fragment.cells.single { it.cell.logicalRow == 0 }
        val hit = layoutEngine.hitTest(
            x = (merged.bounds.left + merged.bounds.right) / 2f,
            y = (merged.bounds.top + merged.bounds.bottom) / 2f,
            pages = result.pages
        )
        assertNotNull(hit)
        assertEquals(0, hit!!.tableRow)
        assertEquals(0, hit.tableColumn)
        assertEquals(3, hit.tableCell?.sourceCellOrdinal)
    }
}
