package com.makerandreas.papirusoffice.data.framework

// ---------------------------------------------------------
// Drawing Pages and Shapes
// ---------------------------------------------------------

interface XDrawPagesSupplier {
    val drawPages: XDrawPages
}

interface XDrawPages : XIndexAccess {
    fun insertNewByIndex(index: Int): XDrawPage
    fun remove(page: XDrawPage)
}

interface XDrawPage : XShapes // GenericDrawPage properties via XPropertySet

interface XMasterPagesSupplier {
    val masterPages: XDrawPages
}

interface XMasterPageTarget {
    var masterPage: XDrawPage
}

interface XDrawPageDuplicator {
    fun duplicate(page: XDrawPage): XDrawPage
}

interface XControlShape : XShape {
    var control: Any? // Represents XControlModel from Forms
}

interface XShapeGroup : XShape {
    // Methods specific to shape groups
}

interface XShapeGrouper {
    fun group(shapes: XShapes): XShapeGroup
    fun ungroup(group: XShapeGroup)
}

interface XShapeCombiner {
    fun combine(shapes: XShapes): XShape
    fun split(shape: XShape)
}

interface XShapeBinder {
    fun bind(shapes: XShapes): XShape
    fun unbind(shape: XShape)
}

// ---------------------------------------------------------
// Layers
// ---------------------------------------------------------

interface XLayerSupplier {
    val layerManager: XNameAccess // Usually returns XNameAccess which can be cast to XLayerManager
}

interface XLayerManager : XNameAccess, XIndexAccess {
    fun insertNewByIndex(index: Int): XLayer
    fun attachShapeToLayer(shape: XShape, layer: XLayer)
}

interface XLayer {
    // Properties via XPropertySet: Name, IsVisible, IsLocked
}

// ---------------------------------------------------------
// Glue Points
// ---------------------------------------------------------

interface XGluePointsSupplier {
    val gluePoints: Any // Returns XIndexContainer (or similar)
}

data class GluePoint2(
    var IsRelative: Boolean = false,
    var PositionAlignment: Short = 0, // Alignment
    var Escape: Short = 0, // EscapeDirection
    var IsUserDefined: Boolean = false,
    var Position: Point = Point()
)

object Alignment {
    const val TOP_LEFT: Short = 0
    const val TOP: Short = 1
    const val TOP_RIGHT: Short = 2
    const val LEFT: Short = 3
    const val CENTER: Short = 4
    const val RIGHT: Short = 5
    const val BOTTOM_LEFT: Short = 6
    const val BOTTOM: Short = 7
    const val BOTTOM_RIGHT: Short = 8
}

object EscapeDirection {
    const val SMART: Short = 0
    const val LEFT: Short = 1
    const val RIGHT: Short = 2
    const val UP: Short = 3
    const val DOWN: Short = 4
    const val HORIZONTAL: Short = 5
    const val VERTICAL: Short = 6
}

// ---------------------------------------------------------
// Presentation Specific
// ---------------------------------------------------------

interface XPresentationSupplier {
    val presentation: XPresentation
}

interface XPresentation {
    fun start()
    fun end()
    fun rehearseTimings()
}

interface XPresentationPage : XDrawPage {
    val notesPage: XDrawPage
}

interface XHandoutMasterSupplier {
    val handoutMasterPage: XDrawPage
}

interface XCustomPresentationSupplier {
    val customPresentations: XNameContainer
}

interface XDrawView {
    var currentPage: XDrawPage
}

// ---------------------------------------------------------
// Enums and Structs
// ---------------------------------------------------------

object PolygonKind {
    const val LINE: Short = 0
    const val POLY: Short = 1
    const val PLIN: Short = 2
    const val PATHLINE: Short = 3
    const val PATHFILL: Short = 4
}

object PolygonFlags {
    const val NORMAL: Short = 0
    const val SMOOTH: Short = 1
    const val CONTROL: Short = 2
    const val SYMMETRIC: Short = 3
}

data class PolyPolygonBezierCoords(
    var Coordinates: Array<Array<Point>> = emptyArray(),
    var Flags: Array<Array<Short>> = emptyArray()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PolyPolygonBezierCoords
        if (!Coordinates.contentDeepEquals(other.Coordinates)) return false
        if (!Flags.contentDeepEquals(other.Flags)) return false
        return true
    }
    override fun hashCode(): Int {
        var result = Coordinates.contentDeepHashCode()
        result = 31 * result + Flags.contentDeepHashCode()
        return result
    }
}

object LineStyle {
    const val NONE: Short = 0
    const val SOLID: Short = 1
    const val DASH: Short = 2
}

data class LineDash(
    var Dots: Short = 0,
    var DotLen: Int = 0,
    var Dashes: Short = 0,
    var DashLen: Int = 0,
    var Distance: Int = 0
)

object FillStyle {
    const val NONE: Short = 0
    const val SOLID: Short = 1
    const val GRADIENT: Short = 2
    const val HATCH: Short = 3
    const val BITMAP: Short = 4
}

data class Gradient(
    var Style: Short = 0,
    var StartColor: Int = 0,
    var EndColor: Int = 0,
    var Angle: Short = 0,
    var Border: Short = 0,
    var XOffset: Short = 0,
    var YOffset: Short = 0,
    var StartIntensity: Short = 0,
    var EndIntensity: Short = 0,
    var StepCount: Short = 0
)

