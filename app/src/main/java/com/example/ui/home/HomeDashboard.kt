package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.theme.*
import com.makerandreas.papirusoffice.data.PapirusConfigManager
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.GeminiAiService
import com.example.ui.theme.ThemeSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// LOCAL RECENT FILES PERSISTENCE TRACKER
// ==========================================
object RecentFilesTracker {
    private const val PREFS_NAME = "papirus_recents_prefs"
    private const val KEY_RECENTS = "recent_files_list"

    data class RecentFile(
        val path: String,
        val name: String,
        val lastOpened: Long,
        val fileType: String,
        val size: String
    )

    fun normalizeModule(type: String, fileName: String): String {
        val lowerName = fileName.lowercase()
        return when {
            type == "Inky" || type == "Cellina" || type == "Slidia" || type == "Pagella" -> type
            lowerName.endsWith(".ods") || lowerName.endsWith(".ots") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv") -> "Cellina"
            lowerName.endsWith(".odp") || lowerName.endsWith(".otp") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") -> "Slidia"
            lowerName.endsWith(".pdf") -> "Pagella"
            else -> "Inky"
        }
    }

    fun getRecents(context: Context): List<RecentFile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENTS, "[]") ?: "[]"
        val list = mutableListOf<RecentFile>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val path = obj.getString("path")
                val name = obj.getString("name")
                val lastOpened = obj.getLong("lastOpened")
                val fileType = normalizeModule(obj.getString("fileType"), name)
                val size = obj.getString("size")
                list.add(RecentFile(path, name, lastOpened, fileType, size))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Do NOT filter out missing files here so moved/renamed documents can still be listed and removed manually
        return list.sortedByDescending { it.lastOpened }
    }

    fun addFile(context: Context, path: String, fileType: String) {
        val file = File(path)
        if (!file.exists()) return

        val sizeBytes = file.length()
        val sizeStr = when {
            sizeBytes < 1024 -> "$sizeBytes B"
            sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
            else -> String.format(Locale.getDefault(), "%.1f MB", sizeBytes.toDouble() / (1024 * 1024))
        }

        val recents = getRecents(context).toMutableList()
        recents.removeAll { it.path == path }

        recents.add(0, RecentFile(
            path = path,
            name = file.name,
            lastOpened = System.currentTimeMillis(),
            fileType = normalizeModule(fileType, file.name),
            size = sizeStr
        ))

        saveRecents(context, recents)
    }

    fun removeFile(context: Context, path: String) {
        val recents = getRecents(context).toMutableList()
        recents.removeAll { it.path == path }
        saveRecents(context, recents)
    }

    private fun saveRecents(context: Context, list: List<RecentFile>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("path", item.path)
                put("name", item.name)
                put("lastOpened", item.lastOpened)
                put("fileType", item.fileType)
                put("size", item.size)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RECENTS, jsonArray.toString()).apply()
    }
}

// ==========================================
// STORAGE UTILITIES AND SHORTCUT DETECTOR
// ==========================================
fun getMockStorageRoot(context: Context): File {
    val realRoot = File("/storage/emulated/0")
    try {
        if (realRoot.exists() && realRoot.canRead() && realRoot.canWrite()) {
            return realRoot
        }
    } catch (e: Exception) {}
    
    // Fallback sandbox storage structured like /storage/emulated/0
    val fallback = File(context.filesDir, "storage_emulated_0")
    if (!fallback.exists()) {
        fallback.mkdirs()
    }
    return fallback
}

fun getDirectoryShortcut(context: Context, path: String): File {
    val root = getMockStorageRoot(context)
    val relative = path.removePrefix("/storage/emulated/0").removePrefix("/")
    val targetFile = if (relative.isEmpty()) root else File(root, relative)
    if (!targetFile.exists()) {
        targetFile.mkdirs()
    }
    return targetFile
}

