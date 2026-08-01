package com.example.ui.options

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun InkyViewSettingsSubpage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferencesRepository = remember { InkyPreferencesRepository(context) }
    val viewOptions by preferencesRepository.viewOptionsFlow.collectAsState(initial = InkyViewOptions())

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
                Text(
                    text = "Content Display Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Images", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Display inline graphics and embedded pictures in documents", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewOptions.showImages,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                preferencesRepository.updateShowImages(isChecked)
                            }
                        },
                        modifier = Modifier.testTag("switch_show_images")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Tables", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Render cells and structured spreadsheets elements inside text", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewOptions.showTables,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                preferencesRepository.updateShowTables(isChecked)
                            }
                        },
                        modifier = Modifier.testTag("switch_show_tables")
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Navigation & Layout Controls",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Outline Folding", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Double-tap headings to collapse or expand nested sections", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewOptions.enableOutlineFolding,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                preferencesRepository.updateEnableOutlineFolding(isChecked)
                            }
                        },
                        modifier = Modifier.testTag("switch_outline_folding")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Smooth Scrolling", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text("Use physics and damping animations when navigating documents", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = viewOptions.enableSmoothScrolling,
                        onCheckedChange = { isChecked ->
                            coroutineScope.launch {
                                preferencesRepository.updateEnableSmoothScrolling(isChecked)
                            }
                        },
                        modifier = Modifier.testTag("switch_smooth_scrolling")
                    )
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Zoom Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text("Zoom Mode", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZoomMode.values().forEach { mode ->
                        val isSelected = viewOptions.zoomMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    preferencesRepository.updateZoomMode(mode)
                                }
                            },
                            label = {
                                Text(
                                    text = when (mode) {
                                        ZoomMode.LAST -> "Last Used"
                                        ZoomMode.FIT_WIDTH -> "Fit Width"
                                        ZoomMode.HUNDRED -> "100%"
                                        ZoomMode.CUSTOM -> "Custom"
                                    },
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("chip_zoom_mode_${mode.name.lowercase()}")
                        )
                    }
                }

                if (viewOptions.zoomMode == ZoomMode.CUSTOM) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Custom Zoom Factor", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("${viewOptions.customZoomPercent}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = viewOptions.customZoomPercent.toFloat(),
                        onValueChange = { value ->
                            coroutineScope.launch {
                                preferencesRepository.updateCustomZoomPercent(value.toInt())
                            }
                        },
                        valueRange = 25f..400f,
                        steps = 15,
                        modifier = Modifier.fillMaxWidth().testTag("slider_custom_zoom")
                    )
                }
            }
        }
    }
}
