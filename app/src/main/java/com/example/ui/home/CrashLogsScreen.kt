package com.example.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashLog(
    val id: String,
    val title: String,
    val exceptionType: String,
    val timestamp: String,
    val tag: String, // "Native Core", "JVM UI", "JNI Bridge"
    val severity: String, // "FATAL", "CRITICAL", "WARNING"
    val summary: String,
    val stackTrace: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashLogsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val crashLogFile = remember { File(context.filesDir, "crash.log") }
    val initialLogs = remember { mutableStateListOf<CrashLog>() }

    var pendingSaveText by remember { mutableStateOf<String?>(null) }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            pendingSaveText?.let { textToSave ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(textToSave.toByteArray())
                    }
                    Toast.makeText(context, "Log saved successfully via SAF!", Toast.LENGTH_SHORT).show()
                } catch (e: java.lang.Exception) {
                    Toast.makeText(context, "Failed to save: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
        pendingSaveText = null
    }

    // Helper function to read/parse logs from crash.log
    fun readLogsFromFile() {
        initialLogs.clear()
        if (crashLogFile.exists()) {
            try {
                val fileContent = crashLogFile.readText()
                val entries = fileContent.split("=== END CRASH REPORT ===")
                var idCounter = 1
                entries.forEach { entry ->
                    if (entry.trim().isNotEmpty() && entry.contains("=== CRASH REPORT ===")) {
                        var timestamp = ""
                        var threadName = ""
                        var exceptionType = ""
                        var message = ""
                        val stackTrace = entry.trim()
                        
                        val lines = entry.lines()
                        lines.forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.startsWith("Timestamp:")) {
                                timestamp = trimmed.substringAfter("Timestamp:").trim()
                            } else if (trimmed.startsWith("Thread:")) {
                                threadName = trimmed.substringAfter("Thread:").trim()
                            } else if (trimmed.startsWith("Exception:")) {
                                exceptionType = trimmed.substringAfter("Exception:").trim()
                            } else if (trimmed.startsWith("Message:")) {
                                message = trimmed.substringAfter("Message:").trim()
                            }
                        }
                        
                        val tag = when {
                            stackTrace.contains("libreoffice") || stackTrace.contains("lok::") || exceptionType.contains("Signal") -> "Native Core"
                            stackTrace.contains("JNI") || exceptionType.contains("Serialization") -> "JNI Bridge"
                            stackTrace.contains("DocParser") || exceptionType.contains("Xml") || exceptionType.contains("Tag") -> "Document Parser"
                            else -> "JVM UI"
                        }
                        
                        val title = if (message.isNotEmpty()) {
                            if (message.length > 50) message.take(50) + "..." else message
                        } else {
                            exceptionType.substringAfterLast(".")
                        }
                        
                        val fullStackTrace = if (!stackTrace.endsWith("=== END CRASH REPORT ===")) {
                            stackTrace + "\n=== END CRASH REPORT ==="
                        } else {
                            stackTrace
                        }

                        initialLogs.add(
                            CrashLog(
                                id = idCounter.toString(),
                                title = title,
                                exceptionType = exceptionType,
                                timestamp = timestamp,
                                tag = tag,
                                severity = if (tag == "Native Core") "FATAL" else "ERROR",
                                summary = "Thread: $threadName - $message",
                                stackTrace = fullStackTrace
                            )
                        )
                        idCounter++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        readLogsFromFile()
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var activeDetailLog by remember { mutableStateOf<CrashLog?>(null) }

    // Filter and search logic
    val filteredLogs = remember(selectedFilter, searchQuery, initialLogs.size) {
        initialLogs.filter { log ->
            val matchFilter = selectedFilter == "All" || log.tag == selectedFilter
            val matchQuery = log.title.contains(searchQuery, ignoreCase = true) ||
                    log.exceptionType.contains(searchQuery, ignoreCase = true) ||
                    log.summary.contains(searchQuery, ignoreCase = true)
            matchFilter && matchQuery
        }
    }

    // Helper functions
    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
        Toast.makeText(context, "Copied log content to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareLogs(text: String, title: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    fun saveToTxtFile(text: String, fileName: String) {
        pendingSaveText = text
        createDocumentLauncher.launch(fileName)
    }

    fun deleteSingleLog(log: CrashLog) {
        initialLogs.remove(log)
        try {
            if (initialLogs.isEmpty()) {
                if (crashLogFile.exists()) {
                    crashLogFile.delete()
                }
            } else {
                val sb = java.lang.StringBuilder()
                initialLogs.forEach { remainingLog ->
                    sb.append(remainingLog.stackTrace).append("\n\n")
                }
                crashLogFile.writeText(sb.toString())
            }
            Toast.makeText(context, "Log deleted", Toast.LENGTH_SHORT).show()
            readLogsFromFile()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to delete: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteAllLogs() {
        try {
            if (crashLogFile.exists()) {
                crashLogFile.delete()
            }
            initialLogs.clear()
            Toast.makeText(context, "All logs deleted successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to delete logs: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun simulateNewCrash() {
        Toast.makeText(context, "Crashing application now...", Toast.LENGTH_SHORT).show()
        throw java.lang.RuntimeException("Real simulated crash via Expressive Debug Station!")
    }

    fun buildFullLogsString(): String {
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("PAPIRUS OFFICE DIAGNOSTIC SYSTEM REPORT\n")
        sb.append("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("Total Incidents: ${initialLogs.size}\n")
        sb.append("=========================================\n\n")
        initialLogs.forEachIndexed { idx, log ->
            sb.append("INCIDENT #${idx + 1} [${log.severity}] (${log.tag})\n")
            sb.append("Title: ${log.title}\n")
            sb.append("Type: ${log.exceptionType}\n")
            sb.append("Time: ${log.timestamp}\n")
            sb.append("Summary: ${log.summary}\n")
            sb.append("StackTrace:\n${log.stackTrace}\n")
            sb.append("-----------------------------------------\n\n")
        }
        return sb.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Diagnostic & Crash Reports",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_crash_logs")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save All action
                    IconButton(
                        onClick = {
                            val allLogsText = buildFullLogsString()
                            saveToTxtFile(allLogsText, "papirus_all_crash_logs.txt")
                        },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save All logs")
                    }
                    // Share All action
                    IconButton(
                        onClick = {
                            val allLogsText = buildFullLogsString()
                            shareLogs(allLogsText, "Share Diagnostic Reports")
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share All logs")
                    }
                    // Copy All action
                    IconButton(
                        onClick = {
                            val allLogsText = buildFullLogsString()
                            copyToClipboard(allLogsText)
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All logs")
                    }
                    // Delete All action
                    IconButton(
                        onClick = { deleteAllLogs() },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear all logs")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)) // Beautiful subtle soft slate bg
        ) {
            // Material 3 Expressive Header Banner Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF1E293B), // Dark Slate
                                Color(0xFF475569)  // Medium Slate
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Expressive Debug Station",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Real-time native signals, core execution segmentation dumps, and UI pipeline telemetry logs.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Incidents: ${initialLogs.size}",
                            color = Color(0xFFF8FAFC),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Button(
                            onClick = { simulateNewCrash() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp).padding(end = 2.dp)
                            )
                            Text("Simulasikan Crash Baru", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar Component
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logs (e.g. OOM, render, JVM)...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFF475569),
                    unfocusedBorderColor = Color(0xFFE2E8F0)
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Horizontal Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf("All", "Native Core", "JVM UI", "JNI Bridge")
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else Color(0xFF475569),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Crash Logs List Section
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "No reports found matching criteria",
                            color = Color(0xFF64748B),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { activeDetailLog = log }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Badge Tag (Native, JVM, etc.)
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (log.tag) {
                                                    "Native Core" -> Color(0xFFFEE2E2)
                                                    "JVM UI" -> Color(0xFFFEF3C7)
                                                    else -> Color(0xFFE0F2FE)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = log.tag,
                                            color = when (log.tag) {
                                                "Native Core" -> Color(0xFFEF4444)
                                                "JVM UI" -> Color(0xFFD97706)
                                                else -> Color(0xFF0284C7)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Timestamp
                                    Text(
                                        text = log.timestamp,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Title
                                Text(
                                    text = log.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Exception Type
                                Text(
                                    text = log.exceptionType,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Summary text
                                Text(
                                    text = log.summary,
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Spacer(modifier = Modifier.height(8.dp))

                                // Small item actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "View full dump",
                                        fontSize = 12.sp,
                                        color = Color(0xFF2563EB),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier
                                            .clickable { activeDetailLog = log }
                                            .padding(8.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    IconButton(
                                        onClick = { copyToClipboard(log.stackTrace) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy log",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { shareLogs(log.stackTrace, "Share single crash dump") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Share,
                                            contentDescription = "Share log",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { deleteSingleLog(log) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete log",
                                            tint = MaterialTheme.colorScheme.error,
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
    }

    // Modal detailed stack trace viewer
    activeDetailLog?.let { activeLog ->
        AlertDialog(
            onDismissRequest = { activeDetailLog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeLog.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = activeLog.exceptionType,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFEF4444)
                        )
                    }
                    IconButton(onClick = { activeDetailLog = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close details")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Actions row on modal
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { copyToClipboard(activeLog.stackTrace) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy Trace", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { saveToTxtFile(activeLog.stackTrace, "papirus_crash_dump_${activeLog.id}.txt") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save TXT", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { shareLogs(activeLog.stackTrace, "Share single crash dump") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                deleteSingleLog(activeLog)
                                activeDetailLog = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }

                    // Metadata details block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Severity: ${activeLog.severity}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF475569))
                            Text("Subsystem: ${activeLog.tag}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color(0xFF475569))
                            Text("Time logged: ${activeLog.timestamp}", fontSize = 11.sp, color = Color(0xFF475569))
                            Text("Incident ID: ${activeLog.id}", fontSize = 11.sp, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(activeLog.summary, fontSize = 12.sp, color = Color(0xFF1E293B), lineHeight = 16.sp)
                        }
                    }

                    // StackTrace Monospace Scrollable block
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Deep developer black bg
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            val scrollState = androidx.compose.foundation.rememberScrollState()
                            Text(
                                text = activeLog.stackTrace,
                                color = Color(0xFF34D399), // Monospace terminal green
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDetailLog = null }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
