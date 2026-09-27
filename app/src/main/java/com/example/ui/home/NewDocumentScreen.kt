package com.example.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.core.util.TemplateManager
import com.example.modules.pagella.PagellaPdfCreator
import com.example.ui.theme.*
import com.makerandreas.papirusoffice.data.OpenedDocumentStore
import kotlinx.coroutines.launch
import java.io.File

/**
 * Checks if the device has an active internet connection.
 */
fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager != null) {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }
    return false
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDocumentScreen(
    onBack: () -> Unit,
    onNavigateToModule: (String) -> Unit
) {
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val activeSubTab = if (pagerState.currentPage == 0) "Create New" else "Create from Template"
    var selectedTemplateFilter by remember { mutableStateOf("All") } // "All", "ODT", "ODS", "ODP"

    val handleBack = {
        isSearchActive = false
        searchQuery = ""
        onBack()
    }

    BackHandler(onBack = handleBack)
    val context = LocalContext.current
    
    // Automatically close template search bar when switching away from "Create from Template"
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 1) {
            isSearchActive = false
            searchQuery = ""
        }
    }

    // Check network connectivity on screen entry
    val isOnline = remember { isNetworkAvailable(context) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.create_new_document),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = handleBack,
                            modifier = Modifier.testTag("new_doc_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back_to_home_dashboard)
                            )
                        }
                    },
                    actions = {
                        // Only show search button in "Create from Template" tab
                        if (pagerState.currentPage == 1) {
                            IconButton(
                                onClick = { isSearchActive = !isSearchActive },
                                modifier = Modifier.testTag("template_search_btn")
                            ) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = stringResource(R.string.search_templates)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Inline Search Bar for template search
                AnimatedVisibility(visible = isSearchActive && pagerState.currentPage == 1) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("template_search_field"),
                        placeholder = { Text(stringResource(R.string.search_templates)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.cd_clear_search))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        isSearchActive = false
                    },
                    icon = { Icon(Icons.AutoMirrored.Rounded.NoteAdd, contentDescription = stringResource(R.string.cd_create_new_document_tab)) },
                    label = { Text(stringResource(R.string.tab_create_new)) },
                    modifier = Modifier.testTag("tab_create_new")
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    icon = { Icon(Icons.Rounded.DashboardCustomize, contentDescription = stringResource(R.string.cd_create_from_template_tab)) },
                    label = { Text(stringResource(R.string.tab_from_template)) },
                    modifier = Modifier.testTag("tab_from_template")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> CreateNewDocumentList(onNavigateToModule = onNavigateToModule)
                    1 -> CreateFromTemplateView(
                        isOnline = isOnline,
                        selectedFilter = selectedTemplateFilter,
                        searchQuery = searchQuery,
                        onFilterSelected = { selectedTemplateFilter = it },
                        onNavigateToModule = onNavigateToModule
                    )
                }
            }
        }
    }
}

