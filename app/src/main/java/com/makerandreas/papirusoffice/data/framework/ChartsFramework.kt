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
interface XFormattedString {
    var string: String
}

interface XTitle {
    var text: Array<XFormattedString>
}

interface XTitled {
    var titleObject: XTitle?
}

// ---------------------------------------------------------
// Chart2 API (New Chart API)
// ---------------------------------------------------------

interface XChartDocument2 : XModel {
    var firstDiagram: XDiagram2
    val dataProvider: XDataProvider
    fun createInternalDataProvider(cloneExistingData: Boolean)
}

interface XDiagram2 {
    var coordinateSystems: Array<XCoordinateSystem>
    var legend: Any // XLegend
    val defaultColorScheme: Any // XColorScheme
    fun setDiagramData(dataSource: Any, arguments: Array<PropertyValue>)
}

interface XCoordinateSystem {
    var coordinateSystemType: String
    val dimension: Int
    fun getAxisByDimension(dimension: Int, index: Int): Any // XAxis
    fun setAxisByDimension(dimension: Int, axis: Any, index: Int)
    val chartTypes: Array<XChartType>
    fun setChartTypes(chartTypes: Array<XChartType>)
    fun addChartType(chartType: XChartType)
    fun removeChartType(chartType: XChartType)
}

interface XChartType {
    val chartType: String
    fun createCoordinateSystem(dimension: Int): XCoordinateSystem
    val dataSeries: Array<XDataSeries>
    fun setDataSeries(dataSeries: Array<XDataSeries>)
    fun addDataSeries(dataSeries: XDataSeries)
    fun removeDataSeries(dataSeries: XDataSeries)
}

interface XDataSeries {
    // Defines a series of data
    fun getDataPointByIndex(index: Int): Any // XPropertySet
    fun resetDataPoint(index: Int)
}

interface XDataProvider {
    fun createDataSource(arguments: Array<PropertyValue>): Any // XDataSource
    fun detectArguments(dataSource: Any): Array<PropertyValue>
    fun createDataSequenceByRangeRepresentation(rangeRepresentation: String): Any // XDataSequence
    fun createDataSequenceByValueArray(role: String, valueArray: String): Any // XDataSequence
}

// ---------------------------------------------------------
// Chart2 Template Names (Chapter 28, 29, 30)
// ---------------------------------------------------------

object Chart2Templates {
    const val COLUMN = "Column"
    const val STACKED_COLUMN = "StackedColumn"
    const val PERCENT_STACKED_COLUMN = "PercentStackedColumn"
    const val BAR = "Bar"
    const val STACKED_BAR = "StackedBar"
    const val PERCENT_STACKED_BAR = "PercentStackedBar"
    const val PIE = "Pie"
    const val PIE_ALL_EXPLODED = "PieAllExploded"
    const val THREE_D_PIE = "ThreeDPie"
    const val THREE_D_PIE_ALL_EXPLODED = "ThreeDPieAllExploded"
    const val DONUT = "Donut"
    const val DONUT_ALL_EXPLODED = "DonutAllExploded"
    const val THREE_D_DONUT = "ThreeDDonut"
    const val AREA = "Area"
    const val STACKED_AREA = "StackedArea"
    const val PERCENT_STACKED_AREA = "PercentStackedArea"
    const val LINE = "Line"
    const val LINE_SYMBOL = "LineSymbol"
    const val STACKED_LINE_SYMBOL = "StackedLineSymbol"
    const val COLUMN_WITH_LINE = "ColumnWithLine"
    const val BUBBLE = "Bubble"
    const val NET = "Net"
    const val NET_LINE = "NetLine"
    const val NET_SYMBOL = "NetSymbol"
    const val STACKED_NET = "StackedNet"
    const val PERCENT_STACKED_NET = "PercentStackedNet"
    const val STOCK_LOW_HIGH_CLOSE = "StockLowHighClose"
    const val STOCK_OPEN_LOW_HIGH_CLOSE = "StockOpenLowHighClose"
    const val STOCK_VOLUME_LOW_HIGH_CLOSE = "StockVolumeLowHighClose"
    const val STOCK_VOLUME_OPEN_LOW_HIGH_CLOSE = "StockVolumeOpenLowHighClose"
}

object ChartDataPointLabel {
    const val DP_NONE = 0
    const val DP_NUMBER = 1
    const val DP_PERCENT = 2
    const val DP_TEXT = 4
    const val DP_SYMBOL = 8
    const val DP_CATEGORY = 16
}

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
// Chart Types
// ---------------------------------------------------------

object ChartTypes {
    const val BAR_DIAGRAM = "com.sun.star.chart.BarDiagram" // Used for Column and Bar charts
    const val LINE_DIAGRAM = "com.sun.star.chart.LineDiagram"
    const val PIE_DIAGRAM = "com.sun.star.chart.PieDiagram"
    const val AREA_DIAGRAM = "com.sun.star.chart.AreaDiagram"
    const val XY_DIAGRAM = "com.sun.star.chart.XYDiagram"
    const val DONUT_DIAGRAM = "com.sun.star.chart.DonutDiagram"
    const val NET_DIAGRAM = "com.sun.star.chart.NetDiagram"
    const val STOCK_DIAGRAM = "com.sun.star.chart.StockDiagram"
    const val BUBBLE_DIAGRAM = "com.sun.star.chart.BubbleDiagram"
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
