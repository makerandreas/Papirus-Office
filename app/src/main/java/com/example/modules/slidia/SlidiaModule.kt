package com.example.modules.slidia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.SaveAsDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.FullPageDocumentLoadingPopup
import com.example.ui.components.SavingProgressPopupDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class SlideItem(val id: Int, val title: String, val subtitle: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidiaModule(
    isTablet: Boolean,
    onTransitionSelected: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Mode state
    var isEditMode by remember { mutableStateOf(false) }
    var docTitle by remember { mutableStateOf("Slidia_Presentation.odp") }
    var isSaved by remember { mutableStateOf(true) }
    var isNewDocument by remember { mutableStateOf(com.example.MainActivity.openedFilePath == null) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var currentSaveMimeType by remember { mutableStateOf("application/vnd.oasis.opendocument.presentation") }
    var currentSaveDefaultFilename by remember { mutableStateOf("Slidia_Presentation.odp") }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var showSaveFailedDialog by remember { mutableStateOf(false) }
    var showSavingProgressPopup by remember { mutableStateOf(false) }
    var savingProgressDocName by remember { mutableStateOf(docTitle) }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showBottomBar by remember { mutableStateOf(false) }
    var activeRibbonTab by remember { mutableStateOf("Home") }
    var isWebView by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var pendingActionAfterSave by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Loading Popup state
    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var loadingDocName by remember { mutableStateOf(docTitle) }
    var loadingProgressStatus by remember { mutableStateOf("") }

    // Slide state
    val slides = remember {
        listOf(
            SlideItem(1, "Papirus Suite Overview", "A modular PC-level office suite for mobile and tablet."),
            SlideItem(2, "Inky Core & ODF", "Advanced WordprocessingML and OpenDocument text compliance layers."),
            SlideItem(3, "Cellina Calculations", "Cell references, formula parsers, and multi-sheet grids."),
            SlideItem(4, "Slidia Slides", "M3 design transitions and interactive slide shows."),
            SlideItem(5, "Pagella PDF Reader", "PDF viewing, styling, annotations and fallbacks.")
        )
    }

    var activeSlideIndex by remember { mutableIntStateOf(0) }
    var isSlideShowMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    val activeSlide = slides[activeSlideIndex]
    val moduleColor = Color(0xFFD97706) // Impress Amber/Orange

    val performSave = { simulateError: Boolean ->
        coroutineScope.launch {
            isSaving = true
            saveFailed = false
            delay(1000)
            if (simulateError) {
                isSaving = false
                saveFailed = true
                showSaveFailedDialog = true
            } else {
                isSaving = false
                isSaved = true
                saveFailed = false
                Toast.makeText(context, "Document saved", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val performSaveWithPopup = { docName: String, simulateError: Boolean, onSuccess: (() -> Unit)? ->
        coroutineScope.launch {
            showSavingProgressPopup = true
            savingProgressDocName = docName
            isSaving = true
            saveFailed = false
            delay(1200)
            showSavingProgressPopup = false
            isSaving = false
            if (simulateError) {
                saveFailed = true
                showSaveFailedDialog = true
            } else {
                isSaved = true
                saveFailed = false
                Toast.makeText(context, "Document saved", Toast.LENGTH_SHORT).show()
                onSuccess?.invoke()
            }
        }
    }

    val saveDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(currentSaveMimeType)
    ) { uri ->
        uri?.let {
            var savedName = currentSaveDefaultFilename
            try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            savedName = c.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            docTitle = savedName
            isSaved = true
            isNewDocument = false
            Toast.makeText(context, context.getString(R.string.doc_saved_success, savedName), Toast.LENGTH_SHORT).show()
            pendingActionAfterSave?.invoke()
            pendingActionAfterSave = null
        }
    }

    val handleSaveCommand: () -> Unit = {
        if (isNewDocument) {
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

    if (isSlideShowMode) {
        // Fullscreen Slide Show Mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("slideshow_canvas")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = activeSlide.title,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = activeSlide.subtitle,
                    color = Color.LightGray,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { if (activeSlideIndex > 0) activeSlideIndex-- },
                    enabled = activeSlideIndex > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Slide", tint = Color.White)
                }

                Text(
                    text = "${activeSlideIndex + 1} / ${slides.size}",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { if (activeSlideIndex < slides.size - 1) activeSlideIndex++ },
                        enabled = activeSlideIndex < slides.size - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Next Slide", tint = Color.White)
                    }

                    IconButton(
                        onClick = { isSlideShowMode = false },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Red.copy(alpha = 0.3f))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Exit Slideshow", tint = Color.White)
                    }
                }
            }
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER TOP APP BAR ---
            if (!isEditMode) {
                // VIEWER MODE APP BAR
                TopAppBar(
                    title = {
                        Text(
                            text = docTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (!isSaved) {
                                pendingActionAfterSave = { onBack() }
                                showUnsavedChangesDialog = true
                            } else {
                                onBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Toast.makeText(context, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload to Drive")
                        }
                        IconButton(onClick = {
                            isWebView = !isWebView
                            Toast.makeText(context, if (isWebView) "Mobile View Active" else "Normal View Active", Toast.LENGTH_SHORT).show()
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
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as...") },
                                    onClick = {
                                        showMoreMenu = false
                                        showSaveAsDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = "Save As") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simulate Save Error") },
                                    onClick = {
                                        showMoreMenu = false
                                        performSave(true)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Simulate Error", tint = MaterialTheme.colorScheme.error) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, "Printing presentation...", Toast.LENGTH_SHORT).show()
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
                // EDIT MODE APP BAR
                TopAppBar(
                    title = { /* Headline & Subtitle removed in Edit Mode */ },
                    navigationIcon = {
                        IconButton(onClick = { isEditMode = false }) {
                            Icon(Icons.Default.Check, contentDescription = "Exit Edit Mode", tint = moduleColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Toast.makeText(context, "Uploading to Google Drive...", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = "Upload to Drive")
                        }
                        IconButton(onClick = {
                            isWebView = !isWebView
                            Toast.makeText(context, if (isWebView) "Mobile View" else "Normal View", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                contentDescription = "Document View Mode"
                            )
                        }
                        IconButton(onClick = { handleSaveCommand() }) {
                            Icon(Icons.Rounded.Save, contentDescription = "Save")
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
                                    text = { Text("Save") },
                                    onClick = {
                                        showMoreMenu = false
                                        handleSaveCommand()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = "Save") }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simulate Save Error") },
                                    onClick = {
                                        showMoreMenu = false
                                        performSave(true)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Simulate Error", tint = MaterialTheme.colorScheme.error) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to PDF") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, "Exporting to PDF...", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = "PDF") }
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

                // TOP STATUS BAR (Semi-transparent directly below App Bar)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
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
                            text = docTitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = moduleColor
                                )
                                Text(
                                    text = stringResource(R.string.status_saving),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = moduleColor
                                )
                            } else if (saveFailed) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.status_save_failed),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else if (isSaved) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = moduleColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.status_saved),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = moduleColor
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.status_unsaved),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // --- PRESENTATION CANVAS & SLIDE THUMBNAILS ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
            ) {
                // Left Slide Navigation Thumbnails
                Column(
                    modifier = Modifier
                        .width(96.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    slides.forEachIndexed { index, slide ->
                        val isSelected = index == activeSlideIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable {
                                    activeSlideIndex = index
                                    if (!isEditMode) isEditMode = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) moduleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) moduleColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Slide ${slide.id}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) moduleColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Main Slide Editor Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { if (!isEditMode) isEditMode = true },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = activeSlide.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = activeSlide.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // --- BOTTOM TOOLBAR HUB / TRIGGER (Edit Mode Only) ---
            AnimatedVisibility(
                visible = isEditMode && !showBottomBar,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { isSlideShowMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = moduleColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start Slide Show", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { showBottomBar = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = moduleColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewAgenda,
                                contentDescription = "Open Standard Bottom Sheet",
                                tint = moduleColor
                            )
                        }
                    }
                }
            }

            // --- FOOTER STATS & STATUS BAR (Edit Mode Only) ---
            AnimatedVisibility(
                visible = isEditMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
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
                            text = "Slide ${activeSlideIndex + 1} of ${slides.size}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (zoomScale > 0.5f) zoomScale -= 0.1f },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { if (zoomScale < 2.0f) zoomScale += 0.1f },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }
        }

        // --- PERSISTENT STANDARD BOTTOM SHEET ---
        AnimatedVisibility(
            visible = showBottomBar,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.40f),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar: Ribbon Tabs on Left, Persistent Controls on Right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Ribbon Tabs Horizontal Scroll
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tabs = listOf("File", "Home", "Insert", "Layout", "Transitions", "Animations", "Slide Show", "Review", "View")
                            tabs.forEach { tab ->
                                val isSelected = activeRibbonTab == tab
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (isSelected) moduleColor.copy(alpha = 0.2f)
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
                                        color = if (isSelected) moduleColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Vertical Divider
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .width(1.dp)
                                .height(24.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )

                        // Trailing Persistent Controls: Undo, Redo, Close
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(onClick = { Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Rounded.Undo, contentDescription = "Undo", tint = moduleColor)
                            }
                            IconButton(onClick = { Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.Rounded.Redo, contentDescription = "Redo", tint = moduleColor)
                            }
                            IconButton(onClick = { showBottomBar = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close Standard Bottom Sheet", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Placeholder Content for Standard Bottom Sheet Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Slidia $activeRibbonTab Ribbon Options",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- FLOATING ACTION BUTTON (Viewer Mode Only) ---
        if (!isEditMode && !showBottomBar) {
            ExtendedFloatingActionButton(
                onClick = { 
                    isEditMode = true
                },
                icon = { Icon(Icons.Default.Edit, contentDescription = "Edit") },
                text = { Text("Edit Presentation") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("fab_edit_presentation"),
                containerColor = moduleColor,
                contentColor = Color.White
            )
        }

        // --- FULL-PAGE DOCUMENT LOADING POPUP ---
        if (isLoadingDocument) {
            FullPageDocumentLoadingPopup(
                isCreating = isCreatingDoc,
                docName = loadingDocName,
                progressStatus = loadingProgressStatus,
                moduleColor = moduleColor
            )
        }

        // --- SAVING PROGRESS POPUP DIALOG ---
        if (showSavingProgressPopup) {
            SavingProgressPopupDialog(
                docName = savingProgressDocName,
                moduleColor = moduleColor
            )
        }

        // --- SAVE FAILURE POPUP ---
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
                            }
                        ) {
                            Text(stringResource(R.string.btn_exit_without_saving), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }

        // --- UNSAVED CHANGES DIALOG ---
        if (showUnsavedChangesDialog) {
            AlertDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                title = { Text(stringResource(R.string.unsaved_changes_title), style = MaterialTheme.typography.headlineSmall) },
                text = { Text(stringResource(R.string.unsaved_changes_msg, docTitle), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        performSaveWithPopup(docTitle, false) {
                            pendingActionAfterSave?.invoke()
                            pendingActionAfterSave = null
                        }
                    }) {
                        Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        pendingActionAfterSave?.invoke()
                        pendingActionAfterSave = null
                    }) {
                        Text(stringResource(R.string.dont_save), color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        if (showSaveAsDialog) {
            SaveAsDialog(
                moduleType = "Slidia",
                currentTitle = docTitle,
                onDismiss = { showSaveAsDialog = false },
                onConfirmSave = { selectedFormat, extension, mimeType ->
                    currentSaveMimeType = mimeType
                    val baseName = docTitle.substringBeforeLast(".")
                    currentSaveDefaultFilename = if (baseName.isBlank()) "Slidia_Presentation$extension" else "$baseName$extension"
                    showSaveAsDialog = false
                    saveDocumentLauncher.launch(currentSaveDefaultFilename)
                }
            )
        }
    }
}
