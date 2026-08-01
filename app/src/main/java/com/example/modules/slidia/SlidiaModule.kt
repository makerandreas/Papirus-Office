package com.example.modules.slidia

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.SaveAsDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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

data class SlideItem(
    val id: Int,
    var title: String,
    var subtitle: String,
    var bullets: List<String> = emptyList(),
    var isMaster: Boolean = false,
    var showFooter: Boolean = true,
    var showSlideNumber: Boolean = true,
    var footerText: String = "Papirus Slidia Presentation",
    var chartImagePath: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidiaModule(
    isTablet: Boolean,
    onTransitionSelected: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Room DB Metadata Repository
    val inkyMetadataRepo = remember(context) {
        val db = com.makerandreas.papirusoffice.data.cache.DocumentDatabase.getInstance(context)
        com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataRepository(db.inkyDocumentMetadataDao())
    }

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

    // Dialog States for Slide Deck Manipulation & Metadata
    var showDocPropertiesDialog by remember { mutableStateOf(false) }
    var showImportNotesDialog by remember { mutableStateOf(false) }
    var showExtractTextDialog by remember { mutableStateOf(false) }
    var showAppendDeckDialog by remember { mutableStateOf(false) }
    var showMasterPageDialog by remember { mutableStateOf(false) }
    var showNewSlideDialog by remember { mutableStateOf(false) }
    var importedNotesText by remember { mutableStateOf("") }
    var globalMasterFooterText by remember { mutableStateOf("Papirus Slidia Presentation") }
    var isMasterViewActive by remember { mutableStateOf(false) }

    // Chapter 18: Slide Shows State Variables
    var isEndless by remember { mutableStateOf(false) }
    var autoChange by remember { mutableStateOf(false) }
    var transitionDurationSeconds by remember { mutableIntStateOf(3) }
    var transitionEffect by remember { mutableStateOf("Fade") } // "None", "Fade", "Dissolve", "Wipe", "Push", "Cut"
    var transitionSpeed by remember { mutableStateOf("Fast") } // "Slow", "Medium", "Fast"
    var usePenMode by remember { mutableStateOf(false) }
    var showCustomShowDialog by remember { mutableStateOf(false) }
    var showUniversalChartSheet by remember { mutableStateOf(false) }
    var customShowName by remember { mutableStateOf("Executive Summary") }
    var customShowIndicesText by remember { mutableStateOf("1, 3, 5") }
    var activePlaylistIndices by remember { mutableStateOf<List<Int>?>(null) }
    var playlistCurrentIndex by remember { mutableIntStateOf(0) }
    var isAutoAdvancing by remember { mutableStateOf(false) }

    // Loading Popup state
    var isLoadingDocument by remember { mutableStateOf(false) }
    var isCreatingDoc by remember { mutableStateOf(false) }
    var loadingDocName by remember { mutableStateOf(docTitle) }
    var loadingProgressStatus by remember { mutableStateOf("") }

    // Slide state (mutable list for dynamic slide deck manipulation)
    val slides = remember {
        mutableStateListOf(
            SlideItem(
                id = 1,
                title = "Papirus Suite Overview",
                subtitle = "A modular PC-level office suite for mobile and tablet.",
                bullets = listOf("Multi-platform support", "High-performance ODF/OOXML engines", "Low-end device optimization")
            ),
            SlideItem(
                id = 2,
                title = "Inky Core & ODF",
                subtitle = "Advanced WordprocessingML and OpenDocument text compliance layers.",
                bullets = listOf("LibreOffice Writer API compatibility", "Full XML styling & formatting", "Real-time pagination")
            ),
            SlideItem(
                id = 3,
                title = "Cellina Calculations",
                subtitle = "Cell references, formula parsers, and multi-sheet grids.",
                bullets = listOf("OpenFormula support", "Large dataset virtualization", "CSV & XLSX export")
            ),
            SlideItem(
                id = 4,
                title = "Slidia Slides",
                subtitle = "M3 design transitions and interactive slide shows.",
                bullets = listOf("Slide deck manipulation", "Master pages & layouts", "Notes import & image export")
            ),
            SlideItem(
                id = 5,
                title = "Pagella PDF Reader",
                subtitle = "PDF viewing, styling, annotations and fallbacks.",
                bullets = listOf("Vector rendering", "Text extraction", "Form filling")
            )
        )
    }

    var activeSlideIndex by remember { mutableIntStateOf(0) }
    var isSlideShowMode by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Ensure index stays valid if slides are removed
    if (activeSlideIndex >= slides.size) {
        activeSlideIndex = (slides.size - 1).coerceAtLeast(0)
    }

    val activeSlide = slides.getOrElse(activeSlideIndex) { slides[0] }
    val moduleColor = Color(0xFFD97706) // Impress Amber/Orange

    // Helper: Sync Document Metadata to Room DB
    val updateSlidiaMetadata: suspend (String, String) -> Unit = { path, name ->
        val existing = inkyMetadataRepo.getMetadata(path)
        val totalWords = slides.sumOf { slide ->
            val text = "${slide.title} ${slide.subtitle} ${slide.bullets.joinToString(" ")}"
            if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).count { word -> word.any { it.isLetterOrDigit() } }
        }
        val totalChars = slides.sumOf { slide ->
            "${slide.title} ${slide.subtitle} ${slide.bullets.joinToString(" ")}".length
        }
        val now = System.currentTimeMillis()
        val entity = com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataEntity(
            filePath = path,
            fileName = name,
            createdAt = existing?.createdAt ?: now,
            lastModifiedAt = now,
            author = existing?.author ?: "Papirus Slidia User",
            wordCount = totalWords,
            characterCount = totalChars,
            paragraphCount = slides.size,
            fileType = if (name.endsWith(".pptx", ignoreCase = true)) "PPTX" else "ODP"
        )
        inkyMetadataRepo.saveOrUpdateMetadata(entity)
    }

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
                        .width(112.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        slides.forEachIndexed { index, slide ->
                            val isSelected = index == activeSlideIndex
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clickable {
                                        activeSlideIndex = index
                                        if (!isEditMode) isEditMode = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) moduleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) moduleColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "${index + 1}. ${slide.title}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) moduleColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isSelected && isEditMode) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (index > 0) {
                                                        val temp = slides[index]
                                                        slides[index] = slides[index - 1]
                                                        slides[index - 1] = temp
                                                        activeSlideIndex = index - 1
                                                    }
                                                },
                                                enabled = index > 0,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(10.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (index < slides.size - 1) {
                                                        val temp = slides[index]
                                                        slides[index] = slides[index + 1]
                                                        slides[index + 1] = temp
                                                        activeSlideIndex = index + 1
                                                    }
                                                },
                                                enabled = index < slides.size - 1,
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(10.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    val copy = slide.copy(id = slides.size + 1, title = "${slide.title} (Copy)")
                                                    slides.add(index + 1, copy)
                                                    activeSlideIndex = index + 1
                                                },
                                                modifier = Modifier.size(18.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add Slide Quick Button
                    OutlinedButton(
                        onClick = { showNewSlideDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = moduleColor)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+ Slide", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMasterViewActive) Color(0xFFFFFBEB) else Color.White
                        ),
                        border = if (isMasterViewActive) BorderStroke(2.dp, moduleColor) else null
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                            // Header badge if Master View Active
                            if (isMasterViewActive) {
                                Surface(
                                    color = moduleColor,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "MASTER SLIDE LAYOUT",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center)
                                    .padding(vertical = 16.dp),
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
                                if (activeSlide.subtitle.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = activeSlide.subtitle,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center,
                                        color = Color.DarkGray
                                    )
                                }

                                // Bullet List Section
                                if (activeSlide.bullets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Column(
                                        modifier = Modifier.fillMaxWidth(0.85f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        activeSlide.bullets.forEach { bullet ->
                                            Row(verticalAlignment = Alignment.Top) {
                                                Text("• ", fontWeight = FontWeight.Bold, color = moduleColor, fontSize = 16.sp)
                                                Text(bullet, fontSize = 14.sp, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            // Slide Master Footer & Slide Number
                            if (activeSlide.showFooter || activeSlide.showSlideNumber) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (activeSlide.showFooter) {
                                        Text(
                                            text = activeSlide.footerText,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    if (activeSlide.showSlideNumber) {
                                        Text(
                                            text = "${activeSlideIndex + 1}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
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

                    // Standard Bottom Sheet Body Ribbon Switcher
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        when (activeRibbonTab) {
                            "File" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("File & Document Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { handleSaveCommand() }) {
                                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save")
                                        }
                                        OutlinedButton(onClick = { showSaveAsDialog = true }) {
                                            Icon(Icons.Rounded.SaveAs, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save As...")
                                        }
                                        OutlinedButton(onClick = { showDocPropertiesDialog = true }) {
                                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Document Properties")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Slide Deck Manipulation (SDK Ch. 17)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { showImportNotesDialog = true }) {
                                            Icon(Icons.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Import Notes to Deck")
                                        }
                                        OutlinedButton(onClick = { showAppendDeckDialog = true }) {
                                            Icon(Icons.Rounded.LibraryAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Append Deck")
                                        }
                                        OutlinedButton(onClick = { showExtractTextDialog = true }) {
                                            Icon(Icons.Rounded.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Extract All Text")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = {
                                            Toast.makeText(context, "Exported Slide ${activeSlideIndex + 1} as PNG image!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Export Slide as Image")
                                        }
                                    }
                                }
                            }
                            "Home", "Insert" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Slide Operations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { showNewSlideDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = moduleColor)) {
                                            Icon(Icons.Rounded.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("New Slide")
                                        }
                                        OutlinedButton(onClick = {
                                            val copy = activeSlide.copy(id = slides.size + 1, title = "${activeSlide.title} (Copy)")
                                            slides.add(activeSlideIndex + 1, copy)
                                            activeSlideIndex++
                                            Toast.makeText(context, "Slide duplicated!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Duplicate Slide")
                                        }
                                        OutlinedButton(onClick = {
                                            if (slides.size > 1) {
                                                slides.removeAt(activeSlideIndex)
                                                if (activeSlideIndex >= slides.size) activeSlideIndex = slides.size - 1
                                                Toast.makeText(context, "Slide deleted!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Cannot delete sole slide", Toast.LENGTH_SHORT).show()
                                            }
                                        }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Delete Slide")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Reorder Active Slide", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                if (activeSlideIndex > 0) {
                                                    val temp = slides[activeSlideIndex]
                                                    slides[activeSlideIndex] = slides[activeSlideIndex - 1]
                                                    slides[activeSlideIndex - 1] = temp
                                                    activeSlideIndex--
                                                }
                                            },
                                            enabled = activeSlideIndex > 0
                                        ) {
                                            Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Move Up")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                if (activeSlideIndex < slides.size - 1) {
                                                    val temp = slides[activeSlideIndex]
                                                    slides[activeSlideIndex] = slides[activeSlideIndex + 1]
                                                    slides[activeSlideIndex + 1] = temp
                                                    activeSlideIndex++
                                                }
                                            },
                                            enabled = activeSlideIndex < slides.size - 1
                                        ) {
                                            Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Move Down")
                                        }
                                    }
                                }
                            }
                            "Layout", "View" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Master Slide & Layout Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = isMasterViewActive,
                                            onClick = { isMasterViewActive = !isMasterViewActive },
                                            label = { Text("Master Slide View") },
                                            leadingIcon = { Icon(Icons.Rounded.ViewCarousel, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        OutlinedButton(onClick = { showMasterPageDialog = true }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Footer Text & Numbers")
                                        }
                                    }
                                }
                            }
                            "Transitions" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Slide Transition Effects (SDK Ch. 18)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("None", "Fade", "Dissolve", "Wipe", "Push", "Cut").forEach { effect ->
                                            FilterChip(
                                                selected = transitionEffect == effect,
                                                onClick = { transitionEffect = effect },
                                                label = { Text(effect) }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Transition Timing & Speed", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = autoChange,
                                            onClick = { autoChange = !autoChange },
                                            label = { Text("Auto Advance (${transitionDurationSeconds}s)") },
                                            leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        listOf(1, 2, 3, 5).forEach { dur ->
                                            FilterChip(
                                                selected = transitionDurationSeconds == dur,
                                                onClick = { transitionDurationSeconds = dur },
                                                label = { Text("${dur}s") }
                                            )
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Speed: ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        listOf("Slow", "Medium", "Fast").forEach { speed ->
                                            FilterChip(
                                                selected = transitionSpeed == speed,
                                                onClick = { transitionSpeed = speed },
                                                label = { Text(speed) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { Toast.makeText(context, "Applied '$transitionEffect' transition to all slides!", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                    ) {
                                        Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Apply Transition to All Slides")
                                    }
                                }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Presentation Controls (XPresentation2 / XSlideShowController)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                activePlaylistIndices = null
                                                playlistCurrentIndex = 0
                                                activeSlideIndex = 0
                                                isSlideShowMode = true
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("From Beginning")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                activePlaylistIndices = null
                                                playlistCurrentIndex = activeSlideIndex
                                                isSlideShowMode = true
                                            }
                                        ) {
                                            Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("From Slide ${activeSlideIndex + 1}")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Presentation Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = isEndless,
                                            onClick = { isEndless = !isEndless },
                                            label = { Text("Endless Loop (IsEndless)") },
                                            leadingIcon = { Icon(Icons.Rounded.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        FilterChip(
                                            selected = usePenMode,
                                            onClick = { usePenMode = !usePenMode },
                                            label = { Text("Pointer / Pen Mode (UsePen)") },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { showCustomShowDialog = true }) {
                                            Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Custom Show Playlist...")
                                        }
                                        if (activePlaylistIndices != null) {
                                            Button(
                                                onClick = {
                                                    playlistCurrentIndex = 0
                                                    isSlideShowMode = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                            ) {
                                                Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Start '$customShowName'")
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
                title = { Text(stringResource(R.string.unsaved_changes_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.unsaved_changes_msg, docTitle), style = MaterialTheme.typography.bodyMedium) },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnsavedChangesDialog = false
                            performSaveWithPopup(docTitle, false) {
                                pendingActionAfterSave?.invoke()
                                pendingActionAfterSave = null
                            }
                        },
                        modifier = Modifier.testTag("btn_unsaved_save_slidia")
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
                        modifier = Modifier.testTag("btn_unsaved_dont_save_slidia")
                    ) {
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

        // --- DOCUMENT PROPERTIES DIALOG (ROOM DB METADATA) ---
        if (showDocPropertiesDialog) {
            var meta by remember { mutableStateOf<com.makerandreas.papirusoffice.data.cache.InkyDocumentMetadataEntity?>(null) }
            LaunchedEffect(docTitle) {
                val currentPath = com.example.MainActivity.openedFilePath ?: "templates/slidia/Default.otp"
                updateSlidiaMetadata(currentPath, docTitle)
                meta = inkyMetadataRepo.getMetadata(currentPath)
            }
            AlertDialog(
                onDismissRequest = { showDocPropertiesDialog = false },
                icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = moduleColor) },
                title = { Text("Document Properties") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        meta?.let { m ->
                            val dateFmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            Text("File Name: ${m.fileName}", fontWeight = FontWeight.Bold)
                            Text("Format: ${m.fileType}")
                            Text("Author: ${m.author}")
                            Text("Created: ${dateFmt.format(java.util.Date(m.createdAt))}")
                            Text("Last Modified: ${dateFmt.format(java.util.Date(m.lastModifiedAt))}")
                            HorizontalDivider()
                            Text("Total Slides: ${m.paragraphCount}", fontWeight = FontWeight.Bold)
                            Text("Total Words: ${m.wordCount}")
                            Text("Total Characters: ${m.characterCount}")
                        } ?: CircularProgressIndicator()
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDocPropertiesDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // --- IMPORT NOTES TO DECK DIALOG ---
        if (showImportNotesDialog) {
            AlertDialog(
                onDismissRequest = { showImportNotesDialog = false },
                icon = { Icon(Icons.Rounded.NoteAdd, contentDescription = null, tint = moduleColor) },
                title = { Text("Import Notes to Slide Deck") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Paste text notes below. Lines starting with '>' or '-' will be imported as bullet points.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = importedNotesText,
                            onValueChange = { importedNotesText = it },
                            placeholder = { Text("What is an Algorithm?\n> An algorithm is a finite set of instructions.\n> Must terminate in finite time.") },
                            modifier = Modifier.fillMaxWidth().height(140.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importedNotesText.isNotBlank()) {
                                val lines = importedNotesText.lines()
                                var currentTitle = "Imported Notes"
                                val currentBullets = mutableListOf<String>()
                                lines.forEach { line ->
                                    val trimmed = line.trim()
                                    if (trimmed.startsWith(">") || trimmed.startsWith("-") || trimmed.startsWith("*")) {
                                        currentBullets.add(trimmed.removePrefix(">").removePrefix("-").removePrefix("*").trim())
                                    } else if (trimmed.isNotBlank()) {
                                        if (currentBullets.isNotEmpty()) {
                                            slides.add(SlideItem(slides.size + 1, currentTitle, "", currentBullets.toList()))
                                            currentBullets.clear()
                                        }
                                        currentTitle = trimmed
                                    }
                                }
                                if (currentTitle.isNotBlank() || currentBullets.isNotEmpty()) {
                                    slides.add(SlideItem(slides.size + 1, currentTitle, "Imported from notes", currentBullets.toList()))
                                }
                                activeSlideIndex = slides.size - 1
                                Toast.makeText(context, "Slides built from notes!", Toast.LENGTH_SHORT).show()
                            }
                            showImportNotesDialog = false
                            importedNotesText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Build Deck")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportNotesDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- EXTRACT TEXT FROM DECK DIALOG ---
        if (showExtractTextDialog) {
            val extractedText = remember(slides.size) {
                slides.mapIndexed { idx, s ->
                    "Slide ${idx + 1}: ${s.title}\n${s.subtitle}\n" +
                            s.bullets.joinToString("\n") { "  • $it" }
                }.joinToString("\n\n")
            }
            AlertDialog(
                onDismissRequest = { showExtractTextDialog = false },
                icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = moduleColor) },
                title = { Text("Extracted Presentation Text") },
                text = {
                    Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                        Text(extractedText, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Presentation Text", extractedText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showExtractTextDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Copy Text")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExtractTextDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // --- APPEND DECK DIALOG ---
        if (showAppendDeckDialog) {
            AlertDialog(
                onDismissRequest = { showAppendDeckDialog = false },
                icon = { Icon(Icons.Rounded.LibraryAdd, contentDescription = null, tint = moduleColor) },
                title = { Text("Append Presentation Deck") },
                text = {
                    Text("Append pre-built slide template (Inspiration_Template.otp) to current presentation deck.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val nextId = slides.size + 1
                            slides.add(SlideItem(nextId, "Appended: Inspiration Template", "Design & Architecture", listOf("Modular widgets", "Custom palette", "Dynamic layouts")))
                            slides.add(SlideItem(nextId + 1, "Appended: Financial Summary", "Q3/Q4 Performance", listOf("Revenue Growth +24%", "Operating Expenses -8%", "Profit Margin +12%")))
                            activeSlideIndex = slides.size - 1
                            Toast.makeText(context, "Appended 2 slides from external template!", Toast.LENGTH_SHORT).show()
                            showAppendDeckDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Append Deck")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAppendDeckDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- MASTER PAGE DIALOG ---
        if (showMasterPageDialog) {
            AlertDialog(
                onDismissRequest = { showMasterPageDialog = false },
                icon = { Icon(Icons.Rounded.ViewCarousel, contentDescription = null, tint = moduleColor) },
                title = { Text("Master Page Settings") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Configure default footer text and master layout for all slides.")
                        OutlinedTextField(
                            value = globalMasterFooterText,
                            onValueChange = { globalMasterFooterText = it },
                            label = { Text("Footer Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            slides.forEach { it.footerText = globalMasterFooterText }
                            Toast.makeText(context, "Updated master page footer for all slides", Toast.LENGTH_SHORT).show()
                            showMasterPageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Apply to All")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMasterPageDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- NEW SLIDE DIALOG ---
        if (showNewSlideDialog) {
            AlertDialog(
                onDismissRequest = { showNewSlideDialog = false },
                icon = { Icon(Icons.Rounded.AddBox, contentDescription = null, tint = moduleColor) },
                title = { Text("Add New Slide") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                val newIndex = activeSlideIndex + 1
                                slides.add(newIndex, SlideItem(slides.size + 1, "New Title & Subtitle", "Click to edit text"))
                                activeSlideIndex = newIndex
                                showNewSlideDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Title & Subtitle Layout")
                        }
                        OutlinedButton(
                            onClick = {
                                val newIndex = activeSlideIndex + 1
                                slides.add(newIndex, SlideItem(slides.size + 1, "New Bullet Slide", "Key Takeaways", listOf("Point 1", "Point 2", "Point 3")))
                                activeSlideIndex = newIndex
                                showNewSlideDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bullet List Layout")
                        }
                        OutlinedButton(
                            onClick = {
                                val newIndex = activeSlideIndex + 1
                                slides.add(newIndex, SlideItem(slides.size + 1, "Blank Slide", ""))
                                activeSlideIndex = newIndex
                                showNewSlideDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Blank Slide")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showNewSlideDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- CUSTOM SHOW (PLAYLIST) DIALOG (SDK CHAPTER 18) ---
        if (showCustomShowDialog) {
            AlertDialog(
                onDismissRequest = { showCustomShowDialog = false },
                icon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = moduleColor) },
                title = { Text("Build Custom Slide Show (Playlist)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Create a non-linear display sequence (playlist) referencing slide numbers.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = customShowName,
                            onValueChange = { customShowName = it },
                            label = { Text("Custom Show Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = customShowIndicesText,
                            onValueChange = { customShowIndicesText = it },
                            label = { Text("Slide Numbers (e.g., 1, 3, 5)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsed = customShowIndicesText.split(",")
                                .mapNotNull { it.trim().toIntOrNull() }
                                .map { (it - 1).coerceIn(0, slides.size - 1) }
                            if (parsed.isNotEmpty()) {
                                activePlaylistIndices = parsed
                                playlistCurrentIndex = 0
                                isSlideShowMode = true
                                Toast.makeText(context, "Custom Show '$customShowName' created with ${parsed.size} slides!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid slide numbers", Toast.LENGTH_SHORT).show()
                            }
                            showCustomShowDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Start Custom Show")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomShowDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- FULLSCREEN SLIDE SHOW OVERLAY (SDK CHAPTER 18) ---
        if (isSlideShowMode) {
            val totalInPlay = activePlaylistIndices?.size ?: slides.size
            val currentSlideIdxInDeck = activePlaylistIndices?.getOrNull(playlistCurrentIndex)
                ?: playlistCurrentIndex.coerceIn(0, (slides.size - 1).coerceAtLeast(0))
            val currentSlideObj = slides.getOrElse(currentSlideIdxInDeck) { slides[0] }

            // Automatic Timer Coroutine for Slide Advance
            LaunchedEffect(isSlideShowMode, isAutoAdvancing, autoChange, playlistCurrentIndex) {
                if (isSlideShowMode && (isAutoAdvancing || autoChange)) {
                    val delayMs = (transitionDurationSeconds * 1000L).coerceAtLeast(1000L)
                    delay(delayMs)
                    if (playlistCurrentIndex < totalInPlay - 1) {
                        playlistCurrentIndex++
                    } else if (isEndless) {
                        playlistCurrentIndex = 0
                    } else {
                        isAutoAdvancing = false
                        Toast.makeText(context, "End of presentation", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Interactive Slide View with Transition Animations
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp, top = 16.dp, start = 16.dp, end = 16.dp)
                        .clickable {
                            if (playlistCurrentIndex < totalInPlay - 1) {
                                playlistCurrentIndex++
                            } else if (isEndless) {
                                playlistCurrentIndex = 0
                            } else {
                                Toast.makeText(context, "End of presentation", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = currentSlideObj,
                        transitionSpec = {
                            when (transitionEffect) {
                                "Fade" -> fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                                "Wipe" -> slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                                "Push" -> slideInVertically { height -> height } + fadeIn() togetherWith slideOutVertically { height -> -height } + fadeOut()
                                "Dissolve" -> scaleIn(initialScale = 0.85f) + fadeIn() togetherWith scaleOut(targetScale = 1.15f) + fadeOut()
                                else -> fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
                            }
                        },
                        label = "FullscreenSlideTransition"
                    ) { slide ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f),
                            elevation = CardDefaults.cardElevation(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = slide.title,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = Color.Black
                                    )
                                    if (slide.subtitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = slide.subtitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            textAlign = TextAlign.Center,
                                            color = Color.DarkGray
                                        )
                                    }
                                    if (slide.bullets.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Column(
                                            modifier = Modifier.fillMaxWidth(0.85f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            slide.bullets.forEach { bullet ->
                                                Row(verticalAlignment = Alignment.Top) {
                                                    Text("• ", fontWeight = FontWeight.Bold, color = moduleColor, fontSize = 20.sp)
                                                    Text(bullet, fontSize = 18.sp, color = Color.Black)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Master Footer & Number
                                if (slide.showFooter || slide.showSlideNumber) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (slide.showFooter) {
                                            Text(slide.footerText, fontSize = 12.sp, color = Color.Gray)
                                        }
                                        if (slide.showSlideNumber) {
                                            Text("${currentSlideIdxInDeck + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Laser Pointer / Pen Mode Indicator Overlay
                if (usePenMode) {
                    Surface(
                        color = Color.Red.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color.Red, RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Laser Pointer Active (UsePen)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // On-Screen Presenter Controls Bar (Bottom)
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (playlistCurrentIndex > 0) playlistCurrentIndex--
                                },
                                enabled = playlistCurrentIndex > 0
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Slide", tint = Color.White)
                            }
                            IconButton(
                                onClick = { isAutoAdvancing = !isAutoAdvancing }
                            ) {
                                Icon(
                                    imageVector = if (isAutoAdvancing || autoChange) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause Auto Advance",
                                    tint = moduleColor
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (playlistCurrentIndex < totalInPlay - 1) {
                                        playlistCurrentIndex++
                                    } else if (isEndless) {
                                        playlistCurrentIndex = 0
                                    }
                                },
                                enabled = playlistCurrentIndex < totalInPlay - 1 || isEndless
                            ) {
                                Icon(Icons.Default.ArrowForward, contentDescription = "Next Slide", tint = Color.White)
                            }
                            Text(
                                text = "Slide ${playlistCurrentIndex + 1} / $totalInPlay",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activePlaylistIndices != null) {
                                Surface(
                                    color = moduleColor.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = customShowName,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { usePenMode = !usePenMode }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Toggle Laser Pen",
                                    tint = if (usePenMode) Color.Red else Color.White
                                )
                            }
                            IconButton(onClick = { isSlideShowMode = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Slideshow", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
