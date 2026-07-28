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
    fun getGluePoints(): XIndexContainer? = gluePoints as? XIndexContainer
}

data class HomogenMatrixLine3(var Column1: Double = 0.0, var Column2: Double = 0.0, var Column3: Double = 0.0)
data class HomogenMatrix3(
    var Line1: HomogenMatrixLine3 = HomogenMatrixLine3(),
    var Line2: HomogenMatrixLine3 = HomogenMatrixLine3(),
    var Line3: HomogenMatrixLine3 = HomogenMatrixLine3()
)
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

    fun getSlidesCount(doc: Any?): Int {
        val slides = getSlides(doc)
        return slides?.count ?: 0
    }

    fun getSlides(doc: Any?): XDrawPages? {
        return (doc as? XDrawPagesSupplier)?.drawPages
    }

    fun getSlide(doc: Any?, idx: Int): XDrawPage? {
        val slides = getSlides(doc) ?: return null
        return getSlide(slides, idx)
    }

    fun getSlide(slides: XDrawPages, idx: Int): XDrawPage? {
        return try {
            slides.getByIndex(idx) as? XDrawPage
        } catch (e: Exception) {
            println("Could not get slide $idx")
            null
        }
    }

    fun getSlideSize(xDrawPage: XDrawPage): Size? {
        return try {
            val props = xDrawPage as? XPropertySet ?: return null
            val width = (props.getPropertyValue("Width") as? Number)?.toLong() ?: 0L
            val height = (props.getPropertyValue("Height") as? Number)?.toLong() ?: 0L
            Size(width / 100, height / 100)
        } catch (e: Exception) {
            println("Could not get page dimensions")
            null
        }
    }

    fun makeShape(shapeType: String, x: Long, y: Long, width: Long, height: Long): XShape? {
        return try {
            val fullType = if (shapeType.startsWith("com.sun.star.")) shapeType else "com.sun.star.drawing.$shapeType"
            val shape = DummyShape(fullType)
            shape.position = Point(x * 100, y * 100)
            shape.size = Size(width * 100, height * 100)
            shape
        } catch (e: Exception) {
            println("Unable to create shape: $shapeType")
            null
        }
    }

    fun addShape(slide: XDrawPage, shapeType: String, x: Long, y: Long, width: Long, height: Long): XShape? {
        val shape = makeShape(shapeType, x, y, width, height)
        if (shape != null) {
            slide.add(shape)
        }
        return shape
    }
    fun drawLine(slide: XDrawPage, x1: Long, y1: Long, x2: Long, y2: Long): XShape? {
        if (x1 == x2 && y1 == y2) {
            println("Line is a point")
            return null
        }
        val width = x2 - x1
        val height = y2 - y1
        return addShape(slide, "LineShape", x1, y1, width, height)
    }

    fun drawPolarLine(slide: XDrawPage, x: Long, y: Long, angle: Double, length: Long): XShape? {
        val rad = Math.toRadians(angle)
        val x2 = (x + length * Math.cos(rad)).toLong()
        val y2 = (y + length * Math.sin(rad)).toLong()
        return drawLine(slide, x, y, x2, y2)
    }

    fun setDashedLine(shape: XShape, isDashed: Boolean) {
        val ld = LineDash().apply {
            Dots = 0
            DotLen = 100
            Dashes = 5
            DashLen = 200
            Distance = 200
        }
        val props = shape as? XPropertySet ?: return
        try {
            if (isDashed) {
                props.setPropertyValue("LineStyle", LineStyle.DASH)
                props.setPropertyValue("LineDash", ld)
            } else {
                props.setPropertyValue("LineStyle", LineStyle.SOLID)
            }
        } catch (e: Exception) {
            println("Could not set dashed line property")
        }
    }

    fun drawEllipse(slide: XDrawPage, x: Long, y: Long, width: Long, height: Long): XShape? {
        return addShape(slide, "EllipseShape", x, y, width, height)
    }

    fun drawCircle(slide: XDrawPage, x: Long, y: Long, radius: Long): XShape? {
        return drawEllipse(slide, x - radius, y - radius, radius * 2, radius * 2)
    }

    fun drawRectangle(slide: XDrawPage, x: Long, y: Long, width: Long, height: Long): XShape? {
        return addShape(slide, "RectangleShape", x, y, width, height)
    }

    fun setGradientColor(shape: XShape, name: String) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillStyle", FillStyle.GRADIENT)
            props.setPropertyValue("FillGradientName", name)
        } catch (e: Exception) {
            println("Could not set gradient color name $name")
        }
    }

    fun setGradientColor(shape: XShape, startColor: Int, endColor: Int, angle: Int = 0) {
        val grad = Gradient().apply {
            Style = GradientStyle.LINEAR
            StartColor = startColor
            EndColor = endColor
            Angle = (angle * 10).toShort()
            Border = 0
            XOffset = 0
            YOffset = 0
            StartIntensity = 100
            EndIntensity = 100
            StepCount = 10
        }
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillStyle", FillStyle.GRADIENT)
            props.setPropertyValue("FillGradient", grad)
        } catch (e: Exception) {
            println("Could not set gradient colors")
        }
    }

    fun setHatchingColor(shape: XShape, name: String) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillStyle", FillStyle.HATCH)
            props.setPropertyValue("FillHatchName", name)
        } catch (e: Exception) {
            println("Could not set hatching color name $name")
        }
    }

    fun setBitmapColor(shape: XShape, name: String) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillStyle", FillStyle.BITMAP)
            props.setPropertyValue("FillBitmapName", name)
        } catch (e: Exception) {
            println("Could not set bitmap color name $name")
        }
    }

    fun setBitmapFileColor(shape: XShape, fnm: String) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillStyle", FillStyle.BITMAP)
            props.setPropertyValue("FillBitmapURL", fnm)
        } catch (e: Exception) {
            println("Could not set bitmap file color $fnm")
        }
    }

    fun setTransparency(shape: XShape, level: Int) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("FillTransparence", level.toShort())
        } catch (e: Exception) {
            println("Could not set transparency")
        }
    }

    fun drawText(slide: XDrawPage, msg: String, x: Long, y: Long, width: Long, height: Long, fontSize: Int = 0): XShape? {
        val shape = addShape(slide, "TextShape", x, y, width, height) ?: return null
        addText(shape, msg, fontSize)
        return shape
    }
    fun addText(shape: XShape, msg: String, fontSize: Int = 0) {
        val xText = shape as? XText ?: return
        val cursor = xText.createTextCursor()
        cursor.gotoEnd(false)
        if (fontSize > 0) {
            (cursor as? XPropertySet)?.setPropertyValue("CharHeight", fontSize.toFloat())
        }
        val range = cursor as? XTextRange
        range?.string = msg
    }

    fun getTextProperties(xShape: XShape): XPropertySet? {
        val xText = xShape as? XText ?: return null
        val cursor = xText.createTextCursor()
        cursor.gotoStart(false)
        cursor.gotoEnd(true)
        return cursor as? XPropertySet
    }

    fun findShapeByName(slide: XDrawPage, shapeName: String): XShape? {
        val shapes = getShapes(slide)
        for (shape in shapes) {
            val props = shape as? XPropertySet
            val nm = props?.getPropertyValue("Name") as? String
            if (nm == shapeName) return shape
        }
        return null
    }
    fun getShapes(slide: XDrawPage): List<XShape> {
        val list = mutableListOf<XShape>()
        for (i in 0 until slide.count) {
            val s = slide.getByIndex(i) as? XShape
            if (s != null) list.add(s)
        }
        return list
    }

    fun reportPosSize(shape: XShape?) {
        if (shape == null) {
            println("The shape is null")
            return
        }
        val props = shape as? XPropertySet
        val nm = props?.getPropertyValue("Name")
        println("Shape name: $nm")
        println("  Type: ${shape.shapeType}")
        val pt = shape.position
        val sz = shape.size
        println("  Position/size: (${pt.X / 100}, ${pt.Y / 100}) / (${sz.Width / 100}, ${sz.Height / 100})")
    }

    fun drawFormula(slide: XDrawPage, formula: String, x: Long, y: Long, width: Long, height: Long): XShape? {
        val shape = addShape(slide, "OLE2Shape", x, y, width, height) ?: return null
        val props = shape as? XPropertySet
        props?.setPropertyValue("CLSID", "078B7ABA-54FC-457F-8551-A48011E4826C")
        val model = props?.getPropertyValue("Model") as? XModel
        if (model is XPropertySet) {
            model.setPropertyValue("Formula", formula)
        }
        return shape
    }
    fun drawPolygon(slide: XDrawPage, x: Long, y: Long, radius: Long, nSides: Int): XShape? {
        val polygon = addShape(slide, "PolyPolygonShape", 0, 0, 0, 0) ?: return null
        val pts = genPolygonPoints(x, y, radius, nSides)
        val polys = arrayOf(pts)
        (polygon as? XPropertySet)?.setPropertyValue("PolyPolygon", polys)
        return polygon
    }

    fun genPolygonPoints(x: Long, y: Long, radius: Long, nSides: Int): Array<Point> {
        var sides = nSides
        if (sides < 3) sides = 3
        if (sides > 30) sides = 30
        val pts = Array(sides) { Point() }
        val angleStep = Math.PI / sides
        for (i in 0 until sides) {
            pts[i] = Point(
                Math.round((x * 100) + (radius * 100) * Math.cos(i * 2 * angleStep)),
                Math.round((y * 100) + (radius * 100) * Math.sin(i * 2 * angleStep))
            )
        }
        return pts
    }

    fun drawLines(slide: XDrawPage, xs: LongArray, ys: LongArray): XShape? {
        if (xs.size != ys.size) {
            println("The two arrays must be the same length")
            return null
        }
        val numPoints = xs.size
        val pts = Array(numPoints) { i ->
            Point(xs[i] * 100, ys[i] * 100)
        }
        val linePaths = arrayOf(pts)
        val polyLine = addShape(slide, "PolyLineShape", 0, 0, 0, 0) ?: return null
        (polyLine as? XPropertySet)?.setPropertyValue("PolyPolygon", linePaths)
        return polyLine
    }

    // Chapter 14: Animation & Gallery Helpers

    fun drawImage(slide: XDrawPage, imFnm: String, x: Long, y: Long, width: Long, height: Long): XShape? {
        println("Adding the picture \"$imFnm\"")
        val imShape = addShape(slide, "GraphicObjectShape", x, y, width, height) ?: return null
        setImage(imShape, imFnm)
        setLineStyle(imShape, LineStyle.NONE)
        return imShape
    }

    fun drawImage(slide: XDrawPage, imFnm: String, x: Long, y: Long): XShape? {
        // Default size fallback if dimensions not specified
        return drawImage(slide, imFnm, x, y, 90, 50)
    }

    fun setImage(shape: XShape, imFnm: String) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("GraphicURL", imFnm)
        } catch (e: Exception) {
            println("Could not set GraphicURL")
        }
    }

    fun setLineStyle(shape: XShape, style: Short) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("LineStyle", style)
        } catch (e: Exception) {
            println("Could not set LineStyle")
        }
    }

    fun getPosition(shape: XShape): Point {
        val pt = shape.position
        return Point(pt.X / 100, pt.Y / 100)
    }

    fun setPosition(shape: XShape, x: Long, y: Long) {
        shape.position = Point(x * 100, y * 100)
    }

    fun getRotation(shape: XShape): Int {
        val props = shape as? XPropertySet ?: return 0
        val rot = props.getPropertyValue("RotateAngle")
        return if (rot is Number) rot.toInt() / 100 else 0
    }

    fun setRotation(shape: XShape, angle: Int) {
        val props = shape as? XPropertySet ?: return
        try {
            props.setPropertyValue("RotateAngle", angle * 100)
        } catch (e: Exception) {
            println("Could not set RotateAngle")
        }
    }

    fun getTransformation(shape: XShape): HomogenMatrix3 {
        val props = shape as? XPropertySet
        val matrix = props?.getPropertyValue("Transformation") as? HomogenMatrix3
        return matrix ?: HomogenMatrix3()
    }

    fun printMatrix(mat: HomogenMatrix3) {
        println("Transformation Matrix:")
        println("\t${mat.Line1.Column1}\t${mat.Line1.Column2}\t${mat.Line1.Column3}")
        println("\t${mat.Line2.Column1}\t${mat.Line2.Column2}\t${mat.Line2.Column3}")
        println("\t${mat.Line3.Column1}\t${mat.Line3.Column2}\t${mat.Line3.Column3}")

        val radAngle = Math.atan2(mat.Line2.Column1.toDouble(), mat.Line1.Column1.toDouble())
        val currAngle = Math.round(Math.toDegrees(radAngle)).toInt()
        println("  Current angle: $currAngle")
    }

    fun animShapes(currSlide: XDrawPage) {
        // Circle moving right while shrinking
        var xc = 40L
        var yc = 150L
        var radius = 40L
        var circle: XShape? = null
        for (i in 0 until 20) {
            if (circle != null) currSlide.remove(circle)
            circle = drawCircle(currSlide, xc, yc, radius)
            xc += 5
            radius = (radius * 0.95).toLong()
        }

        // Line rotating counter-clockwise
        var x2 = 140L
        var y2 = 110L
        var line: XShape? = null
        for (i in 0..25) {
            if (line != null) currSlide.remove(line)
            line = drawLine(currSlide, 40, 100, x2, y2)
            x2 -= 4
            y2 -= 4
        }
    }

    fun animateBike(currSlide: XDrawPage, fnm: String) {
        val shape = drawImage(currSlide, fnm, 60, 100, 90, 50) ?: return
        val pt = getPosition(shape)
        val angle = getRotation(shape)
        for (i in 0..18) {
            setPosition(shape, pt.X + (i * 5), pt.Y)
            setRotation(shape, angle + (i * 5))
        }
        printMatrix(getTransformation(shape))
    }

    // Chapter 15: Complex Shapes (Connectors, Grouping, Binding, Combining, Bezier)

    const val CONNECT_TOP = 0
    const val CONNECT_RIGHT = 1
    const val CONNECT_BOTTOM = 2
    const val CONNECT_LEFT = 3

    const val MERGE = 0
    const val INTERSECT = 1
    const val SUBTRACT = 2
    const val COMBINE = 3

    fun addConnector(
        slide: XDrawPage,
        shape1: XShape,
        fromConnect: Int,
        shape2: XShape,
        toConnect: Int,
        edgeKind: Short = ConnectorType.STANDARD
    ): XShape? {
        val xConnector = addShape(slide, "ConnectorShape", 0, 0, 0, 0) ?: return null
        val props = xConnector as? XPropertySet
        try {
            props?.setPropertyValue("StartShape", shape1)
            props?.setPropertyValue("StartGluePointIndex", fromConnect)
            props?.setPropertyValue("EndShape", shape2)
            props?.setPropertyValue("EndGluePointIndex", toConnect)
            props?.setPropertyValue("EdgeKind", edgeKind)
        } catch (e: Exception) {
            println("Could not connect the shapes")
        }
        return xConnector
    }

    fun getGluePoints(shape: XShape): Array<GluePoint2>? {
        val gpSupp = shape as? XGluePointsSupplier
        val gluePts = gpSupp?.getGluePoints() ?: return arrayOf(
            GluePoint2(true, Alignment.TOP, EscapeDirection.UP, false, Point(0, -1000)),
            GluePoint2(true, Alignment.RIGHT, EscapeDirection.RIGHT, false, Point(1000, 0)),
            GluePoint2(true, Alignment.BOTTOM, EscapeDirection.DOWN, false, Point(0, 1000)),
            GluePoint2(true, Alignment.LEFT, EscapeDirection.LEFT, false, Point(-1000, 0))
        )
        val numGPs = gluePts.count
        if (numGPs == 0) return null
        return Array(numGPs) { i ->
            (gluePts.getByIndex(i) as? GluePoint2) ?: GluePoint2()
        }
    }

    fun combineShape(doc: Any?, shapes: XShapes, combineOp: Int): XShape? {
        val opName = when (combineOp) {
            MERGE -> "Merge"
            INTERSECT -> "Intersect"
            SUBTRACT -> "Substract"
            COMBINE -> "Combine"
            else -> "Merge"
        }
        println("Executing dispatch command: $opName on ${shapes.count} shapes")
        val combinedShape = DummyShape("com.sun.star.drawing.PolyPolygonShape")
        if (shapes is XDrawPage) {
            shapes.add(combinedShape)
        }
        return combinedShape
    }

    fun drawBezier(slide: XDrawPage, pts: Array<Point>, flags: Array<Short>, isOpen: Boolean): XShape? {
        if (pts.size != flags.size) {
            println("Mismatch in lengths of points and flags array")
            return null
        }
        val bezierType = if (isOpen) "OpenBezierShape" else "ClosedBezierShape"
        val bezierPoly = addShape(slide, bezierType, 0, 0, 0, 0) ?: return null

        val aCoords = PolyPolygonBezierCoords().apply {
            Coordinates = arrayOf(pts)
            Flags = arrayOf(flags)
        }
        (bezierPoly as? XPropertySet)?.setPropertyValue("PolyPolygonBezier", aCoords)
        return bezierPoly
    }

    fun showShapesInfo(currSlide: XDrawPage) {
        println("Draw Page shapes (count=${currSlide.count}):")
        for (i in 0 until currSlide.count) {
            val shape = currSlide.getByIndex(i) as? XShape
            val service = shape?.shapeType ?: "UnknownShape"
            println("  Shape service: $service; z-order: $i")
        }
    }
}

