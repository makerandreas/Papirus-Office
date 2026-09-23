package com.example.modules.inky

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DocxEmbeddedImage
import com.makerandreas.papirusoffice.data.*
import java.io.File

/**
 * Single page-stack renderer for Viewer and Editor. Viewer mode draws the
 * laid-out pages read-only; Editor mode swaps each textual element for a
 * field whose value is a window into the one global edit string (see
 * [DocumentTextWindows]), so both modes always agree on page count.
 */
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
    editorValue: TextFieldValue? = null,
    onEditorValueChange: ((TextFieldValue) -> Unit)? = null,
    // Viewer mode reuses editorValue read-only; selection changes from the
    // read-only fields are mapped back through the element windows.
    onViewerSelectionChange: ((TextRange) -> Unit)? = null,
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

    val editable = isEditMode && editorValue != null && onEditorValueChange != null
    val selectable = !isEditMode && editorValue != null && onViewerSelectionChange != null
    val textWindows = remember(document.body.elements, editorValue?.text, editable, selectable) {
        if (editable || selectable) DocumentTextWindows.compute(document.body.elements, editorValue!!.text) else emptyMap()
    }
    val focusRequesters = remember { HashMap<Int, FocusRequester>() }
    var focusedElement by remember { mutableStateOf(-1) }
    if (editable) {
        val focusTarget = DocumentTextWindows.elementForOffset(textWindows, editorValue!!.selection.min)
        LaunchedEffect(focusTarget?.elementIndex, textWindows) {
            val targetIndex = focusTarget?.elementIndex ?: return@LaunchedEffect
            if (targetIndex != focusedElement) {
                focusRequesters[targetIndex]?.requestFocus()
            }
        }
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
                    .then(
                        // Edit-mode taps belong to the element fields; the
                        // hit-test tap wiring is Viewer-only.
                        if (editable) {
                            Modifier
                        } else {
                            Modifier.pointerInput(page) {
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
                        }
                    )
                    .padding((24 * zoomScale).dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy((8 * zoomScale).dp)
                ) {
                    page.elements.forEach { elemLayout ->
                        RenderLaidOutElement(
                            elemLayout = elemLayout,
                            zoomScale = zoomScale,
                            styles = document.styles,
                            enableOutlineFolding = enableOutlineFolding,
                            outlineEngine = outlineEngine,
                            extractedImages = extractedImages,
                            textColor = textColor,
                            onToggleOutline = { layoutTrigger++ },
                            editableWindow = textWindows[elemLayout.elementIndex],
                            editorValue = editorValue,
                            onEditorValueChange = onEditorValueChange,
                            onViewerSelectionChange = onViewerSelectionChange,
                            focusRequester = focusRequesters.getOrPut(elemLayout.elementIndex) { FocusRequester() },
                            onFieldFocused = { focusedElement = elemLayout.elementIndex }
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
    styles: DocumentStyles,
    enableOutlineFolding: Boolean,
    outlineEngine: OutlineEngine?,
    extractedImages: Map<String, File>,
    textColor: Color,
    onToggleOutline: () -> Unit,
    editableWindow: DocumentTextWindow?,
    editorValue: TextFieldValue?,
    onEditorValueChange: ((TextFieldValue) -> Unit)?,
    onViewerSelectionChange: ((TextRange) -> Unit)?,
    focusRequester: FocusRequester,
    onFieldFocused: () -> Unit
) {
    val element = elemLayout.element
    val paragraphIndex = if (element is OfficeDocElement.ParagraphElement) {
        elemLayout.paragraphLayout?.paragraphIndex
    } else {
        elemLayout.elementIndex
    }

    // One synthesized paragraph per textual element: headings resolve through
    // their "Heading N" name so display and pagination share one style source.
    @Composable
    fun TextOrField(paragraph: OfficeParagraph, leadingPrefix: String? = null) {
        val window = editableWindow
        val value = editorValue
        when {
            window != null && value != null && onEditorValueChange != null -> ParagraphEditField(
                window = window,
                globalValue = value,
                onGlobalChange = onEditorValueChange!!,
                paragraph = paragraph,
                styles = styles,
                leadingPrefix = leadingPrefix,
                zoomScale = zoomScale,
                textColor = textColor,
                focusRequester = focusRequester,
                onFocused = onFieldFocused
            )
            window != null && value != null && onViewerSelectionChange != null -> ParagraphSelectField(
                window = window,
                globalValue = value,
                onSelectionChange = onViewerSelectionChange!!,
                paragraph = paragraph,
                styles = styles,
                leadingPrefix = leadingPrefix,
                zoomScale = zoomScale,
                textColor = textColor
            )
            else -> ParagraphText(
                paragraph = paragraph,
                leadingPrefix = leadingPrefix,
                styles = styles,
                zoomScale = zoomScale,
                enableOutlineFolding = enableOutlineFolding,
                outlineEngine = outlineEngine,
                paragraphIndex = paragraphIndex,
                textColor = textColor,
                onToggleOutline = onToggleOutline
            )
        }
    }

    when (element) {
        is OfficeParagraph -> {
            TextOrField(element)
        }
        is OfficeHeading -> {
            TextOrField(
                OfficeParagraph(
                    text = element.text,
                    styleName = element.styleName ?: "Heading ${element.level}",
                    runs = element.runs
                )
            )
        }
        is OfficeListItem -> {
            TextOrField(
                OfficeParagraph(text = element.text, runs = element.runs),
                leadingPrefix = element.bullet
            )
        }
        is OfficeDocElement.ParagraphElement -> {
            TextOrField(element.paragraph)
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
private fun ParagraphEditField(
    window: DocumentTextWindow,
    globalValue: TextFieldValue,
    onGlobalChange: (TextFieldValue) -> Unit,
    paragraph: OfficeParagraph,
    styles: DocumentStyles,
    leadingPrefix: String?,
    zoomScale: Float,
    textColor: Color,
    focusRequester: FocusRequester,
    onFocused: () -> Unit
) {
    val resolved = remember(paragraph.styleName, styles) {
        OfficeRuns.baseStyle(paragraph, styles)
    }
    val sizeSp = resolved.fontSizeSp
    // Window-scoped view of the paragraph; the field shows window.text so the
    // runs are resliced lazily from the window's block on each re-merge.
    val windowParagraph = remember(window.text, paragraph.styleName, paragraph.alignment, paragraph.runs) {
        paragraph.copy(text = window.text)
    }
    val annotated = remember(windowParagraph, styles, zoomScale, textColor) {
        OfficeRuns.toAnnotatedString(windowParagraph, styles, zoomScale, textColor)
    }

    val selStart = (globalValue.selection.start - window.start).coerceIn(0, window.text.length)
    val selEnd = (globalValue.selection.end - window.start).coerceIn(0, window.text.length)
    val localValue = TextFieldValue(
        text = annotated,
        selection = TextRange(minOf(selStart, selEnd), maxOf(selStart, selEnd)),
        composition = globalValue.composition?.let {
            TextRange(
                (it.start - window.start).coerceIn(0, window.text.length),
                (it.end - window.start).coerceIn(0, window.text.length)
            )
        }
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        if (leadingPrefix != null) {
            Text(
                text = leadingPrefix,
                fontSize = (sizeSp * zoomScale).sp,
                lineHeight = ((sizeSp + 5f) * zoomScale).sp,
                color = textColor,
                fontFamily = OfficeRuns.fontFamilyFor(resolved.fontFamily)
            )
        }
        BasicTextField(
            value = localValue,
            onValueChange = { newLocal ->
                val newGlobalText = DocumentTextWindows.applyLocalEdit(globalValue.text, window, newLocal.text)
                val newSelStart = (newLocal.selection.start + window.start).coerceIn(0, newGlobalText.length)
                val newSelEnd = (newLocal.selection.end + window.start).coerceIn(0, newGlobalText.length)
                onGlobalChange(
                    TextFieldValue(
                        text = newGlobalText,
                        selection = TextRange(newSelStart, newSelEnd),
                        composition = newLocal.composition?.let {
                            TextRange(it.start + window.start, it.end + window.start)
                        }
                    )
                )
            },
            enabled = true,
            readOnly = false,
            textStyle = TextStyle(
                color = textColor,
                fontSize = (sizeSp * zoomScale).sp,
                lineHeight = ((sizeSp + 5f) * zoomScale).sp,
                fontFamily = OfficeRuns.fontFamilyFor(resolved.fontFamily),
                textAlign = OfficeRuns.composeTextAlign(paragraph.alignment ?: resolved.alignment)
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .testTag("doc_body_editor_element_${window.elementIndex}")
        )
    }
}

/**
 * Read-only field for Viewer mode. Selection is reported through
 * [onSelectionChange] as a range in the global edit string, so the FCT
 * (copy, select all, character and paragraph modes) works on the same model
 * the Editor uses. Text edits are ignored by design.
 */
@Composable
private fun ParagraphSelectField(
    window: DocumentTextWindow,
    globalValue: TextFieldValue,
    onSelectionChange: (TextRange) -> Unit,
    paragraph: OfficeParagraph,
    styles: DocumentStyles,
    leadingPrefix: String?,
    zoomScale: Float,
    textColor: Color
) {
    val resolved = remember(paragraph.styleName, styles) {
        OfficeRuns.baseStyle(paragraph, styles)
    }
    val sizeSp = resolved.fontSizeSp
    val windowParagraph = remember(window.text, paragraph.styleName, paragraph.alignment, paragraph.runs) {
        paragraph.copy(text = window.text)
    }
    val annotated = remember(windowParagraph, styles, zoomScale, textColor) {
        OfficeRuns.toAnnotatedString(windowParagraph, styles, zoomScale, textColor)
    }

    val selStart = (globalValue.selection.start - window.start).coerceIn(0, window.text.length)
    val selEnd = (globalValue.selection.end - window.start).coerceIn(0, window.text.length)
    val localValue = TextFieldValue(
        text = annotated,
        selection = TextRange(minOf(selStart, selEnd), maxOf(selStart, selEnd))
    )

    Row(modifier = Modifier.fillMaxWidth()) {
        if (leadingPrefix != null) {
            Text(
                text = leadingPrefix,
                fontSize = (sizeSp * zoomScale).sp,
                lineHeight = ((sizeSp + 5f) * zoomScale).sp,
                color = textColor,
                fontFamily = OfficeRuns.fontFamilyFor(resolved.fontFamily)
            )
        }
        BasicTextField(
            value = localValue,
            onValueChange = { newLocal ->
                if (newLocal.text == window.text) {
                    onSelectionChange(DocumentTextWindows.toGlobalSelection(window, newLocal.selection))
                }
            },
            enabled = true,
            readOnly = true,
            textStyle = TextStyle(
                color = textColor,
                fontSize = (sizeSp * zoomScale).sp,
                lineHeight = ((sizeSp + 5f) * zoomScale).sp,
                fontFamily = OfficeRuns.fontFamilyFor(resolved.fontFamily),
                textAlign = OfficeRuns.composeTextAlign(paragraph.alignment ?: resolved.alignment)
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .testTag("doc_body_viewer_element_${window.elementIndex}")
        )
    }
}

@Composable
private fun ParagraphText(
    paragraph: OfficeParagraph,
    leadingPrefix: String?,
    styles: DocumentStyles,
    zoomScale: Float,
    enableOutlineFolding: Boolean,
    outlineEngine: OutlineEngine?,
    paragraphIndex: Int?,
    textColor: Color,
    onToggleOutline: () -> Unit
) {
    val isHeading = paragraph.styleName?.contains("Heading", ignoreCase = true) == true ||
        paragraph.styleName?.contains("Judul", ignoreCase = true) == true
    val headingModifier = if (isHeading && enableOutlineFolding && outlineEngine != null) {
        Modifier.pointerInput(paragraph.text) {
            detectTapGestures(
                onDoubleTap = {
                    if (paragraphIndex != null) {
                        outlineEngine.toggle(paragraphIndex)
                        onToggleOutline()
                    }
                }
            )
        }
    } else {
        Modifier
    }

    val displayParagraph = if (leadingPrefix != null) paragraph.copy(text = leadingPrefix + paragraph.text) else paragraph
    val resolved = remember(displayParagraph.styleName, styles) {
        OfficeRuns.baseStyle(displayParagraph, styles)
    }
    val sizeSp = resolved.fontSizeSp
    val annotated = remember(displayParagraph, styles, zoomScale, textColor) {
        OfficeRuns.toAnnotatedString(displayParagraph, styles, zoomScale, textColor)
    }

    Text(
        text = annotated,
        fontSize = (sizeSp * zoomScale).sp,
        lineHeight = ((sizeSp + 5f) * zoomScale).sp,
        color = textColor,
        fontFamily = OfficeRuns.fontFamilyFor(resolved.fontFamily),
        textAlign = OfficeRuns.composeTextAlign(paragraph.alignment ?: resolved.alignment),
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
