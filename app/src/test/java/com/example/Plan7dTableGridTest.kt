package com.example

import com.makerandreas.papirusoffice.data.OfficeTable
import com.makerandreas.papirusoffice.data.OfficeTableCell
import com.makerandreas.papirusoffice.data.OfficeTableColumnSpec
import com.makerandreas.papirusoffice.data.OfficeTableRow
import com.makerandreas.papirusoffice.data.TableCellOccupancy
import com.makerandreas.papirusoffice.data.TableGridResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Plan7dTableGridTest {
    @Test
    fun horizontalAndVerticalSpansUseOneOriginAndCoveredSlots() {
        val table = OfficeTable(
            numColumns = 3,
            columns = listOf(OfficeTableColumnSpec(repeatCount = 3, sourceColumnOrdinal = 0)),
            rows = listOf(
                OfficeTableRow(
                    sourceRowOrdinal = 0,
                    cells = listOf(
                        OfficeTableCell("A", columnSpan = 2, rowSpan = 2, sourceCellOrdinal = 0),
                        OfficeTableCell("", occupancy = TableCellOccupancy.COVERED, sourceCellOrdinal = 1),
                        OfficeTableCell("C", sourceCellOrdinal = 2)
                    )
                ),
                OfficeTableRow(
                    sourceRowOrdinal = 1,
                    cells = listOf(
                        OfficeTableCell("", occupancy = TableCellOccupancy.COVERED, sourceCellOrdinal = 0),
                        OfficeTableCell("", occupancy = TableCellOccupancy.COVERED, sourceCellOrdinal = 1),
                        OfficeTableCell("D", sourceCellOrdinal = 2)
                    )
                )
            )
        )

        val grid = TableGridResolver.resolve(table)
        assertEquals(2, grid.rowCount)
        assertEquals(3, grid.columnCount)
        assertEquals("A", grid.slotAt(0, 0)?.cell?.sourceCell?.text)
        assertEquals(TableCellOccupancy.COVERED, grid.slotAt(0, 1)?.occupancy)
        assertEquals("A", grid.slotAt(0, 1)?.cell?.sourceCell?.text)
        assertEquals(TableCellOccupancy.COVERED, grid.slotAt(1, 0)?.occupancy)
        assertEquals(TableCellOccupancy.COVERED, grid.slotAt(1, 1)?.occupancy)
        assertEquals("A", grid.slotAt(1, 1)?.cell?.sourceCell?.text)
        assertEquals("D", grid.slotAt(1, 2)?.cell?.sourceCell?.text)
        assertEquals(3, grid.originCells().size)
        assertTrue(grid.diagnostics.isEmpty())
    }

    @Test
    fun highCellRepeatsRemainCompactInTheSourceAndExpandWithoutTheOldSmallClamp() {
        val table = OfficeTable(
            numColumns = 500,
            rows = listOf(
                OfficeTableRow(
                    sourceRowOrdinal = 0,
                    cells = listOf(OfficeTableCell("x", repeatCount = 500, sourceCellOrdinal = 0))
                )
            )
        )

        val grid = TableGridResolver.resolve(table)
        assertEquals(500, grid.columnCount)
        assertEquals(500, grid.originCells().size)
        assertEquals(499, grid.originCells().last().key.sourceRepeatIndex)
    }

    @Test
    fun repeatedSourceRowsKeepDistinctOriginKeys() {
        val grid = TableGridResolver.resolve(
            OfficeTable(
                numColumns = 1,
                rows = listOf(
                    OfficeTableRow(
                        sourceRowOrdinal = 2,
                        repeatCount = 3,
                        cells = listOf(OfficeTableCell("row", sourceCellOrdinal = 4))
                    )
                )
            )
        )

        assertEquals(3, grid.rowCount)
        assertEquals(3, grid.originCells().size)
        assertEquals(setOf(0, 1, 2), grid.originCells().map { it.key.sourceRowRepeatIndex }.toSet())
    }

    @Test
    fun orphanCoveredCellIsSafeAndDiagnostic() {
        val grid = TableGridResolver.resolve(
            OfficeTable(
                numColumns = 1,
                rows = listOf(
                    OfficeTableRow(
                        sourceRowOrdinal = 0,
                        cells = listOf(OfficeTableCell("", occupancy = TableCellOccupancy.COVERED, sourceCellOrdinal = 0))
                    )
                )
            )
        )

        assertEquals(TableCellOccupancy.COVERED, grid.slotAt(0, 0)?.occupancy)
        assertTrue(grid.diagnostics.any { it.code.name == "MISSING_GRID_SLOT" })
    }
}