object ConnectorType {
    const val STANDARD: Short = 0
    const val CURVE: Short = 1
    const val LINE: Short = 2
    const val LINES: Short = 3
}

interface XGalleryItem {
    val name: String
    val path: String
    val title: String
    val type: String
}

interface XGalleryTheme {
    val name: String
    val count: Int
    fun getByIndex(index: Int): XGalleryItem?
}

interface XGalleryThemeProvider {
    fun getThemeNames(): Array<String>
    fun getTheme(name: String): XGalleryTheme?
}

object Gallery {
    fun reportGallerys() {
        println("Gallery Themes list initialized")
    }
    fun reportGalleryItems(themeName: String) {
        println("Listing gallery items for theme: $themeName")
    }
    fun findGalleryItem(themeName: String, itemName: String): XGalleryItem? {
        println("Finding item $itemName in theme $themeName")
        return null
    }
    fun reportGalleryItem(item: XGalleryItem?) {
        if (item == null) {
            println("Gallery item is null")
            return
        }
        println("Gallery item information:")
        println("  Fnm: \"${item.name}\"")
        println("  Path: \"${item.path}\"")
        println("  Title: \"${item.title}\"")
        println("  Type: ${item.type}")
    }
}

open class DummyShape(override val shapeType: String) : XShape, XPropertySet, XText {
    override var position: Point = Point()
    override var size: Size = Size()
    private val propertyMap = mutableMapOf<String, Any?>()
    override fun getPropertySetInfo(): XPropertySetInfo {
        return object : XPropertySetInfo {
            override fun getProperties(): Array<Property> = emptyArray()
            override fun getPropertyByName(name: String): Property = Property(name, 0, String::class.java, 0)
            override fun hasPropertyByName(name: String): Boolean = true
        }
    }
    override fun setPropertyValue(propertyName: String, value: Any) {
        propertyMap[propertyName] = value
    }
    override fun getPropertyValue(propertyName: String): Any {
        return propertyMap[propertyName] ?: ""
    }
    override fun addPropertyChangeListener(propertyName: String, listener: Any) {}
    override fun removePropertyChangeListener(propertyName: String, listener: Any) {}
    override fun addVetoableChangeListener(propertyName: String, listener: Any) {}
    override fun removeVetoableChangeListener(propertyName: String, listener: Any) {}
    private var textContent: String = ""
    override fun createTextCursor(): XTextCursor {
        return DummyTextCursor(this)
    }
    override fun createTextCursorByRange(textPosition: XTextRange): XTextCursor {
        return DummyTextCursor(this)
    }
    override fun insertString(range: XTextRange, string: String, absorb: Boolean) {
        textContent += string
    }
    override fun insertControlCharacter(range: XTextRange, controlCharacter: Short, absorb: Boolean) {}
    override fun insertTextContent(range: XTextRange, content: XTextContent, absorb: Boolean) {}
    override fun removeTextContent(content: XTextContent) {}
    override val text: XText get() = this
    override val start: XTextRange get() = this
    override val end: XTextRange get() = this
    override var string: String
        get() = textContent
        set(value) { textContent = value }
}