@Composable
fun CreateNewDocumentList(onNavigateToModule: (String) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val openPdfInPagella = { pdfFile: File, successStringRes: Int ->
        RecentFilesTracker.addFile(context, pdfFile.absolutePath, "Pagella")
        com.example.MainActivity.pendingNewDocument = false
        com.example.MainActivity.openedFilePath = pdfFile.absolutePath
        com.example.MainActivity.openedFileType = "Pagella"
        com.example.MainActivity.openedFileNonce++
        Toast.makeText(context, successStringRes, Toast.LENGTH_SHORT).show()
        onNavigateToModule("Pagella")
    }

    // 1. Create from Image launcher (*.jpg, *.jpeg, *.png, *.webp)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val pdfFile = PagellaPdfCreator.createPdfFromImageUri(context, uri)
                if (pdfFile != null) {
                    openPdfInPagella(pdfFile, R.string.toast_pdf_created_from_image)
                } else {
                    Toast.makeText(context, R.string.toast_pdf_creation_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 2. Create from Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured: Boolean ->
        val file = pendingCameraFile
        if (captured && file != null && file.exists()) {
            coroutineScope.launch {
                val baseName = "Camera_Scan_${System.currentTimeMillis()}"
                val pdfFile = PagellaPdfCreator.createPdfFromImageFile(context, file, baseName)
                try { file.delete() } catch (_: Exception) {}
                if (pdfFile != null) {
                    openPdfInPagella(pdfFile, R.string.toast_pdf_created_from_camera)
                } else {
                    Toast.makeText(context, R.string.toast_pdf_creation_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val launchCameraCapture = {
        try {
            val captureDir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
            val photoFile = File(captureDir, "pagella_capture_${System.currentTimeMillis()}.jpg")
            pendingCameraFile = photoFile
            val authority = "${context.packageName}.fileprovider"
            val photoUri = FileProvider.getUriForFile(context, authority, photoFile)
            takePictureLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e("CreateNewDocumentList", "Failed to launch camera capture", e)
            Toast.makeText(context, R.string.toast_pdf_creation_failed, Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            launchCameraCapture()
        } else {
            Toast.makeText(context, R.string.toast_camera_permission_required, Toast.LENGTH_SHORT).show()
        }
    }

    // 3. Convert from Document launcher (ODF or OOXML -> PDF)
    val convertDocPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val displayName = PagellaPdfCreator.resolveDisplayName(context, uri, "Document.odt")
                    if (!PagellaPdfCreator.isSupportedOfficeDocumentName(displayName)) {
                        Toast.makeText(context, R.string.toast_unsupported_document_for_pdf, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    val persisted = OpenedDocumentStore.persistFromUri(context, uri, displayName)
                    val pdfFile = PagellaPdfCreator.convertDocumentToPdf(context, persisted, displayName)
                    if (pdfFile != null) {
                        openPdfInPagella(pdfFile, R.string.toast_pdf_converted_from_document)
                    } else {
                        Toast.makeText(context, R.string.toast_pdf_creation_failed, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("CreateNewDocumentList", "Failed to convert document to PDF", e)
                    Toast.makeText(context, R.string.toast_pdf_creation_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section 1: Create New Document (3-column single-row carousel grid, no supporting text)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.create_new_document),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("group_title_create_new_document")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .testTag("create_new_carousel_grid"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CreateNewModuleGridCard(
                        title = stringResource(R.string.create_new_inky_card),
                        iconRes = R.drawable.ic_inky_logo,
                        contentDescription = stringResource(R.string.cd_inky_document),
                        testTag = "item_new_inky",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            val templateFile = TemplateManager.getInkyNormalTemplateFile(context)
                            com.example.MainActivity.pendingNewDocument = true
                            com.example.MainActivity.openedFilePath = templateFile?.absolutePath
                            com.example.MainActivity.openedFileType = "Inky"
                            com.example.MainActivity.openedFileNonce++
                            onNavigateToModule("Inky")
                        }
                    )

                    CreateNewModuleGridCard(
                        title = stringResource(R.string.create_new_cellina_card),
                        iconRes = R.drawable.ic_cellina_logo,
                        contentDescription = stringResource(R.string.cd_cellina_spreadsheet),
                        testTag = "item_new_cellina",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            val templateFile = TemplateManager.getCalcDefaultTemplateFile(context)
                            com.example.MainActivity.pendingNewDocument = true
                            com.example.MainActivity.openedFilePath = templateFile?.absolutePath
                            com.example.MainActivity.openedFileType = "Cellina"
                            com.example.MainActivity.openedFileNonce++
                            onNavigateToModule("Cellina")
                        }
                    )

                    CreateNewModuleGridCard(
                        title = stringResource(R.string.create_new_slidia_card),
                        iconRes = R.drawable.ic_slidia_logo,
                        contentDescription = stringResource(R.string.cd_slidia_presentation),
                        testTag = "item_new_slidia",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            val templateFile = TemplateManager.getSlidiaDefaultTemplateFile(context)
                            com.example.MainActivity.pendingNewDocument = true
                            com.example.MainActivity.openedFilePath = templateFile?.absolutePath
                            com.example.MainActivity.openedFileType = "Slidia"
                            com.example.MainActivity.openedFileNonce++
                            onNavigateToModule("Slidia")
                        }
                    )
                }
            }
        }

        // Section 2: Create Pagella PDF Document (Grouped list)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.group_create_pagella_pdf),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .testTag("group_title_pagella_pdf")
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_card_pagella_pdf")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PagellaCreateListItem(
                            title = stringResource(R.string.pagella_create_from_image_title),
                            description = stringResource(R.string.pagella_create_from_image_desc),
                            icon = Icons.Rounded.Image,
                            iconContentDescription = stringResource(R.string.cd_create_pagella_from_image),
                            testTag = "item_pagella_from_image",
                            onClick = {
                                imagePickerLauncher.launch(
                                    arrayOf("image/jpeg", "image/png", "image/webp")
                                )
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        PagellaCreateListItem(
                            title = stringResource(R.string.pagella_create_from_camera_title),
                            description = stringResource(R.string.pagella_create_from_camera_desc),
                            icon = Icons.Rounded.PhotoCamera,
                            iconContentDescription = stringResource(R.string.cd_create_pagella_from_camera),
                            testTag = "item_pagella_from_camera",
                            onClick = {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    launchCameraCapture()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        PagellaCreateListItem(
                            title = stringResource(R.string.pagella_convert_from_document_title),
                            description = stringResource(R.string.pagella_convert_from_document_desc),
                            icon = Icons.Rounded.PictureAsPdf,
                            iconContentDescription = stringResource(R.string.cd_convert_document_to_pdf),
                            testTag = "item_pagella_convert_document",
                            onClick = {
                                convertDocPickerLauncher.launch(
                                    arrayOf(
                                        "application/vnd.oasis.opendocument.text",
                                        "application/vnd.oasis.opendocument.text-template",
                                        "application/vnd.oasis.opendocument.spreadsheet",
                                        "application/vnd.oasis.opendocument.spreadsheet-template",
                                        "application/vnd.oasis.opendocument.presentation",
                                        "application/vnd.oasis.opendocument.presentation-template",
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                        "application/msword",
                                        "application/vnd.ms-excel",
                                        "application/vnd.ms-powerpoint"
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateNewModuleGridCard(
    title: String,
    iconRes: Int,
    contentDescription: String,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .heightIn(min = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = contentDescription,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PagellaCreateListItem(
    title: String,
    description: String,
    icon: ImageVector,
    iconContentDescription: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFromTemplateView(
    isOnline: Boolean,
    selectedFilter: String,
    searchQuery: String,
    onFilterSelected: (String) -> Unit,
    onNavigateToModule: (String) -> Unit
) {
    val filters = listOf("All", "ODT", "ODS", "ODP")
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var templates by remember { mutableStateOf<List<TemplateManager.TemplateItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var downloadProgressMap by remember { mutableStateOf<Map<String, Float>>(emptyMap()) }
    var downloadedFilesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Fetch templates whenever filter changes, with automatic offline fallback
    LaunchedEffect(selectedFilter) {
        isLoading = true
        try {
            templates = TemplateManager.searchTemplates(context, selectedFilter)
        } catch (e: Exception) {
            Log.e("CreateFromTemplateView", "Error fetching templates", e)
        } finally {
            isLoading = false
        }
    }

    // Filter by search query
    val filteredTemplates = remember(templates, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            templates
        } else {
            templates.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        // Horizontally scrolling filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Querying ODF repositories...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!isOnline && templates.isEmpty()) {
            // Offline Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(28.dp)
                                )
                        )
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .offset(x = 18.dp, y = (-18).dp)
                        )
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.no_internet),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.no_internet_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else if (filteredTemplates.isEmpty()) {
            // No Results Empty State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(28.dp)
                                )
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Article,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = stringResource(R.string.no_templates),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.no_templates_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }
        } else {
            // Display Template List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTemplates) { template ->
                    val progress = downloadProgressMap[template.name]
                    val isDownloading = progress != null && progress < 1.0f
                    val isDownloaded = downloadedFilesMap.containsKey(template.name)

                    // Theme Colors according to document/template type
                    val (themeColor, icon) = when (template.type) {
                        "ODT" -> Color(0xFF2563EB) to Icons.Rounded.Description
                        "ODS" -> Color(0xFF10B981) to Icons.Rounded.GridView
                        else -> Color(0xFFD97706) to Icons.Rounded.Slideshow
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDownloading) {
                                coroutineScope.launch {
                                    if (isDownloaded) {
                                        // Already downloaded, just open it
                                        val filePath = downloadedFilesMap[template.name]
                                        if (filePath == null) {
                                            Toast.makeText(context, R.string.toast_template_file_is_missing_please_re_download, Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        com.example.MainActivity.openedFilePath = filePath
                                        com.example.MainActivity.openedFileType = when (template.type) {
                                            "ODS" -> "Cellina"
                                            "ODP" -> "Slidia"
                                            else -> "Inky"
                                        }
                                        onNavigateToModule(com.example.MainActivity.openedFileType ?: "Inky")
                                    } else {
                                        // Start downloading
                                        Toast.makeText(context, context.getString(R.string.toast_downloading_template_template_name, template.name), Toast.LENGTH_SHORT).show()
                                        downloadProgressMap = downloadProgressMap + (template.name to 0f)
                                        val file = TemplateManager.downloadTemplate(context, template) { prog ->
                                            downloadProgressMap = downloadProgressMap + (template.name to prog)
                                        }
                                        if (file != null) {
                                            downloadProgressMap = downloadProgressMap + (template.name to 1.0f)
                                            downloadedFilesMap = downloadedFilesMap + (template.name to file.absolutePath)
                                            Toast.makeText(context, R.string.toast_download_complete_opening, Toast.LENGTH_SHORT).show()
                                            
                                            com.example.MainActivity.openedFilePath = file.absolutePath
                                            com.example.MainActivity.openedFileType = when (template.type) {
                                                "ODS" -> "Cellina"
                                                "ODP" -> "Slidia"
                                                else -> "Inky"
                                            }
                                            onNavigateToModule(com.example.MainActivity.openedFileType ?: "Inky")
                                        } else {
                                            downloadProgressMap = downloadProgressMap - template.name
                                            Toast.makeText(context, R.string.toast_download_failed_please_check_your_connection, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(themeColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = template.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Papirus Template • ${template.type}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isDownloading) {
                                    CircularProgressIndicator(
                                        progress = { progress ?: 0f },
                                        modifier = Modifier.size(24.dp),
                                        color = themeColor,
                                        strokeWidth = 2.dp
                                    )
                                } else if (isDownloaded) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = stringResource(R.string.cd_downloaded_successfully),
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = stringResource(R.string.cd_download_template),
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            if (template.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = template.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
