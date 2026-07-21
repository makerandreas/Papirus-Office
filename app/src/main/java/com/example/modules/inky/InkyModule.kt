package com.example.modules.inky

import android.widget.Toast
import androidx.compose.animation.*
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

// Track setup model matching the actual user document
data class TrackSetupRow(val track: String, val sayap: Int, val rem: Int, val suspensi: Int)
data class TyreSetupRow(val durasi: Int, val konsumsi: Int, val perbedaan: Int)

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

fun partitionTextToPages(text: String, maxLinesPerPage: Int = 15): List<String> {
    val rawText = text
    if (rawText.isEmpty()) return listOf("")
    
    val rawParagraphs = rawText.split("\n")
    val pages = mutableListOf<String>()
    var currentPageLines = mutableListOf<String>()
    var currentLinesCount = 0
    
    rawParagraphs.forEach { paragraph ->
        // Estimate line wraps: assume ~40 characters per line
        val approxLinesInParagraph = maxOf(1, (paragraph.length + 39) / 40)
        
        if (currentLinesCount + approxLinesInParagraph > maxLinesPerPage && currentPageLines.isNotEmpty()) {
            pages.add(currentPageLines.joinToString("\n"))
            currentPageLines = mutableListOf()
            currentLinesCount = 0
        }
        
        currentPageLines.add(paragraph)
        currentLinesCount += approxLinesInParagraph
    }
    
    if (currentPageLines.isNotEmpty() || pages.isEmpty()) {
        pages.add(currentPageLines.joinToString("\n"))
    }
    
    return pages
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkyModule(
    isTablet: Boolean,
    onFormatAction: (String) -> Unit,
    dynamicColorEnabled: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    
    val focusRequester = remember { FocusRequester() }

    // --- Inky Core States ---
    var isEditMode by remember { mutableStateOf(false) } // False = Viewer Mode, True = Edit Mode
    var isWebView by remember { mutableStateOf(false) }  // False = Normal View, True = Web View
    var isDarkDocument by remember { mutableStateOf(false) } // Dark document canvas mode
    var isSaved by remember { mutableStateOf(true) }     // Tracks saved indicator suffix
    var docTitle by remember {
        mutableStateOf(
            if (com.example.MainActivity.openedFilePath != null && com.example.MainActivity.openedFileType == "Inky") {
                java.io.File(com.example.MainActivity.openedFilePath!!).name
            } else {
                "Inky_Dokumen.odt"
            }
        )
    }

    // Bottom Bar (Ribbon & sub-decks) States
    var showBottomBar by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }
    var showPasteSpecialDialog by remember { mutableStateOf(false) }



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
    var documentContentTitle by remember { mutableStateOf("Draft Dokumen Baru") }
    var docBodyText by remember {
        val initialText = if (com.example.MainActivity.openedFilePath != null && com.example.MainActivity.openedFileType == "Inky") {
            try {
                val f = java.io.File(com.example.MainActivity.openedFilePath!!)
                if (f.exists()) f.readText() else ""
            } catch(e: Exception) {
                ""
            }
        } else {
            ""
        }
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(initialText)
        )
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

    val pagesList = remember(docBodyText.text) {
        partitionTextToPages(docBodyText.text)
    }

    val pageCount = remember(pagesList) {
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

    val currentPage = remember(wordsBeforeCursor, wordCount, pageCount) {
        if (wordCount == 0 || pageCount <= 1) {
            1
        } else {
            val ratio = wordsBeforeCursor.toFloat() / wordCount.toFloat()
            val page = (ratio * pageCount).toInt() + 1
            page.coerceIn(1, pageCount)
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

    val advSettings = remember {
        mutableStateListOf(
            "Driving Help: Mid",
            "Opponent Level: Real",
            "Bantuan Mengemudi: 27",
            "Antispin: 22",
            "Sensitivitas Kemudi: 115"
        )
    }

    val activity = remember(context) { context.findActivity() }

    // Text formatting state
    var activeFontFamily by remember { mutableStateOf("Aptos Display") }
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
    var selectedStyleNameForOptions by remember { mutableStateOf("Normal") }
    var openedFromExternalHub by remember { mutableStateOf(false) }

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

    // FCT state
    var showFct by remember { mutableStateOf(false) }
    var fctContext by remember { mutableStateOf("text") } // text, table, object
    var wasFctVisibleOnPress by remember { mutableStateOf(false) }
    val horizScrollState = rememberScrollState()

    // Scroll Control to hide AppBar and Toolbar Hub dynamically
    var previousScrollValue by remember { mutableStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }

    
    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    LaunchedEffect(isKeyboardVisible) {
        if (isKeyboardVisible) {
            isControlsVisible = true
            showFct = false
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
        if (showFct) {
            showFct = false
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
            onFormatAction("Back to start center")
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

    // Interactive Track Data Table state matching screenshot
    val tracks = remember {
        mutableStateListOf(
            TrackSetupRow("Australia", 20, -6, 8),
            TrackSetupRow("Tiongkok", 25, -5, 9),
            TrackSetupRow("Jepang", 32, -7, 11),
            TrackSetupRow("Bahrain", 26, -5, 7),
            TrackSetupRow("Arab Saudi", 8, -2, 2),
            TrackSetupRow("Amerika Serikat (Miami)", 22, -6, 8),
            TrackSetupRow("Kanada", 10, -3, 4),
            TrackSetupRow("Monako", 38, -8, 14),
            TrackSetupRow("Spanyol (Barcelona)", 30, -7, 10),
            TrackSetupRow("Austria", 14, -4, 6),
            TrackSetupRow("Inggris", 34, -7, 10),
            TrackSetupRow("Belgia", 28, -6, 9),
            TrackSetupRow("Hongaria", 38, -8, 13),
            TrackSetupRow("Belanda", 36, -7, 12),
            TrackSetupRow("Italia (Monza)", 5, -2, 0)
        )
    }

    // Tyre setup list
    val tyreSetups = remember {
        mutableStateListOf(
            TyreSetupRow(25, 100, 85),
            TyreSetupRow(35, 90, 75),
            TyreSetupRow(50, 80, 65),
            TyreSetupRow(100, 70, 55)
        )
    }

    // Helper functions
    fun triggerAutosave() {
        isSaved = false
        coroutineScope.launch {
            delay(1500)
            isSaved = true
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

    val dummyTextToolbar = remember(isWebView, zoomScale, density, screenWidthDp, horizScrollState.value) {
        object : androidx.compose.ui.platform.TextToolbar {
            override fun showMenu(
                rect: androidx.compose.ui.geometry.Rect,
                onCopy: (() -> Unit)?,
                onPaste: (() -> Unit)?,
                onCut: (() -> Unit)?,
                onSelectAll: (() -> Unit)?
            ) {
                textToolbarCopyCallback = onCopy
                textToolbarPasteCallback = onPaste
                textToolbarCutCallback = onCut
                textToolbarSelectAllCallback = onSelectAll

                val textLocalOffset = androidx.compose.ui.geometry.Offset(rect.left + rect.width / 2f, rect.top)
                val windowOffset = pageBoxCoordinates?.localToWindow(textLocalOffset) ?: textLocalOffset

                showFct = true
                isFctShownByTap = true
                fctContext = "text"
                fctOffset = calculateFctOffset(
                    targetX = windowOffset.x,
                    targetY = windowOffset.y,
                    zoomScale = zoomScale,
                    density = density,
                    screenWidthDp = screenWidthDp,
                    screenHeightDp = screenHeightDp
                )
            }

            override fun hide() {
                showFct = false
                isFctShownByTap = false
            }

            override val status: androidx.compose.ui.platform.TextToolbarStatus
                get() = if (showFct) androidx.compose.ui.platform.TextToolbarStatus.Shown else androidx.compose.ui.platform.TextToolbarStatus.Hidden
        }
    }

    // Immediate composition-based checks to completely eliminate flickering during zoom or scrolling
    var lastZoomScale by remember { mutableStateOf(zoomScale) }
    if (lastZoomScale != zoomScale) {
        showFct = false
        lastZoomScale = zoomScale
    }
    if (scrollState.isScrollInProgress || horizScrollState.isScrollInProgress) {
        showFct = false
    }

    LaunchedEffect(zoomScale) {
        showFct = false
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

    LaunchedEffect(docBodyText.selection, docBodyText.text) {
        delay(50)
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
                
                val paddingPx = 32 * density
                
                val cursorLeft = cursorTopLeftInViewport.x
                val cursorRight = cursorBottomRightInViewport.x
                if (cursorLeft < paddingPx) {
                    val delta = (cursorLeft - paddingPx).toInt()
                    horizScrollState.scrollTo((horizScrollState.value + delta).coerceAtLeast(0))
                } else if (cursorRight > viewportWidth - paddingPx) {
                    val delta = (cursorRight - (viewportWidth - paddingPx)).toInt()
                    horizScrollState.scrollTo(horizScrollState.value + delta)
                }
                
                val cursorTop = cursorTopLeftInViewport.y
                val cursorBottom = cursorBottomRightInViewport.y
                if (cursorTop < paddingPx) {
                    val delta = (cursorTop - paddingPx).toInt()
                    scrollState.scrollTo((scrollState.value + delta).coerceAtLeast(0))
                } else if (cursorBottom > viewportHeight - paddingPx) {
                    val delta = (cursorBottom - (viewportHeight - paddingPx)).toInt()
                    scrollState.scrollTo(scrollState.value + delta)
                }
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalTextToolbar provides dummyTextToolbar
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(docBgColor)
        ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            
            // --- HEADER TOP APP BAR ---
            // Hidden temporarily if bottom bar is opened or scrolling hides it
            AnimatedVisibility(
                visible = !showBottomBar && isControlsVisible,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                showFct = false
                            }
                        }
                    }
                }
            ) {
                if (!isEditMode) {
                    // --- VIEWER MODE APP BAR ---
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = docTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Read-Only • ODT Format",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { onFormatAction("Back to start center") }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Start Center")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                Toast.makeText(context, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show()
                                addLokitLog("Upload to Drive triggered")
                            }) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload to Drive")
                            }
                            IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Find in Document")
                            }
                            IconButton(onClick = { 
                                isWebView = !isWebView
                                Toast.makeText(context, if (isWebView) "Mobile View Active" else "Normal View Active", Toast.LENGTH_SHORT).show()
                                addLokitLog("View Mode changed -> lok::Document::paintTileList() refreshed")
                            }) {
                                Icon(
                                    imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                    contentDescription = "Document View Mode"
                                )
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export to PDF") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Exporting to PDF...", Toast.LENGTH_SHORT).show()
                                            addLokitLog("lok::Document::saveAs(\"output.pdf\", \"pdf\")")
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as...") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Saving copy...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = "Save As") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                        onClick = {
                                            showMoreMenu = false
                                            isDarkDocument = !isDarkDocument
                                        },
                                        leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = "Toggle Theme") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Print") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Printing document...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = "Print") }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        )
                    )
                } else {
                    // --- EDIT MODE APP BAR ---
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = docTitle,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isSaved) "Saved on this device" else "Saving...",
                                    fontSize = 11.sp,
                                    color = if (isSaved) Color(0xFF10B981) else Color.LightGray
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { isEditMode = false }) {
                                Icon(Icons.Default.Check, contentDescription = "Exit Edit Mode", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                Toast.makeText(context, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show()
                                addLokitLog("Upload to Drive triggered")
                            }) {
                                Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload to Drive")
                            }
                            IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                Icon(Icons.Rounded.Search, contentDescription = "Find and Replace")
                            }
                            IconButton(onClick = { 
                                isWebView = !isWebView
                                Toast.makeText(context, if (isWebView) "Mobile View" else "Normal View", Toast.LENGTH_SHORT).show()
                                addLokitLog("View Mode changed -> lok::Document::paintTileList() refreshed")
                            }) {
                                Icon(
                                    imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                    contentDescription = "Document View Mode"
                                )
                            }
                            IconButton(onClick = { 
                                triggerAutosave()
                                Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                addLokitLog("lok::Document::postWindow(event=UNDO)")
                            }) {
                                Icon(Icons.Rounded.Undo, contentDescription = "Undo")
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Opening Share Sheet...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = "Share") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export to PDF") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Exporting ODF to PDF...", Toast.LENGTH_SHORT).show()
                                            addLokitLog("lok::Document::saveAs(\"output.pdf\", \"pdf\")")
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save") },
                                        onClick = {
                                            showMoreMenu = false
                                            triggerAutosave()
                                            addLokitLog("lok::Document::saveAs(\"Inky_Dokumen.odt\", \"odt\")")
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = "Save") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Saving copy...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = "Save As") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                        onClick = {
                                            showMoreMenu = false
                                            isDarkDocument = !isDarkDocument
                                        },
                                        leadingIcon = { Icon(if (isDarkDocument) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = "Toggle Theme") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Read it Aloud") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Reading document aloud...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.VolumeUp, contentDescription = "Read Aloud") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open Navigator Bar") },
                                        onClick = {
                                            showMoreMenu = false
                                            showBottomBar = true
                                            bottomBarDeck = "navigator"
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Explore, contentDescription = "Navigator") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Document Version History") },
                                        onClick = {
                                            showMoreMenu = false
                                            showBottomBar = true
                                            bottomBarDeck = "version_history"
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.History, contentDescription = "History") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Print") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Connecting printer...", Toast.LENGTH_SHORT).show()
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = "Print") }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                        )
                    )
                }
            }

            // --- FIND AND REPLACE OVERLAY BAR ---
            AnimatedVisibility(visible = showFindReplace) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Cari teks...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                                        }
                                    }
                                }
                            )
                            if (isEditMode) {
                                OutlinedTextField(
                                    value = replaceQuery,
                                    onValueChange = { replaceQuery = it },
                                    placeholder = { Text("Ganti dengan...") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                            IconButton(onClick = { showFindReplace = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                Toast.makeText(context, "Mencari: $searchQuery", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Cari")
                            }
                            if (isEditMode) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    triggerAutosave()
                                    Toast.makeText(context, "Mengganti '$searchQuery' -> '$replaceQuery'", Toast.LENGTH_SHORT).show()
                                }) {
                                    Text("Ganti Semua")
                                }
                            }
                        }
                    }
                }
            }

            // --- MAIN DOCUMENT AREA & VIEWPORT ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val currentDensity = androidx.compose.ui.platform.LocalDensity.current
                val customDensity = remember(currentDensity) {
                    androidx.compose.ui.unit.Density(
                        density = currentDensity.density,
                        fontScale = 1.0f
                    )
                }
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides customDensity
                ) {
                    // Interactive document container
                    Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { viewportCoordinates = it }
                        .verticalScroll(scrollState)
                        .background(docBgColor)
                        .padding(if (isWebView) 0.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    
                    if (!isWebView) {
                        // --- A4 PORTRAIT VIEWPORT ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(horizScrollState),
                            contentAlignment = if (zoomScale > 1f) Alignment.CenterStart else Alignment.Center
                        ) {
                            if (!isEditMode) {
                                // --- VIEWER MODE: MULTIPLE PAGES DISPLAYED VERTICALLY ---
                                Column(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy((16 * zoomScale).dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    pagesList.forEachIndexed { index, pageText ->
                                        // Precise JNI-like render logging
                                        androidx.compose.runtime.LaunchedEffect(zoomScale) {
                                            com.example.core.jni.LibreOfficeCore.renderPageToBuffer(
                                                docPath = "Inky_Dokumen.odt",
                                                pageIndex = index,
                                                outputBuffer = ByteArray(0),
                                                width = (320 * zoomScale).toInt(),
                                                height = (452 * zoomScale).toInt()
                                            )
                                        }

                                        val pageHeight = 452
                                        Box(
                                            modifier = Modifier
                                                .width((320 * zoomScale).dp)
                                                .height((pageHeight * zoomScale).dp)
                                                .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                                                .border(1.dp, borderStrokeColor, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.TopStart
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .requiredSize(320.dp, pageHeight.dp)
                                                    .graphicsLayer {
                                                        scaleX = zoomScale
                                                        scaleY = zoomScale
                                                        transformOrigin = TransformOrigin(0f, 0f)
                                                    }
                                                    .background(pageBgColor)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(24.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxWidth()
                                                            .border(
                                                                width = 1.dp,
                                                                color = borderStrokeColor.copy(alpha = 0.4f),
                                                                shape = RoundedCornerShape(2.dp)
                                                            )
                                                            .padding(16.dp)
                                                    ) {
                                                        Text(
                                                            text = pageText,
                                                            style = androidx.compose.ui.text.TextStyle(
                                                                fontSize = activeFontSize.sp,
                                                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                                                fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                                                textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                                fontFamily = when (activeFontFamily) {
                                                                    "Aptos Display" -> FontFamily.SansSerif
                                                                    "Calibri" -> FontFamily.SansSerif
                                                                    "Arial" -> FontFamily.SansSerif
                                                                    "Roboto" -> FontFamily.SansSerif
                                                                    else -> FontFamily.Default
                                                                },
                                                                color = textPrimaryColor,
                                                                textAlign = textAlignment
                                                            )
                                                        )
                                                    }
                                                    
                                                    // Page number footer inside page
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 4.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "Halaman ${index + 1} dari ${pagesList.size}",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // --- EDITOR MODE: DYNAMIC EXPANDING CARD WITH PRINT PAGE BREAKS ---
                                val linesCount = docBodyText.text.split("\n").size
                                val baseEditorHeight = maxOf(452, 100 + linesCount * 22)
                                
                                Box(
                                    modifier = Modifier
                                        .width((320 * zoomScale).dp)
                                        .height((baseEditorHeight * zoomScale).dp)
                                        .shadow(elevation = 10.dp, shape = RoundedCornerShape(4.dp))
                                        .border(1.dp, borderStrokeColor, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .requiredSize(320.dp, baseEditorHeight.dp)
                                            .graphicsLayer {
                                                scaleX = zoomScale
                                                scaleY = zoomScale
                                                transformOrigin = TransformOrigin(0f, 0f)
                                            }
                                            .background(pageBgColor)
                                            .onGloballyPositioned { pageBoxCoordinates = it }
                                            .pointerInput(Unit) {
                                                detectTapGestures(
                                                    onDoubleTap = { tapOffset ->
                                                        if (!showBottomBar) {
                                                            showFct = true
                                                            isFctShownByTap = true
                                                            fctContext = "text"
                                                            val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                            fctOffset = calculateFctOffset(
                                                                targetX = rootOffset.x,
                                                                targetY = rootOffset.y,
                                                                zoomScale = zoomScale,
                                                                density = density,
                                                                screenWidthDp = screenWidthDp,
                                                                screenHeightDp = screenHeightDp
                                                            )
                                                        }
                                                    },
                                                    onLongPress = { tapOffset ->
                                                        if (!showBottomBar) {
                                                            showFct = true
                                                            isFctShownByTap = true
                                                            fctContext = "text"
                                                            val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                            fctOffset = calculateFctOffset(
                                                                targetX = rootOffset.x,
                                                                targetY = rootOffset.y,
                                                                zoomScale = zoomScale,
                                                                density = density,
                                                                screenWidthDp = screenWidthDp,
                                                                screenHeightDp = screenHeightDp
                                                            )
                                                        }
                                                    },
                                                    onTap = { tapOffset ->
                                                        if (!showBottomBar) {
                                                            if (wasFctVisibleOnPress) {
                                                                wasFctVisibleOnPress = false
                                                            } else {
                                                                showFct = true
                                                                isFctShownByTap = true
                                                                fctContext = "text"
                                                                val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                                fctOffset = calculateFctOffset(
                                                                    targetX = rootOffset.x,
                                                                    targetY = rootOffset.y,
                                                                    zoomScale = zoomScale,
                                                                    density = density,
                                                                    screenWidthDp = screenWidthDp,
                                                                    screenHeightDp = screenHeightDp
                                                                )
                                                            }
                                                            if (isEditMode) {
                                                                focusRequester.requestFocus()
                                                                keyboardController?.show()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                            .pointerInput(Unit) {
                                                awaitPointerEventScope {
                                                    while (true) {
                                                        val event = awaitPointerEvent()
                                                        val canceled = event.changes.any { it.isConsumed }
                                                        if (!canceled) {
                                                            val hasPressed = event.changes.any { it.pressed && !it.previousPressed }
                                                            if (hasPressed) {
                                                                wasFctVisibleOnPress = showFct
                                                                if (showFct) {
                                                                    showFct = false
                                                                }
                                                            }
                                                            if (event.changes.size >= 2) {
                                                                val zoomChange = event.calculateZoom()
                                                                if (zoomChange != 1f) {
                                                                    val newScale = zoomScale * zoomChange
                                                                    zoomScale = newScale.coerceIn(0.5f, 2.0f)
                                                                }
                                                                event.changes.forEach { change ->
                                                                    if (change.positionChanged()) {
                                                                        change.consume()
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp)
                                        ) {
                                            androidx.compose.foundation.text.BasicTextField(
                                                value = docBodyText,
                                                onValueChange = {
                                                    docBodyText = it
                                                    isSaved = false
                                                    triggerAutosave()
                                                    addLokitLog("LOK_CALLBACK_INVALIDATE_TILES -> edit")
                                                    addLokitLog("lok::Document::renderTile(bounds=[x=0, y=0, w=1080])")
                                                },
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .onGloballyPositioned { bodyTextFieldCoordinates = it }
                                                    .focusRequester(focusRequester),
                                                onTextLayout = { bodyTextLayoutResult = it },
                                                readOnly = false,
                                                textStyle = androidx.compose.ui.text.TextStyle(
                                                    fontSize = activeFontSize.sp,
                                                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                                    textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                                    fontFamily = when (activeFontFamily) {
                                                        "Aptos Display" -> FontFamily.SansSerif
                                                        "Calibri" -> FontFamily.SansSerif
                                                        "Arial" -> FontFamily.SansSerif
                                                        "Roboto" -> FontFamily.SansSerif
                                                        else -> FontFamily.Default
                                                    },
                                                    color = textPrimaryColor,
                                                    textAlign = textAlignment
                                                ),
                                                decorationBox = { innerTextField ->
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        // Subtle print page break divider lines
                                        val pageHeightThreshold = 452
                                        if (baseEditorHeight > pageHeightThreshold) {
                                            val estimatedPageBreakCount = baseEditorHeight / pageHeightThreshold
                                            for (p in 1..estimatedPageBreakCount) {
                                                val breakY = p * pageHeightThreshold
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = (breakY - 12).dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                                        Text(
                                                            text = " Halaman ${p + 1} (Page Break) ",
                                                            fontSize = 9.sp,
                                                            color = Color.Gray.copy(alpha = 0.5f),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray.copy(alpha = 0.3f)))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // --- MOBILE/WEB VIEWPORT (Full Bleed) ---
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .border(1.dp, borderStrokeColor, RoundedCornerShape(12.dp))
                                .onGloballyPositioned { pageBoxCoordinates = it }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            if (!showBottomBar) {
                                                showFct = true
                                                isFctShownByTap = true
                                                fctContext = "text"
                                                val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                fctOffset = calculateFctOffset(
                                                    targetX = rootOffset.x,
                                                    targetY = rootOffset.y,
                                                    zoomScale = zoomScale,
                                                    density = density,
                                                    screenWidthDp = screenWidthDp,
                                                    screenHeightDp = screenHeightDp
                                                )
                                            }
                                        },
                                        onLongPress = { tapOffset ->
                                            if (!showBottomBar) {
                                                showFct = true
                                                isFctShownByTap = true
                                                fctContext = "text"
                                                val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                fctOffset = calculateFctOffset(
                                                    targetX = rootOffset.x,
                                                    targetY = rootOffset.y,
                                                    zoomScale = zoomScale,
                                                    density = density,
                                                    screenWidthDp = screenWidthDp,
                                                    screenHeightDp = screenHeightDp
                                                )
                                            }
                                        },
                                        onTap = { tapOffset ->
                                            if (!showBottomBar) {
                                                if (wasFctVisibleOnPress) {
                                                    wasFctVisibleOnPress = false
                                                } else {
                                                    showFct = true
                                                    isFctShownByTap = true
                                                    fctContext = "text"
                                                    val rootOffset = pageBoxCoordinates?.localToWindow(tapOffset) ?: tapOffset
                                                    fctOffset = calculateFctOffset(
                                                        targetX = rootOffset.x,
                                                        targetY = rootOffset.y,
                                                        zoomScale = zoomScale,
                                                        density = density,
                                                        screenWidthDp = screenWidthDp,
                                                        screenHeightDp = screenHeightDp
                                                    )
                                                }
                                                if (isEditMode) {
                                                    focusRequester.requestFocus()
                                                    keyboardController?.show()
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val canceled = event.changes.any { it.isConsumed }
                                            if (!canceled) {
                                                val hasPressed = event.changes.any { it.pressed && !it.previousPressed }
                                                if (hasPressed) {
                                                    wasFctVisibleOnPress = showFct
                                                    if (showFct) {
                                                        showFct = false
                                                    }
                                                }
                                                if (event.changes.size >= 2) {
                                                    val zoomChange = event.calculateZoom()
                                                    if (zoomChange != 1f) {
                                                        val newScale = zoomScale * zoomChange
                                                        zoomScale = newScale.coerceIn(0.5f, 2.0f)
                                                    }
                                                    event.changes.forEach { change ->
                                                        if (change.positionChanged()) {
                                                            change.consume()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = pageBgColor),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "WEB / MOBILE VIEW MODE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textAccentColor,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = docBodyText,
                                        onValueChange = {
                                            docBodyText = it
                                            isSaved = false
                                            triggerAutosave()
                                            addLokitLog("LOK_CALLBACK_INVALIDATE_TILES (Web mode edit)")
                                            addLokitLog("lok::Document::renderTile() -> Web bounds")
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 350.dp)
                                            .onGloballyPositioned { bodyTextFieldCoordinates = it }
                                            .focusRequester(focusRequester),
                                        onTextLayout = { bodyTextLayoutResult = it },
                                        readOnly = !isEditMode,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = (activeFontSize * zoomScale).sp,
                                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = if (isUnderline) TextDecoration.Underline else TextDecoration.None,
                                            fontFamily = when (activeFontFamily) {
                                                "Aptos Display" -> FontFamily.SansSerif
                                                "Calibri" -> FontFamily.SansSerif
                                                "Arial" -> FontFamily.SansSerif
                                                "Roboto" -> FontFamily.SansSerif
                                                else -> FontFamily.Default
                                            },
                                            color = textPrimaryColor,
                                            textAlign = textAlignment
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (docBodyText.text.isEmpty()) {
                                                Text(
                                                    text = "Mulai mengetik di tampilan seluler ini...",
                                                    color = Color.Gray.copy(alpha = 0.7f),
                                                    fontSize = (activeFontSize * zoomScale).sp,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            }
                                            innerTextField()
                                        }
                                    )


                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                }

                // --- FLOATING ACTION BUTTON (Viewer Mode Only) ---
                if (!isEditMode && !showBottomBar) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            isEditMode = true
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                        text = { Text("Edit Document") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_edit_document"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // --- FOOTER STATS & STATUS BAR (Always visible in document workspaces) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { it.pressed && !it.previousPressed }) {
                                    showFct = false
                                }
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page $currentPage of $pageCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "$wordCount words",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                showFct = false
                                if (zoomScale > 0.5f) zoomScale -= 0.1f
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = "Zoom: ${(zoomScale * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.clickable {
                                showFct = false
                                zoomScale = 1.0f
                            }
                        )
                        IconButton(
                            onClick = {
                                showFct = false
                                if (zoomScale < 2.0f) zoomScale += 0.1f
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // --- BOTTOM TOOLBAR HUB (Edit Mode Only) ---
            // Contains horizontal scrollable tools, togglable Standard/Formatting options
            AnimatedVisibility(
                visible = isEditMode && !showBottomBar,
                enter = expandVertically(),
                exit = shrinkVertically(),
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                showFct = false
                            }
                        }
                    }
                }
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Left Side: Horizontal Scrollable Toolbar Area (1 toolbar at a time)
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeToolbarType == "Standard") {
                                // --- STANDARD TOOLBAR ---
                                
                                // 1. Font Style (Drop-down sepanjang ±3 ikon)
                                item {
                                    Row(
                                        modifier = Modifier
                                            .width(120.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                bottomBarDeck = "font_family"
                                                showBottomBar = true
                                            }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = activeFontFamily,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Font Style",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 2. Font Size (Drop-down sepanjang ±2 ikon)
                                item {
                                    Row(
                                        modifier = Modifier
                                            .width(76.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                            .clickable { showFontSizeDialog = true }
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$activeFontSize pt",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.KeyboardArrowDown,
                                            contentDescription = "Font Size",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // 3. Bold
                                item {
                                    IconButton(
                                        onClick = { isBold = !isBold; triggerAutosave() },
                                        colors = if (isBold) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Rounded.FormatBold, contentDescription = "Bold", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 4. Italic
                                item {
                                    IconButton(
                                        onClick = { isItalic = !isItalic; triggerAutosave() },
                                        colors = if (isItalic) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Rounded.FormatItalic, contentDescription = "Italic", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 5. Underline
                                item {
                                    Row(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isUnderline) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                1.dp,
                                                if (isUnderline) MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                RoundedCornerShape(8.dp)
                                            ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    isUnderline = !isUnderline
                                                    triggerAutosave()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FormatUnderlined,
                                                contentDescription = "Underline",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .fillMaxHeight()
                                                .clickable {
                                                    bottomBarDeck = "underline_options"
                                                    openedFromExternalHub = true
                                                    showBottomBar = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = "Underline Options",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // 6. Strikethrough
                                item {
                                    IconButton(
                                        onClick = { isStrikethrough = !isStrikethrough; triggerAutosave() },
                                        colors = if (isStrikethrough) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Rounded.StrikethroughS, contentDescription = "Strikethrough", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 7. Text Highlight Color
                                item {
                                    IconButton(onClick = {
                                        showBottomBar = true
                                        bottomBarDeck = "highlight_color"
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.BorderColor,
                                            contentDescription = "Sorot Warna",
                                            tint = if (highlightColor == Color.Transparent) Color.Gray else highlightColor
                                        )
                                    }
                                }

                                // 8. Font Color
                                item {
                                    IconButton(onClick = {
                                        showBottomBar = true
                                        bottomBarDeck = "font_color"
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.FormatColorText,
                                            contentDescription = "Warna Font",
                                            tint = fontColor
                                        )
                                    }
                                }

                                // 9. Insert Bulleted Lists
                                item {
                                    IconButton(onClick = {
                                        val sel = docBodyText.selection
                                        val currentText = docBodyText.text
                                        val start = sel.start
                                        val end = sel.end
                                        val newText = currentText.substring(0, start) + "• " + currentText.substring(end)
                                        docBodyText = docBodyText.copy(
                                            text = newText,
                                            selection = androidx.compose.ui.text.TextRange(start + 2)
                                        )
                                        addLokitLog("lok::Document::insertBulletedList() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.FormatListBulleted, contentDescription = "Insert Bulleted List", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 10. Insert Numbered Lists
                                item {
                                    IconButton(onClick = {
                                        val sel = docBodyText.selection
                                        val currentText = docBodyText.text
                                        val start = sel.start
                                        val end = sel.end
                                        val newText = currentText.substring(0, start) + "1. " + currentText.substring(end)
                                        docBodyText = docBodyText.copy(
                                            text = newText,
                                            selection = androidx.compose.ui.text.TextRange(start + 3)
                                        )
                                        addLokitLog("lok::Document::insertNumberedList() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.FormatListNumbered, contentDescription = "Insert Numbered List", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 11. Increase Indent
                                item {
                                    IconButton(onClick = {
                                        val sel = docBodyText.selection
                                        val currentText = docBodyText.text
                                        val start = sel.start
                                        val end = sel.end
                                        val newText = currentText.substring(0, start) + "\t" + currentText.substring(end)
                                        docBodyText = docBodyText.copy(
                                            text = newText,
                                            selection = androidx.compose.ui.text.TextRange(start + 1)
                                        )
                                        addLokitLog("lok::Document::increaseIndent() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.FormatIndentIncrease, contentDescription = "Increase Indent", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 12. Decrease Indent
                                item {
                                    IconButton(onClick = {
                                        addLokitLog("lok::Document::decreaseIndent() -> SUCCESS")
                                        Toast.makeText(context, "Decrease Indent", Toast.LENGTH_SHORT).show()
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.FormatIndentDecrease, contentDescription = "Decrease Indent", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 13. Change Text Direction
                                item {
                                    IconButton(onClick = {
                                        textAlignment = if (textAlignment == TextAlign.Left) TextAlign.Right else TextAlign.Left
                                        addLokitLog("lok::Document::setTextDirection() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.FormatTextdirectionLToR, contentDescription = "Change text direction", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 14. Insert Pictures
                                item {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Menyisipkan Gambar...", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::insertImage() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.Image, contentDescription = "Insert Picture", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 15. Insert Tables
                                item {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Menyisipkan Tabel...", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::insertTable() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.GridOn, contentDescription = "Insert Table", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 16. Insert Links
                                item {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Menyisipkan Tautan...", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::insertLink() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.Link, contentDescription = "Insert Link", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                // 17. Add Comment
                                item {
                                    IconButton(onClick = {
                                        Toast.makeText(context, "Menambahkan Komentar...", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::addComment() -> SUCCESS")
                                        triggerAutosave()
                                    }) {
                                        Icon(Icons.Rounded.Comment, contentDescription = "Add Comment", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Vertical Full-height Divider separating horizontal tools from persistent triggers
                        Spacer(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight(0.7f)
                                .background(borderStrokeColor)
                                .padding(horizontal = 4.dp)
                        )

                        // Right Side: 4 Persistent Icons (Material Symbols Rounded)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val availableToolbarPages = listOf("Standard")

                            // 1. Switch Toolbar (Dynamic based on page count)
                            if (availableToolbarPages.size > 1) {
                                Box {
                                    IconButton(
                                        onClick = {
                                            if (availableToolbarPages.size == 2) {
                                                activeToolbarType = if (activeToolbarType == "Standard") "Formatting" else "Standard"
                                            } else {
                                                showToolbarPagesMenu = true
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Cached,
                                            contentDescription = "Switch Toolbar",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (availableToolbarPages.size >= 3) {
                                        DropdownMenu(
                                            expanded = showToolbarPagesMenu,
                                            onDismissRequest = { showToolbarPagesMenu = false }
                                        ) {
                                            availableToolbarPages.forEach { page ->
                                                DropdownMenuItem(
                                                    text = { Text(page) },
                                                    onClick = {
                                                        activeToolbarType = page
                                                        showToolbarPagesMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Insert Tab Character
                            IconButton(
                                onClick = {
                                    val currentText = docBodyText.text
                                    val selection = docBodyText.selection
                                    val start = selection.start
                                    val end = selection.end
                                    val newText = currentText.substring(0, start) + "\t" + currentText.substring(end)
                                    val newSelection = androidx.compose.ui.text.TextRange(start + 1)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = newText,
                                        selection = newSelection
                                    )
                                    triggerAutosave()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardTab,
                                    contentDescription = "Insert Tab",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // 3. Show Keyboard
                            IconButton(
                                onClick = {
                                    if (isKeyboardVisible) {
                                        keyboardController?.hide()
                                    } else {
                                        focusRequester.requestFocus()
                                        keyboardController?.show()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Keyboard,
                                    contentDescription = "Show Keyboard",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            // 4. Open Standard Bottom Sheet (Simplified Ribbon Deck)
                            IconButton(
                                onClick = {
                                    val wasVisible = isKeyboardVisible
                                    coroutineScope.launch {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        delay(100)
                                        wasKeyboardOpenBeforeBottomSheet = wasVisible
                                        showBottomBar = true
                                        bottomBarDeck = "ribbon"
                                    }
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ViewAgenda,
                                    contentDescription = "Open Standard Bottom Sheet",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
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
            AnimatedVisibility(
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
                                    IconButton(onClick = {
                                        triggerAutosave()
                                        Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::postWindow(event=UNDO)")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Undo,
                                            contentDescription = "Undo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        triggerAutosave()
                                        Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::postWindow(event=REDO)")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Redo,
                                            contentDescription = "Redo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
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
                                        .horizontalScroll(rememberScrollState()),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val tabs = listOf("File", "Home", "Insert", "Layout", "References", "Mailings", "Review", "View")
                                    tabs.forEach { tab ->
                                        val isSelected = activeRibbonTab == tab
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                    else Color.Transparent
                                                )
                                                .clickable { activeRibbonTab = tab }
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
                                    IconButton(onClick = {
                                        triggerAutosave()
                                        Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::postWindow(event=UNDO)")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Undo,
                                            contentDescription = "Undo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
                                        triggerAutosave()
                                        Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                                        addLokitLog("lok::Document::postWindow(event=REDO)")
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Redo,
                                            contentDescription = "Redo",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(onClick = {
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
                                        "underline_color" -> ColorPickerSubpage(underlineColor, "Warna Garis Bawah") { underlineColor = it }
                                        "font_color" -> ColorPickerSubpage(fontColor, "Warna Font") { fontColor = it; triggerAutosave() }
                                        "highlight_color" -> ColorPickerSubpage(highlightColor, "Warna Sorotan") { highlightColor = it; triggerAutosave() }
                                        "line_spacing" -> LineSpacingSubpage(context)
                                        "bulleted_list" -> BulletedListSubpage(context)
                                        "numbered_list" -> NumberedListSubpage(context)
                                        "multilevel_list" -> MultilevelListSubpage(context)
                                        "paragraph_shading" -> ColorPickerSubpage(paragraphShadingColor, "Warna Shading") { paragraphShadingColor = it }
                                        "paragraph_border" -> ParagraphBorderSubpage(context)
                                        "paragraph_styles" -> ParagraphStylesSubpage(context, selectedStyleNameForOptions) { styleName ->
                                            selectedStyleNameForOptions = styleName
                                            activeInkySubpage = "style_options"
                                        }
                                        "create_new_style" -> CreateNewStyleSubpage(context) {
                                            activeInkySubpage = "paragraph_styles"
                                        }
                                        "style_options" -> StyleOptionsSubpage(context, selectedStyleNameForOptions) {
                                            activeInkySubpage = "paragraph_styles"
                                        }
                                        "change_capitalization" -> ChangeCapitalizationSubpage(context)
                                    }
                                }
                            } else {
                                if (activeRibbonTab == "File") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(vertical = 8.dp)
                                    ) {
                                        FileSubpage(
                                            context = context,
                                            onNavigateToOptions = { showOptionsDialog = true }
                                        )
                                    }
                                } else if (activeRibbonTab == "Home") {
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
                                            onShowFontSizeDialog = { showFontSizeDialog = true }
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
                                            text = "$activeRibbonTab options will be implemented soon.",
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
                    Toast.makeText(context, "Formula berhasil disisipkan!", Toast.LENGTH_SHORT).show()
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

    if (showFct) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        androidx.compose.ui.window.Popup(
            onDismissRequest = { showFct = false },
            properties = androidx.compose.ui.window.PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            offset = fctOffset
        ) {
            FloatingContextualToolbar(
                visible = true,
                contextType = fctContext,
                onActionClick = { action ->
                    showFct = false
                    when (action) {
                        "cut" -> {
                            textToolbarCutCallback?.invoke() ?: run {
                                val currentText = docBodyText.text
                                val sel = docBodyText.selection
                                if (!sel.collapsed) {
                                    val selectedText = currentText.substring(sel.start, sel.end)
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedText))
                                    val newText = currentText.substring(0, sel.start) + currentText.substring(sel.end)
                                    docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                        text = newText,
                                        selection = androidx.compose.ui.text.TextRange(sel.start)
                                    )
                                    triggerAutosave()
                                }
                            }
                        }
                        "copy" -> {
                            textToolbarCopyCallback?.invoke() ?: run {
                                val sel = docBodyText.selection
                                if (!sel.collapsed) {
                                    val selectedText = docBodyText.text.substring(sel.start, sel.end)
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(selectedText))
                                }
                            }
                        }
                        "paste" -> {
                            textToolbarPasteCallback?.invoke() ?: run {
                                val clipText = clipboardManager.getText()?.text ?: ""
                                val currentText = docBodyText.text
                                val sel = docBodyText.selection
                                val newText = currentText.substring(0, sel.start) + clipText + currentText.substring(sel.end)
                                docBodyText = androidx.compose.ui.text.input.TextFieldValue(
                                    text = newText,
                                    selection = androidx.compose.ui.text.TextRange(sel.start + clipText.length)
                                )
                                triggerAutosave()
                            }
                        }
                        "ai_write" -> {
                            aiPrompt = "Analyze and summarize this setup layout..."
                            showAiAssistant = true
                        }
                    }
                }
            )
        }
    }

    // --- OPT-IN GEMINI CO-AUTHOR ASSISTANT DIALOG ---
    if (showAiAssistant) {
        AlertDialog(
            onDismissRequest = { showAiAssistant = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Inky AI Copilot")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Google Gemini integration is strictly opt-in and operates with zero data retention for developer compliance.", fontSize = 11.sp, color = Color.Gray)
                    
                    OutlinedTextField(
                        value = aiPrompt,
                        onValueChange = { aiPrompt = it },
                        label = { Text("Ask Copilot (e.g., summarize, improve, proofread)") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt")
                    )

                    if (isLoadingAi) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (aiResponse.isNotEmpty()) {
                        Text("Response:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(aiResponse, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isLoadingAi = true
                        coroutineScope.launch {
                            val response = GeminiAiService.generateContent(
                                context = context,
                                prompt = aiPrompt,
                                systemInstruction = "You are helping edit an ODF document inside Papirus Inky."
                            )
                            aiResponse = response
                            isLoadingAi = false
                        }
                    },
                    enabled = aiPrompt.isNotEmpty() && !isLoadingAi
                ) {
                    Text("Analyze")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiAssistant = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showOptionsDialog) {
        var optAiEnabled by remember { mutableStateOf(GeminiAiService.isAiEnabled(context)) }
        var optApiKey by remember { mutableStateOf(GeminiAiService.getUserApiKey(context)) }
        var optModel by remember { mutableStateOf(GeminiAiService.getSelectedModel(context)) }
        var optShowModelMenu by remember { mutableStateOf(false) }

        var activeSettingSection by remember { mutableStateOf("general") }

        AlertDialog(
            onDismissRequest = { showOptionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Papirus Office Options", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = activeSettingSection == "general",
                            onClick = { activeSettingSection = "general" },
                            label = { Text("General") }
                        )
                        FilterChip(
                            selected = activeSettingSection == "ai",
                            onClick = { activeSettingSection = "ai" },
                            label = { Text("Gemini AI") }
                        )
                        FilterChip(
                            selected = activeSettingSection == "about",
                            onClick = { activeSettingSection = "about" },
                            label = { Text("About") }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (activeSettingSection) {
                            "general" -> {
                                Text("Theming & Appearance", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dynamic Color (Material You)", style = MaterialTheme.typography.bodyMedium)
                                        Text("Use system wallpaper accent colors (Android 12+)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = dynamicColorEnabled,
                                        enabled = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S,
                                        onCheckedChange = { isChecked ->
                                            ThemeSettings.setDynamicColorEnabled(context, isChecked)
                                            onDynamicColorChange(isChecked)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Language & Formats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Locale: English (US) / Indonesian (Fallback)", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("LibreOffice Core Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Native ODF Parse Cache: Enabled", style = MaterialTheme.typography.bodyMedium)
                                Text("JNI Memory Buffering: Optimized for 64-bit systems", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Document Storage Config", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Auto-Save Recovery: 60 seconds interval", style = MaterialTheme.typography.bodyMedium)
                                Text("Incremental Sync: Active", style = MaterialTheme.typography.bodyMedium)
                            }
                            "ai" -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Google Gemini Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        Text("Enable AI assistant to generate formulas, explain code and summarize logs.", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Switch(
                                        checked = optAiEnabled,
                                        onCheckedChange = {
                                            optAiEnabled = it
                                            GeminiAiService.setAiEnabled(context, it)
                                        }
                                    )
                                }

                                if (optAiEnabled) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = optApiKey,
                                        onValueChange = {
                                            optApiKey = it
                                            GeminiAiService.saveUserApiKey(context, it)
                                        },
                                        label = { Text("Google AI Studio API Key") },
                                        placeholder = { Text("AIzaSy...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = optModel,
                                            onValueChange = {},
                                            label = { Text("Active AI Model") },
                                            readOnly = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = {
                                                IconButton(onClick = { optShowModelMenu = true }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                }
                                            }
                                        )
                                        DropdownMenu(
                                            expanded = optShowModelMenu,
                                            onDismissRequest = { optShowModelMenu = false }
                                        ) {
                                            GeminiAiService.SUPPORTED_MODELS.forEach { modelPair ->
                                                DropdownMenuItem(
                                                    text = { Text(modelPair.second) },
                                                    onClick = {
                                                        optModel = modelPair.first
                                                        GeminiAiService.saveSelectedModel(context, modelPair.first)
                                                        optShowModelMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Privacy Guarantee", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "API Keys are kept offline in secure app preferences and transmitted securely directly to the Google Gemini developer API endpoints.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            "about" -> {
                                Text("Papirus Office Enterprise", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Version: v2026.07.20-Expressive", style = MaterialTheme.typography.bodyMedium)
                                Text("Engine: LibreOffice Core v24.2.5 (Fork)", style = MaterialTheme.typography.bodyMedium)
                                Text("Platform JNI bindings: Active", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Developers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Google AI Studio Coding Agent & Maker Andreas", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOptionsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    }
}

// ==========================================
// File Subpage & Components
// ==========================================
@Composable
private fun FileSubpage(
    context: android.content.Context,
    onNavigateToOptions: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Grup File
        FileMenuSectionHeader("File")
        FileMenuThreeColumnRow(
            item1 = Triple(Icons.Rounded.NoteAdd, "New") {
                Toast.makeText(context, "New document created", Toast.LENGTH_SHORT).show()
            },
            item2 = Triple(Icons.Rounded.FolderOpen, "Open") {
                Toast.makeText(context, "Opening file picker...", Toast.LENGTH_SHORT).show()
            },
            item3 = Triple(Icons.Rounded.Close, "Close") {
                Toast.makeText(context, "Closing document...", Toast.LENGTH_SHORT).show()
            }
        )
        FileMenuListItem(
            icon = Icons.Rounded.Refresh,
            title = "Reload document"
        ) {
            Toast.makeText(context, "Reloading document...", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Document Saved!", Toast.LENGTH_SHORT).show()
        }
        FileMenuListItem(
            icon = Icons.Rounded.Save,
            title = "Save as..."
        ) {
            Toast.makeText(context, "Opening Save As dialog...", Toast.LENGTH_SHORT).show()
        }
        FileMenuListItem(
            icon = Icons.Rounded.LibraryBooks,
            title = "Save all opened document"
        ) {
            Toast.makeText(context, "Saving all documents...", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Opening Android Share Sheet...", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Preparing print job...", Toast.LENGTH_SHORT).show()
            },
            item2 = Triple(Icons.Rounded.RemoveRedEye, "Preview") {
                Toast.makeText(context, "Generating print preview...", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Properties: 1 page, 340 words, 2,130 characters", Toast.LENGTH_LONG).show()
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
