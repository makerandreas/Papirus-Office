package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makerandreas.papirusoffice.data.SwTextFormattingEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwTextFormattingInspectorDialog(
    summary: SwTextFormattingEngine.ParaPortionSummary,
    fontFamilyName: String,
    fontSizePt: Int,
    isBold: Boolean,
    isItalic: Boolean,
    isUnderline: Boolean,
    onDismiss: () -> Unit,
    onApplyStyle: (String) -> Unit,
    onToggleDropCap: (Boolean) -> Unit,
    onSetLineSpacing: (Float) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Scripts, 2: Lines, 3: SwDoc Model

    val swDoc = remember {
        com.makerandreas.papirusoffice.data.SwDoc("Papirus Writer Document").apply {
            val bodyText = summary.lines.joinToString(" ") { it.textSnippet }
            nodes.appendBodyTextNode(bodyText)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("formatting_inspector_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "Writer Core Layout & Formatting",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "SwDoc • SwNodes • SwpHintsArray • SwFrame",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Overview", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Scripts", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Lines (${summary.lines.size})", fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("SwDoc Model", fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Active Style & Font Attr
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Active Paragraph Style (SwTxtFmtColl):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = "${summary.activeStyleName}  •  Font: $fontFamilyName $fontSizePt pt (${fontSizePt * 20} twips)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Direct Character Attributes: " + listOfNotNull(
                                            if (isBold) "Bold" else null,
                                            if (isItalic) "Italic" else null,
                                            if (isUnderline) "Underline" else null
                                        ).joinToString(", ").ifEmpty { "Normal" },
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            // Layout Sizing metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Line Layout", fontSize = 10.sp, color = Color.Gray)
                                        Text("${summary.lines.size} Lines", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${summary.lineSpacingTwips} twips (${"%.2f".format(summary.lineSpacingFactor)}x)", fontSize = 11.sp)
                                    }
                                }

                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Text Portions", fontSize = 10.sp, color = Color.Gray)
                                        Text("${summary.totalPortions} Portions", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${summary.totalLength} Chars", fontSize = 11.sp)
                                    }
                                }
                            }

                            // Drop Cap State
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Drop Cap (SwDropPortion)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            text = if (summary.dropCap.enabled) 
                                                "Active: '${summary.dropCap.initialChar}' covers ${summary.dropCap.linesCount} lines (${summary.dropCap.dropHeightTwips} twips)"
                                                else "Disabled (Click to toggle 3-line drop cap)",
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = summary.dropCap.enabled,
                                        onCheckedChange = { onToggleDropCap(it) }
                                    )
                                }
                            }

                            // Fast Line Spacing Presets
                            Text("Line Spacing Presets (SwLineLayout::nRealHeight):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1.0f to "1.0x", 1.15f to "1.15x", 1.5f to "1.5x", 2.0f to "2.0x").forEach { (factor, label) ->
                                    FilterChip(
                                        selected = summary.lineSpacingFactor == factor,
                                        onClick = { onSetLineSpacing(factor) },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Script Classification (SwScriptInfo)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "LibreOffice Writer partitions text portions based on Unicode script ranges for font fallback & kerning.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            val script = summary.scriptAnalysis
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                ScriptProgressRow("Latin (Western)", script.latinCount, script.latinPercent, Color(0xFF1976D2))
                                ScriptProgressRow("Asian (CJK)", script.asianCount, script.asianPercent, Color(0xFF388E3C))
                                ScriptProgressRow("CTL (Complex Layout)", script.ctlCount, script.ctlPercent, Color(0xFFD32F2F))
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Script Change Boundaries: ${script.scriptChanges.size} transitions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("Primary Script Engine: ${script.primaryScript.name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("Asian Grid Mode: ${if (summary.asianGridEnabled) "Enabled (20 cols/10 rows grid)" else "Standard Flow"}", fontSize = 11.sp)
                                    Text("Hanging Punctuation: ${if (summary.hangingPunctuation) "Enabled (Asian Punctuation Overlap)" else "Standard Margin"}", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    2 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(summary.lines) { line ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Line #${line.lineNumber}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            Text("${line.heightTwips} twips (Ascent: ${line.ascentTwips}, Descent: ${line.descentTwips})", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Text(
                                            text = if (line.textSnippet.isBlank()) "[Blank Line]" else "\"${line.textSnippet}\"",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                        Text("Offset: ${line.startCharIdx} • Length: ${line.lengthChars} chars • Portions: ${line.portionCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("SwDoc Central Architecture", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Document: ${swDoc.documentTitle}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                        Text("SwNodes Total: ${swDoc.nodes.getAllNodes().size} nodes across 5 top-level sections", fontSize = 11.sp)
                                    }
                                }
                            }

                            item {
                                Text("1. SwNodes Top-Level Sections:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("• Section 0: Empty Section (Start: 0)", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("• Section 1: Footnote Content (Start: ${swDoc.nodes.footnoteSectionStart})", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("• Section 2: Frame / Header / Footer Content (Start: ${swDoc.nodes.headerFooterSectionStart})", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("• Section 3: Deleted Change Tracking (Start: ${swDoc.nodes.changeTrackingSectionStart})", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("• Section 4: Body Content (Start: ${swDoc.nodes.bodySectionStart})", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            item {
                                Text("2. Text Attributes & Sub-Structure (SwpHintsArray):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("• Formatting: SwTextCharFormat, RES_TXTATR_AUTOFMT", fontSize = 11.sp)
                                    Text("• Nesting: SwTextINetFormat (Hyperlinks), SwTextRuby, Meta", fontSize = 11.sp)
                                    Text("• Misc: Reference Marks, ToX Marks", fontSize = 11.sp)
                                    Text("• Without End: Fields, Footnotes, Flys (AS_CHAR dummy char)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            item {
                                Text("3. Fields, Lists & Layout Tree:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("• DocumentFieldsManager: SwFieldType (PageNumber, Author, Date)", fontSize = 11.sp)
                                    Text("• DocumentListsManager: SwNumRule, SwNodeNum, SwList", fontSize = 11.sp)
                                    Text("• UndoManager: Stack depth (${swDoc.undoManager.getUndoStackSize()} undo / ${swDoc.undoManager.getRedoStackSize()} redo)", fontSize = 11.sp)
                                    Text("• Layout Tree: SwPageFrame -> SwBodyFrame -> SwTxtFrm (SwFlowFrame master/follow)", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close Inspector")
            }
        }
    )
}

@Composable
private fun ScriptProgressRow(label: String, count: Int, percent: Float, barColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Text("$count chars (${"%.1f".format(percent)}%)", fontSize = 11.sp, color = Color.Gray)
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color.LightGray.copy(alpha = 0.3f)
        )
    }
}