fun getExternalStorageShortcut(context: Context): File? {
    val dirs = androidx.core.content.ContextCompat.getExternalFilesDirs(context, null)
    if (dirs.size > 1) {
        val extFile = dirs[1]
        if (extFile != null) {
            val path = extFile.absolutePath
            val idx = path.indexOf("/Android/data")
            if (idx != -1) {
                return File(path.substring(0, idx))
            }
        }
    }
    return null
}

fun getFileTypeForFile(file: File): String? {
    return when (file.extension.lowercase()) {
        "odt", "docx", "doc", "txt" -> "Inky"
        "ods", "xlsx", "xls", "csv" -> "Cellina"
        "odp", "pptx", "ppt" -> "Slidia"
        "pdf" -> "Pagella"
        else -> null
    }
}

// ==========================================
// PAPIRUS OFFICE START SCREEN COMPOSABLE
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeDashboard(
    isTablet: Boolean,
    onNavigateToModule: (String) -> Unit,
    dynamicColorEnabled: Boolean = false,
    onDynamicColorChange: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val tabs = listOf("Recents", "Files", "Google Drive")
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { tabs.size })
    val activeTab = tabs[pagerState.currentPage]
    
    // Search queries
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Drawer state for hamburger menu bar
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Auto-close search bar when switching subpages (Recents, Files, Google Drive)
    LaunchedEffect(pagerState.currentPage) {
        isSearchActive = false
        searchQuery = ""
    }

    // Dialog & Options states
    var showOptionsDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var showUniversalPrintSheet by remember { mutableStateOf(false) }
    var showUniversalEmailSheet by remember { mutableStateOf(false) }
    var showUniversalClipboardSheet by remember { mutableStateOf(false) }
    var showUniversalXmlImportSheet by remember { mutableStateOf(false) }
    var showUniversalOdfSheet by remember { mutableStateOf(false) }
    var resetSuccessMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            PapirusConfigManager.initialize(context)
        }
        PapirusConfigManager.checkAndShowResetSuccessPopup(context) { msg ->
            resetSuccessMessage = msg
        }
    }

    // Close drawer on system back button press if open
    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isSearchActive,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                drawerTonalElevation = 0.dp,
                modifier = Modifier.width(360.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(12.dp)
                ) {
                    // Main Section Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Papirus Office",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Normal,
                                lineHeight = 28.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.1.sp
                                )
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            showOptionsDialog = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("drawer_item_settings")
                    )

                    // Crash Logs
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Comment,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "Crash Logs",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.1.sp
                                )
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToModule("crash_logs")
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("drawer_item_crash_logs")
                    )

                    // About Papirus Office
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "About Papirus Office",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 20.sp,
                                    letterSpacing = 0.1.sp
                                )
                            )
                        },
                        selected = false,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            onNavigateToModule("about")
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("drawer_item_about")
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Papirus Office",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                    }
                                },
                                modifier = Modifier.testTag("btn_top_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Menu,
                                    contentDescription = "Main Menu"
                                )
                            }
                        },
                        actions = {
                            if (activeTab != "Files") {
                                IconButton(
                                    onClick = { isSearchActive = !isSearchActive },
                                    modifier = Modifier.testTag("btn_top_search")
                                ) {
                                    Icon(
                                        imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                        contentDescription = "Search"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    )

                    AnimatedVisibility(visible = isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("search_field"),
                            placeholder = { Text(stringResource(R.string.search_placeholder)) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
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
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = { Icon(Icons.Rounded.AccessTime, contentDescription = "Recents tab") },
                        label = { Text("Recents") }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { Icon(Icons.Rounded.Folder, contentDescription = "Files tab") },
                        label = { Text("Files") }
                    )
                    NavigationBarItem(
                        selected = pagerState.currentPage == 2,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_google_drive),
                                contentDescription = "Google Drive tab",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text("Google Drive") }
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { onNavigateToModule("create_new_document") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    ),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("main_fab")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fab_new_document),
                        contentDescription = stringResource(R.string.create_new_document),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
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
                        0 -> RecentsSubPage(
                            searchQuery = searchQuery,
                            onNavigateToModule = onNavigateToModule
                        )
                        1 -> FilesSubPage(
                            onNavigateToModule = onNavigateToModule
                        )
                        2 -> GoogleDriveSubPage()
                    }
                }
            }
        }
    }

    // ==========================================
    // PAPIRUS OFFICE OPTIONS SCREEN WITH SLIDE ANIMATION & PREDICTIVE BACK
    // ==========================================
    AnimatedVisibility(
        visible = showOptionsDialog,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        com.example.ui.options.PapirusOfficeOptionsScreen(
            sourceModule = "general",
            onCloseOptions = { showOptionsDialog = false },
            onDynamicColorChange = onDynamicColorChange
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
            docTitle = "Financial_Statement_Q1.ods",
            docContent = "Excel sheets or ODS sheet simulation.",
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

    if (resetSuccessMessage != null) {
        AlertDialog(
            onDismissRequest = { resetSuccessMessage = null },
            icon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.reset_dialog_title), fontWeight = FontWeight.Bold) },
            text = { Text(resetSuccessMessage!!) },
            confirmButton = {
                Button(onClick = { resetSuccessMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

    // ==========================================
    // RECENTS SUB-PAGE
    // ==========================================
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun RecentsSubPage(
        searchQuery: String,
        onNavigateToModule: (String) -> Unit
    ) {
        val context = LocalContext.current
        var selectedFilter by remember { mutableStateOf("All") }

        var recentFiles by remember(searchQuery) {
            mutableStateOf(RecentFilesTracker.getRecents(context))
        }

        androidx.compose.runtime.LaunchedEffect(Unit) {
            recentFiles = RecentFilesTracker.getRecents(context)
        }

        val filteredFiles = remember(recentFiles, searchQuery, selectedFilter) {
            val searched = if (searchQuery.trim().isEmpty()) {
                recentFiles
            } else {
                recentFiles.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
            }

            if (selectedFilter == "All") {
                searched
            } else {
                searched.filter {
                    val fileTypeToMatch = when (selectedFilter) {
                        "Inky Documents" -> "Inky"
                        "Cellina Spreadsheets" -> "Cellina"
                        "Slidia Presentations" -> "Slidia"
                        "Pagella PDF" -> "Pagella"
                        else -> ""
                    }
                    it.fileType == fileTypeToMatch
                }
            }
        }

        var selectedDocForProps by remember { mutableStateOf<RecentFilesTracker.RecentFile?>(null) }
        var showFileNotFoundDialog by remember { mutableStateOf(false) }

        if (showFileNotFoundDialog) {
            AlertDialog(
                onDismissRequest = { showFileNotFoundDialog = false },
                icon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text(stringResource(R.string.error_file_not_found_title), fontWeight = FontWeight.Bold) },
                text = { Text(stringResource(R.string.error_file_not_found_msg)) },
                confirmButton = {
                    TextButton(onClick = { showFileNotFoundDialog = false }) {
                        Text("OK")
                    }
                }
            )
        }

        selectedDocForProps?.let { doc ->
            AlertDialog(
                onDismissRequest = { selectedDocForProps = null },
                icon = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text(stringResource(R.string.doc_props_title), fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${stringResource(R.string.doc_props_name)}: ${doc.name}", fontWeight = FontWeight.SemiBold)
                        Text("${stringResource(R.string.doc_props_path)}: ${doc.path}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${stringResource(R.string.doc_props_size)}: ${doc.size}")
                        Text("${stringResource(R.string.doc_props_type)}: ${doc.fileType}")
                        Text("${stringResource(R.string.doc_props_modified)}: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(doc.lastOpened))}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedDocForProps = null }) {
                        Text("Close")
                    }
                }
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Chips under the app bar
            val filterOptions = listOf(
                "All" to R.string.filter_all,
                "Inky Documents" to R.string.filter_inky,
                "Cellina Spreadsheets" to R.string.filter_cellina,
                "Slidia Presentations" to R.string.filter_slidia,
                "Pagella PDF" to R.string.filter_pagella
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filterOptions) { (filterKey, stringResId) ->
                    val isSelected = selectedFilter == filterKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filterKey },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else null,
                        label = {
                            Text(
                                text = stringResource(stringResId),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        elevation = if (isSelected) FilterChipDefaults.filterChipElevation(elevation = 1.dp) else null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = if (isSelected) {
                            null
                        } else {
                            FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        },
                        modifier = Modifier.testTag("filter_chip_${filterKey.lowercase().replace(" ", "_")}")
                    )
                }
            }

            if (filteredFiles.isEmpty()) {
                val emptyTitle = when (selectedFilter) {
                    "Cellina Spreadsheets" -> stringResource(R.string.no_recent_spreadsheets_title)
                    "Slidia Presentations" -> stringResource(R.string.no_recent_presentations_title)
                    "Pagella PDF" -> stringResource(R.string.no_recent_pdfs_title)
                    else -> stringResource(R.string.no_recent_documents_title)
                }

                val emptyDesc = when (selectedFilter) {
                    "Cellina Spreadsheets" -> stringResource(R.string.no_recent_spreadsheets_desc)
                    "Slidia Presentations" -> stringResource(R.string.no_recent_presentations_desc)
                    "Pagella PDF" -> stringResource(R.string.no_recent_pdfs_desc)
                    else -> stringResource(R.string.no_recent_documents_desc)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        RecentsEmptyStateIllustration(
                            filter = selectedFilter,
                            modifier = Modifier.size(140.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = emptyDesc,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles) { file ->
                        var showItemMenu by remember { mutableStateOf(false) }

                        val displayNameWithSuffix = remember(file.name, file.fileType) {
                            if (file.name.contains(".")) {
                                file.name
                            } else {
                                when (file.fileType) {
                                    "Inky" -> "${file.name}.docx"
                                    "Cellina" -> "${file.name}.ods"
                                    "Slidia" -> "${file.name}.odp"
                                    "Pagella" -> "${file.name}.pdf"
                                    else -> file.name
                                }
                            }
                        }

                        val formattedDateTime = remember(file.lastOpened) {
                            try {
                                val date = Date(file.lastOpened)
                                val dFormat = android.text.format.DateFormat.getMediumDateFormat(context)
                                val tFormat = android.text.format.DateFormat.getTimeFormat(context)
                                "${dFormat.format(date)}, ${tFormat.format(date)}"
                            } catch (e: Exception) {
                                SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastOpened))
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!File(file.path).exists()) {
                                        showFileNotFoundDialog = true
                                    } else {
                                        com.example.MainActivity.openedFilePath = file.path
                                        com.example.MainActivity.openedFileType = file.fileType
                                        onNavigateToModule(file.fileType)
                                        val toastMsg = context.getString(R.string.opening_file, displayNameWithSuffix)
                                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("recent_file_item_${file.name.lowercase().replace(" ", "_").replace(".", "_")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val lowerName = file.name.lowercase()
                                val iconRes = when {
                                    file.fileType == "Inky" || lowerName.endsWith(".odt") || lowerName.endsWith(".docx") || lowerName.endsWith(".doc") || lowerName.endsWith(".txt") || lowerName.endsWith(".rtf") -> R.drawable.ic_inky_logo
                                    file.fileType == "Cellina" || lowerName.endsWith(".ods") || lowerName.endsWith(".ots") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") || lowerName.endsWith(".csv") -> R.drawable.ic_cellina_logo
                                    file.fileType == "Slidia" || lowerName.endsWith(".odp") || lowerName.endsWith(".otp") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") -> R.drawable.ic_slidia_logo
                                    file.fileType == "Pagella" || lowerName.endsWith(".pdf") -> R.drawable.ic_pagella_logo
                                    else -> R.drawable.ic_papirus_logo
                                }

                                Image(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = "${file.fileType} Document",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier.size(width = 32.dp, height = 40.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = displayNameWithSuffix,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontSize = 16.sp,
                                            lineHeight = 24.sp,
                                            letterSpacing = 0.5.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formattedDateTime,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            lineHeight = 20.sp,
                                            letterSpacing = 0.25.sp,
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    IconButton(
                                        onClick = { showItemMenu = true },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = "More Options for ${file.name}",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showItemMenu,
                                        onDismissRequest = { showItemMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
                                            text = { Text("Open") },
                                            onClick = {
                                                showItemMenu = false
                                                if (!File(file.path).exists()) {
                                                    showFileNotFoundDialog = true
                                                } else {
                                                    com.example.MainActivity.openedFilePath = file.path
                                                    com.example.MainActivity.openedFileType = file.fileType
                                                    onNavigateToModule(file.fileType)
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                                            text = { Text(stringResource(R.string.menu_share)) },
                                            onClick = {
                                                showItemMenu = false
                                                if (!File(file.path).exists()) {
                                                    showFileNotFoundDialog = true
                                                } else {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_SUBJECT, displayNameWithSuffix)
                                                        putExtra(Intent.EXTRA_TEXT, "Document: $displayNameWithSuffix\nPath: ${file.path}")
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                                                }
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Rounded.PictureAsPdf, contentDescription = null) },
                                            text = { Text(stringResource(R.string.menu_export_pdf)) },
                                            onClick = {
                                                showItemMenu = false
                                                if (!File(file.path).exists()) {
                                                    showFileNotFoundDialog = true
                                                } else {
                                                    Toast.makeText(context, "Exported $displayNameWithSuffix to PDF", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                        if (file.fileType == "Inky") {
                                            DropdownMenuItem(
                                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null) },
                                                text = { Text(stringResource(R.string.menu_export_epub)) },
                                                onClick = {
                                                    showItemMenu = false
                                                    if (!File(file.path).exists()) {
                                                        showFileNotFoundDialog = true
                                                    } else {
                                                        Toast.makeText(context, "Exported $displayNameWithSuffix to ePub", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                                            text = { Text(stringResource(R.string.menu_doc_properties)) },
                                            onClick = {
                                                showItemMenu = false
                                                if (!File(file.path).exists()) {
                                                    showFileNotFoundDialog = true
                                                } else {
                                                    selectedDocForProps = file
                                                }
                                            }
                                        )
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                            text = { Text(stringResource(R.string.menu_remove_from_list), color = MaterialTheme.colorScheme.error) },
                                            onClick = {
                                                showItemMenu = false
                                                RecentFilesTracker.removeFile(context, file.path)
                                                recentFiles = RecentFilesTracker.getRecents(context)
                                            }
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

@Composable
fun RecentsEmptyStateIllustration(
    filter: String = "All",
    modifier: Modifier = Modifier
) {
    val iconRes = when (filter) {
        "Inky Documents" -> R.drawable.ic_recents_empty_inky
        "Cellina Spreadsheets" -> R.drawable.ic_recents_empty_cellina
        else -> R.drawable.ic_recents_empty_illustration
    }
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = modifier
    )
}

// ==========================================
// FILES SUB-PAGE (DYNAMIC FILES EXPLORER)
// ==========================================
@Composable
fun ShortcutCard(
    title: String,
    path: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(path, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline, fontFamily = FontFamily.Monospace)
            }

            Icon(Icons.Rounded.ChevronRight, contentDescription = "Browse folder", tint = MaterialTheme.colorScheme.outline)
        }
    }
}

// ==========================================
// GOOGLE DRIVE SUB-PAGE
// ==========================================
@Composable
fun GoogleDriveSubPage() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF1F5F9),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Google Drive Sync",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Connect your Google Workspace accounts to access and collaborate on your cloud-stored documents, spreadsheets and presentation decks directly within Papirus Office.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    Toast.makeText(context, "Cloud sync is a placeholder and will be configured in the next development cycle.", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.CloudQueue, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hubungkan Akun Google")
            }
        }
    }
}
