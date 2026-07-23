package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.jni.LibreOfficeCore
import com.example.modules.cellina.CellinaModule
import com.example.modules.inky.InkyModule
import com.example.modules.pagella.PagellaModule
import com.example.modules.slidia.SlidiaModule
import com.example.ui.components.*
import com.example.ui.home.HomeDashboard
import com.example.ui.home.CrashLogsScreen
import com.example.ui.home.NewDocumentScreen
import com.example.ui.home.WelcomeScreen
import com.example.ui.theme.PapirusTheme
import android.os.Build
import android.os.Environment
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    companion object {
        var openedFilePath: String? = null
        var openedFileType: String? = null
        var newDocIndex: Int = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup uncaught exception handler to capture real crashes in crash.log
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = java.io.File(filesDir, "crash.log")
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date())
                val stackTraceString = android.util.Log.getStackTraceString(throwable)
                
                val content = """
                    === CRASH REPORT ===
                    Timestamp: $dateStr
                    Thread: ${thread.name}
                    Exception: ${throwable.javaClass.name}
                    Message: ${throwable.message ?: "No message provided"}
                    
                    StackTrace:
                    $stackTraceString
                    === END CRASH REPORT ===
                """.trimIndent()
                
                file.appendText(content + "\n\n")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        // Initialize JNI LibreOffice Core configuration
        val cacheDir = cacheDir.absolutePath
        LibreOfficeCore.initialize(
            cacheDir = cacheDir,
            enableOoxml = BuildConfig.ENABLE_OOXML_SUPPORT,
            enableOmml = BuildConfig.ENABLE_OMML_PARSER
        )

        enableEdgeToEdge()
        setContent {
            PapirusTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    PapirusAppletContainer(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapirusAppletContainer(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    
    // Adaptive device size calculation: Width >= 600dp represents tablets or foldables
    val isTablet = configuration.screenWidthDp >= 600

    // Master Workspace Navigation State
    // "welcome" (Onboarding), "home" (Start Center / Dashboard), "Inky" (Writer), "Cellina" (Calc), "Slidia" (Impress), "Pagella" (PDF)
    var currentWorkspace by remember { mutableStateOf("home") }

    BackHandler(enabled = currentWorkspace != "home" && currentWorkspace != "welcome") {
        currentWorkspace = "home"
    }
    
    // Check initial permission state
    LaunchedEffect(Unit) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) {
            currentWorkspace = "welcome"
        }
    }

    // Dynamic theme preference state
    var dynamicColorEnabled by remember {
        mutableStateOf(com.example.ui.theme.ThemeSettings.isDynamicColorEnabled(context))
    }

    // Adaptive formatting toolbar / ribbon bar states
    var ribbonVisible by remember { mutableStateOf(false) }
    var selectedRibbonCategory by remember { mutableStateOf("Home") }
    var formattingObjectType by remember { mutableStateOf("text") } // text, table, image, chart, formula

    // Search bar state
    var showFindReplace by remember { mutableStateOf(false) }

    // Sync active object formatting depending on selected workspace
    LaunchedEffect(currentWorkspace) {
        formattingObjectType = when (currentWorkspace) {
            "Inky" -> "text"
            "Cellina" -> "formula"
            "Slidia" -> "chart"
            "Pagella" -> "image"
            else -> "text"
        }
        // Hide ribbon on home or pdf switch
        if (currentWorkspace == "home" || currentWorkspace == "Pagella") {
            ribbonVisible = false
        }
    }

    PapirusTheme(
        workspace = currentWorkspace,
        dynamicColor = dynamicColorEnabled
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Global Header TopBar (Excluded for document modules which manage their own topbar/status bar)
            if (currentWorkspace != "home" && currentWorkspace != "Inky" && currentWorkspace != "Cellina" && currentWorkspace != "Slidia" && currentWorkspace != "Pagella" && currentWorkspace != "crash_logs" && currentWorkspace != "create_new_document" && currentWorkspace != "welcome") {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentWorkspace != "home") {
                                IconButton(
                                    onClick = { currentWorkspace = "home" },
                                    modifier = Modifier.testTag("btn_back_home")
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = "Back to Start Center")
                                }
                            }
                            Text(
                                text = if (currentWorkspace == "home") "Papirus Office" else "Papirus — $currentWorkspace Workspace",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    },
                    actions = {
                        // Quick shortcut to launch Find & Replace
                        if (currentWorkspace != "home") {
                            IconButton(onClick = { showFindReplace = !showFindReplace }) {
                                Icon(Icons.Default.Search, contentDescription = "Find & Replace")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )

                // Quick Access Toolbar (QAT)
                QuickAccessToolbar(
                    isTablet = isTablet,
                    onActionClick = { actionName ->
                        when (actionName) {
                            "Save" -> Toast.makeText(context, "Document Saved!", Toast.LENGTH_SHORT).show()
                            "Undo" -> Toast.makeText(context, "Undo performed", Toast.LENGTH_SHORT).show()
                            "Redo" -> Toast.makeText(context, "Redo performed", Toast.LENGTH_SHORT).show()
                            "Share" -> Toast.makeText(context, "Opening Android Share Sheet...", Toast.LENGTH_SHORT).show()
                            "Search" -> showFindReplace = !showFindReplace
                            "AI" -> Toast.makeText(context, "AI features can be toggled in settings below.", Toast.LENGTH_LONG).show()
                            else -> Toast.makeText(context, "Clicked: $actionName", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Find & Replace Overlay Bar (Mobile / Tablet adaptive formats)
            AnimatedVisibility(
                visible = showFindReplace,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                FindAndReplaceBar(
                    isTablet = isTablet,
                    onFind = { query ->
                        Toast.makeText(context, "Searching document for: $query", Toast.LENGTH_SHORT).show()
                    },
                    onReplace = { find, replace ->
                        Toast.makeText(context, "Replaced instances of '$find' with '$replace'", Toast.LENGTH_SHORT).show()
                    },
                    onClose = { showFindReplace = false }
                )
            }

            // Expanded Tablet Ribbon bar docking area
            if (isTablet && ribbonVisible && currentWorkspace != "home" && currentWorkspace != "Inky" && currentWorkspace != "Cellina" && currentWorkspace != "Slidia" && currentWorkspace != "Pagella" && currentWorkspace != "welcome") {
                RibbonFullView(
                    selectedCategory = selectedRibbonCategory,
                    onCategoryChange = { selectedRibbonCategory = it },
                    onActionClick = { action ->
                        Toast.makeText(context, "Ribbon: $action", Toast.LENGTH_SHORT).show()
                    },
                    moduleContext = currentWorkspace.lowercase()
                )
            }

            // Core Workspace display area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = currentWorkspace,
                    transitionSpec = {
                        val isGoingBack = targetState == "home" || targetState == "welcome"
                        if (isGoingBack) {
                            (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                            )
                        } else {
                            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                            )
                        }
                    },
                    label = "WorkspaceTransition"
                ) { workspace ->
                    when (workspace) {
                        "welcome" -> WelcomeScreen(
                            onAccessGranted = {
                                currentWorkspace = "home"
                            }
                        )
                        "home" -> HomeDashboard(
                            isTablet = isTablet,
                            onNavigateToModule = { workspaceName ->
                                currentWorkspace = workspaceName
                            },
                            dynamicColorEnabled = dynamicColorEnabled,
                            onDynamicColorChange = { dynamicColorEnabled = it }
                        )
                        "create_new_document" -> NewDocumentScreen(
                            onBack = { currentWorkspace = "home" },
                            onNavigateToModule = { workspaceName ->
                                currentWorkspace = workspaceName
                            }
                        )
                        "crash_logs" -> CrashLogsScreen(
                            onBack = { currentWorkspace = "home" }
                        )
                        "Inky" -> InkyModule(
                            isTablet = isTablet,
                            onFormatAction = { act ->
                                if (act == "Back to start center") {
                                    currentWorkspace = "home"
                                } else {
                                    Toast.makeText(context, act, Toast.LENGTH_SHORT).show()
                                }
                            },
                            dynamicColorEnabled = dynamicColorEnabled,
                            onDynamicColorChange = { dynamicColorEnabled = it }
                        )
                        "Cellina" -> CellinaModule(
                            isTablet = isTablet,
                            onFormulaSelected = { formula ->
                                Toast.makeText(context, "Formula: $formula", Toast.LENGTH_SHORT).show()
                            },
                            onBack = { currentWorkspace = "home" }
                        )
                        "Slidia" -> SlidiaModule(
                            isTablet = isTablet,
                            onTransitionSelected = { trans ->
                                Toast.makeText(context, trans, Toast.LENGTH_SHORT).show()
                            },
                            onBack = { currentWorkspace = "home" }
                        )
                        "Pagella" -> PagellaModule(
                            isTablet = isTablet,
                            onPdfAction = { action ->
                                Toast.makeText(context, action, Toast.LENGTH_SHORT).show()
                            },
                            onBack = { currentWorkspace = "home" }
                        )
                    }
                }
            }

            // Mobile dropdown ribbon sheet
            if (!isTablet && ribbonVisible && currentWorkspace != "home" && currentWorkspace != "Inky" && currentWorkspace != "Cellina" && currentWorkspace != "Slidia" && currentWorkspace != "Pagella" && currentWorkspace != "welcome") {
                SimplifiedRibbonBar(
                    selectedCategory = selectedRibbonCategory,
                    onCategoryChange = { selectedRibbonCategory = it },
                    onCloseRibbon = { ribbonVisible = false },
                    onActionClick = { action ->
                        Toast.makeText(context, "Ribbon action: $action", Toast.LENGTH_SHORT).show()
                    },
                    moduleContext = currentWorkspace.lowercase()
                )
            }

            // Bottom Adaptive Formatting Toolbar (Visible in document workspaces)
            if (currentWorkspace != "home" && currentWorkspace != "Inky" && currentWorkspace != "Cellina" && currentWorkspace != "Slidia" && currentWorkspace != "Pagella" && currentWorkspace != "crash_logs" && currentWorkspace != "create_new_document" && currentWorkspace != "welcome") {
                AdaptiveFormattingToolbar(
                    selectedObjectType = formattingObjectType,
                    onFormatClick = { formatAction ->
                        Toast.makeText(context, "Format style: $formatAction", Toast.LENGTH_SHORT).show()
                        // Toggle contextual element formatting simulation
                        if (formatAction == "toggle_header") {
                            formattingObjectType = "text"
                        } else if (formatAction == "helper") {
                            formattingObjectType = "formula"
                        }
                    },
                    onToggleKeyboard = {
                        Toast.makeText(context, "Virtual Keyboard Toggled", Toast.LENGTH_SHORT).show()
                    },
                    onToggleRibbon = {
                        ribbonVisible = !ribbonVisible
                    }
                )
            }
        }
    }
}
