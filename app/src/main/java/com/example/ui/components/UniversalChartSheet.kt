package com.example.ui.components

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makerandreas.papirusoffice.data.framework.Chart2Templates
import com.makerandreas.papirusoffice.data.framework.ChartDataModel
import com.makerandreas.papirusoffice.data.framework.ChartSeriesData
import com.makerandreas.papirusoffice.data.framework.CrossModuleChartEngine

/**
 * Universal Chart Builder & Inspector (SDK Guide Ch. 33 "Using Charts in Other Documents")
 * 
 * Provides full chart creation, data editing, styling, copy-paste, and embedding capabilities
 * directly across ALL Papirus Office modules (Inky, Cellina, Slidia, Pagella) WITHOUT switching modules!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalChartSheet(
    activeModuleName: String, // "Inky", "Cellina", "Slidia", "Pagella"
    initialChartData: ChartDataModel? = null,
    onDismiss: () -> Unit,
    onInsertChart: (ChartDataModel) -> Unit
) {
    val context = LocalContext.current
    val engine = remember { CrossModuleChartEngine.getInstance() }

    var chartState by remember {
        mutableStateOf(
            initialChartData ?: engine.createDefaultDataForTemplate(Chart2Templates.COLUMN)
        )
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Chart Type, 1 = Data & Titles, 2 = Appearance
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Re-render chart preview whenever chart parameters change
    LaunchedEffect(chartState) {
        previewBitmap = engine.renderChartBitmap(chartState, 800, 500)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                "Universal Chart Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Module: $activeModuleName • SDK Ch. 33 Cross-Document",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chart Live Graphic Preview Canvas
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        previewBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Chart Preview",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        } ?: CircularProgressIndicator()
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Chart Type") },
                        icon = { Icon(Icons.Rounded.PieChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Data & Titles") },
                        icon = { Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Style & 3D") },
                        icon = { Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Contents
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ChartTypeSelectorTab(
                            currentType = chartState.chartType,
                            onSelectTemplate = { newType ->
                                chartState = engine.createDefaultDataForTemplate(newType)
                            }
                        )
                        1 -> ChartDataAndTitlesTab(
                            chart = chartState,
                            onUpdate = { updated -> chartState = updated }
                        )
                        2 -> ChartStyleTab(
                            chart = chartState,
                            onUpdate = { updated -> chartState = updated }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Copy to Clipboard (SDK Ch 33 UNO Copy)
                    OutlinedButton(
                        onClick = {
                            val success = engine.copyChartToClipboard(context, chartState)
                            if (success) {
                                Toast.makeText(context, "Chart copied to UNO Clipboard! (.uno:Copy)", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Chart")
                    }

                    // Paste from Clipboard (SDK Ch 33 UNO Paste)
                    OutlinedButton(
                        onClick = {
                            val clipboardChart = engine.activeClipboardChart
                            if (clipboardChart != null) {
                                chartState = clipboardChart.copy()
                                Toast.makeText(context, "Pasted chart from UNO Clipboard! (.uno:Paste)", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No chart in clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Chart")
                    }

                    // Export PNG (SDK Ch 33 Images.saveImage)
                    OutlinedButton(
                        onClick = {
                            val file = engine.exportChartAsPng(context, chartState)
                            if (file != null) {
                                Toast.makeText(context, "Saved PNG: ${file.name}", Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export PNG")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Primary Insert Button
                    Button(
                        onClick = {
                            onInsertChart(chartState)
                            Toast.makeText(context, "Embedded ${chartState.title} into $activeModuleName!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        }
                    ) {
                        Icon(Icons.Default.AddChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Insert into $activeModuleName")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartTypeSelectorTab(
    currentType: String,
    onSelectTemplate: (String) -> Unit
) {
    val templates = listOf(
        Pair("Column", Chart2Templates.COLUMN),
        Pair("Bar", Chart2Templates.BAR),
        Pair("Pie", Chart2Templates.PIE),
        Pair("3D Pie", Chart2Templates.THREE_D_PIE),
        Pair("Donut", Chart2Templates.DONUT),
        Pair("Area", Chart2Templates.AREA),
        Pair("Line", Chart2Templates.LINE_SYMBOL),
        Pair("Bubble", Chart2Templates.BUBBLE),
        Pair("Net / Radar", Chart2Templates.NET_LINE),
        Pair("Stock (Candlestick)", Chart2Templates.STOCK_OPEN_LOW_HIGH_CLOSE)
    )

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Select Chart Template (SDK Chapters 28–32):", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
        itemsIndexed(templates) { _, (label, type) ->
            val isSelected = (currentType == type)
            Card(
                onClick = { onSelectTemplate(type) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartDataAndTitlesTab(
    chart: ChartDataModel,
    onUpdate: (ChartDataModel) -> Unit
) {
    var title by remember(chart) { mutableStateOf(chart.title) }
    var subtitle by remember(chart) { mutableStateOf(chart.subtitle) }
    var xAxisTitle by remember(chart) { mutableStateOf(chart.xAxisTitle) }
    var yAxisTitle by remember(chart) { mutableStateOf(chart.yAxisTitle) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    onUpdate(chart.copy(title = it))
                },
                label = { Text("Chart Title") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = subtitle,
                onValueChange = {
                    subtitle = it
                    onUpdate(chart.copy(subtitle = it))
                },
                label = { Text("Subtitle") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = xAxisTitle,
                    onValueChange = {
                        xAxisTitle = it
                        onUpdate(chart.copy(xAxisTitle = it))
                    },
                    label = { Text("X Axis Title") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = yAxisTitle,
                    onValueChange = {
                        yAxisTitle = it
                        onUpdate(chart.copy(yAxisTitle = it))
                    },
                    label = { Text("Y Axis Title") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text("Data Categories:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Comma-separated items", fontSize = 12.sp, color = Color.Gray)
            var catText by remember(chart) { mutableStateOf(chart.categories.joinToString(", ")) }
            OutlinedTextField(
                value = catText,
                onValueChange = {
                    catText = it
                    val newCats = it.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }
                    onUpdate(chart.copy(categories = newCats))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ChartStyleTab(
    chart: ChartDataModel,
    onUpdate: (ChartDataModel) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("3D Chart Display Mode", fontWeight = FontWeight.Medium)
            Switch(
                checked = chart.is3D,
                onCheckedChange = { onUpdate(chart.copy(is3D = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Show Data Point Labels", fontWeight = FontWeight.Medium)
            Switch(
                checked = chart.showDataLabels,
                onCheckedChange = { onUpdate(chart.copy(showDataLabels = it)) }
            )
        }

        Column {
            Text("Chart Transparency (${chart.transparency}%)", fontWeight = FontWeight.Medium)
            Slider(
                value = chart.transparency.toFloat(),
                onValueChange = { onUpdate(chart.copy(transparency = it.toInt())) },
                valueRange = 0f..100f
            )
        }
    }
}
