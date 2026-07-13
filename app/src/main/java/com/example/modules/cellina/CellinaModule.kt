package com.example.modules.cellina

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.scale
import com.example.ui.components.FloatingContextualToolbar

@Composable
fun CellinaModule(
    isTablet: Boolean,
    onFormulaSelected: (String) -> Unit
) {
    // Spreadsheet state
    var selectedSheet by remember { mutableStateOf("Sheet1") }
    val sheets = listOf("Sheet1", "Sheet2", "Sheet3")
    
    // Formula bar logic
    var formulaText by remember { mutableStateOf("=SUM(B2:C3)") }
    var activeCellRow by remember { mutableStateOf(2) } // 1-indexed for spreadsheet
    var activeCellCol by remember { mutableStateOf(2) } // 2 = 'B'

    // FCT state
    var showFct by remember { mutableStateOf(true) }
    var selectionMode by remember { mutableStateOf("single") } // "single" or "multi"

    // Simulate cell data values
    val columnsLabels = listOf("A", "B", "C", "D", "E")
    val cellValues = remember {
        mutableStateMapOf(
            "A1" to "Quarter", "B1" to "Inky Sales", "C1" to "Cellina Sales", "D1" to "Total",
            "A2" to "Q1", "B2" to "12000", "C2" to "15000", "D2" to "27000",
            "A3" to "Q2", "B3" to "14500", "C3" to "18200", "D3" to "32700",
            "A4" to "Q3", "B4" to "16000", "C4" to "21000", "D4" to "37000",
            "A5" to "Average", "B5" to "14166", "C5" to "18066", "D5" to "32233"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
    ) {
        // Excel-like Header & Active cell info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active cell box display e.g. "B2"
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${columnsLabels.getOrNull(activeCellCol - 1) ?: "A"}$activeCellRow",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 14.sp
                )
            }

            // Formula Icon and Input Bar
            Icon(
                imageVector = Icons.Default.Functions,
                contentDescription = "Formula Icon",
                tint = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = formulaText,
                onValueChange = {
                    formulaText = it
                    onFormulaSelected(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("formula_input"),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        // Horizontal Quick-Navigation and Sheet switcher row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left navigation arrows (cell jumping)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        if (activeCellCol > 1) activeCellCol--
                        else if (activeCellRow > 1) {
                            activeCellRow--
                            activeCellCol = columnsLabels.size
                        }
                    },
                    modifier = Modifier.testTag("btn_prev_cell")
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Cell (Shift+Tab)")
                }
                IconButton(
                    onClick = {
                        if (activeCellCol < columnsLabels.size) activeCellCol++
                        else {
                            activeCellRow++
                            activeCellCol = 1
                        }
                    },
                    modifier = Modifier.testTag("btn_next_cell")
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Cell (Tab)")
                }
            }

            // Sheet Swapping Tabs (Bottom level)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sheets.forEach { sheet ->
                    val isSelected = sheet == selectedSheet
                    Button(
                        onClick = { selectedSheet = sheet },
                        colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(sheet, fontSize = 12.sp)
                    }
                }
            }

            // Selection mode toggle (single vs multi-range FCT demonstration)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Range Mode:", fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                Switch(
                    checked = selectionMode == "multi",
                    onCheckedChange = { selectionMode = if (it) "multi" else "single" },
                    modifier = Modifier.scale(0.7f).testTag("range_toggle")
                )
            }
        }

        // Active Sheet Grid Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column {
                // Header Row (A, B, C, D, E)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(28.dp)
                            .background(Color.LightGray.copy(alpha = 0.5f))
                            .border(0.5.dp, Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    columnsLabels.forEach { label ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                                .border(0.5.dp, Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Grid Body rows (1 to 5)
                for (rowIdx in 1..5) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Row Numbering Column
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(40.dp)
                                .background(Color.LightGray.copy(alpha = 0.3f))
                                .border(0.5.dp, Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$rowIdx", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        // Cells loop
                        for (colIdx in 1..5) {
                            val colLabel = columnsLabels[colIdx - 1]
                            val cellId = "$colLabel$rowIdx"
                            val cellVal = cellValues[cellId] ?: ""
                            
                            val isActive = rowIdx == activeCellRow && colIdx == activeCellCol
                            
                            // Color code Formula highlighted range B2:C3 for SUM formulas!
                            val isFormulaHighlighted = formulaText.contains("B2:C3") && 
                                    (rowIdx in 2..3 && colIdx in 2..3)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(
                                        when {
                                            isActive -> MaterialTheme.colorScheme.primaryContainer
                                            isFormulaHighlighted -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                            else -> Color.White
                                        }
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = if (isActive) 2.dp else 0.5.dp,
                                            color = if (isActive) MaterialTheme.colorScheme.primary else Color.LightGray
                                        )
                                    )
                                    .clickable {
                                        activeCellRow = rowIdx
                                        activeCellCol = colIdx
                                        showFct = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cellVal,
                                    fontSize = 13.sp,
                                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else Color.Black,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Cell context selector popup FCT overlay
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                FloatingContextualToolbar(
                    visible = showFct,
                    contextType = if (selectionMode == "single") "calc_cell" else "calc_multi",
                    onActionClick = { action ->
                        showFct = false
                        onFormulaSelected("Tapped contextual action: $action on cell ${columnsLabels[activeCellCol-1]}$activeCellRow")
                    }
                )
            }
        }
    }
}
