package com.example.modules.pagella

import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R

// Data class to store drawn lines
data class LinePath(
    val points: androidx.compose.runtime.snapshots.SnapshotStateList<Offset>,
    val color: Color
)

// The PDF preview is a paper card pinned white, so its body text and page
// edge are document colours, not chrome: they keep their Material-2014 values
// as explicit ARGB and stay legible whatever the app theme does (plan-03 3.11).
private val PaperInkMuted = Color(0xFF444444)
private val PaperEdge = Color(0xFF888888)

@Composable
fun PagellaModule(
    isTablet: Boolean,
    onPdfAction: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    var currentPage by remember { mutableStateOf(1) }
    var totalPages by remember { mutableStateOf(12) }
    var zoomLevel by remember { mutableStateOf(100) }
    
    // Ink Drawing State
    var isInkMode by remember { mutableStateOf(false) }
    var strokeColor by remember { mutableStateOf(Color.Red) }
    
    // Store lines drawn on the canvas
    val paths = remember { mutableStateListOf<LinePath>() }
    var currentPath by remember { mutableStateOf<LinePath?>(null) }

    // Loading Popup state
    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var loadingDocName by remember { mutableStateOf("Pagella_Document.pdf") }
    var loadingProgressStatus by remember { mutableStateOf("") }

    val moduleColor = Color(0xFFDC2626) // Pagella Red

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE2E8F0)) // Cool slate gray boards
        ) {
        // PDF navigation / toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left page controllers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                IconButton(
                    onClick = { if (currentPage > 1) currentPage-- },
                    enabled = currentPage > 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = stringResource(R.string.cd_previous_page))
                }
                Text("Page $currentPage / $totalPages", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                IconButton(
                    onClick = { if (currentPage < totalPages) currentPage++ },
                    enabled = currentPage < totalPages
                ) {
                    Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = stringResource(R.string.cd_next_page))
                }
            }

            // Middle zoom controllers
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { if (zoomLevel > 50) zoomLevel -= 25 }) {
                    Icon(Icons.Default.ZoomOut, contentDescription = stringResource(R.string.cd_zoom_out))
                }
                Text("$zoomLevel%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { if (zoomLevel < 300) zoomLevel += 25 }) {
                    Icon(Icons.Default.ZoomIn, contentDescription = stringResource(R.string.cd_zoom_in))
                }
            }

            // Right stylus ink controllers
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { isInkMode = !isInkMode },
                    modifier = Modifier.testTag("btn_ink_mode")
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = stringResource(R.string.cd_toggle_ink_drawing_annotations),
                        tint = if (isInkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isInkMode) {
                    IconButton(onClick = { paths.clear() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.cd_clear_ink_drawings), tint = MaterialTheme.colorScheme.error)
                    }
                    IconButton(onClick = { onPdfAction("Exported ink annotations to annotated_${currentPage}.png") }) {
                        Icon(Icons.Default.SaveAlt, contentDescription = stringResource(R.string.cd_export_drawings_as_png), tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }

        // Active page render area with ink layer overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxSize()
                    .border(0.5.dp, PaperEdge, MaterialTheme.shapes.medium)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // PDF Document static content render helper
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Papirus Portable Document (Page $currentPage)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "This PDF document is rendered by the Pagella native layout renderer. JNI bridging handles the high-performance rasterization of lines, vectors, and font assets.",
                            color = PaperInkMuted,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "The stylus ink layer enables vector graphics to be annotated directly above text elements, which can be stored as PNG images inside ODF documents or exported as separate vector overlays.",
                            color = PaperInkMuted,
                            lineHeight = 20.sp
                        )
                    }

                    // Touch / Stylus Freehand Ink Drawing Canvas layer
                    if (isInkMode) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("ink_canvas")
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val newPath = LinePath(mutableStateListOf(offset), strokeColor)
                                            paths.add(newPath)
                                            currentPath = newPath
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentPath?.points?.add(change.position)
                                        },
                                        onDragEnd = {
                                            currentPath = null
                                        }
                                    )
                                }
                        ) {
                            paths.forEach { pathItem ->
                                val drawPath = Path()
                                if (pathItem.points.isNotEmpty()) {
                                    val start = pathItem.points.first()
                                    drawPath.moveTo(start.x, start.y)
                                    for (i in 1 until pathItem.points.size) {
                                        val point = pathItem.points[i]
                                        drawPath.lineTo(point.x, point.y)
                                    }
                                    drawPath(
                                        path = drawPath,
                                        color = pathItem.color,
                                        style = Stroke(width = 4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        }

        if (isLoadingDocument) {
            com.example.ui.components.FullPageDocumentLoadingPopup(
                isCreating = isCreatingDoc,
                docName = loadingDocName,
                progressStatus = loadingProgressStatus,
                moduleColor = moduleColor
            )
        }
    }
}
