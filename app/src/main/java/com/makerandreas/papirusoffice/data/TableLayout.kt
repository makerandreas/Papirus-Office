package com.makerandreas.papirusoffice.data

import kotlin.math.max

private fun resolveTableParagraphStyle(paragraph: OfficeParagraph, styles: DocumentStyles): ParagraphStyle {
    val base = StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
    return base.copy(indentStartUnits = base.indentStartUnits + paragraph.indent)
}

/** A paragraph layout positioned inside one table cell. */
data class TableCellParagraphGeometry(
    val paragraph: OfficeParagraph,
    val layout: ParagraphLayout,
    val bounds: OfficeRect
)

data class TableCellGeometry(
    val cell: TableGridCell,
    val bounds: OfficeRect,
    val contentBounds: OfficeRect,
    val style: TableCellBoxStyle,
    val paragraphs: List<TableCellParagraphGeometry>,
    val paintBackground: Boolean = true
)

data class TableBorderEdgeGeometry(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val border: TableBorder,
    val owner: TableCellSourceKey
)

data class TableFragmentRowGeometry(
    val logicalRow: Int,
    val sourceRowOrdinal: Int?,
    val sourceRepeatIndex: Int,
    val bounds: OfficeRect,
    val isHeader: Boolean,
    val isRepeatedHeader: Boolean,
    val isBodyCoverage: Boolean
)

data class TableFragmentGeometry(
    val elementIndex: Int,
    val tableName: String?,
    val bounds: OfficeRect,
    val rows: List<TableFragmentRowGeometry>,
    val cells: List<TableCellGeometry>,
    val borderEdges: List<TableBorderEdgeGeometry>,
    val repeatedHeaderRows: Set<Int>,
    val bodyLogicalRows: Set<Int>,
    val sourceRowStart: Int?,
    val sourceRowEndExclusive: Int?,
    val diagnostics: List<TableDiagnostic> = emptyList()
)

data class TableMeasuredRow(
    val logicalRow: Int,
    val sourceRowOrdinal: Int?,
    val isHeader: Boolean,
    val style: TableRowStyle,
    val height: Float
)

data class TableRowGroup(
    val startInclusive: Int,
    val endExclusive: Int
) {
    val rowCount: Int get() = endExclusive - startInclusive
}

data class TableMeasuredCell(
    val cell: TableGridCell,
    val requiredHeight: Float,
    val paragraphs: List<Pair<OfficeParagraph, ParagraphLayout>>
)

