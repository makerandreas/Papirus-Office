package com.example.ui.components

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
import com.makerandreas.papirusoffice.data.framework.PapirusOdfEngine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalOdfSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Metadata, 1 = ZIP Unpack, 2 = Doc Creation, 3 = Concatenate, 4 = Java SDK Code, 5 = Help & Devices

    // SDK Code Examples list
    val sdkExamples = remember {
        listOf(
            "DocInfo.java" to "Inspect ODF metadata properties and custom XPropertyContainer entries.",
            "DocUnzip.java" to "Query zipped zip files, MIMETYPE entries, and extract individual XML data stream.",
            "MakeTextDoc.java" to "Generate simple ODT documents with structured lists and nested table grids.",
            "MakeSheet.java" to "Generate simple ODS sheets, populating formula values and cell numbers.",
            "MakeSlides.java" to "Generate ODP slides featuring layouts, outlines, and anchored graphical assets.",
            "MoveSlide.java" to "Rearrange slide layouts by indexing specific presentation indices.",
            "CombineTexts.java" to "Append ODT text documents with page breaks and automatic styles copying.",
            "CombineSheets.java" to "Append ODS sheets one-by-one inside a unified workbook.",
            "CombineDecks.java" to "Concatenate presentations slide-by-slide cleanly."
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
        liveLogs = PapirusOdfEngine.getLogs()
    }

    LaunchedEffect(Unit) {
        PapirusOdfEngine.clearLogs()
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
                .testTag("odf_framework_dialog"),
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
                                    imageVector = Icons.Rounded.Code,
                                    contentDescription = "ODF Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Simple ODF & Package Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LibreOffice SDK Ch.51 & OpenDocument Packaging / Properties",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_odf_sheet_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close ODF dialog")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Metadata", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("ZIP Unpack", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.FolderZip, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Doc Creation", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.CreateNewFolder, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Concatenate", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.LibraryBooks, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Java SDK Code", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 5,
                        onClick = { selectedTab = 5 },
                        text = { Text("Help & Hardware", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.QuestionMark, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Scrollable Content
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> MetadataTab(onRefreshLogs = refreshLogs)
                            1 -> ZipUnpackTab(onRefreshLogs = refreshLogs)
                            2 -> DocCreationTab(onRefreshLogs = refreshLogs)
                            3 -> ConcatenateTab(onRefreshLogs = refreshLogs)
                            4 -> SdkExamplesTab(
                                sdkExamples = sdkExamples,
                                selectedExampleFile = selectedExampleFile,
                                onSelectedExampleFileChange = { selectedExampleFile = it },
                                sdkCodeContent = sdkCodeContent,
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(sdkCodeContent))
                                    Toast.makeText(context, "SDK ODF Java code copied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            5 -> HelpAndOptimizationTab()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time Console Log Buffer
                    TerminalLogs(liveLogs = liveLogs, onClearLogs = {
                        PapirusOdfEngine.clearLogs()
                        refreshLogs()
                    })
                }
            }
        }
    }
}

// --- TAB 0: METADATA & PROPERTIES ---
@Composable
private fun MetadataTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var props by remember { mutableStateOf(PapirusOdfEngine.getDocProperties()) }

    var titleField by remember { mutableStateOf(props.title) }
    var authorField by remember { mutableStateOf(props.author) }
    var subjectField by remember { mutableStateOf(props.subject) }
    var descField by remember { mutableStateOf(props.description) }
    var secretField by remember { mutableStateOf(props.secretValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. Document Metadata (XDocumentProperties / DocInfo.java)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ODF properties maps content directly to 'meta.xml'. Edit values below to demonstrate properties saving:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleField,
                    onValueChange = { titleField = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = authorField,
                    onValueChange = { authorField = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subjectField,
                    onValueChange = { subjectField = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descField,
                    onValueChange = { descField = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = secretField,
                    onValueChange = { secretField = it },
                    label = { Text("Custom User-Defined Property (XPropertyContainer)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            props = PapirusOdfEngine.getDocProperties()
                            titleField = props.title
                            authorField = props.author
                            subjectField = props.subject
                            descField = props.description
                            secretField = props.secretValue
                            onRefreshLogs()
                            Toast.makeText(context, "Queried latest document properties!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Query Properties")
                    }

                    Button(
                        onClick = {
                            val updated = PapirusOdfEngine.OdfProperties(
                                title = titleField,
                                author = authorField,
                                subject = subjectField,
                                description = descField,
                                generator = props.generator,
                                modificationDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                                secretValue = secretField
                            )
                            PapirusOdfEngine.updateDocProperties(updated)
                            props = updated
                            onRefreshLogs()
                            Toast.makeText(context, "Saved metadata properties into meta.xml!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Properties")
                    }
                }
            }
        }
    }
}

// --- TAB 1: ZIP ARCHIVE & EXTRACTION ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ZipUnpackTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var selectedDoc by remember { mutableStateOf("algs.odp") }
    var entriesList by remember { mutableStateOf<List<PapirusOdfEngine.ZipEntryInfo>>(emptyList()) }
    var selectedEntryToExtract by remember { mutableStateOf("content.xml") }

    LaunchedEffect(selectedDoc) {
        entriesList = PapirusOdfEngine.getOdfZipContents(selectedDoc)
        onRefreshLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. OpenDocument Package Structure (DocUnzip.java)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ODF documents are standardized zipped archives containing core XML elements. Select a document template to view its internal package map:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("algs.odp", "makeSheet.ods", "MakeTextDoc.odt").forEach { docName ->
                        FilterChip(
                            selected = selectedDoc == docName,
                            onClick = { selectedDoc = docName },
                            label = { Text(docName) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Zipped Container Contents List:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Zip Name", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f))
                                Text("Raw Size", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text("Compressed", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            }
                        }
                        items(entriesList) { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(entry.name, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1.8f), maxLines = 1)
                                Text("${entry.rawSize} B", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                                Text("${entry.compressedSize} B", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Extraction simulations
                Text("Simulate Single File Extraction (SimpleFileAccess):", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("mimetype", "content.xml", "styles.xml", "meta.xml").forEach { entryName ->
                        FilterChip(
                            selected = selectedEntryToExtract == entryName,
                            onClick = { selectedEntryToExtract = entryName },
                            label = { Text(entryName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val outputName = PapirusOdfEngine.simulateUnzipFile(selectedDoc, selectedEntryToExtract)
                        onRefreshLogs()
                        Toast.makeText(context, "Unzipped & Extracted entry saved as: $outputName", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Rounded.Unarchive, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Extract Entry \"$selectedEntryToExtract\"")
                }
            }
        }
    }
}

// --- TAB 2: DOCUMENT CREATION ---
@Composable
private fun DocCreationTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var makeDocType by remember { mutableStateOf(0) } // 0 = Text Doc, 1 = Spreadsheet, 2 = Slides

    // Form inputs
    var textTitle by remember { mutableStateOf("Hello World, Hello Simple ODF!") }
    var includeTextLogo by remember { mutableStateOf(true) }
    var textListItemsString by remember { mutableStateOf("item1, item2, item3") }

    var startNumValue by remember { mutableStateOf("2.0") }
    var rowMathMultiplier by remember { mutableStateOf("2.0") }

    var presentationTitle by remember { mutableStateOf("Important Slide Presentation") }
    var presentationBulletsString by remember { mutableStateOf("Item 1, Item 2") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "3. Create ODF Documents via Simple ODF API",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Built on top of ODFDOM, the ODF Toolkit Simple API allows creation and population of documents without running LibreOffice.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector inside
                TabRow(selectedTabIndex = makeDocType, modifier = Modifier.fillMaxWidth()) {
                    Tab(selected = makeDocType == 0, onClick = { makeDocType = 0 }, text = { Text("Text (.odt)") })
                    Tab(selected = makeDocType == 1, onClick = { makeDocType = 1 }, text = { Text("Sheet (.ods)") })
                    Tab(selected = makeDocType == 2, onClick = { makeDocType = 2 }, text = { Text("Slides (.odp)") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (makeDocType) {
                    0 -> {
                        Text("Configure MakeTextDoc.odt Parameters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textTitle,
                            onValueChange = { textTitle = it },
                            label = { Text("Paragraph Text") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textListItemsString,
                            onValueChange = { textListItemsString = it },
                            label = { Text("List Items (comma-separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = includeTextLogo, onCheckedChange = { includeTextLogo = it })
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Include 'odf-logo.png' brand image asset", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val list = textListItemsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val result = PapirusOdfEngine.simulateMakeTextDoc(textTitle, includeTextLogo, list)
                                onRefreshLogs()
                                Toast.makeText(context, "Generated and saved $result!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Text Document")
                        }
                    }

                    1 -> {
                        Text("Configure MakeSheet.ods Parameters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = startNumValue,
                            onValueChange = { startNumValue = it },
                            label = { Text("Base Number Start Value") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = rowMathMultiplier,
                            onValueChange = { rowMathMultiplier = it },
                            label = { Text("Row Math Multiplier (row * val)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val start = startNumValue.toDoubleOrNull() ?: 0.0
                                val mult = rowMathMultiplier.toDoubleOrNull() ?: 2.0
                                val result = PapirusOdfEngine.simulateMakeSheet(start, mult)
                                onRefreshLogs()
                                Toast.makeText(context, "Generated and saved $result!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Spreadsheet")
                        }
                    }

                    2 -> {
                        Text("Configure MakeSlides.odp Parameters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = presentationTitle,
                            onValueChange = { presentationTitle = it },
                            label = { Text("Main Slide 1 Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = presentationBulletsString,
                            onValueChange = { presentationBulletsString = it },
                            label = { Text("Slide 2 Outline bullets (comma-separated)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val bullets = presentationBulletsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                val result = PapirusOdfEngine.simulateMakeSlides(presentationTitle, bullets)
                                onRefreshLogs()
                                Toast.makeText(context, "Generated presentation $result!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Rounded.Slideshow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate Slide Deck")
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 3: CONCATENATE & REARRANGE ---
@Composable
private fun ConcatenateTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "4. Compound Concatenation & Relocation APIs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simple API excels at merging documents and shuffling layout pages natively, simplifying complex Office Dispatch commands:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: Slide movement
                Text("Rearrange Slide Indexes (MoveSlide.java)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val result = PapirusOdfEngine.simulateSlideRearrange()
                        onRefreshLogs()
                        Toast.makeText(context, "Successfully shuffeled slides in $result!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.CompareArrows, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run Slide Relocation (0 -> END)")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action 2: Text Combine
                Text("Concatenate Text Documents (CombineTexts.java)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val result = PapirusOdfEngine.simulateCombineTexts()
                        onRefreshLogs()
                        Toast.makeText(context, "Saved merged ODT: $result", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Rounded.MergeType, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Merge doc2.odt into doc1.odt")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action 3: Spreadsheet Combine
                Text("Concatenate Sheet Workbooks (CombineSheets.java)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val result = PapirusOdfEngine.simulateCombineSheets()
                        onRefreshLogs()
                        Toast.makeText(context, "Saved combined workbook: $result", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Rounded.AddBox, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Append ss2.ods sheet cells into ss1.ods")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action 4: Slides combine
                Text("Concatenate Presentation Decks (CombineDecks.java)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        val result = PapirusOdfEngine.simulateCombineDecks()
                        onRefreshLogs()
                        Toast.makeText(context, "Saved combined presentations: $result", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.AddToPhotos, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Append deck2.odp into deck1.odp")
                }
            }
        }
    }
}

// --- TAB 4: JAVA SDK CODE ---
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
            text = "Choose Java Simple ODF SDK Example:",
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
                        Text(text = fileName, fontWeight = FontWeight.Bold)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val currentDesc = sdkExamples.firstOrNull { it.first == selectedExampleFile }?.second ?: ""
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(currentDesc, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = sdkCodeContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    FloatingActionButton(
                        onClick = onCopyClick,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .testTag("copy_sdk_odf_example_button"),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Example Code")
                    }
                }
            }
        }
    }
}

// --- TAB 5: HELP & OPTIMIZATIONS FOR LOW-END DEVICES ---
@Composable
private fun HelpAndOptimizationTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚙️ Hardware Optimizations: Realme C3 & Galaxy A11",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Both Realme C3 (Android 10, 3GB RAM, MediaTek Helio G70) and Samsung Galaxy A11 (Android 12, 3GB RAM, Snapdragon 450) are memory-constrained low-end targets. Compressing, unzipping, and processing large ODF files can trigger fatal out-of-memory (OOM) exceptions and UI stutters. Observe these strict tuning mechanisms:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "1. Avoid Memory-Mapped Stream Arrays",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Do not read unzipped raw XML files (e.g. content.xml) fully into raw heap String variables. A 10MB document content text expands up to 40MB in UTF-16 heap variables, causing GC thrashing. Use streaming readers with 8KB buffer blocks to process lines incrementally.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "2. Streaming Unzip Cleanup (Analogy)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Analogy: \"Think of unzip operations like unpacking a suitcase in a small hotel room — only take out one item at a time instead of dumping everything on the floor!\" Always close ZipFile instances inside 'try-with-resources' statements immediately after reading. Leaving package file handles open causes kernel-level leaks on eMMC storage engines.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "3. Cache Garbage Collection Thrashing prevention",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simple API simulations and document creation (e.g. MakeSlides) allocate temporary layout bounds structures frequently. Run allocations within localized garbage disposal scopes, and defer complex slide drawing when the screen keyboard is shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- TERMINAL LOG BUFFER COMPONENT ---
@Composable
private fun TerminalLogs(
    liveLogs: List<String>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        // Log Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Terminal,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ODF Package Console Logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = onClearLogs,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = "Clear logs",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Logs Scrollable container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF181818))
                .padding(8.dp)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(liveLogs.size) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (liveLogs.isEmpty()) {
                    Text(
                        text = "Console quiet. Awaiting ODF Simple API tasks...",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                } else {
                    liveLogs.forEach { log ->
                        Text(
                            text = log,
                            color = when {
                                log.contains("[SimpleAPI]") -> Color(0xFF90CAF9)
                                log.contains("[XDocumentProperties]") -> Color(0xFFA5D6A7)
                                log.contains("[SimpleFileAccess]") -> Color(0xFFFFE082)
                                else -> Color(0xFFE0E0E0)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
