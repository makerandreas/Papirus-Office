package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Chart Document
// ---------------------------------------------------------

interface XChartDocument : XModel {
    val data: XChartData
    fun attachData(data: XChartData)
    val title: XShape
    val subTitle: XShape
    val legend: XShape
    val area: Any // Represents an XPropertySet
    var diagram: XDiagram

    fun connectController(controller: Any /* XController */)
    fun disconnectController(controller: Any /* XController */)
    var currentController: Any /* XController */
    val currentSelection: Any /* XInterface */
}

// ---------------------------------------------------------
// Chart Data
// ---------------------------------------------------------

interface XChartData {
    fun addChartDataChangeEventListener(listener: XChartDataChangeEventListener)
    fun removeChartDataChangeEventListener(listener: XChartDataChangeEventListener)
    val notANumber: Double
    fun isNotANumber(number: Double): Boolean
}

interface XChartDataArray : XChartData {
    var data: Array<Array<Double>>
    var rowDescriptions: Array<String>
    var columnDescriptions: Array<String>
}

interface XChartDataChangeEventListener : XEventListener {
    fun chartDataChanged(event: ChartDataChangeEvent)
}

data class ChartDataChangeEvent(
    val source: Any,
    var Type: Short = 0, // ChartDataChangeType
    var StartColumn: Int = 0,
    var EndColumn: Int = 0,
    var StartRow: Int = 0,
    var EndRow: Int = 0
)

object ChartDataChangeType {
    const val ALL: Short = 0
    const val DATA: Short = 1
    const val COLUMN_INSERT: Short = 2
    const val ROW_INSERT: Short = 3
    const val COLUMN_DELETE: Short = 4
    const val ROW_DELETE: Short = 5
}

object ChartDataRowSource {
    const val COLUMNS: Short = 0
    const val ROWS: Short = 1
}

// ---------------------------------------------------------
// Diagrams and Axis
// ---------------------------------------------------------

interface XDiagram {
    var dataRowSource: Short // ChartDataRowSource
    fun getDataRowProperties(row: Int): Any // XPropertySet
    fun getDataPointProperties(column: Int, row: Int): Any // XPropertySet
}

interface XAxisXSupplier {
    val xAxis: XShape // Returns XPropertySet
}

interface XAxisYSupplier {
    val yAxis: XShape // Returns XPropertySet
}

interface XAxisZSupplier {
    val zAxis: XShape // Returns XPropertySet
}

interface XTwoAxisXSupplier : XAxisXSupplier {
    val secondaryXAxis: XShape
}

interface XTwoAxisYSupplier : XAxisYSupplier {
    val secondaryYAxis: XShape
}

interface X3DDisplay {
    val wall: Any // XPropertySet
    val floor: Any // XPropertySet
}

data class Direction3D(
    var DirectionX: Double = 0.0,
    var DirectionY: Double = 0.0,
    var DirectionZ: Double = 0.0
)

// ---------------------------------------------------------
// Chart Settings Enums
// ---------------------------------------------------------

object ChartSymbolType {
    const val NONE: Short = -2
    const val AUTO: Short = -1
    const val SYMBOL0: Short = 0
    const val SYMBOL1: Short = 1
    const val SYMBOL2: Short = 2
    const val SYMBOL3: Short = 3
    const val SYMBOL4: Short = 4
    const val SYMBOL5: Short = 5
    const val SYMBOL6: Short = 6
    const val SYMBOL7: Short = 7
    const val BITMAPURL: Short = 8
}

object ChartDataCaption {
    const val NONE: Short = 0
    const val VALUE: Short = 1
    const val PERCENT: Short = 2
    const val TEXT: Short = 4
    const val SYMBOL: Short = 8
}

object ChartRegressionCurveType {
    const val NONE: Short = 0
    const val LINEAR: Short = 1
    const val LOGARITHM: Short = 2
    const val EXPONENTIAL: Short = 3
    const val POWER: Short = 4
}

object ChartErrorCategory {
    const val NONE: Short = 0
    const val VARIANCE: Short = 1
    const val STANDARD_DEVIATION: Short = 2
    const val ERROR_MARGIN: Short = 3
    const val CONSTANT_VALUE: Short = 4
    const val PERCENT: Short = 5
}

object ChartErrorIndicatorType {
    const val NONE: Short = 0
    const val TOP_AND_BOTTOM: Short = 1
    const val UPPER: Short = 2
    const val LOWER: Short = 3
}

object ChartSolidType {
    const val RECTANGULAR_SOLID: Short = 0
    const val CYLINDER: Short = 1
    const val CONE: Short = 2
    const val PYRAMID: Short = 3
}

// ---------------------------------------------------------
// General Interfaces
// ---------------------------------------------------------

interface XEmbeddedObjectSupplier {
    val embeddedObject: XComponent
}

interface XInitialization {
    fun initialize(arguments: Array<Any>)
}

interface XRefreshable {
    fun refresh()
    fun addRefreshListener(listener: XRefreshListener)
    fun removeRefreshListener(listener: XRefreshListener)
}

interface XRefreshListener : XEventListener {
    fun refreshed(event: EventObject)
}