class DummyTextCursor(private val hostRange: XTextRange) : XTextCursor, XPropertySet {
    private val propertyMap = mutableMapOf<String, Any?>()
    override fun collapseToStart() {}
    override fun collapseToEnd() {}
    override fun isCollapsed(): Boolean = true
    override fun goLeft(count: Short, expand: Boolean): Boolean = true
    override fun goRight(count: Short, expand: Boolean): Boolean = true
    override fun gotoStart(expand: Boolean) {}
    override fun gotoEnd(expand: Boolean) {}
    override fun gotoRange(range: XTextRange, expand: Boolean) {}
    override val text: XText get() = hostRange.text
    override val start: XTextRange get() = hostRange.start
    override val end: XTextRange get() = hostRange.end
    override var string: String
        get() = hostRange.string
        set(value) { hostRange.string = value }
    override fun getPropertySetInfo(): XPropertySetInfo {
        return object : XPropertySetInfo {
            override fun getProperties(): Array<Property> = emptyArray()
            override fun getPropertyByName(name: String): Property = Property(name, 0, String::class.java, 0)
            override fun hasPropertyByName(name: String): Boolean = true
        }
    }
    override fun setPropertyValue(propertyName: String, value: Any) { propertyMap[propertyName] = value }
    override fun getPropertyValue(propertyName: String): Any = propertyMap[propertyName] ?: ""
    override fun addPropertyChangeListener(propertyName: String, listener: Any) {}
    override fun removePropertyChangeListener(propertyName: String, listener: Any) {}
    override fun addVetoableChangeListener(propertyName: String, listener: Any) {}
    override fun removeVetoableChangeListener(propertyName: String, listener: Any) {}
}

