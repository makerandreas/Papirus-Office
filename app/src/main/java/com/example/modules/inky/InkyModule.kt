package com.example.modules.inky
import androidx.compose.material.icons.automirrored.rounded.*
import android.util.Log
import kotlin.math.roundToInt
import com.makerandreas.papirusoffice.data.toOfficeDocument
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.SaveAsDialog
import com.example.ui.components.CloudSyncBar
import com.example.ui.components.GeminiCopilotDialog
import com.example.core.util.TemplateManager
import com.example.ui.home.RecentFilesTracker
import com.example.ui.home.ShortcutCard
import com.example.ui.home.RecentsEmptyStateIllustration
import androidx.compose.ui.res.stringResource
import com.example.R

import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import androidx.activity.compose.BackHandler
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.equation.EquationParser
import com.example.core.ai.GeminiAiService
import com.example.ui.components.FloatingContextualToolbar
import com.example.ui.theme.ThemeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Safe helper to find the ComponentActivity from any wrapped context
fun android.content.Context.findActivity(): androidx.activity.ComponentActivity? {
    var currentContext = this
    while (currentContext is android.content.ContextWrapper) {
        if (currentContext is androidx.activity.ComponentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InkyModule(
    isTablet: Boolean,
    onFormatAction: (String) -> Unit,
    dynamicColorEnabled: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember { com.makerandreas.papirusoffice.data.InkyPreferencesRepository(context) }
    val viewOptions by preferencesRepository.viewOptionsFlow.collectAsState(initial = com.makerandreas.papirusoffice.data.InkyViewOptions())
    val scrollState = rememberScrollState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    val focusRequester = remember { FocusRequester() }

    // --- Inky Core States ---
    var isEditMode by remember { mutableStateOf(false) } // False = Viewer Mode, True = Edit Mode
    var editorMode by remember { mutableStateOf(if (isEditMode) com.example.modules.inky.state.EditorMode.EDIT else com.example.modules.inky.state.EditorMode.VIEW) }
    var isWebView by remember { mutableStateOf(false) }  // False = Normal View, True = Web View
    var isDarkDocument by remember { mutableStateOf(false) } // Dark document canvas mode
    var isSaved by remember { mutableStateOf(true) }     // Tracks saved indicator suffix
    var isNewDocument by remember { mutableStateOf(com.example.MainActivity.openedFilePath == null) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showRestartConfirmDialog by remember { mutableStateOf(false) }
    var currentSaveMimeType by remember { mutableStateOf("application/vnd.oasis.opendocument.text") }
    var currentSaveDefaultFilename by remember { mutableStateOf("Document.odt") }

    var docTitle by remember {
        mutableStateOf(
            if (com.example.MainActivity.openedFilePath != null && com.example.MainActivity.openedFileType == "Inky") {
                java.io.File(com.example.MainActivity.openedFilePath!!).name
            } else {
                "Document.odt"
            }
        )
    }

    var detectedDocPageCount by remember { mutableStateOf<Int?>(null) }

    // Bottom Bar (Ribbon & sub-decks) States
    var showBottomBar by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showPasteSpecialDialog by remember { mutableStateOf(false) }
    var showUniversalChartSheet by remember { mutableStateOf(false) }
    var showUniversalFormsSheet by remember { mutableStateOf(false) }
    var showUniversalPrintSheet by remember { mutableStateOf(false) }
    var showUniversalEmailSheet by remember { mutableStateOf(false) }
    var showUniversalClipboardSheet by remember { mutableStateOf(false) }
    var showUniversalXmlImportSheet by remember { mutableStateOf(false) }
    var showUniversalOdfSheet by remember { mutableStateOf(false) }



    // Density and screen width helpers for precise layout/FCT sizing
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val screenWidthDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp
    var isFctShownByTap by remember { mutableStateOf(false) }
    var pageBoxCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var previousScrollBeforeKeyboard by remember { mutableStateOf(0) }
    var bodyTextLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    var bodyTextFieldCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    var viewportCoordinates by remember { mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(null) }
    // Width the page must fit into. 100 % zoom means "page width fills this",
    // so the status-bar figure and the page box always agree.
    var viewportWidthDp by remember { mutableStateOf(0f) }

    // The screen cannot own the page stack's FocusRequesters; the renderer
    // registers its handlers here so the keyboard button and the sheet-close
    // path can focus a real field before asking the IME to appear.
    val rendererFocusBridge = remember { RendererFocusBridge() }

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    val isKeyboardVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible

    // Zoom and dynamic typing states
    var zoomScale by remember { mutableStateOf(1.0f) }

    LaunchedEffect(viewOptions) {
        val calculated = when (viewOptions.zoomMode) {
            com.makerandreas.papirusoffice.data.ZoomMode.HUNDRED -> 1.0f
            com.makerandreas.papirusoffice.data.ZoomMode.FIT_WIDTH -> 1.35f
            com.makerandreas.papirusoffice.data.ZoomMode.CUSTOM -> viewOptions.customZoomPercent.toFloat() / 100f
            com.makerandreas.papirusoffice.data.ZoomMode.LAST -> 1.15f
        }
        if (Math.abs(zoomScale - calculated) > 0.01f) {
            zoomScale = calculated
        }
    }

    LaunchedEffect(zoomScale) {
        // Debounced: restarting on every frame collapses pinch-zoom write spam
        // into a single DataStore write when the gesture settles.
        kotlinx.coroutines.delay(500)
        val calculatedPercent = (zoomScale * 100).toInt().coerceIn(25, 400)
        if (calculatedPercent != viewOptions.customZoomPercent) {
            preferencesRepository.updateCustomZoomPercent(calculatedPercent)
            if (viewOptions.zoomMode != com.makerandreas.papirusoffice.data.ZoomMode.CUSTOM) {
                preferencesRepository.updateZoomMode(com.makerandreas.papirusoffice.data.ZoomMode.CUSTOM)
            }
        }
    }

    val untitledDocumentTitle = stringResource(R.string.default_document_title)
    var documentContentTitle by remember { mutableStateOf(untitledDocumentTitle) }

    val docxParser = remember { com.makerandreas.papirusoffice.data.DocxDocumentParser(context) }
    var docxImages by remember { mutableStateOf<Map<String, java.io.File>>(emptyMap()) }
    var docxExtents by remember { mutableStateOf<Map<String, Pair<Long, Long>>>(emptyMap()) }
    var isParsingDoc by remember { mutableStateOf(false) }

    val inkyMetadataRepo = remember(context) {
        val db = com.makerandreas.papirusoffice.data.cache.DocumentDatabase.getInstance(context)
        com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataRepository(db.inkyDocumentMetadataDao())
    }

    val updateInkyMetadata: suspend (String, String, String) -> Unit = { path, name, text ->
        val existing = inkyMetadataRepo.getMetadata(path)
        val cleanText = text.trim()
        val words = if (cleanText.isEmpty()) 0 else cleanText.split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }
        val chars = text.length
        val paragraphs = if (cleanText.isEmpty()) 0 else text.split("\n").count { it.isNotBlank() }
        val now = System.currentTimeMillis()
        val entity = com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataEntity(
            filePath = path,
            fileName = name,
            createdAt = existing?.createdAt ?: now,
            lastModifiedAt = now,
            author = existing?.author ?: "Papirus Office User",
            wordCount = words,
            characterCount = chars,
            paragraphCount = paragraphs,
            fileType = if (name.endsWith(".docx", ignoreCase = true)) "DOCX" else "ODT"
        )
        inkyMetadataRepo.saveOrUpdateMetadata(entity)
    }

    val updateActiveSession = { file: java.io.File, parsedDoc: com.makerandreas.papirusoffice.data.OfficeParsedDocument? ->
        if (parsedDoc != null) {
            val officeDoc = parsedDoc.toOfficeDocument()
            val session = com.makerandreas.papirusoffice.data.DocumentSession(
                engine = com.makerandreas.papirusoffice.data.DocumentEngine(),
                document = officeDoc,
                file = com.makerandreas.papirusoffice.data.OfficeFile(file)
            )
            com.makerandreas.papirusoffice.data.SessionManager.getInstance().setCurrentSession(session)
        }
    }

    val currentSessionState by com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.collectAsState()
    // Subscribed for its recomposition side effect: StateFlow conflates
    // same-instance sessions, so every dirty-flag transition bumps this
    // revision to refresh the `session.dirty` reads below.
    @Suppress("unused")
    val dirtyRevision by com.makerandreas.papirusoffice.data.SessionManager.getInstance().dirtyRevision.collectAsState()
    // Stable fallbacks: allocating a new StateFlow here on every recomposition
    // would restart the collectors each time the session is null.
    val undoFallbackFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val redoFallbackFlow = remember { kotlinx.coroutines.flow.MutableStateFlow(false) }
    val canUndo by (currentSessionState?.undoManager?.historyManager?.canUndo ?: undoFallbackFlow).collectAsState()
    val canRedo by (currentSessionState?.undoManager?.historyManager?.canRedo ?: redoFallbackFlow).collectAsState()
    var layoutCursor by remember { mutableStateOf(com.makerandreas.papirusoffice.data.DocumentCursor()) }

    val navEngine = remember {
        currentSessionState?.navigationEngine ?: com.makerandreas.papirusoffice.data.navigation.NavigationEngine()
    }
    // P2-2: Navigator follows app locale (English app -> English Navigator); wire DataStore toggle
    LaunchedEffect(viewOptions.navigatorFollowAppLocale) {
        val appTag = java.util.Locale.getDefault().language
        navEngine.setNavigatorLocalePolicy(viewOptions.navigatorFollowAppLocale, appTag)
    }

    var docBodyText by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue("")
        )
    }

    LaunchedEffect(isEditMode) {
        editorMode = if (isEditMode) com.example.modules.inky.state.EditorMode.EDIT else com.example.modules.inky.state.EditorMode.VIEW
        if (docBodyText.selection.length > 0) {
            docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(docBodyText.selection.end.coerceIn(0, docBodyText.text.length)))
        }
    }

    var lastTextRecordedValue by remember { mutableStateOf("") }
    val isUndoEnabled by remember(canUndo, docBodyText.text, lastTextRecordedValue) {
        derivedStateOf { canUndo || (docBodyText.text != lastTextRecordedValue) }
    }
    var initialLoadedText by remember { mutableStateOf("") }

    LaunchedEffect(docBodyText.text, docTitle, currentSessionState?.document) {
        // Debounced: re-indexing the whole document on every keystroke janks,
        // so wait until typing pauses. Layout-aware reindex happens after LayoutEngine is constructed.
        kotlinx.coroutines.delay(350)
        val activeDoc = currentSessionState?.document ?: com.makerandreas.papirusoffice.data.OfficeDocument(
            metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
        )
        navEngine.updateDocument(com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(activeDoc, docBodyText.text))
    }

    LaunchedEffect(currentSessionState?.document) {
        val sessionDoc = currentSessionState?.document
        if (sessionDoc != null) {
            val textFromDoc = sessionDoc.body.elements.mapNotNull {
                when (it) {
                    is com.makerandreas.papirusoffice.data.OfficeParagraph -> it.text
                    is com.makerandreas.papirusoffice.data.OfficeHeading -> it.text
                    is com.makerandreas.papirusoffice.data.OfficeListItem -> it.text
                    is com.makerandreas.papirusoffice.data.OfficeDocElement.ParagraphElement -> it.paragraph.text
                    else -> null
                }
            }.joinToString("\n\n")

            if (textFromDoc.isNotBlank() && textFromDoc != docBodyText.text) {
                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                    text = textFromDoc,
                    selection = androidx.compose.ui.text.TextRange(textFromDoc.length)
                )
                lastTextRecordedValue = textFromDoc
                initialLoadedText = textFromDoc
            }
        }
    }

    val navEngineState by navEngine.state.collectAsState()

    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var loadingDocName by remember { mutableStateOf(docTitle) }
    var loadingProgressStatus by remember { mutableStateOf("") }
    var showDocOpenFailedDialog by remember { mutableStateOf(false) }
    var docOpenFailedError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(com.example.MainActivity.openedFileNonce, com.example.MainActivity.openedFilePath) {
        val filePath = com.example.MainActivity.openedFilePath
        if (filePath != null && com.example.MainActivity.openedFileType == "Inky") {
            val f = java.io.File(filePath)
            if (f.exists()) {
                isNewDocument = false
                isSaved = true
                docTitle = f.name
                loadingDocName = f.name
                isParsingDoc = true
                isLoadingDocument = true
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val parseResult = docxParser.parseDocument(f)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        isLoadingDocument = false
                        isParsingDoc = false
                        if (parseResult.parsedDocument?.isParsingFailed == true) {
                            showDocOpenFailedDialog = true
                            docOpenFailedError = parseResult.parsedDocument.failureReason
                        } else {
                            val parsedDoc = parseResult.parsedDocument
                            if (parsedDoc?.pageCount != null && parsedDoc.pageCount > 0) {
                                detectedDocPageCount = parsedDoc.pageCount
                            }

                            // Session state restore on reopen / process recreation
                            val sessionRestore = com.makerandreas.papirusoffice.data.SafeSessionRestore(context)
                            val lastSession = sessionRestore.getLastSession()
                            if (lastSession != null && lastSession.uri == f.absolutePath) {
                                if (!lastSession.isSaved && !lastSession.draftText.isNullOrBlank()) {
                                    val safeCursor = lastSession.cursor.coerceIn(0, lastSession.draftText.length)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = lastSession.draftText,
                                        selection = androidx.compose.ui.text.TextRange(safeCursor)
                                    )
                                    isSaved = false
                                } else {
                                    val safeCursor = lastSession.cursor.coerceIn(0, parseResult.text.length)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = parseResult.text,
                                        selection = androidx.compose.ui.text.TextRange(safeCursor)
                                    )
                                }
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(100)
                                    scrollState.scrollTo(lastSession.scroll.coerceIn(0, scrollState.maxValue))
                                }
                            } else {
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                            }

                            lastTextRecordedValue = parseResult.text
                            initialLoadedText = parseResult.text
                            docxImages = parseResult.extractedImages
                            docxExtents = parseResult.imageExtents
                            updateInkyMetadata(f.absolutePath, f.name, parseResult.text)
                            RecentFilesTracker.addFile(context, f.absolutePath, "Inky")
                            updateActiveSession(f, parseResult.parsedDocument)
                        }
                    }
                }
            }
        } else if (com.example.MainActivity.openedFilePath == null) {
            // Load Normal.ott template automatically as the base for new Inky documents
            isNewDocument = true
            isSaved = true
            docTitle = "Document.odt"
            loadingDocName = "Document.odt"
            isLoadingDocument = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val templateFile = com.example.core.util.TemplateManager.getInkyNormalTemplateFile(context)
                val parseResult = if (templateFile != null && templateFile.exists()) {
                    docxParser.parseDocument(templateFile)
                } else {
                    com.makerandreas.papirusoffice.data.DocxParseResult("")
                }
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    isLoadingDocument = false
                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                    lastTextRecordedValue = parseResult.text
                    initialLoadedText = parseResult.text
                    docxImages = parseResult.extractedImages
                    docxExtents = parseResult.imageExtents

                    // Set active session for the default loaded template
                    val officeDoc = parseResult.parsedDocument?.toOfficeDocument() ?: com.makerandreas.papirusoffice.data.OfficeDocument(
                        metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = "Document.odt")
                    )
                    val dummyFile = java.io.File(context.filesDir, "Document.odt")
                    val session = com.makerandreas.papirusoffice.data.DocumentSession(
                        engine = com.makerandreas.papirusoffice.data.DocumentEngine(),
                        document = officeDoc,
                        file = com.makerandreas.papirusoffice.data.OfficeFile(dummyFile)
                    )
                    com.makerandreas.papirusoffice.data.SessionManager.getInstance().setCurrentSession(session)
                }
            }
        }
    }

    DisposableEffect(docxParser) {
        val observer = androidx.lifecycle.Observer<com.makerandreas.papirusoffice.data.ParsingProgress> { progress ->
            run {
                loadingProgressStatus = progress.statusMessage
                if (progress.isFailed) {
                    isLoadingDocument = false
                    showDocOpenFailedDialog = true
                    docOpenFailedError = progress.errorMessage
                }
            }
        }
        docxParser.parsingProgress.observeForever(observer)
        onDispose {
            docxParser.parsingProgress.removeObserver(observer)
        }
    }
    var activeToolbarType by remember { mutableStateOf("Standard") } // Default to Standard toolbar as requested
    var wasKeyboardOpenBeforeBottomSheet by remember { mutableStateOf(false) }

    val wordCount = remember(docBodyText.text) {
        val text = docBodyText.text.trim()
        if (text.isEmpty()) {
            0
        } else {
            text.split("\\s+".toRegex()).count { word ->
                word.any { it.isLetterOrDigit() }
            }
        }
    }

    val outlineEngine = remember { com.makerandreas.papirusoffice.data.OutlineEngineImpl() }
    val reminderManager = remember { com.makerandreas.papirusoffice.data.ReminderManager() }
    val documentPageSpec = remember(currentSessionState?.document) {
        currentSessionState?.document?.styles?.defaultPageStyle
            ?: com.makerandreas.papirusoffice.data.PageStyleSpec.FALLBACK
    }
    val layoutEngine = remember(documentPageSpec, viewOptions.hyphenationEnabled) {
        // Dictionary is only parsed while the user has opted in.
        val hyphenator = if (viewOptions.hyphenationEnabled) {
            try {
                com.makerandreas.papirusoffice.data.HyphenationEngine.loadDefault(context)
            } catch (e: Exception) {
                null
            }
        } else null
        com.makerandreas.papirusoffice.data.LayoutEngine(documentPageSpec, hyphenator = hyphenator)
    }

    val activeLayoutDocument = remember(currentSessionState?.document, docBodyText.text, docTitle) {
        val base = currentSessionState?.document ?: com.makerandreas.papirusoffice.data.OfficeDocument(
            metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
        )
        com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(base, docBodyText.text)
    }
    val documentLayout = remember(activeLayoutDocument) {
        layoutEngine.performLayout(activeLayoutDocument, outlineEngine = outlineEngine)
    }
    LaunchedEffect(activeLayoutDocument, documentLayout) {
        navEngine.updateDocument(activeLayoutDocument, documentLayout)
    }

    // Go to Page Dialog state
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var targetPageText by remember { mutableStateOf("") }

    // Set Reminder Dialog state
    var showSetReminderDialog by remember { mutableStateOf(false) }
    var reminderNoteText by remember { mutableStateOf("") }

    val totalDocPages = remember(documentLayout) {
        documentLayout.pages.size.coerceAtLeast(1)
    }

    val wordsBeforeCursor = remember(docBodyText.text, docBodyText.selection) {
        val selStart = docBodyText.selection.start.coerceIn(0, docBodyText.text.length)
        val textBefore = docBodyText.text.substring(0, selStart).trim()
        if (textBefore.isEmpty()) {
            0
        } else {
            textBefore.split("\\s+".toRegex()).count { word ->
                word.any { it.isLetterOrDigit() }
            }
        }
    }

    val currentDocPage by remember(totalDocPages, wordCount, isEditMode) {
        derivedStateOf {
            if (totalDocPages <= 1) {
                1
            } else if (scrollState.maxValue > 0) {
                val scrollRatio = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                val pageFromScroll = (scrollRatio * (totalDocPages - 1)).roundToInt() + 1
                pageFromScroll.coerceIn(1, totalDocPages)
            } else if (isEditMode && wordCount > 0) {
                val ratio = wordsBeforeCursor.toFloat() / wordCount.toFloat()
                val page = (ratio * totalDocPages).toInt() + 1
                page.coerceIn(1, totalDocPages)
            } else {
                1
            }
        }
    }

    val documentNavigator = remember(scrollState, totalDocPages, viewOptions) {
        object : com.makerandreas.papirusoffice.data.DocumentNavigator {
            override fun goToPage(page: Int) {
                coroutineScope.launch {
                    val targetPage = page.coerceIn(1, totalDocPages)
                    val ratio = if (totalDocPages > 1) (targetPage - 1).toFloat() / (totalDocPages - 1).toFloat() else 0f
                    val targetScroll = (ratio * scrollState.maxValue).toInt()
                    if (viewOptions.enableSmoothScrolling) {
                        scrollState.animateScrollTo(targetScroll)
                    } else {
                        scrollState.scrollTo(targetScroll)
                    }
                }
            }
            override fun currentPage(): Int = currentDocPage
            override fun pageCount(): Int = totalDocPages
        }
    }

    LaunchedEffect(navEngineState.navTargetSignal) {
        val signal = navEngineState.navTargetSignal
        if (signal != null) {
            val elements = activeLayoutDocument.body.elements
            val elemIdx = when {
                signal.targetElementIndex in elements.indices -> signal.targetElementIndex
                signal.targetParagraphIndex in elements.indices -> signal.targetParagraphIndex
                else -> -1
            }
            if (elemIdx >= 0) {
                layoutCursor = com.makerandreas.papirusoffice.data.DocumentCursor(
                    elementIndex = elemIdx,
                    paragraphIndex = elemIdx,
                    offset = 0
                )
            }
            val layoutPage = when {
                elemIdx >= 0 -> documentLayout.elementPageIndex[elemIdx]
                signal.targetPageIndex > 0 -> signal.targetPageIndex
                else -> null
            }
            if (layoutPage != null && layoutPage > 0) {
                documentNavigator.goToPage(layoutPage)
            } else if (signal.targetPageIndex > 0) {
                documentNavigator.goToPage(signal.targetPageIndex)
            }
            navEngine.clearNavSignal()
        }
    }

    // LibreOfficeKit diagnostics log. When no native LOKit build has loaded
    // (see LokitEngine.statusLabel), the engine runs simulated and entries
    // below are produced locally, not by native dispatch.
    val lokitLogs = remember {
        mutableStateListOf(
            "LOKit Core: " + com.example.core.jni.LokitEngine.statusLabel,
            com.example.core.jni.LokitEngine.tagLog("lok::Office::documentLoad(\"Untitled.odt\")"),
            com.example.core.jni.LokitEngine.tagLog("lok::Document::registerCallback(LOK_CALLBACK_INVALIDATE_TILES)")
        )
    }

    fun addLokitLog(message: String) {
        if (lokitLogs.size > 15) {
            lokitLogs.removeAt(0)
        }
        // Event names mirror LOKit dispatch; the tag keeps the simulated
        // engine honest until a native build is bundled.
        lokitLogs.add(com.example.core.jni.LokitEngine.tagLog(message))
    }

    var activeToolbarTypeState by remember { mutableStateOf("Standard") } // For compatibility or internal tracking

    val activity = remember(context) { context.findActivity() }

    DisposableEffect(activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
            ) {
                val currentPath = com.example.MainActivity.openedFilePath
                val sessionRestore = com.makerandreas.papirusoffice.data.SafeSessionRestore(context)
                if (currentPath != null && !isLoadingDocument) {
                    sessionRestore.saveLastSession(
                        com.makerandreas.papirusoffice.data.LastSessionInfo(
                            uri = currentPath,
                            cursor = docBodyText.selection.start,
                            zoom = zoomScale,
                            scroll = scrollState.value,
                            module = com.makerandreas.papirusoffice.data.ModuleType.WRITER,
                            docTitle = docTitle,
                            draftText = if (!isSaved) docBodyText.text else null,
                            isSaved = isSaved
                        )
                    )
                }
                sessionRestore.flush()
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.core.jni.LibreOfficeCore.registerCallback(1, object : com.example.core.jni.LibreOfficeCore.DocumentCallback {
            override fun onEvent(type: Int, payload: String) {
                android.util.Log.i("InkyModule", "LibreOfficeKit Callback: type=$type payload=$payload")
                addLokitLog("LOK_CALLBACK_EVENT(type=$type, payload=$payload)")
            }
        })
    }

    // Text formatting state
    var activeFontFamily by remember { mutableStateOf("Liberation Serif") }
    var activeFontSize by remember { mutableStateOf(12) }

    // Status bar "section or object information" (WG Ch.1 Table 1). Filled by
    // the caret-to-element pass below, which is the one place that already maps
    // the caret onto a body element; a second mapping here would be a second
    // source of truth. Only facts the model can prove are shown: a heading's
    // level and text (resolved by the Navigator index, which is also the only
    // resolver that follows style parents, so Sample-5's paragraph-styled
    // headings count), the element kind for tables and list items, and the
    // hyphen placeholder elsewhere. Table row/column, section names and image
    // geometry are not guessed: they arrive with plans 19/21.
    var statusBarObjectInfo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(docBodyText.selection, activeLayoutDocument, navEngineState.index) {
        val elements = activeLayoutDocument.body.elements
        if (elements.isEmpty()) {
            statusBarObjectInfo = null
            return@LaunchedEffect
        }
        val caret = docBodyText.selection.start.coerceIn(0, docBodyText.text.length)
        val windows = com.makerandreas.papirusoffice.data.DocumentTextWindows.compute(elements, docBodyText.text)
        val hit = com.makerandreas.papirusoffice.data.DocumentTextWindows.elementForOffset(windows, caret)
        val caretElementIndex = hit?.elementIndex ?: layoutCursor.elementIndex
        statusBarObjectInfo = resolveStatusBarObjectInfo(
            context = context,
            caretElementIndex = caretElementIndex,
            elements = elements,
            headings = com.makerandreas.papirusoffice.data.navigation.flattenHeadings(navEngineState.index.headings)
        )
        val element = hit?.let { elements.getOrNull(it.elementIndex) }
            ?: elements.getOrNull(layoutCursor.elementIndex)
        val paragraph = when (element) {
            is com.makerandreas.papirusoffice.data.OfficeParagraph -> element
            is com.makerandreas.papirusoffice.data.OfficeHeading -> com.makerandreas.papirusoffice.data.OfficeParagraph(
                text = element.text,
                styleName = element.styleName ?: "Heading ${element.level}",
                runs = element.runs
            )
            is com.makerandreas.papirusoffice.data.OfficeListItem -> com.makerandreas.papirusoffice.data.OfficeParagraph(
                text = element.text,
                runs = element.runs
            )
            is com.makerandreas.papirusoffice.data.OfficeDocElement.ParagraphElement -> element.paragraph
            else -> null
        } ?: return@LaunchedEffect
        val styles = activeLayoutDocument.styles
        val base = com.makerandreas.papirusoffice.data.StyleResolver.resolveParagraphStyle(paragraph.styleName, styles)
        val localOffset = if (hit != null) {
            (caret - hit.start).coerceIn(0, paragraph.text.length)
        } else {
            layoutCursor.offset.coerceIn(0, paragraph.text.length)
        }
        var pos = 0
        var run: com.makerandreas.papirusoffice.data.OfficeTextRun? = null
        for (candidate in paragraph.runs) {
            val end = pos + candidate.text.length
            if (localOffset < end || (localOffset == paragraph.text.length && end == paragraph.text.length)) {
                run = candidate
                break
            }
            pos = end
        }
        val resolved = if (run != null) {
            com.makerandreas.papirusoffice.data.OfficeRuns.mergeRun(run, base, styles)
        } else {
            base
        }
        resolved.fontFamily?.takeIf { it.isNotBlank() }?.let { activeFontFamily = it }
        activeFontSize = kotlin.math.round(resolved.fontSizeSp).toInt().coerceIn(1, 1638)
    }
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var textAlignment by remember { mutableStateOf(TextAlign.Left) }
    var fontColor by remember { mutableStateOf(Color.Black) }
    var highlightColor by remember { mutableStateOf(Color.Transparent) }
    var underlineColor by remember { mutableStateOf(Color.Black) }
    var paragraphShadingColor by remember { mutableStateOf(Color.Transparent) }

    // Text formatting engine advanced state (SwTxtFrm, SwParaPortion, SwScriptInfo)
    var lineSpacingFactor by remember { mutableStateOf(1.0f) }
    var dropCapEnabled by remember { mutableStateOf(false) }
    var dropCapLines by remember { mutableStateOf(3) }
    var asianGridEnabled by remember { mutableStateOf(false) }
    var hangingPunctuation by remember { mutableStateOf(true) }
    var showTextFormattingInspector by remember { mutableStateOf(false) }

    // Dialog & overlay triggers
    var showFindReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showEquationDialog by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    var isLoadingAi by remember { mutableStateOf(false) }

    // Bottom Bar (Ribbon & sub-decks) States
    var bottomBarDeck by remember { mutableStateOf("ribbon") } // ribbon, font_color, font_size, font_family, highlight_color
    var showRibbonTabMenu by remember { mutableStateOf(false) }
    var activeInkySubpage by remember { mutableStateOf("") }
    var previousInkySubpage by remember { mutableStateOf("") }
    var selectedStyleNameForOptions by remember { mutableStateOf("Normal") }
    var openedFromExternalHub by remember { mutableStateOf(false) }

    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showReloadConfirmationDialog by remember { mutableStateOf(false) }
    var pendingActionAfterSave by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showCreateFromTemplateDialog by remember { mutableStateOf(false) }
    var showOpenDocumentDialog by remember { mutableStateOf(false) }

    val triggerReload = {
        isLoadingDocument = true
        isParsingDoc = true
        coroutineScope.launch {
            var reloadedText = initialLoadedText
            val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
            if (currentSession != null) {
                val result = com.makerandreas.papirusoffice.data.framework.DocumentLifecycleManager.reload(context, currentSession)
                if (result != null) {
                    reloadedText = result.text
                }
            } else {
                // Fallback if no active session
                val path = com.example.MainActivity.openedFilePath
                if (path != null) {
                    try {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            val cacheRepo = com.makerandreas.papirusoffice.data.cache.DocumentCacheRepository(context)
                            cacheRepo.invalidateCache(file)
                            val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                            val parseResult = parser.parseDocument(file, bypassCache = true)
                            reloadedText = parseResult.text
                        }
                    } catch (e: Exception) {}
                }
            }
            docBodyText = androidx.compose.ui.text.input.TextFieldValue(reloadedText)
            lastTextRecordedValue = reloadedText
            initialLoadedText = reloadedText
            isSaved = true
            Toast.makeText(context, context.getString(R.string.toast_reload_success), Toast.LENGTH_SHORT).show()
            isLoadingDocument = false
            isParsingDoc = false
        }
        Unit
    }

    // Save & Loading states
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var showSaveFailedDialog by remember { mutableStateOf(false) }
    var showSavingProgressPopup by remember { mutableStateOf(false) }
    var savingProgressDocName by remember { mutableStateOf(docTitle) }

    val performSave = { simulateError: Boolean ->
        if (isNewDocument || com.example.MainActivity.openedFilePath == null) {
            showSaveAsDialog = true
        } else {
            coroutineScope.launch {
                isSaving = true
                saveFailed = false
                delay(1000)
                if (simulateError) {
                    isSaving = false
                    saveFailed = true
                    showSaveFailedDialog = true
                } else {
                    val path = com.example.MainActivity.openedFilePath
                    var actualSuccess = true
                    if (path != null) {
                        try {
                            val file = java.io.File(path)
                            val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                            
                            val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
                            val documentToSave = if (currentSession != null) {
                                val isTextDirty = (currentSession.dirty) || (docBodyText.text != initialLoadedText)
                                val updatedDoc = com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                    currentSession.document, docBodyText.text
                                ).copy(isModified = isTextDirty)
                                currentSession.document = updatedDoc
                                updatedDoc
                            } else {
                                com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                    com.makerandreas.papirusoffice.data.OfficeDocument(
                                        metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
                                    ),
                                    docBodyText.text
                                ).copy(isModified = true)
                            }
                            
                            val serializer = com.makerandreas.papirusoffice.data.DocumentSerializer(context)
                            actualSuccess = serializer.serializeToFormat(documentToSave, if (file.name.endsWith(".docx", ignoreCase = true)) "DOCX" else "ODT", file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            actualSuccess = false
                        }
                    }
                    
                    isSaving = false
                    if (actualSuccess) {
                        isSaved = true
                        saveFailed = false
                        initialLoadedText = docBodyText.text
                        if (path != null) {
                            RecentFilesTracker.addFile(context, path, "Inky")
                        }
                        Toast.makeText(context, R.string.toast_document_saved, Toast.LENGTH_SHORT).show()
                    } else {
                        saveFailed = true
                        showSaveFailedDialog = true
                    }
                }
            }
        }
    }

    val performSaveWithPopup = { docName: String, simulateError: Boolean, onSuccess: (() -> Unit)? ->
        if (isNewDocument || com.example.MainActivity.openedFilePath == null) {
            showSaveAsDialog = true
        } else {
            coroutineScope.launch {
                showSavingProgressPopup = true
                savingProgressDocName = docName
                isSaving = true
                saveFailed = false
                delay(1200)
                showSavingProgressPopup = false
                
                if (simulateError) {
                    isSaving = false
                    saveFailed = true
                    showSaveFailedDialog = true
                } else {
                    val path = com.example.MainActivity.openedFilePath
                    var actualSuccess = true
                    if (path != null) {
                        try {
                            val file = java.io.File(path)
                            val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                            
                            val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
                            val documentToSave = if (currentSession != null) {
                                val isTextDirty = (currentSession.dirty) || (docBodyText.text != initialLoadedText)
                                val updatedDoc = com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                    currentSession.document, docBodyText.text
                                ).copy(isModified = isTextDirty)
                                currentSession.document = updatedDoc
                                updatedDoc
                            } else {
                                com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                    com.makerandreas.papirusoffice.data.OfficeDocument(
                                        metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
                                    ),
                                    docBodyText.text
                                ).copy(isModified = true)
                            }
                            
                            val serializer = com.makerandreas.papirusoffice.data.DocumentSerializer(context)
                            actualSuccess = serializer.serializeToFormat(documentToSave, if (file.name.endsWith(".docx", ignoreCase = true)) "DOCX" else "ODT", file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            actualSuccess = false
                        }
                    }
                    
                    isSaving = false
                    if (actualSuccess) {
                        isSaved = true
                        saveFailed = false
                        initialLoadedText = docBodyText.text
                        if (path != null) {
                            updateInkyMetadata(path, docTitle, docBodyText.text)
                            RecentFilesTracker.addFile(context, path, "Inky")
                        }
                        Toast.makeText(context, R.string.toast_document_saved, Toast.LENGTH_SHORT).show()
                        onSuccess?.invoke()
                    } else {
                        saveFailed = true
                        showSaveFailedDialog = true
                    }
                }
            }
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                var actualSuccess = true
                val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
                val documentToSave = if (currentSession != null) {
                    val isTextDirty = (currentSession.dirty) || (docBodyText.text != initialLoadedText)
                    val updatedDoc = com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                        currentSession.document, docBodyText.text
                    ).copy(isModified = isTextDirty)
                    currentSession.document = updatedDoc
                    updatedDoc
                } else {
                    com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                        com.makerandreas.papirusoffice.data.OfficeDocument(
                            metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
                        ),
                        docBodyText.text
                        ).copy(isModified = true)
                }
                
                try {
                    val isDocx = currentSaveDefaultFilename.endsWith(".docx", ignoreCase = true)
                    val extension = if (isDocx) ".docx" else ".odt"
                    val tempFile = java.io.File(context.cacheDir, "temp_uri_save$extension")
                    if (tempFile.exists()) tempFile.delete()
                    
                    val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                    val serializer = com.makerandreas.papirusoffice.data.DocumentSerializer(context)
                    val success = serializer.serializeToFormat(documentToSave, if (tempFile.name.endsWith(".docx", ignoreCase = true)) "DOCX" else "ODT", tempFile)
                    if (success && tempFile.exists()) {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            tempFile.inputStream().copyTo(outputStream)
                        }
                        tempFile.delete()
                    } else {
                        context.contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(docBodyText.text.toByteArray())
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    actualSuccess = false
                }
                
                if (actualSuccess) {
                    val prefs = context.getSharedPreferences("papirus_options", android.content.Context.MODE_PRIVATE)
                    if (prefs.getBoolean("always_create_backup_copy", false)) {
                        try {
                            val backupDir = context.getExternalFilesDir("backups") ?: java.io.File(context.filesDir, "backups")
                            if (!backupDir.exists()) backupDir.mkdirs()
                            val backupFile = java.io.File(backupDir, "${currentSaveDefaultFilename}.bak")
                            backupFile.writeText(docBodyText.text)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    var savedName = currentSaveDefaultFilename
                    try {
                        val cursor = context.contentResolver.query(it, null, null, null, null)
                        cursor?.use { c ->
                            if (c.moveToFirst()) {
                                val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex >= 0) {
                                    val queried = c.getString(nameIndex)
                                    if (!queried.isNullOrBlank()) {
                                        savedName = queried
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    // Clean up double extensions if SAF appended one
                    savedName = savedName.replace(".docx.odt", ".docx").replace(".odt.docx", ".odt")

                    docTitle = savedName
                    isSaved = true
                    isNewDocument = false
                    initialLoadedText = docBodyText.text
                    
                    val localSavedFile = com.makerandreas.papirusoffice.data.OpenedDocumentStore.allocateFile(context, savedName)
                    try {
                        val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                        
                        // Synchronize UI text into OfficeDocument elements before saving
                        val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
                        val documentToSave = if (currentSession != null) {
                            val isTextDirty = (currentSession.dirty) || (docBodyText.text != initialLoadedText)
                            val updatedDoc = com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                currentSession.document, docBodyText.text
                            ).copy(isModified = isTextDirty)
                            currentSession.document = updatedDoc
                            updatedDoc
                        } else {
                            com.makerandreas.papirusoffice.data.DocumentTextMerger.mergeEditedText(
                                com.makerandreas.papirusoffice.data.OfficeDocument(
                                    metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
                                ),
                                docBodyText.text
                                ).copy(isModified = true)
                        }
                        
                        val serializer = com.makerandreas.papirusoffice.data.DocumentSerializer(context)
                        serializer.serializeToFormat(documentToSave, if (localSavedFile.name.endsWith(".docx", ignoreCase = true)) "DOCX" else "ODT", localSavedFile)
                        com.example.MainActivity.openedFilePath = localSavedFile.absolutePath
                        com.example.MainActivity.openedFileType = "Inky"
                        RecentFilesTracker.addFile(context, localSavedFile.absolutePath, "Inky")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    updateInkyMetadata(it.toString(), savedName, docBodyText.text)
                    Toast.makeText(context, context.getString(R.string.doc_saved_success, savedName), Toast.LENGTH_SHORT).show()
                    pendingActionAfterSave?.invoke()
                    pendingActionAfterSave = null
                } else {
                    Toast.makeText(context, R.string.toast_error_saving_document_to_uri, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                showSavingProgressPopup = true
                val cleanName = if (docTitle.endsWith(".pdf", ignoreCase = true)) docTitle else "${docTitle.substringBeforeLast(".")}.pdf"
                savingProgressDocName = cleanName
                delay(1000)
                var actualSuccess = false
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        actualSuccess = InkyPdfExporter.exportToPdf(
                            context = context,
                            docTitle = docTitle.substringBeforeLast("."),
                            bodyText = docBodyText.text,
                            outputStream = outputStream
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                showSavingProgressPopup = false
                if (actualSuccess) {
                    Toast.makeText(context, R.string.toast_document_exported_as_pdf_successfully, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, R.string.toast_failed_to_export_pdf, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val handleSaveCommand: () -> Unit = {
        if (isNewDocument || com.example.MainActivity.openedFilePath == null) {
            showSaveAsDialog = true
        } else {
            if (!isEditMode) {
                performSaveWithPopup(docTitle, false) {
                    pendingActionAfterSave?.invoke()
                    pendingActionAfterSave = null
                }
            } else {
                performSave(false)
            }
        }
    }

    val runDocumentLoading = { isCreating: Boolean, name: String, onFinished: () -> Unit ->
        coroutineScope.launch {
            isLoadingDocument = true
            isCreatingDoc = isCreating
            loadingDocName = name
            loadingProgressStatus = context.getString(R.string.loading_status_odf)
            delay(500)
            loadingProgressStatus = context.getString(R.string.loading_status_rendering)
            delay(500)
            loadingProgressStatus = context.getString(R.string.loading_status_preparing)
            delay(400)
            isLoadingDocument = false
            onFinished()
        }
    }

    val handleOpenDocument = {
        val openAction = {
            showOpenDocumentDialog = true
        }
        if (!isSaved) {
            pendingActionAfterSave = openAction
            showUnsavedChangesDialog = true
        } else {
            openAction()
        }
    }

    val handleLoadTemplate = { template: TemplateManager.TemplateItem ->
        val loadTemplate = {
            val name = "Document.odt"
            val sampleTemplateContent = "RESUME (MODERN)\n\nJohn Doe • Professional Software Engineer\nEmail: john.doe@email.com • Tel: +1 555-0199\n\nSUMMARY\nHighly motivated developer with experience building native Android productivity engines.\n\nEXPERIENCE\nSenior Developer • Papirus Office Inc.\n- Designed and implemented Google Gemini ODF template recommendation search APIs.\n- Optimized document parsing pipelines to improve large-file load times.\n\nEDUCATION\nBachelor of Science in Computer Science • University of Antigravity"
            
            val filePath = com.example.MainActivity.openedFilePath
            val file = if (filePath != null) java.io.File(filePath) else null
            if (file != null && file.exists()) {
                isLoadingDocument = true
                coroutineScope.launch {
                    val parseResult = docxParser.parseDocument(file)
                    runDocumentLoading(true, name) {
                        docTitle = name
                        docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                        lastTextRecordedValue = parseResult.text
                        docxImages = parseResult.extractedImages
                        docxExtents = parseResult.imageExtents
                        isSaved = true
                        isEditMode = true
                        isNewDocument = true
                        showBottomBar = false
                        showCreateFromTemplateDialog = false

                        // Set active session!
                        val officeDoc = parseResult.parsedDocument?.toOfficeDocument() ?: com.makerandreas.papirusoffice.data.OfficeDocument(
                            metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = name)
                        )
                        val session = com.makerandreas.papirusoffice.data.DocumentSession(
                            engine = com.makerandreas.papirusoffice.data.DocumentEngine(),
                            document = officeDoc,
                            file = com.makerandreas.papirusoffice.data.OfficeFile(file)
                        )
                        com.makerandreas.papirusoffice.data.SessionManager.getInstance().setCurrentSession(session)
                    }
                }
            } else {
                runDocumentLoading(true, name) {
                    docTitle = name
                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(sampleTemplateContent)
                    lastTextRecordedValue = sampleTemplateContent
                    isSaved = true
                    isEditMode = true
                    isNewDocument = true
                    showBottomBar = false
                    showCreateFromTemplateDialog = false

                    // Set active session!
                    val officeDoc = com.makerandreas.papirusoffice.data.OfficeDocument(
                        metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = name)
                    )
                    val dummyFile = java.io.File(context.filesDir, name)
                    val session = com.makerandreas.papirusoffice.data.DocumentSession(
                        engine = com.makerandreas.papirusoffice.data.DocumentEngine(),
                        document = officeDoc,
                        file = com.makerandreas.papirusoffice.data.OfficeFile(dummyFile)
                    )
                    com.makerandreas.papirusoffice.data.SessionManager.getInstance().setCurrentSession(session)
                }
            }
            Unit
        }
        if (!isSaved) {
            pendingActionAfterSave = loadTemplate
            showUnsavedChangesDialog = true
        } else {
            loadTemplate()
        }
    }

    val handleNewDocument = {
        val createNew = {
            val name = "Document.odt"
            com.example.MainActivity.pendingNewDocument = true
            com.example.MainActivity.openedFilePath = null
            com.example.MainActivity.openedFileType = "Inky"
            com.makerandreas.papirusoffice.data.SafeSessionRestore(context).clearLastSession()
            com.example.core.jni.LibreOfficeCore.createDocument(name)
            runDocumentLoading(true, name) {
                docTitle = name
                coroutineScope.launch {
                    val templateFile = com.example.core.util.TemplateManager.getInkyNormalTemplateFile(context)
                    val parseResult = if (templateFile != null && templateFile.exists()) {
                        docxParser.parseDocument(templateFile)
                    } else {
                        com.makerandreas.papirusoffice.data.DocxParseResult("")
                    }
                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                    lastTextRecordedValue = parseResult.text
                    initialLoadedText = parseResult.text
                    docxImages = parseResult.extractedImages
                    docxExtents = parseResult.imageExtents

                    // Set active session!
                    val officeDoc = parseResult.parsedDocument?.toOfficeDocument() ?: com.makerandreas.papirusoffice.data.OfficeDocument(
                        metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = name)
                    )
                    val dummyFile = java.io.File(context.filesDir, name)
                    val session = com.makerandreas.papirusoffice.data.DocumentSession(
                        engine = com.makerandreas.papirusoffice.data.DocumentEngine(),
                        document = officeDoc,
                        file = com.makerandreas.papirusoffice.data.OfficeFile(dummyFile)
                    )
                    com.makerandreas.papirusoffice.data.SessionManager.getInstance().setCurrentSession(session)
                }
                isSaved = true
                isEditMode = true
                isNewDocument = true
                activeFontFamily = "Aptos Display"
                activeFontSize = 12
                isBold = false
                isItalic = false
                isUnderline = false
                showBottomBar = false
            }
            Unit
        }
        if (!isSaved) {
            pendingActionAfterSave = createNew
            showUnsavedChangesDialog = true
        } else {
            createNew()
        }
    }

    val handleClose = {
        val closeAction = {
            val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
            if (currentSession != null) {
                com.makerandreas.papirusoffice.data.framework.DocumentLifecycleManager.close(currentSession)
            }
            com.makerandreas.papirusoffice.data.SafeSessionRestore(context).clearLastSession()
            com.example.MainActivity.openedFilePath = null
            onFormatAction("Back to start center")
        }
        val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
        val isDirty = !isSaved || (currentSession?.dirty == true)
        if (isDirty) {
            pendingActionAfterSave = closeAction
            showUnsavedChangesDialog = true
        } else {
            closeAction()
        }
    }

    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            wasKeyboardOpenBeforeBottomSheet = isKeyboardVisible
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
        } else {
            // Closing a sheet used to leave the editor with no focused field, so
            // neither the keyboard button nor a tap could raise the IME again.
            // Restore what was there before the sheet opened.
            if (wasKeyboardOpenBeforeBottomSheet) {
                rendererFocusBridge.requestFirstEditable()
                keyboardController?.show()
            }
            wasKeyboardOpenBeforeBottomSheet = false
            // Reset subpage states when closing
            activeInkySubpage = ""
            openedFromExternalHub = false
            bottomBarDeck = "ribbon"
        }
    }

    LaunchedEffect(bottomBarDeck, showBottomBar) {
        if (showBottomBar) {
            when (bottomBarDeck) {
                "font_color" -> {
                    activeInkySubpage = "font_color"
                    openedFromExternalHub = true
                }
                "highlight_color" -> {
                    activeInkySubpage = "highlight_color"
                    openedFromExternalHub = true
                }
                "font_family" -> {
                    activeInkySubpage = "font_style"
                    openedFromExternalHub = true
                }
                "bulleted_list" -> {
                    activeInkySubpage = "bulleted_list"
                    openedFromExternalHub = true
                }
                "numbered_list" -> {
                    activeInkySubpage = "numbered_list"
                    openedFromExternalHub = true
                }
                "multilevel_list" -> {
                    activeInkySubpage = "multilevel_list"
                    openedFromExternalHub = true
                }
                "underline_options" -> {
                    activeInkySubpage = "underline_options"
                    openedFromExternalHub = true
                }
            }
        }
    }

    // FCT state & scroll
    val customTextToolbar = remember { com.example.ui.components.PapirusTextToolbar() }
    val horizScrollState = rememberScrollState()

    // Scroll Control to hide AppBar and Toolbar Hub dynamically
    var previousScrollValue by remember { mutableStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }

    
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            isControlsVisible = true
            if (showBottomBar) {
                showBottomBar = false
            } else {
                // Restore scroll position to prevent autoscroll-up
                scrollState.scrollTo(previousScrollBeforeKeyboard)
            }
        } else {
            isControlsVisible = true
            previousScrollBeforeKeyboard = scrollState.value
        }
    }

    LaunchedEffect(customTextToolbar.status) {
        if (customTextToolbar.status == androidx.compose.ui.platform.TextToolbarStatus.Shown && showBottomBar) {
            showBottomBar = false
        }
    }

    val inkyEditingEngine = remember { com.makerandreas.papirusoffice.data.writer.EditingEngine() }
    val inkyClipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val enterEditMode = {
        isEditMode = true
        editorMode = com.example.modules.inky.state.EditorMode.EDIT
        com.makerandreas.papirusoffice.data.PapirusLogger.d("BACK", "mode=EDIT (entered)")
    }

    val exitEditMode = {
        isEditMode = false
        editorMode = com.example.modules.inky.state.EditorMode.VIEW
        focusManager.clearFocus()
        customTextToolbar.hide()
        keyboardController?.hide()
        if (!docBodyText.selection.collapsed) {
            docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(0))
        }
        com.makerandreas.papirusoffice.data.PapirusLogger.d("BACK", "mode=VIEW (exited)")
    }

    val handleBack: () -> Unit = {
        com.makerandreas.papirusoffice.data.PapirusLogger.d("BACK", "handleBack: mode=$editorMode, fct=${customTextToolbar.status}, bottomBar=$showBottomBar, keyboard=$isKeyboardVisible")
        when {
            customTextToolbar.status == androidx.compose.ui.platform.TextToolbarStatus.Shown -> {
                customTextToolbar.hide()
                if (isKeyboardVisible) {
                    keyboardController?.hide()
                }
            }
            showBottomBar -> {
                if (activeInkySubpage.isNotEmpty()) {
                    if (openedFromExternalHub) {
                        showBottomBar = false
                        openedFromExternalHub = false
                        bottomBarDeck = "ribbon"
                    } else {
                        // sequential back
                        when (activeInkySubpage) {
                            "underline_color" -> activeInkySubpage = "underline_options"
                            "create_new_style", "style_options" -> activeInkySubpage = "paragraph_styles"
                            else -> activeInkySubpage = ""
                        }
                    }
                } else {
                    showBottomBar = false
                }
            }
            isKeyboardVisible -> {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
            isEditMode || editorMode == com.example.modules.inky.state.EditorMode.EDIT -> {
                exitEditMode()
            }
            else -> {
                handleClose()
            }
        }
    }

    // Autosave helper
    fun triggerAutosave() {
        isSaved = false
        val currentPath = com.example.MainActivity.openedFilePath
        if (currentPath != null) {
            val sessionRestore = com.makerandreas.papirusoffice.data.SafeSessionRestore(context)
            sessionRestore.saveLastSession(
                com.makerandreas.papirusoffice.data.LastSessionInfo(
                    uri = currentPath,
                    cursor = docBodyText.selection.start,
                    zoom = zoomScale,
                    scroll = scrollState.value,
                    module = com.makerandreas.papirusoffice.data.ModuleType.WRITER,
                    docTitle = docTitle,
                    draftText = docBodyText.text,
                    isSaved = false
                )
            )
        }
    }

    LaunchedEffect(scrollState.value, docBodyText.selection) {
        val currentPath = com.example.MainActivity.openedFilePath
        if (currentPath != null && !isLoadingDocument) {
            val sessionRestore = com.makerandreas.papirusoffice.data.SafeSessionRestore(context)
            sessionRestore.saveLastSession(
                com.makerandreas.papirusoffice.data.LastSessionInfo(
                    uri = currentPath,
                    cursor = docBodyText.selection.start,
                    zoom = zoomScale,
                    scroll = scrollState.value,
                    module = com.makerandreas.papirusoffice.data.ModuleType.WRITER,
                    docTitle = docTitle,
                    draftText = if (!isSaved) docBodyText.text else null,
                    isSaved = isSaved
                )
            )
        }
    }

    val typingBuffer = remember {
        com.example.modules.inky.state.PendingTypingBuffer(
            scope = coroutineScope,
            getLiveText = { docBodyText.text },
            getBaseline = { lastTextRecordedValue },
            setBaseline = { lastTextRecordedValue = it },
            getSelection = { docBodyText.selection },
            recordCommittedTyping = { oldValue, newValue, selectionAtCommit ->
                val diff = newValue.length - oldValue.length
                val title = when {
                    diff > 0 -> {
                        val added = if (newValue.startsWith(oldValue)) {
                            newValue.substring(oldValue.length)
                        } else {
                            newValue
                        }
                        "Typing \"${added.take(15)}${if (added.length > 15) "..." else ""}\""
                    }
                    diff < 0 -> "Delete text"
                    else -> "Edit Document"
                }
                val icon = if (diff >= 0) "text_fields" else "backspace"
                val cmdType = if (diff >= 0) "TYPING" else "DELETE_TEXT"
                val session = currentSessionState
                if (session != null) {
                    session.undoManager.recordAction(object : com.makerandreas.papirusoffice.data.undo.UndoAction {
                        override val title = title
                        override val timestamp = System.currentTimeMillis()
                        override val icon = icon
                        override val commandType = cmdType
                        override suspend fun undo() {
                            docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                text = oldValue,
                                selection = androidx.compose.ui.text.TextRange(oldValue.length)
                            )
                            lastTextRecordedValue = oldValue
                        }
                        override suspend fun redo() {
                            docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                text = newValue,
                                selection = selectionAtCommit
                            )
                            lastTextRecordedValue = newValue
                        }
                    })
                }
            }
        )
    }

    val flushPendingTyping: suspend (String) -> Unit = { textToCommit ->
        if (typingBuffer.flush(textToCommit)) {
            triggerAutosave()
        }
    }

    val performUndo: () -> Unit = {
        customTextToolbar.hide()
        coroutineScope.launch {
            flushPendingTyping(docBodyText.text)
            val success = currentSessionState?.undoManager?.undo() ?: false
            if (success) {
                triggerAutosave()
                Toast.makeText(context, R.string.toast_undo_performed, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_nothing_to_undo, Toast.LENGTH_SHORT).show()
            }
        }
        addLokitLog("lok::Document::postWindow(event=UNDO)")
    }

    val performRedo: () -> Unit = {
        customTextToolbar.hide()
        coroutineScope.launch {
            flushPendingTyping(docBodyText.text)
            val success = currentSessionState?.undoManager?.redo() ?: false
            if (success) {
                triggerAutosave()
                Toast.makeText(context, R.string.toast_redo_performed, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_nothing_to_redo, Toast.LENGTH_SHORT).show()
            }
        }
        addLokitLog("lok::Document::postWindow(event=REDO)")
    }

    val performUndoTo: (com.makerandreas.papirusoffice.data.undo.HistoryEntry) -> Unit = { entry ->
        customTextToolbar.hide()
        coroutineScope.launch {
            flushPendingTyping(docBodyText.text)
            currentSessionState?.undoManager?.undoTo(entry)
            triggerAutosave()
            Toast.makeText(context, R.string.toast_actions_undone, Toast.LENGTH_SHORT).show()
        }
    }

    val performRedoTo: (com.makerandreas.papirusoffice.data.undo.HistoryEntry) -> Unit = { entry ->
        customTextToolbar.hide()
        coroutineScope.launch {
            flushPendingTyping(docBodyText.text)
            currentSessionState?.undoManager?.redoTo(entry)
            triggerAutosave()
            Toast.makeText(context, R.string.toast_actions_redone, Toast.LENGTH_SHORT).show()
        }
    }

    val handleTextValueChange: (androidx.compose.ui.text.input.TextFieldValue) -> Unit = { newValue ->
        val prevValue = docBodyText
        val prevText = prevValue.text
        val prevSel = prevValue.selection
        val newText = newValue.text
        val newSel = newValue.selection

        if (newText == prevText) {
            // Selection or cursor change only
            docBodyText = newValue
        } else {
            isSaved = false
            val hadSelection = !prevSel.collapsed

            if (hadSelection) {
                typingBuffer.cancelPending()

                val start = kotlin.math.min(prevSel.start, prevSel.end)
                val end = kotlin.math.max(prevSel.start, prevSel.end)
                val selRange = com.makerandreas.papirusoffice.data.writer.SelectionRange(start, end)
                val fullText = prevText
                val isDelete = newText.length < prevText.length

                coroutineScope.launch {
                    val session = currentSessionState
                    // Flush-then-delete is owned by the typing buffer, so the
                    // delete always records against a committed baseline.

                    // Record selection delete/replace
                    if (isDelete) {
                        typingBuffer.deleteSelection(
                            selection = selRange,
                            fullText = fullText,
                            engine = inkyEditingEngine,
                            onApply = { appliedText, s ->
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = appliedText,
                                    selection = androidx.compose.ui.text.TextRange(s.min, s.max)
                                )
                                lastTextRecordedValue = appliedText
                                isSaved = false
                            }
                        )
                    } else if (session != null) {
                        session.undoManager.recordAction(object : com.makerandreas.papirusoffice.data.undo.UndoAction {
                            override val title = "Replace text"
                            override val timestamp = System.currentTimeMillis()
                            override val icon = "edit"
                            override val commandType = "REPLACE_SELECTION"
                            override suspend fun undo() {
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = fullText,
                                    selection = prevSel
                                )
                                lastTextRecordedValue = fullText
                            }
                            override suspend fun redo() {
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = newText,
                                    selection = newSel
                                )
                                lastTextRecordedValue = newText
                            }
                        })
                        lastTextRecordedValue = newText
                    }
                }

                if (!isDelete) {
                    docBodyText = newValue
                }
                triggerAutosave()
            } else {
                // Collapsed cursor: typing, enter, backspace
                docBodyText = newValue
                triggerAutosave()

                val isEnter = (newText.length == prevText.length + 1) &&
                        (prevSel.min < newText.length && newText[prevSel.min] == '\n')

                if (isEnter) {
                    typingBuffer.cancelPending()
                    val priorOld = lastTextRecordedValue
                    val priorNew = newText
                    coroutineScope.launch {
                        val session = currentSessionState
                        if (session != null && priorNew != priorOld) {
                            session.undoManager.recordAction(object : com.makerandreas.papirusoffice.data.undo.UndoAction {
                                override val title = "New Paragraph"
                                override val timestamp = System.currentTimeMillis()
                                override val icon = "keyboard_return"
                                override val commandType = "SPLIT_PARAGRAPH"
                                override suspend fun undo() {
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = priorOld,
                                        selection = prevSel
                                    )
                                    lastTextRecordedValue = priorOld
                                }
                                override suspend fun redo() {
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = priorNew,
                                        selection = newSel
                                    )
                                    lastTextRecordedValue = priorNew
                                }
                            })
                        }
                        lastTextRecordedValue = priorNew
                    }
                } else {
                    // Continuous typing / backspacing: debounce for 800ms
                    typingBuffer.onTextChanged()
                }
            }
        }
    }

    val handleFctDelete: () -> Unit = {
        val selection = docBodyText.selection
        if (!selection.collapsed) {
            val start = kotlin.math.min(selection.start, selection.end)
            val end = kotlin.math.max(selection.start, selection.end)
            val selRange = com.makerandreas.papirusoffice.data.writer.SelectionRange(start, end)
            val fullText = docBodyText.text

            coroutineScope.launch {
                com.makerandreas.papirusoffice.data.PapirusLogger.d("UNDO", "DeleteSelectionCommand")
                typingBuffer.deleteSelection(
                    selection = selRange,
                    fullText = fullText,
                    engine = inkyEditingEngine,
                    onApply = { appliedText, newSel ->
                        docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                            text = appliedText,
                            selection = androidx.compose.ui.text.TextRange(newSel.min, newSel.max)
                        )
                        lastTextRecordedValue = appliedText
                        isSaved = false
                    }
                )
            }
        }
    }

    val handleFctCut: () -> Unit = {
        val selection = docBodyText.selection
        if (!selection.collapsed) {
            val start = kotlin.math.min(selection.start, selection.end)
            val end = kotlin.math.max(selection.start, selection.end)
            val selectedStr = docBodyText.text.substring(start, end)
            inkyClipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedStr))
            com.makerandreas.papirusoffice.data.PapirusLogger.d("UNDO", "CutSelectionCommand")
            handleFctDelete()
        }
    }

    val handleFctCopy: () -> Unit = {
        val selection = docBodyText.selection
        if (!selection.collapsed) {
            val start = kotlin.math.min(selection.start, selection.end)
            val end = kotlin.math.max(selection.start, selection.end)
            val selectedStr = docBodyText.text.substring(start, end)
            inkyClipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedStr))
            com.makerandreas.papirusoffice.data.PapirusLogger.d("CLIPBOARD", "Copied text")
        }
    }

    val handleFctSelectAll: () -> Unit = {
        docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(0, docBodyText.text.length))
    }

    BackHandler {
        handleBack()
    }

    LaunchedEffect(isEditMode) {
        editorMode = if (isEditMode) com.example.modules.inky.state.EditorMode.EDIT else com.example.modules.inky.state.EditorMode.VIEW
        customTextToolbar.hide()
        focusManager.clearFocus()
        // Automatically deselect any selected text or objects when switching modes
        if (!docBodyText.selection.collapsed) {
            docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(0))
        }
    }

    LaunchedEffect(scrollState.value) {
        if (!isKeyboardVisible) {
            previousScrollBeforeKeyboard = scrollState.value
        }
        if (isKeyboardVisible) {
            isControlsVisible = true
            previousScrollValue = scrollState.value
            return@LaunchedEffect
        }
        val delta = scrollState.value - previousScrollValue
        if (delta > 8 && isControlsVisible && scrollState.isScrollInProgress) {
            isControlsVisible = false
        } else if (delta < -8 && !isControlsVisible && scrollState.isScrollInProgress) {
            isControlsVisible = true
        }
        previousScrollValue = scrollState.value
    }

    LaunchedEffect(currentSessionState) {
        lastTextRecordedValue = docBodyText.text
    }

    fun Float.safeCoerceIn(min: Float, max: Float): Float {
        return if (min >= max) min else this.coerceIn(min, max)
    }

    fun calculateFctOffset(
        targetX: Float,
        targetY: Float,
        zoomScale: Float,
        density: Float,
        screenWidthDp: Int,
        screenHeightDp: Int
    ): androidx.compose.ui.unit.IntOffset {
        val fctWidthPx = 220 * density
        val fctHeightPx = 54 * density

        // Target coordinates are in root screen space.
        val x = targetX - fctWidthPx / 2f
        val cursorHeight = activeFontSize * zoomScale * density
        val yAbove = targetY - fctHeightPx - 8 * density
        val yBelow = targetY + cursorHeight + 8 * density

        // Place above by default. If too close to the top app bar, place below.
        val topSafeArea = 80 * density
        val y = if (yAbove < topSafeArea) yBelow else yAbove

        // Bound FCT to screen visible area
        val screenWidthPx = screenWidthDp * density
        val screenHeightPx = screenHeightDp * density
        val coercedX = x.coerceIn(8 * density, screenWidthPx - fctWidthPx - 8 * density)
        val coercedY = y.coerceIn(topSafeArea, screenHeightPx - fctHeightPx - 8 * density)

        return androidx.compose.ui.unit.IntOffset(coercedX.toInt(), coercedY.toInt())
    }

    // Layout configuration variables
    val docBgColor = if (isDarkDocument) Color(0xFF181A1B) else Color(0xFFD0D5DD)
    val pageBgColor = if (isDarkDocument) Color(0xFF242627) else Color.White
    val textPrimaryColor = if (isDarkDocument) Color(0xFFE8E6E3) else fontColor
    val textAccentColor = if (isDarkDocument) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val borderStrokeColor = if (isDarkDocument) Color(0xFF3C3F41) else Color(0xFFE2E8F0)

    var fctOffset by remember { mutableStateOf(androidx.compose.ui.unit.IntOffset(16, 16)) }

    var showFontMenuInToolbar by remember { mutableStateOf(false) }
    var showSizeMenuInToolbar by remember { mutableStateOf(false) }
    var showToolbarPagesMenu by remember { mutableStateOf(false) }

    var textToolbarCopyCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var textToolbarPasteCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var textToolbarCutCallback by remember { mutableStateOf<(() -> Unit)?>(null) }
    var textToolbarSelectAllCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Immediate composition-based checks to completely eliminate flickering during zoom or scrolling
    var lastZoomScale by remember { mutableStateOf(zoomScale) }
    if (lastZoomScale != zoomScale) {
        customTextToolbar.hide()
        lastZoomScale = zoomScale
    }
    if (scrollState.isScrollInProgress || horizScrollState.isScrollInProgress) {
        customTextToolbar.hide()
    }

    LaunchedEffect(zoomScale) {
        customTextToolbar.hide()
    }

    var previousZoomScale by remember { mutableStateOf(zoomScale) }
    LaunchedEffect(zoomScale) {
        val oldScale = previousZoomScale
        val newScale = zoomScale
        if (oldScale != newScale) {
            val ratio = newScale / oldScale
            val halfScreenWidthPx = (screenWidthDp * density) / 2f
            val halfScreenHeightPx = (screenHeightDp * density) / 2f
            
            val currentH = horizScrollState.value
            val currentV = scrollState.value
            
            val targetH = ((currentH + halfScreenWidthPx) * ratio - halfScreenWidthPx).toInt()
            val targetV = ((currentV + halfScreenHeightPx) * ratio - halfScreenHeightPx).toInt()
            
            horizScrollState.scrollTo(targetH.coerceAtLeast(0))
            scrollState.scrollTo(targetV.coerceAtLeast(0))
            
            previousZoomScale = newScale
        }
    }

    @OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
    val noOpBringIntoViewResponder = remember {
        object : BringIntoViewResponder {
            override fun calculateRectForParent(localRect: androidx.compose.ui.geometry.Rect): androidx.compose.ui.geometry.Rect {
                return localRect
            }

            override suspend fun bringChildIntoView(localRect: () -> androidx.compose.ui.geometry.Rect?) {
                // Intentionally empty: prevents default BasicTextField bringIntoView from
                // scrolling the parent scrollState and pushing paper top padding off-screen.
            }
        }
    }

    LaunchedEffect(docBodyText.selection, docBodyText.text, isKeyboardVisible) {
        delay(if (isKeyboardVisible) 150L else 50L)
        val cursorOffset = docBodyText.selection.start
        if (cursorOffset >= 0 && cursorOffset <= docBodyText.text.length) {
            val layoutResult = bodyTextLayoutResult
            val localCursorRect = if (layoutResult != null && cursorOffset <= layoutResult.layoutInput.text.length) {
                try {
                    layoutResult.getCursorRect(cursorOffset)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            val textFieldCoords = bodyTextFieldCoordinates
            val viewportCoords = viewportCoordinates
            if (localCursorRect != null && textFieldCoords != null && viewportCoords != null && textFieldCoords.isAttached && viewportCoords.isAttached) {
                val cursorTopLeftInViewport = viewportCoords.localPositionOf(textFieldCoords, localCursorRect.topLeft)
                val cursorBottomRightInViewport = viewportCoords.localPositionOf(textFieldCoords, localCursorRect.bottomRight)
                
                val viewportWidth = viewportCoords.size.width
                val viewportHeight = viewportCoords.size.height
                
                val hPaddingPx = 32 * density
                val topPaddingPx = 16 * density
                val bottomPaddingPx = 16 * density
                
                val cursorLeft = cursorTopLeftInViewport.x
                val cursorRight = cursorBottomRightInViewport.x
                if (cursorLeft < hPaddingPx) {
                    val delta = (cursorLeft - hPaddingPx).toInt()
                    horizScrollState.scrollTo((horizScrollState.value + delta).coerceAtLeast(0))
                } else if (cursorRight > viewportWidth - hPaddingPx) {
                    val delta = (cursorRight - (viewportWidth - hPaddingPx)).toInt()
                    horizScrollState.scrollTo(horizScrollState.value + delta)
                }
                
                val cursorTop = cursorTopLeftInViewport.y
                val cursorBottom = cursorBottomRightInViewport.y
                
                // Only adjust vertical scroll if cursor is actually obscured / outside visible bounds
                val isCursorVisibleVertically = cursorTop >= topPaddingPx && cursorBottom <= viewportHeight - bottomPaddingPx
                if (!isCursorVisibleVertically) {
                    if (cursorTop < topPaddingPx) {
                        val delta = (cursorTop - topPaddingPx).toInt()
                        scrollState.scrollTo((scrollState.value + delta).coerceAtLeast(0))
                    } else if (cursorBottom > viewportHeight - bottomPaddingPx) {
                        val delta = (cursorBottom - (viewportHeight - bottomPaddingPx)).toInt()
                        scrollState.scrollTo((scrollState.value + delta).coerceAtLeast(0))
                    }
                }
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides customTextToolbar
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(docBgColor)
                .onPreviewKeyEvent { event ->
                    if (event.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                        if (event.key == androidx.compose.ui.input.key.Key.F5) {
                            showBottomBar = true
                            bottomBarDeck = "navigator"
                            true
                        } else if (event.isCtrlPressed) {
                            if (event.isShiftPressed && event.key == androidx.compose.ui.input.key.Key.N) {
                                showCreateFromTemplateDialog = true
                                true
                            } else if (event.key == androidx.compose.ui.input.key.Key.N) {
                                handleNewDocument()
                                true
                            } else if (event.key == androidx.compose.ui.input.key.Key.G) {
                                targetPageText = currentDocPage.toString()
                                showGoToPageDialog = true
                                true
                            } else if (event.key == androidx.compose.ui.input.key.Key.O) {
                                handleOpenDocument()
                                true
                            } else if (event.key == androidx.compose.ui.input.key.Key.Z) {
                                if (event.isShiftPressed) {
                                    performRedo()
                                } else {
                                    performUndo()
                                }
                                true
                            } else if (event.key == androidx.compose.ui.input.key.Key.Y) {
                                performRedo()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }
        ) {
        Column(modifier = Modifier.fillMaxSize().then(if (!showBottomBar) Modifier.imePadding() else Modifier)) {
                 // --- HEADER TOP APP BAR ---
            AnimatedVisibility(
                visible = !showBottomBar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (showFindReplace) {
                    com.example.ui.components.FindAndReplaceBar(
                        isTablet = false,
                        onFind = { query ->
                            searchQuery = query
                            val text = docBodyText.text
                            if (query.isNotEmpty() && text.contains(query, ignoreCase = true)) {
                                val index = text.indexOf(query, ignoreCase = true)
                                docBodyText = docBodyText.copy(
                                    selection = androidx.compose.ui.text.TextRange(index, index + query.length)
                                )
                                Toast.makeText(context, context.getString(R.string.toast_found_match_at_character_index, index), Toast.LENGTH_SHORT).show()
                            } else if (query.isNotEmpty()) {
                                Toast.makeText(context, R.string.toast_no_match_found, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onReplace = { find, replace ->
                            val text = docBodyText.text
                            if (find.isNotEmpty() && text.contains(find, ignoreCase = true)) {
                                val updatedText = text.replace(find, replace, ignoreCase = true)
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = updatedText,
                                    selection = androidx.compose.ui.text.TextRange(0)
                                )
                                isSaved = false
                                Toast.makeText(context, R.string.toast_replaced_successfully, Toast.LENGTH_SHORT).show()
                            } else if (find.isNotEmpty()) {
                                Toast.makeText(context, R.string.toast_nothing_to_replace, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onClose = {
                            showFindReplace = false
                        }
                    )
                } else {
                    TopAppBar(
                        title = {
                            if (!isEditMode) {
                                Text(
                                    text = docTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { handleBack() },
                                modifier = Modifier.testTag("btn_top_app_bar_back")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = if (isEditMode) "Exit Edit Mode" else "Start Center")
                            }
                        },
                        actions = {
                            // 1. Upload to Google Drive
                            IconButton(onClick = {
                                Toast.makeText(context, R.string.toast_uploading_to_google_drive, Toast.LENGTH_SHORT).show()
                                addLokitLog("Upload to Drive triggered")
                            }) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = stringResource(R.string.cd_upload_to_drive))
                            }

                            // 2. Find in Page
                            IconButton(onClick = {
                                showFindReplace = !showFindReplace
                            }) {
                                Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.cd_find_in_page))
                            }

                            if (isEditMode) {
                                // 3. Mobile view (Edit Mode only)
                                IconButton(onClick = {
                                    isWebView = !isWebView
                                    Toast.makeText(context, if (isWebView) R.string.toast_mobile_view_active else R.string.toast_normal_view_active, Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                        contentDescription = stringResource(R.string.cd_document_view_mode)
                                    )
                                }

                                // 4. Undo (Edit Mode only) - Click for Undo, Long press for Actions to Undo Bottom Sheet (unified affordance like Redo)
                                LongClickIconButton(
                                    onClick = performUndo,
                                    onLongClick = {
                                        customTextToolbar.hide()
                                        coroutineScope.launch { flushPendingTyping(docBodyText.text) }
                                        previousInkySubpage = activeInkySubpage
                                        activeInkySubpage = "actions_to_undo"
                                        showBottomBar = true
                                    },
                                    enabled = isUndoEnabled,
                                    modifier = Modifier.testTag("btn_top_app_bar_undo")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.cd_undo_long_press_for_history))
                                }
                            }

                            // 5. More Options (both modes, different menu items)
                            Box {
                                var showMoreMenuInAppBar by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMoreMenuInAppBar = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                                }
                                DropdownMenu(
                                    expanded = showMoreMenuInAppBar,
                                    onDismissRequest = { showMoreMenuInAppBar = false }
                                ) {
                                    if (!isEditMode) {
                                        // Viewer Mode Items: Share as PDF, Save as, Switch to Dark Mode, Open navigation bar, Read aloud, Print
                                        DropdownMenuItem(
                                            text = { Text("Share as PDF") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                Toast.makeText(context, R.string.toast_exporting_and_sharing_as_pdf, Toast.LENGTH_SHORT).show()
                                                coroutineScope.launch {
                                                    try {
                                                        val cleanName = docTitle.substringBeforeLast(".").replace(" ", "_")
                                                        val shareFile = java.io.File(context.cacheDir, "$cleanName.pdf")
                                                        if (shareFile.exists()) shareFile.delete()
                                                        
                                                        val outputStream = java.io.FileOutputStream(shareFile)
                                                        val success = InkyPdfExporter.exportToPdf(
                                                            context = context,
                                                            docTitle = docTitle.substringBeforeLast("."),
                                                            bodyText = docBodyText.text,
                                                            outputStream = outputStream
                                                        )
                                                        outputStream.close()
                                                        
                                                        if (success && shareFile.exists()) {
                                                            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.fileprovider",
                                                                shareFile
                                                            )
                                                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                                type = "application/pdf"
                                                                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                                                                putExtra(android.content.Intent.EXTRA_SUBJECT, "Share PDF: $docTitle")
                                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            val chooserIntent = android.content.Intent.createChooser(shareIntent, "Share Document via").apply {
                                                                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                            }
                                                            context.startActivity(chooserIntent)
                                                        } else {
                                                            Toast.makeText(context, R.string.toast_failed_to_generate_pdf_for_sharing, Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        Toast.makeText(context, context.getString(R.string.toast_error_sharing_pdf_e_message, e.message), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = stringResource(R.string.cd_share_as_pdf)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Save as...") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showSaveAsDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = stringResource(R.string.cd_save_as)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                isDarkDocument = !isDarkDocument
                                            },
                                            leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = stringResource(R.string.cd_toggle_theme)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open navigation bar") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showBottomBar = true
                                                bottomBarDeck = "navigator"
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.cd_open_navigation_bar)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Read aloud") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                Toast.makeText(context, R.string.toast_reading_document_aloud, Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.cd_read_aloud)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalPrintSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = stringResource(R.string.cd_print)) }
                                        )
                                    } else {
                                        // Edit Mode Items: Share, Switch to Dark Mode, Read aloud, Open Navigation Bar, Print
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalEmailSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.cd_share)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                isDarkDocument = !isDarkDocument
                                            },
                                            leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = stringResource(R.string.cd_toggle_theme)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Read aloud") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                Toast.makeText(context, R.string.toast_reading_document_aloud, Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = stringResource(R.string.cd_read_aloud)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open Navigation Bar") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showBottomBar = true
                                                bottomBarDeck = "navigator"
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = stringResource(R.string.cd_open_navigation_bar_2)) }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalPrintSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = stringResource(R.string.cd_print)) }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // --- Status Bar Atas (ONLY visible in Edit Mode) ---
            if (isEditMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = docTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isSaved) "Saved" else "Modified",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Page box width at 100 % zoom: the viewport minus the gutter on each
            // side, so the sheet reads as paper rather than as a bleeding card.
            // The geometry comes from the renderer, not from a second guess here.
            val pageFitWidthDp = (viewportWidthDp - PageStackMetrics.GUTTER_DP * 2f).coerceAtLeast(0f)

            // --- MAIN DOCUMENT WORKSPACE CANVAS (viewportCoordinates wired for elongated Viewer status bar) ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(docBgColor)
                    .onGloballyPositioned { coordinates ->
                        viewportCoordinates = coordinates
                        val widthDp = coordinates.size.width / density
                        if (widthDp != viewportWidthDp) viewportWidthDp = widthDp
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .horizontalScroll(horizScrollState)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isEditMode) {
                        // Viewer shares the Editor's global model read-only:
                        // selection lands in docBodyText so the FCT acts on it.
                        LayoutDrivenDocumentRenderer(
                            document = activeLayoutDocument,
                            zoomScale = zoomScale,
                            isEditMode = false,
                            cursor = layoutCursor,
                            onCursorChange = { layoutCursor = it },
                            outlineEngine = outlineEngine,
                            enableOutlineFolding = true,
                            showImages = true,
                            showTables = true,
                            layoutResult = documentLayout,
                            extractedImages = docxImages,
                            pageSpec = documentPageSpec,
                            textColor = textPrimaryColor,
                            editorValue = docBodyText,
                            onViewerSelectionChange = { docBodyText = docBodyText.copy(selection = it) },
                            onViewerToolbarRequest = { rect ->
                                if (rect != null) customTextToolbar.show(rect) else customTextToolbar.hide()
                            },
                            viewportWidthDp = pageFitWidthDp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Edit Mode renders the same layout-driven page stack as
                        // Viewer Mode, so both modes share one pagination source.
                        if (!isWebView) {
                            LayoutDrivenDocumentRenderer(
                                document = activeLayoutDocument,
                                zoomScale = zoomScale,
                                isEditMode = true,
                                cursor = layoutCursor,
                                onCursorChange = { layoutCursor = it },
                                outlineEngine = outlineEngine,
                                enableOutlineFolding = true,
                                showImages = true,
                                showTables = true,
                                layoutResult = documentLayout,
                                extractedImages = docxImages,
                                pageSpec = documentPageSpec,
                                textColor = textPrimaryColor,
                                editorValue = docBodyText,
                                onEditorValueChange = handleTextValueChange,
                                viewportWidthDp = pageFitWidthDp,
                                focusBridge = rendererFocusBridge,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            // Web View keeps one flowing sheet under the global edit value
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .defaultMinSize(minHeight = (480 * zoomScale).dp)
                                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(4.dp))
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(4.dp)),
                                color = pageBgColor,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding((20 * zoomScale).dp)
                                ) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = docBodyText,
                                        onValueChange = handleTextValueChange,
                                        enabled = true,
                                        readOnly = false,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = textPrimaryColor,
                                            fontSize = (activeFontSize * zoomScale).sp,
                                            fontFamily = when (activeFontFamily.lowercase()) {
                                                "serif", "times new roman" -> FontFamily.Serif
                                                "sans-serif", "roboto", "arial" -> FontFamily.SansSerif
                                                "monospace", "courier" -> FontFamily.Monospace
                                                else -> FontFamily.Default
                                            },
                                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = buildList {
                                                if (isUnderline) add(androidx.compose.ui.text.style.TextDecoration.Underline)
                                                if (isStrikethrough) add(androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                            }.fold(androidx.compose.ui.text.style.TextDecoration.None) { acc, dec -> acc + dec },
                                            textAlign = textAlignment
                                        ),
                                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = (440 * zoomScale).dp)
                                            .focusRequester(focusRequester)
                                            .onGloballyPositioned { bodyTextFieldCoordinates = it }
                                            .testTag("doc_body_editor")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Status Bar Bawah (bottom): elongated unified (P1-1), Viewer + Editor share one tonal bar ---
            // Elongated = full-width tonal bar pinned between canvas and toolbar hub, visible in both Viewer & Editor.
            // Viewer: Page x–x of y (range) + word/char; Editor: Page x of y + word/char + zoom.
            // Range is viewport-aware: continuous scroll shows current + next page when > ~0.85 page heights visible.
            run {
                val charCount = docBodyText.text.length
                val viewerPageEnd = remember(
                    isEditMode, currentDocPage, totalDocPages, viewportCoordinates,
                    documentPageSpec, viewportWidthDp, zoomScale, density
                ) {
                    derivedStateOf {
                        if (!isEditMode && totalDocPages > 1 && currentDocPage < totalDocPages) {
                            // Height of one sheet as actually drawn: the same
                            // PageTransform the renderer uses, so this range can
                            // never describe paper nobody sees.
                            val pageHeightDp = PageTransform
                                .sheetFor(documentPageSpec, if (viewportWidthDp > 0f) pageFitWidthDp else 0f, zoomScale)
                                .heightDp
                            val viewportH = viewportCoordinates?.size?.height?.toFloat()?.div(density)
                            val looksContinuous = viewportH == null || viewportH > pageHeightDp * 0.85f
                            if (looksContinuous) (currentDocPage + 1).coerceAtMost(totalDocPages) else currentDocPage
                        } else currentDocPage
                    }
                }.value
                val pageText = if (!isEditMode && viewerPageEnd > currentDocPage) {
                    stringResource(R.string.viewer_status_page_range, currentDocPage, viewerPageEnd, totalDocPages)
                } else {
                    stringResource(R.string.viewer_status_page_single, currentDocPage, totalDocPages)
                }
                val wordsCharsText = stringResource(R.string.viewer_status_words_chars, wordCount, charCount)
                // P1-1 status bar, rebuilt in Plan 2: the counter is centred on
                // the screen itself (Box overlay, not a SpaceBetween row), so it
                // stays centred whatever the page label or the trailing action
                // measures. Every control here is a 48 dp touch target.
                var showZoomMenu by remember { mutableStateOf(false) }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp)
                    ) {
                        // 1. Page counter (leading): opens Go to Page. Geometry
                        // untouched: the WG object-information field shares the
                        // centre slot below instead of widening this one, so the
                        // 320 dp bar keeps the three-slot layout plan 2 signed off.
                        Row(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .height(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    targetPageText = currentDocPage.toString()
                                    showGoToPageDialog = true
                                }
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Description,
                                contentDescription = stringResource(R.string.inky_status_pages),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = pageText,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        // 2. Centre slot: words and characters, or the WG
                        // "section or object information" while the caret sits
                        // in a structural element. The two share one slot for
                        // the 320 dp reason above, and the swap has the guide's
                        // own precedent: the selection count "will temporarily
                        // replace the document total count" in the same field.
                        // The count is deferred, never lost: the info only
                        // appears while the caret is inside a heading, table or
                        // list item.
                        Text(
                            text = statusBarObjectInfo ?: wordsCharsText,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .widthIn(max = 200.dp)
                                .padding(horizontal = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // 3. Trailing action: zoom (Editor) or Edit (Viewer).
                        if (isEditMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showZoomMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.inky_status_zoom_percent, (zoomScale * 100).roundToInt()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = showZoomMenu,
                                    onDismissRequest = { showZoomMenu = false }
                                ) {
                                    listOf(50, 100, 150, 200, 300).forEach { percent ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(stringResource(R.string.inky_status_zoom_percent, percent))
                                            },
                                            onClick = {
                                                zoomScale = percent / 100f
                                                showZoomMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            // Viewer: the page-stack Edit affordance lives here now
                            // (it used to be a FAB floating over this bar), so the
                            // bar is never covered and nothing overlaps a tap target.
                            IconButton(
                                onClick = {
                                    if (!docBodyText.selection.collapsed) {
                                        docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(0))
                                    }
                                    customTextToolbar.hide()
                                    isEditMode = true
                                    showBottomBar = false
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(48.dp)
                                    .testTag("fab_open_edit_mode")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.inky_status_open_edit_mode),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // --- BOTTOM TOOLBAR HUB (Quick Action Bar, ONLY visible in Edit Mode) ---
            AnimatedVisibility(
                visible = isEditMode && !showBottomBar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, borderStrokeColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- Main scrollable area ---
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Font style (Dropdown style with text & chevron)
                            Row(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, borderStrokeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        showBottomBar = true
                                        activeInkySubpage = "font_style"
                                        openedFromExternalHub = true
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_select_font_style),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 2. Font size (Dropdown style with number & chevron)
                            Row(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, borderStrokeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        showFontSizeDialog = true
                                    }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activeFontSize.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.cd_select_font_size),
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 3. Bold
                            IconButton(
                                onClick = {
                                    isBold = !isBold
                                    triggerAutosave()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isBold) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Rounded.FormatBold, contentDescription = stringResource(R.string.cd_bold))
                            }

                            // 4. Italic
                            IconButton(
                                onClick = {
                                    isItalic = !isItalic
                                    triggerAutosave()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isItalic) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Rounded.FormatItalic, contentDescription = stringResource(R.string.cd_italic))
                            }

                            // 5. Underline with Tap and Hold
                            LongClickIconButton(
                                onClick = {
                                    isUnderline = !isUnderline
                                    triggerAutosave()
                                },
                                onLongClick = {
                                    showBottomBar = true
                                    activeInkySubpage = "underline_options"
                                    openedFromExternalHub = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FormatUnderlined,
                                    contentDescription = stringResource(R.string.cd_underline),
                                    tint = if (isUnderline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 6. Strikethrough
                            IconButton(
                                onClick = {
                                    isStrikethrough = !isStrikethrough
                                    triggerAutosave()
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = if (isStrikethrough) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            ) {
                                Icon(Icons.Rounded.FormatStrikethrough, contentDescription = stringResource(R.string.cd_strikethrough))
                            }

                            // 7. Highlight color
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "highlight_color"
                                openedFromExternalHub = true
                            }) {
                                Icon(
                                    Icons.Rounded.BorderColor,
                                    contentDescription = stringResource(R.string.cd_highlight_color),
                                    tint = if (highlightColor != Color.Transparent) highlightColor else MaterialTheme.colorScheme.primary
                                )
                            }

                            // 8. Font color
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "font_color"
                                openedFromExternalHub = true
                            }) {
                                Icon(
                                    Icons.Rounded.FormatColorText,
                                    contentDescription = stringResource(R.string.cd_font_color),
                                    tint = fontColor
                                )
                            }

                            // 9. Create bulleted list
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "bulleted_list"
                                openedFromExternalHub = true
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.FormatListBulleted, contentDescription = stringResource(R.string.cd_bulleted_list))
                            }

                            // 10. Create numbered list
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "numbered_list"
                                openedFromExternalHub = true
                            }) {
                                Icon(Icons.Rounded.FormatListNumbered, contentDescription = stringResource(R.string.cd_numbered_list))
                            }

                            // 11. Increase indent
                            IconButton(onClick = {
                                val currentText = docBodyText.text
                                val selection = docBodyText.selection
                                val start = selection.start
                                val end = selection.end
                                val newText = currentText.substring(0, start) + "    " + currentText.substring(end)
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(start + 4)
                                )
                                triggerAutosave()
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.FormatIndentIncrease, contentDescription = stringResource(R.string.cd_increase_indent))
                            }

                            // 12. Decrease indent
                            IconButton(onClick = {
                                val currentText = docBodyText.text
                                val selection = docBodyText.selection
                                val start = selection.start
                                val end = selection.end
                                if (start >= 4 && currentText.substring(start - 4, start) == "    ") {
                                    val newText = currentText.substring(0, start - 4) + currentText.substring(end)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(start - 4)
                                    )
                                    triggerAutosave()
                                } else {
                                    Toast.makeText(context, R.string.toast_cannot_decrease_indent_further, Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.FormatIndentDecrease, contentDescription = stringResource(R.string.cd_decrease_indent))
                            }

                            // 13. Add image. Disabled look (38 % tint) + the reason
                            // in the contentDescription, and a press that names the
                            // plan holding the real insertion path. TODO(plan-6).
                            IconButton(onClick = {
                                Toast.makeText(context, R.string.toast_add_image_unavailable, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.cd_add_image),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // 14. Add table. TODO(plan-7): real insertion path.
                            IconButton(onClick = {
                                Toast.makeText(context, R.string.toast_add_table_unavailable, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Rounded.GridOn,
                                    contentDescription = stringResource(R.string.cd_add_table),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // 15. Add link. TODO(plan-7): real hyperlink path once
                            // the ODF/DOCX link text survives parsing (G-3/H-4).
                            IconButton(onClick = {
                                Toast.makeText(context, R.string.toast_add_link_unavailable, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.Rounded.Link,
                                    contentDescription = stringResource(R.string.cd_add_link),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }

                            // 16. Add comment. TODO(plan-8): real comment model;
                            // none exists, so the tool can only say so.
                            IconButton(onClick = {
                                Toast.makeText(context, R.string.toast_add_comment_unavailable, Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Comment,
                                    contentDescription = stringResource(R.string.cd_add_comment),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        }

                        // --- Vertical Divider ---
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(borderStrokeColor.copy(alpha = 0.5f))
                        )

                        // --- Persistent Trailing Actions ---
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // a. Insert Tab
                            IconButton(
                                onClick = {
                                    val currentText = docBodyText.text
                                    val selection = docBodyText.selection
                                    val start = selection.start
                                    val end = selection.end
                                    val newText = currentText.substring(0, start) + "\t" + currentText.substring(end)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(start + 1)
                                    )
                                    triggerAutosave()
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.KeyboardTab, contentDescription = stringResource(R.string.cd_insert_tab), tint = MaterialTheme.colorScheme.primary)
                            }

                            // b. Toggle Keyboard: focuses a real page field first
                            // (through the renderer bridge) and only then asks for
                            // the IME; showing the keyboard without a focused editor
                            // is what made this button look dead after a sheet.
                            IconButton(
                                onClick = {
                                    if (isKeyboardVisible) {
                                        keyboardController?.hide()
                                    } else {
                                        // Web View owns a single flow field instead
                                        // of the page stack, so it keeps its own
                                        // requester; the bridge handles the rest.
                                        if (!rendererFocusBridge.requestFirstEditable() && isWebView) {
                                            focusRequester.requestFocus()
                                        }
                                        keyboardController?.show()
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Keyboard, contentDescription = stringResource(R.string.inky_hub_toggle_keyboard), tint = MaterialTheme.colorScheme.primary)
                            }

                            // c. Open Standard Bottom Sheet
                            IconButton(
                                onClick = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    showBottomBar = true
                                    bottomBarDeck = "ribbon"
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Rounded.ViewAgenda, contentDescription = stringResource(R.string.cd_open_bottom_sheet), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }

        // --- PERSISTENT STANDARD BOTTOM SHEET (Material 3 Expressive) ---
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = showBottomBar,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.40f), // occupies exactly 40% of the screen height
                    tonalElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    border = BorderStroke(1.dp, borderStrokeColor),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (bottomBarDeck == "navigator") {
                            com.example.ui.components.NavigatorSheetContent(
                                navEngine = navEngine,
                                isEditMode = isEditMode,
                                onOpenNavigateBy = { bottomBarDeck = "navigate_by" },
                                onUndo = performUndo,
                                onRedo = performRedo,
                                onClose = { showBottomBar = false },
                                canUndo = isUndoEnabled,
                                canRedo = canRedo
                            )
                        } else if (bottomBarDeck == "navigate_by") {
                            com.example.ui.components.NavigateBySheetContent(
                                navEngine = navEngine,
                                isEditMode = isEditMode,
                                onBackToNavigator = { bottomBarDeck = "navigator" },
                                onUndo = performUndo,
                                onRedo = performRedo,
                                onClose = { showBottomBar = false },
                                canUndo = isUndoEnabled,
                                canRedo = canRedo
                            )
                        } else {
                        // The strip and the pager both read WriterRibbonModel, so
                        // they cannot disagree about which tab owns which deck
                        // (plan 3B 3.6). The pager pages are the tabs that have
                        // content, in strip order; the strip shows every tab.
                        val ribbonTabs = WriterRibbonTab.entries
                        val ribbonDecks = WriterRibbonTab.withDecks
                        val ribbonPagerState = androidx.compose.foundation.pager.rememberPagerState(
                            initialPage = WriterRibbonTab.pageOf(WriterRibbonTab.HOME).coerceAtLeast(0),
                            pageCount = { ribbonDecks.size }
                        )
                        val ribbonTabScrollState = rememberScrollState()

                        LaunchedEffect(ribbonPagerState.currentPage) {
                            ribbonTabScrollState.animateScrollTo((ribbonPagerState.currentPage * 75).dp.value.toInt())
                        }
                        if (activeInkySubpage.isNotEmpty()) {
                            // Subpage Header Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Leading Back Button
                                    if (!openedFromExternalHub) {
                                        IconButton(onClick = {
                                            when (activeInkySubpage) {
                                                "underline_color" -> activeInkySubpage = "underline_options"
                                                "create_new_style", "style_options" -> activeInkySubpage = "paragraph_styles"
                                                "actions_to_undo", "actions_to_redo" -> {
                                                    activeInkySubpage = previousInkySubpage
                                                    previousInkySubpage = ""
                                                    if (activeInkySubpage.isEmpty()) {
                                                        bottomBarDeck = "ribbon"
                                                    }
                                                }
                                                else -> {
                                                    activeInkySubpage = ""
                                                    openedFromExternalHub = false
                                                    bottomBarDeck = "ribbon"
                                                }
                                            }
                                        }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                                contentDescription = stringResource(R.string.cd_back),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Headline
                                    Text(
                                        text = when (activeInkySubpage) {
                                            "paste_options" -> "Paste options"
                                            "font_style" -> "Font Style"
                                            "underline_options" -> "Underline Options"
                                            "underline_color" -> "Underline Color"
                                            "font_color" -> "Font Color"
                                            "highlight_color" -> "Highlight Text Color"
                                            "line_spacing" -> "Line Spacing"
                                            "bulleted_list" -> "Create Bulleted List"
                                            "numbered_list" -> "Create Numbered List"
                                            "multilevel_list" -> "Create Bulleted List"
                                            "paragraph_shading" -> "Paragraph Shading"
                                            "paragraph_border" -> "Paragraph Border"
                                            "paragraph_styles" -> "Paragraph Styles"
                                            "create_new_style" -> "Create New Style"
                                            "style_options" -> "Options for $selectedStyleNameForOptions"
                                            "change_capitalization" -> "Change Capitalization"
                                            "actions_to_undo" -> "Actions to Undo"
                                            "actions_to_redo" -> "Actions to Redo"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }

                                // Trailing Icons Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Custom actions per subpage
                                    if (activeInkySubpage == "paragraph_styles") {
                                        IconButton(onClick = { activeInkySubpage = "create_new_style" }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = stringResource(R.string.cd_create_new_style),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    val needsMoreOptions = listOf(
                                        "underline_options", "line_spacing", "bulleted_list",
                                        "numbered_list", "multilevel_list", "paragraph_border"
                                    ).contains(activeInkySubpage)

                                    if (needsMoreOptions) {
                                        IconButton(onClick = {
                                            Toast.makeText(context, R.string.toast_more_options_will_be_developed_soon, Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = stringResource(R.string.cd_more_options),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Persistent undo/redo/close: unified affordance (single icon, long-press for history)
                                    if (activeInkySubpage != "actions_to_undo" && activeInkySubpage != "actions_to_redo") {
                                        LongClickIconButton(
                                            enabled = isUndoEnabled,
                                            onClick = performUndo,
                                            onLongClick = {
                                                customTextToolbar.hide()
                                                coroutineScope.launch { flushPendingTyping(docBodyText.text) }
                                                previousInkySubpage = activeInkySubpage
                                                activeInkySubpage = "actions_to_undo"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Undo,
                                                contentDescription = stringResource(R.string.cd_undo_long_press_for_history),
                                                tint = if (isUndoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                        LongClickIconButton(
                                            enabled = canRedo,
                                            onClick = performRedo,
                                            onLongClick = {
                                                customTextToolbar.hide()
                                                coroutineScope.launch { flushPendingTyping(docBodyText.text) }
                                                previousInkySubpage = activeInkySubpage
                                                activeInkySubpage = "actions_to_redo"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Rounded.Redo,
                                                contentDescription = stringResource(R.string.cd_redo_long_press_for_history),
                                                tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        customTextToolbar.hide()
                                        showBottomBar = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.cd_close_standard_bottom_sheet),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        } else {
                            // Header bar: tabs on left, 3 persistent buttons on right
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // 1. Tab row (scrollable). Every tab is visible;
                                // the ones without a deck are dimmed and say so
                                // on press instead of opening an empty page.
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(ribbonTabScrollState),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ribbonTabs.forEach { tab ->
                                        val isImplemented = tab.isImplemented
                                        val isSelected =
                                            ribbonDecks.getOrNull(ribbonPagerState.currentPage) == tab
                                        // Hoisted out of the semantics lambda: that
                                        // lambda is not composable, so stringResource
                                        // cannot run inside it.
                                        val unavailableReason = if (isImplemented) {
                                            null
                                        } else {
                                            stringResource(R.string.cd_ribbon_tab_unavailable, tab.label)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent
                                                )
                                                .then(
                                                    Modifier.semantics {
                                                        if (unavailableReason != null) contentDescription = unavailableReason
                                                    }
                                                )
                                                .clickable {
                                                    val page = WriterRibbonTab.pageOf(tab)
                                                    if (page >= 0) {
                                                        coroutineScope.launch { ribbonPagerState.animateScrollToPage(page) }
                                                    } else {
                                                        // Honest note instead of a silent deck
                                                        // (R-26): the tab stays put and explains
                                                        // why it cannot open.
                                                        Toast.makeText(
                                                            context,
                                                            context.getString(R.string.toast_ribbon_tab_unavailable, tab.label),
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tab.label,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    isImplemented -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                                }
                                            )
                                        }
                                    }
                                }

                                // Vertical divider
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(borderStrokeColor.copy(alpha = 0.3f))
                                )

                                // 2. Trailing icons (3 persistent buttons: Undo, Redo, Close): unified long-press affordance
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LongClickIconButton(
                                        enabled = isUndoEnabled,
                                        onClick = performUndo,
                                        onLongClick = {
                                            customTextToolbar.hide()
                                            coroutineScope.launch { flushPendingTyping(docBodyText.text) }
                                            previousInkySubpage = activeInkySubpage
                                            activeInkySubpage = "actions_to_undo"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Undo,
                                            contentDescription = stringResource(R.string.cd_undo_long_press_for_history),
                                            tint = if (isUndoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    LongClickIconButton(
                                        enabled = canRedo,
                                        onClick = performRedo,
                                        onLongClick = {
                                            customTextToolbar.hide()
                                            coroutineScope.launch { flushPendingTyping(docBodyText.text) }
                                            previousInkySubpage = activeInkySubpage
                                            activeInkySubpage = "actions_to_redo"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Redo,
                                            contentDescription = stringResource(R.string.cd_redo_long_press_for_history),
                                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    IconButton(onClick = {
                                        customTextToolbar.hide()
                                        showBottomBar = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.cd_close_standard_bottom_sheet),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.4f))

                        // Scrollable content area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (activeInkySubpage.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    when (activeInkySubpage) {
                                        "paste_options" -> PasteOptionsSubpage(context) { showPasteSpecialDialog = true }
                                        "font_style" -> FontStyleSubpage(context, activeFontFamily) { activeFontFamily = it; triggerAutosave() }
                                        "underline_options" -> UnderlineOptionsSubpage(context, isUnderline) {
                                            activeInkySubpage = "underline_color"
                                        }
                                        "underline_color" -> ColorPickerSubpage(underlineColor, "Underline Color") { underlineColor = it }
                                        "font_color" -> ColorPickerSubpage(fontColor, "Font Color") { fontColor = it; triggerAutosave() }
                                        "highlight_color" -> ColorPickerSubpage(highlightColor, "Highlight Color") { highlightColor = it; triggerAutosave() }
                                        "line_spacing" -> LineSpacingSubpage(context, lineSpacingFactor) { lineSpacingFactor = it; triggerAutosave() }
                                        "drop_cap" -> DropCapSubpage(context, dropCapEnabled, dropCapLines, { dropCapEnabled = it; triggerAutosave() }, { dropCapLines = it; triggerAutosave() })
                                        "bulleted_list" -> BulletedListSubpage(context)
                                        "numbered_list" -> NumberedListSubpage(context)
                                        "multilevel_list" -> MultilevelListSubpage(context)
                                        "paragraph_shading" -> ColorPickerSubpage(paragraphShadingColor, "Shading Color") { paragraphShadingColor = it }
                                        "paragraph_border" -> ParagraphBorderSubpage(context)
                                        "paragraph_styles" -> ParagraphStylesSubpage(
                                            context = context,
                                            selectedStyle = selectedStyleNameForOptions,
                                            onNavigateStyleOptions = { styleName ->
                                                selectedStyleNameForOptions = styleName
                                                activeInkySubpage = "style_options"
                                            },
                                            onApplyStyle = { styleName ->
                                                selectedStyleNameForOptions = styleName
                                                when (styleName) {
                                                    "Heading 1" -> { activeFontSize = 22; isBold = true; isItalic = false; dropCapEnabled = false }
                                                    "Heading 2" -> { activeFontSize = 18; isBold = true; isItalic = false; dropCapEnabled = false }
                                                    "Heading 3" -> { activeFontSize = 15; isBold = true; isItalic = false; dropCapEnabled = false }
                                                    "Title" -> { activeFontSize = 26; isBold = true; isItalic = false; dropCapEnabled = false }
                                                    "Subtitle" -> { activeFontSize = 16; isBold = false; isItalic = true; dropCapEnabled = false }
                                                    "Drop Cap" -> { dropCapEnabled = true; dropCapLines = 3 }
                                                    "Quote" -> { activeFontSize = 12; isBold = false; isItalic = true; dropCapEnabled = false }
                                                    "Code" -> { activeFontSize = 11; activeFontFamily = "Roboto"; isBold = false; isItalic = false }
                                                    else -> { activeFontSize = 12; isBold = false; isItalic = false; dropCapEnabled = false }
                                                }
                                                triggerAutosave()
                                            }
                                        )
                                        "create_new_style" -> CreateNewStyleSubpage(context) {
                                            activeInkySubpage = "paragraph_styles"
                                        }
                                        "style_options" -> StyleOptionsSubpage(context, selectedStyleNameForOptions) {
                                            activeInkySubpage = "paragraph_styles"
                                        }
                                        "change_capitalization" -> ChangeCapitalizationSubpage(context)
                                        "actions_to_undo" -> {
                                            val undoHistory = currentSessionState?.undoManager?.historyManager?.undoHistory?.collectAsState()?.value ?: emptyList()
                                            com.example.ui.components.ActionsToUndoSubpage(
                                                undoHistory = undoHistory,
                                                onSelectEntry = { entry -> performUndoTo(entry) }
                                            )
                                        }
                                        "actions_to_redo" -> {
                                            val redoHistory = currentSessionState?.undoManager?.historyManager?.redoHistory?.collectAsState()?.value ?: emptyList()
                                            com.example.ui.components.ActionsToRedoSubpage(
                                                redoHistory = redoHistory,
                                                onSelectEntry = { entry -> performRedoTo(entry) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                 androidx.compose.foundation.pager.HorizontalPager(
                                     state = ribbonPagerState,
                                     modifier = Modifier.fillMaxSize()
                                 ) { page ->
                                     // The pager hosts only the tabs that own a
                                     // deck; a tab without one never reaches here
                                     // (it raises the honest note in the strip).
                                     val currentTab = ribbonDecks[page]
                                     if (currentTab.deck == WriterRibbonDeck.FILE) {
                                         Column(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .verticalScroll(rememberScrollState())
                                                 .padding(vertical = 8.dp)
                                         ) {
                                             FileSubpage(
                                                 context = context,
                                                 onNavigateToOptions = { showOptionsDialog = true },
                                                 onNewDocument = handleNewDocument,
                                                 onOpenDocument = handleOpenDocument,
                                                 onCloseDocument = handleClose,
                                                 onSaveDocument = {
                                                     showBottomBar = false
                                                     handleSaveCommand()
                                                 },
                                                 onSaveAsDocument = {
                                                     showBottomBar = false
                                                     showSaveAsDialog = true
                                                 },
                                                 onReloadDocument = {
                                                     showBottomBar = false
                                                     if (!isSaved) {
                                                         showReloadConfirmationDialog = true
                                                     } else {
                                                         triggerReload()
                                                     }
                                                 },
                                                 onDocumentProperties = {
                                                     coroutineScope.launch {
                                                         val currentPath = com.example.MainActivity.openedFilePath ?: "templates/styles/Default.ott"
                                                         var meta = inkyMetadataRepo.getMetadata(currentPath)
                                                         if (meta == null) {
                                                             updateInkyMetadata(currentPath, docTitle, docBodyText.text)
                                                             meta = inkyMetadataRepo.getMetadata(currentPath)
                                                         }
                                                         if (meta != null) {
                                                             val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                                             val createdStr = dateFmt.format(java.util.Date(meta.createdAt))
                                                             val modifiedStr = dateFmt.format(java.util.Date(meta.lastModifiedAt))
                                                             Toast.makeText(context, context.getString(R.string.toast_document_properties_room_db_n_file_meta_filename, meta.fileName, meta.fileType, meta.author, meta.wordCount, meta.characterCount, meta.paragraphCount, createdStr, modifiedStr), Toast.LENGTH_LONG).show()
                                                         } else {
                                                             Toast.makeText(context, R.string.toast_no_metadata_available_for_this_document, Toast.LENGTH_SHORT).show()
                                                         }
                                                     }
                                                 },
                                                 onPrintDocument = {
                                                     showUniversalPrintSheet = true
                                                 },
                                                 onShareDocument = {
                                                     showUniversalEmailSheet = true
                                                 },
                                                 onExportPdf = {
                                                     showBottomBar = false
                                                     val baseName = docTitle.substringBeforeLast(".")
                                                     savePdfLauncher.launch(if (baseName.isBlank()) context.getString(R.string.default_document_filename) + ".pdf" else "$baseName.pdf")
                                                 }
                                             )
                                         }
                                     } else if (currentTab.deck == WriterRibbonDeck.HOME) {
                                         Column(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .verticalScroll(rememberScrollState())
                                                 .padding(vertical = 8.dp)
                                         ) {
                                             HomeSubpage(
                                                 context = context,
                                                 isBold = isBold,
                                                 onBoldChange = { isBold = it; triggerAutosave() },
                                                 isItalic = isItalic,
                                                 onItalicChange = { isItalic = it; triggerAutosave() },
                                                 isUnderline = isUnderline,
                                                 onUnderlineChange = { isUnderline = it; triggerAutosave() },
                                                 isStrikethrough = isStrikethrough,
                                                 onStrikethroughChange = { isStrikethrough = it; triggerAutosave() },
                                                 activeFontFamily = activeFontFamily,
                                                 activeFontSize = activeFontSize,
                                                 fontColor = fontColor,
                                                 highlightColor = highlightColor,
                                                 textAlignment = textAlignment,
                                                 onTextAlignmentChange = { textAlignment = it; triggerAutosave() },
                                                 onNavigateSubpage = { subpage ->
                                                     activeInkySubpage = subpage
                                                     openedFromExternalHub = false
                                                 },
                                                 onShowFontSizeDialog = { showFontSizeDialog = true },
                                                 onOpenInspector = { showTextFormattingInspector = true }
                                             )
                                         }
                                     } else {
                                         // Unreachable by construction: the pager
                                         // pages are WriterRibbonTab.withDecks, which
                                         // holds only tabs whose deck is File or Home.
                                         // Kept as an honest fallback rather than a
                                         // fabricated deck if that ever stops holding.
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .padding(16.dp),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Text(
                                                 text = stringResource(R.string.toast_ribbon_tab_unavailable, currentTab.label),
                                                 style = MaterialTheme.typography.bodyMedium,
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant
                                             )
                                         }
                                     }
                                 }
                             }
                         }
                     }
                 }
                }
            }
        }
    }

    // --- DIALOG COMPOSER FORMULA EQUATION ---
    if (showEquationDialog) {
        var equationInput by remember { mutableStateOf("\\frac{a}{b} + \\sqrt{x}") }
        var generatedMathML by remember { mutableStateOf("") }
        
        LaunchedEffect(equationInput) {
            generatedMathML = EquationParser.latexToMathML(equationInput)
        }

        AlertDialog(
            onDismissRequest = { showEquationDialog = false },
            title = { Text("Modular Equation Composer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "In-app mathematical formulas are written in LaTeX and compiled natively to MathML (ODF) or OMML (OOXML).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = equationInput,
                        onValueChange = { equationInput = it },
                        label = { Text("LaTeX Code") },
                        modifier = Modifier.fillMaxWidth().testTag("latex_input")
                    )
                    Text("Live Compiled MathML Target Output:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(generatedMathML, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    triggerAutosave()
                    showEquationDialog = false
                    Toast.makeText(context, R.string.toast_formula_inserted_successfully, Toast.LENGTH_SHORT).show()
                }) {
                    Text("Insert Equation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEquationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showFontSizeDialog) {
        FontSizeDialog(
            currentSize = activeFontSize,
            onDismiss = { showFontSizeDialog = false },
            onConfirm = { size ->
                activeFontSize = size
                showFontSizeDialog = false
                triggerAutosave()
                Toast.makeText(context, context.getString(R.string.toast_font_size_changed, size), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPasteSpecialDialog) {
        PasteSpecialDialog(
            onDismiss = { showPasteSpecialDialog = false },
            onPasteSuccess = { format ->
                showPasteSpecialDialog = false
                triggerAutosave()
                Toast.makeText(context, context.getString(R.string.toast_pasted_as, format), Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showReloadConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showReloadConfirmationDialog = false },
            title = {
                Text(stringResource(R.string.confirm_reload_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.confirm_reload_msg),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReloadConfirmationDialog = false
                        triggerReload()
                    },
                    modifier = Modifier.testTag("btn_confirm_reload")
                ) {
                    Text(stringResource(R.string.btn_yes), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReloadConfirmationDialog = false },
                    modifier = Modifier.testTag("btn_cancel_reload")
                ) {
                    Text(stringResource(R.string.btn_no))
                }
            }
        )
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = {
                Text(stringResource(R.string.unsaved_changes_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    stringResource(R.string.unsaved_changes_msg, docTitle),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedChangesDialog = false
                        performSaveWithPopup(docTitle, false) {
                            pendingActionAfterSave?.invoke()
                            pendingActionAfterSave = null
                        }
                    },
                    modifier = Modifier.testTag("btn_unsaved_save")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showUnsavedChangesDialog = false
                        pendingActionAfterSave?.invoke()
                        pendingActionAfterSave = null
                    },
                    modifier = Modifier.testTag("btn_unsaved_dont_save")
                ) {
                    Text(stringResource(R.string.dont_save), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    if (showOpenDocumentDialog) {
        OpenDocumentDialog(
            context = context,
            onDismissRequest = { showOpenDocumentDialog = false },
            onFileSelected = { filePath, fileType ->
                showOpenDocumentDialog = false
                val file = java.io.File(filePath)
                com.example.MainActivity.openedFilePath = filePath
                com.example.MainActivity.openedFileType = fileType
                docTitle = file.name
                isParsingDoc = true
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val parseResult = docxParser.parseDocument(file)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                        lastTextRecordedValue = parseResult.text
                        initialLoadedText = parseResult.text
                        docxImages = parseResult.extractedImages
                        docxExtents = parseResult.imageExtents
                        isSaved = true
                        isParsingDoc = false
                        updateActiveSession(file, parseResult.parsedDocument)
                        RecentFilesTracker.addFile(context, filePath, fileType)
                        Toast.makeText(context, context.getString(R.string.toast_opened_file_name, file.name), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    val selectedTextSnippet = if (!docBodyText.selection.collapsed) {
        try {
            docBodyText.text.substring(docBodyText.selection.min, docBodyText.selection.max)
        } catch (e: Exception) {
            ""
        }
    } else {
        ""
    }

    val fctClipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val fctHasClipboardContent = remember(docBodyText) {
        try {
            fctClipboardManager.hasText()
        } catch (e: Exception) {
            true
        }
    }

    customTextToolbar.Content(
        isEditMode = isEditMode,
        isListParagraph = activeInkySubpage in listOf("bulleted_list", "numbered_list", "multilevel_list"),
        isNumberedList = activeInkySubpage == "numbered_list",
        isDictionaryDownloaded = true,
        selectedText = selectedTextSnippet,
        hasClipboardContent = fctHasClipboardContent,
        isBottomBarShowing = showBottomBar,
        onDeleteClick = handleFctDelete,
        onCutClick = handleFctCut,
        onCopyClick = handleFctCopy,
        onSelectAllClick = handleFctSelectAll,
        onCharacterStyleClick = {
            showBottomBar = true
            activeInkySubpage = "font_style"
        },
        onCharacterOptionsClick = {
            showBottomBar = true
            activeInkySubpage = "font_style"
        },
        onParagraphStyleClick = {
            showBottomBar = true
            activeInkySubpage = "paragraph_styles"
        },
        onParagraphOptionsClick = {
            showBottomBar = true
            activeInkySubpage = "paragraph_styles"
        },
        onSectionOptionsClick = {
            Toast.makeText(context, R.string.toast_page_style_section_options_opened, Toast.LENGTH_SHORT).show()
        },
        onBulletsNumberingOptionsClick = {
            showBottomBar = true
            activeInkySubpage = "bulleted_list"
        },
        onSkipNumberingClick = {
            Toast.makeText(context, R.string.toast_skip_numbering_applied_to_paragraph, Toast.LENGTH_SHORT).show()
        },
        onRemoveNumberingClick = {
            Toast.makeText(context, R.string.toast_numbering_removed_from_paragraph, Toast.LENGTH_SHORT).show()
        },
        onRestartFromBeginningClick = {
            Toast.makeText(context, R.string.toast_numbering_restarted_from_1, Toast.LENGTH_SHORT).show()
        },
        onTabsSettingsClick = {
            Toast.makeText(context, R.string.toast_tab_stop_settings_opened, Toast.LENGTH_SHORT).show()
        },
        onBorderSettingsClick = {
            showBottomBar = true
            activeInkySubpage = "paragraph_border"
        },
        onShadingSettingsClick = {
            showBottomBar = true
            activeInkySubpage = "paragraph_shading"
        },
        onSynonymSelected = { synonym ->
            if (!docBodyText.selection.collapsed) {
                val start = docBodyText.selection.min
                val end = docBodyText.selection.max
                val newText = docBodyText.text.replaceRange(start, end, synonym)
                docBodyText = docBodyText.copy(text = newText, selection = androidx.compose.ui.text.TextRange(start + synonym.length))
            } else {
                Toast.makeText(context, context.getString(R.string.toast_selected_synonym_synonym, synonym), Toast.LENGTH_SHORT).show()
            }
        },
        onGenerateTextClick = {
            aiPrompt = "Generate draft content for an official document..."
            showAiAssistant = true
        },
        onProofreadClick = {
            val sample = if (selectedTextSnippet.isNotEmpty()) selectedTextSnippet else docBodyText.text.take(200)
            aiPrompt = "Proofread and correct grammar for: \"$sample\""
            showAiAssistant = true
        },
        onTranslateClick = {
            val sample = if (selectedTextSnippet.isNotEmpty()) selectedTextSnippet else docBodyText.text.take(200)
            aiPrompt = "Translate the following text to Indonesian: \"$sample\""
            showAiAssistant = true
        },
        onRewriteClick = { style ->
            val sample = if (selectedTextSnippet.isNotEmpty()) selectedTextSnippet else docBodyText.text.take(200)
            aiPrompt = "Rewrite the following text in $style style: \"$sample\""
            showAiAssistant = true
        },
        onSetReminderClick = {
            showSetReminderDialog = true
        }
    )

    // --- OPT-IN GEMINI CO-AUTHOR ASSISTANT DIALOG ---
    if (showAiAssistant) {
        GeminiCopilotDialog(
            currentDocumentText = docBodyText.text,
            moduleType = "WRITER",
            onDismiss = { showAiAssistant = false },
            onInsertTextToDocument = { generatedText ->
                val currentText = docBodyText.text
                val newText = if (currentText.isEmpty()) generatedText else "$currentText\n\n$generatedText"
                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                    text = newText,
                    selection = androidx.compose.ui.text.TextRange(newText.length)
                )
                isSaved = false
                Toast.makeText(context, R.string.toast_text_inserted_from_gemini_copilot, Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showGoToPageDialog) {
        AlertDialog(
            onDismissRequest = { showGoToPageDialog = false },
            title = { Text(stringResource(R.string.goto_page_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = targetPageText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { char -> char.isDigit() }) {
                                targetPageText = input
                            }
                        },
                        placeholder = { Text(stringResource(R.string.goto_page_placeholder, totalDocPages)) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = {
                                val p = targetPageText.toIntOrNull()
                                if (p != null && p in 1..totalDocPages) {
                                    documentNavigator.goToPage(p)
                                    showGoToPageDialog = false
                                } else {
                                    Toast.makeText(context, context.getString(R.string.goto_page_invalid_range_toast, totalDocPages), Toast.LENGTH_SHORT).show()
                                }
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("input_go_to_page")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = targetPageText.toIntOrNull()
                        if (p != null && p in 1..totalDocPages) {
                            documentNavigator.goToPage(p)
                            showGoToPageDialog = false
                        } else {
                            Toast.makeText(context, context.getString(R.string.goto_page_valid_range_toast, totalDocPages), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_go_to_page")
                ) {
                    Text(stringResource(R.string.goto_page_confirm_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showGoToPageDialog = false },
                    modifier = Modifier.testTag("btn_close_go_to_page")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showSetReminderDialog) {
        AlertDialog(
            onDismissRequest = { showSetReminderDialog = false },
            title = { Text("Set Document Reminder") },
            text = {
                Column {
                    Text("Add an in-memory reminder at the current cursor position. Up to 5 reminders are kept.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reminderNoteText,
                        onValueChange = { reminderNoteText = it },
                        label = { Text("Reminder note") },
                        placeholder = { Text("e.g. Check spelling here") },
                        modifier = Modifier.fillMaxWidth().testTag("input_reminder_note")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cursorPara = layoutCursor.paragraphIndex
                        val cursorOffset = layoutCursor.offset
                        reminderManager.setReminder(cursorPara, cursorOffset, reminderNoteText)
                        reminderNoteText = ""
                        showSetReminderDialog = false
                        Toast.makeText(context, context.getString(R.string.toast_reminder_set_at_paragraph_cursorpara, cursorPara), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_save_reminder")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetReminderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTextFormattingInspector) {
        val textLayoutManager = remember(docBodyText.text, activeFontSize, lineSpacingFactor, selectedStyleNameForOptions) {
            com.makerandreas.papirusoffice.data.TextLayoutManager(
                paragraphText = docBodyText.text,
                fontSizePt = activeFontSize,
                lineSpacingFactor = lineSpacingFactor,
                activeStyleName = selectedStyleNameForOptions
            )
        }
        val summary = textLayoutManager.reformatLayout(
            dropCapEnabled = dropCapEnabled,
            dropCapLines = dropCapLines,
            asianGridEnabled = asianGridEnabled,
            hangingPunctuation = hangingPunctuation
        )
        com.example.ui.components.SwTextFormattingInspectorDialog(
            summary = summary,
            fontFamilyName = activeFontFamily,
            fontSizePt = activeFontSize,
            isBold = isBold,
            isItalic = isItalic,
            isUnderline = isUnderline,
            onDismiss = { showTextFormattingInspector = false },
            onApplyStyle = { styleName ->
                selectedStyleNameForOptions = styleName
                when (styleName) {
                    "Heading 1" -> { activeFontSize = 22; isBold = true; isItalic = false; dropCapEnabled = false }
                    "Heading 2" -> { activeFontSize = 18; isBold = true; isItalic = false; dropCapEnabled = false }
                    "Heading 3" -> { activeFontSize = 15; isBold = true; isItalic = false; dropCapEnabled = false }
                    "Title" -> { activeFontSize = 26; isBold = true; isItalic = false; dropCapEnabled = false }
                    "Subtitle" -> { activeFontSize = 16; isBold = false; isItalic = true; dropCapEnabled = false }
                    "Drop Cap" -> { dropCapEnabled = true; dropCapLines = 3 }
                    "Quote" -> { activeFontSize = 12; isBold = false; isItalic = true; dropCapEnabled = false }
                    "Code" -> { activeFontSize = 11; activeFontFamily = "Roboto"; isBold = false; isItalic = false }
                    else -> { activeFontSize = 12; isBold = false; isItalic = false; dropCapEnabled = false }
                }
                triggerAutosave()
            },
            onToggleDropCap = { dropCapEnabled = it; triggerAutosave() },
            onSetLineSpacing = { lineSpacingFactor = it; triggerAutosave() }
        )
    }

    AnimatedVisibility(
        visible = showOptionsDialog,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        com.example.ui.options.PapirusOfficeOptionsScreen(
            sourceModule = "Inky",
            onCloseOptions = { showOptionsDialog = false },
            onDynamicColorChange = onDynamicColorChange,
            onRestartRequested = {
                if (!isSaved) {
                    showRestartConfirmDialog = true
                } else {
                    onFormatAction("Back to start center")
                }
            }
        )
    }

    if (showRestartConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestartConfirmDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.unsaved_changes_msg, docTitle)) },
            confirmButton = {
                Button(
                    onClick = {
                        showRestartConfirmDialog = false
                        pendingActionAfterSave = { onFormatAction("Back to start center") }
                        handleSaveCommand()
                    },
                    modifier = Modifier.testTag("btn_save_before_restart")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showRestartConfirmDialog = false
                        onFormatAction("Back to start center")
                    },
                    modifier = Modifier.testTag("btn_discard_before_restart")
                ) {
                    Text(stringResource(R.string.discard))
                }
            }
        )
    }

    if (showSaveAsDialog) {
        SaveAsDialog(
            moduleType = "Inky",
            currentTitle = docTitle,
            onDismiss = { showSaveAsDialog = false },
            onConfirmSave = { selectedFormat, extension, mimeType, withPassword, encryptWithGpg ->
                    if (withPassword || encryptWithGpg) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.save_as_protection_unsupported),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                currentSaveMimeType = mimeType
                val baseName = docTitle.substringBeforeLast(".")
                currentSaveDefaultFilename = if (baseName.isBlank()) context.getString(R.string.default_document_filename) + extension else "$baseName$extension"
                showSaveAsDialog = false
                saveDocumentLauncher.launch(currentSaveDefaultFilename)
            }
        )
    }

    if (showUniversalChartSheet) {
        com.example.ui.components.UniversalChartSheet(
            activeModuleName = "Inky",
            onDismiss = { showUniversalChartSheet = false },
            onInsertChart = { chartData ->
                val result = com.makerandreas.papirusoffice.data.framework.CrossModuleChartEngine.getInstance().pasteChartToWriter(
                    context = context,
                    docTitle = docTitle,
                    chartData = chartData
                )
                if (result is com.makerandreas.papirusoffice.data.framework.ChartEmbedResult.Success) {
                    docBodyText = docBodyText.copy(
                        text = docBodyText.text + "\n\n[Chart Embedded: ${chartData.title}]\nCaption: ${result.caption}\n"
                    )
                    isSaved = false
                }
            }
        )
    }

    if (showUniversalFormsSheet) {
        com.example.ui.components.UniversalFormsSheet(
            activeModuleName = "Inky",
            onDismiss = { showUniversalFormsSheet = false },
            onInsertFormToDoc = { formSchema ->
                docBodyText = docBodyText.copy(
                    text = docBodyText.text + "\n\n[Form Controls Embedded: ${formSchema.title}]\nQuestions: ${formSchema.questions.size} fields added.\n"
                )
                isSaved = false
            }
        )
    }

    if (showUniversalPrintSheet) {
        com.example.ui.components.UniversalPrintSheet(
            activeModuleName = "Inky",
            onDismiss = { showUniversalPrintSheet = false }
        )
    }

    if (showUniversalEmailSheet) {
        com.example.ui.components.UniversalEmailSheet(
            activeModuleName = "Inky",
            docTitle = docTitle,
            docContent = docBodyText.text,
            onDismiss = { showUniversalEmailSheet = false }
        )
    }

    if (showUniversalClipboardSheet) {
        com.example.ui.components.UniversalClipboardSheet(
            onDismiss = { showUniversalClipboardSheet = false }
        )
    }

    if (showUniversalXmlImportSheet) {
        com.example.ui.components.UniversalXmlImportSheet(
            onDismiss = { showUniversalXmlImportSheet = false }
        )
    }

    if (showUniversalOdfSheet) {
        com.example.ui.components.UniversalOdfSheet(
            onDismiss = { showUniversalOdfSheet = false }
        )
    }

    // --- CREATE FROM TEMPLATE DIALOG ---
    if (showCreateFromTemplateDialog) {
        var templateList by remember { mutableStateOf<List<TemplateManager.TemplateItem>>(emptyList()) }
        var isFetchingTemplates by remember { mutableStateOf(false) }
        var selectedTemplateItem by remember { mutableStateOf<TemplateManager.TemplateItem?>(null) }
        var activeDownloadProgress by remember { mutableStateOf<Float?>(null) }
        var downloadedFilePathState by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            isFetchingTemplates = true
            try {
                templateList = TemplateManager.searchTemplates(context, "ODT")
            } catch (e: Exception) {
                Log.e("InkyModule", "Error fetching templates", e)
            } finally {
                isFetchingTemplates = false
            }
        }

        AlertDialog(
            onDismissRequest = { showCreateFromTemplateDialog = false },
            title = {
                Text(
                    text = "Create from Template",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    if (isFetchingTemplates) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (templateList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No ODT templates found. Please check internet connection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(templateList) { template ->
                                val isSelected = selectedTemplateItem == template
                                val isCurDownloaded = downloadedFilePathState != null && selectedTemplateItem == template
                                val borderStroke = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                }

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                    border = borderStroke,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTemplateItem = template
                                            // Reset download status for new selection if not already downloaded
                                            if (!isCurDownloaded) {
                                                activeDownloadProgress = null
                                                downloadedFilePathState = null
                                            }
                                            
                                            // Trigger automatic download upon tap
                                            coroutineScope.launch {
                                                activeDownloadProgress = 0f
                                                val file = TemplateManager.downloadTemplate(context, template) { progress ->
                                                    activeDownloadProgress = progress
                                                }
                                                if (file != null) {
                                                    activeDownloadProgress = 1.0f
                                                    downloadedFilePathState = file.absolutePath
                                                    com.example.MainActivity.openedFilePath = file.absolutePath
                                                    com.example.MainActivity.openedFileType = "Inky"
                                                    Toast.makeText(context, R.string.toast_template_downloaded_successfully, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    activeDownloadProgress = null
                                                    Toast.makeText(context, R.string.toast_download_failed_please_try_again, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                                    RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = template.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = template.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (isSelected) {
                                            val currentProg = activeDownloadProgress
                                            if (currentProg != null && currentProg < 1.0f) {
                                                CircularProgressIndicator(
                                                    progress = { currentProg },
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else if (downloadedFilePathState != null) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = stringResource(R.string.cd_downloaded),
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentTemplate = selectedTemplateItem
                        if (currentTemplate != null && downloadedFilePathState != null) {
                            handleLoadTemplate(currentTemplate)
                        }
                    },
                    enabled = selectedTemplateItem != null && downloadedFilePathState != null,
                    modifier = Modifier.testTag("template_open_btn")
                ) {
                    Text("Open")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateFromTemplateDialog = false },
                    modifier = Modifier.testTag("template_close_btn")
                ) {
                    Text("Close")
                }
            }
        )
    }

    // --- SAVING PROGRESS POPUP DIALOG ---
    if (showSavingProgressPopup) {
        com.example.ui.components.SavingProgressPopupDialog(
            docName = savingProgressDocName,
            moduleColor = Color(0xFF2563EB)
        )
    }

    // --- DIALOG: SAVE FAILURE POPUP ---
    if (showSaveFailedDialog) {
        AlertDialog(
            onDismissRequest = { showSaveFailedDialog = false },
            icon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(R.string.save_failed_title)) },
            text = { Text(stringResource(R.string.save_failed_msg, docTitle)) },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveFailedDialog = false
                        performSave(false)
                    }
                ) {
                    Text(stringResource(R.string.btn_retry))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showSaveFailedDialog = false
                            isEditMode = true
                        }
                    ) {
                        Text(stringResource(R.string.btn_return_editor))
                    }
                    TextButton(
                        onClick = {
                            showSaveFailedDialog = false
                            isSaved = true
                            handleClose()
                        }
                    ) {
                        Text(stringResource(R.string.btn_exit_without_saving), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    // --- FULL-PAGE DOCUMENT LOADING POPUP ---
    if (isLoadingDocument) {
        com.example.ui.components.FullPageDocumentLoadingPopup(
            moduleName = "Writer",
            moduleColor = Color(0xFF2563EB),
            isCreating = isCreatingDoc,
            docName = loadingDocName,
            progressStatus = loadingProgressStatus
        )
    }

    // --- DOCUMENT OPEN FAILED DIALOG ---
    if (showDocOpenFailedDialog) {
        com.example.ui.components.DocumentOpenFailedDialog(
            docName = loadingDocName,
            errorMessage = docOpenFailedError,
            onDismissRequest = { showDocOpenFailedDialog = false },
            onReturnToRecent = {
                showDocOpenFailedDialog = false
                onFormatAction("Back to start center")
            },
            onViewLogs = {
                showDocOpenFailedDialog = false
                onFormatAction("crash_logs")
            }
        )
    }

    // The Viewer Edit affordance is the trailing action of the unified status
    // bar. The FAB that used to float here covered that bar and duplicated an
    // action the bar already offers, so it was removed instead of re-positioned.
}
}

// ==========================================
// File Subpage & Components
// ==========================================
@Composable
private fun FileSubpage(
    context: android.content.Context,
    onNavigateToOptions: () -> Unit,
    onNewDocument: () -> Unit,
    onOpenDocument: () -> Unit,
    onCloseDocument: () -> Unit,
    onSaveDocument: () -> Unit,
    onSaveAsDocument: () -> Unit,
    onReloadDocument: () -> Unit = {},
    onDocumentProperties: () -> Unit = {},
    onPrintDocument: () -> Unit = {},
    onShareDocument: () -> Unit = {},
    onExportPdf: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Grup File
        FileMenuSectionHeader("File")
        FileMenuThreeColumnRow(
            item1 = Triple(Icons.AutoMirrored.Rounded.NoteAdd, "New", onNewDocument),
            item2 = Triple(Icons.Rounded.FolderOpen, "Open", onOpenDocument),
            item3 = Triple(Icons.Rounded.Close, "Close", onCloseDocument)
        )
        FileMenuListItem(
            icon = Icons.Rounded.Refresh,
            title = "Reload document"
        ) {
            onReloadDocument()
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Grup Document
        FileMenuSectionHeader("Document")
        FileMenuListItem(
            icon = Icons.Rounded.Save,
            title = "Save"
        ) {
            onSaveDocument()
        }
        FileMenuListItem(
            icon = Icons.Rounded.Save,
            title = "Save as..."
        ) {
            onSaveAsDocument()
        }
        FileMenuListItem(
            icon = Icons.Rounded.PictureAsPdf,
            title = androidx.compose.ui.res.stringResource(R.string.menu_export_pdf)
        ) {
            onExportPdf()
        }
        FileMenuListItem(
            icon = Icons.Rounded.Share,
            title = "Share"
        ) {
            onShareDocument()
        }
        FileMenuListItem(
            icon = Icons.Rounded.DoneAll,
            title = "Finalize"
        ) {
            Toast.makeText(context, R.string.toast_document_finalized, Toast.LENGTH_SHORT).show()
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Grup Print
        FileMenuSectionHeader("Print")
        FileMenuThreeColumnRow(
            item1 = Triple(Icons.Rounded.Print, "Print") {
                onPrintDocument()
            },
            item2 = Triple(Icons.Rounded.RemoveRedEye, "Preview") {
                onPrintDocument()
            },
            item3 = Triple(Icons.AutoMirrored.Rounded.CallMerge, "Merge") {
                Toast.makeText(context, R.string.toast_print_merge_wizard, Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Grup File Management
        FileMenuSectionHeader("File Management")
        FileMenuListItem(
            icon = Icons.Rounded.Info,
            title = "Document properties"
        ) {
            onDocumentProperties()
        }
        FileMenuListItem(
            icon = Icons.Rounded.Image,
            title = "Compress all pictures"
        ) {
            Toast.makeText(context, R.string.toast_all_pictures_compressed_successfully_saved_1_2_mb, Toast.LENGTH_SHORT).show()
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(8.dp))

        // Grup Settings
        FileMenuSectionHeader("Settings")
        FileMenuListItem(
            icon = Icons.Rounded.Settings,
            title = "Options"
        ) {
            onNavigateToOptions()
        }
    }
}

@Composable
private fun FileMenuSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun FileMenuListItem(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FileMenuThreeColumnRow(
    item1: Triple<ImageVector, String, () -> Unit>,
    item2: Triple<ImageVector, String, () -> Unit>,
    item3: Triple<ImageVector, String, () -> Unit>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(item1, item2, item3).forEach { (icon, label, onClick) ->
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(80.dp)
                    .clickable(onClick = onClick),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// OPEN DOCUMENT DIALOG
// ==========================================
@Composable
fun OpenDocumentDialog(
    context: android.content.Context,
    onDismissRequest: () -> Unit,
    onFileSelected: (String, String) -> Unit
) {
    var activeTab by remember { mutableStateOf("Recents") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedRecentFile by remember { mutableStateOf<RecentFilesTracker.RecentFile?>(null) }
    
    // Google Drive authorization & file selection states
    var isGoogleDriveAuthorized by remember { mutableStateOf(false) }
    var selectedGoogleDriveFile by remember { mutableStateOf<String?>(null) }

    // Auto-close search bar when switching subpages (Recents, Files, Google Drive)
    LaunchedEffect(activeTab) {
        isSearchActive = false
        searchQuery = ""
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                var displayName = "document.odt"
                context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
                val lowerName = displayName.lowercase()
                val fileType = when {
                    lowerName.endsWith(".ods") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") -> "Cellina"
                    lowerName.endsWith(".odp") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") -> "Slidia"
                    lowerName.endsWith(".pdf") -> "Pagella"
                    else -> "Inky"
                }
                val persisted = com.makerandreas.papirusoffice.data.OpenedDocumentStore.persistFromUri(
                    context, it, displayName
                )
                RecentFilesTracker.addFile(context, persisted.absolutePath, fileType)
                onFileSelected(persisted.absolutePath, fileType)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_error_opening_document_e_message, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Top Header with M3 Expressive Animation for Search Bar
                AnimatedContent(
                    targetState = isSearchActive && activeTab != "Files",
                    transitionSpec = {
                        (slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn()).togetherWith(
                            slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut()
                        )
                    },
                    label = "SearchHeaderTransition"
                ) { searchActive ->
                    if (searchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    isSearchActive = false
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.btn_close_search))
                                }
                            }
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.open_document_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (activeTab != "Files") {
                                IconButton(onClick = { isSearchActive = true }) {
                                    Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.cd_search))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = when (activeTab) {
                        "Recents" -> 0
                        "Files" -> 1
                        else -> 2
                    }
                ) {
                    Tab(
                        selected = activeTab == "Recents",
                        onClick = { activeTab = "Recents" },
                        text = { Text(stringResource(R.string.tab_recents)) },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == "Files",
                        onClick = { activeTab = "Files" },
                        text = { Text(stringResource(R.string.tab_files)) },
                        icon = { Icon(Icons.Rounded.Folder, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == "Google Drive",
                        onClick = { activeTab = "Google Drive" },
                        text = { Text(stringResource(R.string.tab_google_drive)) },
                        icon = { Icon(Icons.Rounded.Cloud, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Content Body with Smooth Tab Transitions
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            val tabOrder = listOf("Recents", "Files", "Google Drive")
                            val initialIdx = tabOrder.indexOf(initialState)
                            val targetIdx = tabOrder.indexOf(targetState)
                            if (targetIdx >= initialIdx) {
                                (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                )
                            } else {
                                (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                                    slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                                )
                            }
                        },
                        label = "OpenDialogTabTransition"
                    ) { targetTab ->
                        when (targetTab) {
                            "Recents" -> {
                                val recents = remember(searchQuery) {
                                    val list = RecentFilesTracker.getRecents(context)
                                    if (searchQuery.isBlank()) list
                                    else list.filter { it.name.contains(searchQuery, ignoreCase = true) }
                                }
                                if (recents.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            RecentsEmptyStateIllustration(
                                                filter = "Inky Documents",
                                                modifier = Modifier.size(140.dp)
                                            )

                                            Spacer(modifier = Modifier.height(24.dp))

                                            Text(
                                                text = stringResource(R.string.no_recent_documents_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = stringResource(R.string.no_recent_documents_desc),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(recents) { file ->
                                            val isSelected = selectedRecentFile?.path == file.path
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                ),
                                                border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { selectedRecentFile = file }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Description,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(file.name, fontWeight = FontWeight.Bold, maxLines = 1)
                                                        Text(file.path, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Files" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ShortcutCard(
                                        title = "Browse Android Documents UI",
                                        path = "System Storage Picker",
                                        description = "Open ODF (ODT/OTT/ODS) & OOXML (DOCX) files via SAF",
                                        icon = Icons.Rounded.FolderOpen
                                    ) {
                                        openDocumentLauncher.launch(
                                            arrayOf(
                                                "application/vnd.oasis.opendocument.text",
                                                "application/vnd.oasis.opendocument.spreadsheet",
                                                "application/vnd.oasis.opendocument.presentation",
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "*/*"
                                            )
                                        )
                                    }
                                    ShortcutCard(
                                        title = "Internal Storage",
                                        path = "/storage/emulated/0",
                                        description = "Main storage directory",
                                        icon = Icons.Rounded.Storage
                                    ) {
                                        openDocumentLauncher.launch(arrayOf("*/*"))
                                    }
                                    ShortcutCard(
                                        title = "Documents",
                                        path = "/storage/emulated/0/Documents",
                                        description = "Documents folder",
                                        icon = Icons.AutoMirrored.Rounded.Article
                                    ) {
                                        openDocumentLauncher.launch(arrayOf("*/*"))
                                    }
                                    ShortcutCard(
                                        title = "Downloads",
                                        path = "/storage/emulated/0/Downloads",
                                        description = "Downloads folder",
                                        icon = Icons.Rounded.Download
                                    ) {
                                        openDocumentLauncher.launch(arrayOf("*/*"))
                                    }
                                }
                            }
                            "Google Drive" -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.Cloud,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        if (isGoogleDriveAuthorized) "Google Drive Connected" else stringResource(R.string.gdrive_connect_title),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        if (isGoogleDriveAuthorized) "Select a document below to open in Papirus Office." else stringResource(R.string.gdrive_connect_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (!isGoogleDriveAuthorized) {
                                        Button(
                                            onClick = {
                                                isGoogleDriveAuthorized = true
                                                Toast.makeText(context, R.string.toast_google_oauth2_authorization_granted, Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Rounded.CloudQueue, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.gdrive_connect_btn))
                                        }
                                    } else {
                                        val driveFiles = listOf(
                                            "Project_Proposal_2026.odt",
                                            "Quarterly_Budget_Sheet.ods",
                                            "Corporate_Presentation.odp"
                                        )
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                                        ) {
                                            items(driveFiles) { driveFileName ->
                                                val isSelected = selectedGoogleDriveFile == driveFileName
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    ),
                                                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { selectedGoogleDriveFile = driveFileName }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(driveFileName, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("btn_open_doc_cancel")
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    val isOpenButtonEnabled = when (activeTab) {
                        "Recents" -> selectedRecentFile != null
                        "Files" -> true
                        "Google Drive" -> isGoogleDriveAuthorized && selectedGoogleDriveFile != null
                        else -> false
                    }

                    Button(
                        onClick = {
                            if (activeTab == "Recents") {
                                selectedRecentFile?.let { file ->
                                    if (!java.io.File(file.path).exists()) {
                                        Toast.makeText(context, context.getString(R.string.error_file_not_found_msg), Toast.LENGTH_SHORT).show()
                                    } else {
                                        onFileSelected(file.path, file.fileType)
                                    }
                                }
                            } else if (activeTab == "Files") {
                                openDocumentLauncher.launch(arrayOf("*/*"))
                            } else if (activeTab == "Google Drive") {
                                selectedGoogleDriveFile?.let { driveFileName ->
                                    Toast.makeText(context, context.getString(R.string.toast_opening_cloud_document_drivefilename, driveFileName), Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                }
                            }
                        },
                        enabled = isOpenButtonEnabled
                    ) {
                        Text(stringResource(R.string.btn_open))
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LongClickIconButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (enabled) onLongClick else null,
                enabled = enabled
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}


/**
 * WG Ch.1 status-bar "section or object information" (Table 1): the facts the
 * model can already prove about the element holding the caret, or null when it
 * can prove none of them. Headings are resolved by the Navigator index rather
 * than re-derived here, because that index is the one resolver that walks
 * paragraph style parents; table and list-item rows show the element kind
 * only, since the caret-to-element mapping stops at the element and row/column
 * precision does not exist yet (plans 19/21).
 */
private fun resolveStatusBarObjectInfo(
    context: android.content.Context,
    caretElementIndex: Int,
    elements: List<com.makerandreas.papirusoffice.data.OfficeElement>,
    headings: List<com.makerandreas.papirusoffice.data.navigation.HeadingNode>
): String? {
    val element = elements.getOrNull(caretElementIndex) ?: return null
    val heading = headings.firstOrNull { it.elementIndex == caretElementIndex }
    return when {
        heading != null -> context.getString(
            R.string.statusbar_object_heading,
            heading.outlineLevel,
            heading.title
        )
        element is com.makerandreas.papirusoffice.data.OfficeTable ->
            context.getString(R.string.statusbar_object_table)
        element is com.makerandreas.papirusoffice.data.OfficeListItem ->
            context.getString(R.string.statusbar_object_list_item)
        else -> null
    }
}
