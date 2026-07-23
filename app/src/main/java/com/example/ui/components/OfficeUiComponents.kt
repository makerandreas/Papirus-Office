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

// --- QAT (Quick Access Toolbar) ---
@Composable
fun QuickAccessToolbar(
    isTablet: Boolean,
    onActionClick: (String) -> Unit
) {
    var showOverflow by remember { mutableStateOf(false) }
    val qatActions = listOf(
        Triple("Save", Icons.Outlined.Save, "Save Document"),
        Triple("Undo", Icons.Outlined.Undo, "Undo"),
        Triple("Redo", Icons.Outlined.Redo, "Redo"),
        Triple("Share", Icons.Outlined.Share, "Share File"),
        Triple("Print", Icons.Outlined.Print, "Print Document"),
        Triple("Search", Icons.Outlined.Search, "Find & Replace"),
        Triple("AI", Icons.Outlined.AutoAwesome, "AI Assistant"),
        Triple("Cloud", Icons.Outlined.CloudUpload, "Cloud Sync"),
        Triple("Settings", Icons.Outlined.Settings, "Options")
    )

    val visibleCount = if (isTablet) 7 else 3
    val visibleActions = qatActions.take(visibleCount)
    val overflowActions = qatActions.drop(visibleCount)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleActions.forEach { action ->
                IconButton(
                    onClick = { onActionClick(action.first) },
                    modifier = Modifier.testTag("qat_${action.first.lowercase()}")
                ) {
                    Icon(
                        imageVector = action.second,
                        contentDescription = action.third,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Overflow Indicator (Inverted Triangle Icon)
        if (overflowActions.isNotEmpty()) {
            Box {
                IconButton(onClick = { showOverflow = !showOverflow }) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "QAT More Options",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                DropdownMenu(
                    expanded = showOverflow,
                    onDismissRequest = { showOverflow = false }
                ) {
                    overflowActions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action.third) },
                            leadingIcon = { Icon(action.second, contentDescription = null) },
                            onClick = {
                                onActionClick(action.first)
                                showOverflow = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Adaptive Formatting Toolbar ---
@Composable
fun AdaptiveFormattingToolbar(
    selectedObjectType: String, // "text", "table", "image", "chart", "formula"
    onFormatClick: (String) -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleRibbon: () -> Unit
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Horizontal Scrollable Formatter Area
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Context-adaptive elements based on user selected type
            when (selectedObjectType) {
                "text" -> {
                    FormatButton(Icons.Default.FormatBold, "Bold") { onFormatClick("bold") }
                    FormatButton(Icons.Default.FormatItalic, "Italic") { onFormatClick("italic") }
                    FormatButton(Icons.Default.FormatUnderlined, "Underline") { onFormatClick("underline") }
                    FormatButton(Icons.Default.FormatStrikethrough, "Strikethrough") { onFormatClick("strikethrough") }
                    VerticalSeparator()
                    FormatButton(Icons.Default.FormatAlignLeft, "Align Left") { onFormatClick("left") }
                    FormatButton(Icons.Default.FormatAlignCenter, "Align Center") { onFormatClick("center") }
                    FormatButton(Icons.Default.FormatAlignRight, "Align Right") { onFormatClick("right") }
                    FormatButton(Icons.Default.FormatAlignJustify, "Align Justify") { onFormatClick("justify") }
                    VerticalSeparator()
                    FormatButton(Icons.Default.FormatListBulleted, "Bullets") { onFormatClick("bullets") }
                    FormatButton(Icons.Default.FormatListNumbered, "Numbers") { onFormatClick("numbers") }
                }
                "table" -> {
                    FormatButton(Icons.Default.BorderAll, "Borders") { onFormatClick("borders") }
                    FormatButton(Icons.Default.GridOn, "Add Row") { onFormatClick("add_row") }
                    FormatButton(Icons.Default.GridOff, "Delete Row") { onFormatClick("delete_row") }
                    VerticalSeparator()
                    FormatButton(Icons.Default.TableChart, "Header Row") { onFormatClick("toggle_header") }
                    FormatButton(Icons.Default.MergeType, "Merge Cells") { onFormatClick("merge") }
                }
                "image" -> {
                    FormatButton(Icons.Default.Crop, "Crop") { onFormatClick("crop") }
                    FormatButton(Icons.Default.Filter, "Filters") { onFormatClick("filters") }
                    FormatButton(Icons.Default.Layers, "Arrange") { onFormatClick("arrange") }
                    VerticalSeparator()
                    FormatButton(Icons.Default.AspectRatio, "Fit Screen") { onFormatClick("fit") }
                }
                "chart" -> {
                    FormatButton(Icons.Default.Edit, "Edit Data") { onFormatClick("edit_data") }
                    FormatButton(Icons.Default.BarChart, "Chart Type") { onFormatClick("chart_type") }
                    FormatButton(Icons.Default.LegendToggle, "Toggle Legend") { onFormatClick("legend") }
                }
                "formula" -> {
                    FormatButton(Icons.Default.Functions, "Insert Sum") { onFormatClick("sum") }
                    FormatButton(Icons.Default.Calculate, "Formula Helper") { onFormatClick("helper") }
                    FormatButton(Icons.Default.FormatListBulleted, "A1 Reference") { onFormatClick("ref_type") }
                }
            }
        }

        VerticalSeparator()

        // Persistent Buttons on the right: Toggle Keyboard, Dropdown Ribbon
        IconButton(onClick = onToggleKeyboard) {
            Icon(Icons.Default.Keyboard, contentDescription = "Show/Hide Keyboard", tint = MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = onToggleRibbon) {
            Icon(Icons.Default.MenuOpen, contentDescription = "Open Simplified Ribbon Bar", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

// --- Simplified Ribbon Bar (Mobile Layout) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplifiedRibbonBar(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onCloseRibbon: () -> Unit,
    onActionClick: (String) -> Unit,
    moduleContext: String = "writer" // writer, calc, impress, pdf
) {
    var showCategorySelector by remember { mutableStateOf(false) }

    val categories = when (moduleContext) {
        "writer" -> listOf("File", "Home", "Insert", "Layout", "Review", "View", "Drawing", "Object")
        "calc" -> listOf("File", "Home", "Insert", "Formula", "Data", "Review", "View", "PivotTable")
        "impress" -> listOf("File", "Home", "Insert", "Design", "Transition", "Animation", "Slide show", "View")
        else -> listOf("File", "View", "Drawing")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showCategorySelector = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedCategory,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Switch Ribbon Tab",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onActionClick("Undo") }) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = { onActionClick("Redo") }) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }
                IconButton(onClick = onCloseRibbon) {
                    Icon(Icons.Default.Close, contentDescription = "Close Simplified Ribbon")
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // Actions Grid/Scrollable List representing active category options
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (selectedCategory) {
                "Home" -> {
                    RibbonItem("Paste", Icons.Default.ContentPaste) { onActionClick("paste") }
                    RibbonItem("Cut", Icons.Default.ContentCut) { onActionClick("cut") }
                    RibbonItem("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                    RibbonItem("Bold", Icons.Default.FormatBold) { onActionClick("bold") }
                    RibbonItem("Italic", Icons.Default.FormatItalic) { onActionClick("italic") }
                    RibbonItem("Underline", Icons.Default.FormatUnderlined) { onActionClick("underline") }
                }
                "Insert" -> {
                    RibbonItem("Table", Icons.Default.GridOn) { onActionClick("insert_table") }
                    RibbonItem("Picture", Icons.Default.Image) { onActionClick("insert_picture") }
                    RibbonItem("Shape", Icons.Default.Category) { onActionClick("insert_shape") }
                    RibbonItem("Equation", Icons.Default.Functions) { onActionClick("insert_equation") }
                    RibbonItem("Chart", Icons.Default.BarChart) { onActionClick("insert_chart") }
                    RibbonItem("Scan", Icons.Default.QrCodeScanner) { onActionClick("insert_scan") }
                }
                "File" -> {
                    RibbonItem("New", Icons.Default.Add) { onActionClick("file_new") }
                    RibbonItem("Open", Icons.Default.FolderOpen) { onActionClick("file_open") }
                    RibbonItem("Save", Icons.Default.Save) { onActionClick("file_save") }
                    RibbonItem("Export PDF", Icons.Default.PictureAsPdf) { onActionClick("file_export_pdf") }
                }
                "Formula" -> {
                    RibbonItem("Sum", Icons.Default.Functions) { onActionClick("formula_sum") }
                    RibbonItem("Average", Icons.Default.Analytics) { onActionClick("formula_average") }
                    RibbonItem("Count", Icons.Default.FormatListNumbered) { onActionClick("formula_count") }
                }
                else -> {
                    RibbonItem("Properties", Icons.Default.Info) { onActionClick("properties") }
                    RibbonItem("Zoom", Icons.Default.ZoomIn) { onActionClick("zoom") }
                    RibbonItem("Refresh", Icons.Default.Refresh) { onActionClick("refresh") }
                }
            }
        }

        // Tab selection sheet dialog
        if (showCategorySelector) {
            ModalBottomSheet(
                onDismissRequest = { showCategorySelector = false }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Ribbon Tabs",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    categories.forEach { cat ->
                        ListItem(
                            headlineContent = { Text(cat) },
                            modifier = Modifier
                                .background(
                                    if (cat == selectedCategory) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onCategoryChange(cat)
                                    showCategorySelector = false
                                }
                        )
                    }
                }
            }
        }
    }
}

// --- Ribbon Full View (Tablet Tabbed Layout) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RibbonFullView(
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onActionClick: (String) -> Unit,
    moduleContext: String = "writer"
) {
    var isMinimized by remember { mutableStateOf(false) }

    val categories = when (moduleContext) {
        "writer" -> listOf("File", "Home", "Insert", "Layout", "Review", "View")
        "calc" -> listOf("File", "Home", "Insert", "Formula", "Data", "Review", "View")
        "impress" -> listOf("File", "Home", "Insert", "Design", "Transition", "Animation", "View")
        else -> listOf("File", "View", "Drawing")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Tab Row with double tap listener for minimization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start
            ) {
                categories.forEach { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { onCategoryChange(cat) },
                                onDoubleClick = { isMinimized = !isMinimized }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = cat,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            IconButton(onClick = { isMinimized = !isMinimized }) {
                Icon(
                    imageVector = if (isMinimized) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                    contentDescription = "Minimize Ribbon"
                )
            }
        }

        // Expanded Options Content
        AnimatedVisibility(
            visible = !isMinimized,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large descriptive ribbon action cards
                when (selectedCategory) {
                    "Home" -> {
                        RibbonActionGroup("Clipboard", listOf(
                            Pair("Paste", Icons.Default.ContentPaste),
                            Pair("Cut", Icons.Default.ContentCut),
                            Pair("Copy", Icons.Default.ContentCopy)
                        ), onActionClick)

                        RibbonActionGroup("Font Style", listOf(
                            Pair("Bold", Icons.Default.FormatBold),
                            Pair("Italic", Icons.Default.FormatItalic),
                            Pair("Underline", Icons.Default.FormatUnderlined),
                            Pair("Strike", Icons.Default.FormatStrikethrough)
                        ), onActionClick)
                    }
                    "Insert" -> {
                        RibbonActionGroup("Pages & Tables", listOf(
                            Pair("Cover Page", Icons.Default.Description),
                            Pair("Table", Icons.Default.GridOn)
                        ), onActionClick)

                        RibbonActionGroup("Illustrations", listOf(
                            Pair("Picture", Icons.Default.Image),
                            Pair("Shape", Icons.Default.Category),
                            Pair("Chart", Icons.Default.BarChart)
                        ), onActionClick)

                        RibbonActionGroup("Math", listOf(
                            Pair("LaTeX MathML", Icons.Default.Functions),
                            Pair("OMML Math", Icons.Default.Calculate)
                        ), onActionClick)
                    }
                    "File" -> {
                        RibbonActionGroup("Document Ops", listOf(
                            Pair("New", Icons.Default.Add),
                            Pair("Open", Icons.Default.FolderOpen),
                            Pair("Save", Icons.Default.Save),
                            Pair("Save As", Icons.Default.SaveAs)
                        ), onActionClick)
                    }
                    else -> {
                        Text("Category elements loaded dynamically.", modifier = Modifier.padding(16.dp), color = Color.Gray)
                    }
                }
            }
        }
    }
}

// --- Floating Contextual Toolbar (FCT) ---
@Composable
fun FloatingContextualToolbar(
    visible: Boolean,
    contextType: String, // "text", "calc_cell", "calc_multi", "image", "chart"
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
                    "text" -> {
                        FctButton("Cut", Icons.Default.ContentCut) { onActionClick("cut") }
                        FctButton("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                        FctButton("Paste", Icons.Default.ContentPaste) { onActionClick("paste") }
                        FctSeparator()
                        FctButton("Gemini Write", Icons.Default.AutoAwesome) { onActionClick("ai_write") }
                    }
                    "calc_cell" -> {
                        FctButton("Cut", Icons.Default.ContentCut) { onActionClick("cut") }
                        FctButton("Copy", Icons.Default.ContentCopy) { onActionClick("copy") }
                        FctButton("Paste", Icons.Default.ContentPaste) { onActionClick("paste") }
                        FctSeparator()
                        FctButton("Ref Type", Icons.Default.FormatListBulleted) { onActionClick("reference_type") }
                        FctButton("Comment", Icons.Default.Comment) { onActionClick("comment") }
                    }
                    "calc_multi" -> {
                        FctButton("Clear", Icons.Default.ClearAll) { onActionClick("clear") }
                        FctButton("Fill Mode", Icons.Default.FormatColorFill) { onActionClick("fill") }
                        FctButton("Merge", Icons.Default.MergeType) { onActionClick("merge") }
                        FctButton("Chart", Icons.Default.BarChart) { onActionClick("chart") }
                    }
                    "image" -> {
                        FctButton("Duplicate", Icons.Default.ContentCopy) { onActionClick("duplicate") }
                        FctButton("Delete", Icons.Default.Delete) { onActionClick("delete") }
                        FctButton("Fit", Icons.Default.AspectRatio) { onActionClick("fit") }
                        FctSeparator()
                        FctButton("AI Style", Icons.Default.AutoAwesome) { onActionClick("ai_style") }
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
                Icon(Icons.Default.ArrowBack, contentDescription = "Exit Find")
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
private fun RibbonItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RibbonActionGroup(
    groupTitle: String,
    actions: List<Pair<String, ImageVector>>,
    onActionClick: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        modifier = Modifier.wrapContentWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                actions.forEach { act ->
                    Column(
                        modifier = Modifier
                            .clickable { onActionClick(act.first) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(act.second, contentDescription = act.first, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(act.first, fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = groupTitle,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
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
    docName: String = "Inky_Dokumen.odt",
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
                        Text(
                            text = moduleName,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = moduleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Area Tengah: Loading Indicator khas Material 3 Expressive
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(96.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(72.dp),
                            color = moduleColor,
                            strokeWidth = 6.dp,
                            trackColor = moduleColor.copy(alpha = 0.15f)
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    // Area Bawah: 2 baris teks rata tengah
                    // Baris 1: "Opening [docName]..." atau "Creating document..."
                    Text(
                        text = if (isCreating) {
                            androidx.compose.ui.res.stringResource(com.example.R.string.loading_creating_doc)
                        } else {
                            androidx.compose.ui.res.stringResource(com.example.R.string.loading_opening_file, docName)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Baris 2: Status/progres pembuatan/pembukaan dokumen
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

// --- BARU: Popup Dialog "Saving..." ---
@Composable
fun SavingProgressPopupDialog(
    docName: String = "Inky_Dokumen.odt",
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