typealias Draw = DrawShapeTypes

const val LAYOUT_TITLE_SUB = 0
const val LAYOUT_TITLE_BULLETS = 1
const val LAYOUT_TITLE_ONLY = 19
const val LAYOUT_BLANK = 20

const val TITLE_TEXT = "com.sun.star.presentation.TitleTextShape"
const val SUBTITLE_TEXT = "com.sun.star.presentation.SubtitleShape"
const val BULLETS_TEXT = "com.sun.star.presentation.OutlinerShape"

fun addSlide(doc: Any?): XDrawPage? {
    val slides = Draw.getSlides(doc) ?: return null
    val idx = slides.count
    return slides.insertNewByIndex(idx) as? XDrawPage
}

fun titleSlide(currSlide: XDrawPage, title: String, subTitle: String) {
    val props = currSlide as? XPropertySet
    props?.setPropertyValue("Layout", LAYOUT_TITLE_SUB.toShort())
    
    val titleShape = findShapeByType(currSlide, TITLE_TEXT)
    (titleShape as? XText)?.let { it.string = title }
    
    val subTitleShape = findShapeByType(currSlide, SUBTITLE_TEXT)
    (subTitleShape as? XText)?.let { it.string = subTitle }
}

fun bulletsSlide(currSlide: XDrawPage, title: String): XText? {
    val props = currSlide as? XPropertySet
    props?.setPropertyValue("Layout", LAYOUT_TITLE_BULLETS.toShort())

    val titleShape = findShapeByType(currSlide, TITLE_TEXT)
    (titleShape as? XText)?.let { it.string = title }

    val bulletsShape = findShapeByType(currSlide, BULLETS_TEXT)
    return bulletsShape as? XText
}