object GradientStyle {
    const val LINEAR: Short = 0
    const val AXIAL: Short = 1
    const val RADIAL: Short = 2
    const val ELLIPTICAL: Short = 3
    const val SQUARE: Short = 4
    const val RECT: Short = 5
}

object TextFitToSizeType {
    const val NONE: Short = 0
    const val PROPORTIONAL: Short = 1
    const val ALLLINES: Short = 2
    const val RESIZEATTR: Short = 3
}

object FadeEffect {
    const val NONE: Short = 0
    const val FADE_FROM_LEFT: Short = 1
    const val FADE_FROM_TOP: Short = 2
    const val FADE_FROM_RIGHT: Short = 3
    const val FADE_FROM_BOTTOM: Short = 4
    const val FADE_TO_CENTER: Short = 5
    const val FADE_FROM_CENTER: Short = 6
    const val MOVE_FROM_LEFT: Short = 7
    const val MOVE_FROM_TOP: Short = 8
    const val MOVE_FROM_RIGHT: Short = 9
    const val MOVE_FROM_BOTTOM: Short = 10
    const val RANDOM: Short = 19
}

object AnimationSpeed {
    const val SLOW: Short = 0
    const val MEDIUM: Short = 1
    const val FAST: Short = 2
}

object ClickAction {
    const val NONE: Short = 0
    const val PREVPAGE: Short = 1
    const val NEXTPAGE: Short = 2
    const val FIRSTPAGE: Short = 3
    const val LASTPAGE: Short = 4
    const val BOOKMARK: Short = 5
    const val DOCUMENT: Short = 6
    const val INVISIBLE: Short = 7
    const val SOUND: Short = 8
    const val VERB: Short = 9
    const val VANISH: Short = 10
    const val PROGRAM: Short = 11
    const val MACRO: Short = 12
    const val STOPPRESENTATION: Short = 13
}

object AnimationEffect {
    const val NONE: Short = 0
    const val APPEAR: Short = 1
    const val HIDE: Short = 2
    const val WAVYLINE_FROM_BOTTOM: Short = 18
}

data class Hatch(
    var Style: Short = 0,
    var Color: Int = 0,
    var Distance: Int = 0,
    var Angle: Short = 0
)

object HatchStyle {
    const val SINGLE: Short = 0
    const val DOUBLE: Short = 1
    const val TRIPLE: Short = 2
}

object BitmapMode {
    const val REPEAT: Short = 0
    const val STRETCH: Short = 1
    const val NO_REPEAT: Short = 2
}

object DrawShapeTypes {
    const val RECTANGLE = "com.sun.star.drawing.RectangleShape"
    const val ELLIPSE = "com.sun.star.drawing.EllipseShape"
    const val LINE = "com.sun.star.drawing.LineShape"
    const val POLYGON = "com.sun.star.drawing.PolygonShape"
    const val POLY_POLYGON = "com.sun.star.drawing.PolyPolygonShape"
    const val POLY_POLYGON_BEZIER = "com.sun.star.drawing.PolyPolygonBezierShape"
    const val CLOSED_BEZIER = "com.sun.star.drawing.ClosedBezierShape"
    const val OPEN_BEZIER = "com.sun.star.drawing.OpenBezierShape"
    const val GRAPHIC_OBJECT = "com.sun.star.drawing.GraphicObjectShape"
    const val TEXT = "com.sun.star.drawing.TextShape"
    const val CONNECTOR = "com.sun.star.drawing.ConnectorShape"
    const val CAPTION = "com.sun.star.drawing.CaptionShape"
    const val MEASURE = "com.sun.star.drawing.MeasureShape"
    const val FRAME = "com.sun.star.drawing.FrameShape"
    const val OLE2 = "com.sun.star.drawing.OLE2Shape"
    const val PAGE = "com.sun.star.drawing.PageShape"
    const val GROUP = "com.sun.star.drawing.GroupShape"
    const val CONTROL = "com.sun.star.drawing.ControlShape"
    const val TITLE_TEXT = "com.sun.star.presentation.TitleTextShape"
    const val OUTLINER = "com.sun.star.presentation.OutlinerShape"
    const val SUBTITLE = "com.sun.star.presentation.SubtitleShape"
    const val NOTES = "com.sun.star.presentation.NotesShape"
    const val HANDOUT = "com.sun.star.presentation.HandoutShape"
    const val HEADER = "com.sun.star.presentation.HeaderShape"
    const val FOOTER = "com.sun.star.presentation.FooterShape"
    const val SLIDE_NUMBER = "com.sun.star.presentation.SlideNumberShape"
    const val DATE_TIME = "com.sun.star.presentation.DateTimeShape"
}

data class HomogenMatrixLine3(
    var Column1: Double = 0.0,
    var Column2: Double = 0.0,
    var Column3: Double = 0.0
)

data class HomogenMatrix3(
    var Line1: HomogenMatrixLine3 = HomogenMatrixLine3(),
    var Line2: HomogenMatrixLine3 = HomogenMatrixLine3(),
    var Line3: HomogenMatrixLine3 = HomogenMatrixLine3()
)

