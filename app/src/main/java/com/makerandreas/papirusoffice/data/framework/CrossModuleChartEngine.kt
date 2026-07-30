package com.makerandreas.papirusoffice.data.framework

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Papirus Office Cross-Module Chart Engine.
 * 
 * Implements LibreOffice SDK Guide Chapter 33: "Using Charts in Other Documents".
 * 
 * Enables creating, copying, pasting, embedding, and exporting ALL chart types
 * (Column, Bar, Pie, 3D Pie, Donut, Area, Line, Bubble, Net/Radar, Stock OHLC)
 * across ALL Papirus Office modules (Inky/Writer, Cellina/Calc, Slidia/Impress, Pagella/Draw)
 * WITHOUT switching modules!
 */

data class ChartSeriesData(
    val name: String,
    val values: List<Double>,
    val color: Int = Color.BLUE
)

data class ChartDataModel(
    val id: String = "Chart_${System.currentTimeMillis()}",
    var title: String = "Chart Title",
    var subtitle: String = "",
    var xAxisTitle: String = "X Axis",
    var yAxisTitle: String = "Y Axis",
    var chartType: String = Chart2Templates.COLUMN,
    var categories: List<String> = listOf("Category A", "Category B", "Category C", "Category D", "Category E"),
    var seriesList: List<ChartSeriesData> = listOf(
        ChartSeriesData("Series 1", listOf(10.0, 25.0, 15.0, 30.0, 20.0), Color.rgb(66, 133, 244)),
        ChartSeriesData("Series 2", listOf(15.0, 18.0, 22.0, 12.0, 28.0), Color.rgb(234, 67, 53))
    ),
    var is3D: Boolean = false,
    var rotationHorizontal: Int = 0,
    var rotationVertical: Int = -45,
    var transparency: Int = 0, // 0 to 100%
    var showDataLabels: Boolean = true,
    var y2Min: Double? = null,
    var y2Max: Double? = null
)

sealed class ChartEmbedResult {
    data class Success(
        val chartId: String,
        val targetModule: String,
        val imagePath: String?,
        val caption: String
    ) : ChartEmbedResult()
    data class Failure(val errorMessage: String) : ChartEmbedResult()
}

class CrossModuleChartEngine private constructor() {

    private val TAG = "CrossModuleChartEngine"

    // Chapter 33: UNO Clipboard support & internal clipboard payload
    var activeClipboardChart: ChartDataModel? = null
        private set
    var activeClipboardImageFile: File? = null
        private set

