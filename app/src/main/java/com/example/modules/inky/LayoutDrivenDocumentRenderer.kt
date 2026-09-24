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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DocxEmbeddedImage
import com.makerandreas.papirusoffice.data.*
import java.io.File

/**
 * Lets the screen ask the page stack to move input focus without owning the
 * per-element [FocusRequester]s, which live inside the renderer. The screen
 * calls [requestFirstEditable] before raising the IME; a `false` answer means
 * no field could be focused, so the caller must not assume the keyboard is up.
 */
class RendererFocusBridge {
    internal var focusFirstEditableAction: (() -> Boolean)? = null
    internal var focusElementAction: ((Int) -> Boolean)? = null

    fun requestFirstEditable(): Boolean = focusFirstEditableAction?.invoke() == true

    fun requestElement(elementIndex: Int): Boolean = focusElementAction?.invoke(elementIndex) == true
}

/**
 * Geometry the page stack is built from. The screen needs the same numbers to
 * size the viewport and to decide how much of a page is on screen, so they live
 * here once instead of being copied into the caller.
 */
object PageStackMetrics {
    /** Card width the page stack has always been laid out against. */
    const val BASE_CARD_WIDTH_DP = 320f

    /** Card height used when a document declares no page size. */
    const val FALLBACK_CARD_HEIGHT_DP = 452f

    /** Free space the sheet keeps on each side of the viewport at 100 %. */
    const val GUTTER_DP = 8f
}

// Page-card chrome. These are chrome, not document layout: the gap between two
// sheets and the paper colour stay fixed while the sheet itself scales, so the
// stack still reads as separate pages at 300 % zoom.
private val PAGE_GAP_DP = 16.dp
private val STACK_PADDING_DP = 12.dp
private val PAGE_CORNER_DP = 4.dp
private val PAGE_ELEVATION_DP = 6.dp
private val PAGE_BORDER_DP = 1.dp
private val TABLE_LINE_DP = 0.5.dp

/** Vertical rhythm between blocks inside one page, scaled with the sheet. */
private const val PAGE_BLOCK_GAP_DP = 8f

