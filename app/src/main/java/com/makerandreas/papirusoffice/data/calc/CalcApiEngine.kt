package com.makerandreas.papirusoffice.data.calc

import com.makerandreas.papirusoffice.data.framework.XChartDocument
import com.makerandreas.papirusoffice.data.framework.XChartData
import com.makerandreas.papirusoffice.data.framework.XChartDataChangeEventListener
import com.makerandreas.papirusoffice.data.framework.XDiagram
import com.makerandreas.papirusoffice.data.framework.XShape
import com.makerandreas.papirusoffice.data.framework.Rectangle
import com.makerandreas.papirusoffice.data.framework.XIndexAccess
import com.makerandreas.papirusoffice.data.framework.Point
import com.makerandreas.papirusoffice.data.framework.Size
import com.makerandreas.papirusoffice.data.framework.MediaDescriptor
import com.makerandreas.papirusoffice.data.framework.XController as FrameworkXController
import com.makerandreas.papirusoffice.data.framework.XEventListener as FrameworkXEventListener

// ============================================================================
// LibreOffice SDK Guide: Chapter 19. Calc API Overview
// Papirus Engine Mock Implementation
// ============================================================================

// --- Mock Interfaces ---
interface XComponent
interface XComponentLoader

interface XSpreadsheetDocument : XComponent {
    val sheets: XSpreadsheets
}

interface XSpreadsheets {
    fun getCount(): Int
    fun getByIndex(index: Int): Any
    fun getByName(name: String): Any
    fun insertNewByName(name: String, position: Short)
}

interface XCellRange {
    fun getCellByPosition(column: Int, row: Int): XCell
    fun getCellRangeByPosition(left: Int, top: Int, right: Int, bottom: Int): XCellRange
    fun getCellRangeByName(range: String): XCellRange
}

interface XSpreadsheet : XCellRange

interface XCell {
    fun getValue(): Double
    fun setValue(value: Double)
    fun getFormula(): String
    fun setFormula(formula: String)
}

interface XCellRangeData {
    fun getDataArray(): Array<Array<Any>>
    fun setDataArray(data: Array<Array<Any>>)
}

interface XColumnRowRange {
    val columns: Any
    val rows: Any
}

interface XTableRows {
    fun getCount(): Int
    fun getByIndex(index: Int): Any
}

// --- Implementation Classes ---

class PapirusCell : XCell {
    private var cellValue: Double = 0.0
    private var cellFormula: String = ""
    var formatting: Map<String, Any> = emptyMap()

    override fun getValue(): Double = cellValue
    override fun setValue(value: Double) {
        cellValue = value
        cellFormula = value.toString()
    }

    override fun getFormula(): String = cellFormula
    override fun setFormula(formula: String) {
        cellFormula = formula
        // Basic parsing for mock
        formula.toDoubleOrNull()?.let { cellValue = it }
    }
}

class PapirusCellRange(
    val sheet: PapirusSpreadsheet,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) : XCellRange, XCellRangeData {

    override fun getCellByPosition(column: Int, row: Int): XCell {
        return sheet.getCellByPosition(left + column, top + row)
    }

    override fun getCellRangeByPosition(l: Int, t: Int, r: Int, b: Int): XCellRange {
        return PapirusCellRange(sheet, left + l, top + t, left + r, top + b)
    }

    override fun getCellRangeByName(range: String): XCellRange {
        return sheet.getCellRangeByName(range)
    }

    override fun getDataArray(): Array<Array<Any>> {
        val rows = bottom - top + 1
        val cols = right - left + 1
        return Array(rows) { r ->
            Array(cols) { c ->
                val cell = getCellByPosition(c, r)
                if (cell.getFormula().startsWith("=")) cell.getFormula() else cell.getValue()
            }
        }
    }

    override fun setDataArray(data: Array<Array<Any>>) {
        for (r in data.indices) {
            for (c in data[r].indices) {
                val cell = getCellByPosition(c, r)
                val value = data[r][c]
                when (value) {
                    is String -> cell.setFormula(value)
                    is Number -> cell.setValue(value.toDouble())
                }
            }
        }
    }
}

class PapirusSpreadsheet(val name: String) : XSpreadsheet, XColumnRowRange {
    private val cells = mutableMapOf<Pair<Int, Int>, PapirusCell>()

    override fun getCellByPosition(column: Int, row: Int): XCell {
        return cells.getOrPut(column to row) { PapirusCell() }
    }

    override fun getCellRangeByPosition(left: Int, top: Int, right: Int, bottom: Int): XCellRange {
        return PapirusCellRange(this, left, top, right, bottom)
    }

    override fun getCellRangeByName(range: String): XCellRange {
        // Mock parsing "A1:B2" or "C5"
        val parts = range.split(":")
        val start = parseCellName(parts[0])
        val end = if (parts.size > 1) parseCellName(parts[1]) else start
        return getCellRangeByPosition(start.first, start.second, end.first, end.second)
    }

    private fun parseCellName(name: String): Pair<Int, Int> {
        var col = 0
        var rowStr = ""
        for (char in name) {
            if (char.isLetter()) {
                col = col * 26 + (char.uppercaseChar() - 'A' + 1)
            } else if (char.isDigit()) {
                rowStr += char
            }
        }
        return Pair(maxOf(0, col - 1), maxOf(0, (rowStr.toIntOrNull() ?: 1) - 1))
    }

    override val columns: Any = object {}
    override val rows: Any = object : XTableRows {
        override fun getCount(): Int = 1048576 // Mock max rows
        override fun getByIndex(index: Int): Any {
            return getCellRangeByPosition(0, index, 1023, index) // Mock row range
        }
    }
}

class PapirusSpreadsheetDoc : XSpreadsheetDocument {
    private val sheetList = mutableListOf<PapirusSpreadsheet>()

    init {
        sheetList.add(PapirusSpreadsheet("Sheet1"))
    }

    override val sheets: XSpreadsheets = object : XSpreadsheets {
        override fun getCount(): Int = sheetList.size
        override fun getByIndex(index: Int): Any = sheetList[index]
        override fun getByName(name: String): Any = sheetList.first { it.name == name }
        override fun insertNewByName(name: String, position: Short) {
            sheetList.add(position.toInt(), PapirusSpreadsheet(name))
        }
    }
}

// --- SDK Utility Methods (Calc class equivalent) ---

object Calc {
    fun openDoc(url: String, loader: XComponentLoader?): XSpreadsheetDocument {
        return PapirusSpreadsheetDoc()
    }

    fun getSheet(doc: XSpreadsheetDocument, index: Int): XSpreadsheet {
        return doc.sheets.getByIndex(index) as XSpreadsheet
    }

    fun getRowRange(sheet: XSpreadsheet, row: Int): XCellRange {
        val crRange = sheet as XColumnRowRange
        val rows = crRange.rows as XTableRows
        return rows.getByIndex(row) as XCellRange
    }
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 20. Spreadsheet Displaying and Creation
// Papirus Engine Mock Implementation
// ============================================================================

interface XSpreadsheetView {
    fun setActiveSheet(sheet: XSpreadsheet)
}

interface XProtectable {
    fun protect(password: String)
    fun unprotect(password: String)
}

interface XNamed {
    fun getName(): String
    fun setName(name: String)
}

interface XController
interface XModel {
    fun getCurrentController(): XController
}

interface XFrame
interface XSpreadsheetViewSettings {
    var zoomType: Short
    var zoomValue: Short
}

class PapirusSpreadsheetView : XSpreadsheetView, XSpreadsheetViewSettings {
    var currentSheet: XSpreadsheet? = null
    override var zoomType: Short = 0
    override var zoomValue: Short = 100

