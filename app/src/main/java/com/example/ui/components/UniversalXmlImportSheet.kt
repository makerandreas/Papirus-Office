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
import com.makerandreas.papirusoffice.data.framework.PapirusXmlEngine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalXmlImportSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Filters & CLI, 1 = DOM & Tokenizer, 2 = JAXB, 3 = Java Examples, 4 = Help & Optimizations

    // SDK Code Examples list
    val sdkExamples = remember {
        listOf(
            "FiltersInfo.java" to "Inspect FilterFactory properties, flags, user data, and templates.",
            "ApplyInFilter.java" to "Convert raw XML to Flat ODF XML using JAXP XSLT and open in Office.",
            "ApplyOutFilter.java" to "Export active ODF document as Flat XML and transform back to custom XML.",
            "ExamineCompany.java" to "Load company.xml and extract nested executive details via DOM tree parsing.",
            "CreatePay.java" to "Retrieve payment records via DOM and load into spreadsheet cells dynamically.",
            "CreateAssoc.java" to "Parse structured club-database XML files into modular sheets.",
            "ExtractXMLInfo.java" to "Parse XML node and attribute data into quote-delimited labeled string layout.",
            "BuildXMLSheet.java" to "Tokenize labeled layouts, mapping double spaces to spreadsheet cells.",
            "UnmarshallPay.java" to "Perform unmarshalling of payment XML into typed Java entity lists.",
            "UnmarshallClubs.java" to "JAXB unmarshal of nested relational databases to lists.",
            "UnmarshallWeather.java" to "Demonstrate customized XSD annotations to resolve XML attribute name conflicts."
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
        liveLogs = PapirusXmlEngine.getLogs()
    }

    LaunchedEffect(Unit) {
        PapirusXmlEngine.clearLogs()
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
                .testTag("xml_import_framework_dialog"),
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
                                    contentDescription = "XML Import Icon",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Papirus XML Importing Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LibreOffice SDK Ch.50 & Flat XML / DOM / JAXB Integrations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("close_xml_import_button")
                    ) {
                        Icon(imageVector = Icons.Rounded.Close, contentDescription = "Close XML import dialog")
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
                        text = { Text("Filters & CLI", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Terminal, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("DOM & Labeled Parsing", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Schema, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("JAXB Objects", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.DataObject, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Java SDK Code", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text("Help & Hardware", maxLines = 1) },
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Scrollable Content
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            0 -> FiltersAndCliTab(onRefreshLogs = refreshLogs)
                            1 -> DomAndLabeledTab(onRefreshLogs = refreshLogs)
                            2 -> JaxbTab(onRefreshLogs = refreshLogs)
                            3 -> SdkExamplesTab(
                                sdkExamples = sdkExamples,
                                selectedExampleFile = selectedExampleFile,
                                onSelectedExampleFileChange = { selectedExampleFile = it },
                                sdkCodeContent = sdkCodeContent,
                                onCopyClick = {
                                    clipboardManager.setText(AnnotatedString(sdkCodeContent))
                                    Toast.makeText(context, "SDK Java code copied!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            4 -> HelpAndOptimizationTab()
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real-time Console Log Buffer
                    TerminalLogs(liveLogs = liveLogs, onClearLogs = {
                        PapirusXmlEngine.clearLogs()
                        refreshLogs()
                    })
                }
            }
        }
    }
}

// --- TAB 1: FILTERS & CLI ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersAndCliTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var selectedFilterQuery by remember { mutableStateOf("Pay") }
    var filterPropsResult by remember { mutableStateOf<PapirusXmlEngine.FilterProps?>(null) }

    var cliFilename by remember { mutableStateOf("pay.xml") }
    var cliFilterName by remember { mutableStateOf("Pay") }
    var cliOutputFormat by remember { mutableStateOf("xml:Pay") }
    var convertedOutput by remember { mutableStateOf("") }

    LaunchedEffect(selectedFilterQuery) {
        filterPropsResult = PapirusXmlEngine.getFilterProperties(selectedFilterQuery)
        onRefreshLogs()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card 1: FilterFactory Properties
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "1. FilterFactory Property Query (Info.java)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Query the registered UNO service FilterFactory to look up XML import/export filter definitions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select Filter:", style = MaterialTheme.typography.bodySmall)
                    listOf("Pay", "Clubs", "AbiWord").forEach { fName ->
                        FilterChip(
                            selected = selectedFilterQuery == fName,
                            onClick = { selectedFilterQuery = fName },
                            label = { Text(fName) }
                        )
                    }
                }

                filterPropsResult?.let { props ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Filter Registration Metadata:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Name: ${props.name}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            Text("UI Name: ${props.uiname}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            Text("Flags: ${props.flags} (0x${Integer.toHexString(props.flags)})", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            Text("Document Service: ${props.documentService.substringAfterLast('.')}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            Text("Filter Service: ${props.filterService.substringAfterLast('.')}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            if (props.templateName.isNotEmpty()) {
                                Text("Associated Template: ${props.templateName.substringAfterLast('/')}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            }
                            if (props.userData.isNotEmpty()) {
                                Text("UserData Components:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                                props.userData.forEachIndexed { i, d ->
                                    Text("  [$i] $d", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Card 2: CLI infilter and convert
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "2. Command-Line Importing (infilter & convert)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Execute LibreOffice CLI commands with infilter or convert flags directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                // CLI 1: infilter
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Scenario A: infilter.bat (Import)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cliFilename,
                            onValueChange = { cliFilename = it },
                            label = { Text("Filename") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cliFilterName,
                            onValueChange = { cliFilterName = it },
                            label = { Text("Filter") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val success = PapirusXmlEngine.simulateInfilter(cliFilename, cliFilterName)
                            onRefreshLogs()
                            if (success) {
                                Toast.makeText(context, "Command Executed: Document opened!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Execution failed.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run: infilter $cliFilename \"$cliFilterName\"")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // CLI 2: convert
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text("Scenario B: convert.bat (Export to Custom XML)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        var exportSource by remember { mutableStateOf("payment.ods") }
                        OutlinedTextField(
                            value = exportSource,
                            onValueChange = { exportSource = it },
                            label = { Text("Source Document") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cliOutputFormat,
                            onValueChange = { cliOutputFormat = it },
                            label = { Text("Format/Filter") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val output = PapirusXmlEngine.simulateConvert("payment.ods", cliOutputFormat)
                            onRefreshLogs()
                            if (output != null) {
                                convertedOutput = output
                                Toast.makeText(context, "Export output saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Unsupported export.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.Output, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Run: convert payment.ods \"$cliOutputFormat\"")
                    }

                    if (convertedOutput.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Exported XML Output:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState()),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(convertedOutput, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: DOM & LABELED PARSING ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DomAndLabeledTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var xmlChoice by remember { mutableStateOf("pay.xml") }
    var xmlContent by remember { mutableStateOf(PapirusXmlEngine.PAY_XML) }
    var domResult by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var mapped2DResult by remember { mutableStateOf<Array<Array<Any>>?>(null) }
    var labeledStringOutput by remember { mutableStateOf("") }
    var alignedSpreadsheetTable by remember { mutableStateOf<Array<Array<Any>>?>(null) }

    LaunchedEffect(xmlChoice) {
        xmlContent = when (xmlChoice) {
            "pay.xml" -> PapirusXmlEngine.PAY_XML
            "company.xml" -> PapirusXmlEngine.COMPANY_XML
            "weather.xml" -> PapirusXmlEngine.WEATHER_XML
            "clubs.xml" -> PapirusXmlEngine.CLUBS_XML
            else -> ""
        }
        domResult = emptyList()
        mapped2DResult = null
        labeledStringOutput = ""
        alignedSpreadsheetTable = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(end = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // XML Source Code Viewer
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Source XML Input:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row {
                        listOf("pay.xml", "company.xml", "weather.xml", "clubs.xml").forEach { choice ->
                            FilterChip(
                                selected = xmlChoice == choice,
                                onClick = { xmlChoice = choice },
                                label = { Text(choice, fontSize = 11.sp) },
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = xmlContent,
                    onValueChange = { xmlContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("xml_input_editor"),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    placeholder = { Text("Paste custom XML here...") }
                )
            }
        }

        // Section A: DOM Parsing
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A. Data Extraction by DOM Tree (ExamineCompany / CreatePay)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Standard DOM parses nodes recursively, extracting nested text elements and attributes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (xmlChoice == "company.xml") {
                                domResult = PapirusXmlEngine.parseCompaniesDom()
                                mapped2DResult = null
                                onRefreshLogs()
                                Toast.makeText(context, "DOM parsed company structures!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please select 'company.xml' first for executive structure.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.AccountTree, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Parse Executive DOM")
                    }

                    Button(
                        onClick = {
                            if (xmlChoice == "pay.xml") {
                                mapped2DResult = PapirusXmlEngine.parsePaymentsTo2D()
                                domResult = emptyList()
                                onRefreshLogs()
                                Toast.makeText(context, "Mapped payment rows to grid!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please select 'pay.xml' first for tabular payments data.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.GridOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Map 2D Spreadsheet")
                    }
                }

                if (domResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("DOM Hierarchy Executive Objects:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        domResult.forEach { item ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Company: ${item["company"]}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text("Executive: ${item["firstName"]} ${item["lastName"]} (${item["execType"]})", style = MaterialTheme.typography.bodyMedium)
                                    Text("Address: ${item["street"]}, ${item["city"]}, ${item["state"]} ${item["zip"]}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                mapped2DResult?.let { grid ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Aligned Spreadsheet Mapping:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            grid.forEach { row ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    row.forEach { cell ->
                                        Text(cell.toString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section B: Labeled Strings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "B. Labeled Indented Extractor (ExtractXMLInfo / BuildXMLSheet)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Strips tags from complex weather/unstructured XML, representing elements with ':' and attributes with '='.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            labeledStringOutput = PapirusXmlEngine.extractXmlAsLabeledStrings(xmlContent)
                            onRefreshLogs()
                            Toast.makeText(context, "XML stripped to labeled lines!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.FormatIndentIncrease, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Extract Labeled Strings")
                    }

                    Button(
                        onClick = {
                            if (labeledStringOutput.isNotEmpty()) {
                                alignedSpreadsheetTable = PapirusXmlEngine.tokenizeLabeledStringToTable(labeledStringOutput)
                                onRefreshLogs()
                                Toast.makeText(context, "Tokenized into aligned spreadsheet cells!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please extract labeled strings first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tokenize to Cells")
                    }
                }

                if (labeledStringOutput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Indented Labeled Text Representation:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(rememberScrollState()),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = labeledStringOutput,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                alignedSpreadsheetTable?.let { grid ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("BuildXMLSheet Multi-column Tabular Grid:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            grid.forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    row.forEach { cell ->
                                        Box(modifier = Modifier.width(100.dp).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)).padding(4.dp)) {
                                            Text(
                                                text = if (cell.toString().isEmpty()) "—" else cell.toString(),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (cell.toString().isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface
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
}

// --- TAB 3: JAXB UNMARSHELLING ---
@Composable
private fun JaxbTab(onRefreshLogs: () -> Unit) {
    val context = LocalContext.current
    var resolveConflict by remember { mutableStateOf(false) }
    var jaxbScenario by remember { mutableStateOf("Pay") }

    var paymentObjects by remember { mutableStateOf<List<PapirusXmlEngine.PaymentJaxb>>(emptyList()) }
    var associationsList by remember { mutableStateOf<List<PapirusXmlEngine.AssociationJaxb>>(emptyList()) }
    var weatherConflictResolved by remember { mutableStateOf<Boolean?>(null) }

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
                    text = "3. JAXB Object Unmarshalling Contexts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Java Architecture for XML Binding converts nested elements into strongly-typed POJO lists via JAXBContext.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Unmarshall Class:", style = MaterialTheme.typography.bodySmall)
                    listOf("Pay", "Clubs", "Weather").forEach { scen ->
                        FilterChip(
                            selected = jaxbScenario == scen,
                            onClick = { jaxbScenario = scen },
                            label = { Text(scen) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (jaxbScenario) {
                    "Pay" -> {
                        Button(
                            onClick = {
                                val res = PapirusXmlEngine.simulateUnmarshallPay()
                                paymentObjects = res.payments
                                onRefreshLogs()
                                Toast.makeText(context, "Unmarshalled payments!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unmarshall Payments.class")
                        }

                        if (paymentObjects.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Unmarshalled Typed Objects List:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                paymentObjects.forEach { pay ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(pay.purpose, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Maturity: ${pay.maturity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("$${pay.amount}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                Text("Tax: ${pay.tax}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Clubs" -> {
                        Button(
                            onClick = {
                                val res = PapirusXmlEngine.simulateUnmarshallClubs()
                                associationsList = res.associations
                                onRefreshLogs()
                                Toast.makeText(context, "Unmarshalled associations!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unmarshall ClubDatabase.class")
                        }

                        if (associationsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Unmarshalled Relational Club Database List:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                associationsList.forEach { assoc ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("Association ID: ${assoc.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            assoc.clubs.forEach { club ->
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text("• ${club.name} (Charter: ${club.charter})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                                    Text("  Contact: ${club.contact} | Phone: ${club.phone} | Email: ${club.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "Weather" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text("Handling Schema Name Conflicts (Salami Slice xjc error)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("weather.xml uses attribute name 'value' multiple times in conflicting locations. JAXB compiler xjc will fail unless custom bindings resolve the namespaces.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Checkbox(
                                    checked = resolveConflict,
                                    onCheckedChange = { resolveConflict = it }
                                )
                                Text("Bind <jaxb:property name=\"valueAttribute\"/> in weather.xsd", style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    weatherConflictResolved = PapirusXmlEngine.simulateUnmarshallWeatherWithConflictResolve(resolveConflict)
                                    onRefreshLogs()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Rounded.Construction, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Compile & Unmarshall Weather.class")
                            }

                            weatherConflictResolved?.let { resolved ->
                                Spacer(modifier = Modifier.height(12.dp))
                                if (!resolved) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "xjc COMPILATION ERROR:\nProperty \"Value\" is already defined. Use <jaxb:property> to resolve this conflict inside weather.xsd schema definition.",
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("xjc compilation: SUCCESSFUL", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Unmarshalled current weather of Hat Yai successfully. LastUpdate value parsed into valueAttribute Calendar successfully. It was NOT raining on 02/01/2017.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B5E20))
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
}

// --- TAB 4: SDK JAVA EXAMPLES ---
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
            text = "Choose Java XML Import/Export SDK Example:",
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
                            .testTag("copy_sdk_example_button"),
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

// --- TAB 5: HELP & TARGET DEVICES ---
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
                    text = "Developer Guidance: XML Importing",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Memahami XML Importing seperti menyusun lego. XML adalah kepingan lego, dan XSLT atau DOM adalah instruksi manualnya. Kita mengubah data terstruktur mentah menjadi format Flat ODF XML yang dapat dibaca oleh Calc (scalc) atau Writer (swriter).",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Komponen Utama Pembahasan:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• XSLT Filters: Menggunakan lembar transformasi XSLT 1.0 (melalui libxslt) untuk mengubah XML ke format Flat ODF secara real-time.", style = MaterialTheme.typography.bodySmall)
                Text("• DOM Tree Parsing: Membaca dokumen XML utuh sebagai diagram pohon memori, cocok untuk ekstraksi struktur yang sangat spesifik.", style = MaterialTheme.typography.bodySmall)
                Text("• Labeled Extractor & Tokenizer: Mempermudah penulisan parsing dengan memisahkan data menggunakan simbol ':' dan '=', lalu menyelaraskannya ke kolom spreadsheet berdasarkan identasi spasi ganda.", style = MaterialTheme.typography.bodySmall)
                Text("• JAXB: Secara otomatis membuat kelas objek Java dari skema XML (XSD). Jika ada konflik nama atribut seperti kata 'value', selesaikan dengan membubuhkan anotasi <jaxb:property>.", style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Target Device Optimizations (A11 & Realme C3)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Karena target kita adalah low-end devices dengan RAM terbatas (3 GB), perhatikan optimasi pemrosesan XML berikut:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("1. Batasi Penggunaan DOM untuk File Besar", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("DOM memuat seluruh diagram pohon ke dalam memori RAM. Untuk perangkat low-end, parsing file XML berukuran megabyte dapat menyebabkan OutOfMemory (OOM) crash. Solusinya, gunakan SAX parser atau StAX stream parser.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Gunakan TinyXML Parser untuk C++ / Native Layer", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("Untuk engine native C++, pakai parser ringan seperti TinyXML2 daripada modul parser besar yang menghabiskan CPU Qualcomm SDM450 milik Galaxy A11.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("3. Pre-compile JAXB Contexts", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Text("Pembuatan JAXBContext sangat lambat dan intensif CPU. Pre-compile dan cache instance JAXBContext agar pemrosesan XML berikutnya berjalan instan.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// --- SHARED UI TERMINAL LOGS ---
@Composable
private fun TerminalLogs(
    liveLogs: List<String>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DeveloperMode,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "XML Engine Terminal Console Log",
                    color = Color(0xFFE0E0E0),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onClearLogs,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Clear", color = Color(0xFF9E9E9E), fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(liveLogs.size) {
                if (liveLogs.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (liveLogs.isEmpty()) {
                    Text(
                        "Console idle. Trigger an operation above to output diagnostics...",
                        color = Color(0xFF757575),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                } else {
                    liveLogs.forEach { log ->
                        Text(
                            text = log,
                            color = if (log.contains("ERROR", true) || log.contains("failed", true)) Color(0xFFEF5350) else Color(0xFF81C784),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}