data class TableLayoutPlan(
    val grid: TableGrid,
    val tableWidth: Float,
    val columnWidths: List<Float>,
    val rows: List<TableMeasuredRow>,
    val rowGroups: List<TableRowGroup>,
    val headerRowCount: Int,
    val measuredCells: Map<TableCellSourceKey, TableMeasuredCell>,
    val styles: DocumentStyles,
    val diagnostics: List<TableDiagnostic>
) {
    val totalHeight: Float get() = rows.sumOf { it.height.toDouble() }.toFloat()

    fun rowHeight(row: Int): Float = rows.getOrNull(row)?.height ?: 0f

    fun heightOf(group: TableRowGroup): Float =
        (group.startInclusive until group.endExclusive).sumOf { rowHeight(it).toDouble() }.toFloat()

    fun heightOfRows(rowIndices: List<Int>): Float = rowIndices.sumOf { rowHeight(it).toDouble() }.toFloat()

    fun fragment(
        elementIndex: Int,
        left: Float,
        top: Float,
        rowRefs: List<TableFragmentRowRef>,
        extraDiagnostics: List<TableDiagnostic> = emptyList()
    ): TableFragmentGeometry {
        if (rowRefs.isEmpty()) {
            val bounds = OfficeRect(left, top, left + tableWidth, top)
            return TableFragmentGeometry(
                elementIndex = elementIndex,
                tableName = grid.tableName,
                bounds = bounds,
                rows = emptyList(),
                cells = emptyList(),
                borderEdges = emptyList(),
                repeatedHeaderRows = emptySet(),
                bodyLogicalRows = emptySet(),
                sourceRowStart = null,
                sourceRowEndExclusive = null,
                diagnostics = diagnostics + extraDiagnostics
            )
        }

        val rowGeometry = mutableListOf<TableFragmentRowGeometry>()
        var y = top
        rowRefs.forEach { ref ->
            val height = rowHeight(ref.logicalRow)
            val row = grid.rows[ref.logicalRow]
            val bounds = OfficeRect(left, y, left + tableWidth, y + height)
            rowGeometry += TableFragmentRowGeometry(
                logicalRow = ref.logicalRow,
                sourceRowOrdinal = row.sourceRowOrdinal,
                sourceRepeatIndex = row.sourceRepeatIndex,
                bounds = bounds,
                isHeader = row.isHeader,
                isRepeatedHeader = ref.isRepeatedHeader,
                isBodyCoverage = ref.isBodyCoverage
            )
            y += height
        }
        val includedRows = rowRefs.map { it.logicalRow }.toSet()
        val cells = mutableListOf<TableCellGeometry>()
        val edgeOwners = linkedMapOf<TableEdgeKey, TableBorderEdgeGeometry>()
        val rowByLogical = rowGeometry.associateBy { it.logicalRow }

        val originCells = grid.originCells().filter { it.logicalRow in includedRows }
        originCells.forEach { origin ->
            val row = rowByLogical[origin.logicalRow] ?: return@forEach
            val x = left + columnWidths.take(origin.logicalColumn).sum()
            val width = columnWidths
                .drop(origin.logicalColumn)
                .take(origin.columnSpan)
                .sum()
                .coerceAtLeast(1f)
            val coveredRows = (origin.logicalRow until (origin.logicalRow + origin.rowSpan))
                .filter { it in includedRows }
            val height = coveredRows.sumOf { rowHeight(it).toDouble() }.toFloat().coerceAtLeast(row.bounds.bottom - row.bounds.top)
            val bounds = OfficeRect(x, row.bounds.top, x + width, row.bounds.top + height)
            val style = origin.sourceCell.boxStyle
            val contentLeft = bounds.left + style.padding.startUnits + style.borderStart.widthUnits
            val contentRight = bounds.right - style.padding.endUnits - style.borderEnd.widthUnits
            val contentTop = bounds.top + style.padding.topUnits + style.borderTop.widthUnits
            val contentBottom = bounds.bottom - style.padding.bottomUnits - style.borderBottom.widthUnits
            val contentBounds = OfficeRect(
                contentLeft,
                contentTop,
                contentRight.coerceAtLeast(contentLeft + 1f),
                contentBottom.coerceAtLeast(contentTop)
            )
            val measured = measuredCells[origin.key]
            val contentHeight = measured?.paragraphs?.sumOf { (paragraph, layout) ->
                val paragraphStyle = resolveTableParagraphStyle(paragraph, styles)
                (layout.height + paragraphStyle.spaceBeforeUnits + paragraphStyle.spaceAfterUnits).toDouble()
            }?.toFloat() ?: 0f
            val available = (contentBounds.bottom - contentBounds.top).coerceAtLeast(0f)
            val verticalOffset = when (style.verticalAlignment) {
                TableVerticalAlignment.MIDDLE -> ((available - contentHeight) / 2f).coerceAtLeast(0f)
                TableVerticalAlignment.BOTTOM -> (available - contentHeight).coerceAtLeast(0f)
                else -> 0f
            }
            var paragraphY = contentBounds.top + verticalOffset
            val paragraphGeometry = measured?.paragraphs?.map { (paragraph, layout) ->
                val paragraphStyle = resolveTableParagraphStyle(paragraph, styles)
                paragraphY += paragraphStyle.spaceBeforeUnits
                val paragraphBounds = OfficeRect(
                    contentBounds.left,
                    paragraphY,
                    contentBounds.right,
                    paragraphY + layout.height
                )
                paragraphY += layout.height + paragraphStyle.spaceAfterUnits
                TableCellParagraphGeometry(paragraph, layout, paragraphBounds)
            }.orEmpty()
            cells += TableCellGeometry(origin, bounds, contentBounds, style, paragraphGeometry)
            addEdge(edgeOwners, TableEdgeKey(x, bounds.top, x + width, bounds.top), style.borderTop, origin.key)
            addEdge(edgeOwners, TableEdgeKey(x, bounds.bottom, x + width, bounds.bottom), style.borderBottom, origin.key)
            addEdge(edgeOwners, TableEdgeKey(x, bounds.top, x, bounds.bottom), style.borderStart, origin.key)
            addEdge(edgeOwners, TableEdgeKey(x + width, bounds.top, x + width, bounds.bottom), style.borderEnd, origin.key)
        }

        val sourceRows = rowGeometry.mapNotNull { it.sourceRowOrdinal }
        return TableFragmentGeometry(
            elementIndex = elementIndex,
            tableName = grid.tableName,
            bounds = OfficeRect(left, top, left + tableWidth, y),
            rows = rowGeometry,
            cells = cells,
            borderEdges = edgeOwners.values.toList(),
            repeatedHeaderRows = rowRefs.filter { it.isRepeatedHeader }.map { it.logicalRow }.toSet(),
            bodyLogicalRows = rowRefs.filter { it.isBodyCoverage }.map { it.logicalRow }.toSet(),
            sourceRowStart = sourceRows.minOrNull(),
            sourceRowEndExclusive = sourceRows.maxOrNull()?.plus(1),
            diagnostics = diagnostics + extraDiagnostics
        )
    }

    private fun addEdge(
        owners: MutableMap<TableEdgeKey, TableBorderEdgeGeometry>,
        key: TableEdgeKey,
        border: TableBorder,
        owner: TableCellSourceKey
    ) {
        if (border.widthUnits <= 0f || border.style == TableBorderLineStyle.NONE) return
        val candidate = TableBorderEdgeGeometry(key.x1, key.y1, key.x2, key.y2, border, owner)
        val previous = owners[key]
        if (previous == null || borderStrength(border) > borderStrength(previous.border)) {
            owners[key] = candidate
        }
    }

    private fun borderStrength(border: TableBorder): Float =
        border.widthUnits + when (border.style) {
            TableBorderLineStyle.DOUBLE -> 0.4f
            TableBorderLineStyle.SOLID -> 0.3f
            TableBorderLineStyle.DASHED -> 0.2f
            TableBorderLineStyle.DOTTED -> 0.1f
            else -> 0f
        }

    private data class TableEdgeKey(val x1: Float, val y1: Float, val x2: Float, val y2: Float)
}

