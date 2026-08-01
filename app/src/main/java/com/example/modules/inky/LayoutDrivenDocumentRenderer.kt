package com.example.modules.inky

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DocxEmbeddedImage
import com.makerandreas.papirusoffice.data.*

@Composable
fun LayoutDrivenDocumentRenderer(
    document: OfficeDocument,
    zoomScale: Float,
    isEditMode: Boolean,
    cursor: DocumentCursor,
    onCursorChange: (DocumentCursor) -> Unit,
    outlineEngine: OutlineEngine? = null,
    enableOutlineFolding: Boolean = true,
    showImages: Boolean = true,
    showTables: Boolean = true,
    modifier: Modifier = Modifier
) {
    var layoutTrigger by remember { mutableStateOf(0) }
    val layoutEngine = remember { LayoutEngine() }
    val layoutResult = remember(document, layoutTrigger, showImages, showTables, enableOutlineFolding) {
        layoutEngine.performLayout(
            document = document,
            outlineEngine = if (enableOutlineFolding) outlineEngine else null,
            showImages = showImages,
            showTables = showTables
        )
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 12.dp)
    ) {
        if (layoutResult.pages.isEmpty()) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No pages to display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        layoutResult.pages.forEachIndexed { pageIdx, page ->
            Box(
                modifier = Modifier
                    .width((320 * zoomScale).dp)
                    .height((452 * zoomScale).dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .pointerInput(page) {
                        detectTapGestures { offset ->
                            // Scale tap offset back to virtual page layout coordinates
                            val virtualX = offset.x / zoomScale
                            val virtualY = offset.y / zoomScale + (pageIdx * page.heightDp)
                            val hitResult = layoutEngine.hitTest(virtualX, virtualY, layoutResult.pages)
                            if (hitResult != null) {
                                onCursorChange(
                                    DocumentCursor(
                                        elementIndex = hitResult.elementIndex,
                                        paragraphIndex = hitResult.paragraphIndex,
                                        runIndex = hitResult.lineIndex,
                                        offset = hitResult.characterOffset
                                    )
                                )
                            }
                        }
                    }
                    .padding((24 * zoomScale).dp)
            ) {
                // Layout elements inside this page
                Column(
                    verticalArrangement = Arrangement.spacedBy((8 * zoomScale).dp)
                ) {
                    page.elements.forEach { elemLayout ->
                        val element = elemLayout.element
                        when (element) {
                            is OfficeDocElement.ParagraphElement -> {
                                val p = element.paragraph
                                val isCurrentPara = cursor.paragraphIndex == elemLayout.paragraphLayout?.paragraphIndex

                                val isHeading = p.styleName?.contains("Heading") == true
                                val headingModifier = if (isHeading && enableOutlineFolding && outlineEngine != null) {
                                    Modifier.pointerInput(p) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                val pIdx = elemLayout.paragraphLayout?.paragraphIndex
                                                if (pIdx != null) {
                                                    outlineEngine.toggle(pIdx)
                                                    layoutTrigger++
                                                }
                                            }
                                        )
                                    }
                                } else Modifier

                                Text(
                                    text = if (isCurrentPara && isEditMode) {
                                        // Simple caret visualization inside paragraph
                                        val offset = cursor.offset.coerceIn(0, p.text.length)
                                        p.text.substring(0, offset) + "|" + p.text.substring(offset)
                                    } else p.text,
                                    fontSize = (13 * zoomScale).sp,
                                    lineHeight = (18 * zoomScale).sp,
                                    color = Color.Black,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                                    textAlign = when (p.alignment) {
                                        "Center" -> TextAlign.Center
                                        "Right" -> TextAlign.Right
                                        "Justify" -> TextAlign.Justify
                                        else -> TextAlign.Left
                                    },
                                    modifier = Modifier.fillMaxWidth().then(headingModifier)
                                )
                            }
                            is OfficeDocElement.TableElement -> {
                                val table = element.table
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(0.5.dp, Color.Gray)
                                ) {
                                    table.rows.forEach { row ->
                                        Row(modifier = Modifier.fillMaxWidth()) {
                                            row.cells.forEach { cell ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .border(0.5.dp, Color.LightGray)
                                                        .padding(4.dp)
                                                ) {
                                                    Text(
                                                        text = cell.text,
                                                        fontSize = (10 * zoomScale).sp,
                                                        color = Color.DarkGray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is OfficeDocElement.ImageElement -> {
                                val img = element.image
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DocxEmbeddedImage(
                                        imageFile = img.imageFile,
                                        extentCx = if (img.widthDp > 0) (img.widthDp * 9525).toLong() else 1905000L,
                                        extentCy = if (img.heightDp > 0) (img.heightDp * 9525).toLong() else 1428750L
                                    )
                                }
                            }
                            else -> {
                                // Fallback for other layout elements
                            }
                        }
                    }
                }

                // Page Number Indicator Footer
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "Page ${page.pageNumber}",
                        fontSize = (9 * zoomScale).sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
