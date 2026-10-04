package com.makerandreas.papirusoffice.data

import kotlin.math.max

/** Identifies one logical instance of a compact source cell declaration. */
data class TableCellSourceKey(
    val sourceRowOrdinal: Int,
    val sourceCellOrdinal: Int,
    val sourceRepeatIndex: Int,
    /** Repeat ordinal of the compact source row; keeps repeated rows distinct. */
    val sourceRowRepeatIndex: Int = 0
)

data class TableGridColumn(
    val logicalColumn: Int,
    val sourceColumnOrdinal: Int? = null,
    val sourceRepeatIndex: Int = 0,
    val width: TableColumnWidthSpec = TableColumnWidthSpec(),
    val defaultCellStyleName: String? = null
)

data class TableGridCell(
    val key: TableCellSourceKey,
    val sourceCell: OfficeTableCell,
    val logicalRow: Int,
    val logicalColumn: Int,
    val columnSpan: Int,
    val rowSpan: Int,
    val isHeader: Boolean
)

/**
 * One logical slot. A covered slot points at its origin cell; an empty slot has
 * no cell. This makes merged-cell hit testing possible without copying text.
 */
data class TableGridSlot(
    val logicalRow: Int,
    val logicalColumn: Int,
    val occupancy: TableCellOccupancy? = null,
    val cell: TableGridCell? = null,
    val sourceRowOrdinal: Int? = null,
    val sourceCellOrdinal: Int? = null,
    val isHeader: Boolean = false
)

data class TableGridRow(
    val logicalRow: Int,
    val sourceRowOrdinal: Int?,
    val sourceRepeatIndex: Int,
    val isHeader: Boolean,
    val rowStyle: TableRowStyle,
    val slots: List<TableGridSlot>
)

data class TableGrid(
    val tableName: String?,
    val tableStyleName: String?,
    val tableWidth: TableColumnWidthSpec,
    val columns: List<TableGridColumn>,
    val rows: List<TableGridRow>,
    val diagnostics: List<TableDiagnostic> = emptyList()
) {
    val columnCount: Int get() = columns.size
    val rowCount: Int get() = rows.size

    fun slotAt(logicalRow: Int, logicalColumn: Int): TableGridSlot? =
        rows.getOrNull(logicalRow)?.slots?.getOrNull(logicalColumn)

    fun originCells(): List<TableGridCell> = rows
        .asSequence()
        .flatMap { it.slots.asSequence() }
        .filter { it.occupancy == TableCellOccupancy.ORIGIN }
        .mapNotNull { it.cell }
        .distinctBy { it.key }
        .toList()
}

/**
 * Resolves compact ODF declarations into one rectangular logical occupancy
 * grid. It never expands source objects: repeated source cells point to the
 * same declaration and receive a repeat index in [TableCellSourceKey].
 */
object TableGridResolver {
    /** A security budget, deliberately much higher than normal Writer tables. */
    const val MAX_GRID_SLOTS: Long = 2_000_000L
    const val MAX_GRID_ROWS: Int = 100_000
    const val MAX_GRID_COLUMNS: Int = 10_000

    private data class ActiveSpan(
        val cell: TableGridCell,
        val remainingRows: Int
    )

