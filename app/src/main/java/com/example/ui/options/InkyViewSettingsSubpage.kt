package com.example.ui.options

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makerandreas.papirusoffice.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkyViewSettingsSubpage(
    activeSubSubpage: String? = null,
    onNavigateSubSubpage: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember { InkyPreferencesRepository(context) }
    val viewOptions by preferencesRepository.viewOptionsFlow.collectAsState(initial = InkyViewOptions())

    var showCustomZoomDialog by remember { mutableStateOf(false) }
    var customZoomInput by remember { mutableStateOf(viewOptions.customZoomPercent.toString()) }
    var zoomDropdownExpanded by remember { mutableStateOf(false) }

    if (activeSubSubpage == "double_tap_fold") {
        // --- SUB-SUBPAGE: Double Tap to Fold Outline ---
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Double Tap to Fold",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Double tap any heading to collapse or expand all content under it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = viewOptions.enableOutlineFolding,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch {
                                    preferencesRepository.updateEnableOutlineFolding(isChecked)
                                }
                            },
                            modifier = Modifier.testTag("switch_enable_double_tap_fold")
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include Sub-levels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Also fold lower-level subheadings nested under the clicked heading",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = viewOptions.includeSubLevelsFold,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                preferencesRepository.updateIncludeSubLevelsFold(isChecked)
                            }
                        },
                        modifier = Modifier.testTag("switch_include_sub_levels")
                    )
                }
            }
        }
        return
    }

    // --- MAIN SUBPAGE: Inky Options - View ---
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Group 1: Guides
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Guides",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Helplines while Moving", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.helplinesWhileMoving,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateHelplinesWhileMoving(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_helplines_while_moving")
                    )
                }
            }
        }

        // Group 2: Display
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Images and Objects", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showImages,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowImages(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_show_images")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tables", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showTables,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowTables(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_show_tables")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Drawing and Controls", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showDrawingControls,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowDrawingControls(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_drawing_controls")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Comments", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showComments,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowComments(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_show_comments")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Resolved Comments", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showResolvedComments,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowResolvedComments(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_resolved_comments")
                    )
                }
            }
        }

        // Group 3: Display Fields
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Display Fields",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hidden text", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showHiddenText,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowHiddenText(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_hidden_text")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hidden paragraphs", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.showHiddenParagraphs,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateShowHiddenParagraphs(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_hidden_paragraphs")
                    )
                }
            }
        }

        // Group 4: Track Changes
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Track Changes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tracked Deletions in Margin", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.trackedDeletionsInMargin,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateTrackedDeletionsMargin(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_tracked_deletions")
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tooltips on Tracked Changes", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.tooltipsOnTrackedChanges,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateTooltipsTrackedChanges(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_tooltips_tracked")
                    )
                }
            }
        }

        // Group 5: Outline Folding
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Outline Folding",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateSubSubpage("double_tap_fold") }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Double Tap to Fold Outline", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Configure outline section folding behavior", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = viewOptions.enableOutlineFolding,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateEnableOutlineFolding(isChecked) }
                            },
                            modifier = Modifier.testTag("switch_outline_fold_quick")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = "Open Double Tap to Fold Options")
                    }
                }
            }
        }

        // Group 6: View
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Smooth Scrolling", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = viewOptions.enableSmoothScrolling,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch { preferencesRepository.updateEnableSmoothScrolling(isChecked) }
                        },
                        modifier = Modifier.testTag("switch_smooth_scrolling")
                    )
                }
            }
        }

        // Group 7: Zoom
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Zoom",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("Zoom Options", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                val selectedZoomLabel = when (viewOptions.zoomMode) {
                    ZoomMode.LAST -> "Use last document setting"
                    ZoomMode.FIT_WIDTH -> "Fit width"
                    ZoomMode.HUNDRED -> "100%"
                    ZoomMode.CUSTOM -> "Custom (${viewOptions.customZoomPercent}%)"
                }

                ExposedDropdownMenuBox(
                    expanded = zoomDropdownExpanded,
                    onExpandedChange = { zoomDropdownExpanded = !zoomDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedZoomLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoomDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("dropdown_zoom_options")
                    )
                    ExposedDropdownMenu(
                        expanded = zoomDropdownExpanded,
                        onDismissRequest = { zoomDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Use last document setting") },
                            onClick = {
                                zoomDropdownExpanded = false
                                coroutineScope.launch { preferencesRepository.updateZoomMode(ZoomMode.LAST) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fit width") },
                            onClick = {
                                zoomDropdownExpanded = false
                                coroutineScope.launch { preferencesRepository.updateZoomMode(ZoomMode.FIT_WIDTH) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("100%") },
                            onClick = {
                                zoomDropdownExpanded = false
                                coroutineScope.launch { preferencesRepository.updateZoomMode(ZoomMode.HUNDRED) }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Custom") },
                            onClick = {
                                zoomDropdownExpanded = false
                                coroutineScope.launch { preferencesRepository.updateZoomMode(ZoomMode.CUSTOM) }
                                customZoomInput = viewOptions.customZoomPercent.toString()
                                showCustomZoomDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCustomZoomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomZoomDialog = false },
            title = { Text("Custom Zoom Scaling") },
            text = {
                Column {
                    Text("Enter custom zoom scale percentage (25% - 400%):", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customZoomInput,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                customZoomInput = input
                            }
                        },
                        label = { Text("Zoom Percentage") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_custom_zoom")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val zoomVal = customZoomInput.toIntOrNull()
                        if (zoomVal != null && zoomVal in 25..400) {
                            coroutineScope.launch {
                                preferencesRepository.updateCustomZoomPercent(zoomVal)
                                preferencesRepository.updateZoomMode(ZoomMode.CUSTOM)
                            }
                            showCustomZoomDialog = false
                        } else {
                            Toast.makeText(context, "Please enter a value between 25 and 400", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_custom_zoom")
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomZoomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