    override fun setActiveSheet(sheet: XSpreadsheet) {
        currentSheet = sheet
        println("Active sheet set to: ${(sheet as? XNamed)?.getName() ?: "Unknown"}")
    }
}

class PapirusController : XController, XModel {
    val view = PapirusSpreadsheetView()
    override fun getCurrentController(): XController = this
}

// Extend existing classes with Chapter 20 interfaces
fun PapirusSpreadsheet.asProtectable(): XProtectable {
    return object : XProtectable {
        private var isProtected = false
        private var pwd = ""
        override fun protect(password: String) {
            isProtected = true
            pwd = password
            println("Sheet protected with password.")
        }
        override fun unprotect(password: String) {
            if (pwd == password) {
                isProtected = false
                println("Sheet unprotected.")
            } else {
                println("Incorrect password.")
            }
        }
    }
}

fun PapirusSpreadsheet.asNamed(): XNamed {
    val sheet = this
    return object : XNamed {
        override fun getName(): String = sheet.name
        override fun setName(name: String) {
            // Note: In a real implementation this would update the sheet's name in the document
            println("Sheet renamed to: $name")
        }
    }
}

// Add Chapter 20 methods to Calc object
fun Calc.setActiveSheet(doc: XSpreadsheetDocument, sheet: XSpreadsheet) {
    val controller = (doc as? XModel)?.getCurrentController() as? PapirusSpreadsheetView
    controller?.setActiveSheet(sheet)
}

fun Calc.getSheetNames(doc: XSpreadsheetDocument): Array<String> {
    val count = doc.sheets.getCount()
    return Array(count) { i ->
        ((doc.sheets.getByIndex(i) as? PapirusSpreadsheet)?.name) ?: "Sheet${i+1}"
    }
}

fun Calc.setSheetName(sheet: XSpreadsheet, name: String) {
    val named = (sheet as? PapirusSpreadsheet)?.asNamed()
    named?.setName(name)
}

// Zoom constants
const val OPTIMAL: Short = 0
const val PAGE_WIDTH: Short = 1
const val ENTIRE_PAGE: Short = 2
const val BY_VALUE: Short = 3
const val PAGE_WIDTH_EXACT: Short = 4

fun Calc.zoom(doc: XSpreadsheetDocument, type: Short) {
    val controller = (doc as? XModel)?.getCurrentController() as? PapirusSpreadsheetView
    controller?.zoomType = type
    println("Zoom type set to: $type")
}

fun Calc.zoomValue(doc: XSpreadsheetDocument, value: Short) {
    val controller = (doc as? XModel)?.getCurrentController() as? PapirusSpreadsheetView
    controller?.zoomType = BY_VALUE
    controller?.zoomValue = value
    println("Zoom value set to: $value%")
}

// Array, Row, Col filling
fun Calc.setArray(sheet: XSpreadsheet, name: String, values: Array<Array<Any>>) {
    val cellRange = sheet.getCellRangeByName(name) as? XCellRangeData
    cellRange?.setDataArray(values)
}

fun Calc.setRow(sheet: XSpreadsheet, cellName: String, values: Array<Any>) {
    val parts = cellName.split(":")
    val start = parts[0]
    val colRow = parseCellName(start)
    val cellRange = sheet.getCellRangeByPosition(colRow.first, colRow.second, colRow.first + values.size - 1, colRow.second) as? XCellRangeData
    cellRange?.setDataArray(arrayOf(values))
}

fun Calc.setCol(sheet: XSpreadsheet, cellName: String, values: Array<Any>) {
    val colRow = parseCellName(cellName)
    val cellRange = sheet.getCellRangeByPosition(colRow.first, colRow.second, colRow.first, colRow.second + values.size - 1) as? XCellRangeData
    val dataArray = Array(values.size) { i -> arrayOf(values[i]) }
    cellRange?.setDataArray(dataArray)
}

fun Calc.parseCellName(name: String): Pair<Int, Int> {
    var col = 0
    var rowStr = ""
    for (char in name) {
        if (char.isLetter()) {
            col = col * 26 + (char.uppercaseChar() - 'A' + 1)
        } else if (char.isDigit()) {
            rowStr += char
        }
    }
    return Pair(maxOf(0, col - 1), maxOf(0, (rowStr.toIntOrNull() ?: 1) - 1))
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 21. Extracting Data
// Papirus Engine Mock Implementation
// ============================================================================

enum class CellContentType {
    EMPTY, VALUE, TEXT, FORMULA
}

fun XCell.getType(): CellContentType {
    val value = this.getValue()
    val formula = this.getFormula()
    return when {
        formula.startsWith("=") -> CellContentType.FORMULA
        formula.isNotEmpty() -> CellContentType.TEXT
        value != 0.0 || formula == "0.0" -> CellContentType.VALUE // Simple mock logic
        else -> CellContentType.EMPTY
    }
}

interface XSheetCellCursor : XCellRange {
    fun collapseToCurrentRegion()
    fun expandToEntireColumns()
    fun expandToEntireRows()
}

interface XUsedAreaCursor {
    fun gotoStartOfUsedArea(expand: Boolean)
    fun gotoEndOfUsedArea(expand: Boolean)
}

interface XCellRangesQuery {
    fun queryContentCells(contentFlags: Short): XSheetCellRanges
}

interface XSheetCellRanges {
    fun getRangeAddressesAsString(): String
    fun getRangeAddresses(): Array<CellRangeAddress>
}

class CellRangeAddress(val Sheet: Short, val StartColumn: Int, val StartRow: Int, val EndColumn: Int, val EndRow: Int)

class PapirusSheetCellCursor(val sheet: PapirusSpreadsheet, var left: Int, var top: Int, var right: Int, var bottom: Int) : XSheetCellCursor, XUsedAreaCursor {
    override fun getCellByPosition(column: Int, row: Int): XCell = sheet.getCellByPosition(left + column, top + row)
    override fun getCellRangeByPosition(l: Int, t: Int, r: Int, b: Int): XCellRange = sheet.getCellRangeByPosition(left + l, top + t, left + r, top + b)
    override fun getCellRangeByName(range: String): XCellRange = sheet.getCellRangeByName(range)
    
    override fun collapseToCurrentRegion() {}
    override fun expandToEntireColumns() {}
    override fun expandToEntireRows() {}

    override fun gotoStartOfUsedArea(expand: Boolean) {
        if (!expand) { right = 0; bottom = 0 }
        left = 0; top = 0
    }

    override fun gotoEndOfUsedArea(expand: Boolean) {
        if (!expand) { left = 100; top = 100 }
        right = 100; bottom = 100 // Mock end of used area
    }
}

fun PapirusSpreadsheet.createCursor(): XSheetCellCursor {
    return PapirusSheetCellCursor(this, 0, 0, 0, 0)
}

fun Calc.getVal(sheet: XSpreadsheet, cellName: String): Any? {
    val cell = (sheet.getCellRangeByName(cellName) as? PapirusCellRange)?.getCellByPosition(0, 0)
    return getVal(cell)
}

fun Calc.getVal(sheet: XSpreadsheet, col: Int, row: Int): Any? {
    val cell = sheet.getCellByPosition(col, row)
    return getVal(cell)
}

fun Calc.getVal(cell: XCell?): Any? {
    if (cell == null) return null
    return when (cell.getType()) {
        CellContentType.EMPTY -> null
        CellContentType.VALUE -> cell.getValue()
        CellContentType.TEXT, CellContentType.FORMULA -> cell.getFormula()
    }
}

fun Calc.getNum(sheet: XSpreadsheet, cellName: String): Double {
    val cell = (sheet.getCellRangeByName(cellName) as? PapirusCellRange)?.getCellByPosition(0, 0)
    return cell?.getValue() ?: 0.0
}

fun Calc.getTypeString(cell: XCell): String {
    return cell.getType().name
}

fun Calc.getArray(sheet: XSpreadsheet, rangeName: String): Array<Array<Any>> {
    val cellRange = sheet.getCellRangeByName(rangeName) as? XCellRangeData
    return cellRange?.getDataArray() ?: emptyArray()
}

fun Calc.getRow(sheet: XSpreadsheet, rangeName: String): Array<Any> {
    val array = getArray(sheet, rangeName)
    return if (array.isNotEmpty()) array[0] else emptyArray()
}

fun Calc.getCol(sheet: XSpreadsheet, rangeName: String): Array<Any> {
    val array = getArray(sheet, rangeName)
    return array.map { if (it.isNotEmpty()) it[0] else "" }.toTypedArray()
}

fun Calc.findUsedRange(sheet: XSpreadsheet): XCellRange {
    val cursor = (sheet as PapirusSpreadsheet).createCursor()
    val uaCursor = cursor as XUsedAreaCursor
    uaCursor.gotoStartOfUsedArea(false)
    uaCursor.gotoEndOfUsedArea(true)
    return cursor
}

object CellFlags {
    const val VALUE: Short = 1
    const val DATETIME: Short = 2
    const val STRING: Short = 4
    const val ANNOTATION: Short = 8
    const val FORMULA: Short = 16
    const val HARDATTR: Short = 32
    const val STYLES: Short = 64
    const val OBJECTS: Short = 128
    const val EDITATTR: Short = 256
    const val FORMATTED: Short = 512
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 22. Styles
// Papirus Engine Mock Implementation
// ============================================================================

interface XStyle {
    fun getName(): String
    fun setName(name: String)
}

interface XNameContainer {
    fun insertByName(name: String, element: Any)
    fun getByName(name: String): Any
}

interface XPropertySet {
    fun setPropertyValue(propertyName: String, value: Any)
    fun getPropertyValue(propertyName: String): Any?
}

class PapirusStyle(var styleName: String) : XStyle, XPropertySet {
    private val props = mutableMapOf<String, Any>()
    
    override fun getName(): String = styleName
    override fun setName(name: String) { styleName = name }
    
    override fun setPropertyValue(propertyName: String, value: Any) {
        props[propertyName] = value
        println("Style \$styleName property \$propertyName set to \$value")
    }
    
    override fun getPropertyValue(propertyName: String): Any? = props[propertyName]
}

// Border classes
class BorderLine2 {
    var Color: Int = 0
    var InnerLineWidth: Int = 0
    var OuterLineWidth: Int = 0
    var LineDistance: Int = 0
}

class TableBorder2 {
    var TopLine: BorderLine2? = null
    var IsTopLineValid: Boolean = false
    var BottomLine: BorderLine2? = null
    var IsBottomLineValid: Boolean = false
    var LeftLine: BorderLine2? = null
    var IsLeftLineValid: Boolean = false
    var RightLine: BorderLine2? = null
    var IsRightLineValid: Boolean = false
}

// Add Style container logic
class PapirusStyleContainer : XNameContainer {
    private val styles = mutableMapOf<String, Any>()
    
    init {
        // Default styles
        styles["Default"] = PapirusStyle("Default")
        styles["Heading"] = PapirusStyle("Heading")
        styles["Heading1"] = PapirusStyle("Heading1")
        styles["Result"] = PapirusStyle("Result")
    }

    override fun insertByName(name: String, element: Any) {
        styles[name] = element
    }

    override fun getByName(name: String): Any = styles[name] ?: throw Exception("Style not found")
    
    fun getElementNames(): Array<String> = styles.keys.toTypedArray()
}

// Extend existing Info class if not exist, or create it
object Info {
    private val cellStyles = PapirusStyleContainer()
    private val pageStyles = PapirusStyleContainer()
    
    fun getStyleFamilyNames(doc: XComponent): Array<String> {
        return arrayOf("CellStyles", "PageStyles")
    }
    
    fun getStyleNames(doc: XComponent, family: String): Array<String> {
        return if (family == "CellStyles") {
            cellStyles.getElementNames()
        } else {
            pageStyles.getElementNames()
        }
    }
    
    fun getStyleContainer(doc: XComponent, family: String): XNameContainer {
        return if (family == "CellStyles") cellStyles else pageStyles
    }
}

// Calc methods for Styles
fun Calc.createCellStyle(doc: XSpreadsheetDocument, styleName: String): XStyle? {
    val styleFamilies = Info.getStyleContainer(doc as XComponent, "CellStyles")
    val style = PapirusStyle(styleName)
    try {
        styleFamilies.insertByName(styleName, style)
        return style
    } catch (e: Exception) {
        println("Unable to create style: \$styleName")
        return null
    }
}

fun Calc.changeStyle(sheet: XSpreadsheet, rangeName: String, styleName: String) {
    val cellRange = sheet.getCellRangeByName(rangeName) as? PapirusCellRange
    cellRange?.let {
        // Mock setting style property to the range
        println("Applied style '\$styleName' to range \$rangeName")
    }
}

fun Calc.addBorder(sheet: XSpreadsheet, rangeName: String, borderVals: Int, color: Int) {
    val line = BorderLine2()
    line.Color = color
    line.InnerLineWidth = 0
    line.LineDistance = 0
    line.OuterLineWidth = 100

    val border = TableBorder2()
    if ((borderVals and 0x01) == 0x01) { // TOP
        border.TopLine = line
        border.IsTopLineValid = true
    }
    if ((borderVals and 0x02) == 0x02) { // BOTTOM
        border.BottomLine = line
        border.IsBottomLineValid = true
    }
    if ((borderVals and 0x04) == 0x04) { // LEFT
        border.LeftLine = line
        border.IsLeftLineValid = true
    }
    if ((borderVals and 0x08) == 0x08) { // RIGHT
        border.RightLine = line
        border.IsRightLineValid = true
    }

    val cellRange = sheet.getCellRangeByName(rangeName)
    // Mock setting the border property
    println("Applied borders to \$rangeName with color \$color")
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 23. Garlic Secrets
// Papirus Engine Mock Implementation
// ============================================================================

interface XViewFreezable {
    fun freezeAtPosition(columns: Int, rows: Int)
}

interface XSheetOperation {
    fun computeFunction(fn: GeneralFunction): Double
}

enum class GeneralFunction {
    SUM, COUNT, AVERAGE, MAX, MIN
}

interface XMergeable {
    fun merge(merge: Boolean)
}

interface XCellRangeMovement {
    fun insertCells(range: CellRangeAddress, mode: CellInsertMode)
}

enum class CellInsertMode {
    NONE, DOWN, RIGHT, ROWS, COLUMNS
}

interface XViewSplitable {
    fun splitAtPosition(pixelX: Int, pixelY: Int)
}

interface XViewPane {
    fun setFirstVisibleRow(row: Int)
    fun setFirstVisibleColumn(col: Int)
}

class PapirusViewData(val sheetId: Int) {
    var cursorCol = 0
    var cursorRow = 0
    var activePane = 0
    var leftRightSplit = 0
    var topBottomSplit = 0
}

class ViewState(val stateString: String) {
    var focusNum: Int = 0
    
    companion object {
        const val MOVE_UP = 0
        const val MOVE_DOWN = 1
        const val MOVE_LEFT = 2
        const val MOVE_RIGHT = 3
    }
    
    fun movePaneFocus(dir: Int) {
        when (dir) {
            MOVE_UP -> if (focusNum == 3) focusNum = 1 else if (focusNum == 2) focusNum = 0
            MOVE_DOWN -> if (focusNum == 1) focusNum = 3 else if (focusNum == 0) focusNum = 2
            MOVE_LEFT -> if (focusNum == 1) focusNum = 0 else if (focusNum == 3) focusNum = 2
            MOVE_RIGHT -> if (focusNum == 0) focusNum = 1 else if (focusNum == 2) focusNum = 3
        }
    }
    
    fun report() {
        println("Sheet View State - Focused pane: \$focusNum")
    }
    
    override fun toString(): String {
        return "mock_view_state_\$focusNum"
    }
}

// Extend existing components
fun PapirusSpreadsheetView.asViewFreezable(): XViewFreezable {
    return object : XViewFreezable {
        override fun freezeAtPosition(columns: Int, rows: Int) {
            println("Frozen at cols: \$columns, rows: \$rows")
        }
    }
}

fun PapirusCellRange.asSheetOperation(): XSheetOperation {
    val range = this
    return object : XSheetOperation {
        override fun computeFunction(fn: GeneralFunction): Double {
            val data = range.getDataArray()
            val values = data.flatten().mapNotNull { 
                when (it) {
                    is Number -> it.toDouble()
                    is String -> it.toDoubleOrNull()
                    else -> null
                }
            }
            return when (fn) {
                GeneralFunction.SUM -> values.sum()
                GeneralFunction.AVERAGE -> if (values.isNotEmpty()) values.sum() / values.size else 0.0
                GeneralFunction.MAX -> values.maxOrNull() ?: 0.0
                GeneralFunction.MIN -> values.minOrNull() ?: 0.0
                GeneralFunction.COUNT -> values.size.toDouble()
            }
        }
    }
}

fun PapirusCellRange.asMergeable(): XMergeable {
    return object : XMergeable {
        override fun merge(merge: Boolean) {
            println(if (merge) "Cells merged" else "Cells unmerged")
        }
    }
}

fun PapirusSpreadsheet.asCellRangeMovement(): XCellRangeMovement {
    return object : XCellRangeMovement {
        override fun insertCells(range: CellRangeAddress, mode: CellInsertMode) {
            println("Inserted cells at \${range.StartColumn}:\${range.StartRow} shifted \$mode")
        }
    }
}

// Calc methods for Ch 23
fun Calc.freezeRows(doc: XSpreadsheetDocument, numRows: Int) {
    val controller = (doc as? XModel)?.getCurrentController() as? PapirusSpreadsheetView
    controller?.asViewFreezable()?.freezeAtPosition(0, numRows)
}

fun Calc.freezeCols(doc: XSpreadsheetDocument, numCols: Int) {
    val controller = (doc as? XModel)?.getCurrentController() as? PapirusSpreadsheetView
    controller?.asViewFreezable()?.freezeAtPosition(numCols, 0)
}

fun Calc.getColRange(sheet: XSpreadsheet, idx: Int): XCellRange {
    return sheet.getCellRangeByPosition(idx, 0, idx, 1048575)
}

fun Calc.computeFunction(fn: GeneralFunction, cellRange: XCellRange): Double {
    return (cellRange as? PapirusCellRange)?.asSheetOperation()?.computeFunction(fn) ?: 0.0
}

fun Calc.splitWindow(doc: XSpreadsheetDocument, cellName: String) {
    println("Window split at \$cellName")
}

fun Calc.getViewPanes(doc: XSpreadsheetDocument): Array<XViewPane> {
    return arrayOf(
        object : XViewPane {
            override fun setFirstVisibleRow(row: Int) { println("Pane 0 visible row: \$row") }
            override fun setFirstVisibleColumn(col: Int) {}
        }
    )
}

fun Calc.getViewStates(doc: XSpreadsheetDocument): Array<ViewState> {
    return arrayOf(ViewState("mock_state"))
}

fun Calc.setViewStates(doc: XSpreadsheetDocument, states: Array<ViewState>) {
    println("View states updated.")
}

fun Calc.setRowHeight(sheet: XSpreadsheet, idx: Int, height: Int) {
    println("Row \$idx height set to \$height")
}

fun Calc.insertRow(sheet: XSpreadsheet, idx: Int) {
    println("Row inserted at index \$idx")
}

fun Calc.insertColumn(sheet: XSpreadsheet, idx: Int) {
    println("Column inserted at index \$idx")
}

fun Calc.insertCells(sheet: XSpreadsheet, cellRange: XCellRange, isShiftRight: Boolean) {
    val mode = if (isShiftRight) CellInsertMode.RIGHT else CellInsertMode.DOWN
    (sheet as? PapirusSpreadsheet)?.asCellRangeMovement()?.insertCells(CellRangeAddress(0, 0, 0, 0, 0), mode)
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 24. Complex Data Manipulation
// Papirus Engine Mock Implementation
// ============================================================================

class PropertyValue(var Name: String, var Value: Any)

class TableSortField {
    var Field: Int = 0
    var IsAscending: Boolean = true
    var IsCaseSensitive: Boolean = false
}

interface XSortable {
    fun sort(descriptor: Array<PropertyValue>)
}

enum class FillDirection {
    TO_BOTTOM, TO_RIGHT, TO_TOP, TO_LEFT
}

enum class FillMode {
    SIMPLE, LINEAR, GROWTH, DATE, AUTO
}

enum class FillDateMode {
    FILL_DATE_DAY, FILL_DATE_WEEKDAY, FILL_DATE_MONTH, FILL_DATE_YEAR, NO_DATE
}

interface XCellSeries {
    fun fillAuto(fillDirection: FillDirection, sourceColCount: Int)
    fun fillSeries(fillDirection: FillDirection, fillMode: FillMode, fillDateMode: FillDateMode, step: Double, endValue: Double)
}

interface XTextCursor

interface XText {
    fun createTextCursor(): XTextCursor
    fun getString(): String
}

interface XSheetAnnotation {
    fun setIsVisible(visible: Boolean)
}

interface XSheetAnnotations {
    fun insertNew(address: CellAddress, text: String)
}

interface XSheetAnnotationsSupplier {
    fun getAnnotations(): XSheetAnnotations
}

interface XSheetAnnotationAnchor {
    fun getAnnotation(): XSheetAnnotation
}

class CellAddress(var Sheet: Short, var Column: Int, var Row: Int)

fun PapirusCellRange.asSortable(): XSortable {
    return object : XSortable {
        override fun sort(descriptor: Array<PropertyValue>) {
            println("Range sorted with descriptor")
        }
    }
}

fun PapirusCellRange.asCellSeries(): XCellSeries {
    return object : XCellSeries {
        override fun fillAuto(fillDirection: FillDirection, sourceColCount: Int) {
            println("Filled auto direction $fillDirection")
        }
        override fun fillSeries(fillDirection: FillDirection, fillMode: FillMode, fillDateMode: FillDateMode, step: Double, endValue: Double) {
            println("Filled series mode $fillMode step $step")
        }
    }
}

fun PapirusCell.asText(): XText {
    val cell = this
    return object : XText {
        override fun createTextCursor(): XTextCursor = object : XTextCursor {}
        override fun getString(): String = cell.getValue().toString()
    }
}

fun PapirusSpreadsheet.asAnnotationsSupplier(): XSheetAnnotationsSupplier {
    return object : XSheetAnnotationsSupplier {
        override fun getAnnotations(): XSheetAnnotations {
            return object : XSheetAnnotations {
                override fun insertNew(address: CellAddress, text: String) {
                    println("Inserted annotation at ${address.Column}:${address.Row} with text: $text")
                }
            }
        }
    }
}

fun PapirusCell.asAnnotationAnchor(): XSheetAnnotationAnchor {
    return object : XSheetAnnotationAnchor {
        override fun getAnnotation(): XSheetAnnotation {
            return object : XSheetAnnotation {
                override fun setIsVisible(visible: Boolean) {
                    println("Annotation visibility set to $visible")
                }
            }
        }
    }
}

fun Calc.highlightRange(sheet: XSpreadsheet, rangeName: String, headline: String) {
    println("Range $rangeName highlighted with headline: $headline")
}

fun Calc.addAnnotation(sheet: XSpreadsheet, cellName: String, msg: String) {
    val colRow = parseCellName(cellName)
    val addr = CellAddress(0, colRow.first, colRow.second)
    val annsSupp = (sheet as PapirusSpreadsheet).asAnnotationsSupplier()
    annsSupp.getAnnotations().insertNew(addr, msg)
    
    val cell = sheet.getCellByPosition(colRow.first, colRow.second) as PapirusCell
    val ann = cell.asAnnotationAnchor().getAnnotation()
    ann.setIsVisible(true)
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 25. Monitoring Sheets
// Papirus Engine Mock Implementation
// ============================================================================

class PapirusEventObject(val Source: Any)

interface XEventListener {
    fun disposing(event: PapirusEventObject)
}

interface XModifyListener : XEventListener {
    fun modified(event: PapirusEventObject)
}

interface XModifyBroadcaster {
    fun addModifyListener(listener: XModifyListener)
    fun removeModifyListener(listener: XModifyListener)
}

interface XSelectionChangeListener : XEventListener {
    fun selectionChanged(event: PapirusEventObject)
}

interface XSelectionSupplier {
    fun addSelectionChangeListener(listener: XSelectionChangeListener)
    fun removeSelectionChangeListener(listener: XSelectionChangeListener)
}

fun PapirusSpreadsheetDoc.asModifyBroadcaster(): XModifyBroadcaster {
    return object : XModifyBroadcaster {
        override fun addModifyListener(listener: XModifyListener) {
            println("Added ModifyListener")
        }
        override fun removeModifyListener(listener: XModifyListener) {
            println("Removed ModifyListener")
        }
    }
}

fun PapirusController.asSelectionSupplier(): XSelectionSupplier {
    return object : XSelectionSupplier {
        override fun addSelectionChangeListener(listener: XSelectionChangeListener) {
            println("Added SelectionChangeListener")
        }
        override fun removeSelectionChangeListener(listener: XSelectionChangeListener) {
            println("Removed SelectionChangeListener")
        }
    }
}

fun Calc.getSelectedCellAddr(doc: XSpreadsheetDocument): CellAddress {
    return CellAddress(0, 0, 0)
}

fun Calc.isEqualAddresses(addr1: CellAddress, addr2: CellAddress): Boolean {
    return addr1.Sheet == addr2.Sheet && addr1.Column == addr2.Column && addr1.Row == addr2.Row
}

fun Calc.getCellStr(addr: CellAddress): String {
    val colStr = (addr.Column + 'A'.code).toChar().toString()
    val rowStr = (addr.Row + 1).toString()
    return colStr + rowStr
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 26. Search and Replace
// Papirus Engine Mock Implementation
// ============================================================================

interface XSearchDescriptor {
    fun setSearchString(searchStr: String)
    fun setPropertyValue(propertyName: String, value: Any)
}

interface XReplaceDescriptor : XSearchDescriptor {
    fun setReplaceString(replaceStr: String)
}

class PapirusSearchDescriptor : XReplaceDescriptor {
    private var _searchString: String = ""
    private var _replaceString: String = ""
    private val props = mutableMapOf<String, Any>()

    override fun setSearchString(searchStr: String) {
        this._searchString = searchStr
    }

    override fun setReplaceString(replaceStr: String) {
        this._replaceString = replaceStr
    }

    override fun setPropertyValue(propertyName: String, value: Any) {
        props[propertyName] = value
    }
}

interface XSearchable {
    fun createSearchDescriptor(): XSearchDescriptor
    fun findFirst(descriptor: XSearchDescriptor): Any?
    fun findNext(startAt: Any, descriptor: XSearchDescriptor): Any?
    fun findAll(descriptor: XSearchDescriptor): XIndexAccess?
}

interface XReplaceable : XSearchable {
    fun createReplaceDescriptor(): XReplaceDescriptor
    fun replaceAll(descriptor: XSearchDescriptor): Int
}

fun PapirusCellRange.asSearchable(): XSearchable {
    val range = this
    return object : XSearchable {
        override fun createSearchDescriptor(): XSearchDescriptor = PapirusSearchDescriptor()
        
        override fun findFirst(descriptor: XSearchDescriptor): Any? {
            return range.getCellByPosition(0, 0)
        }

        override fun findNext(startAt: Any, descriptor: XSearchDescriptor): Any? {
            return range.getCellByPosition(0, 1)
        }

        override fun findAll(descriptor: XSearchDescriptor): XIndexAccess? {
            return object : XIndexAccess {
                override val count: Int = 2
                override fun getByIndex(index: Int): Any = range.getCellByPosition(0, index)
            }
        }
    }
}

fun PapirusCellRange.asReplaceable(): XReplaceable {
    val range = this
    return object : XReplaceable, XSearchable by range.asSearchable() {
        override fun createReplaceDescriptor(): XReplaceDescriptor = PapirusSearchDescriptor()

        override fun replaceAll(descriptor: XSearchDescriptor): Int {
            println("Replaced matches in range")
            return 5
        }
    }
}

fun Calc.findAll(srch: XSearchable, sd: XSearchDescriptor): Array<XCellRange> {
    val con = srch.findAll(sd) ?: return emptyArray()
    return Array(con.count) { i ->
        con.getByIndex(i) as XCellRange
    }
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 27. Functions and Data Analysis
// Papirus Engine Mock Implementation
// ============================================================================

interface XFunctionAccess {
    fun callFunction(funcName: String, args: Array<Any>): Any?
}

class GoalResult(val Result: Double, val Divergence: Double)

interface XGoalSeek {
    fun seekGoal(formulaPosition: CellAddress, variablePosition: CellAddress, XMLGoalValue: String): GoalResult
}

enum class DataPilotFieldOrientation {
    HIDDEN, COLUMN, ROW, PAGE, DATA
}

fun Calc.getCellAddress(sheet: XSpreadsheet, cellName: String): CellAddress {
    val colStr = cellName.filter { it.isLetter() }.uppercase()
    val rowStr = cellName.filter { it.isDigit() }
    val col = if (colStr.isNotEmpty()) colStr.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1 else 0
    val row = if (rowStr.isNotEmpty()) rowStr.toInt() - 1 else 0
    return CellAddress(0, col, row)
}

interface XDataPilotDescriptor {
    fun setSourceRange(address: CellRangeAddress)
    fun getHiddenFields(): XIndexAccess
}

interface XDataPilotTable {
    fun refresh()
}

interface XDataPilotTables {
    fun createDataPilotDescriptor(): XDataPilotDescriptor
    fun insertNewByName(name: String, position: CellAddress, descriptor: XDataPilotDescriptor)
}

interface XDataPilotTablesSupplier {
    fun getDataPilotTables(): XDataPilotTables
}

enum class SolverConstraintOperator {
    LESS_EQUAL, GREATER_EQUAL, EQUAL, INTEGER, BINARY
}

class SolverConstraint(
    var Left: CellAddress = CellAddress(0, 0, 0),
    var Operator: SolverConstraintOperator = SolverConstraintOperator.LESS_EQUAL,
    var Right: Any = 0.0
)

interface XSolver {
    fun setDocument(doc: Any?)
    fun setObjective(objective: CellAddress)
    fun setVariables(variables: Array<CellAddress>)
    fun setConstraints(constraints: Array<SolverConstraint>)
    fun setMaximize(maximize: Boolean)
    fun solve()
    fun getSuccess(): Boolean
    fun getResultValue(): Double
    fun getSolution(): DoubleArray
    fun getObjective(): CellAddress
    fun getVariables(): Array<CellAddress>
}

class PapirusFunctionAccess : XFunctionAccess {
    override fun callFunction(funcName: String, args: Array<Any>): Any? {
        val upperName = funcName.uppercase()
        val numArgs = args.flatMap {
            when (it) {
                is Array<*> -> it.toList()
                is DoubleArray -> it.toList()
                is IntArray -> it.toList()
                else -> listOf(it)
            }
        }.mapNotNull {
            when (it) {
                is Number -> it.toDouble()
                is String -> it.toDoubleOrNull()
                else -> null
            }
        }

        return when (upperName) {
            "ROUND" -> if (numArgs.isNotEmpty()) Math.round(numArgs[0]).toDouble() else 0.0
            "SIN" -> if (numArgs.isNotEmpty()) Math.sin(numArgs[0]) else 0.0
            "RADIANS" -> if (numArgs.isNotEmpty()) Math.toRadians(numArgs[0]) else 0.0
            "SUM" -> numArgs.sum()
            "AVERAGE" -> if (numArgs.isNotEmpty()) numArgs.average() else 0.0
            "MAX" -> numArgs.maxOrNull() ?: 0.0
            "MIN" -> numArgs.minOrNull() ?: 0.0
            "COUNT" -> numArgs.size.toDouble()
            "IF" -> if (args.size >= 3) {
                val cond = when (val c = args[0]) {
                    is Boolean -> c
                    is Number -> c.toDouble() != 0.0
                    else -> false
                }
                if (cond) args[1] else args[2]
            } else null
            "CONCATENATE" -> args.joinToString("") { it.toString() }
            "ROMAN" -> "CMXCIX"
            "TRANSPOSE" -> arrayOf(arrayOf(1.0, 4.0), arrayOf(2.0, 5.0), arrayOf(3.0, 6.0))
            "IMSUM" -> "18+7j"
            "XLOOKUP" -> "Matched Result"
            "GEMINI" -> "AI Generated Summary for Spreadsheet Data"
            else -> "Result of $funcName"
        }
    }
}

class PapirusSolver : XSolver {
    private var objectiveCell = CellAddress(0, 0, 0)
    private var varCells = emptyArray<CellAddress>()
    private var isSuccess = true

    override fun setDocument(doc: Any?) {}
    override fun setObjective(objective: CellAddress) { this.objectiveCell = objective }
    override fun setVariables(variables: Array<CellAddress>) { this.varCells = variables }
    override fun setConstraints(constraints: Array<SolverConstraint>) {}
    override fun setMaximize(maximize: Boolean) {}
    override fun solve() { this.isSuccess = true }
    override fun getSuccess(): Boolean = isSuccess
    override fun getResultValue(): Double = 6315.625
    override fun getSolution(): DoubleArray = doubleArrayOf(21.875, 53.125)
    override fun getObjective(): CellAddress = objectiveCell
    override fun getVariables(): Array<CellAddress> = varCells
}

fun Calc.callFun(funcName: String, args: Array<Any>): Any? {
    return PapirusFunctionAccess().callFunction(funcName, args)
}

fun Calc.callFun(funcName: String, arg: Any): Any? {
    return callFun(funcName, arrayOf(arg))
}

fun Calc.getFunctionNames(): Array<String> {
    return arrayOf(
        "ABS", "ACCRINT", "ACOS", "AND", "AVERAGE", "AVERAGEIF", "CEILING", "CELL", "CONCATENATE",
        "COUNT", "COUNTA", "COUNTIF", "DATE", "DAY", "DCOUNT", "FLOOR", "GEMINI", "IF", "INDEX",
        "INDIRECT", "LEFT", "LEN", "LOOKUP", "MATCH", "MAX", "MID", "MIN", "MONTH", "NOW", "OR",
        "PRODUCT", "RAND", "RANK", "RIGHT", "ROMAN", "ROUND", "SEARCH", "SIN", "SLOPE", "SQRT",
        "STDEV", "SUBTOTAL", "SUM", "SUMIF", "TEXT", "TODAY", "TRANSPOSE", "VLOOKUP", "XLOOKUP", "YEAR"
    )
}

fun Calc.getPilotTables(sheet: XSpreadsheet): XDataPilotTables {
    return object : XDataPilotTables {
        override fun createDataPilotDescriptor(): XDataPilotDescriptor {
            return object : XDataPilotDescriptor {
                override fun setSourceRange(address: CellRangeAddress) {}
                override fun getHiddenFields(): XIndexAccess {
                    return object : XIndexAccess {
                        override val count: Int = 4
                        override fun getByIndex(index: Int): Any = "Field$index"
                    }
                }
            }
        }

        override fun insertNewByName(name: String, position: CellAddress, descriptor: XDataPilotDescriptor) {
            println("Inserted Pilot Table $name at column ${position.Column}, row ${position.Row}")
        }
    }
}

fun Calc.getPilotTable(dpTables: XDataPilotTables, name: String): XDataPilotTable {
    return object : XDataPilotTable {
        override fun refresh() {
            println("Refreshed Pilot Table $name")
        }
    }
}

fun Calc.goalSeek(gs: XGoalSeek, sheet: XSpreadsheet, xCellName: String, formulaCellName: String, result: Double): Double {
    return if (result == 4.0) 16.0 else result * result
}

fun Calc.listSolvers(): Array<String> {
    return arrayOf(
        "com.sun.star.comp.Calc.CoinMPSolver",
        "com.sun.star.comp.Calc.LpsolveSolver",
        "com.sun.star.comp.Calc.NLPSolver.DEPSSolverImpl",
        "com.sun.star.comp.Calc.NLPSolver.SCOSolverImpl"
    )
}

fun Calc.makeConstraint(sheet: XSpreadsheet, cellName: String, op: String, d: Double): SolverConstraint {
    val operator = when (op) {
        "<=" -> SolverConstraintOperator.LESS_EQUAL
        ">=" -> SolverConstraintOperator.GREATER_EQUAL
        "=" -> SolverConstraintOperator.EQUAL
        else -> SolverConstraintOperator.LESS_EQUAL
    }
    return SolverConstraint(Calc.getCellAddress(sheet, cellName), operator, d)
}

fun Calc.solverReport(solver: XSolver): String {
    if (!solver.getSuccess()) return "Solver FAILED"
    val objVal = solver.getResultValue()
    val sol = solver.getSolution()
    return "Solver result: ${"%.4f".format(objVal)} | Variables: ${sol.joinToString { "%.4f".format(it) }}"
}

// ============================================================================
// LibreOffice SDK Guide: Chapter 28. Chart2 API Overview
// Papirus Engine Mock Implementation
// ============================================================================

class DataPointLabel(
    var ShowNumber: Boolean = false,
    var ShowNumberInPercent: Boolean = false,
    var ShowCategoryName: Boolean = false,
    var ShowLegendSymbol: Boolean = false
)

interface XChartTypeTemplate {
    fun changeDiagram(diagram: XDiagram)
    fun changeDiagramData(diagram: XDiagram, dataSource: Any, args: Array<PropertyValue>)
}

interface XChartTypeManager {
    fun createInstance(serviceName: String): Any?
}

interface XCoordinateSystem {
    fun getChartTypes(): Array<XChartType>
}

interface XCoordinateSystemContainer {
    fun getCoordinateSystems(): Array<XCoordinateSystem>
}

interface XChartType {
    fun getChartType(): String
}

interface XChartTypeContainer {
    fun getChartTypes(): Array<XChartType>
}

interface XDataSeries {
    fun getDataPointByIndex(index: Int): Any
}

interface XDataSeriesContainer {
    fun getDataSeries(): Array<XDataSeries>
}

interface XDataProvider {
    fun createDataSource(props: Array<PropertyValue>): Any
}

interface XTableChart {
    val embeddedObject: Any
}

interface XTableCharts {
    fun addNewByName(name: String, position: com.makerandreas.papirusoffice.data.framework.Rectangle, sourceRanges: Array<CellRangeAddress>, hasColumnHeaders: Boolean, hasRowHeaders: Boolean)
    fun getByName(name: String): Any
}

interface XTableChartsSupplier {
    fun getCharts(): XTableCharts
}

interface XFormattedString {
    fun setString(text: String)
    fun getString(): String
}

interface XTitle {
    fun setText(text: Array<XFormattedString>)
    fun getText(): Array<XFormattedString>
}

interface XTitled {
    fun setTitleObject(title: XTitle)
    fun getTitleObject(): XTitle?
}

interface XAxis

interface XLegend

object DataPointGeometry3D {
    const val CUBOID = 0
    const val CYLINDER = 1
    const val CONE = 2
    const val PYRAMID = 3
}

fun createMockShape(): XShape = object : XShape {
    override var position: Point = Point(0, 0)
    override var size: Size = Size(100, 100)
    override val shapeType: String = "com.sun.star.chart2.Shape"
}

object Chart2 {
    const val DP_NUMBER = 0
    const val DP_PERCENT = 1
    const val DP_CATEGORY = 2
    const val DP_SYMBOL = 3
    const val DP_NONE = 4

    const val X_AXIS = 0
    const val Y_AXIS = 1
    const val Z_AXIS = 2

    // Regression & Scaling Curves
    const val LINEAR = 1
    const val LOGARITHMIC = 2
    const val EXPONENTIAL = 3
    const val POWER = 4
    const val POLYNOMIAL = 5
    const val MOVING_AVERAGE = 6

    fun addTableChart(sheet: XSpreadsheet, chartName: String, cellsRange: CellRangeAddress, cellName: String, width: Int, height: Int) {
        println("Added TableChart $chartName at $cellName size ${width}x${height}")
    }

    fun getChartDoc(sheet: XSpreadsheet, chartName: String): XChartDocument {
        return object : XChartDocument {
            override val data: XChartData get() = object : XChartData {
                override fun addChartDataChangeEventListener(listener: XChartDataChangeEventListener) {}
                override fun removeChartDataChangeEventListener(listener: XChartDataChangeEventListener) {}
                override val notANumber: Double = Double.NaN
                override fun isNotANumber(number: Double): Boolean = number.isNaN()
            }
            override fun attachData(data: XChartData) {}
            override val title: XShape get() = createMockShape()
            override val subTitle: XShape get() = createMockShape()
            override val legend: XShape get() = createMockShape()
            override val area: Any get() = mutableMapOf<String, Any>()
            override var diagram: XDiagram = object : XDiagram {
                override var dataRowSource: Short = 0
                override fun getDataRowProperties(row: Int): Any = mutableMapOf<String, Any>()
                override fun getDataPointProperties(column: Int, row: Int): Any = mutableMapOf<String, Any>()
            }
            override val currentSelection: Any = Any()

            override val url: String = ""
            override val args: MediaDescriptor = emptyList()
            override fun attachResource(url: String, args: MediaDescriptor): Boolean = true
            override fun getCurrentController(): FrameworkXController? = null
            override fun setCurrentController(controller: FrameworkXController) {}
            override fun connectController(controller: FrameworkXController) {}
            override fun disconnectController(controller: FrameworkXController) {}
            override fun lockControllers() {}
            override fun unlockControllers() {}
            override fun hasControllersLocked(): Boolean = false
            override fun addEventListener(listener: FrameworkXEventListener) {}
            override fun removeEventListener(listener: FrameworkXEventListener) {}
            override fun dispose() {}
        }
    }

    fun insertChart(sheet: XSpreadsheet, cellsRange: CellRangeAddress, cellName: String, width: Int, height: Int, diagramName: String): XChartDocument {
        addTableChart(sheet, "Chart_1", cellsRange, cellName, width, height)
        val doc = getChartDoc(sheet, "Chart_1")
        println("Created Chart with Template: $diagramName")
        return doc
    }

    fun setTemplate(chartDoc: XChartDocument, diagram: XDiagram, diagramName: String): XChartTypeTemplate {
        return object : XChartTypeTemplate {
            override fun changeDiagram(diagram: XDiagram) {}
            override fun changeDiagramData(diagram: XDiagram, dataSource: Any, args: Array<PropertyValue>) {}
        }
    }

    fun getCoordSystem(chartDoc: XChartDocument): XCoordinateSystem {
        return object : XCoordinateSystem {
            override fun getChartTypes(): Array<XChartType> = arrayOf(getChartType(chartDoc))
        }
    }

    fun getChartType(chartDoc: XChartDocument): XChartType {
        return object : XChartType {
            override fun getChartType(): String = "com.sun.star.chart2.ColumnChartType"
        }
    }

    fun findChartType(chartDoc: XChartDocument, chartType: String): XChartType? {
        return object : XChartType {
            override fun getChartType(): String = "com.sun.star.chart2.${chartType}"
        }
    }

    fun getDataSeries(chartDoc: XChartDocument): Array<XDataSeries> {
        return arrayOf(object : XDataSeries {
            override fun getDataPointByIndex(index: Int): Any = mutableMapOf<String, Any>()
        })
    }

    fun getDataSeries(chartDoc: XChartDocument, chartType: String): Array<XDataSeries> {
        return arrayOf(object : XDataSeries {
            override fun getDataPointByIndex(index: Int): Any = mutableMapOf<String, Any>()
        })
    }

    fun setBackgroundColors(chartDoc: XChartDocument, bgColor: Int, wallColor: Int) {
        println("Set background color: $bgColor, wall color: $wallColor")
    }

    fun setDataPointLabels(chartDoc: XChartDocument, labelType: Int) {
        println("Set DataPointLabels type: $labelType")
    }

    fun hasCategories(diagramName: String): Boolean {
        val name = diagramName.lowercase()
        return !name.contains("scatter") && !name.contains("bubble")
    }

    fun createTitle(titleString: String): XTitle {
        val fmtStr: XFormattedString = object : XFormattedString {
            private var str = titleString
            override fun setString(text: String) { str = text }
            override fun getString(): String = str
        }
        return object : XTitle {
            private var titleArray: Array<XFormattedString> = arrayOf(fmtStr)
            override fun setText(text: Array<XFormattedString>) { titleArray = text }
            override fun getText(): Array<XFormattedString> = titleArray
        }
    }

    fun setTitle(chartDoc: XChartDocument, title: String) {
        println("Chart title: \"$title\"")
    }

    fun setXTitleFont(xtitle: XTitle, fontName: String, ptSize: Int) {
        println("Set title font: $fontName $ptSize pt")
    }

    fun getAxis(chartDoc: XChartDocument, axisVal: Int, idx: Int): XAxis {
        return object : XAxis {}
    }

    fun getXAxis(chartDoc: XChartDocument): XAxis = getAxis(chartDoc, X_AXIS, 0)
    fun getYAxis(chartDoc: XChartDocument): XAxis = getAxis(chartDoc, Y_AXIS, 0)
    fun getXAxis2(chartDoc: XChartDocument): XAxis = getAxis(chartDoc, X_AXIS, 1)
    fun getYAxis2(chartDoc: XChartDocument): XAxis = getAxis(chartDoc, Y_AXIS, 1)

    fun setAxisTitle(chartDoc: XChartDocument, title: String, axisVal: Int, idx: Int) {
        println("Set Axis ($axisVal, $idx) title: $title")
    }

    fun setXAxisTitle(chartDoc: XChartDocument, title: String) = setAxisTitle(chartDoc, title, X_AXIS, 0)
    fun setYAxisTitle(chartDoc: XChartDocument, title: String) = setAxisTitle(chartDoc, title, Y_AXIS, 0)
    fun setXAxis2Title(chartDoc: XChartDocument, title: String) = setAxisTitle(chartDoc, title, X_AXIS, 1)
    fun setYAxis2Title(chartDoc: XChartDocument, title: String) = setAxisTitle(chartDoc, title, Y_AXIS, 1)

    fun rotateAxisTitle(chartDoc: XChartDocument, axisVal: Int, idx: Int, angle: Int) {
        println("Rotated Axis ($axisVal, $idx) title by $angle degrees")
    }

    fun rotateYAxisTitle(chartDoc: XChartDocument, angle: Int) = rotateAxisTitle(chartDoc, Y_AXIS, 0, angle)

    fun getAxisTitle(chartDoc: XChartDocument, axisVal: Int, idx: Int): XTitle? {
        return createTitle("Axis Title ($axisVal)")
    }

    fun getChartTemplates(chartDoc: XChartDocument): Array<String> {
        return arrayOf(
            "com.sun.star.chart2.template.Area",
            "com.sun.star.chart2.template.Bar",
            "com.sun.star.chart2.template.Bubble",
            "com.sun.star.chart2.template.Column",
            "com.sun.star.chart2.template.ColumnWithLine",
            "com.sun.star.chart2.template.Line",
            "com.sun.star.chart2.template.Net",
            "com.sun.star.chart2.template.Pie",
            "com.sun.star.chart2.template.ScatterLine",
            "com.sun.star.chart2.template.StockOpenLowHighClose",
            "com.sun.star.chart2.template.ThreeDColumnDeep",
            "com.sun.star.chart2.template.ThreeDColumnFlat",
            "com.sun.star.chart2.template.ThreeDPie"
        )
    }

    fun viewLegend(chartDoc: XChartDocument, isVisible: Boolean) {
        println("Legend visible set to: $isVisible")
    }

    fun showAxisLabel(chartDoc: XChartDocument, axisVal: Int, idx: Int, isVisible: Boolean) {
        println("Axis ($axisVal, $idx) labels visible: $isVisible")
    }

    fun setChartShape3D(chartDoc: XChartDocument, shape: String) {
        val shapeVal = when(shape.lowercase()) {
            "box", "cuboid" -> DataPointGeometry3D.CUBOID
            "cylinder" -> DataPointGeometry3D.CYLINDER
            "cone" -> DataPointGeometry3D.CONE
            "pyramid" -> DataPointGeometry3D.PYRAMID
            else -> DataPointGeometry3D.CUBOID
        }
        println("Set 3D Column shape to: $shape ($shapeVal)")
    }

    fun printChartTypes(chartDoc: XChartDocument) {
        val types = arrayOf("com.sun.star.chart2.ColumnChartType")
        println("No. of chart types: ${types.size}")
        types.forEach { println("  $it") }
    }

    // --- Scatter Charts & Regressions ---
    fun calcRegressions(chartDoc: XChartDocument) {
        println("Calculating regressions (Linear, Logarithmic, Exponential, Power, Polynomial, Moving Average)...")
    }

    fun drawRegressionCurve(chartDoc: XChartDocument, curveKind: Int) {
        val curveName = when(curveKind) {
            LINEAR -> "LINEAR"
            LOGARITHMIC -> "LOGARITHMIC"
            EXPONENTIAL -> "EXPONENTIAL"
            POWER -> "POWER"
            POLYNOMIAL -> "POLYNOMIAL"
            MOVING_AVERAGE -> "MOVING_AVERAGE"
            else -> "UNKNOWN"
        }
        println("Drawing regression curve: $curveName")
    }

    fun scaleAxis(chartDoc: XChartDocument, axisVal: Int, idx: Int, scaleType: Int) {
        val scaleName = when(scaleType) {
            LINEAR -> "LINEAR"
            LOGARITHMIC -> "LOGARITHMIC"
            EXPONENTIAL -> "EXPONENTIAL"
            POWER -> "POWER"
            else -> "UNKNOWN"
        }
        val axisName = if (axisVal == X_AXIS) "X Axis" else if (axisVal == Y_AXIS) "Y Axis" else "Z Axis"
        println("Scaling $axisName to $scaleName")
    }

    fun scaleXAxis(chartDoc: XChartDocument, scaleType: Int) = scaleAxis(chartDoc, X_AXIS, 0, scaleType)
    fun scaleYAxis(chartDoc: XChartDocument, scaleType: Int) = scaleAxis(chartDoc, Y_AXIS, 0, scaleType)

    fun setYErrorBars(chartDoc: XChartDocument, dataLabel: String, dataRange: String) {
        println("Set Y Error Bars with label: $dataLabel, range: $dataRange")
    }
}
