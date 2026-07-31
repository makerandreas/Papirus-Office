package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makerandreas.papirusoffice.data.framework.PapirusClipboardEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalClipboardSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Tester, 1 = Document Scenarios, 2 = SDK Examples, 3 = Help & Optimization

    // SDK Code Examples list
    val sdkExamples = remember {
        listOf(
            "Clip.java" to "Support class implementing com.sun.star.datatransfer.clipboard.XSystemClipboard helper methods.",
            "CPTests.java" to "Office Clipboard API verification: setting text, listing current mime flavors, reading contents.",
            "JCPTests.java" to "Java Clipboard API verification: copying and extracting 2D serialized Object arrays.",
            "CopyPasteText.java" to "Writer sentence copier using visible cursor view highlight and dispatches.",
            "CopyPasteCalc.java" to "Calc cell range copier utilizing both rich 2D arrays and OS selection dispatches.",
            "CopySlide.java" to "Impress slide copy routine switching view to Slide Sorter (DiaMode) and capturing slides.",
            "CopyResultSet.java" to "Base database SQL query rows serializer copying forward-only data cursors to JClip."
        )
    }
    var selectedExampleFile by remember { mutableStateOf(sdkExamples.first().first) }
    var sdkCodeContent by remember { mutableStateOf("Loading SDK code example...") }

    // Read example contents dynamically from assets
    LaunchedEffect(selectedExampleFile) {
        sdkCodeContent = try {
            context.assets.open("sdk_examples/$selectedExampleFile").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "// Failed to load example: ${e.localizedMessage}"
        }
    }

    // Live logs
    var liveLogs by remember { mutableStateOf(listOf<String>()) }
    val refreshLogs = {
        liveLogs = PapirusClipboardEngine.getLogs()
    }

    LaunchedEffect(Unit) {
        PapirusClipboardEngine.clearLogs()
        refreshLogs()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .testTag("clipboard_framework_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Clipboard Framework icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Papirus Clipboard & Selection Framework",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LibreOffice SDK Ch.43 & Native Android ClipboardManager",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_clipboard_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close clipboard dialog")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector (Scrollable to fit well on all screen sizes)
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("API Tester", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Handyman, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Doc Scenarios", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Task, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("SDK Java Examples", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Help & Optimization", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content pane + Fixed logs at bottom
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> ApiTesterTab(onRefreshLogs = refreshLogs)
                            1 -> DocumentScenariosTab(onRefreshLogs = refreshLogs)
                            2 -> SdkExamplesTab(
                                sdkExamples = sdkExamples,
                                selectedExampleFile = selectedExampleFile,
                                onSelectedExampleFileChange = { selectedExampleFile = it },
                                sdkCodeContent = sdkCodeContent,
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(sdkCodeContent))
                                    Toast.makeText(context, "SDK Java code copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            3 -> HelpAndOptimizationTab()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time Console Log Buffer (Shared)
                    TerminalLogs(liveLogs = liveLogs, onClearLogs = {
                        PapirusClipboardEngine.clearLogs()
                        refreshLogs()
                    })
                }
            }
        }
    }
}