    fun resolve(table: OfficeTable): TableGrid {
        val diagnostics = table.diagnostics.toMutableList()
        val sourceColumns = expandColumns(table.columns, diagnostics)
        val declaredColumnCount = maxOf(sourceColumns.size, table.numColumns).coerceAtMost(MAX_GRID_COLUMNS)
        val rows = mutableListOf<TableGridRow>()
        val active = linkedMapOf<Int, ActiveSpan>()
        var logicalRow = 0
        var slotBudget = MAX_GRID_SLOTS
        var stop = false

        rowLoop@ for ((sourceRowIndex, sourceRow) in table.rows.withIndex()) {
            val sourceRowOrdinal = sourceRow.sourceRowOrdinal.takeIf { it >= 0 } ?: sourceRowIndex
            val rowRepeat = sourceRow.repeatCount.coerceAtLeast(1)
            for (sourceRepeatIndex in 0 until rowRepeat) {
                if (logicalRow >= MAX_GRID_ROWS || slotBudget <= 0L) {
                    diagnostics += TableDiagnostic(
                        TableDiagnosticCode.UNSUPPORTED_DECLARATION,
                        "Table logical-grid budget was reached; remaining repeats were not materialized",
                        sourceRowOrdinal = sourceRowOrdinal
                    )
                    break@rowLoop
                }

                val rowSlots = linkedMapOf<Int, TableGridSlot>()
                val coveredQueue = java.util.TreeSet<Int>()
                active.forEach { (column, span) ->
                    rowSlots[column] = TableGridSlot(
                        logicalRow = logicalRow,
                        logicalColumn = column,
                        occupancy = TableCellOccupancy.COVERED,
                        cell = span.cell,
                        sourceRowOrdinal = span.cell.key.sourceRowOrdinal,
                        sourceCellOrdinal = span.cell.key.sourceCellOrdinal,
                        isHeader = sourceRow.isHeader
                    )
                    coveredQueue += column
                }

                var cursor = 0
                for ((sourceCellIndex, sourceCell) in sourceRow.cells.withIndex()) {
                    val sourceCellOrdinal = sourceCell.sourceCellOrdinal.takeIf { it >= 0 } ?: sourceCellIndex
                    val cellRepeat = sourceCell.repeatCount.coerceAtLeast(1)
                    for (sourceCellRepeatIndex in 0 until cellRepeat) {
                        if (slotBudget <= 0L) {
                            stop = true
                            break
                        }
                        if (sourceCell.occupancy == TableCellOccupancy.COVERED) {
                            val target = coveredQueue.firstOrNull { it >= cursor }
                                ?: coveredQueue.firstOrNull()
                            if (target == null) {
                                val orphanColumn = nextFreeColumn(rowSlots, cursor)
                                rowSlots[orphanColumn] = TableGridSlot(
                                    logicalRow = logicalRow,
                                    logicalColumn = orphanColumn,
                                    occupancy = TableCellOccupancy.COVERED,
                                    sourceRowOrdinal = sourceRowOrdinal,
                                    sourceCellOrdinal = sourceCellOrdinal,
                                    isHeader = sourceRow.isHeader
                                )
                                diagnostics += TableDiagnostic(
                                    TableDiagnosticCode.MISSING_GRID_SLOT,
                                    "Covered cell has no preceding spanning origin",
                                    sourceRowOrdinal = sourceRowOrdinal,
                                    sourceCellOrdinal = sourceCellOrdinal,
                                    logicalRow = logicalRow,
                                    logicalColumn = orphanColumn
                                )
                                cursor = orphanColumn + 1
                            } else {
                                coveredQueue.remove(target)
                                cursor = target + 1
                            }
                            slotBudget--
                            continue
                        }

                        val columnSpan = sourceCell.columnSpan.coerceAtLeast(1)
                        val rowSpan = sourceCell.rowSpan.coerceAtLeast(1)
                        val startColumn = findFreeSpanStart(rowSlots, cursor, columnSpan)
                        val origin = TableGridCell(
                            key = TableCellSourceKey(
                                sourceRowOrdinal = sourceRowOrdinal,
                                sourceCellOrdinal = sourceCellOrdinal,
                                sourceRepeatIndex = sourceCellRepeatIndex,
                                sourceRowRepeatIndex = sourceRepeatIndex
                            ),
                            sourceCell = sourceCell,
                            logicalRow = logicalRow,
                            logicalColumn = startColumn,
                            columnSpan = columnSpan,
                            rowSpan = rowSpan,
                            isHeader = sourceRow.isHeader
                        )
                        val endColumn = startColumn + columnSpan
                        var overlaps = false
                        for (column in startColumn until endColumn) {
                            if (rowSlots.containsKey(column)) overlaps = true
                        }
                        if (overlaps) {
                            diagnostics += TableDiagnostic(
                                TableDiagnosticCode.OVERLAPPING_SPAN,
                                "Cell span overlaps an occupied logical slot; the origin was reduced to one slot",
                                sourceRowOrdinal = sourceRowOrdinal,
                                sourceCellOrdinal = sourceCellOrdinal,
                                logicalRow = logicalRow,
                                logicalColumn = startColumn
                            )
                        }
                        val safeEnd = if (overlaps) startColumn + 1 else endColumn
                        for (column in startColumn until safeEnd) {
                            val occupancy = if (column == startColumn) TableCellOccupancy.ORIGIN else TableCellOccupancy.COVERED
                            rowSlots[column] = TableGridSlot(
                                logicalRow = logicalRow,
                                logicalColumn = column,
                                occupancy = occupancy,
                                cell = origin,
                                sourceRowOrdinal = sourceRowOrdinal,
                                sourceCellOrdinal = sourceCellOrdinal,
                                isHeader = sourceRow.isHeader
                            )
                            if (occupancy == TableCellOccupancy.COVERED) coveredQueue += column
                        }
                        if (rowSpan > 1) {
                            for (column in startColumn until safeEnd) {
                                active[column] = ActiveSpan(origin, rowSpan - 1)
                            }
                        }
                        cursor = safeEnd
                        slotBudget -= max(1, safeEnd - startColumn).toLong()
                    }
                    if (stop) break
                }

                val width = maxOf(declaredColumnCount, (rowSlots.keys.maxOrNull() ?: -1) + 1)
                    .coerceAtMost(MAX_GRID_COLUMNS)
                if (width > declaredColumnCount && declaredColumnCount > 0) {
                    diagnostics += TableDiagnostic(
                        TableDiagnosticCode.UNSUPPORTED_DECLARATION,
                        "Cell occupancy extends beyond declared columns; automatic columns were added",
                        sourceRowOrdinal = sourceRowOrdinal
                    )
                }
                val row = TableGridRow(
                    logicalRow = logicalRow,
                    sourceRowOrdinal = sourceRowOrdinal,
                    sourceRepeatIndex = sourceRepeatIndex,
                    isHeader = sourceRow.isHeader,
                    rowStyle = sourceRow.rowStyle,
                    slots = (0 until width).map { column ->
                        rowSlots[column] ?: run {
                            diagnostics += TableDiagnostic(
                                TableDiagnosticCode.MISSING_GRID_SLOT,
                                "Required logical slot has no source cell",
                                sourceRowOrdinal = sourceRowOrdinal,
                                logicalRow = logicalRow,
                                logicalColumn = column
                            )
                            TableGridSlot(
                                logicalRow = logicalRow,
                                logicalColumn = column,
                                isHeader = sourceRow.isHeader
                            )
                        }
                    }
                )
                rows += row
                logicalRow++

                val nextActive = linkedMapOf<Int, ActiveSpan>()
                // remainingRows counts rows still covered after the row that
                // created the anchor. Keep a zero-count entry for the final
                // covered row, then remove it after that row is consumed.
                active.forEach { (column, span) ->
                    if (span.remainingRows > 0) {
                        nextActive[column] = span.copy(remainingRows = span.remainingRows - 1)
                    }
                }
                active.clear()
                active.putAll(nextActive)
                if (stop) break@rowLoop
            }
        }

        val finalColumnCount = maxOf(
            declaredColumnCount,
            rows.maxOfOrNull { it.slots.size } ?: 0
        ).coerceAtMost(MAX_GRID_COLUMNS)
        val columns = expandColumnsToWidth(sourceColumns, finalColumnCount)
        val normalizedRows = rows.map { row ->
            if (row.slots.size >= finalColumnCount) row else row.copy(
                slots = row.slots + (row.slots.size until finalColumnCount).map { column ->
                    diagnostics += TableDiagnostic(
                        TableDiagnosticCode.MISSING_GRID_SLOT,
                        "Required logical slot has no source cell",
                        sourceRowOrdinal = row.sourceRowOrdinal,
                        logicalRow = row.logicalRow,
                        logicalColumn = column
                    )
                    TableGridSlot(
                        logicalRow = row.logicalRow,
                        logicalColumn = column,
                        isHeader = row.isHeader
                    )
                }
            )
        }
        return TableGrid(
            tableName = table.name,
            tableStyleName = table.styleName,
            tableWidth = table.tableWidth,
            columns = columns,
            rows = normalizedRows,
            diagnostics = diagnostics
        )
    }

