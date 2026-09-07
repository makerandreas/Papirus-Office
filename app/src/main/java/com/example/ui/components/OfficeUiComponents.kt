package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.res.stringResource
import com.example.R





// --- Floating Contextual Toolbar (FCT) ---
@Composable
fun FloatingContextualToolbar(
    visible: Boolean,
    contextType: String, // "text", "text_compact", "calc_cell", "calc_multi", "image", "chart"
    modifier: Modifier = Modifier,
    onActionClick: (String) -> Unit
) {
    if (visible) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                .then(modifier)
                .padding(8.dp)
                .wrapContentWidth()
                .testTag("fct_container")
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Actions depending on selection
                when (contextType) {
                    "text_compact", "text_viewer" -> {
                        FctButton("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                        FctButton("Edit", Icons.Default.Edit) { onActionClick("edit") }
                        FctButton("Select All", Icons.Default.SelectAll) { onActionClick("select_all") }
                    }
                    "text" -> {
                        FctButton("Cut", Icons.Default.ContentCut) { onActionClick("cut") }
                        FctButton("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                        FctButton("Paste", Icons.Default.ContentPaste) { onActionClick("paste") }
                        FctSeparator()
                        FctButton("Gemini Write", Icons.Default.EditNote) { onActionClick("ai_write") }
                    }
                    "calc_cell" -> {
                        FctButton("Cut", Icons.Default.ContentCut) { onActionClick("cut") }
                        FctButton("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                        FctButton("Paste", Icons.Default.ContentPaste) { onActionClick("paste") }
                        FctSeparator()
                        FctButton("Ref Type", Icons.AutoMirrored.Filled.FormatListBulleted) { onActionClick("reference_type") }
                        FctButton("Comment", Icons.AutoMirrored.Filled.Comment) { onActionClick("comment") }
                    }
                    "calc_multi" -> {
                        FctButton("Clear", Icons.Default.ClearAll) { onActionClick("clear") }
                        FctButton("Fill Mode", Icons.Default.FormatColorFill) { onActionClick("fill") }
                        FctButton("Merge", Icons.AutoMirrored.Filled.MergeType) { onActionClick("merge") }
                        FctButton("Chart", Icons.Default.BarChart) { onActionClick("chart") }
                    }
                    "image" -> {
                        FctButton("Duplicate", Icons.Default.ContentCopy) { onActionClick("duplicate") }
                        FctButton("Delete", Icons.Default.Delete) { onActionClick("delete") }
                        FctButton("Fit", Icons.Default.AspectRatio) { onActionClick("fit") }
                        FctSeparator()
                        FctButton("AI Style", Icons.Default.Style) { onActionClick("ai_style") }
                    }
                    "chart" -> {
                        FctButton("Data", Icons.Default.Edit) { onActionClick("edit_data") }
                        FctButton("To Mermaid", Icons.Default.SwapHoriz) { onActionClick("convert_mermaid") }
                        FctButton("Delete", Icons.Default.Delete) { onActionClick("delete") }
                    }
                }
            }
        }
    }
}

