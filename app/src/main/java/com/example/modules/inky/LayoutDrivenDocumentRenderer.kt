package com.example.modules.inky

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DocxEmbeddedImage
import com.makerandreas.papirusoffice.data.*
import java.io.File

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
    layoutResult: DocumentLayoutResult? = null,
    extractedImages: Map<String, File> = emptyMap(),
    pageSpec: PageStyleSpec = PageStyleSpec.FALLBACK,
    textColor: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    var layoutTrigger by remember { mutableStateOf(0) }
    val layoutEngine = remember(pageSpec) { LayoutEngine(pageSpec) }
    val computed = remember(document, layoutTrigger, showImages, showTables, enableOutlineFolding, layoutResult) {
        layoutResult ?: layoutEngine.performLayout(
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
        if (computed.pages.isEmpty()) {
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

        computed.pages.forEachIndexed { pageIdx, page ->
            // Page card aspect follows the laid-out page box; the historical
            // 320x452 card is kept while pages sit at the Letter fallback.
            val cardWidthDp = 320f * zoomScale
            val isFallbackBox = page.widthDp == PageStyleSpec.FALLBACK.widthDp &&
                page.heightDp == PageStyleSpec.FALLBACK.heightDp
            val cardHeightDp = when {
                page.widthDp <= 0f || page.heightDp <= 0f || isFallbackBox -> 452f * zoomScale
                else -> cardWidthDp * (page.heightDp / page.widthDp)
            }
            Box(
                modifier = Modifier
                    .width(cardWidthDp.dp)
                    .height(cardHeightDp.dp)
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .pointerInput(page) {
                        detectTapGestures { offset ->
                            val virtualX = offset.x / zoomScale
                            val virtualY = offset.y / zoomScale + (pageIdx * page.heightDp)
                            val hitResult = layoutEngine.hitTest(virtualX, virtualY, computed.pages)
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
                Column(
                    verticalArrangement = Arrangement.spacedBy((8 * zoomScale).dp)
                ) {
                    page.elements.forEach { elemLayout ->
                        RenderLaidOutElement(
                            elemLayout = elemLayout,
                            zoomScale = zoomScale,
                            isEditMode = isEditMode,
                            cursor = cursor,
                            enableOutlineFolding = enableOutlineFolding,
                            outlineEngine = outlineEngine,
                            extractedImages = extractedImages,
                            textColor = textColor,
                            onToggleOutline = { layoutTrigger++ }
                        )
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        text = "${page.pageNumber} / ${computed.pages.size}",
                        fontSize = (9 * zoomScale).sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderLaidOutElement(
    elemLayout: PageElementLayout,
    zoomScale: Float,
    isEditMode: Boolean,
    cursor: DocumentCursor,
    enableOutlineFolding: Boolean,
    outlineEngine: OutlineEngine?,
    extractedImages: Map<String, File>,
    textColor: Color,
    onToggleOutline: () -> Unit
) {
    val element = elemLayout.element
    when (element) {
        is OfficeParagraph -> {
            ParagraphText(
                text = element.text,
                styleName = element.styleName,
                alignment = element.alignment,
                zoomScale = zoomScale,
                isCurrent = cursor.paragraphIndex == elemLayout.elementIndex,
                isEditMode = isEditMode,
                cursor = cursor,
                enableOutlineFolding = enableOutlineFolding,
                outlineEngine = outlineEngine,
                paragraphIndex = elemLayout.elementIndex,
                textColor = textColor,
                onToggleOutline = onToggleOutline
            )
        }
        is OfficeHeading -> {
            ParagraphText(
                text = element.text,
                styleName = element.styleName ?: "Heading ${element.level}",
                alignment = null,
                zoomScale = zoomScale,
                isCurrent = cursor.paragraphIndex == elemLayout.elementIndex,
                isEditMode = isEditMode,
                cursor = cursor,
                enableOutlineFolding = enableOutlineFolding,
                outlineEngine = outlineEngine,
                paragraphIndex = elemLayout.elementIndex,
                textColor = textColor,
                onToggleOutline = onToggleOutline,
                forceHeading = true,
                headingLevel = element.level
            )
        }
        is OfficeListItem -> {
            ParagraphText(
                text = "${element.bullet}${element.text}",
                styleName = null,
                alignment = null,
                zoomScale = zoomScale,
                isCurrent = cursor.paragraphIndex == elemLayout.elementIndex,
                isEditMode = isEditMode,
                cursor = cursor,
                enableOutlineFolding = false,
                outlineEngine = null,
                paragraphIndex = elemLayout.elementIndex,
                textColor = textColor,
                onToggleOutline = onToggleOutline
            )
        }
        is OfficeDocElement.ParagraphElement -> {
            val p = element.paragraph
            ParagraphText(
                text = p.text,
                styleName = p.styleName,
                alignment = p.alignment,
                zoomScale = zoomScale,
                isCurrent = cursor.paragraphIndex == elemLayout.paragraphLayout?.paragraphIndex,
                isEditMode = isEditMode,
                cursor = cursor,
                enableOutlineFolding = enableOutlineFolding,
                outlineEngine = outlineEngine,
                paragraphIndex = elemLayout.paragraphLayout?.paragraphIndex,
                textColor = textColor,
                onToggleOutline = onToggleOutline
            )
        }
        is OfficeTable -> {
            RenderTable(element.rows.map { it.cells.map { c -> c.text } }, zoomScale)
        }
        is OfficeDocElement.TableElement -> {
            RenderTable(element.table.rows.map { it.cells.map { c -> c.text } }, zoomScale)
        }
        is OfficeImage -> {
            RenderImage(element.imageFile, element.imagePath, element.widthDp, element.heightDp, extractedImages, zoomScale)
        }
        is OfficeDocElement.ImageElement -> {
            val img = element.image
            RenderImage(img.imageFile, img.imagePath, img.widthDp, img.heightDp, extractedImages, zoomScale)
        }
        else -> { }
    }
}

@Composable
private fun ParagraphText(
    text: String,
    styleName: String?,
    alignment: String?,
    zoomScale: Float,
    isCurrent: Boolean,
    isEditMode: Boolean,
    cursor: DocumentCursor,
    enableOutlineFolding: Boolean,
    outlineEngine: OutlineEngine?,
    paragraphIndex: Int?,
    textColor: Color,
    onToggleOutline: () -> Unit,
    forceHeading: Boolean = false,
    headingLevel: Int = 1
) {
    val isHeading = forceHeading || styleName?.contains("Heading", ignoreCase = true) == true ||
        styleName?.contains("Judul", ignoreCase = true) == true
    val headingModifier = if (isHeading && enableOutlineFolding && outlineEngine != null) {
        Modifier.pointerInput(text) {
            detectTapGestures(
                onDoubleTap = {
                    if (paragraphIndex != null) {
                        outlineEngine.toggle(paragraphIndex)
                        onToggleOutline()
                    }
                }
            )
        }
    } else Modifier

    val display = if (isCurrent && isEditMode) {
        val offset = cursor.offset.coerceIn(0, text.length)
        text.substring(0, offset) + "|" + text.substring(offset)
    } else text

    val sizeSp = when {
        isHeading && headingLevel <= 1 -> 18f
        isHeading && headingLevel == 2 -> 16f
        isHeading -> 14f
        else -> 13f
    }

    Text(
        text = display,
        fontSize = (sizeSp * zoomScale).sp,
        lineHeight = ((sizeSp + 5f) * zoomScale).sp,
        color = textColor,
        fontFamily = FontFamily.Default,
        fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
        textAlign = when (alignment) {
            "Center" -> TextAlign.Center
            "Right" -> TextAlign.Right
            "Justify" -> TextAlign.Justify
            else -> TextAlign.Left
        },
        modifier = Modifier.fillMaxWidth().then(headingModifier)
    )
}

@Composable
private fun RenderTable(rows: List<List<String>>, zoomScale: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.Gray)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color.LightGray)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = cell,
                            fontSize = (10 * zoomScale).sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderImage(
    imageFile: File?,
    imagePath: String,
    widthDp: Float,
    heightDp: Float,
    extractedImages: Map<String, File>,
    zoomScale: Float
) {
    val lower = imagePath.lowercase(java.util.Locale.ROOT)
    val fileNameLower = imagePath.substringAfterLast('/').lowercase(java.util.Locale.ROOT)
    val resolved = imageFile?.takeIf { it.exists() }
        ?: extractedImages[imagePath]
        ?: extractedImages[lower]
        ?: extractedImages[imagePath.substringAfterLast('/')]
        ?: extractedImages[fileNameLower]
        ?: extractedImages.values.firstOrNull { it.name.equals(imagePath.substringAfterLast('/'), ignoreCase = true) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (resolved != null && resolved.exists()) {
            DocxEmbeddedImage(
                imageFile = resolved,
                extentCx = if (widthDp > 0) (widthDp * 9525).toLong() else 1905000L,
                extentCy = if (heightDp > 0) (heightDp * 9525).toLong() else 1428750L
            )
        } else {
            Text(
                text = "[Image]",
                fontSize = (11 * zoomScale).sp,
                color = Color.Gray
            )
        }
    }
}