data class TableFragmentRowRef(
    val logicalRow: Int,
    val isRepeatedHeader: Boolean = false,
    val isBodyCoverage: Boolean = true
)

object TableLayoutEngine {
    fun measure(
        table: OfficeTable,
        styles: DocumentStyles,
        availableWidth: Float,
        advanceSource: AdvanceSource,
        hyphenator: HyphenationEngine?
    ): TableLayoutPlan {
        val grid = TableGridResolver.resolve(table)
        val widthResult = resolveColumnWidths(grid, availableWidth)
        val diagnostics = grid.diagnostics.toMutableList()
        val measuredCells = linkedMapOf<TableCellSourceKey, TableMeasuredCell>()
        var paragraphIndex = -1
        grid.originCells().forEach { origin ->
            val cellWidth = widthResult.columnWidths
                .drop(origin.logicalColumn)
                .take(origin.columnSpan)
                .sum()
                .coerceAtLeast(1f)
            val style = origin.sourceCell.boxStyle
            val innerWidth = (
                cellWidth - style.padding.startUnits - style.padding.endUnits -
                    style.borderStart.widthUnits - style.borderEnd.widthUnits
                ).coerceAtLeast(1f)
            val paragraphs = if (origin.sourceCell.paragraphs.isNotEmpty()) {
                origin.sourceCell.paragraphs.map { paragraph ->
                    val paragraphStyle = resolveTableParagraphStyle(paragraph, styles)
                    val layout = ParagraphMeasurer(advanceSource, hyphenator).measure(
                        paragraphIndex--, paragraph, paragraphStyle, styles, innerWidth
                    )
                    paragraph to layout
                }
            } else if (origin.sourceCell.text.isNotEmpty()) {
                val paragraph = OfficeParagraph(origin.sourceCell.text)
                val paragraphStyle = StyleResolver.resolveParagraphStyle(null, styles)
                listOf(
                    paragraph to ParagraphMeasurer(advanceSource, hyphenator).measure(
                        paragraphIndex--, paragraph, paragraphStyle, styles, innerWidth
                    )
                )
            } else {
                emptyList()
            }
            val contentHeight = paragraphs.sumOf { (paragraph, layout) ->
                val paragraphStyle = resolveTableParagraphStyle(paragraph, styles)
                (paragraphStyle.spaceBeforeUnits + layout.height + paragraphStyle.spaceAfterUnits).toDouble()
            }.toFloat()
            val requiredHeight = (
                contentHeight + style.padding.topUnits + style.padding.bottomUnits +
                    style.borderTop.widthUnits + style.borderBottom.widthUnits
                ).coerceAtLeast(0f)
            measuredCells[origin.key] = TableMeasuredCell(origin, requiredHeight, paragraphs)
        }

        val rowHeights = grid.rows.map { row ->
            var height = max(1f, row.rowStyle.minimumHeightUnits ?: 0f)
            row.slots.filter { it.occupancy == TableCellOccupancy.ORIGIN }.mapNotNull { it.cell }.forEach { cell ->
                if (cell.rowSpan == 1) {
                    height = max(height, measuredCells[cell.key]?.requiredHeight ?: 0f)
                }
            }
            row.rowStyle.exactHeightUnits?.let { height = max(height, it) }
            height
        }.toMutableList()
        measuredCells.values.filter { it.cell.rowSpan > 1 }.forEach { measured ->
            val start = measured.cell.logicalRow
            val end = (start + measured.cell.rowSpan).coerceAtMost(rowHeights.size)
            if (start < end) {
                val current = (start until end).sumOf { rowHeights[it].toDouble() }.toFloat()
                val deficit = measured.requiredHeight - current
                if (deficit > 0f) rowHeights[end - 1] += deficit
            }
        }
        val rows = grid.rows.mapIndexed { index, row ->
            TableMeasuredRow(index, row.sourceRowOrdinal, row.isHeader, row.rowStyle, rowHeights[index])
        }
        val groups = rowGroups(grid, diagnostics)
        val headerCount = grid.rows.takeWhile { it.isHeader }.size
        return TableLayoutPlan(
            grid = grid,
            tableWidth = widthResult.tableWidth,
            columnWidths = widthResult.columnWidths,
            rows = rows,
            rowGroups = groups,
            headerRowCount = headerCount,
            measuredCells = measuredCells,
            styles = styles,
            diagnostics = diagnostics
        )
    }