    private fun expandColumns(
        sourceColumns: List<OfficeTableColumnSpec>,
        diagnostics: MutableList<TableDiagnostic>
    ): List<TableGridColumn> {
        val result = mutableListOf<TableGridColumn>()
        for (source in sourceColumns) {
            val count = source.repeatCount.coerceAtLeast(1)
            for (repeatIndex in 0 until count) {
                if (result.size >= MAX_GRID_COLUMNS) {
                    diagnostics += TableDiagnostic(
                        TableDiagnosticCode.UNSUPPORTED_DECLARATION,
                        "Table column budget was reached; remaining column repeats were not materialized"
                    )
                    return result
                }
                result += TableGridColumn(
                    logicalColumn = result.size,
                    sourceColumnOrdinal = source.sourceColumnOrdinal.takeIf { it >= 0 },
                    sourceRepeatIndex = repeatIndex,
                    width = source.width,
                    defaultCellStyleName = source.defaultCellStyleName
                )
            }
        }
        return result
    }

    private fun expandColumnsToWidth(
        sourceColumns: List<TableGridColumn>,
        width: Int
    ): List<TableGridColumn> = (0 until width).map { column ->
        sourceColumns.getOrNull(column)?.copy(logicalColumn = column)
            ?: TableGridColumn(logicalColumn = column)
    }

    private fun nextFreeColumn(rowSlots: Map<Int, TableGridSlot>, start: Int): Int {
        var candidate = start.coerceAtLeast(0)
        while (rowSlots.containsKey(candidate)) candidate++
        return candidate
    }

    private fun findFreeSpanStart(
        rowSlots: Map<Int, TableGridSlot>,
        start: Int,
        span: Int
    ): Int {
        var candidate = start.coerceAtLeast(0)
        while (candidate < MAX_GRID_COLUMNS) {
            if ((candidate until candidate + span).all { !rowSlots.containsKey(it) }) return candidate
            candidate = nextFreeColumn(rowSlots, candidate + 1)
        }
        return MAX_GRID_COLUMNS - 1
    }
}
