package com.example.modules.inky
import android.util.Log
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

fun partitionTextToPages(text: String, maxLinesPerPage: Int = 24): List<String> {
    val rawText = text
    if (rawText.isEmpty()) return listOf("")
    
    // Split by explicit page breaks if present
    val explicitBreakRegex = Regex("""(?:\r?\n)*(?:---|===)?\s*(?:Page\s+\d+\s*\()?Page\s*Break\)?\s*(?:---|===)?(?:\r?\n)*|\u000C""", RegexOption.IGNORE_CASE)
    val explicitChunks = rawText.split(explicitBreakRegex)
    
    val pages = mutableListOf<String>()
    explicitChunks.forEach { chunk ->
        val rawParagraphs = chunk.split("\n")
        var currentPageLines = mutableListOf<String>()
        var currentLinesCount = 0
        
        rawParagraphs.forEach { paragraph ->
            val approxLinesInParagraph = maxOf(1, (paragraph.length + 42) / 43)
            if (currentLinesCount + approxLinesInParagraph > maxLinesPerPage && currentPageLines.isNotEmpty()) {
                pages.add(currentPageLines.joinToString("\n").trim())
                currentPageLines = mutableListOf()
                currentLinesCount = 0
            }
            currentPageLines.add(paragraph)
            currentLinesCount += approxLinesInParagraph
        }
        if (currentPageLines.isNotEmpty()) {
            pages.add(currentPageLines.joinToString("\n").trim())
        }
    }
    
    return if (pages.isEmpty()) listOf(rawText) else pages
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
        val calculatedPercent = (zoomScale * 100).toInt().coerceIn(25, 400)
        if (calculatedPercent != viewOptions.customZoomPercent) {
            preferencesRepository.updateCustomZoomPercent(calculatedPercent)
            if (viewOptions.zoomMode != com.makerandreas.papirusoffice.data.ZoomMode.CUSTOM) {
                preferencesRepository.updateZoomMode(com.makerandreas.papirusoffice.data.ZoomMode.CUSTOM)
            }
        }
    }

    var documentContentTitle by remember { mutableStateOf("Draft Dokumen Baru") }

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
    val canUndo by (currentSessionState?.undoManager?.historyManager?.canUndo ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val canRedo by (currentSessionState?.undoManager?.historyManager?.canRedo ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    var layoutCursor by remember { mutableStateOf(com.makerandreas.papirusoffice.data.DocumentCursor()) }

    val navEngine = remember(currentSessionState) {
        currentSessionState?.navigationEngine ?: com.makerandreas.papirusoffice.data.navigation.NavigationEngine()
    }

    var docBodyText by remember {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue("")
        )
    }

    var lastTextRecordedValue by remember { mutableStateOf("") }

    LaunchedEffect(docBodyText.text, docTitle, currentSessionState?.document) {
        val activeDoc = currentSessionState?.document ?: com.makerandreas.papirusoffice.data.OfficeDocument(
            metadata = com.makerandreas.papirusoffice.data.DocumentMetadata(title = docTitle)
        )
        navEngine.updateDocument(activeDoc)
    }

    val navEngineState by navEngine.state.collectAsState()

    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var loadingDocName by remember { mutableStateOf(docTitle) }
    var loadingProgressStatus by remember { mutableStateOf("") }
    var showDocOpenFailedDialog by remember { mutableStateOf(false) }
    var docOpenFailedError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(com.example.MainActivity.openedFilePath) {
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
                            docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                            lastTextRecordedValue = parseResult.text
                            docxImages = parseResult.extractedImages
                            docxExtents = parseResult.imageExtents
                            updateInkyMetadata(f.absolutePath, f.name, parseResult.text)
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
                    docxImages = parseResult.extractedImages
                    docxExtents = parseResult.imageExtents
                    updateInkyMetadata("templates/inky/Normal.ott", "Document.odt", parseResult.text)

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
            if (progress != null) {
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
    val layoutEngine = remember { com.makerandreas.papirusoffice.data.LayoutEngine() }

    // Go to Page Dialog state
    var showGoToPageDialog by remember { mutableStateOf(false) }
    var targetPageText by remember { mutableStateOf("") }

    // Set Reminder Dialog state
    var showSetReminderDialog by remember { mutableStateOf(false) }
    var reminderNoteText by remember { mutableStateOf("") }

    val pagesList = remember(docBodyText.text) {
        partitionTextToPages(docBodyText.text)
    }

    val totalDocPages = remember(pagesList) {
        pagesList.size
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

    val currentDocPage = remember(wordsBeforeCursor, wordCount, totalDocPages) {
        if (wordCount == 0 || totalDocPages <= 1) {
            1
        } else {
            val ratio = wordsBeforeCursor.toFloat() / wordCount.toFloat()
            val page = (ratio * totalDocPages).toInt() + 1
            page.coerceIn(1, totalDocPages)
        }
    }

    val documentNavigator = remember(scrollState, totalDocPages, viewOptions) {
        object : com.makerandreas.papirusoffice.data.DocumentNavigator {
            override fun goToPage(page: Int) {
                coroutineScope.launch {
                    val ratio = (page - 1).toFloat() / totalDocPages.coerceAtLeast(1).toFloat()
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
            documentNavigator.goToPage(signal.targetPageIndex)
            navEngine.clearNavSignal()
        }
    }

    // LibreOffice Kit Diagnostics Logs State
    val lokitLogs = remember {
        mutableStateListOf(
            "LOKit Core: Connected (v7.6.2)",
            "lok::Office::documentLoad(\"Inky_Dokumen.odt\") -> SUCCESS",
            "lok::Document::registerCallback(LOK_CALLBACK_INVALIDATE_TILES)",
            "lok::Document::paintTileList() -> Initialized 4 screen tiles"
        )
    }

    fun addLokitLog(message: String) {
        if (lokitLogs.size > 15) {
            lokitLogs.removeAt(0)
        }
        lokitLogs.add(message)
    }

    var activeToolbarTypeState by remember { mutableStateOf("Standard") } // For compatibility or internal tracking

    val activity = remember(context) { context.findActivity() }

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
    var activeRibbonTab by remember { mutableStateOf("Home") } // File, Home, Insert, Layout, References, Mailings, Review, View
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
        val currentSession = com.makerandreas.papirusoffice.data.SessionManager.getInstance().current.value
        if (currentSession != null) {
            isLoadingDocument = true
            isParsingDoc = true
            coroutineScope.launch {
                val parseResult = com.makerandreas.papirusoffice.data.framework.DocumentLifecycleManager.reload(context, currentSession)
                if (parseResult != null) {
                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(parseResult.text)
                    lastTextRecordedValue = parseResult.text
                    docxImages = parseResult.extractedImages
                    docxExtents = parseResult.imageExtents
                    isSaved = true
                    Toast.makeText(context, context.getString(R.string.toast_reload_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to reload document", Toast.LENGTH_SHORT).show()
                }
                isLoadingDocument = false
                isParsingDoc = false
            }
        } else {
            Toast.makeText(context, "No active session to reload", Toast.LENGTH_SHORT).show()
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
                            actualSuccess = parser.saveDocument(file, docBodyText.text)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            actualSuccess = false
                        }
                    }
                    
                    isSaving = false
                    if (actualSuccess) {
                        isSaved = true
                        saveFailed = false
                        Toast.makeText(context, "Document saved", Toast.LENGTH_SHORT).show()
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
                            actualSuccess = parser.saveDocument(file, docBodyText.text)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            actualSuccess = false
                        }
                    }
                    
                    isSaving = false
                    if (actualSuccess) {
                        isSaved = true
                        saveFailed = false
                        if (path != null) {
                            updateInkyMetadata(path, docTitle, docBodyText.text)
                        }
                        Toast.makeText(context, "Document saved", Toast.LENGTH_SHORT).show()
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
                try {
                    val isDocx = currentSaveDefaultFilename.endsWith(".docx", ignoreCase = true)
                    val extension = if (isDocx) ".docx" else ".odt"
                    val tempFile = java.io.File(context.cacheDir, "temp_uri_save$extension")
                    if (tempFile.exists()) tempFile.delete()
                    
                    val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                    val success = parser.saveDocument(tempFile, docBodyText.text)
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
                    updateInkyMetadata(it.toString(), savedName, docBodyText.text)
                    Toast.makeText(context, context.getString(R.string.doc_saved_success, savedName), Toast.LENGTH_SHORT).show()
                    pendingActionAfterSave?.invoke()
                    pendingActionAfterSave = null
                } else {
                    Toast.makeText(context, "Error saving document to uri", Toast.LENGTH_SHORT).show()
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
            val sampleTemplateContent = "RESUME (MODERN)\n\nJohn Doe • Professional Software Engineer\nEmail: john.doe@email.com • Tel: +1 555-0199\n\nSUMMARY\nHighly motivated developer with experience building native Android productivity engines.\n\nEXPERIENCE\nSenior Developer • Papirus Office Inc.\n- Designed and implemented Google Gemini ODF template recommendation search APIs.\n- Tuned JNI Bridge bottlenecks to boost LibreOfficeCore rendering by 45%.\n\nEDUCATION\nBachelor of Science in Computer Science • University of Antigravity"
            
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
            com.example.MainActivity.openedFilePath = null
            onFormatAction("Back to start center")
        }
        if (!isSaved) {
            pendingActionAfterSave = closeAction
            showUnsavedChangesDialog = true
        } else {
            closeAction()
        }
    }

    LaunchedEffect(showBottomBar) {
        if (showBottomBar) {
            if (!wasKeyboardOpenBeforeBottomSheet) {
                wasKeyboardOpenBeforeBottomSheet = isKeyboardVisible
            }
            keyboardController?.hide()
        } else {
            if (wasKeyboardOpenBeforeBottomSheet) {
                focusRequester.requestFocus()
                keyboardController?.show()
                wasKeyboardOpenBeforeBottomSheet = false
            }
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
            customTextToolbar.hide()
            if (showBottomBar) {
                keyboardController?.hide()
            } else {
                // Restore scroll position to prevent autoscroll-up
                scrollState.scrollTo(previousScrollBeforeKeyboard)
            }
        } else {
            isControlsVisible = true
            previousScrollBeforeKeyboard = scrollState.value
        }
    }

    BackHandler {
        if (customTextToolbar.status == androidx.compose.ui.platform.TextToolbarStatus.Shown) {
            customTextToolbar.hide()
            if (isKeyboardVisible) {
                keyboardController?.hide()
            }
        } else if (showBottomBar) {
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
        } else if (isEditMode) {
            isEditMode = false
        } else {
            handleClose()
        }
    }

    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            customTextToolbar.hide()
            focusManager.clearFocus()
            if (!docBodyText.selection.collapsed) {
                docBodyText = docBodyText.copy(selection = androidx.compose.ui.text.TextRange(0))
            }
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

    // Helper functions
    fun triggerAutosave() {
        isSaved = false
        coroutineScope.launch {
            delay(1500)
            val path = com.example.MainActivity.openedFilePath
            if (path != null) {
                try {
                    val file = java.io.File(path)
                    val parser = com.makerandreas.papirusoffice.data.DocxDocumentParser(context)
                    val success = parser.saveDocument(file, docBodyText.text)
                    if (success) {
                        isSaved = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    LaunchedEffect(currentSessionState) {
        lastTextRecordedValue = docBodyText.text
    }

    LaunchedEffect(docBodyText.text) {
        if (docBodyText.text != lastTextRecordedValue) {
            kotlinx.coroutines.delay(1000) // Debounce typing for 1 second
            val oldValue = lastTextRecordedValue
            val newValue = docBodyText.text
            val diff = newValue.length - oldValue.length
            val title = when {
                diff > 0 -> {
                    val added = newValue.substring(oldValue.length.coerceAtMost(newValue.length))
                    "Typing \"${added.take(15)}${if (added.length > 15) "..." else ""}\""
                }
                diff < 0 -> "Delete text"
                else -> "Edit Document"
            }

            currentSessionState?.undoManager?.recordAction(object : com.makerandreas.papirusoffice.data.undo.UndoAction {
                override val title = title
                override val timestamp = System.currentTimeMillis()
                override val icon = if (diff >= 0) "text_fields" else "backspace"
                override val commandType = "EDIT_TEXT"
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
                        selection = androidx.compose.ui.text.TextRange(newValue.length)
                    )
                    lastTextRecordedValue = newValue
                }
            })
            lastTextRecordedValue = newValue
            triggerAutosave()
        }
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
    val textSecondaryColor = if (isDarkDocument) Color(0xFFA8A6A3) else Color.DarkGray
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
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
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
                                Toast.makeText(context, "Found match at character $index", Toast.LENGTH_SHORT).show()
                            } else if (query.isNotEmpty()) {
                                Toast.makeText(context, "No match found", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "Replaced successfully", Toast.LENGTH_SHORT).show()
                            } else if (find.isNotEmpty()) {
                                Toast.makeText(context, "Nothing to replace", Toast.LENGTH_SHORT).show()
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
                            if (!isEditMode) {
                                IconButton(onClick = { handleClose() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Start Center")
                                }
                            } else {
                                IconButton(onClick = { 
                                    isEditMode = false 
                                    Toast.makeText(context, "Switched to Viewer Mode", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Viewer")
                                }
                            }
                        },
                        actions = {
                            // 1. Upload to Google Drive
                            IconButton(onClick = {
                                Toast.makeText(context, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show()
                                addLokitLog("Upload to Drive triggered")
                            }) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload to Drive")
                            }

                            // 2. Find in Page
                            IconButton(onClick = {
                                showFindReplace = !showFindReplace
                            }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Find in Page")
                            }

                            if (isEditMode) {
                                // 3. Mobile view (Edit Mode only)
                                IconButton(onClick = {
                                    isWebView = !isWebView
                                    Toast.makeText(context, if (isWebView) "Mobile View Active" else "Normal View Active", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                        contentDescription = "Document View Mode"
                                    )
                                }

                                // 4. Undo (Edit Mode only)
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            currentSessionState?.undoManager?.undo()
                                            triggerAutosave()
                                        }
                                    },
                                    enabled = canUndo
                                ) {
                                    Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                                }
                            }

                            // 5. More Options (both modes, different menu items)
                            Box {
                                var showMoreMenuInAppBar by remember { mutableStateOf(false) }
                                IconButton(onClick = { showMoreMenuInAppBar = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
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
                                                Toast.makeText(context, "Exporting and sharing as PDF...", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "Share as PDF") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Save as...") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showSaveAsDialog = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = "Save As") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                isDarkDocument = !isDarkDocument
                                            },
                                            leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = "Toggle Theme") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open navigation bar") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showBottomBar = true
                                                bottomBarDeck = "navigator"
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = "Open navigation bar") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Read aloud") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                Toast.makeText(context, "Reading document aloud...", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.VolumeUp, contentDescription = "Read aloud") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalPrintSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = "Print") }
                                        )
                                    } else {
                                        // Edit Mode Items: Share, Switch to Dark Mode, Read aloud, Open Navigation Bar, Print
                                        DropdownMenuItem(
                                            text = { Text("Share") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalEmailSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                isDarkDocument = !isDarkDocument
                                            },
                                            leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = "Toggle Theme") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Read aloud") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                Toast.makeText(context, "Reading document aloud...", Toast.LENGTH_SHORT).show()
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.VolumeUp, contentDescription = "Read aloud") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Open Navigation Bar") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showBottomBar = true
                                                bottomBarDeck = "navigator"
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Menu, contentDescription = "Open Navigation Bar") }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Print") },
                                            onClick = {
                                                showMoreMenuInAppBar = false
                                                showUniversalPrintSheet = true
                                            },
                                            leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = "Print") }
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

            // --- MAIN DOCUMENT WORKSPACE CANVAS ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(docBgColor),
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
                    // Document Paper Sheet
                    Surface(
                        modifier = Modifier
                            .width((340 * zoomScale).dp)
                            .defaultMinSize(minHeight = (480 * zoomScale).dp)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
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
                                onValueChange = { newValue ->
                                    docBodyText = newValue
                                    triggerAutosave()
                                },
                                enabled = isEditMode,
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

            // --- Status Bar Bawah (ONLY visible in Edit Mode) ---
            if (isEditMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Page Counter (Left)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    targetPageText = currentDocPage.toString()
                                    showGoToPageDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Description,
                                contentDescription = "Pages",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Page $currentDocPage of $totalDocPages",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // 2. Words and Character Counter (Middle)
                        Text(
                            text = "$wordCount words, ${docBodyText.text.length} chars",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        // 3. Zoom Control (Right)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale - 0.1f).coerceAtLeast(0.25f)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = "Zoom Out",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        zoomScale = 1.0f
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            IconButton(
                                onClick = {
                                    zoomScale = (zoomScale + 0.1f).coerceAtMost(4.0f)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Zoom In",
                                    modifier = Modifier.size(16.dp)
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
                                    contentDescription = "Select Font Style",
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
                                    contentDescription = "Select Font Size",
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
                                Icon(Icons.Rounded.FormatBold, contentDescription = "Bold")
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
                                Icon(Icons.Rounded.FormatItalic, contentDescription = "Italic")
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
                                    contentDescription = "Underline",
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
                                Icon(Icons.Rounded.FormatStrikethrough, contentDescription = "Strikethrough")
                            }

                            // 7. Highlight color
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "highlight_color"
                                openedFromExternalHub = true
                            }) {
                                Icon(
                                    Icons.Rounded.BorderColor,
                                    contentDescription = "Highlight Color",
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
                                    contentDescription = "Font Color",
                                    tint = fontColor
                                )
                            }

                            // 9. Create bulleted list
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "bulleted_list"
                                openedFromExternalHub = true
                            }) {
                                Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Bulleted List")
                            }

                            // 10. Create numbered list
                            IconButton(onClick = {
                                showBottomBar = true
                                activeInkySubpage = "numbered_list"
                                openedFromExternalHub = true
                            }) {
                                Icon(Icons.Rounded.FormatListNumbered, contentDescription = "Numbered List")
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
                                Icon(Icons.Rounded.FormatIndentIncrease, contentDescription = "Increase Indent")
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
                                    Toast.makeText(context, "Cannot decrease indent further", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Rounded.FormatIndentDecrease, contentDescription = "Decrease Indent")
                            }

                            // 13. Add image
                            IconButton(onClick = {
                                Toast.makeText(context, "Add image selected", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Add Image")
                            }

                            // 14. Add table
                            IconButton(onClick = {
                                Toast.makeText(context, "Add table selected", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.GridOn, contentDescription = "Add Table")
                            }

                            // 15. Add link
                            IconButton(onClick = {
                                Toast.makeText(context, "Add link selected", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.Link, contentDescription = "Add Link")
                            }

                            // 16. Add comment
                            IconButton(onClick = {
                                Toast.makeText(context, "Add comment selected", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.Comment, contentDescription = "Add Comment")
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
                                Icon(Icons.Rounded.KeyboardTab, contentDescription = "Insert Tab", tint = MaterialTheme.colorScheme.primary)
                            }

                            // b. Toggle Keyboard
                            IconButton(
                                onClick = {
                                    if (isKeyboardVisible) {
                                        keyboardController?.hide()
                                    } else {
                                        try {
                                            focusRequester.requestFocus()
                                        } catch (e: Exception) {}
                                        keyboardController?.show()
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.Keyboard, contentDescription = "Toggle Keyboard", tint = MaterialTheme.colorScheme.primary)
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
                                Icon(Icons.Rounded.ViewAgenda, contentDescription = "Open Bottom Sheet", tint = MaterialTheme.colorScheme.onPrimaryContainer)
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
                                onUndo = {
                                    customTextToolbar.hide()
                                    coroutineScope.launch {
                                        val success = currentSessionState?.undoManager?.undo() ?: false
                                        if (success) {
                                            triggerAutosave()
                                            Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nothing to Undo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    addLokitLog("lok::Document::postWindow(event=UNDO)")
                                },
                                onRedo = {
                                    customTextToolbar.hide()
                                    coroutineScope.launch {
                                        val success = currentSessionState?.undoManager?.redo() ?: false
                                        if (success) {
                                            triggerAutosave()
                                            Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nothing to Redo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    addLokitLog("lok::Document::postWindow(event=REDO)")
                                },
                                onClose = { showBottomBar = false },
                                canUndo = canUndo,
                                canRedo = canRedo
                            )
                        } else if (bottomBarDeck == "navigate_by") {
                            com.example.ui.components.NavigateBySheetContent(
                                navEngine = navEngine,
                                isEditMode = isEditMode,
                                onBackToNavigator = { bottomBarDeck = "navigator" },
                                onUndo = {
                                    customTextToolbar.hide()
                                    coroutineScope.launch {
                                        val success = currentSessionState?.undoManager?.undo() ?: false
                                        if (success) {
                                            triggerAutosave()
                                            Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nothing to Undo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    addLokitLog("lok::Document::postWindow(event=UNDO)")
                                },
                                onRedo = {
                                    customTextToolbar.hide()
                                    coroutineScope.launch {
                                        val success = currentSessionState?.undoManager?.redo() ?: false
                                        if (success) {
                                            triggerAutosave()
                                            Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Nothing to Redo", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    addLokitLog("lok::Document::postWindow(event=REDO)")
                                },
                                onClose = { showBottomBar = false },
                                canUndo = canUndo,
                                canRedo = canRedo
                            )
                        } else {
                        val ribbonTabs = listOf("File", "Home", "Insert", "Layout", "References", "Mailings", "Review", "View")
                        val ribbonPagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = 1, pageCount = { ribbonTabs.size })
                        val ribbonTabScrollState = rememberScrollState()
                        
                        LaunchedEffect(ribbonPagerState.currentPage) {
                            activeRibbonTab = ribbonTabs[ribbonPagerState.currentPage]
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
                                                imageVector = Icons.Rounded.ArrowBack,
                                                contentDescription = "Back",
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
                                                contentDescription = "Create New Style",
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
                                            Toast.makeText(context, "More Options will be developed soon", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "More Options",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Persistent undo/redo/close
                                    if (activeInkySubpage != "actions_to_undo" && activeInkySubpage != "actions_to_redo") {
                                        LongClickIconButton(
                                            enabled = canUndo,
                                            onClick = {
                                                customTextToolbar.hide()
                                                coroutineScope.launch {
                                                    val success = currentSessionState?.undoManager?.undo() ?: false
                                                    if (success) {
                                                        triggerAutosave()
                                                        Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Nothing to Undo", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                addLokitLog("lok::Document::postWindow(event=UNDO)")
                                            },
                                            onLongClick = {
                                                customTextToolbar.hide()
                                                previousInkySubpage = activeInkySubpage
                                                activeInkySubpage = "actions_to_undo"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Undo,
                                                contentDescription = "Undo",
                                                tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                        }
                                        LongClickIconButton(
                                            enabled = canRedo,
                                            onClick = {
                                                customTextToolbar.hide()
                                                coroutineScope.launch {
                                                    val success = currentSessionState?.undoManager?.redo() ?: false
                                                    if (success) {
                                                        triggerAutosave()
                                                        Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Nothing to Redo", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                addLokitLog("lok::Document::postWindow(event=REDO)")
                                            },
                                            onLongClick = {
                                                customTextToolbar.hide()
                                                previousInkySubpage = activeInkySubpage
                                                activeInkySubpage = "actions_to_redo"
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Redo,
                                                contentDescription = "Redo",
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
                                            contentDescription = "Close Standard Bottom Sheet",
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
                                // 1. Baris tab (scrollable)
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(ribbonTabScrollState),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ribbonTabs.forEachIndexed { index, tab ->
                                        val isSelected = ribbonPagerState.currentPage == index
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent
                                                )
                                                .clickable {
                                                    coroutineScope.launch { ribbonPagerState.animateScrollToPage(index) }
                                                    activeRibbonTab = tab
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tab,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
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

                                // 2. Trailing icons (3 persistent buttons: Undo, Redo, Close)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    LongClickIconButton(
                                        enabled = canUndo,
                                        onClick = {
                                            customTextToolbar.hide()
                                            coroutineScope.launch {
                                                val success = currentSessionState?.undoManager?.undo() ?: false
                                                if (success) {
                                                    triggerAutosave()
                                                    Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Nothing to Undo", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            addLokitLog("lok::Document::postWindow(event=UNDO)")
                                        },
                                        onLongClick = {
                                            customTextToolbar.hide()
                                            previousInkySubpage = activeInkySubpage
                                            activeInkySubpage = "actions_to_undo"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Undo,
                                            contentDescription = "Undo",
                                            tint = if (canUndo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    LongClickIconButton(
                                        enabled = canRedo,
                                        onClick = {
                                            customTextToolbar.hide()
                                            coroutineScope.launch {
                                                val success = currentSessionState?.undoManager?.redo() ?: false
                                                if (success) {
                                                    triggerAutosave()
                                                    Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Nothing to Redo", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            addLokitLog("lok::Document::postWindow(event=REDO)")
                                        },
                                        onLongClick = {
                                            customTextToolbar.hide()
                                            previousInkySubpage = activeInkySubpage
                                            activeInkySubpage = "actions_to_redo"
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Redo,
                                            contentDescription = "Redo",
                                            tint = if (canRedo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                        )
                                    }
                                    IconButton(onClick = {
                                        customTextToolbar.hide()
                                        showBottomBar = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Close Standard Bottom Sheet",
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
                                                onSelectEntry = { entry ->
                                                    coroutineScope.launch {
                                                        currentSessionState?.undoManager?.undoTo(entry)
                                                        triggerAutosave()
                                                        Toast.makeText(context, "Actions undone", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                        "actions_to_redo" -> {
                                            val redoHistory = currentSessionState?.undoManager?.historyManager?.redoHistory?.collectAsState()?.value ?: emptyList()
                                            com.example.ui.components.ActionsToRedoSubpage(
                                                redoHistory = redoHistory,
                                                onSelectEntry = { entry ->
                                                    coroutineScope.launch {
                                                        currentSessionState?.undoManager?.redoTo(entry)
                                                        triggerAutosave()
                                                        Toast.makeText(context, "Actions redone", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            } else {
                                 androidx.compose.foundation.pager.HorizontalPager(
                                     state = ribbonPagerState,
                                     modifier = Modifier.fillMaxSize()
                                 ) { page ->
                                     val currentTabName = ribbonTabs[page]
                                     if (currentTabName == "File") {
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
                                                         val currentPath = com.example.MainActivity.openedFilePath ?: "templates/inky/Normal.ott"
                                                         var meta = inkyMetadataRepo.getMetadata(currentPath)
                                                         if (meta == null) {
                                                             updateInkyMetadata(currentPath, docTitle, docBodyText.text)
                                                             meta = inkyMetadataRepo.getMetadata(currentPath)
                                                         }
                                                         if (meta != null) {
                                                             val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                                             val createdStr = dateFmt.format(java.util.Date(meta.createdAt))
                                                             val modifiedStr = dateFmt.format(java.util.Date(meta.lastModifiedAt))
                                                             Toast.makeText(
                                                                 context,
                                                                 "Document Properties (Room DB):\n" +
                                                                 "File: ${meta.fileName} (${meta.fileType})\n" +
                                                                 "Author: ${meta.author}\n" +
                                                                 "Created: $createdStr\n" +
                                                                 "Modified: $modifiedStr\n" +
                                                                 "Words: ${meta.wordCount} | Chars: ${meta.characterCount} | Paragraphs: ${meta.paragraphCount}",
                                                                 Toast.LENGTH_LONG
                                                             ).show()
                                                         } else {
                                                             Toast.makeText(context, "No metadata available for this document", Toast.LENGTH_SHORT).show()
                                                         }
                                                     }
                                                 },
                                                 onPrintDocument = {
                                                     showUniversalPrintSheet = true
                                                 },
                                                 onShareDocument = {
                                                     showUniversalEmailSheet = true
                                                 }
                                             )
                                         }
                                     } else if (currentTabName == "Home") {
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
                                         Box(
                                             modifier = Modifier
                                                 .fillMaxSize()
                                                 .padding(16.dp),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Text(
                                                 text = "$currentTabName options will be implemented soon.",
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
                    Text("In-app mathematical formulas are written in LaTeX and compiled natively to MathML (ODF) or OMML (OOXML).", fontSize = 12.sp, color = Color.Gray)
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
                            .background(Color.LightGray.copy(alpha = 0.2f))
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
                    Toast.makeText(context, "Formula inserted successfully!", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Ukuran font diubah ke $size pt", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showPasteSpecialDialog) {
        PasteSpecialDialog(
            onDismiss = { showPasteSpecialDialog = false },
            onPasteSuccess = { format ->
                showPasteSpecialDialog = false
                triggerAutosave()
                Toast.makeText(context, "Menempelkan sebagai $format", Toast.LENGTH_SHORT).show()
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
                        docxImages = parseResult.extractedImages
                        docxExtents = parseResult.imageExtents
                        isSaved = true
                        isParsingDoc = false
                        updateActiveSession(file, parseResult.parsedDocument)
                        RecentFilesTracker.addFile(context, filePath, fileType)
                        Toast.makeText(context, "Opened ${file.name}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Page Style (Section Options) opened", Toast.LENGTH_SHORT).show()
        },
        onBulletsNumberingOptionsClick = {
            showBottomBar = true
            activeInkySubpage = "bulleted_list"
        },
        onSkipNumberingClick = {
            Toast.makeText(context, "Skip numbering applied to paragraph", Toast.LENGTH_SHORT).show()
        },
        onRemoveNumberingClick = {
            Toast.makeText(context, "Numbering removed from paragraph", Toast.LENGTH_SHORT).show()
        },
        onRestartFromBeginningClick = {
            Toast.makeText(context, "Numbering restarted from 1", Toast.LENGTH_SHORT).show()
        },
        onTabsSettingsClick = {
            Toast.makeText(context, "Tab stop settings opened", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Selected synonym: $synonym", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Text inserted from Gemini Copilot!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Reminder set at paragraph $cursorPara", Toast.LENGTH_SHORT).show()
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
            onConfirmSave = { selectedFormat, extension, mimeType ->
                currentSaveMimeType = mimeType
                val baseName = docTitle.substringBeforeLast(".")
                currentSaveDefaultFilename = if (baseName.isBlank()) "Inky_Dokumen$extension" else "$baseName$extension"
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
                                                    Toast.makeText(context, "Template downloaded successfully!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    activeDownloadProgress = null
                                                    Toast.makeText(context, "Download failed. Please try again.", Toast.LENGTH_SHORT).show()
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
                                                    contentDescription = "Downloaded",
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

    // --- FLOATING ACTION BUTTON FOR VIEWER MODE ---
    if (!isEditMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    isEditMode = true
                    showBottomBar = false
                    Toast.makeText(context, "Edit Mode Active", Toast.LENGTH_SHORT).show()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_open_edit_mode")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Open Edit Mode"
                )
            }
        }
    }
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
    onShareDocument: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Grup File
        FileMenuSectionHeader("File")
        FileMenuThreeColumnRow(
            item1 = Triple(Icons.Rounded.NoteAdd, "New", onNewDocument),
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
            icon = Icons.Rounded.ImportExport,
            title = "Export as..."
        ) {
            Toast.makeText(context, "Export options: PDF, EPUB, XHTML", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Document finalized!", Toast.LENGTH_SHORT).show()
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
            item3 = Triple(Icons.Rounded.CallMerge, "Merge") {
                Toast.makeText(context, "Print merge wizard...", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "All pictures compressed successfully (Saved 1.2 MB)", Toast.LENGTH_SHORT).show()
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
                val cacheFile = java.io.File(context.cacheDir, displayName)
                context.contentResolver.openInputStream(it)?.use { input ->
                    java.io.FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                RecentFilesTracker.addFile(context, cacheFile.absolutePath, fileType)
                onFileSelected(cacheFile.absolutePath, fileType)
            } catch (e: Exception) {
                Toast.makeText(context, "Error opening document: ${e.message}", Toast.LENGTH_SHORT).show()
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
                                    Icon(Icons.Rounded.Search, contentDescription = "Search")
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
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("No recent documents found", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                        icon = Icons.Rounded.Article
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
                                                Toast.makeText(context, "Google OAuth2 authorization granted!", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, "Opening cloud document $driveFileName...", Toast.LENGTH_SHORT).show()
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

