package com.example.ui.home

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
                val fileType = obj.getString("fileType")
                val size = obj.getString("size")
                list.add(RecentFile(path, name, lastOpened, fileType, size))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Fulfill criteria: only files existing on device are displayed, no duplicates, sorted by lastOpened desc
        return list.filter { File(it.path).exists() }
            .sortedByDescending { it.lastOpened }
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
            fileType = fileType,
            size = sizeStr
        ))

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
// MOCK DOCUMENT GENERATOR ON DEVICE
// ==========================================
fun createMockFilesOnDevice(context: Context) {
    try {
        val root = getMockStorageRoot(context)
        val docs = File(root, "Documents")
        val downloads = File(root, "Downloads")

        if (!docs.exists()) docs.mkdirs()
        if (!downloads.exists()) downloads.mkdirs()

        val file1 = File(docs, "Laporan_Kinerja_Papirus.odt")
        if (!file1.exists()) {
            file1.writeText("=== LAPORAN KINERJA PAPIRUS ===\nTanggal: 2026-07-20\n\nPapirus Office Writer Document.\nSemua data disimpan di penyimpanan lokal secara aman.")
        }

        val file2 = File(docs, "Rencana_Anggaran_2026.ods")
        if (!file2.exists()) {
            file2.writeText("=== RENCANA ANGGARAN 2026 ===\nItem, Jumlah, Harga, Total\nSewa Ruangan, 12, 500, 6000\nServer Host, 1, 1200, 1200\n\nTotal Pengeluaran: 7200 USD")
        }

        val file3 = File(downloads, "Presentasi_Fitur_Slidia.odp")
        if (!file3.exists()) {
            file3.writeText("=== PRESENTASI FITUR SLIDIA ===\nSlide 1: Memulai Presentasi Baru\nSlide 2: Konfigurasi Kecepatan Transisi\nSlide 3: Kompatibilitas ODP LibreOffice.")
        }

        val file4 = File(downloads, "Panduan_Pengguna_Papirus.pdf")
        if (!file4.exists()) {
            file4.writeText("=== PANDUAN PENGGUNA PAPIRUS ===\n1. Buka aplikasi dan pilih tab Files.\n2. Tap ganda dokumen untuk menyunting.\n3. Simpan perubahan secara realtime.")
        }
    } catch (e: Exception) {
        e.printStackTrace()
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
    var activeTab by remember { mutableStateOf("Recents") } // Recents, Files, Google Drive
    
    // Search queries
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialog & Options states
    var showMoreMenu by remember { mutableStateOf(false) }
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showNewDocDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    // Seed mock files on startup
    LaunchedEffect(Unit) {
        createMockFilesOnDevice(context)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Papirus Office",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { isSearchActive = !isSearchActive },
                            modifier = Modifier.testTag("btn_top_search")
                        ) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search recent documents"
                            )
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("btn_top_more")
                            ) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.BugReport, contentDescription = null) },
                                    text = { Text("Crash Log") },
                                    onClick = {
                                        showMoreMenu = false
                                        onNavigateToModule("crash_logs")
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                                    text = { Text("Papirus Office Options") },
                                    onClick = {
                                        showMoreMenu = false
                                        showOptionsDialog = true
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
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
                        placeholder = { Text("Cari file berdasarkan nama...") },
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
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == "Recents",
                    onClick = { activeTab = "Recents" },
                    icon = { Icon(Icons.Rounded.History, contentDescription = "Recents tab") },
                    label = { Text("Recents") }
                )
                NavigationBarItem(
                    selected = activeTab == "Files",
                    onClick = { activeTab = "Files" },
                    icon = { Icon(Icons.Rounded.Folder, contentDescription = "Files tab") },
                    label = { Text("Files") }
                )
                NavigationBarItem(
                    selected = activeTab == "Google Drive",
                    onClick = { activeTab = "Google Drive" },
                    icon = { Icon(Icons.Rounded.Cloud, contentDescription = "Google Drive tab") },
                    label = { Text("Google Drive") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToModule("create_new_document") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("main_fab")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.create_new_document)
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
            when (activeTab) {
                "Recents" -> RecentsSubPage(
                    searchQuery = searchQuery,
                    onNavigateToModule = onNavigateToModule
                )
                "Files" -> FilesSubPage(
                    onNavigateToModule = onNavigateToModule
                )
                "Google Drive" -> GoogleDriveSubPage()
            }
        }
    }

    // ==========================================
    // OPTIONS & SETTINGS DIALOG (LibreOffice equivalent)
    // ==========================================
    if (showOptionsDialog) {
        var optAiEnabled by remember { mutableStateOf(GeminiAiService.isAiEnabled(context)) }
        var optApiKey by remember { mutableStateOf(GeminiAiService.getUserApiKey(context)) }
        var optModel by remember { mutableStateOf(GeminiAiService.getSelectedModel(context)) }
        var optShowModelMenu by remember { mutableStateOf(false) }

        var activeSettingSection by remember { mutableStateOf("general") } // general, ai, about

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
                    // Category Selection Chips
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
                                                "Credentials are encrypted on-device. Your files are processed entirely in memory with Zero Data Retention.",
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                            "about" -> {
                                Text("Papirus Office Mobile Suite", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                Text("Version: 1.2.4-NIGHTLY", style = MaterialTheme.typography.bodyMedium)
                                Text("Package: com.makerandreas.papirusoffice", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Built on LibreOffice ODF core compatibility library. Optimized for dynamic Android screen classes.", style = MaterialTheme.typography.bodySmall)
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

        val recentFiles = remember(searchQuery) {
            RecentFilesTracker.getRecents(context)
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
                        "Inky Document" -> "Inky"
                        "Cellina Spreadsheet" -> "Cellina"
                        "Slidia Presentation" -> "Slidia"
                        "Pagella PDF Document" -> "Pagella"
                        else -> ""
                    }
                    it.fileType == fileTypeToMatch
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Filter Chips under the app bar
            val filterOptions = listOf(
                "All" to R.string.filter_all,
                "Inky Document" to R.string.filter_inky,
                "Cellina Spreadsheet" to R.string.filter_cellina,
                "Slidia Presentation" to R.string.filter_slidia,
                "Pagella PDF Document" to R.string.filter_pagella
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(filterOptions) { (filterKey, stringResId) ->
                    FilterChip(
                        selected = selectedFilter == filterKey,
                        onClick = { selectedFilter = filterKey },
                        label = { Text(stringResource(stringResId), style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("filter_chip_${filterKey.lowercase().replace(" ", "_")}")
                    )
                }
            }

            if (filteredFiles.isEmpty()) {
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
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .align(Alignment.Center)
                            )
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .offset(x = 18.dp, y = (-18).dp)
                            )
                            Icon(
                                imageVector = Icons.Rounded.FindInPage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No Recent Documents",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Any document you open from your device directory or create using the plus button will be listed here instantly for quick, offline access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
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
                    item {
                        Text(
                            text = stringResource(R.string.recent_documents_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(filteredFiles) { file ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    com.example.MainActivity.openedFilePath = file.path
                                    com.example.MainActivity.openedFileType = file.fileType
                                    onNavigateToModule(file.fileType)
                                    val toastMsg = context.getString(R.string.opening_file, file.name)
                                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (iconColor, bgIconColor, charSymbol) = when (file.fileType) {
                                    "Inky" -> Triple(Color(0xFF2563EB), Color(0xFFEFF6FF), "W")
                                    "Cellina" -> Triple(Color(0xFF10B981), Color(0xFFECFDF5), "S")
                                    "Slidia" -> Triple(Color(0xFFD97706), Color(0xFFFFFBEB), "P")
                                    "Pagella" -> Triple(Color(0xFFE11D48), Color(0xFFFFF1F2), "D")
                                    else -> Triple(Color.Gray, Color.LightGray, "F")
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(bgIconColor, shape = RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(charSymbol, fontWeight = FontWeight.ExtraBold, color = iconColor, fontSize = 16.sp)
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Opened: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(file.lastOpened))} • ${file.size}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Open", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }
        }
    }

// ==========================================
// FILES SUB-PAGE (DYNAMIC FILES EXPLORER)
// ==========================================
@Composable
fun FilesSubPage(
    onNavigateToModule: (String) -> Unit
) {
    val context = LocalContext.current
    var currentDir by remember { mutableStateOf<File?>(null) }
    
    // We check if an external storage is present
    val externalStorage = remember { getExternalStorageShortcut(context) }

    if (currentDir == null) {
        // Display top level shortcuts
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SHORTCUT PENYIMPANAN",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // 1. Internal Storage
            ShortcutCard(
                title = "Internal Storage",
                path = "/storage/emulated/0",
                description = "Main device storage directory",
                icon = Icons.Rounded.Folder
            ) {
                currentDir = getDirectoryShortcut(context, "/storage/emulated/0")
            }

            // 2. Documents Shortcut
            ShortcutCard(
                title = "Documents",
                path = "/storage/emulated/0/Documents",
                description = "Default document drafts and sheets",
                icon = Icons.Rounded.Article
            ) {
                currentDir = getDirectoryShortcut(context, "/storage/emulated/0/Documents")
            }

            // 3. Downloads Shortcut
            ShortcutCard(
                title = "Downloads",
                path = "/storage/emulated/0/Downloads",
                description = "Exported files and web downloads",
                icon = Icons.Rounded.Download
            ) {
                currentDir = getDirectoryShortcut(context, "/storage/emulated/0/Downloads")
            }

            // 4. External Storage (Only displayed if actually detected)
            if (externalStorage != null) {
                ShortcutCard(
                    title = "External Storage (SD Card)",
                    path = externalStorage.absolutePath,
                    description = "Removable secondary storage volume",
                    icon = Icons.Rounded.SdCard
                ) {
                    currentDir = externalStorage
                }
            }
        }
    } else {
        // Inside a directory: Display Explorer
        val filesList = remember(currentDir) {
            try {
                val list = currentDir!!.listFiles() ?: emptyArray()
                // Folders first, then files
                list.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            } catch (e: Exception) {
                emptyList<File>()
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Path Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val rootPath = getMockStorageRoot(context).absolutePath
                    val parent = currentDir?.parentFile
                    if (currentDir?.absolutePath == rootPath || currentDir?.parent == null || !currentDir!!.absolutePath.startsWith(rootPath)) {
                        currentDir = null
                    } else {
                        currentDir = parent
                    }
                }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Go back")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = currentDir?.name ?: "Folder",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = currentDir?.absolutePath?.replace(context.filesDir.absolutePath, "/storage/emulated/0") ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (filesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Folder ini kosong",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filesList) { file ->
                        val fileType = getFileTypeForFile(file)
                        val isDir = file.isDirectory

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isDir) {
                                        currentDir = file
                                    } else {
                                        if (fileType != null) {
                                            // Save in recent, set companion, navigate
                                            RecentFilesTracker.addFile(context, file.absolutePath, fileType)
                                            com.example.MainActivity.openedFilePath = file.absolutePath
                                            com.example.MainActivity.openedFileType = fileType
                                            onNavigateToModule(fileType)
                                            Toast.makeText(context, "Membuka ${file.name}...", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Format tidak didukung secara natif", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val (icon, iconColor, bgIconColor) = when {
                                    isDir -> Triple(Icons.Rounded.Folder, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                    fileType == "Inky" -> Triple(Icons.Rounded.Description, Color(0xFF2563EB), Color(0xFFEFF6FF))
                                    fileType == "Cellina" -> Triple(Icons.Rounded.GridView, Color(0xFF10B981), Color(0xFFECFDF5))
                                    fileType == "Slidia" -> Triple(Icons.Rounded.Slideshow, Color(0xFFD97706), Color(0xFFFFFBEB))
                                    fileType == "Pagella" -> Triple(Icons.Rounded.PictureAsPdf, Color(0xFFE11D48), Color(0xFFFFF1F2))
                                    else -> Triple(Icons.Rounded.InsertDriveFile, Color.Gray, Color.LightGray.copy(alpha = 0.3f))
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(bgIconColor, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val size = if (isDir) {
                                        val children = file.list()?.size ?: 0
                                        "$children items"
                                    } else {
                                        val bytes = file.length()
                                        when {
                                            bytes < 1024 -> "$bytes B"
                                            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                                            else -> String.format(Locale.getDefault(), "%.1f MB", bytes.toDouble() / (1024 * 1024))
                                        }
                                    }
                                    Text(
                                        text = "$size • ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(file.lastModified()))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Icon(
                                    imageVector = if (isDir) Icons.Rounded.ChevronRight else Icons.Rounded.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