fun titleOnlySlide(currSlide: XDrawPage, header: String) {
    val props = currSlide as? XPropertySet
    props?.setPropertyValue("Layout", LAYOUT_TITLE_ONLY.toShort())

    val titleShape = findShapeByType(currSlide, TITLE_TEXT)
    (titleShape as? XText)?.let { it.string = header }
}

fun blankSlide(currSlide: XDrawPage) {
    val props = currSlide as? XPropertySet
    props?.setPropertyValue("Layout", LAYOUT_BLANK.toShort())
}

fun addBullet(bullsText: XText?, level: Int, text: String) {
    if (bullsText == null) return
    val tr = bullsText.end
    (tr as? XPropertySet)?.setPropertyValue("NumberingLevel", level.toShort())
    tr.string = "$text\n"
}

fun findShapeByType(slide: XDrawPage, shapeType: String): XShape? {
    val shapes = Draw.getShapes(slide)
    for (shape in shapes) {
        if (shape.shapeType == shapeType) return shape
    }
    return null
}

fun drawImageOffset(slide: XDrawPage, imFnm: String, xOffset: Double, yOffset: Double): XShape? {
    var xOff = xOffset
    var yOff = yOffset
    if (xOff < 0 || xOff >= 1) xOff = 0.5
    if (yOff < 0 || yOff >= 1) yOff = 0.5

    val slideSize = Draw.getSlideSize(slide) ?: return null
    val x = Math.round(slideSize.Width * xOff).toLong()
    val y = Math.round(slideSize.Height * yOff).toLong()
    
    val width = 50L  // arbitrary width for now
    val height = 50L // arbitrary height for now

    return Draw.addShape(slide, "GraphicObjectShape", x, y, width, height)
}

fun drawMedia(slide: XDrawPage, fnm: String, x: Long, y: Long, width: Long, height: Long): XShape? {
    val shape = Draw.addShape(slide, "MediaShape", x, y, width, height)
    val props = shape as? XPropertySet
    props?.setPropertyValue("MediaURL", fnm)
    props?.setPropertyValue("Loop", true)
    return shape
}
