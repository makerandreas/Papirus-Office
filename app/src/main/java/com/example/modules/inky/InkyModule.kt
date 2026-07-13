package com.example.modules.inky

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Track setup model matching the actual user document
data class TrackSetupRow(val track: String, val sayap: Int, val rem: Int, val suspensi: Int)
data class TyreSetupRow(val durasi: Int, val konsumsi: Int, val perbedaan: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkyModule(
    isTablet: Boolean,
    onFormatAction: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // --- Inky Core States ---
    var isEditMode by remember { mutableStateOf(false) } // False = Viewer Mode, True = Edit Mode
    var isWebView by remember { mutableStateOf(false) }  // False = Normal View, True = Web View
    var isDarkDocument by remember { mutableStateOf(false) } // Dark document canvas mode
    var isSaved by remember { mutableStateOf(true) }     // Tracks saved indicator suffix
    var docTitle by remember { mutableStateOf("My Monoposto Settings") }

    // Zoom and dynamic typing states
    var zoomScale by remember { mutableStateOf(1.0f) }
    var documentContentTitle by remember { mutableStateOf("Monoposto Realistic Settings") }
    var activeToolbarType by remember { mutableStateOf("Formatting") } // Formatting vs Standard

    val advSettings = remember {
        mutableStateListOf(
            "Driving Help: Mid",
            "Opponent Level: Real",
            "Bantuan Mengemudi: 27",
            "Antispin: 22",
            "Sensitivitas Kemudi: 115"
        )
    }

    val activity = LocalContext.current as? androidx.activity.ComponentActivity

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
    var showBottomBar by remember { mutableStateOf(false) }
    var bottomBarDeck by remember { mutableStateOf("ribbon") } // ribbon, font_color, font_size, font_family, highlight_color
    var activeRibbonTab by remember { mutableStateOf("Beranda") } // File, Beranda, Sisipkan, Tata Letak, Ditinjau, Tampilan
    var showRibbonTabMenu by remember { mutableStateOf(false) }

    LaunchedEffect(showBottomBar) {
        activity?.window?.let { win ->
            val controller = androidx.core.view.WindowCompat.getInsetsController(win, win.decorView)
            if (showBottomBar) {
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            } else {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    // FCT state
    var showFct by remember { mutableStateOf(false) }
    var fctContext by remember { mutableStateOf("text") } // text, table, object

    // Scroll Control to hide AppBar and Toolbar Hub dynamically
    var previousScrollValue by remember { mutableStateOf(0) }
    var isControlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(scrollState.value) {
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
            Toast.makeText(context, "Dokumen disimpan otomatis ke local sync!", Toast.LENGTH_SHORT).show()
        }
    }

    // Layout configuration variables
    val docBgColor = if (isDarkDocument) Color(0xFF181A1B) else Color(0xFFF1F5F9)
    val pageBgColor = if (isDarkDocument) Color(0xFF242627) else Color.White
    val textPrimaryColor = if (isDarkDocument) Color(0xFFE8E6E3) else fontColor
    val textSecondaryColor = if (isDarkDocument) Color(0xFFA8A6A3) else Color.DarkGray
    val textAccentColor = if (isDarkDocument) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val borderStrokeColor = if (isDarkDocument) Color(0xFF3C3F41) else Color(0xFFE2E8F0)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(docBgColor)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // --- HEADER TOP APP BAR ---
            // Hidden temporarily if bottom bar is opened or scrolling hides it
            AnimatedVisibility(
                visible = !showBottomBar && isControlsVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                if (!isEditMode) {
                    // --- VIEWER MODE APP BAR ---
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = docTitle,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Hanya Lihat • ODT Format",
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
                            IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                Icon(Icons.Default.Search, contentDescription = "Find in Document")
                            }
                            IconButton(onClick = { Toast.makeText(context, "Mengunggah ke Google Drive...", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload to Drive")
                            }
                            IconButton(onClick = { 
                                isWebView = !isWebView
                                Toast.makeText(context, if (isWebView) "Tampilan Seluler Aktif" else "Tampilan Normal Aktif", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = if (isWebView) Icons.Default.PhoneAndroid else Icons.Default.Web,
                                    contentDescription = "Document View Mode"
                                )
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Export to PDF") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Mengekspor ke PDF...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as...") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Menyimpan sebagai file baru...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                        onClick = {
                                            showMoreMenu = false
                                            isDarkDocument = !isDarkDocument
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Print") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Mempersiapkan printer...", Toast.LENGTH_SHORT).show()
                                        }
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = docTitle,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSaved) {
                                        Text(
                                            text = " - Disimpan",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    } else {
                                        Text(
                                            text = " - Menyimpan...",
                                            fontSize = 12.sp,
                                            color = Color.LightGray,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                                // QAT quick inline actions
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudQueue,
                                        contentDescription = "Cloud synced",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("Local Sync Active", fontSize = 9.sp, color = Color.Gray)
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { isEditMode = false }) {
                                Icon(Icons.Default.Check, contentDescription = "Exit Edit Mode", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        actions = {
                            IconButton(onClick = { Toast.makeText(context, "Mengunggah ke Google Drive...", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload to Drive")
                            }
                            IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                Icon(Icons.Default.FindReplace, contentDescription = "Find and Replace")
                            }
                            IconButton(onClick = { 
                                isWebView = !isWebView
                                Toast.makeText(context, if (isWebView) "Tampilan Seluler" else "Tampilan Normal", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = if (isWebView) Icons.Default.PhoneAndroid else Icons.Default.Web,
                                    contentDescription = "Document View Mode"
                                )
                            }
                            IconButton(onClick = { 
                                triggerAutosave()
                                Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo")
                            }
                            Box {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Share") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Membuka Android Share Sheet...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Export to PDF") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Mengekspor ODF ke PDF...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save") },
                                        onClick = {
                                            showMoreMenu = false
                                            triggerAutosave()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Save as") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Menyimpan salinan...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (isDarkDocument) "Light Document Mode" else "Dark Document Mode") },
                                        onClick = {
                                            showMoreMenu = false
                                            isDarkDocument = !isDarkDocument
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Read it aloud") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Membacakan dokumen...", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Open Navigator Bar") },
                                        onClick = {
                                            showMoreMenu = false
                                            showBottomBar = true
                                            bottomBarDeck = "navigator"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Document Version History") },
                                        onClick = {
                                            showMoreMenu = false
                                            showBottomBar = true
                                            bottomBarDeck = "version_history"
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Print") },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(context, "Menghubungkan printer...", Toast.LENGTH_SHORT).show()
                                        }
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
                // Interactive document container
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(if (isWebView) 0.dp else 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Normal Document Page Canvas (styled as a paper layout)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(bottom = 32.dp)
                            .border(
                                width = if (isWebView) 0.dp else 1.dp,
                                color = borderStrokeColor,
                                shape = if (isWebView) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                            )
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        showFct = true
                                        fctContext = "text"
                                    },
                                    onLongPress = {
                                        showFct = true
                                        fctContext = "table"
                                    },
                                    onTap = {
                                        // Tap outside dismisses Bottom Bar and FCT
                                        showBottomBar = false
                                        showFct = false
                                    }
                                )
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isWebView) 0.dp else 4.dp),
                        colors = CardDefaults.cardColors(containerColor = pageBgColor),
                        shape = if (isWebView) RoundedCornerShape(0.dp) else RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isWebView) 16.dp else 28.dp)
                        ) {
                            
                            // Document Header / Title (Editable in Edit Mode)
                            if (isEditMode) {
                                androidx.compose.foundation.text.BasicTextField(
                                     value = documentContentTitle,
                                     onValueChange = {
                                         documentContentTitle = it
                                         triggerAutosave()
                                     },
                                     textStyle = androidx.compose.ui.text.TextStyle(
                                         fontSize = if (isBold) (28 * zoomScale).sp else (24 * zoomScale).sp,
                                         fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
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
                                     modifier = Modifier.fillMaxWidth().padding(bottom = (8 * zoomScale).dp)
                                )
                            } else {
                                Text(
                                     text = documentContentTitle,
                                     fontSize = if (isBold) (28 * zoomScale).sp else (24 * zoomScale).sp,
                                     fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
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
                                     modifier = Modifier.fillMaxWidth().padding(bottom = (8 * zoomScale).dp),
                                     textAlign = textAlignment
                                )
                            }

                            // Track Setup Section
                            Text(
                                text = "Track Setup",
                                fontSize = (18 * zoomScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = textAccentColor,
                                modifier = Modifier.padding(top = (12 * zoomScale).dp, bottom = (8 * zoomScale).dp)
                            )

                            // Responsive custom bento-grid styled data table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(textAccentColor.copy(alpha = 0.1f))
                                        .padding((10 * zoomScale).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Trek", fontWeight = FontWeight.Bold, fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(2f), color = textPrimaryColor)
                                    Text("Sayap", fontWeight = FontWeight.Bold, fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    Text("Rem", fontWeight = FontWeight.Bold, fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    Text("Suspensi", fontWeight = FontWeight.Bold, fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1.2f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                }

                                HorizontalDivider(color = borderStrokeColor)

                                // Dynamic Rows
                                tracks.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                fctContext = "table"
                                                showFct = true
                                            }
                                            .background(
                                                if (idx % 2 == 0) pageBgColor else textAccentColor.copy(alpha = 0.03f)
                                            )
                                            .padding(vertical = (8 * zoomScale).dp, horizontal = (10 * zoomScale).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(item.track, fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(2f), color = textPrimaryColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${item.sayap}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                        Text("${item.rem}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                        Text("${item.suspensi}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1.2f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    }
                                    if (idx < tracks.size - 1) {
                                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Tyre Setup Section
                            Text(
                                text = "Tyre Setup",
                                fontSize = (18 * zoomScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = textAccentColor,
                                modifier = Modifier.padding(bottom = (8 * zoomScale).dp)
                            )

                            // Tyre table
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(textAccentColor.copy(alpha = 0.08f))
                                        .padding((10 * zoomScale).dp)
                                ) {
                                    Text("Durasi Balapan (%)", fontWeight = FontWeight.Bold, fontSize = (11 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    Text("Konsumsi Ban", fontWeight = FontWeight.Bold, fontSize = (11 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    Text("Perbedaan Konsumsi Ban", fontWeight = FontWeight.Bold, fontSize = (11 * zoomScale).sp, modifier = Modifier.weight(1.2f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                }
                                HorizontalDivider(color = borderStrokeColor)
                                tyreSetups.forEachIndexed { idx, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = (8 * zoomScale).dp, horizontal = (10 * zoomScale).dp)
                                    ) {
                                        Text("${item.durasi}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                        Text("${item.konsumsi}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                        Text("${item.perbedaan}", fontSize = (12 * zoomScale).sp, modifier = Modifier.weight(1.2f), color = textPrimaryColor, textAlign = TextAlign.Center)
                                    }
                                    if (idx < tyreSetups.size - 1) {
                                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.5f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height((24 * zoomScale).dp))

                            // Advanced Options Section
                            Text(
                                text = "Pengaturan Lanjutan",
                                fontSize = (16 * zoomScale).sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor,
                                modifier = Modifier.padding(bottom = (8 * zoomScale).dp)
                            )

                            // Bulleted lists (Editable in Edit Mode)
                            advSettings.forEachIndexed { idx, setting ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = (4 * zoomScale).dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = (8 * zoomScale).dp)
                                            .size((5 * zoomScale).dp)
                                            .background(textPrimaryColor, CircleShape)
                                    )
                                    if (isEditMode) {
                                        androidx.compose.foundation.text.BasicTextField(
                                            value = setting,
                                            onValueChange = { newValue ->
                                                advSettings[idx] = newValue
                                                triggerAutosave()
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = (13 * zoomScale).sp,
                                                color = textSecondaryColor
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Text(
                                            text = setting,
                                            fontSize = (13 * zoomScale).sp,
                                            color = textSecondaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- FLOATING ACTION BUTTON (Viewer Mode Only) ---
                if (!isEditMode && !showBottomBar) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            isEditMode = true
                            Toast.makeText(context, "Beralih ke Mode Edit", Toast.LENGTH_SHORT).show()
                        },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                        text = { Text("Edit Document") },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(24.dp)
                            .testTag("fab_edit_document"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                }

                // --- FLOATING CONTEXTUAL TOOLBAR (FCT Overlay) ---
                FloatingContextualToolbar(
                    visible = showFct,
                    contextType = fctContext,
                    onActionClick = { action ->
                        showFct = false
                        when (action) {
                            "cut" -> Toast.makeText(context, "Teks dipotong ke clipboard", Toast.LENGTH_SHORT).show()
                            "copy" -> Toast.makeText(context, "Teks disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            "paste" -> Toast.makeText(context, "Teks ditempel dari clipboard", Toast.LENGTH_SHORT).show()
                            "delete" -> Toast.makeText(context, "Elemen dihapus", Toast.LENGTH_SHORT).show()
                            "edit_data" -> {
                                Toast.makeText(context, "Membuka editor tabel...", Toast.LENGTH_SHORT).show()
                            }
                            "ai_write" -> {
                                aiPrompt = "Analyze and summarize this setup layout..."
                                showAiAssistant = true
                            }
                        }
                    }
                )
            }

            // --- FOOTER STATS & STATUS BAR (Always visible in document workspaces) ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Halaman 1 dari 2",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Text(
                        text = "312 kata",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (zoomScale > 0.5f) zoomScale -= 0.1f },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(androidx.compose.material.icons.Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = "Zoom: ${(zoomScale * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.clickable { zoomScale = 1.0f }
                        )
                        IconButton(
                            onClick = { if (zoomScale < 2.0f) zoomScale += 0.1f },
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
                visible = isEditMode && !showBottomBar && isControlsVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    if (dragAmount.y < -6f) {
                                        if (activeToolbarType != "Standard") {
                                            activeToolbarType = "Standard"
                                        }
                                    } else if (dragAmount.y > 6f) {
                                        if (activeToolbarType != "Formatting") {
                                            activeToolbarType = "Formatting"
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Switchable Toolbar Mode Indicator Pill
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .padding(start = 6.dp, end = 6.dp)
                                .clickable {
                                    activeToolbarType = if (activeToolbarType == "Formatting") "Standard" else "Formatting"
                                }
                        ) {
                            Text(
                                text = activeToolbarType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Switchable Scrollable Toolbars (Standard vs Formatting)
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (activeToolbarType == "Formatting") {
                                // --- FORMATTING TOOLBAR ---
                                item {
                                    IconButton(
                                        onClick = { 
                                            isBold = !isBold
                                            triggerAutosave()
                                        },
                                        colors = if (isBold) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                                    }
                                }
                                item {
                                    IconButton(
                                        onClick = { 
                                            isItalic = !isItalic
                                            triggerAutosave()
                                        },
                                        colors = if (isItalic) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                                    }
                                }
                                item {
                                    IconButton(
                                        onClick = { 
                                            isUnderline = !isUnderline
                                            triggerAutosave()
                                        },
                                        colors = if (isUnderline) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                                    }
                                }
                                item {
                                    IconButton(
                                        onClick = { 
                                            isStrikethrough = !isStrikethrough
                                            triggerAutosave()
                                        },
                                        colors = if (isStrikethrough) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer) else IconButtonDefaults.iconButtonColors()
                                    ) {
                                        Icon(Icons.Default.StrikethroughS, contentDescription = "Strikethrough")
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        textAlignment = when (textAlignment) {
                                            TextAlign.Left -> TextAlign.Center
                                            TextAlign.Center -> TextAlign.Right
                                            TextAlign.Right -> TextAlign.Justify
                                            else -> TextAlign.Left
                                        }
                                        triggerAutosave()
                                    }) {
                                        Icon(
                                            imageVector = when (textAlignment) {
                                                TextAlign.Center -> Icons.Default.FormatAlignCenter
                                                TextAlign.Right -> Icons.Default.FormatAlignRight
                                                TextAlign.Justify -> Icons.Default.FormatAlignJustify
                                                else -> Icons.Default.FormatAlignLeft
                                            },
                                            contentDescription = "Alignment"
                                        )
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        showBottomBar = true
                                        bottomBarDeck = "font_color"
                                    }) {
                                        Icon(Icons.Default.FormatColorText, contentDescription = "Font Color", tint = fontColor)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        showBottomBar = true
                                        bottomBarDeck = "highlight_color"
                                    }) {
                                        Icon(Icons.Default.BorderColor, contentDescription = "Highlight Color", tint = if (highlightColor == Color.Transparent) Color.Gray else highlightColor)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        showEquationDialog = true
                                    }) {
                                        Icon(Icons.Default.Functions, contentDescription = "Equation Composer")
                                    }
                                }
                            } else {
                                // --- STANDARD TOOLBAR ---
                                item {
                                    IconButton(onClick = { 
                                        Toast.makeText(context, "Dokumen Disimpan!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Save, contentDescription = "Simpan", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        Toast.makeText(context, "Batal tindakan terakhir", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        Toast.makeText(context, "Ulangi tindakan terakhir", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        showFindReplace = !showFindReplace
                                    }) {
                                        Icon(Icons.Default.Search, contentDescription = "Cari & Ganti", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        showAiAssistant = true
                                    }) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = "Asisten AI", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        Toast.makeText(context, "Mengekspor berkas ke PDF...", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Ekspor PDF", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                item {
                                    IconButton(onClick = { 
                                        Toast.makeText(context, "Sinkronisasi cloud diaktifkan", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Sync", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // Persistent control triggers on the rightmost area
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { 
                                documentContentTitle = documentContentTitle + "    "
                                Toast.makeText(context, "Tab (Spasi Ganda) dimasukkan", Toast.LENGTH_SHORT).show() 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = "Insert Tab", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { 
                                keyboardController?.show()
                                Toast.makeText(context, "Keyboard virtual ditampilkan", Toast.LENGTH_SHORT).show() 
                            }) {
                                Icon(Icons.Default.Keyboard, contentDescription = "Show Keyboard", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { 
                                    showBottomBar = true
                                    bottomBarDeck = "ribbon"
                                },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(Icons.Default.ViewAgenda, contentDescription = "Open Bottom Bar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            // --- BOTTOM BAR DECK (Simplified Ribbon & Sub-Decks) ---
            // Open bottom bar deck will overlay perfectly, hiding upper AppBars & Toolbars
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    tonalElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = BorderStroke(1.dp, borderStrokeColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        
                        // --- BOTTOM BAR DECK HEADER ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Context-dependent back button
                                if (bottomBarDeck != "ribbon") {
                                    IconButton(onClick = { bottomBarDeck = "ribbon" }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Ribbon")
                                    }
                                }

                                // Interactive Category / Menu Tab Selector
                                if (bottomBarDeck == "ribbon") {
                                    Box {
                                        Row(
                                            modifier = Modifier
                                                .clickable { showRibbonTabMenu = true }
                                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = activeRibbonTab,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Ribbon Tab", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }

                                        DropdownMenu(
                                            expanded = showRibbonTabMenu,
                                            onDismissRequest = { showRibbonTabMenu = false }
                                        ) {
                                            listOf("File", "Beranda", "Sisipkan", "Tata Letak", "Ditinjau", "Tampilan").forEach { tab ->
                                                DropdownMenuItem(
                                                    text = { Text(tab) },
                                                    onClick = {
                                                        activeRibbonTab = tab
                                                        showRibbonTabMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Custom title for sub-decks
                                    Text(
                                        text = when (bottomBarDeck) {
                                            "font_color" -> "Warna Font"
                                            "font_size" -> "Ukuran Font"
                                            "font_family" -> "Font Family"
                                            "highlight_color" -> "Sorot Warna"
                                            "navigator" -> "Navigator"
                                            "version_history" -> "Riwayat Versi"
                                            else -> "Pilihan Format"
                                        },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            // Undo / Redo & Hide bottom deck
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { 
                                    triggerAutosave()
                                    Toast.makeText(context, "Undo", Toast.LENGTH_SHORT).show() 
                                }) {
                                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                                }
                                IconButton(onClick = { 
                                    triggerAutosave()
                                    Toast.makeText(context, "Redo", Toast.LENGTH_SHORT).show() 
                                }) {
                                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                                }
                                IconButton(onClick = { showBottomBar = false }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Hide Bottom Bar")
                                }
                            }
                        }

                        HorizontalDivider(color = borderStrokeColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

                        // --- BOTTOM BAR DECK CONTENT CANVAS ---
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            when (bottomBarDeck) {
                                "ribbon" -> {
                                    // Render content according to active ribbon tab
                                    when (activeRibbonTab) {
                                        "Beranda" -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                // Font Family & Size selectors
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Card(
                                                        onClick = { bottomBarDeck = "font_family" },
                                                        modifier = Modifier.weight(1.5f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(activeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                                            Icon(Icons.Default.ChevronRight, contentDescription = "Select font", modifier = Modifier.size(16.dp))
                                                        }
                                                    }

                                                    Card(
                                                        onClick = { bottomBarDeck = "font_size" },
                                                        modifier = Modifier.weight(1f),
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("$activeFontSize pt", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                                            Icon(Icons.Default.ChevronRight, contentDescription = "Select size", modifier = Modifier.size(16.dp))
                                                        }
                                                    }
                                                }

                                                // Inline Formatting Toggle Group
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    listOf(
                                                        Triple(Icons.Default.FormatBold, isBold, "Bold"),
                                                        Triple(Icons.Default.FormatItalic, isItalic, "Italic"),
                                                        Triple(Icons.Default.FormatUnderlined, isUnderline, "Underline"),
                                                        Triple(Icons.Default.StrikethroughS, isStrikethrough, "Strikethrough")
                                                    ).forEach { item ->
                                                        val isSelected = item.second
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                                                )
                                                                .border(1.dp, borderStrokeColor, RoundedCornerShape(8.dp))
                                                                .clickable {
                                                                    when (item.third) {
                                                                        "Bold" -> isBold = !isBold
                                                                        "Italic" -> isItalic = !isItalic
                                                                        "Underline" -> isUnderline = !isUnderline
                                                                        "Strikethrough" -> isStrikethrough = !isStrikethrough
                                                                    }
                                                                    triggerAutosave()
                                                                }
                                                                .padding(vertical = 10.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = item.first,
                                                                contentDescription = item.third,
                                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }
                                                }

                                                // Highlight & Color selection redirects
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { bottomBarDeck = "highlight_color" }
                                                            .border(1.dp, borderStrokeColor, RoundedCornerShape(10.dp))
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.BorderColor, contentDescription = null, tint = if (highlightColor == Color.Transparent) Color.Gray else highlightColor)
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Sorot", fontSize = 13.sp)
                                                        }
                                                        Icon(Icons.Default.ChevronRight, contentDescription = "Detail", modifier = Modifier.size(16.dp))
                                                    }

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { bottomBarDeck = "font_color" }
                                                            .border(1.dp, borderStrokeColor, RoundedCornerShape(10.dp))
                                                            .padding(12.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(modifier = Modifier.size(20.dp).background(fontColor, CircleShape))
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text("Warna Font", fontSize = 13.sp)
                                                        }
                                                        Icon(Icons.Default.ChevronRight, contentDescription = "Detail", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        "File" -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(onClick = { triggerAutosave() }, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.Save, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Simpan")
                                                    }
                                                    Button(onClick = { Toast.makeText(context, "Mengekspor PDF...", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) {
                                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("PDF")
                                                    }
                                                }
                                                OutlinedButton(onClick = { Toast.makeText(context, "Membuka printer...", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) {
                                                    Icon(Icons.Default.Print, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Cetak Dokumen")
                                                }
                                            }
                                        }
                                        "Sisipkan" -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                listOf(
                                                    Triple(Icons.Default.GridOn, "Tabel", "table"),
                                                    Triple(Icons.Default.Image, "Gambar", "image"),
                                                    Triple(Icons.Default.Functions, "Formula", "equation")
                                                ).forEach { item ->
                                                    Card(
                                                        onClick = {
                                                            if (item.third == "equation") {
                                                                showEquationDialog = true
                                                            } else {
                                                                Toast.makeText(context, "Menyisipkan ${item.second}...", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Icon(item.first, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(item.second, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        "Tata Letak" -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Tata Letak Halaman (ODF/LibreOffice)", fontSize = 11.sp, color = Color.Gray)
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(onClick = { Toast.makeText(context, "Margin diubah ke Standard", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) {
                                                        Text("Margin Normal")
                                                    }
                                                    OutlinedButton(onClick = { Toast.makeText(context, "Orientasi diubah ke Lanskap", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f)) {
                                                        Text("Lanskap")
                                                    }
                                                }
                                            }
                                        }
                                        "Ditinjau" -> {
                                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Button(onClick = { 
                                                    showAiAssistant = true
                                                    aiPrompt = "Periksa ejaan & tata bahasa dokumen ini."
                                                }) {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("AI Proofread")
                                                }
                                                OutlinedButton(onClick = { Toast.makeText(context, "Word Count: 312 words, 2048 characters", Toast.LENGTH_LONG).show() }) {
                                                    Text("Jumlah Kata")
                                                }
                                            }
                                        }
                                        "Tampilan" -> {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Tampilan Seluler (Web view)", fontSize = 13.sp)
                                                    Switch(checked = isWebView, onCheckedChange = { isWebView = it })
                                                }
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Dark document mode", fontSize = 13.sp)
                                                    Switch(checked = isDarkDocument, onCheckedChange = { isDarkDocument = it })
                                                }
                                            }
                                        }
                                    }
                                }

                                "font_family" -> {
                                    val fonts = listOf("Aptos Display", "Calibri", "Arial", "Roboto", "Times New Roman", "Courier New")
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        fonts.forEach { f ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activeFontFamily = f
                                                        bottomBarDeck = "ribbon"
                                                    }
                                                    .background(
                                                        if (activeFontFamily == f) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(f, fontWeight = if (activeFontFamily == f) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }

                                "font_size" -> {
                                    val sizes = listOf(10, 11, 12, 14, 16, 18, 20, 24, 28, 32, 36)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        sizes.forEach { size ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        activeFontSize = size
                                                        bottomBarDeck = "ribbon"
                                                    }
                                                    .background(
                                                        if (activeFontSize == size) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("$size pt", fontWeight = if (activeFontSize == size) FontWeight.Bold else FontWeight.Normal)
                                            }
                                        }
                                    }
                                }

                                "font_color" -> {
                                    // Theme color layout matching screenshot
                                    val themeColorColumns = listOf(
                                        listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC), Color(0xFFF1F5F9), Color(0xFFE2E8F0), Color(0xFFCBD5E1), Color(0xFF94A3B8)),
                                        listOf(Color(0xFF000000), Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569), Color(0xFF64748B)),
                                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFBFDBFE), Color(0xFF93C5FD), Color(0xFF60A5FA), Color(0xFF2563EB)),
                                        listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE), Color(0xFFBFDBFE), Color(0xFF93C5FD), Color(0xFF60A5FA), Color(0xFF1E3A8A)),
                                        listOf(Color(0xFFF0FDFA), Color(0xFFCCFBF1), Color(0xFF99F6E4), Color(0xFF5EEAD4), Color(0xFF2DD4BF), Color(0xFF0D9488)),
                                        listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFED7AA), Color(0xFFFDBA74), Color(0xFFFB923C), Color(0xFFF97316)),
                                        listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFBBF7D0), Color(0xFF86EFAC), Color(0xFF4ADE80), Color(0xFF22C55E)),
                                        listOf(Color(0xFFECFEFF), Color(0xFFCFFAFE), Color(0xFFA5F3FC), Color(0xFF67E8F9), Color(0xFF22D3EE), Color(0xFF06B6D4)),
                                        listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF), Color(0xFFE9D5FF), Color(0xFFD8B4FE), Color(0xFFC084FC), Color(0xFFA855F7)),
                                        listOf(Color(0xFFF7FEE7), Color(0xFFECFCCB), Color(0xFFD9F99D), Color(0xFFBEF264), Color(0xFFA3E635), Color(0xFF84CC16))
                                    )
                                    val standardColors = listOf(
                                        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF06B6D4),
                                        Color(0xFF3B82F6), Color(0xFF1E3A8A), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFF78350F)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Warna Tema", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        
                                        // Grid display theme colors
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            themeColorColumns.forEach { column ->
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    column.forEach { color ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(20.dp)
                                                                .background(color, RoundedCornerShape(2.dp))
                                                                .border(0.5.dp, Color.LightGray, RoundedCornerShape(2.dp))
                                                                .clickable {
                                                                    fontColor = color
                                                                    triggerAutosave()
                                                                    bottomBarDeck = "ribbon"
                                                                }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text("Warna Standar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            standardColors.forEach { color ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(color, RoundedCornerShape(4.dp))
                                                        .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            fontColor = color
                                                            triggerAutosave()
                                                            bottomBarDeck = "ribbon"
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                "highlight_color" -> {
                                    val colors = listOf(
                                        Color.Transparent, Color(0xFFFEF08A), Color(0xFFBBF7D0), Color(0xFFBFDBFE),
                                        Color(0xFFFBCFE8), Color(0xFFFED7AA), Color(0xFFE9D5FF), Color(0xFF99F6E4)
                                    )
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(colors) { color ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(44.dp)
                                                    .background(
                                                        if (color == Color.Transparent) Color.LightGray.copy(alpha = 0.3f) else color,
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        highlightColor = color
                                                        triggerAutosave()
                                                        bottomBarDeck = "ribbon"
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (color == Color.Transparent) {
                                                    Text("Tanpa Sorotan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                "navigator" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text("Daftar Struktur ODF Document", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        listOf("Judul: Monoposto Realistic Settings", "Tabel 1: Track Setup", "Tabel 2: Tyre Setup", "Bagian: Pengaturan Lanjutan").forEach { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        Toast.makeText(context, "Navigating to $item", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Menu, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(item, fontSize = 13.sp)
                                            }
                                        }
                                    }
                                }

                                "version_history" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text("Riwayat Versi Cloud & Local Sync", fontSize = 12.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        listOf("Versi Terkini - Disimpan Hari Ini pukul 12.40", "Versi 2 - Diubah kemarin oleh MA", "Versi 1 - Inisialisasi ODF Template").forEach { ver ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        Toast.makeText(context, "Membuka $ver...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(ver, fontSize = 13.sp)
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
}
