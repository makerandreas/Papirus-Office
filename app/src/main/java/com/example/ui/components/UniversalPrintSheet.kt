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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makerandreas.papirusoffice.data.framework.*

/**
 * Universal Printing Sheet (LibreOffice SDK Chapter 41 "Printing" & Java Print Service JPS).
 * Provides document print setup, Android native PrintManager spooler execution,
 * and live inspection for the 6 Java SDK printing examples.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalPrintSheet(
    activeModuleName: String = "Inky", // "Inky", "Cellina", "Slidia", "Pagella"
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = Document Print Control, 1 = SDK Java Examples, 2 = Printing Architecture Help

    // Print options state
    var docType by remember { mutableStateOf(activeModuleName) }
    var docTitle by remember { mutableStateOf("$docType Document") }
    var selectedPaperFormat by remember { mutableStateOf(PaperFormat.A4) }
    var selectedOrientation by remember { mutableStateOf(PaperOrientation.PORTRAIT) }
    var copiesCount by remember { mutableStateOf(1) }
    var pageRange by remember { mutableStateOf("1-") }
    var pagesPerSheet by remember { mutableStateOf(1) } // 1, 2, 4, 6
    var isCollated by remember { mutableStateOf(true) }
    var lastJobStatus by remember { mutableStateOf("Ready to print") }

    // SDK Java Examples state
    val sdkExampleFiles = remember {
        listOf(
            "PrintPS.java" to "PostScript stream printing with A4, 2 copies, two-sided duplex & staple finishings.",
            "PrintGIFtoStream.java" to "Exports GIF image stream as PostScript output file via JPS StreamPrintServiceFactory.",
            "Print2DPrinterJob.java" to "Java 2D Printable interface with PrinterJob page setup & print dialogs.",
            "Print2DGraphics.java" to "Service-formatted 2D graphics printing using DocFlavor.SERVICE_FORMATTED.PRINTABLE.",
            "Print2DtoStream.java" to "Renders 2D graphics drawing to PostScript stream output.",
            "PrintGIF.java" to "Direct GIF image document printing using custom InputStreamDoc."
        )
    }
    var selectedExampleFile by remember { mutableStateOf(sdkExampleFiles.first().first) }
    var sdkCodeContent by remember { mutableStateOf("Loading SDK code example...") }

    LaunchedEffect(selectedExampleFile) {
        sdkCodeContent = PapirusPrintingEngine.readSdkExampleContent(context, selectedExampleFile)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp)),
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
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Printing Framework",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Papirus Printing Framework",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LibreOffice SDK Ch.41 & Java Print Service (JPS)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Print Control") },
                        icon = { Icon(Icons.Default.Print, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("SDK Java Examples") },
                        icon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Help & Guide") },
                        icon = { Icon(Icons.Default.HelpOutline, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content
                when (selectedTab) {
                    0 -> DocumentPrintControlTab(
                        docType = docType,
                        onDocTypeChange = { docType = it },
                        docTitle = docTitle,
                        onDocTitleChange = { docTitle = it },
                        selectedPaperFormat = selectedPaperFormat,
                        onPaperFormatChange = { selectedPaperFormat = it },
                        selectedOrientation = selectedOrientation,
                        onOrientationChange = { selectedOrientation = it },
                        copiesCount = copiesCount,
                        onCopiesChange = { copiesCount = it },
                        pageRange = pageRange,
                        onPageRangeChange = { pageRange = it },
                        pagesPerSheet = pagesPerSheet,
                        onPagesPerSheetChange = { pagesPerSheet = it },
                        isCollated = isCollated,
                        onCollatedChange = { isCollated = it },
                        lastJobStatus = lastJobStatus,
                        onPrintClick = {
                            lastJobStatus = "Submitting job to Android PrintManager..."
                            val isLandscape = selectedOrientation == PaperOrientation.LANDSCAPE
                            PapirusPrintingEngine.printSampleDocument(context, docType)
                            lastJobStatus = "Job sent to System Spooler ($docType)"
                            Toast.makeText(context, "Opening Android Native Print Spooler...", Toast.LENGTH_SHORT).show()
                        }
                    )

                    1 -> SdkExamplesTab(
                        sdkExampleFiles = sdkExampleFiles,
                        selectedFile = selectedExampleFile,
                        onSelectFile = { selectedExampleFile = it },
                        codeContent = sdkCodeContent,
                        onCopyCode = {
                            clipboardManager.setText(AnnotatedString(sdkCodeContent))
                            Toast.makeText(context, "SDK Example copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    2 -> PrintingHelpTab()
                }
            }
        }
    }
}

@Composable
private fun DocumentPrintControlTab(
    docType: String,
    onDocTypeChange: (String) -> Unit,
    docTitle: String,
    onDocTitleChange: (String) -> Unit,
    selectedPaperFormat: PaperFormat,
    onPaperFormatChange: (PaperFormat) -> Unit,
    selectedOrientation: PaperOrientation,
    onOrientationChange: (PaperOrientation) -> Unit,
    copiesCount: Int,
    onCopiesChange: (Int) -> Unit,
    pageRange: String,
    onPageRangeChange: (String) -> Unit,
    pagesPerSheet: Int,
    onPagesPerSheetChange: (Int) -> Unit,
    isCollated: Boolean,
    onCollatedChange: (Boolean) -> Unit,
    lastJobStatus: String,
    onPrintClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Module & Document Setup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "Target Module:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        listOf("Inky", "Cellina", "Slidia", "Pagella").forEach { module ->
                            FilterChip(
                                selected = docType.equals(module, ignoreCase = true),
                                onClick = { onDocTypeChange(module) },
                                label = { Text(module) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = onDocTitleChange,
                        label = { Text("Document Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Card {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "PrintDescriptor & Page Setup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Paper Format:")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(PaperFormat.A4, PaperFormat.LETTER, PaperFormat.A3, PaperFormat.LEGAL).forEach { fmt ->
                                FilterChip(
                                    selected = selectedPaperFormat == fmt,
                                    onClick = { onPaperFormatChange(fmt) },
                                    label = { Text(fmt.name) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Orientation:")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = selectedOrientation == PaperOrientation.PORTRAIT,
                                onClick = { onOrientationChange(PaperOrientation.PORTRAIT) },
                                label = { Text("Portrait") }
                            )
                            FilterChip(
                                selected = selectedOrientation == PaperOrientation.LANDSCAPE,
                                onClick = { onOrientationChange(PaperOrientation.LANDSCAPE) },
                                label = { Text("Landscape") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Copies Count:")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (copiesCount > 1) onCopiesChange(copiesCount - 1) }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease")
                            }
                            Text(
                                text = "$copiesCount",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = { onCopiesChange(copiesCount + 1) }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = pageRange,
                        onValueChange = onPageRangeChange,
                        label = { Text("Page Range (e.g. 1-, 1-2, 2-4;6)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Pages / Sheet (Handout Layout):")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(1, 2, 4, 6).forEach { num ->
                                FilterChip(
                                    selected = pagesPerSheet == num,
                                    onClick = { onPagesPerSheetChange(num) },
                                    label = { Text("${num}p") }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Collate Pages:")
                        Switch(checked = isCollated, onCheckedChange = onCollatedChange)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "XPrintJobBroadcaster Status",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = lastJobStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        item {
            Button(
                onClick = onPrintClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Print via Native Android Spooler", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SdkExamplesTab(
    sdkExampleFiles: List<Pair<String, String>>,
    selectedFile: String,
    onSelectFile: (String) -> Unit,
    codeContent: String,
    onCopyCode: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Java Print Service (JPS) SDK Reference Examples (*.java)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // File list chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sdkExampleFiles.forEach { (file, _) ->
                FilterChip(
                    selected = selectedFile == file,
                    onClick = { onSelectFile(file) },
                    label = { Text(file, fontFamily = FontFamily.Monospace) }
                )
            }
        }

        val activeDesc = sdkExampleFiles.find { it.first == selectedFile }?.second ?: ""
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = activeDesc,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "File: $selectedFile",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = onCopyCode) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code")
            }
        }

        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = codeContent,
                    color = Color(0xFFD4D4D4),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PrintingHelpTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Printing Framework Overview (Analogy Sederhana)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Anggap proses mencetak ini seperti mengirim surat via Pos:\n\n" +
                            "1. XPrintable & XPagePrintable: Seperti isi surat dan amplopnya. Menentukan ukuran kertas (A4, Letter), arah cetak (Portrait/Landscape), dan berapa lembar halaman yang dicetak per kertas.\n" +
                            "2. DocFlavor: Seperti label jenis paket (misal: Gambar GIF, Dokumen PDF, atau PostScript).\n" +
                            "3. PrintManager Android: Petugas pos lokal di HP yang meneruskan surat ke printer terhubung via Wi-Fi, USB, atau PDF Spooler.\n" +
                            "4. Fallback Architecture: Jika printer fisik tidak tersedia, Papirus secara otomatis mengubah dokumen menjadi format PDF standar yang siap disimpan atau dibagikan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Key LibreOffice SDK Printing Interfaces",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• XPrintable: Interface utama untuk mencetak dokumen Writer, Calc, Impress, Draw.\n" +
                            "• XPagePrintable: Mengatur tata letak cetak khusus seperti 'PageRows', 'PageColumns', dan margin.\n" +
                            "• XPrintJobBroadcaster: Memberikan status real-time siklus pencetakan (JOB_STARTED, JOB_SPOOLED, JOB_COMPLETED).\n" +
                            "• PrintDescriptor: Menyimpan nama printer, status sibuk (IsBusy), dan orientasi kertas.",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Card {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Low-End Device Performance (Samsung A11 & Realme C3)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "• Pencetakan dilakukan secara asynchronous dengan Android Native PdfDocument canvas untuk menghemat RAM 3GB/eMMC.\n" +
                            "• Spooling halaman dilakukan berurutan tanpa memuat seluruh dokumen ke dalam memori bitmap sekaligus.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