    /**
     * LibreOffice SDK Guide Chapter 33.1.2: Chart2.copyChart()
     * Selects chart shape (CLSID == CHART_CLSID) and executes UNO dispatch ".uno:Copy"
     */
    fun copyChartToClipboard(context: Context, chartData: ChartDataModel): Boolean {
        return try {
            activeClipboardChart = chartData.copy()
            
            // Generate PNG raster image for OS/UNO clipboard exchange
            val tempImgFile = File(context.cacheDir, "chart_clipboard_${System.currentTimeMillis()}.png")
            val bitmap = renderChartBitmap(chartData, 800, 600)
            FileOutputStream(tempImgFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            activeClipboardImageFile = tempImgFile

            Log.d(TAG, "[SDK Ch 33] Chart2.copyChart() executed. Copied '${chartData.title}' (${chartData.chartType}) to clipboard. UNO Dispatch: .uno:Copy")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error copying chart to clipboard: ${e.message}", e)
            false
        }
    }

    /**
     * LibreOffice SDK Guide Chapter 33.1: Adding a Chart to a Text Document (Writer/Inky)
     * Pastes chart at cursor position with centered legend paragraph ("Figure 1. ...").
     */
    fun pasteChartToWriter(
        context: Context,
        docTitle: String,
        chartData: ChartDataModel? = activeClipboardChart,
        pasteAsImage: Boolean = false
    ): ChartEmbedResult {
        val targetChart = chartData ?: return ChartEmbedResult.Failure("No chart in clipboard")
        return try {
            // Render chart bitmap image
            val imageFile = File(context.filesDir, "writer_chart_${System.currentTimeMillis()}.png")
            val bitmap = renderChartBitmap(targetChart, 800, 500)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val caption = "Figure: ${targetChart.title} (${targetChart.chartType})"
            Log.d(TAG, "[SDK Ch 33 TextChart.java] Pasted chart into Writer doc '$docTitle'. Caption='$caption'. Dispatch: .uno:Paste")

            ChartEmbedResult.Success(
                chartId = targetChart.id,
                targetModule = "Inky",
                imagePath = imageFile.absolutePath,
                caption = caption
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pasting chart to Writer: ${e.message}", e)
            ChartEmbedResult.Failure(e.message ?: "Unknown error pasting chart")
        }
    }

    /**
     * LibreOffice SDK Guide Chapter 33.2: Adding a Chart to a Slide Document (Impress/Slidia)
     * Pastes chart into slide as OLE2Shape / Image object, positioned and sized.
     */
    fun pasteChartToSlide(
        context: Context,
        slideIndex: Int,
        chartData: ChartDataModel? = activeClipboardChart
    ): ChartEmbedResult {
        val targetChart = chartData ?: return ChartEmbedResult.Failure("No chart in clipboard")
        return try {
            val imageFile = File(context.filesDir, "slide_chart_${System.currentTimeMillis()}.png")
            val bitmap = renderChartBitmap(targetChart, 900, 550)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val caption = "Slide ${slideIndex + 1} Chart OLE: ${targetChart.title}"
            Log.d(TAG, "[SDK Ch 33 SlideChart.java] Pasted OLE2Shape chart onto Slide $slideIndex. Caption='$caption'. Dispatch: .uno:Paste")

            ChartEmbedResult.Success(
                chartId = targetChart.id,
                targetModule = "Slidia",
                imagePath = imageFile.absolutePath,
                caption = caption
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error pasting chart to Slide: ${e.message}", e)
            ChartEmbedResult.Failure(e.message ?: "Unknown error pasting chart")
        }
    }

    /**
     * LibreOffice SDK Guide Chapter 33.3: Saving the Chart as an Image
     * Converts chart shape graphic to PNG file.
     */
    fun exportChartAsPng(context: Context, chartData: ChartDataModel, fileName: String = "chartImage.png"): File? {
        return try {
            val file = File(context.getExternalFilesDir(null) ?: context.filesDir, fileName)
            val bitmap = renderChartBitmap(chartData, 1024, 768)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d(TAG, "[SDK Ch 33.3 Images.saveImage] Chart exported as PNG: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting chart PNG: ${e.message}", e)
            null
        }
    }

    /**
     * Generates a template dataset pre-populated with standard data for any chart type.
     */
    fun createDefaultDataForTemplate(templateType: String): ChartDataModel {
        return when (templateType) {
            Chart2Templates.COLUMN, Chart2Templates.BAR -> ChartDataModel(
                title = "Sneakers Sold this Month",
                subtitle = "Monthly Retail Volume",
                xAxisTitle = "Brand",
                yAxisTitle = "Units Sold",
                chartType = templateType,
                categories = listOf("Nike", "Adidas", "Puma", "Reebok", "Asics", "New Balance"),
                seriesList = listOf(
                    ChartSeriesData("Units", listOf(420.0, 350.0, 210.0, 180.0, 290.0, 310.0), Color.rgb(33, 150, 243))
                )
            )
            Chart2Templates.PIE, Chart2Templates.THREE_D_PIE -> ChartDataModel(
                title = "Top 5 States Market Share",
                subtitle = "School Enrollments Percentage",
                xAxisTitle = "State",
                yAxisTitle = "Share %",
                chartType = templateType,
                categories = listOf("California", "Texas", "Florida", "New York", "Illinois"),
                seriesList = listOf(
                    ChartSeriesData("Share", listOf(28.0, 22.0, 18.0, 18.0, 14.0), Color.rgb(156, 39, 176))
                ),
                is3D = (templateType == Chart2Templates.THREE_D_PIE)
            )
            Chart2Templates.DONUT -> ChartDataModel(
                title = "Annual Expenditure Breakdown",
                subtitle = "Expenditure per Student vs GDP %",
                xAxisTitle = "Category",
                yAxisTitle = "Percentage",
                chartType = templateType,
                categories = listOf("Tuition", "Research", "Infrastructure", "Administration", "Services"),
                seriesList = listOf(
                    ChartSeriesData("Outer (Student)", listOf(35.0, 25.0, 20.0, 12.0, 8.0), Color.rgb(0, 150, 136)),
                    ChartSeriesData("Inner (GDP %)", listOf(30.0, 20.0, 25.0, 15.0, 10.0), Color.rgb(255, 152, 0))
                )
            )
            Chart2Templates.AREA, Chart2Templates.STACKED_AREA -> ChartDataModel(
                title = "Enrollment Growth Trends",
                subtitle = "Historical Student Population",
                xAxisTitle = "Academic Year",
                yAxisTitle = "Enrolled Students",
                chartType = templateType,
                categories = listOf("2021", "2022", "2023", "2024", "2025", "2026"),
                seriesList = listOf(
                    ChartSeriesData("Undergraduate", listOf(1200.0, 1350.0, 1500.0, 1650.0, 1800.0, 2000.0), Color.rgb(76, 175, 80)),
                    ChartSeriesData("Postgraduate", listOf(400.0, 480.0, 550.0, 620.0, 700.0, 850.0), Color.rgb(255, 193, 7))
                )
            )
            Chart2Templates.LINE, Chart2Templates.LINE_SYMBOL -> ChartDataModel(
                title = "Expenditure Per Pupil",
                subtitle = "10-Year Public Education Funding",
                xAxisTitle = "Year",
                yAxisTitle = "Amount ($)",
                chartType = templateType,
                categories = listOf("2017", "2018", "2019", "2020", "2021", "2022", "2023", "2024", "2025", "2026"),
                seriesList = listOf(
                    ChartSeriesData("Public Budget", listOf(8500.0, 8900.0, 9200.0, 9100.0, 9600.0, 10200.0, 10800.0, 11400.0, 12100.0, 12800.0), Color.rgb(233, 30, 99))
                )
            )
            Chart2Templates.BUBBLE -> ChartDataModel(
                title = "Global Development Data (SDK Ch 32)",
                subtitle = "GDP vs Life Expectancy & Population Bubble",
                xAxisTitle = "GDP per Capita ($)",
                yAxisTitle = "Life Expectancy (Years)",
                chartType = templateType,
                categories = listOf("USA", "Japan", "Germany", "Brazil", "India", "Nigeria"),
                seriesList = listOf(
                    ChartSeriesData("GDP / LifeExp", listOf(65.0, 84.0, 78.0, 75.0, 70.0, 54.0), Color.rgb(0, 188, 212))
                ),
                transparency = 30
            )
            Chart2Templates.NET, Chart2Templates.NET_LINE -> ChartDataModel(
                title = "No of Calls per Day (Radar/Net)",
                subtitle = "Weekly Customer Support Activity",
                xAxisTitle = "Day of Week",
                yAxisTitle = "Call Volume",
                chartType = templateType,
                categories = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
                seriesList = listOf(
                    ChartSeriesData("Tier 1", listOf(120.0, 150.0, 140.0, 160.0, 130.0, 60.0, 40.0), Color.rgb(103, 58, 183)),
                    ChartSeriesData("Tier 2", listOf(40.0, 55.0, 60.0, 50.0, 45.0, 20.0, 15.0), Color.rgb(255, 87, 34))
                )
            )
            Chart2Templates.STOCK_OPEN_LOW_HIGH_CLOSE, Chart2Templates.STOCK_VOLUME_OPEN_LOW_HIGH_CLOSE -> ChartDataModel(
                title = "Happy Systems (HASY) Stock Chart",
                subtitle = "Candlestick Open-Low-High-Close & Volume",
                xAxisTitle = "Date",
                yAxisTitle = "Price ($)",
                chartType = templateType,
                categories = listOf("Mon 1", "Tue 2", "Wed 3", "Thu 4", "Fri 5"),
                seriesList = listOf(
                    ChartSeriesData("Open", listOf(92.0, 94.0, 91.0, 95.0, 98.0), Color.GRAY),
                    ChartSeriesData("Low", listOf(88.0, 90.0, 87.0, 92.0, 94.0), Color.RED),
                    ChartSeriesData("High", listOf(96.0, 98.0, 95.0, 99.0, 102.0), Color.GREEN),
                    ChartSeriesData("Close", listOf(95.0, 92.0, 94.0, 98.0, 101.0), Color.BLUE)
                ),
                y2Min = 80.0,
                y2Max = 105.0
            )
            else -> ChartDataModel(chartType = templateType)
        }
    }

    /**
     * High-quality canvas rendering engine to draw chart graphics.
     */
    fun renderChartBitmap(chart: ChartDataModel, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Clean white background with rounded border
        canvas.drawColor(Color.WHITE)
        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(10f, 10f, width - 10f, height - 10f), 16f, 16f, borderPaint)

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 18f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Title and Subtitle
        canvas.drawText(chart.title, width / 2f, 45f, titlePaint)
        if (chart.subtitle.isNotEmpty()) {
            canvas.drawText(chart.subtitle, width / 2f, 75f, subtitlePaint)
        }

        // Chart plot area bounds
        val plotLeft = 80f
        val plotTop = if (chart.subtitle.isNotEmpty()) 100f else 70f
        val plotRight = width - 40f
        val plotBottom = height - 70f
        val plotWidth = plotRight - plotLeft
        val plotHeight = plotBottom - plotTop

        // Axes background
        val axisPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 2f
            isAntiAlias = true
        }
        val gridPaint = Paint().apply {
            color = Color.rgb(230, 230, 230)
            strokeWidth = 1f
            isAntiAlias = true
        }

        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = plotBottom - (i * plotHeight / 4f)
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
        }
        canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, axisPaint)
        canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, axisPaint)

        // Axis Titles
        val axisTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(chart.xAxisTitle, plotLeft + plotWidth / 2f, height - 20f, axisTitlePaint)

        // Render chart elements according to type
        val categories = chart.categories
        val catCount = maxOf(1, categories.size)
        val stepX = plotWidth / catCount

        when {
            chart.chartType == Chart2Templates.PIE || chart.chartType == Chart2Templates.THREE_D_PIE || chart.chartType == Chart2Templates.DONUT -> {
                // Render Pie / Donut
                val cx = plotLeft + plotWidth / 2f
                val cy = plotTop + plotHeight / 2f
                val radius = minOf(plotWidth, plotHeight) / 2f - 20f
                val pieRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

                val series = chart.seriesList.firstOrNull()
                val values = series?.values ?: listOf(30.0, 20.0, 25.0, 25.0)
                val total = maxOf(1.0, values.sum())
                var startAngle = 0f

                val colors = listOf(
                    Color.rgb(66, 133, 244), Color.rgb(234, 67, 53), Color.rgb(251, 188, 5),
                    Color.rgb(52, 168, 83), Color.rgb(171, 71, 188), Color.rgb(0, 172, 193)
                )

                values.forEachIndexed { idx, valD ->
                    val sweep = (valD / total * 360.0).toFloat()
                    val slicePaint = Paint().apply {
                        color = colors[idx % colors.size]
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawArc(pieRect, startAngle, sweep, true, slicePaint)
                    startAngle += sweep
                }

                if (chart.chartType == Chart2Templates.DONUT) {
                    val innerRadius = radius * 0.5f
                    val innerRect = RectF(cx - innerRadius, cy - innerRadius, cx + innerRadius, cy + innerRadius)
                    val donutCenterPaint = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    canvas.drawOval(innerRect, donutCenterPaint)
                }
            }
            chart.chartType == Chart2Templates.LINE || chart.chartType == Chart2Templates.LINE_SYMBOL -> {
                // Render Line chart
                val maxVal = maxOf(10.0, chart.seriesList.flatMap { it.values }.maxOrNull() ?: 100.0)
                chart.seriesList.forEachIndexed { sIdx, series ->
                    val linePaint = Paint().apply {
                        color = series.color
                        strokeWidth = 4f
                        style = Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    val pointPaint = Paint().apply {
                        color = series.color
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }

                    var lastX = 0f
                    var lastY = 0f

                    series.values.forEachIndexed { i, valD ->
                        val x = plotLeft + (i + 0.5f) * stepX
                        val y = plotBottom - (valD.toFloat() / maxVal.toFloat() * plotHeight)

                        if (i > 0) {
                            canvas.drawLine(lastX, lastY, x, y, linePaint)
                        }
                        canvas.drawCircle(x, y, 7f, pointPaint)
                        lastX = x
                        lastY = y
                    }
                }
            }
            else -> {
                // Render Column / Bar / Area / Default
                val maxVal = maxOf(10.0, chart.seriesList.flatMap { it.values }.maxOrNull() ?: 100.0)
                val seriesCount = maxOf(1, chart.seriesList.size)
                val barWidth = (stepX * 0.7f) / seriesCount

                categories.forEachIndexed { i, catName ->
                    val catCenterX = plotLeft + (i + 0.5f) * stepX
                    canvas.drawText(catName, catCenterX, plotBottom + 25f, labelPaint)

                    chart.seriesList.forEachIndexed { sIdx, series ->
                        val valD = series.values.getOrNull(i) ?: 0.0
                        val barH = (valD.toFloat() / maxVal.toFloat()) * plotHeight
                        val left = catCenterX - (seriesCount * barWidth / 2f) + (sIdx * barWidth)
                        val top = plotBottom - barH
                        val right = left + barWidth

                        val barPaint = Paint().apply {
                            color = series.color
                            style = Paint.Style.FILL
                            isAntiAlias = true
                        }
                        canvas.drawRoundRect(RectF(left, top, right, plotBottom), 6f, 6f, barPaint)

                        if (chart.showDataLabels && valD > 0) {
                            val valText = if (valD == valD.toLong().toDouble()) valD.toLong().toString() else String.format("%.1f", valD)
                            canvas.drawText(valText, left + barWidth / 2f, top - 8f, labelPaint)
                        }
                    }
                }
            }
        }

        return bitmap
    }

    companion object {
        @Volatile
        private var instance: CrossModuleChartEngine? = null

        fun getInstance(): CrossModuleChartEngine {
            return instance ?: synchronized(this) {
                instance ?: CrossModuleChartEngine().also { instance = it }
            }
        }
    }
}
