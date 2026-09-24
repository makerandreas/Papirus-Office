package com.example.modules.cellina

import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
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
import com.example.ui.components.FloatingContextualToolbar
import com.example.ui.components.FullPageDocumentLoadingPopup
import com.example.ui.components.SavingProgressPopupDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The sheet preview is a simulated spreadsheet on a pinned light board
// (0xFFF1F5F9) with pinned white cells, so its grid fills and hairlines are
// document colours, not chrome: they keep their Material-2014 values as
// explicit ARGB and stay legible whatever the app theme does (plan-03 3.11).
private val SheetGridFill = Color(0xFFCCCCCC)
private val SheetGridLine = Color(0xFF888888)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CellinaModule(
    isTablet: Boolean,
    onFormulaSelected: (String) -> Unit,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bridge = remember { com.makerandreas.papirusoffice.data.bridge.PapirusSdkBridge.getInstance() }

    val docxParser = remember { com.makerandreas.papirusoffice.data.DocxDocumentParser(context) }
    var showDocOpenFailedDialog by remember { mutableStateOf(false) }
    var docOpenFailedError by remember { mutableStateOf<String?>(null) }

    // Mode state
    var isEditMode by remember { mutableStateOf(false) }
    var docTitle by remember { mutableStateOf("Cellina_Data.ods") }
    var isSaved by remember { mutableStateOf(true) }
    var isNewDocument by remember { mutableStateOf(com.example.MainActivity.openedFilePath == null) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var currentSaveMimeType by remember { mutableStateOf("application/vnd.oasis.opendocument.spreadsheet") }
    var currentSaveDefaultFilename by remember { mutableStateOf("Cellina_Data.ods") }
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

    // Spreadsheet state
    var selectedSheet by remember { mutableStateOf("Sheet1") }
    val sheets = listOf("Sheet1", "Sheet2", "Sheet3")
    
    // Formula bar logic
    var formulaText by remember { mutableStateOf("=SUM(B2:C3)") }
    var activeCellRow by remember { mutableStateOf(2) } // 1-indexed for spreadsheet
    var activeCellCol by remember { mutableStateOf(2) } // 2 = 'B'

    // FCT state
    var showFct by remember { mutableStateOf(true) }
    var selectionMode by remember { mutableStateOf("single") } // "single" or "multi"

    // Zoom scale state
    var zoomScale by remember { mutableFloatStateOf(1.0f) }

    // Chapter 19: Calc API State Variables
    var selectedChartType by remember { mutableStateOf("BarDiagram") } // "LineDiagram", "BarDiagram", "PieDiagram", "NetDiagram", "XYDiagram", "StockDiagram", "AreaDiagram"
    var isChart3D by remember { mutableStateOf(true) }
    var currencySymbol by remember { mutableStateOf("EUR") } // "EUR", "USD", "DM", "IDR"
    var isFrozenPane by remember { mutableStateOf(false) }
    var frozenColIndex by remember { mutableIntStateOf(1) }
    var frozenRowIndex by remember { mutableIntStateOf(1) }
    var showGridLines by remember { mutableStateOf(true) }
    var showDataPilotDialog by remember { mutableStateOf(false) }
    var showAddInDialog by remember { mutableStateOf(false) }
    var selectedAddInFunc by remember { mutableStateOf("getMyFirstValue") }
    var addInResultText by remember { mutableStateOf("") }
    var currentScenarioName by remember { mutableStateOf("Base Plan") }
    var cellCommentText by remember { mutableStateOf("") }
    var showCommentDialog by remember { mutableStateOf(false) }
    var showUniversalFormsSheet by remember { mutableStateOf(false) }

    // Simulate cell data values
    val columnsLabels = listOf("A", "B", "C", "D", "E")
    val cellValues = remember {
        mutableStateMapOf(
            "A1" to "Quarter", "B1" to "Inky Sales", "C1" to "Cellina Sales", "D1" to "Total",
            "A2" to "Q1", "B2" to "12000", "C2" to "15000", "D2" to "27000",
            "A3" to "Q2", "B3" to "14500", "C3" to "18200", "D3" to "32700",
            "A4" to "Q3", "B4" to "16000", "C4" to "21000", "D4" to "37000",
            "A5" to "Average", "B5" to "14166", "C5" to "18066", "D5" to "32233"
        )
    }

    DisposableEffect(Unit) {
        val observer = androidx.lifecycle.Observer<com.makerandreas.papirusoffice.data.ParsingProgress> { progress ->
            progress?.let {
                loadingProgressStatus = it.statusMessage
                if (it.isFailed) {
                    isLoadingDocument = false
                    docOpenFailedError = it.errorMessage
                    showDocOpenFailedDialog = true
                }
            }
        }
        docxParser.parsingProgress.observeForever(observer)
        onDispose {
            docxParser.parsingProgress.removeObserver(observer)
        }
    }

    LaunchedEffect(com.example.MainActivity.openedFileNonce, com.example.MainActivity.openedFilePath) {
        val path = com.example.MainActivity.openedFilePath
        if (path != null) {
            val file = java.io.File(path)
            if (file.exists()) {
                docTitle = file.name
                loadingDocName = file.name
                isLoadingDocument = true

                val result = docxParser.parseDocument(file)
                isLoadingDocument = false

                if (result.parsedDocument?.isParsingFailed == true) {
                    docOpenFailedError = result.parsedDocument.failureReason ?: context.getString(R.string.doc_open_failed_msg, file.name)
                    showDocOpenFailedDialog = true
                } else if (result.text.isNotBlank()) {
                    val lines = result.text.split("\n")
                    var rowIdx = 1
                    for (line in lines) {
                        if (line.isNotBlank()) {
                            val cells = line.split("\t")
                            var colIdx = 0
                            for (cell in cells) {
                                if (colIdx < columnsLabels.size) {
                                    val cellKey = "${columnsLabels[colIdx]}$rowIdx"
                                    cellValues[cellKey] = cell.trim()
                                }
                                colIdx++
                            }
                            rowIdx++
                        }
                    }
                }
            }
        }
    }

    val moduleColor = Color(0xFF16A34A) // Calc Green

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
                Toast.makeText(context, R.string.toast_document_saved, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, R.string.toast_document_saved, Toast.LENGTH_SHORT).show()
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Toast.makeText(context, R.string.toast_uploading_to_google_drive, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = stringResource(R.string.cd_upload_to_drive))
                        }
                        IconButton(onClick = { 
                            isWebView = !isWebView
                            Toast.makeText(context, if (isWebView) R.string.toast_mobile_view_active else R.string.toast_normal_view_active, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                contentDescription = stringResource(R.string.cd_document_view_mode)
                            )
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Export to PDF") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, R.string.toast_exporting_to_pdf, Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = stringResource(R.string.cd_pdf)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Save as...") },
                                    onClick = {
                                        showMoreMenu = false
                                        showSaveAsDialog = true
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.SaveAs, contentDescription = stringResource(R.string.cd_save_as)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simulate Save Error") },
                                    onClick = {
                                        showMoreMenu = false
                                        performSave(true)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = stringResource(R.string.cd_simulate_error), tint = MaterialTheme.colorScheme.error) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, R.string.toast_printing_document, Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = stringResource(R.string.cd_print)) }
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
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_exit_edit_mode), tint = moduleColor)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            Toast.makeText(context, R.string.toast_uploading_to_google_drive, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = stringResource(R.string.cd_upload_to_drive))
                        }
                        IconButton(onClick = {
                            isWebView = !isWebView
                            Toast.makeText(context, if (isWebView) R.string.toast_mobile_view else R.string.toast_normal_view, Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(
                                imageVector = if (isWebView) Icons.Rounded.PhoneAndroid else Icons.Rounded.Web,
                                contentDescription = stringResource(R.string.cd_document_view_mode)
                            )
                        }
                        IconButton(onClick = { handleSaveCommand() }) {
                            Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.cd_save))
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
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
                                    leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = stringResource(R.string.cd_save)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Simulate Save Error") },
                                    onClick = {
                                        showMoreMenu = false
                                        performSave(true)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = stringResource(R.string.cd_simulate_error), tint = MaterialTheme.colorScheme.error) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Export to PDF") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, R.string.toast_exporting_to_pdf, Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = stringResource(R.string.cd_pdf)) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Print") },
                                    onClick = {
                                        showMoreMenu = false
                                        Toast.makeText(context, R.string.toast_connecting_printer, Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Print, contentDescription = stringResource(R.string.cd_print)) }
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

            // --- SPREADSHEET MAIN EDITOR BODY ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
            ) {
                // Formula Bar Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${columnsLabels.getOrNull(activeCellCol - 1) ?: "A"}$activeCellRow",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 14.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = stringResource(R.string.cd_formula_icon),
                        tint = moduleColor
                    )
                    OutlinedTextField(
                        value = formulaText,
                        onValueChange = {
                            formulaText = it
                            isSaved = false
                            onFormulaSelected(it)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("formula_input"),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }

                // Quick Navigation and Sheet tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                if (activeCellCol > 1) activeCellCol--
                                else if (activeCellRow > 1) {
                                    activeCellRow--
                                    activeCellCol = columnsLabels.size
                                }
                            },
                            modifier = Modifier.testTag("btn_prev_cell")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_previous_cell))
                        }
                        IconButton(
                            onClick = {
                                if (activeCellCol < columnsLabels.size) activeCellCol++
                                else {
                                    activeCellRow++
                                    activeCellCol = 1
                                }
                            },
                            modifier = Modifier.testTag("btn_next_cell")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.cd_next_cell))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sheets.forEach { sheet ->
                            val isSelected = sheet == selectedSheet
                            Button(
                                onClick = { selectedSheet = sheet },
                                colors = if (isSelected) ButtonDefaults.buttonColors(containerColor = moduleColor) else ButtonDefaults.filledTonalButtonColors(),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(sheet, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Range Mode:", fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                        Switch(
                            checked = selectionMode == "multi",
                            onCheckedChange = { selectionMode = if (it) "multi" else "single" },
                            modifier = Modifier.scale(0.7f).testTag("range_toggle")
                        )
                    }
                }

                // Grid View
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column {
                        // Header Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(28.dp)
                                    .background(SheetGridFill.copy(alpha = 0.5f))
                                    .border(0.5.dp, SheetGridLine),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            columnsLabels.forEach { label ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .background(SheetGridFill.copy(alpha = 0.5f))
                                        .border(0.5.dp, SheetGridLine),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Grid Rows
                        for (rowIdx in 1..5) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp)
                                        .height(40.dp)
                                        .background(SheetGridFill.copy(alpha = 0.3f))
                                        .border(0.5.dp, SheetGridLine),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$rowIdx", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                for (colIdx in 1..5) {
                                    val colLabel = columnsLabels[colIdx - 1]
                                    val cellId = "$colLabel$rowIdx"
                                    val cellVal = cellValues[cellId] ?: ""
                                    val isActive = rowIdx == activeCellRow && colIdx == activeCellCol

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(40.dp)
                                            .background(if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.White)
                                            .border(
                                                border = BorderStroke(
                                                    width = if (isActive) 2.dp else 0.5.dp,
                                                    color = if (isActive) moduleColor else SheetGridLine
                                                )
                                            )
                                            .clickable {
                                                activeCellRow = rowIdx
                                                activeCellCol = colIdx
                                                showFct = true
                                                if (!isEditMode) isEditMode = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cellVal,
                                            fontSize = 13.sp,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Floating Contextual Toolbar Overlay
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        FloatingContextualToolbar(
                            visible = showFct,
                            contextType = if (selectionMode == "single") "calc_cell" else "calc_multi",
                            onActionClick = { action ->
                                showFct = false
                                onFormulaSelected("Tapped $action on cell ${columnsLabels[activeCellCol-1]}$activeCellRow")
                            }
                        )
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
                        Text(
                            text = "Sheet: $selectedSheet | Cell: ${columnsLabels.getOrNull(activeCellCol - 1)}$activeCellRow",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )

                        IconButton(
                            onClick = { showBottomBar = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = moduleColor.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewAgenda,
                                contentDescription = stringResource(R.string.cd_open_standard_bottom_sheet),
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
                            text = "100% • Sheet 1 of 3",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (zoomScale > 0.5f) zoomScale -= 0.1f },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_zoom_out), modifier = Modifier.size(12.dp))
                            }
                            Text(
                                text = "${(zoomScale * 100).toInt()}%",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            IconButton(
                                onClick = { if (zoomScale < 2.0f) zoomScale += 0.1f },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_zoom_in), modifier = Modifier.size(12.dp))
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
                            val tabs = listOf("File", "Home", "Insert", "Layout", "Formula", "Data", "Review", "View")
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
                            IconButton(onClick = { Toast.makeText(context, R.string.toast_undo_performed, Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = stringResource(R.string.cd_undo), tint = moduleColor)
                            }
                            IconButton(onClick = { Toast.makeText(context, R.string.toast_redo_performed, Toast.LENGTH_SHORT).show() }) {
                                Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = stringResource(R.string.cd_redo), tint = moduleColor)
                            }
                            IconButton(onClick = { showBottomBar = false }) {
                                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close_standard_bottom_sheet), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Interactive Content for Standard Bottom Sheet Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (activeRibbonTab) {
                            "File" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Spreadsheet Document (SDK Ch. 19 & 20)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { handleSaveCommand() },
                                            colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                        ) {
                                            Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save Document")
                                        }
                                        OutlinedButton(onClick = { showSaveAsDialog = true }) {
                                            Icon(Icons.Rounded.SaveAs, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save As...")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_sheet_protected_with_password_xprotectable, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Protect Sheet")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_renamed_active_sheet_xnamed, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rename Sheet")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Calculation Engine Settings (XPropertySet)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = true,
                                            onClick = { Toast.makeText(context, R.string.toast_isiterationenabled_toggled, Toast.LENGTH_SHORT).show() },
                                            label = { Text("IsIterationEnabled") },
                                            leadingIcon = { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        Text("IterationCount: 100", fontSize = 12.sp)
                                    }
                                    Text("NullDate Baseline: 1899-12-30 (Standard)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            "Home" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Cell Formatting (CellProperties / CharacterProperties)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(onClick = { Toast.makeText(context, R.string.toast_bold_toggled, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.FormatBold, contentDescription = stringResource(R.string.cd_bold))
                                        }
                                        IconButton(onClick = { Toast.makeText(context, R.string.toast_italic_toggled, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.FormatItalic, contentDescription = stringResource(R.string.cd_italic))
                                        }
                                        IconButton(onClick = { Toast.makeText(context, R.string.toast_background_color_applied, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.FormatColorFill, contentDescription = stringResource(R.string.cd_fill_color), tint = moduleColor)
                                        }
                                        IconButton(onClick = { Toast.makeText(context, R.string.toast_text_color_changed, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.FormatColorText, contentDescription = stringResource(R.string.cd_text_color))
                                        }
                                        IconButton(onClick = { Toast.makeText(context, R.string.toast_border_added, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.BorderAll, contentDescription = stringResource(R.string.cd_borders))
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Currency & Number Format (EuroAdaption.java)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("EUR", "USD", "DM", "IDR").forEach { curr ->
                                            FilterChip(
                                                selected = currencySymbol == curr,
                                                onClick = {
                                                    if (currencySymbol == "DM" && curr == "EUR") {
                                                        // Apply DM to EUR conversion factor 1.95583f
                                                        cellValues.keys.forEach { k ->
                                                            cellValues[k]?.toDoubleOrNull()?.let { v ->
                                                                cellValues[k] = String.format("%.2f", v / 1.95583)
                                                            }
                                                        }
                                                        Toast.makeText(context, R.string.toast_converted_dm_to_eur_factor_1_95583, Toast.LENGTH_SHORT).show()
                                                    }
                                                    currencySymbol = curr
                                                },
                                                label = { Text(curr) }
                                            )
                                        }
                                    }
                                }
                            }
                            "Insert" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Insert Components (TableRows / TableColumns / Arrays)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = {
                                            val newSheet = "Sheet${sheets.size + 1}"
                                            Toast.makeText(context, context.getString(R.string.toast_inserted_newsheet, newSheet), Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert Sheet")
                                        }
                                        OutlinedButton(onClick = { Toast.makeText(context, context.getString(R.string.toast_inserted_row_above_row_activecellrow, activeCellRow), Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.TableRows, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert Row")
                                        }
                                        OutlinedButton(onClick = { Toast.makeText(context, context.getString(R.string.toast_inserted_column_at_columnslabels_getornull, columnsLabels.getOrNull(activeCellCol - 1)), Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert Column")
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            // Mocking setArray functionality
                                            cellValues["A1"] = "Name"
                                            cellValues["B1"] = "Score"
                                            cellValues["A2"] = "Alice"
                                            cellValues["B2"] = "95"
                                            cellValues["A3"] = "Bob"
                                            cellValues["B3"] = "88"
                                            Toast.makeText(context, R.string.toast_inserted_2d_array_setarray, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.DataArray, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert Data Array")
                                        }
                                        OutlinedButton(onClick = { Toast.makeText(context, R.string.toast_inserted_image_xdrawpagesupplier, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert Image")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Extracting Data (SDK Ch. 21)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_get_cell_type_value_getval_getnum_gettypestring, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Get Cell Val/Type")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_extract_array_row_col_getarray_getrow_getcol, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.TableRows, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Extract Range")
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_query_content_cells_sheetrangesquery, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Query Content")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_find_used_area_sheetcellcursor, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Find Used Area")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Cell Styles & Borders (SDK Ch. 22)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_created_applied_cell_style_xstyle_info, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Style, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Create & Apply Style")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_added_table_borders_tableborder2_borderline2, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.BorderAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Borders")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Garlic Secrets (SDK Ch. 23)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_rows_frozen_window_split_xviewfreezable, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Splitscreen, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Freeze & Split")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_calculated_using_generalfunction_sum, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("General Function")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_shifted_and_inserted_cells_xcellrangemovement, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.ViewArray, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Insert & Shift")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Complex Data (SDK Ch. 24)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_sorted_cells_xsortable, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sort Data")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_generated_data_series_xcellseries, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Generate Series")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_added_annotations_and_borders_xsheetannotations, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Add Annotations")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Monitoring Sheets (SDK Ch. 25)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_added_xmodifylistener, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Listen for Edits")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_added_xselectionchangelistener, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Listen for Selections")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Search & Replace (SDK Ch. 26)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_iterative_search_xsearchable_findfirst_findnext, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Search Iterative")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_find_all_matches_xsearchable_findall, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.FindInPage, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Find All")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_replace_all_matches_xreplaceable_replaceall, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.FindReplace, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Replace All")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Functions & Data Analysis (SDK Ch. 27)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_executed_xfunctionaccess_500_functions_sum_average, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Functions, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Call Functions")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_created_pivot_table_xdatapilottables, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pivot Tables")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_goal_seek_xgoalseek_target_4_input_16, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Goal Seek")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_solver_lpsolve_coinmp_sco_deps_p_143x_60y_max_6315, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Solvers")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Chart2 API Overview (SDK Ch. 28)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_created_tablechart_via_xtablecharts_addnewbyname, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.InsertChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("TableChart")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_chart2_template_column_stacked_3d_percent, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chart Templates")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_chart2_elements_xdiagram_wall_floor_legend, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Chart Elements & Styles")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Column Charts (SDK Ch. 29)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_colchart_title_x_y_axis_titles_rotated_90, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Single Column")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_multcolchart_multi_series_column_chart_with_legend, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Multiple Columns")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_3d_column_threedcolumndeep_flat_cylinder_pyramid, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.Rounded.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("3D Column Shapes")
                                        }
                                        OutlinedButton(onClick = { 
                                            Toast.makeText(context, R.string.toast_collinechart_columnwithline_template, Toast.LENGTH_SHORT).show() 
                                        }) {
                                            Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Column + Line Combo")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Bar, Pie, Area, Line Charts (SDK Ch. 30)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                     Row(
                                         modifier = Modifier.horizontalScroll(rememberScrollState()),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         OutlinedButton(onClick = { 
                                             bridge.insertBarChart(0, "A2:B8", "Sneakers Sold this Month", "Brand", "Number Sold")
                                             Toast.makeText(context, R.string.toast_barchart_swapped_axes_vertical_x_axis_rotated_90, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Bar Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertPieChart(0, "E2:F8", "Top 5 States", "No. of Schools", is3D = true, rotationHorizontal = 0, rotationVertical = -45)
                                             Toast.makeText(context, R.string.toast_pie3dchart_threedpie_template_subtitle_rotated_45, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.PieChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("3D Pie Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertDonutChart(0, "A44:C50", "Annual Expenditure", "Expenditure/Student", "GDP %")
                                             Toast.makeText(context, R.string.toast_donutchart_donut_template_showing_multi_ring_dataset, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Donut Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertAreaChart(0, "E45:G50", "Enrollment Trends", "StackedArea")
                                             Toast.makeText(context, R.string.toast_areachart_area_stackedarea_percentstackedarea, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.AreaChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Area Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertLineChart(0, "E27:G39", "Expenditure Per Pupil", "LineSymbol", showDataLabels = false)
                                             Toast.makeText(context, R.string.toast_lineschart_linesymbol_template_with_dp_none_labels, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Line Chart")
                                         }
                                     }
                                     HorizontalDivider()
                                     Text("Bubble, Net, Stock Charts (SDK Ch. 32)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                     Row(
                                         modifier = Modifier.horizontalScroll(rememberScrollState()),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         OutlinedButton(onClick = { 
                                             bridge.insertBubbleChart(0, "H63:J93", "World Data", "GDP per Capita", "Life Expectancy", categoryLabelsRange = "K64:K93", transparency = 50)
                                             Toast.makeText(context, R.string.toast_labeledbubblechart_50_transparency_category_labels, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.BubbleChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Bubble Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertNetChart(0, "A56:D63", "No of Calls per Day", template = com.makerandreas.papirusoffice.data.framework.Chart2Templates.NET_LINE, reverseAxisClockwise = true)
                                             Toast.makeText(context, R.string.toast_netchart_radar_spider_web_chart_with_clockwise_day, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Net / Radar Chart")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.insertStockChart(0, "A86:F104", "Happy Systems (HASY)", template = com.makerandreas.papirusoffice.data.framework.Chart2Templates.STOCK_VOLUME_OPEN_LOW_HIGH_CLOSE, y2Min = 83.0, y2Max = 103.0)
                                             Toast.makeText(context, R.string.toast_happystockchart_candlesticks_green_red_y2_axis, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.Rounded.CandlestickChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Stock Chart (Candlestick)")
                                         }
                                         OutlinedButton(onClick = { 
                                             bridge.addStockLine("StockChart1", "J141", "J142:J146", lineColorHex = 0xFFFF0000)
                                             Toast.makeText(context, R.string.toast_addstockline_added_pork_bellies_line_graph_series, Toast.LENGTH_SHORT).show() 
                                         }) {
                                             Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                             Spacer(modifier = Modifier.width(4.dp))
                                             Text("Add Stock Line (Pork Bellies)")
                                         }
                                     }
                                     HorizontalDivider()
                                     Text("Embedded Chart Type Switcher (ChartTypeChange.java)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("BarDiagram", "LineDiagram", "PieDiagram", "NetDiagram", "XYDiagram", "StockDiagram", "AreaDiagram", "BubbleDiagram").forEach { cType ->
                                            FilterChip(
                                                selected = selectedChartType == cType,
                                                onClick = {
                                                    selectedChartType = cType
                                                    Toast.makeText(context, context.getString(R.string.toast_changed_chart_to_ctype, cType), Toast.LENGTH_SHORT).show()
                                                },
                                                label = { Text(cType.removeSuffix("Diagram")) }
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = isChart3D,
                                            onClick = { isChart3D = !isChart3D },
                                            label = { Text("3D Chart Mode (Dim3D)") },
                                            leadingIcon = { Icon(Icons.Rounded.ViewInAr, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                }
                            }
                            "Formula" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Functions & Add-In Services (CalcAddins.java / ExampleAddIn.java)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("SUM", "AVERAGE", "COUNT", "MAX", "MIN", "ZTEST").forEach { func ->
                                            FilterChip(
                                                selected = formulaText.contains(func),
                                                onClick = {
                                                    formulaText = "=$func(B2:D4)"
                                                    onFormulaSelected(formulaText)
                                                    isSaved = false
                                                },
                                                label = { Text(func) }
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showAddInDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                        ) {
                                            Icon(Icons.Rounded.Extension, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Execute Calc Add-In Method...")
                                        }
                                        OutlinedButton(onClick = {
                                            formulaText = "={=A10:C12}"
                                            Toast.makeText(context, R.string.toast_inserted_array_formula_xarrayformularange, Toast.LENGTH_SHORT).show()
                                        }) {
                                            Text("Array Formula")
                                        }
                                    }
                                }
                            }
                            "Data" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Data Analysis & Operations (DataPilot / Scenarios / Filter)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { showDataPilotDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                                        ) {
                                            Icon(Icons.Rounded.PivotTableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("DataPilot Pivot Table")
                                        }
                                        OutlinedButton(onClick = { Toast.makeText(context, R.string.toast_applied_autofilter_xsheetfilterable, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.Rounded.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("AutoFilter")
                                        }
                                        OutlinedButton(onClick = { Toast.makeText(context, R.string.toast_sorted_range_ascending_tablesortfield, Toast.LENGTH_SHORT).show() }) {
                                            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sort A-Z")
                                        }
                                    }
                                    HorizontalDivider()
                                    Text("Scenarios Manager (XScenarios)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("Base Plan", "Optimistic", "Conservative").forEach { scen ->
                                            FilterChip(
                                                selected = currentScenarioName == scen,
                                                onClick = {
                                                    currentScenarioName = scen
                                                    Toast.makeText(context, context.getString(R.string.toast_switched_to_scenario_scen, scen), Toast.LENGTH_SHORT).show()
                                                },
                                                label = { Text(scen) }
                                            )
                                        }
                                    }
                                }
                            }
                            "View" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Spreadsheet View Controls (ViewSample.java / XViewFreezable)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilterChip(
                                            selected = isFrozenPane,
                                            onClick = {
                                                isFrozenPane = !isFrozenPane
                                                Toast.makeText(
                                                    context,
                                                    if (isFrozenPane) context.getString(R.string.toast_frozen_pane_at_col_row, activeCellCol, activeCellRow) else context.getString(R.string.toast_unfrozen_panes),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            },
                                            label = { Text("Freeze Panes (freezeAtPosition)") },
                                            leadingIcon = { Icon(Icons.Rounded.AcUnit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                        FilterChip(
                                            selected = showGridLines,
                                            onClick = { showGridLines = !showGridLines },
                                            label = { Text("Grid Lines (ShowGrid)") },
                                            leadingIcon = { Icon(Icons.Rounded.Grid3x3, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Zoom Level: ${(zoomScale * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        IconButton(onClick = { if (zoomScale > 0.5f) zoomScale -= 0.1f }) {
                                            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = stringResource(R.string.cd_zoom_out))
                                        }
                                        IconButton(onClick = { if (zoomScale < 2.0f) zoomScale += 0.1f }) {
                                            Icon(Icons.Rounded.AddCircleOutline, contentDescription = stringResource(R.string.cd_zoom_in))
                                        }
                                        OutlinedButton(onClick = { zoomScale = 1.0f }) {
                                            Text("Reset Zoom")
                                        }
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
                                    Text("Cellina $activeRibbonTab Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = moduleColor)
                                    Text("Full OpenOffice / LibreOffice Calc SDK Chapter 19 features active.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                icon = { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit)) },
                text = { Text("Edit Spreadsheet") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("fab_edit_spreadsheet"),
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

        // --- DOCUMENT OPEN FAILED DIALOG ---
        if (showDocOpenFailedDialog) {
            com.example.ui.components.DocumentOpenFailedDialog(
                docName = loadingDocName,
                errorMessage = docOpenFailedError,
                onDismissRequest = { showDocOpenFailedDialog = false },
                onReturnToRecent = {
                    showDocOpenFailedDialog = false
                    onBack()
                },
                onViewLogs = {
                    showDocOpenFailedDialog = false
                    onBack()
                }
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
                        modifier = Modifier.testTag("btn_unsaved_save_cellina")
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
                        modifier = Modifier.testTag("btn_unsaved_dont_save_cellina")
                    ) {
                        Text(stringResource(R.string.dont_save), color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }

        if (showSaveAsDialog) {
            SaveAsDialog(
                moduleType = "Cellina",
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
                    currentSaveDefaultFilename = if (baseName.isBlank()) "Cellina_Data$extension" else "$baseName$extension"
                    showSaveAsDialog = false
                    saveDocumentLauncher.launch(currentSaveDefaultFilename)
                }
            )
        }

        // --- DATAPILOT PIVOT TABLE DIALOG (SDK CHAPTER 19) ---
        if (showDataPilotDialog) {
            var rowDim by remember { mutableStateOf("Quarter") }
            var colDim by remember { mutableStateOf("Product") }
            var aggFunc by remember { mutableStateOf("SUM") }

            AlertDialog(
                onDismissRequest = { showDataPilotDialog = false },
                icon = { Icon(Icons.Rounded.PivotTableChart, contentDescription = null, tint = moduleColor) },
                title = { Text("DataPilot Pivot Table (SDK Ch. 19)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Configure DataPilotSource dimension mapping and aggregation function.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = rowDim,
                            onValueChange = { rowDim = it },
                            label = { Text("Row Dimension") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = colDim,
                            onValueChange = { colDim = it },
                            label = { Text("Column Dimension") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Aggregation Function:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("SUM", "AVERAGE", "COUNT", "MAX").forEach { fn ->
                                FilterChip(
                                    selected = aggFunc == fn,
                                    onClick = { aggFunc = fn },
                                    label = { Text(fn) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            cellValues["A5"] = "Pivot Summary"
                            cellValues["B5"] = "96700"
                            Toast.makeText(context, context.getString(R.string.toast_datapilot_pivot_table_created_aggfunc_on_rowdim_x, aggFunc, rowDim, colDim), Toast.LENGTH_SHORT).show()
                            showDataPilotDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Generate Pivot Table")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDataPilotDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- CALC ADD-IN EXECUTOR DIALOG (SDK CHAPTER 19) ---
        if (showAddInDialog) {
            var dummyValueText by remember { mutableStateOf("10") }

            AlertDialog(
                onDismissRequest = { showAddInDialog = false },
                icon = { Icon(Icons.Rounded.Extension, contentDescription = null, tint = moduleColor) },
                title = { Text("Execute Calc Add-In Method") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Interface: org.openoffice.sheet.addin.XCalcAddins", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("getMyFirstValue", "getMySecondValue", "getIncremented", "getCounter").forEach { m ->
                                FilterChip(
                                    selected = selectedAddInFunc == m,
                                    onClick = { selectedAddInFunc = m },
                                    label = { Text(m) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = dummyValueText,
                            onValueChange = { dummyValueText = it },
                            label = { Text("Parameter Value (intDummy / Value / Name)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (addInResultText.isNotBlank()) {
                            Surface(
                                color = moduleColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "Result: $addInResultText",
                                    fontWeight = FontWeight.Bold,
                                    color = moduleColor,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val param = dummyValueText.toIntOrNull() ?: 0
                            val res = when (selectedAddInFunc) {
                                "getMyFirstValue" -> 1
                                "getMySecondValue" -> 2 + param
                                "getIncremented" -> param + 1
                                "getCounter" -> "$dummyValueText counter = 1"
                                else -> 0
                            }
                            addInResultText = "$res"
                            val activeCellKey = "${columnsLabels.getOrNull(activeCellCol - 1) ?: "A"}$activeCellRow"
                            cellValues[activeCellKey] = "$res"
                            Toast.makeText(context, context.getString(R.string.toast_selectedaddinfunc_returned_res, selectedAddInFunc, res), Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = moduleColor)
                    ) {
                        Text("Run Add-In Function")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddInDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        if (showUniversalFormsSheet) {
            com.example.ui.components.UniversalFormsSheet(
                activeModuleName = "Cellina",
                onDismiss = { showUniversalFormsSheet = false },
                onInsertFormToDoc = { formSchema ->
                    val activeCellKey = "${columnsLabels.getOrNull(activeCellCol - 1) ?: "A"}$activeCellRow"
                    cellValues[activeCellKey] = "[Form: ${formSchema.title}]"
                    Toast.makeText(context, context.getString(R.string.toast_inserted_form_reference_into_activecellkey, activeCellKey), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
