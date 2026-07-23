package com.example.ui.options

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun LoadSaveGeneralSubpage(
    activeSubSubpage: String?,
    onNavigateSubSubpage: (String?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("papirus_options_prefs", Context.MODE_PRIVATE) }

    // Persistent States with Defaults
    var loadUserSpecificSettings by remember { mutableStateOf(prefs.getBoolean("load_user_specific_settings", true)) }
    var loadPrinterSettings by remember { mutableStateOf(prefs.getBoolean("load_printer_settings", true)) }

    var autoRecoveryEnabled by remember { mutableStateOf(prefs.getBoolean("auto_recovery_enabled", true)) }
    var autoRecoveryInterval by remember { mutableIntStateOf(prefs.getInt("auto_recovery_interval", 10)) }
    var autoSaveDocumentToo by remember { mutableStateOf(prefs.getBoolean("auto_save_document_too", false)) }

    var editDocPropertiesBeforeSaving by remember { mutableStateOf(prefs.getBoolean("edit_doc_properties_before_saving", false)) }

    var alwaysCreateBackupCopy by remember { mutableStateOf(prefs.getBoolean("always_create_backup_copy", true)) }
    var placeBackupInSameFolder by remember { mutableStateOf(prefs.getBoolean("place_backup_in_same_folder", false)) }

    var saveUrlsRelativeFileSystem by remember { mutableStateOf(prefs.getBoolean("save_urls_relative_file_system", true)) }
    var saveUrlsRelativeInternet by remember { mutableStateOf(prefs.getBoolean("save_urls_relative_internet", true)) }

    var odfFormatVersion by remember { mutableStateOf(prefs.getString("odf_format_version", "1.4 Extended (recommended)") ?: "1.4 Extended (recommended)") }
    var alwaysSaveAs by remember { mutableStateOf(prefs.getString("always_save_as", "OpenDocument") ?: "OpenDocument") }
    var warnWhenNotSavingOdf by remember { mutableStateOf(prefs.getBoolean("warn_when_not_saving_odf", true)) }

    // Menu and Dialog states
    var showIntervalDialog by remember { mutableStateOf(false) }
    var tempIntervalText by remember { mutableStateOf(autoRecoveryInterval.toString()) }
    var showOdfVersionMenu by remember { mutableStateOf(false) }
    var showAlwaysSaveAsMenu by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = activeSubSubpage,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                // Navigating deeper into sub-subpage (slide in from right + fade in)
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> -width / 3 } + fadeOut()
                )
            } else if (targetState == null && initialState != null) {
                // Navigating back out (predictive back slide out to right + fade)
                (slideInHorizontally { width -> -width / 3 } + fadeIn()).togetherWith(
                    slideOutHorizontally { width -> width } + fadeOut()
                )
            } else {
                (fadeIn() + scaleIn(initialScale = 0.95f)).togetherWith(
                    fadeOut() + scaleOut(targetScale = 0.95f)
                )
            }
        },
        label = "LoadSaveSubSubpageTransition"
    ) { currentSubSubpage ->
        when (currentSubSubpage) {
            "auto_recovery" -> {
                // SUB-SUBPAGE: Save Auto Recovery Options
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Expressive Card Container for the primary option
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.save_auto_recovery_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoRecoveryEnabled,
                                onCheckedChange = {
                                    autoRecoveryEnabled = it
                                    prefs.edit().putBoolean("auto_recovery_enabled", it).apply()
                                },
                                modifier = Modifier.testTag("switch_top_auto_recovery")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // Item 1: Save every
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    tempIntervalText = autoRecoveryInterval.toString()
                                    showIntervalDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.save_every),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.save_every_minutes, autoRecoveryInterval),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Item 2: Automatically Save the Document too
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.auto_save_doc_too),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoSaveDocumentToo,
                                onCheckedChange = {
                                    autoSaveDocumentToo = it
                                    prefs.edit().putBoolean("auto_save_document_too", it).apply()
                                },
                                modifier = Modifier.testTag("switch_auto_save_doc")
                            )
                        }
                    }
                }
            }

            "backup_copies" -> {
                // SUB-SUBPAGE: Always Create a Backup Copy
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Expressive Card Container for the primary option
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.always_create_backup_copy),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = alwaysCreateBackupCopy,
                                onCheckedChange = {
                                    alwaysCreateBackupCopy = it
                                    prefs.edit().putBoolean("always_create_backup_copy", it).apply()
                                },
                                modifier = Modifier.testTag("switch_top_backup_copy")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text(
                                text = stringResource(R.string.place_backup_same_folder),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = placeBackupInSameFolder,
                                onCheckedChange = {
                                    placeBackupInSameFolder = it
                                    prefs.edit().putBoolean("place_backup_in_same_folder", it).apply()
                                },
                                modifier = Modifier.testTag("switch_place_backup_same_folder")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.default_backup_dir_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            else -> {
                // MAIN SUBPAGE: General (Loading and saving documents)
                Column(modifier = Modifier.fillMaxWidth()) {
                    // GROUP 1: Load
                    SubCategoryHeader(title = stringResource(R.string.load_group_title))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // Load user-specific settings
                        SubSettingSwitchRow(
                            title = stringResource(R.string.load_user_settings),
                            checked = loadUserSpecificSettings,
                            onCheckedChange = {
                                loadUserSpecificSettings = it
                                prefs.edit().putBoolean("load_user_specific_settings", it).apply()
                            },
                            testTag = "switch_load_user_settings"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Load printer settings
                        SubSettingSwitchRow(
                            title = stringResource(R.string.load_printer_settings),
                            checked = loadPrinterSettings,
                            onCheckedChange = {
                                loadPrinterSettings = it
                                prefs.edit().putBoolean("load_printer_settings", it).apply()
                            },
                            testTag = "switch_load_printer_settings"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GROUP 2: Save
                    SubCategoryHeader(title = stringResource(R.string.save_group_title))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // Save Auto Recovery Options (Sub-subpage + Toggle Switch)
                        SubSettingComplexSubpageRow(
                            title = stringResource(R.string.save_auto_recovery_title),
                            switchChecked = autoRecoveryEnabled,
                            onSwitchChange = {
                                autoRecoveryEnabled = it
                                prefs.edit().putBoolean("auto_recovery_enabled", it).apply()
                            },
                            onNavigate = { onNavigateSubSubpage("auto_recovery") },
                            switchTestTag = "switch_auto_recovery"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Edit document properties before saving
                        SubSettingSwitchRow(
                            title = stringResource(R.string.edit_doc_props_before_saving),
                            checked = editDocPropertiesBeforeSaving,
                            onCheckedChange = {
                                editDocPropertiesBeforeSaving = it
                                prefs.edit().putBoolean("edit_doc_properties_before_saving", it).apply()
                            },
                            testTag = "switch_edit_doc_props"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Always create backup copy (Sub-subpage + Toggle Switch)
                        SubSettingComplexSubpageRow(
                            title = stringResource(R.string.always_create_backup_copy),
                            switchChecked = alwaysCreateBackupCopy,
                            onSwitchChange = {
                                alwaysCreateBackupCopy = it
                                prefs.edit().putBoolean("always_create_backup_copy", it).apply()
                            },
                            onNavigate = { onNavigateSubSubpage("backup_copies") },
                            switchTestTag = "switch_backup_copy"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Save URLs Relative to File System
                        SubSettingSwitchRow(
                            title = stringResource(R.string.save_urls_relative_file_system),
                            checked = saveUrlsRelativeFileSystem,
                            onCheckedChange = {
                                saveUrlsRelativeFileSystem = it
                                prefs.edit().putBoolean("save_urls_relative_file_system", it).apply()
                            },
                            testTag = "switch_save_urls_file_system"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Save URLs Relative to Internet
                        SubSettingSwitchRow(
                            title = stringResource(R.string.save_urls_relative_internet),
                            checked = saveUrlsRelativeInternet,
                            onCheckedChange = {
                                saveUrlsRelativeInternet = it
                                prefs.edit().putBoolean("save_urls_relative_internet", it).apply()
                            },
                            testTag = "switch_save_urls_internet"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GROUP 3: Default File Formats and ODF Settings
                    SubCategoryHeader(title = stringResource(R.string.default_file_formats_odf_group))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        // ODF Format Version
                        Box {
                            SubSettingDropdownRow(
                                title = stringResource(R.string.odf_format_version),
                                valueSubtitle = odfFormatVersion,
                                onClick = { showOdfVersionMenu = true }
                            )

                            DropdownMenu(
                                expanded = showOdfVersionMenu,
                                onDismissRequest = { showOdfVersionMenu = false }
                            ) {
                                listOf(
                                    "1.4 Extended (recommended)",
                                    "1.3 Extended",
                                    "1.3",
                                    "1.2 Extended",
                                    "1.2"
                                ).forEach { versionOption ->
                                    DropdownMenuItem(
                                        text = { Text(versionOption) },
                                        onClick = {
                                            odfFormatVersion = versionOption
                                            prefs.edit().putString("odf_format_version", versionOption).apply()
                                            showOdfVersionMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Always Save As (Simplified to 2 options: OpenDocument or Microsoft Office)
                        Box {
                            SubSettingDropdownRow(
                                title = stringResource(R.string.always_save_as),
                                valueSubtitle = alwaysSaveAs,
                                onClick = { showAlwaysSaveAsMenu = true }
                            )

                            DropdownMenu(
                                expanded = showAlwaysSaveAsMenu,
                                onDismissRequest = { showAlwaysSaveAsMenu = false }
                            ) {
                                listOf(
                                    "OpenDocument",
                                    "Microsoft Office"
                                ).forEach { formatOption ->
                                    DropdownMenuItem(
                                        text = { Text(formatOption) },
                                        onClick = {
                                            alwaysSaveAs = formatOption
                                            prefs.edit().putString("always_save_as", formatOption).apply()
                                            showAlwaysSaveAsMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Warn when not saving in ODF format
                        SubSettingSwitchRow(
                            title = stringResource(R.string.warn_when_not_saving_odf),
                            checked = warnWhenNotSavingOdf,
                            onCheckedChange = {
                                warnWhenNotSavingOdf = it
                                prefs.edit().putBoolean("warn_when_not_saving_odf", it).apply()
                            },
                            testTag = "switch_warn_non_odf"
                        )
                    }
                }
            }
        }
    }

    // Interval Dialog
    if (showIntervalDialog) {
        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            title = { Text(stringResource(R.string.save_auto_recovery_title)) },
            text = {
                Column {
                    Text("Enter interval in minutes for auto recovery saving:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempIntervalText,
                        onValueChange = { tempIntervalText = it.filter { char -> char.isDigit() } },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = tempIntervalText.toIntOrNull() ?: 10
                        val validMinutes = if (minutes > 0) minutes else 10
                        autoRecoveryInterval = validMinutes
                        prefs.edit().putInt("auto_recovery_interval", validMinutes).apply()
                        showIntervalDialog = false
                        Toast.makeText(context, "Auto Recovery set to every $validMinutes minutes", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun SubCategoryHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SubSettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
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
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
fun SubSettingComplexSubpageRow(
    title: String,
    switchChecked: Boolean,
    onSwitchChange: (Boolean) -> Unit,
    onNavigate: () -> Unit,
    switchTestTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigate() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Open subpage",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            Switch(
                checked = switchChecked,
                onCheckedChange = onSwitchChange,
                modifier = Modifier.testTag(switchTestTag)
            )
        }
    }
}

@Composable
fun SubSettingDropdownRow(
    title: String,
    valueSubtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = valueSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