/**
 * Single page-stack renderer for Viewer and Editor. Viewer mode draws the
 * laid-out pages read-only; Editor mode swaps each textual element for a
 * field whose value is a window into the one global edit string (see
 * [DocumentTextWindows]), so both modes always agree on page count.
 *
 * [viewportWidthDp] is the width the page must fit into. It drives one scale
 * ([fitScale]) used for the card box *and* for the text inside it, so 100 %
 * zoom means "page width fills the viewport" on every screen instead of a
 * fixed 320 dp card that overflows narrow phones.
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
    // Viewer-only: FCT anchor request. A non-null rect asks the screen to show
    // the floating toolbar above that rect; null asks it to hide.
    onViewerToolbarRequest: ((Rect?) -> Unit)? = null,
    viewportWidthDp: Float = 0f,
    focusBridge: RendererFocusBridge? = null,
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

    // Fit-to-width: 100 % zoom means the page box fills the viewport. The same
    // factor scales the card and the text it holds, so a run never renders at
    // a size the layout did not reserve room for.
    val fitScale = if (viewportWidthDp > 0f) viewportWidthDp / PageStackMetrics.BASE_CARD_WIDTH_DP else 1f
    val renderScale = fitScale * zoomScale

    // Which element currently owns the touch selection, if any. Only that element
    // drives the floating toolbar, so neighbouring fields cannot hide it again in
    // the same frame. A collapsed selection is a caret, not a selection.
    val selection = editorValue?.selection
    val selectionWindow = if (selectable && selection != null && !selection.collapsed) {
        DocumentTextWindows.elementForOffset(textWindows, selection.min)
    } else {
        null
    }

    // The screen cannot reach the per-element requesters, so it asks through
    // the bridge. A request that cannot be satisfied reports false instead of
    // throwing inside a swallowed try/catch.
    if (focusBridge != null) {
        SideEffect {
            if (!editable) {
                focusBridge.focusFirstEditableAction = null
                focusBridge.focusElementAction = null
            } else {
                focusBridge.focusFirstEditableAction = {
                    // Ask the elements in document order and stop at the first one
                    // that actually took focus. A requester that was never attached
                    // (element not composed yet) answers false instead of throwing.
                    var focused = false
                    for (target in textWindows.keys.sorted()) {
                        val requester = focusRequesters[target] ?: continue
                        focused = try {
                            requester.requestFocus()
                            true
                        } catch (e: IllegalStateException) {
                            false
                        }
                        if (focused) break
                    }
                    focused
                }
                focusBridge.focusElementAction = { elementIndex ->
                    val requester = if (textWindows.containsKey(elementIndex)) {
                        focusRequesters[elementIndex]
                    } else {
                        null
                    }
                    when (requester) {
                        null -> false
                        else -> try {
                            requester.requestFocus()
                            true
                        } catch (e: IllegalStateException) {
                            false
                        }
                    }
                }
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(PAGE_GAP_DP),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = STACK_PADDING_DP)
    ) {
        if (computed.pages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.inky_pages_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        computed.pages.forEachIndexed { pageIdx, page ->
            // The card keeps the document's aspect ratio: the historical
            // 320x452 card for the Letter fallback box, the page's own ratio
            // once a page size was declared.
            val cardWidthDp = PageStackMetrics.BASE_CARD_WIDTH_DP * renderScale
            val isFallbackBox = page.widthDp == PageStyleSpec.FALLBACK.widthDp &&
                page.heightDp == PageStyleSpec.FALLBACK.heightDp
            val cardHeightDp = when {
                page.widthDp <= 0f || page.heightDp <= 0f || isFallbackBox ->
                    cardWidthDp * (PageStackMetrics.FALLBACK_CARD_HEIGHT_DP / PageStackMetrics.BASE_CARD_WIDTH_DP)
                else -> cardWidthDp * (page.heightDp / page.widthDp)
            }
            // The factor that maps the virtual page onto the sheet on screen:
            // margins and hit-testing use it, so a tap lands on the character
            // the finger points at whatever the page size is. The text itself is
            // still drawn at renderScale; unifying those two is plan 5, where
            // real text measurement replaces the 2.5f measuring hack.
            val pageScale = if (page.widthDp > 0f) cardWidthDp / page.widthDp else renderScale
            Box(
                modifier = Modifier
                    .width(cardWidthDp.dp)
                    .height(cardHeightDp.dp)
                    .shadow(elevation = PAGE_ELEVATION_DP, shape = RoundedCornerShape(PAGE_CORNER_DP))
                    .border(PAGE_BORDER_DP, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(PAGE_CORNER_DP))
                    .background(Color.White)
                    .then(
                        // Edit-mode taps belong to the element fields; the
                        // hit-test tap wiring is Viewer-only.
                        if (editable) {
                            Modifier
                        } else {
                            Modifier.pointerInput(page) {
                                detectTapGestures { offset ->
                                    // Back into the layout's own coordinate space:
                                    // page-local, origin at the paper corner.
                                    val virtualX = offset.x / pageScale
                                    val virtualY = offset.y / pageScale + (pageIdx * page.heightDp)
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
                    // Margins come from the page style the layout engine wrapped
                    // against, not from a fixed guess, so the text column on the
                    // sheet is the column the line breaks were computed for.
                    .padding(
                        start = (pageSpec.marginStartDp * pageScale).dp,
                        end = (pageSpec.marginEndDp * pageScale).dp,
                        top = (pageSpec.marginTopDp * pageScale).dp,
                        bottom = (pageSpec.marginBottomDp * pageScale).dp
                    )
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy((PAGE_BLOCK_GAP_DP * renderScale).dp)
                ) {
                    page.elements.forEach { elemLayout ->
                        RenderLaidOutElement(
                            elemLayout = elemLayout,
                            zoomScale = renderScale,
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
                            onViewerToolbarRequest = onViewerToolbarRequest,
                            selectionElementIndex = selectionWindow?.elementIndex,
                            focusRequester = focusRequesters.getOrPut(elemLayout.elementIndex) { FocusRequester() },
                            onFieldFocused = { focusedElement = elemLayout.elementIndex }
                        )
                    }
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
    onViewerToolbarRequest: ((Rect?) -> Unit)?,
    selectionElementIndex: Int?,
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
                onToolbarRequest = onViewerToolbarRequest,
                isToolbarOwner = selectionElementIndex == elemLayout.elementIndex,
                anySelectionActive = selectionElementIndex != null,
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
        annotatedString = annotated,
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
 * Read-only field for Viewer mode. The value is this element's window into
 * the shared global edit string, so a long-press selection lands in the same
 * model the Editor and the floating text toolbar act on.
 *
 * A read-only field does not raise the platform text toolbar on this path, so
 * the field reports its own selection upward instead: [onToolbarRequest] gets
 * the field bounds while a selection is held, and null when it collapses.
 */
@Composable
private fun ParagraphSelectField(
    window: DocumentTextWindow,
    globalValue: TextFieldValue,
    onSelectionChange: (TextRange) -> Unit,
    onToolbarRequest: ((Rect?) -> Unit)?,
    isToolbarOwner: Boolean,
    anySelectionActive: Boolean,
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
    val hasSelection = selEnd > selStart
    val localValue = TextFieldValue(
        annotatedString = annotated,
        selection = TextRange(minOf(selStart, selEnd), maxOf(selStart, selEnd))
    )

    // Exactly one field owns the selection, so two fields can never fight over
    // showing and hiding the toolbar in the same frame: the owner asks for it,
    // a field that is not the owner stays quiet while a selection is alive, and
    // only a fully collapsed selection hides it.
    val requestToolbar by rememberUpdatedState(onToolbarRequest)
    var fieldBounds by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(isToolbarOwner, anySelectionActive, fieldBounds) {
        val request = requestToolbar ?: return@LaunchedEffect
        when {
            isToolbarOwner && fieldBounds != Rect.Zero -> request(fieldBounds)
            !anySelectionActive -> request(null)
            else -> Unit
        }
    }
    DisposableEffect(Unit) {
        onDispose { requestToolbar?.invoke(null) }
    }

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
                .onGloballyPositioned { coordinates -> fieldBounds = coordinates.boundsInWindow() }
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
private fun RenderTable(
    rows: List<List<String>>,
    zoomScale: Float,
    textColor: Color = Color.Black
) {
    // Grid lines come from the theme instead of fixed greys: R-25 wants at
    // least 4.5:1 for 10 sp cell text, and Color.DarkGray on white is 2.3:1.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(TABLE_LINE_DP, MaterialTheme.colorScheme.outline)
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(TABLE_LINE_DP, MaterialTheme.colorScheme.outlineVariant)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = cell,
                            fontSize = (10 * zoomScale).sp,
                            color = textColor
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
                text = stringResource(R.string.inky_image_placeholder),
                fontSize = (11 * zoomScale).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
