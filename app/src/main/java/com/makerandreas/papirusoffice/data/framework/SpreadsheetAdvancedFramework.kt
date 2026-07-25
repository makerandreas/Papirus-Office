package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// DataPilot (Pivot Tables)
// ---------------------------------------------------------

interface XDataPilotTablesSupplier {
    val dataPilotTables: XDataPilotTables
}

interface XDataPilotTables : XNameAccess {
    fun createDataPilotDescriptor(): XDataPilotDescriptor
    fun insertNewByName(name: String, outputAddress: CellAddress, descriptor: XDataPilotDescriptor)
    fun removeByName(name: String)
}

interface XDataPilotDescriptor {
    fun setSourceRange(sourceRange: CellRangeAddress)
    val dataPilotFields: XIndexAccess // Returns DataPilotFields
    val columnFields: XIndexAccess
    val rowFields: XIndexAccess
    val pageFields: XIndexAccess
    val dataFields: XIndexAccess
    val hiddenFields: XIndexAccess
    fun setTag(tag: String)
    val filterDescriptor: XSheetFilterDescriptor
}

interface XDataPilotField {
    var orientation: Short // DataPilotFieldOrientation
    var function: Short // GeneralFunction
}

object DataPilotFieldOrientation {
    const val HIDDEN: Short = 0
    const val COLUMN: Short = 1
    const val ROW: Short = 2
    const val PAGE: Short = 3
    const val DATA: Short = 4
}

object GeneralFunction {
    const val NONE: Short = 0
    const val AUTO: Short = 1
    const val SUM: Short = 2
    const val COUNT: Short = 3
    const val AVERAGE: Short = 4
    const val MAX: Short = 5
    const val MIN: Short = 6
    const val PRODUCT: Short = 7
    const val COUNTNUMS: Short = 8
    const val STDEV: Short = 9
    const val STDEVP: Short = 10
    const val VAR: Short = 11
    const val VARP: Short = 12
}

// ---------------------------------------------------------
// Scenarios
// ---------------------------------------------------------

interface XScenariosSupplier {
    val scenarios: XScenarios
}

interface XScenarios : XNameAccess, XIndexAccess {
    fun addNewByName(name: String, ranges: Array<CellRangeAddress>, comment: String)
    fun removeByName(name: String)
}

interface XScenario {
    val isScenario: Boolean
    var scenarioComment: String
    fun addRanges(ranges: Array<CellRangeAddress>)
    fun apply()
}

// ---------------------------------------------------------
// Outlines
// ---------------------------------------------------------

interface XSheetOutline {
    fun group(range: CellRangeAddress, orientation: Short)
    fun ungroup(range: CellRangeAddress, orientation: Short)
    fun autoOutline(range: CellRangeAddress)
    fun clearOutline()
    fun hideDetail(range: CellRangeAddress)
    fun showDetail(range: CellRangeAddress)
    fun showLevel(level: Short, orientation: Short)
}

// ---------------------------------------------------------
// Auditing / Detective
// ---------------------------------------------------------

interface XSheetAuditing {
    fun hideDependents(position: CellAddress)
    fun hidePrecedents(position: CellAddress)
    fun showDependents(position: CellAddress)
    fun showPrecedents(position: CellAddress)
    fun showErrors(position: CellAddress)
    fun showInvalid()
    fun clearArrows()
}

// ---------------------------------------------------------
// Consolidation
// ---------------------------------------------------------

interface XConsolidatable {
    fun createConsolidationDescriptor(): XConsolidationDescriptor
    fun consolidate(descriptor: XConsolidationDescriptor)
}

interface XConsolidationDescriptor {
    var function: Short
    var sources: Array<CellRangeAddress>
    var startOutputPosition: CellAddress
    var useColumnHeaders: Boolean
    var useRowHeaders: Boolean
    var insertLinks: Boolean
}

// ---------------------------------------------------------
// Charts in Spreadsheet
// ---------------------------------------------------------

interface XTableChartsSupplier {
    val charts: XTableCharts
}

interface XTableCharts : XNameAccess, XIndexAccess {
    fun addNewByName(name: String, rect: Any /* Rectangle */, ranges: Array<CellRangeAddress>, columnHeaders: Boolean, rowHeaders: Boolean)
    fun removeByName(name: String)
}

interface XTableChart {
    val hasColumnHeaders: Boolean
    val hasRowHeaders: Boolean
    var ranges: Array<CellRangeAddress>
    fun setHasColumnHeaders(hasColumnHeaders: Boolean)
    fun setHasRowHeaders(hasRowHeaders: Boolean)
}

// ---------------------------------------------------------
// Data Validation
// ---------------------------------------------------------

interface XSheetCondition {
    var operator: Short // ConditionOperator
    var formula1: String
    var formula2: String
    var sourcePosition: CellAddress
}

object ConditionOperator {
    const val NONE: Short = 0
    const val EQUAL: Short = 1
    const val NOT_EQUAL: Short = 2
    const val GREATER: Short = 3
    const val GREATER_EQUAL: Short = 4
    const val LESS: Short = 5
    const val LESS_EQUAL: Short = 6
    const val BETWEEN: Short = 7
    const val NOT_BETWEEN: Short = 8
    const val FORMULA: Short = 9
}

object ValidationType {
    const val ANY: Short = 0
    const val WHOLE: Short = 1
    const val DECIMAL: Short = 2
    const val DATE: Short = 3
    const val TIME: Short = 4
    const val TEXT_LEN: Short = 5
    const val LIST: Short = 6
    const val CUSTOM: Short = 7
}

object ValidationAlertStyle {
    const val STOP: Short = 0
    const val WARNING: Short = 1
    const val INFO: Short = 2
    const val MACRO: Short = 3
}
