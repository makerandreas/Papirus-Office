package com.example.ui.options

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    AnimatedContent(
        targetState = activeSubSubpage,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else if (targetState == null && initialState != null) {
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            } else {
                (fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(
                    fadeOut() + scaleOut(targetScale = 0.95f)
                )
            }
        },
        label = "InkyViewSubSubpageTransition"
    ) { currentSubSubpage ->
        when (currentSubSubpage) {
            "double_tap_fold" -> {
                // --- SUB-SUBPAGE: Double Tap to Fold Outline ---
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Expressive Card Container for the primary option
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable Double Tap to Fold",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Double tap any heading to collapse or expand all content under it",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Switch(
                                checked = viewOptions.enableOutlineFolding,
                                onCheckedChange = { isChecked ->
                                    coroutineScope.launch {
                                        preferencesRepository.updateEnableOutlineFolding(isChecked)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("switch_enable_double_tap_fold")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isFoldingEnabled = viewOptions.enableOutlineFolding
                    val textColor = if (isFoldingEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    val subtitleColor = if (isFoldingEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Sub-levels",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Also fold lower-level subheadings nested under the clicked heading",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = subtitleColor
                                )
                            }
                            Switch(
                                checked = viewOptions.includeSubLevelsFold && isFoldingEnabled,
                                enabled = isFoldingEnabled,
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
            }

            else -> {
                // --- MAIN SUBPAGE: Inky Options - View ---
                Column(modifier = modifier.fillMaxWidth()) {
                    // Group 1: Guides
                    InkySubCategoryHeader(title = "Guides")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        InkySwitchRow(
                            title = "Helplines while Moving",
                            checked = viewOptions.helplinesWhileMoving,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateHelplinesWhileMoving(isChecked) }
                            },
                            testTag = "switch_helplines_while_moving"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 2: Display
                    InkySubCategoryHeader(title = "Display")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        InkySwitchRow(
                            title = "Images and Objects",
                            checked = viewOptions.showImages,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowImages(isChecked) }
                            },
                            testTag = "switch_show_images"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Tables",
                            checked = viewOptions.showTables,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowTables(isChecked) }
                            },
                            testTag = "switch_show_tables"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Drawing and Controls",
                            checked = viewOptions.showDrawingControls,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowDrawingControls(isChecked) }
                            },
                            testTag = "switch_drawing_controls"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Comments",
                            checked = viewOptions.showComments,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowComments(isChecked) }
                            },
                            testTag = "switch_show_comments"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Resolved Comments",
                            checked = viewOptions.showResolvedComments,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowResolvedComments(isChecked) }
                            },
                            testTag = "switch_resolved_comments"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 3: Display Fields
                    InkySubCategoryHeader(title = "Display Fields")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        InkySwitchRow(
                            title = "Hidden text",
                            checked = viewOptions.showHiddenText,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowHiddenText(isChecked) }
                            },
                            testTag = "switch_hidden_text"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Hidden paragraphs",
                            checked = viewOptions.showHiddenParagraphs,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateShowHiddenParagraphs(isChecked) }
                            },
                            testTag = "switch_hidden_paragraphs"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 4: Track Changes
                    InkySubCategoryHeader(title = "Track Changes")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        InkySwitchRow(
                            title = "Tracked Deletions in Margin",
                            checked = viewOptions.trackedDeletionsInMargin,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateTrackedDeletionsMargin(isChecked) }
                            },
                            testTag = "switch_tracked_deletions"
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        InkySwitchRow(
                            title = "Tooltips on Tracked Changes",
                            checked = viewOptions.tooltipsOnTrackedChanges,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateTooltipsTrackedChanges(isChecked) }
                            },
                            testTag = "switch_tooltips_tracked"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 5: Outline Folding
                    InkySubCategoryHeader(title = "Outline Folding")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateSubSubpage("double_tap_fold") }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Double Tap to Fold Outline",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Configure outline section folding behavior",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Open Double Tap to Fold Options",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 6: View
                    InkySubCategoryHeader(title = "View")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        InkySwitchRow(
                            title = "Enable Smooth Scrolling",
                            checked = viewOptions.enableSmoothScrolling,
                            onCheckedChange = { isChecked ->
                                coroutineScope.launch { preferencesRepository.updateEnableSmoothScrolling(isChecked) }
                            },
                            testTag = "switch_smooth_scrolling"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Group 7: Zoom
                    InkySubCategoryHeader(title = "Zoom")
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(16.dp)
                    ) {
                        Text("Zoom Options", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(12.dp))

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_zoom")
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

@Composable
private fun InkySubCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 12.dp)
    )
}

@Composable
private fun InkySwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