// --- 1. API TESTER TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiTesterTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf("Excellent copy-paste simulation!") }
    var retrievedText by remember { mutableStateOf("") }
    var generatedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var retrievedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Setup dummy table for 2D array copy
    val sourceArray = remember {
        arrayOf(
            arrayOf<Any>("ID", "Product", "Qnty", "Price"),
            arrayOf<Any>("P101", "Fountain Pen", 5, 24.5),
            arrayOf<Any>("P102", "Oasis Sketchbook", 2, 45.0),
            arrayOf<Any>("P103", "Charcoal Set", 12, 15.75)
        )
    }
    var retrievedArray by remember { mutableStateOf<Array<Array<Any>>?>(null) }

    LaunchedEffect(Unit) {
        // Generate a small decorative bitmap for testing
        val width = 120
        val height = 120
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = android.graphics.Color.BLUE
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 14f
        canvas.drawText("LO CH43", 20f, 65f, paint)
        generatedBitmap = bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: Text Transfer (Clip.java & JClip.java)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Text Clipboard Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Text to Copy") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("clip_text_input"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val success = PapirusClipboardEngine.clipSetText(context, textInput)
                            onRefreshLogs()
                            if (success) Toast.makeText(context, "Text set via Clip.java!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("clip_set_text_button")
                    ) {
                        Icon(Icons.Rounded.CopyAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clip.java (LO)")
                    }

                    Button(
                        onClick = {
                            val success = PapirusClipboardEngine.jClipSetText(context, textInput)
                            onRefreshLogs()
                            if (success) Toast.makeText(context, "Text set via JClip.java!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("jclip_set_text_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.DataObject, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("JClip.java (Java)")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val text = PapirusClipboardEngine.clipGetText(context)
                        onRefreshLogs()
                        if (text != null) {
                            retrievedText = text
                        } else {
                            Toast.makeText(context, "Clipboard empty!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("clip_get_text_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Rounded.ContentPasteGo, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Read & Paste Text")
                }

                if (retrievedText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Retrieved Text Content:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(retrievedText, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Card 2: Image Transfer (Clip.java & JClip.java)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Image Clipboard Transfer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    generatedBitmap?.let { bmp ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text("Test Asset:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Test Bitmap",
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(2.0f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                generatedBitmap?.let {
                                    val success = PapirusClipboardEngine.clipSetImage(context, it)
                                    onRefreshLogs()
                                    if (success) Toast.makeText(context, "Image set via Clip.java!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Image (Clip.java)")
                        }

                        Button(
                            onClick = {
                                val bmp = PapirusClipboardEngine.clipGetImage(context)
                                onRefreshLogs()
                                if (bmp != null) {
                                    retrievedBitmap = bmp
                                } else {
                                    Toast.makeText(context, "No image found in clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Rounded.FileDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paste Image")
                        }
                    }
                }

                retrievedBitmap?.let { bmp ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Pasted Image from Clipboard:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Pasted image result",
                                modifier = Modifier
                                    .size(100.dp)
                                    .border(1.dp, MaterialTheme.colorScheme.outline)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Size: ${bmp.width}x${bmp.height}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Card 3: 2D Array Serialized Transfer (JClip.java)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2D Object Array Transfer (JClip.java)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rich arrays (like spreadsheet cells or SQL result tables) can be packed into serializable array flavors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display source array grid
                Text("Source Dataset (to copy):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    sourceArray.forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { cell ->
                                Text(
                                    text = cell.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val success = PapirusClipboardEngine.jClipSetArray(context, sourceArray)
                            onRefreshLogs()
                            if (success) Toast.makeText(context, "2D Array copied successfully!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Array")
                    }

                    Button(
                        onClick = {
                            val array = PapirusClipboardEngine.jClipGetArray(context)
                            onRefreshLogs()
                            if (array != null) {
                                retrievedArray = array
                            } else {
                                Toast.makeText(context, "No array found on clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.ContentPaste, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Array")
                    }
                }

                retrievedArray?.let { array ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Retrieved 2D Array Content:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            array.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell.toString(),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
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
}

// --- 2. DOCUMENT SCENARIOS TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentScenariosTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var writerStory by remember { mutableStateOf("The young developer sat under the banyan tree. The compilation succeeded after several hours. The system clipboard listener triggered immediately. Using the clipboard simplifies data exchange between office modules.") }
    var selectedSentenceIndex by remember { mutableStateOf(2) }
    var writerPastedSentence by remember { mutableStateOf("") }

    var calcSourceGrid by remember {
        mutableStateOf(
            arrayOf(
                arrayOf<Any>("Grade", "Class", "Min", "Max"),
                arrayOf<Any>("Grade A", "Inky", 85.0, 100.0),
                arrayOf<Any>("Grade B", "Cellina", 70.0, 84.9)
            )
        )
    }
    var calcPasteGrid by remember { mutableStateOf<Array<Array<Any>>?>(null) }

    var selectedSlideName by remember { mutableStateOf("03_DrawingMode_Canvas") }
    var slidePastedSource by remember { mutableStateOf("") }

    var targetBaseTable by remember { mutableStateOf("Course") }
    var queryResultsArray by remember { mutableStateOf<Array<Array<Any>>?>(null) }

    var slideDummyBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        val width = 150
        val height = 100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint()
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = android.graphics.Color.YELLOW
        canvas.drawCircle(75f, 50f, 30f, paint)
        slideDummyBitmap = bmp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SCENARIO 1: WRITER SENTENCE TRAVERSAL
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scenario 1: Writer Sentence Copier", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Simulates selecting text in storyStart.doc using XSentenceCursor and XTextViewCursor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Text("Document Content:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(writerStory, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Sentence Index:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0, 1, 2, 3).forEach { index ->
                            FilterChip(
                                selected = selectedSentenceIndex == index,
                                onClick = { selectedSentenceIndex = index },
                                label = { Text(index.toString()) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val sentence = PapirusClipboardEngine.simulateWriterCopy(context, writerStory, selectedSentenceIndex)
                            onRefreshLogs()
                            if (sentence != null) {
                                Toast.makeText(context, "Sentence Copied!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Icon(Icons.Rounded.CallSplit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Sentence")
                    }

                    Button(
                        onClick = {
                            val clipboardText = PapirusClipboardEngine.clipGetText(context)
                            onRefreshLogs()
                            if (clipboardText != null) {
                                writerPastedSentence = clipboardText
                            } else {
                                Toast.makeText(context, "Clipboard doesn't contain text!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.0f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Sentence")
                    }
                }

                if (writerPastedSentence.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "Writer Paste Buffer:\n\"$writerPastedSentence\"",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // SCENARIO 2: CALC CELL RANGE COPYING
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.GridOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scenario 2: Calc Cell Range (Addresses.ods)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Simulates selecting cell ranges, exporting to ODS metadata, and loading 2D ranges.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val success = PapirusClipboardEngine.simulateCalcCopy(context, calcSourceGrid)
                            onRefreshLogs()
                            if (success) Toast.makeText(context, "Calc Cell Range Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Icon(Icons.Rounded.CopyAll, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy Cell Range")
                    }

                    Button(
                        onClick = {
                            val array = PapirusClipboardEngine.jClipGetArray(context)
                            onRefreshLogs()
                            if (array != null) {
                                calcPasteGrid = array
                            } else {
                                Toast.makeText(context, "No 2D range on clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.0f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.DriveFileMove, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Paste Range")
                    }
                }

                calcPasteGrid?.let { grid ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Calc Paste Target Range:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            grid.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    row.forEach { cell ->
                                        Text(cell.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SCENARIO 3: IMPRESS SLIDE COPIER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CoPresent, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scenario 3: Impress Slide Sorter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Simulates switching View mode to DiaMode, capturing active slides, and copying ODP.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Active Slide:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("01_Introduction", "02_Architecture", "03_DrawingMode").forEach { name ->
                            FilterChip(
                                selected = selectedSlideName == name,
                                onClick = { selectedSlideName = name },
                                label = { Text(name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        slideDummyBitmap?.let { bmp ->
                            val success = PapirusClipboardEngine.simulateImpressCopy(context, selectedSlideName, bmp)
                            onRefreshLogs()
                            if (success) {
                                Toast.makeText(context, "Slide copied successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FileCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simulate Impress slide copy & save")
                }
            }
        }

        // SCENARIO 4: BASE SQL RESULT SET
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Dataset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scenario 4: Base SQL Resultset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Simulates running SQL statements, packing forward-only result set records as rich 2D array.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Source Database Table:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Course", "Enrollment", "Student").forEach { table ->
                            FilterChip(
                                selected = targetBaseTable == table,
                                onClick = { targetBaseTable = table },
                                label = { Text(table) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val dummyResults = when (targetBaseTable) {
                                "Course" -> arrayOf(
                                    arrayOf<Any>("courseId", "subjectId", "courseNumber", "title", "numOfCredits"),
                                    arrayOf<Any>(11111, "CSCI", 1301, "Intro to Java I", 4),
                                    arrayOf<Any>(11112, "CSCI", 1302, "Intro to Java II", 3)
                                )
                                "Enrollment" -> arrayOf(
                                    arrayOf<Any>("studentId", "courseId", "enrollDate", "grade"),
                                    arrayOf<Any>(101, 11111, "2026-01-15", "A"),
                                    arrayOf<Any>(102, 11111, "2026-01-16", "B")
                                )
                                else -> arrayOf(
                                    arrayOf<Any>("studentId", "firstName", "lastName", "major"),
                                    arrayOf<Any>(101, "Andreas", "Maker", "Computer Science"),
                                    arrayOf<Any>(102, "Elena", "Rostova", "Mathematics")
                                )
                            }
                            val success = PapirusClipboardEngine.simulateBaseCopy(context, targetBaseTable, dummyResults)
                            onRefreshLogs()
                            if (success) Toast.makeText(context, "Base Table Copied!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1.0f)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate SQL Query & Copy")
                    }

                    Button(
                        onClick = {
                            val array = PapirusClipboardEngine.jClipGetArray(context)
                            onRefreshLogs()
                            if (array != null) {
                                queryResultsArray = array
                            } else {
                                Toast.makeText(context, "No result set array on clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1.0f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Load Array")
                    }
                }

                queryResultsArray?.let { array ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Database ResultSet Dump:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            array.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    row.forEach { cell ->
                                        Text(cell.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- 3. SDK EXAMPLES TAB ---
@Composable
private fun SdkExamplesTab(
    sdkExamples: List<Pair<String, String>>,
    selectedExampleFile: String,
    onSelectedExampleFileChange: (String) -> Unit,
    sdkCodeContent: String,
    onCopyClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Choose Java Clipboard SDK Example:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ScrollableTabRow(
            selectedTabIndex = sdkExamples.indexOfFirst { it.first == selectedExampleFile },
            edgePadding = 0.dp
        ) {
            sdkExamples.forEach { (fileName, _) ->
                Tab(
                    selected = selectedExampleFile == fileName,
                    onClick = { onSelectedExampleFileChange(fileName) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = fileName, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val currentDesc = sdkExamples.firstOrNull { it.first == selectedExampleFile }?.second ?: ""
        Text(
            text = currentDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Code Box Container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedExampleFile,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Copy code to clipboard",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = sdkCodeContent,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// --- 4. HELP & OPTIMIZATION TAB ---
@Composable
private fun HelpAndOptimizationTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Clipboard API Services & Architecture",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SystemClipboard (com.sun.star.datatransfer.clipboard)\n" +
                            "  ├── XSystemClipboard\n" +
                            "  │     └── [addClipboardListener / removeClipboardListener]\n" +
                            "  ├── XClipboard\n" +
                            "  │     ├── getName()\n" +
                            "  │     ├── getContents() -> returns XTransferable\n" +
                            "  │     └── setContents(contents, owner)\n" +
                            "  └── XClipboardOwner [lostOwnership]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = "Active Windows & Lo.wait() vs Programmatic View Cursors",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Using Lo.dispatchCmd(\"Copy\") relies entirely on bringing the correct OS application window to the front and executing synchronous copy routines. This creates major visual delays, and user actions (such as mouse clicks) during the tiny delay can break the selection.\n" +
                    "\n" +
                    "By adopting programmatic view cursors (like XTextViewCursor and XCellRangeData.getDataArray()), Papirus Office interacts directly with internal memory structures, ensuring 100% data fidelity without locking user interface threads or requiring arbitrary Lo.wait() periods.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Optimization for Samsung Galaxy A11 & Realme C3",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )

        Text(
            text = "• Heavy Binary Compression: Low-end devices with eMMC storage (Galaxy A11, Realme C3) face immediate RAM bottlenecks when translating large graphic blocks (like Impress slides or extensive spreadsheet cell frames) into Base64 format for system clip exchange.\n" +
                    "• Coroutine Safeguards: The Papirus Clipboard Engine executes binary serialization, image scaling, and CSV text formatting inside Dispatchers.IO background coroutines, completely mitigating main UI thread lags.\n" +
                    "• Native Syncing limits: Standard text and lightweight mime shapes sync to the Android clipboard instantly, while large 2D rich Object tables are cached in the local UNO memory context.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- SHARED TERMINAL LOGS COMPONENT ---
@Composable
private fun TerminalLogs(
    liveLogs: List<String>,
    onClearLogs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Terminal Diagnostic Trace (LO Ch.43)",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Green,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("clear_logs_button")
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Clear clipboard logs", tint = Color.Green)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    if (liveLogs.isEmpty()) {
                        Text(
                            text = "No diagnostic events registered.",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color.LightGray
                        )
                    } else {
                        liveLogs.forEach { log ->
                            Text(
                                text = log,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }
        }
    }
}
