package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun SaveAsDialog(
    moduleType: String = "Inky", // "Inky", "Cellina", "Slidia", "Pagella"
    currentTitle: String = "",
    onDismiss: () -> Unit,
    onConfirmSave: (selectedFormat: String, extension: String, mimeType: String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("papirus_options", Context.MODE_PRIVATE) }
    val defaultFormat = remember { prefs.getString("always_save_as", "OpenDocument") ?: "OpenDocument" }
    val warnNonOdf = remember { prefs.getBoolean("warn_when_not_saving_odf", true) }

    var selectedFormatOption by remember { mutableStateOf(if (defaultFormat == "Microsoft Office") "Microsoft Office" else "OpenDocument") }
    var saveWithPassword by remember { mutableStateOf(false) }
    var encryptWithGpg by remember { mutableStateOf(false) }
    var autoAddSuffix by remember { mutableStateOf(true) }

    var showNonOdfWarning by remember { mutableStateOf(false) }

    val (odfSub, ooxmlSub, odfExt, ooxmlExt, odfMime, ooxmlMime) = when (moduleType) {
        "Cellina" -> Sextuple("ODS", "XLSX", ".ods", ".xlsx", "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        "Slidia" -> Sextuple("ODP", "PPTX", ".odp", ".pptx", "application/vnd.oasis.opendocument.presentation", "application/vnd.openxmlformats-officedocument.presentationml.slideshow")
        "Pagella" -> Sextuple("PDF", "PDF/A", ".pdf", ".pdf", "application/pdf", "application/pdf")
        else -> Sextuple("ODT", "DOCX", ".odt", ".docx", "application/vnd.oasis.opendocument.text", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
    }

    if (showNonOdfWarning) {
        AlertDialog(
            onDismissRequest = {
                showNonOdfWarning = false
            },
            title = {
                Text(
                    text = stringResource(R.string.warn_non_odf_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.warn_non_odf_msg, ooxmlSub),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNonOdfWarning = false
                        onConfirmSave("Microsoft Office", ooxmlExt, ooxmlMime)
                    },
                    modifier = Modifier.testTag("btn_warning_continue")
                ) {
                    Text(stringResource(R.string.btn_continue), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNonOdfWarning = false
                    },
                    modifier = Modifier.testTag("btn_warning_cancel")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = stringResource(R.string.save_as_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Entry 1: OpenDocument
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedFormatOption == "OpenDocument"),
                                onClick = { selectedFormatOption = "OpenDocument" },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedFormatOption == "OpenDocument"),
                            onClick = null,
                            modifier = Modifier.testTag("radio_opendocument")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.save_as_opendocument),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = odfSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Entry 2: Microsoft Office
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selectedFormatOption == "Microsoft Office"),
                                onClick = { selectedFormatOption = "Microsoft Office" },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedFormatOption == "Microsoft Office"),
                            onClick = null,
                            modifier = Modifier.testTag("radio_microsoft_office")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.save_as_microsoft_office),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = ooxmlSub,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Checkbox 1: Save with Password
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveWithPassword = !saveWithPassword }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.save_as_with_password),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = saveWithPassword,
                            onCheckedChange = { saveWithPassword = it },
                            modifier = Modifier.testTag("checkbox_save_password")
                        )
                    }

                    // Checkbox 2: Encrypt with GPG
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { encryptWithGpg = !encryptWithGpg }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.save_as_encrypt_gpg),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Checkbox(
                            checked = encryptWithGpg,
                            onCheckedChange = { encryptWithGpg = it },
                            modifier = Modifier.testTag("checkbox_encrypt_gpg")
                        )
                    }

                    // Checkbox 3: Automatically Add File Format Suffix
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoAddSuffix = !autoAddSuffix }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.save_as_auto_suffix),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Checkbox(
                            checked = autoAddSuffix,
                            onCheckedChange = { autoAddSuffix = it },
                            modifier = Modifier.testTag("checkbox_auto_suffix")
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedFormatOption == "Microsoft Office" && warnNonOdf) {
                            showNonOdfWarning = true
                        } else {
                            val (ext, mime) = if (selectedFormatOption == "Microsoft Office") {
                                Pair(ooxmlExt, ooxmlMime)
                            } else {
                                Pair(odfExt, odfMime)
                            }
                            onConfirmSave(selectedFormatOption, ext, mime)
                        }
                    },
                    modifier = Modifier.testTag("btn_save_as_confirm")
                ) {
                    Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_save_as_cancel")
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)
