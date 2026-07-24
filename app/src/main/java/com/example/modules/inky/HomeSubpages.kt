package com.example.modules.inky

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

// ==========================================
// M3 Expressive Shared UI Base Components
// (Derived from File Subpage Design Base)
// ==========================================

@Composable
fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun HomeSectionDivider() {
    Spacer(modifier = Modifier.height(12.dp))
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun M3ListItem(
    headlineText: String,
    supportingText: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(modifier = Modifier.padding(end = 16.dp)) {
                    leadingIcon()
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (trailingContent != null) {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    trailingContent()
                }
            }
        }
    }
}

@Composable
fun ExpressiveActionCard(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ThreeColumnRow(
    col1: @Composable RowScope.() -> Unit,
    col2: @Composable RowScope.() -> Unit,
    col3: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth()) { col1() }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth()) { col2() }
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(modifier = Modifier.fillMaxWidth()) { col3() }
        }
    }
}

// ==========================================
// Home Ribbon Tab Main Subpage
// ==========================================

@Composable
fun HomeSubpage(
    context: Context,
    isBold: Boolean,
    onBoldChange: (Boolean) -> Unit,
    isItalic: Boolean,
    onItalicChange: (Boolean) -> Unit,
    isUnderline: Boolean,
    onUnderlineChange: (Boolean) -> Unit,
    isStrikethrough: Boolean,
    onStrikethroughChange: (Boolean) -> Unit,
    activeFontFamily: String,
    activeFontSize: Int,
    fontColor: Color,
    highlightColor: Color,
    textAlignment: TextAlign,
    onTextAlignmentChange: (TextAlign) -> Unit,
    onNavigateSubpage: (String) -> Unit,
    onShowFontSizeDialog: () -> Unit
) {
    var showRtlState by remember { mutableStateOf(false) }
    var showParagraphMarks by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- GRUP EDIT ---
        HomeSectionHeader("Edit")

        // Paste Item
        M3ListItem(
            headlineText = "Paste",
            supportingText = "Paste from clipboard",
            leadingIcon = {
                Icon(
                    Icons.Rounded.ContentPaste,
                    contentDescription = "Paste",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                IconButton(onClick = { onNavigateSubpage("paste_options") }) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Paste Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            onClick = {
                Toast.makeText(context, "Pasting text from clipboard...", Toast.LENGTH_SHORT).show()
            }
        )

        // Cut, Copy, Painter 3-Column Expressive Row
        ThreeColumnRow(
            col1 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.ContentCut,
                    label = "Cut",
                    onClick = { Toast.makeText(context, "Cut text", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_cut_btn")
                )
            },
            col2 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy",
                    onClick = { Toast.makeText(context, "Copied text", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_copy_btn")
                )
            },
            col3 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.FormatPaint,
                    label = "Painter",
                    onClick = { Toast.makeText(context, "Format Painter activated", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth().testTag("home_painter_btn")
                )
            }
        )

        HomeSectionDivider()

        // --- GRUP CHARACTER ---
        HomeSectionHeader("Character")

        M3ListItem(
            headlineText = "Font style",
            supportingText = activeFontFamily,
            leadingIcon = {
                Icon(
                    Icons.Rounded.FontDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Font", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("font_style") }
        )

        M3ListItem(
            headlineText = "Font size",
            supportingText = "$activeFontSize pt",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatSize,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.Edit, contentDescription = "Ubah Ukuran Font", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = onShowFontSizeDialog
        )

        // Bold, Italic, Underline Expressive Cards
        ThreeColumnRow(
            col1 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.FormatBold,
                    label = "Bold",
                    isSelected = isBold,
                    onClick = { onBoldChange(!isBold) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col2 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.FormatItalic,
                    label = "Italic",
                    isSelected = isItalic,
                    onClick = { onItalicChange(!isItalic) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col3 = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isUnderline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onUnderlineChange(!isUnderline) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Rounded.FormatUnderlined,
                                    contentDescription = "Underline",
                                    tint = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    "Underline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    color = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        )
                        IconButton(
                            onClick = { onNavigateSubpage("underline_options") },
                            modifier = Modifier.width(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Underline Options",
                                tint = if (isUnderline) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        )

        // Strikethrough, Subscript, Superscript Row
        ThreeColumnRow(
            col1 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.StrikethroughS,
                    label = "Strikethrough",
                    isSelected = isStrikethrough,
                    onClick = { onStrikethroughChange(!isStrikethrough) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col2 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.Subscript,
                    label = "Subscript",
                    onClick = { Toast.makeText(context, "Subscript applied", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col3 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.Superscript,
                    label = "Superscript",
                    onClick = { Toast.makeText(context, "Superscript applied", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        M3ListItem(
            headlineText = "Change Capitalization",
            leadingIcon = {
                Icon(
                    Icons.Rounded.TextFields,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Change Capitalization Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("change_capitalization") }
        )

        M3ListItem(
            headlineText = "Font Color",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(fontColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("font_color") }
        )

        M3ListItem(
            headlineText = "Highlight Text Color",
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (highlightColor == Color.Transparent) Color.LightGray else highlightColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("highlight_color") }
        )

        M3ListItem(
            headlineText = "Delete all formatting",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatClear,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = {
                onBoldChange(false)
                onItalicChange(false)
                onUnderlineChange(false)
                onStrikethroughChange(false)
                Toast.makeText(context, "All formatting cleared!", Toast.LENGTH_SHORT).show()
            }
        )

        M3ListItem(
            headlineText = "Character Options",
            leadingIcon = {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = { Toast.makeText(context, "Character Options will be available soon", Toast.LENGTH_SHORT).show() }
        )

        HomeSectionDivider()

        // --- GRUP PARAGRAPH ---
        HomeSectionHeader("Paragraph")

        // Alignments 4-card row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val alignments = listOf(
                TextAlign.Left to Icons.Rounded.FormatAlignLeft,
                TextAlign.Center to Icons.Rounded.FormatAlignCenter,
                TextAlign.Right to Icons.Rounded.FormatAlignRight,
                TextAlign.Justify to Icons.Rounded.FormatAlignJustify
            )
            alignments.forEach { (align, icon) ->
                val isSelected = textAlignment == align
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable { onTextAlignmentChange(align) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Align ${align.toString()}",
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Indent Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExpressiveActionCard(
                icon = Icons.Rounded.FormatIndentIncrease,
                label = "Increase Indent",
                onClick = { Toast.makeText(context, "Indent increased", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            )
            ExpressiveActionCard(
                icon = Icons.Rounded.FormatIndentDecrease,
                label = "Decrease Indent",
                onClick = { Toast.makeText(context, "Indent decreased", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            )
        }

        M3ListItem(
            headlineText = "Set line spacing",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatLineSpacing,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Change Spacing", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("line_spacing") }
        )

        M3ListItem(
            headlineText = "Create bulleted list",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatListBulleted,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Bullets", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("bulleted_list") }
        )

        M3ListItem(
            headlineText = "Create numbered list",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatListNumbered,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Numbers", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("numbered_list") }
        )

        M3ListItem(
            headlineText = "Create multilevel list",
            leadingIcon = {
                Icon(
                    Icons.Rounded.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Multilevel", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("multilevel_list") }
        )

        M3ListItem(
            headlineText = "Toggle paragraph marks",
            leadingIcon = {
                Icon(
                    Icons.Rounded.Notes,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Switch(
                    checked = showParagraphMarks,
                    onCheckedChange = { showParagraphMarks = it }
                )
            },
            onClick = { showParagraphMarks = !showParagraphMarks }
        )

        M3ListItem(
            headlineText = "Sort text/table",
            leadingIcon = {
                Icon(
                    Icons.Rounded.SortByAlpha,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = { Toast.makeText(context, "Sorting document...", Toast.LENGTH_SHORT).show() }
        )

        M3ListItem(
            headlineText = "Toggle RTL writing direction",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatTextdirectionRToL,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Switch(
                    checked = showRtlState,
                    onCheckedChange = { showRtlState = it }
                )
            },
            onClick = { showRtlState = !showRtlState }
        )

        M3ListItem(
            headlineText = "Paragraph Shading Color",
            leadingIcon = {
                Icon(
                    Icons.Rounded.FormatColorFill,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Select Shading Color", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("paragraph_shading") }
        )

        M3ListItem(
            headlineText = "Paragraph Border",
            leadingIcon = {
                Icon(
                    Icons.Rounded.BorderAll,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Configure Border", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("paragraph_border") }
        )

        M3ListItem(
            headlineText = "Paragraph Options",
            leadingIcon = {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = { Toast.makeText(context, "Paragraph Options will be available soon", Toast.LENGTH_SHORT).show() }
        )

        HomeSectionDivider()

        // --- GRUP STYLES ---
        HomeSectionHeader("Styles")

        M3ListItem(
            headlineText = "Style selector",
            supportingText = "Select paragraph formatting style",
            leadingIcon = {
                Icon(
                    Icons.Rounded.Style,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            trailingContent = {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Open Paragraph Styles", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            onClick = { onNavigateSubpage("paragraph_styles") }
        )

        M3ListItem(
            headlineText = "Paragraph Style Options",
            leadingIcon = {
                Icon(
                    Icons.Rounded.EditNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            onClick = { Toast.makeText(context, "Paragraph Style Options will be available soon", Toast.LENGTH_SHORT).show() }
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
// Sub-Subpages
// ==========================================

// 1. PasteOptionsSubpage
@Composable
fun PasteOptionsSubpage(context: Context, onShowPasteSpecial: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Paste Options")
        M3ListItem(
            headlineText = "Keep source formatting",
            supportingText = "Keep original style from the source",
            leadingIcon = { Icon(Icons.Rounded.Brush, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Pasted text with Keep Source Formatting", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Merge formatting",
            supportingText = "Merge source style with document style",
            leadingIcon = { Icon(Icons.Rounded.MergeType, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Pasted text with Merge Formatting", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Paste unformatted text",
            supportingText = "Paste clean text without formatting",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Clean text pasted successfully", Toast.LENGTH_SHORT).show() }
        )

        HomeSectionDivider()

        HomeSectionHeader("Advanced Paste")
        M3ListItem(
            headlineText = "Paste Special...",
            supportingText = "Advanced formatting choices",
            leadingIcon = { Icon(Icons.Rounded.SettingsApplications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = onShowPasteSpecial
        )
    }
}

// 2. FontStyleSubpage
@Composable
fun FontStyleSubpage(
    context: Context,
    currentFont: String,
    onFontSelected: (String) -> Unit
) {
    val fonts = listOf(
        "Aptos Display", "Calibri", "Arial", "Roboto", "Space Grotesk",
        "JetBrains Mono", "Montserrat", "Playfair Display", "Inter",
        "Merriweather", "Lora", "Open Sans", "Poppins"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Google Fonts Database")
        fonts.forEach { fontName ->
            val isSelected = fontName == currentFont
            M3ListItem(
                headlineText = fontName,
                supportingText = if (isSelected) "Active Font" else "Tap to apply",
                leadingIcon = {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier.size(24.dp)
                    )
                },
                onClick = {
                    onFontSelected(fontName)
                    Toast.makeText(context, "Font changed to $fontName", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 3. UnderlineOptionsSubpage
@Composable
fun UnderlineOptionsSubpage(
    context: Context,
    currentUnderlineState: Boolean,
    onOpenColorPage: () -> Unit
) {
    val lineStyles = listOf(
        "Single Line", "Double Line", "Thick Line", "Dotted Line",
        "Dashed Line", "Dot-Dashed Line", "Wave Line", "Double Wave Line"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Color Settings")
        M3ListItem(
            headlineText = "Underline color",
            supportingText = "Choose underline color palette",
            leadingIcon = { Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            trailingContent = { Icon(Icons.Rounded.ChevronRight, contentDescription = "Color", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            onClick = onOpenColorPage
        )

        HomeSectionDivider()

        HomeSectionHeader("Line Style")
        lineStyles.forEach { style ->
            M3ListItem(
                headlineText = style,
                supportingText = "Apply this underline style",
                leadingIcon = { Icon(Icons.Rounded.HorizontalRule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Underline style $style applied", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 4. LineSpacingSubpage
@Composable
fun LineSpacingSubpage(context: Context) {
    val options = listOf(
        "Single", "1,16 line", "1,5 line", "Double",
        "Proportional", "At least", "Leading", "Fixed"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Line Spacing Presets")
        options.forEach { opt ->
            M3ListItem(
                headlineText = opt,
                supportingText = "Select line spacing $opt",
                leadingIcon = { Icon(Icons.Rounded.LineStyle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Spacing $opt selected", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 5. BulletedListSubpage
@Composable
fun BulletedListSubpage(context: Context) {
    val variants = listOf(
        "Circle filled", "Circle outlined", "Rectangle filled",
        "Rectangle outlined", "Rhombus filled", "Rhombus outlined"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Bullet Variants")
        variants.forEach { variant ->
            M3ListItem(
                headlineText = variant,
                supportingText = "Apply this bullet style",
                leadingIcon = { Icon(Icons.Rounded.RadioButtonChecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Bulleted $variant applied successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 6. NumberedListSubpage
@Composable
fun NumberedListSubpage(context: Context) {
    val variants = listOf(
        "1.", "1)", "(1)", "A.", "a.", "a)", "(a)", "i.", "i)", "(i)"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Numbering Formats")
        variants.forEach { variant ->
            M3ListItem(
                headlineText = "Format $variant",
                supportingText = "Use this numbering sequence",
                leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Numbered $variant applied successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 7. MultilevelListSubpage
@Composable
fun MultilevelListSubpage(context: Context) {
    val variants = listOf(
        "1. > 1.1 > 1.1.1",
        "1. > A. > a.",
        "1) > a) > i)",
        "Chapter 1 > Section 1 > Subsection 1"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Multilevel List Structures")
        variants.forEach { variant ->
            M3ListItem(
                headlineText = variant,
                supportingText = "Use this multilevel structure",
                leadingIcon = { Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Multilevel list $variant applied successfully", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 8. ParagraphBorderSubpage
@Composable
fun ParagraphBorderSubpage(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Quick Border")
        M3ListItem(
            headlineText = "No border",
            supportingText = "Remove all border lines",
            leadingIcon = { Icon(Icons.Rounded.BorderClear, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Border cleared", Toast.LENGTH_SHORT).show() }
        )

        HomeSectionDivider()

        HomeSectionHeader("Normal Category")
        ThreeColumnRow(
            col1 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderTop,
                    label = "Top",
                    onClick = { Toast.makeText(context, "Normal Top Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col2 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderBottom,
                    label = "Bottom",
                    onClick = { Toast.makeText(context, "Normal Bottom Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col3 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderOuter,
                    label = "Sides",
                    onClick = { Toast.makeText(context, "Normal Side Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        HomeSectionHeader("Thick Category")
        ThreeColumnRow(
            col1 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderTop,
                    label = "Thick Top",
                    onClick = { Toast.makeText(context, "Thick Top Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col2 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderBottom,
                    label = "Thick Bot",
                    onClick = { Toast.makeText(context, "Thick Bottom Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            col3 = {
                ExpressiveActionCard(
                    icon = Icons.Rounded.BorderOuter,
                    label = "Thick Box",
                    onClick = { Toast.makeText(context, "Thick Outer Border", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        HomeSectionDivider()

        HomeSectionHeader("Grid & Box Models")
        M3ListItem(
            headlineText = "Box and grid",
            supportingText = "Apply complete box and grid borders",
            leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Box and Grid borders applied", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Box",
            supportingText = "Apply outer box border only",
            leadingIcon = { Icon(Icons.Rounded.CheckBoxOutlineBlank, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Outer Box border applied", Toast.LENGTH_SHORT).show() }
        )
        M3ListItem(
            headlineText = "Inside (Grid)",
            supportingText = "Inner grid lines only",
            leadingIcon = { Icon(Icons.Rounded.GridGoldenratio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = { Toast.makeText(context, "Inner Grid border applied", Toast.LENGTH_SHORT).show() }
        )
    }
}

// 9. ParagraphStylesSubpage
@Composable
fun ParagraphStylesSubpage(
    context: Context,
    selectedStyle: String,
    onNavigateStyleOptions: (String) -> Unit
) {
    var activeStyle by remember { mutableStateOf(selectedStyle) }
    val styles = listOf("Normal", "Heading 1", "Heading 2", "Heading 3", "Title", "Subtitle", "Footnote")

    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Paragraph Styles")
        styles.forEach { styleName ->
            val isSelected = styleName == activeStyle
            M3ListItem(
                headlineText = styleName,
                supportingText = if (isSelected) "Active formatting" else "Tap to apply style",
                leadingIcon = {
                    RadioButton(
                        selected = isSelected,
                        onClick = {
                            activeStyle = styleName
                            Toast.makeText(context, "Style changed to $styleName", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                trailingContent = {
                    IconButton(onClick = { onNavigateStyleOptions(styleName) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Options for $styleName", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                onClick = {
                    activeStyle = styleName
                    Toast.makeText(context, "Style changed to $styleName", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// 10. CreateNewStyleSubpage
@Composable
fun CreateNewStyleSubpage(
    context: Context,
    onSuccess: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Create Style")
        M3ListItem(
            headlineText = "Create New",
            supportingText = "Define new custom style parameters",
            leadingIcon = { Icon(Icons.Rounded.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "New style created successfully", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        M3ListItem(
            headlineText = "Create New from Text",
            supportingText = "Save current text formatting as a style",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "New style from selected text saved successfully", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
    }
}

// 11. StyleOptionsSubpage
@Composable
fun StyleOptionsSubpage(
    context: Context,
    styleName: String,
    onSuccess: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Style Actions ($styleName)")
        M3ListItem(
            headlineText = "Edit",
            supportingText = "Modify style attributes",
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "Editing style $styleName...", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        M3ListItem(
            headlineText = "Update from Text",
            supportingText = "Redefine style matching active text selection",
            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "Style $styleName updated", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        )
        if (styleName != "Normal") {
            M3ListItem(
                headlineText = "Delete",
                supportingText = "Remove style permanently",
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp)) },
                onClick = {
                    Toast.makeText(context, "Style $styleName deleted successfully", Toast.LENGTH_SHORT).show()
                    onSuccess()
                }
            )
        }
    }
}

// 12. ChangeCapitalizationSubpage
@Composable
fun ChangeCapitalizationSubpage(context: Context) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HomeSectionHeader("Capitalization Options")
        M3ListItem(
            headlineText = "First Character Uppercase",
            supportingText = "Capitalize first letter of selected text",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "First character capitalized", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "First word uppercase",
            supportingText = "Capitalize first letter of each sentence",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "First word capitalized", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "ALL UPPERCASE",
            supportingText = "Convert all characters to uppercase",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "ALL UPPERCASE", Toast.LENGTH_SHORT).show()
            }
        )
        M3ListItem(
            headlineText = "all lowercase",
            supportingText = "Convert all characters to lowercase",
            leadingIcon = { Icon(Icons.Rounded.TextFields, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            onClick = {
                Toast.makeText(context, "all lowercase", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// 13. ColorPickerSubpage
@Composable
fun ColorPickerSubpage(
    currentColor: Color,
    title: String,
    onColorSelected: (Color) -> Unit
) {
    var r by remember { mutableStateOf((currentColor.red * 255).toInt()) }
    var g by remember { mutableStateOf((currentColor.green * 255).toInt()) }
    var b by remember { mutableStateOf((currentColor.blue * 255).toInt()) }
    var hex by remember { mutableStateOf(String.format("#%02X%02X%02X", r, g, b)) }

    LaunchedEffect(r, g, b) {
        val newColor = Color(r, g, b)
        hex = String.format("#%02X%02X%02X", r, g, b)
        onColorSelected(newColor)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeSectionHeader("Color Presets ($title)")

        val presets = listOf(
            Color.Red, Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7),
            Color(0xFF3F51B5), Color(0xFF2196F3), Color(0xFF00BCD4), Color(0xFF009688),
            Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
            Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548),
            Color.Black, Color.DarkGray, Color.Gray, Color.LightGray, Color.White
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            2.dp,
                            if (currentColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                            CircleShape
                        )
                        .clickable {
                            r = (color.red * 255).toInt()
                            g = (color.green * 255).toInt()
                            b = (color.blue * 255).toInt()
                        }
                )
            }
        }

        HomeSectionDivider()

        HomeSectionHeader("Custom Color Picker")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(r, g, b))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            )
            Column {
                Text("Color Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(hex, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // R Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("R", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Red)
                Slider(
                    value = r.toFloat(),
                    onValueChange = { r = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(r.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
            // G Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("G", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Green)
                Slider(
                    value = g.toFloat(),
                    onValueChange = { g = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(g.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
            // B Slider
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("B", modifier = Modifier.width(16.dp), fontWeight = FontWeight.Bold, color = Color.Blue)
                Slider(
                    value = b.toFloat(),
                    onValueChange = { b = it.toInt() },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
                Text(b.toString(), modifier = Modifier.width(32.dp), textAlign = TextAlign.End)
            }
        }

        OutlinedTextField(
            value = hex,
            onValueChange = { input ->
                if (input.length <= 7) {
                    hex = input
                    try {
                        val parsed = Color(android.graphics.Color.parseColor(input))
                        r = (parsed.red * 255).toInt()
                        g = (parsed.green * 255).toInt()
                        b = (parsed.blue * 255).toInt()
                    } catch (e: Exception) {
                        // ignore invalid format during typing
                    }
                }
            },
            label = { Text("HEX Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace)
        )
    }
}

// 14. Font Size Dialog
@Composable
fun FontSizeDialog(
    currentSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sizeInput by remember { mutableStateOf(currentSize.toString()) }
    var sizeValue by remember { mutableStateOf(currentSize.toFloat()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Ubah Ukuran Font",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = sizeInput,
                    onValueChange = { input ->
                        sizeInput = input
                        input.toIntOrNull()?.let {
                            if (it in 1..200) {
                                sizeValue = it.toFloat()
                            }
                        }
                    },
                    label = { Text("Font size (pt)") },
                    modifier = Modifier.fillMaxWidth().testTag("font_size_input_field"),
                    singleLine = true
                )

                Text(
                    text = "Slide to adjust (max 96 pt):",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = sizeValue.coerceIn(1f, 96f),
                    onValueChange = {
                        sizeValue = it
                        sizeInput = it.toInt().toString()
                    },
                    valueRange = 1f..96f,
                    modifier = Modifier.fillMaxWidth().testTag("font_size_slider_widget")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            sizeInput.toIntOrNull()?.let {
                                onConfirm(it)
                            } ?: onConfirm(sizeValue.toInt())
                        },
                        modifier = Modifier.testTag("font_size_confirm_ok")
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

// 15. Paste Special Dialog
@Composable
fun PasteSpecialDialog(
    onDismiss: () -> Unit,
    onPasteSuccess: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Paste Special",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Select advanced paste format:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val formats = listOf(
                    "HTML Format",
                    "Rich Text (RTF)",
                    "Unicode Text (UTF-8)",
                    "Unformatted Unicode Text",
                    "Device Independent Bitmap"
                )

                formats.forEach { fmt ->
                    OutlinedButton(
                        onClick = {
                            onPasteSuccess(fmt)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(fmt)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
