package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Core Spreadsheet Interfaces
// ---------------------------------------------------------

interface XSpreadsheetDocument : XModel {
    val sheets: XSpreadsheets
}

interface XSpreadsheets : XNameContainer, XIndexAccess {
    fun insertNewByName(name: String, position: Short)
    fun moveByName(name: String, destination: Short)
    fun copyByName(name: String, destination: String, position: Short)
}

interface XSpreadsheet : XSheetCellRange {
    var isVisible: Boolean
}

interface XSheetCellRange : XCellRange {
    val spreadsheet: XSpreadsheet
}

interface XSheetCell : XCell {
    var formulaLocal: String
    val formulaResultType: Short // From FormulaResult enum
    var cellBackColor: Int
    var charColor: Int
    var horiJustify: Short
    var vertJustify: Short
    val cellAddress: CellAddress
}

// ---------------------------------------------------------
// Data and Address Structs
// ---------------------------------------------------------

data class CellAddress(var Sheet: Short = 0, var Column: Int = 0, var Row: Int = 0)

data class CellRangeAddress(
    var Sheet: Short = 0,
    var StartColumn: Int = 0,
    var StartRow: Int = 0,
    var EndColumn: Int = 0,
    var EndRow: Int = 0
)

// ---------------------------------------------------------
// Range Access and Operations
// ---------------------------------------------------------

interface XColumnRowRange {
    val columns: XTableColumns
    val rows: XTableRows
}

interface XCellRangeData {
    var dataArray: Array<Array<Any>>
}

interface XCellSeries {
    fun fillSeries(fillDirection: Short, fillMode: Short, fillDateMode: Short, step: Double, endValue: Double)
    fun fillAuto(fillDirection: Short, cellCount: Int)
}

interface XSheetOperation {
    fun computeFunction(function: Short): Double
    fun clearContents(flags: Int)
}

interface XMultipleOperation {
    fun setTableOperation(formulaRange: CellRangeAddress, mode: Short, columnCell: CellAddress, rowCell: CellAddress)
}

interface XSubTotalCalculatable {
    fun createSubTotalDescriptor(empty: Boolean): XSubTotalDescriptor
    fun applySubTotals(descriptor: XSubTotalDescriptor, replace: Boolean)
    fun removeSubTotals()
}

interface XArrayFormulaRange {
    var arrayFormula: String
}

// ---------------------------------------------------------
// SubTotals, Sorting and Filtering
// ---------------------------------------------------------

interface XSubTotalDescriptor {
    fun addNew(columns: Array<SubTotalColumn>, groupColumn: Int)
}

data class SubTotalColumn(
    var Column: Int = 0,
    var Function: Short = 0 // GeneralFunction
)

interface XSheetFilterable {
    fun createFilterDescriptor(empty: Boolean): XSheetFilterDescriptor
    fun filter(descriptor: XSheetFilterDescriptor)
}

interface XSheetFilterableEx {
    fun createFilterDescriptorByObject(filterable: XSheetFilterable): XSheetFilterDescriptor
}

interface XSheetFilterDescriptor {
    var filterFields: Array<TableFilterField>
}

data class TableFilterField(
    var Connection: Short = 0, // FilterConnection
    var Field: Int = 0,
    var Operator: Short = 0, // FilterOperator
    var IsNumeric: Boolean = false,
    var NumericValue: Double = 0.0,
    var StringValue: String = ""
)

// ---------------------------------------------------------
// Queries and Containers
// ---------------------------------------------------------

interface XCellRangesQuery {
    fun queryVisibleCells(): XSheetCellRanges
    fun queryEmptyCells(): XSheetCellRanges
    fun queryContentCells(contentFlags: Short): XSheetCellRanges
    fun queryFormulaCells(resultFlags: Short): XSheetCellRanges
    fun queryColumnDifferences(compareCellAddress: CellAddress): XSheetCellRanges
    fun queryRowDifferences(compareCellAddress: CellAddress): XSheetCellRanges
    fun queryIntersection(range: CellRangeAddress): XSheetCellRanges
}

interface XSheetCellRanges : XIndexAccess {
    val cells: XEnumerationAccess
    val rangeAddressesAsString: String
    val rangeAddresses: Array<CellRangeAddress>
}

interface XSheetCellRangeContainer : XSheetCellRanges {
    fun addRangeAddress(range: CellRangeAddress, mergeRanges: Boolean)
    fun addRangeAddresses(ranges: Array<CellRangeAddress>, mergeRanges: Boolean)
    fun removeRangeAddress(range: CellRangeAddress)
    fun removeRangeAddresses(ranges: Array<CellRangeAddress>)
}

// ---------------------------------------------------------
// Cursors
// ---------------------------------------------------------

interface XSheetCellCursor : XSheetCellRange, XCellCursor {
    fun collapseToCurrentRegion()
    fun collapseToCurrentArray()
    fun collapseToMergedArea()
    fun expandToEntireColumns()
    fun expandToEntireRows()
    fun collapseToSize(columns: Int, rows: Int)
}

interface XCellCursor : XCellRange {
    fun gotoStart()
    fun gotoEnd()
    fun gotoOffset(columnOffset: Int, rowOffset: Int)
    fun gotoPrevious()
    fun gotoNext()
}

interface XUsedAreaCursor : XSheetCellCursor {
    fun gotoStartOfUsedArea(expand: Boolean)
    fun gotoEndOfUsedArea(expand: Boolean)
}

// ---------------------------------------------------------
// View and Controller
// ---------------------------------------------------------

interface XSpreadsheetView {
    val activeSheet: XSpreadsheet
    fun setActiveSheet(sheet: XSpreadsheet)
}

interface XViewSplitable {
    val isWindowSplit: Boolean
    val splitHorizontal: Int
    val splitVertical: Int
    val splitColumn: Int
    val splitRow: Int
    fun splitAtPosition(pixelX: Int, pixelY: Int)
}

interface XViewFreezable {
    val hasFrozenPanes: Boolean
    fun freezeAtPosition(columns: Int, rows: Int)
}

interface XViewPane {
    var firstVisibleColumn: Int
    var firstVisibleRow: Int
    val visibleRange: CellRangeAddress
}

interface XRangeSelection {
    fun startRangeSelection(arguments: Array<PropertyValue>)
    fun abortRangeSelection()
    fun addRangeSelectionListener(listener: XRangeSelectionListener)
    fun removeRangeSelectionListener(listener: XRangeSelectionListener)
    fun addRangeSelectionChangeListener(listener: XRangeSelectionChangeListener)
    fun removeRangeSelectionChangeListener(listener: XRangeSelectionChangeListener)
}

interface XRangeSelectionListener : XEventListener {
    fun done(event: RangeSelectionEvent)
    fun aborted(event: RangeSelectionEvent)
}

interface XRangeSelectionChangeListener : XEventListener {
    fun descriptorChanged(event: RangeSelectionEvent)
}

data class RangeSelectionEvent(
    val source: Any,
    val RangeDescriptor: String
)

// ---------------------------------------------------------
// Extensibility and Functions
// ---------------------------------------------------------

interface XFunctionAccess {
    fun callFunction(name: String, arguments: Array<Any>): Any
}

interface XFunctionDescriptions : XNameAccess, XIndexAccess {
    fun getById(id: Int): Array<PropertyValue>
}

interface XRecentFunctions {
    var recentFunctionIds: IntArray
    val maxRecentFunctions: Int
}