    private data class WidthResult(val tableWidth: Float, val columnWidths: List<Float>)

    private fun resolveColumnWidths(grid: TableGrid, availableWidth: Float): WidthResult {
        val available = availableWidth.coerceAtLeast(1f)
        val tableWidth = when (grid.tableWidth.kind) {
            TableColumnWidthKind.ABSOLUTE -> grid.tableWidth.value.coerceAtLeast(1f).coerceAtMost(available)
            TableColumnWidthKind.RELATIVE -> (available * (grid.tableWidth.value / 100f).coerceIn(0f, 1f)).coerceAtLeast(1f)
            TableColumnWidthKind.AUTO -> available
        }
        val specs = if (grid.columns.isEmpty()) listOf(TableGridColumn(0)) else grid.columns
        val fixed = specs.map { if (it.width.kind == TableColumnWidthKind.ABSOLUTE) it.width.value else 0f }
        val fixedTotal = fixed.sum()
        val relativeTotal = specs.sumOf { if (it.width.kind == TableColumnWidthKind.RELATIVE) it.width.value.toDouble() else 0.0 }.toFloat()
        val autoCount = specs.count { it.width.kind == TableColumnWidthKind.AUTO }
        val flexibleWeight = relativeTotal + autoCount.toFloat()
        val widths = when {
            fixedTotal > tableWidth -> {
                val scale = tableWidth / fixedTotal
                fixed.map { it * scale }
            }
            fixedTotal < tableWidth && flexibleWeight > 0f -> {
                val remainder = tableWidth - fixedTotal
                specs.map { spec ->
                    when (spec.width.kind) {
                        TableColumnWidthKind.ABSOLUTE -> spec.width.value
                        TableColumnWidthKind.RELATIVE -> remainder * spec.width.value / flexibleWeight
                        TableColumnWidthKind.AUTO -> remainder / flexibleWeight
                    }
                }
            }
            fixedTotal > 0f -> {
                val scale = tableWidth / fixedTotal
                fixed.map { it * scale }
            }
            else -> List(specs.size) { tableWidth / specs.size.coerceAtLeast(1) }
        }.map { it.coerceAtLeast(1f) }
        return WidthResult(widths.sum(), widths)
    }

    private fun rowGroups(grid: TableGrid, diagnostics: MutableList<TableDiagnostic>): List<TableRowGroup> {
        if (grid.rows.isEmpty()) return emptyList()
        val intervals = grid.originCells()
            .filter { it.rowSpan > 1 }
            .map { it.logicalRow to (it.logicalRow + it.rowSpan).coerceAtMost(grid.rows.size) }
            .filter { it.first < it.second }
            .sortedBy { it.first }
            .toMutableList()
        val merged = mutableListOf<TableRowGroup>()
        intervals.forEach { (start, end) ->
            val previous = merged.lastOrNull()
            if (previous != null && start < previous.endExclusive) {
                merged[merged.lastIndex] = previous.copy(endExclusive = max(previous.endExclusive, end))
            } else {
                merged += TableRowGroup(start, end)
            }
        }
        val groups = mutableListOf<TableRowGroup>()
        var row = 0
        merged.forEach { group ->
            while (row < group.startInclusive) groups += TableRowGroup(row, row + 1).also { row++ }
            groups += group
            row = group.endExclusive
        }
        while (row < grid.rows.size) groups += TableRowGroup(row, row + 1).also { row++ }
        if (merged.any { it.startInclusive < grid.rows.takeWhile { r -> r.isHeader }.size && it.endExclusive > grid.rows.takeWhile { r -> r.isHeader }.size }) {
            diagnostics += TableDiagnostic(
                TableDiagnosticCode.UNSUPPORTED_DECLARATION,
                "A vertical span crosses the leading header boundary; it remains indivisible"
            )
        }
        return groups
    }
}