// --- Find & Replace Bar ---
@Composable
fun FindAndReplaceBar(
    isTablet: Boolean,
    onFind: (String) -> Unit,
    onReplace: (String, String) -> Unit,
    onClose: () -> Unit
) {
    var findQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    if (!isTablet) {
        // Mobile layout replacing title bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit Find")
            }
            Column(modifier = Modifier.weight(1f)) {
                TextField(
                    value = findQuery,
                    onValueChange = {
                        findQuery = it
                        onFind(it)
                    },
                    placeholder = { Text("Find text...") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                TextField(
                    value = replaceQuery,
                    onValueChange = { replaceQuery = it },
                    placeholder = { Text("Replace with...") },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
            IconButton(onClick = { onReplace(findQuery, replaceQuery) }) {
                Icon(Icons.Default.FindReplace, contentDescription = "Replace")
            }
        }
    } else {
        // Tablet layout below Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = findQuery,
                onValueChange = {
                    findQuery = it
                    onFind(it)
                },
                label = { Text("Find") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = replaceQuery,
                onValueChange = { replaceQuery = it },
                label = { Text("Replace With") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { onFind(findQuery) }) {
                Text("Find")
            }
            Button(onClick = { onReplace(findQuery, replaceQuery) }) {
                Text("Replace All")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close Find")
            }
        }
    }
}

// --- Dialogs & Sidebars Wrappers ---
@Composable
fun OfficeDialogSheet(
    title: String,
    onBack: () -> Unit,
    onApply: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // Full screen dialog sheet for mobile
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onApply) {
                Text("Apply")
            }
        }
        HorizontalDivider()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun OfficeSidebar(
    title: String,
    onClose: () -> Unit,
    onApply: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    // Docked sidebar for Tablet
    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxHeight()
            .width(360.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close Sidebar")
                    }
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                content()
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onClose) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onApply) {
                    Text("Apply")
                }
            }
        }
    }
}

// --- Small helper widgets ---

@Composable
private fun FormatButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = desc)
    }
}

@Composable
private fun VerticalSeparator() {
    Spacer(modifier = Modifier.width(4.dp))
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
    )
    Spacer(modifier = Modifier.width(4.dp))
}


@Composable
private fun FctButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun FctSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(16.dp)
            .background(Color.Gray.copy(alpha = 0.4f))
    )
}

// --- BARU: Full-page Loading Popup ---
@Composable
fun FullPageDocumentLoadingPopup(
    moduleName: String = "Writer",
    moduleColor: Color = Color(0xFF2563EB),
    isCreating: Boolean = false,
    docName: String = "Document.odt",
    progressStatus: String = stringResource(R.string.loading_status_odf),
    onDismissRequest: () -> Unit = {}
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Area Atas: Teks Rata Tengah "Papirus [nama modul]"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.example.R.string.loading_module_prefix),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = moduleName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = moduleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Area Tengah: Loading Indicator Animasi Kustom Papirus Engine
                    com.makerandreas.papirusoffice.ui.components.PapirusEngineLoadingIndicator(
                        moduleName = moduleName,
                        moduleColor = moduleColor,
                        statusMessage = if (isCreating) {
                            androidx.compose.ui.res.stringResource(com.example.R.string.loading_creating_doc)
                        } else {
                            androidx.compose.ui.res.stringResource(com.example.R.string.loading_opening_file, docName)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Baris Status / Progres Rendering Dokumen
                    Text(
                        text = progressStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- BARU: Document Open Failed Dialog ---
@Composable
fun DocumentOpenFailedDialog(
    docName: String,
    errorMessage: String? = null,
    onDismissRequest: () -> Unit = {},
    onReturnToRecent: () -> Unit,
    onViewLogs: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.doc_open_failed_title),
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.doc_open_failed_msg, docName),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onReturnToRecent,
                modifier = Modifier.testTag("btn_failed_ok")
            ) {
                Text(stringResource(R.string.btn_ok), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            FilledTonalButton(
                onClick = onViewLogs,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.testTag("btn_failed_view_logs")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_view_logs), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

// --- BARU: Popup Dialog "Saving..." ---
@Composable
fun SavingProgressPopupDialog(
    docName: String = "Document.odt",
    moduleColor: Color = Color(0xFF2563EB),
    statusText: String = stringResource(R.string.status_saving),
    onDismissRequest: () -> Unit = {}
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismissRequest,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Area Atas-Tengah: Loading indicator khas Material 3 Expressive
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(64.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = moduleColor,
                        strokeWidth = 4.dp,
                        trackColor = moduleColor.copy(alpha = 0.15f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Teks Progres Penyimpanan
                Text(
                    text = stringResource(R.string.saving_doc_progress, docName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

