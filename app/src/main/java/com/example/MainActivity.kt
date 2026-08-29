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
import com.example.ui.home.AboutScreen
import com.example.ui.theme.PapirusTheme
import android.os.Build
import android.os.Environment
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Intent
import com.example.ui.home.RecentFilesTracker

class MainActivity : ComponentActivity() {
    companion object {
        var openedFilePath: String? = null
        var openedFileType: String? = null
        var newDocIndex: Int = 1
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val dataUri = intent.data
        if ((action == Intent.ACTION_VIEW || action == Intent.ACTION_SEND) && dataUri != null) {
            try {
                var displayName = "document.odt"
                contentResolver.query(dataUri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        displayName = cursor.getString(nameIndex)
                    }
                }
                
                val lowerName = displayName.lowercase()
                val fileType = when {
                    lowerName.endsWith(".ods") || lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls") -> "Cellina"
                    lowerName.endsWith(".odp") || lowerName.endsWith(".pptx") || lowerName.endsWith(".ppt") -> "Slidia"
                    lowerName.endsWith(".pdf") -> "Pagella"
                    else -> "Inky"
                }
                
                val cacheFile = java.io.File(cacheDir, displayName)
                contentResolver.openInputStream(dataUri)?.use { input ->
                    java.io.FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
                
                openedFilePath = cacheFile.absolutePath
                openedFileType = fileType
                
                // Track in recent files too
                RecentFilesTracker.addFile(this, cacheFile.absolutePath, fileType)
                
                Toast.makeText(this, "Opening: $displayName", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to resolve file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize global system crash notification handler & log manager
        com.makerandreas.papirusoffice.data.crash.CrashHandlerManager.init(this)

        handleIntent(intent)
        
        // Request POST_NOTIFICATIONS permission for Android 13+ if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Initialize Firebase if needed
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                com.google.firebase.FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            android.util.Log.w("MainActivity", "FirebaseApp init: ${e.message}")
        }

        // Initialize JNI LibreOffice Core configuration
        val cacheDir = cacheDir.absolutePath
        LibreOfficeCore.initialize(
            cacheDir = cacheDir,
            enableOoxml = BuildConfig.ENABLE_OOXML_SUPPORT,
            enableOmml = BuildConfig.ENABLE_OMML_PARSER
        )

        // Schedule periodic Room cache cleanup worker
        try {
            com.makerandreas.papirusoffice.data.cache.DocumentCacheCleanupWorker.schedulePeriodicCleanup(this)
            com.makerandreas.papirusoffice.data.worker.PapirusWorkScheduler.schedulePeriodicCacheCleanup(this)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "WorkScheduler initialization skipped/failed: ${e.message}")
        }

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
    var currentWorkspace by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("home") }

    val activity = context as? android.app.Activity
    LaunchedEffect(activity?.intent) {
        if (activity?.intent?.getBooleanExtra("OPEN_CRASH_LOGS", false) == true) {
            currentWorkspace = "crash_logs"
        }
    }

    LaunchedEffect(MainActivity.openedFilePath, MainActivity.openedFileType) {
        val path = MainActivity.openedFilePath
        val type = MainActivity.openedFileType
        if (path != null && type != null) {
            currentWorkspace = type
        }
    }

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

    // Dynamic theme and theme mode preference state
    var dynamicColorEnabled by remember {
        mutableStateOf(com.example.ui.theme.ThemeSettings.isDynamicColorEnabled(context))
    }
    var themeMode by remember {
        mutableStateOf(com.example.ui.theme.ThemeSettings.getThemeMode(context))
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }

    PapirusTheme(
        darkTheme = isDark,
        workspace = currentWorkspace,
        dynamicColor = dynamicColorEnabled
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                val normalizedWorkspace = when (workspace) {
                    "home", "create_new_document", "crash_logs", "about", "welcome", "Inky", "Cellina", "Slidia", "Pagella" -> workspace
                    else -> {
                        val lower = workspace.lowercase()
                        when {
                            lower.endsWith(".ods") || lower.endsWith(".ots") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") -> "Cellina"
                            lower.endsWith(".odp") || lower.endsWith(".otp") || lower.endsWith(".pptx") || lower.endsWith(".ppt") -> "Slidia"
                            lower.endsWith(".pdf") -> "Pagella"
                            else -> "Inky"
                        }
                    }
                }

                when (normalizedWorkspace) {
                    "welcome" -> WelcomeScreen(
                        onAccessGranted = {
                            currentWorkspace = "home"
                        }
                    )
                    "home" -> HomeDashboard(
                        isTablet = isTablet,
                        onNavigateToModule = { workspaceName ->
                            val targetModule = when (workspaceName) {
                                "home", "create_new_document", "crash_logs", "about", "welcome", "Inky", "Cellina", "Slidia", "Pagella" -> workspaceName
                                else -> {
                                    val lower = workspaceName.lowercase()
                                    when {
                                        lower.endsWith(".ods") || lower.endsWith(".ots") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") -> "Cellina"
                                        lower.endsWith(".odp") || lower.endsWith(".otp") || lower.endsWith(".pptx") || lower.endsWith(".ppt") -> "Slidia"
                                        lower.endsWith(".pdf") -> "Pagella"
                                        else -> "Inky"
                                    }
                                }
                            }
                            currentWorkspace = targetModule
                        },
                        dynamicColorEnabled = dynamicColorEnabled,
                        onDynamicColorChange = {
                            dynamicColorEnabled = it
                            themeMode = com.example.ui.theme.ThemeSettings.getThemeMode(context)
                        }
                    )
                    "create_new_document" -> NewDocumentScreen(
                        onBack = { currentWorkspace = "home" },
                        onNavigateToModule = { workspaceName ->
                            val targetModule = when (workspaceName) {
                                "home", "create_new_document", "crash_logs", "about", "welcome", "Inky", "Cellina", "Slidia", "Pagella" -> workspaceName
                                else -> {
                                    val lower = workspaceName.lowercase()
                                    when {
                                        lower.endsWith(".ods") || lower.endsWith(".ots") || lower.endsWith(".xlsx") || lower.endsWith(".xls") || lower.endsWith(".csv") -> "Cellina"
                                        lower.endsWith(".odp") || lower.endsWith(".otp") || lower.endsWith(".pptx") || lower.endsWith(".ppt") -> "Slidia"
                                        lower.endsWith(".pdf") -> "Pagella"
                                        else -> "Inky"
                                    }
                                }
                            }
                            currentWorkspace = targetModule
                        }
                    )
                    "crash_logs" -> CrashLogsScreen(
                        onBack = { currentWorkspace = "home" }
                    )
                    "about" -> AboutScreen(
                        onBack = { currentWorkspace = "home" }
                    )
                    "Inky" -> InkyModule(
                        isTablet = isTablet,
                        onFormatAction = { act ->
                            if (act == "Back to start center") {
                                currentWorkspace = "home"
                            } else if (act == "crash_logs") {
                                currentWorkspace = "crash_logs"
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
    }
}
