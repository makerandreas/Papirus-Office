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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
                                                    val shareFile = File(file.path)
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        putExtra(Intent.EXTRA_SUBJECT, displayNameWithSuffix)
                                                        putExtra(Intent.EXTRA_TEXT, "Document: $displayNameWithSuffix\nPath: ${file.path}")
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                        try {
                                                            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                                                                context,
                                                                "${context.packageName}.fileprovider",
                                                                shareFile
                                                            )
                                                            putExtra(Intent.EXTRA_STREAM, contentUri)
                                                            type = "application/octet-stream"
                                                        } catch (e: Exception) {
                                                            type = "text/plain"
                                                        }
                                                    }
                                                    val chooser = Intent.createChooser(shareIntent, "Share Document").apply {
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(chooser)
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

private const val EMPTY_BG_ROSETTE = "M119.415,14.68C119.614,14.485 119.714,14.387 119.8,14.306C124.406,9.968 131.594,9.968 136.2,14.306C136.286,14.387 136.386,14.485 136.585,14.68C136.705,14.797 136.765,14.856 136.822,14.91C139.769,17.723 143.946,18.842 147.905,17.88C147.981,17.861 148.063,17.84 148.225,17.799C148.495,17.729 148.63,17.695 148.746,17.667C154.904,16.214 161.128,19.808 162.949,25.867C162.983,25.981 163.02,26.115 163.095,26.384C163.14,26.546 163.163,26.626 163.185,26.702C164.331,30.612 167.389,33.669 171.298,34.815C171.374,34.837 171.454,34.86 171.616,34.905C171.885,34.98 172.019,35.017 172.133,35.051C178.192,36.872 181.786,43.096 180.333,49.254C180.305,49.37 180.271,49.505 180.201,49.775C180.16,49.937 180.139,50.019 180.12,50.095C179.158,54.054 180.277,58.231 183.09,61.178C183.144,61.235 183.203,61.295 183.32,61.415C183.515,61.614 183.613,61.714 183.694,61.8C188.032,66.406 188.032,73.594 183.694,78.2 C183.613,78.286 183.515,78.386 183.32,78.585C183.203,78.705 183.144,78.765 183.09,78.822C180.277,81.769 179.158,85.946 180.12,89.905C180.139,89.982 180.16,90.063 180.201,90.225C180.271,90.495 180.305,90.63 180.333,90.746C181.786,96.904 178.192,103.128 172.133,104.949C172.019,104.983 171.885,105.02 171.616,105.095C171.454,105.14 171.374,105.163 171.298,105.185C167.388,106.331 164.331,109.389 163.185,113.298C163.163,113.374 163.14,113.454 163.095,113.616C163.02,113.885 162.983,114.019 162.949,114.133C161.128,120.192 154.904,123.786 148.746,122.333C148.63,122.305 148.495,122.271 148.225,122.201C148.063,122.16 147.981,122.139 147.905,122.12C143.946,121.158 139.769,122.277 136.822,125.09C136.765,125.144 136.705,125.203 136.585,125.32C136.386,125.515 136.286,125.613 136.2,125.694C131.594,130.032 124.406,130.032 119.8,125.694C119.714,125.613 119.614,125.515 119.415,125.32C119.295,125.203 119.235,125.144 119.178,125.09C116.231,122.277 112.054,121.158 108.095,122.12C108.019,122.139 107.937,122.16 107.775,122.201C107.505,122.271 107.37,122.305 107.254,122.333C101.096,123.786 94.872,120.192 93.051,114.133C93.017,114.019 92.98,113.885 92.905,113.616C92.86,113.454 92.837,113.374 92.815,113.298C91.669,109.389 88.611,106.331 84.702,105.185C84.626,105.163 84.546,105.14 84.384,105.095C84.115,105.02 83.981,104.983 83.867,104.949C77.808,103.128 74.214,96.904 75.667,90.746C75.695,90.63 75.729,90.495 75.799,90.225 C75.84,90.063 75.861,89.982 75.88,89.905 C76.842,85.946 75.723,81.769 72.91,78.822 C72.856,78.765 72.797,78.705 72.68,78.585 C72.485,78.386 72.387,78.286 72.306,78.2 C67.968,73.594 67.968,66.406 72.306,61.8 C72.387,61.714 72.485,61.614 72.68,61.415 C72.797,61.295 72.856,61.235 72.91,61.178 C75.723,58.231 76.842,54.054 75.88,50.095 C75.861,50.019 75.84,49.937 75.799,49.775 C75.729,49.505 75.695,49.37 75.667,49.254 C74.214,43.096 77.808,36.872 83.867,35.051 C83.981,35.017 84.115,34.98 84.384,34.905 C84.546,34.86 84.626,34.837 84.702,34.815 C88.611,33.669 91.669,30.612 92.815,26.702 C92.837,26.626 92.86,26.546 92.905,26.384 C92.98,26.115 93.017,25.981 93.051,25.867 C94.872,19.808 101.096,16.214 107.254,17.667 C107.37,17.695 107.505,17.729 107.775,17.799 C107.937,17.84 108.019,17.861 108.095,17.88 C112.054,18.842 116.231,17.723 119.178,14.91 C119.235,14.856 119.295,14.797 119.415,14.68 Z"
private const val EMPTY_BG_INKY = "M197.415,358.68C197.614,358.485 197.714,358.387 197.8,358.306C202.406,353.968 209.594,353.968 214.2,358.306C214.286,358.387 214.386,358.485 214.585,358.68C214.705,358.797 214.765,358.856 214.822,358.91C217.769,361.723 221.946,362.842 225.905,361.88C225.981,361.861 226.063,361.84 226.225,361.799C226.495,361.729 226.63,361.695 226.746,361.667C232.904,360.214 239.128,363.808 240.949,369.867C240.983,369.981 241.02,370.115 241.095,370.384C241.14,370.546 241.163,370.626 241.185,370.702C242.331,374.612 245.389,377.669 249.298,378.815C249.374,378.837 249.454,378.86 249.616,378.905C249.885,378.98 250.019,379.017 250.133,379.051C256.192,380.872 259.786,387.096 258.333,393.254C258.305,393.37 258.271,393.505 258.201,393.775C258.16,393.937 258.139,394.019 258.12,394.095C257.158,398.054 258.277,402.231 261.09,405.178C261.144,405.235 261.203,405.295 261.32,405.415C261.515,405.614 261.613,405.714 261.694,405.8C266.032,410.406 266.032,417.594 261.694,422.2C261.613,422.286 261.515,422.386 261.32,422.585C261.203,422.705 261.144,422.765 261.09,422.822C258.277,425.769 257.158,429.946 258.12,433.905C258.139,433.982 258.16,434.063 258.201,434.225C258.271,434.495 258.305,434.63 258.333,434.746C259.786,440.904 256.192,447.128 250.133,448.949C250.019,448.983 249.885,449.02 249.616,449.095C249.454,449.14 249.374,449.163 249.298,449.185C245.388,450.331 242.331,453.389 241.185,457.298C241.163,457.374 241.14,457.454 241.095,457.616C241.02,457.885 240.983,458.019 240.949,458.133C239.128,464.192 232.904,467.786 226.746,466.333C226.63,466.305 226.495,466.271 226.225,466.201C226.063,466.16 225.981,466.139 225.905,466.12C221.946,465.158 217.769,466.277 214.822,469.09C214.765,469.144 214.705,469.203 214.585,469.32C214.386,469.515 214.286,469.613 214.2,469.694C209.594,474.032 202.406,474.032 197.8,469.694C197.714,469.613 197.614,469.515 197.415,469.32C197.295,469.203 197.235,469.144 197.178,469.09C194.231,466.277 190.054,465.158 186.095,466.12C186.019,466.139 185.937,466.16 185.775,466.201C185.505,466.271 185.37,466.305 185.254,466.333C179.096,467.786 172.872,464.192 171.051,458.133C171.017,458.019 170.98,457.885 170.905,457.616C170.86,457.454 170.837,457.374 170.815,457.298C169.669,453.389 166.611,450.331 162.702,449.185C162.626,449.163 162.546,449.14 162.384,449.095C162.115,449.02 161.981,448.983 161.867,448.949C155.808,447.128 152.214,440.904 153.667,434.746C153.695,434.63 153.729,434.495 153.799,434.225C153.84,434.063 153.861,433.982 153.88,433.905C154.842,429.946 153.723,425.769 150.91,422.822C150.856,422.765 150.797,422.705 150.68,422.585C150.485,422.386 150.387,422.286 150.306,422.2C145.968,417.594 145.968,410.406 150.306,405.8C150.387,405.714 150.485,405.614 150.68,405.415C150.797,405.295 150.856,405.235 150.91,405.178C153.723,402.231 154.842,398.054 153.88,394.095C153.861,394.019 153.84,393.937 153.799,393.775C153.729,393.505 153.695,393.37 153.667,393.254C152.214,387.096 155.808,380.872 161.867,379.051C161.981,379.017 162.115,378.98 162.384,378.905C162.546,378.86 162.626,378.837 162.702,378.815C166.611,377.669 169.669,374.612 170.815,370.702C170.837,370.626 170.86,370.546 170.905,370.384C170.98,370.115 171.017,369.981 171.051,369.867C172.872,363.808 179.096,360.214 185.254,361.667C185.37,361.695 185.505,361.729 185.775,361.799C185.937,361.84 186.019,361.861 186.095,361.88C190.054,362.842 194.231,361.723 197.178,358.91C197.235,358.856 197.295,358.797 197.415,358.68Z"
private const val EMPTY_BG_CELLINA = "M159.15,414C154.334,408.23 151.433,400.783 151.433,392.654C151.433,374.329 166.173,359.474 184.355,359.474C192.584,359.474 200.108,362.517 205.88,367.547C211.638,362.517 219.144,359.474 227.354,359.474C245.494,359.474 260.199,374.329 260.199,392.654C260.199,400.783 257.305,408.23 252.5,414C257.305,419.77 260.199,427.217 260.199,435.346C260.199,453.671 245.494,468.526 227.354,468.526C219.144,468.526 211.638,465.483 205.88,460.453C200.108,465.483 192.584,468.526 184.355,468.526C166.173,468.526 151.433,453.671 151.433,435.346C151.433,427.217 154.334,419.77 159.15,414Z"

private const val EMPTY_DOC_BASE_ILLUSTRATION = "M131.443,39C132.269,39 133.057,39.155 133.806,39.465C134.555,39.775 135.214,40.214 135.783,40.782L150.818,55.817C151.386,56.386 151.825,57.045 152.135,57.794C152.445,58.543 152.6,59.331 152.6,60.158V94.8C152.6,96.505 151.993,97.965 150.779,99.179C149.565,100.393 148.105,101 146.4,101H109.2C107.495,101 106.035,100.393 104.821,99.179C103.607,97.965 103,96.505 103,94.8V45.2C103,43.495 103.607,42.035 104.821,40.821C106.035,39.607 107.495,39 109.2,39H131.443ZM113.629,86.519C113.127,86.519 112.706,86.688 112.366,87.028C112.027,87.367 111.857,87.788 111.857,88.29C111.857,88.792 112.027,89.213 112.366,89.552C112.706,89.892 113.127,90.061 113.629,90.061H141.972C142.474,90.061 142.894,89.892 143.234,89.552C143.573,89.213 143.743,88.792 143.743,88.29C143.743,87.788 143.573,87.367 143.234,87.028C142.894,86.688 142.474,86.519 141.972,86.519H113.629ZM113.629,77.661C113.127,77.661 112.706,77.831 112.366,78.171C112.027,78.51 111.857,78.931 111.857,79.433C111.857,79.935 112.027,80.355 112.366,80.695C112.706,81.035 113.127,81.204 113.629,81.204H141.972C142.474,81.204 142.894,81.035 143.234,80.695C143.573,80.355 143.743,79.935 143.743,79.433C143.743,78.931 143.573,78.51 143.234,78.171C142.894,77.831 142.474,77.661 141.972,77.661H113.629ZM113.629,68.804C113.127,68.804 112.706,68.974 112.366,69.314C112.027,69.653 111.857,70.074 111.857,70.576C111.857,71.078 112.027,71.498 112.366,71.838C112.706,72.177 113.127,72.347 113.629,72.347H131.343C131.845,72.347 132.266,72.177 132.605,71.838C132.945,71.498 133.114,71.078 133.114,70.576C133.114,70.074 132.945,69.653 132.605,69.314C132.266,68.974 131.845,68.804 131.343,68.804 H113.629ZM130.9,57.6C130.9,58.478 131.197,59.215 131.791,59.809C132.386,60.403 133.122,60.7 134,60.7H146.4L130.9,45.2V57.6ZM112.911,58.84H115.513V54.98H116.857C117.758,54.98 118.535,54.818 119.187,54.495C119.838,54.166 120.337,53.698 120.683,53.092C121.035,52.48 121.21,51.754 121.21,50.915V50.728C121.21,49.884 121.035,49.158 120.683,48.552C120.337,47.945 119.838,47.48 119.187,47.157C118.535,46.828 117.758,46.664 116.857,46.664H112.911V58.84ZM116.806,49.028C117.356,49.028 117.786,49.175 118.098,49.47C118.416,49.765 118.574,50.17 118.574,50.686V50.958C118.574,51.474 118.416,51.879 118.098,52.174C117.786,52.468 117.356,52.616 116.806,52.616H115.513V49.028H116.806Z"
private const val EMPTY_DOC_INKY = "M209.443,383C210.269,383 211.057,383.155 211.806,383.465C212.555,383.775 213.214,384.214 213.783,384.782L228.818,399.817C229.386,400.386 229.825,401.045 230.135,401.794C230.445,402.543 230.6,403.331 230.6,404.158V438.8C230.6,440.505 229.993,441.965 228.779,443.179C227.565,444.393 226.105,445 224.4,445H187.2C185.495,445 184.035,444.393 182.821,443.179C181.607,441.965 181,440.505 181,438.8V389.2C181,387.495 181.607,386.035 182.821,384.821C184.035,383.607 185.495,383 187.2,383H209.443ZM196.113,410.047C195.352,410.047 194.7,410.319 194.158,410.861C193.616,411.403 193.345,412.054 193.345,412.815V432.19C193.345,432.952 193.616,433.603 194.158,434.145C194.7,434.687 195.352,434.958 196.113,434.958H215.488C216.249,434.958 216.9,434.687 217.443,434.145C217.984,433.603 218.255,432.952 218.255,432.19V412.815C218.255,412.054 217.984,411.403 217.443,410.861C216.9,410.319 216.249,410.047 215.488,410.047H196.113ZM215.488,432.19H196.113V412.815H215.488V432.19ZM200.264,426.655C199.872,426.655 199.544,426.787 199.278,427.053C199.013,427.318 198.88,427.647 198.88,428.039C198.88,428.431 199.013,428.759 199.278,429.025C199.544,429.29 199.872,429.422 200.264,429.422H207.184C207.576,429.422 208.17,429.025C208.435,428.759 208.568,428.431 208.568,428.039C208.568,427.647 208.435,427.318 208.17,427.053C207.905,426.787 207.576,426.655 207.184,426.655H200.264ZM200.264,421.119C199.872,421.119 199.544,421.252 199.278,421.517C199.013,421.782 199.88,422.111 199.88,422.503C199.88,422.895 199.013,423.224 199.278,423.489C199.544,423.754 199.872,423.887 200.264,423.887H211.336C211.728,423.887 212.057,423.754 212.322,423.489C212.587,423.224 212.72,422.895 212.72,422.503C212.72,422.111 212.587,421.782 212.322,421.517C212.057,421.252 211.728,421.119 211.336,421.119H200.264ZM200.264,415.583C199.872,415.583 199.544,415.716 199.278,415.981C199.013,416.246 199.88,416.575 199.88,416.967C199.88,417.359 199.013,417.688 199.278,417.953C199.544,418.218 199.872,418.351 200.264,418.351H211.336C211.728,418.351 212.057,418.218 212.322,417.953C212.587,417.688 212.72,417.359 212.72,416.967C212.72,416.575 212.587,416.246 212.322,415.981C212.057,415.716 211.728,415.583 211.336,415.583H200.264ZM208.9,401.6C208.9,402.478 209.197,403.215 209.791,403.809C210.386,404.403 211.122,404.7 212,404.7H224.4L208.9,389.2V401.6ZM191.065,390.664V393.113H193.641V400.383H191.065V402.84H198.853V400.383H196.277V393.113H198.853V390.664H191.065Z"
private const val EMPTY_DOC_CELLINA = "M209.443,383C210.269,383 211.057,383.155 211.806,383.465C212.555,383.775 213.214,384.214 213.783,384.782L228.818,399.817C229.386,400.386 229.825,401.045 230.135,401.794C230.445,402.543 230.6,403.331 230.6,404.158V438.8C230.6,440.505 229.993,441.965 228.779,443.179C227.565,444.393 226.105,445 224.4,445H187.2C185.495,445 184.035,444.393 182.821,443.179C181.607,441.965 181,440.505 181,438.8V389.2C181,387.495 181.607,386.035 182.821,384.821C184.035,383.607 185.495,383 187.2,383H209.443ZM196.113,410.047C195.351,410.047 194.7,410.319 194.158,410.861C193.616,411.403 193.345,412.054 193.345,412.815V432.19C193.345,432.952 193.616,433.603 194.158,434.145C194.7,434.687 195.351,434.958 196.113,434.958H215.488C216.249,434.958 216.9,434.687 217.442,434.145C217.984,433.603 218.255,432.952 218.255,432.19V412.815C218.255,412.054 217.984,411.403 217.442,410.861C216.9,410.319 216.249,410.047 215.488,410.047H196.113ZM204.416,432.19H196.113V426.655H204.416V432.19ZM215.488,432.19H207.184V426.655H215.488V432.19ZM204.416,423.887H196.113V418.351H204.416V423.887ZM215.488,423.887H207.184V418.351H215.488V423.887ZM215.488,415.583H196.113V412.815H215.488V415.583ZM208.9,401.6C208.9,402.478 209.197,403.215 209.791,403.809C210.386,404.403 211.122,404.7 212,404.7H224.4L208.9,389.2V401.6ZM195.359,390.375C194.372,390.375 193.516,390.63 192.791,391.14C192.065,391.644 191.501,392.362 191.099,393.291C190.702,394.215 190.503,395.306 190.503,396.565V396.939C190.503,398.186 190.707,399.274 191.116,400.204C191.529,401.128 192.105,401.848 192.842,402.364C193.584,402.874 194.452,403.129 195.444,403.129C196.498,403.129 197.388,402.829 198.113,402.228C198.839,401.627 199.332,400.785 199.593,399.702L197.178,398.988C196.9,400.105 196.317,400.663 195.427,400.663C194.724,400.663 194.171,400.34 193.769,399.694C193.372,399.042 193.173,398.138 193.173,396.982V396.522C193.173,395.349 193.36,394.442 193.734,393.801C194.109,393.161 194.639,392.841 195.325,392.841C195.778,392.841 196.138,392.971 196.404,393.232C196.677,393.492 196.883,393.901 197.025,394.456L199.44,393.733C199.179,392.651 198.694,391.82 197.986,391.242C197.283,390.664 196.407,390.375 195.359,390.375Z"

private const val EMPTY_DOC_SLIDIA = "M131.443,39C132.269,39 133.057,39.155 133.806,39.465C134.555,39.775 135.214,40.214 135.783,40.782L150.818,55.817C151.386,56.386 151.825,57.045 152.135,57.794C152.445,58.543 152.6,59.331 152.6,60.158V94.8C152.6,96.505 151.993,97.965 150.779,99.179C149.565,100.393 148.105,101 146.4,101H109.2C107.495,101 106.035,100.393 104.821,99.179C103.607,97.965 103,96.505 103,94.8V45.2C103,43.495 103.607,42.035 104.821,40.821C106.035,39.607 107.495,39 109.2,39H131.443ZM113.629,86.519C113.127,86.519 112.706,86.688 112.366,87.028C112.027,87.367 111.857,87.788 111.857,88.29C111.857,88.792 112.027,89.213 112.366,89.552C112.706,89.892 113.127,90.061 113.629,90.061H141.972C142.474,90.061 142.894,89.892 143.234,89.552C143.573,89.213 143.743,88.792 143.743,88.29C143.743,87.788 143.573,87.367 143.234,87.028C142.894,86.688 142.474,86.519 141.972,86.519H113.629ZM113.629,77.661C113.127,77.661 112.706,77.831 112.366,78.171C112.027,78.51 111.857,78.931 111.857,79.433C111.857,79.935 112.027,80.355 112.366,80.695C112.706,81.035 113.127,81.204 113.629,81.204H141.972C142.474,81.204 142.894,81.035 143.234,80.695C143.573,80.355 143.743,79.935 143.743,79.433C143.743,78.931 143.573,78.51 143.234,78.171C142.894,77.831 142.474,77.661 141.972,77.661H113.629ZM130.9,57.6C130.9,58.478 131.197,59.215 131.791,59.809C132.386,60.403 133.122,60.7 134,60.7H146.4L130.9,45.2V57.6ZM112.911,58.84H121.21V54.98H116.857C114.758,54.98 112.911,53.092 112.911,50.915C112.911,48.552 114.838,46.664 116.857,46.664H121.21V50.5H116.857C116.1,50.5 115.5,51.1 115.5,51.8C115.5,52.5 116.1,53.1 116.857,53.1H121.21V58.84H112.911Z"

@Composable
fun RecentsEmptyStateIllustration(
    filter: String = "All",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDynamicEnabled = ThemeSettings.isDynamicColorEnabled(context)

    // When Material You dynamic accent color is active in Papirus settings, use dynamic Material 3 Expressive primary and onPrimary colors.
    // Otherwise, use each module's static status accent color with white icons.
    val shapeColor = if (isDynamicEnabled) {
        MaterialTheme.colorScheme.primary
    } else {
        when (filter) {
            "Inky Documents" -> BrandInky
            "Cellina Spreadsheets" -> BrandCellina
            "Slidia Presentations" -> BrandSlidia
            "Pagella PDF" -> BrandPagella
            else -> BrandBase
        }
    }

    val iconColor = if (isDynamicEnabled) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        Color.White
    }

    val (translateX, translateY, bgPathData, docPathData) = when (filter) {
        "Inky Documents" -> Quadruple(-136f, -344f, EMPTY_BG_INKY, EMPTY_DOC_INKY)
        "Cellina Spreadsheets" -> Quadruple(-136f, -344f, EMPTY_BG_CELLINA, EMPTY_DOC_CELLINA)
        "Slidia Presentations" -> Quadruple(-58f, 0f, EMPTY_BG_ROSETTE, EMPTY_DOC_SLIDIA)
        "Pagella PDF" -> Quadruple(-58f, 0f, EMPTY_BG_ROSETTE, EMPTY_DOC_BASE_ILLUSTRATION)
        else -> Quadruple(-58f, 0f, EMPTY_BG_ROSETTE, EMPTY_DOC_BASE_ILLUSTRATION)
    }

    val imageVector = remember(filter, shapeColor, iconColor) {
        ImageVector.Builder(
            name = "recents_empty_$filter",
            defaultWidth = 140.dp,
            defaultHeight = 140.dp,
            viewportWidth = 140f,
            viewportHeight = 140f
        ).apply {
            group(
                translationX = translateX,
                translationY = translateY
            ) {
                addPath(
                    pathData = PathParser().parsePathString(bgPathData).toNodes(),
                    fill = SolidColor(shapeColor)
                )
                addPath(
                    pathData = PathParser().parsePathString(docPathData).toNodes(),
                    fill = SolidColor(iconColor)
                )
            }
        }.build()
    }

    Image(
        painter = rememberVectorPainter(image = imageVector),
        contentDescription = null,
        modifier = modifier
    )
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

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
